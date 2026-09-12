package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class ProLocalizationSourceTest {
    @Test
    fun `active project policy resources cover configured locales quantities and distinct actions`() {
        val configuration =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/xml/locales_config.xml").toFile())
        val locales = configuration.getElementsByTagName("locale")
        val directories =
            (0 until locales.length).map {
                val language = (locales.item(it) as Element).getAttribute("android:name")
                if (language == "en") "values" else "values-$language"
            }
        assertEquals(allResourceDirectories.toSet(), directories.toSet())
        val activeWords =
            mapOf(
                "values" to "active",
                "values-fi" to "aktiivi",
                "values-sv" to "aktiv",
                "values-de" to "aktiv",
                "values-fr" to "actif",
                "values-es" to "activo",
                "values-pt" to "ativo",
                "values-it" to "attiv",
                "values-nb" to "aktiv",
                "values-da" to "aktiv",
                "values-nl" to "actie",
            )
        directories.forEach { directory -> assertActiveProjectCopy(directory, activeWords.getValue(directory)) }
        val prompt = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/components/ProPromptSheet.kt")
        assertTrue(prompt.contains("ProPromptSource.ProjectReactivation -> R.string.pro_prompt_reactivation_title"))
        assertTrue(prompt.contains("R.string.pro_prompt_reactivation_trial_body"))
        assertTrue(prompt.contains("R.string.pro_prompt_reactivation_body"))
        assertFalse(
            ProjectSourceFiles.read("app/src/main/res/values/strings.xml").contains("Completed projects also count."),
        )
    }

    @Test
    fun `user visible locale resources reject the middle dot separator`() {
        allResourceDirectories.forEach { directory ->
            val source =
                ProjectSourceFiles
                    .file("app/src/main/res/$directory/strings.xml")
                    .toFile()
                    .readText()
            assertFalse("Literal middle dot found in $directory", source.contains('\u00b7'))
            assertFalse("Escaped middle dot found in $directory", escapedMiddleDot.containsMatchIn(source))

            val document =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(ProjectSourceFiles.file("app/src/main/res/$directory/strings.xml").toFile())
            val strings = document.getElementsByTagName("string")
            repeat(strings.length) { index ->
                val element = strings.item(index) as Element
                assertFalse(
                    "Rendered middle dot found in ${element.getAttribute("name")} in $directory",
                    element.textContent.contains('\u00b7'),
                )
            }
        }
    }

    @Test
    fun `pro resources keep keys types and placeholders in every locale`() {
        val base = readResources("values")

        supportedResourceDirectories.forEach { directory ->
            val localized = readResources(directory)
            requiredResourceNames.forEach { key ->
                assertTrue("Missing $key in $directory", localized.containsKey(key))
                assertEquals(
                    "Resource type differs for $key in $directory",
                    base.getValue(key).type,
                    localized.getValue(key).type,
                )
                assertEquals(
                    "Placeholders differ for $key in $directory",
                    base.getValue(key).placeholders,
                    localized.getValue(key).placeholders,
                )
            }
        }
    }

    private fun assertActiveProjectCopy(
        directory: String,
        activeWord: String,
    ) {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/$directory/strings.xml").toFile())
        val resources = document.documentElement.childNodes
        val elements =
            (0 until resources.length)
                .mapNotNull { resources.item(it) as? Element }
                .associateBy { it.getAttribute("name") }
        val expectedQuantities =
            if (directory in setOf("values-fr", "values-es", "values-it", "values-pt")) {
                setOf("one", "many", "other")
            } else {
                setOf("one", "other")
            }
        for (key in listOf("pro_prompt_projects_trial_body", "pro_prompt_projects_body")) {
            val items = elements.getValue(key).getElementsByTagName("item")
            val quantities =
                (0 until items.length)
                    .map {
                        (
                            items.item(
                                it,
                            ) as Element
                        ).getAttribute("quantity")
                    }.toSet()
            assertEquals(directory, expectedQuantities, quantities)
            repeat(items.length) { index ->
                val text = items.item(index).textContent
                assertEquals(
                    "$directory/$key",
                    listOf("%1\$d"),
                    placeholderRegex.findAll(text).map { it.value }.toList(),
                )
                assertTrue("$directory/$key", text.contains(activeWord))
            }
        }
        assertTrue(
            directory,
            elements.getValue("pro_group_projects_body").textContent.contains(activeWord),
        )
        assertFalse(
            directory,
            elements.getValue("pro_prompt_reactivation_title").textContent ==
                elements.getValue("pro_prompt_projects_title").textContent,
        )
    }

    private fun readResources(directory: String): Map<String, ResourceShape> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/$directory/strings.xml").toFile())
        val resources = mutableMapOf<String, ResourceShape>()
        listOf("string", "plurals").forEach { type ->
            val nodes = document.getElementsByTagName(type)
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as Element
                val variants =
                    if (type == "plurals") {
                        val items = element.getElementsByTagName("item")
                        List(items.length) { itemIndex -> items.item(itemIndex).textContent }
                    } else {
                        listOf(element.textContent)
                    }
                resources[element.getAttribute("name")] =
                    ResourceShape(
                        type = type,
                        placeholders =
                            variants
                                .map { text ->
                                    placeholderRegex
                                        .findAll(text)
                                        .map { it.value }
                                        .sorted()
                                        .toList()
                                }.toSet(),
                    )
            }
        }
        return resources
    }

    private data class ResourceShape(
        val type: String,
        val placeholders: Set<List<String>>,
    )

    private companion object {
        val placeholderRegex = Regex("%\\d+\\$[ds]")
        val escapedMiddleDot = Regex("""(?i)\\u00b7|&#0*183;|&#x0*b7;""")
        val requiredResourceNames =
            setOf(
                "pro_start_free_trial",
                "pro_start_14_day_trial",
                "pro_trial_start_failed",
                "pro_prompt_not_now",
                "pro_prompt_see_pro",
                "pro_prompt_trial_body",
                "pro_prompt_projects_title",
                "pro_prompt_reactivation_title",
                "pro_prompt_reactivation_trial_body",
                "pro_prompt_reactivation_body",
                "pro_prompt_projects_trial_body",
                "pro_prompt_projects_body",
                "pro_prompt_photos_title",
                "pro_prompt_photos_body",
                "pro_prompt_notes_title",
                "pro_prompt_notes_body",
                "pro_prompt_yarn_title",
                "pro_prompt_yarn_body",
                "pro_prompt_save_yarn_title",
                "pro_prompt_save_yarn_body",
                "pro_prompt_counters_title",
                "pro_prompt_counters_body",
                "pro_prompt_reminders_title",
                "pro_prompt_reminders_body",
                "pro_prompt_pattern_camera_title",
                "pro_prompt_pattern_camera_body",
                "pro_prompt_widget_title",
                "pro_prompt_widget_body",
                "pro_badge",
                "pro_badge_locked_description",
                "pro_badge_trial_description",
                "knittools_pro",
                "pro_content_stays_available",
                "pro_status_not_started",
                "pro_status_trial_days",
                "pro_status_trial_ended",
                "pro_status_purchased",
                "pro_trial_ended_title",
                "pro_trial_ended_body",
                "pro_continue_free",
                "pro_page_intro",
                "pro_page_trust",
                "pro_group_projects_title",
                "pro_group_projects_body",
                "pro_group_workflow_title",
                "pro_group_workflow_body",
                "pro_group_insights_title",
                "pro_group_insights_body",
                "pro_page_purchased",
                "pro_buy_for_price",
                "pro_price_unavailable",
                "billing_purchase_pending",
            )
        val supportedResourceDirectories =
            listOf(
                "values-da",
                "values-de",
                "values-es",
                "values-fi",
                "values-fr",
                "values-it",
                "values-nb",
                "values-nl",
                "values-pt",
                "values-sv",
            )
        val allResourceDirectories = listOf("values") + supportedResourceDirectories
    }
}
