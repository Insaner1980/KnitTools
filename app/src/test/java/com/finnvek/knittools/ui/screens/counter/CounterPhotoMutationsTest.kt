package com.finnvek.knittools.ui.screens.counter

import android.net.Uri
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterPhotoMutationsTest : CounterViewModelFixture() {
    @Test
    fun `camera result cannot save under a different selected project`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            val uri = mockk<Uri>()
            coEvery { photoRepository.createPhotoCaptureTarget(7L) } returns (File("capture.jpg") to uri)
            viewModel.authorizeProgressPhotoCreation()
            viewModel.createPhotoCaptureTarget(7L) {}
            advanceUntilIdle()
            val other = CounterProject(id = 8L, name = "Other")
            every { repository.observeProject(8L) } returns MutableStateFlow(other)
            viewModel.selectProject(other)
            advanceUntilIdle()

            viewModel.savePhoto(uri)
            advanceUntilIdle()

            coVerify(exactly = 0) { photoRepository.savePhoto(any(), uri, any(), any()) }
        }

    @Test
    fun `late capture target is cleaned without launching the departed owner`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            val uri = mockk<Uri>()
            val file = File("capture.jpg")
            val reply = CompletableDeferred<Pair<File, Uri>>()
            coEvery { photoRepository.createPhotoCaptureTarget(7L) } coAnswers { reply.await() }
            coEvery { photoRepository.deletePendingPhotoFile(file.absolutePath) } returns Unit
            val callbacks = mutableListOf<PhotoCaptureTarget?>()
            viewModel.authorizeProgressPhotoCreation()
            viewModel.createPhotoCaptureTarget(7L) { callbacks += it }
            runCurrent()
            val other = CounterProject(id = 8L, name = "Other")
            every { repository.observeProject(8L) } returns MutableStateFlow(other)
            viewModel.selectProject(other)
            advanceUntilIdle()
            reply.complete(file to uri)
            advanceUntilIdle()

            assertTrue(callbacks.isEmpty())
            coVerify(exactly = 1) { photoRepository.deletePendingPhotoFile(file.absolutePath) }
        }

    @Test
    fun `project switch clears old gallery and overview photos before new photos load`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            val oldPhoto = ProgressPhoto(id = 1L, projectId = 7L, photoUri = "file:///old.jpg", rowNumber = 0)
            val oldPhotos = MutableStateFlow(listOf(oldPhoto))
            val newPhotos = MutableSharedFlow<List<ProgressPhoto>>()
            every { photoRepository.getLatestPhotos(7L) } returns oldPhotos
            every { photoRepository.getPhotosForProject(7L) } returns oldPhotos
            every { photoRepository.getLatestPhotos(8L) } returns newPhotos
            every { photoRepository.getPhotosForProject(8L) } returns newPhotos
            val other = CounterProject(id = 8L, name = "Other")
            every { repository.observeProject(8L) } returns MutableStateFlow(other)
            viewModel.selectProject(observedProject.value)
            advanceUntilIdle()
            assertEquals(listOf(oldPhoto), viewModel.allPhotos.value)
            assertEquals(listOf(oldPhoto), viewModel.uiState.value.latestPhotos)

            viewModel.selectProject(other)
            advanceUntilIdle()

            assertEquals(8L, viewModel.uiState.value.projectId)
            assertTrue(viewModel.allPhotos.value.isEmpty())
            assertTrue(
                viewModel.uiState.value.latestPhotos
                    .isEmpty(),
            )
            val newPhoto = oldPhoto.copy(id = 2L, projectId = 8L, photoUri = "file:///new.jpg")
            newPhotos.emit(listOf(newPhoto))
            advanceUntilIdle()
            assertEquals(listOf(newPhoto), viewModel.allPhotos.value)
            assertEquals(listOf(newPhoto), viewModel.uiState.value.latestPhotos)
        }

    @Test
    fun `save failure does not crash and permits retry`() =
        runTest {
            assertPhotoSaveRetry { uri ->
                coEvery { photoRepository.savePhoto(7L, uri, any(), any()) } throws
                    IOException("synthetic save failure")
            }
        }

    @Test
    fun `delete failure does not crash and permits retry`() =
        runTest {
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.photoErrors)
            advanceUntilIdle()
            val photo = ProgressPhoto(id = 1L, projectId = 7L, photoUri = "", rowNumber = 0)
            coEvery { photoRepository.deletePhoto(photo) } throws IOException("synthetic delete failure")

            viewModel.deletePhoto(photo)
            advanceUntilIdle()
            coEvery { photoRepository.deletePhoto(photo) } returns Unit
            viewModel.deletePhoto(photo)
            advanceUntilIdle()

            coVerify(exactly = 2) { photoRepository.deletePhoto(photo) }
            assertEquals(1, errors.size)
        }

    @Test
    fun `note failure does not crash and permits retry`() =
        runTest {
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.photoErrors)
            advanceUntilIdle()
            coEvery { photoRepository.updatePhotoNote(1L, "Note") } throws IOException("synthetic note failure")

            viewModel.updatePhotoNote(1L, "Note")
            advanceUntilIdle()
            coEvery { photoRepository.updatePhotoNote(1L, "Note") } returns Unit
            viewModel.updatePhotoNote(1L, "Note")
            advanceUntilIdle()

            coVerify(exactly = 2) { photoRepository.updatePhotoNote(1L, "Note") }
            assertEquals(1, errors.size)
        }

    @Test
    fun `failed photo compression reports failure and keeps creation authorized for retry`() =
        runTest {
            assertPhotoSaveRetry { uri ->
                coEvery { photoRepository.savePhoto(7L, uri, any(), any()) } returns 0L
            }
        }

    @Test
    fun `cancelled photo mutation is not reported as a write failure`() =
        runTest {
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.photoErrors)
            coEvery { photoRepository.updatePhotoNote(1L, "Note") } throws CancellationException("cancelled")

            viewModel.updatePhotoNote(1L, "Note")
            advanceUntilIdle()

            assertEquals(0, errors.size)
        }

    private fun TestScope.assertPhotoSaveRetry(stubFailure: (Uri) -> Unit) {
        val viewModel = viewModel()
        val errors = collectErrors(viewModel.photoErrors)
        advanceUntilIdle()
        val uri = mockk<Uri>()
        viewModel.authorizeProgressPhotoCreation()
        stubFailure(uri)

        viewModel.savePhoto(uri)
        advanceUntilIdle()
        coEvery { photoRepository.savePhoto(7L, uri, any(), any()) } returns 1L
        viewModel.savePhoto(uri)
        advanceUntilIdle()

        coVerify(exactly = 2) { photoRepository.savePhoto(7L, uri, any(), any()) }
        assertEquals(1, errors.size)
    }
}
