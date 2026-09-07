package com.example.ui.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.SystemUiHelper
import kotlinx.coroutines.delay

@Composable
fun ReaderStatusBar(
    visible: Boolean,
    isTopRight: Boolean,
    isOpaqueBlack: Boolean,
    currentPage: Int,
    totalPages: Int,
    progressPercent: Float,
    isComic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var battery by remember { mutableIntStateOf(SystemUiHelper.getBatteryPercentage(context)) }
    var timeStr by remember { mutableStateOf(SystemUiHelper.getCurrentTimeFormatted()) }

    LaunchedEffect(Unit) {
        while (true) {
            battery = SystemUiHelper.getBatteryPercentage(context)
            timeStr = SystemUiHelper.getCurrentTimeFormatted()
            delay(30000) // Update every 30 seconds
        }
    }

    val bgColor = if (isOpaqueBlack) Color(0xFF000000) else Color(0xAA000000)
    val alignModifier = if (isTopRight) {
        Modifier.padding(top = 16.dp, end = 16.dp)
    } else {
        Modifier.padding(top = 16.dp, start = 16.dp)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.then(alignModifier)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Battery
                Icon(
                    Icons.Default.BatteryFull,
                    contentDescription = "电量",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$battery%",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "·",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Time
                Text(
                    text = timeStr,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "·",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Page / Progress
                val progressText = if (isComic) {
                    "${currentPage}/${totalPages.coerceAtLeast(1)}"
                } else {
                    "${(progressPercent * 100).toInt()}%"
                }
                Text(
                    text = progressText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
