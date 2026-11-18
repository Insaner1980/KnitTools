package com.finnvek.knittools.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceAwareHapticFeedbackTest {
    @Test
    fun `enabled preference delegates haptic feedback`() {
        val delegate = RecordingHapticFeedback()
        val feedback = PreferenceAwareHapticFeedback(enabled = { true }, delegate = delegate)

        feedback.performHapticFeedback(HapticFeedbackType.LongPress)

        assertEquals(listOf(HapticFeedbackType.LongPress), delegate.performedTypes)
    }

    @Test
    fun `disabled preference suppresses haptic feedback`() {
        val delegate = RecordingHapticFeedback()
        var enabled = false
        val feedback = PreferenceAwareHapticFeedback(enabled = { enabled }, delegate = delegate)

        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        enabled = true
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        enabled = false
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)

        assertEquals(listOf(HapticFeedbackType.TextHandleMove), delegate.performedTypes)
    }

    private class RecordingHapticFeedback : HapticFeedback {
        val performedTypes = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performedTypes += hapticFeedbackType
        }
    }
}
