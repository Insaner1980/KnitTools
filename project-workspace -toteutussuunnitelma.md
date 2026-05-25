# KnitTools Project Workspace -toteutussuunnitelma

## Yhteenveto

Tavoite on muuttaa row counter aktiivisen projektin työtilaksi ilman visuaalista redesignia: laskuri pysyy ensimmäisen näkymän pääasiana, mutta ohje, langat, muistiinpanot, kuvat, muistutukset ja lisälaskurit näkyvät selkeinä projektikontekstin osina. Mikrofonitoiminto poistetaan kokonaan.

Toteutus tehdään neljässä erässä: ensin poistetaan voice/mic-pinta, sitten rakennetaan scrollaava counter workspace, sitten lisätään projektikohtainen lankamuistiinpano ja lopuksi hiotaan Projects/Library/Pro-copyt sekä testit. Compose-rakenne tehdään virallisten Compose-ohjeiden mukaisesti: screen state ViewModelissa, UI-eventit lambdoina ja scrollaava sisältö yhtenä `LazyColumn`ina, ei sisäkkäisiä pystyscrollauksia. Lähteet: [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting), [Compose state](https://developer.android.com/develop/ui/compose/state), [lazy lists](https://developer.android.com/develop/ui/compose/lists), [performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices).

## Keskeiset muutokset

- Poista voice kokonaan: `VoiceCommandHandler`, `VoiceCommandParser`, `VoiceResponseManager`, `CounterVoiceSummaryItem`, mikrofoni-UI, `RECORD_AUDIO`, `ProFeature.VOICE_COMMANDS`, voice-stringit ja voiceen sidotut testioletukset.
- Säilytä ei-AI-paikalliset ominaisuudet: regex-pohjainen `InstructionParser`, notes autosave, pattern PDF, photo gallery, row reminders, project counters ja yarn card -editointi.
- Muuta `CounterScreenContent` ei-scrollaavasta keskitetystä `Column`-rakenteesta `LazyColumn`-työtilaksi:
  - Header: Back, projektin nimi, More.
  - Pattern line: PDF-nimi tai `Attach pattern`.
  - Counter hero: repeat/secondary, row label, iso numero, progress, stitch tracker, minus/plus/undo.
  - Quick Actions 2x2: Pattern, Notes, Yarn, Photos.
  - Project Info: näytä vain olemassa oleva tieto; tyhjälle projektille kompaktisti “Add to this project”.
  - Extra Counters: näytä vain jos `projectCounters` ei ole tyhjä.
  - Manage: history, rename, reset, complete/archive, delete alemmas tai More-sheetiin.
- Siirrä yläpalkin pattern/photo-toiminnot Quick Actionsiin; yläpalkkiin jää vain back + more. More-sheet jää hallinnollisille ja harvemmin käytetyille toiminnoille.
- Lisää counter UI:lle selkeät esitysmallit ilman uutta Room-dataa: `CounterWorkspaceSummary`, `CounterQuickAction`, `ProjectInfoRow`.
- Lisää projektikohtainen lankamuistiinpano Room v12: `project_yarn_notes` taulu, jossa `id`, `projectId`, `name`, `quantity`, `notes`, `photoUri`, `createdAt`, `updatedAt`, `savedYarnCardId?`.
- `YarnCardRepository` säilyy My Yarnin lähteenä; uusi `ProjectYarnNoteRepository` omistaa projektikohtaiset yarn notes -rivit. “Save to My Yarn” luo `YarnCard`in ja päivittää `savedYarnCardId`.
- Counterin Yarn Quick Action avaa valinnan: `Choose from My Yarn` tai `Add yarn to this project`. Ensimmäinen käyttää nykyistä `updateLinkedProjectId`, toinen tallentaa `project_yarn_notes`.
- My Yarn pysyy Library > My Collection -kohdassa. Päivitä copy muotoon “Yarn notes, quantities, and linked projects”; älä lupaa automaattista brand/weight/color-tunnistusta.
- Pattern camera scan -sanasto muutetaan rehelliseksi kuvaus/PDF-kieleksi: esimerkiksi `Take pattern photo` / `Pattern photo could not be converted to PDF`. Älä käytä “scan”-sanaa UI-copyssa.
- Projects-listan korttihierarkia: projekti ensin, sitten section/status, sitten row/date, lopuksi yarn/photo/notes-indikaattorit. Pattern filename ei saa nousta projektin nimen alle, ellei section puutu.
- Continue Knitting -korttiin lisätään korkeintaan yksi kontekstirivi: section name jos on, muuten row/target/repeat/time. Ei tehdä siitä dashboardia.
- Päivitä `AGENTS.md`, `CODEX.md` ja projektin repo-memory-dokumentti, koska voice poistuu arkkitehtuurista ja uusi projektikohtainen yarn note -virta muuttaa data flow’ta.

## Toteutuserät

1. Voice/mic-poisto:
- Poista `RECORD_AUDIO` manifestista.
- Poista `ProFeature.VOICE_COMMANDS` ja Pro-upgrade-listasta `pro_feature_voice_commands`.
- Poista `canUseVoiceCommands`, `canStartClassicVoice`, `emitLocalVoiceFeedback`, voice response flow ja kaikki `VoiceCommand*`-viittaukset `CounterViewModel`ista ja `CounterScreen`istä.
- Poista voice-lähdetiedostot ja voice-stringit kaikista `values*`-kansioista.
- Päivitä tai poista testit: `FeatureGateRaceSourceTest` voice-case pois, `ProFeatureGateSourceTest` voice-odotukset pois, `CounterStitchTrackingSourceTest` voice reset -case pois, `AiRemovalUiSourceTest` tarkistamaan ettei voice/mic-stringejä jää UI:hin.

2. Counter workspace:
- Jaa nykyisestä `CounterScreen.kt`istä uudet pienet composablet samaan `ui/screens/counter`-pakettiin: `CounterWorkspaceSections.kt`, `CounterQuickActions.kt`, `CounterProjectInfo.kt`.
- Muuta sisältö yhdeksi `LazyColumn`iksi, käytä `contentPadding = scaffoldPadding + horizontal 24.dp`, `verticalArrangement = spacedBy(...)`, ja anna dynaamisille riveille vakaat keyt.
- Lisää Quick Actions -laatat 2x2 gridinä olemassa olevilla theme-tokeneilla. Labelit:
  - Pattern: `Open Pattern` / `Attach Pattern`
  - Notes: `Notes` / `Add Note`
  - Yarn: `Yarn` / `Add Yarn`
  - Photos: `Photos` / `Add Photo`
- Lisää Project Info -rivit:
  - Pattern: PDF-nimi tai Ravelry metadata, jos hyödyllinen
  - Yarn: linked yarn + project yarn note summary
  - Notes: ensimmäinen ei-tyhjä rivi + “2 notes” vain jos toteutetaan erilliset notes myöhemmin; v1: koko project notes -kentästä ensimmäinen rivi
  - Reminder: lähin aktiivinen tuleva reminder nykyisestä rivistä
  - Photos: `1 photo` / `%d photos`
- Lisää Extra Counters inline-osioon vain `projectCounters`-listalle; legacy `secondaryCount` jää counter hero -alueelle.
- More-sheetistä poistetaan päivittäiset Quick Actions -duplikaatit tai jätetään vain fallbackit, jos toiminto ei mahdu työtilaan.

3. Projektikohtainen yarn:
- Room v12: lisää `ProjectYarnNoteEntity`, DAO ja schema export.
- Repository: `ProjectYarnNoteRepository.observeForProject(projectId)`, `save(note)`, `delete(id)`, `saveToMyYarn(noteId)`.
- CounterViewModel: lisää `projectYarnNotes: List<ProjectYarnNote>` stateen ja eventit `saveProjectYarnNote`, `deleteProjectYarnNote`, `saveProjectYarnNoteToMyYarn`.
- UI: Yarn Quick Action näyttää bottom sheetin, jossa kaksi valintaa: `Choose from My Yarn` ja `Add yarn to this project`; project-only lomake kysyy vain nimi/kuvaus, quantity ja notes.
- Project Info yhdistää summaryksi ensin linked My Yarn -kortit, sitten project-only yarn notes. Tyhjässä tilassa ei näytetä virhettä.

4. Copy, listat ja dokumentit:
- Päivitä My Yarn -description ja empty state kaikkiin `values*`-kansioihin, vähintään base + fi; jos kaikkia lokalisaatioita ei käännetä käsin, poista vain poistettavat avaimet kaikista ja anna uusien avainten fallbackata baseen.
- Poista dead `YarnLabelPhotoStorage`, jos yksikään live-koodi ei käytä sitä; säilytä `file_paths.xml` yarn_photos vain jos `AppFileStorage` tarvitsee vanhojen kuvien cleanupia.
- Poista `AppLanguage.promptLanguageName()` tai nimeä se AI-vapaaksi, jos se on oikeasti käyttämätön.
- Projects-listan `ProjectCard` saa uuden orderin: name, section/status, row/date, yarn/photo/notes. Pattern näytetään vain attachment-indikaattorina tai kun section puuttuu.
- `ContinueKnittingProject` saa `sectionName`, `targetRows`, `repeatSummary?` vain jos data on jo saatavilla ilman raskasta uutta queryä.
- `AGENTS.md` ja `CODEX.md`: poista voice-arkkitehtuuririvi, lisää project yarn notes -data flow ja päivitä camera wording -linja.

## Testisuunnitelma

- Lisää lähdekooditason regressiot:
  - Ei `RECORD_AUDIO`, `SpeechRecognizer`, `TextToSpeech`, `VoiceCommand`, `voice_`, `Mic`, `VOICE_COMMANDS` live-lähteissä.
  - Ei UI-copya sanoilla `AI`, `Gemini`, `scanner`, `scan yarn`, `yarn label scan`; pattern camera copy saa käyttää vain photo/PDF-kieltä.
  - Counter top bar ei sisällä mic/pattern/photo-ikonipolkuja, vaan Quick Actions -stringit.
- Päivitä nykyiset Pro/gate-testit vastaamaan voice-poistoa ja pattern camera -uusia stringejä.
- Lisää Room migration -testi v11 -> v12: vanhat `yarn_cards`, `counter_projects`, `project_counters`, `row_reminders` ja `progress_photos` säilyvät; uusi `project_yarn_notes` on tyhjä ja indeksoitu `projectId`:llä.
- Lisää repository-testit:
  - Project yarn note tallentuu projektikohtaisesti.
  - “Save to My Yarn” luo `YarnCard`in, linkittää projektiin ja säilyttää project note -viitteen.
  - Projektin poisto poistaa project yarn notes -rivit.
- Lisää ViewModel-/source-testit:
  - Quick Action labelit muuttuvat attached/missing-tilan mukaan.
  - Project Info näyttää vain olemassa olevat rivit ja tyhjän projektin helperin.
  - Lähin reminder valitaan nykyisestä tai seuraavasta rivistä.
  - Project card order ei nosta PDF filenamea nimen alle sectionin ohi.
- Aja toteutuksen jälkeen:
  - `./gradlew.bat --no-daemon test --console=plain`
  - `./gradlew.bat --no-daemon :app:detekt --console=plain`
  - `./gradlew.bat --no-daemon assembleDebug --console=plain`
  - älä aja käyttäjän `lc` tai `sc` wrappereita.

## Oletukset ja rajaukset

- Mikrofonitoiminto poistetaan kokonaan, ei piiloteta Pro-gaten taakse.
- Pattern camera -ominaisuutta ei poisteta, koska se on PDF/photo-työkalu, mutta kaikki “scan”-sanasto vaihdetaan ei-AI-lupaukseen.
- Project-only yarn ei näy My Yarn -listassa ennen “Save to My Yarn” -toimintoa.
- Row counterista ei tehdä erillistä Project Overview -näyttöä v1:ssä; työtila rakennetaan suoraan counter screeniin.
- Ei muuteta väripalettia, typography-suuntaa, bottom navia, plus-painikkeen visuaalista ilmettä tai appin yleistä KnitTools-identiteettiä.
- Ei committeja ilman erillistä pyyntöä.
