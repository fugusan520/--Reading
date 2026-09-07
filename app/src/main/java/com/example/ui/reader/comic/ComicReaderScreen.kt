package com.example.ui.reader.comic

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PageTurnMode
import com.example.ui.reader.ReaderViewModel
import com.example.ui.reader.components.ReaderStatusBar
import com.example.util.SystemUiHelper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicReaderScreen(
    viewModel: ReaderViewModel,
    onBackToShelf: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    val config = uiState.comicConfig
    var containerScale by remember { mutableFloatStateOf(1.0f) }
    var containerOffset by remember { mutableStateOf(Offset.Zero) }

    var showDisplayAdjustPanel by remember { mutableStateOf(false) }
    var showSecondaryOptionsSheet by remember { mutableStateOf(false) }

    // Immersive Mode & Screen Brightness
    DisposableEffect(activity) {
        if (activity != null) {
            SystemUiHelper.enterImmersiveSticky(activity)
            SystemUiHelper.applyScreenBrightness(
                activity,
                uiState.comicConfig.brightnessPercent,
                uiState.comicConfig.isNightMode
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

    LaunchedEffect(uiState.comicConfig.brightnessPercent, uiState.comicConfig.isNightMode) {
        if (activity != null) {
            SystemUiHelper.applyScreenBrightness(
                activity,
                uiState.comicConfig.brightnessPercent,
                uiState.comicConfig.isNightMode
            )
        }
    }

    // Back gesture handling
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("comic_reader_container")
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FF66))
            }
        } else {
            val pageCount = uiState.comicPageCount
            val currentPage = uiState.currentComicPageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = maxWidth.value
                val screenHeight = maxHeight.value

                // Gestures: Double-tap, Pinch-to-zoom with boundary control & Single-tap navigation (Requirement 3.3)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (containerScale * zoom).coerceIn(1.0f, 4.0f)
                                containerScale = newScale
                                viewModel.updateComicZoom(newScale)

                                if (newScale > 1.0f) {
                                    val maxPanX = (newScale - 1f) * size.width / 2f
                                    val maxPanY = (newScale - 1f) * size.height / 2f
                                    val newX = (containerOffset.x + pan.x).coerceIn(-maxPanX, maxPanX)
                                    val newY = (containerOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                    containerOffset = Offset(newX, newY)
                                } else {
                                    containerOffset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(pageCount) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (containerScale > 1.2f) {
                                        containerScale = 1.0f
                                        containerOffset = Offset.Zero
                                        viewModel.updateComicZoom(1.0f)
                                    } else {
                                        containerScale = 2.0f
                                        containerOffset = Offset.Zero
                                        viewModel.updateComicZoom(2.0f)
                                    }
                                },
                                onTap = { offset ->
                                    val width = size.width.toFloat()
                                    if (width > 0f) {
                                        val xRatio = offset.x / width
                                        when {
                                            xRatio < 0.30f -> viewModel.previousComicPage()
                                            xRatio < 0.70f -> {
                                                viewModel.toggleMenu()
                                                if (showDisplayAdjustPanel) showDisplayAdjustPanel = false
                                            }
                                            else -> viewModel.nextComicPage()
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(containerOffset.x.roundToInt(), containerOffset.y.roundToInt()) }
                            .graphicsLayer(
                                scaleX = containerScale,
                                scaleY = containerScale
                            )
                    ) {
                        when (config.pageTurnMode) {
                            PageTurnMode.VERTICAL_SCROLL -> {
                                // Seamless Vertical Scrolling: 0.dp spacing without borders/gaps (Requirement 3.2)
                                val comicListState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

                                LaunchedEffect(comicListState) {
                                    snapshotFlow { comicListState.firstVisibleItemIndex }
                                        .distinctUntilChanged()
                                        .collect { idx ->
                                            if (idx != currentPage) {
                                                viewModel.setComicPage(idx)
                                            }
                                        }
                                }

                                LaunchedEffect(currentPage) {
                                    if (comicListState.firstVisibleItemIndex != currentPage) {
                                        comicListState.scrollToItem(currentPage)
                                    }
                                }

                                LazyColumn(
                                    state = comicListState,
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = config.paddingHorizontalDp.dp)
                                ) {
                                    items(pageCount) { pageIdx ->
                                        ComicPageItem(
                                            viewModel = viewModel,
                                            pageIndex = pageIdx,
                                            isPdf = uiState.isPdf,
                                            isVerticalScroll = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(0.dp)
                                        )
                                    }
                                }
                            }

                            PageTurnMode.LEFT_TO_RIGHT -> {
                                val hPagerState = rememberPagerState(
                                    initialPage = currentPage,
                                    pageCount = { pageCount }
                                )

                                LaunchedEffect(currentPage) {
                                    if (hPagerState.currentPage != currentPage) {
                                        hPagerState.scrollToPage(currentPage)
                                    }
                                }

                                LaunchedEffect(hPagerState.currentPage) {
                                    if (hPagerState.currentPage != currentPage) {
                                        viewModel.setComicPage(hPagerState.currentPage)
                                    }
                                }

                                HorizontalPager(
                                    state = hPagerState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = config.paddingHorizontalDp.dp)
                                ) { pageIdx ->
                                    ComicPageItem(
                                        viewModel = viewModel,
                                        pageIndex = pageIdx,
                                        isPdf = uiState.isPdf,
                                        isVerticalScroll = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            PageTurnMode.RIGHT_TO_LEFT -> {
                                val hPagerState = rememberPagerState(
                                    initialPage = currentPage,
                                    pageCount = { pageCount }
                                )

                                LaunchedEffect(currentPage) {
                                    if (hPagerState.currentPage != currentPage) {
                                        hPagerState.scrollToPage(currentPage)
                                    }
                                }

                                LaunchedEffect(hPagerState.currentPage) {
                                    if (hPagerState.currentPage != currentPage) {
                                        viewModel.setComicPage(hPagerState.currentPage)
                                    }
                                }

                                HorizontalPager(
                                    state = hPagerState,
                                    reverseLayout = true,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = config.paddingHorizontalDp.dp)
                                ) { pageIdx ->
                                    ComicPageItem(
                                        viewModel = viewModel,
                                        pageIndex = pageIdx,
                                        isPdf = uiState.isPdf,
                                        isVerticalScroll = false,
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
                    totalPages = pageCount,
                    progressPercent = if (pageCount > 0) (currentPage + 1).toFloat() / pageCount else 0f,
                    isComic = true,
                    modifier = Modifier.align(
                        if (config.statusBarAtTopRight) Alignment.TopEnd else Alignment.TopStart
                    )
                )

                // Top Menu Overlay
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
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回书架",
                                    tint = Color.White
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.book?.title ?: "漫画阅读",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentPage + 1} / $pageCount 页",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }

                            val isBookmarked = uiState.bookmarks.any { it.pageIndex == currentPage }
                            IconButton(onClick = { viewModel.toggleBookmark() }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "添加书签",
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
                            // Progress Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "上一页",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable { viewModel.previousComicPage() }
                                        .padding(4.dp)
                                )
                                Slider(
                                    value = currentPage.toFloat(),
                                    onValueChange = { viewModel.setComicPage(it.toInt()) },
                                    valueRange = 0f..(pageCount - 1).coerceAtLeast(0).toFloat(),
                                    steps = (pageCount - 2).coerceAtLeast(0),
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
                                        .clickable { viewModel.nextComicPage() }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Buttons (5 items: 目录, 关灯模式, 显示调节 1/4 screen, 选项, 设置 - Requirement 3.1)
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 1. Catalog
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

                                // 2. Night mode
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { viewModel.toggleComicNightMode() }
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

                                // 3. Display Adjustment (1/4 screen height panel - Requirement 3.1)
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

                                // 4. Secondary Options
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

                                // 5. Settings shortcut (Requirement 3.1)
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

                // Display Adjustment Panel: Exactly ~1/4 screen height (Requirement 3.1)
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
                                    onValueChange = { viewModel.updateComicBrightness(it) },
                                    valueRange = if (config.isNightMode) 0.01f..0.20f else 0.01f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (config.isNightMode) Color(0xFFFFD54F) else Color(0xFF00FF66),
                                        activeTrackColor = if (config.isNightMode) Color(0xFFFFD54F) else Color(0xFF00FF66)
                                    ),
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                            }

                            // Row 2: Reset Zoom & Scale Quick Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("缩放比例: ${(containerScale * 100).toInt()}%", color = Color.White, fontSize = 12.sp)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF333333))
                                            .clickable {
                                                containerScale = 1.0f
                                                containerOffset = Offset.Zero
                                                viewModel.updateComicZoom(1.0f)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("适应屏幕 (100%)", color = Color.White, fontSize = 11.sp)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF333333))
                                            .clickable {
                                                containerScale = 2.0f
                                                viewModel.updateComicZoom(2.0f)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("放大 200%", color = Color(0xFF00FF66), fontSize = 11.sp)
                                    }
                                }
                            }

                            // Row 3: Horizontal Margins (左右页面边距 0-80dp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("边距: ${config.paddingHorizontalDp.toInt()}dp", color = Color.White, fontSize = 12.sp)
                                Slider(
                                    value = config.paddingHorizontalDp,
                                    onValueChange = { viewModel.updateComicConfig { c -> c.copy(paddingHorizontalDp = it) } },
                                    valueRange = 0f..80f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF66), activeTrackColor = Color(0xFF00FF66)),
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Comic Chapters / Pages
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
                    text = "页面目录 (${uiState.comicPageCount}页)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.comicPageCount) { pageIndex ->
                        val isSelected = pageIndex == uiState.currentComicPageIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setComicPage(pageIndex)
                                    viewModel.toggleChapterDrawer()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = "第 ${pageIndex + 1} 页",
                                color = if (isSelected) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Secondary Options Menu (AI Crop, Split, Page Turn Mode - Requirement 3.1)
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
                Text("漫画与图像选项", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                // Page Turn Direction
                Column {
                    Text("翻页方向", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                                    .clickable { viewModel.setComicPageTurnMode(mode) }
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

                // AI Crop White Borders
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("智能自动裁白边", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("自动识别画面四周亮度高且连续的白边并裁切", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = config.autoCropWhiteBorders,
                        onCheckedChange = { viewModel.updateComicConfig { c -> c.copy(autoCropWhiteBorders = it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FF66))
                    )
                }

                // Auto Split Double-Page
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动分割双页跨页", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("当图片宽>高*1.3时，从中轴线自动切分为左右双页", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = config.autoSplitDoublePage,
                        onCheckedChange = { viewModel.updateComicConfig { c -> c.copy(autoSplitDoublePage = it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FF66))
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
                        onCheckedChange = { viewModel.updateComicConfig { c -> c.copy(showStatusBarUi = it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00FF66))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ComicPageItem(
    viewModel: ReaderViewModel,
    pageIndex: Int,
    isPdf: Boolean,
    isVerticalScroll: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, pageIndex, isPdf) {
        value = if (isPdf) {
            viewModel.renderPdfPage(pageIndex)
        } else {
            viewModel.loadComicBitmap(pageIndex)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            if (isVerticalScroll) {
                // In vertical scroll mode, seamless continuous flow with exact image aspect ratio
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "第 ${pageIndex + 1} 页",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                )
            } else {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "第 ${pageIndex + 1} 页",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isVerticalScroll) 320.dp else 480.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00FF66).copy(alpha = 0.6f))
            }
        }
    }
}
