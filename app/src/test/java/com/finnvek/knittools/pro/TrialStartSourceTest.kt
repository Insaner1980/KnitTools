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

        assertEqualsOne(needle = "editPreferencesSafely", source = startBody)
        assertTrue(startBody.contains("if (startTimestamp == 0L && !clockTamperedAlready)"))
        assertTrue(startBody.contains("preferences[KEY_TRIAL_START] = now"))
        assertTrue(startBody.contains("preferences[KEY_CLOCK_TAMPERED] = false"))
        assertTrue(startBody.contains("classifyExistingTrial"))
    }

    @Test
    fun `trial clock updates derive the previous timestamp inside the atomic edit`() {
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
            assertTrue(body.contains("preferences[KEY_LAST_KNOWN_TIMESTAMP]"))
        }
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
