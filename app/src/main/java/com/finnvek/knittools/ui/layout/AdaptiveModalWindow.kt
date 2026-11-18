package com.finnvek.knittools.ui.layout

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.DialogWindowProvider
import androidx.window.layout.WindowMetricsCalculator

/** Opt in at the owning route; shared modal components retain their normal behavior elsewhere. */
internal val LocalAdaptiveModalWindow = compositionLocalOf { false }

/** Called inside a dialog's composition, where LocalView belongs to its separate window. */
@Composable
@SuppressLint("RtlHardcoded") // Pane coordinates use the physical left edge in both LTR and RTL layouts.
internal fun AdaptiveModalWindow() {
    if (!LocalAdaptiveModalWindow.current) return
    val activity = LocalActivity.current ?: return
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val folds = LocalWindowFolds.current
    val bounds =
        remember(activity, configuration, folds) {
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds
        }
    val available = IntRect(0, 0, bounds.width(), bounds.height())
    val pane = unobstructedWindowBounds(available, folds, layoutDirection)
    if (pane != available) {
        DisposableEffect(window, pane) {
            val original = WindowManager.LayoutParams().apply { copyFrom(window.attributes) }
            // Material resets a sheet window to MATCH_PARENT in its own SideEffect.
            // Apply the pane after those updates, before drawing either modal content or controls.
            val listener =
                ViewTreeObserver.OnPreDrawListener {
                    val current = window.attributes
                    val matches =
                        current.width == pane.width &&
                            current.height == pane.height &&
                            current.x == pane.left &&
                            current.y == pane.top &&
                            current.gravity == (Gravity.TOP or Gravity.LEFT)
                    if (!matches) {
                        window.attributes =
                            WindowManager.LayoutParams().apply {
                                copyFrom(current)
                                width = pane.width
                                height = pane.height
                                gravity = Gravity.TOP or Gravity.LEFT
                                x = pane.left
                                y = pane.top
                                flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            }
                    }
                    matches
                }
            val observer = window.decorView.viewTreeObserver
            observer.addOnPreDrawListener(listener)
            listener.onPreDraw()
            onDispose {
                window.decorView.viewTreeObserver.removeOnPreDrawListener(listener)
                window.attributes = original
            }
        }
    }
}
