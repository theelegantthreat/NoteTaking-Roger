package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppBackupPayload
import com.example.data.Note
import com.example.data.NoteRepository
import com.example.git.GitHubSyncManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("NoteTakingRogerPrefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(database.noteDao())
    private val gitHubSyncManager = GitHubSyncManager()

    // Preferences properties
    val themeState = MutableStateFlow(sharedPrefs.getString("active_theme", "roger") ?: "roger")
    val selectThemeOptions = listOf("classified", "detective", "keep-dark", "keep", "occult", "polished", "puppy", "roger", "terminal", "xfce")

    val ghTokenState = MutableStateFlow(sharedPrefs.getString("gh_token", "") ?: "")
    val ghRepoState = MutableStateFlow(sharedPrefs.getString("gh_repo", "") ?: "")
    val ghPathState = MutableStateFlow(sharedPrefs.getString("gh_path", "notes.json") ?: "notes.json")

    // UI state flows
    val searchQuery = MutableStateFlow("")
    val activeTags = MutableStateFlow<Set<String>>(emptySet())
    val sortOrder = MutableStateFlow("title-asc") // title-asc, title-desc, newest, oldest

    // Active Note being edited
    val activeNoteId = MutableStateFlow<Long?>(null)
    val activeNote = activeNoteId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.allNotesFlow.map { notes -> notes.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Sync status and states
    val isSyncing = MutableStateFlow(false)
    val ghStatus = MutableStateFlow("")

    // Raw Notes Flow
    private val _rawNotes = repository.allNotesFlow
    val allNotesState: StateFlow<List<Note>> = _rawNotes.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    // Sorted and Filtered Notes Flow
    val filteredNotes: StateFlow<List<Note>> = combine(
        _rawNotes, searchQuery, activeTags, sortOrder
    ) { notes, search, selectedTags, sort ->
        var result = if (notes.isEmpty()) {
            // Initiate default/starter note if empty
            val starterNotes = createStarterNote()
            starterNotes
        } else {
            notes
        }

        // Apply Search
        if (search.isNotEmpty()) {
            val query = search.lowercase(Locale.getDefault())
            result = result.filter { note ->
                note.title.lowercase(Locale.getDefault()).contains(query) ||
                        note.content.lowercase(Locale.getDefault()).contains(query) ||
                        note.tags.any { it.lowercase(Locale.getDefault()).contains(query) }
            }
        }

        // Apply Tag filters (Note must have ALL active tags)
        if (selectedTags.isNotEmpty()) {
            result = result.filter { note ->
                selectedTags.all { note.tags.contains(it) }
            }
        }

        // Apply Sorting
        val sortedList = when (sort) {
            "title-asc" -> result.sortedBy { it.title.lowercase(Locale.getDefault()) }
            "title-desc" -> result.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            "newest" -> result.sortedByDescending { it.id }
            "oldest" -> result.sortedBy { it.id }
            else -> result
        }
        val (pinned, unpinned) = sortedList.partition { it.isPinned }
        pinned + unpinned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All distinct tags across all notes
    val allTags: StateFlow<List<String>> = _rawNotes.map { notes ->
        notes.flatMap { it.tags }.distinct().sortedBy { it.lowercase(Locale.getDefault()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getFormattedDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun createStarterNote(): List<Note> {
        val list = mutableListOf<Note>()
        viewModelScope.launch(Dispatchers.IO) {
            val starter = Note(
                id = System.currentTimeMillis(),
                title = "Alchemy Dev Documentation",
                content = "Welcome to NoteTaking Roger!\n\nThis app is optimized for managing development documentation on the go, with complete offline Room Database support and real-time GitHub repository updates using Personal Access Tokens (PAT).\n\nFeel free to explore the visual theme selections in the settings bar above (including Occult, Retro Terminal, Puppy Linux, google Keep styles and XFCE desktops) which restructure the components matching developer aesthetics.\n\nHappy Coding!\nRoger",
                tags = listOf("dev", "documentation", "alchemy"),
                created = getFormattedDate(),
                updated = getFormattedDate()
            )
            repository.insertNote(starter)
        }
        return list
    }

    // Note actions
    fun createNewNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val newNote = Note(
                id = now,
                title = "",
                content = "",
                tags = emptyList(),
                created = getFormattedDate(),
                updated = getFormattedDate()
            )
            repository.insertNote(newNote)
            withContext(Dispatchers.Main) {
                activeNoteId.value = now
            }
        }
    }

    fun createDailyJournal() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val title = "Daily Journal - $dateStr"
            val content = """
                |# Daily Journal - $dateStr
                |
                |💤 Dreams
                |*Recall details of your recent dreams here:*
                |
                |## 🎯 Goals for Today
                |- [ ] Goal 1: 
                |- [ ] Goal 2: 
                |- [ ] Goal 3: 
                |
                |## 📝 Reflections
                |- **Current Mindset:** 
                |- **What went well today?** 
                |- **What challenges did I face, and how did I overcome them?** 
                |
                |## 🛠️ Tasks
                |- [ ] Task 1: 
                |- [ ] Task 2: 
                |- [ ] Task 3: 
            """.trimMargin()
            val newNote = Note(
                id = now,
                title = title,
                content = content,
                tags = listOf("daily", "dream", "journal"),
                created = getFormattedDate(),
                updated = getFormattedDate()
            )
            repository.insertNote(newNote)
            withContext(Dispatchers.Main) {
                activeNoteId.value = now
                Toast.makeText(getApplication(), "Daily Journal created successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun selectNote(id: Long) {
        activeNoteId.value = id
    }

    fun closeActiveNote() {
        activeNoteId.value = null
    }

    fun deleteActiveNote() {
        val currentId = activeNoteId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNoteById(currentId)
            withContext(Dispatchers.Main) {
                activeNoteId.value = null
                Toast.makeText(getApplication(), "Note deleted successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun duplicateActiveNote() {
        val note = activeNote.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val copyTitle = if (note.title.isNotEmpty()) "${note.title} [Copy]" else "Untitled Note [Copy]"
            val copy = Note(
                id = now,
                title = copyTitle,
                content = note.content,
                tags = note.tags,
                created = getFormattedDate(),
                updated = getFormattedDate()
            )
            repository.insertNote(copy)
            withContext(Dispatchers.Main) {
                activeNoteId.value = now
            }
        }
    }

    fun updateActiveNote(title: String, content: String, tagsCommaSeparated: String) {
        val currentId = activeNoteId.value ?: return
        val currentNote = activeNote.value ?: return
        
        val tagsList = tagsCommaSeparated.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        viewModelScope.launch(Dispatchers.IO) {
            val updatedNote = currentNote.copy(
                title = title,
                content = content,
                tags = tagsList,
                updated = getFormattedDate()
            )
            repository.insertNote(updatedNote)
        }
    }

    fun togglePinNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = repository.getNoteById(id)
            if (note != null) {
                val updatedNote = note.copy(
                    isPinned = !note.isPinned,
                    updated = getFormattedDate()
                )
                repository.insertNote(updatedNote)
            }
        }
    }

    // Tag Management
    fun selectTag(tag: String) {
        val current = activeTags.value
        if (current.contains(tag)) {
            activeTags.value = current - tag
        } else {
            activeTags.value = current + tag
        }
    }

    fun clearFilters() {
        activeTags.value = emptySet()
        searchQuery.value = ""
    }

    fun deleteTagFromAllNotes(tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val notes = filteredNotes.value
            notes.forEach { note ->
                if (note.tags.contains(tag)) {
                    val newTags = note.tags.filter { it != tag }
                    repository.insertNote(note.copy(tags = newTags, updated = getFormattedDate()))
                }
            }
        }
    }

    fun renameTagInAllNotes(oldTag: String, newTag: String) {
        if (newTag.trim().isEmpty() || oldTag == newTag) return
        viewModelScope.launch(Dispatchers.IO) {
            val notes = filteredNotes.value
            notes.forEach { note ->
                if (note.tags.contains(oldTag)) {
                    val replaced = note.tags.map { if (it == oldTag) newTag.trim() else it }.distinct()
                    repository.insertNote(note.copy(tags = replaced, updated = getFormattedDate()))
                }
            }
        }
    }

    // Preferences & Settings Management
    fun changeTheme(theme: String) {
        themeState.value = theme
        sharedPrefs.edit().putString("active_theme", theme).apply()
    }

    val isTestingConnection = MutableStateFlow(false)
    val testConnectionResult = MutableStateFlow<String?>(null)

    fun testGitHubConnection(token: String, repo: String, path: String) {
        if (token.isEmpty() || repo.isEmpty() || path.isEmpty()) {
            testConnectionResult.value = "Missing fields: Please fill in all settings lines."
            return
        }
        val parts = repo.split("/")
        if (parts.size != 2) {
            testConnectionResult.value = "Invalid Repository format. Must be owner/repo."
            return
        }

        isTestingConnection.value = true
        testConnectionResult.value = "Testing API connection..."

        viewModelScope.launch {
            try {
                val owner = parts[0].trim()
                val repoName = parts[1].trim()
                
                val response = withContext(Dispatchers.IO) {
                    gitHubSyncManager.testConnection(
                        token = token,
                        repoName = repoName,
                        owner = owner,
                        path = path.trim()
                    )
                }
                
                isTestingConnection.value = false
                if (response.isSuccess) {
                    val fileFound = response.getOrDefault(false)
                    testConnectionResult.value = "Connected! (Database file exists: $fileFound)"
                } else {
                    val msg = response.exceptionOrNull()?.message ?: "Unknown error"
                    if (msg.contains("404")) {
                        // 404 is a success for credential validation! The repo is accessible, but the file is new.
                        testConnectionResult.value = "Connected! (Repo valid, notes file ready to initialize)"
                    } else if (msg.contains("401") || msg.contains("403")) {
                        testConnectionResult.value = "Auth failed: Bad credentials or insufficient OAuth scopes."
                    } else {
                        testConnectionResult.value = "Connection failed: $msg"
                    }
                }
            } catch (e: Exception) {
                isTestingConnection.value = false
                testConnectionResult.value = "Error: ${e.message}"
            }
        }
    }

    fun saveGitHubSettings(token: String, repo: String, path: String) {
        ghTokenState.value = token
        ghRepoState.value = repo
        ghPathState.value = path

        sharedPrefs.edit().apply {
            putString("gh_token", token)
            putString("gh_repo", repo)
            putString("gh_path", path)
            apply()
        }
    }

    // GitHub synchronization actions
    fun pushToGitHub() {
        val token = ghTokenState.value
        val repo = ghRepoState.value
        val path = ghPathState.value

        if (token.isEmpty() || repo.isEmpty() || path.isEmpty()) {
            ghStatus.value = "Fill in GitHub Settings first."
            return
        }

        isSyncing.value = true
        ghStatus.value = "Pushing data to GitHub..."

        viewModelScope.launch {
            // Get all database notes
            val currentNotes = filteredNotes.value
            val result = withContext(Dispatchers.IO) {
                gitHubSyncManager.pushNotes(token, repo, path, currentNotes)
            }
            isSyncing.value = false
            result.onSuccess { msg ->
                ghStatus.value = "Success: $msg"
            }.onFailure { err ->
                ghStatus.value = "Error: ${err.message}"
            }
        }
    }

    fun pullFromGitHub() {
        val token = ghTokenState.value
        val repo = ghRepoState.value
        val path = ghPathState.value

        if (token.isEmpty() || repo.isEmpty() || path.isEmpty()) {
            ghStatus.value = "Fill in GitHub Settings first."
            return
        }

        isSyncing.value = true
        ghStatus.value = "Pulling data from GitHub..."

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                gitHubSyncManager.pullNotes(token, repo, path)
            }
            isSyncing.value = false
            result.onSuccess { pulledNotes ->
                withContext(Dispatchers.IO) {
                    // Save and merge pulled notes
                    repository.insertNotes(pulledNotes)
                }
                ghStatus.value = "Pulled & Merged successfully!"
            }.onFailure { err ->
                ghStatus.value = "Error: ${err.message}"
            }
        }
    }

    // Import Backup JSON locally
    fun importBackup(jsonString: String): Boolean {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            var importedNotes: List<Note>? = null

            try {
                val payloadAdapter = moshi.adapter(AppBackupPayload::class.java)
                val payload = payloadAdapter.fromJson(jsonString)
                if (payload != null) {
                    importedNotes = payload.notes
                    payload.activeTheme.let { theme ->
                        if (theme.isNotEmpty()) {
                            changeTheme(theme)
                        }
                    }
                    if (payload.ghToken.isNotEmpty() || payload.ghRepo.isNotEmpty() || payload.ghPath.isNotEmpty()) {
                        saveGitHubSettings(
                            if (payload.ghToken.isNotEmpty()) payload.ghToken else ghTokenState.value,
                            if (payload.ghRepo.isNotEmpty()) payload.ghRepo else ghRepoState.value,
                            if (payload.ghPath.isNotEmpty()) payload.ghPath else ghPathState.value
                        )
                    }
                }
            } catch (e: Exception) {
                // Fall back to try parsing as a raw JSON array of Notes
            }

            if (importedNotes == null) {
                try {
                    val listAdapter = moshi.adapter<List<Note>>(
                        Types.newParameterizedType(List::class.java, Note::class.java)
                    )
                    importedNotes = listAdapter.fromJson(jsonString)
                } catch (e: Exception) {
                    importedNotes = null
                }
            }

            if (importedNotes == null) return false

            viewModelScope.launch(Dispatchers.IO) {
                repository.insertNotes(importedNotes)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getExportBackupJson(): String {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(AppBackupPayload::class.java)
            val notesToExport = if (allNotesState.value.isNotEmpty()) allNotesState.value else filteredNotes.value
            val payload = AppBackupPayload(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                activeTheme = themeState.value,
                ghToken = ghTokenState.value,
                ghRepo = ghRepoState.value,
                ghPath = ghPathState.value,
                notes = notesToExport
            )
            adapter.toJson(payload)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun exportBackupToUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val jsonString = getExportBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importBackupFromUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return false
            importBackup(jsonString)
        } catch (e: Exception) {
            false
        }
    }
}
