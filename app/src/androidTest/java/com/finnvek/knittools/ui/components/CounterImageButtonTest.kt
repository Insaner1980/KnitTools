package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Rule
import org.junit.Test

class CounterImageButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonExposesTouchTargetAndKeyboardFocus() {
        composeRule.setContent {
            KnitToolsTheme {
                CounterImageButton(
                    imageRes = R.drawable.counter_undo_button,
                    contentDescription = "Undo",
                    visualSize = 40.dp,
                    onClick = {},
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Undo")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
    }
}
