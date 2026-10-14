package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.takeWithoutSplittingSurrogatePair

object ReminderLogic {
    const val MESSAGE_MAX_LENGTH = 200

    fun validatedForPersistence(reminder: RowReminder): RowReminder? {
        if (reminder.targetRow <= 0) return null
        if (reminder.repeatInterval != null && reminder.repeatInterval <= 0) return null
        val trimmedMessage = reminder.message.trim()
        if (trimmedMessage.isEmpty()) return null
        return reminder.copy(message = trimmedMessage.takeWithoutSplittingSurrogatePair(MESSAGE_MAX_LENGTH))
    }

    fun shouldTrigger(
        reminder: RowReminder,
        currentRow: Int,
    ): Boolean {
        if (currentRow <= 0 || reminder.targetRow <= 0 || reminder.isCompleted) return false
        val interval = reminder.repeatInterval
        return when {
            interval == null -> currentRow == reminder.targetRow
            interval <= 0 -> false
            else -> {
                // Toistuvat: laukeaa targetRow:ssa ja joka interval:n välein sen jälkeen
                currentRow >= reminder.targetRow &&
                    (currentRow - reminder.targetRow) % interval == 0
            }
        }
    }

    fun repeatCount(
        reminder: RowReminder,
        currentRow: Int,
    ): Int {
        val interval = reminder.repeatInterval ?: return 0
        if (interval <= 0 || currentRow < reminder.targetRow) return 0
        return (currentRow - reminder.targetRow) / interval + 1
    }

    fun activeReminders(
        reminders: List<RowReminder>,
        currentRow: Int,
    ): List<RowReminder> = reminders.filter { shouldTrigger(it, currentRow) }
}
