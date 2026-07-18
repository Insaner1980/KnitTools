package com.finnvek.knittools

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.DebugDemoDataSeeder
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

object DemoDataSeeder {
    fun seedIfNeeded(
        applicationScope: CoroutineScope,
        ioDispatcher: CoroutineDispatcher,
        database: Lazy<KnitToolsDatabase>,
        transactionRunner: Lazy<DatabaseTransactionRunner>,
        projectCounterRepository: Lazy<ProjectCounterRepository>,
        yarnCardRepository: Lazy<YarnCardRepository>,
    ) = DebugDemoDataSeeder.seedIfNeeded(
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher,
        database = database,
        transactionRunner = transactionRunner,
        addCounter = { counter -> projectCounterRepository.get().addCounter(counter) },
        saveYarnCard = { card -> yarnCardRepository.get().saveCardInCurrentTransaction(card) },
    )
}
