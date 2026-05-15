package com.finnvek.knittools.ui.screens.pattern

internal fun String.isLocalPatternUri(): Boolean = startsWith("content://") || startsWith("file://")
