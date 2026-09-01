package com.finnvek.knittools

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.storage.CounterLaunchTokenStore
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.parseWebPatternSharedText
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.InAppUpdateManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.ui.ProvidePreferenceAwareHapticFeedback
import com.finnvek.knittools.ui.navigation.CounterLaunchIntentData
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest
import com.finnvek.knittools.ui.navigation.KnitToolsNavActions
import com.finnvek.knittools.ui.navigation.KnitToolsNavHost
import com.finnvek.knittools.ui.navigation.KnitToolsNavRequests
import com.finnvek.knittools.ui.navigation.PatternShareCoordinatorViewModel
import com.finnvek.knittools.ui.navigation.PatternShareOfferResult
import com.finnvek.knittools.ui.navigation.TopLevelDestination
import com.finnvek.knittools.ui.navigation.toPatternSharePayload
import com.finnvek.knittools.ui.navigation.withValidatedCounterLaunchTrust
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var proManager: ProManager

    @Inject
    lateinit var trialManager: TrialManager

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val patternShareCoordinator: PatternShareCoordinatorViewModel by viewModels()

    private val updateResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            inAppUpdateManager.onUpdateFlowResult(result.resultCode)
            // Flexible mode — lataustulos käsitellään installStateListenerissa
        }

    private val ravelryAuthTabLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            when (result.resultCode) {
                AuthTabIntent.RESULT_OK -> {
                    result.resultUri?.let(::handleRavelryCallbackUri) ?: lifecycleScope.launch {
                        ravelryAuthManager.refreshAuthStatus()
                    }
                }

                else -> {
                    ravelryAuthManager.markBrowserAuthCancelled()
                }
            }
        }

    private var counterLaunchRequest by mutableStateOf<CounterLaunchRequest?>(null)
    private var openProUpgradeRequest by mutableStateOf(false)
    private var openWidgetProPromptRequest by mutableStateOf(false)
    private var consumedCounterLaunchRequestId: String? = null
    private var startupThemeLoaded = false
    private var edgeToEdgeDarkTheme: Boolean? = null
    private var launchRequestJob: Job? = null
    private var launchRequestsReady by mutableStateOf(false)
    private var suppressPassiveTrialNotice by mutableStateOf(true)
    private var showTrialEndedNotice by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !startupThemeLoaded }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            if (!ValueAnimator.areAnimatorsEnabled()) {
                splashScreenView.remove()
                return@setOnExitAnimationListener
            }

            val exitInterpolator = AccelerateInterpolator()
            splashScreenView.iconView
                .animate()
                .scaleX(SPLASH_ICON_EXIT_SCALE)
                .scaleY(SPLASH_ICON_EXIT_SCALE)
                .setDuration(SPLASH_EXIT_DURATION_MILLIS)
                .setInterpolator(exitInterpolator)
                .start()
            splashScreenView.view
                .animate()
                .alpha(0f)
                .setDuration(SPLASH_EXIT_DURATION_MILLIS)
                .setInterpolator(exitInterpolator)
                .withEndAction(splashScreenView::remove)
                .start()
        }
        startLaunchRequestInitialization(savedInstanceState)
        checkForInAppUpdate()
        setContent { MainActivityContent() }
    }

    @Composable
    @Suppress("kotlin:S3776") // Aktivointipyynnöt käsitellään yhdessä activityn juurisisällössä.
    private fun MainActivityContent() {
        val prefs by preferencesManager.preferences.collectAsStateWithLifecycle(initialValue = null)
        val proState by proManager.proState.collectAsStateWithLifecycle()
        val proStateReady by proManager.initialStateReady.collectAsStateWithLifecycle()
        val patternShareImportRequest by patternShareCoordinator.pending.collectAsStateWithLifecycle()
        val isDarkTheme = prefs.resolveStartupDarkTheme(isSystemInDarkTheme()) ?: return

        SideEffect {
            applyEdgeToEdgeIfNeeded(isDarkTheme)
            startupThemeLoaded = true
        }

        val reviewEligible by
            inAppReviewManager.reviewEligibility.collectAsStateWithLifecycle(initialValue = false)
        LaunchedEffect(reviewEligible) {
            if (reviewEligible) {
                inAppReviewManager.maybeRequestReview(this@MainActivity)
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }
        // In-App Update: näytä snackbar aina kun ladattu päivitys havaitaan.
        val downloadedUpdatePromptId by
            inAppUpdateManager.downloadedUpdatePromptId.collectAsStateWithLifecycle()
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

        ProvidePreferenceAwareHapticFeedback(enabled = prefs?.hapticFeedback == true) {
            KnitToolsTheme(isDarkTheme = isDarkTheme) {
                LaunchedEffect(
                    launchRequestsReady,
                    suppressPassiveTrialNotice,
                    proStateReady,
                    proState.status,
                ) {
                    val shouldClaimTrialEndNotice =
                        launchRequestsReady &&
                            !suppressPassiveTrialNotice &&
                            proStateReady &&
                            proState.status == ProStatus.TRIAL_EXPIRED
                    if (shouldClaimTrialEndNotice && trialManager.claimTrialEndNotice()) {
                        showTrialEndedNotice = true
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    KnitToolsNavHost(
                        startDestination = TopLevelDestination.Projects.route,
                        requests =
                            KnitToolsNavRequests(
                                counterLaunch = counterLaunchRequest,
                                openProUpgrade = openProUpgradeRequest,
                                openWidgetProPrompt = openWidgetProPromptRequest,
                                patternShareImport = patternShareImportRequest,
                            ),
                        snackbarHostState = snackbarHostState,
                        actions = createNavActions(),
                    )
                }
                if (showTrialEndedNotice) {
                    TrialEndedDialog(
                        onSeePro = {
                            showTrialEndedNotice = false
                            openProUpgradeRequest = true
                        },
                        onContinueFree = { showTrialEndedNotice = false },
                    )
                }
            }
        }
    }

    private fun createNavActions() =
        KnitToolsNavActions(
            onPurchasePro = billingManager::launchPurchaseFlow,
            onLaunchRavelryAuth = ::launchRavelryAuth,
            onBrowseRavelry = ::launchRavelryBrowse,
            onCounterLaunchHandled = {
                counterLaunchRequest?.let {
                    consumedCounterLaunchRequestId = it.requestId
                }
                counterLaunchRequest = null
                clearCounterLaunchIntent()
            },
            onProUpgradeLaunchHandled = {
                openProUpgradeRequest = false
                clearProUpgradeLaunchIntent()
            },
            onWidgetProPromptLaunchHandled = {
                openWidgetProPromptRequest = false
                clearWidgetProPromptLaunchIntent()
            },
            onPatternShareImportHandled = patternShareCoordinator::acknowledge,
        )

    private fun startLaunchRequestInitialization(savedInstanceState: Bundle?) {
        launchRequestJob?.cancel()
        launchRequestJob =
            lifecycleScope.launch {
                initializeLaunchRequests(savedInstanceState)
            }
    }

    private suspend fun initializeLaunchRequests(savedInstanceState: Bundle?) {
        launchRequestsReady = false
        restoreCounterLaunchRequest(savedInstanceState)
        openProUpgradeRequest = intent?.action == ACTION_OPEN_PRO_UPGRADE
        openWidgetProPromptRequest = intent?.action == ACTION_OPEN_WIDGET_PRO_PROMPT
        val isOAuthCallback = handleOAuthCallbackIfNeeded(intent)
        val isShareImport = !isOAuthCallback && handlePatternShareIntentIfNeeded(intent)
        if (isOAuthCallback || isShareImport) {
            counterLaunchRequest = null
        }
        suppressPassiveTrialNotice =
            openProUpgradeRequest ||
            openWidgetProPromptRequest ||
            isOAuthCallback ||
            isShareImport ||
            counterLaunchRequest != null
        launchRequestsReady = true
    }

    private fun checkForInAppUpdate() {
        inAppUpdateManager.checkForUpdate(
            resultLauncher = updateResultLauncher,
            canStartUpdateFlow = { !isFinishing && !isDestroyed },
        )
    }

    private suspend fun restoreCounterLaunchRequest(savedInstanceState: Bundle?) {
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

    fun launchRavelryBrowse() {
        CustomTabsIntent
            .Builder()
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()
            .launchUrl(this, RAVELRY_PATTERN_SEARCH_URL.toUri())
    }

    fun launchRavelryAuth(uri: Uri) {
        try {
            AuthTabIntent
                .Builder()
                .build()
                .launch(ravelryAuthTabLauncher, uri, RavelryAuthManager.REDIRECT_SCHEME)
        } catch (_: ActivityNotFoundException) {
            CustomTabsIntent
                .Builder()
                .build()
                .launchUrl(this, uri)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequestsReady = false
        openProUpgradeRequest = intent.action == ACTION_OPEN_PRO_UPGRADE
        openWidgetProPromptRequest = intent.action == ACTION_OPEN_WIDGET_PRO_PROMPT
        val isOAuthCallback = handleOAuthCallbackIfNeeded(intent)
        val isShareImport = !isOAuthCallback && handlePatternShareIntentIfNeeded(intent)
        launchRequestJob?.cancel()
        launchRequestJob =
            lifecycleScope.launch {
                counterLaunchRequest =
                    if (isOAuthCallback || isShareImport) {
                        null
                    } else {
                        intent.toCounterLaunchRequest(consumedRequestId = null)
                    }
                suppressPassiveTrialNotice =
                    openProUpgradeRequest ||
                    openWidgetProPromptRequest ||
                    isOAuthCallback ||
                    isShareImport ||
                    counterLaunchRequest != null
                launchRequestsReady = true
            }
    }

    private fun handleOAuthCallbackIfNeeded(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        if (!ravelryAuthManager.isOAuthCallback(uri)) return false
        handleRavelryCallbackUri(uri)
        return true
    }

    private fun handleRavelryCallbackUri(uri: Uri) {
        clearCounterLaunchIntent()
        lifecycleScope.launch {
            val handled = ravelryAuthManager.handleCallback(uri)
            if (handled) {
                clearOAuthCallbackIntent(uri)
            }
        }
    }

    private fun clearOAuthCallbackIntent(uri: Uri) {
        if (intent?.data == uri) {
            intent.data = null
        }
    }

    private fun handlePatternShareIntentIfNeeded(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_SEND) return false
        if (intent.type != MIME_TYPE_TEXT_PLAIN) return false

        val payload =
            parseWebPatternSharedText(
                text = intent.getStringExtra(Intent.EXTRA_TEXT),
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
            ).toPatternSharePayload()
        when (patternShareCoordinator.offer(payload)) {
            is PatternShareOfferResult.Accepted,
            is PatternShareOfferResult.Queued,
            -> clearPatternShareIntent(intent)

            PatternShareOfferResult.Busy -> Unit
        }
        return true
    }

    private fun clearCounterLaunchIntent() {
        intent?.removeExtra(EXTRA_OPEN_COUNTER)
        intent?.removeExtra(EXTRA_PROJECT_ID)
        intent?.removeExtra(EXTRA_COUNTER_LAUNCH_ID)
    }

    private fun clearProUpgradeLaunchIntent() {
        intent
            ?.takeIf { it.action == ACTION_OPEN_PRO_UPGRADE }
            ?.setAction(Intent.ACTION_MAIN)
    }

    private fun clearWidgetProPromptLaunchIntent() {
        intent
            ?.takeIf { it.action == ACTION_OPEN_WIDGET_PRO_PROMPT }
            ?.setAction(Intent.ACTION_MAIN)
    }

    private fun clearPatternShareIntent(sourceIntent: Intent? = intent) {
        sourceIntent
            ?.takeIf { it.action == Intent.ACTION_SEND }
            ?.apply {
                setAction(Intent.ACTION_MAIN)
                setType(null)
                removeExtra(Intent.EXTRA_TEXT)
                removeExtra(Intent.EXTRA_SUBJECT)
            }
    }

    private suspend fun Intent?.toCounterLaunchRequest(consumedRequestId: String?): CounterLaunchRequest? {
        if (this == null) return null
        val isOAuthCallback = data?.let(ravelryAuthManager::isOAuthCallback) == true
        val shouldOpenCounter = getBooleanExtra(EXTRA_OPEN_COUNTER, false)
        val launchId = getStringExtra(EXTRA_COUNTER_LAUNCH_ID)
        val intentData =
            withContext(ioDispatcher) {
                CounterLaunchIntentData(
                    shouldOpenCounter = shouldOpenCounter,
                    projectId = getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L },
                    launchId = launchId,
                    isOAuthCallback = isOAuthCallback,
                ).withValidatedCounterLaunchTrust { candidateLaunchId ->
                    CounterLaunchTokenStore.consumeLaunchId(this@MainActivity, candidateLaunchId)
                }
            }
        return CounterLaunchRequest.fromIntentData(
            intentData = intentData,
            consumedRequestId = consumedRequestId,
        )
    }

    companion object {
        private const val EXTRA_OPEN_COUNTER = "com.finnvek.knittools.extra.OPEN_COUNTER"
        private const val EXTRA_PROJECT_ID = "com.finnvek.knittools.extra.PROJECT_ID"
        private const val EXTRA_COUNTER_LAUNCH_ID = "com.finnvek.knittools.extra.COUNTER_LAUNCH_ID"
        private const val ACTION_OPEN_PRO_UPGRADE = "com.finnvek.knittools.action.OPEN_PRO_UPGRADE"
        private const val ACTION_OPEN_WIDGET_PRO_PROMPT = "com.finnvek.knittools.action.OPEN_WIDGET_PRO_PROMPT"
        private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
        private const val RAVELRY_PATTERN_SEARCH_URL = "https://www.ravelry.com/patterns/search"
        private const val SPLASH_EXIT_DURATION_MILLIS = 180L
        private const val SPLASH_ICON_EXIT_SCALE = 0.94f
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

        fun createProUpgradeLaunchIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_PRO_UPGRADE
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

        fun createWidgetProPromptLaunchIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_WIDGET_PRO_PROMPT
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}

@Composable
private fun TrialEndedDialog(
    onSeePro: () -> Unit,
    onContinueFree: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinueFree,
        title = { Text(stringResource(R.string.pro_trial_ended_title)) },
        text = { Text(stringResource(R.string.pro_trial_ended_body)) },
        confirmButton = {
            Button(onClick = onSeePro) {
                Text(stringResource(R.string.pro_prompt_see_pro))
            }
        },
        dismissButton = {
            TextButton(onClick = onContinueFree) {
                Text(stringResource(R.string.pro_continue_free))
            }
        },
    )
}
