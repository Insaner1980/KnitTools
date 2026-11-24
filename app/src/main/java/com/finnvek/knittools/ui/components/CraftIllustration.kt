package com.finnvek.knittools.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.finnvek.knittools.ui.theme.FontScaleThresholds
import com.finnvek.knittools.ui.theme.IllustrationDimens

/** Miten kuvitus tulee näkyviin: rauhallinen nousu tai juhlistava jousi. */
enum class IllustrationEntrance { Rise, Bounce }

/**
 * Savimainen 3D-kuvitus (onboarding ja Pro-sivu). Kuva tulee sisään kerran, eikä sillä ole ikuista
 * animaatiota, joten järjestelmän animaatioasetus pysäyttää liikkeen kokonaan.
 */
@Composable
fun CraftIllustration(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    height: Dp = IllustrationDimens.Height,
    entrance: IllustrationEntrance = IllustrationEntrance.Rise,
) {
    val progress = remember { Animatable(0f) }
    val risePx = with(LocalDensity.current) { IllustrationDimens.Rise.toPx() }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                when (entrance) {
                    IllustrationEntrance.Rise -> {
                        tween(IllustrationDimens.ENTER_MILLIS, easing = FastOutSlowInEasing)
                    }

                    IllustrationEntrance.Bounce -> {
                        spring(dampingRatio = IllustrationDimens.BOUNCE_DAMPING, stiffness = Spring.StiffnessLow)
                    }
                },
        )
    }
    Image(
        painter = painterResource(imageRes),
        // Koristekuva: viereinen otsikko kertoo saman asian ruudunlukijalle.
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier =
            modifier
                // Kiinteä korkeus: leveys, enimmäiskorkeus ja kuvasuhde yhdessä päästivät kuvan rajojensa yli.
                .fillMaxWidth()
                .height(height)
                .graphicsLayer {
                    val value = progress.value
                    alpha = value.coerceIn(0f, 1f)
                    translationY = (1f - value) * risePx
                    if (entrance == IllustrationEntrance.Bounce) {
                        val scale =
                            IllustrationDimens.BOUNCE_SCALE_FROM + (1f - IllustrationDimens.BOUNCE_SCALE_FROM) * value
                        scaleX = scale
                        scaleY = scale
                    }
                },
    )
}

/** Listarivin porrastetun sisääntulon tila; luodaan [rememberStaggeredEntrance]illa. */
class StaggeredEntrance internal constructor(
    internal val progress: Animatable<Float, *>,
    internal val risePx: Float,
)

/**
 * Listarivin porrastettu sisääntulo: häivytys ja pieni nousu, viive kasvaa rivin [index]in mukaan.
 * Animaatio ajetaan kerran eikä muuta asettelua, joten tekstin mitat pysyvät vakaina.
 */
@Composable
fun rememberStaggeredEntrance(index: Int): StaggeredEntrance {
    val progress = remember { Animatable(0f) }
    val risePx = with(LocalDensity.current) { IllustrationDimens.EntranceRise.toPx() }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = IllustrationDimens.ENTRANCE_MILLIS,
                    delayMillis =
                        IllustrationDimens.ENTRANCE_BASE_DELAY_MILLIS +
                            index * IllustrationDimens.ENTRANCE_STAGGER_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
        )
    }
    return remember(progress, risePx) { StaggeredEntrance(progress, risePx) }
}

fun Modifier.staggeredEntrance(entrance: StaggeredEntrance): Modifier =
    graphicsLayer {
        val value = entrance.progress.value
        alpha = value
        translationY = (1f - value) * entrance.risePx
    }

/**
 * Kuvituksen korkeus käytettävissä olevan korkeuden mukaan: matalassa näkymässä pienempi ja hyvin
 * matalassa ei lainkaan (null). Suurella fontilla teksti tarvitsee tilan, joten matalassa näkymässä kuvitus
 * jää silloin kokonaan pois. Yhteinen perehdytykselle ja Pro-sivulle.
 */
fun illustrationHeightFor(
    viewportHeight: Dp,
    regular: Dp,
    fontScale: Float = 1f,
): Dp? =
    when {
        viewportHeight < IllustrationDimens.HiddenBelowViewportHeight -> null
        viewportHeight < IllustrationDimens.ShortViewportHeight && fontScale > FontScaleThresholds.COMPACT -> null
        viewportHeight < IllustrationDimens.ShortViewportHeight -> minOf(regular, IllustrationDimens.ShortHeight)
        else -> regular
    }
