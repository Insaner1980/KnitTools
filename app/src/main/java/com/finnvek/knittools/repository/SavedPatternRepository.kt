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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
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
        private val saveMutex = Mutex()

        fun getAll(): Flow<List<SavedPattern>> = dao.getAll().map { patterns -> patterns.map { it.toDomain() } }

        fun getCount(): Flow<Int> = dao.getCount()

        suspend fun getById(id: Long): SavedPattern? = dao.getById(id)?.toDomain()

        suspend fun getByIdIfAvailable(id: Long): SavedPattern? {
            val pattern = dao.getById(id) ?: return null
            if (pattern.patternUrl.isAppOwnedMissingFile()) {
                deleteById(id)
                return null
            }
            return pattern.toDomain()
        }

        suspend fun getByRavelryId(ravelryId: Int): SavedPattern? = dao.getByRavelryId(ravelryId)?.toDomain()

        suspend fun getByPatternUrl(patternUrl: String): SavedPattern? = dao.getByPatternUrl(patternUrl)?.toDomain()

        suspend fun pruneMissingLocalPattern(patternUrl: String): Boolean {
            if (!patternUrl.isAppOwnedMissingFile()) return false
            dao.getByPatternUrl(patternUrl)?.let { pattern -> deleteById(pattern.id) }
            return true
        }

        suspend fun save(pattern: SavedPattern): Long = dao.insert(pattern.toEntity())

        suspend fun saveRavelryPatternIfMissing(pattern: SavedPattern): Long {
            if (pattern.ravelryId <= 0) return save(pattern)
            return saveMutex.withLock {
                dao.getByRavelryId(pattern.ravelryId)?.id ?: dao.insert(pattern.toEntity())
            }
        }

        suspend fun saveImportedPatternIfMissing(
            patternUrl: String,
            name: String,
        ): Long? {
            if (!patternUrl.startsWith("content://") && !patternUrl.startsWith("file://")) return null
            return saveMutex.withLock {
                val existing = dao.getByPatternUrl(patternUrl)
                if (existing != null) return@withLock existing.id

                dao.insert(
                    SavedPatternEntity(
                        ravelryId = 0,
                        name = name,
                        designerName = context.getString(R.string.imported_pattern_designer),
                        patternUrl = patternUrl,
                    ),
                )
            }
        }

        suspend fun findReusableImportedPatternUrl(
            candidatePatternUrl: String,
            name: String,
        ): String? {
            val candidateFile =
                AppFileStorage
                    .resolveAppOwnedFile(context, candidatePatternUrl.toUri())
                    ?.takeIf(File::exists)
                    ?: return null
            val candidates =
                dao
                    .getImportedPatternsOnce()
                    .filter { pattern ->
                        pattern.name == name &&
                            pattern.patternUrl != candidatePatternUrl &&
                            pattern.patternUrl.isNotBlank()
                    }

            return withContext(ioDispatcher) {
                val reusablePattern =
                    candidates.firstOrNull { pattern ->
                        val existingFile =
                            AppFileStorage
                                .resolveAppOwnedFile(context, pattern.patternUrl.toUri())
                                ?.takeIf(File::exists)
                        existingFile != null && filesHaveSameContent(candidateFile, existingFile)
                    }
                reusablePattern?.patternUrl
            }
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

        suspend fun deleteLocalPatternFileIfUnused(patternUrl: String) {
            if (patternUrl.isBlank()) return
            val uri = patternUrl.toUri()
            if (!AppFileStorage.isAppOwnedUri(context, uri)) return

            val savedPatternStillReferencesFile = dao.getByPatternUrl(patternUrl) != null
            val projectStillReferencesFile = counterProjectDao.countProjectsUsingPatternUri(patternUrl) > 0
            if (!savedPatternStillReferencesFile && !projectStillReferencesFile) {
                withContext(ioDispatcher) {
                    AppFileStorage.deleteUri(context, uri)
                }
            }
        }

        private suspend fun deleteUnusedLocalPatternFiles(patterns: List<SavedPatternEntity>) {
            patterns
                .map { it.patternUrl }
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { patternUrl -> deleteLocalPatternFileIfUnused(patternUrl) }
        }

        private fun String.isAppOwnedMissingFile(): Boolean {
            if (isBlank()) return false
            val file = AppFileStorage.resolveAppOwnedFile(context, toUri()) ?: return false
            return !file.exists()
        }

        private fun filesHaveSameContent(
            first: File,
            second: File,
        ): Boolean {
            if (first.length() != second.length()) return false
            first.inputStream().use { firstInput ->
                second.inputStream().use { secondInput ->
                    val firstBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    val secondBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val firstRead = firstInput.read(firstBuffer)
                        val secondRead = secondInput.read(secondBuffer)
                        if (firstRead != secondRead) return false
                        if (firstRead == -1) return true
                        for (index in 0 until firstRead) {
                            if (firstBuffer[index] != secondBuffer[index]) return false
                        }
                    }
                }
            }
        }
    }
