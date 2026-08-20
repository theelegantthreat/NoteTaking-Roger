package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET folder = :newFolderName WHERE folder = :oldFolderName")
    suspend fun renameFolderInNotes(oldFolderName: String, newFolderName: String)

    @Query("UPDATE notes SET folder = :targetFolder WHERE id = :noteId")
    suspend fun moveNoteToFolder(noteId: Long, targetFolder: String)

    @Query("UPDATE notes SET folder = 'General' WHERE folder = :deletedFolder")
    suspend fun resetNotesFolderToGeneral(deletedFolder: String)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
}
