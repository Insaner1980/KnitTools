package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSecScriptSourceTest {
    @Test
    fun `deepsec report revalidates stored findings before export`() {
        val packageJson = ProjectSourceFiles.read(".deepsec/package.json")
        val exportPipeline =
            "pnpm run deepsec:process && pnpm run deepsec:revalidate && " +
                "node scripts/mark-accepted-risks.mjs && pnpm run deepsec:export"

        assertTrue(
            packageJson.contains(
                "pnpm run deepsec:scan && $exportPipeline",
            ),
        )
        assertTrue(
            packageJson.contains(
                "pnpm run deepsec:scan:custom && $exportPipeline",
            ),
        )
        assertFalse(packageJson.contains("deepsec revalidate --project-id knittools --min-severity"))
    }

    @Test
    fun `deepsec accepted risk marker only covers documented ravelry credential findings`() {
        val marker = ProjectSourceFiles.read(".deepsec/scripts/mark-accepted-risks.mjs")

        assertTrue(marker.contains("config/security-decisions.md"))
        assertTrue(marker.contains("Ravelry embedded credentials"))
        assertTrue(marker.contains("accepted-risk"))
        assertTrue(marker.contains("RAVELRY_ACCEPTED_RISK_FILES"))
        assertTrue(marker.contains("secrets-exposure"))
        assertFalse(marker.contains("expensive-api-abuse"))
        assertFalse(marker.contains("prompt-injection"))
    }
}
