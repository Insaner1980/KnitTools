package com.finnvek.knittools.data.backup

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupBudgetTest {
    @Test fun sessionsHaveAnIndependentCeilingThatCustomBudgetsCannotRelax() {
        for (trackIdentities in listOf(true, false)) {
            val budget =
                BackupBudget(
                    BackupLimits(maxSessionRows = Long.MAX_VALUE, maxRowsPerTable = Long.MAX_VALUE),
                    trackEmbeddedIdentities = trackIdentities,
                )
            repeat(BackupLimits.MAX_SESSION_ROWS.toInt()) { budget.addRow("sessions") }
            assertThrows(BackupException::class.java) { budget.addRow("sessions") }
        }
        val tighter = BackupBudget(BackupLimits(maxSessionRows = 2))
        repeat(2) { tighter.addRow("sessions") }
        assertThrows(BackupException::class.java) { tighter.addRow("sessions") }
        val generic = BackupBudget(BackupLimits(maxRowsPerTable = 1))
        generic.addRow("sessions")
        assertThrows(BackupException::class.java) { generic.addRow("sessions") }
    }

    @Test fun tableAndExtractedByteLimitsAcceptExactBoundaryAndRejectNextByte() {
        val limits =
            BackupLimits(
                maxExtractedBytes = 10,
                maxTableBytes = 6,
                maxTableTotalBytes = 8,
                maxDurableFileBytes = 4,
                maxDurableBytes = 4,
            )
        BackupBudget(limits).apply {
            addArchiveEntry("tables/counter_projects.jsonl", 6)
            addArchiveEntry("tables/sessions.jsonl", 2)
            addArchiveEntry("files/${"a".repeat(64)}.bin", 2)
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).addArchiveEntry("tables/counter_projects.jsonl", 7)
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply {
                addArchiveEntry("tables/counter_projects.jsonl", 6)
                addArchiveEntry("tables/sessions.jsonl", 3)
            }
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply {
                addArchiveEntry("tables/counter_projects.jsonl", 6)
                addArchiveEntry("files/${"a".repeat(64)}.bin", 4)
                addArchiveEntry("tables/sessions.jsonl", 1)
            }
        }
    }

    @Test fun archiveLimitAcceptsExactBoundaryAndRejectsNextByte() {
        val budget = BackupBudget(BackupLimits(maxArchiveBytes = 10))
        budget.requireArchiveSize(10)
        assertThrows(BackupException::class.java) { budget.requireArchiveSize(11) }
    }

    @Test fun rowAndFieldLimitsAggregateAcrossTables() {
        val limits =
            BackupLimits(
                maxRowsPerTable = 2,
                maxTotalRows = 3,
                maxRowCharacters = 5,
                maxFieldCharacters = 4,
            )
        BackupBudget(limits).apply {
            addRow("active_sessions")
            addRow("active_sessions")
            addRow("project_folder_assignments")
            requireRowLength(5)
            requireFieldLength(4)
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply { repeat(3) { addRow("active_sessions") } }
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply {
                repeat(2) { addRow("active_sessions") }
                repeat(2) { addRow("project_folder_assignments") }
            }
        }
        assertThrows(BackupException::class.java) { BackupBudget(limits).requireRowLength(6) }
        assertThrows(BackupException::class.java) { BackupBudget(limits).requireFieldLength(5) }
    }

    @Test fun durableFilesAcceptExactCountAndBytesButRejectEachLimitPlusOne() {
        val limits =
            BackupLimits(
                maxDurableFileBytes = 4,
                maxDurableFileCount = 2,
                maxDurableBytes = 6,
            )
        BackupBudget(limits).apply {
            addDurableCopy("first", 4)
            addDurableCopy("second", 2)
            addDurableCopy("first", 4)
            assertEquals(6, requiredDurableCopyBytes)
        }
        assertThrows(BackupException::class.java) { BackupBudget(limits).addDurableCopy("file", 5) }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply {
                addDurableCopy("first", 3)
                addDurableCopy("second", 3)
                addDurableCopy("third", 0)
            }
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply {
                addDurableCopy("first", 4)
                addDurableCopy("second", 3)
            }
        }
    }

    @Test fun identityTotalsAggregateAcrossTablesWithoutOverflow() {
        val limits = BackupLimits(maxIdentitiesPerTable = 2, maxTotalIdentities = 3)
        BackupBudget(limits).apply {
            addIdentity("counter_projects")
            addIdentity("counter_projects")
            addIdentity("sessions")
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply { repeat(3) { addIdentity("counter_projects") } }
        }
        assertThrows(BackupException::class.java) {
            BackupBudget(limits).apply {
                repeat(2) { addIdentity("counter_projects") }
                repeat(2) { addIdentity("sessions") }
            }
        }
        assertEquals(Long.MAX_VALUE, BackupFormat.addWithinLimit(Long.MAX_VALUE - 1, 1, Long.MAX_VALUE))
        assertThrows(BackupException::class.java) {
            BackupFormat.addWithinLimit(Long.MAX_VALUE, 1, Long.MAX_VALUE)
        }
        assertThrows(BackupException::class.java) {
            BackupFormat.multiplyWithinLimit(Long.MAX_VALUE, 2)
        }
    }

    @Test fun danglingChartCounterIdsShareTheProjectCounterBudgetAndDuplicatesDoNot() {
        val limits = BackupLimits(maxIdentitiesPerTable = 3, maxTotalIdentities = 4)
        val budget = BackupBudget(limits)
        for (id in 1L..2L) {
            budget.addRow("project_counters")
            budget.observeRow("project_counters", mutableMapOf<String, JsonElement>("id" to JsonPrimitive(id)))
        }
        budget.addRow("pattern_annotations")
        val dangling =
            mutableMapOf<String, JsonElement>(
                "kind" to JsonPrimitive("CHART_TRACKER"),
                "payloadJson" to JsonPrimitive("""{"extraCounterId":7}"""),
            )
        budget.observeRow("pattern_annotations", dangling)
        budget.observeRow("pattern_annotations", dangling)
        budget.complete()

        val exceeded = BackupBudget(limits)
        for (id in 1L..2L) {
            exceeded.addRow("project_counters")
            exceeded.observeRow("project_counters", mapOf("id" to JsonPrimitive(id)))
        }
        exceeded.addRow("pattern_annotations")
        exceeded.observeRow("pattern_annotations", dangling)
        assertThrows(BackupException::class.java) {
            exceeded.observeRow(
                "pattern_annotations",
                mapOf(
                    "kind" to JsonPrimitive("CHART_TRACKER"),
                    "payloadJson" to JsonPrimitive("""{"extraCounterId":8}"""),
                ),
            )
        }
    }
}
