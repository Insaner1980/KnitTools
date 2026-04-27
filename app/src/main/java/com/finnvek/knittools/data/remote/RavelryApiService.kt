package com.finnvek.knittools.data.remote

import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.auth.RavelryAuthManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class PatternSearchParams(
    val query: String,
    val craft: String? = "knitting",
    val availability: String? = null,
    val pc: String? = null,
    val weight: String? = null,
    val difficultyFrom: Int? = null,
    val difficultyTo: Int? = null,
    val page: Int = 1,
    val pageSize: Int = 20,
)

@Singleton
class RavelryApiService
    @Inject
    constructor(
        private val client: HttpClient,
        private val authManager: RavelryAuthManager,
    ) {
        companion object {
            private const val BASE_URL = "https://api.ravelry.com"
            private const val MAX_TRANSIENT_RETRIES = 2
            private const val INITIAL_RETRY_DELAY_MS = 500L
        }

        suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse =
            authenticatedGet("$BASE_URL/patterns/search.json") {
                parameter("query", params.query)
                params.craft?.let { parameter("craft", it) }
                params.availability?.let { parameter("availability", it) }
                params.pc?.let { parameter("pc", it) }
                params.weight?.let { parameter("weight", it) }
                params.difficultyFrom?.let { parameter("diff_from", it) }
                params.difficultyTo?.let { parameter("diff_to", it) }
                parameter("page", params.page)
                parameter("page_size", params.pageSize)
                parameter("sort", "best")
            }

        suspend fun getPatternDetail(id: Int): PatternDetail =
            authenticatedGet<PatternDetailResponse>("$BASE_URL/patterns/$id.json")
                .pattern

        /**
         * GET-pyyntö autentikointi- ja retry-logiikalla:
         * 1. Bearer-token jos saatavilla
         * 2. Jos 401/403 → refresh token
         * 3. Jos refresh epäonnistuu → fallback Basic Auth
         * 4. Transientit verkkovirheet ja 5xx-vastaukset yritetään rajatusti uudelleen
         */
        private suspend inline fun <reified T> authenticatedGet(
            url: String,
            crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
        ): T {
            var attempt = 0
            var retryDelayMs = INITIAL_RETRY_DELAY_MS

            while (true) {
                try {
                    return authenticatedGetOnce(url, block)
                } catch (e: IOException) {
                    if (attempt >= MAX_TRANSIENT_RETRIES) {
                        throw e
                    }
                }

                delay(retryDelayMs)
                retryDelayMs *= 2
                attempt++
            }
        }

        private suspend inline fun <reified T> authenticatedGetOnce(
            url: String,
            crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
        ): T {
            val token = authManager.accessToken

            // 1. Yritä Bearer-tokenilla jos saatavilla
            if (token != null) {
                val response =
                    client.get(url) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        block()
                    }

                response.throwIfTransientServerError()

                if (response.status != HttpStatusCode.Unauthorized &&
                    response.status != HttpStatusCode.Forbidden
                ) {
                    return response.body()
                }

                // 2. Token vanhentunut → yritä refreshata
                if (authManager.refreshAccessToken(client)) {
                    val retryResponse =
                        client.get(url) {
                            header(HttpHeaders.Authorization, "Bearer ${authManager.accessToken}")
                            block()
                        }
                    retryResponse.throwIfTransientServerError()
                    if (retryResponse.status != HttpStatusCode.Unauthorized &&
                        retryResponse.status != HttpStatusCode.Forbidden
                    ) {
                        return retryResponse.body()
                    }
                }

                // 3. Refresh epäonnistui → kirjaa ulos ja fallback Basic Auth:iin
                authManager.signOut()
            }

            // Basic Auth -fallback
            return client
                .get(url) {
                    header(HttpHeaders.Authorization, basicAuthHeader())
                    block()
                }.also { response -> response.throwIfTransientServerError() }
                .body()
        }

        private fun io.ktor.client.statement.HttpResponse.throwIfTransientServerError() {
            if (status.value in 500..599) {
                throw TransientRavelryException(status.value)
            }
        }

        @OptIn(ExperimentalEncodingApi::class)
        private fun basicAuthHeader(): String {
            val credentials =
                Base64.encode(
                    "${BuildConfig.RAVELRY_BASIC_AUTH_USER}:${BuildConfig.RAVELRY_BASIC_AUTH_PASSWORD}"
                        .toByteArray(),
                )
            return "Basic $credentials"
        }

        private class TransientRavelryException(
            statusCode: Int,
        ) : IOException("Ravelry returned HTTP $statusCode")
    }
