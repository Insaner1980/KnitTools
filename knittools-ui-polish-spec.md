# KnitTools UI Polish — Implementation Spec

**Scope:** Three Project List UI improvements. No new features, no new database fields, no new screens.

**Base:** Current v1.0.0 codebase.

---

## 1. Project List — Deduplicate Hero Card Project

### Problem
The Continue Knitting hero card shows the most recently active project, and that same project appears again as the first item in the ACTIVE section. This feels redundant.

### Change
When rendering the ACTIVE project list, **exclude the project currently shown in the Continue Knitting hero card** from the list.

### Rules
- Only exclude when the hero card is visible (i.e. there is an active project with rows > 0)
- If the excluded project is the **only** active project, the ACTIVE section header still appears but shows empty state or is hidden — use your judgment on which looks better
- COMPLETED section is unaffected

### File
- `ui/screens/projects/ProjectListScreen.kt` (or its ViewModel) — filter logic when building the active projects list

---

## 2. Project List — Deduplicate Title/Subtitle in Project Cards

### Problem
When a project's name and its linked pattern name are identical, the card shows the same text twice: once as the title and once as an orange subtitle. This wastes space.

### Change
- If `project.name` equals the linked pattern name (case-insensitive trim comparison): show **only one title line**
- If they differ: show title + subtitle as currently

### File
- `ui/screens/projects/ProjectListScreen.kt` — inside ProjectCard composable (or wherever the card content is rendered)

---

## 3. Project List — Reduce Emphasis on 0 Rows

### Problem
Projects with `0 rows` display the row count with the same visual weight as active projects. An unstarted project shouldn't demand the same attention.

### Change
- When `count == 0`: use `onSurfaceMuted` color (from extended colors) for the row count text instead of `Primary`
- When `count > 0`: keep current `Primary` color

### Rules
- Only the row count number/text changes color. Everything else on the card stays the same.
- This applies in both ACTIVE and COMPLETED sections (though 0-row completed projects should be rare)

### File
- `ui/screens/projects/ProjectListScreen.kt` — inside ProjectCard, conditional color on the row count text

---

## General Notes

- No new routes, no new database migrations, no new entities
- All changes are visual/layout adjustments to existing screens
- Follow existing color tokens from `MaterialTheme.knitToolsColors` — no hardcoded colors
