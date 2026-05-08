# KnitTools — Project Summary with Gemini

**Type:** Delta spec — replace Nano backend, improve output quality  
**Status:** Spec ready for implementation  
**Depends on:** Firebase AI Logic setup (GeminiAiService, AiQuotaManager)  
**Pro only:** Yes  
**First AI feature:** This is the simplest possible test of the Firebase AI Logic pipeline

---

## 1. What Changes

The existing "View AI summary" feature in the Counter screen project card switches from Gemini Nano (ML Kit GenAI) to Gemini 2.5 Flash-Lite via Firebase AI Logic.

### What stays the same
- "View AI summary" link position in project card
- Bottom sheet UI (slides up from bottom)
- Pro gating (free users don't see the link)
- Summary is generated on demand when user taps the link
- Summary is not persisted to Room

### What changes
- **Backend:** Nano → GeminiAiService (Firebase AI Logic)
- **Availability:** Works on ALL devices with internet, not just Nano-capable ones
- **Gating:** `ProState.hasFeature(AI_FEATURES) && aiQuotaManager.hasQuota()` instead of `ProState.hasFeature(NANO_FEATURES) && nanoAvailability.isAvailable()`
- **Tone:** Informative and natural, not cheerleader
- **Content:** Richer — includes pattern name and yarn info when available
- **Quota:** Each summary generation counts as 1 AI call

---

## 2. Data Gathered for Prompt

Collect from existing project state (already available in CounterViewModel):

| Data | Source | Example |
|------|--------|---------|
| Project name | CounterProjectEntity.name | "Project 4" |
| Current row | CounterProjectEntity.currentCount | 24 |
| Pattern name | SavedPatternEntity.title (if linked) | "Corner To Corner Knit Dishcloth" |
| Linked yarn | YarnCardEntity.brand + name (if linked) | "Drops Alpaca, natural white" |
| Total time | Aggregated from SessionEntity | 47 minutes |
| Session count | Count of SessionEntity for project | 3 |
| Days since created | CounterProjectEntity.createdAt | 5 days |
| Average rows per session | currentCount / sessionCount | 8 |
| Notes | CounterProjectEntity.notes | "Using smaller needles than recommended" |
| Stitch count | CounterProjectEntity.stitchCount (if set) | 58 |
| Active counters | ProjectCounterEntity names + values | "Sleeve repeat: 3" |

Not all fields will have values. The prompt handles missing data gracefully.

---

## 3. Prompt Design

```
You are a knitting project assistant. Summarize the current state of this knitting project in 2-3 sentences. Be informative and concise, like a knowledgeable friend glancing at your project. State facts about progress, mention the pattern or yarn if known, and note anything useful from the notes. Do not be overly enthusiastic or use exclamation marks excessively. Do not add advice or suggestions unless the user's notes indicate a problem.

Respond in English.

Project data:
- Name: {projectName}
- Current row: {currentRow}
- Pattern: {patternName or "not set"}
- Yarn: {yarnInfo or "not set"}
- Total knitting time: {totalMinutes} minutes across {sessionCount} sessions
- Started: {daysAgo} days ago
- Average pace: {avgRowsPerSession} rows per session
- Stitches per row: {stitchCount or "not set"}
- Notes: {notes or "none"}
- Counters: {counterSummary or "none"}
```

### Example outputs

**Full data (pattern + yarn + notes):**
> Corner To Corner Dishcloth in Drops Alpaca, row 24 of the decrease section. You've been averaging 8 rows per session across 3 sessions over 5 days. Your note mentions using smaller needles than recommended.

**Pattern linked, no yarn:**
> Corner To Corner Knit Dishcloth, row 24. About 7 minutes of knitting in one session so far — just getting started.

**Minimal data (no pattern, no yarn, no notes):**
> Project 4, row 24. Three knitting sessions totaling 47 minutes over 5 days.

### What makes this better than current Nano output

Current Nano output:
> "Great progress on Project 4! You've knitted 24 rows so far, which took about 7 minutes across one knitting session. You've been working on this for 5 days now, averaging 24 rows per session – a really consistent pace! It's wonderful to see you're enjoying your knitting."

Problems: generic cheerleading, doesn't mention pattern or yarn, doesn't reference notes, same tone regardless of context, excessive praise for minimal progress.

Gemini output will be: factual, context-aware, mentions what the user is actually making, references their notes, appropriate tone for the amount of progress.

---

## 4. Implementation

### 4.1 Modify existing ProjectSummarizer (or equivalent)

Replace Nano call with GeminiAiService call:

```kotlin
// Before (Nano)
val result = nanoModel.generateContent(prompt)

// After (Gemini via Firebase AI Logic)
val result = geminiAiService.generateText(prompt)
```

The prompt assembly (collecting project data, formatting the prompt string) stays in the same place. Only the AI call changes.

### 4.2 Update gating

```kotlin
// Before
val showSummaryLink = proState.hasFeature(ProFeature.NANO_FEATURES)
    && nanoAvailability.isAvailable()

// After
val showSummaryLink = proState.hasFeature(ProFeature.AI_FEATURES)
    && aiQuotaManager.hasQuota()
```

### 4.3 Language

Hardcoded to English for now. When the app is localized later, this line changes to read from `Locale.getDefault().displayLanguage` and the prompt switches dynamically.

### 4.4 Handle loading and errors

| State | UI |
|-------|-----|
| User taps "View AI summary" | Bottom sheet opens with shimmer/loading |
| Gemini returns text | Replace shimmer with summary text |
| No internet | Show brief message: "Summary requires internet connection" |
| Quota exhausted | Show brief message: "Monthly AI limit reached" |
| API error | Show brief message: "Could not generate summary" |

Error messages are shown inside the bottom sheet, same position as where the summary would appear. No toasts or snackbars needed.

### 4.5 Quota counting

Call `aiQuotaManager.recordCall()` only after a successful response. Failed calls don't consume quota.

---

## 5. Files to Modify

| File | Change |
|------|--------|
| `ProjectSummarizer.kt` (or equivalent) | Replace Nano call with GeminiAiService. Update prompt. Add locale. |
| Counter screen composable | Update gating condition for "View AI summary" visibility |
| Summary bottom sheet composable | Add error state handling (no internet, quota, API error) |
| ViewModel (CounterViewModel or dedicated) | Update summary generation to use new service |

## 6. Files NOT Modified

| File | Reason |
|------|--------|
| Room entities / migrations | No schema changes — summary is not persisted |
| Navigation | No new routes — bottom sheet is already in place |
| Bottom sheet UI layout | Same visual structure, just content changes |
| Project card layout | "View AI summary" link stays in same position |

---

## 7. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Brand new project (row 0, no sessions) | Summary says something like "Just started, no rows yet" |
| Very old project with lots of data | Summary prioritizes recent activity and current state |
| No pattern, no yarn, no notes | Summary uses only row count and time data |
| Very long notes field | Truncate notes to ~200 characters before sending to prompt |
| User taps summary rapidly multiple times | Cancel previous request, show latest result |
| Offline | "Summary requires internet connection" in bottom sheet |
| Summary already showing, user taps again | Regenerate (fresh call, not cached) |

---

## 8. Testing

### Verify Firebase AI Logic pipeline works
- [ ] Tap "View AI summary" → loading state appears → summary text appears
- [ ] Summary mentions pattern name when pattern is linked
- [ ] Summary mentions yarn when yarn is linked
- [ ] Summary references notes content when notes exist
- [ ] Free user does not see "View AI summary" link
- [ ] Pro user without internet sees error message in bottom sheet
- [ ] AI monitoring dashboard in Firebase Console shows the request

### Verify Nano removal
- [ ] Feature works on a device WITHOUT Gemini Nano support
- [ ] No references to NanoAvailability in summary flow
- [ ] No ML Kit GenAI imports in summary-related files

---

## 9. Success Criteria

1. "View AI summary" works on any Android device with internet + Pro
2. Summary tone is informative, not generic cheerleading
3. Summary includes contextual details (pattern, yarn, notes) when available
4. Firebase AI monitoring shows successful requests
5. This validates the entire GeminiAiService → AiQuotaManager pipeline for all future features
