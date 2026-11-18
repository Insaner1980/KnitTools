package com.finnvek.knittools.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object ProjectDocumentSchemaConstraints {
    fun create(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `project_documents_primary_insert`
            BEFORE INSERT ON `project_documents`
            WHEN NEW.isPrimary = 1 AND EXISTS (
                SELECT 1 FROM `project_documents`
                WHERE projectId = NEW.projectId AND isPrimary = 1
            )
            BEGIN
                SELECT RAISE(ABORT, 'project already has a primary document');
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `project_documents_primary_update`
            BEFORE UPDATE OF projectId, isPrimary ON `project_documents`
            WHEN NEW.isPrimary = 1 AND EXISTS (
                SELECT 1 FROM `project_documents`
                WHERE projectId = NEW.projectId AND isPrimary = 1 AND id != NEW.id
            )
            BEGIN
                SELECT RAISE(ABORT, 'project already has a primary document');
            END
            """.trimIndent(),
        )
    }

    val callback =
        object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                create(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                create(db)
            }
        }
}
