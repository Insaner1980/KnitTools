buildscript {
    configurations.configureEach {
        resolutionStrategy.force(
            "com.google.guava:guava:33.8.0-jre",
            "com.fasterxml.jackson.core:jackson-core:2.23.0",
            "com.fasterxml.jackson.core:jackson-databind:2.22.4",
            "org.jsoup:jsoup:1.24.1",
            "org.bitbucket.b_c:jose4j:0.9.7",
            "org.jdom:jdom2:2.0.6.1",
            "org.json:json:20260814",
            "org.bouncycastle:bcprov-jdk18on:1.86",
            "org.bouncycastle:bcpkix-jdk18on:1.86",
            "org.bouncycastle:bcutil-jdk18on:1.86",
        )
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.baselineprofile) apply false
    // TODO: lisää takaisin kun dependency-analysis tukee AGP 9.x
    // alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics) apply false
}

val turvallinenWireRuntime = libs.wire.runtime.get()
val safeGuavaVersion = libs.versions.guava.get().removeSuffix("-android")

subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.guava" && requested.name == "guava") {
                val flavor = if (requested.version?.endsWith("-android") == true) "android" else "jre"
                useVersion("$safeGuavaVersion-$flavor")
                because("Guava 33.7.2 fixes deserialization memory exhaustion (CVE-2026-102554)")
            }
            if (
                requested.group == turvallinenWireRuntime.module.group &&
                requested.name == turvallinenWireRuntime.module.name
            ) {
                useVersion(turvallinenWireRuntime.versionConstraint.requiredVersion)
                because("Wire-dekoodauksen negatiivisen pituuden tarkistus korjattiin versiossa 6.3.0")
            }
        }
    }
}

val sonarProjectProperties =
    java.util.Properties().apply {
        val file = rootProject.file("sonar-project.properties")
        if (file.isFile) {
            file.inputStream().use(::load)
        }
    }

val gradleManagedSonarProperties =
    setOf(
        "sonar.sources",
        "sonar.tests",
        "sonar.java.binaries",
        "sonar.java.test.binaries",
        "sonar.java.libraries",
        "sonar.java.test.libraries",
        "sonar.kotlin.binaries",
    )

ktlint {
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

sonar {
    properties {
        property("sonar.host.url", sonarProjectProperties.getProperty("sonar.host.url", "https://sonarcloud.io"))
        sonarProjectProperties.forEach { key, value ->
            val propertyName = key.toString()
            if (propertyName !in gradleManagedSonarProperties) {
                property(propertyName, value.toString())
            }
        }
    }
}

project(":app") {
    sonar {
        properties {
            property(
                "sonar.java.test.binaries",
                tasks
                    .named<JavaCompile>("compileDebugAndroidTestJavaWithJavac")
                    .get()
                    .destinationDirectory
                    .get()
                    .asFile,
            )
            sonarProjectProperties
                .getProperty("sonar.coverage.exclusions")
                ?.takeIf { it.isNotBlank() }
                ?.let { exclusions ->
                    property("sonar.coverage.exclusions", exclusions)
                }
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory
                    .file("reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml")
                    .get()
                    .asFile
                    .absolutePath,
            )
        }
    }
}

tasks.named("sonar") {
    dependsOn(":app:jacocoDebugUnitTestReport")
    dependsOn(":app:compileDebugAndroidTestJavaWithJavac")
}
