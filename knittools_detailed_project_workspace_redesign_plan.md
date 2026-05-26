# KnitTools Detailed Project Workspace Redesign Plan

## 1. Purpose

This document is a more detailed product and UI plan for improving KnitTools after reviewing the current `PROJECT.md`, the latest KnitTools screenshots, and the LooseLoop reference screenshots.

The goal is not to visually redesign KnitTools. The existing KnitTools identity should remain intact. The app should continue to use its current warm craft palette, Outfit typography, rounded cards, earthy surfaces, orange accent, muted greens, calm spacing, and tactile counter button.

The goal is to improve structure, hierarchy, and task flow so that project information is easier to see, easier to attach, and easier to use while knitting.

This document intentionally avoids exact file paths and exact implementation details. Codex should inspect the current code and route structure before making changes, because `PROJECT.md` says code is the final source of truth when documents and implementation disagree.

## 2. Important Context About `PROJECT.md`

`PROJECT.md` should be treated as a baseline description of the project before the newest UI experiments and planning changes.

Use `PROJECT.md` for these things:

- What capabilities already exist in the app
- What broad navigation structure exists
- What routes and screens exist conceptually
- Which data concepts exist, such as projects, yarn cards, notes, reminders, photos, saved patterns, sessions, and counters
- Which current assumptions may be stale

Do not treat `PROJECT.md` as a finished UX plan for the new workspace. It was written before the latest thinking about larger project cards, removing duplicated quick actions and project info rows, and hiding launch-excluded voice, AI-like, or tip surfaces.

In practice:

- `PROJECT.md` is useful for understanding the current app architecture.
- This document is the more recent product direction for the row counter and project workspace.
- Codex should verify the current code before assuming any UI or feature is still present.

## 3. Current App Capabilities To Preserve

The current app already has the right building blocks for a strong knitting project workspace.

Existing or documented feature areas include:

- Multiple projects
- Row counter
- Target rows
- Stitch tracking
- Multiple project-specific counters
- Shaping and repeat counter paths
- Row reminders
- Progress photos
- Project notes
- Session history
- Pattern PDF attachment
- Pattern viewer and annotations
- Saved patterns
- My Yarn / yarn cards
- All Photos
- Reference screens
- Gauge, increase/decrease, cast-on, yarn estimator, and Ravelry search/detail tools
- Insights with time, pace, activity, heatmap/streak-style views
- One-time Pro purchase and trial model
- Widget support that can open directly into the counter

The redesign should not remove these product strengths. It should make the most important project-related capabilities feel connected instead of scattered.

## 4. Current Navigation Direction

Keep the current top-level navigation:

1. Projects
2. Library
3. Tools
4. Insights
5. Settings

This structure is sound.

Projects should remain the primary tab and app start area. KnitTools should feel like a project-centered knitting companion, not a generic utility drawer.

Library should remain the place for saved user resources and reference material. It makes sense as long as it contains both user-owned content and reference content.

Tools should remain for calculators and utility functions. It should not become the home for project-specific yarn, notes, photos, or pattern links.

Insights can stay as a value and reflection screen, but it should not compete with the core knitting workflow.

Settings should remain administrative.

## 5. Main UX Problem

The app has the right features, but the project experience still feels split across too many surfaces.

A project can include:

- A row counter
- Pattern PDF
- Notes
- Photos
- Yarn
- Reminders
- Extra counters
- Stitch settings
- Session history
- Completion state

However, the user currently has to find these things through different screens, icons, bottom sheets, and menus.

The main problem is not missing functionality. The main problem is visibility and mental model.

The user should not feel that a project is a list card plus a separate counter plus hidden tools. The user should feel that a project is one workspace.

## 6. Product Direction

The row counter should become the main active project workspace.

This does not mean the row counter should become crowded. It means the row counter should clearly show the most relevant project context after the main counting area.

The first visible area must still prioritize counting. A user who opens the counter should be able to increment the row without scrolling.

Below the counter, the user should see large, readable, tappable project content cards. These cards should show what is attached to the project and should act as the way to open or edit that content.

The major update from earlier plans is this:

Do not build a separate Quick Actions section and then repeat the same information in a Project Info table.

Instead, combine action and information into large project content cards.

Each project card should answer two questions:

1. What is attached here?
2. What happens if I tap this?

This is the useful lesson from LooseLoop. LooseLoop often uses large task areas and clear primary actions. KnitTools should apply that clarity without copying LooseLoop's visual style.

## 7. Design Principles

### 7.1 Keep KnitTools visually intact

Do not change:

- Color palette
- Outfit typography direction
- Rounded card language
- Warm light and dark surfaces
- Orange accent
- Muted green and beige tones
- Bottom navigation visual style
- Large tactile counter button
- Overall KnitTools personality

The design issue is hierarchy and discoverability, not visual identity.

### 7.2 Make tappable areas obvious

Important actions should not look like small utility text or tiny icons.

The user should immediately understand that Pattern, Yarn, Notes, Photos, Reminder, and Extra Counters are tappable. Use large cards, clear labels, strong secondary text, and visible affordances.

### 7.3 Do not design for perfect data

Real users will often attach incomplete information.

A project may have:

- Only a pattern
- Only a row counter
- Only one yarn name
- One quick note
- One photo
- One reminder
- No neatly filled yarn metadata

The UI must look intentional even when data is partial.

### 7.4 Separate daily knitting from administration

Daily project content:

- Pattern
- Yarn
- Notes
- Photos
- Next reminder
- Extra counters

Administrative or lower-frequency actions:

- History
- Rename
- Reset counter
- Complete project
- Delete project
- Counter settings
- Stitch tracking setup

These should not have the same visual priority.

### 7.5 Avoid false promises

Do not imply that the app can automatically recognize yarn, extract label data, detect colors, or parse patterns unless the feature actually exists and works reliably.

Camera and photo actions should be honest. A photo can be saved as a photo. It should not be presented as scanning or automatic recognition unless that is actually implemented.

### 7.6 Remove launch-excluded assistant-like surfaces

If the current launch direction excludes AI-like helpers and voice-style assistant UI, remove or hide:

- Microphone UI
- AI summary rows
- AI scanner wording
- Automatic yarn recognition wording
- Tip cards that feel like assistant content
- Any visible copy that suggests hidden intelligence when the app is only storing user data

This does not necessarily mean deleting all internal code immediately. It means the launch UI should not surface it unintentionally.

## 8. Row Counter Workspace, New Structure

The row counter screen should become scrollable, but the first viewport should remain focused on counting.

Recommended structure:

```text
Header
Project title
Pattern line
Counter area
Project content cards
Active extra counters
More / Manage
```

The key detail is that Project content cards replace the earlier idea of separate Quick Actions and Project Info rows.

## 9. Row Counter Header

The header should remain simple.

Recommended contents:

```text
Back
Project name
More
```

Avoid a crowded header full of small icons.

Do not duplicate actions in the header if they are already available as large project cards. For example, if Photos has a large card below the counter, a small camera icon in the header is probably not needed.

If the microphone icon still exists in the UI and is not intentionally part of launch, remove or hide it.

## 10. Pattern Line

Keep the attached pattern visible directly under the project title.

If a pattern is attached:

```text
two_sleeves_one_promise.pdf
```

Tapping opens the project pattern viewer.

If no pattern is attached:

```text
Attach pattern
```

Tapping opens the attach pattern flow.

This line should remain clear but not overly dominant. The full Pattern card below the counter can provide the larger action area.

## 11. Counter Area

The counter remains the core of the screen.

It should include:

- Repeat or section control, if active
- Row label, for example `Row 67 / 88`
- Large current row number
- Progress bar
- Section progress, if active
- Minus button
- Large plus button
- Undo button

The plus button should remain large and easy to tap.

The user should not need to scroll to increment the row.

The counter area can be made slightly more compact if needed, but do not solve layout pressure by making important text small. Reduce duplication and reorganize instead.

## 12. Project Content Cards

After the counter, show a clear section for project content.

Recommended section title:

```text
Project
```

Recommended cards:

1. Pattern
2. Yarn
3. Notes
4. Photos
5. Next Reminder, only if active or coming soon
6. Extra counters, if active

Each card should be large, readable, and clearly tappable.

Do not make these cards tiny chips.

Do not make them table rows with a small label on the left and a value on the right.

Do not repeat the same content again in a separate Project Info section.

The cards are both the information and the action.

## 13. Pattern Card

### Attached state

```text
Open Pattern
two_sleeves_one_promise.pdf
```

Tap opens the project pattern viewer.

If the filename is long, truncate safely, but still show enough to identify it. Consider showing a saved pattern title if available instead of the raw PDF filename.

### Empty state

```text
Attach Pattern
Add a PDF or saved pattern to this project
```

Tap opens the attach pattern flow.

### Attach Pattern Flow

Use a simple task-focused flow, inspired by the clarity of LooseLoop but styled like KnitTools.

Recommended options:

```text
Choose from Saved Patterns
Import PDF
Continue without pattern
```

Optional if supported by current code:

```text
Open Ravelry Search
```

Do not imply guided pattern mode, size-specific progress, or automatic parsing unless the app really supports it.

## 14. Yarn Card

### Linked state

```text
Yarn
Isager Highland Wool
1 skein
```

or:

```text
Yarn
Blue sock yarn
2 skeins
```

Tap opens the linked yarn flow.

### Empty state

```text
Add Yarn
Choose from My Yarn or add yarn just for this project
```

Tap opens yarn linking options.

### Yarn Linking Flow

Offer two large, clear choices:

```text
Choose from My Yarn
Pick an existing saved yarn
```

```text
Add yarn to this project
Save a quick yarn note for this project only
```

This is important because the current code may not yet have a complete manual yarn card creation UI. The current app state documents My Yarn and yarn card detail behavior, but warns not to assume a full manual yarn card creation form exists without checking implementation.

### Project-only yarn notes

A project-only yarn note should be allowed.

Minimum useful data:

- Yarn name or short description
- Quantity, optional
- Notes, optional
- Label photo, optional

The user should not need to create a full My Yarn record before attaching yarn to a project.

### Save to My Yarn

If a project-only yarn note becomes useful, offer an optional later action:

```text
Save to My Yarn
```

This should not be required in the initial project flow.

## 15. My Yarn Direction

My Yarn should stay in Library under My Collection.

Do not add Yarn as a separate bottom tab.

My Yarn should be practical project support, not a perfect stash database.

Recommended Library card text:

```text
My Yarn
Yarn saved for your projects
```

or:

```text
My Yarn
Yarn notes, quantities, and linked projects
```

Avoid text like:

```text
brands, weights, and colors
```

unless those fields are truly easy and reliable to maintain manually.

### My Yarn list cards

Use text-based cards that match KnitTools.

Do not use large realistic yarn ball images as the default list visual.

Examples:

```text
Blue sock yarn
1 skein
Not linked
```

```text
Regia 4-ply
1 skein · In use
Emergency Sock
```

```text
Regia 4-ply
Fingering · 1 skein · In use
Emergency Sock
```

Missing brand, color, fiber, or gauge should not make the card look broken.

### Yarn photos and label photos

Without AI scanning, photo capture should be presented honestly:

```text
Save label photo
Add yarn photo
```

A camera icon alone can imply scanning. Prefer a plus action that opens explicit options.

Do not present label photos as automatic recognition.

### Color handling

Color should be manual and optional.

Possible fields:

- Color name
- Color number
- Manual color tag
- Dye lot

If a manual color tag exists, use a small swatch or dot. If no color exists, the card should still look complete.

## 16. Notes Card

### Existing notes

```text
Notes
First sleeve is moving again. Decrease every 8 rows.
```

or:

```text
Notes
Heel turn · 2 notes
```

Tap opens the notes editor or project notes screen.

### Empty state

```text
Add Note
Save a quick project note
```

Tap should allow fast note entry.

Notes are important project content. They should not be buried as a small icon or only shown in a bottom sheet.

If there is a full-screen notes editor, keep it, but make it easy to reach from the row counter.

## 17. Photos Card

### Existing photos

```text
Photos
1 progress photo
```

Tap opens the project photo gallery.

### Empty state

```text
Add Photo
Save a progress photo for this project
```

Tap starts photo add flow.

All Photos remains a global Library view. The row counter Photos card should show and open photos for the current project.

Avoid large thumbnails inside the row counter for launch unless they remain clean. A clear text summary is enough.

## 18. Reminder Card

A reminder should be visible when it is relevant.

### Active or upcoming reminder

```text
Next Reminder
Row 74: Try on before cuff
```

Tap opens reminders.

This is much more useful than a small `1 reminder` count.

### No reminder

Do not show a large empty Reminder card by default unless reminders become a central launch feature.

Reminder creation can live in More or be offered in a smaller secondary way.

## 19. Extra Counters

Extra counters are active knitting tools, not passive metadata.

If extra counters exist, show them near the counter workflow.

Examples:

```text
Extra Counters

Decrease repeats
3 / 8
```

```text
Sleeve shaping
54
Next: Row 56 -> 58 sts
```

Do not hide active extra counters only inside a generic More sheet.

If no extra counters exist, do not show a large empty section.

## 20. Stitch Tracking and Target Rows

Stitch tracking and target rows already exist in the current project capability set.

The main row counter should remain dominant.

If stitch tracking is enabled and relevant, show active values where useful. Setup and toggles can live in More.

Do not make a new user understand stitch tracking before they can use the basic row counter.

## 21. More / Manage Sheet

The overflow menu should open a clear Manage or More sheet.

This sheet should not repeat all daily project content. Pattern, Yarn, Notes, Photos, and active next reminders should already be visible as project cards.

The More sheet should contain lower-frequency actions and settings.

Recommended structure:

```text
This Project
Reminders
Counters

Counter Tools
Stitches per row
Track stitches

Project Actions
History
Rename
Reset counter
Complete project
Delete project
```

Delete should remain visually distinct and clearly destructive.

Avoid putting Reset, Rename, Complete, and Delete next to Yarn, Notes, or Photos with the same visual weight.

## 22. Row Counter Layout Example

A practical final structure:

```text
Back                                      More

SLEEVE 1 OF 2, ALLEGEDLY
two_sleeves_one_promise.pdf

Repeat
-    6    +

Row 67 / 88
67
progress bar

37 / 72

-         +         undo

Project

Open Pattern
two_sleeves_one_promise.pdf

Yarn
Isager Highland Wool · 1 skein

Notes
First sleeve is moving again. Decrease every 8 rows.

Next Reminder
Row 74: Try on before cuff

Photos
1 progress photo

Extra Counters
Decrease repeats 3 / 8
Sleeve shaping 54 · Next: Row 56 -> 58 sts
```

This is intentionally larger and clearer than the earlier mockup with small project info rows.

## 23. Empty Project Layout Example

For a project with very little attached:

```text
Back                                      More

NEW SOCK PROJECT
Attach pattern

Row 0
0

-         +         undo

Add to this project

Attach Pattern
Add a PDF or saved pattern

Add Yarn
Choose from My Yarn or add project yarn

Add Note
Save modifications or reminders

Add Photo
Save a progress photo
```

This teaches the user what can be attached without making the project look broken.

## 24. Projects Screen

Projects remains the main entry point.

### Continue Knitting

Keep the Continue Knitting card. It is a strong concept.

It can become slightly more informative:

```text
CONTINUE KNITTING
Sleeve 1 of 2, allegedly
Row 67 of 88 · Repeat 6 · 3h 16m
```

Do not add too much. This card should still feel like a quick resume action.

### Project cards

Project cards should prioritize:

1. Project name
2. Current section or status
3. Row count and last updated date
4. Linked yarn and quiet attachment indicators

The raw PDF filename should not dominate the project card.

Suggested structure:

```text
Emergency Sock
Heel turn
63 rows · May 16
Regia 4-ply · 1 photo · notes
```

Another example:

```text
Scarf That Became Something Else
Texture repeat
126 rows · May 16
Cascade 220 Superwash · 1 photo · notes
```

If a pattern name is meaningful, it can appear in a secondary position. If it is a long technical filename, keep it inside the project workspace instead.

### Project card interactions

Recommended simple approach:

- Tapping Continue Knitting opens the row counter.
- Tapping a project card opens the row counter workspace.
- Tapping specific small attachment indicators can be added later, but is not required for launch.

Optional later approach:

- Play button opens row counter directly.
- Card body opens a Project Overview screen.
- Attachment indicators open their content directly.

Do not add this extra complexity unless it improves the actual workflow.

## 25. Optional Project Overview Screen

A separate Project Overview screen is optional.

It may be useful later if the row counter becomes too crowded.

If added, it should show:

- Project name
- Current row and target rows
- Current section/status
- Pattern
- Yarn
- Notes
- Photos
- Reminders
- Extra counters
- Continue button

But it must not slow down the most common action, which is continuing to count rows.

For launch, it is probably better to make the row counter itself the workspace before adding a separate overview screen.

## 26. Library Screen

Keep Library as a top-level tab.

The current structure is right:

```text
My Collection
Saved Patterns
My Yarn
Photos

Reference
Needle Sizes
Size Charts
Abbreviations
Chart Symbols
```

Library should feel like the place for saved resources and references.

### Remove or hide Quick Tip

If the product direction is to avoid assistant-like or AI-like surfaces for launch, remove or hide the Quick Tip card from Library.

Even if the tip is static, it can feel like leftover helper content and distracts from the Library's main job.

If tips remain in the code behind a setting, they should not take priority in the main Library list.

## 27. Saved Patterns

Saved Patterns should stay in Library.

A saved pattern should open:

- Local/imported pattern viewer, if it is a local/imported pattern
- Ravelry detail, if it is a Ravelry-linked saved pattern

The project attach pattern flow should be able to choose from Saved Patterns.

Do not imply guided pattern mode, size-specific progress, or automatic pattern conversion unless those features are actually implemented.

## 28. Pattern Attachment Flow

The pattern attachment flow should be direct and large.

Suggested screen or sheet:

```text
Attach Pattern
Add a PDF or saved pattern to this project.

Choose from Saved Patterns
Import PDF
Search Ravelry
Continue without pattern
```

Only include Search Ravelry if that flow is stable and appropriate for project attachment.

Avoid complex explanations. The user is trying to link a pattern, not learn a whole system.

## 29. Photos

There are two photo contexts:

1. Project photos
2. All Photos in Library

The row counter Photos card should open project-specific photos.

Library Photos should show the global photo collection.

This distinction should be clear in copy:

```text
Photos
1 progress photo
```

versus:

```text
Photos
Progress photos from all projects
```

## 30. Reminders

Row reminders already exist and are valuable.

They should be surfaced in two ways:

- The next relevant reminder appears as a large card in the row counter.
- Reminder management lives in More or a reminders sheet.

The row counter should not only show a count. It should show the actual reminder text when useful.

Good:

```text
Next Reminder
Row 74: Try on before cuff
```

Less useful:

```text
Reminders
1
```

## 31. Notes

Notes are first-class project content.

They can include:

- Modifications
- Mistakes
- Needle changes
- Size changes
- Row notes
- Personal reminders
- Pattern adjustments

The row counter should make notes obvious.

Good card:

```text
Notes
First sleeve is moving again. Decrease every 8 rows.
```

Empty card:

```text
Add Note
Save modifications, changes, or reminders
```

If a full-screen notes editor exists, keep it. The main improvement is easier entry and better visibility from the row counter.

## 32. Tools

Tools should remain for utility functions:

- Gauge
- Increase / Decrease
- Cast On
- Yarn Estimator
- Ravelry search/detail

Do not move project-specific yarn management into Tools.

Project yarn belongs in:

- Row counter project workspace
- Library -> My Yarn

The Tools tab can still contain Yarn Estimator because that is a calculator, not the user's yarn library.

## 33. Insights

Insights can remain as it is conceptually.

Keep:

- Total time
- Average pace
- Completed projects
- Activity grid
- Time per project
- Project progress bars

Be careful with streak and productivity language. Knitting is a relaxing hobby for many users. Streaks can motivate some people, but they can also make the app feel like a productivity tracker.

If tips are removed from Library, avoid adding similar tip/helper blocks into Insights.

## 34. Voice, Mic, Parser, and AI-Like Surfaces

`PROJECT.md` describes local voice-command surfaces and a regex-based instruction parser. It also notes there is no model-based interpretation layer.

The current product decision appears to be that launch should avoid AI-related or assistant-like surfaces.

For UI:

- Remove or hide the microphone button if it is not intentionally part of launch.
- Remove AI summary rows.
- Remove AI scanner wording.
- Remove automatic yarn recognition wording.
- Avoid user-facing copy that suggests hidden AI.
- Hide Quick Tip if it feels like an assistant/tip feature and is not part of launch.

For code:

- Do not blindly delete internal voice/parser code unless that is a separate technical decision.
- If voice UI is removed, review whether user-facing feature labels, Pro feature copy, permissions, and settings still mention voice.
- If RECORD_AUDIO is no longer used by any launch feature, Codex should verify whether the permission should remain.

## 35. Pro and Trial Considerations

The app has a 14-day trial and one-time Pro purchase model.

Some project content may be Pro-gated according to current product logic.

If a row counter project card points to a Pro-gated feature, the UI should be clear.

Options:

1. Hide the card until Pro is active.
2. Show the card with a subtle lock and clear copy.
3. Show the card during trial and gate only after trial expiration.

For launch clarity, avoid filling the row counter with locked cards. The row counter should still feel useful and calm.

Recommended behavior:

- Core pattern access and basic row counting should remain central.
- Pro-gated cards should appear only when they are relevant or already used.
- If the user taps a locked card, route to the Pro explanation in a calm, non-aggressive way.

Codex should inspect existing Pro gating before changing behavior.

## 36. Widget Considerations

The widget already opens the app directly into the counter and has its own concise UI.

The row counter workspace redesign should not make widget launch unreliable.

When opening from widget:

- The selected active project should still load into the counter.
- The counter should still be immediately usable.
- Any new scrollable project cards should not block counting.

The widget itself does not need to show the full project workspace.

## 37. State and Navigation Considerations

Codex should account for the existing navigation model.

The counter route may not carry project ID directly. The active project may be selected and preserved in a shared counter view model or equivalent state.

This plan does not require changing that model unless the current code makes the new UI impossible.

When linking from My Yarn detail to the counter, keep the existing behavior of opening the linked project counter if available.

When attaching content to a project, make sure the operation uses the active project consistently.

## 38. Recommended Implementation Phases

### Phase 0, cleanup and alignment

- Confirm which UI changes from previous experiments are already in code.
- Hide or remove mic UI if not part of launch.
- Hide AI summary rows and AI-like wording.
- Hide Quick Tip from Library if that is the current launch decision.
- Remove duplicated small header actions if project cards will replace them.

### Phase 1, row counter workspace structure

- Make row counter scrollable.
- Keep counting usable without scrolling.
- Keep project title and pattern line visible.
- Add a Project section below the counter.
- Replace separate Quick Actions and Project Info table with large project content cards.

### Phase 2, project content cards

Implement cards for:

- Pattern
- Yarn
- Notes
- Photos
- Next Reminder, only when relevant
- Extra Counters, only when active

Each card should have filled and empty states.

### Phase 3, yarn linking

- Allow choosing from My Yarn.
- Allow adding a project-only yarn note if current code supports it or if this is added.
- Do not require a complete yarn card.
- Avoid camera wording that implies scanning.
- Keep My Yarn in Library.

### Phase 4, project list refinement

- Reduce raw PDF filename dominance.
- Prioritize project name, section/status, rows/date, yarn/attachments.
- Improve Continue Knitting with one or two useful progress details.

### Phase 5, Library cleanup

- Keep My Collection and Reference sections.
- Remove or hide Quick Tip if it is not part of launch.
- Update My Yarn description copy.
- Keep Saved Patterns, My Yarn, Photos, and reference cards large and clear.

### Phase 6, optional later work

- Consider a separate Project Overview screen only if the row counter becomes too crowded.
- Improve My Yarn creation if the app needs a full manual yarn card form.
- Add deeper direct attachment actions from project cards only after the simple flow works well.

## 39. Acceptance Criteria

The updated UI should pass these checks:

- A user can open an active project and increment the row without scrolling.
- The user can see the attached pattern from the row counter.
- The user can open or attach a pattern from a large, obvious area.
- The user can see linked yarn from the row counter if one exists.
- The user can add yarn to a project without creating a perfect yarn database entry.
- The user can see a useful note summary from the row counter if notes exist.
- The user can add a note without hunting through More.
- The user can open project photos from a clear row counter card.
- The user can see the next relevant reminder when one exists.
- Active extra counters are visible near the counter workflow.
- Administrative actions do not compete with daily knitting content.
- My Yarn works with partial data.
- No visible UI implies automatic yarn recognition unless it exists.
- No launch-facing mic, AI summary, AI scanner, or assistant-like tip UI remains unintentionally.
- Library still helps users find Saved Patterns, My Yarn, Photos, and Reference quickly.
- The app still looks like KnitTools.

## 40. Real Scenario Tests

Test with these project states:

### Empty project

- No pattern
- No yarn
- No notes
- No photos
- No reminders
- No extra counters

Expected result: The screen should not look broken. It should show clear add cards.

### Basic project

- Row counter only
- Target rows set

Expected result: Counting remains dominant. Optional attachments are discoverable but not overwhelming.

### Pattern project

- Pattern PDF attached
- No other resources

Expected result: Pattern is visible and easy to open.

### Sock project

- Pattern attached
- Yarn linked
- Heel turn note
- One upcoming reminder

Expected result: The user can see row, pattern, yarn, note, and reminder without opening More.

### Sweater sleeve project

- Target rows
- Repeat counter
- Shaping counter
- Yarn
- Multiple notes

Expected result: Main row count remains dominant. Extra counters are visible and not hidden.

### Photo-heavy project

- Several progress photos
- Notes
- Yarn

Expected result: Row counter shows photo summary only. Full gallery opens from Photos card.

### Partial yarn project

- Yarn name only, for example `Blue sock yarn`
- No brand, weight, fiber, or dye lot

Expected result: The card still looks intentional.

### Long filename project

- Very long PDF filename

Expected result: Project card does not become ugly. Filename is secondary or truncated safely.

### Non-Pro or expired trial state

- Project has Pro-gated features

Expected result: Locked states are calm and clear. Counter remains useful.

## 41. Copy Guidelines

Use direct, practical copy.

Good:

```text
Attach Pattern
Add a PDF or saved pattern to this project
```

```text
Yarn
Isager Highland Wool · 1 skein
```

```text
Add Yarn
Choose from My Yarn or add yarn just for this project
```

```text
Save label photo
```

Avoid:

```text
Scan yarn details
```

```text
Detect yarn color
```

```text
AI summary
```

```text
Smart yarn recognition
```

Avoid making the app sound smarter than it is. A reliable manual tool is better than an unreliable smart tool.

## 42. What Codex Should Be Careful About

- `PROJECT.md` may be older than the latest UI experiments and planning changes.
- Code remains the final source of truth.
- Do not assume a manual My Yarn creation form exists without checking implementation.
- Do not remove internal voice/parser code unless that is explicitly requested.
- Do not introduce new visual styling that conflicts with the locked KnitTools theme.
- Do not copy LooseLoop's visual style.
- Do not make row counter controls smaller just to fit more information.
- Do not duplicate the same Pattern/Yarn/Notes/Photos information in both quick cards and a separate info table.
- Do not overfill the first viewport.
- Do not make destructive actions easy to tap accidentally.

## 43. Final Direction

KnitTools should become clearer by making projects feel complete and connected.

The project list helps the user choose what to work on.

The row counter is the main active workspace.

Pattern, yarn, notes, photos, reminders, and extra counters belong visibly to the project.

Library stores reusable resources and references.

Tools stay as calculators and utilities.

Insights stays as reflection and value, not the core workflow.

The app should feel calmer, clearer, and more useful without changing its visual identity.
