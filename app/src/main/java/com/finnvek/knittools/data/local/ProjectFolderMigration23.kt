package com.finnvek.knittools.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object ProjectFolderMigration23 {
    val migration =
        object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `normalizedName` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_folders_normalizedName` " +
                        "ON `project_folders` (`normalizedName`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_folder_assignments` (
                        `projectId` INTEGER NOT NULL,
                        `folderId` INTEGER NOT NULL,
                        PRIMARY KEY(`projectId`),
                        FOREIGN KEY(`projectId`) REFERENCES `counter_projects`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`folderId`) REFERENCES `project_folders`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_project_folder_assignments_folderId` " +
                        "ON `project_folder_assignments` (`folderId`)",
                )
            }
        }
}
