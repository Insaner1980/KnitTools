package com.finnvek.knittools

import androidx.lifecycle.SavedStateHandle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun SavedStateHandle.serializedCopy(): SavedStateHandle {
    val bytes = ByteArrayOutputStream()
    ObjectOutputStream(bytes).use { output ->
        output.writeObject(keys().associateWith { get<Any?>(it) })
    }
    val values = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
    @Suppress("UNCHECKED_CAST")
    return SavedStateHandle(values as Map<String, Any?>)
}
