package com.finnvek.knittools

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

object DemoDataSeeder {
    fun seedIfNeeded(
        @Suppress("UNUSED_PARAMETER") applicationScope: CoroutineScope,
        @Suppress("UNUSED_PARAMETER") ioDispatcher: CoroutineDispatcher,
        @Suppress("UNUSED_PARAMETER") database: Lazy<KnitToolsDatabase>,
        @Suppress("UNUSED_PARAMETER") transactionRunner: Lazy<DatabaseTransactionRunner>,
        @Suppress("UNUSED_PARAMETER") projectCounterRepository: Lazy<ProjectCounterRepository>,
        @Suppress("UNUSED_PARAMETER") yarnCardRepository: Lazy<YarnCardRepository>,
    ) = Unit
}
