# KnitTools v3.0 Delta — Toteutussuunnitelma

## Context

KnitTools v3.0 tuo kolme isoa ominaisuutta: PDF Pattern Viewer rivien seurannalla, silmukkatason seuranta (stitch tracking) ja eksplisiittinen kertausseurant (repeat section). Kaikki rakentuvat Room-migraation 6→7 päälle. Spec: `knittools-v3-delta-spec.md`.

---

## Vaihe 1: Room-migraatio 6→7

Kaikki v3-ominaisuudet vaativat uudet sarakkeet/taulut. Tämä tehdään ensin.

### 1.1 CounterProjectEntity — 6 uutta saraketta
**Tiedosto:** `app/src/main/java/com/finnvek/knittools/data/local/CounterProjectEntity.kt`

Lisätään `linkedPatternId`-kentän jälkeen:
```kotlin
@ColumnInfo(defaultValue = "NULL") val patternUri: String? = null,
@ColumnInfo(defaultValue = "NULL") val patternName: String? = null,
@ColumnInfo(defaultValue = "0")    val currentPatternPage: Int = 0,
@ColumnInfo(defaultValue = "NULL") val patternRowMapping: String? = null,
@ColumnInfo(defaultValue = "0")    val stitchTrackingEnabled: Boolean = false,
@ColumnInfo(defaultValue = "0")    val currentStitch: Int = 0,
```

### 1.2 ProjectCounterEntity — 4 uutta saraketta
**Tiedosto:** `app/src/main/java/com/finnvek/knittools/data/local/ProjectCounterEntity.kt`

Lisätään `shapeEveryN`-kentän jälkeen:
```kotlin
@ColumnInfo(defaultValue = "NULL") val repeatStartRow: Int? = null,
@ColumnInfo(defaultValue = "NULL") val repeatEndRow: Int? = null,
@ColumnInfo(defaultValue = "NULL") val totalRepeats: Int? = null,
@ColumnInfo(defaultValue = "NULL") val currentRepeat: Int? = null,
```

### 1.3 PatternAnnotationEntity + DAO (uudet tiedostot)
**Uusi:** `data/local/PatternAnnotationEntity.kt`
```kotlin
@Entity(tableName = "pattern_annotations",
    foreignKeys = [ForeignKey(CounterProjectEntity::class, ["id"], ["projectId"], CASCADE)],
    indices = [Index("projectId")])
data class PatternAnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long, val page: Int,
    val pathData: String, val color: String,
    val strokeWidth: Float, val createdAt: Long = System.currentTimeMillis())
```

**Uusi:** `data/local/PatternAnnotationDao.kt`
- `getAnnotationsForPage(projectId, page): Flow<List<…>>`
- `insert(annotation): Long`
- `deleteForProject(projectId)`
- `deleteById(id)`

### 1.4 KnitToolsDatabase
**Tiedosto:** `data/local/KnitToolsDatabase.kt`

- Lisää `PatternAnnotationEntity::class` entities-listaan (9. entity)
- `version = 7`
- Uusi `abstract fun patternAnnotationDao(): PatternAnnotationDao`
- Uusi `MIGRATION_6_7` companion-objektiin — 12 SQL-lausetta specin mukaan:
  - 6× ALTER TABLE counter_projects
  - 4× ALTER TABLE project_counters
  - CREATE TABLE pattern_annotations
  - CREATE INDEX index_pattern_annotations_projectId

### 1.5 DatabaseModule
**Tiedosto:** `di/DatabaseModule.kt`

- Lisää `MIGRATION_6_7` addMigrations-kutsuun
- Uusi `@Provides fun providePatternAnnotationDao(db): PatternAnnotationDao`

### 1.6 Migraatiotestit
**Tiedosto:** `app/src/androidTest/java/com/finnvek/knittools/data/local/MigrationTest.kt`

- `migrate6to7()`: Luo v6-data, aja migraatio, varmista oletusarvot + taulun olemassaolo
- `migrate1to7()`: Täysi ketjutesti v1→v7
- Päivitä `allMigrations`-lista

---

## Vaihe 2: Silmukkatason seuranta (Stitch Tracking)

Yksinkertaisin ominaisuus — kompakti UI-elementti CounterScreeniin.

### 2.1 CounterProjectDao — uudet kyselyt
**Tiedosto:** `data/local/CounterProjectDao.kt`

```kotlin
@Query("UPDATE counter_projects SET currentStitch = :stitch, updatedAt = :updatedAt WHERE id = :id")
suspend fun updateCurrentStitch(id: Long, stitch: Int, updatedAt: Long)

@Query("UPDATE counter_projects SET stitchTrackingEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
suspend fun updateStitchTrackingEnabled(id: Long, enabled: Boolean, updatedAt: Long)
```

### 2.2 CounterRepository — uudet metodit
**Tiedosto:** `repository/CounterRepository.kt`

- `updateCurrentStitch(id, stitch)` → delegoi DAO:lle
- `updateStitchTrackingEnabled(id, enabled)` → delegoi DAO:lle

### 2.3 CounterUiState + CounterViewModel
**Tiedosto:** `ui/screens/counter/CounterViewModel.kt`

Lisää `CounterUiState`:iin:
- `stitchTrackingEnabled: Boolean = false`
- `currentStitch: Int = 0`

Uudet funktiot:
- `incrementStitch()` — +1, cap stitchCount:iin, persistoi
- `decrementStitch()` — -1, floor 0, persistoi
- `setStitchTrackingEnabled(enabled)` — toggle, persistoi

Muokkaa `increment()`:a: kun stitchTrackingEnabled, resetoi currentStitch → 0 rivin vaihtuessa.

Muokkaa project-observation (observeSelectedProject/selectProject) lukemaan uudet kentät entitystä.

### 2.4 StitchCounter-komponentti (uusi)
**Uusi:** `ui/components/StitchCounter.kt`

Kompakti rivi: `◄ 23/120 ►`
- Parametrit: currentStitch, totalStitches, onIncrement, onDecrement
- SurfaceVariant-tausta, RoundedCornerShape(16.dp)
- Kun currentStitch == totalStitches → Primary-highlight

### 2.5 CounterScreen-muutokset
**Tiedosto:** `ui/screens/counter/CounterScreen.kt`

1. **StitchCounter** lisätään PatternRepeatPill-blokin jälkeen (rivi ~396), ennen CounterButtons:ia:
   ```kotlin
   if (state.stitchTrackingEnabled) {
       StitchCounter(currentStitch, stitchCount, onIncrement, onDecrement)
   }
   ```

2. **ProjectInfoCard**:iin lisätään "Track stitches" -switch kun stitchCount != null

### 2.6 Äänikomennot
**Tiedosto:** `ui/screens/counter/VoiceCommandHandler.kt`

- Uudet enum-arvot: `STITCH_INCREMENT`, `STITCH_DECREMENT`
- Sanat: "stitch"/"next stitch"/"silmukka"/"seuraava silmukka" ja "back stitch"/"edellinen silmukka"
- **Tärkeää:** Tarkista stitch-komennot ENNEN yleisiä increment/decrement-komentoja `parseCommand()`:ssa (koska "next stitch" sisältää "next")
- CounterScreenissä: käsittele uudet komennot viewModel.incrementStitch()/decrementStitch()

### 2.7 Pro-gating
- Stitch tracking itsessään: **Free**
- Voice-komennot silmukoille: olemassa oleva `ProFeature.VOICE_COMMANDS`
- Ei uusia ProFeature-arvoja tässä vaiheessa

---

## Vaihe 3: Eksplisiittinen kertausseuranta (Repeat Section)

Uusi laskurityyppi REPEAT_SECTION. Pro-ominaisuus.

### 3.1 RepeatSectionLogic (uusi)
**Uusi:** `domain/calculator/RepeatSectionLogic.kt`

```kotlin
object RepeatSectionLogic {
    fun updatePosition(counter: ProjectCounterEntity, mainRowCount: Int): ProjectCounterEntity
    fun isComplete(counter: ProjectCounterEntity): Boolean
    fun currentRowInRepeat(counter: ProjectCounterEntity, mainRowCount: Int): Int
    fun progress(counter: ProjectCounterEntity, mainRowCount: Int): Float
}
```

Logiikka: rowRange = endRow - startRow + 1. Repeat# = (mainRow - startRow) / rowRange + 1. RowInRepeat = (mainRow - startRow) % rowRange + startRow. Cap totalRepeats.

### 3.2 ProjectCounterLogic — REPEAT_SECTION
**Tiedosto:** `domain/calculator/ProjectCounterLogic.kt`

Lisää REPEAT_SECTION-käsittely: ei resettaa itse, vaan palauttaa counterin sellaisenaan (sitä ohjaa päärivilaskuri).

### 3.3 ProjectCounterDao — uusi kysely
**Tiedosto:** `data/local/ProjectCounterDao.kt` (tai olemassa oleva ProjectCounterDao)

```kotlin
@Query("UPDATE project_counters SET count = :count, currentRepeat = :currentRepeat WHERE id = :id")
suspend fun updateRepeatSectionState(id: Long, count: Int, currentRepeat: Int)
```

### 3.4 AddCounterDialog — 4. tyyppi
**Tiedosto:** `ui/screens/counter/MultiCounterComponents.kt`

1. SegmentedToggle: 3 → 4 vaihtoehtoa (Count up, Repeating, Shaping, **Repeat section**)
2. `selectedType == 3` → näytä 3 kenttää: Start row, End row, Total repeats
3. `counterTypeFromIndex`: index 3 → "REPEAT_SECTION"
4. `isAddCounterFormValid`: lisää repeat section -validointi (startRow < endRow, totalRepeats > 0)
5. **onSave-callback laajennus**: lisää repeatStartRow, repeatEndRow, totalRepeats, currentRepeat parametrit

### 3.5 CounterDialogActions + ViewModel
**Tiedostot:** `CounterScreen.kt` (CounterDialogActions), `CounterViewModel.kt`

- `onAddCounterSave`-signatuuri laajenee 4 uudella parametrillä
- `addProjectCounter()` ottaa vastaan uudet kentät, luo ProjectCounterEntity

### 3.6 RepeatSectionItem-komponentti
**Tiedosto:** `ui/screens/counter/MultiCounterComponents.kt` (sama tiedosto)

Uusi `RepeatSectionItem` composable:
```
Sleeve increases
  Repeat 3 of 5 · Row 4 of 6
  ████████░░░░░░  (LinearProgressIndicator)
```
- Käyttää RepeatSectionLogic:ia päärivilaskurin perusteella
- Complete-tila: ✓-ikoni

### 3.7 Counter-lista CounterScreeniin
**Tiedosto:** `ui/screens/counter/CounterScreen.kt`

Lisää CounterButtons:in jälkeen (ennen `Spacer(16.dp)`):
```kotlin
if (state.isPro && state.projectCounters.isNotEmpty()) {
    // Jokainen counter: REPEAT_SECTION → RepeatSectionItem, muut → CounterListItem
}
// + Add Counter FAB/nappi
```

### 3.8 Auto-advance logiikka
**Tiedosto:** `ui/screens/counter/CounterViewModel.kt`

`increment()`- ja `decrement()`-funktioissa: päärivin muuttuessa iteroi REPEAT_SECTION-laskurit, laske `RepeatSectionLogic.updatePosition()` ja persistoi `updateRepeatSectionState()`.

### 3.9 ProFeature
**Tiedosto:** `pro/ProState.kt`

Lisää: `REPEAT_SECTION`

Gate AddCounterDialog:ssa: Repeat section -vaihtoehto näkyy vain Pro-käyttäjille.

---

## Vaihe 4: PDF Pattern Viewer

Monimutkaisin ominaisuus. Jaettu alivaiheisiin.

### 4A: Navigaatio ja pattern-liitos

**Screen.kt** — uusi reitti:
```kotlin
data class PatternViewer(val projectId: Long) : Screen("pattern_viewer/$projectId") {
    companion object { const val ROUTE = "pattern_viewer/{projectId}" }
}
```

**NavGraph.kt** — lisää projectsGraph:iin Counter- ja PhotoGallery-composable:n rinnalle:
```kotlin
composable(Screen.PatternViewer.ROUTE, arguments = [...]) {
    val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
    PatternViewerScreen(viewModel = counterViewModel, onBack = { navController.popBackStack() })
}
```
- Lisää `Screen.PatternViewer.ROUTE` HIDE_BOTTOM_BAR_ROUTES:iin
- CounterScreen saa uuden `onPatternViewer: (Long) -> Unit` callbackin

**CounterProjectDao** — uudet kyselyt:
```kotlin
updatePattern(id, uri, name, updatedAt)
updateCurrentPatternPage(id, page, updatedAt)
updatePatternRowMapping(id, mapping, updatedAt)
```

**CounterRepository** — vastaavat wrapper-metodit

**CounterViewModel** — uudet funktiot:
- `attachPattern(uri, name)`, `detachPattern()`, `updatePatternPage(page)`, `updatePatternRowMapping(mapping)`
- Lisää patternUri/patternName/currentPatternPage CounterUiState:iin

**CounterScreen (ProjectHeader)** — pattern-linkin UI:
- Ei patternia → "Attach pattern" (Tertiary) → avaa PatternPickerSheet
- Pattern liitetty → patternin nimi (klikattava → viewer) + × detach
- Pattern liitetty → "Show pattern" chip laskurialueen alla

### 4B: PatternPickerSheet (uusi)
**Uusi:** `ui/screens/pattern/PatternPickerSheet.kt`

ModalBottomSheet kolmella lähteellä:
1. **Saved Patterns** — lista SavedPatternEntity:stä
2. **Device files** — `ActivityResultContracts.OpenDocument("application/pdf")` + persistable URI
3. **Camera scan** — Pro only, CameraX → PDF (stretch goal)

### 4C: PDF-renderöinti
**Uusi:** `ui/screens/pattern/PdfPageRenderer.kt`

Wrapper `android.graphics.pdf.PdfRenderer`:lle (API 21+):
- `openPdf(context, uri)` → avaa contentResolver:lla
- `renderPage(pageIndex, width): Bitmap`
- `pageCount: Int`
- Lifecycle management (close Closeable)

### 4D: PatternViewerScreen (uusi)
**Uusi:** `ui/screens/pattern/PatternViewerScreen.kt`

Layout:
- **TopAppBar:** ← + pattern-nimi (truncated) + overflow (annotation, page jump, detach)
- **PDF-alue:** Pystysuuntaan scrollattava, renderöidyt sivut (LazyColumn/HorizontalPager)
- **Row highlight overlay:** semi-transparent Primary-bändi aktiivisella rivillä
- **Bottom bar:** Kelluva palkki: rivinumero (synced Counter) + ±/rivinapit + "Page X of Y"

Käyttää jaettua CounterViewModel:ia (sama scoping kuin PhotoGallery).

### 4E: Row highlight + mapping
**Uusi:** `ui/screens/pattern/RowHighlightOverlay.kt`

Canvas-composable: piirtää semi-transparent bändin rivin kohtaan. Interpoloi markereiden välillä.

**Uusi:** `domain/calculator/RowMappingParser.kt`
```kotlin
data class RowMarker(val row: Int, val page: Int, val yPosition: Float)
fun parseMapping(json: String?): List<RowMarker>
fun serializeMapping(markers: List<RowMarker>): String
fun interpolateYPosition(markers, targetRow, page): Float?
```

Manual mapping: long-press PDF:llä → asettaa markerin. Tallennetaan JSON:na patternRowMapping-kenttään.

### 4F: Auto row mapping — Nano (Pro)
**Uusi:** `ai/nano/PatternRowDetector.kt`

- InstructionParser-pattern: Nano + regex fallback
- Input: PDF-sivun teksti
- Output: `RowMarkerSuggestion(row, page, yPosition, confidence)`
- Confidence ≥ 0.7 → auto-accept, alle → suggested (katkoviiva)
- Regex fallback: "Row 1:", "Rnd 1:", numeroitu chart

### 4G: Annotation overlay (Pro)
**Uusi:** `ui/screens/pattern/AnnotationOverlay.kt`

- `Modifier.pointerInput` piirtämiselle
- Tools: pen (Primary), highlighter (Secondary 40% opacity), eraser
- Path-data serialisoidaan JSON:ksi

**Uusi:** `repository/PatternAnnotationRepository.kt`
- Wrappaa PatternAnnotationDao
- DI: injektoidaan Hiltilla (DAO jo providerissa vaiheesta 1.5)

### 4H: ProFeature-laajennukset
**Tiedosto:** `pro/ProState.kt`

Lisää:
```kotlin
PATTERN_ANNOTATION,
PATTERN_AUTO_MAPPING,
PATTERN_CAMERA_SCAN,
```

### 4I: String-resurssit
**Tiedosto:** `res/values/strings.xml`

Kaikista vaiheista tarvittavat stringit (pattern viewer, stitch tracking, repeat section).

---

## Toteutusjärjestys

```
Vaihe 1: Migraatio 6→7          ← ENSIN, kaiken pohja
    │
    ├── Vaihe 2: Stitch Tracking ← yksinkertaisin, voi aloittaa heti
    │
    ├── Vaihe 3: Repeat Section  ← itsenäinen vaiheesta 2
    │
    └── Vaihe 4A: Nav + Attach   ← itsenäinen
         │
         ├── 4B: PatternPickerSheet
         ├── 4C: PdfPageRenderer
         ├── 4D: PatternViewerScreen
         ├── 4E: Row highlight + mapping
         ├── 4F: Auto mapping (Nano)
         └── 4G: Annotation overlay
```

---

## Tiedostoyhteenveto

### Uudet tiedostot (13)
| # | Tiedosto | Vaihe |
|---|----------|-------|
| 1 | `data/local/PatternAnnotationEntity.kt` | 1 |
| 2 | `data/local/PatternAnnotationDao.kt` | 1 |
| 3 | `ui/components/StitchCounter.kt` | 2 |
| 4 | `domain/calculator/RepeatSectionLogic.kt` | 3 |
| 5 | `domain/calculator/RowMappingParser.kt` | 4E |
| 6 | `ui/screens/pattern/PatternViewerScreen.kt` | 4D |
| 7 | `ui/screens/pattern/PatternPickerSheet.kt` | 4B |
| 8 | `ui/screens/pattern/PdfPageRenderer.kt` | 4C |
| 9 | `ui/screens/pattern/RowHighlightOverlay.kt` | 4E |
| 10 | `ui/screens/pattern/AnnotationOverlay.kt` | 4G |
| 11 | `repository/PatternAnnotationRepository.kt` | 4G |
| 12 | `ai/nano/PatternRowDetector.kt` | 4F |

### Muokattavat tiedostot (15)
| # | Tiedosto | Vaihe |
|---|----------|-------|
| 1 | `data/local/CounterProjectEntity.kt` | 1 |
| 2 | `data/local/ProjectCounterEntity.kt` | 1 |
| 3 | `data/local/KnitToolsDatabase.kt` | 1 |
| 4 | `di/DatabaseModule.kt` | 1 |
| 5 | `data/local/CounterProjectDao.kt` | 2, 4A |
| 6 | `repository/CounterRepository.kt` | 2, 4A |
| 7 | `ui/screens/counter/CounterViewModel.kt` | 2, 3, 4A |
| 8 | `ui/screens/counter/CounterScreen.kt` | 2, 3, 4A |
| 9 | `ui/screens/counter/VoiceCommandHandler.kt` | 2 |
| 10 | `data/local/ProjectCounterDao.kt` | 3 |
| 11 | `domain/calculator/ProjectCounterLogic.kt` | 3 |
| 12 | `ui/screens/counter/MultiCounterComponents.kt` | 3 |
| 13 | `ui/navigation/Screen.kt` | 4A |
| 14 | `ui/navigation/NavGraph.kt` | 4A |
| 15 | `pro/ProState.kt` | 3, 4H |
| 16 | `androidTest/.../MigrationTest.kt` | 1 |

---

## Verification

Jokaisen vaiheen jälkeen:
1. `./gradlew assembleDebug` — kääntyy ilman virheitä
2. Migraatiotestit (vaihe 1): `./gradlew connectedAndroidTest` tai manuaalisesti MigrationTest
3. Stitch tracking (vaihe 2): avaa projekti, aseta stitchCount, ota tracking päälle → ◄/► toimii, rivin vaihto nollaa
4. Repeat section (vaihe 3): luo REPEAT_SECTION-laskuri, kasvata pääriviä → repeat-tila päivittyy
5. Pattern viewer (vaihe 4): liitä PDF, avaa viewer, scrollaa sivuja, aseta row markers, tarkista synkronointi laskurin kanssa
6. `./gradlew :app:detekt` ja `./gradlew lint` — ei uusia varoituksia
