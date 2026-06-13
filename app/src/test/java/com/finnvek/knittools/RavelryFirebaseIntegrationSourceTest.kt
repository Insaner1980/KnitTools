package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryFirebaseIntegrationSourceTest {
    @Test
    fun `gradle declares firebase auth functions bom and google services gate`() {
        val versionCatalog = ProjectSourceFiles.read("gradle/libs.versions.toml")
        val rootBuild = ProjectSourceFiles.read("build.gradle.kts")
        val appBuild = ProjectSourceFiles.read("app/build.gradle.kts")
        val buildWorkflow = ProjectSourceFiles.read(".github/workflows/build.yml")
        val codeQlWorkflow = ProjectSourceFiles.read(".github/workflows/codeql.yml")

        assertTrue(versionCatalog.contains("firebaseBom = \"34.14.0\""))
        assertTrue(versionCatalog.contains("googleServices = \"4.4.4\""))
        assertTrue(versionCatalog.contains("firebase-bom"))
        assertTrue(versionCatalog.contains("firebase-auth"))
        assertTrue(versionCatalog.contains("firebase-functions"))
        assertTrue(versionCatalog.contains("google-services"))
        assertTrue(rootBuild.contains("alias(libs.plugins.google.services) apply false"))
        assertTrue(appBuild.contains("platform(libs.firebase.bom)"))
        assertTrue(appBuild.contains("implementation(libs.firebase.auth)"))
        assertTrue(appBuild.contains("implementation(libs.firebase.functions)"))
        assertTrue(appBuild.contains("verifyGoogleServicesJson"))
        assertTrue(appBuild.contains("@get:InputFiles"))
        assertFalse(appBuild.contains("@get:InputFile\n    @get:Optional"))
        assertTrue(appBuild.contains("app/google-services.json"))
        assertTrue(buildWorkflow.contains("KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"))
        assertTrue(codeQlWorkflow.contains("KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"))
    }

    @Test
    fun `sonar wrapper does not require firebase build artifact config`() {
        val rootBuild = ProjectSourceFiles.read("build.gradle.kts")
        val sonarWrapper = ProjectSourceFiles.read("tools/sonar.ps1")

        assertTrue(rootBuild.contains("dependsOn(\":app:jacocoDebugUnitTestReport\")"))
        assertTrue(sonarWrapper.contains("Command: reports/sonar.txt :: ./gradlew sonar"))
        assertTrue(sonarWrapper.contains("& .\\gradlew.bat \"sonar\" \"--console=plain\""))
        assertFalse(sonarWrapper.contains("assembleDebug"))
    }

    @Test
    fun `backend client and anonymous auth gateway are the Android Ravelry backend seams`() {
        val backendClient = ProjectSourceFiles.read(RAVELRY_BACKEND_CLIENT)
        val authGateway = ProjectSourceFiles.read(FIREBASE_AUTH_GATEWAY)
        val firebaseModule = ProjectSourceFiles.read(FIREBASE_MODULE)
        val apiService = ProjectSourceFiles.read(RAVELRY_API_SERVICE)

        assertTrue(backendClient.contains("ravelryStartAuth"))
        assertTrue(backendClient.contains("ravelrySearchPatterns"))
        assertTrue(backendClient.contains("ravelryImportPatternById"))
        assertTrue(backendClient.contains("ravelryImportPatternByUrl"))
        assertTrue(backendClient.contains("FirebaseFunctions"))
        assertTrue(authGateway.contains("signInAnonymously"))
        assertTrue(firebaseModule.contains("FirebaseAuth.getInstance()"))
        assertTrue(firebaseModule.contains("FirebaseFunctions.getInstance(\"europe-west1\")"))
        assertTrue(apiService.contains("RavelryBackendClient"))
        assertFalse(apiService.contains("BuildConfig.RAVELRY"))
    }

    private companion object {
        private const val RAVELRY_BACKEND_CLIENT =
            "app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendClient.kt"
        private const val FIREBASE_AUTH_GATEWAY =
            "app/src/main/java/com/finnvek/knittools/auth/FirebaseAnonymousAuthGateway.kt"
        private const val FIREBASE_MODULE =
            "app/src/main/java/com/finnvek/knittools/di/FirebaseModule.kt"
        private const val RAVELRY_API_SERVICE =
            "app/src/main/java/com/finnvek/knittools/data/remote/RavelryApiService.kt"
    }
}
