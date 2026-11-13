package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.compositeOver
import com.finnvek.knittools.ui.components.PROJECT_PROGRESS_TRACK_ALPHA
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectListContrastTest {
    @Test
    fun `progress fill clears the graphical contrast minimum against its track`() {
        val darkContainers = listOf(SurfaceHigh, ActionContainer, Primary.copy(alpha = 0.07f).compositeOver(Background))
        val lightContainers =
            listOf(LightCardContainer, LightActionContainer, Primary.copy(alpha = 0.07f).compositeOver(LightBackground))
        darkContainers.forEach { container ->
            val track = TextPrimary.copy(alpha = PROJECT_PROGRESS_TRACK_ALPHA).compositeOver(container)
            assertTrue(contrastRatio(PrimaryReadable, track) >= MINIMUM_GRAPHICAL_CONTRAST)
        }
        lightContainers.forEach { container ->
            val track = LightTextPrimary.copy(alpha = PROJECT_PROGRESS_TRACK_ALPHA).compositeOver(container)
            assertTrue(contrastRatio(LightPrimaryReadable, track) >= MINIMUM_GRAPHICAL_CONTRAST)
        }
    }

    @Test
    fun `readable accent supports small text on background and buttons`() {
        listOf(Background, ActionContainer).forEach { assertTrue(contrastRatio(PrimaryReadable, it) >= 4.5) }
        listOf(LightBackground, LightActionContainer).forEach {
            assertTrue(
                contrastRatio(LightPrimaryReadable, it) >= 4.5,
            )
        }
    }

    private companion object {
        const val MINIMUM_GRAPHICAL_CONTRAST = 3.0
    }
}
