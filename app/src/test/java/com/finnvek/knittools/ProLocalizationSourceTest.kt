package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

class ProLocalizationSourceTest {
    private fun expectedPluralQuantities(directory: String): Set<String> =
        if (directory in setOf("values-fr", "values-es", "values-it", "values-pt", "values-pt-rBR")) {
            setOf("one", "many", "other")
        } else {
            setOf("one", "other")
        }

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
                when (language) {
                    "en" -> "values"
                    "pt-BR" -> "values-pt-rBR"
                    else -> "values-$language"
                }
            }
        assertEquals(allResourceDirectories.toSet(), directories.toSet())
        directories.forEach(::assertActiveProjectCopy)
        val prompt = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/components/ProPromptSheet.kt")
        assertTrue(prompt.contains("ProPromptSource.ProjectReactivation -> R.string.onboarding_pro_reactivation_title"))
        assertTrue(prompt.contains("R.string.onboarding_pro_reactivation_body"))
        assertTrue(prompt.contains("R.string.onboarding_pro_retention_projects"))
        assertFalse(
            ProjectSourceFiles.read("app/src/main/res/values/strings.xml").contains("Completed projects also count."),
        )
    }

    @Test
    fun `user visible locale resources reject the middle dot separator`() {
        Files.walk(ProjectSourceFiles.file("app/src/main/res")).use { files ->
            files
                .filter { file ->
                    val name = file.fileName.toString()
                    file.parent.fileName
                        .toString()
                        .startsWith("values") &&
                        (
                            name.startsWith(
                                "strings",
                            ) &&
                                name.endsWith(".xml") ||
                                name == "pattern_reading_assistance.xml"
                        )
                }.forEach { file ->
                    val source = ProjectSourceFiles.read(file)
                    assertFalse("Literal middle dot found in $file", source.contains('\u00b7'))
                    assertFalse("Escaped middle dot found in $file", escapedMiddleDot.containsMatchIn(source))

                    val document =
                        DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(file.toFile())
                    assertFalse(
                        "Rendered middle dot found in $file",
                        document.documentElement.textContent.contains('\u00b7'),
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
                val original = base.getValue(key)
                val translated = localized.getValue(key)
                if (original.type == "plurals") {
                    val expectedQuantities = expectedPluralQuantities(directory)
                    assertEquals(
                        "Quantities differ for $key in $directory",
                        expectedQuantities,
                        translated.quantities.toSet(),
                    )
                    assertEquals(
                        "Duplicate quantity for $key in $directory",
                        translated.quantities.size,
                        translated.quantities.toSet().size,
                    )
                }
                translated.placeholders.forEach { (variant, placeholders) ->
                    assertEquals(
                        "Placeholders differ for $key/$variant in $directory",
                        original.placeholders[variant] ?: original.placeholders["other"],
                        placeholders,
                    )
                }
            }
        }
    }

    private fun assertActiveProjectCopy(directory: String) {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/$directory/strings_onboarding.xml").toFile())
        val resources = document.documentElement.childNodes
        val elements =
            (0 until resources.length)
                .mapNotNull { resources.item(it) as? Element }
                .associateBy { it.getAttribute("name") }
        val expectedQuantities = expectedPluralQuantities(directory)
        val items = elements.getValue("onboarding_pro_active_projects").getElementsByTagName("item")
        val quantities = List(items.length) { (items.item(it) as Element).getAttribute("quantity") }.toSet()
        assertEquals(directory, expectedQuantities, quantities)
        repeat(items.length) { index ->
            assertEquals(
                "$directory/onboarding_pro_active_projects",
                listOf("%1\$d"),
                placeholderRegex.findAll(items.item(index).textContent).map { it.value }.toList(),
            )
        }
        if (directory == "values") {
            for (key in listOf("onboarding_pro_projects_trial_body", "onboarding_pro_projects_body")) {
                assertTrue(elements.getValue(key).textContent.contains("one project in progress at a time"))
            }
            assertTrue(
                elements
                    .getValue("onboarding_pro_retention_projects")
                    .textContent
                    .contains("Completed projects don\\'t count toward the free limit"),
            )
        }
        assertFalse(
            directory,
            elements.getValue("onboarding_pro_reactivation_title").textContent ==
                elements.getValue("onboarding_pro_projects_title").textContent,
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
                        List(items.length) { itemIndex ->
                            val item = items.item(itemIndex) as Element
                            item.getAttribute("quantity") to item.textContent
                        }
                    } else {
                        listOf("string" to element.textContent)
                    }
                resources[element.getAttribute("name")] =
                    ResourceShape(
                        type = type,
                        quantities = if (type == "plurals") variants.map { it.first } else emptyList(),
                        placeholders =
                            variants
                                .associate { (quantity, text) ->
                                    quantity to
                                        placeholderRegex
                                            .findAll(text)
                                            .map { it.value }
                                            .sorted()
                                            .toList()
                                },
                    )
            }
        }
        return resources
    }

    private data class ResourceShape(
        val type: String,
        val placeholders: Map<String, List<String>>,
        val quantities: List<String>,
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
                "pro_badge",
                "pro_badge_locked_description",
                "pro_badge_trial_description",
                "knittools_pro",
                "pro_content_stays_available",
                "pro_status_trial_days",
                "pro_status_trial_ended",
                "pro_status_purchased",
                "pro_trial_ended_body",
                "pro_continue_free",
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
                "values-pt-rBR",
                "values-sv",
            )
        val allResourceDirectories = listOf("values") + supportedResourceDirectories
    }
}
