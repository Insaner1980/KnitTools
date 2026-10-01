package com.finnvek.knittools.ui.screens.notes

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.repository.counterHistoryTestRepository
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
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

class NotesEditorFailureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: KnitToolsDatabase
    private lateinit var viewModel: NotesEditorViewModel
    private val handle = SavedStateHandle(mapOf("projectId" to 1L))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = ViewModelStore()
    private val visible = mutableStateOf(true)

    @Volatile private var failWrites = true
    private var exits = 0

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
        val dao = database.counterProjectDao()
        runBlocking {
            dao.insert(
                CounterProjectEntity(id = 1L, name = "Notes test", notes = "Base", notesCreated = true),
            )
        }
        val failingDao =
            object : CounterProjectDao by dao {
                override suspend fun updateNotes(
                    id: Long,
                    notes: String,
                    updatedAt: Long,
                ) {
                    if (failWrites) throw IOException("Controlled note write failure")
                    dao.updateNotes(id, notes, updatedAt)
                }
            }
        val repository = counterHistoryTestRepository(database, context, failingDao)
        val proManager =
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java,
                ).proManager()
        composeRule.runOnUiThread {
            viewModel = NotesEditorViewModel(repository, proManager, scope, handle)
            store.put("notes", viewModel)
            composeRule.activity.setContent(content = content)
        }
        composeRule.waitUntil(5_000) { viewModel.uiState.value.isLoaded }
    }

    @After
    fun cleanup() {
        composeRule.runOnUiThread { store.clear() }
        scope.cancel()
        database.close()
    }

    private val content: @Composable () -> Unit = {
        KnitToolsTheme {
            if (visible.value) {
                NotesEditorScreen(
                    onBack = {
                        exits++
                        visible.value = false
                        store.clear()
                    },
                    onSeePro = {},
                    viewModelProvider = { viewModel },
                )
            }
        }
    }

    @Test fun toolbarBackFailureCanRetryAndPreservesConcurrentNotes() = retryAfterFailure(systemBack = false)

    @Test fun systemBackFailureCanRetryAndPreservesConcurrentNotes() = retryAfterFailure(systemBack = true)

    private fun retryAfterFailure(systemBack: Boolean) {
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Local edit")
        back(systemBack)
        composeRule.waitUntil(5_000) { viewModel.uiState.value.saveFailed }
        composeRule.onNodeWithText(context.getString(R.string.notes_save_failed)).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, exits)
            assertEquals("Local edit", handle.get<String>("notesDraft"))
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        composeRule.onNode(hasSetTextAction()).assertTextContains("Local edit")
        back(systemBack)
        composeRule.onNodeWithText(context.getString(R.string.notes_discard_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
        runBlocking { database.counterProjectDao().updateNotes(1L, "Base\nExternal addition", 1L) }
        failWrites = false
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        composeRule.waitUntil(5_000) { !viewModel.uiState.value.saveFailed }
        composeRule.onNode(hasSetTextAction()).assertTextContains("Local edit\nExternal addition")
        back(systemBack)
        composeRule.runOnIdle {
            assertEquals(1, exits)
            assertNull(handle.get<String>("notesDraft"))
        }
        assertEquals(
            "Local edit\nExternal addition",
            runBlocking { database.counterProjectDao().getProject(1L)?.notes },
        )
    }

    @Test
    fun explicitDiscardAfterAutosaveFailureLeavesPersistedNotesIntact() {
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Discarded edit")
        composeRule.waitUntil(5_000) { viewModel.uiState.value.saveFailed }
        composeRule.onNodeWithText(context.getString(R.string.notes_save_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.notes_discard_action)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
        composeRule.onNode(hasSetTextAction()).assertTextContains("Discarded edit")
        back(systemBack = true)
        composeRule.onAllNodesWithText(context.getString(R.string.notes_discard_action)).onLast().performClick()
        composeRule.runOnIdle {
            assertEquals(1, exits)
            assertNull(handle.get<String>("notesDraft"))
        }
        failWrites = false
        assertEquals("Base", runBlocking { database.counterProjectDao().getProject(1L)?.notes })
    }

    private fun back(systemBack: Boolean) {
        if (systemBack) {
            composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        } else {
            composeRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        }
    }
}
