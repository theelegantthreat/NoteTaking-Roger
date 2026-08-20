package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {

    val allNotesFlow: Flow<List<Note>> = noteDao.getAllNotesFlow()
    val allFoldersFlow: Flow<List<Folder>> = folderDao.getAllFoldersFlow()

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

    suspend fun moveNoteToFolder(noteId: Long, targetFolder: String) {
        noteDao.moveNoteToFolder(noteId, targetFolder)
    }

    suspend fun insertFolder(folder: Folder) {
        folderDao.insertFolder(folder)
    }

    suspend fun insertFolders(folders: List<Folder>) {
        folderDao.insertFolders(folders)
    }

    suspend fun renameFolder(id: String, oldName: String, newName: String) {
        folderDao.renameFolder(id, newName)
        noteDao.renameFolderInNotes(oldName, newName)
    }

    suspend fun deleteFolder(id: String, folderName: String) {
        folderDao.deleteFolderById(id)
        noteDao.resetNotesFolderToGeneral(folderName)
    }

    suspend fun clearAllNotes() {
        noteDao.clearAllNotes()
    }
}
