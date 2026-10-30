# KnitTools Android Release Readiness Plan

- Onboarding release gate PASSED (2026-10-30): onboarding strings translated into all 11 non-English locales and MissingTranslation ignore removed. All 212 resources per locale passed completeness, placeholder, plural and comment checks. Phase 10 full test (2509 tests), Detekt and lint passed; evidence is in `reports/onboarding-implementation/PROGRESS.md`.

- Date: 2026-10-20; current-status reconciliation: 2026-10-28 (RW-11F)
- **Current status:** engineering reconciliation/backlog production is complete (RW-01/RW-11F); overall release readiness remains OPEN. [RW-11F report](reports/rw-11f-20261028/REPORT.md) is the finite remaining-work register. RW-CR-01 confirmed corrections and selected RW-03H/I/J physical checks are complete within their recorded scopes. [RW-11D](reports/candidate-20261028-rw11d-cbb80189/REPORT.md) completes local signed APK/AAB packaging/static verification; [RW-11E](reports/rw-11e-20261028/REPORT.md) passes only that exact APK's bounded physical local-PDF journey. Signing is not currently missing. Historical runtime results below remain bound to their original artifacts.
- **Remaining acceptance:** current case/branch and scanner-input evidence gaps, bounded consent SDK/integration limits, genuine signed trial expiry, selected physical camera/widget/haptic behavior, RW-06A's missing timing pair, wider conditional runtime/resource limits and external/service/CI/Play/owner prerequisites remain explicitly classified. The three RW-CR-01 owner decisions remain pending. No confirmed unresolved application defect or presently authorized local implementation/validation blocker was established by RW-11F; no successor selected.
- Historical implementation summary: sections 5, 6 and 7 implemented on 2026-10-20. Section 7 is complete within the automated/emulator evidence boundaries recorded below. W02 was subsequently verified with actual app-process termination and return through Recents. T08 was subsequently verified on 2026-10-22 with two actual background process replacements through the retained task, within section 5's saved-state/synthetic-entitlement emulator boundary. Live Ravelry W11 subsequently passed on the authorized Pixel 9 with the ordinary debug APK on 2026-10-22 (6.10); the same retained signed candidate subsequently passes the required live R10 assertions on 2026-10-23 (10.9). The shared adaptive foundation from 8.1–8.4 and the shared host in 8.6 is implemented; its scoped verification is recorded in 8.9. The scoped list/library/detail/tool/settings/notes/shared-form adaptations are implemented and verified within the boundaries in 8.10. Counter and Project Overview adaptations and focused verification are complete within the boundaries in 8.11. Photos, the PDF reader and the existing Ravelry browser width integration are implemented/verified within the scoped boundaries in 8.12. Insights adaptation and its focused validation are complete within the boundaries in 8.13. Whole-app adaptive integration, A01–A12, locally available W04 configurations and API 29 compatibility are complete within the isolated-app/emulator boundaries recorded in 8.14 on 2026-10-21; work package 2 has no remaining locally executable adaptive blocker. Physical/OEM and optimized-release behavior are not certified by this evidence. Other work packages remain outside these implementation tasks.
- Repository: `C:\Dev\KnitTools`
- Historical Section 10 checkpoint — 2026-10-21: optimized test-artifact smoke passed 7/7 and the CI selection passed 2/2 locally (10.6), with no GitHub workflow run. Signing was missing at that checkpoint; later 12.6 and RW-11D complete signed packaging. Signed R10 passed on the older 2026-10-23 candidate (10.9); R11 genuine-expiry/entitlement acceptance remains open.
- Scope boundary: the other-project incident in historical §4.1 is excluded from the current KnitTools queue. No incident-state assertion or credential/remediation action is part of RW-11F; its sensitive historical details were not reopened.
- Inspected branch: `codex/audit-korjaukset-20261018`
- Inspected HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`

## 1. Objective and scope

Make KnitTools substantially closer to release readiness by resolving the confirmed gaps identified in the Android documentation review and the subsequent discussion.

**Large-screen, landscape, foldable, and resizing work belongs to the current implementation scope. It must be implemented and verified now, together with the other work in this plan. It is not a post-release improvement or a deferred tablet smoke test.**

The required result includes:

1. Reliable restoration of the trial-ended notice and Ravelry browsing context.
2. External-camera capture without an unnecessary KnitTools camera permission.
3. Content-appropriate layouts, navigation, and continuity across supported window sizes.
4. Runtime validation of optimized release code, including the real release candidate.
5. Meaningful startup readiness reporting, repeatable startup measurements, and generated profiles included in the final artifact.
6. Focused resource, localization, manifest, navigation, and dependency-injection verification.
7. Explicit evidence for completed checks and a visible list of remaining release prerequisites.

This is a detailed plan for the reviewed Android scope, not a claim that every possible product, store, backend, device, or policy requirement has already been audited.

### 1.1 Authorization and working boundaries

- The current request authorizes writing this plan. It does not authorize implementing it in this planning task.
- A subsequent implementation request should execute the scoped work without asking for approval again at every phase.
- Preserve the existing dirty checkout. Many UI, localization, test, and documentation files already contain unrelated work.
- Use the existing feature branch if appropriate. Never implement directly on a protected integration branch.
- Do not commit, push, create a pull request, publish an app, or deploy a backend as part of executing this plan unless separately authorized. The repository forbids agent-created pull requests.
- Do not migrate navigation, replace Hilt, change the Room schema, lower `targetSdk`, restore Ravelry API/OAuth access, or upgrade unrelated dependencies.
- Do not run the user's umbrella check wrappers such as `lc` or `sc`. Use relevant direct Gradle tasks and explicitly scoped checks permitted by the repository.
- Do not add remote profiling, analytics, telemetry fields, or external analysis uploads.
- Keep existing product wording and visual conventions from `CLAUDE.md`. Use English for new source identifiers and comments under the user's current working rules.
- Do not rerun 320 dp, 200% font scale, their combination, or TalkBack checks. `AGENTS.md` explicitly says these were already tested and excludes repeating them without a specific user request. This plan does not override that instruction.

### 1.2 Meaning of completion

A task is complete only when its behavior, relevant failure paths, and integration have been verified. A source-code assertion, successful compilation, screenshot, or generated test file is not automatically runtime proof.

Use these statuses in implementation reporting:

| Status | Meaning |
| --- | --- |
| Not started | No implementation evidence yet. |
| Implemented | Code exists, but required verification is incomplete. |
| Verified | The stated check actually passed against identified inputs. |
| Blocked | A named prerequisite prevents the required check. |
| Not applicable | The requirement does not apply, with a concrete reason. |
| Excluded by instruction | The user or repository explicitly excludes the check. |

Do not report the reviewed scope as release-ready while a required candidate, state-restoration, adaptive-layout, or optimized-runtime check remains blocked.

## 2. Verified starting point

The following facts were checked against current source/configuration during planning. Runtime behavior was not exercised in this planning task.

| Area | Current evidence | Consequence |
| --- | --- | --- |
| Toolchain | AGP `9.4.1`, Kotlin `2.4.21`, Gradle `9.8.1`, Compose BOM `2026.09.00`; documented JDK 17 | Preserve the toolchain unless a narrowly required dependency exposes a verified incompatibility. |
| Android support | `minSdk = 29`, `compileSdk = 37`, `targetSdk = 37` | Test the existing target behavior; do not lower the target to avoid adaptive requirements. |
| Orientation | `AndroidManifest.xml` requests portrait for `MainActivity` | Portrait cannot be relied upon to protect the layout on modern large-screen devices. |
| Trial notice | `MainActivity.showTrialEndedNotice` is an Activity property initialized to false; it is not saved | A visible notice can disappear on Activity recreation. |
| Trial claim | `TrialManager.claimTrialEndNotice()` atomically persists that the notice has been claimed | Preserve the once-only claim behavior while restoring the pending presentation. |
| Ravelry | `RavelryBrowserScreen` owns a WebView with `remember`; its factory loads the start/search URL; disposal destroys it | Page and browser-history restoration are missing. |
| Ravelry navigation | `RavelryLinks.kt` distinguishes start URLs, in-app navigation, external links, payment destinations, and redirects | Reuse the existing policy; restoration must not silently bypass it. |
| Camera | Manifest declares `CAMERA`; both capture flows request it before `TakePicture()` | Remove the unnecessary app permission and permission-specific UI/control flow. |
| Camera failure handling | Both capture flows already catch launch failures including `SecurityException` | Do not describe the current issue as a demonstrated uncaught crash. Preserve cleanup. |
| Shared scaffold | `ToolScreenScaffold` applies a private 600 dp maximum to all its content | Make width selection explicit, so a browser or other expansive content is not constrained like a form. |
| Tools overview | `HomeScreen` also uses a 600 dp maximum | Centralize the relevant width token without imposing it on every screen. |
| Main navigation | `NavGraph.kt` always uses the bottom bar where top-level navigation is shown | Add a suitable wide-window navigation presentation while retaining route behavior. |
| Photo lists | `AllPhotosScreen` and `PhotoGalleryScreen` use `GridCells.Fixed(2)` | Adapt gallery columns to the available content width. |
| Counter | Existing `BoxWithConstraints` logic and `CounterControlsMaxWidth` already constrain some controls | Extend the existing layout rather than replacing its working behavior. |
| R8 | Release enables minification, resource shrinking, and optimized default rules | The primary gap is proving optimized runtime behavior, not enabling optimization. |
| Keep rules | `app/proguard-rules.pro` currently contains comments rather than broad app keeps | Do not add speculative blanket rules for Room, Hilt, serialization, or the whole app. |
| Release prerequisites | Build logic requires signing environment variables and real Firebase configuration for release artifacts | Do not bypass the gate or treat a placeholder benchmark build as the real candidate. |
| CI | `.github/workflows/build.yml` assembles debug and runs unit/static checks | No explicit optimized-runtime smoke job was found there. Do not infer that all CI work is debug-only: `test` and `lint` are broad tasks. |
| Profile generator | `BaselineProfileGenerator.startup()` only calls `startActivityAndWait()` | Extend the exercised paths and add separate timing measurements. |
| Profile evidence | No checked-in `baseline-prof` or `startup-prof` source file was found | Generate and verify app profile inputs and final packaging; library profiles may still exist. |
| Startup reporting | No app `ReportDrawnWhen`/`reportFullyDrawn`, `StartupTimingMetric`, or `measureRepeated` implementation was found | Implement readiness and measurement as distinct responsibilities. |
| Project loading | `ProjectListViewModel.activeProjects` initially exposes an empty list; folder state has separate loading information | An empty list cannot establish whether the first real project result has loaded. |
| Architecture | Hilt, Room, repository boundaries, injected dispatchers, and existing navigation state-saving conventions are established | Preserve these; no DI or navigation rewrite is justified by the linked guides. |

### 2.1 Source locations to use

All paths below are repository-relative implementation locations, not promises that every file must be edited.

| Responsibility | Existing source |
| --- | --- |
| Activity state and startup | `app/src/main/java/com/finnvek/knittools/MainActivity.kt` |
| Trial claim/state | `app/src/main/java/com/finnvek/knittools/pro/TrialManager.kt` |
| Browser lifecycle | `app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryBrowserScreen.kt` |
| Browser URL policy | `app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryLinks.kt` |
| Progress capture | `app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt` |
| Pattern capture | `app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt` |
| Capture persistence | Existing `CounterViewModel`, `ProgressPhotoRepository`, and pattern storage/import owners |
| App shell | `app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt` |
| Tab semantics | `app/src/main/java/com/finnvek/knittools/ui/navigation/TopLevelNavigation.kt` and `Screen.kt` |
| Shared layout | `app/src/main/java/com/finnvek/knittools/ui/components/ToolScreenScaffold.kt` |
| Layout tokens | `app/src/main/java/com/finnvek/knittools/ui/theme/ComponentDimens.kt` and existing screen-specific dimension objects |
| Project readiness | `app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt`, `ProjectFoldersState.kt`, `ProjectListScreen.kt` |
| Counter layout | `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt` and `CounterScreen.kt` |
| PDF reader | `app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt` |
| Profile producer | `baselineprofile/build.gradle.kts` and `baselineprofile/src/main/java/com/finnvek/knittools/baselineprofile/BaselineProfileGenerator.kt` |
| Build and CI | `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.github/workflows/build.yml` |
| Release boundaries | `config/security-decisions.md`, `config/posthog.md`, existing release-surface implementation and tests |

Recheck these locations at implementation time because the checkout is actively changing.

## 3. Execution order

All work packages below are part of the current release-readiness effort.

| Order | Work package | Dependency | Required result |
| --- | --- | --- | --- |
| 0 | Preserve the checkout and establish evidence inputs | None | Known baseline, available devices, and build prerequisites |
| 1 | Trial notice, browser restoration, camera permission removal | 0 | Focused functional fixes with recreation/capture coverage |
| 2 | Adaptive shell and content layouts | 1 for reliable continuity tests | Actual large-screen/landscape/foldable fixes now |
| 3 | Resource/pseudolocale and manifest/navigation regression checks | 1–2 | Correct resources and stable navigation under the new layouts |
| 4 | Optimized runtime smoke and CI coverage | 1–3 | Minified runtime evidence and a real release-candidate path |
| 5 | Startup readiness, timing, profile generation | 1–2; optimized variant available | Meaningful TTID/TTFD measurements and packaged profiles |
| 6 | Final candidate verification and closure | All preceding packages | Evidence-backed completion or an explicit blocker list |

An early optimized build can run before all UI work is complete to discover R8 problems sooner. Final profile generation and final candidate verification must use the completed UI and startup implementation.

## 4. Work package 0: preparation and evidence

### Implementation preparation

1. Read current `AGENTS.md`, `CODEX.md`, and the relevant `CLAUDE.md` sections.
2. Record branch, HEAD, dirty paths, staged paths, and relevant input hashes. Do not stash, reset, clean, or overwrite unrelated work.
3. Confirm the current toolchain from the wrapper and version catalog; use JDK 17.
4. Check whether a compatible AndroidX WebKit or WindowManager artifact already exists in the resolved dependency graph before adding direct dependencies.
5. List available test devices/emulators and select explicit serials. Use disposable test devices or isolated packages for destructive test setup.
6. Check release prerequisite availability without printing values:
   - `KNITTOOLS_KEYSTORE_PATH`
   - `KNITTOOLS_KEYSTORE_PASSWORD`
   - `KNITTOOLS_KEY_ALIAS`
   - `KNITTOOLS_KEY_PASSWORD`
   - ignored `app/google-services.json` or `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`
7. Record whether actual-device performance measurement and actual-camera testing are available.
8. Inspect generated Gradle tasks before writing commands for benchmark/profile variants. Do not guess plugin-generated task names.
9. Inspect relevant existing tests before adding new tests. Reuse their fixtures and isolation rules.

### Evidence format

For each actual check, record:

- work-package/test-case ID;
- source revision plus relevant dirty-input identity;
- command or manual steps;
- build variant and application ID;
- APK/AAB hash when applicable;
- device model/serial, Android API, WebView version where relevant;
- logical window width/height, density, posture, and theme for adaptive cases;
- expected result, actual result, and evidence location;
- failures, exclusions, or unavailable environments.

Keep generated reports local and ignored. A compact completion report can refer to them; do not commit generated `reports/`.

### Acceptance

- [ ] Existing work is identified and preserved.
- [ ] Available validation environments are known.
- [ ] Missing signing/configuration/device prerequisites are listed explicitly.
- [ ] No implementation choice depends on an assumed tool or API version.

### 4.1 INC-SIGN-01: signing-password disclosure assessment — 2026-10-22

**Disposition: assessment complete; credential-owner response pending, not resolved.** The confirmed incident concerns two `SATEENKAARI_*` password settings in the user's global Gradle configuration. It is not evidence that KnitTools' production key or Ravelry credentials were disclosed. This record replaces the generic recurring incident status; it does not reopen completed T08, renderer recovery or debug live-W11 results.

#### Evidence and ownership

- **Confirmed operation:** an ad hoc `functions.exec` / `exec_command` toolchain-inspection command ran `Get-Content "$env:USERPROFILE/.gradle/gradle.properties" -ErrorAction SilentlyContinue` without filtering. Its purpose/context was JDK discovery during the 8.12 media/browser work. It did not invoke a repository script, redirect the result to a report, open the referenced keystore, or run a key-export operation. Do not rerun this unsafe command.
- **Primary local evidence:** chat `Mukauta kuva-, PDF- ja Ravelry-näkym`, session `01a15d04-a7a2-7983-818d-9cf14234bc50`; existing `%USERPROFILE%\.codex\sessions\2026\10\21\rollout-2026-10-21T08-52-28-01a15d04-a7a2-7983-818d-9cf14234bc50.jsonl`. Line 139 records call `call_1cdadead56174a2494253bfa5251be44` at **2026-10-21 05:56:53 UTC**; lines **141 and 143** retain the unredacted completion/tool result (05:56:55–05:56:56 UTC). These references locate sensitive evidence; never display those raw records.
- **Confirmed categories:** `SATEENKAARI_KEYSTORE_PASSWORD` and `SATEENKAARI_KEY_PASSWORD` were nonempty and printed; `SATEENKAARI_KEYSTORE` and `SATEENKAARI_KEY_ALIAS` disclosed location/alias metadata. The four configuration assignments in that output contain no `KNITTOOLS_*` or Ravelry settings. No other secret category was identified in the inspected operation.
- **Ownership boundary:** the setting namespace and referenced filename point to Sateenkaari/Sateenkaarikello; the referenced path is outside `C:\Dev\KnitTools`. Its actual consuming applications, any cross-project password/key reuse, and whether it is an upload key, app-signing key, or only a locally distributed app key remain **unverified**. A global Gradle file is not a KnitTools credential inventory. The user/credential custodian, EmmaH, owns the remaining verification; no other project's source or configuration was edited.
- **Confirmed current state:** an in-memory comparison against the two specifically identified properties on 2026-10-22 found both still present and equal to the disclosed values. Only presence/equality booleans were emitted, never values, fragments or password hashes. The referenced keystore exists; its contents were not read or unlocked. Stale configuration is possible, so this does not prove those passwords still unlock it.
- **Reported, then checked within a boundary:** the original assistant's final message at line 2219 acknowledged disclosure and said the values were not copied into project files. A current exact-value check in memory found **0 matches in 166 readable text files**: text reports/before-copies under `reports/adaptive-media-20261021/`, current paths named by that task's `changed-inputs.txt` and `new-inputs.txt`, plus this plan, `app/build.gradle.kts`, `tools/release-surface.ps1` and `config/security-decisions.md`. This is a bounded negative result, not proof of universal non-exposure. Images, archives/APKs, other reports, Git history and remote/published artifacts were not scanned.

#### Exposure boundary and remediation evidence

| Surface or question | Disposition |
| --- | --- |
| Tool/model conversation and local retention | **Confirmed:** password values reached tool output/model context and remain in the original local session's completion and tool-result records. Existing evidence was retained unchanged; no additional raw copy was made. |
| Private key / keystore disclosure | **Not established:** the offending command read the properties file, not the keystore bytes. Password disclosure is distinct from disclosure of the protected key. This does not prove the keystore was never copied elsewhere. |
| Local access | **Confirmed current ACLs:** the properties file, incident transcript and referenced keystore each inherit Allow ReadAndExecute/Synchronize for local `CodexSandboxUsers`, with FullControl for EmmaH, SYSTEM and Administrators. These ACLs describe permitted local access, not who actually accessed the files or their historical ACLs. Review of intended readers and possible keystore copies remains necessary. No ACL was changed. |
| Local reports / repository files | No match within the 166-file scope above; no report redirection in the offending command. The exposed user configuration and session are outside this repository. This does not certify all tracked/untracked files or Git history as clean. |
| Shared chats, exports, service retention, published artifacts | **Unknown:** no publication/export was identified in the inspected operation; sharing recipients, provider retention/access, other log copies and remote artifact contents were not established. No external account/history sweep or third-party contact was performed. |
| Subsequent remediation | No password change, key reset, revocation or completed access review is documented in the inspected original incident record or current plan. The unchanged password settings are evidence against assuming configuration remediation; they are not an unlock test. Missing environment variables, an ignored file, or a passing scanner cannot close this incident. |
| Historical Ravelry findings | **Separate:** `config/security-decisions.md` records removal of Android's historical embedded API/OAuth credential path. That is unrelated to these SATEENKAARI signing-password fields. The Ravelry known-secret guard and its unavailable comparison inputs neither assess nor resolve INC-SIGN-01. |

#### Response required before closure

The evidence warrants changing any still-used disclosed protection passwords and reviewing access to the keystore and retained output. **It does not by itself warrant generating/replacing a signing key or changing Play Console.** Password replacement protects the retained keystore; it cannot revoke a private key or neutralize an old keystore copy already held with its old passwords. Android distinguishes private keys from public certificates and requires protecting both signing keys and their passwords. [Android signing guidance](https://developer.android.com/studio/publish/app-signing#secure-key)

**One next owner action — EmmaH:** separately authorize a bounded remediation of the identified **SATEENKAARI signing configuration**. Its prerequisites are identification of the actual keystore owner/consumers and key role, a trusted recoverable backup, and review of the intended readers/copies of the three known sensitive files (including whether the chat was shared/exported). For an active password-only exposure, change both applicable protection passwords using the actual keystore format's supported procedure, update only confirmed consumers, and verify the public certificate/key identity is unchanged and the new credentials work without printing them. Review known backups so old-password copies are not left accessible. If the settings are stale, establish and record that neither disclosed password is still used before closing that part. Never paste passwords into chat or put them in recorded command-line arguments. Java's `storepasswd` and `keypasswd` affect different protections; format compatibility must be established before choosing the exact operation. [JDK 17 keytool reference](https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html#commands-for-managing-the-keystore)

If that review establishes or gives reason to suspect **private-key compromise**, stop the password-only closure: for an enrolled **upload key**, prepare a separately authorized Play upload-key reset; for an **app-signing key**, assess Play App Signing enrollment and the applicable key-upgrade/update-continuity procedure. These are different operations; a lost self-managed app-signing key has no equivalent upload-key-reset recovery. No Play registration/enrollment or key role was assumed or changed here. [Google Play key-management and recovery guidance](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)

**KnitTools production-signing consequence:** current `app/build.gradle.kts` uses only the four `KNITTOOLS_*` environment settings for release signing, with no SATEENKAARI/global-property fallback. This incident establishes no need to rotate a KnitTools signing key. Before provisioning production signing, EmmaH must confirm the intended KnitTools key/passwords are independent of the disclosed pair, or include any reuse in the owner remediation above. Do not substitute these exposed settings into KnitTools. Production packaging remains independently blocked by the legitimate signing/service prerequisites in 9.4/10.3; the availability check below does not resolve the incident. Once independence is confirmed, remediation of an unrelated app's credentials is not itself a KnitTools signing gate.

#### Completed prevention and focused validation

The cause was ad hoc inspection, so no repository script correction is justified. For future prerequisite checks, emit only allowlisted setting names and booleans. For the original JDK question, check only whether `org.gradle.java.home` is configured; do not dump global/user properties, all environment variables or Gradle's complete property model. If a diagnostic needs a specific value, process and redact locally before any tool output, including exception output.

```powershell
$signingSettingNames = @(
    'KNITTOOLS_KEYSTORE_PATH', 'KNITTOOLS_KEYSTORE_PASSWORD',
    'KNITTOOLS_KEY_ALIAS', 'KNITTOOLS_KEY_PASSWORD'
)
foreach ($settingName in $signingSettingNames) {
    [pscustomobject]@{
        Setting = $settingName
        Present = -not [string]::IsNullOrWhiteSpace(
            [Environment]::GetEnvironmentVariable($settingName, 'Process')
        )
    }
}
$gradlePropertiesPath = Join-Path $env:USERPROFILE '.gradle/gradle.properties'
$javaHomeConfigured = $false
if (Test-Path -LiteralPath $gradlePropertiesPath -PathType Leaf) {
    foreach ($propertyLine in [IO.File]::ReadLines($gradlePropertiesPath)) {
        if ($propertyLine -match '^\s*org\.gradle\.java\.home\s*[=:]\s*\S') {
            $javaHomeConfigured = $true
            break
        }
    }
}
[pscustomobject]@{ Setting = 'org.gradle.java.home'; Present = $javaHomeConfigured }
Remove-Variable propertyLine -ErrorAction SilentlyContinue
```

Executed checks for this documentation-only assessment: the snippet above emits only names/booleans (all four KnitTools signing settings absent; user JDK override absent); the bounded 166-file comparison and three ACL inspections above; `git diff --check`, plus a separate whitespace check for this already-untracked plan. No application build/test, key operation, credential/configuration/ACL change, evidence deletion, commit, push, PR or external mutation was performed. Only this plan was edited on the existing dirty feature branch; temporary commands exited naturally and no helper/server was left running. Completed T08, renderer recovery and ordinary-debug live W11 retain their evidence. Production packaging, optimized/signed R10, remaining R11, physical timings, final profile/artifact refresh and unrelated security issues remain separate work packages.

## 5. Work package 1A: trial-ended notice restoration

### Required behavior

A trial-ended dialog that is already pending or visible must remain pending through normal Activity recreation and saved-instance restoration. A dialog the user has dismissed must not return because of recreation.

### Implementation

1. Keep `TrialManager.claimTrialEndNotice()` as the atomic once-only claim boundary.
2. Save and restore the Activity's pending notice flag using its existing instance-state lifecycle, or an equivalently small Compose saveable-state owner. Prefer the existing Activity save/restore pattern.
3. Restore before the dialog decision and claim effect can replace the restored value.
4. Keep the flag pending until the existing explicit dialog action consumes it.
5. Preserve both existing actions:
   - See Pro clears the notice and opens the upgrade flow.
   - Continue Free clears the notice.
6. Continue to wait for real Pro-state initialization before displaying the notice.
7. Suppress obsolete pending presentation if the verified current state no longer represents an expired trial.
8. Preserve the existing suppression behavior for external launches and contextual prompts. Recreation must not introduce an unrelated trial popup into an incoming PDF or widget flow.
9. Preserve other saved Activity keys, including consumed share and counter-launch identifiers.

Do not move the persisted claim outside its atomic DataStore operation or reset it whenever a new Activity is created.

### Tests and acceptance

| ID | Scenario | Expected result |
| --- | --- | --- |
| T01 | Expired trial becomes eligible | One notice appears. |
| T02 | Recreate Activity while notice is visible | Notice remains available; claim is not duplicated. |
| T03 | Dismiss with Continue Free, then recreate | Notice stays dismissed. |
| T04 | Choose See Pro, then recreate | No duplicate notice or duplicate upgrade destination. |
| T05 | Saved pending flag, current purchased-Pro state | Expired-trial notice is not displayed. |
| T06 | Initial Pro state is still loading | No premature expired-trial notice. |
| T07 | Suppressed external-launch path | Existing suppression and destination behavior remain correct. |
| T08 | Background app-process termination after state was saved | Pending presentation restores without re-claiming; deliberate termination is not evidence of naturally occurring memory-pressure reclamation. |

Extend relevant existing `TrialManagerTest`, `TrialStartSourceTest`, and isolated trial runtime coverage. Use a real Activity recreation test for T02–T04; a test that merely checks the presence of a Bundle key is insufficient.

**Guarantee boundary:** saved-instance restoration cannot guarantee delivery after every abrupt process termination, force-stop, cleared task, or crash between persisting a claim and saving UI state. Do not claim exactly-once visible delivery across those events. A durable pending/acknowledged notification protocol would be a separate behavioral change, not a necessary expansion of this small restoration fix.

### Implementation record — 2026-10-20

**Original status on 2026-10-20: Implemented; verification incomplete only for T08.** This task implements section 5 only. The 2026-10-22 follow-up below closes T08 within its explicit emulator/saved-state boundary.

- `MainActivity` saves and restores the pending notice before installing Compose content. A restored pending notice does not attempt another claim.
- The existing atomic `TrialManager.claimTrialEndNotice()` implementation is unchanged. Both dialog actions still consume the pending notice; See Pro retains the existing upgrade navigation.
- Presentation waits for launch initialization and verified expired-trial state. A verified non-expired state clears an obsolete pending notice; an uninitialized state does not consume it.
- Passive-notice suppression is also saved because handling a widget, counter, shared link, or incoming PDF consumes its launch intent. Restoration must retain suppression after those intent fields disappear.
- Existing share-consumption and counter-request saved keys are preserved. No schema, dependency, entitlement policy, product wording, or other work package was changed.
- Added `app/src/androidTest/java/com/finnvek/knittools/pro/TrialNoticeRestorationTest.kt`: real Activity recreation, rendered Android UI assertions, real DataStore claims, and synthetic entitlement/readiness inputs restricted to the isolated test package.

| Case | Status | Executed evidence |
| --- | --- | --- |
| T01 | Verified | A real persisted expired-trial fixture displays the notice. Four concurrent DataStore claims have exactly one winner; another manager cannot claim it again. |
| T02 | Verified | A new Activity instance displays the pending notice after `ActivityScenario.recreate()`; the persisted claim remains consumed. |
| T03 | Verified | Continue Free dismisses the notice; after recreation the Projects content is visible without the notice. |
| T04 | Verified | See Pro opens the upgrade screen, which survives recreation without the notice. One Back action returns to Projects, proving no duplicate upgrade destination. |
| T05 | Verified | Change to purchased Pro while stopped, recreate and resume: no notice. Returning to expired state and recreating again does not resurrect the obsolete pending flag. Purchase input is synthetic. |
| T06 | Verified | With readiness withheld, the restored pending notice stays hidden; releasing readiness displays it without another claim. Readiness is controlled only in the test. |
| T07 | Verified | Pending notices stay suppressed after counter, widget-prompt, web-share and incoming-PDF handling and recreation. Counter/PDF/share content restores. The existing widget prompt remains transient; its restored Projects destination has no passive notice. The existing `AuditPart15LaunchTest` also passed. |
| T08 | Verified on 2026-10-22; originally open | The follow-up below proves two actual background app-process replacements through the retained task, with the persisted claim consumed throughout. It resolves the old process-local synthetic billing fixture limitation without changing production billing or trial state. |

#### Executed checks and inputs

- Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`, with the existing dirty checkout preserved.
- Gradle 9.8.1, Eclipse Temurin JDK 17; debug variant with `-Pknittools.isolatedTest=activepolicy`.
- Device: `medium_phone` AVD, serial `emulator-5584`, model `sdk_gphone64_x86_64`, Android API 36.
- Installed target: `com.finnvek.knittools.activepolicytest`; test package: `com.finnvek.knittools.activepolicytest.test`. No live Play Billing or network-service validation is claimed.
- App APK SHA-256: `861D1B6CC9922DBA6B87F55A901DFA580EC5A8DAD073B234CB6CCB217D61C09C`.
- Final test APK SHA-256: `BC04F2DF5D24DFB7C954C92463D51C05020407A19989B36471E76CDFD136C662`.
- `MainActivity.kt` SHA-256: `9B108B3C673868836837745B6C000C2FBDC84F970DC5C549DB63DA7AEEC6127F`.
- `TrialNoticeRestorationTest.kt` SHA-256: `6712A0A831F3F57CD435FE6D7C0E0C530D5AA440A8A2BE4ACDE8F9BE33E50789`.

The following direct Gradle checks passed:

```powershell
.\gradlew.bat --no-daemon :app:ktlintCheck :app:testDebugUnitTest `
  --tests 'com.finnvek.knittools.pro.TrialManagerTest' `
  --tests 'com.finnvek.knittools.pro.TrialStartSourceTest' `
  --tests 'com.finnvek.knittools.ProFeatureGateSourceTest' `
  --tests 'com.finnvek.knittools.AppStartupSourceTest' `
  :app:assembleDebugAndroidTest :app:assembleDebug `
  '-Pknittools.isolatedTest=activepolicy'

# After the final runtime-fixture adjustments:
.\gradlew.bat --no-daemon :app:ktlintAndroidTestSourceSetCheck `
  :app:assembleDebugAndroidTest '-Pknittools.isolatedTest=activepolicy'
```

Results: **51 JVM tests passed** (28 TrialManager, 7 TrialStartSource, 9 ProFeatureGateSource, 7 AppStartupSource); debug and instrumentation APK builds passed. Full app ktlint passed, followed by another passing Android-test ktlint check on the final test source.

The APKs were installed with `adb -s emulator-5584 install -r`. The isolated target package was cleared before each suite. These two selected instrumentation runs passed:

```powershell
adb -s emulator-5584 shell am instrument -w -r `
  -e class com.finnvek.knittools.pro.TrialNoticeRestorationTest `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner

adb -s emulator-5584 shell am instrument -w -r `
  -e class com.finnvek.knittools.ui.AuditPart15LaunchTest `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
```

Results: **10 new runtime tests passed**, plus **1 existing external-launch regression test passed**. The final ten-test run took 23.331 seconds with no skipped tests.

Local evidence is under `%TEMP%\knittools-trial-restoration-20261020`: `validation-rerun.log`, `runtime-input-check.log`, `runtime-result.log`, `existing-launch-runtime.log`, copied `TEST-*.xml`, `tested-input-hashes.json`, and the before/after preservation records. Earlier attempts exposed test-fixture compilation/style issues, an incorrect JUnit setup return type, a Compose-test-clock startup stall, and incorrect screen-label/transient-prompt expectations. They were corrected without changing production behavior to satisfy the tests; failed/interrupted attempt logs remain in the same directory.

- `git diff --check` passed. SHA-256 comparison against the initial file inventory found no unrelated file changes; the index stayed empty and HEAD/branch stayed unchanged. Only `MainActivity.kt`, this plan, and the new runtime test belong to this task.
- The task's temporary emulator, its remaining console helper and the ADB server started by this task were stopped. No task-owned background process remains; cleanup evidence is in `cleanup.json`.

**Original remaining boundary on 2026-10-20:** T08 was still open, so the combined T01–T08 release checklist was not marked complete. The follow-up below supersedes that T08 status. Release builds, real purchases, other work packages, 320 dp, 200% fonts and TalkBack were not run in this task. No commit, push or pull request was made.

### T08 actual process-restoration verification — 2026-10-22

**PASS within the saved-state/synthetic-entitlement emulator boundary.** This closes only the outstanding T08 verification and supersedes earlier dated statements that T08 remains open. No production defect was demonstrated and no production source, billing behavior, navigation, saved Activity key, atomic claim, startup path, dependency or profile was changed.

#### Test-only fixture and setup

- Existing feature branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`; existing dirty work preserved.
- New disposable AVD `KnitToolsT08_20261022`, `medium_phone` device definition, `system-images;android-36;google_apis_playstore;x86_64`, serial `emulator-5584`, API 36 / `sdk_gphone64_x86_64`, 1080 × 2400 at 420 dpi, font scale 1.0. No physical device was used.
- Isolated debug target `com.finnvek.knittools.activepolicytest`, instrumentation package `.activepolicytest.test`, built with `-Pknittools.isolatedTest=activepolicy`. The installed target's requested permissions were checked to exclude INTERNET and BILLING.
- `TrialNoticeProcessSeedTest.prepareExpiredTrial` runs only with `-e trialProcessSeed true`. It uses the existing production singleton DataStore accessor and real `TrialManager` to prepare a synthetic trial with 15 days of elapsed duration, valid current boot/elapsed/wall-clock anchors and `clockTampered=false`. It asserts expired, started, zero remaining days and an unconsumed claim. It never overrides `TrialState`.
- `TrialNoticeProcessTestProvider` is compiled only in debug and registered only in `src/isolatedTest/AndroidManifest.xml`, with `exported=false`. Only an active-policy package with the private `files/trial-notice-process-test` fixture marker sets the existing synthetic purchased input to false on standalone startup. This supplies test billing readiness after process replacement. The existing `ProPolicyTestControl` implementation/defaults are unchanged; trial initialization, expiry evaluation, persistence and claim still use real production code. Normal/release app manifests do not register this provider. This is not real Play entitlement acceptance.
- `tools/trial-notice-process-test.py` runs outside the target process. Instrumentation finishes after seeding, before the standalone launcher start. There is no target instrumentation, reseeding, claim reset, force-stop, fresh launcher start or constructed saved-state Bundle between either background kill and its Recents return. Read-only `run-as` file reads decode AndroidX's [Preferences protobuf schema](https://github.com/androidx/androidx/blob/androidx-main/datastore/datastore-preferences-proto/src/main/proto/preferences.proto); Android CLI layouts assert rendered UI and supply the Recents/action tap coordinates.

Exact setup/build/test commands (SDK tools were resolved under `C:\Users\EmmaH\AppData\Local\Android\Sdk`):

```powershell
$sdk = 'C:\Users\EmmaH\AppData\Local\Android\Sdk'
$evidence = 'C:\Dev\KnitTools\reports\trial-process-20261022'
'no' | & "$sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd `
  -n KnitToolsT08_20261022 -k 'system-images;android-36;google_apis_playstore;x86_64' -d medium_phone
Start-Process -FilePath "$sdk\emulator\emulator.exe" -WindowStyle Hidden -PassThru `
  -ArgumentList '-avd','KnitToolsT08_20261022','-port','5584','-no-window',`
    '-no-audio','-no-snapshot','-no-boot-anim','-gpu','software' `
  -RedirectStandardOutput "$evidence\emulator.stdout.log" `
  -RedirectStandardError "$evidence\emulator.stderr.log"

.\gradlew.bat --no-daemon --max-workers=2 :app:assembleDebug :app:assembleDebugAndroidTest `
  :app:ktlintDebugSourceSetCheck :app:ktlintAndroidTestSourceSetCheck :app:testDebugUnitTest `
  --tests 'com.finnvek.knittools.pro.TrialManagerTest' `
  --tests 'com.finnvek.knittools.pro.TrialStartSourceTest' `
  --tests 'com.finnvek.knittools.ProFeatureGateSourceTest' `
  --tests 'com.finnvek.knittools.AppStartupSourceTest' '-Pknittools.isolatedTest=activepolicy'

adb -s emulator-5584 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5584 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
python tools/trial-notice-process-test.py --serial emulator-5584 --avd KnitToolsT08_20261022 `
  --output reports/trial-process-20261022/process-run-2

# After the complete T08 run: release the separate Android CLI UI helper before instrumentation.
# This does not force-stop the target app or participate in either restoration cycle.
adb -s emulator-5584 shell am force-stop com.android.cli.interact.instrumentation
adb -s emulator-5584 shell pm clear com.finnvek.knittools.activepolicytest
adb -s emulator-5584 shell am instrument -w -r `
  -e class 'com.finnvek.knittools.pro.TrialNoticeRestorationTest,com.finnvek.knittools.ui.AuditPart15LaunchTest' `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
git diff --check
```

Here `$sdk` denotes the SDK directory above and `$evidence` is `C:\Dev\KnitTools\reports\trial-process-20261022`. The host helper records its exact ADB commands in `process-run-2/commands.txt`, including the single pre-launch `pm clear`, the seed invocation, Home, [targeted background `am kill`](https://developer.android.com/tools/adb#am), PID checks and Recents taps. No data reset occurs inside either restoration cycle.

#### Observed process, task, UI and persistence results

| Stage | Saved-task/process evidence | UI and persistence assertions |
| --- | --- | --- |
| Initial standalone launch | MainActivity in task **9**, PID **7642** | Actual `Your Pro trial has ended` dialog rendered. `trial_end_notice_shown` changes from absent/false in the seed to true before backgrounding. |
| Background with pending notice | `state=STOPPED`, `mHaveState=true`, `mIcicle=Bundle[mParcelledData.dataSize=8240]` | Claim remains true. These bytes describe the complete Activity payload, not only the trial flag. |
| First targeted kill | `am kill com.finnvek.knittools.activepolicytest`; `pidof` empty and PID 7642 absent from `ps`; task **9** retains `app=null`, saved state and Bundle | Every stored trial preference is identical immediately before and after the kill. |
| Recents return | Tap the actual KnitTools snapshot card; task **9** resumes with new PID **7772** | The pending dialog renders after initialization. The claim remains true, trial start is unchanged and real trial initialization advances its persisted elapsed-time anchor. No new claim can be obtained from the consumed stored flag. |
| Continue Free, then Home | Existing `Continue free` action renders `No active projects`; task **9** stops with `mHaveState=true`, Bundle **8068 bytes** | Dialog is absent and the persisted claim remains true. |
| Second kill and Recents return | PID **7772** ends; task **9** retains saved state with `app=null`; Recents resumes it with PID **7926** | Projects render, the real trial anchor advances again, and the notice remains absent, including a delayed final check. Claim is still true and the trial has not been reseeded. |

Both replacements were **deliberately terminated background app processes**, not naturally occurring memory-pressure reclamation. The test exercised actual MainActivity saved-instance restoration and consumption in two new processes. `ActivityScenario.recreate()` is used only by the separate existing regression suite, not as T08 evidence.

#### Validation, artifacts and limits

- **Actual process test: PASS**, with `process-run-2/result.json` and the saved/restored/after-kill Activity dumps, UI layouts, original `.preferences_pb` snapshots, decoded preference assertions and absent-PID records in the same directory.
- **11/11 existing instrumentation regressions passed**, zero failures/skips, **46.143 seconds** (`trial-regressions-verified.log`): all ten `TrialNoticeRestorationTest` tests plus `AuditPart15LaunchTest`. This retains coverage of See Pro, initialization gating, obsolete-state suppression, external-launch precedence and atomic concurrent claims.
- **51/51 focused JVM tests passed**, zero failures/errors/skips: TrialManager 28, TrialStartSource 7, ProFeatureGateSource 9, AppStartupSource 7. Both APK builds and debug/Android-test ktlint checks passed (`build-checks-verified.log`, `jvm-results.json`, copied `TEST-*.xml`).
- App APK SHA-256: `2ACCD3761FD015168F6982819C8D326616E40E685D19EED2EF29C60DCCC9F2E3`; test APK SHA-256: `A1CC5E40FD804BF85DC1416D1EE49D40A84C7FC5618D0ADD10D1C915F2F301DF`.
- Earlier attempt logs are retained: test-source formatting errors were fixed; process-run-1 stopped before any kill because Android prints `Hist  #0` with variable whitespace, corrected in the host parser; the first regression invocation collided with the task's still-connected Android CLI UiAutomation helper. Stopping that helper allowed the unchanged regression tests to pass. None required a production correction or weakened assertion.
- `git diff --check` and Python syntax validation passed. The initial tracked/untracked input hash comparison found changes only in this plan and the isolated-test manifest; the other task additions are the provider, seed fixture and host helper. MainActivity, TrialManager, ProManager, ProPolicyTestControl and all existing tests remain byte-for-byte unchanged from this task's starting inputs. Branch/HEAD and the empty Git index are unchanged (`existing-input-changes.json`, `unchanged-owning-code.json`).
- The task's Android CLI UI helper and disposable emulator processes were stopped; the disposable AVD was deleted. The pre-existing ADB server was preserved. No task-owned temporary process remains (`cleanup.json`).
- Production startup paths and profile inputs were not changed. Existing startup/profile results are neither rerun nor promoted to a new artifact claim by this test-only change. Production signing/package verification, physical performance, live W11/R10, remaining release R11 entitlement checks, the separate INC-SIGN-01 owner action (4.1) and unrelated WebView/security findings remain **OPEN/BLOCKED**. This synthetic debug result does not close the expired-trial/release-billing portion of R11.
- The excluded 320 dp, 200% font scale, combined configuration, TalkBack, benchmark, profile and adaptive campaigns were not repeated. No secrets or personal data were logged. No commit, push, PR, publication, deployment or other work package was performed.

**Precise remaining guarantee:** a pending notice restores when Android has saved the Activity state and retains its task, provided entitlement initialization resolves to an eligible expired trial. Consuming it through Continue Free persists in the next saved Activity state. There is still no exactly-once visible-delivery promise across unsaved termination, arbitrary crashes, force-stops, task clearing or a crash between the persisted claim and UI-state saving. No durable pending/acknowledged notification protocol was added.

## 6. Work package 1B: Ravelry browser restoration

### 6.1 Selected approach

Preserve a bounded browser-history snapshot and a validated current-page URL fallback. Prefer AndroidX's bounded serializer over manually modifying an opaque WebView state Bundle.

`WebViewCompat.saveState(webView, bundle, maxSizeBytes, includeForwardState)` supports bounded serialization and can omit forward history. It requires a runtime `WebViewFeature.SAVE_STATE` support check. The API was introduced in WebKit 1.14.0; the official release page listed 1.17.1 as stable on the planning date. Verify the latest compatible stable version when implementing. [API reference](https://developer.android.com/reference/androidx/webkit/WebViewCompat#saveState(android.webkit.WebView,android.os.Bundle,int,boolean)), [WebKit releases](https://developer.android.com/jetpack/androidx/releases/webkit)

Add a direct WebKit dependency through the version catalog if needed. Check compatibility and dependency-verification metadata. Do not upgrade Compose, Kotlin, AGP, or unrelated libraries for this feature.

### 6.2 State ownership and timing

1. Scope browser state to the current navigation entry/request identity.
2. Preserve state across Activity recreation of that entry.
3. A newly shared URL must take precedence over an older browser snapshot. This includes opening a new request for the same textual URL.
4. Retain the current WebView only for its actual UI lifecycle. Do not store an Activity-backed WebView in a ViewModel or application singleton.
5. Register a saveable-state provider/Saver at the screen lifecycle boundary that captures the current WebView when Android saves state.
6. Do not rely exclusively on `onRelease`, `onDispose`, or `ON_STOP`: those may run after the required state-save point.
7. Perform WebView save/restore calls on the main thread.
8. Remove provider registrations when the owning entry is disposed.
9. Preserve current cookie flushing and `destroy()` cleanup.
10. Store only the browser snapshot and small restoration metadata. Do not include downloaded PDFs, image bytes, cookies, credentials, or WebView objects in saved state.

### 6.3 Size policy and fallback

1. Start with a named browser-specific saved-state budget of **32 KiB**, measured after serialization. This is a conservative implementation budget, not an Android guarantee or a platform requirement.
2. Include restoration metadata and the fallback URL in the budget assessment.
3. Leave capacity for Navigation, form drafts, and every other Activity saved-state contributor. Never treat the Binder limit as a private WebView allowance.
4. When `SAVE_STATE` is supported, serialize using the bounded API.
5. Set `includeForwardState = false` while the product has no Forward action.
6. When unsupported, use framework `WebView.saveState()` and measure the serialized result with a properly recycled `Parcel`.
7. Keep the framework result only if it fits the same budget.
8. If the state cannot be saved, is oversized, or cannot be restored, load the validated fallback URL.
9. If the fallback URL is invalid, disallowed, or itself too large for the small metadata allowance, use the existing Ravelry search start page.
10. Do not clear the live WebView's history merely because its saved snapshot is too large.
11. Do not manually decode, trim, or rewrite WebView's internal state entries.
12. Revisit the budget only if measured representative sessions demonstrate a problem; do not silently raise it toward the process transaction limit.

Bounded native serialization retains recent history instead of discarding everything when the budget is exceeded. The unsupported-device URL fallback remains intentionally simpler. [WebView state management](https://developer.android.com/develop/ui/views/layout/webapps/manage-state)

### 6.4 Restoration sequence

1. Construct a fresh WebView.
2. Apply the existing security settings, clients, download listener, and cookie behavior.
3. Select only a snapshot belonging to the current request identity.
4. Validate the app-owned restoration metadata and restorable destinations before initiating a restore.
5. Attempt `restoreState` before any initial `loadUrl`.
6. If restoration succeeds, initialize `canGoBack` and the relevant visible UI state from the restored WebView.
7. Do not subsequently load the route's start URL over a successful restoration.
8. If restoration fails or is rejected, load the safe fallback URL once.

Do not promise complete scroll-position, form-input, login-form, renderer-cache, or in-flight network-request restoration. Those are not application-level guarantees of this implementation.

### 6.5 URL and checkout rules

`shouldOverrideUrlLoading` is not a universal hook for every navigation; the API documentation explicitly excludes some paths such as application-initiated `loadUrl` and POST requests. Do not rely on it alone for restored or application-directed navigation. [WebViewClient reference](https://developer.android.com/reference/android/webkit/WebViewClient)

Implementation requirements:

- Reuse `ravelryPageUrlOrNull`, `isRavelryPurchasePage`, and the existing navigation policy where their semantics apply.
- Distinguish a user-supplied start URL from a browser redirect. Do not accidentally forbid legitimate PDF redirect/download behavior by applying the start-URL rule to every network request.
- Preserve a last eligible Ravelry page as the fallback, rather than a transient external download or payment destination.
- Treat payment/checkout pages and unsupported schemes as ineligible restoration destinations.
- Inspect candidate history using public history APIs when saving. Keep only snapshots that satisfy the chosen restoration policy; if history cannot be safely admitted, fall back to a validated page.
- Validate saved eligibility metadata again under the current policy before restore. Do not accept state Bundles from intents, shared content, or external files.
- Before application-driven Back traversal, check the target entry against the applicable main-page/payment rules. Route an explicit payment action externally; do not reopen checkout automatically during background restoration.
- Handle missing or stale history entries without throwing or creating a Back loop.
- Do not log history, full URLs, cookie values, or credentials.

The checkout concern is a regression-prevention requirement, not evidence that a currently exploitable restoration vulnerability has been demonstrated.

### 6.6 Tests

| ID | Scenario | Expected result |
| --- | --- | --- |
| W01 | Browse A → B → C and recreate | C restores; Back reaches B and then A. |
| W02 | Background, allow saved state, reclaim process, return through task | Bounded navigation context restores. |
| W03 | Change app/system theme while browsing | Browser does not reset to search. |
| W04 | Resize/rotate/fold while browsing | Page and eligible Back history remain usable. |
| W05 | Native bounded-save feature unsupported | Framework-size-check or URL fallback succeeds. |
| W06 | Oversized snapshot | Saved payload stays bounded; live history is not cleared. |
| W07 | Missing/failed restore result | Safe fallback loads once. |
| W08 | Open a new shared link with an existing browser | New request replaces the old browsing context. |
| W09 | Invalid, non-HTTPS, checkout, or external fallback | Disallowed page is not restored in-app. |
| W10 | Back target is a payment/blocked destination | Policy is enforced; no navigation loop. |
| W11 | Download a valid PDF after restoration | Existing Save Pattern flow still receives it. |
| W12 | Leave browser permanently | WebView is destroyed and Activity is not retained. |

Extend `RavelryLinksTest` and `RavelryBrowserNavigationTest`. The existing navigation runtime test uses the network-disabled `.activepolicytest` package: preserve that isolation for deterministic request-identity tests.

Use synthetic HTTPS page responses or an equivalent controlled harness for history tests, without relaxing production URL rules. Keep live Ravelry login and PDF-download checks separate. Do not make CI depend on a real account or the availability of the website.

Activity recreation and actual process reclamation are separate checks. `ActivityScenario.recreate()` proves the former only.

### 6.7 Implementation record — 2026-10-20

**Historical status: implemented; acceptance verification incomplete at this record's completion.** This record describes the section 6 implementation. W02 passed the subsequent host-driven process-restoration check below. W04 was initially verified for phone rotation only; section 8.14 adds the remaining locally available resize/freeform/fold/posture configurations. W11 was still blocked here; section 6.10 subsequently completes its live journey within an ordinary-debug/physical-device evidence boundary.

Implemented:

- Added direct `androidx.webkit:webkit:1.17.1` through the version catalog and SHA-256 verification entries for its AAR, POM and module metadata obtained from Google Maven. The pre-change `debugRuntimeClasspath` had no WebKit artifact. The resolved post-change dependency is 1.17.1; no unrelated dependency or toolchain version changed.
- Rechecked the [official stable release listing](https://developer.android.com/jetpack/androidx/releases/webkit) and the [bounded save API](https://developer.android.com/reference/androidx/webkit/WebViewCompat#saveState(android.webkit.WebView,android.os.Bundle,int,boolean)). The published AAR requires minSdk 24, compileSdk 33 and AGP 7.2.0; this project uses 29, 37 and 9.4.1 respectively. Gradle's app and Android-test AAR compatibility checks passed.
- `RavelryBrowserState` belongs to the current screen/request. Its `rememberSaveable` Saver captures the current WebView during Android saved-state collection on the main thread. Release also retains a bounded snapshot for navigation disposal ordering, then clears the view reference; the existing cookie flush and `destroy()` remain in `AndroidView.onRelease`. No WebView is retained in a ViewModel or singleton.
- The total serialized browser Bundle is limited to **32 KiB**, including request identity, fallback and eligibility metadata. Eligibility metadata is limited to 8 KiB; the fallback URL is limited to 2,048 Kotlin characters. These are application budgets, not platform guarantees. Long eligible initial/Back URLs remain usable; the small fallback allowance does not limit normal navigation.
- Saving checks the public history entries' current and original URLs. Native `SAVE_STATE` is checked at runtime and uses `includeForwardState = false`; unsupported providers use framework `saveState` followed by a recycled-Parcel size measurement. Oversized/failed/ineligible snapshots fall back without clearing live history. No opaque WebView entries are decoded, trimmed or rewritten.
- Restoration validates the request, fallback and saved eligibility metadata, then attempts `restoreState` before any initial `loadUrl`. A successful restore initializes Back/progress state; a failed or rejected restore loads one safe fallback. The last eligible Ravelry page survives transient external destinations. Background restoration never opens checkout externally.
- Shared navigation now carries the existing coordinator's persisted request ID. A fresh request, including the same URL again, gets a fresh screen/context; recreation of the same request retains its snapshot. No coordinator, MainActivity or trial-notice behavior was changed.
- App-driven Back checks public history and skips ineligible destinations, leaving the browser when no eligible target remains. Explicit checkout links still use the existing external-browser policy. Existing redirect handling, download listener, per-URL cookie callback, HTTPS downloader and Save Pattern wiring remain unchanged.
- Extended `RavelryLinksTest` and `RavelryBrowserNavigationTest`; updated only the affected source contracts. The Web Pattern architecture contract has exact exceptions for the Ravelry state helper's imports and the WebKit catalog entry, retaining its restrictions for other code.

#### W01–W12 evidence

The automated runtime tests use real WebViews and the real MainActivity/navigation in the network-disabled isolated package. History fixtures serve controlled HTTPS responses through a test-only client that delegates navigation/history callbacks to the production client. The one `clearHistory()` in the test helper establishes the initial synthetic fixture; production save/restore never clears history.

| Case | Status | Evidence and boundary |
| --- | --- | --- |
| W01 | Verified — emulator instrumentation | Three eligible pages A → B → C, a different Activity/WebView after recreation, then system Back reaches B and A. This is Activity recreation evidence only. |
| W02 | Verified — host-driven emulator process termination | Opened Tools → Ravelry through the UI and loaded controlled HTTPS pages A → B → C through local WebView DevTools interception. After Home, Android reported `STOPPED`, `mHaveState=true`, and a saved Activity Bundle. Targeted `am kill` removed app PID 2929; `pidof` returned no process while task 134 and its saved state remained. Selecting the same card in Recents created app PID 3926 in task 134. Read-only history assertions confirmed C with A/B history, then Android Back reached B and A. This is actual app-process termination/restoration on one emulator, not Activity recreation or a naturally occurring low-memory event. The network-disabled package could not reload live page content. See the follow-up record below. |
| W03 | Verified — emulator instrumentation | AppCompat night-mode change creates a new WebView and retains C and eligible history. Live Ravelry/login continuity was not tested. |
| W04 | Verified for locally available configurations — see 8.14 | Initial evidence covered phone rotation only: C and Back to B remained usable. The earlier inventory had no foldable AVD. Section 8.14 subsequently verifies the complete normal window matrix on API 37/36, actual freeform on API 37, and CLOSED/OPENED/HALF_OPENED plus rotated tabletop on the task-created landscape-natural fold emulator. Controlled page/history/Back continuity passes; physical/OEM and signed-in live Ravelry behavior are not inferred. |
| W05 | Verified — forced unsupported-feature branch | A real WebView's framework snapshot stays within budget and restores C/B/A when the bounded-save branch is disabled in the test. This does not claim execution on a physically unsupported WebView provider. The separate native test passed on the available provider. |
| W06 | Verified — emulator instrumentation | A real history-state fixture serializes above 32 KiB. Framework save returns bounded URL-only metadata; the default/native path also remains within budget. Live history size/index are unchanged. A separate native test preserves Back history while omitting Forward state from the saved copy and retaining live Forward history. |
| W07 | Verified — fault-injection instrumentation | Missing snapshot and a controlled null `restoreState` result each load the safe fallback once. A stale request loads the new initial URL without attempting the old restore. The restore-failure injection uses a small WebView subclass; it is not a service failure test. |
| W08 | Verified — emulator instrumentation | Real Android share launches replace the view for different URLs and for the same URL again. The recreation test additionally delivers the same URL through the real `onNewIntent` callback, then verifies that the fresh context survives recreation without the old history. |
| W09 | Verified — JVM policy + instrumentation | Invalid/non-HTTPS, checkout, payment/external, credential-bearing and oversized fallback values are rejected. Ineligible saved eligibility metadata does not call restore; fallback uses search. Transient external navigation retains the last eligible page. |
| W10 | Verified — emulator instrumentation | A controlled checkout entry between A and C makes the snapshot ineligible; Back skips it to A, then has no eligible target, without looping. Existing explicit purchase/external navigation classification also passes JVM tests. No purchase was made. |
| W11 | Verified on the ordinary debug APK and physical Pixel 9 — section 6.10 | Following the postponed emulator preparation in 6.9, the user authorizes the available phone and manually signs in. Real Activity recreation and retained-task restoration after background process termination preserve a two-page history, Back and signed-in access. The real free PDF download reaches Save pattern, saves to Library, renders/reopens, and a second pending download cancels cleanly without affecting the saved file. This does not close optimized/signed R10 acceptance or promise complete history retention; the five-entry URL-only fallback is recorded in 6.10. |
| W12 | Verified — emulator instrumentation + source inspection | After permanent Activity closure, weak references to both the WebView and Activity become null. The production release callback still flushes cookies and calls `destroy()`, and the holder clears its WebView reference. No cross-device leak guarantee is inferred. |

#### Executed inputs and checks

- Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`; dirty working-tree inputs, not a committed release candidate.
- Gradle 9.8.1, Eclipse Temurin JDK 17; debug variant with `-Pknittools.isolatedTest=activepolicy`.
- Device: `medium_phone`, `emulator-5584`, `sdk_gphone64_x86_64`, API 36; physical display 1080 × 2400, density 420, font scale 1.0. The rotation test changes Activity orientation, not font scale or a small-width configuration.
- WebView: `com.google.android.webview` 155.0.8059.43. The native bounded-save test passed, including its runtime `SAVE_STATE` support assumption; no tests were skipped in the final eleven-test run.
- Application ID: `com.finnvek.knittools.activepolicytest`; runner package: `com.finnvek.knittools.activepolicytest.test`.
- App APK SHA-256: `7D30E3463624187D99B8BCC7382784112863BE2208480605570FBDFA6424DA6C`.
- Final test APK SHA-256: `E133E6FEB3C41932E62600C1993EF0124154935A7C93348CCCB866D85FDDCBCD`.

The final production/test build and focused JVM run passed:

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest `
  --tests 'com.finnvek.knittools.ui.screens.ravelry.RavelryLinksTest' `
  --tests 'com.finnvek.knittools.RavelryBrowserSourceTest' `
  --tests 'com.finnvek.knittools.RavelryShareTargetSourceTest' `
  --tests 'com.finnvek.knittools.RavelrySecuritySourceTest' `
  --tests 'com.finnvek.knittools.WebPatternLocalizationArchitectureContractTest' `
  --tests 'com.finnvek.knittools.ui.navigation.NavigationArgumentSafetyTest' `
  --tests 'com.finnvek.knittools.ui.navigation.IncomingPdfViewModelTest' `
  --tests 'com.finnvek.knittools.data.remote.WebPdfDownloaderTest' `
  :app:assembleDebug :app:assembleDebugAndroidTest `
  :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck `
  :app:ktlintAndroidTestSourceSetCheck '-Pknittools.isolatedTest=activepolicy'

# After the final test-only lifecycle-fixture correction:
.\gradlew.bat --no-daemon :app:assembleDebugAndroidTest `
  :app:ktlintAndroidTestSourceSetCheck '-Pknittools.isolatedTest=activepolicy'

adb -s emulator-5584 shell am instrument -w -r `
  -e class com.finnvek.knittools.ui.screens.ravelry.RavelryBrowserNavigationTest `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
```

Results: **47 JVM tests passed**, both APK builds passed, all three selected source-set ktlint checks passed, and **11 browser instrumentation tests passed in 36.134 seconds**, with no skipped tests. APKs were installed with `adb -s emulator-5584 install -r`; only the isolated target package was cleared.

The previous `com.finnvek.knittools.pro.TrialNoticeRestorationTest` was also rerun through the same explicit `am instrument -w -r -e class` command after clearing only the isolated package: **all 10 tests passed in 33.566 seconds** (`trial-regression.log`). This is a regression check of section 5; its separate T08 remains open.

`.\gradlew.bat --no-daemon :app:lintDebug '-Pknittools.isolatedTest=activepolicy'` also passed (`lint-final.log`): **zero errors and four reports of the same `MissingOnRenderProcessGone` warning** on the existing production WebViewClient. WebKit's lint now surfaces the pre-existing absence of renderer-process crash handling. It was neither suppressed nor expanded into a renderer-recovery feature in this task. This warning concerns the WebView renderer and does not prove or disprove W02's app-process restoration.

Preservation checks: `git diff --check` passed; the index remained empty and branch/HEAD were unchanged. All previously dirty/untracked input-file hashes stayed unchanged except this plan and the scoped additions to `WebPatternLocalizationArchitectureContractTest`. MainActivity SHA-256 remains `9B108B3C673868836837745B6C000C2FBDC84F970DC5C549DB63DA7AEEC6127F`, and the existing trial runtime test remains `6712A0A831F3F57CD435FE6D7C0E0C530D5AA440A8A2BE4ACDE8F9BE33E50789`. The final tested-input hash comparison had zero mismatches.

Local evidence is under `%TEMP%\knittools-ravelry-restoration-20261020`: `validated-inputs.log`, `share-fixture-rebuild.log`, `runtime-verified.log`, `lint-dependency.log`, copied `TEST-*.xml`, `tested-input-hashes.json`, and preservation records. Earlier failed logs remain: style corrections, the request-ID type correction, ActivityScenario's mismatch after consumption of ACTION_SEND, and the rejected data-URL history fixture were resolved without weakening production URL rules. The unsuccessful process-restoration harness was removed from the final test class rather than presented as a passing process-death check.

No camera-permission, adaptive-layout, Room, targetSdk, billing, backend, resource or other work package was implemented. The previous trial-notice implementation is retained. The excluded 320 dp, 200% font-scale and TalkBack checks were not run. No commit, push, pull request or publication was performed.

The temporary emulator, its remaining owned launcher process and the ADB server started for this task were stopped; no task-owned device/helper remains running (`cleanup.json`). Gradle used single-use daemons and completed normally.

#### W02 follow-up — 2026-10-20

At the user's request to try another test method, W02 was verified without an in-app instrumentation runner. The earlier shared-link adb command had been rejected by automatic approval review with `blocked by policy` and no detailed reason; that command was not retried. The alternative used a normal MAIN/LAUNCHER start and UI taps on Tools → Ravelry.

The installed APK has the same SHA-256 recorded above, and all previously recorded tested-input hashes still match. The device was again `medium_phone` / `emulator-5584`, API 36, WebView 155.0.8059.43, 1080 × 2400, density 420 and font scale 1.0. The isolated `.activepolicytest` package remained without INTERNET/BILLING.

The host-side Node helper used a local ADB forward to the debug WebView and DevTools `Fetch.fulfillRequest` to serve synthetic HTTPS pages A, B and C. It asserted their URLs, titles and completed loads, then disconnected before backgrounding. No application state was injected or rewritten during restoration.

1. Home backgrounded the application. `dumpsys activity activities` confirmed `state=STOPPED`, `mHaveState=true`, and `mIcicle=Bundle[mParcelledData.dataSize=17664]` for task 134. This is the whole saved Activity payload observed in this fixture, not a measurement of only the browser Bundle.
2. `adb -s emulator-5584 shell am kill com.finnvek.knittools.activepolicytest` terminated the background app process. The [documented targeted background-process command](https://developer.android.com/tools/adb#am) was used; no force-stop or task removal was used for the test.
3. `pidof` confirmed PID 2929 was absent. Android still held task 134, `app=null` and the saved Bundle.
4. The Recents UI card resumed task 134 with new PID 3926. A fresh debug connection only read `Page.getNavigationHistory`: the current URL was C and preceding entries were A and B. No navigation, reload, response interception or state injection was performed to obtain that restored result.
5. Two `KEYCODE_BACK` events invoked the application's Back behavior. Read-only assertions verified B and then A.

**Result: W02 passed for actual, deliberately triggered app-process death and saved-task restoration on this emulator.** This does not simulate naturally arising memory pressure or establish live Ravelry/login continuity. After restoration, network reloads displayed the expected unavailable-page content because INTERNET is absent; the restored URL/history and Back traversal are the verified behavior. W04's remaining configurations were open at this point and are subsequently covered within 8.14's local boundaries. W11 remains open, so section 6 acceptance is still incomplete.

Reproducible helper and raw evidence are in `%TEMP%\knittools-ravelry-w02-20261020`: `browser-history.mjs`, `history-before.json`, `activity-saved.txt`, `process-absent.json`, `activity-after-kill.txt`, `activity-restored.txt`, `history-restored.json`, `history-back-b.json`, `history-back-a.json`, and `result.json`. Assertions passed, with zero tested-input hash mismatches. No source, dependency or test-class change was needed; only this plan was updated. The local forwarding and device XML were removed, and the task-owned emulator and ADB processes were stopped (`cleanup.json`).

### 6.8 Renderer-process termination — 2026-10-22

This is a separate renderer-lifecycle correction, not a rerun or closure of T08/W02 app-process restoration or live W11/R10. Historical reports and artifact hashes above retain their original scope.

#### Cause, platform contract and correction

The existing production `WebViewClient` had no `onRenderProcessGone` override. Both recorded lint XML reports contained four instances of `MissingOnRenderProcessGone` for that same client. Normal `AndroidView.onRelease` called a parameterless `RavelryBrowserState.release()` that saved the current WebView, cleared its reference and then destroyed the released view. Reusing that path after renderer loss would attempt to save an unusable view; a delayed release could also clear a replacement's state.

The [platform callback contract](https://developer.android.com/reference/android/webkit/WebViewClient#onRenderProcessGone(android.webkit.WebView,%20android.webkit.RenderProcessGoneDetail)) and [termination guidance](https://developer.android.com/develop/ui/views/layout/webapps/handle-termination) require removing/destroying each affected instance and clearing its references before reporting the exit handled. The callback applies to both crashes and non-crash exits and must not assume that unrelated views are affected. The callback is available from API 26 and the test-only [renderer terminate API](https://developer.android.com/reference/android/webkit/WebViewRenderProcess#terminate()) from API 29. The project retains minSdk **29**, compile/targetSdk **37**, AGP **9.4.1**, Gradle **9.8.1** and Temurin daemon JDK **17**. Official guidance was checked with the API references and `android docs search` / `android docs fetch kb://android/develop/ui/views/layout/webapps/handle-termination`.

**Necessary WebKit correction:** implementing the callback removed two findings, but WebKit **1.17.1** still incorrectly reported two Kotlin superclass-constructor calls in both debug/release Lint reports. Its bundled detector unconditionally reports those constructor calls. AndroidX documents this exact [Kotlin detector defect and fix](https://android.googlesource.com/platform/frameworks/support/+/8003b908c7bfaecd6636512ad5cb1720a142ad51%5E!/). WebKit alone is updated to the current stable [**1.18.0** (2026-10-21)](https://developer.android.com/jetpack/androidx/releases/webkit#1.18.0), whose downloaded detector includes the Kotlin superclass/delegating-constructor exclusions. Its AAR requires compileSdk >= 33 and AGP >= 7.2.0, within this project's toolchain. The AAR, module and POM SHA-256 values were verified from Google Maven and added to `gradle/verification-metadata.xml` without rewriting existing entries. No finding suppression, baseline entry or unrelated dependency/toolchain upgrade is added.

- `RavelryBrowserState` owns the current instance, progress, Back availability and a saveable renderer-loss flag. Renderer loss drops the instance reference, clears progress/Back, detaches and destroys only that instance, without reading its URL/history or calling either save-state API. Normal release still saves bounded state and flushes cookies, but release now accepts the specific view and ignores retired instances.
- Navigation, history, progress and download callbacks check instance identity. Browser client configuration is shared by the production factory and the focused callback tests. A retired callback/release cannot change a replacement, dispatch a PDF download or open an external browser.
- The failure screen reuses localized `generic_error_unknown` and `retry`, with the existing toolbar/system Back path. It never automatically recreates or reloads a renderer. The failure flag also survives Activity state saving/recreation. Explicit Retry creates a fresh configured view and uses existing request/HTTPS/checkout/restoration policies to load the last eligible URL captured before the exit, or the existing search fallback.
- Renderer recovery intentionally retains **URL-only** context. It does not recover uncaptured history, scroll position, unsaved forms or in-flight requests and does not replay download/external-action callbacks. Cookies are not cleared and no sign-out is performed. Normal bounded history restoration remains in place: total saved browser state **32 KiB**, history metadata **8 KiB**, fallback URL **2,048 characters**, native bounded save when supported and measured framework fallback otherwise.
- Deliberate `chrome://crash` and `WebViewRenderProcess.terminate()` calls exist only in `androidTest`. Production URL restrictions and PDF ownership are unchanged.

#### Verification record

Evidence directory: `reports/renderer-loss-20261022/` (ignored). The records below first describe the implementation with WebKit 1.17.1; final 1.18.0 evidence is recorded separately. Disposable AVD: `KnitToolsRenderer_20261022`, serial `emulator-5584`, `sdk_gphone64_x86_64`, Android **16 / API 36**, build `google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.D1/13818094:user/release-keys`, WebView `com.google.android.webview` **133.0.6943.137**. The target is the network/Billing-disabled `com.finnvek.knittools.activepolicytest` package. Controlled HTTPS fixtures are supplied only by test-side request interception; no live login is used.

Executed host checks:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 -I reports/renderer-loss-20261022/format-scoped.gradle `
  :app:ktlintMainSourceSetFormat :app:ktlintAndroidTestSourceSetFormat `
  :app:assembleDebug :app:assembleDebugAndroidTest :app:ktlintCheck :app:detekt `
  :app:testDebugUnitTest --tests '*Ravelry*Test' --tests '*WebPdfDownloaderTest' `
  --tests '*IncomingPdfViewModelTest' --tests '*ExternalWebLinkOpenerTest' `
  --tests '*WebPatternLocalizationArchitectureContractTest' '-Pknittools.isolatedTest=activepolicy'
.\gradlew.bat --no-daemon --max-workers=1 -I reports/renderer-loss-20261022/format-scoped.gradle `
  :app:ktlintAndroidTestSourceSetFormat :app:assembleDebugAndroidTest :app:ktlintAndroidTestSourceSetCheck `
  :app:dependencyInsight --dependency androidx.webkit:webkit --configuration debugRuntimeClasspath `
  '-Pknittools.isolatedTest=activepolicy'
.\gradlew.bat --no-daemon --max-workers=1 :app:lintDebug :app:lintRelease '-Pknittools.isolatedTest=activepolicy'
git diff --check
```

The ignored formatter init script selects only the two changed production browser files and three browser instrumentation files. `build-checks-3.log` passed: both APK builds, all app KtLint checks, **0 Detekt findings**, and **48/48 JVM tests** (zero failures/errors/skips). `test-build-4.log` passed the test-only rebuild/format check and confirmed resolved `androidx.webkit:webkit:1.17.1`. JVM coverage comprises URL/navigation policy (9), browser/source/security/share/localization contracts (9), downloader (8), incoming PDF state (16), external opening (3), and Web Pattern architecture/localization (3). The initial `lint.log` completed both variants in **33m 34s**, with **0 errors / 8 warnings** each: the two confirmed constructor false positives plus six unrelated warnings (`RtlHardcoded` x3, `VisibleForTests` x2, `ModifierParameter` x1). Copies are retained as `lint-webkit-1.17.1-{debug,release}.xml`.

Device setup used the installed SDK's `avdmanager.bat create avd -n KnitToolsRenderer_20261022 -k 'system-images;android-36;google_apis_playstore;x86_64' -d medium_phone` with the `no` hardware-profile answer. The emulator was started with `Start-Process -WindowStyle Hidden -PassThru` and `-avd KnitToolsRenderer_20261022 -port 5584 -no-window -no-audio -no-snapshot -no-boot-anim -gpu software`, retaining its PID and stdout/stderr logs. Actual test commands:

```powershell
adb -s emulator-5584 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5584 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
$selection = (Get-Content reports/renderer-loss-20261022/instrumentation-selection.txt -Raw).Trim()
adb -s emulator-5584 shell am instrument -w -r -e class $selection `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
```

`instrumentation-selection.txt` records both new test classes plus the eleven original `RavelryBrowserNavigationTest` methods. Its two separately gated adaptive methods (`platformTransitionsPreserveControlledHistoryAndBack`, `adaptiveViewportKeepsControlledHistoryAcrossTheWindowMatrix`) are excluded. The final API-36 run (`instrumentation-2.log`, **106.763 s**) has **15 passed / 0 failed / 1 assumption skip**; the runner's `OK (16 tests)` includes that skip and must not be reported as sixteen passes.

| Evidence | Result and boundary |
| --- | --- |
| Actual renderer crashes | `RavelryRendererLossTest` invokes the documented `chrome://crash` hook twice. Both real provider callbacks report `didCrash=true`; Chromium reports renderer PIDs **7805** and **7877**, code **11**. App PID stays **7659**. |
| Actual non-crash exits | The same class calls `WebViewRenderProcess.terminate()` twice. Both return true and reach actual provider callbacks with `didCrash=false`; renderer PIDs **7931** and **7975**, code **-1**. App PID remains **7659**. This is deliberate termination, not an induced low-memory event. |
| Recovery and UI | Each exit detaches the old view and leaves a stable error/Retry screen without a WebView during a quiet interval. Both direct Retry and Retry after Activity recreation create a different view. Controlled navigation, Back and another Activity recreation work afterward. A second renderer exit still waits for user action. System Back and toolbar Back each leave the failed browser. Android `UiAutomation` drives the real localized actions. |
| Injected callbacks/state owner | Three `RavelryBrowserStateTest` tests pass. Both crash-detail values are injected separately from the real provider tests. A guarded WebView records/rejects post-loss URL/history/save/navigation access and counts exactly one destruction. Old progress/history/navigation/download/loss callbacks and release cannot modify/destroy a replacement or dispatch another download/external action. An unaffected owner/view remains intact. Normal release saves before destruction; failed-state saving stays bounded; retry validates fallback/request identity and never repeats checkout. |
| Existing browser regressions | Ten pass on WebView 133: Activity/history/theme/orientation restoration, request replacement, framework state, oversized state, rejected state, checkout/Back filtering, fallback and view/Activity collection. `nativeBoundedSaveOmitsForwardHistory` is skipped because this provider does not support `WebViewFeature.SAVE_STATE`; it is not a native-save pass. |

`instrumentation-2-logcat.txt` and `runtime-summary-api36.json` retain the real exit evidence. Other Chromium code -1 messages also occur during ordinary view teardown; they are not counted as additional deliberate failure tests. The controlled tests do not prove live login or PDF downloading after recovery.

Intermediate WebKit 1.17.1 isolated artifacts (`webkit-1.17.1-artifacts.json`): app APK SHA-256 **AC8F2460F37A453ABAB72999005194F962DBE64B961451A024A93616F236F3DC**, test APK SHA-256 **A7DFBC4BDF37EA57425D4070F22DD58C93F253377AD149395E46B1EF005F6C02**. These are debug test artifacts, not a signed production candidate, and do not identify the final WebKit 1.18.0 build.

Earlier attempts remain recorded. `build-checks-1.log` found a test-formatting issue; `build-checks-2.log` found an invalid test import; both were corrected. `instrumentation-1.log` passed the three state tests but stalled in the Compose/ActivityScenario test driver before a deliberate renderer exit. Only that isolated test process was force-stopped; the resulting runner `Process crashed` message is **not renderer-crash evidence**. The driver now uses platform UiAutomation without a Compose test rule. A diagnostic Java-stack request was unavailable on the non-root Play image. The injected `RenderProcessGoneDetail` fixture uses its deprecated public constructor; this test-only compiler warning is retained without suppression, alongside the existing unrelated test/Gradle deprecation diagnostics.

**Native-save follow-up:** the unsupported-feature skip was resolved on a second disposable image, `KnitToolsRendererNative_20261022`, created with `system-images;android-37.1;google_apis_playstore_ps16k;x86_64` and the same emulator flags/port after deleting the API-36 AVD. It reports Android **17 / API 37**, model `sdk_gphone16k_x86_64`, build `google/sdk_gphone16k_x86_64/emu64xa16k:17/CP31.260623.012/16064790:user/dev-keys`, and WebView **149.0.7827.5** (`device-native.json`). Only the skipped existing test was selected:

```powershell
adb -s emulator-5584 shell am instrument -w -r `
  -e class com.finnvek.knittools.ui.screens.ravelry.RavelryBrowserNavigationTest#nativeBoundedSaveOmitsForwardHistory `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
```

The first attempt timed out in the initial browser-launch wait, before installing the controlled page fixture (`native-save.log`, `native-save-attempt1-logcat.txt`). The unchanged test/APKs passed the retry: **1 passed / 0 failed / 0 skipped**, **28.526 s** (`native-save-2.log`). Native bounded save keeps eligible Back history, omits the saved Forward history, preserves live Forward history and remains within 32 KiB. Thus **all 16 distinct selected device tests have passing evidence across the two providers**; this is not sixteen passes on one provider, and the initial timeout/skip records are retained.

Both task-created emulators were stopped with `adb -s emulator-5584 emu kill` only after checking their exact AVD names, then removed with `avdmanager.bat delete avd -n <task-created-name>`. `cleanup.json` confirms zero remaining emulator processes for these task names. The pre-existing ADB server, physical device and unrelated emulator/processes were preserved. No temporary UI helper/server was started. Single-use Gradle processes exit naturally after their checks.

#### Final WebKit 1.18.0 verification

The final build/check invocation is recorded in `final-checks.log`:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug :app:assembleDebugAndroidTest `
  :app:ktlintCheck :app:detekt :app:testDebugUnitTest `
  --tests '*Ravelry*Test' --tests '*WebPdfDownloaderTest' --tests '*IncomingPdfViewModelTest' `
  --tests '*ExternalWebLinkOpenerTest' --tests '*WebPatternLocalizationArchitectureContractTest' `
  :app:dependencyInsight --dependency androidx.webkit:webkit --configuration debugRuntimeClasspath `
  :app:lintDebug :app:lintRelease '-Pknittools.isolatedTest=activepolicy'
```

Both APK build tasks and all app KtLint checks passed; Detekt has **0 findings**. The final JVM run has **48 passed / 0 failed / 0 errors / 0 skipped** across nine suites (`final-jvm-results.json`). Dependency insight resolves WebKit **1.18.0**. Final APK SHA-256 values (`final-artifacts.json`) are app **1FFA78D0D0BC501B8FC9C53DB3455778214714F5927F5B37768E3D99DB34D3AF** and test **A7DFBC4BDF37EA57425D4070F22DD58C93F253377AD149395E46B1EF005F6C02**; the test APK is unchanged because its packaged code did not change. `dependency-preservation.json` confirms the catalog's only edit is WebKit and every pre-existing verification-metadata entry is preserved byte for byte.

`final-checks.log` completed successfully in **34m 8s**. Both final Lint XML reports have **0 errors, 0 `MissingOnRenderProcessGone` findings and 6 unchanged unrelated warnings**: `RtlHardcoded` x3, `VisibleForTests` x2 and `ModifierParameter` x1. Comparing each remaining finding's ID, message and source file against the pre-task report produces zero differences (`final-{debug,release}-lint-summary.json`). The production callback and the corrected WebKit detector resolve the finding without suppression.

The final device run uses disposable `KnitToolsRendererFinal_20261022`, serial `emulator-5584`, Android **17 / API 37**, `sdk_gphone16k_x86_64`, fingerprint `google/sdk_gphone16k_x86_64/emu64xa16k:17/CP31.260623.012/16064790:user/dev-keys`, WebView **149.0.7827.5** before and after testing (`device-final.json`). Creation used `avdmanager.bat create avd -n KnitToolsRendererFinal_20261022 -k 'system-images;android-37.1;google_apis_playstore_ps16k;x86_64' -d medium_phone`; startup used the same hidden/no-window/no-snapshot/software-GPU flags above. The same two `adb install -r` commands and exact `instrumentation-selection.txt` selection above tested the final artifacts.

**Final result: 16 passed / 0 failed / 0 skipped in 175.364 seconds**, on this one provider (`instrumentation-final-2.log`, `runtime-summary-final.json`). This includes all three injected state-owner tests, both actual renderer-exit tests and all eleven selected existing browser regressions, including native bounded save. The excluded adaptive tests were not selected.

| Final actual renderer exit | Provider evidence | App/recovery evidence |
| --- | --- | --- |
| Crash, twice | Renderer PIDs **7075**, **7210**; code **11**; actual callbacks `didCrash=true` | App PID **6427** throughout; both dead views detached; explicit Retry, failed-state recreation, replacement navigation/Back and leaving pass |
| Deliberate termination, twice | Renderer PIDs **7321**, **7412**; code **-1**; actual callbacks `didCrash=false` | Same app PID **6427** throughout; direct Retry, repeated exit without automatic reload, failed-state recreation and toolbar Back pass |

`instrumentation-final-2-logcat.txt` contains these four real exit markers. Injected callback coverage remains separate. A first emulator startup was stopped during the host build to relieve memory pressure, and restarted after Gradle completed. The first instrumentation attempt then ended **before any test started** (`instrumentation-final.log`); `ApplicationExitInfo` explicitly reports app PID **3538**, reason **3 / LOW_MEMORY** (`instrumentation-final-attempt1-exit.txt`), with no Java crash trace. After the emulator's initial setup settled, the unchanged packages and selection passed the complete retry above. This setup failure is not renderer-crash evidence or a W02 acceptance result; its logs are retained.

The final AVD was stopped only after checking its exact name, removed with `avdmanager.bat delete avd -n KnitToolsRendererFinal_20261022`, and verified deleted. `cleanup-final.json` records zero remaining task emulator processes and an exited single-use Gradle daemon. The pre-existing ADB server, unrelated emulator and physical device remain untouched. `preservation-final.json` records zero unexpected changes among the captured existing-file hashes, zero tested-input hash mismatches, unchanged HEAD/index and branch `codex/audit-korjaukset-20261018`. `git diff --check` passes. No commit, push, PR, publication or deployment was performed.

#### Release boundaries

Production signing/package verification, physical performance, live W11/R10, remaining R11 entitlement checks, the separate INC-SIGN-01 owner action (4.1) and unrelated security findings remain **OPEN/BLOCKED**. Renderer callback injection is distinct from actual provider termination; neither is app-process death or natural memory-pressure evidence. The excluded 320 dp / 200% font / combined / TalkBack checks and the whole adaptive campaign are not repeated.

**Final artifact refresh remains required:** this task changes production browser code and its WebKit dependency. Earlier optimized/profiled APK hashes, bundled-profile verification, optimized smoke and startup/performance measurements do not describe this implementation. Regenerate/revalidate the final candidate's baseline/startup profiles as applicable, rebuild and hash its artifacts, and rerun its required optimized/release checks before making a final candidate claim. No profile collection, production signing, publication or deployment is performed here.

**2026-10-23 follow-up:** section **12.6** completes the required profile refresh and genuine signed local APK/AAB packaging from the current renderer/WebKit inputs. Earlier hashes remain historical. Final candidate runtime, including signed R10, and physical performance remain separate.

### 6.9 Live W11 preparation and user-requested postponement — 2026-10-22

**Historical status: BLOCKED / postponed at the user's request; superseded by the authorized phone follow-up in 6.10.** The live journey was prepared on a new disposable emulator, but stopped before manual sign-in. The user reported insufficient host RAM while another emulator was testing another app; the phone was also unavailable at that point. No personal installation or unrelated emulator was used or cleared in this preparation.

Evidence: `reports/live-ravelry-w11-20261022/` (ignored). The existing feature branch is `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. This task changes only this plan; there is no production, test-source, dependency or build-configuration correction.

| Input / preparation | Verified result |
| --- | --- |
| Build | Existing ordinary `debug` variant, built with `.\gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug`, without the isolated/optimized properties. **BUILD SUCCESSFUL in 9m 29s**; `build-debug.log`. |
| Exact APK | Retained as `reports/live-ravelry-w11-20261022/app-live-debug.apk`; SHA-256 **63ACD372EE22161D3A1C7E9C08EFAD15D75107228B36031290F5E654A72CA3B6**. Package `com.finnvek.knittools`, version 1 / 1.0.0, debug/non-minified build, not an optimized or production candidate. |
| Actual configuration | APK inspection confirms debuggable, INTERNET and BILLING permissions, `usesCleartextTraffic=false`, and Crashlytics collection false. Generated debug configuration confirms `ACTIVE_POLICY_TEST=false`, PostHog disabled and no Sentry DSN configured. No purchases were attempted. The network-disabled isolated/optimized packages and production signing/service gates were not changed. |
| Disposable device | Newly created `KnitToolsLiveW11_20261022`, serial `emulator-5584`, Android 17 / API 37, `sdk_gphone16k_x86_64`; fingerprint `google/sdk_gphone16k_x86_64/emu64xa16k:17/CP31.260623.012/16064790:user/dev-keys`. 1080 x 2400, density 420, font scale 1.0, 2048 MiB guest RAM. Existing debug demo data is synthetic; no personal app data was copied. |
| WebView | `com.google.android.webview` **149.0.7827.5**; app WebKit dependency **1.18.0**. This attempt did not independently exercise native bounded-save support. |
| Real entry | APK installed successfully; ordinary MAIN/LAUNCHER start, then actual UI taps **Tools > Ravelry**. The real HTTPS Ravelry login page finished loading and contained a password input; no external challenge was indicated by the minimal page-state probe. No credentials or form values were read. |

`environment.json`, `apk-badging.txt`, `debug-config-summary.json` and `login-ready.json` contain the sanitized preparation evidence. `browser-probe.mjs` uses a temporary local ADB forward and read-only DOM booleans; the login-ready probe does not capture cookies, headers, page bodies, form values, private account data, network traffic or download URLs. The optional history probe was not executed. Each forward was removed after its probe. No mock page, intercepted response, injected URL/state Bundle or direct HTTP PDF download was used.

**Restoration mechanism and live outcomes:** none ran in this attempt. Manual authentication, usable signed-in browsing/history, Activity recreation, background process termination/saved-task restoration, post-restoration PDF download, Save pattern delivery, saving/reopening/rendering, ownership/duplicate checks and cancellation/cleanup of a live import all remain **NOT RUN**. Existing section 6.7/6.8 local regression evidence is not relabeled as live evidence. No app defect was established, and no authenticated renderer failure was induced.

The first emulator boot was stopped to reduce host memory pressure during compilation, then restarted after the build. After reaching login, the emulator had exited before the user's postponement response was processed. Its exact task-owned AVD was deleted, and the recorded launcher/child PIDs are absent (`cleanup.json`). The pre-existing ADB server, unrelated emulators and physical phone were preserved. No authenticated session was established. The completed single-use Gradle invocation and one-shot probes leave no required background process.

`preservation-after-build.json` compares **1,298** captured source files with **zero changes**, unchanged branch/HEAD and an empty index before this plan edit. Final preservation and whitespace checks are recorded in `preservation-final.json` and `diff-check.txt`. No 320 dp, 200% font, combined configuration, TalkBack or adaptive campaign was repeated. No commit, push, pull request, third-party message, publication or deployment occurred.

**Release boundary:** R10's optimized result remains partial; this debug login-page preparation adds no restored-browser/download acceptance. Signed production packaging, physical performance, remaining R11 checks, the separate INC-SIGN-01 owner action (4.1) and unrelated security findings remain open/blocked. T08 and the renderer correction remain closed within their existing recorded boundaries. Section 6.8's final artifact/profile refresh requirement still applies to its production changes; this task adds no production change and does not refresh older optimized/profiled evidence. Recheck source/artefact identity and the live environment when resuming; this APK hash identifies only this preparation.

### 6.10 Live W11 on the authorized physical phone — 2026-10-22

**Result: W11 PASSED for the exact ordinary debug APK and physical-device environment below.** The user explicitly resumed W11, authorized the now-available phone instead of another emulator, and manually completed Ravelry sign-in on the app's real login page. This is live website/download evidence, not an optimized or signed production acceptance result. No production defect was established and no production, dependency, build-configuration or test-source change was made.

Evidence directory: `reports/live-ravelry-w11-20261022/phone-20261022/` (ignored). Branch remains `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. Source-hash comparison against the 6.9 build found only the intervening plan edit; the app inputs were unchanged. The existing older phone installation was updated with `adb install -r`, preserving app data. No app-data reset, other-device operation or emulator creation was needed.

| Tested input | Exact evidence |
| --- | --- |
| Artifact | Ordinary `debug`, package `com.finnvek.knittools`, version 1 / 1.0.0; retained `../app-live-debug.apk`, SHA-256 **63ACD372EE22161D3A1C7E9C08EFAD15D75107228B36031290F5E654A72CA3B6**. The installed APK's on-device SHA-256 matches (`install-after.json`). The successful 6.9 build and its inspected network/telemetry configuration are reused, not claimed as a new build. |
| Device | Physical **Pixel 9**, Android **17 / API 37**; fingerprint `google/tokay/tokay:17/CP3A.261005.005/16271449:user/release-keys`. Existing display 1080 x 2424, density 420, font scale 1.15; no size/density/font campaign or modification. |
| WebView | `com.google.android.webview` **155.0.8059.43**, confirmed before and after; app dependency WebKit **1.18.0**. The exact native/framework serializer branch was not separately instrumented in this live run. |
| Initial state | The app's own **Tools > Ravelry** opened the real HTTPS login page. Before manual sign-in the sanitized probe found the login form and no logout control. Afterwards it found usable authenticated search/pattern pages and a logout control, without reading form values, cookies or account identity. |

#### Restoration and actual website journey

1. Real search actions and site links established browsing history. The first five-entry context, including search/login history, restored the current eligible **Flax DK** page and signed-in access after Activity recreation, but retained only one history entry. This is **URL-only fallback evidence**, not a full-history pass. The exact fallback trigger (size, eligibility or serializer result) was not instrumented; no state-budget increase or policy change was made.
2. A short history was then established through actual site links: **Flax DK > Flax worsted**. Changing Android's night mode caused a real `MainActivity` destroy/create in PID **10448**, confirmed by the package-specific lifecycle event records. The two history-entry hashes and current index survived. Android Back returned to Flax DK, which remained signed in and usable. The phone's original `Night mode: yes` was restored before the process-restoration test.
3. After re-establishing that two-page context, Home backgrounded the app. Android reported task **26622**, `state=STOPPED`, `mHaveState=true`, and a saved Activity Bundle of **16,516 bytes**. This is the whole Activity payload, not a browser-only measurement. `adb shell am kill com.finnvek.knittools` terminated the background process; `pidof` became empty while the same task retained its Bundle with `app=null`.
4. A real tap on the observed KnitTools **Recents** card resumed task **26622** in new PID **23258**. Both page hashes/current index were retained, the live page loaded, and signed-in access remained usable. Android Back again returned to Flax DK. This is deliberately triggered background process replacement with saved-task restoration; it is not natural memory-pressure, force-stop, task-removal or arbitrary unsaved-death coverage.
5. **After both restoration mechanisms**, the site's actual **Download > download PDF** controls were tapped on Flax DK, which offers a **free Ravelry download**. The existing WebView download listener and `WebPdfDownloader` path delivered the file to the native **Save pattern** sheet. No URL was injected and no standalone HTTP download or response interception was used.
6. The sheet's title was set to synthetic **W11 Test 20261022**, and **Save to Library only** was tapped once. The resulting saved-pattern detail opened the PDF. Its first page was visually verified in the app (`saved-pdf-rendered.png`); the reader reported **Page 1 of 8**. The browser/account UI is absent from this screenshot. Returning to **Library > Saved Patterns** showed one test entry, which reopened and rendered the same PDF.

`history-*.json` retains only entry identifiers, eligibility booleans and hashes, not raw URLs. `status-*.json`, `activity-*.json`, `process-absent.json` and the package-filtered lifecycle events identify the restoration mechanisms and their outcomes. The existing host-side background/Recents method was adapted to the explicitly authorized phone; the emulator-only T08 runner and its fixture were not invoked or weakened. No login-form screenshot, private account dump, raw network log, signed download URL, cookie, password or authorization header was captured. No authenticated renderer crash/termination was attempted.

#### Ownership, cancellation and cleanup

Only task-created Library data was saved; existing projects and app data were preserved. App-private `files/pattern_pdfs` contained **0 files before**, **1 pending/saved file after the first live download**, **2 while a second real download awaited confirmation**, **1 after Cancel**, and **0 after removing the test Library entry**. `cache/web_pdf_downloads` was empty after each completed download and after cleanup. These are file inventories under `run-as` for this app; no personal database or WebView cookie store was copied.

The second real website download reached Save pattern, its sheet was expanded by a real swipe, and **Cancel** was tapped. The original saved file's path hash remained identical while the second copy disappeared (`cancel-result.json`). The Library still showed exactly one named test entry; its PDF reopened with a rendered image and the same eight-page count after cancellation. This proves this pending-import cancellation/cleanup path and absence of an extra test entry from that cancellation. It does **not** claim content-based deduplication of multiple independently confirmed imports or a live mid-transfer timeout/cancellation test; earlier downloader/ViewModel regression evidence remains local evidence only.

Cleanup used the site's existing logout control for the session created by this task. Reopening Tools > Ravelry returned to the real login form with no logout control (`logout-reopen-check.json`). Only **W11 Test 20261022** was removed through its detail menu and confirmation; the app-owned PDF count returned to baseline and the test entry disappeared. The original night mode is restored, temporary ADB forwards are absent, and one-shot Node/PowerShell helpers have exited (`cleanup.json`). No unrelated browser session was cleared or unrelated process terminated. The updated phone installation and all other app data remain in place.

`node reports/live-ravelry-w11-20261022/phone-20261022/verify-evidence.mjs` **passed**: it checks the recorded two-page/recreation and process/task invariants, authentication booleans, file counts and preserved-file identity, test-entry count, reopened PDF controls, logout, setting restoration and installed APK identity (`result.json`). This validates the saved observations, not a replay of the live service. Node/PowerShell helper syntax checks and `git diff --check` pass; no application tests/build/lint/adaptive campaign were rerun because this follow-up changes only this plan. Final source preservation is recorded in `preservation-final.json`.

**Remaining release boundaries:** the live-browser portion of R10 now has passing evidence **only on this ordinary debug APK and Pixel 9**. Optimized R10 and signed production acceptance remain open, along with production signing/package verification, physical performance measurements, remaining R11 checks, the separate INC-SIGN-01 owner action (4.1) and unrelated security findings. T08 and the renderer correction retain their established closure boundaries. Section 6.8's required final artifact/profile refresh for its production changes still applies; no older optimized/profiled hash is relabeled as evidence for this APK. No excluded UI check, commit, push, pull request, third-party message, publication, deployment or other work package was performed.

## 7. Work package 1C: remove the unnecessary camera permission

### Implementation

1. Search the current app for all direct camera access and every `CAMERA` reference before removal.
2. Confirm both user capture paths still delegate to `ActivityResultContracts.TakePicture()`.
3. Remove the app's `android.permission.CAMERA` declaration.
4. Remove only the camera-permission-specific launchers, pending permission owner state, rationale/settings UI, imports, and obsolete branches from:
   - `PhotoGalleryScreen.kt`;
   - `PatternPickerSheet.kt`.
5. Start the existing capture flow directly after the existing product/Pro admission checks.
6. Preserve request ownership, project identity, pending output URI/file state, and Activity-result restoration.
7. Preserve the distinction between an admitted in-flight capture and permission to start a new Pro-gated operation.
8. Keep capture-target creation and cleanup with the existing repository/ViewModel/storage owners.
9. Keep `ActivityNotFoundException`, `SecurityException`, and other existing meaningful launch-failure handling.
10. Preserve FileProvider URI grants and narrowly scoped paths.
11. Keep the camera hardware feature optional. Do not add a required camera feature.
12. Remove obsolete permission strings only after proving they are unused across all locales; do not combine this with a translation rewrite.
13. Update tests that previously expected a runtime camera permission request.

External camera delegation does not require the calling app to request `CAMERA`. This change removes an unnecessary prompt and avoids denying capture merely because the user denied that app permission. [Minimize permission requests](https://developer.android.com/privacy-and-security/minimize-permission-requests)

### Tests

| ID | Scenario | Expected result |
| --- | --- | --- |
| C01 | Progress-photo capture succeeds | Photo persists under the correct project with no app camera-permission prompt. |
| C02 | Pattern-photo capture succeeds | Existing image-to-PDF/attachment flow works with no app camera-permission prompt. |
| C03 | Cancel either camera flow | No attachment or abandoned temporary capture remains. |
| C04 | No camera handler / handler launch fails | Existing understandable failure path runs and the target is cleaned up. |
| C05 | Project changes or disappears while capture is pending | Result is not attached to the wrong project; cleanup remains correct. |
| C06 | Activity recreated while external camera is active | Result is handled once for the original admitted request. |
| C07 | Free/expired and authorized trial/Pro paths | Existing gates and contextual continuation still work. |
| C08 | Upgrade from a version whose camera permission was denied | The new app flow no longer depends on the old permission decision. |
| C09 | Merged release manifest | `CAMERA` is absent, including transitive declarations. |

Reuse `ProgressPhotoCommitBoundaryTest`, capture/storage tests, and relevant pattern-picker tests where applicable. If a dependency reintroduces `CAMERA`, address the merged-manifest source deliberately and verify the final artifact.

### 7.1 Implementation record — 2026-10-20

**Status: Implemented and C01–C09 verified within the explicitly stated evidence boundaries below.** This task implements section 7 only. It does not certify a release candidate, physical camera devices, process-death restoration, or live billing. The unrelated release-surface self-test blocker remains recorded below.

Implemented:

- Inspected camera permission references and direct camera API usage. Both production capture paths still use `ActivityResultContracts.TakePicture()`; there is no direct Camera/Camera2/CameraX implementation to justify the permission.
- Removed `android.permission.CAMERA` from the main manifest. `android.hardware.camera` remains `required="false"`; the existing FileProvider authority, non-exported provider, URI grants and narrow capture paths are unchanged.
- Removed runtime camera-permission launchers, denial/settings guidance, imports and obsolete pending permission state from `PhotoGalleryScreen` and `PatternPickerSheet`. Existing product/Pro admission now starts capture directly. Pattern capture retains a saveable `pendingCaptureLaunch` for its actual queued launch, distinct from permission state.
- Preserved pending owner/URI/file/result state, delayed Room-restoration handling, admitted-operation authorization, duplicate-result protection and the existing ViewModel/repository/storage cleanup owners. Existing `ActivityNotFoundException`, `IllegalStateException` and `SecurityException` handlers still abandon the target; both paths also show the existing localized generic error message on launch failure.
- Removed only `camera_permission_denied` and `camera_permission_denied_permanent` from all 12 resource directories after confirming no remaining active usages. Historical language-audit receipts were retained. No translation rewrite was included.
- Updated the three affected JVM/source contracts and the release-surface exact permission set. Added a self-test mutation that rejects reintroduced CAMERA; the existing RECORD_AUDIO mutation now anchors to VIBRATE. Updated only the camera/permission descriptions in `PROJECT.md`.
- Extended the existing `ExternalActivityResultOwnershipTest` for direct launches, failures, cleanup, gates and admitted results. Added `ExternalCameraCaptureTest` to drive the real external AOSP camera and Android Activity results, including recreation of MainActivity while the camera remains active.

#### C01–C09 evidence

| Case | Status | Evidence and boundary |
| --- | --- | --- |
| C01 | Verified — actual external camera on emulator | AOSP camera writes through the production FileProvider URI, returns through `TakePicture`, and creates exactly one readable progress photo for the original project at row 23. The installed app does not request CAMERA; no app permission prompt is accepted or bypassed by the test. |
| C02 | Verified — actual external camera on emulator | Camera result enters the existing image-import surface; Create pattern PDF produces exactly one project document for the original project. Android `PdfRenderer` opens the resulting one-page PDF, and temporary captures are removed. |
| C03 | Verified — actual external camera + controlled results | Android Back cancels each real external camera flow; neither photos nor documents are created and each target is deleted. Controlled result tests also verify both cancellation paths against real files/Room. |
| C04 | Verified — fault-injection instrumentation + source inspection | Each capture UI receives `ActivityNotFoundException`, `IllegalStateException` and `SecurityException` from its ActivityResultRegistry launch. All six cases leave no target/attachment; the production handlers show the existing generic error Toast. This does not claim a physical device without a camera handler or a visual assertion of the Toast. |
| C05 | Verified — controlled-result instrumentation | Switching from A to B and deleting A before the result are tested for both capture paths using real ViewModels, Room and files. No result attaches to B or recreates A; abandoned captures are removed. |
| C06 | Verified — actual external camera and Activity recreation | Both external-camera success tests call Android `Activity.recreate()` while the camera is foreground, observe destruction of the original Activity, then take/confirm a photo and observe a different resumed Activity. Exactly one result is stored for the admitted owner. Separate registry tests cover restored saved state waiting for Room and duplicate results. These are not process-death tests. |
| C07 | Verified — real policy/Room with synthetic entitlement | Existing `AuditPart15RoomTest` covers free, active trial, expired and purchased policy states. UI tests reject locked capture before target creation, continue each pending action after contextual trial start, and preserve admitted capture after the gate closes. Existing PDF readability after expiry also passes. No Play Billing purchase or live entitlement was exercised. |
| C08 | Verified — emulator package upgrade without clearing data | Installed the previous CAMERA-declaring APK, revoked CAMERA and set `USER_SET`/`USER_FIXED`; `dumpsys package` confirmed denial. Installed the new APK with `adb install -r`, without clearing the target package. Its requested permissions contain no CAMERA, and the real external capture/cancellation/recreation tests above pass on this upgraded installation. |
| C09 | Verified — generated merged release manifest | `:app:processReleaseMainManifest` passed. Parsed every `uses-permission*` entry in `app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`: CAMERA count **0**, optional camera feature retained, FileProvider `exported=false` and `grantUriPermissions=true`. This is the merged release manifest, not a built/signed release APK. |

#### Executed inputs and checks

- Branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`; dirty local inputs, no commit.
- Gradle 9.8.1 with Eclipse Temurin JDK 17; debug runtime checks use `-Pknittools.isolatedTest=activepolicy`, application ID `com.finnvek.knittools.activepolicytest`, runner `com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner`.
- Task-owned, read-only `medium_phone` AVD on `emulator-5586`, API 36, `sdk_gphone64_x86_64`, 1080 × 2400, density 420, font scale 1.0. External handler: `com.android.camera2/com.android.camera.CaptureActivity`; emulated camera image source. No physical Android device was connected.
- Previous upgrade APK SHA-256: `7D30E3463624187D99B8BCC7382784112863BE2208480605570FBDFA6424DA6C`.
- Tested app APK SHA-256: `76A021395D2BFC0D298CA99393D7A4091D3D8299D15689F22E438A88533BE542`.
- Final external-camera test APK SHA-256: `509DE12A1142CBC432C12D03C8B16C59DA2BDA9A3398BAD5F034D2D18098C4C3`. The 20-test registry run used `013AC15EE458477B56A2E11B01BF80634FD5AE5ECEFF0543C4EA0638034A093A`; only the external-camera test driver's sheet-expansion handling changed between these APKs.
- Merged release manifest SHA-256: `BECEACA8473FBA3833196B978835715327545EE6FFA18755E290347FC4E0C8BE`.

| Check actually executed | Result |
| --- | --- |
| Focused `:app:testDebugUnitTest` selection | **60 passed**, zero failures: `ProgressPhotoGallerySourceTest`, `CounterPhotoMutationsTest`, `PatternPickerSourceTest`, `PatternPickerWebEntryTest`, `PatternImageImportViewModelTest`, `ProgressPhotoStorageTest`, `ProgressPhotoRepositoryDomainApiTest`, `FeatureGateRaceSourceTest`, `ProjectFoldersContractTest`, `WebPatternLocalizationArchitectureContractTest`. |
| `:app:assembleDebug`, `:app:assembleDebugAndroidTest` | Passed. Android-test APK rebuilt after final test-driver corrections. |
| `:app:ktlintMainSourceSetCheck`, `:app:ktlintTestSourceSetCheck`, `:app:ktlintAndroidTestSourceSetCheck` | Passed; final test-only rebuild also passed its ktlint check. |
| `:app:lintDebug` with the isolated activepolicy property | Passed, **0 errors, 4 existing `MissingOnRenderProcessGone` warnings**, unchanged from section 6. |
| `:app:processReleaseMainManifest` + XML inspection | Passed; C09 evidence above. |
| `ExternalActivityResultOwnershipTest` via explicit `adb -s emulator-5586 shell am instrument -w -r -e class ...` | **20 passed** in 59.190 seconds, no skipped tests. Real app persistence with a delayed/injected ActivityResultRegistry; these are not external-camera tests. |
| `ExternalCameraCaptureTest` via the same explicit runner | **4 passed** in 27.526 seconds, no skipped tests. Real external AOSP camera, real Activity results, real Activity recreation, actual files/Room/PDF. |
| `ProgressPhotoCommitBoundaryTest` + `AuditPart15RoomTest#admittedCameraImageCreatesRealPdfAfterExpiryAndExistingPdfRemainsReadable` + `AuditPart15RoomTest#allSevenGatesUseRealPolicyAndCounterYarnWritesAcrossFourStates` | **5 passed**, no skipped tests. Real Room/SQLite and filesystem checks include cancellation before/after commit and failed post-commit readback. |
| `tools/release-surface-test.ps1` | **22 passed, 2 failed**. CAMERA and RECORD_AUDIO mutations both pass. Baseline and widget-token warning-fixture assertions fail because existing locale parity reports declared `pt-BR` versus resource directory `pt-rBR`. The locale checker/configuration was not changed in this task; this overall check is not marked passed. |

Local evidence: `%TEMP%\knittools-camera-permission-20261020`, including `validation-rerun.log`, copied JVM XML results, `final-build.log`, `runtime-driver-final-build.log`, `ownership-final.log`, `external-camera-verified.log`, `room-boundaries-runtime.log`, `upgrade-before.log`, `upgrade-after-package.log`, `merged-release-check.json`, copied merged manifest, APK hashes and `release-surface-test.log`.

Earlier failed runs remain in that evidence directory. Initial ktlint issues were corrected. Runtime fixture failures came from attempting Expand on an already-expanded sheet and from looking for camera/PDF actions in a partially expanded sheet; the test drivers now use available expansion actions. A separate host `uiautomator dump` diagnostic could not acquire the already-used UI automation connection and provided no valid test evidence. The final passing runs above do not rely on it.

#### Remaining limitations and scope

- Physical/OEM camera apps and a device with no camera handler were unavailable. Launch failures were injected; no unsupported-device success is claimed.
- Actual app-process death during camera capture was not exercised. C06 covers Activity recreation while the external camera is active, as specified; registry save-state tests are additional controlled coverage.
- Runtime checks used the isolated debug target and synthetic policy states. Optimized/signed release runtime and live Play Billing are unverified and outside section 7.
- The pre-existing locale-parity failure and four WebView lint warnings remain unresolved. No unrelated localization or browser fix was included.
- Initial file fingerprints were retained to check preservation. Trial notice, Ravelry restoration, MainActivity, FileProvider paths, dependencies and other out-of-scope inputs are unchanged; the index remains empty and branch/HEAD unchanged.
- Excluded 320 dp, 200% font-scale and TalkBack campaigns were not run. No adaptive-layout work or another work package was started. No commit, push, pull request or publication occurred.
- The task-owned emulator and its private helpers were stopped (`cleanup.json`); single-use Gradle daemons completed normally. A different task started `emulator-5590` during this run, so the now-shared ADB server and emulator networking service were deliberately left running to avoid interrupting that work.

## 8. Work package 2: adaptive layouts and continuity — required now

### 8.1 Product contract

Deliver usable, intentional layouts for phone portrait, phone landscape, tablet portrait/landscape, resized windows, and foldable transitions. Preserve the established visual design and all five top-level destinations.

The current target is API 37. Android 17 removes the Android 16 developer opt-out for orientation/resizability restrictions on affected large displays. Therefore, the portrait manifest setting is not a substitute for this work. [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17)

Do not expand this task into TV, automotive, XR, a new app information architecture, or a general multi-pane navigation framework.

### 8.2 Window information and shared tokens

1. Derive layout decisions from the current app window and available content constraints, not device model, physical screen size, or `rotation == 0`.
2. Recompute when width, height, density, insets, or folding features change.
3. Use a small shared sizing policy with standard width categories:
   - compact: below 600 dp;
   - medium: 600–839 dp;
   - expanded: 840 dp and above.
4. Recognize compact height below 480 dp. A wide but short window must not receive a layout that requires tablet height.
5. Use content constraints after navigation and inset allocation when sizing child content.
6. Prefer existing Compose layout APIs for width/height changes.
7. For separating or occluding folding features, use the supported AndroidX WindowManager APIs. Add only the necessary stable artifact if not already available; verify compatibility and dependency metadata.
8. Do not add an entire adaptive navigation stack solely to obtain a width breakpoint.

The width categories are layout inputs, not a mandate that every content type use the same maximum width. [Window size classes](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes)

Recommended initial project tokens:

| Token purpose | Initial value/policy | Rationale and validation |
| --- | --- | --- |
| Form/reading column | 600 dp maximum | Retains the established tool/form reading width. |
| Single-column list/detail body | 840 dp maximum | Prevents sparse rows from stretching across a desktop-sized window. |
| Insights content region | 1200 dp maximum | Gives charts more room while bounding text/statistics separately. |
| PDF, full photo, browser viewport | Available unobstructed content area | No global 600 dp clamp. |
| Photo thumbnail minimum | Start at 180 dp on wider windows | Tune from actual thumbnails and captions; preserve the existing compact two-column presentation where it fits. |
| Counter action group | Existing `CounterControlsMaxWidth` | Do not stretch the main counter controls with the screen. |
| Outer page padding | Existing 16 dp compact; consider shared 24 dp wide | Use theme tokens and avoid duplicated padding. |

These are proposed KnitTools design values, not Android requirements. Confirm them visually with real app content before finalizing. Keep the number of tokens and wrappers small; reuse existing dimension owners.

### 8.3 Manifest and lifecycle

1. Remove the portrait-only Activity restriction as part of enabling and testing landscape support.
2. Remove only lint suppressions made obsolete by that removal.
3. Retain normal Activity recreation. Do not add a broad `configChanges` list to hide state-restoration failures.
4. Do not add aspect-ratio/resizability opt-outs or device-specific orientation workarounds.
5. Keep inset and IME handling correct after each layout change.
6. Do not cache dp-to-pixel results without the relevant density/size keys.
7. Preserve normalized PDF annotation/reading-guide coordinates; never rescale persisted geometry as part of a window resize.

The foldable guide includes manual configuration handling as an option, but it is not a requirement to replace this app's lifecycle model. Camera-preview and sensor-remapping sections do not justify adding CameraX or sensor logic to an external-camera app. [Trifolds and landscape foldables](https://developer.android.com/develop/adaptive-apps/guides/foldables/trifolds-and-landscape-foldables)

### 8.4 Navigation shell

1. Preserve `TopLevelDestination` as the source of truth for Projects, Library, Tools, Insights, and Settings.
2. Keep the existing bottom navigation on compact windows.
3. Add a navigation rail for windows at least 600 dp wide with sufficient height for all five destinations.
4. For compact-height windows, use the presentation that keeps every destination reachable without clipping; the default plan is to retain the compact navigation presentation.
5. Apply existing route-based navigation visibility to both presentations. Full-screen/media routes must not gain unwanted persistent navigation.
6. Reuse labels, route selection, icons where already used, and `navigateToTopLevel`.
7. Keep one `NavHost`/`NavController` across presentation changes. Do not create two alternative hosts with independent back stacks.
8. Preserve `saveState`, `restoreState`, and `launchSingleTop` behavior.
9. Preserve Projects graph ownership of `CounterViewModel`.
10. Ensure resizing does not duplicate destinations, reset a selected project, or change the highlighted top-level tab incorrectly.
11. Respect RTL start/end placement.

This is a navigation presentation change. It does not add list-detail routes, change destination identities, or migrate to another navigation library.

### 8.5 Screen-by-screen implementation

| Screen family | Required current-work behavior | Existing behavior to preserve |
| --- | --- | --- |
| Projects | Center and bound the list; size the Continue hero intentionally; keep filters, selection actions, and creation reachable in short windows. | SQL/list ordering, folder filter, active/completed visibility, Continue action, card-to-overview navigation. |
| Project overview | Use a bounded detail column; constrain hero imagery and prevent very long text rows. Reflow existing sections only where necessary. | Section order, shared overview components, document/yarn/session ownership. |
| Counter | Keep number/actions compact; make all actions and extra counters reachable in landscape. Reduce height assumptions or allow scrolling before inventing a second pane. | Counter step, Undo, sessions, reminders, Pro rules, persistence, feedback, and existing control width. |
| Library overview | Bound text/list content; prevent full-width empty or sparse rows. | Established hub structure and destinations. |
| My Yarn / Saved Patterns | Bound list/detail text; preserve stable item identity and list position across resize. | Sorting, filters, metadata, editing, links, and shared detail components. |
| All Photos / project photos | Replace fixed wide-window two-column behavior with width-aware columns; bound cell sizes; keep selection and captions usable. | Compact layout where appropriate, project filtering, stable item keys, photo ordering. |
| Full photo viewer | Use the full unobstructed viewport with aspect ratio preserved and controls inside insets. | Opaque background, existing zoom/pan/dismiss behavior. |
| PDF reader | Use available reading space, not a form width. Recompute viewport transforms after resize. Ensure toolbar, bookmarks, guides, and export actions remain reachable. | Reading/markup modes, page/document identity, normalized annotations, follow state, bookmarks, export behavior. |
| Ravelry browser | Permit the WebView to use the available viewport; keep explanatory text readable without constraining the whole page to 600 dp. | Website-owned login, checkout rules, restoration, PDF download interception. |
| Insights | Permit wider charts; bound hero/prose/statistics independently. Verify axis labels, selection, and empty/sparse ranges. | Editorial appearance, no new cards/surfaces, existing aggregation, filters, time/counter semantics. |
| Tools home | Use the shared reading/list width token instead of a duplicated literal. | Tool destinations and established typography. |
| Calculators and reference tables | Keep input forms at reading width; give legitimately wide tables horizontal scrolling or a suitable wider region when needed. | Canonical values, unit conversion, localization, result ordering. |
| Settings / Backup / Pro | Bound prose and form content; keep actions visible with IME and in short windows. | Existing data/privacy sections, real entitlement state, backup confirmation semantics. |
| Notes and histories | Bound reading/editing text while keeping scrolling and keyboard behavior usable. | Draft/merge ownership, project identity, list order, overflow actions. |
| Shared sheets/dialogs | Use sensible width/height limits and scrolling where required; keep Cancel and the primary action reachable. | `FormSheet`, `SheetTitle`, `ConfirmationDialog`, `CancelButton`, colors and component conventions. |

Update `ToolScreenScaffold` to accept an explicit content-width policy or maximum-width parameter. Default ordinary tools/forms to the current reading width. Opt media/browser or wide-data screens into their appropriate policy. Do not silently remove the cap from every existing caller.

Do not add a grid to Projects simply because the display is wide. A bounded list is acceptable when it preserves the product hierarchy and uses space deliberately. Likewise, do not add a PDF-plus-counter workspace or two-pane routing unless a concrete requirement emerges.

### 8.6 Fold and hinge behavior

1. Cover folded/outer, unfolded/inner, and continuous resize transitions.
2. Do not assume a device's natural orientation is portrait.
3. Recompute sizes when density differs between displays.
4. Distinguish a crease from an occluding/separating feature. A harmless crease does not require artificially narrowing the whole UI.
5. Do not place a main counter action, form action, modal, or essential PDF control under a separating hinge.
6. For the current single-content layouts, using a suitable unobstructed pane is an acceptable minimal response to a separating hinge.
7. Use a dedicated supporting pane only if needed for usability; do not build a general multi-pane framework.
8. Test both tabletop and book-like transitions where supported by the test device.
9. Do not invent a half-open/tabletop requirement for a trifold device that does not expose that posture.
10. An emulator approximating a wide unfolded window proves responsive layout only; it is not proof of all physical trifold/OEM behavior.

### 8.7 State that must survive layout changes

| State | Required owner/behavior |
| --- | --- |
| Active project and counter values | Existing repository/ViewModel ownership; no reset or duplicate mutation |
| Active work session | Existing Room-owned session anchors; resizing never starts/stops a session |
| Project folder and selection context | Existing navigation/SavedStateHandle ownership; retain valid context |
| List scroll position | Saveable lazy-list/grid state with stable item keys; preserve a meaningful visible item when columns change |
| Form text and validation | Existing saveable draft/ViewModel state; no blanking on recreation |
| Notes | Existing editor and merge semantics; no loss or duplicate save |
| PDF document/page/guides | Existing document-owned state and viewport restoration |
| PDF gestures | No duplicate geometry transform; cancellation of an interrupted gesture is safe |
| Ravelry context | Work package 1B |
| Trial dialog | Work package 1A |
| Pending camera result | Work package 1C |
| Incoming PDF/share | No duplicated imports, wrong owner, or automatically accepted confirmation |
| Theme and app language | Existing preference/AppCompat ownership, without fallback-language flashes |

Inspect and fix any additional loss exposed by this matrix. Change the owning state path only; do not move every UI property into a ViewModel.

### 8.8 Adaptive validation matrix

Dimensions below refer to logical app-window dimensions, not emulator pixel resolution. Use normal font scale for this matrix.

| ID | Configuration | Required coverage |
| --- | --- | --- |
| A01 | Phone portrait, about 360–411 dp wide | Compact-layout regression across all five tabs and key detail screens. |
| A02 | Phone landscape, about 640–800 dp wide and 360–450 dp high | Short-height counter, forms, dialogs, PDF controls, keyboard behavior. |
| A03 | Widths just below/at/above 600 dp | Navigation presentation and content-size transition without state loss. |
| A04 | Widths just below/at/above 840 dp | Wide-content transition and component bounds. |
| A05 | Tablet portrait, about 800 × 1280 dp | All screen families, sheets, and meaningful empty/populated states. |
| A06 | Tablet landscape, about 1280 × 800 dp | Full content-width matrix, galleries, charts, PDF and browser. |
| A07 | Large/resizable window, about 1600 dp wide | No excessively stretched text, controls, or thumbnails. |
| A08 | Split screen / freeform resize through compact, medium, expanded | Back stacks, drafts, list position, insets, and state continuity. |
| A09 | Folded → unfolded → folded, landscape-natural foldable | Correct window-based layout and continuity. |
| A10 | Separating hinge/posture simulation where available | Essential controls and content are not hidden by the hinge. |
| A11 | Display-density transition where available | Fresh measurements and correct PDF/canvas transforms. |
| A12 | Resize/recreate with keyboard open and a form draft | Input survives and actions remain reachable. |

Execute the current target behavior on API 37 and a representative pre-37 platform. Include a minimum-supported API 29 startup/navigation check in final compatibility coverage.

Record measured dp values. Boundary checks can be focused rather than a full-app Cartesian product. Exercise all screen families on a tablet and compact regression device; repeat only risk-relevant journeys on other configurations.

Use both themes on representative affected screens. Do not repeat the excluded 320 dp, 200% font, or TalkBack campaigns.

### Acceptance

- [x] Landscape is enabled and usable.
- [x] All five destinations remain reachable and retain their state when navigation presentation changes.
- [x] Forms/text, lists, charts, media, and browser content have distinct appropriate sizing.
- [x] Galleries adapt without giant thumbnails or lost selection.
- [x] PDF annotations/guides retain correct alignment after resizing.
- [x] No resize/fold operation loses a draft, counter value, work session, pending capture, or browser context covered by the contract.
- [x] Every required matrix row is verified or explicitly blocked; a single tablet launch is not accepted as completion.

Completed within section 8.14's isolated-app/emulator evidence boundaries. No required locally executable adaptive check remains blocked; unrelated release prerequisites remain open.

### 8.9 Shared-foundation implementation record — 2026-10-20

**Status: this shared-foundation task is complete, with the verification limits below. Work package 2 remains open.** This task is limited to sections 8.1–8.4 and the shared window/folding-feature host in 8.6. Section 8.5 and full adaptive integration acceptance are not completed by this change.

Implemented:

- `ui/layout/AdaptiveLayout.kt` defines the compact/medium/expanded width decisions at 600/840 dp, compact height below 480 dp, and explicit `Form` (600 dp), `ListDetail` (840 dp), `Chart` (1200 dp), and `Unrestricted` content-width policies. Existing 16/24 dp spacing and counter dimension owners remain unchanged.
- `AdaptiveContent` derives its dimensions from current Compose constraints and explicitly reads `LocalDensity` during composition. The density-only runtime case exposed stale dp values when pixel constraints were unchanged; converting the current pixel constraints with current density fixes that case without rebuilding navigation. The shell provides content dimensions after navigation/inset allocation; `ToolScreenScaffold` provides them again after its toolbar and width policy. No physical-display, device-name, rotation-zero, or cached density conversion determines layout.
- `UnobstructedWindowContent` observes `WindowInfoTracker` with lifecycle-aware collection. Safe-drawing/IME insets are allocated and consumed once. Separating or fully occluding features divide the available window-coordinate rectangle; the largest unobstructed pane hosts the existing shell and content, with top/start tie-breaking and physical placement in RTL. Harmless creases leave the viewport intact. Multiple reported separators are handled without inventing device postures.
- `KnitToolsNavigationScaffold` keeps one content slot while choosing the existing bottom bar or Material navigation rail. Rail requires an unobstructed area at least 600 dp wide and 480 dp high; its items can scroll if needed. Both presentations use the existing destinations, icons, localized labels, selection behavior, `navigateToTopLevel`, and shared route-visibility rule in `TopLevelNavigation.kt`.
- The existing `rememberNavController`, `NavHost`, route identities, graph builders, tab `saveState`/`restoreState`/`launchSingleTop`, and Projects-scoped `CounterViewModel` ownership are preserved. A presentation or pane change does not branch between hosts.
- `ToolScreenScaffold` defaults to the existing form width. Ravelry explicitly selects `Unrestricted`, while its explanatory text retains form width; the browser restoration/download/security logic is unchanged. Tools home replaces its duplicated 600 dp literal with the shared form policy. These are the only individual-screen integrations in this task.
- MainActivity's portrait restriction and its `DiscouragedApi,LockedOrientationActivity` suppression were removed. Normal Activity recreation, `singleTop`, and `adjustResize` remain; no `configChanges`, orientation, aspect-ratio, or resizability opt-out was added.
- Dependency inspection found WindowManager 1.5.0 already in the resolved graph via Compose UI 1.12.1. Direct `androidx.window:window:1.5.1` and instrumentation-only `window-testing:1.5.1` declare the APIs used here. This is the stable correction release documented by [AndroidX](https://developer.android.com/jetpack/androidx/releases/window#1.5.1). New verification-metadata checksums were independently matched against Google Maven; AAR metadata compatibility passed on the current AGP 9.4.1 / Kotlin 2.4.21 / compileSdk 37 toolchain. No adaptive navigation framework or navigation migration was added.

#### Focused verification

Local raw evidence is in `reports/adaptive-foundation-20261020/`, including the initial branch/HEAD/dirty-input hashes, dependency graph, checksum verification, build logs, and copied JVM XML results.

- The focused JVM selection passed **93 tests, zero failures/errors/skips**: adaptive geometry and thresholds, navigation arguments/launches/shares, incoming PDFs, tab-label contracts, counter workspace, UI consistency/state retention, trial-notice source contract, Ravelry source contract, and pattern-picker source contract.
- `:app:assembleDebug` and `:app:assembleDebugAndroidTest` passed with `-Pknittools.isolatedTest=activepolicy`. The final density correction, 93-test JVM selection and all three source-set ktlint checks are verified in `density-validation.log`. The aggregate command remains failed solely because of the two Detekt findings below.
- `:app:detekt` failed on two existing findings only: `MainActivity.kt:257` (`ComplexCondition`) and `RavelryBrowserState.kt:89` (`ReturnCount`). Both production files match their initial hashes. These unrelated trial/browser changes were preserved; no baseline or suppression was added.
- `:app:lintDebug` was attempted but interrupted after prolonged class analysis under host memory pressure. It did not produce a completed result for this change. Earlier sections' lint results and the pre-existing report on disk are not current adaptive-foundation verification.
- Existing `TrialNoticeRestorationTest` (10 tests) and `RavelryBrowserNavigationTest` (11 tests) both passed against the final production APK on API 37: **21 passed, zero failures/skips**, 35.196 seconds (`regression-api37-verified.log`). These retain their earlier synthetic-entitlement/local-browser boundaries; they do not close T08 or live-service W11.

The final production APK passes all **five adaptive instrumentation tests on API 37**, zero failures/skips, 41.123 seconds (`runtime-api37-verified.log`), and **the same five on API 36**, zero failures/skips, 32.710 seconds (`runtime-api36-final.log`). Including the 21 trial/browser regression tests, these are **31 passing instrumentation executions** across the two API levels, not 31 distinct tests. The final test-only IME-animation synchronization change also passes Android-test assembly and ktlint (`ime-fixture-build.log`).

| Matrix reference | Focused evidence; not full row acceptance |
| --- | --- |
| A01 | Actual MainActivity at 360 × 800 and 411 × 800 dp, all five tabs reachable, Tools → Cast On draft and tab restoration. |
| A02 | Actual orientation change to 800 × 360 dp recreates MainActivity, keeps the draft and uses bottom navigation. No claim about counter/dialog/PDF screen redesigns. |
| A03–A04 | Rendered shell at 599/600/601 and 839/840/841 dp; same NavController, back-stack entry ID, previous entry, saveable draft and Projects-graph test ViewModel across transitions. Actual MainActivity at 600/840 dp also preserves the calculator draft. Form/list/chart/unrestricted bodies measure 600/840/1200/1600 dp respectively. |
| A08 | Actual emulator window overrides and synthetic parent-constraint changes exercise resize continuity, tab restoration and Back. This is not a real split-screen/freeform gesture campaign. |
| A09–A10 | Real WindowManager test publisher emits book/tabletop separating features, harmless crease and unfolded state. Shell controls and content remain outside the hinge; draft and entry survive. JVM cases additionally cover occlusion without separation, inset origins, RTL tie-breaking, out-of-host features and multiple separators. Physical folded-display/natural-orientation transitions remain open. |
| Relevant A11–A12 portions | Same-pixel 1.0 → 1.25 density changes refresh dp sizing; 479/480 dp height and inset changes switch navigation correctly in light/dark themes. RTL rail is at the physical right. Actual IME visibility keeps the bottom bar above the keyboard; the tool draft survives orientation, resize and explicit Activity recreation. No PDF transform or physical display-density transition claim. |

`AdaptiveNavigationTest` uses real Compose Navigation with synthetic destinations and a real graph-scoped test ViewModel. Production graph/CounterViewModel ownership is unchanged by source comparison; this is not a claim that every project/session state path was exercised. `AdaptiveActivityTest` drives the actual MainActivity and calculator; it reuses `TestContentSplashRule` to bypass only the splash pre-draw gate that conflicts with the Compose test clock, so startup/splash timing is not tested. The test restores the emulator's original display override in `finally`.

The API 37 device is the task-owned read-only `Small_Phone` AVD, `emulator-5592`, `sdk_gphone16k_x86_64`. API 36 uses the task-owned read-only `medium_phone` AVD, `emulator-5594`, `sdk_gphone64_x86_64`. Both synthetic hosts use a 2000 × 1800 pixel emulator window at 160 dpi; MainActivity checks record the logical dimensions above at the same density and font scale 1.0. Recorded API 37 content bounds include 360 × 671 pixels at 360 × 800, 800 × 232 in landscape, and 520 × 744 / 760 × 744 after rail allocation at 600/840 dp. Each platform's measurements are in its `runtime-api37-verified-dimensions.log` / `runtime-api36-final-dimensions.log`.

Both runs used this explicit isolated runner command, with the corresponding owned serial:

```powershell
adb -s emulator-5592 shell am instrument -w -r `
  -e class com.finnvek.knittools.ui.navigation.AdaptiveNavigationTest,com.finnvek.knittools.ui.navigation.AdaptiveActivityTest `
  -e adaptiveResizeAllowed true `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
```

Tested artifacts:

- Production debug APK, identical for both platforms and the final trial/browser regression: SHA-256 `C91EC73AD2131AA7FB4F37A9FAB2E598F3CC3C0E2A5D8378F197E762AE63C630`.
- API 37 test APK: `D75B1E8DFA065E816C3274EF21123F3DDE9DBC66A5CE3BD26F5FC0C76B9247F8`.
- Final API 36 test APK: `A92A6BB0DF63585CC5BFF253AA8DAA7DC536EE489CCB72C85D5485F8FC7F558E`. Only the IME test synchronization changed between these test APKs; production code and the other test methods are identical.

Earlier failed attempts remain as evidence: the Material navigation icon-description selector was replaced by the actual selectable labeled item; the existing splash test helper resolved the test-clock launch stall; two API 37 instrumentation starts were killed by the emulator's low-memory killer before tests began; and the density-only case exposed the production measurement defect fixed above. The first API 36 IME assertion ran before the OS inset animation reached Compose; a separate UI hierarchy/screenshot confirmed the correct settled layout, and the test now waits for the inset-driven layout before enforcing the same position assertion. These earlier attempts are not counted as passing runs.

Preservation: initial hashes cover 254 already-dirty/untracked input files. Only the eight scoped existing inputs listed in `preservation.json` changed; all other initial input hashes, including the trial implementation, camera flows, browser state holder and stability baseline, remain unchanged. The remaining foundation files were initially clean or newly added. `git diff --check` passed; branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`, and the empty index are unchanged. Temporary task-owned emulators were stopped; no background service is required by this implementation.

#### Remaining integration and acceptance boundaries

- All section 8.5 adaptations remain open except the minimal ToolScreenScaffold/browser/Tools token integration described above: project/list/detail sizing, counter short-height layout, gallery columns, wider Insights charts, PDF viewport/control integration, and other screen-specific changes.
- Dialogs, sheets, popups, and full-photo overlays that own separate windows require their own fold/inset integration and verification. The shared fold guarantee here covers the shell and its single content host; it is not a claim that every modal is hinge-aware.
- Full A01–A12 coverage remains open, including full screen-family matrices, list/selection/session/capture/PDF continuity, actual freeform/split-screen and physical/OEM folded-display transitions, and minimum-API compatibility. Synthetic WindowManager features prove the shared handling, not physical trifold behavior.
- The excluded 320 dp, 200% font-scale (including the combination), and TalkBack campaigns are not part of this task.
- Sections 5–7 and their progress notes are preserved. Their previously recorded open checks are not closed by this work. No release-candidate, optimized-runtime, publication, commit, push, or pull-request action is included.

### 8.10 Lists, tools, settings, notes and shared forms — 2026-10-20

**Status: this scoped screen-family task is complete within the verification boundaries below. Work package 2 and full adaptive integration remain open.** This follow-up is limited to the screen families below. It builds on section 8.9; the foundation record's open-screen list describes its earlier stopping point.

Implemented screen coverage:

- Projects, Library overview, My Yarn and Saved Patterns use the shared `ListDetail` policy (840 dp). Projects remains one list with unchanged ordering, folder ownership, card-to-overview routing and Continue hero actions. Creation and selection actions share the bounded screen region.
- Yarn-card and saved-pattern details, session/counter histories, needle sizes, abbreviations and chart-symbol reference rows explicitly select `ListDetail` in the existing `ToolScreenScaffold`. Detail section order, imagery height, typography, colors and overflow actions are preserved.
- Size Charts selects `Chart` (1200 dp), retaining its existing minimum column widths and horizontal scrolling. Categories and the finite reference table share vertical scrolling: the runtime 800 x 400 dp check exposed a 28 dp table viewport when the category selector was fixed above it. Compact windows still scroll wide tables rather than squeezing the columns.
- Settings and Notes use `Form` (600 dp). Tools home, all four calculators, Backup and Pro retain the foundation's existing reading-width policy and scrolling; no duplicate layout host or local width token was introduced for these already suitable screens.
- My Yarn, Saved Patterns and session history keep their saveable lazy-list state outside the loading/empty branch. Empty creation states scroll in short windows. Saved Patterns reserves creation-button space inside the list content instead of shrinking its viewport.
- Notes keeps text selection through a saveable `TextFieldValue`, synchronizes displayed text from the existing ViewModel and calls its writer only for text changes. Selection-only focus-teardown callbacks after lifecycle pause do not overwrite the saved selection. Repository merge and auto-save ownership remain unchanged. Editor insets are consumed before IME padding. Settings retains its open language picker through recreation.
- `FormSheet` uses the shared form width and retains its existing scrollable fields and Cancel/confirm row, so very short keyboard-open windows can bring either inputs or actions into view. Folder, language, linked-project, project-picker and Pro-prompt sheets select the same width policy while retaining their existing scroll/confirmation behavior. Existing Material confirmation and scrollable form dialogs retain their bounded sizing.

Focused validation and remaining limits:

- Raw evidence is retained locally in `reports/adaptive-screens-20261020/`, with initial input hashes, a task-only production diff, JVM XML results, APK hashes, runtime measurements and screenshots.
- **144 distinct JVM tests across 16 focused suites passed**, zero failures/errors/skips. The initial 143-test selection covered UI conventions, state retention, list/detail contracts, folder ownership, Gauge state, note merge/persistence and histories. The follow-up reran the affected notes/history/UI suites and added the Size Charts component contract (`validation.log`, `refined-validation.log`, `jvm-summary.json`).
- Final isolated `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:ktlintMainSourceSetCheck` and `:app:ktlintAndroidTestSourceSetCheck` passed (`final-build.log`); the final keyboard-open test refinement also passes test APK assembly and Android-test ktlint (`keyboard-open-build-verified.log`).
- The final `:app:detekt` run (`final-detekt.log`) reported the same two pre-existing findings as section 8.9: `MainActivity.kt:257` (`ComplexCondition`) and `RavelryBrowserState.kt:89` (`ReturnCount`). Both files retain their initial hashes. No suppression/baseline change was made. Full `lintDebug` was not rerun in this focused task; the earlier adaptive-foundation lint limitation remains open.
- **User-requested Detekt follow-up completed (2026-10-20):** reused one trial-notice eligibility condition for claiming/showing the notice and combined browser-history URL validation into one fallback guard. Existing notice behavior, URL restrictions and saved-state size limits are preserved; no suppression or baseline change was added. `:app:detekt` now reports **0 findings**; main-source ktlint, **19 focused JVM tests**, isolated debug/test APK assembly and **21/21 API 37 instrumentation tests** (`TrialNoticeRestorationTest`, `RavelryBrowserNavigationTest`, zero failures/skips) passed. Evidence is in `reports/detekt-followup-20261020/`. This resolves the two historical Detekt findings above, without adapting the Ravelry layout or closing full lint/adaptive integration work.
- `git diff --check` passed. Initial-file hashes confirm that changes are confined to these 18 production files and this plan; the adaptive instrumentation file is new. The index and other existing work remain unchanged.

Device verification:

- API 37 `AdaptiveScreenFamiliesTest`: **5/5 passed**, zero failures/skips, 163.738 seconds (`runtime-api37-stable.log`). It uses the actual MainActivity, production navigation and isolated Room fixtures. Initial test-only selectors and Android window/IME animation synchronization were corrected; failed exploratory logs are retained and are not counted as passes.
- The final strengthened form case additionally passed on API 37, **1/1**, 30.459 seconds (`keyboard-open-api37.log`): the software keyboard is confirmed open before resize/recreation, and the restored input and actions are checked above the settled IME afterward.
- API 36 runs the final complete `AdaptiveScreenFamiliesTest`: **5/5 passed**, zero failures/skips, 166.014 seconds (`runtime-api36.log`). The same production APK and final test APK are identified in `final-apk-hashes.json`.
- Existing `MainCounterStepSheetTest`: **30/30 passed**, 122.883 seconds at **800 x 400 dp**, font scale 1.0, English/Finnish/German and both themes (`form-regression-api37.log`). This verifies the shared FormSheet caller's focus, scrolling, validation, mutual exclusion, pending-save and failure/retry behavior without adapting the excluded Counter layout.
- Existing `NotesEditorFailureTest`: **3/3 passed**, 18.446 seconds at **1280 x 800 dp** (`notes-regression-api37.log`): retry after toolbar/system Back failures, draft restoration, concurrent-note merging and explicit discard. Repository/ViewModel failure semantics are preserved.

| Relevant matrix portion | Focused screen evidence; not full matrix acceptance |
| --- | --- |
| A01, A06-A07 and medium-width sampling | Projects, Library, My Yarn/Saved Patterns and their details, four reference screens, Tools and all four calculators, Settings, Backup and Pro at **360 x 800**, **700 x 900**, **1280 x 800**, **800 x 400**, **1600 x 900 dp**. These are measured logical windows at 160 dpi and font scale 1.0. |
| A02 | Short-height lists, empty creation states, all listed references/tools/settings destinations and histories remain scrollable. The last Size Charts row is reached with real vertical scrolling; wide columns retain their existing horizontal-scroll container. |
| A08 portions | Projects folder/filter and selection, My Yarn/Saved Patterns selections, and a meaningful visible list item survive resize and explicit Activity recreation. Session-history scroll anchor, needle search/selection and size-chart category also survive. Display overrides exercise current-window sizing; they are not physical split-screen/freeform gestures. |
| A12 portions | New Project, Add Yarn and Web Pattern drafts survive resize/recreation with the keyboard already open. Actual soft-keyboard visibility, complete editable-field bounds and Cancel/confirm bounds above the IME are checked after platform inset animations settle. |
| Notes and navigation | The 60-line notes draft and exact selected text range survive resize/recreation. Continue opens Counter; card content opens overview, with existing routes used to reach Notes and histories. No counter mutation or session start is introduced. |
| Theme/visual checks | Light/dark Projects, yarn detail, Size Charts and Settings captures reviewed. The same screen-width loop covers both themes. Forms and text retain 600 dp policies, list/detail bodies 840 dp and Size Charts 1200 dp; screenshot and measured-bounds evidence is saved locally. |

The API 37 run uses the task-owned read-only `Small_Phone` AVD (`emulator-5592`). Software-keyboard display with an emulator hardware keyboard is explicitly enabled for IME tests. The target app package is `com.finnvek.knittools.activepolicytest`; destructive fixture setup checks this package and requires `-e adaptiveResizeAllowed true`. The suite bypasses only the existing splash test gate, seeds local synthetic data and uses synthetic purchased access. It does not prove live billing, remote services, physical device/OEM behavior, process death, or release optimization. API 36 uses the task-owned read-only `medium_phone` AVD (`emulator-5594`). Both task-owned emulators were stopped after evidence capture; single-use Gradle daemons exit after their commands.


Project overview/Counter layouts, photos, PDF reader, Ravelry browser, Insights, startup/profiles, release CI and pseudolocales are not adapted by this task. Full A01–A12, the exact 800 x 1280 dp tablet-portrait sweep, API 29 final compatibility coverage, physical fold/freeform/density integration and separate-window hinge verification remain open. No 320 dp, 200% font-scale or TalkBack checks are included.

### 8.11 Counter and Project Overview — 2026-10-20

**Status: this scoped implementation and focused verification are complete within the boundaries below. Work package 2 and final whole-app adaptive integration remain open.** This record supersedes the Counter/Overview stopping points in 8.9–8.10, without closing other screen families or their acceptance checks.

Implemented changes:

- Project Overview centers its existing detail column with `ContentWidthPolicy.ListDetail` (840 dp). The existing 210 dp hero-photo height, 96 dp empty placeholder and three-thumbnail row remain; bounding the column also bounds imagery and text. Section order, typography, shared components and actions are preserved.
- Counter uses the shared `Form` width (600 dp), with the number/control group bounded by the existing 360 dp `CounterControlsMaxWidth`. The width constraint precedes `fillMaxWidth`. Compact-height content removes the artificial hero minimum height and uses the existing compact spacing tokens. The single keyed `LazyColumn` keeps session controls, content actions and extra counters reachable by scrolling.
- Counter lists and the notes preview sheet now scroll. Counter-owned sheets use the shared form width. Target, stitch, extra-counter and session dialogs reuse `ScrollableFormDialog`, so their fields and actions can scroll together in short or keyboard-open windows. Plain confirmations retain `ConfirmationDialog`.
- Counter and Overview opt into fold-aware modal windows at their existing navigation destinations. The shared foundation supplies its observed folding features to the existing sheet/dialog components. `AdaptiveModalWindow` constrains the separate Android window to one unobstructed pane, reapplies that geometry after Material window updates and restores normal geometry when the constraint ends. It does not create another content pane, NavHost or ViewModel. The Overview overflow menu aligns its end to its action anchor and bounds its height when a separating/occluding feature is present.
- Project identity, saveable drafts/list state, Projects-scoped `CounterViewModel`, Pro gates and repository ownership remain in their existing paths. No repository, session writer, counter mutation, schema, dependency or navigation-model changes were made.

Verification:

- Evidence is retained locally in `reports/adaptive-counter-20261020/`: starting status/hashes, before-file copies, task-only production diff, JVM XML, build/runtime logs, measurements, screenshots and APK hashes.
- **139 distinct JVM tests across 12 focused suites passed**, with zero failures/errors/skips. Coverage includes main-counter transactions, active sessions, step-size and ViewModel restoration/recovery, adaptive geometry, UI conventions and Counter/Overview contracts. See `build-and-unit.log`, `validation-3.log`, `overview-menu-validation.log` and `jvm-summary.json`.
- Isolated `:app:assembleDebug` and `:app:assembleDebugAndroidTest` passed. Main, Android-test and JVM-test source-set KtLint checks passed; `:app:detekt` reports **0 findings**. Final production verification is in `overview-menu-validation.log`; the final test-only synchronization refinement also passes Android-test KtLint and APK assembly in `ime-sync-test-build.log`. Full `lintDebug` was not rerun; its earlier recorded limitation remains open.
- Actual MainActivity/Room `AdaptiveCounterTest`: **5/5 passed on API 37**, 90.770 seconds (`runtime-api37-verified.log`), and **5/5 passed on API 36**, 138.275 seconds (`runtime-api36.log`), zero failures/skips. Both use the same production and final test APKs.
- API 37 regressions: **12/12** Overview navigation and selected normal-font session-dialog tests passed (`navigation-session-regression-api37.log`); **4/4** shared adaptive-navigation tests passed (`foundation-regression-api37.log`); **30/30** `MainCounterStepSheetTest` cases passed at **800 x 400 dp**, font scale 1.0, English/Finnish/German and both themes (`step-sheet-regression-api37.log`). No excluded font-scale or TalkBack cases were invoked.

| Applicable matrix portion | Verified scoped behavior |
| --- | --- |
| A01–A02, A05–A07 | Actual logical windows **360 x 800**, **800 x 400**, **800 x 1280**, **1280 x 800** and **1600 x 900 dp**, at 160 dpi/font scale 1.0. Overview/Counter columns are centered and capped at 840/600 dp, and the primary counter action group stays within 360 dp. Scroll reaches the content actions, last of 12 extra counters, last of 12 reminders, counters sheet and session action. Empty-photo and populated-photo Overview states are exercised; the populated hero is measured at 210 dp high. Representative light/dark screenshots were reviewed. |
| A03–A04 and A08 portions | Widths **599/600/601** and **839/840/841 dp**, tablet/landscape/phone transitions and explicit Activity recreation retain project ID, count, step size, counter history and a visible extra-counter scroll anchor. Overview scroll position also survives recreation. Counter/Overview round trips preserve navigation and Undo. |
| Session persistence | A real Start action creates the Room-owned session. A +5 counter action and its linked-counter effect survive every resize/recreation with the same session token/start/zone and unchanged completed-session count. Undo reverses the counter/link change. The Stop dialog survives recreation; Cancel leaves the session active. Only explicit Save creates one completed session, and another recreation does not duplicate it. Resize-only journeys leave counter history and sessions unchanged. |
| A09–A10 simulation | WindowManager test emissions supply **24 px vertical and horizontal separating features** at 1280 x 800 dp, plus unfolding. Counter content, action sheet, step sheet, extra-counter dialog and Overview menu stay outside the feature. Measured modal panes are **628 x 800** and **1280 x 388 px**. Drafts survive unfold/recreation; cancellation creates no counter or session mutation. |
| A12 | Step-size, reminder and project-details drafts survive resizing/recreation with the keyboard already open. Fields and Save/Cancel remain reachable. Step size is checked in Room before and after explicit Save; cancelled reminder/project drafts do not persist. Both fold orientations are exercised with the keyboard visible, including a tabletop pane entirely above the IME with correctly zero overlapping IME inset. |

The devices are task-owned read-only `Small_Phone` (`emulator-5592`, API 37) and `medium_phone` (`emulator-5594`, API 36). Tests use only `com.finnvek.knittools.activepolicytest`, synthetic Room fixtures and purchased-access test control, with the existing splash test gate. The destructive fixture setup verifies the isolated package and requires `-e adaptiveResizeAllowed true`. This is Activity recreation and UI/persistence evidence, not process-death, live billing or startup-timing certification.

The complete suite was run on each serial with:

```powershell
adb -s emulator-5592 shell am instrument -w -r `
  -e class com.finnvek.knittools.ui.navigation.AdaptiveCounterTest `
  -e adaptiveResizeAllowed true `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
```

Final SHA-256 values (`apk-hashes-final.json`):

- Production debug APK: `D637CC7F2A2E98DA2A4CFCA51C7C20CB167D8578358D2E6F1E5B1EBB7763D623`.
- Test APK: `0A6D308186CDD791892D1BB09631E2B9A49071C673326ACB1DB5C977FC5E8FA9`.

Exploratory failures are retained rather than counted as passes. They exposed the separate-window fold handling and Overview popup crossing fixed above. Test refinements corrected selectors/navigation synchronization, anchored the scroll test at an actual row start, and distinguished IME visibility from overlap in tabletop mode. Several early API 37 instrumentation processes were killed by the emulator's low-memory killer before tests began; settled runs passed. These attempts do not establish startup readiness.

Preservation: hashes cover **281 initially dirty/untracked files**; changes to those inputs are limited to the scoped Counter/shared-foundation integration, its source contract and this plan. Other initial work, including restoration, camera, previous screen adaptations and excluded families, remains unchanged. The branch, HEAD and empty index are unchanged. `git diff --check` passed. Task-owned emulators were stopped; no background service is needed.

Remaining acceptance boundaries:

- **Not verified here:** actual split-screen/freeform gestures, physical/OEM folding and landscape-natural foldable transitions, physical display-density changes, and API 29 final compatibility coverage. Logical window overrides and synthetic folding features cover only the recorded portions of A08–A11. These remaining checks must be completed in the mandatory final adaptive/compatibility integration task before release.
- Photo galleries, PDF reader, Ravelry browser, Insights and other remaining screen adaptations are **open mandatory tasks in this same release-readiness effort**, followed by the **mandatory final whole-app adaptive integration check**. Separate-window fold integration outside Counter/Overview remains open. None of these is a post-release deferral.
- The earlier restoration/camera acceptance limits, optimized release runtime, startup/profiles, release CI and other work-package requirements remain open as previously recorded. This task does not certify the release candidate or complete work package 2.
- 320 dp, 200% font scale (including their combination) and TalkBack checks remain **excluded by instruction** and were not rerun.

### 8.12 Photos, PDF reader and Ravelry layout implementation record — 2026-10-21

**Status: this scoped implementation and validation task is complete within the evidence boundaries below.** This task covers only these four section 8.5 families and their applicable 8.6–8.8 requirements. The implementations and evidence in 8.9–8.11 remain intact. Work package 2 and final whole-app acceptance remain open.

Implemented:

- All Photos and project galleries share `PhotoGridCells`: retain two compact columns and add columns before cells exceed 220 dp. Existing spacing, square crops, captions, ordering and stable photo-ID keys remain. Saveable grid state is owned above loading/empty branches, preserving the nearby visible row through asynchronous loading, column changes and recreation. Existing filter/selection ownership is preserved.
- Full-screen photos retain the opaque background and fit the image to the available safe viewport without changing aspect ratio. Zoom and bounded pan use saveable viewport-relative state; resizing recalculates fitted bounds, and double-tap resets the view. Close/share/delete controls remain outside the transformed image and inside safe-drawing insets. Sharing and photo storage ownership are unchanged.
- The reader retains unrestricted reading width. PDF zoom, normalized pan and page-relative scroll position survive size changes and restoration, with separate saveable state per document/page across asynchronous rendering. The page and overlay share one graphics transform; input uses its inverse. Resize cancellation discards an unfinished stroke instead of committing stale coordinates. Persisted annotations, bookmarks and document-owned guide/follow fields are never rewritten for a viewport change.
- The existing height-bounded, scrollable markup toolbar is preserved. Document/layer sheets and page-jump/photo-caption/document-rename forms use existing shared width and scrolling policies. Document action drafts retain IDs/text; two-point row calibration retains its first point and second input across recreation.
- Media routes opt into the existing `AdaptiveModalWindow`. Photo dialogs and nested confirmations, PDF sheets/forms, and the incoming-PDF sheet constrain their actual Android windows to an unobstructed pane. `AdaptiveDropdownMenu` similarly constrains popup windows for gallery filters and PDF/document/bookmark menus, including a popup inside a sheet. Ordinary callers retain the Material menu. The incoming-PDF host observes the existing fold mapping because it sits outside the navigation content host.
- Ravelry's existing `Unrestricted` WebView and `Form`-width explanatory text are reused without another width implementation. Browser production code, bounded-history restoration, new shared-link handling, checkout restrictions, cookies, download interception and cleanup are unchanged. Rendering/storage limits, schemas, repository ownership, dependencies and release configuration are unchanged.

Validation and evidence:

- Local evidence is retained in `reports/adaptive-media-20261021/`, including initial hashes/status, before-file copies, JVM XML, build/runtime logs and screenshots. The final production build and focused checks are recorded in `build-validation-11.log`.
- **82 distinct JVM tests in 11 suites passed**, zero failures/errors/skips: adaptive layout/grid/menu geometry, photo transforms, PDF transforms/source contracts, gallery contracts, UI conventions and Ravelry source/security contracts. Grid geometry also covers 599/600/601 and 839/840/841 dp boundaries and densities 1.0/2.5.
- Isolated debug and instrumentation APK assembly passed. Main, Android-test and JVM-test KtLint checks passed, and Detekt reports **0 findings**. Subsequent test-harness-only changes are checked separately. Full `lintDebug` is not rerun and remains open.
- API 37 controlled WebView regression: **12/12 passed**, including the five-window resize/recreation/history case, bounded save and fallback, new shared-link identity, blocked history and cleanup (`api37-browser.log`). WebView is `149.0.7827.5`.
- API 37 media regression: **13/13 passed** (`api37-regressions.log`): existing All Photos asynchronous restoration, stylus pressure, two bookmark-sheet cases, and nine PDF-export cases covering output, limits, cancellation, failure cleanup and source-file protection. The methods using excluded 320 dp/font-scale configurations were not invoked.
- API 37 new media coverage: **9/9 distinct cases passed** across `api37-media-verified-3.log` (six passing cases), `api37-media-rechecks.log` (calibration and incoming PDF), and `api37-photo-verified.log` (project gallery/viewer). Earlier failures remain recorded; the latter two runs correct only test-harness assumptions, with identical production code. This makes **34 distinct passing API 37 cases** with the browser and existing media regressions above.
- Final test APK assembly and Android-test KtLint passed in `test-build-15.log`. Production APK SHA-256: `0B9E84A81E400CD2DC602CE2676E7E5BF18D75D6B2C35D2269839DE34BB4F46F`; final test APK: `0C74C72D16BAD50AA2172C73861E809C1687E0B924358AA72089F0EAAA4C2915`. Earlier API 37 test-harness APK identities are retained in the `apk-hashes-*.json` evidence files; production APK identity is constant throughout the reported passing runtime runs.
- API 36 instrumentation: **21/21 passed**, zero failures/skips in one final run (`api36-media-browser.log`, 229.832 seconds): all nine new media cases and all twelve controlled browser cases. Device: read-only `medium_phone` / `emulator-5594`, WebView `155.0.8059.43`. API 37 used read-only `Small_Phone` / `emulator-5592`. API 36 used the final APK pair recorded above.

The scoped logical-window matrix is **360 × 800, 800 × 400, 800 × 1280, 1280 × 800 and 1600 × 900 dp**, at density 160 / font scale 1.0. Both API 37 and API 36 passed gallery filter/selection/thumbnail bounds and nearby-row continuity, photo-viewer controls and gestures, PDF document/page/guide/annotation retention and toolbar/menu/export reachability, and controlled browser history through resizing and Activity recreation. Independent viewport tests additionally check pixel-level photo zoom/pan/reset and saved-state restoration, page-relative PDF scroll/zoom, an actual rendered normalized annotation, inverse pointer coordinates, and cancellation of a stroke interrupted by resize.

Synthetic 24-pixel separating/occluding book and tabletop folds test actual focused-window bounds: photo viewer and nested delete confirmation; PDF overflow, bookmark sheet and its nested popup/add form, layers, page jump, documents and rename form; gallery filter popup; incoming-PDF sheet. Caption/bookmark/calibration drafts are exercised in short windows, including caption resize/recreation with the keyboard open. A successful SAF export-picker launch is followed by cancellation; actual export correctness and cleanup are covered by the separate nine exporter tests. Both light and dark PDF layouts and representative gallery/photo screenshots were inspected.

The runtime matrix uses synthetic local checkerboard images, 100 photo rows, two three-page PDFs with normalized annotations/guides/bookmarks, and controlled WebView pages. Tests target only `com.finnvek.knittools.activepolicytest` and require `-e adaptiveResizeAllowed true` before changing emulator window size. They do not use the personal app package, make purchases or contact live Ravelry. A partially expanded incoming-PDF sheet is expanded using a real swipe before its lower actions are checked. Gallery row continuity allows the normal row-boundary shift when column count changes; it must not reset to the beginning.

Exploratory attempts are retained separately and are not counted as passing evidence. Test fixes corrected navigation selectors, asynchronous render/data and keyboard synchronization, nested-popup Back dispatch, and an invalid import fixture (the production importer correctly rejects its own permanent files; the controlled source now lives in cache). A toolbar `performScrollTo` call targeted the action row's horizontal scroll owner; the test instead uses vertical user swipes, and an unnecessary experimental production change was reverted. A thread dump identified a test resource lookup blocked in `ActivityScenario.onActivity` / `Instrumentation.waitForIdleSync` during a Compose fling; application-context resource lookup removes that harness dependency. Gallery continuity compares retained photo identities rather than treating the semantics scroll value as a column-independent item index. Calibration snapshots are taken after its explicit guide-enable write, and the no-replay WindowManager test publisher supplies the current posture to the newly opened incoming host. API 37 System UI ANRs interrupted exploratory runs; API 36 also showed a System UI ANR after emulator boot, which was closed before its successful test run. No startup or emulator-stability claim follows from these passing functional tests.

Preservation/cleanup: all **1,249 existing inputs outside the 17 scoped existing-file changes** match their starting hashes; seven new Kotlin files belong to this task. This includes the previous Counter/Overview implementation and browser production code. `AGENTS.md` and `CODEX.md` remain identical and unchanged. Branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f` and the empty index are unchanged. `git diff --check` passed. Task-owned emulators and the diagnostic debugger connection were stopped; unrelated processes were not terminated. No commit, push, pull request, purchase, publication or deployment was performed.

Remaining boundaries:

- **W11 remains open:** controlled WebView history and local PDF import/cleanup do not prove a signed-in live Ravelry PDF download after restoration. Live login/cookie/redirect continuity is a separate check. Earlier T08 and unverified W04 portions are not silently closed.
- Actual split-screen/freeform gestures, physical/OEM folding and landscape-natural devices, physical display-density transitions, API 29 final compatibility and full lint remain open. Logical window overrides, saved-state tests and synthetic WindowManager features prove only the recorded scoped behavior, not those physical/platform checks or new process-death coverage.
- Insights, startup measurement, profiles, release CI, pseudolocales and the final whole-app adaptive verification remain mandatory separate tasks. The historical remaining-family lists in 8.10–8.11 describe those earlier tasks; this record supplies only the scoped media/browser follow-up.
- **Excluded and not run:** 320 dp, 200% font scale and TalkBack. Existing PDF regression methods with hardcoded 320 dp are excluded individually as well.

### 8.13 Insights adaptation implementation record — 2026-10-21

**Status: this scoped implementation and validation task is complete within the evidence boundaries below.** Only the Insights row in 8.5 and its applicable 8.6–8.8 requirements are addressed. Previous foundation, Counter/Overview, media/PDF and other work is preserved. The whole-app A01–A12 matrix and work package 2 acceptance remain open.

Implemented:

- The existing editorial list uses the shared `Chart` policy (1200 dp maximum). Hero, context, explanatory text, statistics, section headings and selected-day details use `Form` (600 dp); project rows, project mix and history links use `ListDetail` (840 dp). Charts retain the wider region and the same editorial left edge. The All Time context can wrap its localized range and project filter. No new cards, decorative surfaces, duplicate controls, embedded session-history list or screen structure were added.
- Range and project filter live in the existing ViewModel's `SavedStateHandle`. Its initial loading state includes restored filters, and a restored project is cleared only after a real project emission proves it invalid. Saved-state writes execute on the main collector; existing data aggregation remains on the injected dispatcher.
- Chart and project-fabric selections are saveable date identities rather than transient indexes. Validation waits for authoritative loaded data; unavailable identities and changed filters discard obsolete selections. The saveable lazy-list state stays above the loading branch, and stable section/project keys preserve a meaningful visible row. The skeleton cannot clamp a restored list position to its own smaller item count.
- Chart gestures use current callbacks and current plot dimensions, restarting safely when dimensions or buckets change. Axis labels use measured widths, omit colliding/oversized ticks, and stay aligned with the Canvas's physical coordinates in RTL. Monthly labels include the year when the chart spans years. Bucket values, grouping, range boundaries and source data are unchanged.
- The range menu reuses `AdaptiveDropdownMenu`; the project filter already uses that shared component. The existing completion-history sheet uses `AdaptiveModalWindow` and saveable expansion state. The shared content host and actual popup/sheet windows respect the unobstructed pane; ordinary short windows retain vertical scrolling and the existing completion timeline's horizontal scrolling.

Validation and evidence:

- Evidence: `reports/adaptive-insights-20261021/` contains initial status/hashes, before-file copies, scoped build/check logs, JVM XML, runtime logs, measured dimensions, APK hashes and screenshots. No personal app database, live backend or real purchase was used.
- **228 JVM tests in 31 suites passed**, zero failures/errors/skips (`validation-4.log`, `jvm-summary.json`, `jvm-xml/`). Selection-coordinate and saved-filter restoration coverage was added; existing Insights, repository, UI-consistency, contrast and shared-layout tests were reused. They cover session zones/ranges, completion events, neutral counter units, loading, empty ranges, cancellation and Pro behavior. The owning command was `:app:testDebugUnitTest` with filters for `ui.screens.insights.*`, `repository.CounterRepositoryInsightsTest`, `ui.UiConsistencySourceTest`, `ui.theme.InsightsContrastTest` and `ui.layout.*` under `com.finnvek.knittools`.
- Isolated debug and instrumentation APK assembly passed. Main, JVM-test and Android-test KtLint passed; Detekt reports **0 findings** (`validation-4.log`, `detekt-final.xml`). The final Android-test-only correction was rebuilt and checked in `test-build-7.log`. Commands used `--no-daemon` and `-Pknittools.isolatedTest=activepolicy`; scoped formatting touched only this task's files. Full lint was not rerun.
- API 37: **11/11 instrumentation cases passed**, zero failures/skips (`api37-final-7.log`, 106.517 seconds), on read-only `Small_Phone` / `emulator-5592`.
- API 36: **the same 11/11 cases passed**, zero failures/skips (`api36-verified-8.log`, 79.547 seconds), on read-only `medium_phone` / `emulator-5594` with the identical APK pair.
- Production APK SHA-256: `E955033AB23AC8C09126EE37196036B35CECF0C93BB6DFDFBC82C67264880764`; final test APK: `5D891413E6376091CACBAA365D5EE4AD196F0A7F180174BED97CD52B512C8B1F` (`apk-hashes-final.json`). Production code is identical across the final passing runtime runs.

The instrumentation set comprises four `AdaptiveInsightsContentTest` cases, two `AdaptiveInsightsTest` Activity cases, the existing counter-unit and relative-month cases, and three existing completion-section cases (`emptySelectedProjectOmitsSection`, `emptyRangeOmitsSection`, `moreOpensOlderEventsAndTimelineExposesZeroCounts`). Completion tests with excluded width/font configurations were not selected. Each run uses explicit `am instrument -w -r -e class ... -e adaptiveResizeAllowed true` against `com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner`.

The measured logical Activity windows are **360 × 800, 800 × 400, 800 × 1280 and 1280 × 800 dp**, at density 160 / font scale 1.0 on both platforms. Week, month and All Time chart touches select different expected regions after resize; selection readouts survive Activity recreation. Project filter, range and a nearby project row survive the same transitions; session-history navigation remains reachable. The before/after SQL snapshots of `counter_projects`, `sessions`, `project_completions` and `active_sessions` are equal. The fixture has 25 synthetic projects, 125 fixed-duration UTC sessions at deterministic day offsets from the run's date, and five completion events; the active-session table starts empty. This is not an active-session lifecycle or process-death test.

Independent content tests exercise empty/loading, one-project sparse data, empty selected ranges, the free chart gate, saved-state restoration through loading, removed selections, and project-fabric touch mapping. Both themes use compact, short-landscape and tablet content bounds. Axis layout is measured at 328/568/1168 dp plot widths (328 is the chart inside a 360 dp screen, not a 320 dp device check), across English, Finnish, German and Brazilian Portuguese and all three ranges. Visible placed labels must remain inside the plot without overlap or truncation; multi-year labels retain their years. An injected RTL layout direction checks physical label order without configuring pseudolocales.

German light/dark Activity views and long synthetic project names are exercised in every window. The project menu must scroll its offscreen target into view before selection. Synthetic 24-pixel separating book/tabletop features check the actual focused bounds of the range menu, project menu and completion-history sheet, including reaching the oldest completion. Representative compact, landscape, tablet and folded screenshots were inspected. These are simulated features, not physical foldable evidence.

Exploratory failures are retained and are not counted as passing evidence. Harness corrections fixed a fixture DAO call, selection of unplaced axis candidates, test-class size and dispatcher teardown, ambiguous text selectors, and an attempted click on an offscreen project-menu row. API 36 initially displayed a System UI ANR from `com.android.systemui/.keyguard.KeyguardService`, which stole the focus required by the fold check. One retry was deliberately stopped while clearing that emulator dialog; after confirming it was gone, the unchanged APKs passed the full final run. No production workaround, relaxed assertion or emulator-stability claim follows from these attempts.

Preservation: **1,267 baseline-hashed files outside the six scoped existing-file changes** retain their initial hashes; three new Kotlin test files belong to this task. `AGENTS.md`/`CODEX.md`, prior adaptive/media/PDF changes, schemas, repositories, resources and dependencies are unchanged. Branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f` and the empty index are preserved. `git diff --check` passed. Task-owned emulators were shut down; no unrelated processes were stopped. No commit, push, pull request, publication or deployment was performed.

Remaining boundaries:

- Earlier **T08, unverified W04 configurations and live Ravelry W11** remain open, including real login/cookie/redirect/download continuity. No live Ravelry check ran here.
- Physical/OEM folding, landscape-natural devices, actual split-screen/freeform operation, physical display-density changes, API 29 final compatibility and full lint remain open. Saved-state/Activity tests do not add process-death guarantees. Earlier unresolved release-surface/resource-parity and WebView findings are not resolved by this task.
- The signing incident now has a concrete owner action in [INC-SIGN-01](#41-inc-sign-01-signing-password-disclosure-assessment--2026-10-22). Global/user Gradle property contents, environment-variable values, signing credentials and secret-file contents were not printed or reproduced.
- Final whole-app adaptive integration, pseudolocale setup, release CI, startup measurements and profiles were not started. Historical remaining-family lists in 8.9–8.12 describe those earlier snapshots; this record closes only Insights within its stated scope.
- **Excluded and not run:** 320 dp, 200% font scale and TalkBack. No whole-app matrix completion is claimed.

### 8.14 Whole-app adaptive integration record — 2026-10-21

**Status: complete within the isolated-app/emulator boundaries below.** A01–A12, the section 8 acceptance criteria, locally available W04 configurations and minimum-API startup/navigation are verified. No required locally executable adaptive check remains blocked. This record supersedes the earlier records' remaining-integration lists only for checks explicitly completed here. Physical/OEM behavior and optimized release runtime are not inferred. Raw evidence is in `reports/adaptive-integration-20261021/`.

#### Scope and confirmed correction

The starting application/test APK hashes matched 8.13. The shared foundation and all screen-family implementations were inspected together with their existing reports. Their unchanged calculation, repository, coordinate-transform and screen-specific tests remain applicable within their original boundaries; the current runs add application-level navigation, modal, configuration and platform integration evidence.

The new vertical/horizontal hinge regression failed on the New Project sheet before the correction (`hinge-before-3.log`). Adaptive modal-window handling had been enabled only on selected navigation routes, leaving Project creation, Add Yarn and Web Pattern forms outside the fold-aware context. `MainActivity` now provides the existing `LocalAdaptiveModalWindow` and live `LocalWindowFolds` at the application content root. Existing modal geometry and navigation/state owners are reused. The same three-form test then passed for both separating hinge orientations. There are no schema, dependency, product-flow or calculation changes.

The integration tests additionally cover the 800 × 1280 dp screen-family sweep, a live counter session across all five tabs and English → Finnish/dark-theme recreation, real platform checkpoints for a keyboard draft/counter/browser, and PDF display-density changes. The platform checkpoint helper is instrumentation-only, opt-in and restricted to `com.finnvek.knittools.activepolicytest`; its host driver is retained in the evidence directory.

#### Matrix and continuity coverage

All configurations use font scale **1.0**. The application-window matrix uses **360 × 800, 800 × 400, 800 × 1280, 1280 × 800 and 1600 × 900 dp**, with additional focused 599/600/601 and 839/840/841 dp transitions. Insets and navigation consume part of these window dimensions; content is not reported as the full window. `api37-dimensions.log`, `api37-final-dimensions.log` and subsequent platform logs record the measured configuration and content bounds.

| Row | Current evidence and boundary |
| --- | --- |
| A01 | API 37 and API 36 verified: all five tabs; Projects/Overview/Counter, Library/Yarn/Patterns, photos/PDF/browser, Insights, Tools/calculators/tables, Settings/Backup/Pro, notes/history and important forms/details. |
| A02 | API 37 and API 36 verified at 800 × 400 dp: counter/action controls, scrollable forms, keyboard, media controls and browser. |
| A03 | API 37 and API 36 verified at 599/600/601 dp: one navigation host, graph owner and tab back stacks; actual counter/session/history continuity across recreation. |
| A04 | API 37 and API 36 verified at 839/840/841 dp: measured layout boundaries, counter scroll anchor and unchanged persisted state. |
| A05 | API 37 and API 36 verified at 800 × 1280 dp: all screen families, populated/empty presentation and modal paths. |
| A06 | API 37 and API 36 verified at 1280 × 800 dp: bounded forms/lists, charts, galleries, PDF, browser and separating modal windows. |
| A07 | API 37 and API 36 verified at 1600 × 900 dp: content-width policies, media cell limits, charts and browser. Screenshots supplement the bounds assertions. |
| A08 | Actual API 37 freeform browser/history/Back passed at 560 × 800, 700 × 800, 1100 × 800 and fullscreen 1600 × 1000 dp. `dumpsys activity` confirms `mode=freeform` and the application confirms `isInMultiWindowMode=true`; these are actual task resizes, not just display overrides. The final keyboard-draft/Back and counter-session/Undo journeys passed on API 36; the keyboard journey also passed on API 37. |
| A09 | Verified on the landscape-natural Pixel Fold emulator: actual CLOSED → OPENED → HALF_OPENED → CLOSED, with 411 × 797 dp outer and 841 × 701 dp inner windows at density 420. Keyboard draft/tab stacks, counter/session/Undo and browser/history/Back all pass. Physical/OEM behavior remains outside this emulator evidence. |
| A10 | API 37 and API 36 synthetic vertical and horizontal separating-feature cases pass for the shared host and all affected modal families, including the new creation forms. Harmless crease/unfold behavior remains covered. Actual API 37 book-like HALF_OPENED and rotated tabletop transitions also passed; tabletop measures 701 × 841 dp and the counter content occupies the unobstructed upper pane. |
| A11 | API 37 and API 36 real emulator density changes 160 → 240 → 200 → 160 dpi at fixed 1280 × 800 px measured **1280 × 800 → 853 × 533 → 1024 × 640 → 1280 × 800 dp**. PDF document/page/guides and annotation rows stay unchanged, controls remain reachable, and settled screenshots were inspected. Existing rendered-pixel/normalized-transform tests also passed. This is emulator display-density evidence, not a physical display switch. |
| A12 | API 37 and API 36 keyboard-open resizing/recreation pass for calculator, project/yarn/web forms, counter step/reminder/details and notes. Actual freeform keyboard/Back passes on both API levels; folded/inner/half-open/outer keyboard continuity passes on the API 37 fold emulator. |

| Section 8.7 state | Integration evidence |
| --- | --- |
| Active project/counter/Undo/work session | Counter test crosses every breakpoint and normal window, recreates, visits all five tabs, changes language/theme, restores tab back stacks, and verifies Room count, history, linked-counter value, stable session token/anchors and completed-session count. Undo remains usable; finalization writes one additional session only. |
| Folder/filter/selection/list position | Screen-family test retains folder context, selected projects and meaningful visible rows; media tests retain project filter, selected photo IDs and nearby grid rows; Insights retains range/project filter/chart selection and scroll without database writes. |
| Forms/validation/notes | Drafts survive configuration/recreation, explicit Save/Cancel semantics are preserved, and notes/history navigation returns to the correct project. Keyboard action bounds are asserted. |
| PDF/document/page/guides/bookmarks | Media tests retain the active secondary document, page, document-owned guides, bookmarks and annotation data through windows/recreation/density changes; calibration's first point and incoming-PDF confirmation survive. |
| PDF gestures/photo viewer | Independent production-viewport tests verify rendered marker alignment, inverse coordinates, page-relative scroll/zoom restoration and cancellation of an interrupted stroke without committing it. Photo zoom/pan/reset and saved viewport restoration pass. |
| Ravelry | The 12 controlled WebView cases pass, including all five windows/recreation, Back history, bounded save/fallback and blocked navigation. Actual freeform history/Back also passes. Earlier W02 process-death evidence remains bounded as recorded; live W11 is still open. |
| Trial notice | All 10 existing restoration/claim/launch-precedence regressions pass. Synthetic entitlement controls do not close T08. |
| Pending camera/external results | All 20 ownership and four external-camera cases pass, including actual emulator camera results, original-project ownership after recreation/navigation and cancellation/cleanup. |
| Incoming PDF/share | Existing share restoration/queue tests and incoming-PDF media tests pass: confirmation is retained, cancellation cleans the copy, and no duplicate import or owner reassignment occurs. |
| Theme/language | Both themes are exercised across representative families; the live counter/tab journey preserves state through English → Finnish plus dark-theme recreation. Startup locale/theme source contracts pass; no frame-timing or process-death claim is added. |

#### Commands and results

Every runtime run targets the isolated package with synthetic local data; INTERNET and BILLING remain absent. No personal database or real purchase is used. The direct Gradle validation was:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 `
  -I reports/adaptive-integration-20261021/format-scoped.gradle `
  :app:ktlintMainSourceSetFormat :app:assembleDebug :app:assembleDebugAndroidTest `
  :app:ktlintCheck :app:detekt :app:testDebugUnitTest `
  --tests 'com.finnvek.knittools.ui.layout.*' `
  --tests 'com.finnvek.knittools.ui.UiConsistencySourceTest' `
  --tests 'com.finnvek.knittools.AppStartupSourceTest' `
  --tests 'com.finnvek.knittools.RavelryBrowserSourceTest' `
  '-Pknittools.isolatedTest=activepolicy'
```

`validation-2.log` passed: both debug APK builds, full app KtLint, Detekt with **zero findings**, and **31 JVM tests, zero failures/errors/skips** (`jvm-xml/`, `jvm-summary.json`). The ignored formatter init script restricts formatting to this task's files. Subsequent test-only changes passed instrumentation APK assembly and the complete Android-test KtLint check in `test-build-4.log`, `test-build-5.log` and the final `test-build-6.log` using `:app:ktlintAndroidTestSourceSetFormat :app:assembleDebugAndroidTest :app:ktlintAndroidTestSourceSetCheck` with the same init script and isolated property.

Full `:app:lintDebug` passed in **14m 27s** (`lint-debug-final.log`, copied `lint-results-debug.xml`/`.txt`), using `--no-daemon --max-workers=1 '-Pknittools.isolatedTest=activepolicy'`, with **0 errors and 10 warnings**. Production, Android-test and JVM-test analysis completed; no source subset or rule override was used. All four warned source files match their starting hashes:

- Four reports of `MissingOnRenderProcessGone` are the already tracked WebView renderer-recovery warning; this task does not add that unrelated feature.
- Three `RtlHardcoded` reports refer to `Gravity.LEFT` in `AdaptiveModalWindow`. Its `x` is an absolute physical pane coordinate, already selected with layout direction taken into account; changing this to relative `START` would mirror that offset again. The existing physical placement is retained without suppression.
- Two `VisibleForTests` reports flag the default `SavedStateHandle()` argument in `InsightsViewModel`. Inspection of the generated Hilt factory confirms production passes `savedStateHandleProvider.get()` explicitly; the default does not replace the production saved-state owner. Actual Insights filter/selection restoration passed on both APIs. The advisory remains visible.
- One `ModifierParameter` report flags the existing parameter order in `ScrollableFormDialog`; it is an API-style advisory, not a reproduced adaptive or lifecycle defect. No unrelated signature cleanup is added.

Full `:app:lintRelease` also passed in **4m 30s**, using the same direct Gradle options and isolated property (`lint-release-final.log`, copied `lint-results-release.xml`/`.txt`). It reports **0 errors and the same 10 warnings**, with no suppression or baseline change. `lint-summary.json` records both variants and current report timestamps. These are full lint analyses of the isolated variants, not optimized release runtime or release-candidate certification.

API 37 has **29 distinct passing integration cases** across `api37-integration.log`, `api37-synthetic-window.log` and `api37-final-rechecks.log`, plus **48 passing existing regressions** in `api37-regressions.log`. API 36 passes **24/24 integration cases**, zero failures/skips (`api36-integration.log`, 392.575 seconds), covering every real-Activity screen family plus the controlled browser matrix. The six-case synthetic host/viewport run requires a 1600 × 1200 dp host so its explicitly larger test nodes are not physically clipped. All real-Activity matrix tests resize their own window and are run separately from that host fixture.

Actual platform runs also pass, with no skips:

- API 37 freeform: controlled browser/history/Back (`api37-freeform-browser-runtime.log`) and keyboard draft/tab stacks/Back (`api37-freeform-form-settled-runtime.log`).
- API 36 freeform: keyboard draft/tab stacks/Back and counter/session/history/Undo (`api36-freeform-form-runtime.log`, `api36-freeform-counter-runtime.log`).
- API 37 fold emulator: the same keyboard, counter and browser journeys through CLOSED/OPENED/HALF_OPENED/CLOSED (`api37-fold-{form,counter,browser}-runtime.log`).
- API 37 rotated tabletop: counter/session/Undo and controlled browser/history/Back (`api37-tabletop-{counter,browser}-runtime.log`). The driver uses `emu fold`, `emu unfold`, `emu sensor set hinge-angle0 90` and, for tabletop, `wm user-rotation lock 1`, restoring rotation 0 when closing. `*-platform.log`, per-step `*-activities.txt` and `api37-fold-dimensions.log` confirm the actual device states, rotations and window/content bounds.

API 37 normal-window runs use read-only `Small_Phone` / `emulator-5584`; API 36 uses read-only `medium_phone` / `emulator-5584` in a separate session. Fold runs and the final API 37 freeform draft run use the task-created `KnitToolsAdaptiveFold` / `emulator-5586`, based on the Pixel Fold profile and the same API 37 image. The fold runs reset all size/density overrides and use the profile's physical display regions at 420 dpi. `fold-avd-config.ini` records its landscape-natural inner display. The API 36 dimensions and screenshots are in `api36-dimensions.log`, `api36-platform-dimensions.log`, `api36-screens/` and `api36-media/`.

Minimum-supported **API 29** startup/navigation passes **1/1**, zero failures/skips, in **10.456 seconds** (`api29-startup-navigation-final.log`). The test visits all five tabs, enters Cast On input `29`, restores the Tools back stack after visiting Library, retains the input through Activity recreation and returns to Tools with Back. The task-created `KnitToolsAdaptiveApi29` / `emulator-5594` uses the `android-29;google_apis;x86_64` image and small-phone profile. Its 360 × 800 px display override at 160 dpi/font scale 1.0 yields Android 10's measured **360 × 728 dp configuration** after system-bar exclusion; the adaptive content bounds are `(0,24)–(360,672)` px (`api29-dimensions-final.log`). These measurements are distinguished from the full display size. The earlier normal launcher startup and rendered five-tab UI are also recorded in `api29-first-launch.log` and `api29-first-launch-layout.json`; these are functional checks, not startup benchmarks.

Isolated application APK SHA-256 stays **D1026D5624677DA607AE04D6CA44A329CA3C76DE01794238243FDEA0FF3AD48D** throughout these passing runs. The final API 29 test APK is **80C0B09A7D8F0C4DBEE792DFC1EC5242994C39FE44B7E89C3E87FCDC7D83128A** (`apk-hashes-final.json`); earlier test-only iteration hashes are preserved in `apk-hashes-*.json`. The last test-only changes put API 29 locale setup on the main dispatcher and wait for startup navigation readiness; they do not change production behavior.

Runtime commands use `adb -s emulator-5584 install -r` for both APKs and:

```powershell
$classes = (Get-Content reports/adaptive-integration-20261021/integration-classes.txt -Raw).Trim()
adb -s emulator-5584 shell am instrument -w -r -e adaptiveResizeAllowed true `
  -e class $classes com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
# The regression and API 36 class selections are stored alongside this file.
& reports/adaptive-integration-20261021/platform-driver.ps1 -Serial emulator-5584 `
  -Mode freeform -Test 'com.finnvek.knittools.ui.screens.ravelry.RavelryBrowserNavigationTest#platformTransitionsPreserveControlledHistoryAndBack' `
  -Name api37-freeform-browser
adb -s emulator-5594 shell am instrument -w -r `
  -e class 'com.finnvek.knittools.ui.navigation.AdaptiveActivityTest#startupAndAllFiveTabsKeepCalculatorBackStack' `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
```

The driver records each task mode/bounds and device state before releasing the instrumentation checkpoint. It invokes `am start --windowingMode 5` with the same launcher action/category, `am task resize` for three real freeform sizes, and `--windowingMode 1` for fullscreen. A display override alone is never counted as A08.

Exploratory failures remain in the evidence directory and are not passing results: low-memory emulator process kills before tests started, an interrupted initial lint run, a missing shell quote in one Gradle property, synthetic views clipped by a compact host, stale fixture data before a trial suite, test selectors/startup readiness, ActivityScenario launcher identity, IME synchronization and API 29 test locale setup off the main thread. Production startup and Settings already use the main dispatcher for locale application. The confirmed product defect is the missing root modal context described above. No assertion, lint rule, security restriction or baseline was weakened.

Final preservation checks compare all **1,277** starting files: **1,270 are byte-for-byte unchanged**; the seven changed existing files are `MainActivity.kt`, five instrumentation test files and this plan. The only new nonignored file is the instrumentation checkpoint helper. `preservation-final.json` records no unexpected changes, the unchanged starting branch/HEAD and an empty index. `AGENTS.md` and `CODEX.md` remain identical and unchanged. `git -c core.safecrlf=false diff --check` passes. All temporary emulator sessions and Gradle processes started for this task have ended. Unrelated processes were left untouched. No temporary server or watcher remains from this task.

**Unchanged release boundaries:** T08, signed-in live Ravelry W11, the separate INC-SIGN-01 owner action (4.1) and unrelated release-surface/resource-parity/WebView findings remain open. This task adds no process-death, live-service, physical/OEM or release-artifact guarantee. Global/user Gradle properties, environment-variable values, signing credentials and secret-file contents were not printed. Pseudolocales, release CI, startup benchmarks, profiles, 320 dp, 200% font scale and TalkBack are excluded and were not run. No commit, push, pull request, publication or deployment is authorized or performed.

## 9. Work package 3: resources, localization, and related contracts

### 9.1 Pseudolocales

1. Enable `isPseudoLocalesEnabled = true` for debug only.
2. Confirm `en-XA` and `ar-XB` are available in the intended debug test environment.
3. Do not add pseudolocales to the real user-facing language picker or advertised supported-language list.
4. If the app's explicit locale configuration prevents selecting them through normal settings, use a debug/test-only locale path; do not pollute release resources.
5. Exercise representative new/changed layouts with:
   - expanded pseudo-English text;
   - RTL pseudolocale mirroring and mixed-direction numbers, URLs, and project names.
6. Confirm no release pseudolocale resources or debug locale controls are accidentally packaged.

RTL pseudolocale testing is useful even without an Arabic translation: it checks the app's declared RTL behavior and start/end layout assumptions. It is not a substitute for a linguistic translation audit. [Pseudolocales](https://developer.android.com/guide/topics/resources/pseudolocales)

### 9.2 Resource validation

- Reuse the repository's localization contract tests.
- Validate new or changed resources across all currently supported locales.
- Check default resource availability, duplicate keys, format-argument index/type agreement, plural usage, escaping, and `translatable="false"` handling.
- Confirm current language definitions, `locales_config.xml`, and actual locale resource directories agree, including the ongoing Brazilian Portuguese work.
- Preserve canonical numeric values and locale-aware display formatting.
- Use start/end alignment and padding; mirror directional navigation icons appropriately.
- Check long labels on new rail items, buttons, chart labels, and sheets.
- Keep plain URLs/identifiers from receiving unintended locale casing or numeric formatting.
- Do not retranslate existing content simply because a layout token changes.
- Do not carry forward earlier numeric resource-count claims as current evidence without rerunning the relevant check.

Existing localization work and `LANGUAGE_AUDIT.md` are separate concurrent inputs. Respect their ownership and unresolved findings.

### 9.3 Manifest, navigation, and DI verification

Inspect the merged release manifest and verify:

- camera permission removal;
- no unintended exported components;
- correct PDF/text share intent filters;
- FileProvider authority and narrow paths;
- cleartext remains disabled;
- locale/RTL configuration;
- intended orientation/resizability behavior;
- debug-only metadata does not leak into release.

Run focused navigation regression coverage for:

- tab reselection and per-tab restoration;
- Projects → Overview → Counter and reciprocal navigation;
- new shared Ravelry links;
- incoming PDF handling;
- external counter/widget launches;
- Back/Up from media/detail screens.

Keep Hilt construction, graph-scoped ViewModels, repository ownership, and dispatcher injection unchanged unless a concrete defect introduced by this work requires a local correction.

### 9.4 Resources and localization implementation record — 2026-10-21

**Scope:** section 9 only, on the existing `codex/audit-korjaukset-20261018` branch at starting HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. The dirty checkout, including section 8.14 and the independent language work, was captured before editing. Evidence is in ignored `reports/resources-localization-20261021/`. The section 8.14 adaptive, fold/freeform, restoration and controlled WebView evidence is reused within its recorded limits; this is not another full adaptive campaign. No 320 dp, 200% font scale, combined configuration or TalkBack check ran.

#### Changes and resource contracts

- Debug now enables generated pseudolocales with `isPseudoLocalesEnabled = true`. A debug-only `res/xml/locales_config.xml` retains the 12 real locales and adds only `en-XA` and `ar-XB`, allowing the platform app-locale API to select them. The main locale configuration, `AppLanguage` picker, advertised languages and production translations are unchanged.
- Reproduced the previously recorded locale-parity failure before fixing it: `release-surface-before.log` has **22 passing / 2 failing** self-tests. `Convert-ValuesDirectoryToLocale` incorrectly returned `pt-rBR` for `values-pt-rBR`; it now returns the BCP 47 tag `pt-BR`. Added a regional-locale mutation proving that removing the `pt-BR` declaration still fails the guard. `release-surface-after.log` has **25 passing / 0 failing** self-tests; the current source scan has **13 passing / 0 warnings / 0 failures**.
- Extended `ReferenceResourceLocalizationTest` to compare actual language directories and debug/main locale declarations, reject duplicate locale tags, and check format-argument indexes/types in every plural variant and string alias. Existing tests remain responsible for duplicate resource keys, complete translatable-key parity, matching resource types, alias validity, `other` plural quantities and `translatable="false"` exclusions. Resource compilation checks Android escaping/syntax.
- Fresh inventory: all **12 locales have 1,337 translatable keys**, comprising **1,306 strings + 31 plurals**. The two-form locales have **1,368 variants**, and fr/it/es/pt/pt-BR have **1,399 variants**. Two default nontranslatable strings remain excluded. `resource-inventory.json` and the JVM reports record the current counts; older campaign counts are not reused as current evidence.
- `LANGUAGE_AUDIT.md` and its ownership were inspected and left unchanged. Its language-content acceptance, supplementary rendering limits and native-review status remain separate. No translation campaign, new language or linguistic approval is implied by these structural checks.
- Existing numeric/measurement/locale-formatting and URL tests passed. Canonical measurement values and raw numeric form input remain separate from display formatting. Source inspection confirms URL normalization only applies the established invariant casing to scheme/host, preserving path/query case. Directional Back icons and logical padding remain in place. The absolute pane positioning in `AdaptiveModalWindow` and physical canvas/axis coordinates in `InsightsChart` are intentional and were not mechanically mirrored.

#### Validation commands and ownership

All Gradle runs used `--no-daemon --max-workers=1`; the configured Gradle toolchain is Eclipse Temurin **17**. The installed debug APK is the existing isolated `activepolicytest` variant with synthetic local data; it is not a production release artifact. The disposable `Small_Phone` emulator uses API **37**, serial `emulator-5584`, density **320 dpi**, normal font scale **1.0**. The task-owned emulator is read-only and started without loading or saving snapshots.

```powershell
./tools/release-surface-test.ps1
./tools/release-surface.ps1

./gradlew.bat --no-daemon --max-workers=1 `
  :app:assembleDebug :app:assembleDebugAndroidTest :app:ktlintCheck :app:detekt `
  :app:testDebugUnitTest `
  --tests '*Localization*' --tests '*Locale*Test' --tests '*CountPluralResourcesSourceTest' `
  --tests '*AppLanguage*Test' --tests '*InstructionHintLocalizationTest' `
  --tests '*ReleaseSigningSourceTest' --tests '*RavelryLinksTest' `
  --tests '*RavelryShareTargetSourceTest' --tests '*AppStartupSourceTest' `
  --tests '*WebPatternUrlTest' --tests '*Measurement*Test' `
  '-Pknittools.isolatedTest=activepolicy'

adb -s emulator-5584 shell am instrument -w -r `
  -e class com.finnvek.knittools.ui.navigation.PseudolocaleNavigationTest `
  -e pseudoResizeAllowed true `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner

# Exact existing-test selection is recorded in navigation-classes.txt.
$navigationClasses = (Get-Content reports/resources-localization-20261021/navigation-classes.txt -Raw).Trim()
adb -s emulator-5584 shell am instrument -w -r `
  -e class $navigationClasses -e adaptiveResizeAllowed true `
  com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner

# Production configuration: no isolated-test property, signing bypass or artifact upload.
./gradlew.bat --no-daemon --max-workers=1 `
  :app:verifyGoogleServicesJson :app:processReleaseMainManifest :app:processReleaseResources
./gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease --dry-run
./gradlew.bat --no-daemon --max-workers=1 :app:lintDebug :app:lintRelease
```

The first combined build also ran `:app:ktlintTestSourceSetFormat` and `:app:ktlintAndroidTestSourceSetFormat` through the recorded `format-scoped.gradle` init script, restricted to the two test files owned here. Final test-driver changes were rebuilt with `:app:assembleDebugAndroidTest` and `:app:ktlintAndroidTestSourceSetCheck` using the same isolated property and scoped formatter (`test-build-6.log`). Unrelated files were not formatted.

- `validation-2.log`: debug/app-test builds, complete ktlint and Detekt passed; **115 JVM tests in 25 classes passed**, zero failures/errors/skips (`jvm-summary.json`, `jvm-xml/`). This covers resource/locale contracts, numeric formatting/parsing, measurements, Brazilian Portuguese integration, Ravelry/ordinary URL boundaries and release-signing source contracts. These are not release runtime checks.
- `navigation-runtime-2.log`: **15 existing instrumentation tests passed**, zero failures/skips, **60.276 seconds**. Coverage includes Projects/Overview/Counter reciprocal navigation, saved text-share requests, queued Ravelry shares, new and repeated shared URLs with recreation, incoming PDF/media Back, notes/history/reference recreation, Brazilian Portuguese resources and external counter/widget launch admission. Tab reselection and per-tab calculator restoration are additionally exercised by the pseudolocale tests.
- `pseudo-runtime-5.log`: **2 pseudolocale journey tests passed**, zero failures/skips, **55.208 seconds**. Both `en-XA` and `ar-XB` were selected through the real platform `LocaleManager`, verified against the packaged `LocaleConfig` and active Activity configuration, and retained through Activity recreation. Both locales ran at **360 × 800 dp and 1280 × 800 dp, font scale 1.0**. Coverage includes all five navigation destinations and their label bounds, bottom-bar/rail mirroring, Projects list and new-project sheet, Counter increment/Undo, calculator raw `12.5` state across tab switches/reselection, saved-pattern detail/Back, web-pattern editor with mixed-direction title and exact case-sensitive URL, and a chart with three days of synthetic sessions. Database assertions preserve the project name and original count. The **28 final screenshots** under `screens-final/pseudolocale-screens/{en-XA,ar-XB}-{360,1280}-*.png` were visually inspected: wrapping and actions remain usable, chart labels fit, and URL text stays intact (the compact editor scrolls horizontally). No scoped product clipping or directional defect was confirmed; production UI code is unchanged.
- `lint.log`: **debug and release Android Lint passed**, **0 errors / 10 warnings each**, with fresh analyzer-owned XML copied to `lint-debug.xml` and `lint-release.xml`. The warnings match section 8.14: four `MissingOnRenderProcessGone` in the Ravelry browser, three `RtlHardcoded` in intentional physical pane positioning, two `VisibleForTests` in the Insights ViewModel and one `ModifierParameter` in `ScrollableFormDialog`. They remain visible; no suppression, baseline or threshold changed. This production-configuration run completed in **16m 54s** and did not build or distribute a signed release artifact.

Exploratory failures are retained separately and are not counted as passes. They included an incorrect formatter-task selector before source formatting began, a UiAutomation collision with Android CLI's layout instrumentation, an existing test combination without independent entitlement fixtures, and new-test selectors/fixtures. The pseudolocale label check was corrected to inspect rendered line extents and ellipsis because generic paragraph overflow also includes unused RTL origin space; screenshots confirmed the labels were visible. The Insights driver originally expected a literal range heading and seeded only one chart bucket, which the product intentionally hides. It now locates the existing list tag and seeds three dated sessions. No production assertion, baseline, gate or lint rule was weakened to make these tests pass.

#### Production manifest and resource evidence

The production-configuration `verifyGoogleServicesJson`, `processReleaseMainManifest` and `processReleaseResources` tasks passed (`release-resources.log`). The ignored local Firebase configuration was available and passed the existing non-placeholder validator; its contents and all environment/credential values were not printed.

- Fresh merged manifest: `app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`, SHA-256 `1D49CD89C1B67BF8A084BBEA23A192D1E908BB302559A5792B1AC695742A1929`. A copy and structured inspection are in `merged-release-manifest.xml` and `release-manifest-summary.json`.
- Package is `com.finnvek.knittools`; CAMERA is absent, cleartext is false, RTL is enabled and `localeConfig` points to `@xml/locales_config`. MainActivity is exported/singleTop with launcher, SEND text/plain and VIEW/SEND application/pdf filters. It declares no orientation lock, resizability override or configuration-change opt-out. Debuggable/testOnly flags are absent.
- Exported entries are the intended launcher/share activity, widget receiver protected by BIND_APPWIDGET, Glance service protected by BIND_REMOTEVIEWS, WorkManager job service protected by BIND_JOB_SERVICE, and WorkManager/ProfileInstaller diagnostic receivers protected by DUMP. Other merged components do not introduce an unprotected exported entry. This is manifest-contract verification, not a new security audit.
- FileProvider is non-exported, grants URI permissions and uses `com.finnvek.knittools.fileprovider`; the linked `file_paths.xml` exposes only app-owned `progress_photos/` and `pattern_captures/`. The merged app metadata contains the approved Crashlytics collection setting and dependency billing/Google Play services versions, without debug Sentry/test metadata.
- AAPT2 inspected the actual production **linked resource intermediate**, `app/build/intermediates/linked_resources_proto_format/release/processReleaseResources/linked-resources-proto-format-release.ap_`, SHA-256 `AE72836E56C5C63CF1ED3290B675F651D9F11544D073E533714BDDB876588838`. `dump configurations` contains neither generated pseudolocale; `dump xmltree --file res/xml/locales_config.xml` contains exactly the 12 production tags; `dump xmltree --file res/xml/file_paths.xml` confirms the narrow paths. Outputs are `release-configurations.txt`, `release-locales.txt` and `release-file-paths.txt`.
- The installed isolated debug APK independently contains generated `en-rXA`/`ar-rXB` resources and the 14-entry debug locale configuration (`debug-configurations.txt`, `debug-locales.txt`, `app-apk-hash.json`). This comparison proves the debug overlay does not enter production resource linking. It does **not** prove final signed APK/AAB contents after optimization/packaging.

**Remaining section 9 blocker:** the normal `:app:assembleRelease --dry-run` fails at the unchanged release gate (`release-gate.log`) because **KNITTOOLS_KEYSTORE_PATH, KNITTOOLS_KEYSTORE_PASSWORD, KNITTOOLS_KEY_ALIAS and KNITTOOLS_KEY_PASSWORD** are unavailable. Final production APK/AAB packaging verification remains **OPEN / BLOCKED** until those prerequisites are supplied in a trusted environment and a real signed candidate can be inspected. No isolated artifact, bypass or intermediate is counted as that final verification.

Final preservation compares all **1,277 captured starting files**: **1,272 are byte-for-byte unchanged**. Only this plan, `app/build.gradle.kts`, `ReferenceResourceLocalizationTest.kt` and the two release-surface scripts changed among those files. The two task-owned new files are `PseudolocaleNavigationTest.kt` and the debug `locales_config.xml`. `preservation-final.json` also distinguishes an unrelated path outside the starting inventory, which this task did not create or edit. The branch/HEAD are unchanged, the index is empty, `AGENTS.md` and `CODEX.md` remain identical and unchanged, and `git -c core.safecrlf=false diff --check` passes. The task-owned emulator, its helper processes and all task Gradle runs have ended; unrelated processes were preserved. No temporary server or watcher remains from this task.

**Unchanged boundaries:** T08, signed-in live Ravelry W11, the separate INC-SIGN-01 owner action (4.1), unrelated release-surface/security findings and WebView findings remain open. The specific `pt-rBR` parity defect above is resolved. The source guard's known-secret check had no known local/environment Ravelry values available for comparison, so its passing status does not resolve historical exposure. Hilt construction, graph-scoped ViewModels, repository/dispatcher ownership and the WebView-only Ravelry architecture are unchanged. No CI implementation, startup benchmarking, profile generation, unrelated security remediation, commit, push, pull request, publication or deployment was performed.

#### Signing-prerequisite follow-up — 2026-10-23

Production-signing preparation is now **COMPLETE for local prerequisites**, following the owner's confirmation that KnitTools has not previously been uploaded or distributed as a release. The final record in **10.3** documents the newly authorized KnitTools-only key, successful private-key/signature checks and passing signing/Firebase/PostHog validators. Credentials are recovered only into the verification/build process from protected project-local storage. The separate encrypted off-computer backup and independent recovery/signing verification are **COMPLETE**, as recorded in **10.3** and `.local/signing/README.md`. Earlier failed attempts and completed resource, manifest and test results remain historical evidence. **At completion of that prerequisite task, no release candidate had been built. Final signed local APK/AAB packaging was subsequently verified in 12.6 on 2026-10-23; candidate runtime and distribution remain open.**

## 10. Work package 4: optimized runtime validation and CI

### 10.1 Three distinct build purposes

| Artifact | Purpose | What it does not prove |
| --- | --- | --- |
| Debug / isolated debug | Fast functional tests, synthetic policy-state setup, pseudolocales | Real release optimization, production entitlement behavior, or final release configuration |
| Optimized benchmark/test artifact | Repeatable R8 runtime smoke and performance measurement with controlled nonproduction configuration | Final signing, production Firebase behavior, or Play-distributed billing |
| Actual signed release candidate | Final app behavior, packaged resources/profiles, release source sets and configuration | Store/backend/device checks that were not actually executed |

Prefer the existing `benchmarkRelease` path over adding another build type. Before using it for optimized smoke, inspect the effective variant configuration and prove that minification and resource shrinking actually run. The name alone is insufficient.

`nonMinifiedRelease` is useful for profile generation; it is not acceptable evidence for R8 runtime behavior.

### 10.2 Ordinary CI changes

1. Keep existing debug, unit-test, ktlint, Detekt, and lint coverage.
2. Make required release lint coverage explicit if the current aggregate task does not prove it in CI logs.
3. Build a genuinely optimized test/benchmark artifact using the existing plugin-generated variant when possible.
4. Use only nonproduction signing and the existing allowed placeholder configuration for that non-distributable artifact.
5. Run a small deterministic smoke suite on a disposable emulator through an external UIAutomator-style test driver.
6. Do not enable debuggability on the target just to simplify testing.
7. Keep production signing keys and real release secrets out of untrusted pull-request execution.
8. Pin any newly added CI action according to the repository's existing immutable-SHA convention.
9. Preserve analyzer-owned reports, smoke results, and failure diagnostics as CI artifacts according to existing retention practice.
10. Do not disguise a skipped emulator test as a passing smoke test.
11. Do not add unstable absolute performance thresholds to shared-emulator PR CI.

If the existing benchmark variant cannot reproduce release optimization, make the smallest variant adjustment and document exactly how it differs from release. Do not weaken the real release artifact gate.

### 10.3 Actual release-candidate build

1. Run the real release build only in a trusted local or trusted CI environment with the existing prerequisites.
2. Preserve all signing and Firebase validation logic.
3. Build the APK for direct device verification and the AAB for final packaging verification as appropriate.
4. Verify package/version identity, signing certificate identity, non-debuggable status, optimization outputs, and artifact hashes.
5. Check release dependencies: no Sentry, Firebase Auth/Functions, model-backed parsing, or debug Pro override.
6. Preserve the approved Crashlytics and consent-based PostHog behavior.
7. Use synthetic project data on a dedicated test device/profile. Do not overwrite the user's installed production data for testing.
8. Do not edit or inject a test database into the final artifact.
9. Preserve R8 mapping output and its association with the exact artifact.
10. If signing or real Firebase configuration is unavailable, report the candidate verification as blocked. An optimized placeholder artifact does not close it.

#### Production-signing prerequisites — 2026-10-23

This preparation was limited to KnitTools on the existing dirty feature branch `codex/audit-korjaukset-20261018`. Only this plan was edited; existing staged/unstaged work and earlier evidence were preserved.

| Prerequisite | Verified outcome / remaining boundary |
| --- | --- |
| Signing mechanism | `app/build.gradle.kts` requires all four nonblank environment variables before assigning release signing and retains its release-artifact admission gate. No signing fallback or gate change was made. |
| Process configuration | Presence-only checks found `KNITTOOLS_KEYSTORE_PATH`, `KNITTOOLS_KEYSTORE_PASSWORD`, `KNITTOOLS_KEY_ALIAS` and `KNITTOOLS_KEY_PASSWORD` all absent. No values were printed or user/system environment settings changed. |
| Intended key and identity | The inspected KnitTools source and project documentation do not establish an assigned production/upload key, authorized location/alias, or trusted production/upload certificate fingerprint. Whether an existing signing identity must be preserved remains **UNKNOWN**; absence of configuration does not prove that no key exists or that this is a first release. No external keystore was inspected or used. |
| Key verification | Keystore existence/opening, private-key entry/alias, both passwords, public certificate recording/comparison and a local signing/verification exercise are **NOT RUN**, pending identification and authorization of the intended key and secret-safe process configuration. No key was created or modified. |
| Firebase / PostHog | `app/google-services.json` and `posthog.properties` are present, ignored and untracked; their contents were not displayed and their bytes remained unchanged. These are **PRESENT, NOT FRESHLY VALIDATED**, not missing configuration. The earlier Firebase validation in 9.4 retains its original date and scope. |

The existing `:app:verifyGoogleServicesJson` and `:app:verifyPostHogConfig` tasks were inspected before invocation: the former depends on a writer that returns without changing an existing Firebase file, then checks existence and the debug-placeholder marker; the latter checks token syntax only. Neither proves live service access, project ownership or runtime delivery. A Gradle **9.8.1** invocation used the existing Temurin **17** runtime, a temporary repository-local Gradle user home/project cache, the existing dependency cache in read-only mode, and `--offline --no-daemon --max-workers=1 --no-configuration-cache --no-build-cache --console=plain`. It requested only those two validators. Configuration failed during isolated offline dependency resolution (**exit 1, 32 s**), before either validator executed; this is neither a validator pass nor evidence that the configuration is invalid. User-global Gradle properties and shared credential stores were not read or changed. Errors were reduced to a secret-safe failure category.

**Initial preparation status: INCOMPLETE / BLOCKED; the owner action and authorization below were superseded by the continuation record.** At this attempt, an assigned production/upload key and authorized path were unknown. Key creation/replacement was not authorized, all key checks were deferred and configuration validators had not executed. No replacement or first-release assumption was made.

No release build or release task-graph dry run, APK/AAB creation/upload, Crashlytics mapping upload, analytics event, profile generation, device test or external-service change was performed. The release candidate and final packaging remain **UNVERIFIED**. Task-owned Gradle processes have exited. The temporary ignored `.gradle/signing-prereq-20261023/` directory remains: automatic approval rejected its removal even after the exact task-owned path and exited processes were verified (`blocked by policy`, with no further reason supplied). The plan-only whitespace check and repository `git diff --check` pass; no commit, push, PR or subsequent work package is included.

#### Signing-history and configuration continuation — 2026-10-23

The owner does not know whether a release key was previously configured. The follow-up explicitly authorizes creation of a new KnitTools-only key under `.local/signing/` **only if initial setup is established with no registered or distributed release identity to preserve**. It allows ordinary dependency downloads, keeps all project/global-configuration boundaries, and treats the earlier denied directory cleanup as separate housekeeping. That conditional key-creation decision remains pending; no signing directory or secret material was created.

**Repository evidence:** 26 signing-related `app/build.gradle.kts` history snapshots, from initial commit `31934f91` through the later audit history, use the `KNITTOOLS` environment-variable mechanism. The inspected signing assignments contain no static keystore filename or properties-file signing setup. Git history contains no tracked `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*keystore.properties` or `*signing.properties` path. Current project documentation provides no assigned release-key path or established Play certificate. These are bounded repository observations, not proof that no external key or prior release exists. No external key candidate was identified or inspected.

**Existing artifacts:** Android SDK 37 `aapt2 dump badging` and `apksigner verify --print-certs` inspected all seven current APKs in `app/build/outputs` and `baselineprofile/build/outputs`, plus the three retained app snapshots `reports/audit-part08-device-20261013/{normal-original,normal-current}.apk` and `reports/audit-part09-20261013/phone/audit-original-app.apk`. All **10/10** signatures verified with exit 0, all packages belong to `com.finnvek.knittools` or its test variants, and all certificates have the Android Debug subject and SHA-256 **`F6ED10FCF19C9AA5F6A31A53358CC245F487FE5B1765E7028080E020AEBB7D05`**. The optimized test APKs are non-debuggable but still debug-signed. No production-signed artifact or AAB was identified in this bounded inventory. No app was installed or device queried.

| Signing identity | Established state |
| --- | --- |
| Ordinary debug signing | Verified on the ten existing APKs above; not a production/upload-key verification. |
| Local release/upload key | Not identified. All four signing variables remain absent; key opening, private-key use and a local signing exercise remain unperformed. |
| Google Play upload certificate | Not established. An upload certificate is the identity Play accepts for submitted bundles/APKs; the debug certificate does not establish it. |
| Google Play app-signing certificate | Not established. This is the certificate Google uses for distributed APKs and can differ from the upload certificate. |

The identity distinction follows [Android app-signing guidance](https://developer.android.com/studio/publish/app-signing). Read-only browser navigation to Play Console reached `/console/signup` (developer-account creation) for the current signed-in account, with no accessible KnitTools app/signing page. No signup, account switch, enrollment, settings change or upload was performed. This does not rule out another account or separately distributed release.

**Configuration validators: PASS.** The same existing Gradle **9.8.1** / Temurin **17** setup was reused with repository-local `GRADLE_USER_HOME` and project cache, read-only access to the dependency cache, and `--no-daemon --max-workers=1 --no-configuration-cache --no-build-cache --console=plain` (without `--offline`). Only `:app:verifyGoogleServicesJson` and `:app:verifyPostHogConfig` were requested. `writeGoogleServicesJsonFromEnv`, `verifyGoogleServicesJson` and `verifyPostHogConfig` all executed successfully: **exit 0, 4m 44s, three executed tasks**. Before/after hashes confirm that both private configuration files and `app/build.gradle.kts` are unchanged. This proves the existing Firebase existence/non-placeholder check and PostHog token-syntax check; it does not prove live service access, delivery or production runtime behavior. Sanitized outcomes and public artifact metadata are retained in the already ignored `.gradle/signing-prereq-20261023/online-validation-result.json` and `certificate-evidence.json`.

**Historical continuation status: INCOMPLETE pending release-history confirmation; superseded by the final setup record below.** The owner was asked whether KnitTools had ever been uploaded to Play Console, including internal/closed testing, or separately distributed as a signed release. No keystore-path knowledge was required. At that point `.local/signing/` did not exist and was not ignored; key creation and its storage/reuse/backup checks remained conditional on the answer.

Only this plan was edited in the continuation; branch/HEAD, the empty index and existing user work were preserved. The verification launcher and its single-use Gradle daemon have exited. The previously denied temporary-directory removal was not retried. No release artifact, mapping upload, analytics event, device test, dependency/CI change, external-service change, commit, push, PR or further work package was performed. The plan-only whitespace check and repository `git diff --check` pass. Final release-candidate and packaging verification remain open.

#### Initial signing setup completed — 2026-10-23

**Decision evidence:** in direct response to the question covering Play uploads (including test tracks) and separately distributed signed releases, the owner confirmed that KnitTools has not been published anywhere. Together with the bounded Git/artifact findings above, this establishes the authorized initial-setup path; there is no known previously registered/distributed KnitTools release identity to preserve. The user's conditional authorization permits a new dedicated key only under `C:\Dev\KnitTools\.local\signing\`. No external keystore was used.

**Created storage:** `.gitignore` now contains only the added `/.local/signing/` rule. Ignore/untracked checks passed before writing secrets. The new directory's inherited access was removed, granting access only to its creating Windows user and SYSTEM; all created files were subsequently checked against that boundary. No existing shared directory or other application's permissions were changed. The new PKCS12 file is `knittools-upload.p12`, with one private-key entry `knittools-upload`, **RSA 3072 / SHA256withRSA**, subject `CN=KnitTools Upload`, valid **2026-10-23 to 2056-10-23**. Its public certificate SHA-256 is:

`C31F2806EFA207CE5669647EC6902A1395788B5C0143AE3527983951F1A37D97`

This is the new **local release/upload certificate**, distinct from the debug certificate recorded above. It has **not been registered with Google Play**. No Google Play app-signing certificate was selected or verified. Generation follows the [Android signing guidance](https://developer.android.com/studio/publish/app-signing) and [JDK 17 keytool guidance](https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html): 30-year validity, PKCS12 storage and secret input through `-storepass:env` / `-keypass:env`. One cryptographically random 32-byte password, Base64-encoded, protects both the store and key for Android compatibility; its value was never printed or placed in tool-call arguments, source, reports or Git.

`credential.clixml` persists that password encrypted with Windows DPAPI, recoverable by the creating user on this machine. Recovery was verified in a fresh process. `Invoke-WithSigning.ps1` is a small ignored local wrapper that sets only the existing four `KNITTOOLS_*` variables around the supplied command, then restores their earlier process values; it starts no build itself. Focused checks passed for both normal completion and an intentionally thrown command error, including removal of originally absent variables. No user/system environment configuration was changed. Plaintext-password checks of task-owned text/logs found zero occurrences. Public certificate files contain no private key or password.

**Verification: PASS.** `keytool` opened the store with the persisted password and verified the intended `PrivateKeyEntry`. JDK `jarsigner` signed a small local test JAR using the recovered key password, then `jarsigner -verify -strict` verified it against the intended alias/store (**both exit 0**). Verification left the keystore and encrypted credential bytes unchanged. This is a local cryptographic exercise, not an Android release artifact.

The resolved AGP **9.4.1** signing-validation implementation and actual task graph were inspected before execution. A local verification init script asserts the exact six-task graph and zero actions on `preBuild` / `preReleaseBuild`; it also opens the key through the **effective Gradle release signing configuration** and verifies the private-key entry and the public fingerprint above without printing credentials. Final Gradle **9.8.1** / Temurin **17** validation used the existing isolated project-local Gradle home/cache and requested only `:app:validateSigningRelease`, `:app:verifyGoogleServicesJson` and `:app:verifyPostHogConfig`. Result: **BUILD SUCCESSFUL, exit 0, 23 s; three executed tasks and one up-to-date writer**. Both lifecycle nodes were empty/up-to-date; no compile, package, profile-generation or upload task was in the graph. The existing production signing logic/gates and Firebase/PostHog configuration files remain unchanged. Safe evidence is in `.local/signing/checks/{verification,gradle-verification}.json` and `public-certificate.json`.

**Reuse and backup:** `.local/signing/README.md` provides an executable verification-only command using the existing Gradle/JDK, isolated Gradle configuration and `Invoke-WithSigning.ps1`. A later explicitly authorized release-build task can reuse that process-scoped signing wrapper; the verification-only task-graph guard is not part of a release build. As documented by [Microsoft](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.utility/export-clixml), DPAPI credential files are bound to the creating user/machine: copying `credential.clixml` alone is not a portable recovery backup. **At initial setup, no off-computer backup had been made or tested and no external backup location had been accessed.** The backup-only continuation below records the subsequently completed encrypted backup and independent recovery test.

**Final preparation status: COMPLETE — local signing and service-configuration prerequisites verified.** The only repository changes for this setup are `.gitignore` and these relevant plan records; new signing files remain ignored/untracked. Branch/HEAD, the empty index and unrelated user work were preserved, and `git diff --check` passed. Task-owned CLI processes and single-use Gradle daemons have exited. The previously denied temporary-directory cleanup remains separate and was not bypassed. No final APK/AAB build, upload, mapping upload, analytics event, device test, Play enrollment/settings change, dependency/CI/application change, commit, push, PR or other work package was performed. **The release candidate and final production packaging remain UNVERIFIED.**

#### Recoverable signing backup completed — 2026-10-23

Scope is exclusively the existing KnitTools key. The owner selected the Google Drive folder `KnitTools-backup`; the exact destination and completed file link remain in the ignored local README. A new **5644-byte** `KnitTools-signing-recovery.zip.gpg` was uploaded there through the authenticated browser at **2026-10-23 12:50:47 UTC / 15:50:47 Europe/Helsinki**. The connector's initial upload failed with insufficient OAuth scopes; browser upload succeeded without changing scopes, sharing or service settings. No existing cloud file was overwritten. Drive storage is not treated as the encryption boundary: the package itself is encrypted.

The ignored `.local/signing/Backup-SigningKey.ps1` is a one-task preparation/recovery command using existing GnuPG **2.4.9**. It prepares an in-memory ZIP containing the unchanged PKCS12, its original password, the public certificate and recovery instructions, then writes only an **AES-256 OpenPGP encrypted** package to a fresh project-local directory. An independently retained owner-selected password protects that outer package; the original key and its password are not rotated. Secret input uses masked local prompts and a process input pipe, never command arguments or plaintext temporary files. Output files use create-new semantics. The recovery branch runs in a fresh process and does not read the original DPAPI credential: it checks encrypted-package integrity, the pinned public certificate, a recovered-key challenge signature, and rejection of a wrong password, altered package and altered challenge. It does not modify the active signing setup.

**Independent recovery/signing: PASS.** The owner entered the backup password through the masked local workflow and confirmed that it was retained independently of this Windows installation. A fresh process recovered the package at **2026-10-23 12:48:04 UTC / 15:48:04 Europe/Helsinki** without reading `credential.clixml`. The recovered certificate matched the existing public SHA-256 `C31F2806EFA207CE5669647EC6902A1395788B5C0143AE3527983951F1A37D97`; the recovered private key signed a random 32-byte local challenge, whose SHA-256/RSA signature verified against that public certificate. Wrong-password, ciphertext-tampering and altered-challenge rejection passed. The local script checks that the active keystore and DPAPI file remain unchanged before starting that isolated recovery process.

**Off-computer copy/readback: PASS.** Drive folder/file metadata confirmed the intended parent and uploaded file. Authenticated raw-file readback through the connector was compared in memory, and the browser download was also checked: both matched the independently tested local package **byte for byte**. Package SHA-256: `86E7AE0FCAF4D29E4C823063BB76236C22FDE2C5C61BEFB6701E6756EEBBAA54`. Only the task-created browser download was removed after comparison; the verified repository-local package and Drive backup remain. The safe records are `.local/signing/backup-20261023-154712-fd7f457c/KnitTools-signing-recovery.zip.gpg.{verification,cloud-verification}.json`, with UI evidence in `.local/signing/checks/drive-backup-20261023.png`. The earlier `checks/backup-toolcheck-20261023.json` remains historical synthetic-preparation evidence.

**Status: COMPLETE for this backup work package.** Recovery needs the Drive package and the separately retained outer password; the package contains the unchanged original PKCS12, its original password, the public certificate and recovery instructions. No old Windows account or DPAPI credential is needed for that procedure. The bounded test used a separate process on the current machine, not a reinstalled Windows environment; the cloud copy was validated by exact equality to the successfully recovered package. No further owner action is pending for completion, beyond continuing to retain the independent password and Drive access. The active key/password were not replaced or rotated. No release build, profile/device campaign, Play action, service-setting change, commit, push, PR or unrelated work package was performed. The previously denied cleanup was not retried.

#### Local signed candidate follow-up — 2026-10-23

The existing dedicated key and local Firebase/PostHog configuration now produced genuine signed release APK/AAB artifacts. Their signatures, production packaging, R8/resource shrinking, refreshed profiles and exact mapping association passed local verification. See **12.6** for commands, input/artifact identities and retained files. Mapping upload, genuine-candidate runtime, service delivery and Play distribution remain separate; R01–R13 are not closed by this packaging result.

### 10.4 Smoke journeys

| ID | Journey | Required assertions |
| --- | --- | --- |
| R01 | Fresh offline launch | Usable empty state, no crash, no false perpetual loading. |
| R02 | Create project, reopen it, increment/decrement/Undo | Hilt/Room/navigation initialize; values persist correctly. |
| R03 | Open/edit a project and return between tabs | Expected back stack and project identity. |
| R04 | Start/stop a synthetic work session and view Insights | Session storage and existing statistics render under R8. |
| R05 | Import a small local PDF, read it, use a bookmark/annotation, reopen | FileProvider/SAF, rendering, state, and stored metadata remain usable. |
| R06 | Capture/cancel progress and pattern photos | No app camera permission; correct ownership and cleanup. |
| R07 | Save/edit a yarn card and a web-pattern entry | Persistence and serialization paths work. |
| R08 | Export and restore a small backup on a disposable installation | Existing backup semantics survive optimization; no test on user data. |
| R09 | Theme and supported-language change | Resource loading and recreation work in release. |
| R10 | Ravelry open, restore, Back, and a live permitted PDF download | Browser behavior and downloader remain integrated. |
| R11 | Free/expired/Pro boundaries | Release does not inherit debug feature bypasses. |
| R12 | Widget/external launch into a synthetic project | Initialization and deep-link admission work. |
| R13 | Representative wide-screen transitions | Optimized artifact preserves adaptive behavior. |

Automate deterministic local journeys in CI. Keep actual camera handlers, live Ravelry, Play Billing, and final service verification as separately recorded device/service checks where needed.

Do not make real purchases merely to satisfy a smoke test. Use an authorized Play test environment for billing behavior, and identify any unverified release gate.

### 10.5 R8 failure handling

1. Reproduce the failing journey on the optimized artifact.
2. Compare with debug/non-minified only to isolate the optimization-sensitive boundary.
3. Read the actual missing-class, reflection, resource, serialization, or retraced crash evidence.
4. Check library consumer rules and generated code before adding app rules.
5. Add only the narrow rule or code correction needed for the demonstrated issue.
6. Rebuild and rerun the same failing journey.
7. Verify the correction does not disable optimization across an entire package or app.
8. Do not solve the failure with global `-dontwarn`, `-dontoptimize`, or broad `-keep class **`.

The existing minify/shrink DSL remains supported by the current AGP documentation. Migrating to `optimization { enable = true }` is not required for release readiness. [Enable R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [keep rules](https://developer.android.com/topic/performance/app-optimization/add-keep-rules), [optimization troubleshooting](https://developer.android.com/topic/performance/app-optimization/troubleshoot-the-optimization)

### 10.6 Optimized runtime and CI implementation record — 2026-10-21

**Scope:** section 10 on the existing `codex/audit-korjaukset-20261018` branch, starting HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. The dirty checkout and index were captured before editing. Evidence is under ignored `reports/optimized-runtime-20261021/`; paths below are relative to that directory. Existing sections 5–9.4 remain historical evidence within their recorded boundaries.

#### Optimized target and production boundary

- Effective Gradle discovery (`discovery.log`) confirms the existing plugin-generated `benchmarkRelease` has **minify=true, shrink=true, debuggable=false, signing=debug**. `nonMinifiedRelease` has neither optimization enabled and is not used as R8 evidence. No build type or dependency version was added.
- The opt-in `-Pknittools.optimizedSmoke=true` changes only the benchmark target to `com.finnvek.knittools.optimizedtest`. Its manifest overlay makes it `testOnly` and removes INTERNET/BILLING; CAMERA is already absent. The build checks the effective optimization, debuggability and nonproduction signing settings before applying the overlay. Main/release source sets and release Pro behavior remain in use; debug fixtures and the debug Pro override are absent.
- The smoke Google Services task uses a generated copy of the existing allowed placeholder configuration with the isolated package name. It does not select the ignored local production configuration or the environment writer. PostHog is disabled with an empty token and Crashlytics collection is disabled for this test artifact. Existing real-release signing, Firebase and PostHog gates and approved production service behavior are unchanged. `FirebaseGradleIntegrationSourceTest` was updated for the exact smoke-task exception while retaining its production-gate expectations.
- The actual build executed `minifyBenchmarkReleaseWithR8`, `convertShrunkResourcesToBinaryBenchmarkRelease` and `optimizeBenchmarkReleaseResources` (`build-3.log`); the subsequent complete app/driver assembly passed (`build-4.log`, repeated in `final-optimized-build.log`). R8 produced a **97,133,196-byte mapping**, a **7,584,846-byte usage report** and **610 unreachable-resource entries** (`optimization-summary.json`). Packaged manifest/resource inspection verifies non-debuggable/test-only flags, absent network/billing/camera permissions and absent `en-rXA`/`ar-rXB`. Runtime checks independently assert the target flags and permissions before clearing its synthetic data.
- The resolved benchmark runtime dependency report in `final-optimized-build.log` contains no Sentry, Firebase Auth/Functions or model-backed AI dependency. The release no-op diagnostic implementations remain selected. No app runtime code or R8 keep rule changed in this task.

| Final tested artifact | Identity |
| --- | --- |
| App | `app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk`; `com.finnvek.knittools.optimizedtest`, versionCode **1**, versionName **1.0.0**, **11,139,244 bytes** |
| App SHA-256 | `09FAD2639648D8007699F7E78386228AD5F805D454FEB46FC2E25BA3852A48F5` |
| Mapping | `app/build/outputs/mapping/benchmarkRelease/mapping.txt`; SHA-256 `4FE5ACE701738EAA7FFC007C5D2E33EA5432BB87076C10D3758BC531620D58CC` |
| External driver SHA-256 | `575CC512BC285F5849D9E5A0B4DB7932D626CB0B7D621B369D8A2BB83F2C04C6` |
| Nonproduction certificate | Android Debug; SHA-256 `f6ed10fcf19c9aa5f6a31a53358cc245f487fe5b1765e7028080e020aebb7d05`; `apksigner verify` passed (`signature.txt`) |

`verified-suite/identity.json` and `ci-local/identity.json` bind both final runs to these exact hashes. The driver is external UIAutomator instrumentation in the existing `baselineprofile` module's `benchmarkRelease` source set. It creates synthetic content through normal app/system UI and exports backups through SAF; it does not inject target database or entitlement fixtures. Root access on the disposable userdebug emulator is limited to reading app-owned file paths for cleanup assertions and terminating the exact isolated target PID for cold-launch/restoration checks. Those latter checks wait for Android's `mHaveState=true` / `STOPPED` state and retain termination evidence. The baseline profile generator was neither changed nor executed.

#### Runtime results and R01–R13 status

The complete final suite passed **7/7 tests, 0 failures, 0 ignored**, **247.803 seconds** (`verified-suite/instrumentation.txt`). The exact two-method CI selection then passed **2/2, 0 failures, 0 ignored**, **53.262 seconds** (`ci-local/instrumentation.txt`). These are nine passing executions of seven distinct tests. Earlier failing exploratory runs remain separately recorded and are not counted as passes.

All rows below use the exact optimized app/driver pair above on task-created `KnitToolsOptimized20261021`, `emulator-5584`, **API 36 / google_apis / x86_64**, Pixel 2 profile, WebView **133.0.6943.137**, and AOSP camera `com.android.camera2` **2.0.002 (20002000)**. Default smoke display is **720 × 1600 px at 320 dpi**, font scale **1.0**; wide overrides are **2560 × 1600** and **1600 × 800 px** at the same density (display sizes 360 × 800, 1280 × 800 and 800 × 400 dp, before insets). Actual screenshot dimensions are recorded in `runtime-window-dimensions.json`. No 320 dp, 200% font scale, combined configuration or TalkBack check ran.

| Row | Executed evidence on the optimized artifact | Status and remaining boundary |
| --- | --- | --- |
| R01 | `projectCounterSessionAndNavigation`: cleared isolated installation, normal offline launcher, empty Projects message and New Project action, then usable creation flow. `R01-empty` screenshot/XML. | **Local pass.** No production-service initialization claim. |
| R02 | Same test: create project; increment 0→1→2, decrement to 1, Undo to 2; reopen; later count 3 persists through force-stop/relaunch. | **Local pass.** Real Hilt/Room/repository/UI path; no test database. |
| R03 | Same test: edit project name through overview actions, visit all five tabs, reopen the matching counter and preserve its value/name. | **Local pass** for this navigation journey; not exhaustive deep-link/back-stack certification. |
| R04 | Same test: explicitly start a session, change count, force-stop/relaunch, stop/save it and render nonempty Insights for the matching project. `R04-insights` shows 1 display minute and counter progress 1; the Start action is available again afterward. | **Local pass.** A short session exercises existing minute rounding; reboot/24-hour review and T08 are not tested here. |
| R05 | `pdfImportBookmarksAnnotationsAndExternalShare`: generate a two-page synthetic PDF outside the app, import with SAF, read page 2, add bookmark and pen stroke, force-stop/reopen page 2 and bookmark. A fresh exported backup has nonempty `project_documents`, `pattern_bookmarks` and `pattern_annotations` rows. Reopened stroke was visually checked. | **Local pass** for import, reading, bookmark and annotation persistence. Large/encrypted/provider-specific PDFs and annotated-PDF export are outside this smoke. |
| R06 | `cameraCaptureAndCancellation`: invoke the actual external AOSP camera handler for progress and pattern photos; cancel and save both paths; reopen generated one-page PDF. Read-only file assertions verify 0 canceled progress files, 1 saved progress file, 0 remaining pattern captures and 1 final PDF. | **Emulator-handler pass.** CAMERA absent from target. Physical/OEM camera handlers, image quality and unavailable-handler behavior remain unverified here. |
| R07 | `yarnWebPatternAndBackupRestore`: create yarn and a web pattern with its URL, edit both names through UI, and retain the records after app restart and backup restore. | **Local pass** for these persistence/serialization paths; external website availability is not tested. |
| R08 | Same test: export a uniquely named fresh backup through SAF, add a second project, restore that exact backup with normal replacement confirmation, verify the extra project is gone and original count/yarn/pattern remain. | **Local pass** on disposable data only. No personal data, cloud provider or cross-version restore claim. |
| R09 | `themeLanguageAndOfflineBrowser`: select Dark then Light; English→Suomi, force-stop/relaunch in Finnish, return to English. Screenshots verify both themes and Finnish resources. | **Local pass** for these changes; not a renewed 12-language/native-language audit. |
| R10 | Same optimized test: open Ravelry's WebView, retain the allowed search URL across a wide resize, stop/save the Activity, terminate the app PID, return through Recents, verify restored WebView/URL and Back to Tools. | **PARTIAL for the optimized target.** Its evidence remains offline error-page/route restoration, with no native bounded-save capability claim for that older provider. Section 6.10 separately passes live signed-in Activity/task restoration, Back, a permitted PDF download, Save pattern, Library/rendering and cancellation on the ordinary debug APK/physical Pixel 9. W11 is closed within that boundary; optimized/signed live-browser/download acceptance remains **OPEN**. |
| R11 | `freeGateTrialAndAdaptiveTransitions`: second active project is blocked by the real free gate; the normal local Start free trial action then permits creation. Other media/widget/backup journeys use that same user-facing trial action. | **PARTIAL.** Confirms no debug Pro bypass and working local trial activation. Expired-trial notice/T08, purchased/restored/refunded Pro and Play Billing remain **OPEN**, requiring an authorized Play test environment; no purchase was made. |
| R12 | `widgetLaunchAndCounter`: place a real launcher widget, select/update the synthetic project through app UI, terminate its stopped process, open the matching counter from the widget, update 3→4 and observe 4 on the widget. PDF test also exercises external ACTION_VIEW/URI grant→Save pattern→Library. Browser test rejects a forged counter-launch token. | **Local pass** for these entry paths. The widget's own +/- buttons, other launchers/OEMs and all external intent combinations are not certified. |
| R13 | `freeGateTrialAndAdaptiveTransitions`: compact→wide→short landscape→compact overrides retain counter value and project identity; five-tab navigation and reopening remain usable. Wide/short screenshots show rail/bottom navigation respectively. | **Local representative pass.** No repeated whole-app adaptive campaign, physical foldable, hinge, OEM or performance guarantee. |

Success screenshots/XML, two fresh backup archives and process-termination records are in `verified-suite/optimized-smoke/`. PDF annotation reopening, Insights, widget count, browser fallback, theme/language and representative wide/short layouts were visually inspected alongside the automated assertions.

#### CI and local validation

`.github/workflows/build.yml` preserves debug assembly, unit tests, app ktlint, Detekt, aggregate lint and the existing Node 22 Functions job. It adds baselineprofile ktlint, explicit `:app:lintRelease`, an independent optimized build/emulator smoke job, and always-uploaded test/lint/mapping/failure evidence. Existing immutable action pins remain; new emulator-runner and upload-artifact actions are pinned to verified full SHAs. No prior artifact-retention setting existed in this workflow; the new diagnostic artifacts retain **7 days**. No production signing/service secrets, performance thresholds, uploads to a release destination or remote workflow triggers were added.

The smoke runner requires explicit disposable-emulator consent, verifies the artifact before installation, and checks the exact expected success count plus failure/crash/ignored/assumption status codes. A shell/instrumentation exit code of zero alone cannot pass the job. Its earlier failing runs returned failure as intended; screenshots, hierarchy and bounded logcat are preserved. Missing mapping, wrong package/configuration or a skipped test fail closed. CI selects only the project/session/navigation and free/trial/adaptive tests; full local camera/SAF/browser/widget coverage does not depend on untrusted PR access to external accounts.

Commands actually executed (all Gradle calls used `--no-daemon --max-workers=1`; toolchain unchanged: AGP 9.4.1, Kotlin 2.4.21, Gradle 9.8.1, Temurin JDK 17):

```powershell
./gradlew.bat --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' `
  :app:assembleBenchmarkRelease :baselineprofile:assembleBenchmarkRelease
./gradlew.bat --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' `
  :app:dependencies --configuration benchmarkReleaseRuntimeClasspath

./tools/optimized-smoke.ps1 -Serial emulator-5584 -DisposableEmulator `
  -OutputDirectory reports/optimized-runtime-20261021/verified-suite
./tools/optimized-smoke.ps1 -Serial emulator-5584 -DisposableEmulator `
  -OutputDirectory reports/optimized-runtime-20261021/ci-local -ExpectedTests 2 `
  -Test 'com.finnvek.knittools.baselineprofile.OptimizedSmokeTest#projectCounterSessionAndNavigation,com.finnvek.knittools.baselineprofile.OptimizedSmokeTest#freeGateTrialAndAdaptiveTransitions'

./gradlew.bat --no-daemon --max-workers=1 assembleDebug test
./gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest `
  --tests '*FirebaseGradleIntegrationSourceTest'
./gradlew.bat --no-daemon --max-workers=1 :app:ktlintCheck :baselineprofile:ktlintCheck :app:detekt
./gradlew.bat --no-daemon --max-workers=1 lint :app:lintRelease
./tools/release-surface-test.ps1
./tools/release-surface.ps1
```

- Optimized app/driver builds passed; the final driver-only rebuild/ktlint passed in **1m 9s** (`driver-final.log`). Formatting used the recorded `format-scoped.gradle`, limited to task-owned build scripts/test sources. No unrelated files were formatted.
- Debug assembly passed. The full JVM run executed **2,179 tests in 322 suites**, **2 failures / 0 errors / 0 skips** (`debug-and-unit.log`, `unit-full/`). One failure was this task's changed Firebase wiring expectation: it was corrected, and all **3 Firebase contract tests subsequently passed** (`final-checks.log`, `unit-firebase-contract/`). The remaining **pre-existing** failure is `EdgeToEdgeConfigurationTest > root scaffold owns app snackbar host`, expecting an old literal SnackbarHost expression. That test, `NavGraph.kt` and `MainActivity.kt` are byte-for-byte unchanged from task start (`preexisting-unit-failure.json`). It was not weakened or fixed outside scope. The full unit suite/ordinary CI job is therefore **not claimed green**.
- App/baselineprofile ktlint and app Detekt passed (`static-checks.log`, `final-checks.log`, `driver-final.log`). Release-surface self-tests passed **25/25** and the source guard **13 passed / 0 warnings / 0 failures**.
- Aggregate lint and explicit release lint passed in **15m 51s** (`lint.log`): **0 errors / 10 warnings each** for debug and release (`lint-summary.json`, copied analyzer XML). These are the same four `MissingOnRenderProcessGone`, three `RtlHardcoded`, two `VisibleForTests` and one `ModifierParameter` warnings recorded in 9.4. No suppression/baseline/threshold changed.
- `actionlint 1.7.12` passed and PowerShell parser errors were **0** (`workflow-runner-final.json`). Relevant build/check and emulator commands ran locally on Windows. **The workflow has not run on GitHub or on its Linux runner.** No push or remote execution was used to claim CI evidence.

The exploratory failures were driver/API/selector/timing issues, including competing Android CLI instrumentation, PDF sheet dismissal, initial widget selection and terminating the browser before Android saved the Activity. They were reproduced and corrected in the external driver; the same optimized app SHA then passed. There is no demonstrated missing-class/reflection/serialization/resource runtime defect requiring an app fix or keep rule. The build still reports **“Failed to collect Compose stack trace mapping”** for `CounterWidget.provideGlance`; ordinary R8 mapping is present, but that supplemental Compose trace output is not certified. The warning was not suppressed and optimization remains enabled.

#### Remaining prerequisites and closure limits

**Production candidate / section 9.4 remains OPEN / BLOCKED.** Availability-only checks found all four settings absent: `KNITTOOLS_KEYSTORE_PATH`, `KNITTOOLS_KEYSTORE_PASSWORD`, `KNITTOOLS_KEY_ALIAS`, `KNITTOOLS_KEY_PASSWORD` (`signing-availability.json`). No values, global/user Gradle properties or secret-file contents were printed. No real signed APK/AAB was built. A trusted environment with legitimate signing and configuration passing the existing Firebase/PostHog validators must run the unchanged release gates, build APK/AAB, verify signing/identity/dependencies and inspect final pseudolocale/debug-control packaging under 9.4/10.3. This test-only APK and prior linked-resource intermediates do not close that requirement.

**T08, signed-in live W11, the separate INC-SIGN-01 owner action (4.1) and unrelated WebView/security findings remain open.** The known-secret guard had no known Ravelry values available for comparison; its pass does not resolve earlier exposure. Physical/OEM and production-service checks remain separate. Startup-readiness signaling, startup benchmarks and baseline/startup profile generation were not implemented or executed.

Final preservation compares **1,279 starting files**: **1,274 are byte-for-byte unchanged**. The five changed existing files are this plan, `app/build.gradle.kts`, `baselineprofile/build.gradle.kts`, `.github/workflows/build.yml` and `FirebaseGradleIntegrationSourceTest.kt`. The four task-owned new files are the optimized manifest overlay, the two external-driver Kotlin files and `tools/optimized-smoke.ps1`. The unrelated `ui-insights-päivitys.md` path outside the starting inventory was not created or edited here. `preservation-final.json` records unchanged branch/HEAD, an empty index and identical unchanged `AGENTS.md`/`CODEX.md`; `git diff --check` passes. The task-owned emulator and helper processes have exited, all task Gradle runs have ended, and unrelated Java processes were preserved. No temporary server/watcher remains. No commit, push, pull request, publication or deployment occurred.

#### Independent reverification — 2026-10-21

The section 10 implementation and the evidence above were already present when this follow-up began. Source, configuration, raw results and artifact hashes were inspected before making changes. No additional app, driver, Gradle or workflow correction was needed. Only this verification record was added; existing uncommitted work was preserved on the same branch and HEAD. Fresh evidence is in `reports/optimized-runtime-20261021/reverification/`.

- Rebuilt the optimized app and external driver with the command below: **BUILD SUCCESSFUL**, 1m 26s, 1 task executed / 88 up-to-date. Effective discovery again confirms `benchmarkRelease` is minified, resource-shrunk, non-debuggable and debug-signed, with the release source set. R8 and resource-shrink tasks were up-to-date in this replay; their actual earlier execution is retained in `build-3.log`. The app, driver and mapping SHA-256 values match the final artifact table above exactly (`artifact-comparison.json` and both fresh `identity.json` files).
- Replayed all seven tests on read-only `KnitToolsOptimized20261021`, **emulator-5586 / API 36**: **7 passed, 0 failures, 0 ignored**, **249.774 seconds** (`full-suite/instrumentation.txt`). Every R01–R13 row above retains its stated assertion scope and limitations on this identical artifact. Fresh screenshots confirm nonempty Insights, the reopened PDF stroke, widget count 4, dark/Finnish settings, wide rail and short-window bottom navigation. No excluded width/font/TalkBack case ran.
- Replayed the exact two-method CI selection through `pwsh -NoProfile -File`: **2 passed, 0 failures, 0 ignored**, **53.161 seconds** (`ci-selection/instrumentation.txt`). `actionlint` again returned 0 and the PowerShell parser found 0 errors (`workflow-runner.json`). **No GitHub or Linux-runner execution is claimed.**
- Reused the earlier unit/static/lint evidence above; those suites were not rerun during this documentation-only follow-up. The failing `EdgeToEdgeConfigurationTest`, `NavGraph.kt` and `MainActivity.kt` still match the original section 10 starting hashes (`preexisting-failure.json`). The ordinary CI job therefore remains unverified as a whole and is not reported green.
- A fresh availability-only check again found **all four signing settings absent** (`signing-availability.json`). No production APK/AAB was built; **9.4 and 10.3 remain blocked**. R10/R11 external acceptance, T08, live W11, the separate INC-SIGN-01 owner action (4.1) and unrelated WebView/security findings remain open.
- Both instrumentation processes and the Gradle invocation exited. The task-started emulator and its recorded helper processes were stopped; `process-cleanup.json` confirms none remain. No startup/profile work, commit, push, PR, publication or deployment occurred.

```powershell
./gradlew.bat --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' `
  --init-script reports/optimized-runtime-20261021/inspect.gradle `
  :app:assembleBenchmarkRelease :baselineprofile:assembleBenchmarkRelease
./tools/optimized-smoke.ps1 -Serial emulator-5586 -DisposableEmulator `
  -OutputDirectory reports/optimized-runtime-20261021/reverification/full-suite
pwsh -NoProfile -File tools/optimized-smoke.ps1 -Serial emulator-5586 -DisposableEmulator `
  -OutputDirectory reports/optimized-runtime-20261021/reverification/ci-selection -ExpectedTests 2 `
  -Test 'com.finnvek.knittools.baselineprofile.OptimizedSmokeTest#projectCounterSessionAndNavigation,com.finnvek.knittools.baselineprofile.OptimizedSmokeTest#freeGateTrialAndAdaptiveTransitions'
```

#### Edge-to-edge JVM regression follow-up — 2026-10-21

**Verified cause:** a fresh focused run reproduced `EdgeToEdgeConfigurationTest > root scaffold owns app snackbar host` at line 56: **3 tests, 1 failure, 0 errors/skips** (`reports/edge-to-edge-followup-20261021/focused-before.log` and `focused-before/`). Inspection confirmed that `MainActivity` remembers the application `SnackbarHostState`, uses it for the downloaded-update prompt and passes it to `KnitToolsNavHost`. `NavGraph.kt` forwards that same state to `KnitToolsNavigationScaffold`, whose Material `Scaffold.snackbarHost` slot renders `SnackbarHost(it)`. The old test still required the previous inline `SnackbarHost(hostState = it)` expression in `NavGraph.kt`; this was a stale source-test expectation, not a demonstrated production wiring defect.

**Correction:** updated only the existing snackbar regression test to check state forwarding within the `KnitToolsNavHost` and `KnitToolsNavigationScaffold` calls, plus the host expression in the shared scaffold's `snackbarHost` slot. The manifest `adjustResize` and notes-editor IME assertions remain unchanged. No production source, dependency, build configuration or CI behavior changed.

Commands executed from the repository root, in this order (the focused command ran both before and after the correction):

```powershell
./gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest `
  --tests com.finnvek.knittools.EdgeToEdgeConfigurationTest
./gradlew.bat --no-daemon --max-workers=1 test
./gradlew.bat --no-daemon --max-workers=1 :app:ktlintTestSourceSetCheck :app:detekt
git diff --check
```

- Corrected focused class: **3 passed, 0 failures/errors/skips**, build successful in **1m 26s** (`focused-after.log`, `focused-after/`).
- Ordinary JVM suite: **2,179 tests in 322 suites passed, 0 failures/errors/skips**, build successful in **2m 3s** (`unit-full.log`, `unit-summary.json`, `unit-full/`). The unfiltered root `test` task matches `.github/workflows/build.yml`; it executed `:app:testDebugUnitTest`, completed `:app:test`, and reported `:baselineprofile:test` up-to-date. These are fresh local JVM results. **The ordinary JVM suite now passes; no GitHub workflow or Linux-runner execution is claimed.**
- JVM-test-source KtLint and app Detekt passed in **47s**; Detekt reports **0 findings** (`static-checks.log`, `detekt.xml`). `git diff --check` passed. All follow-up evidence is under ignored `reports/edge-to-edge-followup-20261021/`.

The historical failed-run XML at `reports/optimized-runtime-20261021/unit-full/TEST-com.finnvek.knittools.EdgeToEdgeConfigurationTest.xml` is unchanged, and the original failed-run records above remain intact. Only this plan and `EdgeToEdgeConfigurationTest.kt` changed from the follow-up baseline; the existing branch, HEAD, index and unrelated work were preserved. Task-owned Gradle processes exited; no temporary process remains and no unrelated process was terminated.

**Closure limits remain unchanged:** production signing/package validation (9.4/10.3), T08, signed-in live W11/R10, remaining R11 checks, the separate INC-SIGN-01 owner action (4.1) and unrelated WebView/security findings stay **OPEN/BLOCKED** as previously recorded. No signing or credential values were read or printed. Optimized emulator smoke, the adaptive matrix, Android Lint, 320 dp, 200% font scale, their combination and TalkBack were not rerun. Section 11 was not started; no commit, push, PR, publication or deployment occurred.

### 10.7 Retained signed candidate: local runtime verification — 2026-10-23

**Local scope complete. At this local run, R10 and R11 remained PARTIAL because live/entitlement requirements were excluded; 10.9 subsequently passes R10 on the same retained candidate. R11 remains separate.** Seven distinct API 37 journey tests have passing executions, plus one bounded API 29 startup/navigation test. No production defect was confirmed, no production input changed, and no replacement APK/AAB was built. This record does not certify the entire release as ready.

#### Exact target and isolation

| Identity | Verified value |
| --- | --- |
| Retained APK | `reports/candidate-20261023/artifacts/app-release.apk`, 11,614,422 bytes |
| APK SHA-256 (C1 below) | `91E312882F7DACDEAA63B8A994986EEA988AEC696D5F68D519C70DD360C33803` |
| Package / version | `com.finnvek.knittools`, `1.0.0` / code **1**, minSdk **29**, targetSdk **37** |
| Certificate SHA-256 | `C31F2806EFA207CE5669647EC6902A1395788B5C0143AE3527983951F1A37D97` |
| Frozen and current build-input identity | Both `C9B22EA7A65988DAB7105D3D8E2F5808039DE51A0F88940CE37E250D8D0D608A`; **zero changed frozen build inputs** at task start |
| Branch / HEAD | `codex/audit-korjaukset-20261018` / `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f` |

The retained APK was hashed against `artifacts.json` and the supplied expected hash; fresh `apksigner verify --verbose --print-certs` succeeded (v2) and matched the recorded public certificate **before installation**. Packaged version identity was read with `aapt2`. Every executed candidate test independently checked the installed APK hash, certificate, version, non-debuggable/non-test-only flags, production INTERNET/BILLING permissions and absence of CAMERA. Signing preparation, backup, profile refresh and packaging acceptance from 12.6 were not repeated. All nine retained artifacts still match their original hashes, including the APK/AAB/mapping.

Evidence is under **`reports/candidate-runtime-20261023/`**. `source-before.json` distinguishes the full current dirty checkout from the frozen build inputs. `candidate-identity.json`, `signature.txt`, `badging.txt`, each run's `identity.json` and `candidate-runtime/runtime-identity.txt` bind the tests to C1. The source tree was inspected, but persistence claims below come from UI observations and supported backup exports.

| Disposable device | System / conditions |
| --- | --- |
| **D37:** `KnitToolsCandidateRuntime37`, `emulator-5590` | Newly created API **37**, Android 17, Google APIs x86_64 image `android-37.0` revision 8; fingerprint `google/sdk_gphone64_x86_64/emu64xa:17/CE2A.260420.051.A3/16571990:userdebug/dev-keys`; WebView **145.0.7632.218**; actual AOSP camera `com.android.camera2` **2.0.002 / 20002000** |
| **D29:** `KnitToolsCandidateRuntime29`, `emulator-5592` | Newly created API **29**, Android 10, Google APIs x86_64; fingerprint `google/sdk_gphone_x86_64/generic_x86_64:10/QSR1.211112.011/13135432:userdebug/dev-keys` |

Both AVDs live only inside this task's report directory. All device commands used explicit serials; no existing installation or personal phone was changed. Emulator networking started with restricted user-mode networking and a non-listening proxy. Before installing C1, non-loopback outbound IPv4 **and** IPv6 were rejected by guest firewall rules, Wi-Fi/mobile data were disabled, and airplane mode was selected. Tests asserted both firewall rules before each data reset; final rule snapshots remain. This leaves the APK's production configuration intact. No live-service verification or deliberate analytics/crash delivery occurred.

The normal display was **720×1600 px at 320 dpi = 360×800 dp**, font **1.0**. Representative overrides were **2560×1600 px (1280×800 dp)** and **1600×800 px (800×400 dp)**. No 320 dp width, 200% font, combination, TalkBack, physical timing or whole adaptive campaign was repeated.

#### Executed results and harness corrections

| Run directory | Actual result |
| --- | --- |
| `api29-startup` | Initial **harness failure**: the new Kotlin class was absent from the driver APK; no candidate assertion passed in this attempt. |
| `api29-startup-retry` | **1/1 passed**, **17.470 s**: empty startup, project creation/count, all five tabs, reopening and force-stop/relaunch persistence. |
| `api37-suite-attempt1` | **5 passed / 2 failed**, **282.498 s**. Failures were the driver's external MediaStore URI grant and obsolete launcher-widget selector. This run is not relabeled as a fully passing suite. |
| `api37-targeted-retry` | **2/2 passed**, **94.767 s**: complete PDF/bookmark/annotation/external-open and widget journeys after harness-only corrections. |

The original `tools/optimized-smoke.ps1`, `SmokeDevice.kt`, `OptimizedSmokeTest.kt`, their isolated-package/testOnly/permission guards, and the baseline-profile generator remain byte-identical. A separate report-local `CandidateDevice` / `CandidateRuntimeTest` copies the applicable UI interactions, checks C1's production identity, requires explicit disposable-emulator opt-in and offline firewall rules, and uses normal app/system UI. Its build graph contains only `:baselineprofile:` tasks; the installed app was always the retained C1 APK.

The initial successful driver SHA-256 is `8CC4AD0BACB386486469C0F7FA97BD4DA6CCB2E68019FEA453C6BE497821499E`; the targeted-retry driver is `1EA821A559BFD74961EFFBEC1212F1B936A4FB85C5B74F9D18F9B0B91A2019B9`. The original driver APK/sources are retained in `api37-suite-attempt1/`; final sources are in `driver-src/`. The APK under test did not change between runs.

The PDF harness failure was a `SecurityException` in the **sending driver**, before the external intent reached KnitTools: API 37 rejected its MediaStore URI grant. The final driver sends the same synthetic PDF through its own unexported, read-only, grant-capable fixture provider. This provider is exclusively in the external driver and cannot access the candidate database or entitlements. An attempted AndroidX helper dependency failed existing metadata verification; it was removed, with no verification relaxation or repository dependency change. The final driver needs no added dependency. The launcher failure referenced the old `widgets_search_bar_edit_text`; actual API 37 UI inspection identified the new Browse → KnitTools → preview → Add path. Those observations and both original failure logs/screenshots remain. The final PDF test also explicitly jumps from page 1 back to its saved page-2 bookmark.

Root operations on these userdebug emulators were limited to network isolation, read-only app-owned file-name/count checks for camera cleanup, and terminating the exact stopped/saved candidate process for restoration and widget launch. There was no database injection/read, entitlement forgery, APK patch, debuggability change or production test hook. Log inspection found no candidate `FATAL EXCEPTION` or ANR in the completed runs; no retrace or production fix was needed.

#### Candidate-specific R01–R13 matrix

All shortened hashes below refer to the complete **C1** hash above. Evidence aliases, relative to `reports/candidate-runtime-20261023/`: **M** = `api37-suite-attempt1/candidate-runtime/`; **T** = `api37-targeted-retry/candidate-runtime/`; **N** = `api29-startup-retry/candidate-runtime/`. Raw assertions/results are in the adjacent `instrumentation.txt`; `driver-src/CandidateRuntimeTest.kt` records the final executed steps. Pulled directories are cumulative device snapshots: only the named run's executed methods and cited files are credited; copied older files do not establish a new pass.

| ID | Actual steps and assertions | APK / device | Status | Evidence | Exact remaining external requirement |
| --- | --- | --- | --- | --- | --- |
| R01 | Clear only disposable C1 data; launch offline; observe empty Projects and usable New Project; successfully enter creation. Also independently passed on minimum API. | `91E31288…C33803` / D37 API 37; D29 API 29 | **PASSED locally** | M `R01-empty.{png,xml}`; N `API29-empty.{png,xml}` | None for local empty startup; service delivery is separate. |
| R02 | Create project; count 0→1→2→1; Undo→2; reopen; later count 3 survives force-stop/relaunch. Minimum-API test separately preserves count 1. | `91E31288…C33803` / D37 API 37; D29 API 29 | **PASSED** | M run `projectCounterSessionAndNavigation`; N `API29-reopened.{png,xml}` | None for this bounded journey. |
| R03 | Open overview, edit name to `Edited smoke project`, Back, visit Library/Tools/Insights/Settings/Projects, reopen the same named counter with count 2. Minimum-API navigation/reopening also passes. | `91E31288…C33803` / D37 API 37; D29 API 29 | **PASSED** | M same method; N `minimumStartupNavigation` | No external blocker for this route sequence; not an exhaustive intent/back-stack campaign. |
| R04 | Start real local work session; add one count; force-stop/relaunch; stop and save; Insights shows matching project, **1 display minute** and **counter progress 1**; Start is available again. | `91E31288…C33803` / D37 API 37 | **PASSED** | M `R04-insights.{png,xml}` and method assertions | None for the short-session smoke; no new reboot/24-hour acceptance claim. |
| R05 | Generate small two-page local PDF outside C1; import through SAF; read page 2; create bookmark and pen stroke; restart/reopen page 2 with visible stroke; go to page 1 and use bookmark to return to page 2. Supported export contains one annotation, bookmark and document relation. | `91E31288…C33803` / D37 API 37 | **PASSED** after harness correction | T `R05-annotation`, `R05-reopened`, `R05-bookmark-jump` PNG/XML; T fresh `candidate-runtime-pattern-*.knittools-backup` | None for local PDF journey; authenticated network download belongs to R10. |
| R06 | Invoke actual AOSP camera for both progress and pattern photos; cancel and capture/accept both paths; create/reopen one-page pattern PDF. Assert **0 canceled progress files**, **1 saved progress file**, **0 remaining pattern captures**, **1 final PDF**. Installed C1 requests no CAMERA permission. | `91E31288…C33803` / D37 API 37 | **PASSED on emulator handler** | M `R06-progress-capture`, `R06-pattern-capture`, `R06-reopened` PNG/XML; M `media-*.txt`; `installed-package.txt` | Physical/OEM camera-handler behavior and image quality remain separate; this does not replace device coverage. |
| R07 | Create/edit yarn card and HTTPS web-pattern entry; restart and observe edited names; after backup restoration observe yarn and web-pattern details/URL again. | `91E31288…C33803` / D37 API 37 | **PASSED** | M `R07-R08-restored.{png,xml}`; M backup has one yarn card and one saved-pattern row; `export-table-counts.json` | None for local metadata persistence; external website availability was not exercised. |
| R08 | Export uniquely named backup through SAF; create `Not in backup`; restore exact exported file with normal replacement confirmation; original project/count 1/yarn/pattern remain, added project disappears. | `91E31288…C33803` / D37 API 37 | **PASSED** on disposable data | M `R08-exported`, `R07-R08-restored` PNG/XML; M `candidate-runtime-backup-413498.knittools-backup` | None for same-version local restore; no personal/cloud/cross-version restore claim. |
| R09 | Dark→Light; English→Suomi; verify Finnish settings; force-stop/relaunch still Finnish; return to English and use Tools. | `91E31288…C33803` / D37 API 37 | **PASSED** | M `R09-dark`, `R09-finnish` PNG/XML; `themeLanguageAndOfflineBrowser` | None for these supported resources/recreation; not a repeated language campaign. |
| R10 | Original local run: Tools→Ravelry WebView; expected HTTPS search URL and `ERR_INTERNET_DISCONNECTED`; wide resize retains URL; stop/save Activity, kill exact process, reopen via Recents; same offline URL; Back returns to Tools. The same C1 subsequently passed manual sign-in, real two-page Activity and saved-task/Recents restoration with Back, website PDF download → Save pattern → Library/render/reopen, and second-import cancellation. | `91E31288…C33803` / D37 API 37 locally; Pixel 9 API 37 live | **PASSED required local + signed-candidate live assertions (10.9)** | M `R10-offline-browser`, `R10-restored-browser` PNG/XML; M `terminated-*.txt`; `reports/r10-live-20261023/phone-live/verified-results.json` and 10.9 | No required live assertion remains. Private temporary-file deletion is not independently verified on the non-debuggable phone. The incidental notification screenshot was excluded and subsequently removed by the user; its absence was verified read-only. See 10.9 for the URL-only fallback and exact evidence boundary. |
| R11 | With no injected state, second active project is blocked by the free gate; choose normal Start free trial; second project becomes creatable and remains available after restart. Other gated local workflows use the same normal trial action. Installed release is non-debuggable and has no test-only identity. | `91E31288…C33803` / D37 API 37 | **PARTIAL overall; free/trial assertions passed** | M `R11-free-gate.{png,xml}`; `freeGateTrialAndAdaptiveTransitions`; runtime identity | Genuine expired local trial: notice/recreation/process-restoration/acknowledgement and retained-content/new-feature boundaries on the signed candidate. Separately, authorized Play test distribution for purchased, restored-after-reinstall and refunded/revoked states plus entitlement refresh. No clock manipulation, purchase or fabricated entitlement was used. |
| R12 | Place real launcher widget; select synthetic project through normal app; count 3 appears; stop/save and kill process; widget opens correct project/count; app increment→4 appears on widget. External ACTION_VIEW + normal URI grant opens Save pattern, saves to Library and survives restart (second PDF row observed). Forged counter-launch token is rejected to Projects. | `91E31288…C33803` / D37 API 37 | **PASSED** after harness correction | T `R12-widget-launch`, `R12-widget-count`, `R12-external-pdf-saved` PNG/XML; T `terminated-*.txt`; M browser method's forged-token assertion | None for these local entries. Widget +/- buttons, other launchers/OEMs and all intent combinations were not claimed. |
| R13 | Compact→1280×800 dp wide→800×400 dp short→compact; retain `Trial project` and count 1; navigate five tabs; restart/reopen with count 1. Wide rail and short-window bottom navigation visually checked. | `91E31288…C33803` / D37 API 37 | **PASSED representative scope** | M `R13-2560x1600`, `R13-1600x800`, `R13-720x1600` PNG/XML | Physical/foldable/OEM performance and a full adaptive recertification remain outside this task. |

#### Reproduction and closure

Final driver build: `pwsh -NoProfile -File reports/candidate-runtime-20261023/build-driver.ps1` (**58 s**, 12 executed / 21 up-to-date). This uses the established project-local Gradle home/cache, Temurin 17.0.20 and Gradle 9.8.1, with `--no-daemon`, an allowlisted driver-only graph and no signing wrapper. `start-emulator.ps1`, `install.ps1` and `run.ps1` retain the exact disposable-device, offline, identity and expected-result gates. To reproduce, use fresh run/evidence directories; preserve the retained C1 artifact. The final sources, rather than the initial copy-generation script, are authoritative for the corrected driver.

Executed main selection: `projectCounterSessionAndNavigation,freeGateTrialAndAdaptiveTransitions,themeLanguageAndOfflineBrowser,yarnWebPatternAndBackupRestore,pdfImportBookmarksAnnotationsAndExternalShare,widgetLaunchAndCounter,cameraCaptureAndCancellation`. Targeted retry: the final two PDF/widget methods only. API 29 selection: `minimumStartupNavigation` only. No redundant full-suite rerun or API multiplication was used after the focused corrections passed.

Only this plan changes among the pre-existing project files; report-local drivers/evidence remain ignored. Branch, HEAD, index, production inputs, original artifacts and unrelated work are preserved. `results.json` records before/after hashes and actual test outcomes. `git diff --check` and a separate whitespace check of this previously untracked plan pass. Both task-created emulators were stopped by explicit serial; recorded emulator descendants and the final single-use Gradle daemon have exited (`process-cleanup.json`). No unnecessary task process remains.

**Remaining acceptance at completion of this local run:** signed-candidate authenticated Ravelry behavior/downloads (subsequently passed in 10.9); genuine expired-trial and authorized Play purchase/restore/refund states; physical/OEM camera/widget behavior and physical performance; approved service-delivery/mapping-upload verification and distribution/CI checks. These were not attempted here. No key generation/backup, other-project/credential-store access, global Gradle-property access, Play action, upload, deployment, commit, push or PR occurred. This local completion does not close those external release requirements.

### 10.8 Signed-candidate R10 phone installation; live testing deferred — 2026-10-23

**Historical installation checkpoint.** The following records the user's initial install-only stopping point. The user later resumed testing and completed manual login; 10.9 records the subsequent live results and supersedes the deferred status.

**Exact candidate installed on the authorized phone and left unlaunched; R10 remains PARTIAL overall.** The user stopped the disposable emulator because of RAM usage and requested the connected phone instead. Read-only preflight initially blocked installation because its debug APK had a different certificate and the original request prohibited replacement. The user then explicitly authorized replacing that installation and directed that the new version be installed **without testing yet**. That later instruction supersedes the earlier installation restriction. No application defect was established.

Evidence is retained in ignored `reports/r10-live-20261023/`. Fresh SHA-256 and `apksigner verify --verbose --print-certs` checks before installation confirmed the retained APK against `artifacts.json`: **91E312882F7DACDEAA63B8A994986EEA988AEC696D5F68D519C70DD360C33803**, package **com.finnvek.knittools**, version **1.0.0 / 1**, certificate **C31F2806EFA207CE5669647EC6902A1395788B5C0143AE3527983951F1A37D97**, valid v2 signature. The current build-input identity still equals the frozen candidate identity **C9B22EA7A65988DAB7105D3D8E2F5808039DE51A0F88940CE37E250D8D0D608A**, with zero changed frozen inputs. The candidate was not modified, re-signed, repackaged or rebuilt.

The authorized **Pixel 9**, Android **17 / API 37**, fingerprint `google/tokay/tokay:17/CP3A.261005.005/16271449:user/release-keys`, has WebView **155.0.8059.43**. Read-only preflight and a pulled installed APK established the previous **debuggable** installation's SHA-256 **63ACD372EE22161D3A1C7E9C08EFAD15D75107228B36031290F5E654A72CA3B6** and certificate **F6ED10FCF19C9AA5F6A31A53358CC245F487FE5B1765E7028080E020AEBB7D05** (`phone-signature-before.txt`, `phone-preflight.json`). This was the earlier ordinary-debug artifact from 6.10, not C1; the initial blocked-preflight records remain historical evidence.

After replacement authorization, the old app was stopped solely to take a consistent local backup. `phone-before-release-install.tar` retains `knittools.db`, its WAL/SHM, `files/datastore` and `files/pattern_pdfs`: **13 files / 732,846 content bytes**, with every archived file's SHA-256 compared against the stopped app. Archive SHA-256: **41BFCF51689048869362D01D0BBDC6C28F13A282403908C67B295E2DB97295DE**. `phone-backup.json` records the scope. WebView stores/cookies, shared preferences, service credentials and caches were excluded. This is a **technical recovery archive, not the app's importable backup format**; no data was restored into C1. The old APK is also retained. No personal content was printed.

`adb uninstall com.finnvek.knittools` and `adb install --no-incremental <retained-C1-APK>` both succeeded on this explicit phone (`phone-remove-debug.txt`, `phone-install-release.txt`). Fresh on-device APK hashing equals C1 exactly, binding the installed bytes to its verified release certificate. Package metadata reports version **1.0.0 / 1**, neither DEBUGGABLE nor TEST_ONLY, **stopped=true**, **notLaunched=true**, and no app process (`phone-installed-release.json`). The new installation has not been opened or runtime-tested. The old app's on-device data was removed by the authorized uninstall; the scoped local recovery archive remains available. No system/service setting was changed.

The task-created visible emulator **KnitToolsR10Live37 / emulator-5594** was interrupted during preparation, before any verified candidate installation or live assertion. Its recorded launcher/child processes have exited. Automatic approval review rejected the command containing deletion of the task-owned AVD directory with `blocked by policy` and no further reason; the stopped AVD files remain on disk and deletion was not retried. No other device or process was stopped. `cleanup.json` records cleanup and `preservation-final.json` records source/artifact preservation. Only this plan changes among pre-existing project files.

**Not executed, by the user's explicit stopping point:** manual login; Activity recreation and signed-in history Back; background process replacement with retained-task/Recents evidence; actual website PDF download, native Save pattern, Library save/render/reopen, second-import cancellation and private-file cleanup assertions. No new Ravelry session or test Library entry was created, so no session/content cleanup was needed. Usage analytics was not enabled and no service setting was changed. No local journey/API 29/adaptive campaign was repeated. Stopping the old debug app for backup is not saved-task restoration evidence.

Continuation requires the user to resume testing and manually sign in on Ravelry's page in the installed candidate. Do not start the live journey while the user is unavailable. The previous local results remain valid within 10.7; R11 entitlement checks, physical performance, service/distribution/CI acceptance and other release boundaries remain separately open. No production defect, whole-release readiness, commit, push or PR is claimed.

### 10.9 Signed-candidate R10 authenticated live verification — 2026-10-23

**R10 required live assertions PASSED on the retained C1 APK.** The user resumed testing on the connected Pixel 9, unlocked it and manually signed in directly on Ravelry's own page. No production defect was confirmed and no production input, state budget, APK, profile or signing configuration changed. This closes the live requirement left open by 10.7/10.8, within the observations below; it does not certify the whole release.

Identity: APK SHA-256 **91E312882F7DACDEAA63B8A994986EEA988AEC696D5F68D519C70DD360C33803**, package **com.finnvek.knittools**, version **1.0.0 / 1**, release certificate **C31F2806EFA207CE5669647EC6902A1395788B5C0143AE3527983951F1A37D97**. The installed APK hash was checked again after the live journey and still matched. Device: **Pixel 9, Android 17 / API 37**, fingerprint and pre-install identity in 10.8, WebView **155.0.8059.43**, normal display **1080×2424 px / 420 dpi**, font scale **1.15**. No display/font/accessibility campaign was repeated.

Evidence below is relative to ignored `reports/r10-live-20261023/phone-live/`. `verify-evidence.py` validates the recorded UI/lifecycle assertions in `verified-results.json`; it does not replay the journey or inspect credentials. Native accessibility observations exclude notification/IME windows. Web observations retain only explicitly allowed public labels, structural metadata and account/logout semantics; no raw website layout or signed download URL is retained. A manual-login guard prevented observations while the user entered credentials. No WebView debugging, fixture injection or standalone HTTP download was used.

| Check | Actual observation and evidence |
| --- | --- |
| Authentication | User confirmed manual login. Website access/downloads continued through both restoration mechanisms without further login. Afterward a real account-menu logout control was present (`authenticated-logout-control.json`); normal logout returned to a signed-out page and Username/Password/Log In form (`signed-out*.json`). No cookie/token inspection or private account-content evidence is claimed. |
| Activity recreation + Back | Real website search opened **Flax DK**; its related-pattern link opened **Flax worsted**. System night mode change triggered MainActivity stop/destroy/create/resume at **20:38:29 Europe/Helsinki**, retaining **task 27104 / PID 31895**. Current page remained Flax worsted; actual Android Back returned to Flax DK with the browser still open. `two-pattern-activity-{before,after}.json`, `two-pattern-recreation-events.txt`, `two-pattern-{before-recreation,after-recreation,back-after-recreation}.json`. |
| Background process replacement + Recents + Back | Re-established Flax DK → Flax worsted; pressed Home and waited for **STOPPED**, **mHaveState=true**, saved Activity Bundle **17,300 bytes** (not a measurement of the WebView budget). Only then `am kill com.finnvek.knittools` terminated the background process. Evidence confirms absent process, saved state and retained **task 27104**. Opened real Recents and selected its KnitTools card: same task, **new PID 3499**, Flax worsted restored, actual Back returned to Flax DK. `process-stopped-saved.json`, `process-after-kill.json`, `recents-before-resume.json`, `process-after-recents.json`, `after-recents-restoration.json`, `back-after-process-restoration.json`. This was deliberate background termination, not force-stop, launcher cold start or naturally occurring memory pressure. |
| Actual permitted PDF download | On the real **free Flax DK** page, tapped the site's Download and download PDF controls after restoration. Native Save pattern became ready. Named the single entry **R10 Test 20261023** and selected Save to Library only. `real-download-dialog.json`, `first-pending-save-ready.json`, `first-title-ready.json`, `first-saved-entry.json`. No purchase was required or made. |
| Render / leave / reopen | Open Pattern visibly rendered the PDF cover. Native reader evidence shows its image viewport and **Page 1 of 8**. Returned through detail to Library, reopened the same entry and observed the rendered-reader viewport/page count again. `first-pdf-rendered-controls.json`, `saved-pattern-list-first.json`, `first-pdf-reopened-controls.json`. No pixel-diff or private PDF byte-identity claim is made. |
| Second import cancellation | Repeated the site's actual free PDF controls, waited for the second native Save pattern sheet, then Cancel. The sheet closed; Saved Patterns still contained exactly the original named entry. That entry reopened with its PDF viewport and **Page 1 of 8**. `second-real-download-dialog.json`, `second-pending-save-ready.json`, `second-import-canceled.json`, `saved-pattern-list-after-cancel.json`, `pdf-reopened-after-cancel.json`. |
| App cleanup/settings | Removed only R10 Test 20261023; Library returned to **0** saved patterns. Signed out using Ravelry's actual logout control. Restored Keep screen awake **OFF** and original system night mode **yes**. Share usage statistics remained **OFF**. `cleanup-library-empty.json`, `signed-out*.json`, `settings-restored.json`, `cleanup-settings-before.json`, `final-environment.json`. The signed candidate remains installed. |

**Fallback and failed-preparation boundary:** the earlier search → Flax DK recreation retained Flax DK but Back returned to Tools (`short-history-{before,after}-recreation.json`, `short-back-after-recreation.json`). This is recorded as observed URL-only fallback, not a history pass; the exact internal fallback trigger was not instrumented. The later two-pattern route passed both required history checks with unchanged production policy/budget. Earlier menu/scroll preparation and an immediate PAUSED/not-yet-saved background observation are not counted as successful assertions; process termination waited for the later STOPPED/saved record. No app defect was established by those preparation/fallback observations.

**File-evidence limit:** `run-as` refused the production package as non-debuggable (`private-file-evidence-boundary.json`). No root/debugging bypass was attempted. Cancellation and final Library deletion are verified at the UI boundary only; private temporary-file cleanup and physical deletion of the saved PDF are **not independently verified**. This explicitly limited claim does not turn the optional safe-file observation into a false pass.

**Incidental capture cleanup COMPLETE:** one PDF screenshot unexpectedly included a personal notification banner. It is excluded from evidence and must not be published or reused. Automatic approval review rejected deletion of the exact local `first-pdf-rendered.png` with **blocked by policy**, without further reason. No bypass was attempted. The user subsequently confirmed manual deletion; an exact-path read-only check verified the file was absent (`phone-live/screenshot-cleanup-confirmed.json`). All reported render/reopen results cite the sanitized native observations instead. No login-entry screenshot was taken. The task-owned AVD directory was absent at the final read-only check and its recorded processes had exited; unrelated emulator-5590 was not targeted. One-shot live helpers exited naturally and no temporary server/watcher/Gradle process was started for this live continuation.

Only this plan changes among pre-existing project files; ignored report-local scripts/evidence retain the sequence. Final source/artifact/index/branch checks are recorded in `reports/r10-live-20261023/preservation-live-final.json`. Earlier seven local journeys and API 29 results were not rerun. No build, entitlement/trial-expiry test, Play purchase/restore/refund, deliberate telemetry delivery, service-setting change, upload, deployment, commit, push or PR occurred. **R11, physical/OEM/performance, service/distribution/CI and unrelated security acceptance remain separately open.**

## 11. Work package 5A: startup readiness and timing

### 11.1 Define readiness before implementing the signal

For an ordinary launcher start into Projects, define usable readiness as:

- stored app language has been applied;
- initial theme/preferences have resolved;
- initial launch-request routing has resolved;
- the initial destination is composed;
- the actual initial project/folder data required for that destination has loaded, including a legitimately empty result;
- any essential gate affecting the visible controls has resolved through its existing success/failure behavior.

Do not require project count to be greater than zero. Do not wait for unrelated thumbnail downloads, remote Ravelry content, analytics, update checks, or optional background work.

Inspect active-session/Continue-hero inputs and determine which are necessary for the initially interactive Projects surface. Avoid reporting readiness while a default empty seed is still standing in for the real data.

If a visible error state is the final usable initial surface, define and test that case explicitly. A timer that blindly marks the app ready is not a substitute for a loading/error model.

### 11.2 Implementation

1. Introduce the smallest explicit initial-load state needed in the current project UI owner.
2. Distinguish `Loading` from `Loaded(emptyList())`.
3. Reuse the folder loading state where valid, but do not assume it proves the separately collected project list has emitted.
4. Keep actual data loading in existing repositories/ViewModels.
5. Add `ReportDrawnWhen` at an appropriate composition scope where it observes the defined condition.
6. Register the readiness wait early enough that the initial composition cannot report ready before asynchronous initialization starts.
7. Report once per Activity launch; do not reset the startup metric on every tab switch, sort change, or later refresh.
8. Handle deep-link/share/widget launches with the readiness of their actual initial destination rather than waiting forever for an invisible Projects route.
9. Preserve existing splash, language, and theme behavior; do not add blocking startup reads.
10. Do not send this signal through a new analytics event.

`ReportDrawnWhen` provides a readiness signal. The benchmark provides the measurement. Keep those responsibilities distinct.

### 11.3 Startup test cases

| ID | Case | Expected measurement behavior |
| --- | --- | --- |
| S01 | Fresh installation with zero projects | TTID and TTFD are produced; empty data is ready after loading. |
| S02 | Small synthetic populated database | Readiness follows the actual initial data. |
| S03 | Delayed initial Room/preferences emission | No premature ready signal. |
| S04 | Visible terminal initial error state | Defined behavior, without indefinite metric waiting. |
| S05 | Stored nondefault language and dark theme | Initial ready surface uses the selected configuration. |
| S06 | Existing active work session | Readiness does not reset/start/finalize the session. |
| S07 | Widget/deep-link/share launch | Actual destination becomes ready without waiting on an absent Projects screen. |
| S08 | Repeated later updates/navigation | No duplicate startup completion event within the same launch. |

### 11.4 Macrobenchmark implementation

1. Add a separate startup benchmark class in the existing `baselineprofile` test module if its plugin configuration supports the required benchmark task.
2. Use `MacrobenchmarkRule.measureRepeated` with `StartupTimingMetric`.
3. Keep the profile generator separate from the timing benchmark.
4. Use an optimized, non-debuggable, profileable target and verify the effective variant.
5. Run cold-start cases for both empty and representative populated state.
6. Prepare deterministic data outside the measured block. Do not measure fixture creation or website latency as app startup.
7. Use a UI-ready condition tied to actual content semantics; avoid fixed sleeps.
8. Collect both initial-display and full-display timing where the signal is available.
9. Use explicit compilation modes to compare a baseline-profile-required run with an appropriate no-profile compilation baseline.
10. Use enough repeated iterations for a useful comparison; start with ten per scenario/mode on a controlled physical device and inspect variance before drawing conclusions.
11. Record device, OS, thermal conditions, compilation mode, app/data state, and artifact identity.
12. Keep benchmark JSON and Perfetto traces as local/CI evidence.
13. Emulator runs may verify that the harness works. Use a physical device for conclusions about performance improvements.

Do not claim that Play vitals displays this app-defined TTFD. Its startup reporting and the local Macrobenchmark/Perfetto measurements are separate. [Macrobenchmark metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics), [Android vitals launch time](https://developer.android.com/google/play/vitals/launch-time)

### Acceptance

- [x] Empty and populated startup both produce valid readiness evidence.
- [x] Readiness is based on actual loading completion.
- [x] Benchmark setup does not contaminate the measured block.
- [x] Results identify compilation and profile state.
- [x] No speedup percentage is claimed from an emulator-only or uncontrolled comparison.

### 11.5 Implemented readiness and runtime evidence (2026-10-22)

This work is limited to section 11. Evidence is under `reports/startup-20261022/` on the existing dirty feature branch `codex/audit-korjaukset-20261018`, starting at `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. It does not supersede the production signing/package blocker, T08, live W11/R10, remaining R11 checks, the separate INC-SIGN-01 owner action (4.1), or unrelated WebView/security findings.

Readiness ownership:

- `MainActivity` registers `ReportDrawnWhen` before the language/theme early returns. A stable snapshot reader observes applied language, real preferences, resolved launch requests and passive trial notice, the actual destination, and removal of the existing splash. The splash keep condition, exit duration and removal behavior are unchanged; readiness observes them without extending them. AndroidX owns the one-shot completion, so later state changes cannot emit another completion for that Activity.
- The one-shot reporter listener also invalidates the decor view. The inspected AppCompat 1.8.0 `setContentView` delegates directly and bypasses Activity 1.13.0's content-view draw-listener registration. Without this redraw, the recorded ready signal could follow the final rendered frame and Macrobenchmark 1.5.0 failed with `No RT frame slice associated with UI thread frame slice ends after reportFullyDrawn`. The redraw pairs the unchanged ready condition with a rendered frame; it adds no timer, splash extension or visual content.
- `ProjectListViewModel.content` combines the first real preferences, SQL-sorted active projects, completed projects when shown, active-session observation, folder organization/filter, and completion of the existing session refresh. Its initial `isLoading=true` is distinct from a loaded empty result. Folder retry failures remain loading until a real snapshot arrives. The Continue hero is derived from the same project snapshot; optional document labels and photos do not block it.
- `CounterUiState.startupReady` requires the selected project and preferences, settled gates, active-session observation/initial refresh, and reminders/counters/document metadata for that project ID. The existing repository/session operations remain the owners; reporting adds no session start/stop/finalization.
- `ProManager.initialControlsReady` follows completed trial initialization plus either the existing purchase-read success or exhausted connection/query failure. It does not change `initialStateReady`, purchases, entitlements, or retry policy. Pending retries do not qualify as completion.
- Navigation accepts readiness only from the current back-stack entry. Counter/widget launches wait for their selected counter; missing projects fall back to the loaded Projects destination. The locked-widget prompt waits for its own controls. PDF VIEW/SEND waits for the local copy and real project/gate choices in the Save pattern sheet. Shared web links wait for the editor to consume the request; a displayed invalid-link error with editable controls is a usable terminal surface. Ravelry waits only for the composed local browser, never the remote page. The Pro route also waits for its existing product-loading state to settle.
- Repository read retries and unresolved preference/trial initialization remain unready; no timeout turns them into success. A failed incoming PDF copy uses the existing error notification and underlying destination readiness after the copy attempt settles. No analytics event, blocking startup read, dependency upgrade, profile generation, or startup framework was added.

`StartupReadinessTest` observes the real `MainActivity.fullyDrawnReporter`, real splash and isolated Room database. It follows lifecycle callbacks directly because consumed PDF/share intents change identity during `onCreate`. `StartupReporterTest` verifies the early-return/loaded-empty/one-shot contract using the real AndroidX reporter; owner-level delayed-flow tests verify the actual ViewModel inputs. These are scoped cold-entry/readiness checks, not a new all-screen restoration or adaptive campaign.

| Case | Executed readiness evidence |
| --- | --- |
| S01 | Empty launcher reports once after the loaded-empty surface. `ProjectStartupReadinessTest` and the real reporter test distinguish no emission from empty data. Timing evidence is recorded below. |
| S02 | A real Room project with count 24 appears before readiness; the Continue hero follows the same data. The timing fixture below uses three synthetic projects. |
| S03 | Independently delayed preferences, active/completed projects, folders, session observation and refresh; counter preferences/session/gates/refresh; no premature ready value. The Android reporter remains pending across language/theme early returns and reports on loaded-empty data. |
| S04 | Invalid shared text opens the editable error surface and reports once. A simulated folder-read failure stays loading until a later real snapshot. The ProManager test distinguishes pending billing from its existing terminal unavailable fallback. |
| S05 | Stored Finnish/dark configuration is asserted at actual completion, including locale and status-bar appearance; screenshot `startup-finnish-dark.png` inspected. A separate optimized cold-process check is recorded below. |
| S06 | Launcher and trusted counter entry preserve an existing session token/start timestamp/start row, project count 24 and zero completed sessions. Optimized cold-process/measurement checks are recorded below. |
| S07 | Trusted widget counter, missing-project fallback, locked-widget prompt, web share, invalid share, Ravelry share, PDF `ACTION_VIEW` and PDF `ACTION_SEND` all reach their actual initial surface. No remote Ravelry success or physical widget-host campaign is claimed. |
| S08 | Later tab navigation and a Room insertion leave the Activity completion count at one. The reporter test also changes ready data back to pending and then to new data without another completion. |

Validation already executed: ordinary root `test` **2,183/2,183 passed**, including the new owner/gate cases; `StartupReadinessTest` plus `StartupReporterTest` **8/8 passed** (`instrumentation-3.txt`); existing `TrialNoticeRestorationTest`, `ProjectOverviewNavigationTest` and `IncomingPdfDiscardBoundaryTest` **20/20 passed** (`regression-instrumentation.txt`). The first iterations exposed and then corrected test fixture/selector/lifecycle issues; the final results above are the reruns, not those failed attempts. The forbidden 320 dp, 200% font, combined and TalkBack checks were not run.

### 11.6 Timing harness and comparison boundary

`baselineprofile/src/benchmarkRelease/.../StartupTimingBenchmark.kt` contains separate empty/populated cold-start tests using `MacrobenchmarkRule.measureRepeated` and `StartupTimingMetric`. It waits for the root `startup_ready` semantics plus the expected content. Fixture creation and session checks run outside the measured block. The section-11 change left the generator, optimized smoke tests and CI smoke selection unchanged. Section 12 subsequently extends the generator and shares fixture setup while retaining the measured block and CI selection.

The disposable-emulator fixture uses the existing guarded `.optimizedtest` reset, normal UI project creation and normal local trial action. Populated state is three active projects (scarf 4, socks 8, cardigan 12), the cardigan Continue hero, one running session and zero completed sessions. Before/after read-only SQL checks compare its token, project, start timestamp/start row and completed-session count. Empty state has no projects or sessions. Physical-device execution never resets data: it requires a separately prepared synthetic fixture and the matching `startupPreparedFixture=empty|populated` argument.

Compilation modes are explicit:

- `emptyNoProfile` / `populatedNoProfile`: `CompilationMode.None()`.
- `emptyRequiredProfile` / `populatedRequiredProfile`: `CompilationMode.Partial(BaselineProfileMode.Require, warmupIterations = 0)`.

**Historical section-11 prerequisite, now resolved for the optimized test APK by sections 12.2–12.4:** at section-11 completion, `app/src` had no baseline/startup profile sources and the merged baseline had 5,507 dependency rules with **zero** app-owned rules (`profile-source-inventory.json`, `merged-profile-inventory.json`). The new generated app profile is reviewed and packaged, and both required-profile methods and their corresponding `None` methods have now passed against the same rebuilt APK. `None` describes compilation state, not the absence of profile assets. The section-11 APK numbers below are historical evidence and are not used for the new comparison. Physical timing and final signed production packaging remain pending.

Build command (PowerShell):

```powershell
./gradlew --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' :app:assembleBenchmarkRelease :baselineprofile:assembleBenchmarkRelease
```

The effective `benchmarkRelease` target is minified, resource-shrunk, non-debuggable and shell-profileable. It uses release source implementations, package `com.finnvek.knittools.optimizedtest`, `testOnly=true`, version `1.0.0` / code `1`, and the existing debug signing configuration. Compared with production it also removes INTERNET/BILLING/CAMERA permissions and disables Crashlytics collection/PostHog. These existing isolation differences make this a harness artifact, not the signed production release. Effective-variant/manifest evidence, APK/driver/mapping hashes and signing-certificate digest are in `build-optimized-1.log`, `optimized-effective.json`, `optimized-artifacts.json` and `optimized-cert.txt`.

Executed emulator commands/results and trace inspection are recorded below. For a later authorized physical comparison, start with ten iterations per fixture/mode, inspect variance and thermal state, and repeat as required by 11.4. For example, after the section-12 prerequisite and preparation of the empty synthetic fixture:

```powershell
adb -s <authorized-physical-serial> shell am instrument -w -r -e class com.finnvek.knittools.baselineprofile.StartupTimingBenchmark#emptyRequiredProfile -e startupPreparedFixture empty -e startupIterations 10 com.finnvek.knittools.baselineprofile/androidx.test.runner.AndroidJUnitRunner
```

Run the corresponding `emptyNoProfile`, then the two populated methods with `startupPreparedFixture populated`, under matched conditions. Do not pass the emulator reset opt-ins or emulator error suppression to a physical run. A Pixel 9 appeared in the ADB inventory later in this task; it was not identified as an authorized, controlled test device and was left untouched. A physical run with explicitly prepared synthetic fixtures, and the profile-required comparison, remain open. App-defined fully-drawn reporting is separate from Play vitals. [Android launch timing](https://developer.android.com/topic/performance/issues/launch-time), [Macrobenchmark metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics).

### 11.7 Executed timing evidence and remaining prerequisites

Historical section-11 timing run (the same-artifact profile comparison is now recorded in 12.4): **2/2 methods passed**, three cold launches per fixture, `CompilationMode.None()`, zero warmup iterations (`benchmark-3.txt`, 75.651 s). The exact invocation after installing the optimized app and benchmark driver was:

```powershell
adb -s emulator-5584 shell am instrument -w -r -e class 'com.finnvek.knittools.baselineprofile.StartupTimingBenchmark#emptyNoProfile,com.finnvek.knittools.baselineprofile.StartupTimingBenchmark#populatedNoProfile' -e optimizedSmokeAllowed true -e startupFixtureResetAllowed true -e startupIterations 3 -e androidx.benchmark.suppressErrors EMULATOR -e additionalTestOutputDir /sdcard/Android/data/com.finnvek.knittools.baselineprofile/files/startup-20261022-v3 com.finnvek.knittools.baselineprofile/androidx.test.runner.AndroidJUnitRunner
```

| Synthetic fixture | TTID min / median / max (ms) | TTFD min / median / max (ms) |
| --- | --- | --- |
| Empty, zero sessions | 1,278.0 / 1,328.3 / 1,369.2 | 4,602.3 / 4,642.7 / 4,663.8 |
| Three projects, one existing active session | 1,243.7 / 1,263.7 / 1,299.0 | 4,627.0 / 4,629.3 / 4,669.7 |

These numbers prove local metric collection only. They include the unchanged splash and actual readiness gates. They are not a production latency estimate or an improvement comparison. Before/after session assertions passed for both fixtures; the populated run preserved its existing token, project, start timestamp/start row and zero completed sessions (`startup-populated-session.txt`, `startup-empty-session.txt`). A pinned fixture widget had restarted the target during an earlier cold-start setup, so the explicitly resettable emulator path removes it before measurement; the framework's cold-process checks remain enabled. Fixture creation, removal and SQL inspection are outside the measured block. Earlier failed attempts remain in `benchmark-1.txt` / `benchmark-2.txt` and are not included in the table.

Device and conditions: disposable read-only `KnitToolsOptimized20261021` AVD, `emulator-5584`, Android 16 / API 36, x86_64, model `sdk_gphone64_x86_64`, fingerprint `google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`, four virtual cores with unlocked CPU. The fixture used 720×1600 pixels at density 320 (**360 dp width**), font scale 1.0 and all three animation scales 1.0. Thermal status was 0 before/after, with emulated skin 30.1 °C and battery 30.2 °C; Macrobenchmark recorded zero thermal-throttle sleep. These emulated temperatures do not establish physical thermal stability (`measured-device.json`, `thermal-valid-before.txt`, `thermal-valid-after.txt`).

Final measured artifact SHA-256 identities (`optimized-artifacts.json`):

- App APK: `0D2B2AB3339785AB7D830F138544BBEEBF861A17EB5643D32A121384E49AD546`.
- Benchmark driver APK: `7AD78302299AA68A729D694346A105E697B01E745DF2AD875B51180A9CF904F1`.
- R8 mapping: `FFF2EA256E0F610BD3BED7E40F69DC49DEF76F3D339F0968BDE4D63E0CB32166`.
- Debug signing certificate SHA-256: `f6ed10fcf19c9aa5f6a31a53358cc245f487fe5b1765e7028080e020aebb7d05`.

Preserved raw evidence: `benchmark-output-valid/com.finnvek.knittools.baselineprofile-benchmarkData.json` and all six `.perfetto-trace` files in that directory. Each trace was inspected with Perfetto trace processor v58.2 and `verify-fully-drawn.sql`: **6/6 have exactly one platform `reportFullyDrawn` slice and a completed associated RenderThread frame after the signal**. Per-trace CSV and `trace-validation.json` retain the timestamps. This explicitly verifies the full-display signal, rather than inferring it from a successful UI wait.

`StartupLaunchTest` also passed on this final optimized APK (**1/1**, `cold-config-final.txt`, 17.506 s): persisted Finnish/dark mode, force-stopped process, loaded Continue hero and unchanged existing session. `cold-finnish-dark-final.png` was visually inspected. This configuration/session check is separate from the timing samples and the CI smoke selection.

After the final draw-listener change, the two startup test classes and the three existing regression classes were rerun together against the rebuilt isolated debug APK: **28/28 passed** (`instrumentation-final.txt`, 80.962 s; hashes in `isolated-final-artifacts.json`). Together with the optimized configuration check, this is **29 passing functional device tests**, separate from the two timing benchmark methods/six samples. The earlier full JVM run passed **2,183 tests**; subsequent production changes were limited to the one-shot decor redraw verified by the final device tests and traces. Final optimized assembly, app ktlint and Detekt passed (`build-optimized-2.log`); benchmark-driver assembly and ktlint passed (`build-driver-4.log`).

Final isolated debug app/test assembly and `:app:lintDebug` also **passed** (`build-lint-final.log`, 20m 25s). Lint reports **0 errors / 10 unchanged warnings**, all outside this change: four `MissingOnRenderProcessGone` in Ravelry, two `VisibleForTests` in Insights, one `ModifierParameter` in `ScrollableFormDialog` and three `RtlHardcoded` in `AdaptiveModalWindow`. The original/final XML comparison confirms no new diagnostics (`lint-results-final-debug.xml`, `lint-final-comparison.json`). Validation commands:

```powershell
./gradlew --no-daemon --max-workers=1 test :app:ktlintCheck :baselineprofile:ktlintCheck :app:detekt
./gradlew --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' :app:assembleBenchmarkRelease :app:ktlintCheck :app:detekt
./gradlew --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' :baselineprofile:assembleBenchmarkRelease :baselineprofile:ktlintCheck
./gradlew --no-daemon --max-workers=1 '-Pknittools.isolatedTest=activepolicy' :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s emulator-5584 shell am instrument -w -r -e class 'com.finnvek.knittools.ui.navigation.StartupReadinessTest,com.finnvek.knittools.ui.navigation.StartupReporterTest,com.finnvek.knittools.pro.TrialNoticeRestorationTest,com.finnvek.knittools.ui.navigation.ProjectOverviewNavigationTest,com.finnvek.knittools.repository.IncomingPdfDiscardBoundaryTest' com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5584 shell am instrument -w -r -e class com.finnvek.knittools.baselineprofile.StartupLaunchTest -e optimizedSmokeAllowed true com.finnvek.knittools.baselineprofile/androidx.test.runner.AndroidJUnitRunner
```

Remaining execution boundaries:

- **Physical timing pending:** an authorized, controlled device with isolated synthetic empty/populated fixtures; at least ten iterations per scenario/mode, recorded thermal conditions and variance inspection. The connected physical-device candidate was not modified.
- **Required-profile emulator comparison completed in 12.4:** both `*RequiredProfile` methods passed with `Require`/zero warmup, alongside both `None` methods on the same rebuilt APK; four methods / twelve samples. This resolves the missing-app-profile prerequisite for the test artifact only.
- **Compose Stability remains blocked outside this scope:** its generated metadata confirmed all five new readiness callbacks as STABLE, and only those five baseline function blocks were refreshed. The final check still reports 20 earlier adaptive/UI differences (`stability-final.log`, `stability-summary.json`); they were not suppressed or swept into this baseline update. This is Compose stability metadata, not Android baseline/startup profile generation.

The production signing/package, T08, live W11/R10, remaining R11, INC-SIGN-01 owner action (4.1) and unrelated WebView/security boundaries remain unchanged. No section-12 implementation, profile generation, commit, push, PR, publishing or deployment was performed.

Final preservation check found no changed or new files outside the task-owned set; branch/HEAD remained unchanged and `git diff --check` passed (`preservation-final.json`). The task-owned emulator and its descendant helper processes were stopped after evidence collection (`emulator-cleanup-remaining.json` is empty). No unrelated development process or connected physical device was stopped or modified.

### 11.8 Debug Compose Stability follow-up — 2026-10-22

**Status: the section-11.7 debug baseline differences are resolved.** This separate follow-up supersedes only the Compose Stability blocker above. The original `reports/startup-20261022/stability-final.log` and `stability-summary.json`, and the earlier failed-run record, are preserved unchanged. New evidence is in `reports/stability-followup-20261022/`.

The first owning `:app:debugStabilityCheck` recompiled debug Kotlin and failed with **exactly the same 20 differences: 9 new functions and 11 changed parameter lists** (`check-before.log`, exit 1). Review compared the starting debug baseline, each implementation/caller and the fresh `app/build/stability/compileDebugKotlin/stability-info.json` (retained as `stability-info.before.json`). All 12 added parameters across the 11 existing functions are reported **STABLE**; their existing parameter classifications/reasons and skippable/restartable flags are unchanged. No genuine stability regression requiring a production correction was demonstrated.

| Confirmed difference | Classification and source justification |
| --- | --- |
| `AdaptiveContent` — new | Intentional adaptive API (8.9): provides current constraint/density-derived content dimensions. Both `modifier` and `content` are STABLE. |
| `AdaptiveDropdownMenu` — new | Intentional media/modal API (8.12): bounds an existing popup to the unobstructed pane. All five parameters are STABLE. |
| `AdaptiveModalWindow` — new | Intentional modal integration (8.11): applies/restores fold-aware window geometry. No parameters; generated flags are retained. |
| `UnobstructedWindowContent` — new | Intentional adaptive host (8.9): applies safe insets and observed folds to the existing content instance. Both parameters are STABLE. |
| `rememberWindowFolds` — new | Intentional lifecycle-aware fold observation (8.9). No parameters; returns `List<WindowFold>`. Retain generated `skippable: false` / `restartable: false`; its presence is not a parameter-stability regression. |
| `KnitToolsNavigationRail` — new | Intentional rail presentation (8.9), with an **UNKNOWN advisory** for `navController: NavController`. It observes `currentBackStackEntryAsState()` and uses the existing navigation actions. Retain `skippable: false`. |
| `KnitToolsNavigationScaffold` — new | Intentional shared shell (8.9), with the same **UNKNOWN advisory** for `navController`; its other four parameters are STABLE. The controller comes from the existing single `rememberNavController()` in `KnitToolsNavHost`. Retain `skippable: false`; no navigation wrapper/refactor is justified by this classification alone. |
| `PhotoViewport` — new | Intentional media viewport (8.12): saveable zoom/pan and bounded image placement. `uri`, `description` and `modifier` are STABLE. |
| `InsightsScreenContent` — new | Intentional content extraction (8.13): the existing route collects ViewModel state and passes it with callbacks to adaptive content. All six parameters are STABLE; `InsightsUiState` already carries its existing annotation. |
| `ToolScreenScaffold` — 9 → 10 | Intentional `contentWidthPolicy: ContentWidthPolicy`, STABLE: shared form/list/chart/unrestricted width selection (8.9–8.10). |
| `CounterHero` — 4 → 5 | Intentional `compactHeight: Boolean`, STABLE: existing compact-height counter spacing (8.11). |
| `PhotoGalleryContent` — 4 → 5 | Intentional `gridState: LazyGridState`, STABLE: passes state retained above empty/content branches (8.12). |
| `PhotoGalleryGrid` — 4 → 5 | Intentional `gridState: LazyGridState`, STABLE: uses that same state in the adaptive grid (8.12). |
| `axisLabel` — 3 → 4 | Intentional `spansYears: Boolean`, STABLE: includes the year in monthly labels when needed (8.13). Existing String-returning helper flags remain false/false. |
| `AllPhotosContent` — 7 → 8 | Intentional `gridState: LazyGridState`, STABLE: forwards the screen-owned retained grid state (8.12). |
| `MyYarnList` — 3 → 4 | Intentional `listState: LazyListState`, STABLE: retains scroll position outside loading/empty branches (8.10). |
| `PhotoGrid` — 5 → 6 | Intentional `gridState: LazyGridState`, STABLE: connects All Photos' adaptive grid to that retained state (8.12). |
| `SavedPatternsList` — 3 → 5 | Intentional `listState: LazyListState` and `bottomContentPadding: Dp`, both STABLE: retained scrolling and creation-action clearance inside list content (8.10). |
| `ProjectOverviewMenu` — 4 → 5 | Intentional `anchorWidth: Int`, STABLE: aligns the popup to its action within the selected fold pane (8.11). |
| `RavelryBrowserScreen` — 3 → 4 | Intentional `requestId: String`, STABLE: scopes `rememberSaveable` browser state and its saver to the navigation request (6.2/6.7); this is earlier restoration work, not a new WebView change. |

Only `app/stability/app-debug.stability` was regenerated, using the confirmed `:app:debugStabilityDump` task. The task-relative diff contains exactly those 20 function blocks and no removals or unrelated changes (`baseline.diff`, `baseline-diff-review.json`, `reviewed-metadata.json`, `existing-flags.json`). All five previously reviewed readiness blocks—`IncomingPdfHost`, `KnitToolsNavHost`, `WebPatternEditorScreen`, `ProUpgradeScreen`, `ProjectListScreen`—are byte-identical to the starting blocks, including `onStartupReady` and `startupGatesReady`. The earlier `BadgePill` baseline update and all other existing work are preserved. Production code, annotations, analyzer rules/thresholds/exclusions, `stability_config.conf`, dependencies and release/benchmark baselines were not changed.

Validation used ordinary **debug**, without isolated-test or optimized-smoke properties: Gradle **9.8.1**, actual daemon **Eclipse Temurin 17.0.20+8**, AGP **9.4.1**, Kotlin Compose plugin **2.4.21**, Compose Stability Analyzer **0.12.0**. Commands/results:

```powershell
./gradlew.bat --version
./gradlew.bat --no-daemon --max-workers=1 help --task :app:debugStabilityDump
./gradlew.bat --no-daemon --max-workers=1 :app:debugStabilityCheck
./gradlew.bat --no-daemon --max-workers=1 :app:debugStabilityDump
./gradlew.bat --no-daemon --max-workers=1 :app:debugStabilityCheck
git diff --check
```

Task discovery passed. The first check failed as expected (**2m 54s**, 20 differences); dump passed (**44s**, `dump.log`, exit 0); the final owning check **passed in 26s**, exit 0 (`check-after.log`). The final check reused the unchanged compiled debug metadata; its check task executed. `git diff --check` passed. Starting-file hashes and final status/diff inspection confirm only the debug baseline and this appended plan record changed; other variant dumps and original startup reports are unchanged (`preservation-final.json`). The feature branch `codex/audit-korjaukset-20261018`, HEAD and empty index remain unchanged. Task-owned Gradle processes exited; no temporary process remains.

Remaining metadata limits: the full debug inventory is **512 composables**, **1,568 STABLE / 116 RUNTIME / 2 UNKNOWN / 0 UNSTABLE parameter entries**. The 116 RUNTIME entries are unchanged. Both UNKNOWN entries retain the analyzer's reason, `interface or non-final class; concrete implementation unknown`; this does not establish either stability or a measured runtime slowdown. A passing check proves that the reviewed baseline matches analyzer output, **not that every parameter is stable or that runtime performance improved**. The existing Gradle-10 deprecation advisory is unchanged.

This baseline-only follow-up did not repeat JVM/device/lint/benchmark/adaptive campaigns, 320 dp, 200% font, their combination or TalkBack. **At this follow-up's completion, physical timing and the section-12 comparison were pending; profile generation had not started. Section 12 below subsequently completes generation and the emulator comparison. Physical timing remains pending. The production signing/package blocker, T08, live W11/R10, remaining R11 checks, the separate INC-SIGN-01 owner action (4.1) and unrelated WebView/security findings remain explicitly open.** No commit, push, PR, publication, deployment or other work package was performed.

## 12. Work package 5B: baseline and startup profiles

### Generator journeys

Extend the current startup-only generator with a small set of important, deterministic paths:

| Journey | Profile intent |
| --- | --- |
| Cold launcher start through real Projects readiness | Startup profile plus baseline startup path |
| Open a synthetic project, enter Counter, increment/decrement | Frequent core workflow |
| Move through Library and Tools and open a representative calculator | Common Compose/navigation and tool path |
| Open Insights with a small synthetic local history | Local analytics rendering |
| Open a small local PDF and perform a short reading interaction | Reader initialization and rendering path |

Do not put live Ravelry login, billing purchase, external-camera UI, or unpredictable network downloads into profile generation.

### Implementation and packaging

1. Keep `BaselineProfileRule.collect` for profile generation.
2. Use the appropriate startup-profile inclusion option only for actual startup paths; do not mark the entire app tour as startup.
3. Use synthetic data outside the collected interaction where supported, and keep any test-only setup out of production source sets.
4. Prefer driving existing UI for simple setup. Reuse established test-only facilities if UI setup would be unreliable.
5. Verify generated variants and task names using Gradle task discovery.
6. Generate on a supported device/emulator according to the current Benchmark plugin requirements.
7. Persist the generated app profile source in the plugin's intended source location, using `saveInSrc` where appropriate.
8. Keep profile generation explicit; do not make every developer build require a connected device through automatic generation.
9. Record the source/input revision used to generate the profile.
10. Build the final optimized artifact from those profile inputs.
11. Inspect the APK/AAB for the expected compiled baseline profile assets, such as `baseline.prof` and applicable metadata, in the correct artifact location.
12. Verify the generated profile contains app-owned startup/core workflow entries, not only dependency-provided entries.
13. Verify the startup-profile input is consumed by the build's DEX layout process. Do not expect a standalone `startup.prof` runtime asset as the proof.
14. Exercise the required-profile benchmark mode so a missing installation/profile is a visible failure.
15. If adaptive/startup code changes after generation, regenerate the affected profile before final acceptance.

Generated profile source is an intended versioned build input; generated benchmark reports and traces are not. Do not manually invent profile entries. [Configure Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles)

### Acceptance

- [x] Generator covers real core workflows with deterministic local inputs.
- [x] Startup-only and broader baseline coverage are intentionally distinguished.
- [x] App profile source exists in the intended source location.
- [x] Final signed production APK and AAB include the compiled profile. **Local candidate packaging verified in 12.6; candidate runtime and Play distribution remain separate.**
- [x] A required-profile measurement succeeds.
- [x] Recorded results show the measured effect without unsupported promises.

### 12.1 Implemented collection and isolation (2026-10-22)

This work implements section 12 and the comparison deferred from section 11. Evidence is in `reports/profiles-20261022/`, on the existing dirty `codex/audit-korjaukset-20261018` branch at `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. `source-at-final-generation.json` identifies the recorded input inventory (SHA-256 `48A116C2B3930F710D02497407DE21D3801661BB5D9E768C7AC2FB5A27B995EB`). No production Kotlin, entitlement, startup readiness, dependency, CI selection or smoke-runner changes were made.

`BaselineProfileGenerator` retains `BaselineProfileRule.collect` on the plugin-generated `nonMinifiedRelease` variant. `SmokeDevice` moved byte-for-byte from `src/benchmarkRelease` to `src/main`; `StartupFixtures.kt` shares the existing UI preparation and read-only session identity checks with `StartupTimingBenchmark`. Preparation is before `collect`/`measureRepeated`; Benchmark 1.5.0 has no collection `setupBlock`.

| Collection | Fixture and assertions | Startup inclusion |
| --- | --- | --- |
| `emptyStartup` | Normal launcher, zero projects/sessions; waits for `startup_ready` and the actual empty Projects content; session identity unchanged. | `true` |
| `populatedStartup` | Three projects (counts 4/8/12), ordinary local trial and one existing active session; cold launcher waits for real Projects readiness and Startup cardigan; session token/project/start anchors and zero completed sessions preserved. | `true` |
| `coreWorkflows` | Profile scarf, three completed UI-created sessions with three rows each, count 9; project overview → Counter, 9→10→9; Library; Tools → Increase / Decrease with 40/8 and visible result; Insights with actual local history; imported two-page local PDF, rendered page 1→2→1; completed history and counter unchanged afterwards. | `false` |

All fixture writes use existing UI, including ordinary trial initiation and SAF PDF import. The core fixture has three short local sessions, not a multi-day history. PDF creation/import and source cleanup are outside collection. No live Ravelry, purchase, camera, network download or production fixture hook is involved. Explicit `optimizedSmokeAllowed=true` and `startupFixtureResetAllowed=true` are required; the existing reset checks enforce an emulator and the isolated, test-only, non-debuggable `com.finnvek.knittools.optimizedtest` package with INTERNET/BILLING/CAMERA absent. `ANDROID_SERIAL=emulator-5584` limits Gradle collection to that emulator. The connected personal phone was not used.

`-Pknittools.optimizedSmoke=true` now applies the existing isolated package/manifest/empty analytics token and placeholder Google Services wiring to both collection `nonMinifiedRelease` and optimized `benchmarkRelease`. Assertions retain non-debuggable/debug-signing requirements, and R8/resource shrinking for the optimized sibling. Ordinary release signing and service configuration gates remain intact. `saveInSrc=true` and `automaticGenerationDuringBuild=false` make generation explicit.

### 12.2 Generated source and reviewed coverage

Actual final generation passed **3/3 methods**, zero failures/errors/skips, in **995.320 s** of instrumentation / **19m 4s** Gradle time (`generate-3.log`, `generate-3-results.xml`, `generate-3-output/`). No profile entries were written manually. Inputs are intended repository files:

| Source under `app/src/release/generated/baselineProfiles/` | Total rules | App-owned rules | SHA-256 |
| --- | ---: | ---: | --- |
| `baseline-prof.txt` | 36,825 | 7,536 | `903AA40752F63999534DD0A09A8D0FE276D25B406671AB42FDE34C5EA5BB653F` |
| `startup-prof.txt` | 28,604 | 3,948 | `1A99F6E50F38344A56F29D1DD9A468DA795A73ABEC024E0360AF89F86932186B` |

Reviewed app-owned baseline matches include MainActivity **309**, ProjectList **603**, CounterScreen **403**, CounterRepository **185**, LibraryScreen **60**, HomeScreen **45**, IncreaseDecrease **97**, Insights **498**, PatternViewer **411**, PdfPageRenderer **15**. These are matching rule counts, not percentages or disjoint coverage categories. Startup sources are collected only by the two launch methods; route/type initialization can still reference other features, so zero references to every later screen are not asserted.

The library defaults remain `maxIterations=15`, `stableIterations=3`, `strictStability=false`. Empty startup completed at iteration **4** and populated startup at **6**. The core tour completed all interactions but reached **15** without full-set convergence (`generation-driver.log`); its output is usable collected coverage, not proof of a fully stable rule set. No setting was relaxed to obtain a pass.

Collection used Gradle **9.8.1**, actual daemon **Eclipse Temurin 17.0.20**, AGP **9.4.1**, Kotlin Compose **2.4.21**, Benchmark/Baseline Profile **1.5.0**. Device: disposable read-only `KnitToolsOptimized20261021`, `emulator-5584`, Android **16/API 36**, **x86_64**, fingerprint `google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`. Display was 720×1600 at density 320 (**360 dp**), font 1.0 and animations 1.0. Generation artifact hashes are retained in `generation-artifacts.json` (collection app `E7C9B9F7392B1FF74E3A004F98C835D9F6515B4F3BE3547DD03D04553F33817E`, driver `FAF5FF0783BC0938988E92845D80802B3E28819DB1CAEEB2A7D1C83168BA0181`).

### 12.3 Optimized consumption and packaging

`packaging-verification.json` verifies that **all 7,536 generated app baseline rules** reach the merged baseline (normalizing flags), and that generated and merged startup rule sets match. R8's executed `minifyBenchmarkReleaseWithR8` task had `artProfileRewriting=true`; its actual `inputProfileForDexStartupOptimization` was the merged startup profile, byte-identical to the saved source SHA-256 above (`r8-startup-consumption.json`, `profile-evidence.gradle`). The resolved AGP source passes this property to `runR8`. This verifies DEX-layout input consumption without expecting a runtime `startup.prof` asset.

The R8-rewritten profile contains **2,903 app residual rules**, matched through this build's mapping. The APK includes `assets/dexopt/baseline.prof` (**17,050 bytes**, SHA-256 `2CB81087355F99D021DE85A925B502B3C71CDDAA7EE1DAD2F5D6BDD945C41DB9`) and `baseline.profm` (**1,185 bytes**, `FC63659F02C7C8602FD963837236313A8F77C63AA1DDB4CCC76DF07173C5C1D6`), byte-identical to the build's compiled profile/metadata outputs. Effective `benchmarkRelease` has minification and resource shrinking enabled, `debuggable=false`; the packaged manifest has `testOnly=true`, profileable enabled/shell true and the isolated application ID. It has no INTERNET/BILLING/CAMERA permission. The verified signing certificate is Android Debug, SHA-256 `f6ed10fcf19c9aa5f6a31a53358cc245f487fe5b1765e7028080e020aebb7d05`.

This is optimized test-artifact evidence. All four required production signing variables are absent (`prerequisites.json`, presence booleans only), and normal release has no signing configuration. **At this 2026-10-22 run, final signed production APK/AAB profile packaging remained blocked. The new 2026-10-23 result in 12.6 supersedes that packaging blocker.** No signing or service gate was bypassed; local secret files and environment values were not printed.

### 12.4 Deferred startup comparison — executed

The unchanged measured block waits for `startup_ready` plus the expected Projects content. The final run passed **4/4 methods**, **three cold launches per method / twelve samples**, **188.043 s** (`benchmark-final.txt`). Required modes remain `CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require, warmupIterations = 0)`, so installation failure is fatal; no `UseIfAvailable` substitution was made. Both `CompilationMode.None()` cases use the **same APK containing the same assets**, with matched fixture preparation and emulator configuration.

| Fixture / compilation mode | TTID min / median / max (ms) | TTFD min / median / max (ms) |
| --- | --- | --- |
| Empty / None | 1,459.0 / 2,214.0 / 2,420.9 | 4,646.7 / 5,477.1 / 5,483.9 |
| Empty / Partial Require | 1,375.8 / 1,488.4 / 1,935.9 | 4,739.5 / 4,801.8 / 5,080.1 |
| Populated / None | 1,469.8 / 1,509.8 / 3,437.3 | 4,790.8 / 4,810.1 / 6,486.8 |
| Populated / Partial Require | 1,413.1 / 1,493.2 / 1,529.3 | 4,993.7 / 5,224.3 / 6,149.8 |

These are functional emulator measurements, not a physical-device speedup or production latency claim. Variance is substantial (populated/None TTID coefficient of variation **0.526**); the populated TTFD median is higher with Require. No percentage improvement or consistent performance benefit is inferred. The old section-11 APK results are not a comparison baseline.

The device remained the API-36 read-only AVD described above, at **360 dp**, font **1.0**, animation scales **1.0** (`benchmark-device-conditions.json`). Thermal status was **0** before/after, emulated skin **30.1 °C**, battery **30.2 °C**; all methods recorded zero thermal-throttle sleep. These are emulated readings, not physical thermal stability. Fixture construction, widget removal, and before/after SQL session checks were outside measurement. All session-preservation assertions passed; the populated fixture retained the same active token/project/start anchors and zero completed sessions (`startup-populated-session.txt`, `startup-empty-session.txt`). The core fixture separately passed SQLite integrity/foreign-key inspection with three completed sessions and the imported document (`core-fixture-valid.txt`).

Raw benchmark JSON and all **12 Perfetto traces** are preserved in `benchmark-output-final/`. Perfetto trace processor **v58.2**, using the existing section-11 SQL, verified **12/12 traces with exactly one platform `reportFullyDrawn` slice and a completed RenderThread frame after it**. Per-trace CSVs, `trace-validation.json` and `benchmark-summary.json` retain timings, variance and checks.

The optimized build, measured app, driver and smoke artifacts have the same SHA-256 identities (`packaging-verification.json`, `ci-smoke/identity.json`):

| Artifact | SHA-256 |
| --- | --- |
| `app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk` | `6C588C019C179DE997F079D8ED59D4B102412D705AD3C8A77BC4C4596B23CC02` |
| `baselineprofile/build/outputs/apk/benchmarkRelease/baselineprofile-benchmarkRelease.apk` | `7C178948B088AA9485A47ABBA5465F3FFE3C3E27F6CDCBC86840D4560DE29672` |
| `app/build/outputs/mapping/benchmarkRelease/mapping.txt` | `EA85DCF07F3F76BB785C0B89653A5DD28648DB48EEB2797C1639F806801EC2BA` |

The instrumented R8-input build and final passing build produced byte-identical APK, driver, mapping and packaged profile assets. No production input changed after generation; subsequent edits only corrected the source-test expectation and this report.

### 12.5 Commands, validation and remaining boundaries

Generation task discovery used `:app:tasks :baselineprofile:tasks --all` with the isolated-profile property. The successful commands were:

```powershell
$previousSerial = $env:ANDROID_SERIAL
$env:ANDROID_SERIAL = 'emulator-5584'
try {
    ./gradlew.bat --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' '-Pandroid.testInstrumentationRunnerArguments.class=com.finnvek.knittools.baselineprofile.BaselineProfileGenerator' '-Pandroid.testInstrumentationRunnerArguments.optimizedSmokeAllowed=true' '-Pandroid.testInstrumentationRunnerArguments.startupFixtureResetAllowed=true' :app:generateReleaseBaselineProfile
} finally { $env:ANDROID_SERIAL = $previousSerial }

./gradlew.bat --no-daemon --max-workers=1 '-Pknittools.optimizedSmoke=true' :app:assembleBenchmarkRelease :baselineprofile:assembleBenchmarkRelease :app:ktlintCheck :baselineprofile:ktlintCheck :app:detekt :app:testDebugUnitTest --tests '*FirebaseGradleIntegrationSourceTest' --tests '*ReleaseSigningSourceTest'
./gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug --dry-run

adb -s emulator-5584 install -r -t app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk
adb -s emulator-5584 install -r -t baselineprofile/build/outputs/apk/benchmarkRelease/baselineprofile-benchmarkRelease.apk
adb -s emulator-5584 shell am instrument -w -r -e class com.finnvek.knittools.baselineprofile.StartupTimingBenchmark -e optimizedSmokeAllowed true -e startupFixtureResetAllowed true -e startupIterations 3 -e androidx.benchmark.suppressErrors EMULATOR -e additionalTestOutputDir /sdcard/Android/data/com.finnvek.knittools.baselineprofile/files/startup-profiles-final com.finnvek.knittools.baselineprofile/androidx.test.runner.AndroidJUnitRunner
./tools/optimized-smoke.ps1 -Serial emulator-5584 -DisposableEmulator -OutputDirectory reports/profiles-20261022/ci-smoke -ExpectedTests 2 -Test 'com.finnvek.knittools.baselineprofile.OptimizedSmokeTest#projectCounterSessionAndNavigation,com.finnvek.knittools.baselineprofile.OptimizedSmokeTest#freeGateTrialAndAdaptiveTransitions'

python reports/profiles-20261022/verify-packaging.py
python reports/profiles-20261022/verify-benchmark.py
python reports/profiles-20261022/verify-preservation.py
git diff --check
```

Final optimized app/driver assembly, both ktlint checks, app Detekt and the **4/4 focused JVM tests passed** (`build-profiled-3.log`, **6m 16s**; Firebase wiring **3**, release signing **1**). The ordinary debug **task-graph dry run** has no generation or connected-device task; it is not an executed debug assembly (`ordinary-debug-task-graph.log`). The existing two-test CI smoke selection passed **2/2**, **59.874 s**, on the profiled optimized APK (`ci-smoke/`); the runner and CI workflow are unchanged. Its existing 360/1280/800 dp transitions are a narrow regression check, not a repeated adaptive campaign. Smoke ran after timing because its runner disables animations.

Earlier attempts remain separate evidence: unquoted PowerShell dotted-property discovery failed; generation attempts 1/2 exposed UI setup/transition races; the first core dry run exposed that `collect` kills the target before postconditions. Those driver defects were corrected, and core dry run 2 passed **1/1** before final full collection. Dry-run profile output was not used as the final source. `:baselineprofile:lint` is unavailable for this test module, so that attempted task selection failed and is not a lint pass. The first optimized validation run built the APK and passed static tasks but failed one literal source expectation after ktlint wrapped a Gradle expression; the expectation was corrected without weakening its wiring checks, then all four focused tests passed. Logs and failed reports are retained. App lint/full JVM/adaptive campaigns were not repeated for this build/test-only change; section-11 results retain their own scope. The Gradle-10 deprecation advisory remains.

**Remaining prerequisites:** authorized controlled physical measurements with isolated synthetic fixtures, starting at **ten iterations per scenario/mode**, thermal recording and variance review; final signed production APK/AAB profile packaging once legitimate signing/service prerequisites exist. The broader core profile's 15-iteration convergence limit remains recorded in 12.2. T08, live W11/R10, remaining R11 checks, the separate INC-SIGN-01 owner action (4.1) and unrelated WebView/security findings remain explicitly open. The resolved debug Compose Stability baseline was not reopened or modified. No 320 dp, 200% font, combined configuration or TalkBack check was rerun.

Final preservation verification passed (`preservation-final.json`): only the scoped build/test/profile files and this plan changed, the shared driver is byte-identical after its move, and unrelated existing work—including the resolved debug stability baseline—was preserved. Branch/HEAD and the initially empty index are unchanged. `git diff --check` passed; existing CRLF-conversion advisories are retained in its log. Task-owned emulation and tracked Gradle processes have exited (`emulator-cleanup-remaining.json` is empty, `tracked-processes-final.json`); no unrelated process or personal device was stopped. No final release-readiness closure, commit, push, PR, publication or deployment was performed.

### 12.6 Final profile refresh and local signed candidate — 2026-10-23

**COMPLETE for profile refresh and local signed packaging only.** Evidence and executable verification scripts are retained in ignored `reports/candidate-20261023/`; verified candidate copies are in its `artifacts/` directory. This supersedes earlier missing-signing and stale-profile packaging status in 6.8, 9.4, 10.3 and 12.3, while retaining their historical results. It does not close candidate runtime, R01–R13, live services, physical performance, CI or Play distribution. Subsequent local runtime verification is recorded in 10.7; 10.9 subsequently passes signed-candidate live R10. R11 and other external boundaries remain open.

#### Frozen inputs and preservation

- Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- `inputs.py` inventories tracked and nonignored untracked files, hashing raw bytes with SHA-256. The build identity hashes the UTF-8 JSON mapping of relative path to uppercase content hash, with sorted keys and compact separators. Its 1,014 Android build inputs cover app/profile source sets and schemas, Gradle configuration/version/verification files, ProGuard rules and stability configuration. HEAD alone is not the identity.
- Starting build identity: `74D6CBE52068CD39D83AB2FADAB21D727153CA20619D7AF693087FE30EE2305C`.
- Final build identity, after real generation and unchanged through both optimized and signed builds: `C9B22EA7A65988DAB7105D3D8E2F5808039DE51A0F88940CE37E250D8D0D608A`.
- Identity excluding generated profile sources: `B2B93E90E6F629396F2B7DE501A5EEF6C0531FA1613712C24D6FB80D5530E2D8`, unchanged. Only the two profile source files and this plan changed among the 1,299 starting files. HEAD, branch and index are unchanged. `before.json`, `after-generation.json`, `after-build.json`, `after-documentation.json`, `preservation-final.json`, the original dirty diff/status and `build-sources.zip` preserve the evidence.
- Project-local Firebase/PostHog files remained byte-identical. Their hashes are retained locally, not their values. Neither corresponding environment override was present. The existing dedicated key was reused through `.local/signing/Invoke-WithSigning.ps1`; no replacement, rotation, backup/recovery operation, shared credential-store inspection or global Gradle-properties read/edit was performed.

#### Actual collection and required-profile check

The unchanged generator ran on a task-owned read-only instance of `KnitToolsOptimized20261021`, `emulator-5584`, Android 16 / API 36, x86_64, fingerprint `google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`. Conditions: 720×1600 at 320 dpi (**360 dp** width), font 1.0, all animation scales 1.0. No personal phone was connected or targeted.

All **3/3** methods passed, zero failures/errors/skips: `emptyStartup`, `populatedStartup` and `coreWorkflows`. Instrumentation took **857.640 s**; generation including the fresh collection build took **27m 50s**. Existing synthetic journeys, session-preservation assertions, ordinary trial setup and local two-page PDF import were retained. Only the two startup methods use `includeInStartupProfile=true`; core collection remains false. ART state was reset and ProfileInstaller's skip marker set before collection. No entries were invented or copied from historical results.

The unchanged defaults are `maxIterations=15`, `stableIterations=3`, `strictStability=false`. Empty startup converged at **5**, populated startup at **9**, and core workflows at **11**. This run did **not** reach the convergence limit; the earlier 15-iteration limitation in 12.2 remains historical. Raw XML, collection output and `generation-driver.log` are retained.

| Source in `app/src/release/generated/baselineProfiles/` | Rules | App-owned rules | SHA-256 |
| --- | ---: | ---: | --- |
| `baseline-prof.txt` | 37,427 | 7,539 | `0326F83807EE8443F3F111AA095AA6B6A6DFA1B86BB84ABAC1ADB3990AF3C407` |
| `startup-prof.txt` | 29,326 | 3,954 | `A8D2D40C506327D01B9815CD5094FF94D14FA9E991A7D25EAAA4628237774D1A` |

The rebuilt `benchmarkRelease` test APK is minified, resource-shrunk, non-debuggable, test-only and shell-profileable, with application ID `com.finnvek.knittools.optimizedtest`, no INTERNET/BILLING/CAMERA permission, disabled analytics and debug certificate `F6ED10FCF19C9AA5F6A31A53358CC245F487FE5B1765E7028080E020AEBB7D05`. Its SHA-256 is `13F5CE8B9B8397D0EF1E30CCD2EDA03007859A2AEAD4216496BA9877A80D105D`; driver SHA-256 `42F5125423A16708F13DE2E7F47A443BB6794977633CA7238B4C8FEE7EAB2D0E`. Build passed in **9m 28s**. Paths, mapping and hashes are in `required-profile-artifacts.json`.

The smallest selected existing check was `StartupTimingBenchmark#populatedRequiredProfile`: **1/1 passed**, **three cold starts**, **69.602 s**, retaining `BaselineProfileMode.Require` with zero warmup iterations and the existing session/readiness assertions. Raw JSON and three traces are retained in `required-profile-output/`; no timing comparison or physical-device improvement is claimed. This is a separate isolated test artifact, not the production candidate. Its strict decoded profile contains all **2,825** R8 residual app rules.

#### Genuine signed release and local-only controls

Actual `:app:assembleRelease` and `:app:bundleRelease` passed in **12m 30s**: 70 actionable tasks, 67 executed and 3 up-to-date. Real Firebase/PostHog validators executed; the unchanged signing validator was up-to-date from the same local setup, followed by fresh cryptographic verification of both completed artifacts. `lintVitalAnalyzeRelease`, `lintVitalReportRelease` and `lintVitalRelease` passed. The source release-surface check passed **13/13**; the resolved release inventory contains **198** artifacts.

The initial dry-run graph identified `:app:uploadCrashlyticsMappingFileRelease`. The local build excludes exactly that task using Gradle's supported [`-x` control](https://docs.gradle.org/current/userguide/command_line_interface.html). `evidence.gradle` records each graph and rejects upload/publication/deployment tasks in executed graphs. The local graph has 78 nodes and retains signing/service gates, R8, resource shrinking, profile compilation, mapping-ID injection and APK/AAB signing. No production configuration, entitlement code, R8 rule or validator was weakened. The signing-preparation-only guard was not used. No app launch, analytics event, deployment, mapping/symbol upload, Play action or publication occurred in the production build.

The runner reuses the documented project-local Gradle home and project cache under `.gradle/signing-prereq-20261023/`, the installed Gradle binary and Temurin runtime. The read-only dependency-cache setting is correctly rooted at `C:/Users/EmmaH/.gradle/caches` (Gradle appends `modules-2`). The first discovery dry run used the README's too-deep cache path and logged that the shared cache was disabled; it still completed. This was corrected only in the ignored task runner before generation, with no global-property access or repository build change.

Toolchain: Gradle **9.8.1**, Eclipse Temurin **17.0.20**, AGP **9.4.1**, R8 **9.4.24**, Kotlin Compose **2.4.21**, Benchmark/Baseline Profile **1.5.0**, WebKit **1.18.0**, SDK build tools **37.0.0**, SDK command-line tools **23.0**. Product identity: **com.finnvek.knittools**, **1.0.0 / code 1**, minSdk **29**, compile/targetSdk **37**.

| Retained file under `reports/candidate-20261023/artifacts/` | Bytes | SHA-256 |
| --- | ---: | --- |
| `app-release.apk` | 11,614,422 | `91E312882F7DACDEAA63B8A994986EEA988AEC696D5F68D519C70DD360C33803` |
| `app-release.aab` | 13,370,583 | `88B866C00E9B518EDB0150DBE4692C2886A4F8AF0619685AA2758A6F8B7D5460` |
| `mapping.txt` | 95,182,429 | `50A917EDF83A27652127D8573F2E9009335A0E131D2CFF553DEEAC7173A039FE` |

Original outputs remain at `app/build/outputs/apk/release/app-release.apk`, `app/build/outputs/bundle/release/app-release.aab`, and `app/build/outputs/mapping/release/mapping.txt`. Retained copies were checked byte-for-byte by hash. Configuration, resource-shrinking, seed/usage output and profile sources accompany the mapping. APK signature scheme v2 verified; the AAB verifier read and cryptographically verified all **1,425** non-META-INF payload entries. Both match existing certificate SHA-256 **C31F2806EFA207CE5669647EC6902A1395788B5C0143AE3527983951F1A37D97**. This remains a local release/upload certificate, not a verified Google Play app-signing identity.

#### Packaged profile and release contracts

- All **7,539** generated app baseline rules reach the merged baseline. Generated and merged startup rules match. The executed `minifyReleaseWithR8` records `artProfileRewriting=true`, resource shrinking enabled and startup input hash `A8D2D40C506327D01B9815CD5094FF94D14FA9E991A7D25EAAA4628237774D1A`. Resource conversion/optimization tasks executed and `resources.txt` is retained. This proves actual optimization and DEX-layout input consumption, without expecting a standalone runtime `startup.prof`.
- SDK `profgen dumpProfile`, with default **strict=true**, verifies profile/DEX compatibility and decodes **all 2,829** R8 residual app rules from the production binary profile. The compiled outputs match APK `assets/dexopt/baseline.prof` (**17,087 bytes**, `3CCBF58E2E9C1D14DD2FC3D0963776359497988926F444853C7B6A7EB2E74884`) and `baseline.profm` (**1,177 bytes**, `C9F5858F4267C5CD4AA0F53D50DEFB76045597F3B0645F27345260AC5D6E0074`). AAB `BUNDLE-METADATA/com.android.tools.build.profiles/` contains byte-identical profiles, and both `classes.dex` / `classes2.dex` match the APK. See [Android's binary-profile inspection guidance](https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure).
- Final APK and AAB manifests and resource-configuration sets match exactly. Both are production-ID, non-debuggable, non-test-only packages without pseudolocales. The AAB base was converted to `bundle-base-inspection.apk` only to inspect protobuf manifest/resources; that unsigned inspection file is not a candidate.
- Actual packaged Firebase resource values match the real local configuration, and the packaged PostHog token matches the local project token, without logging values. Crashlytics collection and release PostHog remain enabled as configured; the existing opt-in analytics behavior was not changed or runtime-tested.
- Generated `BuildConfig.DEBUG=false`; test-control/debug diagnostics fields are absent. Mapping plus DEX inspection confirms `ProState` residual `gv4.a` delegates to `isPro`, whose enum branches are `PRO_PURCHASED` and `TRIAL_ACTIVE`; R8 removed the debug-bypass boolean parameter. Synthetic journey names, debug seeding/test classes, Sentry and prohibited SDK implementations are absent. This is static packaged-code proof, not billing/entitlement runtime acceptance.
- A first report-local namespace assertion misclassified Crashlytics's single `AnalyticsConnector` interface. Its owning `firebase-measurement-connector:20.0.1` AAR and direct dependency in the resolved Crashlytics **20.1.1** POM were verified. The checker now allows only that exact interface and still rejects other Analytics classes, Firebase Analytics/Play measurement implementations, Auth/Functions/AI, ML Kit, Sentry and test dependencies. No product/dependency change was needed; `connector-verification.json` records the correction.
- R8 mapping ID is `25717e3492e8c107b2b2df0fa21f5fbbace5013caedad8284a001104588ddc09`. Packaged Crashlytics mapping-file ID is `975be336703340d298ef47d63f8a067c`, associated with the exact retained mapping and artifacts in `mapping-association.json` / `artifacts.json`. Mapping upload and crash-delivery verification remain separate.

#### Reproducible commands and remaining boundary

Every Gradle invocation is recorded in its `*-command.json`, graph, JVM record, result and log. `gradle.ps1` sets the project-local Gradle environment, uses `--no-daemon --max-workers=1 --no-configuration-cache --no-build-cache --console=plain`, and applies only the ignored evidence/graph script. `-Signed` wraps the invocation with the existing signing helper. Commands actually run:

```powershell
python reports/candidate-20261023/inputs.py before
./tools/release-surface.ps1
./reports/candidate-20261023/gradle.ps1 -Log release-discovery -Signed :app:assembleRelease :app:bundleRelease --dry-run
./reports/candidate-20261023/gradle.ps1 -Log generate '-Pknittools.optimizedSmoke=true' '-Pandroid.testInstrumentationRunnerArguments.class=com.finnvek.knittools.baselineprofile.BaselineProfileGenerator' '-Pandroid.testInstrumentationRunnerArguments.optimizedSmokeAllowed=true' '-Pandroid.testInstrumentationRunnerArguments.startupFixtureResetAllowed=true' :app:generateReleaseBaselineProfile
python reports/candidate-20261023/inputs.py after-generation
./reports/candidate-20261023/gradle.ps1 -Log optimized '-Pknittools.optimizedSmoke=true' :app:assembleBenchmarkRelease :baselineprofile:assembleBenchmarkRelease
python reports/candidate-20261023/verify-profiles.py benchmarkRelease
# SDK profgen dumpProfile with --profile, --apk and --output; strict defaults to true.
./reports/candidate-20261023/required-profile.ps1
./reports/candidate-20261023/gradle.ps1 -Log release-local-graph -Signed -x :app:uploadCrashlyticsMappingFileRelease :app:assembleRelease :app:bundleRelease :app:candidateReleaseDependencies --dry-run
./reports/candidate-20261023/gradle.ps1 -Log release -Signed -x :app:uploadCrashlyticsMappingFileRelease :app:assembleRelease :app:bundleRelease :app:candidateReleaseDependencies
./reports/candidate-20261023/verify-artifacts.ps1
python reports/candidate-20261023/inputs.py after-build
python reports/candidate-20261023/retain.py
python reports/candidate-20261023/inputs.py after-documentation
git diff --check
```

The verification script retains the exact `apksigner verify --verbose --print-certs`, Java all-payload AAB verification, `apkanalyzer manifest print`, `aapt2` manifest/resource conversion and strict `profgen dumpProfile` commands. Required-profile instrumentation arguments and artifact hashes are recorded separately. Existing generator/benchmark assertions and collection stability settings were not changed.

**No packaging blocker remains.** The pre-existing Compose mapping warning for `CounterWidget.provideGlance` recurs (also present in the previous 2026-10-22 optimized log); enhanced Compose stack mapping for that function is incomplete. Standard R8 mapping is retained. Prebuilt native-library stripping warnings and the Gradle-10 deprecation advisory also remain. No dependency upgrade or unrelated fix was attempted.

**Separate candidate-runtime task remains required:** install only the retained genuine APK on an explicitly disposable target; record candidate-specific R01–R13 coverage and distinguish camera handlers, live Ravelry, billing/entitlements, widgets/external launches and service delivery. This task never launched the production candidate. Physical timing, live service ownership/delivery, Crashlytics upload/reporting, GitHub CI and Play distribution remain unverified here. No full regression campaign, 320 dp, 200% font, combined case or TalkBack check was run. Task-owned emulator/logcat and single-use Gradle processes exited; original user work and final artifacts remain. No commit, push, PR or upload occurred.

## 13. Validation commands and test ownership

These are implementation-time commands. They were not executed while writing this plan. Use the Windows wrapper and JDK 17.

### 13.1 Existing direct checks

```powershell
.\gradlew.bat :app:ktlintCheck :app:detekt :app:lintDebug :app:lintRelease
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
git diff --check
```

During individual fixes, select the owning tests first. Run the complete debug unit suite once after integration rather than after every small edit.

Existing relevant tests include:

- `com.finnvek.knittools.pro.TrialManagerTest`
- `com.finnvek.knittools.pro.TrialStartSourceTest`
- `com.finnvek.knittools.ui.screens.ravelry.RavelryLinksTest`
- `com.finnvek.knittools.RavelryBrowserSourceTest`
- `com.finnvek.knittools.RavelrySecuritySourceTest`
- `com.finnvek.knittools.ui.screens.pattern.PatternPickerSourceTest`
- `com.finnvek.knittools.ui.screens.counter.ProgressPhotoGallerySourceTest`
- `com.finnvek.knittools.ReleaseSigningSourceTest`
- `com.finnvek.knittools.AppStartupSourceTest`
- relevant existing localization, PDF-coordinate, navigation, and UI-consistency tests.

Confirm class names and behavior before invoking them. Update stale source-contract assertions only when the intended behavior changes; do not weaken them just to make a refactor pass.

### 13.2 Isolated runtime example

The existing Ravelry navigation test explicitly requires the isolated active-policy package:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
  '-Pknittools.isolatedTest=activepolicy' `
  '-Pandroid.testInstrumentationRunnerArguments.class=com.finnvek.knittools.ui.screens.ravelry.RavelryBrowserNavigationTest'
```

Select the intended test device through the supported runner/device setup and use `adb -s <serial>` for direct device operations. Do not allow an incidental connected personal phone to receive a destructive test.

Do not use this network-disabled profile as evidence for live Ravelry or Play Billing.

### 13.3 Variant/profile discovery and real candidate

```powershell
.\gradlew.bat :app:tasks --all
.\gradlew.bat :baselineprofile:tasks --all
.\gradlew.bat :app:generateBaselineProfile
```

The generator task is documented in the repository. Confirm its resolved task graph and device requirements before executing it.

Only after real signing/Firebase prerequisites are available:

```powershell
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

Do not substitute `nonMinifiedRelease` or a placeholder-config variant and report this command as verified.

The local-only execution in **12.6** uses the existing signing wrapper, project-local Gradle configuration and `-x :app:uploadCrashlyticsMappingFileRelease`, after task-graph inspection. Use those recorded commands for this retained candidate; the generic command above does not itself prevent uploads.

### 13.4 Focused new test coverage

Add only tests that prove behavior not already covered:

- trial pending-visibility recreation;
- bounded browser state, fallback, request identity, and Back policy;
- camera launch/result handling after permission removal;
- width-category/navigation transitions and gallery adaptation;
- PDF viewport/annotation alignment through resizing;
- startup loading-versus-empty readiness;
- optimized runtime smoke journeys;
- benchmark and profile-generation journeys.

Prefer behavior assertions over tests that mirror implementation constants or merely search for new method names.

## 14. Final release-readiness closure

### 14.1 Candidate-specific checks

**Current reconciliation — 2026-10-28:** RW-11D verifies its frozen dirty build inputs, genuine signatures, packaged contracts, exact mappings and reused compatible profiles. RW-11E validates only its exact APK's two local SAF imports, document-list removal/promotion, persisted-state reload and annotated export/reopening. This does not validate the distinct detach/failure paths or every R01–R13 journey on that artifact. The historical candidate status below and its runtime checks remain unchanged; report L1 retains final candidate applicability/accounting and U/E rows retain acceptance limits. No blanket rerun or profile regeneration is selected.

1. Freeze the candidate inputs after the functional, adaptive, resource, and startup work is complete.
2. Record dirty-input identity as well as HEAD if the candidate is built from uncommitted work.
3. Generate the final profile from those inputs.
4. Build the candidate and record hashes.
5. Run the final optimized smoke journeys against the identified candidate.
6. Confirm the merged manifest and packaged profile/resource outputs.
7. Confirm debug-only diagnostics, Pro controls, synthetic data, and pseudolocales are absent from the distributable artifact.
8. Verify the scoped adaptive matrix, including stateful transitions, is complete.
9. Check the minimum-supported and current-target startup/navigation coverage.
10. Review relevant failures from local tests and CI; do not erase or reclassify unrelated failures as passes.
11. Update only the source-authoritative documentation affected by the actual implementation. Keep `AGENTS.md` and `CODEX.md` aligned if either changes.
12. Stop temporary emulators, test servers, helpers, or other processes started specifically for this effort when they are no longer needed. Never terminate unrelated tools by process name.

**2026-10-23 candidate status:** 12.6 verifies frozen dirty inputs, refreshed profiles, genuine signed artifacts, manifest/resources and static release packaging. Section 10.7 completes the retained candidate's applicable local R01–R13 assertions on API 37 and minimum-supported API 29 startup/navigation. Section 10.9 subsequently passes the required signed-candidate live R10 assertions with the explicit private-file evidence limit; incidental-capture cleanup is complete. R11 remains PARTIAL for entitlement requirements; physical/service/distribution acceptance remains open. No whole-release closure is inferred.

### 14.2 Existing external prerequisites to reconcile

These are existing release concerns surfaced by repository documentation, not new speculative requirements:

| Item | Current evidence boundary | Required closure |
| --- | --- | --- |
| Ravelry written permission | `config/security-decisions.md` says permission had not yet been obtained and should be requested before Google Play release | Confirm whether the owner already obtained it; record the actual outcome or retain the repository prerequisite as open. Do not infer current legal/Play compliance from code or send a request without authorization. |
| Release signing/Firebase configuration | Dedicated key/configuration and historical 12.6 packaging preserved; RW-11D verifies current signed APK/AAB and public-client configuration parity | Local packaging complete; earlier missing-signing blockers are historical. Classified public findings retain exit 51; cloud restrictions and runtime/distribution behavior are not verified by that classification. |
| Other-project incident (historical 4.1) | Excluded from the current KnitTools queue by the user's project boundary; no current incident-state assertion is made here | No KnitTools prerequisite or action. Preserve historical records; do not access another project's credentials, recovery material or shared properties. |
| Crashlytics mapping/reporting | RW-11D verifies exact current APK/AAB mapping identity and embedded AAB mapping; mapping upload deliberately excluded | Live upload/report delivery/deobfuscation remains separately authorized verification (report E1); do not ship a crash trigger. |
| PostHog consent | Local contract reconciliation complete in handoff §8.10; RW-11D verifies packaged configuration | CD1/CD2 composed-fault/SDK queue limits remain report L3; live destination/consent delivery remains E1. Do not repeat the whole local review or infer queue purging from capture admission; no event/read authorized here. |
| Play Billing | Debug feature access differs from real release entitlement | Use an authorized test environment for release billing checks; record unverified states rather than using debug as proof. |
| Store declarations/privacy | The app already includes approved release services | Reconcile current declarations with the final artifact through the existing release process; do not certify them from this Android code review alone. |

Do not automatically publish, contact Ravelry, send messages, upload an AAB, or make purchases to close these rows.

### 14.3 Completion checklist

Current as of RW-11F, 2026-10-28. Checked rows mean the explicitly bounded result or recording task is complete, not unrestricted release acceptance. Unchecked acceptance is retained, not waived. Historical sections remain authoritative for their original inputs.

- [x] T01–T08: trial restoration checks completed within section 5's saved-state/synthetic-entitlement emulator guarantee; T08 includes two actual background process replacements on 2026-10-22. Real release billing and arbitrary unsaved termination remain outside this evidence.
- [x] W01–W12: browser restoration/history/fallback/policy checks completed within the recorded local-regression and ordinary-debug boundaries. Section 6.10 adds live W11 on the authorized Pixel 9, including Activity/saved-task restoration, short-history Back, authenticated PDF download, Library save/render/reopen and cancellation cleanup. The five-entry debug URL fallback remains explicit; 10.9 separately passes the retained signed-candidate live R10 assertions and records its own URL-only fallback/file-evidence limits.
- [x] C01–C09: both camera paths and merged release manifest verified within section 7.1's automated/emulator boundaries.
- [x] A01–A12: adaptive implementation and required matrix completed within section 8.14's isolated-app/emulator boundaries, including API 29 startup/navigation.
- [x] Changed resources, pseudolocales, RTL behavior, and manifest/navigation contracts verified within 9.4's selected JVM/navigation and en-XA/ar-XB journeys; final release packaging verified in 12.6. Historical execution, not an exhaustive linguistic or UI certification (RW-01 reconciliation, 2026-10-23).
- [ ] Actual hosted optimized CI smoke for the intended revision remains unverified. Workflow/correction review and local selections are complete within their scopes; report E3 records publication and run prerequisites.
- [x] Current genuine signed APK/AAB packaged and statically verified in RW-11D; exact signed APK passes RW-11E's bounded physical PDF journey. Earlier 12.6/10.7/10.9 and RW-11B/C runtime results retain their older artifacts; neither the current whole smoke matrix nor release readiness is claimed.
- [x] Historical R01–R13 smoke coverage recorded with external checks distinguished (10.7). R10 required live assertions passed on that older candidate (10.9); R11 remains partial. Current-candidate applicability and global case/branch accounting remain report L1, not an automatic rerun.
- [x] S01–S08 startup readiness cases verified within 11.5's owner, reporter and isolated runtime cases (RW-01 reconciliation, 2026-10-23); physical performance remains separate.
- [ ] Physical timing comparison is measured but incomplete: RW-06A (handoff §8.93) passed four methods/40 cold iterations with 39 TTID/TTFD pairs; empty Require iteration 006 is missing. Retain the isolated optimized artifact and variance/thermal limits; no signed-production performance claim or imputation (report U3).
- [x] Generated app profiles from 12.6 are reused in RW-11D with strict compatibility verification of 2,794 residual app rules and APK/AAB DEX/profile parity. No regeneration or comprehensive runtime/performance acceptance is inferred.
- [x] Existing external prerequisites are explicitly classified in §14.2 and report E1–E4; listing is complete, prerequisite satisfaction is not.
- [ ] Overall required release acceptance: genuine signed expiry, selected physical behavior, remaining local evidence work and external/deferred prerequisites are still open. See the single RW-11F backlog; no confirmed application defect is inferred from those gaps.
- [x] RW-01/RW-11F engineering reconciliation and usable finite backlog complete. Three owner decisions remain pending, not accepted limitations.
- [x] RW-11F preserved pre-existing user work; documentation/status/metadata verification only, not a new whole-tree byte audit.
- [x] No excluded UI campaign was rerun by RW-11F; no application check was executed.
- [x] RW-11F started no background server/build/watcher; its temporary inspection REPL is reset after document verification. Earlier task cleanup retains its own receipts.
- [x] No commit, push, pull request, publication or deployment occurred in RW-11F.

## 15. Explicit dispositions for non-required changes

| Suggestion | Decision |
| --- | --- |
| Remove `android.enableR8.fullMode=true` | Optional one-line cleanup while already editing build configuration; not a functional fix or a release-readiness gate. |
| Migrate the R8 DSL solely for newer syntax | Do not do it in this scope. Existing supported configuration must be verified, not rewritten for appearance. |
| Add broad keep rules | Do not do it without a reproduced optimized-runtime failure. |
| Add ProfilingManager now | Do not add production collection in this scope. Local Macrobenchmark/Perfetto evidence answers the present need. The API does not inherently require a backend. |
| Add trigger-based production profiling | No current demonstrated need; would require its own API-support, collection, storage, retention, and privacy decisions. |
| Install another R8 skill | Not necessary for this plan; the previous review found a local `r8-analyzer` installation. Reuse it if a later concrete R8 investigation needs it. |
| Rewrite Hilt or repository architecture | No verified need. |
| Replace the navigation library | No verified need; adapt presentation around the current graph. |
| Add CameraX/Camera2 | No need for the existing external-camera product flow. |
| Reintroduce Ravelry API/OAuth/backend usage | Explicitly out of scope and contrary to the current product decision. |
| Apply 600 dp to every screen | Rejected. Use the content-specific policy in work package 2. |
| Defer tablet/foldable/landscape work until later | Rejected by the current user request. Implement and verify it in this effort. |
| General two-pane product redesign | Not required to complete the scoped adaptive result. Keep single-content routes unless a concrete usability failure needs a local layout change. |
| Retest 320 dp / 200% fonts / TalkBack | Excluded by the explicit repository instruction. |

## 16. Coverage of every originally supplied link

The manifest introduction was supplied twice. It is covered once below. GitHub's Android organization is a repository directory, not a requirement to adopt every project it contains.

| Source | Application to this plan |
| --- | --- |
| [Adaptive apps](https://developer.android.com/develop/adaptive-apps) | Work package 2: current-window sizing, suitable navigation, content-aware layouts, immediate verification. |
| [Trifolds and landscape foldables](https://developer.android.com/develop/adaptive-apps/guides/foldables/trifolds-and-landscape-foldables) | Work package 2: landscape-natural devices, density changes, fold/resize continuity, hinge handling. Camera-preview/sensor work is inapplicable to the current flow. |
| [Android GitHub organization](https://github.com/android) | Use relevant official examples only when implementing a concrete requirement; no repository/tool installation task follows from the directory itself. |
| [Enable app optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization) | Work package 4: retain and prove R8/resource shrinking; avoid unnecessary DSL churn. |
| [Add keep rules](https://developer.android.com/topic/performance/app-optimization/add-keep-rules) | Work package 4: evidence-based, narrowly targeted rules only. |
| [Troubleshoot optimization](https://developer.android.com/topic/performance/app-optimization/troubleshoot-the-optimization) | Work package 4: optimized artifact reproduction, retracing, targeted correction. |
| [ProfilingManager](https://developer.android.com/reference/android/os/ProfilingManager) | Explicitly no production integration now; use local measurements for this effort. |
| [Trigger-based capture](https://developer.android.com/topic/performance/tracing/profiling-manager/trigger-based-capture) | No speculative production capture; revisit only for a demonstrated field-diagnostics need. |
| [Provide resources](https://developer.android.com/guide/topics/resources/providing-resources) | Work package 3: complete defaults, qualified resources, packaged-release checks. |
| [Runtime configuration changes](https://developer.android.com/guide/topics/resources/runtime-changes) | Work packages 1–2: restoration and live resizing; no blanket recreation suppression. |
| [Localization](https://developer.android.com/guide/topics/resources/localization) | Work package 3: changed resources, supported locale consistency, default fallback. |
| [Pseudolocales](https://developer.android.com/guide/topics/resources/pseudolocales) | Work package 3: debug-only expansion and RTL validation. |
| [Internationalization](https://developer.android.com/guide/topics/resources/internationalization) | Work package 3: locale-sensitive formatting, plurals, bidi, and canonical-value preservation. |
| [Manifest overview](https://developer.android.com/guide/topics/manifest/manifest-intro) | Work packages 1C, 2, and 3: permissions, orientation, merged manifest, intentional exported surfaces. |
| [Navigation principles](https://developer.android.com/guide/navigation/principles) | Work package 2 and regression checks: stable top-level destinations, Back/Up, single graph, retained tab state. |
| [Dependency injection](https://developer.android.com/training/dependency-injection) | Preserve existing Hilt/ViewModel/repository ownership; no DI migration. |

## 17. Planning verification record

During preparation of this document:

- Inspected current source, build configuration, CI, relevant test locations, and repository instructions.
- Rechecked official documentation for bounded WebView state, current WebKit release metadata, adaptive behavior, R8, startup measurement, and profile generation.
- Identified current implementation gaps separately from unexecuted runtime checks.
- Wrote this plan only.

No app build, unit test, instrumentation test, emulator run, performance measurement, release build, profile generation, live Ravelry login, purchase, service upload, or publication was performed as part of writing this plan.
