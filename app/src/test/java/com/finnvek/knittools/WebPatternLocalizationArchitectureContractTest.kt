package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

class WebPatternLocalizationArchitectureContractTest {
    @Test
    fun `all eleven locales define complete web pattern copy with matching placeholders`() {
        assertEquals(11, localeDirectories.size)
        val defaultStrings = localizedStrings("values")
        assertEquals("Saved patterns from websites and PDFs", defaultStrings["desc_saved_patterns"])
        assertEquals(
            "No saved patterns yet.\\nAdd a web pattern or import a PDF.",
            defaultStrings["empty_saved_patterns"],
        )

        localeDirectories.forEach { directory ->
            val strings = localizedStrings(directory)
            localizedContractStringNames.forEach { name ->
                val defaultValue = defaultStrings[name]
                val localizedValue = strings[name]
                assertNotNull("values is missing $name", defaultValue)
                assertNotNull("$directory is missing $name", localizedValue)
                requireNotNull(defaultValue)
                requireNotNull(localizedValue)
                assertTrue("$directory has blank $name", localizedValue.isNotBlank())
                assertEquals(
                    "$directory has different placeholders for $name",
                    placeholders(defaultValue),
                    placeholders(localizedValue),
                )
            }
            sourceNeutralSavedPatternStringNames.forEach { name ->
                assertFalse(
                    "$directory $name must be source-neutral",
                    strings.getValue(name).contains("Ravelry", ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun `web pattern copy avoids decorative separator characters`() {
        localeDirectories.forEach { directory ->
            val strings = localizedStrings(directory)
            localizedContractStringNames.forEach { name ->
                val value = strings[name]
                assertNotNull("$directory is missing $name", value)
                requireNotNull(value)
                forbiddenSeparators.forEach { separator ->
                    assertFalse("$directory $name contains forbidden separator $separator", separator in value)
                }
            }
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `web pattern V1 leaves persistence platform entitlement and backend boundaries unchanged`() {
        val database = ProjectSourceFiles.read(DATABASE)
        val entity = ProjectSourceFiles.read(SAVED_PATTERN_ENTITY)
        val schema = ProjectSourceFiles.read(SCHEMA_24)
        val manifest = ProjectSourceFiles.read(MANIFEST)
        val fileProviderPaths = ProjectSourceFiles.read(FILE_PROVIDER_PATHS)
        val proState = ProjectSourceFiles.read(PRO_STATE)
        val counterRepository = ProjectSourceFiles.read(COUNTER_REPOSITORY)
        val savedPatternCreateSql = roomCreateSql(schema, "saved_patterns")
        val projectDocumentCreateSql = roomCreateSql(schema, "project_documents")

        assertTrue(database.contains("version = 24"))
        assertFalse(database.contains("MIGRATION_24_25"))
        assertFalse(Files.exists(ProjectSourceFiles.file(SCHEMA_25)))
        assertTrue(schema.contains("\"version\": 24"))
        assertTrue(schema.contains("\"identityHash\": \"f9a2845195abd670d0bf330708bdcd75\""))
        assertEquals("TEXT", roomTextColumnDefinition(savedPatternCreateSql, "localPdfUri"))
        assertEquals("TEXT NOT NULL", roomTextColumnDefinition(projectDocumentCreateSql, "localPdfUri"))
        assertEquals(savedPatternEntityFields, constructorFields(entity, "data class SavedPatternEntity"))

        assertEquals(
            setOf(
                "android.permission.INTERNET",
                "android.permission.VIBRATE",
                "android.permission.CAMERA",
            ),
            manifestAttributeValues(manifest, "uses-permission", "name"),
        )
        assertEquals(
            setOf(".MainActivity", ".widget.CounterWidgetReceiver"),
            exportedComponentNames(manifest, exported = "true"),
        )
        assertEquals(
            setOf(".widget.CounterWidgetActions", "androidx.core.content.FileProvider"),
            exportedComponentNames(manifest, exported = "false"),
        )
        assertEquals(1, Regex("android.intent.action.SEND").findAll(manifest).count())
        assertEquals(1, Regex("android:mimeType=\"text/plain\"").findAll(manifest).count())
        assertEquals(
            setOf(
                "progress_photos" to "progress_photos/",
                "pattern_captures" to "pattern_captures/",
            ),
            xmlAttributePairs(fileProviderPaths, "files-path", "name", "path"),
        )

        val metadataOnlyAttachment =
            balancedContentAfter(
                counterRepository,
                "private suspend fun attachMetadataOnlySavedPattern",
                '{',
                '}',
            )
        assertTrue(metadataOnlyAttachment.contains("dao.updatePatternInformation("))
        assertFalse(metadataOnlyAttachment.contains("projectDocumentRepository"))
        val proFeatures = balancedContentAfter(proState, "enum class ProFeature", '{', '}')
        assertFalse(proFeatures.contains("WEB_"))

        val dependencyAndProductionSources =
            listOf(
                ProjectSourceFiles.read("app/build.gradle.kts"),
                ProjectSourceFiles.read("gradle/libs.versions.toml"),
            ) + sourceFilesUnder("app/src/main/java")
        val dependencyAndProductionText = dependencyAndProductionSources.joinToString("\n")
        forbiddenImplementationTokens.forEach { token ->
            assertFalse("Web pattern V1 must not introduce $token", dependencyAndProductionText.contains(token))
        }

        val backendText = sourceFilesUnder("functions/src").joinToString("\n")
        assertFalse(backendText.contains("WEB_LINK"))
        assertFalse(backendText.contains("WebPattern"))
        assertFalse(backendText.contains("webPattern"))
    }

    private fun localizedStrings(directory: String): Map<String, String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/$directory/strings.xml").toFile())
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val element = nodes.item(index) as Element
            element.getAttribute("name") to element.textContent
        }
    }

    private fun placeholders(value: String): List<String> = placeholderRegex.findAll(value).map { it.value }.toList()

    private fun constructorFields(
        source: String,
        declaration: String,
    ): List<String> =
        Regex("""\bval\s+([A-Za-z][A-Za-z0-9_]*)\s*:""")
            .findAll(balancedContentAfter(source, declaration, '(', ')'))
            .map { it.groupValues[1] }
            .toList()

    private fun roomCreateSql(
        schema: String,
        tableName: String,
    ): String =
        requireNotNull(
            Regex(
                """"tableName"\s*:\s*"${Regex.escape(tableName)}"\s*,\s*"createSql"\s*:\s*"([^"]+)"""",
            ).find(schema),
        ) { "Room schema is missing $tableName" }.groupValues[1]

    private fun roomTextColumnDefinition(
        createSql: String,
        columnName: String,
    ): String =
        requireNotNull(
            Regex("""`${Regex.escape(columnName)}`\s+(TEXT(?:\s+NOT NULL)?)""").find(createSql),
        ) { "Room table is missing $columnName" }.groupValues[1]

    private fun balancedContentAfter(
        source: String,
        marker: String,
        opening: Char,
        closing: Char,
    ): String {
        val markerIndex = source.indexOf(marker)
        require(markerIndex >= 0) { "Source is missing $marker" }
        val start = source.indexOf(opening, markerIndex)
        require(start >= 0) { "Source is missing $opening after $marker" }
        var depth = 0
        for (index in start until source.length) {
            when (source[index]) {
                opening -> depth += 1
                closing -> {
                    depth -= 1
                    if (depth == 0) return source.substring(start + 1, index)
                }
            }
        }
        error("Source has an unbalanced $opening after $marker")
    }

    private fun manifestAttributeValues(
        manifest: String,
        elementName: String,
        attributeName: String,
    ): Set<String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(manifest.byteInputStream())
        val nodes = document.getElementsByTagName(elementName)
        return (0 until nodes.length).mapTo(mutableSetOf()) { index ->
            (nodes.item(index) as Element).getAttributeNS(ANDROID_NAMESPACE, attributeName)
        }
    }

    private fun exportedComponentNames(
        manifest: String,
        exported: String,
    ): Set<String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(manifest.byteInputStream())
        return componentElements
            .flatMap { elementName ->
                val nodes = document.getElementsByTagName(elementName)
                (0 until nodes.length).mapNotNull { index ->
                    val element = nodes.item(index) as Element
                    element
                        .takeIf { it.getAttributeNS(ANDROID_NAMESPACE, "exported") == exported }
                        ?.getAttributeNS(ANDROID_NAMESPACE, "name")
                }
            }.toSet()
    }

    private fun xmlAttributePairs(
        xml: String,
        elementName: String,
        firstAttribute: String,
        secondAttribute: String,
    ): Set<Pair<String, String>> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(xml.byteInputStream())
        val nodes = document.getElementsByTagName(elementName)
        return (0 until nodes.length).mapTo(mutableSetOf()) { index ->
            val element = nodes.item(index) as Element
            element.getAttribute(firstAttribute) to element.getAttribute(secondAttribute)
        }
    }

    private fun sourceFilesUnder(relativePath: String): List<String> {
        val root = ProjectSourceFiles.file(relativePath)
        return Files.walk(root).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".kt") || path.toString().endsWith(".ts") }
                .map { path -> ProjectSourceFiles.read(path) }
                .toList()
        }
    }

    private companion object {
        private const val DATABASE =
            "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        private const val SAVED_PATTERN_ENTITY =
            "app/src/main/java/com/finnvek/knittools/data/local/SavedPatternEntity.kt"
        private const val SCHEMA_24 =
            "app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/24.json"
        private const val SCHEMA_25 =
            "app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/25.json"
        private const val MANIFEST = "app/src/main/AndroidManifest.xml"
        private const val FILE_PROVIDER_PATHS = "app/src/main/res/xml/file_paths.xml"
        private const val PRO_STATE = "app/src/main/java/com/finnvek/knittools/pro/ProState.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

        private val localeDirectories =
            listOf(
                "values",
                "values-fi",
                "values-sv",
                "values-de",
                "values-fr",
                "values-es",
                "values-pt",
                "values-it",
                "values-nb",
                "values-da",
                "values-nl",
            )

        private val webPatternStringNames =
            setOf(
                "web_pattern_add",
                "web_pattern_label",
                "web_pattern_title_label",
                "web_pattern_designer_label",
                "web_pattern_url_label",
                "web_pattern_website_label",
                "web_pattern_edit",
                "web_pattern_delete",
                "web_pattern_delete_confirm_title",
                "web_pattern_delete_confirm_message",
                "web_pattern_attach",
                "web_pattern_unlink",
                "web_pattern_replace_confirm_title",
                "web_pattern_replace_confirm_message",
                "web_pattern_open_website",
                "web_pattern_open_website_description",
                "web_pattern_edit_description",
                "web_pattern_delete_description",
                "web_pattern_attach_description",
                "web_pattern_unlink_description",
                "web_pattern_shared_link",
                "web_pattern_confirm_details",
                "web_pattern_error_title_required",
                "web_pattern_error_title_too_long",
                "web_pattern_error_url_required",
                "web_pattern_error_url_invalid",
                "web_pattern_error_url_web_only",
                "web_pattern_http_warning",
                "web_pattern_already_saved",
                "web_pattern_save_failed",
                "web_pattern_update_failed",
                "web_pattern_delete_failed",
                "web_pattern_open_failed",
                "web_pattern_no_browser",
                "web_pattern_source_controlled",
                "web_pattern_not_offline",
                "web_pattern_opens_original",
                "web_pattern_keep_current_draft",
                "web_pattern_use_shared_link",
                "web_pattern_discard_current_draft",
                "web_pattern_share_ambiguous",
                "web_pattern_project_unavailable",
            )
        private val sourceNeutralSavedPatternStringNames = setOf("desc_saved_patterns", "empty_saved_patterns")
        private val localizedContractStringNames = webPatternStringNames + sourceNeutralSavedPatternStringNames

        private val savedPatternEntityFields =
            listOf(
                "id",
                "source",
                "ravelryPatternId",
                "name",
                "designerName",
                "thumbnailUrl",
                "difficulty",
                "gaugeStitches",
                "gaugeRows",
                "needleSize",
                "yarnWeight",
                "yardage",
                "availability",
                "originalUrl",
                "canonicalUrl",
                "localPdfUri",
                "isAvailableOffline",
                "savedAt",
                "updatedAt",
                "lastSyncedAt",
            )

        private val placeholderRegex = Regex("""%(?:\d+\$)?[a-zA-Z]""")
        private val forbiddenSeparators = setOf("\u00B7", "\u2013", "\u2014")
        private val componentElements = listOf("activity", "activity-alias", "service", "receiver", "provider")
        private val forbiddenImplementationTokens =
            setOf(
                "android.webkit.WebView",
                "androidx.webkit",
                "org.jsoup",
                "com.google.mlkit",
                "firebase-ai",
                "generativeai",
            )
    }
}
