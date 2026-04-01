package com.finnvek.knittools.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CounterProjectEntity::class,
        CounterHistoryEntity::class,
        YarnCardEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class KnitToolsDatabase : RoomDatabase() {
    abstract fun counterProjectDao(): CounterProjectDao

    abstract fun yarnCardDao(): YarnCardDao
}
