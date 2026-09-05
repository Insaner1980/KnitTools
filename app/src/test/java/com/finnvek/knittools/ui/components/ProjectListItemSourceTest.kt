package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectListItemSourceTest {
    @Test
    fun `section line takes precedence and raw or duplicate pattern names stay hidden`() {
        assertEquals("Sleeve", secondaryLine(" Sleeve ", "Cardigan.pdf", "Cardigan"))
        assertEquals(null, secondaryLine(null, "Cardigan.pdf", "Cardigan"))
        assertEquals("Cozy Cardigan", secondaryLine(null, "Cozy Cardigan", "Cardigan"))
        assertEquals(null, secondaryLine(" ", "Cardigan", "Cardigan"))
    }

    @Test
    fun `list item keeps the required hierarchy and responsive layout`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)
        val item = sourceBetween(source, "fun ProjectListItem(", "private fun ProjectListItemHeader(")
        val header =
            sourceBetween(source, "private fun ProjectListItemHeader(", "private fun ProjectListItemName(")
        val name =
            sourceBetween(source, "private fun ProjectListItemName(", "private fun ProjectListItemContext(")
        val context =
            sourceBetween(source, "private fun ProjectListItemContext(", "private fun ProjectListItemProgress(")
        val count =
            sourceBetween(source, "private fun ProjectListItemCount(", "private fun ProjectListItemTargetStatus(")
        val status =
            sourceBetween(source, "private fun ProjectListItemTargetStatus(", "private fun ProjectListItemFooter(")
        val footer =
            sourceBetween(source, "private fun ProjectListItemFooter(", "private fun projectListTargetStatusText(")
        val hierarchyPositions =
            listOf(
                "ProjectListItemHeader(",
                "ProjectListItemContext(",
                "ProjectListItemProgress(",
                "ProjectListItemFooter(",
            ).map(item::indexOf)

        assertTrue("Missing list item hierarchy call", hierarchyPositions.all { it >= 0 })
        assertTrue("List item hierarchy is out of order", hierarchyPositions.zipWithNext().all { (a, b) -> a < b })
        assertTrue("Header must render the project name", header.contains("ProjectListItemName("))
        assertTrue(name.contains("style = MaterialTheme.typography.titleLarge"))
        assertTrue(count.contains("style = MaterialTheme.typography.titleMedium"))
        assertTrue(context.contains("style = MaterialTheme.typography.bodyMedium"))
        assertTrue(header.contains("style = MaterialTheme.typography.bodySmall"))
        assertTrue(footer.contains("style = MaterialTheme.typography.bodySmall"))
        assertTrue(status.contains("style = MaterialTheme.typography.labelLarge"))
        assertTrue(item.contains("usesCompactProjectListItemLayout("))
        assertTrue(name.contains("maxLines = 2"))
    }

    @Test
    fun `list item is cardless neutral and image free`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)

        assertFalse(source.contains("Surface("))
        assertFalse(source.contains("BorderStroke"))
        assertFalse(source.contains("KeyboardArrowRight"))
        assertFalse(source.contains("YarnColors"))
        assertFalse(source.contains('\u00b7'))
    }

    @Test
    fun `target status and progress use the shared target model`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)
        val item = sourceBetween(source, "fun ProjectListItem(", "private fun ProjectListItemHeader(")
        val progress =
            sourceBetween(source, "private fun ProjectListItemProgress(", "private fun ProjectListItemCount(")
        val statusText =
            sourceBetween(
                source,
                "private fun projectListTargetStatusText(",
                "private fun projectListItemSecondaryLine(",
            )
        val normalizedProgress = normalizeWhitespace(progress)

        assertTrue(
            item.contains("mainCounterTargetStatus(counterDisplay.targetLine).takeUnless { project.isCompleted }"),
        )
        assertTrue(
            item.contains("mainCounterTargetFraction(counterDisplay.targetLine).takeUnless { project.isCompleted }"),
        )
        assertTrue(statusText.contains("is MainCounterTargetStatus.Remaining"))
        assertTrue(statusText.contains("MainCounterTargetStatus.Reached"))
        assertTrue(statusText.contains("is MainCounterTargetStatus.Past"))
        assertTrue(progress.contains("statusText?.let { ProjectListItemTargetStatus(it) }"))
        assertTrue(progress.contains("if (progressFraction != null)"))
        assertTrue(
            normalizedProgress.contains(
                ".fillMaxWidth() .padding(horizontal = ProjectListDimens.ProgressTrackInset) " +
                    ".height(ProjectListDimens.ProgressTrackHeight) " +
                    ".background( MaterialTheme.colorScheme.onSurface.copy( " +
                    "alpha = ProjectListDimens.ProgressTrackAlpha, ), )",
            ),
        )
        assertTrue(
            normalizedProgress.contains(
                ".fillMaxWidth(progressFraction) .height(ProjectListDimens.ProgressTrackHeight) " +
                    ".background(MaterialTheme.colorScheme.primary)",
            ),
        )
    }

    @Test
    fun `footer keeps yarn and attachment actions together with accessible targets`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)
        val footer =
            sourceBetween(
                source,
                "private fun ProjectListItemFooter(",
                "private fun ProjectListItemAttachmentActions(",
            )
        val actions =
            sourceBetween(
                source,
                "private fun ProjectListItemAttachmentActions(",
                "private fun ProjectListItemAttachmentAction(",
            )
        val action =
            sourceBetween(
                source,
                "private fun ProjectListItemAttachmentAction(",
                "private fun projectListTargetStatusText(",
            )
        val dimens = ProjectSourceFiles.read(PROJECT_LIST_DIMENS)
        val footerPositions = listOf("text = yarnName", "ProjectListItemAttachmentActions(").map(footer::indexOf)

        assertTrue(footer.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue("Missing footer row content", footerPositions.all { it >= 0 })
        assertTrue("Footer content is out of order", footerPositions[0] < footerPositions[1])
        assertTrue(footer.contains(".defaultMinSize(minHeight = ProjectListDimens.FooterActionTouchSize)"))
        assertTrue(footer.contains("Modifier.clickable(onClick = onYarnClick)"))
        assertTrue(actions.contains("onPatternClick: (() -> Unit)?"))
        assertTrue(actions.contains("onPhotosClick: (() -> Unit)?"))
        assertTrue(actions.contains("onNotesClick: (() -> Unit)?"))
        assertTrue(actions.contains("label = formatIntegerForDisplay(photoCount.toLong()"))
        assertTrue(action.contains("minWidth = ProjectListDimens.FooterActionTouchSize"))
        assertTrue(action.contains("minHeight = ProjectListDimens.FooterActionTouchSize"))
        assertTrue(action.contains("Modifier.clickable(onClick = onClick)"))
        assertTrue(action.contains("modifier = Modifier.size(ProjectListDimens.FooterIconSize)"))
        assertTrue(dimens.contains("val FooterActionTouchSize = 48.dp"))
    }

    @Test
    fun `list item uses Projects dimension tokens`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)

        assertTrue(source.contains("ProjectListDimens.ProjectItemVerticalPadding"))
        assertTrue(source.contains("ProjectListDimens.ItemLineGap"))
        assertTrue(source.contains("ProjectListDimens.ProgressGroupTopGap"))
        assertTrue(source.contains("ProjectListDimens.ProgressTrackInset"))
        assertTrue(source.contains("ProjectListDimens.FooterTopGap"))
    }

    @Test
    fun `selection uses a checkbox fill and selected semantics`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)
        val item = sourceBetween(source, "fun ProjectListItem(", "private fun ProjectListItemHeader(")
        val actions =
            sourceBetween(
                source,
                "private fun ProjectListItemAttachmentActions(",
                "private fun ProjectListItemAttachmentAction(",
            )
        val normalizedItem = normalizeWhitespace(item)

        assertTrue(item.contains("val selectionModifier ="))
        assertTrue(item.contains("Modifier.semantics { this.selected = isSelected }"))
        assertTrue(item.contains("if (selected == true)"))
        assertTrue(normalizedItem.contains(".then(selectionModifier) .combinedClickable("))
        assertTrue(item.contains("onClick = onClick"))
        assertTrue(item.contains("Checkbox("))
        assertTrue(normalizedItem.contains(".width(48.dp) .defaultMinSize(minHeight = 48.dp)"))
        assertTrue(item.contains("onYarnClick = onYarnClick.takeIf { selected == null }"))
        assertTrue(item.contains("onPatternClick = onPatternClick.takeIf { selected == null }"))
        assertTrue(item.contains("onNotesClick = onNotesClick.takeIf { selected == null }"))
        assertTrue(item.contains("onPhotosClick = onPhotosClick.takeIf { selected == null }"))
        assertTrue(item.contains("actionsEnabled = selected == null"))
        assertTrue(
            actions.contains(
                "contentDescription = stringResource(R.string.pattern_viewer_title)",
            ),
        )
        assertTrue(
            actions.contains(
                "contentDescription = stringResource(R.string.progress_photos)",
            ),
        )
        assertTrue(
            actions.contains(
                "contentDescription = stringResource(R.string.notes)",
            ),
        )
    }

    @Test
    fun `completed item shows final count and localized completion age without target progress`() {
        val source = ProjectSourceFiles.read(PROJECT_LIST_ITEM)

        assertTrue(source.contains("projectTimestampText(lastUpdated, project.isCompleted)"))
        assertTrue(source.contains(".takeUnless { project.isCompleted }"))
        assertTrue(
            source.contains(
                "if (isCompleted) R.string.project_completed_format else R.string.project_updated_format",
            ),
        )
        assertTrue(source.contains("?: mainCounterCountText(counterDisplay.projectCardCount)"))
        assertTrue(source.contains("DateUtils"))
        assertTrue(source.contains(".getRelativeTimeSpanString("))
    }

    private fun secondaryLine(
        sectionName: String?,
        patternName: String?,
        projectName: String,
    ): String? =
        projectListItemKt()
            .getDeclaredMethod(
                "projectListItemSecondaryLine",
                String::class.java,
                String::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            .invoke(null, sectionName, patternName, projectName) as String?

    private fun projectListItemKt(): Class<*> = Class.forName("com.finnvek.knittools.ui.components.ProjectListItemKt")

    private fun sourceBetween(
        source: String,
        start: String,
        end: String,
    ): String {
        val startIndex = source.indexOf(start)
        check(startIndex >= 0) { "Missing source marker: $start" }
        val endIndex = source.indexOf(end, startIndex + start.length)
        check(endIndex > startIndex) { "Missing source marker after $start: $end" }
        return source.substring(startIndex, endIndex)
    }

    private fun normalizeWhitespace(source: String): String = source.replace(WHITESPACE, " ").trim()

    private companion object {
        private const val PROJECT_LIST_ITEM =
            "app/src/main/java/com/finnvek/knittools/ui/components/ProjectListItem.kt"
        private const val PROJECT_LIST_DIMENS =
            "app/src/main/java/com/finnvek/knittools/ui/theme/ProjectListDimens.kt"
        private val WHITESPACE = "\\s+".toRegex()
    }
}
