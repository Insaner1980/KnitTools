package com.finnvek.knittools.domain.model

/** Laskuritransaktion todelliset arvot; Undo ei käynnistä tavoitteen juhlintaa. */
data class CommittedMainCounterChange(
    val beforeCount: Int,
    val afterCount: Int,
    val targetRows: Int?,
    val change: MainCounterChange,
) {
    val crossedTarget: Boolean
        get() = change != MainCounterChange.Undo && targetRows?.let { beforeCount < it && afterCount >= it } == true
}
