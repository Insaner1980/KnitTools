package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.ui.TestContentSplashRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource

class OnboardingNavigationTest {
    @get:Rule(order = 0)
    val freshInstall =
        object : ExternalResource() {
            override fun before() {
                val app = ApplicationProvider.getApplicationContext<App>()
                check(ProPolicyTestControl.isActive && app.packageName == "com.finnvek.knittools.activepolicytest")
                runBlocking(Dispatchers.IO) {
                    app.database.get().clearAllTables()
                    check(OnboardingStateRepository(app).saveDraft(OnboardingDraft("navigation-test")))
                }
            }
        }

    @get:Rule(order = 1)
    val splash = TestContentSplashRule()

    @get:Rule(order = 2)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createThenContinueOpensRealProjectAndDoesNotReturnToOnboarding() {
        check(ProPolicyTestControl.isActive)
        setTestProStatus(ProStatus.TRIAL_NOT_STARTED)
        compose.waitUntil(15_000) {
            compose
                .onAllNodesWithTag(
                    "onboarding_next",
                ).fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        nextTo(R.string.journey_craft_title)
        compose.onNodeWithTag("onboarding_craft_knitting").performClick()
        waitForStep(R.string.journey_needs_title)
        nextTo(R.string.journey_counter_title)
        compose.onNodeWithTag("onboarding_demo_plus").performScrollTo().performClick()
        nextTo(R.string.journey_project_title)
        compose.onNodeWithTag("onboarding_next").assertIsNotEnabled()
        compose.onNodeWithTag("onboarding_project_name").performTextInput("Onboarding test")
        compose.onNodeWithTag("onboarding_next").performClick()
        waitForText(R.string.journey_ready_title)
        // Keskeytynyt Ready-vaihe palautuu Readyyn eikä luo projektia uudelleen.
        compose.activityRule.scenario.recreate()
        waitForText(R.string.journey_ready_title)
        // Luonnos latautuu uudelleenluonnin jälkeen; painike on käytössä vasta latauksen jälkeen.
        compose.waitUntil(15_000) {
            compose
                .onAllNodes(hasTestTag("onboarding_next") and isEnabled())
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.waitUntil(15_000) {
            compose
                .onAllNodes(
                    hasTestTag("onboarding_continue_free") and isEnabled(),
                ).fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        compose.onNodeWithTag("onboarding_continue_free").assertIsDisplayed().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isEmpty() }
        val undo = compose.activity.getString(R.string.counter_undo_last_change)
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(undo).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(undo).assertIsNotEnabled()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.counter_add_row)).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasContentDescription(undo) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) {
            compose
                .onAllNodes(
                    hasContentDescription(undo) and isEnabled(),
                ).fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        compose.onNodeWithContentDescription(undo).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasContentDescription(undo) and isNotEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Onboarding test").onFirst().assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithTag("onboarding").assertDoesNotExist()
        compose.onAllNodesWithText("Onboarding test").onFirst().assertIsDisplayed()
    }

    private fun nextTo(title: Int) {
        compose.onNodeWithTag("onboarding_next").performClick()
        waitForStep(title)
    }

    private fun waitForText(text: Int) {
        compose.waitUntil(15_000) {
            compose
                .onAllNodesWithText(compose.activity.getString(text))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun waitForStep(title: Int) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(compose.activity.getString(title)).fetchSemanticsNodes().isNotEmpty() &&
                compose
                    .onAllNodes(
                        hasText(compose.activity.getString(R.string.journey_skip)) and isEnabled(),
                    ).fetchSemanticsNodes()
                    .isNotEmpty()
        }
    }
}
