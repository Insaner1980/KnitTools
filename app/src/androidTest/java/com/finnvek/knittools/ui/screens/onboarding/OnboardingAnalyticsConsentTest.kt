package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class OnboardingAnalyticsConsentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun consentIsOptionalOffByDefaultAndCanBeRevoked() {
        val enabled = mutableStateOf(false)
        var continued = 0
        val label = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.usage_analytics)
        compose.setContent {
            KnitToolsTheme {
                OnboardingScreen(
                    draft = OnboardingDraft("synthetic"),
                    price = OnboardingPriceState(ready = true),
                    busy = false,
                    messageRes = null,
                    actions = OnboardingActions(next = { continued++ }),
                    usageAnalyticsEnabled = enabled.value,
                    onUsageAnalyticsChanged = { enabled.value = it },
                )
            }
        }
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.runOnIdle {
            assertEquals(1, continued)
            assertFalse(enabled.value)
        }
        val toggle = compose.onNodeWithContentDescription(label)
        toggle
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
        toggle.performClick().assertIsOff()
    }
}
