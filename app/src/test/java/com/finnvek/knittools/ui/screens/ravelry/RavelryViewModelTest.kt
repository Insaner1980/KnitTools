package com.finnvek.knittools.ui.screens.ravelry

import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.data.remote.Paginator
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.remote.PatternSearchResponse
import com.finnvek.knittools.data.remote.PatternSearchResult
import com.finnvek.knittools.data.remote.RavelryHttpException
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.RavelryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RavelryViewModelTest {
    private lateinit var testDispatcher: TestDispatcher

    private lateinit var repository: RavelryRepository
    private lateinit var proManager: ProManager
    private lateinit var authManager: RavelryAuthManager

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        authManager = mockk(relaxed = true)

        every { authManager.isAuthenticated } returns MutableStateFlow(false)
        every { repository.getSavedPatterns() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(isPro: Boolean): RavelryViewModel {
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns isPro
        return RavelryViewModel(
            repository,
            proManager,
            authManager,
        )
    }

    @Test
    fun `free user with existing active project is routed to pro upgrade instead of creating project`() =
        runTest(testDispatcher) {
            val pattern = PatternDetail(id = 42, name = "Test Pattern", permalink = "test-pattern")
            coEvery { repository.getPatternDetail(42) } returns pattern
            coEvery { repository.isPatternSaved(42) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1

            val vm = createViewModel(isPro = false)
            vm.loadDetail(42)
            advanceUntilIdle()
            var upgradeEvents = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.upgradeToPro.collect {
                    upgradeEvents += 1
                }
            }

            vm.createProjectFromPattern()
            advanceUntilIdle()

            assertEquals(1, upgradeEvents)
            coVerify(exactly = 0) { repository.createProjectFromPattern(any()) }
        }

    @Test
    fun `savePattern ignores repeated taps while save is in flight`() =
        runTest(testDispatcher) {
            val pattern = PatternDetail(id = 42, name = "Test Pattern", permalink = "test-pattern")
            coEvery { repository.getPatternDetail(42) } returns pattern
            coEvery { repository.isPatternSaved(42) } returns false
            val saveResult = CompletableDeferred<Long>()
            coEvery { repository.savePattern(pattern) } coAnswers {
                saveResult.await()
            }
            val vm = createViewModel(isPro = true)
            vm.loadDetail(42)
            advanceUntilIdle()

            vm.savePattern()
            runCurrent()
            vm.savePattern()
            saveResult.complete(7L)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.savePattern(pattern) }
        }

    @Test
    fun `savePattern emits success only after repository save succeeds`() =
        runTest(testDispatcher) {
            val pattern = PatternDetail(id = 42, name = "Test Pattern", permalink = "test-pattern")
            coEvery { repository.getPatternDetail(42) } returns pattern
            coEvery { repository.isPatternSaved(42) } returns false
            val saveResult = CompletableDeferred<Long>()
            coEvery { repository.savePattern(pattern) } coAnswers {
                saveResult.await()
            }
            val vm = createViewModel(isPro = true)
            val events = mutableListOf<PatternSaveResult>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.patternSaveResults.collect { events += it }
            }
            vm.loadDetail(42)
            advanceUntilIdle()

            vm.savePattern()
            runCurrent()

            assertEquals(emptyList<PatternSaveResult>(), events)

            saveResult.complete(7L)
            advanceUntilIdle()

            assertEquals(listOf(PatternSaveResult.Saved), events)
            assertEquals(true, vm.isPatternSaved.value)
        }

    @Test
    fun `savePattern emits failure when repository save fails`() =
        runTest(testDispatcher) {
            val pattern = PatternDetail(id = 42, name = "Test Pattern", permalink = "test-pattern")
            coEvery { repository.getPatternDetail(42) } returns pattern
            coEvery { repository.isPatternSaved(42) } returns false
            coEvery { repository.savePattern(pattern) } throws RuntimeException("save failed")
            val vm = createViewModel(isPro = true)
            val events = mutableListOf<PatternSaveResult>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.patternSaveResults.collect { events += it }
            }
            vm.loadDetail(42)
            advanceUntilIdle()

            vm.savePattern()
            advanceUntilIdle()

            assertEquals(listOf(PatternSaveResult.Failed), events)
            assertFalse(vm.isPatternSaved.value)
        }

    @Test
    fun `blank search clears results without calling repository`() =
        runTest(testDispatcher) {
            val vm = createViewModel(isPro = true)
            coEvery { repository.searchPatterns(any()) } returns searchResponse(1)

            vm.updateQuery("   ")
            vm.search()
            advanceUntilIdle()

            assertEquals(emptyList<PatternSearchResult>(), vm.searchResults.value)
            assertNull(vm.searchError.value)
            coVerify(exactly = 0) { repository.searchPatterns(any()) }
        }

    @Test
    fun `loadMore is ignored when draft query no longer matches submitted query`() =
        runTest(testDispatcher) {
            coEvery {
                repository.searchPatterns(match { it.query == "socks" && it.page == 1 })
            } returns searchResponse(1, page = 1, pageCount = 2)
            coEvery {
                repository.searchPatterns(match { it.page == 2 })
            } returns searchResponse(2, page = 2, pageCount = 2)
            val vm = createViewModel(isPro = true)

            vm.updateQuery("socks")
            vm.search()
            advanceUntilIdle()
            vm.updateQuery("hat")
            vm.loadMore()
            advanceUntilIdle()

            assertEquals(listOf(1), vm.searchResults.value.map { it.id })
            coVerify(exactly = 0) { repository.searchPatterns(match { it.page == 2 }) }
        }

    @Test
    fun `older overlapping search response cannot replace newer search results`() =
        runTest(testDispatcher) {
            val socksResponse = CompletableDeferred<PatternSearchResponse>()
            coEvery {
                repository.searchPatterns(match { it.query == "socks" })
            } coAnswers {
                socksResponse.await()
            }
            coEvery {
                repository.searchPatterns(match { it.query == "hat" })
            } returns searchResponse(2)
            val vm = createViewModel(isPro = true)

            vm.updateQuery("socks")
            vm.search()
            vm.updateQuery("hat")
            vm.search()
            advanceUntilIdle()
            socksResponse.complete(searchResponse(1))
            advanceUntilIdle()

            assertEquals(listOf(2), vm.searchResults.value.map { it.id })
        }

    @Test
    fun `in flight response is ignored when draft query changes before completion`() =
        runTest(testDispatcher) {
            val socksResponse = CompletableDeferred<PatternSearchResponse>()
            coEvery {
                repository.searchPatterns(match { it.query == "socks" })
            } coAnswers {
                socksResponse.await()
            }
            val vm = createViewModel(isPro = true)

            vm.updateQuery("socks")
            vm.search()
            vm.updateQuery("hat")
            socksResponse.complete(searchResponse(1))
            advanceUntilIdle()

            assertEquals(emptyList<PatternSearchResult>(), vm.searchResults.value)
            assertNull(vm.searchError.value)
        }

    @Test
    fun `http rate limit maps to rate limited search error and clears when query changes`() =
        runTest(testDispatcher) {
            coEvery { repository.searchPatterns(any()) } throws RavelryHttpException(429)
            val vm = createViewModel(isPro = true)

            vm.updateQuery("socks")
            vm.search()
            advanceUntilIdle()

            assertEquals(RavelrySearchError.RateLimited, vm.searchError.value)

            vm.updateQuery("hat")

            assertNull(vm.searchError.value)
        }

    @Test
    fun `detail load exposes authentication error when ravelry rejects credentials`() =
        runTest(testDispatcher) {
            coEvery { repository.getPatternDetail(42) } throws RavelryHttpException(401)
            val vm = createViewModel(isPro = true)

            vm.loadDetail(42)
            advanceUntilIdle()

            assertEquals(RavelrySearchError.Authentication, vm.detailError.value)
            assertNull(vm.patternDetail.value)
            assertFalse(vm.isDetailLoading.value)
        }

    private fun searchResponse(
        id: Int,
        page: Int = 1,
        pageCount: Int = 1,
    ): PatternSearchResponse =
        PatternSearchResponse(
            patterns = listOf(PatternSearchResult(id = id, name = "Pattern $id")),
            paginator = Paginator(page = page, pageCount = pageCount, results = pageCount),
        )
}
