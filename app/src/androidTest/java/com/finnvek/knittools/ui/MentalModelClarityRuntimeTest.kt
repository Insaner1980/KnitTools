package com.finnvek.knittools.ui

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.remote.PatternSearchParams
import com.finnvek.knittools.data.remote.PatternSearchResponse
import com.finnvek.knittools.data.remote.RavelryApiService
import com.finnvek.knittools.data.remote.RavelryBackendAuthStatus
import com.finnvek.knittools.data.remote.RavelryBackendClient
import com.finnvek.knittools.data.remote.RavelryBackendCurrentUser
import com.finnvek.knittools.data.remote.RavelryStartAuthResponse
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.RavelryRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.ui.screens.counter.YarnManagementSheet
import com.finnvek.knittools.ui.screens.counter.YarnManagementSheetActions
import com.finnvek.knittools.ui.screens.library.SavedPatternDetailScreen
import com.finnvek.knittools.ui.screens.library.SavedPatternsActions
import com.finnvek.knittools.ui.screens.library.SavedPatternsScreen
import com.finnvek.knittools.ui.screens.library.SavedPatternsState
import com.finnvek.knittools.ui.screens.ravelry.RavelryDetailScreen
import com.finnvek.knittools.ui.screens.ravelry.RavelryViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class MentalModelClarityRuntimeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val arguments = InstrumentationRegistry.getArguments()
    private val language = arguments.getString("clarityLocale", "en")
    private val dark = arguments.getString("clarityDark", "false").toBoolean()
    private val context get() = rule.activity
    private val store = ViewModelStore()
    private var database: KnitToolsDatabase? = null
    private val title = "Cable cardigan with a long descriptive pattern title for winter evenings"

    @After
    fun closeFixtures() {
        rule.runOnUiThread { store.clear() }
        database?.close()
    }

    private fun render(content: @Composable () -> Unit) {
        rule.setContent {
            KnitToolsTheme(isDarkTheme = dark, content = content)
        }
    }

    private fun text(id: Int) = context.getString(id)

    private fun pattern(source: SavedPatternSource = SavedPatternSource.Ravelry) =
        SavedPattern(
            id = 7,
            source = source,
            name = title,
            designerName = "Synthetic designer",
            originalUrl = if (source == SavedPatternSource.WebLink) "https://example.invalid/pattern" else "",
            canonicalUrl =
                if (source ==
                    SavedPatternSource.Ravelry
                ) {
                    "https://www.ravelry.com/patterns/library/test"
                } else {
                    ""
                },
        )

    @Test fun singleDeletion() = deletion(SavedPatternSource.Ravelry)

    @Test fun webDeletion() = deletion(SavedPatternSource.WebLink)

    private fun deletion(source: SavedPatternSource) {
        var deleted = 0
        render {
            SavedPatternDetailScreen(
                pattern = pattern(source),
                onBack = {},
                onOpenPattern = {},
                onAttachToProject = {},
                onRemove = { deleted++ },
            )
        }
        val action =
            text(
                if (source ==
                    SavedPatternSource.WebLink
                ) {
                    R.string.web_pattern_delete
                } else {
                    R.string.remove_pattern
                },
            )
        val message =
            if (source == SavedPatternSource.WebLink) {
                context.getString(R.string.web_pattern_delete_confirm_message, title)
            } else {
                text(R.string.saved_pattern_detail_remove_confirm)
            }
        rule.onNodeWithText(action).performScrollTo().performClick()
        capture("delete-${source.name}")
        assertReadable(message)
        rule.onNodeWithText(message).performTouchInput { swipeUp() }
        capture("delete-${source.name}-scrolled")
        rule.onNodeWithText(text(R.string.cancel)).assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(0, deleted) }
        rule.onNodeWithText(action).performClick()
        rule
            .onAllNodesWithText(action)
            .onLast()
            .assertIsDisplayed()
            .performClick()
        rule.runOnIdle { assertEquals(1, deleted) }
    }

    @Test fun bulkDeletion() {
        var deleted = 0
        val patterns = listOf(pattern(), pattern(SavedPatternSource.WebLink).copy(id = 8))
        render {
            SavedPatternsScreen(
                SavedPatternsState(patterns, true, setOf(7, 8), 0),
                SavedPatternsActions({}, {}, {}, {}, {}, { deleted++ }, {}, {}),
            )
        }
        rule.onNodeWithText(text(R.string.delete)).performClick()
        capture("delete-bulk")
        assertReadable(context.resources.getQuantityString(R.plurals.delete_patterns_confirm, 2, 2))
        rule
            .onNodeWithText(context.resources.getQuantityString(R.plurals.delete_patterns_confirm, 2, 2))
            .performTouchInput { swipeUp() }
        capture("delete-bulk-scrolled")
        rule.onNodeWithText(text(R.string.cancel)).assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(0, deleted) }
        rule.onNodeWithText(text(R.string.delete)).performClick()
        rule
            .onAllNodesWithText(text(R.string.delete))
            .onLast()
            .assertIsDisplayed()
            .performClick()
        rule.runOnIdle { assertEquals(1, deleted) }
    }

    @Test fun savedPdfStates() {
        val current = mutableStateOf(pattern())
        render {
            SavedPatternDetailScreen(current.value, {}, {}, {}, {})
        }
        val explanation = text(R.string.saved_pattern_detail_no_pdf_explanation)
        rule.onNodeWithText(explanation).performScrollTo()
        capture("ravelry-no-pdf")
        assertReadable(explanation)
        rule
            .onNode(hasText(text(R.string.open_in_ravelry)) and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        rule.runOnIdle { current.value = pattern().copy(localPdfUri = "content://synthetic/unreadable.pdf") }
        rule.onAllNodesWithText(explanation).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.saved_pattern_detail_open_pattern)).performScrollTo().assertIsEnabled()
        capture("ravelry-attached-pdf")
    }

    @Test fun projectYarnEntries() {
        val notes =
            mutableStateOf(
                listOf(
                    ProjectYarnNote(
                        1,
                        1,
                        "Long named wool and alpaca yarn for the winter cardigan",
                        notes = "Private project notes",
                    ),
                    ProjectYarnNote(2, 1, "Previously saved contrasting yarn", savedYarnCardId = 20),
                    ProjectYarnNote(3, 1, "Third project yarn"),
                ),
            )
        var saved = 0L
        var usageOpened = false
        render {
            YarnManagementSheet(
                linkedYarns = emptyList(),
                projectYarnNotes = notes.value,
                proStatus = ProStatus.PRO_PURCHASED,
                actions = YarnManagementSheetActions({}, {}, { _, _, _, _ -> }, {}, { saved = it }, {}),
                usageItems =
                    notes.value.map {
                        ProjectYarnUsageItem(
                            YarnUsageSource(projectYarnNoteId = it.id),
                            it.name,
                        )
                    },
                onUsage = { usageOpened = true },
            )
        }
        rule.onNode(isDialog()).performTouchInput { swipeUp() }
        rule.onAllNodesWithText(text(R.string.save_to_my_yarn_explanation))[0].performScrollTo()
        capture("yarn-unsaved")
        rule
            .onAllNodesWithText(text(R.string.save_to_my_yarn))[0]
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        rule.runOnIdle { assertEquals(1L, saved) }
        rule.onNodeWithText(text(R.string.saved_to_my_yarn_explanation)).performScrollTo()
        capture("yarn-saved")
        assertReadable(text(R.string.saved_to_my_yarn_explanation))
        val usage = context.getString(R.string.yarn_usage_track_named, notes.value[1].name)
        rule
            .onNodeWithContentDescription(usage)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        rule.runOnIdle { assertTrue(usageOpened) }
        rule.onNodeWithText(notes.value.last().name).performScrollTo().assertIsDisplayed()
        capture("yarn-third-entry")
    }

    @Test fun unsavedRavelryDetail() {
        val db = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
        database = db
        val backend = SyntheticRavelryBackend()
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val savedRepository =
            SavedPatternRepository(
                db.savedPatternDao(),
                context,
                db.counterProjectDao(),
                RoomDatabaseTransactionRunner(db),
                Dispatchers.IO,
            )
        val vm =
            RavelryViewModel(
                RavelryRepository(RavelryApiService(backend), savedRepository, entry.counterRepository()),
                entry.proManager(),
                RavelryAuthManager(backend),
                SavedStateHandle(),
            )
        store.put("ravelry", vm)
        render { RavelryDetailScreen(42, {}, {}, viewModelProvider = { vm }) }
        rule.waitUntil(15_000) { vm.patternDetail.value != null && !vm.isDetailLoading.value }
        val explanation = text(R.string.ravelry_save_pattern_explanation)
        rule.onNodeWithText(explanation).performScrollTo()
        capture("ravelry-before-save")
        assertReadable(explanation)
        rule
            .onNodeWithText(text(R.string.save_pattern))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        rule.waitUntil(15_000) { vm.isPatternSaved.value }
        rule.onAllNodesWithText(explanation).assertCountEquals(0)
        capture("ravelry-after-save")
    }

    private fun assertReadable(value: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText(value, useUnmergedTree = true)
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse("Explanation text must not overflow its layout", layouts.any { it.hasVisualOverflow })
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        SystemClock.sleep(350)
        val directory = File(context.getExternalFilesDir(null), "clarity-qa/$language-$dark").apply { mkdirs() }
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(directory, "api-${Build.VERSION.SDK_INT}-$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }
}

private class SyntheticRavelryBackend : RavelryBackendClient {
    override suspend fun authStatus() = RavelryBackendAuthStatus(true, "Synthetic tester")

    override suspend fun importPatternById(ravelryPatternId: Int) =
        PatternDetail(id = ravelryPatternId, name = "Synthetic cable cardigan", permalink = "test")

    override suspend fun startAuth(): RavelryStartAuthResponse = error("External auth is forbidden in this fixture")

    override suspend fun disconnect() = Unit

    override suspend fun currentUser() = RavelryBackendCurrentUser(true)

    override suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse = error("Unused")

    override suspend fun importPatternByUrl(url: String): PatternDetail = error("Unused")
}
