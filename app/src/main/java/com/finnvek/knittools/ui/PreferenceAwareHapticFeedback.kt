package com.finnvek.knittools.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

internal class PreferenceAwareHapticFeedback(
    private val enabled: Boolean,
    private val delegate: HapticFeedback,
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (enabled) delegate.performHapticFeedback(hapticFeedbackType)
    }
}

@Composable
internal fun ProvidePreferenceAwareHapticFeedback(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val platformHapticFeedback = LocalHapticFeedback.current
    val preferenceAwareHapticFeedback =
        remember(enabled, platformHapticFeedback) {
            PreferenceAwareHapticFeedback(enabled, platformHapticFeedback)
        }
    CompositionLocalProvider(
        LocalHapticFeedback provides preferenceAwareHapticFeedback,
        content = content,
    )
}
