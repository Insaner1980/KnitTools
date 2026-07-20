import com.android.build.api.variant.BuildConfigField
import org.gradle.api.GradleException
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File
import java.io.StringReader
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.stability.analyzer)
    jacoco
}

val releaseSigningEnvPrefix = "KNITTOOLS" // Change to your app name, e.g. "KNITTOOLS"
val debugCredentialsFile = rootProject.layout.projectDirectory.file("debug.credentials.properties")
val debugCredentialsText = providers.fileContents(debugCredentialsFile).asText.orElse("")
val googleServicesJsonConfigFile = layout.projectDirectory.file("google-services.json")

// Variantit, jotka eivät päädy jakeluun ja joille riittää paikallinen placeholder-config.
// benchmarkRelease ja nonMinifiedRelease tulevat baselineprofile-pluginista.
val googleServicesPlaceholderVariants = listOf("debug", "benchmarkRelease", "nonMinifiedRelease")

fun googleServicesJsonFileFor(variantName: String) =
    layout.projectDirectory.file("src/$variantName/google-services.json")

val googleServicesJsonBase64EnvVar = "KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"
val googleServicesJsonBase64Env = providers.environmentVariable(googleServicesJsonBase64EnvVar)
val googleServicesPlaceholderJson =
    """
    {
      "project_info": {
        "project_number": "123456789012",
        "project_id": "knittools-local-debug",
        "storage_bucket": "knittools-local-debug.appspot.com"
      },
      "client": [
        {
          "client_info": {
            "mobilesdk_app_id": "1:123456789012:android:0000000000000000",
            "android_client_info": {
              "package_name": "com.finnvek.knittools"
            }
          },
          "oauth_client": [],
          "api_key": [
            {
              "current_key": "debug-placeholder-api-key"
            }
          ],
          "services": {
            "appinvite_service": {
              "other_platform_oauth_client": []
            }
          }
        }
      ],
      "configuration_version": "1"
    }
    """.trimIndent()

apply(plugin = "com.google.gms.google-services")

object GoogleServicesJsonTaskActions {
    private const val PLACEHOLDER_API_KEY = "debug-placeholder-api-key"

    fun writeFromEnv(
        targetFile: File,
        envName: String,
        encodedConfig: String?,
    ) {
        if (targetFile.isFile) {
            return
        }

        val encodedConfig = encodedConfig?.takeIf { it.isNotBlank() } ?: return
        val decodedConfig =
            try {
                Base64.getMimeDecoder().decode(encodedConfig)
            } catch (exception: IllegalArgumentException) {
                throw GradleException(
                    "$envName ei ole kelvollinen Base64-koodattu google-services.json.",
                    exception,
                )
            }

        targetFile.parentFile.mkdirs()
        targetFile.writeBytes(decodedConfig)
    }

    fun writePlaceholder(
        rootGoogleServicesJsonFile: File,
        encodedConfig: String?,
        placeholderJson: String,
        targetFile: File,
    ) {
        if (rootGoogleServicesJsonFile.isFile || !encodedConfig.isNullOrBlank()) {
            if (targetFile.isFile && targetFile.readText(Charsets.UTF_8).contains(PLACEHOLDER_API_KEY)) {
                targetFile.delete()
            }
            return
        }

        if (targetFile.isFile) {
            return
        }

        targetFile.parentFile.mkdirs()
        targetFile.writeText(placeholderJson, Charsets.UTF_8)
    }

    fun verify(googleServicesJsonFile: File) {
        if (!googleServicesJsonFile.isFile) {
            error(
                "Android Firebase -build vaatii tiedoston app/google-services.json. " +
                    "Pidä tiedosto gitignored-polussa paikallisesti tai luo se CI:ssä " +
                    "KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64 -salaisuudesta.",
            )
        }
    }
}

val releaseSigningEnvNames =
    listOf(
        "${releaseSigningEnvPrefix}_KEYSTORE_PATH",
        "${releaseSigningEnvPrefix}_KEYSTORE_PASSWORD",
        "${releaseSigningEnvPrefix}_KEY_ALIAS",
        "${releaseSigningEnvPrefix}_KEY_PASSWORD",
    )

val releaseSigningAvailable =
    releaseSigningEnvNames.all { envName ->
        providers.environmentVariable(envName).orNull?.isNotBlank() == true
    }

fun missingEnvNames(names: List<String>): List<String> =
    names.filter { envName ->
        providers.environmentVariable(envName).orNull?.isBlank() != false
    }

fun requiredReleaseEnv(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: error("Release signing requires the $name environment variable.")

fun debugBuildConfigField(
    name: String,
    vararg envNames: String,
): Provider<BuildConfigField<String>> {
    val credentialsTextProvider = debugCredentialsText
    val environmentValue =
        envNames.firstNotNullOfOrNull { envName ->
            providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        }

    return credentialsTextProvider.map { text ->
        val localValue =
            Properties()
                .also { props ->
                    StringReader(text).use { props.load(it) }
                }.getProperty(name, "")
        val value = environmentValue ?: localValue
        val quotedValue =
            "\"${value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")}\""
        BuildConfigField("String", quotedValue, null)
    }
}

android {
    namespace = "com.finnvek.knittools"
    compileSdk =
        libs.versions.androidCompileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.finnvek.knittools"
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.androidTargetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
        getByName("debug").kotlin.directories.add("src/debugShared/kotlin")
    }

    signingConfigs {
        create("release") {
            if (releaseSigningAvailable) {
                storeFile = file(requiredReleaseEnv("${releaseSigningEnvPrefix}_KEYSTORE_PATH"))
                storePassword = requiredReleaseEnv("${releaseSigningEnvPrefix}_KEYSTORE_PASSWORD")
                keyAlias = requiredReleaseEnv("${releaseSigningEnvPrefix}_KEY_ALIAS")
                keyPassword = requiredReleaseEnv("${releaseSigningEnvPrefix}_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true

        enable +=
            setOf(
                "NewApi",
                "InlinedApi",
                "ObsoleteSdkInt",
                "UnusedResources",
                "MissingPermission",
                "HardcodedText",
                "MissingTranslation",
                "Recycle",
                "StaticFieldLeak",
                "SetTextI18n",
                "RtlHardcoded",
                "ContentDescription",
                "PrivateResource",
                "InvalidPackage",
                "WrongThread",
            )

        disable +=
            setOf(
                "GradleDependency",
                "AndroidGradlePluginVersion",
            )

        checkGeneratedSources = false
        htmlReport = true
        xmlReport = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val buildConfigFields =
            variant.buildConfigFields
                ?: error("Debug BuildConfig -kenttiä ei voi määrittää, koska BuildConfig ei ole käytössä.")

        buildConfigFields.put(
            "SENTRY_DSN",
            debugBuildConfigField("sentry.dsn", "KNITTOOLS_SENTRY_DSN", "SENTRY_DSN"),
        )
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    outputDirectory = rootProject.layout.projectDirectory.dir("reports")
    data {
        val defaultDataDirectory =
            rootProject.layout.projectDirectory
                .dir(".gradle/dependency-check-data")
                .asFile.absolutePath

        directory =
            providers
                .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
                .orElse(defaultDataDirectory)
                .get()
    }
    autoUpdate =
        (providers.environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE").orNull ?: "true")
            .toBoolean()
    failBuildOnCVSS =
        providers
            .environmentVariable("DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS")
            .orNull
            ?.toFloatOrNull()
            ?: 7f
    suppressionFiles =
        listOf(
            rootProject.file("config/dependency-check-suppressions.xml").absolutePath,
        )
    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    skipTestGroups = true
    hostedSuppressions {
        enabled = false
    }
    analyzers {
        kev {
            enabled = false
        }
        ossIndex {
            enabled = false
        }
        retirejs {
            enabled = false
        }
    }
    nvd {
        providers.environmentVariable("NVD_API_KEY").orNull?.let { apiKey = it }
        delay =
            providers
                .environmentVariable("NVD_API_DELAY_MS")
                .orNull
                ?.toIntOrNull()
                ?: 6_000
        maxRetryCount =
            providers
                .environmentVariable("NVD_API_MAX_RETRY_COUNT")
                .orNull
                ?.toIntOrNull()
                ?: 20
        validForHours =
            providers
                .environmentVariable("NVD_VALID_FOR_HOURS")
                .orNull
                ?.toIntOrNull()
                ?: 24
    }
}

gradle.taskGraph.whenReady {
    val appReleaseArtifactTasks =
        setOf(
            ":app:assembleRelease",
            ":app:bundleRelease",
            ":app:packageRelease",
            ":app:packageReleaseBundle",
            ":app:packageReleaseUniversalApk",
            ":app:signReleaseBundle",
            ":app:publishRelease",
        )

    val appReleaseArtifactsRequested =
        allTasks.any { task ->
            task.path in appReleaseArtifactTasks
        }

    if (appReleaseArtifactsRequested) {
        val missingSigningEnvNames = missingEnvNames(releaseSigningEnvNames)
        val releaseProblems =
            buildList {
                if (missingSigningEnvNames.isNotEmpty()) {
                    add(
                        "Puuttuvat release signing -muuttujat: " +
                            missingSigningEnvNames.joinToString(),
                    )
                }
            }

        if (releaseProblems.isNotEmpty()) {
            error(
                "Release build estetty.\n" +
                    releaseProblems.joinToString(separator = "\n") { "- $it" },
            )
        }
    }
}

val writeGoogleServicesJsonFromEnv =
    tasks.register("writeGoogleServicesJsonFromEnv") {
        group = "build setup"
        description = "Luo ignored app/google-services.json -tiedoston ympäristömuuttujasta tarvittaessa."

        val targetFile = googleServicesJsonConfigFile.asFile
        val envName = googleServicesJsonBase64EnvVar
        val encodedConfig = googleServicesJsonBase64Env.orNull

        inputs.property("base64EnvName", envName)
        inputs.property("encodedConfig", encodedConfig.orEmpty())
        outputs.file(targetFile)
        outputs.upToDateWhen { targetFile.isFile }

        doLast {
            GoogleServicesJsonTaskActions.writeFromEnv(targetFile, envName, encodedConfig)
        }
    }

val writeGoogleServicesPlaceholderTasks =
    googleServicesPlaceholderVariants.associateWith { variantName ->
        val taskSuffix = variantName.replaceFirstChar { it.uppercaseChar() }
        tasks.register("write${taskSuffix}GoogleServicesJson") {
            group = "build setup"
            description = "Luo $variantName-buildille ignored placeholder Firebase -konfiguraation tarvittaessa."
            dependsOn(writeGoogleServicesJsonFromEnv)

            val rootFile = googleServicesJsonConfigFile.asFile
            val targetFile = googleServicesJsonFileFor(variantName).asFile
            val encodedConfig = googleServicesJsonBase64Env.orNull
            val placeholderJson = googleServicesPlaceholderJson

            inputs.files(rootFile).withPropertyName("rootGoogleServicesJsonFile")
            inputs.property("encodedConfig", encodedConfig.orEmpty())
            inputs.property("placeholderJson", placeholderJson)
            outputs.file(targetFile)
            outputs.upToDateWhen {
                rootFile.isFile ||
                    !encodedConfig.isNullOrBlank() ||
                    targetFile.isFile
            }

            doLast {
                GoogleServicesJsonTaskActions.writePlaceholder(
                    rootFile,
                    encodedConfig,
                    placeholderJson,
                    targetFile,
                )
            }
        }
    }

val verifyGoogleServicesJson =
    tasks.register("verifyGoogleServicesJson") {
        group = "verification"
        description = "Tarkistaa, että Android Firebase -konfiguraatio on paikallaan."
        dependsOn(writeGoogleServicesJsonFromEnv)

        val targetFile = googleServicesJsonConfigFile.asFile

        inputs.files(targetFile).withPropertyName("googleServicesJsonFile")

        doLast {
            GoogleServicesJsonTaskActions.verify(targetFile)
        }
    }

val firebaseConfiguredArtifactTaskNames =
    setOf(
        "assembleRelease",
        "bundleRelease",
    )

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

room {
    schemaDirectory("$projectDir/schemas")
}

hilt {
    enableAggregatingTask = true
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("detekt-baseline.xml")
    parallel = true
}

tasks.configureEach {
    if (name.startsWith("hiltJavaCompile") && name.endsWith("UnitTest")) {
        enabled = false
    }
}

val jacocoCoverageExclusionPatterns =
    listOf(
        "**/BuildConfig.*",
        "**/R.class",
        "**/R$*.class",
        "**/*Test*.*",
        "**/*ComposableSingletons*.*",
        "**/*_Factory.*",
        "**/*_HiltModules*.*",
        "**/*Hilt*.*",
        "**/*Dagger*.*",
        "**/App.*",
        "**/App$*.*",
        "**/MainActivity.*",
        "**/MainActivity$*.*",
        "**/MainActivityKt*.*",
        "**/SentryInit.*",
        "**/SentryInit$*.*",
        "**/di/**",
        "**/ui/**",
        "**/widget/**",
        "**/auth/**",
        "**/billing/**",
        "**/pro/**",
        "**/data/datastore/**",
        "**/data/storage/**",
        "**/data/remote/**",
        "**/data/local/**",
    )

val jacocoCoverageExclusions =
    jacocoCoverageExclusionPatterns +
        jacocoCoverageExclusionPatterns.map { it.replace("/", "\\") }

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Luo debug-unit-testien JaCoCo XML- ja HTML-kattavuusraportit."

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
                exclude(*jacocoCoverageExclusions.toTypedArray())
            },
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
                exclude(*jacocoCoverageExclusions.toTypedArray())
            },
        ),
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            )
        },
    )
}

dependencies {
    constraints {
        implementation(libs.kotlinx.serialization.core) {
            because("Room 2.8.x migration helpers require kotlinx.serialization 1.8.1")
        }
        implementation(libs.guava) {
            because(
                "kotlinx-coroutines-guava tuo Guava 31.0.1-jre:n; " +
                    "constraint nostaa Android-artefaktin korjattuun versioon",
            )
        }
        implementation(libs.kotlin.stdlib.jdk7) {
            because(
                "Kotlin stdlib tuo vanhan jdk7-artefaktin transitiivisesti; " +
                    "constraint nostaa sen korjattuun Kotlin-versioon",
            )
        }
        implementation(libs.work.runtime) {
            because(
                "Glance 1.1.1 tuo WorkManager 2.7.1:n, jonka inspector.jar " +
                    "sisaltaa haavoittuvan protobuf-javaliten",
            )
        }
        implementation(libs.work.runtime.ktx) {
            because("Pidetaan WorkManager runtime ja ktx samassa korjatussa versiossa")
        }
    }

    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(composeBom)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.process)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.datastore.preferences)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.appcompat)

    // SplashScreen
    implementation(libs.splashscreen)

    // Google Play In-App Review
    implementation(libs.play.review)

    // Google Play In-App Updates
    implementation(libs.play.update)

    // Google Play Billing
    implementation(libs.billing)

    // Glance (home screen widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // Ktor (HTTP client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Browser (Custom Chrome Tabs)
    implementation(libs.browser)

    // Firebase backend integration
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.functions)

    // Baseline Profiles
    implementation(libs.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Sentry on vain debug-diagnostiikkaa. Release-luokkapolku tarkistetaan tools\sentry.ps1-komennolla.
    debugImplementation(libs.sentry.android.core)

    // Detekt plugins
    detektPlugins(libs.detekt.compose.rules)
    ktlintRuleset(libs.compose.rules.ktlint)
    lintChecks(libs.android.security.lints)

    // Testing
    testImplementation(libs.org.json)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
