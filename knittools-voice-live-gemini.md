# KnitTools — Voice Commands v3 with Gemini 3.1 Flash Live

**Type:** Delta spec — rebuild voice layer using Live API audio-to-audio  
**Status:** Spec ready for implementation  
**Depends on:** Firebase AI Logic setup (already implemented), Voice Commands v2 (current system)  
**Pro only:** Yes  
**Key change:** Replace three-stage pipeline (SpeechRecognizer + text LLM + Android TTS) with single Live API audio-to-audio session

---

## 1. What Changes

### Current architecture (v2)
```
User speaks
    ↓
Android SpeechRecognizer → text
    ↓
Local keyword matching (exact → counted → first-word fallback)
    ↓
If unknown: Gemini 2.5 Flash Lite → text response
    ↓
Android TTS → robotic voice output
```

Three problems:
- Android TTS sounds robotic
- Text-only Gemini doesn't understand tone/emphasis
- Three systems chained together means three failure points and accumulated latency

### New architecture (v3)
```
User speaks
    ↓
Local keyword matching first (for known commands — stays instant and offline)
    ↓
If unknown: Gemini 3.1 Flash Live (audio-to-audio via WebSocket)
    ↓
Natural voice response streamed directly to speaker
```

One system for conversational queries. Local keyword matching stays because it's fast and free for common actions.

---

## 2. Two-Tier Voice System

### Tier 1: Local keyword matching (unchanged)

Fast offline commands that do NOT go to the Live API:
- "add row" / "next" / "plus one" → Increment(1)
- "undo" → undo last action
- "next 5" / "add three" → Increment(n) via number parser
- etc.

This keeps common commands instant and free. The three-layer recognition (exact → counted → first-word) stays as-is.

### Tier 2: Gemini 3.1 Flash Live (new)

When local matching returns Unknown, the audio is sent to Flash Live via WebSocket. The model:
- Understands the spoken question
- Has access to project context (current row, pattern, yarn, notes, sessions)
- Responds directly with natural voice
- Can be interrupted

Example questions that Tier 2 handles:
- "How many decrease rows do I have left?"
- "What yarn am I using?"
- "When does the next pattern repeat start?"
- "How long have I been knitting today?"
- "What was the previous row's instruction?"
- "Is this yarn right for this pattern?"

---

## 3. Live API Integration

### Connection

Live API uses WebSocket, not HTTP. The Firebase AI Logic SDK includes Live API support. Connection is established when voice mode is activated (mic toggle or long-press), and closed when voice mode is disabled or after a timeout of inactivity.

Model: `gemini-3.1-flash-live-preview`

### Session configuration

When opening a session, send the project context as the system prompt:

```
You are a knitting assistant helping with the user's current project. Answer their voice questions concisely and naturally. Keep responses brief — this is spoken dialogue, not a written explanation. Do not repeat the question back.

Current project context:
- Name: {projectName}
- Current row: {currentRow}
- Pattern: {patternName or "not attached"}
- Current page: {currentPage or "not applicable"}
- Linked yarn: {yarnBrand} {yarnName}, {weightCategory} ({metersPerSkein}m/{gramsPerSkein}g)
- Total knitting time today: {minutesToday} minutes
- Total sessions: {sessionCount}
- Active counters: {counterSummary}
- Notes: {notesShort}
- Latest row instruction: {latestInstruction or "not available"}

Respond in English. If the question is something you cannot answer from this context, say so briefly.
```

This context is refreshed when the user switches projects or when significant state changes (e.g., row advance, pattern change).

### Audio streaming

Live API is bidirectional streaming over WebSocket:
- Mic audio streams up continuously while voice mode is active
- Response audio streams down and plays through speaker
- Interruption: if user starts talking during response, old audio stops, new input is processed

### Session lifecycle

- Open session when user activates continuous listening (TopBar mic toggle) OR when one-shot (long-press +) is used and local matching fails
- Close session when user deactivates listening OR after 60 seconds of silence
- One session can handle multiple questions without reconnecting — user can have a back-and-forth conversation

---

## 4. Context Refresh Strategy

The system prompt contains project context. This needs to stay current without re-sending on every turn.

Approach:
- Build context once when session opens
- If row changes during session, send a system update (small message) rather than reconnecting
- If user switches projects, close session and reopen with new context

Keep context compact — only data that's likely relevant to voice questions. Don't dump the entire database.

---

## 5. UI Changes

Minimal. The existing voice command UI stays:
- TopBar mic toggle for continuous mode
- Long-press + for one-shot mode
- Visual indicator when actively listening
- Visual indicator when AI is responding

### New: "AI is thinking" and "AI is speaking" states

When Flash Live is processing or responding, show a subtle visual indicator (pulsing mic icon or similar) so the user knows the system is working. Do not block the UI.

### Settings

Add to Settings → a new toggle:
- **Voice response** — can be "Device voice" (Android TTS, offline, robotic) or "Natural voice" (Flash Live, requires internet, uses AI quota)
- Default: Natural voice
- If user turns off Natural voice, Flash Live is not used. Unknown commands fall back to Android TTS with a text response from regular Gemini 2.5 Flash Lite.

This respects users who:
- Want offline voice
- Want to conserve quota
- Don't need the natural voice

---

## 6. Quota and Cost

Flash Live is billed by minute of audio (both input and output):
- Input audio: $0.005/minute
- Output audio: $0.018/minute

Typical voice interaction: ~5 sec input + ~3 sec output = ~0.13 cents per interaction.

### Quota counting

Count in minutes, not interactions:
- Track `voiceMinutesThisMonth` in DataStore
- Include both input and output minutes
- Monthly allowance: e.g. 30 minutes per month for Pro users (roughly 200-300 interactions)
- When quota exhausted, fall back to Tier 1 only (local keyword matching) with a brief snackbar: "Voice quota reached. Basic commands still work."

This is a new quota dimension separate from the text-based AI call count. They don't share the same counter.

---

## 7. Fallback Behavior

| Scenario | Behavior |
|----------|----------|
| No internet | Tier 1 only. Unknown commands say "Requires internet connection" via Android TTS. |
| Voice quota exhausted | Tier 1 only. Snackbar informs user. |
| Live API connection fails | Fall back to old v2 flow (text Gemini + Android TTS) for this command. |
| User disabled "Natural voice" in Settings | Same as v2 behavior. |
| Free user | Tier 1 only. No Flash Live access. Upgrade prompt on Pro-gated voice attempts. |

---

## 8. Implementation

### 8.1 New VoiceLiveSession class

```kotlin
class VoiceLiveSession @Inject constructor(
    private val firebaseAi: FirebaseAI,
    private val voiceQuotaManager: VoiceQuotaManager,
) {
    private var session: LiveSession? = null
    
    suspend fun open(projectContext: ProjectVoiceContext) {
        val systemPrompt = buildSystemPrompt(projectContext)
        session = firebaseAi.generativeModel("gemini-3.1-flash-live-preview")
            .startLiveSession(systemInstruction = systemPrompt)
    }
    
    suspend fun sendAudio(audioData: ByteArray): Flow<LiveResponse> {
        return session?.send(audioData) ?: emptyFlow()
    }
    
    suspend fun updateContext(contextUpdate: String) {
        session?.sendText("System update: $contextUpdate")
    }
    
    suspend fun close() {
        session?.close()
        session = null
    }
}
```

(Exact SDK method names may differ — Claude Code should verify against the Firebase AI Logic SDK docs.)

### 8.2 Voice command router

The existing VoiceCommandHandler gets a new branch:

```kotlin
suspend fun handleAudio(audioData: ByteArray): VoiceResult {
    // Tier 1: try local matching on the transcribed text (from partial SpeechRecognizer result)
    val localResult = localMatcher.match(audioData)
    if (localResult != VoiceCommand.Unknown) {
        return executeLocally(localResult)
    }
    
    // Tier 2: Flash Live
    if (!voiceQuotaManager.hasQuota()) {
        return VoiceResult.QuotaExhausted
    }
    
    if (!isOnline()) {
        return VoiceResult.OfflineFallback
    }
    
    return liveSession.sendAudio(audioData)
}
```

### 8.3 VoiceQuotaManager

New quota manager specifically for voice minutes:

```kotlin
class VoiceQuotaManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun hasQuota(): Boolean
    suspend fun recordMinutes(inputMinutes: Double, outputMinutes: Double)
    suspend fun getRemainingMinutes(): Double
}
```

### 8.4 Project context builder

New utility that assembles the system prompt context from existing data sources (CounterRepository, YarnCardRepository, SessionDao, PatternViewerViewModel cache, etc.). This should be cached and only rebuilt on meaningful state changes.

---

## 9. Files to Create

| File | Purpose |
|------|---------|
| `VoiceLiveSession.kt` | Live API WebSocket session management |
| `VoiceQuotaManager.kt` | Minute-based quota for voice |
| `ProjectVoiceContextBuilder.kt` | Assembles system prompt context |

## 10. Files to Modify

| File | Change |
|------|--------|
| `VoiceCommandHandler.kt` | Add Tier 2 branch for Flash Live when local matching returns Unknown |
| `VoiceResponseManager.kt` | Handle Live API audio streaming output, keep Android TTS fallback |
| Settings screen | Add "Voice response" toggle (Device voice / Natural voice) |
| `GeminiAiService.kt` | Optionally expose Live API client if not handled in VoiceLiveSession |
| Pro gating | Voice Live is Pro-only. Free users get Tier 1 only. |

## 11. Files to Leave Alone

| File | Reason |
|------|--------|
| Local keyword matching code | Still used as Tier 1 |
| Number parser (EN/FI 1-20) | Still used for counted commands |
| Existing VoiceCommand sealed class | Increment/Decrement commands still work via Tier 1 |
| Other AI features | Flash Live is voice-only; other features keep using Flash Lite |

---

## 12. Edge Cases

| Scenario | Behavior |
|----------|----------|
| User asks long question | Live API streams response, user hears it naturally |
| User interrupts response | Old audio stops, new input processed |
| Network drops mid-session | Attempt reconnect; if fails, fall back to v2 flow for next command |
| User switches projects during session | Close session, reopen with new context |
| Row changes during session | Send context update to session, don't reconnect |
| Session open for a long time | Auto-close after 60 seconds of silence |
| Multiple rapid questions | Same session handles them sequentially |
| User says something unrelated to knitting | Model answers briefly; no filtering needed |
| Quota runs out mid-session | Current response completes, next attempt falls back to Tier 1 |

---

## 13. Testing

### Core functionality
- [ ] Activate voice mode → local commands ("add row") work instantly via Tier 1
- [ ] Unknown command → sent to Flash Live → natural voice response
- [ ] Response mentions correct project context (pattern name, current row, etc.)
- [ ] Voice sounds natural, not robotic
- [ ] User can interrupt and ask follow-up

### Fallback behavior
- [ ] Offline → Tier 1 only, clear message for unknown commands
- [ ] Quota exhausted → fallback to Tier 1 with snackbar
- [ ] "Natural voice" setting off → old v2 flow (Gemini text + Android TTS)
- [ ] Free user → Tier 1 only, Pro prompt on unknown commands

### Context
- [ ] Asking "what yarn am I using" returns correct yarn name
- [ ] Asking "what row am I on" returns correct number
- [ ] After row advance, asking about current row returns updated number
- [ ] Switching projects clears old context

### Quota
- [ ] Minutes counted correctly for both input and output
- [ ] Quota resets on month boundary
- [ ] Near-quota warning (e.g., "5 minutes remaining this month")

---

## 14. Rollout Considerations

### Preview status

Gemini 3.1 Flash Live is in preview. Google's preview models typically stay available for months before deprecation, with clear migration paths. This is acceptable for KnitTools because:
- The voice feature degrades gracefully if the model becomes unavailable (falls back to Tier 1 and/or old v2 flow)
- Firebase Remote Config can switch models without app update if needed

### Rate limits

Preview models have stricter rate limits than stable models. Monitor usage in Firebase Console. If rate limits are hit during testing, consider:
- More aggressive local matching (handle more commands via Tier 1)
- Shorter session timeouts
- Caching common answers

---

## 15. Success Criteria

1. Voice responses sound natural — not robotic
2. Answers are context-aware (mention current project data, not generic responses)
3. Simple commands still work offline and instantly
4. Quota system prevents runaway costs
5. Free users retain basic voice command functionality
6. Preview model status doesn't break the app if model changes
