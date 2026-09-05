package com.finnvek.knittools.data.remote

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

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

internal class TransientRavelryException(
    val statusCode: Int,
) : IOException("Ravelry returned HTTP $statusCode")

internal class RavelryHttpException(
    val statusCode: Int,
    message: String = "Ravelry returned HTTP $statusCode",
) : RuntimeException(message)

@Singleton
class RavelryApiService
    @Inject
    constructor(
        private val backendClient: RavelryBackendClient,
    ) {
        suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse =
            backendClient.searchPatterns(params)

        suspend fun getPatternDetail(id: Int): PatternDetail = backendClient.importPatternById(id)

        suspend fun importPatternByUrl(url: String): PatternDetail = backendClient.importPatternByUrl(url)
    }
