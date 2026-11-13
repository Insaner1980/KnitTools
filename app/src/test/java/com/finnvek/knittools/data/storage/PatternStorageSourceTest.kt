package com.finnvek.knittools.data.storage

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class PatternStorageSourceTest {
    @Test
    fun `FileProvider exposes only owned photo and capture directories`() {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.read(FILE_PATHS).byteInputStream())
        val children = document.documentElement.childNodes
        val paths =
            (0 until children.length)
                .mapNotNull { children.item(it) as? Element }
                .map { Triple(it.tagName, it.getAttribute("name"), it.getAttribute("path")) }
        assertEquals(
            listOf(
                Triple("files-path", "progress_photos", "progress_photos/"),
                Triple("files-path", "pattern_captures", "pattern_captures/"),
            ),
            paths,
        )
    }

    private companion object {
        private const val FILE_PATHS = "app/src/main/res/xml/file_paths.xml"
    }
}
