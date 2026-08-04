package com.nuvio.app.core.util

import kotlin.math.roundToInt

fun Double.toOneDecimalString(): String {
    if (!isFinite()) return toString()
    val scaled = (this * 10).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val value = if (scaled < 0) -scaled else scaled
    return "${sign}${value / 10}.${value % 10}"
}