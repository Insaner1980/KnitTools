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
 * Body measurements, not garment dimensions. Source categories and ranges are retained.
 * Metric cells below are the published cm values; displayed inches are cm / 2.54,
 * not the independently rounded inch cells in the source. Unsupported columns are omitted.
 * https://www.craftyarncouncil.com/standards/baby-size-chart
 * https://www.craftyarncouncil.com/standards/child-youth-sizes
 * https://www.craftyarncouncil.com/standards/woman-size
 * https://www.craftyarncouncil.com/standards/man-size
 * https://www.craftyarncouncil.com/standards/head-circumference-chart
 * https://www.craftyarncouncil.com/standards/hand-size-charts
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
            Category.BABY, Category.MEN -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_chest,
                    R.string.size_col_waist,
                    R.string.size_col_hips,
                )
            }

            Category.CHILD_YOUTH -> {
                listOf(
                    R.string.size_col_size,
                    R.string.size_col_chest,
                    R.string.size_col_waist,
                    R.string.size_col_hips,
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

    private fun m(
        cm: Double,
        maxCm: Double = cm,
    ): SizeMeasurement = SizeMeasurement(cm, cm / 2.54, maxCm)

    private fun res(resId: Int): SizeLabel = SizeLabel.Resource(resId)

    fun formatMeasurementValue(
        measurement: SizeMeasurement,
        useImperial: Boolean,
        locale: Locale = Locale.getDefault(),
    ): String {
        val value = if (useImperial) measurement.inches else measurement.cm

        fun format(number: Double): String {
            val fractionDigits = if (number == number.toLong().toDouble()) 0 else 1
            return formatDecimalForDisplay(number, locale, fractionDigits, fractionDigits)
        }
        val maximum = if (useImperial) measurement.maxInches else measurement.maxCm
        return if (measurement.cm == measurement.maxCm) format(value) else "${format(value)}–${format(maximum)}"
    }

    private val babyEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_age_0_3m), listOf(m(40.5), m(45.5), m(48.0))),
            SizeChartEntry(res(R.string.size_row_age_3_6m), listOf(m(43.0), m(48.0), m(50.5))),
            SizeChartEntry(res(R.string.size_row_age_6_12m), listOf(m(45.5), m(50.5), m(50.5))),
            SizeChartEntry(res(R.string.size_row_age_12_18m), listOf(m(48.0), m(52.0), m(53.5))),
            SizeChartEntry(res(R.string.size_row_age_18_24m), listOf(m(50.5), m(53.5), m(56.0))),
        )

    private val childEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_child_2), listOf(m(53.0), m(53.5), m(56.0), m(21.5))),
            SizeChartEntry(res(R.string.size_row_child_4), listOf(m(58.5), m(54.5), m(59.5), m(26.5))),
            SizeChartEntry(res(R.string.size_row_child_6), listOf(m(63.5), m(57.0), m(63.5), m(29.0))),
            SizeChartEntry(res(R.string.size_row_child_8), listOf(m(67.0), m(59.5), m(71.0), m(31.5))),
            SizeChartEntry(res(R.string.size_row_child_10), listOf(m(71.0), m(62.0), m(75.0), m(34.5))),
            SizeChartEntry(res(R.string.size_row_child_12), listOf(m(76.0), m(63.5), m(80.0), m(38.0))),
            SizeChartEntry(res(R.string.size_row_child_14), listOf(m(80.0), m(67.5), m(83.5), m(40.5))),
            SizeChartEntry(res(R.string.size_row_child_16), listOf(m(82.5), m(69.5), m(90.0), m(42.0))),
        )

    private val womenEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_women_xs_2_4), listOf(m(71.0, 76.0), m(58.5, 61.0), m(83.5, 86.0))),
            SizeChartEntry(res(R.string.size_row_women_s_6_8), listOf(m(81.0, 86.0), m(63.5, 67.5), m(89.0, 91.5))),
            SizeChartEntry(res(R.string.size_row_women_m_10_12), listOf(m(91.5, 96.5), m(71.0, 76.0), m(96.5, 101.5))),
            SizeChartEntry(
                res(R.string.size_row_women_l_14_16),
                listOf(m(101.5, 106.5), m(81.5, 86.5), m(106.5, 111.5)),
            ),
            SizeChartEntry(
                res(R.string.size_row_women_xl_18_20),
                listOf(m(111.5, 117.0), m(91.5, 96.5), m(116.5, 122.0)),
            ),
            SizeChartEntry(
                res(R.string.size_row_women_2xl_22_24),
                listOf(m(122.0, 127.0), m(101.5, 106.5), m(132.0, 134.5)),
            ),
            SizeChartEntry(
                res(R.string.size_row_women_3xl_26_28),
                listOf(m(132.0, 137.0), m(111.5, 114.0), m(137.0, 139.5)),
            ),
            SizeChartEntry(
                res(R.string.size_row_women_4xl_30_32),
                listOf(m(142.0, 147.0), m(116.5, 119.0), m(142.0, 144.5)),
            ),
            SizeChartEntry(
                res(R.string.size_row_women_5xl_34_36),
                listOf(m(152.0, 158.0), m(124.0, 127.0), m(155.0, 157.0)),
            ),
        )

    private val menEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_men_s_34_36), listOf(m(86.0, 91.5), m(71.0, 76.0), m(89.0, 94.0))),
            SizeChartEntry(res(R.string.size_row_men_m_38_40), listOf(m(96.5, 101.5), m(81.5, 86.5), m(99.0, 104.0))),
            SizeChartEntry(res(R.string.size_row_men_l_42_44), listOf(m(106.5, 111.5), m(91.5, 96.5), m(109.0, 114.0))),
            SizeChartEntry(
                res(R.string.size_row_men_xl_46_48),
                listOf(m(116.5, 122.0), m(106.5, 112.0), m(119.0, 124.5)),
            ),
            SizeChartEntry(
                res(R.string.size_row_men_2xl_50_52),
                listOf(m(127.0, 132.0), m(117.0, 122.0), m(129.0, 134.0)),
            ),
            SizeChartEntry(
                res(R.string.size_row_men_3xl_54_56),
                listOf(m(137.0, 142.0), m(127.0, 132.0), m(137.0, 142.0)),
            ),
            SizeChartEntry(
                res(R.string.size_row_men_4xl_58_60),
                listOf(m(147.5, 152.0), m(137.0, 142.0), m(142.0, 147.5)),
            ),
            SizeChartEntry(
                res(R.string.size_row_men_5xl_62_64),
                listOf(m(157.5, 162.5), m(147.5, 152.5), m(147.5, 152.5)),
            ),
        )

    private val headEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_preemie), listOf(m(23.0, 30.5))),
            SizeChartEntry(res(R.string.size_row_baby), listOf(m(35.5, 40.5))),
            SizeChartEntry(res(R.string.size_row_toddler), listOf(m(40.5, 46.0))),
            SizeChartEntry(res(R.string.size_row_child), listOf(m(45.5, 51.0))),
            SizeChartEntry(res(R.string.size_row_tween), listOf(m(51.0, 56.0))),
            SizeChartEntry(res(R.string.category_women), listOf(m(53.0, 58.5))),
            SizeChartEntry(res(R.string.category_men), listOf(m(56.0, 61.0))),
        )

    private val handEntries =
        listOf(
            SizeChartEntry(res(R.string.size_row_child_s), listOf(m(13.0), m(10.0))),
            SizeChartEntry(res(R.string.size_row_child_m), listOf(m(15.0), m(12.0))),
            SizeChartEntry(res(R.string.size_row_child_l), listOf(m(16.5), m(13.5))),
            SizeChartEntry(
                SizeLabel.Resource(R.string.size_row_women_s_6_8, R.string.category_women),
                listOf(m(18.0), m(16.0)),
            ),
            SizeChartEntry(
                SizeLabel.Resource(R.string.size_row_women_m_10_12, R.string.category_women),
                listOf(m(19.0), m(16.5)),
            ),
            SizeChartEntry(
                SizeLabel.Resource(R.string.size_row_women_l_14_16, R.string.category_women),
                listOf(m(20.5), m(19.0)),
            ),
            SizeChartEntry(
                SizeLabel.Resource(R.string.size_row_men_s_34_36, R.string.category_men),
                listOf(m(20.5), m(19.0)),
            ),
            SizeChartEntry(
                SizeLabel.Resource(R.string.size_row_men_m_38_40, R.string.category_men),
                listOf(m(21.5), m(20.0)),
            ),
            SizeChartEntry(
                SizeLabel.Resource(R.string.size_row_men_l_42_44, R.string.category_men),
                listOf(m(23.5), m(21.5)),
            ),
        )
}
