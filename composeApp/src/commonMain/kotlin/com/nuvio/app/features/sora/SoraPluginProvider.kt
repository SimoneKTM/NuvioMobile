package com.nuvio.app.features.sora

import com.nuvio.app.features.plugins.PluginManifest
import com.nuvio.app.features.plugins.PluginManifestScraper
import com.nuvio.app.features.plugins.PluginRuntimeResult
import com.nuvio.app.features.plugins.PluginScraper

class SoraPluginProvider(private val repository: SoraPluginRepository = SoraPluginRepository) {

    fun buildPluginManifest(repo: SoraRepository): PluginManifest {
        val modules = repository.uiState.value.modules
            .filter { it.repositoryUrl == repo.id }
        return PluginManifest(
            name = repo.name,
            version = "1.0.0",
            description = "Sora modules from ${repo.name}",
            author = "Sora Plugin Provider",
            scrapers = modules.map { module ->
                PluginManifestScraper(
                    id = module.id,
                    name = module.manifest.sourceName,
                    description = "Sora module - ${module.manifest.language} (${module.manifest.quality})",
                    version = module.manifest.version,
                    filename = module.manifest.scriptUrl,
                    supportedTypes = soraTypeToMediaType(module.manifest.type),
                    enabled = module.enabled,
                    hasSettings = false,
                    logo = module.manifest.iconUrl,
                    contentLanguage = listOf(module.manifest.language),
                    supportedFormats = listOf(module.manifest.streamType),
                )
            }
        )
    }

    fun getPluginScrapers(): List<PluginScraper> {
        return repository.uiState.value.modules.map { module ->
            PluginScraper(
                id = module.id,
                repositoryUrl = module.repositoryUrl,
                name = module.manifest.sourceName,
                description = "Sora: ${module.manifest.sourceName} (${module.manifest.language})",
                version = module.manifest.version,
                filename = module.manifest.scriptUrl,
                supportedTypes = soraTypeToMediaType(module.manifest.type),
                enabled = module.enabled,
                manifestEnabled = true,
                hasSettings = false,
                logo = module.manifest.iconUrl,
                contentLanguage = listOf(module.manifest.language),
                formats = listOf(module.manifest.streamType),
                code = module.scriptCode,
            )
        }
    }

    suspend fun executeSoraModule(
        module: SoraModule,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<PluginRuntimeResult> {
        return SoraRuntimeAdapter.executeSoraModule(module, tmdbId, mediaType, season, episode)
    }

    suspend fun executeScraper(
        scraper: PluginScraper,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<PluginRuntimeResult> {
        val module = repository.uiState.value.modules.find { it.id == scraper.id }
            ?: return emptyList()
        return executeSoraModule(module, tmdbId, mediaType, season, episode)
    }

    fun addRepository(url: String) {
        repository.addRepository(url)
    }

    fun removeRepository(repoId: String) {
        repository.removeRepository(repoId)
    }

    fun refreshRepository(repoId: String) {
        repository.refreshRepository(repoId)
    }

    fun refreshAll() {
        repository.refreshAll()
    }

    fun toggleModule(moduleId: String, enabled: Boolean) {
        repository.toggleModule(moduleId, enabled)
    }

    fun getEnabledModulesForType(mediaType: String): List<SoraModule> {
        return repository.getEnabledModulesForType(mediaType)
    }
}
