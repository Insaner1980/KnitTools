package com.finnvek.knittools

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.InAppUpdateManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest
import com.finnvek.knittools.ui.navigation.KnitToolsNavHost
import com.finnvek.knittools.ui.navigation.TopLevelDestination
import com.finnvek.knittools.ui.screens.settings.SettingsViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    lateinit var httpClient: io.ktor.client.HttpClient

    private val updateResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { /* Flexible mode — tulos käsitelty installStateListenerissa */ }

    private var counterLaunchRequest by mutableStateOf<CounterLaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        counterLaunchRequest = intent.toCounterLaunchRequest()
        handleOAuthCallbackIfNeeded(intent)
        enableEdgeToEdge()
        inAppUpdateManager.checkForUpdate(updateResultLauncher)
        setContent {
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

            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

            val isDarkTheme =
                when (prefs.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }

            KnitToolsTheme(isDarkTheme = isDarkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        KnitToolsNavHost(
                            startDestination = TopLevelDestination.Projects.route,
                            counterLaunchRequest = counterLaunchRequest,
                            onCounterLaunchHandled = { counterLaunchRequest = null },
                        )
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.checkDownloadedOnResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.cleanup()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallbackIfNeeded(intent)
        counterLaunchRequest = intent.toCounterLaunchRequest()
    }

    private fun handleOAuthCallbackIfNeeded(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.finnvek.knittools" && uri.host == "oauth") {
            lifecycleScope.launch {
                ravelryAuthManager.handleCallback(httpClient, uri)
            }
        }
    }

    private fun Intent?.toCounterLaunchRequest(): CounterLaunchRequest? {
        if (this?.getBooleanExtra(EXTRA_OPEN_COUNTER, false) != true) return null
        return CounterLaunchRequest(
            projectId = getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L },
        )
    }

    companion object {
        private const val EXTRA_OPEN_COUNTER = "com.finnvek.knittools.extra.OPEN_COUNTER"
        private const val EXTRA_PROJECT_ID = "com.finnvek.knittools.extra.PROJECT_ID"

        fun createCounterLaunchIntent(
            context: Context,
            projectId: Long?,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_COUNTER, true)
                projectId?.let { putExtra(EXTRA_PROJECT_ID, it) }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}
