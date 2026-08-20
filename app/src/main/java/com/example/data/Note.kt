package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val created: String,
    val updated: String,
    val timestamp: Long = System.currentTimeMillis(),
    val gitSyncStatus: String = "NOT_SYNCED",
    val isPinned: Boolean = false,
    val folder: String = "General"
)

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String,
    val name: String,
    val color: String = "#808080",
    val icon: String = "folder",
    val createdAt: Long = System.currentTimeMillis()
)

data class AppBackupPayload(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val activeTheme: String = "roger",
    val ghToken: String = "",
    val ghRepo: String = "",
    val ghPath: String = "notes.json",
    val folders: List<Folder> = emptyList(),
    val notes: List<Note> = emptyList()
)

class Converters {
    @TypeConverter
    fun fromTags(tags: List<String>?): String {
        return tags?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toTags(data: String?): List<String> {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
