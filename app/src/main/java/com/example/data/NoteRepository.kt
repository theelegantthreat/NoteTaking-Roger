package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao
) {

    val allNotesFlow: Flow<List<Note>> = noteDao.getAllNotesFlow()

    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun insertNotes(notes: List<Note>) {
        noteDao.insertNotes(notes)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    suspend fun clearAllNotes() {
        noteDao.clearAllNotes()
    }
}
