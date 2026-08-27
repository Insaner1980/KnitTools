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
        assertTrue(startBody.contains("preferences[KEY_TRIAL_START] = now"))
        assertTrue(startBody.contains("TrialStartResult.AlreadyStarted"))
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
