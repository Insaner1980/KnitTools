package com.finnvek.knittools.domain.calculator

import android.content.Context
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.KnittingAbbreviation

object AbbreviationData {
    val abbreviations: List<KnittingAbbreviation> =
        listOf(
            // Perussilmukat
            KnittingAbbreviation("K", R.string.abbr_k_meaning, R.string.abbr_k_desc),
            KnittingAbbreviation("P", R.string.abbr_p_meaning, R.string.abbr_p_desc),
            KnittingAbbreviation("St(s)", R.string.abbr_sts_meaning, R.string.abbr_sts_desc),
            KnittingAbbreviation("RS", R.string.abbr_rs_meaning, R.string.abbr_rs_desc),
            KnittingAbbreviation("WS", R.string.abbr_ws_meaning, R.string.abbr_ws_desc),
            KnittingAbbreviation("YO", R.string.abbr_yo_meaning, R.string.abbr_yo_desc),
            KnittingAbbreviation("Kwise", R.string.abbr_kwise_meaning, R.string.abbr_kwise_desc),
            KnittingAbbreviation("Pwise", R.string.abbr_pwise_meaning, R.string.abbr_pwise_desc),
            // Kavennukset
            KnittingAbbreviation("K2tog", R.string.abbr_k2tog_meaning, R.string.abbr_k2tog_desc),
            KnittingAbbreviation("P2tog", R.string.abbr_p2tog_meaning, R.string.abbr_p2tog_desc),
            KnittingAbbreviation("SSK", R.string.abbr_ssk_meaning, R.string.abbr_ssk_desc),
            KnittingAbbreviation("SSP", R.string.abbr_ssp_meaning, R.string.abbr_ssp_desc),
            KnittingAbbreviation("SKP", R.string.abbr_skp_meaning, R.string.abbr_skp_desc),
            KnittingAbbreviation("K3tog", R.string.abbr_k3tog_meaning, R.string.abbr_k3tog_desc),
            KnittingAbbreviation("P3tog", R.string.abbr_p3tog_meaning, R.string.abbr_p3tog_desc),
            KnittingAbbreviation("S2KP", R.string.abbr_s2kp_meaning, R.string.abbr_s2kp_desc),
            KnittingAbbreviation("SK2P", R.string.abbr_sk2p_meaning, R.string.abbr_sk2p_desc),
            KnittingAbbreviation("CDD", R.string.abbr_cdd_meaning, R.string.abbr_cdd_desc),
            KnittingAbbreviation("Psso", R.string.abbr_psso_meaning, R.string.abbr_psso_desc),
            KnittingAbbreviation("Dec", R.string.abbr_dec_meaning, R.string.abbr_dec_desc),
            // Lisäykset
            KnittingAbbreviation("M1", R.string.abbr_m1_meaning, R.string.abbr_m1_desc),
            KnittingAbbreviation("M1L", R.string.abbr_m1l_meaning, R.string.abbr_m1l_desc),
            KnittingAbbreviation("M1R", R.string.abbr_m1r_meaning, R.string.abbr_m1r_desc),
            KnittingAbbreviation("KFB", R.string.abbr_kfb_meaning, R.string.abbr_kfb_desc),
            KnittingAbbreviation("PFB", R.string.abbr_pfb_meaning, R.string.abbr_pfb_desc),
            KnittingAbbreviation("Inc", R.string.abbr_inc_meaning, R.string.abbr_inc_desc),
            // Siirtäminen
            KnittingAbbreviation("SL", R.string.abbr_sl_meaning, R.string.abbr_sl_desc),
            KnittingAbbreviation("SL1K", R.string.abbr_sl1k_meaning, R.string.abbr_sl1k_desc),
            KnittingAbbreviation("SL1P", R.string.abbr_sl1p_meaning, R.string.abbr_sl1p_desc),
            // Aloitus ja lopetus
            KnittingAbbreviation("CO", R.string.abbr_co_meaning, R.string.abbr_co_desc),
            KnittingAbbreviation("BO", R.string.abbr_bo_meaning, R.string.abbr_bo_desc),
            KnittingAbbreviation("PU", R.string.abbr_pu_meaning, R.string.abbr_pu_desc),
            // Merkit ja toisto
            KnittingAbbreviation("PM", R.string.abbr_pm_meaning, R.string.abbr_pm_desc),
            KnittingAbbreviation("SM", R.string.abbr_sm_meaning, R.string.abbr_sm_desc),
            KnittingAbbreviation("Rep", R.string.abbr_rep_meaning, R.string.abbr_rep_desc),
            KnittingAbbreviation("Rnd", R.string.abbr_rnd_meaning, R.string.abbr_rnd_desc),
            KnittingAbbreviation("EOR", R.string.abbr_eor_meaning, R.string.abbr_eor_desc),
            // Puikot
            KnittingAbbreviation("DPN", R.string.abbr_dpn_meaning, R.string.abbr_dpn_desc),
            KnittingAbbreviation("CN", R.string.abbr_cn_meaning, R.string.abbr_cn_desc),
            KnittingAbbreviation("Circ", R.string.abbr_circ_meaning, R.string.abbr_circ_desc),
            // Takareunasta
            KnittingAbbreviation("Tbl", R.string.abbr_tbl_meaning, R.string.abbr_tbl_desc),
            KnittingAbbreviation("Ktbl", R.string.abbr_ktbl_meaning, R.string.abbr_ktbl_desc),
            KnittingAbbreviation("Ptbl", R.string.abbr_ptbl_meaning, R.string.abbr_ptbl_desc),
            // Palmikot
            KnittingAbbreviation("C4F", R.string.abbr_c4f_meaning, R.string.abbr_c4f_desc),
            KnittingAbbreviation("C4B", R.string.abbr_c4b_meaning, R.string.abbr_c4b_desc),
            KnittingAbbreviation("C6F", R.string.abbr_c6f_meaning, R.string.abbr_c6f_desc),
            KnittingAbbreviation("C6B", R.string.abbr_c6b_meaning, R.string.abbr_c6b_desc),
            // Lankasijainti
            KnittingAbbreviation("Wyif", R.string.abbr_wyif_meaning, R.string.abbr_wyif_desc),
            KnittingAbbreviation("Wyib", R.string.abbr_wyib_meaning, R.string.abbr_wyib_desc),
            // Lyhennetyt kerrokset
            KnittingAbbreviation("W&T", R.string.abbr_wt_meaning, R.string.abbr_wt_desc),
            // Neuletyypit
            KnittingAbbreviation("St st", R.string.abbr_st_st_meaning, R.string.abbr_st_st_desc),
            KnittingAbbreviation("Rev St st", R.string.abbr_rev_st_st_meaning, R.string.abbr_rev_st_st_desc),
            KnittingAbbreviation("G st", R.string.abbr_g_st_meaning, R.string.abbr_g_st_desc),
            KnittingAbbreviation("Seed st", R.string.abbr_seed_st_meaning, R.string.abbr_seed_st_desc),
            // Yleiset termit
            KnittingAbbreviation("Tog", R.string.abbr_tog_meaning, R.string.abbr_tog_desc),
            KnittingAbbreviation("Rem", R.string.abbr_rem_meaning, R.string.abbr_rem_desc),
            KnittingAbbreviation("Beg", R.string.abbr_beg_meaning, R.string.abbr_beg_desc),
            KnittingAbbreviation("Alt", R.string.abbr_alt_meaning, R.string.abbr_alt_desc),
            KnittingAbbreviation("Approx", R.string.abbr_approx_meaning, R.string.abbr_approx_desc),
            KnittingAbbreviation("Cont", R.string.abbr_cont_meaning, R.string.abbr_cont_desc),
            KnittingAbbreviation("Foll", R.string.abbr_foll_meaning, R.string.abbr_foll_desc),
            KnittingAbbreviation("Prev", R.string.abbr_prev_meaning, R.string.abbr_prev_desc),
            KnittingAbbreviation("Patt", R.string.abbr_patt_meaning, R.string.abbr_patt_desc),
            KnittingAbbreviation("Sk", R.string.abbr_sk_meaning, R.string.abbr_sk_desc),
            KnittingAbbreviation("Selvage", R.string.abbr_selvage_meaning, R.string.abbr_selvage_desc),
            KnittingAbbreviation("LH", R.string.abbr_lh_meaning, R.string.abbr_lh_desc),
            KnittingAbbreviation("RH", R.string.abbr_rh_meaning, R.string.abbr_rh_desc),
            // Värit
            KnittingAbbreviation("MC", R.string.abbr_mc_meaning, R.string.abbr_mc_desc),
            KnittingAbbreviation("CC", R.string.abbr_cc_meaning, R.string.abbr_cc_desc),
            // Mittaus
            KnittingAbbreviation("WPI", R.string.abbr_wpi_meaning, R.string.abbr_wpi_desc),
            KnittingAbbreviation("Gauge", R.string.abbr_gauge_meaning, R.string.abbr_gauge_desc),
            // Yhteisötermit
            KnittingAbbreviation("FO", R.string.abbr_fo_meaning, R.string.abbr_fo_desc),
            KnittingAbbreviation("WIP", R.string.abbr_wip_meaning, R.string.abbr_wip_desc),
            KnittingAbbreviation("UFO", R.string.abbr_ufo_meaning, R.string.abbr_ufo_desc),
            KnittingAbbreviation("Frog", R.string.abbr_frog_meaning, R.string.abbr_frog_desc),
            KnittingAbbreviation("Tink", R.string.abbr_tink_meaning, R.string.abbr_tink_desc),
            KnittingAbbreviation("LYS", R.string.abbr_lys_meaning, R.string.abbr_lys_desc),
        )

    fun search(
        context: Context,
        query: String,
    ): List<KnittingAbbreviation> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return abbreviations
        return abbreviations.filter {
            it.abbreviation.lowercase().contains(trimmed) ||
                context.getString(it.meaningResId).lowercase().contains(trimmed)
        }
    }
}
