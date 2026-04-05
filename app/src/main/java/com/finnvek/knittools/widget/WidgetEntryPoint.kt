package com.finnvek.knittools.widget

import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
@Suppress("kotlin:S6517") // Hilt @EntryPoint vaatii interfacen
interface WidgetEntryPoint {
    fun counterRepository(): CounterRepository
}
