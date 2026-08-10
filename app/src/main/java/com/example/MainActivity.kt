package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Note
import com.example.ui.MainViewModel
import com.example.ui.theme.NotesTheme
import com.example.ui.theme.NotesThemeConfig
import java.util.*
import java.text.SimpleDateFormat
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val activeThemeKey by viewModel.themeState.collectAsStateWithLifecycle()
            val themeConfig = NotesTheme.getThemeConfig(activeThemeKey)

            // Dynamic Styling Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(themeConfig.bgPrimary)
                    .drawBehind {
                        // Creative context styling accents
                        when (activeThemeKey) {
                            "terminal" -> {
                                // Draw horizontal Retro CRT scanlines
                                val strokeWidth = 1f
                                val spacing = 6f
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = Color(0x1233FF33),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                    y += spacing
                                }
                            }
                            "classified" -> {
                                // Draw high-tension hazard warning diagonals at top status bar
                                val stripeWidth = 24f
                                val spacing = 48f
                                var x = -size.height
                                while (x < size.width) {
                                    drawLine(
                                        color = Color(0x0CFF6B35),
                                        start = Offset(x, 0f),
                                        end = Offset(x + size.height, size.height),
                                        strokeWidth = stripeWidth
                                    )
                                    x += spacing
                                }
                            }
                            "occult" -> {
                                // Draw circular radial glow in the center top
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x19D9B56D), Color.Transparent),
                                        center = Offset(size.width / 2f, 0f),
                                        radius = size.width * 0.5f
                                    ),
                                    radius = size.width * 0.5f,
                                    center = Offset(size.width / 2f, 0f)
                                )
                            }
                        }
                    }
            ) {
                NoteTakingRogerApp(viewModel = viewModel, themeConfig = themeConfig, themeKey = activeThemeKey)
            }
        }
    }
}

@Composable
fun NoteTakingRogerApp(
    viewModel: MainViewModel,
    themeConfig: NotesThemeConfig,
    themeKey: String
) {
    val context = LocalContext.current
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedTags by viewModel.activeTags.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val activeNote by viewModel.activeNote.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val ghStatus by viewModel.ghStatus.collectAsStateWithLifecycle()

    // Dialog flags
    var showGitHubDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Daily Journal FAB
                ExtendedFloatingActionButton(
                    onClick = { viewModel.createDailyJournal() },
                    modifier = Modifier.testTag("new_journal_fab").padding(horizontal = 8.dp),
                    containerColor = themeConfig.bgSecondary,
                    contentColor = themeConfig.accent,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Filled.Book, contentDescription = "Create daily journal")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Daily Journal", fontFamily = themeConfig.fontFamily, fontWeight = FontWeight.Bold)
                }

                // New Note FAB
                ExtendedFloatingActionButton(
                    onClick = { viewModel.createNewNote() },
                    modifier = Modifier.testTag("new_note_fab").padding(8.dp),
                    containerColor = themeConfig.accent,
                    contentColor = if (themeKey == "detective" || themeKey == "puppy" || themeKey == "xfce") Color.White else themeConfig.bgPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create note")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Note", fontFamily = themeConfig.fontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Title Header Bar
            NoteHeaderBar(
                viewModel = viewModel,
                themeConfig = themeConfig,
                themeKey = themeKey,
                onGitHubSettingsClick = { showGitHubDialog = true },
                onBackupClick = { showBackupDialog = true }
            )

            // Search and sort row
            SearchAndSortRow(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.searchQuery.value = it },
                sortOrder = sortOrder,
                onSortChange = { viewModel.sortOrder.value = it },
                themeConfig = themeConfig,
                themeKey = themeKey,
                allTags = tags
            )

            // Tags selection horizontal bar
            TagsFilterRow(
                tags = tags,
                selectedTags = selectedTags,
                onTagSelect = { viewModel.selectTag(it) },
                onClearFilters = { viewModel.clearFilters() },
                onManageTagsClick = { showTagDialog = true },
                themeConfig = themeConfig,
                themeKey = themeKey
            )

            // Responsive Layout - Split Pane on wide displays, grid-list on compact
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth > 720.dp
                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Notes Sidebar Panel
                        Box(modifier = Modifier.weight(0.45f)) {
                            NotesGrid(
                                notes = notes,
                                activeNoteId = activeNote?.id,
                                onSelectNote = { viewModel.selectNote(it) },
                                themeConfig = themeConfig,
                                themeKey = themeKey,
                                isCompact = true,
                                onTogglePin = { viewModel.togglePinNote(it) }
                            )
                        }

                        // Split pane border line
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(themeConfig.border)
                        )

                        // Editor detail panel
                        Box(modifier = Modifier.weight(0.55f)) {
                            if (activeNote != null) {
                                NoteContentEditor(
                                    note = activeNote!!,
                                    onUpdate = { title, content, tagsText ->
                                        viewModel.updateActiveNote(title, content, tagsText)
                                    },
                                    onClose = { viewModel.closeActiveNote() },
                                    onDuplicate = { viewModel.duplicateActiveNote() },
                                    onDelete = { viewModel.deleteActiveNote() },
                                    onTogglePin = { viewModel.togglePinNote(activeNote!!.id) },
                                    themeConfig = themeConfig,
                                    themeKey = themeKey,
                                    allTags = tags,
                                    allNotes = notes
                                )
                            } else {
                                EmptyEditorState(themeConfig = themeConfig)
                            }
                        }
                    }
                } else {
                    // Mobile Single View: Notes Grid
                    NotesGrid(
                        notes = notes,
                        activeNoteId = activeNote?.id,
                        onSelectNote = { viewModel.selectNote(it) },
                        themeConfig = themeConfig,
                        themeKey = themeKey,
                        isCompact = false,
                        onTogglePin = { viewModel.togglePinNote(it) }
                    )

                    // Mobile view launches full sheet modal for edit
                    if (activeNote != null) {
                        Dialog(onDismissRequest = { viewModel.closeActiveNote() }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(themeConfig.bgSecondary)
                                    .border(2.dp, themeConfig.accent, RoundedCornerShape(12.dp))
                            ) {
                                NoteContentEditor(
                                    note = activeNote!!,
                                    onUpdate = { title, content, tagsText ->
                                        viewModel.updateActiveNote(title, content, tagsText)
                                    },
                                    onClose = { viewModel.closeActiveNote() },
                                    onDuplicate = { viewModel.duplicateActiveNote() },
                                    onDelete = { viewModel.deleteActiveNote() },
                                    onTogglePin = { viewModel.togglePinNote(activeNote!!.id) },
                                    themeConfig = themeConfig,
                                    themeKey = themeKey,
                                    allTags = tags,
                                    allNotes = notes
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialog: GitHub Sync dialog
        if (showGitHubDialog) {
            GitHubSyncDialog(
                viewModel = viewModel,
                themeConfig = themeConfig,
                isSyncing = viewModel.isSyncing,
                ghStatus = viewModel.ghStatus,
                onDismiss = { showGitHubDialog = false }
            )
        }

        // Dialog: Tag Settings / Manage dialog
        if (showTagDialog) {
            ManageTagsDialog(
                viewModel = viewModel,
                themeConfig = themeConfig,
                onDismiss = { showTagDialog = false }
            )
        }

        // Dialog: Local Backup dialog
        if (showBackupDialog) {
            BackupDialog(
                viewModel = viewModel,
                themeConfig = themeConfig,
                onDismiss = { showBackupDialog = false }
            )
        }
    }
}

// ==========================================
// COMPOSABLE COMPONENT LIBRARIES
// ==========================================

@Composable
fun NoteHeaderBar(
    viewModel: MainViewModel,
    themeConfig: NotesThemeConfig,
    themeKey: String,
    onGitHubSettingsClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    val activeTheme by viewModel.themeState.collectAsStateWithLifecycle()
    var themeDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val success = viewModel.exportBackupToUri(context, uri)
            if (success) {
                Toast.makeText(context, "Backup JSON exported successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to export backup JSON.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = viewModel.importBackupFromUri(context, uri)
            if (success) {
                Toast.makeText(context, "Backup JSON imported successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to import JSON backup.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeConfig.bgSecondary.copy(alpha = 0.85f))
            .drawBehind {
                val strokeWidth = 1f
                val y = size.height - strokeWidth
                drawLine(
                    color = themeConfig.border,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var hamburgerMenuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { hamburgerMenuExpanded = true },
                    modifier = Modifier.testTag("hamburger_menu_btn").size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu Options",
                        tint = themeConfig.accent
                    )
                }

                DropdownMenu(
                    expanded = hamburgerMenuExpanded,
                    onDismissRequest = { hamburgerMenuExpanded = false },
                    modifier = Modifier
                        .background(themeConfig.bgSecondary)
                        .border(1.dp, themeConfig.border)
                ) {
                    Box {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Change aesthetic Theme",
                                        tint = themeConfig.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Change Theme",
                                        fontFamily = themeConfig.fontFamily,
                                        color = themeConfig.textPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                themeDropdownExpanded = true
                            },
                            modifier = Modifier.testTag("theme_picker_btn")
                        )

                        DropdownMenu(
                            expanded = themeDropdownExpanded,
                            onDismissRequest = { themeDropdownExpanded = false },
                            modifier = Modifier
                                .background(themeConfig.bgSecondary)
                                .border(1.dp, themeConfig.border)
                        ) {
                            listOf(
                                "classified" to "🛸 Classified Research",
                                "detective" to "🕵️ Detective Database",
                                "keep-dark" to "📝 Google Keep Dark",
                                "keep" to "📝 Google Keep Light",
                                "occult" to "📜 Occult Archive",
                                "polished" to "✨ Professional Polish",
                                "puppy" to "🐾 Puppy Linux",
                                "roger" to "🎨 Terracotta's Roger",
                                "terminal" to "💾 Retro Terminal",
                                "xfce" to "🖥️ XFCE Desktop"
                            ).forEach { (key, name) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = name,
                                            fontFamily = themeConfig.fontFamily,
                                            color = if (activeTheme == key) themeConfig.accent else themeConfig.textPrimary,
                                            fontWeight = if (activeTheme == key) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.changeTheme(key)
                                        themeDropdownExpanded = false
                                        hamburgerMenuExpanded = false
                                    },
                                    modifier = Modifier.background(if (activeTheme == key) themeConfig.buttonBg else Color.Transparent)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = themeConfig.border,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export JSON Backup",
                                    tint = themeConfig.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Export JSON",
                                    fontFamily = themeConfig.fontFamily,
                                    color = themeConfig.textPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = {
                            hamburgerMenuExpanded = false
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            exportLauncher.launch("backup-NoteRoger-$dateStr.JSON")
                        },
                        modifier = Modifier.testTag("menu_export_json_btn")
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Import JSON Backup",
                                    tint = themeConfig.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Import JSON",
                                    fontFamily = themeConfig.fontFamily,
                                    color = themeConfig.textPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = {
                            hamburgerMenuExpanded = false
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.testTag("menu_import_json_btn")
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "GitHub Saved Configurations",
                                    tint = themeConfig.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub Backup",
                                    fontFamily = themeConfig.fontFamily,
                                    color = themeConfig.textPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = {
                            hamburgerMenuExpanded = false
                            onGitHubSettingsClick()
                        },
                        modifier = Modifier.testTag("menu_github_sync_btn")
                    )
                }
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NoteTaking Roger",
                        style = TextStyle(
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeConfig.accent
                        )
                    )
                }
                Text(
                    text = "Secure Git-persistence dev journal",
                    style = TextStyle(
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        color = themeConfig.textSecondary.copy(alpha = 0.8f)
                    )
                )
            }
        }

        if (themeKey == "polished") {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFD3E2FF), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36)
                )
            }
        }
    }
}

@Composable
fun SearchAndSortRow(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortOrder: String,
    onSortChange: (String) -> Unit,
    themeConfig: NotesThemeConfig,
    themeKey: String,
    allTags: List<String> = emptyList()
) {
    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val currentWord = remember(searchQuery) {
        val lastSpace = searchQuery.lastIndexOf(' ')
        if (lastSpace == -1) searchQuery else searchQuery.substring(lastSpace + 1)
    }
    val searchTagSuggestions = remember(currentWord, allTags) {
        if (currentWord.startsWith("#")) {
            val query = currentWord.removePrefix("#")
            allTags.filter { it.startsWith(query, ignoreCase = true) }.take(8)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        "Search notes or tags (type # for tags)...",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 13.sp,
                        color = themeConfig.textSecondary.copy(alpha = 0.5f)
                    )
                },
                textStyle = TextStyle(
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 14.sp,
                    color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search notes query",
                        tint = if (themeKey == "puppy") Color.Black else themeConfig.textSecondary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .onFocusChanged { isSearchFocused = it.isFocused }
                    .testTag("search_note_input"),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = themeConfig.bgSecondary.copy(alpha = 0.5f),
                    unfocusedContainerColor = themeConfig.bgSecondary.copy(alpha = 0.3f),
                    focusedBorderColor = themeConfig.accent,
                    unfocusedBorderColor = themeConfig.border,
                    focusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary,
                    unfocusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                )
            )

            // Sort configuration wrapper
            Box {
                OutlinedButton(
                    onClick = { sortDropdownExpanded = true },
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("sort_notes_btn"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, themeConfig.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = themeConfig.bgSecondary.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Sort order option select",
                        tint = themeConfig.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (sortOrder) {
                            "title-asc" -> "Title A-Z"
                            "title-desc" -> "Title Z-A"
                            "oldest" -> "Oldest first"
                            else -> "Newest"
                        },
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 12.sp,
                        color = themeConfig.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = sortDropdownExpanded,
                    onDismissRequest = { sortDropdownExpanded = false },
                    modifier = Modifier
                        .background(themeConfig.bgSecondary)
                        .border(1.dp, themeConfig.border)
                ) {
                    listOf(
                        "newest" to "Newest Note First",
                        "oldest" to "Oldest Note First",
                        "title-asc" to "Title A-Z",
                        "title-desc" to "Title Z-A"
                    ).forEach { (orderKey, orderName) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = orderName,
                                    fontFamily = themeConfig.fontFamily,
                                    color = if (sortOrder == orderKey) themeConfig.accent else themeConfig.textPrimary
                                )
                            },
                            onClick = {
                                onSortChange(orderKey)
                                sortDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (isSearchFocused && searchTagSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔍 Filter Tag: ",
                    fontSize = 11.sp,
                    fontFamily = themeConfig.fontFamily,
                    color = if (themeKey == "puppy") Color.Black else themeConfig.textSecondary
                )
                searchTagSuggestions.forEach { tagSuggestion ->
                    AutocompleteChip(
                        text = "#$tagSuggestion",
                        onClick = {
                            val lastSpace = searchQuery.lastIndexOf(' ')
                            val newQuery = if (lastSpace == -1) {
                                "#$tagSuggestion"
                            } else {
                                searchQuery.substring(0, lastSpace + 1) + "#$tagSuggestion"
                            }
                            onSearchChange(newQuery + " ")
                        },
                        themeConfig = themeConfig,
                        themeKey = themeKey
                    )
                }
            }
        }
    }
}

@Composable
fun TagsFilterRow(
    tags: List<String>,
    selectedTags: Set<String>,
    onTagSelect: (String) -> Unit,
    onClearFilters: () -> Unit,
    onManageTagsClick: () -> Unit,
    themeConfig: NotesThemeConfig,
    themeKey: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tag icon or Clear tag Filters
        if (selectedTags.isNotEmpty()) {
            InputChip(
                selected = true,
                onClick = onClearFilters,
                label = { Text("Clear Filters", fontSize = 11.sp, fontFamily = themeConfig.fontFamily) },
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = Color(0xFFC41E3A).copy(alpha = 0.3f),
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFFC41E3A).copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Horizontal scroll list of tags
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                val chipShape = if (themeKey == "polished") RoundedCornerShape(8.dp) else RoundedCornerShape(6.dp)
                val chipColors = if (themeKey == "polished") {
                    FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFFE0E2EC),
                        labelColor = Color(0xFF44474E),
                        selectedContainerColor = themeConfig.accent,
                        selectedLabelColor = Color.White
                    )
                } else if (themeKey == "puppy") {
                    FilterChipDefaults.filterChipColors(
                        containerColor = themeConfig.bgSecondary.copy(alpha = 0.4f),
                        labelColor = Color.Black,
                        selectedContainerColor = themeConfig.accent.copy(alpha = 0.25f),
                        selectedLabelColor = Color.Black
                    )
                } else {
                    FilterChipDefaults.filterChipColors(
                        containerColor = themeConfig.bgSecondary.copy(alpha = 0.4f),
                        labelColor = themeConfig.textSecondary,
                        selectedContainerColor = themeConfig.accent.copy(alpha = 0.25f),
                        selectedLabelColor = themeConfig.accent
                    )
                }
                val chipBorderColor = if (themeKey == "polished") {
                    Color.Transparent
                } else {
                    if (isSelected) themeConfig.accent else themeConfig.border
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onTagSelect(tag) },
                    label = {
                        Text(
                            text = "#$tag",
                            fontSize = 11.sp,
                            fontFamily = themeConfig.fontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (themeKey == "puppy") Color.Black else Color.Unspecified
                        )
                    },
                    shape = chipShape,
                    colors = chipColors,
                    border = BorderStroke(
                        width = if (themeKey == "polished") 0.dp else 1.dp,
                        color = chipBorderColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Tool tag configurations button
        IconButton(
            onClick = onManageTagsClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("tag_settings_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Manage system tags",
                tint = themeConfig.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun NotesGrid(
    notes: List<Note>,
    activeNoteId: Long?,
    onSelectNote: (Long) -> Unit,
    themeConfig: NotesThemeConfig,
    themeKey: String,
    isCompact: Boolean,
    onTogglePin: (Long) -> Unit
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "No saved notes present details",
                    tint = themeConfig.textSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No developer notes found.",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 14.sp,
                    color = themeConfig.textSecondary.copy(alpha = 0.6f)
                )
                Text(
                    text = "Use the plus FAB to begin your log stream.",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = themeConfig.textSecondary.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isCompact) 1 else 2),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("notes_grid"),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                val isActive = note.id == activeNoteId
                NoteCard(
                    note = note,
                    isActive = isActive,
                    onClick = { onSelectNote(note.id) },
                    onTogglePin = onTogglePin,
                    themeConfig = themeConfig,
                    themeKey = themeKey
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    isActive: Boolean,
    onClick: () -> Unit,
    onTogglePin: (Long) -> Unit,
    themeConfig: NotesThemeConfig,
    themeKey: String
) {
    // Unique theme visual styles for the card backgrounds
    val cardBackground = when (themeKey) {
        "polished" -> if (isActive) themeConfig.buttonBg else themeConfig.bgCard
        "detective" -> themeConfig.bgCard             // vintage parchment yellow paper!
        "puppy" -> themeConfig.bgCard                 // pale document yellow
        "xfce" -> if (isActive) Color(0xFFCFE0F8) else themeConfig.bgCard
        else -> if (isActive) themeConfig.buttonBg else themeConfig.bgSecondary.copy(alpha = 0.65f)
    }

    val cardBorderColor = if (isActive) {
        themeConfig.accent
    } else {
        if (themeKey == "polished") themeConfig.border else themeConfig.border.copy(alpha = 0.4f)
    }

    val actualTextOnCardColor = when (themeKey) {
        "detective" -> themeConfig.textOnCard
        "puppy" -> themeConfig.textOnCard
        else -> themeConfig.textPrimary
    }

    val cardShape = if (themeKey == "polished") RoundedCornerShape(16.dp) else RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 200.dp)
            .clip(cardShape)
            .background(cardBackground)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = cardBorderColor,
                shape = cardShape
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.title.ifEmpty { "Untitled Log" },
                fontFamily = themeConfig.fontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (themeKey == "terminal") Color(0xFFFFFF00) else if (themeKey == "detective" || themeKey == "puppy") themeConfig.textOnCard else themeConfig.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Pin toggle icon inside NoteCard header
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable { onTogglePin(note.id) }
                    .testTag("pin_note_card_btn_${note.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                    tint = if (note.isPinned) {
                        if (themeKey == "puppy") Color.Black else themeConfig.accent
                    } else {
                        (if (themeKey == "detective" || themeKey == "puppy") themeConfig.textOnCard else themeConfig.textSecondary).copy(alpha = 0.35f)
                    },
                    modifier = Modifier.size(14.dp)
                )
            }

            if (themeKey == "polished") {
                val statusType = note.id % 3
                val (bg, txtColor, label) = when (statusType) {
                    0L -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Synced")
                    1L -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Local Only")
                    else -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Conflict")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(bg, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = themeConfig.fontFamily,
                        color = txtColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = note.content.ifEmpty { "No written documentation content..." },
            fontFamily = themeConfig.fontFamily,
            fontSize = 12.sp,
            color = actualTextOnCardColor.copy(alpha = 0.85f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Show horizontal scroll of individual tags assigned to this note
        if (note.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                note.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (themeKey == "detective" || themeKey == "puppy") {
                                    themeConfig.textSecondary.copy(alpha = 0.12f)
                                } else {
                                    themeConfig.accent.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 9.sp,
                            fontFamily = themeConfig.fontFamily,
                            color = if (themeKey == "detective" || themeKey == "puppy") {
                                themeConfig.textSecondary
                            } else {
                                themeConfig.accent
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

class DeveloperSyntaxHighlighter(val themeConfig: NotesThemeConfig) : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val rawText = text.text
        val builder = androidx.compose.ui.text.AnnotatedString.Builder(rawText)
        
        // 1. Find block code matches
        val blockCodeRegex = Regex("(?s)```.*?```")
        val blockCodeMatches = blockCodeRegex.findAll(rawText).toList()
        
        // 2. Find inline backticks matches (excluding those inside block code)
        val backtickRegex = Regex("`[^`\\n]+`")
        val backtickMatches = backtickRegex.findAll(rawText).filter { match ->
            blockCodeMatches.none { block -> match.range.first >= block.range.first && match.range.last <= block.range.last }
        }.toList()

        // Helper to check if a range overlaps with block code or backticks
        val inCode = { start: Int, end: Int ->
            blockCodeMatches.any { block -> start >= block.range.first && end <= block.range.last } ||
            backtickMatches.any { back -> start >= back.range.first && end <= back.range.last }
        }

        // Apply style to block code matches
        for (match in blockCodeMatches) {
            builder.addStyle(
                androidx.compose.ui.text.SpanStyle(
                    background = themeConfig.bgSecondary.copy(alpha = 0.6f),
                    color = themeConfig.textPrimary.copy(alpha = 0.9f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        // Apply style to inline backticks
        for (match in backtickMatches) {
            builder.addStyle(
                androidx.compose.ui.text.SpanStyle(
                    background = themeConfig.bgSecondary.copy(alpha = 0.5f),
                    color = themeConfig.textSecondary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        // 3. Highlight headers (not in code)
        val headerRegex = Regex("^(#+)\\s+([^\\n]*)", RegexOption.MULTILINE)
        for (match in headerRegex.findAll(rawText)) {
            val start = match.range.first
            val end = match.range.last
            if (!inCode(start, end)) {
                val hashesGroup = match.groups[1]
                val textGroup = match.groups[2]
                if (hashesGroup != null && textGroup != null) {
                    val level = hashesGroup.value.length
                    // Format hashes to be slightly muted but bold
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = themeConfig.accent.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        ),
                        hashesGroup.range.first,
                        hashesGroup.range.last + 1
                    )
                    
                    // Format header text
                    val headerColor = when (level) {
                        1 -> themeConfig.accent
                        2 -> themeConfig.textSecondary
                        else -> themeConfig.textPrimary
                    }
                    val headerSize = when (level) {
                        1 -> 18.sp
                        2 -> 16.sp
                        else -> 14.sp
                    }
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = headerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = headerSize
                        ),
                        textGroup.range.first,
                        textGroup.range.last + 1
                    )
                }
            }
        }

        // 4. Highlight list bullet markers (not in code)
        val listRegex = Regex("^[ \\t]*([*+-]|\\d+\\.)\\s", RegexOption.MULTILINE)
        for (match in listRegex.findAll(rawText)) {
            val start = match.range.first
            val end = match.range.last
            if (!inCode(start, end)) {
                val marker = match.groups[1]
                if (marker != null) {
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = themeConfig.accent,
                            fontWeight = FontWeight.Bold
                        ),
                        marker.range.first,
                        marker.range.last + 1
                    )
                }
            }
        }

        // 5. Highlight Blockquotes (not in code)
        val blockquoteRegex = Regex("^>[ \\t]*(.*)", RegexOption.MULTILINE)
        for (match in blockquoteRegex.findAll(rawText)) {
            val start = match.range.first
            val end = match.range.last
            if (!inCode(start, end)) {
                // Style the '>' symbol
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(
                        color = themeConfig.accent,
                        fontWeight = FontWeight.Bold
                    ),
                    start,
                    start + 1
                )
                // Style inner text as italic and secondary
                val textContent = match.groups[1]
                if (textContent != null && textContent.value.isNotEmpty()) {
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = themeConfig.textSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        textContent.range.first,
                        textContent.range.last + 1
                    )
                }
            }
        }

        // 6. Highlight Bold text (not in code)
        val boldRegex = Regex("\\*\\*([^\\n*]+?)\\*\\*|__([^\\n_]+?)__")
        for (match in boldRegex.findAll(rawText)) {
            val start = match.range.first
            val end = match.range.last
            if (!inCode(start, end)) {
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    end + 1
                )
                
                // Mute the ** or __ markers
                val markerLen = if (match.value.startsWith("**") || match.value.startsWith("__")) 2 else 0
                if (markerLen > 0) {
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(color = themeConfig.textSecondary.copy(alpha = 0.5f)),
                        start,
                        start + markerLen
                    )
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(color = themeConfig.textSecondary.copy(alpha = 0.5f)),
                        end + 1 - markerLen,
                        end + 1
                    )
                }
            }
        }

        // 7. Highlight Italic text (not in code)
        val italicRegex = Regex("(?<!\\*)\\*([^\\n*]+?)\\*(?!\\*)|(?<!_)_([^\\n_]+?)_(?!_)")
        for (match in italicRegex.findAll(rawText)) {
            val start = match.range.first
            val end = match.range.last
            if (!inCode(start, end)) {
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    start,
                    end + 1
                )
                
                // Mute the * or _ markers
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(color = themeConfig.textSecondary.copy(alpha = 0.5f)),
                    start,
                    start + 1
                )
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(color = themeConfig.textSecondary.copy(alpha = 0.5f)),
                    end,
                    end + 1
                )
            }
        }

        return androidx.compose.ui.text.input.TransformedText(builder.toAnnotatedString(), androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}

@Composable
fun RichFormattingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    textLabel: String? = null,
    tooltip: String,
    themeConfig: NotesThemeConfig,
    themeKey: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (themeKey == "puppy") Color(0xFFD6D0C0) else themeConfig.textSecondary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = if (themeKey == "puppy") Color(0xFFA09888) else themeConfig.border.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .testTag("formatting_btn_${tooltip.replace(" ", "_").lowercase(Locale.getDefault())}")
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltip,
                    tint = if (themeKey == "puppy") Color.Black else themeConfig.accent,
                    modifier = Modifier.size(13.dp)
                )
            }
            if (textLabel != null) {
                Text(
                    text = textLabel,
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                )
            }
        }
    }
}

@Composable
fun AutocompleteChip(
    text: String,
    onClick: () -> Unit,
    themeConfig: NotesThemeConfig,
    themeKey: String
) {
    Box(
        modifier = Modifier
            .background(
                color = if (themeKey == "puppy") Color(0xFFD6D0C0) else themeConfig.bgPrimary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (themeKey == "puppy") Color(0xFFA09888) else themeConfig.border.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontFamily = themeConfig.fontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
        )
    }
}

@Composable
fun NoteContentEditor(
    note: Note,
    onUpdate: (title: String, content: String, tags: String) -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    themeConfig: NotesThemeConfig,
    themeKey: String,
    allTags: List<String> = emptyList(),
    allNotes: List<Note> = emptyList()
) {
    val context = LocalContext.current
    var title by remember(note.id) { mutableStateOf(note.title) }
    var saveStatus by remember(note.id) { mutableStateOf("Saved") }
    var isDirty by remember(note.id) { mutableStateOf(false) }
    var contentValue by remember(note.id) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(note.content)) }
    var undoStack by remember(note.id) { mutableStateOf(listOf<androidx.compose.ui.text.input.TextFieldValue>()) }
    var redoStack by remember(note.id) { mutableStateOf(listOf<androidx.compose.ui.text.input.TextFieldValue>()) }
    var lastEditTime by remember(note.id) { mutableStateOf(0L) }

    fun updateContent(newValue: androidx.compose.ui.text.input.TextFieldValue, forcePush: Boolean = false) {
        val oldValue = contentValue
        if (oldValue.text != newValue.text) {
            val now = System.currentTimeMillis()
            val timeDiff = now - lastEditTime
            val isWordBoundary = newValue.text.length > oldValue.text.length && 
                                 (newValue.text.endsWith(" ") || newValue.text.endsWith("\n") || newValue.text.endsWith("\t"))
                                 
            val shouldPush = forcePush || 
                             undoStack.isEmpty() || 
                             timeDiff > 1200L || 
                             isWordBoundary || 
                             kotlin.math.abs(newValue.text.length - oldValue.text.length) > 1
            
            if (shouldPush) {
                if (undoStack.isEmpty() || undoStack.last().text != oldValue.text) {
                    undoStack = undoStack + oldValue
                }
            }
            redoStack = emptyList()
            lastEditTime = now
            isDirty = true
        }
        contentValue = newValue
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val prevState = undoStack.last()
            undoStack = undoStack.dropLast(1)
            redoStack = redoStack + contentValue
            contentValue = prevState
            isDirty = true
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.last()
            redoStack = redoStack.dropLast(1)
            undoStack = undoStack + contentValue
            contentValue = nextState
            isDirty = true
        }
    }

    var tagsCommaStr by remember(note.id) { mutableStateOf(note.tags.joinToString(", ")) }

    var previewMode by remember { mutableStateOf("edit") }

    var isTitleFocused by remember { mutableStateOf(false) }
    var isTagsFocused by remember { mutableStateOf(false) }
    var isBodyFocused by remember { mutableStateOf(false) }

    val currentWordBeingTyped = remember(contentValue) {
        val text = contentValue.text
        val cursor = contentValue.selection.start
        if (cursor <= 0 || text.isEmpty()) ""
        else {
            val beforeCursor = text.substring(0, cursor)
            val lastSpace = beforeCursor.lastIndexOfAny(charArrayOf(' ', '\n', '\t'))
            if (lastSpace == -1) beforeCursor else beforeCursor.substring(lastSpace + 1)
        }
    }

    fun insertTextAtCursor(textToInsert: String) {
        val text = contentValue.text
        val cursor = contentValue.selection.start
        val beforeCursor = text.substring(0, cursor)
        val lastSpace = beforeCursor.lastIndexOfAny(charArrayOf(' ', '\n', '\t'))
        val startOfWord = if (lastSpace == -1) 0 else lastSpace + 1
        val newText = text.substring(0, startOfWord) + textToInsert + text.substring(cursor)
        val newCursor = startOfWord + textToInsert.length
        updateContent(
            androidx.compose.ui.text.input.TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newCursor, newCursor)
            ),
            forcePush = true
        )
    }

    var isDictating by remember { mutableStateOf(false) }
    var dictationStatus by remember { mutableStateOf("") }
    var dictationRms by remember { mutableStateOf(0f) }

    val speechRecognizer = remember {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore destruction error on exit
            }
        }
    }

    fun insertDictatedTextAtCursor(textToInsert: String) {
        val text = contentValue.text
        val cursor = contentValue.selection.start
        val formattedInsert = if (cursor > 0 && !text[cursor - 1].isWhitespace() && !textToInsert.startsWith(" ")) {
            " $textToInsert"
        } else {
            textToInsert
        }
        val newText = text.substring(0, cursor) + formattedInsert + text.substring(cursor)
        val newCursor = cursor + formattedInsert.length
        updateContent(
            androidx.compose.ui.text.input.TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newCursor, newCursor)
            ),
            forcePush = true
        )
    }

    fun toggleLineCheckbox(lineIndex: Int) {
        val lines = contentValue.text.replace("\r\n", "\n").split("\n").toMutableList()
        if (lineIndex in lines.indices) {
            val originalLine = lines[lineIndex]
            val trimmed = originalLine.trim()
            val formatted = if (trimmed.startsWith("- [ ]")) {
                originalLine.replaceFirst("- [ ]", "- [x]")
            } else if (trimmed.startsWith("- [x]")) {
                originalLine.replaceFirst("- [x]", "- [ ]")
            } else if (trimmed.startsWith("* [ ]")) {
                originalLine.replaceFirst("* [ ]", "* [x]")
            } else if (trimmed.startsWith("* [x]")) {
                originalLine.replaceFirst("* [x]", "* [ ]")
            } else {
                originalLine
            }
            lines[lineIndex] = formatted
            val newText = lines.joinToString("\n")
            val cursor = contentValue.selection.start
            val clampedCursor = cursor.coerceAtMost(newText.length)
            updateContent(
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(clampedCursor, clampedCursor)
                ),
                forcePush = true
            )
        }
    }

    fun applyFormatting(formatType: String) {
        val text = contentValue.text
        val selection = contentValue.selection
        val start = selection.min
        val end = selection.max
        
        val selectedText = text.substring(start, end)
        
        val (newText, newSelectionStart, newSelectionEnd) = when (formatType) {
            "bold" -> {
                val formatted = "**$selectedText**"
                val offset = if (selectedText.isEmpty()) 2 else 0
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + 2,
                    end + 2 + offset
                )
            }
            "italic" -> {
                val formatted = "*$selectedText*"
                val offset = if (selectedText.isEmpty()) 1 else 0
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + 1,
                    end + 1 + offset
                )
            }
            "code_inline" -> {
                val formatted = "`$selectedText`"
                val offset = if (selectedText.isEmpty()) 1 else 0
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + 1,
                    end + 1 + offset
                )
            }
            "code_block" -> {
                val formatted = "\n```kotlin\n$selectedText\n```\n"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + 11,
                    start + 11 + selectedText.length
                )
            }
            "h1" -> {
                val formatted = "\n# $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            "h2" -> {
                val formatted = "\n## $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            "h3" -> {
                val formatted = "\n### $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            "list_bullet" -> {
                val formatted = "\n- $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            "list_todo" -> {
                val formatted = "\n- [ ] $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            "blockquote" -> {
                val formatted = "\n> $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            "callout_info" -> {
                val formatted = "\n> [!INFO]\n> $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + 11,
                    start + 11 + selectedText.length
                )
            }
            "callout_warning" -> {
                val formatted = "\n> [!WARNING]\n> $selectedText"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + 14,
                    start + 14 + selectedText.length
                )
            }
            "table" -> {
                val formatted = "\n| Header 1 | Header 2 |\n|---|---|\n| Cell 1 | Cell 2 |"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            "link" -> {
                val formatted = if (selectedText.isEmpty()) "[Title](url)" else "[$selectedText](url)"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + 1,
                    start + 1 + (if (selectedText.isEmpty()) 5 else selectedText.length)
                )
            }
            "divider" -> {
                val formatted = "\n\n---\n\n"
                Triple(
                    text.substring(0, start) + formatted + text.substring(end),
                    start + formatted.length,
                    start + formatted.length
                )
            }
            else -> Triple(text, start, end)
        }
        
        updateContent(
            androidx.compose.ui.text.input.TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)
            ),
            forcePush = true
        )
    }

    fun startSpeechToText(ctx: Context) {
        if (speechRecognizer == null || !SpeechRecognizer.isRecognitionAvailable(ctx)) {
            Toast.makeText(ctx, "Speech Recognition is not available on this device", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isDictating = true
                dictationStatus = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                dictationStatus = "Dictating..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                dictationRms = rmsdB
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                dictationStatus = "Processing..."
            }

            override fun onError(error: Int) {
                isDictating = false
                dictationRms = 0f
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client-side speech error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match, please speak again"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Google speech service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server-side speech error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input received"
                    else -> "Speech recognition error"
                }
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    if (spokenText.isNotEmpty()) {
                        insertDictatedTextAtCursor(spokenText)
                    }
                }
                isDictating = false
                dictationRms = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    if (spokenText.isNotEmpty()) {
                        dictationStatus = "Hearing: \"$spokenText\""
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
            isDictating = true
            dictationStatus = "Initializing..."
        } catch (e: Exception) {
            isDictating = false
            Toast.makeText(ctx, "Failed to start listening: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopSpeechToText() {
        if (isDictating) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            isDictating = false
            dictationRms = 0f
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechToText(context)
        } else {
            Toast.makeText(context, "Microphone permission denied. Cannot dictate.", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkPermissionAndStartDictation() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startSpeechToText(context)
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }
    val wordCount = contentValue.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    val charCount = contentValue.text.length
    val paragraphCount = if (contentValue.text.isEmpty()) 0 else contentValue.text.split(Regex("\\n+")).filter { it.trim().isNotEmpty() }.size
    val readingTimeMin = kotlin.math.ceil(wordCount / 200.0).toInt()

    // Track original content for updates sync trigger with debounced auto-save
    LaunchedEffect(title, contentValue.text, tagsCommaStr) {
        val hasChanges = title != note.title || 
                         contentValue.text != note.content || 
                         tagsCommaStr != note.tags.joinToString(", ")
        if (hasChanges) {
            isDirty = true
            saveStatus = "Saving..."
            kotlinx.coroutines.delay(1000) // Debounce for 1 second of typing silence
            onUpdate(title, contentValue.text, tagsCommaStr)
            isDirty = false
            saveStatus = "Saved"
            Toast.makeText(context, "Note saved successfully!", Toast.LENGTH_SHORT).show()
        } else {
            saveStatus = "Saved"
            isDirty = false
        }
    }

    // Capture latest parameters to run the save on disposal
    val currentOnUpdate = rememberUpdatedState(onUpdate)
    val curTitle = rememberUpdatedState(title)
    val curContent = rememberUpdatedState(contentValue.text)
    val curTags = rememberUpdatedState(tagsCommaStr)
    val curIsDirty = rememberUpdatedState(isDirty)

    DisposableEffect(note.id) {
        onDispose {
            if (curIsDirty.value) {
                currentOnUpdate.value(curTitle.value, curContent.value, curTags.value)
                Toast.makeText(context, "Note saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Wrap Helper inside selection block
    fun wrapSelection(prefix: String, suffix: String) {
        val text = contentValue.text
        val selection = contentValue.selection
        val start = selection.start
        val end = selection.end
        val selectedText = text.substring(start, end)
        
        val replacement = prefix + selectedText + suffix
        val newText = text.substring(0, start) + replacement + text.substring(end)
        val newCursorPos = start + prefix.length + selectedText.length + suffix.length
        contentValue = androidx.compose.ui.text.input.TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursorPos, newCursorPos)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeConfig.bgSecondary)
            .padding(16.dp)
    ) {
        // Toolbar controls inside active editor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status Indicator
                val indicatorColor = when (saveStatus) {
                    "Saving..." -> themeConfig.accent
                    else -> Color(0xFF2E7D32)
                }
                Box(
                    modifier = Modifier
                        .background(indicatorColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                        .border(1.dp, indicatorColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(indicatorColor, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (saveStatus == "Saving...") "Saving..." else "Autosaved",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = indicatorColor
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        onClick = onClose,
                        role = androidx.compose.ui.semantics.Role.Button
                    )
                    .testTag("close_note_btn"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = themeConfig.textSecondary.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .border(1.dp, themeConfig.textSecondary.copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Collapse active editor",
                        tint = themeConfig.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal scrolling actions row below word count and character count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // (0) Voice Dictation (Speech-to-Text)
            Box(
                modifier = Modifier
                    .background(
                        color = if (isDictating) {
                            Color(0xFFD32F2F).copy(alpha = 0.15f)
                        } else {
                            if (themeKey == "puppy") Color(0xFFD6D0C0) else themeConfig.textSecondary.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = if (isDictating) 1.5.dp else if (themeKey == "puppy") 1.dp else 0.dp,
                        color = if (isDictating) Color(0xFFD32F2F) else if (themeKey == "puppy") Color(0xFFA09888) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        if (isDictating) {
                            stopSpeechToText()
                        } else {
                            checkPermissionAndStartDictation()
                        }
                    }
                    .testTag("action_dictate_note_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isDictating) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse_mic")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(550, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale_mic"
                        )
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Stop Dictation",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier
                                .size(14.dp)
                                .scale(scale)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start Dictation",
                            tint = if (themeKey == "puppy") Color.Black else themeConfig.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (isDictating) "Stop Dictating" else "Dictate",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDictating) Color(0xFFD32F2F) else (if (themeKey == "puppy") Color.Black else themeConfig.textSecondary)
                    )
                }
            }

            // (1) copy content of the note
            Box(
                modifier = Modifier
                    .background(themeConfig.textSecondary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("developer_note_taking", contentValue.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Note body copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("action_copy_content_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text content",
                        tint = themeConfig.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Copy Content",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textSecondary
                    )
                }
            }

            // (1.3) Print to PDF
            Box(
                modifier = Modifier
                    .background(themeConfig.textSecondary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable {
                        printNoteToPdf(context, note, title, contentValue.text, themeConfig)
                    }
                    .testTag("action_print_pdf_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print note to PDF",
                        tint = themeConfig.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Print to PDF",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textSecondary
                    )
                }
            }

            // (1.5) Pin/Unpin Note
            Box(
                modifier = Modifier
                    .background(
                        color = if (note.isPinned) {
                            themeConfig.accent.copy(alpha = 0.15f)
                        } else {
                            if (themeKey == "puppy") Color(0xFFD6D0C0) else themeConfig.textSecondary.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onTogglePin() }
                    .testTag("action_pin_note_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "Unpin current note" else "Pin current note",
                        tint = if (note.isPinned) {
                            if (themeKey == "puppy") Color.Black else themeConfig.accent
                        } else {
                            if (themeKey == "puppy") Color.Black else themeConfig.accent
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (note.isPinned) "Unpin Note" else "Pin Note",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (note.isPinned && themeKey == "puppy") Color.Black else themeConfig.textSecondary
                    )
                }
            }

            // (2) create a copy of the note (The copy has "[Copy]" added to the right side of the title - handled in the VM)
            Box(
                modifier = Modifier
                    .background(themeConfig.textSecondary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable { onDuplicate() }
                    .testTag("action_duplicate_note_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileCopy,
                        contentDescription = "Duplicate current note",
                        tint = themeConfig.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Duplicate Note",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textSecondary
                    )
                }
            }

            // (3) close note icon
            Box(
                modifier = Modifier
                    .background(themeConfig.textSecondary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable { onClose() }
                    .testTag("action_close_note_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close current note",
                        tint = themeConfig.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Close",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textSecondary
                    )
                }
            }

            // (4) delete note icon
            Box(
                modifier = Modifier
                    .background(Color(0xFFC41E3A).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { onDelete() }
                    .testTag("action_delete_note_btn")
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete current note",
                        tint = Color(0xFFC41E3A),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Delete",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC41E3A)
                    )
                }
            }
        }

        if (isDictating) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFD32F2F).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFD32F2F).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val scale by rememberInfiniteTransition(label = "pulse_banner").animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(scale)
                            .background(Color(0xFFD32F2F), androidx.compose.foundation.shape.CircleShape)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Speech Dictation Active",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dictationStatus,
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                        )
                    }
                    Button(
                        onClick = { stopSpeechToText() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title Input text
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = {
                Text(
                    "Untitled Document...",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 20.sp,
                    color = themeConfig.textSecondary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            },
            textStyle = TextStyle(
                fontFamily = themeConfig.fontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (themeKey == "terminal") Color(0xFFFFFF00) else if (themeKey == "puppy") Color.Black else themeConfig.accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isTitleFocused = it.isFocused }
                .testTag("note_editor_title"),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = themeConfig.accent,
                unfocusedIndicatorColor = themeConfig.border.copy(alpha = 0.5f),
                focusedTextColor = if (themeKey == "puppy") Color.Black else (if (themeKey == "terminal") Color(0xFFFFFF00) else themeConfig.accent),
                unfocusedTextColor = if (themeKey == "puppy") Color.Black else (if (themeKey == "terminal") Color(0xFFFFFF00) else themeConfig.accent)
            )
        )

        if (isTitleFocused) {
            val titleSuggestions = remember(title, allNotes) {
                val list = mutableListOf<String>()
                
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayJournal = "Daily Journal - $todayStr"
                val dreamJournal = "Dream Log - $todayStr"
                
                if (!title.equals(todayJournal, ignoreCase = true)) list.add(todayJournal)
                if (!title.equals(dreamJournal, ignoreCase = true)) list.add(dreamJournal)
                list.add("Meeting Notes")
                list.add("Weekly Review")
                list.add("Sprint Description")
                list.add("Brainstorming List")
                
                if (title.isNotEmpty()) {
                    val matchingTitles = allNotes.map { it.title }
                        .filter { it.contains(title, ignoreCase = true) && !it.equals(title, ignoreCase = true) }
                        .take(3)
                    list.addAll(matchingTitles)
                }
                
                if (title.isNotEmpty()) {
                    list.filter { it.contains(title, ignoreCase = true) }
                } else {
                    list
                }
            }
            
            if (titleSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💡 Completions: ", 
                        fontSize = 11.sp, 
                        fontFamily = themeConfig.fontFamily, 
                        color = if (themeKey == "puppy") Color.Black else themeConfig.textSecondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    titleSuggestions.forEach { sug ->
                        AutocompleteChip(
                            text = sug,
                            onClick = { title = sug },
                            themeConfig = themeConfig,
                            themeKey = themeKey
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tags Input text
        OutlinedTextField(
            value = tagsCommaStr,
            onValueChange = { tagsCommaStr = it },
            placeholder = {
                Text(
                    "comma-separated-tags (e.g., rust, build, api)",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 12.sp,
                    color = themeConfig.textSecondary.copy(alpha = 0.4f)
                )
            },
            textStyle = TextStyle(
                fontFamily = themeConfig.fontFamily,
                fontSize = 13.sp,
                color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
            ),
            label = {
                Text(
                    "Assigned Filter Tags",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = if (themeKey == "puppy") Color.Black else themeConfig.accent
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isTagsFocused = it.isFocused }
                .testTag("note_editor_tags"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeConfig.accent,
                unfocusedBorderColor = themeConfig.border,
                focusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary,
                unfocusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
            )
        )

        if (isTagsFocused) {
            val currentTagsList = remember(tagsCommaStr) {
                tagsCommaStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            val currentTagQuery = remember(tagsCommaStr) {
                val parts = tagsCommaStr.split(",")
                parts.lastOrNull()?.trim() ?: ""
            }
            
            val tagSuggestions = remember(currentTagQuery, allTags) {
                val queried = if (currentTagQuery.isNotEmpty()) {
                    allTags.filter { it.startsWith(currentTagQuery, ignoreCase = true) && !currentTagsList.contains(it) }
                } else {
                    allTags.filter { !currentTagsList.contains(it) }
                }
                val defaults = listOf("daily", "dream", "journal", "notes", "archive")
                    .filter { it.startsWith(currentTagQuery, ignoreCase = true) && !currentTagsList.contains(it) && !queried.contains(it) }
                
                val combined = (queried + defaults).toMutableList()
                if (currentTagQuery.isNotEmpty() && !combined.contains(currentTagQuery) && !currentTagsList.contains(currentTagQuery)) {
                    combined.add(0, currentTagQuery)
                }
                combined
            }
            
            if (tagSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💡 Completions: ", 
                        fontSize = 11.sp, 
                        fontFamily = themeConfig.fontFamily, 
                        color = if (themeKey == "puppy") Color.Black else themeConfig.textSecondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    tagSuggestions.forEach { tagSuggestion ->
                        val isNew = currentTagQuery.isNotEmpty() && tagSuggestion == currentTagQuery && !allTags.contains(currentTagQuery)
                        val chipText = if (isNew) "➕ Create #$tagSuggestion" else "#$tagSuggestion"
                        AutocompleteChip(
                            text = chipText,
                            onClick = {
                                val parts = tagsCommaStr.split(",").toMutableList()
                                if (parts.isNotEmpty()) {
                                    parts.removeAt(parts.lastIndex)
                                }
                                parts.add(tagSuggestion)
                                tagsCommaStr = parts.joinToString(", ") + ", "
                            },
                            themeConfig = themeConfig,
                            themeKey = themeKey
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Markdown Mode Selector bar with Undo/Redo controls placed to the left
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo & Redo circular arrows group
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo Button
                IconButton(
                    onClick = { handleUndo() },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (undoStack.isNotEmpty()) {
                                if (themeKey == "puppy") Color(0xFFE8E2D2) else themeConfig.bgSecondary
                            } else {
                                if (themeKey == "puppy") Color(0xFFE8E2D2).copy(alpha = 0.4f) else themeConfig.bgSecondary.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (undoStack.isNotEmpty()) themeConfig.border else themeConfig.border.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .testTag("undo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (undoStack.isNotEmpty()) {
                            if (themeKey == "puppy") Color.Black else themeConfig.accent
                        } else {
                            if (themeKey == "puppy") Color.Gray.copy(alpha = 0.4f) else themeConfig.textSecondary.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Redo Button
                IconButton(
                    onClick = { handleRedo() },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (redoStack.isNotEmpty()) {
                                if (themeKey == "puppy") Color(0xFFE8E2D2) else themeConfig.bgSecondary
                            } else {
                                if (themeKey == "puppy") Color(0xFFE8E2D2).copy(alpha = 0.4f) else themeConfig.bgSecondary.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (redoStack.isNotEmpty()) themeConfig.border else themeConfig.border.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .testTag("redo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) {
                            if (themeKey == "puppy") Color.Black else themeConfig.accent
                        } else {
                            if (themeKey == "puppy") Color.Gray.copy(alpha = 0.4f) else themeConfig.textSecondary.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Selector Row
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(themeConfig.bgPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .border(1.dp, themeConfig.border, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "edit" to "📝 Edit",
                    "split" to "📖 Split View",
                    "preview" to "👁️ Preview"
                ).forEach { (mode, label) ->
                    val isSelected = previewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) themeConfig.buttonBg else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { previewMode = mode }
                            .testTag("markdown_mode_${mode}_btn")
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) themeConfig.textPrimary else themeConfig.textSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val bodySuggestions = remember(currentWordBeingTyped, allTags, allNotes) {
            val list = mutableListOf<Pair<String, String>>() // Pair of (Label, InsertionText)
            
            if (currentWordBeingTyped.startsWith("#")) {
                val query = currentWordBeingTyped.removePrefix("#")
                allTags.filter { it.startsWith(query, ignoreCase = true) }
                    .forEach { tag ->
                         list.add("#$tag" to "#$tag ")
                    }
            } else if (currentWordBeingTyped.startsWith("[[") || currentWordBeingTyped.startsWith("@")) {
                val isAt = currentWordBeingTyped.startsWith("@")
                val prefix = if (isAt) "@" else "[["
                val query = currentWordBeingTyped.removePrefix(prefix)
                allNotes.filter { it.title.contains(query, ignoreCase = true) }
                    .forEach { note ->
                        val insertText = if (isAt) note.title else "[[${note.title}]]"
                        list.add("📄 ${note.title}" to insertText + " ")
                    }
            } else {
                list.add("🎯 Goals" to "\n## 🎯 Goals for Today\n- [ ] Goal 1: \n- [ ] Goal 2: \n")
                list.add("💤 Dreams" to "\n## 💤 Dreams\n*Recall details of your recent dreams here:*\n")
                list.add("📝 Reflections" to "\n## 📝 Reflections\n- **Current Mindset:** \n- **What went well today?** \n")
                list.add("🛠️ Tasks" to "\n## 🛠️ Tasks\n- [ ] Task 1: \n- [ ] Task 2: \n")
                list.add("☑️ Task" to "- [ ] ")
                list.add("⭐ Bold" to "**text**")
                list.add("✍️ Italic" to "*text*")
                list.add("💻 Code Block" to "\n```kotlin\n\n```\n")
                list.add("🔗 Link" to "[Title](url)")
                
                if (currentWordBeingTyped.isNotEmpty()) {
                    val filtered = list.filter { it.first.contains(currentWordBeingTyped, ignoreCase = true) || it.second.contains(currentWordBeingTyped, ignoreCase = true) }
                    if (filtered.isNotEmpty()) {
                        return@remember filtered
                    }
                }
            }
            list
        }

        if (isBodyFocused && bodySuggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Insert: ",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = if (themeKey == "puppy") Color.Black else themeConfig.textSecondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bodySuggestions.forEach { (label, insertionText) ->
                        AutocompleteChip(
                            text = label,
                            onClick = { insertTextAtCursor(insertionText) },
                            themeConfig = themeConfig,
                            themeKey = themeKey
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (previewMode != "preview") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔧 Tools:",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (themeKey == "puppy") Color.Black else themeConfig.textSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RichFormattingButton(icon = Icons.Default.FormatBold, textLabel = "B", tooltip = "Bold", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("bold")
                    }
                    RichFormattingButton(icon = Icons.Default.FormatItalic, textLabel = "I", tooltip = "Italic", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("italic")
                    }
                    RichFormattingButton(icon = Icons.Default.Title, textLabel = "H1", tooltip = "H1", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("h1")
                    }
                    RichFormattingButton(icon = Icons.Default.Title, textLabel = "H2", tooltip = "H2", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("h2")
                    }
                    RichFormattingButton(icon = Icons.Default.Title, textLabel = "H3", tooltip = "H3", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("h3")
                    }
                    RichFormattingButton(icon = Icons.Default.Code, textLabel = "Inline", tooltip = "Inline Code", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("code_inline")
                    }
                    RichFormattingButton(icon = Icons.Default.Code, textLabel = "Block", tooltip = "Code Block", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("code_block")
                    }
                    RichFormattingButton(icon = Icons.Default.TaskAlt, textLabel = "Todo", tooltip = "Todo Task", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("list_todo")
                    }
                    RichFormattingButton(icon = Icons.AutoMirrored.Filled.FormatListBulleted, textLabel = "Bullet", tooltip = "Bullet List", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("list_bullet")
                    }
                    RichFormattingButton(icon = Icons.Default.TableChart, textLabel = "Table", tooltip = "Markdown Table", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("table")
                    }
                    RichFormattingButton(icon = Icons.Default.Info, textLabel = "Info Box", tooltip = "Info Callout", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("callout_info")
                    }
                    RichFormattingButton(icon = Icons.Default.Warning, textLabel = "Warning Box", tooltip = "Warning Callout", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("callout_warning")
                    }
                    RichFormattingButton(icon = Icons.Default.Link, textLabel = "Link", tooltip = "Web Link", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("link")
                    }
                    RichFormattingButton(icon = Icons.Default.HorizontalRule, textLabel = "Divider", tooltip = "Divider", themeConfig = themeConfig, themeKey = themeKey) {
                        applyFormatting("divider")
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Responsive Body Layout
        when (previewMode) {
            "preview" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, themeConfig.border, RoundedCornerShape(4.dp))
                        .background(themeConfig.bgPrimary.copy(alpha = 0.15f))
                ) {
                    MarkdownRenderer(
                        markdown = contentValue.text,
                        themeConfig = themeConfig,
                        modifier = Modifier.fillMaxSize(),
                        onToggleCheckbox = { toggleLineCheckbox(it) }
                    )
                }
            }
            "split" -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val isWide = maxWidth >= 600.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left Pane (Edit)
                            OutlinedTextField(
                                value = contentValue,
                                onValueChange = {
                                    if (it.text.length <= 20000) {
                                        updateContent(it)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "Write your documentation markdown details, ideas or server configurations here...",
                                        fontFamily = themeConfig.fontFamily,
                                        fontSize = 14.sp,
                                        color = themeConfig.textSecondary.copy(alpha = 0.4f)
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = themeConfig.fontFamily,
                                    fontSize = 14.sp,
                                    color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .onFocusChanged { isBodyFocused = it.isFocused }
                                    .testTag("note_editor_body"),
                                visualTransformation = DeveloperSyntaxHighlighter(themeConfig),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = themeConfig.bgPrimary.copy(alpha = 0.4f),
                                    unfocusedContainerColor = themeConfig.bgPrimary.copy(alpha = 0.15f),
                                    focusedBorderColor = themeConfig.accent,
                                    unfocusedBorderColor = themeConfig.border,
                                    focusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary,
                                    unfocusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                                )
                            )

                            // Right Pane (Preview)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(1.dp, themeConfig.border, RoundedCornerShape(4.dp))
                                    .background(themeConfig.bgPrimary.copy(alpha = 0.15f))
                            ) {
                                MarkdownRenderer(
                                    markdown = contentValue.text,
                                    themeConfig = themeConfig,
                                    modifier = Modifier.fillMaxSize(),
                                    onToggleCheckbox = { toggleLineCheckbox(it) }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Pane (Edit)
                            OutlinedTextField(
                                value = contentValue,
                                onValueChange = {
                                    if (it.text.length <= 20000) {
                                        updateContent(it)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "Write your documentation markdown details, ideas or server configurations here...",
                                        fontFamily = themeConfig.fontFamily,
                                        fontSize = 14.sp,
                                        color = themeConfig.textSecondary.copy(alpha = 0.4f)
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = themeConfig.fontFamily,
                                    fontSize = 14.sp,
                                    color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .onFocusChanged { isBodyFocused = it.isFocused }
                                    .testTag("note_editor_body"),
                                visualTransformation = DeveloperSyntaxHighlighter(themeConfig),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = themeConfig.bgPrimary.copy(alpha = 0.4f),
                                    unfocusedContainerColor = themeConfig.bgPrimary.copy(alpha = 0.15f),
                                    focusedBorderColor = themeConfig.accent,
                                    unfocusedBorderColor = themeConfig.border,
                                    focusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary,
                                    unfocusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                                )
                            )

                            // Bottom Pane (Preview)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .border(1.dp, themeConfig.border, RoundedCornerShape(4.dp))
                                    .background(themeConfig.bgPrimary.copy(alpha = 0.15f))
                            ) {
                                MarkdownRenderer(
                                    markdown = contentValue.text,
                                    themeConfig = themeConfig,
                                    modifier = Modifier.fillMaxSize(),
                                    onToggleCheckbox = { toggleLineCheckbox(it) }
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Document Body editor
                OutlinedTextField(
                    value = contentValue,
                    onValueChange = {
                        if (it.text.length <= 20000) {
                            updateContent(it)
                        }
                    },
                    placeholder = {
                        Text(
                            "Write your documentation markdown details, ideas or server configurations here...",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 14.sp,
                            color = themeConfig.textSecondary.copy(alpha = 0.4f)
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 14.sp,
                        color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onFocusChanged { isBodyFocused = it.isFocused }
                        .testTag("note_editor_body"),
                    visualTransformation = DeveloperSyntaxHighlighter(themeConfig),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = themeConfig.bgPrimary.copy(alpha = 0.4f),
                        unfocusedContainerColor = themeConfig.bgPrimary.copy(alpha = 0.15f),
                        focusedBorderColor = themeConfig.accent,
                        unfocusedBorderColor = themeConfig.border,
                        focusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary,
                        unfocusedTextColor = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time statistics panel in note footer
        RealtimeStatsPanel(
            wordCount = wordCount,
            charCount = charCount,
            paragraphCount = paragraphCount,
            readingTimeMin = readingTimeMin,
            themeConfig = themeConfig,
            themeKey = themeKey,
            modifier = Modifier.testTag("realtime_stats_panel")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Note timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Created: ${note.created}",
                fontFamily = themeConfig.fontFamily,
                fontSize = 10.sp,
                color = themeConfig.textSecondary.copy(alpha = 0.45f)
            )
            Text(
                text = "Modified: ${note.updated}",
                fontFamily = themeConfig.fontFamily,
                fontSize = 10.sp,
                color = themeConfig.textSecondary.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun CodeBlockWidget(
    code: String,
    language: String,
    themeConfig: NotesThemeConfig,
    context: Context
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeConfig.border, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = themeConfig.bgSecondary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeConfig.bgSecondary.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(Locale.getDefault()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeConfig.accent
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied CodeSnippet", code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "$language code copied!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        .testTag("copy_code_block_btn")
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = themeConfig.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Copy",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 10.sp,
                        color = themeConfig.textSecondary
                    )
                }
            }
            HorizontalDivider(color = themeConfig.border.copy(alpha = 0.5f), thickness = 1.dp)
            // Code Text Body
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = themeConfig.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun TableWidget(headers: List<String>, rows: List<List<String>>, themeConfig: NotesThemeConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeConfig.border, RoundedCornerShape(8.dp))
            .background(themeConfig.bgSecondary.copy(alpha = 0.2f))
            .padding(1.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeConfig.bgSecondary.copy(alpha = 0.6f))
                .padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            headers.forEach { header ->
                Text(
                    text = parseInlineMarkdown(header, themeConfig),
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeConfig.accent,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
            }
        }
        HorizontalDivider(color = themeConfig.border.copy(alpha = 0.5f), thickness = 1.dp)
        // Data Rows
        rows.forEach { rowCells ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                headers.indices.forEach { colIdx ->
                    val cellText = if (colIdx < rowCells.size) rowCells[colIdx] else ""
                    Text(
                        text = parseInlineMarkdown(cellText, themeConfig),
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 12.sp,
                        color = themeConfig.textPrimary,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = themeConfig.border.copy(alpha = 0.2f), thickness = 0.5.dp)
        }
    }
}

@Composable
fun CalloutBoxWidget(type: String, body: String, themeConfig: NotesThemeConfig) {
    val (bgColor, borderColor, icon, titleLabel) = when (type) {
        "INFO" -> Quadruple(Color(0xFF0288D1).copy(alpha = 0.08f), Color(0xFF0288D1), Icons.Default.Info, "INFO")
        "WARNING" -> Quadruple(Color(0xFFF57C00).copy(alpha = 0.08f), Color(0xFFF57C00), Icons.Default.Warning, "WARNING")
        "SUCCESS" -> Quadruple(Color(0xFF388E3C).copy(alpha = 0.08f), Color(0xFF388E3C), Icons.Default.CheckCircle, "SUCCESS")
        "ERROR" -> Quadruple(Color(0xFFD32F2F).copy(alpha = 0.08f), Color(0xFFD32F2F), Icons.Default.Error, "ERROR")
        else -> Quadruple(themeConfig.accent.copy(alpha = 0.08f), themeConfig.accent, Icons.Default.Lightbulb, "NOTE")
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
            .drawBehind {
                val strokeWidth = 5.dp.toPx()
                drawLine(
                    color = borderColor,
                    start = Offset(strokeWidth / 2, 0f),
                    end = Offset(strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .background(bgColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = titleLabel,
            tint = borderColor,
            modifier = Modifier.size(16.dp).padding(top = 1.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleLabel,
                fontFamily = themeConfig.fontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = borderColor
            )
            if (body.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = parseInlineMarkdown(body.trim(), themeConfig),
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 12.sp,
                    color = themeConfig.textPrimary
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun MarkdownRenderer(
    markdown: String,
    themeConfig: NotesThemeConfig,
    modifier: Modifier = Modifier,
    onToggleCheckbox: ((lineIndex: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(themeConfig.bgPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = markdown.replace("\r\n", "\n").split("\n")
        var inCodeBlock = false
        val currentCodeBlock = java.lang.StringBuilder()
        var currentLanguage = ""

        var idx = 0
        while (idx < lines.size) {
            val rawLine = lines[idx]
            val line = rawLine.trim()

            // Code Blocks Parser
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    val blockText = currentCodeBlock.toString().trim()
                    val displayLanguage = currentLanguage.ifEmpty { "code" }
                    item {
                        CodeBlockWidget(
                            code = blockText,
                            language = displayLanguage,
                            themeConfig = themeConfig,
                            context = context
                        )
                    }
                    currentCodeBlock.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    currentLanguage = line.removePrefix("```").trim()
                }
                idx++
                continue
            }

            if (inCodeBlock) {
                currentCodeBlock.append(rawLine).append("\n")
                idx++
                continue
            }

            // Tables Parser
            if (line.startsWith("|") && line.indexOf('|', startIndex = 1) != -1) {
                val tableLines = mutableListOf<String>()
                var tableIdx = idx
                while (tableIdx < lines.size && lines[tableIdx].trim().startsWith("|")) {
                    tableLines.add(lines[tableIdx].trim())
                    tableIdx++
                }
                if (tableLines.size >= 2) {
                    val headers = tableLines[0].split("|").map { it.trim() }.filterIndexed { index, _ -> index > 0 && index < tableLines[0].split("|").lastIndex }
                    val separator = tableLines[1]
                    val hasSeparator = separator.contains("-")
                    
                    val rows = mutableListOf<List<String>>()
                    val startRowIdx = if (hasSeparator) 2 else 1
                    for (r in startRowIdx until tableLines.size) {
                        val cells = tableLines[r].split("|").map { it.trim() }.filterIndexed { index, _ -> index > 0 && index < tableLines[r].split("|").lastIndex }
                        rows.add(cells)
                    }
                    item {
                        TableWidget(
                            headers = headers,
                            rows = rows,
                            themeConfig = themeConfig
                        )
                    }
                    idx = tableIdx
                    continue
                }
            }

            // Admonitions / Callout Blockquote Parser
            if (line.startsWith(">")) {
                val content = line.drop(1).trim()
                val isCallout = content.startsWith("[!INFO]") || content.startsWith("[!WARNING]") || 
                                content.startsWith("[!SUCCESS]") || content.startsWith("[!ERROR]") || content.startsWith("[!NOTE]")
                if (isCallout) {
                    val calloutLines = mutableListOf<String>()
                    var callIdx = idx
                    while (callIdx < lines.size && lines[callIdx].trim().startsWith(">")) {
                        calloutLines.add(lines[callIdx].trim().drop(1).trim())
                        callIdx++
                    }
                    val typeHeader = calloutLines.firstOrNull() ?: ""
                    val type = when {
                        typeHeader.startsWith("[!INFO]") -> "INFO"
                        typeHeader.startsWith("[!WARNING]") -> "WARNING"
                        typeHeader.startsWith("[!SUCCESS]") -> "SUCCESS"
                        typeHeader.startsWith("[!ERROR]") -> "ERROR"
                        else -> "NOTE"
                    }
                    val calloutBody = calloutLines.drop(1).joinToString("\n")
                    item {
                        CalloutBoxWidget(
                            type = type,
                            body = calloutBody,
                            themeConfig = themeConfig
                        )
                    }
                    idx = callIdx
                    continue
                }
            }

            // Headers Parser
            if (line.startsWith("#")) {
                val depth = line.takeWhile { it == '#' }.length
                val content = line.drop(depth).trim()
                if (depth in 1..6) {
                    val fontSize = when (depth) {
                        1 -> 22.sp
                        2 -> 18.sp
                        3 -> 16.sp
                        else -> 14.sp
                    }
                    val fontWeight = FontWeight.Bold
                    item {
                        Text(
                            text = parseInlineMarkdown(content, themeConfig),
                            fontFamily = themeConfig.fontFamily,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = themeConfig.accent,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    idx++
                    continue
                }
            }

            // Bullet Lists Parser
            if (line.startsWith("* ") || line.startsWith("- ") || line.startsWith("+ ")) {
                val isChecklist = line.startsWith("- [ ]") || line.startsWith("- [x]") || line.startsWith("* [ ]") || line.startsWith("* [x]")
                if (isChecklist) {
                    val isChecked = line.contains("[x]")
                    val content = line.substring(5).trim()
                    val currentLineIndex = idx
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(
                                        width = 1.5.dp, 
                                        color = if (isChecked) themeConfig.accent else themeConfig.textSecondary.copy(alpha = 0.5f), 
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = if (isChecked) themeConfig.accent.copy(alpha = 0.1f) else Color.Transparent, 
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        onToggleCheckbox?.invoke(currentLineIndex)
                                    }
                                    .testTag("markdown_checkbox_$currentLineIndex"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked",
                                        tint = themeConfig.accent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(content, themeConfig),
                                fontFamily = themeConfig.fontFamily,
                                fontSize = 13.sp,
                                color = if (isChecked) themeConfig.textSecondary.copy(alpha = 0.7f) else themeConfig.textPrimary,
                                style = TextStyle(
                                    textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            )
                        }
                    }
                } else {
                    val content = line.drop(2).trim()
                    item {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "•",
                                fontSize = 14.sp,
                                color = themeConfig.accent,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = parseInlineMarkdown(content, themeConfig),
                                fontFamily = themeConfig.fontFamily,
                                fontSize = 13.sp,
                                color = themeConfig.textPrimary
                            )
                        }
                    }
                }
                idx++
                continue
            }

            // Normal Blockquotes Block Parser
            if (line.startsWith(">")) {
                val content = line.drop(1).trim()
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(IntrinsicSize.Min)
                                .background(themeConfig.accent)
                        ) {
                            Spacer(modifier = Modifier.fillMaxHeight())
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = parseInlineMarkdown(content, themeConfig),
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = themeConfig.textSecondary
                        )
                    }
                }
                idx++
                continue
            }

            // Horizontal Partition Rule Line Parser
            if (line == "---" || line == "***" || line == "___") {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = themeConfig.border
                    )
                }
                idx++
                continue
            }

            // Numbered List Parser
            val numPrefix = line.takeWhile { it.isDigit() }
            if (numPrefix.isNotEmpty() && line.drop(numPrefix.length).startsWith(".")) {
                val content = line.drop(numPrefix.length + 1).trim()
                item {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "$numPrefix.",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeConfig.accent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(content, themeConfig),
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 13.sp,
                            color = themeConfig.textPrimary
                        )
                    }
                }
                idx++
                continue
            }

            // Single general line / paragraph
            if (line.isNotEmpty()) {
                item {
                    Text(
                        text = parseInlineMarkdown(line, themeConfig),
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 13.sp,
                        color = themeConfig.textPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            idx++
        }
    }
}

fun parseInlineMarkdown(text: String, themeConfig: NotesThemeConfig): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    var index = 0
    while (index < text.length) {
        // Double Asterisk (Bold)
        if (text.startsWith("**", index)) {
            val nextDouble = text.indexOf("**", index + 2)
            if (nextDouble != -1) {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(text.substring(index + 2, nextDouble))
                builder.pop()
                index = nextDouble + 2
                continue
            }
        }
        
        // Single Asterisk (Italic)
        if (text.startsWith("*", index)) {
            val nextSingle = text.indexOf("*", index + 1)
            if (nextSingle != -1) {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                builder.append(text.substring(index + 1, nextSingle))
                builder.pop()
                index = nextSingle + 1
                continue
            }
        }

        // Inline code (Backticks)
        if (text.startsWith("`", index)) {
            val nextBacktick = text.indexOf("`", index + 1)
            if (nextBacktick != -1) {
                builder.pushStyle(
                    androidx.compose.ui.text.SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = themeConfig.bgSecondary.copy(alpha = 0.5f),
                        color = themeConfig.accent,
                        fontSize = 12.sp
                    )
                )
                builder.append(text.substring(index + 1, nextBacktick))
                builder.pop()
                index = nextBacktick + 1
                continue
            }
        }

        // Markdown Link: [text](url)
        if (text.startsWith("[", index)) {
            val endBracket = text.indexOf("]", index)
            if (endBracket != -1 && text.startsWith("(", endBracket + 1)) {
                val endParenthesis = text.indexOf(")", endBracket + 2)
                if (endParenthesis != -1) {
                    val linkText = text.substring(index + 1, endBracket)
                    val url = text.substring(endBracket + 2, endParenthesis)
                    builder.pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = themeConfig.accent,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    builder.append(linkText)
                    builder.pop()
                    index = endParenthesis + 1
                    continue
                }
            }
        }

        builder.append(text[index].toString())
        index++
    }
    return builder.toAnnotatedString()
}

@Composable
fun EmptyEditorState(themeConfig: NotesThemeConfig) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeConfig.bgSecondary.copy(alpha = 0.4f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Selection empty details",
                tint = themeConfig.textSecondary.copy(alpha = 0.25f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select a Note from your list to analyze.",
                fontFamily = themeConfig.fontFamily,
                fontSize = 14.sp,
                color = themeConfig.textSecondary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dynamic edits and offline persistent cache saves automatically.",
                fontFamily = themeConfig.fontFamily,
                fontSize = 11.sp,
                color = themeConfig.textSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

// ==========================================
// POPUP DIALOG FLOATING MODALS
// ==========================================

@Composable
fun GitHubSyncDialog(
    viewModel: MainViewModel,
    themeConfig: NotesThemeConfig,
    isSyncing: StateFlow<Boolean>,
    ghStatus: StateFlow<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val token by viewModel.ghTokenState.collectAsStateWithLifecycle()
    val repo by viewModel.ghRepoState.collectAsStateWithLifecycle()
    val path by viewModel.ghPathState.collectAsStateWithLifecycle()

    val activeSyncState by isSyncing.collectAsStateWithLifecycle()
    val statusText by ghStatus.collectAsStateWithLifecycle()

    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val testConnectionResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()

    var mutableToken by remember { mutableStateOf(token) }
    var mutableRepo by remember { mutableStateOf(repo) }
    var mutablePath by remember { mutableStateOf(path) }

    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeConfig.bgSecondary),
            border = BorderStroke(2.dp, themeConfig.accent)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud Synchronize Icon",
                            tint = themeConfig.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GitHub Synced Vault",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeConfig.accent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close github settings popup", tint = themeConfig.textPrimary)
                    }
                }

                // Friendly Tip & Documentation block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeConfig.bgPrimary.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, themeConfig.border.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "How to generate a Personal Access Token (PAT):",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeConfig.accent
                        )
                        Text(
                            text = "1. Go to GitHub Settings > Developer Settings > Personal Access Tokens (Classic).\n2. Generate a token with 'repo' scope authorized.\n3. Repository must exist and be formatted as 'username/repository-name'.",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 10.sp,
                            color = themeConfig.textSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }

                // 1. Token Input Fields with Visibility Toggles and Icons
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Authentication",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textPrimary
                    )
                    OutlinedTextField(
                        value = mutableToken,
                        onValueChange = { mutableToken = it },
                        placeholder = { Text("Paste ghp_... token here", fontFamily = themeConfig.fontFamily, fontSize = 12.sp) },
                        label = { Text("Personal Access Token (PAT)", fontFamily = themeConfig.fontFamily, fontSize = 11.sp) },
                        textStyle = TextStyle(fontFamily = themeConfig.fontFamily, fontSize = 13.sp, color = themeConfig.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gh_token_input"),
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Token Key Icon",
                                tint = themeConfig.accent
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = themeConfig.textSecondary
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeConfig.accent,
                            unfocusedBorderColor = themeConfig.border,
                            focusedLabelColor = themeConfig.accent,
                            unfocusedLabelColor = themeConfig.textSecondary
                        )
                    )
                }

                // 2. Repository Configuration Fields
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Repository Setup",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textPrimary
                    )
                    OutlinedTextField(
                        value = mutableRepo,
                        onValueChange = { mutableRepo = it },
                        label = { Text("Repository (owner/repo-name)", fontFamily = themeConfig.fontFamily, fontSize = 11.sp) },
                        placeholder = { Text("e.g. roger/developer-notes", fontFamily = themeConfig.fontFamily, fontSize = 12.sp) },
                        textStyle = TextStyle(fontFamily = themeConfig.fontFamily, fontSize = 13.sp, color = themeConfig.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gh_repo_input"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Repository Icon",
                                tint = themeConfig.accent
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeConfig.accent,
                            unfocusedBorderColor = themeConfig.border,
                            focusedLabelColor = themeConfig.accent,
                            unfocusedLabelColor = themeConfig.textSecondary
                        )
                    )

                    // Automated formatting validation
                    if (mutableRepo.isNotEmpty() && !mutableRepo.contains("/")) {
                        Text(
                            text = "⚠️ Format must be 'owner/repository-name' (missing a slash)",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 3. File Path Inside Remote Repository
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Synced Target File",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textPrimary
                    )
                    OutlinedTextField(
                        value = mutablePath,
                        onValueChange = { mutablePath = it },
                        label = { Text("File Path in Repo", fontFamily = themeConfig.fontFamily, fontSize = 11.sp) },
                        placeholder = { Text("e.g. notes.json or data/nodes-backup.json", fontFamily = themeConfig.fontFamily, fontSize = 12.sp) },
                        textStyle = TextStyle(fontFamily = themeConfig.fontFamily, fontSize = 13.sp, color = themeConfig.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gh_path_input"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Document Path Icon",
                                tint = themeConfig.accent
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeConfig.accent,
                            unfocusedBorderColor = themeConfig.border,
                            focusedLabelColor = themeConfig.accent,
                            unfocusedLabelColor = themeConfig.textSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions Area Header
                Text(
                    text = "Sync & Verification Controls",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeConfig.textPrimary
                )

                // 4. Save and Test Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Test Connection Button
                    Button(
                        onClick = {
                            viewModel.testGitHubConnection(mutableToken, mutableRepo, mutablePath)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gh_test_connection_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = themeConfig.bgPrimary),
                        border = BorderStroke(1.dp, themeConfig.accent),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isTestingConnection
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(color = themeConfig.accent, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = "Test connection state", modifier = Modifier.size(16.dp), tint = themeConfig.accent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Conn", fontFamily = themeConfig.fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeConfig.accent)
                        }
                    }

                    // Save Local Settings trigger
                    Button(
                        onClick = {
                            viewModel.saveGitHubSettings(
                                token = mutableToken,
                                repo = mutableRepo,
                                path = mutablePath
                            )
                            Toast.makeText(context, "GitHub credentials saved locally!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gh_save_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = themeConfig.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save settings", modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Save",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 11.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 5. Connection Test Result Display Panel
                testConnectionResult?.let { testResult ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (testResult.contains("Connected!")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp, 
                                if (testResult.contains("Connected!")) Color(0xFF2E7D32) else Color(0xFFC62828), 
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (testResult.contains("Connected!")) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = "Verification status result icon",
                                tint = if (testResult.contains("Connected!")) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testResult,
                                fontFamily = themeConfig.fontFamily,
                                fontSize = 11.sp,
                                color = if (testResult.contains("Connected!")) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = themeConfig.border.copy(alpha = 0.4f))

                // 6. Push & Pull Main Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.pushToGitHub() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gh_push_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3465A4)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !activeSyncState
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = "Push database notes up to GitHub", modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push Cloud", fontFamily = themeConfig.fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.pullFromGitHub() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gh_pull_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E9A06)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !activeSyncState
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Pull remote content down into local database", modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull Cloud", fontFamily = themeConfig.fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // 7. Push/Pull Status message pane
                if (statusText.isNotEmpty() || activeSyncState) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeConfig.bgPrimary.copy(alpha = 0.4f), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, themeConfig.border.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (activeSyncState) {
                                CircularProgressIndicator(color = themeConfig.accent, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = statusText,
                                fontFamily = themeConfig.fontFamily,
                                fontSize = 11.sp,
                                color = themeConfig.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageTagsDialog(
    viewModel: MainViewModel,
    themeConfig: NotesThemeConfig,
    onDismiss: () -> Unit
) {
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    var editingTag by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeConfig.bgSecondary),
            border = BorderStroke(2.dp, themeConfig.accent)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxHeight(0.7f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage Tags Index",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.accent
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close tag management settings dialog")
                    }
                }

                Text(
                    text = "Rename or purge search filter tags globally. Changes directly updates all associated note documents.",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = themeConfig.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (tags.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "No tags currently present in local database records.",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 11.sp,
                            color = themeConfig.textSecondary.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manage_tags_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(themeConfig.bgPrimary)
                                    .border(1.dp, themeConfig.border)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (editingTag == tag) {
                                    OutlinedTextField(
                                        value = renameValue,
                                        onValueChange = { renameValue = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = TextStyle(fontFamily = themeConfig.fontFamily, fontSize = 13.sp, color = themeConfig.textPrimary)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.renameTagInAllNotes(tag, renameValue)
                                            editingTag = null
                                        }
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = "Validate tag renaming", tint = Color(0xFF4E9A06))
                                    }
                                    IconButton(onClick = { editingTag = null }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Cancel tag renaming", tint = Color(0xFFC41E3A))
                                    }
                                } else {
                                    Text(
                                        text = "#$tag",
                                        fontFamily = themeConfig.fontFamily,
                                        fontSize = 13.sp,
                                        color = themeConfig.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingTag = tag
                                                renameValue = tag
                                            }
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit tag entry value", tint = themeConfig.textSecondary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteTagFromAllNotes(tag)
                                            }
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Purge tag globally", tint = Color(0xFFC41E3A), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupDialog(
    viewModel: MainViewModel,
    themeConfig: NotesThemeConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputJsonText by remember { mutableStateOf("") }
    val backupString = remember { viewModel.getExportBackupJson() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeConfig.bgSecondary),
            border = BorderStroke(2.dp, themeConfig.accent)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup & Recovery utilities",
                        fontFamily = themeConfig.fontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.accent
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close local backup utility dialog")
                    }
                }

                // EXPORT SEGMENT
                Text(
                    text = "EXPORT BACKUP JSON",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = themeConfig.accent,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Use this auto-generated payload representation to backup your local developer logs to raw files or secondary devices.",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = themeConfig.textSecondary
                )

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("note_taking_roger_backup", backupString)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup JSON string copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_export_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = themeConfig.buttonBg),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, themeConfig.accent)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Copy JSON", tint = themeConfig.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Backup Payload to Clipboard", fontFamily = themeConfig.fontFamily, fontSize = 11.sp, color = themeConfig.textPrimary)
                }

                HorizontalDivider(color = themeConfig.border)

                // IMPORT SEGMENT
                Text(
                    text = "IMPORT BACKUP JSON",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = themeConfig.accent,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Paste a valid JSON backup list matching the schema models below to reconstruct or merge logs into this client instantly.",
                    fontFamily = themeConfig.fontFamily,
                    fontSize = 11.sp,
                    color = themeConfig.textSecondary
                )

                OutlinedTextField(
                    value = inputJsonText,
                    onValueChange = { inputJsonText = it },
                    placeholder = {
                        Text(
                            "[ { \"id\": ... } ]",
                            fontFamily = themeConfig.fontFamily,
                            fontSize = 12.sp,
                            color = themeConfig.textSecondary.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = TextStyle(fontFamily = themeConfig.fontFamily, fontSize = 11.sp, color = themeConfig.textPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("backup_import_textarea"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = themeConfig.bgPrimary,
                        unfocusedContainerColor = themeConfig.bgPrimary,
                        focusedBorderColor = themeConfig.accent,
                        unfocusedBorderColor = themeConfig.border
                    )
                )

                Button(
                    onClick = {
                        if (inputJsonText.trim().isEmpty()) {
                            Toast.makeText(context, "Input string is empty!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val success = viewModel.importBackup(inputJsonText)
                        if (success) {
                            Toast.makeText(context, "Backup JSON imported successfully!", Toast.LENGTH_LONG).show()
                            inputJsonText = ""
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to parse JSON. Validate parameters.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_import_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = themeConfig.accent),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Upload imported items directly to Room", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Execute Import Package", fontFamily = themeConfig.fontFamily, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RealtimeStatsPanel(
    wordCount: Int,
    charCount: Int,
    paragraphCount: Int,
    readingTimeMin: Int,
    themeConfig: NotesThemeConfig,
    themeKey: String,
    modifier: Modifier = Modifier
) {
    val readingTimeStr = if (readingTimeMin <= 1) {
        if (wordCount == 0) "0 min read" else "< 1 min read"
    } else {
        "$readingTimeMin min read"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(themeConfig.bgSecondary.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, themeConfig.border.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            icon = Icons.Default.Description,
            value = "$wordCount",
            label = "words",
            themeConfig = themeConfig,
            themeKey = themeKey
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(themeConfig.border.copy(alpha = 0.4f))
        )
        StatItem(
            icon = Icons.Default.TextFields,
            value = "$charCount",
            label = "chars",
            themeConfig = themeConfig,
            themeKey = themeKey
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(themeConfig.border.copy(alpha = 0.4f))
        )
        StatItem(
            icon = Icons.AutoMirrored.Filled.Subject,
            value = "$paragraphCount",
            label = "paragraphs",
            themeConfig = themeConfig,
            themeKey = themeKey
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(themeConfig.border.copy(alpha = 0.4f))
        )
        StatItem(
            icon = Icons.Default.AccessTime,
            value = readingTimeStr,
            label = "",
            themeConfig = themeConfig,
            themeKey = themeKey
        )
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    themeConfig: NotesThemeConfig,
    themeKey: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = themeConfig.accent,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = buildString {
                append(value)
                if (label.isNotEmpty()) {
                    append(" ")
                    append(label)
                }
            },
            fontFamily = themeConfig.fontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (themeKey == "puppy") Color.Black else themeConfig.textPrimary
        )
    }
}

fun printNoteToPdf(
    context: Context,
    note: Note,
    title: String,
    content: String,
    themeConfig: NotesThemeConfig
) {
    val wordCount = content.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    val charCount = content.length

    val themeAccentColorHex = try {
        val argb = themeConfig.accent.toArgb()
        String.format("#%06X", 0xFFFFFF and argb)
    } catch (e: Exception) {
        "#005CBB"
    }

    val bodyHtml = convertMarkdownToHtmlForPdf(content, themeAccentColorHex)

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <title>${title}</title>
        <style>
            body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                color: #1a1a1a;
                background-color: #ffffff;
                margin: 0;
                padding: 30px;
                line-height: 1.6;
                font-size: 14px;
            }
            .header {
                border-bottom: 2px solid $themeAccentColorHex;
                padding-bottom: 12px;
                margin-bottom: 24px;
            }
            .title {
                font-size: 28px;
                font-weight: 700;
                margin: 0 0 8px 0;
                color: #111111;
            }
            .meta {
                font-size: 11px;
                color: #666666;
                margin-top: 4px;
            }
            .meta-item {
                display: inline-block;
                margin-right: 15px;
            }
            .tag {
                background-color: #f0f0f0;
                border: 1px solid #e0e0e0;
                border-radius: 4px;
                padding: 2px 6px;
                font-family: monospace;
                color: $themeAccentColorHex;
                font-size: 11px;
                margin-right: 4px;
            }
            .content {
                margin-top: 20px;
            }
            pre {
                background-color: #f8f9fa;
                border: 1px solid #e2e8f0;
                border-radius: 6px;
                padding: 12px;
                overflow-x: auto;
                font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
                font-size: 13px;
                line-height: 1.5;
                margin: 16px 0;
            }
            code {
                font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
                background-color: #f1f3f5;
                padding: 2px 4px;
                border-radius: 4px;
                font-size: 0.9em;
                color: #d63384;
            }
            pre code {
                background-color: transparent;
                padding: 0;
                border-radius: 0;
                color: inherit;
                font-size: inherit;
            }
            h1 {
                font-size: 22px;
                border-bottom: 1px solid #e9ecef;
                padding-bottom: 6px;
                margin-top: 24px;
                margin-bottom: 12px;
                color: #222222;
            }
            h2 {
                font-size: 18px;
                margin-top: 20px;
                margin-bottom: 10px;
                color: #333333;
            }
            h3 {
                font-size: 15px;
                margin-top: 16px;
                margin-bottom: 8px;
                color: #444444;
            }
            p {
                margin: 0 0 12px 0;
            }
            blockquote {
                border-left: 4px solid $themeAccentColorHex;
                margin: 16px 0;
                padding: 8px 16px;
                background-color: #f8f9fa;
                color: #495057;
                font-style: italic;
            }
            ul, ol {
                margin: 8px 0 16px 0;
                padding-left: 24px;
            }
            li {
                margin-bottom: 4px;
            }
            @media print {
                body {
                    padding: 0;
                }
                @page {
                    margin: 1.5cm;
                }
            }
        </style>
        </head>
        <body>
            <div class="header">
                <h1 class="title">${title}</h1>
                <div class="meta">
                    <span class="meta-item"><strong>Date:</strong> ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.timestamp))}</span>
                    <span class="meta-item"><strong>Words:</strong> $wordCount</span>
                    <span class="meta-item"><strong>Chars:</strong> $charCount</span>
                    ${if (note.tags.isNotEmpty()) """<span class="meta-item"><strong>Tags:</strong> ${note.tags.joinToString("") { "<span class=\"tag\">#$it</span>" }}</span>""" else ""}
                </div>
            </div>
            <div class="content">
                $bodyHtml
            </div>
        </body>
        </html>
    """.trimIndent()

    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "Note_${title.replace(Regex("[^a-zA-Z0-9]"), "_")}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(
                        jobName,
                        printAdapter,
                        android.print.PrintAttributes.Builder().build()
                    )
                } else {
                    Toast.makeText(context, "Printing is not supported on this device", Toast.LENGTH_SHORT).show()
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to print note: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun convertMarkdownToHtmlForPdf(content: String, themeAccentColorHex: String): String {
    val blockCodeRegex = Regex("(?s)```(.*?)```")
    val blockCodeMatches = blockCodeRegex.findAll(content).toList()
    val blockCodePlaceholders = ArrayList<String>()
    var textWithBlockPlaceholders = content

    for ((index, match) in blockCodeMatches.withIndex()) {
        val rawBlockCode = match.value
        val blockContent = match.groupValues[1]
        val firstLineEndIdx = blockContent.indexOf('\n')
        val finalCode = if (firstLineEndIdx != -1) {
            blockContent.substring(firstLineEndIdx + 1)
        } else {
            blockContent
        }

        val highlightedCode = highlightCodeSyntaxForPdf(finalCode)
        val placeholder = "%%BLOCK_CODE_PLACEHOLDER_${index}%%"
        blockCodePlaceholders.add("<pre><code>$highlightedCode</code></pre>")
        textWithBlockPlaceholders = textWithBlockPlaceholders.replace(rawBlockCode, placeholder)
    }

    val backtickRegex = Regex("`([^`\\n]+)`")
    val backtickMatches = backtickRegex.findAll(textWithBlockPlaceholders).toList()
    val inlineCodePlaceholders = ArrayList<String>()
    var textWithAllPlaceholders = textWithBlockPlaceholders

    for ((index, match) in backtickMatches.withIndex()) {
        val rawInlineCode = match.value
        val inlineContent = match.groupValues[1]
        val escapedInline = inlineContent.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        
        val placeholder = "%%INLINE_CODE_PLACEHOLDER_${index}%%"
        inlineCodePlaceholders.add("<code>$escapedInline</code>")
        textWithAllPlaceholders = textWithAllPlaceholders.replace(rawInlineCode, placeholder)
    }

    var escapedText = textWithAllPlaceholders
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    val lines = escapedText.split("\n")
    val htmlLines = ArrayList<String>()
    var inList = false
    var inBlockquote = false

    for (line in lines) {
        var processedLine = line.trimEnd()
        
        if (processedLine.startsWith("&gt;") || processedLine.startsWith(">")) {
            val prefix = if (processedLine.startsWith("&gt;")) "&gt;" else ">"
            processedLine = processedLine.removePrefix(prefix).trim()
            if (!inBlockquote) {
                htmlLines.add("<blockquote style=\"border-left: 4px solid $themeAccentColorHex;\">")
                inBlockquote = true
            }
        } else {
            if (inBlockquote) {
                htmlLines.add("</blockquote>")
                inBlockquote = false
            }
        }
        
        val listRegex = Regex("^\\s*([*+-]|\\d+\\.)\\s+(.*)")
        val listMatch = listRegex.matchEntire(processedLine)
        if (listMatch != null) {
            val content = listMatch.groupValues[2]
            if (!inList) {
                htmlLines.add("<ul>")
                inList = true
            }
            processedLine = "<li>$content</li>"
        } else {
            if (inList) {
                htmlLines.add("</ul>")
                inList = false
            }
        }
        
        val headerRegex = Regex("^(#+)\\s+(.*)")
        val headerMatch = headerRegex.matchEntire(processedLine)
        if (headerMatch != null) {
            val hashes = headerMatch.groupValues[1]
            val headerText = headerMatch.groupValues[2]
            val level = hashes.length.coerceIn(1, 6)
            processedLine = "<h$level>$headerText</h$level>"
        } else if (listMatch == null && processedLine.trim().isNotEmpty()) {
            processedLine = "<p>$processedLine</p>"
        } else if (processedLine.trim().isEmpty() && !inList && !inBlockquote) {
            processedLine = "<br/>"
        }
        
        htmlLines.add(processedLine)
    }

    if (inBlockquote) htmlLines.add("</blockquote>")
    if (inList) htmlLines.add("</ul>")

    var combinedHtml = htmlLines.joinToString("\n")

    combinedHtml = combinedHtml.replace(Regex("\\*\\*([^\\n*]+?)\\*\\*|__([^\\n_]+?)__")) { matchResult ->
        val text = if (matchResult.groupValues[1].isNotEmpty()) matchResult.groupValues[1] else matchResult.groupValues[2]
        "<strong>$text</strong>"
    }
    combinedHtml = combinedHtml.replace(Regex("(?<!\\*)\\*([^\\n*]+?)\\*(?!\\*)|(?<!_)_([^\\n_]+?)_(?!_)")) { matchResult ->
        val text = if (matchResult.groupValues[1].isNotEmpty()) matchResult.groupValues[1] else matchResult.groupValues[2]
        "<em>$text</em>"
    }

    for (i in inlineCodePlaceholders.indices) {
        combinedHtml = combinedHtml.replace("%%INLINE_CODE_PLACEHOLDER_${i}%%", inlineCodePlaceholders[i])
    }
    for (i in blockCodePlaceholders.indices) {
        combinedHtml = combinedHtml.replace("%%BLOCK_CODE_PLACEHOLDER_${i}%%", blockCodePlaceholders[i])
    }

    return combinedHtml
}

fun highlightCodeSyntaxForPdf(code: String): String {
    var escaped = code
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    val commentsList = ArrayList<String>()
    val commentRegex = Regex("(//.*)|(/\\*.*?\\*/)", RegexOption.DOT_MATCHES_ALL)
    escaped = escaped.replace(commentRegex) { match ->
        val placeholder = "%%COMMENT_PLACEHOLDER_${commentsList.size}%%"
        commentsList.add("<span style=\"color: #008000; font-style: italic;\">${match.value}</span>")
        placeholder
    }

    val stringsList = ArrayList<String>()
    val stringRegex = Regex("(\"[^\"]*\")|('[^']*')")
    escaped = escaped.replace(stringRegex) { match ->
        val placeholder = "%%STRING_PLACEHOLDER_${stringsList.size}%%"
        stringsList.add("<span style=\"color: #a31515;\">${match.value}</span>")
        placeholder
    }

    val keywords = setOf(
        "class", "fun", "val", "var", "import", "package", "return", "if", "else", 
        "for", "while", "override", "private", "public", "protected", "internal", 
        "null", "true", "false", "this", "interface", "object", "const", "init",
        "try", "catch", "finally", "throw", "when", "is", "as", "in", "out", "by",
        "data", "sealed", "enum", "companion", "suspend", "constructor"
    )
    val keywordRegex = Regex("\\b(${keywords.joinToString("|")})\\b")
    escaped = escaped.replace(keywordRegex) { match ->
        "<span style=\"color: #0000ff; font-weight: bold;\">${match.value}</span>"
    }

    val types = setOf(
        "String", "Int", "Boolean", "Double", "Float", "Long", "Char", "Short", "Byte", "Any", "Unit",
        "List", "Map", "Set", "Modifier", "Composable", "ViewModel", "Note", "StateFlow", "MutableStateFlow",
        "Flow", "LiveData", "Context", "Toast", "Bundle", "Intent"
    )
    val typeRegex = Regex("\\b(${types.joinToString("|")})\\b")
    escaped = escaped.replace(typeRegex) { match ->
        "<span style=\"color: #267f99;\">${match.value}</span>"
    }

    val numberRegex = Regex("\\b(\\d+(\\.\\d+)?f?|0x[0-9a-fA-F]+)\\b")
    escaped = escaped.replace(numberRegex) { match ->
        "<span style=\"color: #098658;\">${match.value}</span>"
    }

    for (i in stringsList.indices) {
        escaped = escaped.replace("%%STRING_PLACEHOLDER_${i}%%", stringsList[i])
    }
    for (i in commentsList.indices) {
        escaped = escaped.replace("%%COMMENT_PLACEHOLDER_${i}%%", commentsList[i])
    }

    return escaped
}
