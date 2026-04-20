package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedPatternRepository
    @Inject
    constructor(
        private val dao: SavedPatternDao,
        @param:ApplicationContext private val context: Context,
    ) {
        fun getAll(): Flow<List<SavedPatternEntity>> = dao.getAll()

        fun getCount(): Flow<Int> = dao.getCount()

        suspend fun getById(id: Long): SavedPatternEntity? = dao.getById(id)

        suspend fun getByRavelryId(ravelryId: Int): SavedPatternEntity? = dao.getByRavelryId(ravelryId)

        suspend fun getByPatternUrl(patternUrl: String): SavedPatternEntity? = dao.getByPatternUrl(patternUrl)

        suspend fun save(pattern: SavedPatternEntity): Long = dao.insert(pattern)

        suspend fun saveImportedPatternIfMissing(
            patternUrl: String,
            name: String,
        ): Long? {
            if (!patternUrl.startsWith("content://") && !patternUrl.startsWith("file://")) return null
            val existing = dao.getByPatternUrl(patternUrl)
            if (existing != null) return existing.id

            return dao.insert(
                SavedPatternEntity(
                    ravelryId = 0,
                    name = name,
                    designerName = context.getString(R.string.imported_pattern_designer),
                    patternUrl = patternUrl,
                ),
            )
        }

        suspend fun deleteById(id: Long) = dao.deleteById(id)

        suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)
    }
