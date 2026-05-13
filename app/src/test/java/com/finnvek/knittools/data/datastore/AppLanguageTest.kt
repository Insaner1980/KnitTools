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
        assertEquals(AppLanguage.ITALIAN, AppLanguage.fromLanguageTag("it"))
        assertEquals(AppLanguage.NORWEGIAN, AppLanguage.fromLanguageTag("nb"))
        assertEquals(AppLanguage.DANISH, AppLanguage.fromLanguageTag("da"))
        assertEquals(AppLanguage.DUTCH, AppLanguage.fromLanguageTag("nl"))
    }

    @Test
    fun `regional locale tags map to supported base language`() {
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromLanguageTag("pt-BR"))
        assertEquals(AppLanguage.NORWEGIAN, AppLanguage.fromLanguageTag("nb-NO"))
    }

    @Test
    fun `first locale in list controls app language`() {
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromLanguageTag("fi,en"))
    }
}
