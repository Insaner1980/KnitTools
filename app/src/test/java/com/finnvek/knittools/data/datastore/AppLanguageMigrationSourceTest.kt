package com.finnvek.knittools.data.datastore

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageMigrationSourceTest {
    @Test
    fun `system locale sync waits until stored language migration has completed`() {
        val source = ProjectSourceFiles.read(PREFERENCES_MANAGER)
        val syncBody =
            source
                .substringAfter("suspend fun syncAppLanguageFromSystem()")
                .substringBefore("private fun applyAppLanguage")

        assertTrue(syncBody.contains("prefs[KEY_APP_LANGUAGE_MIGRATED_TO_SYSTEM] != true"))
        assertTrue(syncBody.contains("return@editPreferencesSafely"))
    }

    @Test
    fun `api 33 locale access does not require an active appcompat activity`() {
        val source = ProjectSourceFiles.read(PREFERENCES_MANAGER)
        val applyBody =
            source
                .substringAfter("private fun applyAppLanguage")
                .substringBefore("private suspend fun migrateStoredLanguageToSystemIfNeeded")
        val currentBody =
            source
                .substringAfter("private fun currentAppLanguage")
                .substringBefore("private companion object")

        listOf(applyBody, currentBody).forEach { body ->
            assertTrue(body.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU"))
            assertTrue(body.contains("getSystemService(LocaleManager::class.java)"))
        }
        assertTrue(applyBody.contains("LocaleList.forLanguageTags(languageTags)"))
        assertTrue(applyBody.contains("AppCompatDelegate.setApplicationLocales"))
        assertTrue(currentBody.contains("AppCompatDelegate.getApplicationLocales().toLanguageTags()"))
    }

    private companion object {
        const val PREFERENCES_MANAGER =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt"
    }
}
