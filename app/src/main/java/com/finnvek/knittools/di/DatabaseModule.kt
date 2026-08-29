package com.finnvek.knittools.di

import android.content.Context
import androidx.room.Room
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationLayerDao
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.PatternBookmarkDao
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.ProjectFolderDao
import com.finnvek.knittools.data.local.ProjectYarnNoteDao
import com.finnvek.knittools.data.local.ProjectYarnUsageDao
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.RowReminderDao
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.time.AndroidSessionTimeSource
import com.finnvek.knittools.data.time.SessionTimeSource
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
            .addMigrations(*KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)
            .addCallback(PatternAnnotationSchemaConstraints.callback)
            .addCallback(ActiveSessionSchemaConstraints.callback)
            .addCallback(ProjectDocumentSchemaConstraints.callback)
            .build()

    @Provides
    @Singleton
    fun provideDatabaseTransactionRunner(database: KnitToolsDatabase): DatabaseTransactionRunner =
        RoomDatabaseTransactionRunner(database)

    @Provides
    @Singleton
    fun provideSessionTimeSource(
        @ApplicationContext context: Context,
    ): SessionTimeSource = AndroidSessionTimeSource(context)

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
    fun provideProjectYarnNoteDao(db: KnitToolsDatabase): ProjectYarnNoteDao = db.projectYarnNoteDao()

    @Provides
    fun provideSavedPatternDao(db: KnitToolsDatabase): SavedPatternDao = db.savedPatternDao()

    @Provides
    fun providePatternAnnotationLayerDao(db: KnitToolsDatabase): PatternAnnotationLayerDao =
        db.patternAnnotationLayerDao()

    @Provides
    fun providePatternAnnotationDao(db: KnitToolsDatabase): PatternAnnotationDao = db.patternAnnotationDao()

    @Provides
    fun providePatternBookmarkDao(db: KnitToolsDatabase): PatternBookmarkDao = db.patternBookmarkDao()

    @Provides
    fun provideProjectDocumentDao(db: KnitToolsDatabase): ProjectDocumentDao = db.projectDocumentDao()
}

@Module
@InstallIn(SingletonComponent::class)
object ProjectFolderDatabaseModule {
    @Provides
    fun provideProjectYarnUsageDao(db: KnitToolsDatabase): ProjectYarnUsageDao = db.projectYarnUsageDao()

    @Provides
    fun provideProjectFolderDao(db: KnitToolsDatabase): ProjectFolderDao = db.projectFolderDao()
}
