package com.finnvek.knittools.data.backup

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.data.local.KNITTOOLS_DATABASE_VERSION
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCoverageTest {
    @Test fun formatCoversEveryDurableRoomTable() {
        val schema =
            ProjectSourceFiles.read(
                "app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/$KNITTOOLS_DATABASE_VERSION.json",
            )
        val root =
            BackupFormat.json
                .parseToJsonElement(schema)
                .jsonObject
                .getValue("database")
                .jsonObject
        val tables =
            root
                .getValue("entities")
                .jsonArray
                .map {
                    it.jsonObject
                        .getValue("tableName")
                        .jsonPrimitive.content
                }.toSet()
        // Draft receipts belong to the installation-local onboarding state, not backed-up project content.
        assertEquals(setOf("onboarding_projects"), tables - BackupFormat.tables.toSet())
        assertEquals(tables - "onboarding_projects", BackupFormat.tables.toSet())
        assertFalse(BackupFormat.allowedPath("tables/onboarding_projects.jsonl"))
    }

    @Test fun deeplyNestedInputIsRejectedBeforeDeserialization() {
        assertThrows(BackupException::class.java) { BackupFormat.requireJsonDepth("[".repeat(100_000), 4) }
        BackupFormat.requireJsonDepth("""["text with [brackets]",1]""", 1)
    }
}
