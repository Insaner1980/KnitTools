package com.finnvek.knittools.ui.screens.insights

import java.time.LocalDate
import java.util.BitSet
import java.util.TreeMap

private const val DAYS_PER_ACTIVITY_BLOCK = 4096L

/** Harvat aikavälit eivät varaa väliin jääviä vuosia eivätkä yhtä oliota päivää kohden. */
internal class InsightsActivityDates : AbstractSet<LocalDate>() {
    private val blocks = TreeMap<Long, BitSet>()
    override var size = 0
        private set

    fun add(date: LocalDate) {
        val day = date.toEpochDay()
        val bits = blocks.getOrPut(Math.floorDiv(day, DAYS_PER_ACTIVITY_BLOCK)) { BitSet() }
        val offset = Math.floorMod(day, DAYS_PER_ACTIVITY_BLOCK).toInt()
        if (!bits[offset]) {
            bits.set(offset)
            size++
        }
    }

    override fun contains(element: LocalDate): Boolean {
        val day = element.toEpochDay()
        return blocks[Math.floorDiv(day, DAYS_PER_ACTIVITY_BLOCK)]
            ?.get(Math.floorMod(day, DAYS_PER_ACTIVITY_BLOCK).toInt()) == true
    }

    private fun epochDays(): Sequence<Long> =
        sequence {
            for ((block, bits) in blocks) {
                var offset = bits.nextSetBit(0)
                while (offset >= 0) {
                    yield(block * DAYS_PER_ACTIVITY_BLOCK + offset)
                    offset = bits.nextSetBit(offset + 1)
                }
            }
        }

    override fun iterator(): Iterator<LocalDate> = epochDays().map(LocalDate::ofEpochDay).iterator()

    fun bestStreak(): Int {
        var best = 0
        var streak = 0
        var previous: Long? = null
        epochDays().forEach { day ->
            streak = if (previous?.plus(1) == day) streak + 1 else 1
            best = maxOf(best, streak)
            previous = day
        }
        return best
    }
}
