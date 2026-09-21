package com.finnvek.knittools.pro

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialStartSourceTest {
    @Test
    fun `startup only refreshes persisted trial state`() {
        val source = ProjectSourceFiles.read(TRIAL_MANAGER)
        val initializeBody = source.substringAfter("suspend fun initialize()").substringBefore("suspend fun startTrial")

        assertTrue(initializeBody.contains("refreshTrialState()"))
        assertFalse(initializeBody.contains("KEY_TRIAL_START"))
    }

    @Test
    fun `trial start uses one atomic datastore edit`() {
        val source = ProjectSourceFiles.read(TRIAL_MANAGER)
        val startBody =
            source
                .substringAfter("suspend fun startTrial")
                .substringBefore("suspend fun claimTrialEndNotice")
        val cleanStartBody = source.substringAfter("private fun isCleanUnstartedState")

        assertEqualsOne(needle = "editPreferencesSafely", source = startBody)
        assertTrue(startBody.contains("isCleanUnstartedState("))
        assertTrue(cleanStartBody.contains("startTimestamp == 0L"))
        assertTrue(cleanStartBody.contains("storedTiming == StoredTrialTiming.Legacy"))
        assertTrue(startBody.contains("preferences[KEY_TRIAL_START] = now"))
        assertTrue(startBody.contains("preferences.persistTrialEvaluation("))
        assertTrue(startBody.contains("classifyExistingTrial"))
    }

    @Test
    fun `trial start persistence includes every timing anchor in the same datastore edit`() {
        val source = ProjectSourceFiles.read(TRIAL_MANAGER)
        val persistenceBody =
            source
                .substringAfter("private fun MutablePreferences.persistTrialEvaluation")
                .substringBefore("private suspend fun startRefreshLoop")

        assertTrue(persistenceBody.contains("KEY_LAST_KNOWN_TIMESTAMP"))
        assertTrue(persistenceBody.contains("KEY_CLOCK_TAMPERED"))
        assertTrue(persistenceBody.contains("KEY_TRIAL_ELAPSED_DURATION"))
        assertTrue(persistenceBody.contains("KEY_TRIAL_ANCHOR_WALL_CLOCK"))
        assertTrue(persistenceBody.contains("KEY_TRIAL_ANCHOR_ELAPSED_REALTIME"))
        assertTrue(persistenceBody.contains("KEY_TRIAL_ANCHOR_BOOT_COUNT"))
    }

    @Test
    fun `trial progress updates derive and persist anchors inside one atomic edit`() {
        val source = ProjectSourceFiles.read(TRIAL_MANAGER)
        val updateBody =
            source
                .substringAfter("suspend fun updateTimestamp()")
                .substringBefore("private suspend fun refreshTrialState")
        val refreshBody =
            source
                .substringAfter("private suspend fun refreshTrialState()")
                .substringBefore("private fun startRefreshLoop")

        listOf(updateBody, refreshBody).forEach { body ->
            assertEqualsOne(needle = "editPreferencesSafely", source = body)
            assertFalse(body.contains("safePreferencesData.first()"))
            assertTrue(body.contains("preferences.evaluateAndPersistTrialState(now)"))
        }
    }

    @Test
    fun `trial progress fails closed in memory when datastore write fails`() {
        val source = ProjectSourceFiles.read(TRIAL_MANAGER)
        val updateBody =
            source
                .substringAfter("suspend fun updateTimestamp()")
                .substringBefore("private suspend fun refreshTrialState")
        val refreshBody =
            source
                .substringAfter("private suspend fun refreshTrialState()")
                .substringBefore("private fun MutablePreferences.evaluateAndPersistTrialState")

        assertTrue(updateBody.contains("if (!didWrite)"))
        assertTrue(updateBody.contains("_trialState.value = TrialState()"))
        assertTrue(refreshBody.contains("else {"))
        assertTrue(refreshBody.contains("_trialState.value = TrialState()"))
    }

    private fun assertEqualsOne(
        needle: String,
        source: String,
    ) {
        assertTrue(source.indexOf(needle) >= 0)
        assertTrue(source.indexOf(needle) == source.lastIndexOf(needle))
    }

    private companion object {
        const val TRIAL_MANAGER = "app/src/main/java/com/finnvek/knittools/pro/TrialManager.kt"
    }
}
