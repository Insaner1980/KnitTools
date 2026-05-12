package com.finnvek.knittools.ui.navigation

internal fun Long.toPositiveRouteIdOrNull(): Long? = takeIf { it > 0L }

internal fun Int.toPositiveRouteIdOrNull(): Int? = takeIf { it > 0 }
