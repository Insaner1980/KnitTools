package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectsStartStateTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun welcomePromotionUsesLoadedEntitlementAndLinkOnlyNavigates() {
        val ready = mutableStateOf(false)
        val state = mutableStateOf(ProState())
        val compact = mutableStateOf(false)
        var navigations = 0
        var dismissals = 0
        var creations = 0
        compose.setContent {
            KnitToolsTheme {
                ProjectsStartState(
                    compact.value,
                    CraftType.KNITTING,
                    { creations++ },
                    { dismissals++ },
                    {},
                    {},
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    proState = state.value,
                    proAccessReady = ready.value,
                    onOpenPro = { navigations++ },
                )
            }
        }
        val link = context.getString(R.string.welcome_learn_pro)
        compose.onNodeWithText(link).assertDoesNotExist()
        compose.runOnIdle { ready.value = true }
        compose.onNodeWithText(context.getString(R.string.welcome_free_pro_trial)).assertExists()
        compose.onNodeWithText(link).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, navigations)
            assertEquals(0, dismissals)
            assertEquals(0, creations)
        }
        listOf(ProStatus.TRIAL_ACTIVE, ProStatus.TRIAL_EXPIRED).forEach { status ->
            compose.runOnIdle { state.value = ProState(status) }
            compose.onNodeWithText(context.getString(R.string.welcome_free_pro_general)).assertExists()
            compose.onNodeWithText(context.getString(R.string.welcome_free_pro_trial)).assertDoesNotExist()
        }
        compose.runOnIdle { state.value = ProState(ProStatus.PRO_PURCHASED) }
        compose.onNodeWithText(link).assertDoesNotExist()
        compose.runOnIdle {
            state.value = ProState()
            compact.value = true
        }
        compose.onNodeWithText(link).assertDoesNotExist()
    }
}
