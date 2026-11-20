package com.finnvek.knittools

import com.finnvek.knittools.data.datastore.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class BrazilianPortugueseLocaleContractTest {
    @Test
    fun `locale configuration matches selectable languages and regional resource directory`() {
        val nodes =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/xml/locales_config.xml").toFile())
                .getElementsByTagName("locale")
        val tags = (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("android:name") }
        assertEquals(AppLanguage.entries.mapNotNull { it.languageTag }.toSet(), tags.toSet())
        assertEquals(tags.size, tags.toSet().size)
        assertTrue("values-pt-rBR" in ProjectSourceFiles.configuredResourceDirectories())
        ProjectSourceFiles.configuredResourceDirectories().forEach { directory ->
            assertTrue(
                directory,
                ProjectSourceFiles
                    .read("app/src/main/res/$directory/strings.xml")
                    .contains("<string name=\"settings_language_brazilian_portuguese\">Português (Brasil)</string>"),
            )
        }
    }

    @Test
    fun `Brazilian resources preserve every format argument and define Portuguese plural categories`() {
        val base = resources("values")
        val brazilian = resources("values-pt-rBR")
        val deferred = ProjectSourceFiles.deferredOnboardingTranslationNames()
        base.filterValues { it.getAttribute("translatable") != "false" }.forEach { (name, original) ->
            if (name !in brazilian && name in deferred) return@forEach
            val localized = brazilian.getValue(name)
            val variants = if (localized.tagName == "plurals") items(localized) else listOf(localized)
            if (localized.tagName == "plurals") {
                assertEquals(name, setOf("one", "many", "other"), variants.map { it.getAttribute("quantity") }.toSet())
            }
            variants.forEach { variant ->
                val expectedText =
                    if (original.tagName == "plurals") {
                        val originalVariants = items(original)
                        val quantity =
                            variant.getAttribute("quantity").let {
                                if (it == "one" &&
                                    ProjectSourceFiles.reminderIntervalUsesQuantityInSingular(name, "values-pt-rBR")
                                ) {
                                    "other"
                                } else {
                                    it
                                }
                            }
                        (
                            originalVariants.firstOrNull { it.getAttribute("quantity") == quantity }
                                ?: originalVariants.first { it.getAttribute("quantity") == "other" }
                        ).textContent
                    } else {
                        original.textContent
                    }
                assertEquals(name, placeholders(expectedText), placeholders(variant.textContent))
            }
        }
    }

    private fun placeholders(text: String): List<String> =
        Regex("""%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]""")
            .findAll(text)
            .map { it.value }
            .sorted()
            .toList()

    private fun items(element: Element): List<Element> {
        val nodes = element.getElementsByTagName("item")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun resources(directory: String): Map<String, Element> =
        ProjectSourceFiles.textResources(directory).filterValues { it.tagName in setOf("string", "plurals") }
}
