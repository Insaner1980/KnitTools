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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
            assertEquals(listOf("state-1" to "proof-1"), backend.completeAuthCalls)
        }

    @Test
    fun `callback ignores missing or mismatched state before changing auth state`() =
        runTest {
            val noPendingBackend = FakeRavelryBackendClient()
            val noPendingManager = RavelryAuthManager(noPendingBackend)

            val noStateHandled = noPendingManager.handleCallback(callbackUri())

            assertFalse(noStateHandled)
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
    fun `callback accepts only the exact token free redirect shape`() {
        val manager = RavelryAuthManager(FakeRavelryBackendClient())

        assertTrue(manager.isOAuthCallback(callbackUri(state = "state-1")))
        assertTrue(manager.isOAuthCallback(callbackUri(state = "state-1", error = "access_denied")))

        val invalidCallbacks =
            listOf(
                callbackUri(state = "state-1", uriScheme = "https"),
                callbackUri(state = "state-1", uriHost = "other"),
                callbackUri(state = "state-1", uriAuthority = "user@ravelry-auth-complete"),
                callbackUri(state = "state-1", uriPath = "/unexpected"),
                callbackUri(state = "state-1", uriFragment = "fragment"),
                callbackUri(),
                callbackUri(queryValues = mapOf("state" to listOf("one", "two"))),
                callbackUri(
                    queryValues =
                        mapOf(
                            "state" to listOf("state-1"),
                            "unexpected" to listOf("value"),
                        ),
                ),
                callbackUri(
                    queryValues =
                        mapOf(
                            "state" to listOf("state-1"),
                            "error" to listOf(""),
                        ),
                ),
            )

        invalidCallbacks.forEach { uri -> assertFalse(manager.isOAuthCallback(uri)) }
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

    @Test
    fun `late auth start response cannot overwrite completed disconnect`() =
        runTest {
            val startResponse = CompletableDeferred<RavelryStartAuthResponse>()
            val manager = RavelryAuthManager(FakeRavelryBackendClient(startAuthResponse = startResponse))

            val startResult = async { manager.startAuth() }
            yield()
            assertEquals(RavelryAuthState.Starting, manager.authState.value)

            manager.disconnect()
            startResponse.complete(
                RavelryStartAuthResponse(
                    authorizeUrl = AUTHORIZE_URL,
                    state = "late-state",
                    expiresAtMillis = 123L,
                ),
            )

            assertEquals(null, startResult.await())
            assertEquals(RavelryAuthState.NotConnected, manager.authState.value)
        }

    @Test
    fun `late status response cannot overwrite completed disconnect`() =
        runTest {
            val statusResponse = CompletableDeferred<RavelryBackendAuthStatus>()
            val manager = RavelryAuthManager(FakeRavelryBackendClient(authStatusResponse = statusResponse))

            val refreshResult = async { manager.refreshAuthStatus() }
            yield()

            manager.disconnect()
            statusResponse.complete(RavelryBackendAuthStatus(true, "stale-user"))

            assertEquals(RavelryAuthState.NotConnected, refreshResult.await())
            assertEquals(RavelryAuthState.NotConnected, manager.authState.value)
        }

    @Test
    fun `late completion preserves a newer pending sign in`() =
        runTest {
            for (fails in listOf(false, true)) {
                val completion = CompletableDeferred<Unit>()
                val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "new-user"))
                val manager = backend.managerWithPendingCompletion(completion, state = "state-1")
                val states = mutableListOf<RavelryAuthState>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { manager.authState.toList(states) }

                withParsedUri(AUTHORIZE_URL) {
                    manager.startAuth()
                    val callback =
                        async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
                    backend.startAuthState = "state-2"
                    manager.startAuth()
                    val beforeLateResult = states.toList()

                    if (fails) {
                        completion.completeExceptionally(
                            IllegalStateException("synthetic failure"),
                        )
                    } else {
                        completion.complete(Unit)
                    }
                    assertTrue(callback.await())

                    assertEquals(beforeLateResult, states)
                    assertEquals(0, backend.authStatusCalls)
                    manager.handleCallback(callbackUri("unrelated-state"))
                    assertEquals(listOf("state-1"), backend.completeAuthCalls.map { it.first })
                    manager.handleCallback(callbackUri("state-2"))
                    assertEquals(listOf("state-1", "state-2"), backend.completeAuthCalls.map { it.first })
                    assertEquals(RavelryAuthState.Connected("new-user"), manager.authState.value)
                }
            }
        }

    @Test
    fun `late completion cannot suppress a newer browser launch`() =
        runTest {
            for (fails in listOf(false, true)) {
                val completion = CompletableDeferred<Unit>()
                val startResponse = CompletableDeferred<RavelryStartAuthResponse>()
                val backend = FakeRavelryBackendClient()
                val manager = backend.managerWithPendingCompletion(completion)

                withParsedUri(AUTHORIZE_URL) { expectedUri ->
                    manager.startAuth()
                    val callback =
                        async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
                    backend.startAuthResponse = startResponse
                    val start = async(start = CoroutineStart.UNDISPATCHED) { manager.startAuth() }

                    if (fails) {
                        completion.completeExceptionally(
                            IllegalStateException("synthetic failure"),
                        )
                    } else {
                        completion.complete(Unit)
                    }
                    callback.await()
                    assertEquals(RavelryAuthState.Starting, manager.authState.value)
                    assertEquals(RavelryAuthState.Starting, manager.refreshAuthStatus())
                    assertEquals(0, backend.authStatusCalls)

                    startResponse.complete(RavelryStartAuthResponse(AUTHORIZE_URL, "state-2", 123L))
                    assertSame(expectedUri, start.await())
                    assertEquals(RavelryAuthState.AwaitingBrowser, manager.authState.value)
                }
            }
        }

    @Test
    fun `disconnect owns state before and after a late completion response`() =
        runTest {
            for (fails in listOf(false, true)) {
                for (disconnectPending in listOf(false, true)) {
                    val completion = CompletableDeferred<Unit>()
                    val disconnectResponse = CompletableDeferred<Unit>()
                    val backend = FakeRavelryBackendClient()
                    backend.onCompleteAuth = { completion.await() }
                    if (disconnectPending) backend.disconnectResponse = disconnectResponse
                    val manager = RavelryAuthManager(backend)
                    val states = mutableListOf<RavelryAuthState>()
                    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { manager.authState.toList(states) }

                    withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
                    val callback =
                        async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
                    val disconnect = async(start = CoroutineStart.UNDISPATCHED) { manager.disconnect() }
                    val beforeLateResult = states.toList()
                    if (disconnectPending) {
                        assertEquals(RavelryAuthState.Disconnecting, manager.refreshAuthStatus())
                    }

                    if (fails) {
                        completion.completeExceptionally(
                            IllegalStateException("synthetic failure"),
                        )
                    } else {
                        completion.complete(Unit)
                    }
                    callback.await()
                    assertEquals(beforeLateResult, states)
                    assertEquals(0, backend.authStatusCalls)
                    disconnectResponse.complete(Unit)
                    assertEquals(RavelryAuthState.NotConnected, disconnect.await())
                    assertEquals(RavelryAuthState.NotConnected, manager.authState.value)
                }
            }
        }

    @Test
    fun `browser cancellation owns state after a late completion response`() =
        runTest {
            for (fails in listOf(false, true)) {
                val completion = CompletableDeferred<Unit>()
                val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "old-user"))
                val manager = backend.managerWithPendingCompletion(completion)
                withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
                val callback =
                    async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }

                manager.markBrowserAuthCancelled()
                if (fails) {
                    completion.completeExceptionally(
                        IllegalStateException("synthetic failure"),
                    )
                } else {
                    completion.complete(Unit)
                }
                callback.await()

                assertEquals(RavelryAuthState.Cancelled, manager.authState.value)
                assertEquals(0, backend.authStatusCalls)
                manager.handleCallback(callbackUri("state-1"))
                assertEquals(1, backend.completeAuthCalls.size)
            }
        }

    @Test
    fun `completion supersedes older status and ignores passive refresh until resolved`() =
        runTest {
            val completion = CompletableDeferred<Unit>()
            val oldStatus = CompletableDeferred<RavelryBackendAuthStatus>()
            val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "knitter"))
            val manager = backend.managerWithPendingCompletion(completion)
            val states = mutableListOf<RavelryAuthState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { manager.authState.toList(states) }
            withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
            backend.authStatusResponse = oldStatus
            val refresh = async(start = CoroutineStart.UNDISPATCHED) { manager.refreshAuthStatus() }
            val callback = async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }

            val passiveRefresh = async(start = CoroutineStart.UNDISPATCHED) { manager.refreshAuthStatus() }
            assertTrue(passiveRefresh.isCompleted)
            assertEquals(RavelryAuthState.AwaitingBrowser, passiveRefresh.await())
            assertEquals(1, backend.authStatusCalls)
            oldStatus.complete(RavelryBackendAuthStatus(false))
            assertEquals(RavelryAuthState.AwaitingBrowser, refresh.await())
            backend.authStatusResponse = null
            completion.complete(Unit)
            callback.await()

            assertEquals(2, backend.authStatusCalls)
            assertEquals(
                listOf(
                    RavelryAuthState.NotConnected,
                    RavelryAuthState.Starting,
                    RavelryAuthState.AwaitingBrowser,
                    RavelryAuthState.Connected("knitter"),
                ),
                states,
            )
        }

    @Test
    fun `callbacks first delivered during a newer start cannot claim its operation`() =
        runTest {
            val response = CompletableDeferred<RavelryStartAuthResponse>()
            val backend = FakeRavelryBackendClient()
            val manager = RavelryAuthManager(backend)
            withParsedUri(AUTHORIZE_URL) { expectedUri ->
                manager.startAuth()
                backend.startAuthResponse = response
                val start = async(start = CoroutineStart.UNDISPATCHED) { manager.startAuth() }

                manager.handleCallback(callbackUri("state-1"))
                manager.handleCallback(callbackUri("state-1", error = "access_denied"))
                assertEquals(RavelryAuthState.Starting, manager.authState.value)
                assertTrue(backend.completeAuthCalls.isEmpty())
                assertEquals(0, backend.authStatusCalls)

                response.complete(RavelryStartAuthResponse(AUTHORIZE_URL, "state-2", 123L))
                assertSame(expectedUri, start.await())
            }
        }

    @Test
    fun `missing pending after disconnect is not treated as process recovery`() =
        runTest {
            val backend = FakeRavelryBackendClient()
            val manager = RavelryAuthManager(backend)
            manager.disconnect()
            manager.refreshAuthStatus()

            manager.handleCallback(callbackUri("old-state"))
            manager.handleCallback(callbackUri("old-state", error = "access_denied"))

            assertEquals(RavelryAuthState.NotConnected, manager.authState.value)
            assertTrue(backend.completeAuthCalls.isEmpty())
            assertEquals(1, backend.authStatusCalls)
        }

    @Test
    fun `duplicate callbacks do not repeat activation or status publication`() =
        runTest {
            val completion = CompletableDeferred<Unit>()
            val status = CompletableDeferred<RavelryBackendAuthStatus>()
            val backend = FakeRavelryBackendClient(authStatusResponse = status)
            val manager = backend.managerWithPendingCompletion(completion)
            withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
            val callback = async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }

            val duplicate =
                async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
            assertTrue(duplicate.isCompleted)
            assertTrue(duplicate.await())
            assertEquals(1, backend.completeAuthCalls.size)
            assertEquals(0, backend.authStatusCalls)
            completion.complete(Unit)
            runCurrent()
            val activatedDuplicate =
                async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
            assertTrue(activatedDuplicate.isCompleted)
            assertTrue(activatedDuplicate.await())
            assertEquals(1, backend.completeAuthCalls.size)
            assertEquals(1, backend.authStatusCalls)
            val passiveRefresh = async(start = CoroutineStart.UNDISPATCHED) { manager.refreshAuthStatus() }
            assertTrue(passiveRefresh.isCompleted)
            assertEquals(RavelryAuthState.AwaitingBrowser, passiveRefresh.await())
            assertEquals(1, backend.authStatusCalls)

            status.complete(RavelryBackendAuthStatus(true, "knitter"))
            callback.await()
            assertTrue(manager.handleCallback(callbackUri("state-1")))
            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
            assertEquals(1, backend.completeAuthCalls.size)
            assertEquals(1, backend.authStatusCalls)
        }

    @Test
    fun `late failure cannot overwrite a newer successful completion`() =
        runTest {
            val completion = CompletableDeferred<Unit>()
            val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "new-user"))
            val manager = backend.managerWithPendingCompletion(completion, state = "state-1")
            withParsedUri(AUTHORIZE_URL) {
                manager.startAuth()
                val callback =
                    async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
                backend.startAuthState = "state-2"
                manager.startAuth()
                manager.handleCallback(callbackUri("state-2"))
                completion.completeExceptionally(IllegalStateException("synthetic failure"))
                callback.await()

                assertEquals(RavelryAuthState.Connected("new-user"), manager.authState.value)
                assertEquals(1, backend.authStatusCalls)
            }
        }

    @Test
    fun `cancelled completion releases its claim so callback can be retried`() =
        runTest {
            val completion = CompletableDeferred<Unit>()
            val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "knitter"))
            val manager = backend.managerWithPendingCompletion(completion)
            withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
            val callback = async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
            callback.cancelAndJoin()

            assertEquals(RavelryAuthState.AwaitingBrowser, manager.authState.value)
            assertEquals(0, backend.authStatusCalls)
            backend.onCompleteAuth = {}
            manager.handleCallback(callbackUri("wrong-state"))
            assertEquals(1, backend.completeAuthCalls.size)
            manager.handleCallback(callbackUri("state-1"))

            assertEquals(2, backend.completeAuthCalls.size)
            assertEquals(1, backend.authStatusCalls)
            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
        }

    @Test
    fun `cancelled status after activation retries status without reactivating callback`() =
        runTest {
            val status = CompletableDeferred<RavelryBackendAuthStatus>()
            val backend =
                FakeRavelryBackendClient(
                    authStatus = RavelryBackendAuthStatus(true, "knitter"),
                    authStatusResponse = status,
                )
            val manager = RavelryAuthManager(backend)
            withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
            val callback = async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
            assertEquals(1, backend.completeAuthCalls.size)
            assertEquals(1, backend.authStatusCalls)
            callback.cancelAndJoin()
            backend.authStatusResponse = null
            manager.handleCallback(callbackUri("state-1"))

            assertEquals(1, backend.completeAuthCalls.size)
            assertEquals(2, backend.authStatusCalls)
            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
        }

    @Test
    fun `old callback cancellation cannot release a newer completion claim`() =
        runTest {
            val firstCompletion = CompletableDeferred<Unit>()
            val secondCompletion = CompletableDeferred<Unit>()
            val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "new-user"))
            backend.onCompleteAuth =
                { state -> if (state == "state-1") firstCompletion.await() else secondCompletion.await() }
            val manager = RavelryAuthManager(backend)
            withParsedUri(AUTHORIZE_URL) {
                manager.startAuth()
                val first =
                    async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
                backend.startAuthState = "state-2"
                manager.startAuth()
                val second =
                    async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-2")) }
                first.cancelAndJoin()

                assertEquals(RavelryAuthState.AwaitingBrowser, manager.refreshAuthStatus())
                assertEquals(0, backend.authStatusCalls)
                manager.handleCallback(callbackUri("state-2"))
                assertEquals(2, backend.completeAuthCalls.size)
                secondCompletion.complete(Unit)
                second.await()
                assertEquals(RavelryAuthState.Connected("new-user"), manager.authState.value)
            }
        }

    @Test
    fun `recovered callback after passive status reserves its state until completion`() =
        runTest {
            val completion = CompletableDeferred<Unit>()
            val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "knitter"))
            val manager = RavelryAuthManager(backend)
            manager.refreshAuthStatus()
            backend.onCompleteAuth = { completion.await() }
            val callback =
                async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("recovered-state")) }

            manager.handleCallback(callbackUri("unrelated-state", error = "access_denied"))
            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
            assertEquals(RavelryAuthState.Connected("knitter"), manager.refreshAuthStatus())
            assertEquals(1, backend.authStatusCalls)
            completion.complete(Unit)
            callback.await()
            assertEquals(1, backend.completeAuthCalls.size)
            assertEquals(2, backend.authStatusCalls)
            assertEquals(RavelryAuthState.Connected("knitter"), manager.authState.value)
        }

    @Test
    fun `cancelled start and disconnect release passive status refresh`() =
        runTest {
            for (disconnect in listOf(false, true)) {
                val backend = FakeRavelryBackendClient(authStatus = RavelryBackendAuthStatus(true, "knitter"))
                backend.startAuthResponse = CompletableDeferred()
                backend.disconnectResponse = CompletableDeferred()
                val manager = RavelryAuthManager(backend)
                val operation =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        if (disconnect) manager.disconnect() else manager.startAuth()
                    }
                operation.cancelAndJoin()

                assertEquals(RavelryAuthState.Connected("knitter"), manager.refreshAuthStatus())
                assertEquals(1, backend.authStatusCalls)
            }
        }

    @Test
    fun `late completion status cannot overwrite disconnect`() =
        runTest {
            for (fails in listOf(false, true)) {
                val status = CompletableDeferred<RavelryBackendAuthStatus>()
                val backend = FakeRavelryBackendClient(authStatusResponse = status)
                val manager = RavelryAuthManager(backend)
                withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
                val callback =
                    async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }
                assertEquals(1, backend.authStatusCalls)

                manager.disconnect()
                if (fails) {
                    status.completeExceptionally(IllegalStateException("synthetic failure"))
                } else {
                    status.complete(RavelryBackendAuthStatus(true, "old-user"))
                }
                callback.await()

                assertEquals(RavelryAuthState.NotConnected, manager.authState.value)
                assertEquals(1, backend.authStatusCalls)
                assertEquals(1, backend.completeAuthCalls.size)
            }
        }

    @Test
    fun `callback error invalidates suspended completion`() =
        runTest {
            for ((error, expectedState) in listOf(
                "access_denied" to RavelryAuthState.Cancelled,
                "state_expired" to RavelryAuthState.Expired,
            )) {
                val completion = CompletableDeferred<Unit>()
                val backend = FakeRavelryBackendClient()
                val manager = backend.managerWithPendingCompletion(completion)
                withParsedUri(AUTHORIZE_URL) { manager.startAuth() }
                val callback =
                    async(start = CoroutineStart.UNDISPATCHED) { manager.handleCallback(callbackUri("state-1")) }

                manager.handleCallback(callbackUri("state-1", error = error))
                assertEquals(expectedState, manager.authState.value)
                completion.complete(Unit)
                callback.await()

                assertEquals(expectedState, manager.authState.value)
                assertEquals(0, backend.authStatusCalls)
            }
        }

    @Test
    fun `current completion failure publishes error and releases status refresh`() =
        runTest {
            val backend = FakeRavelryBackendClient()
            backend.onCompleteAuth = { throw IllegalStateException("synthetic failure") }
            val manager = RavelryAuthManager(backend)
            withParsedUri(AUTHORIZE_URL) { manager.startAuth() }

            assertTrue(manager.handleCallback(callbackUri("state-1")))
            assertEquals(RavelryAuthState.BackendUnavailable, manager.authState.value)
            assertEquals(0, backend.authStatusCalls)
            assertEquals(RavelryAuthState.NotConnected, manager.refreshAuthStatus())
            assertEquals(1, backend.authStatusCalls)
        }
}

private fun FakeRavelryBackendClient.managerWithPendingCompletion(
    completion: CompletableDeferred<Unit>,
    state: String? = null,
): RavelryAuthManager {
    onCompleteAuth = { incomingState ->
        if (state == null || incomingState == state) completion.await()
    }
    return RavelryAuthManager(this)
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
    proof: String? = if (state != null && error == null) "proof-1" else null,
    uriScheme: String = RavelryAuthManager.REDIRECT_SCHEME,
    uriHost: String = RavelryAuthManager.REDIRECT_HOST,
    uriAuthority: String = uriHost,
    uriPath: String = "",
    uriFragment: String? = null,
    queryValues: Map<String, List<String>> =
        buildMap {
            state?.let { put("state", listOf(it)) }
            error?.let { put("error", listOf(it)) }
            proof?.let { put("proof", listOf(it)) }
        },
): Uri {
    val uri = mockk<Uri>()
    every { uri.scheme } returns uriScheme
    every { uri.host } returns uriHost
    every { uri.encodedAuthority } returns uriAuthority
    every { uri.path } returns uriPath
    every { uri.fragment } returns uriFragment
    every { uri.queryParameterNames } returns queryValues.keys
    every { uri.getQueryParameters(any()) } answers {
        queryValues[firstArg()].orEmpty()
    }
    every { uri.getQueryParameter(any()) } answers {
        queryValues[firstArg()]?.firstOrNull()
    }
    return uri
}

private class FakeRavelryBackendClient(
    private var authStatus: RavelryBackendAuthStatus = RavelryBackendAuthStatus(false),
    var startAuthResponse: CompletableDeferred<RavelryStartAuthResponse>? = null,
    var authStatusResponse: CompletableDeferred<RavelryBackendAuthStatus>? = null,
) : RavelryBackendClient {
    var startAuthState = "state-1"
    var onCompleteAuth: suspend (String) -> Unit = {}
    var disconnectResponse: CompletableDeferred<Unit>? = null
    var disconnectCalls = 0
    var authStatusCalls = 0
    val completeAuthCalls = mutableListOf<Pair<String, String>>()

    override suspend fun startAuth(): RavelryStartAuthResponse =
        startAuthResponse?.await()
            ?: RavelryStartAuthResponse(
                authorizeUrl = AUTHORIZE_URL,
                state = startAuthState,
                expiresAtMillis = 123L,
            )

    override suspend fun authStatus(): RavelryBackendAuthStatus {
        authStatusCalls += 1
        return authStatusResponse?.await() ?: authStatus
    }

    override suspend fun completeAuth(
        state: String,
        proof: String,
    ) {
        completeAuthCalls += state to proof
        onCompleteAuth(state)
    }

    override suspend fun disconnect() {
        disconnectCalls += 1
        disconnectResponse?.await()
        authStatus = RavelryBackendAuthStatus(false)
    }

    override suspend fun currentUser(): RavelryBackendCurrentUser = RavelryBackendCurrentUser(false)

    override suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse = error("not used")

    override suspend fun importPatternById(ravelryPatternId: Int): PatternDetail = error("not used")

    override suspend fun importPatternByUrl(url: String): PatternDetail = error("not used")
}

private const val AUTHORIZE_URL = "https://www.ravelry.com/oauth2/auth"
