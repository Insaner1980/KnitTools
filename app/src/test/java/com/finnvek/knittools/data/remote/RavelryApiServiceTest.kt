package com.finnvek.knittools.data.remote

import com.finnvek.knittools.auth.RavelryAuthManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RavelryApiServiceTest {
    @Test
    fun `search throws when ravelry returns non success status`() {
        val service =
            RavelryApiService(
                client =
                    testClient {
                        respond(
                            content = """{"error":"rate limited"}""",
                            status = HttpStatusCode.TooManyRequests,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                authManager =
                    mockk<RavelryAuthManager> {
                        every { accessToken } returns null
                    },
            )

        val error =
            assertThrows(RuntimeException::class.java) {
                runTest {
                    service.searchPatterns(PatternSearchParams(query = "socks"))
                }
            }

        assertTrue(
            error.message?.contains("HTTP 429") == true,
        )
    }

    @Test
    fun `expired bearer token signs out and falls back when refresh throws`() =
        runTest {
            val authHeaders = mutableListOf<String?>()
            val authManager =
                mockk<RavelryAuthManager> {
                    every { accessToken } returns "expired"
                    coEvery { refreshAccessToken(any()) } throws IOException("refresh failed")
                    every { signOut() } just Runs
                }
            val service =
                RavelryApiService(
                    client =
                        testClient { request ->
                            val authorization = request.headers[HttpHeaders.Authorization]
                            authHeaders += authorization
                            if (authorization?.startsWith("Bearer ") == true) {
                                respond(
                                    content = """{"error":"expired"}""",
                                    status = HttpStatusCode.Unauthorized,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            } else {
                                respond(
                                    content = """{"patterns":[{"id":7,"name":"Fallback"}]}""",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            }
                        },
                    authManager = authManager,
                )

            val response = service.searchPatterns(PatternSearchParams(query = "socks"))

            assertEquals(listOf(7), response.patterns.map { it.id })
            assertTrue(authHeaders.any { it?.startsWith("Bearer ") == true })
            assertTrue(authHeaders.any { it?.startsWith("Basic ") == true })
            coVerify(exactly = 1) { authManager.refreshAccessToken(any()) }
            verify(exactly = 1) { authManager.signOut() }
        }

    private fun testClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine(handler)) {
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
