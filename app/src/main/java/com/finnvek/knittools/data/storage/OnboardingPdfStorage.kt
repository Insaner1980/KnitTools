package com.finnvek.knittools.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.PROJECT_DOCUMENT_LABEL_MAX_LENGTH
import com.finnvek.knittools.domain.model.takeWithoutSplittingSurrogatePair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OnboardingPdfStorage
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val documents: PatternDocumentStorage,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun copy(source: String): Pair<String, String>? =
            withContext(ioDispatcher) {
                val uri = source.toUri()
                if (uri.scheme != "content") return@withContext null
                val resolver = context.contentResolver
                var acquiredGrant = false
                try {
                    val hadGrant = resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
                    try {
                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        acquiredGrant = !hadGrant
                    } catch (_: SecurityException) {
                        // A temporary read grant is sufficient for the immediate app-owned copy.
                    }
                    val name = readDisplayName(uri)
                    val copied = documents.copyPdfToInternal(context, 0L, uri, name) ?: return@withContext null
                    copied to name.trim().takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                } finally {
                    if (acquiredGrant) {
                        try {
                            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (_: SecurityException) {
                            // The app-owned copy no longer depends on the provider grant.
                        }
                    }
                }
            }

        private fun readDisplayName(uri: Uri): String = context.contentResolver.displayNameOrNull(uri) ?: "pattern.pdf"
    }
