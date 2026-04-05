# KnitTools — Gemini Nano Features

**Platform:** On-device Gemini Nano via ML Kit GenAI / AICore
**Requirement:** Android device with AICore support (Pixel 8+, Samsung S24+)
**Visibility:** Hidden entirely if device doesn't support Nano — no broken UI
**Gate:** Pro-only (all Nano features require Pro subscription)

---

## Toteutetut ominaisuudet

### 1. Instruction Parser — Increase/Decrease
**Tiedosto:** `ai/nano/InstructionParser.kt`
**Näyttö:** Increase/Decrease Calculator → "Paste instruction"
**Vaikeustaso:** ✅ Toteutettu

Parsii englanninkielisiä increase/decrease-ohjeita ja täyttää laskurin kentät:
- "Increase 12 stitches evenly across 96 stitches" → current=96, change=12
- "Dec 8 sts over 120 sts" → current=120, change=8
- "Increase to 108 from 96" → laskee change=12 automaattisesti
- "K2tog every 12th stitch (96 sts)" → tunnistaa implisiittisen decreasein
- Tukee lyhenteitä: inc, dec, st, sts, k2tog, ssk, p2tog
- Regex-fallback jos Nano epäonnistuu
- Typo-toleranssi: 40+ yleisintä kirjoitusvirhettä korjataan automaattisesti

### 2. Instruction Parser — Gauge
**Tiedosto:** `ai/nano/InstructionParser.kt`
**Näyttö:** Gauge Calculator → "Paste instruction"
**Vaikeustaso:** ✅ Toteutettu

Parsii gauge/tension-ohjeita:
- "22 sts and 30 rows = 10cm" → stitches=22, rows=30
- "Tension: 28 sts x 36 rows to 10cm on 4mm needles"
- "5.5 sts per inch, 7 rows per inch" → muuntaa per 10cm (×4)
- "Gauge 22/30" — pelkät numerot gauge-kontekstissa
- Regex-fallback ja typo-toleranssi

### 3. Instruction Parser — Swatch Measurement
**Tiedosto:** `ai/nano/InstructionParser.kt`
**Näyttö:** Gauge Calculator → "Paste instruction"
**Vaikeustaso:** ✅ Toteutettu

Parsii swatch-mittauksia ja täyttää My Gauge -osion kentät:
- "Measured width is 30 cm" → width=30
- "Width 30, 22 stitches" → width=30, stitches=22
- "My swatch is 12cm wide with 26 stitches" → width=12, stitches=26
- "24 sts over 10 cm" → stitches=24, width=10
- "I got 22 sts in 10cm" → stitches=22
- Osittainen täyttö: vain mainitut kentät täytetään

### 4. Yarn Label OCR Parser
**Tiedosto:** `ai/nano/YarnLabelNanoParser.kt`
**Näyttö:** Yarn Estimator → Camera → Yarn Card Review
**Vaikeustaso:** ✅ Toteutettu

ML Kit OCR tunnistaa raakatekstin lankalapusta, Nano parsii sen strukturoiduiksi kentiksi:
- Brand, yarn name, fiber content, weight, length, needle size, gauge
- Color name/number, dye lot, weight category
- Monikielinen: suomi, ruotsi, saksa jne. (kuitunimien käännös englanniksi)
- OCR-virheiden korjaus: katkenneet sanat, 0/O-sekaannus, 1/l/I-sekaannus
- Tunnettujen brändien tunnistus virheellisestäkin OCR-tekstistä
- Regex-fallback (`YarnLabelParser.kt`) jos Nano ei saatavilla

### 5. Project Summary
**Tiedosto:** `ai/nano/ProjectSummarizer.kt`
**Näyttö:** Counter Screen → sparkle (✨) -ikoni projektikortin oikeassa yläkulmassa
**Vaikeustaso:** ✅ Toteutettu

Generoi luettavan yhteenvedon projektin datasta:
- Rivimäärä, sessiot, kokonaisaika, keskimääräinen tahti
- Linkitetyt langat, muistiinpanojen avainpointit
- Projektin ikä päivinä
- Lämmin, ystävällinen sävy — 3-5 lausetta
- Näytetään BottomSheetissä loading-spinnerillä
- Fallback: `simpleSummary()` generoi yhteenvedon ilman Nanoa pelkästä datasta
- Nano EI keksi dataa — kaikki faktat tulevat DB:stä

---

## Suunnitellut ominaisuudet (ei vielä toteutettu)

### 6. Personoidut neulontavinkit
**Vaikeustaso:** 🟡 Keskivaikea

Nykyiset Quick Tips ovat staattinen pool (25 vinkkiä). Nano voisi generoida vinkkejä aktiivisen projektin perusteella:
- "Working on socks? Remember to try them on as you go"
- "You've been knitting for 3 hours — take a stretch break!"
- Syöte: projektin tyyppi, edistyminen, käytetty aika

**Haaste:** Projektin tyyppi ei ole tiedossa (ei kenttää sille). Vinkit voivat olla epärelevantteja.
**Ratkaisu:** Lisää `projectType`-kenttä (socks, sweater, scarf, hat, other) tai päättele nimestä.

### 7. Pattern Repeat -avustin
**Vaikeustaso:** 🟡 Keskivaikea

Käyttäjä liimaa neulekaavan rivin ja Nano analysoi sen:
- "K2, P2, K2, YO, K2tog, K4" → silmukkamäärä per repeat = 12, tekniikat: knit, purl, yarn over, decrease
- Tunnistaa kuviojaon ja kertoo mitä taitoja tarvitaan

**Haaste:** Neulenotaatio vaihtelee (US/UK, kaavio vs. teksti). Rajattu sanasto mutta paljon variaatioita.
**Ratkaisu:** Oma regex-parseri yleisimmille lyhenteille, Nano vain vapaamuotoisille.

### 8. Sessioanalyysi ja ennusteet
**Vaikeustaso:** 🟢 Helppo

Analysoi sessiohistoriasta trendejä:
- "Neulot keskimäärin 12 riviä tunnissa"
- "Tällä tahdilla projekti valmistuu noin 5 sessiossa"
- "Viime viikolla neuloit 3x enemmän kuin tällä viikolla"

**Haaste:** Matala — laskenta tehdään koodissa, Nano vain muotoilee.
**Ratkaisu:** Laske trendit `CounterRepository`:sta, anna Nanolle numerot ja pyydä luettava teksti.

### 9. Muistiinpanojen tiivistys
**Vaikeustaso:** 🟢 Helppo

Jos projektissa on paljon muistiinpanoja eri sessioilta, Nano tiivistäisi:
- "Vaihdettu puikkoja sessiossa 3, kiristettiin reunaa sessiossa 5, vasemmassa hihassa 2 ylimääräistä lisäystä"

**Haaste:** Matala — omaa tekstiä sisään, tiivistelmä ulos.
**Ratkaisu:** Yhdistä kaikki muistiinpanot ja pyydä Nanolta tiivistelmä.

### 10. Langankulutuksen arviointi sanallisesta kuvauksesta
**Vaikeustaso:** 🔴 Vaikea

"I'm knitting a women's medium sweater with worsted weight yarn" → arvio tarvittavasta langasta metreinä.

**Haaste:** Korkea — vaatii laajaa tietokantaa vaatetyypeistä, kokoluokista ja langankulutuksesta. Nano ei välttämättä tiedä tarkkoja lukuja. Väärä arvio voi johtaa liian vähäiseen lankaostoon.
**Ratkaisu:** Hardcodattu taulukko (vaatetyyppi × koko × lankavahvuus → metrimäärä), Nano vain tunnistaa parametrit tekstistä.

### 11. Koomuunnokset (flat ↔ circular, koon vaihto)
**Vaikeustaso:** 🔴 Vaikea

"Convert this pattern from flat to circular" tai "Change from size M to size L" → Nano laskisi muutokset.

**Haaste:** Korkea — monimutkaista matematiikkaa, kontekstin ymmärrystä, ja virhe voi pilata koko projektin.
**Ratkaisu:** Ei suositella Nanolle. Parempi toteuttaa deterministisillä laskureilla joissa käyttäjä syöttää arvot.

### 12. Projektiavustin (vapaat kysymykset)
**Vaikeustaso:** 🔴 Vaikea

Käyttäjä voi kysyä projektistaan: "How much more yarn do I need?", "Am I on track?", "When will I finish?"

**Haaste:** Korkea — avoimet kysymykset vaativat kontekstin yhdistämistä monesta lähteestä. Vastauksen oikeellisuutta vaikea taata.
**Ratkaisu:** Rajattu joukko "template-kysymyksiä" joissa laskenta tehdään koodissa ja Nano muotoilee vastauksen.

---

## Vaikeustason selitykset

| Taso | Kuvaus |
|------|--------|
| 🟢 Helppo | Data on valmiina, Nano vain muotoilee/tiivistää. Ei voi aiheuttaa vahinkoa. |
| 🟡 Keskivaikea | Vaatii rajattua päättelyä. Regex-fallback mahdollinen. Virheen seuraus pieni. |
| 🔴 Vaikea | Avoin päättely, vaikea validoida. Väärä vastaus voi johtaa harhaan tai pilata projektin. |

---

## Arkkitehtuuri

```
Käyttäjän syöte / projektin data
         │
         ▼
    Nano-prompt (SYSTEM_PROMPT)
         │
         ▼
    Gemini Nano (on-device, AICore)
         │
         ▼
    parseResponse() — key:value parsinta
         │
    ┌────┴────┐
    │ Onnistui │ → Strukturoitu tulos → UI kenttiin
    └────┬────┘
         │ Epäonnistui
         ▼
    parseWithRegex() — regex-fallback + typo-korjaus
         │
    ┌────┴────┐
    │ Onnistui │ → Sama tulos → UI kenttiin
    └────┬────┘
         │ Epäonnistui
         ▼
    simpleSummary() / Failure → Fallback-teksti tai virheilmoitus
```

**Periaatteet:**
- Nano ei koskaan ole ainoa polku — aina regex-fallback tai data-pohjainen fallback
- Nano ei keksi dataa — kaikki faktat tulevat DB:stä tai käyttäjän syötteestä
- Pro-gate: kaikki Nano-ominaisuudet vaativat Pro-tilauksen
- Piilotetaan kokonaan jos laite ei tue — ei "upgrade your phone" -viestejä
