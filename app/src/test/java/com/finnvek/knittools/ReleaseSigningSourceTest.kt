package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseSigningSourceTest {
    @Test
    fun `release signing gate covers lower level artifact tasks`() {
        val appBuild = ProjectSourceFiles.read(APP_BUILD_SCRIPT)
        val releaseArtifactTasks =
            Regex("""val appReleaseArtifactTasks\s*=\s*setOf\((.*?)\)""", RegexOption.DOT_MATCHES_ALL)
                .find(appBuild)
                ?.groupValues
                ?.get(1)
                ?: error("appReleaseArtifactTasks block not found")

        listOf(
            ":app:assembleRelease",
            ":app:bundleRelease",
            ":app:packageRelease",
            ":app:packageReleaseBundle",
            ":app:packageReleaseUniversalApk",
            ":app:signReleaseBundle",
            ":app:publishRelease",
        ).forEach { taskPath ->
            assertTrue(
                "$taskPath must be blocked by missing release signing env",
                releaseArtifactTasks.contains("\"$taskPath\""),
            )
        }
        assertTrue(appBuild.contains("Release build estetty."))
        assertTrue(appBuild.contains("KEYSTORE_PATH ei osoita olemassa olevaan tiedostoon."))
    }

    private companion object {
        private const val APP_BUILD_SCRIPT = "app/build.gradle.kts"
    }
}
