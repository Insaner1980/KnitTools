package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.SizeChartEntry
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

    fun headers(category: Category): List<String> =
        when (category) {
            Category.BABY -> listOf("Size", "Chest", "Waist", "Hips", "Head", "Length")
            Category.CHILD_YOUTH -> listOf("Size", "Chest", "Waist", "Hips", "Height", "Arm")
            Category.WOMEN -> listOf("Size", "Bust", "Waist", "Hips")
            Category.MEN -> listOf("Size", "Chest", "Waist", "Hips")
            Category.HEAD -> listOf("Size", "Circumference")
            Category.HAND -> listOf("Size", "Circumference", "Length")
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

    private val babyEntries =
        listOf(
            SizeChartEntry("Preemie", listOf(m(30.5), m(30.5), m(31.0), m(30.5), m(38.0))),
            SizeChartEntry("0–3m", listOf(m(40.5), m(40.5), m(43.0), m(35.5), m(53.5))),
            SizeChartEntry("3–6m", listOf(m(43.0), m(43.0), m(45.5), m(38.0), m(58.5))),
            SizeChartEntry("6–12m", listOf(m(47.0), m(47.0), m(47.0), m(43.0), m(66.0))),
            SizeChartEntry("12–18m", listOf(m(50.5), m(49.5), m(50.5), m(47.0), m(73.5))),
            SizeChartEntry("18–24m", listOf(m(52.0), m(50.5), m(52.0), m(48.5), m(78.5))),
        )

    private val childEntries =
        listOf(
            SizeChartEntry("2", listOf(m(53.5), m(51.0), m(54.5), m(89.0), m(24.0))),
            SizeChartEntry("4", listOf(m(56.0), m(53.5), m(58.5), m(101.5), m(27.5))),
            SizeChartEntry("6", listOf(m(63.5), m(56.0), m(63.5), m(114.5), m(31.0))),
            SizeChartEntry("8", listOf(m(67.5), m(58.5), m(68.5), m(127.0), m(34.5))),
            SizeChartEntry("10", listOf(m(71.0), m(61.0), m(72.5), m(139.5), m(37.0))),
            SizeChartEntry("12", listOf(m(76.0), m(63.5), m(77.5), m(149.5), m(39.5))),
            SizeChartEntry("14", listOf(m(80.0), m(66.0), m(82.5), m(155.0), m(42.0))),
            SizeChartEntry("16", listOf(m(84.0), m(68.5), m(87.5), m(162.5), m(44.5))),
        )

    private val womenEntries =
        listOf(
            SizeChartEntry("XS (2–4)", listOf(m(78.5), m(60.0), m(85.0))),
            SizeChartEntry("S (6–8)", listOf(m(84.0), m(66.0), m(91.5))),
            SizeChartEntry("M (10–12)", listOf(m(91.5), m(73.5), m(99.0))),
            SizeChartEntry("L (14–16)", listOf(m(99.0), m(81.5), m(106.5))),
            SizeChartEntry("XL (18–20)", listOf(m(109.0), m(91.5), m(117.0))),
            SizeChartEntry("2XL (22–24)", listOf(m(119.5), m(101.5), m(127.0))),
            SizeChartEntry("3XL (26–28)", listOf(m(130.0), m(112.0), m(137.0))),
            SizeChartEntry("4XL (30–32)", listOf(m(140.0), m(122.0), m(147.5))),
            SizeChartEntry("5XL (34–36)", listOf(m(150.0), m(132.0), m(157.5))),
        )

    private val menEntries =
        listOf(
            SizeChartEntry("XS (30–32)", listOf(m(81.5), m(68.5), m(84.0))),
            SizeChartEntry("S (34–36)", listOf(m(91.5), m(76.0), m(91.5))),
            SizeChartEntry("M (38–40)", listOf(m(101.5), m(84.0), m(99.0))),
            SizeChartEntry("L (42–44)", listOf(m(112.0), m(91.5), m(106.5))),
            SizeChartEntry("XL (46–48)", listOf(m(122.0), m(101.5), m(114.5))),
            SizeChartEntry("2XL (50–52)", listOf(m(132.0), m(112.0), m(124.5))),
            SizeChartEntry("3XL (54–56)", listOf(m(142.0), m(122.0), m(134.5))),
            SizeChartEntry("4XL (58–60)", listOf(m(152.0), m(132.0), m(144.5))),
            SizeChartEntry("5XL (62–64)", listOf(m(162.5), m(142.0), m(154.5))),
        )

    private val headEntries =
        listOf(
            SizeChartEntry("Preemie", listOf(m(30.5))),
            SizeChartEntry("Baby", listOf(m(35.5))),
            SizeChartEntry("Toddler", listOf(m(46.0))),
            SizeChartEntry("Child", listOf(m(50.5))),
            SizeChartEntry("Adult S", listOf(m(53.5))),
            SizeChartEntry("Adult M", listOf(m(56.0))),
            SizeChartEntry("Adult L", listOf(m(58.5))),
        )

    private val handEntries =
        listOf(
            SizeChartEntry("Child S", listOf(m(12.5), m(11.5))),
            SizeChartEntry("Child M", listOf(m(14.0), m(13.5))),
            SizeChartEntry("Child L", listOf(m(15.0), m(14.5))),
            SizeChartEntry("Adult S", listOf(m(17.5), m(17.0))),
            SizeChartEntry("Adult M", listOf(m(19.0), m(18.5))),
            SizeChartEntry("Adult L", listOf(m(21.5), m(20.5))),
            SizeChartEntry("Adult XL", listOf(m(23.0), m(22.0))),
        )
}
