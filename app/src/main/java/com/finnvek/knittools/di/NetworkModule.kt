package com.finnvek.knittools.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val CALL_TIMEOUT_SECONDS = 45L
    private const val READ_WRITE_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient =
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    readTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    writeTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
}
