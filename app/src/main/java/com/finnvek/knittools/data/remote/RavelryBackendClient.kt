package com.finnvek.knittools.data.remote

import com.finnvek.knittools.auth.FirebaseAnonymousAuthGateway
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
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
                    params.toBackendData(),
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
            return result as? Map<*, *> ?: throw RavelryHttpException(500, "Malformed Ravelry backend response")
        }
    }

private fun FirebaseFunctionsException.toRavelryException(): Exception = ravelryExceptionForFirebaseCodeName(code.name)

internal fun ravelryExceptionForFirebaseCodeName(codeName: String): Exception =
    when (codeName) {
        "RESOURCE_EXHAUSTED" -> TransientRavelryException(429)
        "UNAVAILABLE" -> TransientRavelryException(503)
        "DEADLINE_EXCEEDED" -> TransientRavelryException(504)
        "INTERNAL" -> TransientRavelryException(500)
        "INVALID_ARGUMENT" -> RavelryHttpException(400)
        "UNAUTHENTICATED" -> RavelryHttpException(401)
        "NOT_FOUND" -> RavelryHttpException(404)
        "FAILED_PRECONDITION" -> RavelryHttpException(412)
        else -> RavelryHttpException(500)
    }

private fun PatternSearchParams.toBackendData(): Map<String, Any> =
    buildMap {
        put("query", query)
        putOptional("craft", craft)
        putOptional("availability", availability)
        putOptional("pc", pc)
        putOptional("weight", weight)
        putOptional("difficultyFrom", difficultyFrom)
        putOptional("difficultyTo", difficultyTo)
        put("page", page)
        put("pageSize", pageSize)
    }

private fun <T : Any> MutableMap<String, Any>.putOptional(
    key: String,
    value: T?,
) {
    value?.let { put(key, it) }
}

// CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
private fun Map<*, *>.optionalString(key: String): String? = this[key]?.toString()?.takeIf { it.isNotBlank() }

private fun Map<*, *>.string(key: String): String = optionalString(key) ?: ""

private fun Map<*, *>.long(key: String): Long =
// CPD-ON
    when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    } ?: 0L

private fun Map<*, *>.boolean(key: String): Boolean = this[key] as? Boolean ?: false
