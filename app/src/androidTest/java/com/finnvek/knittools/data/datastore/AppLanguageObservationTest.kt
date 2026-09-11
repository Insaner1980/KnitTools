package com.finnvek.knittools.data.datastore

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageObservationTest {
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun activeObserverReceivesSelectionsAndExternalLocaleSync() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val manager = PreferencesManager(context)
            manager.applyStoredAppLanguage()
            val before = manager.preferences.first().appLanguage
            val locales = checkNotNull(context.getSystemService(LocaleManager::class.java))
            val beforeLocales = locales.applicationLocales
            val observed = Channel<AppLanguage>(Channel.UNLIMITED)
            val collector =
                launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                    manager.preferences.collect { observed.send(it.appLanguage) }
                }

            suspend fun awaitLanguage(expected: AppLanguage) =
                withTimeout(5_000) {
                    do {
                        val language = observed.receive()
                    } while (language != expected)
                }
            try {
                awaitLanguage(before)
                var previous = before
                for (language in listOf(
                    AppLanguage.ENGLISH,
                    AppLanguage.FRENCH,
                    AppLanguage.SYSTEM,
                    AppLanguage.FINNISH,
                )) {
                    manager.setAppLanguage(language)
                    if (language != previous) awaitLanguage(language)
                    previous = language
                    assertEquals(language.languageTag.orEmpty(), locales.applicationLocales.toLanguageTags())
                }
                withContext(Dispatchers.Main) { locales.applicationLocales = LocaleList.forLanguageTags("sv") }
                manager.syncAppLanguageFromSystem()
                awaitLanguage(AppLanguage.SWEDISH)
                assertEquals(AppLanguage.SWEDISH, manager.preferences.first().appLanguage)
            } finally {
                collector.cancel()
                manager.setAppLanguage(before)
                withContext(Dispatchers.Main) { locales.applicationLocales = beforeLocales }
            }
        }
}
