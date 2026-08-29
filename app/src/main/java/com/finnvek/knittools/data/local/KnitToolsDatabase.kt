package com.finnvek.knittools.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CounterProjectEntity::class,
        CounterHistoryEntity::class,
        YarnCardEntity::class,
        SessionEntity::class,
        ActiveSessionEntity::class,
        RowReminderEntity::class,
        ProgressPhotoEntity::class,
        ProjectCounterEntity::class,
        ProjectYarnNoteEntity::class,
        SavedPatternEntity::class,
        PatternAnnotationLayerEntity::class,
        PatternAnnotationEntity::class,
        PatternBookmarkEntity::class,
        ProjectDocumentEntity::class,
        ProjectFolderEntity::class,
        ProjectFolderAssignmentEntity::class,
        ProjectYarnUsageEntity::class,
    ],
    version = 24,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class KnitToolsDatabase : RoomDatabase() {
    abstract fun counterProjectDao(): CounterProjectDao

    abstract fun yarnCardDao(): YarnCardDao

    abstract fun sessionDao(): SessionDao

    abstract fun rowReminderDao(): RowReminderDao

    abstract fun progressPhotoDao(): ProgressPhotoDao

    abstract fun projectCounterDao(): ProjectCounterDao

    abstract fun projectYarnNoteDao(): ProjectYarnNoteDao

    abstract fun savedPatternDao(): SavedPatternDao

    abstract fun patternAnnotationLayerDao(): PatternAnnotationLayerDao

    abstract fun patternAnnotationDao(): PatternAnnotationDao

    abstract fun patternBookmarkDao(): PatternBookmarkDao

    abstract fun projectDocumentDao(): ProjectDocumentDao

    abstract fun projectFolderDao(): ProjectFolderDao

    abstract fun projectYarnUsageDao(): ProjectYarnUsageDao

    companion object {
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Uudet taulut
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `row_reminders` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `targetRow` INTEGER NOT NULL,
                            `repeatInterval` INTEGER,
                            `message` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_row_reminders_projectId` ON `row_reminders` (`projectId`)",
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `progress_photos` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `photoUri` TEXT NOT NULL,
                            `rowNumber` INTEGER NOT NULL,
                            `note` TEXT,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_progress_photos_projectId` ON `progress_photos` (`projectId`)",
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `project_counters` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `count` INTEGER NOT NULL,
                            `stepSize` INTEGER NOT NULL,
                            `repeatAt` INTEGER,
                            `sortOrder` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_project_counters_projectId` ON `project_counters` (`projectId`)",
                    )

                    // Legacy secondaryCount pysyy counter_projects-taulun omana kenttänä.
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `saved_patterns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `ravelryId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `designerName` TEXT NOT NULL,
                            `thumbnailUrl` TEXT,
                            `difficulty` REAL,
                            `gaugeStitches` REAL,
                            `gaugeRows` REAL,
                            `needleSize` TEXT,
                            `yarnWeight` TEXT,
                            `yardage` INTEGER,
                            `isFree` INTEGER NOT NULL,
                            `patternUrl` TEXT NOT NULL,
                            `savedAt` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "ALTER TABLE counter_projects ADD COLUMN linkedPatternId INTEGER DEFAULT NULL",
                    )
                }
            }

        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Yarn Stash -laajennus: määrä, status, linkitetty projekti
                    db.execSQL(
                        "ALTER TABLE yarn_cards ADD COLUMN quantityInStash INTEGER NOT NULL DEFAULT 1",
                    )
                    db.execSQL(
                        "ALTER TABLE yarn_cards ADD COLUMN status TEXT NOT NULL DEFAULT 'IN_STASH'",
                    )
                    db.execSQL(
                        "ALTER TABLE yarn_cards ADD COLUMN linkedProjectId INTEGER DEFAULT NULL",
                    )

                    // Shaping Counter -laajennus: laskurityyppi ja muotoilukentät
                    db.execSQL(
                        "ALTER TABLE project_counters ADD COLUMN counterType TEXT NOT NULL DEFAULT 'COUNT_UP'",
                    )
                    db.execSQL(
                        "ALTER TABLE project_counters ADD COLUMN startingStitches INTEGER DEFAULT NULL",
                    )
                    db.execSQL(
                        "ALTER TABLE project_counters ADD COLUMN stitchChange INTEGER DEFAULT NULL",
                    )
                    db.execSQL(
                        "ALTER TABLE project_counters ADD COLUMN shapeEveryN INTEGER DEFAULT NULL",
                    )

                    // Backfill: olemassa olevat repeatAt-laskurit → REPEATING-tyyppi
                    db.execSQL(
                        "UPDATE project_counters SET counterType = 'REPEATING' WHERE repeatAt IS NOT NULL",
                    )
                }
            }

        val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN patternUri TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN patternName TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN currentPatternPage INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN patternRowMapping TEXT DEFAULT NULL")
                    db.execSQL(
                        "ALTER TABLE counter_projects ADD COLUMN stitchTrackingEnabled INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN currentStitch INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE project_counters ADD COLUMN repeatStartRow INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE project_counters ADD COLUMN repeatEndRow INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE project_counters ADD COLUMN totalRepeats INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE project_counters ADD COLUMN currentRepeat INTEGER DEFAULT NULL")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `pattern_annotations` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `page` INTEGER NOT NULL,
                            `pathData` TEXT NOT NULL,
                            `color` TEXT NOT NULL,
                            `strokeWidth` REAL NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_pattern_annotations_projectId` ON `pattern_annotations` (`projectId`)",
                    )
                }
            }

        val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN targetRows INTEGER DEFAULT NULL")
                }
            }

        val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_startedAt` ON `sessions` (`startedAt`)")
                }
            }

        val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE sessions ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE sessions ADD COLUMN rowsWorked INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE sessions SET durationSeconds = durationMinutes * 60")
                    db.execSQL(
                        """
                        UPDATE sessions
                        SET rowsWorked = CASE
                            WHEN endRow <= startRow THEN 0
                            WHEN endRow - startRow > ${Int.MAX_VALUE} THEN ${Int.MAX_VALUE}
                            ELSE endRow - startRow
                        END
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_10_11 =
            object : Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_sessions_endedAt_startedAt` " +
                            "ON `sessions` (`endedAt`, `startedAt`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_sessions_projectId_endedAt_startedAt` " +
                            "ON `sessions` (`projectId`, `endedAt`, `startedAt`)",
                    )
                }
            }

        val MIGRATION_11_12 =
            object : Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `project_yarn_notes` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL DEFAULT 1,
                            `notes` TEXT NOT NULL,
                            `savedYarnCardId` INTEGER DEFAULT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_project_yarn_notes_projectId` " +
                            "ON `project_yarn_notes` (`projectId`)",
                    )
                }
            }

        val MIGRATION_12_13 =
            object : Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN craftType TEXT NOT NULL DEFAULT 'KNITTING'")
                    db.execSQL(
                        "ALTER TABLE counter_projects ADD COLUMN mainCounterLabelType TEXT NOT NULL DEFAULT 'ROWS'",
                    )
                    db.execSQL("ALTER TABLE counter_projects ADD COLUMN mainCounterCustomLabel TEXT DEFAULT NULL")
                    db.execSQL(
                        "ALTER TABLE counter_projects ADD COLUMN readingLineEnabled INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL(
                        "ALTER TABLE counter_projects ADD COLUMN readingLineYFraction REAL NOT NULL DEFAULT 0.5",
                    )
                    db.execSQL(
                        "ALTER TABLE project_counters ADD COLUMN linkedToMainCounter INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        val MIGRATION_13_14 =
            object : Migration(13, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `saved_patterns_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `source` TEXT NOT NULL,
                            `ravelryPatternId` INTEGER,
                            `name` TEXT NOT NULL,
                            `designerName` TEXT NOT NULL,
                            `thumbnailUrl` TEXT,
                            `difficulty` REAL,
                            `gaugeStitches` REAL,
                            `gaugeRows` REAL,
                            `needleSize` TEXT,
                            `yarnWeight` TEXT,
                            `yardage` INTEGER,
                            `isFree` INTEGER NOT NULL,
                            `originalUrl` TEXT NOT NULL,
                            `canonicalUrl` TEXT NOT NULL,
                            `localPdfUri` TEXT,
                            `isAvailableOffline` INTEGER NOT NULL,
                            `savedAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `lastSyncedAt` INTEGER
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `saved_patterns_new` (
                            id, source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                            gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage, isFree,
                            originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                            savedAt, updatedAt, lastSyncedAt
                        )
                        SELECT
                            id,
                            CASE
                                WHEN ravelryId > 0 THEN 'RAVELRY'
                                WHEN patternUrl LIKE 'content://%' OR patternUrl LIKE 'file://%' THEN 'LOCAL_FILE'
                                ELSE 'OTHER'
                            END,
                            CASE WHEN ravelryId > 0 THEN ravelryId ELSE NULL END,
                            name,
                            designerName,
                            thumbnailUrl,
                            difficulty,
                            gaugeStitches,
                            gaugeRows,
                            needleSize,
                            yarnWeight,
                            yardage,
                            isFree,
                            patternUrl,
                            CASE WHEN ravelryId > 0 THEN patternUrl ELSE '' END,
                            CASE
                                WHEN patternUrl LIKE 'content://%' OR patternUrl LIKE 'file://%' THEN patternUrl
                                ELSE NULL
                            END,
                            CASE
                                WHEN patternUrl LIKE 'content://%' OR patternUrl LIKE 'file://%' THEN 1
                                ELSE 0
                            END,
                            savedAt,
                            savedAt,
                            NULL
                        FROM `saved_patterns`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `saved_patterns`")
                    db.execSQL("ALTER TABLE `saved_patterns_new` RENAME TO `saved_patterns`")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_ravelryPatternId` " +
                            "ON `saved_patterns` (`ravelryPatternId`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_canonicalUrl` " +
                            "ON `saved_patterns` (`canonicalUrl`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_originalUrl` " +
                            "ON `saved_patterns` (`originalUrl`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_localPdfUri` " +
                            "ON `saved_patterns` (`localPdfUri`)",
                    )
                }
            }

        val MIGRATION_14_15 =
            object : Migration(14, 15) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_counter_projects_linkedPatternId` " +
                            "ON `counter_projects` (`linkedPatternId`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_yarn_cards_linkedProjectId` " +
                            "ON `yarn_cards` (`linkedProjectId`)",
                    )
                }
            }

        val MIGRATION_15_16 =
            object : Migration(15, 16) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE sessions ADD COLUMN zoneId TEXT")
                }
            }

        val MIGRATION_16_17: Migration = PatternAnnotationMigration17.migration

        val MIGRATION_17_18 =
            object : Migration(17, 18) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE counter_projects " +
                            "ADD COLUMN secondaryCounterUsed INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL(
                        "ALTER TABLE counter_projects " +
                            "ADD COLUMN notesCreated INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL(
                        "UPDATE counter_projects SET secondaryCounterUsed = 1, notesCreated = 1",
                    )
                }
            }

        val MIGRATION_18_19 =
            object : Migration(18, 19) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TEMP TABLE `saved_pattern_layer_backup` (
                            `id` INTEGER PRIMARY KEY NOT NULL,
                            `projectId` INTEGER,
                            `savedPatternId` INTEGER,
                            `documentKey` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `saved_pattern_layer_backup` (
                            id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                        )
                        SELECT
                            id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                        FROM `pattern_annotation_layers`
                        WHERE savedPatternId IS NOT NULL
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TEMP TABLE `saved_pattern_annotation_backup` (
                            `id` INTEGER PRIMARY KEY NOT NULL,
                            `layerId` INTEGER NOT NULL,
                            `page` INTEGER NOT NULL,
                            `kind` TEXT NOT NULL,
                            `payloadVersion` INTEGER NOT NULL,
                            `payloadJson` TEXT NOT NULL,
                            `zIndex` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `saved_pattern_annotation_backup` (
                            id, layerId, page, kind, payloadVersion, payloadJson,
                            zIndex, createdAt, updatedAt
                        )
                        SELECT
                            annotations.id,
                            annotations.layerId,
                            annotations.page,
                            annotations.kind,
                            annotations.payloadVersion,
                            annotations.payloadJson,
                            annotations.zIndex,
                            annotations.createdAt,
                            annotations.updatedAt
                        FROM `pattern_annotations` AS annotations
                        INNER JOIN `saved_pattern_layer_backup` AS layers
                            ON layers.id = annotations.layerId
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        DELETE FROM `pattern_annotations`
                        WHERE layerId IN (SELECT id FROM `saved_pattern_layer_backup`)
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        DELETE FROM `pattern_annotation_layers`
                        WHERE id IN (SELECT id FROM `saved_pattern_layer_backup`)
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `saved_patterns_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `source` TEXT NOT NULL,
                            `ravelryPatternId` INTEGER,
                            `name` TEXT NOT NULL,
                            `designerName` TEXT NOT NULL,
                            `thumbnailUrl` TEXT,
                            `difficulty` REAL,
                            `gaugeStitches` REAL,
                            `gaugeRows` REAL,
                            `needleSize` TEXT,
                            `yarnWeight` TEXT,
                            `yardage` INTEGER,
                            `availability` TEXT NOT NULL,
                            `originalUrl` TEXT NOT NULL,
                            `canonicalUrl` TEXT NOT NULL,
                            `localPdfUri` TEXT,
                            `isAvailableOffline` INTEGER NOT NULL,
                            `savedAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `lastSyncedAt` INTEGER
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `saved_patterns_new` (
                            id, source, ravelryPatternId, name, designerName, thumbnailUrl, difficulty,
                            gaugeStitches, gaugeRows, needleSize, yarnWeight, yardage, availability,
                            originalUrl, canonicalUrl, localPdfUri, isAvailableOffline,
                            savedAt, updatedAt, lastSyncedAt
                        )
                        SELECT
                            id,
                            source,
                            ravelryPatternId,
                            name,
                            designerName,
                            thumbnailUrl,
                            difficulty,
                            gaugeStitches,
                            gaugeRows,
                            needleSize,
                            yarnWeight,
                            yardage,
                            CASE WHEN isFree = 1 THEN 'free' ELSE 'unknown' END,
                            originalUrl,
                            canonicalUrl,
                            localPdfUri,
                            isAvailableOffline,
                            savedAt,
                            updatedAt,
                            lastSyncedAt
                        FROM `saved_patterns`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `saved_patterns`")
                    db.execSQL("ALTER TABLE `saved_patterns_new` RENAME TO `saved_patterns`")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_ravelryPatternId` " +
                            "ON `saved_patterns` (`ravelryPatternId`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_canonicalUrl` " +
                            "ON `saved_patterns` (`canonicalUrl`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_originalUrl` " +
                            "ON `saved_patterns` (`originalUrl`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_saved_patterns_localPdfUri` " +
                            "ON `saved_patterns` (`localPdfUri`)",
                    )
                    db.execSQL(
                        """
                        INSERT INTO `pattern_annotation_layers` (
                            id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                        )
                        SELECT
                            id, projectId, savedPatternId, documentKey, isActive, createdAt, updatedAt
                        FROM `saved_pattern_layer_backup`
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `pattern_annotations` (
                            id, layerId, page, kind, payloadVersion, payloadJson,
                            zIndex, createdAt, updatedAt
                        )
                        SELECT
                            id, layerId, page, kind, payloadVersion, payloadJson,
                            zIndex, createdAt, updatedAt
                        FROM `saved_pattern_annotation_backup`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `saved_pattern_annotation_backup`")
                    db.execSQL("DROP TABLE `saved_pattern_layer_backup`")
                }
            }

        val MIGRATION_19_20 =
            object : Migration(19, 20) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE counter_projects " +
                            "ADD COLUMN verticalReadingGuideEnabled INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL(
                        "ALTER TABLE counter_projects " +
                            "ADD COLUMN verticalReadingGuideXFraction REAL NOT NULL DEFAULT 0.5",
                    )
                    db.execSQL(
                        "ALTER TABLE counter_projects " +
                            "ADD COLUMN readingLineFollowCurrentRow INTEGER NOT NULL DEFAULT 1",
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `pattern_bookmarks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `documentKey` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `pageIndex` INTEGER NOT NULL,
                            `yFraction` REAL NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_pattern_bookmarks_project_document_position` " +
                            "ON `pattern_bookmarks` " +
                            "(`projectId`, `documentKey`, `pageIndex`, `yFraction`, `createdAt`, `id`)",
                    )
                }
            }

        val MIGRATION_20_21 =
            object : Migration(20, 21) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `active_sessions` (
                            `singletonId` INTEGER NOT NULL,
                            `sessionToken` TEXT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `startedAtWallMillis` INTEGER NOT NULL,
                            `startZoneId` TEXT NOT NULL,
                            `startRow` INTEGER NOT NULL,
                            `lastObservedRow` INTEGER NOT NULL,
                            `trustedLastObservedRow` INTEGER NOT NULL,
                            `trustedRowsWorked` INTEGER NOT NULL,
                            `pendingRowsWorked` INTEGER NOT NULL,
                            `reviewedRowsWorked` INTEGER NOT NULL,
                            `reviewedLastObservedRow` INTEGER NOT NULL,
                            `unreviewedRowsWorked` INTEGER NOT NULL,
                            `checkpointedDurationSeconds` INTEGER NOT NULL,
                            `reviewedDurationBaselineSeconds` INTEGER NOT NULL,
                            `segmentStartedAtWallMillis` INTEGER NOT NULL,
                            `segmentStartedElapsedRealtimeMillis` INTEGER NOT NULL,
                            `bootCount` INTEGER,
                            `recoveryReason` TEXT,
                            `recoveryIntervalToken` TEXT,
                            `recoverySuggestedDurationSeconds` INTEGER,
                            `recoveryPromptShown` INTEGER NOT NULL,
                            `updatedAtWallMillis` INTEGER NOT NULL,
                            PRIMARY KEY(`singletonId`),
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_active_sessions_projectId` " +
                            "ON `active_sessions` (`projectId`)",
                    )
                    ActiveSessionSchemaConstraints.create(db)
                }
            }

        val MIGRATION_21_22: Migration = ProjectDocumentMigration22.migration

        val MIGRATION_22_23: Migration = ProjectFolderMigration23.migration

        val MIGRATION_23_24: Migration = ProjectYarnUsageMigration24.migration

        val ALL_MANUAL_MIGRATIONS: Array<Migration>
            get() =
                arrayOf(
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                )
    }
}
