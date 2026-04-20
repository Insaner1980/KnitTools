package com.finnvek.knittools.ui.navigation

data class CounterLaunchRequest(
    val requestId: Long = System.nanoTime(),
    val projectId: Long? = null,
)
