# Project Instructions

## Build & Test

- `./gradlew :app:generateBaselineProfile` — generoi Baseline Profile yhdistetyllä laitteella
- Erilliset, verkkoluvattomat testipaketit (`-Pknittools.isolatedTest=activepolicy|notes`, vain debug): pakettiin pääte `.activepolicytest`/`.notestest`, INTERNET- ja BILLING-oikeudet poistettu (`app/src/isolatedTest/AndroidManifest.xml`). `ActiveProjectPolicyRuntimeTest` ajetaan `activepolicy`-profiililla; se tyhjentää paketin tietokannan ja ohjaa osto- ja kokeilutilaa debug-only `ProPolicyTestControl`ista (`src/debugShared`; releasen vastine `src/release` vain välittää oikeat arvot, ei asettajia). Profiili kytkee pois debugin Pro-avauksen ja demodatan. `PatternNotesNavigationRuntimeTest` ajetaan `notes`-profiililla ja argumentilla `-Pandroid.testInstrumentationRunnerArguments.notesNavigationIsolated=true`. Älä lisää Pro- tai ostotilan ohituksia main-lähdekoodiin.

## Quality Tools (global, in ~/bin/)

- `lint-check` (alias `lc`) — runs ktlint + detekt + Android lint, results in `reports/`
- `security-check` (alias `sc`) — repo-local authoritative entrypoint is `tools/sc.ps1`, which delegates to the shared Android-check engine
- `security-check-full` (alias `sc-full`) — compatibility alias täydelle security-ajolle
- Run these scripts when requested directly or in a requested script list.
- `reports/` is gitignored, never commit it
- Security risk decisions and temporary exceptions are documented in `config/security-decisions.md`
- `scripts/security-check.sh` ja muut `scripts/security-check*.sh`-polut ovat vain fail-closed-yhteensopivuusdelegaatteja samaan `tools/sc.ps1`-entrypointiin.
- `sc` ajaa yhteisen manifestin mukaiset dependency-, secret- ja Semgrep-tarkistukset. Dependency-tarkistukset voi ohittaa vain erikseen: `sc -WithoutDeps` tai `./scripts/security-check.sh --without-deps`.
- Ensimmäinen OWASP dependency-check -ajo voi olla hidas, koska se alustaa CVE-tietokannan automaattisesti. `NVD_API_KEY` nopeuttaa NVD-päivitystä, jos sellainen on käytössä.
- `sonar` ajaa projektin SonarCloud-skannauksen Gradlen `sonar`-taskilla, joka ajaa ensin `:app:jacocoDebugUnitTestReport`-tehtävän, ja kirjoittaa lokin `reports/sonar.txt`; `sonar auth login/status/...` ohjautuu edelleen SonarQube CLI:lle.
- SonarCloud-skannaus tarvitsee `SONAR_TOKEN`-ympäristömuuttujan. SonarQube CLI:n keychain-kirjautumista käytetään issueiden lukemiseen `reports/sonar-issues.json`-raporttiin.

## Conventions

- Hilt for DI, Room for local DB, DataStore for preferences
- ViewModels expose StateFlow, screens collect via collectAsStateWithLifecycle()
- All user-visible strings use resources. Onboarding copy is in `res/values/strings_onboarding.xml`, with matching da, de, es, fi, fr, it, nb, nl, pt, pt-rBR and sv translations. Every key has a Finnish translator-context comment. Preserve placeholders and locale plural quantities; MissingTranslation suppression is not allowed. Add strings only with their consumers. `OnboardingResourcesSourceTest` enforces all configured locales.

- No hardcoded colors/dimensions — use theme tokens (`MaterialTheme.colorScheme.*` and `MaterialTheme.knitToolsColors.*`)
- No inline `letterSpacing`, `fontSize`, or `fontWeight` overrides — use Type.kt roles. Exception: CounterScreen's main number (115sp Bold)
- Fontti on Barlow Semi Condensed (`KnitToolsFontFamily`, SIL OFL, lisenssi `assets/licenses/barlow_semi_condensed_ofl.txt`) kapeutensa vuoksi; leveä Outfit katkaisi projektien nimiä. Projektin nimeä ei muuteta versaaleiksi (myös laskurin yläpalkki näyttää nimen kuten projektinäkymä).
- Finnish in commit messages and comments

- Julkaisuhinta tulee Playn olemassa olevan knittools_pro-kertaoston voimassa olevasta launch-tarjouksesta. Sama tilannekuva sitoo hinnan, valuutan, ostotunnuksen ja päättymishetken. Hintanäkymät päivittyvät palattaessa, ja päättynyt tarjous mitätöidään ennen uutta hakua. Muuttuneen hinnan osto vaatii uuden napautuksen. ProPriceDetails jakaa esityksen Pro-sivulle, kehotteeseen ja kokeilun päättymisilmoitukseen. Puuttuva hinta ei estä ilmaiskäyttöä tai vapaaehtoista 14 päivän kokeilua. Kieli ei valitse valuuttaa. Julkaisukampanja kestää julkisesta julkaisusta yhden kalenterikuukauden Europe/Helsinki-ajassa; päivämäärät ovat vielä asettamatta. Kokeilu ei varaa kampanjahintaa. Nykyinen sisällön käyttö- ja muokkausoikeus sekä debugin ominaisuusavaus säilyvät. Aloitussivun Pro-tieto perustuu ladattuun ProStatus-tilaan, ja ostaneille ei näytetä mainosta. Henkilökohtainen yhteenveto ei korvaa sisällön säilymisestä kertovaa tekstiä.

## UI Rules

- UI-kohtaiset säännöt ovat tiedostossa `.claude/rules/ui.md` (latautuu automaattisesti UI-tiedostojen kanssa).
- Navigaatio: 5 tabia (Projects, Library, Tools, Insights, Settings). Sovellus käynnistyy Projects-tabista. Room v29.
- Projektin luettavien PDF-tiedostojen, ensisijaisuuden, järjestyksen ja dokumenttikohtaisen lukutilan auktoriteetti on `project_documents` ja `ProjectDocumentRepository`; `counter_projects`-taulun vanhat pattern- ja lukutilakentät ovat vain migraatioyhteensopivuutta, eivät rinnakkainen lähdetotuus.
- Suomenkielinen käyttöliittymä kutsuu neuleohjetta aina sanalla “ohje”, ei “kaava” (kaava tarkoittaa suomeksi ompelukaavaa): “Ohje ja dokumentit”, “Liitä ohje”, “Tallennetut ohjeet”. Verkkolinkillä tallennettu ohje on käyttöliittymässä verkko-ohje: käytä termejä “Lisää verkko-ohje”, “Verkkosivun osoite”, “Avaa verkkosivu” ja projektissa “ohjeen tiedot”. Älä kutsu linkkiä PDF:ksi, offline-ohjeeksi, lataukseksi, tuonniksi tai synkronoinniksi. Verkkosivu avataan alkuperäisellä sivustolla sovelluksen ulkopuolella; HTTP-osoite näyttää varoituksen, ja Ravelry-kaavalinkki avautuu sovelluksen Ravelry-selaimeen. Projektiin liittäminen, korvaaminen ja irrottaminen koskevat vain kaavan tietoja eivätkä projektin PDF-dokumentteja; korvaus vaatii nimenomaisen vahvistuksen.
- Destruktiivinen repository-polku kerää siivoustiedot ennen transaktiota, kommitoi auktoritatiivisen tietokantapoiston ensin ja siivoaa app-owned-tiedostot vasta sen jälkeen best effort -periaatteella. Tietokantavirhe tai kommittia edeltävä peruutus säilyttää tiedostot; kommitin jälkeinen tiedostovirhe saa jättää orvon mutta ei saa esittää tietokantapoistoa peruttuna. Jaetut PDF:t kulkevat aina `SavedPatternRepository.deleteLocalPatternFileIfUnused`-portin kautta.
- Työskentelyistunto alkaa vain projektiaktioiden Start-toiminnosta. Stop avaa yhteenvedon, jossa istunto tallennetaan, hylätään tai jätetään aktiiviseksi. Projektin avaaminen, vaihtaminen, historia, taustalle siirtyminen tai ViewModelin poistuminen ei käynnistä, jaa eikä päätä aikaa. Aktiivinen tila näkyy projektirivillä ja counter-heron lähellä kompaktina tekstirivinä ilman reunusta, gradienttia, hehkua tai väriraitaa. Epävarma väli pyydetään kerran omassa dialogissa: Add hyväksyy ehdotuksen ja jatkaa, Discard jättää epävarman välin pois ja päättää, Edit tallentaa käyttäjän antaman kokonaisajan ja päättää. Dialogin sulkeminen jättää asian ratkaistavaksi passiivisesta tilarivistä.
- Voice- ja mikrofonikomennot puuttuvat tarkoituksella. Älä palauta `SpeechRecognizer`-, `TextToSpeech`- tai keskustelevaa voice-flow'ta ilman uutta nimenomaista tuote- ja turvallisuuspäätöstä.
- Pro: käyttäjä aloittaa 14 päivän kokeilun itse. Ilman ostettua Prota tai aktiivista kokeilua käyttäjä voi luoda tai aktivoida projektin uudelleen, kun aktiivisia projekteja ei ole. Valmistuneet projektit eivät kuluta ilmaista kiintiötä, ja ne saa säilyttää rajatta. Aiemmat useat aktiiviset projektit pysyvät normaalisti käytettävinä; vain uuden aktiivisen projektin lisääminen on portin takana. Olemassa oleva sisältö pysyy käytettävissä oikeuden päätyttyä; portit koskevat vain uusia Pro-objekteja ja edistyneitä esitystapoja. Kontekstikehote säilyttää odottavan toiminnon, eikä navigoi hiljaa yleiselle ostosivulle. Insights laskee `hasMeaningfulChartData`-arvon luovuttamatta kaaviopisteitä ilmaiselle UI:lle ja näyttää Pro-kehotteen vain vähintään kahdella ei-tyhjällä korilla. Kehote on muun Insightsin tavoin kortiton editoriaalinen sisältö hiusviivojen välissä.
- Notes: bottom sheet + full-screen editor (`notes_editor/{projectId}`), `NotesEditorViewModel` ja paikallinen auto-save. Project note replacement kulkee `CounterRepository.saveProjectNotes`-polun kautta, jotta rinnakkaiset editorivirrat eivät ylikirjoita toisiaan.
- Notes editing is local-only; älä palauta cloud-journalia, AI-siistintää tai voice-transcript-journalointia ilman uutta nimenomaista päätöstä.
- Projektin langankäyttö kuuluu nykyiseen Yarn-sheetiin, ei uuteen projektilaattaan tai välilehteen. Planned, Allocated to this project ja Used ovat erillisiä käyttäjän määriä; Remaining tarkoittaa vain projektille varatusta langasta jäljellä olevaa osuutta, ja Over by kertoo ylityksen tekstinä. Älä anna ymmärtää, että My Yarn -varastosaldo vähenee. Muistiinpano ja siitä tallennettu lankakortti näyttävät yhden käyttöyhteenvedon; lähteettömän kirjauksen voi yhä muokata tai poistaa. Käyttöseuranta on ilmainen myös valmistuneissa projekteissa. Säilytä viisi projektin sisältölaattaa ja nykyiset langan lisäys- ja linkitystoiminnot.
- Ravelry API: Androidissa ei ole Ravelry-API-asiakasta, OAuth-kirjautumista, `knittools://ravelry-auth-complete`-callbackia eikä Firebase Auth/Functions -riippuvuuksia (poistettu 2026-10-09; yksi kirjautuminen Ravelryn omalla sivulla sovelluksen selaimessa). `functions/`-backend on repossa, mutta sovellus ei kutsu sitä. Älä palauta API-hakua tai OAuthia ilman uutta päätöstä; secretit ja token exchange kuuluisivat silloinkin palvelimelle. Tallennettujen ohjeiden Ravelry-rivit säilyvät. Saved patterns säilyttävät schema 19:ssä käyttöön otetun kanonisen `free`/`paid`/`unknown`-saatavuuden; etäkuvat käyttävät yhtä HTTPS-rajattua Coil-verkkopolkua ja poistuvat hiljaisesti asettelusta latausvirheessä. Todellista Firebase/Ravelry-käyttöönottoa tai päästä päähän -ajoa ei pidä päätellä paikallisista testeistä.

## Google Play

- SplashScreen: `installSplashScreen()` ennen `super.onCreate()`; yksi kiinteä brändisplash tulee `values/themes.xml`-tiedostosta ilman järjestelmän light/dark-resurssihaaraa, ja varsinainen sovellusteema ratkaistaan `PreferencesManager.preferences`-arvosta ennen splashin poistamista
- In-App Review: triggerit Pro-osto tai 20+ laskuritoimintoa, pyydetään kerran (DataStore)
- In-App Updates: flexible mode, checkForUpdate (onCreate), checkDownloadedOnResume (onResume)
- Baseline Profiles: `:baselineprofile`-moduuli, generoi `./gradlew :app:generateBaselineProfile`
