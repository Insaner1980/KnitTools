# KnitTools — Code Review Prompts for Claude Code

31 focused prompts. Feed one at a time. Each is self-contained.

---

## 1 — Build config & dependencies

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review build.gradle(.kts) files and libs.versions.toml only:
    1. Are all dependencies at latest stable versions? Flag outdated ones with current vs latest.
    2. Are there unused dependencies (declared but never imported in code)?
    3. Is ProGuard/R8 configured for release? Check keep rules for Room, Hilt, ML Kit, Gemini Nano, Glance.
    4. Is signing config safe (no hardcoded secrets, not using debug keystore for release)?
    5. Is isMinifyEnabled and isShrinkResources true for release?
    6. Are there Log.d/Log.v calls that should be stripped in release builds?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code. I decide what to fix.
    2. EVIDENCE REQUIRED. Every finding needs: exact file path, line number, quoted code, why it's a problem. No evidence = drop the finding.
    3. NO SPECULATION. "There might be" = you're guessing. Only report what you verify in code.
    4. VERIFY BEFORE REPORTING. Re-read the code. Are you seeing the problem or assuming it?
    5. IT'S OK TO FIND NOTHING. "All checks passed" is a valid result.
    6. CONFIDENCE: CERTAIN (verified) / LIKELY (strong evidence, can't run) / UNCERTAIN (flag separately at end).
    7. CAN'T FIND A FILE? Say so. Don't guess what it contains.
    8. INTENTIONAL DESIGN — do NOT flag: no Firebase/analytics/crash reporting (privacy-first), no cloud sync (offline-first), single-module architecture, Manrope font.
    9. ONE FINDING PER ISSUE. Same pattern in multiple files = one finding, list files.
    10. BUGS NOT PREFERENCES. Missing config = bug. "I'd structure it differently" = preference.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    [OK] What you checked and found fine.
    ### Could Not Verify
    [UNVERIFIED] What and why.
    ### Summary
  </output>
</code_review>
```

---

## 2 — Manifest, permissions & release safety

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review AndroidManifest.xml and release configuration:
    1. Are declared permissions (CAMERA, BILLING) actually used? Any undeclared but needed?
    2. CAMERA: is uses-feature android.hardware.camera set to required=false? (Prevents filtering devices without camera from Play Store)
    3. Is INTERNET permission present? If so, verify it's only needed for billing.
    4. Are exported components safe? Any unintended exports?
    5. Is @HiltAndroidApp on the Application class?
    6. Is edge-to-edge theme configured correctly?
    7. Is debuggable=false for release build type?
    8. Are versionCode and versionName set correctly?
    9. Search entire codebase for HTTP/HTTPS URLs (other than Play Billing). Flag any that contradict privacy-first.
    10. Search for analytics/tracking library imports. There should be none.
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code. I decide what to fix.
    2. EVIDENCE REQUIRED. Every finding: exact file, line, quoted code, why it's a problem. No evidence = drop it.
    3. NO SPECULATION. Only report verified issues.
    4. VERIFY BEFORE REPORTING. Re-read the code before each finding.
    5. IT'S OK TO FIND NOTHING. Don't fabricate.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. INTENTIONAL: no Firebase/analytics (privacy-first), no cloud sync (offline-first).
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 3 — Project structure & dead code

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review project file structure against PROJECT.md spec:
    1. Does the package layout match the documented architecture?
    2. Are there orphaned files (not imported/referenced anywhere)?
    3. Unused imports across the codebase (run a grep/scan).
    4. Are there leftover .gitkeep, TODO files, or temp files?
    5. Clear code duplication that could be safely reduced without behavior change.
    6. Any hardcoded strings in Compose code that should be in strings.xml?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code. I decide what to fix.
    2. EVIDENCE REQUIRED. Every finding: exact file, line, quoted code, why it's a problem. No evidence = drop it.
    3. NO SPECULATION. Only report verified issues.
    4. VERIFY BEFORE REPORTING. Re-read the code before each finding.
    5. IT'S OK TO FIND NOTHING. Don't fabricate.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. INTENTIONAL: single-module architecture — do not suggest modularization.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES. Unused code = finding. "I'd name it differently" = preference.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 4 — Room entities, TypeConverters & indices

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review Room entity definitions in data/local/ only:
    1. Are column types appropriate (Long for timestamps, not String)?
    2. Are nullable fields correctly annotated?
    3. Is yarnCardIds TypeConverter (List<String> → JSON) robust against malformed data?
    4. Are indices defined for foreign key columns (projectId in counter_history, sessions)?
    5. Is CASCADE delete behavior correct (deleting project cascades to history and sessions)?
    6. Are default values sensible for all columns?
    7. Is the database @Singleton scoped in Hilt module?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code. I decide what to fix.
    2. EVIDENCE REQUIRED. Every finding: exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION. Only report verified issues.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest redesigning the schema. Only flag correctness issues.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 5 — Room DAOs & queries

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review all DAO interfaces in data/local/:
    1. Are reactive queries using Flow where the UI needs live updates?
    2. Are write operations (insert, update, delete) suspend functions?
    3. Are there N+1 patterns (loading a list then querying each item individually)?
    4. Is there SQL injection risk in any @RawQuery or string-concatenated queries?
    5. Are transactions used where atomicity is needed (e.g., saving session + updating project)?
    6. Could counter writes from widget and app conflict? Any evidence of thread-safety handling?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION. Verify in code.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 6 — Room migrations

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review Room migration code and migration tests:
    1. Are AutoMigration specs correctly defined for schema changes v1→v2 and v2→v3?
    2. Do the 3 instrumented migration tests use realistic data (not just empty DBs)?
    3. Do tests verify data integrity after migration (field values preserved)?
    4. Does the full chain test (v1→v3) exercise both migrations in sequence?
    5. Is there a fallback strategy if migration fails (fallbackToDestructiveMigration or similar)?
    6. Are new column defaults correct in migration specs?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 7 — DataStore preferences

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review data/datastore/ files:
    1. Is PreferencesManager thread-safe and @Singleton scoped?
    2. Are reads using Flow (not blocking the main thread)?
    3. Is there error handling for corrupted DataStore files?
    4. Are preference keys consistent and not duplicated?
    5. Are default values sensible?
    6. Can UI state and persisted settings drift apart (e.g., theme toggle shows wrong state)?
    7. Is the mapping between stored values and app models (ThemeMode enum etc.) safe?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest replacing DataStore.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 8 — Navigation & routing

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ui/navigation/ files (Screen.kt, TopLevelDestination, KnitToolsNavHost, KnitToolsBottomBar):
    1. Are all 18 routes defined and actually used? Any orphaned routes?
    2. Are route arguments type-safe (cardId, projectId)?
    3. Is nested NavGraph structure correct (one per tab)?
    4. Is back navigation correct within and across tabs?
    5. Is tab state preserved when switching tabs?
    6. Is there duplicate navigation protection (rapid taps)?
    7. Does the bottom bar use M3 NavigationBar with correct icons (FolderOpen, Build, LibraryBooks)?
    8. Does widget deep-link to counter screen work?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest replacing the navigation approach.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 9 — ViewModel scoping & lifecycle

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review all ViewModels (7 total) for scoping and lifecycle:
    1. Is CounterViewModel correctly scoped to Projects nav graph (shared between CounterScreen and ProjectListScreen)?
    2. Is YarnCardViewModel scoped to KnitToolsNavHost level? Does this cause it to live too long?
    3. Are other ViewModels per-screen scoped via hiltViewModel()?
    4. Do any ViewModels hold references they shouldn't (Context, Activity, View)?
    5. Is coroutine work in viewModelScope lifecycle-safe?
    6. Is session save on ViewModel destroy reliable? What if process dies before onCleared?
    7. Is SavedStateHandle used where critical state must survive process death?
    8. Are there ViewModels that should be shared but aren't (causing state loss on navigation)?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest architecture changes unless there's a concrete bug.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 10 — Theme colors & hardcoded color audit

```xml
<code_review>
  <role>Senior Android engineer and UI reviewer, strict code review. Report only.</role>
  <scope>
    Review ui/theme/Color.kt and Theme.kt, then SCAN all files in ui/:
    1. Are M3 color roles mapped correctly for both light and dark schemes?
    2. Are extended colors (KnitToolsExtendedColors) provided via CompositionLocal correctly?
    3. Does MaterialTheme.knitToolsColors extension work?
    4. Do extended color values match PROJECT.md spec?
    5. CRITICAL AUDIT: Search ALL .kt files in ui/ for hardcoded Color() values, hex colors, or direct color references that bypass theme tokens. Report EVERY instance.
    6. Do text colors meet WCAG AA contrast (4.5:1) against backgrounds in both themes? Especially onSurfaceMuted on surfaceVariant, and gold primary on both backgrounds.
    7. Is enableEdgeToEdge() called? System bar insets handled? Status bar transparent?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. INTENTIONAL: the specific color values in Color.kt/Theme.kt are deliberate design decisions. Only flag if they don't match PROJECT.md or fail contrast.
    9. ONE FINDING PER ISSUE. Hardcoded colors: one finding listing all instances.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 11 — Typography, shapes & noise texture

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ui/theme/Type.kt, Shapes.kt, and noise texture in MainActivity:
    1. Does Type.kt define all 12 M3 type roles?
    2. Is Manrope loaded correctly as variable font with 5 weights (Normal, Medium, SemiBold, Bold, ExtraBold)?
    3. Is fontFeatureSettings = "tnum" applied to display styles?
    4. Is the 96sp ExtraBold counter style the ONLY inline font override? Search for other inline size/weight overrides in composables.
    5. Are shape tokens defined and used consistently?
    6. Is noise texture correctly applied: 256x256 tileable, 2.5% opacity, Overlay blend mode?
    7. Is the noise texture asset in the correct drawable folder?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. INTENTIONAL: Manrope is a deliberate brand choice. Do not suggest other fonts.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 12 — Shared UI components & animations

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ALL files in ui/components/ (20 components):
    1. Do components accept Modifier as parameter?
    2. Are callbacks named per convention (onClick, onValueChange)?
    3. RollingCounter: handles 99→100 transition? Uses 96sp ExtraBold? tnum? Smooth animation?
    4. NumberInputField: handles decimal, empty, zero? Correct keyboard type? Validates ranges?
    5. ProjectCard: long-press menu correct per state (Active vs Completed)? Haptic on long-press? Swipe-to-dismiss smooth?
    6. ConfirmationDialog: delete uses error-colored confirm button? Dismissible via back + outside tap?
    7. Touch targets: all interactive elements at least 48dp? Check pattern repeat pills, care symbol picker, yarn card × icon.
    8. Are animations using remember/derivedStateOf correctly?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES. "I'd structure the API differently" = preference.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 13 — Compose state & recomposition safety

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review all composables in ui/screens/ and ui/components/ for Compose correctness:
    1. State hoisting: is state managed at the correct level?
    2. Are there unnecessary recompositions from unstable parameters?
    3. Is remember used correctly for expensive computations?
    4. Is derivedStateOf used where appropriate?
    5. Are lambda parameters stabilized where needed?
    6. Are any composables doing I/O, database calls, or heavy computation during composition?
    7. Is rememberSaveable used for state that must survive config changes?
    8. Are side-effects (LaunchedEffect, DisposableEffect) used correctly with proper keys?
    9. Are long-lived objects incorrectly created inside composables?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION. Do NOT invent recomposition issues without concrete evidence in code.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT rewrite composables because another style is possible.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 14 — CounterScreen & CounterViewModel

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review CounterScreen and CounterViewModel in depth (most complex screen):
    1. Is counter increment/decrement atomic and thread-safe?
    2. Is undo implemented correctly (single undo, restores previous value)?
    3. Is the counter value persisted to DB on each tap (not just on ViewModel destroy)?
    4. Does session save work on ViewModel destroy AND project switch?
    5. Is pattern repeat counter (Pro) gated correctly?
    6. Is sparkle icon (AI summary) gated behind BOTH Pro AND Nano availability?
    7. Does the stats row (STITCHES + TIME) update reactively?
    8. Is "Reset Counter" protected by confirmation dialog?
    9. Does haptic feedback fire when enabled in settings?
    10. Does keepScreenAwake work when enabled?
    11. Is the project card showing correct info (name, yarns, notes)?
    12. Can counter value go negative?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 15 — Calculator screens & domain logic

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review calculator screens (Gauge, IncDec, CastOn) and domain/calculator/:
    1. Is gauge conversion math correct? (stitches × your_gauge / pattern_gauge)
    2. Is increase/decrease evenly distributed? Do stitch counts add up exactly?
    3. Is cast-on calculation correct with pattern repeat and edge stitches?
    4. Is division by zero handled in all calculators?
    5. Are negative and zero inputs handled safely?
    6. Is rounding logic correct (no off-by-one at .5 boundaries)?
    7. Are imperial/metric conversions correct?
    8. Does "Paste Instruction" (Pro) invoke Nano InstructionParser correctly?
    9. Are results animated with AnimatedResultNumber?
    10. Is UnitToggle state persisted?
    11. Test with real values: 96→108 stitches inc/dec, 22sts/10cm gauge conversion.
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING. Trace the math through the actual code.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT rewrite formulas unless clearly mathematically wrong.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 16 — Yarn Estimator, Yarn Cards & OCR flow screens

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review YarnEstimatorScreen, YarnCardReviewScreen, YarnCardListScreen, YarnCardDetailScreen and their ViewModels:
    1. Is yarn estimation calculation correct (total yarn, per skein rounded up, weight)?
    2. Does the full OCR flow work: camera → ML Kit → Nano parse → Review → save?
    3. Is "Link to project?" dialog shown after saving a yarn card?
    4. Is camera permission requested at the right time (not on app start)?
    5. Does linking yarn to project work from both YarnEstimator and CounterScreen?
    6. Is yarn card list sorted and displayed correctly?
    7. Does yarn card detail show all fields including care symbols?
    8. Is the BottomSheet yarn picker working in CounterScreen?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 17 — Reference, Settings & ProUpgrade screens

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ReferenceHub, NeedleSizes, SizeCharts, Abbreviations, ChartSymbols, Settings, ProUpgrade screens:
    1. Is needle size data correct (2.0–25mm, Metric/US/UK/JP conversions)?
    2. Does needle size search/filter work?
    3. Are all 76 abbreviations present with descriptions?
    4. Are size charts complete (6 categories)?
    5. Do expandable items animate smoothly?
    6. Settings: do all toggles persist to DataStore? Does theme apply immediately?
    7. ProUpgrade: correct feature comparison? Purchase flow integration correct?
    8. Are empty states handled where applicable?
    9. Is loading state handled for DB-sourced data?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 18 — Coroutines & concurrency

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review coroutine usage across the entire codebase:
    1. Are all coroutine launches using proper exception handling (CoroutineExceptionHandler or try-catch)?
    2. Are there unhandled exceptions that could crash the app?
    3. Is supervisorScope used where child failure shouldn't cancel siblings?
    4. Are Flow collectors handling errors (catch operator)?
    5. Is any long-running work on the main thread (missing IO dispatcher)?
    6. Are there race conditions in state updates (concurrent counter increments)?
    7. Is viewModelScope the only scope used in ViewModels (no manual scopes leaking)?
    8. Are Flow collections in composables done safely (collectAsStateWithLifecycle)?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION. Do NOT invent race conditions without concrete code evidence showing two concurrent writes to the same state.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest stylistic coroutine rewrites unless they fix a real problem.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 19 — ML Kit OCR pipeline

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ai/ocr/ files (YarnLabelScanner.kt, YarnLabelParser.kt):
    1. Is Text Recognition v2 used (not v1)?
    2. Is camera lifecycle managed correctly (start/stop/release)?
    3. Does camera preview display correctly in Compose?
    4. What happens if OCR returns empty text? Handled gracefully?
    5. Is there visual feedback during scanning?
    6. Is regex parsing in YarnLabelParser robust against noisy OCR output?
    7. Are the 17 parser tests covering international formats (metric/imperial, different label layouts)?
    8. Is camera memory properly released after scanning?
    9. Does ML Kit run on background thread (not freezing UI)?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest cloud OCR. This is intentionally on-device.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 20 — Gemini Nano integration

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ai/nano/ files (InstructionParser.kt, YarnLabelNanoParser.kt, ProjectSummarizer.kt, NanoAvailability.kt):
    1. NanoAvailability: cached or checked every time? UI hidden (not just disabled) when unavailable? Timeout? Background thread?
    2. InstructionParser: prompts well-structured? Regex fallback covers same cases? 40+ typo corrections comprehensive? Garbage Nano response handled? Timeout?
    3. YarnLabelNanoParser: parses OCR to structured fields? Handles noise? Regex fallback matches Nano capability?
    4. ProjectSummarizer: what data goes to Nano? simpleSummary fallback useful? BottomSheet with loading state?
    5. Is every Nano call in try-catch? Are timeouts set?
    6. Is the Nano model loaded lazily (not at app start)? Released when unused?
    7. Is feature gating consistent with Pro access?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest online AI. Do NOT speculate about model quality.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 21 — Pro features, billing & trial logic

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ALL files in pro/ and billing/:
    1. ProManager/ProState: how is Pro determined? Cached StateFlow? No Play Store fallback?
    2. ProFeature enum: all features enumerated? Cross-reference PROJECT.md. Free features accidentally gated? Pro accidentally free?
    3. TrialManager: start date tamper-resistant? Clock manipulation handled? Silent during trial? Banner text? Post-expiry = soft lock?
    4. Do 8 TrialManager tests cover: fresh install, mid-trial, expired, clock change, reinstall?
    5. Billing: Play Billing Library version? acknowledgePurchase? Failure/cancel/pending? Restore on reinstall? Offline verification? Connection closed?
    6. After purchase: does UI update immediately without restart?
    7. Edge: refund handling? ITEM_ALREADY_OWNED? No GMS fallback?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION. Do NOT invent security claims without code evidence.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT add subscription logic or redesign monetization.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 22 — Glance widget

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review widget/ files (CounterWidget.kt, CounterWidgetState.kt):
    1. Uses Jetpack Glance (not legacy RemoteViews)?
    2. Widget receiver in AndroidManifest? Widget info XML correct (preview, size, resize)?
    3. How does widget get counter value? Efficient (not polling)?
    4. Does widget update when counter changes in app?
    5. Race condition between app and widget writes?
    6. Deep-link to counter screen works? PendingIntent FLAG_IMMUTABLE?
    7. What shows for free users? Graceful degradation?
    8. No active project handling? App data cleared handling?
    9. Widget preview image provided?
    10. Does widget style match app (gold, warm aesthetic)?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 23 — Memory, resources & process death

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review entire codebase for resource lifecycle and process death:
    1. Context retention risks (Activity/Fragment leaked via ViewModel or singleton)?
    2. BillingClient lifecycle: connected/disconnected correctly?
    3. Camera resources released after OCR?
    4. Nano model released when unused?
    5. Noise texture bitmap: sized correctly (256x256)? Not recreated on each recomposition?
    6. Long-lived observers/callbacks that could leak?
    7. Process death: is counter value persisted per-tap (not just on ViewModel destroy)?
    8. Do dialogs survive configuration changes?
    9. Is navigation state restored after process death?
    10. Can sessions overlap in time for same project?
    11. What happens if yarnCardIds references a deleted yarn card?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION. Only report risks traceable to actual code patterns.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 24 — Input validation & null safety

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Scan the entire codebase for crash-prone patterns:
    1. Force-unwraps (!!) — list every instance. Are they justified or crash risks?
    2. Division by zero in calculators — is it guarded in all paths?
    3. NumberFormatException risks from parsing user input?
    4. Navigation argument parsing — what if cardId or projectId is invalid?
    5. Nullable types from Room/DataStore — handled safely?
    6. Array/list index out of bounds risks?
    7. What happens if user pastes non-numeric text into NumberInputField?
    8. Maximum lengths for project names, notes — enforced or unbounded?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE. Force-unwraps: one finding listing all instances.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 25 — Accessibility

```xml
<code_review>
  <role>Senior Android engineer and accessibility specialist. Report only.</role>
  <scope>
    Review entire ui/ package for accessibility:
    1. Do ALL interactive elements have contentDescription? Decorative = null?
    2. Are descriptions meaningful (not just "button")?
    3. Are touch targets at least 48dp? Check: - button, pattern repeat pills, care symbol picker, yarn × icon.
    4. Is reading order logical? Headings marked with semantics { heading() }?
    5. Is there an alternative to long-press for context menu? Alternative to swipe-to-dismiss?
    6. Can RollingCounter value be read by TalkBack (live region)?
    7. Is color never the sole indicator of state?
    8. Does the app respect system font size and display size settings?
    9. Does the app respect "Reduce motion" system setting?
    10. Is camera viewfinder described for screen readers?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE. Missing contentDescription across files: one finding, list all.
    9. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 26 — Test quality & coverage gaps

```xml
<code_review>
  <role>Senior Android engineer, strict code review. Report only.</role>
  <scope>
    Review ALL test files (test/ and androidTest/):
    1. Existing test quality: descriptive names? Specific assertions? Realistic data? Independent tests?
    2. Calculator tests (48): boundary values? Float precision? Division by zero? Imperial/metric?
    3. Parser tests: cover noisy/international input?
    4. Migration tests (3): use real data? Verify integrity? Full chain v1→v3?
    5. Coverage gaps — what's NOT tested: Repositories? ViewModels (CounterViewModel!)? ProManager? Billing? Navigation? DataStore? Compose UI?
    6. Top 10 most important missing tests ranked by risk.
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code or add tests.
    2. EVIDENCE REQUIRED. When noting a gap, point to the untested code with file path.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK IF COVERAGE IS GOOD. Don't demand 100%.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. Do NOT suggest UI tests unless clearly justified by a concrete risk.
    9. ONE FINDING PER ISSUE.
    10. Focus on risk-based coverage, not coverage percentage.
  </rules>
  <output>
    ### Test Quality
    What's good about existing tests.
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Coverage Gaps
    [GAP] Area — Risk.
    ### Top 10 Missing Tests
    Ranked by risk.
    ### Summary
  </output>
</code_review>
```

---

## 27 — Release readiness

```xml
<code_review>
  <role>Senior Android engineer, pre-release audit. Report only.</role>
  <scope>
    Final release checklist:
    1. App icon in all densities (mdpi–xxxhdpi)? Adaptive icon format?
    2. Is a privacy policy URL needed (required for IAP apps on Play Store)?
    3. Third-party library licenses: any requiring attribution? Is Manrope license commercial-compatible?
    4. Does targetSdk 36 meet current Play Store requirements?
    5. Are targetSdk 36 behavior changes handled (predictive back, etc.)?
    6. Are all user-facing strings in strings.xml? Any hardcoded strings in Compose?
    7. What languages are supported? Is default language set?
    8. Is RTL layout considered?
    9. Play Console crash dashboard will be the only crash visibility — is there a global uncaught exception handler that saves state?
    10. Widget preview image provided for widget picker?
    11. Feature graphic and screenshots available?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code, why it's a problem.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. INTENTIONAL: no crash reporting is deliberate. Do NOT suggest adding Firebase Crashlytics.
    9. ONE FINDING PER ISSUE.
    10. BUGS NOT PREFERENCES.
  </rules>
  <output>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Release Blockers
    [BLOCKER] What and why. If none: "No blockers found."
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </output>
</code_review>
```

---

## 28 — UI spec compliance (layout & hierarchy)

```xml
<code_review>
  <role>Senior Android engineer comparing implementation against design spec. Report only.</role>
  <scope>
    Compare each screen's Compose implementation against PROJECT.md spec. Check one tab at a time. Start with Projects tab:

    **CounterScreen — verify these layout details:**
    1. Project card: is it a compact surface card with CURRENT PROJECT label + sparkle icon + chevron (>) in that order?
    2. Project name: tappable for rename?
    3. "+ Add yarn" and note icon inside the project card (not outside)?
    4. Linked yarns inside the card with × dismiss icon?
    5. Counter: TOTAL ROWS label above the number? RollingCounter at 96sp ExtraBold?
    6. Pattern repeat: compact pill/chip below counter with −/+ inside the pill?
    7. Buttons: − at 48dp outlined muted, + at 72dp gold fill, undo at 48dp outlined muted?
    8. Stats row: STITCHES + TIME side by side below buttons, surfaceVariant background? TIME tappable → Session History?
    9. Reset Counter: muted bodySmall below stats row?

    **ProjectListScreen:**
    10. ACTIVE / COMPLETED sections with labelSmall dusty rose headers?
    11. ProjectCard: name, row count (gold), last updated, chevron?
    12. FAB "+ New Project"?

    **Tools tab:**
    13. Tools list uses HubListItem components in correct order: Gauge, Inc/Dec, Cast On, Yarn Estimator?
    14. QuickTipCard present? Randomized tip?

    Flag ONLY differences between spec and implementation. If implementation matches spec, report OK.
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code showing what's different from spec.
    3. NO SPECULATION. Only flag what you can verify by reading the composable code.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING. If layout matches spec perfectly, say so.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. This is about SPEC COMPLIANCE, not style preferences. "I'd lay it out differently" is irrelevant.
    9. ONE FINDING PER ISSUE.
  </rules>
  <o>
    ### Matches Spec
    [OK] Screen/element — matches spec as described.
    ### Deviations from Spec
    [DEVIATION] File:line — `code` — Spec says X, implementation does Y.
    ### Could Not Verify
    ### Summary
  </o>
</code_review>
```

---

## 29 — Dark/light parity & state variants

```xml
<code_review>
  <role>Senior Android engineer reviewing visual completeness. Report only.</role>
  <scope>
    Review ALL screens in ui/screens/ for two things:

    **A) Dark/light theme parity:**
    1. Are there elements that use theme-aware colors in light but hardcoded values in dark (or vice versa)?
    2. Are there conditional color branches (if dark then X else Y) that bypass the theme system?
    3. Are any icons or assets only provided for one theme?
    4. Is the noise texture overlay correctly applied ONLY in dark theme?
    5. Are dividers, borders, shadows visible and appropriate in both themes?

    **B) State variants — for EACH screen, check if these are implemented:**
    6. Empty state: what shows when there's no data? (No projects, no yarn cards, no sessions, no search results)
    7. Loading state: is there a loading indicator when data comes from Room on first load?
    8. Error state: what happens if a DB read fails?

    List which screens have empty/loading/error states and which are missing them.
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code.
    3. NO SPECULATION.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING. If all states are handled, say so.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <o>
    ### Dark/Light Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### State Variant Coverage
    | Screen | Empty | Loading | Error |
    For each: Implemented / Missing / N/A
    ### Findings
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </o>
</code_review>
```

---

## 30 — Layout edge cases & overflow

```xml
<code_review>
  <role>Senior Android engineer reviewing layout robustness. Report only.</role>
  <scope>
    Review composables for layout edge cases:

    1. **Long text overflow:** What happens when project name is very long (50+ chars)? Does it ellipsize or break layout? Check: ProjectCard, project card in CounterScreen, yarn card names, abbreviation descriptions.
    2. **Large system font size:** Does the 96sp counter number respect system font scaling? If a user has font scale 2.0, does 96sp become 192sp and break the layout? Is there a cap?
    3. **Small screens:** Do layouts work on a 5-inch screen (320dp width)? Are there fixed-width elements that could overflow?
    4. **Keyboard overlap:** When NumberInputField gets focus and keyboard appears, is the input still visible (not covered by keyboard)?
    5. **Landscape orientation:** Do screens handle landscape, or is orientation locked? If locked, is it declared in manifest?
    6. **Scrollability:** Are screens with variable content height scrollable? Especially: CounterScreen (project card + counter + buttons + stats), calculator result areas, yarn card detail.
    7. **Bottom nav overlap:** Is content not hidden behind the bottom navigation bar? Are insets applied correctly?
    8. **Dialog sizing:** Do rename dialog and notes BottomSheet work on small screens?
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. Exact file, line, quoted code showing the layout constraint (or lack of).
    3. NO SPECULATION. Only flag issues you can verify from the composable code (e.g., Text without maxLines or overflow parameter, fixed dp values that exceed small screens).
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN. Many layout issues are LIKELY (can't run on device).
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER ISSUE.
    9. BUGS NOT PREFERENCES.
  </rules>
  <o>
    ### Findings
    [SEVERITY] [CONFIDENCE] File:line — `code` — Issue.
    ### Checked OK
    ### Could Not Verify
    ### Summary
  </o>
</code_review>
```

---

## 31 — Code consistency & scattered patterns

```xml
<code_review>
  <role>Senior Android engineer reviewing codebase consistency. Report only.</role>
  <scope>
    Scan the ENTIRE codebase for inconsistent patterns — places where the same thing is done differently without reason:

    1. **Pro feature gating:** Is ProManager/ProState checked the same way in every screen that gates features? Or does each screen have its own ad-hoc check?
    2. **Loading state pattern:** Is loading handled consistently across screens (same component/pattern), or does each screen invent its own approach?
    3. **Error handling pattern:** Is error handling consistent across ViewModels, or does each one do it differently?
    4. **Nano availability check:** Is it checked the same way everywhere, or scattered with different logic?
    5. **Navigation pattern:** Do all screens navigate the same way (navigate() calls), or are there mixed approaches?
    6. **State modeling:** Do all ViewModels model UI state consistently (e.g., all use data class UiState vs some use scattered StateFlows)?
    7. **Scaffold usage:** Do all tool screens use ToolScreenScaffold, or do some build their own scaffold?
    8. **String handling:** Are all user-facing strings in strings.xml, or do some screens have inline strings?
    9. **Spacing and padding:** Are spacing values from a consistent scale (4/8/12/16/24/32dp), or are there arbitrary values like 13dp, 7dp scattered around?
    10. **Import style:** Are there wildcard imports (import x.*) mixed with specific imports?

    For each inconsistency: show the different patterns used, which files use which, and which pattern is most common (likely the "correct" one).
  </scope>
  <rules>
    1. REPORT ONLY. Do NOT modify any code.
    2. EVIDENCE REQUIRED. For each inconsistency, quote the different patterns with file:line references.
    3. NO SPECULATION. Only report patterns you can verify across multiple files.
    4. VERIFY BEFORE REPORTING.
    5. IT'S OK TO FIND NOTHING. A consistent codebase is a valid result.
    6. CONFIDENCE: CERTAIN / LIKELY / UNCERTAIN.
    7. CAN'T FIND A FILE? Say so.
    8. ONE FINDING PER INCONSISTENCY. Show all files involved.
    9. BUGS NOT PREFERENCES. "I'd do it a third way" is irrelevant. Only flag that the codebase does it 2+ different ways.
    10. Do NOT suggest architecture changes. Only report the inconsistency.
  </rules>
  <o>
    ### Inconsistencies Found
    For each:
    [SEVERITY] [CONFIDENCE]
    Pattern A: `code` — used in: File1.kt, File2.kt
    Pattern B: `code` — used in: File3.kt
    Most common: Pattern A (likely intended).

    ### Consistent Patterns (OK)
    [OK] What you checked and found consistent.
    ### Could Not Verify
    ### Summary
  </o>
</code_review>
```

---

## Bonus — Self-verification follow-up

Use after ANY review:

```xml
<self_review>
  <instructions>
    Review your findings from the previous message. For each finding:
    1. Re-read the actual code you cited. Is it still valid?
    2. Did you verify it in code, or did you assume it?
    3. Did you quote actual code, or paraphrase/imagine it?
    4. Would a senior engineer agree it's a real issue, not a style preference?

    Remove any finding that fails. List removed ones under "## Retracted" with reason.
    If all survive: "All findings verified. No retractions."
  </instructions>
</self_review>
```
