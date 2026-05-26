package com.finnvek.knittools.ui.screens.yarncard

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class YarnCardDetailSourceTest {
    @Test
    fun `empty optional yarn details are shown as an intentional partial-data state`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)

        assertTrue(source.contains("YarnDetailsEmptyState("))
        assertTrue(source.contains("stringResource(R.string.yarn_details_empty_body)"))
        assertTrue(source.contains("if (detailRows.isEmpty()) {"))
        assertTrue(source.contains("YarnDetailsEmptyState("))
        assertTrue(source.contains("return"))
    }

    @Test
    fun `manual yarn details can be edited from the detail screen`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)
        val viewModel = ProjectSourceFiles.read(YARN_CARD_VIEW_MODEL)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertTrue(source.contains("showManualDetailsSheet"))
        assertTrue(source.contains("ManualYarnCardSheet("))
        assertTrue(source.contains("initialInput = form.toManualYarnCardInput()"))
        assertTrue(source.contains("titleRes = R.string.edit_yarn_details"))
        assertTrue(source.contains("viewModel.updateManualDetails(input)"))
        assertTrue(source.contains("TextButton(onClick = onEditManualDetails)"))
        assertTrue(source.contains("stringResource(R.string.edit_yarn_details)"))
        assertTrue(viewModel.contains("fun updateManualDetails("))
        assertTrue(strings.contains("""name="edit_yarn_details""""))
    }

    @Test
    fun `yarn photo action uses explicit photo picker wording`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)
        val viewModel = ProjectSourceFiles.read(YARN_CARD_VIEW_MODEL)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertTrue(source.contains("ActivityResultContracts.PickVisualMedia()"))
        assertTrue(source.contains("PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)"))
        assertTrue(source.contains("viewModel.updatePhotoUri(uri)"))
        assertTrue(source.contains("onAddYarnPhoto"))
        assertTrue(source.contains("R.string.add_yarn_photo"))
        assertTrue(source.contains("R.string.change_yarn_photo"))
        assertTrue(viewModel.contains("fun updatePhotoUri("))
        assertTrue(strings.contains("""name="add_yarn_photo""""))
        assertTrue(strings.contains("""name="change_yarn_photo""""))
    }

    @Test
    fun `yarn detail empty copy is localized with practical manual wording`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val text = ProjectSourceFiles.read(file)

            assertTrue(
                "$file is missing yarn_details_empty_body",
                text.contains("""name="yarn_details_empty_body""""),
            )
            assertTrue(
                "$file is missing add_yarn_photo",
                text.contains("""name="add_yarn_photo""""),
            )
            assertTrue(
                "$file is missing change_yarn_photo",
                text.contains("""name="change_yarn_photo""""),
            )
        }

        val baseStrings = ProjectSourceFiles.read(STRINGS)
        assertTrue(baseStrings.contains("Brand, weight, color, and dye lot are optional."))
        assertTrue(baseStrings.contains("Add only what helps this project."))
    }

    private companion object {
        private const val YARN_CARD_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt"
        private const val YARN_CARD_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardViewModel.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
