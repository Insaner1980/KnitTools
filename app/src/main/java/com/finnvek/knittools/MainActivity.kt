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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.storage.CounterLaunchTokenStore
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.InAppUpdateManager
import com.finnvek.knittools.ui.navigation.CounterLaunchIntentData
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest
import com.finnvek.knittools.ui.navigation.KnitToolsNavHost
import com.finnvek.knittools.ui.navigation.TopLevelDestination
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var inAppReviewManager: InAppReviewManager

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
        ) { result ->
            inAppUpdateManager.onUpdateFlowResult(result.resultCode)
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
        checkForInAppUpdate()
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
                inAppReviewManager.maybeRequestReview(activity)
            }

            // In-App Update: näytä snackbar aina kun ladattu päivitys havaitaan.
            val downloadedUpdatePromptId by
                inAppUpdateManager.downloadedUpdatePromptId.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            var lastShownDownloadedUpdatePromptId by rememberSaveable { mutableLongStateOf(0L) }
            val updateMessage = stringResource(R.string.update_downloaded)
            val restartLabel = stringResource(R.string.restart)
            LaunchedEffect(downloadedUpdatePromptId) {
                if (downloadedUpdatePromptId > lastShownDownloadedUpdatePromptId) {
                    lastShownDownloadedUpdatePromptId = downloadedUpdatePromptId
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

    private fun checkForInAppUpdate() {
        inAppUpdateManager.checkForUpdate(
            resultLauncher = updateResultLauncher,
            canStartUpdateFlow = { !isFinishing && !isDestroyed },
        )
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
        lifecycleScope.launch {
            preferencesManager.syncAppLanguageFromSystem()
        }
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
        val isOAuthCallback = handleOAuthCallbackIfNeeded(intent)
        counterLaunchRequest =
            if (isOAuthCallback) {
                null
            } else {
                intent.toCounterLaunchRequest(consumedRequestId = null)
            }
    }

    private fun handleOAuthCallbackIfNeeded(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        if (!ravelryAuthManager.isOAuthCallback(uri)) return false
        clearCounterLaunchIntent()
        lifecycleScope.launch {
            val handled = ravelryAuthManager.handleCallback(httpClient, uri)
            if (handled) {
                clearOAuthCallbackIntent(uri)
            }
        }
        return true
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
        val isOAuthCallback = data?.let(ravelryAuthManager::isOAuthCallback) == true
        val launchId = getStringExtra(EXTRA_COUNTER_LAUNCH_ID)
        return CounterLaunchRequest.fromIntentData(
            intentData =
                CounterLaunchIntentData(
                    shouldOpenCounter = getBooleanExtra(EXTRA_OPEN_COUNTER, false),
                    projectId = getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L },
                    launchId = launchId,
                    isTrustedCounterLaunch =
                        CounterLaunchTokenStore.isKnownLaunchId(this@MainActivity, launchId),
                    isOAuthCallback = isOAuthCallback,
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
                putExtra(EXTRA_COUNTER_LAUNCH_ID, CounterLaunchTokenStore.issueLaunchId(context))
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}
