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
import com.finnvek.knittools.domain.model.SavedPatternSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.Locale
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

        fun getAll(): Flow<List<SavedPattern>> =
            dao
                .getAll()
                .map { patterns -> patterns.map { it.toDomain() } }
                .retryOnRepositoryReadFailure()

        fun getCount(): Flow<Int> = dao.getCount().retryOnRepositoryReadFailure()

        suspend fun getById(id: Long): SavedPattern? = dao.getById(id)?.toDomain()

        suspend fun getByIds(ids: List<Long>): List<SavedPattern> {
            val distinctIds = ids.distinct()
            if (distinctIds.isEmpty()) return emptyList()
            return dao.getByIds(distinctIds).map { it.toDomain() }
        }

        suspend fun getByIdIfAvailable(id: Long): SavedPattern? {
            val pattern = dao.getById(id) ?: return null
            if (pattern.patternUrl.isAppOwnedMissingFile()) {
                deleteById(id)
                return null
            }
            return pattern.toDomain()
        }

        suspend fun getByRavelryPatternId(ravelryPatternId: Int): SavedPattern? =
            dao.getByRavelryPatternId(ravelryPatternId)?.toDomain()

        suspend fun pruneMissingLocalPattern(patternUrl: String): Boolean {
            if (!patternUrl.isAppOwnedMissingFile()) return false
            dao.getByLocalPdfUri(patternUrl)?.let { pattern -> deleteById(pattern.id) }
            return true
        }

        suspend fun save(pattern: SavedPattern): Long = dao.insert(pattern.toEntity())

        suspend fun saveRavelryPatternIfMissing(pattern: SavedPattern): Long {
            if (pattern.ravelryPatternId == null && pattern.canonicalUrl.isBlank() && pattern.originalUrl.isBlank()) {
                return save(pattern)
            }
            return saveMutex.withLock {
                findDuplicateCandidate(pattern, includeTitleDesigner = false)?.id ?: dao.insert(pattern.toEntity())
            }
        }

        suspend fun findDuplicateCandidate(
            pattern: SavedPattern,
            includeTitleDesigner: Boolean,
        ): SavedPattern? {
            pattern.ravelryPatternId?.let { ravelryPatternId ->
                if (ravelryPatternId > 0) {
                    dao.getByRavelryPatternId(ravelryPatternId)?.toDomain()?.let { return it }
                }
            }

            pattern.canonicalUrl.takeIf { it.isNotBlank() }?.let { canonicalUrl ->
                dao.getByCanonicalUrl(canonicalUrl)?.toDomain()?.let { return it }
            }

            pattern.originalUrl.takeIf { it.isNotBlank() }?.let { originalUrl ->
                dao.getByOriginalUrl(originalUrl)?.toDomain()?.let { return it }
            }

            val normalizedOriginalUrl = pattern.originalUrl.normalizedOriginalUrl()
            if (normalizedOriginalUrl.isNotBlank()) {
                val originalUrlMatch =
                    dao.getAllOnce().firstOrNull { candidate ->
                        candidate.originalUrl.normalizedOriginalUrl() == normalizedOriginalUrl
                    }
                if (originalUrlMatch != null) return originalUrlMatch.toDomain()
            }

            return if (includeTitleDesigner && pattern.name.isNotBlank() && pattern.designerName.isNotBlank()) {
                dao.getByTitleAndDesignerName(pattern.name, pattern.designerName)?.toDomain()
            } else {
                null
            }
        }

        suspend fun saveImportedPatternIfMissing(
            patternUrl: String,
            name: String,
        ): Long? {
            if (!patternUrl.startsWith("content://") && !patternUrl.startsWith("file://")) return null
            return saveMutex.withLock {
                val existing = dao.getByLocalPdfUri(patternUrl)
                if (existing != null) return@withLock existing.id

                dao.insert(
                    SavedPatternEntity(
                        source = SavedPatternSource.LocalFile.persistedValue,
                        name = name,
                        designerName = context.getString(R.string.imported_pattern_designer),
                        originalUrl = patternUrl,
                        localPdfUri = patternUrl,
                        isAvailableOffline = true,
                    ),
                )
            }
        }

        suspend fun findReusableImportedPatternUrl(
            candidatePatternUrl: String,
            name: String,
        ): String? {
            val candidateFile =
                withContext(ioDispatcher) {
                    AppFileStorage
                        .resolveAppOwnedFile(context, candidatePatternUrl.toUri())
                        ?.takeIf(File::exists)
                }
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
            val isAppOwned = withContext(ioDispatcher) { AppFileStorage.isAppOwnedUri(context, uri) }
            if (!isAppOwned) return

            val savedPatternStillReferencesFile = dao.getByLocalPdfUri(patternUrl) != null
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

        private suspend fun String.isAppOwnedMissingFile(): Boolean {
            if (isBlank()) return false
            return withContext(ioDispatcher) {
                val file = AppFileStorage.resolveAppOwnedFile(context, toUri()) ?: return@withContext false
                !file.exists()
            }
        }

        private fun String.normalizedOriginalUrl(): String =
            normalizedRavelryPatternUrl()
                ?: trim()
                    .removeSuffix("/")
                    .lowercase(Locale.US)

        private fun String.normalizedRavelryPatternUrl(): String? {
            val uri = runCatching { URI(trim()) }.getOrNull() ?: return null
            val host = uri.host?.lowercase(Locale.US) ?: return null
            if (host !in RAVELRY_PATTERN_HOSTS) return null

            val segments =
                uri.path
                    ?.split("/")
                    ?.filter { it.isNotBlank() }
                    ?: return null
            if (segments.size < 3 || segments[0] != "patterns" || segments[1] != "library") return null

            return segments[2]
                .trim()
                .takeIf { it.isNotBlank() }
                ?.lowercase(Locale.US)
                ?.let { patternSlug -> "$RAVELRY_PATTERN_KEY_PREFIX$patternSlug" }
        }

        private fun filesHaveSameContent(
            first: File,
            second: File,
        ): Boolean {
            if (first.length() != second.length()) return false
            return first.inputStream().use { firstInput ->
                second.inputStream().use { secondInput ->
                    streamsHaveSameContent(firstInput, secondInput)
                }
            }
        }

        private fun streamsHaveSameContent(
            firstInput: InputStream,
            secondInput: InputStream,
        ): Boolean {
            val firstBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val secondBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val firstRead = firstInput.read(firstBuffer)
                val secondRead = secondInput.read(secondBuffer)
                if (firstRead != secondRead) return false
                if (firstRead == -1) return true
                if (!buffersHaveSameContent(firstBuffer, secondBuffer, firstRead)) return false
            }
        }

        private fun buffersHaveSameContent(
            first: ByteArray,
            second: ByteArray,
            byteCount: Int,
        ): Boolean =
            (0 until byteCount).all { index ->
                first[index] == second[index]
            }

        private companion object {
            const val RAVELRY_PATTERN_KEY_PREFIX = "ravelry:"
            val RAVELRY_PATTERN_HOSTS =
                setOf(
                    "ravelry.com",
                    "www.ravelry.com",
                    "carts.ravelry.com",
                )
        }
    }
