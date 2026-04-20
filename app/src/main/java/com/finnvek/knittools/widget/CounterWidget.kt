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
            setOf(SMALL_SIZE, MEDIUM_SIZE),
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
                CounterWidgetState.save(context, project.name, project.count, project.id)
                prefs = WidgetData(project.name, project.count, project.id)
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
                    val isSmall = size.width < MEDIUM_SIZE.width

                    if (isSmall) {
                        SmallWidget(
                            context = context,
                            projectName = prefs.projectName,
                            count = prefs.count,
                            projectId = prefs.projectId.takeIf { it > 0L },
                        )
                    } else {
                        MediumWidget(
                            context = context,
                            projectName = prefs.projectName,
                            count = prefs.count,
                            projectId = prefs.projectId.takeIf { it > 0L },
                        )
                    }
                }
            }
        }
    }

    companion object {
        val SMALL_SIZE = DpSize(120.dp, 48.dp)
        val MEDIUM_SIZE = DpSize(180.dp, 120.dp)
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
    projectName: String,
    count: Int,
    projectId: Long? = null,
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
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = projectName,
                    style =
                        TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                    maxLines = 1,
                )
                Text(
                    text = "$count",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                        ),
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MediumWidget(
    context: Context,
    projectName: String,
    count: Int,
    projectId: Long? = null,
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
        Text(
            text = projectName,
            style =
                TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        Text(
            text = "$count",
            style =
                TextStyle(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetButton(
                text = "−",
                onClick =
                    actionSendBroadcast(
                        CounterWidgetActions.decrementIntent(context),
                    ),
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            WidgetButton(
                text = "+",
                onClick =
                    actionSendBroadcast(
                        CounterWidgetActions.incrementIntent(context),
                    ),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetButton(
    text: String,
    onClick: androidx.glance.action.Action,
) {
    Box(
        modifier =
            GlanceModifier
                .size(40.dp)
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
