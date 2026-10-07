package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectThumbnailQueryTest {
    @Test
    fun latestPhotoPerProjectHasStableTieBreakAndSurvivesDeletion() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
            try {
                val first = database.counterProjectDao().insert(CounterProjectEntity(name = "Active"))
                val second =
                    database.counterProjectDao().insert(
                        CounterProjectEntity(name = "Completed", isCompleted = true),
                    )
                val photos = database.progressPhotoDao()
                val old =
                    photos.insert(
                        ProgressPhotoEntity(
                            projectId = first,
                            photoUri = "file:///old.jpg",
                            rowNumber = 1,
                            createdAt = 100,
                        ),
                    )
                val latest =
                    photos.insert(
                        ProgressPhotoEntity(
                            projectId = first,
                            photoUri = "file:///new.jpg",
                            rowNumber = 2,
                            createdAt = 100,
                        ),
                    )
                val completed =
                    photos.insert(
                        ProgressPhotoEntity(
                            projectId = second,
                            photoUri = "file:///complete.jpg",
                            rowNumber = 3,
                            createdAt = 90,
                        ),
                    )
                assertEquals(
                    setOf(latest, completed),
                    photos
                        .observeLatestPhotosPerProject()
                        .first()
                        .map { it.id }
                        .toSet(),
                )
                photos.delete(latest)
                assertEquals(
                    setOf(old, completed),
                    photos
                        .observeLatestPhotosPerProject()
                        .first()
                        .map { it.id }
                        .toSet(),
                )
            } finally {
                database.close()
            }
        }
}
