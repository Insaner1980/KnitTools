package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {
    @Query("SELECT * FROM progress_photos WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getPhotosForProject(projectId: Long): Flow<List<ProgressPhotoEntity>>

    @Query(
        "SELECT * FROM progress_photos WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT :limit",
    )
    fun getLatestPhotos(
        projectId: Long,
        limit: Int = 5,
    ): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT COUNT(*) FROM progress_photos WHERE projectId = :projectId")
    fun getPhotoCount(projectId: Long): Flow<Int>

    @Query("SELECT * FROM progress_photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT COUNT(*) FROM progress_photos")
    fun getAllPhotoCount(): Flow<Int>

    @Insert
    suspend fun insert(photo: ProgressPhotoEntity): Long

    @Query("UPDATE progress_photos SET note = :note WHERE id = :id")
    suspend fun updateNote(
        id: Long,
        note: String?,
    )

    @Query("DELETE FROM progress_photos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM progress_photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM progress_photos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ProgressPhotoEntity>
}
