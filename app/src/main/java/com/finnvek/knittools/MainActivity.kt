package com.finnvek.knittools

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.InAppUpdateManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.ui.navigation.CounterLaunchIntentData
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest
import com.finnvek.knittools.ui.navigation.KnitToolsNavHost
import com.finnvek.knittools.ui.navigation.TopLevelDestination
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var inAppReviewManager: InAppReviewManager

    @Inject
    lateinit var proManager: ProManager

    @Inject
    lateinit var inAppUpdateManager: InAppUpdateManager

    @Inject
    lateinit var ravelryAuthManager: RavelryAuthManager

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var httpClient: io.ktor.client.HttpClient

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val updateResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) {
            inAppUpdateManager.onUpdateFlowResult()
            // Flexible mode — lataustulos käsitellään installStateListenerissa
        }

    private var counterLaunchRequest by mutableStateOf<CounterLaunchRequest?>(null)
    private var consumedCounterLaunchRequestId: String? = null
    private var startupThemeLoaded = false
    private var edgeToEdgeDarkTheme: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !startupThemeLoaded }
        restoreCounterLaunchRequest(savedInstanceState)
        handleOAuthCallbackIfNeeded(intent)
        inAppUpdateManager.checkForUpdate(updateResultLauncher)
        setContent {
            val prefs by preferencesManager.preferences.collectAsStateWithLifecycle(initialValue = null)
            val isDarkTheme = prefs.resolveStartupDarkTheme(isSystemInDarkTheme())
            if (isDarkTheme == null) {
                return@setContent
            }

            SideEffect {
                applyEdgeToEdgeIfNeeded(isDarkTheme)
                startupThemeLoaded = true
            }

            val activity = this@MainActivity
            LaunchedEffect(Unit) {
                val proState = proManager.proState.first { it.trialStartTimestamp > 0L }
                inAppReviewManager.maybeRequestReview(activity, isPro = proState.isPro)
            }

            // In-App Update: näytä snackbar kun päivitys on ladattu
            val updateDownloaded by inAppUpdateManager.updateDownloaded.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val updateMessage = stringResource(R.string.update_downloaded)
            val restartLabel = stringResource(R.string.restart)
            LaunchedEffect(updateDownloaded) {
                if (updateDownloaded) {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = updateMessage,
                            actionLabel = restartLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        inAppUpdateManager.completeUpdate()
                    }
                }
            }

            KnitToolsTheme(isDarkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KnitToolsNavHost(
                        startDestination = TopLevelDestination.Projects.route,
                        counterLaunchRequest = counterLaunchRequest,
                        snackbarHostState = snackbarHostState,
                        onPurchasePro = billingManager::launchPurchaseFlow,
                        onCounterLaunchHandled = {
                            counterLaunchRequest?.let {
                                consumedCounterLaunchRequestId = it.requestId
                            }
                            counterLaunchRequest = null
                            clearCounterLaunchIntent()
                        },
                    )
                }
            }
        }
    }

    private fun restoreCounterLaunchRequest(savedInstanceState: Bundle?) {
        consumedCounterLaunchRequestId = savedInstanceState?.getString(STATE_CONSUMED_COUNTER_LAUNCH_REQUEST_ID)
        counterLaunchRequest =
            intent.toCounterLaunchRequest(
                consumedRequestId = consumedCounterLaunchRequestId.takeIf { savedInstanceState != null },
            )
    }

    private fun applyEdgeToEdgeIfNeeded(isDarkTheme: Boolean) {
        if (edgeToEdgeDarkTheme == isDarkTheme) return
        edgeToEdgeDarkTheme = isDarkTheme
        val transparent = Color.TRANSPARENT
        val systemBarStyle =
            SystemBarStyle.auto(
                lightScrim = transparent,
                darkScrim = transparent,
                detectDarkMode = { isDarkTheme },
            )
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle,
        )
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.checkDownloadedOnResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        consumedCounterLaunchRequestId?.let {
            outState.putString(STATE_CONSUMED_COUNTER_LAUNCH_REQUEST_ID, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.cleanup()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallbackIfNeeded(intent)
        counterLaunchRequest = intent.toCounterLaunchRequest(consumedRequestId = null)
    }

    private fun handleOAuthCallbackIfNeeded(intent: Intent?) {
        val uri = intent?.data ?: return
        if (ravelryAuthManager.isOAuthCallback(uri)) {
            lifecycleScope.launch {
                val handled = ravelryAuthManager.handleCallback(httpClient, uri)
                if (handled) {
                    clearOAuthCallbackIntent(uri)
                }
            }
        }
    }

    private fun clearOAuthCallbackIntent(uri: Uri) {
        if (intent?.data == uri) {
            intent.data = null
        }
    }

    private fun clearCounterLaunchIntent() {
        intent?.removeExtra(EXTRA_OPEN_COUNTER)
        intent?.removeExtra(EXTRA_PROJECT_ID)
        intent?.removeExtra(EXTRA_COUNTER_LAUNCH_ID)
    }

    private fun Intent?.toCounterLaunchRequest(consumedRequestId: String?): CounterLaunchRequest? {
        if (this == null) return null
        return CounterLaunchRequest.fromIntentData(
            intentData =
                CounterLaunchIntentData(
                    shouldOpenCounter = getBooleanExtra(EXTRA_OPEN_COUNTER, false),
                    projectId = getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L },
                    launchId = getStringExtra(EXTRA_COUNTER_LAUNCH_ID),
                ),
            consumedRequestId = consumedRequestId,
        )
    }

    companion object {
        private const val EXTRA_OPEN_COUNTER = "com.finnvek.knittools.extra.OPEN_COUNTER"
        private const val EXTRA_PROJECT_ID = "com.finnvek.knittools.extra.PROJECT_ID"
        private const val EXTRA_COUNTER_LAUNCH_ID = "com.finnvek.knittools.extra.COUNTER_LAUNCH_ID"
        private const val STATE_CONSUMED_COUNTER_LAUNCH_REQUEST_ID =
            "com.finnvek.knittools.state.CONSUMED_COUNTER_LAUNCH_REQUEST_ID"

        fun createCounterLaunchIntent(
            context: Context,
            projectId: Long?,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_COUNTER, true)
                projectId?.let { putExtra(EXTRA_PROJECT_ID, it) }
                putExtra(EXTRA_COUNTER_LAUNCH_ID, UUID.randomUUID().toString())
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}
