# KnitTools Code Review Prompts: Part 02

Prepared on November 10, 2026, Europe/Helsinki, from the supplied PROJECT.md and prompt-writing instructions. These are review instructions, not completed audit findings. Every prompt includes its own complete rules. Execute the numbered prompts in order, preserve the exact continuation boundary if interrupted, and never claim unperformed work is complete.

## Part 02: Prompts 0051 to 0100

This part contains 50 independent prompts. Global numbering continues across parts.

### Starter setup, contextual guidance and guide actions

#### Prompt 0051: Pattern and screen-awake tips evaluate their separate prerequisites

```text
KNITTOOLS CODE REVIEW PROMPT 0051
Title: Pattern and screen-awake tips evaluate their separate prerequisites
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
Inspect Pattern and Keep Screen On eligibility from counter-open counts and current project/preference facts. Verify an existing document or compatible web pattern suppresses the missing-pattern suggestion, while the screen-awake suggestion respects its own four-open threshold and off preference. Follow state refresh so one tip cannot borrow another trigger counter.

CONCRETE VERIFICATION
Compare three opens with no pattern, four opens with an attached pattern and screen-awake enabled, and an onboarding-ready PDF context.

TARGETED VALIDATION
Use focused TipRulesTest inputs with separate prerequisite changes; inspect owner mapping rather than adding broad UI tests.

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
After recording this result, continue automatically to prompt 0052 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0051 as fully completed only if all its required work is complete, identify any incomplete work, and name 0052 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0052: Widget tip requires actual pin capability and activity facts

```text
KNITTOOLS CODE REVIEW PROMPT 0052
Title: Widget tip requires actual pin capability and activity facts
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
Trace widget-tip eligibility from pinning support and saved or pending first-session facts or counter-open days. Verify unsupported pinning cannot be treated as an actionable recommendation and that temporary session review is classified according to its documented pending state. Keep launcher behavior separate from the pure recommendation decision.

CONCRETE VERIFICATION
Compare supported and unsupported pinning with the same three-day activity, then use a first-session pending fact with fewer open days.

TARGETED VALIDATION
Use TipRulesTest and widget-pin host policy source; do not attempt launcher operations or treat a mocked capability as runtime proof.

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
After recording this result, continue automatically to prompt 0053 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0052 as fully completed only if all its required work is complete, identify any incomplete work, and name 0053 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0053: Backup and folder tips use unfiltered project membership

```text
KNITTOOLS CODE REVIEW PROMPT 0053
Title: Backup and folder tips use unfiltered project membership
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
Inspect the source facts used by the backup inline card and folder suggestion. Verify folder filtering cannot reduce the project count or imply no content, and successful-backup state and first-use age remain independent prerequisites. Trace loaded organization snapshots to ensure placeholder membership cannot flash a misleading suggestion.

CONCRETE VERIFICATION
Use four projects hidden by a narrow filter, with backup age just below and at seven days, and compare both guidance decisions.

TARGETED VALIDATION
Run pure tip/state tests with unfiltered membership fixtures and fake dates; leave backup execution outside this focused check.

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
After recording this result, continue automatically to prompt 0054 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0053 as fully completed only if all its required work is complete, identify any incomplete work, and name 0054 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0054: Target celebration uses the committed crossing snapshot

```text
KNITTOOLS CODE REVIEW PROMPT 0054
Title: Target celebration uses the committed crossing snapshot
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
Trace applyMainCounterChangeWithResult through TargetReached eligibility and its project-tagged signal. Verify before, after and target come from one committed operation, including clamped steps and concurrent target edits. Confirm Undo and no-op writes cannot fabricate a crossing merely because the optimistic display or a subsequent read meets the target.

CONCRETE VERIFICATION
Cross a target with a large step, fail an equivalent write, and Undo across the same boundary; compare emitted target signals.

TARGETED VALIDATION
Use CounterRepositoryCelebrationTest and CounterCelebrationSignalsTest with transaction results; do not infer commitment from animated delta feedback.

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
After recording this result, continue automatically to prompt 0055 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0054 as fully completed only if all its required work is complete, identify any incomplete work, and name 0055 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0055: Target celebration identity is recorded after visibility

```text
KNITTOOLS CODE REVIEW PROMPT 0055
Title: Target celebration identity is recorded after visibility
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
Inspect the per-project celebrated-target set and the point at which TipHost records successful presentation. Verify blocked or canceled candidates remain eligible, genuinely shown identities survive resetting ordinary tips, and set bounding preserves the documented chronological policy. Identify the actual atomic persistence and failure behavior before proposing replay suppression.

CONCRETE VERIFICATION
Block a target tip before placement, later show it, reset tips and revisit that project; compare eligibility with an uncelebrated project.

TARGETED VALIDATION
Run targeted TipSession and onboarding-state tests that separate queueing from visibility; inspect any bounded-set eviction contract in current code.

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
After recording this result, continue automatically to prompt 0056 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0055 as fully completed only if all its required work is complete, identify any incomplete work, and name 0056 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0056: First saved-session celebration remains monotonic after history deletion

```text
KNITTOOLS CODE REVIEW PROMPT 0056
Title: First saved-session celebration remains monotonic after history deletion
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
Trace wasFirstCompletedSession classification within the save transaction into first_save_observed and pending guidance. Verify a null saved ID, failed save or replayed result cannot claim a first session, while deleting history or resetting tips cannot recreate the introduction. Inspect concurrency between save results and guidance-state persistence.

CONCRETE VERIFICATION
Save the first session, delete its history row, reset tips and save again; contrast with a first save that failed before commit.

TARGETED VALIDATION
Use repository celebration and onboarding-state tests with explicit saved IDs and monotonic marker assertions; preserve the existing persistence boundary.

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
After recording this result, continue automatically to prompt 0057 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0056 as fully completed only if all its required work is complete, identify any incomplete work, and name 0057 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0057: Single-project completion celebration consumes the exact event once

```text
KNITTOOLS CODE REVIEW PROMPT 0057
Title: Single-project completion celebration consumes the exact event once
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
Inspect the completion-event snapshot captured before selection is cleared and consumed by ProjectCompletedSheet. Verify bulk completion, repeated idempotent completion and stale project selection do not synthesize celebrations. A reopen followed by a new completion may produce a new event, so suppression must use event identity rather than project identity alone.

CONCRETE VERIFICATION
Complete one project, replay its result, then reopen and complete again; compare with a multi-project completion operation.

TARGETED VALIDATION
Use ProjectListCompletionCelebrationTest and CounterCompletionCelebrationTest source or host tests; assert event IDs and actual sheet requests.

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
After recording this result, continue automatically to prompt 0058 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0057 as fully completed only if all its required work is complete, identify any incomplete work, and name 0058 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0058: Tip reset preserves unrelated durable guidance identities

```text
KNITTOOLS CODE REVIEW PROMPT 0058
Title: Tip reset preserves unrelated durable guidance identities
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
Trace Show Tips Again through persistent reset and process-session reset. Verify process budget changes only after persistence succeeds, while target celebration identities, first-save identity and trial activity markers retain their documented monotonic meaning. Inspect failure propagation so Settings or Guide cannot show success for a reset that never committed.

CONCRETE VERIFICATION
Fail the reset write with an exhausted process budget, then retry successfully; inspect budget, target memory and first-save marker after each result.

TARGETED VALIDATION
Use SettingsViewModelTest and OnboardingStateRepositoryTest focused failure cases; compare preserved keys directly in the existing test abstraction.

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
After recording this result, continue automatically to prompt 0059 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0058 as fully completed only if all its required work is complete, identify any incomplete work, and name 0059 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0059: Conflicting tip outside-tap descriptions are resolved from current behavior

```text
KNITTOOLS CODE REVIEW PROMPT 0059
Title: Conflicting tip outside-tap descriptions are resolved from current behavior
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
Compare the guidance chapter with Preferences and Other Persisted State, which describe outside-tap propagation differently. Trace TipHost pointer consumption, overlay bounds and underlying callbacks in current source and tests before deciding the intended behavior. Do not choose either documentation sentence as authority or alter working interaction merely to reconcile prose.

CONCRETE VERIFICATION
Inspect a tap outside the tip over a counter control and over inert space; identify whether dismissal and underlying action are both reachable.

TARGETED VALIDATION
Use existing host-capable TipHostTest evidence and source event tracing; explicitly report any unresolved product-intent conflict without speculative modification.

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
After recording this result, continue automatically to prompt 0060 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0059 as fully completed only if all its required work is complete, identify any incomplete work, and name 0060 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0060: Bundled guide content and contextual action outcomes remain coherent

```text
KNITTOOLS CODE REVIEW PROMPT 0060
Title: Bundled guide content and contextual action outcomes remain coherent
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
Trace GuideContent topic keys, item actions and Pro metadata through GuideViewModel result handling. Verify unknown topics and missing project context use documented fallbacks, while delayed action results remain attached to their originating entry. Check that the guide stays readable without website availability and reset failures use the shared reset outcome.

CONCRETE VERIFICATION
Choose a project-dependent item before a delayed project lookup, leave its entry, and compare the result with unknown-topic and offline-content reads.

TARGETED VALIDATION
Use GuideContentTest and GuideViewModelTest for deterministic content and stale-result assertions; leave generic graph and external-link review to their owners.

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
After recording this result, continue automatically to prompt 0061 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0060 as fully completed only if all its required work is complete, identify any incomplete work, and name 0061 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


### Project creation and list semantics

#### Prompt 0061: Project names normalize for collision checks without losing display case

```text
KNITTOOLS CODE REVIEW PROMPT 0061
Title: Project names normalize for collision checks without losing display case
Review area: Project creation and list semantics

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
Trace project-name input through trimming, nonblank validation, collision comparison and insertion. Verify comparison uses the current Locale.ROOT rule while the stored display name preserves intended case. Inspect all creation callers so one entry point cannot bypass naming merely because its source is onboarding or imported content.

CONCRETE VERIFICATION
Create names differing only by surrounding whitespace and case under a non-English locale; inspect accepted display text and collision handling.

TARGETED VALIDATION
Use ProjectNameRules and creation tests with controlled locale; do not introduce a name-length limit absent from current requirements.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0062 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0061 as fully completed only if all its required work is complete, identify any incomplete work, and name 0062 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0062: Duplicate project suffixing chooses the next available name

```text
KNITTOOLS CODE REVIEW PROMPT 0062
Title: Duplicate project suffixing chooses the next available name
Review area: Project creation and list semantics

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
Inspect ProjectNameRules suffix allocation against the complete existing-name set, including completed projects. Trace the resulting normalized candidate through the insertion boundary and establish how simultaneous creates are serialized. Verify existing parenthesized names are handled by the implemented naming rule rather than a presumed renumbering convention.

CONCRETE VERIFICATION
Use existing names Hat, Hat (2) and Hat (4), then create Hat twice; compare chosen candidates and actual transaction ordering.

TARGETED VALIDATION
Run focused naming tests and inspect creation serialization; distinguish repository naming evidence from any nonexistent database uniqueness constraint.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0063 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0062 as fully completed only if all its required work is complete, identify any incomplete work, and name 0063 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0063: Custom counter labels remain valid after bounded normalization

```text
KNITTOOLS CODE REVIEW PROMPT 0063
Title: Custom counter labels remain valid after bounded normalization
Review area: Project creation and list semantics

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
Trace custom-label selection through trimming, safe truncation and persistence. Verify an empty normalized label cannot remain the selected custom type and truncation does not split a surrogate pair. Check how saved label type and optional text are read back, without borrowing folder or project-name limits.

CONCRETE VERIFICATION
Compare whitespace-only custom text with a label whose supplementary character crosses the 32-unit boundary, then inspect the persisted type/text pair.

TARGETED VALIDATION
Use focused label-normalization tests and creation argument assertions; preserve the actual Kotlin-unit contract rather than changing the limit.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0064 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0063 as fully completed only if all its required work is complete, identify any incomplete work, and name 0064 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0064: Creation craft defaults use the last successful creation

```text
KNITTOOLS CODE REVIEW PROMPT 0064
Title: Creation craft defaults use the last successful creation
Review area: Project creation and list semantics

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
Inspect initial and restored craft defaults from last_project_craft_type into ordinary project drafts. Verify unknown persisted values follow the current knitting fallback and failed or canceled creation does not become the remembered craft. Trace post-commit preference failure separately so project success remains authoritative and subsequent defaults are predictable.

CONCRETE VERIFICATION
Compare first use, an unknown stored craft and a canceled crochet draft followed by a new form; inspect the selected default.

TARGETED VALIDATION
Use LastProjectCraftPreferenceTest and ProjectCreationActionsTest with captured persistence calls; avoid changing language or global preferences during validation.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0065 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0064 as fully completed only if all its required work is complete, identify any incomplete work, and name 0065 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0065: Blank starting count is zero rather than invalid or inherited

```text
KNITTOOLS CODE REVIEW PROMPT 0065
Title: Blank starting count is zero rather than invalid or inherited
Review area: Project creation and list semantics

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
Trace ProjectStartValues from raw ordinary-form text through repository arguments. Verify empty count text becomes zero, valid values stay within the documented start range and unrelated previous project state cannot seed the count. Check restored drafts and quota-retry payloads before concluding an apparent default came from the parser.

CONCRETE VERIFICATION
Open creation after a project at count 90, leave start blank and compare with explicit zero and 99,999 inputs.

TARGETED VALIDATION
Use ProjectStartValuesTest plus one captured creation request; verify no prior selected-project field supplies a hidden default.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0066 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0065 as fully completed only if all its required work is complete, identify any incomplete work, and name 0066 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0066: Initial project values do not simulate count actions

```text
KNITTOOLS CODE REVIEW PROMPT 0066
Title: Initial project values do not simulate count actions
Review area: Project creation and list semantics

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
Follow ordinary createProject with a nonzero starting count and optional target through insertion and post-creation effects. Verify initial values produce no counter history, linked delta, stitch reset, reading-line movement or session checkpoint. A newly created project must remain usable at its supplied count without manufacturing prior work.

CONCRETE VERIFICATION
Create at count 35, immediately invoke Undo and inspect history and session calls; the initial count should remain unchanged.

TARGETED VALIDATION
Use CounterRepositoryCreateProjectTest and the relevant transaction source; assert absence of action side effects rather than only the inserted count.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0067 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0066 as fully completed only if all its required work is complete, identify any incomplete work, and name 0067 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0067: Optional target parsing is shared by creation and target editing

```text
KNITTOOLS CODE REVIEW PROMPT 0067
Title: Optional target parsing is shared by creation and target editing
Review area: Project creation and list semantics

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
Compare ProjectStartValues use in the creation form and existing target dialog. Trace blank, invalid and valid target results into persistence, ensuring the same documented positive range applies where required. Check whether either caller converts invalid raw input to absence or retains an obsolete previous target on confirmation.

CONCRETE VERIFICATION
Apply blank, zero, one and 99,999 in both entry points, then compare validation and persisted optional-target semantics.

TARGETED VALIDATION
Run shared parser tests and inspect each caller mapping; add only a focused integration assertion for an evidenced discrepancy.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0068 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0067 as fully completed only if all its required work is complete, identify any incomplete work, and name 0068 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0068: Quota retry retains the exact pending creation values

```text
KNITTOOLS CODE REVIEW PROMPT 0068
Title: Quota retry retains the exact pending creation values
Review area: Project creation and list semantics

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
Trace ProjectCreationActions through rejected admission, pending draft storage and retry. Verify name, craft, label, starting count, target and intended folder remain associated with the original request rather than current screen defaults. Identify when current repository validation is rerun so stale pending values do not bypass ordinary creation rules.

CONCRETE VERIFICATION
Defer a creation containing a nonzero start and target, change visible defaults, then retry after admission changes; inspect the final arguments.

TARGETED VALIDATION
Use ProjectCreationActionsTest and ProjectCreationStateTest with captured retries; entitlement mechanics themselves remain outside this focus.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0069 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0068 as fully completed only if all its required work is complete, identify any incomplete work, and name 0069 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0069: Pending creation payload version rejects incompatible legacy drafts safely

```text
KNITTOOLS CODE REVIEW PROMPT 0069
Title: Pending creation payload version rejects incompatible legacy drafts safely
Review area: Project creation and list semantics

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
Inspect pending-draft serialization around serial UID 1 and payloadVersion 2. Verify deserialization distinguishes absent legacy fields from intentionally blank new fields and discards incompatible payloads without crashing or submitting guessed values. Trace fallback into a usable creation flow and preserve any unrelated existing project state.

CONCRETE VERIFICATION
Restore a current payload and the supported legacy-incompatible fixture; compare accepted fields and whether any automatic creation is attempted.

TARGETED VALIDATION
Use ProjectCreationStateTest compatibility fixtures and current serializer code; avoid altering format versions without a confirmed compatibility defect.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0070 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0069 as fully completed only if all its required work is complete, identify any incomplete work, and name 0070 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0070: Creation validates destination folder at commit time

```text
KNITTOOLS CODE REVIEW PROMPT 0070
Title: Creation validates destination folder at commit time
Review area: Project creation and list semantics

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
Trace the optional destination folder from form context through createProject admission and assignment insertion. Verify deletion or invalidation after opening the form is checked inside the same operation that creates the project. Determine the documented failure result so a rejected folder cannot silently create an unintended unfiled project.

CONCRETE VERIFICATION
Open creation in a folder, remove that folder before submission, and compare with a valid destination under identical project inputs.

TARGETED VALIDATION
Use existing creation/folder repository tests and inspect the transaction boundary; report real SQLite evidence separately from fake-call assertions.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0071 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0070 as fully completed only if all its required work is complete, identify any incomplete work, and name 0071 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0071: Creation result binds the new project rather than stale selection

```text
KNITTOOLS CODE REVIEW PROMPT 0071
Title: Creation result binds the new project rather than stale selection
Review area: Project creation and list semantics

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
Follow successful createProject output through ProjectCreationActions and selection/open requests. Verify a delayed result cannot reuse a previously selected project ID or lose the newly generated ID after quota retry. Check failure and cancellation paths so a nonexistent result never opens a workspace that looks successfully created.

CONCRETE VERIFICATION
Delay creation while selecting an older project, then complete the request and inspect the project identity in the intended completion handoff.

TARGETED VALIDATION
Use existing creation-action tests with controlled scheduling and explicit returned IDs; leave generic navigation-stack behavior to its separate review.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0072 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0071 as fully completed only if all its required work is complete, identify any incomplete work, and name 0072 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0072: Edit details does not accidentally expose creation-only writes

```text
KNITTOOLS CODE REVIEW PROMPT 0072
Title: Edit details does not accidentally expose creation-only writes
Review area: Project creation and list semantics

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
Inspect the Edit variant of ProjectDetailsSheet and its repository mutation path. Verify editing name, craft or label cannot overwrite count, starting values or target through reused creation draft defaults. Trace preservation of fields not in the current edit contract and assess current validation before suggesting any expanded editor capability.

CONCRETE VERIFICATION
Edit only the name on a project with a nonzero count and target; compare all count-related fields before and after persistence.

TARGETED VALIDATION
Use focused project-detail or repository tests with nondefault fields and inspect update columns; do not add unsupported editing features.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0073 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0072 as fully completed only if all its required work is complete, identify any incomplete work, and name 0073 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0073: Active Updated sort follows update time with deterministic ties

```text
KNITTOOLS CODE REVIEW PROMPT 0073
Title: Active Updated sort follows update time with deterministic ties
Review area: Project creation and list semantics

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
Trace ProjectSortOrder.Updated for active projects from DAO ordering through list mapping and folder filtering. Verify updatedAt descending and ID descending tie-breaking survive any downstream sorting or grouping. Check a count change or metadata-only folder change against the actual timestamp contract before claiming incorrect movement.

CONCRETE VERIFICATION
Use equal updatedAt values with different IDs, then update one count and move another between folders; compare resulting active order.

TARGETED VALIDATION
Inspect SQL and existing project-list sorting tests; run host assertions for downstream order without treating fake ordering as SQLite proof.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0074 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0073 as fully completed only if all its required work is complete, identify any incomplete work, and name 0074 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0074: Completed Updated sort follows completion time

```text
KNITTOOLS CODE REVIEW PROMPT 0074
Title: Completed Updated sort follows completion time
Review area: Project creation and list semantics

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
Inspect completed-project sorting and presentation after repeated completion and reopening cycles. Verify Updated uses completedAt rather than unrelated updatedAt, with documented descending ID ties. Trace lifecycle writes that provide completion time so ordering is evaluated from current state rather than the historical event list.

CONCRETE VERIFICATION
Complete two projects at distinct times, edit unrelated allowed metadata, then compare their completed-list order and tie behavior.

TARGETED VALIDATION
Use list-state tests and inspect completed DAO ordering plus lifecycle timestamp assignments; no device sorting walkthrough is required.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0075 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0074 as fully completed only if all its required work is complete, identify any incomplete work, and name 0075 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0075: Name sorting preserves SQLite semantics before folder filtering

```text
KNITTOOLS CODE REVIEW PROMPT 0075
Title: Name sorting preserves SQLite semantics before folder filtering
Review area: Project creation and list semantics

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
Trace Name order from COLLATE NOCASE SQL through active/completed flows and folder predicates. Verify downstream code does not replace it with locale-sensitive sorting or lowercase the displayed names. Treat SQLite ordering as the current contract, especially for non-ASCII names, rather than imposing natural-language collation as a defect fix.

CONCRETE VERIFICATION
Compare mixed-case names and a few non-ASCII names across All Projects and one folder; filtering should retain the original relative order.

TARGETED VALIDATION
Inspect actual SQL and existing database-test fixtures; use host list-mapping tests only for order preservation after filtering.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0076 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0075 as fully completed only if all its required work is complete, identify any incomplete work, and name 0076 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0076: Created sort ignores later activity while retaining deterministic ties

```text
KNITTOOLS CODE REVIEW PROMPT 0076
Title: Created sort ignores later activity while retaining deterministic ties
Review area: Project creation and list semantics

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
Inspect Created ordering through project insertion, observation and mapping. Verify later counting, completion metadata and folder moves do not rewrite createdAt or cause downstream resorting by another field. Check equal timestamps against the documented descending ID fallback before treating visible card order as nondeterministic.

CONCRETE VERIFICATION
Create equal-time fixture projects, update an older project and change its folder; compare Created order before and after the operations.

TARGETED VALIDATION
Use focused project-list tests and exact timestamp fields; inspect repository writers rather than adding a separate sorting implementation.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0077 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0076 as fully completed only if all its required work is complete, identify any incomplete work, and name 0077 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0077: Completed visibility is global rather than folder-local

```text
KNITTOOLS CODE REVIEW PROMPT 0077
Title: Completed visibility is global rather than folder-local
Review area: Project creation and list semantics

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
Trace the completed-project visibility preference through list ViewModel restoration and folder changes. Verify its default and persisted scope remain independent of a SavedStateHandle folder filter. Inspect how changing visibility recomputes rows and selection without modifying the projects' completion state or the all-project existence signal.

CONCRETE VERIFICATION
Enable completed visibility in one folder, switch folders and reopen the list; compare the setting and actual project lifecycle data.

TARGETED VALIDATION
Use ProjectFoldersViewModelTest with shared preference emissions and independent filter state; limit verification to the documented ownership boundary.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0078 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0077 as fully completed only if all its required work is complete, identify any incomplete work, and name 0078 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0078: Continue hero chooses the first visible positive-count active project

```text
KNITTOOLS CODE REVIEW PROMPT 0078
Title: Continue hero chooses the first visible positive-count active project
Review area: Project creation and list semantics

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
Follow Continue hero derivation from already-sorted active projects through current folder filtering and count qualification. Verify zero-count projects are skipped, completed projects never qualify and no independently persisted selected ID overrides the first eligible visible candidate. Check recomputation after count reset, filter changes and sorting changes.

CONCRETE VERIFICATION
Use two positive-count active projects and a newer zero-count project, then change sort and folder; compare the derived hero ID.

TARGETED VALIDATION
Run focused list ViewModel derivation tests with explicit ordering; source-trace hero callbacks only to verify the same identity.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0079 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0078 as fully completed only if all its required work is complete, identify any incomplete work, and name 0079 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0079: Continue hero remains included in active project totals

```text
KNITTOOLS CODE REVIEW PROMPT 0079
Title: Continue hero remains included in active project totals
Review area: Project creation and list semantics

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
Inspect any splitting of the hero project from ordinary card rows and the computation of active-count labels. Verify moving one item into the hero presentation does not reduce the membership count or quota facts. Trace empty-hero transitions so count zero affects presentation eligibility without removing the project itself.

CONCRETE VERIFICATION
Reset the sole positive-count active project to zero; compare visible hero disappearance with the unchanged active-project count.

TARGETED VALIDATION
Use project-list model tests that assert both hero identity and membership totals from the same source snapshot.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0080 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0079 as fully completed only if all its required work is complete, identify any incomplete work, and name 0080 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0080: Overview aggregate work derives from saved sessions only

```text
KNITTOOLS CODE REVIEW PROMPT 0080
Title: Overview aggregate work derives from saved sessions only
Review area: Project creation and list semantics

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
Trace total recorded work in project overview from session facts to its displayed aggregate. Verify merely opening overview or an active unfinalized interval cannot be counted as completed history unless the current contract explicitly distinguishes it. Inspect exact-second versus compatibility-minute inputs to avoid cumulative truncation errors.

CONCRETE VERIFICATION
Compare two short saved sessions with one currently active session and inspect which durations contribute to the overview total.

TARGETED VALIDATION
Use existing aggregate helpers and focused project-state tests; confirm units and source fields before asserting any rounding defect.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0081 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0080 as fully completed only if all its required work is complete, identify any incomplete work, and name 0081 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0081: Overview and Counter observe one project-content owner

```text
KNITTOOLS CODE REVIEW PROMPT 0081
Title: Overview and Counter observe one project-content owner
Review area: Project creation and list semantics

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
Inspect the shared Projects-scoped CounterViewModel path used by overview and Counter, focusing on selected project facts and content actions. Verify a count, target or note update observed on one surface is not overwritten by a separate stale copy on return. Identify actual observation-generation guards before proposing synchronization changes.

CONCRETE VERIFICATION
Change project content through one surface while a delayed read from the other is pending; compare the final selected project and fields.

TARGETED VALIDATION
Use controlled CounterViewModel tests and ownership source tracing; do not duplicate generic navigation or lifecycle audits.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0082 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0081 as fully completed only if all its required work is complete, identify any incomplete work, and name 0082 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0082: Project card completion facts come from current lifecycle state

```text
KNITTOOLS CODE REVIEW PROMPT 0082
Title: Project card completion facts come from current lifecycle state
Review area: Project creation and list semantics

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
Trace card mapping for active and completed projects, including completion date, original-cased name and progress/section text. Verify reopened projects stop displaying a stale current completion date while preserving historical completion events elsewhere. Inspect model reuse and memoization for a concrete mechanism that could retain old lifecycle facts.

CONCRETE VERIFICATION
Complete, reopen and rename one project while keeping its historical event; compare the card model after each observed update.

TARGETED VALIDATION
Use project-list mapping tests with successive snapshots; inspect data bindings without requiring theme, layout or device checks.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt

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
After recording this result, continue automatically to prompt 0083 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0082 as fully completed only if all its required work is complete, identify any incomplete work, and name 0083 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


### Folder metadata and visible selection

#### Prompt 0083: Virtual All Projects and Unfiled filters never become persisted folders

```text
KNITTOOLS CODE REVIEW PROMPT 0083
Title: Virtual All Projects and Unfiled filters never become persisted folders
Review area: Folder metadata and visible selection

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
Trace typed folder-filter values through list selection, repository queries and move commands. Verify All Projects and Unfiled are interpreted by their actual types rather than invented sentinel row IDs. Check restored values and destination translation so a virtual filter cannot create a spurious assignment or accidentally target an unrelated real folder.

CONCRETE VERIFICATION
Compare selecting All Projects, selecting Unfiled and moving to Unfiled; inspect repository arguments and resulting assignment presence.

TARGETED VALIDATION
Use ProjectFoldersViewModelTest and folder repository tests; assert typed values and assignments without modifying any existing user folders.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0084 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0083 as fully completed only if all its required work is complete, identify any incomplete work, and name 0084 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0084: Folder display text is trimmed without losing intentional case

```text
KNITTOOLS CODE REVIEW PROMPT 0084
Title: Folder display text is trimmed without losing intentional case
Review area: Folder metadata and visible selection

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
Inspect folder create/rename normalization from raw input to stored display name. Verify trimming is separated from comparison normalization, so user casing remains visible while invalid blank names are rejected. Trace all mutation callers and restored drafts before claiming a UI-only validator protects repository entry points.

CONCRETE VERIFICATION
Create and rename using surrounding whitespace and mixed case, then read the persisted display name and normalized comparison value.

TARGETED VALIDATION
Run focused ProjectFolderRepositoryTest normalization cases and inspect each public mutation path for consistent validation.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0085 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0084 as fully completed only if all its required work is complete, identify any incomplete work, and name 0085 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0085: Folder names reject forbidden controls and separators

```text
KNITTOOLS CODE REVIEW PROMPT 0085
Title: Folder names reject forbidden controls and separators
Review area: Folder metadata and visible selection

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
Trace the actual folder-name character validation for controls and Unicode line or paragraph separators. Verify accepted normalization cannot reintroduce rejected characters, and both creation and rename apply the same rule. Distinguish visible whitespace trimming from rejection of embedded separators before proposing broader restrictions on ordinary Unicode names.

CONCRETE VERIFICATION
Compare a normal accented name with embedded newline, line-separator and paragraph-separator inputs in both create and rename operations.

TARGETED VALIDATION
Use pure validation tests or focused repository cases; preserve legitimate Unicode text and the documented rejection boundary.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0086 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0085 as fully completed only if all its required work is complete, identify any incomplete work, and name 0086 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0086: Folder length uses the documented Kotlin-unit boundary

```text
KNITTOOLS CODE REVIEW PROMPT 0086
Title: Folder length uses the documented Kotlin-unit boundary
Review area: Folder metadata and visible selection

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
Inspect the folder-name length check after trimming and any Unicode normalization. Verify the 50-unit contract is enforced consistently without accidentally importing grapheme or byte limits. Determine actual treatment of supplementary characters and whether invalid rename leaves the original name untouched rather than partially normalized in storage.

CONCRETE VERIFICATION
Compare trimmed names of 49, 50 and 51 Kotlin units, including a supplementary character near the boundary.

TARGETED VALIDATION
Use focused repository validation tests and persisted-name assertions; do not change the limit based on display-width preferences.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0087 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0086 as fully completed only if all its required work is complete, identify any incomplete work, and name 0087 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0087: Folder uniqueness combines NFC normalization with root casing

```text
KNITTOOLS CODE REVIEW PROMPT 0087
Title: Folder uniqueness combines NFC normalization with root casing
Review area: Folder metadata and visible selection

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
Trace folder uniqueness keys for creation and rename, including lookup exclusion of the row being renamed. Verify canonically equivalent Unicode names and case variants collide under the documented rule without corrupting display text. Inspect transaction ordering so current repository data participates in the final collision decision. Compare the generated normalizedName with the actual unique index and rename exclusion predicates, including concurrent create/rename admission.

CONCRETE VERIFICATION
Use composed and decomposed accented forms, case variants and a rename to the same normalized name; compare accepted results.

TARGETED VALIDATION
Run ProjectFolderRepositoryTest normalization cases and inspect integration tests for concurrent collision handling; keep database evidence limits explicit.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0088 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0087 as fully completed only if all its required work is complete, identify any incomplete work, and name 0088 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0088: Folder rename does not alter membership or project timestamps

```text
KNITTOOLS CODE REVIEW PROMPT 0088
Title: Folder rename does not alter membership or project timestamps
Review area: Folder metadata and visible selection

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
Follow folder rename from request through row update and organization snapshot emission. Verify the mutation changes folder metadata only and retains its identity, ordering and existing assignments. Inspect downstream project list recomputation for accidental project writes rather than treating harmless list invalidation as a data mutation.

CONCRETE VERIFICATION
Rename a folder containing active and completed projects, then compare assignment IDs and each project count, timestamp and session facts.

TARGETED VALIDATION
Use repository interaction or integration assertions on touched rows; do not add broad tests for unrelated content tables.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0089 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0088 as fully completed only if all its required work is complete, identify any incomplete work, and name 0089 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0089: Folder ordering persists identity rather than display-name positions

```text
KNITTOOLS CODE REVIEW PROMPT 0089
Title: Folder ordering persists identity rather than display-name positions
Review area: Folder metadata and visible selection

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
Inspect the current folder-order mutation API and read ordering before assessing behavior. Trace submitted folder identities, validation and persistence so a rename or concurrent deletion cannot reorder an unintended row through stale indices. Preserve the implemented ordering contract rather than inventing drag behavior or unsupported controls.

CONCRETE VERIFICATION
Using an existing reorder path, change a folder name and remove another folder between reading and submitting order; inspect affected IDs.

TARGETED VALIDATION
Use existing folder-order tests and current mutation fixtures; if no such callable path remains, document that scope as inapplicable.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0090 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0089 as fully completed only if all its required work is complete, identify any incomplete work, and name 0090 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0090: Deleting a folder removes assignments while preserving projects

```text
KNITTOOLS CODE REVIEW PROMPT 0090
Title: Deleting a folder removes assignments while preserving projects
Review area: Folder metadata and visible selection

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
Trace folder deletion through repository validation, assignment removal and organization observation. Verify projects become unfiled rather than deleted or otherwise modified, including completed projects. Inspect transaction and result handling so a failed deletion does not leave the UI claiming a folder was removed while its memberships remain unchanged.

CONCRETE VERIFICATION
Delete a folder containing mixed lifecycle states and compare project IDs, content values and assignment absence; repeat with injected failure.

TARGETED VALIDATION
Use folder repository integration tests or their source; distinguish rollback evidence from a fake that merely records calls.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0091 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0090 as fully completed only if all its required work is complete, identify any incomplete work, and name 0091 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0091: Single-project moves preserve at most one current assignment

```text
KNITTOOLS CODE REVIEW PROMPT 0091
Title: Single-project moves preserve at most one current assignment
Review area: Folder metadata and visible selection

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
Follow an existing project's move between real folders and Unfiled through the repository's actual assignment operation. Verify replacing membership cannot retain both old and new assignments or insert a second canonical project. Check project and destination validation at the write boundary, including stale project IDs.

CONCRETE VERIFICATION
Move one project from folder A to B and then Unfiled; inspect the single effective membership after each operation.

TARGETED VALIDATION
Use focused folder database/repository tests and assignment queries; keep schema constraints review outside this feature-level path.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0092 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0091 as fully completed only if all its required work is complete, identify any incomplete work, and name 0092 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0092: Bulk move deduplicates repeated project IDs before writing

```text
KNITTOOLS CODE REVIEW PROMPT 0092
Title: Bulk move deduplicates repeated project IDs before writing
Review area: Folder metadata and visible selection

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
Inspect the bulk-move input normalization and validation order. Verify duplicate project IDs cannot cause inconsistent membership results, redundant dependent actions or misleading affected-count reporting. Trace the deduplicated set into the same transaction and preserve the documented all-or-nothing metadata behavior rather than borrowing sequential bulk-delete semantics.

CONCRETE VERIFICATION
Submit the same project several times together with another valid project and compare final memberships and reported operation result.

TARGETED VALIDATION
Run focused ProjectFolderRepositoryTest cases that capture validated IDs and write calls; inspect real transaction usage for atomicity.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0093 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0092 as fully completed only if all its required work is complete, identify any incomplete work, and name 0093 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0093: Bulk move validates every project and destination before mutation

```text
KNITTOOLS CODE REVIEW PROMPT 0093
Title: Bulk move validates every project and destination before mutation
Review area: Folder metadata and visible selection

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
Trace bulk move from visible selection into repository revalidation of all project IDs and the destination folder. Verify one missing project or deleted destination rejects the whole metadata operation before assignments change. Identify how concurrent deletion is covered by the existing transaction rather than adding speculative retry infrastructure.

CONCRETE VERIFICATION
Move several projects with a missing ID in the middle, then repeat with a removed destination; compare all memberships against baseline.

TARGETED VALIDATION
Use existing integration tests for rollback where executable, plus focused fake tests for validation order and explicit failure results.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0094 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0093 as fully completed only if all its required work is complete, identify any incomplete work, and name 0094 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0094: Folder filter restoration waits for a real organization snapshot

```text
KNITTOOLS CODE REVIEW PROMPT 0094
Title: Folder filter restoration waits for a real organization snapshot
Review area: Folder metadata and visible selection

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
Inspect SavedStateHandle folder restoration and the loaded/snapshot discriminator supplied by the organization repository. Verify an initial placeholder cannot invalidate a stored real-folder filter, while a confirmed missing folder eventually falls back according to current behavior. Trace error states separately from a genuine empty organization snapshot.

CONCRETE VERIFICATION
Restore a valid folder ID, emit a loading placeholder, then its actual snapshot; contrast with a loaded snapshot proving deletion.

TARGETED VALIDATION
Run ProjectFoldersViewModelTest with controlled emissions and assert filter changes only at the intended authoritative boundary.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0095 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0094 as fully completed only if all its required work is complete, identify any incomplete work, and name 0095 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0095: A new task starts with All Projects without overwriting restored state

```text
KNITTOOLS CODE REVIEW PROMPT 0095
Title: A new task starts with All Projects without overwriting restored state
Review area: Folder metadata and visible selection

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
Trace initialization of the folder filter for a new ViewModel versus reconstruction from SavedStateHandle. Verify default All Projects is not stored globally across tasks and does not overwrite a valid restored entry. Inspect any preference conversion left from older code before inferring a persistence defect.

CONCRETE VERIFICATION
Create one owner without saved state and another with a valid saved folder ID; feed the same organization snapshot to both.

TARGETED VALIDATION
Use ViewModel construction tests and inspect actual saved-state keys; no task-manager or device operations are necessary.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0096 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0095 as fully completed only if all its required work is complete, identify any incomplete work, and name 0096 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0096: Folder organization never changes working project timestamps

```text
KNITTOOLS CODE REVIEW PROMPT 0096
Title: Folder organization never changes working project timestamps
Review area: Folder metadata and visible selection

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
Trace create, rename, move and delete folder operations through all repository collaborators. Verify the documented metadata-only contract excludes project updatedAt changes, which would otherwise reorder Updated and Continue unexpectedly. Seek actual update statements or shared helpers causing the effect rather than treating flow re-emission as a timestamp write.

CONCRETE VERIFICATION
Move the currently continued project between folders and compare updatedAt, count history and active-session state before and after.

TARGETED VALIDATION
Use folder repository call assertions plus current SQL inspection; add a narrowly scoped regression only for an evidenced timestamp mutation.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0097 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0096 as fully completed only if all its required work is complete, identify any incomplete work, and name 0097 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0097: Visible selection is reconciled after folder or completion visibility changes

```text
KNITTOOLS CODE REVIEW PROMPT 0097
Title: Visible selection is reconciled after folder or completion visibility changes
Review area: Folder metadata and visible selection

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
Inspect selected-ID state, visible-row derivation and reconciliation in ProjectListViewModel. Verify changing folders or hiding completed projects removes IDs no longer actionable without erasing valid visible selection. Check the ordering of asynchronous organization and project emissions so stale IDs cannot survive until a destructive command.

CONCRETE VERIFICATION
Select active and completed projects, hide completed rows and switch folders; inspect the resulting IDs supplied to bulk actions.

TARGETED VALIDATION
Use ProjectFoldersViewModelTest with successive visibility snapshots; assert action inputs rather than relying only on selected-count text.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0098 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0097 as fully completed only if all its required work is complete, identify any incomplete work, and name 0098 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0098: Select All includes only the currently visible project set

```text
KNITTOOLS CODE REVIEW PROMPT 0098
Title: Select All includes only the currently visible project set
Review area: Folder metadata and visible selection

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
Trace Select All through filtered active and completed rows and any separate Continue hero presentation. Verify hidden projects are excluded and a visible hero is handled consistently with actual membership. Check whether loading or stale lists can supply a different set than the one represented by the current selection model.

CONCRETE VERIFICATION
Use visible active, hidden completed and other-folder projects, including the hero candidate; compare selected IDs with the visible membership set.

TARGETED VALIDATION
Run focused list ViewModel selection tests across loaded snapshots; inspect hero splitting without adding layout assertions.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0099 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0098 as fully completed only if all its required work is complete, identify any incomplete work, and name 0099 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0099: Sequential bulk complete and delete stop at the first failed result

```text
KNITTOOLS CODE REVIEW PROMPT 0099
Title: Sequential bulk complete and delete stop at the first failed result
Review area: Folder metadata and visible selection

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
Inspect bulk complete/delete iteration, typed outcomes and progress reporting. Verify a missing or failed project halts later operations while earlier committed successes remain represented accurately. Do not assume these commands are one transaction or attempt compensation that would undo legitimate prior results; trace selection cleanup for partial completion.

CONCRETE VERIFICATION
Supply three projects and fail the middle operation; verify the first remains committed, the third is untouched and the result describes partial progress.

TARGETED VALIDATION
Use deterministic repository fakes in list action tests and inspect exact operation order; avoid deleting real project content.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0100 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0099 as fully completed only if all its required work is complete, identify any incomplete work, and name 0100 as the next prompt. Do not invent its content or claim the whole audit is finished.
```


#### Prompt 0100: Projects folder filters do not leak into independent feature readers

```text
KNITTOOLS CODE REVIEW PROMPT 0100
Title: Projects folder filters do not leak into independent feature readers
Review area: Folder metadata and visible selection

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
Trace the current folder filter's consumers beyond ProjectListViewModel. Verify Insights project lists, widget resolution, direct project lookup and history readers obtain their documented independent project context. A hidden project must remain addressable where supported; identify an actual shared-flow dependency before treating visibility as unintended access restriction.

CONCRETE VERIFICATION
Choose a restrictive folder while resolving a different active project for widget or history state; compare results with All Projects.

TARGETED VALIDATION
Use source consumer tracing and focused reader tests with independent filters; avoid duplicating generic navigation or access-control reviews.

SOURCE LEADS TO RESOLVE IN THE CURRENT REPOSITORY
- Projects, folders, sorting, and selection
- app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt

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
After recording this result, continue automatically to prompt 0101 when it is present in the supplied input. If it is unavailable or continuation is impossible, report 0100 as fully completed only if all its required work is complete, identify any incomplete work, and name 0101 as the next prompt. Do not invent its content or claim the whole audit is finished.
```
