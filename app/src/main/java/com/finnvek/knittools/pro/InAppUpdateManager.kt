package com.finnvek.knittools.pro

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
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
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val appUpdateManager = AppUpdateManagerFactory.create(context)

        private val _updateDownloaded = MutableStateFlow(false)
        val updateDownloaded: StateFlow<Boolean> = _updateDownloaded.asStateFlow()

        private val installStateListener =
            InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    _updateDownloaded.value = true
                }
            }

        /**
         * Tarkistaa päivityksen saatavuuden ja aloittaa flexible-latauksen taustalla.
         * Kutsutaan Activityn onCreatessa.
         */
        fun checkForUpdate(resultLauncher: ActivityResultLauncher<IntentSenderRequest>) {
            appUpdateManager.registerListener(installStateListener)
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        resultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    )
                }
            }
        }

        /**
         * Tarkistaa onko päivitys jo ladattu mutta asentamatta (esim. jos sovellus palautettiin).
         * Kutsutaan Activityn onResumessa.
         */
        fun checkDownloadedOnResume() {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _updateDownloaded.value = true
                }
            }
        }

        /** Käynnistää sovelluksen uudelleen asentaakseen ladatun päivityksen. */
        fun completeUpdate() {
            appUpdateManager.completeUpdate()
        }

        /** Poistaa kuuntelijan. Kutsutaan Activityn onDestroyssa. */
        fun cleanup() {
            appUpdateManager.unregisterListener(installStateListener)
        }
    }
