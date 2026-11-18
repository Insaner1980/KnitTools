package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonarMaintainabilitySourceTest {
    @Test
    fun `top level composables group action parameters`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val patternCard = ProjectSourceFiles.read(PATTERN_CARD)
        val stepperButton = ProjectSourceFiles.read(COUNTER_STEPPER_BUTTON)

        assertTrue(navGraph.contains("data class KnitToolsNavActions("))
        assertTrue(navGraph.contains("actions: KnitToolsNavActions = KnitToolsNavActions()"))
        assertTrue(navGraph.contains("data class KnitToolsNavRequests("))
        assertTrue(navGraph.contains("requests: KnitToolsNavRequests = KnitToolsNavRequests()"))
        assertTrue(counterScreen.contains("data class CounterScreenActions("))
        assertTrue(counterScreen.contains("actions: CounterScreenActions,"))
        assertTrue(patternCard.contains("data class PatternCardState("))
        assertTrue(patternCard.contains("state: PatternCardState,"))
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
        private const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val COUNTER_SCREEN = "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val PATTERN_CARD = "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/PatternCard.kt"
        private const val COUNTER_STEPPER_BUTTON =
            "app/src/main/java/com/finnvek/knittools/ui/components/CounterStepperButton.kt"
    }
}
