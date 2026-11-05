package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.ui.theme.OnboardingDimens

/** Vaiheiden välinen liuku: eteenpäin oikealta, taaksepäin vasemmalta. */
internal fun AnimatedContentTransitionScope<OnboardingStep>.journeyStepTransition(): ContentTransform {
    val forward = targetState.ordinal >= initialState.ordinal
    val direction = if (forward) 1 else -1
    val duration = OnboardingDimens.STEP_TRANSITION_MILLIS
    val offset = { width: Int -> (width * OnboardingDimens.STEP_SLIDE_FRACTION).toInt() }
    return (
        slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { direction * offset(it) } +
            fadeIn(tween(duration))
    ) togetherWith
        (
            slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing)) { -direction * offset(it) } +
                fadeOut(tween(duration / 2))
        )
}
