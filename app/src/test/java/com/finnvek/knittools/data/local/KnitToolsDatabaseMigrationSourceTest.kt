package com.finnvek.knittools.data.local

import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnitToolsDatabaseMigrationSourceTest {
    @Test
    fun `migration 29 to 30 clears only old localized designer labels on imported PDFs`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        KnitToolsDatabase.MIGRATION_29_30.migrate(db)

        verify {
            db.execSQL(
                match {
                    it.contains("SET designerName = NULL") &&
                        it.contains("source = 'LOCAL_FILE'") &&
                        it.contains("localPdfUri IS NOT NULL")
                },
                match {
                    it.toSet() ==
                        setOf(
                            "Imported PDF",
                            "Tuotu PDF",
                            "Importiertes PDF",
                            "PDF importé",
                            "PDF importado",
                            "Importert PDF",
                            "Geïmporteerde PDF",
                            "PDF importato",
                            "Importerad PDF",
                            "Importeret PDF",
                        )
                },
            )
        }
        assertTrue(KnitToolsDatabase.MIGRATION_29_30 in KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)
    }

    @Test
    fun `legacy document labels strip storage prefixes and preserve filename colons`() {
        val provider = "content://com.android.externalstorage.documents/document/"
        val cases =
            listOf(
                "file:///patterns/chart:v2.pdf" to "file:///patterns/chart:v2.pdf",
                "${provider}primary%3APatterns%2Fchart%3Av2.pdf" to "${provider}primary:Patterns/chart:v2.pdf",
                "${provider}primary%3Achart%3Av2.pdf" to "${provider}primary:chart:v2.pdf",
                "file:///patterns/document/chart:v2.pdf" to "file:///patterns/document/chart:v2.pdf",
                "content://other/document/chart:v2.pdf" to "content://other/document/chart:v2.pdf",
            )
        val label =
            ProjectDocumentMigration22::class.java.getDeclaredMethod("decodedFileName", String::class.java).apply {
                isAccessible = true
            }
        mockkStatic(Uri::class)
        try {
            cases.forEach { (encoded, decoded) ->
                every { Uri.decode(encoded) } returns decoded
                assertEquals("chart:v2.pdf", label.invoke(ProjectDocumentMigration22, encoded))
            }
        } finally {
            unmockkStatic(Uri::class)
        }
    }

    @Test
    fun `migration 3 to 4 does not duplicate legacy secondary counter into project counters`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val statements = mutableListOf<String>()
        every { db.execSQL(capture(statements)) } just Runs

        KnitToolsDatabase.MIGRATION_3_4.migrate(db)

        assertFalse(
            statements.any { statement ->
                statement.contains("INSERT INTO project_counters", ignoreCase = true)
            },
        )
    }

    @Test
    fun `migration 17 to 18 unlocks existing project content conservatively`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val statements = mutableListOf<String>()
        every { db.execSQL(capture(statements)) } just Runs

        KnitToolsDatabase.MIGRATION_17_18.migrate(db)

        assertEquals(
            listOf(
                "ALTER TABLE counter_projects ADD COLUMN secondaryCounterUsed INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE counter_projects ADD COLUMN notesCreated INTEGER NOT NULL DEFAULT 0",
                "UPDATE counter_projects SET secondaryCounterUsed = 1, notesCreated = 1",
            ),
            statements,
        )
    }

    @Test
    fun `migration 18 to 19 preserves annotation children and maps false to unknown`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val statements = mutableListOf<String>()
        every { db.execSQL(capture(statements)) } just Runs

        KnitToolsDatabase.MIGRATION_18_19.migrate(db)

        val sql = statements.map { it.replace(Regex("\\s+"), " ").trim() }
        assertTrue(sql.any { it.startsWith("CREATE TEMP TABLE `saved_pattern_layer_backup`") })
        assertTrue(sql.any { it.startsWith("CREATE TEMP TABLE `saved_pattern_annotation_backup`") })
        assertTrue(sql.any { it.contains("CASE WHEN isFree = 1 THEN 'free' ELSE 'unknown' END") })
        assertTrue(sql.any { it.contains("`availability` TEXT NOT NULL") })
        assertFalse(sql.any { it.contains("CASE WHEN isFree = 0 THEN 'paid'") })
        val deleteAnnotations = sql.indexOfFirst { it.startsWith("DELETE FROM `pattern_annotations`") }
        val deleteLayers = sql.indexOfFirst { it.startsWith("DELETE FROM `pattern_annotation_layers`") }
        val restoreLayers = sql.indexOfFirst { it.startsWith("INSERT INTO `pattern_annotation_layers`") }
        val restoreAnnotations = sql.indexOfFirst { it.startsWith("INSERT INTO `pattern_annotations`") }
        assertTrue(deleteAnnotations >= 0 && deleteLayers >= 0)
        assertTrue(restoreLayers >= 0 && restoreAnnotations >= 0)
        assertTrue(deleteAnnotations < deleteLayers)
        assertTrue(restoreLayers < restoreAnnotations)
    }

    @Test
    fun `migration 19 to 20 adds project guides and owned bookmarks`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val statements = mutableListOf<String>()
        every { db.execSQL(capture(statements)) } just Runs

        KnitToolsDatabase.MIGRATION_19_20.migrate(db)

        val sql = statements.map { it.replace(Regex("\\s+"), " ").trim() }
        assertTrue(sql.any { it.contains("verticalReadingGuideEnabled INTEGER NOT NULL DEFAULT 0") })
        assertTrue(sql.any { it.contains("verticalReadingGuideXFraction REAL NOT NULL DEFAULT 0.5") })
        assertTrue(sql.any { it.contains("readingLineFollowCurrentRow INTEGER NOT NULL DEFAULT 1") })
        assertTrue(sql.any { it.startsWith("CREATE TABLE IF NOT EXISTS `pattern_bookmarks`") })
        assertTrue(sql.any { it.contains("ON UPDATE NO ACTION ON DELETE CASCADE") })
        assertTrue(sql.any { it.contains("(`projectId`, `documentKey`, `pageIndex`, `yFraction`, `createdAt`, `id`)") })
    }
}
