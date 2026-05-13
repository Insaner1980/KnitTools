package com.finnvek.knittools.data.storage

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StorageFileNamesTest {
    @Test
    fun `timestamped file appends suffix when timestamp name already exists`() {
        val directory = tempDirectory()
        File(directory, "scan_42.jpg").writeText("old")

        val file =
            StorageFileNames.uniqueTimestampedFile(
                directory = directory,
                prefix = "scan_",
                extension = ".jpg",
                timestampMillis = 42L,
            )

        assertEquals("scan_42_1.jpg", file.name)
    }

    @Test
    fun `timestamped file skips existing suffixed names`() {
        val directory = tempDirectory()
        File(directory, "42.jpg").writeText("old")
        File(directory, "42_1.jpg").writeText("older")

        val file =
            StorageFileNames.uniqueTimestampedFile(
                directory = directory,
                prefix = "",
                extension = ".jpg",
                timestampMillis = 42L,
            )

        assertEquals("42_2.jpg", file.name)
    }

    private fun tempDirectory(): File = Files.createTempDirectory("storage-file-names").toFile()
}
