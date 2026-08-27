package com.finnvek.knittools.pro

import androidx.compose.runtime.Immutable
import com.finnvek.knittools.BuildConfig

enum class ProStatus {
    TRIAL_NOT_STARTED,
    TRIAL_ACTIVE,
    TRIAL_EXPIRED,
    PRO_PURCHASED,
}

enum class ProFeature {
    UNLIMITED_PROJECTS,
    NOTES,
    SECONDARY_COUNTER,
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
}

@Immutable
data class ProState(
    val status: ProStatus = ProStatus.TRIAL_NOT_STARTED,
    val trialDaysRemaining: Int = 0,
    val trialStartTimestamp: Long = 0L,
    val purchaseTimestamp: Long = 0L,
) {
    val isPro: Boolean
        get() = status == ProStatus.PRO_PURCHASED || status == ProStatus.TRIAL_ACTIVE

    // Nykyinen tuotemalli on yksi Pro-taso: feature-parametri nimeää käyttöpaikan,
    // mutta ei vielä eriytä oikeuksia ominaisuuskohtaisesti.
    @Suppress("UNUSED_PARAMETER")
    fun hasFeature(
        feature: ProFeature,
        debugUnlockAllFeatures: Boolean = BuildConfig.DEBUG,
    ): Boolean = debugUnlockAllFeatures || isPro
}
