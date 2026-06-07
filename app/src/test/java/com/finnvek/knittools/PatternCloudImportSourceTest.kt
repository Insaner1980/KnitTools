package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternCloudImportSourceTest {
    @Test
    fun `drive and dropbox import stays on the existing open document path`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER_SHEET)

        assertTrue(picker.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(picker.contains("takePersistableUriPermission(uri, flags)"))
        assertTrue(picker.contains("private const val PATTERN_PDF_MIME_TYPE = \"application/pdf\""))
        assertTrue(picker.contains("private fun pdfMimeTypes(): Array<String> = arrayOf(PATTERN_PDF_MIME_TYPE)"))
        assertTrue(picker.contains("val openPdfDocumentPicker = { openDocumentLauncher.launch(pdfMimeTypes()) }"))
        assertTrue(picker.contains("openDeviceFiles = openPdfDocumentPicker"))
        assertTrue(picker.contains("openCloudProviderFiles = openPdfDocumentPicker"))
        assertTrue(picker.contains("R.string.pattern_picker_import_cloud_pdf"))
        assertTrue(picker.contains("onDocumentSelected(uri.toString(), resolvePatternName(context, uri))"))

        listOf("OAuth", "oauth", "DropboxClient", "DriveScopes", "GoogleSignIn", "accessToken").forEach { token ->
            assertFalse("Pattern picker must not add provider-specific auth: $token", picker.contains(token))
        }
    }

    @Test
    fun `drive and dropbox import copy is localized`() {
        val englishStrings = ProjectSourceFiles.read(STRINGS)

        assertTrue(englishStrings.contains("Drive or Dropbox"))
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val text = ProjectSourceFiles.read(file)
            val value = stringValue(text, "pattern_picker_import_cloud_pdf")
            assertNotNull("$file missing pattern_picker_import_cloud_pdf", value)
            assertFalse("$file has blank pattern_picker_import_cloud_pdf", value.isNullOrBlank())
        }
    }

    @Test
    fun `drive and dropbox import does not add provider dependencies`() {
        val buildText =
            listOf(ROOT_BUILD, SETTINGS, APP_BUILD, VERSION_CATALOG)
                .joinToString(separator = "\n") { ProjectSourceFiles.read(it).lowercase() }
        val forbidden =
            listOf(
                "com.dropbox",
                "dropbox-core-sdk",
                "google-api-services-drive",
                "google-auth-library",
                "com.google.api.services.drive",
            )
        val offenders = forbidden.filter(buildText::contains)

        assertTrue("Provider-specific dependencies were added: $offenders", offenders.isEmpty())
    }

    private fun stringValue(
        text: String,
        key: String,
    ): String? {
        val pattern = Regex("""<string\s+name="$key"[^>]*>(.*?)</string>""")
        return pattern.find(text)?.groupValues?.get(1)
    }

    private companion object {
        const val PATTERN_PICKER_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
        const val STRINGS = "app/src/main/res/values/strings.xml"
        const val ROOT_BUILD = "build.gradle.kts"
        const val SETTINGS = "settings.gradle.kts"
        const val APP_BUILD = "app/build.gradle.kts"
        const val VERSION_CATALOG = "gradle/libs.versions.toml"
    }
}
