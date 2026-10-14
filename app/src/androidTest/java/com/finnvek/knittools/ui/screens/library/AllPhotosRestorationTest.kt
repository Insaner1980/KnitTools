package com.finnvek.knittools.ui.screens.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AllPhotosRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun restoredProjectFilterWaitsForPhotosAndClearsOnlyAfterAuthoritativeRemoval() {
        val updates = MutableSharedFlow<List<ProgressPhoto>>(extraBufferCapacity = 1)
        val projects = listOf(CounterProject(id = 7L, name = "Alpha"), CounterProject(id = 8L, name = "Beta"))
        val photos =
            listOf(
                ProgressPhoto(id = 1L, projectId = 7L, photoUri = "", rowNumber = 1, note = "Alpha photo"),
                ProgressPhoto(id = 2L, projectId = 8L, photoUri = "", rowNumber = 2, note = "Beta photo"),
            )
        val allProjects = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.all_projects)
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            val observed by produceState<List<ProgressPhoto>?>(null) { updates.collect { value = it } }
            KnitToolsTheme(isDarkTheme = false) {
                AllPhotosScreen(
                    state =
                        AllPhotosState(observed.orEmpty(), projects, false, emptySet(), isLoading = observed == null),
                    actions = AllPhotosActions({}, {}, {}, {}, {}, {}, {}),
                )
            }
        }
        composeRule.runOnIdle { assertTrue(updates.tryEmit(photos)) }
        composeRule.onNodeWithText(allProjects).performClick()
        composeRule.onNode(hasText("Alpha") and hasAnyAncestor(isPopup())).performClick()
        composeRule.onNodeWithText("Alpha photo").assertExists()
        composeRule.onNodeWithText("Beta photo").assertDoesNotExist()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertTrue(updates.tryEmit(photos)) }
        composeRule.onNodeWithText("Alpha photo").assertExists()
        composeRule.onNodeWithText("Beta photo").assertDoesNotExist()

        composeRule.runOnIdle { assertTrue(updates.tryEmit(photos.filter { it.projectId == 8L })) }
        composeRule.onNodeWithText(allProjects).assertExists()
        composeRule.onNodeWithText("Beta photo").assertExists()
    }
}
