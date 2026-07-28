package com.nuvio.app.features.anime

import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.catalog.fetchCatalogPage
import com.nuvio.app.features.home.HomeCatalogDefinition
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.HomeUiState
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.buildHomeCatalogDefinitions
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

object AnimeHomeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var activeRequestKey: String? = null
    private var completedRequestKey: String? = null
    private var currentDefinitions: List<HomeCatalogDefinition> = emptyList()
    private var cachedSections: Map<String, HomeCatalogSection> = emptyMap()
    private var lastErrorMessage: String? = null

    fun refresh(addons: List<ManagedAddon>, force: Boolean = false) {
        val activeAddons = addons.filter { it.enabled }
        val requests = buildHomeCatalogDefinitions(activeAddons)
        currentDefinitions = requests
        val requestCacheKeys = requests.mapTo(mutableSetOf(), HomeCatalogDefinition::cacheKey)
        cachedSections = cachedSections.filterKeys(requestCacheKeys::contains)
        val requestKey = requests.joinToString(separator = "|", transform = HomeCatalogDefinition::cacheKey)

        if (!force && activeRequestKey == requestKey && _uiState.value.isLoading) return

        if (
            !force &&
            requestKey == completedRequestKey &&
            requestCacheKeys.all(cachedSections::containsKey) &&
            requestCacheKeys.any { hasRenderableCachedSection(it) }
        ) {
            if (_uiState.value.sections.isEmpty() || _uiState.value.heroItems.isEmpty()) {
                applyCurrentSettings()
            }
            return
        }
        activeRequestKey = requestKey

        if (requests.isEmpty()) {
            activeJob?.cancel()
            activeJob = null
            activeRequestKey = null
            completedRequestKey = requestKey
            cachedSections = emptyMap()
            lastErrorMessage = null
            publishCurrentState(isLoading = false, requestKey = requestKey)
            return
        }

        activeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        activeJob = scope.launch {
            val pendingRequests = requests.filter { definition ->
                force || cachedSections[definition.cacheKey] == null
            }
            if (pendingRequests.isEmpty()) {
                publishCurrentState(isLoading = false, requestKey = requestKey)
                return@launch
            }
            val loadedSections = linkedMapOf<String, HomeCatalogSection>().apply {
                putAll(cachedSections)
            }
            var firstErrorMessage: String? = null
            var batchIndex = 0

            pendingRequests.chunked(HOME_CATALOG_FETCH_BATCH_SIZE).forEach { batch ->
                if (activeRequestKey != requestKey) return@launch
                val results = batch.map { request ->
                    async { request to runCatching { request.toSection() } }
                }.awaitAll()

                if (activeRequestKey != requestKey) return@launch

                results.mapNotNull { (request, result) ->
                    result.getOrNull()?.let { section -> request.cacheKey to section }
                }.forEach { (cacheKey, section) ->
                    loadedSections[cacheKey] = section
                }
                if (firstErrorMessage == null) {
                    firstErrorMessage = results.firstNotNullOfOrNull { (_, result) ->
                        result.exceptionOrNull()?.message
                    }
                }
                cachedSections = loadedSections.toMap()
                lastErrorMessage = firstErrorMessage
                if (batchIndex == 0 || (batchIndex + 1) % HOME_CATALOG_PUBLISH_INTERVAL == 0) {
                    publishCurrentState(isLoading = true, requestKey = requestKey)
                }
                batchIndex++
            }

            if (activeRequestKey != requestKey) return@launch

            cachedSections = loadedSections.toMap()
            lastErrorMessage = firstErrorMessage
            if (cachedSections.values.any { section -> section.items.isNotEmpty() }) {
                completedRequestKey = requestKey
            }
            activeRequestKey = null
            publishCurrentState(isLoading = false, requestKey = requestKey)
        }
    }

    fun applyCurrentSettings() {
        publishCurrentState(
            isLoading = _uiState.value.isLoading,
            requestKey = activeRequestKey ?: completedRequestKey,
        )
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        activeRequestKey = null
        completedRequestKey = null
        currentDefinitions = emptyList()
        cachedSections = emptyMap()
        lastErrorMessage = null
        _uiState.value = HomeUiState()
    }

    private fun hasRenderableCachedSection(cacheKey: String): Boolean =
        cachedSections[cacheKey]?.items?.isNotEmpty() == true

    private fun publishCurrentState(
        isLoading: Boolean,
        requestKey: String?,
    ) {
        val snapshot = AnimeHomeCatalogSettingsRepository.snapshot()
        val preferences = snapshot.preferences
        val todayIsoDate = if (snapshot.hideUnreleasedContent) CurrentDateProvider.todayIsoDate() else null

        fun MetaPreview.isUnreleasedForAnime(todayIsoDate: String): Boolean {
            rawReleaseDate
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { rawReleased ->
                    isoCalendarDateOrNull(rawReleased.substringBefore('T'))?.let { releaseDate ->
                        return releaseDate > todayIsoDate
                    }
                }
            val info = releaseInfo ?: return false
            isoCalendarDateOrNull(info.trim())?.let { releaseDate ->
                return releaseDate > todayIsoDate
            }
            val yearMatch = Regex("""\b(19|20)\d{2}\b""").find(info)?.value?.toIntOrNull() ?: return false
            val currentYear = todayIsoDate.take(4).toIntOrNull() ?: return false
            return yearMatch > currentYear
        }

        fun HomeCatalogSection.withReleaseFilterForAnime(): HomeCatalogSection {
            val filteredItems = items.filterNot { item ->
                todayIsoDate?.let { item.isUnreleasedForAnime(it) } ?: false
            }
            return if (filteredItems.size == items.size) this else copy(items = filteredItems)
        }

        val sections = currentDefinitions
            .sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
            .mapNotNull { definition ->
                val preference = preferences[definition.key]
                if (preference?.enabled == false) return@mapNotNull null

                val section = cachedSections[definition.cacheKey]?.let { section ->
                    if (todayIsoDate == null) section else section.withReleaseFilterForAnime()
                } ?: return@mapNotNull null
                if (section.items.isEmpty()) return@mapNotNull null
                val customTitle = preference?.customTitle.orEmpty()
                section.copy(
                    title = customTitle.ifBlank { definition.titleFor(snapshot.showCatalogType) },
                )
            }

        val catalogHeroItems = if (snapshot.heroEnabled) {
            val heroRandom = Random((requestKey?.hashCode() ?: 0).absoluteValue + 1)
            currentDefinitions
                .filter { definition -> preferences[definition.key]?.heroSourceEnabled != false }
                .mapNotNull { definition -> cachedSections[definition.cacheKey] }
                .flatMap { section -> section.items }
                .distinctBy { item -> "${item.type}:${item.id}" }
                .shuffled(heroRandom)
                .take(HOME_HERO_ITEM_LIMIT)
        } else {
            emptyList()
        }
        val heroItems = if (snapshot.heroEnabled) catalogHeroItems else emptyList()

        _uiState.value = HomeUiState(
            isLoading = isLoading,
            heroItems = heroItems,
            sections = sections,
            errorMessage = if (sections.isEmpty()) lastErrorMessage else null,
        )
    }

    private suspend fun HomeCatalogDefinition.toSection(): HomeCatalogSection {
        val page = fetchCatalogPage(
            manifestUrl = manifestUrl,
            type = type,
            catalogId = catalogId,
            maxItems = HOME_CATALOG_PREVIEW_FETCH_LIMIT,
        )
        val items = page.items.map { it.copy(isAnime = true) }
        if (items.isEmpty()) {
            return HomeCatalogSection(
                key = key,
                title = defaultTitle,
                subtitle = addonName,
                addonName = addonName,
                target = CatalogTarget.Addon(
                    manifestUrl = manifestUrl,
                    contentType = type,
                    catalogId = catalogId,
                    supportsPagination = supportsPagination,
                ),
                items = emptyList(),
                availableItemCount = 0,
                hasMore = false,
            )
        }

        return HomeCatalogSection(
            key = key,
            title = defaultTitle,
            subtitle = addonName,
            addonName = addonName,
            target = CatalogTarget.Addon(
                manifestUrl = manifestUrl,
                contentType = type,
                catalogId = catalogId,
                supportsPagination = supportsPagination,
            ),
            items = items,
            availableItemCount = page.rawItemCount,
            hasMore = supportsPagination && page.nextSkip != null,
        )
    }
}

private fun isoCalendarDateOrNull(value: String?): String? {
    val date = value?.trim()?.takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) } ?: return null
    val year = date.substring(0, 4).toIntOrNull() ?: return null
    val month = date.substring(5, 7).toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = date.substring(8, 10).toIntOrNull() ?: return null
    if (day !in 1..daysInMonth(year, month)) return null
    return date
}

private fun daysInMonth(year: Int, month: Int): Int =
    when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private const val HOME_HERO_ITEM_LIMIT = 8
private const val HOME_CATALOG_FETCH_BATCH_SIZE = 4
private const val HOME_CATALOG_PREVIEW_FETCH_LIMIT = 18
private const val HOME_CATALOG_PUBLISH_INTERVAL = 2
