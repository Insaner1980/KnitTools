package com.finnvek.knittools.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

internal fun Context.localizedTestContext(language: String): Context =
    createConfigurationContext(
        Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) },
    )

internal fun localeThemeCases(): List<Array<Any>> =
    listOf("en", "fi", "de", "fr").flatMap { language ->
        listOf(false, true).map { dark -> arrayOf(language, dark) }
    }
