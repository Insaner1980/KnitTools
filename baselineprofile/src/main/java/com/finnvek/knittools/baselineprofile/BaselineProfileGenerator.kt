package com.finnvek.knittools.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generoi Baseline Profilen KnitTools-sovellukselle.
 *
 * Aja: ./gradlew :app:generateBaselineProfile
 * Tai yhdistetyllä laitteella: ./gradlew :baselineprofile:connectedAndroidTest
 *
 * Profiili esikääntää kriittiset koodipolut (Compose, navigaatio, laskurit)
 * ja parantaa käynnistysaikaa ja suorituskykyä.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() =
        baselineProfileRule.collect(
            packageName = "com.finnvek.knittools",
        ) {
            // Käynnistys ja ensimmäisen ruudun renderöinti — tärkein Compose-profiili
            startActivityAndWait()
        }
}
