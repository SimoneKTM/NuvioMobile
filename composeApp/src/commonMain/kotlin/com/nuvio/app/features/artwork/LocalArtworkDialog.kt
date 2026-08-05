package com.nuvio.app.features.artwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Panorama
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LocalArtworkDialog(
    videoId: String,
    mediaType: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        LocalArtworkRepository.initialize()
    }

    val state by LocalArtworkRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(ArtworkType.POSTER) }

    val existingArtwork = LocalArtworkRepository.getArtworkForVideo(videoId, mediaType)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.artwork_customize_title))
        },
        text = {
            Column(modifier = modifier.fillMaxWidth()) {
                Text(
                    stringResource(Res.string.artwork_select_type_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ArtworkType.entries) { type ->
                        ArtworkTypeChip(
                            type = type,
                            isSelected = type == selectedType,
                            hasExisting = existingArtwork.any { it.artworkType == type },
                            onClick = { selectedType = type },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val currentArtwork = existingArtwork.find { it.artworkType == selectedType }
                if (currentArtwork != null) {
                    Text(
                        stringResource(Res.string.artwork_current_type, selectedType.value),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        coil3.compose.AsyncImage(
                            model = currentArtwork.localPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = {
                                LocalArtworkRepository.removeArtwork(videoId, mediaType, selectedType)
                            },
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.action_remove))
                        }
                    }
                } else {
                    Text(
                        stringResource(Res.string.artwork_no_custom_set, selectedType.value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        scope.launch {
                            val result = LocalArtworkRepository.pickArtworkFromGallery()
                            if (result.wasSuccessful && result.localPath != null) {
                                LocalArtworkRepository.addArtwork(
                                    videoId = videoId,
                                    mediaType = mediaType,
                                    artworkType = selectedType,
                                    localPath = result.localPath,
                                )
                            }
                        }
                    },
                ) {
                    Icon(
                        Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.artwork_choose_from_gallery))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_done))
            }
        },
    )
}

@Composable
private fun ArtworkTypeChip(
    type: ArtworkType,
    isSelected: Boolean,
    hasExisting: Boolean,
    onClick: () -> Unit,
) {
    val (icon, label) = when (type) {
        ArtworkType.POSTER -> Icons.Rounded.Image to stringResource(Res.string.artwork_type_poster)
        ArtworkType.BACKGROUND -> Icons.Rounded.Panorama to stringResource(Res.string.artwork_type_background)
        ArtworkType.LOGO -> Icons.Rounded.PhotoLibrary to stringResource(Res.string.artwork_type_logo)
        ArtworkType.THUMBNAIL -> Icons.Rounded.Movie to stringResource(Res.string.artwork_type_thumbnail)
        ArtworkType.CLEARART -> Icons.Rounded.Movie to stringResource(Res.string.artwork_type_clearart)
        ArtworkType.BANNER -> Icons.Rounded.Panorama to stringResource(Res.string.artwork_type_banner)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
            )
            if (hasExisting) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.AddPhotoAlternate,
                    contentDescription = stringResource(Res.string.artwork_has_custom),
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
