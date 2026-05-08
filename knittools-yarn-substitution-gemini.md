# KnitTools — Yarn Substitution Awareness in Project Summary

**Type:** Delta spec — prompt enhancement to existing Project Summary  
**Status:** Spec ready for implementation  
**Depends on:** Project Summary with Gemini + "Where Was I?" context awareness (both already implemented)  
**Pro only:** Yes  
**Key idea:** No new UI. Summary automatically mentions yarn compatibility when project has both a pattern and a linked yarn that differ from the pattern's recommendation.

---

## 1. What Changes

The Project Summary prompt is extended to notice when a project has:
- A linked pattern (with recommended yarn info)
- A linked yarn card (from My Yarn)
- And these two are different yarns

When this situation is detected, the summary includes a brief compatibility note comparing weight category, fiber content, and meters-per-gram ratio.

No new buttons, screens, or menu items.

---

## 2. Additional Data for Prompt

Add to the existing data already gathered for project summary:

| Data | Source | Example |
|------|--------|---------|
| Pattern recommended yarn | Extracted from pattern name/metadata if available | "Drops Alpaca" |
| Linked yarn brand + name | YarnCardEntity.brand + name | "Novita Nalle" |
| Linked yarn weight category | YarnCardEntity.weightCategory | "FINGERING" |
| Linked yarn fiber content | YarnCardEntity.fiberContent | "75% Wool, 25% Polyamide" |
| Linked yarn meters per skein | YarnCardEntity.metersPerSkein | 260 |
| Linked yarn grams per skein | YarnCardEntity.gramsPerSkein | 100 |
| Linked yarn recommended needle | YarnCardEntity.recommendedNeedle | "3.5mm" |
| Linked yarn gauge stitches | YarnCardEntity.gaugeStitches | 26 |

Most of this data is already available from the yarn card. The pattern's recommended yarn may come from the pattern name or saved pattern metadata.

---

## 3. Updated Prompt

Add this paragraph to the existing project summary prompt, after the existing instructions:

```
If the project has both a linked pattern and a linked yarn, briefly note yarn compatibility. Compare weight category, fiber type, and meters-per-gram ratio if available. If they are clearly different (e.g., DK pattern with fingering yarn), mention it factually with a practical note about gauge adjustment. If they seem compatible, do not mention it — only flag differences. Keep the yarn note to one sentence maximum.
```

Add this to the project data section:

```
- Linked yarn: {brand} {name}, {weightCategory}, {fiberContent}, {metersPerSkein}m/{gramsPerSkein}g, recommended needle {recommendedNeedle}, gauge {gaugeStitches} sts/10cm
- Pattern recommended yarn: {patternYarnInfo or "not specified in metadata"}
```

---

## 4. Example Outputs

**Different weight — clear mismatch:**
> Corner To Corner Dishcloth, row 24 in the decrease section. Last session was 5 days ago. You've linked Novita Nalle (fingering, 260m/100g) but the pattern calls for Drops Alpaca (DK, 167m/50g) — different weight category, check your gauge before continuing.

**Same weight, different fiber — minor note:**
> Corner To Corner Dishcloth, row 24. Good pace at 8 rows per session. You're using Sandnes Peer Gynt (100% wool) where the pattern suggests a wool-alpaca blend — should work fine at the same gauge but the fabric will feel slightly different.

**Compatible yarn — no mention:**
> Corner To Corner Dishcloth, row 24. Three sessions over 5 days, averaging 8 rows per session.

(No yarn compatibility note because the yarns are similar enough.)

**No pattern or no yarn linked — no mention:**
> Project 4, row 24. Three sessions totaling 47 minutes over 5 days.

(Nothing to compare.)

**Only yarn linked, no pattern:**
> Corner To Corner Dishcloth in Novita Nalle, row 24. Steady pace at 8 rows per session over 5 days.

(Mentions the yarn but no compatibility note since there's nothing to compare against.)

---

## 5. Implementation

### 5.1 Gather linked yarn data

```kotlin
// Already available if yarn is linked to project
val linkedYarn = yarnCardDao.getYarnCardById(project.linkedYarnCardId)
```

Build a yarn info string for the prompt:
```kotlin
val yarnInfo = if (linkedYarn != null) {
    buildString {
        append("${linkedYarn.brand} ${linkedYarn.name}")
        linkedYarn.weightCategory?.let { append(", $it") }
        linkedYarn.fiberContent?.let { append(", $it") }
        if (linkedYarn.metersPerSkein != null && linkedYarn.gramsPerSkein != null) {
            append(", ${linkedYarn.metersPerSkein}m/${linkedYarn.gramsPerSkein}g")
        }
        linkedYarn.recommendedNeedle?.let { append(", needle $it") }
        linkedYarn.gaugeStitches?.let { append(", gauge $it sts/10cm") }
    }
} else "not set"
```

### 5.2 Update prompt template

Add the yarn compatibility instruction and yarn data fields to the existing prompt. No other code changes needed.

---

## 6. Files to Modify

| File | Change |
|------|--------|
| ProjectSummarizer.kt (or equivalent) | Add yarn data to prompt, add compatibility instruction to prompt template |

## 7. Files NOT Modified

| File | Reason |
|------|--------|
| Everything else | Prompt-only change with existing data |

---

## 8. Edge Cases

| Scenario | Behavior |
|----------|----------|
| No yarn linked | No compatibility note in summary |
| No pattern linked | No compatibility note |
| Both linked but same/similar yarn | No compatibility note — AI recognizes they match |
| Different weight category | Summary mentions the difference and suggests checking gauge |
| Same weight, different fiber | Summary mentions briefly, notes texture may differ |
| Yarn data incomplete (no meters/grams) | AI works with what's available, skips comparison it can't make |
| Multiple yarns linked | Use the first/primary linked yarn for comparison |

---

## 9. Testing

- [ ] Project with mismatched pattern yarn and linked yarn → summary mentions compatibility
- [ ] Project with compatible yarns → summary does NOT mention compatibility
- [ ] Project with no yarn linked → no compatibility note
- [ ] Project with no pattern → no compatibility note
- [ ] Compatibility note is one sentence, factual, not alarming

---

## 10. Success Criteria

1. No new UI elements
2. Yarn mismatches are flagged naturally within the existing summary
3. Compatible yarns produce no unnecessary warnings
4. Practical — mentions gauge adjustment, not just "these are different"
