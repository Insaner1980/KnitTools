package com.finnvek.knittools.data.storage

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

internal abstract class PdfTestContentProvider : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri) = "application/pdf"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0
}
