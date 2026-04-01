plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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

fun requiredReleaseEnv(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: error("Release signing requires the $name environment variable.")

android {
    namespace = "com.finnvek.knittools"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.finnvek.knittools"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            // Add debug-only build config fields here
        }
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

gradle.taskGraph.whenReady {
    val releaseArtifactsRequested =
        allTasks.any { task ->
            val name = task.name
            name.endsWith("Release") &&
                (
                    name.startsWith("assemble") ||
                        name.startsWith("bundle") ||
                        name.startsWith("package") ||
                        name.startsWith("publish")
                )
        }

    if (releaseArtifactsRequested && !releaseSigningAvailable) {
        error(
            "Release signing requires these environment variables: " +
                releaseSigningEnvNames.joinToString(),
        )
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
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Google Play Billing
    implementation(libs.billing)

    // ML Kit OCR
    implementation(libs.mlkit.text.recognition)

    // CameraX
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Detekt plugins
    detektPlugins(libs.detekt.compose.rules)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
}
