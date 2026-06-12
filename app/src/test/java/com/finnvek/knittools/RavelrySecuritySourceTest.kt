package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelrySecuritySourceTest {
    @Test
    fun `ravelry secrets and token storage are not owned by Android`() {
        val authManager = ProjectSourceFiles.read(RAVELRY_AUTH_MANAGER)
        val apiService = ProjectSourceFiles.read(RAVELRY_API_SERVICE)
        val buildScript = ProjectSourceFiles.read(APP_BUILD_SCRIPT)
        val securityDecisions = ProjectSourceFiles.read(SECURITY_DECISIONS)

        assertFalse(authManager.contains("EncryptedSharedPreferences"))
        assertFalse(authManager.contains("MasterKey"))
        assertFalse(authManager.contains("KEY_ACCESS_TOKEN"))
        assertFalse(authManager.contains("KEY_REFRESH_TOKEN"))
        assertFalse(authManager.contains("RAVELRY_OAUTH2_CLIENT_SECRET"))
        assertFalse(apiService.contains("RAVELRY_BASIC_AUTH_PASSWORD"))
        assertFalse(apiService.contains("Basic "))
        assertFalse(buildScript.contains("KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS"))
        assertFalse(buildScript.contains("KNITTOOLS_RAVELRY_BASIC_AUTH_USER"))
        assertFalse(buildScript.contains("RAVELRY_OAUTH2_CLIENT_SECRET"))
        assertTrue(securityDecisions.contains("Ravelry embedded credentials"))
        assertTrue(securityDecisions.contains("removed from Android"))
    }

    private companion object {
        private const val RAVELRY_AUTH_MANAGER =
            "app/src/main/java/com/finnvek/knittools/auth/RavelryAuthManager.kt"
        private const val RAVELRY_API_SERVICE =
            "app/src/main/java/com/finnvek/knittools/data/remote/RavelryApiService.kt"
        private const val APP_BUILD_SCRIPT = "app/build.gradle.kts"
        private const val SECURITY_DECISIONS = "config/security-decisions.md"
    }
}
