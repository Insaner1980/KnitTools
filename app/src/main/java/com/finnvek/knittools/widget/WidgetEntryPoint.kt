package com.finnvek.knittools.widget

import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun counterRepository(): CounterRepository
}
