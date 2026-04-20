package com.finnvek.knittools.widget

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.OnPrimary
import com.finnvek.knittools.ui.theme.Primary
import com.finnvek.knittools.ui.theme.Surface
import com.finnvek.knittools.ui.theme.TextPrimary
import com.finnvek.knittools.ui.theme.TextSecondary
import dagger.hilt.android.EntryPointAccessors

class CounterWidget : GlanceAppWidget() {
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

        var prefs = CounterWidgetState.load(context)

        // Ensimmäinen renderöinti — hae data Roomista
        if (isPro && prefs.projectId == 0L) {
            val repository = entryPoint.counterRepository()
            repository.getFirstProject()?.let { project ->
                CounterWidgetState.save(context, project)
                prefs = project.toWidgetData()
            }
        }

        provideContent {
            val widgetScheme =
                darkColorScheme(
                    primary = Primary,
                    onPrimary = OnPrimary,
                    surface = Surface,
                    onSurface = TextPrimary,
                    onSurfaceVariant = TextSecondary,
                )
            GlanceTheme(colors = ColorProviders(dark = widgetScheme, light = widgetScheme)) {
                if (!isPro) {
                    ProRequiredWidget(context)
                } else {
                    val size = LocalSize.current
                    val projectId = prefs.projectId.takeIf { it > 0L }
                    when {
                        size.width >= LARGE_SIZE.width && size.height >= LARGE_SIZE.height ->
                            LargeWidget(context = context, data = prefs, projectId = projectId)
                        size.width >= MEDIUM_SIZE.width && size.height >= MEDIUM_SIZE.height ->
                            MediumWidget(context = context, data = prefs, projectId = projectId)
                        else ->
                            SmallWidget(context = context, data = prefs, projectId = projectId)
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
    Box(
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
                ).padding(8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = data.projectName,
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                maxLines = 1,
            )
            Text(
                text = formatCountWithTarget(data),
                style =
                    TextStyle(
                        fontSize = 24.sp,
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
                ).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WidgetHeader(data = data, fontSize = 12.sp)

        Spacer(modifier = GlanceModifier.defaultWeight())

        Text(
            text = formatCountWithTarget(data),
            style =
                TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetButton(
                text = "−",
                size = 40.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.decrementIntent(context)),
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            WidgetButton(
                text = "+",
                size = 40.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.incrementIntent(context)),
            )
        }
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
                ).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        WidgetHeader(data = data, fontSize = 13.sp)

        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetButton(
                text = "−",
                size = 48.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.decrementIntent(context)),
            )

            Box(
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatCountWithTarget(data),
                    style =
                        TextStyle(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                        ),
                    maxLines = 1,
                )
            }

            WidgetButton(
                text = "+",
                size = 48.dp,
                onClick = actionSendBroadcast(CounterWidgetActions.incrementIntent(context)),
            )
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        if (data.stitchTrackingEnabled && data.totalStitches != null) {
            Text(
                text = context.getString(
                    R.string.widget_stitches_format,
                    data.currentStitch,
                    data.totalStitches,
                ),
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                maxLines = 1,
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetHeader(
    data: WidgetData,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    Text(
        text = data.projectName,
        style =
            TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
        maxLines = 1,
    )
    data.sectionName?.let { section ->
        Text(
            text = section,
            style =
                TextStyle(
                    fontSize = (fontSize.value - 2f).sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            maxLines = 1,
        )
    }
}

private fun formatCountWithTarget(data: WidgetData): String =
    if (data.targetRows != null) {
        "${data.count} / ${data.targetRows}"
    } else {
        "${data.count}"
    }

@androidx.compose.runtime.Composable
private fun WidgetButton(
    text: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: androidx.glance.action.Action,
) {
    Box(
        modifier =
            GlanceModifier
                .size(size)
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onPrimary,
                ),
        )
    }
}
