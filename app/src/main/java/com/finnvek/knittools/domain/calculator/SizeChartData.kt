package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SizeChartEntry
import com.finnvek.knittools.domain.model.SizeLabel
import com.finnvek.knittools.domain.model.SizeMeasurement
import java.util.Locale

/**
 * Lähde: Craft Yarn Council body sizing -standardit:
 * https://www.craftyarncouncil.com/standards/body-sizing
 *
 * Taulukot ovat reference-käyttöön tarkoitettuja yleismittoja. Mitat tallennetaan senttimetreinä;
 * tuumat johdetaan samasta arvosta, jotta yksikkövalinta ei muuta lähdedataa.
 */
object SizeChartData {
    enum class Category {
        BABY,
        CHILD_YOUTH,
        WOMEN,
        MEN,
        HEAD,
        HAND,
    }

    fun headers(category: Category): List<Int> =
        when (category) {
            Category.BABY -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_chest,
                    R.string.size_col_waist,
                    R.string.size_col_hips,
                    R.string.size_col_head,
                    R.string.size_col_length,
                )
            }

            Category.CHILD_YOUTH -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_chest,
                    R.string.size_col_waist,
                    R.string.size_col_hips,
                    R.string.size_col_height,
                    R.string.size_col_arm,
                )
            }

            Category.WOMEN -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_bust,
                    R.string.size_col_waist,
                    R.string.size_col_hips,
                )
            }

            Category.MEN -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_chest,
                    R.string.size_col_waist,
                    R.string.size_col_hips,
                )
            }

            Category.HEAD -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_circumference,
                )
            }

            Category.HAND -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_circumference,
                    R.string.size_col_length,
                )
            }
        }

    fun entries(category: Category): List<SizeChartEntry> =
        when (category) {
            Category.BABY -> babyEntries
            Category.CHILD_YOUTH -> childEntries
            Category.WOMEN -> womenEntries
            Category.MEN -> menEntries
            Category.HEAD -> headEntries
            Category.HAND -> handEntries
        }

    private fun m(cm: Double): SizeMeasurement = SizeMeasurement(cm, cm / 2.54)

    private fun res(resId: Int): SizeLabel = SizeLabel.Resource(resId)

    fun formatMeasurement(
        measurement: SizeMeasurement,
        useImperial: Boolean,
        cmUnit: String,
        inchUnit: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val value = if (useImperial) measurement.inches else measurement.cm
        val unit = if (useImperial) inchUnit else cmUnit
        val formatted =
            if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format(locale, "%.1f", value)
            }
        return "$formatted $unit"
    }

    private val babyEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_preemie), listOf(m(30.5), m(30.5), m(31.0), m(30.5), m(38.0))),
            SizeChartEntry(res(R.string.size_row_age_0_3m), listOf(m(40.5), m(40.5), m(43.0), m(35.5), m(53.5))),
            SizeChartEntry(res(R.string.size_row_age_3_6m), listOf(m(43.0), m(43.0), m(45.5), m(38.0), m(58.5))),
            SizeChartEntry(res(R.string.size_row_age_6_12m), listOf(m(47.0), m(47.0), m(47.0), m(43.0), m(66.0))),
            SizeChartEntry(res(R.string.size_row_age_12_18m), listOf(m(50.5), m(49.5), m(50.5), m(47.0), m(73.5))),
            SizeChartEntry(res(R.string.size_row_age_18_24m), listOf(m(52.0), m(50.5), m(52.0), m(48.5), m(78.5))),
        )

    private val childEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_child_2), listOf(m(53.5), m(51.0), m(54.5), m(89.0), m(24.0))),
            SizeChartEntry(res(R.string.size_row_child_4), listOf(m(56.0), m(53.5), m(58.5), m(101.5), m(27.5))),
            SizeChartEntry(res(R.string.size_row_child_6), listOf(m(63.5), m(56.0), m(63.5), m(114.5), m(31.0))),
            SizeChartEntry(res(R.string.size_row_child_8), listOf(m(67.5), m(58.5), m(68.5), m(127.0), m(34.5))),
            SizeChartEntry(res(R.string.size_row_child_10), listOf(m(71.0), m(61.0), m(72.5), m(139.5), m(37.0))),
            SizeChartEntry(res(R.string.size_row_child_12), listOf(m(76.0), m(63.5), m(77.5), m(149.5), m(39.5))),
            SizeChartEntry(res(R.string.size_row_child_14), listOf(m(80.0), m(66.0), m(82.5), m(155.0), m(42.0))),
            SizeChartEntry(res(R.string.size_row_child_16), listOf(m(84.0), m(68.5), m(87.5), m(162.5), m(44.5))),
        )

    private val womenEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_women_xs_2_4), listOf(m(78.5), m(60.0), m(85.0))),
            SizeChartEntry(res(R.string.size_row_women_s_6_8), listOf(m(84.0), m(66.0), m(91.5))),
            SizeChartEntry(res(R.string.size_row_women_m_10_12), listOf(m(91.5), m(73.5), m(99.0))),
            SizeChartEntry(res(R.string.size_row_women_l_14_16), listOf(m(99.0), m(81.5), m(106.5))),
            SizeChartEntry(res(R.string.size_row_women_xl_18_20), listOf(m(109.0), m(91.5), m(117.0))),
            SizeChartEntry(res(R.string.size_row_women_2xl_22_24), listOf(m(119.5), m(101.5), m(127.0))),
            SizeChartEntry(res(R.string.size_row_women_3xl_26_28), listOf(m(130.0), m(112.0), m(137.0))),
            SizeChartEntry(res(R.string.size_row_women_4xl_30_32), listOf(m(140.0), m(122.0), m(147.5))),
            SizeChartEntry(res(R.string.size_row_women_5xl_34_36), listOf(m(150.0), m(132.0), m(157.5))),
        )

    private val menEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_men_xs_30_32), listOf(m(81.5), m(68.5), m(84.0))),
            SizeChartEntry(res(R.string.size_row_men_s_34_36), listOf(m(91.5), m(76.0), m(91.5))),
            SizeChartEntry(res(R.string.size_row_men_m_38_40), listOf(m(101.5), m(84.0), m(99.0))),
            SizeChartEntry(res(R.string.size_row_men_l_42_44), listOf(m(112.0), m(91.5), m(106.5))),
            SizeChartEntry(res(R.string.size_row_men_xl_46_48), listOf(m(122.0), m(101.5), m(114.5))),
            SizeChartEntry(res(R.string.size_row_men_2xl_50_52), listOf(m(132.0), m(112.0), m(124.5))),
            SizeChartEntry(res(R.string.size_row_men_3xl_54_56), listOf(m(142.0), m(122.0), m(134.5))),
            SizeChartEntry(res(R.string.size_row_men_4xl_58_60), listOf(m(152.0), m(132.0), m(144.5))),
            SizeChartEntry(res(R.string.size_row_men_5xl_62_64), listOf(m(162.5), m(142.0), m(154.5))),
        )

    private val headEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_preemie), listOf(m(30.5))),
            SizeChartEntry(res(R.string.size_row_baby), listOf(m(35.5))),
            SizeChartEntry(res(R.string.size_row_toddler), listOf(m(46.0))),
            SizeChartEntry(res(R.string.size_row_child), listOf(m(50.5))),
            SizeChartEntry(res(R.string.size_row_adult_s), listOf(m(53.5))),
            SizeChartEntry(res(R.string.size_row_adult_m), listOf(m(56.0))),
            SizeChartEntry(res(R.string.size_row_adult_l), listOf(m(58.5))),
        )

    private val handEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_child_s), listOf(m(12.5), m(11.5))),
            SizeChartEntry(res(R.string.size_row_child_m), listOf(m(14.0), m(13.5))),
            SizeChartEntry(res(R.string.size_row_child_l), listOf(m(15.0), m(14.5))),
            SizeChartEntry(res(R.string.size_row_adult_s), listOf(m(17.5), m(17.0))),
            SizeChartEntry(res(R.string.size_row_adult_m), listOf(m(19.0), m(18.5))),
            SizeChartEntry(res(R.string.size_row_adult_l), listOf(m(21.5), m(20.5))),
            SizeChartEntry(res(R.string.size_row_adult_xl), listOf(m(23.0), m(22.0))),
        )
}
