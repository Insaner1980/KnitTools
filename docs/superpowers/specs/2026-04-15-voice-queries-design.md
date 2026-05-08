# Voice Queries — Ääneen vastattavat projektikyselyt

## Tavoite

Laajentaa Voice Commands v2:n toimintopohjaista mallia kyselyillä. Neuloja voi kysyä projektistaan äänellä ja saada TTS-vastauksen — kädet pysyvät puikoilla.

## Arkkitehtuuri

Nykyinen putki pysyy samana, laajennetaan sitä:

```
Puhe → SpeechRecognizer → teksti
  → keyword match (toimintokomennot, kuten ennenkin)
  → Gemini luokittelee intentin (toiminto TAI kysely)
  → executeVoiceAction:
      - toiminto → suorita (increment, undo, jne.)
      - kysely → paikallinen koodi hakee datan → TTS vastaa
```

Gemini tuottaa vain action-tyypin (JSON). Vastauksen teksti tuotetaan paikallisesta datasta — ei hallusinaatioita, ei ylimääräistä kiintiökustannusta.

Ei uusia luokkia tai tiedostoja.

## Muutettavat tiedostot

1. `AiVoiceAction` — 4 uutta sealed-varianttia
2. `VoiceCommandInterpreter.ProjectContext` — rikkaampi konteksti
3. `VoiceCommandInterpreter.buildPrompt()` — uudet actionit + konteksti
4. `VoiceCommandInterpreter.parseResponse()` — uudet action-stringit
5. `CounterViewModel.interpretVoiceCommand()` — rikkaamman kontekstin rakennus
6. `CounterViewModel.executeVoiceAction()` — korjatut + uudet vastauslogiikat
7. `strings.xml` (EN + FI) — TTS-vastauspohjat

## Uudet ja korjatut action-tyypit

### Korjattavat (olemassa mutta vajaat)

| Action | Nyt | Korjauksen jälkeen |
|---|---|---|
| `QueryProgress` | "Row 45" aina | "Row 45 of 120, 38%" kun target asetettu |
| `QueryRemaining` | "No target set" aina | "75 rows left" kun target asetettu |

### Uudet

| Action | Kysymysesimerkki | Datan lähde |
|---|---|---|
| `QuerySessionTime` | "How long have I knitted?" | `state.sessionSeconds` |
| `QueryYarn` | "What yarn am I using?" | `state.linkedYarns` |
| `QueryInstruction` | "What's the current instruction?" | PatternViewer instruction state |
| `QueryShaping` | "When is the next decrease?" | `state.projectCounters` (shaping) |

## Kontekstin laajentaminen

`VoiceCommandInterpreter.ProjectContext` saa uudet kentät:

```kotlin
data class ProjectContext(
    // Nykyiset (ennallaan):
    val projectName: String,
    val currentRow: Int,
    val targetRows: Int?,
    val stitchTrackingEnabled: Boolean,
    val currentStitch: Int,
    val totalStitches: Int?,
    val activeCounters: List<CounterInfo>,
    // Uudet:
    val sessionSeconds: Int,
    val linkedYarnNames: List<String>,
    val currentInstruction: String?,
    val shapingCounters: List<ShapingInfo>,
)

data class ShapingInfo(
    val name: String,
    val currentCount: Int,
    val targetCount: Int?,
    val repeatInterval: Int?,
)
```

## Gemini-promptin muutokset

### Uudet actionit

```
- {"action": "query_session_time"}
- {"action": "query_yarn"}
- {"action": "query_instruction"}
- {"action": "query_shaping"}
```

### Disambiguation-lisäys

```
- Questions about time, duration, "how long" → query_session_time
- Questions about yarn, fiber, material, color → query_yarn
- Questions about instruction, pattern, "what do I do" → query_instruction
- Questions about shaping, decrease, increase, "when next" → query_shaping
- Questions about progress, percentage, "how far" → query_progress
- Questions about remaining, "how many left" → query_remaining
```

### Konteksti laajenee

```
- Session time: 45 minutes
- Linked yarns: [Drops Alpaca (light blue), Novita Nalle (white)]
- Current instruction: "K2, P2, repeat to end"
- Shaping counters: [Raglan decrease: row 5/8, every 2nd row]
```

## Vastauslogiikka

### QueryProgress (korjattu)

```kotlin
if (targetRows != null && targetRows > 0) {
    val percent = (currentRow * 100) / targetRows
    // "Row 45 of 120, 38%"
} else {
    // "Row 45"
}
```

### QueryRemaining (korjattu)

```kotlin
if (targetRows != null) {
    val remaining = (targetRows - currentRow).coerceAtLeast(0)
    // "75 rows left" / "0 rows left, you're done!"
} else {
    // "No target set"
}
```

### QuerySessionTime

```kotlin
val minutes = sessionSeconds / 60
// >= 1min: "You've been knitting for 45 minutes"
// < 1min: "Less than a minute"
```

### QueryYarn

```kotlin
if (linkedYarns.isNotEmpty()) {
    // Ensimmäisen langan nimi + väri, lista jos useita
} else {
    // "No yarn linked to this project"
}
```

### QueryInstruction

```kotlin
if (currentInstruction != null) {
    // Lukee ohjeen ääneen suoraan
} else {
    // "No pattern linked"
}
```

### QueryShaping

```kotlin
val shaping = shapingCounters.firstOrNull()
if (shaping != null && shaping.repeatInterval != null) {
    val rowsUntilNext = shaping.repeatInterval - (currentRow % shaping.repeatInterval)
    // "Next decrease in 3 rows"
} else if (shaping != null) {
    // "Shaping counter at row 5"
} else {
    // "No shaping counter active"
}
```

## String-resurssit

| Avain | EN | FI |
|---|---|---|
| `voice_progress_with_target` | Row %1$d of %2$d, %3$d%% | Rivi %1$d / %2$d, %3$d%% |
| `voice_remaining_rows` | %1$d rows left | %1$d riviä jäljellä |
| `voice_remaining_done` | You're done! | Valmis! |
| `voice_session_minutes` | You've been knitting for %1$d minutes | Olet neulonut %1$d minuuttia |
| `voice_session_under_minute` | Less than a minute | Alle minuutti |
| `voice_yarn_single` | %1$s | %1$s |
| `voice_yarn_multiple` | %1$s and %2$d more | %1$s ja %2$d muuta |
| `voice_yarn_none` | No yarn linked | Ei lankaa liitetty |
| `voice_instruction_none` | No pattern linked | Ei kaavaa liitetty |
| `voice_shaping_next` | Next %1$s in %2$d rows | Seuraava %1$s %2$d rivin päästä |
| `voice_shaping_at` | %1$s at row %2$d | %1$s rivillä %2$d |
| `voice_shaping_none` | No shaping counter active | Ei muotoilulaskuria käytössä |

## Toteutusjärjestys

1. `AiVoiceAction` — 4 uutta sealed-varianttia
2. `VoiceCommandInterpreter.ProjectContext` — uudet kentät + `ShapingInfo`
3. `VoiceCommandInterpreter.buildPrompt()` — actionit, konteksti, disambiguation
4. `VoiceCommandInterpreter.parseResponse()` — tunnista uudet action-stringit
5. `CounterViewModel.interpretVoiceCommand()` — rakenna rikkaampi konteksti
6. `CounterViewModel.executeVoiceAction()` — korjaa 2 + lisää 4 vastauslogiikkaa
7. `strings.xml` — EN + FI resurssit
