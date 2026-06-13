import com.android.build.api.variant.BuildConfigField
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.StringReader
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

if (googleServicesJsonConfigFile.asFile.isFile) {
    apply(plugin = "com.google.gms.google-services")
}

abstract class VerifyGoogleServicesJsonTask : DefaultTask() {
    @get:InputFiles
    abstract val googleServicesJsonFile: RegularFileProperty

    @TaskAction
    fun verify() {
        if (!googleServicesJsonFile.asFile.get().isFile) {
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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.finnvek.knittools"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

val verifyGoogleServicesJson by tasks.registering(VerifyGoogleServicesJsonTask::class) {
    group = "verification"
    description = "Tarkistaa, että Android Firebase -konfiguraatio on paikallaan."
    googleServicesJsonFile.set(googleServicesJsonConfigFile)
}

val firebaseConfiguredArtifactTaskNames =
    setOf(
        "assembleDebug",
        "assembleRelease",
        "bundleRelease",
    )

tasks.configureEach {
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
        "**/MainActivity.*",
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
        "**/PatternRowDetector*.*",
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
        implementation(libs.kotlinx.serialization.json) {
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
