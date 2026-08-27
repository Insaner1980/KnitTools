package com.finnvek.knittools.data.local

import android.database.Cursor
import android.net.Uri
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnvek.knittools.domain.model.PROJECT_DOCUMENT_LABEL_MAX_LENGTH
import com.finnvek.knittools.domain.model.READING_LINE_MAX_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MIN_Y_FRACTION

internal object ProjectDocumentMigration22 {
    val migration =
        object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createTable(db)
                createIndexes(db)
                backfillDocuments(db)
                ProjectDocumentSchemaConstraints.create(db)
            }
        }

    private fun createTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `project_documents` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `projectId` INTEGER NOT NULL,
                `savedPatternId` INTEGER,
                `documentKey` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `localPdfUri` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `isPrimary` INTEGER NOT NULL,
                `currentPage` INTEGER NOT NULL,
                `rowMapping` TEXT,
                `readingLineEnabled` INTEGER NOT NULL,
                `readingLineYFraction` REAL NOT NULL,
                `readingLineFollowCurrentRow` INTEGER NOT NULL,
                `verticalReadingGuideEnabled` INTEGER NOT NULL,
                `verticalReadingGuideXFraction` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`savedPatternId`) REFERENCES `saved_patterns`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
    }

    private fun createIndexes(db: SupportSQLiteDatabase) {
        listOf(
            "CREATE INDEX IF NOT EXISTS `index_project_documents_project_order` " +
                "ON `project_documents` (`projectId`, `sortOrder`, `id`)",
            "CREATE INDEX IF NOT EXISTS `index_project_documents_project_primary` " +
                "ON `project_documents` (`projectId`, `isPrimary`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_documents_project_key` " +
                "ON `project_documents` (`projectId`, `documentKey`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_documents_project_uri` " +
                "ON `project_documents` (`projectId`, `localPdfUri`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_documents_project_saved_pattern` " +
                "ON `project_documents` (`projectId`, `savedPatternId`)",
            "CREATE INDEX IF NOT EXISTS `index_project_documents_savedPatternId` " +
                "ON `project_documents` (`savedPatternId`)",
            "CREATE INDEX IF NOT EXISTS `index_project_documents_localPdfUri` " +
                "ON `project_documents` (`localPdfUri`)",
        ).forEach(db::execSQL)
    }

    private fun backfillDocuments(db: SupportSQLiteDatabase) {
        db
            .query(
                """
                SELECT
                    id, linkedPatternId, patternUri, patternName, currentPatternPage,
                    patternRowMapping, readingLineEnabled, readingLineYFraction,
                    readingLineFollowCurrentRow, verticalReadingGuideEnabled,
                    verticalReadingGuideXFraction, createdAt, updatedAt
                FROM counter_projects
                WHERE TRIM(COALESCE(patternUri, '')) != ''
                ORDER BY id
                """.trimIndent(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    insertMigratedDocument(db, cursor)
                }
            }
    }

    private fun insertMigratedDocument(
        db: SupportSQLiteDatabase,
        cursor: Cursor,
    ) {
        val projectId = cursor.getLong(0)
        val localPdfUri = cursor.getString(2).trim()
        val matchingSavedPattern = findMatchingSavedPattern(db, cursor.longOrNull(1), localPdfUri)
        val documentKey = resolveDocumentKey(db, projectId, matchingSavedPattern?.id)
        val label = migratedLabel(cursor.stringOrNull(3), matchingSavedPattern?.name, localPdfUri)
        db.execSQL(
            """
            INSERT INTO project_documents (
                projectId, savedPatternId, documentKey, label, localPdfUri,
                sortOrder, isPrimary, currentPage, rowMapping,
                readingLineEnabled, readingLineYFraction,
                readingLineFollowCurrentRow, verticalReadingGuideEnabled,
                verticalReadingGuideXFraction, createdAt, updatedAt
            ) VALUES (?, ?, ?, ?, ?, 0, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                projectId,
                matchingSavedPattern?.id,
                documentKey,
                label,
                localPdfUri,
                cursor.getInt(4).coerceAtLeast(0),
                cursor.stringOrNull(5),
                cursor.getInt(6),
                sanitizeFraction(cursor.getFloat(7)),
                cursor.getInt(8),
                cursor.getInt(9),
                sanitizeFraction(cursor.getFloat(10)),
                cursor.getLong(11),
                cursor.getLong(12),
            ),
        )
    }

    private fun findMatchingSavedPattern(
        db: SupportSQLiteDatabase,
        linkedPatternId: Long?,
        localPdfUri: String,
    ): MatchingSavedPattern? {
        if (linkedPatternId == null) return null
        return db
            .query(
                """
                SELECT id, name FROM saved_patterns
                WHERE id = ?
                    AND TRIM(COALESCE(localPdfUri, '')) != ''
                    AND TRIM(localPdfUri) = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf<Any>(linkedPatternId, localPdfUri),
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    MatchingSavedPattern(cursor.getLong(0), cursor.getString(1))
                } else {
                    null
                }
            }
    }

    private fun resolveDocumentKey(
        db: SupportSQLiteDatabase,
        projectId: Long,
        savedPatternId: Long?,
    ): String {
        queryDocumentKey(
            db,
            """
            SELECT documentKey FROM pattern_annotation_layers
            WHERE projectId = ? AND isActive = 1 AND TRIM(documentKey) != ''
            ORDER BY createdAt, id LIMIT 1
            """.trimIndent(),
            arrayOf(projectId),
        )?.let { return it }
        queryDocumentKey(
            db,
            """
            SELECT documentKey FROM pattern_annotation_layers
            WHERE projectId = ? AND TRIM(documentKey) != ''
            ORDER BY createdAt, id LIMIT 1
            """.trimIndent(),
            arrayOf(projectId),
        )?.let { return it }
        if (savedPatternId != null) {
            queryDocumentKey(
                db,
                """
                SELECT documentKey FROM pattern_annotation_layers
                WHERE savedPatternId = ? AND TRIM(documentKey) != ''
                ORDER BY createdAt, id LIMIT 1
                """.trimIndent(),
                arrayOf(savedPatternId),
            )?.let { return it }
            return "saved:$savedPatternId:v1"
        }
        return "legacy-project:$projectId"
    }

    private fun queryDocumentKey(
        db: SupportSQLiteDatabase,
        sql: String,
        args: Array<out Any>,
    ): String? =
        db.query(sql, args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).trim().takeIf(String::isNotEmpty) else null
        }

    private fun migratedLabel(
        legacyName: String?,
        savedPatternName: String?,
        localPdfUri: String,
    ): String {
        val candidate =
            sequenceOf(
                legacyName,
                savedPatternName,
                decodedFileName(localPdfUri),
                "Pattern",
            ).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
                .first()
        return candidate.take(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
    }

    private fun decodedFileName(localPdfUri: String): String? =
        localPdfUri
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('/')
            .let(Uri::decode)
            .trim()
            .takeIf(String::isNotEmpty)

    private fun sanitizeFraction(value: Float): Float =
        if (value.isFinite()) {
            value.coerceIn(READING_LINE_MIN_Y_FRACTION, READING_LINE_MAX_Y_FRACTION)
        } else {
            0.5f
        }

    private fun android.database.Cursor.longOrNull(index: Int): Long? = if (isNull(index)) null else getLong(index)

    private fun android.database.Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private data class MatchingSavedPattern(
        val id: Long,
        val name: String,
    )
}
