package com.finnvek.knittools.repository

import android.content.Context
import androidx.core.net.toUri
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.SavedPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedPatternRepository
    @Inject
    constructor(
        private val dao: SavedPatternDao,
        @param:ApplicationContext private val context: Context,
        private val counterProjectDao: CounterProjectDao,
        private val transactionRunner: DatabaseTransactionRunner,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun getAll(): Flow<List<SavedPattern>> = dao.getAll().map { patterns -> patterns.map { it.toDomain() } }

        fun getCount(): Flow<Int> = dao.getCount()

        suspend fun getById(id: Long): SavedPattern? = dao.getById(id)?.toDomain()

        suspend fun getByRavelryId(ravelryId: Int): SavedPattern? = dao.getByRavelryId(ravelryId)?.toDomain()

        suspend fun getByPatternUrl(patternUrl: String): SavedPattern? = dao.getByPatternUrl(patternUrl)?.toDomain()

        suspend fun save(pattern: SavedPattern): Long = dao.insert(pattern.toEntity())

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

        suspend fun deleteById(id: Long) = deleteByIds(listOf(id))

        suspend fun deleteByIds(ids: List<Long>) {
            if (ids.isEmpty()) return
            val patterns = dao.getByIds(ids)
            transactionRunner.run {
                counterProjectDao.clearLinkedPatternIds(ids, System.currentTimeMillis())
                dao.deleteByIds(ids)
            }
            deleteUnusedLocalPatternFiles(patterns)
        }

        private suspend fun deleteUnusedLocalPatternFiles(patterns: List<SavedPatternEntity>) {
            patterns
                .map { it.patternUrl }
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { patternUrl ->
                    val uri = patternUrl.toUri()
                    if (!AppFileStorage.isAppOwnedUri(context, uri)) return@forEach

                    val savedPatternStillReferencesFile = dao.getByPatternUrl(patternUrl) != null
                    val projectStillReferencesFile = counterProjectDao.countProjectsUsingPatternUri(patternUrl) > 0
                    if (!savedPatternStillReferencesFile && !projectStillReferencesFile) {
                        withContext(ioDispatcher) {
                            AppFileStorage.deleteUri(context, uri)
                        }
                    }
                }
        }
    }
