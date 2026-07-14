package com.finnvek.knittools.data.storage

import android.content.Context
import java.util.UUID

object CounterLaunchTokenStore {
    private const val PREFS_NAME = "counter_launch_tokens"
    private const val KEY_LAUNCH_IDS = "launch_ids"
    private const val MAX_LAUNCH_IDS = 100
    private const val TOKEN_SEPARATOR = '\t'
    internal const val TOKEN_TTL_MILLIS = 24L * 60L * 60L * 1000L

    fun issueLaunchId(context: Context): String = issueLaunchId(context, System.currentTimeMillis())

    @Synchronized
    internal fun issueLaunchId(
        context: Context,
        nowMillis: Long,
    ): String {
        val launchId = UUID.randomUUID().toString()
        val prefs =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val launchTokens =
            (
                prefs.readLaunchTokens().filter { it.isUsableAt(nowMillis) } +
                    LaunchToken(launchId = launchId, issuedAtMillis = nowMillis)
            ).takeLast(MAX_LAUNCH_IDS)
        prefs.writeLaunchTokens(launchTokens)
        return launchId
    }

    fun consumeLaunchId(
        context: Context,
        launchId: String?,
    ): Boolean = consumeLaunchId(context, launchId, System.currentTimeMillis())

    @Synchronized
    internal fun consumeLaunchId(
        context: Context,
        launchId: String?,
        nowMillis: Long,
    ): Boolean {
        if (launchId.isNullOrBlank()) return false
        val prefs =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val launchTokens = prefs.readLaunchTokens()
        val isUsable = launchTokens.any { it.launchId == launchId && it.isUsableAt(nowMillis) }
        val remainingTokens =
            launchTokens.filter { token ->
                token.launchId != launchId && token.isUsableAt(nowMillis)
            }
        val persisted = prefs.writeLaunchTokens(remainingTokens)
        return isUsable && persisted
    }

    private fun android.content.SharedPreferences.readLaunchTokens(): List<LaunchToken> =
        getString(KEY_LAUNCH_IDS, null)
            ?.lineSequence()
            ?.mapNotNull(LaunchToken::parse)
            ?.toList()
            .orEmpty()

    private fun android.content.SharedPreferences.writeLaunchTokens(tokens: List<LaunchToken>): Boolean =
        edit()
            .putString(KEY_LAUNCH_IDS, tokens.joinToString("\n", transform = LaunchToken::serialize))
            .commit()

    private data class LaunchToken(
        val launchId: String,
        val issuedAtMillis: Long,
    ) {
        fun isUsableAt(nowMillis: Long): Boolean =
            issuedAtMillis >= 0L &&
                issuedAtMillis <= nowMillis &&
                nowMillis - issuedAtMillis < TOKEN_TTL_MILLIS

        fun serialize(): String = "$issuedAtMillis$TOKEN_SEPARATOR$launchId"

        companion object {
            fun parse(value: String): LaunchToken? {
                val separatorIndex = value.indexOf(TOKEN_SEPARATOR)
                if (separatorIndex <= 0 || separatorIndex == value.lastIndex) return null
                val issuedAtMillis = value.substring(0, separatorIndex).toLongOrNull() ?: return null
                val launchId = value.substring(separatorIndex + 1).takeIf { it.isNotBlank() } ?: return null
                return LaunchToken(launchId = launchId, issuedAtMillis = issuedAtMillis)
            }
        }
    }
}
