package com.finnvek.knittools.ui.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTipSession = staticCompositionLocalOf<TipSession?> { null }
val LocalTipRouteKey = staticCompositionLocalOf { "" }
val LocalTipBlocked = staticCompositionLocalOf { false }

/** Estot yhdistetään isäntää ylempänä, jotta myös sen oma vihje pysyy piilossa. */
@Composable
fun TipEnvironment(
    blocked: Boolean = false,
    routeKey: String = LocalTipRouteKey.current,
    content: @Composable () -> Unit,
) {
    val inheritedBlock = LocalTipBlocked.current
    CompositionLocalProvider(
        LocalTipRouteKey provides routeKey,
        LocalTipBlocked provides (inheritedBlock || blocked),
        content = content,
    )
}
