package com.finnvek.knittools.ui.screens.library

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.ui.navigation.IncomingPdf
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Rule
import org.junit.Test

class IncomingPdfSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun restoringAfterPendingPdfChangesUsesTheNewDocumentsName() {
        var pdf = IncomingPdf("file:///first.pdf", "First chart")
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            DisposableEffect(Unit) {
                onDispose { pdf = IncomingPdf("file:///second.pdf", "Second chart") }
            }
            KnitToolsTheme(isDarkTheme = false) {
                IncomingPdfSheet(
                    pdf = pdf,
                    projects = emptyList(),
                    canStartProject = false,
                    proState = ProState(),
                    proAccessReady = false,
                    busy = false,
                    actions = IncomingPdfSheetActions({ _, _ -> }, {}, {}, {}),
                )
            }
        }
        composeRule.onNodeWithText("First chart").performTextReplacement("Edited first chart")
        composeRule.onNodeWithText("Edited first chart").assertExists()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Second chart").assertExists()
        composeRule.onNodeWithText("Edited first chart").assertDoesNotExist()
    }
}
