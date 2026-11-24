package com.finnvek.knittools.ui.screens.guide

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GuideScreenTest {
    @get:Rule val compose = createComposeRule()
    private val topic = mutableStateOf<GuideTopic?>(null)
    private val proState = mutableStateOf<ProState?>(null)
    private val selectedTopics = mutableListOf<GuideTopic>()
    private val actions = mutableListOf<GuideAction>()
    private var backRequests = 0

    @Test
    fun rootShowsEveryTopicAndInvokesTopicAndBackActions() {
        render()
        compose.onNodeWithText(text(R.string.guide_intro)).assertIsDisplayed()
        GuideTopic.entries.forEach {
            compose.onNodeWithText(text(it.title)).performScrollTo().performClick()
        }
        assertEquals(GuideTopic.entries, selectedTopics)
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        assertEquals(1, backRequests)
    }

    @Test
    fun topicsShowBundledSectionsAndDispatchTheirDeclaredActions() {
        topic.value = GuideTopic.CONTINUE
        render()
        GuideTopic.entries.forEach { current ->
            compose.runOnIdle { topic.value = current }
            compose.onNodeWithText(text(current.title)).assertIsDisplayed()
            compose.onNodeWithText(text(current.subtitle)).assertIsDisplayed()
            current.items.forEach { item ->
                compose.onNodeWithText(text(item.title)).assertExists()
                compose.onNodeWithText(text(item.body)).assertExists()
            }
            current.items.mapNotNull { it.action }.indices.forEach { index ->
                compose.onAllNodesWithText(text(R.string.guide_try_it))[index].performScrollTo().performClick()
            }
        }
        assertEquals(GuideTopic.entries.flatMap { it.items.mapNotNull(GuideItem::action) }, actions)
    }

    @Test
    fun proMarkersWaitForStateDescribeTrialAndLockAndHideForPaidAccess() {
        topic.value = GuideTopic.YARN
        render()
        compose.onNodeWithText(text(R.string.guide_yarn_my_yarn_body)).assertExists()
        compose.onNodeWithText(text(R.string.pro_badge)).assertDoesNotExist()
        compose.runOnIdle { proState.value = ProState(status = ProStatus.TRIAL_ACTIVE) }
        compose
            .onNodeWithContentDescription(
                text(R.string.pro_badge_trial_description),
            ).performScrollTo()
            .assertIsDisplayed()
        compose.runOnIdle { proState.value = ProState(status = ProStatus.TRIAL_EXPIRED) }
        compose.onNodeWithContentDescription(text(R.string.pro_badge_locked_description)).assertIsDisplayed()
        compose.runOnIdle { proState.value = ProState(status = ProStatus.PRO_PURCHASED) }
        compose.onNodeWithText(text(R.string.pro_badge)).assertDoesNotExist()
    }

    private fun render() {
        compose.setContent {
            KnitToolsTheme {
                GuideScreen(
                    topic = topic.value,
                    proState = proState.value,
                    onBack = { backRequests++ },
                    onTopicSelected = selectedTopics::add,
                    onAction = actions::add,
                )
            }
        }
    }

    private fun text(resource: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
}
