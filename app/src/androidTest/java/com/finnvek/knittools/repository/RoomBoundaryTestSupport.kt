package com.finnvek.knittools.repository

import android.content.Context
import androidx.room.Room
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference

internal fun auditDatabase(context: Context): KnitToolsDatabase =
    Room
        .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
        .addCallback(ActiveSessionSchemaConstraints.callback)
        .addCallback(PatternAnnotationSchemaConstraints.callback)
        .addCallback(ProjectDocumentSchemaConstraints.callback)
        .build()

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
