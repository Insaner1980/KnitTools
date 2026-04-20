package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * TextToSpeech-wrapper joka koordinoi SpeechRecognizerin
 * kanssa estääkseen audiopalautesilmukan.
 */
class VoiceResponseManager(
    context: Context,
    private val voiceCommandHandler: VoiceCommandHandler,
) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts =
            TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isReady = true
                    val locale = Locale.getDefault()
                    val result = tts?.setLanguage(locale)
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                    tts?.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                            }

                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                                // SpeechRecognizer vaatii main-threadin — TTS-callback tulee taustasäikeestä
                                mainHandler.post { voiceCommandHandler.resumeListening() }
                            }

                            @Suppress("kotlin:S1133") // Pakollinen UtteranceProgressListener-rajapinnan metodi
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                                mainHandler.post { voiceCommandHandler.resumeListening() }
                            }
                        },
                    )
                }
            }
    }

    fun speak(text: String) {
        if (!isReady) return
        // Max 1 utterance — uusi korvaa edellisen
        tts?.stop()
        voiceCommandHandler.pauseListening()
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
