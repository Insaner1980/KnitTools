package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryDetailFlowSourceTest {
    @Test
    fun `pdf save explanation is limited to the unsaved detail action`() {
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)
        val actions =
            detailScreen.substringAfter("private fun PatternActions(").substringBefore("private fun DetailRow(")
        assertTrue(actions.contains("if (!isSaved) {"))
        val explanation = actions.substringAfter("if (!isSaved) {").substringBefore("\n    }")

        assertTrue(explanation.contains("stringResource(R.string.ravelry_save_pattern_explanation)"))
        assertFalse(explanation.contains("maxLines"))
        assertFalse(explanation.contains("TextOverflow"))
        assertTrue(actions.contains("onClick = onSave"))
        assertTrue(actions.contains("enabled = !isSaved"))
        val body =
            detailScreen
                .substringAfter("private fun PatternDetailBody(")
                .substringBefore("private fun RavelrySearchError.")
        val loading = body.substringAfter("isLoading -> {").substringBefore("detail != null -> {")
        assertTrue(loading.contains("CircularProgressIndicator()"))
        assertFalse(loading.contains("PatternDetailContent("))
        assertTrue(body.substringAfter("detail != null -> {").contains("isSaved = isSaved"))
    }

    @Test
    fun `saved ravelry pdf explanation uses attachment absence rather than offline flag`() {
        val detail = ProjectSourceFiles.read(SAVED_PATTERN_DETAIL_SCREEN)
        val availability =
            detail
                .substringAfter("SavedPatternAvailability(pattern = pattern,")
                .substringBefore("SavedPatternDetailActions(")
        assertTrue(availability.contains("if (pattern.requiresRavelryAccess) {"))
        val explanation =
            availability.substringAfter("if (pattern.requiresRavelryAccess) {").substringBefore("\n                }")

        assertTrue(explanation.contains("stringResource(R.string.saved_pattern_detail_no_pdf_explanation)"))
        assertFalse(explanation.contains("maxLines"))
        assertFalse(explanation.contains("TextOverflow"))
        val helpers =
            detail
                .substringAfter("private val SavedPattern.hasAttachedPdf:")
                .substringBefore("private fun SavedPattern.ravelryUrlOrNull")
        assertTrue(helpers.contains("get() = !localPdfUri.isNullOrBlank()"))
        assertTrue(helpers.contains("get() = source == SavedPatternSource.Ravelry && !hasAttachedPdf"))
        assertFalse(helpers.contains("isAvailableOffline"))
        val webContent =
            detail
                .substringAfter("private fun WebPatternDetailContent(")
                .substringBefore("private fun SavedPatternDetailHeader(")
        assertTrue(webContent.contains("R.string.web_pattern_not_offline"))
        assertFalse(webContent.contains("R.string.saved_pattern_detail_no_pdf_explanation"))
    }

    @Test
    fun `detail screen reports save result from view model events`() {
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)

        assertTrue(detailScreen.contains("CollectWithLifecycleEffect({ viewModel.patternSaveResults })"))
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
        assertFalse(externalLinks.contains("ActivityNotFoundException"))
        assertTrue(externalLinks.contains("runCatching"))
        assertTrue(externalLinks.contains("onFailure = { false }"))
    }

    @Test
    fun `external ravelry link helper validates URL before launching view intent`() {
        val externalLinks = ProjectSourceFiles.read(RAVELRY_EXTERNAL_LINKS)

        assertTrue(externalLinks.contains("ravelryExternalUriOrNull(url)"))
        assertTrue(externalLinks.contains("Intent(Intent.ACTION_VIEW, uri)"))
        assertTrue(externalLinks.contains("addCategory(Intent.CATEGORY_BROWSABLE)"))
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
        private const val SAVED_PATTERN_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternDetailScreen.kt"
        private const val RAVELRY_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt"
        private const val RAVELRY_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryViewModel.kt"
        private const val RAVELRY_EXTERNAL_LINKS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryExternalLinks.kt"
    }
}
