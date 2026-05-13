package com.finnvek.knittools.data.local

object ImmediateDatabaseTransactionRunner : DatabaseTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
