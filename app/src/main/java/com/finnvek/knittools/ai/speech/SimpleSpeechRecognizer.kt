package com.finnvek.knittools.ai.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechError {
    data object Permission : SpeechError()

    data object Network : SpeechError()

    data object NoMatch : SpeechError()

    data object Unavailable : SpeechError()

    data object Generic : SpeechError()
}

/**
 * Kevyt yleiskäyttöinen SpeechRecognizer-wrapper raakatranskriptiota varten.
 * Ei komentojen tunnistusta — palauttaa sellaisenaan puheen tekstinä.
 */
class SimpleSpeechRecognizer(
    private val context: Context,
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val finalText: SharedFlow<String> = _finalText.asSharedFlow()

    private val _error = MutableSharedFlow<SpeechError>(extraBufferCapacity = 1)
    val error: SharedFlow<SpeechError> = _error.asSharedFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (!isAvailable()) {
            _error.tryEmit(SpeechError.Unavailable)
            return
        }
        if (_isListening.value) return

        destroyRecognizer()
        _partialText.value = ""
        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(Listener())
            }

        _isListening.value = true
        speechRecognizer?.startListening(createIntent())
    }

    fun stop() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun destroy() {
        destroyRecognizer()
        _isListening.value = false
        _partialText.value = ""
    }

    private fun createIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private inner class Listener : RecognitionListener {
        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val match =
                results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
            if (match.isNotEmpty()) {
                _partialText.value = match
                _finalText.tryEmit(match)
            } else {
                _error.tryEmit(SpeechError.NoMatch)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text =
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
            if (text.isNotEmpty()) {
                _partialText.value = text
            }
        }

        override fun onError(error: Int) {
            _isListening.value = false
            val mapped =
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.Permission

                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    -> SpeechError.Network

                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> SpeechError.NoMatch

                    else -> SpeechError.Generic
                }
            _error.tryEmit(mapped)
        }

        // Pakolliset, ei käsittelyä
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onEvent(
            eventType: Int,
            params: Bundle?,
        ) = Unit
    }
}
