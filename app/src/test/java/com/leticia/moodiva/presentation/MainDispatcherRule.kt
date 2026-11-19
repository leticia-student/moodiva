package com.leticia.moodiva.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Regra do JUnit que troca o `Dispatchers.Main` por um [StandardTestDispatcher].
 *
 * O `viewModelScope` roda no Main, que não existe na JVM dos testes. Com esta regra,
 * as coroutines do ViewModel rodam no dispatcher de teste, controlado pelo `runTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class) // setMain/resetMain ainda são marcadas como experimentais
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
