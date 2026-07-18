package com.finnvek.knittools

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

object DemoDataSeeder {
    fun seedIfNeeded(
        @Suppress("UNUSED_PARAMETER") applicationScope: CoroutineScope,
        @Suppress("UNUSED_PARAMETER") ioDispatcher: CoroutineDispatcher,
        @Suppress("UNUSED_PARAMETER") database: Lazy<KnitToolsDatabase>,
        @Suppress("UNUSED_PARAMETER") transactionRunner: Lazy<DatabaseTransactionRunner>,
    ) = Unit
}
