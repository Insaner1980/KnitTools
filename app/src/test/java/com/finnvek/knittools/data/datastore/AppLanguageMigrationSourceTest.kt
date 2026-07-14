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

    private companion object {
        const val PREFERENCES_MANAGER =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt"
    }
}
