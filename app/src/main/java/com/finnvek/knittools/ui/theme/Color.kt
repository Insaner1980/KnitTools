package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color

// === Taustat ===
val Background = Color(0xFF1E1E12) // Päätausta — tumma oliivi
val BackgroundAlt = Color(0xFF252518) // Hieman vaaleampi, kontrastialue

// === Pinnat (tumma khaki/oliivi — EI valkoinen) ===
val Surface = Color(0xFF2E2E20) // Korttien peruspinta
val SurfaceHigh = Color(0xFF3A3A2A) // Korotetut kortit, työkalu/referenssi-itemit
val SurfaceHighest = Color(0xFF454535) // Korkein korotus, syötekentät

// === Primary — Poltettu oranssi ===
val Primary = Color(0xFFC45100) // Pääväri, + nappi, CTA:t
val PrimaryContainer = Color(0xFFD4722A) // Vaaleampi oranssi, gradientit
val OnPrimary = Color(0xFFFFFFFF) // Teksti/ikonit primaryn päällä

// === Secondary — Avokado ===
val Secondary = Color(0xFF8BA44A) // Labelit ("CURRENT ROW"), osio-otsikot
val SecondaryMuted = Color(0xFF6B8A35) // Himmeämpi vihreä
val SecondaryContainer = Color(0xFF3A4020) // Vihreä konttitausta

// === Tertiary — Sinapinkeltainen ===
val Tertiary = Color(0xFFC9A435) // Neulevinkit, aksenttikorostukset
val TertiaryContainer = Color(0xFF3A3520) // Quick tip -kortin tausta

// === Teksti ===
val TextPrimary = Color(0xFFE8E4D0) // Pääteksti — lämmin kerma
val TextSecondary = Color(0xFFB8B4A0) // Kuvaukset, toissijainen info
val TextMuted = Color(0xFF8A866E) // Hiljennetty teksti, chevronit, aikaleimat
val TextDisabled = Color(0xFF5A5840) // Disabled-tila

// === Aksentti ===
val DustyRose = Color(0xFFB8908F) // Pro trial -teksti, AI summary, yarn card

// === Status ===
val Error = Color(0xFFC44D4D)
val ErrorContainer = Color(0xFF3A2020)
val Success = Color(0xFF8BA44A) // Sama kuin Secondary
val SuccessContainer = Color(0xFF3A4020)

// === Navigaatio ===
val NavBackground = Color(0xFF161610) // Alanav-tausta — erittäin tumma
val NavText = Color(0xFFB0AC92) // Inaktiiviset navikohteet — vaalennettu luettavuutta varten
val NavActive = Color(0xFFC45100) // Aktiivinen kohde — poltettu oranssi
val NavActiveBg = Color(0xFF3A2010) // Aktiivisen tabin indikaattoritausta

// === Erotin ===
val Divider = Color(0xFF3A3A2A)

// === Ravelry ===
val RavelryTeal = Color(0xFF5F8A8B)
val LightRavelryTeal = Color(0xFF4A7172)

// === Light-teeman taustat ===
val LightBackground = Color(0xFFE8E4D0) // Lämmin kerma (app-ikonin tausta)
val LightBackgroundAlt = Color(0xFFDDD8C3) // Hieman tummempi, kontrastialue

// === Light-teeman pinnat (tummempi = korkeampi korotus) ===
val LightSurface = Color(0xFFD2CDB5) // Korttien peruspinta
val LightSurfaceHigh = Color(0xFFBBB59A) // Korotetut kortit, työkalu/referenssi-itemit
val LightSurfaceMediumHigh = Color(0xFFC8C3A8) // Dialogit, popupit
val LightSurfaceHighest = Color(0xFFA49D80) // Korkein korotus, syötekentät

// === Light-teeman Secondary — Tummempi avokado (kontrasti vaalealla taustalla) ===
val LightSecondary = Color(0xFF6B8A2E)
val LightSecondaryMuted = Color(0xFF5A7525)
val LightSecondaryContainer = Color(0xFFD0DDB5)

// === Light-teeman Tertiary — Tumma kulta/sinappi ===
val LightTertiary = Color(0xFF9A7B18)
val LightTertiaryContainer = Color(0xFFE8DFB5)

// === Light-teeman teksti (lämmin ruskea, ei mustaa) ===
val LightTextPrimary = Color(0xFF2E2A1E) // Tumma lämmin ruskea
val LightTextSecondary = Color(0xFF5C5643) // Keskiväri ruskea
val LightTextMuted = Color(0xFF8A8370) // Hiljennetty ruskea
val LightTextDisabled = Color(0xFFC0BAA5) // Disabled-tila

// === Light-teeman aksentti ===
val LightDustyRose = Color(0xFF9E706E) // Syvempi dusty rose

// === Light-teeman status ===
val LightErrorContainer = Color(0xFFEAD0D0)
val LightSuccessContainer = Color(0xFFD0DDB5)

// === Light-teeman navigaatio ===
val LightNavBackground = Color(0xFFDDD8C3) // Lämmin navipalkki
val LightNavText = Color(0xFF5A5440) // Inaktiiviset navikohteet — tummennettu luettavuutta varten
val LightNavActiveBg = Color(0xFFEAD0B5) // Aktiivisen tabin indikaattoritausta

// === Light-teeman erotin ===
val LightDivider = Color(0xFFC5C0A8)

// === Lanka-ikonien väripaletti (deterministinen ID:n perusteella) ===
val YarnColors =
    listOf(
        Color(0xFFC45100), // Poltettu oranssi
        Color(0xFF8BA44A), // Avokado
        Color(0xFFC9A435), // Sinappi
        Color(0xFFB8908F), // Dusty rose
        Color(0xFF9A6B4A), // Terrakotta
        Color(0xFF5A8A7A), // Teal
        Color(0xFF9A82AA), // Laventeli
        Color(0xFFA85A3A), // Ruosteenpunainen
    )
