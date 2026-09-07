package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.util.ImageProcessor
import com.example.util.NovelParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("极简阅读", appName)
  }

  @Test
  fun `novel parser detects chapters and paginates`() {
    val rawText = """
      序章 初始之刻
      这是一个宁静的夜晚，微风吹拂。
      
      第一章 启程
      少年背起行囊，迈向了未知的旅途。路途遥远且充满挑战。
      
      第二章 重逢
      在老旧的小镇酒馆里，久违的伙伴再次举杯。
    """.trimIndent()

    val parsed = NovelParser.parseNovel(rawText)
    assertEquals(3, parsed.chapters.size)
    assertEquals("序章 初始之刻", parsed.chapters[0].title)
    assertEquals("第一章 启程", parsed.chapters[1].title)
    assertEquals("第二章 重逢", parsed.chapters[2].title)

    val pages = NovelParser.paginateText(rawText, charsPerPage = 50)
    assertTrue("Should paginate into multiple pages", pages.size >= 2)
  }

  @Test
  fun `image processor double page split`() {
    // A 1000 x 500 image (aspect 2.0 > 1.3) should split into 2 pages of width 500
    val bitmap = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888)
    val pages = ImageProcessor.splitDoublePage(bitmap)
    assertEquals(2, pages.size)
    assertEquals(500, pages[0].width)
    assertEquals(500, pages[1].width)
  }
}

