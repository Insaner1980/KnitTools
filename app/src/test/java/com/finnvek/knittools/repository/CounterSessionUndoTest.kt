package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.CounterSessionUndo
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.MainCounterChange
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterSessionUndoTest : CounterRepositoryActiveSessionFixture() {
    private var project = CounterProjectEntity(id = 7L, name = "Undo", count = 12)
    private val history = mutableListOf<CounterHistoryEntity>()

    @Test
    fun `undo falls back for malformed incompatible or invalid session snapshots`() =
        runTest {
            for (corruption in listOf("malformed", "incompatible", "invalid")) {
                prepareCounter(12, 1)
                repository.startSession(7L)
                change(MainCounterChange.Increment)
                val snapshot = requireNotNull(history.last().sessionUndoJson)
                val corrupted =
                    when (corruption) {
                        "malformed" -> "not-json"
                        "incompatible" -> snapshot.dropLast(1) + ",\"futureField\":true}"
                        else -> CounterSessionUndo.decode(snapshot).copy(trustedRowsWorked = -1).encode()
                    }
                history[history.lastIndex] = history.last().copy(sessionUndoJson = corrupted)

                change(MainCounterChange.Undo)

                assertEquals(12, project.count)
                assertEquals(12, active?.lastObservedRow)
                assertEquals(0, active?.trustedRowsWorked)
                assertTrue(history.isEmpty())
                repository.discardActiveSession(requireNotNull(active).sessionToken)
            }
        }

    @Test
    fun `decrement and undo preserve zero session progress across clamps`() =
        runTest {
            for ((count, step) in listOf(12 to 1, 12 to 5, 2 to 5)) {
                prepareCounter(count, step)
                repository.startSession(7L)
                timeSource.advance(1)
                change(MainCounterChange.Decrement)
                change(MainCounterChange.Undo)

                assertEquals(count, project.count)
                assertEquals(0, active?.trustedRowsWorked)
                assertEquals(0, active?.unreviewedRowsWorked)
                assertEquals(0, active?.pendingRowsWorked)
                assertEquals(count, active?.lastObservedRow)
                assertTrue(history.isEmpty())
                stopReviewedSession(requireNotNull(active).sessionToken)
                assertEquals(0, completed.last().rowsWorked)
            }
        }

    @Test
    fun `undo preserves work counted after reset instead of deriving it from the session start row`() =
        runTest {
            prepareCounter(12, 5)
            repository.startSession(7L)
            change(MainCounterChange.Reset)
            change(MainCounterChange.Increment)
            change(MainCounterChange.Decrement)
            change(MainCounterChange.Undo)

            assertEquals(5, project.count)
            assertEquals(5, active?.trustedRowsWorked)
            assertEquals(5, active?.unreviewedRowsWorked)
        }

    private fun prepareCounter(
        count: Int,
        step: Int,
    ) {
        project = project.copy(count = count, stepSize = step)
        history.clear()
        coEvery { projectDao.getProject(7L) } answers { project }
        projectDao.recordCounterHistory(7L, history) { project = project.copy(count = it) }
        coEvery { projectDao.updateLatestHistorySessionUndo(7L, any()) } answers {
            history[history.lastIndex] = history.last().copy(sessionUndoJson = secondArg())
        }
        repository = buildRepository(repositoryProjectCounterDao())
    }

    @Test
    fun `undo restores pending rows within recovery and trusted rows across reboot`() =
        runTest {
            for (rebootBeforeChange in listOf(false, true)) {
                prepareCounter(12, 5)
                repository.startSession(7L)
                if (rebootBeforeChange) timeSource.reboot()
                change(MainCounterChange.Decrement)
                if (!rebootBeforeChange) timeSource.reboot()
                change(MainCounterChange.Undo)
                assertEquals(12, active?.lastObservedRow)
                assertEquals(0, active?.trustedRowsWorked)
                assertEquals(0, active?.pendingRowsWorked)
                assertEquals(0, active?.unreviewedRowsWorked)
                assertEquals(ActiveSessionRecoveryReason.REBOOTED.name, active?.recoveryReason)
                repository.discardActiveSession(requireNotNull(active).sessionToken)
            }
        }

    @Test
    fun `undo follows unreviewed rows into long running recovery`() =
        runTest {
            prepareCounter(12, 5)
            repository.startSession(7L)
            change(MainCounterChange.Increment)
            change(MainCounterChange.Decrement)
            timeSource.advance(24 * 60 * 60L)
            change(MainCounterChange.Undo)
            assertEquals(17, active?.lastObservedRow)
            assertEquals(0, active?.trustedRowsWorked)
            assertEquals(5, active?.pendingRowsWorked)
            assertEquals(0, active?.unreviewedRowsWorked)
            assertEquals(ActiveSessionRecoveryReason.LONG_RUNNING.name, active?.recoveryReason)
            change(MainCounterChange.Undo)
            assertEquals(0, active?.pendingRowsWorked)
        }

    @Test
    fun `undo from an earlier session cannot restore its row snapshot`() =
        runTest {
            prepareCounter(12, 5)
            repository.startSession(7L)
            change(MainCounterChange.Increment)
            change(MainCounterChange.Increment)
            repository.discardActiveSession(requireNotNull(active).sessionToken)
            repository.startSession(7L)
            change(MainCounterChange.Undo)
            assertEquals(17, active?.lastObservedRow)
            assertEquals(0, active?.trustedRowsWorked)
            assertEquals(0, active?.unreviewedRowsWorked)
        }

    @Test
    fun `undo does not rewind an explicitly reviewed recovery baseline`() =
        runTest {
            prepareCounter(12, 5)
            repository.startSession(7L)
            timeSource.reboot()
            change(MainCounterChange.Increment)
            val recovery = requireNotNull(active)
            repository.addRecoveryInterval(recovery.sessionToken, requireNotNull(recovery.recoveryIntervalToken), 30)
            change(MainCounterChange.Undo)
            assertEquals(5, active?.reviewedRowsWorked)
            assertEquals(17, active?.reviewedLastObservedRow)
            assertEquals(30L, active?.reviewedDurationBaselineSeconds)
            assertEquals(0, active?.trustedRowsWorked)
            assertEquals(0, active?.unreviewedRowsWorked)
        }

    private suspend fun change(change: MainCounterChange) {
        assertTrue(repository.applyMainCounterChange(7L, change))
    }
}
