package com.nuvio.app.features.kitsu.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.util.toOneDecimalString
import com.nuvio.app.features.kitsu.KitsuLibraryItem
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsuEditMediaBottomSheet(
    item: KitsuLibraryItem,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSave: (status: String, progress: Int, rating: Double?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusOptions = listOf("Planned", "Current", "Completed", "On Hold", "Dropped")

    var selectedStatus by remember(item) {
        val initial = when (item.status.lowercase()) {
            "current" -> "Current"
            "completed" -> "Completed"
            "planned" -> "Planned"
            "on_hold" -> "On Hold"
            "dropped" -> "Dropped"
            else -> item.status.lowercase().replaceFirstChar { it.uppercase() }
        }
        mutableStateOf(initial)
    }

    var progress by remember(item) { mutableIntStateOf(item.progress) }
    var isEditingProgress by remember { mutableStateOf(false) }
    var progressInputText by remember { mutableStateOf(progress.toString()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(progress) {
        if (!isEditingProgress) progressInputText = progress.toString()
    }
    LaunchedEffect(isEditingProgress) {
        if (isEditingProgress) focusRequester.requestFocus()
    }

    var score by remember(item) {
        mutableStateOf(item.rating ?: 0.0)
    }

    val sheetBg = Color(0xFF17171D)
    val cardBg = Color(0xFF22222B)
    val buttonDarkBg = Color(0xFF2E2E3A)
    val lavender = Color(0xFFB7B8FF)
    val textPrimary = Color.White
    val textMuted = Color(0xFFA0A0AB)
    val redText = Color(0xFFFF8B8B)

    NuvioModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = textPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        showDragHandle = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(buttonDarkBg, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onDismissRequest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, stringResource(Res.string.action_close), tint = textPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Text(
                text = stringResource(Res.string.tracker_edit_media_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textMuted),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { option ->
                    val isActive = option.equals(selectedStatus, ignoreCase = true)
                    val bgColor by animateColorAsState(
                        targetValue = if (isActive) lavender else buttonDarkBg,
                        animationSpec = tween(200)
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color(0xFF17171D) else textPrimary.copy(alpha = 0.75f),
                        animationSpec = tween(200)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(bgColor)
                            .clickable { selectedStatus = option }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = textColor))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.tracker_edit_progress), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary))
                Text(
                    "$progress / ${item.totalEpisodes ?: "??"}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = textMuted)
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(cardBg, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable { if (progress > 0) progress-- },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Remove, stringResource(Res.string.tracker_edit_progress_decrease), tint = textPrimary, modifier = Modifier.size(24.dp)) }

                Box(
                    modifier = Modifier.weight(1f).height(56.dp).background(cardBg, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable { isEditingProgress = true; progressInputText = progress.toString() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditingProgress) {
                        BasicTextField(
                            value = progressInputText,
                            onValueChange = { newVal ->
                                val filtered = newVal.filter { it.isDigit() }
                                if (filtered.length <= 5) {
                                    progressInputText = filtered
                                    filtered.toIntOrNull()?.let { parsed ->
                                        progress = parsed.coerceIn(0, item.totalEpisodes ?: Int.MAX_VALUE)
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (progressInputText.isEmpty()) progressInputText = progress.toString(); isEditingProgress = false }),
                            singleLine = true,
                            cursorBrush = SolidColor(textPrimary),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).focusRequester(focusRequester)
                        )
                    } else {
                        Text(progress.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary))
                    }
                }

                Box(
                    modifier = Modifier.size(56.dp).background(cardBg, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable { val max = item.totalEpisodes ?: Int.MAX_VALUE; if (progress < max) progress++ },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Add, stringResource(Res.string.tracker_edit_progress_increase), tint = textPrimary, modifier = Modifier.size(24.dp)) }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.tracker_edit_score), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary))
                Text("${score.toOneDecimalString()} / 5", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = textMuted))
            }
            Spacer(Modifier.height(12.dp))

            Slider(
                value = score.toFloat(),
                onValueChange = { score = it.toDouble() },
                valueRange = 0f..5f,
                steps = 49,
                colors = SliderDefaults.colors(thumbColor = lavender, activeTrackColor = lavender, inactiveTrackColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = cardBg, contentColor = redText),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).height(54.dp)
                ) { Text(stringResource(Res.string.action_delete), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = redText)) }

                Button(
                    onClick = {
                        val finalProgress = if (isEditingProgress) progressInputText.toIntOrNull()?.coerceIn(0, item.totalEpisodes ?: Int.MAX_VALUE) ?: progress else progress
                        onSave(selectedStatus, finalProgress, if (score > 0.0) score else null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lavender, contentColor = Color(0xFF17171D)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).height(54.dp)
                ) { Text(stringResource(Res.string.action_save_changes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF17171D))) }
            }
        }
    }
}
