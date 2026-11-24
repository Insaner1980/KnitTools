package com.finnvek.knittools.ui

import androidx.compose.ui.text.TextLayoutResult
import java.text.BreakIterator
import java.util.Locale

internal fun TextLayoutResult.linesEndingInsideWords(locale: Locale): List<String> {
    val text = layoutInput.text.text
    val words = BreakIterator.getWordInstance(locale).apply { setText(text) }
    return (0 until lineCount - 1).mapNotNull { line ->
        val end = getLineEnd(line, visibleEnd = true)
        text.substring(getLineStart(line), end).takeUnless { words.isBoundary(end) }
    }
}
