package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class YarnCardFlowSourceTest {
    @Test
    fun `my yarn cards use deterministic newest first ordering`() {
        val dao = ProjectSourceFiles.read(YARN_CARD_DAO)

        assertTrue(dao.contains("ORDER BY createdAt DESC, id DESC"))
    }

    @Test
    fun `my yarn delete failures are surfaced to the screen`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(navGraph.contains("val yarnDeleteErrorId by libraryViewModel.yarnDeleteErrorId"))
        assertTrue(navGraph.contains("deleteErrorId = yarnDeleteErrorId"))
        assertTrue(screen.contains("LaunchedEffect(state.deleteErrorId)"))
        assertTrue(screen.contains("SnackbarHost(hostState = snackbarHostState)"))
    }

    private companion object {
        private const val YARN_CARD_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/YarnCardDao.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val MY_YARN_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/MyYarnScreen.kt"
    }
}
