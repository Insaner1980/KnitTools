# KnitToolsin Fable UI -uudistus ja ennen/jälkeen-version säilytys

## Yhteenveto

Toteutetaan Fablen koko raportti vain niiltä osin, jotka nykyinen lähdekoodi tai Pixel 9 -vertailu vahvistaa. Työ tehdään erillisessä `codex/fable-ui-uudistus`-haarassa ja `C:\Dev\KnitTools-fable-ui`-worktreessä. Nykyinen versio säilytetään tagilla, APK:lla, tarkistussummalla ja kuvakaappauksilla; valmis versio tallennetaan samalla tavalla.

Counterin nykyinen identiteetti, WebP-painikkeet, Outfit-fontti, 24 dp vaakamarginaali ja ensimmäisen viewportin rakenne säilyvät.

## 1. Lähtöversion jäädytys

- Tarkista nykyiset `PROJECT.md`- ja `ArchitectureSingleSourceSourceTest.kt`-muutokset kohdennetulla testillä ja commitoi ne omana suomenkielisenä baseline-committina.
- Aja `assembleDebug`, kopioi APK nimellä `KnitTools-ui-before-fable.apk` hakemistoon `C:\Dev\KnitTools-ui-comparison\before` ja tallenna SHA-256.
- Asenna baseline-APK Pixel 9:ään nykyiset sovellustiedot säilyttäen ja kuvaa raportin yhdeksän auditoitua näkymää.
- Luo ja pushaa annotoitu tagi `ui-before-fable`.
- Luo tagista haara `codex/fable-ui-uudistus` ja worktree `C:\Dev\KnitTools-fable-ui`. Alkuperäinen `C:\Dev\KnitTools` jää baseline-versioon. Menettely perustuu Gitin [worktree-malliin](https://git-scm.com/docs/git-worktree.html).

## 2. Design-system ja sisäiset rajapinnat

- Lisää yleiset semanttiset mitat `Dimens.kt`:hon: 4/8/16/24/32 dp spacing, 12 dp listaväli, 16 dp korttisisus, 48 dp minimitouch ja 88 dp FAB-clearance. Counter-kohtaiset arvot jäävät `CounterDimens`-lähteeseen, mukaan lukien 24 dp screen padding.
- Säilytä nykyiset 8/12/16 dp Material-muodot. Lisää yhteinen 24 dp yläkulmien `SheetShape`; listakortit käyttävät 12 dp ja counterin sisältökortit 16 dp muotoa.
- Laajenna `KnitToolsExtendedColors`-teemaa:
  - `accentTextPrimary`: tumma `#D4722A`, vaalea `#943A00`.
  - `inputPlaceholder`: tumma nykyinen muted-rooli, vaalea täyskontrastinen `onSurface`.
  - `modalContainer`: tumma `surfaceContainer`, vaalea cream/background.
  - teeman mukaan ratkaistava `InsightChartPalette`.
- Nosta tumma divider väriin `#4A4A38`. Älä vaihda `LightTextMuted`-väriä Fablen `#6B6552`:een, koska se heikentäisi pienen tekstin kontrastin alle 4,5:1 useilla vaaleilla pinnoilla.
- Reititä kaikki alle 18 sp oranssit tekstit `accentTextPrimary`-rooliin. Säilytä raw `primary` täytöissä, FABeissa, kytkimissä, ikoneissa ja vähintään 22 sp otsikoissa.
- Lisää keskitetyt `NavLabel`- ja section-label-tyylit ilman paikallisia `fontSize`, `letterSpacing` tai `fontWeight`-ylikirjoituksia.
- Lisää tekstipohjainen yhteinen `SectionLabel`; nykyinen ikonillinen `SectionHeader` säilyy eri käyttötarkoituksena.
- Muuta `TopLevelDestination` sisältämään erilliset selected/unselected-ikonit. Inaktiiviset ovat outlined ja aktiiviset filled.
- Dokumentoi värien viisipaikkainen aksenttijärjestys visuaalisena, ei semanttisena sääntönä.

## 3. Komponentit ja näkymät

- Navigaatio:
  - ota nykyinen `navBarIndicator` oikeasti käyttöön;
  - käytä `accentTextPrimary`-aktiivitekstiä;
  - säilytä nykyinen yhteinen label-mittaus ja fontin automaattinen sovitus;
  - varmista suomi ja saksa yhdellä rivillä.
- Projects:
  - lisää LazyColumnin 88 dp alapadding ja yhtenäistä listaväli 12 dp:hen;
  - käytä yhteistä section-labelia;
  - keskitä trailing count ja chevron yhtenäisesti;
  - säilytä nollaluvun nykyinen hillitty käsittely, ellei laitevertailu osoita epäjohdonmukaisuutta.
- Tools ja Library:
  - jatka nykyisen yhteisen `HubListItem`-komponentin käyttöä;
  - yhtenäistä osio-otsikot olive/secondary-tyyliin;
  - tee “Unlock all tools” selväksi tekstipainikkeeksi ja käytä kontrastikelpoista aksenttitekstiä.
- Counter:
  - säilytä 24 dp vaakamarginaali ja nykyinen hero-rakenne;
  - muuta sisältökorttien radius 16 dp:hen ja käytä aina noun-muotoista “Pattern”-otsikkoa kaikissa kielissä;
  - yhtenäistä REPEAT- ja STITCHES-labelit uppercase/section-label-perheeseen yhdistämättä rakenteellisesti erilaisia pillereitä;
  - säilytä jo oikeat 48–56 dp stepper- ja overflow-kosketusalueet;
  - vahvista WebP-painikkeiden nykyinen painallus 0,96-skaalaukseen ja lisää näkyvä näppäimistö-/switch-focus-rengas ilman assettien vaihtamista;
  - palauta Compose-piirrettyihin steppereihin normaali ripple/focus-indikaatio.
- Top bar:
  - lisää pinned scroll behavior ja scrolled-container + divider Counter-, Projects-, Tools-, Library- ja Insights-näkymiin sekä yhteiseen `ToolScreenScaffold`iin;
  - älä muuta immersiivistä PDF-vieweria tai editorin erityisiä top bareja ilman vastaavaa päällekkäisyysongelmaa. Toteutus seuraa Androidin [virallista app bar -mallia](https://developer.android.com/develop/ui/compose/components/app-bars).
- Project actions sheet ja muut modalit:
  - käytä yhteistä `SheetShape`- ja `modalContainer`-roolia kaikissa bottom sheeteissä;
  - käytä samaa modal-container-sääntöä dialogeissa ja dropdown-menuissa;
  - vaihda Project details-, Reset- ja Add counter -ikonit toisistaan erottuviksi outlined-ikoneiksi;
  - lisää oikea divider Complete- ja Delete-toimintojen väliin;
  - säilytä olemassa oleva section-jako, rivikorkeus ja vaalean teeman cream-pinta. Material 3 tukee eksplisiittisiä shape- ja containerColor-arvoja [ModalBottomSheetissä](https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalBottomSheet.composable) ja [AlertDialogissa](https://developer.android.com/reference/kotlin/androidx/compose/material3/AlertDialog.composable).
- Needle Sizes:
  - muuta header `stickyHeader`-rakenteeksi;
  - aseta datarivin vähimmäiskorkeus 52 dp;
  - kohdista disclaimerin info-painike tekstin alkuun;
  - käytä vaaleassa teemassa kontrastikelpoista placeholder-roolia.
- Insights:
  - lisää projektivalintaan dropdown-caret;
  - tee valitun ja valitsemattoman aikafiltterin tilat selvästi erilaisiksi;
  - nimeä keskimmäinen pace-arvo esimerkiksi “Latest 43 r/hr”, jotta se ei näytä irralliselta datapisteeltä;
  - lisää 50 % gridline ja käytä korjattua divider-roolia baselineen ja heatmapin tyhjiin soluihin;
  - siirrä chart-värit theme-resolved-palettiin.
- Vähäriskiset optional-kohdat toteutetaan vain, jos baseline-kuva vahvistaa ongelman: metatietojen erottimet ja chevronin optinen keskitys. Makuun perustuvia tekstin tai värin vaihtoja ei tehdä ilman näkyvää hyötyä.

## 4. Testaus ja hyväksyntä

- Lisää kontrastitestit `accentTextPrimary`- ja placeholder-rooleille niiden todellisia taustoja vasten; pieni teksti vaatii vähintään 4,5:1.
- Päivitä navigaation, sheetin, counter-painikkeiden ja source-contractien testit vastaamaan uusia tarkoituksellisia sopimuksia.
- Lisää Compose/instrumentaatiotarkistukset:
  - interaktiiviset kohteet vähintään 48 dp;
  - aktiivinen navigaatiokohde erottuu myös ilman väriä;
  - counterin custom-painikkeilla on button-semantics ja näkyvä fokus;
  - project sheetissä Delete on erillisessä ryhmässä.
- Androidin virallinen saavutettavuusohje edellyttää 48 dp interaktiivista aluetta ja suosittelee Compose-accessibility-tarkistuksia kontrastille, touch targeteille ja traversalille: [API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [testing](https://developer.android.com/develop/ui/compose/accessibility/testing).
- Aja pienimmästä laajimpaan:
  1. kohdennetut UI- ja source-contract-testit;
  2. `:app:testDebugUnitTest`;
  3. `:app:ktlintCheck` ja `:app:detekt`;
  4. `lint`;
  5. `assembleDebug`.
- Älä aja käyttäjän `lc`- tai `sc`-wrappereita.
- Pixel 9 -visuaalimatriisi:
  - tumma: Projects, Tools, Library, Needle Sizes, Counter first viewport, Counter project cards, Counter scrolled, Insights ja overflow sheet;
  - vaalea: Projects, Tools, Library, Needle Sizes/search, Counter first viewport, Insights ja overflow sheet;
  - tarkista lisäksi suomi ja saksa, normaali ja suurennettu fontti sekä D-pad/switch-focus.
- Hyväksyntäehdot:
  - ei alle 4,5:1 pientä oranssia tai placeholder-tekstiä;
  - FAB ei peitä viimeistä listakohdetta;
  - aktiivinen navigaatio erottuu värin lisäksi indikaattorilla ja ikonilla;
  - top bar ei törmää skrollattuun sisältöön;
  - Delete on visuaalisesti erotettu;
  - counterin ensimmäisen viewportin hierarkia ja 24 dp hengittävyys eivät heikkene;
  - light ja dark näyttävät samalta design-järjestelmältä.

## 5. Jälkiversio ja toimitus

- Tee muutokset pieninä suomenkielisinä committeina: theme/tokenit, yhteiset komponentit, navigaatio/modalit, näkymäkohtaiset parannukset sekä testit.
- Rakenna lopullinen APK, kopioi se nimellä `KnitTools-ui-after-fable-v1.apk` hakemistoon `C:\Dev\KnitTools-ui-comparison\after` ja tallenna SHA-256.
- Luo annotoitu tagi `ui-after-fable-v1`.
- Pushaa feature-haara sekä molemmat tagit originiin.
- Säilytä kuvamatriisi ja APK:t vertailuhakemistossa repositorion ulkopuolella; APK-binaareja tai paikallisia Firebase/Sentry-tiedostoja ei commitoida.
- UI-muutos ei muuta moduuleja, data flow’ta, Roomia tai vastuurajoja, joten `AGENTS.md`/`CODEX.md`-arkkitehtuuripäivitystä ei tehdä. Fablen virheelliset tai jo toteutetut ehdotukset kirjataan lopputuloksessa tarkoituksellisiksi non-change-kohdiksi.
