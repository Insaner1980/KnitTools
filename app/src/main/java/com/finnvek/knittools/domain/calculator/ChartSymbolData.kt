package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ChartSymbol
import com.finnvek.knittools.domain.model.ChartSymbolCategory

object ChartSymbolData {
    val symbols: List<ChartSymbol> =
        listOf(
            // Perussilmukat
            ChartSymbol("knit", "Knit", "Knit on RS, purl on WS", ChartSymbolCategory.BASIC),
            ChartSymbol("purl", "Purl", "Purl on RS, knit on WS", ChartSymbolCategory.BASIC),
            ChartSymbol(
                "yarn_over",
                "Yarn Over",
                "Wrap yarn around needle to create a hole",
                ChartSymbolCategory.BASIC,
            ),
            ChartSymbol("knit_tbl", "Knit TBL", "Knit through the back loop (twisted)", ChartSymbolCategory.BASIC),
            ChartSymbol("purl_tbl", "Purl TBL", "Purl through the back loop (twisted)", ChartSymbolCategory.BASIC),
            ChartSymbol("slip_knitwise", "Slip Knitwise", "Slip stitch as if to knit", ChartSymbolCategory.BASIC),
            ChartSymbol("slip_purlwise", "Slip Purlwise", "Slip stitch as if to purl", ChartSymbolCategory.BASIC),
            // Kavennukset
            ChartSymbol("k2tog", "K2tog", "Knit 2 together — right-leaning decrease", ChartSymbolCategory.DECREASES),
            ChartSymbol("ssk", "SSK", "Slip, slip, knit — left-leaning decrease", ChartSymbolCategory.DECREASES),
            ChartSymbol("p2tog", "P2tog", "Purl 2 together", ChartSymbolCategory.DECREASES),
            ChartSymbol("s2kp", "S2KP / CDD", "Centered double decrease", ChartSymbolCategory.DECREASES),
            ChartSymbol("k3tog", "K3tog", "Knit 3 together — double decrease", ChartSymbolCategory.DECREASES),
            ChartSymbol("sk2p", "SK2P", "Slip 1, K2tog, pass slipped stitch over", ChartSymbolCategory.DECREASES),
            // Lisäykset
            ChartSymbol("m1l", "M1L", "Make 1 left — left-leaning increase", ChartSymbolCategory.INCREASES),
            ChartSymbol("m1r", "M1R", "Make 1 right — right-leaning increase", ChartSymbolCategory.INCREASES),
            ChartSymbol("kfb", "KFB", "Knit front and back — increase", ChartSymbolCategory.INCREASES),
            ChartSymbol(
                "lifted_inc_left",
                "Lifted Inc Left",
                "Left-leaning lifted increase",
                ChartSymbolCategory.INCREASES,
            ),
            ChartSymbol(
                "lifted_inc_right",
                "Lifted Inc Right",
                "Right-leaning lifted increase",
                ChartSymbolCategory.INCREASES,
            ),
            // Palmikot
            ChartSymbol("cable_2_2_left", "Cable 2/2 Left", "2-over-2 left cross cable", ChartSymbolCategory.CABLES),
            ChartSymbol("cable_2_2_right", "Cable 2/2 Right", "2-over-2 right cross cable", ChartSymbolCategory.CABLES),
            ChartSymbol("cable_3_3_left", "Cable 3/3 Left", "3-over-3 left cross cable", ChartSymbolCategory.CABLES),
            ChartSymbol("cable_3_3_right", "Cable 3/3 Right", "3-over-3 right cross cable", ChartSymbolCategory.CABLES),
            // Muut
            ChartSymbol("no_stitch", "No Stitch", "Gray box — placeholder for shaping", ChartSymbolCategory.OTHER),
            ChartSymbol("marker", "Marker", "Stitch marker position", ChartSymbolCategory.OTHER),
            ChartSymbol("repeat", "Repeat", "Pattern repeat bracket", ChartSymbolCategory.OTHER),
        )

    fun byCategory(): Map<ChartSymbolCategory, List<ChartSymbol>> = symbols.groupBy { it.category }
}
