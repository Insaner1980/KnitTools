package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

class RemotePatternImageFoundationSourceTest {
    @Test
    fun `Coil Ktor 3 network support is declared once and available to instrumented tests`() {
        val catalog = ProjectSourceFiles.read(VERSION_CATALOG)
        val buildScript = ProjectSourceFiles.read(APP_BUILD_SCRIPT)
        val networkArtifacts =
            Regex("name = \"coil-network-(?:ktor2|ktor3|okhttp)\"")
                .findAll(catalog)
                .toList()

        assertEquals(1, networkArtifacts.size)
        assertTrue(
            catalog.contains(
                "coil-network-ktor3 = { group = \"io.coil-kt.coil3\", " +
                    "name = \"coil-network-ktor3\", version.ref = \"coil\" }",
            ),
        )
        assertTrue(buildScript.contains("implementation(libs.coil.network.ktor3)"))
        assertTrue(buildScript.contains("androidTestImplementation(libs.ktor.client.mock)"))
    }

    @Test
    fun `production relies on Coil service loading instead of a custom ImageLoader registry`() {
        val component = ProjectSourceFiles.read(REMOTE_PATTERN_IMAGE)
        val mainSourceRoot = ProjectSourceFiles.file(MAIN_SOURCE_ROOT)
        val customRegistrySource =
            Files.walk(mainSourceRoot).use { paths ->
                paths
                    .filter { path -> path.fileName.toString().endsWith(".kt") }
                    .map { path -> ProjectSourceFiles.read(path) }
                    .anyMatch { source ->
                        source.contains("ImageLoader.Builder(") ||
                            source.contains("KtorNetworkFetcherFactory(")
                    }
            }

        assertFalse(customRegistrySource)
        assertTrue(component.contains("SingletonImageLoader.get(context)"))
    }

    @Test
    fun `remote image is decorative quiet and detached from app owned storage`() {
        val component = ProjectSourceFiles.read(REMOTE_PATTERN_IMAGE)

        assertTrue(component.contains("contentDescription = null"))
        assertTrue(component.contains("MaterialTheme.colorScheme.surfaceVariant"))
        assertTrue(component.contains("onError = { failed = true }"))
        assertTrue(component.contains("remember(normalizedUrl)"))
        assertFalse(component.contains("CircularProgressIndicator"))
        assertFalse(component.contains("com.finnvek.knittools.data.storage"))
        assertFalse(component.contains("progress_photos"))
        assertFalse(component.contains("yarn_photos"))
        assertFalse(component.contains("pattern_captures"))
        assertFalse(component.contains("pattern_pdfs"))
        assertFalse(component.contains("java.io."))
    }

    @Test
    fun `all current Ravelry image surfaces use the shared remote image path`() {
        val patternCard = ProjectSourceFiles.read(PATTERN_CARD)
        val ravelryDetail = ProjectSourceFiles.read(RAVELRY_DETAIL)
        val savedPatternDetail = ProjectSourceFiles.read(SAVED_PATTERN_DETAIL)
        val search = ProjectSourceFiles.read(RAVELRY_SEARCH)
        val importConfirmation = ProjectSourceFiles.read(RAVELRY_IMPORT_CONFIRMATION)
        val savedPatterns = ProjectSourceFiles.read(SAVED_PATTERNS)

        listOf(patternCard, ravelryDetail, savedPatternDetail).forEach { source ->
            assertTrue(source.contains("RemotePatternImage("))
            assertFalse(source.contains("AsyncImage("))
        }
        listOf(search, importConfirmation, savedPatterns).forEach { source ->
            assertTrue(source.contains("PatternCard("))
        }
    }

    @Test
    fun `cleartext stays disabled and FileProvider roots stay exact`() {
        val manifest = parseXml(ANDROID_MANIFEST)
        val application = manifest.getElementsByTagName("application").item(0) as Element

        assertEquals(
            "false",
            application.getAttributeNS(ANDROID_NAMESPACE, "usesCleartextTraffic"),
        )
        assertFalse(application.hasAttributeNS(ANDROID_NAMESPACE, "networkSecurityConfig"))

        val filePaths = parseXml(FILE_PROVIDER_PATHS)
        val roots =
            filePaths.documentElement.childNodes
                .let { nodes ->
                    buildList {
                        repeat(nodes.length) { index ->
                            val element = nodes.item(index) as? Element
                            if (element != null) {
                                add(
                                    ProviderRoot(
                                        tag = element.tagName,
                                        name = element.getAttribute("name"),
                                        path = element.getAttribute("path"),
                                    ),
                                )
                            }
                        }
                    }
                }

        assertEquals(
            listOf(
                ProviderRoot("files-path", "progress_photos", "progress_photos/"),
                ProviderRoot("files-path", "pattern_captures", "pattern_captures/"),
            ),
            roots,
        )
    }

    private fun parseXml(path: String) =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(ProjectSourceFiles.file(path).toFile())

    private data class ProviderRoot(
        val tag: String,
        val name: String,
        val path: String,
    )

    private companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        private const val VERSION_CATALOG = "gradle/libs.versions.toml"
        private const val APP_BUILD_SCRIPT = "app/build.gradle.kts"
        private const val MAIN_SOURCE_ROOT = "app/src/main/java"
        private const val REMOTE_PATTERN_IMAGE =
            "app/src/main/java/com/finnvek/knittools/ui/components/RemotePatternImage.kt"
        private const val PATTERN_CARD =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/PatternCard.kt"
        private const val RAVELRY_DETAIL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt"
        private const val SAVED_PATTERN_DETAIL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternDetailScreen.kt"
        private const val RAVELRY_SEARCH =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val RAVELRY_IMPORT_CONFIRMATION =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryImportConfirmationSheet.kt"
        private const val SAVED_PATTERNS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt"
        private const val ANDROID_MANIFEST = "app/src/main/AndroidManifest.xml"
        private const val FILE_PROVIDER_PATHS = "app/src/main/res/xml/file_paths.xml"
    }
}
