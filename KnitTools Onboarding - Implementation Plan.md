# KnitTools Onboarding: Implementation Plan

Status: **historical implementation baseline**. The phased plan below records the approved direction on 2026-10-29; implementation has since progressed. See [PROJECT.md](PROJECT.md) for implemented behavior and its dated validation records. This plan is not a current completion or test-status report.
Date: 2026-10-29.
Scope: first-run experience, in-context teaching, in-app guide, Pro presentation, celebrations.
Language: English ships first. All other locales follow in a dedicated phase before the Play release.

This plan combines the strongest parts of two proposals:

- `KnitTools Onboarding - suunnitelma.md` (Claude): interactive demo with the real 3D plus button, growing fabric swatch and reading line, starting count with "Next up" helper, live project card preview, stitch progress row, project setup list, stitch-marker tips, celebrations, truthful Pro marker with a bespoke look.
- Codex's proposal: non-blocking start state on the Projects tab with the bottom navigation visible, a tools-only path, no questionnaire, craft asked in the project form, an in-app task-based guide, a visible "Trial" marker state, strict tip rules, benefit-first Pro prompt copy, the six-task usability test.

Where this plan differs from the earlier Finnish plan, this plan wins. The earlier plan stated that the pattern picker opens automatically after a new project. That is not true: `CounterScreen` opens `PatternPickerSheet` only from the pattern tile, the documents sheet or the project actions sheet.

---

## Table of contents

1. Product decisions
2. User journey
3. Verified facts the plan depends on
4. Architecture overview
5. Phase 0: Mockup refresh and sign-off
6. Phase 1: Foundations
7. Phase 2: First project (starting count, target, preview)
8. Phase 3: Projects start state
9. Phase 4: Stitch-marker tips
10. Phase 5: Project setup list
11. Phase 6: In-app guide
12. Phase 7: Pro presentation
13. Phase 8: Celebrations
14. Phase 9: Pro copy, Pro page and trial journey
15. Phase 10: Translations (release gate)
16. Phase 11: Entitlement rebalancing (decision-gated)
17. English copy catalogue
18. Analytics
19. Testing strategy
20. Documentation updates
21. Usability test and acceptance criteria
22. Risks
23. Out of scope

---

## 1. Product decisions

| # | Decision | Source |
| --- | --- | --- |
| P1 | Core promise: **"Pick Up Where You Left Off."** KnitTools is the memory of your craft work: count, place in the pattern, materials and progress stay together. | Codex wording, Claude framing |
| P2 | No full-screen onboarding route and no questionnaire. The first touch is a **start state inside the Projects tab** with the bottom navigation visible. It replaces the empty list only while the user has no projects at all. | Codex |
| P3 | The start state contains an **interactive example card**: the real `counter_plus_button`, a growing `FabricSwatch` and a small pattern page with a moving reading line. It is labeled "Example" and never touches real data. | Claude visuals, Codex sandbox rules |
| P4 | Three exits from the start state: **New Project** (primary, inline `LabeledCounterImageButton`), **"I just need the tools"** (text action to Tools), **close** (hides the full start state, a compact empty state remains). | Codex paths, Claude components |
| P5 | Craft is chosen in the project form, not upfront. The last chosen craft becomes the default next time. | Codex, plus implicit personalization |
| P6 | The project form gains an optional **"Rows already done"** field with a live **"Next up: row 38"** helper, an optional **target** field and, for the very first project, a **live `ContinueProjectCard` preview**. | Claude (unambiguous wording), Codex (optional field) |
| P7 | After creation the app opens the Counter as today. A tip on the plus button explains the first row; after the first tap a tip teaches Undo. The user is never forced to tap. | Both |
| P8 | The first project gets a **Project setup** section on its project page: 4 free items, progress drawn as a stitch row, items already done in the form are already knitted. | Claude, limited to free items per Codex |
| P9 | In-context teaching uses **stitch-marker tips** with strict rules: one at a time, at most two automatic tips per app process, never covering counter controls, dismissed tips stay dismissed, "Show tips again" in the guide and Settings. | Claude catalogue, Codex rules |
| P10 | An offline **in-app guide, "How to Use KnitTools"**, organized by task (5 topics). Entry points: Settings Info and the project page footer. The external Help and Guide link stays. | Codex |
| P11 | Pro marker states: **Locked** shows a woven "PRO" label, **Trial** shows an outlined woven "TRIAL" label, purchased, unknown and already-unlocked content show nothing. One resolver decides marker and prompt. | Codex states, Claude visual and resolver |
| P12 | Hidden Pro features become visible: the compact repeat counter and the instruction paste button show a locked action with the marker. The widget gets an in-app entry ("Add widget to home screen"). | Both |
| P13 | Celebrations stay quiet and on-brand: target reached tip, first saved work session tip, a **"Bound Off!" / "Fastened Off!"** sheet when a project is completed. No confetti, no sounds, no streak pressure. | Claude, Codex's tone on breaks |
| P14 | Entitlements do **not** change in this plan's main phases. Onboarding works with today's free tier (counter, PDF, web patterns, project yarn, sessions, backup). Rebalancing is a separate decision-gated phase. | Codex sequencing, Claude recommendations |
| P15 | No notification permission is requested. There are no notifications. | Both |
| P16 | English first in a dedicated resource file; all locales before release. | User request |

---

## 2. User journey

### 2.1 Brand-new user

1. App opens on Projects (start destination unchanged). Bottom navigation visible.
2. The full start state shows: Example card, headline, body, trust line, inline **New Project** pill, "I just need the tools", close icon.
3. Optional: the user taps the example plus. Count 23 → 24, a row knits onto the swatch, the reading line moves one row, caption changes, Undo appears. Second tap: "That's it. KnitTools remembers your place, so you don't have to."
4. **New Project** opens `ProjectDetailsSheet` (create mode, first-project variant): live preview card, name, craft, counting unit, rows already done, target.
5. Create opens the Counter (existing behavior). Tip **first_row** points at the plus button.
6. First real increment: tip **undo** ("Row 38 is saved...").
7. Later visits: tips appear when behavior shows the need (step size, work session, project page, pattern, keep screen on, backup, folders, widget).
8. On the project page, **Project setup** shows 1–4 of 4 done.

### 2.2 Tools-only user

"I just need the tools" switches to the Tools tab and marks the full start state dismissed. Projects later shows the compact empty state (swatch, "What's on your needles?", New Project pill). Tools gets tip **tools_intro** on first visit.

### 2.3 Existing user (has any project)

Never sees the start state. On first launch of this version, basic tips (`first_row`, `undo`, `overview_intro`, `step_size`) are marked seen. Other tips can still appear when their triggers fire. The guide is available.

### 2.4 Old but empty install

Sees the start state like a new user. It is non-blocking, so this is harmless.

### 2.5 External entry on first launch

Shared PDF, Open-with PDF, shared Ravelry link, widget launch, counter launch request and Pro launch run exactly as today. The start state is simply the Projects empty state, so nothing needs to be deferred. If the external flow creates a project (for example Save Pattern → Start new project), the start state disappears because a project exists. That project is not a starter project (no setup list), but tips still apply.

### 2.6 Restore from backup

After a successful replacement restore, mark the basic tips seen. The start state disappears naturally if projects were restored.

---

## 3. Verified facts the plan depends on

| Fact | Where |
| --- | --- |
| Start destination is always Projects | `MainActivity.kt` (`startDestination = TopLevelDestination.Projects.route`) |
| Empty Projects shows ACTIVE (0) and `no_active_projects` | `ProjectListScreen.kt` `ProjectListContent`, around line 1002 |
| Continue hero only exists for an active project with count > 0 | `ProjectListContent.kt`, PROJECT.md "Sorting and visibility" |
| Creation path | `ProjectListViewModel` → `ProjectCreationActions.create(PendingProjectCreation)` → `CounterRepository.createProject` → `insertProjectInCurrentTransaction` |
| `PendingProjectCreation` is `Serializable`, stored in `SavedStateHandle`, `serialVersionUID = 1L` | `ProjectCreationActions.kt` |
| New projects always start at count 0. No set-count operation exists. | `CounterRepository.insertProjectInCurrentTransaction`, `MainCounterChange` |
| Target input: digits only, max 5 characters, must be > 0 | `TargetRowsDialog.kt`, `CounterViewModel.setTargetRows` |
| `ProjectDetailsSheet` uses Material `TextField` with an inner label for the name | `ui/components/ProjectDetailsSheet.kt` |
| Work session Start lives in the project actions sheet opened from the counter top bar icon (`project_actions_title`) | `ProjectActionsBottomSheet.kt` `WorkSessionActionRow`, `CounterScreen.kt` around line 1762 |
| Pattern picker opens only on user action | `CounterScreen.kt` lines ~498, ~738, ~1031 |
| Reading line follows the main counter only when follow is on. Labels: "Show reading line", "Follow main counter", "Return to current count", "Calibrate counter positions" | `values/strings.xml`, `values/pattern_reading_assistance.xml` |
| Lint `MissingTranslation` is enabled, `abortOnError = true`, `checkReleaseBuilds = true` | `app/build.gradle.kts` `lint {}` |
| A separate resource file already exists: `values/pattern_reading_assistance.xml` | precedent for `strings_onboarding.xml` |
| The middle-dot test reads only `values*/strings.xml` | `ProLocalizationSourceTest` "user visible locale resources reject the middle dot separator" |
| Separate DataStores precedent: `review_state` (`InAppReviewManager`), `trial_state` (`TrialManager`) | `pro/` |
| Preferences are excluded from backup | `data/backup` has no preference handling |
| `ProBadge` renders identical visuals for Trial and Locked | `ui/components/ProBadge.kt` |
| `PasteInstructionButton` returns nothing when `!isPro` | `ui/components/PasteInstructionButton.kt` |
| Compact secondary counter is hidden unless `canUseSecondaryCounter` | `CounterUiStateReducers.kt`, `CounterWorkspaceSections.kt` |
| Widget receiver | `widget/CounterWidgetReceiver.kt` (`GlanceAppWidgetReceiver`) |
| Navigation hidden routes | `TopLevelNavigation.kt` `HIDE_NAVIGATION_ROUTES` |
| Analytics screen allowlist lacks `project_overview` | PROJECT.md "Optional release usage analytics" |
| Finnish bug: `counter_round_value_of_target_format` is "Kerros %1$d / %2$d", should be "Kierros" | `values-fi/strings.xml` line 87 |

---

## 4. Architecture overview

### 4.1 New state store: `onboarding_state`

A separate Preferences DataStore (pattern of `review_state`), owned by a new `OnboardingStateRepository` (`@Singleton`, Hilt). Not part of backup.

| Key | Type | Meaning |
| --- | --- | --- |
| `initialized_version` | Int | 0 = never initialized. Set to `ONBOARDING_STATE_VERSION = 1` after first-launch seeding. |
| `welcome_dismissed` | Boolean | Full start state was closed or the tools-only path was taken |
| `starter_project_id` | Long | Project created while zero projects existed. Absent = none. |
| `project_setup_hidden` | Boolean | User hid the setup section |
| `project_setup_celebrated` | Boolean | "All set" line was shown once |
| `tips_seen` | Set<String> | `TipId.key` values that were shown and dismissed or acted on |
| `first_use_epoch_day` | Long | Local epoch day of first launch with this version |
| `counter_open_count` | Int | Number of Counter route entries |
| `counter_open_days` | Int | Number of distinct local days with a Counter entry |
| `counter_open_last_day` | Long | Last epoch day counted in `counter_open_days` |
| `first_session_tip_pending` | Boolean | Set when the first ever work session is saved |
| `target_celebrated_projects` | Set<String> | Project ids whose target-reached tip was shown (capped at 200, oldest dropped) |
| `backup_created` | Boolean | Set when a backup export succeeds in this install (suppresses the backup tip) |

Read failures return defaults. Corruption uses `ReplaceFileCorruptionHandler { emptyPreferences() }` like `review_state`.

One general preference goes to the existing `knittools_preferences` via `PreferencesManager`:

| Key | Type | Meaning |
| --- | --- | --- |
| `last_project_craft_type` | String? | `CraftType.name` of the last created project. Default for the create form. Absent = KNITTING. |

### 4.2 New and changed modules

```
app/src/main/java/com/finnvek/knittools/
  repository/OnboardingStateRepository.kt          new
  domain/tips/TipId.kt                             new
  domain/tips/TipRules.kt                          new (pure Kotlin)
  domain/calculator/ProjectStartValues.kt          new (parsing and validation)
  pro/ProActionAccess.kt                           new
  ui/components/FabricStrokes.kt                   new (extracted from FabricSwatch.kt)
  ui/components/FabricSwatch.kt                    changed (optional fixed scale)
  ui/components/StitchProgressRow.kt               new
  ui/components/ProMarker.kt                       new (replaces ProBadge.kt, which is deleted)
  ui/components/tips/StitchMarkerTip.kt            new
  ui/components/tips/TipHost.kt                    new
  ui/components/tips/TipAnchor.kt                  new
  ui/components/ProjectDetailsSheet.kt             changed
  ui/components/SheetOptionRow.kt                  unchanged in this plan
  ui/screens/project/start/ProjectsStartState.kt   new
  ui/screens/project/start/WelcomeExampleCard.kt   new
  ui/screens/project/start/PatternPageSketch.kt    new
  ui/screens/project/ProjectSetupSection.kt        new
  ui/screens/project/ProjectCompletedSheet.kt      new (Phase 8)
  ui/screens/guide/GuideScreen.kt                  new
  ui/screens/guide/GuideTopicScreen.kt             new
  ui/screens/guide/GuideContent.kt                 new (static topic model)
  ui/theme/OnboardingDimens.kt                     new
app/src/main/res/values/strings_onboarding.xml     new (English only until Phase 10)
```

### 4.3 Data flow

- `ProjectListViewModel` adds `hasAnyProject: Boolean` (unfiltered, active or completed) and `welcomeDismissed: Boolean` to its UI state.
- `CounterViewModel` exposes `tipSignals: SharedFlow<TipSignal>` (increment with timestamp, completed save) in addition to its state. `TipHost` on each screen combines screen state, signals and `OnboardingStateRepository` into a `TipContext` and asks `TipRules.next(context)`.
- `ProActionAccess` is computed from `ProManager.proState`, its readiness and a per-action context. Every marker and every prompt decision uses it.

---

## 5. Phase 0: Mockup refresh and sign-off

The current mockups (`reports/mockups/onboarding/`) show a full-screen welcome route. This plan places the welcome inside the Projects tab. Before coding, update the HTML prototype and render both themes at 360 × 800:

1. Full start state in the Projects tab, bottom navigation visible, Example card with plus and Undo, headline, body, trust line, inline New Project pill, tools text action, close icon.
2. Compact start state.
3. `ProjectDetailsSheet` first-project variant with live preview, starting count, "Next up" helper and target.
4. Counter with tip `first_row` (bubble above the plus button, stitch marker on its edge).
5. Project page with Project setup (2/4 done).
6. Guide root and one topic page.
7. `ProMarker` locked and trial states in a sheet row and in the New Project pill.

**Exit criteria:** user approves visuals. Memory rule: show mockups before code.

---

## 6. Phase 1: Foundations

### 6.1 Onboarding state store

- Create `OnboardingStateRepository` with the keys in 4.1, exposing:
  - `val state: Flow<OnboardingState>` (data class with all fields, safe defaults)
  - `suspend fun initializeIfNeeded(hasAnyProject: Boolean, today: Long)`: if `initialized_version < 1`, write `first_use_epoch_day = today`; if `hasAnyProject`, add basic tips to `tips_seen`; set `initialized_version = 1`.
  - `suspend fun dismissWelcome()`
  - `suspend fun setStarterProject(projectId: Long)` (only if no starter project yet)
  - `suspend fun hideProjectSetup()`, `suspend fun markProjectSetupCelebrated()`
  - `suspend fun markTipSeen(id: TipId)`, `suspend fun resetTips()` (clears `tips_seen` only)
  - `suspend fun recordCounterOpen(today: Long)`
  - `suspend fun markBasicTipsSeen()` (used after restore)
  - `suspend fun setFirstSessionTipPending(pending: Boolean)`
  - `suspend fun markTargetCelebrated(projectId: Long)`
  - `suspend fun markBackupCreated()` (called from the backup export success path)
- Call `initializeIfNeeded` from `App` application scope after the database is available (`counterRepository.hasAnyProject()`), never with `runBlocking`. Cancellation is rethrown, other failures are logged and ignored.
- Add `CounterProjectDao.observeHasAnyProject(): Flow<Boolean>` (`SELECT EXISTS(SELECT 1 FROM counter_projects)`) and `suspend fun hasAnyProject(): Boolean`. Expose through `CounterRepository`.

### 6.2 Last craft preference

- `PreferencesManager`: add `KEY_LAST_PROJECT_CRAFT = stringPreferencesKey("last_project_craft_type")`, `AppPreferences.lastProjectCraftType: CraftType = CraftType.KNITTING`, `suspend fun setLastProjectCraftType(type: CraftType)`. Unknown stored names fall back to KNITTING.

### 6.3 English-first resources

- Create `app/src/main/res/values/strings_onboarding.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="MissingTranslation">
    <!-- Onboarding, tips, guide and Pro marker copy. English first; translations in Phase 10.
         Remove tools:ignore before the Play release. -->
</resources>
```

- All new user-visible strings from this plan go into this file. Existing translated strings are not edited in English-only phases (that would silently make 11 translations stale). Copy rewrites of existing keys happen in Phase 9 together with translations.
- Extend the middle-dot test in `ProLocalizationSourceTest` to scan every `values*/strings*.xml` and `values*/pattern_reading_assistance.xml` file, not only `strings.xml`.
- Add `OnboardingResourcesSourceTest`:
  - every key in `strings_onboarding.xml` is referenced from main source (no dead strings)
  - no `·`, `•` or ` | ` in any onboarding string
  - placeholders in any locale copy match English (when a locale file exists)
  - screen-title keys (suffix `_screen_title`) are Title Case in English

### 6.4 Finnish round label fix

- `values-fi/strings.xml`: `counter_round_value_of_target_format` → `Kierros %1$d / %2$d`. Add an assertion to `CounterStringResourcesSourceTest` that the Finnish rounds format does not start with "Kerros".

### 6.5 Analytics plumbing

- Add events (section 18) to `UsageEvent`. Add `project_overview`, `guide` and `guide_topic` to the screen allowlist in `PostHogAnalytics`.

**Acceptance:** unit tests for `OnboardingStateRepository` (defaults, corruption, seeding with and without projects, idempotent initialization), resources tests green, `./gradlew lint` green with English-only strings.

---

## 7. Phase 2: First project (starting count, target, preview)

### 7.1 Domain validation

`domain/calculator/ProjectStartValues.kt`:

```kotlin
const val MAX_PROJECT_START_COUNT = 99_999
const val MAX_TARGET_ROWS = 99_999

/** Tyhjä tarkoittaa nollaa. Vain numerot, 0–99 999. */
fun parseProjectStartCount(text: String): Int?

/** Tyhjä tarkoittaa ei tavoitetta (TargetInput.NoTarget). Vain numerot, 1–99 999. */
fun parseTargetRows(text: String): TargetInput  // NoTarget, Valid(Int), Invalid
```

`TargetRowsDialog` switches to `parseTargetRows` so there is one rule for targets (no duplicate logic). Keep its 5-digit input filter.

### 7.2 Repository

`CounterRepository.createProject(...)` gains `initialCount: Int = 0` and `targetRows: Int? = null`, forwarded to `insertProjectInCurrentTransaction`:

- Validate `initialCount in 0..MAX_PROJECT_START_COUNT` and `targetRows == null || targetRows in 1..MAX_TARGET_ROWS`. Otherwise return `ProjectCreationResult.InvalidProject`.
- Insert `CounterProject(count = initialCount, targetRows = targetRows, ...)`.
- **No** `counter_history` row, **no** session checkpoint, **no** linked-counter delta, **no** reading-line move. The starting count is the initial state, so Undo has nothing to undo and cannot go below it.
- Quota check stays first and inside the same transaction.
- `createProjectWithImportedPdf` is unchanged.

### 7.3 Creation plumbing

- `PendingProjectCreation`: add `initialCount: Int = 0`, `targetRows: Int? = null` and `payloadVersion: Int = 2`, retaining `serialVersionUID = 1L`. Drop restored legacy payloads (`payloadVersion == 0`) in the `savedStateHandle` getter and let the user resubmit (no crash). User-approved correction on 2026-10-29: Lifecycle 2.11.0 deserializes Bundle values before constructing SavedStateHandle, so changing the Java serial UID would fail before the proposed getter could catch it.
- `ProjectCreationActions.createProjectInternal` passes both values.
- On `ProjectCreationResult.Created`:
  - `preferencesManager.setLastProjectCraftType(request.craftType)`
  - if `wasFirstProject` (captured before insert: `!counterRepository.hasAnyProject()`), `onboardingStateRepository.setStarterProject(projectId)`
- `ProjectDetailsValues` gains `initialCount: Int` and `targetRows: Int?` (edit mode ignores them).

### 7.4 `ProjectDetailsSheet`

New parameters:

```kotlin
fun ProjectDetailsSheet(
    ...,
    mode: ProjectDetailsMode = ProjectDetailsMode.Edit,   // Create or Edit
    showPreview: Boolean = false,                          // true only when no project exists
)
```

Create mode layout inside the shared `FormSheet` (top to bottom):

1. **Preview** (only `showPreview`): `ContinueProjectCard(project = draftProject, sessionStatus = null, onClick = null, onClickLabel = null, onOpenCounter = null)`. Draft name falls back to the placeholder text while empty. Semantics: one node, content description from `project_create_preview_a11y`, live region polite.
2. **Name**: switch from Material `TextField` to the shared `LabeledTextField` (consistency rule: text and number fields in the same form look identical). Placeholder per craft: `project_name_placeholder_knitting` "Striped socks", `project_name_placeholder_crochet` "Granny square blanket".
3. **Craft** (`CraftTypeToggle`, unchanged). Initial value from `lastProjectCraftType`.
4. **Count** label (`MainCounterLabelToggle`, unchanged) and custom label field.
5. **Already done** (`NumberInputField`, optional): label per label type (`project_start_count_label_rows`, `_rounds`, `_repeats`, `_custom`), placeholder `0`. Supporting text:
   - empty or 0: `project_start_count_help_new` "Leave at 0 for a new project."
   - N ≥ 1: `project_start_count_next_rows` "Next up: row %1$d" with N + 1 (rounds, repeats, custom variants).
   - invalid: `project_start_count_error`, Create disabled.
6. **Target** (`NumberInputField`, optional): label `project_target_label` "Target (optional)", supporting text `project_target_help`, invalid → `project_target_error`, Create disabled.

Edit mode shows only name, craft and count label, as today, but with `LabeledTextField` for the name.

All drafts are `rememberSaveable`. Focus order: name → already done → target, IME Next/Done. When opened from the start state, initial focus goes to the name field.

Callers:
- `ProjectListScreen` create path: `mode = Create`, `showPreview = !state.hasAnyProject`.
- Edit details (overview overflow): `mode = Edit`.

### 7.5 Tests

- `ProjectStartValuesTest`: empty, 0, 99999, 100000, signs, spaces, decimals, overflow.
- `CounterRepositoryCreateProjectTest` (Robolectric or instrumented, matching existing repository tests): count and target persisted, no history row, quota still enforced, invalid values rejected, Undo after creation is a no-op.
- `ProjectCreationActionsTest`: values forwarded, last craft saved, starter project set only when the database was empty, v1 pending object handled.
- Compose test for the sheet: helper text updates, preview updates, Create disabled on invalid input, edit mode hides new fields.
- Isolated `activepolicy` runtime test: creating the first project from the start state is free; a second requires Pro.

**Acceptance:** a user mid-project can create "Striped socks" at 37 of 120 in one sheet; Projects shows the Continue hero with "Row 37 / 120" and "83 left".

---

## 8. Phase 3: Projects start state

### 8.1 Visibility

In `ProjectListContent`:

```
if (state.isLoading) → loading (unchanged)
else if (!state.hasAnyProject && !state.isMultiSelectMode) {
    if (!state.welcomeDismissed) ProjectsStartState.Full else ProjectsStartState.Compact
}
else → existing folder empty state / hero / sections (unchanged)
```

`hasAnyProject` is unfiltered. A selected folder that is empty while other projects exist keeps today's `ProjectFolderEmptyState`.

### 8.2 Full start state layout

Inside the existing `LazyColumn` (same horizontal padding `ProjectListDimens.ScreenHorizontalPadding`), top to bottom:

1. **Close** icon button, top end, 48 dp, content description `welcome_dismiss_a11y`. Action: `dismissWelcome()`, event `welcome dismissed`.
2. **Example card** (`WelcomeExampleCard`), `knitToolsColors.cardContainer`, `shapes.large`, padding 16 dp:
   - Top start: `BadgePill(text = welcome_example_label)` "Example".
   - Row: `PatternPageSketch` (132 × 176 dp) on the start side; on the end side the unit label (existing ROWS label via `mainCounterLabelText`), the count (`MaterialTheme.typography.displayMedium`, no inline size override; the 115 sp exception belongs only to the real counter) and a growing `FabricSwatch` (width 150 dp, fixed stitch scale, height = rows × row height, 4 rows at start, max 7, animated 250 ms).
   - Centered `CounterImageButton(counter_plus_button)`, visual 96 dp, touch 104 dp, content description `welcome_plus_a11y`.
   - Caption under the button (`bodyMedium`, `onSurfaceVariant`), live region polite:
     - 0 taps: `welcome_demo_try`
     - 1 tap: `welcome_demo_after_first` with the count, plus a `TextButton` "Undo" (`welcome_demo_undo`; the existing `counter_undo_last_change` "Undo last change" is too long for this spot)
     - 2+ taps: `welcome_demo_after_second`
   - Undo decrements by one, never below 23, and moves the swatch and line back.
   - Example state lives in `rememberSaveable` (count, taps). Nothing is persisted. Event `welcome demo used` once per process on the first tap.
   - The reading band on `PatternPageSketch` moves one line per count (wraps after the last line). The sketch is code-drawn: paper surface, gray text bars, a small chart grid; no real text. Band: `primary` at 16 % alpha with a 2 dp `primary` bottom line, matching the PDF reading line look.
   - Haptics via the existing preference-aware haptic feedback.
3. **Headline** `welcome_screen_title` "Pick Up Where You Left Off" (`headlineMedium`).
4. **Body** `welcome_body` (`bodyLarge`, `onSurfaceVariant`).
5. **Trust line** `welcome_trust` (`bodySmall`, `knitToolsColors.emptyStateText`).
6. **New Project**: inline `LabeledCounterImageButton(counter_plus_button, label = new_project)`, the same component and label as the floating pill. Action identical to the floating pill (`requestProjectCreation()`).
7. **"I just need the tools"** `TextButton`: `dismissWelcome()`, `navigateToTopLevel(TopLevelDestination.Tools)`, event `welcome tools chosen`.

While the full start state is visible, the floating New Project pill is not shown (the same action is inline; two identical pills would compete). In every other state the floating pill stays.

### 8.3 Compact start state

- `FabricSwatch` 96 dp in the last craft, centered.
- Title by last craft: `start_compact_title_knitting` "What's On Your Needles?" or `start_compact_title_crochet` "What's On Your Hook?".
- Body `start_compact_body`.
- Floating New Project pill visible.

### 8.4 Components and rules

- `FabricSwatch` gains `stitchScale: Dp? = null`. When set, the stitch unit is derived from it instead of `size.minDimension`, so stitches keep their size while the swatch grows. Default behavior unchanged.
- Extract the strand drawing and shade helpers from `FabricSwatch.kt` into `FabricStrokes.kt` (internal) so `FabricSwatch`, `StitchProgressRow` and `PatternPageSketch` share one implementation.
- `OnboardingDimens`: example card padding, sketch size, swatch width, start rows, max rows, plus visual and touch size, caption min height.
- No hardcoded colors: paper uses a new token `knitToolsColors.paperSurface` (light `#F7F4E8`, dark `#ECE8D8`; a PDF page stays light in dark theme) and `knitToolsColors.paperInk` (`#B9B29A`).
- `UiConsistencySourceTest`: start state uses `LabeledCounterImageButton`, `BadgePill`, shared tokens; no `FilterChip`; no raw `colorScheme.surfaceVariant` in `ui/screens`.

### 8.5 Tests

- Compose: full state shown with no projects; compact after dismiss; hidden once a project exists; folder empty state unchanged; tools action navigates and dismisses; floating pill hidden only in the full state.
- Example card: tap and Undo bounds, caption sequence, state survives recreation, nothing written to the database (assert repository untouched with a fake).

**Acceptance:** with zero projects the user understands the promise and can create a project or leave for Tools without a blocking flow.

---

## 9. Phase 4: Stitch-marker tips

### 9.1 Model

```kotlin
enum class TipId(val key: String) {
    FIRST_ROW("first_row"),
    UNDO("undo"),
    STEP_SIZE("step_size"),
    WORK_SESSION("work_session"),
    OVERVIEW_INTRO("overview_intro"),
    PATTERN_TILE("pattern_tile"),
    READING_LINE("reading_line"),
    KEEP_SCREEN_ON("keep_screen_on"),
    TARGET_REACHED("target_reached"),
    FIRST_SESSION_SAVED("first_session_saved"),
    BACKUP("backup"),
    FOLDERS("folders"),
    WIDGET("widget"),
    TOOLS_INTRO("tools_intro"),
}

val BASIC_TIPS = setOf(TipId.FIRST_ROW, TipId.UNDO, TipId.OVERVIEW_INTRO, TipId.STEP_SIZE)
```

`TipContext` (immutable): screen, `tipsSeen`, `shownThisProcess` (count), `today`, onboarding counters, project facts (id, isStarter, count, target, stepSize, hasDocuments, hasSessions, isCompleted), app facts (projectCount, keepScreenAwake, backupCreated, widgetPinSupported), last increment timestamps, blocking surface flag (any sheet, dialog, menu, selection mode or text input focused).

`TipRules.next(context): TipId?` is pure and ordered by priority within the screen.

### 9.2 Global rules

1. At most one tip visible.
2. At most two automatic tips per app process. Exception: `UNDO` directly after `FIRST_ROW` does not count.
3. A tip is never shown while `blockingSurface` is true, never within 2 s of screen entry, and never during PDF import, purchase, error dialogs, backup restore or work-session recovery.
4. A shown tip is marked seen when dismissed, acted on or auto-hidden. It never returns unless the user resets tips.
5. Tips never cover the counter's minus, plus, Undo or step ring. Placement prefers above the anchor; if there is no room, below; never overlapping the anchor bounds.
6. Tapping anywhere outside the bubble dismisses it **and** the tap reaches the element below (pointer observed in `PointerEventPass.Initial` without consuming).
7. Skipping a tip is not recorded as learning; it only stops the tip.

### 9.3 Catalogue

| Tip | Screen and anchor | Trigger | Extra condition | Action |
| --- | --- | --- | --- | --- |
| `FIRST_ROW` | Counter, plus button | First Counter entry for the starter project | Not seen | Auto-dismiss on first increment |
| `UNDO` | Counter, Undo button | First increment after `FIRST_ROW` | | Auto-hide after 6 s |
| `STEP_SIZE` | Counter, ×N ring | 3 increments within 2 s, or 30 increments total in the project | Step is 1 | |
| `WORK_SESSION` | Counter, project actions icon | `counter_open_days >= 2` and the project has no sessions | No active session | |
| `OVERVIEW_INTRO` | Project page, Continue card | First project page entry | | |
| `PATTERN_TILE` | Counter, pattern tile | Project has no documents or web pattern and `counter_open_count >= 3` | | |
| `READING_LINE` | PDF reader, reading line control | First reader open from a project | Project PDF | |
| `KEEP_SCREEN_ON` | Counter, project actions icon | `counter_open_count >= 4` | `keepScreenAwake == false` | "Keep screen on" sets the existing preference |
| `TARGET_REACHED` | Counter, count | Count reaches target for the first time in this project | Target set | |
| `FIRST_SESSION_SAVED` | Project page, total time row | `first_session_tip_pending` | | Clears the flag |
| `BACKUP` | Projects, inline one-time card below the list | `today - first_use_epoch_day >= 7` and at least 1 project | `backup_created == false` | "Back up" opens Backup |
| `FOLDERS` | Projects, title selector | 4 or more projects | | |
| `WIDGET` | Counter, project actions icon | First saved session or `counter_open_days >= 3` | Launcher supports pinning | "Add widget" (Phase 7) |
| `TOOLS_INTRO` | Tools, first list item | First Tools entry | | |

Tips anchored to the project actions icon point to where the action lives; the actions sheet then shows the relevant row first in its section (no reordering, the tip text names the row).

Approved implementation clarification (2026-10-30): add a visible reading-line button to the project PDF reader's toolbar in reading mode. It opens the existing reading-line settings and provides the `READING_LINE` anchor, since the existing overflow-only control cannot host a tip while its menu is open. The Library reader keeps its current controls.

### 9.4 Components

- `TipAnchor.kt`: `CompositionLocal` registry, `Modifier.tipAnchor(id: TipId)` reports bounds in root coordinates via `onGloballyPositioned`.
- `TipHost.kt`: a `Box` that wraps a screen's content (used in `CounterScreen`, `ProjectOverviewScreen`, `ProjectListScreen`, Tools hub, pattern viewer). It draws the active `StitchMarkerTip` in the same window (no Popup, so accessibility order stays sane), positioned from the anchor bounds.
- `StitchMarkerTip.kt`:
  - Bubble: `knitToolsColors.cardContainer`, `shapes.medium`, 16 dp padding, max width 280 dp, `bodyMedium` text, end-aligned `TextButton` "Got it" and an optional action `TextButton`.
  - Notch (8 dp) toward the anchor.
  - Stitch marker: code-drawn locking stitch marker (ring with a small clasp gap), 14 dp, `primary`, drawn on the anchor edge nearest the bubble.
  - No left accent bar, no Material `RichTooltip` styling.
  - Enter: fade and 6 dp slide, 200 ms; none when animations are off.
  - Semantics: `liveRegion = Polite`, `paneTitle`, focusable bubble; "Got it" and the action are separate buttons; focus is not moved automatically.
- Dimensions in `OnboardingDimens`.

### 9.5 Show tips again

- Settings Info: `settings_show_tips_again` row → `resetTips()` → toast `settings_tips_reset_done`.
- Guide root footer: same action.

### 9.6 Tests

- `TipRulesTest`: every trigger, priority, per-process cap, UNDO chaining exception, blocking surfaces, seen tips never return, reset.
- Compose: bubble placement never intersects anchor bounds (counter controls), outside tap dismisses and passes through, semantics present.
- `OnboardingStateRepositoryTest`: counter day counting across midnight and zone change (use injected clock).

---

## 10. Phase 5: Project setup list

### 10.1 Visibility

Shown on the project page (`ProjectOverviewScreen`) directly under the Continue card when:
- the project id equals `starter_project_id`, and
- `project_setup_hidden == false`, and
- the project is active.

### 10.2 Items (all free under current entitlements)

| Item | Done when | Action when not done | Glyph |
| --- | --- | --- | --- |
| Starting point | `count > 0` or `targetRows != null` | Opens the existing target dialog host | `COUNTER` (new) |
| Pattern | `projectDocuments.isNotEmpty()` or linked web pattern | Opens pattern picker in `INITIAL_PROJECT_PATTERN` mode | `PATTERN` |
| Yarn | Project has a yarn note or a linked yarn card | Opens the Yarn sheet | `YARN` |
| Work time | Project has at least one completed session | Opens the Counter and shows `WORK_SESSION` tip immediately (bypasses its day condition) | `CLOCK` (new) |

Items are computed from existing state, no new table. Order: not-done items first in the table order, then done items.

### 10.3 Layout

- Header: `OverviewSectionHeader(title = project_setup_title)` with `StitchProgressRow(total = 4, done = n, craft, color = yarnColorForId(projectId, palette))` and "%1$d of %2$d" (`project_setup_progress`) at the end. No card.
- Rows: `OverviewLinkRow(glyph)`. Done rows use `onSurfaceVariant` text, a check at the end, no click.
- Overflow on the header: `RowOverflowMenu` with "Hide" → `hideProjectSetup()`.
- When the 4th item completes: the last stitch knits, the section shows `project_setup_done` once, `markProjectSetupCelebrated()`, and the section disappears on the next visit.

### 10.4 `StitchProgressRow`

- Parameters: `total`, `done`, `current: Int? = null`, `craft: CraftType`, `color: Color`.
- Knit stitch: two legs from `FabricStrokes`, leg thickness 0.30 of stitch width, legs from ±0.47 W at the top to ±0.08 W at the bottom (reads as V, not a heart).
- Crochet stitch: long and short slanted strands as in `FabricSwatch`.
- Done: filled and shaded. Current: 1.5 dp outline in `primary`. Upcoming: 1.5 dp outline in `onSurface` at 38 % alpha.
- Stitch width 22 dp, gap 6 dp, height 30 dp (`OnboardingDimens`).
- Semantics: one node, `ProgressBarRangeInfo(done, 0..total)`, description "%1$d of %2$d done".
- New glyphs `COUNTER` and `CLOCK` in `CrossStitchGlyph` using the existing 9-column cross-stitch grid style, plus `accentColor()` mappings.

### 10.5 Tests

- Item completion logic unit test.
- Compose: visible only for the starter project, hide persists, celebration shows once.
- `StitchProgressRow` semantics test.

---

## 11. Phase 6: In-app guide

### 11.1 Navigation

- Routes: `Screen.Guide` (`guide`) and `Screen.GuideTopic` (`guide/{topic}`, topic is a `GuideTopic.key`).
- Registered in the Settings graph. Also reachable from the project page: follow the `session_history` dual-registration pattern in `NavGraph.kt` so the bottom navigation highlight stays on Projects.
- Bottom navigation stays visible (not in `HIDE_NAVIGATION_ROUTES`).
- Screens use `ToolScreenScaffold` and `colorScheme.background`.

### 11.2 Entry points

- Settings Info, first row: `guide_entry_settings` "How to Use KnitTools" (chevron). The existing external "Help and Guide" row stays below it.
- Project page footer: `guide_entry_overview` "How project tools work" (`OverviewTextAction`) → guide root.
- Guide root footer: "Show tips again".

### 11.3 Content model

`GuideContent.kt`:

```kotlin
enum class GuideTopic(val key: String, @StringRes val title: Int, @StringRes val subtitle: Int, val glyph: CrossStitchGlyph, val items: List<GuideItem>)

data class GuideItem(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val proFeature: ProFeature? = null,   // marker shown from ProActionAccess at feature level
    val action: GuideAction? = null,
)

enum class GuideAction { OpenCounterForLatestProject, OpenLatestProjectPage, OpenTools, OpenLibrary, OpenMyYarn, OpenSavedPatterns, OpenInsights, OpenBackup, OpenRavelry }
```

Actions that need a project fall back to Projects when there is none. All content is bundled, offline, no web view.

### 11.4 Screens

- Root: title `guide_screen_title`, intro `guide_intro`, five `HubListItem`-style rows (hairline rows with `CrossStitchIcon`, title, subtitle, chevron).
- Topic: title from topic (Title Case), one-line purpose, items as hairline sections: `SectionLabel`-free item title (`titleMedium`), body (`bodyMedium`), optional `ProMarker` at the end of the title row, optional text action `guide_try_it` "Try it". No cards (editorial like Insights).

The full English copy for all topics is in section 17.4. It reflects current entitlements; Phase 11 updates Pro notes if entitlements change.

### 11.5 Tests

- Every `GuideItem` string exists, every action resolves, topic route round-trips, unknown topic key falls back to root.
- Source test: guide strings that mention Pro correspond to items with `proFeature != null`.

---

## 12. Phase 7: Pro presentation

### 12.1 Resolver

`pro/ProActionAccess.kt`:

```kotlin
enum class ProActionAccess { Allowed, Trial, Locked, Unknown }

/**
 * Yksi totuus merkille ja kehotteelle.
 * needsEntitlement = false: toiminto on tässä tilanteessa ilmainen (esim. ensimmäinen aktiivinen projekti).
 * alreadyUnlocked = true: monotoninen lippu tai olemassa oleva sisältö (esim. notesCreated).
 */
fun proActionAccess(
    proState: ProState,
    ready: Boolean,
    feature: ProFeature,
    needsEntitlement: Boolean = true,
    alreadyUnlocked: Boolean = false,
): ProActionAccess
```

Rules: `!ready` → Unknown. `!needsEntitlement || alreadyUnlocked` → Allowed. `proState.hasFeature(feature)` and status `TRIAL_ACTIVE` → Trial. `hasFeature` → Allowed (covers purchase and the debug override). Otherwise Locked.

Readiness: `ProManager` already tracks initial Pro and Billing readiness (`hasFeatureAfterInitialLoad`); expose a `StateFlow<Boolean>` for UI.

Contexts:

| Action | Feature | needsEntitlement | alreadyUnlocked |
| --- | --- | --- | --- |
| New Project pill (list and start state) | `UNLIMITED_PROJECTS` | `activeProjectCount >= 1` (all folders) | |
| Reactivate project (overflow) | `UNLIMITED_PROJECTS` | `activeProjectCount >= 1` | |
| Save PDF → Start new project | `UNLIMITED_PROJECTS` | `activeProjectCount >= 1` | (replaces `canStartProject`) |
| Add note (editor, overview, notes sheet) | `NOTES` | true | `notesCreated` |
| Add progress photo (gallery pill, overview) | `PROGRESS_PHOTOS` | true | |
| Add yarn (My Yarn pill and empty state) | `UNLIMITED_YARN` | true | |
| Save to My Yarn | `UNLIMITED_YARN` | true | |
| Add counter (actions sheet, workspace) | `MULTIPLE_COUNTERS` | true | |
| Shaping or repeat section type | `SHAPING_COUNTER` / `REPEAT_SECTION` | true | |
| Add reminder (sheet Add action, overview) | `ROW_REMINDERS` | true | |
| Photos to PDF (camera, gallery) | `PATTERN_CAMERA_SCAN` | true | |
| Compact repeat counter row | `SECONDARY_COUNTER` | true | `secondaryCounterUsed` |
| Paste instruction (Increase/Decrease, Gauge) | new `INSTRUCTION_PASTE` | true | |
| Add widget to home screen | `WIDGET` | true | |

### 12.2 `ProMarker`

- `ProMarker(access: ProActionAccess, modifier)` renders nothing for `Allowed` and `Unknown`.
- **Locked:** woven label. Height 22 dp, horizontal padding 8 dp, corner 4 dp, fill `proMarkerContainer`, inner dashed stitch line (1 dp, inset 2 dp, dash 3/2 dp) in `proMarkerStitch`, text `pro_badge` "PRO" in `labelSmall`, `proMarkerContent`.
- **Trial:** same shape, transparent fill, dashed outline only, text `pro_marker_trial` "TRIAL".
- Tokens in `KnitToolsColors` for both themes:

| Token | Light | Dark |
| --- | --- | --- |
| `proMarkerContainer` | `#E8DFB5` | `#3A3520` |
| `proMarkerContent` | `#6B5410` | `#E3C766` |
| `proMarkerStitch` | `#9A7B18` | `#C9A435` |

- Contrast test (pattern of `YarnPaletteContrastTest`): `proMarkerContent` on `proMarkerContainer` and on `cardContainer` ≥ 4.5:1 in both themes (expected about 5.4:1 light and 7.4:1 dark on the container).
- Semantics: Locked uses `pro_badge_locked_description`, Trial uses `pro_badge_trial_description`. When the marker sits inside a clickable row, the description is merged into the row's content description (pattern already used by `LabeledCounterImageButton`).
- Delete `ProBadge.kt` and `proBadgeDescription` after migrating all call sites (dead code rule). `UiConsistencySourceTest`: `ProMarker` must be called with a `ProActionAccess` value produced by `proActionAccess(...)`; `ProBadge` must not exist.

### 12.3 Placement changes

| Place | File | Change |
| --- | --- | --- |
| Add counter row | `ProjectActionsBottomSheet.kt:153` | `ProMarker(access)` |
| Extra counters section action | `CounterWorkspaceSections.kt:238`, `:942` | `ProMarker(access)` |
| Reminders sheet | `ReminderComponents.kt:92` | Move marker from the title row to the Add action |
| Save to My Yarn | `YarnManagementSheet.kt:334` | `ProMarker(access)` |
| Save PDF, Start new project | `IncomingPdfSheet.kt:84` | Use resolver (keeps conditional behavior) |
| My Yarn add pill and empty state | `MyYarnScreen.kt:229`, `:396`, `:413` | `ProMarker(access)` |
| Notes editor Add note | `NotesEditorScreen.kt:213` | `ProMarker(access)` |
| Photos to PDF options | `PatternPickerSheet.kt:530`, `:536` | `ProMarker(access)` |
| Gallery Take photo pill | `PhotoGalleryScreen.kt:256` | `ProMarker(access)` |
| New Project pill and start state pill | `ProjectListScreen.kt` | **New** marker when Locked or Trial |
| Reactivate project | project page overflow | **New** marker |
| Overview Add actions (notes, photos, reminders) | `ProjectOverviewSections.kt` | **New** marker, same resolver |
| Compact repeat counter | `ProjectActionsBottomSheet.kt` Counter tools section | **New** row `pro_repeat_counter_row` "Repeat counter", visible only when not allowed, opens the existing pending-action prompt path |
| Paste instruction | `PasteInstructionButton.kt` | Show the button with marker when Locked; tap opens `ProPromptSheet` with new source `InstructionPaste`; route the gate through `hasFeature(ProFeature.INSTRUCTION_PASTE)` instead of `isPro` |
| Widget | project actions sheet | **New** row `widget_add_row` "Add widget to home screen" with marker; when allowed and `AppWidgetManager.isRequestPinAppWidgetSupported`, call `requestPinAppWidget(ComponentName(context, CounterWidgetReceiver::class.java), null, null)`; unsupported launchers get `widget_add_manual` instructions |

`ProPromptSource` gains `InstructionPaste` and `WidgetPin`. Existing prompt strings are reused in this phase (English rewrites wait for Phase 9).

### 12.4 Tests

- `ProActionAccessTest`: table of every context × status (not started, active, expired, purchased) × readiness × flags.
- Source test: every `ProMarker` call site uses the resolver; no ad-hoc `isPro` checks for UI markers remain.
- Isolated `activepolicy` profile: markers appear for a free user and change to Trial after starting the trial.
- Widget pin: unit test of the supported and unsupported branches with a fake `AppWidgetManager` wrapper.

---

## 13. Phase 8: Celebrations

- `TARGET_REACHED` and `FIRST_SESSION_SAVED` tips are delivered by Phase 4; this phase adds the triggers:
  - `CounterViewModel` emits `TipSignal.TargetReached(projectId)` when a successful count change crosses from below the target to the target or above, once per project (remember the project id in `onboarding_state` set `target_celebrated_projects`, capped at 200 ids).
  - On the first successful Stop and Save ever, `setFirstSessionTipPending(true)`.
- **`ProjectCompletedSheet`** after a successful completion (overview overflow and selection bar single completion only; bulk completion does not show it):
  - Visual: newest progress photo, otherwise a 160 dp `FabricSwatch` in the project yarn color.
  - Title: `completed_title_knitting` "Bound Off!" or `completed_title_crochet` "Fastened Off!" (`headlineSmall` via `SheetTitle`).
  - Facts as label-value pairs stacked vertically: time worked (`DurationText`), final count (`MainCounterDisplayText`), started and finished dates (`LocaleDateFormat`).
  - Actions: neutral `CancelButton` "Close" and filled "Start Your Next Project" (`completed_next_project`) which calls `requestProjectCreation()`; completion frees the active slot, so this is free when no other project is active.
  - Shown once per completion event; reopening and completing again shows it again.

Tests: target crossing logic (step sizes, decrement back and forth, Undo does not re-trigger), sheet facts, bulk completion excluded.

---

## 14. Phase 9: Pro copy, Pro page and trial journey

This phase edits existing translated strings, so it ships **with** translations (Phase 10) or as new keys with old keys removed once all locales are done.

### 14.1 Prompt copy (benefit first, clear trial terms)

| Source | Title | Body before trial |
| --- | --- | --- |
| Projects | Keep Every Project in Progress | The free version keeps one project in progress at a time. Try Pro free for 14 days to add all your projects. |
| Reminders | Remember the Next Decrease | Add a reminder for the row where something changes. |
| Counters | Count Repeats Next to Your Rows | Add counters for pattern repeats, shaping or a section of rows. |
| Notes | Leave a Note for Next Time | Write down needle sizes, changes and what comes next. |
| Photos | Save Your Progress in Photos | Take photos of your project as it grows. |
| Yarn | Build Your Yarn Library | Save yarn once and link it to any project. |
| Photos to PDF | Turn a Paper Pattern into a PDF | Photograph the pages and keep them with your project. |
| Widget | Count from Your Home Screen | Add the counter widget and count without opening the app. |
| Instruction paste | Fill the Calculator from Your Pattern | Paste an instruction and the numbers fill in for you. |

Shared footer lines:
- Before trial: "Try it free for 14 days. No payment method needed, and the trial never turns into a charge."
- After trial: "Pro is a one-time purchase. Everything you've created stays available."
- Content line per source (existing meaning kept), for example notes: "Notes you've already written stay editable."

### 14.2 Pro page

Editorial, no cards:
1. Title and one line: "More Projects, Deeper Tools".
2. Personal line when data exists: "You have 3 projects in progress." or after a trial "During your trial you added 2 projects and 3 counters. They stay yours."
3. `SectionLabel` "Free Forever": one project in progress, counter with step size, PDF reader with reading line and markup, web patterns and Ravelry downloads, project yarn and amounts, work time, calculators and reference, backup.
4. `SectionLabel` "Pro Adds": updated three groups.
5. Purchase section unchanged (Play price, one-time purchase, restore).

### 14.3 Trial journey

| Moment | Behavior |
| --- | --- |
| Trial starts | Sheet "Your Pro Trial Has Started" with up to 5 things to try, each with a direct action (Add widget, Add a repeat counter, Add another project, See this week's chart, Turn a paper pattern into a PDF). Progress as `StitchProgressRow`. Same list on the Pro page. |
| 3 days left | One in-app notice (no notification) at next launch: "3 days of your trial left. So far you've added 2 projects and the widget. Everything stays yours." Actions: See Pro, Got it. |
| Trial ends | Existing notice gains the same personal summary. "Continue free" keeps equal weight with "See Pro". |

Summary counts come from existing timestamps after `trial_start_timestamp` (projects `createdAt`; other entities only if they have creation timestamps; verify per table and omit those without).

---

## 15. Phase 10: Translations (release gate)

- Translate `strings_onboarding.xml` and all Phase 9 strings into da, de, es, fi, fr, it, nb, nl, pt, pt-BR, sv (`values-*/strings_onboarding.xml`).
- Finnish terminology: "ohje" never "kaava", "verkko-ohje", "Lisää verkko-ohje", project name keeps its casing.
- Craft idioms: "on your needles" and "on your hook" use natural local equivalents or a neutral "työn alla" style phrase. Translator notes go into XML comments above each key.
- Title Case applies only to English screen titles.
- Remove `tools:ignore="MissingTranslation"` from `values/strings_onboarding.xml`. `./gradlew lint` must pass.
- Extend `OnboardingResourcesSourceTest`: every key exists in every supported locale with matching placeholders (same mechanism as `ProLocalizationSourceTest`).
- Add the gate to `ANDROID_RELEASE_READINESS_PLAN.md`: "Onboarding strings translated, MissingTranslation ignore removed".

---

## 16. Phase 11: Entitlement rebalancing (decision-gated)

Implement only after an explicit product decision. Recommendations from the earlier plan:

| # | Change | Recommendation | Implementation notes |
| --- | --- | --- | --- |
| D1 | Notes free | Yes | Remove `NOTES` gates in `CounterViewModel`, `NotesEditorViewModel`, `CounterRepository.saveProjectNotes`; keep `notesCreated` column for compatibility |
| D2 | Progress photos free | Yes | Remove `PROGRESS_PHOTOS` gates; add the photo item to Project setup |
| D3 | 5 free yarn cards | Yes | Count-aware check in the yarn repository; `ProActionAccess` context gains `existingYarnCards` |
| D4 | One free extra counter per project | Yes | Count-aware check in `ProjectCounterRepository`; shaping and section stay Pro |
| D5 | Current streak free | Yes | `STREAK` split: current streak free, best streak and charts Pro |
| D6 | Instruction paste free | Yes | Remove `INSTRUCTION_PASTE` gate |

Each change updates guide Pro notes, Pro page lists, prompt sources, tests and PROJECT.md's Pro tables. Existing data stays available (Model A).

---

## 17. English copy catalogue

All keys live in `values/strings_onboarding.xml` unless marked "existing". English screen titles use Title Case.

### 17.1 Start state

| Key | English |
| --- | --- |
| `welcome_example_label` | Example |
| `welcome_demo_try` | Try it: tap + when you finish a row. |
| `welcome_demo_after_first` | Row %1$d is saved. Tapped by mistake? Undo takes it back. |
| `welcome_demo_after_second` | That's it. KnitTools remembers your place, so you don't have to. |
| `welcome_demo_undo` | Undo |
| `welcome_demo_a11y` | Example counter at row %1$d. The reading line on the example pattern moves with it. |
| `welcome_plus_a11y` | Add one row to the example |
| `welcome_screen_title` | Pick Up Where You Left Off |
| `welcome_body` | KnitTools keeps your count, your place in the pattern and your project details together. Put your work down any time and come back to exactly the right spot. |
| `welcome_trust` | No account needed. Your projects stay on this phone. |
| `welcome_tools_only` | I just need the tools |
| `welcome_dismiss_a11y` | Hide introduction |
| `start_compact_title_knitting` | What's On Your Needles? |
| `start_compact_title_crochet` | What's On Your Hook? |
| `start_compact_body` | Add the project you're working on, or start a new one. |

### 17.2 Project form

| Key | English |
| --- | --- |
| `project_name_placeholder_knitting` | Striped socks |
| `project_name_placeholder_crochet` | Granny square blanket |
| `project_start_count_label_rows` | Rows already done |
| `project_start_count_label_rounds` | Rounds already done |
| `project_start_count_label_repeats` | Repeats already done |
| `project_start_count_label_custom` | Count so far |
| `project_start_count_help_new` | Leave at 0 for a new project. |
| `project_start_count_next_rows` | Next up: row %1$d |
| `project_start_count_next_rounds` | Next up: round %1$d |
| `project_start_count_next_repeats` | Next up: repeat %1$d |
| `project_start_count_next_custom` | Next up: %1$d |
| `project_start_count_error` | Enter a number from 0 to %1$s. |
| `project_target_label` | Target (optional) |
| `project_target_help` | See how many are left as you go. |
| `project_target_error` | Enter a number from 1 to %1$s. |
| `project_create_preview_a11y` | Preview: %1$s |

Numbers in errors are formatted with `formatIntegerForDisplay`.

### 17.3 Tips

| Key | English |
| --- | --- |
| `tip_got_it` | Got it |
| `tip_first_row_rows` | When you finish a row, tap +. Tapping the big number works too. |
| `tip_first_row_rounds` | When you finish a round, tap +. Tapping the big number works too. |
| `tip_first_row_repeats` | When you finish a repeat, tap +. Tapping the big number works too. |
| `tip_first_row_custom` | Tap + each time you finish one. Tapping the big number works too. |
| `tip_undo` | Saved. Tapped by mistake? Undo takes back the last change. |
| `tip_step_size` | Counting more than one at a time? Change how much each tap adds here. |
| `tip_work_session` | Want to know how long this project takes? Start a work session from this menu. Time never starts on its own. |
| `tip_overview_intro` | This is your whole project: pattern, yarn, notes and photos. Continue takes you back to the counter. |
| `tip_pattern_tile` | Attach your pattern to keep it with this project. |
| `tip_reading_line` | Turn on Follow main counter and the reading line moves as you count. |
| `tip_keep_screen_on` | Screen going dark while you work? Keep it on while you count. |
| `tip_keep_screen_on_action` | Keep screen on |
| `tip_target_reached` | Target reached. Keep counting, or mark the project finished from the project page menu. |
| `tip_first_session_saved` | Your first work session is saved. The total time for this project is here. |
| `tip_backup` | Your projects are only on this phone. Make a backup so they're safe if you switch phones. |
| `tip_backup_action` | Back up |
| `tip_folders` | Lots of projects? Sort them into folders from the title. |
| `tip_widget` | Count from your home screen without opening the app. |
| `tip_widget_action` | Add widget |
| `tip_tools_intro` | The calculators work offline. Example values show what to enter. |
| `settings_show_tips_again` | Show tips again |
| `settings_tips_reset_done` | Tips will show again. |

### 17.4 Guide

| Key | English |
| --- | --- |
| `guide_screen_title` | How to Use KnitTools |
| `guide_intro` | Pick what you're doing right now. You can come back here any time. |
| `guide_entry_settings` | How to Use KnitTools |
| `guide_entry_overview` | How project tools work |
| `guide_try_it` | Try it |

In the topic tables below, "(Pro marker)" is not part of the copy. It means the item sets `proFeature`, so `ProMarker` shows the current access state next to the item title.

**Continue a Project** (`guide_topic_continue_title`, subtitle "Counter, Undo and your project page")

| Item | Body |
| --- | --- |
| Count as You Go | Tap + when you finish a row. Tapping the big number adds one too. Minus takes one away. |
| Undo Mistakes | Undo steps back through your recent changes, one tap at a time. |
| Count More Than One | The ×1 ring between the buttons sets how much each tap adds, from 1 to 99, separately for each project. |
| Project Page and Counter | Tapping a project opens its project page with the pattern, yarn, notes and photos. Continue opens the counter. |
| Find It Again | A project you've started waits at the top of Projects, ready to continue. |
| Count from Your Home Screen | Add the counter widget to count without opening the app. (Pro marker) |

**Follow Your Pattern** (`guide_topic_pattern_title`, subtitle "PDFs, web patterns and the reading line")

| Item | Body |
| --- | --- |
| Attach Your Pattern | Add a PDF from your phone or cloud storage, or save a link to a web pattern. |
| Several Documents | A project can hold several PDFs. Each one remembers its own page. |
| Read and Mark Up | Reading mode gives the page the whole screen. Mark up adds pens, highlighters, text and chart tools. |
| The Reading Line | Turn on Follow main counter and the line moves as you count. Calibrate counter positions once if your rows don't line up. |
| Bookmarks | Bookmark the spots you keep coming back to. |
| Patterns from Ravelry | Open Ravelry in the app, sign in on Ravelry's own page, and download PDFs you own straight into KnitTools. |
| Paper Patterns | Photograph the pages and KnitTools turns them into a PDF. (Pro marker) |

**Keep Yarn Organized** (`guide_topic_yarn_title`, subtitle "Project yarn, amounts and My Yarn")

| Item | Body |
| --- | --- |
| Yarn for This Project | Note the yarn you're using in the project, with planned, allocated and used amounts. |
| What Remaining Means | Remaining is what's left of the yarn allocated to this project. Your stash numbers don't change on their own. |
| My Yarn | Save yarn once with color number, dye lot and photos, then link it to any project. (Pro marker) |

**Remember the Details** (`guide_topic_details_title`, subtitle "Notes, extra counters, reminders and photos")

| Item | Body |
| --- | --- |
| Notes | Keep needle sizes, changes and what comes next. (Pro marker) |
| Extra Counters | Count repeats, shaping or a section of rows next to the main count. (Pro marker) |
| Row Reminders | Get a reminder in the app when you reach a row, like the next decrease. Reminders show inside KnitTools, not as phone notifications. (Pro marker) |
| Progress Photos | Take photos as your project grows. The newest one becomes the project's picture. (Pro marker) |

**Calculate, Track and Back Up** (`guide_topic_tools_title`, subtitle "Tools, work time, Insights and backup")

| Item | Body |
| --- | --- |
| Calculators | Gauge, increases and decreases, cast-on and yarn amounts. They work offline. |
| Reference | Needle sizes, size charts, abbreviations and chart symbols live in Library. |
| Work Time | Start a work session from the project menu to track your time. Stop when you're done to save or discard it. |
| Insights | See the time you've recorded. Pro adds daily, weekly and monthly charts and your streak. |
| Folders | Group projects into folders from the Projects title. One project in progress at a time is free, and finished projects don't count. |
| Backup | Make a backup file and keep it somewhere safe, like your cloud drive. Restoring replaces everything on this phone. |

Item keys follow `guide_<topic>_<item>_title` and `guide_<topic>_<item>_body`.

### 17.5 Project setup

| Key | English |
| --- | --- |
| `project_setup_title` | Project setup |
| `project_setup_progress` | %1$d of %2$d |
| `project_setup_starting_point` | Set your starting point |
| `project_setup_starting_point_done` | Starting point set |
| `project_setup_pattern` | Attach your pattern |
| `project_setup_pattern_done` | Pattern attached |
| `project_setup_yarn` | Add the yarn you're using |
| `project_setup_yarn_done` | Yarn added |
| `project_setup_session` | Save your first work session |
| `project_setup_session_done` | Work session saved |
| `project_setup_done` | All set. Enjoy your project. |
| `project_setup_hide` | Hide |
| `stitch_progress_a11y` | %1$d of %2$d done |

### 17.6 Pro marker and new Pro rows

| Key | English |
| --- | --- |
| `pro_badge` (existing) | PRO |
| `pro_marker_trial` | TRIAL |
| `pro_badge_locked_description` (existing) | Pro feature |
| `pro_badge_trial_description` (existing) | Pro feature, available during trial |
| `pro_repeat_counter_row` | Repeat counter |
| `widget_add_row` | Add widget to home screen |
| `widget_add_manual` | Touch and hold your home screen, choose Widgets, then find KnitTools. |
| `pro_prompt_instruction_paste_title` | Fill the Calculator from Your Pattern |
| `pro_prompt_instruction_paste_body` | Paste an instruction and the numbers fill in for you. |
| `pro_prompt_widget_pin_title` | Count from Your Home Screen |

### 17.7 Celebrations

| Key | English |
| --- | --- |
| `completed_title_knitting` | Bound Off! |
| `completed_title_crochet` | Fastened Off! |
| `completed_time_label` | Time worked |
| `completed_count_label` | Final count |
| `completed_started_label` | Started |
| `completed_finished_label` | Finished |
| `completed_next_project` | Start Your Next Project |

---

## 18. Analytics

Opt-in only, no content, no ids, no counts.

| Event | When |
| --- | --- |
| `welcome demo used` | First example tap in a process |
| `welcome tools chosen` | "I just need the tools" |
| `welcome dismissed` | Close icon |
| `guide opened` | Guide root opened |
| `tip acted` | A tip action button was used |
| `tip dismissed` | A tip was dismissed |

Existing `project created` covers creation. Screens added to the allowlist: `project_overview`, `guide`, `guide_topic`.

---

## 19. Testing strategy

| Layer | What |
| --- | --- |
| Unit | `ProjectStartValues`, `TipRules`, `ProActionAccess`, setup item logic, target crossing, `OnboardingStateRepository` (with a test DataStore and fixed clock) |
| Repository | `createProject` with start count and target, no history, quota, invalid input |
| Compose | Start state variants and transitions, example card, project form (preview, helpers, validation), tip placement and pass-through, setup section, guide navigation, completed sheet |
| Source | `UiConsistencySourceTest` additions, `OnboardingResourcesSourceTest`, extended middle-dot test, `ProMarker` resolver usage, no `ProBadge` |
| Isolated runtime | `activepolicy` profile for free, trial and expired marker states and the first-project quota |
| Manual | Phase 0 mockup approval; usability test (section 21) |

Device checks at 320 dp, 200 % font and TalkBack are run only on explicit request (project rule). New UI is still designed for them: wrapping text, 48 dp targets, semantics as specified.

Commands per phase: `./gradlew test`, `./gradlew :app:detekt`, `./gradlew lint`, plus the user-run `lc` and `sc`.

---

## 20. Documentation updates

Update in the phase that introduces the change:

- `CLAUDE.md`, `AGENTS.md`, `CODEX.md` UI Rules: start state rules (no blocking onboarding route, inline pill replaces the floating pill only in the full state), project form fields, tips rules and component names, Project setup, guide, `ProMarker` and `ProActionAccess` rule, English-first resource file.
- `PROJECT.md`: product model (start state, guide), Projects creation (starting count and target semantics), preferences and `onboarding_state`, navigation routes, Pro tables (marker states, new feature `INSTRUCTION_PASTE`, new prompt sources), localization (resource file and release gate), analytics events, test ownership map.
- `memory/MEMORY.md`: architecture pointer for onboarding.
- `ANDROID_RELEASE_READINESS_PLAN.md`: translation gate.

Workflow rule: implement phase by phase, report each phase, and commit or push only when the user asks.

---

## 21. Usability test and acceptance criteria

Six people, knitters and crocheters, mixed app experience. Hand over the phone, no instructions.

1. Tell in your own words what you'd use KnitTools for.
2. Add a project you're already working on, at the right row.
3. Attach a pattern or add the project yarn.
4. Leave the project and find your way back to continue it.
5. Find help for the counter or for reading a pattern.
6. Explain what is free, what the trial does and what happens when it ends.

Acceptance:
- A usable own project exists in about one minute without starting the trial.
- Continuing is found without help.
- The difference between the project page and the counter is understood.
- Project yarn and My Yarn are told apart.
- Nobody believes PDF reading is paid.
- Everyone understands the trial never turns into a charge.
- Skipping every tip and the start state leaves the app fully usable.

---

## 22. Risks

| Risk | Mitigation |
| --- | --- |
| Start state feels like an ad | One promise, one example, one primary action; closable; no questions |
| Example plus confused with New Project | "Example" pill, the example lives inside its own card, caption names it |
| Tips annoy | Strict caps, behavior triggers, never over counter controls, reset available |
| Starting count corrupts history | Initial state only, no history row, tests |
| English-only UI in other locales during development | Dedicated resource file, release gate in Phase 10 |
| Ravelry guide item before Ravelry permission | Item hidden with the same condition that hides Tools → Ravelry if that entry is ever disabled |
| Debug build opens all Pro gates | Marker tests run in the isolated `activepolicy` profile |
| `PendingProjectCreation` version change | Safe deserialization fallback |
| Widget pinning unsupported | Manual instructions string |

---

## 23. Out of scope

- Entitlement changes (Phase 11, decision-gated).
- Shareable finished-object card, bundled sample pattern PDF, user-chosen project color, ongoing work-session notification. Possible later work.
- Voice features (removed on purpose), cloud sync, accounts.
