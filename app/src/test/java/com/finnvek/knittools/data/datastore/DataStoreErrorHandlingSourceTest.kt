package com.finnvek.knittools.data.datastore

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreErrorHandlingSourceTest {
    @Test
    fun `core preference reads use IOException fallback`() {
        val source = ProjectSourceFiles.read(PREFERENCES_MANAGER)
        val safety = ProjectSourceFiles.read(PREFERENCES_DATASTORE_SAFETY)

        assertTrue(source.contains("context.dataStore.safePreferencesData"))
        assertTrue(source.contains("context.dataStore.readPreferencesOrNull"))
        assertTrue(safety.contains("catch { exception ->"))
        assertTrue(safety.contains("emit(emptyPreferences())"))
    }

    @Test
    fun `quota stores replace corrupted preferences with empty preferences`() {
        listOf(AI_QUOTA_MANAGER, VOICE_LIVE_QUOTA_MANAGER).forEach { path ->
            val source = ProjectSourceFiles.read(path)

            assertTrue(source.contains("ReplaceFileCorruptionHandler"))
            assertTrue(source.contains("corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }"))
        }
    }

    @Test
    fun `quota reads use IOException fallback`() {
        listOf(
            AI_QUOTA_MANAGER to "context.aiQuotaStore.safePreferencesData",
            VOICE_LIVE_QUOTA_MANAGER to "context.voiceLiveQuotaStore.safePreferencesData",
        ).forEach { (path, expectedRead) ->
            val source = ProjectSourceFiles.read(path)

            assertTrue(source.contains(expectedRead))
        }
    }

    @Test
    fun `other preference stores use IOException fallback`() {
        listOf(
            IN_APP_REVIEW_MANAGER to "context.reviewDataStore.safePreferencesData",
            TRIAL_MANAGER to "context.trialDataStore.safePreferencesData",
            COUNTER_WIDGET_STATE to "context.widgetDataStore.safePreferencesData",
        ).forEach { (path, expectedRead) ->
            val source = ProjectSourceFiles.read(path)

            assertTrue(source.contains(expectedRead))
        }
    }

    @Test
    fun `preference writes use safe write wrappers`() {
        listOf(
            PREFERENCES_MANAGER,
            AI_QUOTA_MANAGER,
            VOICE_LIVE_QUOTA_MANAGER,
            IN_APP_REVIEW_MANAGER,
            TRIAL_MANAGER,
        ).forEach { path ->
            val source = ProjectSourceFiles.read(path)

            assertTrue(source.contains("editPreferencesSafely"))
        }

        assertTrue(ProjectSourceFiles.read(COUNTER_WIDGET_STATE).contains("updatePreferencesSafely"))
    }

    private companion object {
        const val PREFERENCES_MANAGER =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt"
        const val PREFERENCES_DATASTORE_SAFETY =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesDataStoreSafety.kt"
        const val AI_QUOTA_MANAGER =
            "app/src/main/java/com/finnvek/knittools/ai/AiQuotaManager.kt"
        const val VOICE_LIVE_QUOTA_MANAGER =
            "app/src/main/java/com/finnvek/knittools/ai/live/VoiceLiveQuotaManager.kt"
        const val IN_APP_REVIEW_MANAGER =
            "app/src/main/java/com/finnvek/knittools/pro/InAppReviewManager.kt"
        const val TRIAL_MANAGER =
            "app/src/main/java/com/finnvek/knittools/pro/TrialManager.kt"
        const val COUNTER_WIDGET_STATE =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetState.kt"
    }
}
