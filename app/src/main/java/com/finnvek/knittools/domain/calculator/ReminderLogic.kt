package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.data.local.RowReminderEntity

object ReminderLogic {
    fun shouldTrigger(
        reminder: RowReminderEntity,
        currentRow: Int,
    ): Boolean {
        if (currentRow <= 0) return false
        val interval = reminder.repeatInterval
        return if (interval != null && interval > 0) {
            // Toistuvat: laukeaa targetRow:ssa ja joka interval:n välein sen jälkeen
            currentRow >= reminder.targetRow &&
                (currentRow - reminder.targetRow) % interval == 0
        } else {
            // Kertaluonteiset: laukeaa vain targetRow:ssa, ellei jo kuitattu
            !reminder.isCompleted && currentRow == reminder.targetRow
        }
    }

    fun repeatCount(
        reminder: RowReminderEntity,
        currentRow: Int,
    ): Int {
        val interval = reminder.repeatInterval ?: return 0
        if (interval <= 0 || currentRow < reminder.targetRow) return 0
        return (currentRow - reminder.targetRow) / interval + 1
    }

    fun activeReminders(
        reminders: List<RowReminderEntity>,
        currentRow: Int,
    ): List<RowReminderEntity> = reminders.filter { shouldTrigger(it, currentRow) }
}
