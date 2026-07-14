# Codex UI plan: Ravelry screen and Saved Patterns experience

Current date: 2026-06-10

This file is for Codex. It describes the desired UI direction. Codex should inspect the current app and make the final implementation decision based on the existing navigation, theme, data model, and screen structure.

## Product decision

Ravelry belongs under Tools. Keep it visible.

Do not move Ravelry's main entry point into Settings or bury it inside Library. Settings may later contain a secondary account management entry, but Tools > Ravelry is the real user-facing feature.

Do not split the visible pattern library into source-based categories such as:

- Saved from Ravelry
- Saved in KnitTools
- Ravelry Patterns
- Local Patterns

If a pattern is saved, imported, or downloaded into the app, it should appear in the normal Saved Patterns list.

The user does not need to constantly know where the pattern originally came from. The app can store origin internally for duplicate detection, source links, and future sync.

## UI principle

Ravelry is the discovery and import tool. KnitTools is the place where the user's saved patterns, projects, row counters, notes, yarn, and photos live.

The ideal user journey is:

1. User opens Tools > Ravelry.
2. User signs in or is already connected.
3. User searches Ravelry or browses real Ravelry pages.
4. User saves/imports a pattern.
5. The pattern appears in Saved Patterns.
6. User attaches the pattern to a project.
7. The project and row counter can open the saved pattern or its detail screen.

## Keep the current conceptual structure

The current Ravelry screen concept is acceptable:

```text
Ravelry

[Sign in with Ravelry]

Search | Saved Patterns

Search knitting patterns...
```

Do not redesign the whole app just to add backend security. Keep the screen recognizable, but clarify states and error handling.

## Not connected state

When Ravelry is not connected, the screen should still clearly show that Ravelry exists and what it is for.

Suggested structure:

```text
Ravelry

[Sign in with Ravelry]

Connect Ravelry to search and import patterns.

Search | Saved Patterns

Search knitting patterns...
```

If search cannot work without connection, keep the search field visible but disabled or show a small helper message:

```text
Sign in with Ravelry to search patterns.
```

Saved Patterns can still be visible if local saved patterns exist. The user should not lose access to already saved patterns just because Ravelry is disconnected.

Suggested empty Saved Patterns message:

```text
No saved patterns yet.
Search Ravelry or attach a pattern to a project.
```

## Connected state

When connected, do not keep showing a giant sign-in button.

Suggested structure:

```text
Ravelry

Connected as username

[Browse Ravelry]

Search | Saved Patterns

Search knitting patterns...
```

A small connected status is enough. Put Disconnect in the overflow menu or a secondary account options sheet, not as a main button.

Suggested overflow items:

```text
Ravelry account
Disconnect
Help
```

## Primary actions

The Ravelry screen should support these actions if technically feasible:

- Sign in with Ravelry
- Browse Ravelry
- Search Ravelry
- Save Pattern
- Open saved pattern
- Attach saved pattern to project
- Open on Ravelry
- Disconnect, secondary only

For launch, prioritize:

1. Sign in with Ravelry
2. Search
3. Saved Patterns
4. Import/save from URL or search result
5. Attach to project

Browse Ravelry is valuable if it can be added cleanly without destabilizing the release.

## Search tab

The Search tab is for finding patterns.

Suggested layout:

```text
Search
[Search knitting patterns...]

Recent or results list
```

Result card content:

```text
Pattern title
Designer name
Small thumbnail if available
[Save]
```

Optional practical metadata:

```text
Free
Paid
Already saved
```

Do not overload cards with technical source labels.

If a result is already saved, show:

```text
Saved
```

or change the action to:

```text
Open
```

## Browse Ravelry action

`Browse Ravelry` should open real Ravelry pages in Custom Tabs or Auth Tab. It should not use Android WebView for sign-in.

This gives the user Ravelry's own browsing and filtering experience without forcing KnitTools to rebuild all Ravelry discovery features.

When browsing in a Custom Tab, add one of these if technically feasible:

```text
Save to KnitTools
```

or

```text
Save Pattern
```

Use `Save to KnitTools` only inside the Ravelry web browsing context, because there the destination needs to be clear. After the pattern is saved, it appears under the normal Saved Patterns list.

Also support Android Sharesheet import. If the user shares a Ravelry URL to KnitTools from any browser, open the import confirmation flow.

## Saved Patterns tab

Keep the user-facing name:

```text
Saved Patterns
```

This list should include patterns saved from any source:

- Ravelry search
- Ravelry URL import
- local PDF attach flow
- future import sources

Do not split the default list by origin.

Pattern card content:

```text
Pattern title
Designer name or short subtitle
Optional small thumbnail
Optional availability label
```

Useful availability labels:

```text
Available offline
PDF attached
Open on Ravelry
Requires Ravelry
```

Use these only when they matter. For example, if a card has no local PDF and only opens on Ravelry, `Open on Ravelry` is useful. If every visible card is a local PDF, do not repeat `Available offline` everywhere unless the design needs it.

Pattern card actions can be direct or in a detail screen:

- Open
- Attach to Project
- Open on Ravelry
- Remove

## Import confirmation screen

When importing from a Ravelry URL, Custom Tab action, share target, or search result, do not silently save without confirmation unless the existing app already behaves that way and it is clearly better.

Suggested confirmation:

```text
Save pattern?

Pattern title
Designer name
Small image if available

[Save Pattern]
[Open on Ravelry]
[Cancel]
```

If a duplicate is found:

```text
Already saved

This pattern is already in Saved Patterns.

[Open Saved Pattern]
[Cancel]
```

If metadata cannot be fetched:

```text
Could not import this pattern.
You can still open it on Ravelry.

[Open on Ravelry]
[Cancel]
```

## Pattern detail screen

A saved pattern detail screen should avoid source-based language unless useful.

Suggested layout:

```text
Pattern title
Designer name

[Open Pattern]
[Attach to Project]
[Open on Ravelry]

Availability
PDF attached / Opens on Ravelry

Notes
```

If a local PDF exists, `Open Pattern` opens the local file or pattern viewer.

If no local PDF exists, `Open Pattern` can either open Ravelry or become `Open on Ravelry`. Do not imply that a PDF is downloaded when only metadata and a link are saved.

## Project attachment flow

The Attach Pattern flow should use the same Saved Patterns library.

Suggested project flow:

```text
Attach Pattern

Saved Patterns
Import from Ravelry
Attach PDF from device
```

If the user chooses Saved Patterns, show the same saved pattern list.

If the user chooses Import from Ravelry, go to Tools > Ravelry or open a compact Ravelry search/import flow.

If the user chooses Attach PDF, use the local file picker.

Do not force the user to understand technical source categories.

## Row counter integration

If a project has an attached pattern, the row counter or project workspace should show the pattern name directly.

Suggested line:

```text
Pattern name.pdf
```

or

```text
Pattern title
```

If no pattern is attached:

```text
Attach pattern
```

Tapping the pattern line should open the best available pattern action:

- local PDF viewer if a local file exists
- saved pattern detail if only metadata exists
- Ravelry page if the saved item only has a Ravelry URL and no local file

## Copy rules

Use simple copy.

Preferred labels:

```text
Ravelry
Sign in with Ravelry
Connected as username
Browse Ravelry
Search
Saved Patterns
Save Pattern
Save to KnitTools
Open Pattern
Open on Ravelry
Attach to Project
Available offline
PDF attached
Requires Ravelry
Disconnect
```

Avoid these unless technically true:

```text
Sync your library
Synced from Ravelry
Downloaded PDF
Offline pattern
```

Use `import` rather than `sync` unless true repeated sync exists.

Use `Save to KnitTools` only when the user is currently inside Ravelry web browsing or Android share context. In the app's own library, just use Saved Patterns.

## Visual style

Use the existing KnitTools visual language.

Do not copy the competitor screenshot's white/red styling. The reference is only useful for the idea that Ravelry can be a visible account/pattern source. KnitTools should keep its own style.

Keep:

- current dark earthy theme where used
- rounded cards
- calm spacing
- large touch targets
- readable typography
- orange accent for primary Ravelry action if that matches the current app

Avoid:

- too many tiny chips
- source labels on every card
- crowded headers
- making Disconnect visually equal to Search or Save
- a hidden Ravelry feature that users cannot find

## Loading and error states

Add explicit states. Do not fail silently.

Suggested messages:

```text
Connecting to Ravelry...
Ravelry sign-in was cancelled.
Could not connect to Ravelry. Check your connection and try again.
This Ravelry session expired. Please sign in again.
Could not import this pattern.
This pattern is already saved.
No saved patterns yet.
No results found.
```

If the backend is unavailable:

```text
KnitTools could not reach its Ravelry service. Try again later.
```

Do not expose raw OAuth errors to the user. Log sanitized diagnostic codes only.

## Accessibility

Keep the Ravelry UI usable with large text and screen readers.

Requirements:

- Buttons have clear accessible labels.
- Pattern cards are tappable but not overloaded with nested tiny targets.
- Loading state is announced if the current architecture supports it.
- Error messages are visible text, not only Toasts.
- Color is not the only signal for connected, saved, or error states.
- Touch targets should be comfortable.

## What not to do

Do not rename Saved Patterns to Saved from Ravelry.

Do not rename Saved Patterns to Saved in KnitTools.

Do not move Ravelry's main entry point to Settings.

Do not make Ravelry web pages the only way to access saved patterns.

Do not automatically save every Ravelry page the user visits.

Do not imply that paid PDFs or private files are downloaded unless the implementation truly downloads them with permission.

Do not show Ravelry token or technical account details in the UI.

Do not show a source label on every pattern card unless the user explicitly opens a technical detail view.

## Acceptance criteria

The UI work is acceptable when:

- Ravelry is still clearly visible under Tools.
- The Ravelry screen has clear not-connected and connected states.
- Search and Saved Patterns remain understandable.
- The visible library name is Saved Patterns.
- Imported Ravelry patterns appear in Saved Patterns.
- Local and Ravelry-origin patterns are not split into separate user-facing libraries.
- The app indicates practical availability when needed, such as Available offline or Open on Ravelry.
- Users can attach saved patterns to projects.
- Row counter/project workspace can open the attached pattern or pattern detail.
- Disconnect is available but secondary.
- Error states are clear and calm.
- The UI still looks like KnitTools.

## References

- Android Auth Tab: https://developer.chrome.com/docs/android/custom-tabs/guide-auth-tab
- Android Custom Tabs overview: https://developer.chrome.com/docs/android/custom-tabs
- Android Custom Tabs interactivity: https://developer.chrome.com/docs/android/custom-tabs/guide-interactivity
- Android receiving shared data: https://developer.android.com/training/sharing/receive
- OAuth 2.0 for Native Apps, RFC 8252: https://datatracker.ietf.org/doc/html/rfc8252
- Ravelry Goodies page: https://www.ravelry.com/about/goodies
