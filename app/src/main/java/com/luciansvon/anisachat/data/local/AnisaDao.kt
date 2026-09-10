package com.luciansvon.anisachat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AnisaDao {
    @Query("SELECT * FROM runtime_metadata WHERE id = 1 LIMIT 1")
    abstract suspend fun readRuntimeMetadata(): RuntimeMetadataEntity?

    @Query("SELECT * FROM emotion_state WHERE id = 1 LIMIT 1")
    abstract suspend fun readEmotionState(): EmotionStateEntity?

    @Query("SELECT * FROM relationship_state WHERE id = 1 LIMIT 1")
    abstract suspend fun readRelationshipState(): RelationshipStateEntity?

    @Query("SELECT * FROM messages ORDER BY row_id DESC LIMIT :limit")
    abstract suspend fun readRecentMessagesDescending(limit: Int): List<MessageEntity>

    @Query("SELECT * FROM memories ORDER BY created_at DESC")
    abstract suspend fun readMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRuntimeMetadata(entity: RuntimeMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertEmotionState(entity: EmotionStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRelationshipState(entity: RelationshipStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMessages(entities: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMemory(entity: MemoryEntity)

    @Transaction
    open suspend fun persistConversationState(
        runtimeMetadata: RuntimeMetadataEntity,
        emotionState: EmotionStateEntity,
        relationshipState: RelationshipStateEntity,
        messages: List<MessageEntity>,
    ) {
        upsertRuntimeMetadata(runtimeMetadata)
        upsertEmotionState(emotionState)
        upsertRelationshipState(relationshipState)
        if (messages.isNotEmpty()) {
            insertMessages(messages)
        }
    }
}
