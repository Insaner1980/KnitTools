package com.finnvek.knittools.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.coroutines.resume

class PatternTextExtractor
    @Inject
    constructor() {
        private val cacheMutex = Mutex()
        private val pageTextCache = mutableMapOf<PageCacheKey, String>()
        private var recognizerOverride: (suspend (Bitmap) -> String)? = null

        internal constructor(
            recognizer: suspend (Bitmap) -> String,
        ) : this() {
            recognizerOverride = recognizer
        }

        suspend fun getPageText(
            patternUri: String,
            pageIndex: Int,
            bitmap: Bitmap,
        ): String {
            val key = PageCacheKey(patternUri = patternUri, pageIndex = pageIndex)
            cacheMutex.withLock {
                pageTextCache[key]?.let { return it }
            }

            val extractedText = (recognizerOverride ?: ::recognizeText)(bitmap)
            cacheMutex.withLock {
                return pageTextCache.getOrPut(key) { extractedText }
            }
        }

        suspend fun clearPattern(patternUri: String?) {
            if (patternUri == null) {
                cacheMutex.withLock { pageTextCache.clear() }
                return
            }
            cacheMutex.withLock {
                pageTextCache.keys.removeAll { it.patternUri == patternUri }
            }
        }

        private suspend fun recognizeText(bitmap: Bitmap): String =
            suspendCancellableCoroutine { continuation ->
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer
                    .process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.toPreservedText())
                        recognizer.close()
                    }.addOnFailureListener {
                        continuation.resume("")
                        recognizer.close()
                    }
            }

        private fun Text.toPreservedText(): String =
            textBlocks
                .flatMap { block -> block.lines }
                .joinToString(separator = "\n") { line -> line.text.trim() }
                .ifBlank { text.trim() }

        private data class PageCacheKey(
            val patternUri: String,
            val pageIndex: Int,
        )
    }
