package com.finnvek.knittools.repository

import android.content.Context
import androidx.core.net.toUri
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.PatternAvailability
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.WebPatternDesignerValidation
import com.finnvek.knittools.domain.model.WebPatternTitleValidation
import com.finnvek.knittools.domain.model.WebPatternUrl
import com.finnvek.knittools.domain.model.WebPatternUrlValidation
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.validateWebPatternDesigner
import com.finnvek.knittools.domain.model.validateWebPatternTitle
import com.finnvek.knittools.domain.model.validateWebPatternUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
@Suppress("TooManyFunctions") // Yksi repository säilyttää Saved Pattern -lähteiden yhteiset invariantit.
class SavedPatternRepository
    @Inject
    constructor(
        private val dao: SavedPatternDao,
        @param:ApplicationContext private val context: Context,
        private val counterProjectDao: CounterProjectDao,
        private val transactionRunner: DatabaseTransactionRunner,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val projectDocumentDao: ProjectDocumentDao? = null,
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

        suspend fun createWebPattern(input: WebPatternInput): WebPatternMutationResult {
            val validated = validateWebPatternInput(input) ?: return invalidWebPatternInputResult(input)
            if (validated.url.isRavelryPattern) return WebPatternMutationResult.RavelryOwnedUrl

            return persistWebPatternMutation {
                saveMutex.withLock {
                    transactionRunner.run {
                        findWebPatternDuplicate(validated.url)?.let { duplicate ->
                            return@run WebPatternMutationResult.Duplicate(duplicate.id)
                        }
                        val now = System.currentTimeMillis()
                        val patternId =
                            dao.insert(
                                SavedPatternEntity(
                                    source = SavedPatternSource.WebLink.persistedValue,
                                    ravelryPatternId = null,
                                    name = validated.title,
                                    designerName = validated.designer,
                                    thumbnailUrl = null,
                                    difficulty = null,
                                    gaugeStitches = null,
                                    gaugeRows = null,
                                    needleSize = null,
                                    yarnWeight = null,
                                    yardage = null,
                                    availability = PatternAvailability.Unknown.persistedValue,
                                    originalUrl = validated.url.originalUrl,
                                    canonicalUrl = validated.url.canonicalUrl,
                                    localPdfUri = null,
                                    isAvailableOffline = false,
                                    savedAt = now,
                                    updatedAt = now,
                                    lastSyncedAt = null,
                                ),
                            )
                        WebPatternMutationResult.Created(patternId)
                    }
                }
            }
        }

        suspend fun updateWebPattern(
            patternId: Long,
            expectedUpdatedAt: Long,
            input: WebPatternInput,
        ): WebPatternMutationResult {
            val validated = validateWebPatternInput(input) ?: return invalidWebPatternInputResult(input)
            if (validated.url.isRavelryPattern) return WebPatternMutationResult.RavelryOwnedUrl
            if (patternId <= 0L) return WebPatternMutationResult.PatternMissing

            return persistWebPatternMutation {
                saveMutex.withLock {
                    transactionRunner.run {
                        val current = dao.getById(patternId) ?: return@run WebPatternMutationResult.PatternMissing
                        if (current.updatedAt != expectedUpdatedAt) return@run WebPatternMutationResult.StaleAction
                        if (!current.toDomain().isWebPatternCompatible) {
                            return@run WebPatternMutationResult.NotEditableAsWebPattern
                        }
                        findWebPatternDuplicate(validated.url, excludingPatternId = patternId)?.let { duplicate ->
                            return@run WebPatternMutationResult.Duplicate(duplicate.id)
                        }

                        val now = System.currentTimeMillis()
                        val updatedAt = if (now > current.updatedAt) now else current.updatedAt + 1L
                        dao.update(
                            current.copy(
                                source = SavedPatternSource.WebLink.persistedValue,
                                name = validated.title,
                                designerName = validated.designer,
                                originalUrl = validated.url.originalUrl,
                                canonicalUrl = validated.url.canonicalUrl,
                                updatedAt = updatedAt,
                            ),
                        )
                        counterProjectDao.updateLinkedPatternName(patternId, validated.title, updatedAt)
                        WebPatternMutationResult.Updated(patternId)
                    }
                }
            }
        }

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

        suspend fun deleteWebPattern(id: Long): SavedPatternDeleteResult = deleteSingleWebPattern(id)

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
            val projectDocumentStillReferencesFile = projectDocumentDao?.isUriReferenced(patternUrl) == true
            if (!savedPatternStillReferencesFile &&
                !projectStillReferencesFile &&
                !projectDocumentStillReferencesFile
            ) {
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

        private suspend fun deleteSingleWebPattern(id: Long): SavedPatternDeleteResult {
            if (id <= 0L) return SavedPatternDeleteResult.PatternMissing
            val transactionResult =
                try {
                    saveMutex.withLock {
                        transactionRunner.run {
                            val pattern =
                                dao.getById(id)
                                    ?: return@run SavedPatternDeleteTransactionResult.PatternMissing
                            if (!pattern.toDomain().isWebPatternCompatible) {
                                return@run SavedPatternDeleteTransactionResult.NotWebPattern
                            }
                            counterProjectDao.clearLinkedPatternIds(listOf(id), System.currentTimeMillis())
                            dao.deleteById(id)
                            SavedPatternDeleteTransactionResult.Deleted(pattern)
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    return SavedPatternDeleteResult.PersistenceFailure
                }

            return when (transactionResult) {
                is SavedPatternDeleteTransactionResult.Deleted -> {
                    try {
                        deleteUnusedLocalPatternFiles(listOf(transactionResult.pattern))
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        // Tietokantapoisto on jo valmis; fyysinen jälkisiivous on best effort.
                    }
                    SavedPatternDeleteResult.Deleted
                }

                SavedPatternDeleteTransactionResult.PatternMissing -> SavedPatternDeleteResult.PatternMissing
                SavedPatternDeleteTransactionResult.NotWebPattern -> SavedPatternDeleteResult.NotWebPattern
            }
        }

        private suspend fun findWebPatternDuplicate(
            url: WebPatternUrl,
            excludingPatternId: Long? = null,
        ): SavedPatternEntity? {
            val exactMatch =
                if (excludingPatternId == null) {
                    dao.getByCanonicalUrl(url.canonicalUrl)
                } else {
                    dao.getByCanonicalUrlExcludingId(url.canonicalUrl, excludingPatternId)
                }
            if (exactMatch != null) return exactMatch

            return dao.getAllOnce().firstOrNull { candidate ->
                candidate.id != excludingPatternId && candidate.matchesCanonicalWebPatternUrl(url.canonicalUrl)
            }
        }

        private fun SavedPatternEntity.matchesCanonicalWebPatternUrl(canonicalUrl: String): Boolean =
            sequenceOf(originalUrl, this.canonicalUrl)
                .filter(String::isNotBlank)
                .mapNotNull { storedUrl ->
                    (validateWebPatternUrl(storedUrl) as? WebPatternUrlValidation.Valid)?.value?.canonicalUrl
                }.any { candidateCanonicalUrl -> candidateCanonicalUrl == canonicalUrl }

        private suspend fun persistWebPatternMutation(
            mutation: suspend () -> WebPatternMutationResult,
        ): WebPatternMutationResult =
            try {
                mutation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                WebPatternMutationResult.PersistenceFailure
            }

        private fun validateWebPatternInput(input: WebPatternInput): ValidatedWebPatternInput? {
            val title = (validateWebPatternTitle(input.title) as? WebPatternTitleValidation.Valid)?.value ?: return null
            val designer =
                (validateWebPatternDesigner(input.designer) as? WebPatternDesignerValidation.Valid)?.value
                    ?: return null
            val url = (validateWebPatternUrl(input.url) as? WebPatternUrlValidation.Valid)?.value ?: return null
            return ValidatedWebPatternInput(title, designer, url)
        }

        private fun invalidWebPatternInputResult(input: WebPatternInput): WebPatternMutationResult =
            when {
                validateWebPatternTitle(input.title) !is WebPatternTitleValidation.Valid ->
                    WebPatternMutationResult.InvalidTitle
                validateWebPatternDesigner(input.designer) !is WebPatternDesignerValidation.Valid ->
                    WebPatternMutationResult.InvalidDesigner
                else -> WebPatternMutationResult.InvalidUrl
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

private data class ValidatedWebPatternInput(
    val title: String,
    val designer: String,
    val url: WebPatternUrl,
)

private sealed interface SavedPatternDeleteTransactionResult {
    data class Deleted(
        val pattern: SavedPatternEntity,
    ) : SavedPatternDeleteTransactionResult

    data object PatternMissing : SavedPatternDeleteTransactionResult

    data object NotWebPattern : SavedPatternDeleteTransactionResult
}
