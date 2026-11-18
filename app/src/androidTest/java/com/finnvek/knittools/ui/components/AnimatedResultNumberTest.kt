package com.finnvek.knittools.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AnimatedResultNumberTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exposesOnlyCurrentResultDuringAnimationAndRemovesItWhenInputBecomesInvalid() {
        val result = mutableStateOf<String?>("24 stitches")
        rule.setContent {
            val value = result.value
            if (value == null) {
                Text("Invalid input")
            } else {
                AnimatedResultNumber(value) { Text(it) }
            }
        }
        rule.onNodeWithText("24 stitches").assertIsDisplayed()

        rule.mainClock.autoAdvance = false
        rule.runOnIdle { result.value = "36 stitches" }
        rule.mainClock.advanceTimeBy(64)
        rule.onNodeWithText("36 stitches").assertIsDisplayed()
        rule.onNodeWithText("24 stitches").assertDoesNotExist()

        rule.runOnIdle { result.value = null }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Invalid input").assertIsDisplayed()
        rule.onNodeWithText("36 stitches").assertDoesNotExist()
    }
}
