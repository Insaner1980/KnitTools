package com.finnvek.knittools.ui.theme

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyFontFallbackSourceTest {
    @Test
    fun `Outfit-painot käyttävät järjestelmäfonttia latauksen varalla`() {
        val type = ProjectSourceFiles.read(TYPE)

        assertTrue(type.contains("loadingStrategy = FontLoadingStrategy.OptionalLocal"))
        assertTrue(type.contains("Font(DeviceFontFamilyName(\"sans-serif\"), weight = weight)"))
    }

    private companion object {
        const val TYPE = "app/src/main/java/com/finnvek/knittools/ui/theme/Type.kt"
    }
}
