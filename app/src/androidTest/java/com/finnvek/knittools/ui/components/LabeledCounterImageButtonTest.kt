package com.finnvek.knittools.ui.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.ui.theme.ProjectListDimens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LabeledCounterImageButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pillExposesOneAccessibleButtonThatActivatesThroughTheClickAction() {
        var clicks = 0
        composeRule.setContent {
            KnitToolsTheme {
                LabeledCounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    label = "New Project",
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = { clicks += 1 },
                )
            }
        }

        // Ruudunlukija, Switch Access ja Voice Access aktivoivat pilleriä OnClick-toiminnolla, eivät kosketuksella.
        composeRule
            .onNodeWithContentDescription("New Project")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle { assertEquals(1, clicks) }
        // Näkyvä teksti ei ole erillinen solmu, joten ruudunlukija kuulee nimen vain kerran.
        composeRule.onAllNodesWithText("New Project").assertCountEquals(0)
    }

    @Test
    fun disabledPillKeepsItsButtonButReportsDisabled() {
        composeRule.setContent {
            KnitToolsTheme {
                LabeledCounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    label = "New Project",
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = {},
                    enabled = false,
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("New Project")
            .assert(hasClickAction())
            .assertIsNotEnabled()
    }
}
