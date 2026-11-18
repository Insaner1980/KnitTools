package com.finnvek.knittools.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object ProjectCompletionMigration25 {
    val migration =
        object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `zoneId` TEXT,
                        FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_project_completions_projectId` ON `project_completions` (`projectId`)",
                )
                db.execSQL(
                    """
                    INSERT INTO project_completions (projectId, completedAt, zoneId)
                    SELECT id, completedAt, NULL FROM counter_projects
                    WHERE isCompleted = 1 AND completedAt > 0 AND completedAt >= createdAt
                    """.trimIndent(),
                )
            }
        }
}
