package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapingCounterLogicTest {
    // calculateCurrentStitches

    @Test
    fun `calculateCurrentStitches palauttaa aloitusarvon rivillä 0`() {
        assertEquals(80, ShapingCounterLogic.calculateCurrentStitches(80, -2, 4, 0))
    }

    @Test
    fun `calculateCurrentStitches kaventaa oikein`() {
        // 80 sts, -2 joka 4. rivi. Rivillä 8: 2 kavennusta tehty → 80 - 4 = 76
        assertEquals(76, ShapingCounterLogic.calculateCurrentStitches(80, -2, 4, 8))
    }

    @Test
    fun `calculateCurrentStitches lisää oikein`() {
        // 60 sts, +2 joka 6. rivi. Rivillä 18: 3 lisäystä → 60 + 6 = 66
        assertEquals(66, ShapingCounterLogic.calculateCurrentStitches(60, 2, 6, 18))
    }

    @Test
    fun `calculateCurrentStitches ei laske muotoilurivin välissä`() {
        // Rivillä 5 (ei muotoilurivi kun shapeEveryN=4): 1 kavennus tehty (rivi 4)
        assertEquals(78, ShapingCounterLogic.calculateCurrentStitches(80, -2, 4, 5))
    }

    @Test
    fun `calculateCurrentStitches ei mene negatiiviseksi`() {
        assertEquals(0, ShapingCounterLogic.calculateCurrentStitches(4, -2, 1, 100))
    }

    @Test
    fun `calculateCurrentStitches nolla-shapeEveryN palauttaa aloitusarvon`() {
        assertEquals(80, ShapingCounterLogic.calculateCurrentStitches(80, -2, 0, 10))
    }

    // nextShapingRow

    @Test
    fun `nextShapingRow palauttaa seuraavan muotoilurivin`() {
        assertEquals(8, ShapingCounterLogic.nextShapingRow(4, 5))
    }

    @Test
    fun `nextShapingRow muotoilurivin kohdalla palauttaa seuraavan`() {
        assertEquals(8, ShapingCounterLogic.nextShapingRow(4, 4))
    }

    @Test
    fun `nextShapingRow rivillä 0 palauttaa ensimmäisen muotoilurivin`() {
        assertEquals(4, ShapingCounterLogic.nextShapingRow(4, 0))
    }

    // isShapingRow

    @Test
    fun `isShapingRow tunnistaa muotoilurivin`() {
        assertTrue(ShapingCounterLogic.isShapingRow(4, 8))
    }

    @Test
    fun `isShapingRow palauttaa false ei-muotoiluriveille`() {
        assertFalse(ShapingCounterLogic.isShapingRow(4, 5))
    }

    @Test
    fun `isShapingRow rivi 0 ei ole muotoilurivi`() {
        assertFalse(ShapingCounterLogic.isShapingRow(4, 0))
    }

    @Test
    fun `isShapingRow negatiivinen shapeEveryN palauttaa false`() {
        assertFalse(ShapingCounterLogic.isShapingRow(-1, 4))
    }
}
