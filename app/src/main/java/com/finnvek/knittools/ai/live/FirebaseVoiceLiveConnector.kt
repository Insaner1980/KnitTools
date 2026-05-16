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

@OptIn(PublicPreviewAPI::class)
internal class FirebaseVoiceLiveConnector : VoiceLiveConnector {
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
