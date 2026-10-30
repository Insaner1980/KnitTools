# KnitTools: mitä on testattu ja mitä vielä tarvitaan

Julkaisutarkistusten tilanne 29.10.2026; testien yksilöintiä, 30.10. valmistunutta onboarding-kampanjaa ja aineiston säilytyslistaa täydennetty 30.10.2026. Tämä on koonti olemassa olevista testiraporteista. Koontia täydennettäessä ei ajettu sovellustestejä eikä poistettu aineistoa.

**Lukujen 1–9 julkaisutarkistukset koskevat niiden alkuperäisiä versioita.** Versio- ja ympäristörekisteri sekä myöhempi onboarding-näyttö ovat luvussa 10; `reports`-kansion läpikäynti ja säilytettävä aineisto luvussa 11. Käyttäjä korjaa koodia samanaikaisesti toisessa chatissä: tämä koonti ei hyväksy muuttuvaa työpuuta testatuksi eikä muuta sen koodia. P1–P4/T1–T7-tiloja ei suljeta siivouksen tai uuden testimäärän perusteella.

## 1. Missä nyt ollaan?

**Sovellusta on jo testattu paljon. Kaikkea ei tarvitse aloittaa alusta.** Tässä koonnissa yksilöity 29.10. allekirjoitettu versio on rakennettu, sen paketti on tarkistettu ja sen käynnistys sekä Asetukset on testattu Pixel 9:llä. Aiemmissa testikierroksissa löydetyt vahvistetut korjaukset on käsitelty raportoitujen rajojensa sisällä. Tämä paketti ei sisällä sen jälkeen tehtyä onboarding-työtä tai parhaillaan tehtäviä korjauksia.

Jäljellä on muutama nimetty puhelintarkistus, analytiikan tekninen rajoite, mittaustuloksen ja testikattavuuden koontia sekä ulkoisten palvelujen tarkistuksia. Lisäksi Play-ostot ja Play-asennukset odottavat Console-valmistelua. Ne eivät tee jo läpäistyistä paikallisista testeistä keskeneräisiä.

**Sinulla ei ole vielä KnitToolsille mitään Google Play Consolessa.** Siksi tämä tiedosto erottaa kolme vaihetta:

| Vaihe | Mitä siihen kuuluu? | Tarvitaanko julkinen julkaisu? |
| --- | --- | --- |
| Nyt, ilman Play Consolea | Sovelluksen paikalliset toiminnot, kamera, widget, paikallinen kokeiluaika, analytiikan suostumus, erikseen sovitut palvelutestit ja GitHubin automaattiset tarkistukset. | Ei. Palvelutesteillä ja GitHubilla on omat edellytyksensä. |
| Myöhemmin, kun Play Console on valmisteltu | Testiostot, ostotilan palautuminen ja Playn kautta asentaminen/päivittäminen. | Ei. Nämä voidaan tehdä ennen julkista julkaisua. |
| Julkisen julkaisun jälkeen | Oikeiden käyttäjien virheiden, laiteongelmien ja palautteen seuranta. | Kyllä, oikean käyttäjäkunnan havaintojen osalta. Tämä ei kuulu tämänhetkiseen testijonoon. |

Playn sisäinen tai suljettu testijakelu on eri asia kuin sovelluksen julkaiseminen yleisölle. Testiostot tarvitsevat Play-asetukset ja lisenssitestaajan; pelkkä testijakeluun osallistuminen ei tee ostoksista maksuttomia. Lähteet: [Googlen testijakeluohje](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en) ja [Billing-testauksen ohje](https://developer.android.com/google/play/billing/test), tarkistettu 29.10.2026.

### Näin luet tilat

- **Tehty:** nimetty tarkistus on suoritettu ja tulos hyväksytty raportissa. Se ei tarkoita kaikkien mahdollisten tilanteiden testaamista.
- **Vielä tehtävä:** aiempaan suunnitelmaan kirjattu, yksilöity puuttuva tarkistus.
- **Odottaa edellytystä:** testi on nimetty, mutta esimerkiksi sopiva kokeilutila, palvelulupa tai Play-asetus puuttuu.
- **Selvitettävä/päätettävä:** tarvitaan tietojen yhdistämistä tai päätös. Tämä ei ole automaattisesti uusi testi.
- **Rajaus:** raportti kertoo, mitä testi ei kattanut. Rajaus ei yksin muodosta uutta pakollista tehtävää.

## 2. Mitä on jo testattu?

### Koontiin yksilöity allekirjoitettu versio 29.10.2026

Alla olevat kolme kohtaa koskevat 29.10.2026 paketoitua versiota. Sen [rakennusraportti](reports/candidate-20261029-post8134-3e9a271b/REPORT.md) ja [puhelinraportti](reports/startup-smoke-20261029-post8135/REPORT.md) yksilöivät käytetyt tiedostot.

| Tarkistus | Tulos ja käytännön merkitys |
| --- | --- |
| Koodin lint-tarkistukset, myös julkaisuversiolle | **Tehty.** `lc -Full` läpäistiin. Olemassa olevat dokumentoidut poikkeukset säilyivät; tämä ei tarkoita poikkeuksetonta koodia. |
| Allekirjoitetun APK:n ja AAB:n rakentaminen | **Tehty.** Mukana ovat viimeiset kolme tuotantokoodin korjausta. Allekirjoitukset, paketin sisältö ja virheraporttien tulkitsemiseen tarvittava mapping-tiedosto tarkistettiin. |
| Käynnistys ja Asetukset Pixel 9:llä | **Tehty.** Projects ja Asetukset toimivat. Yhden pakotetun pysäytyksen ja uudelleenkäynnistyksen jälkeen havaitut asetukset olivat samat. Analytiikka oli pois päältä. |

Alkuperäinen Avastin estämä yritys on historiallinen merkintä. Käyttäjän poistettua Avastin sama tarkistus jatkettiin onnistuneesti. Varoituksen syytä ei selvitetty, mutta se ei enää estänyt tätä testiä.

**Handoffin alun väitteet puuttuvasta täydestä lint-ajosta ja puuttuvasta uudesta paketista ovat vanhentuneet.** Sen lopun §§8.135–8.136 kirjaavat ne tehdyiksi. Niitä ei pidä tilata uudestaan vanhan yhteenvedon perusteella.

### Aiemmin suoritetut toiminta- ja korjaustestit

Nämä tulokset ovat olemassa ja käyttökelpoisia omassa laajuudessaan. Osa on aiemmasta allekirjoitetusta versiosta, osa erillisestä testisovelluksesta tai tietokoneella ajetuista automaattisista testeistä. Taulukko ei väitä kaikkia toimintoja uudelleen suoritetuiksi uusimmalla APK:lla.

| Alue | Mitä on käytännössä kokeiltu? | Tila ja lähde |
| --- | --- | --- |
| Projektit ja laskuri | Projektin luonti, nimen muutos, avaaminen, plus/miinus, kumoaminen ja lukeman säilyminen uudelleenkäynnistyksessä. | **Tehty aiemmalla allekirjoitetulla versiolla.** Julkaisutarkistussuunnitelma §10.7. |
| Työskentelyjakso ja tilastot | Jakson aloitus, laskurin muutos, sovelluksen pysäytys/uudelleenavaus, jakson lopetus ja tallennetun tuloksen näkyminen tilastoissa. | **Tehty rajattuna käyttötapauksena.** §10.7. |
| PDF, kirjanmerkki ja piirros | PDF:n tuonti, sivun vaihto, kirjanmerkki, piirros ja niiden säilyminen uudelleenavauksessa. | **Tehty aiemmalla allekirjoitetulla versiolla.** §10.7. |
| Useampi projektin PDF ja merkittyjen sivujen vienti | Kaksi PDF:ää samaan projektiin, ensisijaisen poistaminen, jäljelle jäävän avaaminen uudelleenkäynnistyksen jälkeen, merkintä, vienti ja viedyn PDF:n avaaminen. | **Tehty Pixel 9:llä 28.10. version APK:lla.** Handoff §8.115. |
| PDF-viennin virhetilanne | Kohdetiedoston oikea katkaiseminen sekä viennin epäonnistuminen ilman väärää varatallennusta. | **Tehty erillisessä puhelintestissä.** Handoff §8.108. |
| Lanka ja verkko-ohje | Luonti, muokkaus sekä tietojen säilyminen käynnistyksen ja varmuuskopion palautuksen yli. | **Tehty aiemmalla allekirjoitetulla versiolla.** §10.7. |
| Lankakuvan vaihtaminen | Kuvan tarkistuksen, tallennuksen ja epäonnistumisten valittuja tapauksia oikealla Android-kuvankäsittelyllä ja tietokannalla. | **Kolme puhelintestiä läpäisty.** Handoff §8.106. |
| Varmuuskopio | Pienen varmuuskopion vienti ja palautus erillisillä testitiedoilla; alkuperäisten tietojen palautuminen. | **Tehty aiemmalla allekirjoitetulla versiolla.** §10.7. |
| Keskeytynyt palautus | Palautumista eri prosessissa ennen/jälkeen tietokantamuutoksen sekä siivouksen keskeytymistä. Myös toinen palautumiskäynnistys. | **Tehty erillisillä testitiedoilla.** Handoff §§8.98–8.99. |
| Tietokannan päivitys | Projektin PDF-tietojen siirtyminen tietokantaversiosta 21 versioon 22. | **Kaksi rajattua puhelintestiä läpäisty.** Handoff §8.104. Ei kaikkien vanhojen käyttäjätietojen päivitystakuu. |
| Suuri tilastohistoria | 100 001 työskentelyriviä käsiteltiin kerran kukin, ilman tietojen muuttamista. | **Tehty oikealla Room-tietokannalla puhelimessa.** Handoff §8.97. |
| Teema ja kieli | Tumma/vaalea, englanti/suomi ja kielen säilyminen uudelleenkäynnistyksessä. | **Tehty aiemmalla allekirjoitetulla versiolla.** §10.7. |
| Ravelry | Oikea kirjautuminen, sivuhistorian palautuminen, Takaisin, PDF:n lataus, tallennus Kirjastoon, avaus/uudelleenavaus ja toisen tuonnin peruutus. | **Tehty Pixel 9:llä allekirjoitetulla versiolla.** §10.9. Tätä ei avata uudelleen pelkän päivämäärän takia. |
| Ilmainen käyttö ja kokeilun aloitus | Ilmaisversion rajoitus ja tavallisesta käyttöliittymästä aloitettu kokeilu. Kokeilun päättymisilmoituksen palautumista on testattu keinotekoisella testitilalla. | **Nimetyt tapaukset tehty.** Aito 14 päivän päättyminen ja Play-ostot ovat erikseen avoinna. Handoff §8.12. |
| Kamera ja widget | Molempien kuvauspolkujen tallennus/peruutus emulaattorilla, aiempia puhelimen kameran palautumistestejä, widgetistä avaaminen ja laskurin näyttö. Erillisessä puhelimen widget-testissä painettiin plussaa. | **Osittain tehty.** Oikean kotinäytön miinus sekä nykyisen allekirjoitetun version puhelinkameran puuttuvat tapaukset ovat luvussa 3. |
| Analytiikan suostumus ja jono | Suostumuksen perumisen jälkeen vanhat jonossa olevat tapahtumat eivät lähteneet; uudelleen sallimisen jälkeen lähti vain uusi tapahtuma paikalliseen testivastaanottimeen. Myös tallennus- ja virhetilanteita. | **Tehty tietokoneella ja rajatusti oikealla Android-laitteella.** Handoff §§8.123 ja 8.125. Ei oikean PostHog-palvelun toimitustesti. |
| Viimeisten korjausten regressiot | Analytiikan alustamisen epäonnistumisen siivous, kuvan lukemisen keskeytys sekä virheellisen piirtoeleen jälkeen kelvollisen eleen tallennus. | **Kohdennetut automaattitestit tehty.** Handoff §8.134 kirjaa 50 testiä ja myöhemmän kuuden testin uusinnan; kuusi sisältyy aiempaan joukkoon. |
| Käyttöliittymän sovittuminen ja saavutettavuus | Sovittuvat näkymät, pienet leveydet, suuri fontti, niiden yhdistelmä ja TalkBack aiemmissa kampanjoissa. | **Tehty kirjatuissa laajuuksissa.** Ei uusintaa ilman nimenomaista pyyntöä. |
| Koodikatselmointi ja automaattiset tarkistukset | Laaja CodeRabbit-katselmointi, vahvistettujen havaintojen korjaukset ja kohdennetut jatkotarkistukset. Kampanjassa raportoitiin 2 331 Androidin JVM-testin läpäisy. | **Tehty raportoiduille versioille.** Handoff §§8.101, 8.131 ja 8.134–8.135. Katselmointi ei ole puhelimen käyttötapaus eikä testimäärä tarkoita täydellistä haarakattavuutta. |

Päälähteet: [Androidin julkaisutarkistussuunnitelma](ANDROID_RELEASE_READINESS_PLAN.md), [handoff](KNITTOOLS_REMAINING_WORK_HANDOFF.md) ja [jäljellä olevien töiden koonti](reports/rw-11f-20261028/REPORT.md). Handoffin myöhemmät päivätyt tulokset täydentävät vanhempaa koontia.

## 3. Vielä tarvittavat paikalliset puhelintarkistukset

Nämä eivät tarvitse Google Play Consolea tai julkista julkaisua. Käytetään tunnistettua allekirjoitettua versiota ja vain tehtävää varten tehtyjä testitietoja. Henkilökohtaisia projekteja ei poisteta tai korvata. Jos toiminto tarvitsee Pro-oikeuden, testin todellinen käyttöoikeustila selvitetään ensin; debug-version vapaa pääsy ei todista julkaisuversiota.

### P1. Kamera: tallennus ja peruutus — vielä tehtävä

**Miksi:** emulaattorin kameratestit ja vanhemman testisovelluksen puhelintulokset eivät vielä kata nykyisen allekirjoitetun version tavallista kameraketjua.

1. Testiprojektissa avataan edistymiskuvan kamera ja peruutetaan. Kuvaa ei saa ilmestyä projektiin.
2. Otetaan ja hyväksytään yksi kuva. Sen pitää näkyä oikeassa projektissa ja avautua oikein päin.
3. Ohjeen kuvaamisessa peruutetaan kerran ja hyväksytään kerran.
4. Hyväksytystä kuvasta luodaan PDF sovelluksen normaalilla toiminnolla ja avataan PDF uudelleen.

**Valmis, kun:** tallennus tuo yhden oikeaan projektiin kuuluvan kuvan/PDF:n, peruutus ei tuo liitettä ja aiemmat dokumentit säilyvät. Kirjataan käytetty puhelin, kameran käsittelijä ja sovellusversio. Muita puhelinmerkkejä ei tarvitse hankkia tämän kohdan sulkemiseksi. Lähde: handoff §8.14, CW1.

### P2. Kotinäytön widget: oikeat painikkeet — vielä tehtävä

**Miksi:** tietokannan suora vähennys ei ole sama asia kuin kotinäytön widgetin miinuksen painaminen.

1. Widget liitetään testiprojektiin ja sen lähtölukema kirjataan.
2. Sovelluksen ollessa suljettuna painetaan widgetin plussaa ja miinusta.
3. Widgetistä avataan sovellus. Tarkistetaan oikea projekti ja tallentunut lukema.
4. Lukemaa muutetaan sovelluksessa ja tarkistetaan widgetin päivittyminen.

**Valmis, kun:** yksi painallus aiheuttaa yhden asetetun laskuriaskeleen, näyttö ja tallennettu lukema vastaavat toisiaan ja widget avaa oikean projektin. Mahdollinen puuttuva käyttöoikeus kirjataan esteeksi, ei widget-viaksi. Kahden widgetin, poistetun projektin tai uudelleenkäynnistyksen yhdistelmiä ei lisätä automaattisesti. Lähde: handoff §8.14, CW2.

### P3. Värinäasetus oikeassa kädessä — vielä tehtävä

**Miksi:** automaattitesti voi tarkistaa värinäkutsun, mutta ei sitä, tuntuuko värinä puhelimessa.

1. Kirjataan alkuperäinen värinäasetus.
2. Värinä päällä kokeillaan laskurin painallusta ja komponentin pitkää painallusta, joka normaalisti antaa värinäpalautteen.
3. Samat toiminnot kokeillaan värinä pois päältä.
4. Palautetaan alkuperäinen asetus.

**Valmis, kun:** käyttäjä vahvistaa palautteen tuntuvan päällä ja puuttuvan pois päältä. Puhelimen omien asetusten estämä värinä erotetaan sovelluksen virheestä. Viimeisin käynnistystesti vain luki asetuksen; se ei tehnyt tätä koetta. Lähde: handoff §8.65, ST3.

### P4. Oman 14 päivän kokeilun päättyminen — odottaa sopivaa tilaa

**Miksi:** kokeilun logiikkaa ja ilmoituksen palautumista on testattu, mutta aidosti kuluneen 14 päivän päättymisestä ei ole dokumentoitua hyväksyttyä tulosta.

Testi tehdään asennuksella, jonka kokeilu on aloitettu normaalisti ja jonka aika on todella kulunut loppuun. Nykyisen puhelimen soveltuvuutta ei ole tässä tarkistettu.

1. Varmistetaan tavallinen kokeilun aloitus, kulunut aika ja käyttöoikeuden latautuminen.
2. Avataan sovellus ja tarkistetaan kokeilun päättymisilmoitus.
3. Tarkistetaan ilmoituksen säilyminen sovitussa näkymän uudelleenluonnissa tai tallennetusta tehtävästä paluussa sekä sen kuittaaminen.
4. Tarkistetaan, että vanha sisältö säilyy käytettävänä sovelluksen sääntöjen mukaan ja edustava uusi Pro-toiminto estyy.
5. Tarkistetaan ilmoituksen Jatka ilmaiseksi / Näytä Pro -toimintojen käsittely. Ostosta ei tarvita.

**Valmis, kun:** luonnollinen päättyminen, ilmoitus/kuittaus ja vanhan sisällön sekä uusien toimintojen raja on havaittu. Jos sopivaa tilaa ei ole, kohta jää odottamaan; puhelimen kelloa tai yksityisiä kokeilutietoja ei muuteta. Keinotekoinen testi ei muutu aidoksi 14 päivän testiksi. Tämä kokeilu ei ole Play-tilaus. Lähde: handoff §8.12, TE2.

## 4. Muut avoimet asiat, jotka eivät tarvitse Play Consolea

Näitä ei pidä sekoittaa käsin tehtäviin puhelintesteihin. Ne voidaan valmistella ennen julkaisua, mutta kaikilla ei ole nyt suoritusvalmiita edellytyksiä.

| Tunnus ja tila | Mitä täsmälleen pitää selvittää tai tarkistaa? | Milloin valmis / mitä odottaa? |
| --- | --- | --- |
| T1 — **selvitettävä:** analytiikan taustasäikeet | PostHogin sulkeminen voi jättää joutilaaksi jääviä SDK-säikeitä prosessin loppuun asti. Toistuva pois/päälle vaihtaminen voi lisätä niitä, vaikka tapahtumien lähetys on estetty. Arvioidaan rajatusti vaikutus ja tarvitaanko korjaus. | Kirjattu arvio ja tarvittaessa kohdennettu korjaus/testi, tai käyttäjän nimenomainen päätös hyväksyä rajattu haitta. Tätä ei ole vielä hyväksytty eikä korjattu. Ei automaattista pitkää kuormitustestausta. Handoff §§8.123, 8.125, 8.131. |
| T2 — **selvitettävä:** yksi puuttuva käynnistysmittaus | Aiemmista 40 kierroksesta saatiin 39 käynnistymisaikojen mittariparia. Selvitetään puuttuvan näytteen syy; tarvittaessa täydennetään vain kyseinen vertailu. | Alkuperäisen mittausprotokollan puuttuva kohta on täydennetty tai poikkeama on erikseen päätetty hyväksyä. Tulosta ei keksitä eikä kaikkia profiileja luoda uudelleen. Mittaus tehtiin erillisellä optimoidulla testiversiolla. Handoff §8.93; koonnin U3. |
| T3 — **vielä koottava:** mitä nykyisestä koodista jo katetaan | Yhdistetään olemassa olevat testitulokset ja kattavuustiedot: mitä testit oikeasti tarkistavat, mitä aiempia tuloksia myöhemmät muutokset koskevat ja mitkä nimetyt haarat ovat vielä ilman näyttöä. Mukaan aiemmat 13 julkaisuversiolle määriteltyä käyttötapausta. | Puuttuva yhteinen koonti on tehty. Mahdollinen lisätesti perustellaan täsmällisellä vaatimuksella ja puuttuvalla tapauksella. Koontia ei täytetä käynnistämällä koko testijoukkoa alusta. Koonnin L1. |
| T4 — **odottaa palvelutestin rajausta:** analytiikan oikea toimitus | Oikean julkaisuasetuksilla toimivan sovelluksen suostumusketju ja toimitus tarkoitettuun PostHog-projektiin: pois päältä ei käytön tapahtumia, päällä vain sallittuja tapahtumia, perumisen jälkeen ei uusia lähetyksiä eikä vanhan jonon paluuta uudelleen sallittaessa. Jo liikkeelle lähtenyt pyyntö voi valmistua. | Sovelluksen asetus ja oikea vastaanottaja on yhdistetty havaintoon. Nykyinen paikallisen jonon testisarja säilytetään; sitä ei uusita kokonaan. Tarvitsee luvan tarkoituksellisiin testitapahtumiin ja rajattuun palvelutarkistukseen, ei Playta. Koonnin E1 ja handoff §§8.123–8.131. |
| T5 — **odottaa palvelutestin rajausta:** virheraportointi | Tarkistetaan, että hallittu testivirhe saapuu oikeaan Crashlytics-projektiin ja että oikean version mapping-tiedostolla virheraportin nimet saadaan luettaviksi. | Toimitus ja tulkittava raportti on havaittu. Paketin ja mappingin vastaavuus on jo tarkistettu. Tarvitsee rajatun palvelulähetyksen ja mappingin latauksen; testikaadinta ei jätetä jaettavaan sovellukseen. Koonnin E1. |
| T6 — **odottaa palveluasetusten tarkistusta:** palvelujen kohteet ja rajoitukset | Tarkistetaan sovelluksen käyttämien Firebase-/PostHog-asetusten oikea kohde ja dokumentoidut pilvipuolen käyttörajoitukset. Julkisen asiakastunnisteen luokittelu ei todista näitä asetuksia. | Rajattu asetustarkistus on kirjattu. Ei yleistä pilviauditointia, tunnisteiden tulostusta tai asetusten muuttamista tästä listasta. Koonnin E1/X2. |
| T7 — **odottaa GitHubiin toimitettua versiota:** CI | Tarkistetaan oikea GitHub/Linux-ajo sille koodiversiolle, joka on tarkoitus julkaista. Paikallinen ajo ei yksin todista GitHubin ympäristöä. | Sovitut CI-tarkistukset läpäisevät tunnistetulla versiolla. Paikallisia tallentamattomia/viemättömiä muutoksia ei voi todistaa etäajolla. Commit ja push tarvitsevat erillisen pyynnön; käyttäjä luo PR:n. Koonnin E3. |

T4–T6 ovat mahdollisia ennen Playta ja julkista julkaisua. Niitä ei siirretä automaattisesti julkaisun jälkeisiksi vain siksi, että ne käyttävät verkkopalvelua. Tämä tiedosto ei kuitenkaan käynnistä palvelutoimia.

## 5. Muut dokumentoidut rajaukset: mahdollisia testejä, eivät automaattinen lisäjono

Vanhoissa raporteissa on paljon lauseita kuten ”ei todista kaikkia virhepolkuja”. Alla ovat niiden paikallisesti testattavat pääryhmät. Niitä ei merkitä läpäistyiksi, mutta myöskään jokaista yhdistelmää ei vaadita ajettavaksi. T3:n koonnissa erotetaan todellinen avoin vaatimus pelkästä rajauksesta.

| Ryhmä | Mitä vielä voisi tarkistaa, jos täsmällinen vaatimus jää kattamatta? | Milloin siitä tulee tehtävä? |
| --- | --- | --- |
| Tallennuksen virheet ja keskeytykset | Täysi levy, tiedostopalvelun virhe, tallennuksen peruutus tai tarkasti valittu prosessin päättymishetki. | Kun nimetään puuttuva tietojen säilymistä koskeva vaatimus ja sen kohta tallennusketjussa. Jo tehtyjä varmuuskopion palautumistestejä ei nollata. |
| Näyttöjen yhteistoiminta | Muistutuksen muokkaus, vanhentunut avoin lomake, projektin vaihtuminen kesken toiminnon, palaaminen Pro-kehotteesta tai odottava komento. | Kun olemassa olevista tuloksista puuttuu juuri kyseisen toiminnon näyttö. Kaikkien näkymien kaikkia yhdistelmiä ei tilata kerralla. |
| Asetusten vaikutukset ja paluu | Näytön päällä pitäminen vain oikeassa näkymässä, järjestelmäkielen/-teeman seuraaminen, yksiköt, Ohje-linkin avaus ja paluu. | Vain valittu vielä todistamaton asetus/siirtymä. Viimeisin asetusten säilymistesti ei kattanut kaikkien asetusten muuttamista. |
| Prosessin päättyminen ja uudelleenkäynnistys | Tallennetusta tehtävästä paluu, kamerasta paluu prosessin vaihduttua tai työskentelyjakson palautuminen puhelimen käynnistyessä. | Ensin erotetaan pakotettu pysäytys, Androidin lopettama taustaprosessi ja puhelimen uudelleenkäynnistys. Testataan vain vaatimus, jota aiempi näyttö ei kata. |
| Suuret aineistot ja pitkä käyttö | Suuret PDF:t/kuvat/varmuuskopiot, samanaikainen tietokannan luku ja kirjoitus, muisti, akku ja pitkään jatkuva käyttö. | Tarvitaan sovittu aineisto, kesto ja hyväksymisraja. ”Testaa kunnes varmasti toimii” ei ole testi. Aiempi 100 001 rivin tulos säilyy. |
| Muut Android-versiot ja tiedostopalvelut | Nimetty puuttuva yhteensopivuustapaus, esimerkiksi toisen tiedostopalvelun PDF-käsittely. | Kun puuttuva vaatimus ja soveltuva laite/palvelu on yksilöity. Aiempi API 29 -tulos säilyy; emulaattoreita tai uusia laitehankintoja ei lisätä automaattisesti. |

Lähde: koonnin U4 sekä handoffin toimintokohtaiset rajaukset. Tämä taulukko ei hyväksy avoimia vaatimuksia pois. Se estää muuttamasta jokaista kuviteltavissa olevaa tilannetta uudeksi pakolliseksi testiksi.

Vanhan käynnistystestin yhteydessä havaittu prosessin selittämätön palaaminen on edelleen luokittelematon havainto. Uusimman testin loppusiivouksessa prosessi pysyi pysäytettynä. Näistä ei voi päätellä SDK-säikeiden vikaa tai sen korjaantumista; havainto ei yksin edellytä uutta yleistä käynnistyskampanjaa.

## 6. Päätöksiä ja julkaisuvalmistelua, ei sovellustestejä

| Asia | Mitä puuttuu? |
| --- | --- |
| Tiedostosiivouksen ohje | Sovitaan ohjeen sanamuoto suhteessa toteutuksen turvalliseen tietokantatapahtumaan. Tämä ei itsessään vaadi uutta siivoustoimintoa. Koonnin D1. |
| Lisälaskurin pitkä teksti | Päätetään, kuuluuko teksti rivittää vai katkaista. Vanha testiodotus ja nykyinen käyttöliittymä ovat ristiriidassa. Sen jälkeen korjataan vain väärä odotus tai sovittu toiminta. D2. |
| Vanhan Ravelry-taustapalvelun odotusaika | Säilytetyn taustapalvelun 24 sekunnin virhepaluu odottaa mahdollista tuoteratkaisua. Android ei enää käytä tätä API/OAuth-polkua; siitä ei tehdä Android-julkaisun uutta testikampanjaa. D3. |
| Ravelry-lupa | Dokumenteissa mainitun kirjallisen luvan todellinen tilanne pitää vahvistaa julkaisuvalmistelussa. Tässä ei päätellä luvan nykytilaa eikä lähetetä viestejä. E4. |
| Kauppa- ja tietosuojatiedot | Kuvauksien ja ilmoitusten pitää vastata lopullisen sovelluksen todellista tietojen käyttöä. Paikallinen testitulos ei korvaa tätä valmistelua. E4. |

## 7. Kun Play Console on valmisteltu — ennen julkista julkaisua

**Nämä ovat nyt odottamassa Consolea. Niitä ei ole merkitty tehdyiksi eikä niitä yritetä korvata debug-version Pro-ohituksella.** Julkista julkaisua ei tarvitse tehdä ensin, jotta nämä voidaan testata.

KnitToolsin Pro on kertaostos. Sovelluksen oma 14 päivän kokeilu on luvun 3 asia; tilauksen nopeutettua uusimista ei lisätä tähän.

| Tarkistus | Odotettu tulos |
| --- | --- |
| Onnistunut maksuton lisenssitestaajan testiosto | Pro avautuu oikean ostotuloksen perusteella ja sovellus kuittaa oston. |
| Oston peruutus ja hylätty maksu | Pro ei avaudu eikä käyttäjälle ilmoiteta onnistuneesta ostosta. |
| Odottava ostos, jos käytetty testimaksutapa tuottaa sen | Keskeneräistä maksua ei käsitellä valmiina ostona; myöhempi lopputulos käsitellään oikein. |
| Ostetun Pron säilyminen ja palautuminen | Sovelluksen uudelleenkäynnistys ja tuettu palautus tunnistavat ostetun oikeuden. Uudelleenasennustesti vaatii erillisen, tietoja suojaavan menettelyn. |
| Testioston hyvitys/oikeuden poistuminen | Päivittynyt ostotila poistaa Pro-oikeuden sovitulla tavalla; käyttäjän olemassa oleva sisältö säilyy sääntöjen mukaan. |
| Playn kautta asennus ja päivitys | Playn toimittama versio käynnistyy ja päivitys säilyttää testitiedot. Paikallisesti asennetun APK:n testi ei yksin todista Play-jakelua tai sen allekirjoitusta. |

Lähteet: handoff RW-09/RW-10, koonnin E2 sekä [Googlen Billing-testausohje](https://developer.android.com/google/play/billing/test). Tämän vaiheen tarkka tili-, tuote- ja testijakeluasetus tarkistetaan silloin, kun käyttäjä haluaa aloittaa Play-valmistelun.

## 8. Mitä ei tarvitse aloittaa uudelleen?

- Täysi lint, viimeisin paketointi ja 29.10. käynnistys/Asetukset-testi ovat tehtyjä.
- Valmiita CodeRabbit- ja muita laajoja tarkistuskampanjoita ei aloiteta alusta ilman perusteltua muutosta.
- 320 dp, 200 prosentin fontti, niiden yhdistelmä, TalkBack ja valmis kielikampanja eivät kuulu automaattiseen uusintaan.
- Valmista Ravelryn kirjautumis-/PDF-käyttökoetta ei uusita vain vanhan päivämäärän vuoksi.
- Allekirjoitusavaimia ja niiden varmuuskopiointia ei tehdä uudelleen tätä listaa varten.
- Poistettujen ääni-, OAuth/API- tai pilvimuistiinpano-ominaisuuksien testausta ei palauteta jonoon.
- Toisen sovelluksen asiat eivät kuulu KnitToolsin valmiuteen.

## 9. Milloin tämä kierros loppuu?

Tämän dokumentoidun paikallisen kierroksen seurattavat kohdat ovat **P1–P4 ja T1–T7**. Niissä erotetaan suoritettavat testit, puuttuvat edellytykset ja selvitettävät asiat. Luvun 5 rajauksista ei synny lisätehtäviä ilman nimettyä avointa vaatimusta. Luvun 6 päätökset ja luvun 7 Play-valmistelu pidetään erillään paikallisista testituloksista.

Kohdan lopussa kirjataan lyhyesti:

1. Mitä tehtiin ja millä sovellusversiolla/laitteella tai palvelulla.
2. Mikä oli odotettu tulos ja mitä oikeasti havaittiin.
3. Onko kohta valmis, löytyikö korjattava vika vai mikä täsmällinen edellytys puuttuu.
4. Linkki tulokseen. Muita kohtia ei avata automaattisesti sen perään.

Jos koodi muuttuu, tarkistetaan muutoksen vaikutus aikaisempaan näyttöön. Vanha testi pysyy vanhan version testinä, mutta pelkkä uusi APK tai raportti ei tee kaikkien testien uusimisesta tarpeellista.

**”Tämä testi ei todista kokonaisjulkaisuvalmiutta” on tuloksen rajaus, ei käsky jatkaa testaamista loputtomasti.** Paikalliset testit voidaan saada valmiiksi, vaikka Play-vaihe olisi vielä tekemättä. Avoin vaatimus suljetaan testituloksella tai nimenomaisella soveltuvuus-/hyväksymispäätöksellä, ei unohtamalla sitä eikä keksimällä uusia vaatimuksia tilalle.

## 10. Testattu versio, ympäristö, tulos ja rajaus

### 10.1 Versioiden tunnistaminen

Alla olevien allekirjoitettujen pakettien versionName/versionCode ovat kaikki **1.0.0 / 1**, joten pelkkä versionumero ei erota niitä. Tarkat APK-tiivisteet ovat alkuperäisraporteista; paketteja ei rakennettu tai hashattu uudelleen tässä dokumentointityössä. Historiallinen HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f` ei yksin yksilöi buildia, koska työpuussa oli tallentamattomia muutoksia. Säilytetään myös raportin lähdeinventaario ja tarvittaessa sitä vastaava lähdearkisto.

| Tunnus | Paketti ja alkuperäinen rakennusraportti | APK:n SHA-256 |
| --- | --- | --- |
| S23 | [23.10. allekirjoitettu ehdokas](reports/candidate-20261023/REPORT.md) | `91E312882F7DACDEAA63B8A994986EEA988AEC696D5F68D519C70DD360C33803` |
| S28D | [28.10. RW-11D](reports/candidate-20261028-rw11d-cbb80189/REPORT.md) | `23B079C44B5D98002EE1262F97D8C3397D860C0BACA2D01DAF9D5BD10729C27C` |
| S28G | [28.10. RW-11G, suostumuskorjauksen jälkeen](reports/candidate-20261028-rw11g-da03837e/REPORT.md) | `BAEF789D20CAAC40E19881B73F77C2554C33E45B720705CD8B5B90A964CC3F4A` |
| S29 | [29.10. §8.134:n kolmen korjauksen jälkeen](reports/candidate-20261029-post8134-3e9a271b/REPORT.md) | `65D971AD369C313FE4971D2C0F7465FE2BCEE6108D56184C08347D2830D49797` |

Eristetty `activepolicytest`-sovellus, optimoitu `optimizedtest` ja allekirjoitettu tuotantosovellus ovat eri testikohteita. Eristetyn testin tulosta ei siirretä S23–S29-paketeille. Seuraavan taulukon lyhyet hash-alkuosat ovat lukemisen apuna; täydet app/test APK- ja lähdetiivisteet säilyvät linkitetyissä raporteissa ja manifesteissa.

### 10.2 Säilytettävä testirekisteri

| Testi / ajankohta | Versio ja ympäristö | Dokumentoitu tulos | Rajaus ja avoin asia |
| --- | --- | --- | --- |
| Paikalliset käyttötapaukset 23.10. | S23; Android 17/API 37- ja Android 10/API 29 -emulaattorit; normaali 360 × 800 dp, fontti 1.0; verkko estetty. [Raportti](reports/candidate-runtime-20261023/REPORT.md), julkaisutarkistussuunnitelma §10.7. | Seitsemällä eri API 37 -käyttötapauksella hyväksytty suoritus sekä API 29:n 1/1 käynnistys/navigointitesti. Ensimmäinen API 37 -ajo oli 5 hyväksyttyä / 2 epäonnistunutta; rajattu uusinta 2/2 hyväksyttyä. | Driverin URI-oikeus ja widget-valitsin korjattiin. Alkuperäinen epäonnistunut ajo säilyy epäonnistuneena. Ei live-palveluja, aitoa kokeilun päättymistä tai puhelimen suorituskykyhyväksyntää. |
| Ravelryn kirjautuminen ja PDF 23.10. | S23; Pixel 9, Android 17/API 37, WebView 155.0.8059.43. [Raportti](reports/r10-live-20261023/REPORT.md). | R10:n nimetyt live-vaatimukset läpäisty: kirjautuminen, Activity-/tehtäväpalautuminen, historia/Takaisin, PDF:n tallennus ja uudelleenavaus, toisen tuonnin peruutus. | Yksi aiempi URL-fallback ei ollut historiatestin PASS. Yksityisten tiedostojen fyysistä poistumista ei voitu tarkastaa. Ei R11-/osto-/palvelutoimitustestiä. |
| Kylmäkäynnistyksen mittaus RW-06A 27.10. | `optimizedtest`, debug-allekirjoitettu optimoitu APK `9504F711…`; Pixel 9/API 37. [Mittauskooste](reports/rw-06a-20261027/measurement-summary.json), handoff §8.93. | Neljä benchmark-metodia läpäisi; 40 jälkeä mutta **39/40 TTID/TTFD-mittariparia**. | Ei tuotantoallekirjoitettu APK. Empty/Require-kierroksen 006 mittarit puuttuvat. T2 pysyy avoimena; säilytetään alkuperäiset mittaustiedot ja jäljet. |
| Suuri Insights-historia RW-03D 27.10. | `activepolicytest` app `72E16304…`, test APK `9DBAC19F…`; Pixel 9/API 37, oikea Room. [Ajoloki](reports/rw-03d-20261027/instrumentation.log), [paketit](reports/rw-03d-20261027/tested-apks.json), handoff §8.97. | 1/1 testi; 100 001 riviä täsmälleen kerran, sisältö säilyi. | Yksi synteettinen jakauma ja All Time -valinta; ei suorituskykyrajaa, samanaikaisen kirjoittajan vasteaikaa tai tuotanto-APK:n näyttöä. |
| Palautuksen keskeytyminen RW-03F/G 27.10. | Sama `72E16304…` app; F:n test APK `4BF4CA0A…`, G:n `93A25069…`; Pixel 9/API 37. [F ennen commitia](reports/rw-03f-20261027/precommit-result.json), [F commitin jälkeen](reports/rw-03f-20261027/postcommit-result.json), [G:n kooste](reports/rw-03g-20261027/verification-summary.json); handoff §§8.98–8.99. | F: 4/4 eri prosessissa tehtyä varmennusta; G: 1/1 osittaisen jälkisiivouksen jälkeen. | Tahallisesti tapetut seed-ajot ovat keskeytyksiä, eivät PASS-testejä. Vain nimetyt katkaisukohdat ja synteettiset tiedot, ei henkilökohtaisten tietojen palautuskoetta. |
| RW-CR-01, valmistui 28.10. | Kampanjan dirty-lähdeversio; paikallinen JVM/Gradle, ei laiteajo. [Kampanjaraportti](reports/coderabbit-audit/2026-10-27-rw-cr-01-e4416c7cf0a7/campaign-report.md), [lopputulos](reports/coderabbit-audit/2026-10-27-rw-cr-01-e4416c7cf0a7/android-integrated-final-result.json). | 2 331 JVM-testiä, 0 failures/errors/skipped; Android-testilähteet kääntyivät. | Instrumentointia ei suoritettu tässä ajossa. Aiemmat keskeytykset ja epäonnistumiset säilyvät erillisinä; testimäärä ei todista kaikkia haaroja tai nykyistä työpuuta. |
| Room 21→22 RW-03H 28.10. | `activepolicytest` app `49F05657…`, test APK `B64D2BD5…`; Pixel 9/API 37. [Raportti](reports/rw-03h-20261028/REPORT.md), [tulos](reports/rw-03h-20261028/instrumentation-status.json). | 2/2 testiä; URI-otsikot, Unicode ja nimetyt omistajuus-/säilymisinvariantit. | Synteettinen migraatio oikeassa Roomissa; ei koko migraatioketjua, live-SAF:ää tai oikean käyttäjän tietokannan päivitystä. |
| Lankakuvan vaihto RW-03I 28.10. | Sama `49F05657…` app, test APK `FF572378…`; Pixel 9/API 37. [Raportti](reports/rw-03i-20261028/REPORT.md), [tulos](reports/rw-03i-20261028/instrumentation-status.json). | 3/3: tyhjän ja virheellisen kuvan hylkäys säilyttää vanhan; kelvollinen JPEG vaihtuu ja vanha poistuu. | Synteettiset paikalliskuvat ja oikea Android-dekoodaus; ei kaikkia kuvamuotoja, pilvipalveluntarjoajia tai palautumistilanteita. |
| PDF-viennin katkaisu RW-03J 28.10. | Sama `49F05657…` app, test APK `42559105…`; Pixel 9/API 37. [Raportti](reports/rw-03j-20261028/REPORT.md), [tulos](reports/rw-03j-20261028/results.json). | 2/2: lyhyempi vienti katkaisee kohteen; `wt`-hylkäys säilyttää kohteen ilman `w`-varapolkua. | Oikea renderer ja tiedostot, mutta prosessin sisäinen testiprovider; ei ulkoisen providerin/pickerin yhteistoiminnan todiste. |
| Usean PDF:n käyttö RW-11E 28.10. | S28D; Pixel 9/API 37, oikea SAF. [Raportti](reports/rw-11e-20261028/REPORT.md). | Kahden PDF:n tuonti, ensisijaisen poisto, jäljelle jääneen uudelleenavaus stop/startin jälkeen, piirros ja viedyn PDF:n avaus. | Nimetty dokumentin poistopolku; ei erillinen detach, OS-prosessikuolema, rollback tai fyysisten orpojen siivouksen varmennus. |
| Suostumuksen tiukennus RW-07B 28.10. | Paikallinen JVM; PostHog Android 3.72.1/core 6.47.1; lähdetiivisteet jatkoraportissa. [Jatkoraportti](reports/rw-07b-20261028/CONTINUATION-20261028.md). | 16/16 testiä. Vanha jono ei palaa uuden suostumuksen yhteydessä; kontrolloitu vastaanotin, ei DNS-/palvelulähetystä. | Alkuperäinen REPORT.md on aiemman esteen raportti. Jo lähtenyttä pyyntöä ei voi perua takautuvasti; SDK-työntekijöiden elinkaari ja T1/T4 ovat erillisiä. |
| Android-suostumus RW-07C 28.10. | `activepolicytest` app `243D38E1…`, test APK `D9A73CB4…`; Pixel 9/API 37, oikea DataStore/SDK/jono. [Raportti](reports/rw-07c-20261028/REPORT.md). | 1/1 rajattu integraatiotesti; uudelleen sallimisen jälkeen vain uusi tapahtuma kontrolloituun vastaanottimeen. | Ei tuotanto-Appin kaikkia polkuja, oikeaa PostHog-toimitusta, ajastimia tai SDK-säikeiden tuotantoelinkaarta. |
| Käynnistys/Asetukset RW-11H 28.10. | S28G; Pixel 9/API 37, analytiikka pois. [Raportti](reports/rw-11h-20261028/REPORT.md). | Rajattu käyttöketju ja yksi stop/start läpäisty; havaitut asetukset säilyivät. | Loppusiivouksessa prosessi palasi selittämättä. Syytä ei vahvistettu; havainto ei ole todiste SDK-viasta tai sen korjauksesta. |
| Korjausten regressiot ja täysi lint 28.–29.10. | S29:n lähdeaineisto; Gradle 9.8.1 / Temurin 17.0.20. [S29-raportti](reports/candidate-20261029-post8134-3e9a271b/REPORT.md), handoff §8.134. | 50 kohdennettua JVM-testiä ja myöhempi niihin sisältyvä 6/6 uusinta; käyttäjän `lc -Full` läpäisty, debug- ja release-lint. | Ei 56 eri testiä. 22 olemassa olevaa Detekt-baselinen merkintää. Osan 50 testin näyttö on handoffin tallennettu ajotulos, ei enää erillinen JUnit-kuitti. |
| Paketointi 29.10. | S29; Gradle 9.8.1 / Temurin 17.0.20, offline Windows-build. [Raportti](reports/candidate-20261029-post8134-3e9a271b/REPORT.md). | APK/AAB, allekirjoitus, pakettisisältö, mapping-yhteys ja olemassa olevien profiilien yhteensopivuus hyväksytty. | Staattinen paketointinäyttö, ei ajonaikainen kolmen korjatun virhepolun testi, Play-hyväksyntä tai palvelutoimitus. |
| Käynnistys/Asetukset 29.10. | S29; Pixel 9, Android 17/API 37. [Raportti](reports/startup-smoke-20261029-post8135/REPORT.md). | Avastin estämä ensimmäinen yritys; käyttäjän poistettua Avastin jatko läpäisi Projects/Asetukset-ketjun ja yhden stop/startin. Kuusi havaittua asetusarvoa säilyi. | Varoituksen syy tuntematon. Ei SDK-elinkaaren, OS-prosessikuoleman, suorituskyvyn tai palvelujen hyväksyntää. |
| Onboarding-vaiheet 1–10, valmis 30.10. | `codex/onboarding-20261029`, muuttunut työpuu; paikalliset JVM/Gradle-ajot ja eristetyt activepolicy-emulaattoritestit. [Edistymisraportti](reports/onboarding-implementation/PROGRESS.md). | Raportin mukaan jokaisessa vaiheessa test/Detekt/lint hyväksytty. Lopussa **2 509/2 509 JVM-testiä**; vaiheen 10 testi-/Detekt- ja lint-lokien BUILD SUCCESSFUL tarkistettu tässä koonnissa. Vaiheiden 2–9 kohdennetut runtime-tulokset on eritelty alkuperäisraportissa. | 2 509 on raportista, ei tässä uudelleen laskettu JUnit-XML:istä. Lopullinen lint-loki osoittaa debug-lintin, ei uutta release-lintiä. Vaihe 11 pois rajauksesta. Tarkkaa yhteistä APK-hashia/emulaattori-image-identiteettiä ei sidottu tähän koontiin; ei S29:n testitulos eikä nykyisen korjaustyön hyväksyntä. |

Onboardingin vaiheessa 9 ensimmäinen laiteajo oli **36 testiä / 9 epäonnistumista**. Alkuperäinen loki vahvistaa tämän. Activityn uudelleenluonnin splash-tilaa ja testin Compose-solmun hakua korjattiin; loppuajossa `TrialNoticeRestorationTest` läpäisi **14/14**. Kampanja raportoi kaikki 36 valittua tapausta hyväksytyiksi usean lopullisen ajon perusteella, ei yhtenä 36/36-uusinta-ajona. Määrät ovat päällekkäisiä eikä niitä lasketa uusiksi erillisiksi testeiksi.

### 10.3 Mitä tämä täydennys ei sulje

- P1–P4, T1–T7 sekä luvun 6 päätökset säilyvät viimeisimmän luetun tilaraporttinsa mukaisina. Uusi onboarding-kampanja ei yksin sulje kameraa, oikean widgetin miinusta, käsin tunnettavaa värinää, luonnollista 14 päivän päättymistä, palvelutoimitusta tai Play-jakelua.
- T3:n koko nykyisen koodin kattavuus-/vaatimusvertailu on edelleen erillinen avoin asia. Tämä rekisteri yksilöi säilytettävän näytön; se ei ole uusi koko sovelluksen auditointi.
- T2:n puuttuva mittaripari ja RW-11H:n prosessihavainto säilyvät alkuperäisellä epävarmuudellaan. Onboardingin korjattu splash-havainto on eri ilmiö.
- Säilytetyt alkuperäisraportit voivat sisältää myöhemmin vanhentuneita esteitä. Päivätty jatkoraportti ratkaisee vain oman esteensä; vanhaa FAIL/BLOCKED-tulosta ei kirjoiteta jälkikäteen PASSiksi.
- Jos jo siivotuissa build-kansioissa oli ainoa alkuperäinen JUnit-/HTML-tulos, sitä ei palauteta tällä tekstillä. Jäljellä oleva loki tai koonti merkitään sen tasoiseksi näytöksi. Puutetta ei paikata keksityllä tuloksella tai automaattisella testiuusinnalla.

## 11. Reports-kansion säilytys ja myöhempi siivous

### 11.1 Mitä käytiin läpi 30.10.2026

Koko `reports`-tiedostopuu käytiin läpi nimien, kokojen ja hakemistorakenteen perusteella: **158 782 tiedostoa, noin 13,78 GiB (14 800 697 154 tavua)**. Hakemistolinkkejä ei seurattu, lukuvirheitä ei tullut. Luvun 10 keskeiset päätulosraportit sekä valitut alkuperäiset ajolokit/tulos-JSONit luettiin vaadittujen secrets-tarkistusten jälkeen. Kaikkien tiedostojen sisältöä, arkistoja, kuvia tai binäärejä ei tarkastettu. Samanaikainen työ voi muuttaa määriä; tämä on inventointihetken tilanne.

Aiemmin nimetyt seitsemän suurta AVD-kansiota olivat poissa. Niiden vanhat raportit säilyvät historiatietona myös emulaattorien poistamisen jälkeen. Tässä tehtävässä ei poistettu, siirretty tai kopioitu aineistoa.

Alla luokat eivät mene päällekkäin: `build`/`.gradle`- ja `node_modules`-hakemistojen tiedostot on laskettu niiden omiin luokkiin ennen tiedostopäätteitä. Kansioon tallennettu `build` voi olla vanhan version varmuuskopio, joten luokitus ei ole poistovaltuutus.

| Aineisto | Koko / määrä | Käsittely |
| --- | --- | --- |
| APK:t build-/riippuvuuskansioiden ulkopuolella | 4,89 GiB / 118 | Vanhoja testi-, aiemman asennuksen ja julkaisun kopioita. Valitaan poistettavat vasta sidonnan ja palautustarpeen perusteella. |
| TXT | 2,16 GiB | Sisältää mapping-tiedostoja, mittaustuloksia ja alkuperäisiä runner-tuloksia; ei yleistä `*.txt`-poistoa. |
| PNG | 1,95 GiB / 17 470 | Suuri säästökohde myöhemmin. Säilytetään virheen osoittavat ja hyväksynnän ainoana näyttönä käytetyt kuvat. |
| `build` / `.gradle` hakemistot raporttien sisällä | 1,09 GiB | Osa on eristetyn lähdekopion rakennustuloksia, osa nimettyjä `previous-build-outputs`-varmuuskopioita. |
| JSON | 1,01 GiB | Mukana tulokset, paketti-/lähdeidentiteetit, UI-dumpit ja toistuvat säilymisinventaarit. Ei yleistä `*.json`-poistoa. |
| `node_modules` raporttien sisällä | 0,69 GiB / 51 919 tiedostoa | Uudelleen asennettavia riippuvuuskopioita; säilytetään ympäröivät lähteet ja lukitustiedostot. Linkit vaativat rajatun käsittelyn. |
| Markdown build-/riippuvuuskansioiden ulkopuolella | 63,9 MiB / 626 | Sisältää myös vanhoja kokonaisia dokumenttikopioita. Pienten varsinaisten tulosraporttien säilyttäminen on halpaa. |
| `.knittools-backup` ja `.tar` | 427,2 MiB yhteensä | Sekä synteettistä testiaineistoa että yksityistä palautusaineistoa; ei poisteta massana. |
| Perfetto-jäljet | 169,3 MiB / 68 | Avoimeen T2-mittausasiaan liittyvät alkuperäiset jäljet säilytetään. |

Suurimmat jäljellä olevat kokonaisuudet ovat `coderabbit-audit` (1,34 GiB), `optimized-runtime-20261021` (1,33 GiB), `language-audit` (0,90 GiB), `runs` (0,78 GiB) ja `audit-part10-20261017-01a14a83` (0,71 GiB). Näitä ei pidä poistaa kokonaisina pelkän koon perusteella.

`optimized-runtime-20261021` sisältää 14 eri alihakemistossa samankokoisen **97 133 196 tavun `mapping.txt`**-tiedoston. Tämä on noin 1,27 GiB. Sisällön yhtäläisyyttä ei hashattu tässä tehtävässä. Myöhemmin yksi kopio voidaan säilyttää kutakin todellista hashia/buildia kohti, jos viittaukset päivitetään. Pelkkä sama koko ei vielä todista duplikaattia.

### 11.2 Säilytettävät alkuperäistulokset nykyisissä paikoissaan

Tämä on siivouksen **säilytyslista**, ei lupa poistaa kaikkea listaamatonta. Alkuperäisiä tiedostoja ei tarvitse kopioida uuteen raporttikansioon. Säilytetään myös tarvittavat tulkintatiedot: tarkka komento/rajaukset, app/test APK- ja lähdetiivisteet, ympäristö sekä lopputulos. Jos raportin viittaama tiedosto poistetaan myöhemmin, siihen merkitään poisto ja korvaava säilytyspaikka; vanhaa linkkiä ei jätetä näennäiseksi todisteeksi.

| Kokonaisuus | Säilytettävä vähintään |
| --- | --- |
| S29:n julkaisu ja lähdeversio | [REPORT.md](reports/candidate-20261029-post8134-3e9a271b/REPORT.md), [artifacts.json](reports/candidate-20261029-post8134-3e9a271b/artifacts.json), [SHA256SUMS.txt](reports/candidate-20261029-post8134-3e9a271b/SHA256SUMS.txt), [mapping-association.json](reports/candidate-20261029-post8134-3e9a271b/mapping-association.json), [build-sources.zip](reports/candidate-20261029-post8134-3e9a271b/build-sources.zip) ja `artifacts/app-release.apk`, `app-release.aab`, `mapping.txt`. Viimeiset kolme ovat noin 115 MiB yhteensä; mapping ei ole tavallinen tarpeeton loki. Säilytetään myös alkuperäiset lähde-/build-identiteetit ja tarkistusraportit, joihin tämä REPORT viittaa. |
| S29:n lint ja korjaustestit | [full-lint-verification.json](reports/candidate-20261029-post8134-3e9a271b/full-lint-verification.json), [reused-test-evidence.json](reports/candidate-20261029-post8134-3e9a271b/reused-test-evidence.json), säilytetty queue-JUnit XML sekä [käyttäjän täysi lint-ajo](reports/runs/20261028T221431935-49376123/) manifesteineen ja viisine alkuperäisraportteineen. |
| S29:n käynnistys | [REPORT.md](reports/startup-smoke-20261029-post8135/REPORT.md) ja [public-ui-resumed.jsonl](reports/startup-smoke-20261029-post8135/public-ui-resumed.jsonl); yhteensä noin 23 KiB. Säilyttää myös ensimmäisen estetyn yrityksen. |
| S28G:n historiallinen käynnistys | [REPORT.md](reports/rw-11h-20261028/REPORT.md) ja [public-ui.jsonl](reports/rw-11h-20261028/public-ui.jsonl); prosessin palaamishavaintoa ei poisteta myöhemmän onnistumisen vuoksi. |
| S23:n käyttötapaukset | [REPORT.md](reports/candidate-runtime-20261023/REPORT.md); `api29-startup`, `api29-startup-retry`, `api37-suite-attempt1` ja `api37-targeted-retry` -ajojen identiteetit ja alkuperäiset `instrumentation.txt`-tulokset sekä kahden virheen kuvat. Säilytetään raporttiin sidottu driver-lähde/korjaus; vanha AVD ei ole tarpeen tuloksen lukemiseen. |
| S23:n Ravelry ja S28D:n PDF | [R10-raportti](reports/r10-live-20261023/REPORT.md), [sanitoitu live-tulos](reports/r10-live-20261023/phone-live/verified-results.json), [lopputila](reports/r10-live-20261023/phone-live/final-environment.json), [RW-11E-raportti](reports/rw-11e-20261028/REPORT.md), sen paketti-/ympäristösidonta ja onnistuneen viennin synteettinen PDF/tuloskuva. Yksityinen palautusarkisto käsitellään erikseen luvussa 11.4. |
| RW-03D/F/G/H/I/J | Luvun 10 raportit, tulos-JSONit, `tested-apks.json`/vastaavat manifestit ja lähdetiivisteet. D:n `instrumentation.log`; F/G:n tahallisten keskeytysten sekä jatkoprosessien alkuperäiset lokit ja checkpoint-tulokset; H/I:n `instrumentation-raw.txt`; J:n `instrumentation.txt` ja [provider-events.json](reports/rw-03j-20261028/provider-events.json). Säilytetään palautusvarmennusten vertaamat taulu-/tiedostotiivisteet, vaikka myöhemmin poistettaisiin testien APK-kopioita. |
| RW-07B/C | Alkuperäinen [este/probe](reports/rw-07b-20261028/REPORT.md), [jatkoraportti](reports/rw-07b-20261028/CONTINUATION-20261028.md), probe-lähde/JUnit sekä jatkon kolme lopullista JUnit XML:ää, `acceptance-final.log` ja `inputs.json`. [RW-07C-raportti](reports/rw-07c-20261028/REPORT.md), sen alkuperäinen instrumentointitulos ja lähde-/SDK-/APK-identiteetit. |
| RW-06A / avoin T2 | [measurement-summary.json](reports/rw-06a-20261027/measurement-summary.json), [raw-identities.json](reports/rw-06a-20261027/raw-identities.json), [trace-validation.json](reports/rw-06a-20261027/trace-validation.json), alkuperäiset benchmark-JSONit, 40 jälkeä, komennot ja ympäristö; erityisesti `emptyRequiredProfile`-kierros 006 sekä [trace-inspection.json](reports/rw-06a-20261027/emptyRequiredProfile/trace-inspection.json). Säilytetään myös `emptyNoProfile/instrumentation.txt` ja hyväksytty `emptyNoProfile-retry1`. |
| RW-CR-01 | [campaign-report.md](reports/coderabbit-audit/2026-10-27-rw-cr-01-e4416c7cf0a7/campaign-report.md), kampanjan `coverage.md`, `triage.json`, `final-correction-identities.json`, `final-source-state.json`, korjausdiffit sekä lopullinen ja keskeytynyt Android-tulos/loki. Katselmoinnin päätökset ja toteutuksen testitulokset ovat eri näyttöä. |
| Onboarding 1–10 | [PROGRESS.md](reports/onboarding-implementation/PROGRESS.md), vaiheiden lopulliset test-/Detekt-/lint- ja runtime-lokit. Erityisesti [phase10-test-detekt.log](reports/onboarding-implementation/phase10-test-detekt.log), [phase10-lint.log](reports/onboarding-implementation/phase10-lint.log), [phase9-runtime.log](reports/onboarding-implementation/phase9-runtime.log), [phase9-notices-final.log](reports/onboarding-implementation/phase9-notices-final.log) ja vaiheen 9 lopullisten ajokokonaisuuksien muut lokit. Säilytetään `phase9-*-verified.png` sekä muut raportissa hyväksynnän näyttönä nimetyt lopulliset kuvat. Koko kansio on vain noin 6,2 MiB; sen karsinta ei ole ensisijainen säästökohde. |
| Vanhat käyttöliittymä-, kieli- ja auditointikierrokset | Varsinaiset `REPORT`/`AUDIT`/`RESULTS`/`validation`/`ledger`-päätulokset, sovellettavat version/ympäristön tunnisteet, avoimet löydökset ja niiden ainoa alkuperäisnäyttö. Kansioita ei tyhjennetä sillä perusteella, ettei jokaista vanhaa testiä ole kopioitu tähän rekisteriin. |

### 11.3 Epäonnistumiset, joita ei saa hukata onnistumisten alle

| Havainto | Säilytys ja oikea tulkinta |
| --- | --- |
| S23:n API 29 -driveristä puuttuva luokka; API 37 URI-oikeus/widget-valitsin | Säilytetään alkuperäiset runner-tulokset ja onnistuneet rajatut uusinnat rinnakkain. Testityökalun virhe ei automaattisesti ole sovellusvika. |
| RW-06A:n UiAutomation-este ja puuttuva kierroksen 006 mittaripari | Esteen ajo ei tuottanut hyväksyttävää näytettä. Puuttuva mittaripari säilyy puuttuvana myös neljän benchmark-metodin PASSista huolimatta. |
| RW-03F/G:n `Process crashed` seed-lokit | Tahallisesti aiheutettu testiärsyke. Vain erillisten palautumisprosessien varmennukset ovat PASS. |
| RW-CR-01:n keskeytynyt JVM-ajo | [Keskeytynyt tulos](reports/coderabbit-audit/2026-10-27-rw-cr-01-e4416c7cf0a7/android-integrated-interrupted-result.json): 1 932 kirjattua testiä, 3 epäonnistumista, 1 ohitettu; muisti-/fixture-/odotusongelmat on erotettu myöhemmästä 2 331/2 331-lopputuloksesta. |
| RW-07B:n alkuperäinen jonon paluu | Probe osoitti vanhan A-tapahtuman palaavan C:n kanssa ennen korjausta. Säilytetään probe ja jatkon todellinen rajattu hyväksyntä, ei pelkkää lopullista PASS-lausetta. |
| Käyttäjän 28.10. CPD-löydökset | Handoff §8.134 ja [alkuperäinen ajo](reports/runs/20261028T210753169-4a3eea1e/) säilyttävät seitsemän duplikaattiryhmää, wrapper exit 1/native exit 4. [Korjauksen suora CPD-varmennus](reports/runs/20261028-cpd-fix/) on erillinen; tyhjä tulostiedosto tarvitsee ajon/exit-koodin asiayhteyden. |
| S29:n R8-tarkistushelperin ensimmäinen hylkäys | S29-kansion `inclusion-verification-attempt1-result.json`, `inclusion-verification-attempt1.log` ja `verify-inclusion-attempt1.py` sekä lopullinen inclusion-tulos. Väärä oletus erillisestä R8-luokasta korjattiin; sovellusta ei rakennettu tämän vuoksi uudelleen. |
| S29:n Avast-este ja RW-11H:n prosessin palaaminen | Säilytetään alkuperäisraportit; kumpaakaan syytä ei ole todistettu. S29:n onnistunut jatko ei ratkaise RW-11H:n erillistä havaintoa. |
| Onboardingin yhdeksän TrialNoticeRestoration-epäonnistumista | `phase9-runtime.log` ja `phase9-notices-final.log` sekä PROGRESS.md:n korjausselitys säilyvät yhdessä. Lopulliset kuvat säilytetään; tyhjää ennen renderöintiä otettua kuvaa ei lasketa visuaaliseksi hyväksynnäksi. |

### 11.4 Mitä ei voi vielä luokitella massapoistettavaksi

- **Yksityinen palautusaineisto:** R10-raportti yksilöi `reports/r10-live-20261023/phone-before-release-install.tar`-arkiston aiemman asennuksen tekniseksi palautusvarmuuskopioksi. Sen sisältöä ei avattu tässä tehtävässä. Se ei ole tavallinen testiloki eikä appin tuontimuotoinen varmuuskopio. Säilytys-/poistopäätös tehdään erikseen; koko R10-kansiota ei julkaista tai kopioida yhteiseen raporttipakettiin.
- **Lähdekopiot, korjausdiffit ja Git-hakemistot:** `coderabbit-audit/2026-10-27-rw-cr-01-e4416c7cf0a7` sisältää kolme sisäistä `.git`-kohdetta. Myös vanhemmassa kampanjassa on eristettyjä lähdekopioita. Niiden työn säilymistä ei todisteta pelkällä sijainnilla `reports`-kansiossa.
- **Vanhojen buildien varmuuskopiot:** `previous-build-outputs`, `previous-apks`, `before-apks`, `before/outputs` ja vastaavat voivat olla ainoa tietyn testiversion kopio. Ne ovat mahdollisia karsintakohteita, kun vaadittu paketti/mapping/lähdesidonta on säilytetty toisaalla.
- **Arkistoidut tietoturva-aineistot:** `archived-audit-artifacts` ja muut aiemman secrets-käsittelyn aineistot pidetään erillään tavallisista kuvakaappauksista. Arkistoja ei avattu eikä niiden turvallisuudesta tai poistettavuudesta päätelty mitään nimen perusteella.
- **Ainoat hyväksyntäkuvat ja avoimien havaintojen näyttö:** kuvan suuri määrä tai ikä ei yksin riitä poistoperusteeksi. Sama koskee mittausjälkiä, raakavarmennuksia ja pientä tulos-JSONia.

### 11.5 Suositeltu seuraava siivous

1. Säilytetään tämä kooste sekä yllä nimetyt alkuperäistulokset nykyisissä paikoissaan. Ei uutta kopiota koko `reports`-kansiosta.
2. Valitaan tarkat vanhojen riippuvuuskopioiden ja varmasti tarpeettomien rakennusvälitulosten polut. Raporttien sisäisiin varmuuskopioihin ei sovelleta yleistä `build`-nimistä kansiopoistoa.
3. Verrataan samankokoisten mapping-/APK-kopioiden tiivisteet ja säilytetään yksi tarvittava kopio kutakin rakennetta kohti sekä pakettien ja testien välinen sidonta. Julkaisun mapping säilyy aina sitä vastaavan säilytettävän julkaisupaketin kanssa.
4. Karsitaan kuvista toistuvat välivaiheet vasta kun lopullinen hyväksyntäkuva tai epäonnistumisen todiste on nimetty. Alkuperäiset pienet päätulosraportit säästetään.
5. Käyttäjä päättää erikseen yksityisistä palautusarkistoista ja vanhoista lähdetyöpuista. Vasta valittuihin kohteisiin annetaan rajattu poistokomento.

Tämä inventointi ja säilytyslista eivät vielä oikeuta koko `reports`-kansion tai kaiken listaamattoman aineiston poistamiseen. Aineisto säilyy tässä vaiheessa paikallaan, eikä dokumentointi käynnistä uusia testikierroksia.
