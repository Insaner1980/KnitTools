package com.finnvek.knittools.data.local

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
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
        val widgetResolver = ProjectSourceFiles.read(COUNTER_WIDGET_RESOLVER)

        assertTrue(
            dao.contains(
                "SELECT * FROM counter_projects WHERE isCompleted = 0 " +
                    "ORDER BY updatedAt DESC, id DESC LIMIT 1",
            ),
        )
        assertTrue(repository.contains("suspend fun getLatestActiveProject(): CounterProject?"))
        assertTrue(counterViewModel.contains("repository.getActiveProjects().collect"))
        assertTrue(widget.contains("entryPoint.counterRepository()"))
        assertTrue(widgetResolver.contains("getLatestActiveProject()?.toWidgetData()"))
        assertTrue(widgetResolver.contains("return project.takeUnless { it.isCompleted }"))
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

    @Test
    fun `project photo counts use grouped query`() {
        val dao = ProjectSourceFiles.read(PROGRESS_PHOTO_DAO)

        assertTrue(
            dao.contains(
                "SELECT projectId, COUNT(*) AS count FROM progress_photos",
            ),
        )
        assertTrue(dao.contains("WHERE projectId IN (:projectIds) GROUP BY projectId"))
        assertTrue(dao.contains("suspend fun getPhotoCountsByProjectIds(projectIds: List<Long>)"))
    }

    @Test
    fun `project total minutes use exact session seconds`() {
        val dao = ProjectSourceFiles.read(SESSION_DAO)

        assertTrue(dao.contains("SUM(durationSeconds)"))
        assertFalse(dao.contains("SUM(durationMinutes)"))
    }

    @Test
    fun `insights session queries use sargable range filters and matching indexes`() {
        val dao = ProjectSourceFiles.read(SESSION_DAO)
        val entity = ProjectSourceFiles.read(SESSION_ENTITY)
        val database = ProjectSourceFiles.read(KNIT_TOOLS_DATABASE)

        assertFalse(dao.contains("fun getSessionsForInsights("))
        assertFalse(dao.contains("(:start IS NULL OR endedAt >= :start)"))
        assertTrue(dao.contains("WHERE endedAt >= :start"))
        assertTrue(dao.contains("WHERE projectId = :projectId"))
        assertTrue(entity.contains("Index(value = [\"endedAt\", \"startedAt\"])"))
        assertTrue(entity.contains("Index(value = [\"projectId\", \"endedAt\", \"startedAt\"])"))
        assertTrue(database.contains("MIGRATION_10_11"))
        assertTrue(database.contains("index_sessions_endedAt_startedAt"))
        assertTrue(database.contains("index_sessions_projectId_endedAt_startedAt"))
    }

    private companion object {
        private const val COUNTER_PROJECT_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/CounterProjectDao.kt"
        private const val SAVED_PATTERN_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/SavedPatternDao.kt"
        private const val PROGRESS_PHOTO_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/ProgressPhotoDao.kt"
        private const val SESSION_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/SessionDao.kt"
        private const val SESSION_ENTITY =
            "app/src/main/java/com/finnvek/knittools/data/local/SessionEntity.kt"
        private const val KNIT_TOOLS_DATABASE =
            "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val COUNTER_WIDGET =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt"
        private const val COUNTER_WIDGET_RESOLVER =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetDataResolver.kt"
    }
}
