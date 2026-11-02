package com.finnvek.knittools.ui.tips

import android.util.Log
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.domain.tips.TipContext
import com.finnvek.knittools.domain.tips.TipId
import com.finnvek.knittools.domain.tips.TipRules
import com.finnvek.knittools.domain.tips.TipScreen
import com.finnvek.knittools.domain.tips.TipSignal
import com.finnvek.knittools.repository.OnboardingState
import com.finnvek.knittools.repository.OnboardingStateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class TipPresentation(
    val routeKey: String,
    val id: TipId,
    val projectId: Long?,
)

data class TipSessionState(
    val loaded: Boolean = false,
    val onboarding: OnboardingState = OnboardingState(),
    val routeKey: String? = null,
    val routeEntryGeneration: Long = 0L,
    val presentation: TipPresentation? = null,
    val workSessionRequest: TipPresentation? = null,
    val shownThisProcess: Int = 0,
    val processSeen: Set<TipId> = emptySet(),
    val incrementTimesMillis: Map<Long, List<Long>> = emptyMap(),
    val incrementsInProject: Map<Long, Int> = emptyMap(),
    val undoProjectId: Long? = null,
    val targetReachedProjectId: Long? = null,
    val processCelebratedTargets: Set<Long> = emptySet(),
) {
    internal fun hasSeen(
        id: TipId,
        projectId: Long?,
    ): Boolean =
        if (id == TipId.TARGET_REACHED) {
            projectId != null &&
                (projectId in processCelebratedTargets || projectId.toString() in onboarding.targetCelebratedProjects)
        } else {
            id in processSeen || id in onboarding.tipsSeen
        }

    private fun seenTipsFor(context: TipContext): Set<TipId> {
        val seen = context.tipsSeen + onboarding.tipsSeen + processSeen - TipId.TARGET_REACHED
        return if (hasSeen(TipId.TARGET_REACHED, context.projectId)) seen + TipId.TARGET_REACHED else seen
    }

    internal fun canShowAutomaticTip(
        routeKey: String,
        id: TipId,
        projectId: Long?,
    ): Boolean {
        if (!loaded || onboarding.initializedVersion == 0 || this.routeKey != routeKey) return false
        if (presentation != null || workSessionRequest != null || hasSeen(id, projectId)) return false
        return if (id == TipId.UNDO) {
            projectId != null && undoProjectId == projectId
        } else {
            shownThisProcess < TipRules.MAX_AUTOMATIC_TIPS
        }
    }

    internal fun workSessionRequestFor(
        routeKey: String,
        context: TipContext,
    ): TipPresentation? {
        val request = workSessionRequest ?: return null
        if (this.routeKey != routeKey || request.routeKey != routeKey) return null
        if (!loaded || onboarding.initializedVersion <= 0 || !context.screenReady || context.blocked) return null
        if (context.screen != TipScreen.COUNTER || request.projectId != context.projectId) return null
        if (context.isCompleted || context.hasSessions || context.hasActiveSession) return null
        return request
    }

    fun contextFor(context: TipContext): TipContext =
        context.copy(
            screenReady = context.screenReady && loaded && onboarding.initializedVersion > 0,
            isStarter = context.projectId != null && context.projectId == onboarding.starterProjectId,
            onboardingPatternReady =
                context.projectId != null && context.projectId == onboarding.guidedPatternProjectId,
            tipsSeen = seenTipsFor(context),
            shownThisProcess = shownThisProcess,
            firstUseEpochDay = onboarding.firstUseEpochDay,
            counterOpenCount = onboarding.counterOpenCount,
            counterOpenDays = onboarding.counterOpenDays,
            backupCreated = onboarding.backupCreated,
            firstSessionTipPending = onboarding.firstSessionTipPending,
            incrementTimesMillis = incrementTimesMillis[context.projectId].orEmpty(),
            incrementsInProject =
                maxOf(
                    onboarding.projectIncrementCounts[context.projectId] ?: 0,
                    incrementsInProject[context.projectId] ?: 0,
                ),
            undoAfterFirstRow = context.projectId != null && context.projectId == undoProjectId,
            targetReached = context.projectId != null && context.projectId == targetReachedProjectId,
        )
}

@Singleton
class TipSession
    @Inject
    constructor(
        private val onboarding: OnboardingStateRepository,
        private val analytics: UsageAnalytics,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val lock = Any()
        private val persistenceMutex = Mutex()
        private val mutableState = MutableStateFlow(TipSessionState())
        val state: StateFlow<TipSessionState> = mutableState.asStateFlow()
        private var counterEntryProject: Long? = null
        private var counterEntryDay: Long? = null
        private var firstRowProject: Long? = null

        init {
            scope.launch {
                onboarding.state.collect { stored ->
                    mutableState.update { it.copy(loaded = true, onboarding = stored) }
                }
            }
        }

        fun onRouteChanged(routeKey: String?) {
            // Palautuvan NavHostin tilapäinen null ei ole uusi käynti eikä poistuminen.
            if (routeKey == null) return
            synchronized(lock) {
                if (mutableState.value.routeKey == routeKey) return
                mutableState.value.presentation?.let { finishPresentation(it, acted = false) }
                mutableState.update {
                    it.copy(
                        routeKey = routeKey,
                        routeEntryGeneration = it.routeEntryGeneration + 1L,
                        workSessionRequest = null,
                        undoProjectId = null,
                        targetReachedProjectId = null,
                    )
                }
                counterEntryProject = null
                counterEntryDay = null
                firstRowProject = null
            }
        }

        fun requestWorkSession(
            projectId: Long,
            destinationRouteKey: String,
        ) {
            synchronized(lock) {
                // Kutsuja on jo navigoinut ja varmistanut todellisen laskurikohteen.
                onRouteChanged(destinationRouteKey)
                mutableState.update {
                    it.copy(workSessionRequest = TipPresentation(destinationRouteKey, TipId.WORK_SESSION, projectId))
                }
            }
        }

        internal fun tryShowRequestedWorkSession(
            routeKey: String,
            context: TipContext,
        ): Boolean =
            synchronized(lock) {
                val current = mutableState.value
                val request = current.workSessionRequestFor(routeKey, context) ?: return false
                if (current.presentation != null) return false
                // Käyttäjän pyytämä ohje ei kuluta automaattisten vihjeiden näyttöbudjettia.
                mutableState.update { it.copy(presentation = request) }
                true
            }

        internal fun cancelInvalidWorkSessionRequest(
            routeKey: String,
            context: TipContext,
        ) {
            synchronized(lock) {
                val current = mutableState.value
                val request = current.workSessionRequest ?: return
                if (current.routeKey != routeKey ||
                    request.routeKey != routeKey ||
                    context.screen != TipScreen.COUNTER
                ) {
                    return
                }
                if (!current.contextFor(context).screenReady) return
                val validProject =
                    context.projectId == request.projectId &&
                        !context.isCompleted &&
                        !context.hasSessions &&
                        !context.hasActiveSession
                if (validProject) return
                if (current.presentation == request) finishPresentation(request, acted = false)
                mutableState.update { it.copy(workSessionRequest = null) }
            }
        }

        fun recordCounterOpen(
            routeKey: String,
            projectId: Long,
            today: Long,
        ) {
            synchronized(lock) {
                if (mutableState.value.routeKey != routeKey) return
                if (counterEntryProject == projectId && counterEntryDay == today) return
                counterEntryProject = projectId
                counterEntryDay = today
                persist { onboarding.recordCounterOpen(today) }
            }
        }

        fun tryShow(
            routeKey: String,
            id: TipId,
            projectId: Long?,
        ): Boolean =
            synchronized(lock) {
                val current = mutableState.value
                if (!current.canShowAutomaticTip(routeKey, id, projectId)) return false
                mutableState.update {
                    it.copy(
                        presentation = TipPresentation(routeKey, id, projectId),
                        shownThisProcess = it.shownThisProcess + if (id == TipId.UNDO) 0 else 1,
                    )
                }
                if (id == TipId.FIRST_ROW) firstRowProject = projectId
                true
            }

        fun dismiss(
            routeKey: String,
            id: TipId,
            acted: Boolean = false,
        ) {
            synchronized(lock) {
                val presentation = mutableState.value.presentation ?: return
                if (presentation.routeKey == routeKey && presentation.id == id) finishPresentation(presentation, acted)
            }
        }

        fun markPresentationVisible(presentation: TipPresentation) {
            synchronized(lock) {
                val projectId = presentation.projectId ?: return
                val current = mutableState.value
                if (presentation.id != TipId.TARGET_REACHED ||
                    current.presentation != presentation ||
                    current.hasSeen(TipId.TARGET_REACHED, projectId)
                ) {
                    return
                }
                mutableState.update { it.copy(processCelebratedTargets = it.processCelebratedTargets + projectId) }
                persist { onboarding.markTargetCelebrated(projectId) }
            }
        }

        fun onSignal(signal: TipSignal) {
            synchronized(lock) {
                when (signal) {
                    is TipSignal.Increment -> recordIncrement(signal)
                    is TipSignal.TargetReached -> {
                        if (!mutableState.value.hasSeen(TipId.TARGET_REACHED, signal.projectId)) {
                            mutableState.update { it.copy(targetReachedProjectId = signal.projectId) }
                        }
                    }
                    is TipSignal.SessionSaved ->
                        persist {
                            onboarding.recordFirstSessionSaved(
                                signal.wasFirstCompletedSession,
                            )
                        }
                }
            }
        }

        fun resetTips(): Deferred<Boolean> =
            scope.async(start = CoroutineStart.UNDISPATCHED) {
                persistenceMutex.withLock {
                    val succeeded =
                        try {
                            onboarding.resetTips()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            false
                        }
                    if (succeeded) clearSessionTips()
                    succeeded
                }
            }

        private fun clearSessionTips() {
            synchronized(lock) {
                firstRowProject = null
                mutableState.update {
                    it.copy(
                        onboarding = it.onboarding.copy(tipsSeen = emptySet()),
                        presentation = null,
                        workSessionRequest = null,
                        shownThisProcess = 0,
                        processSeen = emptySet(),
                        undoProjectId = null,
                        targetReachedProjectId = null,
                    )
                }
            }
        }

        private fun recordIncrement(signal: TipSignal.Increment) {
            mutableState.update { current ->
                val previous =
                    maxOf(
                        current.onboarding.projectIncrementCounts[signal.projectId] ?: 0,
                        current.incrementsInProject[signal.projectId] ?: 0,
                    )
                val recent =
                    (current.incrementTimesMillis[signal.projectId].orEmpty() + signal.monotonicMillis)
                        .sorted()
                        .takeLast(TipRules.BURST_INCREMENT_COUNT)
                current.copy(
                    incrementTimesMillis = current.incrementTimesMillis + (signal.projectId to recent),
                    incrementsInProject =
                        current.incrementsInProject +
                            (signal.projectId to (previous + 1).coerceAtMost(TipRules.STEP_INCREMENT_THRESHOLD)),
                )
            }
            persist { onboarding.recordProjectIncrement(signal.projectId) }
            if (firstRowProject == signal.projectId) {
                mutableState.value.presentation?.takeIf { it.id == TipId.FIRST_ROW }?.let {
                    finishPresentation(it, acted = false)
                }
                mutableState.update { it.copy(undoProjectId = signal.projectId) }
                firstRowProject = null
            }
        }

        private fun finishPresentation(
            presentation: TipPresentation,
            acted: Boolean,
        ) {
            mutableState.update {
                it.copy(
                    presentation = null,
                    workSessionRequest = it.workSessionRequest.takeUnless { request -> request == presentation },
                    processSeen =
                        if (presentation.id ==
                            TipId.TARGET_REACHED
                        ) {
                            it.processSeen
                        } else {
                            it.processSeen + presentation.id
                        },
                    undoProjectId = if (presentation.id == TipId.UNDO) null else it.undoProjectId,
                    targetReachedProjectId =
                        if (presentation.id ==
                            TipId.TARGET_REACHED
                        ) {
                            null
                        } else {
                            it.targetReachedProjectId
                        },
                )
            }
            try {
                analytics.track(if (acted) UsageEvent.TIP_ACTED else UsageEvent.TIP_DISMISSED)
            } catch (_: Exception) {
                Log.w("TipSession", "Unable to record guidance event")
            }
            persist {
                if (presentation.id != TipId.TARGET_REACHED) onboarding.markTipSeen(presentation.id)
                if (presentation.id == TipId.FIRST_SESSION_SAVED) onboarding.setFirstSessionTipPending(false)
            }
        }

        private fun persist(action: suspend () -> Unit): Job =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                // Navigointi ei peru jo näytetyn vihjeen tallennusta, ja nollaus odottaa aiemmat kirjoitukset.
                persistenceMutex.withLock {
                    try {
                        action()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        Log.w("TipSession", "Unable to persist guidance state")
                    }
                }
            }
    }
