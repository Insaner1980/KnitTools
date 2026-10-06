package com.finnvek.knittools.ui.screens.yarncard

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YarnCardDetailSourceTest {
    @Test
    fun `empty optional yarn details are shown as an intentional partial-data state`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)

        assertTrue(source.contains("if (detailRows.isEmpty()) {"))
        assertTrue(source.contains("OverviewEmptyText(R.string.yarn_details_empty_body)"))
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
        assertTrue(source.contains("onEditManualDetails = { showManualDetailsSheet = true }"))
        assertTrue(viewModel.contains("fun updateManualDetails("))
        assertTrue(strings.contains("""name="edit_yarn_details""""))
    }

    @Test
    fun `yarn page uses the project overview section structure`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)

        // Hiusviivaosiot ja SectionLabel-otsikot kuten projektinäkymässä, ei osioita korttien sisällä.
        listOf(
            "R.string.status_label",
            "R.string.quantity_label",
            "R.string.linked_project_label",
            "R.string.yarn_details_title",
            "R.string.care_symbols",
        ).forEach { title ->
            val header = Regex("""OverviewSectionHeader\(\s*${Regex.escape(title)}""")
            assertTrue(title, header.containsMatchIn(source))
        }
        assertFalse(source.contains("private fun ActionRow("))
        assertFalse(source.contains("HorizontalDivider("))
        assertFalse(source.contains("typography.labelSmall"))
        assertTrue(source.contains("SectionLabel(text = stringResource(R.string.select_project))"))
    }

    @Test
    fun `quantity uses a large value and the counter 3D buttons`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)

        assertTrue(source.contains("imageRes = R.drawable.counter_minus_button"))
        assertTrue(source.contains("imageRes = R.drawable.counter_plus_button"))
        assertTrue(source.contains("visualSize = ProjectOverviewDimens.StepperVisualSize"))
        assertTrue(source.contains("modifier = Modifier.size(ProjectOverviewDimens.StepperTouchSize)"))
        assertTrue(source.contains("enabled = quantity > 0"))
        assertFalse(source.contains("Icons.Filled.Remove"))
    }

    @Test
    fun `delete lives in the overflow menu instead of a loose bottom button`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)

        assertTrue(source.contains("actions = { YarnDetailMenu(onDelete = { showDeleteDialog = true }) }"))
        assertTrue(source.contains("text = { Text(stringResource(R.string.delete_yarn_card)) }"))
        assertFalse(source.contains("text = stringResource(R.string.delete),"))
    }

    @Test
    fun `top bar title stays hidden until the in-content yarn name scrolls away`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)

        assertTrue(source.contains("val scrollTitle = rememberScrollTitleState()"))
        assertTrue(source.contains("showTitle = scrollTitle.showTitle,"))
        assertTrue(source.contains(".verticalScroll(scrollTitle.scrollState)"))
        assertTrue(source.contains("headerModifier = scrollTitle.headerModifier,"))
    }

    @Test
    fun `yarn photo uses the shared fabric placeholder and explicit picker wording`() {
        val source = ProjectSourceFiles.read(YARN_CARD_DETAIL_SCREEN)
        val viewModel = ProjectSourceFiles.read(YARN_CARD_VIEW_MODEL)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertTrue(source.contains("ActivityResultContracts.PickVisualMedia()"))
        assertTrue(source.contains("PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)"))
        assertTrue(source.contains("viewModel.updatePhotoUri(uri)"))
        assertTrue(source.contains("FabricPhotoPlaceholder("))
        // Langan oikeaa väriä ei tiedetä, joten tilkku on neutraali eikä id:stä laskettu.
        assertTrue(source.contains("color = MaterialTheme.knitToolsColors.yarnSwatchNeutral"))
        assertTrue(source.contains("OverviewHeroPhoto("))
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
