package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryDetailFlowSourceTest {
    @Test
    fun `detail screen reports save result from view model events`() {
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)

        assertTrue(detailScreen.contains("patternSaveResults.collect"))
        assertTrue(detailScreen.contains("PatternSaveResult.Saved"))
        assertTrue(detailScreen.contains("PatternSaveResult.Failed"))
        assertTrue(detailScreen.contains("onSave = { viewModel.savePattern() }"))
        assertFalse(detailScreen.contains("viewModel.savePattern()\n                        Toast"))
    }

    @Test
    fun `open in ravelry is guarded by permalink and activity failure handling`() {
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)
        val externalLinks = ProjectSourceFiles.read(RAVELRY_EXTERNAL_LINKS)

        assertTrue(detailScreen.contains("fun PatternDetail.ravelryUrlOrNull()"))
        assertTrue(detailScreen.contains("permalink.isBlank()"))
        assertTrue(detailScreen.contains("openRavelryUrl("))
        assertTrue(externalLinks.contains("ActivityNotFoundException"))
        assertTrue(externalLinks.contains("runCatching"))
    }

    @Test
    fun `external ravelry link helper validates URL before launching view intent`() {
        val externalLinks = ProjectSourceFiles.read(RAVELRY_EXTERNAL_LINKS)

        assertTrue(externalLinks.contains("ravelryExternalUriOrNull(url)"))
        assertTrue(externalLinks.contains("Intent(Intent.ACTION_VIEW, uri)"))
        assertFalse(externalLinks.contains("Intent(Intent.ACTION_VIEW, url.toUri())"))
    }

    @Test
    fun `detail screen offers reconnect action for authentication errors`() {
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)
        val viewModel = ProjectSourceFiles.read(RAVELRY_VIEW_MODEL)

        assertTrue(viewModel.contains("val detailError"))
        assertTrue(viewModel.contains("_detailError.value = e.toSearchError()"))
        assertTrue(detailScreen.contains("detailError.collectAsStateWithLifecycle"))
        assertTrue(detailScreen.contains("RavelrySearchError.Authentication"))
        assertTrue(detailScreen.contains("RavelryAccountHeader"))
    }

    private companion object {
        private const val RAVELRY_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt"
        private const val RAVELRY_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryViewModel.kt"
        private const val RAVELRY_EXTERNAL_LINKS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryExternalLinks.kt"
    }
}
