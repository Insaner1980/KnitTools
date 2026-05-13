package com.finnvek.knittools.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.net.toUri
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.ai.ParsedYarnLabel
import com.finnvek.knittools.ai.YarnLabelGeminiScanner
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.YarnLabelPhotoStorage
import com.finnvek.knittools.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YarnLabelScanRepository
    @Inject
    constructor(
        private val geminiAiService: GeminiAiService,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun createScanPhotoUri(): Uri = YarnLabelPhotoStorage.createImageFile(context).second

        suspend fun scanLabel(photoUri: Uri): ParsedYarnLabel? {
            val bitmap =
                withContext(ioDispatcher) {
                    loadBitmapFromUri(photoUri)
                } ?: return null
            return YarnLabelGeminiScanner.scan(geminiAiService, bitmap)
        }

        fun deleteScanPhoto(uriString: String) {
            if (uriString.isBlank()) return
            try {
                AppFileStorage.deleteUri(context, uriString.toUri())
            } catch (_: Exception) {
                // Tiedostoa ei löydy tai oikeutta ei enää ole.
            }
        }

        @Suppress("TooGenericExceptionCaught")
        private fun loadBitmapFromUri(uri: Uri): Bitmap? =
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(2)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (_: Exception) {
                null
            }
    }
