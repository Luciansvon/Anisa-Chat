package com.luciansvon.anisachat.memory

import java.time.Instant
import java.util.Locale
import java.util.UUID

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val importance: Int = 50,
    val createdAt: Instant = Instant.now(),
)

interface MemoryRepository {
    suspend fun add(item: MemoryItem)
    suspend fun search(query: String, limit: Int = 4): List<MemoryItem>
}

class InMemoryMemoryRepository(
    initial: List<MemoryItem> = emptyList(),
) : MemoryRepository {
    private val items = initial.toMutableList()

    override suspend fun add(item: MemoryItem) {
        if (item.text.isNotBlank()) {
            items += item.copy(importance = item.importance.coerceIn(0, 100))
        }
    }

    override suspend fun search(query: String, limit: Int): List<MemoryItem> =
        LexicalMemorySearch.rank(items, query, limit)
}

internal object LexicalMemorySearch {
    fun rank(
        items: Iterable<MemoryItem>,
        query: String,
        limit: Int,
    ): List<MemoryItem> {
        if (query.isBlank() || limit <= 0) return emptyList()
        val queryTokens = tokens(query)
        if (queryTokens.isEmpty()) return emptyList()

        return items
            .asSequence()
            .map { item -> item to score(queryTokens, tokens(item.text), item.importance) }
            .filter { (_, score) -> score > 0.0 }
            .sortedByDescending { (_, score) -> score }
            .take(limit)
            .map { (item, _) -> item }
            .toList()
    }

    private fun score(
        queryTokens: Set<String>,
        memoryTokens: Set<String>,
        importance: Int,
    ): Double {
        if (memoryTokens.isEmpty()) return 0.0
        val overlap = queryTokens.intersect(memoryTokens).size
        if (overlap == 0) return 0.0

        val lexical = overlap.toDouble() / queryTokens.size.coerceAtLeast(1)
        val importanceBoost = importance.coerceIn(0, 100) / 500.0
        return lexical + importanceBoost
    }

    private fun tokens(text: String): Set<String> = text
        .lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .asSequence()
        .filter { it.length >= 3 }
        .toSet()
}
