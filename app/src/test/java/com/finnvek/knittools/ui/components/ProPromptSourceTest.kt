package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProPromptSourceTest {
    @Test
    fun `trial and purchase entitlement resume the pending action at most once`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/components/ProPromptSheet.kt",
            )

        assertTrue(source.contains("var pendingActionResumed by remember(request)"))
        assertTrue(source.contains("if (!pendingActionResumed)"))
        assertTrue(source.contains("if (proState.isPro) resumePendingAction()"))
        assertTrue(source.contains("TrialStartResult.Started"))
        assertTrue(source.contains("TrialStartResult.AlreadyActive"))
        assertTrue(source.contains("TrialStartResult.AlreadyExpired"))
        assertTrue(source.contains("TrialStartResult.AlreadyTampered"))

        val trialResultHandler =
            source
                .substringAfter("CollectWithLifecycleEffect({ viewModel.trialStartResults })")
                .substringBefore("ModalBottomSheet(")
        val successfulTrialBranch =
            trialResultHandler
                .substringAfter("TrialStartResult.Started")
                .substringBefore("TrialStartResult.AlreadyExpired")
        val unavailableTrialBranch =
            trialResultHandler
                .substringAfter("TrialStartResult.AlreadyExpired")
                .substringBefore("TrialStartResult.Failed")

        assertTrue(successfulTrialBranch.contains("resumePendingAction()"))
        assertFalse(unavailableTrialBranch.contains("resumePendingAction()"))
    }

    @Test
    fun `dismissed prompt cannot replay an old trial result`() {
        val viewModel =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProPromptViewModel.kt",
            )

        assertTrue(viewModel.contains("MutableSharedFlow<TrialStartResult>(extraBufferCapacity = 1)"))
        assertFalse(viewModel.contains("Channel<TrialStartResult>"))
    }
}
