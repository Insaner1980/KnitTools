package com.finnvek.knittools

import com.finnvek.knittools.domain.model.CounterHistoryAction
import com.finnvek.knittools.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterHistoryContractTest {
    @Test fun knownPersistedActionsAndUnknownValuesHaveSafeSemanticLabels() {
        assertEquals(CounterHistoryAction.INCREASE, CounterHistoryAction.fromPersistedValue("increment"))
        assertEquals(CounterHistoryAction.DECREASE, CounterHistoryAction.fromPersistedValue("decrement"))
        assertEquals(CounterHistoryAction.RESET, CounterHistoryAction.fromPersistedValue("reset"))
        listOf("", "undo", "future", "INCREMENT").forEach {
            assertEquals(CounterHistoryAction.CHANGED, CounterHistoryAction.fromPersistedValue(it))
        }
    }

    @Test fun counterAndSessionHistoryAreDistinctProjectScopedRoutes() {
        assertEquals("counter_history/42", Screen.CounterHistory(42).route)
        assertEquals("counter_history/{projectId}", Screen.CounterHistory.ROUTE)
        assertNotEquals(Screen.SessionHistory(42).route, Screen.CounterHistory(42).route)
    }

    @Test fun everyCurrentLocaleContainsTheWholeHistoryVocabulary() {
        val names =
            listOf(
                "title",
                "empty_title",
                "empty_body",
                "increase",
                "decrease",
                "reset",
                "changed",
                "event_description",
                "transition",
            )
        val files = ProjectSourceFiles.localizedStringFiles()
        assertEquals(11, files.size)
        files.forEach { file ->
            val source = ProjectSourceFiles.read(file)
            assertFalse("$file: obsolete retention copy", source.contains("name=\"counter_history_retention\""))
            names.forEach { name -> assertTrue("$file: $name", source.contains("name=\"counter_history_$name\"")) }
        }
    }
}
