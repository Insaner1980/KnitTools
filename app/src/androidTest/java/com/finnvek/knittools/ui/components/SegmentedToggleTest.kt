package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SegmentedToggleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedOptionExposesSelectionSemantics() {
        composeRule.setContent {
            MaterialTheme {
                var selectedIndex by remember { mutableIntStateOf(1) }

                SegmentedToggle(
                    options = listOf("Metric", "Imperial"),
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                )
            }
        }

        composeRule.onNodeWithText("Imperial").assertIsSelected()
        composeRule.onNodeWithText("Metric").assertIsNotSelected()

        composeRule.onNodeWithText("Metric").performClick()

        composeRule.onNodeWithText("Metric").assertIsSelected()
        composeRule.onNodeWithText("Imperial").assertIsNotSelected()
    }
}
