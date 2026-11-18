package com.finnvek.knittools.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.ui.descendants
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Real RemoteViews in a disposable host; the user's launcher is never edited. */
class WidgetHostRuntimeTest {
    @Test
    fun responsiveSizesRenderAndButtonWritesThroughReceiver() =
        runBlocking(Dispatchers.IO) {
            val app = ApplicationProvider.getApplicationContext<App>()
            check(app.packageName.endsWith(".activepolicytest"))
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val entryPoint = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java)
            val repository = entryPoint.counterRepository()
            ProPolicyTestControl.setPurchased(true)
            try {
                withTimeout(15_000) { entryPoint.proManager().proState.first { it.isPro } }
                // Pahin tapaus: pisin yksikkösana (Repeats, esim. "WIEDERHOLUNGEN") ja nelinumeroinen tavoite.
                val created =
                    repository.createProject(
                        PROJECT_NAME,
                        mainCounterLabelType = MainCounterLabelType.REPEATS,
                        canCreateAdditionalProjects = true,
                    )
                check(created is ProjectCreationResult.Created)
                val projectId = created.projectId
                repository.setTargetRows(projectId, WORST_CASE_TARGET)
                val host = AppWidgetHost(app, 110013)
                val manager = AppWidgetManager.getInstance(app)
                val ids = mutableListOf<Int>()
                val scenario = ActivityScenario.launch(ComponentActivity::class.java)
                try {
                    val widgetData = checkNotNull(repository.getProject(projectId)).toWidgetData()
                    CounterWidgetState.save(app, widgetData)
                    instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
                    host.startListening()
                    for ((width, height) in listOf(120 to 48, 120 to 80, 160 to 160, 300 to 160)) {
                        val id = host.allocateAppWidgetId().also { ids.add(it) }
                        val options =
                            Bundle().apply {
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
                            }
                        assertTrue(
                            manager.bindAppWidgetIdIfAllowed(
                                id,
                                ComponentName(app, CounterWidgetReceiver::class.java),
                                options,
                            ),
                        )
                        lateinit var view: AppWidgetHostView
                        scenario.onActivity { activity ->
                            view = host.createView(activity, id, manager.getAppWidgetInfo(id))
                            val density = activity.resources.displayMetrics.density
                            // Widget options describe content size; the host also reserves platform padding.
                            val padding =
                                AppWidgetHostView.getDefaultPaddingForWidget(
                                    activity,
                                    ComponentName(app, CounterWidgetReceiver::class.java),
                                    null,
                                )
                            val container = FrameLayout(activity)
                            container.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    (width * density).toInt() + padding.left + padding.right,
                                    (height * density).toInt() + padding.top + padding.bottom,
                                ),
                            )
                            activity.setContentView(container)
                        }
                        CounterWidgetState.syncAll(app, widgetData)
                        await {
                            var found = false
                            scenario.onActivity {
                                found =
                                    descendants(view).filterIsInstance<TextView>().any { it.text.toString() == "0" }
                            }
                            found
                        }
                        scenario.onActivity { activity ->
                            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                            view.draw(Canvas(bitmap))
                            File(activity.getExternalFilesDir(null), "widget-${width}x$height.png").outputStream().use {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                            bitmap.recycle()
                            val count =
                                descendants(view).filterIsInstance<TextView>().single { it.text.toString() == "0" }
                            assertFullyVisible(count)
                            val ink = Rect()
                            count.paint.getTextBounds(count.text.toString(), 0, count.length(), ink)
                            assertTrue("Counter glyph top is visible", count.baseline + ink.top >= 0)
                            assertTrue("Counter glyph bottom is visible", count.baseline + ink.bottom <= count.height)
                            // Käännetyt tekstit (yksikkö, "of 9999") mahtuvat kokonaan; vain käyttäjän
                            // projektinimi saa katketa.
                            descendants(view)
                                .filterIsInstance<TextView>()
                                .filter { it.isShown && it.text.toString() != PROJECT_NAME }
                                .forEach { text ->
                                    val available = text.width - text.totalPaddingLeft - text.totalPaddingRight
                                    assertTrue(
                                        "Text fits without truncation in ${width}x$height: ${text.text}",
                                        text.paint.measureText(text.text.toString()) <= available + 1,
                                    )
                                }
                            // Plus on kaikissa koissa laskurin 3D-kuva, joten se löytyy saavutettavasta nimestä.
                            val increase = activity.getString(R.string.counter_increase)
                            val plus = descendants(view).first { it.contentDescription?.toString() == increase }
                            var action: View = plus
                            while (!action.isClickable && action.parent is View) action = action.parent as View
                            assertTrue("Visible increment action", action.isShown)
                            assertFullyVisible(action)
                            assertTrue(
                                "Increment target remains 48 dp",
                                action.height >= 48 * activity.resources.displayMetrics.density - 1,
                            )
                            assertTrue("Increment click", action.performClick())
                        }
                        await {
                            runBlocking {
                                app.database
                                    .get()
                                    .counterProjectDao()
                                    .getProject(projectId)
                                    ?.count == 1
                            }
                        }
                        repository.applyWidgetCountChange(projectId, false)
                        host.deleteAppWidgetId(id)
                        ids.remove(id)
                    }
                } finally {
                    ids.forEach(host::deleteAppWidgetId)
                    host.stopListening()
                    instrumentation.uiAutomation.dropShellPermissionIdentity()
                    scenario.close()
                    repository.deleteProject(projectId)
                }
            } finally {
                ProPolicyTestControl.setPurchased(false)
            }
        }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (!condition()) {
            assertTrue("Widget state timed out", SystemClock.elapsedRealtime() < deadline)
            SystemClock.sleep(100)
        }
    }

    private fun assertFullyVisible(view: View) {
        val visible = Rect()
        assertTrue("Widget element is visible", view.getGlobalVisibleRect(visible))
        assertTrue(
            "Widget element is not vertically clipped: ${visible.height()}/${view.height}",
            visible.height() >= view.height - 1,
        )
    }

    private companion object {
        const val PROJECT_NAME = "Synthetic long widget project"
        const val WORST_CASE_TARGET = 9999
    }
}
