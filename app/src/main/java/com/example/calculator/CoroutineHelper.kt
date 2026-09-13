package com.example.calculator

import kotlinx.coroutines.*

object CoroutineHelper {
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun runInIO(runnable: Runnable): Job {
        return ioScope.launch { runnable.run() }
    }
}
