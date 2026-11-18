# KnitTools: onboarding ja Pro-esitys, suunnitelma

Tila: **ehdotus päätettäväksi**. Koodia ei ole muutettu tämän dokumentin perusteella.
Päivitetty: 2026-10-29.
Pohja: `PROJECT.md`, nykyinen koodi (haara `codex/audit-korjaukset-20261018`) ja muistiinpano `mobile-onboarding.md` (12 käytäntöä).

---

## 0. Tiivistelmä

KnitToolsissa ei ole nyt minkäänlaista ensikäynnistystä. Uusi käyttäjä päätyy Projects-näkymään, jossa on osio-otsikko ACTIVE (0), teksti "No active projects" ja New Project -pilleri. Kesken olevaa työtä ei voi edes aloittaa oikeasta kerroksesta: uusi projekti alkaa aina nollasta, eikä laskurin arvoa voi asettaa. Kerroksessa 137 oleva neuloja joutuisi napauttamaan plusnappia 137 kertaa.

Ehdotan onboardingia nimeltä **Ensimmäinen kerros**. Se ei luettele ominaisuuksia. Käyttäjä tekee heti sen, mitä varten sovellus on olemassa:

1. Jo tervetulonäkymässä hän napauttaa oikeaa 3D-plusnappia. Luku kasvaa, neuletilkkuun neuloutuu rivi ja pienen ohjesivun lukukaista siirtyy riville alemmas. Yksi kuva kertoo, mistä sovelluksessa on kyse: **KnitTools muistaa kohdan, jotta sinun ei tarvitse.**
2. Neljällä lyhyellä kysymyksellä syntyy hänen oma projektinsa oikeaan kerrokseen, ja hän näkee oman jatka-korttinsa piirtyvän sitä mukaa kuin kirjoittaa.
3. Hän laskeutuu laskuriin valmiina neulomaan seuraavan kerroksen. Ensimmäinen napautus oikean kerroksen jälkeen on aha-hetki.

Etenemistä ei näytetä pisteinä vaan **silmukkarivinä**, joka neuloutuu vaihe kerrallaan. Käytön aikana opetus jatkuu **silmukkamerkkivihjeinä**: yksi lyhyt vihje kerrallaan, vasta kun käyttäjän toiminta osoittaa tarpeen. Ensimmäisen projektin näkymässä on **Projektin alku** -lista, jonka ensimmäiset silmukat on jo neulottu onboardingissa.

Pro-merkit uudistetaan periaatteella **merkki kertoo totuuden**: PRO näkyy vain toiminnossa, joka juuri nyt avaisi Pro-kehotteen. Nykyiset merkit ovat osin väärissä paikoissa, näkyvät myös kokeilun aikana ja puuttuvat kokonaan osasta portteja. Yksi Pro-toiminto (ohjetekstin liittäminen laskimiin) on kokonaan piilossa ilmaiskäyttäjiltä. Lisäksi ehdotan, että muistiinpanot, edistymiskuvat, muutama lankakortti ja yksi lisälaskuri olisivat ilmaisia, koska ne kuuluvat ensimmäisen projektin peruskäyttöön. Nämä ovat tuotepäätöksiä, jotka on koottu lukuun 17.

---

## 1. Lähtötilanne koodissa

**Ensikäynnistys**
- Onboardingia, first run -lippua tai opastusta ei ole (`onboard`, `welcome`, `first_run`, `tutorial` eivät löydy main-lähteistä).
- `MainActivity` pitää splashin näkyvissä, kunnes teema on luettu (`setKeepOnScreenCondition { !startupThemeLoaded }`), ja käynnistää aina `TopLevelDestination.Projects.route`.
- Tyhjä Projects (`ProjectListScreen.kt`, `ProjectListContent`): osio-otsikko Active, lukumäärä 0 ja teksti `no_active_projects`. Continue-hero näkyy vain, kun projektin luku on yli 0.
- Settings → Info → Help and Guide avaa ulkoisen verkkosivun. Sovelluksen sisäistä opastusta ei ole.

**Projektin aloitus**
- `CounterRepository.createProject` luo projektin aina luvulla 0. Laskurin muutoksia on neljä: `Increment`, `Decrement`, `Reset`, `Undo` (`MainCounterChange`). Arvon asettamista ei ole.
- Tavoite asetetaan vasta laskurissa (`CounterViewModel.setTargetRows`).
- Uuden projektin jälkeen laskuri avaa ohjeen valinnan (`PatternPickerSheet`, tila `INITIAL_PROJECT_PATTERN`): PDF, pilvi-PDF, Ravelry, verkko-ohje sekä Pro-merkityt kuvat ja kamera. Tätä polkua voi käyttää onboardingissa uudelleen.

**Ilmoitukset ja analytiikka**
- Manifestissa on vain `INTERNET` ja `VIBRATE`. Ilmoituksia ei ole, ja rivimuistutukset ovat sovelluksen sisäisiä.
- Käyttötilastot ovat opt-in ja oletuksena pois. Tapahtumia on 12 (`UsageEvent`). Näkymäsallilistalta puuttuu `project_overview`.

**Pro**
- 13 `ProFeature`-porttia, käyttäjän itse aloittama 14 päivän kokeilu ja kertaosto `knittools_pro`.
- `ProBadge` (`ui/components/ProBadge.kt`): teksti PRO, pohja `colorScheme.secondaryContainer`. Tila `Trial` piirtyy täsmälleen kuten `Locked`, vain ruudunlukijan kuvaus eroaa. Merkki on käytössä yhdeksässä kohdassa (luku 11.2).
- `ProPromptSheet`: otsikko, leipäteksti, Not now ja Start free trial. Odottava toiminto jatkuu kokeilun alettua.
- `ProUpgradeScreen`: johdanto, kolme tekstiryhmää ja osto-osio. Sivu ei kerro, mitä ilmaisversioon kuuluu.
- `PasteInstructionButton` (Increase/Decrease ja Gauge) palauttaa ilmaiskäyttäjälle tyhjää (`if (!isPro) return`). Toiminto ei ole `ProFeature`-listalla, Pro-sivulla eikä merkitty missään.

---

## 2. Tavoite ja mittarit

**Ensimmäinen onnistuminen:** käyttäjän oma projekti on sovelluksessa oikeassa kerroksessa, ja hän on napauttanut plusnappia oikean kerroksen valmistuttua. Siitä hetkestä sovellus muistaa hänen työnsä.

| Mittari | Määritelmä | Alustava tavoite |
| --- | --- | --- |
| Onboardingin läpäisy | Projekti luotu onboardingissa tai Katselen ensin valittu | vähintään 75 % aloittaneista |
| Aika ensimmäiseen napautukseen | Käynnistyksestä ensimmäiseen laskurin muutokseen omassa projektissa | alle 90 s (kesken olevan työn polku ilman ohjetta) |
| Aktivoituminen | Projekti ja vähintään yksi laskurin muutos ensimmäisen vuorokauden aikana | vähintään 60 % |
| Paluu | Laskurin muutos päivinä 2–7 | seurataan, ei tavoitetta ennen dataa |
| Kokeilun aloitus | Kokeilu aloitettu ja mistä kehotteesta | seurataan lähteittäin |

Analytiikka on opt-in, joten otos on pieni. Ennen julkaisua kulku testataan 3–5 neulojalla ja virkkaajalla (puhelin käteen, ei ohjeita, katsotaan missä he pysähtyvät).

---

## 3. Kaksitoista käytäntöä KnitToolsissa

| # | Käytäntö | Toteutus KnitToolsissa |
| --- | --- | --- |
| 1 | Suunnittele ensimmäinen onnistuminen | Oma projekti oikeaan kerrokseen ja ensimmäinen napautus. Jokainen vaihe palvelee tätä. |
| 2 | Näytä sovellus toiminnassa | Tervetulonäkymän plusnappi on oikea. Luku, tilkku ja lukukaista reagoivat. Ei kuvakaruselliä. |
| 3 | Kokeilu ennen rekisteröitymistä | Tiliä ei ole lainkaan. Se sanotaan ääneen luottamuslauseena. |
| 4 | Vain hyödylliset kysymykset | Neljä kysymystä, joista jokainen muuttaa jotain: käsityö (oletus, sanasto, pinta), avun kohteet (vaiheet, vihjeet, lista), tilanne (aloituskerros), ohjeen sijainti (oikea tuontipolku). Yksiköitä ei kysytä vaan ne päätellään alueesta. Kieltä ja teemaa ei kysytä. |
| 5 | Salli useampi tavoite | Avun kohteet ovat monivalinta. |
| 6 | Näytä personoinnin tulos heti | Käsityön valinta vaihtaa tilkun pinnan heti. Projektivaiheessa oma jatka-kortti piirtyy reaaliajassa. Lopuksi oma laskuri ja oma Continue-hero. |
| 7 | Inhimillinen kieli | Sinä-muoto, käsityöläisen sanasto, erilliset neule- ja virkkaustekstit, ei markkinointikieltä. |
| 8 | Perustele maksullinen | Pro tarjotaan vain tarpeen hetkellä (toinen keskeneräinen työ, paperiohje). Teksti puhuu käyttäjän omasta tilanteesta. Ensimmäiseen onnistumiseen ei osu maksumuuria. |
| 9 | Opeta käytön yhteydessä | Silmukkamerkkivihjeet käyttäjän toiminnan perusteella, ei kellonajan. |
| 10 | Helpota etenemistä | Silmukkarivi etenemisen mittarina, yksi päätoiminto per näkymä, välitön palaute. |
| 11 | Perustele ilmoituslupa | Lupaa ei kysytä, koska ilmoituksia ei ole. Mahdollinen tuleva ilmoitus kysytään vasta tarpeen hetkellä esikatselun kanssa (luku 12). |
| 12 | Jätä tarpeeton pois | Ohita on aina saatavilla. Katselen ensin vie suoraan sovellukseen. Ohjevaihe jää pois, jos ohjetta ei valittu. |

---

## 4. Konsepti: Ensimmäinen kerros

Kolme visuaalista ankkuria. Kaikki ovat jo sovelluksen omaa kieltä, eivät yleisiä kuvakkeita.

### 4.1 Silmukkarivi etenemisen mittarina

Uusi jaettu komponentti `StitchProgressRow(total, done, craftType)`.

- Vaakarivi ⋁-silmukoita samalla piirrolla kuin `FabricSwatch`in neulepinta. Virkkauksessa vinot säieparit.
- Tehty silmukka on täytetty (onboardingissa `primary`, myöhemmin projektin lankaväri). Tuleva silmukka on ohut katkoviivainen ääriviiva `onSurface`-alfalla.
- Uusi silmukka "neuloutuu" lyhyellä piirtoanimaatiolla (noin 250 ms). Ei animaatiota, jos järjestelmän animaatiot ovat pois.
- Ensimmäinen silmukka on jo neulottu, kun käyttäjä lähtee tervetulonäkymästä: hän teki sen itse napauttamalla plusnappia. Valmiiksi aloitettu tehtävä viedään loppuun todennäköisemmin (endowed progress).
- Sama komponentti toistuu Projektin alku -listassa (luku 8) ja Pro-kokeilun listassa (luku 11.9). Käyttäjä oppii yhden merkityksen: rivi neuloutuu, kun teet asioita.
- Saavutettavuus: yksi elementti, jonka kuvaus on "Vaihe 3/6" tai "3 kohtaa 5:stä tehty", `ProgressBarRangeInfo`.

### 4.2 Elävä neuletilkku

Tervetulonäkymän iso `FabricSwatch` kasvaa rivin jokaisesta plusnapautuksesta. Käsityön valinta vaihtaa pinnan neuleesta virkkaukseen samalla hetkellä. Tilkku on "sinun ensimmäinen kerroksesi" koko onboardingin ajan.

### 4.3 Oma jatka-kortti esikatseluna

Projektivaiheessa jaettu `ContinueProjectCard` piirtyy esikatseluna käyttäjän tiedoilla: nimi, iso luku, tavoite, edistymispalkki ja "83 kerrosta jäljellä". Kun projekti luodaan, sama kortti on Projects-näkymän Continue-herona. Käyttäjä näkee ennen tallennusta, miltä hänen työnsä sovelluksessa näyttää. Itse rakennettu tuntuu omalta.

---

## 5. Ensikäynnistyksen kulku

```
Tervetuloa ── Ohita ──────────────────────────────────────────→ Projects (opastava tyhjä tila)
    │
    ▼
1 Käsityö → 2 Avun kohteet → 3 Tilanne ── Katselen ensin ──────→ Projects (opastava tyhjä tila)
                                  │
                                  ▼
                             4 Projekti → 5 Ohje (vain jos valittu) → Laskuri: ensimmäinen kerros
```

Silmukkarivissä on kuusi silmukkaa: tervetuloa (neulotaan napautuksella), käsityö, avun kohteet, tilanne, projekti, ohje. Jos ohjevaihe jää pois, rivissä on viisi silmukkaa.

Kaikissa vaiheissa on järjestelmän Takaisin ja vasemman yläkulman takaisin-nuoli. Alanavigaatio on piilossa kuten Ravelry-selaimessa. Taustana `colorScheme.background`. Yksi täytetty päätoiminto per näkymä. Vaiheiden välillä vaakasiirtymä ja häivytys noin 250 ms.

Kaikki EN-otsikot noudattavat Title Case -sääntöä. Suomenkielinen termi on aina "ohje", ei "kaava".

### 5.0 Tervetuloa (interaktiivinen)

**Tarkoitus:** kertoa yhdellä katseella, mistä sovelluksessa on kyse, ja antaa kokeilla ennen kuin mitään kysytään.

**Asettelu** (360 × 800 dp ylhäältä alas):
- Kohtaus noin 45 % korkeudesta:
  - Vasemmalla pieni ohjesivu, joka on piirretty koodilla: paperinsävyinen pinta, harmaat palkit tekstiriveinä ja yksi pieni neulekaaviolohko. Oikeaa tekstiä ei ole, joten lokalisointia ei tarvita. Sivun päällä lukukaista samalla ilmeellä kuin PDF-lukijan lukukaista.
  - Oikealla iso luku laskurin tyylillä (pienempänä) ja sen alla `FabricSwatch`, joka kasvaa.
- Keskellä oikea `counter_plus_button`, sama 3D-kuva ja painalluspalaute kuin laskurissa. Sen alla pieni teksti "Kokeile: napauta".
- Alaosa: otsikko, leipäteksti, luottamuslause, päätoiminto ja Ohita.

**Vuorovaikutus:**
- Napautus: luku 12 → 13, tilkkuun neuloutuu rivi, lukukaista liukuu ohjesivulla seuraavalle riville ja haptinen palaute toimii asetuksen mukaan.
- Kahden napautuksen jälkeen pikkuteksti vaihtuu: "Juuri näin. KnitTools muistaa kohdan, jotta sinun ei tarvitse." Aloitetaan-painike saa kerran hienovaraisen huomion (pieni skaalaus, ei jatkuvaa sykettä).
- Napauttaminen ei ole pakollista. Jos käyttäjä painaa suoraan Aloitetaan, ensimmäinen silmukka neuloutuu silti.

**Tekstit:**

| Rooli | FI | EN |
| --- | --- | --- |
| Otsikko | Paikkasi pysyy tallessa | Never Lose Your Place |
| Leipäteksti | KnitTools muistaa kerroksen, ohjeen kohdan ja kaiken työhön liittyvän. Voit laskea työn käsistäsi milloin tahansa ja jatkaa siitä, mihin jäit. | KnitTools remembers your row, your spot in the pattern and everything else about the project. Put your work down any time and pick up exactly where you left off. |
| Luottamuslause | Ei tiliä eikä kirjautumista. Projektisi tallentuvat tähän puhelimeen. | No account, no sign-in. Your projects stay on this phone. |
| Päätoiminto | Aloitetaan | Let's Start |
| Toissijainen | Ohita | Skip |

**Tallentuu:** ei mitään. Luku on pelkkä esimerkki.

**Saavutettavuus:** kohtaus on yksi ryhmä, jonka kuvaus päivittyy kohteliaana live regionina: "Esimerkkilaskuri, luku 13. Ohjeen lukukaista siirtyi seuraavalle riville." Plusnapilla on Button-rooli ja kuvaus "Lisää esimerkkilaskuriin yksi".

### 5.1 Käsityö

**Kysymys:** "Mitä teet useimmiten?" / "What Do You Make Most Often?"

**Asettelu:** yläosassa iso esikatselutilkku. Sen alla täysleveä `SegmentedToggle`: Neulon, Virkkaan, Molempia. Valinta vaihtaa tilkun pinnan heti, ja Molempia näyttää tilkun, jonka vasen puoli on neulepintaa ja oikea virkkausta. Tämä noudattaa sääntöä "valinnat ovat aina `SegmentedToggle`".

**Selite:** "Tästä tulee uusien projektien oletus. Voit vaihtaa sen projektikohtaisesti."

**Vaikutus:**
- Tallentuu `default_craft_type` (KNITTING, CROCHET tai tyhjä, jolloin projektilomake kysyy aina).
- `ProjectDetailsSheet` käyttää oletusta.
- Silmukkarivi ja tyhjien tilojen tekstit vaihtuvat käsityön mukaan.
- Laskurin yksikön oletus seuraa nykyistä sääntöä (neule kerrokset, virkkaus kierrokset).

### 5.2 Avun kohteet (monivalinta)

**Kysymys:** "Laskuri on aina mukana. Missä muussa KnitTools voi auttaa?" / "The Counter Is Always There. What Else Can We Help With?"
**Selite:** "Valitse kaikki sopivat. Kaikki on käytettävissä myöhemminkin."

Laskentaa ei listata, koska se on itsestäänselvyys. Valinnat ovat `SheetOptionRow`-kortteja, joissa vasemmalla `CrossStitchIcon` ja oikealla valintamerkki.

| Valinta | Kuvake | Mitä valinta muuttaa |
| --- | --- | --- |
| Seurata ohjetta | `PATTERN` | Vaihe 5 Ohje näytetään. Projektin alku -listassa Liitä ohje on ensimmäisenä. Lukukaistavihje H6 on käytössä. |
| Pitää kirjaa langoista | `YARN` | Projektin alku -listaan tulee Lisää lanka. My Yarn -vihje H10. |
| Nähdä, paljonko aikaa työhön menee | uusi `CLOCK` | Työskentelyvihje H4 näytetään heti ensimmäisellä laskurikäynnillä eikä vasta kolmantena päivänä. Listaan tulee Tallenna ensimmäinen työskentely. |
| Laskea silmukoita, tiheyttä ja lankamenekkiä | uusi `RULER` | Tools-välilehdelle tulee kerran näkyvä huomiopiste ja vihje H11. |

Tallentuu: `onboarding_goals` (merkkijonojoukko). Jatka on aina käytettävissä. Nolla valintaa tarkoittaa pelkkää laskentaa, ja ohjevaihe jää pois.

Komponenttimuutos: `SheetOptionRow` saa valinnaiset parametrit `leading` (kuvake tai tilkku) ja `selected: Boolean?` (kun annettu, rooli on Checkbox ja valinta näkyy `cardContainerColor(selected)`-pohjana ja oikean reunan checkinä, ei vasemman reunan palkkina). Laajennetaan jaettua komponenttia, ei tehdä paikallista kopiota.

### 5.3 Tilanne

**Kysymys:** neuleelle "Mitä sinulla on nyt puikoilla?", virkkaukselle ja molemmille "Mitä sinulla on nyt työn alla?" / "What's On Your Needles?" ja "What Are You Working On?"

| Valinta | Alateksti | Tilkku vasemmalla |
| --- | --- | --- |
| Minulla on työ kesken | Aloitat siitä kerroksesta, jossa olet nyt | Puoliksi neulottu tilkku |
| Aloitan uuden työn | Laskuri alkaa nollasta | Pelkkä luomisrivi |
| Katselen ensin | Voit lisätä projektin myöhemmin | Neutraali kerrattu lanka |

Napautus valitsee ja jatkaa heti, erillistä Jatka-painiketta ei ole. Valinta tallentuu vain onboardingin luonnokseen.

### 5.4 Projekti

**Otsikko:** kesken olevalle työlle "Kerro työstäsi", uudelle "Uusi työ".

**Asettelu:**
1. Yläosassa `ContinueProjectCard`-esikatselu. Ei napautettava. Ruudunlukijalle "Esikatselu: Raitasukat, 37 kerrosta".
2. Kentät korttipinnassa kuten `ToolInputCard` (`cardTextFieldColors()`):
   - **Työn nimi** (`LabeledTextField`). Esimerkkiarvo käsityön mukaan: "Esim. Raitasukat" tai "Esim. Isoäidinneliöpeitto".
   - **Käsityö** `SegmentedToggle`, vain jos vaiheessa 1 valittiin Molempia.
   - **Lasken** `SegmentedToggle`: Kerroksia, Kierroksia. Oletus käsityön mukaan. Toistot ja oma nimike jäävät projektin asetuksiin.
   - **Kesken olevalle työlle:** "Montako kerrosta olet jo neulonut?" (yksikön ja käsityön mukaan). Iso luku, pienemmät 3D-miinus ja -plus, ja luvun napautus avaa numeronäppäimistön. Apuriville päivittyvä teksti: "Seuraavaksi neulot kerroksen 38." Kysymys on muotoiltu valmiiksi neulottuina kerroksina, koska laskurin luku tarkoittaa valmiita kerroksia. Muotoilu poistaa yhden kerroksen heiton, joka muuten syntyisi helposti.
   - **Tavoite** (valinnainen, `NumberInputField`): "Montako kerrosta ohjeessa on?" Esimerkkiarvo 120. Apuriville "Näet, paljonko on jäljellä." Esikatselukortti näyttää heti palkin ja jäljellä olevan määrän.
3. Päätoiminto: "Luo projekti" (kesken oleva työ) tai "Aloita" (uusi).

**Validointi:** nimi on pakollinen (trimmattu). Luku 0–99 999. Tavoite tyhjä tai nykyisen tavoitevalidoinnin rajoissa. Nimen törmäys hoituu nykyisellä `ProjectNameRules`-säännöllä (Nimi (2)).

**Tallentuu:** yksi transaktio `CounterRepository.createProject(..., initialCount, targetRows)`. Aloitusluku ei luo historiariviä, joten Kumoa ei voi viedä aloituskohtaa alemmas. `onboarding_project_id` tallentuu Projektin alku -listaa varten.

**Pro:** aktiivisia projekteja on tässä vaiheessa aina nolla, joten luonti on ilmainen. Jos aktiivinen projekti on jo olemassa, onboardingia ei näytetä lainkaan (luku 13.2).

### 5.5 Ohje (vain jos Seurata ohjetta valittiin)

**Kysymys:** "Missä ohjeesi on?" / "Where Is Your Pattern?"
**Selite:** "Kun ohje on projektissa, lukukaista seuraa laskuria." Pieni lukukaistakohtaus toistaa tervetulonäkymän liikkeen.

| Valinta | Mitä tapahtuu |
| --- | --- |
| PDF-tiedostona puhelimessa | Nykyinen tiedostovalitsin, sitten laskuri |
| Verkkosivulla | Nykyinen verkko-ohjeen editori (Verkkosivun osoite) |
| Ravelryssä | Sovelluksen Ravelry-selain. Näytetään vain, kun Ravelryn kirjallinen lupa on saatu (`config/security-decisions.md`). |
| Paperilla (alateksti "Kuvaa sivut, niin niistä tulee PDF") | Totuudenmukainen PRO-merkki. Ilman oikeutta avautuu kontekstikehote kokeilutarjouksella, ja kokeilun alettua kamera jatkaa odottavana toimintona. |
| Lisään myöhemmin (tekstipainike) | Suoraan laskuriin |

**Toteutus:** projekti on jo luotu, joten valinta vie laskuriin ja käynnistää vastaavan polun suoraan `PatternPickerSheet`in nykyisillä toiminnoilla. Uutta tuontilogiikkaa ei kirjoiteta.

### 5.6 Laskeutuminen laskuriin

- Silmukkarivin viimeinen silmukka neuloutuu, ja siirtymä vie laskuriin. Valinnainen viimeistely: esikatselukortti muuttuu laskurin otsikkoalueeksi jaetulla elementtisiirtymällä (`SharedTransitionLayout`). Jos tämä osoittautuu hauraaksi, tavallinen häivytys riittää.
- Onboarding poistetaan pinosta. Takaisin laskurista vie Projectsiin, jossa oma projekti on Continue-herona.
- Laskurissa näkyy vihje H1 (luku 7).

### 5.7 Katselen ensin ja Ohita

Molemmat vievät Projectsiin, jossa on opastava tyhjä tila (luku 9). Onboarding merkitään suoritetuksi, eikä sitä näytetä uudelleen. Jos käsityö ehdittiin valita, oletus säilyy.

---

## 6. Ensimmäinen onnistuminen ja juhlahetket

Juhla on hillitty ja omaa kieltä: ei konfettia, ei äänitehosteita. Jokainen hetki näytetään kerran, ja järjestelmän animaatioasetusta kunnioitetaan.

| Hetki | Mitä tapahtuu |
| --- | --- |
| Ensimmäinen napautus omassa projektissa | Vihje H1 kuittautuu, ja H2 kertoo: "Kerros 38 on tallessa. Jos napautit vahingossa, Kumoa palauttaa edellisen luvun." |
| Tavoite saavutettu | `ProjectProgressBar` täyttyy, ja luvun viereen tulee `BadgePill` "Tavoite saavutettu". Vihje H9 kertoo, että voit jatkaa yli tai merkitä työn valmiiksi. |
| Ensimmäinen tallennettu työskentely | Yhteenvedon jälkeen kerran: "Ensimmäinen työskentely tallessa: 42 min. Näet kokonaisajan projektinäkymässä." |
| Projekti valmis | Uusi **Päätelty**-sheet: projektin uusin kuva tai iso lankavärinen `FabricSwatch`, nimi, työskentelyn kokonaisaika, laskurin lopullinen luku ja aloitus- ja valmistumispäivä allekkain. Toiminnot: "Aloita seuraava projekti" (ilmainen, koska aktiivinen paikka vapautui) ja neutraali Sulje. |

Päätelty-hetki sulkee kehän: valmistuminen vapauttaa ilmaisen paikan, ja seuraava projekti alkaa samasta näkymästä.

---

## 7. Silmukkamerkkivihjeet (opetus käytön yhteydessä)

### 7.1 Ulkoasu

- Silmukkamerkki on pieni koodilla piirretty lukittuva silmukkamerkki (rengas, jossa kapea lukko), `primary`-värinen. Se kiinnittyy kohde-elementin reunaan.
- Kupla: `knitToolsColors.cardContainer`, `shapes.medium`, teksti `bodyMedium`, oikeassa alakulmassa tekstipainike "Selvä". Pieni nokka osoittaa kohteeseen. Ei vasemman reunan väripalkkia, ei Material-tooltipin oletusulkoasua.
- Kupla ei koskaan peitä kohdetta, johon se viittaa. Plusnapin vihje on napin yläpuolella, jotta napautus onnistuu suoraan.
- Uusi jaettu komponentti `StitchMarkerHint` ja ankkuri `Modifier.hintAnchor(HintId)`.

### 7.2 Säännöt

1. Yksi vihje kerrallaan. Enintään yksi uusi vihje per sovelluksen avaus. Poikkeus on ketju H1 → H2.
2. Jokainen vihje näytetään kerran. Kuittaus tallentuu (`hints_seen`).
3. Vihje laukeaa käyttäjän toiminnasta tai tilasta, ei kellosta.
4. Ei sheetin, dialogin, valintatilan tai aktiivisen tekstinsyötön päälle.
5. Ei kahteen sekuntiin näkymän avautumisesta (käyttäjä ehtii orientoitua).
6. Napautus mihin tahansa kuplan ulkopuolelle kuittaa vihjeen ja välittää napautuksen alla olevalle elementille. Laskeminen ei koskaan esty.
7. Ruudunlukija: kupla ilmoitetaan kohteliaana live regionina, siihen pääsee fokuksella, ja Selvä on oma toimintonsa. Fokusta ei siirretä väkisin.
8. Asetuksissa (Info) on "Näytä vinkit uudelleen", joka tyhjentää kuittaukset.

### 7.3 Vihjeluettelo

| Tunnus | Laukaisu | Ankkuri | Teksti (FI) | Ehto |
| --- | --- | --- | --- | --- |
| H1 Ensimmäinen kerros | Laskurin ensimmäinen avaus onboardingin jälkeen | Plusnappi | Kun kerros on valmis, napauta plusnappia. Voit napauttaa myös isoa lukua. | Kuittautuu myös ensimmäisestä napautuksesta |
| H2 Kumoa | Ensimmäinen lisäys | Kumoa | Kerros 38 on tallessa. Jos napautit vahingossa, Kumoa palauttaa edellisen luvun. | Häviää itsestään 6 s kuluttua |
| H3 Askelkoko | Kolme lisäystä kahden sekunnin sisällä tai 30 lisäystä | ×1-rengas | Lasketko kaksi kerrosta kerralla? Muuta askelta tästä. | Vain jos askel on 1 |
| H4 Työskentely | Toinen laskurikäynti eri päivänä ilman istuntoja. Heti ensimmäisellä käynnillä, jos aika valittiin tavoitteeksi. | Start | Haluatko tietää, kauanko työhön menee? Aloita työskentely. Aika ei käynnisty itsestään. | Ei aktiivista istuntoa |
| H5 Liitä ohje | Projektilla ei ole ohjetta ja laskurikäyntejä on vähintään kaksi | Ohje-laatta | Liitä ohje, niin lukukaista seuraa laskuria. | Ohje valittu tavoitteeksi tai kolme käyntiä |
| H6 Lukukaista | Ensimmäinen PDF:n avaus laskurista | Lukukaista | Kaista siirtyy, kun napautat plusnappia. Vedä se oikealle riville. | Tarkistetaan nykyisen kalibroinnin ele toteutusvaiheessa |
| H7 Projektinäkymä | Ensimmäinen projektinäkymän avaus | Jatka-kortti | Tässä on koko projektisi: ohje, langat, muistiinpanot ja kuvat. Jatka vie laskuriin. | |
| H8 Näyttö päällä | Kolmas laskurikäynti ja asetus pois päältä | Laskurin otsikkoalue | Sammuuko näyttö kesken neulomisen? Pidä näyttö päällä laskurissa. Toiminto: Ota käyttöön | Toiminto kytkee nykyisen Keep Screen Awake -asetuksen |
| H9 Tavoite saavutettu | Luku saavuttaa tavoitteen ensimmäisen kerran | Edistymispalkki | Tavoite saavutettu. Voit jatkaa laskemista tai merkitä työn valmiiksi projektinäkymän valikosta. | |
| H10 Lankakirja | Ensimmäinen My Yarn -käynti | Lisää lanka | Lisää langan tiedot kerran, niin voit liittää sen mihin tahansa projektiin. | Lanka valittu tavoitteeksi |
| H11 Laskimet | Ensimmäinen Tools-käynti | Ensimmäinen laskin | Laskimet toimivat ilman nettiä. Esimerkkiarvot näyttävät, mitä kenttään kirjoitetaan. | |
| H12 Varmuuskopio | 7 päivää käyttöä ja vähintään yksi projekti, ei varmuuskopiota | Projects, kertakortti listan alla | Projektisi ovat vain tässä puhelimessa. Tee varmuuskopio, niin ne säilyvät puhelimen vaihtuessakin. Toiminto: Varmuuskopioi | Kerran, suljettavissa |
| H13 Kansiot | Vähintään neljä projektia | Projects-otsikon valitsin | Järjestä projektit kansioihin otsikosta. | |
| H14 Widget | Ensimmäinen tallennettu työskentely tai kolmas käyttöpäivä | Laskurin ylivuotovalikko | Laske suoraan kotinäytöltä. Toiminto: Lisää widget | Pro tai kokeilu. Ilman oikeutta totuudenmukainen PRO-merkki (luku 11.9). |

H12 on tärkeä paikalliselle sovellukselle: luottamus syntyy siitä, että sovellus kertoo rehellisesti, missä data on.

---

## 8. Projektin alku -lista

Ensimmäisen projektin näkymässä (`onboarding_project_id`) heti jatka-kortin alla on hiusviivaosio **Projektin alku**. Ei korttia, sama `OverviewSectionHeader`-rakenne kuin muissa osioissa.

- Otsikkorivillä `StitchProgressRow` (5 silmukkaa projektin lankavärillä) ja oikeassa reunassa "3/5".
- Kohdat ovat `OverviewLinkRow`-rivejä (`CrossStitchGlyph`). Tehty kohta näkyy himmeämpänä ja checkillä, eikä sitä voi napauttaa.
- Kohtien tila lasketaan olemassa olevasta datasta, joten uutta taulua ei tarvita. Tallennetaan vain piilotus.

| Kohta | Valmis kun | Toiminto |
| --- | --- | --- |
| Kerros ja tavoite | Luku tai tavoite asetettu onboardingissa | Valmis jo onboardingista (ensimmäinen silmukka neulottu) |
| Liitä ohje | Projektilla on dokumentti tai verkko-ohje | Avaa ohjeen valinnan |
| Lisää lanka | Projektilla on lankamuistiinpano tai linkitetty lankakortti | Avaa Yarn-sheetin |
| Ota kuva työstä | Projektilla on edistymiskuva | Avaa kameran (edellyttää päätöstä D2, muuten kohta jätetään pois) |
| Tallenna ensimmäinen työskentely | Projektilla on tallennettu istunto | Vie laskuriin, ankkuri Start |

Järjestys seuraa avun kohteita: valittu kohde nousee ylös. Kun kaikki on tehty, rivi neuloutuu loppuun, osio näyttää kerran tekstin "Projekti on valmiina. Hyvää neulontaa!" ja poistuu pysyvästi. Ylivuotovalikossa on Piilota.

Listalla ei ole yhtään maksullista kohtaa. Tämä on yksi peruste päätökselle D2: jos kuvat jäävät Pron taakse, kohta jätetään pois.

---

## 9. Tyhjät tilat

### 9.1 Projects ilman projekteja

Nykyinen ACTIVE (0) ja "No active projects" korvataan opastavalla tilalla:

- Iso `FabricSwatch` oletuskäsityön pinnalla (sama 96 dp tyhjän tilan grafiikka kuin `CrossStitchEmptyState`issa).
- Otsikko käsityön mukaan: "Mitä sinulla on puikoilla?" tai "Mitä sinulla on työn alla?"
- Kaksi `SheetOptionRow`-valintaa:
  - "Lisää kesken oleva työ", alateksti "Aloita siitä kerroksesta, jossa olet nyt". Avaa `ProjectDetailsSheet`in, jossa aloituskerroskenttä on näkyvissä.
  - "Aloita uusi työ". Avaa `ProjectDetailsSheet`in tavalliseen tapaan.
- Tekstitoiminto "Tuo PDF-ohje", joka käyttää olemassa olevaa polkua `createProjectWithImportedPdf`.
- New Project -pilleri säilyy alareunassa kuten nyt.

### 9.2 Muut tyhjät tilat

Periaate: tyhjä tila kertoo, mitä tähän tulee ja miksi siitä on hyötyä, ja tarjoaa yhden toiminnon.

| Näkymä | Nyt | Ehdotus |
| --- | --- | --- |
| My Yarn | No yarn notes saved yet. | Kerrattu lankatilkku ja "Kirjaa langat kerran, niin näet aina värinumeron ja värjäyserän." Toiminto Lisää lanka. |
| Saved Patterns | No saved patterns yet. Add a web pattern or import a PDF. | Lukukaistakohtaus pienenä ja "Tallenna ohje, niin voit liittää sen projektiin ja lukea sitä laskurin vieressä." |
| Insights | No work sessions yet. | Säilyy, lisätään linkki viimeisimpään projektiin: "Avaa Raitasukat ja aloita työskentely." |
| Projektin kuvat | Photos you take of this project appear here. | Säilyy. Lisäksi Projektin alku -listan kohta. |

---

## 10. Pienet ensikäytön parannukset

1. **Aloituskerros kaikille:** `ProjectDetailsSheet`in uuteen projektiin valinnainen kenttä "Montako kerrosta olet jo neulonut?". Muokkauslomakkeeseen sitä ei tule, koska luvun muutokset kulkevat laskurin historian kautta.
2. **Tavoite luontivaiheessa:** sama valinnainen tavoitekenttä kuin onboardingissa.
3. **Yksiköt päätellään:** ensikäynnistyksessä `use_imperial` asetetaan alueen mukaan (esimerkiksi US, LR, MM imperial, muut metrinen). Ei kysymystä.
4. **Settings → Info:** uusi rivi "Näytä vinkit uudelleen".
5. **Analytiikka:** `project_overview` ja `onboarding` näkymäsallilistalle.

---

## 11. Pro uudelleen

### 11.1 Periaatteet

1. **Ilmainen tekee yhdestä projektista täydellisen. Pro antaa tilaa useammalle ja syvemmät työkalut.** Kaikki, mitä tarvitaan yhden työn aloittamiseen, seuraamiseen ja valmiiksi saamiseen, on ilmaista.
2. **Ensimmäinen onnistuminen ei osu maksumuuriin.** Onboarding ja Projektin alku -lista toimivat kokonaan ilman Prota.
3. **Merkki kertoo totuuden.** PRO näkyy vain toiminnossa, joka juuri nyt avaisi Pro-kehotteen. Merkki ei koskaan kuvaa osiota, laattaa tai olemassa olevaa sisältöä.
4. **Arvo ennen pyyntöä.** Kehote kertoo, mitä käyttäjä saa juuri tässä tilanteessa, ei listaa ominaisuuksia.
5. **Malli A säilyy:** oma sisältö pysyy aina käytettävissä, vain uuden luominen voi vaatia Pron.
6. **Ei piilotettuja Pro-ominaisuuksia.** Jokainen Pro-toiminto näkyy ilmaiskäyttäjälle merkittynä.

### 11.2 Nykyiset PRO-merkit ja arvio

| # | Paikka | Lähde | Arvio |
| --- | --- | --- | --- |
| 1 | Projektin toimintosheet, rivi Add counter | `ProjectActionsBottomSheet.kt:153` | Oikea paikka. Muutetaan totuudenmukaiseksi. Päätös D4 muuttaa ehtoa. |
| 2 | Laskurin työtila, Extra counters -osion Add counter | `CounterWorkspaceSections.kt:238` | Oikea paikka, sama muutos kuin 1. |
| 3 | Muistutukset-sheetin otsikkorivi | `ReminderComponents.kt:92` | **Väärä paikka.** Merkki on otsikon vieressä ja antaa ymmärtää, että koko muistutusosio on maksullinen, vaikka olemassa olevat muistutukset toimivat aina. Siirretään Add-toimintoon. |
| 4 | Projektin lanka-sheet, Save to My Yarn | `YarnManagementSheet.kt:334` | Oikea paikka. Päätös D3 muuttaa ehtoa. |
| 5 | Tallenna ohje -sheet, Aloita uusi projekti | `IncomingPdfSheet.kt:84` | **Malli muille.** Merkki näkyy jo vain silloin, kun toiminto oikeasti avaisi kehotteen (`canStartProject`). |
| 6 | My Yarn, Add Yarn -pilleri ja tyhjän tilan painike | `MyYarnScreen.kt:229`, `:396` | Merkki näkyy nyt aina ilman oikeutta. Päätös D3 muuttaa ehtoa. |
| 7 | Muistiinpanoeditorin Add note -painike | `NotesEditorScreen.kt:213` | Täytetty päätoimintopainike, jonka sisällä PRO. Jos päätös D1 hyväksytään, merkki ja kehote poistuvat. |
| 8 | Ohjevalitsin, Choose images ja Camera scan | `PatternPickerSheet.kt:530`, `:536` | Oikea paikka. Muutetaan totuudenmukaiseksi. |
| 9 | Projektin kuvagalleria, Take photo -pilleri | `PhotoGalleryScreen.kt:256` | Jos päätös D2 hyväksytään, merkki poistuu. |

Yhteiset ongelmat:
- **Merkki näkyy kokeilun aikana** täsmälleen samannäköisenä kuin ilman oikeutta. Kokeilija ei tiedä, voiko toimintoa käyttää.
- **Ulkoasu on geneerinen**: pieni oliivinen pilleri raa'alla `secondaryContainer`-värillä. Ei omaa tokenia eikä KnitToolsin omaa ilmettä.
- **Saman toiminnon merkintä vaihtelee paikasta toiseen** (luku 11.3).

### 11.3 Portit, joilta merkki puuttuu

| Portti | Missä käyttäjä törmää siihen | Nyt | Ehdotus |
| --- | --- | --- | --- |
| Toinen aktiivinen projekti | New Project -pilleri, kun aktiivinen projekti on jo | Ei merkkiä, kehote yllättää | Totuudenmukainen merkki pillerissä |
| Projektin uudelleenaktivointi | Projektinäkymän ylivuotovalikko | Ei merkkiä | Merkki valikkorivin oikeaan reunaan |
| Muistiinpanojen, kuvien ja muistutusten lisäys | Projektinäkymän osioiden Add-toiminnot (`OverviewTextAction`) | Ei merkkiä, vaikka samat toiminnot sheeteissä on merkitty | Sama totuudenmukainen merkki kaikkialla tai merkki poistuu, jos D1/D2 |
| Kotinäytön widget | Ei mitään sisäänkäyntiä sovelluksessa | Kehote vasta, kun widgetiä napauttaa kotinäytöllä | Uusi Lisää widget -toiminto (H14 ja laskurin ylivuotovalikko) |
| Ohjetekstin liittäminen laskimeen | Increase/Decrease ja Gauge | **Kokonaan piilossa** ilmaiskäyttäjältä, ei `ProFeature`-listalla | Päätös D6 |
| Insights-kaaviot ja putki | Insights | Kortiton editoriaalinen kehote | Säilyy ennallaan, sopii Insightsin linjaan |

### 11.4 Ominaisuuksien sijoittelu: ilmainen vai Pro

Arviointikysymykset: kuuluuko toiminto yhden projektin peruskäyttöön? Luoko se kiintymystä omaan työhön (pitää käyttäjän sovelluksessa, jolloin hän ehtii tarvita Prota)? Onko se määrää, tehoa vai syvempää ymmärrystä?

| Ominaisuus | Nyt | Ehdotus | Peruste |
| --- | --- | --- | --- |
| Useampi aktiivinen projekti (`UNLIMITED_PROJECTS`) | Pro | **Pro** | Vahvin ja reiluin syy maksaa. Useimmilla neulojilla on useita töitä kesken. |
| Muistiinpanot (`NOTES`) | Pro (projektin ensimmäinen muistiinpano) | **Ilmainen** (D1) | Perustarve ("vaihdoin 4 mm puikkoihin"). Nyt ensimmäisen projektin Notes-laatta johtaa maksumuuriin ensimmäisenä päivänä. |
| Edistymiskuvat (`PROGRESS_PHOTOS`) | Pro | **Ilmainen** (D2) | Kuva tekee projektikortista ja projektinäkymän herosta käyttäjän oman. Kiintymys omaan työhön pitää sovelluksessa, ja se johtaa toisen projektin tarpeeseen. |
| Lankakortit (`UNLIMITED_YARN`) | Pro, ei yhtään ilmaista | **5 ilmaista, sitten Pro** (D3) | Lankavarastoa ei voi nyt kokeilla lainkaan. Nimi UNLIMITED olettaa rajan, jota ei ole. |
| Lisälaskurit (`MULTIPLE_COUNTERS`, `SECONDARY_COUNTER`) | Pro | **Yksi ilmainen lisälaskuri projektia kohden** (tavallinen tai toistuva), loput Pro (D4) | Kerrokset ja mallikerrat on tavallisin yhdistelmä. Yksi lisälaskuri kuuluu peruskäyttöön. |
| Muotoilu- ja toisto-osiolaskurit | Pro | **Pro** | Edistynyt toiminto. |
| Rivimuistutukset | Pro | **Pro** | Mukavuus, ei perustarve. |
| Kuvista PDF | Pro | **Pro** | Selkeä lisäarvo. Tarjotaan onboardingissa tarpeen hetkellä (paperiohje). |
| Kotinäytön widget | Pro | **Pro**, mutta löydettäväksi | Hyvä tottumus ja hyvä kokeilun esittely. Nyt sitä ei löydä sovelluksesta. |
| Insights-kaaviot | Pro | **Pro** | Syvempi ymmärrys. |
| Putki (`STREAK`) | Pro | **Nykyinen putki ilmainen, paras putki ja kaaviot Pro** (D5) | Putki on tottumuksen koukku. Se pitää ilmaiskäyttäjää sovelluksessa, mikä palvelee myös Pron myyntiä. |
| Ohjetekstin liittäminen laskimeen | Pro, piilossa | **Ilmainen** (D6) tai näkyvä Pro-toiminto merkillä | Piilotettu toiminto ei myy mitään. Se tukee vain englantia, joten sitä on vaikea myydä 11 kielellä. |

Jos ehdotukset hyväksytään, Pro sisältää edelleen: useamman aktiivisen projektin, rajattomat lisälaskurit, muotoilu- ja toisto-osiolaskurit, rivimuistutukset, kuvista PDF:n, kotinäytön widgetin, rajattoman lankavaraston, Insights-kaaviot ja parhaan putken. Pron arvolupaus selkenee muotoon **"enemmän töitä, syvemmät työkalut, parempi näkymä edistymiseen"**.

Ajoitus: sovellusta ei ole vielä julkaistu Play-kauppaan. Rajojen siirtäminen on nyt halpaa. Julkaisun jälkeen ilmaisen ominaisuuden siirtäminen Pron taakse olisi kallista luottamukselle.

### 11.5 Merkin uusi ulkoasu

Uusi jaettu komponentti `ProMarker` korvaa `ProBadge`n ulkoasun. Värit omista tokeneista `knitToolsColors.proMarkerContainer` ja `knitToolsColors.proMarkerContent`, molempiin teemoihin. Tekstin kontrasti vähintään 4,5:1 (11 sp), lukitaan testillä kuten `YarnPaletteContrastTest`.

Vaihtoehdot mallikuviin:

- **A. Kudottu merkki (suositus).** Pieni suorakaide 4 dp pyöristyksellä, kullansävyinen pohja ja sisäreunassa ohut katkoviivainen pisto kuin vaatteeseen ommellussa kudotussa nimilapussa. Teksti PRO. Käsityöläiset ompelevat tällaisia lappuja valmiisiin töihinsä, joten merkki kuuluu tähän maailmaan eikä näytä sovelluskaupan yleismerkiltä. Huom: nykyinen tertiary-kulta `#9A7B18` vaalealla kermalla jää noin 3:1 kontrastiin, joten tekstiin tarvitaan tummempi kultatoken.
- **B. Ristipistotähti.** Teksti PRO kullalla ja sen edessä pieni `CrossStitchGlyph`-tähti. Kevyempi, sopii ristipistokuvakkeiden kanssa.
- **C. Pelkkä teksti.** PRO kultaisella `labelSmall`-tekstillä ilman pohjaa. Hiljaisin vaihtoehto.

Lukittu toiminto ei koskaan näytä käytöstä poistetulta. Se on samannäköinen kuin muut, merkki oikeassa reunassa, kuten nykyinen sääntö sanoo.

### 11.6 Merkin näkyvyys

Uusi resolver `ProActionState` laskee yhdessä paikassa, mitä toiminto juuri nyt tekisi:

```
Allowed           → ei merkkiä, toiminto suoritetaan
LockedTrialReady  → merkki, kehote tarjoaa kokeilua
LockedTrialUsed   → merkki, kehote tarjoaa ostoa
```

Esimerkkejä kontekstista: New Project on lukittu vain, kun aktiivisia projekteja on vähintään yksi eikä oikeutta ole. Add Yarn on lukittu vain, kun ilmaiset kortit on käytetty (D3). Add counter on lukittu vain, kun projektin ilmainen lisälaskuri on käytetty (D4). Kaikki merkit ja kehotteet käyttävät samaa resolveria, joten merkki ja kehote eivät voi olla ristiriidassa. `UiConsistencySourceTest` estää `ProMarker`in käytön ilman `ProActionState`a.

**Kokeilun aikana** merkkejä ei näytetä, koska toiminnot ovat käytössä. Kokeilun tila näkyy keskitetysti: Settingsin Pro-rivillä ("Pro-kokeilu, 9 päivää jäljellä") ja Pro-sivulla. Vaihtoehto päätökseen D7: kolmena viimeisenä päivänä merkit palaavat ohuempina, jotta käyttäjä oppii ennen kokeilun loppua, mitkä toiminnot ovat Pro-toimintoja.

### 11.7 Kontekstikehote

`ProPromptSheet` säilyttää rakenteensa (`SheetTitle`, teksti, neutraali `CancelButton`, yksi täytetty päätoiminto, odottava toiminto jatkuu). Muutokset:

1. Yläosaan pieni toiminnon oma kuva koodilla: kaksi projektikorttia rinnakkain (projektit), lisälaskurin luku (laskurit), kudottu kotinäyttö (widget).
2. Teksti puhuu käyttäjän tilanteesta. Esimerkki projekteista:
   - Otsikko: "Kaikki keskeneräiset työt samaan paikkaan"
   - Teksti: "Ilmaisversiossa työn alla on yksi projekti kerrallaan. Pro-kokeilulla voit lisätä kaikki keskeneräiset työsi. Kokeilu on maksuton 14 päivää eikä vaadi maksutapaa. Kaikki luomasi säilyy."
3. Peruutuksen sanamuoto säilyy neutraalina (Ei nyt). Ei syyllistäviä muotoiluja.

### 11.8 Pro-sivu

Rakenne kortittomana editoriaalisena sivuna kuten Insights:

1. Otsikko ja yksi lause arvosta: "Enemmän töitä, syvemmät työkalut."
2. **Henkilökohtainen rivi**, kun siihen on dataa: "Sinulla on 3 projektia työn alla" tai kokeilun jälkeen "Kokeilun aikana loit 2 projektia ja 3 lisälaskuria. Ne säilyvät."
3. **Ilmaiseksi aina** -osio (`SectionLabel`): yksi aktiivinen projekti, laskuri ja askelkoko, PDF-lukija ja lukukaista, merkinnät, muistiinpanot ja kuvat (jos D1/D2), työskentelyaika, laskimet ja taulukot, varmuuskopio. Tämä rakentaa luottamusta ja vastaa kysymykseen "mitä menetän".
4. **Pro lisää** -osio: kolme ryhmää kuten nyt, sisältö päivitettynä päätösten mukaan.
5. Osto-osio ennallaan: hinta Playstä, "Kertaosto, ei tilausta", palautus.

### 11.9 Kokeilun polku

| Hetki | Mitä tapahtuu |
| --- | --- |
| Kokeilu alkaa | Sheet "Pro-kokeilu alkoi" ja lista asioista, jotka kannattaa kokeilla. Jokaisella oma toiminto: Lisää widget kotinäytölle (`requestPinAppWidget`, jos käynnistin tukee), Lisää toistolaskuri, Lisää toinen projekti, Katso viikon kaavio, Kuvaa paperiohje PDF:ksi. Edistyminen `StitchProgressRow`illa. Sama lista löytyy myöhemmin Pro-sivulta. |
| 3 päivää jäljellä | Kerran sovelluksessa (ei ilmoitusta): "Kokeilua on jäljellä 3 päivää. Tähän mennessä olet luonut 2 projektia ja lisännyt widgetin. Kaikki säilyy, vaikka kokeilu päättyy." Toiminnot: Katso Pro ja Selvä. |
| Kokeilu päättyy | Nykyinen ilmoitus (`pro_trial_ended_*`) saa saman henkilökohtaisen yhteenvedon. Jatka ilmaiseksi säilyy yhtä painavana kuin Katso Pro. |

Yhteenvedon luvut lasketaan olemassa olevista aikaleimoista kokeilun alun jälkeen (projektien `createdAt` ja muut, tarkistetaan toteutuksessa).

### 11.10 Mitä ei tehdä

- Ei maksumuuria käynnistyksessä eikä onboardingissa ennen ensimmäistä onnistumista.
- Ei vastakkain asetettuja painikkeita, joista toinen syyllistää ("Ei kiitos, en halua parempaa").
- Ei keinotekoisia ajastimia, alennuslaskureita tai "vain tänään" -tarjouksia.
- Ei Pro-merkkiä osion otsikossa, laatassa tai olemassa olevassa sisällössä.
- Ei piilotettuja Pro-toimintoja.

---

## 12. Ilmoituslupa

Lupaa ei kysytä nyt, koska ilmoituksia ei ole. Pyytäminen ilman käyttöä olisi juuri sitä, mitä käytäntö 11 kieltää.

Mahdollinen tuleva ilmoitus (luku 18): **käynnissä olevan työskentelyn ilmoitus**, jossa kulunut aika ja Stop. Silloin lupa pyydetään vasta, kun käyttäjä painaa Start ensimmäisen kerran: sheet näyttää esikatselun ilmoituksesta ("Raitasukat, työskentely käynnissä 0:42, Stop") ja perustelun "Näet ajan lukitusnäytöltä etkä unohda lopettaa". Vasta sen jälkeen järjestelmän lupadialogi. Kieltäytyminen ei estä työskentelyä.

---

## 13. Tekninen toteutus

### 13.1 Tila ja tallennus

Uudet avaimet `knittools_preferences`-DataStoreen `PreferencesManager`in kautta (yksi lähdetotuus asetuksille):

| Avain | Tyyppi | Käyttö |
| --- | --- | --- |
| `onboarding_completed_version` | Int | 0 = ei suoritettu. Versio mahdollistaa myöhemmän päivitetyn onboardingin. |
| `default_craft_type` | String? | KNITTING, CROCHET tai tyhjä |
| `onboarding_goals` | Set<String> | PATTERN, YARN, TIME, TOOLS |
| `onboarding_project_id` | Long? | Projektin alku -listaa varten |
| `project_start_list_hidden` | Boolean | Listan piilotus |
| `hints_seen` | Set<String> | Kuitatut vihjeet |
| `hint_counters` | Preferences-avaimet | Laskurikäynnit, käyttöpäivät ja muut vihjeiden laukaisutiedot |

Asetukset eivät ole varmuuskopiossa (nykyinen raja). Varmuuskopion palautus merkitsee onboardingin suoritetuksi ja perusvihjeet (H1–H3, H7) kuitatuiksi, koska palauttaja on jo käyttäjä.

**Olemassa oleva käyttäjä:** ensimmäisellä käynnistyksellä uuden version jälkeen, jos tietokannassa on yksikin projekti, tallennettu ohje tai lankakortti, onboarding merkitään suoritetuksi ja perusvihjeet kuitatuiksi. Koska julkaisua ei ole vielä tehty, tämä koskee lähinnä testilaitteita, mutta se on halpa tehdä oikein.

### 13.2 Käynnistys ja navigointi

- `MainActivity`n splash odottaa teeman lisäksi onboarding-tilan lukemista (sama DataStore-luku), jotta Projects ei välähdä ennen onboardingia.
- Uusi juuritason reitti `onboarding` (`Screen.Onboarding`) ja dynaaminen `startDestination`. Alanavigaatio piilotetaan reitillä kuten Ravelry-selaimessa.
- Valmistuessa `popUpTo(onboarding) { inclusive = true }`, sitten Projects-graafiin ja laskuriin nykyisellä `CounterLaunchRequest`-mekanismilla, jotta pino on sama kuin uuden projektin luonnissa.
- `OnboardingViewModel` (Hilt) pitää vaiheen ja luonnoksen `SavedStateHandle`ssa, joten prosessin kuolema ei hävitä syötettyä.

### 13.3 Ulkoiset avaukset ensikäynnistyksessä

Jaettu PDF, Avaa-toiminto, jaettu Ravelry-linkki, widget, laskurin launch request ja Pro-avaus ohittavat onboardingin. Pyydetty toiminto suoritetaan ensin. Jos käyttäjä luo sen kautta projektin, onboarding merkitään suoritetuksi ja vihjeet hoitavat opetuksen. Jos projektia ei synny, onboarding näytetään seuraavalla tavallisella käynnistyksellä.

### 13.4 Repository-muutokset

- `CounterRepository.createProject(..., initialCount: Int = 0, targetRows: Int? = null)`: validointi samassa transaktiossa kuin aktiivisen projektin kiintiö. Ei historiariviä aloitusluvulle.
- Päätösten mukaan (luku 17): `ProFeature`-tarkistukset muuttuvat määrätietoisiksi (lankakorttien ja lisälaskurien ilmainen määrä) repositoryn rajalla, ei vain UI:ssa. Malli A ja monotoniset liput (`notesCreated`, `secondaryCounterUsed`) säilyvät.
- Kokeilun yhteenveto: kevyt kysely luotujen kohteiden määristä kokeilun alun jälkeen.

### 13.5 Uudet ja muutettavat tiedostot

Uudet:
- `ui/screens/onboarding/OnboardingScreen.kt`, `OnboardingViewModel.kt`, `OnboardingSteps.kt` (vaiheet), `WelcomeScene.kt` (tervetulokohtaus ja ohjesivupiirros)
- `ui/components/StitchProgressRow.kt`
- `ui/components/StitchMarkerHint.kt` ja `HintHost` (ankkurointi, yksi vihje kerrallaan)
- `ui/components/ProMarker.kt` ja `pro/ProActionState.kt`
- `ui/screens/project/ProjectStartListSection.kt` (Projektin alku)
- `ui/screens/project/ProjectCompletedSheet.kt` (Päätelty)
- `domain/onboarding/HintRules.kt` (puhtaat laukaisusäännöt, yksikkötestattavat)
- `repository/HintRepository.kt` (kuittaukset ja laskurit DataStoreen)
- `ui/theme/OnboardingDimens.kt`

Muutettavat:
- `SheetOptionRow` (`leading`, `selected`), `CrossStitchGlyph` (uudet `COUNTER`, `CLOCK`, `RULER` samalla ristipistotyylillä)
- `ProjectDetailsSheet` (aloituskerros, tavoite, oletuskäsityö)
- `ProjectListScreen` (opastava tyhjä tila)
- `ProPromptSheet`, `ProUpgradeScreen`, `ProBadge`-kutsupaikat → `ProMarker`
- `ReminderComponents` (merkki Add-toimintoon), `ProjectOverviewSections` (merkit Add-toimintoihin päätösten mukaan)
- `NavGraph`, `Screen`, `MainActivity`
- `PreferencesManager`, `UsageAnalytics`, `PostHogAnalytics` (sallilista)
- `SettingsScreen` (Näytä vinkit uudelleen, kokeilun päivät)
- `Color.kt`, `Theme.kt` (`proMarkerContainer`, `proMarkerContent`)
- `strings.xml` englanti ja 11 käännöskieltä

### 13.6 Vihjejärjestelmä

- `HintRules` saa tapahtumia (laskurin muutos, näkymän avaus, istunnon tallennus, päivämäärä) ja palauttaa seuraavan näytettävän vihjeen. Puhdas Kotlin, ei Android-riippuvuuksia.
- ViewModelit lähettävät tapahtumia, `HintHost` piirtää vihjeen ankkurin kohdalle ja kuittaa sen `HintRepository`n kautta.
- Ankkurin sijainti `onGloballyPositioned`-mittauksesta. Jos ankkuri ei ole näkyvissä (vieritetty pois), vihje odottaa.

### 13.7 Analytiikka (opt-in, ei sisältöä)

Uudet `UsageEvent`-tapahtumat: `onboarding started`, `onboarding skipped`, `onboarding browse chosen`, `onboarding project created`, `onboarding pattern step completed`, `onboarding completed`, `first count after onboarding`, `hint acted`, `trial started from prompt`. Ei projektin nimiä, lukuja tai tavoitteita. Nykyinen yksityisyysraja säilyy.

### 13.8 Testit

- Yksikkötestit: `HintRules` (laukaisut, rajoitukset, ketju H1 → H2), `OnboardingViewModel` (vaiheet, takaisin, prosessin palautus, ohjevaiheen pois jääminen), olemassa olevan käyttäjän tunnistus, `ProActionState` (taulukkotestit jokaiselle toiminnolle ja tilalle), `createProject(initialCount, targetRows)` rajoineen.
- Compose-testit: koko kulku molemmilla käsitöillä, Ohita, Katselen ensin, ulkoinen avaus ensikäynnistyksessä, opastava tyhjä tila, Projektin alku -listan laskenta.
- `UiConsistencySourceTest`: uudet komponentit käyttävät tokeneita ja jaettuja komponentteja, `ProMarker` vain `ProActionState`n kautta, ei `ProBadge`-jäänteitä, ei FilterChipiä.
- Kontrastitesti `ProMarker`in tokeneille molemmissa teemoissa.
- Eristetty `activepolicy`-profiili: onboardingin projekti noudattaa aktiivisen projektin kiintiötä. Huom: debug-build avaa kaikki Pro-portit (`BuildConfig.DEBUG`), joten merkkien totuudenmukaisuus testataan eristetyllä profiililla ja `ProPolicyTestControl`illa.
- Laitetarkistukset (320 dp, 200 % fonttikoko, TalkBack) ajetaan vain erikseen pyydettäessä projektin käytännön mukaisesti.

### 13.9 Dokumentaatio

`CLAUDE.md`, `AGENTS.md` ja `CODEX.md` UI Rules (onboarding, vihjeet, Projektin alku, `ProMarker`-sääntö), `PROJECT.md` (uudet reitit, asetukset, Pro-taulukko, analytiikka) ja muistin `MEMORY.md` arkkitehtuurimuutoksen mukaisesti.

---

## 14. Saavutettavuus ja lokalisointi

- Jokaisella interaktiivisella kohteella vähintään 48 dp kosketusalue. `SegmentedToggle`n nykyiset säännöt pätevät.
- Animaatiot ohitetaan, kun järjestelmän animaatiot ovat pois (sama tarkistus kuin splashissa).
- Ruudunlukija: vaiheen otsikko saa fokuksen vaiheen vaihtuessa. Silmukkarivi ilmoittaa vaiheen numeron.
- Tekstit kirjoitetaan neule- ja virkkausversioina erillisinä resursseina, ei verbiä interpoloiden, koska taivutuskielissä lauseenosia ei voi yhdistellä.
- "Puikoilla" on idiomi, joka ei käänny kaikkiin kieliin. Kääntäjäkommentti: käytä kielen luontevaa vastinetta tai neutraalia "työn alla".
- Pitkät käännökset: otsikot rivittyvät, painikkeet eivät katkea. Mitoitus ei oleta englannin pituutta.

---

## 15. Vaiheistus

| Vaihe | Sisältö | Koko | Riippuvuus |
| --- | --- | --- | --- |
| 0 Päätökset ja mallikuvat | Päätökset D1–D8. Mallikuvat molemmissa teemoissa oikeassa koossa: tervetulokohtaus, silmukkarivi, projektivaihe esikatselukortteineen, opastava tyhjä tila, silmukkamerkkivihje, `ProMarker`-vaihtoehdot A–C. | S | |
| 1 Ydin | Onboarding-reitti ja vaiheet, aloituskerros ja tavoite luontiin, oletuskäsityö, opastava tyhjä tila, olemassa olevan käyttäjän tunnistus, analytiikka, testit | L | 0 |
| 2 Opetus | `StitchMarkerHint`, `HintRules`, vihjeet H1–H8, Projektin alku -lista, Näytä vinkit uudelleen | M | 1 |
| 3 Pro | `ProActionState`, `ProMarker`, merkkien siirrot ja lisäykset, kehotteiden tekstit, Pro-sivu, päätösten D1–D7 portit | M | 0 |
| 4 Kokeilu ja juhla | Kokeilun lista ja yhteenvedot, widgetin kiinnitys, Päätelty-sheet, loput vihjeet H9–H14 | M | 2, 3 |
| 5 Valinnaiset | Luvun 18 ideat erikseen päätettäessä | | |

Vaihe 3 voidaan tehdä rinnakkain vaiheen 1 kanssa, koska ne koskevat eri tiedostoja (lukuun ottamatta `ProjectListScreen`iä, joka kannattaa tehdä samassa vaiheessa).

---

## 16. Riskit

| Riski | Torjunta |
| --- | --- |
| Onboarding tuntuu pitkältä | Enintään kuusi näkymää, Ohita aina, ohjevaihe vain tarvittaessa, tavoite alle 90 s |
| Vihjeet ärsyttävät | Yksi per avaus, kerran, toiminnan perusteella, ei koskaan laskemisen tiellä, Näytä vinkit uudelleen |
| Ravelry-polku ennen lupaa | Valinta näytetään vain, kun lupa on saatu |
| Ilmaisen laajennus heikentää Pron myyntiä | Pro säilyttää vahvimmat syyt (projektit, widget, kaaviot). Mitataan kokeilun aloitukset lähteittäin. Päätös tehdään ennen julkaisua. |
| Käännösten pituus ja idiomit | Erilliset neule- ja virkkausresurssit, kääntäjäkommentit, rivittyvä asettelu |
| Debug-buildin avoimet portit peittävät virheet | Merkkien testaus eristetyllä profiililla |
| Aloitusluku sekoittuu historiaan | Aloitusluku ei luo historiariviä. Kumoa ei mene sen alle. |

---

## 17. Päätettävät asiat

| # | Päätös | Suositus |
| --- | --- | --- |
| D1 | Muistiinpanot ilmaisiksi | Kyllä |
| D2 | Edistymiskuvat ilmaisiksi | Kyllä (Projektin alku -listan kuvakohta riippuu tästä) |
| D3 | Lankakorttien ilmainen määrä | 5 korttia |
| D4 | Yksi ilmainen lisälaskuri projektia kohden | Kyllä (tavallinen tai toistuva) |
| D5 | Nykyinen putki ilmaiseksi, paras putki ja kaaviot Pro | Kyllä |
| D6 | Ohjetekstin liittäminen laskimiin | Ilmaiseksi ja näkyviin (vaihtoehto: näkyvä Pro-toiminto merkillä) |
| D7 | PRO-merkit kokeilun aikana | Piilossa, keskitetty kokeilutila Settingsissä ja Pro-sivulla (vaihtoehto: palaavat 3 viimeisenä päivänä) |
| D8 | `ProMarker`in ulkoasu | A, kudottu merkki (päätetään mallikuvista) |

---

## 18. Ajatuksia laatikon ulkopuolelta (valinnaiset)

1. **Valmistumiskortti.** Päätelty-sheetistä jaettava kuva: projektin kuva, nimi, kokonaisaika, aloitus- ja valmistumispäivä ja pieni KnitTools-tunniste. Neulojat jakavat valmiita töitä Instagramissa ja Ravelryssä, joten jokainen jako esittelee sovelluksen. Tehdään paikallisesti, ei palvelinta.
2. **Oma esimerkkiohje.** Sovelluksen mukana tuleva lyhyt, itse tehty karttaliinan ohje PDF:nä. "Kokeile lukukaistaa esimerkkiohjeella" näyttää PDF-ominaisuuden ilman omaa tiedostoa. Vaatii tekijänoikeudellisesti puhtaan, itse kirjoitetun ohjeen kaikilla kielillä tai kielineutraalina kaaviona.
3. **Lankavärin valinta onboardingissa.** Käyttäjä valitsee projektin värin kahdeksan sävyn paletista, ja tilkku, kortti ja widget käyttävät sitä. Vaatii uuden sarakkeen ja Room-migraation, koska väri lasketaan nyt projektin tunnisteesta.
4. **Käynnissä olevan työskentelyn ilmoitus** (luku 12). Hyödyllinen, koska aika ei käynnisty eikä pysähdy itsestään, ja Stop unohtuu helposti.
5. **Paluutervehdys.** Kun sovellus avataan yli kolmen päivän tauon jälkeen, Continue-heron yläriville kerran "Jatketaan siitä, mihin jäit: kerros 41". Pieni asia, joka vahvistaa lupauksen "paikkasi pysyy tallessa".
