# KnitTools

**Developer:** Finnvek (solo indie)
**Package:** `com.finnvek.knittools`
**Platform:** Android (Kotlin, Jetpack Compose, Material Design 3)
**SDK:** compileSdk 36, minSdk 29, targetSdk 36
**Version:** 1.0.0

---

## Konsepti

Neulontatyökalujen kokoelma yhdessä puhtaassa, kauniissa sovelluksessa. Projektinhallinta, laskurityökalut, lankakorttien skannaus ja referenssimateriaalit.

**Filosofia:**
- Kaunis, rauhallinen UI — warm premium craft aesthetic
- Ei mainoksia, ei tilauksia — kertamaksu €1.99 (Pro)
- Privacy-first: nolla analytiikkaa, nolla datankeruuta
- Offline-first: kaikki toimii ilman nettiä

---

## Navigaatio

M3 NavigationBar 3 välilehdellä:

| Välilehti | Ikoni | Sisältö |
|-----------|-------|---------|
| **Projects** | FolderOpen | Row Counter (root), Project List, Session History |
| **Tools** | Build | Tools List (root, default), Gauge, Increase/Decrease, Cast On, Yarn Estimator, Yarn Card Review/List/Detail |
| **Reference** | LibraryBooks | Reference Hub → Needle Sizes, Size Charts, Abbreviations, Chart Symbols |

`TopLevelDestination`-enum `Screen.kt`:ssä (3 entries). Nested NavGraphs per tab. Tools on oletusaloitussivu.

### Reitit (18 kpl)

| Näyttö | Route | Välilehti |
|--------|-------|-----------|
| Tools List | `tools` | Tools (root) |
| Gauge Converter | `gauge` | Tools |
| Increase/Decrease | `increase_decrease` | Tools |
| Cast On Calculator | `cast_on` | Tools |
| Yarn Estimator | `yarn` | Tools |
| Yarn Card Review | `yarn_card_review` | Tools |
| Yarn Card List | `yarn_card_list` | Tools |
| Yarn Card Detail | `yarn_card_detail/{cardId}` | Tools |
| Row Counter | `counter` | Projects (root) |
| Project List | `project_list` | Projects |
| Session History | `session_history/{projectId}` | Projects |
| Reference Hub | `reference` | Reference (root) |
| Needle Sizes | `needles` | Reference |
| Size Charts | `size_charts` | Reference |
| Abbreviations | `abbreviations` | Reference |
| Chart Symbols | `chart_symbols` | Reference |
| Settings | `settings` | Globaali |
| Pro Upgrade | `pro_upgrade` | Globaali |

---

## Työkalut

### Row Counter (Projects-välilehti)
- **Projektikortti:** Kompakti surface-kortti. CURRENT PROJECT label + sparkle-ikoni (AI summary, Pro) + chevron (>) → Project List. Projektin nimi (tap = rename). "+ Add yarn" + note-ikoni kortin sisällä.
- **Linkitetyt langat:** Kortin sisällä, ×-ikonilla poistettavissa. "+ Add yarn" → BottomSheet yarn picker tallennetuista korteista.
- **Notes:** Note-ikoni projektikortin sisällä → ModalBottomSheet (minLines=6)
- **Laskuri:** Iso RollingCounter (96sp ExtraBold), TOTAL ROWS label yläpuolella
- **Pattern repeat:** Pro, kompakti pill/chip laskurin alla (−/+ napit pillin sisällä)
- **Napit:** − (48dp outlined muted) / + (72dp, light: gold fill + cream icon, dark: cream fill + dark icon) / undo (48dp outlined muted)
- **Stats-rivi:** STITCHES + TIME vierekkäin nappien alla, surfaceVariant-tausta. TIME tappable → Session History
- **Reset Counter:** Muted bodySmall stats-rivin alla
- **Sessiot:** Tallennetaan DB:hen kun ViewModel tuhoutuu tai projekti vaihtuu
- **AI Summary:** Sparkle-ikoni → Nano generoi projektiyhteenvedon BottomSheetiin (fallback: simpleSummary ilman Nanoa)

### Project List
- ACTIVE / COMPLETED osiot (labelSmall dusty rose)
- ProjectCard: nimi, rivimäärä (gold), viimeisin päivitys, chevron
- **Long-press context menu:** Active: Rename / Archive / Delete. Completed: Rename / Reactivate / Delete
- **Rename-dialogi:** AlertDialog + TextField
- **Delete-vahvistus:** AlertDialog error-värisellä confirm-napilla
- **Swipe-to-dismiss:** Active → archive, Completed → delete (vahvistus)
- FAB "+ New Project"

### Session History
- SessionItem-kortit: päivämäärä, kesto, riviväli (Rows X → Y)
- Free: 24h historia, Pro: rajaton
- Pro-hint banner free-käyttäjille

### Tools List (Tools-välilehti)
- Suora laskimilista HubListItem-komponenteilla: Gauge Converter, Increase/Decrease, Cast On Calculator, Yarn Estimator
- QuickTipCard (25 vinkkiä, satunnaisvalinta, piilotettavissa Settingsistä)
- Pro trial -banneri

### Calculators
- **Gauge Converter:** Swatch-mittaus (width, stitches, height, rows) + gauge-muunnos + Paste Instruction (Pro)
- **Increase/Decrease:** Tasainen jakaminen (easy + balanced) + Paste Instruction (Pro)
- **Cast On:** Silmukamäärä leveydestä ja tiheydestä

### Yarn Estimator
- Langankulutuksen arviointi (total yarn, per skein, weight)
- Pro: kamera-OCR lankalapusta → Yarn Card Review → tallennus
- Tallennuksessa dialogi: "Link to [projekti]?"
- Yarn Card linkitys projektiin myös Counter-näkymästä

### Reference
- **Needle Sizes:** Hakutaulukko (Metric/US/UK/JP), 2.0–25mm
- **Size Charts:** 6 kategoriaa (Baby, Child, Women, Men, Head, Hand), XS–5XL naisille/miehille
- **Abbreviations:** 76 lyhennettä, haettava, laajennettavat kuvaukset, kategorioidut
- **Chart Symbols:** Kategorioidut symbolit (Basic, Decreases, Increases, Cables, Other)

---

## AI-ominaisuudet (Pro)

### ML Kit OCR — Yarn Label Scanner
`ai/ocr/YarnLabelScanner.kt` + `YarnLabelParser.kt` (regex fallback). On-device ML Kit Text Recognition v2.

### Gemini Nano (5 toteutettua ominaisuutta)
Kattava dokumentaatio: `GEMINI-NANO.md`

| Ominaisuus | Tiedosto | Kuvaus |
|------------|----------|--------|
| Instruction Parser (Inc/Dec) | `ai/nano/InstructionParser.kt` | Parsii increase/decrease-ohjeita kenttiin |
| Instruction Parser (Gauge) | `ai/nano/InstructionParser.kt` | Parsii gauge/tension-ohjeita kenttiin |
| Instruction Parser (Swatch) | `ai/nano/InstructionParser.kt` | Parsii swatch-mittauksia kenttiin |
| Yarn Label OCR Parser | `ai/nano/YarnLabelNanoParser.kt` | Siistii OCR-raakatekstin strukturoiduiksi kentiksi |
| Project Summary | `ai/nano/ProjectSummarizer.kt` | Generoi luettavan projektiyhteenvedon |

Kaikissa Nano-ominaisuuksissa: regex-fallback + typo-toleranssi (40+ korjausta). Piilotetaan kokonaan jos laite ei tue.

---

## Visuaalinen design

### Teemajärjestelmä
- **Värit keskitetysti:** `Color.kt` (värimäärittelyt) + `Theme.kt` (M3 color schemes + extended colors)
- **Extended colors:** `KnitToolsExtendedColors` — surfaceTint, secondaryOutline, onSurfaceMuted, brandWine, inactiveContent. Käytä `MaterialTheme.knitToolsColors`.
- **Ei hardcodattuja värejä** näyttökoodissa — kaikki teematokenien kautta

### Light theme
- **Background:** #F8F4F0 (lämmin beige) — kaikki Scaffoldit
- **Surface:** #FFFFFF — kortit, bottom nav
- **Primary:** Gold #C9A96E — interaktiiviset elementit
- **Secondary:** Dusty rose #B8908F — labelit, korostukset
- **Text:** #2A1E17 (onSurface), #8A7A6E (onSurfaceVariant)

### Dark theme
- **Background:** #1A1410 — syvä lämmin ruskea
- **Surface:** #231C16 — kortit, tonal hierarkia
- **Primary:** Gold #C9A96E — sama kuin light
- **Secondary:** #C4A0A0 — hieman vaaleampi dusty rose
- **Noise-tekstuuri:** 256×256 tileable, 2.5% opacity, Overlay blend mode

### Typografia
- **Fontti:** Manrope (variable font), 5 painoa (Normal, Medium, SemiBold, Bold, ExtraBold)
- **Type scale:** Type.kt, 12 M3-roolia. All-caps labelit `labelSmall` (11sp, letterSpacing 1.5sp)
- **Laskurinumero:** displayMedium.copy(96sp, ExtraBold) — ainoa inline override
- **Tabular digits:** `fontFeatureSettings = "tnum"` display-tyyleissä

---

## Monetisaatio

### Free
- Kaikki laskurityökalut toimivat täysin
- Yksi aktiivinen projekti, historia 24h
- Ei mainoksia

### Pro — kertamaksu €1.99
- Rajattomat projektit + täysi historia
- Muistiinpanot ja toissijaiset laskurit (pattern repeat)
- Yarn Label OCR + tallennetut lankakortit
- Gemini Nano -ominaisuudet (5 kpl)
- Kotinäyttöwidget (Glance)

### Reverse Trial
7 päivän hiljainen trial. Kotinäytöllä "Pro trial — X days left". Trialen jälkeen Pro-ominaisuudet lukittuvat pehmeällä kehotteella.

---

## Arkkitehtuuri

```
com.finnvek.knittools/
├── ai/
│   ├── ocr/           # ML Kit OCR (YarnLabelScanner, YarnLabelParser)
│   └── nano/          # Gemini Nano (InstructionParser, YarnLabelNanoParser,
│                      #   ProjectSummarizer, NanoAvailability)
├── billing/           # Google Play Billing
├── data/
│   ├── datastore/     # DataStore (PreferencesManager, AppPreferences, ThemeMode)
│   └── local/         # Room v3: 4 entities, 3 DAOs, AutoMigrations
├── di/                # Hilt modules (DatabaseModule)
├── domain/
│   ├── calculator/    # 6 laskuria + 4 referenssidatatiedostoa
│   └── model/         # Domain-mallit
├── pro/               # ProManager, ProState, ProFeature, TrialManager
├── repository/        # CounterRepository, YarnCardRepository
├── ui/
│   ├── components/    # 17 jaettua komponenttia + 3 care-symboli
│   ├── navigation/    # Screen (18 reittiä), TopLevelDestination (3 tabia),
│   │                  #   KnitToolsNavHost, KnitToolsBottomBar
│   ├── screens/       # 16 näyttöä, 7 ViewModelia
│   └── theme/         # Color.kt, Theme.kt, Type.kt, Shapes.kt
├── util/              # Apufunktiot
├── widget/            # Glance-widget (CounterWidget, CounterWidgetState)
├── App.kt             # Application-luokka (@HiltAndroidApp)
└── MainActivity.kt    # Single activity, enableEdgeToEdge, noise texture
```

### Room Database v3
| Entity | Taulu | Kentät |
|--------|-------|--------|
| CounterProjectEntity | counter_projects | id, name, count, secondaryCount, stepSize, notes, sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds, createdAt, updatedAt |
| CounterHistoryEntity | counter_history | id, projectId (FK CASCADE), action, previousValue, newValue, timestamp |
| SessionEntity | sessions | id, projectId (FK CASCADE), startedAt, endedAt, startRow, endRow, durationMinutes |
| YarnCardEntity | yarn_cards | id, brand, yarnName, fiberContent, weightGrams, lengthMeters, needleSize, gaugeInfo, colorName, colorNumber, dyeLot, weightCategory, careSymbols, photoUri, createdAt |

Migraatiot: AutoMigration 1→2, AutoMigration 2→3

### DataStore (AppPreferences)
themeMode (SYSTEM/LIGHT/DARK), hapticFeedback, keepScreenAwake, useImperial, showKnittingTips

### ViewModel-scopet
- **CounterViewModel:** Scopattu `TopLevelDestination.Projects.route` nav graphiin — jaettu CounterScreenin ja ProjectListScreenin kesken
- **YarnCardViewModel:** Scopattu `KnitToolsNavHost`-tasolle — jaettu Tools-tabin yarn-näyttöjen kesken
- Muut ViewModelit: per-screen scope (hiltViewModel)

---

## UI-komponentit (`ui/components/`, 20 kpl)

**Ydin:** ToolScreenScaffold, NumberInputField, ResultCard, UnitToggle, SegmentedToggle, ConfirmationDialog, PasteInstructionButton

**Animaatiot:** AnimatedResultNumber (crossfade+slide), RollingCounter (digit roll), ResultNumberInset

**Projektinhallinta:** ProjectCard (long-press tuki), SessionItem

**Lisäkomponentit:** BadgePill, InfoNote, SectionHeader, HubListItem, QuickTipCard

**Care-symbolit:** CareSymbol, CareSymbolIcon, CareSymbolPicker

---

## Testit

### Yksikkötestit (`./gradlew test`, 128 kpl)
| Alue | Testejä | Tiedostot |
|------|---------|-----------|
| InstructionParser | 34 | key:value, regex (inc/dec, gauge, swatch), typo-toleranssi, edge cases |
| ProjectSummarizer | 7 | simpleSummary kaikilla datayhdistelmillä |
| YarnLabelNanoParser | 6 | parseResponse: täysi, osittainen, tyhjä, roska |
| YarnLabelParser | 17 | OCR regex-parsinta |
| Laskurit | 48 | CastOn, Gauge, IncDec, Counter, YarnEstimator, NeedleSize |
| TrialManager | 8 | Pro trial -logiikka |

### Instrumented testit (`./gradlew connectedDebugAndroidTest`, 3 kpl)
| Testi | Kuvaus |
|-------|--------|
| migrate1to2 | Room AutoMigration v1→v2 testidatalla |
| migrate2to3 | Room AutoMigration v2→v3 — uudet kentät + sessions-taulu |
| migrate1to3 | Koko migraatioketju v1→v3 |

---

## Quick Tips

25 neulontavinkkiä `strings.xml` string-arrayna (`knitting_tips`). Satunnaisvalinta `HomeViewModel`:ssa. Näkyvyys hallittavissa Settings-näkymän "Show knitting tips" -togglella.

---

## Spec-dokumentit

| Tiedosto | Kuvaus |
|----------|--------|
| `knitting-toolkit-spec.md` | Päädokumentti: toiminnallisuus, arkkitehtuuri |
| `knittools-design-spec-v4.md` | Visuaalinen design-speksi (navigaatio, värit, kaikki näytöt) |
| `knittools-nav-restructure.md` | 4→3 tab navigaatiouudistus |
| `knittools-project-management.md` | Projektinhallinta: long-press, rename, archive, reactivate |
| `knittools-ocr-expansion.md` | OCR + Yarn Card -laajennus |
| `GEMINI-NANO.md` | Kaikki Nano-ominaisuudet: toteutetut (5) + suunnitellut (7) |

---

## Data & Privacy

- Kaikki data paikallisesti (Room + DataStore)
- Ei pilvisynkkausta, ei tilejä, ei analytiikkaa
- Ei Firebase, ei crash reporting releasessa
- Oikeudet: CAMERA (vain OCR), BILLING (IAP)
- Play Store Data Safety: ei kerätä, ei jaeta dataa
