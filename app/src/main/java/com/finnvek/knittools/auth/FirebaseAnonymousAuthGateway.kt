package com.finnvek.knittools.auth

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

class FirebaseAnonymousAuthException : RuntimeException("Firebase anonymous auth did not return a user.")

@Singleton
class FirebaseAnonymousAuthGateway
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth,
    ) {
        suspend fun ensureSignedIn(): String {
            firebaseAuth.currentUser?.let { return it.uid }
            val result = firebaseAuth.signInAnonymously().await()
            return result.user?.uid ?: throw FirebaseAnonymousAuthException()
        }
    }
