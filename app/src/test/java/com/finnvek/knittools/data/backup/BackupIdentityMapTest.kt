package com.finnvek.knittools.data.backup

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnvek.knittools.domain.model.YARN_CARD_IDS_MAX_CHARACTERS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun backupCursor(rows: List<List<Any?>>): Cursor {
    var position = -1
    return mockk(relaxed = true) {
        every { moveToFirst() } answers {
            position = 0
            rows.isNotEmpty()
        }
        every { moveToNext() } answers { ++position < rows.size }
        every { getLong(any()) } answers { (rows[position][firstArg<Int>()] as Number).toLong() }
        every { getInt(any()) } answers { (rows[position][firstArg<Int>()] as Number).toInt() }
        every { getDouble(any()) } answers { (rows[position][firstArg<Int>()] as Number).toDouble() }
        every { getString(any()) } answers { rows[position][firstArg<Int>()] as String }
        every { getType(any()) } answers {
            when (rows[position][firstArg<Int>()]) {
                null -> Cursor.FIELD_TYPE_NULL
                is Long, is Int -> Cursor.FIELD_TYPE_INTEGER
                is Double -> Cursor.FIELD_TYPE_FLOAT
                is String -> Cursor.FIELD_TYPE_STRING
                else -> Cursor.FIELD_TYPE_BLOB
            }
        }
    }
}

class BackupIdentityMapTest {
    private fun database(maximum: Long = 40): SupportSQLiteDatabase =
        mockk(relaxed = true) {
            every { query(any<String>()) } answers {
                val query = firstArg<String>()
                when {
                    query.startsWith("SELECT COALESCE") -> backupCursor(listOf(listOf(maximum)))
                    query.startsWith("SELECT id") -> backupCursor(listOf(listOf(1L), listOf(2L)))
                    else ->
                        backupCursor(
                            listOf(
                                listOf("""{"extraCounterId":7}"""),
                                listOf("""{"extraCounterId":7}"""),
                                listOf("{}"),
                                listOf("""{"extraCounterId":null}"""),
                            ),
                        )
                }
            }
            every { query(any<String>(), any<Array<Any?>>()) } answers { backupCursor(listOf(listOf(50L))) }
        }

    @Test fun remapsIdentitiesLinksAndRetainedChartCounterIdsAboveDestinationSequence() {
        val source = database()
        val destination = database()
        val identities = BackupIdentityMap(source, destination)
        val row =
            mutableMapOf<String, JsonElement>(
                "id" to JsonPrimitive(1),
                "projectId" to JsonPrimitive(2),
                "linkedProjectId" to JsonPrimitive(999),
                "linkedPatternId" to JsonNull,
                "savedPatternId" to JsonPrimitive(1),
                "savedYarnCardId" to JsonPrimitive(2),
                "yarnCardId" to JsonPrimitive(1),
                "projectYarnNoteId" to JsonPrimitive(2),
                "layerId" to JsonPrimitive(1),
                "folderId" to JsonPrimitive(2),
                "yarnCardIds" to
                    JsonPrimitive(" 1,invalid,2,1,9223372036854775808,-1,0,999 "),
                "documentKey" to JsonPrimitive("saved:1:v1"),
                "kind" to JsonPrimitive("CHART_TRACKER"),
                "payloadJson" to JsonPrimitive("""{"extraCounterId":7}"""),
            )
        identities.apply("pattern_annotations", row)
        assertEquals(JsonPrimitive(51), row["id"])
        assertEquals(JsonPrimitive(52), row["projectId"])
        assertEquals(JsonNull, row["linkedProjectId"])
        assertEquals(JsonNull, row["linkedPatternId"])
        assertEquals(JsonPrimitive("51,52"), row["yarnCardIds"])
        assertEquals(JsonPrimitive("saved:51:v1"), row["documentKey"])
        assertEquals(
            JsonPrimitive(53),
            BackupFormat.json
                .parseToJsonElement(
                    row.getValue("payloadJson").jsonPrimitive.content,
                ).jsonObject["extraCounterId"],
        )
        identities.reserveIds(destination)
        verify {
            destination.execSQL(
                "UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = ?",
                arrayOf<Any>(53L, "project_counters"),
            )
        }
    }

    @Test fun oversizedYarnCardIdsAreRejectedBeforeIdentityRemapping() {
        val identities = BackupIdentityMap(database(), database())
        val oversized = "x".repeat(YARN_CARD_IDS_MAX_CHARACTERS + 1)
        val row =
            mutableMapOf<String, JsonElement>(
                "id" to JsonPrimitive(1),
                "yarnCardIds" to JsonPrimitive(oversized),
            )

        val failure =
            assertThrows(BackupException::class.java) {
                identities.apply("counter_projects", row)
            }

        assertEquals(BackupError.VALIDATION, failure.error)
        assertEquals(JsonPrimitive(1), row["id"])
        assertEquals(JsonPrimitive(oversized), row["yarnCardIds"])
    }

    @Test fun documentKeysRemainConsistentWhileOrphanKeysCannotCollideWithLiveKeys() {
        val identities = BackupIdentityMap(database(), database())
        for ((key, expected) in listOf("legacy-project:2" to "legacy-project:52", "content:abc" to "content:abc")) {
            val row = mutableMapOf<String, JsonElement>("documentKey" to JsonPrimitive(key))
            identities.apply("project_documents", row)
            assertEquals(JsonPrimitive(expected), row["documentKey"])
        }
        for (key in listOf("saved:999:v1", "legacy-project:999")) {
            val first = mutableMapOf<String, JsonElement>("documentKey" to JsonPrimitive(key))
            val second = first.toMutableMap()
            identities.apply("pattern_bookmarks", first)
            identities.apply("pattern_annotation_layers", second)
            assertEquals(first, second)
            assertTrue(
                first
                    .getValue("documentKey")
                    .jsonPrimitive.content
                    .startsWith("restored-key:"),
            )
        }
    }

    @Test fun activeSessionsLoseTrustedAnchorsAndReceiveFreshTokens() {
        val identities = BackupIdentityMap(database(), database())
        val row = mutableMapOf<String, JsonElement>("id" to JsonPrimitive(1), "sessionToken" to JsonPrimitive("old"))
        identities.apply("active_sessions", row)
        assertEquals(JsonPrimitive(1), row["id"])
        assertEquals(JsonNull, row["bootCount"])
        assertEquals(JsonPrimitive("BOOT_IDENTITY_UNAVAILABLE"), row["recoveryReason"])
        assertEquals(JsonNull, row["recoverySuggestedDurationSeconds"])
        assertEquals(JsonPrimitive(0), row["recoveryPromptShown"])
        assertNotEquals(JsonPrimitive("old"), row["sessionToken"])
        assertNotEquals(row["sessionToken"], row["recoveryIntervalToken"])
    }

    @Test fun missingRequiredReferencesAndOverflowAreRejected() {
        val identities = BackupIdentityMap(database(), database())
        assertThrows(BackupException::class.java) {
            identities.apply("sessions", mutableMapOf("projectId" to JsonPrimitive(999)))
        }
        assertThrows(BackupException::class.java) { BackupIdentityMap(database(), database(Long.MAX_VALUE)) }
        for (payload in listOf("{}", """{"extraCounterId":null}""")) {
            val row =
                mutableMapOf<String, JsonElement>(
                    "kind" to JsonPrimitive("CHART_TRACKER"),
                    "payloadJson" to JsonPrimitive(payload),
                )
            identities.apply("pattern_annotations", row)
            assertEquals(JsonPrimitive(payload), row["payloadJson"])
        }
        assertThrows(BackupException::class.java) {
            identities.apply(
                "pattern_annotations",
                mutableMapOf(
                    "kind" to JsonPrimitive("CHART_TRACKER"),
                    "payloadJson" to JsonPrimitive("""{"extraCounterId":999}"""),
                ),
            )
        }
    }

    @Test fun directConstructionEnforcesPerTableAndAggregateIdentityBudgets() {
        val source = database()
        val destination = database()
        assertThrows(BackupException::class.java) {
            BackupIdentityMap(
                source,
                destination,
                BackupLimits(maxIdentitiesPerTable = 1, maxTotalIdentities = 100),
            )
        }
        BackupIdentityMap(
            source,
            destination,
            BackupLimits(maxIdentitiesPerTable = 3, maxTotalIdentities = 33),
        )
        assertThrows(BackupException::class.java) {
            BackupIdentityMap(
                source,
                destination,
                BackupLimits(maxIdentitiesPerTable = 3, maxTotalIdentities = 32),
            )
        }
    }
}
