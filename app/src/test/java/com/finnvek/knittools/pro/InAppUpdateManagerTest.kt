package com.finnvek.knittools.pro

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import app.cash.turbine.test
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppUpdateManagerTest {
    private val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
    private val resultLauncher = mockk<ActivityResultLauncher<IntentSenderRequest>>(relaxed = true)

    @Test
    fun `install listener preserves downloaded prompts until a terminal status`() {
        val listener = slot<InstallStateUpdatedListener>()
        every { appUpdateManager.registerListener(capture(listener)) } returns Unit
        every { appUpdateManager.appUpdateInfo } returns pendingTask<AppUpdateInfo>().task
        val manager = InAppUpdateManager(appUpdateManager)
        manager.checkForUpdate(resultLauncher)

        listOf(InstallStatus.CANCELED, InstallStatus.FAILED, InstallStatus.INSTALLED).forEach { terminalStatus ->
            listener.captured.onStateUpdate(mockk { every { installStatus() } returns InstallStatus.DOWNLOADED })
            val promptId = manager.downloadedUpdatePromptId.value
            assertTrue(promptId > 0L)
            listOf(InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING, InstallStatus.UNKNOWN)
                .forEach { status ->
                    listener.captured.onStateUpdate(mockk { every { installStatus() } returns status })
                    assertEquals(promptId, manager.downloadedUpdatePromptId.value)
                }
            listener.captured.onStateUpdate(mockk { every { installStatus() } returns terminalStatus })
            assertEquals(0L, manager.downloadedUpdatePromptId.value)
        }
    }

    @Test
    fun `a concurrent availability failure cannot release an already started update flow`() {
        val first = pendingTask<AppUpdateInfo>()
        val second = pendingTask<AppUpdateInfo>()
        val results = listOf(first.task, second.task, successTask(flexibleUpdateInfo()))
        every { appUpdateManager.appUpdateInfo } returnsMany results
        every {
            appUpdateManager.startUpdateFlowForResult(any(), resultLauncher, any<AppUpdateOptions>())
        } returns true
        val manager = InAppUpdateManager(appUpdateManager)

        manager.checkForUpdate(resultLauncher)
        manager.checkForUpdate(resultLauncher)
        first.succeed(flexibleUpdateInfo())
        second.fail(IllegalStateException("availability failed"))
        manager.checkForUpdate(resultLauncher)

        verify(exactly = 1) {
            appUpdateManager.startUpdateFlowForResult(any(), resultLauncher, any<AppUpdateOptions>())
        }
    }

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
    fun `activity cleanup discards its pending availability callback while replacement can start`() {
        val oldRequest = pendingTask<AppUpdateInfo>()
        val newRequest = pendingTask<AppUpdateInfo>()
        val newLauncher = mockk<ActivityResultLauncher<IntentSenderRequest>>()
        val oldGuard = mockk<() -> Boolean>()
        every { oldGuard() } returns true
        every { appUpdateManager.appUpdateInfo } returnsMany listOf(oldRequest.task, newRequest.task)
        every {
            appUpdateManager.startUpdateFlowForResult(any(), newLauncher, any<AppUpdateOptions>())
        } returns true
        val manager = InAppUpdateManager(appUpdateManager)

        manager.checkForUpdate(resultLauncher, oldGuard)
        manager.cleanup()
        manager.checkForUpdate(newLauncher)
        oldRequest.succeed(flexibleUpdateInfo())
        newRequest.succeed(flexibleUpdateInfo())

        verify(exactly = 0) { oldGuard() }
        verify(exactly = 0) {
            appUpdateManager.startUpdateFlowForResult(any(), resultLauncher, any<AppUpdateOptions>())
        }
        verify(exactly = 1) {
            appUpdateManager.startUpdateFlowForResult(any(), newLauncher, any<AppUpdateOptions>())
        }
        verify(exactly = 2) { appUpdateManager.registerListener(any()) }
        verify(exactly = 1) { appUpdateManager.unregisterListener(any()) }
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
    fun `obsolete resume result cannot publish a restart prompt`() {
        val obsolete = pendingTask<AppUpdateInfo>()
        val current = pendingTask<AppUpdateInfo>()
        every { appUpdateManager.appUpdateInfo } returnsMany listOf(obsolete.task, current.task)
        val manager = InAppUpdateManager(appUpdateManager)

        manager.checkDownloadedOnResume()
        manager.cleanup()
        manager.checkDownloadedOnResume()
        obsolete.succeed(mockk { every { installStatus() } returns InstallStatus.DOWNLOADED })

        assertEquals(0L, manager.downloadedUpdatePromptId.value)
        current.succeed(mockk { every { installStatus() } returns InstallStatus.DOWNLOADED })
        assertEquals(1L, manager.downloadedUpdatePromptId.value)
    }

    @Test
    fun `restart requests at most one completion while installation is pending`() {
        val info = mockk<AppUpdateInfo> { every { installStatus() } returns InstallStatus.DOWNLOADED }
        every { appUpdateManager.appUpdateInfo } returns successTask(info)
        every { appUpdateManager.completeUpdate() } returns pendingTask<Void>().task
        val manager = InAppUpdateManager(appUpdateManager)

        manager.checkDownloadedOnResume()
        manager.completeUpdate(1L)
        manager.checkDownloadedOnResume()
        manager.completeUpdate(1L)

        verify(exactly = 1) { appUpdateManager.completeUpdate() }
    }

    @Test
    fun `completeUpdate failure re-emits downloaded prompt`() =
        runTest {
            val install = pendingTask<Void>()
            val info = mockk<AppUpdateInfo> { every { installStatus() } returns InstallStatus.DOWNLOADED }
            every { appUpdateManager.appUpdateInfo } returns successTask(info)
            every { appUpdateManager.completeUpdate() } returns install.task
            val manager = InAppUpdateManager(appUpdateManager)

            manager.downloadedUpdatePromptId.test {
                awaitItem()

                manager.checkDownloadedOnResume()
                assertEquals(1L, awaitItem())
                manager.completeUpdate(1L)
                assertEquals(0L, awaitItem())
                install.fail(IllegalStateException("install failed"))
                assertEquals(2L, awaitItem())
            }
        }

    @Test
    fun `downloaded state survives activity cleanup and failed installation remains retryable`() {
        val updateInfo = mockk<AppUpdateInfo> { every { installStatus() } returns InstallStatus.DOWNLOADED }
        val install = pendingTask<Void>()
        every { appUpdateManager.appUpdateInfo } returns successTask(updateInfo)
        every { appUpdateManager.completeUpdate() } returns install.task
        val manager = InAppUpdateManager(appUpdateManager)

        manager.checkDownloadedOnResume()
        assertEquals(1L, manager.downloadedUpdatePromptId.value)
        manager.cleanup()
        manager.checkDownloadedOnResume()
        assertEquals(1L, manager.downloadedUpdatePromptId.value)

        manager.completeUpdate(1L)
        install.fail(IllegalStateException("install failed"))
        assertEquals(2L, manager.downloadedUpdatePromptId.value)
        manager.cleanup()
        manager.checkDownloadedOnResume()
        assertEquals(2L, manager.downloadedUpdatePromptId.value)

        every { appUpdateManager.completeUpdate() } returns mockk(relaxed = true)
        manager.completeUpdate(1L)
        manager.completeUpdate(2L)
        verify(exactly = 2) { appUpdateManager.completeUpdate() }
    }

    private fun flexibleUpdateInfo(): AppUpdateInfo =
        mockk {
            every { updateAvailability() } returns UpdateAvailability.UPDATE_AVAILABLE
            every { isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns true
        }

    private fun <T> successTask(value: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isSuccessful } returns true
        every { task.result } returns value
        every { task.addOnCompleteListener(any()) } answers {
            firstArg<OnCompleteListener<T>>().onComplete(task)
            task
        }
        every { task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<T>>().onSuccess(value)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        return task
    }

    private fun <T> pendingTask(): PendingTask<T> = PendingTask()

    private class PendingTask<T> {
        val completionListeners = mutableListOf<OnCompleteListener<T>>()
        val successListeners = mutableListOf<OnSuccessListener<T>>()
        val failureListeners = mutableListOf<OnFailureListener>()
        val task: Task<T> = mockk()

        init {
            every { task.addOnCompleteListener(any()) } answers {
                completionListeners += firstArg<OnCompleteListener<T>>()
                task
            }
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
            every { task.isSuccessful } returns true
            every { task.result } returns value
            completionListeners.forEach { it.onComplete(task) }
            successListeners.forEach { it.onSuccess(value) }
        }

        fun fail(error: Exception) {
            every { task.isSuccessful } returns false
            completionListeners.forEach { it.onComplete(task) }
            failureListeners.forEach { it.onFailure(error) }
        }
    }
}
