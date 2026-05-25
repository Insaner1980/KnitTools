# KnitTools Row Counter and Project Workspace Plan

## Goal

The row counter screen should become the main working space for an active knitting project.

It should still feel like KnitTools. The existing visual style, colors, typography direction, card language, rounded shapes, spacing, and earthy premium feel should remain. The goal is not to redesign the app visually. The goal is to reorganize the information so the user can see the important project details directly inside the row counter screen without the screen becoming cramped or confusing.

The screen should become scrollable. The counter itself should remain easy to use, large, and central. Additional project information should appear below the counter in clear, readable sections.

AI-related features should not be included. Remove any remaining microphone UI if it was tied to AI or voice commands.

## Current Problem

The current row counter screen is clean because it is mostly non-scrollable and focused on the counter. That works well for a simple counter, but it hides too much of the project context.

Important project information currently lives in separate places, icons, menus, or bottom sheets. The user may have attached a pattern, yarn, notes, photos, reminders, or extra counters, but the screen does not make all of that project context visible enough at a glance.

The result is that the counter screen is visually clean, but not yet a full project workspace.

## Product Direction

The row counter should be treated as a project workspace, not only as a number counter.

The user should be able to open an active project and immediately understand:

- What project they are working on
- Which pattern is attached
- Which yarn is linked to the project
- What row they are on
- Whether there are notes, photos, reminders, or extra counters
- What action they are likely to need next

The screen should not try to show every detail in full. It should show the most useful project-level summary and provide clear ways to open or edit deeper details.

## Visual Identity Must Stay Intact

Do not change the existing KnitTools visual identity.

Do not change:

- The color palette
- The typography direction
- The rounded card language
- The warm dark and light surfaces
- The orange accent
- The bottom navigation visual style
- The large tactile counter button
- The general KnitTools personality

The issue is information structure, not visual identity. New and edited features should look like they already belong inside the current app.

## Recommended Screen Structure

The row counter screen should be scrollable and divided into clear zones.

Recommended order:

1. Header
2. Pattern line
3. Counter area
4. Quick actions
5. Project info
6. Extra counters, if active
7. Manage project actions

This keeps the counter near the top and prevents the project details from overwhelming the main task.

## Header

The header should remain simple.

Recommended contents:

- Back button
- Project name
- Overflow menu

Remove the microphone icon if it was connected to AI or voice commands. If pattern, camera, note, or yarn actions are moved into Quick Actions, the header should not duplicate them too heavily.

The header should not become a row of many icons. Too many icons make the screen feel technical and less calm.

## Pattern Line

Under the project title, show the attached pattern directly.

If a pattern is attached:

```text
two_sleeves_one_promise.pdf
```

This should be tappable and open the pattern viewer.

If no pattern is attached:

```text
Attach pattern
```

This should be tappable and open the attach pattern flow.

This is important because the pattern is often one of the most important project resources while knitting. It should not only be hidden in a menu.

## Counter Area

The counter area should remain the visual and functional focus of the screen.

Recommended contents:

- Repeat or section control, if active
- Row label, for example `Row 67 / 88`
- Large current row number
- Progress bar
- Section counter, if active
- Minus button
- Large plus button
- Undo button

The plus button should remain large and easy to tap. It should not be pushed too far down by project information.

If the screen becomes scrollable, the first visible area should still allow the user to increment the row without needing to scroll.

## Quick Actions

Add a clear Quick Actions section directly below the counter area.

This section should provide fast access to common project actions without making the header crowded.

Recommended Quick Actions:

1. Open Pattern / Attach Pattern
2. Notes / Add Note
3. Yarn / Add Yarn
4. Photos / Add Photo

These four actions are the strongest fit because they match the project information users are most likely to attach and revisit.

Do not put too many quick actions here. Four is enough. More than four will make the screen feel busy.

## Dynamic Quick Action Labels

Quick Action labels should change depending on whether the project already has that item attached.

Pattern:

- If attached: `Open Pattern`
- If missing: `Attach Pattern`

Notes:

- If notes exist: `Notes`
- If missing: `Add Note`

Yarn:

- If yarn is linked: `Yarn`
- If missing: `Add Yarn`

Photos:

- If photos exist: `Photos`
- If missing: `Add Photo`

Each action can show a small secondary line when useful.

Examples:

- `Open Pattern` / `PDF attached`
- `Notes` / `Heel turn`
- `Yarn` / `Regia 4-ply`
- `Photos` / `1 photo`

This gives the user useful context without forcing them to open each section.

## Quick Actions Layout

Use large, readable buttons that match the existing KnitTools style.

Recommended layout:

```text
Quick Actions
[ Open Pattern ] [ Notes ]
[ Yarn         ] [ Add Photo ]
```

A 2 by 2 grid is likely better than a small horizontal chip row because the app already uses large cards, strong typography, and generous spacing.

The quick action buttons should not look like tiny utility chips. They should feel like calm project cards or action tiles.

## Project Info Section

Below Quick Actions, add a Project Info section that shows the main attached information in readable rows.

This section answers: “What has the user attached to this project?”

Recommended rows:

- Pattern
- Yarn
- Notes
- Photos
- Reminder

Only show useful rows. Do not force a long list of empty fields.

If a row has information, show it directly.

Examples:

```text
Yarn
Regia 4-ply, 1 skein
```

```text
Notes
Heel turn
```

```text
Reminder
Row 70: start decreases
```

```text
Photos
1 progress photo
```

Rows should be tappable. Tapping a row opens the relevant detail screen or bottom sheet.

## Empty Project Info Behavior

Do not make a new or empty project look broken.

If the user has not attached much information, avoid showing a long list of empty rows.

For a new project, show a compact helper card instead:

```text
Add to this project
Pattern · Yarn · Notes · Photos
```

This teaches the user what can be attached without overwhelming them.

After the user adds an item, that item should appear as a real row in Project Info.

## Yarn Linking

Yarn should be linked to projects in a flexible way. Do not require a perfect global yarn database before the user can attach yarn to a project.

When the user adds yarn to a project, offer two paths:

1. Choose from My Yarn
2. Add yarn to this project

`Choose from My Yarn` links an existing saved yarn entry.

`Add yarn to this project` creates a lightweight project-only yarn note. This can include only the information the user knows right now.

Optional later action:

- Save this yarn to My Yarn

This keeps project use fast and practical.

## Yarn Data Model for Launch

The app should not assume yarn information is automatically scanned or complete.

Yarn entries should work with partial information.

Minimum useful yarn entry:

- Name or short description
- Quantity, optional
- Linked project, optional
- Notes, optional
- Photo or label photo, optional

Optional detailed fields:

- Brand
- Weight category
- Fiber content
- Weight in grams
- Length in meters or yards
- Needle size
- Gauge
- Color name
- Color number
- Dye lot

The UI must look intentional even when most of these fields are empty.

## My Yarn Position in the App

Keep My Yarn inside Library under My Collection.

The current Library structure is strong:

- My Collection
  - Saved Patterns
  - My Yarn
  - Photos
- Reference
  - Needle Sizes
  - Size Charts
  - Abbreviations

This structure is clear and should not be replaced by a separate bottom navigation tab for yarn.

## My Yarn Description Copy

The current My Yarn description should avoid promising a complete automatic yarn stash.

Better options:

```text
Yarn saved for your projects
```

or

```text
Yarn notes, quantities, and linked projects
```

Avoid wording that implies automatic recognition of brands, weights, and colors.

## My Yarn List

The My Yarn list should remain text-based and card-based, matching the app style.

Do not use large realistic yarn ball images as the default list visual.

The list should support partial data gracefully.

Examples:

Minimal entry:

```text
Blue sock yarn
1 skein
Not linked
```

Normal entry:

```text
Regia 4-ply
1 skein · In use
Emergency Sock
```

Detailed entry:

```text
Regia 4-ply
Fingering · 1 skein · In use
Emergency Sock
```

Do not make missing brand, weight, color, or fiber information feel like an error.

## Yarn Photos and Label Photos

Without AI scanning, photo capture should be presented honestly.

A photo should be saved as a photo, not treated as automatic recognition.

Recommended actions:

- Add yarn manually
- Save label photo
- Add yarn photo

If the camera button remains, its purpose must be clear. A camera icon alone may imply scanning or automatic detection.

Prefer a plus action that opens options:

```text
Add yarn
Save label photo
```

This avoids promising automatic extraction of yarn details.

## Color Handling

Do not make color detection a central feature.

Color should be manual and optional.

Possible fields:

- Color name
- Color number
- Manual color tag
- Dye lot

If the user selects a manual color tag, it can be shown as a small swatch or dot. If no color is selected, the UI should still look complete.

Do not rely on automatic color extraction from yarn photos.

## Notes in the Row Counter

Notes should be easy to add from the row counter.

The Notes Quick Action should open a fast note entry flow, not force the user through a heavy management screen first.

Recommended behavior:

- Tap `Add Note`
- Open small bottom sheet or screen
- User writes note
- Save to current project
- Note summary appears in Project Info

If notes already exist, show the most useful note summary, not only the count.

Example:

```text
Notes
Heel turn
```

If multiple notes exist:

```text
Notes
Heel turn · 2 notes
```

## Photos in the Row Counter

Photos should be treated as project progress photos or reference photos.

The Photos Quick Action should add a photo to the current project.

In Project Info, show a simple summary:

```text
Photos
1 progress photo
```

Avoid showing large thumbnails inside the row counter screen unless the design remains clean. A text summary is safer for launch.

## Reminders in the Row Counter

Reminders are useful, but they do not need to be one of the main four Quick Actions unless they become a central feature.

If a reminder exists, it should appear in Project Info.

Example:

```text
Reminder
Row 70: start decreases
```

This is more useful than showing only `1 reminder`.

If no reminder exists, do not necessarily show an empty reminders row. It can be added from the manage area or from a smaller secondary action.

## Extra Counters

Extra counters should stay close to the counter area, not hidden only in Project Info.

If extra counters are active, show them below the main row counter or as a compact section below Quick Actions.

Example:

```text
Extra Counters
Sleeve repeats 6
Decrease section 37 / 72
```

Only show this section when extra counters exist.

## Manage Project Actions

Administrative actions should not be mixed with daily project content.

Move these lower on the scrollable screen or keep them in the overflow menu:

- History
- Rename
- Reset counter
- Archive project
- Delete project

These should not appear with the same visual priority as Yarn, Notes, Photos, or Pattern.

## Updated Row Counter Screen Outline

Recommended final structure:

```text
Header
Back · Project name · More

Pattern line
Open attached pattern or attach pattern

Counter area
Repeat
Row 67 / 88
Large 67
Progress bar
Section counter
Minus · Plus · Undo

Quick Actions
Open Pattern · Notes
Yarn · Add Photo

Project Info
Yarn: Regia 4-ply, 1 skein
Notes: Heel turn
Reminder: Row 70, start decreases
Photos: 1 progress photo

Extra Counters, if active
Sleeve repeats: 6
Decrease section: 37 / 72

Manage
History
Rename
Reset counter
```

## Projects List Cards

Project cards should become easier to scan without changing the visual style.

The card hierarchy should prioritize:

1. Project name
2. Current working section or status
3. Row count and last updated date
4. Linked yarn and quiet attachment indicators

PDF filenames should not dominate the card. They can appear inside the project or only when short and useful.

Suggested card format:

```text
Emergency Sock
Heel turn
63 rows · updated May 16
Regia 4-ply · 1 photo · notes
```

Another example:

```text
Scarf That Became Something Else
Texture repeat
126 rows · updated May 16
Cascade 220 Superwash · 1 photo · notes
```

The goal is not to remove useful information. The goal is to make the hierarchy clearer.

## Continue Knitting Card

Keep the Continue Knitting card. It is one of the strongest parts of the Projects screen.

It can show slightly more useful context without becoming crowded.

Example:

```text
CONTINUE KNITTING
Sleeve 1 of 2, allegedly
Row 67 of 88 · Repeat 6 · 3h 16m
```

Do not add too much detail. The card should remain a fast resume action.

## Optional Project Overview

The current main direction is to make the row counter itself the active project workspace.

However, if the row counter screen later becomes too crowded, consider a lightweight Project Overview screen.

A possible compromise:

- Tapping the play button opens the row counter directly.
- Tapping the project card body opens Project Overview.
- Tapping attachment indicators opens the relevant project content.

This is optional. It should only be added if the row counter cannot remain clear while showing enough project context.

## Insights Screen

Keep the Insights screen useful and calm.

Good elements to keep:

- Total time
- Average pace
- Completed projects
- Knitting activity grid
- Time per project
- Project-specific progress bars

Be careful with streak language. Some users like it, but others may not want a relaxing hobby to feel like a productivity app.

Use soft wording and avoid guilt-based text.

## What to Remove or Reduce

Remove remaining AI-related UI from the launch version.

Remove or hide:

- Microphone button, if it was tied to AI voice commands
- AI summary rows
- AI scanner wording
- Any label that implies automatic yarn recognition
- Any camera action that implies scanning when it only saves a photo

Reduce header icon count if Quick Actions are added. Avoid duplicate actions in both the header and Quick Actions.

## Why This Direction Works

This structure keeps the row counter fast while making it a true project workspace.

The user can still increment rows quickly, but they can also see the attached project context without hunting through menus.

The screen becomes more useful without becoming a cluttered dashboard because information is grouped by purpose:

- Counter area: current knitting action
- Quick Actions: common actions
- Project Info: attached project context
- Manage: less frequent administrative actions

This fits KnitTools better than copying a competitor’s stash-heavy or AI-heavy structure.

## Implementation Priority

Suggested order:

1. Remove microphone and any remaining AI-related UI from the row counter.
2. Make the row counter screen scrollable.
3. Keep the counter visible and easy to use at the top.
4. Add Quick Actions with four actions: Pattern, Notes, Yarn, Photos.
5. Add Project Info rows below Quick Actions.
6. Support project-only yarn notes, not only full My Yarn entries.
7. Move History, Rename, Reset, Archive, and Delete into a lower Manage section or overflow menu.
8. Update My Yarn wording and behavior so it supports partial manual data.
9. Refine Projects list card hierarchy so project name, status, row count, and linked yarn are easier to scan.
10. Improve the Continue Knitting card with one or two useful progress details.
11. Keep Insights useful but avoid making it feel like a productivity pressure system.

## Key Principle

Do not design the screen around perfect data.

Design it around real users who may attach only a pattern, one yarn name, a quick note, and one photo.

If the UI works beautifully with partial information, it will feel much more reliable and useful at launch.
