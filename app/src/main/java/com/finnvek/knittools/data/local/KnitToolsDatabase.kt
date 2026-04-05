package com.finnvek.knittools.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CounterProjectEntity::class,
        CounterHistoryEntity::class,
        YarnCardEntity::class,
        SessionEntity::class,
    ],
    version = 3,
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
}
