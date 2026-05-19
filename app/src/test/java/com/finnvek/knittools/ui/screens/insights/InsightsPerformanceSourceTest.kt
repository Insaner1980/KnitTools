package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsPerformanceSourceTest {
    @Test
    fun `insights calculations run on injected io dispatcher`() {
        val viewModel = ProjectSourceFiles.read(INSIGHTS_VIEW_MODEL)

        assertTrue(viewModel.contains("import com.finnvek.knittools.di.IoDispatcher"))
        assertTrue(viewModel.contains("import kotlinx.coroutines.flow.flowOn"))
        assertTrue(viewModel.contains("@param:IoDispatcher private val ioDispatcher: CoroutineDispatcher"))
        assertTrue(viewModel.contains(".flowOn(ioDispatcher)"))
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
