package com.finnvek.knittools.di

import android.content.Context
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.analytics.PostHogAnalytics
import com.finnvek.knittools.analytics.UsageAnalytics
import com.posthog.PersonProfiles
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    fun usageAnalytics(analytics: PostHogAnalytics): UsageAnalytics = analytics

    @Provides
    @Singleton
    fun postHogClient(
        @ApplicationContext context: Context,
    ): PostHogInterface = PostHogAndroid.with(context, postHogConfig(BuildConfig.POSTHOG_PROJECT_TOKEN))
}

internal fun postHogConfig(token: String): PostHogAndroidConfig =
    PostHogAndroidConfig(token, host = "https://eu.i.posthog.com").apply {
        optOut = true
        captureApplicationLifecycleEvents = false
        captureScreenViews = false
        captureDeepLinks = false
        capturePushNotificationSubscriptions = false
        capturePushNotificationOpened = false
        personProfiles = PersonProfiles.NEVER
        setDefaultPersonProperties = false
        preloadFeatureFlags = false
        sendFeatureFlagEvent = false
        sessionReplay = false
        surveys = false
        errorTrackingConfig.autoCapture = false
        errorTrackingConfig.captureNativeCrashes = false
        debug = false
    }
