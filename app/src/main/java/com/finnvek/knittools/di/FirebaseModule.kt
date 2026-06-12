package com.finnvek.knittools.di

import com.finnvek.knittools.data.remote.FirebaseRavelryBackendClient
import com.finnvek.knittools.data.remote.RavelryBackendClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("europe-west1")
}

@Module
@InstallIn(SingletonComponent::class)
interface FirebaseBindingsModule {
    @Binds
    @Singleton
    fun bindRavelryBackendClient(client: FirebaseRavelryBackendClient): RavelryBackendClient
}
