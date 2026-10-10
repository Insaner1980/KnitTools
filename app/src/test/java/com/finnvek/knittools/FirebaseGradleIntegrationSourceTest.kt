package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FirebaseGradleIntegrationSourceTest {
    @Test
    fun `gradle declares crashlytics bom and stable google services wiring`() {
        val versionCatalog = ProjectSourceFiles.read("gradle/libs.versions.toml")
        val rootBuild = ProjectSourceFiles.read("build.gradle.kts")
        val appBuild = ProjectSourceFiles.read("app/build.gradle.kts")
        val gitignore = ProjectSourceFiles.read(".gitignore")
        val debugFirebaseOptions =
            ProjectSourceFiles.file("app/src/debug/res/values/debug_firebase_options.xml")
        val buildWorkflow = ProjectSourceFiles.read(".github/workflows/build.yml")
        val codeQlWorkflow = ProjectSourceFiles.read(".github/workflows/codeql.yml")

        assertTrue(versionCatalog.contains("firebaseBom = \"34.19.0\""))
        assertTrue(versionCatalog.contains("googleServices = \"4.5.0\""))
        assertTrue(versionCatalog.contains("firebase-bom"))
        assertFalse(versionCatalog.contains("firebase-auth"))
        assertFalse(versionCatalog.contains("firebase-functions"))
        assertTrue(versionCatalog.contains("google-services"))
        assertTrue(rootBuild.contains("alias(libs.plugins.google.services) apply false"))
        assertTrue(appBuild.contains("platform(libs.firebase.bom)"))
        assertTrue(appBuild.contains("implementation(libs.firebase.crashlytics)"))
        assertTrue(appBuild.contains("verifyGoogleServicesJson"))
        assertTrue(appBuild.contains("writeGoogleServicesJsonFromEnv"))
        assertGoogleServicesPlaceholderVariants(appBuild)
        assertGoogleServicesTaskWiring(appBuild)
        assertTrue(appBuild.contains("googleServicesPlaceholderJson"))
        assertTrue(appBuild.contains("apply(plugin = \"com.google.gms.google-services\")"))
        assertFalse(appBuild.contains("debugFirebaseArtifactRequested"))
        assertFalse(appBuild.contains("canMaterializeGoogleServicesJson"))
        assertFalse(appBuild.contains("if (canMaterializeGoogleServicesJson)"))
        assertReleaseLintWiring(appBuild)
        assertTrue(appBuild.contains("firebaseConfiguredArtifactTaskNames"))
        assertReleaseArtifactTasks(appBuild)
        assertFalse(appBuild.contains("\"assembleDebug\",\n        \"assembleRelease\""))
        assertTrue(appBuild.contains("outputs.upToDateWhen { targetFile.isFile }"))
        assertTrue(appBuild.contains("KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"))
        assertTrue(appBuild.contains("Base64.getMimeDecoder().decode(encodedConfig)"))
        assertFalse(appBuild.contains("inputs.property(\"encodedConfig\", encodedConfig.orEmpty())"))
        assertTrue(appBuild.contains("inputs.property(\"encodedConfigPresent\", !encodedConfig.isNullOrBlank())"))
        assertTrue(appBuild.contains("Release Firebase -konfiguraatio ei saa olla paikallinen debug-placeholder."))
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

    private fun assertReleaseArtifactTasks(appBuild: String) {
        listOf(
            "assembleRelease",
            "bundleRelease",
            "packageRelease",
            "packageReleaseBundle",
            "packageReleaseUniversalApk",
            "signReleaseBundle",
            "publishRelease",
        ).forEach { taskName ->
            assertTrue(appBuild.contains("\"$taskName\""))
        }
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
                        dependsOn(verifyPostHogConfig)
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    private fun assertReleaseLintWiring(appBuild: String) {
        assertTrue(
            appBuild.contains(
                "releaseLintRequested && !appReleaseArtifactsRequested && !firebaseConfigAvailable",
            ),
        )
        assertTrue(appBuild.contains("task.path == \":app:processReleaseGoogleServices\""))
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
        assertTrue(sonarWrapper.contains("Invoke-ManagedProcess"))
        assertTrue(sonarWrapper.contains("-Arguments @(\"sonar\", \"--console=plain\")"))
        assertFalse(sonarWrapper.contains("assembleDebug"))
    }
}
