package com.finnvek.knittools.data.local

import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test

class ProjectDocumentMigration22Test {
    @Test
    fun `whitespace-only legacy URI does not create a document`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val projects = mockk<Cursor>(relaxed = true)
        val layers = mockk<Cursor>(relaxed = true)
        every { db.query(any<String>()) } returns projects
        every { db.query(any<String>(), any<Array<out Any?>>()) } returns layers
        every { projects.moveToNext() } returnsMany listOf(true, false)
        every { projects.getLong(0) } returns 1L
        every { projects.isNull(1) } returns true
        every { projects.getString(2) } returns "\t\n"
        every { projects.getString(3) } returns "Pattern"

        mockkStatic(Uri::class)
        try {
            every { Uri.decode("") } returns ""
            ProjectDocumentMigration22.migration.migrate(db)
        } finally {
            unmockkStatic(Uri::class)
        }

        verify(exactly = 0) {
            db.execSQL(match { it.trimStart().startsWith("INSERT INTO project_documents") }, any())
        }
    }
}
