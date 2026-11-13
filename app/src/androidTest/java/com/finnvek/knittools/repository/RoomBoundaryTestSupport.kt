package com.finnvek.knittools.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference

internal fun notesTestContext(): Context =
    ApplicationProvider.getApplicationContext<Context>().also {
        check(it.packageName == "com.finnvek.knittools.notestest")
    }

internal fun auditDatabase(context: Context): KnitToolsDatabase =
    Room
        .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
        .addCallback(ActiveSessionSchemaConstraints.callback)
        .addCallback(PatternAnnotationSchemaConstraints.callback)
        .addCallback(ProjectDocumentSchemaConstraints.callback)
        .build()

internal fun interceptTestDatabase(
    wrap: (SupportSQLiteDatabase) -> SupportSQLiteDatabase,
): SupportSQLiteOpenHelper.Factory =
    SupportSQLiteOpenHelper.Factory { configuration ->
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        object : SupportSQLiteOpenHelper by helper {
            override val writableDatabase get() = wrap(helper.writableDatabase)

            override val readableDatabase get() = wrap(helper.readableDatabase)
        }
    }

internal suspend fun KnitToolsDatabase.deleteTestActiveSession(projectId: Long) {
    sessionDao().getActiveSession()?.takeIf { it.projectId == projectId }?.let {
        sessionDao().deleteActiveSession(it.sessionToken)
    }
}

internal open class BoundaryPause<Phase> {
    protected val phase = AtomicReference<Phase?>()
    var reached = CompletableDeferred<Unit>()
    var release = CompletableDeferred<Unit>()

    fun arm(value: Phase) {
        reached = CompletableDeferred()
        release = CompletableDeferred()
        phase.set(value)
    }

    protected suspend fun pause() {
        reached.complete(Unit)
        release.await()
    }
}

internal fun DatabaseTransactionRunner.pauseBeforeTransaction(
    entered: CompletableDeferred<Unit>,
    release: CompletableDeferred<Unit>,
): DatabaseTransactionRunner =
    object : DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            entered.complete(Unit)
            release.await()
            return this@pauseBeforeTransaction.run(block)
        }
    }

internal fun KnitToolsDatabase.auditSnapshot(): Map<String, List<List<String?>>> {
    val sqlite = openHelper.writableDatabase
    val tables =
        sqlite
            .query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                    "AND name NOT IN ('room_master_table','android_metadata')",
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
    return tables.associateWith { table ->
        sqlite.query("SELECT * FROM `$table`").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        (0 until cursor.columnCount).map {
                            if (cursor.isNull(it)) null else cursor.getString(it)
                        },
                    )
                }
            }.sortedBy { it.toString() }
        }
    }
}
