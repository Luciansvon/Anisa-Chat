package com.luciansvon.anisachat.chat

import com.luciansvon.anisachat.data.InMemoryConversationStateStore
import com.luciansvon.anisachat.domain.ConversationState
import com.luciansvon.anisachat.emotion.EmotionEngine
import com.luciansvon.anisachat.inference.InferenceRequest
import com.luciansvon.anisachat.inference.InferenceRuntime
import com.luciansvon.anisachat.inference.ModelSessionManager
import com.luciansvon.anisachat.memory.InMemoryMemoryRepository
import com.luciansvon.anisachat.time.TimeContextEngine
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatOrchestratorTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    @Test
    fun `asking current time does not load model`() = runTest {
        val previous = localTime(9, 0)
        val now = localTime(17, 0)
        val runtime = CountingRuntime()
        val store = InMemoryConversationStateStore(
            ConversationState(lastUserMessageAt = previous.toInstant()),
        )
        val orchestrator = buildOrchestrator(
            store = store,
            runtime = runtime,
            clock = Clock.fixed(now.toInstant(), jakarta),
            scope = this,
        )

        val result = orchestrator.send("sekarang jam berapa?")

        assertFalse(result.usedModel)
        assertEquals(0, runtime.loadCount)
        assertTrue(result.reply.contains("17:00"))
    }

    @Test
    fun `free form conversation loads model`() = runTest {
        val now = localTime(17, 0)
        val runtime = CountingRuntime()
        val orchestrator = buildOrchestrator(
            store = InMemoryConversationStateStore(),
            runtime = runtime,
            clock = Clock.fixed(now.toInstant(), jakarta),
            scope = this,
        )

        val result = orchestrator.send("halo, lagi apa?")

        assertTrue(result.usedModel)
        assertEquals(1, runtime.loadCount)
        assertEquals("model-reply", result.reply)
    }

    private fun buildOrchestrator(
        store: InMemoryConversationStateStore,
        runtime: CountingRuntime,
        clock: Clock,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = ChatOrchestrator(
        store = store,
        memoryRepository = InMemoryMemoryRepository(),
        timeEngine = TimeContextEngine(clock),
        emotionEngine = EmotionEngine(),
        deterministicRouter = DeterministicRouter(),
        contextBuilder = SystemContextBuilder(),
        modelSession = ModelSessionManager(runtime, scope),
    )

    private fun localTime(hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 9, 10, hour, minute, 0, 0, jakarta)

    private class CountingRuntime : InferenceRuntime {
        override var isLoaded: Boolean = false
        var loadCount = 0

        override suspend fun load() {
            isLoaded = true
            loadCount += 1
        }

        override suspend fun generate(request: InferenceRequest): String = "model-reply"

        override suspend fun unload() {
            isLoaded = false
        }
    }
}
