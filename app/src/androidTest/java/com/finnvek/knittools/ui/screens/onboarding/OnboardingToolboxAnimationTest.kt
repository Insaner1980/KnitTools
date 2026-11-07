package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
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
    private val settlingMillis =
        (
            IllustrationDimens.ENTRANCE_BASE_DELAY_MILLIS +
                JourneyTool.entries.size * IllustrationDimens.ENTRANCE_STAGGER_MILLIS +
                IllustrationDimens.ENTRANCE_MILLIS + OnboardingDimens.TOOLBOX_DETAIL_MILLIS
        ).toLong()

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

    @Test
    fun completedDetailDoesNotReplayWhenColumnsChange() {
        val scroll = ScrollState(0)
        val baseLayout = 360.dp to 1f
        val layout = mutableStateOf(baseLayout)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, layout.value.second)) {
                KnitToolsTheme {
                    Column(Modifier.width(layout.value.first).height(260.dp).verticalScroll(scroll)) {
                        JourneyToolbox(CraftType.KNITTING, emptyList())
                    }
                }
            }
        }
        val tile = compose.onNodeWithTag("onboarding_tool_Insights")

        fun revealTile() {
            val top =
                tile
                    .fetchSemanticsNode()
                    .positionInRoot
                    .y
            val targetY = (top + scroll.value).roundToInt()
            compose.runOnIdle { runBlocking { scroll.scrollTo(targetY) } }
            compose.mainClock.advanceTimeByFrame()
        }

        revealTile()
        compose.mainClock.advanceTimeBy(settlingMillis)
        val finished = tile.captureToImage().asAndroidBitmap()
        compose.mainClock.autoAdvance = false
        val entranceMillis =
            (
                IllustrationDimens.ENTRANCE_BASE_DELAY_MILLIS +
                    JourneyTool.Insights.ordinal * IllustrationDimens.ENTRANCE_STAGGER_MILLIS +
                    IllustrationDimens.ENTRANCE_MILLIS
            ).toLong()
        listOf(300.dp to 1f, 360.dp to 1.5f).forEach { changedLayout ->
            compose.runOnIdle { layout.value = changedLayout }
            compose.mainClock.advanceTimeByFrame()
            revealTile()
            compose.mainClock.advanceTimeBy(settlingMillis)
            compose.runOnIdle { layout.value = baseLayout }
            compose.mainClock.advanceTimeByFrame()
            revealTile()
            compose.mainClock.advanceTimeBy(entranceMillis + OnboardingDimens.TOOLBOX_DETAIL_MILLIS / 2L)
            assertTrue(
                "Completed detail replayed after $changedLayout",
                finished.sameAs(tile.captureToImage().asAndroidBitmap()),
            )
            compose.mainClock.advanceTimeBy(settlingMillis)
        }
    }
}
