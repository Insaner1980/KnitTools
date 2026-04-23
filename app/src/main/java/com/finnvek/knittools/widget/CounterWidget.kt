package com.finnvek.knittools.widget

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import com.finnvek.knittools.ui.theme.LightSurfaceHigh
import com.finnvek.knittools.ui.theme.LightTextPrimary
import com.finnvek.knittools.ui.theme.LightTextSecondary
import com.finnvek.knittools.ui.theme.OnPrimary
import com.finnvek.knittools.ui.theme.Primary
import com.finnvek.knittools.ui.theme.PrimaryContainer
import com.finnvek.knittools.ui.theme.Surface
import com.finnvek.knittools.ui.theme.SurfaceHigh
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

        // Uudelle widget-instanssille peilataan viimeisin tunnettu tila, tai haetaan
        // ensimmäinen projekti jos mitään tilaa ei ole vielä alustettu.
        if (isPro && widgetData.projectId == 0L) {
            val sharedWidgetData = CounterWidgetState.load(context)
            when {
                sharedWidgetData.projectId > 0L -> {
                    CounterWidgetState.saveGlance(context, id, sharedWidgetData)
                }

                else -> {
                    val repository = entryPoint.counterRepository()
                    repository.getFirstProject()?.let { project ->
                        val initialData = project.toWidgetData()
                        CounterWidgetState.save(context, initialData)
                        CounterWidgetState.saveGlance(context, id, initialData)
                    }
                }
            }
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val data = CounterWidgetState.fromPreferences(context, prefs)
            val darkScheme =
                darkColorScheme(
                    primary = Primary,
                    onPrimary = OnPrimary,
                    surface = Surface,
                    surfaceVariant = SurfaceHigh,
                    onSurface = TextPrimary,
                    onSurfaceVariant = TextSecondary,
                    tertiary = PrimaryContainer,
                )
            val lightScheme =
                lightColorScheme(
                    primary = Primary,
                    onPrimary = OnPrimary,
                    surface = LightSurface,
                    surfaceVariant = LightSurfaceHigh,
                    onSurface = LightTextPrimary,
                    onSurfaceVariant = LightTextSecondary,
                    tertiary = Primary,
                )
            GlanceTheme(colors = ColorProviders(dark = darkScheme, light = lightScheme)) {
                if (!isPro) {
                    ProRequiredWidget(context)
                } else {
                    val size = LocalSize.current
                    val projectId = data.projectId.takeIf { it > 0L }
                    when {
                        size.width >= LARGE_SIZE.width && size.height >= LARGE_SIZE.height -> {
                            LargeWidget(context = context, data = data, projectId = projectId)
                        }

                        size.width >= MEDIUM_SIZE.width && size.height >= MEDIUM_SIZE.height -> {
                            MediumWidget(context = context, data = data, projectId = projectId)
                        }

                        else -> {
                            SmallWidget(context = context, data = data, projectId = projectId)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val SMALL_SIZE = DpSize(120.dp, 48.dp)
        val MEDIUM_SIZE = DpSize(160.dp, 160.dp)
        val LARGE_SIZE = DpSize(300.dp, 160.dp)
    }
}

@androidx.compose.runtime.Composable
private fun ProRequiredWidget(context: Context) {
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(
                    actionStartActivity(
                        MainActivity.createCounterLaunchIntent(context = context, projectId = null),
                    ),
                ).padding(12.dp),
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

@androidx.compose.runtime.Composable
private fun SmallWidget(
    context: Context,
    data: WidgetData,
    projectId: Long?,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(
                    actionStartActivity(
                        MainActivity.createCounterLaunchIntent(
                            context = context,
                            projectId = projectId,
                        ),
                    ),
                ).padding(horizontal = 14.dp, vertical = 6.dp),
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
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(
                    actionStartActivity(
                        MainActivity.createCounterLaunchIntent(
                            context = context,
                            projectId = projectId,
                        ),
                    ),
                ).padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        WidgetHeader(data = data, fontSize = 13.sp, centered = true, maxLines = 2)
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
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetActionButton(
                text = "−",
                size = 42.dp,
                contentDescription = context.getString(R.string.counter_decrease),
                onClick = actionSendBroadcast(CounterWidgetActions.decrementIntent(context)),
            )
            Spacer(modifier = GlanceModifier.width(16.dp))
            WidgetActionButton(
                text = "+",
                size = 42.dp,
                contentDescription = context.getString(R.string.counter_increase),
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
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(
                    actionStartActivity(
                        MainActivity.createCounterLaunchIntent(
                            context = context,
                            projectId = projectId,
                        ),
                    ),
                ).padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        WidgetHeader(data = data, fontSize = 14.sp, centered = true, maxLines = 2)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatPrimaryCount(data),
                style =
                    TextStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(14.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetActionButton(
                text = "−",
                size = 50.dp,
                contentDescription = context.getString(R.string.counter_decrease),
                onClick = actionSendBroadcast(CounterWidgetActions.decrementIntent(context)),
            )
            Spacer(modifier = GlanceModifier.width(18.dp))
            WidgetActionButton(
                text = "+",
                size = 50.dp,
                contentDescription = context.getString(R.string.counter_increase),
                onClick = actionSendBroadcast(CounterWidgetActions.incrementIntent(context)),
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@androidx.compose.runtime.Composable
private fun WidgetHeader(
    data: WidgetData,
    fontSize: androidx.compose.ui.unit.TextUnit,
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
private fun WidgetActionButton(
    text: String,
    size: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    onClick: androidx.glance.action.Action,
) {
    Box(
        modifier =
            GlanceModifier
                .size(size)
                .cornerRadius(size / 2)
                .background(GlanceTheme.colors.tertiary)
                .clickable(onClick),
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
