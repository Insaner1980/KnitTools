# KnitTools Codex Code Review Questions


## 1. Build and Gradle configuration

Source of truth: `app/build.gradle.kts`, `baselineprofile/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`, root `build.gradle.kts`.

### BUILD-1

```
[BUILD-1] Are all version numbers actually read from `gradle/libs.versions.toml` rather than hardcoded anywhere in the build scripts? List any hardcoded versions outside the catalog.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-2

```
[BUILD-2] Is the `org.jetbrains.kotlin.android` plugin genuinely absent, and is the Compose plugin (`org.jetbrains.kotlin.plugin.compose`) applied correctly without it?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-3

```
[BUILD-3] Is `dependency-analysis` actually commented out in the root build for AGP 9.x compatibility, and is there a dangling reference to it anywhere that would break configuration?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-4

```
[BUILD-4] Does the release build genuinely block artifacts when signing env vars are missing? Trace the exact condition and confirm it fails the build rather than silently producing an unsigned artifact.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-5

```
[BUILD-5] Does the release build genuinely block artifacts when Ravelry credentials are missing? Confirm the check covers all four `RAVELRY_*` BuildConfig fields, not just one.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-6

```
[BUILD-6] Is the opt-in flag `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true` checked strictly (exact string equality, case sensitivity), and does a missing or malformed value fail closed?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-7

```
[BUILD-7] Does the debug build read Ravelry keys from `debug.credentials.properties`, and is that file path handled gracefully when absent (debug build still works without secrets)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-8

```
[BUILD-8] Are any secrets, keys, or credentials accidentally printed to logs, written to BuildConfig in a way that lands in debug logs, or exposed in a generated file that could be committed?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-9

```
[BUILD-9] Is `compileSdk` / `targetSdk` / `minSdk` set to `36 / 36 / 29` consistently across `:app`, and is `:baselineprofile` `minSdk` `29`? Flag any drift between modules.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-10

```
[BUILD-10] Is the Java target `17` set consistently (Kotlin `jvmTarget`, `compileOptions`, toolchain) so there is no mismatch that would cause subtle build warnings?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-11

```
[BUILD-11] Is Room configured with a schema directory, and does the configured path match `app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-12

```
[BUILD-12] Is the `google-services` plugin genuinely not applied, with no `apply plugin: 'com.google.gms.google-services'` or KTS equivalent hidden anywhere?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-13

```
[BUILD-13] Are there any dependencies declared that are never used (dead deps), or used but not declared (relying on transitive)? Note only clear cases.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-14

```
[BUILD-14] Does the Sonar config actually run `:app:jacocoDebugUnitTestReport` before `sonar`, and do the delegated source/binary paths exist?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-15

```
[BUILD-15] Is `jacoco` configured so coverage reports are produced for the debug unit test variant without failing the build when a module has no tests?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-16

```
[BUILD-16] Are ktlint and detekt configured to actually fail the build on violations in CI (not just warn)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-17

```
[BUILD-17] Is the OWASP `dependency-check` plugin applied so it does not run automatically on every build (kept manual), and does its absence from the default flow not break `assembleDebug`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-18

```
[BUILD-18] Does `settings.gradle.kts` declare exactly `:app` and `:baselineprofile`, with repositories locked down (no unexpected repos that could pull untrusted artifacts)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-19

```
[BUILD-19] Is the baseline profile module wired so it is not included in the release APK as a runtime dependency (test-only)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BUILD-20

```
[BUILD-20] Are ProGuard/R8 rules present and correct for Room, Hilt, Ktor serialization, and Glance so release minification does not strip needed classes? Check for missing keep rules.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 2. Application startup and lifecycle

Source of truth: `App.kt`, `MainActivity.kt`, `MainActivityTheme.kt`, `AndroidManifest.xml`, `themes.xml`, `CounterLaunchRequest.kt`, `CounterLaunchTokenStore.kt`.

### START-1

```
[START-1] In `App.onCreate()`, are `applyStoredAppLanguage()`, `BillingManager.initialize()`, and `ProManager.initialize()` called in an order that cannot deadlock or block the main thread for a noticeable time?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-2

```
[START-2] Does `installSplashScreen()` run strictly before `super.onCreate()` in `MainActivity`, as the splash screen API requires?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-3

```
[START-3] Is the splash kept on screen only until the startup theme is resolved, with a guaranteed exit condition so it cannot hang forever if the preferences flow never emits?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-4

```
[START-4] Is `enableEdgeToEdge()` genuinely deferred until the light/dark theme is known, and does deferring it not cause a visible flash or inset jump on the first frame?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-5

```
[START-5] When reading the `CounterLaunchRequest` from the intent, is malformed or missing extra data handled without crashing (null safety on every extra)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-6

```
[START-6] Does the Ravelry OAuth callback handling in `onCreate` / `onNewIntent` correctly distinguish a real callback URI from other intents so it cannot mis-trigger?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-7

```
[START-7] Verify the code enforces that the counter only opens for a valid widget launch id from `CounterLaunchTokenStore`, and that an OAuth callback intent cannot satisfy that path.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-8

```
[START-8] Is a consumed counter launch id correctly retained across Activity recreation (config change) so the counter is not re-opened twice, and are consumed intent extras cleared?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-9

```
[START-9] In `onResume()`, does `checkDownloadedOnResume()` run safely when no update is pending (no null pointer, no repeated prompts)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-10

```
[START-10] Does `syncAppLanguageFromSystem()` run only on Android 13+ and mirror the per-app locale into DataStore without creating a write loop with `applyStoredAppLanguage()`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-11

```
[START-11] Is the In-App Update check started in a way that does not block startup and does not crash if Play services are unavailable (sideloaded debug build)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-12

```
[START-12] Is the review request tied to runtime state rather than a static screen, and guarded so it cannot fire repeatedly or during an OAuth/widget launch?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-13

```
[START-13] Does `onNewIntent` update the stored intent (`setIntent`) before processing, so OAuth and widget launches read the new intent and not the stale one?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-14

```
[START-14] Is the activity locked to portrait in the manifest, and does any code path attempt to change orientation in a way that conflicts with that lock?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-15

```
[START-15] Are any long running operations (billing init, pro init, file IO) accidentally started on the main thread in `App.onCreate()` or `MainActivity.onCreate()`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### START-16

```
[START-16] Is the theme read directly from the `PreferencesManager.preferences` flow without a second source of truth that could disagree at startup?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 3. Dependency injection (Hilt) and scoping

Source of truth: `di/DatabaseModule.kt`, `di/DispatchersModule.kt`, `di/NetworkModule.kt`, `@HiltAndroidApp` on `App`, ViewModels, managers.

### HILT-1

```
[HILT-1] Is `App` annotated `@HiltAndroidApp`, and are all activities/receivers that need injection annotated appropriately (`@AndroidEntryPoint`, custom entry points for Glance)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-2

```
[HILT-2] Are singletons that hold mutable state (`BillingManager`, `ProManager`, `TrialManager`) genuinely `@Singleton` scoped, with no second instance created manually anywhere?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-3

```
[HILT-3] Does `DispatchersModule` provide named/qualified dispatchers, and are IO bound operations injected with the IO dispatcher rather than hardcoding `Dispatchers.IO` in repositories?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-4

```
[HILT-4] Is the Room database provided as a singleton, and is `KnitToolsDatabase` never built more than once at runtime?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-5

```
[HILT-5] In `NetworkModule`, are the Ktor client and OkHttp client provided as singletons (not recreated per request), and are the timeouts exactly connect 15s, call 45s, read/write 30s?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-6

```
[HILT-6] Are any `@Provides` methods returning a new instance each call where a singleton is expected, causing duplicated state or wasted resources?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-7

```
[HILT-7] Is `CounterViewModel` scoped to the Projects nav graph entry, `LibraryViewModel` to the Library graph, and `YarnCardViewModel` to the `yarn_card_detail/{cardId}` parent entry? Verify the scoping mechanism in the nav code.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-8

```
[HILT-8] Do any injected dependencies form a circular constructor injection cycle?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-9

```
[HILT-9] Are `ApplicationContext` vs `ActivityContext` qualifiers used correctly, with no `Activity` context held by a singleton (leak risk)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### HILT-10

```
[HILT-10] For the Glance widget entry point (`WidgetEntryPoint.kt`), is the Hilt entry point pattern implemented correctly so the widget can resolve repositories without an `@AndroidEntryPoint` activity?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 4. Navigation

Source of truth: `Screen.kt`, `NavGraph.kt`, `KnitToolsBottomBar.kt`, `CounterLaunchRequest.kt`.

### NAV-1

```
[NAV-1] Is `TopLevelDestination` in `Screen.kt` the single source of truth for the five tabs, with no duplicate tab list defined elsewhere that could drift?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-2

```
[NAV-2] Do the five start routes (`project_list`, `library`, `tools`, `insights`, `settings`) each exist as destinations and match the strings declared per top level?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-3

```
[NAV-3] Is `HIDE_BOTTOM_BAR_ROUTES` exactly `pro_upgrade`, `pattern_viewer/{projectId}`, `library_pattern_viewer/{savedPatternId}`, `notes_editor/{projectId}`, and does the matching logic handle parameterized routes correctly (not a naive string equality that fails on arguments)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-4

```
[NAV-4] Verify `Screen.Counter.route` is NOT in `HIDE_BOTTOM_BAR_ROUTES`, so the bottom bar stays visible on the counter.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-5

```
[NAV-5] Does the `counter` route correctly carry no `projectId` argument, with the active project resolved from the shared `CounterViewModel`? Confirm there is no leftover `counter/{projectId}` route.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-6

```
[NAV-6] For every route that takes an argument (`{projectId}`, `{cardId}`, `{patternId}`, `{savedPatternId}`), is the argument extracted with a safe fallback so a missing or malformed argument cannot crash the screen?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-7

```
[NAV-7] Are nav argument types declared (`NavType.StringType` / `LongType`) consistently with how IDs are stored in Room, so there is no silent type mismatch?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-8

```
[NAV-8] Do the old routes `yarn_card_review` and `library_yarn_card_review` genuinely no longer exist anywhere in `Screen.kt` / `NavGraph.kt`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-9

```
[NAV-9] When navigating between tabs, is back stack handling correct (single top, restore state, save state) so tab switching does not pile up duplicate destinations?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-10

```
[NAV-10] Do widget launch, Ravelry Start Project, yarn card detail, and project list all funnel through the same project selection model in `CounterViewModel`, with no divergent path that sets the active project differently?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-11

```
[NAV-11] Is `library_pattern_viewer/{savedPatternId}` used only for local/imported pattern URIs and `library_ravelry_detail/{patternId}` for Ravelry links, with no path that sends a Ravelry-only pattern to the local viewer?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-12

```
[NAV-12] Are deep link navigations from project list cards (pattern, photos, notes, yarn) guarded against navigating to a destination whose backing data was deleted between render and tap?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-13

```
[NAV-13] Is there any race where a rapid double tap on a nav action pushes the same destination twice? Check for debouncing or a guard.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NAV-14

```
[NAV-14] Does the bottom bar label sizing logic (runtime shrink to fit five localized labels) have a sane lower bound so labels never collapse to unreadable sizes in long-locale languages (German, Finnish)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 5. Room entities, DAOs, and indexes

Source of truth: `data/local/KnitToolsDatabase.kt`, entity classes, DAOs, `app/schemas/.../12.json`.

### ROOM-1

```
[ROOM-1] Is `KnitToolsDatabase.version` set to `12`, and does it match the latest exported schema `12.json`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-2

```
[ROOM-2] Are all ten entities actually registered in the `@Database(entities = [...])` array, with none missing or duplicated?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-3

```
[ROOM-3] For each foreign key relationship (counters/history/notes referencing a project), is `onDelete` behaviour defined explicitly so deleting a project does not orphan rows or violate constraints?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-4

```
[ROOM-4] Are the indexes `sessions(endedAt, startedAt)`, `sessions(projectId, endedAt, startedAt)`, and `sessions.startedAt` present in the entity annotations, not just in the migration?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-5

```
[ROOM-5] Do DAO queries that filter or sort on `projectId`, `endedAt`, or `startedAt` actually benefit from those indexes, or is there a hot query with no supporting index?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-6

```
[ROOM-6] Are DAO methods that return many rows exposed as `Flow` or suspend functions (not blocking calls on the main thread)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-7

```
[ROOM-7] Does `SessionDao.getTotalMinutes(...)` sum `durationSeconds` and round up to minutes correctly, with no integer division truncation that loses the rounding?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-8

```
[ROOM-8] Are nullable columns handled in the entity so reading a legacy row with a null in a newly added column does not crash deserialization?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-9

```
[ROOM-9] For CSV-style columns like `counter_projects.yarnCardIds`, is parsing always done through `domain/model/YarnCardLinks.kt` and never with ad hoc string splitting elsewhere?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-10

```
[ROOM-10] Do any `@Query` strings with hardcoded column names risk silently breaking if a column were renamed? Note the riskiest ones.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-11

```
[ROOM-11] Do any DAO upsert/insert methods use a conflict strategy (`REPLACE` vs `ABORT` vs `IGNORE`) that could unexpectedly wipe related data on conflict?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ROOM-12

```
[ROOM-12] Are date/time values stored in a consistent type and unit across entities (epoch millis vs seconds), with no column mixing units?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 6. Room migrations 1 to 12

Source of truth: migration definitions in `KnitToolsDatabase.kt` (or a migrations file), exported schemas `1.json` through `12.json`.

### MIG-1

```
[MIG-1] Is every version step covered with no gap? Confirm `1->2`, `2->3` are auto migrations and `3->4` through `11->12` are present as manual migrations.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-2

```
[MIG-2] In migration `9->10`, does the backfill `sessions.durationSeconds = durationMinutes * 60` handle null or zero `durationMinutes` without producing a negative or nonsensical value?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-3

```
[MIG-3] In `9->10`, is `rowsWorked` backfilled with a positive `endRow - startRow`, and what happens when `endRow < startRow`? Confirm it clamps rather than storing a negative.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-4

```
[MIG-4] In `10->11`, are the two new session indexes created safely against re-run, and do their names not collide with existing indexes?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-5

```
[MIG-5] In `11->12`, does creating `project_yarn_notes` use a `CREATE TABLE` whose columns and types exactly match the entity and the exported `12.json`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-6

```
[MIG-6] For each manual migration doing `ALTER TABLE ADD COLUMN`, does the new column have a default or is it nullable, so existing rows do not violate NOT NULL?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-7

```
[MIG-7] Do any migrations recreate a table (create new, copy, drop, rename), and if so is the column order and data copy complete, with no dropped data?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-8

```
[MIG-8] Is `targetRows` added in `7->8` as nullable or with a default, and does the counter logic treat a null/zero target as no target rather than crashing or showing a 0 progress bar?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-9

```
[MIG-9] Are all migrations registered in the database builder via `.addMigrations(...)`, with the exact set matching what is defined? Flag any defined-but-not-registered migration.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-10

```
[MIG-10] Is any destructive fallback (`fallbackToDestructiveMigration`) absent from the production builder, so a missing migration fails loudly instead of wiping user data?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-11

```
[MIG-11] Do the migration tests actually run each migration against real seeded data and assert the data survives, rather than only asserting the schema shape?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### MIG-12

```
[MIG-12] Are SQL string literals in migrations free of typos in column/table names that would only fail at runtime on a user's device (not in a clean install)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 7. DataStore and preferences

Source of truth: `data/datastore/PreferencesManager.kt`, `AppLanguage.kt`, plus `trial_state`, `review_state`, `counter_widget` stores.

### DATA-1

```
[DATA-1] Is each separate DataStore (`preferences`, `trial_state`, `review_state`, `counter_widget`) created exactly once as a singleton, not re-created per access (which corrupts DataStore)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### DATA-2

```
[DATA-2] Are reads exposed as `Flow` with sensible default values so a fresh install (empty store) never returns null or throws?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### DATA-3

```
[DATA-3] Does `applyStoredAppLanguage()` avoid an infinite write loop with the `onResume` system-locale sync? Trace the read/write paths for `app_language`.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### DATA-4

```
[DATA-4] Are enum-backed preferences (`ThemeMode`, `AppLanguage`, `ProjectSortOrder`) stored via a stable `persistedValue`, so renaming an enum constant later would not break stored data?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### DATA-5

```
[DATA-5] When an unknown/invalid persisted enum value is read, does the mapping fall back to a safe default rather than throwing?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### DATA-6

```
[DATA-6] Are writes done with `edit { }` (transactional) and not racing each other, so two rapid toggles cannot interleave and lose a value?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### DATA-7

```
[DATA-7] Is the dismissed-tooltip set stored so it grows bounded (not an ever-growing string), and is each one-shot tooltip keyed uniquely?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### DATA-8

```
[DATA-8] Does any preference read happen synchronously/blocking on the main thread (e.g. `runBlocking` on the preferences flow) in a hot path?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 8. File and photo storage

Source of truth: `data/storage/` (AppFileStorage, PatternDocumentStorage, PdfPageRenderer, ProgressPhotoStorage, YarnPhotoStorage, StorageFileNames, CounterLaunchTokenStore), `res/xml/file_paths.xml`, `PatternAttachmentUriResolver.kt`.

### FILE-1

```
[FILE-1] Does `resolvePatternAttachmentUri(...)` return `null` when copying an external PDF fails, and does the caller then NOT attach a pattern (no half-attached state)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-2

```
[FILE-2] When a pattern URI is already app-owned, is it reused without re-copying, and is the already-app-owned check robust against spoofed URIs?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-3

```
[FILE-3] Does `file_paths.xml` expose only `progress_photos` and `pattern_captures`, and crucially NOT `yarn_photos` or `pattern_pdfs`? Confirm no over-broad `root-path`/`external-path` entry leaks the whole storage.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-4

```
[FILE-4] Are PDFs and photos written to internal storage (`pattern_pdfs/<projectId>`, `progress_photos/<projectId>`, `yarn_photos/<cardId>`) with path components sanitized so a crafted id cannot do path traversal (`../`)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-5

```
[FILE-5] On yarn card photo replacement, is the old app-owned image deleted ONLY after the new URI is successfully stored, and is the newly copied image cleaned up if the save fails? Trace both failure branches.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-6

```
[FILE-6] Does `AppFileStorage` correctly recognize legacy roots (`yarn_photos`, `progress_photos`, `pattern_captures`, `pattern_pdfs`, `patterns`) for cleanup, without accidentally deleting a still-referenced file?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-7

```
[FILE-7] Are file streams always closed via `use { }` or try/finally, with no leaked file descriptors on the error path?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-8

```
[FILE-8] Does `PdfPageRenderer` close the `PdfRenderer` and each `Page` it opens, and is it safe against a corrupt or non-PDF file passed in?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-9

```
[FILE-9] When a project is deleted, are its associated files (pattern PDFs, captures, progress photos) actually removed, or do they leak and accumulate over time?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-10

```
[FILE-10] Is camera capture output written to `pattern_captures/<projectId>` and shared only via FileProvider, with the temporary file cleaned up after use?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-11

```
[FILE-11] Is all file IO performed off the main thread (on the IO dispatcher), including the copy operations and deletions?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-12

```
[FILE-12] Are file names generated through `StorageFileNames` consistently, with no two code paths generating clashing names that could overwrite each other?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### FILE-13

```
[FILE-13] Does `CounterLaunchTokenStore` generate launch ids that cannot be guessed or replayed by an external app to force-open a counter?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 9. Ravelry OAuth2 PKCE and token storage

Source of truth: `auth/RavelryAuthManager.kt`, `repository/RavelryRepository.kt`, `data/remote/RavelryApiService.kt`.

### OAUTH-1

```
[OAUTH-1] Is the PKCE flow using `S256` (not `plain`), and is the `code_verifier` generated with a cryptographically secure random of sufficient length (43 to 128 chars)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-2

```
[OAUTH-2] Is the `code_challenge` computed as base64url(SHA-256(verifier)) without padding, matching the PKCE spec exactly?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-3

```
[OAUTH-3] Is the OAuth `state` parameter generated randomly, stored, and verified on callback to prevent CSRF, with a mismatch rejected?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-4

```
[OAUTH-4] Are `access_token`, `refresh_token`, pending `state`, and `code_verifier` stored in `EncryptedSharedPreferences` with a `MasterKey`, and never in plain SharedPreferences or logs?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-5

```
[OAUTH-5] Is the callback URI exactly `com.finnvek.knittools://oauth/callback`, and does the manifest intent filter match it without being so broad it captures unrelated links?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-6

```
[OAUTH-6] On token refresh, after a 401/403 the code refreshes and retries once; is there a guard against an infinite refresh loop if refresh itself keeps returning 401?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-7

```
[OAUTH-7] After a refresh failure, does the code sign out and clear stored tokens, and does the fallback to Basic Auth not leak user-specific data (Basic Auth is app-level, not user-level)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-8

```
[OAUTH-8] Is the Custom Chrome Tab launched correctly, and is the case handled where the user has no browser that supports Custom Tabs (graceful fallback, no crash)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-9

```
[OAUTH-9] Is there a race between the OAuth callback and the encrypted store read/write that could leave tokens half-written if the process is killed mid-flow?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-10

```
[OAUTH-10] Are the `code_verifier` and `state` cleared after a successful or failed exchange so they cannot be reused?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-11

```
[OAUTH-11] Does pattern search work without sign-in, and is the sign-in banner truly dismissible without blocking search?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### OAUTH-12

```
[OAUTH-12] Is the client secret used only where unavoidable (token exchange / Basic Auth) and never exposed to the Custom Tab or included in the authorization URL?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 10. Ravelry networking (Ktor / OkHttp)

Source of truth: `data/remote/RavelryApiService.kt`, `RavelryModels.kt`, `di/NetworkModule.kt`, `repository/RavelryRepository.kt`.

### NET-1

```
[NET-1] Are the timeouts (connect 15s, call 45s, read/write 30s) actually applied to the HTTP client, at the OkHttp layer that Ktor uses?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-2

```
[NET-2] Is transient 5xx retry genuinely bounded (fixed max attempts), and does it use backoff so it does not hammer the API?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-3

```
[NET-3] Do non-2xx responses other than retried 5xx raise `RavelryHttpException`, and is that exception caught everywhere it can propagate so the UI shows an error instead of crashing?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-4

```
[NET-4] Are JSON models tolerant of missing/extra fields (lenient / ignoreUnknownKeys), so a Ravelry API change does not crash deserialization?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-5

```
[NET-5] Is `usesCleartextTraffic="false"` respected, with every Ravelry endpoint using HTTPS and no accidental `http://` URL constructed anywhere?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-6

```
[NET-6] Are network calls cancellable (tied to a coroutine scope) so navigating away cancels in-flight requests rather than leaking?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-7

```
[NET-7] Is pagination of search results handled correctly (page boundaries, end-of-results), with no off-by-one that skips or duplicates a page?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-8

```
[NET-8] Are user-supplied search queries URL-encoded so special characters do not break the request or inject into the query string?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-9

```
[NET-9] Does the repository expose network state (loading/error/success) in a way the UI can render, rather than swallowing errors silently?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NET-10

```
[NET-10] Is there any place a network exception is caught with an empty `catch {}` that hides real failures?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 11. Billing, Pro, and Trial

Source of truth: `billing/BillingManager.kt`, `pro/ProManager.kt`, `pro/ProState.kt`, `pro/TrialManager.kt`, `pro/InAppReviewManager.kt`, `pro/InAppUpdateManager.kt`.

### BILL-1

```
[BILL-1] Is `PRODUCT_ID = "knittools_pro"` and the trial length `14` days each defined in exactly one place, with no second hardcoded copy that could drift?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-2

```
[BILL-2] Does the billing flow handle `BillingResponseCode` cases (OK, USER_CANCELED, ITEM_ALREADY_OWNED, SERVICE_DISCONNECTED) explicitly, especially restoring an already-owned purchase?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-3

```
[BILL-3] Is the purchase acknowledged after a successful one-time purchase? An unacknowledged purchase is auto-refunded by Play after 3 days. Confirm acknowledgement happens.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-4

```
[BILL-4] Since this is a one-time (non-consumable) product, is it NOT consumed anywhere (consuming would let the user re-buy and lose Pro)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-5

```
[BILL-5] Is purchase state verified on `BillingClient` reconnect (`queryPurchasesAsync`) so Pro status survives reinstall and is restored without a new purchase?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-6

```
[BILL-6] Does `TrialManager` compute trial expiry from a stored start timestamp in a way that resists trivial clock manipulation, or at least does not crash on a backwards clock?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-7

```
[BILL-7] Is the `ProStatus` transition (`TRIAL_ACTIVE` to `TRIAL_EXPIRED` to `PRO_PURCHASED`) one-directional where it should be, and can a purchase always override an expired trial?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-8

```
[BILL-8] Confirm `ProState.hasFeature(feature)` currently returns the same as `isPro`, that this is intentional, and that no UI path assumes true per-feature gating that does not exist yet.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-9

```
[BILL-9] Are all `ProFeature` entries actually referenced by a gate somewhere, or are some enum values dead (gated nowhere)? List any unused ones.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-10

```
[BILL-10] Is `BillingClient` connection lifecycle managed so it reconnects after `SERVICE_DISCONNECTED` and is ended/released appropriately, without leaking the client?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-11

```
[BILL-11] Is the In-App Review request throttled by the Play API limits and not triggered immediately on first launch or during onboarding?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-12

```
[BILL-12] Does the flexible In-App Update flow handle the downloaded-but-not-installed state across `onResume`, and does declining an update not soft-lock the app?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### BILL-13

```
[BILL-13] Is Pro state exposed as a single observable source so every gated screen reads the same value, with no screen caching a stale `isPro`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 12. Counter screen and workspace

Source of truth: `counter/CounterScreen.kt`, `CounterWorkspaceSections.kt`, `CounterViewModel.kt`, `ui/theme/CounterDimens.kt`.

### CNT-1

```
[CNT-1] Is the counter workspace a single `LazyColumn` in `CounterWorkspaceSections.kt`, with the first item being `counter-hero`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-2

```
[CNT-2] Does the top bar show only back arrow, uppercase project name, and overflow, with no pattern subtitle / PDF name / Pattern attached text leaking into the header?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-3

```
[CNT-3] Are the removed components `CounterQuickActions` and `CounterProjectInfo` genuinely gone from the workspace (not just unused but still present)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-4

```
[CNT-4] Does incrementing/decrementing the row counter persist through `CounterViewModel` and the repository, and is rapid tapping coalesced so no increments are lost or double-applied?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-5

```
[CNT-5] Can the row counter go below zero, and if not, is the lower bound enforced in the view model (single source) rather than only by disabling the button in the UI?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-6

```
[CNT-6] Does the progress bar compute progress safely when `targetRows` is null or zero (no divide-by-zero, no NaN width)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-7

```
[CNT-7] Is the large row number using the fontScale-compensated ~115sp Bold style, and does it remain readable at large system font scales without clipping?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-8

```
[CNT-8] Is the active project read from the shared `CounterViewModel` and not re-derived from a stale snapshot, so widget / Start Project / list selections are always reflected?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-9

```
[CNT-9] When the active project is deleted while the counter is open, does the screen handle the now-missing project gracefully (navigate away or empty state) rather than crash?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-10

```
[CNT-10] Are haptic feedback calls gated behind the user preference, and do they no-op cleanly when disabled or when the device lacks a vibrator?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-11

```
[CNT-11] Is keep-screen-awake applied only while the counter is active and removed on leave, so it does not keep the screen on app-wide?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-12

```
[CNT-12] Does the repeat/section row render only when the active project uses repeats, and is the section math (current repeat, rows per repeat) correct at boundaries?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CNT-13

```
[CNT-13] Is dimension token usage consistent through `CounterDimens.kt` (no stray hardcoded dp for hero/grid/touch targets that bypass the tokens)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 13. Counter reducers and decisions

Source of truth: `CounterUiStateReducers.kt`, `CounterScreenDecisions.kt`, `CounterViewModel.kt`.

### RED-1

```
[RED-1] In `CounterUiStateReducers`, when project, counter changes, active reminder, and dismissed-reminder state are combined into `CounterUiState`, is every input null-safe so a missing piece yields a sensible partial state?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### RED-2

```
[RED-2] Is the reducer free of side effects (pure transformation), with no IO or navigation triggered inside it?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### RED-3

```
[RED-3] Does `requestCounterFeature` correctly route a gated feature to the Pro upgrade flow when the user is not Pro, and proceed when they are?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### RED-4

```
[RED-4] Does `handleStitchTrackingToggle` require a positive stitch count before enabling tracking, and does it prompt for the count rather than silently enabling with zero?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### RED-5

```
[RED-5] Is the dismissed-reminder state scoped correctly (per reminder, per session) so dismissing one reminder does not hide all of them, and does it reset when a new reminder becomes active?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### RED-6

```
[RED-6] Are state combinations using `combine`/`stateIn` with a correct `SharingStarted` policy so the UI does not lose state on configuration change or brief unsubscribe?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### RED-7

```
[RED-7] Is there any place the reducer recomputes on every emission unnecessarily, causing excess recomposition? Note only clear inefficiencies.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 14. Project content cards

Source of truth: `counter/CounterProjectContentCards.kt`, `ui/components/ProjectCard.kt`.

### CARD-1

```
[CARD-1] Does `ProjectContentCards` always render exactly the five cards (pattern, yarn, notes, photos, reminders) with no preview text, file names, image counts, chevrons, or reminder messages?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CARD-2

```
[CARD-2] Is the pattern card title `Open Pattern` when `patternUri` or `linkedPattern` exists, and `Add Pattern` otherwise, with the branch logic matching that rule?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CARD-3

```
[CARD-3] When only `linkedPattern` exists (no `patternUri`), does the pattern card open the pattern-info path and not attempt to open a missing PDF?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CARD-4

```
[CARD-4] Do the notes and photos cards respect the Pro gate before opening their flows, routing non-Pro users to upgrade?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CARD-5

```
[CARD-5] Does `ProjectCard` hide a raw `.pdf` file name from the secondary line when that would be the only pattern name shown?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CARD-6

```
[CARD-6] Are the five cards laid out as squares using the dimension tokens, and do they reflow correctly on small/large screens without overlap or clipping?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CARD-7

```
[CARD-7] Is each card's tap target the whole card (accessible size) and not just an inner icon?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 15. Yarn management sheet and project yarn notes

Source of truth: `counter/YarnManagementSheet.kt`, `CounterViewModel.kt`, `repository/ProjectYarnNoteRepository.kt`.

### YARN-1

```
[YARN-1] Does the yarn management sheet show both linked My Yarn cards and project-specific yarn notes in the same sheet, with the two actions `Choose from My Yarn` and `Add yarn to project`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YARN-2

```
[YARN-2] Confirm `Add yarn to project` (a project yarn note) does NOT automatically create a My Yarn card.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YARN-3

```
[YARN-3] Does `saveProjectYarnNote(...)` persist a `project_yarn_notes` row via `ProjectYarnNoteRepository.save(...)` and nothing else?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YARN-4

```
[YARN-4] Does `saveProjectYarnNoteToMyYarn(noteId)` create a linked `YarnCard` with status `IN_USE` and store the `savedYarnCardId` reference in the same repository transaction, so a partial failure cannot leave a dangling reference?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YARN-5

```
[YARN-5] Are project yarn notes kept only in the management sheet and never re-surfaced as a `ProjectContentCards` preview?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YARN-6

```
[YARN-6] Are text fields trimmed and validated (non-empty where required) before save, consistent with the My Yarn manual card rules?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 16. Project list

Source of truth: `project/ProjectListScreen.kt`, `ProjectListViewModel`, `repository/CounterRepository.kt`, `ProjectSortOrder`.

### LIST-1

```
[LIST-1] Are active and completed projects fetched from sort-order-aware flows, with the sort order persisted via `ProjectSortOrder.persistedValue`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIST-2

```
[LIST-2] For free users, is creating a new active project blocked when at least one active project already exists, and is that check in the view model (not just the button state)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIST-3

```
[LIST-3] Is `ContinueKnittingProject` selected as the first active project with `count > 0`, and what is shown when no such project exists (no crash, sensible empty state)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIST-4

```
[LIST-4] Do project cards show row count, last-updated day, first linked yarn name, photo count, pattern state, and note indicator, with each value null-safe?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIST-5

```
[LIST-5] Does the yarn deep link use the first result of `parseYarnCardIds(project.yarnCardIds)` and navigate to `yarn_card_detail/{cardId}` in the Library tab? Confirm it handles an empty/invalid CSV without crashing.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIST-6

```
[LIST-6] Is the last-updated timestamp formatted using the device locale and time zone, with no hardcoded format that breaks in some locales?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIST-7

```
[LIST-7] Is completed-project visibility driven solely by `PreferencesManager.showCompletedProjects`, with no second flag?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIST-8

```
[LIST-8] Is the list keyed stably (project id) so reordering does not cause flicker or wrong item recomposition?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 17. Library hub and My Yarn

Source of truth: `library/LibraryScreen.kt`, `library/MyYarnScreen.kt`, `library/LibraryViewModel.kt`, `library/SavedPatternsScreen.kt`, `library/AllPhotosScreen.kt`.

### LIB-1

```
[LIB-1] Does the Library hub compute the saved-pattern, yarn-card, and photo counts from `LibraryViewModel` flows, with counts updating reactively?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIB-2

```
[LIB-2] Does `MyYarnScreen` show an explicit `Add Yarn` button in the empty state and a FAB opening `ManualYarnCardSheet` in the non-empty state, with no overlap or double entry point?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIB-3

```
[LIB-3] Does `createManualYarnCard(...)` trim fields, require a non-empty name, force quantity to at least `1`, and save with status `IN_STASH`?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIB-4

```
[LIB-4] Confirm there is no scanner or AI/label-parse language or code path in the My Yarn flow; the input is manual only.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIB-5

```
[LIB-5] Does long-press start multi-select, and does `deleteCards(...)` remove the selected cards and their CSV references atomically?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIB-6

```
[LIB-6] Is the list summary using text plus one color dot (no metadata pills)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIB-7

```
[LIB-7] Does multi-select state reset correctly on navigation away or on completing a delete, so a stale selection cannot delete the wrong cards later?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LIB-8

```
[LIB-8] Does a saved pattern open `library_pattern_viewer/{savedPatternId}` only for local/imported URIs and `library_ravelry_detail/{patternId}` for Ravelry links, with the branch decided by the actual stored type?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 18. Yarn card detail and link invariants

Source of truth: `yarncard/YarnCardDetailScreen.kt`, `yarncard/YarnCardViewModel.kt`, `repository/YarnCardRepository.kt`, `domain/model/YarnCardLinks.kt`.

### YCARD-1

```
[YCARD-1] Does the detail route exit to Library when `cardId` is missing or `observeCard(id)` reports a deleted row, rather than rendering a broken screen?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-2

```
[YCARD-2] Is the two-way link kept consistent: `yarn_cards.linkedProjectId` (forward) and `counter_projects.yarnCardIds` (reverse CSV), with all writes going through the repository methods?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-3

```
[YCARD-3] Does `updateLinkedProjectId(...)` update both the card and every project's CSV inside one `DatabaseTransactionRunner` transaction, so a crash mid-update cannot leave the two sides disagreeing?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-4

```
[YCARD-4] Does `saveCard(...)` normalize an existing `linkedProjectId` only to a project that still exists, dropping a stale link rather than keeping a dangling reference?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-5

```
[YCARD-5] Does `deleteCards(...)` remove card ids from project CSVs before deleting the card rows, and clean app-owned images on the IO dispatcher, in the right order so no orphan id remains?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-6

```
[YCARD-6] Does project deletion call `clearLinkedProject(projectId)` inside the same transaction as the project delete, so no card points at a gone project?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-7

```
[YCARD-7] Does Edit details reopen `ManualYarnCardSheet` prefilled, and does saving preserve existing optional fields (`fiberContent`, `weightGrams`, `lengthMeters`, `needleSize`, `gaugeInfo`, `careSymbols`, `photoUri`, status, linked project) rather than nulling them?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-8

```
[YCARD-8] Does the photo picker use `PickVisualMedia.ImageOnly`, and does `updatePhotoUri(...)` delegate storage copy and old-image cleanup to the repository (not the composable)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-9

```
[YCARD-9] Is `YarnCardLinks.kt` CSV parsing/formatting symmetric (parse then format yields the same set), and does it ignore blanks/duplicates safely?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### YCARD-10

```
[YCARD-10] When opening the linked project counter from detail via `selectProjectByIdForLaunch(...)`, is the case handled where the linked project was deleted (no crash, graceful message)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 19. Glance widget state and bootstrap

Source of truth: `widget/CounterWidget.kt`, `CounterWidgetState.kt`, `CounterWidgetDataResolver.kt`, `WidgetEntryPoint.kt`.

### WIDG-1

```
[WIDG-1] Does new-instance bootstrap try, in order, instance Glance state, then shared widget store, then `getLatestActiveProject()`, then `CounterWidgetState.defaultData(...)`, with each fallback only used when the prior is empty?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WIDG-2

```
[WIDG-2] Is shared widget state mirrored into instance-specific Glance state consistently, so two widgets on the home screen do not show contradictory data?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WIDG-3

```
[WIDG-3] Are the three responsive sizes (small 120x48, medium 160x160, large 300x160) selected by the actual available size, with correct behaviour at exact boundary sizes?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WIDG-4

```
[WIDG-4] Does small show only name + counter, while medium/large add +/- actions, with no path where +/- appear at the small size?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WIDG-5

```
[WIDG-5] Is widget update work done off the main thread, and is it resilient if the resolver finds no active project (shows default rather than crashing the host launcher)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WIDG-6

```
[WIDG-6] Does the widget reflect target rows, section, and stitch tracking only when the active project uses them, with null-safe rendering otherwise?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WIDG-7

```
[WIDG-7] Is the Pro gate enforced for the widget, and what does a non-Pro user see (graceful, not a broken or empty box)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 20. Widget actions and receiver security

Source of truth: `widget/CounterWidgetActions.kt`, `CounterWidgetReceiver.kt`, `WidgetCounterAction.kt`, `repository/CounterRepository.kt` (`applyWidgetCountChange`), `AndroidManifest.xml`.

### WACT-1

```
[WACT-1] Does `applyWidgetCountChange(...)` perform the count, history, and current-stitch-reset update as a single transaction, so a partial update cannot occur?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WACT-2

```
[WACT-2] `CounterWidgetReceiver` is exported (it must be). Is `CounterWidgetActions` exported=false, and do the action broadcasts validate their extras so a malicious app cannot drive arbitrary counter changes?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WACT-3

```
[WACT-3] Are widget +/- broadcasts idempotent or guarded so a duplicated or replayed broadcast does not double-apply a change?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WACT-4

```
[WACT-4] Is the broadcast handling wrapped so a thrown exception does not crash the launcher process hosting the widget?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WACT-5

```
[WACT-5] After applying a widget action, is the widget state refreshed so the displayed number matches the persisted value without a stale frame?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### WACT-6

```
[WACT-6] Does opening the app from the widget use the launch token mechanism so only a legitimate widget tap opens the counter (consistent with the OAuth-cannot-open-counter rule)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 21. Theme, colors, and typography

Source of truth: `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `Shapes.kt`, `KnitToolsExtendedColors`, `res/font/outfit.ttf`.

### THEME-1

```
[THEME-1] Is dynamic color (Material You) genuinely disabled in both light and dark, with no `dynamicLightColorScheme`/`dynamicDarkColorScheme` call anywhere?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-2

```
[THEME-2] Does `isSystemInDarkTheme()` influence the scheme only when `themeMode == SYSTEM`, and are explicit LIGHT/DARK choices honored regardless of system setting?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-3

```
[THEME-3] Are the extended tokens (`surfaceTint`, `secondaryOutline`, `onSurfaceMuted`, `brandWine`, `inactiveContent`, `navBarContainer`, `navBarIndicator`) provided via a `CompositionLocal` and read through `MaterialTheme.knitToolsColors`, with no token read before it is provided?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-4

```
[THEME-4] Do the documented hex values match the code (spot check Primary `#C45100`, Secondary `#8BA44A`, dark `Background #1E1E12`, light `LightBackground #E8E4D0`)? Flag any mismatch, noting the code is authoritative.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-5

```
[THEME-5] Is the Scaffold background `colorScheme.background` (not `surface`) across screens? Flag any screen using `surface` for the scaffold.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-6

```
[THEME-6] Do text-on-surface color pairings meet a reasonable contrast in both themes, especially `TextMuted`/`TextDisabled`? Note any pairing that looks too low contrast (flag as WORTH A LOOK).

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-7

```
[THEME-7] Is the `YarnColors` icon palette selection deterministic by id, so the same yarn always gets the same color across app restarts?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-8

```
[THEME-8] Is the Outfit variable font loaded via `FontVariation.Settings(weight(...))` for each weight, with no missing weight that silently falls back to a default?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-9

```
[THEME-9] Are local `copy(...)` typography overrides limited to the documented surfaces, or are there ad hoc overrides that should just use an `AppTypography` role?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### THEME-10

```
[THEME-10] Do the all-caps label styles use letter spacing that stays readable, and are the strings uppercased via style rather than hardcoded uppercase in `strings.xml` (which would break localization)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 22. Localization and per-app locale

Source of truth: `res/xml/locales_config.xml`, `values-*` directories, `AppLanguage.kt`, `PreferencesManager`, `strings.xml`.

### L10N-1

```
[L10N-1] Do the locales in `locales_config.xml` (en, fi, sv, de, fr, es, pt, it, nb, da, nl) each have a matching `values-*` directory, and vice versa, with none missing?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### L10N-2

```
[L10N-2] Are there string keys in `values/strings.xml` (default) missing from one or more localized files, which would fall back to English silently? List the most impactful missing keys.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### L10N-3

```
[L10N-3] Are there localized strings with format placeholders (`%1$s`, `%d`) whose argument count or order does not match the default, which crashes at format time?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### L10N-4

```
[L10N-4] Is any user-facing text hardcoded in Kotlin/Compose instead of pulled from `strings.xml`? List the worst offenders.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### L10N-5

```
[L10N-5] Does the per-app locale (Android 13+) and the DataStore mirror stay in sync without a write loop, and on Android 12 and below does language selection still apply correctly?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### L10N-6

```
[L10N-6] Are plurals using `<plurals>` resources rather than string concatenation, especially for row and item counts, since plural rules differ across these 11 locales?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### L10N-7

```
[L10N-7] Is `AppLanguage.promptLanguageName()` confirmed to be a legacy helper with no production callers (its name does not imply a model parser exists)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 23. InstructionParser (regex)

Source of truth: `domain/calculator/InstructionParser.kt`.

### PARSE-1

```
[PARSE-1] Does the parser make zero network or SDK calls, and is there genuinely no voice handler, TTS, microphone permission, or model-based interpretation anywhere in the production tree?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PARSE-2

```
[PARSE-2] Are the regex patterns precompiled (`Regex` instances reused) rather than recompiled on every parse call in a hot loop?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PARSE-3

```
[PARSE-3] Could any regex pattern exhibit catastrophic backtracking on adversarial pasted input (nested quantifiers, ambiguous alternation)? Inspect each pattern for ReDoS risk.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PARSE-4

```
[PARSE-4] Does the parser handle empty input, whitespace-only input, and very long pasted text without crashing or hanging?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PARSE-5

```
[PARSE-5] Are numeric extractions bounded (no overflow) and validated, so a pasted huge number does not break the counter?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PARSE-6

```
[PARSE-6] Does the parser run off the main thread when given large input, or is it fast enough that main-thread execution is safe? Confirm which, with evidence.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PARSE-7

```
[PARSE-7] Are the parser's assumptions about pattern formats tested, and do the tests cover malformed and partial instructions returning a sensible no-match rather than wrong data?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 24. Notes editor

Source of truth: `notes/NotesEditorScreen.kt`, `NotesEditorViewModel`, `repository/CounterRepository.kt` (`saveProjectNotes`).

### NOTES-1

```
[NOTES-1] Is the autosave debounce exactly 1000 ms, and does leaving the screen flush the pending save so the last edit is not lost?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NOTES-2

```
[NOTES-2] Does `saveProjectNotes()` merge edits onto the editor base text so two near-simultaneous saves do not overwrite each other, and is the merge logic actually correct (not just claimed)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NOTES-3

```
[NOTES-3] Is the debounce job cancelled and replaced on each keystroke so only one pending save exists, with no leaked coroutine per keystroke?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NOTES-4

```
[NOTES-4] If the project is deleted while the editor is open, does a pending autosave fail gracefully rather than recreating a notes row for a gone project?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### NOTES-5

```
[NOTES-5] Is cursor position and text selection preserved across recomposition and configuration change?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 25. Insights and session metrics

Source of truth: `insights/InsightsScreen.kt`, `SessionMetrics`, `InsightsUiState`, `SessionDao`.

### INSIGHT-1

```
[INSIGHT-1] Does `SessionMetrics` split cross-midnight sessions into the device's local days correctly, including around DST transitions and time-zone changes?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### INSIGHT-2

```
[INSIGHT-2] Are pace values computed from `durationSeconds` and `rowsWorked` guarded against divide-by-zero (zero rows or zero seconds)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### INSIGHT-3

```
[INSIGHT-3] For non-Pro users, are chart lists emptied while base metrics still compute into `InsightsUiState`, with no chart rendering fabricated placeholder data?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### INSIGHT-4

```
[INSIGHT-4] Does the debug-only footer text appear without session data, and is that branch genuinely debug-gated (not shipping in release)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### INSIGHT-5

```
[INSIGHT-5] Are streak and heatmap calculations correct at boundaries (a session exactly at midnight, an empty day in the middle of a streak)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### INSIGHT-6

```
[INSIGHT-6] Is the metrics computation done off the main thread, and is it cached/derived rather than recomputed on every recomposition?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 26. Concurrency, coroutines, and transactions

Source of truth: repositories, view models, `DispatchersModule`, `DatabaseTransactionRunner`.

### CONC-1

```
[CONC-1] Are all suspend functions in repositories main-safe (they switch to the injected IO dispatcher for blocking work), with no blocking call left on the caller's dispatcher?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CONC-2

```
[CONC-2] Is `viewModelScope` used for UI-tied work and an application-scoped coroutine for work that must outlive the screen? Flag any save launched in `viewModelScope` that gets cancelled on navigation.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CONC-3

```
[CONC-3] Are multi-step database writes wrapped in `DatabaseTransactionRunner` (or Room `@Transaction`) wherever two tables must change together (yarn links, widget count change, note-to-card promotion)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CONC-4

```
[CONC-4] Are there any shared mutable variables accessed from multiple coroutines without synchronization (race conditions on `var` state in managers)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CONC-5

```
[CONC-5] Is `Dispatchers.IO`/`Main` hardcoded anywhere instead of using the injected qualified dispatchers, which would make tests non-deterministic?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CONC-6

```
[CONC-6] Are `Flow` collectors using `flowOn` correctly so upstream heavy work runs off main, and is `stateIn` used with `WhileSubscribed` (with a timeout) rather than `Eagerly` where appropriate?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CONC-7

```
[CONC-7] Could any `combine` of flows emit an inconsistent intermediate state during a transactional update, briefly showing a torn read to the UI?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### CONC-8

```
[CONC-8] Are exceptions inside launched coroutines handled (try/catch or `CoroutineExceptionHandler`) so a failed save does not crash the app or silently disappear?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 27. Security, privacy, and manifest

Source of truth: `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml`, storage classes.

### SEC-1

```
[SEC-1] Confirm the privacy claim holds in code: no analytics SDK, no ad network, no Firebase except Crashlytics, no third-party tracker imported anywhere. List any dependency that contradicts this.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-2

```
[SEC-2] If Crashlytics is present, is it the only Firebase dependency, initialized without pulling in Analytics, and does it avoid sending user content (only crash stacks)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-3

```
[SEC-3] Are the only declared permissions `INTERNET`, `VIBRATE`, `CAMERA`, with `CAMERA` feature `required="false"`? Flag any extra permission.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-4

```
[SEC-4] Is `usesCleartextTraffic="false"`, `allowBackup="false"`, and are `dataExtractionRules` and `fullBackupContent` set, with the backup rules not exposing the encrypted token store or user files?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-5

```
[SEC-5] Is `MainActivity` exported=true only because it needs the launcher/OAuth intent filters, and does every exported component validate its inputs?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-6

```
[SEC-6] Is `FileProvider` exported=false with grant-uri-permissions used per-share, so URIs are not broadly readable?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-7

```
[SEC-7] Are intent extras read from external intents (widget, OAuth) validated for type and range before use, to resist a crafted intent?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-8

```
[SEC-8] Is there any logging (`Log.d`/`println`) that could print tokens, file paths with user content, or full pasted patterns in a release build?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SEC-9

```
[SEC-9] Confirm `app/google-services.json` is not present in the build and is not referenced, consistent with no google-services plugin.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 28. Compose performance and stability

Source of truth: Compose screens, `@Stable`/`@Immutable` annotations, stability analyzer config.

### PERF-1

```
[PERF-1] Are state objects passed into composables stable (data classes with stable members, `ImmutableList` for collections), or do unstable `List`/lambda params cause unnecessary recomposition? Note the worst hot-path cases.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PERF-2

```
[PERF-2] Are `remember`/`derivedStateOf` used so expensive computations are not redone every recomposition (e.g. progress fraction, formatted dates)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PERF-3

```
[PERF-3] In the counter workspace `LazyColumn`, are items keyed, and is the hero not re-laid-out on every counter tick beyond what is needed?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PERF-4

```
[PERF-4] Are lambdas passed to children stable (remembered or method references) rather than freshly allocated each recomposition, where it matters for performance?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PERF-5

```
[PERF-5] Is `collectAsStateWithLifecycle` used so flows stop collecting when the screen is not in the lifecycle (battery and correctness)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PERF-6

```
[PERF-6] Are large images (yarn photos, pattern captures) loaded downsampled/sized rather than full-resolution into a small composable, to avoid memory spikes?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### PERF-7

```
[PERF-7] Does any composable read a `CompositionLocal` (like `knitToolsColors`) outside the provider scope, returning a default unexpectedly?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 29. Error handling and edge cases

Cross-cutting. Inspect catch blocks, null handling, and boundary conditions across the app.

### ERR-1

```
[ERR-1] Are there empty `catch {}` blocks anywhere that swallow exceptions and hide failures? List each.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ERR-2

```
[ERR-2] Are there `!!` non-null assertions on values that can legitimately be null at runtime (nav args, intent extras, query results)? List the risky ones.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ERR-3

```
[ERR-3] Is integer arithmetic on counters, targets, and stitch counts safe from overflow at extreme values?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ERR-4

```
[ERR-4] Do flows that can error have a `catch` operator so a single error does not terminate the stream and freeze the screen?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ERR-5

```
[ERR-5] Are user-triggered destructive actions (delete project, delete cards, reset counter) confirmed before execution, and is deletion immediate and final by design or is there undo?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ERR-6

```
[ERR-6] On a completely empty app (no projects, no yarn, no sessions), does every screen render a sensible empty state with no crash or blank white screen?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ERR-7

```
[ERR-7] Is there any TODO/FIXME/`throw NotImplementedError()` left in a reachable production path? List each with location.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### ERR-8

```
[ERR-8] Are resources that must be released (cursors, renderers, streams, BillingClient) released on every path including exceptions?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 30. Accessibility

Cross-cutting. Inspect content descriptions, touch targets, and font scaling.

### A11Y-1

```
[A11Y-1] Do icon-only controls (+/-, overflow, back, FAB) have meaningful `contentDescription`s for TalkBack, not null or a generic button label?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### A11Y-2

```
[A11Y-2] Are interactive touch targets at least ~48dp, including the +/- controls and content cards?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### A11Y-3

```
[A11Y-3] Does the large counter number and other text survive a 1.3x to 2x system font scale without truncation or overlap?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### A11Y-4

```
[A11Y-4] Is information conveyed by color alone (the single yarn color dot, status) also available as text for color-blind and screen-reader users?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### A11Y-5

```
[A11Y-5] Are decorative images marked as decorative (null content description) so TalkBack does not announce noise?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 31. Testing coverage and quality

Source of truth: test sources for domain calculators, parsers, repositories, storage/migrations, Pro/trial, billing, view models, navigation, widget, workspace, My Yarn/Yarn Card.

### TEST-1

```
[TEST-1] Do the migration tests actually run each migration against seeded data and assert the data survives (not just schema validity)? Identify any migration with no real data-survival test.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-2

```
[TEST-2] Do tests assert the absence of removed components (quick-action/project-info, scanner/AI language) by structure, and would they actually fail if those came back?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-3

```
[TEST-3] Are billing tests covering the already-owned/restore path, the unacknowledged-purchase path, and user-cancel, or only the happy path?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-4

```
[TEST-4] Are there tests for the yarn link transaction boundaries (delete card removes CSV refs, project delete clears links) that would catch a partial-update regression?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-5

```
[TEST-5] Do parser tests cover malformed/empty/oversized input and assert no-match rather than only testing well-formed instructions?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-6

```
[TEST-6] Are any tests asserting on time/`now()` without injecting a clock, making them flaky (session cross-midnight, trial expiry)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-7

```
[TEST-7] Are there tests that pass only because they assert nothing meaningful (no assertions, or asserting a value equals itself)? List any.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-8

```
[TEST-8] Do view model tests use a test dispatcher and `runTest` so coroutine timing is deterministic, rather than relying on real delays?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### TEST-9

```
[TEST-9] Is the navigation argument safety / launch tokenization actually exercised by a test that would catch an OAuth intent opening the counter?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 32. Resource and lifecycle leaks

Cross-cutting. Inspect listeners, callbacks, and long-lived references.

### LEAK-1

```
[LEAK-1] Are registered listeners/callbacks (BillingClient state listener, update listener, broadcast receivers, lifecycle observers) unregistered when no longer needed?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LEAK-2

```
[LEAK-2] Does any singleton hold an `Activity`, `View`, or `Context` reference that outlives the activity (classic leak)?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LEAK-3

```
[LEAK-3] Are Custom Tab / browser sessions and any service bindings released, with no leaked connection after OAuth completes?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LEAK-4

```
[LEAK-4] Is the In-App Update listener removed in the right lifecycle callback so it does not leak the activity?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### LEAK-5

```
[LEAK-5] Are bitmaps from `PdfPageRenderer` recycled or allowed to be GC'd, with no strong reference retained after the page is rendered?

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---

## 33. Final sweep (run last, fresh session)

Cross-cutting verification against PROJECT.md and overall impact. The code is authoritative; a mismatch with PROJECT.md is a documentation finding, not necessarily a bug.

### SWEEP-1

```
[SWEEP-1] Pick the ten most load-bearing PROJECT.md claims (counter route stays in bottom bar, no dynamic color, allowBackup false, schema version 12, no voice/AI, FileProvider roots, transaction boundaries, one-time non-consumable product, encrypted token storage, no analytics) and verify each against the code. For each, state HOLDS or MISMATCH with file:line and quote.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SWEEP-2

```
[SWEEP-2] Across everything reviewed, list the top five issues by real user impact (crash, data loss, money, privacy), each with file:line and quote, and explicitly say if there are none.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

### SWEEP-3

```
[SWEEP-3] List anything that looks suspicious but you could not confirm, as WORTH A LOOK, so a human can judge. Do not inflate this list to seem productive; an empty list is fine.

Rules for answering this question (follow all of them):
1. This is a read-only review. Do not modify any code. Only inspect and report.
2. LLMs hallucinate. Do not invent a problem just to have something to report. Inventing a bug that is not real is worse than finding nothing.
3. "No issue found" is a correct and fully expected answer. This specific question may well have nothing wrong with it. If so, say that plainly.
4. Every finding MUST include the exact file path, the exact line number(s), and a verbatim quote of the code. No quote means no finding.
5. If you cannot find the file or code this question refers to, say "could not locate this in the codebase" instead of guessing or describing what it probably does.
6. Do not propose a rewrite that adds complexity. If a fix is needed, prefer the smallest, simplest change. Simpler beats clever.
7. Label your answer as exactly one of: CONFIRMED ISSUE / WORTH A LOOK / NO ISSUE.
8. If this question is based on a wrong assumption about how the code actually works, say that directly instead of forcing an answer to fit.
```

---
