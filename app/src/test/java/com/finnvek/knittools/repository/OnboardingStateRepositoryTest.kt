package com.finnvek.knittools.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.finnvek.knittools.domain.model.OnboardingCraft
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingNeed
import com.finnvek.knittools.domain.model.OnboardingStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class OnboardingStateRepositoryTest {
    @Test
    fun `legacy benefits draft retains choices and project form when resuming`() =
        runBlocking {
            val json =
                """{"id":"legacy","step":"Benefits","needs":["Counting","Yarn"],"craft":"Crochet",
                    |"demoCount":2,"demoHistory":[0,1],"name":"Benefits","initialCount":"12",
                    |"targetRows":"72","showOptions":true,"pdfUri":"file:///pattern.pdf","pdfName":"Benefits.pdf"}
                """.trimMargin()
            val dataStore =
                object : DataStore<Preferences> {
                    override val data = flowOf(preferencesOf(stringPreferencesKey("journey_draft_v1") to json))

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                        transform(data.first())
                }
            val repository = OnboardingStateRepository(dataStore)
            val expected =
                OnboardingDraft(
                    id = "legacy",
                    step = OnboardingStep.Project,
                    needs = listOf(OnboardingNeed.Counting, OnboardingNeed.Yarn),
                    craft = OnboardingCraft.Crochet,
                    demoCount = 2,
                    demoHistory = listOf(0, 1),
                    name = "Benefits",
                    initialCount = "12",
                    targetRows = "72",
                    showOptions = true,
                    pdfUri = "file:///pattern.pdf",
                    pdfName = "Benefits.pdf",
                )

            assertEquals(expected, repository.draft.first())
        }

    @Test
    fun `unknown draft fields remain readable and invalid drafts are absent`() =
        runBlocking {
            val key = stringPreferencesKey("journey_draft_v1")
            val values =
                listOf(
                    """{"id":"saved","futureField":true}""" to "saved",
                    """{"id":"saved","step":"RemovedStep"}""" to null,
                    "{broken" to null,
                    "{}" to null,
                )
            values.forEach { (json, expectedId) ->
                val dataStore =
                    object : DataStore<Preferences> {
                        override val data = flowOf(preferencesOf(key to json))

                        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                            transform(data.first())
                    }
                assertEquals(expectedId, OnboardingStateRepository(dataStore).draft.first()?.id)
            }
        }

    @Test
    fun `journey completion stores the draft destination together`() =
        runBlocking {
            val repository = repository()
            val draft =
                com.finnvek.knittools.domain.model.OnboardingDraft(
                    "stable",
                    name = "My socks",
                    projectId = 7L,
                    pdfUri = "file:///example.pdf",
                    completed = true,
                    exitPending = true,
                )
            assertTrue(repository.saveDraft(draft))
            assertEquals(draft, repository.draft.first())
            val state = repository.state.first()
            assertTrue(state.welcomeDismissed)
            assertEquals(7L, state.starterProjectId)
        }

    @Test
    fun `failed draft persistence is reported and draft reads do not hide IO failure`() =
        runBlocking {
            val repository = OnboardingStateRepository(failingStore(IOException("unavailable")))
            assertFalse(
                repository.saveDraft(
                    com.finnvek.knittools.domain.model
                        .OnboardingDraft("stable"),
                ),
            )
            assertTrue(runCatching { repository.draft.first() }.exceptionOrNull() is IOException)
        }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val jobs = mutableListOf<Job>()

    @After
    fun closeStores() =
        runBlocking {
            jobs.forEach { it.cancelAndJoin() }
        }

    @Test
    fun `empty store exposes safe defaults`() =
        runBlocking {
            assertEquals(OnboardingState(), repository().state.first())
        }

    @Test
    fun `first read removes legacy preferences and preserves first use and backup`() =
        runBlocking {
            val store = store(File(temporaryFolder.root, "legacy.preferences_pb"))
            val legacy =
                preferencesOf(
                    stringSetPreferencesKey("tips_seen") to setOf("undo"),
                    intPreferencesKey("counter_open_count") to 5,
                    intPreferencesKey("counter_open_days") to 2,
                    longPreferencesKey("counter_open_last_day") to 43L,
                    booleanPreferencesKey("first_session_tip_pending") to true,
                    booleanPreferencesKey("first_save_observed") to true,
                    stringSetPreferencesKey("target_celebrated_projects") to setOf("7"),
                    stringPreferencesKey("target_celebrated_project_order") to "7",
                    stringSetPreferencesKey("project_increment_counts") to setOf("7:10"),
                    longPreferencesKey("guided_pattern_project_id") to 7L,
                )
            val retained =
                preferencesOf(
                    intPreferencesKey("initialized_version") to 1,
                    longPreferencesKey("first_use_epoch_day") to 42L,
                    booleanPreferencesKey("backup_created") to true,
                )
            store.updateData {
                legacy.toMutablePreferences().apply {
                    this[intPreferencesKey("initialized_version")] = 1
                    this[longPreferencesKey("first_use_epoch_day")] = 42L
                    this[booleanPreferencesKey("backup_created")] = true
                }
            }
            val state = OnboardingStateRepository(store).state.first()

            assertEquals(42L, state.firstUseEpochDay)
            assertTrue(state.backupCreated)
            assertEquals(retained, store.data.first())
        }

    @Test
    fun `corrupt store is replaced with empty preferences`() =
        runBlocking {
            val file = File(temporaryFolder.root, "corrupt.preferences_pb")
            file.writeBytes(byteArrayOf(-1, -1, -1))
            val repository = repository(file)

            assertEquals(OnboardingState(), repository.state.first())
            repository.initializeIfNeeded(today = 42L)
            assertEquals(ONBOARDING_STATE_VERSION, repository.state.first().initializedVersion)
        }

    @Test
    fun `first launch without projects initialization is idempotent`() =
        runBlocking {
            val repository = repository()
            repository.initializeIfNeeded(today = 42L)
            repository.initializeIfNeeded(today = 43L)

            val state = repository.state.first()
            assertEquals(ONBOARDING_STATE_VERSION, state.initializedVersion)
            assertEquals(42L, state.firstUseEpochDay)
            assertFalse(state.welcomeDismissed)
        }

    @Test
    fun `starter is assigned once and saved flags survive reopening`() =
        runBlocking {
            val file = File(temporaryFolder.root, "restart.preferences_pb")
            val repository = repository(file)
            repository.setStarterProject(7L)
            repository.setStarterProject(9L)
            repository.dismissWelcome()
            repository.dismissBackupNotice()
            repository.dismissReadingLineNotice()
            val saved = repository.state.first()
            jobs.last().cancelAndJoin()

            assertEquals(saved, repository(file).state.first())
            assertEquals(7L, saved.starterProjectId)
            assertTrue(saved.backupNoticeDismissed)
            assertTrue(saved.readingLineNoticeDismissed)
        }

    @Test
    fun `setup hide for a different project leaves the starter visible`() =
        runBlocking {
            val repository = repository()
            repository.setStarterProject(7L)
            repository.hideProjectSetup(8L)
            assertFalse(repository.state.first().projectSetupHidden)
            assertTrue(repository.hideProjectSetup(7L))
            assertTrue(repository.state.first().projectSetupHidden)
        }

    @Test
    fun `trial progress markers reject missing starts and preserve the newest trial across reopening`() =
        runBlocking {
            val file = File(temporaryFolder.root, "trial-progress.preferences_pb")
            val repository = repository(file)
            repository.markTrialChartSeen(0L)
            repository.markTrialPhotoPdfCreated(-1L)
            assertEquals(0L, repository.state.first().trialChartSeenForStart)
            assertEquals(0L, repository.state.first().trialPhotoPdfCreatedForStart)
            repository.markTrialChartSeen(100L)
            repository.markTrialPhotoPdfCreated(100L)
            repository.markTrialChartSeen(200L)
            repository.markTrialPhotoPdfCreated(200L)
            repository.markTrialChartSeen(100L)
            repository.markTrialPhotoPdfCreated(100L)
            jobs.last().cancelAndJoin()

            val restored = repository(file).state.first()
            assertEquals(200L, restored.trialChartSeenForStart)
            assertEquals(200L, restored.trialPhotoPdfCreatedForStart)
        }

    @Test
    fun `read and write IO failures preserve safe defaults`() =
        runBlocking {
            val repository = OnboardingStateRepository(failingStore(IOException("unavailable")))
            assertEquals(OnboardingState(), repository.state.first())
            repository.initializeIfNeeded(today = 42L)
            repository.dismissWelcome()
            repository.markTrialChartSeen(100L)
            repository.markTrialPhotoPdfCreated(100L)
            assertEquals(OnboardingState(), repository.state.first())
        }

    @Test
    fun `cancellation is never converted into empty state or successful write`() =
        runBlocking {
            val repository = OnboardingStateRepository(failingStore(CancellationException("cancelled")))
            assertTrue(runCatching { repository.state.first() }.exceptionOrNull() is CancellationException)
            assertTrue(
                runCatching {
                    repository.initializeIfNeeded(today = 42L)
                }.exceptionOrNull() is CancellationException,
            )
        }

    private fun repository(
        file: File = File(temporaryFolder.newFolder(), "onboarding.preferences_pb"),
    ): OnboardingStateRepository = OnboardingStateRepository(store(file))

    private fun store(file: File): DataStore<Preferences> {
        val job = SupervisorJob().also(jobs::add)
        val storage =
            OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = { file.path.toPath() },
            )
        return PreferenceDataStoreFactory.create(
            storage = storage,
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + job),
        )
    }

    private fun failingStore(failure: Exception): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw failure }

            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                throw failure
        }
}
