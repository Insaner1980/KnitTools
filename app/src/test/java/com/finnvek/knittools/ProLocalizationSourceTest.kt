package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class ProLocalizationSourceTest {
    @Test
    fun `project limit clarification follows the first sentence once in every plural item`() {
        val clarifications =
            mapOf(
                "values" to "Completed projects also count.",
                "values-fi" to "Myös valmistuneet projektit lasketaan mukaan.",
                "values-sv" to "Färdiga projekt räknas också med.",
                "values-de" to "Abgeschlossene Projekte zählen ebenfalls mit.",
                "values-fr" to "Les projets terminés comptent aussi.",
                "values-es" to "Los proyectos terminados también cuentan.",
                "values-pt" to "Os projetos concluídos também contam.",
                "values-it" to "Anche i progetti completati vengono conteggiati.",
                "values-nb" to "Fullførte prosjekter teller også med.",
                "values-da" to "Afsluttede projekter tæller også med.",
                "values-nl" to "Voltooide projecten tellen ook mee.",
            )
        assertEquals(allResourceDirectories.toSet(), clarifications.keys)
        clarifications.forEach { (directory, clarification) ->
            val document =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(ProjectSourceFiles.file("app/src/main/res/$directory/strings.xml").toFile())
            listOf("pro_prompt_projects_trial_body", "pro_prompt_projects_body").forEach { key ->
                val plurals = document.getElementsByTagName("plurals")
                val resource =
                    (0 until plurals.length)
                        .map { plurals.item(it) as Element }
                        .single { it.getAttribute("name") == key }
                val items = resource.getElementsByTagName("item")
                repeat(items.length) { index ->
                    val item = items.item(index) as Element
                    val text = item.textContent
                    val context = "$directory/$key/${item.getAttribute("quantity")}"
                    assertTrue(context, text.substringAfter(". ").startsWith("$clarification "))
                    assertEquals(context, 1, Regex(Regex.escape(clarification)).findAll(text).count())
                }
            }
        }
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
