package com.finnvek.knittools.repository

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingLinePersistenceSourceTest {
    @Test
    fun `counter repository persists clamped reading line state through active document`() {
        val repository = ProjectSourceFiles.read(REPOSITORY)

        assertTrue(repository.contains("suspend fun updateReadingLine("))
        assertTrue(repository.contains("val sanitizedYFraction ="))
        assertTrue(repository.contains("sanitizeReadingLineYFraction(yFraction)"))
        assertTrue(repository.contains("projectDocumentRepository.getActiveDocument(id)"))
        assertTrue(repository.contains("projectDocumentRepository.updateViewerStateInTransaction("))
    }

    @Test
    fun `project document dao has focused viewer state update query`() {
        val dao = ProjectSourceFiles.read(DAO)

        assertTrue(dao.contains("UPDATE project_documents SET"))
        assertTrue(dao.contains("readingLineEnabled = :readingLineEnabled,"))
        assertTrue(dao.contains("readingLineYFraction = :readingLineYFraction,"))
        assertTrue(dao.contains("suspend fun updateViewerState("))
    }

    @Test
    fun `counter ui state carries reading line fields from observed active document`() {
        val viewModel = ProjectSourceFiles.read(VIEW_MODEL)

        assertTrue(viewModel.contains("val readingLineEnabled: Boolean = false"))
        assertTrue(viewModel.contains("val readingLineYFraction: Float = DEFAULT_READING_LINE_Y_FRACTION"))
        assertTrue(viewModel.contains("readingLineEnabled = activeDocument?.readingLineEnabled ?: false"))
        assertTrue(viewModel.contains("activeDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION"))
    }

    private companion object {
        const val REPOSITORY = "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        const val DAO = "app/src/main/java/com/finnvek/knittools/data/local/ProjectDocumentDao.kt"
        const val VIEW_MODEL = "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
    }
}
