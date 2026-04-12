package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    ) {
        fun getAllPhotos(): Flow<List<ProgressPhotoEntity>> = dao.getAllPhotos()

        fun getAllPhotoCount(): Flow<Int> = dao.getAllPhotoCount()

        fun getPhotosForProject(projectId: Long): Flow<List<ProgressPhotoEntity>> = dao.getPhotosForProject(projectId)

        fun getLatestPhotos(projectId: Long): Flow<List<ProgressPhotoEntity>> = dao.getLatestPhotos(projectId)

        fun getPhotoCount(projectId: Long): Flow<Int> = dao.getPhotoCount(projectId)

        suspend fun savePhoto(
            projectId: Long,
            sourceUri: Uri,
            rowNumber: Int,
            note: String? = null,
        ): Long =
            withContext(Dispatchers.IO) {
                val (file, _) = storage.createPhotoFile(context, projectId)
                storage.compressAndSave(context, sourceUri, file)
                dao.insert(
                    ProgressPhotoEntity(
                        projectId = projectId,
                        photoUri = Uri.fromFile(file).toString(),
                        rowNumber = rowNumber,
                        note = note?.take(100),
                    ),
                )
            }

        suspend fun updatePhotoNote(
            id: Long,
            note: String?,
        ) {
            dao.updateNote(id, note?.take(100)?.ifBlank { null })
        }

        suspend fun deletePhoto(photo: ProgressPhotoEntity) {
            withContext(Dispatchers.IO) {
                storage.deletePhoto(photo.photoUri)
            }
            dao.delete(photo.id)
        }

        suspend fun deletePhotos(ids: List<Long>) {
            val photos = dao.getByIds(ids)
            withContext(Dispatchers.IO) {
                photos.forEach { storage.deletePhoto(it.photoUri) }
            }
            dao.deleteByIds(ids)
        }

        fun deleteAllPhotosForProject(projectId: Long) {
            storage.deleteProjectPhotos(context, projectId)
        }
    }
