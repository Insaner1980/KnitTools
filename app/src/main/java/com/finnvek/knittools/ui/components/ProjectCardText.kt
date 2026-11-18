package com.finnvek.knittools.ui.components

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay

@Composable
internal fun projectListTargetStatusText(status: MainCounterTargetStatus): String =
    when (status) {
        is MainCounterTargetStatus.Remaining ->
            stringResource(
                R.string.counter_target_remaining_format,
                formatIntegerForDisplay(status.countSlot.count.toLong(), rememberCurrentLocale()),
            )
        MainCounterTargetStatus.Reached -> stringResource(R.string.counter_target_reached)
        is MainCounterTargetStatus.Past ->
            stringResource(
                R.string.counter_target_past_format,
                formatIntegerForDisplay(status.countSlot.count.toLong(), rememberCurrentLocale()),
            )
    }

internal fun projectListItemSecondaryLine(
    sectionName: String?,
    patternName: String?,
    projectName: String,
): String? {
    sectionName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }

    val trimmedName = projectName.trim()
    return patternName
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.equals(trimmedName, ignoreCase = true) }
        ?.takeUnless(::isRawPdfFileName)
}

private fun isRawPdfFileName(name: String): Boolean = name.endsWith(".pdf", ignoreCase = true)

@Composable
internal fun projectTimestampText(
    timestamp: Long,
    isCompleted: Boolean,
): String {
    val now = System.currentTimeMillis()
    val relativeTime =
        if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) {
            stringResource(R.string.just_now)
        } else {
            DateUtils
                .getRelativeTimeSpanString(
                    timestamp,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString()
        }
    return stringResource(
        if (isCompleted) R.string.project_completed_format else R.string.project_updated_format,
        relativeTime,
    )
}

/** Aktiivisen työskentelyistunnon kompakti tilarivi projektikortissa ja jatka-kortissa. */
@Composable
internal fun workSessionStatusText(
    hasActiveSession: Boolean,
    needsReview: Boolean,
): String? =
    if (hasActiveSession) {
        stringResource(if (needsReview) R.string.work_session_recovery_needed else R.string.work_session_active)
    } else {
        null
    }
