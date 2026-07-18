package com.finnvek.knittools.data.local

import android.graphics.Color
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject

internal object PatternAnnotationMigration17 {
    val migration =
        object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createLayerTable(db)
                migrateProjectLayers(db)
                createNewAnnotationTable(db)
                migrateLegacyAnnotations(db)
                replaceLegacyAnnotationTable(db)
                PatternAnnotationSchemaConstraints.create(db)
            }
        }

    private fun createLayerTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pattern_annotation_layers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `projectId` INTEGER,
                `savedPatternId` INTEGER,
                `documentKey` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`savedPatternId`) REFERENCES `saved_patterns`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
                `index_pattern_annotation_layers_projectId_documentKey`
            ON `pattern_annotation_layers` (`projectId`, `documentKey`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
                `index_pattern_annotation_layers_savedPatternId_documentKey`
            ON `pattern_annotation_layers` (`savedPatternId`, `documentKey`)
            """.trimIndent(),
        )
    }

    private fun migrateProjectLayers(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `pattern_annotation_layers` (
                projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
            )
            SELECT
                projects.id,
                NULL,
                CASE
                    WHEN projects.linkedPatternId IS NOT NULL
                        THEN 'saved:' || projects.linkedPatternId || ':v1'
                    ELSE 'legacy-project:' || projects.id
                END,
                CASE
                    WHEN projects.patternUri IS NOT NULL OR projects.linkedPatternId IS NOT NULL THEN 1
                    ELSE 0
                END,
                MIN(annotations.createdAt),
                MAX(annotations.createdAt)
            FROM `pattern_annotations` AS annotations
            INNER JOIN `counter_projects` AS projects ON projects.id = annotations.projectId
            GROUP BY projects.id
            """.trimIndent(),
        )
    }

    private fun createNewAnnotationTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pattern_annotations_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `layerId` INTEGER NOT NULL,
                `page` INTEGER NOT NULL,
                `kind` TEXT NOT NULL,
                `payloadVersion` INTEGER NOT NULL,
                `payloadJson` TEXT NOT NULL,
                `zIndex` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`layerId`) REFERENCES `pattern_annotation_layers`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }

    private fun migrateLegacyAnnotations(db: SupportSQLiteDatabase) {
        val cursor =
            db.query(
                """
                SELECT
                    annotations.id,
                    layers.id,
                    annotations.page,
                    annotations.pathData,
                    annotations.color,
                    annotations.strokeWidth,
                    annotations.createdAt
                FROM `pattern_annotations` AS annotations
                INNER JOIN `pattern_annotation_layers` AS layers
                    ON layers.projectId = annotations.projectId
                ORDER BY layers.id, annotations.page, annotations.createdAt, annotations.id
                """.trimIndent(),
            )
        var previousLayerId = Long.MIN_VALUE
        var previousPage = Int.MIN_VALUE
        var zIndex = 0L
        cursor.use {
            while (it.moveToNext()) {
                val layerId = it.getLong(1)
                val page = it.getInt(2)
                if (layerId != previousLayerId || page != previousPage) {
                    zIndex = 0L
                    previousLayerId = layerId
                    previousPage = page
                }
                val createdAt = it.getLong(6)
                db.execSQL(
                    """
                    INSERT INTO `pattern_annotations_new` (
                        id, layerId, page, kind, payloadVersion, payloadJson,
                        zIndex, createdAt, updatedAt
                    ) VALUES (?, ?, ?, 'FREEHAND', 1, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        it.getLong(0),
                        layerId,
                        page,
                        legacyPayloadJson(
                            pathData = it.getString(3),
                            color = it.getString(4),
                            strokeWidth = it.getFloat(5),
                        ),
                        zIndex,
                        createdAt,
                        createdAt,
                    ),
                )
                zIndex += 1L
            }
        }
    }

    private fun replaceLegacyAnnotationTable(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE `pattern_annotations`")
        db.execSQL("ALTER TABLE `pattern_annotations_new` RENAME TO `pattern_annotations`")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_pattern_annotations_layerId_page_zIndex`
            ON `pattern_annotations` (`layerId`, `page`, `zIndex`)
            """.trimIndent(),
        )
    }

    private fun legacyPayloadJson(
        pathData: String,
        color: String,
        strokeWidth: Float,
    ): String {
        val safeStrokeWidth = strokeWidth.takeIf(Float::isFinite) ?: 1f
        val argb = runCatching { Color.parseColor(color) }.getOrDefault(Color.BLACK)
        return """
            {
                "points":[],
                "argb":$argb,
                "strokeWidth":$safeStrokeWidth,
                "pressureEnabled":false,
                "legacyPathData":${JSONObject.quote(pathData)},
                "legacyColor":${JSONObject.quote(color)}
            }
            """.trimIndent()
    }
}

internal object PatternAnnotationSchemaConstraints {
    val callback =
        object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                create(db)
            }
        }

    fun create(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `pattern_annotation_layers_owner_insert`
            BEFORE INSERT ON `pattern_annotation_layers`
            WHEN
                (NEW.projectId IS NULL AND NEW.savedPatternId IS NULL) OR
                (NEW.projectId IS NOT NULL AND NEW.savedPatternId IS NOT NULL)
            BEGIN
                SELECT RAISE(ABORT, 'pattern annotation layer requires exactly one owner');
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `pattern_annotation_layers_owner_update`
            BEFORE UPDATE OF projectId, savedPatternId ON `pattern_annotation_layers`
            WHEN
                (NEW.projectId IS NULL AND NEW.savedPatternId IS NULL) OR
                (NEW.projectId IS NOT NULL AND NEW.savedPatternId IS NOT NULL)
            BEGIN
                SELECT RAISE(ABORT, 'pattern annotation layer requires exactly one owner');
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `pattern_annotation_layers_active_insert`
            BEFORE INSERT ON `pattern_annotation_layers`
            WHEN
                NEW.projectId IS NOT NULL AND
                NEW.isActive = 1 AND
                EXISTS (
                    SELECT 1 FROM `pattern_annotation_layers`
                    WHERE projectId = NEW.projectId AND isActive = 1
                )
            BEGIN
                SELECT RAISE(ABORT, 'project already has an active annotation layer');
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `pattern_annotation_layers_active_update`
            BEFORE UPDATE OF projectId, isActive ON `pattern_annotation_layers`
            WHEN
                NEW.projectId IS NOT NULL AND
                NEW.isActive = 1 AND
                EXISTS (
                    SELECT 1 FROM `pattern_annotation_layers`
                    WHERE projectId = NEW.projectId AND isActive = 1 AND id != NEW.id
                )
            BEGIN
                SELECT RAISE(ABORT, 'project already has an active annotation layer');
            END
            """.trimIndent(),
        )
    }
}
