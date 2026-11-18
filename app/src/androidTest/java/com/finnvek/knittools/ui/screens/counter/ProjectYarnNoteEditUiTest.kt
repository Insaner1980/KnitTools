package com.finnvek.knittools.ui.screens.counter

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inspector.WindowInspector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ProjectYarnNoteEditResult
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.navigation.awaitKeyboard
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class ProjectYarnNoteEditUiTest(
    private val language: String,
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: ProjectYarnNoteRepository
    private lateinit var context: Context
    private var failSave = false
    private var attempts = 0
    private var pendingSave: CompletableDeferred<ProjectYarnNoteEditResult>? = null

    @Before
    fun setUp() =
        runBlocking {
            check(base.packageName == "com.finnvek.knittools.activepolicytest")
            val args = InstrumentationRegistry.getArguments()
            assertEquals(
                requireNotNull(args.getString("yarnWidth")).toInt(),
                base.resources.configuration.screenWidthDp,
            )
            assertEquals(
                requireNotNull(args.getString("yarnFontScale")).toFloat(),
                base.resources.configuration.fontScale,
            )
            database = Room.inMemoryDatabaseBuilder(base, KnitToolsDatabase::class.java).build()
            database.counterProjectDao().insert(
                CounterProjectEntity(id = 1, name = "Synthetic project", count = 7, updatedAt = 10),
            )
            database.yarnCardDao().upsert(YarnCardEntity(id = 3, yarnName = "Library original", quantityInStash = 9))
            database.projectYarnNoteDao().upsert(
                ProjectYarnNoteEntity(
                    id = 4,
                    projectId = 1,
                    name = "Merino",
                    description = "Silk blend",
                    quantity = 3,
                    notes = "Hold double",
                ),
            )
            val runner = RoomDatabaseTransactionRunner(database)
            repository =
                ProjectYarnNoteRepository(
                    database.projectYarnNoteDao(),
                    YarnCardRepository(
                        database.yarnCardDao(),
                        database.counterProjectDao(),
                        base,
                        runner,
                        Dispatchers.IO,
                    ),
                    runner,
                    database.projectYarnUsageDao(),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun opensPrefilledCancelsWithoutWritingThenSavesSameRowAndReopens() {
        render()
        val initial = note()
        openEditor()
        assertFields("Merino", "Silk blend", "3", "Hold double")
        input("name").performScrollTo().performTextReplacement("Discard me")
        compose.onNodeWithText(context.getString(R.string.cancel)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(initial, note()) }
        openEditor()
        assertFields("Merino", "Silk blend", "3", "Hold double")
        input("name").performScrollTo().performTextReplacement("  Edited wool  ")
        input("description").performScrollTo().performTextReplacement("  Cashmere  ")
        input("quantity").performScrollTo().performTextReplacement("6")
        input("notes").performScrollTo().performTextReplacement("  Sleeves  ")
        save()
        compose.waitUntil(5_000) { note().name == "Edited wool" }
        assertEquals(
            initial.copy(
                name = "Edited wool",
                description = "Cashmere",
                quantity = 6,
                notes = "Sleeves",
                updatedAt = note().updatedAt,
            ),
            note(),
        )
        openEditor()
        assertFields("Edited wool", "Cashmere", "6", "Sleeves")
        assertEquals(7, runBlocking { database.counterProjectDao().getProject(1)?.count })
        assertEquals(10L, runBlocking { database.counterProjectDao().getProject(1)?.updatedAt })
    }

    @Test
    fun invalidNameAndFailureKeepDraftThenLinkedNoteCanRetry() {
        runBlocking { database.projectYarnNoteDao().updateSavedYarnCardId(4, 3, 100) }
        render()
        val initial = note()
        openEditor()
        input("name").performScrollTo().performTextReplacement("   ")
        compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().assertIsNotEnabled()
        assertEquals(initial, note())
        input("name").performScrollTo().performTextReplacement("Changed locally")
        failSave = true
        save()
        compose
            .onNodeWithText(
                context.getString(R.string.project_yarn_edit_failed),
            ).performScrollTo()
            .assertIsDisplayed()
        assertEquals(initial, note())
        input(
            "name",
        ).performScrollTo().assert(
            editableTextEquals("Changed locally"),
        )
        failSave = false
        save()
        compose.waitUntil(5_000) { note().name == "Changed locally" }
        assertEquals(2, attempts)
        assertEquals(3L, note().savedYarnCardId)
        assertEquals("Library original", runBlocking { database.yarnCardDao().getCard(3)?.yarnName })
        assertEquals(9, runBlocking { database.yarnCardDao().getCard(3)?.quantityInStash })
    }

    @Test
    fun keyboardScrollingTouchTargetsTranslationsAndAccessibilityFocus() {
        render()
        openEditor()
        input("notes").performScrollTo().performClick().assertIsFocused()
        compose.awaitKeyboard(true)
        input("notes").performTextReplacement("Long synthetic note\nSecond line\nThird line")
        val controls =
            listOf(
                input("name"),
                input("description"),
                input("quantity"),
                input("notes"),
                compose.onNodeWithText(context.getString(R.string.cancel)),
                compose.onNodeWithText(context.getString(R.string.save)),
            )
        controls.forEach { control ->
            control.performScrollTo().assertIsDisplayed()
            val bounds = control.fetchSemanticsNode().touchBoundsInRoot
            val density = context.resources.displayMetrics.density
            assertTrue("Touch height", bounds.height / density >= 47.5f)
            assertTrue("Touch width", bounds.width / density >= 47.5f)
        }
        val texts =
            compose.onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            )
        texts.fetchSemanticsNodes().forEachIndexed { index, _ ->
            val layouts = mutableListOf<TextLayoutResult>()
            texts[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.forEach { layout ->
                repeat(
                    layout.lineCount,
                ) { line -> assertFalse(layout.layoutInput.text.text, layout.isLineEllipsized(line)) }
            }
        }
        val automation =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
        val serviceInfo = automation.serviceInfo
        val originalFlags = serviceInfo.flags
        try {
            serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            automation.serviceInfo = serviceInfo
            val accessibility = base.getSystemService(AccessibilityManager::class.java)
            compose.waitUntil(10_000) {
                accessibility.isTouchExplorationEnabled
            }
            var accessible: AccessibilityNodeInfo? = null
            compose.waitUntil(5_000) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    automation.clearCache()
                }
                accessible = automation.rootInActiveWindow?.let { findSave(it) }
                accessible != null
            }
            val button = requireNotNull(accessible)
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
            compose.waitUntil(5_000) { button.refresh() && button.isAccessibilityFocused }
            val screenshot = requireNotNull(automation.takeScreenshot())
            val configuration = base.resources.configuration
            File(
                base.getExternalFilesDir(null),
                "yarn-edit-$language-$dark-${configuration.screenWidthDp}-${configuration.fontScale}.png",
            ).outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            compose.waitUntil(5_000) { note().notes.startsWith("Long synthetic note") }
        } finally {
            serviceInfo.flags = originalFlags
            automation.serviceInfo = serviceInfo
        }
    }

    @Test
    fun competingEditShowsConflictAndKeepsDraftUntilExplicitClose() {
        render()
        openEditor()
        input("name").performScrollTo().performTextReplacement("My draft")
        val original = note().toDomain()
        runBlocking { repository.edit(original, original.copy(name = "Other editor")) }
        save()
        compose
            .onNodeWithText(
                context.getString(R.string.project_yarn_edit_conflict),
            ).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().assertIsNotEnabled()
        input(
            "name",
        ).performScrollTo().assert(
            editableTextEquals("My draft"),
        )
        assertEquals("Other editor", note().name)
        compose.onNodeWithText(context.getString(R.string.cancel)).performScrollTo().performClick()
        openEditor()
        assertFields("Other editor", "Silk blend", "3", "Hold double")
    }

    @Test
    fun deletionWhileOpenShowsMissingAndDoesNotRecreate() {
        render()
        openEditor()
        input("name").performScrollTo().performTextReplacement("My draft")
        runBlocking { repository.delete(4) }
        save()
        compose
            .onNodeWithText(
                context.getString(R.string.project_yarn_edit_missing),
            ).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().assertIsNotEnabled()
        assertEquals(null, runBlocking { database.projectYarnNoteDao().getById(4) })
    }

    private fun findSave(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && node.text?.toString() == context.getString(R.string.save)) {
            return generateSequence(node) { it.parent }.firstOrNull {
                it.isClickable &&
                    it.packageName == base.packageName
            }
        }
        repeat(node.childCount) { index -> node.getChild(index)?.let { findSave(it)?.let { found -> return found } } }
        return null
    }

    @Test
    fun pendingSavePreventsDismissalAndFailureKeepsEditorVisible() {
        render()
        openEditor()
        input("name").performScrollTo().performTextReplacement("Pending draft")
        val pending = CompletableDeferred<ProjectYarnNoteEditResult>()
        pendingSave = pending
        save()
        input("name").assertIsNotFocused()
        compose.onNodeWithText(context.getString(R.string.cancel)).performScrollTo().assertIsNotEnabled()
        Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            WindowInspector.getGlobalWindowViews().none {
                ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
        }
        Espresso.pressBack()
        compose.waitForIdle()
        input("name").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { pending.complete(ProjectYarnNoteEditResult.Failed) }
        compose
            .onNodeWithText(context.getString(R.string.project_yarn_edit_failed))
            .performScrollTo()
            .assertIsDisplayed()
        input("name").performScrollTo().assertIsDisplayed()
        assertEquals("Merino", note().name)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun render() {
        context =
            base.localizedTestContext(language)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
            ) {
                KnitToolsTheme(isDarkTheme = dark) {
                    val notes by repository.observeForProject(1).collectAsState(emptyList())
                    YarnManagementSheet(
                        linkedYarns = emptyList(),
                        projectYarnNotes = notes,
                        proState = ProState(status = ProStatus.TRIAL_EXPIRED),
                        proAccessReady = true,
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        actions =
                            YarnManagementSheetActions(
                                onUnlinkYarn = {},
                                onAddYarn = {},
                                onSaveProjectYarnNote = {
                                    _,
                                    _,
                                    _,
                                    _,
                                    ->
                                    kotlinx.coroutines.CompletableDeferred(
                                        com.finnvek.knittools.repository.ProjectYarnNoteEditResult.Saved,
                                    )
                                },
                                onDeleteProjectYarnNote = {},
                                onSaveProjectYarnNoteToMyYarn = {},
                                onDismiss = {},
                                onEditProjectYarnNote = { original, draft ->
                                    attempts++
                                    if (pendingSave != null) {
                                        requireNotNull(pendingSave).await()
                                    } else if (failSave) {
                                        ProjectYarnNoteEditResult.Failed
                                    } else {
                                        withContext(
                                            Dispatchers.IO,
                                        ) { repository.edit(original, draft) }
                                    }
                                },
                            ),
                    )
                }
            }
        }
    }

    private fun openEditor() {
        compose.onNodeWithContentDescription(context.getString(R.string.more_options)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.project_overview_edit)).performClick()
        compose.onNodeWithText(context.getString(R.string.project_yarn_edit_title)).assertExists()
    }

    private fun input(field: String) = compose.onNodeWithTag("project-yarn-$field")

    private fun assertFields(
        name: String,
        description: String,
        quantity: String,
        notes: String,
    ) {
        val fields = listOf("name" to name, "description" to description, "quantity" to quantity, "notes" to notes)
        fields.forEach { (field, value) ->
            input(
                field,
            ).performScrollTo().assert(
                editableTextEquals(value),
            )
        }
    }

    private fun save() = compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()

    private fun editableTextEquals(value: String) =
        SemanticsMatcher("Editable text equals $value") { it.config[SemanticsProperties.EditableText].text == value }

    private fun note() = runBlocking { requireNotNull(database.projectYarnNoteDao().getById(4)) }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "language={0} dark={1}")
        fun configurations(): List<Array<Any>> =
            listOf("en", "fi", "de").flatMap {
                listOf(arrayOf(it, false), arrayOf(it, true))
            }
    }
}
