# Swedish Language Audit (sv)

## Scope and snapshot

- Language and selected variety: yleisruotsi, Ruotsin käsityöterminologia. Hyväksyttävät vaihtoehdot säilytetään; ei uutta tyylistandardia.
- Assigned boundary: koko B01–B23, ruotsin resurssit, kaikki variantit, muodostettu teksti ja olennaisesti erilaiset merkityskontekstit. Pysähdys ruotsin hyväksyntään.
- Start date / last update: 2026-10-19 (Europe/Helsinki).
- Branch / HEAD: `codex/audit-korjaukset-20261018` / `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- Prior records: ei aiempaa sv-auditointihakemistoa. Kaikki ruotsin lukutyö tehtiin tässä tehtävässä. Englannin ja suomen AUDIT_PASS ei hyväksy ruotsin käännöksiä.
- Existing work: työpuu oli laajasti muutettu, myös kaikki kolme nyt muokattua ruotsin tiedostoa. [entry-status.txt](entry-status.txt) ja [entry-worktree-hashes.json](entry-worktree-hashes.json) säilyttävät aloitustilan. Indeksi oli ja pysyi tyhjänä. Ei commitia, pushia, PR:ää tai viestejä muihin keskusteluihin.
- Current version: [input-hashes.json](input-hashes.json) yksilöi lopullisen työpuun 468 syötetiedostoa. Pelkkä HEAD ei kuvaa auditoitua versiota. Manifesti sisältää yhteisen 449 syötteen vertailurajauksen; siinä oleva testi ei tarkoita tässä tehtävässä ajettua testiä.
- Actual resource inventory: **1349 käännettävää avainta / 1380 tekstivarianttia**: 1318 string-avainta ja 31 plural-avainta (62 erikseen luettua one/other-varianttia), 0 array-avainta, yksi string-alias. Kaikki neljä sv-XML:ää luettiin; `pattern_annotations.xml` ei tarvinnut muutoksia.
- Coverage: [coverage.tsv](coverage.tsv), **1826 riviä** = 1380 sv-varianttia + 2 kääntämätöntä numerovakiota + 275 taulukkosolua + 85 kooditunnusta + 44 symbolia + 40 erilaista koostamis-/merkityskontekstia. Taulukkosolut = 38 puikkoriviä × 4 + 123 kokomittaa. Tunnukset = 77 neulelyhennettä + 8 lankapaksuusluokkaa. Symbolit = 24 neulemerkkiä + 20 hoitomerkkiä.
- Current callers: tuotannon lähdesettien tuore haku löysi 1788 suoraa resurssiviitettä ja 1338 käytettyä ruotsin avainta. Tämä on viittaushaku, ei 1788 näkymän ajo. Identtiset käytöt on ryhmitelty; viitteet ovat `caller_refs`-kentässä. Dynaamiset jäsenkartat ja lukujen koostaminen arvioitiin erikseen. Englannin avain–batch-karttaa käytettiin löytämisen apuna; nimittäjä laskettiin sv-XML:stä. `yarn_result_placeholder` siirrettiin todellisen käytön mukaan B16:een.
- Exclusions: oletuksen `widget_preview_primary_count=42` ja `counter_step_custom_placeholder=10` ovat tarkoituksellisia kääntämättömiä numeroita, mukana erillisillä riveillä. Käyttäjän nimet/muistiinpanot, Ravelryn sivun sisältö ja debug-esimerkkien synteettiset nimet eivät ole julkaistavan sovelluksen käännettäviä lauseita. Kanonisia/persistoituja tunnuksia ei käännetty. Vanha `Pattern repeat` -sentinel suodatetaan UI-rajalla.
- Unreferenced but reviewed: `insights_rows_count`, `size_col_head`, `size_col_height`, `size_row_adult_l`, `size_row_adult_m`, `size_row_adult_s`, `size_row_adult_xl`, `size_row_men_xs_30_32`, `sym_slip_knitwise_desc`, `sym_slip_knitwise_name`, `yarn_weight_ply_format`. Niitä ei poistettu eikä jätetty lukuarvion ulkopuolelle.
- TSV: UTF-8, yksi tietue rivillä; tekstien tabulaattorit/rivinvaihdot esitetään `\t`/`\n`. Androidin alkuperäinen `\n` säilyy näkyvänä pakomerkintänä. `input_hash` on koko nimetyn lähdetiedoston SHA-256. Item ID:t ovat yksilöllisiä.

## Language decisions and glossary

| Concept | Approved expression/forms | Context | Alternatives and exclusions | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| Craft | stickning / virkning | Neulonta/virkkaus; yhteinen seuranta neutraali | Yleinen aikaseuranta ei väitä vain stickning | SV01; READ-B03/B05 | PASS |
| Row / round | varv; omgång | Varv käy taso- ja pyöröneuleeseen; Omgång ymmärrettävä kierrosnimike | Ei mekaanista row→rad, round→rundvarv-vaihtoa. Diagrammin rad oikein | SV01; READ-B03/B11 | PASS |
| Repeat / shaping | upprepning; rapport; repetitions-/sektionsräknare; formning | Toistokerta, mallikerta, osio, muotoilu | Muistutuksen numero on Upprepning, ei käsky Upprepa; linkitetty laskuri eri kuin johdettu osio | SV01; READ-B04 | PASS |
| Stitch | maska; rät/avig maska; antal maskor | Maska, työvaihe ja lukumäärä | Sticka yksin ei selitä knit/purl-eroa tekniikkakortissa | SV01/SV07; READ-B18 | PASS |
| Pattern | mönster, webbmönster, diagram, rapport | Ohje, verkkometadata, kaavio, kuvio | PDF ja pelkät uppgifter eri asioita | SV01; READ-B08/B11 | PASS |
| Gauge | stickfasthet; masktäthet; maskor/varv per 10 cm tai 4 tum | Molemmat tiheysakselit, valittu pituus | Masktäthet on myös koko tiheyden synonyymi; ei mekaanista korvausta | SV05; READ-B13 | PASS |
| Swatch | provlapp | Näyte ja vertailuperusta | Ei yleispätevää hoito-/pingotuslupausta | SV05; READ-B13 | PASS |
| Increase | ökning, öka; M1, M1L/M1R; lyft ökning | Välilangasta ja alemmasta maskasta tehtävät lisäykset erotettu | M1:n nostosuunta määrää maskbågen; ei aina takalenkki ilman nostosuuntaa | SV03/SV07; E-S05 | PASS |
| Decrease | minskning / minska; rätt/avigt tillsammans | Kulutetut maskat, kallistus, nettomuutos | Hyväksyttävää minskning-sanaa ei vaihdeta kaikkialla hoptagning-muotoon | SV01/02/04/07/08 | PASS |
| Slip | lyft; rätt/avigt; en i taget / tillsammans | SSK/SSP yksitellen, S2KP yhdessä | Erilliset nostot eivät anna samaa keskitettyä kavennusta | SV02/04/07/08; E-S06 | PASS |
| Sides / direction | rätsidan / avigsidan; framsida / baksida; höger / vänster | Neuleen puoli vs. suunta | Offentlig/privat sida ei ole luonnollinen käsityöselite | SV01; READ-B18 | PASS |
| Loops / yarn | främre/bakre maskbågen; framför/bakom arbetet | Silmukan lenkki vs. langan sijainti suhteessa neulojaan | Wyif/wyib ei ole pysyvästi avigsidan/rätsidan; vriden maska, ei tvinnad | SV01/03/07 | PASS |
| Cast on / bind off | lägg upp / maska av | Neuleen aloitus/lopetus | Eri kuin sovelluksen spara/ta bort | SV01; READ-B15/B18 | PASS |
| Yarn thickness / mass | grovlek, garntjocklek; vikt, g | Luokka vs. fyysinen massa | Lace…Jumbo kanonisia nimiä; ply ei takaa rakennetta/paksuutta | SV01; E-S09/S13; READ-B06 | PASS |
| Yarn units | nystan, härva/härvor; m/yard, g | Pakkaus, kokonainen tai murto-osa | Nystan/härva hyväksyttäviä tässä; 1 härva, murtomäärä härvor | SV01; READ-B07/B16 | PASS |
| Ownership / use | Mitt garn; planerat, avsatt, använt, kvar | Varasto vs. projektin erilliset määrät | Kvar ei vähennä varastoa; tuntematon ei nolla; negatiivinen ylitys | READ-B06/B07 | PASS |
| Color / dye lot | färg, färgnummer, färgbad | Nimi, numero, värjäyserä | Färgbad säilytetään; lisänystan harkinnanvarainen | SV01; E-S14 | PASS |
| Tools | sticka, rundsticka, flätsticka; virknål | Puikko vs. virkkuukoukku | Puikko ei nål; runda stickor ei nimeä rundstickor-välinettä | SV01; READ-B14/B18 | PASS |
| Size / fit | kroppsmått, arm handled till armhåla, mån, förtonåring | Vartalomitat ja todelliset ikäluokat | Ei valmisvaatteen mittoja/lisättyä väljyyttä; tween ei nuorempi teini | E-S04/S16–S21; READ-B17 | PASS |
| Care | tvätt högst … °C; kemtvätt P/F | Prosessi, sallittu/kielletty, enimmäislämpötila | P/F ei kemikaalin nimi; legacy A rajattu kemtvättiin | E-S07/S08; READ-B06 | PASS |
| Actions | spara, koppla, ta bort, importera, exportera, återställ, ersätt, öppna | Todellinen tallennus-/poisto-/liitossopimus | Sama Ta bort hyväksyttävä, kun kohde ja seuraus täsmennetään | READ-B02/B08/B09/B21 | PASS |
| Sessions / insights | pass, tid, värde, slutföranden | Kesto, sessio, edistyminen, valmistumiskerrat | Nykyinen laskuriyksikkö ei vanhan session yksikkö | READ-B05 | PASS |
| Availability / billing | kostnadsbelagt; köp; återställ köp | Maksullisuus vs. ostotila | Betald lupasi virheellisesti maksettua omistusta | READ-B08/B22 | PASS |
| Number composition | 1 varv / 2 varv; 1 omgång / 2 omgångar; 25 %; 3 mån | Lause ja määrä | Väli numeron ja merkin/yksikön väliin; muuttujien tyypit säilyvät | SV06; RES-CHECK; READ-B03/B05/B10 | PASS |

## Sources

Uudet sivut luettiin 2026-10-19 taulukon rajauksella. Jokainen ruotsin teksti luettiin erikseen; URL ei itsessään hyväksy kieltä. SV01/SV04/SV05 ovat samaa DROPS-alkuperää, eivät riippumattomia vahvistuksia. SV02, SV03, SV07 ja SV08 ovat eri julkaisijoita. Epäolennaisia virheellisiä sanastorivejä ei sovellettu.

| ID | Title and publisher | URL | Language/region | Access date | Passage/claim and limitations |
| --- | --- | --- | --- | --- | --- |
| SV01 | Ordlista, DROPS / Garnstudio | https://www.garnstudio.com/glossary.php?lang=se&langf=se&langt=us | sv / Ruotsi, vastineet US | 2026-10-19 | Varv=row/round, maska, maskbåge, sticka, höger/vänster, följande, nystan/härva, mönster/rapport. Ei väitettä koko taulukon virheettömyydestä; epäolennaista pund/ounce-riviä ei käytetty. |
| SV02 | Disktrasan Smörblomma, Hildurs Barnbarn | https://www.hildursbarnbarn.se/produkter/monster/disktrasor-och-annat-till-hemmet/juni-disktrasan-smorblomma/ | sv / alkuperäinen neuleohje | 2026-10-19 | Oma selite: SSK:n erilliset oikeinnostoaskeleet ja yhteen neulominen takalenkeistä, rm/am/överdragshoptagning. Riippumaton DROPSista/CYC:stä. |
| SV03 | Parvisa ökningar, stickigt.se | https://www.stickigt.se/parvisa-okningar-okningar-med-vanster-och-hoger-lutning/ | sv / tekniikkaohje | 2026-10-19 | M1L/M1R-työvaiheet, KFB ja alemmasta maskasta nostetut lisäykset. Loppusanaston M1L→höger-virhettä ei omaksuttu; yksityiskohtainen työjärjestys ja E-S05 ratkaisevat suunnan. |
| SV04 | Keskitetyn kavennuksen ruotsinkielinen tekniikkaohje, DROPS | https://www.garnstudio.com/video.php?id=1763&lang=se | sv | 2026-10-19 | Nosta kaksi yhdessä kuin ne neulottaisiin oikein yhteen, yksi oikein, nostetut yli. Riippumaton vahvistus E-S06/SV07. |
| SV05 | Stickfasthet, DROPS | https://www.garnstudio.com/video.php?id=108&lang=se | sv | 2026-10-19 | Stickfasthet/masktäthet: leveysmaskat ja korkeusvarvot, provlapp ja mittaus ilman reunoja. Perustelee hyväksyttävän masktäthet-sanan säilyttämisen. |
| SV06 | Procenttecknet och mellanrum, Isof Frågelådan | https://frageladan.isof.se/faqs/21726 | sv / kielenhuolto | 2026-10-19 | Väli numeron ja %-merkin välissä. Ei neuletekniikan tai kaikkien lyhenteiden auktoriteetti. |
| SV07 | Knitting Abbreviations, Craft Yarn Council | https://www.craftyarncouncil.com/standards/knitting-abbreviations | en / US | 2026-10-19 | SSK/SSP, S2KP2, KFB, K/P, langan etu/takasijainti. Tekninen merkitys; ruotsin luonnollisuus arvioitiin erikseen. |
| SV08 | English–Svenska Knitting Glossary, Sarah Bradberry / Knitting-and.com | https://knitting-and.com/crafts-and-needlework/knitting/tips/knitting-glossaries/english-svenska-swedish-knitting-glossary/ | en–sv | 2026-10-19 | Rajatusti SSP:n työjärjestys yhdessä CYC:n kanssa. Muut osin puutteelliset sanastorivit eivät ole yleistä kielenhyväksyntänäyttöä. |

### SHARED-FACTS: reused factual evidence

[Englannin raportin Sources-taulukko](../en/review.md) sisältää E-S01–E-S22-viitteiden tarkat URL:t, osiot ja rajoitukset (alkuperäiset tunnukset S01–S22). Sen 2026-10-18 lähdeluku ja testit ovat aiempaa evidenssiä, eivät tässä tehtävässä uudelleen ajettuja testejä tai kaikkien sivujen uutta verkkolukua. SV07 kattaa lyhennesivun uuden luvun. Kaikki 449 [englannin manifestin](../en/input-hashes.json) tiedostoa vastasivat nykyistä työpuuta alussa ja lopussa.

- E-S01/S02/S05/S06: operaatiot ja 24 merkin geometria. Nykyiset `ChartSymbolData`, `AbbreviationData` ja kaikki ruotsin merkitys/kuvaus-parit luettiin; katselupuoli, työvaiheet ja ruotsin lähteet tarkastettiin itsenäisesti.
- E-S07/S08: kaikki 20 hoitomerkkiä, Celsius-enimmäisarvot ja P/F/A-prosessiluokat; ruotsin merkitys tarkastettiin erikseen.
- E-S03/S10/S15: US/JP/UK-järjestelmät aluekohtaisista lähteistä (CYC, Clover, Wool Zone). Nykyiset 38 × 4 solua luettiin/laskettiin; ruotsin tekstiä korjattiin vain väärien rajojen/universaalisuusväitteen osalta.
- E-S04/S16–S21: kuusi CYC-vartalomittataulukkoa, 123 nykyistä solua. Ruotsin vartalo/vaate, arm-mitan päätepisteet, kuukausi-iät ja tween arvioitiin erikseen. cm/2,54 ja alueiden esitys tarkastettiin nykyisestä `SizeChartScreen`-koodista.
- E-S09/S13/S14/S22: paksuus vs. säierakenne, harkinnanvarainen lisänystan, g-yksikkö. `StandardYarnWeight` sisältää kahdeksan kanonista nimeä ilman tuotannon ply-väitteitä. `YarnEstimator` pyöristää ostomäärän ylös ja laskee kokonaisten pakkausten painon.
- `shared-fixes.md`: nykykorjaukset, myös B05-01:n rajatut kalenteripäivät, tarkastettiin nykykoodia vasten. Historiallinen OPEN ei kumoa saman asian myöhempää verifiointia. Ruotsin daysMeta saa oikean päivämäärävälin nimittäjän.
- Tässä tehtävässä katsotut vanhat kuvat: `reports/language-audit/shared/20261018-followup/shared-corrected-KFB.png` ja `shared-corrected-P2tog.png`. Niillä tarkasteltiin yhteisten 24 merkin geometriaa. Ne eivät näytä lopullista ruotsin tekstiä eivätkä hyväksy ruotsin V-kenttiä.

Evidence IDs: `CURRENT-CALLERS` = tuore viittaushaku ja merkitykselle olennaisten kutsujien lähdeluku; `READ-Bnn` = alla oleva itsenäinen ruotsin lukuarvio; `RES-CHECK` / `BUILD-TEST` = Validation log. Jäsenviitteiden polut löytyvät coverage-riviltä/manifestista. `SHARED-FACTS Sxx` tarkoittaa yllä rajattua E-Sxx-evidenssiä.

## Batch tracker

Owned/reviewed laskee mukaan erikseen luetut taulukkosolut, tunnukset, symbolit ja merkityskontekstit. Ne eivät kasvata 1380 ruotsin tekstivariantin nimittäjää.

| Batch | Owned items | Reviewed items | Progress | Verdict | Open finding IDs | Evidence |
| --- | ---: | ---: | --- | --- | --- | --- |
| B01 | 27 | 27 | REVIEW_COMPLETE | PASS | Ei | READ-B01; 27 sv-varianttia; 1 korjattua avainta |
| B02 | 104 | 104 | REVIEW_COMPLETE | PASS | Ei | READ-B02; 104 sv-varianttia; 1 korjattua avainta |
| B03 | 84 | 84 | REVIEW_COMPLETE | PASS | Ei | READ-B03; 78 sv-varianttia; 2 korjattua avainta |
| B04 | 42 | 42 | REVIEW_COMPLETE | PASS | Ei | READ-B04; 41 sv-varianttia; 1 korjattua avainta |
| B05 | 129 | 129 | REVIEW_COMPLETE | PASS | Ei | READ-B05; 123 sv-varianttia; 4 korjattua avainta |
| B06 | 87 | 87 | REVIEW_COMPLETE | PASS | Ei | READ-B06; 59 sv-varianttia; 6 korjattua avainta |
| B07 | 69 | 69 | REVIEW_COMPLETE | PASS | Ei | READ-B07; 67 sv-varianttia; 0 korjattua avainta |
| B08 | 78 | 78 | REVIEW_COMPLETE | PASS | Ei | READ-B08; 75 sv-varianttia; 1 korjattua avainta |
| B09 | 77 | 77 | REVIEW_COMPLETE | PASS | Ei | READ-B09; 76 sv-varianttia; 0 korjattua avainta |
| B10 | 69 | 69 | REVIEW_COMPLETE | PASS | Ei | READ-B10; 68 sv-varianttia; 4 korjattua avainta |
| B11 | 78 | 78 | REVIEW_COMPLETE | PASS | Ei | READ-B11; 77 sv-varianttia; 0 korjattua avainta |
| B12 | 32 | 32 | REVIEW_COMPLETE | PASS | Ei | READ-B12; 31 sv-varianttia; 0 korjattua avainta |
| B13 | 79 | 79 | REVIEW_COMPLETE | PASS | Ei | READ-B13; 76 sv-varianttia; 0 korjattua avainta |
| B14 | 35 | 35 | REVIEW_COMPLETE | PASS | Ei | READ-B14; 30 sv-varianttia; 2 korjattua avainta |
| B15 | 21 | 21 | REVIEW_COMPLETE | PASS | Ei | READ-B15; 19 sv-varianttia; 1 korjattua avainta |
| B16 | 14 | 14 | REVIEW_COMPLETE | PASS | Ei | READ-B16; 13 sv-varianttia; 2 korjattua avainta |
| B17 | 348 | 348 | REVIEW_COMPLETE | PASS | Ei | READ-B17; 72 sv-varianttia; 9 korjattua avainta |
| B18 | 235 | 235 | REVIEW_COMPLETE | PASS | Ei | READ-B18; 157 sv-varianttia; 35 korjattua avainta |
| B19 | 83 | 83 | REVIEW_COMPLETE | PASS | Ei | READ-B19; 58 sv-varianttia; 15 korjattua avainta |
| B20 | 36 | 36 | REVIEW_COMPLETE | PASS | Ei | READ-B20; 35 sv-varianttia; 1 korjattua avainta |
| B21 | 29 | 29 | REVIEW_COMPLETE | PASS | Ei | READ-B21; 28 sv-varianttia; 0 korjattua avainta |
| B22 | 67 | 67 | REVIEW_COMPLETE | PASS | Ei | READ-B22; 66 sv-varianttia; 3 korjattua avainta |
| B23 | 3 | 3 | REVIEW_COMPLETE | PASS | Ei | READ-B23; 0 sv-varianttia; 0 korjattua avainta |

### Contextual reading notes

- **READ-B01:** Yleistoiminnot luettu: Avbryt sulkee luonnoksen/valinnan tai peruu työn, Spara vahvistaa kohteena olevan lomakkeen, Ta bort kohdistuu nimetyn vahvistuksen sisältöön. Valda-otsikko toimii myös yhdellä projektilla, kuvalla, langalla ja ohjeella. Yksiköt tulkitaan kentän mukaan.
- **READ-B02:** Projektit ja kansiot: aktiivinen/valmis, lajittelu, virtuaaliset näkymät, siirto ja poisto. Kansion poisto säilyttää projektit; projektin poiston vahvistus on erillinen. Käyttäjän nimi on muuttumaton parametri. Kaikki one/other-lauseet luettu, myös epäonnistunut joukkosiirto.
- **READ-B03:** Pääluvun paikka ja määrä erotettu: Varv 1 / 1 varv, Omgång 1 / 1 omgång, Upprepning 1 / 1 upprepning. ROWS/ROUNDS/REPEATS/CUSTOM sekä askel1/>1, tavoite/kvar/över ja historia käsitelty. Widgetin nimitys neutraloitu. Omia nimiä ei yritetä taivuttaa.
- **READ-B04:** Laskurien nykyarvo, ylös/alas, linkitys, muotoilu ja raporttiosion rajat luettu. Muistutuksen occurrence on järjestysnumero, intervalRows väli, ei automaattisesti neulevarvoiksi todettu määrä. Pääluku voi käyttää omavalintaista yksikköä.
- **READ-B05:** Passi käynnistyy ja päättyy erillisillä toiminnoilla. Lisää/jatka, hylkää/stop ja muokkaa/lopeta erotettu. Historiassa neutraali räknare/värde. Kestot, aikavälit, streak, craft-nimet, trendit ja valmistumistapahtumat luettu. Päiväpluralin quantity on daysInRange. Uusin B05-01-korjaus rajaa kalenteripäivät näyttöväliin.
- **READ-B06:** Lankakortti, varastotila, määrä, värinumero ja färgbad luettu. Grovlek tarkoittaa paksuutta, grammapaino eri asia. Kaikki 20 hoitoluokkaa ja niiden merkitys käsitelty. P/F ovat kemiallisen pesun luokkia, lämpötilat enimmäisarvoja. Kahdeksan kanonista englanninkielistä paksuusnimeä säilytetään; poistettu ply-hint ei esiinny tuotannossa.
- **READ-B07:** Projektimuistiinpano, Mitt garn -kopio ja käytön kolme erillistä määrää luettu. Planerat/avsatt/använt ovat toisistaan riippumattomia; kvar=avsatt-använt, negatiivinen määrä on ylitys, null tuntematon. Mitt garn -saldoa ei muuteta. Härva/nystan ovat näissä yhteyksissä ymmärrettäviä pakkausnimityksiä. Murtomäärä käyttää härvor; kokonainen 1 härva.
- **READ-B08:** Mönster/webbmönster/uppgifter ja PDF erotettu. Alias ratkaistu sekä otsikko- että designer-kentässä. Maksullinen ei tarkoita ostettua. Kirjastopoisto poistaa sen merkintäkerroksen, säilyttää projektien PDF:t/merkinnät. Ravelryn omalla sivulla näkyvät nimet eivät ole app-owned-käännöksiä. Linkki ei lupaa offline-PDF:ää.
- **READ-B09:** Saapuvan PDF:n välitön kopio, projektin dokumentit, päädokumentti, järjestys ja poistot sekä kamera/galleriakuvista tehty PDF luettu. 20 sivua, 25 MiB/kuva ja 200 MiB/valinta vastaavat toteutusta. Ei OCR- eikä synkronointilupausta. Kamera- ja kuvasanasto säilyy.
- **READ-B10:** Sivut, kirjanmerkit, lukuviivat, akselit, kalibroinnin kaksi lukua ja seurannan tauko luettu. Bokmärke/markering/guide ovat eri asioita. Luvut ovat pääluvun arvoja, eivät aina varvoja. Prosenttipaikka on etäisyys ylä-/vasemmasta reunasta; molemmat akselit ja seuranta/manual-lauseet käsitelty.
- **READ-B11:** Työkalut, luku/merkintätilat, valinta, geometria, akselit, diagrammirivit ja kerrokset luettu. Rader tarkoittaa tässä ruudukon rivejä, ei virheellistä craft-yksikköä. Projektikerros muokattava, master luku; PDF-vienti ei muuta alkuperäistä. Tiedostonimen ruotsalainen med-anteckningar-liite ja virhe-/edistymisviestit luettu.
- **READ-B12:** Paikalliset muistiinpanot, tallennuksen virhe/luonnoksen hävitys, kuvat, jakaminen ja yksittäis-/joukkopoistot luettu. Kuvan Värde ei väitä historiallista varvoyksikköä. Käyttäjän valokuva-/projektinimi pysyy omana tietonaan.
- **READ-B13:** Mitat, molemmat tiheysakselit, provlapp, 10 cm/4 tum, omat/ohjeen luvut, alkuperäinen/muuttamaton/pyöristetty koko ja Copy-kooste luettu. Masktäthet on ruotsalaisessa käsityökäytössä myös koko tiheyden synonyymi, joten sitä ei muuteta mekaanisesti. Laskenta käyttää pyöristämättömiä kanonisia arvoja.
- **READ-B14:** Lisäys/kavennus, taso/rund, helppo/tasainen jakauma, virheargumenttien järjestys ja englanniksi rajatut parseriesimerkit luettu. Generaattorin K/M1/K2tog/× saa selitteen olemassa olevaan help-tekstiin. M1 tarvitsee erillisen välilangan; n=1 ei kelpaa positiivisella lisäyksellä.
- **READ-B15:** Luontileveys on ennen reunamaskoja; kokonaisreunamäärä lisätään rungon lähimmän raportin jälkeen. Result ja vaihtoehdot sisältävät reunat. 10 cm ja 4 tum eivät ole sama pituus. Silmukkamäärän molemmat pluralit ja mittayksikkökooste luettu.
- **READ-B16:** Garnberäknare: tarvittava kokonaispituus, nystanin pituus/paino, ylöspäin pyöristetty ostomäärä ja kokonaisten pakkausten massa luettu. Lisänystan on harkinta, ei ehdoton sääntö. g pysyy pienaakkosena.
- **READ-B17:** Kaikki 38 puikkoriviä × 4 solua sekä 123 kokotaulukon mittaa yhdistetty nykyisiin otsikoihin/lähteisiin. JP oma tarkka mitta-asteikko; US/UK valmistajavaihtelu ja puuttuva — säilyvät. Kokotiedot ovat vartalon mittoja; lapsen arm tarkoittaa rannetta–kainaloa, vauvan ikä kuukausia, tween ennen teini-ikää. Tuumat johdetaan cm/2,54.
- **READ-B18:** 77 englanninkielistä lyhennetunnusta ja kaikki ruotsalaiset merkitys/selitys-parit luettu yhdessä. Oikea/avoin maska, nosto yksitellen/yhdessä, langan sijainti ja flätstickan etu/taka erotettu. Lyhenteet ovat neulontaa, vaikka sovellus tarjoaa myös virkkausprojektit. Hakutoiminto hakee myös ruotsalaisia merkityksiä/selityksiä.
- **READ-B19:** 24 käytössä olevaa kaaviomerkkiä, nimet, selitykset ja katselupuoli luettu. CYC:n oikealta puolelta luettava konventio; ohjeen oma selite ensisijainen. KFB/P2tog-kuvia tarkasteltu yhteisen piirroksen evidenssinä, ei ruotsin renderöintitodisteena. Käyttämätön knitwise-slip-resurssipari silti luettu.
- **READ-B20:** Kielinimet, järjestelmäoletus, teemat, palaute, päivitys, yksikköasetus ja tietosuojatekstit luettu. Gauge voi ohittaa oletusyksikön. Crashlytics/PostHog eri toiminnot; usage opt-in ja sisältöä koskeva rajaus vastaavat nykyisiä toteutus-/config-syötteitä.
- **READ-B21:** Varmuuskopio on paikallisen sisällön vienti/palautus, ei synkronointi. Salaamattomuus, korvaaminen, säilytettävät asetukset ja uuteen laitteeseen siirtymättömät oikeudet luettu. Esikatselun määrät ovat otsikko: luku -muotoja. Virheilmoitukset kuvaavat nykyistä restoreValidated- ja rollback-sopimusta, ei uutta tietokantatestiä.
- **READ-B22:** Aktiiviprojektin kiintiö, itse aloitettava 14 päivän kokeilu, kertamaksu, vanhan sisällön säilyminen, portitetut uudet objektit ja edistyneet näkymät luettu. Hinnan parametri on Playn paikallistettu teksti. Peruutettu/odottava/onnistunut osto ja palautus erotettu. Muistutuksia ei rajata varvoihin.
- **READ-B23:** Jäännössisältö ja kaikkien lähdesettien app-owned-teksti täsmäytetty. Numero-/päivä-/kesto-/tiedostonimiformatterit, kanoniset tunnukset ja poissuljetut debugnäytteet kirjattu. Ei uutta ratkaisematonta kielikysymystä; kuvat eivät ole automaattinen hyväksyntäportti.

### Materially different meanings and compositions

40 `generated_output`-riviä kuvaa lähdekoodista arvioituja tilanteita, ei ajettuja kuvakaappaustestejä: pääluvun kaikki nimiketyypit/askel/tavoite, muistutuksen numero/väli, kesto/päivämäärä/historia, langan tuntematon/nolla/ylitys/murto-osa, metadata/PDF/paid/poisto, dokumenttirajat, prosentit, vientinimi, Gauge/kopio, lisäys/kavennus ja yhden lähtömaskan virhe, luontilaskurin reunat/raportit, pakkausten paino, kokomuunnokset, lyhennehaku, merkkien katselupuoli, oletusyksikön ohitus, backup, Pro sekä päivä/kello/nimiformatterit.

Erityisesti luettiin `MainCounterDisplayText`, `GaugePresentation`, `YarnUsagePresentation`, `MeasurementNumberFormatter`, `DurationDisplayFormatter`, `LocaleDateFormat`, `IncreaseDecreaseCalculator`, `CastOnCalculator`, `YarnEstimator`, `InstructionParser`, `SizeChartScreen`, `MultiCounterComponents.CounterValueDisplay.ReminderRepeat`, `PatternCard.patternAvailabilityLabelRes` ja Insightsin laskenta/kutsut. Kirjaston poistotekstiä verrattiin `SavedPatternRepository`-viitelaskentaan. Backup-tekstiä verrattiin `BackupRepository`-vientiin, validointiin, palautukseen ja edelleen pätevään yhteiseen näyttöön; uutta Room-palautusajoa ei tehty.

Pidemmät korjatut lyhenteiden ja symbolien kuvaukset tarkastettiin tuotantokorttien lähdekoodista: ei maxLines-katkaisua tai kiinteää tekstikorkeutta; korttien laajennus/vieritys on olemassa. Yhtään konkreettista näyttöajolla ratkaistavaa kielikysymystä ei jäänyt. Kattavan kuvakokoelman puuttuminen ei ole sellainen kysymys.

## Findings

**88 avainta / 89 tekstivarianttia korjattu.** Yksi muistutusteksti esiintyy sekä one- että other-variantissa. TERM 41, LANGUAGE 23, CONTEXT 14, FACT 10; S2 66, S3 22, ei S1. Kaikki sovelletut löydökset CONFIRMED_DEFECT / VERIFIED.

Alla jokaisen havainnon konteksti kattaa saman avaimen kaikki `coverage.tsv`-variantit ja `caller_refs`-viitteet. Muutettu teksti luettiin uudelleen. Yhteinen lopullisen version verifiointi: XML-arvo, variantit, paikanvaraajat, alias, resurssikäännös, kolme kohdennettua testiä ja käänteinen tavukohtainen säilyvyysvertailu (Validation log). Runtime/native-tarkastusta ei väitetä suoritetuksi.

### LA-sv-B01-01: `n_selected`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `n_selected` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%d valda".
- User-visible consequence, evidence and reasoning: The caller also supplies 1; 1 valda has incorrect number agreement. A count heading works without adding a shared plural resource.
- Evidence: ProjectListScreen;LibraryTopBar; READ-B01.
- Final correction: "Valda: %d".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B02-01: `sort_created_date`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `sort_created_date` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Skapad datum".
- User-visible consequence, evidence and reasoning: Skapad datum has incorrect agreement and is not a natural date label; the compound names the creation-date sort key.
- Evidence: READ-B02; READ-B02.
- Final correction: "Skapandedatum".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B03-01: `widget_name`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `widget_name` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Varvräknare".
- User-visible consequence, evidence and reasoning: The widget counts the selected main-counter unit, including repeats and custom units; a row-only name misstates its scope.
- Evidence: CounterWidget;MainCounterLabelType; READ-B03.
- Final correction: "Projekträknare".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B03-02: `widget_description`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `widget_description` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Varvräknarwidget för aktivt projekt".
- User-visible consequence, evidence and reasoning: The widget is not restricted to rows; remove only that restriction.
- Evidence: CounterWidget;MainCounterLabelType; READ-B03.
- Final correction: "Räknarwidget för aktivt projekt".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B04-01: `reminder_repeat_occurrence_format`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `reminder_repeat_occurrence_format` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: one, other; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Upprepa #%1$d, intervall %2$d".
- User-visible consequence, evidence and reasoning: The first argument is an occurrence number. Upprepa is an imperative telling the user to repeat, not a label for that occurrence. Both one/other need the noun.
- Evidence: MultiCounterComponents.CounterValueDisplay.ReminderRepeat; READ-B04.
- Final correction: "Upprepning #%1$d, intervall %2$d".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B05-01: `insights_empty_title`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `insights_empty_title` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Ingen tid på stickorna än.".
- User-visible consequence, evidence and reasoning: The empty state also serves crochet and mixed projects; stickorna wrongly ties recorded time to knitting needles.
- Evidence: InsightsSections;InsightsCraftMix; READ-B05.
- Final correction: "Ingen registrerad tid än.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B05-02: `relative_time_days_ago`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `relative_time_days_ago` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%1$dd sedan".
- User-visible consequence, evidence and reasoning: The number is joined to the abbreviated duration unit; separate them so the composed date phrase reads correctly.
- Evidence: READ-B05;SV06; READ-B05.
- Final correction: "%1$d d sedan".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B05-03: `relative_time_weeks_ago`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `relative_time_weeks_ago` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%1$dv sedan".
- User-visible consequence, evidence and reasoning: The number is joined to the abbreviated duration unit; separate them so the composed date phrase reads correctly.
- Evidence: READ-B05;SV06; READ-B05.
- Final correction: "%1$d v sedan".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B05-04: `relative_time_months_ago`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `relative_time_months_ago` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%1$dmån sedan".
- User-visible consequence, evidence and reasoning: The number is joined to the abbreviated duration unit; separate them so the composed date phrase reads correctly.
- Evidence: READ-B05;SV06; READ-B05.
- Final correction: "%1$d mån sedan".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B06-01: `care_wash_30`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `care_wash_30` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "30°".
- User-visible consequence, evidence and reasoning: A bare degree value omits the Celsius scale and maximum-wash meaning of this care label.
- Evidence: CareSymbol;SHARED-FACTS S07/S08; READ-B06.
- Final correction: "Tvätt högst 30 °C".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B06-02: `care_wash_40`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `care_wash_40` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "40°".
- User-visible consequence, evidence and reasoning: A bare degree value omits the Celsius scale and maximum-wash meaning of this care label.
- Evidence: CareSymbol;SHARED-FACTS S07/S08; READ-B06.
- Final correction: "Tvätt högst 40 °C".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B06-03: `care_wash_60`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `care_wash_60` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "60°".
- User-visible consequence, evidence and reasoning: A bare degree value omits the Celsius scale and maximum-wash meaning of this care label.
- Evidence: CareSymbol;SHARED-FACTS S07/S08; READ-B06.
- Final correction: "Tvätt högst 60 °C".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B06-04: `care_dryclean_any`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `care_dryclean_any` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Alla lösningsmedel".
- User-visible consequence, evidence and reasoning: The legacy A symbol concerns dry cleaning; the isolated label otherwise permits all solvents without naming the treatment.
- Evidence: CareSymbol;SHARED-FACTS S07/S08; READ-B06.
- Final correction: "Kemtvätt, alla lösningsmedel".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B06-05: `care_dryclean_p`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `care_dryclean_p` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "P-lösningsmedel".
- User-visible consequence, evidence and reasoning: P/F identify professional dry-cleaning process classes, not a chemical called P/F solvent.
- Evidence: CareSymbol;SHARED-FACTS S07/S08; READ-B06.
- Final correction: "Kemtvätt P".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B06-06: `care_dryclean_f`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `care_dryclean_f` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "F-lösningsmedel".
- User-visible consequence, evidence and reasoning: P/F identify professional dry-cleaning process classes, not a chemical called P/F solvent.
- Evidence: CareSymbol;SHARED-FACTS S07/S08; READ-B06.
- Final correction: "Kemtvätt F".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B08-01: `paid`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `paid` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Betald".
- User-visible consequence, evidence and reasoning: Availability.Paid means the pattern costs money, not that this user has paid. The label must not imply purchase ownership.
- Evidence: PatternCard.patternAvailabilityLabelRes;PatternAvailability; READ-B08.
- Final correction: "Kostnadsbelagt".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B10-01: `pattern_reading_line_position_state`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `pattern_reading_line_position_state` — `app/src/main/res/values-sv/pattern_reading_assistance.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%1$d%% från överkanten".
- User-visible consequence, evidence and reasoning: Swedish percentage expressions require a space between the number and percent sign; preserve the escaped literal %%.
- Evidence: SV06;PatternViewerScreen; READ-B10.
- Final correction: "%1$d %% från överkanten".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B10-02: `pattern_vertical_guide_position_state`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `pattern_vertical_guide_position_state` — `app/src/main/res/values-sv/pattern_reading_assistance.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%1$d%% från vänsterkanten".
- User-visible consequence, evidence and reasoning: Swedish percentage expressions require a space between the number and percent sign; preserve the escaped literal %%.
- Evidence: SV06;PatternViewerScreen; READ-B10.
- Final correction: "%1$d %% från vänsterkanten".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B10-03: `pattern_reading_line_following_state`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `pattern_reading_line_following_state` — `app/src/main/res/values-sv/pattern_reading_assistance.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%1$d%% från överkanten. Följer huvudräknaren.".
- User-visible consequence, evidence and reasoning: Swedish percentage expressions require a space between the number and percent sign; preserve the escaped literal %%.
- Evidence: SV06;PatternViewerScreen; READ-B10.
- Final correction: "%1$d %% från överkanten. Följer huvudräknaren.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B10-04: `pattern_reading_line_manual_state`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `pattern_reading_line_manual_state` — `app/src/main/res/values-sv/pattern_reading_assistance.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "%1$d%% från överkanten. Räknarföljning pausad.".
- User-visible consequence, evidence and reasoning: Swedish percentage expressions require a space between the number and percent sign; preserve the escaped literal %%.
- Evidence: SV06;PatternViewerScreen; READ-B10.
- Final correction: "%1$d %% från överkanten. Räknarföljning pausad.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B14-01: `increase_decrease_error_not_enough_bars`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `increase_decrease_error_not_enough_bars` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Det finns inte tillräckligt med mellanrum mellan maskorna för dessa M1-ökningar. Minska antalet ökningar.".
- User-visible consequence, evidence and reasoning: For currentStitches=1, merely reducing a positive increase count cannot resolve the error; state the minimum starting-stitch requirement.
- Evidence: IncreaseDecreaseCalculator.calculate;IncreaseDecreaseScreen.localizedMessage; READ-B14.
- Final correction: "Dessa M1-ökningar kräver minst 2 startmaskor och tillräckligt med mellanrum mellan maskorna. Minska vid behov antalet ökningar.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B14-02: `tip_flat_vs_circular_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `tip_flat_vs_circular_desc` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Platt stickning: du stickar fram och tillbaka på raka eller runda stickor och vänder i slutet av varje varv.\\n\\nRundstickning: du stickar i ett kontinuerligt varv. Ökningar/minskningar fördelas jämnt runt varvet.".
- User-visible consequence, evidence and reasoning: Runda stickor misnames circular needles; the generated K/M1/K2tog notation also has no Swedish explanation in this tool. Add only its notation key to the existing help.
- Evidence: IncreaseDecreaseCalculator;IncreaseDecreaseScreen;SV01;SV03; READ-B14.
- Final correction: "Platt stickning: du stickar fram och tillbaka på raka stickor eller rundstickor och vänder i slutet av varje varv.\\n\\nRundstickning: du stickar i ett kontinuerligt varv. Ökningar/minskningar fördelas jämnt runt varvet.\\n\\nRäknarens beteckningar: K = sticka rätt, M1 = öka en maska genom att sticka tråden mellan maskorna vridet rätt, K2tog = sticka 2 maskor rätt tillsammans, × = upprepa.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B15-01: `desired_width`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `desired_width` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Önskad bredd".
- User-visible consequence, evidence and reasoning: Input width excludes edge stitches, while the output includes them; identify the input scope directly.
- Evidence: CastOnCalculator;CastOnScreen; READ-B15.
- Final correction: "Önskad bredd utan kantmaskor".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B16-01: `total_weight`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `total_weight` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Total vikt: %s g".
- User-visible consequence, evidence and reasoning: The result is ceil(required length/package length) times package mass, not the exact yarn consumption mass.
- Evidence: YarnEstimator;YarnEstimatorScreen; READ-B16.
- Final correction: "Vikt för hela nystan: %s g".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B16-02: `extra_skein_note`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `extra_skein_note` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Köp alltid ett extra nystan — färgbad kan variera".
- User-visible consequence, evidence and reasoning: Always buying an extra package is not a universal requirement. Keep dye-lot advice but make the extra purchase conditional.
- Evidence: SHARED-FACTS S14;YarnEstimator; READ-B16.
- Final correction: "Överväg ett extra nystan — färgbad kan variera".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-01: `size_col_arm`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `size_col_arm` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Arm".
- User-visible consequence, evidence and reasoning: The source measures wrist to underarm, not the whole arm or shoulder-to-wrist; the generic header hides that distinction.
- Evidence: SizeChartData;SHARED-FACTS S04/S17; READ-B17.
- Final correction: "Arm: handled till armhåla".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-02: `desc_size_charts`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `desc_size_charts` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Standardmått för plaggstorlekar".
- User-visible consequence, evidence and reasoning: The tables contain body measurements, not finished-garment measurements; garment ease must not be inferred.
- Evidence: SizeChartData;SHARED-FACTS S04/S16-S21; READ-B17.
- Final correction: "Kroppsmått för standardstorlekar".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-03: `size_row_age_0_3m`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `size_row_age_0_3m` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "3m".
- User-visible consequence, evidence and reasoning: The age label uses m, which denotes meters in the same measurement feature; explicitly abbreviate months in Swedish.
- Evidence: SizeChartData;READ-B17; READ-B17.
- Final correction: "3 mån".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-04: `size_row_age_3_6m`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `size_row_age_3_6m` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "6m".
- User-visible consequence, evidence and reasoning: The age label uses m, which denotes meters in the same measurement feature; explicitly abbreviate months in Swedish.
- Evidence: SizeChartData;READ-B17; READ-B17.
- Final correction: "6 mån".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-05: `size_row_age_6_12m`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `size_row_age_6_12m` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "12m".
- User-visible consequence, evidence and reasoning: The age label uses m, which denotes meters in the same measurement feature; explicitly abbreviate months in Swedish.
- Evidence: SizeChartData;READ-B17; READ-B17.
- Final correction: "12 mån".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-06: `size_row_age_12_18m`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `size_row_age_12_18m` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "18m".
- User-visible consequence, evidence and reasoning: The age label uses m, which denotes meters in the same measurement feature; explicitly abbreviate months in Swedish.
- Evidence: SizeChartData;READ-B17; READ-B17.
- Final correction: "18 mån".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-07: `size_row_age_18_24m`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `size_row_age_18_24m` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "24m".
- User-visible consequence, evidence and reasoning: The age label uses m, which denotes meters in the same measurement feature; explicitly abbreviate months in Swedish.
- Evidence: SizeChartData;READ-B17; READ-B17.
- Final correction: "24 mån".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-08: `size_row_tween`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `size_row_tween` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Yngre tonåring".
- User-visible consequence, evidence and reasoning: Tween means preteen, not a younger teenager; preserve the source age category.
- Evidence: SizeChartData;SHARED-FACTS S20; READ-B17.
- Final correction: "Förtonåring".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B17-09: `tip_needle_sizes_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `tip_needle_sizes_desc` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Metrisk storlek (mm) är den universella standarden. US-storlekar använder ett numrerat system (0–50). UK/Kanada använder ett annat numrerat system. Japanska storlekar har en egen numrering. Kontrollera alltid mm-storleken för säkerhets skull.".
- User-visible consequence, evidence and reasoning: US needle sizes are not bounded by 0–50 (the table itself includes 000/00); metric diameter is useful without claiming a universal numbering convention.
- Evidence: NeedleSizeData;SHARED-FACTS S03/S10/S15; READ-B17.
- Final correction: "Metrisk storlek (mm) är en vanlig standard. US-storlekar använder ett numrerat system. UK/Kanada använder ett annat numrerat system. Japanska storlekar har en egen numrering. Kontrollera alltid mm-storleken för säkerhets skull.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-01: `abbr_sts_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_sts_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "En eller flera öglor på nålen".
- User-visible consequence, evidence and reasoning: The object is a knitting needle (sticka), not a sewing needle (nål); use the correct tool in the technique definition.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "En eller flera öglor på stickan".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-02: `abbr_sm_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_sm_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Flytta markören från vänster till höger nål".
- User-visible consequence, evidence and reasoning: The object is a knitting needle (sticka), not a sewing needle (nål); use the correct tool in the technique definition.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Flytta markören från vänster till höger sticka".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-03: `abbr_lh_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_lh_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Vänster nål eller vänster sida".
- User-visible consequence, evidence and reasoning: The object is a knitting needle (sticka), not a sewing needle (nål); use the correct tool in the technique definition.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Vänster sticka eller vänster sida".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-04: `abbr_rh_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_rh_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Höger nål eller höger sida".
- User-visible consequence, evidence and reasoning: The object is a knitting needle (sticka), not a sewing needle (nål); use the correct tool in the technique definition.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Höger sticka eller höger sida".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-05: `abbr_rs_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `abbr_rs_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Den offentliga sidan av arbetet".
- User-visible consequence, evidence and reasoning: Offentliga sidan is a literal translation of public side, not natural Swedish for the outward fabric face.
- Evidence: SV01; READ-B18.
- Final correction: "Arbetets framsida".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-06: `abbr_ws_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `abbr_ws_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Den privata/baksidan av arbetet".
- User-visible consequence, evidence and reasoning: Privata sidan is a literal translation, not the wrong side of knitted fabric.
- Evidence: SV01; READ-B18.
- Final correction: "Arbetets baksida".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-07: `abbr_pwise_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S2 / VERIFIED.
- Resource key/member and file: `abbr_pwise_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Stick in nålen som om den skulle stickas avigt".
- User-visible consequence, evidence and reasoning: The original says the needle itself would be purled; identify the stitch as the object and the correct tool.
- Evidence: SV01; READ-B18.
- Final correction: "För in stickan som om du skulle sticka en avig maska".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-08: `abbr_k2tog_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_k2tog_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka ihop 2 st".
- User-visible consequence, evidence and reasoning: 2 st is the wrong Swedish stitch abbreviation and the operation omits rätt; distinguish K2tog from P2tog.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka 2 maskor rätt tillsammans".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-09: `abbr_k2tog_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_k2tog_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Högerlutande minskning: sticka två maskor som en".
- User-visible consequence, evidence and reasoning: The described knit decrease must specify rätt, otherwise the operation also fits a purl decrease.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Högerlutande minskning: sticka två maskor rätt tillsammans som en".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-10: `abbr_ssk_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_ssk_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Vänsterlutande minskning: lyft 2 maskor räta, sticka ihop dem genom de bakre öglorna".
- User-visible consequence, evidence and reasoning: Separate knitwise slips and knitting together through back loops are essential; the original omits separate slips and the worked stitch type.
- Evidence: SV02;SV07;SHARED-FACTS S01; READ-B18.
- Final correction: "Vänsterlutande minskning: lyft 2 maskor rätt en i taget, sticka ihop dem rätt genom de bakre maskbågarna".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-11: `abbr_ssp_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_ssp_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Vänsterlutad avig minskning".
- User-visible consequence, evidence and reasoning: The abbreviated title plus lean alone do not explain SSP and leave the viewing side unclear; specify its distinct purl operation.
- Evidence: SV07;SV08; READ-B18.
- Final correction: "Lyft 2 maskor rätt en i taget, sätt tillbaka dem på vänster sticka och sticka dem avigt tillsammans genom de bakre maskbågarna. Minskningen lutar åt vänster sett från rätsidan.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-12: `abbr_skp_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_skp_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lyft en maska, sticka en rät maska och dra den lyfta maskan över".
- User-visible consequence, evidence and reasoning: The slip orientation is part of SKP; add only rätt.
- Evidence: SV02;SV07; READ-B18.
- Final correction: "Lyft en maska rätt, sticka en rät maska och dra den lyfta maskan över".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-13: `abbr_k3tog_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_k3tog_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka ihop 3 st".
- User-visible consequence, evidence and reasoning: 3 st is not Swedish for three knit stitches and does not specify the knit operation.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka 3 maskor rätt tillsammans".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-14: `abbr_p3tog_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `abbr_p3tog_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka 3 avig tillsammans".
- User-visible consequence, evidence and reasoning: 3 avig is grammatically incomplete; state the count object and adverb.
- Evidence: SV01;SV07; READ-B18.
- Final correction: "Sticka 3 maskor avigt tillsammans".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-15: `abbr_s2kp_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_s2kp_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lyft 2 maskor, sticka 1 rät maska och dra de lyfta maskorna över".
- User-visible consequence, evidence and reasoning: For a centered double decrease the two stitches must be slipped together knitwise; separate slips produce another result.
- Evidence: SV04;SV07;SHARED-FACTS S06; READ-B18.
- Final correction: "Lyft 2 maskor tillsammans som om de skulle stickas rätt tillsammans, sticka 1 rät maska och dra de lyfta maskorna över".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-16: `abbr_sk2p_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_sk2p_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lyft 1, sticka 2 rät ihop, lägg den lyfta maskan över".
- User-visible consequence, evidence and reasoning: The original omits the knitwise slip and uses the ungrammatical 2 rät ihop; preserve all three required steps.
- Evidence: SV02;SV07; READ-B18.
- Final correction: "Lyft 1 maska rätt, sticka 2 maskor rätt tillsammans och dra den lyfta maskan över".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-17: `abbr_m1_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_m1_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Gör 1".
- User-visible consequence, evidence and reasoning: Gör 1 does not identify what is made in this technique; name the increase and its unit.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Öka 1 maska".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-18: `abbr_m1_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_m1_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Plocka upp tråden mellan maskorna och sticka den genom bakre maskbågen för att öka".
- User-visible consequence, evidence and reasoning: A generic M1 can use either lift direction; requiring the back loop without specifying the lift does not guarantee a twisted increase.
- Evidence: SV03;SHARED-FACTS S05; READ-B18.
- Final correction: "Plocka upp tråden mellan maskorna och sticka den vridet rätt för att öka".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-19: `abbr_kfb_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_kfb_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka fram och bak".
- User-visible consequence, evidence and reasoning: Fram och bak does not identify the loops or specify knitting rather than purling.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka rätt i främre och bakre maskbågen".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-20: `abbr_kfb_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_kfb_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka fram och bak i samma maska för att öka".
- User-visible consequence, evidence and reasoning: The two insertions concern loops of the same stitch and must both be knit; distinguish KFB from PFB.
- Evidence: SV03;SV07; READ-B18.
- Final correction: "Sticka rätt i främre och bakre maskbågen på samma maska för att öka".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-21: `abbr_pfb_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_pfb_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Aviga fram och bak".
- User-visible consequence, evidence and reasoning: Aviga fram och bak does not clearly name an operation or the loops; retain the purl distinction.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka avigt i främre och bakre maskbågen".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-22: `abbr_pfb_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_pfb_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka avigt fram och bak i samma maska för att öka".
- User-visible consequence, evidence and reasoning: Fram och bak must identify the front/back loops of the same stitch rather than fabric sides.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka avigt i främre och bakre maskbågen på samma maska för att öka".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-23: `abbr_sl1k_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_sl1k_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lyft en maska som om du skulle sticka".
- User-visible consequence, evidence and reasoning: Sticka alone does not distinguish knitwise from purlwise insertion; add the missing orientation.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Lyft en maska som om du skulle sticka den rätt".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-24: `abbr_ktbl_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_ktbl_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka genom bakre öglan".
- User-visible consequence, evidence and reasoning: Ktbl is specifically knit through back loop, distinct from Ptbl; the original only says sticka.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka rätt genom bakre maskbågen".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-25: `abbr_ktbl_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_ktbl_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Vriden stickad maska".
- User-visible consequence, evidence and reasoning: Stickad maska is any knitted stitch; the operation is a twisted knit stitch.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Vriden rät maska".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-26: `abbr_c4f_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_c4f_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sätt 2 till CN och håll framför, K2, K2 från CN".
- User-visible consequence, evidence and reasoning: Sätt ... till CN is not natural Swedish and leaves the held object/position unclear; identify the cable needle, front/back position and knit stitches.
- Evidence: SV01;SV07;SHARED-FACTS S02; READ-B18.
- Final correction: "Sätt 2 maskor på en flätsticka och håll den framför arbetet. Sticka 2 maskor rätt, sedan maskorna på flätstickan rätt.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-27: `abbr_c4b_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_c4b_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sätt 2 till CN och håll bakåt, K2, K2 från CN".
- User-visible consequence, evidence and reasoning: Sätt ... till CN is not natural Swedish and leaves the held object/position unclear; identify the cable needle, front/back position and knit stitches.
- Evidence: SV01;SV07;SHARED-FACTS S02; READ-B18.
- Final correction: "Sätt 2 maskor på en flätsticka och håll den bakom arbetet. Sticka 2 maskor rätt, sedan maskorna på flätstickan rätt.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-28: `abbr_c6f_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_c6f_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sätt 3 till CN och håll framför, K3, K3 från CN".
- User-visible consequence, evidence and reasoning: Sätt ... till CN is not natural Swedish and leaves the held object/position unclear; identify the cable needle, front/back position and knit stitches.
- Evidence: SV01;SV07;SHARED-FACTS S02; READ-B18.
- Final correction: "Sätt 3 maskor på en flätsticka och håll den framför arbetet. Sticka 3 maskor rätt, sedan maskorna på flätstickan rätt.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-29: `abbr_c6b_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_c6b_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sätt 3 till CN och håll bakåt, K3, K3 från CN".
- User-visible consequence, evidence and reasoning: Sätt ... till CN is not natural Swedish and leaves the held object/position unclear; identify the cable needle, front/back position and knit stitches.
- Evidence: SV01;SV07;SHARED-FACTS S02; READ-B18.
- Final correction: "Sätt 3 maskor på en flätsticka och håll den bakom arbetet. Sticka 3 maskor rätt, sedan maskorna på flätstickan rätt.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-30: `abbr_wyif_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_wyif_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Håll garnet framtill (avigsidan) av arbetet".
- User-visible consequence, evidence and reasoning: Yarn in front is relative to the knitter, not permanently the fabric wrong side.
- Evidence: SV01;SV07; READ-B18.
- Final correction: "Håll garnet framför arbetet, mot dig".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-31: `abbr_wyib_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_wyib_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Håll garnet på baksidan (stickad sida) av arbetet".
- User-visible consequence, evidence and reasoning: Yarn in back is relative to the knitter, not the knitted/right fabric side.
- Evidence: SV01;SV07; READ-B18.
- Final correction: "Håll garnet bakom arbetet, bort från dig".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-32: `abbr_st_st_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_st_st_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka på rätsidan, aviga på avigsidan (eller sticka varje varv runt)".
- User-visible consequence, evidence and reasoning: Stockinette requires knit/purl contrast; the original generic sticka and aviga do not give an accurate Swedish instruction.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka rätt på rätsidan och avigt på avigsidan (eller alla varv räta vid rundstickning)".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-33: `abbr_rev_st_st_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `abbr_rev_st_st_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka avigt på rätsidan, sticka på avigsidan".
- User-visible consequence, evidence and reasoning: Reverse stockinette requires knit on WS, not unspecified sticka.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Sticka avigt på rätsidan och rätt på avigsidan".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-34: `abbr_foll_meaning`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `abbr_foll_meaning` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Följer".
- User-visible consequence, evidence and reasoning: The paired description defines next row/round/instruction; follows (Följer) is the wrong grammatical meaning for this entry.
- Evidence: SV01;AbbreviationData; READ-B18.
- Final correction: "Följande".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B18-35: `abbr_wpi_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `abbr_wpi_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Garntjockleksmätning: lindar runt en linjal per tum".
- User-visible consequence, evidence and reasoning: Lindar is a verb (wraps), not the counted wraps that define WPI.
- Evidence: SV01;SHARED-FACTS; READ-B18.
- Final correction: "Mått på garntjocklek: antal lindningsvarv runt en linjal per tum".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-01: `sym_knit_name`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_knit_name` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka".
- User-visible consequence, evidence and reasoning: The legend names a knit stitch, whereas Sticka can name the tool or the general activity.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Rät maska".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-02: `sym_knit_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_knit_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka på rätsidan, aviga på avigsidan".
- User-visible consequence, evidence and reasoning: The right-side knit glyph needs the explicit knit/purl contrast.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Sticka rätt på rätsidan, avigt på avigsidan".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-04: `sym_purl_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_purl_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka avigt på rätsidan, sticka på avigsidan".
- User-visible consequence, evidence and reasoning: The reverse-side operation must be knit, not unspecified sticka.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Sticka avigt på rätsidan, rätt på avigsidan".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-05: `sym_yarn_over_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_yarn_over_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Linda garnet runt nålen för att skapa ett hål".
- User-visible consequence, evidence and reasoning: The yarn-over is made over a knitting needle, not a sewing needle.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Linda garnet runt stickan för att skapa ett hål".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-06: `sym_knit_tbl_name`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_knit_tbl_name` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka TBL".
- User-visible consequence, evidence and reasoning: Sticka TBL leaves the knit operation unspecified and uses an unexplained English loop abbreviation on the symbol card.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Vriden rät maska".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-07: `sym_knit_tbl_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_knit_tbl_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka genom den bakre öglan (tvinnad)".
- User-visible consequence, evidence and reasoning: Tvinnad describes twisting strands together; a twisted knit stitch is vriden, with the knit operation explicit.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Sticka rätt genom bakre maskbågen (vridet)".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-08: `sym_purl_tbl_name`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_purl_tbl_name` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Aviga TBL".
- User-visible consequence, evidence and reasoning: Aviga TBL is an incomplete mixed-language title; identify the twisted purl stitch.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Vriden avig maska".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-09: `sym_purl_tbl_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_purl_tbl_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka avigt genom den bakre öglan (tvinnad)".
- User-visible consequence, evidence and reasoning: Tvinnad misnames the crossed/twisted stitch as plied; retain the existing purl/back-loop operation.
- Evidence: SV01;SHARED-FACTS; READ-B19.
- Final correction: "Sticka avigt genom bakre maskbågen (vridet)".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-10: `sym_ssk_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_ssk_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lyft, lyft, sticka rätt — vänsterlutande minskning".
- User-visible consequence, evidence and reasoning: The isolated card says slip/slip/knit without explaining knit together or slip orientation; it cannot describe the actual decrease.
- Evidence: SV02;SV07; READ-B19.
- Final correction: "Lyft 2 maskor rätt en i taget och sticka dem rätt tillsammans genom bakre maskbågarna — vänsterlutande minskning".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-11: `sym_sk2p_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_sk2p_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lyft 1, 2 rm tillsammans, för över den lyfta maskan".
- User-visible consequence, evidence and reasoning: The knitwise slip is necessary to identify the SK2P operation; preserve the existing decrease sequence.
- Evidence: SV02;SV07; READ-B19.
- Final correction: "Lyft 1 maska rätt, sticka 2 maskor rätt tillsammans och dra den lyfta maskan över".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-12: `sym_m1l_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `sym_m1l_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Gör 1 vänster — vänsterlutad ökning".
- User-visible consequence, evidence and reasoning: Gör 1 vänster is an incomplete literal translation, not a natural Swedish operation.
- Evidence: SV03; READ-B19.
- Final correction: "Öka en maska åt vänster — vänsterlutad ökning".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-13: `sym_kfb_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / TERM / S2 / VERIFIED.
- Resource key/member and file: `sym_kfb_desc` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Sticka fram och bak - öka".
- User-visible consequence, evidence and reasoning: The glyph represents knitting twice into the same stitch; fram och bak alone could be fabric sides and omits the knit operation.
- Evidence: SV03;SV07;SHARED-FACTS S02; READ-B19.
- Final correction: "Sticka rätt i främre och bakre maskbågen på samma maska — ökning".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-14: `sym_lifted_inc_left_name`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `sym_lifted_inc_left_name` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lifted Inc Vänster".
- User-visible consequence, evidence and reasoning: Lifted Inc is untranslated English prose, not a preserved abbreviation token; translate the operation name.
- Evidence: SV03; READ-B19.
- Final correction: "Lyft ökning åt vänster".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-15: `sym_lifted_inc_right_name`

- Classification / category / severity / state: CONFIRMED_DEFECT / LANGUAGE / S3 / VERIFIED.
- Resource key/member and file: `sym_lifted_inc_right_name` — `app/src/main/res/values-sv/reference_strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lifted Inc. Höger".
- User-visible consequence, evidence and reasoning: Lifted Inc. is untranslated English prose, not a preserved abbreviation token; translate the operation name.
- Evidence: SV03; READ-B19.
- Final correction: "Lyft ökning åt höger".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B19-16: `chart_symbols_disclaimer`

- Classification / category / severity / state: CONFIRMED_DEFECT / FACT / S2 / VERIFIED.
- Resource key/member and file: `chart_symbols_disclaimer` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Symbolerna följer vanliga konventioner. Kontrollera alltid symbolförklaringen i ditt mönster.".
- User-visible consequence, evidence and reasoning: The implemented CYC glyphs use a right-side convention; the generic conventional disclaimer omits how to interpret reverse-side rows.
- Evidence: ChartSymbolData;ChartSymbolScreen;SHARED-FACTS S02; READ-B19.
- Final correction: "Symbolerna visar maskorna sedda från rätsidan. Kontrollera alltid symbolförklaringen i ditt mönster.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B20-01: `tip_imperial_units_desc`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `tip_imperial_units_desc` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "När detta är aktiverat använder alla räknare tum i stället för centimeter och yard i stället för meter. Stickfastheten mäts per 4 tum i stället för per 10 cm.".
- User-visible consequence, evidence and reasoning: The preference chooses defaults; Gauge allows independent unit/basis changes. All calculators use... overstates enforced behavior.
- Evidence: GaugeViewModel;GaugeScreen;PreferencesManager; READ-B20.
- Final correction: "När detta är aktiverat är tum och yard standardenheter i längdberäkningar i stället för centimeter och meter. Stickfasthet anges som standard per 4 tum i stället för per 10 cm. I Mått och stickfasthet kan du byta enhet separat.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B22-01: `pro_prompt_reminders_title`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `pro_prompt_reminders_title` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Lägg till en radpåminnelse med Pro".
- User-visible consequence, evidence and reasoning: Reminders trigger on main-counter values for any selected unit; the row-only promise misstates the feature.
- Evidence: RowReminder;CounterRepository;MainCounterLabelType; READ-B22.
- Final correction: "Lägg till en påminnelse med Pro".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B22-02: `pro_prompt_reminders_body`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `pro_prompt_reminders_body` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "En ny radpåminnelse kräver Pro. Dina befintliga påminnelser fortsätter fungera och kan redigeras.".
- User-visible consequence, evidence and reasoning: Reminders trigger on main-counter values for any selected unit; the row-only promise misstates the feature.
- Evidence: RowReminder;CounterRepository;MainCounterLabelType; READ-B22.
- Final correction: "En ny påminnelse kräver Pro. Dina befintliga påminnelser fortsätter fungera och kan redigeras.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### LA-sv-B22-03: `pro_group_workflow_body`

- Classification / category / severity / state: CONFIRMED_DEFECT / CONTEXT / S2 / VERIFIED.
- Resource key/member and file: `pro_group_workflow_body` — `app/src/main/res/values-sv/strings.xml`.
- Variant and affected contexts: value; tämän avaimen kaikki coverage.tsv-kontekstit.
- Original text: "Använd den kompakta repetitionsräknaren, extra-, repetitions-, formnings- och sektionsräknare, radpåminnelser, mönsterbilder som PDF och startskärmswidgeten.".
- User-visible consequence, evidence and reasoning: Reminders trigger on main-counter values for any selected unit; the row-only promise misstates the feature.
- Evidence: RowReminder;CounterRepository;MainCounterLabelType; READ-B22.
- Final correction: "Använd den kompakta repetitionsräknaren, extra-, repetitions-, formnings- och sektionsräknare, påminnelser, mönsterbilder som PDF och startskärmswidgeten.".
- Necessity / minimality: rajattu yllä yksilöityyn merkitys-, fakta- tai kielivirheeseen; muu sanamuoto säilytetty.
- Shared-code / other-language impact: ei muutoksia; vain ruotsin arvo.
- Checks: yhteinen lopullisen version tarkistus (RES-CHECK, BUILD-TEST, säilyvyysvertailu).
- Remaining uncertainty / dependency: ei yksilöityä avointa kielikysymystä.

### Acceptable alternatives retained

`masktäthet`, taso- ja pyöröneuleen `varv`, laskurin `Omgång`, ruudukon `rader`, `minskning`, `färgbad`, käyttöyhteyden `nystan/härva` sekä kaavion lyhyt otsikko `Aviga` säilytettiin. `sym_purl_name`-muutosehdotus peruttiin ennen lopullista validointia: elliptinen otsikko ei ollut todettu virhe. Siksi LA-sv-B19-03 ei ole sovellettu löydös eikä kuulu korjausmäärään. Hyväksyttäviä vaihtoehtoja ei yhtenäistetä kaikkialla.

## Shared handoffs and other-language impact

Ei uutta vahvistettua yhteisvikaa eikä korjaushandoffia. Muiden kielten hyväksyntöjä ei muuteta eikä kopioida ruotsin kielituomioiksi. `LANGUAGE_AUDIT.md` ja `shared-fixes.md` ovat muokkausrajan ulkopuolella.

Koordinaation mahdollinen myöhempi koontipäivitys: `sv — B01–B23 REVIEW_COMPLETE; AUDIT_PASS; 1349 keys / 1380 variants; 88 corrected keys / 89 variants; native NOT_REQUESTED; no open language issue`. Koontipäivitystä ei tehty tässä tehtävässä.

## Validation log

| Date | Input/build identity | Check/command | Scope | Actual result | Evidence/limitation |
| --- | --- | --- | --- | --- | --- |
| 2026-10-19 | Lopulliset sv-resurssit, manifestin työpuu | XML; oletuksen avaimet/tyypit; one/other; alias; indeksoitujen paikanvaraajien tyypit | Kaikki 1349 avainta / 1380 varianttia | PASS | RES-CHECK; ei automaattinen kielenhyväksyntä |
| 2026-10-19 | Sama syöte | `:app:processDebugResources` | Resurssien käännös/linkitys | PASS, tehtävä ajettiin | BUILD-TEST; ei UI-ajo |
| 2026-10-19 | Sama syöte | `:app:testDebugUnitTest` kahdella alla nimetyllä testiluokalla | Kolme nykyistä testiä | 3 PASS; 0 failures/errors/skipped | JUnit 2026-10-19T03:00:18Z; validation.json |
| 2026-10-19 | 449 aiempaa yhteistä syötettä | SHA-256 vs. en/input-hashes.json | Fakta-/kontekstinäytön ajantasaisuus | Kaikki 449 vastaavat alussa ja lopussa | Ei 449 uuden testin ajoa |
| 2026-10-19 | coverage.tsv, lopulliset tiedostot | Avaimet/variantit, ID:t, tekstit, hashit | 1826 riviä / 1380 sv-varianttia | PASS | Ei puuttuvaa/ylimääräistä sv-avainta tai pois jätettyä varianttia |
| 2026-10-19 | Aloitushashit ja lopputila | Kaikkien olemassa olleiden tiedostojen vertailu; korjausten käänteinen sovellus muistissa | Säilyvyys ja muokkausraja | Vain kolme sv-XML:ää muuttui; käänteinen muutos tuotti tarkat aloitushashit | Aiempi työ säilyi myös näissä sv-tiedostoissa |
| 2026-10-19 | Lopullinen työpuu | `git diff --check` | Whitespace | Exit 0 | CRLF→LF-asetusvaroituksia, ei diff-virheitä; rivinvaihtoja ei normalisoitu |

Todellinen yhdistetty komento:

```powershell
.\gradlew.bat :app:processDebugResources :app:testDebugUnitTest --tests com.finnvek.knittools.ReferenceResourceLocalizationTest --tests com.finnvek.knittools.ui.CountPluralResourcesSourceTest --console=plain --no-daemon
```

Tulos: **BUILD SUCCESSFUL in 55s**, 38 tehtävää (8 ajettua, 30 up-to-date). Resurssiprosessointi ja valittu testiajo suoritettiin. Gradlen tulevaa versiota 10 koskevia deprecation-varoituksia ei korjattu tämän sisältötehtävän osana. [validation-gradle.log](validation-gradle.log) ja [validation.json](validation.json) ovat tämän ajon evidenssiä. Lokit pidetään sv-hakemistossa tehtävän tiukan kirjoitusrajan vuoksi.

Uutta Android-runtimeajoa, kuvakaappauskampanjaa, 320 dp:n/200 %:n/yhdistelmän tai TalkBackin tarkastusta ei tehty. Niiden puuttumista ei merkitä PASS/N/A:ksi: kaikki uudet V-kentät ovat tyhjiä. Vanhojen kahden yhteiskuvan geometriatarkastus on rajattu erikseen. Lähdekoodista tarkastettu toiminta ei ole uusi laite-/Room-testi.

## Native-speaker review

- Status: **NOT_REQUESTED**.
- Reviewer qualification and reviewed scope: ei ilmoitettua äidinkielistä arvioijaa eikä native craft-expert -hyväksyntää.
- Remaining issues: ei konkreettista native-asiantuntijaa edellyttävää ratkaisematonta ilmaisua.

## Closeout

- Coverage: B01–B23 kaikki REVIEW_COMPLETE / PASS; 1349 nykyistä sv-avainta, 1380 tekstivarianttia, kaikki pluralit ja alias sekä olennaisesti erilaiset merkitykset. Lisäinventaario täsmää 1826 riviin.
- Confirmed findings: 88 avainta / 89 varianttia, kaikki VERIFIED; alkuperäinen teksti, vaikutus, lähde ja perusteltu korjaus yllä. Ei tyylin yhtenäistämiskampanjaa.
- Unresolved language questions / shared dependencies: ei avoimia vahvistettuja virheitä, nimettyjä ymmärtämiskysymyksiä, muuttuneita yhteisiä syötteitä tai odottavia handoffeja.
- Supplementary rendering: vanhojen yhteiskuvien geometriatarkastus; lopullista ruotsin UI-renderöintiä ei väitetä kattavasti testatuksi. Puuttuva screenshot ei §16:ssa ole itsenäinen kieliblokkeri.
- Technical checks: resurssikäännös, kolme kohdennettua testiä, XML/variantti/paikanvaraaja/alias/kattavuus, syötehashit, säilyvyys ja diff-check.
- Temporary processes: kertaluonteinen `--no-daemon`-Gradle päättyi exit 0. Ei käynnistetty emulaattoria, palvelinta, watcheria tai muita pysyviä taustaprosesseja.
- **Language verdict: AUDIT_PASS** revised LANGUAGE_AUDIT.md §16:n perusteella: täysi nykyinen sisältökattavuus, sovellettavat tarkistukset valmiit, ei vahvistettua jäljellä olevaa virhettä tai konkreettista avointa kielikysymystä. Tämä on kielen hyväksyntä, ei kaiken UI-renderöinnin sertifiointi. **Native review: NOT_REQUESTED.**

## Resume point

- Last fully reviewed item/sub-batch: B23, jäännösteksti ja lopullinen kattavuustäsmäytys.
- Current unfinished work: ei tämän ruotsin tehtävän sisällä.
- Next items: ei muuta kieltä eikä renderöintijonoa. Uusi tarkastus vain myöhemmin muuttuvalle syötteelle tai yksilöidylle uudelle kysymykselle.
- Inputs requiring recheck now: ei; hyväksynnän version määrittelee `input-hashes.json`.

