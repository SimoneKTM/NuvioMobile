package com.nuvio.app.features.kitsu

import androidx.compose.runtime.Composable
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.library_kitsu_status_completed
import nuvio.composeapp.generated.resources.library_kitsu_status_current
import nuvio.composeapp.generated.resources.library_kitsu_status_dropped
import nuvio.composeapp.generated.resources.library_kitsu_status_on_hold
import nuvio.composeapp.generated.resources.library_kitsu_status_planned
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun kitsuSectionDisplayTitle(type: String): String = when {
    type.equals("current", ignoreCase = true) ||
        type.equals("in visione", ignoreCase = true) ||
        type.equals("in corso", ignoreCase = true) ->
        stringResource(Res.string.library_kitsu_status_current)

    type.equals("completed", ignoreCase = true) ||
        type.equals("completato", ignoreCase = true) ->
        stringResource(Res.string.library_kitsu_status_completed)

    type.equals("planned", ignoreCase = true) ||
        type.equals("pianificato", ignoreCase = true) ->
        stringResource(Res.string.library_kitsu_status_planned)

    type.equals("on hold", ignoreCase = true) ||
        type.equals("in pausa", ignoreCase = true) ->
        stringResource(Res.string.library_kitsu_status_on_hold)

    type.equals("dropped", ignoreCase = true) ||
        type.equals("abbandonato", ignoreCase = true) ->
        stringResource(Res.string.library_kitsu_status_dropped)

    else -> type.replaceFirstChar { it.uppercase() }
}

fun kitsuStatusDisplayTitle(type: String): String = runBlocking {
    when {
        type.equals("current", ignoreCase = true) ||
            type.equals("in visione", ignoreCase = true) ||
            type.equals("in corso", ignoreCase = true) ->
            getString(Res.string.library_kitsu_status_current)

        type.equals("completed", ignoreCase = true) ||
            type.equals("completato", ignoreCase = true) ->
            getString(Res.string.library_kitsu_status_completed)

        type.equals("planned", ignoreCase = true) ||
            type.equals("pianificato", ignoreCase = true) ->
            getString(Res.string.library_kitsu_status_planned)

        type.equals("on hold", ignoreCase = true) ||
            type.equals("in pausa", ignoreCase = true) ->
            getString(Res.string.library_kitsu_status_on_hold)

        type.equals("dropped", ignoreCase = true) ||
            type.equals("abbandonato", ignoreCase = true) ->
            getString(Res.string.library_kitsu_status_dropped)

        else -> type.replaceFirstChar { it.uppercase() }
    }
}
