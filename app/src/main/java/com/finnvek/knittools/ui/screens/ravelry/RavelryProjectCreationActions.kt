package com.finnvek.knittools.ui.screens.ravelry

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.remote.NeedleSize
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.RavelryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val PENDING_PROJECT_PATTERN_KEY = "ravelryPendingProjectPattern"

internal class RavelryProjectCreationActions(
    private val repository: RavelryRepository,
    private val proManager: ProManager,
    private val savedStateHandle: SavedStateHandle,
    private val scope: CoroutineScope,
) {
    private val isPro: Boolean get() = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)
    private val navigateToProjectChannel = Channel<Long>(Channel.BUFFERED)
    val navigateToProject = navigateToProjectChannel.receiveAsFlow()

    private val _projectCreationPromptCount = MutableStateFlow<Int?>(null)
    val projectCreationPromptCount = _projectCreationPromptCount.asStateFlow()
    val projectCreationPrompts = projectCreationPromptCount.filterNotNull()

    private var isProjectCreationInFlight = false
    private var pendingProjectPattern: PatternDetail? =
        savedStateHandle.get<String>(PENDING_PROJECT_PATTERN_KEY)?.let { encoded ->
            runCatching { Json.decodeFromString<PatternDetail>(encoded) }.getOrNull()
        }
        set(value) {
            field = value
            savedStateHandle[PENDING_PROJECT_PATTERN_KEY] =
                value?.let {
                    Json.encodeToString(
                        PatternDetail.serializer(),
                        PatternDetail(
                            id = it.id,
                            name = it.name,
                            permalink = it.permalink,
                            designer = it.designer,
                            photos = it.photos.take(1),
                            difficultyAverage = it.difficultyAverage,
                            availability = it.availability,
                            canonicalUrl = it.canonicalUrl,
                            originalUrl = it.originalUrl,
                            rowGauge = it.rowGauge,
                            yardage = it.yardage ?: it.yardageMax,
                            patternNeedleSizes =
                                it.patternNeedleSizes.map { needle ->
                                    NeedleSize(name = needle.name)
                                },
                            yarnWeight = it.yarnWeight,
                        ),
                    )
                }
        }
    private var creationContinuationJob: Job? = null
    private var creationReconciliationRequested = false

    init {
        scope.launch {
            repository.observeActiveProjectCount().collect { reconcilePendingCreation() }
        }
        scope.launch {
            proManager.proState.collect { reconcilePendingCreation() }
        }
    }

    fun dismissPendingProjectCreation() {
        pendingProjectPattern = null
        _projectCreationPromptCount.value = null
        creationReconciliationRequested = false
        creationContinuationJob?.cancel()
    }

    private fun reconcilePendingCreation() {
        if (creationContinuationJob?.isActive == true || isProjectCreationInFlight) {
            creationReconciliationRequested = true
            return
        }
        val request = pendingProjectPattern ?: return
        creationContinuationJob =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val count = repository.getActiveProjectCount()
                    if (pendingProjectPattern != request) return@launch
                    if (isPro || count == 0) {
                        performProjectCreation(request, reconsiderLimit = false)
                    } else {
                        _projectCreationPromptCount.value = count
                    }
                } finally {
                    creationContinuationJob = null
                    val reconsider = creationReconciliationRequested
                    creationReconciliationRequested = false
                    if (reconsider) reconcilePendingCreation()
                }
            }
        creationContinuationJob?.start()
    }

    fun create(detail: PatternDetail) = performProjectCreation(detail, reconsiderLimit = true)

    fun retry() {
        pendingProjectPattern?.let { performProjectCreation(it, reconsiderLimit = true) }
    }

    private fun performProjectCreation(
        detail: PatternDetail,
        reconsiderLimit: Boolean,
    ) {
        if (isProjectCreationInFlight) return
        pendingProjectPattern = detail
        isProjectCreationInFlight = true
        scope.launch {
            var limited = false
            try {
                val result = repository.createProjectFromPattern(detail, isPro)
                if (pendingProjectPattern != detail) return@launch
                when (result) {
                    is ProjectCreationResult.Created -> {
                        pendingProjectPattern = null
                        _projectCreationPromptCount.value = null
                        navigateToProjectChannel.send(result.projectId)
                    }
                    ProjectCreationResult.LimitReached -> {
                        val activeCount = repository.getActiveProjectCount()
                        limited = true
                        if (pendingProjectPattern == detail) _projectCreationPromptCount.value = activeCount
                    }
                    ProjectCreationResult.InvalidProject,
                    ProjectCreationResult.FolderMissing,
                    -> pendingProjectPattern = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Tietonäkymä jää käytettäväksi myöhempää uutta yritystä varten.
            } finally {
                isProjectCreationInFlight = false
                val reconsider = creationReconciliationRequested
                creationReconciliationRequested = false
                if (limited &&
                    (reconsiderLimit || reconsider) &&
                    pendingProjectPattern == detail
                ) {
                    reconcilePendingCreation()
                }
            }
        }
    }
}
