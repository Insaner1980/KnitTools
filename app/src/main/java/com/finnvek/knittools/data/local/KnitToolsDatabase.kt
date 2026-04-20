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
        RowReminderEntity::class,
        ProgressPhotoEntity::class,
        ProjectCounterEntity::class,
        SavedPatternEntity::class,
        PatternAnnotationEntity::class,
    ],
    version = 7,
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

    abstract fun savedPatternDao(): SavedPatternDao

    abstract fun patternAnnotationDao(): PatternAnnotationDao

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
                            `isCompleted` INTEGER NOT NULL DEFAULT 0,
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
                            `count` INTEGER NOT NULL DEFAULT 0,
                            `stepSize` INTEGER NOT NULL DEFAULT 1,
                            `repeatAt` INTEGER,
                            `sortOrder` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_project_counters_projectId` ON `project_counters` (`projectId`)",
                    )

                    // Backfill: siirretään secondaryCount-data project_counters-tauluun
                    db.execSQL(
                        """
                        INSERT INTO project_counters (projectId, name, count, stepSize, repeatAt, sortOrder, createdAt)
                        SELECT id, 'Pattern repeat', secondaryCount, stepSize, stepSize, 0, updatedAt
                        FROM counter_projects
                        WHERE secondaryCount > 0
                        """.trimIndent(),
                    )
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
                            `isFree` INTEGER NOT NULL DEFAULT 1,
                            `patternUrl` TEXT NOT NULL DEFAULT '',
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
    }
}
