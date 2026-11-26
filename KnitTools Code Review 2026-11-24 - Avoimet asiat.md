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

## 1. Ravelryn kassan tunnistus (tehty 2026-11-25)

Todennettu Pixel 9:llä oikealla tilillä ostamatta. WebView-debuggauksella kirjatut ostolomakkeet:

- Buy now: `POST /stores/<kauppa>/products/<id>/checkout`
- Add to cart: `POST /carts/<id>/checkout`
- Send as gift: `POST /stores/<kauppa>/products/<id>/purchase_gift` (lisätty tunnistukseen)

Kaikki kolme pysäytetään sovelluksessa. Käyttäjän selain avaa sivun, jolta osto alkoi, koska
selaimessa ei ole sovelluksen kirjautumista ja kassaosoite näytti siellä virhe- tai kirjautumissivun.
Ostoskorin ja kassan jälkeinen polku (`/carts/<id>`, `paypal.com/checkoutnow`) kulki selaimessa.
Ravelryn kirjallinen lupa sovelluksen selaimelle pyydetään yhä ennen Play-julkaisua.

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
