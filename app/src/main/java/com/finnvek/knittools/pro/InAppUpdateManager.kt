package com.finnvek.knittools.pro

import android.app.Activity
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.play.core.install.model.ActivityResult as PlayUpdateActivityResult

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

        private var listenerRegistered = false
        private var updateFlowInProgress = false
        private var updateDownloadedNotified = false

        private val installStateListener =
            InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    notifyUpdateDownloaded()
                }
            }

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

            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (updateFlowInProgress) return@addOnSuccessListener
                    if (info.isFlexibleUpdateAvailable()) {
                        startFlexibleUpdate(
                            info = info,
                            resultLauncher = resultLauncher,
                            canStartUpdateFlow = canStartUpdateFlow,
                        )
                    }
                }.addOnFailureListener { error ->
                    Log.w(TAG, "Päivitystietojen haku epäonnistui", error)
                    updateFlowInProgress = false
                }
        }

        /**
         * Tarkistaa onko päivitys jo ladattu mutta asentamatta (esim. jos sovellus palautettiin).
         * Kutsutaan Activityn onResumessa.
         */
        fun checkDownloadedOnResume() {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        notifyUpdateDownloaded()
                    }
                }.addOnFailureListener { error ->
                    Log.w(TAG, "Ladatun päivityksen tarkistus epäonnistui", error)
                }
        }

        fun onUpdateFlowResult(resultCode: Int) {
            updateFlowInProgress = false
            when (resultCode) {
                Activity.RESULT_OK -> Unit
                Activity.RESULT_CANCELED -> Log.i(TAG, "Käyttäjä perui sovelluspäivityksen")
                PlayUpdateActivityResult.RESULT_IN_APP_UPDATE_FAILED ->
                    Log.w(TAG, "Sovelluspäivitys epäonnistui Play UI:ssa")
                else -> Log.w(TAG, "Tuntematon sovelluspäivityksen tuloskoodi: $resultCode")
            }
        }

        /** Käynnistää sovelluksen uudelleen asentaakseen ladatun päivityksen. */
        fun completeUpdate() {
            updateDownloadedNotified = false
            appUpdateManager
                .completeUpdate()
                .addOnFailureListener { error ->
                    Log.w(TAG, "Päivityksen viimeistely epäonnistui", error)
                    notifyUpdateDownloaded()
                }
        }

        /** Poistaa kuuntelijan. Kutsutaan Activityn onDestroyssa. */
        fun cleanup() {
            if (listenerRegistered) {
                appUpdateManager.unregisterListener(installStateListener)
                listenerRegistered = false
            }
            updateFlowInProgress = false
        }

        private fun startFlexibleUpdate(
            info: AppUpdateInfo,
            resultLauncher: ActivityResultLauncher<IntentSenderRequest>,
            canStartUpdateFlow: () -> Boolean,
        ) {
            if (!canStartUpdateFlow()) {
                Log.w(TAG, "Päivitysflow'ta ei käynnistetty, koska Activity ei ole enää käytettävissä")
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
                } catch (error: IllegalStateException) {
                    updateFlowInProgress = false
                    Log.w(TAG, "Päivitysflow'n käynnistys epäonnistui", error)
                    return
                }
            if (!started) {
                updateFlowInProgress = false
                Log.w(TAG, "Päivitysflow'n käynnistys palautti false")
            }
        }

        private fun AppUpdateInfo.isFlexibleUpdateAvailable(): Boolean =
            updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

        private fun notifyUpdateDownloaded() {
            updateFlowInProgress = false
            if (updateDownloadedNotified) return
            updateDownloadedNotified = true
            _downloadedUpdatePromptId.update { it + 1L }
        }

        private fun registerListenerIfNeeded() {
            if (!listenerRegistered) {
                appUpdateManager.registerListener(installStateListener)
                listenerRegistered = true
            }
        }

        private companion object {
            private const val TAG = "InAppUpdateManager"
        }
    }
