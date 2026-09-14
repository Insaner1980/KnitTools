package com.finnvek.knittools.data.backup

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class BackupCoverageTest {
    @Test fun formatCoversEveryCurrentRoomTable() {
        val schema = File("schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/25.json")
        val root =
            BackupFormat.json
                .parseToJsonElement(schema.readText())
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
        assertEquals(tables, BackupFormat.tables.toSet())
    }

    @Test fun deeplyNestedInputIsRejectedBeforeDeserialization() {
        assertThrows(BackupException::class.java) { BackupFormat.requireJsonDepth("[".repeat(100_000), 4) }
        BackupFormat.requireJsonDepth("""["text with [brackets]",1]""", 1)
    }
}
