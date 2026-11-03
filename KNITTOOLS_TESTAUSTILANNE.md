# KnitTools: mitä on testattu ja mitä vielä tarvitaan

Päivitetty 3.11.2026. Tämä on pysyvä testauskooste: mitä testattiin, millä versiolla ja ympäristössä, mikä oli tulos ja mitä jäi avoimeksi. Vanhojen raporttien siivous kirjataan lukuun 11. Tässä dokumentointi- ja siivoustyössä ei ajettu sovellustestejä.

**Tulokset koskevat alkuperäisiä testiversioita, eivät automaattisesti nykyistä koodia.** Luvut 1–9 pohjautuvat 29.–30.10. tilannearvioon; myöhemmät tulokset ja täsmennykset ovat luvussa 10.4. P1/P2:n myöhempi puhelintulos on huomioitu myös luvussa 3. Play- ja pilvipalvelujen nykytilaa ei tarkistettu 3.11. Vanhojen raporttipolkujen nimet ovat lähdehistoriaa, eivät lupaus säilytetystä tiedostosta. Kooste säilyttää tuloksen ja rajauksen; se ei korvaa poistettua raakadataa uudelleenanalyysissä.

## 1. Missä nyt ollaan?

**Sovellusta on jo testattu paljon. Kaikkea ei tarvitse aloittaa alusta.** Tässä koonnissa yksilöity 29.10. allekirjoitettu versio on rakennettu, sen paketti on tarkistettu ja sen käynnistys sekä Asetukset on testattu Pixel 9:llä. Aiemmissa testikierroksissa löydetyt vahvistetut korjaukset on käsitelty raportoitujen rajojensa sisällä. Tämä paketti ei sisällä sen jälkeen tehtyä onboarding-työtä tai parhaillaan tehtäviä korjauksia.

Jäljellä on muutama nimetty puhelintarkistus, analytiikan tekninen rajoite, mittaustuloksen ja testikattavuuden koontia sekä ulkoisten palvelujen tarkistuksia. Lisäksi Play-ostot ja Play-asennukset odottavat Console-valmistelua. Ne eivät tee jo läpäistyistä paikallisista testeistä keskeneräisiä.

**29.10.2026 tilannearviossa KnitToolsille ei ollut vielä Play Console -valmisteluja.** Nykytilaa ei tarkistettu tässä siivoustyössä. Siksi tämä tiedosto erottaa kolme vaihetta:

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

Alla olevat kolme kohtaa koskevat 29.10.2026 paketoitua versiota. Sen rakennusraportti (alkuperäinen lähde: `reports/candidate-20261029-post8134-3e9a271b/REPORT.md`) ja puhelinraportti (alkuperäinen lähde: `reports/startup-smoke-20261029-post8135/REPORT.md`) yksilöivät käytetyt tiedostot.

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
| Kamera ja widget | Molempien kuvauspolkujen tallennus/peruutus emulaattorilla, aiempia puhelimen kameran palautumistestejä, widgetistä avaaminen ja laskurin näyttö. Erillisessä puhelimen widget-testissä painettiin plussaa. | **Myöhempi puhelintulos raportoitu 29.10.** Luvut 3 ja 10.4 erottavat chatin yhteenvedon alkuperäisistä ajolokeista. |
| Analytiikan suostumus ja jono | Suostumuksen perumisen jälkeen vanhat jonossa olevat tapahtumat eivät lähteneet; uudelleen sallimisen jälkeen lähti vain uusi tapahtuma paikalliseen testivastaanottimeen. Myös tallennus- ja virhetilanteita. | **Tehty tietokoneella ja rajatusti oikealla Android-laitteella.** Handoff §§8.123 ja 8.125. Ei oikean PostHog-palvelun toimitustesti. |
| Viimeisten korjausten regressiot | Analytiikan alustamisen epäonnistumisen siivous, kuvan lukemisen keskeytys sekä virheellisen piirtoeleen jälkeen kelvollisen eleen tallennus. | **Kohdennetut automaattitestit tehty.** Handoff §8.134 kirjaa 50 testiä ja myöhemmän kuuden testin uusinnan; kuusi sisältyy aiempaan joukkoon. |
| Käyttöliittymän sovittuminen ja saavutettavuus | Sovittuvat näkymät, pienet leveydet, suuri fontti, niiden yhdistelmä ja TalkBack aiemmissa kampanjoissa. | **Tehty kirjatuissa laajuuksissa.** Ei uusintaa ilman nimenomaista pyyntöä. |
| Koodikatselmointi ja automaattiset tarkistukset | Laaja CodeRabbit-katselmointi, vahvistettujen havaintojen korjaukset ja kohdennetut jatkotarkistukset. Kampanjassa raportoitiin 2 331 Androidin JVM-testin läpäisy. | **Tehty raportoiduille versioille.** Handoff §§8.101, 8.131 ja 8.134–8.135. Katselmointi ei ole puhelimen käyttötapaus eikä testimäärä tarkoita täydellistä haarakattavuutta. |

Päälähteet: [Androidin julkaisutarkistussuunnitelma](ANDROID_RELEASE_READINESS_PLAN.md), [handoff](KNITTOOLS_REMAINING_WORK_HANDOFF.md) ja jäljellä olevien töiden koonti (alkuperäinen lähde: `reports/rw-11f-20261028/REPORT.md`). Handoffin myöhemmät päivätyt tulokset täydentävät vanhempaa koontia.

## 3. Vielä tarvittavat paikalliset puhelintarkistukset

Nämä eivät tarvitse Google Play Consolea tai julkista julkaisua. Käytetään tunnistettua allekirjoitettua versiota ja vain tehtävää varten tehtyjä testitietoja. Henkilökohtaisia projekteja ei poisteta tai korvata. Jos toiminto tarvitsee Pro-oikeuden, testin todellinen käyttöoikeustila selvitetään ensin; debug-version vapaa pääsy ei todista julkaisuversiota.

### P1. Kamera: tallennus ja peruutus — myöhemmin raportoitu tehdyksi

29.10. myöhemmän Pixel 9 -testichatin yhteenvedossa kamera- ja PDF-toiminnot raportoitiin hyväksytyiksi uusimmalla allekirjoitetulla APK:lla, jonka vastaavuus laitteen asennukseen tarkistettiin. Testi käytti omaa testiprojektia. Tämä on chatin yhteenvedon tieto, ei 3.11. uudelleen luettu alkuperäinen laiteloki tai jokaisen askeleen erillinen kuitti. Lähde ja rajaus: luku 10.4. Pelkän tämän koonnin vanhan avoimen merkinnän vuoksi testiä ei tilata uudelleen.
### P2. Kotinäytön widget: oikeat painikkeet — myöhemmin raportoitu tehdyksi

Saman 29.10. Pixel 9 -testichatin mukaan widgetin käyttö ja synkronointi läpäisivät. Tulos täydentää aiempaa käynnistystestiä, joka ei kattanut widgetin käyttöä. Kooste ei yksilöi kaikkia painalluksia alkuperäisestä lokista; tuloksen näyttötaso on chatin yhteenveto. Lähde ja rajaus: luku 10.4. Ei automaattista uusintaa.
### P3. Värinäasetus — tekninen havainto tehty, tuntuma vahvistamatta

29.10. myöhemmässä Pixel 9 -testissä `dumpsys vibrator_manager` kirjasi KnitToolsin värinän asetuksen ollessa päällä eikä uusia pyyntöjä pois päältä. Alkuperäinen asetus palautettiin. Tämä tekninen havainto ei yksin vahvista alla tarkoitettua käyttäjän tuntemaa värinää.

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

Alla olevien allekirjoitettujen pakettien versionName/versionCode ovat kaikki **1.0.0 / 1**, joten pelkkä versionumero ei erota niitä. Tarkat APK-tiivisteet ovat alkuperäisraporteista; paketteja ei rakennettu tai hashattu uudelleen tässä dokumentointityössä. Historiallinen HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f` ei yksin yksilöi buildia, koska työpuussa oli tallentamattomia muutoksia. Lähdeinventaarioita ja vanhoja lähdearkistoja ei säilytetä tämän koonnin edellytyksenä.

| Tunnus | Paketti ja alkuperäinen rakennusraportti | APK:n SHA-256 |
| --- | --- | --- |
| S23 | 23.10. allekirjoitettu ehdokas (alkuperäinen lähde: `reports/candidate-20261023/REPORT.md`) | `91E312882F7DACDEAA63B8A994986EEA988AEC696D5F68D519C70DD360C33803` |
| S28D | 28.10. RW-11D (alkuperäinen lähde: `reports/candidate-20261028-rw11d-cbb80189/REPORT.md`) | `23B079C44B5D98002EE1262F97D8C3397D860C0BACA2D01DAF9D5BD10729C27C` |
| S28G | 28.10. RW-11G, suostumuskorjauksen jälkeen (alkuperäinen lähde: `reports/candidate-20261028-rw11g-da03837e/REPORT.md`) | `BAEF789D20CAAC40E19881B73F77C2554C33E45B720705CD8B5B90A964CC3F4A` |
| S29 | 29.10. §8.134:n kolmen korjauksen jälkeen (alkuperäinen lähde: `reports/candidate-20261029-post8134-3e9a271b/REPORT.md`) | `65D971AD369C313FE4971D2C0F7465FE2BCEE6108D56184C08347D2830D49797` |

Eristetty `activepolicytest`-sovellus, optimoitu `optimizedtest` ja allekirjoitettu tuotantosovellus ovat eri testikohteita. Eristetyn testin tulosta ei siirretä S23–S29-paketeille. Seuraavan taulukon lyhyet hash-alkuosat ovat lukemisen apuna; eristettyjen testien täydet app/test APK- ja lähdetiivisteet olivat alkuperäisraporteissa. Niitä ei voi palauttaa lyhyistä tunnisteista raporttien poiston jälkeen.

### 10.2 Säilytettävä testirekisteri

| Testi / ajankohta | Versio ja ympäristö | Dokumentoitu tulos | Rajaus ja avoin asia |
| --- | --- | --- | --- |
| Paikalliset käyttötapaukset 23.10. | S23; Android 17/API 37- ja Android 10/API 29 -emulaattorit; normaali 360 × 800 dp, fontti 1.0; verkko estetty. Raportti (alkuperäinen lähde: `reports/candidate-runtime-20261023/REPORT.md`), julkaisutarkistussuunnitelma §10.7. | Seitsemällä eri API 37 -käyttötapauksella hyväksytty suoritus sekä API 29:n 1/1 käynnistys/navigointitesti. Ensimmäinen API 37 -ajo oli 5 hyväksyttyä / 2 epäonnistunutta; rajattu uusinta 2/2 hyväksyttyä. | Driverin URI-oikeus ja widget-valitsin korjattiin. Alkuperäinen epäonnistunut ajo säilyy epäonnistuneena. Ei live-palveluja, aitoa kokeilun päättymistä tai puhelimen suorituskykyhyväksyntää. |
| Ravelryn kirjautuminen ja PDF 23.10. | S23; Pixel 9, Android 17/API 37, WebView 155.0.8059.43. Raportti (alkuperäinen lähde: `reports/r10-live-20261023/REPORT.md`). | R10:n nimetyt live-vaatimukset läpäisty: kirjautuminen, Activity-/tehtäväpalautuminen, historia/Takaisin, PDF:n tallennus ja uudelleenavaus, toisen tuonnin peruutus. | Yksi aiempi URL-fallback ei ollut historiatestin PASS. Yksityisten tiedostojen fyysistä poistumista ei voitu tarkastaa. Ei R11-/osto-/palvelutoimitustestiä. |
| Kylmäkäynnistyksen mittaus RW-06A 27.10. | `optimizedtest`, debug-allekirjoitettu optimoitu APK `9504F711…`; Pixel 9/API 37. Mittauskooste (alkuperäinen lähde: `reports/rw-06a-20261027/measurement-summary.json`), handoff §8.93. | Neljä benchmark-metodia läpäisi; 40 jälkeä mutta **39/40 TTID/TTFD-mittariparia**. | Ei tuotantoallekirjoitettu APK. Empty/Require-kierroksen 006 mittarit puuttuvat. T2 pysyy avoimena. Raakajälkien poiston jälkeen puuttuvaa paria ei voi selvittää tästä koosteesta. |
| Suuri Insights-historia RW-03D 27.10. | `activepolicytest` app `72E16304…`, test APK `9DBAC19F…`; Pixel 9/API 37, oikea Room. Ajoloki (alkuperäinen lähde: `reports/rw-03d-20261027/instrumentation.log`), paketit (alkuperäinen lähde: `reports/rw-03d-20261027/tested-apks.json`), handoff §8.97. | 1/1 testi; 100 001 riviä täsmälleen kerran, sisältö säilyi. | Yksi synteettinen jakauma ja All Time -valinta; ei suorituskykyrajaa, samanaikaisen kirjoittajan vasteaikaa tai tuotanto-APK:n näyttöä. |
| Palautuksen keskeytyminen RW-03F/G 27.10. | Sama `72E16304…` app; F:n test APK `4BF4CA0A…`, G:n `93A25069…`; Pixel 9/API 37. F ennen commitia (alkuperäinen lähde: `reports/rw-03f-20261027/precommit-result.json`), F commitin jälkeen (alkuperäinen lähde: `reports/rw-03f-20261027/postcommit-result.json`), G:n kooste (alkuperäinen lähde: `reports/rw-03g-20261027/verification-summary.json`); handoff §§8.98–8.99. | F: 4/4 eri prosessissa tehtyä varmennusta; G: 1/1 osittaisen jälkisiivouksen jälkeen. | Tahallisesti tapetut seed-ajot ovat keskeytyksiä, eivät PASS-testejä. Vain nimetyt katkaisukohdat ja synteettiset tiedot, ei henkilökohtaisten tietojen palautuskoetta. |
| RW-CR-01, valmistui 28.10. | Kampanjan dirty-lähdeversio; paikallinen JVM/Gradle, ei laiteajo. Kampanjaraportti (alkuperäinen lähde: `reports/coderabbit-audit/2026-10-27-rw-cr-01-e4416c7cf0a7/campaign-report.md`), lopputulos (alkuperäinen lähde: `reports/coderabbit-audit/2026-10-27-rw-cr-01-e4416c7cf0a7/android-integrated-final-result.json`). | 2 331 JVM-testiä, 0 failures/errors/skipped; Android-testilähteet kääntyivät. | Instrumentointia ei suoritettu tässä ajossa. Aiemmat keskeytykset ja epäonnistumiset säilyvät erillisinä; testimäärä ei todista kaikkia haaroja tai nykyistä työpuuta. |
| Room 21→22 RW-03H 28.10. | `activepolicytest` app `49F05657…`, test APK `B64D2BD5…`; Pixel 9/API 37. Raportti (alkuperäinen lähde: `reports/rw-03h-20261028/REPORT.md`), tulos (alkuperäinen lähde: `reports/rw-03h-20261028/instrumentation-status.json`). | 2/2 testiä; URI-otsikot, Unicode ja nimetyt omistajuus-/säilymisinvariantit. | Synteettinen migraatio oikeassa Roomissa; ei koko migraatioketjua, live-SAF:ää tai oikean käyttäjän tietokannan päivitystä. |
| Lankakuvan vaihto RW-03I 28.10. | Sama `49F05657…` app, test APK `FF572378…`; Pixel 9/API 37. Raportti (alkuperäinen lähde: `reports/rw-03i-20261028/REPORT.md`), tulos (alkuperäinen lähde: `reports/rw-03i-20261028/instrumentation-status.json`). | 3/3: tyhjän ja virheellisen kuvan hylkäys säilyttää vanhan; kelvollinen JPEG vaihtuu ja vanha poistuu. | Synteettiset paikalliskuvat ja oikea Android-dekoodaus; ei kaikkia kuvamuotoja, pilvipalveluntarjoajia tai palautumistilanteita. |
| PDF-viennin katkaisu RW-03J 28.10. | Sama `49F05657…` app, test APK `42559105…`; Pixel 9/API 37. Raportti (alkuperäinen lähde: `reports/rw-03j-20261028/REPORT.md`), tulos (alkuperäinen lähde: `reports/rw-03j-20261028/results.json`). | 2/2: lyhyempi vienti katkaisee kohteen; `wt`-hylkäys säilyttää kohteen ilman `w`-varapolkua. | Oikea renderer ja tiedostot, mutta prosessin sisäinen testiprovider; ei ulkoisen providerin/pickerin yhteistoiminnan todiste. |
| Usean PDF:n käyttö RW-11E 28.10. | S28D; Pixel 9/API 37, oikea SAF. Raportti (alkuperäinen lähde: `reports/rw-11e-20261028/REPORT.md`). | Kahden PDF:n tuonti, ensisijaisen poisto, jäljelle jääneen uudelleenavaus stop/startin jälkeen, piirros ja viedyn PDF:n avaus. | Nimetty dokumentin poistopolku; ei erillinen detach, OS-prosessikuolema, rollback tai fyysisten orpojen siivouksen varmennus. |
| Suostumuksen tiukennus RW-07B 28.10. | Paikallinen JVM; PostHog Android 3.72.1/core 6.47.1; lähdetiivisteet jatkoraportissa. Jatkoraportti (alkuperäinen lähde: `reports/rw-07b-20261028/CONTINUATION-20261028.md`). | 16/16 testiä. Vanha jono ei palaa uuden suostumuksen yhteydessä; kontrolloitu vastaanotin, ei DNS-/palvelulähetystä. | Alkuperäinen REPORT.md on aiemman esteen raportti. Jo lähtenyttä pyyntöä ei voi perua takautuvasti; SDK-työntekijöiden elinkaari ja T1/T4 ovat erillisiä. |
| Android-suostumus RW-07C 28.10. | `activepolicytest` app `243D38E1…`, test APK `D9A73CB4…`; Pixel 9/API 37, oikea DataStore/SDK/jono. Raportti (alkuperäinen lähde: `reports/rw-07c-20261028/REPORT.md`). | 1/1 rajattu integraatiotesti; uudelleen sallimisen jälkeen vain uusi tapahtuma kontrolloituun vastaanottimeen. | Ei tuotanto-Appin kaikkia polkuja, oikeaa PostHog-toimitusta, ajastimia tai SDK-säikeiden tuotantoelinkaarta. |
| Käynnistys/Asetukset RW-11H 28.10. | S28G; Pixel 9/API 37, analytiikka pois. Raportti (alkuperäinen lähde: `reports/rw-11h-20261028/REPORT.md`). | Rajattu käyttöketju ja yksi stop/start läpäisty; havaitut asetukset säilyivät. | Loppusiivouksessa prosessi palasi selittämättä. Syytä ei vahvistettu; havainto ei ole todiste SDK-viasta tai sen korjauksesta. |
| Korjausten regressiot ja täysi lint 28.–29.10. | S29:n lähdeaineisto; Gradle 9.8.1 / Temurin 17.0.20. S29-raportti (alkuperäinen lähde: `reports/candidate-20261029-post8134-3e9a271b/REPORT.md`), handoff §8.134. | 50 kohdennettua JVM-testiä ja myöhempi niihin sisältyvä 6/6 uusinta; käyttäjän `lc -Full` läpäisty, debug- ja release-lint. | Ei 56 eri testiä. 22 olemassa olevaa Detekt-baselinen merkintää. Osan 50 testin näyttö on handoffin tallennettu ajotulos, ei enää erillinen JUnit-kuitti. |
| Paketointi 29.10. | S29; Gradle 9.8.1 / Temurin 17.0.20, offline Windows-build. Raportti (alkuperäinen lähde: `reports/candidate-20261029-post8134-3e9a271b/REPORT.md`). | APK/AAB, allekirjoitus, pakettisisältö, mapping-yhteys ja olemassa olevien profiilien yhteensopivuus hyväksytty. | Staattinen paketointinäyttö, ei ajonaikainen kolmen korjatun virhepolun testi, Play-hyväksyntä tai palvelutoimitus. |
| Käynnistys/Asetukset 29.10. | S29; Pixel 9, Android 17/API 37. Raportti (alkuperäinen lähde: `reports/startup-smoke-20261029-post8135/REPORT.md`). | Avastin estämä ensimmäinen yritys; käyttäjän poistettua Avastin jatko läpäisi Projects/Asetukset-ketjun ja yhden stop/startin. Kuusi havaittua asetusarvoa säilyi. | Varoituksen syy tuntematon. Ei SDK-elinkaaren, OS-prosessikuoleman, suorituskyvyn tai palvelujen hyväksyntää. |
| Onboarding-vaiheet 1–10, valmis 30.10. | `codex/onboarding-20261029`, muuttunut työpuu; paikalliset JVM/Gradle-ajot ja eristetyt activepolicy-emulaattoritestit. Edistymisraportti (alkuperäinen lähde: `reports/onboarding-implementation/PROGRESS.md`). | Raportin mukaan jokaisessa vaiheessa test/Detekt/lint hyväksytty. Lopussa **2 509/2 509 JVM-testiä**; vaiheen 10 testi-/Detekt- ja lint-lokien BUILD SUCCESSFUL tarkistettu tässä koonnissa. Vaiheiden 2–9 kohdennetut runtime-tulokset on eritelty alkuperäisraportissa. | 2 509 on raportista, ei tässä uudelleen laskettu JUnit-XML:istä. Lopullinen lint-loki osoittaa debug-lintin, ei uutta release-lintiä. Vaihe 11 pois rajauksesta. Tarkkaa yhteistä APK-hashia/emulaattori-image-identiteettiä ei sidottu tähän koontiin; ei S29:n testitulos eikä nykyisen korjaustyön hyväksyntä. |

Onboardingin vaiheessa 9 ensimmäinen laiteajo oli **36 testiä / 9 epäonnistumista**. Alkuperäinen loki vahvistaa tämän. Activityn uudelleenluonnin splash-tilaa ja testin Compose-solmun hakua korjattiin; loppuajossa `TrialNoticeRestorationTest` läpäisi **14/14**. Kampanja raportoi kaikki 36 valittua tapausta hyväksytyiksi usean lopullisen ajon perusteella, ei yhtenä 36/36-uusinta-ajona. Määrät ovat päällekkäisiä eikä niitä lasketa uusiksi erillisiksi testeiksi.

### 10.3 Mitä tämä täydennys ei sulje

- P1–P4, T1–T7 sekä luvun 6 päätökset säilyvät viimeisimmän luetun tilaraporttinsa mukaisina. Onboarding-kampanja ei yksin sulje puhelintarkistuksia, luonnollista 14 päivän päättymistä, palvelutoimitusta tai Play-jakelua. Myöhempi Pixel 9 -chatin tulos on erotettu luvussa 10.4.
- T3:n koko nykyisen koodin kattavuus-/vaatimusvertailu on edelleen erillinen avoin asia. Tämä rekisteri yksilöi säilytettävän näytön; se ei ole uusi koko sovelluksen auditointi.
- T2:n puuttuva mittaripari ja RW-11H:n prosessihavainto säilyvät alkuperäisellä epävarmuudellaan. Onboardingin korjattu splash-havainto on eri ilmiö.
- Alkuperäisraportit sisälsivät myös myöhemmin vanhentuneita esteitä. Päivätty jatkoraportti ratkaisee vain oman esteensä; vanhaa FAIL/BLOCKED-tulosta ei kirjoiteta jälkikäteen PASSiksi.
- Jos jo siivotuissa build-kansioissa oli ainoa alkuperäinen JUnit-/HTML-tulos, sitä ei palauteta tällä tekstillä. Jäljellä oleva loki tai koonti merkitään sen tasoiseksi näytöksi. Puutetta ei paikata keksityllä tuloksella tai automaattisella testiuusinnalla.


### 10.4 Myöhemmin koottu näyttö 29.10.–2.11.2026

Tämän luvun raportit ja nimetyt lokit luettiin 3.11. ennen suunniteltua poistoa; Pixel 9 -rivin lähde on erikseen nimetty chatin yhteenveto. Koko reports-puu inventoitiin tiedostotasolla, mutta kaikkia vanhoja lokirivejä, kuvia tai binäärejä ei avattu. Lukumäärät ovat alkuperäisten ajojen tuloksia, eivät tässä tehtävässä ajettuja testejä. Eri ajokertojen määriä ei lasketa yhteen kattavuusluvuksi.

| Ajankohta ja kohde | Todettu tulos ja ympäristö | Rajaus / alkuperäinen lähde |
| --- | --- | --- |
| 29.10. Pixel 9:n myöhempi käyttöläpikäynti | Chatin yhteenvedon mukaan asennusta vastaava uusin allekirjoitettu APK: kamera/PDF ja widgetin käyttö/synkronointi hyväksytty. Laskurit, muistiinpanot, lankakulutus, kirjasto, laskentatyökalut, tilastot ja varmuuskopion vienti toimivat tarkistetuissa poluissa. Ei havaittua FATAL EXCEPTION- tai ANR-tapahtumaa. Värinä päällä näkyi järjestelmän värinäpalvelussa; pois päältä ei tullut uusia pyyntöjä. | Näyttönä on 29.10. testichatin tallennettu yhteenveto, chat `01a186a6-1b1f-71a1-816b-ff0de9d01f36`; alkuperäistä ADB-lokia ei luettu tässä. Yhteenveto ei sisällä tarkkaa APK-hashia tai kaikkien painallusten erittelyä. Ravelry-kirjautuminen ohitettiin eikä varmuuskopiota palautettu olemassa olevien tietojen päälle. Käyttäjän tuntemaa värinää ei voi päätellä järjestelmäkutsusta. Läpikäynti jäi osittaiseksi. |
| 30.10. Gradlen riippuvuusvarmennuksen uusinta | Debug- ja releaseRuntimeClasspath sekä baselineprofilen benchmarkRuntimeClasspath läpäisivät `--dependency-verification strict` -ajot, exit 0. Gradle 9.8.1 / JDK 17. | Alkuperäinen deserialisointivirhe ei toistunut; tarkkaa syytä ei vahvistettu. Ei koodi- tai riippuvuusmuutosta. `reports/dependency-verification-fix-20261030/REPORT.md`. |
| 31.10. Riippuvuuskorjaukset | Tiukka Gradle-varmennus, debug-build ja Dependency-Check läpäisivät. 1 136 valittujen komponenttien havaintoa: 0 osumaa raakaraportin haavoittuviin versioihin. OWASP: 376 riippuvuutta, 0 havaintoa poikkeusten jälkeen. OSV: 127 raakalöydöstä / 66 advisoryä, poikkeusten jälkeen 0 avointa. Functions Node 22.23.4: 113 testiä läpäisi; rimraf/glob-toimintakoe läpäisi. | OSV:n vanhat versiot olivat varmennusmetadatassa, eivät valittuina tarkistetuissa konfiguraatioissa. Poikkeukset määräaikaisia 30.11.2026 asti. Neljä Firestore-emulaattoriryhmää ohitettiin. Androidin yksikkö-/laitetestejä ei ajettu tässä kampanjassa. `reports/dependency-fix-20261031/REPORT.md`. |
| 2.11. Uudistettu onboarding | `codex/onboarding-launch-pricing`, tuolloin muuttunut työpuu. 144 erillistä kohdennettua JVM-testiä useassa ajossa. API 36, 411 dp, fontScale 1.0, activepolicytest: 12 kieli-/teematestiä, 4 Room-/migraatiotestiä ja 1 koko polun testi läpäisivät, yhteensä 17 erillistä Android-testiä. Debug- ja testipaketit kääntyivät. | Koko polun aiempi epäonnistuminen paljasti laskurin Kumoa-virheen; viimeinen ajo läpäisi korjauksen jälkeen. Ostohinta testiaineistoa; ei oikeaa Play-maksua. Ensimmäinen connected-ajo osui myös liittyneeseen Pixel 9:ään erillisellä testipaketilla; siitä ei päätellä tuotantoversion hyväksyntää. 320 dp / 200 % / TalkBack ei uusittu. `reports/onboarding-review-v2/IMPLEMENTATION.md`. |
| 2.11. Yöajon build ja JVM | Build CLEAN; 1 243 syötetiedostoa muuttumattomina. JVM:n ensimmäinen ajo: 2 577 testiä / 6 virhettä vanhoissa skeemaodotuksissa. Korjauksen jälkeen 77 backup-testiä sekä myöhempi koko 2 577 testin Sonar-ajo läpäisivät ilman virheitä/ohituksia. | Testiodotukset päivitettiin Room 26:een; tuotantokoodia ei tässä backup-korjauksessa muutettu. Ei instrumentointia. `reports/night-checks-20261102/REPORT.md`. |
| 2.11. Turvallisuus ja julkaisurajat | sc CLEAN: OSV 127 hyväksyttyä advisoryhavaintoa, 0 estävää; OWASP 11 poikkeuksella käsiteltyä paketti/CVE-paria, 0 estävää. Salaisuudet/Semgrep 0 löydöstä; MobSF 7 informatiivista. Google Android Security Lints ja Dependabot CLEAN. DeepSec: 68 matcher-osumaa / 25 tiedostoa, 2 uutta analyysiä, 0 löydöstä. rs 13 PASS; rst 25 PASS. | Poikkeuksellinen CLEAN ei tarkoita raakalöydösten puuttumista. DeepSecillä rajattu matcher-kattavuus. Tunnettuja Ravelry-salaisuusarvoja ei ollut rs-vertailuun. CodeQL ERROR: workflow/lisenssiedellytys puuttui, analyysi ei käynnistynyt. `reports/night-checks-20261102/REPORT.md`. |
| 2.11. Vakaus, PMD ja Sonar | Onboardingin vakausbaseline päivitettiin, erillinen debugStabilityCheck läpäisi. PMD:n jatkokorjauksissa 5 toistoa väheni 2:een; tulos jäi FINDINGS. Sonarin 7 huomiota väheni 3 perusteltuun S6311-huomioon; CE SUCCESS / Quality Gate OK. Lopullinen koko JVM 2 577/2 577; Android-testilähteiden käännös läpäisi. | PMD:n todellinen raja oli 50 tokenia. Jäljelle jäivät Room-annotaatiot ja lyhyet laskutustestien tarkistukset; IO-rajat säilytettiin JSON/DataStore-työn vuoksi. Sonar varoitti puuttuvista Java-binaareista ja ratkaisemattomista tyypeistä/importeista. Analyysi `77c6d79c-3fc8-4c74-b286-b601c9887981`, pohja-HEAD `5fe70e1d48768de21d3218d25f49b6232071d6d7` sekä työpuumuutokset. `reports/maintenance-findings-20261102/REPORT.md`. |
| 2.11. PR 238:n paikallinen varmennus | 2 588 JVM-testiä, 0 failures/errors/skipped. ktlint, Android-testien ja benchmarkRelease-lähteiden käännökset sekä erillinen Detekt läpäisivät. JaCoCo-vertailun paikallinen uuden koodin arvio 193/208 = 92,79 % PR-pohjaa `b7002bd26e7c99c7d2efcf52b0b305a2c704e4cf` vasten. | Arvio ei ole SonarCloud Quality Gate. Optimoitu smoke ja UI-regressio käännettiin, ei suoritettu paikallisesti. `reports/pr238-fixes/validation.txt`. |
| PR 238:n PDF-jatkokorjaus | `OnboardingPdfStorageTest`, ktlint ja Detekt päättyivät BUILD SUCCESSFUL. | Rajattu myöhempi ajo; siitä ei päätellä uutta koko 2 588 testin ajoa. `reports/pr238-fixes/sonar-followup-validation.txt`. |
| PR 237:n optimoitu smoke-uusinta | Alkuperäisloki: OptimizedSmokeTest `freeGateTrialAndAdaptiveTransitions` ja `projectCounterSessionAndNavigation`, OK (2 tests). | Tämän lokin perusteella ei yksilöity tässä laitteen imagea tai APK-hashia eikä nykyisen HEADin kattavuutta. `reports/pr237-smoke-fix/verified-instrumentation.txt`. |

### 10.5 Historialliset epäonnistumiset ja näytön rajat

Näitä ei muuteta hyväksytyiksi poistamalla alkuperäisloki:

| Havainto | Säilyvä tulkinta |
| --- | --- |
| S23:n API 29 -driverin puuttuva luokka sekä API 37 URI-oikeus/widget-valitsin | Ensimmäiset ajot epäonnistuivat. Korjatut rajatut uusinnat hyväksyttiin; testityökalun virhe ei yksin ole sovellusvika. |
| RW-06A:n UiAutomation-este ja Empty/Require 006:n mittaripari | Esteellinen ajo ei tuottanut hyväksyttävää näytettä. Neljän benchmark-metodin PASS ei täytä puuttuvaa 40. mittariparia. Raakajälkiä ei säilytetä; T2 jää dokumentoiduksi puutteeksi, ei automaattiseksi uusintapyynnöksi. |
| RW-03F/G:n Process crashed | Tahallinen testiärsyke. Vain erillisen prosessin palautumisen varmennukset olivat hyväksyttyjä. |
| RW-CR-01:n keskeytynyt ajo | 1 932 kirjattua testiä, 3 epäonnistumista, 1 ohitettu. Fixture-/muisti-/odotusongelmien jälkeen loppuajo 2 331/2 331. Backend 113 paikallista testiä Node 24.22.1:llä ja matcherit 51 testiä; ei tuossa ajossa Node 22- tai Firestore-emulaattoritodistetta. |
| RW-07B:n suostumusjono | Probe osoitti vanhan A-tapahtuman palaavan C:n mukana ennen korjausta. Korjauksen 16 JVM-testiä ja RW-07C:n yksi Android-testi hyväksyttiin omissa rajauksissaan. |
| 28.10. CPD | Alkuperäinen 7 duplikaattiryhmän löydös, wrapper exit 1/native exit 4. Korjauksen suora CPD-varmennus oli eri ajo. |
| S29:n R8-helperi | Ensimmäinen tarkistus hylkäsi väärän oletuksen erillisestä R8-luokasta. Helper korjattiin ja tarkistus läpäisi ilman sovelluksen uudelleenrakentamista. |
| S29:n Avast ja RW-11H:n prosessin paluu | Avast esti ensimmäisen yrityksen; käyttäjän poiston jälkeen smoke läpäisi. Kummankaan alkuperäisen ilmiön juurisyytä ei todistettu. |
| Onboardingin ensimmäiset ajot | Vaiheen 9 yhdeksän epäonnistumista ja 2.11. Kumoa-havainto ratkaistiin nimetyissä uusintatesteissä. Vanhat epäonnistuneet ajot eivät muutu jälkikäteen onnistuneiksi. |
| 2.11. CodeQL / PMD / Sonar | CodeQL jäi suorittamatta. PMD:n 2 ja Sonarin 3 perusteltua huomiota jäivät näkyviin; niitä ei vaimennettu vihreän tuloksen saamiseksi. |

## 11. Raporttiaineiston siivous 3.11.2026

Käyttäjän pyynnöstä tämä kooste korvaa vanhan vaatimuksen säilyttää raportit, APK/AAB-kopiot, mappingit, kuvat, mittausjäljet, lokit, testien synteettiset aineistot, riippuvuuskopiot ja auditointien eristetyt lähdekopiot. Niitä ei kopioida uuteen arkistoon. Raporttien poistuminen ei peru tehtyjä testejä eikä hyväksy avoimia kohtia. Tarkka vanhan buildin toisintaminen, poistettujen jälkien uudelleenanalyysi tai vanhan APK:n virheiden purkaminen ei enää onnistu tämän koosteen avulla.

Ennen siivousta `C:\Dev\KnitTools\reports` sisälsi 109 519 tiedostoa ja 14 173 685 885 tavua (14,17 Gt / 13,20 GiB). Kansiossa ei ollut Gitin seuraamia tiedostoja. Kymmenen hakemistolinkkiä tunnistettiin, mukaan lukien linkki varsinaiseen `functions/node_modules`-kansioon; linkin kohdetta ei saa poistaa raporttien mukana. Kolme sisäistä Git-repoa olivat RW-CR-01:n synteettisiä katselmointikopioita. Kahden työpuu oli puhdas; kolmannen ainoa muutos oli `gradlew.bat`-rivinvaihdot (diff tyhjä `--ignore-space-at-eol`-vertailussa). Sovelluksen varsinainen työpuu oli alussa puhdas, haara `codex/onboarding-launch-pricing`, HEAD `72fa92e3`.

Henkilökohtaiset palautusvarmuuskopiot säilytetään käyttäjän nimenomaisen vastauksen mukaisesti paikassa `.local/report-recovery-backups/`. Kansio on rajatusti Gitin ulkopuolella. Mukaan jätetään myös henkilökohtaiseksi mahdollisesti kuuluva vanha normal-/notes-palautusaineisto; sitä ei avata sisällön julkaisemiseksi. Nämä TAR-tiedostot ovat teknisiä palautuskopioita, eivät sovelluksen Tuo varmuuskopio -toiminnon tiedostoja. Alkuperäisen hakemiston nimi säilyy palautuskopion polussa.

**Lopputila 3.11.2026: dokumentointi ja palautuskopioiden siirto tehty, raporttien poisto estetty.** Automaattinen hyväksyntätarkistus hylkäsi rajatun PowerShell-poistokomennon ennen sen käynnistymistä: `blocked by policy`. Hakemistolinkkejä tai raportteja ei poistettu eikä estoa kierretty. Raporttikansiossa on siirron jälkeen 109 490 tiedostoa ja 14 111 591 037 tavua (14,11 Gt / 13,14 GiB). Levystä ei vapautunut tilaa, koska 62 094 848 tavun palautuskopiot vain siirrettiin samalla levyllä. Tämä kooste on valmis raporttiaineiston poistamista varten; säilytettävät henkilökohtaiset kopiot ovat sen ulkopuolella.

### Jatkossa

Päivitä testin jälkeen tätä tiedostoa: päivämäärä, lähdeversio tai APK-tunniste, ympäristö, testattu toiminto, tulos ja merkittävä rajaus. Säilytä pysyvä raaka-aineisto vain erikseen nimetyn tarpeen vuoksi; siivoa omat välitulokset saman työn lopussa. `reports` saa syntyä tarkistustyökalujen tilapäiseksi tuloskansioksi, mutta sitä ei käytetä testihistorian pysyvänä arkistona. Tämä ei anna lupaa poistaa toisen keskeneräisen työn tiedostoja.

### Säilytetyt palautuskopiot

Sijainti: `.local/report-recovery-backups/`. Siirretty saman levyn sisällä; jokaisesta tarkistettiin kohteen olemassaolo ja tavumäärä sekä lähtöpolun poistuminen. Arkistojen sisältöä tai palautustoimivuutta ei testattu tässä. Yhteensä 29 tiedostoa, 62094848 tavua.

| Polku palautuskansiossa | Tavua |
| --- | --- |
| `audit-part07-runtime-20261013-2d790e33/normal-after.tar` | 991744 |
| `audit-part07-runtime-20261013-2d790e33/normal-before.tar` | 983552 |
| `audit-part07-runtime-20261013-2d790e33/normal-final.tar` | 1025024 |
| `audit-part07-runtime-20261013-2d790e33/notes-before.tar` | 4000768 |
| `audit-part07-runtime-20261013-2d790e33/notes-restored.tar` | 4000768 |
| `audit-part09-20261013/phone/after-com.finnvek.knittools.notestest.tar` | 3346944 |
| `audit-part09-20261013/phone/after-com.finnvek.knittools.tar` | 849920 |
| `audit-part09-20261013/phone/before-com.finnvek.knittools.notestest.tar` | 3334144 |
| `audit-part09-20261013/phone/before-com.finnvek.knittools.tar` | 849920 |
| `audit-part10-device-20261013/manual-database.tar` | 767488 |
| `audit-part10-device-20261013/notes-data-before.tar` | 3346944 |
| `audit-part10-device-20261013/notes-data-restored.tar` | 3346944 |
| `external-results-20261013-6d38bc4e/normal-before.tar` | 1033216 |
| `external-results-20261013-6d38bc4e/normal-restored.tar` | 1033216 |
| `external-results-20261013-6d38bc4e/notes-before.tar` | 4000768 |
| `external-results-20261013-6d38bc4e/notes-restored.tar` | 4000768 |
| `external-results-fix-20261013-91f24c6a/normal-after-install.tar` | 1049600 |
| `external-results-fix-20261013-91f24c6a/normal-before.tar` | 1041408 |
| `external-results-fix-20261013-91f24c6a/normal-final.tar` | 1049600 |
| `external-results-fix-20261013-91f24c6a/normal-restored.tar` | 1041408 |
| `external-results-fix-20261013-91f24c6a/notes-before.tar` | 4000768 |
| `external-results-fix-20261013-91f24c6a/notes-restored.tar` | 4000768 |
| `external-results-ui-fix-20261013-b84c19ea/normal-after-install.tar` | 1059328 |
| `external-results-ui-fix-20261013-b84c19ea/normal-before.tar` | 1059328 |
| `external-results-ui-fix-20261013-b84c19ea/normal-final.tar` | 1065984 |
| `external-results-ui-fix-20261013-b84c19ea/normal-restored.tar` | 1065984 |
| `external-results-ui-fix-20261013-b84c19ea/notes-before.tar` | 4000768 |
| `external-results-ui-fix-20261013-b84c19ea/notes-restored.tar` | 4000768 |
| `r10-live-20261023/phone-before-release-install.tar` | 747008 |
