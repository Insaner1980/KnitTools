# Suomen kieliauditointi (fi)

## Scope and snapshot

**AUDIT_PASS; native-speaker review NOT_REQUESTED.** B01–B23 on tarkistettu kokonaan nykyisten tekstien ja merkityskontekstien osalta. Päätös koskee suomen sisältöä LANGUAGE_AUDIT.md:n uudistetun kohdan 16 mukaisesti. Se ei ole kattava käyttöliittymän renderöintisertifiointi.

- Kieli: Suomen yleiskieli ja vakiintunut suomalainen neulonnan/virkkauksen sanasto. Ei alueellisen puhekielen standardointia.
- Rajaus: kaikki suomen translatable-resurssit, plural-variantit, alias, olennaiset eri kutsukontekstit ja koodista muodostuva sisältö; pysähdys suomen jälkeen.
- Aloitus ja viimeistely: 2026-10-19.
- Haara: `codex/audit-korjaukset-20261018`; HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. Ei commitia, pushia, PR:ää tai viestejä muihin keskusteluihin.
- Lähtötilassa oli runsaasti yhteisten korjausten muutoksia sekä valmiiksi muutoksia suomen kahdessa XML-tiedostossa. [entry-status.txt](entry-status.txt) ja [entry-worktree-hashes.tsv](entry-worktree-hashes.tsv) säilyttävät lähtötilan. Tässä tehtävässä lisättiin vain suomen korjaukset ja tämän hakemiston auditointitietueet.
- Suomen aiempia auditointitietueita ei ollut. Englannin coverage toimii avain-/kutsukohdehakemistona, **ei suomen hyväksyntänä**. Jokainen suomen teksti luettiin erikseen; yksikään englannin V-status ei siirtynyt suomen tulokseksi.
- Nykyinen sisältö: **1 349 käännettävää avainta, 1 380 suomen varianttia** (1 318 stringiä, 31 plural-avainta × one/other; ei array-resursseja), yksi string-alias. Lisäksi 2 yhteistä `translatable=false`-numerotekstiä (`42`, `10`) on tarkistettu käyttökontekstissaan.
- [coverage.tsv](coverage.tsv): **2 315 tietuetta** = 1 879 resurssi-/kutsukontekstitietuetta (1 382 erillistä varianttia, mukaan lukien nuo 2 numerotekstiä) + 436 kooditietuetta. Lisäkutsukonteksteja 497. Vanhan englannin yhteenvedon 1 382 ei siis tarkoita 1 382 suomen käännettävää varianttia.
- Kooditietueet: 275 viitetietoa (152 puikkosolua, 123 kokotaulukon mittaa), 85 tunnustekstiä (77 lyhennettä, 8 langan paksuusnimikettä), 44 symbolia (24 neulekaaviota, 20 hoitomerkkiä), 32 generoitua tulos-/koostumistapausta.
- [input-hashes.json](input-hashes.json) lukitsee nykyiset resurssit, yhteiset evidenssisyötteet, raportit ja katsotut kuvat. Kaikki englannin 449 aikaisempaa input-hashia täsmäsivät nykyisiin tiedostoihin. Tämä varmentaa evidenssin version, ei väitä kaikkien näiden tiedostojen itsenäistä täydellistä koodiauditointia.
- Omistajuus: vain `values-fi/strings.xml`, `values-fi/pattern_reading_assistance.xml` ja `config/language-audit/fi/`. `pattern_annotations.xml` luettiin kokonaan; ei korjaustarvetta.
- Ei uusia laiteajoja, kuvia, fixtureitä, per-screen-backlogia, 320 dp / 200 % / yhdistelmä- tai TalkBack-tarkistuksia. Suomi ei aiheuttanut nimettyä kielikysymystä, joka olisi jäänyt ratkaisematta lähteiden ja tuotantokutsujen lukemisella.

## Language decisions and glossary

| Käsite | Hyväksytty ilmaisu | Konteksti ja rajat | Evidence | Tila |
| --- | --- | --- | --- | --- |
| Knitting pattern / web pattern | ohje / verkko-ohje | Ohjekuva on ohjeesta otettu kuva. Mallikerta, mallineule, kuvio ja matemaattinen kaava ovat eri käsitteitä; ei sokkokorvauksia. | CLAUDE.md; FI01/FI02; tallennuspolut | PASS |
| Row / round | kerros / kierros | Valitsimessa Kerrokset ja Kierrokset erottavat ROWS/ROUNDS. Suljettuna neulotun kierroksen kutsuminen kerrokseksi muualla on myös vakiintunutta suomea, joten korrektit kerros-muodot säilyivät. | FI01/FI02; MainCounterDisplayText | PASS |
| Counter / position / repeat | laskuri, lukema, toisto; Kerros 1 vs 1 kerros | Omavalintainen nimi pysyy taivuttamattomana. Historiallinen lukema, muistutus, PDF-merkki ja kuvan lukema eivät todista kerrosyksikköä. | CounterValueFormatter; shared-fixes.md | PASS |
| Gauge / swatch | neuletiheys, silmukka-/kerrostiheys, mallitilkku | 10 cm ja 4 in eivät ole sama pituus. Pestyn/pingotetun tilkun tiheys on arvioinnin pohja, ei istuvuustakuu. | FI07; MeasurementCalculator | PASS |
| SSK / SSP / S2KP | oikein/nurin neulottava kavennus; keskitetty kaksoiskavennus | Yksitellen/yhdessä nostaminen sekä katselupuoli ovat ratkaisevia. | FI01/FI03/FI04 | PASS |
| M1 / M1L / M1R | silmukoiden välinen lisäys, vasemmalle/oikealle | Nostosuunnan ja neulottavan reunan on vastattava toisiaan; M1 ei ole KFB tai langankierto. | FI01/FI03; shared S05 | PASS |
| Yarn in front / back | työn edessä / takana suhteessa neulojaan | Ei sama asia kuin työn pysyvä nurja/oikea puoli. | FI01/FI02; shared S01 | PASS |
| Yarn weight / package / mass | paksuus; kerä/vyyhti; g | Kanoniset Lace/Fingering/Sport/DK/Worsted/Aran/Bulky/Super Bulky -luokat säilyvät. Ei luvata ply-vastaavuuksia. Kerä ja vyyhti ovat nykyisissä yhteyksissään ymmärrettäviä pakkausnimikkeitä, eivät uusi yhdenmukaistuskohde. | shared S09/S13; YarnUsagePresentation | PASS |
| Size / arm length | vartalon mitta; ranteesta kainaloon | Taulukko ei sisällä valmiin vaatteen väljyyttä. | SizeChartData; shared S04/S16-S21 | PASS |
| Care symbols | enimmäispesulämpötila °C, kemiallinen pesu P/F | P/F ovat prosessiluokkia. A on aineistossa säilyvä vanha kaikki liuottimet -merkki; sitä ei esitetä uuden standardin kattavana luokkaluettelona. Ei uusia numeerisia silityslämpötiloja. | FI05; shared S07/S08 | PASS |
| Numerot/yksiköt | 1 silmukka, 2 silmukkaa; m, yd, in, g; 50 % | Desimaalit paikallistetaan; prosenttimerkin välilyönti säilytetään. Jokainen one/other luettu erikseen; negatiivisessa lauseessa myös one voi olla partitiivi (1 projektia ei voitu siirtää). | FI06; tuotannon muotoilijat; RES-CHECK | PASS |
| Lyhenteet | englanninkieliset tunnukset, suomen selitykset | Hakemisto ja generaattori käyttävät tarkoituksella K/P/M1/K2tog jne. Laskurin ohjeeseen lisättiin sen käyttämien tunnusten selite. | AbbreviationData; InstructionParser; FI01/FI02 | PASS |

## Sources

Suomalaiset lähteet luettiin tässä tehtävässä. Yhteinen numeerinen/symbolievidenssi käytettiin uudelleen vain muuttumattomiin syötteisiin ja suomen merkitys tarkistettiin itsenäisesti. Lähteiden ohjeita ei kopioitu kokonaisina.

| ID | Lähde / julkaisija | URL | Kieli/alue; luettu | Tuettu väite ja raja |
| --- | --- | --- | --- | --- |
| FI01 | Neulonnan sanasto ja lyhenteet, Novita | https://novita.com/blogs/knitting-school/neulonnan-sanasto-ja-lyhenteet | fi/Suomi; 2026-10-19 | Kerros/silmukka, SSK/SSP, M1L/M1R ja nostotavan merkitys. Tuotesanasto ohje on lisäksi projektin oma vaatimus. |
| FI02 | Neulesanasto fi–US, DROPS/Garnstudio | https://www.garnstudio.com/glossary.php?lang=fi&langf=fi&langt=us | fi/US; 2026-10-19 | Kerros/row/round ja käsityötermien rinnastus; yksittäistä sanastotaulukkoa ei käytetty mekaanisena käännösauktoriteettina. |
| FI03 | Ylivetokavennus, SSK ja kaksi oikein yhteen, Piia Maria Knit | https://piiamariaknit.com/ylivetokavennus-ssk-ja-kaksi-oikein-yhteen-kavennus/ | fi/Suomi; 2026-10-19 | Itsenäinen SSK/SSP-tarkistus: nosto yksitellen ja kallistumisen näkökulma. |
| FI04 | Kolmen silmukan yhteen kaventaminen – 5 eri tapaa, Piia Maria Knit | https://piiamariaknit.com/kolmen-silmukan-yhteen-kaventaminen-5-eri-tapaa/ | fi/Suomi; 2026-10-19 | Keskitetyn kaksoiskavennuksen yhteinen nosto ja vasemmalle kallistuvan kaksoiskavennuksen eri menettely. |
| FI05 | Tekstiilien hoito-ohje 16.6.2025, STJM/Kiwa | https://www.stjm.fi/wp-content/uploads/2025/08/Tekstiilien-hoito-ohje_2025-06-16-v2-2.pdf | fi/Suomi; 2026-10-19 | Pesun enimmäislämpötila, P/F-prosessit. Lyhyitä selitteitä korjattiin ilman epätäydellistä liuotinlistaa. |
| FI06 | Prosenttimerkki, Kielitoimiston ohjepankki | https://kielitoimistonohjepankki.fi/ohje/prosenttimerkki/ | fi/Suomi; 2026-10-19 | Luvun ja prosenttimerkin välinen väli. Ei muutosta Androidin %%-muotoilusopimukseen. |
| FI07 | Mallitilkku ja neuletiheys, Novita | https://novita.com/blogs/faqs/mallitilkku-ja-neuletiheys | fi/Suomi; 2026-10-19 | Silmukat leveydestä, kerrokset korkeudesta, tiheys ja puikkokoko. Ei yleispätevää höyrytyslupausta kaikille kuiduille. |
| SHARED-FACTS | Nykyinen englannin raportti ja yhteiset korjauspäätökset | [en/review.md](../en/review.md), [shared-fixes.md](../shared-fixes.md) | en/fi; nykyisyys tarkistettu 2026-10-19 | S01-S06: CYC/Purl Soho; S07-S09: GINETEX/CYC; S10: Clover JP; S13-S15: valmistajan lanka/ehdollinen lisäkerä/UK; S16-S21: kuusi CYC-vartalotaulukkoa; S22: g-yksikön kirjainkoko. Tarkat alkuperäis-URL:t ja lähderajoitukset rekisterissä. Ei englannin kielituomion periytymistä. |

## Batch tracker

Omistetut ja tarkistetut tietueet ovat samoja nykyisen coverage.tsv:n rivejä, eivät ruutukaappausvelvoitteita. `T/F/L/C/R` kuvaa sisältöä; `V` on uusissa suomen riveissä tyhjä. TSV:n kennoissa tab ja rivinvaihto on paettu muodossa `\t` ja `\n`; XML:n kirjaimellinen Androidin `\n` säilyy sellaisenaan.

| Batch | Owned items | Reviewed items | Progress | Verdict | Open finding IDs | Evidence |
| --- | ---: | ---: | --- | --- | --- | --- |
| B01 | 110 | 110 | REVIEW_COMPLETE | PASS | — | READ-B01; coverage.tsv batch=B01 |
| B02 | 173 | 173 | REVIEW_COMPLETE | PASS | — | READ-B02; coverage.tsv batch=B02 |
| B03 | 115 | 115 | REVIEW_COMPLETE | PASS | — | READ-B03; coverage.tsv batch=B03 |
| B04 | 64 | 64 | REVIEW_COMPLETE | PASS | — | READ-B04; coverage.tsv batch=B04 |
| B05 | 160 | 160 | REVIEW_COMPLETE | PASS | — | READ-B05; coverage.tsv batch=B05 |
| B06 | 142 | 142 | REVIEW_COMPLETE | PASS | — | READ-B06; coverage.tsv batch=B06 |
| B07 | 78 | 78 | REVIEW_COMPLETE | PASS | — | READ-B07; coverage.tsv batch=B07 |
| B08 | 123 | 123 | REVIEW_COMPLETE | PASS | — | READ-B08; coverage.tsv batch=B08 |
| B09 | 89 | 89 | REVIEW_COMPLETE | PASS | — | READ-B09; coverage.tsv batch=B09 |
| B10 | 85 | 85 | REVIEW_COMPLETE | PASS | — | READ-B10; coverage.tsv batch=B10 |
| B11 | 80 | 80 | REVIEW_COMPLETE | PASS | — | READ-B11; coverage.tsv batch=B11 |
| B12 | 52 | 52 | REVIEW_COMPLETE | PASS | — | READ-B12; coverage.tsv batch=B12 |
| B13 | 107 | 107 | REVIEW_COMPLETE | PASS | — | READ-B13; coverage.tsv batch=B13 |
| B14 | 39 | 39 | REVIEW_COMPLETE | PASS | — | READ-B14; coverage.tsv batch=B14 |
| B15 | 25 | 25 | REVIEW_COMPLETE | PASS | — | READ-B15; coverage.tsv batch=B15 |
| B16 | 19 | 19 | REVIEW_COMPLETE | PASS | — | READ-B16; coverage.tsv batch=B16 |
| B17 | 377 | 377 | REVIEW_COMPLETE | PASS | — | READ-B17; coverage.tsv batch=B17 |
| B18 | 235 | 235 | REVIEW_COMPLETE | PASS | — | READ-B18; coverage.tsv batch=B18 |
| B19 | 83 | 83 | REVIEW_COMPLETE | PASS | — | READ-B19; coverage.tsv batch=B19 |
| B20 | 39 | 39 | REVIEW_COMPLETE | PASS | — | READ-B20; coverage.tsv batch=B20 |
| B21 | 30 | 30 | REVIEW_COMPLETE | PASS | — | READ-B21; coverage.tsv batch=B21 |
| B22 | 82 | 82 | REVIEW_COMPLETE | PASS | — | READ-B22; coverage.tsv batch=B22 |
| B23 | 8 | 8 | REVIEW_COMPLETE | PASS | — | READ-B23; coverage.tsv batch=B23 |

### Batch-kohtainen sisältöevidenssi

- **READ-B01:** Luettu yleiset toiminnot, navigaatio ja lukuihin liittyvät yksiköt. Otsikot ja toimintoverbit vastaavat kohdetta; numeerisissa yksikkökentissä myös 1 on mahdollinen. Tuotenimet säilyvät.
- **READ-B02:** Luettu projektin luonti, aktiivinen/valmis, kansiot, siirto, valinta, poisto ja virheet. Yksi aktiivinen ilmaisprojekti; valmistuneet eivät kuluta kiintiötä. Kansion poisto säilyttää projektit, projektin poisto ei. Nimiparametrit ovat nimiä, eivät taivutettavia yleisnimiä.
- **READ-B03:** Luettu pääluvun paikka ja määrä, neljä yksikkövalintaa, toistot, askel ja widget. Määrä käyttää yksikkötaivutusta; sijainti on Kerros 1. Omat nimet erotetaan taivutusta vaativista lauseista. Widgetin tavoite on yksikötön. Tavoitteesta puuttuva määrä ja tavoitteen ylitys ovat eri haarat.
- **READ-B04:** Luettu nimetyt, toisto-, muotoilu- ja osiolaskurit, linkitys ja muistutukset. Toistoväli on pääluvun väli eikä aina kerros. Muistutusnumero ja intervalli ovat eri argumentteja. Osiolaskurin alku/loppu ja linkitetty delta vastaavat ProjectCounterLogicin toimintaa.
- **READ-B05:** Luettu työskentelyjakson Start/Stop, palautuksen Lisää/Hylkää/Muokkaa, kesto, aikatiedot, historiarivit ja Insights. Nykyinen projektin yksikkö ei todista vanhan jakson yksikköä; historia käyttää neutraalia laskuriarvoa. Nykyinen korjattu aktiivisten päivien joukko ja aikaväli sekä valmistumistapahtumat tarkistettu yhteisen päätöksen ja suomen koostumien kannalta.
- **READ-B06:** Luettu lankakortin tiedot, linkitys, kuvat ja poistoseuraukset, kahdeksan kanonista paksuusnimikettä sekä kaikki 20 hoitomerkin selitettä. Paksuus ei ole grammapaino eikä ply-rakenne. P/F ovat pesuprosessimerkintöjä; pesulämpötila on enimmäisarvo Celsiusasteina.
- **READ-B07:** Luettu projektin lankamuistiinpano ja käytön suunniteltu/varattu/käytetty määrä, muunnokset ja virheet. Määrät ovat riippumattomia; tuntematon ei ole nolla. Ylitys on varatun ja käytetyn erotuksen itseisarvo; käyttö ei muuta yleistä varastoa. Kerä/vyyhti tarkoittaa tässä käyttäjän määrittelemää lankapakkausta.
- **READ-B08:** Luettu kirjaston PDF/verkko-ohje/Ravelry-metatieto, saatavuus, liitä/irrota/korvaa/poista ja ulkoinen avaus. Verkko-ohje ei lupaa offline-PDF:ää. Kirjaston poiston pääkerroksen merkinnät ja projektin omat PDF:t/merkinnät erotettu. Designer-nimen pituusvirheen alias ratkaistu suomen kohteeseen.
- **READ-B09:** Luettu saapuva PDF, SAF, ohjekuva/kamera/galleria, sivujärjestys, peruuttaminen ja tuontivirheet. Kuva ei ole skannauslupaus. Ohjekuva, ohje ja PDF pysyvät erillisinä. Maksimi- ja tiedostokokoväitteet verrattu nykyisiin tuontirajoihin; Ravelry-lataus johtaa samaan tallennusvalintaan.
- **READ-B10:** Luettu lukija, sivu, seuranta, pysty/vaaka-apuviiva, laskurimerkit, kalibrointi ja kirjanmerkit. Lukijan laskuripaikka ei aina ole kerros. Sivunumero on pageIndex+1; sijainti prosentteina on ylä-/vasemmasta reunasta. Manuaalinen siirto keskeyttää seurannan; kirjanmerkki ei ole PDF-merkintä.
- **READ-B11:** Luettu merkintätyökalut, värit, paksuudet, tasot, historia ja merkitty PDF -vienti. Kirjaston taso on projektissa vain luettava, projektin taso muokattava. Vienti luo uuden PDF:n ja säilyttää lähteen. Virhe/peruutus/onnistuminen ja sivun edistyminen erillisinä.
- **READ-B12:** Luettu projektin muistiinpanot, valokuvat, kuvien lukemapaikka, kuvatekstit ja poistot. Muistiinpanot ovat paikallisia; valokuvan lukema on neutraali. Käyttäjän projektin nimi, kuvateksti ja tiedostonimi säilyvät datana.
- **READ-B13:** Luettu mitta/tiheys/silmukka/kerros-muunnokset, syötteet, tulokset, varoitukset, kopiointi ja liittämisesimerkit. 4 in = 101,6 mm eikä 10 cm. Silmukka- ja kerrostiheys ovat erillisiä. Pyöristys on esitystä; laskettu silmukkamäärä ei takaa vaatteen istuvuutta. Tyhjän tuloksen viesti kattaa myös virheelliset täytetyt kentät.
- **READ-B14:** Luettu lisäys/kavennus tasossa ja suljettuna sekä generoitu K/M1/K2tog-ohje. Tarkistettu kulutetut silmukat ja nettomuutos sekä M1:n todellinen väli; N>=2, tasossa enintään N-1 lisäystä, suljettuna N. Suomen ohjeteksti selittää muuttumattomat tunnukset. Regex-esimerkkien englanti on hyväksyttävän syötteen kieli, ei kääntämättä jäänyt selitys.
- **READ-B15:** Luettu luontileveys, tiheys, mallikerta, reunasilmukat, vaihtoehdot ja toteutuva leveys. Leveys koskee runkoa; reunasilmukat lisätään sen päälle. Lähin mallikerran monikerta valitaan ennen reunojen lisäämistä; täsmätasatilanteessa pienempi positiivinen vaihtoehto.
- **READ-B16:** Luettu lankamäärän pituus, kerän pituus/paino, tarkka tarve ja kokonaisiksi keriksi pyöristys. 450/200 = 2,25 kerää, ostettava määrä 3 ja 50 g/kerä tuottaa 150 g kokonaisten kerien massan. Värjäyseräero perustelee harkinnan, ei aina yhtä lisäkerää.
- **READ-B17:** Luettu kaikki puikkotaulukon 152 solua ja kokotaulukoiden 123 mittatietuetta (58 vaihteluväliä) sekä niiden suomen otsikot/ryhmät. JP-mitoitus, US/UK-järjestelmät ja vartalomitat tarkistettu nykyisten yhteisten lähdepäätösten avulla. Ikä/ryhmä/XS-5XL ei ole valmisvaatteen mitoitus; tuumat johdetaan cm/2,54.
- **READ-B18:** Luettu kaikki 77 englanninkielistä lyhennetunnusta ja niiden suomen merkitys/selitys sekä hakukonteksti. SSK/SSP/S2KP/M1, nosto oikein/nurin, työstä katsottu puoli ja langan sijainti tarkistettu suomalaisista käsityölähteistä. Mallikuvio ja matemaattinen kaava eivät ole ohje-sanan mekaanisia korvauskohteita.
- **READ-B19:** Luettu kaikki 24 käytössä olevaa kaaviomerkkiä, suomen nimi/selitys ja piirtohaara. Yhteisen kuvan KFB/P2tog-korjaus uudelleentarkistettu olemassa olevista kuvista, ei uudella laiteajolla. Oikealta puolelta katsomisen konventio ja 2/2-/3/3-palmikoiden ylitys/suunta erotettu. Käyttämättömän nostomerkin resurssit on silti luettu.
- **READ-B20:** Luettu asetukset, kieli, teema, haptiikka, yksikkövalinta, analytiikkasuostumus ja tietosisältölupaukset. Mitat ja tiheys voi ohittaa yksikköoletuksen. PostHogin EU-osoite, opt-in-portti ja sisältörajoitukset tarkistettu koodista; ei analytiikkadatan lukua.
- **READ-B21:** Luettu varmuuskopion sisältö/rajat, vienti, esikatselun luvut/päivä, korvaava palautus ja virhetilat. Palautus korvaa nykyiset tietokantatiedot vahvistuksen jälkeen; viestit eivät lupaa yhdistelyä tai pilvisynkronointia. Nykyinen repository korvaa transaktion sisällä.
- **READ-B22:** Luettu Pro/kokeilu/hinta/osto/palautus/virhe/ominaisuusportit. Kokeilu on 14 päivää, debug-portti ei muuta maksavan asiakkaan tilaa. Vanha sisältö pysyy käytettävissä lupauksen mukaisesti; uuden sisällön portti erillinen. Hinta tulee palvelusta valmiiksi muotoiltuna. Muistutus ei ole yksinomaan rivimuistutus, ohjekuva ei ole mallikuva.
- **READ-B23:** Luettu jäljelle jäävä päivämäärä-, kesto-, numero-, tiedostonimi- ja tulostekoostumus, varianttien/aliaksen yhtäpitävyys sekä terminologiahaku. H:mm:ss on numerokello; tiedostonimen ISO-päivä on siirrettävä nimi. Käyttäjän nimet säilytetään. Ei uusia kovakoodattuja käännettäviä käyttöliittymälauseita tunnistettu.


### Olennaiset koostumien erot

- Tavoitteen puuttuva/ylittävä määrä: projektikortin argumentti on pelkkä luku; laskurin argumentti on taivutettu määrä tai oma nimi-arvo. Molemmilla on erilliset kutsukohdat TSV:ssä.
- Session/photo/reminder/PDF-merkinnän lukema on neutraali. Pääluvun oma valinta ROWS/ROUNDS/REPEATS/CUSTOM muuttaa varsinaista laskurin otsikkoa ja toimintotekstiä.
- Suomen one/other-lukemat: 1 kerros / 0 ja 2 kerrosta; kansiotoimintojen kielteinen 1 projektia; käyttömäärän 1 vyyhti / 1,5 vyyhtiä. Quantifier ja näytettävä parametri tarkistettiin kutsuista.
- DurationTextin tekstimuoto saa resurssin välit (1 h 1 min). DurationHero jakaa luvut ja yksiköt erikokoisiksi graafisiksi ajoiksi; tämä ei muuta keston arvoa tai yksikköä. Uutta suomen visuaalista tai puhuttua havaintoa ei väitetä.
- Insights B05-01:n muuttumaton yhteinen korjaus: suomeksi Aktiivisia päiviä 1 / 1, 2 / 2 ja 0 / 1. Kuukausi-/päiväalue tulee paikallisesta päivämäärämuotoilijasta ja korjatusta aikavälistä; ei tulevan kuukauden käänteistä väliä.
- PDF-virheiden samankaltaiset sanat eivät yhdistä eri seurauksia: kirjaston poisto, projektin irrotus, korvaaminen, merkintäkerroksen poisto ja fyysisen PDF:n vienti luettiin erillisinä.
- Parserin englanninkieliset lainausmerkkiesimerkit säilytettiin, koska regex tunnistaa ne; suomen toimintaohje ei lupaa vapaata suomenkielistä tulkintaa.
- Tiedostonimen KnitTools-backup-YYYY-MM-DD ja muuttumattomat lyhenteet eivät ole kääntämättä jäänyttä suomen käyttöliittymäproosaa. Valmis palveluhinta ja käyttäjän nimet ovat ulkoista dataa, eivät omistettuja käännöksiä.

## Findings

**65 muuttunutta suomen tekstiä.** Kaikki ovat alla yksilöityjä vahvistettuja virheitä; ei järjestelmällistä tyylinvaihtoa. Alkuperäinen tarkoittaa tämän tehtävän alussa ollutta tekstiä, ei Git HEADia, jotta aiemmat yhteiset korjaukset eivät kirjaudu tämän tehtävän saavutuksiksi.

Kaikissa seuraavissa: **Classification CONFIRMED_DEFECT; State VERIFIED**. Variantti on `value`; kaikki kyseisen avaimen kutsukontekstit ja tarkat tiedostoviitteet löytyvät coverage.tsv:n `finding_ids`-suodatuksella. Alkuperäinen/final on myös koneellisesti [corrections.json](corrections.json)-tietueessa. Ei jaetun koodin tai muiden kielten muutoksia. Korjattu teksti luettiin uudelleen; XML, paikanvaraajat ja AAPT2 läpäisivät; soveltuvat olemassa olevat resurssitestit läpäisivät. Ei jäljellä olevaa nimettyä epävarmuutta. Nämä yhteiset kentät koskevat jokaista alla olevaa finding-template-tietuetta.

### LA-fi-B01-01: `unit_inches`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: tuumaa
- Final: in
- User-visible consequence / evidence / smallest sufficient correction: Yksikkö liittyy myös lukuun 1 (esim. luontileveys); kiinteä partitiivi tuottaa muodon 1 tuumaa. Tunnus toimii kaikilla arvoilla.
- Evidence: CALLERS; RES-CHECK; READ-B01; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B16-01: `unit_meters`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: metriä
- Final: m
- User-visible consequence / evidence / smallest sufficient correction: Lankalaskurin pituuskenttä hyväksyy myös arvon 1; kiinteä metriä ei taivu sen kanssa. Yksikkötunnus säilyttää merkityksen.
- Evidence: YarnEstimator; YarnEstimatorScreen; SHARED-FACTS S14; READ-B16; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B16-02: `unit_yards`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: jaardia
- Final: yd
- User-visible consequence / evidence / smallest sufficient correction: Lankalaskurin pituuskenttä hyväksyy myös arvon 1; kiinteä jaardia ei taivu sen kanssa. Yksikkötunnus säilyttää merkityksen.
- Evidence: YarnEstimator; YarnEstimatorScreen; SHARED-FACTS S14; READ-B16; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-01: `main_counter_rows`

- Category **TERM**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Rivit
- Final: Kerrokset
- User-visible consequence / evidence / smallest sufficient correction: Neulonnan row on kerros; Rivit ei nimeä neulottavaa yksikköä oikein.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-02: `main_counter_rounds`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Kerrokset
- Final: Kierrokset
- User-visible consequence / evidence / smallest sufficient correction: Samassa valitsimessa ROWS ja ROUNDS ovat eri vaihtoehdot. Kun ROWS korjataan kerroksiksi, kierrokset säilyttää vaihtoehtojen ymmärrettävän eron.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-03: `main_counter_custom_count_format`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: %1$d %2$s
- Final: %2$s: %1$d
- User-visible consequence / evidence / smallest sufficient correction: Käyttäjän nimeä ei taivuteta: esimerkiksi 2 Palmikko on virheellinen. Nimi-arvo-rakenne toimii taivuttamatta vapaata nimeä.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-04: `counter_add_custom`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Lisää %1$s
- Final: Lisää: %1$s
- User-visible consequence / evidence / smallest sufficient correction: Vapaa nimi tulee nominatiivina; Lisää Palmikko voi tarkoittaa uuden laskurin luomista. Kaksoispiste erottaa lisäystoiminnon kohdenimestä.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-05: `counter_decrease_custom`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Vähennä %1$s
- Final: Vähennä: %1$s
- User-visible consequence / evidence / smallest sufficient correction: Vapaa nimi ei ole automaattisesti objektimuodossa. Kaksoispiste erottaa toiminnon taivuttamattomasta kohdenimestä.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-06: `counter_increase_named`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Lisää %1$s
- Final: Lisää: %1$s
- User-visible consequence / evidence / smallest sufficient correction: Parametri voi olla Silmukat tai käyttäjän nimi; Lisää Silmukat on väärin taipuva komento. Nimi erotetaan komennosta.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-07: `counter_decrease_named`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Vähennä %1$s
- Final: Vähennä: %1$s
- User-visible consequence / evidence / smallest sufficient correction: Parametri voi olla Silmukat tai käyttäjän nimi; Vähennä Silmukat on väärin taipuva komento. Nimi erotetaan komennosta.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-08: `counter_step_custom_title`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: %1$s kerrallaan
- Final: %1$s: määrä kerrallaan
- User-visible consequence / evidence / smallest sufficient correction: Vapaa nimi ei ole määrän yksikkömuoto: Palmikot kerrallaan ei kerro muokattavaa askelmäärää. Nimi erotetaan määrän otsikosta.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-09: `widget_name`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Kerroslaskuri
- Final: Projektilaskuri
- User-visible consequence / evidence / smallest sufficient correction: Widget käyttää projektin pääluvun arvoa, joka voi olla kierros, toisto tai oma yksikkö; nimi ei saa luvata vain kerroslaskuria.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-10: `widget_description`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Kerroslaskuriwidget aktiiviselle projektille
- Final: Laskuriwidget aktiiviselle projektille
- User-visible consequence / evidence / smallest sufficient correction: Widget tukee myös muita pääluvun yksiköitä kuin kerroksia; poistetaan väärä yksikkörajaus.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B03-11: `row_label_with_target`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Kerros %1$d / %2$d
- Final: %1$d / %2$d
- User-visible consequence / evidence / smallest sufficient correction: Widgetin tavoiterivi saa minkä tahansa pääluvun ja tavoitteen. Kerros-etuliite nimeää oman laskurin arvon väärin.
- Evidence: FI01; FI02; MainCounterDisplayText; CounterValueFormatter; CounterWidget; READ-B03; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B04-01: `repeat_every`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Toista joka
- Final: Toistoväli
- User-visible consequence / evidence / smallest sufficient correction: Muistutuskenttä yhdistää otsikon ja lukeman välein -päätteen: joka ... välein on kaksinkertainen rakenne. Lyhyt otsikko toimii myös lisälaskurin itsenäisenä kenttäotsikkona.
- Evidence: ReminderComponents; MultiCounterComponents; ProjectCounterLogic; READ-B04; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B05-01: `insights_empty_title`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Puikoilla ei ole vielä aikaa.
- Final: Työskentelyaikaa ei ole vielä tallennettu.
- User-visible consequence / evidence / smallest sufficient correction: Puikoilla ei ole vielä aikaa ei ilmaise suomeksi puuttuvia aikatietoja, ja puikot sulkevat virkkauksen pois.
- Evidence: InsightsSections; shared-fixes.md; en/review.md nykyinen B05-01-päätös; READ-B05; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B06-01: `care_dryclean_p`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: P-liuotin
- Final: Kemiallinen pesu (P)
- User-visible consequence / evidence / smallest sufficient correction: P on kemiallisen pesuprosessin luokka, ei P-niminen liuotin. Prosessimerkintä säilytetään ilman virheellistä aineväitettä.
- Evidence: FI05; CareSymbol; CareSymbolIcon; SHARED-FACTS S07-S09/S13; READ-B06; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B06-02: `care_dryclean_f`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: F-liuotin
- Final: Kemiallinen pesu (F)
- User-visible consequence / evidence / smallest sufficient correction: F on kemiallisen pesuprosessin luokka, ei F-niminen liuotin. Prosessimerkintä säilytetään ilman virheellistä aineväitettä.
- Evidence: FI05; CareSymbol; CareSymbolIcon; SHARED-FACTS S07-S09/S13; READ-B06; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B06-03: `care_wash_30`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: 30°
- Final: Pesu enintään 30 °C
- User-visible consequence / evidence / smallest sufficient correction: Hoitosymbolin tekstin pitää kertoa pesun enimmäislämpötila; 30° yksin jättää toiminnon, enimmäisrajan ja Celsius-yksikön sanomatta.
- Evidence: FI05; CareSymbol; CareSymbolIcon; SHARED-FACTS S07-S09/S13; READ-B06; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B06-04: `care_wash_40`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: 40°
- Final: Pesu enintään 40 °C
- User-visible consequence / evidence / smallest sufficient correction: Hoitosymbolin tekstin pitää kertoa pesun enimmäislämpötila; 40° yksin jättää toiminnon, enimmäisrajan ja Celsius-yksikön sanomatta.
- Evidence: FI05; CareSymbol; CareSymbolIcon; SHARED-FACTS S07-S09/S13; READ-B06; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B06-05: `care_wash_60`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: 60°
- Final: Pesu enintään 60 °C
- User-visible consequence / evidence / smallest sufficient correction: Hoitosymbolin tekstin pitää kertoa pesun enimmäislämpötila; 60° yksin jättää toiminnon, enimmäisrajan ja Celsius-yksikön sanomatta.
- Evidence: FI05; CareSymbol; CareSymbolIcon; SHARED-FACTS S07-S09/S13; READ-B06; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B06-06: `yarn_result_placeholder`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Täytä kentät, niin näet montako kerää tarvitset
- Final: Täytä kentät, niin näet, montako kerää tarvitset
- User-visible consequence / evidence / smallest sufficient correction: Epäsuora kysymys montako kerää tarvitset erotetaan päälauseesta pilkulla.
- Evidence: FI05; CareSymbol; CareSymbolIcon; SHARED-FACTS S07-S09/S13; READ-B06; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B07-01: `yarn_usage_scope`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Seuranta koskee vain tähän projektiin varattua lankaa. Omat langat -määrät eivät muutu.
- Final: Seuranta koskee vain tämän projektin lankaa. Omat langat -määrät eivät muutu.
- User-visible consequence / evidence / smallest sufficient correction: Suunnitellun tai käytetyn määrän voi kirjata ilman varattua määrää. Varattua rajaa toiminnon virheellisesti vain allokoituun lankaan.
- Evidence: ProjectYarnUsageRepository; YarnUsageCalculator; YarnUsagePresentation; READ-B07; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B10-01: `pattern_clear_page_marks`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Poista sivun rivimerkit
- Final: Poista sivun laskurimerkit
- User-visible consequence / evidence / smallest sufficient correction: Valikko poistaa pääluvun merkinnät myös toisto- ja omilla yksiköillä. Rivimerkit ei vastaa muiden nykyisten toimintojen neutraalia laskurimerkki-käsitettä.
- Evidence: FI06; PatternViewerScreen; pattern_reading_assistance.xml; READ-B10; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B13-01: `tool_result_placeholder`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Täytä kentät, niin näet tuloksen
- Final: Syötä kelvolliset arvot, niin näet tuloksen
- User-visible consequence / evidence / smallest sufficient correction: Sama tyhjä tulos näytetään myös virheellisillä syötteillä, kun kentät ovat täynnä. Pelkkä täyttämiskehotus ei selitä tarvittavaa korjausta.
- Evidence: GaugeViewModel; GaugePresentation; MeasurementCalculator; FI07; READ-B13; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B14-01: `increase_result_placeholder`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Täytä molemmat kentät, niin näet mihin lisäykset tehdään
- Final: Syötä kelvolliset silmukkamäärät, niin näet, mihin lisäykset tehdään
- User-visible consequence / evidence / smallest sufficient correction: Tyhjä tulos näkyy myös täytetyillä mutta virheellisillä syötteillä. Lisäksi mihin-sivulauseesta puuttuu pilkku.
- Evidence: FI01; FI03; IncreaseDecreaseCalculator; IncreaseDecreaseScreen; SHARED-FACTS S05; READ-B14; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B14-02: `decrease_result_placeholder`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Täytä molemmat kentät, niin näet mihin kavennukset tehdään
- Final: Syötä kelvolliset silmukkamäärät, niin näet, mihin kavennukset tehdään
- User-visible consequence / evidence / smallest sufficient correction: Tyhjä tulos näkyy myös täytetyillä mutta virheellisillä syötteillä. Lisäksi mihin-sivulauseesta puuttuu pilkku.
- Evidence: FI01; FI03; IncreaseDecreaseCalculator; IncreaseDecreaseScreen; SHARED-FACTS S05; READ-B14; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B14-03: `increase_decrease_error_not_enough_bars`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Silmukoiden välissä ei ole tarpeeksi välejä näille M1-lisäyksille. Vähennä lisäysten määrää.
- Final: Silmukoiden välissä ei ole tarpeeksi välejä näille M1-lisäyksille. Aloita vähintään 2 silmukasta ja vähennä tarvittaessa lisäysten määrää.
- User-visible consequence / evidence / smallest sufficient correction: Nykyinen laskin hylkää yhden lähtösilmukan myös pyöröneuleessa. Pelkkä lisäysten vähentäminen ei korjaa tapausta 1 + 1; kerrotaan myös vähimmäislähtömäärä.
- Evidence: FI01; FI03; IncreaseDecreaseCalculator; IncreaseDecreaseScreen; SHARED-FACTS S05; READ-B14; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B15-01: `desired_width`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Haluttu leveys
- Final: Haluttu leveys ilman reunasilmukoita
- User-visible consequence / evidence / smallest sufficient correction: CastOnCalculator laskee leveyden perusteella rungon silmukat ja lisää reunasilmukat sen jälkeen. Syötteen nimessä on erotettava rungon leveys kokonaisleveydestä.
- Evidence: CastOnCalculator; CastOnScreen; FI01; FI07; READ-B15; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B15-02: `tip_pattern_repeat_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Jos mallikuviosi toistuu joka X silmukan välein (esim. palmikko joka 8. silmukka), syötä luku tähän. Laskuri pyöristää rungon silmukat ennen reunasilmukoita lähimpään sopivaan monikertaan, jotta kuvio asettuu oikein.
- Final: Jos mallikertasi on X silmukan levyinen (esim. 8 silmukkaa), syötä luku tähän. Laskuri pyöristää rungon silmukat ennen reunasilmukoita lähimpään sopivaan monikertaan, jotta kuvio asettuu oikein.
- User-visible consequence / evidence / smallest sufficient correction: Laskuri tarvitsee mallikerran leveyden, ei toimintoa joka kahdeksannella silmukalla. Korvataan vain virheellinen syötteen selitys; laskentaselitys säilyy.
- Evidence: CastOnCalculator; CastOnScreen; FI01; FI07; READ-B15; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B15-03: `cast_on_result_placeholder`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Täytä leveys ja neuletiheys, niin näet montako silmukkaa luodaan
- Final: Täytä leveys ja neuletiheys, niin näet, montako silmukkaa luodaan
- User-visible consequence / evidence / smallest sufficient correction: Epäsuora montako-kysymys erotetaan päälauseesta pilkulla.
- Evidence: CastOnCalculator; CastOnScreen; FI01; FI07; READ-B15; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B16-03: `total_weight`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Kokonaispaino: %s g
- Final: Kokonaisten kerien paino: %s g
- User-visible consequence / evidence / smallest sufficient correction: Arvo on ceil(tarve/keräpituus) kertaa kerän paino, ei käytettävän langan paino. Otsikko kertoo kokonaisina hankittavien kerien massan.
- Evidence: YarnEstimator; YarnEstimatorScreen; SHARED-FACTS S14; READ-B16; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B16-04: `extra_skein_note`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Osta aina yksi ylimääräinen kerä — värjäyserät voivat vaihdella
- Final: Harkitse ylimääräistä kerää — värjäyserät voivat vaihdella
- User-visible consequence / evidence / smallest sufficient correction: Värjäyserävaihtelu ei perustele ehdotonta aina yksi ylimääräinen kerä -ostokehotusta kaikenkokoisille töille. Säilytetään perusteltu harkintaneuvo.
- Evidence: YarnEstimator; YarnEstimatorScreen; SHARED-FACTS S14; READ-B16; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B17-01: `desc_size_charts`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Vaatteiden standardimitoitukset kokojen tueksi
- Final: Vartalon standardimitat vaatekoon valinnan tueksi
- User-visible consequence / evidence / smallest sufficient correction: Taulukot sisältävät vartalon mitat, eivät valmiin vaatteen mitoitusta tai väljyyttä. Tämä ero vaikuttaa koon valintaan.
- Evidence: SizeChartData; NeedleSizeData; SizeChartScreen; SHARED-FACTS S03/S04/S10/S15-S21; READ-B17; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B17-02: `size_col_arm`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Hihan pituus
- Final: Ranteesta kainaloon
- User-visible consequence / evidence / smallest sufficient correction: CYC:n arm length on ranteesta kainaloon, ei valmiin vaatteen koko hihan pituus. Otsikko nimeää taulukon mitan.
- Evidence: SizeChartData; NeedleSizeData; SizeChartScreen; SHARED-FACTS S03/S04/S10/S15-S21; READ-B17; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-01: `desc_abbreviations`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Yleisimpien neulelyhenteiden hakemisto
- Final: Yleisimpien englanninkielisten neulelyhenteiden hakemisto
- User-visible consequence / evidence / smallest sufficient correction: Hakemiston muuttumattomat K/P/BO jne. ovat englanninkielisiä tunnuksia, eivät suomalaisia o/n/päät.-lyhenteitä. Suomen selitykset eivät muuta tunnusten kieltä.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-02: `abbr_ssk_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Vasemmalle kallistuva kavennus: nosta 2 silmukkaa oikein, neulo ne yhteen takareunoista
- Final: Vasemmalle kallistuva kavennus: nosta 2 silmukkaa yksitellen oikein neulomatta, neulo ne takareunoista oikein yhteen
- User-visible consequence / evidence / smallest sufficient correction: SSK:n kaksi silmukkaa nostetaan yksitellen oikein neulomatta. Yhteinen nosto tai oikein neulominen tuottaa eri rakenteen; ohje täsmennetään.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-03: `abbr_ssp_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Vasemmalle kallistuva kavennus nurjan puolen kerroksella
- Final: Nurin neulottava kavennus, joka kallistuu vasemmalle työn oikealta puolelta katsottuna
- User-visible consequence / evidence / smallest sufficient correction: Kallistumisen suunta riippuu katselupuolesta; SSP ei ole rajattu nurjan puolen kerroksille. Ilmaistaan oikea näkökulma.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-04: `abbr_s2kp_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Keskitetty kaksoiskavennus
- Final: Keskitetty kaksoiskavennus: nosta 2 silmukkaa yhdessä oikein neulomatta, neulo 1 oikein ja vedä nostetut silmukat neulotun yli
- User-visible consequence / evidence / smallest sufficient correction: Nosto on tehtävä yhdessä kuin oikein yhteen neulottaessa. Pelkkä nosta 2 -merkitys ja keskitetty-nimi eivät erota tätä yksitellen nostosta.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-05: `abbr_m1_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Poimi silmukoiden välinen lanka ja neulo takareunasta lisätäksesi silmukan
- Final: Poimi silmukoiden välinen lanka etukautta vasemmalle puikolle ja neulo se takareunasta oikein lisätäksesi silmukan
- User-visible consequence / evidence / smallest sufficient correction: Takareunasta neulominen kiertää lisäyksen vain kun nostosuunta vastaa sitä. Lisätään etukautta nosto ja oikein neulominen.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-06: `abbr_m1l_meaning`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Lisää 1 vasen
- Final: Lisää 1 vasemmalle
- User-visible consequence / evidence / smallest sufficient correction: Vasen ei ilmaise lisäyksen suuntaa kieliopillisesti oikein. Korjataan vain suuntasana.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-07: `abbr_m1r_meaning`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Lisää 1 oikea
- Final: Lisää 1 oikealle
- User-visible consequence / evidence / smallest sufficient correction: Oikea ei ilmaise lisäyksen suuntaa kieliopillisesti oikein. Korjataan vain suuntasana.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-08: `abbr_bo_desc`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Päätä silmukat niin ettei työ aukea
- Final: Päätä silmukat niin, ettei työ aukea
- User-visible consequence / evidence / smallest sufficient correction: Että-sivulauseen raja tarvitsee pilkun.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-09: `abbr_eor_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Neulotaan vain joka toisella kerroksella
- Final: Tee ohjeessa mainittu toimenpide joka toisella kerroksella
- User-visible consequence / evidence / smallest sufficient correction: Every other row koskee määrättyä toimenpidettä; välissä olevat kerrokset neulotaan myös. Neulotaan vain voi ohjata jättämään ne tekemättä.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-10: `abbr_dpn_desc`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Lyhyet puikot joissa on kärjet molemmissa päissä, pienen ympärysmitan neulontaan
- Final: Lyhyet puikot, joissa on kärjet molemmissa päissä, pienen ympärysmitan neulontaan
- User-visible consequence / evidence / smallest sufficient correction: Relatiivilauseen aloittava pilkku puuttuu.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-11: `abbr_wyif_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Pidä lanka työn etupuolella (nurjalla puolella)
- Final: Pidä lanka työn edessä itseesi päin
- User-visible consequence / evidence / smallest sufficient correction: Työn edessä tarkoittaa neulojaan päin, ei pysyvästi työn nurjaa puolta; alkuperäinen sulje on väärä oikean puolen kerroksilla.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-12: `abbr_wyib_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Pidä lanka työn takapuolella (oikealla puolella)
- Final: Pidä lanka työn takana itsestäsi poispäin
- User-visible consequence / evidence / smallest sufficient correction: Työn takana tarkoittaa neulojasta poispäin, ei pysyvästi työn oikeaa puolta; alkuperäinen sulje on väärä oikean puolen kerroksilla.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B18-13: `abbr_rem_desc`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Silmukat jotka puikolla on jäljellä
- Final: Silmukat, jotka puikolla on jäljellä
- User-visible consequence / evidence / smallest sufficient correction: Relatiivilauseen aloittava pilkku puuttuu.
- Evidence: FI01; FI02; FI03; FI04; AbbreviationData; READ-B18; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B19-01: `chart_symbols_disclaimer`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Merkit noudattavat yleisiä käytäntöjä. Tarkista aina oman ohjeesi merkkiselite.
- Final: Merkit kuvaavat silmukoita työn oikealta puolelta katsottuna. Tarkista aina oman ohjeesi merkkiselite.
- User-visible consequence / evidence / smallest sufficient correction: Kaavion silmukkamerkit on tulkittava oikealta puolelta katsottuna; nurjalla puolella neulottava silmukka voi vaihtua. Yleisiä käytäntöjä ei ilmaise tätä oleellista näkökulmaa.
- Evidence: FI01; FI03; FI04; ChartSymbolData; ChartSymbolScreen; SHARED-FACTS S02; READ-B19; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B19-02: `sym_ssk_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Nosta, nosta, neulo — vasemmalle kallistuva kavennus
- Final: Nosta 2 silmukkaa yksitellen oikein neulomatta ja neulo ne takareunoista oikein yhteen — vasemmalle kallistuva kavennus
- User-visible consequence / evidence / smallest sufficient correction: Nosta, nosta, neulo ei kerro neulo yhteen -kavennusta eikä nostotapaa. Erillisen symbolikortin toimintaohje täsmennetään SSK:n mukaiseksi.
- Evidence: FI01; FI03; FI04; ChartSymbolData; ChartSymbolScreen; SHARED-FACTS S02; READ-B19; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B19-03: `sym_sk2p_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Nosta 1, K2tog, vedä nostettu yli
- Final: Nosta 1 silmukka oikein neulomatta, neulo 2 oikein yhteen ja vedä nostettu silmukka neulotun yli
- User-visible consequence / evidence / smallest sufficient correction: Suomenkielisen symboliohjeen K2tog jää selittämättä ja nostotapa puuttuu. Kirjoitetaan kyseiset vaiheet suomeksi.
- Evidence: FI01; FI03; FI04; ChartSymbolData; ChartSymbolScreen; SHARED-FACTS S02; READ-B19; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B19-04: `sym_kfb_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Neulo etu- ja takareunasta — lisäys
- Final: Neulo etu- ja takareunasta oikein — lisäys
- User-visible consequence / evidence / smallest sufficient correction: KFB on oikein neulottava lisäys; ilman oikein-sanaa kuvaus sopisi myös erilaiseen PFB-lisäykseen.
- Evidence: FI01; FI03; FI04; ChartSymbolData; ChartSymbolScreen; SHARED-FACTS S02; READ-B19; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B19-05: `sym_no_stitch_desc`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Harmaa laatikko — muotoilukohta jossa ei ole silmukkaa
- Final: Harmaa laatikko — muotoilukohta, jossa ei ole silmukkaa
- User-visible consequence / evidence / smallest sufficient correction: Relatiivilauseen aloittava pilkku puuttuu.
- Evidence: FI01; FI03; FI04; ChartSymbolData; ChartSymbolScreen; SHARED-FACTS S02; READ-B19; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B20-01: `tip_imperial_units_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Kun asetus on käytössä, kaikki laskurit käyttävät senttimetrien sijasta tuumia ja metrien sijasta jaardeja. Tiheys ilmoitetaan silloin 4 tuumaa kohti 10 cm:n sijaan.
- Final: Kun asetus on käytössä, pituuslaskureiden oletusyksikköinä ovat tuumat ja jaardit senttimetrien ja metrien sijaan. Tiheyden oletus on silloin 4 tuumaa kohti 10 cm:n sijaan. Mitat ja tiheys -työkalussa voit vaihtaa yksiköitä erikseen.
- User-visible consequence / evidence / smallest sufficient correction: Mitat ja tiheys sallii erilliset yksikkövalinnat. Asetus määrittää oletukset eikä pakota kaikkia laskureita samoihin yksiköihin.
- Evidence: GaugeViewModel; PreferencesManager; READ-B20; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B22-01: `pro_prompt_reminders_title`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Lisää rivimuistutus Prolla
- Final: Lisää muistutus Prolla
- User-visible consequence / evidence / smallest sufficient correction: Muistutuksen laukaisee pääluvun arvo myös omilla yksiköillä. Rivi rajaa toiminnon väärin.
- Evidence: ProFeature; ProState; PatternImageImportViewModel; reminder callers; READ-B22; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B22-02: `pro_prompt_reminders_body`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Uuden rivimuistutuksen luominen vaatii Pron. Nykyiset muistutuksesi toimivat edelleen ja säilyvät muokattavina.
- Final: Uuden muistutuksen luominen vaatii Pron. Nykyiset muistutuksesi toimivat edelleen ja säilyvät muokattavina.
- User-visible consequence / evidence / smallest sufficient correction: Muistutuksen laukaisee pääluvun arvo myös omilla yksiköillä. Poistetaan vain väärä yksikkörajaus.
- Evidence: ProFeature; ProState; PatternImageImportViewModel; reminder callers; READ-B22; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B22-03: `pro_prompt_pattern_camera_title`

- Category **TERM**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Muunna mallikuvat PDF:ksi Prolla
- Final: Muunna ohjekuvat PDF:ksi Prolla
- User-visible consequence / evidence / smallest sufficient correction: Pattern photo tarkoittaa ohjeesta otettua kuvaa; mallikuva voi tarkoittaa työn esimerkkikuvaa. Sovelletaan tuotteen ohje-sanastoa.
- Evidence: ProFeature; ProState; PatternImageImportViewModel; reminder callers; READ-B22; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B22-04: `pro_prompt_pattern_camera_body`

- Category **TERM**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Uuden PDF:n luominen mallikuvista vaatii Pron. Nykyiset mallisi ja PDF-tiedostosi säilyvät käytettävissä.
- Final: Uuden PDF:n luominen ohjekuvista vaatii Pron. Nykyiset ohjeesi ja PDF-tiedostosi säilyvät käytettävissä.
- User-visible consequence / evidence / smallest sufficient correction: Pattern photo ja saved pattern tarkoittavat tässä ohjekuvaa ja ohjetta, eivät mallikuvia tai käyttäjän malleja.
- Evidence: ProFeature; ProState; PatternImageImportViewModel; reminder callers; READ-B22; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B22-05: `pro_group_workflow_body`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Käytä pientä toistolaskuria, lisä-, toisto-, muotoilu- ja osiolaskureita, rivimuistutuksia, mallikuvia PDF:nä sekä aloitusnäytön pienoissovellusta.
- Final: Käytä pientä toistolaskuria, lisä-, toisto-, muotoilu- ja osiolaskureita, muistutuksia, ohjekuvia PDF:nä sekä aloitusnäytön pienoissovellusta.
- User-visible consequence / evidence / smallest sufficient correction: Rivimuistutus rajaa yksikön väärin; mallikuva ei nimeä ohjeesta tehtävää PDF-tuontia. Muut ominaisuuslupaukset säilyvät.
- Evidence: ProFeature; ProState; PatternImageImportViewModel; reminder callers; READ-B22; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B10-02: `pattern_reading_line_position_state`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/pattern_reading_assistance.xml`.
- Original: %1$d%% yläreunasta
- Final: %1$d %% yläreunasta
- User-visible consequence / evidence / smallest sufficient correction: Suomen prosenttiluvun ja prosenttimerkin välistä puuttuu välilyönti. Muotoilun %% säilyy kirjaimellisen merkin tuottamiseksi.
- Evidence: FI06; PatternViewerScreen; pattern_reading_assistance.xml; READ-B10; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B10-03: `pattern_vertical_guide_position_state`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/pattern_reading_assistance.xml`.
- Original: %1$d%% vasemmasta reunasta
- Final: %1$d %% vasemmasta reunasta
- User-visible consequence / evidence / smallest sufficient correction: Suomen prosenttiluvun ja prosenttimerkin välistä puuttuu välilyönti. Muotoilun %% säilyy kirjaimellisen merkin tuottamiseksi.
- Evidence: FI06; PatternViewerScreen; pattern_reading_assistance.xml; READ-B10; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B10-04: `pattern_reading_line_following_state`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/pattern_reading_assistance.xml`.
- Original: %1$d%% yläreunasta. Seurataan päälaskuria.
- Final: %1$d %% yläreunasta. Seurataan päälaskuria.
- User-visible consequence / evidence / smallest sufficient correction: Suomen prosenttiluvun ja prosenttimerkin välistä puuttuu välilyönti. Muotoilun %% säilyy kirjaimellisen merkin tuottamiseksi.
- Evidence: FI06; PatternViewerScreen; pattern_reading_assistance.xml; READ-B10; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B10-05: `pattern_reading_line_manual_state`

- Category **LANGUAGE**, severity **S3**. File: `app/src/main/res/values-fi/pattern_reading_assistance.xml`.
- Original: %1$d%% yläreunasta. Laskurin seuranta keskeytetty.
- Final: %1$d %% yläreunasta. Laskurin seuranta keskeytetty.
- User-visible consequence / evidence / smallest sufficient correction: Suomen prosenttiluvun ja prosenttimerkin välistä puuttuu välilyönti. Muotoilun %% säilyy kirjaimellisen merkin tuottamiseksi.
- Evidence: FI06; PatternViewerScreen; pattern_reading_assistance.xml; READ-B10; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B17-03: `tip_needle_sizes_desc`

- Category **FACT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Metrinen koko (mm) on yleinen standardi. US-koot käyttävät numeroasteikkoa (0–50). UK- ja kanadalaiset koot käyttävät eri numerojärjestelmää. Japanilaisissa kooissa on oma numerointi. Varmista aina koko millimetreinä.
- Final: Metrinen koko (mm) on yleinen standardi. US-koot käyttävät numeroasteikkoa. UK- ja kanadalaiset koot käyttävät eri numerojärjestelmää. Japanilaisissa kooissa on oma numerointi. Varmista aina koko millimetreinä.
- User-visible consequence / evidence / smallest sufficient correction: US-puikkojen numerointi ei rajoitu välille 0–50 (esim. 000 ja 70). Poistetaan vain virheellinen yleispätevä vaihteluväli.
- Evidence: SizeChartData; NeedleSizeData; SizeChartScreen; SHARED-FACTS S03/S04/S10/S15-S21; READ-B17; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.

### LA-fi-B14-04: `tip_flat_vs_circular_desc`

- Category **CONTEXT**, severity **S2**. File: `app/src/main/res/values-fi/strings.xml`.
- Original: Tasoneule: neulot edestakaisin suorilla tai pyöröpuikoilla ja käännät työn jokaisen kerroksen lopussa.\n\nPyöröneule: neulot jatkuvaa kierrosta. Lisäykset ja kavennukset jaetaan tasaisesti koko kierrokselle.
- Final: Tasoneule: neulot edestakaisin suorilla tai pyöröpuikoilla ja käännät työn jokaisen kerroksen lopussa.\n\nPyöröneule: neulot jatkuvaa kierrosta. Lisäykset ja kavennukset jaetaan tasaisesti koko kierrokselle.\n\nLaskurin merkinnät: K = neulo oikein, M1 = lisää silmukoiden välisestä langasta kiertäen, K2tog = neulo 2 oikein yhteen, × = toista.
- User-visible consequence / evidence / smallest sufficient correction: Laskurin muodostama ohje käyttää kiinteitä englanninkielisiä K/M1/K2tog-tunnuksia. Suomen näkymästä puuttuu niiden selitys; lisätään selite olemassa olevaan ohjetekstiin.
- Evidence: FI01; FI03; IncreaseDecreaseCalculator; IncreaseDecreaseScreen; SHARED-FACTS S05; READ-B14; CALLERS; RES-CHECK; BUILD/TEST. Sama nimetty virhe ja pienin korjaus koskevat kaikkia yllä määriteltyjä variantin kutsukonteksteja.


### Säilytetty korrektina, ei korjausta

`n_selected` (%d valittu) on ymmärrettävä elliptinen käyttöliittymäilmaus. Kerros/kierros, tasoneule/pyöröneule ja kerä/vyyhti eivät vaadi koko aineiston yhdenmukaistamista. `abbr_patt_meaning` tarkoittaa mallikuviota, joten sen vaihtaminen sokkona ohjeeksi muuttaisi merkitystä. Käyttämättömät mutta käännettävät resurssit ovat kattavuudessa mukana; käyttämättömyys ei ole syy väittää uutta renderöintitulosta.

## Shared handoffs and other-language impact

**Ei uutta vahvistettua yhteisen toteutuksen korjaustarvetta eikä suomen hyväksyntää estävää riippuvuutta.** Aiemmat yhteiset havainnot tarkistettiin suomen kannalta: neutraalit laskuriyksiköt; widgetin tiivis luku; poistettu epävarma ply; hoitomerkit/selitteet; M1:n väli- ja reunasäännöt; luontilaskurin pyöristys; g-kirjainkoko; JP-mitat; vartalotaulukot; kaaviosymbolit; historiallinen yksikkö ja Insightsin päiväväli. Suomen vielä virheellinen widget- ja muistutussanasto korjattiin omista resursseista.

- Yhteiset merkityssyötteet: `CounterWorkspaceSections.kt`, `CounterScreen.kt`, `InsightsSections.kt`, `InsightsViewModel.kt`, `InsightsSessionAccumulator.kt`, `CounterWidget.kt`, `IncreaseDecreaseCalculator.kt`, `CastOnCalculator.kt`, `NeedleSizeData.kt`, `SizeChartData.kt`, `ChartSymbolData.kt`, `ChartSymbolScreen.kt`, `CareSymbolIcon.kt`, `YarnEstimatorScreen.kt`, `StandardYarnWeight.kt`. Tarkat nykyiset hashit input-hashes.jsonissa.
- Uudelleen käytetty nykyinen evidenssi: yhteisten korjausten laskenta-/testirekisterit ja englannin nykyinen B05-01-päätös. Niiden ajoja ei kirjata tämän tehtävän uusiksi testeiksi.
- KFB/P2tog-kuvat `reports/language-audit/shared/20261018-followup/shared-corrected-KFB.png` ja `shared-corrected-P2tog.png` katsottiin nyt. Ne vahvistavat kielestä riippumattoman piirrosmerkityksen yhteisen korjauksen; eivät suomen tekstin rivitystä. Nykyiset piirtohaarat vastaavat niitä.
- Alkuperäisten historiallisten OPEN/RUNTIME_PENDING-kirjausten ja LANGUAGE_AUDIT.md:n vanhemman aloitusyhteenvedon rinnalla on uudempi nimenomainen nykyinen päätös. Historiallisia rivejä ei muutettu tai hävitetty.
- Muissa kielissä voi olla vastaavia käännösvirheitä, mutta niitä ei päätelty tai korjattu suomen tuloksen perusteella. Ei viestiä toiseen keskusteluun tai yhteisen rekisterin muokkausta.

## Validation log

| Päivä | Syöte | Tarkistus | Todellinen tulos / raja |
| --- | --- | --- | --- |
| 2026-10-19 | Nykyinen fi + default XML | RES-CHECK: XML-parsinta, avain-/tyyppipariteetti, kaikki plural-variantit, alias, argumentti-indeksit/tyypit ja %% | PASS: 1 349 avainta / 1 380 fi-varianttia, 31 plurals, 1 alias, ei puuttuvaa/ylimääräistä tai paikanvaraajavirhettä. Pelkkä pariteetti ei hyväksy kieltä. |
| 2026-10-19 | input-hashes.json | 449 aikaisemman yhteisen evidenssisyötteen SHA-256 + nyt luetut raportit/fi-resurssit | PASS; uudelleenkäytettyjä ajoja ei tehty uudelleen. |
| 2026-10-19 | Korjatut fi-resurssit | `./gradlew.bat --no-daemon :app:processDebugResources :app:testDebugUnitTest --tests com.finnvek.knittools.ReferenceResourceLocalizationTest --tests com.finnvek.knittools.ui.CountPluralResourcesSourceTest --tests com.finnvek.knittools.PatternReadingAssistanceLocalizationTest --tests com.finnvek.knittools.ui.screens.counter.CounterStringResourcesSourceTest` | **BUILD SUCCESSFUL, 1 min 14 s; 11 testiä / 4 luokkaa, 0 failure/error/skipped.** Osa käännöstehtävistä UP-TO-DATE; resurssiprosessointi ja testitehtävä suoritettiin. |
| 2026-10-19 | Kattavuus + lopullinen diff | Inventaarion täsmäytys; 65 ennen/jälkeen-korjauksen käänteinen hash-tarkistus lähtötilaan; muiden tiedostojen säilyminen; git diff --check | PASS, katso validation.jsonin final_reconciliation. Lähtölistasta Gitin polkulainauksen vuoksi puuttunut vanha ui-insights-päivitys.md varmistettiin erikseen tavuiltaan HEADin mukaiseksi. |
| 2026-10-19 | Prosessit | Käynnistetty vain yllä oleva päättyvä Gradle-komento, --no-daemon | Komento päättyi koodiin 0; kertakäyttödaemoni sammui. Ei käynnistettyä palvelinta, emulaattoria, watcheria tai muuta jätettyä taustatyötä. |

[validation.json](validation.json) säilyttää suite-/metodikohtaiset tulokset, testitulosten hashit ja kattavuusluvut. [validation-gradle.log](validation-gradle.log) sisältää ajolokin. Gradle raportoi deprekaatioita tulevaa Gradle 10:tä varten; ne eivät olleet tämän ajon virheitä eikä työkaluketjua muutettu.

## Native-speaker review

- **NOT_REQUESTED.** Tässä tehtävässä tehtiin kirjattu kielellinen sisältöarvio; ei erillistä ihmisen äidinkieliarviota tai käsityöasiantuntijan hyväksyntää.
- Ei keksittyä arvioijaa, pätevyyttä tai suoritettua puhe-/TalkBack-testiä.

## Closeout

- B01–B23: **REVIEW_COMPLETE / PASS**. Kaikki 1 380 suomen tekstivarianttia sekä 2 yhteistä numerotekstiä, olennaiset eri kutsukontekstit ja 436 kooditietuetta on käsitelty. Peitto 2 315/2 315 tietuetta.
- **65 suomen tekstiä korjattu**; syyt yllä, kaikki lopullisessa versiossa VERIFIED. Toimivaa sanamuotoa ei vaihdettu tyylivaihtoehdon vuoksi.
- Ei vahvistettua avointa sisältövirhettä, nimettyä ratkaisematonta kieli-/ymmärryskysymystä tai yhteistä estävää riippuvuutta.
- V-sarake on tyhjä, koska uusia suomen renderöintihavaintoja ei tehty. Englannin pysäytettyä kampanjaa tai sen rekistereitä ei jatkettu. Puuttuvat kuvakaappaukset eivät yksin estä kohdan 16 mukaista hyväksyntää.
- **Suomen päätös: AUDIT_PASS; native review NOT_REQUESTED.** Päätös perustuu täyteen nykyiseen sisältöpeittoon, itsenäiseen suomen merkitys-/kieliarvioon, lähdetarkistuksiin, lopullisten korjausten validointiin sekä avoimien konkreettisten kysymysten puuttumiseen.

## Resume point

- Viimeinen valmis erä: B23, inventaarion täsmäytys ja lopullinen hyväksyntä.
- Keskeneräinen suomen sisältötyö: ei ole.
- Tämän tehtävän seuraavat työt: ei ole. Pysähdys suomen jälkeen.
- Uusi sisältömuutos vaatii vain muuttuneen avaimen/variantin ja merkitykseen vaikuttavan kutsun uudelleentarkistuksen. Ei määräystä aloittaa seuraavaa kieltä tai renderöintikampanjaa.
