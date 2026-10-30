package com.finnvek.knittools.repository

import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.TrialJourneyFacts
import com.finnvek.knittools.pro.TrialManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrialJourneyStateRepository
    @Inject
    constructor(
        projects: CounterRepository,
        counters: ProjectCounterRepository,
        yarnCards: YarnCardRepository,
        photos: ProgressPhotoRepository,
        onboarding: OnboardingStateRepository,
        proManager: ProManager,
        @ApplicationScope scope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) {
        private val activity =
            combine(
                projects.getAllProjects(),
                counters.getAllCounters(),
                yarnCards.getAllCards(),
                photos.getAllPhotos(),
            ) { projectRows, counterRows, yarnRows, photoRows ->
                TrialActivitySnapshot(projectRows, counterRows, yarnRows, photoRows)
            }

        // Null säilyy, kunnes jokainen lähde on toimittanut oikean arvon.
        val state: StateFlow<TrialJourneyFacts?> =
            combine(
                proManager.proState,
                proManager.initialStateReady,
                activity,
                onboarding.state,
            ) { pro, ready, rows, marks ->
                if (ready) rows.facts(pro.trialStartTimestamp, marks) else null
            }.flowOn(ioDispatcher)
                .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), null)
    }

private data class TrialActivitySnapshot(
    val projects: List<CounterProject>,
    val counters: List<ProjectCounter>,
    val yarnCards: List<YarnCard>,
    val photos: List<ProgressPhoto>,
) {
    fun facts(
        start: Long,
        onboarding: OnboardingState,
    ): TrialJourneyFacts {
        val activeProjects = projects.filterNot(CounterProject::isCompleted)
        val addedCounters = counters.filter { it.createdAt.isDuringTrial(start) }
        return TrialJourneyFacts(
            trialStartTimestamp = start,
            activeProjectCount = activeProjects.size,
            latestActiveProject =
                activeProjects.maxWithOrNull(compareBy(CounterProject::updatedAt, CounterProject::id)),
            projectsAdded = projects.count { it.createdAt.isDuringTrial(start) },
            countersAdded = addedCounters.size,
            yarnCardsAdded = yarnCards.count { it.createdAt.isDuringTrial(start) },
            photosAdded = photos.count { it.createdAt.isDuringTrial(start) },
            repeatCounterAdded = addedCounters.any { it.counterType == ProjectCounterType.REPEATING },
            chartSeen = start > 0L && onboarding.trialChartSeenForStart == start,
            photoPdfCreated = start > 0L && onboarding.trialPhotoPdfCreatedForStart == start,
        )
    }
}

private fun Long.isDuringTrial(start: Long): Boolean =
    start > 0L && this >= start && this - start < TimeUnit.DAYS.toMillis(TrialManager.TRIAL_DURATION_DAYS.toLong())
