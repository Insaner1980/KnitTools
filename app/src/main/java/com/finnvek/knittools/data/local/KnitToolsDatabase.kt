package com.finnvek.knittools.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CounterProjectEntity::class, CounterHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class KnitToolsDatabase : RoomDatabase() {
    abstract fun counterProjectDao(): CounterProjectDao
}
