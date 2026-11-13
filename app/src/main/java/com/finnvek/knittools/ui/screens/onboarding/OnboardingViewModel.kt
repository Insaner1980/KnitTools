package com.finnvek.knittools.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.BillingUserMessage
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.storage.OnboardingPdfStorage
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.ui.screens.pro.TrialJourneyCoordinator
import com.finnvek.knittools.ui.screens.pro.toMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class OnboardingUiState(
    val required: Boolean? = null,
    val draft: OnboardingDraft? = null,
    val busy: Boolean = false,
    val messageRes: Int? = null,
    val trialUnavailable: Boolean = false,
    val completionMessageRes: Int? = null,
)

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val stateRepository: OnboardingStateRepository,
        private val projects: CounterRepository,
        private val preferences: PreferencesManager,
        private val pdfStorage: OnboardingPdfStorage,
        private val patterns: SavedPatternRepository,
        private val proManager: ProManager,
        private val billing: BillingManager,
        private val trialManager: TrialManager,
        private val journeyCoordinator: TrialJourneyCoordinator,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        private val _state = MutableStateFlow(OnboardingUiState())
        val state = _state.asStateFlow()
        val proState = proManager.proState
        val proReady = proManager.initialControlsReady
        val trialState = trialManager.trialState
        val offer = billing.selectedOffer
        val productStatus = billing.productStatus
        val purchaseInFlight = billing.purchaseFlowInFlight
        val purchasePending = billing.purchasePending
        val usageAnalyticsEnabled =
            preferences.preferences
                .map { it.usageAnalyticsEnabled }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        private val writes = Mutex()

        init {
            load()
            viewModelScope.launch {
                billing.purchaseMessages.collect { message ->
                    if (_state.value.draft?.step == OnboardingStep.Price) {
                        _state.update {
                            it.copy(
                                messageRes =
                                    message
                                        .takeUnless {
                                            it ==
                                                BillingUserMessage.PURCHASE_CANCELLED
                                        }?.toMessageRes(),
                            )
                        }
                    }
                }
            }
        }

        fun load() {
            if (_state.value.busy) return
            _state.update { it.copy(busy = true, messageRes = null) }
            viewModelScope.launch {
                try {
                    val loaded = loadDraft()
                    _state.value = OnboardingUiState(required = loaded != null && !loaded.completed, draft = loaded)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.update { it.copy(busy = false, messageRes = R.string.journey_save_failed) }
                }
            }
        }

        private suspend fun loadDraft(): OnboardingDraft? =
            withContext(ioDispatcher) {
                val stored = stateRepository.draft.first()
                if (stored == null) {
                    val previous = stateRepository.state.first()
                    if (previous.welcomeDismissed || projects.hasAnyProject()) return@withContext null
                    return@withContext OnboardingDraft(UUID.randomUUID().toString()).also { persist(it) }
                }
                val created = projects.findOnboardingProject(stored.id)
                when {
                    !stored.completed && created != null -> {
                        // Luotu projekti ei palaa lomakkeeseen; keskeytynyt Ready-näkymä jatkuu Readystä.
                        val step =
                            if (stored.step == OnboardingStep.Ready) OnboardingStep.Ready else OnboardingStep.Price
                        stored.copy(step = step, projectId = created).also { persist(it) }
                    }
                    !stored.completed && projects.hasAnyProject() ->
                        stored.copy(completed = true, exitPending = false).also { persist(it) }
                    else -> stored
                }
            }

        fun edit(transform: (OnboardingDraft) -> OnboardingDraft) {
            val current = _state.value
            if (current.busy || current.required != true) return
            val draft = current.draft?.let(transform) ?: return
            _state.update { it.copy(draft = draft, messageRes = null) }
            applicationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                writes.withLock {
                    try {
                        persist(draft)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        _state.update { it.copy(messageRes = R.string.journey_save_failed) }
                    }
                }
            }
        }

        fun setUsageAnalyticsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                if (!preferences.setUsageAnalyticsEnabled(enabled)) {
                    _state.update { it.copy(messageRes = R.string.generic_error_unknown) }
                }
            }
        }

        fun next() = changeStep(UsageEvent.ONBOARDING_CONTINUED) { it.next() }

        fun previous() = changeStep(UsageEvent.ONBOARDING_BACK) { it.previous() }

        fun skip() = changeStep(UsageEvent.ONBOARDING_SKIPPED) { it.copy(step = OnboardingStep.Price) }

        private fun changeStep(
            event: UsageEvent,
            transform: (OnboardingDraft) -> OnboardingDraft,
        ) = operate {
            val next = transform(it)
            persist(next)
            _state.update { state -> state.copy(draft = next) }
            analytics.track(event, it.step.analyticsDetail())
        }

        fun createProject() =
            operate { draft ->
                analytics.track(UsageEvent.PROJECT_CREATION_SUBMITTED, UsageDetail.SOURCE_ONBOARDING)
                persist(draft)
                when (
                    val result =
                        projects.createOnboardingProject(
                            draft,
                            proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS),
                        )
                ) {
                    is ProjectCreationResult.Created -> {
                        analytics.track(UsageEvent.PROJECT_CREATED, UsageDetail.SOURCE_ONBOARDING)
                        val next = draft.copy(projectId = result.projectId, step = OnboardingStep.Ready)
                        _state.update { it.copy(draft = next) }
                        persist(next)
                        try {
                            preferences.setLastProjectCraftType(draft.craftType)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // The committed project and its durable receipt remain authoritative.
                        }
                    }
                    ProjectCreationResult.LimitReached ->
                        _state.update {
                            it.copy(
                                messageRes = R.string.journey_project_limit,
                            )
                        }
                    else -> _state.update { it.copy(messageRes = R.string.journey_project_invalid) }
                }
            }

        fun selectPdf(source: String) =
            operate { draft ->
                analytics.track(UsageEvent.PDF_IMPORT_STARTED, UsageDetail.SOURCE_ONBOARDING)
                val copied: Pair<String, String>? = pdfStorage.copy(source)
                if (copied == null) {
                    analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.SOURCE_ONBOARDING)
                    _state.update { it.copy(messageRes = R.string.journey_pdf_failed) }
                    return@operate
                }
                val next = draft.copy(pdfUri = copied.first, pdfName = copied.second)
                var persisted = false
                try {
                    persist(next)
                    persisted = true
                } finally {
                    if (!persisted) deleteUnusedPdf(copied.first)
                }
                _state.update { it.copy(draft = next) }
                draft.pdfUri?.let { deleteUnusedPdf(it) }
                analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, UsageDetail.SOURCE_ONBOARDING)
            }

        fun removePdf() =
            operate { draft ->
                val next = draft.copy(pdfUri = null, pdfName = null)
                persist(next)
                _state.update { it.copy(draft = next) }
                draft.pdfUri?.let { deleteUnusedPdf(it) }
            }

        fun startTrial() =
            operate { draft ->
                when (proManager.startTrial()) {
                    TrialStartResult.Started -> {
                        journeyCoordinator.requestStartedJourney(fromOnboarding = true)
                        finishDraft(draft)
                    }
                    TrialStartResult.AlreadyActive -> finishDraft(draft)
                    TrialStartResult.AlreadyExpired ->
                        _state.update {
                            it.copy(trialUnavailable = true, messageRes = R.string.journey_trial_used)
                        }
                    TrialStartResult.AlreadyTampered ->
                        _state.update {
                            it.copy(trialUnavailable = true, messageRes = R.string.journey_trial_unavailable)
                        }
                    TrialStartResult.Failed -> _state.update { it.copy(messageRes = R.string.pro_trial_start_failed) }
                }
            }

        fun finish() = operate { finishDraft(it) }

        fun finishWithPro() = operate { finishDraft(it, confirmedPurchase = true) }

        fun acknowledgeCompletionMessage() {
            _state.update { it.copy(completionMessageRes = null) }
        }

        private suspend fun finishDraft(
            draft: OnboardingDraft,
            confirmedPurchase: Boolean = false,
        ) {
            val projectId = draft.projectId?.takeIf { projects.getProject(it) != null }
            val next = draft.copy(completed = true, exitPending = projectId != null, projectId = projectId)
            persist(next)
            analytics.track(UsageEvent.ONBOARDING_COMPLETED)
            _state.update {
                it.copy(
                    required = false,
                    draft = next,
                    completionMessageRes = if (confirmedPurchase) R.string.journey_pro_active else null,
                )
            }
            if (projectId == null) draft.pdfUri?.let { deleteUnusedPdf(it) }
        }

        private suspend fun deleteUnusedPdf(uri: String) {
            try {
                withContext(NonCancellable) { patterns.deleteLocalPatternFileIfUnused(uri) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Cleanup failure must not undo or misreport the already persisted draft.
            }
        }

        fun acknowledgeExit() {
            applicationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                writes.withLock {
                    val draft = _state.value.draft?.takeIf { it.completed && it.exitPending } ?: return@withLock
                    val next = draft.copy(exitPending = false)
                    try {
                        persist(next)
                        _state.update { it.copy(draft = next) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // A later launch can safely reopen the same project; no project is created here.
                    }
                }
            }
        }

        fun refreshPrice() = billing.retryProductDetails()

        private suspend fun persist(draft: OnboardingDraft) {
            check(stateRepository.saveDraft(draft)) { "Onboarding draft persistence failed" }
        }

        private fun operate(action: suspend (OnboardingDraft) -> Unit) {
            val current = _state.value
            if (current.busy || current.draft == null) return
            _state.update { it.copy(busy = true, messageRes = null) }
            viewModelScope.launch {
                try {
                    writes.withLock { action(current.draft) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.update { it.copy(messageRes = R.string.journey_save_failed) }
                } finally {
                    _state.update { it.copy(busy = false) }
                }
            }
        }
    }

internal fun OnboardingStep.analyticsDetail(): UsageDetail =
    when (this) {
        OnboardingStep.Welcome -> UsageDetail.STEP_WELCOME
        OnboardingStep.Craft -> UsageDetail.STEP_CRAFT
        OnboardingStep.Needs -> UsageDetail.STEP_NEEDS
        OnboardingStep.Counter -> UsageDetail.STEP_COUNTER
        OnboardingStep.Toolbox -> UsageDetail.STEP_TOOLBOX
        OnboardingStep.Project -> UsageDetail.STEP_PROJECT
        OnboardingStep.Ready -> UsageDetail.STEP_READY
        OnboardingStep.Price -> UsageDetail.STEP_PRICE
    }
