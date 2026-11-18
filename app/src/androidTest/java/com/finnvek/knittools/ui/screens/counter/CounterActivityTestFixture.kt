package com.finnvek.knittools.ui.screens.counter

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.TestContentSplashRule
import org.junit.Rule

abstract class CounterActivityTestFixture {
    @get:Rule(order = 0)
    val splash = TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    protected val app: App = ApplicationProvider.getApplicationContext()
    protected lateinit var database: KnitToolsDatabase
    protected lateinit var repository: CounterRepository
}
