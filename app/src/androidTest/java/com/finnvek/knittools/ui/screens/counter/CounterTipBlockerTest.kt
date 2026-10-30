package com.finnvek.knittools.ui.screens.counter

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterTipBlockerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun extraCounterMenuAndRenameBlockTipsUntilClosedOrRemoved() {
        var showCounter by mutableStateOf(true)
        var blocked = false
        composeRule.setContent {
            KnitToolsTheme {
                if (showCounter) {
                    CounterListItem(
                        counter = ProjectCounter(id = 1L, projectId = 7L, name = "Sleeve"),
                        actions = CounterItemActions({}, {}, {}, {}, {}),
                        onBlockingSurfaceChanged = { blocked = it },
                    )
                }
            }
        }
        val menuDescription = composeRule.activity.getString(R.string.counter_actions, "Sleeve")
        composeRule.onNodeWithContentDescription(menuDescription).performClick()
        composeRule.runOnIdle { assertTrue(blocked) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.rename_counter)).performClick()
        composeRule.runOnIdle { assertTrue(blocked) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.cancel)).performClick()
        composeRule.runOnIdle { assertFalse(blocked) }

        composeRule.onNodeWithContentDescription(menuDescription).performClick()
        composeRule.runOnIdle {
            assertTrue(blocked)
            showCounter = false
        }
        composeRule.runOnIdle { assertFalse(blocked) }
    }
}
