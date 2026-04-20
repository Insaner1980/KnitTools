package com.finnvek.knittools.ai.live

import android.annotation.SuppressLint
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class LiveVoiceState {
    IDLE,
    CONNECTING,
    ACTIVE,
    ERROR,
}

/**
 * Hallinnoi Gemini Live API -sessiota äänikomennoille.
 * startAudioConversation() hoitaa mic-kaappauksen ja äänentoiston automaattisesti.
 */
@OptIn(PublicPreviewAPI::class)
@Singleton
class VoiceLiveSession
    @Inject
    constructor(
        private val quotaManager: VoiceLiveQuotaManager,
    ) {
        private val scope = CoroutineScope(SupervisorJob())
        private var session: LiveSession? = null
        private var sessionStartTimeMs: Long = 0L
        private var timeoutJob: Job? = null

        private val _state = MutableStateFlow(LiveVoiceState.IDLE)
        val state: StateFlow<LiveVoiceState> = _state.asStateFlow()

        /** Viimeisin virheviesti — luetaan kun state == ERROR */
        var lastError: String? = null
            private set

        /**
         * Avaa Live API -session ja aloittaa audiokeskustelun.
         * [handler] kutsutaan kun Gemini tekee funktiokutsun (increment, query, jne.).
         */
        @SuppressLint("MissingPermission")
        suspend fun start(
            context: ProjectVoiceContext,
            handler: (FunctionCallPart) -> FunctionResponsePart,
        ) {
            if (_state.value == LiveVoiceState.ACTIVE) return

            _state.value = LiveVoiceState.CONNECTING
            try {
                val liveModel =
                    Firebase
                        .ai(backend = GenerativeBackend.googleAI())
                        .liveModel(
                            modelName = MODEL_NAME,
                            generationConfig =
                                liveGenerationConfig {
                                    responseModality = ResponseModality.AUDIO
                                    speechConfig = SpeechConfig(voice = Voice(VOICE_NAME))
                                },
                            systemInstruction = content { text(buildSystemInstruction(context)) },
                            tools = listOf(VoiceFunctionDeclarations.tool),
                        )

                session = liveModel.connect()
                sessionStartTimeMs = System.currentTimeMillis()
                _state.value = LiveVoiceState.ACTIVE
                resetTimeout()
                // Wrapper joka nollaa timeoutin jokaisen funktiokutsun yhteydessä
                val wrappedHandler: (FunctionCallPart) -> FunctionResponsePart = { call ->
                    resetTimeout()
                    handler(call)
                }
                session?.startAudioConversation(wrappedHandler)
            } catch (e: Exception) {
                android.util.Log.e("VoiceLiveSession", "Live API error", e)
                lastError = "${e.javaClass.simpleName}: ${e.message}"
                _state.value = LiveVoiceState.ERROR
                session = null
            }
        }

        /**
         * Pysäyttää audiokeskustelun ja sulkee session.
         * Kirjaa käytetyt minuutit kiintiöön.
         */
        suspend fun stop() {
            timeoutJob?.cancel()
            timeoutJob = null

            if (_state.value != LiveVoiceState.ACTIVE) {
                _state.value = LiveVoiceState.IDLE
                return
            }

            try {
                session?.stopAudioConversation()
            } catch (_: Exception) {
                // Ohitetaan — session voi olla jo suljettu
            }

            val elapsedMinutes = (System.currentTimeMillis() - sessionStartTimeMs) / 60_000f
            if (elapsedMinutes > 0f) {
                quotaManager.recordMinutes(elapsedMinutes)
            }

            session = null
            sessionStartTimeMs = 0L
            _state.value = LiveVoiceState.IDLE
        }

        fun isActive(): Boolean = _state.value == LiveVoiceState.ACTIVE

        /**
         * Nollaa inactivity-timeoutin. Kutsutaan automaattisesti
         * jokaisen funktiokutsun yhteydessä.
         */
        private fun resetTimeout() {
            timeoutJob?.cancel()
            timeoutJob =
                scope.launch {
                    delay(INACTIVITY_TIMEOUT_MS)
                    stop()
                }
        }

        companion object {
            private const val MODEL_NAME = "gemini-2.5-flash-native-audio-preview-12-2025"
            private const val VOICE_NAME = "Despina"
            private const val INACTIVITY_TIMEOUT_MS = 60_000L
        }
    }
