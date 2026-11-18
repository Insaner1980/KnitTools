package com.finnvek.knittools.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation

/** Reveals the original artwork in three regions without redrawing its lettering. */
internal class SplashLogoDrawable(
    private val bitmap: Bitmap,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val sweep = Path()
    private val middle = Path().apply { addCircle(CENTER_X, CENTER_Y, MIDDLE_RADIUS, Path.Direction.CW) }
    private val center = Path().apply { addCircle(CENTER_X, CENTER_Y, CENTER_RADIUS, Path.Direction.CW) }

    var elapsedMillis: Float = 0f
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        canvas.withTranslation(bounds.left.toFloat(), bounds.top.toFloat()) {
            canvas.scale(bounds.width() / ARTWORK_SIZE, bounds.height() / ARTWORK_SIZE)
            if (elapsedMillis >= REVEAL_END) {
                canvas.drawBitmap(bitmap, null, ARTWORK_BOUNDS, paint)
            } else {
                val outerProgress = (elapsedMillis / OUTER_DURATION).coerceIn(0f, 1f)
                drawSweep(canvas, 1f - (1f - outerProgress) * (1f - outerProgress), OUTER_RADIUS, middle)
                drawSweep(
                    canvas,
                    ((elapsedMillis - TEXT_START) / TEXT_DURATION).coerceIn(0f, 1f),
                    MIDDLE_RADIUS,
                    center,
                )
                val centerProgress = ((elapsedMillis - CENTER_START) / CENTER_DURATION).coerceIn(0f, 1f)
                val top =
                    CENTER_Y + CENTER_RADIUS - CENTER_RADIUS * 2 * (1f - (1f - centerProgress) * (1f - centerProgress))
                canvas.withClip(center) {
                    clipRect(0f, top, ARTWORK_SIZE, ARTWORK_SIZE)
                    drawBitmap(bitmap, null, ARTWORK_BOUNDS, paint)
                }
            }
        }
    }

    private fun drawSweep(
        canvas: Canvas,
        progress: Float,
        radius: Float,
        exclusion: Path,
    ) {
        if (progress <= 0f) return
        sweep.rewind()
        if (progress >= 1f) {
            sweep.addCircle(CENTER_X, CENTER_Y, radius, Path.Direction.CW)
        } else {
            sweep.moveTo(CENTER_X, CENTER_Y)
            sweep.arcTo(
                CENTER_X - radius,
                CENTER_Y - radius,
                CENTER_X + radius,
                CENTER_Y + radius,
                START_ANGLE_DEGREES,
                FULL_CIRCLE_DEGREES * progress,
                false,
            )
            sweep.close()
        }
        canvas.withClip(sweep) {
            clipOutPath(exclusion)
            drawBitmap(bitmap, null, ARTWORK_BOUNDS, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val ARTWORK_SIZE = 500f
        const val CENTER_X = 250f
        const val CENTER_Y = 246f
        const val OUTER_RADIUS = 360f
        const val MIDDLE_RADIUS = 180f
        const val CENTER_RADIUS = 104f
        const val START_ANGLE_DEGREES = -90f
        const val FULL_CIRCLE_DEGREES = 360f
        const val OUTER_DURATION = 380f
        const val TEXT_START = 180f
        const val TEXT_DURATION = 350f
        const val CENTER_START = 380f
        const val CENTER_DURATION = 260f
        const val REVEAL_END = 640f
        val ARTWORK_BOUNDS = RectF(0f, 0f, ARTWORK_SIZE, ARTWORK_SIZE)
    }
}
