package com.finnvek.knittools.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatternFileReferenceCoordinator
    @Inject
    constructor() {
        private val mutex = Mutex()

        suspend fun <T> withReferenceLock(block: suspend () -> T): T = mutex.withLock { block() }
    }
