package com.finnvek.knittools.repository

import android.content.Context
import androidx.core.net.toUri
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDocumentFileAvailability
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun isAvailable(localPdfUri: String): Boolean =
            withContext(ioDispatcher) {
                AppFileStorage
                    .resolveAppOwnedFile(context, localPdfUri.toUri())
                    ?.isFile == true
            }
    }
