package com.finnvek.knittools.ai.nano

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class RowMarkerSuggestion(
    val row: Int,
    val page: Int,
    val yPosition: Float,
    val confidence: Float,
)

object PatternRowDetector {
    private val explicitRowRegex =
        Regex("""\b(?:row|rows|rnd|round|rounds)\s*[:.]?\s*(\d{1,4})\b""", RegexOption.IGNORE_CASE)
    private val numberedLineRegex = Regex("""^\s*(\d{1,4})\s*[:.)-]""")

    suspend fun detectMarkers(
        bitmap: Bitmap,
        page: Int,
    ): List<RowMarkerSuggestion> {
        val visionText = recognizeText(bitmap)
        if (visionText == null) return emptyList()
        return extractMarkers(visionText, page, bitmap.height)
    }

    private suspend fun recognizeText(bitmap: Bitmap): Text? =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer
                .process(image)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                    recognizer.close()
                }.addOnFailureListener {
                    continuation.resume(null)
                    recognizer.close()
                }
        }

    internal fun extractMarkers(
        visionText: Text,
        page: Int,
        imageHeight: Int,
    ): List<RowMarkerSuggestion> {
        if (imageHeight <= 0) return emptyList()

        val suggestions = mutableListOf<RowMarkerSuggestion>()
        visionText.textBlocks
            .flatMap { block -> block.lines }
            .forEach { line ->
                val content = line.text.trim()
                val match = explicitRowRegex.find(content) ?: numberedLineRegex.find(content) ?: return@forEach
                val row = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@forEach
                val box = line.boundingBox ?: return@forEach
                val yCenter = ((box.top + box.bottom) / 2f / imageHeight.toFloat()).coerceIn(0f, 1f)
                val confidence = if (content.contains(explicitRowRegex)) 0.92f else 0.74f
                suggestions +=
                    RowMarkerSuggestion(
                        row = row,
                        page = page,
                        yPosition = yCenter,
                        confidence = confidence,
                    )
            }

        return suggestions
            .groupBy { it.row to it.page }
            .mapNotNull { (_, matches) -> matches.maxByOrNull { it.confidence } }
            .sortedBy { it.row }
    }
}
