package com.finnvek.knittools.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Test

class KnitToolsDatabaseMigrationSourceTest {
    @Test
    fun `migration 3 to 4 does not duplicate legacy secondary counter into project counters`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val statements = mutableListOf<String>()
        every { db.execSQL(capture(statements)) } just Runs

        KnitToolsDatabase.MIGRATION_3_4.migrate(db)

        assertFalse(
            statements.any { statement ->
                statement.contains("INSERT INTO project_counters", ignoreCase = true)
            },
        )
    }
}
