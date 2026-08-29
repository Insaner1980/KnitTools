package com.finnvek.knittools.ui.screens.counter

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ProjectYarnUsageRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ProjectYarnUsageScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val stores = mutableListOf<ViewModelStore>()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: ProjectYarnUsageRepository
    private lateinit var yarnRepository: YarnCardRepository
    private lateinit var noteRepository: ProjectYarnNoteRepository
    private var imperialBefore = false
    private var yarnName = "Project mohair"
    private lateinit var localesBefore: LocaleList
    private var fontBefore = "1.0"

    @Before
    fun setUp() {
        localesBefore = context.getSystemService(LocaleManager::class.java).applicationLocales
        fontBefore = shell("settings get system font_scale").trim()
        database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
        val runner = RoomDatabaseTransactionRunner(database)
        repository =
            ProjectYarnUsageRepository(
                database.projectYarnUsageDao(),
                database.projectYarnNoteDao(),
                database.yarnCardDao(),
                runner,
                Dispatchers.IO,
            )
        yarnRepository =
            YarnCardRepository(database.yarnCardDao(), database.counterProjectDao(), context, runner, Dispatchers.IO)
        noteRepository =
            ProjectYarnNoteRepository(
                database.projectYarnNoteDao(),
                yarnRepository,
                runner,
                database.projectYarnUsageDao(),
            )
        runBlocking(Dispatchers.IO) {
            imperialBefore = PreferencesManager(context).preferences.first().useImperial
            PreferencesManager(context).setUseImperial(false)
            database.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Usage project", updatedAt = 10))
            database.yarnCardDao().upsert(
                YarnCardEntity(id = 3, yarnName = "Stash wool", linkedProjectId = 1, quantityInStash = 9),
            )
            database.projectYarnNoteDao().upsert(ProjectYarnNoteEntity(id = 4, projectId = 1, name = yarnName))
        }
    }

    @After
    fun tearDown() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent { }
            stores.forEach { it.clear() }
        }
        context.getSystemService(LocaleManager::class.java).applicationLocales = localesBefore
        shell("settings put system font_scale $fontBefore")
        runBlocking { PreferencesManager(context).setUseImperial(imperialBefore) }
        database.close()
    }

    @Test
    fun noteTrackingPersistsMetersThenReturnsFocusToItsOnlySummary() {
        render()
        capture("management-empty")
        openNote()
        composeRule.onNodeWithTag("yarn_usage_heading").assertIsFocused()
        composeRule.onNodeWithTag("yarn_usage_save").performScrollTo().assertIsNotEnabled()
        edit(YarnUsageField.PLANNED, "1200")
        edit(YarnUsageField.ALLOCATED, "1000")
        edit(YarnUsageField.USED, "650")
        remaining("Remaining: 350 m")
        capture("editor-meters-remaining")
        save()
        val usage = currentUsage()
        assertEquals(YarnUsageAmounts(1200.0, 1000.0, 650.0), usage.amounts)
        assertReturnFocus("Edit usage for $yarnName")
        composeRule.onAllNodesWithText("Used: 650 m").assertCountEquals(1)
        capture("management-usage")
        assertEquals(9, runBlocking { database.yarnCardDao().getCard(3)?.quantityInStash })
        assertEquals(10L, runBlocking { database.counterProjectDao().getProject(1)?.updatedAt })
    }

    @Test
    fun cardTrackingAndYardSwitchConvertValuesAndCancelDoesNotPersist() {
        render()
        composeRule.onNodeWithContentDescription("Track usage for Stash wool").performScrollTo().performClick()
        edit(YarnUsageField.ALLOCATED, "0.9144")
        chooseUnit(1)
        field(
            YarnUsageField.ALLOCATED,
        ).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                androidx.compose.ui.text
                    .AnnotatedString("1"),
            ),
        )
        edit(YarnUsageField.USED, "1")
        remaining("Remaining: 0 yd")
        save()
        assertEquals(0.9144, currentUsage().amounts.allocatedMeters)
        composeRule.onNodeWithContentDescription("Edit usage for Stash wool").performScrollTo().performClick()
        edit(YarnUsageField.USED, "999")
        composeRule.onNodeWithTag("yarn_usage_cancel").performScrollTo().performClick()
        waitForManagement()
        assertReturnFocus("Edit usage for Stash wool")
        assertEquals(0.9144, currentUsage().amounts.usedMeters)
    }

    @Test
    fun explicitConversionHandlesGramsFractionalSkeinsAndOverage() {
        render()
        openNote()
        chooseUnit(2)
        composeRule.onNodeWithTag("yarn_usage_save").performScrollTo().assertIsNotEnabled()
        capture("missing-conversion")
        edit(YarnUsageField.LENGTH, "200")
        edit(YarnUsageField.WEIGHT, "0")
        composeRule.onNodeWithTag("yarn_usage_save").performScrollTo().assertIsNotEnabled()
        edit(YarnUsageField.WEIGHT, "100")
        edit(YarnUsageField.ALLOCATED, "300")
        edit(YarnUsageField.USED, "175")
        remaining("Remaining: 125 g")
        capture("editor-grams")
        composeRule.onNodeWithText("Remaining: 250 m").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Remaining: 1.25 skeins").performScrollTo().assertIsDisplayed()
        chooseUnit(3)
        remaining("Remaining: 1.25 skeins")
        capture("editor-skeins")
        edit(YarnUsageField.USED, "4.25")
        remaining("Over by: 1.25 skeins")
        capture("overage")
        save()
        assertEquals(850.0, currentUsage().amounts.usedMeters)
        assertEquals(600.0, currentUsage().amounts.allocatedMeters)
    }

    @Test
    fun invalidRawValuesRemainVisibleAndZeroCanBeSavedWithUnknownRemaining() {
        render()
        openNote()
        listOf("-2", "1e3", "1.2.3").forEach { invalid ->
            edit(YarnUsageField.PLANNED, invalid)
            field(YarnUsageField.PLANNED).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            composeRule.onNodeWithTag("yarn_usage_save").performScrollTo().assertIsNotEnabled()
        }
        edit(YarnUsageField.PLANNED, "0")
        remaining("Remaining unknown")
        composeRule.onNodeWithTag("yarn_usage_save").performScrollTo().assertIsEnabled()
        save()
        assertEquals(YarnUsageAmounts(plannedMeters = 0.0), currentUsage().amounts)
    }

    @Test
    fun saveToMyYarnRetainsOneLogicalSummaryAndExistingSourceActions() {
        render()
        openNote()
        edit(YarnUsageField.USED, "350")
        save()
        val id = currentUsage().id
        composeRule.onNodeWithText("Save to My Yarn").performScrollTo().performClick()
        composeRule.waitUntil(
            10_000,
        ) { runBlocking { database.projectYarnNoteDao().getById(4)?.savedYarnCardId != null } }
        composeRule.onAllNodesWithText("Used: 350 m").assertCountEquals(1)
        composeRule.onAllNodesWithText("Edit usage").assertCountEquals(1)
        composeRule.onNodeWithText("Saved to My Yarn").performScrollTo().assertIsDisplayed()
        assertEquals(id, currentUsage().id)
    }

    @Test
    fun orphanUsageRemainsEditableAndDeletionOnlyRemovesTracking() {
        runBlocking {
            repository.create(1, YarnUsageSource(projectYarnNoteId = 4), YarnUsageAmounts(usedMeters = 5.0), yarnName)
        }
        runBlocking { database.projectYarnNoteDao().delete(4) }
        render()
        composeRule.onNodeWithText("Yarn source unavailable").performScrollTo().assertIsDisplayed()
        capture("orphan")
        composeRule.onNodeWithContentDescription("Edit usage for $yarnName").performScrollTo().performClick()
        edit(YarnUsageField.USED, "8")
        save()
        assertEquals(8.0, currentUsage().amounts.usedMeters)
        composeRule.onNodeWithContentDescription("Delete usage for $yarnName").performScrollTo().performClick()
        composeRule.onNodeWithText(context.getString(R.string.yarn_usage_delete_body, yarnName)).assertIsDisplayed()
        capture("delete-confirmation")
        composeRule.onNodeWithTag("yarn_usage_confirm_delete").assertHeightIsAtLeast(48.dp).performClick()
        waitForManagement()
        composeRule.waitUntil(5_000) {
            composeRule
                .onNodeWithTag(
                    "yarn_management_heading",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.Focused]
        }
        composeRule.onNodeWithTag("yarn_management_heading").assertIsFocused()
        assertTrue(runBlocking { requireNotNull(repository.observeForProject(1).first()).all { it.usage == null } })
        assertEquals(9, runBlocking { database.yarnCardDao().getCard(3)?.quantityInStash })
    }

    @Test
    fun recreationRestoresTheEditorRawInputAndCanonicalValues() {
        val model = render(handle = activityHandle())
        openNote()
        edit(YarnUsageField.ALLOCATED, "123.123456789")
        chooseUnit(1)
        val before = model.editor.value.draft
        composeRule.activityRule.scenario.recreate()
        val restored = createModel(activityHandle())
        composeRule.runOnUiThread { composeRule.activity.setContent { TestSurface(restored, Locale.US, false, false) } }
        composeRule.waitUntil(10_000) { composeRule.onNodeWithTag("yarn_usage_heading").isDisplayed() }
        assertEquals(before, restored.editor.value.draft)
        edit(YarnUsageField.USED, "0")
        save()
        assertEquals(123.123456789, currentUsage().amounts.allocatedMeters)
    }

    @Test
    fun committedSaveDoesNotReopenAfterActivityRecreation() {
        val model = render()
        openNote()
        edit(YarnUsageField.USED, "350")
        field(YarnUsageField.USED).performImeAction()
        composeRule.onNodeWithTag("yarn_usage_save").performScrollTo().assertIsEnabled()
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag("yarn_usage_save").performClick()
            composeRule.waitUntil(10_000) { model.editor.value.completed || model.editor.value.draft == null }
            composeRule.activityRule.scenario.recreate()
            composeRule.runOnUiThread {
                composeRule.activity.setContent { TestSurface(model, Locale.US, false, false) }
            }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
        waitForManagement()
        assertNull(model.editor.value.draft)
        assertEquals(350.0, currentUsage().amounts.usedMeters)
        composeRule.onAllNodesWithText("Used: 350 m").assertCountEquals(1)
    }

    @Test
    fun largeFontGermanLightThemeWrapsLongNamesAndExposesUnitsAndTouchTargets() = verifyLargeLayout(false)

    @Test
    fun largeFontGermanDarkThemeWrapsLongNamesAndExposesUnitsAndTouchTargets() = verifyLargeLayout(true)

    @Test
    fun keyboardVisibleEditorCanReachFinalFieldsAndSave() {
        render()
        openNote()
        chooseUnit(2)
        edit(YarnUsageField.LENGTH, "200")
        edit(YarnUsageField.WEIGHT, "100")
        edit(YarnUsageField.USED, "175")
        composeRule.waitUntil(5_000) {
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand("dumpsys input_method")
                .use { descriptor ->
                    android.os.ParcelFileDescriptor
                        .AutoCloseInputStream(
                            descriptor,
                        ).bufferedReader()
                        .readText()
                        .contains("mInputShown=true")
                }
        }
        composeRule
            .onNodeWithTag("yarn_usage_save")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        capture("ime-visible-save")
        save()
    }

    private fun verifyLargeLayout(dark: Boolean) {
        yarnName = "Sehr langes handgefärbtes Merinogarn für die Ärmel und die aufwendige mehrfarbige Passe"
        runBlocking {
            database.projectYarnNoteDao().upsert(
                ProjectYarnNoteEntity(id = 4, projectId = 1, name = yarnName),
            )
        }
        render(locale = Locale.GERMAN, dark = dark, large = true)
        capture(if (dark) "large-management-dark" else "large-management-light")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Verbrauch erfassen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Verbrauch für $yarnName erfassen").performScrollTo().performClick()
        edit(YarnUsageField.ALLOCATED, "600")
        edit(YarnUsageField.USED, "350")
        field(YarnUsageField.USED).performImeAction()
        composeRule
            .onNodeWithTag("yarn_usage_unit")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitUntil(5_000) { composeRule.onNodeWithTag("yarn_usage_unit_option_0").isDisplayed() }
        composeRule
            .onNodeWithTag(
                "yarn_usage_unit_option_0",
            ).assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText(yarnName).performScrollTo().assertIsDisplayed()
        assertNoClippedEditorText()
        capture(if (dark) "large-german-dark" else "large-german-light")
        composeRule
            .onNodeWithTag("yarn_usage_save")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        capture(if (dark) "large-german-dark-actions" else "large-german-light-actions")
        composeRule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
                useUnmergedTree = true,
            ).assertCountEquals(0)
    }

    private fun assertNoClippedEditorText() {
        val nodes =
            composeRule.onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            )
        nodes.fetchSemanticsNodes().forEachIndexed { index, node ->
            val layouts = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.forEach { layout ->
                assertEquals(2f, layout.layoutInput.density.fontScale)
                val width = minOf(layout.size.width, node.size.width).toFloat()
                repeat(layout.lineCount) { line ->
                    assertFalse(layout.layoutInput.text.text, layout.isLineEllipsized(line))
                    assertTrue(layout.layoutInput.text.text, layout.getLineRight(line) <= width + 1f)
                }
            }
        }
    }

    private fun render(
        locale: Locale = Locale.US,
        dark: Boolean = false,
        large: Boolean = false,
        handle: SavedStateHandle = SavedStateHandle(),
    ): ProjectYarnUsageViewModel {
        if (composeRule.activity.resources.configuration.locales[0]
                .language != locale.language
        ) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList(locale)
        }
        shell("settings put system font_scale ${if (large) "2.0" else "1.0"}")
        composeRule.waitUntil(10_000) {
            val config = composeRule.activity.resources.configuration
            config.locales[0].language == locale.language && config.fontScale == if (large) 2f else 1f
        }
        val model = createModel(handle)
        composeRule.setContent { TestSurface(model, locale, dark, large) }
        waitForManagement()
        return model
    }

    @Composable
    private fun TestSurface(
        model: ProjectYarnUsageViewModel,
        locale: Locale,
        dark: Boolean,
        large: Boolean,
    ) {
        val localized =
            context.createConfigurationContext(
                Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                },
            )
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalContext provides localized,
            LocalConfiguration provides localized.resources.configuration,
            LocalDensity provides Density(density.density, if (large) 2f else 1f),
        ) {
            KnitToolsTheme(isDarkTheme = dark) {
                val notes by noteRepository.observeForProject(1).collectAsState(initial = emptyList())
                val cards by yarnRepository.getAllCards().collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()
                ProjectYarnUsageFlow(
                    1,
                    cards.filter { it.linkedProjectId == 1L }.map { it.id to it.yarnName },
                    notes,
                    ProStatus.TRIAL_EXPIRED,
                    YarnManagementSheetActions(
                        onUnlinkYarn = { scope.launch { yarnRepository.updateLinkedProjectId(it, null) } },
                        onAddYarn = {},
                        onSaveProjectYarnNote = { _, _, _, _ -> },
                        onDeleteProjectYarnNote = { scope.launch { noteRepository.delete(it) } },
                        onSaveProjectYarnNoteToMyYarn = { scope.launch { noteRepository.saveToMyYarn(it) } },
                        onDismiss = {},
                    ),
                    viewModelProvider = { model },
                )
            }
        }
    }

    private fun createModel(handle: SavedStateHandle): ProjectYarnUsageViewModel {
        lateinit var model: ProjectYarnUsageViewModel
        composeRule.runOnUiThread {
            model = ProjectYarnUsageViewModel(repository, PreferencesManager(context), handle)
            stores.add(ViewModelStore().apply { put("usage", model) })
        }
        return model
    }

    private fun activityHandle(): SavedStateHandle {
        lateinit var handle: SavedStateHandle
        composeRule.runOnUiThread {
            handle =
                MutableCreationExtras()
                    .apply {
                        set(SAVED_STATE_REGISTRY_OWNER_KEY, composeRule.activity)
                        set(VIEW_MODEL_STORE_OWNER_KEY, composeRule.activity)
                        set(VIEW_MODEL_KEY, "yarn-usage-restoration")
                    }.createSavedStateHandle()
        }
        return handle
    }

    private fun field(field: YarnUsageField) = composeRule.onNodeWithTag("yarn_usage_input_${field.name}")

    private fun edit(
        field: YarnUsageField,
        value: String,
    ) {
        this
            .field(field)
            .performScrollTo()
            .performClick()
            .performTextReplacement(value)
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(250, 5_000)
    }

    private fun openNote() {
        composeRule.onNodeWithContentDescription("Track usage for $yarnName").performScrollTo().performClick()
    }

    private fun chooseUnit(index: Int) {
        composeRule.onNodeWithTag("yarn_usage_unit").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onNodeWithTag("yarn_usage_unit_option_$index").isDisplayed() }
        composeRule.onNodeWithTag("yarn_usage_unit_option_$index").performScrollTo().performClick()
    }

    private fun remaining(text: String) {
        field(YarnUsageField.USED).performImeAction()
        composeRule.waitUntil(5_000) { !shell("dumpsys input_method").contains("mInputShown=true") }
        composeRule.waitUntil(5_000) {
            composeRule.onNodeWithTag("yarn_usage_remaining").performScrollTo()
            composeRule.onNodeWithText(text).isDisplayed()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun save() {
        composeRule.onNodeWithTag("yarn_usage_save").performScrollTo().performClick()
        waitForManagement()
    }

    private fun waitForManagement() {
        composeRule.waitUntil(10_000) { composeRule.onNodeWithTag("yarn_management_heading").isDisplayed() }
    }

    private fun assertReturnFocus(description: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(isFocused()).fetchSemanticsNodes().any {
                it.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.contains(description)
            }
        }
        composeRule.onNodeWithContentDescription(description).assertIsFocused()
    }

    private fun currentUsage() =
        runBlocking {
            requireNotNull(repository.observeForProject(1).first()).mapNotNull { it.usage }.single()
        }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        SystemClock.sleep(350)
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = File(context.getExternalFilesDir(null), "yarn-usage-qa").apply { mkdirs() }
        File(directory, "api-${Build.VERSION.SDK_INT}-$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()
        }
}
