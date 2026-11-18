package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class SheetTitleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headingRemainsNonInteractiveAndUpdatesItsAccessibleText() {
        val title = mutableStateOf("Select project")
        composeRule.setContent {
            MaterialTheme {
                SheetTitle(text = title.value, modifier = Modifier.testTag("sheet_title"))
            }
        }

        val heading = composeRule.onNodeWithTag("sheet_title")
        heading.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        heading.assertHasNoClickAction()
        heading.assertTextEquals("Select project")

        composeRule.runOnIdle { title.value = "Project documents" }

        heading.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        heading.assertHasNoClickAction()
        heading.assertTextEquals("Project documents")
    }
}
