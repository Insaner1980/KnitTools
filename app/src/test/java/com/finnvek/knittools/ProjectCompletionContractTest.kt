package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCompletionContractTest {
    @Test
    fun `schema 25 adds only completion history and preserves all seventeen previous entities`() {
        val previous = ProjectSourceFiles.schemaEntities(24)
        val current = ProjectSourceFiles.schemaEntities(25)
        assertEquals(17, previous.size)
        assertEquals(setOf("project_completions"), current.keys - previous.keys)
        previous.forEach { (table, entity) -> assertEquals(table, entity, current[table]) }
        val completion = current.getValue("project_completions").toString()
        assertTrue(completion.contains("index_project_completions_projectId"))
        assertTrue(completion.contains("CASCADE"))
    }
}
