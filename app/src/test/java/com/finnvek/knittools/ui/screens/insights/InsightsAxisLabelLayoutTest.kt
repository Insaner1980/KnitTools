package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.ui.theme.InsightsDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Akselileimojen asettelusäännöt. "today" piirretään oikeaan reunaan ilman
 * solurajaa, joten sen alle jäävät numeroleimat on pudotettava pois.
 */
class InsightsAxisLabelLayoutTest {
    @Test
    fun `today leaves room so the last numeric label cannot collide with it`() {
        val lastIndex = 17
        val clearance = todayLabelClearance(bucketCount = 18)
        val labelled =
            axisLabelIndices(bucketCount = 18, maxLabels = 5)
                .filterNot { lastIndex - it < clearance }

        // Ilman suodatusta indeksi 16 piirtyi "17":ksi tasan "today"-leiman päälle.
        assertEquals(listOf(0, 4, 8, 12), labelled)
        assertTrue(labelled.all { lastIndex - it >= clearance })
    }

    @Test
    fun `a short week keeps every weekday label`() {
        // Kiinteä kolmen solun varaus söi kolmen päivän viikolta kaikki leimat ja jätti
        // akselille pelkän "today". Leveiä soluja ei tarvitse varata.
        val lastIndex = 2
        val clearance = todayLabelClearance(bucketCount = 3)
        val labelled =
            axisLabelIndices(bucketCount = 3, maxLabels = 3)
                .filterNot { lastIndex - it < clearance }

        assertEquals(0, clearance)
        assertEquals(listOf(0, 1, 2), labelled)
    }

    @Test
    fun `a sparse chart gets a shorter plot than a dense one`() {
        val dense = chartPlotHeight(bucketCount = 18, activeBucketCount = 14)

        assertTrue(chartPlotHeight(bucketCount = 3, activeBucketCount = 1) < dense)
        assertTrue(chartPlotHeight(bucketCount = 10, activeBucketCount = 4) < dense)
        assertEquals(dense, chartPlotHeight(bucketCount = 31, activeBucketCount = 20))
    }

    @Test
    fun `a long axis with only a couple of bars is still a sparse chart`() {
        // 18 päivän akseli, jolla on kaksi pylvästä, on yhtä tyhjä kuin kahden ämpärin akseli.
        assertEquals(
            chartPlotHeight(bucketCount = 3, activeBucketCount = 1),
            chartPlotHeight(bucketCount = 18, activeBucketCount = 2),
        )
        assertTrue(
            chartPlotHeight(bucketCount = 18, activeBucketCount = 2) <
                chartPlotHeight(bucketCount = 18, activeBucketCount = 14),
        )
    }

    @Test
    fun `a range that does not end today keeps every thinned label`() {
        val labelled =
            axisLabelIndices(bucketCount = 18, maxLabels = 5)
                .filterNot { false }

        assertEquals(listOf(0, 4, 8, 12, 16), labelled)
    }

    @Test
    fun `sparse axes centre their labels instead of pinning them to the edges`() {
        // Kahdella ämpärillä pylväät ovat 25 % ja 75 % kohdalla; reunaan pinnattu
        // leima jäi näkyvästi oman pylväänsä ohi.
        assertFalse(2 > SPARSE_AXIS_MAX_BUCKETS)
        assertFalse(4 > SPARSE_AXIS_MAX_BUCKETS)
        assertTrue(18 > SPARSE_AXIS_MAX_BUCKETS)
    }

    @Test
    fun `the filter pill is visually lighter than the touch target it keeps`() {
        // Visuaalinen korkeus saa alittaa kosketuskohteen minimin, koska
        // minimumInteractiveComponentSize() laajentaa osumakohteen erikseen.
        assertTrue(InsightsDimens.FilterPillHeight < InsightsDimens.FilterChipMinTouchTarget)
    }
}
