package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonarMaintainabilitySourceTest {
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
        assertTrue(navGraph.contains("data class KnitToolsNavRequests("))
        assertTrue(navGraph.contains("requests: KnitToolsNavRequests = KnitToolsNavRequests()"))
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
    fun `sonar coverage gate excludes debug framework diagnostics`() {
        val sonarProperties = ProjectSourceFiles.read(SONAR_PROPERTIES)

        assertTrue(sonarProperties.contains("**/SentryInit.kt"))
    }

    @Test
    fun `jacoco report excludes app shell synthetic classes and debug diagnostics`() {
        val appBuild = ProjectSourceFiles.read(APP_BUILD)

        listOf(
            "\"**/App.*\"",
            "\"**/App$*.*\"",
            "\"**/MainActivity.*\"",
            "\"**/MainActivity$*.*\"",
            "\"**/MainActivityKt*.*\"",
            "\"**/SentryInit.*\"",
            "\"**/SentryInit$*.*\"",
        ).forEach { exclusion ->
            assertTrue("JaCoCo exclusion missing: $exclusion", appBuild.contains(exclusion))
        }
        assertFalse(appBuild.contains("\"**/App*.*\""))
        assertFalse(appBuild.contains("\"**/MainActivity*.*\""))
    }

    private companion object {
        private const val APP_BUILD = "app/build.gradle.kts"
        private const val SONAR_PROPERTIES = "sonar-project.properties"
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
