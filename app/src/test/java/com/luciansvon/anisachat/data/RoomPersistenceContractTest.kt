package com.luciansvon.anisachat.data

import com.luciansvon.anisachat.data.local.AnisaDao
import com.luciansvon.anisachat.data.local.EmotionStateEntity
import com.luciansvon.anisachat.data.local.MemoryEntity
import com.luciansvon.anisachat.data.local.MessageEntity
import com.luciansvon.anisachat.data.local.RelationshipStateEntity
import com.luciansvon.anisachat.data.local.RuntimeMetadataEntity
import com.luciansvon.anisachat.domain.ChatMessage
import com.luciansvon.anisachat.domain.ConversationState
import com.luciansvon.anisachat.domain.EmotionState
import com.luciansvon.anisachat.domain.MessageRole
import com.luciansvon.anisachat.domain.RelationshipState
import com.luciansvon.anisachat.memory.MemoryItem
import com.luciansvon.anisachat.memory.RoomMemoryRepository
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPersistenceContractTest {
    @Test
    fun `conversation state survives repository recreation without duplicate messages`() = runTest {
        val dao = FakeAnisaDao()
        val store = RoomConversationStateStore(dao)
        val userAt = Instant.parse("2026-09-10T02:00:00Z")
        val assistantAt = Instant.parse("2026-09-10T02:00:05Z")
        val state = ConversationState(
            lastUserMessageAt = userAt,
            lastAssistantMessageAt = assistantAt,
            emotion = EmotionState(happiness = 120, irritation = -5, sadness = 30, energy = 70),
            relationship = RelationshipState(
                familiarity = 101,
                trust = 80,
                affection = -10,
                unresolvedAbsence = true,
            ),
            recentMessages = listOf(
                ChatMessage(
                    role = MessageRole.USER,
                    content = "halo",
                    createdAt = userAt,
                    id = "message-user",
                ),
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "hai",
                    createdAt = assistantAt,
                    id = "message-assistant",
                ),
            ),
        )

        store.write(state)
        store.write(state)

        val restartedStore = RoomConversationStateStore(dao)
        val restored = restartedStore.read()

        assertEquals(userAt, restored.lastUserMessageAt)
        assertEquals(assistantAt, restored.lastAssistantMessageAt)
        assertEquals(100, restored.emotion.happiness)
        assertEquals(0, restored.emotion.irritation)
        assertEquals(100, restored.relationship.familiarity)
        assertEquals(0, restored.relationship.affection)
        assertTrue(restored.relationship.unresolvedAbsence)
        assertEquals(listOf("message-user", "message-assistant"), restored.recentMessages.map { it.id })
        assertEquals(2, dao.messageCount)
    }

    @Test
    fun `memory remains searchable after repository recreation`() = runTest {
        val dao = FakeAnisaDao()
        val firstRepository = RoomMemoryRepository(dao)
        firstRepository.add(
            MemoryItem(
                id = "memory-1",
                text = "user suka finishing kayu dark nut",
                importance = 80,
                createdAt = Instant.parse("2026-09-10T02:00:00Z"),
            ),
        )

        val restartedRepository = RoomMemoryRepository(dao)
        val result = restartedRepository.search("finishing dark nut favorit", limit = 4)

        assertEquals(1, result.size)
        assertEquals("memory-1", result.single().id)
    }

    private class FakeAnisaDao : AnisaDao() {
        private var runtimeMetadata: RuntimeMetadataEntity? = null
        private var emotionState: EmotionStateEntity? = null
        private var relationshipState: RelationshipStateEntity? = null
        private val messages = linkedMapOf<String, MessageEntity>()
        private val memories = linkedMapOf<String, MemoryEntity>()
        private var nextRowId = 1L

        val messageCount: Int
            get() = messages.size

        override suspend fun readRuntimeMetadata(): RuntimeMetadataEntity? = runtimeMetadata

        override suspend fun readEmotionState(): EmotionStateEntity? = emotionState

        override suspend fun readRelationshipState(): RelationshipStateEntity? = relationshipState

        override suspend fun readRecentMessagesDescending(limit: Int): List<MessageEntity> = messages
            .values
            .sortedByDescending { it.rowId }
            .take(limit)

        override suspend fun readMemories(): List<MemoryEntity> = memories
            .values
            .sortedByDescending { it.createdAtEpochMs }

        override suspend fun upsertRuntimeMetadata(entity: RuntimeMetadataEntity) {
            runtimeMetadata = entity
        }

        override suspend fun upsertEmotionState(entity: EmotionStateEntity) {
            emotionState = entity
        }

        override suspend fun upsertRelationshipState(entity: RelationshipStateEntity) {
            relationshipState = entity
        }

        override suspend fun insertMessages(entities: List<MessageEntity>) {
            entities.forEach { entity ->
                if (entity.messageId !in messages) {
                    messages[entity.messageId] = entity.copy(rowId = nextRowId++)
                }
            }
        }

        override suspend fun upsertMemory(entity: MemoryEntity) {
            memories[entity.id] = entity
        }
    }
}
