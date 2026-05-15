package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class RavelrySecuritySourceTest {
    @Test
    fun `ravelry embedded credential risk is documented and gated`() {
        val authManager = ProjectSourceFiles.read(RAVELRY_AUTH_MANAGER)
        val apiService = ProjectSourceFiles.read(RAVELRY_API_SERVICE)
        val buildScript = ProjectSourceFiles.read(APP_BUILD_SCRIPT)
        val securityDecisions = ProjectSourceFiles.read(SECURITY_DECISIONS)

        assertTrue(authManager.contains("code_challenge"))
        assertTrue(authManager.contains("code_verifier"))
        assertTrue(authManager.contains("RAVELRY_OAUTH2_CLIENT_SECRET"))
        assertTrue(apiService.contains("RAVELRY_BASIC_AUTH_PASSWORD"))
        assertTrue(buildScript.contains("KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS"))
        assertTrue(securityDecisions.contains("Ravelry embedded credentials"))
        assertTrue(securityDecisions.contains("Accepted risk"))
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
