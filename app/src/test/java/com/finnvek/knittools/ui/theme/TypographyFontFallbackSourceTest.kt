package com.finnvek.knittools.ui.theme

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class TypographyFontFallbackSourceTest {
    @Test
    fun `fonttipainot käyttävät järjestelmäfonttia latauksen varalla`() {
        val type = ProjectSourceFiles.read(TYPE)

        assertTrue(type.contains("loadingStrategy = FontLoadingStrategy.OptionalLocal"))
        assertTrue(type.contains("Font(DeviceFontFamilyName(\"sans-serif\"), weight = weight)"))
    }

    @Test
    fun `Barlow Semi Condensed kulkee lisenssinsä kanssa eikä Outfit ole enää mukana`() {
        val type = ProjectSourceFiles.read(TYPE)

        listOf("regular", "medium", "semibold", "bold", "extrabold").forEach { weight ->
            assertTrue(weight, type.contains("R.font.barlow_semi_condensed_$weight"))
            assertTrue(weight, Files.exists(ProjectSourceFiles.file("$FONT_DIR/barlow_semi_condensed_$weight.ttf")))
        }
        // SIL OFL edellyttää, että lisenssiteksti jaetaan fontin mukana.
        assertTrue(Files.exists(ProjectSourceFiles.file(LICENSE)))
        assertFalse(type.contains("R.font.outfit"))
    }

    private companion object {
        const val TYPE = "app/src/main/java/com/finnvek/knittools/ui/theme/Type.kt"
        const val FONT_DIR = "app/src/main/res/font"
        const val LICENSE = "app/src/main/assets/licenses/barlow_semi_condensed_ofl.txt"
    }
}
