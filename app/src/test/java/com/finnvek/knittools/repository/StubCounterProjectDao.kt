package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProjectCompletionEntity
import com.finnvek.knittools.data.local.ProjectCreationReceiptEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

open class StubCounterProjectDao(
    private val projects: List<CounterProjectEntity> = emptyList(),
) : CounterProjectDao {
    override suspend fun findOnboardingProjectId(draftId: String): Long? = null

    override suspend fun insertOnboardingProject(receipt: com.finnvek.knittools.data.local.OnboardingProjectEntity) =
        Unit

    override suspend fun findProjectCreationReceipt(attemptId: String): ProjectCreationReceiptEntity? = null

    override suspend fun insertProjectCreationReceipt(receipt: ProjectCreationReceiptEntity) = Unit

    override suspend fun insertCompletion(event: ProjectCompletionEntity): Long = 0L

    override fun observeCompletions(projectId: Long?): Flow<List<ProjectCompletionEntity>> = flowOf(emptyList())

    override fun getAllProjects(): Flow<List<CounterProjectEntity>> =
        flowOf(projects.sortedByDescending { it.updatedAt })

    override fun observeHasAnyProject(): Flow<Boolean> = flowOf(projects.isNotEmpty())

    override suspend fun hasAnyProject(): Boolean = projects.isNotEmpty()

    override suspend fun getAllProjectsOnce(): List<CounterProjectEntity> = projects

    override suspend fun getProject(id: Long): CounterProjectEntity? = getProjectSync(id)

    override fun observeProject(id: Long): Flow<CounterProjectEntity?> = flowOf(getProjectSync(id))

    override suspend fun insert(project: CounterProjectEntity): Long = 0L

    override suspend fun update(project: CounterProjectEntity) = Unit

    override suspend fun updateCounterState(
        id: Long,
        count: Int,
        stepSize: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateName(
        id: Long,
        name: String,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateProjectDetails(
        id: Long,
        name: String,
        craftType: String,
        mainCounterLabelType: String,
        mainCounterCustomLabel: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateNotes(
        id: Long,
        notes: String,
        updatedAt: Long,
        hasNotes: Boolean,
    ) = Unit

    override suspend fun updateSecondaryCount(
        id: Long,
        secondaryCount: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateStitchState(
        id: Long,
        stitchCount: Int?,
        trackingEnabled: Boolean,
        currentStitch: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateCurrentStitch(
        id: Long,
        stitch: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updatePattern(
        id: Long,
        patternUri: String?,
        patternName: String?,
        currentPatternPage: Int,
        patternRowMapping: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updatePatternAttachment(
        id: Long,
        linkedPatternId: Long?,
        patternUri: String?,
        patternName: String?,
        currentPatternPage: Int,
        patternRowMapping: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updatePatternInformation(
        id: Long,
        linkedPatternId: Long?,
        patternName: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateLinkedPatternName(
        savedPatternId: Long,
        patternName: String,
        updatedAt: Long,
    ) = Unit

    override suspend fun clearPatternInformationIfLinked(
        projectId: Long,
        expectedSavedPatternId: Long,
        updatedAt: Long,
    ): Int = 0

    override suspend fun updateCurrentPatternPage(
        id: Long,
        page: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updatePatternRowMapping(
        id: Long,
        mapping: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateReadingLine(
        id: Long,
        enabled: Boolean,
        yFraction: Float,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateReadingLineVisibility(
        id: Long,
        enabled: Boolean,
        updatedAt: Long,
    ) = Unit

    override suspend fun updatePatternViewerLocation(
        id: Long,
        page: Int,
        yFraction: Float,
        followCurrentRow: Boolean,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateReadingLineFollowState(
        id: Long,
        followCurrentRow: Boolean,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateVerticalReadingGuide(
        id: Long,
        enabled: Boolean,
        xFraction: Float,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateStepSize(
        id: Long,
        stepSize: Int,
        updatedAt: Long,
    ) = Unit

    override suspend fun updateYarnCardIds(
        id: Long,
        yarnCardIds: String,
        updatedAt: Long,
    ) = Unit

    override suspend fun clearLinkedPatternIds(
        patternIds: List<Long>,
        updatedAt: Long,
    ) = Unit

    override suspend fun countProjectsUsingPatternUri(patternUri: String): Int =
        projects.count { it.patternUri == patternUri }

    override suspend fun clearLegacyPatternUriIfMatching(
        projectId: Long,
        patternUri: String,
    ) = Unit

    override suspend fun archiveProject(
        id: Long,
        totalRows: Int,
        completedAt: Long,
        updatedAt: Long,
    ) = Unit

    override suspend fun reactivateProject(
        id: Long,
        updatedAt: Long,
    ) = Unit

    override suspend fun delete(id: Long) = Unit

    override suspend fun getProjectCount(): Int = projects.size

    override suspend fun getLatestActiveProject(): CounterProjectEntity? =
        projects.filter { !it.isCompleted }.maxWithOrNull(
            compareBy<CounterProjectEntity> { it.updatedAt }.thenBy { it.id },
        )

    override fun observeCounterHistory(projectId: Long): Flow<List<CounterHistoryEntity>> = flowOf(emptyList())

    override suspend fun insertHistory(entry: CounterHistoryEntity) = Unit

    override suspend fun getLatestHistory(projectId: Long): CounterHistoryEntity? = null

    override suspend fun updateLatestHistorySessionUndo(
        projectId: Long,
        snapshot: String,
    ) = Unit

    override suspend fun deleteHistoryById(id: Long) = Unit

    override suspend fun updateCount(
        id: Long,
        count: Int,
        updatedAt: Long,
    ) = Unit

    override fun getActiveProjects(): Flow<List<CounterProjectEntity>> =
        flowOf(
            projects
                .filter {
                    !it.isCompleted
                }.sortedWith(compareByDescending<CounterProjectEntity> { it.updatedAt }.thenByDescending { it.id }),
        )

    override fun getActiveProjectsByName(): Flow<List<CounterProjectEntity>> = projectsByName(completed = false)

    override fun getActiveProjectsByCreated(): Flow<List<CounterProjectEntity>> =
        flowOf(
            projects
                .filter {
                    !it.isCompleted
                }.sortedWith(compareByDescending<CounterProjectEntity> { it.createdAt }.thenByDescending { it.id }),
        )

    override fun getCompletedProjects(): Flow<List<CounterProjectEntity>> =
        flowOf(
            projects
                .filter {
                    it.isCompleted
                }.sortedWith(compareByDescending<CounterProjectEntity> { it.completedAt }.thenByDescending { it.id }),
        )

    override fun getCompletedProjectsByName(): Flow<List<CounterProjectEntity>> = projectsByName(completed = true)

    private fun projectsByName(completed: Boolean): Flow<List<CounterProjectEntity>> =
        flowOf(
            projects.filter { it.isCompleted == completed }.sortedWith(
                compareBy<CounterProjectEntity> {
                    it.name
                        .map { char ->
                            if (char in
                                'A'..'Z'
                            ) {
                                char.lowercaseChar()
                            } else {
                                char
                            }
                        }.joinToString("")
                }.thenByDescending { it.id },
            ),
        )

    override fun getCompletedProjectsByCreated(): Flow<List<CounterProjectEntity>> =
        flowOf(
            projects
                .filter {
                    it.isCompleted
                }.sortedWith(compareByDescending<CounterProjectEntity> { it.createdAt }.thenByDescending { it.id }),
        )

    override suspend fun getActiveProjectCount(): Int = projects.count { !it.isCompleted }

    override suspend fun updateTargetRows(
        id: Long,
        targetRows: Int?,
        updatedAt: Long,
    ) = Unit

    private fun getProjectSync(id: Long): CounterProjectEntity? = projects.firstOrNull { it.id == id }
}
