package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ComparisonBuildSourceTest {
    @Test
    fun `comparison build is installable beside the regular app`() {
        val appBuild = File("build.gradle.kts").readText()
        val comparisonConfig = File("src/comparison/res/values/comparison_config.xml").readText()

        assertTrue(appBuild.contains("create(\"comparison\")"))
        assertTrue(appBuild.contains("applicationIdSuffix = \".fable\""))
        assertTrue(appBuild.contains("versionNameSuffix = \"-fable\""))
        assertTrue(comparisonConfig.contains("<string name=\"app_name\">KnitTools Fable</string>"))
    }
}
