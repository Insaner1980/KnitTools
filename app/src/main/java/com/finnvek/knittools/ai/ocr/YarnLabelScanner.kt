package com.finnvek.knittools.ai.ocr

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
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
    ): ParsedYarnLabel =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer
                .process(image)
                .addOnSuccessListener { visionText ->
                    val parsed = YarnLabelParser.parse(visionText.text)
                    continuation.resume(parsed)
                }.addOnFailureListener {
                    continuation.resume(ParsedYarnLabel())
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
