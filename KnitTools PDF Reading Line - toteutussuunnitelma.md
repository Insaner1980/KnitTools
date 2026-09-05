# KnitTools PDF Reading Line - toteutussuunnitelma

> **Historiallinen suunnitelma:** Tämä dokumentti kuvaa schema 13 -vaiheen lähtötilaa eikä nykyistä schemaa tai toteutusarkkitehtuuria. Nykyinen lähde on `PROJECT.md` ja toteutunut arkkitehtuuri on kuvattu `AGENTS.md`/`CODEX.md`-tiedostoissa.

## Yhteenveto

Tavoite: tehdä PDF-patternin lukulinjasta kilpailukykyinen ilman AI:ta: käyttäjä voi näyttää viivan, siirtää sen oikealle riville, sitoa sen nykyiseen row-laskuriin, ja rivin pitää liikkua luotettavasti laskurin mukana.

Kilpailijamalli: My Row Counter käyttää käsin asetettavia highlightereita ja laskuriin linkitettävää chart highlighteria ([lähde](https://rowcounterapp.com/counter.html)), RowKeeper tarjoaa PDF-viewerin, row/column-markerit ja drag-repositionoinnin ([lähde](https://apps.apple.com/us/app/rowkeeper-knitting-tracker/id6758925791)), knitCompanion käyttää row/stitch-markereita ja one-tap-tyyppistä markerin siirtoa ([lähde](https://www.knitcompanion.com/wp-content/uploads/2023/08/User-Guide-2023.pdf)). Android-toteutus nojaa Compose pointer input -eleisiin ([pointer input](https://developer.android.com/develop/ui/compose/touch-input/pointer-input), [gesture docs](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)).

Ehdoton työskentelysääntö seuraavassa chatissä: tee aina vain yksi pieni vaihe kerrallaan, verifioi se, kirjaa mitä teit, pysähdy ja odota käyttäjän `jatka`-pyyntöä. Älä yhdistä vaiheita, vaikka seuraava näyttäisi helpolta.

## Nykytila ja rajaukset

Nykyinen pohja:
- Room schema on 13.
- Attached project PDF:n lukulinjan tila on `counter_projects.readingLineEnabled` ja `readingLineYFraction`.
- Rivikartta on jo olemassa kentässä `counter_projects.patternRowMapping`.
- `RowMarker(row, page, yPosition)`, `parseMapping`, `serializeMapping` ja `interpolateYPosition` ovat olemassa.
- `CounterViewModel` sisältää jo `upsertPatternRowMarker` ja `mergePatternRowMarkers`.
- `PatternViewerScreen` näyttää PDF:n, lukulinjan ja alareunan row/page-ohjaimet.
- PDF:n valkoinen taustakorjaus ja yksinkertainen row-step-liike on jo tehty tässä työpuussa, mutta seuraavan chatin pitää ensin tarkistaa, ovatko ne vielä mukana.

Rajaukset:
- Ei AI:ta, OCR:ää, pilvipalvelua, Drive/Dropbox SDK:ta tai provider-kohtaista importtia.
- Ensimmäinen toteutus pysyy schema 13:ssa. Ei uutta Room-taulua ennen kuin oikeasti tarvitaan osioita, useita highlightereita tai monimutkaista chart-dataa.
- Library-only pattern viewer pysyy session/rotation-saveable-tilassa eikä kirjoita projektin `patternRowMapping`-dataa.
- Älä aja käyttäjän `lc` tai `sc` -wrappereita. Käytä vain kohdennettuja Gradle-komentoja.
- Älä revertata dirty worktree -muutoksia. Lue, erottele ja työskentele niiden kanssa.

## Vaihekohtainen toteutus

### Vaihe 0: Baseline ja etenemismuistio

Tee ensin vain tämä:
- Lue `AGENTS.md`, `CODEX.md`, relevantit tiedostot ja `git status --short`.
- Tarkista, ovatko aiemmin tehdyt muutokset vielä mukana:
  - `PdfPageRenderer.kt`: `bitmap.eraseColor(Color.WHITE)` ennen `page.render`.
  - `ReadingLine.kt`: clamp-vakiot ja row-step.
  - `PatternViewerScreen.kt`: `TrackReadingLineForCurrentRow`.
- Luo tai päivitä etenemismuistio `config/pattern-reading-line-progress.md`.
- Kirjaa sinne:
  - päivämäärä
  - nykyinen branch
  - dirty worktree -huomiot
  - mikä vaihe aloitettiin ja mikä valmistui
  - mitä testejä ajettiin
  - mitä jäi tekemättä
- Jos arkkitehtuuri tai data flow muuttuu myöhemmissä vaiheissa, päivitä sekä `AGENTS.md` että `CODEX.md` samassa vaiheessa.
- Jos muistipäivitys on seuraavassa chatissä edelleen eksplisiittisesti sallittu, tee vaihevalmistumisen jälkeen erillinen Codex memory update note ad_hoc-muistiin. Älä koskaan muokkaa `MEMORY.md` suoraan.

Pysähdy tämän jälkeen.

### Vaihe 1: Drag-commit-rajapinta lukulinjalle

Tavoite: erotetaan viivan live-siirto ja “käyttäjä lopetti vedon” -commit.

Muuta vain pattern viewerin lukulinjaohjausta:
- Lisää `PatternViewerContentActions`-rakenteeseen kaksi callbackia:
  - `onReadingLineYFractionChange(y: Float)` live-päivitykselle
  - `onReadingLineYFractionCommit(y: Float)` vedon lopetukselle
- Päivitä `ReadingLineOverlay` käyttämään `detectVerticalDragGestures`-callbackeja niin, että:
  - `onVerticalDrag` liikuttaa viivaa kuten nyt
  - `onDragEnd` kutsuu commit-callbackia viimeisellä lasketulla arvolla
  - `onDragCancel` ei tallenna uutta ankkuria
- Säilytä nykyinen scale-korjaus: `dragAmount / scale.coerceAtLeast(1f)`.
- Library viewerissa commit voi olla no-op tai sama local saveable -päivitys, mutta se ei saa kirjoittaa projektidataa.

Testit ennen toteutusta:
- Lisää lähdetesti `PatternViewerSourceTest`: sen pitää failata ennen muutosta ja tarkistaa, että `onReadingLineYFractionCommit` kulkee `PatternViewerContentActions` -> `ReadingLineOverlay`.
- Tarkista lähdetestillä, että drag-end mainitaan toteutuksessa.

Verifiointi:
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest`

Kirjaa vaihe muistioon ja pysähdy.

### Vaihe 2: Manuaalinen riviankkuri dragin lopussa

Tavoite: kun projektin PDF:ssä käyttäjä vetää lukulinjan ja päästää irti, sovellus tallentaa nykyisen rivin sijainnin: `row + page + yPosition`.

Muuta vain project pattern viewerin data flow:
- Lisää `PatternViewerScreen`-project-haaraan commit-callback:
  - ensin clampaa y `sanitizeReadingLineYFraction`
  - kutsu `counterViewModel.updateReadingLineYFraction(y)`
  - kutsu `counterViewModel.upsertPatternRowMarker(row = counterState.counter.count, page = currentPage, yPosition = y)`
- Älä tee tätä library viewerissä.
- Älä lisää uutta skeemaa. Käytä nykyistä `patternRowMapping` JSONia.
- Jos current row on 13 ja current page on 2, drag commit lisää tai päivittää markerin `(row=13,page=2,yPosition=<nykyinen>)`.

Testit ennen toteutusta:
- Lisää `PatternViewerSourceTest`-testi, joka vaatii:
  - project viewer antaa `onReadingLineYFractionCommit`
  - commitissä kutsutaan `upsertPatternRowMarker`
  - commit käyttää `counterState.counter.count` ja `currentPage`
  - library viewer ei kutsu `upsertPatternRowMarker`

Lisätesti jos järkevästi mockattavissa:
- `CounterViewModel` tai lähdetesti varmistaa, että `upsertPatternRowMarker` korvaa saman row/page-parin eikä lisää duplikaattia.

Verifiointi:
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest`

Kirjaa vaihe muistioon ja pysähdy.

### Vaihe 3: Korjaa riviliikkeen päätöksenteko ankkureilla

Tavoite: yksi ankkuri ei saa lukita viivaa paikalleen. Kaksi ankkuria mahdollistaa interpolation. Ilman kahta ankkuria fallback-liike käyttää row-stepiä.

Muuta domain-logiikkaa mieluiten uuteen pieneen helperiin:
- Luo domain-tason funktio esimerkiksi `resolveReadingLineYFraction(...)`.
- Säännöt:
  - Jos current row/page löytyy exact-markerina, palauta sen y.
  - Jos samalla sivulla on sekä edellinen että seuraava marker, interpoloi niiden väli.
  - Jos samalla sivulla on vain yksi marker tai vain yksi puoli, älä extrapoloi samaan y-arvoon, vaan käytä nykyistä y:tä ja rowDeltaa.
  - Jos rowDelta on 0 eikä exact/interpolation löydy, älä muuta viivaa.
  - Clampaa aina domainin lukulinjarajoilla.
- Päivitä `TrackReadingLineForCurrentRow` käyttämään tätä helperiä.
- Päivitä `RowMappingParserTest`: vanha “previous or next returns same y” -extrapolointi pitää muuttaa, jos se lukitsee rivin.

Testit:
- `ReadingLineTest`: rowDelta +1 liikuttaa alas yhden stepin.
- `ReadingLineTest` tai uusi calculator-testi: exact marker palautuu täsmälleen.
- Kaksi markeria, row 10 y=0.2 ja row 20 y=0.8, row 15 palauttaa 0.5.
- Yksi marker row 13 y=0.45 ja current row 19 rowDelta +6 palauttaa `0.45 + 6 * step`, clampattuna.
- Page 1 markerit eivät vaikuta page 2:een.

Verifiointi:
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.domain.model.ReadingLineTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest`

Kirjaa vaihe muistioon ja pysähdy.

### Vaihe 4: Käyttäjän näkyvät kontrollit mallille 1

Tavoite: käyttäjä ymmärtää mitä voi tehdä ilman ohjetekstipläjäystä.

Lisää pattern viewerin overflowiin tai tiiviiseen viewer-ohjaimeen:
- `Save line as row X` / suomeksi esimerkiksi `Tallenna viiva riviksi X`.
- `Clear row mark` / `Poista rivimerkki`.
- `Clear page marks` / `Poista sivun rivimerkit`.
- Kaikki user-visible stringit kaikkiin localeihin, ei vain `values`.

Toteutus:
- Lisää `CounterViewModel`-metodit:
  - `removePatternRowMarker(row, page)`
  - `removePatternRowMarkersForPage(page)`
- Molemmat käyttävät `parseMapping` + `serializeMapping` + `updatePatternRowMapping`.
- Project viewer käyttää current row/current page -arvoja.
- Library viewer ei näytä näitä projektikohtaisia toimintoja.

Testit:
- Parser/ViewModel-lähdetesti: remove current row/page poistaa vain yhden markerin.
- Remove page poistaa vain kyseisen page-arvon markerit.
- Pattern viewer -lähdetesti: project viewer näyttää kontrollit, library viewer ei.
- String resource -lähdetesti jos projektissa on vastaava pattern olemassa.

Verifiointi:
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest`

Kirjaa vaihe muistioon ja pysähdy.

### Vaihe 5: Rivimerkinnän visuaalinen laatu

Tavoite: lukulinja tuntuu oikealta työkalulta, ei pelkältä viivalta.

Parannukset:
- Lisää viivan yhteyteen pieni label: `Row 13` tai locale-stringin mukainen lyhyt muoto.
- Label ei saa peittää PDF:n pääsisältöä liikaa:
  - sijoita vasempaan reunaan pienenä pillinä
  - käytä theme-värejä, ei inline-värikeksintöjä
  - pidä touch target edelleen koko viivan/bandin alueella
- Lisää mahdollinen lukitus:
  - `Lock reading line`
  - lukittuna row +/- liikuttaa viivaa, mutta drag ei muuta sitä
  - tätä ei tehdä, jos vaihe kasvaa liian isoksi. Silloin tee label ensin ja pysähdy.

Testit:
- Lähdetesti: label käyttää `currentRow`.
- Lähdetesti: ei hardcoded user-visible text.
- Manuaalinen Android-tarkistus: label ei peitä alabarin tekstejä, eikä PDF skaalaudu oudosti.

Verifiointi:
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest`
- Tarvittaessa `.\gradlew :app:assembleDebug`

Kirjaa vaihe muistioon ja pysähdy.

### Vaihe 6: Kahden pisteen kalibrointi

Tavoite: käyttäjä voi nopeasti opettaa sivun rivivälin: ensimmäinen rivi ja viimeinen rivi, sovellus interpoloi välin.

Toteutus ilman skeemamuutosta:
- Käytä edelleen `patternRowMapping`-listaa.
- Lisää pieni kalibrointitila pattern vieweriin:
  - Step 1: käyttäjä asettaa viivan ensimmäiselle riville ja tallentaa rivinumeron.
  - Step 2: käyttäjä asettaa viivan viimeiselle riville ja tallentaa rivinumeron.
  - Lopuksi `mergePatternRowMarkers(listOf(first,last))`.
- Rivinumerokentän oletus on nykyinen counter row.
- Älä tee automaattista PDF-tekstin tunnistusta.
- Jos käyttäjä peruu, älä muuta mappingia.
- Jos molemmat pisteet ovat samalla row-arvolla, näytä invalid input ja älä tallenna.

Testit:
- Domain/parser-testit kahden markerin interpolationille.
- UI/source-testit kalibrointitilan olemassaololle ja `mergePatternRowMarkers`-kutsulle.
- Stringit kaikkiin localeihin.

Verifiointi:
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest`

Kirjaa vaihe muistioon ja pysähdy.

### Vaihe 7: Persistointipolish ja suorituskyky

Tavoite: vähennetään turhia tietokantakirjoituksia dragin aikana.

Tee tämä vasta kun malli 1 ja kalibrointi toimivat:
- Lisää project viewerille transientti lukulinjan preview-state, jos nykyinen `updateReadingLineYFraction` kirjoittaa DB:hen liian usein.
- Live drag päivittää previewn.
- Commit kirjoittaa `readingLineYFraction` ja mahdollisen `RowMarker`-ankkurin.
- Ulkoinen state-muutos synkkaa previewn vain kun käyttäjä ei vedä viivaa.
- Varmista, että hide/show säilyttää viimeisen commit-position.

Testit:
- Source/testi: drag live ei kutsu repositoryä suoraan.
- ViewModel/repository-testi jos rajapinta muuttuu.
- Manuaalinen: nopea drag ei aiheuta havaittavaa jankkia.

Verifiointi:
- Kohdennetut unit-testit.
- `.\gradlew :app:assembleDebug`

Kirjaa vaihe muistioon ja pysähdy.

### Vaihe 8: Dokumentointi ja arkkitehtuurimuisti

Tavoite: seuraava konteksti ei huku.

Päivitä:
- `AGENTS.md` ja `CODEX.md` samalla tekstillä:
  - attached PDF reading line persists `readingLineEnabled`, `readingLineYFraction`
  - row anchors live in `patternRowMapping` as `RowMarker(row,page,yPosition)`
  - drag commit creates/updates current row anchor
  - library viewer remains session-local
- Päivitä `PROJECT.md` vain jos se on nykytiladokumentin kannalta tarpeen.
- Päivitä `config/pattern-reading-line-progress.md` lopputilaan.
- Jos muistiin kirjaaminen on eksplisiittisesti sallittu seuraavassa chatissä, tee ad_hoc memory update note. Älä editoi varsinaista `MEMORY.md`.

Verifiointi:
- `rg -n "patternRowMapping|readingLineEnabled|readingLineYFraction|RowMarker" AGENTS.md CODEX.md PROJECT.md config`
- Dokumenttien pitää vastata koodia, ei toisin päin.

Kirjaa vaihe muistioon ja pysähdy.

## Hyväksymiskriteerit

Malli 1 on valmis, kun:
- PDF näkyy valkoisella taustalla eikä tumma app-tausta tee tekstistä näkymätöntä.
- Show reading line näyttää viivan.
- Käyttäjä voi vetää viivan oikeaan kohtaan.
- Drag release tallentaa nykyisen row/page/y-ankkurin project PDF:lle.
- Row 13 -> Row 19 liikuttaa viivaa eikä jätä sitä paikalleen.
- Kun palataan row 13:een, viiva osuu tallennettuun anchor-kohtaan.
- Kaksi ankkuria interpoloi niiden välin.
- Yksi ankkuri ei lukitse viivaa yhteen paikkaan.
- Page 1:n markerit eivät liikuta page 2:n viivaa.
- Library viewer ei kirjoita projektin rivikarttaa.
- Zoom/scroll ei riko viivan sijaintia suhteessa PDF:ään.
- Kaikki user-visible stringit ovat resourceissa ja localeissa.
- Kohdennetut unit/source-testit menevät läpi.
- Jokaisen vaiheen jälkeen muistio on päivitetty ja työ pysähtyy.

## Komennot

Käytä pieniä tarkistuksia:
- `git status --short`
- `rg -n "ReadingLine|patternRowMapping|RowMarker|upsertPatternRowMarker" app/src/main/java app/src/test/java`
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest`
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest`
- `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.domain.model.ReadingLineTest`
- `.\gradlew :app:assembleDebug` vasta kun vaihe koskee enemmän kuin yhtä pientä UI/domain-palaa.

Älä aja:
- `lc`
- `sc`
- muita käyttäjän wrappereita ilman erillistä pyyntöä.

## Seuraavan chatin aloitusohje

Aloita seuraavassa chatissä näin:
1. Lue tämä suunnitelma.
2. Lue `AGENTS.md` ja `CODEX.md`.
3. Tee Phase 0.
4. Pysähdy ja raportoi Phase 0:n tulos.
5. Älä jatka Phase 1:een ennen käyttäjän `jatka`-pyyntöä.
