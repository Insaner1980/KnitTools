import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Lataa local.properties BuildConfig-kenttiä varten
val localProps =
    Properties().also { props ->
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { props.load(it) }
    }

val releaseSigningEnvPrefix = "KNITTOOLS" // Change to your app name, e.g. "KNITTOOLS"

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

val embeddedRavelryCredentialsAllowed =
    providers.environmentVariable("KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS").orNull == "true"

val releaseRavelryEnvNames =
    listOf(
        "KNITTOOLS_RAVELRY_BASIC_AUTH_USER",
        "KNITTOOLS_RAVELRY_BASIC_AUTH_PASSWORD",
        "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_ID",
        "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET",
    )

fun missingEnvNames(names: List<String>): List<String> =
    names.filter { envName ->
        providers.environmentVariable(envName).orNull?.isBlank() != false
    }

fun requiredReleaseEnv(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: error("Release signing requires the $name environment variable.")

fun localProp(name: String): String = localProps.getProperty(name, "")

fun releaseEnvOrEmpty(name: String): String =
    providers
        .environmentVariable(name)
        .orNull
        ?.takeIf { it.isNotBlank() }
        .orEmpty()

fun quotedBuildConfigValue(value: String): String =
    "\"${value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")}\""

android {
    namespace = "com.finnvek.knittools"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.finnvek.knittools"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "RAVELRY_BASIC_AUTH_USER", quotedBuildConfigValue(""))
        buildConfigField("String", "RAVELRY_BASIC_AUTH_PASSWORD", quotedBuildConfigValue(""))
        buildConfigField("String", "RAVELRY_OAUTH2_CLIENT_ID", quotedBuildConfigValue(""))
        buildConfigField("String", "RAVELRY_OAUTH2_CLIENT_SECRET", quotedBuildConfigValue(""))
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
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
        debug {
            buildConfigField(
                "String",
                "RAVELRY_BASIC_AUTH_USER",
                quotedBuildConfigValue(localProp("ravelry.basicAuthUser")),
            )
            buildConfigField(
                "String",
                "RAVELRY_BASIC_AUTH_PASSWORD",
                quotedBuildConfigValue(localProp("ravelry.basicAuthPassword")),
            )
            buildConfigField(
                "String",
                "RAVELRY_OAUTH2_CLIENT_ID",
                quotedBuildConfigValue(localProp("ravelry.oauth2ClientId")),
            )
            buildConfigField(
                "String",
                "RAVELRY_OAUTH2_CLIENT_SECRET",
                quotedBuildConfigValue(localProp("ravelry.oauth2ClientSecret")),
            )
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField(
                "String",
                "RAVELRY_BASIC_AUTH_USER",
                quotedBuildConfigValue(releaseEnvOrEmpty("KNITTOOLS_RAVELRY_BASIC_AUTH_USER")),
            )
            buildConfigField(
                "String",
                "RAVELRY_BASIC_AUTH_PASSWORD",
                quotedBuildConfigValue(releaseEnvOrEmpty("KNITTOOLS_RAVELRY_BASIC_AUTH_PASSWORD")),
            )
            buildConfigField(
                "String",
                "RAVELRY_OAUTH2_CLIENT_ID",
                quotedBuildConfigValue(releaseEnvOrEmpty("KNITTOOLS_RAVELRY_OAUTH2_CLIENT_ID")),
            )
            buildConfigField(
                "String",
                "RAVELRY_OAUTH2_CLIENT_SECRET",
                quotedBuildConfigValue(releaseEnvOrEmpty("KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET")),
            )
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

gradle.taskGraph.whenReady {
    val requestedTasks = gradle.startParameter.taskNames

    val explicitAppReleaseArtifactsRequested =
        requestedTasks.any { requestedTask ->
            val taskName = requestedTask.substringAfterLast(':')
            val targetsApp = !requestedTask.contains(':') || requestedTask.startsWith(":app:")
            targetsApp &&
                taskName in
                setOf(
                    "assembleRelease",
                    "bundleRelease",
                    "packageRelease",
                    "publishRelease",
                )
        }

    if (explicitAppReleaseArtifactsRequested) {
        val missingSigningEnvNames = missingEnvNames(releaseSigningEnvNames)
        val missingRavelryEnvNames = missingEnvNames(releaseRavelryEnvNames)
        val releaseProblems =
            buildList {
                if (missingSigningEnvNames.isNotEmpty()) {
                    add(
                        "Puuttuvat release signing -muuttujat: " +
                            missingSigningEnvNames.joinToString(),
                    )
                }
                if (!embeddedRavelryCredentialsAllowed) {
                    add(
                        "Release build upottaa Ravelry-credentialit BuildConfigiin. " +
                            "Aseta KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true jatkaaksesi.",
                    )
                }
                if (missingRavelryEnvNames.isNotEmpty()) {
                    add(
                        "Puuttuvat release Ravelry -muuttujat: " +
                            missingRavelryEnvNames.joinToString(),
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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

dependencies {
    constraints {
        implementation(libs.kotlinx.serialization.core) {
            because("Room 2.8.x migration helpers require kotlinx.serialization 1.8.1")
        }
        implementation(libs.kotlinx.serialization.json) {
            because("Room 2.8.x migration helpers require kotlinx.serialization 1.8.1")
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

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.process)
    implementation(libs.lifecycle.runtime.compose)

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

    // DataStore
    implementation(libs.datastore.preferences)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // SplashScreen
    implementation(libs.splashscreen)

    // Google Play In-App Review
    implementation(libs.play.review)

    // Google Play In-App Updates
    implementation(libs.play.update)

    // Google Play Billing
    implementation(libs.billing)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)

    // ML Kit OCR
    implementation(libs.mlkit.text.recognition)

    // ML Kit GenAI (Gemini Nano on-device)
    implementation(libs.mlkit.genai.prompt)

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

    // Security (encrypted token storage)
    implementation(libs.security.crypto)

    // Browser (Custom Chrome Tabs)
    implementation(libs.browser)

    // Baseline Profiles
    implementation(libs.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Detekt plugins
    detektPlugins(libs.detekt.compose.rules)

    // Testing
    testImplementation("org.json:json:20240303")
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
}
