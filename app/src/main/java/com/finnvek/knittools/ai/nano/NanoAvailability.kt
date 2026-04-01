package com.finnvek.knittools.ai.nano

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation

enum class NanoStatus {
    AVAILABLE,
    DOWNLOADING,
    UNAVAILABLE,
}

object NanoAvailability {
    @Suppress("TooGenericExceptionCaught")
    suspend fun check(): NanoStatus =
        try {
            val model = Generation.getClient()
            val status = model.checkStatus()
            model.close()
            when (status) {
                FeatureStatus.AVAILABLE -> NanoStatus.AVAILABLE
                FeatureStatus.DOWNLOADING, FeatureStatus.DOWNLOADABLE -> NanoStatus.DOWNLOADING
                else -> NanoStatus.UNAVAILABLE
            }
        } catch (_: Exception) {
            NanoStatus.UNAVAILABLE
        }
}
