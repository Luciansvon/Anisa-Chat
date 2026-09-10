package com.luciansvon.anisachat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RuntimeMetadataEntity::class,
        EmotionStateEntity::class,
        RelationshipStateEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AnisaDatabase : RoomDatabase() {
    abstract fun dao(): AnisaDao

    companion object {
        private const val DATABASE_NAME = "anisa-chat.db"

        @Volatile
        private var instance: AnisaDatabase? = null

        fun open(context: Context): AnisaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AnisaDatabase::class.java,
                DATABASE_NAME,
            ).build().also { database ->
                instance = database
            }
        }
    }
}
