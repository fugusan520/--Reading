package com.example.ui.shelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import com.example.data.model.FolderEntity
import com.example.ui.theme.ShelfThemePresets
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShelfScreen(
    viewModel: ShelfViewModel,
    onOpenBook: (BookEntity) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allFolders by viewModel.allDestinationFolders.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importDocuments(uris)
        }
    }

    var isSearchActive by remember { mutableStateOf(false) }

    // Elegant Dark Theme Palette
    val primaryColor = uiState.themePreset.primary
    val secondaryColor = uiState.themePreset.secondary
    val elegantBg = Color(0xFF000000)
    val elegantAccent = secondaryColor // Defaults to #00FF66
    val elegantSurface = Color(0xFF1A1A1A)
    val elegantBorder = Color(0xFF333333)
    val elegantDivider = Color(0xFF1A1A1A)
    val elegantMuted = Color(0xFF8E8E8E)
    val elegantContainer = Color(0xFF1A3326)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = elegantBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(elegantBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Title & Subtitle
                    if (uiState.currentFolderId != null) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(elegantSurface)
                                    .clickable { viewModel.exitFolder() }
                                    .testTag("back_folder_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回上一级",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = uiState.currentFolderName ?: "文件夹",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.5).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "FOLDER",
                                    color = elegantMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "极简阅读",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1
                            )
                            Text(
                                text = "MINIMALIST READER",
                                color = elegantMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Actions (Adaptive 38dp buttons with 6dp spacing to guarantee '+' import button is never clipped - Bug B2 Fix)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(elegantSurface)
                                .clickable {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) viewModel.setSearchQuery("")
                                }
                                .testTag("search_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = if (isSearchActive) Color.White else elegantAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // View mode toggle
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(elegantSurface)
                                .clickable { viewModel.toggleViewMode() }
                                .testTag("view_mode_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "切换视图",
                                tint = elegantMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // New folder button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(elegantSurface)
                                .clickable(enabled = uiState.folderDepth < 2) { viewModel.openNewFolderDialog() }
                                .testTag("new_folder_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "新建文件夹",
                                tint = if (uiState.folderDepth < 2) elegantMuted else Color(0xFF444444),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings Centre shortcut (Requirement 1.1)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(elegantSurface)
                                .clickable { onOpenSettings() }
                                .testTag("settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置中心",
                                tint = elegantMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Add / Import button (Prominent, Always fully displayed - Bug B2 Fix)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(elegantAccent)
                                .clickable {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "application/pdf",
                                            "image/*",
                                            "application/zip",
                                            "application/x-zip-compressed",
                                            "application/x-7z-compressed",
                                            "application/x-rar-compressed",
                                            "*/*"
                                        )
                                    )
                                }
                                .testTag("import_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "导入书籍",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Search field
                if (isSearchActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("搜索书名...", color = elegantMuted, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = elegantSurface,
                            unfocusedContainerColor = elegantSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = elegantAccent,
                            unfocusedBorderColor = elegantBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Elegant Tab Strip: 小说 / 漫画
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = elegantDivider,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        },
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Novel tab
                    Column(
                        modifier = Modifier
                            .clickable { viewModel.setTab(BookType.NOVEL) }
                            .padding(bottom = 10.dp)
                            .testTag("tab_novel")
                    ) {
                        Text(
                            text = "小说",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.currentTab == BookType.NOVEL) elegantAccent else elegantMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .background(if (uiState.currentTab == BookType.NOVEL) elegantAccent else Color.Transparent)
                        )
                    }

                    // Comic tab
                    Column(
                        modifier = Modifier
                            .clickable { viewModel.setTab(BookType.COMIC) }
                            .padding(bottom = 10.dp)
                            .testTag("tab_comic")
                    ) {
                        Text(
                            text = "漫画",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.currentTab == BookType.COMIC) elegantAccent else elegantMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .background(if (uiState.currentTab == BookType.COMIC) elegantAccent else Color.Transparent)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = elegantBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .drawBehind {
                            drawLine(
                                color = elegantDivider,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bookshelf Tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { /* Already on shelf */ }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(elegantContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "书架",
                                tint = elegantAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "书架",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = elegantAccent,
                            letterSpacing = 1.sp
                        )
                    }

                    // Settings / Theme Tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { viewModel.openThemeDialog() }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "设置",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(elegantBg)
        ) {
            if (uiState.isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = secondaryColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在智能识别与解压文件...", color = Color.White, fontSize = 14.sp)
                    }
                }
            } else if (folders.isEmpty() && books.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            if (uiState.currentTab == BookType.NOVEL) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.currentTab == BookType.NOVEL) "书架空空如也" else "漫画架空空如也",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右上角 + 导入本地 TXT/PDF/EPUB/图片/压缩包",
                            fontSize = 13.sp,
                            color = Color.Gray.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                if (uiState.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folders, key = { "f_${it.id}" }) { folder ->
                            FolderGridCard(
                                folder = folder,
                                secondaryColor = secondaryColor,
                                onClick = { viewModel.enterFolder(folder) },
                                onLongClick = { viewModel.selectFolderForMenu(folder) }
                            )
                        }
                        items(books, key = { "b_${it.id}" }) { book ->
                            BookGridCard(
                                book = book,
                                secondaryColor = secondaryColor,
                                onClick = { onOpenBook(book) },
                                onLongClick = { viewModel.selectBookForMenu(book) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folders, key = { "f_${it.id}" }) { folder ->
                            FolderListRow(
                                folder = folder,
                                secondaryColor = secondaryColor,
                                onClick = { viewModel.enterFolder(folder) },
                                onLongClick = { viewModel.selectFolderForMenu(folder) }
                            )
                        }
                        items(books, key = { "b_${it.id}" }) { book ->
                            BookListRow(
                                book = book,
                                secondaryColor = secondaryColor,
                                onClick = { onOpenBook(book) },
                                onLongClick = { viewModel.selectBookForMenu(book) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: New Folder
    if (uiState.showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.closeNewFolderDialog() },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.createFolder(folderName) },
                    enabled = folderName.isNotBlank()
                ) {
                    Text("创建", color = secondaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeNewFolderDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // Dialog: Theme Customization (Requirement 5)
    if (uiState.showThemeDialog) {
        ThemeSettingsDialog(
            currentPreset = uiState.themePreset,
            onSelectPreset = { viewModel.applyThemePreset(it) },
            onApplyCustom = { pri, sec -> viewModel.applyCustomTheme(pri, sec) },
            onDismiss = { viewModel.closeThemeDialog() }
        )
    }

    // BottomSheet: Book Context Menu (Pin, Move, Delete)
    val selectedBook = uiState.selectedBookForMenu
    if (selectedBook != null && !uiState.showMoveBookDialog) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectBookForMenu(null) },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = selectedBook.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Pin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = { viewModel.togglePinBook(selectedBook) })
                        .padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (selectedBook.isPinned) secondaryColor else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(if (selectedBook.isPinned) "取消置顶" else "置顶到书架顶部")
                }

                // Move to folder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = { viewModel.openMoveBookDialog() })
                        .padding(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("移动到文件夹")
                }

                // Remove from shelf
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = { viewModel.deleteBook(selectedBook) })
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("从书架移除（保留原文件）", color = Color(0xFFFF5252))
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog: Move Book to Folder
    if (uiState.showMoveBookDialog && selectedBook != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeMoveBookDialog() },
            title = { Text("选择目标文件夹") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        TextButton(
                            onClick = { viewModel.moveBookToFolder(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("书架根目录 (移除出文件夹)", color = secondaryColor)
                        }
                    }
                    items(allFolders) { folder ->
                        TextButton(
                            onClick = { viewModel.moveBookToFolder(folder.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = secondaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(folder.name, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.closeMoveBookDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // Dialog: Folder Context Menu
    val selectedFolder = uiState.selectedFolderForMenu
    if (selectedFolder != null) {
        AlertDialog(
            onDismissRequest = { viewModel.selectFolderForMenu(null) },
            title = { Text("文件夹: ${selectedFolder.name}") },
            text = { Text("删除该文件夹后，内部的书籍将自动移回上一级/根目录，不会删除源文件。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFolder(selectedFolder) }) {
                    Text("删除文件夹", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.selectFolderForMenu(null) }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridCard(
    folder: FolderEntity,
    secondaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.5.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
        ) {
            // 2x2 miniature book indicators
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333333).copy(alpha = 0.6f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333333).copy(alpha = 0.6f))
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333333).copy(alpha = 0.6f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333333).copy(alpha = 0.6f))
                    )
                }
            }

            // Bottom-left green badge: e.g. "4 册"
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomStart)
                    .background(secondaryColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "目录",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = folder.name,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "文件夹",
            color = Color(0xFF8E8E8E),
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookGridCard(
    book: BookEntity,
    secondaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF222222), Color(0xFF111111))
                    )
                )
                .border(
                    width = if (book.isPinned) 1.5.dp else 1.dp,
                    color = if (book.isPinned) secondaryColor else Color(0xFF262626),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            if (!book.coverImagePath.isNullOrEmpty()) {
                AsyncImage(
                    model = if (book.coverImagePath.startsWith("content://") || book.coverImagePath.startsWith("http")) {
                        book.coverImagePath
                    } else {
                        File(book.coverImagePath)
                    },
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.title,
                        color = Color(0xFF8E8E8E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top right green accent dot
            if (book.isPinned || book.progressPercent < 0.05f) {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(secondaryColor, CircleShape)
                )
            }

            // Format pill badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = book.fileFormat,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom progress bar (track #333333, fill secondaryColor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF333333))
            ) {
                val prog = book.progressPercent.coerceIn(0f, 1f)
                if (prog > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = prog)
                            .fillMaxHeight()
                            .background(secondaryColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val hasProgress = book.currentPage > 1 || book.progressPercent > 0.01f
        val statusText = if (book.bookType == BookType.COMIC) {
            if (!hasProgress) "新导入" else "续读: ${book.currentPage}/${book.totalPages} 页"
        } else {
            val pct = (book.progressPercent * 100).toInt()
            val chapterText = if (!book.lastReadChapterTitle.isNullOrBlank()) "${book.lastReadChapterTitle} · " else ""
            if (!hasProgress) "新导入" else "${chapterText}已读 $pct%"
        }
        Text(
            text = statusText,
            color = if (!hasProgress) secondaryColor else Color(0xFF8E8E8E),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListRow(
    folder: FolderEntity,
    secondaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A1A1A),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "文件夹",
                    color = Color(0xFF8E8E8E),
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onLongClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color(0xFF8E8E8E))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookListRow(
    book: BookEntity,
    secondaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A1A1A),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .border(
                width = if (book.isPinned) 1.5.dp else 1.dp,
                color = if (book.isPinned) secondaryColor else Color(0xFF262626),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 62.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222222))
            ) {
                if (!book.coverImagePath.isNullOrEmpty()) {
                    AsyncImage(
                        model = if (book.coverImagePath.startsWith("content://") || book.coverImagePath.startsWith("http")) {
                            book.coverImagePath
                        } else {
                            File(book.coverImagePath)
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = book.fileFormat,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (book.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "置顶",
                            tint = secondaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = book.title,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val hasProgress = book.currentPage > 1 || book.progressPercent > 0.01f
                val statusText = if (book.bookType == BookType.COMIC) {
                    if (!hasProgress) "未读 · 共 ${book.totalPages} 页" else "续读: 第 ${book.currentPage}/${book.totalPages} 页"
                } else {
                    val pct = (book.progressPercent * 100).toInt()
                    val chapter = if (!book.lastReadChapterTitle.isNullOrBlank()) " (${book.lastReadChapterTitle})" else ""
                    if (!hasProgress) "未读 · 0%" else "续读: $pct%$chapter"
                }
                Text(
                    text = "${book.fileFormat} · $statusText",
                    color = if (hasProgress) Color(0xFFCCCCCC) else Color(0xFF8E8E8E),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { book.progressPercent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = secondaryColor,
                    trackColor = Color(0xFF333333)
                )
            }

            IconButton(onClick = onLongClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color(0xFF8E8E8E))
            }
        }
    }
}

@Composable
fun ThemeSettingsDialog(
    currentPreset: com.example.ui.theme.ShelfThemePreset,
    onSelectPreset: (com.example.ui.theme.ShelfThemePreset) -> Unit,
    onApplyCustom: (Color, Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hexPrimary by remember { mutableStateOf("#000000") }
    var hexSecondary by remember { mutableStateOf("#00FF66") }
    var customError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("书架双色主题", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("预设方案（ColorOS 15 定制）", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))

                ShelfThemePresets.presets.forEach { preset ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = { onSelectPreset(preset) })
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        // Swatches
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(preset.primary)
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(preset.secondary)
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = preset.name,
                            fontWeight = if (preset.name == currentPreset.name) FontWeight.Bold else FontWeight.Normal,
                            color = if (preset.name == currentPreset.name) preset.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("自定义 HEX 颜色代码", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hexPrimary,
                        onValueChange = { hexPrimary = it },
                        label = { Text("主颜色") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hexSecondary,
                        onValueChange = { hexSecondary = it },
                        label = { Text("次颜色") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                if (customError) {
                    Text("请输入正确的十六进制颜色 (如 #000000)", color = Color.Red, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val pri = Color(android.graphics.Color.parseColor(hexPrimary.trim()))
                        val sec = Color(android.graphics.Color.parseColor(hexSecondary.trim()))
                        onApplyCustom(pri, sec)
                        onDismiss()
                    } catch (_: Exception) {
                        customError = true
                    }
                }
            ) {
                Text("应用自定义")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}
