package com.finnvek.knittools.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceAwareHapticFeedbackTest {
    @Test
    fun `enabled preference delegates haptic feedback`() {
        val delegate = RecordingHapticFeedback()
        val feedback = PreferenceAwareHapticFeedback(enabled = true, delegate = delegate)

        feedback.performHapticFeedback(HapticFeedbackType.LongPress)

        assertEquals(listOf(HapticFeedbackType.LongPress), delegate.performedTypes)
    }

    @Test
    fun `disabled preference suppresses haptic feedback`() {
        val delegate = RecordingHapticFeedback()
        val feedback = PreferenceAwareHapticFeedback(enabled = false, delegate = delegate)

        feedback.performHapticFeedback(HapticFeedbackType.LongPress)

        assertEquals(emptyList<HapticFeedbackType>(), delegate.performedTypes)
    }

    private class RecordingHapticFeedback : HapticFeedback {
        val performedTypes = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performedTypes += hapticFeedbackType
        }
    }
}
