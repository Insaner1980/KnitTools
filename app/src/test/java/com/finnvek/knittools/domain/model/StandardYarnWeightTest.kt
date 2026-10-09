package com.finnvek.knittools.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StandardYarnWeightTest {
    @Test
    fun `blank and standard weights open the picker`() {
        assertTrue(StandardYarnWeight.isStandardOrBlank(""))
        assertTrue(StandardYarnWeight.isStandardOrBlank("DK"))
        // Ravelry ja vanhat käsin kirjoitetut arvot voivat poiketa kirjainkooltaan tai välilyönneiltä.
        assertTrue(StandardYarnWeight.isStandardOrBlank(" super bulky "))
    }

    @Test
    fun `other weights stay editable text`() {
        assertFalse(StandardYarnWeight.isStandardOrBlank("Light Fingering"))
        assertFalse(StandardYarnWeight.isStandardOrBlank("8-säikeinen"))
    }
}
