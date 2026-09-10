package com.luciansvon.anisachat.memory

import com.luciansvon.anisachat.data.local.AnisaDao
import com.luciansvon.anisachat.data.local.MemoryEntity
import java.time.Instant

class RoomMemoryRepository(
    private val dao: AnisaDao,
) : MemoryRepository {
    override suspend fun add(item: MemoryItem) {
        if (item.text.isBlank()) return

        dao.upsertMemory(
            MemoryEntity(
                id = item.id,
                text = item.text.trim(),
                importance = item.importance.coerceIn(0, 100),
                createdAtEpochMs = item.createdAt.toEpochMilli(),
            ),
        )
    }

    override suspend fun search(query: String, limit: Int): List<MemoryItem> {
        val items = dao.readMemories().map { entity ->
            MemoryItem(
                id = entity.id,
                text = entity.text,
                importance = entity.importance.coerceIn(0, 100),
                createdAt = Instant.ofEpochMilli(entity.createdAtEpochMs),
            )
        }
        return LexicalMemorySearch.rank(items, query, limit)
    }
}
