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
            private const val QUERY_STATUS = "status"
        }

        private val _authState = MutableStateFlow<RavelryAuthState>(RavelryAuthState.NotConnected)
        val authState: StateFlow<RavelryAuthState> = _authState.asStateFlow()

        private var pendingState: String? = null

        suspend fun refreshAuthStatus(): RavelryAuthState =
            try {
                val status = backendClient.authStatus()
                if (status.connected) {
                    RavelryAuthState.Connected(status.username)
                } else {
                    RavelryAuthState.NotConnected
                }.also { _authState.value = it }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                RavelryAuthState.BackendUnavailable.also { _authState.value = it }
            }

        suspend fun startAuth(): Uri? {
            _authState.value = RavelryAuthState.Starting
            return try {
                val response = backendClient.startAuth()
                pendingState = response.state
                _authState.value = RavelryAuthState.AwaitingBrowser
                response.authorizeUrl.toUri()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                pendingState = null
                _authState.value = RavelryAuthState.BackendUnavailable
                null
            }
        }

        suspend fun handleCallback(uri: Uri): Boolean {
            if (!isOAuthCallback(uri)) return false

            val callbackState = uri.getQueryParameter(QUERY_STATE)
            val expectedState = pendingState
            if (callbackState.isNullOrBlank() || expectedState == null || callbackState != expectedState) {
                return true
            }

            when (uri.callbackFailure()) {
                CallbackFailure.Cancelled -> {
                    pendingState = null
                    _authState.value = RavelryAuthState.Cancelled
                    return true
                }

                CallbackFailure.Expired -> {
                    pendingState = null
                    _authState.value = RavelryAuthState.Expired
                    return true
                }

                null -> Unit
            }

            pendingState = null
            val refreshedState = refreshAuthStatus()
            if (refreshedState == RavelryAuthState.NotConnected) {
                _authState.value = RavelryAuthState.Expired
            }
            return true
        }

        fun isOAuthCallback(uri: Uri): Boolean =
            uri.scheme == REDIRECT_SCHEME &&
                uri.host == REDIRECT_HOST

        suspend fun disconnect(): RavelryAuthState {
            _authState.value = RavelryAuthState.Disconnecting
            return try {
                backendClient.disconnect()
                pendingState = null
                RavelryAuthState.NotConnected.also { _authState.value = it }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                RavelryAuthState.BackendUnavailable.also { _authState.value = it }
            }
        }

        fun markBrowserAuthCancelled() {
            if (_authState.value == RavelryAuthState.AwaitingBrowser || _authState.value == RavelryAuthState.Starting) {
                pendingState = null
                _authState.value = RavelryAuthState.Cancelled
            }
        }

        private fun Uri.callbackFailure(): CallbackFailure? {
            val rawValue = getQueryParameter(QUERY_ERROR) ?: getQueryParameter(QUERY_STATUS) ?: return null
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
