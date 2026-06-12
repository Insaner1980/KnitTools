package com.finnvek.knittools.data.remote

import com.finnvek.knittools.auth.FirebaseAnonymousAuthGateway
import com.finnvek.knittools.auth.await
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import javax.inject.Inject
import javax.inject.Singleton

data class RavelryStartAuthResponse(
    val authorizeUrl: String,
    val state: String,
    val expiresAtMillis: Long,
)

data class RavelryBackendAuthStatus(
    val connected: Boolean,
    val username: String? = null,
)

data class RavelryBackendCurrentUser(
    val connected: Boolean,
    val ravelryUserId: String? = null,
    val ravelryUsername: String? = null,
)

interface RavelryBackendClient {
    suspend fun startAuth(): RavelryStartAuthResponse

    suspend fun authStatus(): RavelryBackendAuthStatus

    suspend fun disconnect()

    suspend fun currentUser(): RavelryBackendCurrentUser

    suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse

    suspend fun importPatternById(ravelryPatternId: Int): PatternDetail

    suspend fun importPatternByUrl(url: String): PatternDetail
}

@Singleton
class FirebaseRavelryBackendClient
    @Inject
    constructor(
        private val functions: FirebaseFunctions,
        private val authGateway: FirebaseAnonymousAuthGateway,
    ) : RavelryBackendClient {
        override suspend fun startAuth(): RavelryStartAuthResponse {
            val data = callBackend("ravelryStartAuth")
            return RavelryStartAuthResponse(
                authorizeUrl = data.string("authorizeUrl"),
                state = data.string("state"),
                expiresAtMillis = data.long("expiresAtMillis"),
            )
        }

        override suspend fun authStatus(): RavelryBackendAuthStatus {
            val data = callBackend("ravelryAuthStatus")
            return RavelryBackendAuthStatus(
                connected = data.boolean("connected"),
                username = data.optionalString("username"),
            )
        }

        override suspend fun disconnect() {
            callBackend("ravelryDisconnect")
        }

        override suspend fun currentUser(): RavelryBackendCurrentUser {
            val data = callBackend("ravelryCurrentUser")
            return RavelryBackendCurrentUser(
                connected = data.boolean("connected"),
                ravelryUserId = data.optionalString("ravelryUserId"),
                ravelryUsername = data.optionalString("ravelryUsername"),
            )
        }

        override suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse =
            RavelryBackendMappers.searchResponseFrom(
                callBackend(
                    "ravelrySearchPatterns",
                    mapOf(
                        "query" to params.query,
                        "craft" to params.craft,
                        "availability" to params.availability,
                        "pc" to params.pc,
                        "weight" to params.weight,
                        "difficultyFrom" to params.difficultyFrom,
                        "difficultyTo" to params.difficultyTo,
                        "page" to params.page,
                        "pageSize" to params.pageSize,
                    ).filterValues { it != null },
                ),
            )

        override suspend fun importPatternById(ravelryPatternId: Int): PatternDetail =
            RavelryBackendMappers.patternDetailFrom(
                callBackend(
                    "ravelryImportPatternById",
                    mapOf("ravelryPatternId" to ravelryPatternId),
                ),
            )

        override suspend fun importPatternByUrl(url: String): PatternDetail =
            RavelryBackendMappers.patternDetailFrom(
                callBackend(
                    "ravelryImportPatternByUrl",
                    mapOf("url" to url),
                ),
            )

        private suspend fun callBackend(
            name: String,
            data: Map<String, Any?> = emptyMap(),
        ): Map<*, *> {
            authGateway.ensureSignedIn()
            val result =
                try {
                    functions
                        .getHttpsCallable(name)
                        .call(data)
                        .await()
                        .data
                } catch (error: FirebaseFunctionsException) {
                    throw error.toRavelryException()
                }
            return result as? Map<*, *> ?: emptyMap<Any?, Any?>()
        }
    }

private fun FirebaseFunctionsException.toRavelryException(): RavelryHttpException =
    RavelryHttpException(
        when (code) {
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> 400
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> 401
            FirebaseFunctionsException.Code.NOT_FOUND -> 404
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> 412
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> 429
            FirebaseFunctionsException.Code.UNAVAILABLE -> 503
            else -> 500
        },
    )

private fun Map<*, *>.optionalString(key: String): String? = this[key]?.toString()?.takeIf { it.isNotBlank() }

private fun Map<*, *>.string(key: String): String = optionalString(key) ?: ""

private fun Map<*, *>.long(key: String): Long =
    when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    } ?: 0L

private fun Map<*, *>.boolean(key: String): Boolean = this[key] as? Boolean ?: false
