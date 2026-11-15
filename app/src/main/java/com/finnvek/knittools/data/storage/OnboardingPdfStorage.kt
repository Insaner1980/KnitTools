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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OnboardingPdfStorage
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val documents: PatternDocumentStorage,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun copy(source: String): Pair<String, String>? {
            var undeliveredUri: String? = null
            try {
                val result =
                    withContext(ioDispatcher) {
                        val uri = source.toUri()
                        if (uri.scheme != "content") return@withContext null
                        val resolver = context.contentResolver
                        val acquiredGrant =
                            runCatching {
                                val hadGrant =
                                    resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
                                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                !hadGrant
                            }.getOrDefault(false)
                        try {
                            val name = readDisplayName(uri)
                            val copied = documents.copyPdfToInternal(context, 0L, uri, name) ?: return@withContext null
                            undeliveredUri = copied
                            copied to name.trim().takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
                        } finally {
                            if (acquiredGrant) {
                                runCatching {
                                    resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            }
                        }
                    }
                undeliveredUri = null
                return result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return null
            } finally {
                undeliveredUri?.let { uri ->
                    withContext(NonCancellable + ioDispatcher) { documents.deleteGeneratedPdf(context, uri) }
                }
            }
        }

        private fun readDisplayName(uri: Uri): String = context.contentResolver.displayNameOrNull(uri) ?: "pattern.pdf"
    }
