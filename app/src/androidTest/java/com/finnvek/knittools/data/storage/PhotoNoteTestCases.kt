package com.finnvek.knittools.data.storage

import android.content.Context
import androidx.room.Room
import com.finnvek.knittools.data.local.KnitToolsDatabase

internal fun photoNoteTestDatabase(
    context: Context,
    name: String,
): KnitToolsDatabase = Room.databaseBuilder(context, KnitToolsDatabase::class.java, name).build()

internal fun photoNoteEmojiCases(): List<Pair<String, String>> =
    listOf(
        ("a".repeat(98) + "\uD83D\uDE00") to ("a".repeat(98) + "\uD83D\uDE00"),
        ("a".repeat(99) + "\uD83D\uDE00") to "a".repeat(99),
    )
