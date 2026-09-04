package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryBackendAuthFlowSourceTest {
    @Test
    fun `manifest uses backend owned ravelry callback route`() {
        val manifest = ProjectSourceFiles.read("app/src/main/AndroidManifest.xml")

        assertTrue(manifest.contains("android:scheme=\"knittools\""))
        assertTrue(manifest.contains("android:host=\"ravelry-auth-complete\""))
        assertFalse(manifest.contains("android:scheme=\"com.finnvek.knittools\""))
        assertFalse(manifest.contains("android:host=\"oauth\""))
    }

    @Test
    fun `ravelry UI uses backend auth state instead of legacy authenticated boolean`() {
        val viewModel = ProjectSourceFiles.read(RAVELRY_VIEW_MODEL)
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)
        val authManager = ProjectSourceFiles.read(RAVELRY_AUTH_MANAGER)
        val backendClient = ProjectSourceFiles.read(RAVELRY_BACKEND_CLIENT)
        val accountHeader = ProjectSourceFiles.read(RAVELRY_ACCOUNT_HEADER)
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val versionCatalog = ProjectSourceFiles.read(VERSION_CATALOG)

        assertTrue(viewModel.contains("val authState"))
        assertTrue(viewModel.contains("signInLaunchRequests"))
        assertTrue(viewModel.contains("startSignIn"))
        assertTrue(viewModel.contains("refreshAuthStatus"))
        assertTrue(viewModel.contains("disconnectRavelry"))
        assertFalse(viewModel.contains("createSignInUri"))
        assertFalse(viewModel.contains("val isAuthenticated"))

        assertTrue(accountHeader.contains("RavelryAuthState.Connected"))
        assertTrue(accountHeader.contains("ravelry_connected_as"))
        assertTrue(searchScreen.contains("CollectWithLifecycleEffect({ viewModel.signInLaunchRequests })"))
        assertTrue(detailScreen.contains("authState.collectAsStateWithLifecycle"))
        assertTrue(navGraph.contains("onLaunchRavelryAuth"))
        assertTrue(mainActivity.contains("AuthTabIntent"))
        assertTrue(mainActivity.contains("CustomTabsIntent"))
        val authLaunch =
            mainActivity
                .substringAfter("fun launchRavelryAuth")
                .substringBefore("override fun onNewIntent")
        assertEquals(2, Regex("catch \\(_:\\sActivityNotFoundException\\)").findAll(authLaunch).count())
        assertTrue(authLaunch.contains("ravelryAuthManager.markBrowserAuthCancelled()"))
        assertTrue(versionCatalog.contains("browser = \"1.10.0\""))
        assertTrue(authManager.contains("backendClient.startAuth"))
        assertTrue(authManager.contains("backendClient.authStatus"))
        assertTrue(authManager.contains("backendClient.disconnect"))
        assertTrue(authManager.contains("markBrowserAuthCancelled"))
        assertTrue(backendClient.contains("ravelryStartAuth"))
        assertTrue(backendClient.contains("ravelryAuthStatus"))
        assertTrue(backendClient.contains("ravelryDisconnect"))
    }

    private companion object {
        private const val RAVELRY_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryViewModel.kt"
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val RAVELRY_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt"
        private const val RAVELRY_AUTH_MANAGER =
            "app/src/main/java/com/finnvek/knittools/auth/RavelryAuthManager.kt"
        private const val RAVELRY_BACKEND_CLIENT =
            "app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendClient.kt"
        private const val RAVELRY_ACCOUNT_HEADER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryAccountHeader.kt"
        private const val MAIN_ACTIVITY =
            "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val VERSION_CATALOG = "gradle/libs.versions.toml"
    }
}
