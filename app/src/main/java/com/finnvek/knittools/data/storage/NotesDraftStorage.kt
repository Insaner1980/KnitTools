package com.finnvek.knittools.data.storage

import android.content.Context
import android.util.AtomicFile
import com.finnvek.knittools.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@Serializable
internal data class NotesDraft(
    val notes: String,
    val baseNotes: String?,
)

class NotesDraftStorage
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        internal suspend fun read(
            projectId: Long,
            id: UUID,
        ): NotesDraft? =
            withContext(ioDispatcher) {
                try {
                    Json.decodeFromString<NotesDraft>(file(projectId, id).readFully().decodeToString())
                } catch (_: IOException) {
                    null
                } catch (_: SerializationException) {
                    null
                }
            }

        internal suspend fun write(
            projectId: Long,
            id: UUID,
            draft: NotesDraft,
        ) {
            withContext(ioDispatcher) {
                val file = file(projectId, id)
                val output =
                    try {
                        file.startWrite()
                    } catch (_: IOException) {
                        return@withContext
                    }
                try {
                    output.write(Json.encodeToString(draft).encodeToByteArray())
                    file.finishWrite(output)
                } catch (_: IOException) {
                    file.failWrite(output)
                }
            }
        }

        internal suspend fun delete(
            projectId: Long,
            id: UUID,
        ) {
            withContext(ioDispatcher) { file(projectId, id).delete() }
        }

        private fun file(
            projectId: Long,
            id: UUID,
        ): AtomicFile = AtomicFile(File(context.noBackupFilesDir, "notes-draft-$projectId-$id.json"))
    }
