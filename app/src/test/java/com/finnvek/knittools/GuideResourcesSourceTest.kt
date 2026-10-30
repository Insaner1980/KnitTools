package com.finnvek.knittools

import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.ui.screens.guide.GuideTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class GuideResourcesSourceTest {
    private val resources by lazy {
        val children =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/values/strings_onboarding.xml").toFile())
                .documentElement.childNodes
        (0 until children.length)
            .mapNotNull { children.item(it) as? Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private val resourceNames by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    @Test
    fun `guide resources match the topic and item references`() {
        val referenced =
            GuideTopic.entries
                .flatMap { topic ->
                    listOf(topic.title, topic.subtitle) + topic.items.flatMap { listOf(it.title, it.body) }
                }.map(::resourceName)
                .toSet() + baseGuideResources
        val guideResources = resources.filterKeys { it.startsWith("guide_") }

        assertEquals(referenced, guideResources.keys)
        guideResources.forEach { (name, text) -> assertTrue("Empty guide resource $name", text.isNotBlank()) }
    }

    @Test
    fun `catalogue Pro markers name the matching feature`() {
        val actual =
            GuideTopic.entries
                .flatMap { it.items }
                .mapNotNull { item ->
                    item.proFeature?.let { resourceName(item.body) to it }
                }.toMap()

        assertEquals(
            mapOf(
                "guide_continue_widget_body" to ProFeature.WIDGET,
                "guide_pattern_camera_body" to ProFeature.PATTERN_CAMERA_SCAN,
                "guide_yarn_my_yarn_body" to ProFeature.UNLIMITED_YARN,
                "guide_details_notes_body" to ProFeature.NOTES,
                "guide_details_counters_body" to ProFeature.MULTIPLE_COUNTERS,
                "guide_details_reminders_body" to ProFeature.ROW_REMINDERS,
                "guide_details_photos_body" to ProFeature.PROGRESS_PHOTOS,
                "guide_tools_insights_body" to ProFeature.INSIGHTS_CHARTS,
            ),
            actual,
        )
    }

    @Test
    fun `guide copy mentioning Pro has feature metadata without catalogue annotations`() {
        resources.filterKeys { it.startsWith("guide_") }.forEach { (name, text) ->
            assertFalse("Catalogue annotation leaked into $name", text.contains("(Pro marker)"))
        }
        GuideTopic.entries.flatMap { it.items }.forEach { item ->
            val title = resourceName(item.title)
            val body = resourceName(item.body)
            val text = resources.getValue(title) + " " + resources.getValue(body)
            if (proWord.containsMatchIn(text)) {
                assertNotNull("Pro copy needs feature metadata: $body", item.proFeature)
            }
        }
    }

    private fun resourceName(id: Int): String = resourceNames.getValue(id)

    private companion object {
        val proWord = Regex("\\bPro\\b")
        val baseGuideResources =
            setOf(
                "guide_screen_title",
                "guide_intro",
                "guide_entry_settings",
                "guide_entry_overview",
                "guide_try_it",
            )
    }
}
