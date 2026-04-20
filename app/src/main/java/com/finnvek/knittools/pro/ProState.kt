package com.finnvek.knittools.pro

import androidx.compose.runtime.Immutable

enum class ProStatus {
    TRIAL_ACTIVE,
    TRIAL_EXPIRED,
    PRO_PURCHASED,
}

enum class ProFeature {
    UNLIMITED_PROJECTS,
    FULL_HISTORY,
    NOTES,
    SECONDARY_COUNTER,
    OCR,
    GEMINI_NANO,
    WIDGET,
    ROW_REMINDERS,
    PROGRESS_PHOTOS,
    MULTIPLE_COUNTERS,
    SHAPING_COUNTER,
    REPEAT_SECTION,
    PATTERN_CAMERA_SCAN,
    INSIGHTS_CHARTS,
    STREAK,
    UNLIMITED_YARN,
    VOICE_COMMANDS,
    VOICE_LIVE,
    AI_FEATURES,
}

@Immutable
data class ProState(
    val status: ProStatus = ProStatus.TRIAL_EXPIRED,
    val trialDaysRemaining: Int = 0,
    val trialStartTimestamp: Long = 0L,
    val purchaseTimestamp: Long = 0L,
) {
    val isPro: Boolean
        get() = status == ProStatus.PRO_PURCHASED || status == ProStatus.TRIAL_ACTIVE

    @Suppress("UNUSED_PARAMETER") // feature-parametri valmiina per-feature-gatingia varten
    fun hasFeature(feature: ProFeature): Boolean = isPro
}
