package com.finnvek.knittools.ai.live

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
class VoiceLiveSession internal constructor(
    private val quotaManager: VoiceLiveQuotaManager,
    private val connector: VoiceLiveConnector,
    private val clock: () -> Long,
    private val scope: CoroutineScope,
    private val hasRecordAudioPermission: () -> Boolean,
) {
    @Inject
    constructor(
        quotaManager: VoiceLiveQuotaManager,
        @ApplicationContext context: Context,
    ) : this(
        quotaManager = quotaManager,
        connector = FirebaseVoiceLiveConnector(),
        clock = System::currentTimeMillis,
        scope = CoroutineScope(SupervisorJob()),
        hasRecordAudioPermission = {
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        },
    )

    private val lifecycleMutex = Mutex()
    private var session: VoiceLiveConnection? = null
    private var sessionStartTimeMs: Long = 0L
    private var timeoutJob: Job? = null
    private var quotaDeadlineJob: Job? = null

    private val _state = MutableStateFlow(LiveVoiceState.IDLE)
    val state: StateFlow<LiveVoiceState> = _state.asStateFlow()

    /** Viimeisin virheviesti — luetaan kun state == ERROR */
    var lastError: String? = null
        private set

    /**
     * Avaa Live API -session ja aloittaa audiokeskustelun.
     * [handler] kutsutaan kun Gemini tekee funktiokutsun (increment, query, jne.).
     */
    suspend fun start(
        context: ProjectVoiceContext,
        handler: (FunctionCallPart) -> FunctionResponsePart,
    ) {
        val remainingMinutes = beginConnecting() ?: return
        var terminalState = LiveVoiceState.IDLE
        try {
            val liveConnection = connector.connect(buildSystemInstruction(context))
            lifecycleMutex.withLock {
                session = liveConnection
                sessionStartTimeMs = clock()
                _state.value = LiveVoiceState.ACTIVE
                scheduleQuotaDeadlineLocked(remainingMinutes)
                resetTimeoutLocked()
            }
            val wrappedHandler: (FunctionCallPart) -> FunctionResponsePart = { call ->
                resetTimeout()
                handler(call)
            }
            liveConnection.startAudioConversation(wrappedHandler)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Live API error: ${e.javaClass.simpleName}")
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            terminalState = LiveVoiceState.ERROR
        } finally {
            withContext(NonCancellable) {
                finishSession(terminalState)
            }
        }
    }

    /**
     * Pysäyttää audiokeskustelun ja sulkee session.
     * Kirjaa käytetyt minuutit kiintiöön.
     */
    suspend fun stop() {
        finishSession(LiveVoiceState.IDLE)
    }

    fun isActive(): Boolean = _state.value == LiveVoiceState.ACTIVE

    /**
     * Nollaa inactivity-timeoutin. Kutsutaan automaattisesti
     * jokaisen funktiokutsun yhteydessä.
     */
    private fun resetTimeout() {
        scope.launch {
            lifecycleMutex.withLock {
                resetTimeoutLocked()
            }
        }
    }

    private suspend fun beginConnecting(): Float? =
        lifecycleMutex.withLock {
            if (_state.value == LiveVoiceState.CONNECTING || _state.value == LiveVoiceState.ACTIVE) {
                return@withLock null
            }
            if (!hasRecordAudioPermission()) {
                lastError = "RECORD_AUDIO permission missing"
                _state.value = LiveVoiceState.ERROR
                return@withLock null
            }
            val remainingMinutes = quotaManager.remainingMinutes()
            if (remainingMinutes <= 0f) return@withLock null
            lastError = null
            _state.value = LiveVoiceState.CONNECTING
            remainingMinutes
        }

    private fun scheduleQuotaDeadlineLocked(remainingMinutes: Float) {
        quotaDeadlineJob?.cancel()
        quotaDeadlineJob =
            scope.launch {
                delay((remainingMinutes * MILLIS_PER_MINUTE).toLong().coerceAtLeast(1L))
                stop()
            }
    }

    private fun resetTimeoutLocked() {
        if (_state.value != LiveVoiceState.ACTIVE) return
        timeoutJob?.cancel()
        timeoutJob =
            scope.launch {
                delay(INACTIVITY_TIMEOUT_MS)
                stop()
            }
    }

    private suspend fun finishSession(nextState: LiveVoiceState) {
        val currentJob = currentCoroutineContext()[Job]
        val finishedSession =
            lifecycleMutex.withLock {
                val activeSession = session
                val elapsedMs =
                    if (_state.value == LiveVoiceState.ACTIVE) {
                        (clock() - sessionStartTimeMs).coerceAtLeast(0L)
                    } else {
                        0L
                    }
                if (timeoutJob !== currentJob) timeoutJob?.cancel()
                if (quotaDeadlineJob !== currentJob) quotaDeadlineJob?.cancel()
                timeoutJob = null
                quotaDeadlineJob = null
                session = null
                sessionStartTimeMs = 0L
                _state.value = nextState
                FinishedSession(activeSession, elapsedMs)
            }

        val elapsedMinutes = finishedSession.elapsedMs / MILLIS_PER_MINUTE
        if (elapsedMinutes > 0f) {
            quotaManager.recordMinutes(elapsedMinutes)
        }

        try {
            finishedSession.connection?.stopAudioConversation()
        } catch (_: Exception) {
            // Sessio voi olla jo suljettu.
        }
    }

    companion object {
        private const val TAG = "VoiceLiveSession"
        private const val INACTIVITY_TIMEOUT_MS = 60_000L
        private const val MILLIS_PER_MINUTE = 60_000f
    }

    private data class FinishedSession(
        val connection: VoiceLiveConnection?,
        val elapsedMs: Long,
    )
}

@OptIn(PublicPreviewAPI::class)
internal fun interface VoiceLiveConnector {
    suspend fun connect(systemInstruction: String): VoiceLiveConnection
}

@OptIn(PublicPreviewAPI::class)
internal interface VoiceLiveConnection {
    suspend fun startAudioConversation(handler: (FunctionCallPart) -> FunctionResponsePart)

    suspend fun stopAudioConversation()
}

@OptIn(PublicPreviewAPI::class)
private class FirebaseVoiceLiveConnector : VoiceLiveConnector {
    override suspend fun connect(systemInstruction: String): VoiceLiveConnection {
        val liveModel =
            Firebase
                .ai(
                    backend = GenerativeBackend.googleAI(),
                    useLimitedUseAppCheckTokens = true,
                ).liveModel(
                    modelName = MODEL_NAME,
                    generationConfig =
                        liveGenerationConfig {
                            responseModality = ResponseModality.AUDIO
                            speechConfig = SpeechConfig(voice = Voice(VOICE_NAME))
                        },
                    systemInstruction = content { text(systemInstruction) },
                    tools = listOf(VoiceFunctionDeclarations.tool),
                )
        return FirebaseVoiceLiveConnection(liveModel.connect())
    }

    private companion object {
        private const val MODEL_NAME = "gemini-2.5-flash-native-audio-preview-12-2025"
        private const val VOICE_NAME = "Despina"
    }
}

@OptIn(PublicPreviewAPI::class)
private class FirebaseVoiceLiveConnection(
    private val session: LiveSession,
) : VoiceLiveConnection {
    @SuppressLint("MissingPermission") // Lupa tarkistetaan VoiceLiveSession.beginConnecting()-rajalla.
    override suspend fun startAudioConversation(handler: (FunctionCallPart) -> FunctionResponsePart) {
        session.startAudioConversation(handler)
    }

    override suspend fun stopAudioConversation() {
        session.stopAudioConversation()
    }
}
