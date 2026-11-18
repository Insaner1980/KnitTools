package com.finnvek.knittools.domain.model

const val WEB_PATTERN_TEXT_MAX_LENGTH = 120

sealed interface WebPatternTitleValidation {
    data class Valid(
        val value: String,
    ) : WebPatternTitleValidation

    data object Invalid : WebPatternTitleValidation
}

sealed interface WebPatternDesignerValidation {
    data class Valid(
        val value: String,
    ) : WebPatternDesignerValidation

    data object Invalid : WebPatternDesignerValidation
}

fun validateWebPatternTitle(value: String): WebPatternTitleValidation {
    if (value.any(Char::isUnsafeWebPatternTextCharacter)) return WebPatternTitleValidation.Invalid
    val trimmed = value.trim()
    return if (trimmed.isEmpty() || !trimmed.isSafeWebPatternText()) {
        WebPatternTitleValidation.Invalid
    } else {
        WebPatternTitleValidation.Valid(trimmed)
    }
}

fun validateWebPatternDesigner(value: String): WebPatternDesignerValidation {
    if (value.any(Char::isUnsafeWebPatternTextCharacter)) return WebPatternDesignerValidation.Invalid
    val trimmed = value.trim()
    return if (!trimmed.isSafeWebPatternText()) {
        WebPatternDesignerValidation.Invalid
    } else {
        WebPatternDesignerValidation.Valid(trimmed)
    }
}

internal fun Char.isUnsafeWebPatternTextCharacter(): Boolean =
    isISOControl() ||
        this == '\u061c' ||
        this == '\u200e' ||
        this == '\u200f' ||
        this == '\u2028' ||
        this == '\u2029' ||
        this in '\u202a'..'\u202e' ||
        this in '\u2066'..'\u2069'

private fun String.isSafeWebPatternText(): Boolean =
    length <= WEB_PATTERN_TEXT_MAX_LENGTH && none(Char::isUnsafeWebPatternTextCharacter)
