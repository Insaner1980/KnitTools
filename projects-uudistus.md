# KnitTools Projects -näkymän toteutussuunnitelma

## Yhteenveto

Projects muutetaan suurista geneerisistä korteista kortittomaksi projektikirjaksi. Continue-hero säilyy erillisenä toimintona, aktiiviset projektit tiivistyvät tavoitetietoisiksi riveiksi ja nykyiset yarn-, pattern-, note- ja photo-oikotiet säilyvät yhdellä alarivillä.

Tietoja ei eroteta keskellä olevalla pallolla. Projektiriveille ei tule värillistä reunaa, sivuraitaa, kehystä tai muuta reunakorostusta. Rakenteen muodostavat typografia, sarakkeet, tyhjä tila ja projektien väliset neutraalit vaakaviivat.

Tietokantaa, navigaatioreittejä, projektitiloja tai lajitteluvaihtoehtoja ei muuteta.

## Ehdottomat suunnittelusäännöt

- Älä lisää värillistä reunaa, vasenta aksenttiraitaa, värillistä kehystä tai koristeellista reunakorostusta mihinkään komponenttiin.
- Älä käytä U+00B7 MIDDLE DOT -merkkiä tietojen erottimena.
- Erota eri tiedot omilla riveillä, sarakkeilla, kohdistuksella tai tyhjällä tilalla.
- Älä lisää Planned-tilaa, uusia suodattimia tai “lähimpänä valmista” -lajittelua.
- Säilytä nykyinen vaalea ja tumma väripaletti. Älä lisää projektikohtaisia tunnistevärejä.
- Älä käytä lankakortin tunnisteesta johdettua väriä langan todellisena värinä.
- Älä palauta chevronia projektirivin oikeaan reunaan.
- Älä kiinnitä projektiriville kiinteää korkeutta. Sisällön ja fonttiskaalan pitää saada kasvattaa riviä.
- Säilytä käyttäjän kaikki nykyiset likaiset työpuumuutokset. Älä palauta, korvaa tai formattoi tehtävän ulkopuolisia tiedostoja.
- Älä committoi tai puske ilman erillistä pyyntöä.

## Rajapinta- ja mallimuutokset

- `ProjectListScreen`-composablen ulkoinen callback-rajapinta säilyy ennallaan.
- `ContinueKnittingProject.totalMinutes` poistetaan, koska hero ei enää näytä työaikaa.
- `ProjectListViewModel.updateContinueKnitting` lopettaa `getTotalMinutesForProject`-haun.
- `ProjectListViewModel.proState` ja `projectCount` poistetaan, jos niille ei jää muuta kuluttajaa näkyvän PRO-merkin poistamisen jälkeen.
- Nykyinen laskuritavoitteen helper-logiikka siirretään Counter-näytöstä jaettuun sisäiseen rajapintaan:
  - `MainCounterTargetStatus.Remaining`
  - `MainCounterTargetStatus.Reached`
  - `MainCounterTargetStatus.Past`
  - `mainCounterTargetStatus(targetLine)`
  - `mainCounterTargetFraction(targetLine)`
- `ProjectCard` nimetään `ProjectListItem`-komponentiksi ja tiedosto vastaavasti `ProjectListItem.kt`:ksi, koska komponentti ei enää ole kortti.

## Toteutusvaiheet

### 1. Tallenna pysyvä käyttöliittymäpreferenssi

Luo ennen repositoriomuutoksia tiedosto:

`C:\Users\EmmaH\.codex\memories\extensions\ad_hoc\notes\<timestamp>-no-colored-edges-or-middle-dot-separators.md`

Sisällön pitää kirjata globaalina käyttäjäpreferenssinä:

- Älä lisää kortteihin, riveihin, osioihin tai muihin käyttöliittymäelementteihin värillistä reunaa, aksenttiraitaa, sivuraitaa tai koristeellista värikehystä.
- Älä käytä U+00B7 MIDDLE DOT -merkkiä tietojen erottimena.
- Erota tiedot ensisijaisesti asemoinnilla, riveillä, sarakkeilla, tyhjällä tilalla tai kieleen sopivalla välimerkillä.

Älä muokkaa varsinaisia muistirekisteritiedostoja.

### 2. Keskitä tavoitteen tila ja etenemislaskenta

Muokkaa `MainCounterDisplayText.kt`:ta ja siirrä sinne `CounterWorkspaceSections.kt`:ssa nykyisin oleva tavoitteen helper-logiikka.

Toteuta seuraavat tilat:

- Ei tavoitetta tai tavoite ei ole positiivinen:
  - status `null`
  - fraction `null`
- Nykyinen laskuri alle tavoitteen:
  - `Remaining` sisältää jäljellä olevan lukumäärän ja laskurin label-tyypin
  - fraction on `count / target`
- Nykyinen laskuri täsmälleen tavoitteessa:
  - `Reached`
  - fraction `1f`
- Nykyinen laskuri yli tavoitteen:
  - `Past` sisältää ylityksen
  - fraction pysyy arvossa `1f`
- Negatiivinen nykyarvo ei saa tuottaa negatiivista viivan leveyttä.
- Fraction rajataan aina välille `0f..1f`.

Counter-näytön nykyinen näkyvä teksti ja toiminta eivät saa muuttua. Se käyttää jaettua statusmallia mutta säilyttää yksiköllisen tekstin, kuten `92 rows left`.

Projects käyttää samaa statusmallia mutta antaa jäljellä olevan määrän resurssille pelkkänä lokalisoituna lukuna. Näin vasemmalla voidaan näyttää `Round 18 / 40` ja oikealla `22 left` ilman yksikön toistamista.

Päivitä nykyinen `CounterTargetHelperTextTest` ja siirrä se jaetun rajapinnan pakettiin. Lisää tapaukset:

- null-tavoite
- nollatavoite
- 0 / 40
- 18 / 40
- 40 / 40
- 43 / 40
- negatiivinen nykyarvo

### 3. Toteuta kortiton `ProjectListItem`

Nimeä `ProjectCard.kt` ja sen pääcomposable `ProjectListItem`-muotoon. Päivitä vain sen todelliset kutsukohdat ja testit.

Rivin normaali rakenne:

1. Ylärivi:
   - projektin nimi vasemmalla, `titleLarge`
   - `Updated 2 weeks ago` oikealla, `bodySmall`
2. Kontekstirivi:
   - trimmattu `sectionName` vasemmalla
   - jos osiota ei ole, käytä nykyistä pattern-fallbackia
   - raw PDF -tiedostonimeä tai projektin kanssa samaa pattern-nimeä ei edelleenkään näytetä
   - craft type oikealla
3. Etenemisrivi:
   - tavoitteen kanssa lokalisoitu `Row 18 / 40`, `Round 18 / 40`, repeat tai custom label vasemmalla
   - status oikealla: `22 left`, `Target reached` tai `3 past target`
   - ilman tavoitetta vain lokalisoitu nykyinen laskuri vasemmalla
4. Lankaviiva:
   - näytetään vain positiivisen tavoitteen kanssa
   - korkeus 4 dp
   - neutraali track käyttää nykyistä tekstiväriä hyvin matalalla alfalla
   - täyttö käyttää `primary`-väriä
   - viiva on sisällön sisällä ja irti rivin ulkoreunoista
   - ei tekstuuria, päätemerkkiä, animaatiohehkua tai erillistä väritettyä reunaa
5. Alarivi:
   - langan nimi vasemmalla, yksi rivi ja ellipsis tarvittaessa
   - ei väripistettä
   - vain olemassa olevat pattern-, note- ja photo-toiminnot oikealla
   - photo näyttää lukumäärän
   - näkyvät ikonit 18–20 dp
   - jokaisen toiminnon kosketuskohde vähintään 48 × 48 dp

Projektin nimi on aina laskurilukua visuaalisesti suurempi. Käytä olemassa olevia typografiaroolitokeneita ilman uusia inline-fonttikokoja:

- nimi `titleLarge`
- tavoitelukema `titleMedium`
- osio `bodyMedium`
- craft type ja tuoreus `bodySmall`
- status `labelLarge`
- lanka `bodySmall`

Rivin ympärille ei tule `Surface`-korttia, täyttöväriä, varjoa, pyöristettyä korttimuotoa tai reunusta. Valintatilassa sallitaan koko rivin hillitty täyttösävy yhdessä checkboxin ja `selected`-semantiikan kanssa, mutta ei värillistä reunaa.

Projektien väliin lisätään `HorizontalDivider`:

- paksuus 1 dp
- neutraali `onSurface` tai `onBackground`
- alfa noin 0,15
- vain rivien väliin, ei listan tai yksittäisen rivin ympärille

Mukautuminen:

- Nimi saa käyttää kahta riviä.
- Tuoreusteksti pysyy kokonaisena.
- Kun leveys on alle 320 dp tai fontScale vähintään 1,3, tuoreusteksti siirtyy nimen alle.
- Samassa kompaktitilassa craft type ja tavoitestatus siirtyvät omille vasemmalle kohdistetuille riveille.
- Langan nimi saa lyhentyä, mutta oikoteiden kosketuskohteet eivät saa kutistua.
- Rivi ei saa käyttää `height`, `requiredHeight` tai muuta sisältöä leikkaavaa kiinteää korkeutta.
- Tavoitekorkeus normaalilla fonttiskaalalla ja täydellä alarivillä on noin 144–160 dp.

### 4. Lisää Projects-näkymän omat mittatokenit

Luo `ui/theme/ProjectListDimens.kt`. Älä sijoita näitä Counter- tai Insights-mittoihin.

Lukitse vähintään seuraavat arvot:

- screen horizontal padding 16 dp
- list top padding 8 dp
- list bottom padding 112 dp
- hero padding 20 dp
- hero action touch size 72 dp
- hero action visual size 64 dp
- project item vertical padding 14 dp
- item line gap 4 dp
- progress group top gap 8 dp
- progress track height 4 dp
- footer top gap 6 dp
- footer action touch size 48 dp
- footer icon size 18 dp
- divider thickness 1 dp
- divider alpha 0,15
- section top spacing 20 dp
- section bottom spacing 8 dp
- create button touch size 72 dp
- create button visual size 64 dp

Desimaalialfa kirjoitetaan Kotlinissa pisteellä. Käyttöliittymäteksteihin ei lisätä erotinmerkkiä.

### 5. Uudista Continue-hero

Säilytä hero vain normaalitilassa ja pidä nykyinen sääntö, joka poistaa saman projektin Active-listasta.

Heron sisältö:

- `CONTINUE KNITTING`, `labelSmall`, primary
- projektin nimi, `titleLarge`
- trimmattu sectionName omalla rivillään, jos se on olemassa
- tavoitelukema vasemmalla
- status oikealla
- sama 4 dp:n lankaviiva kuin projektirivillä
- ilman tavoitetta vain nykyinen laskuri eikä lankaviivaa
- ei kokonaisaikaa
- ei pilkuilla koottua metadatariviä
- ei reunusta

Hero saa käyttää nykyistä `primary`-sävyistä hillittyä taustaa noin 0,10 alfalla ja `MaterialTheme.shapes.large` -muotoa. Tämä on yksi korostettu toimintapinta, ei osa projektirivien korttirakennetta.

Poista:

- `totalMinutes` heron state-mallista
- kokonaisajan repository-haku
- `formatMinutes`
- `DurationDisplayFormatter`-riippuvuus Projects-näytöstä
- nykyinen tasainen gradientti-play-ympyrä
- nykyinen `BorderStroke`

### 6. Luo fyysinen Continue-painike

Käytä nykyistä `counter_plus_button.webp`-tiedostoa visuaalisena lähteenä.

Luo kuvamuokkauksena `drawable-nodpi/counter_continue_button.webp`:

- 500 × 500 px
- WebP
- sama oranssi materiaali, kehämäinen muoto, valaistuksen suunta, syvennys, varjot ja kamerakulma kuin pluspainikkeessa
- keskellä kaiverrettu oikealle osoittava play-kolmio
- ei tekstiä
- ei ylimääräisiä koristeita
- ei lisättyä reunusta
- tausta ja läpinäkyvyys vastaavat nykyistä plusresurssia mahdollisimman tarkasti

Käytä kuvan muokkaamiseen imagegen-työkalua tällä sisällöllä:

> Edit only the center glyph of the existing KnitTools orange counter button. Replace the carved plus with a centered carved right-pointing play triangle. Preserve the rim, recessed center, material, colors, lighting direction, shadow, camera angle, transparency, dimensions, and every other visual detail as closely as possible. Add no text, border, glow, or decoration.

Muunna lopputulos tarvittaessa ImageMagickilla 500 × 500 WebP-muotoon. Älä jätä väliaikaista PNG-tiedostoa versionhallintaan.

Näytä se `CounterImageButton`-komponentilla:

- touch size 72 dp
- visual size 64 dp
- content description `Continue <project name>`
- sama 90 ms painallusanimaatio kuin Row Counterin painikkeilla

### 7. Korvaa New Project FAB fyysisellä pluspainikkeella

Poista Projects-näytöstä `FloatingActionButton`, sen sisäinen `Row` ja näkyvä `ProBadge`.

Käytä `CounterImageButton`-komponenttia nykyisellä `counter_plus_button.webp`-resurssilla:

- touch size 72 dp
- visual size 64 dp
- oikea alakulma 16 dp
- content description nykyisestä `new_project`-resurssista
- näytetään vain normaalitilassa
- piilotetaan multi-select-tilassa

Säilytä muuttamatta:

- `requestProjectCreation`
- ensimmäisen ilmaisen projektin luonti
- Pro-rajan tarkistus
- `ProPromptSheet`
- pending creation -toiminnon säilyttäminen
- trialin jälkeen jatkettava luonti
- navigointi onnistuneesti luotuun projektiin

Poista vain badgea varten lisätyt `proState`- ja `projectCount`-keräykset sekä ViewModelin julkiset flowt, jos niillä ei ole muuta käyttöä.

LazyColumnin 112 dp:n bottom padding varmistaa, ettei pluspainike peitä viimeisen projektin lankaa tai oikoteitä.

### 8. Päivitä osiot ja kaikki listatilat

Section header ottaa tekstin lisäksi näytettävien rivien määrän.

- Normaali tila: Active-luku tarkoittaa herosta pois suodatettujen näkyvien aktiivirivien määrää.
- Multi-select: hero on piilossa ja Active-luku tarkoittaa kaikkia aktiivisia projekteja.
- Jos hero on ainoa aktiivinen projekti, Active-osio jätetään kokonaan pois.
- Kun aktiivisia projekteja ei ole eikä heroa ole, näytä `ACTIVE 0` ja nykyinen tyhjätilateksti.
- Completed käyttää ladattujen completed-rivien määrää.
- Completed-osio näkyy edelleen vain nykyisen asetuksen ollessa päällä.
- Completed-rivi näyttää nimen, section/pattern-kontekstin, craft typen, lopullisen laskurin ja `Completed 2 weeks ago`.
- Completed-riveille ei lisätä uusia yarn- tai attachment-hakuja.

Multi-select:

- checkbox käyttää omaa 48 dp:n johtavaa saraketta
- koko loppurivi togglaa valintaa
- suorat attachment- ja yarn-toiminnot eivät ole fokusoitavia valintatilassa
- progress ja muu projektitieto voivat säilyä näkyvinä
- Continue-hero ja pluspainike ovat piilossa
- nykyinen complete/delete-bottom bar säilyy

### 9. Lokalisoidut tekstit

Lisää kaikkiin nykyisiin 11 `values*`-hakemistoihin seuraavat resurssit idiomaattisina käännöksinä:

- `project_updated_format`
  - default: `Updated %1$s`
- `project_completed_format`
  - default: `Completed %1$s`
- `project_section_count_format`
  - default: `%1$s %2$d`
- `project_continue_content_description`
  - default: `Continue %1$s`

Käytä suhteelliseen aikaan nykyistä Androidin lokalisoitua suhteellisen ajan muotoilua koko aikavälillä. Älä vaihda yli vuorokauden jälkeen takaisin absoluuttiseen päivämäärään.

`updatedAt`-aikaa kutsutaan aina päivitysajaksi, ei työskentelyajaksi. `completedAt` käyttää Completed-tekstiä. Puuttuva `completedAt` käyttää completed-projektin `updatedAt`-aikaa nykyisen fallbackin mukaisesti.

Tarkista, ettei mikään uusi tai muokattu Projects-resurssi sisällä U+00B7 MIDDLE DOT -merkkiä.

### 10. Päivitä ja lisää testit

Päivitä tai nimeä uudelleen nykyiset testit:

- `ProjectCardSourceTest` -> `ProjectListItemSourceTest`
- `CounterTargetHelperTextTest` -> jaetun target-statusmallin testi
- `ProjectListWorkspaceSourceTest`
- `ProjectListViewModelTest`
- locale/source-testit

Testaa vähintään:

- sectionName voittaa pattern-nimen
- raw PDF -nimi ei näy
- projektin kanssa sama pattern-nimi ei näy
- nimi esiintyy ennen kontekstia, progressia ja footeria
- korttikomponentissa ei ole `Surface`-korttia, `BorderStroke`a, chevronia tai YarnColors-riippuvuutta
- Projects-lähteessä ei ole U+00B7 MIDDLE DOT -merkkiä
- langan nimi ja oikotiet ovat samalla rakennerivillä
- kaikki suorat oikotiet säilyvät 48 dp:n kosketuskohteina
- photo count säilyy
- no-target jättää status- ja progress-elementit pois
- remaining, reached ja past-target tuottavat oikean statusmallin
- progress fraction rajataan välille 0–1
- Continue-malli ei sisällä totalMinutes-kenttää
- ViewModel ei kutsu `getTotalMinutesForProject`
- hero-projekti ei toistu normaalissa Active-listassa
- fyysinen continue-resurssi on olemassa ja sitä käytetään
- New Project käyttää `counter_plus_button.webp`-resurssia
- Projects ei enää käytä `FloatingActionButton`ia tai `ProBadge`a
- Pro-prompt ja pending creation -polku säilyvät
- listalla on riittävä bottom padding
- kaikki neljä uutta string-resurssia ovat jokaisessa locale-hakemistossa

### 11. Staattinen ja visuaalinen varmennus

Aja tarkistukset järjestyksessä. Älä käytä `lc`- tai `sc`-wrappereita.

1. Kohdennetut testit:

```powershell
.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.finnvek.knittools.ui.components.MainCounterTargetStatusTest" `
  --tests "com.finnvek.knittools.ui.components.ProjectListItemSourceTest" `
  --tests "com.finnvek.knittools.ui.screens.project.ProjectListWorkspaceSourceTest" `
  --tests "com.finnvek.knittools.ui.screens.project.ProjectListViewModelTest"
```

2. Kaikki debug-unit testit:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

3. Debug-build:

```powershell
.\gradlew.bat :app:assembleDebug
```

4. Tarkista assetti:

```powershell
magick identify app/src/main/res/drawable-nodpi/counter_continue_button.webp
```

Odotus: `WEBP 500x500`.

5. Tarkista vain tehtävän tiedostojen whitespace:

```powershell
git diff --check -- `
  app/src/main/java/com/finnvek/knittools/ui/components `
  app/src/main/java/com/finnvek/knittools/ui/screens/project `
  app/src/main/java/com/finnvek/knittools/ui/theme/ProjectListDimens.kt `
  app/src/main/res `
  app/src/test/java/com/finnvek/knittools/ui/components `
  app/src/test/java/com/finnvek/knittools/ui/screens/project
```

Visuaalinen tarkistus samalla 360 × 800 dp:n puhelinleveydellä:

- vaalea teema
- tumma teema
- hero ja useita aktiiviprojekteja
- projekti tavoitteen kanssa
- projekti ilman tavoitetta
- tavoite saavutettu
- tavoite ylitetty
- pitkä nimi, sectionName ja langan nimi
- pattern, note ja useita photoja
- vain yksi attachment
- ei attachmentteja eikä lankaa
- Completed näkyvissä
- multi-select
- tyhjä Active-lista
- fontScale 1,0
- fontScale 1,3
- fontScale 2,0

Hyväksymiskriteerit:

- Ei värillistä reunaa, sivuraitaa tai värikehystä.
- Ei U+00B7 MIDDLE DOT -erotinta.
- Hero ei näytä työaikaa.
- Projektin nimi on rivin suurin teksti.
- Tavoite, jäljellä oleva määrä ja progress näkyvät ilman välimerkkierotinta.
- Tavoitteeton projekti ei jätä tyhjää progress-aluetta.
- Langan nimi ja suorat oikotiet mahtuvat yhdelle footer-riville normaalilla fonttiskaalalla.
- Pluspainike ei peitä viimeistä projektia.
- Tumma teema ei tarvitse kortin ja taustan välistä kontrastia, koska aktiiviriveillä ei ole korttipintaa.
- Tyypillinen täydet tiedot sisältävä rivi on vähintään noin 20 prosenttia nykyistä korttia matalampi.
- Suuri fonttikoko kasvattaa rivejä ilman tekstin leikkaamista.
- TalkBack löytää ensin projektirivin ja sen jälkeen näkyvät suorat oikotiet.
- Project creation- ja Pro-prompt-polut toimivat kuten ennen.
- Counter-näytön tavoiteteksti ja painikkeet eivät muutu.

## Oletukset

- Käyttöliittymän nykyinen työpuu on toteutushetken lähde totuudelle.
- Continue-projekti määräytyy edelleen nykyisellä ViewModel-logiikalla.
- `updatedAt` on ainoa aktiivirivien tuoreuslähde.
- Uutta session- tai repository-hakua ei lisätä.
- Hero on ainoa täytetty projektipinta; aktiiviset ja completed-projektit ovat kortittomia.
- Fyysinen play-painike lisätään uutena kuvaresurssina, mutta New Project käyttää olemassa olevaa plusresurssia.
- Toteutus ei muuta tietokantaa, Room-skeemaa, navigaatiota, lajittelun persistedValue-arvoja tai Pro-portin liiketoimintalogiikkaa.
