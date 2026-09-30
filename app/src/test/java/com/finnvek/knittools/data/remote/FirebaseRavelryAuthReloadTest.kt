package com.finnvek.knittools.data.remote

import android.util.SparseArray
import com.finnvek.knittools.auth.FirebaseAnonymousAuthException
import com.finnvek.knittools.auth.FirebaseAnonymousAuthGateway
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseRavelryAuthReloadTest {
    @Before
    fun mockAndroidExceptionCodeMap() {
        val values = mutableMapOf<Int, Any>()
        mockkConstructor(SparseArray::class)
        every { anyConstructed<SparseArray<Any>>().get(any()) } answers { values[firstArg<Int>()] }
        every { anyConstructed<SparseArray<Any>>().put(any(), any()) } answers {
            values[firstArg<Int>()] = secondArg()
        }
    }

    @After
    fun restoreAndroidExceptionCodeMap() {
        unmockkConstructor(SparseArray::class)
    }

    @Test
    fun `successful reload reaches the SDK callable without passing a cached uid`() =
        runTest {
            val fixture = Fixture()
            val call = async { fixture.client.authStatus() }
            runCurrent()
            fixture.verifyNoCallable()

            fixture.reload.finish()

            assertEquals(RavelryBackendAuthStatus(true, "test-user"), call.await())
            fixture.verifyExistingSessionCall()
        }

    @Test
    fun `network reload failure allows successful SDK callable for the same user`() =
        runTest {
            val fixture = Fixture()
            val call = async { fixture.client.authStatus() }
            runCurrent()
            fixture.verifyNoCallable()

            fixture.reload.finish(error = sdkError<FirebaseNetworkException>())

            assertEquals(RavelryBackendAuthStatus(true, "test-user"), call.await())
            fixture.verifyExistingSessionCall()
        }

    @Test
    fun `network reload fallback preserves callable status errors`() =
        runTest {
            listOf(
                FirebaseFunctionsException.Code.UNAVAILABLE to 503,
                FirebaseFunctionsException.Code.UNAUTHENTICATED to 401,
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED to 429,
            ).forEach { (code, status) ->
                val fixture = Fixture()
                val backendError = sdkError<FirebaseFunctionsException>()
                every { backendError.code } returns code
                fixture.backend.finish(error = backendError)
                fixture.reload.finish(error = sdkError<FirebaseNetworkException>())

                val error = runCatching { fixture.client.authStatus() }.exceptionOrNull()

                if (status == 401) {
                    assertEquals(status, (error as RavelryHttpException).statusCode)
                } else {
                    assertEquals(status, (error as TransientRavelryException).statusCode)
                }
                fixture.verifyExistingSessionCall()
            }
        }

    @Test
    fun `network reload fallback propagates SDK token acquisition failure`() =
        runTest {
            val fixture = Fixture()
            val tokenError = sdkError<FirebaseAuthInvalidUserException>()
            fixture.backend.finish(error = tokenError)
            fixture.reload.finish(error = sdkError<FirebaseNetworkException>())

            assertSame(tokenError, runCatching { fixture.client.authStatus() }.exceptionOrNull())
            fixture.verifyExistingSessionCall()
        }

    @Test
    fun `deleted disabled and invalid token users retain sign out and anonymous sign in`() =
        runTest {
            listOf(
                "ERROR_USER_NOT_FOUND",
                "ERROR_USER_DISABLED",
                "ERROR_INVALID_USER_TOKEN",
                "ERROR_USER_TOKEN_EXPIRED",
            ).forEach { code ->
                val fixture = Fixture()
                val invalidUser = sdkError<FirebaseAuthInvalidUserException>()
                every { invalidUser.errorCode } returns code
                fixture.reload.finish(error = invalidUser)

                assertTrue(fixture.client.authStatus().connected)
                assertSame(fixture.newUser, fixture.currentUser)
                verify(exactly = 1) { fixture.auth.signOut() }
                verify(exactly = 1) { fixture.auth.signInAnonymously() }
            }
        }

    @Test
    fun `SDK clearing the invalid user still permits the existing anonymous recovery`() =
        runTest {
            val fixture = Fixture()
            val call = async { fixture.client.authStatus() }
            runCurrent()
            fixture.currentUser = null
            fixture.reload.finish(error = sdkError<FirebaseAuthInvalidUserException>())

            assertTrue(call.await().connected)
            assertSame(fixture.newUser, fixture.currentUser)
            verify(exactly = 1) { fixture.auth.signInAnonymously() }
        }

    @Test
    fun `other reload errors propagate without a callable or new sign in`() =
        runTest {
            listOf(
                sdkError<FirebaseException>(),
                sdkError<FirebaseTooManyRequestsException>(),
                sdkError<FirebaseAuthInvalidCredentialsException>(),
                IOException("unclassified transport error"),
                IllegalStateException("unexpected error"),
            ).forEach { error ->
                val fixture = Fixture()
                fixture.reload.finish(error = error)

                assertSame(error, runCatching { fixture.client.authStatus() }.exceptionOrNull())
                fixture.verifyNoCallable()
                fixture.verifyNoSignInOrOut()
            }
        }

    @Test
    fun `sign out or user replacement during reload prevents the original call`() =
        runTest {
            listOf(false, true).forEach { networkFailure ->
                listOf(null, "other-uid", "existing-uid").forEach { replacementUid ->
                    val fixture = Fixture()
                    val call = async { runCatching { fixture.client.authStatus() }.exceptionOrNull() }
                    runCurrent()
                    fixture.currentUser = replacementUid?.let { uid -> mockk { every { this@mockk.uid } returns uid } }
                    fixture.reload.finish(error = if (networkFailure) sdkError<FirebaseNetworkException>() else null)

                    assertTrue(call.await() is FirebaseAnonymousAuthException)
                    fixture.verifyNoCallable()
                    fixture.verifyNoSignInOrOut()
                }
            }
        }

    @Test
    fun `invalid old user must not sign out a replacement user`() =
        runTest {
            val fixture = Fixture()
            val invalidUser = sdkError<FirebaseAuthInvalidUserException>()
            val call = async { runCatching { fixture.client.authStatus() }.exceptionOrNull() }
            runCurrent()
            fixture.currentUser = fixture.newUser
            fixture.reload.finish(error = invalidUser)

            assertSame(invalidUser, call.await())
            assertSame(fixture.newUser, fixture.currentUser)
            fixture.verifyNoCallable()
            fixture.verifyNoSignInOrOut()
        }

    @Test
    fun `cancellation while reload is pending prevents fallback and releases mutex`() =
        runTest {
            val fixture = Fixture()
            val call = async { fixture.client.authStatus() }
            runCurrent()
            call.cancelAndJoin()
            fixture.reload.finish(error = sdkError<FirebaseNetworkException>())
            runCurrent()

            assertTrue(call.isCancelled)
            fixture.verifyNoCallable()
            fixture.verifyNoSignInOrOut()
            assertTrue(fixture.client.authStatus().connected)
        }

    @Test
    fun `reload and callable cancellation remain cancellation`() =
        runTest {
            listOf(false, true).forEach { cancelBackend ->
                val fixture = Fixture()
                val cancellation = CancellationException("cancelled")
                if (cancelBackend) {
                    fixture.reload.finish(error = sdkError<FirebaseNetworkException>())
                    fixture.backend.finish(error = cancellation)
                } else {
                    fixture.reload.finish(error = cancellation)
                }

                assertTrue(runCatching { fixture.client.authStatus() }.exceptionOrNull() is CancellationException)
                if (!cancelBackend) fixture.verifyNoCallable()
                fixture.verifyNoSignInOrOut()
            }
        }

    @Test
    fun `missing current user signs in before the callable`() =
        runTest {
            val fixture = Fixture()
            fixture.currentUser = null

            assertTrue(fixture.client.authStatus().connected)
            assertSame(fixture.newUser, fixture.currentUser)
            verify(exactly = 1) { fixture.auth.signInAnonymously() }
            verify(exactly = 0) { fixture.user.reload() }
        }

    private class Fixture {
        val auth = mockk<FirebaseAuth>()
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val reload = ControlledTask<Void?>()
        val backend = ControlledTask<HttpsCallableResult>()
        val user = mockk<FirebaseUser>()
        val newUser = mockk<FirebaseUser>()
        var currentUser: FirebaseUser? = user
        val client = FirebaseRavelryBackendClient(functions, FirebaseAnonymousAuthGateway(auth))

        init {
            every { user.uid } returns "existing-uid"
            every { user.reload() } returns reload.task
            every { newUser.uid } returns "new-uid"
            every { auth.currentUser } answers { currentUser }
            every { auth.signOut() } answers { currentUser = null }
            val result = mockk<AuthResult> { every { this@mockk.user } returns newUser }
            val signIn = ControlledTask<AuthResult>().apply { finish(result) }
            every { auth.signInAnonymously() } answers {
                currentUser = newUser
                signIn.task
            }
            every { functions.getHttpsCallable("ravelryAuthStatus") } returns callable
            every { callable.call(emptyMap<String, Any?>()) } answers {
                // The SDK boundary is mocked; this does not validate a live ID token.
                assertTrue(currentUser != null)
                backend.task
            }
            backend.finish(
                HttpsCallableResult::class.java
                    .getDeclaredConstructor(Any::class.java)
                    .newInstance(mapOf("connected" to true, "username" to "test-user")),
            )
        }

        fun verifyExistingSessionCall() {
            verify(exactly = 1) { user.reload() }
            verify(exactly = 1) { callable.call(emptyMap<String, Any?>()) }
            verifyNoSignInOrOut()
        }

        fun verifyNoCallable() {
            verify(exactly = 0) { functions.getHttpsCallable(any<String>()) }
        }

        fun verifyNoSignInOrOut() {
            verify(exactly = 0) { auth.signOut() }
            verify(exactly = 0) { auth.signInAnonymously() }
        }
    }

    private class ControlledTask<T> {
        val task = mockk<Task<T>>()
        private var complete = false
        private var resultValue: T? = null
        private var failure: Exception? = null
        private val listeners = mutableListOf<OnCompleteListener<T>>()

        init {
            every { task.isComplete } answers { complete }
            every { task.isCanceled } returns false
            every { task.exception } answers { failure }
            every { task.result } answers { resultValue }
            every { task.addOnCompleteListener(any<Executor>(), any<OnCompleteListener<T>>()) } answers {
                listeners += secondArg<OnCompleteListener<T>>()
                task
            }
        }

        fun finish(
            value: T? = null,
            error: Exception? = null,
        ) {
            resultValue = value
            failure = error
            complete = true
            listeners.forEach { it.onComplete(task) }
        }
    }

    private companion object {
        private inline fun <reified T : Exception> sdkError(): T =
            mockk<T>(relaxed = true) {
                every { cause } returns null
                every { stackTrace } returns emptyArray()
            }
    }
}
