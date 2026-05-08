# KnitTools — Yarn Label Scanner with Gemini

**Type:** Delta spec — replace OCR+Nano pipeline with multimodal Gemini, expand functionality  
**Status:** Spec ready for implementation  
**Depends on:** Firebase AI Logic setup (GeminiAiService, AiQuotaManager)  
**Pro only:** Yes  
**Key improvement:** Image goes directly to Gemini — no OCR intermediate step

---

## 1. What Changes

The yarn label scanner currently works as: Camera → photo → ML Kit OCR → text → Nano cleanup → parsed fields. This pipeline produces poor results because OCR struggles with yarn labels (curved surfaces, small text, colored backgrounds, multilingual content).

New pipeline: Camera → photo → Gemini Flash Lite → structured data. One step instead of three. Gemini sees the label as a human would and extracts all information directly.

### What stays the same
- Camera capture flow (existing CameraX implementation)
- Review screen concept (user reviews parsed data before saving)
- YarnCardEntity structure in Room
- Yarn Estimator field pre-fill behavior

### What changes
- **Pipeline:** OCR+Nano → direct multimodal Gemini call
- **Data extracted:** Much more — all fields a yarn label can contain, not just estimator fields
- **Entry points:** Yarn Estimator camera icon (existing) + new scan option in My Yarn
- **After scan:** User can save to My Yarn AND/OR fill Yarn Estimator, not just one or the other
- **Gating:** `ProState.hasFeature(AI_FEATURES) && aiQuotaManager.hasQuota()` 
- **Availability:** Works on all devices with internet, not just Nano-capable

---

## 2. What Gemini Extracts from a Yarn Label

A typical yarn label contains some or all of these:

| Field | Example | Maps to |
|-------|---------|---------|
| Brand | Drops, Novita, Schachenmayr | YarnCardEntity.brand |
| Yarn name | Alpaca, Nalle, Catania | YarnCardEntity.name |
| Fiber content | 100% Wool, 75% Acrylic 25% Wool | YarnCardEntity.fiberContent |
| Weight category | Fingering, DK, Worsted, Bulky | YarnCardEntity.weightCategory |
| Meters per skein | 167m | YarnCardEntity.metersPerSkein + Yarn Estimator field |
| Yards per skein | 183yds | Convert to meters if needed |
| Grams per skein | 50g | YarnCardEntity.gramsPerSkein + Yarn Estimator field |
| Recommended needle size | 4mm / US 6 | YarnCardEntity.recommendedNeedle |
| Gauge (stitches/10cm) | 22 sts | YarnCardEntity.gaugeStitches |
| Gauge (rows/10cm) | 30 rows | YarnCardEntity.gaugeRows |
| Color name | Natural White | YarnCardEntity.colorName |
| Color number | 0100 | YarnCardEntity.colorNumber |
| Dye lot | 2847 | YarnCardEntity.dyeLot |
| Care instructions | Machine wash 30°, do not tumble dry | YarnCardEntity.careInstructions |

Not all labels have all fields. Gemini returns what it can read, and missing fields stay empty.

---

## 3. Prompt Design

```
You are a yarn label reader. Analyze this photo of a yarn label and extract all visible information.

Return ONLY a JSON object with these fields. Use null for any field not visible on the label. Do not guess or infer values that are not printed on the label.

{
  "brand": "string or null",
  "name": "string or null",
  "fiberContent": "string or null",
  "weightCategory": "one of: LACE, FINGERING, SPORT, DK, WORSTED, ARAN, BULKY, SUPER_BULKY or null",
  "metersPerSkein": "number or null",
  "gramsPerSkein": "number or null",
  "recommendedNeedleMm": "number or null",
  "gaugeStitches": "number or null (stitches per 10cm)",
  "gaugeRows": "number or null (rows per 10cm)",
  "colorName": "string or null",
  "colorNumber": "string or null",
  "dyeLot": "string or null",
  "careInstructions": "string or null"
}

If the image does not show a yarn label, return: {"error": "not_a_yarn_label"}

Important:
- If the label shows yards instead of meters, convert to meters (1 yard = 0.9144 meters) and return meters
- For weight category, infer from needle size and meters/gram if not explicitly stated
- Preserve the original brand and color names exactly as printed
- Do not translate anything
```

### Example response

```json
{
  "brand": "Drops",
  "name": "Alpaca",
  "fiberContent": "100% Alpaca",
  "weightCategory": "DK",
  "metersPerSkein": 167,
  "gramsPerSkein": 50,
  "recommendedNeedleMm": 4.0,
  "gaugeStitches": 22,
  "gaugeRows": 30,
  "colorName": "Natural White",
  "colorNumber": "0100",
  "dyeLot": "2847",
  "careInstructions": "Hand wash 30°C, dry flat"
}
```

---

## 4. User Flow

### 4.1 From Yarn Estimator (existing entry point)

1. User taps camera icon in Yarn Estimator
2. Camera opens, user takes photo of yarn label
3. Loading indicator while Gemini processes
4. Review screen appears with all extracted fields
5. User can edit any field
6. Two actions at bottom:
   - **"Fill Estimator"** — fills meters per skein and grams per skein into Yarn Estimator, returns to Yarn Estimator
   - **"Save to My Yarn"** — saves full Yarn Card to My Yarn collection
   - Both can be done (save first, then fill)

### 4.2 From My Yarn (new entry point)

1. My Yarn screen gets a FAB or "Scan label" button
2. Camera opens, user takes photo
3. Same review screen as above
4. One primary action: **"Save to My Yarn"**
5. After save, user lands back on My Yarn list with the new card visible

### 4.3 Review Screen

The review screen (YarnCardReview) already exists. It needs to be updated to:
- Display all new fields (currently may only show a subset)
- Pre-fill from Gemini response
- Allow editing every field before save
- Show "not detected" in muted text for fields Gemini returned null

---

## 5. Implementation

### 5.1 New scan method in GeminiAiService

```kotlin
suspend fun scanYarnLabel(image: Bitmap): YarnLabelResult? {
    val prompt = buildYarnLabelPrompt()
    val response = generateWithImage(prompt, image)
    return parseYarnLabelJson(response)
}
```

### 5.2 Replace existing scanner flow

The existing camera → OCR → Nano pipeline is replaced entirely. The ML Kit Text Recognition call and YarnLabelNanoParser are no longer used for this feature.

```kotlin
// Before
val ocrText = mlKitTextRecognizer.process(bitmap)
val cleaned = yarnLabelNanoParser.parse(ocrText)

// After
val result = geminiAiService.scanYarnLabel(bitmap)
```

### 5.3 Map Gemini response to existing entities

```kotlin
fun YarnLabelResult.toYarnCardEntity(): YarnCardEntity {
    return YarnCardEntity(
        brand = brand ?: "",
        name = name ?: "",
        fiberContent = fiberContent,
        weightCategory = weightCategory,
        metersPerSkein = metersPerSkein,
        gramsPerSkein = gramsPerSkein,
        recommendedNeedle = recommendedNeedleMm?.toString(),
        gaugeStitches = gaugeStitches,
        gaugeRows = gaugeRows,
        colorName = colorName,
        colorNumber = colorNumber,
        dyeLot = dyeLot,
        careInstructions = careInstructions,
        // existing fields
        quantityInStash = 1,
        status = YarnStatus.IN_STASH
    )
}
```

### 5.4 Add scan entry point to My Yarn

Add a FAB or top bar action on the My Yarn list screen. Same camera flow, same review screen, but navigation returns to My Yarn instead of Yarn Estimator after save.

### 5.5 Quota counting

One AI call per scan. Counted on successful Gemini response, not on camera capture.

---

## 6. YarnCardEntity Changes

Check if these fields already exist on YarnCardEntity. If any are missing, add them with a Room migration:

| Field | Type | Currently exists? |
|-------|------|-------------------|
| brand | String | Yes |
| name | String | Yes |
| fiberContent | String? | Check |
| weightCategory | String? | Yes |
| metersPerSkein | Int? | Check |
| gramsPerSkein | Int? | Check |
| recommendedNeedle | String? | Check |
| gaugeStitches | Int? | Check |
| gaugeRows | Int? | Check |
| colorName | String? | Check |
| colorNumber | String? | Check |
| dyeLot | String? | Check |
| careInstructions | String? | Check |

**Important:** Claude Code must check the current YarnCardEntity fields before implementing. Only add migration if new columns are needed. Do not add columns that already exist.

---

## 7. Files to Modify

| File | Change |
|------|--------|
| Existing scanner class (YarnLabelScanner or equivalent) | Replace OCR+Nano with GeminiAiService.scanYarnLabel() |
| GeminiAiService.kt | Add scanYarnLabel() method with multimodal prompt |
| YarnCardReview screen | Display all extracted fields, allow editing |
| Yarn Estimator screen | Keep camera icon, update to use new scanner flow |
| My Yarn screen | Add scan entry point (FAB or action) |
| Navigation | Ensure review screen is reachable from both Yarn Estimator and My Yarn |
| YarnCardEntity (if needed) | Add missing fields + Room migration |

## 8. Files to Leave Alone

| File | Reason |
|------|--------|
| YarnLabelNanoParser | Keep file, stop using it from scanner flow. May delete later. |
| ML Kit Text Recognition setup | Still used by other features (Pattern Viewer until migrated) |
| Yarn Estimator calculation logic | Unrelated — only the field pre-fill source changes |

---

## 9. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Photo is not a yarn label | Gemini returns error → show "Could not read a yarn label from this photo. Try again." |
| Photo is blurry | Gemini extracts what it can, missing fields show as empty |
| Label is in Japanese/Finnish/German | Gemini reads it natively, preserves original text |
| Label shows yards not meters | Gemini converts to meters per prompt instruction |
| User takes photo of multiple skeins | Gemini reads the most prominent label. Unpredictable — acceptable for v1. |
| No internet | "Scanning requires internet connection" |
| Quota exhausted | "Monthly AI limit reached" |
| Weight category not on label | Gemini infers from needle size + meters/gram, or returns null |

---

## 10. Testing

### Scanner pipeline
- [ ] Take photo of yarn label → review screen shows parsed data
- [ ] All visible fields from label appear in review
- [ ] Missing fields show as empty, not hallucinated values
- [ ] Photo of non-label shows error message
- [ ] Works on device without Nano support

### Entry points
- [ ] Yarn Estimator camera icon → scan → review → "Fill Estimator" fills fields correctly
- [ ] Yarn Estimator camera icon → scan → review → "Save to My Yarn" saves card
- [ ] My Yarn scan button → scan → review → "Save to My Yarn" saves card and returns to list

### Data integrity
- [ ] Saved Yarn Card appears in My Yarn with correct fields
- [ ] Yarn Card detail screen shows all new fields
- [ ] Editing fields on review screen before save preserves edits

---

## 11. Success Criteria

1. Scanning a yarn label produces significantly better results than current OCR+Nano pipeline
2. All visible label information is captured, not just estimator fields
3. Users can scan and save yarn cards from both Yarn Estimator and My Yarn
4. This is the first multimodal (image) AI feature, validating image sending through Firebase AI Logic
