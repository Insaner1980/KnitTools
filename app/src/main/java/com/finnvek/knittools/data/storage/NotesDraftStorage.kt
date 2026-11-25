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
        ): Boolean =
            withContext(ioDispatcher) {
                val file = file(projectId, id)
                val output =
                    try {
                        file.startWrite()
                    } catch (_: IOException) {
                        return@withContext false
                    }
                try {
                    output.write(Json.encodeToString(draft).encodeToByteArray())
                    file.finishWrite(output)
                    true
                } catch (_: IOException) {
                    file.failWrite(output)
                    false
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
        ): AtomicFile = AtomicFile(File(context.noBackupFilesDir, "${projectPrefix(projectId)}$id.json"))

        companion object {
            private const val DRAFT_PREFIX = "notes-draft-"

            private fun projectPrefix(projectId: Long): String = "$DRAFT_PREFIX$projectId-"

            /**
             * Poistaa projektin kaikki luonnokset. Kutsutaan vasta projektin tietokantapoiston jälkeen;
             * AtomicFile voi jättää .new- tai .bak-tiedoston, joten etuliite kattaa ne myös.
             */
            fun deleteProjectDrafts(
                context: Context,
                projectId: Long,
            ) = deleteDraftsWithPrefix(context, projectPrefix(projectId))

            /** Palautus korvaa kaikki projektit, joten mikään aiempi luonnos ei enää kuulu niihin. */
            fun deleteAllDrafts(context: Context) = deleteDraftsWithPrefix(context, DRAFT_PREFIX)

            private fun deleteDraftsWithPrefix(
                context: Context,
                prefix: String,
            ) {
                context.noBackupFilesDir
                    .listFiles { file -> file.isFile && file.name.startsWith(prefix) }
                    ?.forEach(File::delete)
            }
        }
    }
