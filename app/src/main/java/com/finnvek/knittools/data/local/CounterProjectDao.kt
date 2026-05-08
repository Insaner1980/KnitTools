package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterProjectDao {
    @Query("SELECT * FROM counter_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<CounterProjectEntity>>

    @Query("SELECT * FROM counter_projects WHERE id = :id")
    suspend fun getProject(id: Long): CounterProjectEntity?

    @Query("SELECT * FROM counter_projects WHERE id = :id")
    fun observeProject(id: Long): Flow<CounterProjectEntity?>

    @Insert
    suspend fun insert(project: CounterProjectEntity): Long

    @Update
    suspend fun update(project: CounterProjectEntity)

    @Query(
        """
        UPDATE counter_projects
        SET count = CASE WHEN count + :delta < 0 THEN 0 ELSE count + :delta END,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun adjustCount(
        id: Long,
        delta: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET count = CASE WHEN count + :delta < 0 THEN 0 ELSE count + :delta END,
            stepSize = :stepSize,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun adjustCountAndStepSize(
        id: Long,
        delta: Int,
        stepSize: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET count = :count,
            stepSize = :stepSize,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateCounterState(
        id: Long,
        count: Int,
        stepSize: Int,
        updatedAt: Long,
    )

    @Query("UPDATE counter_projects SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateName(
        id: Long,
        name: String,
        updatedAt: Long,
    )

    @Query("UPDATE counter_projects SET notes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNotes(
        id: Long,
        notes: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET secondaryCount = :secondaryCount,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSecondaryCount(
        id: Long,
        secondaryCount: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET sectionName = :sectionName,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSectionName(
        id: Long,
        sectionName: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET stitchCount = :stitchCount,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStitchCount(
        id: Long,
        stitchCount: Int?,
        updatedAt: Long,
    )

    @Query("UPDATE counter_projects SET currentStitch = :stitch, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCurrentStitch(
        id: Long,
        stitch: Int,
        updatedAt: Long,
    )

    @Query("UPDATE counter_projects SET stitchTrackingEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStitchTrackingEnabled(
        id: Long,
        enabled: Boolean,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET patternUri = :patternUri,
            patternName = :patternName,
            currentPatternPage = :currentPatternPage,
            patternRowMapping = :patternRowMapping,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updatePattern(
        id: Long,
        patternUri: String?,
        patternName: String?,
        currentPatternPage: Int,
        patternRowMapping: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET currentPatternPage = :page,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateCurrentPatternPage(
        id: Long,
        page: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET patternRowMapping = :mapping,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updatePatternRowMapping(
        id: Long,
        mapping: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET stepSize = :stepSize,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStepSize(
        id: Long,
        stepSize: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET yarnCardIds = :yarnCardIds,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateYarnCardIds(
        id: Long,
        yarnCardIds: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET targetRows = :targetRows,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateTargetRows(
        id: Long,
        targetRows: Int?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET isCompleted = 1,
            totalRows = :totalRows,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun archiveProject(
        id: Long,
        totalRows: Int,
        completedAt: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE counter_projects
        SET isCompleted = 0,
            totalRows = NULL,
            completedAt = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun reactivateProject(
        id: Long,
        updatedAt: Long,
    )

    @Query("DELETE FROM counter_projects WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM counter_projects")
    suspend fun getProjectCount(): Int

    @Query("SELECT * FROM counter_projects ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getFirstProject(): CounterProjectEntity?

    @Insert
    suspend fun insertHistory(entry: CounterHistoryEntity)

    @Transaction
    suspend fun adjustCountWithHistory(
        projectId: Long,
        delta: Int,
        stepSize: Int,
        action: String,
        previousValue: Int,
        newValue: Int,
        updatedAt: Long,
    ) {
        adjustCountAndStepSize(projectId, delta, stepSize, updatedAt)
        insertHistory(
            CounterHistoryEntity(
                projectId = projectId,
                action = action,
                previousValue = previousValue,
                newValue = newValue,
            ),
        )
    }

    @Transaction
    suspend fun updateCounterStateWithHistory(
        projectId: Long,
        count: Int,
        stepSize: Int,
        action: String,
        previousValue: Int,
        newValue: Int,
        updatedAt: Long,
    ) {
        updateCounterState(projectId, count, stepSize, updatedAt)
        insertHistory(
            CounterHistoryEntity(
                projectId = projectId,
                action = action,
                previousValue = previousValue,
                newValue = newValue,
            ),
        )
    }

    @Query("DELETE FROM counter_history WHERE projectId = :projectId AND timestamp < :before")
    suspend fun deleteHistoryBefore(
        projectId: Long,
        before: Long,
    )

    @Query(
        "SELECT * FROM counter_history WHERE projectId = :projectId " +
            "ORDER BY timestamp DESC, id DESC LIMIT 1",
    )
    suspend fun getLatestHistory(projectId: Long): CounterHistoryEntity?

    @Query("DELETE FROM counter_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("UPDATE counter_projects SET count = :count, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCount(
        id: Long,
        count: Int,
        updatedAt: Long,
    )

    @Transaction
    suspend fun undoLastChange(
        projectId: Long,
        updatedAt: Long,
    ) {
        val entry = getLatestHistory(projectId) ?: return
        updateCount(projectId, entry.previousValue, updatedAt)
        deleteHistoryById(entry.id)
    }

    @Query("SELECT * FROM counter_projects WHERE isCompleted = 0 ORDER BY updatedAt DESC")
    fun getActiveProjects(): Flow<List<CounterProjectEntity>>

    @Query("SELECT * FROM counter_projects WHERE isCompleted = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getActiveProjectsByName(): Flow<List<CounterProjectEntity>>

    @Query("SELECT * FROM counter_projects WHERE isCompleted = 0 ORDER BY id DESC")
    fun getActiveProjectsByCreated(): Flow<List<CounterProjectEntity>>

    @Query("SELECT * FROM counter_projects WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedProjects(): Flow<List<CounterProjectEntity>>

    @Query("SELECT * FROM counter_projects WHERE isCompleted = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getCompletedProjectsByName(): Flow<List<CounterProjectEntity>>

    @Query("SELECT * FROM counter_projects WHERE isCompleted = 1 ORDER BY id DESC")
    fun getCompletedProjectsByCreated(): Flow<List<CounterProjectEntity>>

    @Query("SELECT COUNT(*) FROM counter_projects WHERE isCompleted = 0")
    suspend fun getActiveProjectCount(): Int
}
