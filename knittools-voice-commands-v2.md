# KnitTools Voice Commands v2 — Delta Spec

## Purpose

Replace the current one-shot voice command model with a continuous listening mode, AI-powered natural language interpretation, and text-to-speech responses. This makes the Counter screen genuinely hands-free during knitting sessions.

## Scope

This spec covers only the Counter screen voice command system. It does not modify other screens, navigation, or existing AI features (project summary, pattern instructions, yarn scanning).

## Current State

- Long-press on the large `+` button activates SpeechRecognizer
- Single recognition result is matched against a keyword/synonym list per locale (EN, FI)
- Keywords are defined in `strings.xml` per locale
- One command per activation — user must long-press again for the next command
- Microphone icon was added to the TopAppBar next to the camera icon
- Pro-gated feature
- SpeechRecognizer lifecycle is managed in CounterViewModel

## Design Goals

1. One tap to enter continuous listening — no repeated activation needed
2. Keyword matching stays first for speed and zero quota cost
3. Gemini Flash Lite interprets unrecognized commands with full project context
4. TTS reads back responses and confirmations when appropriate
5. Quota usage stays minimal even in long sessions
6. Graceful degradation: no network → keyword-only mode still works

---

## Architecture Overview

```
Speech input
    │
    ▼
SpeechRecognizer (Android)
    │
    ▼
Recognized text
    │
    ├──► Keyword matcher (local, instant, free)
    │       │
    │       ├── Match found → Execute action immediately
    │       │
    │       └── No match ──►  AI interpreter (Gemini Flash Lite)
    │                              │
    │                              ├── Structured command → Execute action
    │                              │
    │                              ├── Query (e.g. "how many rows left") → TTS response
    │                              │
    │                              └── Unrecognized → TTS "I didn't understand"
    │
    ▼
SpeechRecognizer restarts automatically (continuous mode)
```

---

## 1. Continuous Listening Mode

### Activation

- **TopAppBar microphone icon**: tap to toggle continuous listening on/off
- **Long-press on large `+` button**: activates one-shot mode as before (backward compatible)
- Continuous mode is visually distinct from one-shot mode

### Behavior While Active

- After each `onResults` callback, `SpeechRecognizer.startListening()` is called again automatically
- After `onError` with `ERROR_NO_MATCH` or `ERROR_SPEECH_TIMEOUT`, restart listening (silence is not a reason to stop)
- Continuous mode ends when:
  - User taps the microphone icon again
  - User navigates away from Counter screen
  - Auto-timeout: no recognized command for 5 minutes → stop listening, show snackbar "Voice mode ended due to inactivity"
  - App goes to background (stop in `onPause`, do NOT auto-resume in `onResume`)

### Visual Indicator

- When continuous listening is active, the TopAppBar microphone icon changes to an active state:
  - Icon tint: `burnt orange` (primary color)
  - Subtle pulsing animation on the icon (not on the entire TopAppBar)
- When listening is momentarily processing (between recognizer restart), the animation pauses briefly
- When one-shot mode is active (from long-press), show the existing microphone overlay indicator as before — no TopAppBar change

### SpeechRecognizer Lifecycle

- Only one SpeechRecognizer instance at a time — destroy before creating a new one
- On `onError` with fatal errors (`ERROR_CLIENT`, `ERROR_INSUFFICIENT_PERMISSIONS`, `ERROR_RECOGNIZER_BUSY`): stop continuous mode, show error snackbar
- On `onError` with transient errors (`ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT`, `ERROR_NETWORK` in keyword-only fallback): restart listening
- If `ERROR_NETWORK` and Gemini fallback was expected: continue in keyword-only mode, notify user once via TTS "Offline mode, basic commands only"
- Guard against multiple `startListening` calls — use a boolean `isListenerActive` flag

---

## 2. Command Interpretation Pipeline

### Stage 1: Keyword Matching (unchanged, extended)

Keyword matching remains the first step. It is local, instant, and free.

**Existing commands** (keep as-is from current `strings.xml` synonyms):
- next / seuraava → increment row
- back / previous / taakse / edellinen → decrement row  
- undo / kumoa → undo last action
- reset / nollaa → reset (with confirmation)
- stitch / silmukka → increment stitch (if stitch tracking enabled)

**New keyword commands to add:**
- "stop" / "lopeta" / "stop listening" / "lopeta kuuntelu" → deactivate continuous listening
- "help" / "apua" / "komennot" / "commands" → TTS reads available commands

These new keywords are handled locally — no Gemini call needed.

### Stage 2: AI Interpretation (Gemini Flash Lite)

If keyword matching produces no match, send the recognized text to Gemini for interpretation.

**Input to Gemini:**

```
System prompt:
You are a voice command interpreter for a knitting row counter app.
Given a voice command and project context, return a JSON action.

Available actions:
- {"action": "increment", "count": N}
- {"action": "decrement", "count": N}  
- {"action": "undo"}
- {"action": "reset"} (always requires confirmation)
- {"action": "add_note", "text": "..."}
- {"action": "query_progress"} (user asks about current state)
- {"action": "query_remaining"} (user asks how much is left)
- {"action": "unknown"}

Project context:
- Project name: {name}
- Current row: {currentRow}
- Target rows: {targetRows} (null if not set)
- Stitch tracking: {enabled/disabled}
- Current stitches: {currentStitch}/{totalStitches}
- Active counters: [{name, type, currentCount}]

User said: "{recognized_text}"
Language detected: {locale}

Respond with ONLY a JSON object, no other text.
```

**Important constraints:**
- System prompt is short and static — no conversation history
- User message is just the recognized text + context
- Expected response: one small JSON object
- This keeps token usage minimal per call

**Response handling:**
- Parse JSON response
- If `action` is recognized → execute
- If `action` is `"unknown"` → TTS "I didn't understand that command"
- If JSON parsing fails → treat as unknown
- If network call fails → TTS "Command not recognized offline"

### Stage 3: Action Execution

All actions from both keyword matching and AI interpretation use the same existing CounterViewModel methods. No new business logic paths.

- `increment(count)` → call existing increment, repeat `count` times or add batch increment method
- `decrement(count)` → same pattern
- `undo` → existing undo
- `reset` → show confirmation dialog (even from voice — safety requirement)
- `add_note` → append text to project notes via existing notes save flow
- `query_progress` → compose text response, send to TTS
- `query_remaining` → compose text response, send to TTS

---

## 3. Text-to-Speech (TTS) Responses

### Setup

- Use Android `TextToSpeech` API
- Initialize in CounterViewModel alongside SpeechRecognizer
- Set language to match current app locale (EN/FI)
- Release in `onCleared()`

### When to Speak

| Trigger | TTS Response |
|---|---|
| Increment by N (N > 1) | "Row {newRow}" |
| Increment by 1 | No TTS (too frequent, would be annoying) |
| Decrement | "Back to row {newRow}" |
| Undo | "Undone, row {newRow}" |
| Reset confirmed | "Counter reset" |
| Note added | "Note saved" |
| Query progress | "Row {current} of {target}, {percent}% done" or "Row {current}" if no target |
| Query remaining | "{remaining} rows left" or "No target set" |
| Unknown command | "I didn't understand" |
| Help requested | "Available commands: next, back, undo, reset, and free-form instructions" |
| Network lost during AI mode | "Offline mode, basic commands only" (once per session) |
| Continuous mode auto-timeout | "Voice mode ended" |
| Voice quota reached | "AI commands paused for today, basic commands still work" |

### TTS Configuration

- Do NOT speak while SpeechRecognizer is actively listening (causes feedback loop)
- Pause recognizer → speak → resume recognizer
- If user interrupts TTS with a new command, stop speaking immediately
- Queue length: max 1 utterance — new utterance replaces pending one
- Volume: use `AudioManager.STREAM_MUSIC` at current device volume

---

## 4. Quota Management

### Existing System

`AiQuotaManager` tracks monthly cloud AI calls with a 500-call allowance.

### Voice-Specific Limits

Add a separate daily voice AI budget to prevent one long session from consuming the monthly allowance:

- **Daily voice AI limit: 50 calls**
- Tracked in AiQuotaManager with a separate daily counter that resets at midnight local time
- When daily voice limit is reached:
  - Keyword commands continue working normally
  - AI interpretation is skipped
  - TTS announces once: "AI commands paused for today, basic commands still work"
  - Microphone icon visual changes subtly (e.g. desaturated) to indicate limited mode
- Each voice AI call also counts toward the existing monthly 500-call budget
- Voice AI calls are recorded with type `VOICE_COMMAND` for potential future analytics differentiation

### Quota Optimization Strategies

**Short prompt, short response:**  
The voice interpretation prompt is intentionally minimal. Expected token usage per call: ~200 input tokens, ~30 output tokens. At Flash Lite pricing this is negligible cost, but the 500/month limit is the real constraint.

**Result caching:**
- Cache the last 10 AI-interpreted commands with their results in memory (not Room)
- Cache key: normalized lowercase text (trimmed, single-spaced)
- Cache TTL: duration of the current Counter screen session
- If the same text appears again within the session, return cached result — no Gemini call
- Example: user says "three forward" repeatedly → first call hits Gemini, subsequent ones are cached

**Deduplication window:**
- If the same recognized text arrives within 3 seconds of the previous one (SpeechRecognizer double-fire), skip entirely
- This prevents accidental double-counting from recognizer restarts

---

## 5. Pro Gating

Voice commands remain a Pro feature (`ProFeature.VOICE_COMMANDS`).

Specific gating:
- **Continuous listening mode**: Pro only
- **AI interpretation**: Pro only (since it requires Pro voice commands)
- **TTS responses**: Pro only (tied to voice command activation)
- **One-shot keyword commands via long-press**: Pro only (unchanged from current)

Free users tapping the microphone icon see the existing Pro upgrade prompt.

---

## 6. Permissions

No new permissions needed.

- `RECORD_AUDIO`: already declared and handled at runtime
- `INTERNET`: already declared (needed for Gemini calls, but keyword-only mode works without it)
- TTS: no permission required

---

## 7. UI Changes Summary

### TopAppBar (Counter Screen)

- Microphone icon: existing, now acts as continuous listening toggle
- Active state: burnt orange tint + subtle pulse animation
- Limited mode (quota reached): desaturated icon tint
- Tap behavior:
  - If Pro + permission granted → toggle continuous listening
  - If Pro + permission not granted → request permission
  - If not Pro → show upgrade prompt

### Large `+` Button

- Long-press behavior unchanged: one-shot voice command (Pro only)
- No visual changes

### Snackbar Messages

- "Voice mode ended due to inactivity" (auto-timeout)
- "Offline mode, basic commands only" (network lost)
- "AI commands paused for today, basic commands still work" (daily quota)
- Standard error snackbars for recognizer failures

### No New Screens or Dialogs

This feature adds no new screens. All interaction happens through the existing Counter screen TopAppBar icon, TTS audio output, and occasional snackbars.

---

## 8. Localization

### SpeechRecognizer Locale

- Set to device locale if supported (EN, FI)
- Fall back to EN if locale is unsupported

### AI Interpretation

- The Gemini prompt includes detected locale
- Gemini handles multilingual input natively — no per-language prompt variants needed
- User can speak in any language; Gemini interprets intent regardless

### TTS Locale

- Match app locale setting
- If TTS engine doesn't support FI, fall back to EN and log once

### New Strings

Add to `strings.xml` (EN and FI):
- Voice mode activation/deactivation confirmations
- Quota limit messages
- Help command response text
- Error messages for recognizer failures

---

## 9. Error Handling

| Scenario | Behavior |
|---|---|
| SpeechRecognizer not available on device | Hide microphone icon entirely |
| Permission denied | Show rationale dialog, do not activate |
| Network unavailable | Keyword-only mode, notify once via TTS |
| Gemini call fails (timeout, error) | Treat as unknown command, TTS "Command not recognized" |
| Gemini returns unparseable response | Treat as unknown command |
| TTS engine unavailable | Voice commands still work, just no audio feedback |
| Daily voice quota reached | Keyword-only mode, notify once via TTS |
| Monthly quota reached | Same as daily quota behavior |
| App backgrounded during listening | Stop listening, do not auto-resume |
| Incoming phone call | System stops SpeechRecognizer; on return, do not auto-resume |

---

## 10. Testing Priorities

1. Continuous listening restart loop: verify recognizer restarts after each result and after transient errors
2. Keyword matching still works identically to current implementation
3. Gemini interpretation returns valid actions for common phrasings in EN and FI
4. Daily quota enforcement: verify commands still work after quota, just without AI
5. TTS does not create audio feedback loop with SpeechRecognizer
6. Single SpeechRecognizer instance guarantee — no leaks on rapid toggle
7. Auto-timeout after 5 minutes of silence
8. Background/foreground transitions do not leave orphaned listeners
9. Cache hit for repeated AI commands
10. Deduplication window prevents double-fire

---

## 11. Implementation Order

Recommended sequence:

1. **Continuous listening mode** — toggle activation, auto-restart loop, visual indicator, timeout, lifecycle safety
2. **New keyword commands** — "stop", "help" added to keyword matcher and strings.xml
3. **AI interpretation pipeline** — Gemini prompt, JSON parsing, action mapping
4. **Daily voice quota** — AiQuotaManager extension, daily counter, fallback behavior
5. **TTS integration** — TextToSpeech setup, response triggers, recognizer pause/resume coordination
6. **Result caching and deduplication** — in-memory cache, 3-second dedup window
7. **Localization** — FI strings, TTS locale fallback
8. **Testing and edge cases** — all scenarios from section 10

---

## Dependencies

- Existing: SpeechRecognizer, CounterViewModel, AiQuotaManager, GeminiAiService
- New Android API: `android.speech.tts.TextToSpeech`
- No new libraries or dependencies required
