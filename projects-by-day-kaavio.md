# Toteutussuunnitelma: “Projects by day” -projektineule

## Tavoite

Nykyistä pylväskaaviota ei korvata eikä muuteta. Sen jälkeen lisätään erillinen “Projects by day” -osio, joka näyttää viimeiset 26 kalenteriviikkoa päiväruudukkona.

- Yksi ruutu vastaa yhtä päivää.
- Aktiivinen ruutu näyttää kaikki kyseisenä päivänä työstetyt projektit.
- Projektit näkyvät yhtä leveinä pystysuuntaisina värikaistaleina.
- Koko ruudun yli piirretään täsmälleen pylväissä jo käytetty neulesilmukkapinta.
- Ruudukko kertoo, mitä projekteja tehtiin. Se ei kuvaa ajan määrää tai korvaa pylväiden määrällistä tietoa.
- Osio näkyy vain `All Time` -aikavälillä ja samalla Pro-oikeudella kuin nykyinen kaavio.
- Sijainti on nykyisen kaavion jälkeen ja ennen “Where the time went” -osiota.

## 1. Työpuun suojaaminen

Ennen toteutusta:

- Tarkista `git status --short` ja kohdetiedostojen nykyiset erot.
- Kohtele nykyistä työpuuta lähteenä; älä palauta vanhaa `InsightsFabric`-versiota tai ylikirjoita käyttäjän keskeneräisiä muutoksia.
- Älä käytä `git reset`, `git checkout --` tai muita palauttavia komentoja.
- Erityisesti nykyisiä `activityCellEmpty`- tai `activityRamp`-muutoksia ei oteta tämän ominaisuuden käyttöön eikä siivota samalla.

## 2. Päiväkohtainen tietomalli

Luo `ui/screens/insights/InsightsProjectFabricModel.kt`.

Lisää vain sisäiset tyypit:

```kotlin
internal const val PROJECT_FABRIC_WEEK_COUNT = 26

internal data class InsightsProjectFabricDay(
    val date: LocalDate,
    val projectIds: List<Long>,
)

internal data class InsightsProjectFabricModel(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val firstDayOfWeek: DayOfWeek,
    val days: List<InsightsProjectFabricDay>,
) {
    val activeDayCount: Int
}
```

Lisää puhdas muodostusfunktio:

```kotlin
internal fun buildInsightsProjectFabric(
    sessions: List<KnitSession>,
    today: LocalDate,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
    projectOrder: List<Long>,
): InsightsProjectFabricModel?
```

Muodostussäännöt:

1. Selvitä nykyisen viikon alku käyttäjän nykyisen lokaalin `firstDayOfWeek`-arvolla.
2. Ruudukon alku on nykyisen viikon alku miinus 25 viikkoa. Näin ruudukossa on aina tasan 26 viikkosaraketta.
3. `endDate` on `today`.
4. Muodosta yksi `InsightsProjectFabricDay` jokaiselle päivälle alusta tähän päivään.
5. Nykyisen viikon tulevia päiviä ei lisätä malliin eikä piirretä tyhjiksi päiviksi.
6. Ryhmittele istunnot ensin projektin mukaan.
7. Jaa projektin istunnot päiville nykyisellä:

```kotlin
SessionMetrics.paceBuckets(
    interval = PaceGroupingInterval.DAY,
    rangeStartMillis = null,
    ...
)
```

`rangeStartMillis` jätetään tässä tarkoituksella `null`-arvoksi. Vasta päiväjaon jälkeen päivät rajataan 26 viikon ikkunaan. Näin yön yli jatkuvat istunnot ja istunnon tallennettu `zoneId` käsitellään samalla tavalla kuin muu Insights-data.

8. Projekti kuuluu päivään, jos sen kyseiselle päivälle jaettu `totalSeconds` on suurempi kuin nolla.
9. Saman projektin useat istunnot samana päivänä tuottavat vain yhden värikaistaleen.
10. Päivän projektit järjestetään `timePerProject.map { it.projectId }` -järjestyksen mukaan. Tuntemattomat tunnisteet tulevat lopuksi projektitunnisteen mukaisessa järjestyksessä.
11. Kaikkien projektien tunnisteet säilytetään. Ei `dominantProjectId`-kenttää, projektimäärän katkaisua, “Other”-ryhmää tai intensiteettitasoja.
12. Funktio palauttaa `null`, jos 26 viikon ikkunassa ei ole yhtään aktiivista päivää.

## 3. ViewModel- ja Pro-rajaus

Muuta `InsightsUiState`-tyyppiin:

```kotlin
val projectFabric: InsightsProjectFabricModel? = null
```

Muodosta malli `InsightsViewModel.buildUiState`-funktiossa vasta sen jälkeen, kun `timePerProject` on laskettu:

```kotlin
val projectFabric =
    if (
        params.timeRange == TimeRange.ALL_TIME &&
        featureGates.canUseCharts
    ) {
        buildInsightsProjectFabric(
            sessions = sessions,
            today = today,
            zone = zone,
            firstDayOfWeek = firstDayOfWeek,
            projectOrder = timePerProject.map { it.projectId },
        )
    } else {
        null
    }
```

Tämä varmistaa:

- Ruudukkoa ei lasketa viikko- tai kuukausinäkymille.
- Projektisuodatus toimii automaattisesti, koska `sessionLoad` sisältää jo joko kaikkien projektien tai valitun projektin istunnot.
- Valitun projektin aktiiviset ruudut ovat yksivärisiä.
- Ei-Pro-käyttäjän `InsightsUiState` ei sisällä ruudukon päiväkohtaista dataa.
- Uutta Pro-kehotetta ei lisätä. Nykyinen kaaviokehote pysyy näkymän ainoana kehotteena.
- Roomia, repository-rajapintoja, tietokantaa tai migraatioita ei muuteta.

## 4. Ruudukon geometria ja piirto

Luo `ui/screens/insights/InsightsProjectFabric.kt`.

Lisää `InsightsDimens`-tiedostoon vain ruudukon tarvitsemat mitat:

- solujen väli `1.dp`
- kuukausinimien korkeus
- valintareunuksen paksuus `1.dp`
- valitun päivän tietorivin yläväli

Älä lisää uusia silmukkageometrian tai -värin tokeneita.

### Kalenteriasettelu

- Sarakkeita on aina 26.
- Rivejä on seitsemän.
- Sarake on viikko ja rivi on viikonpäivä.
- Ensimmäinen rivi määräytyy lokaalin `firstDayOfWeek`-arvosta.
- Solun koko lasketaan käytettävissä olevasta leveydestä:

```text
cellSize = (availableWidth - 25 × gap) / 26
```

- Ruudukko ei vierity vaakasuunnassa.
- Nykyisen viikon tulevat päivät jätetään kokonaan piirtämättä.
- Tyhjä mennyt päivä piirretään vain hillityllä nykyiseen teemaan perustuvalla ääriviivalla.
- Yläreunaan piirretään lokalisoidut lyhyet kuukausinimet. Ensimmäisen näkyvän kuukauden nimi alkaa ensimmäisestä sarakkeesta, ja seuraavat nimet sijoitetaan sen viikon kohdalle, jossa kuukausi vaihtuu.
- Viikonpäivätekstejä ei lisätä, jotta 26 sarakkeen neulepinta säilyy riittävän leveänä.

### Projektivärien jako

Aktiivisen päivän ruudulle:

1. Hae jokaiselle `projectId`:lle nykyinen väri:

```kotlin
yarnColorForId(projectId, MaterialTheme.knitToolsColors.yarnPalette)
```

2. Jaa ruudun leveys projektien lukumäärällä.
3. Piirrä yhtä leveät pystysuuntaiset värikaistaleet ilman välejä.
4. Ensimmäinen kaistale alkaa täsmälleen ruudun vasemmasta reunasta ja viimeinen päättyy täsmälleen oikeaan reunaan.
5. Viimeinen kaistale saa mahdollisen liukulukupyöristyksen jäännöksen, jotta reunoille ei jää yhden pikselin rakoa.
6. Projektien käyttämää aikaa ei käytetä kaistaleiden leveyteen, väriin, alfaan tai intensiteettiin.

### Täsmälleen sama neulepinta kuin pylväissä

Ruudukkoon ei tehdä omaa V-kuviota.

- Käytä suoraan nykyistä `knitStitchLattice`-funktiota.
- Käytä samoja `InsightsDimens.ChartStitchTargetWidth`-, `ChartStitchAspect`-, `ChartStitchStrokeRatio`- ja `ChartStitchShadowAlpha`-arvoja.
- Varjon väri on sama `MaterialTheme.colorScheme.scrim` samalla alfalla.
- Viivan pää on sama `StrokeCap.Round`.
- Solun taustalle piirretään ensin kaikki projektivärikaistaleet.
- Sen jälkeen yksi yhteinen silmukkapolku piirretään koko ruudun yli.
- Polku leikataan ruudun muotoon. Näin silmukat jatkuvat värirajojen poikki samalla tavalla kuin nykyisessä `InsightsProjectMixBar`-palkissa.
- Ruudun kulmissa käytetään nykyistä `InsightsDimens.ChartBarCorner`-pyöristystä.
- Sama yhdelle solukoolle muodostettu `KnitStitchLattice` muistetaan ja siirretään jokaiseen aktiiviseen soluun; polkua ei rakenneta uudelleen jokaiselle päivälle.
- Tyhjään päivään ei piirretä neulepintaa.

Nykyiseen `InsightsChart`-pylväsrenderöintiin ei tehdä muutoksia.

## 5. Sijoitus ja valintatila

Lisää `InsightsScreen`-tasolle pylväsvalinnasta riippumaton tila:

```kotlin
var selectedFabricDate by remember { mutableStateOf<LocalDate?>(null) }
```

Nollaa valinta, kun:

- aikaväli vaihtuu
- projektisuodatin vaihtuu
- uusi malli ei enää sisällä valittua päivää

Lisää `projectFabricSection` välittömästi nykyisen `chartSection`-kutsun jälkeen ja ennen `timePerProject`-/“Where the time went” -haaraa.

Osion renderöintiehto on yksinomaan:

```kotlin
uiState.projectFabric != null
```

Se ei riipu `hasMeaningfulChartData`-arvosta. Näin ruudukko voi näyttää yhdenkin aktiivisen päivän, vaikka yhden pylvään kaavio jätetään nykyisen säännön mukaisesti pois.

### Otsikko ja valitun päivän tiedot

- Otsikon resurssinimi: `insights_section_projects_by_day`
- Englanti: `Projects by day`
- Suomi: `Projektit päivittäin`

Kun mitään päivää ei ole valittu:

- otsikon metatieto näyttää ruudukon oman näkyvän aikavälin, ei koko All Time -historian aikaväliä

Kun päivä valitaan:

- otsikon metatieto vaihtuu lokalisoituun päivämäärään
- otsikon alle ilmestyy yksi tarvittaessa rivittyvä tekstirivi
- aktiivisena päivänä rivi sisältää kaikkien päivän projektien nimet samassa järjestyksessä kuin värikaistaleet
- puuttuvalle nimelle käytetään nykyistä `new_project_name_format`-varatekstiä
- tyhjänä päivänä näytetään lokalisoitu “No tracked activity” -teksti
- projektien minuuttimääriä ei näytetä tässä osiossa

Valittu ruutu merkitään teemaan perustuvalla yhden dp:n reunuksella. Muiden ruutujen värejä ei himmennetä tai muuteta.

## 6. Kosketus ja saavutettavuus

### Kosketus

- Koko ruudukko on yksi Canvas-kosketusalue.
- Painalluksen sijainti muunnetaan viikko- ja viikonpäiväindeksiksi.
- Kuukausinimien alueella, solujen väleissä ja nykyhetken jälkeisissä soluissa tapahtuvat painallukset ohitetaan.
- Menneen tyhjän päivän voi valita, jolloin käyttäjä näkee päivämäärän ja “No tracked activity” -tiedon.
- Pyyhkäisyä, haptiikkaa, erillisiä päiväkohtaisia painikkeita tai uusia ruutuvalikoita ei lisätä.

### TalkBack

Yksittäisistä noin 12 dp:n soluista ei tehdä erillisiä saavutettavuuskohteita. Sen sijaan koko ruudukko muodostaa yhden riittävän suuren semanttisen kohteen.

Sen sisältökuvaus kertoo:

- että kyseessä ovat projektit päivittäin
- näkyvän alku- ja loppupäivän
- aktiivisten päivien määrän

Lisää kaksi lokalisoitua mukautettua toimintoa:

- “Previous active day”
- “Next active day”

Toiminnot kiertävät vain aktiivisiä päiviä. Jos valintaa ei vielä ole:

- edellinen valitsee viimeisimmän aktiivisen päivän
- seuraava valitsee ensimmäisen aktiivisen päivän

Valinnan jälkeen päivämäärä ja kaikki projektien nimet välitetään olemassa olevan `InsightsSectionHeader(metaIsLive = true)` -käytännön ja valitun päivän tietorivin semantiikan kautta. Projektiväri ei jää ainoaksi tiedonvälitystavaksi.

## 7. Lokalisaatio

Lisää uudet tekstit kaikkiin nykyisiin resurssihakemistoihin:

- `values`
- `values-da`
- `values-de`
- `values-es`
- `values-fi`
- `values-fr`
- `values-it`
- `values-nb`
- `values-nl`
- `values-pt`
- `values-sv`

Tarvittavat uudet merkitykset:

- osion otsikko
- ruudukon saavutettavuusyhteenveto
- ei seurattua toimintaa
- edellinen aktiivinen päivä
- seuraava aktiivinen päivä

Päivämäärät, kuukaudet ja aikaväli muotoillaan nykyisellä lokaali- ja päivämääräinfrastruktuurilla. Uusia käsin rakennettuja päivämäärämuotoja ei lisätä.

## 8. Testit

### `InsightsProjectFabricModelTest`

Lisää testit, jotka todistavat:

1. Ruudukko alkaa nykyisen viikon alusta miinus 25 viikkoa.
2. Viikko alkaa maanantaina maanantaialueella ja sunnuntaina sunnuntaialueella.
3. Malli sisältää menneet päivät tämän päivän mukaan lukien mutta ei nykyisen viikon tulevia päiviä.
4. Yhden päivän kaikki projektit säilyvät.
5. Kahden, kolmen, viiden ja yli viiden projektin päivää ei typistetä.
6. Saman projektin useat istunnot tuottavat yhden tunnisteen.
7. Projektijärjestys seuraa annettua `projectOrder`-listaa riippumatta istuntojen järjestyksestä.
8. Yön yli jatkuva istunto lisää projektin molemmille päiville.
9. Istunnon tallennettu `zoneId` määrää päiväjaon.
10. Ikkunan ulkopuoliset päivät jätetään pois.
11. Malli palauttaa `null`, kun ikkunassa ei ole positiivista seurattua aikaa.
12. Valitun projektin aineisto tuottaa yksiväriset aktiiviset päivät.

### `InsightsProjectFabricGeometryTest`

Testaa puhtaat geometriatoiminnot:

- 1, 2, 3, 5 ja 20 värikaistaletta kattavat ruudun täsmälleen vasemmasta oikeaan
- kaistaleiden välissä ei ole aukkoja tai päällekkäisyyksiä
- kaistaleet ovat yhtä leveitä liukulukutarkkuuden rajoissa
- kosketus osuu oikeaan päivään
- solujen väli, kuukausirivi ja tuleva päivä eivät valitse päivää
- lokaalin ensimmäinen viikonpäivä tuottaa oikean rivin

### Nykyisten testien laajennukset

- `InsightsViewModelTest`: malli syntyy vain `ALL_TIME + canUseCharts` -yhdistelmällä ja käyttää samaa projektijärjestystä kuin pylväät.
- `InsightsRedesignSourceTest`: järjestys on pylväskaavio → projektiruudukko → “Where the time went”; ruudukko ei korvaa kaaviota.
- `InsightsPerformanceSourceTest`: laskenta säilyy IO-dispatcherilla ja näkymä kerää edelleen yhden `InsightsUiState`-virran.
- `InsightsKnitLatticeTest`: ruudukko käyttää `knitStitchLattice`-funktiota ja samoja neulepinnan tokeneita.
- `InsightsCopySourceTest`: kaikki uudet resurssit löytyvät jokaisesta 11 lokaalikansiosta.
- `YarnPaletteContrastTest` ja `InsightsContrastTest`: nykyiset projektivärit ja valintareunus säilyttävät kontrastivaatimukset.

Lisää lähdekooditesti tai täsmällinen kielteinen tarkistus, joka estää seuraavien palaamisen:

- `dominantProjectId`
- ruudun aktiivisuuteen perustuva `level`
- `activityRamp`
- oma `FabricStitch...`-geometria
- projektien katkaiseminen tai “Other”-segmentti

## 9. Varmistus

Suorita ensin kohdennetut testit:

```powershell
.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.finnvek.knittools.ui.screens.insights.InsightsProjectFabricModelTest" `
  --tests "com.finnvek.knittools.ui.screens.insights.InsightsProjectFabricGeometryTest" `
  --tests "com.finnvek.knittools.ui.screens.insights.InsightsViewModelTest" `
  --tests "com.finnvek.knittools.ui.screens.insights.InsightsKnitLatticeTest" `
  --tests "com.finnvek.knittools.ui.screens.insights.InsightsRedesignSourceTest" `
  --tests "com.finnvek.knittools.ui.screens.insights.InsightsPerformanceSourceTest" `
  --tests "com.finnvek.knittools.ui.screens.insights.InsightsCopySourceTest" `
  --tests "com.finnvek.knittools.ui.theme.YarnPaletteContrastTest" `
  --tests "com.finnvek.knittools.ui.theme.InsightsContrastTest"
```

Sen jälkeen:

```powershell
.\gradlew.bat :app:compileDebugKotlin
git diff --check
```

Lopuksi tarkista debug-versio laitteella:

- vaalea ja tumma teema
- kaikki projektit sekä yksi valittu projekti
- maanantaista ja sunnuntaista alkava lokaali
- tyhjä, yksi, kaksi, kolme, viisi ja useampi projekti samana päivänä
- hyvin harva ja koko 26 viikon aineisto
- nykyisen viikon tulevien solujen puuttuminen
- valinta, valinnan nollautuminen ja pitkien projektinimien rivittyminen
- TalkBack-yhteenveto sekä edellinen/seuraava aktiivinen päivä

Staattisia testejä tai onnistunutta käännöstä ei raportoida laite- tai TalkBack-varmistuksena ilman erillistä laitetarkistusta.

## Rajaukset

- Nykyinen pylväskaavio, sen valinta, asteikko ja data pysyvät ennallaan.
- “Where the time went” -palkki ja projektilista pysyvät ennallaan.
- Ruudukko ei näytä minuutteja, rivejä, intensiteettiä tai projektien keskinäisiä aikaosuuksia.
- Counter-, projektikortti- ja Session History -näkymiin ei lisätä mitään.
- Ei uusia riippuvuuksia, tietokantamuutoksia, repository-rajapintoja tai taustaprosesseja.
- Ei kuvaa tai uutta mockupia.
