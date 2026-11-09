package com.finnvek.knittools.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import com.finnvek.knittools.ui.layout.AdaptiveContent
import com.finnvek.knittools.ui.layout.LocalAdaptiveContentSize
import com.finnvek.knittools.ui.layout.UnobstructedWindowContent

@Composable
internal fun KnitToolsNavigationScaffold(
    navControllerProvider: @Composable () -> NavController,
    showNavigation: Boolean,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    UnobstructedWindowContent(modifier) {
        AdaptiveContent(Modifier.fillMaxSize()) {
            val windowSize = LocalAdaptiveContentSize.current
            val useRail = windowSize.useNavigationRail
            Row(Modifier.fillMaxSize()) {
                if (showNavigation && useRail) {
                    KnitToolsNavigationRail(navControllerProvider)
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
                    bottomBar = {
                        if (showNavigation && !useRail) {
                            KnitToolsBottomBar(navControllerProvider, short = windowSize.useShortNavigationBar)
                        }
                    },
                ) { padding ->
                    AdaptiveContent(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .consumeWindowInsets(padding)
                            .testTag("adaptive-content"),
                        content = content,
                    )
                }
            }
        }
    }
}
