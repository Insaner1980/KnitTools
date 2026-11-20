package com.finnvek.knittools.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object SavedPatternMigration30 {
    // Older versions stored the localized PDF source label as the designer name.
    private val importedPdfLabels =
        arrayOf(
            "Imported PDF",
            "Importeret PDF",
            "Importiertes PDF",
            "PDF importado",
            "Tuotu PDF",
            "PDF importé",
            "PDF importato",
            "Importert PDF",
            "Geïmporteerde PDF",
            "Importerad PDF",
        )

    val migration =
        object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                savePatternReferences(db)
                rebuildSavedPatterns(db)
                restorePatternReferences(db)
                clearLegacyImportedDesigners(db)
            }
        }

    fun clearLegacyImportedDesigners(db: SupportSQLiteDatabase) {
        val placeholders = importedPdfLabels.joinToString(", ") { "?" }
        db.execSQL(
            """
            UPDATE saved_patterns
            SET designerName = NULL
            WHERE source = 'LOCAL_FILE' AND localPdfUri IS NOT NULL AND designerName IN ($placeholders)
            """.trimIndent(),
            importedPdfLabels,
        )
    }

    private fun savePatternReferences(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TEMP TABLE saved_pattern_document_links AS
            SELECT id, savedPatternId FROM project_documents WHERE savedPatternId IS NOT NULL
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TEMP TABLE saved_pattern_layers AS
            SELECT id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            FROM pattern_annotation_layers WHERE savedPatternId IS NOT NULL
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TEMP TABLE saved_pattern_annotations AS
            SELECT id, layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt
            FROM pattern_annotations WHERE layerId IN (SELECT id FROM saved_pattern_layers)
            """.trimIndent(),
        )
        db.execSQL("DELETE FROM pattern_annotations WHERE layerId IN (SELECT id FROM saved_pattern_layers)")
        db.execSQL("DELETE FROM pattern_annotation_layers WHERE savedPatternId IS NOT NULL")
    }

    private fun rebuildSavedPatterns(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE saved_patterns_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                source TEXT NOT NULL,
                ravelryPatternId INTEGER,
                name TEXT NOT NULL,
                designerName TEXT,
                thumbnailUrl TEXT,
                difficulty REAL,
                gaugeStitches REAL,
                gaugeRows REAL,
                needleSize TEXT,
                yarnWeight TEXT,
                yardage INTEGER,
                availability TEXT NOT NULL,
                originalUrl TEXT NOT NULL,
                canonicalUrl TEXT NOT NULL,
                localPdfUri TEXT,
                isAvailableOffline INTEGER NOT NULL,
                savedAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastSyncedAt INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO saved_patterns_new (
                id, source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage, availability,
                originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            )
            SELECT id, source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage, availability,
                originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                savedAt, updatedAt, lastSyncedAt
            FROM saved_patterns
            """.trimIndent(),
        )
        // Legacy projects may still reference the ID of a deleted pattern.
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'saved_patterns_new'")
        db.execSQL(
            "INSERT INTO sqlite_sequence (name, seq) SELECT 'saved_patterns_new', seq " +
                "FROM sqlite_sequence WHERE name = 'saved_patterns'",
        )
        db.execSQL("DROP TABLE saved_patterns")
        db.execSQL("ALTER TABLE saved_patterns_new RENAME TO saved_patterns")
        listOf("ravelryPatternId", "canonicalUrl", "originalUrl", "localPdfUri").forEach { column ->
            db.execSQL("CREATE INDEX `index_saved_patterns_$column` ON saved_patterns (`$column`)")
        }
    }

    private fun restorePatternReferences(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO pattern_annotation_layers (
                id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            )
            SELECT id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            FROM saved_pattern_layers
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO pattern_annotations (
                id, layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt
            )
            SELECT id, layerId, page, kind, payloadVersion, payloadJson, zIndex, createdAt, updatedAt
            FROM saved_pattern_annotations
            """.trimIndent(),
        )
        db.execSQL(
            """
            UPDATE project_documents
            SET savedPatternId = (
                SELECT savedPatternId FROM saved_pattern_document_links WHERE id = project_documents.id
            )
            WHERE id IN (SELECT id FROM saved_pattern_document_links)
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE saved_pattern_annotations")
        db.execSQL("DROP TABLE saved_pattern_layers")
        db.execSQL("DROP TABLE saved_pattern_document_links")
    }
}
