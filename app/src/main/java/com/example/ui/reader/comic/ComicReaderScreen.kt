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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
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
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicReaderScreen(
    viewModel: ReaderViewModel,
    onBackToShelf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    val config = uiState.comicConfig
    var containerScale by remember { mutableFloatStateOf(config.zoomScale) }
    var containerOffset by remember { mutableStateOf(Offset.Zero) }

    // Immersive Fullscreen (Requirement 1)
    DisposableEffect(activity) {
        if (activity != null) {
            SystemUiHelper.enterImmersiveSticky(activity)
            SystemUiHelper.applyScreenBrightness(
                activity,
                config.brightnessPercent,
                config.isNightMode
            )
        }
        onDispose {
            if (activity != null) {
                SystemUiHelper.exitImmersive(activity)
                SystemUiHelper.restoreScreenBrightness(activity)
            }
        }
    }

    LaunchedEffect(config.brightnessPercent, config.isNightMode) {
        if (activity != null) {
            SystemUiHelper.applyScreenBrightness(
                activity,
                config.brightnessPercent,
                config.isNightMode
            )
        }
    }

    BackHandler {
        if (uiState.isMenuVisible || uiState.isSettingsDrawerVisible) {
            viewModel.hideMenu()
        } else {
            onBackToShelf()
        }
    }

    val pageCount = uiState.comicPageCount.coerceAtLeast(1)
    val currentPage = uiState.currentComicPageIndex
    val viewConfig = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("comic_reader_container")
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            // Container with Pinch-to-zoom (Requirement 7) & TouchSlop Click Resolution (Requirement 8)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Pinch to zoom and pan
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (containerScale * zoom).coerceIn(0.5f, 5.0f)
                            containerScale = newScale
                            viewModel.updateComicZoom(newScale)

                            if (newScale > 1.0f) {
                                containerOffset = Offset(
                                    x = containerOffset.x + pan.x,
                                    y = containerOffset.y + pan.y
                                )
                            } else {
                                containerOffset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(pageCount) {
                        val touchSlop = viewConfig.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val startY = down.position.y
                            var totalDist = 0f
                            var lastX = startX
                            var lastY = startY

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val currentChange = event.changes.find { it.id == down.id }

                                if (currentChange == null || !currentChange.pressed) {
                                    // Pointer Up! Requirement 8: TouchSlop check
                                    if (totalDist < touchSlop) {
                                        val width = size.width.toFloat()
                                        if (width > 0f) {
                                            // Pure physical screen ratio!
                                            val xRatio = lastX / width
                                            when {
                                                // 0% ~ 5%: Safe zone for system back gesture (Requirement 6)
                                                xRatio < 0.05f -> {}
                                                // 5% ~ 30%: Previous page
                                                xRatio < 0.30f -> viewModel.previousComicPage()
                                                // 30% ~ 70%: Pop up Menu! 100% reliable even at 5x zoom!
                                                xRatio < 0.70f -> viewModel.toggleMenu()
                                                // 70% ~ 95%: Next page
                                                xRatio < 0.95f -> viewModel.nextComicPage()
                                                // 95% ~ 100%: Right edge safe zone
                                                else -> {}
                                            }
                                        }
                                    }
                                    break
                                } else {
                                    val dx = currentChange.position.x - lastX
                                    val dy = currentChange.position.y - lastY
                                    totalDist += hypot(dx, dy)
                                    lastX = currentChange.position.x
                                    lastY = currentChange.position.y
                                }
                            }
                        }
                    }
            ) {
                // Outer graphicsLayer container scale (Requirement 7: Container matrix scaling across pages!)
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
                            val vPagerState = rememberPagerState(
                                initialPage = currentPage,
                                pageCount = { pageCount }
                            )

                            LaunchedEffect(currentPage) {
                                if (vPagerState.currentPage != currentPage) {
                                    vPagerState.scrollToPage(currentPage)
                                }
                            }

                            LaunchedEffect(vPagerState.currentPage) {
                                if (vPagerState.currentPage != currentPage) {
                                    viewModel.setComicPage(vPagerState.currentPage)
                                }
                            }

                            VerticalPager(
                                state = vPagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { pageIdx ->
                                ComicPageItem(
                                    viewModel = viewModel,
                                    pageIndex = pageIdx,
                                    isPdf = uiState.isPdf
                                )
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
                                modifier = Modifier.fillMaxSize()
                            ) { pageIdx ->
                                ComicPageItem(
                                    viewModel = viewModel,
                                    pageIndex = pageIdx,
                                    isPdf = uiState.isPdf
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
                                modifier = Modifier.fillMaxSize()
                            ) { pageIdx ->
                                ComicPageItem(
                                    viewModel = viewModel,
                                    pageIndex = pageIdx,
                                    isPdf = uiState.isPdf
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
                progressPercent = (currentPage + 1).toFloat() / pageCount,
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
                            onClick = onBackToShelf,
                            modifier = Modifier.testTag("comic_back_button")
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

                        IconButton(onClick = { viewModel.addBookmark() }) {
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

                        // Quick buttons
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Reset Zoom / Fit
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        containerScale = 1.0f
                                        containerOffset = Offset.Zero
                                        viewModel.updateComicZoom(1.0f)
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.FitScreen, contentDescription = "还原缩放", tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("适应屏幕", color = Color.White, fontSize = 12.sp)
                            }

                            // Night mode
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.toggleComicNightMode() }
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

                            // Comic Settings
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.toggleSettingsDrawer() }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("图像设置", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Comic Settings (AI cropping, double page split, direction, brightness)
    if (uiState.isSettingsDrawerVisible) {
        val sheetState = rememberModalBottomSheetState()
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
                Text("漫画与图像处理设置", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Brightness
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("屏幕亮度: ${(config.brightnessPercent * 100).toInt()}%", fontSize = 14.sp)
                }
                Slider(
                    value = config.brightnessPercent,
                    onValueChange = { viewModel.updateComicConfig { c -> c.copy(brightnessPercent = it) } },
                    valueRange = 0.01f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // AI Processing Switches (Requirement 4.3)
                Text("智能图像处理层", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动裁白边", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("自动识别画面四周亮度高且连续的白边并裁切", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = config.autoCropWhiteBorders,
                        onCheckedChange = { viewModel.updateComicConfig { c -> c.copy(autoCropWhiteBorders = it) } }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动分割双页 (Split Double-page)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("当图片宽>高*1.3跨页时，从中轴线分为左右双页", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = config.autoSplitDoublePage,
                        onCheckedChange = { viewModel.updateComicConfig { c -> c.copy(autoSplitDoublePage = it) } }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Turn Direction
                Text("翻页方向", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                                .clickable { viewModel.setComicPageTurnMode(mode) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Floating Status Bar Switches
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
                        onCheckedChange = { viewModel.updateComicConfig { c -> c.copy(showStatusBarUi = it) } }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("状态栏位置 (开启右上角，关闭左上角)", fontSize = 14.sp)
                    Switch(
                        checked = config.statusBarAtTopRight,
                        onCheckedChange = { viewModel.updateComicConfig { c -> c.copy(statusBarAtTopRight = it) } }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ComicPageItem(
    viewModel: ReaderViewModel,
    pageIndex: Int,
    isPdf: Boolean,
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
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "第 ${pageIndex + 1} 页",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
        }
    }
}
