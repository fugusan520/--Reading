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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.util.SystemUiHelper
import com.example.util.readerClickZones
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreen(
    viewModel: ReaderViewModel,
    onBackToShelf: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var showDisplayAdjustPanel by remember { mutableStateOf(false) }
    var showSecondaryOptionsSheet by remember { mutableStateOf(false) }

    // Enter Sticky Immersive Mode without borders
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
            viewModel.saveCurrentProgress()
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
        when {
            showDisplayAdjustPanel -> showDisplayAdjustPanel = false
            showSecondaryOptionsSheet -> showSecondaryOptionsSheet = false
            uiState.isMenuVisible || uiState.isChapterDrawerVisible -> viewModel.hideMenu()
            else -> {
                viewModel.saveCurrentProgress()
                onBackToShelf()
            }
        }
    }

    val config = uiState.novelConfig
    val bgColor = config.currentBgColor
    val textColor = config.currentTextColor

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp)
            .background(bgColor)
            .testTag("novel_reader_container")
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val pages = uiState.novelPages
            val totalPages = pages.size
            val currentPage = uiState.currentNovelPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))

            // Main Text Container
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .readerClickZones(
                        onPrevious = { viewModel.previousNovelPage() },
                        onToggleMenu = {
                            viewModel.toggleMenu()
                            if (showDisplayAdjustPanel) showDisplayAdjustPanel = false
                        },
                        onNext = { viewModel.nextNovelPage() }
                    )
            ) {
                when (config.pageTurnMode) {
                    PageTurnMode.VERTICAL_SCROLL -> {
                        // Continuous vertical scrolling with real-time char offset tracking (Requirement 2.1)
                        val vListState = rememberLazyListState()

                        // Listen to scroll changes and update reading progress continuously without page snapping
                        LaunchedEffect(vListState) {
                            snapshotFlow {
                                val firstIdx = vListState.firstVisibleItemIndex
                                val firstOffset = vListState.firstVisibleItemScrollOffset
                                Pair(firstIdx, firstOffset)
                            }
                                .distinctUntilChanged()
                                .collect { (idx, _) ->
                                    val charOffset = uiState.novelParagraphOffsets.getOrNull(idx)?.toLong() ?: 0L
                                    viewModel.updateNovelScrollPosition(charOffset)
                                }
                        }

                        // Scroll to position when external jump happens (e.g. chapter select or bookmark)
                        LaunchedEffect(uiState.currentScrollCharOffset) {
                            if (uiState.novelParagraphOffsets.isNotEmpty()) {
                                val targetIdx = uiState.novelParagraphOffsets.binarySearch(uiState.currentScrollCharOffset.toInt()).let {
                                    if (it < 0) (-it - 2).coerceAtLeast(0) else it
                                }
                                if (targetIdx in uiState.novelParagraphs.indices && vListState.firstVisibleItemIndex != targetIdx) {
                                    vListState.scrollToItem(targetIdx)
                                }
                            }
                        }

                        LazyColumn(
                            state = vListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = config.paddingHorizontalDp.dp)
                        ) {
                            itemsIndexed(uiState.novelParagraphs) { _, para ->
                                if (para.isNotBlank()) {
                                    Text(
                                        text = para,
                                        fontSize = config.fontSizeSp.sp,
                                        letterSpacing = (config.letterSpacing * config.fontSizeSp * 0.2f).sp,
                                        lineHeight = (config.fontSizeSp * config.lineHeightMultiplier).sp,
                                        color = textColor,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = (config.fontSizeSp * config.paragraphSpacingMultiplier).dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height((config.fontSizeSp * config.paragraphSpacingMultiplier).dp))
                                }
                            }
                        }
                    }

                    PageTurnMode.LEFT_TO_RIGHT -> {
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = config.paddingHorizontalDp.dp)
                        ) { pageIdx ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(0.dp)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = config.paddingHorizontalDp.dp)
                        ) { pageIdx ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(0.dp)
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

            // Floating Status Bar UI
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
                            onClick = {
                                viewModel.saveCurrentProgress()
                                onBackToShelf()
                            },
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

                        val isBookmarked = uiState.bookmarks.any { it.pageIndex == currentPage }
                        IconButton(
                            onClick = { viewModel.toggleBookmark() },
                            modifier = Modifier.testTag("bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "书签",
                                tint = if (isBookmarked) Color(0xFF00FF66) else Color.White
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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

                        // Bottom Menu Bar (5 items: 目录, 关灯模式, 显示调节 1/4 screen, 选项, 设置 - Requirement 2.2 & 2.3)
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Catalog / Chapter list
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.toggleChapterDrawer() }
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = "目录", tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("目录", color = Color.White, fontSize = 11.sp)
                            }

                            // 2. Extreme dark / night mode (Moon icon)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.toggleNovelNightMode() }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.NightlightRound,
                                    contentDescription = "夜间模式",
                                    tint = if (config.isNightMode) Color(0xFFFFD54F) else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (config.isNightMode) "关灯中" else "关灯模式",
                                    color = if (config.isNightMode) Color(0xFFFFD54F) else Color.White,
                                    fontSize = 11.sp
                                )
                            }

                            // 3. Display Adjustment (1/4 screen height panel - Requirement 2.2)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { showDisplayAdjustPanel = !showDisplayAdjustPanel }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "显示调节",
                                    tint = if (showDisplayAdjustPanel) Color(0xFF00FF66) else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("显示调节", color = if (showDisplayAdjustPanel) Color(0xFF00FF66) else Color.White, fontSize = 11.sp)
                            }

                            // 4. Secondary Options (Requirement 2.2)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { showSecondaryOptionsSheet = true }
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Widgets, contentDescription = "选项", tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("选项", color = Color.White, fontSize = 11.sp)
                            }

                            // 5. Settings shortcut (Requirement 2.3)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onOpenSettings() }
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("设置", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Display Adjustment Panel: Exactly ~1/4 screen height (Requirement 2.2)
            AnimatedVisibility(
                visible = showDisplayAdjustPanel,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color(0xF5181A1D),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Brightness Slider (Normal 1%-100% or Night 1%-20% - Requirement B4)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (config.isNightMode) Icons.Default.NightlightRound else Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = if (config.isNightMode) Color(0xFFFFD54F) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (config.isNightMode) "夜间 ${(config.nightBrightness * 100).toInt()}%" else "亮度 ${(config.normalBrightness * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = config.brightnessPercent,
                                onValueChange = { viewModel.updateNovelBrightness(it) },
                                valueRange = if (config.isNightMode) 0.01f..0.20f else 0.01f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (config.isNightMode) Color(0xFFFFD54F) else Color(0xFF00FF66),
                                    activeTrackColor = if (config.isNightMode) Color(0xFFFFD54F) else Color(0xFF00FF66)
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                        }

                        // Row 2: Font Size (A- / A+) & Line Height (- / +)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Font Size
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("字号: ${config.fontSizeSp.toInt()}", color = Color.White, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "A-",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF333333))
                                        .clickable {
                                            if (config.fontSizeSp > 12f) {
                                                viewModel.updateNovelConfig { it.copy(fontSizeSp = it.fontSizeSp - 1f) }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "A+",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF333333))
                                        .clickable {
                                            if (config.fontSizeSp < 32f) {
                                                viewModel.updateNovelConfig { it.copy(fontSizeSp = it.fontSizeSp + 1f) }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Line Height
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("行距: ${String.format("%.1f", config.lineHeightMultiplier)}x", color = Color.White, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "-",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF333333))
                                        .clickable {
                                            if (config.lineHeightMultiplier > 1.0f) {
                                                viewModel.updateNovelConfig { it.copy(lineHeightMultiplier = it.lineHeightMultiplier - 0.1f) }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF333333))
                                        .clickable {
                                            if (config.lineHeightMultiplier < 3.0f) {
                                                viewModel.updateNovelConfig { it.copy(lineHeightMultiplier = it.lineHeightMultiplier + 0.1f) }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Row 3: Horizontal Margins (左右页面边距 0-80dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("边距: ${config.paddingHorizontalDp.toInt()}dp", color = Color.White, fontSize = 12.sp)
                            Slider(
                                value = config.paddingHorizontalDp,
                                onValueChange = { viewModel.updateNovelConfig { c -> c.copy(paddingHorizontalDp = it) } },
                                valueRange = 0f..80f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF66), activeTrackColor = Color(0xFF00FF66)),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                        }

                        // Row 4: Background Color Palette Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NovelColorPalettes.presets.forEach { palette ->
                                val isSelected = config.backgroundColorHex.equals(palette.bgHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(palette.bgHex)))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF00FF66) else Color.Gray,
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
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "文",
                                        color = Color(android.graphics.Color.parseColor(palette.textHex)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Chapters
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
                                color = if (isSelected) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
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

    // Modal Bottom Sheet: Secondary Options Menu (Requirement 2.2)
    if (showSecondaryOptionsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSecondaryOptionsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("阅读排版与页面选项", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                // Page Turn Mode
                Column {
                    Text("翻页模式", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PageTurnMode.values().forEach { mode ->
                            val isSelected = config.pageTurnMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF00FF66) else Color(0xFF2A2A2A))
                                    .clickable { viewModel.setNovelPageTurnMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // Paragraph Spacing
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("段落间距倍数", fontSize = 14.sp)
                        Text("${String.format("%.1f", config.paragraphSpacingMultiplier)}x", color = Color(0xFF00FF66), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = config.paragraphSpacingMultiplier,
                        onValueChange = { viewModel.updateNovelConfig { c -> c.copy(paragraphSpacingMultiplier = it) } },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF66), activeTrackColor = Color(0xFF00FF66))
                    )
                }

                // Status Bar Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("显示极简悬浮状态栏", fontSize = 14.sp)
                        Text("显示电量、时间与页码进度", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = config.showStatusBarUi,
                        onCheckedChange = { viewModel.updateNovelConfig { c -> c.copy(showStatusBarUi = it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FF66))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
