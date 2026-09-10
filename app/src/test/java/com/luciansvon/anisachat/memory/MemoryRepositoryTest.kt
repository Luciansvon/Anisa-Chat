package com.luciansvon.anisachat.memory

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRepositoryTest {
    @Test
    fun `search returns lexically relevant memory without model`() = runTest {
        val repository = InMemoryMemoryRepository(
            listOf(
                MemoryItem(text = "user suka finishing kayu dark nut", importance = 80),
                MemoryItem(text = "user pernah membahas printer thermal", importance = 70),
            ),
        )

        val result = repository.search("finishing dark nut favorit", limit = 2)

        assertEquals(1, result.size)
        assertTrue(result.first().text.contains("dark nut"))
    }
}
