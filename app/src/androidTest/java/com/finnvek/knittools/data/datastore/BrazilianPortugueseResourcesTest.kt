package com.finnvek.knittools.data.datastore

import android.content.Context
import android.content.res.Configuration
import android.icu.text.PluralRules
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatDecimalForDisplay
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

class BrazilianPortugueseResourcesTest {
    @Test
    fun brazilianResourcesAndFormattingUseTheRegionalLocale() {
        val locale = Locale.forLanguageTag("pt-BR")
        val context = localizedContext(locale)
        assertEquals(
            "pt-BR",
            context.resources.configuration.locales[0]
                .toLanguageTag(),
        )
        assertEquals("Configurações", context.getString(R.string.settings))
        assertEquals("Salvar", context.getString(R.string.save))
        assertEquals("Crochê", context.getString(R.string.craft_type_crochet))
        assertEquals("Tricô", context.getString(R.string.sym_purl_name))
        assertEquals("Meus fios", context.getString(R.string.my_yarn_title))
        assertEquals("Português (Brasil)", context.getString(R.string.settings_language_brazilian_portuguese))
        assertEquals("1.234,5", formatDecimalForDisplay(1234.5, locale, 1, 1))
        val date = GregorianCalendar(2026, 9, 19).time
        assertEquals("outubro", SimpleDateFormat("MMMM", locale).format(date))
        assertEquals("19/10/2026", SimpleDateFormat(localizedDateTimePattern(locale, "yMd"), locale).format(date))
        assertEquals("Guardar", localizedContext(Locale.forLanguageTag("pt")).getString(R.string.save))
    }

    @Test
    fun brazilianZeroOneAndMultipleQuantitiesPreserveSelectionAndFormatting() {
        val locale = Locale.forLanguageTag("pt-BR")
        val resources = localizedContext(locale).resources
        val rules = PluralRules.forLocale(locale)
        val millionCategory = if ("many" in rules.keywords) "many" else "other"
        listOf(0 to "one", 1 to "one", 2 to "other", 1_000_000 to millionCategory).forEach { (quantity, category) ->
            assertEquals(category, rules.select(quantity.toDouble()))
            val singular = quantity <= 1
            assertEquals(
                "$quantity ${if (singular) "carreira" else "carreiras"}",
                resources.getQuantityString(R.plurals.rows_format, quantity, quantity),
            )
            assertEquals(
                "$quantity ${if (singular) "volta" else "voltas"}",
                resources.getQuantityString(R.plurals.rounds_format, quantity, quantity),
            )
            assertEquals(
                "$quantity ${if (singular) "novelo" else "novelos"}",
                resources.getQuantityString(R.plurals.skein_count, quantity, quantity),
            )
            assertEquals(
                "há $quantity ${if (singular) "mês" else "meses"}",
                resources.getQuantityString(R.plurals.relative_time_months_ago, quantity, quantity),
            )
        }
        assertEquals("1,5 novelo", resources.getQuantityString(R.plurals.yarn_usage_skeins_format, 1, "1,5"))
        assertEquals("2,5 novelos", resources.getQuantityString(R.plurals.yarn_usage_skeins_format, 2, "2,5"))
    }

    private fun localizedContext(locale: Locale): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(locale) },
        )
    }
}
