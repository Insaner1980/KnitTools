package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.ai.ProjectSummarizer
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface CounterSummaryResult {
    data class Success(
        val summary: String,
    ) : CounterSummaryResult

    data class Fallback(
        val summary: String,
        val error: String,
    ) : CounterSummaryResult

    data class Failure(
        val error: String,
    ) : CounterSummaryResult
}

class CounterSummaryGenerator
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val yarnCardRepository: YarnCardRepository,
        private val geminiAiService: GeminiAiService,
        private val aiQuotaManager: AiQuotaManager,
        private val preferencesManager: PreferencesManager,
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun generate(state: CounterUiState): CounterSummaryResult {
            val projectId =
                state.projectId
                    ?: return CounterSummaryResult.Failure(context.getString(R.string.ai_summary_fallback))
            val data = buildProjectData(projectId, state)

            if (!aiQuotaManager.hasQuota()) {
                return CounterSummaryResult.Failure(context.getString(R.string.ai_quota_exhausted))
            }

            val language =
                preferencesManager.preferences
                    .first()
                    .appLanguage
                    .promptLanguageName()
            val aiSummary = ProjectSummarizer.summarize(geminiAiService, data, language)
            return if (aiSummary != null) {
                aiQuotaManager.recordCall()
                CounterSummaryResult.Success(aiSummary)
            } else {
                CounterSummaryResult.Fallback(
                    summary = ProjectSummarizer.simpleSummary(data),
                    error = context.getString(R.string.ai_summary_fallback),
                )
            }
        }

        private suspend fun buildProjectData(
            projectId: Long,
            state: CounterUiState,
        ): ProjectSummarizer.ProjectData {
            val sessionCount =
                repository
                    .getSessionsForProject(projectId)
                    .first()
                    .size
            val totalMinutes = repository.getTotalMinutesForProject(projectId)
            val avgRows = if (sessionCount > 0) state.counter.count.toDouble() / sessionCount else 0.0
            val createdAt = state.projects.find { it.id == projectId }?.createdAt ?: System.currentTimeMillis()
            val daysActive = ((System.currentTimeMillis() - createdAt) / 86_400_000).toInt().coerceAtLeast(1)
            val counterSummary =
                state.projectCounters
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { "${it.name}: ${it.count}" }
            val lastSession = repository.getLatestSession(projectId)
            val hoursSinceLastSession =
                lastSession?.let {
                    (System.currentTimeMillis() - it.endedAt) / 3_600_000
                }

            return ProjectSummarizer.ProjectData(
                name = state.projectName,
                currentRow = state.counter.count,
                patternName = state.patternName,
                yarnInfo =
                    state.linkedYarns
                        .map { it.second }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(", "),
                yarnDetailedInfo = buildYarnDetailedInfo(state),
                totalSessionMinutes = totalMinutes,
                sessionCount = sessionCount,
                averageRowsPerSession = avgRows,
                stitchCount = state.stitchCount,
                notes = state.notes,
                daysActive = daysActive,
                counterSummary = counterSummary,
                hoursSinceLastSession = hoursSinceLastSession,
                lastSessionEndRow = lastSession?.endRow,
            )
        }

        private suspend fun buildYarnDetailedInfo(state: CounterUiState): String? =
            state.linkedYarns.firstOrNull()?.first?.let { yarnId ->
                yarnCardRepository.getCard(yarnId)?.let { card ->
                    buildString {
                        append("${card.brand} ${card.yarnName}".trim())
                        card.weightCategory.takeIf { it.isNotBlank() }?.let { append(", $it") }
                        card.fiberContent.takeIf { it.isNotBlank() }?.let { append(", $it") }
                        if (card.lengthMeters.isNotBlank() && card.weightGrams.isNotBlank()) {
                            append(", ${card.lengthMeters}m/${card.weightGrams}g")
                        }
                        card.needleSize.takeIf { it.isNotBlank() }?.let { append(", needle $it") }
                        card.gaugeInfo.takeIf { it.isNotBlank() }?.let { append(", gauge $it") }
                    }
                }
            }
    }
