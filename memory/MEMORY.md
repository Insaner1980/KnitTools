# KnitTools Memory

## 2026-05-08

- Lankalipun offline OCR/Nano-parseripolku poistettiin tuotantopinnasta. Lankalipun skannaus kulkee nyt `YarnLabelGeminiScanner`in kautta Firebase AI / Gemini -multimodal-kuvasyotteella.
- `ParsedYarnLabel` kuuluu yleiseen `ai/`-pakettiin, koska sama jäsennetty malli ei ole enää OCR-paketin omistama.
- `AiVoiceAction` kuuluu yleiseen `ai/`-pakettiin AI-äänikomentotulkinnan sopimuksena, ei counter-UI:n omistamaksi tyypiksi.
- Journalin Compose UI ja `JournalEntryViewModel` kuuluvat `ui/screens/notes`-pakettiin; journalin AI-prosessointi ja prosessointitulokset kuuluvat edelleen `ai/journal`-pakettiin.
- Kameralla otettavan lankalippukuvan tiedostoluonti kuuluu `data/storage/YarnLabelPhotoStorage.kt`:lle, ei AI- tai OCR-paketille.
- `ProFeature`-gateja käytetään featurekohtaisten enum-arvojen kautta; Insights tarkistaa `ProFeature.INSIGHTS_CHARTS`in eikä yleistä `isPro()`-tilaa.
- Room-entityjen irrotus on aloitettu lisäämällä Room-vapaat domain-mallit `domain/model`-pakettiin sekä kaksisuuntaiset entity/domain-mapperit `data/local/EntityMappers.kt`:hen.
- `CounterRepository`, `SavedPatternRepository`, `ProgressPhotoRepository`, `YarnCardRepository`, `PatternAnnotationRepository`, `ProjectCounterRepository` ja `ReminderRepository` paljastavat nyt public API:ssaan domain-mallit ja muuntavat entityiksi vain DAO-rajalla. `CounterProject` ja `KnitSession` ovat pääprojektien ja sessioiden UI-/repository-rajapinnan mallit.
- `domain/calculator` ei enää käytä `ProjectCounterEntity`- tai `RowReminderEntity`-tyyppejä; counterin apulaskurit ja rivimuistutukset kulkevat `ProjectCounter`- ja `RowReminder`-domain-malleilla.
- `ui/` ei importtaa Room entityjä tai `data/local`-pakettia entity-irrotuksen vaiheessa 5; pääprojektit ja sessiot kulkevat domain-malleina ViewModel- ja composable-kerroksissa.
