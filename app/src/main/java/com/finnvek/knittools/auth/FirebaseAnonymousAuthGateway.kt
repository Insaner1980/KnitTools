package com.finnvek.knittools.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class FirebaseAnonymousAuthException : RuntimeException("Firebase anonymous auth did not return a user.")

@Singleton
class FirebaseAnonymousAuthGateway
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth,
    ) {
        private val authMutex = Mutex()
        private var inFlightSignIn: Task<AuthResult>? = null

        suspend fun ensureSignedIn(): String {
            val authState =
                authMutex.withLock {
                    val currentUid = currentUserUidOrNull()
                    if (currentUid != null) {
                        AuthState.SignedIn(currentUid)
                    } else {
                        AuthState.SigningIn(
                            inFlightSignIn ?: firebaseAuth.signInAnonymously().also { inFlightSignIn = it },
                        )
                    }
                }
            return when (authState) {
                is AuthState.SignedIn -> authState.uid
                is AuthState.SigningIn -> authState.task.awaitSignIn()
            }
        }

        private suspend fun currentUserUidOrNull(): String? {
            val user = firebaseAuth.currentUser ?: return null
            return try {
                user.reload().await()
                firebaseAuth.currentUser?.uid ?: user.uid
            } catch (_: FirebaseAuthInvalidUserException) {
                firebaseAuth.signOut()
                null
            }
        }

        private suspend fun Task<AuthResult>.awaitSignIn(): String {
            val result =
                try {
                    await()
                } finally {
                    withContext(NonCancellable) {
                        clearInFlightSignIn(this@awaitSignIn)
                    }
                }
            return result.user?.uid ?: throw FirebaseAnonymousAuthException()
        }

        private suspend fun clearInFlightSignIn(task: Task<AuthResult>) {
            authMutex.withLock {
                if (inFlightSignIn === task) {
                    inFlightSignIn = null
                }
            }
        }

        private sealed interface AuthState {
            data class SignedIn(
                val uid: String,
            ) : AuthState

            data class SigningIn(
                val task: Task<AuthResult>,
            ) : AuthState
        }
    }
