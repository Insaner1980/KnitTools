package com.finnvek.knittools.ui.components

import android.content.res.Configuration
import android.icu.text.PluralRules
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class SkeinCountTextTest(
    private val languageTag: String,
    private val singular: String,
    private val plural: String,
    private val million: String,
    private val zeroUsesOne: Boolean,
    private val hasMany: Boolean,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun androidResourcesAndComposeUseLocaleQuantityAndFormatArgument() {
        val locale = Locale.forLanguageTag(languageTag)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        val context = baseContext.createConfigurationContext(configuration)
        val quantities = listOf(0, 1, 2, 21, 1_000_000, 2_000_000, 1_000_001, Int.MAX_VALUE)
        val expectedWords = listOf(
            if (zeroUsesOne) singular else plural,
            singular,
            plural,
            plural,
            million,
            million,
            plural,
            plural,
        )
        val expectedCategories = listOf(
            if (zeroUsesOne) "one" else "other",
            "one",
            "other",
            "other",
            if (hasMany) "many" else "other",
            if (hasMany) "many" else "other",
            "other",
            "other",
        )
        val rules = PluralRules.forLocale(locale)
        val expectedTexts = quantities.zip(expectedWords) { quantity, word -> "$quantity $word" }

        quantities.forEachIndexed { index, quantity ->
            assertEquals("$languageTag category for $quantity", expectedCategories[index], rules.select(quantity.toDouble()))
            assertEquals(
                "$languageTag resources for $quantity",
                expectedTexts[index],
                context.resources.getQuantityString(R.plurals.skein_count, quantity, quantity),
            )
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
                Column {
                    quantities.forEach { quantity ->
                        Text(skeinCountText(quantity), modifier = Modifier.testTag("skeins-$quantity"))
                    }
                }
            }
        }
        quantities.forEachIndexed { index, quantity ->
            composeRule.onNodeWithTag("skeins-$quantity").assertTextEquals(expectedTexts[index])
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun locales(): List<Array<Any>> = listOf(
            arrayOf("en", "skein", "skeins", "skeins", false, false),
            arrayOf("fi", "kerä", "kerää", "kerää", false, false),
            arrayOf("sv", "nystan", "nystan", "nystan", false, false),
            arrayOf("de", "Knäuel", "Knäuel", "Knäuel", false, false),
            arrayOf("fr", "pelote", "pelotes", "de pelotes", true, true),
            arrayOf("es", "ovillo", "ovillos", "de ovillos", false, true),
            arrayOf("pt", "novelo", "novelos", "de novelos", true, true),
            arrayOf("pt-BR", "novelo", "novelos", "de novelos", true, true),
            arrayOf("pt-PT", "novelo", "novelos", "de novelos", false, true),
            arrayOf("it", "gomitolo", "gomitoli", "di gomitoli", false, true),
            arrayOf("nb", "nøste", "nøster", "nøster", false, false),
            arrayOf("da", "nøgle", "nøgler", "nøgler", false, false),
            arrayOf("nl", "streng", "strengen", "strengen", false, false),
        )
    }
}
