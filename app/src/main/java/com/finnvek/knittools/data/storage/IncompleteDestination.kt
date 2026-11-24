package com.finnvek.knittools.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException

/**
 * Käyttäjän valitsema vientikohde (CreateDocument) luodaan tyhjänä ennen kirjoitusta. Jos vienti
 * epäonnistuu, tyhjänä luotu kohde poistetaan, jottei kansioon jää rikkinäistä tiedostoa.
 * Valmiiksi sisältöä sisältävään kohteeseen ei kosketa ennen kuin kirjoitus on alkanut.
 */
internal object IncompleteDestination {
    fun isEmpty(
        context: Context,
        destination: Uri,
    ): Boolean =
        when (destination.scheme) {
            "file" -> {
                destination.path?.let(::File)?.let { it.isFile && it.length() == 0L } == true
            }

            "content" -> {
                runCatching {
                    context.contentResolver
                        .query(destination, arrayOf(OpenableColumns.SIZE), null, null, null)
                        ?.use { cursor ->
                            cursor.moveToFirst() && !cursor.isNull(0) && cursor.getLong(0) == 0L
                        } == true
                }.getOrDefault(false)
            }

            else -> {
                false
            }
        }

    /** Heittää virheen, jos poisto epäonnistuu; kaikki palveluntarjoajat eivät salli poistoa. */
    fun delete(
        context: Context,
        destination: Uri,
    ) {
        val resolver = context.contentResolver
        when {
            destination.scheme == "file" -> {
                val partialFile = destination.path?.let(::File) ?: return
                if (!partialFile.delete() && partialFile.exists()) {
                    throw IOException("Could not remove incomplete destination")
                }
            }

            DocumentsContract.isDocumentUri(context, destination) -> {
                DocumentsContract.deleteDocument(resolver, destination)
            }

            else -> {
                resolver.delete(destination, null, null)
            }
        }
    }

    fun deleteQuietly(
        context: Context,
        destination: Uri,
    ) {
        try {
            delete(context, destination)
        } catch (_: Exception) {
            // Virheviesti ohjaa käyttäjää poistamaan keskeneräisen tiedoston itse.
        }
    }
}
