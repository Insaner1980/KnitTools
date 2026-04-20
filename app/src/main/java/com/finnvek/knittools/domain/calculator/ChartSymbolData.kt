package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ChartSymbol
import com.finnvek.knittools.domain.model.ChartSymbolCategory

object ChartSymbolData {
    val symbols: List<ChartSymbol> =
        listOf(
            // Perussilmukat
            ChartSymbol("knit", R.string.sym_knit_name, R.string.sym_knit_desc, ChartSymbolCategory.BASIC),
            ChartSymbol("purl", R.string.sym_purl_name, R.string.sym_purl_desc, ChartSymbolCategory.BASIC),
            ChartSymbol(
                "yarn_over",
                R.string.sym_yarn_over_name,
                R.string.sym_yarn_over_desc,
                ChartSymbolCategory.BASIC,
            ),
            ChartSymbol(
                "knit_tbl",
                R.string.sym_knit_tbl_name,
                R.string.sym_knit_tbl_desc,
                ChartSymbolCategory.BASIC,
            ),
            ChartSymbol(
                "purl_tbl",
                R.string.sym_purl_tbl_name,
                R.string.sym_purl_tbl_desc,
                ChartSymbolCategory.BASIC,
            ),
            ChartSymbol(
                "slip_knitwise",
                R.string.sym_slip_knitwise_name,
                R.string.sym_slip_knitwise_desc,
                ChartSymbolCategory.BASIC,
            ),
            ChartSymbol(
                "slip_purlwise",
                R.string.sym_slip_purlwise_name,
                R.string.sym_slip_purlwise_desc,
                ChartSymbolCategory.BASIC,
            ),
            // Kavennukset
            ChartSymbol("k2tog", R.string.sym_k2tog_name, R.string.sym_k2tog_desc, ChartSymbolCategory.DECREASES),
            ChartSymbol("ssk", R.string.sym_ssk_name, R.string.sym_ssk_desc, ChartSymbolCategory.DECREASES),
            ChartSymbol("p2tog", R.string.sym_p2tog_name, R.string.sym_p2tog_desc, ChartSymbolCategory.DECREASES),
            ChartSymbol("s2kp", R.string.sym_s2kp_name, R.string.sym_s2kp_desc, ChartSymbolCategory.DECREASES),
            ChartSymbol("k3tog", R.string.sym_k3tog_name, R.string.sym_k3tog_desc, ChartSymbolCategory.DECREASES),
            ChartSymbol("sk2p", R.string.sym_sk2p_name, R.string.sym_sk2p_desc, ChartSymbolCategory.DECREASES),
            // Lisäykset
            ChartSymbol("m1l", R.string.sym_m1l_name, R.string.sym_m1l_desc, ChartSymbolCategory.INCREASES),
            ChartSymbol("m1r", R.string.sym_m1r_name, R.string.sym_m1r_desc, ChartSymbolCategory.INCREASES),
            ChartSymbol("kfb", R.string.sym_kfb_name, R.string.sym_kfb_desc, ChartSymbolCategory.INCREASES),
            ChartSymbol(
                "lifted_inc_left",
                R.string.sym_lifted_inc_left_name,
                R.string.sym_lifted_inc_left_desc,
                ChartSymbolCategory.INCREASES,
            ),
            ChartSymbol(
                "lifted_inc_right",
                R.string.sym_lifted_inc_right_name,
                R.string.sym_lifted_inc_right_desc,
                ChartSymbolCategory.INCREASES,
            ),
            // Palmikot
            ChartSymbol(
                "cable_2_2_left",
                R.string.sym_cable_2_2_left_name,
                R.string.sym_cable_2_2_left_desc,
                ChartSymbolCategory.CABLES,
            ),
            ChartSymbol(
                "cable_2_2_right",
                R.string.sym_cable_2_2_right_name,
                R.string.sym_cable_2_2_right_desc,
                ChartSymbolCategory.CABLES,
            ),
            ChartSymbol(
                "cable_3_3_left",
                R.string.sym_cable_3_3_left_name,
                R.string.sym_cable_3_3_left_desc,
                ChartSymbolCategory.CABLES,
            ),
            ChartSymbol(
                "cable_3_3_right",
                R.string.sym_cable_3_3_right_name,
                R.string.sym_cable_3_3_right_desc,
                ChartSymbolCategory.CABLES,
            ),
            // Muut
            ChartSymbol(
                "no_stitch",
                R.string.sym_no_stitch_name,
                R.string.sym_no_stitch_desc,
                ChartSymbolCategory.OTHER,
            ),
            ChartSymbol("marker", R.string.sym_marker_name, R.string.sym_marker_desc, ChartSymbolCategory.OTHER),
            ChartSymbol("repeat", R.string.sym_repeat_name, R.string.sym_repeat_desc, ChartSymbolCategory.OTHER),
        )

    fun byCategory(): Map<ChartSymbolCategory, List<ChartSymbol>> = symbols.groupBy { it.category }
}
