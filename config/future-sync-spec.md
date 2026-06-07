# KnitTools Future Sync Spec

This document is a future product and architecture boundary, not a v1 implementation plan.

## Boundary

Manual export/import or backup/restore comes before continuous sync.

Do not market continuous cross-device sync until conflict handling and background sync exist.

The current v1 Drive/Dropbox pattern PDF import remains a Storage Access Framework document picker path. It does not add a cloud account, provider SDK, OAuth token storage, background worker, server, or multi-device state model.

## First Shippable Step

The first sync-adjacent release should be user-initiated backup and restore:

- Export a complete KnitTools backup file to a user-chosen location.
- Import or restore a complete backup file selected by the user.
- Let Android's document provider UI surface local files, Drive, Dropbox, or other installed providers.
- Keep the operation explicit and one-shot.
- Do not imply that edits made on one device automatically appear on another.

## Product Gate

A later cloud backup or sync feature needs a Pro gate before implementation:

- Define which backup/export flows are free.
- Define which automatic or provider-authenticated flows are Pro.
- Keep local-only project editing usable without Pro.
- Avoid changing existing debug-only Pro override semantics while testing future gates.

## Data Model Scope

A future backup/sync model must cover all durable user data before it is marketed:

- Room tables for projects, counters, reminders, yarn, saved patterns, annotations, sessions, and preferences.
- App-owned files under pattern PDFs, pattern captures, progress photos, and yarn photos.
- Versioned export format and migration rules.
- Partial-restore behavior for missing files or newer schema versions.
- Privacy boundaries for Ravelry metadata, billing state, and local-only notes.

## Conflict Handling

Conflict handling must be designed before continuous sync:

- Define per-record identity across devices.
- Define merge semantics for counters, notes, reminders, yarn cards, saved patterns, photos, and reading-line state.
- Define what happens when the same project changes offline on two devices.
- Define user-visible conflict resolution for non-mergeable data.
- Preserve counter history and avoid silently overwriting recent local work.

## Multi-Device And Offline

Multi-device sync must be explicit about offline behavior:

- The app must remain usable offline.
- Writes queued offline need durable local state and retry rules.
- Background sync needs power, network, cancellation, and backoff rules.
- Sync status and errors need user-facing states before launch.
- Delete propagation must be reversible or clearly confirmed.

## OAuth And Token Storage

Provider-authenticated sync requires a separate security review:

- OAuth tokens must be stored in encrypted local storage.
- Token revocation must disable background access cleanly.
- Account disconnect must preserve local data unless the user explicitly deletes it.
- Export/import through Android document providers does not require provider OAuth inside KnitTools.

## Google Drive

Google Drive design must distinguish visible user files from hidden app data:

- Use `drive` for user-visible files that the user can find and manage in Drive.
- Use `appDataFolder` only for application-specific data not intended to be directly accessible by users.
- Do not assume files can move between `drive` and `appDataFolder`.
- Pick Drive scopes from the smallest set that supports the chosen behavior.
- Avoid `allDrives` unless shared-drive behavior is explicitly required.

## Dropbox

Dropbox design must stay least-privilege:

- Request minimum scopes for the chosen backup or sync operations.
- Mobile and desktop OAuth flows must use PKCE.
- Background access requires refresh-token handling and secure storage.
- Team scopes are out of scope unless a separate business/team product decision exists.

## Non-Goals For V1

These are not part of the current v1 feature-decision implementation:

- Continuous cross-device sync.
- Provider-specific Drive or Dropbox SDK integration.
- Provider OAuth for pattern PDF import.
- Server-side account sync.
- Automatic background backup.
- Cloud journal processing or cloud cleanup.
