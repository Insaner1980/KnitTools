package com.finnvek.knittools.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsUnitRowTest(
    private val width: Int,
    private val fontScale: Float,
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longLabelKeepsTipVisibleAndSeparateFromToggle() {
        val checked = mutableStateOf(false)
        var changes = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                KnitToolsTheme(isDarkTheme = dark) {
                    Box(Modifier.width(width.dp)) {
                        SwitchRowWithTip(
                            label = "Använd brittiska/amerikanska mått",
                            checked = checked.value,
                            onCheckedChange = {
                                checked.value = it
                                changes++
                            },
                            tipTitle = "Måttenheter",
                            tipDescription = "Längder visas i tum och yards.",
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Måttenheter").assertIsDisplayed().performClick()
        compose.onNodeWithText("Längder visas i tum och yards.").assertIsDisplayed()
        assertEquals(0, changes)
        val ok = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.ok)
        compose.onNodeWithText(ok).performClick()
        compose.onNode(isToggleable()).performClick().assertIsOn()
        assertEquals(1, changes)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0},font={1},dark={2}")
        fun cases(): List<Array<Any>> =
            listOf(320 to 1f, 412 to 2f, 320 to 2f).flatMap { (width, font) ->
                listOf(false, true).map { dark -> arrayOf(width, font, dark) }
            }
    }
}
