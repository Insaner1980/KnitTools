package com.finnvek.knittools.data.remote

import com.finnvek.knittools.auth.FirebaseAnonymousAuthGateway
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseRavelryBackendClientTest {
    @Test
    fun `malformed callable payload is rejected`() =
        runTest {
            val client = clientReturning("not-a-map")

            val error = runCatching { client.authStatus() }.exceptionOrNull()

            assertTrue(error is RavelryHttpException)
        }

    @Test
    fun `transient callable failures retain retryable status`() =
        runTest {
            listOf(
                "RESOURCE_EXHAUSTED" to 429,
                "UNAVAILABLE" to 503,
                "DEADLINE_EXCEEDED" to 504,
                "INTERNAL" to 500,
            ).forEach { (codeName, statusCode) ->
                val error = ravelryExceptionForFirebaseCodeName(codeName)

                assertTrue(error is TransientRavelryException)
                assertEquals(statusCode, (error as TransientRavelryException).statusCode)
            }
        }

    private fun clientReturning(data: Any?): FirebaseRavelryBackendClient =
        clientWithTask(
            successTask(
                HttpsCallableResult::class.java
                    .getDeclaredConstructor(Any::class.java)
                    .newInstance(data),
            ),
        )

    private fun clientWithTask(task: Task<HttpsCallableResult>): FirebaseRavelryBackendClient {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val authGateway = mockk<FirebaseAnonymousAuthGateway>()
        coEvery { authGateway.ensureSignedIn() } returns "uid"
        every { functions.getHttpsCallable(any()) } returns callable
        every { callable.call(any()) } returns task
        return FirebaseRavelryBackendClient(functions, authGateway)
    }

    private fun <T> successTask(value: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.exception } returns null
        every { task.result } returns value
        return task
    }
}
