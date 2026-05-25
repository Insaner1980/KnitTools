package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class AiRemovalUiSourceTest {
    @Test
    fun `removed AI summary copy is not exposed in UI resources`() {
        val stringFiles = localizedStringFiles()
        val offenders =
            stringFiles.filter { file ->
                val text = read(file)
                text.contains("project_actions_ai_summary") ||
                    text.contains("AI summary") ||
                    text.contains("Tekoälyn yhteenveto")
            }

        assertTrue("AI summary strings remain in $offenders", offenders.isEmpty())
    }

    @Test
    fun `camera permission copy is not tied to removed yarn label scan`() {
        val removedYarnScanPhrases =
            listOf(
                "scan yarn labels",
                "lankavyötteen skannaukseen",
                "Garnetiketten zu scannen",
                "garenlabels te scannen",
                "garnetiketter",
                "étiquettes de fil",
                "etichette del filato",
                "etiquetas de hilo",
                "etiquetas de fio",
            )
        val offenders =
            localizedStringFiles().filter { file ->
                val text = read(file)
                removedYarnScanPhrases.any { phrase -> text.contains(phrase) }
            }

        assertTrue("Camera permission strings still mention yarn label scan in $offenders", offenders.isEmpty())
    }

    @Test
    fun `pattern camera UI copy uses photo and PDF language instead of scan language`() {
        val offenders =
            localizedStringFiles().filter { file ->
                val text = read(file)
                patternCameraValues(text).any { value ->
                    value.contains("scan", ignoreCase = true) ||
                        value.contains("skann", ignoreCase = true) ||
                        value.contains("scann", ignoreCase = true) ||
                        value.contains("escaneo", ignoreCase = true) ||
                        value.contains("scansione", ignoreCase = true) ||
                        value.contains("leitura", ignoreCase = true)
                }
            }

        assertTrue("Pattern camera copy still uses scan language in $offenders", offenders.isEmpty())
    }

    @Test
    fun `my yarn copy describes notes quantities and linked projects`() {
        val baseStrings = ProjectSourceFiles.read(STRINGS)
        val finnishStrings = ProjectSourceFiles.read(FINNISH_STRINGS)

        assertTrue(baseStrings.contains("Yarn notes, quantities, and linked projects"))
        assertTrue(finnishStrings.contains("Lankamuistiinpanot, määrät ja linkitetyt projektit"))

        localizedStringFiles().forEach { file ->
            val text = read(file)
            val myYarnValues = myYarnValues(text)
            listOf("brand", "brands", "weight", "weights", "color", "colors").forEach { token ->
                assertFalse("$file still promises automatic yarn metadata: $token", myYarnValues.contains(token))
            }
        }
    }

    @Test
    fun `yarn card detail UI no longer registers removed scan review routes`() {
        val screen = ProjectSourceFiles.read(SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val detailScreen = yarnCardDetailSource()
        val viewModel = ProjectSourceFiles.read(YARN_CARD_VIEW_MODEL)

        assertFalse(screen.contains("YarnCardReview"))
        assertFalse(screen.contains("LibraryYarnCardReview"))
        assertFalse(navGraph.contains("Screen.YarnCardReview"))
        assertFalse(navGraph.contains("Screen.LibraryYarnCardReview"))
        assertFalse(navGraph.contains("libraryYarnCardReviewRoute"))
        assertFalse(detailScreen.contains("YarnCardScanContent"))
        assertFalse(detailScreen.contains("onDiscardScan"))
        assertFalse(detailScreen.contains("skannatun yarn cardin"))
        assertFalse(viewModel.contains("canSaveYarnCards"))
        assertFalse(viewModel.contains("fun saveCard("))
        assertFalse(viewModel.contains("fun saveCardDomain("))
        assertFalse(viewModel.contains("fun getCalculatorValues("))
        assertFalse(viewModel.contains("pendingCalcValues"))
        assertFalse(viewModel.contains("setPendingCalcValues"))
        assertTrue(detailScreen.contains("fun YarnCardDetailScreen("))
    }

    @Test
    fun `voice and microphone surfaces are not exposed in live app sources`() {
        val strings = localizedStringFiles().joinToString(separator = "\n") { read(it) }
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val manifest = ProjectSourceFiles.read(MANIFEST)
        val proState = ProjectSourceFiles.read(PRO_STATE)
        val proUpgradeScreen = ProjectSourceFiles.read(PRO_UPGRADE_SCREEN)
        val mainSources = liveAppSources().joinToString(separator = "\n") { read(it) }

        listOf(
            "voice_",
            "Voice commands",
            "Äänikomennot",
        ).forEach { token ->
            assertFalse("Voice UI copy remains: $token", strings.contains(token))
        }

        listOf(
            "VoiceCommand",
            "VoiceResponseManager",
            "VoiceCommandHandler",
            "canUseVoiceCommands",
            "canStartClassicVoice",
            "emitLocalVoiceFeedback",
            "Manifest.permission.RECORD_AUDIO",
            "hasAudioPermission",
            "Icons.Filled.Mic",
        ).forEach { token ->
            assertFalse("Counter screen still exposes voice surface: $token", counterScreen.contains(token))
        }

        assertFalse(manifest.contains("android.permission.RECORD_AUDIO"))
        assertFalse(proState.contains("VOICE_COMMANDS"))
        assertFalse(proUpgradeScreen.contains("pro_feature_voice_commands"))

        listOf(
            "SpeechRecognizer",
            "TextToSpeech",
            "VoiceCommand",
            "voice_",
            "Icons.Filled.Mic",
            "VOICE_COMMANDS",
        ).forEach { token ->
            assertFalse("Live app source still contains removed voice token: $token", mainSources.contains(token))
        }
    }

    @Test
    fun `removed yarn scan and AI prompt helpers are absent from live app sources`() {
        val mainSources = liveAppSources().joinToString(separator = "\n") { read(it) }

        listOf(
            "YarnLabelPhotoStorage",
            "promptLanguageName",
            "AI-prompteja",
        ).forEach { token ->
            assertFalse("Removed scan or AI helper remains in live app source: $token", mainSources.contains(token))
        }
    }

    @Test
    fun `ui comments no longer mention removed AI summary surface`() {
        val color = ProjectSourceFiles.read(COLOR)

        assertFalse(color.contains("AI summary"))
    }

    private fun localizedStringFiles(): List<Path> {
        val root = ProjectSourceFiles.file("app/src/main/res")
        val result = mutableListOf<Path>()
        Files.walk(root).use { paths ->
            paths
                .filter { path ->
                    path.fileName.toString() == "strings.xml" &&
                        path.parent.fileName
                            .toString()
                            .startsWith("values")
                }.forEach(result::add)
        }
        return result
    }

    private fun liveAppSources(): List<Path> {
        val root = ProjectSourceFiles.file("app/src/main")
        val result = mutableListOf<Path>()
        Files.walk(root).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) }
                .filter { path ->
                    val name = path.fileName.toString()
                    name.endsWith(".kt") || name.endsWith(".xml")
                }.forEach(result::add)
        }
        return result
    }

    private fun read(path: Path): String =
        String(Files.readAllBytes(path), StandardCharsets.UTF_8)
            .replace("\r\n", "\n")

    private fun patternCameraValues(text: String): List<String> =
        listOf(
            "pattern_picker_camera_scan",
            "pattern_scan_failed",
            "pro_feature_pattern_camera_scan",
        ).mapNotNull { key -> stringValue(text, key) }

    private fun myYarnValues(text: String): String =
        listOf(
            "desc_my_yarn",
            "empty_my_yarn",
            "no_saved_yarns",
        ).mapNotNull { key -> stringValue(text, key) }
            .joinToString(separator = "\n")
            .lowercase()

    private fun stringValue(
        text: String,
        key: String,
    ): String? {
        val pattern = Regex("""<string\s+name="$key"[^>]*>(.*?)</string>""")
        return pattern.find(text)?.groupValues?.get(1)
    }

    private fun yarnCardDetailSource(): String {
        val renamed = ProjectSourceFiles.file(YARN_CARD_DETAIL_SCREEN)
        return if (Files.exists(renamed)) {
            read(renamed)
        } else {
            ProjectSourceFiles.read(YARN_CARD_REVIEW_SCREEN)
        }
    }

    private companion object {
        const val SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt"
        const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        const val YARN_CARD_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt"
        const val YARN_CARD_REVIEW_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardReviewScreen.kt"
        const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        const val MANIFEST =
            "app/src/main/AndroidManifest.xml"
        const val PRO_STATE =
            "app/src/main/java/com/finnvek/knittools/pro/ProState.kt"
        const val PRO_UPGRADE_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt"
        const val YARN_CARD_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardViewModel.kt"
        const val COLOR =
            "app/src/main/java/com/finnvek/knittools/ui/theme/Color.kt"
        const val STRINGS = "app/src/main/res/values/strings.xml"
        const val FINNISH_STRINGS = "app/src/main/res/values-fi/strings.xml"
    }
}
