package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SubtitleStylePanel(
    style: SubtitleStyleState,
    subtitleDelayMs: Int,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    isCompact: Boolean,
    onStyleChanged: (SubtitleStyleState) -> Unit,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onAutoSyncCapture: () -> Unit,
    onAutoSyncCueSelected: (SubtitleSyncCue) -> Unit,
    onAutoSyncReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StyleStepper(
                label = stringResource(Res.string.compose_player_font_size),
                value = "${style.fontSizeSp}",
                onMinus = { onStyleChanged(style.copy(fontSizeSp = (style.fontSizeSp - 2).coerceAtLeast(12))) },
                onPlus = { onStyleChanged(style.copy(fontSizeSp = (style.fontSizeSp + 2).coerceAtMost(40))) },
                modifier = Modifier.weight(1f),
            )
            StyleStepper(
                label = stringResource(Res.string.compose_player_bottom_offset),
                value = "${style.bottomOffset}",
                onMinus = { onStyleChanged(style.copy(bottomOffset = (style.bottomOffset - 5).coerceAtLeast(0))) },
                onPlus = { onStyleChanged(style.copy(bottomOffset = (style.bottomOffset + 5).coerceAtMost(200))) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToggleChip(
                label = stringResource(Res.string.compose_player_outline),
                selected = style.outlineEnabled,
                onClick = { onStyleChanged(style.copy(outlineEnabled = !style.outlineEnabled)) },
                modifier = Modifier.weight(1f),
            )
            ToggleChip(
                label = stringResource(Res.string.compose_player_bold),
                selected = style.bold,
                onClick = { onStyleChanged(style.copy(bold = !style.bold)) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_color),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SubtitleColorSwatches.forEach { color ->
                    val isSelected = style.textColor == color
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (color.alpha == 0f) Color(0xFF333333) else color)
                            .border(
                                2.dp,
                                if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.15f),
                                CircleShape,
                            )
                            .clickable { onStyleChanged(style.copy(textColor = color)) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_text_opacity),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentAlphaPercent = (style.textColor.alpha * 100f).roundToInt().coerceIn(0, 100)
                StepButton(
                    icon = Icons.Rounded.Remove,
                    onClick = {
                        val newAlpha = (currentAlphaPercent - 10).coerceAtLeast(0) / 100f
                        onStyleChanged(style.copy(textColor = style.textColor.copy(alpha = newAlpha)))
                    },
                )
                Text(
                    text = "$currentAlphaPercent%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                )
                StepButton(
                    icon = Icons.Rounded.Add,
                    onClick = {
                        val newAlpha = (currentAlphaPercent + 10).coerceAtMost(100) / 100f
                        onStyleChanged(style.copy(textColor = style.textColor.copy(alpha = newAlpha)))
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_outline_color),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SubtitleColorSwatches.forEach { color ->
                    val isSelected = style.outlineColor == color
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (color.alpha == 0f) Color(0xFF333333) else color)
                            .border(
                                2.dp,
                                if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.15f),
                                CircleShape,
                            )
                            .clickable { onStyleChanged(style.copy(outlineColor = color)) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_subtitle_delay),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton(
                    icon = Icons.Rounded.Remove,
                    onClick = {
                        onSubtitleDelayChanged((subtitleDelayMs - SUBTITLE_DELAY_STEP_MS).coerceAtLeast(SUBTITLE_DELAY_MIN_MS))
                    },
                )
                Text(
                    text = formatSubtitleDelay(subtitleDelayMs),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp),
                )
                StepButton(
                    icon = Icons.Rounded.Add,
                    onClick = {
                        onSubtitleDelayChanged((subtitleDelayMs + SUBTITLE_DELAY_STEP_MS).coerceAtMost(SUBTITLE_DELAY_MAX_MS))
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF333333))
                    .clickable(onClick = onSubtitleDelayReset)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.compose_player_reset),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StyleStepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StepButton(icon = Icons.Rounded.KeyboardArrowDown, onClick = onMinus)
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp),
            )
            StepButton(icon = Icons.Rounded.KeyboardArrowUp, onClick = onPlus)
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF333333) else Color(0xFF252525))
            .border(
                1.dp,
                if (selected) Color(0xFFE50914).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF333333))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun formatSubtitleDelay(delayMs: Int): String {
    val sign = if (delayMs >= 0) "+" else "-"
    val absMs = abs(delayMs)
    val seconds = absMs / 1000
    val millis = absMs % 1000
    return "$sign$seconds.${millis.toString().padStart(3, '0').take(2)}s"
}
