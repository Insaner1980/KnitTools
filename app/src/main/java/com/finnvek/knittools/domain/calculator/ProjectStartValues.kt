package com.finnvek.knittools.domain.calculator

const val MAX_PROJECT_START_COUNT = 99_999
const val MAX_TARGET_ROWS = 99_999

data class ProjectStartValues(
    val initialCount: Int = 0,
    val targetRows: Int? = null,
)

sealed interface TargetInput {
    data object NoTarget : TargetInput

    data class Valid(
        val value: Int,
    ) : TargetInput

    data object Invalid : TargetInput
}

/** Tyhjä tarkoittaa uuden projektin nollalukemaa. */
fun parseProjectStartCount(text: String): Int? =
    when {
        text.isEmpty() -> 0
        text.all(Char::isDigit) -> text.toIntOrNull()?.takeIf { it in 0..MAX_PROJECT_START_COUNT }
        else -> null
    }

/** Tyhjä jättää tavoitteen asettamatta, mutta asetetun tavoitteen on oltava positiivinen. */
fun parseTargetRows(text: String): TargetInput {
    if (text.isEmpty()) return TargetInput.NoTarget
    val count = text.takeIf { it.all(Char::isDigit) }?.toIntOrNull()
    return if (count != null && count in 1..MAX_TARGET_ROWS) TargetInput.Valid(count) else TargetInput.Invalid
}
