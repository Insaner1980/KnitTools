package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RavelryFirebaseIntegrationSourceTest {
    @Test
    fun `gradle declares firebase auth functions bom and stable google services wiring`() {
        val versionCatalog = ProjectSourceFiles.read("gradle/libs.versions.toml")
        val rootBuild = ProjectSourceFiles.read("build.gradle.kts")
        val appBuild = ProjectSourceFiles.read("app/build.gradle.kts")
        val gitignore = ProjectSourceFiles.read(".gitignore")
        val debugFirebaseOptions =
            ProjectSourceFiles.file("app/src/debug/res/values/debug_firebase_options.xml")
        val buildWorkflow = ProjectSourceFiles.read(".github/workflows/build.yml")
        val codeQlWorkflow = ProjectSourceFiles.read(".github/workflows/codeql.yml")

        assertTrue(versionCatalog.contains("firebaseBom = \"34.16.0\""))
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
        assertTrue(appBuild.contains("writeGoogleServicesJsonFromEnv"))
        assertGoogleServicesPlaceholderVariants(appBuild)
        assertGoogleServicesTaskWiring(appBuild)
        assertTrue(appBuild.contains("googleServicesPlaceholderJson"))
        assertTrue(appBuild.contains("apply(plugin = \"com.google.gms.google-services\")"))
        assertFalse(appBuild.contains("debugFirebaseArtifactRequested"))
        assertFalse(appBuild.contains("canMaterializeGoogleServicesJson"))
        assertFalse(appBuild.contains("if (canMaterializeGoogleServicesJson)"))
        assertTrue(appBuild.contains("firebaseConfiguredArtifactTaskNames"))
        assertTrue(appBuild.contains("\"assembleRelease\""))
        assertTrue(appBuild.contains("\"bundleRelease\""))
        assertFalse(appBuild.contains("\"assembleDebug\",\n        \"assembleRelease\""))
        assertTrue(appBuild.contains("outputs.upToDateWhen { targetFile.isFile }"))
        assertTrue(appBuild.contains("KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"))
        assertTrue(appBuild.contains("Base64.getMimeDecoder().decode(encodedConfig)"))
        assertTrue(appBuild.contains("inputs.files(rootFile).withPropertyName(\"rootGoogleServicesJsonFile\")"))
        assertTrue(appBuild.contains("inputs.files(targetFile).withPropertyName(\"googleServicesJsonFile\")"))
        assertTrue(appBuild.contains("object GoogleServicesJsonTaskActions"))
        assertTrue(appBuild.contains("tasks.register(\"writeGoogleServicesJsonFromEnv\")"))
        assertTrue(appBuild.contains("tasks.register(\"write\${taskSuffix}GoogleServicesJson\")"))
        assertTrue(appBuild.contains("tasks.register(\"verifyGoogleServicesJson\")"))
        assertTrue(appBuild.contains("GoogleServicesJsonTaskActions.writeFromEnv("))
        assertTrue(appBuild.contains("GoogleServicesJsonTaskActions.writePlaceholder("))
        assertTrue(appBuild.contains("GoogleServicesJsonTaskActions.verify("))
        assertFalse(appBuild.contains("WriteGoogleServicesJsonFromEnvTask : DefaultTask"))
        assertFalse(appBuild.contains("WriteDebugGoogleServicesJsonTask : DefaultTask"))
        assertFalse(appBuild.contains("VerifyGoogleServicesJsonTask : DefaultTask"))
        assertFalse(appBuild.contains("@get:InputFile\n    @get:Optional"))
        assertTrue(appBuild.contains("app/google-services.json"))
        assertTrue(gitignore.contains("app/src/*/google-services.json"))
        assertFalse(Files.exists(debugFirebaseOptions))
        assertFalse(buildWorkflow.contains("Missing KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64 secret"))
        assertFalse(codeQlWorkflow.contains("Missing KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64 secret"))
        assertFalse(buildWorkflow.contains("secrets.KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"))
        assertFalse(codeQlWorkflow.contains("secrets.KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"))
    }

    // Placeholder-config kuuluu vain varianteille, jotka eivät päädy jakeluun.
    // Release-artefaktit nojaavat edelleen verifyGoogleServicesJson-tarkistukseen.
    private fun assertGoogleServicesPlaceholderVariants(appBuild: String) {
        assertTrue(
            appBuild.contains(
                "val googleServicesPlaceholderVariants = " +
                    "listOf(\"debug\", \"benchmarkRelease\", \"nonMinifiedRelease\")",
            ),
        )
        assertTrue(appBuild.contains("layout.projectDirectory.file(\"src/\$variantName/google-services.json\")"))
    }

    private fun assertGoogleServicesTaskWiring(appBuild: String) {
        assertTrue(
            appBuild.contains(
                """
                tasks.configureEach {
                    if (name.startsWith("process") && name.endsWith("GoogleServices")) {
                        dependsOn(writeGoogleServicesJsonFromEnv)

                        // processBenchmarkReleaseGoogleServices -> benchmarkRelease
                        val variantName =
                            name
                                .removePrefix("process")
                                .removeSuffix("GoogleServices")
                                .replaceFirstChar { it.lowercaseChar() }
                        writeGoogleServicesPlaceholderTasks[variantName]?.let { dependsOn(it) }
                    }

                    if (name in firebaseConfiguredArtifactTaskNames) {
                        dependsOn(verifyGoogleServicesJson)
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `pull request workflows let Gradle materialize debug Firebase config`() {
        val buildWorkflow = ProjectSourceFiles.read(".github/workflows/build.yml")
        val codeQlWorkflow = ProjectSourceFiles.read(".github/workflows/codeql.yml")

        assertTrue(buildWorkflow.contains("run: ./gradlew assembleDebug"))
        assertTrue(codeQlWorkflow.contains("run: ./gradlew assembleDebug --no-daemon"))
        assertFalse(buildWorkflow.contains("Write Firebase Android config"))
        assertFalse(codeQlWorkflow.contains("Write Firebase Android config"))
        assertFalse(buildWorkflow.contains("app/google-services.json"))
        assertFalse(codeQlWorkflow.contains("app/google-services.json"))
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
