package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.ProgressPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    ) {
        fun getAllPhotos(): Flow<List<ProgressPhoto>> =
            dao.getAllPhotos().map { photos ->
                photos.map { it.toDomain() }
            }

        fun getAllPhotoCount(): Flow<Int> = dao.getAllPhotoCount()

        fun getPhotosForProject(projectId: Long): Flow<List<ProgressPhoto>> =
            dao.getPhotosForProject(projectId).map { photos -> photos.map { it.toDomain() } }

        fun getLatestPhotos(projectId: Long): Flow<List<ProgressPhoto>> =
            dao.getLatestPhotos(projectId).map { photos -> photos.map { it.toDomain() } }

        fun getPhotoCount(projectId: Long): Flow<Int> = dao.getPhotoCount(projectId)

        suspend fun getPhotoCountsByProjectIds(projectIds: List<Long>): Map<Long, Int> {
            val distinctProjectIds = projectIds.distinct()
            if (distinctProjectIds.isEmpty()) return emptyMap()
            return dao
                .getPhotoCountsByProjectIds(distinctProjectIds)
                .associate { it.projectId to it.count }
        }

        suspend fun savePhoto(
            projectId: Long,
            sourceUri: Uri,
            rowNumber: Int,
            note: String? = null,
        ): Long =
            withContext(ioDispatcher) {
                val (file, _) = storage.createPhotoFile(context, projectId)
                val targetUri = Uri.fromFile(file).toString()
                val saved =
                    try {
                        storage.compressAndSave(context, sourceUri, file)
                    } finally {
                        storage.deleteTemporarySource(context, sourceUri)
                    }
                if (!saved) {
                    storage.deletePhoto(targetUri)
                    return@withContext 0L
                }
                runCatching {
                    dao.insert(
                        ProgressPhotoEntity(
                            projectId = projectId,
                            photoUri = targetUri,
                            rowNumber = rowNumber,
                            note = note?.take(100),
                        ),
                    )
                }.onFailure {
                    storage.deletePhoto(targetUri)
                }.getOrThrow()
            }

        suspend fun updatePhotoNote(
            id: Long,
            note: String?,
        ) {
            dao.updateNote(id, note?.take(100)?.ifBlank { null })
        }

        suspend fun deletePhoto(photo: ProgressPhoto) {
            dao.delete(photo.id)
            withContext(ioDispatcher) {
                storage.deletePhoto(photo.photoUri)
            }
        }

        suspend fun deletePhotos(ids: List<Long>) {
            val photos = dao.getByIds(ids)
            dao.deleteByIds(ids)
            withContext(ioDispatcher) {
                photos.forEach { storage.deletePhoto(it.photoUri) }
            }
        }

        fun deleteAllPhotosForProject(projectId: Long) {
            storage.deleteProjectPhotos(context, projectId)
        }
    }
