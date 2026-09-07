package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NovelColorPalettes
import com.example.data.model.PageTurnMode
import com.example.data.repository.ReaderPreferences
import com.example.ui.reader.ReaderViewModel
import com.example.ui.shelf.ShelfViewModel
import com.example.ui.theme.ShelfThemePreset
import com.example.ui.theme.ShelfThemePresets

@Composable
fun SettingsScreen(
    shelfViewModel: ShelfViewModel,
    readerViewModel: ReaderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember { ReaderPreferences(context) }
    val shelfUiState by shelfViewModel.uiState.collectAsStateWithLifecycle()
    val readerUiState by readerViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 小说设置, 1: 漫画设置
    var skipDetailsPage by remember { mutableStateOf(preferences.getSkipDetailsPage()) }

    // Novel local state
    val novelConfig = readerUiState.novelConfig
    // Comic local state
    val comicConfig = readerUiState.comicConfig

    val bgDark = Color(0xFF121212)
    val cardDark = Color(0xFF1E1E1E)
    val accentGreen = Color(0xFF00FF66)
    val mutedGray = Color(0xFFAAAAAA)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "设置中心",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // General Settings Card (常规设置)
            Text(
                text = "常规设置",
                color = accentGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardDark)
                    .padding(16.dp)
            ) {
                Column {
                    // Toggle: Skip Book Details Page
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "跳过详情页直接阅读",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "开启后，在书架点击书籍将直接进入阅读界面",
                                color = mutedGray,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = skipDetailsPage,
                            onCheckedChange = {
                                skipDetailsPage = it
                                preferences.setSkipDetailsPage(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = accentGreen
                            ),
                            modifier = Modifier.testTag("skip_details_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Theme Colors Swatches (Moved from Shelf Screen as per 1.1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = accentGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "书架主题配色",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShelfThemePresets.presets.forEach { preset ->
                            val isSelected = shelfUiState.themePreset.name == preset.name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { shelfViewModel.applyThemePreset(preset) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(preset.primary)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) preset.secondary else Color(0xFF444444),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(preset.secondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = preset.name,
                                    color = if (isSelected) preset.secondary else mutedGray,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two Tabs: 小说设置 vs 漫画设置
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = bgDark,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = accentGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("小说设置")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("漫画设置")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // NOVEL SETTINGS PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Page turn mode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("默认翻页方式", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PageTurnMode.values().forEach { mode ->
                                    val isSelected = novelConfig.pageTurnMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) accentGreen else Color(0xFF2A2A2A))
                                            .clickable { readerViewModel.setNovelPageTurnMode(mode) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Typography settings
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("字体与排版设置", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                            // Font size
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("字号大小", color = Color.White, fontSize = 13.sp)
                                    Text("${novelConfig.fontSizeSp.toInt()} sp", color = accentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = novelConfig.fontSizeSp,
                                    onValueChange = { readerViewModel.updateNovelConfig { c -> c.copy(fontSizeSp = it) } },
                                    valueRange = 12f..32f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(thumbColor = accentGreen, activeTrackColor = accentGreen)
                                )
                            }

                            // Line height
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("行间距", color = Color.White, fontSize = 13.sp)
                                    Text(String.format("%.1f 倍", novelConfig.lineHeightMultiplier), color = accentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = novelConfig.lineHeightMultiplier,
                                    onValueChange = { readerViewModel.updateNovelConfig { c -> c.copy(lineHeightMultiplier = it) } },
                                    valueRange = 1.0f..3.0f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(thumbColor = accentGreen, activeTrackColor = accentGreen)
                                )
                            }

                            // Paragraph spacing
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("段落间距", color = Color.White, fontSize = 13.sp)
                                    Text(String.format("%.1f 倍", novelConfig.paragraphSpacingMultiplier), color = accentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = novelConfig.paragraphSpacingMultiplier,
                                    onValueChange = { readerViewModel.updateNovelConfig { c -> c.copy(paragraphSpacingMultiplier = it) } },
                                    valueRange = 0.5f..3.0f,
                                    steps = 24,
                                    colors = SliderDefaults.colors(thumbColor = accentGreen, activeTrackColor = accentGreen)
                                )
                            }

                            // Screen Edge Margins (左右页面边距 0-80dp)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("左右页面边距", color = Color.White, fontSize = 13.sp)
                                    Text("${novelConfig.paddingHorizontalDp.toInt()} dp", color = accentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = novelConfig.paddingHorizontalDp,
                                    onValueChange = { readerViewModel.updateNovelConfig { c -> c.copy(paddingHorizontalDp = it) } },
                                    valueRange = 0f..80f,
                                    steps = 15,
                                    colors = SliderDefaults.colors(thumbColor = accentGreen, activeTrackColor = accentGreen)
                                )
                            }
                        }
                    }

                    // Color Palettes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("默认背景配色", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                NovelColorPalettes.presets.forEach { palette ->
                                    val isSelected = novelConfig.backgroundColorHex == palette.bgHex
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                readerViewModel.updateNovelConfig { c ->
                                                    c.copy(backgroundColorHex = palette.bgHex, textColorHex = palette.textHex)
                                                }
                                            }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(palette.bgHex)))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) accentGreen else Color.Gray,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "文",
                                                color = Color(android.graphics.Color.parseColor(palette.textHex)),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(palette.name, color = if (isSelected) accentGreen else Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Brightness Memory Settings (Normal + Dark Mode 1.3 & B4)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("独立亮度记忆", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                            // Normal mode brightness (1% - 100%)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("日间/正常模式亮度", color = Color.White, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${(novelConfig.normalBrightness * 100).toInt()}%", color = accentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = novelConfig.normalBrightness,
                                    onValueChange = {
                                        readerViewModel.updateNovelConfig { c -> c.copy(normalBrightness = it) }
                                    },
                                    valueRange = 0.01f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = accentGreen, activeTrackColor = accentGreen)
                                )
                            }

                            // Dark mode brightness (1% - 20%)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BrightnessLow, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("关灯/夜间模式暗光亮度 (限制 1% ~ 20%)", color = Color(0xFFFFD54F), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${(novelConfig.nightBrightness * 100).toInt()}%", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = novelConfig.nightBrightness,
                                    onValueChange = {
                                        readerViewModel.updateNovelConfig { c -> c.copy(nightBrightness = it) }
                                    },
                                    valueRange = 0.01f..0.20f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD54F), activeTrackColor = Color(0xFFFFD54F))
                                )
                            }
                        }
                    }
                }
            } else {
                // COMIC SETTINGS PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Page turn mode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("漫画翻页模式", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PageTurnMode.values().forEach { mode ->
                                    val isSelected = comicConfig.pageTurnMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) accentGreen else Color(0xFF2A2A2A))
                                            .clickable { readerViewModel.setComicPageTurnMode(mode) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Image Margins
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("图像页面左右边距", color = Color.White, fontSize = 14.sp)
                                Text("${comicConfig.paddingHorizontalDp.toInt()} dp", color = accentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = comicConfig.paddingHorizontalDp,
                                onValueChange = { readerViewModel.updateComicConfig { c -> c.copy(paddingHorizontalDp = it) } },
                                valueRange = 0f..80f,
                                steps = 15,
                                colors = SliderDefaults.colors(thumbColor = accentGreen, activeTrackColor = accentGreen)
                            )
                        }
                    }

                    // Smart Image Switches
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("图像优化功能", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("智能白边裁剪", color = Color.White, fontSize = 14.sp)
                                    Text("自动检测并裁剪扫描漫画四周冗余白色空白", color = mutedGray, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = comicConfig.autoCropWhiteBorders,
                                    onCheckedChange = {
                                        readerViewModel.updateComicConfig { c -> c.copy(autoCropWhiteBorders = it) }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accentGreen)
                                )
                            }

                            HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.8.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("双联横页自动分割", color = Color.White, fontSize = 14.sp)
                                    Text("遇到扫描横向跨页双连图时自动分为左右两页", color = mutedGray, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = comicConfig.autoSplitDoublePage,
                                    onCheckedChange = {
                                        readerViewModel.updateComicConfig { c -> c.copy(autoSplitDoublePage = it) }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accentGreen)
                                )
                            }
                        }
                    }

                    // Comic Brightness Memory
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("漫画独立亮度记忆", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("漫画日间/正常模式亮度", color = Color.White, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${(comicConfig.normalBrightness * 100).toInt()}%", color = accentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = comicConfig.normalBrightness,
                                    onValueChange = {
                                        readerViewModel.updateComicConfig { c -> c.copy(normalBrightness = it) }
                                    },
                                    valueRange = 0.01f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = accentGreen, activeTrackColor = accentGreen)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BrightnessLow, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("漫画夜间模式暗光亮度 (1% ~ 20%)", color = Color(0xFFFFD54F), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${(comicConfig.nightBrightness * 100).toInt()}%", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = comicConfig.nightBrightness,
                                    onValueChange = {
                                        readerViewModel.updateComicConfig { c -> c.copy(nightBrightness = it) }
                                    },
                                    valueRange = 0.01f..0.20f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD54F), activeTrackColor = Color(0xFFFFD54F))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
