package com.example.ui.reader.novel

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NovelColorPalettes
import com.example.data.model.PageTurnMode
import com.example.ui.reader.ReaderViewModel
import com.example.ui.reader.components.ReaderStatusBar
import com.example.util.NovelParser
import com.example.util.SystemUiHelper
import com.example.util.readerClickZones
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreen(
    viewModel: ReaderViewModel,
    onBackToShelf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Enter Sticky Immersive Mode without borders (Requirement 1)
    DisposableEffect(activity) {
        if (activity != null) {
            SystemUiHelper.enterImmersiveSticky(activity)
            SystemUiHelper.applyScreenBrightness(
                activity,
                uiState.novelConfig.brightnessPercent,
                uiState.novelConfig.isNightMode
            )
        }
        onDispose {
            if (activity != null) {
                SystemUiHelper.exitImmersive(activity)
                SystemUiHelper.restoreScreenBrightness(activity)
            }
        }
    }

    // Apply dynamic brightness changes
    LaunchedEffect(uiState.novelConfig.brightnessPercent, uiState.novelConfig.isNightMode) {
        if (activity != null) {
            SystemUiHelper.applyScreenBrightness(
                activity,
                uiState.novelConfig.brightnessPercent,
                uiState.novelConfig.isNightMode
            )
        }
    }

    // Handle system back gesture
    BackHandler {
        if (uiState.isMenuVisible || uiState.isChapterDrawerVisible || uiState.isSettingsDrawerVisible) {
            viewModel.hideMenu()
        } else {
            onBackToShelf()
        }
    }

    val config = uiState.novelConfig
    val bgColor = config.currentBgColor
    val textColor = config.currentTextColor

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("novel_reader_container")
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = textColor)
            }
        } else {
            val pages = uiState.novelPages
            val totalPages = pages.size.coerceAtLeast(1)
            val currentPage = uiState.currentNovelPageIndex

            // Text Content with PageTurnMode switch
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .readerClickZones(
                        onPrevious = { viewModel.previousNovelPage() },
                        onToggleMenu = { viewModel.toggleMenu() },
                        onNext = { viewModel.nextNovelPage() }
                    )
            ) {
                when (config.pageTurnMode) {
                    PageTurnMode.VERTICAL_SCROLL -> {
                        val verticalScrollState = rememberScrollState()
                        // Scrollable continuous reading
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScrollState)
                                .padding(horizontal = 24.dp, vertical = 36.dp)
                        ) {
                            Text(
                                text = uiState.currentChapterTitle,
                                fontSize = (config.fontSizeSp + 4).sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            val displayText = pages.getOrNull(currentPage) ?: ""
                            Text(
                                text = displayText,
                                fontSize = config.fontSizeSp.sp,
                                letterSpacing = (config.letterSpacing * config.fontSizeSp * 0.2f).sp,
                                lineHeight = (config.fontSizeSp * config.lineHeightMultiplier).sp,
                                color = textColor,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    PageTurnMode.LEFT_TO_RIGHT -> {
                        // Standard paging from left to right
                        val pagerState = rememberPagerState(
                            initialPage = currentPage,
                            pageCount = { totalPages }
                        )

                        LaunchedEffect(currentPage) {
                            if (pagerState.currentPage != currentPage) {
                                pagerState.scrollToPage(currentPage)
                            }
                        }

                        LaunchedEffect(pagerState.currentPage) {
                            if (pagerState.currentPage != currentPage) {
                                viewModel.setNovelPage(pagerState.currentPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIdx ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 36.dp)
                            ) {
                                Text(
                                    text = pages.getOrNull(pageIdx) ?: "",
                                    fontSize = config.fontSizeSp.sp,
                                    letterSpacing = (config.letterSpacing * config.fontSizeSp * 0.2f).sp,
                                    lineHeight = (config.fontSizeSp * config.lineHeightMultiplier).sp,
                                    color = textColor,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    PageTurnMode.RIGHT_TO_LEFT -> {
                        // Horizontal right to left paging
                        val pagerState = rememberPagerState(
                            initialPage = currentPage,
                            pageCount = { totalPages }
                        )

                        LaunchedEffect(currentPage) {
                            if (pagerState.currentPage != currentPage) {
                                pagerState.scrollToPage(currentPage)
                            }
                        }

                        LaunchedEffect(pagerState.currentPage) {
                            if (pagerState.currentPage != currentPage) {
                                viewModel.setNovelPage(pagerState.currentPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIdx ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 36.dp)
                            ) {
                                Text(
                                    text = pages.getOrNull(pageIdx) ?: "",
                                    fontSize = config.fontSizeSp.sp,
                                    letterSpacing = (config.letterSpacing * config.fontSizeSp * 0.2f).sp,
                                    lineHeight = (config.fontSizeSp * config.lineHeightMultiplier).sp,
                                    color = textColor,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Floating Status Bar UI (Requirement 3.5)
            ReaderStatusBar(
                visible = config.showStatusBarUi,
                isTopRight = config.statusBarAtTopRight,
                isOpaqueBlack = config.statusBarOpaqueBlack,
                currentPage = currentPage + 1,
                totalPages = totalPages,
                progressPercent = if (totalPages > 0) (currentPage + 1).toFloat() / totalPages else 0f,
                isComic = false,
                modifier = Modifier.align(
                    if (config.statusBarAtTopRight) Alignment.TopEnd else Alignment.TopStart
                )
            )

            // Top Menu Overlay (Frosted Minimalist)
            AnimatedVisibility(
                visible = uiState.isMenuVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0xDD121212),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        IconButton(
                            onClick = onBackToShelf,
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回书架",
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.book?.title ?: "小说阅读",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = uiState.currentChapterTitle,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { viewModel.addBookmark() },
                            modifier = Modifier.testTag("bookmark_button")
                        ) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = "添加书签",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Menu Overlay
            AnimatedVisibility(
                visible = uiState.isMenuVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color(0xEE14171A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Progress slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "上一页",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { viewModel.previousNovelPage() }
                                    .padding(4.dp)
                            )
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { viewModel.setNovelPage(it.toInt()) },
                                valueRange = 0f..(totalPages - 1).coerceAtLeast(0).toFloat(),
                                steps = (totalPages - 2).coerceAtLeast(0),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = "下一页",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { viewModel.nextNovelPage() }
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick buttons: 目录, 夜间关灯模式, 排版设置
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Catalog / Chapter list
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.toggleChapterDrawer() }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = "目录", tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("目录", color = Color.White, fontSize = 12.sp)
                            }

                            // Extreme dark / night mode (Moon icon)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.toggleNovelNightMode() }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.NightlightRound,
                                    contentDescription = "夜间模式",
                                    tint = if (config.isNightMode) Color(0xFFFFD54F) else Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (config.isNightMode) "关灯中" else "关灯模式",
                                    color = if (config.isNightMode) Color(0xFFFFD54F) else Color.White,
                                    fontSize = 12.sp
                                )
                            }

                            // Typography and display settings
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.toggleSettingsDrawer() }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.FormatSize, contentDescription = "排版设置", tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("排版", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Chapters & Bookmarks
    if (uiState.isChapterDrawerVisible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleChapterDrawer() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "目录 (${uiState.novelChapters.size}章)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.novelChapters) { chapter ->
                        val isSelected = chapter.title == uiState.currentChapterTitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.jumpToNovelChapter(chapter) }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = chapter.title,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Typography & Visual Settings
    if (uiState.isSettingsDrawerVisible) {
        val sheetState = rememberModalBottomSheetState()
        val config = uiState.novelConfig

        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleSettingsDrawer() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("阅读视觉与排版设置", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Brightness Slider (1% to 100%)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("屏幕亮度: ${(config.brightnessPercent * 100).toInt()}%", fontSize = 14.sp)
                }
                Slider(
                    value = config.brightnessPercent,
                    onValueChange = { viewModel.updateNovelConfig { c -> c.copy(brightnessPercent = it) } },
                    valueRange = 0.01f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Background Color Palette Presets (Requirement 3.1)
                Text("阅读底色", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NovelColorPalettes.presets.forEach { palette ->
                        val isSelected = config.backgroundColorHex.equals(palette.bgHex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(palette.bgHex))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateNovelConfig { c ->
                                            c.copy(
                                                backgroundColorHex = palette.bgHex,
                                                textColorHex = palette.textHex,
                                                isNightMode = false
                                            )
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(palette.name, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Typography Controls (Requirement 3.2): Font size, Letter spacing, Line height, Paragraph spacing
                // 1. Font Size: 12sp ~ 28sp (step 1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("字号: ${config.fontSizeSp.toInt()} sp", fontSize = 14.sp)
                    Row {
                        Text(
                            text = "A -",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                                .clickable {
                                    if (config.fontSizeSp > 12f) {
                                        viewModel.updateNovelConfig { it.copy(fontSizeSp = it.fontSizeSp - 1f) }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "A +",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                                .clickable {
                                    if (config.fontSizeSp < 28f) {
                                        viewModel.updateNovelConfig { it.copy(fontSizeSp = it.fontSizeSp + 1f) }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Line Height: 1.0 ~ 3.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("行距倍数: ${String.format("%.1f", config.lineHeightMultiplier)}x", fontSize = 14.sp)
                    Row {
                        Text(
                            text = "- 0.1",
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                                .clickable {
                                    if (config.lineHeightMultiplier > 1.0f) {
                                        viewModel.updateNovelConfig { it.copy(lineHeightMultiplier = it.lineHeightMultiplier - 0.1f) }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ 0.1",
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                                .clickable {
                                    if (config.lineHeightMultiplier < 3.0f) {
                                        viewModel.updateNovelConfig { it.copy(lineHeightMultiplier = it.lineHeightMultiplier + 0.1f) }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Letter Spacing: 0.0 ~ 1.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("字距: ${String.format("%.1f", config.letterSpacing)}", fontSize = 14.sp)
                    Slider(
                        value = config.letterSpacing,
                        onValueChange = { viewModel.updateNovelConfig { c -> c.copy(letterSpacing = it) } },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.width(180.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Turn Direction (Requirement 3.3)
                Text("翻页模式", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PageTurnMode.entries.forEach { mode ->
                        val isSelected = config.pageTurnMode == mode
                        Text(
                            text = mode.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray.copy(alpha = 0.2f)
                                )
                                .clickable { viewModel.setNovelPageTurnMode(mode) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Bar UI Switches (Requirement 3.5)
                Text("悬浮状态栏", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("显示悬浮状态栏", fontSize = 14.sp)
                    Switch(
                        checked = config.showStatusBarUi,
                        onCheckedChange = { viewModel.updateNovelConfig { c -> c.copy(showStatusBarUi = it) } }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("状态栏位置 (开启为右上角，关闭为左上角)", fontSize = 14.sp)
                    Switch(
                        checked = config.statusBarAtTopRight,
                        onCheckedChange = { viewModel.updateNovelConfig { c -> c.copy(statusBarAtTopRight = it) } }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("不透明纯黑背景遮挡底字", fontSize = 14.sp)
                    Switch(
                        checked = config.statusBarOpaqueBlack,
                        onCheckedChange = { viewModel.updateNovelConfig { c -> c.copy(statusBarOpaqueBlack = it) } }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
