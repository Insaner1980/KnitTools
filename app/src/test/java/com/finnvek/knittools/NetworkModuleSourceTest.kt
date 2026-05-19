package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModuleSourceTest {
    @Test
    fun `ktor okhttp client has complete request timeout`() {
        val networkModule = ProjectSourceFiles.read(NETWORK_MODULE)

        assertTrue(
            networkModule.contains("callTimeout(") ||
                networkModule.contains("requestTimeoutMillis"),
        )
    }

    private companion object {
        private const val NETWORK_MODULE =
            "app/src/main/java/com/finnvek/knittools/di/NetworkModule.kt"
    }
}
