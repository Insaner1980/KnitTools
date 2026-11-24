package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.distinctSqliteQueryChunks
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.PhotoAvailability
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.takeWithoutSplittingSurrogatePair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressPhotoRepository
    @Inject
    constructor(
        private val dao: ProgressPhotoDao,
        private val storage: ProgressPhotoStorage,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        fun observeLatestPhotoUris(): Flow<Map<Long, String>> =
            dao
                .observeLatestPhotosPerProject()
                .map { photos -> availablePhotos(photos).associate { it.projectId to it.photoUri } }
                .retryOnRepositoryReadFailure()

        fun getAllPhotos(): Flow<List<ProgressPhoto>> =
            dao
                .getAllPhotos()
                .map { photos -> availablePhotos(photos) }
                .retryOnRepositoryReadFailure()

        fun getAllPhotoCount(): Flow<Int> =
            flow {
                pruneUnavailablePhotos(dao.getAllPhotosOnce())
                emitAll(dao.getAllPhotoCount())
            }.retryOnRepositoryReadFailure()

        fun getPhotosForProject(projectId: Long): Flow<List<ProgressPhoto>> =
            dao
                .getPhotosForProject(projectId)
                .map { photos -> availablePhotos(photos) }
                .retryOnRepositoryReadFailure()

        fun getLatestPhotos(projectId: Long): Flow<List<ProgressPhoto>> =
            dao
                .getLatestPhotos(projectId)
                .map { photos -> availablePhotos(photos) }
                .retryOnRepositoryReadFailure()

        fun getPhotoCount(projectId: Long): Flow<Int> = dao.getPhotoCount(projectId).retryOnRepositoryReadFailure()

        suspend fun createPhotoCaptureTarget(projectId: Long): Pair<File, Uri> {
            var unclaimedFile: File? = null
            try {
                val target =
                    withContext(ioDispatcher) {
                        storage.createPhotoFile(context, projectId).also { unclaimedFile = it.first }
                    }
                unclaimedFile = null
                return target
            } finally {
                unclaimedFile?.let { file ->
                    withContext(ioDispatcher + NonCancellable) {
                        storage.deletePendingPhotoFile(file.absolutePath)
                    }
                }
            }
        }

        suspend fun getPhotoCountsByProjectIds(projectIds: List<Long>): Map<Long, Int> =
            projectIds
                .distinctSqliteQueryChunks()
                .flatMap { chunk -> dao.getPhotoCountsByProjectIds(chunk) }
                .associate { it.projectId to it.count }

        suspend fun savePhoto(
            projectId: Long,
            sourceUri: Uri,
            rowNumber: Int,
            note: String? = null,
        ): Long =
            withContext(ioDispatcher) {
                val (targetUri, saved) =
                    try {
                        val (file, _) = storage.createPhotoFile(context, projectId)
                        val uri = Uri.fromFile(file).toString()
                        try {
                            uri to storage.compressAndSave(context, sourceUri, file)
                        } catch (throwable: IOException) {
                            runCatching { storage.deletePhoto(context, projectId, uri) }
                            throw throwable
                        }
                    } finally {
                        storage.deleteTemporarySource(context, sourceUri)
                    }
                if (!saved) {
                    runCatching { storage.deletePhoto(context, projectId, targetUri) }
                    return@withContext 0L
                }
                runCatching {
                    dao.insert(
                        ProgressPhotoEntity(
                            projectId = projectId,
                            photoUri = targetUri,
                            rowNumber = rowNumber,
                            note = note?.takeWithoutSplittingSurrogatePair(100),
                        ),
                    )
                }.fold(
                    onSuccess = { it },
                    onFailure = { failure ->
                        withContext(NonCancellable) {
                            val mayBePublished =
                                runCatching { dao.getAllPhotosOnce().any { it.photoUri == targetUri } }
                                    .getOrDefault(true)
                            if (!mayBePublished) {
                                runCatching { storage.deletePhoto(context, projectId, targetUri) }
                            }
                        }
                        throw failure
                    },
                )
            }

        suspend fun updatePhotoNote(
            id: Long,
            note: String?,
        ) {
            dao.updateNote(id, note?.takeWithoutSplittingSurrogatePair(100)?.ifBlank { null })
        }

        suspend fun deletePhoto(photo: ProgressPhoto) {
            val stored = dao.getByIds(listOf(photo.id)).singleOrNull() ?: return
            if (stored.projectId != photo.projectId) return
            dao.delete(stored.id)
            cleanupDeletedPhotos(listOf(stored))
        }

        suspend fun deletePhotos(ids: List<Long>) {
            val idChunks = ids.distinctSqliteQueryChunks()
            if (idChunks.isEmpty()) return
            val photos = idChunks.flatMap { chunk -> dao.getByIds(chunk) }
            if (photos.isEmpty()) return
            transactionRunner.run {
                photos
                    .map { it.id }
                    .distinctSqliteQueryChunks()
                    .forEach { chunk -> dao.deleteByIds(chunk) }
            }
            cleanupDeletedPhotos(photos)
        }

        suspend fun deletePendingPhotoFile(filePath: String?) {
            withContext(ioDispatcher) {
                storage.deletePendingPhotoFile(filePath)
            }
        }

        private suspend fun cleanupDeletedPhotos(photos: List<ProgressPhotoEntity>) {
            withContext(ioDispatcher + NonCancellable) {
                photos.forEach { photo ->
                    runCatching {
                        storage.deletePhoto(context, photo.projectId, photo.photoUri)
                    }
                }
            }
        }

        private suspend fun availablePhotos(photos: List<ProgressPhotoEntity>): List<ProgressPhoto> =
            withContext(ioDispatcher) {
                photos.mapNotNull { photo ->
                    if (photo.isAvailableOrDelete()) {
                        photo.toDomain()
                    } else {
                        null
                    }
                }
            }

        private suspend fun pruneUnavailablePhotos(photos: List<ProgressPhotoEntity>) =
            withContext(ioDispatcher) {
                photos.forEach { photo -> photo.isAvailableOrDelete() }
            }

        // Rivi poistetaan vain, kun sovelluksen oma tiedosto puuttuu varmasti. Ohimenevästi
        // lukukelvoton ulkoinen kuva piilotetaan, jotta se palaa näkyviin luvan palattua.
        private suspend fun ProgressPhotoEntity.isAvailableOrDelete(): Boolean =
            when (storage.photoAvailability(context, photoUri)) {
                PhotoAvailability.Available -> {
                    true
                }

                PhotoAvailability.Unreachable -> {
                    false
                }

                PhotoAvailability.Missing -> {
                    dao.delete(id)
                    false
                }
            }
    }
