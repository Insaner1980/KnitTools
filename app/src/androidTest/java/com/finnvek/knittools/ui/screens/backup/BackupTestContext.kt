package com.finnvek.knittools.ui.screens.backup

import android.content.Context
import android.content.ContextWrapper
import java.io.File

internal class BackupTestContext(
    app: Context,
    private val root: File,
) : ContextWrapper(app) {
    override fun getApplicationContext(): Context = this

    override fun getFilesDir() = File(root, "files").apply { mkdirs() }

    override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
}
