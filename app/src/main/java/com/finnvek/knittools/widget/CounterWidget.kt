package com.finnvek.knittools.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.ui.components.labelRes
import com.finnvek.knittools.ui.theme.CounterWidgetDimens
import com.finnvek.knittools.ui.theme.LightSurface
import com.finnvek.knittools.ui.theme.LightTextMuted
import com.finnvek.knittools.ui.theme.LightTextPrimary
import com.finnvek.knittools.ui.theme.LightTextSecondary
import com.finnvek.knittools.ui.theme.LightYarnColors
import com.finnvek.knittools.ui.theme.OnPrimary
import com.finnvek.knittools.ui.theme.Primary
import com.finnvek.knittools.ui.theme.PrimaryContainer
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.Surface
import com.finnvek.knittools.ui.theme.TextMuted
import com.finnvek.knittools.ui.theme.TextPrimary
import com.finnvek.knittools.ui.theme.TextSecondary
import com.finnvek.knittools.ui.theme.YarnColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import dagger.hilt.android.EntryPointAccessors
import androidx.glance.color.ColorProvider as DayNightColorProvider

class CounterWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode =
        SizeMode.Responsive(
            setOf(SMALL_SIZE, STACKED_SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE),
        )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
        val manager = entryPoint.proManager()
        val initialAccess = manager.featureAccessAfterInitialLoad(ProFeature.WIDGET)
        val accessFlow = manager.featureAccessFlow(ProFeature.WIDGET)
        val sharedDataFlow = CounterWidgetState.observe(context)
        val initialShared = CounterWidgetState.load(context)
        val initialData =
            if (initialAccess == ProActionAccess.Allowed || initialAccess == ProActionAccess.Trial) {
                resolveWidgetData(context, entryPoint, initialShared, CounterWidgetState.loadGlance(context, id))
            } else {
                CounterWidgetState.defaultData(context)
            }

        provideContent {
            val access by accessFlow.collectAsState(initialAccess)
            val shared by sharedDataFlow.collectAsState(initialShared)
            val refresh by CounterWidgetState.refreshes.collectAsState()
            val prefs = currentState<Preferences>()
            var data by remember { mutableStateOf(initialData) }
            LaunchedEffect(access, shared, refresh) {
                if (access == ProActionAccess.Allowed || access == ProActionAccess.Trial) {
                    data =
                        resolveWidgetData(
                            context,
                            entryPoint,
                            shared,
                            CounterWidgetState.fromPreferences(context, prefs),
                        )
                }
            }
            GlanceTheme(colors = ColorProviders(dark = WidgetDarkScheme, light = WidgetLightScheme)) {
                WidgetSizedContent(data = data, access = access)
            }
        }
    }

    private suspend fun resolveWidgetData(
        context: Context,
        entryPoint: WidgetEntryPoint,
        sharedWidgetData: WidgetData,
        widgetData: WidgetData,
    ): WidgetData {
        // Widgetit seuraavat samaa aktiivista projektia. Peilaa jaettu tila myös silloin,
        // kun Glance-instanssille on jäänyt vanha projekti aiemmasta renderöinnistä.
        val repository = entryPoint.counterRepository()
        val candidates =
            if (sharedWidgetData.projectId > 0L) {
                listOf(sharedWidgetData)
            } else {
                listOf(sharedWidgetData, widgetData)
            }
        val resolvedData =
            repository.resolveWidgetDisplayData(
                context = context,
                candidates = candidates,
            )
        if (resolvedData != sharedWidgetData) {
            CounterWidgetState.replaceIfCurrent(context, sharedWidgetData, resolvedData)
        }
        return resolvedData
    }

    companion object {
        val SMALL_SIZE = DpSize(120.dp, 48.dp)
        val STACKED_SMALL_SIZE = DpSize(120.dp, 80.dp)
        val MEDIUM_SIZE = DpSize(160.dp, 160.dp)
        val LARGE_SIZE = DpSize(300.dp, 160.dp)
    }
}

private val WidgetDarkScheme =
    darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        surface = Surface,
        surfaceVariant = TextMuted,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        tertiary = PrimaryContainer,
    )

private val WidgetLightScheme =
    lightColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        surface = LightSurface,
        surfaceVariant = LightTextMuted,
        onSurface = LightTextPrimary,
        onSurfaceVariant = LightTextSecondary,
        tertiary = PrimaryContainer,
    )

@androidx.compose.runtime.Composable
private fun WidgetSizedContent(
    data: WidgetData,
    access: ProActionAccess,
) {
    if (access == ProActionAccess.Unknown || access == ProActionAccess.Locked) {
        WidgetAccessMessage(unknown = access == ProActionAccess.Unknown)
        return
    }
    val size = LocalSize.current
    val projectId = data.projectId.takeIf { it > 0L }
    if (projectId == null) {
        WidgetCard(projectId = null) {
            WidgetProjectName(data, CounterWidgetDimens.SmallNameSize, TextAlign.Center)
        }
        return
    }
    when {
        size.width >= CounterWidget.LARGE_SIZE.width && size.height >= CounterWidget.LARGE_SIZE.height -> {
            LargeWidget(data = data, projectId = projectId)
        }

        size.width >= CounterWidget.MEDIUM_SIZE.width && size.height >= CounterWidget.MEDIUM_SIZE.height -> {
            MediumWidget(data = data, projectId = projectId)
        }

        else -> {
            SmallWidget(data = data, projectId = projectId)
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetAccessMessage(unknown: Boolean) {
    val context = LocalContext.current
    val description = context.getString(if (unknown) R.string.retry else R.string.widget_pro_required)
    val label =
        if (!unknown && LocalSize.current.height < CounterWidget.MEDIUM_SIZE.height) {
            context.getString(R.string.upgrade_to_pro)
        } else {
            description
        }
    WidgetCard(
        projectId = null,
        openProUpgrade = !unknown,
        onClick = if (unknown) actionSendBroadcast(CounterWidgetActions.retryIntent(context)) else null,
        horizontalPadding = CounterWidgetDimens.HorizontalPadding,
        verticalPadding = CounterWidgetDimens.SectionSpacing,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                modifier = GlanceModifier.semantics { contentDescription = description },
                style =
                    TextStyle(
                        fontSize = CounterWidgetDimens.ProPromptSize,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }
}

/**
 * Pieni widget on yhden napautuksen rivilaskuri: nimi ja luku vasemmalla, laskurin oma
 * 3D-plus oikealla. Miinus ja tavoite jäävät isompiin kokoihin.
 */
@androidx.compose.runtime.Composable
private fun SmallWidget(
    data: WidgetData,
    projectId: Long,
) {
    val stacked = LocalSize.current.height >= CounterWidget.STACKED_SMALL_SIZE.height
    WidgetCard(
        projectId = projectId,
        horizontalPadding = CounterWidgetDimens.SmallHorizontalPadding,
        verticalPadding = 0.dp,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 48 dp:n rivissä nimi ja luku eivät mahdu päällekkäin, joten ne ovat rinnakkain.
            if (stacked) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    WidgetProjectName(
                        data = data,
                        fontSize = CounterWidgetDimens.SmallNameSize,
                        textAlign = TextAlign.Start,
                    )
                    WidgetCount(data = data, fontSize = CounterWidgetDimens.StackedCountSize)
                }
            } else {
                Box(modifier = GlanceModifier.defaultWeight()) {
                    WidgetProjectName(
                        data = data,
                        fontSize = CounterWidgetDimens.SmallNameSize,
                        textAlign = TextAlign.Start,
                    )
                }
                Spacer(modifier = GlanceModifier.width(CounterWidgetDimens.SectionSpacing))
                WidgetCount(data = data, fontSize = CounterWidgetDimens.SmallCountSize)
                Spacer(modifier = GlanceModifier.width(CounterWidgetDimens.SectionSpacing))
            }
            WidgetCountButton(
                action = WidgetCountAction.INCREMENT,
                projectId = projectId,
                size = CounterWidgetDimens.SmallButtonTouchSize,
                imageInset = CounterWidgetDimens.SmallButtonInset,
            )
        }
    }
}

/**
 * Neliö: nimi, luku, tavoitepalkki ja sen alla täysleveä yksikkörivi, napit alhaalla reunoissa.
 * Yksikkö ei mahdu nappien väliin: 160 dp:n leveydestä jää sinne vain noin 36 dp, ja esimerkiksi
 * "KERROKSET" ja "yhteensä 72" katkesivat.
 */
@androidx.compose.runtime.Composable
private fun MediumWidget(
    data: WidgetData,
    projectId: Long,
) {
    WidgetCard(projectId = projectId, verticalPadding = mediumVerticalPadding()) {
        WidgetProjectName(data = data, fontSize = CounterWidgetDimens.NameSize, textAlign = TextAlign.Center)
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetCount(
            data = data,
            fontSize = CounterWidgetDimens.CountSize,
            modifier = GlanceModifier.fillMaxWidth(),
        )
        WidgetTargetBar(data = data)
        WidgetCaptionRow(data = data)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetCountButton(
                action = WidgetCountAction.DECREMENT,
                projectId = projectId,
                size = CounterWidgetDimens.ButtonSize,
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            WidgetCountButton(
                action = WidgetCountAction.INCREMENT,
                projectId = projectId,
                size = CounterWidgetDimens.ButtonSize,
            )
        }
    }
}

/** Leveä: miinus ja plus luvun molemmin puolin kuten laskurissa, tavoitepalkki alla. */
@androidx.compose.runtime.Composable
private fun LargeWidget(
    data: WidgetData,
    projectId: Long,
) {
    WidgetCard(projectId = projectId) {
        WidgetProjectName(data = data, fontSize = CounterWidgetDimens.NameSize, textAlign = TextAlign.Center)
        data.sectionName?.let { section ->
            WidgetCaption(text = section, modifier = GlanceModifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetCountButton(
                action = WidgetCountAction.DECREMENT,
                projectId = projectId,
                size = CounterWidgetDimens.LargeButtonSize,
            )
            WidgetCount(
                data = data,
                fontSize = CounterWidgetDimens.LargeCountSize,
                modifier = GlanceModifier.defaultWeight(),
            )
            WidgetCountButton(
                action = WidgetCountAction.INCREMENT,
                projectId = projectId,
                size = CounterWidgetDimens.LargeButtonSize,
            )
        }
        WidgetTargetBar(data = data)
        WidgetCaptionRow(data = data)
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

/** Neliön pehmuste: aina tiiviimpi kuin muissa koissa, isolla fontilla sama minimi kuin muualla. */
@androidx.compose.runtime.Composable
private fun mediumVerticalPadding(): Dp = minOf(widgetVerticalPadding(), CounterWidgetDimens.MediumVerticalPadding)

@androidx.compose.runtime.Composable
private fun widgetVerticalPadding(): Dp =
    if (LocalContext.current.resources.configuration.fontScale > LARGE_FONT_SCALE) {
        CounterWidgetDimens.LargeFontVerticalPadding
    } else {
        CounterWidgetDimens.VerticalPadding
    }

/**
 * Reunaton pinta, jonka taustalla on projektin lankavärinen sileäneule. Kulmat seuraavat
 * käynnistimen widget-kulmia (Android 12+), ja koko pinta avaa projektin laskurin.
 */
@androidx.compose.runtime.Composable
private fun WidgetCard(
    projectId: Long?,
    openProUpgrade: Boolean = false,
    onClick: Action? = null,
    horizontalPadding: Dp = CounterWidgetDimens.HorizontalPadding,
    verticalPadding: Dp = widgetVerticalPadding(),
    content: @androidx.compose.runtime.Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val clickAction =
        onClick ?: actionStartActivity(
            if (openProUpgrade) {
                MainActivity.createWidgetProPromptLaunchIntent(context)
            } else {
                MainActivity.createCounterLaunchIntent(context = context, projectId = projectId)
            },
        )
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .then(systemWidgetCorners())
                .background(GlanceTheme.colors.surface)
                .clickable(clickAction),
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_knit_texture),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(widgetYarnColor(projectId)),
        )
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

private fun systemWidgetCorners(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        GlanceModifier
    }

/** Sama projektikohtainen lankaväri kuin projektikortin tilkussa, teemakohtaisesta paletista. */
private fun widgetYarnColor(projectId: Long?): ColorProvider {
    val id = projectId ?: 0L
    return DayNightColorProvider(
        day = yarnColorForId(id, LightYarnColors),
        night = yarnColorForId(id, YarnColors),
    )
}

@androidx.compose.runtime.Composable
private fun WidgetProjectName(
    data: WidgetData,
    fontSize: TextUnit,
    textAlign: TextAlign,
) {
    Text(
        text = data.projectName,
        modifier = GlanceModifier.fillMaxWidth(),
        style =
            TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
                textAlign = textAlign,
            ),
        maxLines = 1,
    )
}

@androidx.compose.runtime.Composable
private fun WidgetCount(
    data: WidgetData,
    fontSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = data.count.toString(),
        modifier = modifier,
        style =
            TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            ),
        maxLines = 1,
    )
}

@androidx.compose.runtime.Composable
private fun WidgetCaption(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        style =
            TextStyle(
                fontSize = CounterWidgetDimens.CaptionSize,
                color = GlanceTheme.colors.onSurfaceVariant,
                textAlign = textAlign,
            ),
        maxLines = 1,
    )
}

/** Laskurin yksikkö versaalein kuten osio-otsikoissa ("ROWS"). */
@androidx.compose.runtime.Composable
private fun widgetUnitLabel(data: WidgetData): String {
    val context = LocalContext.current
    val label =
        data.customLabel.takeIf { data.labelType == MainCounterLabelType.CUSTOM }
            ?: context.getString(data.labelType.labelRes)
    return label.uppercase(context.resources.configuration.locales[0])
}

/** "of 72" kuten laskurin tavoiterivillä, tai null ilman tavoitetta. */
@androidx.compose.runtime.Composable
private fun widgetTargetLabel(data: WidgetData): String? {
    val target = data.targetRows?.takeIf { it > 0 } ?: return null
    return LocalContext.current.getString(R.string.counter_target_of_format, target)
}

/** Tavoitteen palkki luvun alla; pohja on onSurfacen alfa kuten ProjectProgressBarissa. */
@androidx.compose.runtime.Composable
private fun WidgetTargetBar(data: WidgetData) {
    val target = data.targetRows?.takeIf { it > 0 } ?: return
    val completed = data.count >= target
    Spacer(modifier = GlanceModifier.height(CounterWidgetDimens.ProgressTopSpacing))
    LinearProgressIndicator(
        progress = (data.count.toFloat() / target).coerceIn(0f, 1f),
        modifier = GlanceModifier.fillMaxWidth().height(CounterWidgetDimens.ProgressHeight),
        color = if (completed) GlanceTheme.colors.tertiary else GlanceTheme.colors.primary,
        backgroundColor = WidgetProgressTrack,
    )
}

/** Palkin alla täysleveänä: yksikkö vasemmalla ja "of 72" oikealla, ilman tavoitetta keskellä. */
@androidx.compose.runtime.Composable
private fun WidgetCaptionRow(data: WidgetData) {
    val unit = widgetUnitLabel(data)
    val target = widgetTargetLabel(data)
    if (target == null) {
        WidgetCaption(text = unit, modifier = GlanceModifier.fillMaxWidth(), textAlign = TextAlign.Center)
        return
    }
    Spacer(modifier = GlanceModifier.height(CounterWidgetDimens.SectionSpacing))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        WidgetCaption(text = unit, modifier = GlanceModifier.defaultWeight())
        WidgetCaption(text = target, textAlign = TextAlign.End)
    }
}

private val WidgetProgressTrack =
    DayNightColorProvider(
        day = LightTextPrimary.copy(alpha = ProjectListDimens.ProgressTrackAlpha),
        night = TextPrimary.copy(alpha = ProjectListDimens.ProgressTrackAlpha),
    )

/** Laskurinäytön 3D-nappi sellaisenaan; kosketusalue on vähintään 48 dp. */
@androidx.compose.runtime.Composable
private fun WidgetCountButton(
    action: WidgetCountAction,
    projectId: Long,
    size: Dp,
    imageInset: Dp = 0.dp,
) {
    val context = LocalContext.current
    val onClick =
        actionSendBroadcast(
            when (action) {
                WidgetCountAction.DECREMENT -> CounterWidgetActions.decrementIntent(context, projectId)
                WidgetCountAction.INCREMENT -> CounterWidgetActions.incrementIntent(context, projectId)
            },
        )
    val actionDescription =
        context.getString(
            when (action) {
                WidgetCountAction.DECREMENT -> R.string.counter_decrease
                WidgetCountAction.INCREMENT -> R.string.counter_increase
            },
        )
    // Nimi on kuvassa: RemoteViews ei välitä Boxin semantiikkaa, ja TalkBack lukee napautettavan
    // laatikon nimen sen sisällöstä.
    Box(
        modifier = GlanceModifier.size(size).clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider =
                ImageProvider(
                    when (action) {
                        WidgetCountAction.DECREMENT -> R.drawable.counter_minus_button
                        WidgetCountAction.INCREMENT -> R.drawable.counter_plus_button
                    },
                ),
            contentDescription = actionDescription,
            modifier = GlanceModifier.fillMaxSize().padding(imageInset),
            contentScale = ContentScale.Fit,
        )
    }
}

private enum class WidgetCountAction {
    DECREMENT,
    INCREMENT,
}

private const val LARGE_FONT_SCALE = 1.3f
