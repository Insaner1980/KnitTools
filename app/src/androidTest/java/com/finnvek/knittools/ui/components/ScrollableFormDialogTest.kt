package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.ui.screens.counter.CounterItemActions
import com.finnvek.knittools.ui.screens.counter.CounterListItem
import com.finnvek.knittools.ui.screens.counter.StitchCountDialog
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScrollableFormDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun renameInsideSheetMatchesStitchesDialogWidth() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val counter = ProjectCounter(id = 1, projectId = 1, name = "Sleeve")
        var showSheet by mutableStateOf(false)
        composeRule.setContent {
            KnitToolsTheme {
                if (showSheet) {
                    ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                        CounterListItem(counter, CounterItemActions({}, {}, {}, {}, {}))
                    }
                } else {
                    StitchCountDialog(24, MainCounterLabelType.ROWS, {}, {})
                }
            }
        }

        val stitchesWidth =
            composeRule
                .onNodeWithContentDescription(resources.getString(R.string.stitches_in_row_label))
                .assertIsDisplayed()
                .getUnclippedBoundsInRoot()
                .let { it.right - it.left }
        composeRule.runOnIdle { showSheet = true }
        composeRule
            .onNodeWithContentDescription(
                resources.getString(R.string.counter_actions, counter.name),
            ).performClick()
        composeRule.onNodeWithText(resources.getString(R.string.rename_counter)).performClick()
        val renameWidth =
            composeRule
                .onNodeWithContentDescription(resources.getString(R.string.counter_name))
                .assertIsDisplayed()
                .getUnclippedBoundsInRoot()
                .let { it.right - it.left }
        assertEquals(stitchesWidth, renameWidth)
    }

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
