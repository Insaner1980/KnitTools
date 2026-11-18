package com.finnvek.knittools.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object ProjectYarnUsageMigration24 {
    val migration =
        object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_yarn_usage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `yarnCardId` INTEGER,
                        `projectYarnNoteId` INTEGER,
                        `sourceNameSnapshot` TEXT NOT NULL,
                        `plannedMeters` REAL,
                        `allocatedMeters` REAL,
                        `usedMeters` REAL,
                        `metersPerSkein` REAL,
                        `gramsPerSkein` REAL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`yarnCardId`) REFERENCES `yarn_cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`projectYarnNoteId`) REFERENCES `project_yarn_notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                listOf("yarnCardId", "projectYarnNoteId").forEach { source ->
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_yarn_usage_projectId_$source` " +
                            "ON `project_yarn_usage` (`projectId`, `$source`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_project_yarn_usage_$source` ON `project_yarn_usage` (`$source`)",
                    )
                }
            }
        }
}
