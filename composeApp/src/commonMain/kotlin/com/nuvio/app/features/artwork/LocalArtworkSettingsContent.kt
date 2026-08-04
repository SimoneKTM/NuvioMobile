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

            NuvioSectionLabel("Local Artwork")

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
                                "Custom Artwork Storage",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Manage locally stored posters, backgrounds, and logos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NuvioInfoBadge(text = "$totalArtwork artworks")
                        NuvioInfoBadge(text = "$totalVideos videos")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NuvioSectionLabel("How to use")

            Spacer(Modifier.height(8.dp))

            NuvioSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "To customize artwork for a movie, TV show, or anime:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Open the detail page for any content",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "2. Tap the artwork button (usually near the poster)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "3. Select artwork type: Poster, Background, Logo, etc.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "4. Choose an image from your device gallery",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "5. The custom artwork will be used across the app",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            NuvioSectionLabel("Supported Artwork Types")

            Spacer(Modifier.height(8.dp))

            ArtworkType.entries.forEach { type ->
                ArtworkTypeInfoRow(type = type)
                HorizontalDivider()
            }

            Spacer(Modifier.height(16.dp))

            if (totalArtwork > 0) {
                NuvioSectionLabel("Manage All Artwork")

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
                                        "$mediaType - ${artworks.size} artworks",
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
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear", color = MaterialTheme.colorScheme.error)
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
        ArtworkType.POSTER -> "Main poster image displayed in catalogs and search"
        ArtworkType.BACKGROUND -> "Background/backdrop image shown on detail pages"
        ArtworkType.LOGO -> "Logo overlay shown on hero sections"
        ArtworkType.THUMBNAIL -> "Thumbnail image for episodes and continue watching"
        ArtworkType.CLEARART -> "Clear artwork without background"
        ArtworkType.BANNER -> "Banner image for wide layouts"
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
