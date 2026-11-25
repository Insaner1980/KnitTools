# KnitTools-koodikatselmointi 2026-11-24: avoimet asiat

Katselmointi tehtiin `/code-review medium` -tasolla osa-alue kerrallaan haarassa
`codex/paikalliset-muutokset-20261124`. Tähän on koottu se, mikä jäi auki tai odottaa vahvistusta.

## Tehdyt kierrokset ja committit

| Osa-alue | Committi |
| --- | --- |
| Data- ja repository-kerros (poistopolut, tiedostosiivous, migraatiot) | `fe1129ae` |
| Pro ja laskutus (kokeilu, ostotila, hinta, projektiportti) | `249f44c0` |
| Laskuri ja työskentelyistunto | `30618848` |
| Varmuuskopio ja palautus | `b5336011` |
| PDF-lukija ja merkinnät | `71f8f741` |
| Ravelry ja PDF-vastaanotto | `36a38567` |
| Lanka ja langankäyttö | `9846e21c` |
| Insights-laskenta (putki, päiväakseli) | `0de67b57` |
| Perehdytys ja kokeilupolku (korjattu oma regressio: perehdytyksen PDF siivouksessa) | `573b5adc` |
| CounterViewModel (lisälaskurin tallennusjärjestys) | tämä committi |
| PDF-lukijan käyttöliittymä | ei löydöksiä |
| Widgetin piirto | ei löydöksiä |

Jätetty tietoisesti korjaamatta: perehdytyksen projektin nimeen kirjoitetut merkit katoavat PDF:n
kopioinnin ajan (`OnboardingViewModel.edit` palaa, kun `busy` on tosi). Korjaus vaatisi luonnoksen
kirjoitusjärjestyksen muuttamista, ja kopiointi on yleensä lyhyt.

## 1. Ravelryn kassan tunnistus (vaatii Ravelry-tilin)

`ui/screens/ravelry/RavelryLinks.kt`, `isRavelryPurchaseUrl` tunnistaa kassan vain näistä:

- `checkout.`-aliverkkotunnus
- `/checkout`-polkusegmentti
- PayPal, Stripe ja Braintree

Jos Ravelryn osto onnistuu ilman näitä (tallennettu maksutapa, Ravelry-krediitit, `/cart`- tai
`/purchase/...`-polku), ohjeen voi ostaa sovelluksen WebViewissä Google Playn maksujärjestelmän ohi.

Tehtävä oikealla tilillä:

1. Avaa maksullinen ohje Ravelryssä tavallisessa selaimessa ja kulje ostopolku loppuun asti ostamatta.
2. Kirjaa jokainen osoite, jonka kautta polku kulkee (ostoskori, maksutavan valinta, vahvistus).
3. Lisää puuttuvat osoitteet `isRavelryPurchaseUrl`-funktioon ja `RavelryLinksTest`-testeihin.

Tämä kuuluu samaan Play-julkaisua edeltävään tarkistukseen kuin Ravelryn kirjallinen lupa.

## 2. Laitetestit (ajamatta)

Useat korjaukset toimivat oikeasti vain laitteella. Nämä kannattaa ajaa liitetyllä Samsungilla:

| Korjaus | Laitetestit |
| --- | --- |
| Varmuuskopion vienti poistaa keskeneräisen tiedoston, palautuksen siivous | `BackupRepositoryTest`, `BackupCancellationRuntimeTest`, `BackupPostCommitCancellationTest` |
| Merkityn PDF:n vienti poistaa tyhjän kohteen | `PatternPdfExporterTest`, `PatternPdfExportDestinationRuntimeTest` |
| Kassaviesti ja WebView-navigaatio | `RavelryBrowserStateTest`, `RavelryBrowserNavigationTest` |
| Projektin poisto delegoi tuotannon polkuun, dokumentin irrotus | `ProjectDocumentRuntimeIntegrationTest`, `AuditRoomContractsTest`, `CounterHistoryRoomTest` |
| Aktiivisten projektien portti | `ActiveProjectPolicyRoomTest`, `ActiveProjectPolicyRuntimeTest` (profiili `-Pknittools.isolatedTest=activepolicy`) |
| Hylättyjen jaettujen PDF:ien siivous, perehdytyksen PDF säilyy | ei omaa testiä; tarkista käsin tai lisää laitetesti |

Lisäksi lint (`lc`) ei ole ajettu tämän päivän muutoksille.

## 3. Suunnitelmapäätökset, jotka muuttuivat

- **Kokeilun peukalointisuoja**: puuttuva `BOOT_COUNT` ei enää lukitse kokeilua, ja saman
  käynnistyksen aikainen seinäkellon oikaisu taaksepäin ei ole peukalointia. Käynnistysten yli
  taaksepäin siirto lukitsee edelleen. Päivitetty `PROJECT.md`:hen.
- **Työskentelyistunnon ajoitus**: ilman käynnistyslaskuria sama käynnistys tunnistetaan
  käynnissäoloajasta ja seinäkellosta viiden minuutin tarkkuudella. Päivitetty `PROJECT.md`:hen.
- **Ostotilan välimuisti**: `PurchaseOwnershipCache` tallentaa viimeisimmän vahvistetun omistuksen
  DataStoreen (`billing_ownership`). Varmuuskopiosäännöt jättävät sen pois.
- **Insightsin putki**: Insights hakee koko istuntohistorian kaikilla aikaväleillä, jotta putki ei katkea
  viikon tai kuukauden alkuun. Päivitetty `PROJECT.md`:hen.

## 4. Kattavuus

Kaikki osa-alueet on käyty läpi medium-tasolla. `PatternViewerScreen`in (noin 2 800 riviä)
Compose-koodia ei luettu rivi riviltä; sen tila kulkee tarkistettujen ViewModelien kautta.
