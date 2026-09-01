package com.finnvek.knittools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions") // Kaikki Saved Pattern -haut ja kirjoitukset kuuluvat samaan Room-rajapintaan.
interface SavedPatternDao {
    @Query("SELECT * FROM saved_patterns ORDER BY savedAt DESC")
    fun getAll(): Flow<List<SavedPatternEntity>>

    @Query("SELECT * FROM saved_patterns WHERE id = :id")
    suspend fun getById(id: Long): SavedPatternEntity?

    @Query("SELECT * FROM saved_patterns WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SavedPatternEntity>

    @Query(
        "SELECT * FROM saved_patterns WHERE ravelryPatternId = :ravelryPatternId " +
            "ORDER BY savedAt DESC, id DESC LIMIT 1",
    )
    suspend fun getByRavelryPatternId(ravelryPatternId: Int): SavedPatternEntity?

    @Query("SELECT * FROM saved_patterns WHERE canonicalUrl = :canonicalUrl ORDER BY savedAt DESC, id DESC LIMIT 1")
    suspend fun getByCanonicalUrl(canonicalUrl: String): SavedPatternEntity?

    @Query(
        "SELECT * FROM saved_patterns WHERE canonicalUrl = :canonicalUrl AND id != :excludedId " +
            "ORDER BY savedAt DESC, id DESC LIMIT 1",
    )
    suspend fun getByCanonicalUrlExcludingId(
        canonicalUrl: String,
        excludedId: Long,
    ): SavedPatternEntity?

    @Query("SELECT * FROM saved_patterns WHERE originalUrl = :originalUrl ORDER BY savedAt DESC, id DESC LIMIT 1")
    suspend fun getByOriginalUrl(originalUrl: String): SavedPatternEntity?

    @Query("SELECT * FROM saved_patterns WHERE localPdfUri = :localPdfUri ORDER BY savedAt DESC, id DESC LIMIT 1")
    suspend fun getByLocalPdfUri(localPdfUri: String): SavedPatternEntity?

    @Query(
        "SELECT * FROM saved_patterns WHERE name = :name AND designerName = :designerName " +
            "ORDER BY savedAt DESC, id DESC LIMIT 1",
    )
    suspend fun getByTitleAndDesignerName(
        name: String,
        designerName: String,
    ): SavedPatternEntity?

    @Query("SELECT * FROM saved_patterns")
    suspend fun getAllOnce(): List<SavedPatternEntity>

    @Query("SELECT * FROM saved_patterns WHERE source = 'LOCAL_FILE' AND localPdfUri IS NOT NULL")
    suspend fun getImportedPatternsOnce(): List<SavedPatternEntity>

    @Insert
    suspend fun insert(pattern: SavedPatternEntity): Long

    @Update
    suspend fun update(pattern: SavedPatternEntity)

    @Query("DELETE FROM saved_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_patterns WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM saved_patterns")
    fun getCount(): Flow<Int>
}
