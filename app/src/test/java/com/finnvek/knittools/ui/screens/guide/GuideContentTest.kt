package com.finnvek.knittools.ui.screens.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuideContentTest {
    @Test
    fun topicKeysRoundTripAndUnknownKeysFallBackToRoot() {
        assertEquals(listOf("continue", "pattern", "yarn", "details", "tools"), GuideTopic.entries.map { it.key })
        GuideTopic.entries.forEach { assertEquals(it, GuideTopic.fromKey(it.key)) }
        listOf(null, "", "unknown", "CONTINUE", " continue").forEach { assertNull(GuideTopic.fromKey(it)) }
    }

    @Test
    fun allTwentySixItemsBelongToTheFiveOrderedTopics() {
        assertEquals(listOf(6, 7, 3, 4, 6), GuideTopic.entries.map { it.items.size })
        val items = GuideTopic.entries.flatMap { it.items }
        assertEquals(26, items.size)
        assertEquals(items.size, items.map { it.title }.distinct().size)
        assertEquals(items.size, items.map { it.body }.distinct().size)
    }

    @Test
    fun bundledActionsCoverEverySupportedDestination() {
        val actions = GuideTopic.entries.flatMap { topic -> topic.items.mapNotNull { it.action } }
        assertEquals(GuideAction.entries.toSet(), actions.toSet())
    }
}
