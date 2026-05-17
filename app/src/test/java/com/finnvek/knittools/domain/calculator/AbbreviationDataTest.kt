package com.finnvek.knittools.domain.calculator

import android.content.Context
import com.finnvek.knittools.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbbreviationDataTest {
    @Test
    fun `all abbreviations have non-blank fields`() {
        AbbreviationData.abbreviations.forEach { abbr ->
            assertTrue("abbreviation tyhjä: $abbr", abbr.abbreviation.isNotBlank())
            assertTrue("meaningResId puuttuu: $abbr", abbr.meaningResId != 0)
            assertTrue("descriptionResId puuttuu: $abbr", abbr.descriptionResId != 0)
        }
    }

    @Test
    fun `no duplicate abbreviations`() {
        val abbrs = AbbreviationData.abbreviations.map { it.abbreviation }
        assertEquals(
            "Duplikaatit: ${abbrs.groupBy { it }.filter { it.value.size > 1 }.keys}",
            abbrs.size,
            abbrs.distinct().size,
        )
    }

    @Test
    fun `no duplicate meaning res ids`() {
        val resIds = AbbreviationData.abbreviations.map { it.meaningResId }
        assertEquals(
            "meaningResId-duplikaatit: ${resIds.groupBy { it }.filter { it.value.size > 1 }.keys}",
            resIds.size,
            resIds.distinct().size,
        )
    }

    @Test
    fun `common abbreviations exist`() {
        val abbrs = AbbreviationData.abbreviations.map { it.abbreviation }
        listOf("K", "P", "YO", "CO", "BO", "K2tog", "SSK", "M1L", "M1R").forEach {
            assertTrue("$it puuttuu", it in abbrs)
        }
    }

    @Test
    fun `abbreviations are sorted for browsing`() {
        val abbrs = AbbreviationData.abbreviations.map { it.abbreviation }
        val sorted = abbrs.sortedWith(String.CASE_INSENSITIVE_ORDER)

        assertEquals(sorted, abbrs)
    }

    @Test
    fun `search matches abbreviation descriptions`() {
        val context =
            mockk<Context> {
                every { getString(any()) } answers {
                    when (firstArg<Int>()) {
                        R.string.abbr_yo_meaning -> "Yarn over"
                        R.string.abbr_yo_desc -> "Wrap yarn around needle to create a new stitch and decorative hole"
                        else -> ""
                    }
                }
            }

        val results = AbbreviationData.search(context, "decorative hole")

        assertTrue(results.any { it.abbreviation == "YO" })
    }
}
