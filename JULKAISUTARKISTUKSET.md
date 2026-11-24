# KnitToolsin ensimmäisen julkaisun muistilista

Samsung Galaxy A13:lla (Android 14) tarkistettiin 24.11.2026, että suomeksi valitun sovelluksen PostHog-tapahtumassa oli `$app_language = fi`. Suostumuksen peruminen poisti vanhan tapahtuman jonosta. Testit käyttivät paikallista vastaanotinta.

Firebase kuittasi saman päivän paketin nimikartan vastaanotetuksi. Kartta täsmäsi paketin sisältöön. Se palautti paikallisen testivirheen oikeaksi tiedostoksi `PostHogConsentClient.kt` ja riviksi 111. Firebasen raportissa ei ollut kaatumisongelmia ajalta 27.8.–24.11.2026.

## Kun Pixel 9 on käytettävissä

- [ ] Päivitä erillinen testiasennus Android 17:llä. Avaa vanha testiprojekti, muuta laskuria ja käynnistä sovellus uudelleen. Tarkista, että lukema säilyy ja projektin PDF avautuu.
- [ ] Lisää laskuriwidget kotinäytölle. Muuta sen kokoa, käännä näyttö ja muuta laskuria widgetistä. Tarkista, että arvo päivittyy sekä widgetissä että sovelluksessa.
- [ ] Avaa Ravelryn verkkosivu sovelluksesta ja palaa projektiin. Tämä kokeilee verkkoyhteyttä ja siirtymistä takaisin sovellukseen Android 17:llä.
- [ ] Jos tarvitaan näyttö Android 17:n muistivirheraporteista, käytä erillistä Firebase-testiprojektia. Tarkista, miten Firebase näyttää muistin loppumisen ja tilanteen, jossa Android sulkee sovelluksen muistirajan vuoksi.

Suuren näytön asettelun voi tarkistaa myöhemmin Android 17 -tabletilla. Myös vanhimman tuetun Android-version (Android 10) laiteajo on vielä auki.

## Ennen Play-julkaisua

- [ ] Tarkista Ravelryn kirjallinen lupa sovelluksen sisäiseen selaimeen. Asia on kirjattu [projektin päätöksiin](config/security-decisions.md).
- [ ] Allekirjoita lopullinen sovelluspaketti omalla Play-latausavaimella ja tarkista asennus Playn sisäisestä testistä.

## Kun ensimmäinen kaatumisraportti saapuu

- [ ] Tarkista ensimmäisestä lopullisen version kaatumisraportista, että Firebase näyttää KnitToolsin tiedostonimen ja rivinumeron. Tämän voi tarkistaa millä tahansa tuetulla Android-versiolla.

Paikallinen testivarmenteella allekirjoitettu [sovelluspaketti](app/build/outputs/bundle/release/app-release.aab) läpäisi bundletool-tarkistuksen. Paketin SHA-256 on `cb28c5562b864967b1779269538849afbd34d686a04322d306f65e555861833b`.
