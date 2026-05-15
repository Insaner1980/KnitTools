package com.finnvek.knittools.domain.model

object YarnCardStatus {
    const val IN_STASH = "IN_STASH"
    const val IN_USE = "IN_USE"
    const val FINISHED = "FINISHED"

    private val supportedStatuses = setOf(IN_STASH, IN_USE, FINISHED)

    fun isSupported(status: String): Boolean = status in supportedStatuses

    fun normalize(status: String): String =
        if (isSupported(status)) {
            status
        } else {
            IN_STASH
        }
}
