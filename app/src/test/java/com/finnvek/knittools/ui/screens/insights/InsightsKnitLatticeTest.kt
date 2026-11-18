package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.ui.theme.InsightsDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Silmukkaruudukon mitoitus. Pinta on koriste pylvään sisällä, joten se ei saa
 * kasvaa niin paksuksi että pylvään keskimääräinen väri putoaa alle 3:1 kontrastin.
 */
class InsightsKnitLatticeTest {
    @Test
    fun `a narrow month bar still gets two stitch columns`() {
        // 10 dp pylväs 6 dp tavoitteella pyöristyisi kahteen, mutta kapeampikaan
        // ei saa pudota yhteen sarakkeeseen: yksi sarake on raita, ei neule.
        assertEquals(MIN_STITCH_COLUMNS, knitStitchMetrics(widthPx = 12f, targetStitchWidthPx = 18f).columns)
        assertEquals(2, knitStitchMetrics(widthPx = 30f, targetStitchWidthPx = 18f).columns)
    }

    @Test
    fun `wider bars get proportionally more stitches, not bigger ones`() {
        val month = knitStitchMetrics(widthPx = 30f, targetStitchWidthPx = 18f)
        val week = knitStitchMetrics(widthPx = 102f, targetStitchWidthPx = 18f)

        assertTrue(week.columns > month.columns)
        // Silmukan leveys pysyy samassa suuruusluokassa, joten pinta näyttää
        // samalta neuleelta aikavälistä riippumatta.
        assertTrue(week.stitchWidthPx / month.stitchWidthPx < 1.5f)
    }

    @Test
    fun `stroke width follows the stitch, not the bar`() {
        val month = knitStitchMetrics(widthPx = 30f, targetStitchWidthPx = 18f)
        val week = knitStitchMetrics(widthPx = 102f, targetStitchWidthPx = 18f)

        assertTrue(week.strokeWidthPx / month.strokeWidthPx < 1.5f)
    }

    @Test
    fun `a stitch is wider than it is tall, like real stockinette`() {
        val metrics = knitStitchMetrics(widthPx = 102f, targetStitchWidthPx = 18f)

        assertTrue(metrics.stitchHeightPx < metrics.stitchWidthPx)
    }

    @Test
    fun `shadow stays light enough to keep the bar readable`() {
        assertTrue(InsightsDimens.ChartStitchShadowAlpha <= 0.2f)
        assertTrue(InsightsDimens.ChartStitchStrokeRatio <= 0.2f)
    }

    @Test
    fun `degenerate sizes never produce a zero stroke or a single column`() {
        listOf(
            knitStitchMetrics(widthPx = 0f, targetStitchWidthPx = 18f),
            knitStitchMetrics(widthPx = 30f, targetStitchWidthPx = 0f),
        ).forEach {
            assertTrue(it.strokeWidthPx > 0f)
            assertTrue(it.columns >= MIN_STITCH_COLUMNS)
        }
    }

    @Test
    fun `project fabric reuses the chart stitch lattice and tokens`() {
        val fabric = ProjectSourceFiles.read(INSIGHTS_PROJECT_FABRIC)

        assertTrue(fabric.contains("knitStitchLattice("))
        assertTrue(fabric.contains("InsightsDimens.ChartStitchTargetWidth"))
        assertTrue(fabric.contains("InsightsDimens.ChartStitchAspect"))
        assertTrue(fabric.contains("InsightsDimens.ChartStitchStrokeRatio"))
        assertTrue(fabric.contains("InsightsDimens.ChartStitchShadowAlpha"))
        assertTrue(fabric.contains("StrokeCap.Round"))
        assertFalse(fabric.contains("FabricStitch"))
    }

    private companion object {
        const val INSIGHTS_PROJECT_FABRIC =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsProjectFabric.kt"
    }
}
