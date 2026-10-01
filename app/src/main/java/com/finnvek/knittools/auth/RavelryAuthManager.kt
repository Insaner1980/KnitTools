package com.finnvek.knittools.auth

import android.net.Uri
import androidx.core.net.toUri
import com.finnvek.knittools.data.remote.RavelryBackendClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

sealed interface RavelryAuthState {
    data object NotConnected : RavelryAuthState

    data object Starting : RavelryAuthState

    data object AwaitingBrowser : RavelryAuthState

    data class Connected(
        val username: String?,
    ) : RavelryAuthState

    data object Cancelled : RavelryAuthState

    data object Expired : RavelryAuthState

    data object BackendUnavailable : RavelryAuthState

    data object Disconnecting : RavelryAuthState
}

@Singleton
class RavelryAuthManager
    @Inject
    constructor(
        private val backendClient: RavelryBackendClient,
    ) {
        companion object {
            const val REDIRECT_SCHEME = "knittools"
            const val REDIRECT_HOST = "ravelry-auth-complete"
            private const val QUERY_STATE = "state"
            private const val QUERY_ERROR = "error"
            private const val QUERY_PROOF = "proof"
        }

        private val _authState = MutableStateFlow<RavelryAuthState>(RavelryAuthState.NotConnected)
        val authState: StateFlow<RavelryAuthState> = _authState.asStateFlow()

        private var pendingState: String? = null
        private var operationId = 0L
        private var activeAuthOperationId: Long? = null
        private var acceptsRecoveredCallback = true
        private var activatedCallbackState: String? = null

        suspend fun refreshAuthStatus(): RavelryAuthState {
            if (activeAuthOperationId == operationId) return _authState.value
            return refreshAuthStatus(RavelryAuthState.NotConnected, beginOperation())
        }

        private suspend fun refreshAuthStatus(
            notConnectedState: RavelryAuthState,
            currentOperationId: Long,
        ): RavelryAuthState =
            try {
                val status = backendClient.authStatus()
                val resolvedState =
                    if (status.connected) {
                        RavelryAuthState.Connected(status.username)
                    } else {
                        notConnectedState
                    }
                applyStateIfCurrent(currentOperationId, resolvedState)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                applyStateIfCurrent(currentOperationId, RavelryAuthState.BackendUnavailable)
            }

        suspend fun startAuth(): Uri? {
            val currentOperationId = beginAuthOperation()
            activeAuthOperationId = currentOperationId
            _authState.value = RavelryAuthState.Starting
            return try {
                val response = backendClient.startAuth()
                if (currentOperationId != operationId) return null
                pendingState = response.state
                _authState.value = RavelryAuthState.AwaitingBrowser
                response.authorizeUrl.toUri()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (currentOperationId == operationId) {
                    pendingState = null
                    _authState.value = RavelryAuthState.BackendUnavailable
                }
                null
            } finally {
                finishAuthOperation(currentOperationId)
            }
        }

        private fun applyStateIfCurrent(
            currentOperationId: Long,
            state: RavelryAuthState,
        ): RavelryAuthState =
            if (currentOperationId == operationId) {
                state.also { _authState.value = it }
            } else {
                _authState.value
            }

        private fun beginOperation(): Long {
            operationId += 1L
            return operationId
        }

        private fun beginAuthOperation(): Long {
            pendingState = null
            activatedCallbackState = null
            acceptsRecoveredCallback = false
            activeAuthOperationId = null
            return beginOperation()
        }

        private fun finishAuthOperation(currentOperationId: Long) {
            if (activeAuthOperationId == currentOperationId) activeAuthOperationId = null
        }

        suspend fun handleCallback(uri: Uri): Boolean {
            if (!isOAuthCallback(uri)) return false

            val callbackState = uri.getQueryParameter(QUERY_STATE)
            val expectedState = pendingState ?: activatedCallbackState
            if (callbackState.isNullOrBlank() || (expectedState != null && callbackState != expectedState)) {
                return true
            }
            if (expectedState == null && !acceptsRecoveredCallback) return true

            val failure = uri.callbackFailure()
            if (failure != null) {
                beginAuthOperation()
                _authState.value =
                    when (failure) {
                        CallbackFailure.Cancelled -> RavelryAuthState.Cancelled
                        CallbackFailure.Expired -> RavelryAuthState.Expired
                    }
            } else if (activeAuthOperationId != operationId) {
                uri.getQueryParameter(QUERY_PROOF)?.let { completeCallback(callbackState, it) }
            }
            return true
        }

        private suspend fun completeCallback(
            callbackState: String,
            completionProof: String,
        ) {
            val currentOperationId = beginOperation()
            activeAuthOperationId = currentOperationId
            acceptsRecoveredCallback = false
            if (activatedCallbackState != callbackState) pendingState = callbackState
            try {
                if (activatedCallbackState != callbackState) {
                    backendClient.completeAuth(callbackState, completionProof)
                    if (currentOperationId != operationId) return
                    pendingState = null
                    activatedCallbackState = callbackState
                }
                refreshAuthStatus(RavelryAuthState.Expired, currentOperationId)
                if (currentOperationId == operationId) activatedCallbackState = null
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (currentOperationId == operationId) {
                    pendingState = null
                    activatedCallbackState = null
                    _authState.value = RavelryAuthState.BackendUnavailable
                }
            } finally {
                finishAuthOperation(currentOperationId)
            }
        }

        fun isOAuthCallback(uri: Uri): Boolean {
            if (
                uri.scheme != REDIRECT_SCHEME ||
                uri.host != REDIRECT_HOST ||
                uri.encodedAuthority != REDIRECT_HOST
            ) {
                return false
            }
            if (!uri.path.isNullOrEmpty() || uri.fragment != null) return false

            val parameterNames = uri.queryParameterNames.toSet()
            val successParameters = setOf(QUERY_STATE, QUERY_PROOF)
            val failureParameters = setOf(QUERY_STATE, QUERY_ERROR)
            if (parameterNames != successParameters && parameterNames != failureParameters) {
                return false
            }
            if (uri.getQueryParameters(QUERY_STATE).singleOrNull().isNullOrBlank()) return false
            return if (QUERY_ERROR in parameterNames) {
                !uri.getQueryParameters(QUERY_ERROR).singleOrNull().isNullOrBlank()
            } else {
                !uri.getQueryParameters(QUERY_PROOF).singleOrNull().isNullOrBlank()
            }
        }

        suspend fun disconnect(): RavelryAuthState {
            val currentOperationId = beginAuthOperation()
            activeAuthOperationId = currentOperationId
            _authState.value = RavelryAuthState.Disconnecting
            return try {
                backendClient.disconnect()
                if (currentOperationId == operationId) pendingState = null
                applyStateIfCurrent(currentOperationId, RavelryAuthState.NotConnected)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                applyStateIfCurrent(currentOperationId, RavelryAuthState.BackendUnavailable)
            } finally {
                finishAuthOperation(currentOperationId)
            }
        }

        fun markBrowserAuthCancelled() {
            if (_authState.value == RavelryAuthState.AwaitingBrowser || _authState.value == RavelryAuthState.Starting) {
                beginAuthOperation()
                _authState.value = RavelryAuthState.Cancelled
            }
        }

        private fun Uri.callbackFailure(): CallbackFailure? {
            val rawValue = getQueryParameter(QUERY_ERROR) ?: return null
            return when (rawValue.lowercase(Locale.US)) {
                "cancelled", "canceled", "access_denied" -> CallbackFailure.Cancelled
                "expired", "state_expired" -> CallbackFailure.Expired
                else -> CallbackFailure.Cancelled
            }
        }

        private enum class CallbackFailure {
            Cancelled,
            Expired,
        }
    }
