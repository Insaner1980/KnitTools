package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ravelry avataan sovelluksen sisäisessä selaimessa Ravelryn omalla kirjautumisella.
 * Androidissa ei ole Ravelry-API-asiakasta, OAuth-paluuosoitetta, salaisuuksia eikä tokeneita.
 */
class RavelrySecuritySourceTest {
    @Test
    fun `android has no ravelry api client oauth callback or secrets`() {
        val manifest = ProjectSourceFiles.read(MANIFEST)
        val buildScript = ProjectSourceFiles.read(APP_BUILD_SCRIPT)
        val securityDecisions = ProjectSourceFiles.read(SECURITY_DECISIONS)

        REMOVED_SOURCES.forEach { path ->
            assertFalse(path, ProjectSourceFiles.file(path).toFile().exists())
        }
        assertFalse(manifest.contains("ravelry-auth-complete"))
        assertFalse(buildScript.contains("libs.firebase.auth"))
        assertFalse(buildScript.contains("libs.firebase.functions"))
        assertFalse(buildScript.contains("KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS"))
        assertFalse(buildScript.contains("KNITTOOLS_RAVELRY_BASIC_AUTH_USER"))
        assertFalse(buildScript.contains("RAVELRY_OAUTH2_CLIENT_SECRET"))
        assertTrue(securityDecisions.contains("Ravelry embedded credentials"))
        assertTrue(securityDecisions.contains("removed from Android"))
        assertTrue(securityDecisions.contains("In-app Ravelry browser and PDF downloads"))
    }

    private companion object {
        private const val MANIFEST = "app/src/main/AndroidManifest.xml"
        private const val APP_BUILD_SCRIPT = "app/build.gradle.kts"
        private const val SECURITY_DECISIONS = "config/security-decisions.md"
        private val REMOVED_SOURCES =
            listOf(
                "app/src/main/java/com/finnvek/knittools/auth/RavelryAuthManager.kt",
                "app/src/main/java/com/finnvek/knittools/data/remote/RavelryApiService.kt",
                "app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendClient.kt",
                "app/src/main/java/com/finnvek/knittools/di/FirebaseModule.kt",
            )
    }
}
