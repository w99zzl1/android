package com.example.calculator

import kotlinx.coroutines.*

object CoroutineHelper {
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun runInIO(action: suspend () -> Unit): Job {
        return ioScope.launch(block = action)
    }
}
