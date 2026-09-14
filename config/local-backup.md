# Local backup v1

## Storage audit and design (2026-09-13)

The live database is Room 25, with 18 entities. This inventory was taken before implementation.

| Classification | Persistence | Decision |
| --- | --- | --- |
| MUST BACK UP | `counter_projects`, `counter_history`, `project_completions`, `project_counters`, `row_reminders`, `sessions`, `active_sessions` | All columns, including notes, reader compatibility fields and completion zones. Active monotonic anchors must become untrusted on restore. |
| MUST BACK UP | `project_folders`, `project_folder_assignments`, `yarn_cards`, `project_yarn_notes`, `project_yarn_usage` | All durable content and relationships. |
| MUST BACK UP | `saved_patterns`, `project_documents`, `pattern_annotation_layers`, `pattern_annotations`, `pattern_bookmarks`, `progress_photos` | All content, including inactive annotation layers and metadata-only web/Ravelry patterns. |
| MUST BACK UP | Referenced PDF/photo bytes | Resolve the five URI columns, including legacy app-owned `patterns`, `pattern_captures`, FileProvider and current file URIs. Copy readable legacy external document references as bytes as well. Never export a whole directory. Missing referenced bytes fail export. |
| SHOULD BACK UP | Ordinary preferences | V1 deliberately leaves device preferences in place: theme, language, haptics, awake, units, completed visibility, sort, dismissed tooltips. A separate DataStore commit cannot join the content replacement transaction. No raw preferences file is copied. Language stays under PreferencesManager/AppCompat/LocaleManager. |
| MUST NOT BACK UP | `trial_state` DataStore | Start timestamp, monotonic last-known wall timestamp, tamper flag and end notice stay installation-local. Restore never writes or clears these keys. |
| MUST NOT BACK UP | Play Billing, Firebase Auth and Ravelry auth | Purchase readiness and entitlement are Play-owned runtime state; Firebase credentials and backend OAuth tokens are not user content. Saved Ravelry Room metadata is included. Existing destination login is unchanged; a new installation reconnects normally. |
| TRANSIENT / REGENERATED | `counter_widget`, per-instance Glance preferences, `counter_launch_tokens`, `review_state` | No widget identities, trust tokens, review prompt counters or notices are transferred. |
| TRANSIENT / REGENERATED | Cache exports, capture/import sessions, orphan photos/PDFs, debug credentials/config, logs, SavedStateHandle | Excluded unless a durable Room file reference explicitly owns the bytes. Build/Firebase configuration and secrets are never traversed. |

## Chosen boundary

Use logical JSON-lines table export inside one Room transaction, with the existing pattern-reference lock and yarn-photo lock around snapshot/file copying. Published photos/PDFs are immutable unique files; replacement creates a new file, then updates Room, then deletes the old file. A failed or missing read fails the whole export. No live `.db`, WAL or SHM copying, SQLite snapshot API, database close, or process kill is needed. The API 29 path uses Room transactions and ordinary cursor reads only.

Restore validates the archive and loads it into a separate current-schema Room database, including production constraint callbacks. Only afterwards can the user confirm replacement. New durable files have fresh names. One transaction replaces all 18 tables in the live singleton; rollback leaves old rows and old files intact. IDs are rebased above the destination high-water marks so stale UI/widget actions and delayed old-project cleanup cannot target restored content. Relationships and document keys are rebased together. A private file journal records cleanup candidates before publishing any new files. On success, failure, or next startup, cleanup retains every currently referenced file and deletes only unreferenced journal candidates. This uses the committed database as the recovery authority, including a crash between SQL commit and journal cleanup.

Room remains at 25. Format/data versions start at 1; v1 only accepts schema 25. A future data version needs an explicit adapter and fixtures before its acceptance boundary changes.

## Product and archive contract

Settings > Backup & restore exposes SAF `CreateDocument(application/octet-stream)` and `OpenDocument`. The default filename is `KnitTools-backup-YYYY-MM-DD.knittools-backup`. Work uses the injected IO dispatcher, with operation phases, cancellation during export/validation, a validated preview, and explicit replace confirmation. All copy is in the existing `strings.xml` files for English, Danish, German, Spanish, Finnish, French, Italian, Norwegian Bokmal, Dutch, Portuguese and Swedish. Preview counts use complete resource labels; dates use the runtime locale.

The ZIP contains `manifest.json`, exactly 18 `tables/<table>.jsonl` files, and deduplicated `files/<sha256>.bin` payloads. The manifest records `format=KnitTools`, format/data version 1, schema 25, creation milliseconds, app version code/name, and each payload's path, byte size and SHA-256. A table starts with its sorted column-name array and then primitive JSON row arrays. Only the current schema's trusted column definitions can produce bound INSERT statements; archive text never becomes SQL. No database or journal bytes are copied.

Limits are 100,000 ZIP entries including the manifest, 2 GiB per durable file, 16 GiB total expanded payload and archive size, 256 MiB per table, two million rows per table, 8 Mi UTF-16 units per JSON line, and 32 MiB for the manifest. A bounded structural pass checks classic/ZIP64 directory counts, offsets and headers before opening `ZipFile`; the central directory is limited to 51,200,000 bytes. Extraction enforces the declared size while streaming, before accepting hashes. JSON nesting is bounded before parsing. The app reserves 32 MiB free space and estimates shadow/replacement database space separately; it may need several temporary copies of the content. Insufficient space aborts without replacing live data.

The ZIP structures follow the [PKWARE specification](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT). V1 accepts the single-volume structure emitted by its own writer, including ZIP64 when needed. Split/encrypted archives, archive comments and arbitrary ZIP layouts are not a backup compatibility promise.

## Replacement and recovery details

Validation extracts only allowlisted logical paths below `noBackupFilesDir/manual-backup/<operation>` and imports into an isolated Room 25 database. It checks SQL types, nullability, bounded integers/booleans, foreign keys, primary-document constraints, annotation payloads, and exact referenced-file inventory. The staged archive is validated again after confirmation. Each selection has a UUID owned by its ViewModel before validation starts; restore and preview cleanup must match it. An older app window cannot confirm or discard another window's staged backup. SHA-256 detects damaged bytes, not forgery: this format is neither encrypted nor authenticated, and the UI says it is not encrypted.

The live replacement transaction reserves fresh IDs above both live IDs and SQLite sequences, remaps foreign keys, yarn CSV links, annotation/bookmark/document keys and embedded chart-counter IDs, and then inserts all restored rows. Legitimately dangling weak metadata links become absent; dangling chart-counter IDs receive reserved unused identities so future counters cannot accidentally bind them. HTTP/Ravelry metadata is not rewritten. Active sessions keep checkpointed durations but lose boot trust and suggested elapsed time, receive new recovery/session tokens, and enter the existing conservative review flow.

Restored PDFs use fresh `pattern_pdfs/0/restore-<uuid>-<hash>.pdf` paths, shared where appropriate. Progress/yarn photos use their fresh project/card directories. Canonical file URIs replace source-device paths. The journal is written and fsynced to a temporary file and renamed before any durable file is published. Each new file is fsynced and hash-checked before SQL replacement. Ordinary failure or pre-commit cancellation rolls SQL back. Cleanup reads authoritative committed references under the same locks and removes only unreferenced old/new journal candidates. An incomplete pre-publication journal is ordinary disposable staging; a complete journal survives a process interruption until startup recovery. Failed cleanup retains its journal for a later startup.

Successful restore keeps the Room singleton open, verifies resulting file references, refreshes placed widgets on a best-effort basis, and clears saved navigation stacks when leaving the success screen. Old pending project actions cannot target fresh restored IDs. Destination settings, language, trial state, Play state and Ravelry login remain untouched; on another installation they follow that installation's normal initialization. Widget instances must remain device-local.

## Validation evidence

Recorded development and final check outputs are under the ignored `reports/local-backup` directory. No schema migration was added, and the existing schema/migration bytes are preserved.

The final production implementation passed 11 focused instrumented tests on the isolated API 36 x86_64 emulator `emulator-5582`: eight real Room/files/repository tests and three Compose tests. The repository tests cover all 18 tables, two PDFs, shared PDF references, progress/yarn images, a legacy FileProvider path, completion events, untrusted session anchors, invalid foreign keys/types despite valid hashes, replacement rollback, cancelled previews, stale confirmation/cleanup ownership across selections, staged-journal recovery, excluded preferences/trial/orphans, and valid/dangling chart-counter links. Compose coverage includes enabled/busy states, callbacks, preview and destructive confirmation, success/error messages, 320 dp width and 2x font scale. `AppStartupSourceTest` checks recovery ordering before photo pruning.

The Android DocumentsUI flow was exercised: restore the representative fixture, export through CreateDocument to Downloads, change row 12 to 13 in the app, select that exported file through OpenDocument, confirm replacement, and observe row 12 again. The project, a restored PDF with its visible black annotation, the progress-photo viewer, the red yarn-photo thumbnail, the 120-second session review, and the completion event in Insights were opened. `reports/local-backup/saf-final.knittools-backup`, `instrumentation-final.txt`, `apk-hashes.json` and `*-final.png` retain the synthetic fixture and evidence. After binding previews to selection UUIDs, `instrumentation-selection.txt` records all 11 tests passing, and the local picker/confirmation/Back-navigation path was repeated (`selection-restored.png`, `selection-project.png`).

Device verification used the debug build and local DocumentsUI, not Drive/Dropbox providers or API 29 hardware/emulation. Interruption tests exercise both journal/commit outcomes against real Room and files; an actual process kill during a write, physical disk exhaustion, large multi-GiB workloads, and spoken TalkBack traversal were not exercised. Debug builds retain their existing demo seeder: if its marker project is absent, a later startup adds sample projects. The unchanged release seeder is a no-op. Room writers wait during the snapshot/replacement transaction, and v1 may need multiple temporary copies of a backup's bytes.

## Final checks (2026-09-14)

All commands ran directly from `C:\Dev\KnitTools`, without aggregate check wrappers.

```powershell
.\gradlew.bat :app:kspDebugKotlin :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:ktlintCheck :app:detekt --rerun-tasks --continue --no-daemon --console=plain
.\gradlew.bat :app:kspDebugKotlin :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:ktlintCheck :app:detekt --continue --no-daemon --console=plain
adb -s emulator-5582 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5582 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5582 shell am instrument -w -e backupFixture true -e class com.finnvek.knittools.repository.BackupRepositoryTest,com.finnvek.knittools.ui.screens.backup.BackupContentTest com.finnvek.knittools.test/androidx.test.runner.AndroidJUnitRunner
git diff --check
```

The uncached task rerun passed in 18m20s (`final-checks-3.txt`). After the selection-ownership fix and final cleanup, the same task set passed in 7m42s (`final-checks-5.txt`): 27 tasks executed, 90 up-to-date. The final JVM execution ran all 1,846 tests with zero failures, errors or skips. The final APKs passed all 11 instrumented tests in 14.801 seconds (`instrumentation-final-2.txt`, `apk-hashes-final.json`). Kotlin/KSP, Android test compilation, both debug APK assemblies, ktlint and Detekt passed. Android lint reported zero errors and four warnings: three existing unused widget-preview resources and the backup free-space check's `UsableSpace` API suggestion. The conservative usable-space check deliberately does not count potentially reclaimable cache as immediately free storage.

No Room migration was added or changed by this task. The real Room 25 replacement and constraint callbacks were tested on API 36; the complete historical migration suite was not rerun. Earlier development failures are retained in the logs and were resolved; the final run had no failures. Final source-input hashes remained stable during checks, all prior unrelated work remained unchanged, and `git diff --check` passed. The task-owned emulator was stopped and its temporary AVD removed. No commit, push, pull request or merge was created.

## Files changed by this task

- PROJECT.md
- app/src/androidTest/java/com/finnvek/knittools/repository/BackupRepositoryTest.kt
- app/src/androidTest/java/com/finnvek/knittools/ui/screens/backup/BackupContentTest.kt
- app/src/main/java/com/finnvek/knittools/App.kt
- app/src/main/java/com/finnvek/knittools/data/backup/BackupArchive.kt
- app/src/main/java/com/finnvek/knittools/data/backup/BackupFiles.kt
- app/src/main/java/com/finnvek/knittools/data/backup/BackupFormat.kt
- app/src/main/java/com/finnvek/knittools/data/backup/BackupIdentityMap.kt
- app/src/main/java/com/finnvek/knittools/data/backup/BackupRestoreFiles.kt
- app/src/main/java/com/finnvek/knittools/data/backup/BackupTables.kt
- app/src/main/java/com/finnvek/knittools/data/backup/BackupZipStructure.kt
- app/src/main/java/com/finnvek/knittools/repository/BackupRepository.kt
- app/src/main/java/com/finnvek/knittools/repository/YarnCardRepository.kt
- app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt
- app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/backup/BackupScreen.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/backup/BackupViewModel.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt
- app/src/main/res/values-da/strings.xml
- app/src/main/res/values-de/strings.xml
- app/src/main/res/values-es/strings.xml
- app/src/main/res/values-fi/strings.xml
- app/src/main/res/values-fr/strings.xml
- app/src/main/res/values-it/strings.xml
- app/src/main/res/values-nb/strings.xml
- app/src/main/res/values-nl/strings.xml
- app/src/main/res/values-pt/strings.xml
- app/src/main/res/values-sv/strings.xml
- app/src/main/res/values/strings.xml
- app/src/test/java/com/finnvek/knittools/AppStartupSourceTest.kt
- app/src/test/java/com/finnvek/knittools/data/backup/BackupArchiveTest.kt
- app/src/test/java/com/finnvek/knittools/data/backup/BackupCoverageTest.kt
- app/src/test/java/com/finnvek/knittools/data/backup/BackupZipStructureTest.kt
- config/local-backup.md
