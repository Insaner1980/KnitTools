package com.finnvek.knittools.di

import android.content.Context
import androidx.room.Room
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.YarnCardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DB_NAME = "knittools.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): KnitToolsDatabase =
        Room
            .databaseBuilder(context, KnitToolsDatabase::class.java, DB_NAME)
            .addMigrations(
                KnitToolsDatabase.MIGRATION_3_4,
                KnitToolsDatabase.MIGRATION_4_5,
                KnitToolsDatabase.MIGRATION_5_6,
                KnitToolsDatabase.MIGRATION_6_7,
                KnitToolsDatabase.MIGRATION_7_8,
                KnitToolsDatabase.MIGRATION_8_9,
            ).build()

    @Provides
    fun provideCounterProjectDao(db: KnitToolsDatabase): CounterProjectDao = db.counterProjectDao()

    @Provides
    fun provideYarnCardDao(db: KnitToolsDatabase): YarnCardDao = db.yarnCardDao()

    @Provides
    fun provideSessionDao(db: KnitToolsDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideRowReminderDao(db: KnitToolsDatabase): RowReminderDao = db.rowReminderDao()

    @Provides
    fun provideProgressPhotoDao(db: KnitToolsDatabase): ProgressPhotoDao = db.progressPhotoDao()

    @Provides
    fun provideProjectCounterDao(db: KnitToolsDatabase): ProjectCounterDao = db.projectCounterDao()

    @Provides
    fun provideSavedPatternDao(db: KnitToolsDatabase): SavedPatternDao = db.savedPatternDao()

    @Provides
    fun providePatternAnnotationDao(db: KnitToolsDatabase): PatternAnnotationDao = db.patternAnnotationDao()
}
