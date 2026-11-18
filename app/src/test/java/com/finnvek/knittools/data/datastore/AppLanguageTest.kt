package com.finnvek.knittools.data.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `empty locale tags map to system language`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
    }

    @Test
    fun `known locale tags map to matching app languages`() {
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromLanguageTag("fi"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en"))
        assertEquals(AppLanguage.SWEDISH, AppLanguage.fromLanguageTag("sv"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromLanguageTag("de"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromLanguageTag("fr"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTag("es"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromLanguageTag("pt"))
        assertEquals(AppLanguage.BRAZILIAN_PORTUGUESE, AppLanguage.fromLanguageTag("pt-BR"))
        assertEquals(AppLanguage.ITALIAN, AppLanguage.fromLanguageTag("it"))
        assertEquals(AppLanguage.NORWEGIAN, AppLanguage.fromLanguageTag("nb"))
        assertEquals(AppLanguage.DANISH, AppLanguage.fromLanguageTag("da"))
        assertEquals(AppLanguage.DUTCH, AppLanguage.fromLanguageTag("nl"))
    }

    @Test
    fun `regional locale tags map to supported base language`() {
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromLanguageTag("pt-PT"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromLanguageTag("pt-AO"))
        assertEquals(AppLanguage.NORWEGIAN, AppLanguage.fromLanguageTag("nb-NO"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en-US"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromLanguageTag("fr-CA"))
    }

    @Test
    fun `Brazilian Portuguese remains distinct in normalized and ordered tags`() {
        assertEquals(AppLanguage.BRAZILIAN_PORTUGUESE, AppLanguage.fromLanguageTag("pt-br"))
        assertEquals(AppLanguage.BRAZILIAN_PORTUGUESE, AppLanguage.fromLanguageTag(" PT-br ,en"))
        assertEquals(AppLanguage.BRAZILIAN_PORTUGUESE, AppLanguage.fromLanguageTag("pt-BR,pt"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromLanguageTag("pt,pt-BR"))
    }

    @Test
    fun `every saved language and supported tag round trips without migration`() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromValue(language.value))
            assertEquals(language, AppLanguage.fromLanguageTag(language.languageTag))
        }
        assertEquals("pt", AppLanguage.PORTUGUESE.value)
        assertEquals("pt-BR", AppLanguage.BRAZILIAN_PORTUGUESE.value)
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromValue(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromValue("unknown"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("ja-JP"))
    }

    @Test
    fun `first locale in list controls app language`() {
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromLanguageTag("fi,en"))
    }
}
