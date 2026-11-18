package com.finnvek.knittools.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DatabaseCorruptionPreservationTest {
    @Test
    fun validDatabaseReopensAndCorruptDatabaseIsNotReplaced() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val databaseName = "corruption-preservation-${UUID.randomUUID()}.db"
            val file = context.getDatabasePath(databaseName)

            fun open() =
                Room
                    .databaseBuilder(context, KnitToolsDatabase::class.java, databaseName)
                    .openHelperFactory(PreservingSQLiteOpenHelperFactory())
                    .build()

            try {
                val initial = open()
                val projectId =
                    try {
                        initial.counterProjectDao().insert(CounterProjectEntity(name = "Synthetic project"))
                    } finally {
                        initial.close()
                    }
                val reopened = open()
                try {
                    assertEquals("Synthetic project", reopened.counterProjectDao().getProject(projectId)?.name)
                } finally {
                    reopened.close()
                }

                val corruptBytes = ByteArray(4096) { 0x5a.toByte() }
                file.writeBytes(corruptBytes)
                repeat(2) {
                    val database = open()
                    try {
                        val failure = runCatching { database.openHelper.writableDatabase }.exceptionOrNull()
                        assertTrue("Corruption must fail instead of creating an empty database", failure != null)
                    } finally {
                        database.close()
                    }
                    assertTrue(file.exists())
                    assertArrayEquals(corruptBytes, file.readBytes())
                }
            } finally {
                context.deleteDatabase(databaseName)
            }
        }
}
