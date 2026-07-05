package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationBackfillSourceTest {
    @Test
    fun `migration 9 to 10 clamps rows worked backfill to Int max`() {
        val database = ProjectSourceFiles.read(DATABASE)

        assertTrue(
            database.contains("WHEN endRow - startRow > \${Int.MAX_VALUE} THEN \${Int.MAX_VALUE}"),
        )
    }

    private companion object {
        private const val DATABASE =
            "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
    }
}
