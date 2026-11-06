package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.ui.theme.IllustrationDimens
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.ui.theme.OnboardingDimens
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class OnboardingToolboxAnimationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun offscreenDetailPlaysOnFirstScrollIntoViewAndDoesNotReplay() {
        val scroll = ScrollState(0)
        compose.setContent {
            KnitToolsTheme {
                Column(Modifier.width(300.dp).height(260.dp).verticalScroll(scroll)) {
                    JourneyToolbox(CraftType.KNITTING, emptyList())
                }
            }
        }
        val settlingMillis =
            (
                IllustrationDimens.ENTRANCE_BASE_DELAY_MILLIS +
                    JourneyTool.entries.size * IllustrationDimens.ENTRANCE_STAGGER_MILLIS +
                    IllustrationDimens.ENTRANCE_MILLIS + OnboardingDimens.TOOLBOX_DETAIL_MILLIS
            ).toLong()
        compose.waitForIdle()
        val tile = compose.onNodeWithTag("onboarding_tool_Insights")
        val targetY =
            tile
                .fetchSemanticsNode()
                .positionInRoot
                .y
                .roundToInt()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { runBlocking { scroll.scrollTo(targetY) } }
        compose.mainClock.advanceTimeByFrame()
        val firstVisible = tile.captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(settlingMillis)
        val finished = tile.captureToImage().asAndroidBitmap()
        assertFalse("Detail finished before its tile became visible", firstVisible.sameAs(finished))

        compose.runOnIdle { runBlocking { scroll.scrollTo(0) } }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { runBlocking { scroll.scrollTo(targetY) } }
        compose.mainClock.advanceTimeBy(OnboardingDimens.TOOLBOX_DETAIL_MILLIS / 2L)
        assertTrue("A completed detail replayed", finished.sameAs(tile.captureToImage().asAndroidBitmap()))
    }
}
