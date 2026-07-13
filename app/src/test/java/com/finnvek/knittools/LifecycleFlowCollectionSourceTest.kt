package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleFlowCollectionSourceTest {
    @Test
    fun `UI-tapahtumavirrat kerataan vain kaynnissa olevan elinkaaren aikana`() {
        val collector = ProjectSourceFiles.read(LIFECYCLE_COLLECTOR)

        assertTrue(collector.contains("lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)"))
        assertTrue(collector.contains("flow.collect"))

        FLOW_CONSUMERS.forEach { path ->
            val source = ProjectSourceFiles.read(path)

            assertTrue("Lifecycle-kerain puuttuu tiedostosta $path", source.contains("CollectWithLifecycleEffect("))
            assertFalse("Suora Flow-kerays jai tiedostoon $path", source.contains(".collect {"))
        }
    }

    private companion object {
        private const val LIFECYCLE_COLLECTOR =
            "app/src/main/java/com/finnvek/knittools/ui/components/CollectWithLifecycleEffect.kt"
        private val FLOW_CONSUMERS =
            listOf(
                "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt",
            )
    }
}
