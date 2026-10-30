package com.finnvek.knittools.pro

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import java.io.File

/** Registered only by the networkless isolated-test manifest, never by the production app. */
class TrialNoticeProcessTestProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val appContext = checkNotNull(context)
        if (
            ProPolicyTestControl.isActive &&
            File(appContext.filesDir, "trial-notice-process-test").isFile
        ) {
            // Recreate only the synthetic billing input. Trial and notice state remain real.
            ProPolicyTestControl.setPurchased(false)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
