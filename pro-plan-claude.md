# Pro-järjestelmän toteutussuunnitelma

Status: **luonnos päätettäväksi** — koodia ei ole muutettu tämän dokumentin perusteella.
Laajuus: `pro/`, `ui/screens/counter/`, `ui/screens/notes/`, `ui/screens/library/`,
`ui/screens/session/`, `ui/screens/insights/`, `ui/screens/pattern/`, `res/values*/strings.xml`.

---

## 0. Päätös joka ohjaa kaikkea

**Malli A: oma sisältö säilyy aina käytettävissä, vain uuden luominen lukitaan.**

Sovellus toteuttaa tätä jo kahdessa paikassa (projektit, lankakortit) ja rikkoo sitä
kuudessa. Jakolinja ei ole periaatteellinen vaan sattumanvarainen: kirjaston objektit
portataan luonnissa, projektin sisäiset objektit portataan pääsyssä.

### Säännön tarkka muotoilu

| Toiminto | Ilmaisversio kokeilun jälkeen |
|---|---|
| **Lue** olemassa olevaa sisältöä | aina sallittu |
| **Käytä** olemassa olevaa objektia (laskuri, muistutus) | aina sallittu |
| **Muokkaa** olemassa olevaa sisältöä | sallittu, paitsi muistiinpanoissa (ks. 0.2) |
| **Luo uusi** objekti | Pro |
| **Poista** omaa sisältöä | aina sallittu |

Sovellus ei koskaan poista käyttäjän sisältöä oikeustason perusteella.

### 0.1 Seuraus jonka pitää hyväksyä: `FULL_HISTORY` ei voi säilyä nykyisellään

Istuntohistoria **on** käyttäjän omaa sisältöä. Malli A tarkoittaa, ettei sitä voi
rajata 24 tuntiin — ei kokeilussa syntyneiden eikä kokeilun jälkeen syntyneiden
istuntojen osalta.

Kaksi vaihtoehtoa:

- **A1 (suositus):** istunnot näkyvät aina kokonaan. `FULL_HISTORY` poistetaan
  `ProFeature`-listalta ja ostosivulta. Pron arvolupaus siirtyy analyysiin:
  *data on aina sinun, Pro kertoo mitä se tarkoittaa* (Insights-kaaviot, putki).
  Tämä on johdonmukainen tarina ja poistaa koko epäjohdonmukaisuuden kerralla.
- **A2:** `FULL_HISTORY` säilyy näkymärajauksena, mutta fyysinen poisto lopetetaan.
  Ristiriita mallin A kanssa jää: käyttäjän omaa työtä piilotetaan.

**Tämä on ainoa avoin päätös. Kaikki muu seuraa mekaanisesti.**

### 0.2 Muistiinpanojen erikoistapaus

Muistiinpano on yksi tekstikenttä projektia kohden, joten "uuden luominen" ei ole
mielekäs raja. Selkein linja on **luku aina, kirjoitus Pro**. Tämä on ainoa kohta
jossa mallia A sovelletaan luku/kirjoitus-rajana eikä luonti/käyttö-rajana, ja se
pitää dokumentoida sellaisena.

---

## 1. Nykytilan auditointi

Merkinnät: **C** = luontiportti (mallin A mukainen), **P** = pääsyportti (muutettava),
**T** = tuhoava (poistettava), **X** = kuollut tai selittämätön (bugi).

### 1.1 Mallin A mukaiset — ei muutoksia

| Ominaisuus | Paikka | Käytös |
|---|---|---|
| `UNLIMITED_PROJECTS` | `ProjectListViewModel.kt:327` | **C** raja 1 aktiivinen projekti, tarkistus vain luonnissa |
| `UNLIMITED_YARN` | `MyYarnScreen.kt:103` | **C** olemassa olevat kortit näkyvät aina |
| `WIDGET` | `CounterWidget.kt:76`, `CounterWidgetActions.kt:40` | pinta, ei sisältöä |
| `INSIGHTS_CHARTS` | `InsightsViewModel.kt:327` | analyysi, ei sisältöä |
| `STREAK` | `InsightsViewModel.kt` | analyysi, ei sisältöä |
| `PATTERN_CAMERA_SCAN` | `PatternPickerSheet.kt:245` | toiminto, ei sisältöä (mutta ks. 1.4) |

### 1.2 Pääsyportit — muutettava

| Ominaisuus | Paikka | Nyt | Mallissa A |
|---|---|---|---|
| `NOTES` | `NavGraph.kt:404` | ohjaa ostosivulle | avaa editorin vain luku -tilassa |
| `NOTES` | `NotesEditorViewModel.kt:60` | asettaa `notes = ""` | lataa oikean tekstin, `isPro = false` |
| `NOTES` | `ProjectListViewModel.kt:375` | ostosivulle | avaa editorin |
| `NOTES` | `CounterViewModel.kt:978` | estää `setNotes` | **säilyy** — kirjoitus on Pro |
| `PROGRESS_PHOTOS` | `LibraryScreen.kt:97` | ostosivulle | avaa gallerian |
| `PROGRESS_PHOTOS` | `ProjectListViewModel.kt:385` | ostosivulle | avaa gallerian |
| `PROGRESS_PHOTOS` | `CounterViewModel.kt:931` | estää `savePhoto` | **säilyy** — uusi kuva on Pro |
| `MULTIPLE_COUNTERS` | `CounterWorkspaceSections.kt:164` | piilottaa koko osion | näyttää olemassa olevat laskurit |
| `MULTIPLE_COUNTERS` | `CounterScreen.kt:472` | sheet ei avaudu | avautuu, "lisää" lukossa |
| `MULTIPLE_COUNTERS` | `CounterViewModel.kt:776` | estää lisäyksen | **säilyy** |
| `MULTIPLE_COUNTERS` | `CounterViewModel.kt:785` | estää **käytön** | poistetaan |
| `SECONDARY_COUNTER` | `CounterWorkspaceSections.kt:244` | piilottaa herosta | näyttää jos `secondaryCount > 0` |
| `SECONDARY_COUNTER` | `CounterViewModel.kt:709,715` | estää inc/dec | sallitaan jos arvo > 0 |
| `ROW_REMINDERS` | `CounterWorkspaceSections.kt:180` | piilottaa hälytyskortin | näyttää kortin |
| `ROW_REMINDERS` | `CounterViewModel.kt:892` | estää kuittauksen | sallitaan |
| `ROW_REMINDERS` | `CounterViewModel.kt:905` | estää poiston | sallitaan |
| `ROW_REMINDERS` | `CounterViewModel.kt:857,877` | estää luonnin/muokkauksen | **säilyy** |
| `SHAPING_COUNTER` | `CounterViewModel.kt:778,787` | luonti + käyttö | käyttö sallitaan, luonti säilyy |
| `REPEAT_SECTION` | `CounterViewModel.kt:779,788,800` | luonti + käyttö | käyttö sallitaan, luonti säilyy |
| `FULL_HISTORY` | `SessionHistoryViewModel.kt:65` | 24 h ikkuna | ks. päätös 0.1 |

### 1.3 Tuhoava — poistettava ehdoitta

| Paikka | Mitä tekee |
|---|---|
| `CounterViewModel.kt:1406` `pruneHistoryForFree()` | `deleteHistoryBefore` → **fyysinen DELETE** `counter_history`-tauluun |
| Kutsupaikka `CounterViewModel.kt:253` | ajetaan aina kun `initialStateReady && !isPro` |

`counter_history` on kumoamispino (`undoLastChange` nostaa viimeisimmän). Käytännön
haitta on pieni, mutta **tämä on koko Pro-järjestelmän ainoa peruuttamaton toimenpide**
ja se laukeaa myös kokeilun päättymishetkellä. Pron ostaminen jälkikäteen ei palauta
poistettua. Se on syntynyt oletusarvona, ei päätöksenä.

### 1.4 Kuolleet ja selittämättömät — bugeja

| Paikka | Oire |
|---|---|
| `CounterScreen.kt:423` + `:472` | "Counters"-valikkokohta sulkee valikon eikä tee mitään |
| `PatternPickerSheet.kt:245` | kameraskannaus disabloitu ilman selitystä |
| `YarnManagementSheet.kt:234` | "Save to My Yarn" disabloitu ilman selitystä |
| `InsightsScreen.kt:296` | Pro-kortti saavuttamaton (ks. työ 1) |
| `CounterScreen.kt:209` | haptinen värinä laukeaa ilman korttia ja ilman kuittausmahdollisuutta |

### 1.5 Ostosivun ja porttien ristiriita

`ProUpgradeScreen.kt:251` listaa **10 ominaisuutta 14:stä**. Puuttuvat:
`SHAPING_COUNTER`, `REPEAT_SECTION`, `INSIGHTS_CHARTS`, `STREAK`.

Insightsin Pro-kortti myy siis "daily rhythm + streak" — kaksi asiaa joita ostosivu
ei mainitse lainkaan.

---

## 2. Työjärjestys

Jokainen vaihe on itsenäisesti julkaisukelpoinen ja vihreä.

### Työ 1 — Insightsin Pro-kortin saavutettavuus *(regressio, tehdään heti)*

Ei riipu päätöksestä 0.1.

**Ongelma:** `InsightsViewModel.kt:327` palauttaa `emptyList()` ilman Pro-oikeutta,
ja `InsightsScreen.kt:296` poistuu ennen Pro-haaraa kun aktiivisia ämpäreitä on < 2.
Kortti ei siis renderöidy koskaan.

**Korjaus:** siirrä `MINIMUM_CHART_BARS`-vartija koskemaan vain kaaviohaaraa.

```
if (uiState.totalMinutes <= 0) return
if (uiState.isPro && aktiivisiaÄmpäreitä < MINIMUM_CHART_BARS) return
```

**Hyväksymiskriteerit**
- Ilman Pro-oikeutta osio näkyy ja `InsightsProChartCard` renderöityy aina kun
  aikavälillä on minuutteja
- Pro-oikeudella yhden pylvään kaaviota ei edelleen piirretä
- `chartHeaderMeta` toimii tyhjällä `chartBuckets`-listalla
- Lähdetesti lukitsee ettei vartija ohita Pro-haaraa

### Työ 2 — Tuhoavan pruning-toiminnon poisto

Poista `pruneHistoryForFree()` ja sen kutsu. Harkitse tilalle oikeustasosta
riippumaton yläraja (esim. 500 viimeisintä merkintää projektia kohden), jos
kumoamispinon kasvu huolestuttaa.

**Hyväksymiskriteerit**
- Ilmaisversio ei poista `counter_history`-rivejä
- Kokeilun päättyminen ei poista mitään
- Testi: kokeilun vanheneminen ei muuta rivimäärää kannassa

### Työ 3 — Päätös 0.1 ja istuntohistoria

Odottaa valintaa A1 / A2.

**A1:** poista `FULL_HISTORY` `ProFeature`-listalta, poista suodatus
`SessionHistoryViewModel.kt:65-77`, poista `pro_full_history_hint`-kortti
`SessionHistoryScreen.kt`:stä, poista `pro_feature_full_history` ostosivulta.

**A2:** säilytä suodatus, muuta kopioteksti vastaamaan totuutta — historia ei
katoa, vain näkymä rajautuu.

### Työ 4 — Pääsyporttien muunto luontiporteiksi

Kohdat taulukosta 1.2, järjestyksessä:

**4a. Muistiinpanot.** Poista `NavGraph.kt:404` -uudelleenohjaus. Korjaa
`NotesEditorViewModel.kt:60` lataamaan oikea teksti `notes = ""` sijaan.
`ProjectListViewModel.kt:375` avaa editorin. Editorissa vain luku -tila:
syöttökenttä `readOnly`, alla yksi rivi Pro-selitystä. `CounterViewModel.kt:978`
säilyy.

**4b. Kuvat.** `LibraryScreen.kt:97` ja `ProjectListViewModel.kt:385` avaavat
gallerian. Galleriassa "lisää kuva" lukossa selityksen kanssa.
`CounterViewModel.kt:931` säilyy.

**4c. Lisälaskurit.** `CounterWorkspaceSections.kt:164` näyttää osion aina kun
`projectCounters.isNotEmpty()`. `CounterScreen.kt:472` avaa sheetin aina.
`CounterViewModel.kt:785` `canUseProjectCounter` poistetaan — olemassa olevaa
laskuria saa käyttää. `:776` `canAddProjectCounter` säilyy.

**4d. Toistolaskuri.** `CounterWorkspaceSections.kt:244` näyttää rivin kun
`secondaryCount > 0`. `CounterViewModel.kt:709,715` sallivat inc/dec samalla
ehdolla. *Harkinnanvarainen:* toistolaskuri on projektin kenttä eikä listaobjekti,
joten "onko sitä käytetty" johdetaan arvosta.

**4e. Muistutukset.** `CounterWorkspaceSections.kt:180` näyttää hälytyskortin.
`CounterViewModel.kt:892` `dismissReminder` ja `:905` `deleteReminder` sallitaan.
`:857` `addReminder` ja `:877` `updateReminder` säilyvät.

**Hyväksymiskriteerit koko työlle**
- Kokeilussa luotu sisältö on kokeilun jälkeen luettavissa ja käytettävissä
- Uuden luominen johtaa selittävään Pro-viestiin, ei hiljaiseen estoon
- Yksikkötestit jokaiselle muutetulle portille molemmilla oikeustasoilla

### Työ 5 — Kuolleiden ja selittämättömien kohtien korjaus

Korvataan yhtenäisellä kontekstuaalisella Pro-selityksellä. Jokainen lukittu
kontrolli **näkyy**, kertoo mikä on lukossa, ja tarjoaa reitin ostosivulle.

- `CounterScreen.kt:423` "Counters" — työ 4c poistaa umpikujan
- `PatternPickerSheet.kt:245` kameraskannaus — selitys disabloinnin tilalle
- `YarnManagementSheet.kt:234` "Save to My Yarn" — selitys
- `CounterScreen.kt:209` haptinen värinä — työ 4e palauttaa kortin, jolloin
  värinällä on jälleen selitys

### Työ 6 — Rajat näkyviin ennen työn aloittamista

`ProjectListViewModel.kt:327` laukeaa vasta kun luontidialogi on täytetty.
Raja pitää näkyä ennen sitä: joko dialogia ei avata lainkaan, tai FAB kertoo
tilanteen. Sama koskee lankakortteja.

### Työ 7 — Ostosivun sanasto vastaamaan portteja

Lisää puuttuvat neljä ominaisuutta. Varmista että jokainen porttiviesti käyttää
samaa sanastoa kuin ostosivun rivi, ja poista/muuta ne joita päätös 0.1 muuttaa.

---

## 3. Testaus

**Yksikkötestit**
- Jokainen muutettu portti: `hasFeature = true/false` × olemassa oleva / uusi objekti
- `pruneHistoryForFree` poistettu: kokeilun vanheneminen ei muuta rivimäärää
- `TrialManager`: siirtymä Pro → ilmainen ei muuta käyttäjän dataa

**Lähdetestit** (`ProFeatureGateSourceTest`-tyyliin)
- Yksikään portti ei kutsu `delete`-metodia oikeustason perusteella
- Jokainen `ProFeature` esiintyy ostosivun listassa
- Insightsin vartija ei ohita Pro-haaraa

**Manuaalinen matriisi**
- Kokeilu voimassa → luo sisältöä jokaisesta portatusta tyypistä
- Kokeilu vanhenee (`TrialManager`-debug-ohitus) → kaikki luotu on yhä luettavissa
- Osta Pro → kaikki toimii ennallaan, mitään ei ole kadonnut
- Light + dark, FI + EN

---

## 4. Avoimet kysymykset

1. **Päätös 0.1**: A1 (historia ei ole Pro) vai A2 (näkymärajaus säilyy)?
2. Toistolaskurin näkyvyysehto (4d) — arvosta johtaminen vai erillinen kenttä?
3. Tarvitaanko `counter_history`-taululle oikeustasosta riippumaton yläraja?
4. Pro-viestin ulkoasu Insightsissä: nykyinen ääriviivakortti on näytön ainoa
   ääriviivapinta. `CLAUDE.md:46` sanoo Insightsin olevan kortiton — se koskee
   dataesitystä, ja Pro-kortti istuu dataesityksen paikalla. Ratkaistaan vasta
   kun Pro-viestintä on muuten päätetty.

---

## 5. Mitä tämä suunnitelma ei kata

- Hinnoittelu, tilausmallit, Play Billing -konfiguraatio
- Kokeilun pituus tai sen uusiminen
- Pro-ominaisuuksien valikoima — oletuksena nykyiset 14 (miinus mahdollinen
  `FULL_HISTORY`)
- Insightsin Pro-kortin visuaalinen suunnittelu (kysymys 4)
