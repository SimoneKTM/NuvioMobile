package com.nuvio.app.features.artwork

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LocalArtworkSettingsContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) { delay(300) }
        withContext(Dispatchers.Default) {
            LocalArtworkRepository.initialize()
        }
    }

    val state by LocalArtworkRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val totalArtwork = state.artworkMap.values.flatten().size
    val totalVideos = state.artworkMap.size

    LazyColumn(modifier = modifier.padding(16.dp)) {
        item {
            Spacer(Modifier.height(8.dp))

            NuvioSectionLabel(stringResource(Res.string.artwork_section_local_artwork))

            Spacer(Modifier.height(8.dp))

            NuvioSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.artwork_storage_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(Res.string.artwork_storage_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NuvioInfoBadge(text = stringResource(Res.string.artwork_count_badge, totalArtwork))
                        NuvioInfoBadge(text = stringResource(Res.string.artwork_videos_badge, totalVideos))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NuvioSectionLabel(stringResource(Res.string.artwork_how_to_use))

            Spacer(Modifier.height(8.dp))

            NuvioSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.artwork_how_to_use_intro),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.artwork_step_1),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(Res.string.artwork_step_2),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(Res.string.artwork_step_3),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(Res.string.artwork_step_4),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(Res.string.artwork_step_5),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            NuvioSectionLabel(stringResource(Res.string.artwork_supported_types))

            Spacer(Modifier.height(8.dp))

            ArtworkType.entries.forEach { type ->
                ArtworkTypeInfoRow(type = type)
                HorizontalDivider()
            }

            Spacer(Modifier.height(16.dp))

            if (totalArtwork > 0) {
                NuvioSectionLabel(stringResource(Res.string.artwork_manage_all))

                Spacer(Modifier.height(8.dp))

                state.artworkMap.forEach { (key, artworks) ->
                    val (mediaType, videoId) = key.split(":", limit = 2)

                    NuvioSurfaceCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        videoId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                    )
                                    Text(
                                        stringResource(Res.string.artwork_count_line, mediaType, artworks.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        scope.launch(Dispatchers.Default) {
                                            LocalArtworkRepository.clearArtworkForVideo(videoId, mediaType)
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = stringResource(Res.string.action_clear),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(Res.string.action_clear), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ArtworkTypeInfoRow(
    type: ArtworkType,
) {
    val description = when (type) {
        ArtworkType.POSTER -> stringResource(Res.string.artwork_type_poster_desc)
        ArtworkType.BACKGROUND -> stringResource(Res.string.artwork_type_background_desc)
        ArtworkType.LOGO -> stringResource(Res.string.artwork_type_logo_desc)
        ArtworkType.THUMBNAIL -> stringResource(Res.string.artwork_type_thumbnail_desc)
        ArtworkType.CLEARART -> stringResource(Res.string.artwork_type_clearart_desc)
        ArtworkType.BANNER -> stringResource(Res.string.artwork_type_banner_desc)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text(
            type.value.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp),
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
