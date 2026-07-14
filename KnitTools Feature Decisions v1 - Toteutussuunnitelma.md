# KnitTools Feature Decisions v1 - Toteutussuunnitelma

## Yhteenveto

- Toteuta dokumentin `knittools-feature-decisions-updated.md` v1-sisältö vaiheittain: käsityötyyppi, päälaskurin joustava nimi, lukulinja PDF-katseluun, Linked counter, debug-only Pro unlock ja pilvipalveluista tuonti nykyisen PDF-tuonnin kautta.
- Rajaa Google Drive/Dropbox -synkronointi erilliseksi myöhemmäksi speksiksi. Androidin Storage Access Framework ja `OpenDocument` riittävät v1-tuontiin, koska järjestelmävalitsin voi näyttää Drive/Dropbox-dokumenttiproviderit ilman provider-kohtaista OAuthia. ([developer.android.com](https://developer.android.com/guide/topics/providers/document-provider)) ([developer.android.com](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.OpenDocument))
- Älä muuta counter-heroa, viiden kortin `CounterProjectContentCards`-rakennetta, `ProjectActionsBottomSheet`/`YarnManagementSheet`-perusrakennetta tai viittä päätabia.
- Toteutuksen alussa lukitse likaisen työpuun lähtötilanne `git status --short` + kohdennettu diff-katsaus. Toteutus ei saa palauttaa tai siivota käyttäjän rinnakkaisia muutoksia.

## Julkiset Malli- Ja Rajapintamuutokset

- Nosta Room schema `12 -> 13` yhdellä manuaalisella migraatiolla ja päivitetyllä schema-exportilla. Roomin migraatiot pitää testata exportoitua schemaa vasten. ([developer.android.com](https://developer.android.com/training/data-storage/room/migrating-db-versions))
- Lisää `counter_projects`-tauluun:
  - `craftType TEXT NOT NULL DEFAULT 'KNITTING'`
  - `mainCounterLabelType TEXT NOT NULL DEFAULT 'ROWS'`
  - `mainCounterCustomLabel TEXT NULL`
  - `readingLineEnabled INTEGER NOT NULL DEFAULT 0`
  - `readingLineYFraction REAL NOT NULL DEFAULT 0.5`
- Lisää `project_counters`-tauluun:
  - `linkedToMainCounter INTEGER NOT NULL DEFAULT 0`
- Lisää domain-enumit:
  - `CraftType`: `KNITTING`, `CROCHET`
  - `MainCounterLabelType`: `ROWS`, `ROUNDS`, `REPEATS`, `CUSTOM`
- Lisää yksi keskitetty päälaskurin tekstiformatteri. Se palauttaa hero-otsikon, tavoiterivin, add/decrease-content descriptionit ja projektikorttien count-tekstin. Älä lisää uusia hajallaan olevia `row/round/repeat`-stringeihin perustuvia formattereita.
- Lisää repository-tasolle yksi pääkirjoituspolku main-counter-muutoksille, esimerkiksi `applyMainCounterChange`, joka hoitaa countin, historyn, stitch-resetin ja linked-counter-deltan samassa transaktiossa. UI, undo/reset ja widget-polku käyttävät samaa semantiikkaa.

## Toteutusvaiheet

1. **Lähtötilan ja testien valmistelu**
   - Tarkista nykyiset kutsujat ennen muutoksia: projektin luonti/editointi, main counter increment/decrement/undo/reset, widget count change, Add counter -dialogi, pattern viewer ja Pro-gatet.
   - Lisää ensin regressiotestit migraatiolle, entity/domain-mappauksille, päälaskurin label-formatterille, linked-counter-deltalle ja debug-Pro-käytökselle.

2. **Craft type + joustava päälaskurin nimi**
   - Vanhojen projektien migraatiodefault on `KNITTING + ROWS`.
   - Uusi knitting-projekti saa defaultiksi `ROWS`; uusi crochet-projekti saa defaultiksi `ROUNDS`.
   - Käyttäjä voi vaihtaa labeliksi `ROWS`, `ROUNDS`, `REPEATS` tai `CUSTOM`; custom-arvo trimmataan, vaatii ei-tyhjän tekstin ja rajataan 32 merkkiin.
   - Lisää yhteinen `ProjectDetailsDialog` projektin luontiin ja projektin asetusten editointiin. Topbarin pikainen nimenmuokkaus saa jäädä vain nimeä varten.
   - Projektikortin metadata näyttää käsityötyypin ja päälaskurin labelin, esimerkiksi `Crochet · Rounds`; counter-topbarissa säilyy vain iso projektin nimi.
   - Abbreviation-filterin täysi crochet-sisältö jää myöhemmäksi, mutta route/UI-state saa craft-contextin valmiiksi, jotta projektista avattaessa oletus tulee samasta `craftType`-arvosta.

3. **Linked counter**
   - Lisää Add/Edit extra counter -asetuksiin switch nimellä `Linked counter` ja lyhyt arkikielinen kuvaus.
   - V1-käytös on yksisuuntainen: päälaskurin delta siirtyy linked-countereihin. Päälaskurin `+1`, `-1`, undo, reset ja widget-muutokset käyttävät samaa deltaa; linked-arvo ei laske alle nollan.
   - Reset käyttää todellista deltaa, esimerkiksi main `30 -> 0` antaa linked-counterille `-30`, alarajana `0`.
   - Linked counterin oma manuaalinen muuttaminen on sallittua; seuraava main-counter-delta jatkaa sen nykyisestä arvosta.
   - Repeat section -countereita ei merkitä linked-countereiksi, koska ne seuraavat jo päälaskuria omalla repeat-section-logiikallaan.

4. **Pattern viewer reading line**
   - Lisää lukulinja vain pattern viewer -näkymään, ei counter screeniin.
   - Toteuta overlay nykyisen PDF-sivun kanssa samaan zoom/pan-koordinaatistoon, jotta se pysyy paikallaan zoomatussa PDF:ssä. Compose `transformable` tukee pan/zoom-eleiden havaitsemista nykyisessä mallissa. ([developer.android.com](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch))
   - Lukulinja on show/hide-toggle topbarin overflowssa. Kun näkyvissä, käyttäjä voi vetää sen pystysijaintia; sijainti tallennetaan `0.0..1.0`-fraktiona ja clampataan pois aivan reunoista.
   - Projektin attached-pattern-viewerissa tila persistoidaan projektiriville. Library-only pattern viewerissa tila on v1:ssä session/rotation-saveable, ellei samaan työhön lisätä erillistä saved-pattern schemaa.
   - Sivunvaihto ei nollaa y-sijaintia; sama lukukorkeus näkyy uudella sivulla.

5. **Debug-only Pro unlock**
   - Lisää debug-varianttiin Pro-override ilman käyttäjälle näkyvää asetusta. Androidin build variant -malli tukee debug/release-erottelua ja debug source set/build type -kohtaisia asetuksia. ([developer.android.com](https://developer.android.com/build/build-variants))
   - Toteuta override `BuildConfig.DEBUG`-portin tai variant-kohtaisen providerin kautta niin, että release-buildissä arvo on aina pois päältä.
   - Debugissä `ProState.hasFeature` palauttaa true kaikille ProFeature-arvoille: secondary, multiple/shaping/repeat counters, reminders, photos, widgets, insights, unlimited yarn ja muut nykyiset Pro-gatet.
   - Billing, trial state, ostotila ja release-logiikka säilyvät muuttumattomina; debug override ei kirjoita ostoa DataStoreen eikä näy UI:ssa “purchased”-väitteenä.

6. **Drive/Dropbox PDF import**
   - Älä lisää Google Drive- tai Dropbox SDK:ta v1-tuontiin.
   - Lisää nykyiseen pattern import -sheetiin selkeä Drive/Dropbox-tuontivaihtoehto tai copy, joka käyttää samaa `OpenDocument(application/pdf)`-launcher-koodipolkua ja `takePersistableUriPermission`-käsittelyä.
   - Lisää testi tai source-check, joka varmistaa ettei v1-tuonti lisää OAuthia, provider-kohtaista dependencyä tai uutta tiedostokopiointipolkua.

7. **Myöhempi sync-speksi**
   - Erillinen tuleva suunnitelma käsittelee backup/sync-mallin, Pro-gaten, konfliktit, monilaitekäytön, offline-tilan ja OAuthin.
   - Oletus jatkospeksille: ensin manuaalinen export/import tai backup/restore käyttäjän omaan pilvitilaan; älä markkinoi jatkuvaa cross-device synciä ennen konfliktien ja taustasynkin toteutusta.
   - Drive-puolella erottele näkyvä `drive`-tila ja piilotettu `appDataFolder`; Dropboxissa käytä vähimmäisscopeja ja mobile/desktop-sovelluksissa PKCE:tä, jos varsinainen API-synkki rakennetaan myöhemmin. ([developers.google.com](https://developers.google.com/workspace/drive/api/guides/about-files?utm_source=openai)) ([developers.dropbox.com](https://developers.dropbox.com/oauth-guide)) ([developers.dropbox.com](https://developers.dropbox.com/oauth-guide))

## Testaus Ja Hyväksyntä

- Unit/source-testit:
  - Room/entity mapper: uudet kentät säilyvät entity-domain-entity kierrossa.
  - Migration `12 -> 13`: vanha projekti saa `KNITTING`, `ROWS`, linked false ja reading-line defaultit.
  - Main counter formatter: rows/rounds/repeats/custom kaikki locale-templateillä, ei inline-tekstejä.
  - Linked counter: increment, decrement, undo, reset ja widget delta; ei alle nollan.
  - Pro debug override: debugissä kaikki `ProFeature`-arvot aukeavat; release-portti on `BuildConfig.DEBUG`-takana.
- Manuaalinen UAT:
  - Luo knitting- ja crochet-projekti, vaihda labelit, tarkista hero, project card metadata ja add/decrease copy.
  - Lisää linked counter ja testaa päälaskurin `+`, `-`, undo, reset ja widget launch/change.
  - Lisää shaping/repeating/repeat-section counter ja varmista ettei nykyinen extra-counter-käytös regressioidu.
  - Avaa PDF paikallisesta tiedostosta sekä Drive/Dropbox-providerista, näytä/piilota lukulinja, vedä sitä zoomattuna ja vaihda sivua.
  - Debug buildissä käy kaikki Pro-gatet läpi ilman trialia/ostoa.
- Komennot toteutuksen lopussa, ilman käyttäjän wrapper-skriptejä:
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:detekt`
  - `./gradlew :app:lintDebug`
  - Instrumentoitu migration-testi emulatorilla, jos laite on saatavilla.

## Dokumentointi Ja Oletukset

- Päivitä arkkitehtuurimuutosten vuoksi `AGENTS.md` ja `CODEX.md`: Room v13, craft type source of truth, main-counter label formatter, linked counter transaction path, debug-only Pro override ja reading-line persistence.
- Lisää muistipäivitys erillisenä ad-hoc note -tiedostona muistijärjestelmän ohjeen mukaisesti; älä editoi `MEMORY.md` suoraan.
- Lisää kaikki uudet user-visible stringit kaikkiin 11 localeen ja pidä source-testillä avaimet synkassa.
- Oletukset: crochet default label on `Rounds`; full abbreviation-content/filter ja hook sizes ovat myöhempi työ; Drive/Dropbox sync ei kuulu v1:een; UI:n perusrakennetta ei redesignata.
