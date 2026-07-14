package com.finnvek.knittools.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTransactionRunnerTest {
    private lateinit var database: KnitToolsDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun transactionRollsBackEarlierLinkWriteWhenLaterWriteFails() =
        runTest {
            val projectDao = database.counterProjectDao()
            projectDao.insert(CounterProjectEntity(id = 10L, yarnCardIds = "1"))
            val runner = RoomDatabaseTransactionRunner(database)

            val failure =
                runCatching {
                    runner.run {
                        projectDao.updateYarnCardIds(id = 10L, yarnCardIds = "1,5", updatedAt = 2L)
                        error("Myöhempi linkkikirjoitus epäonnistui")
                    }
                }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertEquals("1", projectDao.getProject(10L)?.yarnCardIds)
        }
}
