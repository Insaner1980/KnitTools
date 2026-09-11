package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScrollableFormDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun largeTextFormCanScrollBetweenInputAndActionsWithoutLosingInput() {
        var saved: String? = null
        var cancelled = false
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    var value by remember { mutableStateOf("") }
                    ScrollableFormDialog(
                        onDismissRequest = { cancelled = true },
                        modifier = Modifier.width(320.dp).heightIn(max = 320.dp),
                        title = { Text("A long form title that wraps across several lines") },
                        text = {
                            TextField(value = value, onValueChange = { value = it }, label = { Text("Name") })
                            Text("An explanation that needs several lines at this font size. ".repeat(8))
                        },
                        confirmButton = {
                            TextButton(onClick = { saved = value }, enabled = value.isNotBlank()) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { cancelled = true }) { Text("Cancel") }
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        composeRule
            .onNodeWithText("Name")
            .performScrollTo()
            .performClick()
            .performTextInput("Cardigan")
        composeRule.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("Save")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertEquals("Cardigan", saved)
            assertEquals(false, cancelled)
        }
    }
}
