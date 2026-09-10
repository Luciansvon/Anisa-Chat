package com.luciansvon.anisachat.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "runtime_metadata")
data class RuntimeMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "last_user_message_at") val lastUserMessageAtEpochMs: Long? = null,
    @ColumnInfo(name = "last_assistant_message_at") val lastAssistantMessageAtEpochMs: Long? = null,
)

@Entity(tableName = "emotion_state")
data class EmotionStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val happiness: Int,
    val irritation: Int,
    val sadness: Int,
    val energy: Int,
)

@Entity(tableName = "relationship_state")
data class RelationshipStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val familiarity: Int,
    val trust: Int,
    val affection: Int,
    @ColumnInfo(name = "unresolved_absence") val unresolvedAbsence: Boolean,
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["message_id"], unique = true)],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    val rowId: Long = 0,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    val role: String,
    val content: String,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long,
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val text: String,
    val importance: Int,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long,
)

internal const val SINGLETON_ID = 1
