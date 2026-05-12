package com.finnvek.knittools.data.storage

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

object CounterLaunchTokenStore {
    private const val PREFS_NAME = "counter_launch_tokens"
    private const val KEY_LAUNCH_IDS = "launch_ids"
    private const val MAX_LAUNCH_IDS = 100

    @Synchronized
    fun issueLaunchId(context: Context): String {
        val launchId = UUID.randomUUID().toString()
        val prefs =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val launchIds = (prefs.readLaunchIds() + launchId).takeLast(MAX_LAUNCH_IDS)
        prefs.edit { putString(KEY_LAUNCH_IDS, launchIds.joinToString("\n")) }
        return launchId
    }

    @Synchronized
    fun isKnownLaunchId(
        context: Context,
        launchId: String?,
    ): Boolean {
        if (launchId.isNullOrBlank()) return false
        val prefs =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return launchId in prefs.readLaunchIds()
    }

    private fun android.content.SharedPreferences.readLaunchIds(): List<String> =
        getString(KEY_LAUNCH_IDS, null)
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            .orEmpty()
}
