package com.finnvek.knittools.auth

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.finnvek.knittools.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Hallinnoi Ravelry OAuth 2.0 -autentikointia.
 * Authorization Code -flow Chrome Custom Tabilla.
 * Tokenit tallennetaan EncryptedSharedPreferencesiin.
 */
@Singleton
class RavelryAuthManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val AUTH_URL = "https://www.ravelry.com/oauth2/auth"
            private const val TOKEN_URL = "https://www.ravelry.com/oauth2/token"
            private const val REDIRECT_URI = "com.finnvek.knittools://oauth/callback"
            private const val SCOPE = "offline"

            private const val PREFS_FILE = "ravelry_oauth2"
            private const val KEY_ACCESS_TOKEN = "access_token"
            private const val KEY_REFRESH_TOKEN = "refresh_token"
            private const val KEY_PENDING_STATE = "pending_state"
        }

        private val json = Json { ignoreUnknownKeys = true }

        private val prefs: SharedPreferences by lazy {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        private val _isAuthenticated = MutableStateFlow(hasStoredTokens())
        val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

        // CSRF-suojaus: tallennetaan state OAuth-pyynnön ajaksi
        private var pendingState: String? = prefs.getString(KEY_PENDING_STATE, null)

        val accessToken: String? get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        private val refreshToken: String? get() = prefs.getString(KEY_REFRESH_TOKEN, null)

        private fun hasStoredTokens(): Boolean = prefs.getString(KEY_ACCESS_TOKEN, null)?.isNotEmpty() == true

        /**
         * Avaa Chrome Custom Tab Ravelryn OAuth 2.0 -valtuutussivulle.
         */
        fun startOAuthFlow(activity: android.app.Activity) {
            val state = generateState()
            savePendingState(state)

            val authUri =
                Uri
                    .parse(AUTH_URL)
                    .buildUpon()
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("client_id", BuildConfig.RAVELRY_OAUTH2_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", REDIRECT_URI)
                    .appendQueryParameter("scope", SCOPE)
                    .appendQueryParameter("state", state)
                    .build()

            val customTab = CustomTabsIntent.Builder().build()
            customTab.launchUrl(activity, authUri)
        }

        /**
         * Käsittelee OAuth 2.0 -callback deep linkin.
         * Vaihdetaan authorization code tokeneiksi.
         */
        suspend fun handleCallback(
            httpClient: HttpClient,
            uri: Uri,
        ): Boolean {
            val code = uri.getQueryParameter("code") ?: return false
            val state = uri.getQueryParameter("state")
            val expectedState = pendingState ?: prefs.getString(KEY_PENDING_STATE, null)

            // Tarkista CSRF state
            if (state != expectedState) return false

            val success = exchangeCodeForTokens(httpClient, code)
            if (success) {
                clearPendingState()
            }
            return success
        }

        /**
         * Päivittää access tokenin refresh tokenilla.
         * Kutsutaan automaattisesti kun API palauttaa 401.
         */
        suspend fun refreshAccessToken(httpClient: HttpClient): Boolean {
            val currentRefreshToken = refreshToken ?: return false
            return requestTokens(
                httpClient,
                mapOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to currentRefreshToken,
                ),
            )
        }

        fun signOut() {
            prefs.edit().clear().apply()
            pendingState = null
            _isAuthenticated.value = false
        }

        private fun savePendingState(state: String) {
            pendingState = state
            prefs.edit().putString(KEY_PENDING_STATE, state).apply()
        }

        private fun clearPendingState() {
            pendingState = null
            prefs.edit().remove(KEY_PENDING_STATE).apply()
        }

        private suspend fun exchangeCodeForTokens(
            httpClient: HttpClient,
            code: String,
        ): Boolean =
            requestTokens(
                httpClient,
                mapOf(
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "redirect_uri" to REDIRECT_URI,
                ),
            )

        /**
         * Lähettää token-pyynnön Ravelrylle.
         * Client credentials lähetetään Basic Auth -headerissa (Ravelryn vaatimus).
         */
        @OptIn(ExperimentalEncodingApi::class)
        private suspend fun requestTokens(
            httpClient: HttpClient,
            params: Map<String, String>,
        ): Boolean {
            val credentials =
                Base64.encode(
                    "${BuildConfig.RAVELRY_OAUTH2_CLIENT_ID}:${BuildConfig.RAVELRY_OAUTH2_CLIENT_SECRET}"
                        .toByteArray(),
                )

            val response =
                httpClient
                    .submitForm(
                        url = TOKEN_URL,
                        formParameters =
                            parameters {
                                params.forEach { (key, value) -> append(key, value) }
                            },
                    ) {
                        header("Authorization", "Basic $credentials")
                    }.bodyAsText()

            val jsonObj = json.parseToJsonElement(response).jsonObject
            val newAccessToken = jsonObj["access_token"]?.jsonPrimitive?.content ?: return false
            val newRefreshToken = jsonObj["refresh_token"]?.jsonPrimitive?.content

            prefs
                .edit()
                .putString(KEY_ACCESS_TOKEN, newAccessToken)
                .apply {
                    if (newRefreshToken != null) {
                        putString(KEY_REFRESH_TOKEN, newRefreshToken)
                    }
                }.apply()

            _isAuthenticated.value = true
            return true
        }

        @OptIn(ExperimentalEncodingApi::class)
        private fun generateState(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return Base64.UrlSafe.encode(bytes).trimEnd('=')
        }
    }
