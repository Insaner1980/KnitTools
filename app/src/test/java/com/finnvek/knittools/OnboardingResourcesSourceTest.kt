package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class OnboardingResourcesSourceTest {
    @Test
    fun `every onboarding resource is used by main source`() {
        val source = StringBuilder()
        Files.walk(ProjectSourceFiles.file("app/src/main/java")).use { files ->
            files.filter { it.toString().endsWith(".kt") }.forEach { source.append(ProjectSourceFiles.read(it)) }
        }
        resources(baseFile).forEach { (key, resource) ->
            val reference = Regex("\\bR\\.${resource.tagName}\\.${Regex.escape(key)}\\b")
            assertTrue("Unused onboarding resource $key", reference.containsMatchIn(source))
        }
    }

    @Test
    fun `onboarding copy contains no forbidden separators`() {
        onboardingFiles().forEach { file ->
            resources(file).forEach { (key, resource) ->
                val text = resource.textContent
                assertFalse("Forbidden separator in $file/$key", forbiddenSeparator.containsMatchIn(text))
            }
        }
    }

    @Test
    fun `every configured locale has all onboarding keys types quantities and placeholders`() {
        val english = resources(baseFile)
        val files = configuredOnboardingFiles()
        assertEquals("Onboarding locale files differ from configured locales", files.toSet(), onboardingFiles().toSet())
        assertFalse(ProjectSourceFiles.read(baseFile).contains("MissingTranslation"))
        files.forEach { file ->
            assertTrue("Missing onboarding translations: $file", Files.exists(file))
            val translated = resources(file)
            assertEquals("Onboarding keys differ in $file", english.keys, translated.keys)
            translated.forEach { (key, resource) ->
                val original = english.getValue(key)
                assertEquals("Resource type changed in $file/$key", original.tagName, resource.tagName)
                assertEquals("Placeholders changed in $file/$key", placeholders(original), placeholders(resource))
                if (resource.tagName == "plurals") {
                    val items = resource.getElementsByTagName("item")
                    val quantities = List(items.length) { (items.item(it) as Element).getAttribute("quantity") }
                    val expected =
                        if (file.parent.fileName.toString() in manyQuantityDirectories) {
                            setOf("one", "many", "other")
                        } else {
                            setOf("one", "other")
                        }
                    assertEquals("Plural quantities differ in $file/$key", expected, quantities.toSet())
                    assertEquals("Duplicate plural quantity in $file/$key", quantities.size, quantities.toSet().size)
                }
            }
        }
    }

    @Test
    fun `every onboarding key has a translator context comment`() {
        onboardingFiles().forEach { file ->
            resources(file).forEach { (key, resource) ->
                var previous = resource.previousSibling
                while (previous?.nodeType == Node.TEXT_NODE && previous.textContent.isBlank()) {
                    previous = previous.previousSibling
                }
                assertTrue("Missing translator context in $file/$key", previous?.nodeType == Node.COMMENT_NODE)
                assertTrue("Empty translator context in $file/$key", !previous?.textContent.isNullOrBlank())
            }
        }
    }

    @Test
    fun `Finnish onboarding uses the established pattern terminology`() {
        val finnish = resources(ProjectSourceFiles.file("app/src/main/res/values-fi/strings_onboarding.xml"))
        finnish.forEach { (key, resource) ->
            assertFalse(
                "Incorrect Finnish pattern terminology in $key",
                Regex("(?i)\\bkaava\\p{L}*").containsMatchIn(resource.textContent),
            )
        }
        assertTrue(finnish.getValue("guide_topic_pattern_subtitle").textContent.contains("verkko-ohje"))
    }

    @Test
    fun `English onboarding screen titles use title case`() {
        resources(baseFile)
            .filterKeys {
                it.endsWith("_screen_title") || (it.startsWith("guide_topic_") && it.endsWith("_title"))
            }.forEach { (key, resource) ->
                val words = titleWord.findAll(resource.textContent.replace("\\'", "'")).map { it.value }.toList()
                assertTrue("Empty screen title $key", words.isNotEmpty())
                words.forEachIndexed { index, word ->
                    val mayBeLowercase = index > 0 && index < words.lastIndex && word in smallTitleWords
                    assertTrue(
                        "Screen title $key is not Title Case: $word",
                        mayBeLowercase || word.first().isUpperCase(),
                    )
                }
            }
    }

    private fun onboardingFiles(): List<Path> =
        Files.list(ProjectSourceFiles.file("app/src/main/res")).use { directories ->
            directories
                .filter { it.fileName.toString().startsWith("values") }
                .map { it.resolve("strings_onboarding.xml") }
                .filter { Files.exists(it) }
                .toList()
        }

    private fun configuredOnboardingFiles(): List<Path> {
        val locales =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/xml/locales_config.xml").toFile())
                .getElementsByTagName("locale")
        return List(locales.length) { index ->
            val language = (locales.item(index) as Element).getAttribute("android:name")
            val directory =
                when (language) {
                    "en" -> "values"
                    "pt-BR" -> "values-pt-rBR"
                    else -> "values-$language"
                }
            ProjectSourceFiles.file("app/src/main/res/$directory/strings_onboarding.xml")
        }
    }

    private fun resources(file: Path): Map<String, Element> {
        val children =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file.toFile())
                .documentElement.childNodes
        val elements = (0 until children.length).mapNotNull { children.item(it) as? Element }
        val resources = elements.associateBy { it.getAttribute("name") }
        assertEquals("Duplicate onboarding key in $file", elements.size, resources.size)
        return resources
    }

    private fun placeholders(resource: Element): Set<List<String>> {
        val variants =
            if (resource.tagName == "plurals") {
                val items = resource.getElementsByTagName("item")
                List(items.length) { items.item(it).textContent }
            } else {
                listOf(resource.textContent)
            }
        return variants
            .map {
                placeholder
                    .findAll(it)
                    .map { match -> match.value }
                    .sorted()
                    .toList()
            }.toSet()
    }

    private companion object {
        val baseFile = ProjectSourceFiles.file("app/src/main/res/values/strings_onboarding.xml")
        val manyQuantityDirectories = setOf("values-fr", "values-es", "values-it", "values-pt", "values-pt-rBR")
        val placeholder = Regex("%(?:\\d+\\$)?[a-zA-Z]")
        val forbiddenSeparator = Regex("[\\u00b7\\u2022]| \\| |\\\\u(?:00[bB]7|2022)")
        val titleWord = Regex("[A-Za-z][A-Za-z']*")
        val smallTitleWords =
            setOf(
                "a",
                "an",
                "and",
                "as",
                "at",
                "by",
                "for",
                "from",
                "in",
                "into",
                "of",
                "on",
                "or",
                "the",
                "to",
                "with",
            )
    }
}
