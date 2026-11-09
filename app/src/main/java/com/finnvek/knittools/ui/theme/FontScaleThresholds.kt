package com.finnvek.knittools.ui.theme

/** Järjestelmän fonttikertoimen rajat, joista asettelut vaihtuvat. Yksi lähde kaikille näkymille. */
object FontScaleThresholds {
    /** Tätä suuremmalla kertoimella rivit ja ruudukot tiivistyvät (projektikortit, perehdytys, kuvitus). */
    const val COMPACT = 1.3f

    /** Tästä kertoimesta alkaen komponentit vaihtavat suuren fontin asetteluun. */
    const val LARGE = 1.5f
}
