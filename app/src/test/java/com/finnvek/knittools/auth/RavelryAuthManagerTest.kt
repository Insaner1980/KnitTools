package com.finnvek.knittools.auth

import android.net.Uri
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.remote.PatternSearchParams
import com.finnvek.knittools.data.remote.PatternSearchResponse
import com.finnvek.knittools.data.remote.RavelryBackendAuthStatus
import com.finnvek.knittools.data.remote.RavelryBackendClient
import com.finnvek.knittools.data.remote.RavelryBackendCurrentUser
import com.finnvek.knittools.data.remote.RavelryStartAuthResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryAuthManagerTest {
    @Test
    fun `refresh status exposes connected username from backend`() =
        runTest {
            val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "knitter"))
            val manager = RavelryAuthManager(backend)

            manager.refreshAuthStatus()

            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
        }

    @Test
    fun `start auth stores pending state and returns backend authorize uri`() =
        runTest {
            val backend = FakeRavelryBackendClient()
            val manager = RavelryAuthManager(backend)

            withParsedUri(AUTHORIZE_URL) { expectedUri ->
                val uri = manager.startAuth()

                assertSame(expectedUri, uri)
            }

            assertEquals(RavelryAuthState.AwaitingBrowser, manager.authState.value)
        }

    @Test
    fun `callback refreshes backend status when state matches pending auth`() =
        runTest {
            val backend =
                FakeRavelryBackendClient(
                    authStatus = RavelryBackendAuthStatus(true, "knitter"),
                )
            val manager = RavelryAuthManager(backend)
            withParsedUri(AUTHORIZE_URL) {
                manager.startAuth()
            }

            val handled = manager.handleCallback(callbackUri(state = "state-1"))

            assertTrue(handled)
            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
        }

    @Test
    fun `callback ignores missing or mismatched state before changing auth state`() =
        runTest {
            val noPendingBackend = FakeRavelryBackendClient()
            val noPendingManager = RavelryAuthManager(noPendingBackend)

            val noStateHandled = noPendingManager.handleCallback(callbackUri())

            assertTrue(noStateHandled)
            assertEquals(RavelryAuthState.NotConnected, noPendingManager.authState.value)
            assertEquals(0, noPendingBackend.authStatusCalls)

            val pendingBackend = FakeRavelryBackendClient()
            val pendingManager = RavelryAuthManager(pendingBackend)
            withParsedUri(AUTHORIZE_URL) {
                pendingManager.startAuth()
            }

            val wrongStateHandled =
                pendingManager.handleCallback(
                    callbackUri(
                        state = "attacker-state",
                        error = "access_denied",
                    ),
                )

            assertTrue(wrongStateHandled)
            assertEquals(RavelryAuthState.AwaitingBrowser, pendingManager.authState.value)
            assertEquals(0, pendingBackend.authStatusCalls)
        }

    @Test
    fun `callback refreshes backend status when pending state was lost after process recreation`() =
        runTest {
            val backend =
                FakeRavelryBackendClient(
                    authStatus = RavelryBackendAuthStatus(true, "knitter"),
                )
            val manager = RavelryAuthManager(backend)

            val handled = manager.handleCallback(callbackUri(state = "state-after-recreation"))

            assertTrue(handled)
            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
            assertEquals(1, backend.authStatusCalls)
        }

    @Test
    fun `disconnect calls backend and exposes not connected`() =
        runTest {
            val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "knitter"))
            val manager = RavelryAuthManager(backend)
            manager.refreshAuthStatus()

            manager.disconnect()

            assertEquals(1, backend.disconnectCalls)
            assertEquals(RavelryAuthState.NotConnected, manager.authState.value)
        }

    @Test
    fun `browser cancellation exposes cancelled state while auth is pending`() =
        runTest {
            val backend = FakeRavelryBackendClient()
            val manager = RavelryAuthManager(backend)
            withParsedUri(AUTHORIZE_URL) {
                manager.startAuth()
            }

            manager.markBrowserAuthCancelled()

            assertEquals(RavelryAuthState.Cancelled, manager.authState.value)
        }

    private suspend fun <T> withParsedUri(
        rawUri: String,
        block: suspend (Uri) -> T,
    ): T {
        mockkStatic(Uri::class)
        val uri = mockk<Uri>()
        every { Uri.parse(rawUri) } returns uri
        return try {
            block(uri)
        } finally {
            unmockkStatic(Uri::class)
        }
    }

    private fun callbackUri(
        state: String? = null,
        error: String? = null,
        status: String? = null,
    ): Uri =
        mockk {
            every { scheme } returns RavelryAuthManager.REDIRECT_SCHEME
            every { host } returns RavelryAuthManager.REDIRECT_HOST
            every { getQueryParameter("state") } returns state
            every { getQueryParameter("error") } returns error
            every { getQueryParameter("status") } returns status
        }

    private class FakeRavelryBackendClient(
        private var authStatus: RavelryBackendAuthStatus = RavelryBackendAuthStatus(false),
    ) : RavelryBackendClient {
        var disconnectCalls = 0
        var authStatusCalls = 0

        override suspend fun startAuth(): RavelryStartAuthResponse =
            RavelryStartAuthResponse(
                authorizeUrl = "https://www.ravelry.com/oauth2/auth",
                state = "state-1",
                expiresAtMillis = 123L,
            )

        override suspend fun authStatus(): RavelryBackendAuthStatus {
            authStatusCalls += 1
            return authStatus
        }

        override suspend fun disconnect() {
            disconnectCalls += 1
            authStatus = RavelryBackendAuthStatus(false)
        }

        override suspend fun currentUser(): RavelryBackendCurrentUser = RavelryBackendCurrentUser(false)

        override suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse = error("not used")

        override suspend fun importPatternById(ravelryPatternId: Int): PatternDetail = error("not used")

        override suspend fun importPatternByUrl(url: String): PatternDetail = error("not used")
    }

    private companion object {
        private const val AUTHORIZE_URL = "https://www.ravelry.com/oauth2/auth"
    }
}
