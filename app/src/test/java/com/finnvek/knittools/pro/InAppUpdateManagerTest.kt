package com.finnvek.knittools.pro

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import app.cash.turbine.test
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InAppUpdateManagerTest {
    private val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
    private val resultLauncher = mockk<ActivityResultLauncher<IntentSenderRequest>>(relaxed = true)

    @Test
    fun `checkForUpdate does not start update flow after activity becomes unavailable`() {
        val updateInfoTask = pendingTask<AppUpdateInfo>()
        val updateInfo = flexibleUpdateInfo()
        every { appUpdateManager.appUpdateInfo } returns updateInfoTask.task
        val manager = InAppUpdateManager(appUpdateManager)
        var canStartUpdateFlow = true

        manager.checkForUpdate(
            resultLauncher = resultLauncher,
            canStartUpdateFlow = { canStartUpdateFlow },
        )
        canStartUpdateFlow = false
        updateInfoTask.succeed(updateInfo)

        verify(exactly = 0) {
            appUpdateManager.startUpdateFlowForResult(
                any(),
                any<ActivityResultLauncher<IntentSenderRequest>>(),
                any<AppUpdateOptions>(),
            )
        }
    }

    @Test
    fun `checkForUpdate clears in-progress state when update flow does not start`() {
        val updateInfo = flexibleUpdateInfo()
        every { appUpdateManager.appUpdateInfo } returns successTask(updateInfo)
        every {
            appUpdateManager.startUpdateFlowForResult(
                updateInfo,
                resultLauncher,
                any<AppUpdateOptions>(),
            )
        } returns false
        val manager = InAppUpdateManager(appUpdateManager)

        manager.checkForUpdate(resultLauncher)
        manager.checkForUpdate(resultLauncher)

        verify(exactly = 2) {
            appUpdateManager.startUpdateFlowForResult(
                updateInfo,
                resultLauncher,
                any<AppUpdateOptions>(),
            )
        }
    }

    @Test
    fun `checkForUpdate clears in-progress state when launcher is unavailable`() {
        val updateInfo = flexibleUpdateInfo()
        val failure = IllegalStateException("launcher destroyed")
        every { appUpdateManager.appUpdateInfo } returns successTask(updateInfo)
        every {
            appUpdateManager.startUpdateFlowForResult(
                updateInfo,
                resultLauncher,
                any<AppUpdateOptions>(),
            )
        } throws failure
        val manager = InAppUpdateManager(appUpdateManager)

        manager.checkForUpdate(resultLauncher)
        manager.checkForUpdate(resultLauncher)

        verify(exactly = 2) {
            appUpdateManager.startUpdateFlowForResult(
                updateInfo,
                resultLauncher,
                any<AppUpdateOptions>(),
            )
        }
    }

    @Test
    fun `downloaded prompt is emitted only once while same update remains downloaded`() =
        runTest {
            val updateInfo =
                mockk<AppUpdateInfo> {
                    every { installStatus() } returns InstallStatus.DOWNLOADED
                }
            every { appUpdateManager.appUpdateInfo } returns successTask(updateInfo)
            val manager = InAppUpdateManager(appUpdateManager)

            manager.downloadedUpdatePromptId.test {
                awaitItem()

                manager.checkDownloadedOnResume()
                assertEquals(1L, awaitItem())

                manager.checkDownloadedOnResume()
                expectNoEvents()
            }
        }

    @Test
    fun `completeUpdate failure re-emits downloaded prompt`() =
        runTest {
            val failure = IllegalStateException("install failed")
            every { appUpdateManager.completeUpdate() } returns failureTask(failure)
            val manager = InAppUpdateManager(appUpdateManager)

            manager.downloadedUpdatePromptId.test {
                awaitItem()

                manager.completeUpdate()
                assertEquals(1L, awaitItem())
            }
        }

    private fun flexibleUpdateInfo(): AppUpdateInfo =
        mockk {
            every { updateAvailability() } returns UpdateAvailability.UPDATE_AVAILABLE
            every { isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns true
        }

    private fun <T> successTask(value: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<T>>().onSuccess(value)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        return task
    }

    private fun <T> failureTask(error: Exception): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        return task
    }

    private fun <T> pendingTask(): PendingTask<T> = PendingTask()

    private class PendingTask<T> {
        val successListeners = mutableListOf<OnSuccessListener<T>>()
        val failureListeners = mutableListOf<OnFailureListener>()
        val task: Task<T> = mockk()

        init {
            every { task.addOnSuccessListener(any()) } answers {
                successListeners += firstArg<OnSuccessListener<T>>()
                task
            }
            every { task.addOnFailureListener(any()) } answers {
                failureListeners += firstArg<OnFailureListener>()
                task
            }
        }

        fun succeed(value: T) {
            successListeners.forEach { it.onSuccess(value) }
        }
    }
}
