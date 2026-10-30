package com.finnvek.knittools.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.navigation.IncomingPdf
import com.finnvek.knittools.ui.screens.library.IncomingPdfSheet
import com.finnvek.knittools.ui.screens.library.IncomingPdfSheetActions
import com.finnvek.knittools.ui.screens.library.MyYarnActions
import com.finnvek.knittools.ui.screens.library.MyYarnScreen
import com.finnvek.knittools.ui.screens.library.MyYarnState
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProPresentationConsumersTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun requirePolicyProfile() {
        check(ProPolicyTestControl.isActive)
    }

    @Test
    fun incomingPdfKeepsItsControlGateAndShowsConditionalTrialMarker() {
        val pro = mutableStateOf(ProState())
        val ready = mutableStateOf(false)
        val allowed = mutableStateOf(true)
        val projects = mutableStateOf(emptyList<CounterProject>())
        var created = 0
        compose.setContent {
            KnitToolsTheme {
                IncomingPdfSheet(
                    pdf = IncomingPdf("file:///example.pdf", "Example"),
                    projects = projects.value,
                    canStartProject = allowed.value,
                    proState = pro.value,
                    proAccessReady = ready.value,
                    busy = false,
                    actions = IncomingPdfSheetActions({ _, _ -> }, { created++ }, {}, {}),
                )
            }
        }

        val title = context.getString(R.string.incoming_pdf_new_project)
        val locked = context.getString(R.string.pro_badge_locked_description)
        val trial = context.getString(R.string.pro_badge_trial_description)
        compose.onNodeWithText(title).assertIsEnabled().performClick()
        compose.onNodeWithContentDescription(locked).assertDoesNotExist()
        compose.onNodeWithContentDescription(trial).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, created)
            ready.value = true
            allowed.value = false
            projects.value = listOf(CounterProject(id = 1L, name = "Existing"))
        }
        compose.onNodeWithText(title).assertIsNotEnabled()
        compose.onNodeWithContentDescription(locked).assertExists()

        compose.runOnIdle {
            pro.value = ProState(status = ProStatus.TRIAL_ACTIVE)
            allowed.value = true
        }
        compose.onNodeWithContentDescription(trial).assertExists()
        compose.onNodeWithText(title).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, created) }
    }

    @Test
    fun myYarnPillMergesItsMarkerDescriptionAndHidesItUntilReady() {
        val state =
            mutableStateOf(
                MyYarnState(
                    cards = emptyList(),
                    activeProjectNames = emptyMap(),
                    isSelectMode = false,
                    selectedYarnIds = emptySet(),
                    canCreateYarnCard = false,
                    proState = ProState(),
                    proAccessReady = true,
                ),
            )
        compose.setContent {
            KnitToolsTheme {
                MyYarnScreen(
                    state = state.value,
                    actions = MyYarnActions({}, { false }, { false }, {}, {}, {}, {}, {}, {}, {}),
                )
            }
        }
        val label = context.getString(R.string.add_yarn_to_my_yarn)
        val locked = context.getString(R.string.pro_badge_locked_description)
        val trial = context.getString(R.string.pro_badge_trial_description)
        compose
            .onNodeWithContentDescription(label)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(label, locked)))
        compose.runOnIdle {
            state.value =
                state.value.copy(proState = ProState(status = ProStatus.TRIAL_ACTIVE), canCreateYarnCard = true)
        }
        compose
            .onNodeWithContentDescription(label)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(label, trial)))
        compose.runOnIdle { state.value = state.value.copy(proAccessReady = false) }
        compose
            .onNodeWithContentDescription(label)
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(label)))
        compose.onNodeWithContentDescription(trial).assertDoesNotExist()
    }
}
