package com.finnvek.knittools.repository

import android.content.Context
import android.os.Process
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.backup.BackupFormat
import com.finnvek.knittools.data.backup.BackupTables
import com.finnvek.knittools.data.storage.AppFileStorage
import java.io.File

internal class BackupRestoreProcessProbe(
    private val directory: File,
) {
    private var armed = false
    private var changed = false
    private val phase = requireNotNull(phase())

    fun arm(
        database: SupportSQLiteDatabase,
        context: Context,
    ) {
        BackupTables.export(database, File(directory, "before"), { _, _ -> }, check = {})
        val hashes =
            BackupTables
                .references(database)
                .map {
                    checkNotNull(
                        AppFileStorage.resolveAppOwnedFile(context, android.net.Uri.parse(it)),
                    ).canonicalFile
                }.distinct()
                .map { BackupFormat.digest(it) {} }
                .sorted()
        File(directory, "file-hashes.txt").writeText(hashes.joinToString("\n"))
        armed = true
    }

    val factory =
        SupportSQLiteOpenHelper.Factory { configuration ->
            val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)

            fun wrap(database: SupportSQLiteDatabase): SupportSQLiteDatabase =
                object : SupportSQLiteDatabase by database {
                    override fun execSQL(sql: String) {
                        database.execSQL(sql)
                        if (armed && sql.startsWith("DELETE FROM ")) {
                            changed = true
                            if (phase == "precommit") pause(database.inTransaction())
                        }
                    }

                    override fun endTransaction() {
                        database.endTransaction()
                        if (armed && changed && phase == "postcommit" && !database.inTransaction()) pause(false)
                    }
                }
            object : SupportSQLiteOpenHelper by helper {
                override val writableDatabase get() = wrap(helper.writableDatabase)

                override val readableDatabase get() = wrap(helper.readableDatabase)
            }
        }

    private fun pause(inTransaction: Boolean): Nothing {
        check(inTransaction == (phase == "precommit"))
        File(directory, "checkpoint.txt").writeText("${Process.myPid()} $phase $inTransaction")
        while (true) Thread.sleep(1000)
    }

    companion object {
        fun phase(): String? =
            InstrumentationRegistry.getArguments().getString("backupCrashPhase")?.also {
                check(it == "precommit" || it == "postcommit")
                check(ApplicationProvider.getApplicationContext<Context>().packageName.endsWith(".activepolicytest"))
            }
    }
}
