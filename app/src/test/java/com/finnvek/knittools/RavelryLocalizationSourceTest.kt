package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class RavelryLocalizationSourceTest {
    @Test
    fun `pdf explanations are complete translated resources in every configured locale`() {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val locales =
            builder
                .parse(ProjectSourceFiles.file("app/src/main/res/xml/locales_config.xml").toFile())
                .getElementsByTagName("locale")
        val expectedDirectories =
            (0 until locales.length)
                .map { index ->
                    val language = (locales.item(index) as Element).getAttribute("android:name")
                    if (language == "en") "values" else "values-$language"
                }.toSet()
        val files = ProjectSourceFiles.localizedStringFiles()
        assertEquals(expectedDirectories, files.map { it.parent.fileName.toString() }.toSet())
        val messagesByLocale =
            files.associate { file ->
                val strings = builder.parse(file.toFile()).getElementsByTagName("string")
                val messages =
                    (0 until strings.length)
                        .map { strings.item(it) as Element }
                        .filter { it.getAttribute("name") in PDF_EXPLANATION_STRINGS }
                assertEquals("$file explanation resources", PDF_EXPLANATION_STRINGS.size, messages.size)
                file.parent.fileName.toString() to
                    messages.associate { resource ->
                        val key = resource.getAttribute("name")
                        val text = resource.textContent
                        assertFalse("$file $key must be translatable", resource.getAttribute("translatable") == "false")
                        assertTrue("$file $key must explain PDF availability", text.contains("PDF"))
                        assertFalse("$file $key needs no format arguments", text.contains('%'))
                        key to text
                    }
            }
        val defaultMessages = messagesByLocale.getValue("values")
        messagesByLocale.filterKeys { it != "values" }.forEach { (locale, messages) ->
            PDF_EXPLANATION_STRINGS.forEach { key ->
                assertFalse("$locale $key must be translated", messages.getValue(key) == defaultMessages.getValue(key))
            }
        }
    }

    @Test
    fun `ravelry and saved pattern copy is localized without source category headings`() {
        val defaultStrings = ProjectSourceFiles.read(DEFAULT_STRINGS)

        RAVELRY_COPY_STRINGS.forEach { stringName ->
            assertTrue(defaultStrings.contains("""name="$stringName""""))
            assertFalse(
                "$stringName still bypasses translation checks",
                defaultStrings.contains(
                    Regex("""<string name="$stringName"[^>]*tools:ignore="[^"]*MissingTranslation"""),
                ),
            )
        }

        LOCALE_STRINGS.forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)

            RAVELRY_COPY_STRINGS.forEach { stringName ->
                assertTrue("$stringsFile missing $stringName", strings.contains("""name="$stringName""""))
            }
        }

        val allStrings =
            (listOf(DEFAULT_STRINGS) + LOCALE_STRINGS)
                .joinToString(separator = "\n") { ProjectSourceFiles.read(it) }
        assertFalse(allStrings.contains("Saved from Ravelry"))
        assertTrue(defaultStrings.contains("Ravelry and Google Play features use network services"))
        assertFalse(defaultStrings.contains("All data stays on your device"))
        assertFalse(defaultStrings.contains("no accounts"))
    }

    private companion object {
        private val PDF_EXPLANATION_STRINGS =
            setOf("ravelry_save_pattern_explanation", "saved_pattern_detail_no_pdf_explanation")
        private const val DEFAULT_STRINGS = "app/src/main/res/values/strings.xml"
        private val LOCALE_STRINGS =
            listOf(
                "app/src/main/res/values-da/strings.xml",
                "app/src/main/res/values-de/strings.xml",
                "app/src/main/res/values-es/strings.xml",
                "app/src/main/res/values-fi/strings.xml",
                "app/src/main/res/values-fr/strings.xml",
                "app/src/main/res/values-it/strings.xml",
                "app/src/main/res/values-nb/strings.xml",
                "app/src/main/res/values-nl/strings.xml",
                "app/src/main/res/values-pt/strings.xml",
                "app/src/main/res/values-sv/strings.xml",
            )
        private val RAVELRY_COPY_STRINGS =
            listOf(
                "ravelry_open_saved_pattern",
                "ravelry_save_pattern_explanation",
                "saved_pattern_detail_no_pdf_explanation",
                "ravelry_search_requires_sign_in",
                "ravelry_import_loading",
                "ravelry_import_title",
                "ravelry_import_already_saved",
                "ravelry_import_needs_sign_in",
                "ravelry_import_could_not_import",
                "ravelry_import_backend_unavailable",
                "saved_pattern_detail_pdf_attached",
                "saved_pattern_detail_available_offline",
                "saved_pattern_detail_open_on_ravelry",
                "saved_pattern_detail_requires_ravelry",
                "saved_pattern_detail_open_pattern",
                "saved_pattern_detail_attach_to_project",
                "saved_pattern_detail_remove_confirm",
                "ravelry_browse",
                "ravelry_disconnect",
                "ravelry_disconnect_confirm",
                "ravelry_not_connected",
                "ravelry_connecting",
                "ravelry_auth_pending",
                "ravelry_connected",
                "ravelry_connected_as",
                "ravelry_auth_cancelled",
                "ravelry_auth_expired",
                "ravelry_backend_unavailable",
                "ravelry_disconnecting",
                "privacy_summary",
            )
    }
}
