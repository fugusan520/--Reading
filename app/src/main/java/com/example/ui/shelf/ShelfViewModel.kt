package com.example.ui.shelf

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import com.example.data.model.FolderEntity
import com.example.data.repository.BookRepository
import com.example.ui.theme.ShelfThemePreset
import com.example.ui.theme.ShelfThemePresets
import com.example.util.FileImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShelfUiState(
    val currentTab: BookType = BookType.NOVEL,
    val currentFolderId: Long? = null,
    val currentFolderName: String? = null,
    val parentFolderId: Long? = null,
    val isGridView: Boolean = true,
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val themePreset: ShelfThemePreset = ShelfThemePresets.presets[0],
    val showNewFolderDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val selectedBookForMenu: BookEntity? = null,
    val selectedFolderForMenu: FolderEntity? = null,
    val showMoveBookDialog: Boolean = false,
    val folderDepth: Int = 0 // 0 = root, 1 = folder, 2 = subfolder
)

class ShelfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BookRepository = BookRepository(AppDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(
        ShelfUiState(
            themePreset = ShelfThemePresets.loadTheme(application)
        )
    )
    val uiState: StateFlow<ShelfUiState> = _uiState.asStateFlow()

    // Observe books dynamically based on current tab, folder, and search query
    val books: StateFlow<List<BookEntity>> = _uiState
        .flatMapLatest { state ->
            repository.getBooks(state.currentTab, state.currentFolderId)
        }
        .combine(_uiState) { bookList, state ->
            if (state.searchQuery.isBlank()) {
                bookList
            } else {
                bookList.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observe folders dynamically based on current tab and current folder
    val folders: StateFlow<List<FolderEntity>> = _uiState
        .flatMapLatest { state ->
            repository.getFolders(state.currentTab, state.currentFolderId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available folders for moving items (across root and subfolders)
    val allDestinationFolders: StateFlow<List<FolderEntity>> = _uiState
        .flatMapLatest { state ->
            repository.getFolders(state.currentTab, null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: BookType) {
        _uiState.value = _uiState.value.copy(
            currentTab = tab,
            currentFolderId = null,
            currentFolderName = null,
            parentFolderId = null,
            folderDepth = 0
        )
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun enterFolder(folder: FolderEntity) {
        val nextDepth = _uiState.value.folderDepth + 1
        _uiState.value = _uiState.value.copy(
            currentFolderId = folder.id,
            currentFolderName = folder.name,
            parentFolderId = folder.parentFolderId,
            folderDepth = nextDepth
        )
    }

    fun exitFolder() {
        val parentId = _uiState.value.parentFolderId
        val prevDepth = (_uiState.value.folderDepth - 1).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(
            currentFolderId = parentId,
            currentFolderName = if (parentId == null) null else _uiState.value.currentFolderName,
            parentFolderId = null,
            folderDepth = prevDepth
        )
    }

    fun openNewFolderDialog() {
        _uiState.value = _uiState.value.copy(showNewFolderDialog = true)
    }

    fun closeNewFolderDialog() {
        _uiState.value = _uiState.value.copy(showNewFolderDialog = false)
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        val state = _uiState.value
        // Only allow up to 2 levels (root is 0, level 1, level 2)
        if (state.folderDepth >= 2) return

        viewModelScope.launch {
            repository.createFolder(
                name = name.trim(),
                type = state.currentTab,
                parentFolderId = state.currentFolderId
            )
            closeNewFolderDialog()
        }
    }

    fun openThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = true)
    }

    fun closeThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = false)
    }

    fun applyThemePreset(preset: ShelfThemePreset) {
        _uiState.value = _uiState.value.copy(themePreset = preset)
        ShelfThemePresets.saveTheme(getApplication(), preset)
    }

    fun applyCustomTheme(primary: Color, secondary: Color) {
        val custom = ShelfThemePreset("自定义", primary, secondary)
        _uiState.value = _uiState.value.copy(themePreset = custom)
        ShelfThemePresets.saveTheme(getApplication(), custom)
    }

    fun selectBookForMenu(book: BookEntity?) {
        _uiState.value = _uiState.value.copy(selectedBookForMenu = book)
    }

    fun selectFolderForMenu(folder: FolderEntity?) {
        _uiState.value = _uiState.value.copy(selectedFolderForMenu = folder)
    }

    fun togglePinBook(book: BookEntity) {
        viewModelScope.launch {
            repository.togglePin(book.id, book.isPinned)
            selectBookForMenu(null)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            // Delete book record only from shelf without deleting the source file
            repository.deleteBook(book.id)
            selectBookForMenu(null)
        }
    }

    fun openMoveBookDialog() {
        _uiState.value = _uiState.value.copy(showMoveBookDialog = true)
    }

    fun closeMoveBookDialog() {
        _uiState.value = _uiState.value.copy(showMoveBookDialog = false)
    }

    fun moveBookToFolder(targetFolderId: Long?) {
        val book = _uiState.value.selectedBookForMenu ?: return
        viewModelScope.launch {
            repository.moveToFolder(book.id, targetFolderId)
            closeMoveBookDialog()
            selectBookForMenu(null)
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            // Delete folder and move books inside back to parent/root
            repository.deleteFolder(folder.id, folder.parentFolderId)
            selectFolderForMenu(null)
        }
    }

    fun importDocuments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            val imported = FileImporter.importUris(
                context = getApplication(),
                uris = uris,
                targetFolderId = _uiState.value.currentFolderId
            )
            for (book in imported) {
                repository.insertBook(book)
            }
            _uiState.value = _uiState.value.copy(isImporting = false)
        }
    }
}
