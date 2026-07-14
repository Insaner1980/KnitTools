package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStartupSourceTest {
    @Test
    fun `app startup applies stored language without blocking main thread`() {
        val app = ProjectSourceFiles.read(APP)

        assertFalse(app.contains("import kotlinx.coroutines.runBlocking"))
        assertFalse(app.contains("runBlocking {"))
        assertTrue(
            app.contains(
                "@ApplicationScope\n" +
                    "    lateinit var applicationScope: CoroutineScope",
            ),
        )
        assertTrue(
            app.contains(
                "applicationScope.launch {\n" +
                    "            preferencesManager.get().applyStoredAppLanguage()\n" +
                    "        }",
            ),
        )
    }

    @Test
    fun `app startup schedules yarn photo orphan cleanup without blocking main thread`() {
        val app = ProjectSourceFiles.read(APP)

        assertTrue(app.contains("import com.finnvek.knittools.repository.YarnCardRepository"))
        assertTrue(app.contains("lateinit var yarnCardRepository: dagger.Lazy<YarnCardRepository>"))
        assertTrue(
            app.contains(
                "applicationScope.launch {\n" +
                    "            yarnCardRepository.get().pruneUnreferencedPhotoFiles()\n" +
                    "        }",
            ),
        )
    }

    @Test
    fun `app startup schedules stale pattern capture cleanup without blocking main thread`() {
        val app = ProjectSourceFiles.read(APP)

        assertTrue(app.contains("import com.finnvek.knittools.data.storage.PatternDocumentStorage"))
        assertTrue(app.contains("lateinit var patternDocumentStorage: dagger.Lazy<PatternDocumentStorage>"))
        assertTrue(
            app.contains(
                "applicationScope.launch(ioDispatcher) {\n" +
                    "            patternDocumentStorage.get().pruneStaleCaptureImages(this@App)\n" +
                    "        }",
            ),
        )
    }

    @Test
    fun `widget pro gates wait for initial pro state before failing closed`() {
        val billingManager = ProjectSourceFiles.read(BILLING_MANAGER)
        val proManager = ProjectSourceFiles.read(PRO_MANAGER)
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)
        val actions = ProjectSourceFiles.read(COUNTER_WIDGET_ACTIONS)

        assertTrue(
            billingManager.contains(
                "val purchaseStateReady: StateFlow<Boolean> = _purchaseStateReady.asStateFlow()",
            ),
        )
        assertTrue(proManager.contains("suspend fun hasFeatureAfterInitialLoad(feature: ProFeature): Boolean"))
        assertTrue(proManager.contains("billingManager.purchaseStateReady.first { it }"))
        assertTrue(proManager.contains("billingManager.isProPurchased.value || hasFeature(feature)"))
        assertTrue(widget.contains("hasFeatureAfterInitialLoad(ProFeature.WIDGET)"))
        assertTrue(actions.contains("hasFeatureAfterInitialLoad(ProFeature.WIDGET)"))
        assertFalse(widget.contains("hasFeature(ProFeature.WIDGET)"))
        assertFalse(actions.contains("hasFeature(ProFeature.WIDGET)"))
        assertFalse(actions.contains("android.util.Log"))
        assertFalse(actions.contains("Log."))
    }

    @Test
    fun `release-visible logging paths do not call Android Log`() {
        listOf(PRO_MANAGER, IN_APP_UPDATE_MANAGER, PREFERENCES_DATA_STORE_SAFETY).forEach { path ->
            val source = ProjectSourceFiles.read(path)

            assertFalse(source.contains("android.util.Log"))
            assertFalse(source.contains("Log."))
        }
    }

    @Test
    fun `widget pro gate changes refresh placed widgets after initial pro state`() {
        val app = ProjectSourceFiles.read(APP)
        val widgetState = ProjectSourceFiles.read(COUNTER_WIDGET_STATE)

        assertTrue(app.contains("import com.finnvek.knittools.pro.ProFeature"))
        assertTrue(app.contains("import com.finnvek.knittools.widget.CounterWidgetState"))
        assertTrue(app.contains("manager.initialStateReady.first { it }"))
        assertTrue(app.contains(".hasFeatureFlow(ProFeature.WIDGET)"))
        assertTrue(app.contains(".collect { CounterWidgetState.refreshAll(this@App) }"))
        assertTrue(widgetState.contains("suspend fun refreshAll(context: Context)"))
        assertTrue(widgetState.contains("widget.update(context, glanceId)"))
    }

    private companion object {
        const val APP = "app/src/main/java/com/finnvek/knittools/App.kt"
        const val BILLING_MANAGER = "app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt"
        const val PRO_MANAGER = "app/src/main/java/com/finnvek/knittools/pro/ProManager.kt"
        const val IN_APP_UPDATE_MANAGER = "app/src/main/java/com/finnvek/knittools/pro/InAppUpdateManager.kt"
        const val PREFERENCES_DATA_STORE_SAFETY =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesDataStoreSafety.kt"
        const val COUNTER_WIDGET = "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt"
        const val COUNTER_WIDGET_ACTIONS = "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt"
        const val COUNTER_WIDGET_STATE = "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetState.kt"
    }
}
