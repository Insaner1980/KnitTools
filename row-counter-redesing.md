# Row Counter -näkymän redesign-suunnitelma

## Yhteenveto

Toteutetaan vain `counter`-reitin row counter -näkymä niin, että ensimmäinen viewport on rauhallinen laskurityökalu: yläheader, keskitetty iso row counter -hero ja näkyvä alanavigaatio. Projektin kortit, stitch tracking ja lisälaskurit siirtyvät scrollin alle. Navigaatiomallia, ViewModelin dataflow’ta, repository-logiikkaa, widgettejä, Libraryä, Toolsia, Insightsia tai Settingsiä ei muuteta.

Tarkistetut lähteet ennen suunnittelua: Androidin [Scaffold-dokumentaatio](https://developer.android.com/develop/ui/compose/components/scaffold), [WindowInsets-ohje](https://developer.android.com/develop/ui/compose/system/insets), [Lazy lists and grids](https://developer.android.com/develop/ui/compose/lists?hl=en) ja [Compose Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3?authuser=9). Repon BOM käyttää Material3 `1.4.0`, joka on myös docsien mukaan uusin stable 19.5.2026.

## Toteutusmuutokset

- Päivitä [CounterScreen.kt](C:/Dev/KnitTools/app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt) niin, että yläosa näyttää back-nuolen, ison uppercase-projektinimen ja overflow-menun samassa header-kokonaisuudessa. Poista headerista pattern-subtitle kokonaan: ei `Pattern attached`, ei PDF-nimeä, ei Ravelry-nimeä.
- Säilytä ulomman `NavGraph`-Scaffoldin bottom bar nykyisellään. `Screen.Counter.route` ei saa päätyä `HIDE_BOTTOM_BAR_ROUTES`-listaan, eikä muihin tabeihin lisätä sticky/floating counteria.
- Päivitä [CounterWorkspaceSections.kt](C:/Dev/KnitTools/app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt): ensimmäinen LazyColumn-item on hero, jonka minimikorkeus täyttää headerin jälkeisen näkyvän alueen ennen alanavigaatiota. `ProjectContentCards`, stitch tracker ja extra counters alkavat vasta scrollin jälkeen.
- Siirrä `CounterButtons` hero-komponentin sisään. Heron järjestys: kompakti repeat/section-rivi vain jos aktiivinen, row label (`Row n / target` jos target on), iso row-numero, progress bar, kontrollirivi. Plus-painike pysyy puisena `R.drawable.plus_button`-nappina ja on selvästi suurempi kuin minus/undo.
- Aktiivinen due reminder säilytetään reminder-logiikan takia, mutta sitä ei näytetä “Next Reminder” -projektikorttina eikä pääkontrollien alla. Jos se renderöidään ensimmäisessä viewportissa, se on kompakti hero-tilaviestiosa, ei erillinen projektikortti.
- Siirrä `CounterStitchTracker` ja `ProjectCounterWorkspaceItem`-lisälaskurit alemmas: ensin `Project`-korttigrid, sitten `Extra Counters` vain jos niitä on, ja vasta sen jälkeen muut alemmat työtilasisällöt ellei olemassa oleva overflow/action sheet jo kata niitä paremmin.
- Lisää counterin mitoitukselle theme-paketin tokenit, esimerkiksi `ui/theme/CounterDimens.kt`, ja käytä niitä uusissa/muutetuissa hero-, grid-, spacing-, icon- ja touch-target-mitoissa. Käytä väreissä vain `MaterialTheme.colorScheme` ja `MaterialTheme.knitToolsColors`; älä lisää hardcoded-värejä.

## Projektikortit

- Muuta [CounterProjectContentCards.kt](C:/Dev/KnitTools/app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt) square action card -malliksi. `ProjectContentCard` kantaa vain `kind` + `titleRes`; poista preview-kentät (`bodyText`, `bodyRes`, `photoCount`, `reminderRow`, `reminderMessage`) ja niitä käyttävä renderöinti.
- `projectContentCards(state)` palauttaa aina viisi korttia tässä järjestyksessä: Pattern, Yarn, Notes, Photos, Reminders. Pattern-kortin title on `Open Pattern`, jos `patternUri` tai `linkedPattern` on olemassa, muuten `Add Pattern`. Muut title-arvot ovat aina `Yarn`, `Notes`, `Photos`, `Reminders`.
- Kortit sisältävät vain icon + title. Ei subtitleä, preview-tekstiä, chevronia, PDF-tiedostonimeä, lankojen nimiä, note-riviä, kuvamäärää, reminder-row’ta tai reminder-viestiä.
- Toteuta grid ilman sisäistä samaan suuntaan scrollaavaa `LazyVerticalGrid`iä. Käytä staattiselle viiden kortin joukolle `Column` + riveihin jaettua `Row`-rakennetta, `aspectRatio(1f)` ja tyhjää placeholder-paikkaa viimeiselle riville, jotta kaikki kortit pysyvät samankokoisina.
- Säilytä nykyiset click-flow’t: Pattern avaa PDF viewerin, pattern info -sheetin tai picker-flow’n nykyisen `onProjectContentClick`-päätöksen mukaan; Yarn avaa yarn managementin; Notes noudattaa nykyistä Pro-gateä ja notes flow’ta; Photos noudattaa progress photo Pro-gateä; Reminders avaa nykyisen reminder-listan/management-flow’n.

## Strings Ja Dokumentaatio

- Päivitä kaikki `values*/strings.xml`-tiedostot. Lisää tai korvaa no-pattern-kortin label semanttisesti arvoksi “Add Pattern”; poista käytöstä jäävät body-/next-reminder-korttitekstit vain jos mikään muu näkymä ei käytä niitä.
- Poista middle-dot-copy counterin tästä pinnasta: ei `"$name · Ravelry"` eikä `Row n · message` -tyyppisiä merkkijonoja row counter -kortteihin.
- Päivitä [PROJECT.md](C:/Dev/KnitTools/PROJECT.md) sekä samat counter workspace -kohdat [AGENTS.md](C:/Dev/KnitTools/AGENTS.md) ja [CODEX.md](C:/Dev/KnitTools/CODEX.md), koska nykyiset tekstit kuvaavat vanhaa preview-card- ja pattern-header-käyttäytymistä. Pidä mahdolliset nykyiset käyttäjän `PROJECT.md`-muutokset pohjana, älä ylikirjoita niitä.
- Älä päivitä memory/MEMORY.md tässä työssä: muutos on UI-rakenne, ei uusi arkkitehtuuri- tai dataflow-päätös, ja pysyvä muistipäivitys vaatii erillisen käyttäjäpyynnön.

## Testisuunnitelma

- Päivitä `CounterProjectContentCardsTest`: tyhjä ja täytetty state palauttavat aina viisi korttia; täytetty state ei vuodata `two_sleeves_one_promise.pdf`, `Isager Highland Wool`, note-preview’tä, kuvamäärää tai reminder-tekstiä korttimalliin; pattern-title vaihtuu vain open/add-tilan mukaan.
- Päivitä `CounterWorkspaceSourceTest`: `PatternHeaderRow` ja `project_header_pattern_attached` eivät ole enää counter workspace -headerissa; `ProjectContentCards` on `counter-hero`n jälkeen; `CounterButtons` ei ole erillinen LazyColumn-item; bottom nav -reittejä ei muuteta.
- Lisää lähdetesti, joka estää kiellettyjen mallien paluun: ei `CounterQuickActions`, ei `CounterProjectInfo`, ei `project_content_next_reminder` counter-korttimallissa, ei preview-kenttiä `ProjectContentCard`issa, ei middle-dot-joinia `CounterProjectContentCards.kt`:ssa.
- Aja toteutuksen jälkeen vähintään: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew :app:detekt`, `./gradlew lint`.
- Tee manuaalinen UI-tarkistus aktiivisella projektilla: ensimmäinen viewport näyttää vain headerin, ison row-counter-heron ja alanavigaation; kortit tulevat vasta scrollaamalla; grid-korteissa on vain icon + title; kaikki viisi korttia avaavat nykyiset flow’t; muissa tabeissa ei näy sticky/floating counteria.
