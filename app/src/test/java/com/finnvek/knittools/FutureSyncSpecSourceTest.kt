package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FutureSyncSpecSourceTest {
    @Test
    fun `future sync spec keeps v1 import separate from continuous sync`() {
        val specPath = ProjectSourceFiles.file(FUTURE_SYNC_SPEC)

        assertTrue("$FUTURE_SYNC_SPEC is missing", Files.exists(specPath))

        val spec = ProjectSourceFiles.read(specPath)
        assertTrue(spec.contains("Manual export/import or backup/restore comes before continuous sync."))
        assertTrue(
            spec.contains(
                "Do not market continuous cross-device sync until conflict handling and background sync exist.",
            ),
        )
        assertTrue(spec.contains("Pro gate"))
        assertTrue(spec.contains("conflict handling"))
        assertTrue(spec.contains("multi-device"))
        assertTrue(spec.contains("offline"))
        assertTrue(spec.contains("OAuth"))
        assertTrue(spec.contains("Google Drive"))
        assertTrue(spec.contains("`drive`"))
        assertTrue(spec.contains("`appDataFolder`"))
        assertTrue(spec.contains("Dropbox"))
        assertTrue(spec.contains("minimum scopes"))
        assertTrue(spec.contains("PKCE"))
    }

    @Test
    fun `architecture docs point future sync work to the spec`() {
        val agents = ProjectSourceFiles.read(AGENTS)
        val codex = ProjectSourceFiles.read(CODEX)

        listOf(agents, codex).forEach { doc ->
            assertTrue(doc.contains(FUTURE_SYNC_SPEC))
            assertTrue(doc.contains("Manual export/import or backup/restore comes before continuous sync."))
            assertTrue(doc.contains("Drive/Dropbox sync is future-spec work"))
        }
    }

    @Test
    fun `live strings do not promise continuous cross device sync`() {
        val forbiddenClaims =
            listOf(
                "continuous sync",
                "automatic sync",
                "cross-device sync",
                "multi-device sync",
                "synkronoi laitteiden välillä",
                "jatkuva synkronointi",
            )
        val offenders =
            ProjectSourceFiles.localizedStringFiles().filter { file ->
                val text = ProjectSourceFiles.read(file).lowercase()
                forbiddenClaims.any { claim -> text.contains(claim.lowercase()) }
            }

        assertTrue("Live UI strings promise future sync: $offenders", offenders.isEmpty())
    }

    @Test
    fun `future sync planning does not add provider dependencies yet`() {
        val buildText =
            listOf(ROOT_BUILD, SETTINGS, APP_BUILD, VERSION_CATALOG)
                .joinToString(separator = "\n") { ProjectSourceFiles.read(it).lowercase() }
        val forbidden =
            listOf(
                "com.dropbox",
                "dropbox-core-sdk",
                "google-api-services-drive",
                "google-auth-library",
                "com.google.api.services.drive",
            )
        val offenders = forbidden.filter(buildText::contains)

        assertTrue("Provider-specific dependencies were added: $offenders", offenders.isEmpty())
    }

    private companion object {
        const val FUTURE_SYNC_SPEC = "config/future-sync-spec.md"
        const val AGENTS = "AGENTS.md"
        const val CODEX = "CODEX.md"
        const val ROOT_BUILD = "build.gradle.kts"
        const val SETTINGS = "settings.gradle.kts"
        const val APP_BUILD = "app/build.gradle.kts"
        const val VERSION_CATALOG = "gradle/libs.versions.toml"
    }
}
