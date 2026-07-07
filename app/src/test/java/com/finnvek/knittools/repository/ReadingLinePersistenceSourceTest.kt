package com.finnvek.knittools.repository

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingLinePersistenceSourceTest {
    @Test
    fun `counter repository persists clamped reading line state through dao`() {
        val repository = ProjectSourceFiles.read(REPOSITORY)

        assertTrue(repository.contains("suspend fun updateReadingLine("))
        assertTrue(repository.contains("val sanitizedYFraction ="))
        assertTrue(repository.contains("sanitizeReadingLineYFraction(yFraction)"))
        assertTrue(repository.contains("dao.updateReadingLine("))
    }

    @Test
    fun `counter project dao has focused reading line update query`() {
        val dao = ProjectSourceFiles.read(DAO)

        assertTrue(dao.contains("SET readingLineEnabled = :enabled,"))
        assertTrue(dao.contains("readingLineYFraction = :yFraction,"))
        assertTrue(dao.contains("suspend fun updateReadingLine("))
    }

    @Test
    fun `counter ui state carries reading line fields from observed project`() {
        val viewModel = ProjectSourceFiles.read(VIEW_MODEL)
        val reducers = ProjectSourceFiles.read(REDUCERS)

        assertTrue(viewModel.contains("val readingLineEnabled: Boolean = false"))
        assertTrue(viewModel.contains("val readingLineYFraction: Float = DEFAULT_READING_LINE_Y_FRACTION"))
        assertTrue(reducers.contains("readingLineEnabled = project.readingLineEnabled"))
        assertTrue(reducers.contains("readingLineYFraction = project.readingLineYFraction"))
    }

    private companion object {
        const val REPOSITORY = "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        const val DAO = "app/src/main/java/com/finnvek/knittools/data/local/CounterProjectDao.kt"
        const val VIEW_MODEL = "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        const val REDUCERS = "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterUiStateReducers.kt"
    }
}
