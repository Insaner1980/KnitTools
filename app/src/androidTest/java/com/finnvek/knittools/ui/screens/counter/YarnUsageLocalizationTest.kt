package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class YarnUsageLocalizationTest {
    @Test
    fun roundedSkeinCountMatchesDisplayedQuantity() {
        assertSkeins("en-US", 0.994, "0.99 skeins")
        assertSkeins("en-US", 0.996, "1 skein")
        assertSkeins("en-US", 1.004, "1 skein")
        assertSkeins("en-US", 1.006, "1.01 skeins")
        assertSkeins("fi-FI", 1.004, "1 vyyhti")
        assertSkeins("fi-FI", 1.006, "1,01 vyyhtiä")
        assertSkeins("de-DE", 1.004, "1 Strang")
        assertSkeins("de-DE", 1.006, "1,01 Stränge")
        assertSkeins("fr-FR", 1.994, "1,99 écheveau")
        assertSkeins("fr-FR", 1.996, "2 écheveaux")
        assertSkeins("pt-BR", 1.994, "1,99 meada")
        assertSkeins("pt-BR", 1.996, "2 meadas")
        assertSkeins("pt-PT", 0.996, "1 meada")
        assertSkeins("pt-PT", 1.006, "1,01 meadas")
    }

    @Test
    fun fractionalSkeinCountUsesRegionalPluralRules() {
        assertSkeins("en-US", 0.5, "0.5 skeins")
        assertSkeins("fr-FR", 0.5, "0,5 écheveau")
        assertSkeins("pt-BR", 0.0, "0 meada")
        assertSkeins("pt-BR", 0.5, "0,5 meada")
        assertSkeins("pt-PT", 0.0, "0 meadas")
        assertSkeins("pt-PT", 0.5, "0,5 meadas")
        assertSkeins("pt-PT", 1.0, "1 meada")
        assertSkeins("pt-PT", 1.5, "1,5 meadas")
        assertSkeins("da-DK", 0.5, "0,5 fed")
        assertEquals(1, skeinPluralQuantity("0,5", Locale.forLanguageTag("da-DK")))
    }

    private fun assertSkeins(
        languageTag: String,
        value: Double,
        expected: String,
    ) {
        val locale = Locale.forLanguageTag(languageTag)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources =
            context
                .createConfigurationContext(
                    Configuration(context.resources.configuration).apply { setLocale(locale) },
                ).resources
        val formatted = MeasurementNumberFormatter.format(value, locale)
        assertEquals(
            "$languageTag $value",
            expected,
            resources.getQuantityString(
                R.plurals.yarn_usage_skeins_format,
                skeinPluralQuantity(formatted, locale),
                formatted,
            ),
        )
    }
}
