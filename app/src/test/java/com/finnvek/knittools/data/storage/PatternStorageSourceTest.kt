package com.finnvek.knittools.data.storage

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternStorageSourceTest {
    @Test
    fun `FileProvider does not expose the whole pattern document tree`() {
        val paths = ProjectSourceFiles.read(FILE_PATHS)

        assertFalse(paths.contains("path=\"patterns/\""))
        assertTrue(paths.contains("path=\"pattern_captures/\""))
    }

    private companion object {
        private const val FILE_PATHS = "app/src/main/res/xml/file_paths.xml"
    }
}
