package com.finnvek.knittools.ui.screens.notes

import com.finnvek.knittools.data.storage.NotesDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
private data class NotesDraftPatch(
    val baseHash: String,
    val resultHash: String,
    val prefix: Int,
    val suffix: Int,
    val replacement: String,
) {
    fun applyTo(base: String): NotesDraft? {
        if (prefix !in 0..base.length || suffix !in 0..base.length - prefix) return null
        val notes = base.replaceRange(prefix, base.length - suffix, replacement)
        return if (notes.notesHash() == resultHash) NotesDraft(notes, base) else null
    }
}

internal fun encodeNotesDraftPatch(
    base: String,
    notes: String,
    maxChars: Int,
): String? {
    val commonLength = minOf(base.length, notes.length)
    var prefix = 0
    while (prefix < commonLength && base[prefix] == notes[prefix]) prefix++
    var suffix = 0
    while (suffix < commonLength - prefix && base[base.lastIndex - suffix] == notes[notes.lastIndex - suffix]) suffix++
    val end = notes.length - suffix
    if (end - prefix > maxChars) return null
    val patch = NotesDraftPatch(base.notesHash(), notes.notesHash(), prefix, suffix, notes.substring(prefix, end))
    return Json.encodeToString(patch).takeIf { it.length <= maxChars }
}

internal fun restoreNotesDraftPatch(
    encoded: String,
    stored: String,
): NotesDraft? {
    val patch =
        try {
            Json.decodeFromString<NotesDraftPatch>(encoded)
        } catch (_: SerializationException) {
            return null
        }
    return when (stored.notesHash()) {
        patch.resultHash -> NotesDraft(stored, stored)
        patch.baseHash -> patch.applyTo(stored)
        else -> null
    }
}

private fun String.notesHash(): String = MessageDigest.getInstance("SHA-256").digest(encodeToByteArray()).toHexString()
