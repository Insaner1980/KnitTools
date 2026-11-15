package com.finnvek.knittools.ui.screens.onboarding

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.storage.OnboardingPdfStorage
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingState
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.ui.screens.pro.TrialJourneyCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val analytics = mockk<UsageAnalytics>(relaxed = true)

    @Test
    fun `cancelling draft persistence still finishes cleanup of the new PDF`() =
        runTest {
            preparePdfReplacement()
            var deleted = false
            coEvery { state.saveDraft(match { it.pdfUri == "copied-uri" }) } coAnswers { awaitCancellation() }
            coEvery { patterns.deleteLocalPatternFileIfUnused("copied-uri") } coAnswers {
                delay(1)
                deleted = true
            }
            val vm = viewModel()
            vm.selectPdf("new-uri", "pdf")
            runCurrent()
            store.clear()
            advanceUntilIdle()
            verify(exactly = 0) { analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.SOURCE_ONBOARDING) }
            assertTrue(deleted)
            assertEquals("old-uri", saved.value?.pdfUri)
            coVerify(exactly = 0) { patterns.deleteLocalPatternFileIfUnused("old-uri") }
        }

    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val state = mockk<OnboardingStateRepository>()
    private val projects = mockk<CounterRepository>()
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    private val pdf = mockk<OnboardingPdfStorage>()
    private val patterns = mockk<SavedPatternRepository>(relaxed = true)
    private val pro = mockk<ProManager>()
    private val billing = mockk<BillingManager>()
    private val trial = mockk<TrialManager>(relaxed = true)
    private val coordinator = TrialJourneyCoordinator()
    private val saved = MutableStateFlow<OnboardingDraft?>(null)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { preferences.preferences } returns MutableStateFlow(AppPreferences())
        every { state.draft } returns saved
        every { state.state } returns MutableStateFlow(OnboardingState())
        coEvery { state.saveDraft(any()) } coAnswers {
            saved.value = firstArg()
            true
        }
        coEvery { projects.hasAnyProject() } returns false
        coEvery { projects.findOnboardingProject(any()) } returns null
        every { pro.proState } returns MutableStateFlow(ProState())
        every { pro.initialControlsReady } returns MutableStateFlow(true)
        every { pro.hasFeature(any()) } returns false
        every { billing.selectedOffer } returns MutableStateFlow(null)
        every { billing.productStatus } returns MutableStateFlow(BillingProductStatus.Loading)
        every { billing.purchaseFlowInFlight } returns MutableStateFlow(false)
        every { billing.purchasePending } returns MutableStateFlow(false)
        every { billing.purchaseMessages } returns MutableSharedFlow()
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): OnboardingViewModel =
        OnboardingViewModel(
            state,
            projects,
            preferences,
            pdf,
            patterns,
            pro,
            billing,
            trial,
            coordinator,
            dispatcher,
            backgroundScope,
            analytics,
        ).also {
            store.put("onboarding", it)
            runCurrent()
        }

    @Test fun `fresh launch creates durable empty draft but existing users are not interrupted`() =
        runTest {
            val vm = viewModel()
            assertTrue(vm.state.value.required == true)
            assertEquals("", saved.value?.name)
            store.clear()
            saved.value = null
            coEvery { projects.hasAnyProject() } returns true
            assertFalse(viewModel().state.value.required!!)
            assertNull(saved.value)
        }

    @Test fun `analytics remains off until a successful explicit preference write`() =
        runTest {
            val storedPreferences = MutableStateFlow(AppPreferences())
            every { preferences.preferences } returns storedPreferences
            coEvery { preferences.setUsageAnalyticsEnabled(true) } returns false
            val vm = viewModel()
            backgroundScope.launch { vm.usageAnalyticsEnabled.collect() }
            runCurrent()
            assertFalse(vm.usageAnalyticsEnabled.value)
            coVerify(exactly = 0) { preferences.setUsageAnalyticsEnabled(any()) }
            vm.setUsageAnalyticsEnabled(true)
            runCurrent()
            assertFalse(vm.usageAnalyticsEnabled.value)
            assertEquals(R.string.generic_error_unknown, vm.state.value.messageRes)

            coEvery { preferences.setUsageAnalyticsEnabled(true) } coAnswers {
                storedPreferences.value = storedPreferences.value.copy(usageAnalyticsEnabled = true)
                true
            }
            vm.setUsageAnalyticsEnabled(true)
            runCurrent()
            assertTrue(vm.usageAnalyticsEnabled.value)
            coVerify(exactly = 2) { preferences.setUsageAnalyticsEnabled(true) }
        }

    @Test fun `committed receipt resumes price without creating another project`() =
        runTest {
            saved.value = OnboardingDraft("same-id", step = OnboardingStep.Project, name = "My socks")
            coEvery { projects.findOnboardingProject("same-id") } returns 7L
            coEvery { projects.hasAnyProject() } returns true
            val vm = viewModel()
            assertEquals(
                OnboardingStep.Price,
                vm.state.value.draft
                    ?.step,
            )
            assertEquals(
                7L,
                vm.state.value.draft
                    ?.projectId,
            )
            coVerify(exactly = 0) { projects.createOnboardingProject(any(), any()) }
        }

    @Test fun `interrupted ready step resumes ready and continues to price`() =
        runTest {
            saved.value = OnboardingDraft("same-id", step = OnboardingStep.Ready, name = "My socks", projectId = 7)
            coEvery { projects.findOnboardingProject("same-id") } returns 7L
            coEvery { projects.hasAnyProject() } returns true
            val vm = viewModel()
            assertEquals(
                OnboardingStep.Ready,
                vm.state.value.draft
                    ?.step,
            )
            vm.next()
            runCurrent()
            assertEquals(
                OnboardingStep.Price,
                vm.state.value.draft
                    ?.step,
            )
            assertEquals(OnboardingStep.Price, saved.value?.step)
            coVerify(exactly = 0) { projects.createOnboardingProject(any(), any()) }
        }

    @Test fun `double create tap runs one creation and persistence failure leaves retryable draft`() =
        runTest {
            saved.value = OnboardingDraft("same-id", step = OnboardingStep.Project, name = "My socks")
            val result = CompletableDeferred<ProjectCreationResult>()
            coEvery { projects.createOnboardingProject(any(), any()) } coAnswers { result.await() }
            val vm = viewModel()
            vm.createProject()
            vm.createProject()
            runCurrent()
            assertTrue(vm.state.value.busy)
            coEvery { state.saveDraft(match { it.projectId != null }) } returns false
            result.complete(ProjectCreationResult.Created(7))
            runCurrent()
            assertEquals(
                OnboardingStep.Ready,
                vm.state.value.draft
                    ?.step,
            )
            assertEquals(
                7L,
                vm.state.value.draft
                    ?.projectId,
            )
            assertEquals(R.string.journey_save_failed, vm.state.value.messageRes)
            coVerify(exactly = 1) { projects.createOnboardingProject(any(), any()) }
        }

    @Test fun `all trial outcomes preserve the specified destination and sheet rules`() =
        runTest {
            TrialStartResult.entries.forEach { result ->
                saved.value = OnboardingDraft("trial", step = OnboardingStep.Price)
                coordinator.consume()
                val vm = viewModel()
                coEvery { pro.startTrial() } returns result
                vm.startTrial()
                runCurrent()
                val completes = result == TrialStartResult.Started || result == TrialStartResult.AlreadyActive
                assertEquals(!completes, vm.state.value.required)
                assertEquals(result == TrialStartResult.Started, coordinator.pending.value)
                if (result == TrialStartResult.Started) assertTrue(coordinator.fromOnboarding.value)
                if (result == TrialStartResult.AlreadyExpired || result == TrialStartResult.AlreadyTampered) {
                    assertTrue(vm.state.value.trialUnavailable)
                }
                if (result ==
                    TrialStartResult.Failed
                ) {
                    assertEquals(R.string.pro_trial_start_failed, vm.state.value.messageRes)
                }
                store.clear()
            }
        }

    @Test fun `failed PDF replacement preserves existing attachment and filename`() =
        runTest {
            saved.value = OnboardingDraft("pdf", step = OnboardingStep.Project, pdfUri = "old-uri", pdfName = "old.pdf")
            coEvery { pdf.copy("new-uri") } returns null
            val vm = viewModel()
            vm.selectPdf("new-uri", "pdf")
            runCurrent()
            assertEquals("old-uri", saved.value?.pdfUri)
            assertEquals(
                "old.pdf",
                vm.state.value.draft
                    ?.pdfName,
            )
            assertEquals(R.string.journey_pdf_failed, vm.state.value.messageRes)
            verify(exactly = 1) { analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.SOURCE_ONBOARDING) }
            coVerify(exactly = 0) { patterns.deleteLocalPatternFileIfUnused(any()) }
        }

    @Test
    fun `failed PDF draft persistence deletes only the new copy`() =
        runTest {
            preparePdfReplacement()
            coEvery { patterns.deleteLocalPatternFileIfUnused(any()) } returns Unit
            val vm = viewModel()
            coEvery { state.saveDraft(match { it.pdfUri == "copied-uri" }) } returns false

            vm.selectPdf("new-uri", "pdf")
            runCurrent()

            assertEquals("old-uri", saved.value?.pdfUri)
            assertEquals(
                "old.pdf",
                vm.state.value.draft
                    ?.pdfName,
            )
            assertEquals(R.string.journey_save_failed, vm.state.value.messageRes)
            assertFalse(vm.state.value.busy)
            coVerify(exactly = 1) { patterns.deleteLocalPatternFileIfUnused("copied-uri") }
            coVerify(exactly = 0) { patterns.deleteLocalPatternFileIfUnused("old-uri") }
            verify(exactly = 1) { analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.SOURCE_ONBOARDING) }
            verify(exactly = 0) { analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, UsageDetail.SOURCE_ONBOARDING) }
        }

    @Test
    fun `PDF copy exception reports one failed import without changing the draft`() =
        runTest {
            preparePdfReplacement()
            coEvery { pdf.copy("new-uri") } throws IOException("copy failed")
            val vm = viewModel()

            vm.selectPdf("new-uri", "pdf")
            runCurrent()

            assertEquals("old-uri", saved.value?.pdfUri)
            assertEquals(R.string.journey_save_failed, vm.state.value.messageRes)
            verify(exactly = 1) { analytics.track(UsageEvent.PDF_IMPORT_STARTED, UsageDetail.SOURCE_ONBOARDING) }
            verify(exactly = 1) { analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.SOURCE_ONBOARDING) }
            verify(exactly = 0) { analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, UsageDetail.SOURCE_ONBOARDING) }
        }

    @Test
    fun `successful PDF replacement deletes only the old copy after persistence`() =
        runTest {
            preparePdfReplacement()
            coEvery { patterns.deleteLocalPatternFileIfUnused("old-uri") } coAnswers {
                assertEquals("copied-uri", saved.value?.pdfUri)
            }
            val vm = viewModel()

            vm.selectPdf("new-uri", "pdf")
            runCurrent()

            assertEquals("copied-uri", saved.value?.pdfUri)
            assertEquals(
                "new.pdf",
                vm.state.value.draft
                    ?.pdfName,
            )
            assertNull(vm.state.value.messageRes)
            coVerify(exactly = 1) { patterns.deleteLocalPatternFileIfUnused("old-uri") }
            coVerify(exactly = 0) { patterns.deleteLocalPatternFileIfUnused("copied-uri") }
        }

    private fun preparePdfReplacement() {
        saved.value = OnboardingDraft("pdf", step = OnboardingStep.Project, pdfUri = "old-uri", pdfName = "old.pdf")
        coEvery { pdf.copy("new-uri") } returns ("copied-uri" to "new.pdf")
    }

    @Test fun `obsolete PDF results cannot change the current or completed draft`() =
        runTest {
            preparePdfReplacement()
            val vm = viewModel()
            vm.selectPdf("new-uri", "old-draft")
            runCurrent()
            vm.skip()
            runCurrent()
            vm.selectPdf("new-uri", "pdf")
            runCurrent()
            vm.finish()
            runCurrent()
            vm.selectPdf("new-uri", "pdf")
            runCurrent()

            coVerify(exactly = 0) { pdf.copy(any()) }
            assertTrue(saved.value?.completed == true)
        }

    @Test fun `ready summary follows the committed project identified by the receipt`() =
        runTest {
            saved.value = OnboardingDraft("ready", step = OnboardingStep.Ready, name = "  Draft  ", projectId = 7)
            coEvery { projects.findOnboardingProject("ready") } returns 7L
            val project = CounterProject(id = 7L, name = "Draft", count = 4, patternName = "Committed.pdf")
            every { projects.observeProject(7L) } returns MutableStateFlow(project)
            val vm = viewModel()
            backgroundScope.launch { vm.readyProject.collect() }
            runCurrent()

            assertEquals(project, vm.readyProject.value)
            assertEquals("  Draft  ", saved.value?.name)
        }

    @Test fun `purchase confirmation requires the active price draft and verified status`() =
        runTest {
            saved.value = OnboardingDraft("price", step = OnboardingStep.Price)
            val proState = MutableStateFlow(ProState())
            every { pro.proState } returns proState
            val vm = viewModel()
            vm.finishWithPro("price")
            runCurrent()
            assertFalse(saved.value!!.completed)
            proState.value = ProState(status = ProStatus.PRO_PURCHASED)
            vm.finishWithPro("old-price")
            runCurrent()
            assertFalse(saved.value!!.completed)
            vm.finishWithPro("price")
            runCurrent()
            assertEquals(R.string.journey_pro_active, vm.state.value.completionMessageRes)
            assertTrue(saved.value!!.completed)
        }

    @Test fun `queued field edits cannot overwrite a later completed draft`() =
        runTest {
            val vm = viewModel()
            vm.edit { it.copy(name = "My socks") }
            vm.skip()
            runCurrent()
            vm.finish()
            runCurrent()
            assertEquals("My socks", saved.value?.name)
            assertTrue(saved.value?.completed == true)
            assertFalse(vm.state.value.required!!)
        }

    @Test fun `exit acknowledgement finishes persistence after ViewModel is cleared`() =
        runTest {
            saved.value = OnboardingDraft("completed", completed = true, exitPending = true, projectId = 7L)
            val vm = viewModel()
            val writeStarted = CompletableDeferred<Unit>()
            val finishWrite = CompletableDeferred<Unit>()
            coEvery { state.saveDraft(match { !it.exitPending }) } coAnswers {
                writeStarted.complete(Unit)
                finishWrite.await()
                saved.value = firstArg()
                true
            }

            vm.acknowledgeExit("old")
            runCurrent()
            assertFalse(writeStarted.isCompleted)
            vm.acknowledgeExit("completed")
            runCurrent()
            assertTrue(writeStarted.isCompleted)
            store.clear()
            finishWrite.complete(Unit)
            runCurrent()

            assertEquals(false, saved.value?.exitPending)
            assertEquals(true, saved.value?.completed)
            assertEquals(7L, saved.value?.projectId)
            coVerify(exactly = 1) { state.saveDraft(match { !it.exitPending }) }
        }

    @Test fun `read failure never starts a fresh draft`() =
        runTest {
            coEvery { projects.hasAnyProject() } throws IOException("unavailable")
            val vm = viewModel()
            assertNull(vm.state.value.required)
            assertNull(vm.state.value.draft)
            coVerify(exactly = 0) { state.saveDraft(any()) }
        }
}
