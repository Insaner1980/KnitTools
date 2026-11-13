# PR 253 review and dependency fixes

## Confirmed findings

- Codex: `4 inch` was incorrectly classified as a 10 cm gauge. The focused regression failed before the parser fix.
- Greptile: recreating the yarn creation form lost its pending save, allowing a second insert. The focused regression failed before retaining and consuming the ViewModel-owned request.
- CI run 43433436059: `projectCounterSessionAndNavigation` pressed Back immediately after Save session. The failure hierarchy remained on the counter. The test now waits for the save dialog to disappear before navigating back.

## Dependency alerts

These were transitive dependencies of the local `.deepsec` tooling. The five Hono alerts affect the previously locked 4.13.14:

| Advisory | Affected behavior |
| --- | --- |
| GHSA-46wx-v44x-9hh2 | CSRF checks omitted `text/ping` requests |
| GHSA-4h7h-rv7c-7m2v | Form validation skipped multipart requests with extra parameters |
| GHSA-4m76-xfj5-7975 | Mounted paths could bypass scoped middleware |
| GHSA-h7mg-cq63-vhpq | RegExpRouter could skip wildcard middleware |
| GHSA-3gg2-335m-rxc6 | CSS helper interpolation could allow HTML injection |

GitHub's advisory API identifies 4.13.16 as the first patched version for these five advisories. The lockfile now uses the latest stable 4.13.18, which includes subsequent security corrections described in [the upstream release](https://github.com/honojs/hono/releases/tag/v4.13.18).

Zod GHSA-mrhx-c7xc-m3w5 concerns excessive memory use while validating oversized constrained collections. The old Sandbox dependency used locally patched Zod 3.24.4, so version-based auditing still reported it. Sandbox now resolves upstream Zod 4.6.7; the obsolete backport was removed. The existing oversized collection regressions and a Sandbox validator compatibility check pass for the actual resolved package.

`pnpm --dir .deepsec install --frozen-lockfile` succeeded. `pnpm --dir .deepsec test:dependency-security` passed 20 tests. `pnpm --dir .deepsec audit` reported no known vulnerabilities.

## Local checks

- 57 focused JVM tests passed: InstructionParserTest (43), ProjectYarnNoteEditViewModelTest (6), CounterEditorWriteResultTest (8); no failures or skips.
- `:app:ktlintCheck`, `:baselineprofile:ktlintCheck` and `:app:detekt` passed.
- Debug app, Android test APK, optimized benchmarkRelease app and benchmarkRelease test driver built successfully with `-Pknittools.isolatedTest=activepolicy -Pknittools.optimizedSmoke=true`.
- `ProjectYarnNoteCreationRestoreTest` passed on a fresh API 32 emulator (1 test). Two preceding API 36 attempts did not reach the test: the overloaded emulator reported startup ANRs, including System UI and Settings ANRs. Those attempts are not counted as passing tests.
- `OptimizedSmokeTest#projectCounterSessionAndNavigation` passed on the same API 32 emulator (1 test, 54.613 seconds), through `tools/optimized-smoke.ps1 -DisposableEmulator -ExpectedTests 1`. The script verified the test-only, non-debuggable optimized APK, restricted permissions and matching R8 mapping identity. The other six smoke scenarios were not rerun.
- No 320 dp, 200% font or TalkBack checks were repeated.

## Service limitation

CodeRabbit skipped the original PR review because 209 eligible files exceeded its 150-file limit. This is not a source-code failure; review coverage has not been reduced to conceal it.
