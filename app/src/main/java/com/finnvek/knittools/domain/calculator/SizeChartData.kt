package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SizeChartEntry
import com.finnvek.knittools.domain.model.SizeLabel
import com.finnvek.knittools.domain.model.SizeMeasurement

/**
 * Standardikokotaulukot (Craft Yarn Council). Mitat senttimetreinä ja tuumina.
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

    private fun lit(text: String): SizeLabel = SizeLabel.Literal(text)

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
            SizeChartEntry(lit("2"), listOf(m(53.5), m(51.0), m(54.5), m(89.0), m(24.0))),
            SizeChartEntry(lit("4"), listOf(m(56.0), m(53.5), m(58.5), m(101.5), m(27.5))),
            SizeChartEntry(lit("6"), listOf(m(63.5), m(56.0), m(63.5), m(114.5), m(31.0))),
            SizeChartEntry(lit("8"), listOf(m(67.5), m(58.5), m(68.5), m(127.0), m(34.5))),
            SizeChartEntry(lit("10"), listOf(m(71.0), m(61.0), m(72.5), m(139.5), m(37.0))),
            SizeChartEntry(lit("12"), listOf(m(76.0), m(63.5), m(77.5), m(149.5), m(39.5))),
            SizeChartEntry(lit("14"), listOf(m(80.0), m(66.0), m(82.5), m(155.0), m(42.0))),
            SizeChartEntry(lit("16"), listOf(m(84.0), m(68.5), m(87.5), m(162.5), m(44.5))),
        )

    private val womenEntries =
        listOf(
            SizeChartEntry(lit("XS (2–4)"), listOf(m(78.5), m(60.0), m(85.0))),
            SizeChartEntry(lit("S (6–8)"), listOf(m(84.0), m(66.0), m(91.5))),
            SizeChartEntry(lit("M (10–12)"), listOf(m(91.5), m(73.5), m(99.0))),
            SizeChartEntry(lit("L (14–16)"), listOf(m(99.0), m(81.5), m(106.5))),
            SizeChartEntry(lit("XL (18–20)"), listOf(m(109.0), m(91.5), m(117.0))),
            SizeChartEntry(lit("2XL (22–24)"), listOf(m(119.5), m(101.5), m(127.0))),
            SizeChartEntry(lit("3XL (26–28)"), listOf(m(130.0), m(112.0), m(137.0))),
            SizeChartEntry(lit("4XL (30–32)"), listOf(m(140.0), m(122.0), m(147.5))),
            SizeChartEntry(lit("5XL (34–36)"), listOf(m(150.0), m(132.0), m(157.5))),
        )

    private val menEntries =
        listOf(
            SizeChartEntry(lit("XS (30–32)"), listOf(m(81.5), m(68.5), m(84.0))),
            SizeChartEntry(lit("S (34–36)"), listOf(m(91.5), m(76.0), m(91.5))),
            SizeChartEntry(lit("M (38–40)"), listOf(m(101.5), m(84.0), m(99.0))),
            SizeChartEntry(lit("L (42–44)"), listOf(m(112.0), m(91.5), m(106.5))),
            SizeChartEntry(lit("XL (46–48)"), listOf(m(122.0), m(101.5), m(114.5))),
            SizeChartEntry(lit("2XL (50–52)"), listOf(m(132.0), m(112.0), m(124.5))),
            SizeChartEntry(lit("3XL (54–56)"), listOf(m(142.0), m(122.0), m(134.5))),
            SizeChartEntry(lit("4XL (58–60)"), listOf(m(152.0), m(132.0), m(144.5))),
            SizeChartEntry(lit("5XL (62–64)"), listOf(m(162.5), m(142.0), m(154.5))),
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
