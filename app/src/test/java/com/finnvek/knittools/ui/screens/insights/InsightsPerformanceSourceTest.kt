package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsPerformanceSourceTest {
    @Test
    fun `insights does not materialize or group the complete session history`() {
        val viewModel = ProjectSourceFiles.read(INSIGHTS_VIEW_MODEL)
        val accumulator =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSessionAccumulator.kt",
            )
        val repository =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt",
            )
        val dao = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/data/local/SessionDao.kt")
        assertFalse(viewModel.contains(".groupBy"))
        assertFalse(accumulator.contains(".groupBy"))
        assertFalse(viewModel.contains("getSessionsForInsights(null, null)"))
        assertTrue(viewModel.contains(".flatMapLatest { params ->"))
        assertTrue(repository.contains(".observeSessionChanges()"))
        assertTrue(repository.contains(".mapLatest {"))
        assertEquals(5, "LIMIT 256".toRegex().findAll(dao).count())
    }

    @Test
    fun `insights calculations run on injected io dispatcher`() {
        val viewModel = ProjectSourceFiles.read(INSIGHTS_VIEW_MODEL)

        assertTrue(viewModel.contains("import com.finnvek.knittools.di.IoDispatcher"))
        assertTrue(viewModel.contains("import kotlinx.coroutines.flow.flowOn"))
        assertTrue(viewModel.contains("import kotlinx.coroutines.flow.mapLatest"))
        assertTrue(viewModel.contains("@param:IoDispatcher private val ioDispatcher: CoroutineDispatcher"))
        assertTrue(viewModel.contains(".flowOn(ioDispatcher)"))
        assertTrue(viewModel.contains("}.mapLatest { input ->"))
        assertTrue(viewModel.contains("buildInsightsProjectFabric("))
        assertTrue(viewModel.indexOf("val projectFabric") > viewModel.indexOf("val timePerProject"))
    }

    @Test
    fun `insights screen collects one ui state`() {
        val screen = ProjectSourceFiles.read(INSIGHTS_SCREEN)

        assertTrue(screen.contains("val uiState by viewModel.uiState.collectAsStateWithLifecycle()"))
        assertFalse(screen.contains("viewModel.totalMinutes.collectAsStateWithLifecycle()"))
        assertFalse(screen.contains("viewModel.avgPace.collectAsStateWithLifecycle()"))
        assertFalse(screen.contains("viewModel.timePerProject.collectAsStateWithLifecycle()"))
        assertFalse(screen.contains("viewModel.dailyActivity.collectAsStateWithLifecycle()"))
    }

    private companion object {
        private const val INSIGHTS_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsViewModel.kt"
        private const val INSIGHTS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt"
    }
}
