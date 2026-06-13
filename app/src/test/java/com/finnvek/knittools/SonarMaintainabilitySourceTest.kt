package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonarMaintainabilitySourceTest {
    @Test
    fun `main activity uses concise intent branching and KTX Uri creation`() {
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)

        assertTrue(mainActivity.contains("import androidx.core.net.toUri"))
        assertTrue(mainActivity.contains(".launchUrl(this, RAVELRY_PATTERN_SEARCH_URL.toUri())"))
        assertTrue(
            mainActivity.contains(
                "val isShareImport = !isOAuthCallback && handleRavelryShareIntentIfNeeded(intent)",
            ),
        )
        assertFalse(mainActivity.contains("Uri.parse(RAVELRY_PATTERN_SEARCH_URL)"))
        assertFalse(mainActivity.contains("if (isOAuthCallback) false else handleRavelryShareIntentIfNeeded(intent)"))
    }

    @Test
    fun `ravelry backend request builds only present optional values`() {
        val backendClient = ProjectSourceFiles.read(RAVELRY_BACKEND_CLIENT)

        assertTrue(backendClient.contains("private fun PatternSearchParams.toBackendData(): Map<String, Any>"))
        assertTrue(backendClient.contains("putOptional(\"craft\", craft)"))
        assertTrue(backendClient.contains("putOptional(\"difficultyFrom\", difficultyFrom)"))
        assertFalse(backendClient.contains(".filterValues { it != null }"))
    }

    @Test
    fun `firebase binding module is a functional interface`() {
        val firebaseModule = ProjectSourceFiles.read(FIREBASE_MODULE)

        assertTrue(firebaseModule.contains("fun interface FirebaseBindingsModule"))
        assertFalse(firebaseModule.contains("abstract class FirebaseBindingsModule"))
    }

    @Test
    fun `top level composables group action parameters`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val patternCard = ProjectSourceFiles.read(PATTERN_CARD)
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val stepperButton = ProjectSourceFiles.read(COUNTER_STEPPER_BUTTON)

        assertTrue(navGraph.contains("data class KnitToolsNavActions("))
        assertTrue(navGraph.contains("actions: KnitToolsNavActions = KnitToolsNavActions()"))
        assertTrue(counterScreen.contains("data class CounterScreenActions("))
        assertTrue(counterScreen.contains("actions: CounterScreenActions = CounterScreenActions()"))
        assertTrue(patternCard.contains("data class PatternCardState("))
        assertTrue(patternCard.contains("state: PatternCardState,"))
        assertTrue(searchScreen.contains("data class RavelrySearchActions("))
        assertTrue(searchScreen.contains("actions: RavelrySearchActions,"))
        assertTrue(stepperButton.contains("data class CounterStepButtonFaceAppearance("))
        assertTrue(stepperButton.contains("appearance: CounterStepButtonFaceAppearance"))
    }

    @Test
    fun `ravelry search tab is split into focused rendering helpers`() {
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)

        listOf(
            "private fun RavelrySearchField(",
            "private fun LazyListScope.ravelrySearchResults(",
            "private fun LazyListScope.ravelrySearchLoadingItem(",
            "private fun LazyListScope.ravelrySearchErrorItem(",
            "private fun LazyListScope.ravelrySearchEmptyStateItem(",
        ).forEach { helper ->
            assertTrue("RavelrySearchScreen should contain $helper", searchScreen.contains(helper))
        }
    }

    @Test
    fun `sonar coverage gate excludes debug framework diagnostics`() {
        val sonarProperties = ProjectSourceFiles.read(SONAR_PROPERTIES)

        assertTrue(sonarProperties.contains("**/SentryInit.kt"))
    }

    private companion object {
        private const val SONAR_PROPERTIES = "sonar-project.properties"
        private const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        private const val RAVELRY_BACKEND_CLIENT =
            "app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendClient.kt"
        private const val FIREBASE_MODULE = "app/src/main/java/com/finnvek/knittools/di/FirebaseModule.kt"
        private const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val COUNTER_SCREEN = "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val PATTERN_CARD = "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/PatternCard.kt"
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val COUNTER_STEPPER_BUTTON =
            "app/src/main/java/com/finnvek/knittools/ui/components/CounterStepperButton.kt"
    }
}
