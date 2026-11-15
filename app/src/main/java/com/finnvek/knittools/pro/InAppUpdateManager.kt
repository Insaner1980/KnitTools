package com.finnvek.knittools.pro

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppUpdateManager
    internal constructor(
        private val appUpdateManager: AppUpdateManager,
    ) {
        @Inject
        constructor(
            @ApplicationContext context: Context,
        ) : this(AppUpdateManagerFactory.create(context))

        private val _downloadedUpdatePromptId = MutableStateFlow(0L)
        val downloadedUpdatePromptId: StateFlow<Long> = _downloadedUpdatePromptId.asStateFlow()

        private var installStateListener: InstallStateUpdatedListener? = null
        private var listenerOwner: Any? = null
        private var updateFlowInProgress = false
        private var completingPromptId = 0L
        private var lastPromptId = 0L
        private var resumeCheck: Any? = null
        private val pendingChecks = mutableMapOf<Any, (AppUpdateInfo) -> Unit>()

        /**
         * Tarkistaa päivityksen saatavuuden ja aloittaa flexible-latauksen taustalla.
         * Kutsutaan Activityn onCreatessa.
         */
        fun checkForUpdate(
            resultLauncher: ActivityResultLauncher<IntentSenderRequest>,
            canStartUpdateFlow: () -> Boolean = { true },
        ) {
            registerListenerIfNeeded()
            if (updateFlowInProgress) return

            val request = Any()
            pendingChecks[request] = { info ->
                if (!updateFlowInProgress &&
                    completingPromptId == 0L &&
                    _downloadedUpdatePromptId.value == 0L &&
                    info.isFlexibleUpdateAvailable()
                ) {
                    startFlexibleUpdate(info, resultLauncher, canStartUpdateFlow)
                }
            }
            appUpdateManager.appUpdateInfo
                .addOnCompleteListener { task ->
                    val deliver = pendingChecks.remove(request)
                    if (deliver != null && task.isSuccessful) deliver(task.result)
                }
        }

        /**
         * Tarkistaa onko päivitys jo ladattu mutta asentamatta (esim. jos sovellus palautettiin).
         * Kutsutaan Activityn onResumessa.
         */
        fun checkDownloadedOnResume() {
            val request = Any()
            resumeCheck = request
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (resumeCheck !== request) return@addOnSuccessListener
                    resumeCheck = null
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        notifyUpdateDownloaded()
                    } else {
                        _downloadedUpdatePromptId.value = 0L
                    }
                }
        }

        fun onUpdateFlowResult(
            @Suppress("UNUSED_PARAMETER") resultCode: Int,
        ) {
            updateFlowInProgress = false
        }

        /** Käynnistää sovelluksen uudelleen asentaakseen ladatun päivityksen. */
        fun completeUpdate(promptId: Long) {
            if (promptId == 0L || _downloadedUpdatePromptId.value != promptId || completingPromptId != 0L) return
            completingPromptId = promptId
            resumeCheck = null
            _downloadedUpdatePromptId.value = 0L
            appUpdateManager
                .completeUpdate()
                .addOnFailureListener {
                    if (completingPromptId == promptId) {
                        completingPromptId = 0L
                        notifyUpdateDownloaded()
                    }
                }
        }

        /** Poistaa kuuntelijan. Kutsutaan Activityn onDestroyssa. */
        fun cleanup(finishing: Boolean = false) {
            pendingChecks.clear()
            resumeCheck = null
            listenerOwner = null
            installStateListener?.let(appUpdateManager::unregisterListener)
            installStateListener = null
            if (finishing) updateFlowInProgress = false
        }

        private fun startFlexibleUpdate(
            info: AppUpdateInfo,
            resultLauncher: ActivityResultLauncher<IntentSenderRequest>,
            canStartUpdateFlow: () -> Boolean,
        ) {
            if (!canStartUpdateFlow()) {
                return
            }
            updateFlowInProgress = true
            val started =
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        resultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    )
                } catch (_: IllegalStateException) {
                    updateFlowInProgress = false
                    return
                }
            if (started) {
                pendingChecks.clear()
            } else {
                updateFlowInProgress = false
            }
        }

        private fun AppUpdateInfo.isFlexibleUpdateAvailable(): Boolean =
            updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

        private fun notifyUpdateDownloaded() {
            updateFlowInProgress = false
            if (completingPromptId != 0L || _downloadedUpdatePromptId.value != 0L) return
            _downloadedUpdatePromptId.value = ++lastPromptId
        }

        private fun registerListenerIfNeeded() {
            if (installStateListener != null) return
            val owner = Any()
            listenerOwner = owner
            val listener =
                InstallStateUpdatedListener { state ->
                    if (listenerOwner !== owner) return@InstallStateUpdatedListener
                    val status = state.installStatus()
                    if (status == InstallStatus.DOWNLOADED) {
                        resumeCheck = null
                        notifyUpdateDownloaded()
                    } else if (status == InstallStatus.CANCELED ||
                        status == InstallStatus.FAILED ||
                        status == InstallStatus.INSTALLED
                    ) {
                        resumeCheck = null
                        updateFlowInProgress = false
                        completingPromptId = 0L
                        _downloadedUpdatePromptId.value = 0L
                    }
                }
            installStateListener = listener
            appUpdateManager.registerListener(listener)
        }
    }
