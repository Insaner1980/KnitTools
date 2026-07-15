package com.finnvek.knittools

import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityThemeTest {
    @Test
    fun `startup theme is unresolved until preferences load`() {
        val isDarkTheme = null.resolveStartupDarkTheme(systemInDarkTheme = true)

        assertNull(isDarkTheme)
    }

    @Test
    fun `explicit dark theme overrides light system theme`() {
        val isDarkTheme =
            AppPreferences(themeMode = ThemeMode.DARK)
                .resolveStartupDarkTheme(systemInDarkTheme = false)

        assertEquals(true, isDarkTheme)
    }

    @Test
    fun `explicit light theme overrides dark system theme`() {
        val isDarkTheme =
            AppPreferences(themeMode = ThemeMode.LIGHT)
                .resolveStartupDarkTheme(systemInDarkTheme = true)

        assertEquals(false, isDarkTheme)
    }

    @Test
    fun `system theme follows current system darkness`() {
        val preferences = AppPreferences(themeMode = ThemeMode.SYSTEM)

        assertEquals(true, preferences.resolveStartupDarkTheme(systemInDarkTheme = true))
        assertEquals(false, preferences.resolveStartupDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun `splash does not resolve a second theme from system night resources`() {
        val splashTheme = ProjectSourceFiles.read("app/src/main/res/values/themes.xml")
        val nightTheme = ProjectSourceFiles.file("app/src/main/res/values-night/themes.xml")
        val nightColors = ProjectSourceFiles.file("app/src/main/res/values-night/colors.xml")

        assertTrue(splashTheme.contains("<item name=\"android:windowLightStatusBar\">false</item>"))
        assertTrue(splashTheme.contains("<item name=\"android:windowLightNavigationBar\">false</item>"))
        assertFalse(nightTheme.toFile().exists())
        assertFalse(nightColors.toFile().exists())
    }
}
