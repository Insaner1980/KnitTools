package com.finnvek.knittools.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PreservingSQLiteOpenHelperFactoryTest {
    private val context = mockk<Context>()
    private val database = mockk<SupportSQLiteDatabase>(relaxed = true)
    private val helper = mockk<SupportSQLiteOpenHelper>()
    private val callback =
        spyk(
            object : SupportSQLiteOpenHelper.Callback(25) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit

                override fun onDowngrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            },
        )
    private lateinit var forwarded: SupportSQLiteOpenHelper.Configuration

    private fun create(): SupportSQLiteOpenHelper {
        val delegate =
            SupportSQLiteOpenHelper.Factory {
                forwarded = it
                helper
            }
        return PreservingSQLiteOpenHelperFactory(delegate).create(
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name("synthetic.db")
                .callback(callback)
                .noBackupDirectory(true)
                .allowDataLossOnRecovery(true)
                .build(),
        )
    }

    @Test
    fun `normal lifecycle callbacks and database configuration are preserved without destructive recovery`() {
        assertSame(helper, create())
        assertSame(context, forwarded.context)
        assertEquals("synthetic.db", forwarded.name)
        assertEquals(callback.version, forwarded.callback.version)
        assertTrue(forwarded.useNoBackupDirectory)
        assertFalse(forwarded.allowDataLossOnRecovery)

        forwarded.callback.onConfigure(database)
        forwarded.callback.onCreate(database)
        forwarded.callback.onUpgrade(database, 24, 25)
        forwarded.callback.onDowngrade(database, 25, 24)
        forwarded.callback.onOpen(database)

        verifyOrder {
            callback.onConfigure(database)
            callback.onCreate(database)
            callback.onUpgrade(database, 24, 25)
            callback.onDowngrade(database, 25, 24)
            callback.onOpen(database)
        }
        val failure = IllegalStateException("synthetic migration failure")
        every { callback.onUpgrade(database, 24, 25) } throws failure
        val thrown =
            assertThrows(IllegalStateException::class.java) {
                forwarded.callback.onUpgrade(database, 24, 25)
            }
        assertSame(failure, thrown)
    }

    @Test
    fun `corruption closes the connection and fails without calling the deleting callback`() {
        create()
        assertThrows(IllegalStateException::class.java) { forwarded.callback.onCorruption(database) }
        verify(exactly = 1) { database.close() }
        verify(exactly = 0) { callback.onCorruption(any()) }
        verify(exactly = 0) { context.deleteDatabase(any()) }
    }

    @Test
    fun `failure to close still does not invoke destructive corruption handling`() {
        create()
        val failure = IllegalStateException("synthetic close failure")
        every { database.close() } throws failure
        val thrown =
            assertThrows(IllegalStateException::class.java) {
                forwarded.callback.onCorruption(database)
            }
        assertSame(failure, thrown)
        verify(exactly = 0) { callback.onCorruption(any()) }
        verify(exactly = 0) { context.deleteDatabase(any()) }
    }
}
