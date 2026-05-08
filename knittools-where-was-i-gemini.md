# KnitTools — "Where Was I?" Context-Aware Summary

**Type:** Delta spec — prompt improvement to existing Project Summary  
**Status:** Spec ready for implementation  
**Depends on:** Project Summary with Gemini (already implemented)  
**Pro only:** Yes  
**Key idea:** No new UI. Same "View AI summary" button, smarter response based on time since last session.

---

## 1. What Changes

The Project Summary prompt becomes context-aware. It checks how long ago the user last knitted this project and adjusts its response:

- **Returning after a break (24+ hours since last session):** Focus on context recovery — where in the pattern, what section, what to do next, any relevant notes
- **Active project (less than 24 hours):** Focus on progress — pace, time, session stats

This replaces the need for a separate "Where was I?" feature. Same button, same bottom sheet, smarter output.

---

## 2. Additional Data for Prompt

Add to the existing data already gathered for project summary:

| Data | Source | Example |
|------|--------|---------|
| Hours since last session | Latest SessionEntity.endTime vs now | 120 hours (5 days) |
| Last session end row | Latest SessionEntity.endRow | 20 |
| Rows since last session | currentRow - lastSessionEndRow | 4 |
| Pattern section context | From Pattern Viewer instruction cache (if available) | "decrease section" |

---

## 3. Updated Prompt

Replace the existing project summary prompt with:

```
You are a knitting project assistant. Summarize the current state of this knitting project in 2-4 sentences.

If the knitter has not worked on this project recently (more than 24 hours since last session), focus on helping them pick up where they left off: mention what section of the pattern they are in, what the current row means in context, reference any notes, and briefly state what comes next if it can be inferred.

If the knitter has been working on this recently (within 24 hours), focus on progress: pace, sessions, and time spent.

Be informative and concise, like a knowledgeable friend glancing at your project. Do not be overly enthusiastic. Do not add advice or suggestions unless the user's notes indicate a problem.

Respond in English.

Project data:
- Name: {projectName}
- Current row: {currentRow}
- Pattern: {patternName or "not set"}
- Yarn: {yarnInfo or "not set"}
- Total knitting time: {totalMinutes} minutes across {sessionCount} sessions
- Started: {daysAgo} days ago
- Hours since last session: {hoursSinceLastSession}
- Last session ended at row: {lastSessionEndRow or "unknown"}
- Average pace: {avgRowsPerSession} rows per session
- Stitches per row: {stitchCount or "not set"}
- Notes: {notes or "none"}
- Counters: {counterSummary or "none"}
```

---

## 4. Example Outputs

**Returning after 5 days, pattern linked:**
> Corner To Corner Dishcloth, row 24 — you're in the decrease section. Last session was 5 days ago at row 20. Your note mentions using smaller needles than recommended. Continue repeating the decrease row until 7 stitches remain.

**Returning after 3 days, no pattern:**
> Project 4, row 24. You last knitted 3 days ago and ended at row 20. Four rows of progress since then across one short session.

**Active today, good progress:**
> Corner To Corner Dishcloth, row 24. Three sessions today totaling 45 minutes, averaging 8 rows per session. Steady pace through the decrease section.

**Just started, first session ever:**
> Corner To Corner Dishcloth, row 3. Just getting started — 7 minutes in your first session so far.

---

## 5. Implementation

### 5.1 Add last session data to prompt builder

```kotlin
// Get latest session for this project
val lastSession = sessionDao.getLatestSession(projectId)
val hoursSinceLastSession = if (lastSession != null) {
    ChronoUnit.HOURS.between(lastSession.endTime, Instant.now())
} else {
    null  // No sessions yet
}
val lastSessionEndRow = lastSession?.endRow
```

### 5.2 Replace prompt template

Swap the existing prompt string with the updated version from section 3 above. No other code changes needed — same GeminiAiService call, same bottom sheet UI, same gating.

---

## 6. Files to Modify

| File | Change |
|------|--------|
| ProjectSummarizer.kt (or equivalent) | Update prompt template, add hoursSinceLastSession and lastSessionEndRow to data gathering |

## 7. Files NOT Modified

| File | Reason |
|------|--------|
| Everything else | This is a prompt-only change with one extra data query |

---

## 8. Edge Cases

| Scenario | Behavior |
|----------|----------|
| No sessions at all | Prompt receives "Hours since last session: never". Summary says "Just getting started." |
| Last session was minutes ago | Treated as active — progress-focused summary |
| Last session was months ago | Context recovery — emphasizes where they left off |
| Notes field has useful context | Included in both modes |
| No pattern linked | Summary uses only row count and time, no pattern section context |

---

## 9. Testing

- [ ] Open project not touched in 3+ days → summary mentions "last session was X days ago" and emphasizes context
- [ ] Open project worked on today → summary emphasizes progress and pace
- [ ] Project with no sessions → summary says "just getting started" or similar
- [ ] Last session end row is included when available
- [ ] Notes content still referenced in both modes

---

## 10. Success Criteria

1. No new UI elements — same "View AI summary" button
2. User returning after a break gets a context-recovery response, not a progress cheerleader
3. Active user still gets useful progress summary
4. One prompt change, one extra data query — minimal implementation effort
