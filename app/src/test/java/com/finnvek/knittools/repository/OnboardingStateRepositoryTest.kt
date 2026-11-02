package com.finnvek.knittools.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finnvek.knittools.domain.tips.BASIC_TIPS
import com.finnvek.knittools.domain.tips.TipId
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class OnboardingStateRepositoryTest {
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
    fun `journey completion stores the draft destination and tip suppression together`() =
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
            assertEquals(7L, state.guidedPatternProjectId)
            assertTrue(state.tipsSeen.containsAll(setOf(TipId.FIRST_ROW, TipId.UNDO)))
            assertFalse(TipId.PATTERN_TILE in state.tipsSeen)
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
    fun `corrupt store is replaced with empty preferences`() =
        runBlocking {
            val file = File(temporaryFolder.root, "corrupt.preferences_pb")
            file.writeBytes(byteArrayOf(-1, -1, -1))
            val repository = repository(file)

            assertEquals(OnboardingState(), repository.state.first())
            repository.initializeIfNeeded(hasAnyProject = false, today = 42L)
            assertEquals(ONBOARDING_STATE_VERSION, repository.state.first().initializedVersion)
        }

    @Test
    fun `first launch without projects leaves tips available and initialization is idempotent`() =
        runBlocking {
            val repository = repository()
            repository.initializeIfNeeded(hasAnyProject = false, today = 42L)
            repository.initializeIfNeeded(hasAnyProject = true, today = 43L)

            val state = repository.state.first()
            assertEquals(ONBOARDING_STATE_VERSION, state.initializedVersion)
            assertEquals(42L, state.firstUseEpochDay)
            assertTrue(state.tipsSeen.isEmpty())
            assertFalse(state.welcomeDismissed)
        }

    @Test
    fun `existing projects seed only basic tips while preserving other seen tips`() =
        runBlocking {
            val repository = repository()
            repository.markTipSeen(TipId.BACKUP)
            repository.initializeIfNeeded(hasAnyProject = true, today = 42L)

            assertEquals(BASIC_TIPS + TipId.BACKUP, repository.state.first().tipsSeen)
            assertNull(repository.state.first().starterProjectId)
        }

    @Test
    fun `reset clears only seen tips and basic tip seeding merges after restore`() =
        runBlocking {
            val repository = repository()
            repository.initializeIfNeeded(hasAnyProject = true, today = 42L)
            repository.dismissWelcome()
            repository.setStarterProject(7L)
            repository.hideProjectSetup()
            repository.markProjectSetupCelebrated()
            repository.recordCounterOpen(42L)
            repository.recordProjectIncrement(7L)
            repository.setFirstSessionTipPending(true)
            repository.markTargetCelebrated(7L)
            repository.markBackupCreated()
            repository.markTrialChartSeen(100L)
            repository.markTrialPhotoPdfCreated(100L)
            val beforeReset = repository.state.first()

            repository.resetTips()

            assertEquals(beforeReset.copy(tipsSeen = emptySet()), repository.state.first())
            repository.markTipSeen(TipId.READING_LINE)
            repository.markBasicTipsSeen()
            assertEquals(BASIC_TIPS + TipId.READING_LINE, repository.state.first().tipsSeen)
        }

    @Test
    fun `starter is assigned once and saved flags survive reopening`() =
        runBlocking {
            val file = File(temporaryFolder.root, "restart.preferences_pb")
            val repository = repository(file)
            repository.setStarterProject(7L)
            repository.setStarterProject(9L)
            repository.dismissWelcome()
            repository.markTipSeen(TipId.UNDO)
            val saved = repository.state.first()
            jobs.last().cancelAndJoin()

            assertEquals(saved, repository(file).state.first())
            assertEquals(7L, saved.starterProjectId)
        }

    @Test
    fun `counter opens count once per local day and follow midnight and zone changes`() =
        runBlocking {
            val repository = repository()
            val helsinki = ZoneId.of("Europe/Helsinki")
            val beforeMidnight = Clock.fixed(Instant.parse("2026-10-29T21:59:00Z"), helsinki)
            val afterMidnight = Clock.fixed(Instant.parse("2026-10-29T22:01:00Z"), helsinki)
            val nextZoneDay = Clock.fixed(Instant.parse("2026-10-30T11:01:00Z"), ZoneId.of("Pacific/Kiritimati"))
            val firstDay = LocalDate.now(beforeMidnight).toEpochDay()
            repository.recordCounterOpen(firstDay)
            repository.recordCounterOpen(firstDay)
            assertEquals(2, repository.state.first().counterOpenCount)
            assertEquals(1, repository.state.first().counterOpenDays)

            repository.recordCounterOpen(LocalDate.now(afterMidnight).toEpochDay())
            repository.recordCounterOpen(LocalDate.now(nextZoneDay).toEpochDay())

            val state = repository.state.first()
            assertEquals(4, state.counterOpenCount)
            assertEquals(3, state.counterOpenDays)
            assertEquals(LocalDate.now(nextZoneDay).toEpochDay(), state.counterOpenLastDay)
        }

    @Test
    fun `target celebrations drop oldest after two hundred across reopenings`() =
        runBlocking {
            val file = File(temporaryFolder.root, "targets.preferences_pb")
            val repository = repository(file)
            // Tunnistejärjestys poikkeaa aikajärjestyksestä, jotta lajittelu ei läpäise testiä vahingossa.
            for (id in 400L downTo 201L) repository.markTargetCelebrated(id)
            repository.markTargetCelebrated(400L)
            jobs.last().cancelAndJoin()

            val restarted = repository(file)
            restarted.markTargetCelebrated(999L)
            val ids = restarted.state.first().targetCelebratedProjects
            assertEquals(200, ids.size)
            assertFalse("400" in ids)
            assertTrue("399" in ids)
            assertTrue("201" in ids)
            assertTrue("999" in ids)
        }

    @Test
    fun `first saved session stays consumed after dismissal reset and reopening`() =
        runBlocking {
            val file = File(temporaryFolder.root, "first-session.preferences_pb")
            val repository = repository(file)
            repository.recordFirstSessionSaved(wasFirstCompletedSession = true)
            assertTrue(repository.state.first().firstSessionTipPending)
            assertTrue(repository.state.first().firstSaveObserved)
            repository.setFirstSessionTipPending(false)
            repository.markTipSeen(TipId.FIRST_SESSION_SAVED)
            repository.resetTips()
            jobs.last().cancelAndJoin()

            val restarted = repository(file)
            restarted.recordFirstSessionSaved(wasFirstCompletedSession = true)
            assertTrue(restarted.state.first().firstSaveObserved)
            assertFalse(restarted.state.first().firstSessionTipPending)
        }

    @Test
    fun `observing an existing session history prevents a false first after its deletion`() =
        runBlocking {
            val repository = repository()
            repository.recordFirstSessionSaved(wasFirstCompletedSession = false)
            assertTrue(repository.state.first().firstSaveObserved)
            assertFalse(repository.state.first().firstSessionTipPending)

            repository.recordFirstSessionSaved(wasFirstCompletedSession = true)
            assertFalse(repository.state.first().firstSessionTipPending)
        }

    @Test
    fun `legacy first session seen state does not queue another celebration`() =
        runBlocking {
            val repository = repository()
            repository.markTipSeen(TipId.FIRST_SESSION_SAVED)
            repository.recordFirstSessionSaved(wasFirstCompletedSession = true)
            assertTrue(repository.state.first().firstSaveObserved)
            assertFalse(repository.state.first().firstSessionTipPending)
        }

    @Test
    fun `committed increments persist independently per project and saturate at the tip threshold`() =
        runBlocking {
            val file = File(temporaryFolder.root, "increments.preferences_pb")
            val repository = repository(file)
            repeat(35) { repository.recordProjectIncrement(7L) }
            repository.recordProjectIncrement(9L)
            repository.recordProjectIncrement(0L)
            repository.recordProjectIncrement(-1L)
            assertEquals(mapOf(7L to 30, 9L to 1), repository.state.first().projectIncrementCounts)
            jobs.last().cancelAndJoin()

            assertEquals(mapOf(7L to 30, 9L to 1), repository(file).state.first().projectIncrementCounts)
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
    fun `first session pending flag can be cleared`() =
        runBlocking {
            val repository = repository()
            repository.setFirstSessionTipPending(true)
            assertTrue(repository.state.first().firstSessionTipPending)
            repository.setFirstSessionTipPending(false)
            assertFalse(repository.state.first().firstSessionTipPending)
        }

    @Test
    fun `read and write IO failures preserve safe defaults`() =
        runBlocking {
            val repository = OnboardingStateRepository(failingStore(IOException("unavailable")))
            assertEquals(OnboardingState(), repository.state.first())
            repository.initializeIfNeeded(hasAnyProject = true, today = 42L)
            repository.dismissWelcome()
            repository.markTrialChartSeen(100L)
            repository.markTrialPhotoPdfCreated(100L)
            assertFalse(repository.resetTips())
            assertEquals(OnboardingState(), repository.state.first())
        }

    @Test
    fun `cancellation is never converted into empty state or successful write`() =
        runBlocking {
            val repository = OnboardingStateRepository(failingStore(CancellationException("cancelled")))
            assertTrue(runCatching { repository.state.first() }.exceptionOrNull() is CancellationException)
            assertTrue(
                runCatching {
                    repository.initializeIfNeeded(hasAnyProject = true, today = 42L)
                }.exceptionOrNull() is CancellationException,
            )
        }

    private fun repository(
        file: File = File(temporaryFolder.newFolder(), "onboarding.preferences_pb"),
    ): OnboardingStateRepository {
        val job = SupervisorJob().also(jobs::add)
        val storage =
            OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = { file.path.toPath() },
            )
        val store =
            PreferenceDataStoreFactory.create(
                storage = storage,
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = CoroutineScope(Dispatchers.IO + job),
            )
        return OnboardingStateRepository(store)
    }

    private fun failingStore(failure: Exception): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw failure }

            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                throw failure
        }
}
