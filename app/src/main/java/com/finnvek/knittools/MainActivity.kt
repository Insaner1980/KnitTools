package com.finnvek.knittools

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.finnvek.knittools.analytics.PostHogAnalytics
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
import com.finnvek.knittools.ui.SplashLogoDrawable
import com.finnvek.knittools.ui.navigation.CounterLaunchIntentData
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest
import com.finnvek.knittools.ui.navigation.IncomingPdfHost
import com.finnvek.knittools.ui.navigation.IncomingPdfViewModel
import com.finnvek.knittools.ui.navigation.KnitToolsNavActions
import com.finnvek.knittools.ui.navigation.KnitToolsNavHost
import com.finnvek.knittools.ui.navigation.KnitToolsNavRequests
import com.finnvek.knittools.ui.navigation.PatternShareCoordinatorViewModel
import com.finnvek.knittools.ui.navigation.PatternShareOfferResult
import com.finnvek.knittools.ui.navigation.TopLevelDestination
import com.finnvek.knittools.ui.navigation.receiveFromIntent
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
    private val incomingPdfViewModel: IncomingPdfViewModel by viewModels(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                this[DEFAULT_ARGS_KEY] = Bundle()
            }
        },
    )

    private val updateResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            inAppUpdateManager.onUpdateFlowResult(result.resultCode)
            // Flexible mode — lataustulos käsitellään installStateListenerissa
        }

    private var counterLaunchRequest by mutableStateOf<CounterLaunchRequest?>(null)
    private var openProUpgradeRequest by mutableStateOf(false)
    private var openWidgetProPromptRequest by mutableStateOf(false)
    private var consumedCounterLaunchRequestId: String? = null
    private var consumedPatternShareIntent = false
    private var startupThemeLoaded = false
    private var splashExitAnimator: ValueAnimator? = null
    private var edgeToEdgeDarkTheme: Boolean? = null
    private var launchRequestJob: Job? = null
    private var launchRequestsReady by mutableStateOf(false)
    private var suppressPassiveTrialNotice by mutableStateOf(true)
    private var showTrialEndedNotice by mutableStateOf(false)

    @Inject
    lateinit var analytics: PostHogAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !startupThemeLoaded }
        splashScreen.setOnExitAnimationListener(::animateSplashExit)
        startLaunchRequestInitialization(savedInstanceState)
        checkForInAppUpdate()
        setContent { MainActivityContent() }
    }

    private fun animateSplashExit(splash: SplashScreenViewProvider) {
        if (!ValueAnimator.areAnimatorsEnabled()) {
            splash.remove()
            return
        }

        val icon = splash.iconView
        (icon as? ImageView)?.setImageDrawable(null)
        val logo = SplashLogoDrawable(BitmapFactory.decodeResource(resources, R.drawable.splash_logo))
        val insetX = (icon.width * SPLASH_LOGO_INSET).toInt()
        val insetY = (icon.height * SPLASH_LOGO_INSET).toInt()
        logo.setBounds(insetX, insetY, icon.width - insetX, icon.height - insetY)
        icon.overlay.add(logo)
        val animator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = SPLASH_EXIT_DURATION_MILLIS
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val elapsed = (it.animatedValue as Float) * SPLASH_EXIT_DURATION_MILLIS
                    logo.elapsedMillis = elapsed
                    splash.view.alpha = 1f - ((elapsed - SPLASH_REVEAL_MILLIS) / SPLASH_FADE_MILLIS).coerceIn(0f, 1f)
                }
                doOnEnd {
                    icon.overlay.remove(logo)
                    splash.remove()
                    splashExitAnimator = null
                }
            }
        splashExitAnimator = animator
        animator.start()
    }

    @Composable
    @Suppress("kotlin:S3776") // Aktivointipyynnöt käsitellään yhdessä activityn juurisisällössä.
    private fun MainActivityContent() {
        val storedAppLanguageApplied by
            preferencesManager.storedAppLanguageApplied.collectAsStateWithLifecycle()
        val prefs by preferencesManager.preferences.collectAsStateWithLifecycle(initialValue = null)
        val proState by proManager.proState.collectAsStateWithLifecycle()
        val proStateReady by proManager.initialStateReady.collectAsStateWithLifecycle()
        val patternShareImportRequest by patternShareCoordinator.pending.collectAsStateWithLifecycle()
        if (!storedAppLanguageApplied) return
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
        DownloadedUpdatePromptEffect(downloadedUpdatePromptId, snackbarHostState, inAppUpdateManager::completeUpdate)

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
                        onScreenViewed = analytics::screenViewed,
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
                        incomingPdfHost = { onNavigate ->
                            IncomingPdfHost(
                                viewModelProvider = { incomingPdfViewModel },
                                onNavigate = onNavigate,
                            )
                        },
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
            onWebPdfDownload = incomingPdfViewModel::receiveDownload,
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
        consumedPatternShareIntent = savedInstanceState?.getBoolean(STATE_PATTERN_SHARE_INTENT_CONSUMED) == true
        if (consumedPatternShareIntent) clearPatternShareIntent()
        restoreCounterLaunchRequest(savedInstanceState)
        openProUpgradeRequest = intent?.action == ACTION_OPEN_PRO_UPGRADE
        openWidgetProPromptRequest = intent?.action == ACTION_OPEN_WIDGET_PRO_PROMPT
        val isShareImport =
            handlePatternShareIntentIfNeeded(intent) ||
                incomingPdfViewModel.receiveFromIntent(intent, contentResolver)
        if (isShareImport) {
            counterLaunchRequest = null
        }
        suppressPassiveTrialNotice =
            openProUpgradeRequest ||
            openWidgetProPromptRequest ||
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
        outState.putBoolean(STATE_PATTERN_SHARE_INTENT_CONSUMED, consumedPatternShareIntent)
        consumedCounterLaunchRequestId?.let {
            outState.putString(STATE_CONSUMED_COUNTER_LAUNCH_REQUEST_ID, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        splashExitAnimator?.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.cleanup()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumedPatternShareIntent = false
        setIntent(intent)
        launchRequestsReady = false
        openProUpgradeRequest = intent.action == ACTION_OPEN_PRO_UPGRADE
        openWidgetProPromptRequest = intent.action == ACTION_OPEN_WIDGET_PRO_PROMPT
        val isShareImport =
            handlePatternShareIntentIfNeeded(intent) ||
                incomingPdfViewModel.receiveFromIntent(intent, contentResolver)
        launchRequestJob?.cancel()
        launchRequestJob =
            lifecycleScope.launch {
                counterLaunchRequest =
                    if (isShareImport) {
                        null
                    } else {
                        intent.toCounterLaunchRequest(consumedRequestId = null)
                    }
                suppressPassiveTrialNotice =
                    openProUpgradeRequest ||
                    openWidgetProPromptRequest ||
                    isShareImport ||
                    counterLaunchRequest != null
                launchRequestsReady = true
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

            PatternShareOfferResult.Busy -> {
                Toast.makeText(this, R.string.ravelry_import_could_not_import, Toast.LENGTH_SHORT).show()
                clearPatternShareIntent(intent)
            }
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
                consumedPatternShareIntent = true
                setAction(Intent.ACTION_MAIN)
                setType(null)
                removeExtra(Intent.EXTRA_TEXT)
                removeExtra(Intent.EXTRA_SUBJECT)
            }
    }

    private suspend fun Intent?.toCounterLaunchRequest(consumedRequestId: String?): CounterLaunchRequest? {
        if (this == null) return null
        val shouldOpenCounter = getBooleanExtra(EXTRA_OPEN_COUNTER, false)
        val launchId = getStringExtra(EXTRA_COUNTER_LAUNCH_ID)
        val intentData =
            withContext(ioDispatcher) {
                CounterLaunchIntentData(
                    shouldOpenCounter = shouldOpenCounter,
                    projectId = getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L },
                    launchId = launchId,
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
        private const val STATE_PATTERN_SHARE_INTENT_CONSUMED = "pattern_share_intent_consumed"
        private const val EXTRA_OPEN_COUNTER = "com.finnvek.knittools.extra.OPEN_COUNTER"
        private const val EXTRA_PROJECT_ID = "com.finnvek.knittools.extra.PROJECT_ID"
        private const val EXTRA_COUNTER_LAUNCH_ID = "com.finnvek.knittools.extra.COUNTER_LAUNCH_ID"
        private const val ACTION_OPEN_PRO_UPGRADE = "com.finnvek.knittools.action.OPEN_PRO_UPGRADE"
        private const val ACTION_OPEN_WIDGET_PRO_PROMPT = "com.finnvek.knittools.action.OPEN_WIDGET_PRO_PROMPT"
        private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
        private const val SPLASH_EXIT_DURATION_MILLIS = 760L
        private const val SPLASH_REVEAL_MILLIS = 640f
        private const val SPLASH_FADE_MILLIS = 120f
        private const val SPLASH_LOGO_INSET = 0.1f
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
internal fun DownloadedUpdatePromptEffect(
    downloadedUpdatePromptId: Long,
    snackbarHostState: SnackbarHostState,
    onRestart: () -> Unit,
) {
    var lastShownDownloadedUpdatePromptId by remember(snackbarHostState) { mutableLongStateOf(0L) }
    val updateMessage = stringResource(R.string.update_downloaded)
    val restartLabel = stringResource(R.string.restart)
    LaunchedEffect(downloadedUpdatePromptId, snackbarHostState) {
        if (downloadedUpdatePromptId > lastShownDownloadedUpdatePromptId) {
            lastShownDownloadedUpdatePromptId = downloadedUpdatePromptId
            val result =
                snackbarHostState.showSnackbar(
                    message = updateMessage,
                    actionLabel = restartLabel,
                    duration = SnackbarDuration.Indefinite,
                )
            if (result == SnackbarResult.ActionPerformed) {
                onRestart()
            }
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
