package com.finnvek.knittools.data.remote

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RavelryApiServiceTest {
    @Test
    fun `search delegates to backend client`() =
        runTest {
            val params = PatternSearchParams(query = "socks")
            val backendClient =
                mockk<RavelryBackendClient> {
                    coEvery { searchPatterns(params) } returns
                        PatternSearchResponse(
                            patterns = listOf(PatternSearchResult(id = 7, name = "Backend")),
                        )
                }
            val service =
                RavelryApiService(
                    backendClient = backendClient,
                )

            val response = service.searchPatterns(params)

            assertEquals(listOf(7), response.patterns.map { it.id })
        }

    @Test
    fun `detail delegates to backend client import by id`() =
        runTest {
            val backendClient =
                mockk<RavelryBackendClient> {
                    coEvery { importPatternById(42) } returns PatternDetail(id = 42, name = "Backend Detail")
                }
            val service = RavelryApiService(backendClient = backendClient)

            assertEquals("Backend Detail", service.getPatternDetail(42).name)
        }

    @Test
    fun `url import delegates to backend client import by url`() =
        runTest {
            val url = "https://www.ravelry.com/patterns/library/backend-detail"
            val backendClient =
                mockk<RavelryBackendClient> {
                    coEvery { importPatternByUrl(url) } returns PatternDetail(id = 42, name = "Backend Detail")
                }
            val service = RavelryApiService(backendClient = backendClient)

            assertEquals("Backend Detail", service.importPatternByUrl(url).name)
        }
}
