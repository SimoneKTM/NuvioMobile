package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.opensubtitles.OpenSubtitlesSubtitleItem
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_fetch_subtitles
import nuvio.composeapp.generated.resources.addon_title
import nuvio.composeapp.generated.resources.compose_player_built_in
import nuvio.composeapp.generated.resources.compose_player_none
import nuvio.composeapp.generated.resources.compose_player_opensubtitles_section
import nuvio.composeapp.generated.resources.compose_player_subtitles
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubtitleModal(
    visible: Boolean,
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleIndex: Int,
    addonSubtitles: List<AddonSubtitle>,
    selectedAddonSubtitleId: String?,
    isLoadingAddonSubtitles: Boolean,
    subtitleStyle: SubtitleStyleState,
    subtitleDelayMs: Int,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    openSubtitlesItems: List<OpenSubtitlesSubtitleItem>,
    selectedOpenSubtitlesFileId: Int?,
    isLoadingOpenSubtitles: Boolean,
    isOpenSubtitlesConfigured: Boolean,
    onBuiltInTrackSelected: (Int) -> Unit,
    onAddonSubtitleSelected: (AddonSubtitle) -> Unit,
    onFetchAddonSubtitles: () -> Unit,
    onOpenSubtitlesSearch: () -> Unit,
    onOpenSubtitlesItemSelected: (OpenSubtitlesSubtitleItem) -> Unit,
    onStyleChanged: (SubtitleStyleState) -> Unit,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onAutoSyncCapture: () -> Unit,
    onAutoSyncCueSelected: (SubtitleSyncCue) -> Unit,
    onAutoSyncReload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStylePanel by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                )
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(300)) { it / 3 } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(250)) { it / 3 } + fadeOut(tween(250)),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 500.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1A))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(Res.string.compose_player_subtitles),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (showStylePanel) Color(0xFF333333)
                                            else Color(0xFF252525)
                                        )
                                        .clickable { showStylePanel = !showStylePanel }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (showStylePanel) Icons.Rounded.Tune else Icons.Rounded.Settings,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        if (showStylePanel) {
                            SubtitleStylePanel(
                                style = subtitleStyle,
                                subtitleDelayMs = subtitleDelayMs,
                                selectedAddonSubtitle = selectedAddonSubtitle,
                                subtitleAutoSyncState = subtitleAutoSyncState,
                                isCompact = true,
                                onStyleChanged = onStyleChanged,
                                onSubtitleDelayChanged = onSubtitleDelayChanged,
                                onSubtitleDelayReset = onSubtitleDelayReset,
                                onAutoSyncCapture = onAutoSyncCapture,
                                onAutoSyncCueSelected = onAutoSyncCueSelected,
                                onAutoSyncReload = onAutoSyncReload,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        } else {
                            UnifiedSubtitleList(
                                subtitleTracks = subtitleTracks,
                                selectedSubtitleIndex = selectedSubtitleIndex,
                                onBuiltInTrackSelected = onBuiltInTrackSelected,
                                addonSubtitles = addonSubtitles,
                                selectedAddonSubtitleId = selectedAddonSubtitleId,
                                isLoadingAddonSubtitles = isLoadingAddonSubtitles,
                                onAddonSubtitleSelected = onAddonSubtitleSelected,
                                onFetchAddonSubtitles = onFetchAddonSubtitles,
                                openSubtitlesItems = openSubtitlesItems,
                                selectedOpenSubtitlesFileId = selectedOpenSubtitlesFileId,
                                isLoadingOpenSubtitles = isLoadingOpenSubtitles,
                                isOpenSubtitlesConfigured = isOpenSubtitlesConfigured,
                                onOpenSubtitlesSearch = onOpenSubtitlesSearch,
                                onOpenSubtitlesItemSelected = onOpenSubtitlesItemSelected,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedSubtitleList(
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleIndex: Int,
    onBuiltInTrackSelected: (Int) -> Unit,
    addonSubtitles: List<AddonSubtitle>,
    selectedAddonSubtitleId: String?,
    isLoadingAddonSubtitles: Boolean,
    onAddonSubtitleSelected: (AddonSubtitle) -> Unit,
    onFetchAddonSubtitles: () -> Unit,
    openSubtitlesItems: List<OpenSubtitlesSubtitleItem>,
    selectedOpenSubtitlesFileId: Int?,
    isLoadingOpenSubtitles: Boolean,
    isOpenSubtitlesConfigured: Boolean,
    onOpenSubtitlesSearch: () -> Unit,
    onOpenSubtitlesItemSelected: (OpenSubtitlesSubtitleItem) -> Unit,
) {
    val noneLabel = stringResource(Res.string.compose_player_none)
    val builtInLabel = stringResource(Res.string.compose_player_built_in)
    val openSubtitlesSelected = selectedOpenSubtitlesFileId

    if (isLoadingAddonSubtitles || isLoadingOpenSubtitles) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            NuvioLoadingIndicator(
                color = Color(0xFFE50914),
                modifier = Modifier.size(28.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header-none") { SectionHeader(label = noneLabel) }
        item(key = "none") {
            UnifiedSubtitleItem(
                label = noneLabel,
                isSelected = selectedSubtitleIndex == -1 && selectedAddonSubtitleId == null && openSubtitlesSelected == null,
                isNone = true,
                onSelect = { onBuiltInTrackSelected(-1) },
            )
        }

        item(key = "header-builtin") { SectionHeader(label = builtInLabel) }
        subtitleTracks.forEach { track ->
            val isSelected = track.index == selectedSubtitleIndex && selectedAddonSubtitleId == null && openSubtitlesSelected == null
            item(key = "builtin:${track.index}") {
                UnifiedSubtitleItem(
                    label = localizedTrackDisplayName(track.label, track.language, track.index),
                    isSelected = isSelected,
                    onSelect = { onBuiltInTrackSelected(track.index) },
                )
            }
        }

        item(key = "header-opensubtitles") { SectionHeader(label = stringResource(Res.string.compose_player_opensubtitles_section)) }
        openSubtitlesItems.forEach { sub ->
            item(key = "opensubtitles:${sub.fileId}") {
                val label = languageLabelForCode(sub.languageCode)
                    .takeIf { it.isNotBlank() && it != sub.languageCode }
                    ?: sub.language.ifBlank { sub.languageCode.ifBlank { "?" } }
                val suffix = buildString {
                    if (sub.hearingImpaired) append(" [HI]")
                    if (sub.fromTrusted) append(" \u2605")
                }
                UnifiedSubtitleItem(
                    label = label,
                    secondaryLabel = suffix.ifBlank { null },
                    isSelected = sub.fileId == openSubtitlesSelected,
                    onSelect = { onOpenSubtitlesItemSelected(sub) },
                )
            }
        }

        item(key = "header-addon") { SectionHeader(label = stringResource(Res.string.addon_title)) }
        addonSubtitles.forEach { sub ->
            val isSelected = sub.id == selectedAddonSubtitleId
            item(key = "addon:${sub.id}") {
                UnifiedSubtitleItem(
                    label = sub.display,
                    secondaryLabel = languageLabelForCode(sub.language).takeIf { it.isNotBlank() },
                    isSelected = isSelected && openSubtitlesSelected == null,
                    onSelect = { onAddonSubtitleSelected(sub) },
                )
            }
        }

        if (openSubtitlesItems.isEmpty() && isOpenSubtitlesConfigured) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF252525))
                        .clickable(onClick = onOpenSubtitlesSearch)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(Res.string.compose_player_fetch_subtitles),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedSubtitleItem(
    label: String,
    secondaryLabel: String? = null,
    isSelected: Boolean,
    isNone: Boolean = false,
    onSelect: () -> Unit,
) {
    val bgColor = if (isSelected) Color(0xFF333333) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.White.copy(alpha = if (isNone) 0.5f else 0.8f),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            secondaryLabel?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color(0xFFE50914),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp),
    )
}
