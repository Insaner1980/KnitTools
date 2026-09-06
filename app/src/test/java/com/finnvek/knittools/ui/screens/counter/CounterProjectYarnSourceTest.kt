package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class CounterProjectYarnSourceTest {
    @Test
    fun `counter state observes project-only yarn notes and exposes save events`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val sheet = ProjectSourceFiles.read(YARN_MANAGEMENT_SHEET)

        assertTrue(viewModel.contains("ProjectYarnNoteRepository"))
        assertTrue(viewModel.contains("val projectYarnNotes: List<ProjectYarnNote> = emptyList()"))
        assertTrue(viewModel.contains("observeProjectYarnNotes(project.id)"))
        assertTrue(viewModel.contains("fun saveProjectYarnNote("))
        assertTrue(viewModel.contains("fun deleteProjectYarnNote("))
        assertTrue(viewModel.contains("fun saveProjectYarnNoteToMyYarn("))
        assertTrue(screen.contains("projectYarnNotes = state.projectYarnNotes"))
        assertTrue(screen.contains("onSaveProjectYarnNote = viewModel::saveProjectYarnNote"))
        assertTrue(screen.contains("onDeleteProjectYarnNote = viewModel::deleteProjectYarnNote"))
        assertTrue(screen.contains("onSaveProjectYarnNoteToMyYarn = { noteId ->"))
        assertTrue(screen.contains("viewModel.saveProjectYarnNoteToMyYarn(noteId)"))
        assertTrue(sheet.contains("R.string.choose_from_my_yarn"))
        assertTrue(sheet.contains("R.string.add_yarn_to_project"))
    }

    @Test
    fun `project-only yarn notes stay in management sheet instead of card previews`() {
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)

        assertTrue(contentCards.contains("ProjectContentCardKind.YARN"))
        assertTrue(contentCards.contains("R.string.project_content_yarn"))
        assertFalse(contentCards.contains("projectYarnNoteNames"))
        assertFalse(contentCards.contains("linkedYarnNames"))
        assertTrue(screen.contains("projectYarnNotes = state.projectYarnNotes"))
    }

    @Test
    fun `yarn management uses large option cards with supporting copy`() {
        val sheet = ProjectSourceFiles.read(YARN_MANAGEMENT_SHEET)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertTrue(sheet.contains("YarnOptionCard("))
        assertTrue(sheet.contains("R.string.choose_from_my_yarn_body"))
        assertTrue(sheet.contains("R.string.add_yarn_to_project_body"))
        assertTrue(strings.contains("""<string name="choose_from_my_yarn_body">"""))
        assertTrue(strings.contains("""<string name="add_yarn_to_project_body">"""))
    }

    @Test
    fun `linked yarn indicator uses the active theme palette with stable id mapping`() {
        val sheet = ProjectSourceFiles.read(YARN_MANAGEMENT_SHEET)

        assertTrue(sheet.contains("yarnColorForId(id, MaterialTheme.knitToolsColors.yarnPalette)"))
        assertFalse(sheet.contains(".background(YarnColors["))
    }

    @Test
    fun `save explanation stays in its entry and follows persisted saved state`() {
        val row =
            ProjectSourceFiles
                .read(YARN_MANAGEMENT_SHEET)
                .substringAfter("private fun ProjectYarnNoteRow(")
                .substringBefore("private fun ProjectYarnNote.summaryText")
        val explanation = row.substringAfterLast("Text(")

        assertTrue(row.contains("onClick = { onSaveProjectYarnNoteToMyYarn(note.id) }"))
        assertTrue(row.contains("enabled = note.savedYarnCardId == null"))
        val savedLabelIndex = row.indexOf("R.string.saved_to_my_yarn")
        assertTrue(savedLabelIndex >= 0)
        assertTrue(savedLabelIndex < row.indexOf("R.string.save_to_my_yarn_explanation"))
        assertTrue(
            Regex(
                """if \(note.savedYarnCardId == null\) \{""" +
                    """\s*R.string.save_to_my_yarn_explanation\s*""" +
                    """} else \{\s*R.string.saved_to_my_yarn_explanation""",
            ).containsMatchIn(explanation),
        )
        assertTrue(explanation.contains("MaterialTheme.typography.bodySmall"))
        assertTrue(explanation.contains("MaterialTheme.colorScheme.onSurfaceVariant"))
        assertFalse(explanation.contains("maxLines"))
        assertFalse(explanation.contains("TextOverflow.Ellipsis"))
        assertFalse(explanation.contains("proStatus"))
    }

    @Test
    fun `all configured locales explain both project yarn save states`() {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val locales =
            builder
                .parse(ProjectSourceFiles.file("app/src/main/res/xml/locales_config.xml").toFile())
                .getElementsByTagName("locale")
        val directories =
            (0 until locales.length)
                .map { index ->
                    val language = (locales.item(index) as Element).getAttribute("android:name")
                    if (language == "en") "values" else "values-$language"
                }.toSet()
        val files = ProjectSourceFiles.localizedStringFiles()
        assertEquals(directories, files.map { it.parent.fileName.toString() }.toSet())
        files.forEach { file ->
            val strings = builder.parse(file.toFile()).getElementsByTagName("string")
            val explanations =
                (0 until strings.length)
                    .map { strings.item(it) as Element }
                    .filter { it.getAttribute("name") in EXPLANATION_KEYS }
            assertEquals("$file must define both explanations", EXPLANATION_KEYS.size, explanations.size)
            explanations.forEach { resource ->
                val text = resource.textContent
                assertTrue("$file explanation must not be blank", text.isNotBlank())
                assertFalse("$file explanation needs no formatting arguments", text.contains('%'))
                if (file.parent.fileName.toString() == "values") {
                    if (resource.getAttribute("name") == "save_to_my_yarn_explanation") {
                        assertTrue(text.contains("linked to this project"))
                        assertTrue(text.contains("its notes are not copied"))
                    } else {
                        assertTrue(text.contains("entry stays separate"))
                        assertTrue(text.contains("notes were not copied"))
                    }
                } else {
                    assertFalse("$file must translate the explanation", text.startsWith("Copies the yarn"))
                    assertFalse("$file must translate the saved explanation", text.startsWith("This project entry"))
                }
            }
        }
    }

    private companion object {
        private val EXPLANATION_KEYS = setOf("save_to_my_yarn_explanation", "saved_to_my_yarn_explanation")
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val YARN_MANAGEMENT_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/YarnManagementSheet.kt"
        private const val COUNTER_PROJECT_CONTENT_CARDS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
