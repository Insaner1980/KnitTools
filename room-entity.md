# Room Entity -irrotuksen vaiheittainen refaktorointisuunnitelma

## Yhteenveto

Tavoite: Room entityt jäävät `data/local`-kerroksen sisäisiksi tietokantamalleiksi, eikä `ui/`, `domain/calculator/` tai repositoryjen julkinen API enää vuoda `*Entity`-tyyppejä sovelluksen muihin kerroksiin.

Perustelu: Androidin arkkitehtuuriohjeissa Room entity kuvaa tietokantataulun riviä, repository toimii datakerroksen sisäänkäyntinä, ja UI/ViewModel muuntaa datan renderöitäväksi UI-tilaksi. Lähteet: [Room entities](https://developer.android.com/training/data-storage/room/defining-data), [Data layer](https://developer.android.com/topic/architecture/data-layer), [UI layer](https://developer.android.com/topic/architecture/ui-layer).

Toteutustapa: vaiheittain kaikki entityt, mutta yksi domain-alue kerrallaan. Ei tietokantaskeeman muutoksia, ei migraatioita, ei käyttäytymismuutoksia. Room entityjen kenttien nimet ja oletusarvot säilyvät entityissä ennallaan.

## Keskeiset muutokset

- Luo domain-mallit `domain/model`-pakettiin näille käsitteille:
  `CounterProject`, `ProjectCounter`, `RowReminder`, `ProgressPhoto`, `SavedPattern`, `YarnCard`, `KnitSession`, `PatternAnnotation`.
- Luo mapperit `data/local`-pakettiin tai sen alle, esimerkiksi `EntityMappers.kt`.
  Mapperit ovat ainoa sallittu paikka, jossa `Entity -> domain model` ja `domain model -> Entity` -muunnokset tehdään.
- Muuta repositoryjen julkiset metodit palauttamaan ja vastaanottamaan domain-malleja, ei entityjä.
  DAO-metodit pysyvät entity-pohjaisina.
- Päivitä `domain/calculator` käyttämään domain-malleja.
  Nykyiset vuodot ovat ainakin `ProjectCounterLogic -> ProjectCounterEntity` ja `ReminderLogic -> RowReminderEntity`.
- Päivitä ViewModelit ja composablet käyttämään domain-malleja.
  Suurimmat alueet ovat `counter`, `library`, `project`, `insights`, `session`, `yarncard`, `ravelry` ja `pattern`.

## Toteutusvaiheet

1. Baseline ennen refaktorointia
- Aja pienin hyödyllinen tarkistus: `./gradlew test`.
- Kirjaa nykyiset testivirheet erikseen, jos niitä on.
- Älä aja `lint-check` tai `security-check`, koska projektiohje sanoo että käyttäjä ajaa ne itse.

2. Lisää domain-mallit ja mapperit ilman kuluttajien muutosta
- Lisää domain-mallit samoilla kentillä kuin nykyisissä entityissä, mutta ilman Room-annotaatioita.
- Säilytä nimet käyttäjäkäsitteinä: `KnitSession` entityn `SessionEntity` sijaan, jotta vältetään yleisnimi `Session`.
- Lisää mapperit:
  `CounterProjectEntity.toDomain()`, `CounterProject.toEntity()`, jne.
- Lisää mapper-yksikkötestit jokaiselle mallille.
- Hyväksymiskriteeri: mapperit säilyttävät kaikki kentät, mukaan lukien `id`, aikaleimat, nullable-kentät, `yarnCardIds`, `patternUri`, `counterType`, `status`, `repeat*` ja `currentRepeat`.

3. Irrota pienimmät repositoryt ensin
- Aloita `SavedPatternRepository`, `ProgressPhotoRepository`, `YarnCardRepository` ja `PatternAnnotationRepository`.
- Päivitä niiden testit käyttämään domain-malleja repositoryrajalla.
- DAO-testit saavat edelleen käyttää entityjä.
- Päivitä vain näiden repositoryjen suorat UI/ViewModel-kuluttajat.
- Hyväksymiskriteeri: näiden repositoryjen public API:ssa ei näy `SavedPatternEntity`, `ProgressPhotoEntity`, `YarnCardEntity` tai `PatternAnnotationEntity`.

4. Irrota counterin apumallit
- Muuta `ProjectCounterRepository`, `ReminderRepository`, `ProjectCounterLogic` ja `ReminderLogic` domain-malleihin.
- Päivitä `CounterViewModel`, `MultiCounterComponents` ja `ReminderComponents` käyttämään `ProjectCounter` ja `RowReminder`.
- Pidä `counterType` ja `status` tässä vaiheessa stringeinä, jotta refaktorointi ei muuta käyttäytymistä. Mahdollinen enum-tyypitys tehdään vasta erillisenä refaktorina.
- Hyväksymiskriteeri: `domain/calculator` ei importtaa `data.local`-pakettia.

5. Irrota pääprojektit ja sessiot viimeisenä
- Muuta `CounterRepository` palauttamaan `CounterProject` ja `KnitSession`.
- Päivitä `CounterViewModel`, `ProjectListViewModel`, `InsightsViewModel`, `SessionHistoryViewModel`, `LibraryViewModel` ja niihin liittyvät composablet.
- Erityistä huomiota:
  `updatedAt`, `createdAt`, `completedAt`, `isCompleted`, `linkedPatternId`, `pattern*`, `stitchTrackingEnabled`, `currentStitch`, `startRow`, `endRow`, `durationMinutes`.
- Hyväksymiskriteeri: `ui/` ei importtaa `com.finnvek.knittools.data.local.*`.

6. Viimeistely ja dokumentointi
- Aja `rg "data\\.local|.*Entity" app/src/main/java/com/finnvek/knittools/ui app/src/main/java/com/finnvek/knittools/domain app/src/main/java/com/finnvek/knittools/repository`.
- Sallittua:
  repositoryjen sisäinen importti `data.local`-mapperien ja DAOjen takia.
- Ei sallittua:
  `ui/` tai `domain/calculator/` importtaa entityjä.
- Koska tämä on arkkitehtuurimuutos, päivitä `AGENTS.md` ja `memory/MEMORY.md`: repositoryt paljastavat domain-malleja, Room entityt ovat vain `data/local`-sisäisiä.
- Jos myöhemmin commitataan, commit-viestit suomeksi.

## Testisuunnitelma

- Mapper-testit:
  jokainen entity-domain-entity round trip säilyttää kentät.
- Repository-testit:
  repository palauttaa domain-mallit ja kirjoittaa DAOlle oikeat entityt.
- Domain-testit:
  `ProjectCounterLogicTest` ja `ReminderLogicTest` käyttävät domain-malleja, eivät Room entityjä.
- UI/ViewModel-testit:
  päivitä olemassa olevat `ProjectListViewModelTest`, `SessionHistoryViewModelTest`, `YarnCardViewModelTest`, `NotesEditorViewModelTest` käyttämään domain-malleja.
- Lopputarkistus:
  `./gradlew test`.
- Laajempi varmistus ennen julkaisua:
  `./gradlew assembleDebug`.
- Käyttäjän ajettavaksi erikseen:
  `lc`, jos halutaan lint/detekt/Android lint -raportit.

## Oletukset ja rajaukset

- Ei muuteta Room-tauluja, sarakkeita, DAO-kyselyitä tai migraatioita.
- Ei muuteta käyttäjätoiminnallisuutta, sorttauksia, aikaleimalogiikkaa, Pro-rajoja, Ravelry-käytöstä tai widget-käytöstä.
- Ei tehdä enum-refaktorointia samalla, vaikka `counterType` ja `status` ovat stringejä. Se on erillinen laatutyö entity-irrotuksen jälkeen.
- Ei poisteta entityjä. Ne jäävät Roomin pysyviksi tallennusmalleiksi.
- Toteutus tehdään vaiheittain, eikä seuraavaan vaiheeseen mennä ennen kuin edellisen vaiheen kohdennetut testit ovat vihreät.
