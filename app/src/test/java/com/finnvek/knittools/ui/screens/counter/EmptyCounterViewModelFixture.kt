package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.emptyCounterViewModel(
    repository: CounterRepository,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    handle: SavedStateHandle = SavedStateHandle(),
    counters: ProjectCounterRepository = emptyProjectCounters(),
    photos: ProgressPhotoRepository = emptyProgressPhotos(),
): CounterViewModel {
    val preferences = mockk<PreferencesManager>()
    every { preferences.preferences } returns emptyFlow()
    val proManager = mockk<ProManager>()
    every { proManager.proState } returns MutableStateFlow(ProState())
    every { proManager.hasFeature(any()) } returns false
    val yarns = mockk<YarnCardRepository>()
    every { yarns.getAllCards() } returns emptyFlow()
    val savedPatterns = mockk<SavedPatternRepository>()
    every { savedPatterns.getAll() } returns flowOf(emptyList())
    val reminders = mockk<ReminderRepository>()
    every { reminders.getRemindersForProject(any()) } returns flowOf(emptyList())
    val yarnNotes = mockk<ProjectYarnNoteRepository>()
    every { yarnNotes.observeForProject(any()) } returns flowOf(emptyList())
    val documents = mockk<ProjectDocumentRepository>()
    every { documents.observeDocuments(any<Long>()) } returns flowOf(emptyList())
    every { documents.observeActiveDocument(any()) } returns flowOf(null)
    return CounterViewModel(
        repository = repository,
        reminderRepository = reminders,
        projectCounterRepository = counters,
        photoRepository = photos,
        projectYarnNoteRepository = yarnNotes,
        preferencesManager = preferences,
        proManager = proManager,
        yarnCardRepository = yarns,
        savedPatternRepository = savedPatterns,
        projectDocumentRepository = documents,
        patternDocumentStorage = mockk(),
        inAppReviewManager = mockk(),
        savedStateHandle = handle,
        context = mockk<Context>(relaxed = true),
        ioDispatcher = dispatcher,
        applicationScope = backgroundScope,
    )
}

private fun emptyProjectCounters(): ProjectCounterRepository {
    val counters = mockk<ProjectCounterRepository>()
    every { counters.getCountersForProject(any()) } returns flowOf(emptyList())
    return counters
}

private fun emptyProgressPhotos(): ProgressPhotoRepository {
    val photos = mockk<ProgressPhotoRepository>()
    every { photos.getLatestPhotos(any()) } returns flowOf(emptyList())
    every { photos.getPhotosForProject(any()) } returns flowOf(emptyList())
    return photos
}
