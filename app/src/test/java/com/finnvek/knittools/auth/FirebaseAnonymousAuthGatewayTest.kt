package com.finnvek.knittools.auth

import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAnonymousAuthGatewayTest {
    @Test
    fun `concurrent callers share in flight anonymous sign in`() =
        runTest {
            val firebaseAuth = mockk<FirebaseAuth>()
            val signInTask = pendingTask<AuthResult>()
            val user = firebaseUser("uid-1")
            val authResult = authResult(user)
            every { firebaseAuth.currentUser } answers {
                if (signInTask.isComplete) user else null
            }
            every { firebaseAuth.signInAnonymously() } returns signInTask.task
            val gateway = FirebaseAnonymousAuthGateway(firebaseAuth)

            val first = async { gateway.ensureSignedIn() }
            val second = async { gateway.ensureSignedIn() }
            runCurrent()

            verify(exactly = 1) { firebaseAuth.signInAnonymously() }

            signInTask.succeed(authResult)

            assertEquals("uid-1", first.await())
            assertEquals("uid-1", second.await())
        }

    @Test
    fun `concurrent callers receive shared sign in failure and later call can retry`() =
        runTest {
            val firebaseAuth = mockk<FirebaseAuth>()
            val failedTask = pendingTask<AuthResult>()
            val retryUser = firebaseUser("retry-uid")
            val failure = IllegalStateException("auth unavailable")
            every { firebaseAuth.currentUser } returns null
            every { firebaseAuth.signInAnonymously() } returns failedTask.task andThen authResultTask(retryUser)
            val gateway = FirebaseAnonymousAuthGateway(firebaseAuth)

            val first = async { runCatching { gateway.ensureSignedIn() }.exceptionOrNull() }
            val second = async { runCatching { gateway.ensureSignedIn() }.exceptionOrNull() }
            runCurrent()

            verify(exactly = 1) { firebaseAuth.signInAnonymously() }

            failedTask.fail(failure)

            assertEquals(failure::class, first.await()?.let { it::class })
            assertEquals(failure.message, second.await()?.message)
            assertEquals("retry-uid", gateway.ensureSignedIn())
            verify(exactly = 2) { firebaseAuth.signInAnonymously() }
        }

    @Test
    fun `wiped cached anonymous user signs out and creates a new anonymous user`() =
        runTest {
            val firebaseAuth = mockk<FirebaseAuth>()
            val staleUser = firebaseUser("stale-uid")
            val newUser = firebaseUser("new-uid")
            val invalidUser = mockk<FirebaseAuthInvalidUserException>(relaxed = true)
            every { invalidUser.cause } returns null
            every { invalidUser.stackTrace } returns emptyArray()
            every { staleUser.reload() } returns failureTask(invalidUser)
            every { firebaseAuth.currentUser } returns staleUser
            every { firebaseAuth.signOut() } just Runs
            every { firebaseAuth.signInAnonymously() } returns authResultTask(newUser)
            val gateway = FirebaseAnonymousAuthGateway(firebaseAuth)

            val uid = gateway.ensureSignedIn()

            assertEquals("new-uid", uid)
            verify(exactly = 1) { staleUser.reload() }
            verify(exactly = 1) { firebaseAuth.signOut() }
            verify(exactly = 1) { firebaseAuth.signInAnonymously() }
        }

    private fun firebaseUser(uid: String): FirebaseUser =
        mockk {
            every { this@mockk.uid } returns uid
            every { reload() } returns successTask(null)
        }

    private fun authResult(user: FirebaseUser): AuthResult =
        mockk {
            every { this@mockk.user } returns user
        }

    private fun authResultTask(user: FirebaseUser): Task<AuthResult> = successTask(authResult(user))

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    private fun <T> successTask(value: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<T>>().onSuccess(value)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        every { task.addOnCanceledListener(any()) } returns task
        return task
    }

    private fun <T> failureTask(error: Exception): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        // CPD-ON
        every { task.addOnCanceledListener(any()) } returns task
        return task
    }

    private fun <T> pendingTask(): PendingTask<T> = PendingTask()

    private class PendingTask<T> {
        private val successListeners = mutableListOf<OnSuccessListener<T>>()
        private val failureListeners = mutableListOf<OnFailureListener>()
        private val canceledListeners = mutableListOf<OnCanceledListener>()
        var isComplete = false
            private set

        // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
        val task: Task<T> = mockk()

        init {
            every { task.addOnSuccessListener(any()) } answers {
                successListeners += firstArg<OnSuccessListener<T>>()
                task
            }
            every { task.addOnFailureListener(any()) } answers {
                failureListeners += firstArg<OnFailureListener>()
                task
            }
            // CPD-ON
            every { task.addOnCanceledListener(any()) } answers {
                canceledListeners += firstArg<OnCanceledListener>()
                task
            }
        }

        fun succeed(value: T) {
            isComplete = true
            successListeners.forEach { it.onSuccess(value) }
        }

        fun fail(error: Exception) {
            isComplete = true
            failureListeners.forEach { it.onFailure(error) }
        }
    }
}
