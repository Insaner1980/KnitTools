package com.finnvek.knittools.ui.navigation

import java.util.UUID

data class CounterLaunchRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val projectId: Long? = null,
) {
    companion object {
        fun fromIntentData(
            intentData: CounterLaunchIntentData,
            consumedRequestId: String?,
        ): CounterLaunchRequest? {
            if (!intentData.shouldOpenCounter) return null
            val requestId = intentData.launchId ?: legacyRequestId(intentData.projectId)
            if (requestId == consumedRequestId) return null
            return CounterLaunchRequest(
                requestId = requestId,
                projectId = intentData.projectId,
            )
        }

        private fun legacyRequestId(projectId: Long?): String = "legacy-counter:${projectId ?: 0L}"
    }
}

data class CounterLaunchIntentData(
    val shouldOpenCounter: Boolean,
    val projectId: Long?,
    val launchId: String?,
)
