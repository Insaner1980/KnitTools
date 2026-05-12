package com.finnvek.knittools.data.local

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class DaoQuerySourceTest {
    @Test
    fun `created sort uses createdAt instead of row id`() {
        val dao = ProjectSourceFiles.read(COUNTER_PROJECT_DAO)

        assertTrue(
            dao.contains(
                "SELECT * FROM counter_projects WHERE isCompleted = 0 " +
                    "ORDER BY createdAt DESC, id DESC",
            ),
        )
        assertTrue(
            dao.contains(
                "SELECT * FROM counter_projects WHERE isCompleted = 1 " +
                    "ORDER BY createdAt DESC, id DESC",
            ),
        )
    }

    @Test
    fun `active counter entry points ignore completed projects`() {
        val dao = ProjectSourceFiles.read(COUNTER_PROJECT_DAO)
        val repository = ProjectSourceFiles.read(COUNTER_REPOSITORY)
        val counterViewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        assertTrue(
            dao.contains(
                "SELECT * FROM counter_projects WHERE isCompleted = 0 " +
                    "ORDER BY updatedAt DESC, id DESC LIMIT 1",
            ),
        )
        assertTrue(repository.contains("suspend fun getLatestActiveProject(): CounterProject?"))
        assertTrue(counterViewModel.contains("repository.getActiveProjects().collect"))
        assertTrue(widget.contains("entryPoint.counterRepository().getLatestActiveProject()"))
    }

    @Test
    fun `saved pattern single-row lookups are deterministic`() {
        val dao = ProjectSourceFiles.read(SAVED_PATTERN_DAO)

        assertTrue(
            dao.contains(
                "SELECT * FROM saved_patterns WHERE ravelryId = :ravelryId " +
                    "ORDER BY savedAt DESC, id DESC LIMIT 1",
            ),
        )
        assertTrue(
            dao.contains(
                "SELECT * FROM saved_patterns WHERE patternUrl = :patternUrl " +
                    "ORDER BY savedAt DESC, id DESC LIMIT 1",
            ),
        )
    }

    private companion object {
        private const val COUNTER_PROJECT_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/CounterProjectDao.kt"
        private const val SAVED_PATTERN_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/SavedPatternDao.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val COUNTER_WIDGET =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt"
    }
}
