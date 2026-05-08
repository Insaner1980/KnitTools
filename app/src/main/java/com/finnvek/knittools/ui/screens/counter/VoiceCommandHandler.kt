package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

sealed class VoiceCommand {
    data class Increment(
        val count: Int = 1,
    ) : VoiceCommand()

    data class Decrement(
        val count: Int = 1,
    ) : VoiceCommand()

    data object StitchIncrement : VoiceCommand()

    data object StitchDecrement : VoiceCommand()

    data object Undo : VoiceCommand()

    data object Reset : VoiceCommand()

    data object StopListening : VoiceCommand()

    data object Help : VoiceCommand()
}

/** Äänivirheen tyyppi UI-kerrokselle */
sealed class VoiceError {
    data object Timeout : VoiceError()

    data object Fatal : VoiceError()

    data object NetworkLost : VoiceError()
}

/**
 * Android SpeechRecognizer -wrapper äänikomennoille.
 * Tukee sekä one-shot- että jatkuvaa kuuntelutilaa.
 * Peruskomennot tunnistetaan paikallisesti useilla sovelluksen kielillä.
 */
class VoiceCommandHandler(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isContinuousMode = MutableStateFlow(false)
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    private val _recognizedCommand = MutableSharedFlow<VoiceCommand>(extraBufferCapacity = 1)
    val recognizedCommand: SharedFlow<VoiceCommand> = _recognizedCommand.asSharedFlow()

    private val _unrecognizedText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val unrecognizedText: SharedFlow<String> = _unrecognizedText.asSharedFlow()

    private val _voiceError = MutableSharedFlow<VoiceError>(extraBufferCapacity = 1)
    val voiceError: SharedFlow<VoiceError> = _voiceError.asSharedFlow()

    private var lastCommandTimestamp: Long = 0L
    private var timeoutJob: Job? = null
    private var restartJob: Job? = null
    private var networkLostNotified = false

    // === One-shot (nykyinen toiminta) ===

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        if (_isListening.value) return

        destroyRecognizer()
        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(CommandListener())
            }

        _isListening.value = true
        speechRecognizer?.startListening(createRecognizerIntent())
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    // === Jatkuva kuuntelutila ===

    fun startContinuousListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        _isContinuousMode.value = true
        lastCommandTimestamp = SystemClock.elapsedRealtime()
        networkLostNotified = false
        startTimeoutWatcher()
        startListening()
    }

    fun stopContinuousListening() {
        _isContinuousMode.value = false
        timeoutJob?.cancel()
        restartJob?.cancel()
        stopListening()
        destroyRecognizer()
    }

    /** TTS-koordinaatio: pysäytä kuuntelu puheen ajaksi, älä muuta continuous-tilaa */
    fun pauseListening() {
        restartJob?.cancel()
        speechRecognizer?.stopListening()
        destroyRecognizer()
        _isListening.value = false
    }

    /** TTS-koordinaatio: jatka kuuntelua TTS:n jälkeen (vain jatkuvassa tilassa) */
    fun resumeListening() {
        if (_isContinuousMode.value) {
            startListening()
        }
    }

    fun destroy() {
        timeoutJob?.cancel()
        restartJob?.cancel()
        destroyRecognizer()
        _isListening.value = false
        _isContinuousMode.value = false
    }

    // === Sisäiset metodit ===

    private fun createRecognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun startTimeoutWatcher() {
        timeoutJob?.cancel()
        timeoutJob =
            scope.launch {
                while (isActive && _isContinuousMode.value) {
                    delay(TIMEOUT_CHECK_INTERVAL_MS)
                    val elapsed = SystemClock.elapsedRealtime() - lastCommandTimestamp
                    if (elapsed > INACTIVITY_TIMEOUT_MS) {
                        stopContinuousListening()
                        _voiceError.tryEmit(VoiceError.Timeout)
                        return@launch
                    }
                }
            }
    }

    private fun restartListeningAfterDelay() {
        restartJob?.cancel()
        restartJob =
            scope.launch {
                delay(RESTART_DELAY_MS)
                if (_isContinuousMode.value) {
                    startListening()
                }
            }
    }

    private fun parseCommand(text: String): VoiceCommand? = VoiceCommandParser.parse(text)

    private fun handleKeywordMatch(command: VoiceCommand) {
        lastCommandTimestamp = SystemClock.elapsedRealtime()
        if (command is VoiceCommand.StopListening) {
            stopContinuousListening()
        }
        _recognizedCommand.tryEmit(command)
        if (_isContinuousMode.value) {
            restartListeningAfterDelay()
        }
    }

    private fun handleUnrecognizedText(matches: List<String>) {
        lastCommandTimestamp = SystemClock.elapsedRealtime()
        matches.firstOrNull()?.let { _unrecognizedText.tryEmit(it) }
        restartListeningAfterDelay()
    }

    private inner class CommandListener : RecognitionListener {
        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches == null) {
                if (_isContinuousMode.value) restartListeningAfterDelay()
                return
            }
            val command = matches.firstNotNullOfOrNull { parseCommand(it) }
            if (command != null) {
                handleKeywordMatch(command)
            } else if (_isContinuousMode.value) {
                handleUnrecognizedText(matches)
            }
        }

        override fun onError(error: Int) {
            _isListening.value = false
            if (!_isContinuousMode.value) return

            when (error) {
                // Verkkovirheet — ilmoita kerran, jatka keyword-tilassa
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                -> {
                    if (!networkLostNotified) {
                        networkLostNotified = true
                        _voiceError.tryEmit(VoiceError.NetworkLost)
                    }
                    restartListeningAfterDelay()
                }

                // Oikeasti fataalit — permission puuttuu
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    stopContinuousListening()
                    _voiceError.tryEmit(VoiceError.Fatal)
                }

                // Kaikki muut (NO_MATCH, SPEECH_TIMEOUT, RECOGNIZER_BUSY,
                // ERROR_CLIENT, ERROR_SERVER, ERROR_AUDIO jne.) — yritä uudelleen
                else -> {
                    restartListeningAfterDelay()
                }
            }
        }

        // RecognitionListener-rajapinnan pakolliset metodit — käsittely ei tarpeen
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEvent(
            eventType: Int,
            params: Bundle?,
        ) = Unit
    }

    companion object {
        private const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L // 5 minuuttia
        private const val TIMEOUT_CHECK_INTERVAL_MS = 30_000L // tarkista 30s välein
        private const val RESTART_DELAY_MS = 500L // uudelleenkäynnistysviive
    }
}
