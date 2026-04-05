package com.finnvek.knittools.ai.ocr

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.finnvek.knittools.ai.nano.YarnLabelNanoParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object YarnLabelScanner {
    suspend fun analyzeImage(
        context: Context,
        imageUri: Uri,
    ): ParsedYarnLabel {
        // 1. ML Kit OCR — tunnistaa tekstin kuvasta
        val ocrText = recognizeText(context, imageUri)
        if (ocrText.isBlank()) return ParsedYarnLabel()

        // 2. Kokeile Gemini Nanoa ensin (toimii millä tahansa kielellä)
        val nanoResult = YarnLabelNanoParser.parse(ocrText)
        if (nanoResult != null) return nanoResult

        // 3. Fallback regex-parseriin (toimii vain englanniksi)
        return YarnLabelParser.parse(ocrText)
    }

    private suspend fun recognizeText(
        context: Context,
        imageUri: Uri,
    ): String =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer
                .process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }.addOnFailureListener {
                    continuation.resume("")
                }
        }

    fun createImageFile(context: Context): Pair<File, Uri> {
        val dir = File(context.filesDir, "yarn_photos")
        dir.mkdirs()
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }
}
