plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.dependency.analysis)
}

ktlint {
    version.set(libs.versions.ktlintEngine)
}

android {
    namespace = "com.finnvek.knittools.baselineprofile"
    buildToolsVersion = libs.versions.androidBuildTools.get()
    compileSdk {
        version =
            release(
                libs.versions.androidCompileSdk
                    .get()
                    .toInt(),
            ) {
                minorApiLevel =
                    libs.versions.androidCompileMinorSdk
                        .get()
                        .toInt()
            }
    }

    defaultConfig {
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.androidTargetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
}
