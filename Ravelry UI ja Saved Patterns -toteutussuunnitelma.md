# Ravelry UI ja Saved Patterns -toteutussuunnitelma

## Yhteenveto
- UI ei ole vielä dokumentin mukainen. Backend/auth/schema-pohja on valmiina, mutta `config/ravelry-backend-progress.md` sanoo suoraan, että Phase 8 eli URL/share-import ja saved-pattern UX on aloittamatta.
- Toteutus tehdään ilman uutta Room-schemaa: käytetään schema 14:n `source`, `ravelryPatternId`, `canonicalUrl`, `originalUrl`, `localPdfUri`, `isAvailableOffline` sekä projektien olemassa olevaa `linkedPatternId`-kenttää.
- Säilytetään Tools > Ravelry näkyvänä, yksi yhteinen `Saved Patterns` -kirjasto, Auth Tab / Custom Tabs -auth, ei WebView’ta, ei Androidiin Ravelry-secretiä, ei PDF-download-väitteitä.
- Preflight: nosta `androidx.browser:browser` versiosta `1.9.0` versioon `1.10.0`, koska virallinen AndroidX Browser -sivu listaa sen uusimmaksi vakaaksi versioksi 25.3.2026.

## Rajapinnat
- Lisää `RavelryApiService.importPatternByUrl(url: String)` ja `RavelryRepository.importPatternByUrl(url: String)` delegoimaan olemassa olevaan `RavelryBackendClient.importPatternByUrl`.
- Lisää `RavelryRepository.findDuplicateFor(detail: PatternDetail): SavedPattern?` ja keskitetty `PatternDetail -> SavedPattern` -mapperi, jotta import preview, save ja duplicate-logiikka eivät kopioidu.
- Muuta `CounterProjectDao.updatePattern(...)` ottamaan myös `linkedPatternId: Long?`; kaikki pattern-attach/detach-polut päivittävät `linkedPatternId`, `patternUri`, `patternName`, `currentPatternPage` ja `patternRowMapping` samassa kirjoituksessa.
- Lisää `CounterRepository.attachSavedPattern(projectId: Long, savedPatternId: Long): SavedPattern?`: jos `localPdfUri` on olemassa, projekti avaa PDF:n; muuten projekti linkittää metadata-only Ravelry-patternin `linkedPatternId`:llä.
- Lisää navigaatioon `Screen.RavelryImport(encodedUrl)` ja `Screen.SavedPatternDetail(savedPatternId)`. Saved Patterns -listat avaavat jatkossa detail-ruudun, eivät suoraan lähdepohjaisia routeja.

## Toteutusvaiheet
- [x] Preflight: tarkista dirty worktree, selaa nykyiset Ravelry- ja saved-pattern-testit, päivitä Browser `1.10.0`:aan, aja dependency verification metadata -päivitys vain Browser-artefaktille.
- [x] Ravelry header: korvaa nykyinen iso `RavelrySignInPrompt` kompaktilla account headerilla. Not connected näyttää selityksen ja `Sign in with Ravelry`; connected näyttää `Connected as username` ja `Browse Ravelry`; `Disconnect` siirtyy overflow/account-sheetteihin.
- [x] Search-tab: jätä hakukenttä näkyviin, mutta disabloi se kun auth ei ole `Connected`; näytä `Sign in with Ravelry to search patterns.` Saved Patterns -tab pysyy käytössä myös disconnected-tilassa.
- [x] Search-result cards: lisää korttiin 48dp-minimitäyttävä action slot. Jos `PatternSearchResult.id` löytyy nykyisistä `SavedPattern.ravelryPatternId`-arvoista, näytä `Saved`/`Open`; muuten näytä `Save Pattern`, joka avaa import-vahvistuksen eikä tallenna hiljaa.
- [x] Import confirmation: tee yksi bottom sheet / screen hakutulokselle ja jaetulle URL:lle. Tilat: `Loading`, `Ready`, `AlreadySaved`, `NeedsSignIn`, `CouldNotImport`, `BackendUnavailable`. Primary action on `Save Pattern`; duplicate-tila avaa olemassa olevan `SavedPatternDetail`.
- [x] Share target: lisää `MainActivity`lle `ACTION_SEND` + `text/plain` intent-filter. Käsittele vain validoituja Ravelry pattern URL:eja, tyhjennä kulutettu intent, älä anna share-intentin laukaista counter-navigointia.
- [x] Browse Ravelry: lisää connected-tilaan `Browse Ravelry`, joka avaa `https://www.ravelry.com/patterns/search` Custom Tabsilla ja `SHARE_STATE_ON`:lla. Ensimmäisessä UI-vaiheessa `Save to KnitTools` toteutuu Android Sharesheet -tuonnilla, ei erillisellä Custom Tab toolbar -painikkeella.
- [x] SavedPatternDetail: näytä title, designer, thumbnail, availability (`PDF attached`, `Available offline`, `Open on Ravelry`, `Requires Ravelry` vain kun hyödyllinen), sekä toiminnot `Open Pattern` / `Open on Ravelry`, `Attach to Project`, `Remove`.
- [x] Attach to Project: muuta `PatternPickerSheet` näyttämään kaikki saved patternit, ei vain `localPdfUri`-rivejä. Valinta kutsuu `CounterRepository.attachSavedPattern`; `Import from Ravelry` navigoi Ravelry import/search -polkuun; `Attach PDF from device` käyttää nykyistä SAF PDF -polkua.
- [x] Counter/project integraatio: pattern-kortin visuaalinen sääntö säilyy AGENTS.md:n mukaan icon+title-only. Klikkaus avaa PDF-viewerin, jos `patternUri` on olemassa; muuten se avaa `SavedPatternDetail` metadata-only linkille.
- [x] Lokalisointi ja copy: lisää uudet stringit kaikkiin `values-*`-tiedostoihin käyttäen nykyistä Ravelry/Saved Patterns -sanastoa. Älä lisää source-kategoriaotsikoita kuten “Saved from Ravelry”.
- [x] Dokumentit: koska data flow ja navigaatio muuttuvat, päivitä `AGENTS.md`, `CODEX.md`, `PROJECT.md`, `memory/MEMORY.md` ja `config/ravelry-backend-progress.md` Phase 8 -valmiiksi.

## Testisuunnitelma
- Unit/source-testit: lisää RavelryViewModel-testit import URL/search-result confirmationille, duplicate-polulle, 401/403/412 sign-in mappingille, backend unavailable -tilalle ja repeated save -suojalle.
- Repository-testit: lisää `CounterRepository.attachSavedPattern`-testit paikalliselle PDF:lle, metadata-only Ravelrylle, vanhan PDF:n cleanup-portille ja transaktiokirjoitukselle.
- Source-testit: varmista että `PatternPickerSheet` ei enää suodata vain `localPdfUri`-rivejä, manifestissa on `ACTION_SEND text/plain`, WebView’ta ei tule, saved list ei käytä source-kategorioita ja Disconnect ei ole pääpainike connected-tilassa.
- Komennot: `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest --rerun-tasks`, `.\gradlew.bat --no-configuration-cache :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck`, `.\gradlew.bat --no-configuration-cache :app:kspDebugKotlin`, `npm --prefix functions test`, `git diff --check`.
- Huomio: nykyisessä dirty worktreessä `R.drawable.pro_upgrade` näyttää yhä mahdolliselta compile-blokkerilta; älä tulkitse sitä Ravelry-regressioksi, ja final verification tarvitsee sen erillisen blocker-tilan ratkaistuksi.

## Oletukset ja lähteet
- Ei uutta schemaa, ei Drive/Dropbox SDK:ta, ei sync-copya, ei automaattista tallennusta jokaiselta vieraillulta Ravelry-sivulta.
- Custom Tabin oma `Save to KnitTools` -toolbar/menu jätetään tämän vaiheen ulkopuolelle; Androidin share receive -ohjeen mukainen Sharesheet-polku kattaa dokumentin URL-import-vaatimuksen pienemmällä riskillä.
- Tarkistetut lähteet: [AndroidX Browser release notes](https://developer.android.com/jetpack/androidx/releases/browser), [Chrome Auth Tab](https://developer.chrome.com/docs/android/custom-tabs/guide-auth-tab), [Custom Tabs interactivity](https://developer.chrome.com/docs/android/custom-tabs/guide-interactivity), [Android receive shared data](https://developer.android.com/training/sharing/receive), [OAuth 2.0 for Native Apps RFC 8252](https://datatracker.ietf.org/doc/html/rfc8252), [Ravelry patterns search](https://www.ravelry.com/patterns/search), [Ravelry Goodies/API entry](https://www.ravelry.com/about/goodies).
