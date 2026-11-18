package com.finnvek.knittools.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object ActiveSessionSchemaConstraints {
    private const val INSERT_TRIGGER = "active_sessions_require_singleton_id_insert"
    private const val UPDATE_TRIGGER = "active_sessions_require_singleton_id_update"

    fun create(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `$INSERT_TRIGGER`
            BEFORE INSERT ON `active_sessions`
            WHEN NEW.`singletonId` != 1
            BEGIN
                SELECT RAISE(ABORT, 'active_sessions singletonId must be 1');
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `$UPDATE_TRIGGER`
            BEFORE UPDATE OF `singletonId` ON `active_sessions`
            WHEN NEW.`singletonId` != 1
            BEGIN
                SELECT RAISE(ABORT, 'active_sessions singletonId must be 1');
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
