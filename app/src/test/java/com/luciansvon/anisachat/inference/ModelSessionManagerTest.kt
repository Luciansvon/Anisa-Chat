package com.luciansvon.anisachat.inference

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ModelSessionManagerTest {
    @Test
    fun `model loads only for generation then unloads after idle timeout`() = runTest {
        val runtime = CountingRuntime()
        val manager = ModelSessionManager(
            runtime = runtime,
            scope = this,
            idleTimeout = 1.seconds,
        )

        val response = manager.generate(
            InferenceRequest(
                systemContext = "test",
                messages = emptyList(),
            ),
        )

        assertEquals("ok", response)
        assertTrue(runtime.isLoaded)
        assertEquals(1, runtime.loadCount)

        advanceTimeBy(1_001)
        runCurrent()

        assertFalse(runtime.isLoaded)
        assertEquals(1, runtime.unloadCount)
    }

    private class CountingRuntime : InferenceRuntime {
        override var isLoaded = false
        var loadCount = 0
        var unloadCount = 0

        override suspend fun load() {
            isLoaded = true
            loadCount += 1
        }

        override suspend fun generate(request: InferenceRequest): String = "ok"

        override suspend fun unload() {
            isLoaded = false
            unloadCount += 1
        }
    }
}
