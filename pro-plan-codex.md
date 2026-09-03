# KnitToolsin Pro-kokemuksen toteutussuunnitelma

Status: **päätössuunnitelma ennen toteutusta** — tämä dokumentti ei muuta sovelluskoodia.

Lähde: nykyinen työpuu 20.8.2026. Työpuussa oleva keskeneräinen Insights-uudistus säilytetään eikä sitä palauteta tai korvata tämän työn sivuvaikutuksena.

## 1. Lukittu tuoteperiaate

Pro toteutetaan yhden käyttäjälle ymmärrettävän säännön ympärille:

> Käyttäjän oma sisältö säilyy aina käytettävissä. Pro voi rajoittaa uuden Pro-sisällön luomista ja erillisiä edistyneitä näkymiä, mutta ei aikaisemmin luodun sisällön avaamista, muokkaamista, käyttämistä, poistamista tai palauttamista.

Tästä seuraa:

- Olemassa olevat projektit, muistiinpanot, kuvat, lankakortit, laskurit ja muistutukset toimivat kokeilun päätyttyä.
- Kaikki projektit saa aina avata, muokata, arkistoida, poistaa ja aktivoida uudelleen.
- Ilmaiseksi voi luoda ensimmäisen projektin. Kun yksikin projekti on jo olemassa, uuden projektin luominen vaatii Pron. Arkistointi ei vapauta uutta ilmaista luontia; ainoan projektin poistaminen vapauttaa.
- Kokeilussa luodut useat projektit pysyvät kokeilun jälkeen käytettävinä ja uudelleenaktivoitavina. Raja koskee vain seuraavan uuden projektin luomista.
- Olemassa olevan objektin kenttien täydentäminen kuuluu muokkaamiseen. Esimerkiksi olemassa olevaan lankakorttiin voi lisätä tai vaihtaa kuvan ilman Prota.
- Uuden objektin luominen vaatii Pron silloin, kun kyseinen luontitoiminto kuuluu Prohon.
- Projektin ensimmäisen muistiinpanon luominen on Pro-toiminto. Kun projektille on kerran luotu muistiinpano, sitä saa muokata ja tyhjentää vapaasti; tyhjentäminen ei lukitse seuraavaa kirjoitusta.
- Insights-kaaviot, aktiivisuusputki ja kotinäytön widget ovat Pro-näkymiä, eivät käyttäjän tallennettua sisältöä.
- Tallennettu istuntohistoria ei ole Pro-ominaisuus. Kaikki istunnot ovat aina nähtävissä ja poistettavissa.
- Laskurin sisäinen undo-loki ei ole istuntohistoriaa. Sillä on kaikille sama tekninen säilytysraja, joka ei riipu Pro-oikeudesta.
- Prota ei markkinoida keinotekoisella kiireellä, alennuksilla, epätarkoilla lupauksilla, sumeilla esikatseluilla tai jatkuvilla muistutuksilla.
- Psykologinen tavoite on luottamus: raja kerrotaan ennen toimintoa sen omassa asiayhteydessä ja käyttäjän sisällön säilyminen vakuutetaan selvästi.
- Konversiota koskevia oletuksia ei esitetä faktoina eikä sovellukseen lisätä analytiikkaa tai A/B-testikehystä tämän suunnitelman perusteella.

## 2. Nykytilan lähdeauditointi

Nykyiset portit tarkistetaan toteutuksen alussa uudelleen funktioiden ja reittien perusteella. Rivinumerot eivät ole sopimus, koska työpuussa on keskeneräisiä muutoksia.

Merkinnät:

- **C** = uuden sisällön luontiportti, joka voidaan säilyttää
- **P** = olemassa olevan sisällön pääsy- tai käyttöportti, joka muutetaan
- **T** = oikeustasoon sidottu tietojen poisto, joka erotetaan Prosta
- **X** = hiljainen esto, umpikuja tai myyntipinnan ja toteutuksen ristiriita

| Alue | Nykyinen lähde | Luokka | Nykytila ja vaadittu muutos |
|---|---|---:|---|
| Trial | `pro/TrialManager.refreshTrialState` | **X** | Ensimmäinen käynnistys kirjoittaa aloitusajan. Muutetaan käyttäjän vahvistamaksi aloitukseksi. |
| Projects | `ProjectListViewModel.createProjectInternal` | **C** | Tarkistaa aktiivisten projektien määrän vasta dialogin jälkeen. Siirretään yhteiseen atomiseen kokonaismäärään perustuvaan luontirajaan. |
| Counter project creation | `CounterViewModel.createNewProject` | **C/X** | Käyttää omaa projektimäärää ja voi erota päälistan säännöstä. Ohjataan samaan kirjoitusrajaan. |
| Ravelry project creation | `RavelryViewModel.createProjectFromPattern` | **C/X** | Käyttää erillistä aktiivisten projektien tarkistusta. Ohjataan samaan kirjoitusrajaan. |
| Project reactivation | `ProjectListViewModel.reactivateProject` | — | Pysyy aina sallittuna; sitä ei käytetä uuden projektin kiintiön mittarina. |
| Notes route | `NavGraph`, `ProjectListViewModel.openNotesEditor` | **P** | Olemassa oleva muistiinpano ohjataan nyt Pro-sivulle. Reitti avataan aina ja luonti erotetaan muokkauksesta. |
| Notes data | `NotesEditorViewModel`, `CounterViewModel.setNotes` | **P/X** | Ilmainen tila tyhjentää ladatun tekstin ja estää kirjoituksen. Olemassa oleva muistiinpano ladataan ja sitä saa muokata. |
| Progress photos | `LibraryScreen`, `ProjectListViewModel.openPhotoGallery` | **P** | Galleria portataan kokonaan. Galleria avataan aina; vain uuden kuvan luonti portataan. |
| Extra counters | `CounterWorkspaceSections`, `CounterScreen`, `CounterViewModel.canUseProjectCounter` | **P/X** | Olemassa olevat laskurit piilotetaan ja Counters-painallus voi päätyä tyhjään toimintaan. Lista ja käyttö avataan; vain lisäys portataan. |
| Compact repeat counter | `CounterWorkspaceSections`, `CounterViewModel.incrementSecondary/decrementSecondary` | **P** | Käyttö estyy oikeuden päättyessä eikä nolla-arvo kerro, onko laskuria käytetty. Lisätään pysyvä käyttölippu. |
| Row reminders | `CounterWorkspaceSections`, `CounterViewModel` | **P/X** | Olemassa oleva hälytys, muokkaus, kuittaus ja poisto riippuvat Prosta. Ne avataan; vain uuden muistutuksen luonti portataan. |
| Pattern photo-to-PDF | `PatternPickerSheet` | **X** | Painike disabloituu ilman selitystä. Rivi pysyy painettavana ja avaa asiayhteisen Pro-kehotteen. |
| Save to My Yarn | `YarnManagementSheet`, `CounterViewModel` | **X** | Uuden lankakortin luova toiminto disabloituu ilman selitystä. Se avaa asiayhteisen kehotteen. |
| Session History | `SessionHistoryViewModel`, `SessionHistoryScreen` | **P** | Näyttö rajataan 24 tuntiin. `FULL_HISTORY` ja näkyvyysraja poistetaan. |
| Undo history | `CounterViewModel.pruneHistoryForFree`, `counter_history` | **T** | Undo-lokia poistetaan vain ilmaiselta käyttäjältä. Muutetaan kaikille samaksi tekniseksi 24 tunnin retentioksi. |
| Insights | `InsightsScreen.chartSection`, `InsightsViewModel.buildChartBuckets` | — | Työpuussa oleva vartijakorjaus näyttää nykyisen Pro-pinnan, kun minuutteja on. Sitä ei tehdä uudelleen eikä palauteta; lopullinen kehote sidotaan vähintään kahteen merkitykselliseen ämpäriin. |
| Pro page | `ProUpgradeScreen` | **X** | 240 dp:n tyhjä gradienttihero vie tilaa ja lista näyttää vain osan nykyisistä Pro-ominaisuuksista. Korvataan faktapohjaisella rakenteella. |
| Tools trial banners | `HomeScreen` | **X** | Pysyvä kokeilu- ja `Unlock all tools` -viestintä poistetaan. Kokeilun tunnistettavuus hoidetaan Pro-merkinnöillä ja Settings-statuksella. |

Auditoinnin hyväksyntä ennen toteutusta:

- jokainen `ProFeature`-käyttöpaikka on luokiteltu luonniksi, olemassa olevan sisällön käytöksi tai edistyneeksi näkymäksi
- tavallinen, Counter-, Ravelry-, widget-, share- ja deep link -polku on mukana
- yksikään nykyinen tai käyttäjän keskeneräinen muutos ei katoa
- nykyinen tuotanto- ja testikokoonpano tarkistetaan erikseen; lähdetesti ei yksin todista käyttäytymistä

## 3. Pro-tila ja käyttöoikeusmalli

### Tilamalli

`ProStatus` sisältää:

- `TRIAL_NOT_STARTED`
- `TRIAL_ACTIVE`
- `TRIAL_EXPIRED`
- `PRO_PURCHASED`

Ostettu Pro ohittaa kokeilutilan. Debug-käyttöoikeus säilyy keskitettynä `ProState.hasFeature`-logiikassa muuttamatta `isPro`-arvoa, ostotilaa, kokeilutilaa tai käyttöliittymän ostoväitteitä.

`trial_start_timestamp == 0L` on aloittamattoman kokeilun ainoa pysyvä lähde. Erillistä `trial_started`-booleania ei lisätä.

DataStore säilyttää:

- nykyisen `trial_start_timestamp`-arvon
- kellomanipulaation nykyiset tiedot
- `trial_end_notice_shown`-arvon

`startTrial()` tekee aloitusajan kirjoituksen atomisesti ja palauttaa onnistumis- tai virhetuloksen. Vain käyttäjän vahvistama toiminto kutsuu sitä. Tavallinen käynnistys, widget, Ravelry OAuth -paluu, jaettu Ravelry-linkki tai muu deep link ei aloita kokeilua.

Kokeilu kestää 14 vuorokautta aloitushetkestä. Aktiivisessa tilassa jäljellä olevien päivien määrä pyöristetään ylöspäin, jotta käyttöliittymä ei näytä aktiiviselle kokeilulle nollaa päivää.

Kokeilu säilyy paikallisena ilman maksutapaa, käyttäjätiliä tai uutta backendia. Sovellustietojen tyhjentämiseen tai laitteiden väliseen kokeilun uusimiseen ei lisätä palvelinpuolista estoa tässä työssä.

### Käyttöoikeuksien jako

Yleinen `canUse...` ei saa tarkoittaa sekä uuden luonnin että olemassa olevan sisällön käyttöä. Käyttöpaikoissa erotetaan:

- `canCreate(feature)`: saako uuden Pro-objektin tai ensimmäisen Pro-sisällön luoda
- `canUseAdvancedView(feature)`: saako käyttää Insights-kaaviota, aktiivisuusputkea tai widgetiä
- olemassa olevan tietueen avaaminen ja muokkaaminen ratkaistaan tietueen olemassaolon perusteella, ei `canCreate`-arvolla

Uutta yleistä oikeusabstraktiota ei lisätä, jos nykyinen `ProState.hasFeature` ja selkeä työnkulkukohtainen ehto riittävät. Nimet kuvaavat tarkoitusta käyttöpaikassa.

`FULL_HISTORY` poistetaan `ProFeature`-listalta, ostosivulta ja käyttöliittymäporteista.

`sessions` ja `counter_history` pidetään käsitteellisesti erillään:

- kaikki `sessions`-rivit ovat aina näkyvissä ja käyttäjän poistettavissa
- `counter_history` on sisäinen undo-pino
- undo-pino siivotaan kaikilta käyttäjiltä samalla 24 tunnin retentiolla riippumatta kokeilusta tai ostosta
- undo-retentiota ei mainosteta Pro-etuna

### Room 18: käytön pysyvät liput

Room päivitetään versiosta 17 versioon 18. `counter_projects` saa kaksi ei-nullattavaa boolean-kenttää:

#### `secondaryCounterUsed`

- uuden projektin oletus on `false`
- ensimmäinen todellinen toistolaskurin arvonmuutos asettaa arvon atomisesti `true`:ksi
- arvo pysyy `true`:na myös laskurin palautuessa nollaan
- laskuri näkyy, jos käyttäjällä on oikeus tai `secondaryCounterUsed == true`
- pelkkä näkyminen tai avaaminen ei muuta arvoa

#### `notesCreated`

- uuden projektin oletus on `false`
- ensimmäinen oikeutettu ei-tyhjä tallennus asettaa arvon atomisesti `true`:ksi
- arvo pysyy `true`:na, vaikka muistiinpano tyhjennetään
- kun `notesCreated == true`, muistiinpanoa saa aina muokata ilman Prota
- tyhjän editorin avaaminen tai peruminen ei muuta arvoa

Migraatio 17→18 valitsee sisällön säilymisen:

- kaikki ennen versiota 18 olemassa olleet projektit saavat `secondaryCounterUsed = true`
- kaikki ennen versiota 18 olemassa olleet projektit saavat `notesCreated = true`

Vanha skeema ei pysty varmasti osoittamaan, ettei nollattua toistolaskuria tai tyhjennettyä muistiinpanoa olisi käytetty. Nykyisten projektien konservatiivinen avaaminen on parempi kuin aikaisemman käytön lukitseminen.

Päivitetään entiteetti, domain-malli, mapperit, migraatiorekisteri ja Roomin schema export. `AGENTS.md` ja `CODEX.md` päivitetään yhdessä schemaan 18.

### Projektin luontiraja

Projektiraja perustuu kaikkien ei-poistettujen projektien määrään, ei aktiivisten projektien määrään:

- kokonaismäärä `0`: ensimmäisen projektin saa luoda ilman Prota
- kokonaismäärä vähintään `1`: uuden projektin luominen vaatii Pron
- arkistointi ei pienennä kokonaismäärää
- ainoan projektin poistaminen palauttaa mahdollisuuden luoda ensimmäinen projekti
- olemassa olevien projektien uudelleenaktivointi on aina sallittu

Tarkistus ja projektin lisäys tehdään samassa repository-tason Room-transaktiossa. Tavallinen Projects-luonti, Counter-luonti ja Ravelry-projektin luonti käyttävät samaa kirjoitusmetodia. Nopeat tai rinnakkaiset yritykset eivät voi ohittaa rajaa ViewModelissa tehdyn ennakkotarkistuksen kautta.

## 4. Yhteinen Pro-pyyntö ilman globaalia tilakonetta

Lisätään uudelleenkäytettävä `ProPromptSheet` ja keskitetty esitysmalli. Niitä ei toteuteta yhdeksi sovelluksenlaajuiseksi mielivaltaisia jatkotoimintoja säilyttäväksi isännäksi.

Esitysmalli määrittää:

- pyydetyn `ProFeature`-ominaisuuden
- lähteen, kuten Projects, Photos, Notes, Counters, Reminders, Pattern Camera, Widget tai Insights
- faktapohjaisen otsikon ja selityksen
- onko kyse projektirajasta, uuden sisällön luomisesta vai edistyneestä näkymästä

Kukin navigaatiograafi tai näyttö omistaa oman tyypitetyn `PendingProAction`-tilansa. Mielivaltaisia lambda-funktioita ei tallenneta ViewModeliin tai globaaliin singletoniin.

Käyttäjän toiminta etenee näin:

1. Pro-toiminto näyttää ennalta pienen `PRO`-merkinnän.
2. Painike pysyy painettavana ja avaa asiayhteisen pohjaikkunan.
3. Aloittamattomassa kokeilussa käyttäjä voi käynnistää 14 päivän kokeilun tai perua.
4. Onnistunut aloitus jatkaa alkuperäisen tyypitetyn toiminnon kerran.
5. Päättyneessä kokeilussa pohjaikkuna tarjoaa Pro-sivun tai perumisen.
6. Jos käyttäjä ostaa tai palauttaa oston saman pyynnön aikana, alkuperäinen toiminto jatkuu kerran.
7. Takaisin siirtyminen, peruminen tai reitin poistuminen hävittää odottavan toiminnon.

Suoraa hyppyä ostosivulle ei tehdä lukitusta toiminnosta ilman kontekstiselitystä.

### Kesken olevan luonnin oikeus

Yleistä kaikki dialogit kattavaa `CreationAuthorization`-järjestelmää ei rakenneta.

Kertakäyttöinen oikeus säilytetään vain työnkuluissa, joissa ulkoinen sovellus, aktiviteetin uudelleenluonti tai automaattinen tallennus voi muuten hukata jo aloitetun työn:

- progress photo -kameratyönkulku
- pattern photo-to-PDF -kameratyönkulku
- ensimmäisen muistiinpanon editori sen avoimen editointikerran ajan

Oikeus on ominaisuus- ja työnkulkukohtainen, säilyy tarvittaessa `SavedStateHandle`ssa, kulutetaan onnistuneessa tallennuksessa ja hylätään peruttaessa. Se ei oikeuta seuraavaan luontiin.

Projektin, laskurin, muistutuksen ja lankakortin tavalliset dialogit tarkistavat oikeuden tallennusrajalla. Jos oikeus on ehtinyt päättyä, syötetyt arvot säilyvät, Pro-kehote avautuu ja hyväksytty kokeilu tai osto jatkaa saman tallennuksen kerran.

## 5. Näyttökohtainen käyttäytyminen

| Alue | Aina käytettävissä | Prohon kuuluva uusi toiminto |
|---|---|---|
| Projects | Kaikkien projektien avaaminen, muokkaus, arkistointi, palautus, poistaminen ja uudelleenaktivointi | Uuden projektin luominen, kun yksi tai useampi projekti on jo olemassa |
| Ravelry | Tallennettujen mallien selaaminen, tuonti ja liittäminen projektiin | Uuden projektin luominen Ravelry-mallista saman projektirajan mukaisesti |
| Project Notes | Muistiinpano, kun `notesCreated == true`: avaaminen, täydellinen muokkaus, tyhjentäminen ja uudelleenkirjoittaminen | Ensimmäisen muistiinpanon luominen projektiin, jossa `notesCreated == false` |
| Progress Photos | Gallerian avaaminen, vanhojen kuvien katselu, tekstin muokkaus, valinta ja poisto | Uuden edistymiskuvan ottaminen tai lisääminen |
| My Yarn | Lankakorttien avaaminen, muokkaus, kuvan lisääminen tai vaihto, linkitys ja poisto | Uuden lankakortin luominen |
| Project Yarn Note | Projektikohtainen lankamuistiinpano ja olemassa olevan lankakortin linkitys | `Save to My Yarn`, koska se luo uuden lankakortin |
| Extra Counters | Olemassa olevien laskurien näyttö, arvonmuutokset, nollaus, nimeäminen, tyypin muokkaus, linkitys ja poisto | Uuden lisä-, toisto-, muotoilu- tai osiolaskurin luominen |
| Compact Repeat Counter | Kerran käytetyn laskurin jatkuva käyttö myös nollassa | Ensimmäinen käyttöönotto, kun `secondaryCounterUsed == false` |
| Row Reminders | Olemassa olevien muistutusten näyttö, hälytys, haptiikka, kuittaus, muokkaus, toistuminen ja poisto | Uuden muistutuksen luominen |
| Pattern | Tallennetun mallin, Ravelry-linkin, SAF-PDF:n, lukulinjan ja annotaatioiden käyttö | Kuvien ottaminen ja muuttaminen uudeksi PDF:ksi |
| Session History | Koko tallennettu historia ja istuntojen poistaminen | Ei Pro-porttia eikä Pro-mainosta |
| Insights | Nykyiset vapaat yhteenvetotiedot | Päivä-, viikko- ja kuukausikaaviot sekä aktiivisuusputki |
| Widget | Projektin sisältö säilyy sovelluksessa | Kotinäytön widget on Pro-esitystapa |

Tarkemmat käyttöliittymäsäännöt:

- Projects-FAB on tavallinen vain, kun projektien kokonaismäärä on nolla. Muuten uuden projektin toiminto näyttää Pro-merkinnän mutta pysyy painettavana.
- Projektin luontiraja tarkistetaan repositoryn kirjoitusrajalla. Kaikki kolme luontipolkua käyttävät samaa tarkistusta.
- Counters-valikko avautuu aina. Tyhjä lista on oikea tyhjätila, ja vain `Add counter` on luontiportti.
- Photos-, Reminders- ja Notes-ruudut avautuvat aina. Projektin kiinteän viiden kortin ruudukkoa ei muuteta eikä kortteihin lisätä badgeja, esikatseluja tai myyntitekstiä.
- Tyhjä Notes-näkymä näyttää normaalin tyhjätilan ja Pro-merkityn `Add notes` -toiminnon vain, kun `notesCreated == false`.
- Muistutuskortti piirretään ja hälytys suoritetaan aina, jos olemassa oleva muistutus laukeaa. Pro-tilan päättyminen ei vaienna käyttäjän asettamaa muistutusta.
- Pattern picker avautuu aina. Vain photo-to-PDF-rivi on Pro-luontitoiminto.
- Widget näyttää oikeuden päätyttyä rauhallisen Pro-vaatimuksen. Painallus avaa sovelluksessa Widget-kontekstin Pro-pyynnön eikä käynnistä kokeilua automaattisesti.
- Ilmaiselta käyttäjältä ei palauteta varsinaisia Insights-kaaviopisteitä käyttöliittymään, mutta ViewModel laskee erillisen `hasMeaningfulChartData`-arvon samasta suodatetusta aineistosta.
- Insightsin Pro-viesti näytetään vain, jos valitussa aikavälissä ja ryhmittelyssä olisi vähintään kaksi ei-tyhjää päivä-, viikko- tai kuukausikoria.
- Tyhjässä tai yhden korin Insights-näkymässä ei näytetä Pro-mainosta.
- Insights-viesti sijoitetaan kaavion luonnolliseen paikkaan kortittomana editoriaalisena sisältönä. Se käyttää typografiaa, välistyksiä ja hiusviivoja, ei täytettyä `Card`-pintaa.
- Työpuussa jo oleva Insightsin Pro-haaran vartijakorjaus on lähtötila, ei uudelleen toteutettava työ.
- Session Historyn 24 tunnin näkyvyysraja ja sen Pro-kortti poistetaan. Koko tallennettu istuntoaineisto ladataan.
- Tools-näytön pysyvä kokeilupäiväbanneri ja päättyneen kokeilun `Unlock all tools` -kehote poistetaan.
- Kokeilu- ja ostotila näkyy Settingsissä sekä luonnollisesti avatulla Pro-sivulla. Kokeilun aikana Pro-luontitoiminnot säilyttävät hienovaraisen Pro-merkinnän, jotta käyttäjä tietää mitä hän kokeilee.

## 6. Tekstit ja ostamisen psykologia

### Pro-merkinnän kolme tilaa

- **Kokeilua ei aloitettu tai kokeilu on päättynyt:** `PRO` kertoo lukitusta toiminnosta ja painallus avaa kehotteen.
- **Kokeilu on aktiivinen:** hienovarainen `PRO` säilyy ja saavutettavuusteksti kertoo ominaisuuden olevan käytettävissä Pro-kokeilussa; painallus suorittaa toiminnon normaalisti.
- **Pro on ostettu:** lukkomerkintää ei näytetä.

Merkintä on osa painikkeen tai rivin sisältöä, ei erillinen fokuskelpoinen kontrolli. Se ei perustu vain väriin eikä hallitse näkymän hierarkiaa.

### Asiayhteiset pohjaikkunat

Aloittamattoman kokeilun esimerkki edistymiskuvalle:

- Title: `Try progress photos with Pro`
- Body: `Start your free 14-day Pro trial. No payment method is required. Anything you create stays available after the trial.`
- Primary: `Start free trial`
- Secondary: `Not now`

Päättyneen kokeilun versio:

- Title: `Add progress photos with Pro`
- Body: `Creating a new progress photo requires Pro. Your existing photos stay available.`
- Primary: `See Pro`
- Secondary: `Not now`

Projektiraja:

- Title: `Create another project with Pro`
- Body käyttää plurals-resurssia: `Creating a new project requires Pro because you already have %1$d projects. All your existing projects remain available.`

Teksti ei väitä, että ilmaisversiossa voisi olla vain yksi aktiivinen projekti. Raja koskee uuden projektin luomista, ei olemassa olevien projektien käyttöä.

Muille ominaisuuksille tehdään samaan keskitettyyn malliin täsmällinen otsikko ja yksi ominaisuuskohtainen lause. Teksti ei sano `unlock everything`, ellei väite ole kirjaimellisesti totta.

### Kokeilun päättyminen

Seuraavalla tavallisella sovelluksen käynnistyksellä näytetään kerran:

- Title: `Your Pro trial has ended`
- Body: `Everything you created is still available. You can keep using and editing it. Pro is required only for new Pro items and advanced views.`
- Primary: `See Pro`
- Secondary: `Continue free`

`See Pro` on visuaalisesti ensisijainen, koska näkymän tarkoitus on antaa käyttäjälle mahdollisuus arvioida ostoa. `Continue free` pysyy selvästi näkyvänä ja esteettömänä; poistumista ei viivytetä eikä piiloteta.

Ilmoitusta ei näytetä:

- widgetin deep linkin päällä
- Ravelry OAuth -palautuksessa
- jaetun Ravelry-linkin tuonnin päällä
- eksplisiittisen Pro-deep linkin päällä
- ennen kuin kokeilu- ja ostotila on varmasti ladattu
- ostetulle käyttäjälle

Käyttäjän itse aloittama Pro-toiminto voittaa passiivisen ilmoituksen. Päättyneen kokeilun asiayhteisen kehotteen näyttäminen merkitsee ilmoituksen käsitellyksi, jotta kahta kehotetta ei näytetä peräkkäin.

### Insights

Kortiton inline-viesti:

- Title: `See your activity over time`
- Body: `Pro groups your recorded time into daily, weekly, or monthly charts and adds your activity streak.`
- Action: `See Pro`

Teksti näytetään vain, kun data todella muodostaisi vähintään kahden ämpärin vertailun. Se ei väitä näyttävänsä jokaista istuntoa omana kaavionaan.

### Pro-sivu

Nykyinen 240 dp:n tyhjä gradienttihero poistetaan. Sivulla ei käytetä kuvaa.

Rakenne:

1. Normaali teemataustainen `Scaffold` ja `TopAppBar`.
2. Otsikko `KnitTools Pro`.
3. Lyhyt konkreettinen johdanto.
4. Luottamuslause: `Everything you create remains available, even if your trial ends.`
5. Kolme editoriaalista hyötyryhmää:
   - Projects and materials: create additional projects, first project notes, progress photos and yarn cards.
   - Counting and workflow: compact repeat counter; extra, repeating, shaping and section counters; row reminders; turn pattern photos into PDF; home-screen widget.
   - Insights: daily, weekly and monthly activity charts; activity streak.
6. Hinta ja ostotoiminnot.
7. `Restore purchases`.

Tiloittainen toimintohierarkia:

- Kokeilua ei aloitettu:
  - ensisijainen `Start 14-day free trial`
  - erillinen toissijainen `Buy Pro for {formattedPrice}`, kun hinta on ladattu
- Kokeilu aktiivinen:
  - näkyvä kokeilutila ja jäljellä olevat päivät
  - `Buy Pro for {formattedPrice}`
- Kokeilu päättynyt:
  - `Buy Pro for {formattedPrice}`
- Ostettu:
  - tila `Pro purchased`
  - ei uutta ostopainiketta
- Hintaa ei saada:
  - ei kovakoodattua tai vanhaa hintaa
  - `Price unavailable` ja painettava `Retry`
  - kokeilun aloittaminen ja oston palautus säilyvät käytettävissä

Hinnan alla näytetään `One-time purchase. No subscription.` Tuote säilyy Google Playn `INAPP`-kertamaksutuotteena `knittools_pro`. Lokalisoitu hinta otetaan ajantasaisesta `ProductDetails`-tiedosta. [Google Play Billing: one-time products](https://developer.android.com/google/play/billing/one-time-products)

Billing 9.1.0 -integraatiossa tarkistetaan nykyinen kertatuotteen tarjousmalli. Tarkoitettu pysyvä kertamaksuvaihtoehto valitaan deterministisesti ja sen offer token välitetään, jos Play Consolesta palautuva tuote sitä edellyttää. Vanhaa polkua ei poisteta arvaamalla, vaan ratkaisu varmennetaan testituotteella. [Google Play Billing integration](https://developer.android.com/google/play/billing/integrate.html)

### Settings, saavutettavuus ja lokalisaatio

Settingsin Pro-rivi näyttää yhden seuraavista:

- `Not started`
- `Trial · %d days left`
- `Trial ended`
- `Purchased`

Päiväluku toteutetaan plurals-resurssilla.

Pro-merkintä yhdistyy ruudunlukijalle toiminnon nimeen ja tilaan, esimerkiksi `Take photo, Pro feature, available during trial`. Kaikki kosketuskohteet säilyvät vähintään 48 dp:n kokoisina ja tilat välitetään Compose-semanticsin kautta. [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)

Kaikki uudet ja muuttuneet tekstit lisätään englannin lisäksi nykyisiin `da`, `de`, `es`, `fi`, `fr`, `it`, `nb`, `nl`, `pt` ja `sv`-resursseihin. Testi tarkistaa avain-, plurals- ja placeholder-pariteetin. Käännösten luonnollisuus tarkistetaan erikseen, koska XML-pariteetti ei todista kielen laatua.

## 7. Pienet, itsenäisesti julkaistavat työt

Jokainen työ pidetään vihreänä ja rajataan niin, että sen voi arvioida ennen seuraavaan siirtymistä.

### Työ 1 — Istuntohistorian ja undo-retention erottaminen

- Poista `FULL_HISTORY` `ProFeature`-listalta.
- Poista Session Historyn 24 tunnin näkyvyysraja ja Pro-kortti.
- Muuta `counter_history`-siivous kaikille samaksi 24 tunnin tekniseksi retentioksi.
- Poista undo-siivouksen riippuvuus `ProState`sta ja Billingin readinessistä.

Hyväksyntä:

- kaikki tallennetut istunnot näkyvät kaikilla oikeustasoilla
- undo-retentio ei muutu trialin tai oston mukaan
- kummankaan taulun käsittely ei vaikuta toiseen

### Työ 2 — Room 18 ja olemassa olevan sisällön käyttö

- Lisää `secondaryCounterUsed` ja `notesCreated` sekä migraatio 17→18.
- Avaa olemassa olevien muistiinpanojen, kuvien, laskurien, muistutusten ja lankakorttien reitit sekä muokkaus.
- Korjaa Countersin nykyinen kuollut painallus.
- Varmista muistutuksen renderöinti, haptiikka, muokkaus, kuittaus ja poisto ilman Prota.

Hyväksyntä:

- migraatio säilyttää kaiken nykyisen datan
- nollaan palautettu käytetty toistolaskuri pysyy käytettävissä
- tyhjennetty aikaisempi muistiinpano pysyy muokattavana
- uuden projektin käyttöliput ovat `false`

### Työ 3 — Yksi atominen projektin luontiraja

- Toteuta repository-tason transaktio, joka tarkistaa projektien kokonaismäärän ja lisää projektin.
- Ohjaa Projects-, Counter- ja Ravelry-luonti samaan metodiin.
- Säilytä arkistointi ja uudelleenaktivointi vapaina.
- Näytä oikea Pro-tila ennen luontidialogia ja tarkista raja uudelleen tallennuksessa.

Hyväksyntä:

- ensimmäinen projekti onnistuu ilman Prota
- arkistointi ei mahdollista toisen ilmaisen projektin luontia
- ainoan projektin poistaminen mahdollistaa uuden ensimmäisen projektin
- kokeilussa luodut useat projektit pysyvät käytettävissä ja uudelleenaktivoitavissa
- rinnakkaiset luontiyritykset eivät ohita rajaa

### Työ 4 — Käyttäjän käynnistämä kokeilu

- Lisää `TRIAL_NOT_STARTED` ja johda se `trial_start_timestamp == 0L`-arvosta.
- Lopeta kokeilun automaattinen käynnistys.
- Toteuta atominen `startTrial()` ja virhetila.
- Lisää kokeilun aloitus nykyiselle Pro-sivulle ennen sen visuaalista uudistusta.

Hyväksyntä:

- mikään käynnistys- tai deep link -polku ei aloita kokeilua
- käyttäjän vahvistus kirjoittaa aloitusajan kerran
- tallennusvirhe ei avaa ominaisuuksia
- aktiivisen kokeilun päiväluku ei ole nolla

### Työ 5 — Jaettu kehote ja luontiportit

- Toteuta jaettu esitysmalli ja `ProPromptSheet`.
- Pidä pending action tyypitettynä sitä käyttävässä graafissa tai näytössä.
- Lisää kontekstikehotteet projekteille, muistiinpanoille, kuville, lankakorteille, laskureille, muistutuksille ja pattern-kuvaukselle.
- Rajaa pidempikestoinen luontioikeus kamera- ja ensimmäisen muistiinpanon työnkulkuihin.

Hyväksyntä:

- lukittu toiminto ei tee hiljaista no-op-toimintoa eikä hyppää suoraan yleiselle ostosivulle
- kokeilun aloitus tai osto jatkaa alkuperäisen toiminnon kerran
- peruminen tai navigointi poistaa odottavan toiminnon
- syötetyt lomakearvot eivät katoa oikeuden vaihtuessa

### Työ 6 — Kokeilun tunnistettavuus ja päättymishetki

- Lisää Pro-merkinnän kolme tilaa.
- Lisää kerran näytettävä päättymisilmoitus ja intenttien prioriteetit.
- Lisää Settings-status.
- Poista Toolsin pysyvät Pro-bannerit.
- Tee widgetin lukitustilasta kontekstikehotteeseen johtava.

Hyväksyntä:

- käyttäjä tunnistaa kokeilun aikana, mitkä toiminnot kuuluvat Prohon
- päättymisilmoitus näytetään kerran ja vain tavallisessa sopivassa käynnistyksessä
- `See Pro` on ensisijainen ja `Continue free` selvästi saavutettava
- käyttäjä ei saa kahta peräkkäistä Pro-kehotetta

### Työ 7 — Insightsin lopullinen Pro-pinta

- Säilytä työpuun nykyinen vartijakorjaus.
- Lisää `hasMeaningfulChartData` ilman varsinaisten kaaviopisteiden luovuttamista ilmaiselle käyttöliittymälle.
- Näytä kortiton kehote vain vähintään kahdella ei-tyhjällä ämpärillä.
- Varmista, että Pro-käyttäjän kaavio säilyy nykyisen Insights-sopimuksen mukaisena.

### Työ 8 — Pro-sivu ja Billing

- Korvaa gradienttihero editoriaalisella rakenteella ja faktapohjaisilla hyötyryhmillä.
- Käytä valitun Play-tarjouksen lokalisoitua hintaa.
- Varmista osto, palautus, jo omistettu tuote, pending-tila, kuittaus, verkkovirhe ja kylmäkäynnistyksen oikeuksien palautuminen.

### Työ 9 — Dokumentointi ja lokalisointi

- Kirjaa Pro-periaate ja kortiton Insights-esitys `CLAUDE.md`:hen.
- Päivitä schema 18 sekä `AGENTS.md`:hen että `CODEX.md`:hen.
- Päivitä `PROJECT.md`:n Pro-ominaisuudet toteutusta vastaaviksi.
- Lisää kaikki uudet tekstit kaikkiin 11 kieliresurssiin.
- Älä lisää generoituja `reports/`-tiedostoja versionhallintaan.

## 8. Automaattinen varmennus

Kohdennetut käyttäytymis- ja migraatiotestit kattavat ainakin:

- uusi asennus on `TRIAL_NOT_STARTED`
- tavallinen, widget-, Ravelry-, share- ja OAuth-käynnistys eivät aloita kokeilua
- vahvistettu aloitus tallentaa ajan kerran ja jatkaa alkuperäisen toiminnon
- tallennusvirhe ei avaa ominaisuutta
- päivälaskenta näyttää aktiiviselle kokeilulle vähintään yhden päivän
- päättymisilmoitus näytetään kerran ja vain sallitussa käynnistyksessä
- osto ohittaa kokeilutilan; debug-oikeus ei väärennä ostotilaa tai ostotekstejä
- Room 17→18 säilyttää projektit ja merkitsee vanhat `secondaryCounterUsed`- ja `notesCreated`-arvot tosiksi
- uuden projektin molemmat käyttöliput ovat epätosia
- ensimmäinen toistolaskurin arvonmuutos merkitsee sen käytetyksi
- nollaan palautettu käytetty toistolaskuri säilyy käytettävissä
- ensimmäinen oikeutettu muistiinpano merkitsee `notesCreated`-arvon
- muistiinpanon tyhjentäminen ei poista sen käyttöoikeutta
- olemassa olevat muistiinpanot, kuvat, laskurit, muistutukset ja lankakortit toimivat kokeilun päätyttyä
- olemassa olevan muistutuksen hälytys, haptiikka, muokkaus, kuittaus ja poisto toimivat ilman Prota
- uusi objekti avaa oikean asiayhteisen kehotteen
- kaikki tallennetut istunnot näkyvät ilman Prota
- undo-retentio on sama free-, trial- ja purchased-tilassa
- session-rivien määrä ei muutu undo-siivouksessa
- ensimmäinen projekti onnistuu ilman Prota
- arkistoitu projekti estää uuden ilmaisen projektin mutta voidaan itse aktivoida uudelleen
- ainoan projektin poistaminen vapauttaa ensimmäisen projektin luonnin
- trialissa luodut useat projektit säilyvät käytettävinä
- Projects-, Counter- ja Ravelry-luonti käyttävät samaa atomista rajaa
- rinnakkaiset luontiyritykset eivät ohita projektirajaa
- kamera- ja ensimmäisen muistiinpanon työnkulku voivat valmistua oikeuden päättyessä kesken
- yksinkertainen dialogi säilyttää arvonsa ja jatkaa kerran oikeuden saamisen jälkeen
- aktiivisen trialin Pro-toiminto näyttää tunnistettavan mutta ei lukitun tilan
- Insights-kehote ei näy 0–1 ei-tyhjällä korilla ja näkyy vähintään kahdella
- Pro-käyttäjän oikea kaavio säilyy nykyisen valinnan mukaisena
- ostosivun hyödyt eivät sisällä Full historya tai toteuttamattomia ominaisuuksia
- lokalisoitu hinta tulee valitusta Play-tarjouksesta
- merkkijonojen avaimet, plurals ja format-placeholderit täsmäävät kaikissa 11 kielessä

Käyttäytymistestit ovat ensisijaisia. Lähdetestejä käytetään vain kapeisiin arkkitehtuurisopimuksiin, kuten siihen, ettei suora `BuildConfig.DEBUG`-ohitus leviä käyttöliittymään tai että kaikki projektin luontipolut kutsuvat samaa repository-metodia.

Vähimmäisvarmennus toteutuksen jälkeen:

- kohdennetut unit- ja migraatiotestit
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
- `./gradlew :app:lintDebug`
- diff- ja resurssitarkistus ilman käyttäjän wrapper-skriptejä
- ei generoituja `reports/`-tiedostoja versionhallintaan

## 9. Laite- ja visuaalinen hyväksyntä

Testataan vähintään seuraavat tilat oikealla laitteella tai emulaattorilla:

- vaalea ja tumma teema
- fonttikoot 1.0× ja suurin käytännössä tuettu koko
- TalkBack
- uusi asennus
- aktiivinen kokeilu
- juuri päättynyt kokeilu
- ostettu Pro
- offline-tila ja Play-tietojen latausvirhe
- rotaatio kesken muistiinpano- ja kameratyönkulun
- olemassa oleva sisältö jokaisessa portatussa ominaisuudessa
- tyhjä, yhden korin ja monen korin Insights
- widget ennen kokeilua, kokeilun aikana ja sen jälkeen

Otetaan vertailukuvat Projects-, Counter-, Photos-, Notes-, Counters-, Insights-, Settings- ja Pro-näkymistä. Hyväksyntä edellyttää, että:

- Pro-kehote esiintyy toiminnon yhteydessä eikä satunnaisena keskeytyksenä
- tyhjissä tiloissa ei ole irrallisia myyntikortteja
- olemassa oleva sisältö ei koskaan näytä lukitulta
- trial-käyttäjä tunnistaa Pro-toiminnot ilman, että merkintä hallitsee näkymää
- Pro-sivu näyttää samalta sovellukselta kuin muut näkymät
- tekstit eivät katkea tai peitä toimintoja
- ostohinta, kokeilun kesto, ominaisuudet ja säilymislupaus vastaavat todellista toteutusta

## 10. Rajat ja julkaisuportti

Tämä suunnitelma ei päätä:

- varsinaista hintaa tai maakohtaista hinnoittelua
- analytiikkaa tai A/B-testausta
- palvelinpuolista trial-synkronointia tai väärinkäytön estoa
- uutta tilaustuotetta; Pro säilyy kertamaksullisena

Sovellusta ei ole julkaistu. Play Billingin todellinen tuotetieto, offer token, oston palautus ja pending-/already-owned-polut jäävät erilliseksi suljetun testiraidan julkaisua edeltäväksi hyväksyntäportiksi. Toteutusta ei kutsuta valmiiksi pelkän buildin, lähdetestin tai mockatun Billing-testin perusteella.
