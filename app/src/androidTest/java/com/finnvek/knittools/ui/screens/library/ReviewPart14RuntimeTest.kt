package com.finnvek.knittools.ui.screens.library

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.NotesDraftStorage
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.counterHistoryTestRepository
import com.finnvek.knittools.ui.navigation.Screen
import com.finnvek.knittools.ui.navigation.WebPatternEditorOrigin
import com.finnvek.knittools.ui.screens.notes.NotesEditorScreen
import com.finnvek.knittools.ui.screens.notes.NotesEditorViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.UUID

class ReviewPart14RuntimeTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: KnitToolsDatabase
    private val testModels = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Before
    fun setup() {
        check(context.packageName == "com.finnvek.knittools.activepolicytest")
        database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
    }

    @After
    fun cleanup() {
        rule.runOnUiThread { testModels.clear() }
        scope.cancel()
        database.close()
    }

    @Test
    fun committedWebPatternResultDoesNotNavigateAfterRouteReplacement() {
        val room = RoomDatabaseTransactionRunner(database)
        val committed = CompletableDeferred<Unit>()
        val deliverResult = CompletableDeferred<Unit>()
        val runner =
            object : DatabaseTransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T {
                    val result = room.run(block)
                    committed.complete(Unit)
                    deliverResult.await()
                    return result
                }
            }
        val patterns =
            SavedPatternRepository(
                database.savedPatternDao(),
                context,
                database.counterProjectDao(),
                runner,
                Dispatchers.IO,
            )
        val projects = counterHistoryTestRepository(database, context, database.counterProjectDao())
        lateinit var replaceRoute: () -> Unit
        lateinit var model: WebPatternEditorViewModel
        rule.runOnUiThread {
            model =
                WebPatternEditorViewModel(
                    patterns,
                    projects,
                    SavedStateHandle(
                        mapOf(
                            Screen.WebPatternEditor.ARG_ORIGIN to WebPatternEditorOrigin.Manual.persistedValue,
                        ),
                    ),
                )
            testModels.put("web", model)
            rule.activity.setContent {
                val navigation = rememberNavController()
                KnitToolsTheme {
                    NavHost(navigation, startDestination = "home") {
                        composable("home") { Text("Original route") }
                        composable("next") { Text("Replacement route") }
                        composable("detail") { Text("Saved pattern detail") }
                        dialog("editor") {
                            replaceRoute = { navigation.navigate("next") }
                            WebPatternEditorScreen(
                                request = null,
                                onRequestStored = {},
                                onBack = { navigation.popBackStack() },
                                onOpenDetail = { navigation.navigate("detail") },
                                onOpenProject = { error("Unexpected project navigation") },
                                onOpenRavelry = { error("Unexpected Ravelry navigation") },
                                viewModelProvider = { model },
                            )
                        }
                    }
                    LaunchedEffect(navigation) { navigation.navigate("editor") }
                }
            }
        }
        rule.onNodeWithText(context.getString(R.string.web_pattern_title_label)).assertIsDisplayed()
        rule.runOnIdle {
            model.updateTitle("Route timing fixture")
            model.updateUrl("https://example.com/route-timing")
            model.save()
        }
        rule.waitUntil(10_000) { committed.isCompleted }
        rule.runOnUiThread {
            replaceRoute()
            deliverResult.complete(Unit)
        }
        rule.waitUntil(10_000) { !model.uiState.value.isSaving }
        rule.onNodeWithText("Replacement route").assertIsDisplayed()
    }

    @Test
    fun largeUnsavedNotesSurviveAnActivityStopAndResume() {
        val dao = database.counterProjectDao()
        runBlocking {
            dao.insert(
                CounterProjectEntity(id = 1L, name = "Notes fixture", notes = "Base", notesCreated = true),
            )
        }
        val failingDao =
            object : CounterProjectDao by dao {
                override suspend fun updateNotes(
                    id: Long,
                    notes: String,
                    updatedAt: Long,
                ): Unit = throw IOException("Controlled write failure")
            }
        val repository = counterHistoryTestRepository(database, context, failingDao)
        val pro = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager()
        val draftStorage = NotesDraftStorage(context, Dispatchers.IO)
        lateinit var model: NotesEditorViewModel
        lateinit var handle: SavedStateHandle
        rule.runOnUiThread {
            rule.activity.setContent {
                KnitToolsTheme {
                    NavHost(rememberNavController(), startDestination = "notes/1") {
                        composable(
                            "notes/{projectId}",
                            arguments =
                                listOf(
                                    navArgument("projectId") {
                                        type =
                                            NavType.LongType
                                    },
                                ),
                        ) {
                            model =
                                viewModel {
                                    handle = createSavedStateHandle()
                                    NotesEditorViewModel(repository, pro, scope, handle, draftStorage)
                                }
                            NotesEditorScreen(onBack = {}, onSeePro = {}, viewModelProvider = { model })
                        }
                    }
                }
            }
        }
        val draft = "0123456789".repeat(28_000)
        rule.onNode(hasSetTextAction()).performTextReplacement(draft)
        rule.runOnIdle {
            assertEquals(draft.length, model.uiState.value.notes.length)
            model.saveImmediately()
        }
        rule.waitUntil(15_000) { model.uiState.value.saveFailed }
        val scenario = rule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        rule.runOnIdle { assertEquals(draft, model.uiState.value.notes) }
        val token =
            rule.runOnIdle {
                assertNull(handle.get<String>("notesDraft"))
                assertNull(handle.get<String>("notesDraftBase"))
                checkNotNull(handle.get<String>("notesDraftFileId"))
            }
        val stored = runBlocking { draftStorage.read(1L, UUID.fromString(token)) }
        assertEquals(draft, stored?.notes)
        assertEquals("Base", stored?.baseNotes)
        val restored =
            rule.runOnIdle {
                val restoredHandle = SavedStateHandle(mapOf("projectId" to 1L, "notesDraftFileId" to token))
                NotesEditorViewModel(
                    repository,
                    pro,
                    scope,
                    restoredHandle,
                    draftStorage,
                ).also { testModels.put("restored", it) }
            }
        rule.waitUntil(10_000) { restored.uiState.value.isLoaded }
        assertEquals(draft, restored.uiState.value.notes)
    }
}
