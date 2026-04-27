package com.finnvek.knittools.widget

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.LightSurface
import com.finnvek.knittools.ui.theme.LightTextMuted
import com.finnvek.knittools.ui.theme.LightTextPrimary
import com.finnvek.knittools.ui.theme.LightTextSecondary
import com.finnvek.knittools.ui.theme.OnPrimary
import com.finnvek.knittools.ui.theme.Primary
import com.finnvek.knittools.ui.theme.PrimaryContainer
import com.finnvek.knittools.ui.theme.Surface
import com.finnvek.knittools.ui.theme.TextMuted
import com.finnvek.knittools.ui.theme.TextPrimary
import com.finnvek.knittools.ui.theme.TextSecondary
import dagger.hilt.android.EntryPointAccessors

class CounterWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode =
        SizeMode.Responsive(
            setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE),
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
        val isPro = entryPoint.proManager().isPro()
        val widgetData = CounterWidgetState.loadGlance(context, id)
        val initialWidgetData =
            if (isPro) resolveInitialWidgetData(context, id, entryPoint, widgetData) else widgetData

        provideContent {
            val prefs = currentState<Preferences>()
            val storedData = CounterWidgetState.fromPreferences(context, prefs)
            val data =
                if (initialWidgetData.projectId > 0L && initialWidgetData != storedData) {
                    initialWidgetData
                } else {
                    storedData
                }
            GlanceTheme(colors = ColorProviders(dark = WidgetDarkScheme, light = WidgetLightScheme)) {
                WidgetSizedContent(context = context, data = data, isPro = isPro)
            }
        }
    }

    private suspend fun resolveInitialWidgetData(
        context: Context,
        id: GlanceId,
        entryPoint: WidgetEntryPoint,
        widgetData: WidgetData,
    ): WidgetData {
        // Widgetit seuraavat samaa aktiivista projektia. Peilaa jaettu tila myös silloin,
        // kun Glance-instanssille on jäänyt vanha projekti aiemmasta renderöinnistä.
        val sharedWidgetData = CounterWidgetState.load(context)
        return when {
            sharedWidgetData.projectId > 0L -> {
                if (sharedWidgetData != widgetData) {
                    CounterWidgetState.saveGlance(context, id, sharedWidgetData)
                }
                sharedWidgetData
            }

            widgetData.projectId == 0L -> {
                entryPoint.counterRepository().getFirstProject()?.let { project ->
                    val initialData = project.toWidgetData()
                    CounterWidgetState.save(context, initialData)
                    CounterWidgetState.saveGlance(context, id, initialData)
                    initialData
                } ?: widgetData
            }

            else -> {
                widgetData
            }
        }
    }

    companion object {
        val SMALL_SIZE = DpSize(120.dp, 48.dp)
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
    context: Context,
    data: WidgetData,
    isPro: Boolean,
) {
    if (!isPro) {
        ProRequiredWidget(context)
        return
    }
    val size = LocalSize.current
    val projectId = data.projectId.takeIf { it > 0L }
    when {
        size.width >= CounterWidget.LARGE_SIZE.width && size.height >= CounterWidget.LARGE_SIZE.height -> {
            LargeWidget(context = context, data = data, projectId = projectId)
        }

        size.width >= CounterWidget.MEDIUM_SIZE.width && size.height >= CounterWidget.MEDIUM_SIZE.height -> {
            MediumWidget(context = context, data = data, projectId = projectId)
        }

        else -> {
            SmallWidget(context = context, data = data, projectId = projectId)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ProRequiredWidget(context: Context) {
    WidgetCard(
        context = context,
        projectId = null,
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(R.string.widget_pro_required),
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun SmallWidget(
    context: Context,
    data: WidgetData,
    projectId: Long?,
) {
    WidgetCard(
        context = context,
        projectId = projectId,
        horizontalPadding = 14.dp,
        verticalPadding = 4.dp,
        frame = WidgetCardFrame(outerPadding = 2.dp, borderWidth = 2.dp, cornerRadius = 18.dp),
    ) {
        WidgetHeader(data = data, fontSize = 12.sp)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatPrimaryCount(data),
                style =
                    TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                maxLines = 1,
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun MediumWidget(
    context: Context,
    data: WidgetData,
    projectId: Long?,
) {
    WidgetCard(
        context = context,
        projectId = projectId,
        horizontalPadding = 16.dp,
        verticalPadding = 10.dp,
    ) {
        WidgetHeader(data = data, fontSize = 13.sp, centered = true, maxLines = 2)
        WidgetTargetLabel(context = context, data = data, fontSize = 11.sp)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatPrimaryCount(data),
                style =
                    TextStyle(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                maxLines = 1,
            )
        }
        WidgetProgressBar(data = data, topSpacing = 8.dp)
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetActionButton(
                text = "−",
                size = 42.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.decrementIntent(context)),
            )
            Spacer(modifier = GlanceModifier.width(16.dp))
            WidgetActionButton(
                text = "+",
                size = 42.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.incrementIntent(context)),
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@androidx.compose.runtime.Composable
private fun LargeWidget(
    context: Context,
    data: WidgetData,
    projectId: Long?,
) {
    WidgetCard(
        context = context,
        projectId = projectId,
        horizontalPadding = 18.dp,
        verticalPadding = 8.dp,
    ) {
        WidgetHeader(data = data, fontSize = 14.sp, centered = true, maxLines = 2)
        WidgetTargetLabel(context = context, data = data, fontSize = 12.sp)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatPrimaryCount(data),
                style =
                    TextStyle(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                maxLines = 1,
            )
        }
        WidgetProgressBar(data = data, topSpacing = 6.dp)
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetActionButton(
                text = "−",
                size = 44.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.decrementIntent(context)),
            )
            Spacer(modifier = GlanceModifier.width(18.dp))
            WidgetActionButton(
                text = "+",
                size = 44.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.incrementIntent(context)),
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

data class WidgetCardFrame(
    val outerPadding: Dp = 2.dp,
    val borderWidth: Dp = 3.dp,
    val cornerRadius: Dp = 24.dp,
)

@androidx.compose.runtime.Composable
private fun WidgetCard(
    context: Context,
    projectId: Long?,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    frame: WidgetCardFrame = WidgetCardFrame(),
    content: @androidx.compose.runtime.Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .padding(frame.outerPadding)
                .cornerRadius(frame.cornerRadius)
                .background(GlanceTheme.colors.surfaceVariant)
                .clickable(
                    actionStartActivity(
                        MainActivity.createCounterLaunchIntent(
                            context = context,
                            projectId = projectId,
                        ),
                    ),
                ).padding(frame.borderWidth),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(frame.cornerRadius - frame.borderWidth)
                    .background(GlanceTheme.colors.surface)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetHeader(
    data: WidgetData,
    fontSize: TextUnit,
    centered: Boolean = false,
    maxLines: Int = 1,
) {
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    val modifier = if (centered) GlanceModifier.fillMaxWidth() else GlanceModifier
    Text(
        text = data.projectName,
        modifier = modifier,
        style =
            TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.tertiary,
                textAlign = textAlign,
            ),
        maxLines = maxLines,
    )
    data.sectionName?.let { section ->
        Text(
            text = section,
            modifier = modifier,
            style =
                TextStyle(
                    fontSize = (fontSize.value - 2f).sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = textAlign,
                ),
            maxLines = 1,
        )
    }
}

private fun formatPrimaryCount(data: WidgetData): String = data.count.toString()

@androidx.compose.runtime.Composable
private fun WidgetTargetLabel(
    context: Context,
    data: WidgetData,
    fontSize: TextUnit,
) {
    val target = data.targetRows ?: return
    if (target <= 0) return
    Text(
        text = context.getString(R.string.row_label_with_target, data.count, target),
        modifier = GlanceModifier.fillMaxWidth(),
        style =
            TextStyle(
                fontSize = fontSize,
                color = GlanceTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            ),
        maxLines = 1,
    )
}

@androidx.compose.runtime.Composable
private fun WidgetProgressBar(
    data: WidgetData,
    topSpacing: Dp,
) {
    val target = data.targetRows ?: return
    if (target <= 0) return
    val fraction = (data.count.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val completed = data.count >= target
    val fillColor = if (completed) GlanceTheme.colors.tertiary else GlanceTheme.colors.primary
    Spacer(modifier = GlanceModifier.height(topSpacing))
    Row(modifier = GlanceModifier.fillMaxWidth().height(4.dp)) {
        if (fraction > 0f) {
            Box(
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .height(4.dp)
                        .background(fillColor),
                content = {},
            )
        }
        if (fraction < 1f) {
            val emptyBox =
                GlanceModifier
                    .height(4.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
            Box(
                modifier = if (fraction > 0f) emptyBox.defaultWeight() else emptyBox.fillMaxWidth(),
                content = {},
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetActionButton(
    text: String,
    size: Dp,
    onClick: androidx.glance.action.Action,
) {
    Box(
        modifier =
            GlanceModifier
                .size(size)
                .cornerRadius(size / 2)
                .background(GlanceTheme.colors.primary)
                .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                GlanceModifier
                    .size(size - 2.dp)
                    .cornerRadius((size - 2.dp) / 2)
                    .background(GlanceTheme.colors.tertiary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style =
                    TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onPrimary,
                    ),
            )
        }
    }
}
