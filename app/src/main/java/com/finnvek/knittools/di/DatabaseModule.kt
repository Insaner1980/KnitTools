package com.finnvek.knittools.di

import android.content.Context
import androidx.room.Room
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.KnitToolsDatabase
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
            .build()

    @Provides
    fun provideCounterProjectDao(db: KnitToolsDatabase): CounterProjectDao =
        db.counterProjectDao()
}
