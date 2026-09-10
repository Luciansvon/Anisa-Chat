package com.luciansvon.anisachat.inference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ModelSessionManager(
    private val runtime: InferenceRuntime,
    private val scope: CoroutineScope,
    private val idleTimeout: Duration = 60.seconds,
) {
    private val mutex = Mutex()
    private var unloadJob: Job? = null

    suspend fun generate(request: InferenceRequest): String = mutex.withLock {
        unloadJob?.cancel()
        if (!runtime.isLoaded) {
            runtime.load()
        }

        try {
            runtime.generate(request)
        } finally {
            scheduleUnload()
        }
    }

    suspend fun unloadNow() = mutex.withLock {
        unloadJob?.cancel()
        unloadJob = null
        if (runtime.isLoaded) {
            runtime.unload()
        }
    }

    private fun scheduleUnload() {
        unloadJob?.cancel()
        unloadJob = scope.launch {
            delay(idleTimeout)
            mutex.withLock {
                if (runtime.isLoaded) {
                    runtime.unload()
                }
                unloadJob = null
            }
        }
    }
}
