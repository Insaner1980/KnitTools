# KnitTools Code Review Prompts: Part 01

Prepared on November 10, 2026, Europe/Helsinki, from the supplied PROJECT.md and prompt-writing instructions. These are review instructions, not completed audit findings. Every prompt includes its own complete rules. Execute the numbered prompts in order, preserve the exact continuation boundary if interrupted, and never claim unperformed work is complete.

## Part 01: Prompts 0001 to 0050

This part contains 50 independent prompts. Global numbering continues across parts.

### Repository evidence and review boundaries

#### Prompt 0001: Actual working tree versus the documented snapshot

```text
KNITTOOLS CODE REVIEW PROMPT 0001
Title: Actual working tree versus the documented snapshot
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Establish the actual repository root, branch, HEAD, index and nonignored untracked source before evaluating KnitTools. Compare these with the documented dirty snapshot without assuming its commit reproduces its local changes. Identify which current files implement the selected feature.

CONCRETE VERIFICATION
Demonstrate how an untracked fixture or dirty production edit changes the code path under review; treat its presence as context, not permission to remove it.

TARGETED VALIDATION
Record safe Git inspection results and a content fingerprint without exposing credentials or changing branches.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0002 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0001 as fully completed only if all its required work is complete, identify any incomplete work, and name 0002 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0002: Documented implementation versus declared product intent

```text
KNITTOOLS CODE REVIEW PROMPT 0002
Title: Documented implementation versus declared product intent
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect a concrete mismatch between PROJECT.md and a reachable implementation, if one exists. Trace callers, configuration and applicable product instructions to distinguish an obsolete description from an actual behavior regression.

CONCRETE VERIFICATION
Require an observable contradiction and an authoritative intended behavior before recommending a correction; uncertainty alone must leave behavior intact.

TARGETED VALIDATION
Record the competing source locations and the smallest available evidence that resolves the discrepancy; leave PROJECT.md unchanged.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0003 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0002 as fully completed only if all its required work is complete, identify any incomplete work, and name 0003 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0003: Production reachability of apparently abandoned code

```text
KNITTOOLS CODE REVIEW PROMPT 0003
Title: Production reachability of apparently abandoned code
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Determine whether an apparently unused implementation is referenced by source sets, reflection, serialization, resource lookup, generated code or tests before classifying it. Pay particular attention to retained compatibility readers and isolated test controls.

CONCRETE VERIFICATION
Compare a release-reachable path with a deliberately retained or debug-only path; absence from a direct call search must not prove dead code.

TARGETED VALIDATION
Inspect relevant build wiring and caller/resource references; do not delete code merely to improve an unused-code count.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0004 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0003 as fully completed only if all its required work is complete, identify any incomplete work, and name 0004 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0004: Application scope versus sibling project scope

```text
KNITTOOLS CODE REVIEW PROMPT 0004
Title: Application scope versus sibling project scope
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Verify that a proposed fix belongs to this KnitTools repository and Android application, not the separate website, voice prototype or another app. Resolve package, Gradle modules and retained functions directory from actual files.

CONCRETE VERIFICATION
A similarly named symbol outside the repository or in a disconnected prototype must not create a requirement for this app.

TARGETED VALIDATION
Inspect root/module identity and the intended path before editing; report out-of-scope dependencies without modifying them.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0005 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0004 as fully completed only if all its required work is complete, identify any incomplete work, and name 0005 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0005: Generated sources versus their owning inputs

```text
KNITTOOLS CODE REVIEW PROMPT 0005
Title: Generated sources versus their owning inputs
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
For an apparent defect in generated bindings, resources, schemas or reports, locate the canonical generator input and the actual regeneration path. Distinguish committed schema contracts from disposable build output.

CONCRETE VERIFICATION
Verify whether editing a generated file would be overwritten by the next build or would conceal a defective source declaration.

TARGETED VALIDATION
Validate the smallest source change through its owning generation/check task when available, and inspect resulting file changes individually.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0006 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0005 as fully completed only if all its required work is complete, identify any incomplete work, and name 0006 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0006: Ignored local configuration versus distributable source

```text
KNITTOOLS CODE REVIEW PROMPT 0006
Title: Ignored local configuration versus distributable source
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Identify how ignored signing, Firebase, service or local build configuration affects the reviewed behavior without opening or copying secret values. Confirm whether the production source depends on a local-only assumption.

CONCRETE VERIFICATION
Compare a clean configured checkout with the current local environment; missing credentials must be reported as an environment limitation rather than fabricated code failure.

TARGETED VALIDATION
Inspect configuration names, ignore rules and fail-closed checks; do not provision services or insert placeholder credentials.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0007 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0006 as fully completed only if all its required work is complete, identify any incomplete work, and name 0007 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0007: User work attribution throughout a sequential audit

```text
KNITTOOLS CODE REVIEW PROMPT 0007
Title: User work attribution throughout a sequential audit
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Establish the initial diff and untracked-file baseline, then attribute later edits to specific reviewed problems without staging or concealing user work. Check whether prior user edits overlap the narrow fix location.

CONCRETE VERIFICATION
Distinguish a pre-existing change from a correction made during this audit, including two edits in the same file.

TARGETED VALIDATION
Compare task-owned hunks with the preserved baseline and report ambiguity rather than reverting either side.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0008 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0007 as fully completed only if all its required work is complete, identify any incomplete work, and name 0008 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0008: Branch workflow conflicts during safe local review

```text
KNITTOOLS CODE REVIEW PROMPT 0008
Title: Branch workflow conflicts during safe local review
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect applicable branch restrictions against the current branch before any correction. Reconcile them with this task's prohibition on unrequested branch changes, commits and pushes instead of silently creating a branch.

CONCRETE VERIFICATION
If the current branch forbids edits, identify the exact instruction and preserve read-only review progress; do not bypass the rule.

TARGETED VALIDATION
Record blocked correction scope and continue independent safe inspections without changing Git state.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0009 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0008 as fully completed only if all its required work is complete, identify any incomplete work, and name 0009 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0009: Documented absence versus feature regression

```text
KNITTOOLS CODE REVIEW PROMPT 0009
Title: Documented absence versus feature regression
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Check whether a reported missing capability is intentionally absent, retained only as compatibility data, or a future plan. Resolve current callers before treating voice, automatic cloud sync, native Ravelry OAuth or a shaping forecast as a defect.

CONCRETE VERIFICATION
Show a current authorized requirement and reachable broken flow before proposing implementation; a roadmap reference is insufficient.

TARGETED VALIDATION
Record verified absence or current support boundary with source evidence and make no speculative feature addition.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0010 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0009 as fully completed only if all its required work is complete, identify any incomplete work, and name 0010 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0010: Local review artifacts and production packaging

```text
KNITTOOLS CODE REVIEW PROMPT 0010
Title: Local review artifacts and production packaging
Review area: Repository evidence and review boundaries

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect whether diagnostic output, fixtures or audit artifacts created by the current workflow can accidentally enter production assets, resources or source sets. Follow actual include rules rather than filename assumptions.

CONCRETE VERIFICATION
A temporary input under an included assets directory must be evaluated differently from an ignored report outside packaging roots.

TARGETED VALIDATION
Inspect packaging/source-set configuration and any affected archive listing locally; remove only task-owned artifacts when justified.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Scope and verified source snapshot
- Workflow and source authority
- Current status, limitations, and document authority

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0011 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0010 as fully completed only if all its required work is complete, identify any incomplete work, and name 0011 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


### Onboarding draft and project handoff

#### Prompt 0011: First-launch eligibility uses loaded unfiltered project facts

```text
KNITTOOLS CODE REVIEW PROMPT 0011
Title: First-launch eligibility uses loaded unfiltered project facts
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace startup eligibility from actual project-loading results through OnboardingViewModel.loadDraft and draft initialization. Determine whether completed projects and projects outside the current folder are included before deciding the user is new. Distinguish an uninitialized empty flow from a confirmed empty repository, and inspect existing dismissal and stored-draft precedence.

CONCRETE VERIFICATION
Compare a genuinely empty installation with one containing only a completed project or a project hidden by a folder filter; delayed loading must not create a spurious journey.

TARGETED VALIDATION
Use focused onboarding initialization tests or controlled fake-flow emissions; record any startup runtime behavior that source inspection cannot establish.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0012 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0011 as fully completed only if all its required work is complete, identify any incomplete work, and name 0012 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0012: Existing draft wins over a second onboarding initialization

```text
KNITTOOLS CODE REVIEW PROMPT 0012
Title: Existing draft wins over a second onboarding initialization
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow draft loading, initialization and persistence when two callers request onboarding state before its first read completes. Check how the existing draft ID and explicit choices are retained rather than replaced by new defaults. Identify the actual serialization or idempotence mechanism and establish whether duplicate initialization is reachable.

CONCRETE VERIFICATION
Arrange overlapping load requests around a delayed DataStore read containing a partially completed draft, and compare resulting draft identity and selected step.

TARGETED VALIDATION
Inspect OnboardingViewModelTest and repository initialization tests; run the smallest existing host test capable of controlling the read boundary.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0013 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0012 as fully completed only if all its required work is complete, identify any incomplete work, and name 0013 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0013: Craft choice and Both default remain distinct from project edits

```text
KNITTOOLS CODE REVIEW PROMPT 0013
Title: Craft choice and Both default remain distinct from project edits
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the Craft step into persisted explicit-choice state, craft-aware wording and the later project form. Verify the documented Both choice defaults new project creation to knitting without preventing an explicit form change. Inspect restoration and automatic defaults so replaying a load cannot overwrite a deliberately selected project craft.

CONCRETE VERIFICATION
Select Both, change the project form to crochet, restore the draft, and verify the eventual creation request carries the intentional form choice.

TARGETED VALIDATION
Use draft and ViewModel tests with captured creation arguments; verify pure craft mapping without requiring any device interaction.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0014 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0013 as fully completed only if all its required work is complete, identify any incomplete work, and name 0014 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0014: Ordered Needs choices survive removal and restoration

```text
KNITTOOLS CODE REVIEW PROMPT 0014
Title: Ordered Needs choices survive removal and restoration
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect the representation and mutation of onboarding Needs selections from user selection through persisted draft and recommendation generation. Verify selection order is preserved, duplicate choices cannot accumulate, and removal does not reorder remaining choices unexpectedly. Check restoration of valid saved choices before inferring a serialization or collection-order defect.

CONCRETE VERIFICATION
Select yarn, counting and projects, remove counting, restore the draft, then reselect counting and inspect the resulting recommendation order.

TARGETED VALIDATION
Run focused OnboardingDraftTest or OnboardingToolboxTest cases that assert ordered values rather than merely set membership.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0015 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0014 as fully completed only if all its required work is complete, identify any incomplete work, and name 0015 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0015: Needs recommendations do not become feature admission rules

```text
KNITTOOLS CODE REVIEW PROMPT 0015
Title: Needs recommendations do not become feature admission rules
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace each Needs answer into toolbox highlighting and subsequent feature access consumers. Establish that personalization changes recommendation order or wording while ordinary feature admission still uses its real owners. Look for a reachable path where unselected needs suppress an available tool or a highlighted tool bypasses an existing gate.

CONCRETE VERIFICATION
Compare drafts selecting no counting-related need and selecting Counting plus Returning; both must retain the same underlying counter availability under equal access facts.

TARGETED VALIDATION
Use source call-site tracing and focused toolbox policy tests; only add an assertion if a real access or mapping defect is confirmed.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0016 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0015 as fully completed only if all its required work is complete, identify any incomplete work, and name 0016 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0016: Demo counter stays isolated from real project state

```text
KNITTOOLS CODE REVIEW PROMPT 0016
Title: Demo counter stays isolated from real project state
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow onboarding demo increment, decrement and Undo from callbacks to draft persistence and any shared counter helpers. Verify these interactions never invoke project count/history/session mutation APIs. Check that a later project form starts from its own raw starting-count field, so a visible demo total is not silently copied into creation.

CONCRETE VERIFICATION
Exercise several demo changes and Undo, then create a project without entering a starting count; inspect repository calls and the created count.

TARGETED VALIDATION
Use captured repository interactions in onboarding tests and pure draft calculations; absence of writes must be established across all demo actions.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0017 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0016 as fully completed only if all its required work is complete, identify any incomplete work, and name 0017 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0017: Demo pattern row handles zero and signed inputs consistently

```text
KNITTOOLS CODE REVIEW PROMPT 0017
Title: Demo pattern row handles zero and signed inputs consistently
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Locate demoPatternRow and trace every caller that supplies the demo count and indexes the three-row pattern. Confirm the zero-based floor-modulo convention is preserved before indexing and that draft normalization does not introduce a different mapping. Treat negative restored inputs according to current validation rather than inventing a negative-count feature.

CONCRETE VERIFICATION
Check counts zero, one, two and three, plus any malformed negative input accepted by the helper, against the three valid pattern indices.

TARGETED VALIDATION
Run a small pure mapping test at the helper boundary and inspect its consumers for an additional off-by-one conversion.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0018 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0017 as fully completed only if all its required work is complete, identify any incomplete work, and name 0018 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0018: Demo return example counts successful positive changes

```text
KNITTOOLS CODE REVIEW PROMPT 0018
Title: Demo return example counts successful positive changes
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the demo increment tally and return-card eligibility through bounds checks, persistence and Undo. Verify only successful positive changes increase the tally and that Undo does not lower an already-earned eligibility fact. Determine whether a failed draft write or an increment at the ceiling can incorrectly count as success.

CONCRETE VERIFICATION
Compare two successful increments with one increment followed by a no-op at the ceiling; Undo after two successes must retain the established return example.

TARGETED VALIDATION
Use focused draft/ViewModel tests with a failing persistence fake and explicit tally assertions rather than inferring success from the displayed number.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0019 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0018 as fully completed only if all its required work is complete, identify any incomplete work, and name 0019 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0019: Demo history keeps its bounded reversible tail

```text
KNITTOOLS CODE REVIEW PROMPT 0019
Title: Demo history keeps its bounded reversible tail
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect how onboarding demo history is appended, trimmed, restored and consumed by Undo. Verify the documented last-100-state bound retains the newest reversible states in the correct order and excludes no-op changes. Check whether restoration of older or oversized draft data passes through the same normalization before use.

CONCRETE VERIFICATION
Apply more than 100 successful changes, inspect the retained tail, and Undo across its beginning; no discarded older state should reappear.

TARGETED VALIDATION
Use pure OnboardingDraftTest scenarios with boundary lengths 99, 100 and 101 and assert both contents and order.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0020 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0019 as fully completed only if all its required work is complete, identify any incomplete work, and name 0020 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0020: Onboarding Back and Skip respect committed creation

```text
KNITTOOLS CODE REVIEW PROMPT 0020
Title: Onboarding Back and Skip respect committed creation
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace step transition rules for Back, Skip and continuation, including restored createdProjectId and receipt facts. Verify Back is confined to the precreation steps that currently allow it and Ready cannot return to a form that would submit another project. Inspect invalid restored step handling before proposing additional transitions.

CONCRETE VERIFICATION
Compare Back from Craft, Project and Ready, then Skip before creation and after a receipt exists; record the intended resulting step and creation calls.

TARGETED VALIDATION
Use pure transition or onboarding ViewModel tests, including restored state; leave visual navigation validation pending unless already safely host-testable.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0021 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0020 as fully completed only if all its required work is complete, identify any incomplete work, and name 0021 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0021: Starting count in onboarding uses the shared creation parser

```text
KNITTOOLS CODE REVIEW PROMPT 0021
Title: Starting count in onboarding uses the shared creation parser
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow raw starting-count text from the onboarding form through validation and the createOnboardingProject arguments. Confirm empty text means zero and accepted values use the same current boundary as ordinary creation. Inspect whether draft restoration, optional-field disclosure or a creation retry bypasses the parser or substitutes the demo count.

CONCRETE VERIFICATION
Compare empty, zero, 99,999, 100,000 and a restored invalid raw value; only valid explicit values should reach the creation operation.

TARGETED VALIDATION
Use ProjectStartValuesTest and focused onboarding argument assertions; do not duplicate shared parser tests unless this integration is the failing path.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0022 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0021 as fully completed only if all its required work is complete, identify any incomplete work, and name 0022 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0022: Onboarding target remains optional and independent of starting count

```text
KNITTOOLS CODE REVIEW PROMPT 0022
Title: Onboarding target remains optional and independent of starting count
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace optional target text, validation and persistence through the onboarding project transaction. Verify blank means no target, positive accepted values are preserved and a starting count does not implicitly create a target. Check current behavior for starting counts already above a target without assuming the target is a counting cap.

CONCRETE VERIFICATION
Create equivalent drafts with no target, a positive target below the starting count and invalid zero target, then inspect accepted repository arguments.

TARGETED VALIDATION
Use the existing shared start-value tests plus one focused onboarding integration assertion where necessary to expose a wiring error.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0023 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0022 as fully completed only if all its required work is complete, identify any incomplete work, and name 0023 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0023: Onboarding submission is idempotent across the receipt boundary

```text
KNITTOOLS CODE REVIEW PROMPT 0023
Title: Onboarding submission is idempotent across the receipt boundary
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace creation from form submission to the Room draft receipt and subsequent DataStore completion. Verify repeated submission with the same draft ID resolves the committed project rather than inserts another one. Identify how concurrent submissions and commit-before-draft-save failure are distinguished from a genuinely failed project transaction.

CONCRETE VERIFICATION
Simulate commit succeeding while the draft update fails, reload the same draft and submit again; establish the number and identity of created projects.

TARGETED VALIDATION
Inspect OnboardingProjectRoomTest source and run existing host-testable receipt-policy tests; report real transaction execution separately if unavailable.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0024 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0023 as fully completed only if all its required work is complete, identify any incomplete work, and name 0024 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0024: Project attachment failure leaves onboarding creation uncommitted

```text
KNITTOOLS CODE REVIEW PROMPT 0024
Title: Project attachment failure leaves onboarding creation uncommitted
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow the optional-document branch inside createOnboardingProject through project insertion, attachment and draft-receipt writing. Verify the three durable outcomes share the documented transaction and errors cannot leave a receipt pointing to a partial project. Limit this check to creation coordination rather than duplicating PDF parsing or file-security review.

CONCRETE VERIFICATION
Inject an attachment failure after project insertion and compare the project, attachment and receipt observations before retrying the same draft.

TARGETED VALIDATION
Use existing repository transaction tests or inspect the real transaction runner and corresponding Room test; do not claim fake rollback proves SQLite behavior.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0025 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0024 as fully completed only if all its required work is complete, identify any incomplete work, and name 0025 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0025: Receipt recovery preserves Ready versus Price semantics

```text
KNITTOOLS CODE REVIEW PROMPT 0025
Title: Receipt recovery preserves Ready versus Price semantics
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect loadDraft reconciliation when a committed onboarding receipt exists but DataStore contains an unfinished journey. Verify an interrupted Ready resumes Ready while other applicable unfinished steps resume Price, using actual receipt identity. Determine how missing or deleted receipt targets are handled without inventing a new restoration policy.

CONCRETE VERIFICATION
Restore one draft interrupted on Ready and one interrupted immediately after commit but before advancing; compare their recovered steps and absence of duplicate creation.

TARGETED VALIDATION
Use OnboardingViewModelTest fixtures for receipt lookup and restored draft states; report ambiguous deleted-target behavior for clarification instead of redesigning it.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0026 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0025 as fully completed only if all its required work is complete, identify any incomplete work, and name 0026 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0026: A separately created project retires an obsolete onboarding draft

```text
KNITTOOLS CODE REVIEW PROMPT 0026
Title: A separately created project retires an obsolete onboarding draft
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the reconciliation between an unfinished draft, its optional receipt and current unfiltered project existence. Verify a project created through another legitimate entry point prevents stale onboarding from creating an unintended duplicate. Check ordering so the draft is not retired merely because its own already-created project is observed.

CONCRETE VERIFICATION
Pause before onboarding creation, create a different project through the normal repository path, then reload; compare with a draft owning its own receipt.

TARGETED VALIDATION
Run focused initialization/reconciliation tests with explicit project identities and distinguish observed project existence from default empty state.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0027 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0026 as fully completed only if all its required work is complete, identify any incomplete work, and name 0027 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0027: Project creation success survives craft-preference failure

```text
KNITTOOLS CODE REVIEW PROMPT 0027
Title: Project creation success survives craft-preference failure
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow the post-commit attempt to remember the last craft and its result propagation to onboarding step state. Verify an ancillary preference-write failure does not report the committed project as uncreated or encourage duplicate submission. Identify which durable receipt remains authoritative and what draft recovery does after that failure.

CONCRETE VERIFICATION
Make project creation succeed and the subsequent craft preference write throw, then reload and inspect project identity and next-step behavior.

TARGETED VALIDATION
Use a failing preference fake in existing onboarding tests and assert committed creation facts independently from the ancillary error path.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0028 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0027 as fully completed only if all its required work is complete, identify any incomplete work, and name 0028 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0028: Created-project summary derives facts from the committed result

```text
KNITTOOLS CODE REVIEW PROMPT 0028
Title: Created-project summary derives facts from the committed result
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect how Ready obtains the created project name, next count and optional document name. Trace whether these facts come from the committed creation result or a stale editable draft, and check deletion or delayed loading behavior. The summary must not itself issue another creation or counting action.

CONCRETE VERIFICATION
Create with a nonzero starting count and a repository-normalized name, then compare the Ready facts with the committed result after draft restoration.

TARGETED VALIDATION
Use focused ViewModel or pure summary-model tests with normalized creation results; inspect rendering bindings without requiring an emulator.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0029 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0028 as fully completed only if all its required work is complete, identify any incomplete work, and name 0029 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0029: Pending onboarding exit is acknowledged without recreating content

```text
KNITTOOLS CODE REVIEW PROMPT 0029
Title: Pending onboarding exit is acknowledged without recreating content
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace exitPending, createdProjectId and acknowledgement from successful onboarding exit through interruption and reload. Verify pending exit represents reopening an existing project and clears only at the owning acknowledgement boundary. Inspect stale project identity handling so another selected project cannot be substituted silently during the delayed handoff.

CONCRETE VERIFICATION
Interrupt after exitPending is persisted but before acknowledgement, restore, and observe one reopen request for the existing project with zero creation calls.

TARGETED VALIDATION
Use onboarding state tests and captured handoff requests; separate route execution evidence from the durable state-machine assertions.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0030 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0029 as fully completed only if all its required work is complete, identify any incomplete work, and name 0030 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0030: Price mode uses loaded offer and access facts

```text
KNITTOOLS CODE REVIEW PROMPT 0030
Title: Price mode uses loaded offer and access facts
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect OnboardingPriceMode inputs and their use by the last onboarding step. Verify Loading, Trial, Purchase, Continue and Skip are selected from actual readiness and availability facts, without treating default false values as loaded results. Focus on the onboarding state decision, leaving purchase verification to its separate owner.

CONCRETE VERIFICATION
Compare initial loading, unavailable sales and confirmed purchased facts; each settled unavailable condition should retain the documented continuation path.

TARGETED VALIDATION
Run OnboardingPricePolicyTest against the current decision inputs and inspect screen action bindings for the returned mode.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0031 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0030 as fully completed only if all its required work is complete, identify any incomplete work, and name 0031 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0031: Free continuation is not held hostage by onboarding sales failure

```text
KNITTOOLS CODE REVIEW PROMPT 0031
Title: Free continuation is not held hostage by onboarding sales failure
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace continuation out of Price when product information, purchase availability or trial starting fails. Verify the journey still exposes its documented free/skip path and does not leave an exit permanently busy. Distinguish a legitimate in-flight action from a stale loading flag retained after its result or cancellation.

CONCRETE VERIFICATION
Complete project creation, fail an available sales lookup or trial operation, and verify a subsequent permitted free continuation can finish the draft.

TARGETED VALIDATION
Use focused onboarding ViewModel tests with deterministic failures; do not contact Play or change the entitlement implementation.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0032 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0031 as fully completed only if all its required work is complete, identify any incomplete work, and name 0032 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0032: Trial introduction is queued only for a newly started trial

```text
KNITTOOLS CODE REVIEW PROMPT 0032
Title: Trial introduction is queued only for a newly started trial
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace onboarding startTrial results into introductory journey queueing, origin recording and draft completion. Verify Started queues exactly the documented introduction before completion, while AlreadyActive does not replay it. Inspect failure propagation and persistence ordering without assuming a failed result means the underlying access state changed.

CONCRETE VERIFICATION
Feed Started, AlreadyActive and an expired or failed outcome through the onboarding owner; compare queue counts, recorded origin and whether the draft completes.

TARGETED VALIDATION
Use captured coordinator calls in onboarding tests and existing typed-result fixtures; avoid live trial or purchase operations.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0033 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0032 as fully completed only if all its required work is complete, identify any incomplete work, and name 0033 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0033: Confirmed purchase finishes onboarding without stale project recreation

```text
KNITTOOLS CODE REVIEW PROMPT 0033
Title: Confirmed purchase finishes onboarding without stale project recreation
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow changes in confirmed purchased status while Price or a pending exit is active. Verify the onboarding owner uses the committed project receipt and existing completion path, including Pro-active feedback, rather than repeating form submission. Inspect repeated status emissions for duplicate completion or exit effects.

CONCRETE VERIFICATION
Emit purchased status twice after a project has committed and while completion persistence is delayed; inspect draft identity and the number of exit requests.

TARGETED VALIDATION
Use fake entitlement emissions in onboarding tests and source-trace the completion boundary; no live billing validation is required.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0034 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0033 as fully completed only if all its required work is complete, identify any incomplete work, and name 0034 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0034: Draft restoration retains raw fields without restoring an in-flight job

```text
KNITTOOLS CODE REVIEW PROMPT 0034
Title: Draft restoration retains raw fields without restoring an in-flight job
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect OnboardingDraft serialization and ViewModel reconstruction for raw form text, options visibility, demo history and created identity. Distinguish persistable facts from transient busy/error or coroutine state. Verify a restored interrupted operation can be reconciled through its durable result rather than remaining indefinitely busy or restarting a committed action.

CONCRETE VERIFICATION
Restore drafts containing invalid partial text and a committed creation receipt, separately; check that editable text survives while receipt reconciliation owns completed work.

TARGETED VALIDATION
Use draft serialization and ViewModel restoration tests; report any genuine Android saved-state evidence gap separately from those host assertions.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0035 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0034 as fully completed only if all its required work is complete, identify any incomplete work, and name 0035 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0035: Onboarding project context cannot absorb an external request

```text
KNITTOOLS CODE REVIEW PROMPT 0035
Title: Onboarding project context cannot absorb an external request
Review area: Onboarding draft and project handoff

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the handoff between onboarding eligibility and already-pending PDF, share or counter requests at startup. Verify the journey's demo and draft operations cannot consume a request owned by the external-entry coordinator. Restrict the check to onboarding suppression and resumption boundaries rather than reauditing all intent parsing or navigation.

CONCRETE VERIFICATION
Load a first-launch draft while a counter request or accepted pending PDF is present; verify each request retains its identity and intended owner.

TARGETED VALIDATION
Inspect existing onboarding navigation tests and request-priority source, running only appropriate host tests that isolate this decision.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Eight-step onboarding journey
- app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt
- app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0036 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0035 as fully completed only if all its required work is complete, identify any incomplete work, and name 0036 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


### Starter setup, contextual guidance and guide actions

#### Prompt 0036: Compact start-state example never creates user content

```text
KNITTOOLS CODE REVIEW PROMPT 0036
Title: Compact start-state example never creates user content
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the ProjectsStartState example callbacks into their actual state owners and dependencies. Confirm local demonstration changes cannot reach project creation, counter history or session writes. Check empty-state eligibility against loaded unfiltered facts, because a filtered empty list must not turn an established project collection into a new-user example.

CONCRETE VERIFICATION
Compare a truly empty database with a folder containing no visible projects, then exercise every available example counter action.

TARGETED VALIDATION
Inspect start-state ViewModel tests and captured repository interactions; use existing host tests for eligibility and write isolation.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0037 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0036 as fully completed only if all its required work is complete, identify any incomplete work, and name 0037 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0037: Starter identity is assigned only on the first real creation

```text
KNITTOOLS CODE REVIEW PROMPT 0037
Title: Starter identity is assigned only on the first real creation
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow ordinary creation success into starter-project registration and setup eligibility. Verify the decision uses database emptiness before insertion, including completed projects, and does not designate every subsequently visible project as a starter. Identify how a failed guidance preference write affects an otherwise committed project without redefining creation success.

CONCRETE VERIFICATION
Create a project with no prior projects, then create after only completed projects remain; compare starter identity and committed creation results.

TARGETED VALIDATION
Use existing creation and onboarding-state tests with explicit pre-insert facts; report ancillary persistence failures separately from project transactions.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0038 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0037 as fully completed only if all its required work is complete, identify any incomplete work, and name 0038 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0038: Setup completion derives Target from an actual target

```text
KNITTOOLS CODE REVIEW PROMPT 0038
Title: Setup completion derives Target from an actual target
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace ProjectSetupProgress from project fields to the Target suggestion and its available action. Verify a positive current count, a remembered form draft or generic progress text cannot satisfy the target fact. Inspect clearing an existing target so the suggestion reflects the current committed project rather than a stale success signal.

CONCRETE VERIFICATION
Compare count 20 without a target, count zero with a valid target and a project whose target was subsequently cleared.

TARGETED VALIDATION
Run focused ProjectSetupProgressTest cases and inspect the owning state mapping; no navigation or device validation is needed.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0039 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0038 as fully completed only if all its required work is complete, identify any incomplete work, and name 0039 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0039: Setup Pattern recognizes only attached or compatible content

```text
KNITTOOLS CODE REVIEW PROMPT 0039
Title: Setup Pattern recognizes only attached or compatible content
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect the Pattern completion predicate and the flows that supply document and web-pattern facts. Verify an attached project document or currently compatible web pattern satisfies it, while unrelated Library content and pending imports do not. Resolve the actual compatibility rule from current code rather than treating all metadata as a readable document.

CONCRETE VERIFICATION
Compare an unrelated saved pattern, a pending attachment and a committed compatible project pattern; observe when the suggestion changes state.

TARGETED VALIDATION
Use pure setup-progress tests and focused owner-state fixtures; keep PDF validation and import mechanics outside this review.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0040 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0039 as fully completed only if all its required work is complete, identify any incomplete work, and name 0040 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0040: Setup Yarn reflects current project relationships

```text
KNITTOOLS CODE REVIEW PROMPT 0040
Title: Setup Yarn reflects current project relationships
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the Yarn setup fact from linked yarn and project yarn notes to ProjectSetupProgress. Check that unrelated stash entries and transient editor drafts cannot satisfy it, while either documented committed project association can. Follow removal or unlinking through observed state so completion does not remain latched without a current supporting fact.

CONCRETE VERIFICATION
Compare an unlinked stash card, a project yarn note and a removed last project association; inspect the resulting setup suggestion.

TARGETED VALIDATION
Run focused setup-progress tests using real model inputs; inspect relationship readers without duplicating yarn mutation review.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0041 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0040 as fully completed only if all its required work is complete, identify any incomplete work, and name 0041 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0041: Setup Work Time requires a saved session

```text
KNITTOOLS CODE REVIEW PROMPT 0041
Title: Setup Work Time requires a saved session
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect how Work Time completion uses session existence rather than navigation, count activity, a running timer or a pending stop review. Trace successful session persistence to setup state refresh. Determine whether deletion of the only saved session affects current facts according to the implemented setup contract rather than a presumed permanent achievement.

CONCRETE VERIFICATION
Open Counter, start an active session, then save it; the setup state should change only at its documented durable boundary.

TARGETED VALIDATION
Use setup-progress and CounterProjectSetupTest source with controlled session facts; do not start or stop actual user sessions.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0042 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0041 as fully completed only if all its required work is complete, identify any incomplete work, and name 0042 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0042: Setup suggestions order incomplete actions before completed facts

```text
KNITTOOLS CODE REVIEW PROMPT 0042
Title: Setup suggestions order incomplete actions before completed facts
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace ProjectSetupProgress ordering into ProjectSetupList and action availability. Verify completed suggestions remain informational and incomplete suggestions retain their own action identity after reordering. Check that index-based callbacks or stale remembered values cannot invoke the wrong editor when one fact completes asynchronously during the current overview visit.

CONCRETE VERIFICATION
Complete Pattern while Target remains incomplete, then inspect the action assigned to each reordered suggestion and the completed row.

TARGETED VALIDATION
Use pure ordering tests and source-level callback identity inspection; extend a focused host assertion only for a confirmed mismatch.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0043 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0042 as fully completed only if all its required work is complete, identify any incomplete work, and name 0043 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0043: Setup auto-hide waits for the next real visit

```text
KNITTOOLS CODE REVIEW PROMPT 0043
Title: Setup auto-hide waits for the next real visit
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect ProjectSetupVisit project identity and routeEntryGeneration through all-complete observation and persisted hiding. Verify completing the fourth suggestion leaves the current visit intact, while returning through a reused entry is recognized as a later visit. Distinguish recreation from a new visit before attributing disappearance to lifecycle behavior.

CONCRETE VERIFICATION
Complete the final setup fact, recreate the current owner, then simulate a genuine navigation return; compare section visibility at each boundary.

TARGETED VALIDATION
Use CounterProjectSetupTest and ProjectSetupNavigationTest logic that models visit generations; leave unsupported runtime recreation checks explicitly pending.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0044 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0043 as fully completed only if all its required work is complete, identify any incomplete work, and name 0044 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0044: Manual setup hiding persists before it is claimed successful

```text
KNITTOOLS CODE REVIEW PROMPT 0044
Title: Manual setup hiding persists before it is claimed successful
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the Hide action through CounterViewModel and onboarding-state persistence. Check how success, ordinary failure and cancellation update local setup visibility without erasing the wrong project's preference. Identify whether reloading or returning can reveal that an apparent successful hide was never stored, and preserve the current documented error behavior.

CONCRETE VERIFICATION
Hide setup with a successful write and with a failed write, then reload the same project and switch to another starter context.

TARGETED VALIDATION
Use existing state-repository and setup ViewModel tests with controlled writes; assert project identity and persisted visibility separately.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0045 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0044 as fully completed only if all its required work is complete, identify any incomplete work, and name 0045 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0045: Explicit Work Time guidance preserves manual-request exceptions

```text
KNITTOOLS CODE REVIEW PROMPT 0045
Title: Explicit Work Time guidance preserves manual-request exceptions
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the setup Work Time action into the WORK_SESSION tip request and its consumption by TipSession and TipHost. Verify manual requests bypass only documented automatic timing, seen-state and process-budget restrictions. Loading, lifecycle, blockers and placement must still be evaluated through the real owner before presentation is considered successful.

CONCRETE VERIFICATION
Request Work Time after exhausting automatic tips, first under an active blocker and then after it clears; inspect presentation and budget changes.

TARGETED VALIDATION
Use existing manual-tip policy and setup owner tests with controlled readiness; avoid repeating historical device configuration checks.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0046 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0045 as fully completed only if all its required work is complete, identify any incomplete work, and name 0046 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0046: Automatic tip budget is claimed on actual presentation

```text
KNITTOOLS CODE REVIEW PROMPT 0046
Title: Automatic tip budget is claimed on actual presentation
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect TipSession arbitration from candidate eligibility through visible placement, dismissal and persisted seen state. Verify the two-tip process budget is not consumed merely by selecting a candidate that never renders. Trace canceled, blocked and replaced candidates to their actual claim/release mechanism before reporting a lost-tip or duplicate-tip issue.

CONCRETE VERIFICATION
Queue an eligible tip, block its placement, replace the route and later offer another tip; compare visible presentations with consumed budget.

TARGETED VALIDATION
Run focused TipSessionTest and host-capable TipHostTest cases; distinguish policy assertions from native overlay geometry evidence.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0047 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0046 as fully completed only if all its required work is complete, identify any incomplete work, and name 0047 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0047: First-row Undo chaining does not become an unlimited tip bypass

```text
KNITTOOLS CODE REVIEW PROMPT 0047
Title: First-row Undo chaining does not become an unlimited tip bypass
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace the FIRST_ROW committed signal through the special UNDO follow-up and shared process budget. Verify the exception applies only to the intended chain and cannot be replayed by stale events, repeated route attachment or an uncommitted increment. Inspect when chain eligibility is consumed and how ordinary tips remain bounded.

CONCRETE VERIFICATION
Present the first-row tip, trigger its Undo follow-up, then replay the originating signal and offer another normal automatic tip.

TARGETED VALIDATION
Use TipSessionTest and committed-counter signal tests with explicit event sequences; assert presentation identity and budget after each transition.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0048 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0047 as fully completed only if all its required work is complete, identify any incomplete work, and name 0048 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0048: Step-size guidance counts committed increments only

```text
KNITTOOLS CODE REVIEW PROMPT 0048
Title: Step-size guidance counts committed increments only
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Follow projectIncrementCounts from successful repository results through persisted capped tally and TipRules. Verify starting counts, failed optimistic taps, decrements and Undo do not inflate the thirty-increment trigger. Inspect concurrent result handling to determine whether one committed increment can be counted twice or attributed to a newly selected project.

CONCRETE VERIFICATION
Begin at a positive initial count, perform successful and failed increments, then switch projects before a delayed result returns; inspect both tallies.

TARGETED VALIDATION
Run focused onboarding-state and counter-signal tests with controlled repository outcomes; verify the cap without requiring repeated UI interaction.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0049 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0048 as fully completed only if all its required work is complete, identify any incomplete work, and name 0049 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0049: Rapid-tap step guidance uses the intended recent-time window

```text
KNITTOOLS CODE REVIEW PROMPT 0049
Title: Rapid-tap step guidance uses the intended recent-time window
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Inspect the three-recent-increments path in TipRules and the timestamps recorded by its owner. Verify the two-second window uses the intended time source and successful increment identity, and old samples expire without crossing project boundaries. Check its interaction with the saved step so guidance for step one is not shown after a different step commits.

CONCRETE VERIFICATION
Compare three increments just inside and outside the window, then save step five before eligibility is evaluated for the third increment.

TARGETED VALIDATION
Use fake-clock tip tests and captured committed events; document the actual timing source instead of assuming wall-clock or elapsed time.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0050 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0049 as fully completed only if all its required work is complete, identify any incomplete work, and name 0050 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0050: Work-session automatic tip uses distinct open days and real session facts

```text
KNITTOOLS CODE REVIEW PROMPT 0050
Title: Work-session automatic tip uses distinct open days and real session facts
Review area: Starter setup, contextual guidance and guide actions

CONTEXT AND AUTHORITY
Work only in the actual current KnitTools repository. Before reviewing this topic, read or re-check the repository-root file named exactly PROJECT.md. Use it as important context for purpose, intended behavior, architecture, constraints, supported platforms and deliberate decisions. It may be incomplete or outdated: verify implementation claims against current code, configuration, tests, dependencies, source sets and working-tree state. Read applicable repository instructions and observe required safe local pre-read checks without exposing secrets. If documentation and implementation disagree, investigate which is stale or ambiguous; do not change working behavior on an assumption. Do not modify PROJECT.md unless a later separate task explicitly requests that edit. Source leads in this prompt are investigation hints, not proof that a symbol still exists or a test passed. Resolve renamed owners in the current repository. If a conditional feature or technology is absent, verify that absence and record this topic as not applicable; do not add it.

EVIDENCE AND HALLUCINATION CONTROL
AI-assisted code review can hallucinate issues, misread context, or recommend unnecessary changes. Base every finding on the actual current repository and verify the full relevant code path, configuration, tests, version constraints, and existing safeguards before reporting or changing anything. It is completely acceptable to find no issue and make no change. Do not invent findings, force a quota, treat a preference as a defect, or modify code merely to produce visible work. Do not over-engineer, add speculative abstractions, or refactor code that is already correct, safe, clear, and fit for purpose. Prefer the smallest adequate solution. Less is better.

Trace the relevant caller chain, state origin, every material read/write path, error handling, validation, lifecycle ownership, platform restrictions and version guards. Never judge an isolated line or function. Check whether existing safeguards already prevent the alleged failure. Establish a concrete reachable trigger, expected versus actual behavior and violated real requirement before classifying a confirmed defect. Separate confirmed defects, unverified risks, style preferences and alternative implementations. If repository evidence or safe validation cannot confirm an issue, do not present it as a bug or patch it speculatively. Assess severity realistically. Security, concurrency, performance and leak claims require an actual mechanism and relevant impact, not a generic warning or theoretical optimization. Test source, static assertions and historical receipts are not fresh runtime evidence.

CORRECTION BOUNDARIES
Inspect first. If and only if a real issue is confirmed, implement the smallest safe correction necessary to address its root cause. If no issue is confirmed, make no change. Do not perform opportunistic cleanup, unrelated refactoring, dependency updates, broad rewrites, or speculative improvements.

Leave correct, safe, clear and fit-for-purpose code alone. Preserve intentional behavior, public interfaces, data formats, schema, UI behavior and compatibility unless a change is strictly necessary for the confirmed correction. Do not introduce renaming, file reorganization, helpers, abstractions, layers or frameworks merely as another way to write the code. New dependencies, schema/migration, build-system, architectural or public-API changes require direct, strong technical necessity and a documented reason; a sufficient local correction must not become a rewrite. Fix a shared root cause once at its actual owner. If an earlier correction already addresses this topic, independently verify the current path and record ALREADY ADDRESSED without another change; never skip the inspection. A clean review is a successful outcome with zero modifications.

GIT, DATA AND EXTERNAL-ACTION SAFETY
Before the first edit for this prompt, run git status --short and inspect relevant existing changes; establish or preserve the audit's initial tracked, staged and untracked baseline. Treat all pre-existing changes as user work, including a pre-existing PROJECT.md edit. Do not discard, revert, overwrite, hide or clean that work. Do not use git reset, git restore, git checkout, git clean or git stash to manage it. Do not alter unrelated changes, stage files, commit, push, switch/create branches, rewrite history or create/open/merge pull requests without a separate explicit user request. If a current repository branch rule prevents editing, record the exact restriction and continue safe read-only inspection rather than bypassing it.

Do not deploy, publish, create accounts, change cloud services, rotate keys, alter real production/user data or perform other external or hard-to-reverse actions. Do not trigger live purchases, service/telemetry uploads or credential-dependent live tests. Do not expose secret values. If a step needs credentials, an external service, a manual product decision or safety confirmation, leave that step unperformed and report the exact dependency while completing independent safe repository work. Do not launch an emulator or operate a device without explicit task authorization. In particular, do not repeat the previously completed 320 dp, 200% font, combined or TalkBack configurations without the user's explicit request for those checks. Do not implement intentionally absent/future capabilities as audit fixes.

VALIDATION AND CHANGE REVIEW
After each correction, run the appropriate focused existing tests, lint, type checks, build or other repository check that addresses its mechanism. Add or change tests only when genuinely needed to reproduce the defect, validate the fix or protect a meaningful regression; do not inflate counts or mirror implementation. Choose the smallest adequate verification level and respect actual platform/library versions and environment limits. Do not run a heavy whole-project suite after every no-change review. Use broader appropriate validation at final reconciliation. Do not invoke broad or unreviewed automatic fixes; inspect every changed file and hunk. Distinguish a pre-existing or unrelated check failure from your change and report it instead of fixing it as side work. If a check cannot run, name the exact command/check and reason, separate source-level evidence from unexecuted runtime proof, and never claim full verification. Keep prior verified corrections and their evidence attributable as the sequential audit continues.

SPECIFIC INSPECTION
Trace counter-open day tracking, saved-session presence and active-session presence into WORK_SESSION eligibility. Verify repeated opens on one day do not impersonate two days and an active session suppresses a suggestion to start another. Inspect day normalization and loaded-state dependencies before identifying a date or initialization bug.

CONCRETE VERIFICATION
Compare many opens on one day with opens on two days, then introduce an active or saved session before tip selection.

TARGETED VALIDATION
Use TipRulesTest and onboarding-state day fixtures; keep timezone assumptions explicit and validate only the source-defined day contract.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Starter-project setup and the compact Projects start state
- Contextual tips and committed celebrations
- Offline guide and contextual actions

REQUIRED RESULT RECORD
Record this prompt's number and title, then all of the following:
1. The reviewed area and actual relevant files, symbols and code paths inspected.
2. Whether a real issue was confirmed, with a distinct status for no issue, already addressed, not applicable or unverified/blocked.
3. If no issue was confirmed, explicitly state that no changes were made; if the implementation is correct, safe, sufficiently simple and fit for purpose, say so.
4. For each confirmed finding: exact path/class/function/symbol and applicable lines, evidence and reachable trigger, root cause, concrete impact and realistic severity.
5. Every changed file, the smallest correction made and why each change was necessary.
6. Exact validation commands/checks and actual results, including executed versus cached/skipped cases where material.
7. Pre-existing or unrelated failures, separated from the effects of this correction.
8. Unverified risks, unavailable checks, external/manual dependencies and anything that could not be established.
9. A clear continuation statement naming the next numbered prompt, or the exact stopping boundary if continuation is impossible.

CONTINUATION AND CAPACITY
After inspection, any justified minimal correction, focused validation and the result record, automatically continue to the next numbered prompt available in the supplied document. Do not request approval after an ordinary authorized safe repository correction. If context, time, access or available input prevents continuing, stop at a clear numbered boundary and report the last fully completed prompt, any incomplete prompt, the exact next prompt, changed files, validation results and remaining work. Preserve enough evidence to resume safely. Do not skip prompts, replace unperformed inspections with summaries, invent a missing next prompt or claim the full audit is complete when any numbered prompt remains unprocessed.

NEXT NUMBERED PROMPT
After recording this result, continue automatically to prompt 0051 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0050 as fully completed only if all its required work is complete, identify any incomplete work, and name 0051 as the next prompt. Do not invent its content or claim the whole audit is finished.
```
