package com.finnvek.knittools.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object YarnLabelPhotoStorage {
    fun createImageFile(context: Context): Pair<File, Uri> {
        val dir = File(context.filesDir, "yarn_photos")
        dir.mkdirs()
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }
}
