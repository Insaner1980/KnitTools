package com.finnvek.knittools.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PatternDocumentFilesTest {
    @Test
    fun `safe PDF name removes path segments and control characters`() {
        val name = PatternDocumentFiles.safePdfFileName("../folder\\evil\u0000 name")

        assertEquals("evil name.pdf", name)
        assertFalse(name.contains('/'))
        assertFalse(name.contains('\\'))
        assertFalse(name.contains(".."))
    }

    @Test
    fun `safe PDF name falls back for blank names`() {
        val name = PatternDocumentFiles.safePdfFileName("   ", "pattern")

        assertEquals("pattern.pdf", name)
    }

    @Test
    fun `safe PDF name truncates very long names`() {
        val name = PatternDocumentFiles.safePdfFileName("${"a".repeat(300)}.pdf")

        assertEquals("${"a".repeat(176)}.pdf", name)
        assertTrue(name.length <= 180)
    }

    @Test
    fun `safe PDF name normalizes uppercase extension`() {
        assertEquals("Pattern.pdf", PatternDocumentFiles.safePdfFileName("Pattern.PDF"))
    }

    @Test
    fun `safe PDF name avoids reserved device names`() {
        assertEquals("CON_.pdf", PatternDocumentFiles.safePdfFileName("CON.pdf"))
        assertEquals("com1.txt_.pdf", PatternDocumentFiles.safePdfFileName("com1.txt"))
    }

    @Test
    fun `write unique PDF does not overwrite an existing file`() {
        val directory = tempDirectory()
        val existing = File(directory, "pattern.pdf").apply { writeText("old") }

        val written =
            PatternDocumentFiles.writeUniquePdf(directory, "pattern.pdf") { file ->
                file.writeText("new")
            }

        assertEquals("old", existing.readText())
        assertEquals("pattern-1.pdf", written?.name)
        assertEquals("new", written?.readText())
    }

    @Test
    fun `write unique PDF keeps duplicate suffix inside max file name length`() {
        val directory = tempDirectory()
        val existingName = "${"a".repeat(176)}.pdf"
        File(directory, existingName).writeText("old")

        val written =
            PatternDocumentFiles.writeUniquePdf(directory, "${"a".repeat(300)}.pdf") { file ->
                file.writeText("new")
            }

        assertEquals("${"a".repeat(174)}-1.pdf", written?.name)
        assertTrue(written?.name.orEmpty().length <= 180)
    }

    @Test
    fun `write unique PDF uses another name when target is created during write`() {
        val directory = tempDirectory()
        val target = File(directory, "pattern.pdf")

        val written =
            PatternDocumentFiles.writeUniquePdf(directory, "pattern.pdf") { file ->
                target.writeText("raced")
                file.writeText("new")
            }

        assertEquals("pattern-1.pdf", written?.name)
        assertEquals("raced", target.readText())
        assertEquals("new", written?.readText())
    }

    @Test
    fun `write unique PDF removes temporary file when writing fails`() {
        val directory = tempDirectory()

        val written =
            PatternDocumentFiles.writeUniquePdf(directory, "broken.pdf") {
                error("copy failed")
            }

        assertNull(written)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    private fun tempDirectory(): File = Files.createTempDirectory("pattern-documents").toFile()
}
