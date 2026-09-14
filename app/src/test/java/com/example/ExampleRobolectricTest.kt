package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.util.ImageProcessor
import com.example.util.NovelParser
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import com.example.data.model.ReadingProgressEntity
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("梦游", appName)
  }

  @Test
  fun `novel parser detects chapters and paginates with offsets`() {
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

    val paginated = NovelParser.paginateWithOffsets(rawText, charsPerPage = 50)
    assertTrue("Should paginate into multiple pages", paginated.pages.size >= 2)
    assertEquals(paginated.pages.size, paginated.pageStartOffsets.size)

    // Test resume calculation by character offset
    val lastCharOffset = paginated.pageStartOffsets[1].toLong()
    val resumedPage = NovelParser.findPageForCharOffset(paginated.pageStartOffsets, lastCharOffset)
    assertEquals(1, resumedPage)
  }

  @Test
  fun `reading progress dao saves and resumes correctly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val bookDao = db.bookDao()
    val progressDao = db.readingProgressDao()

    val bookId = bookDao.insertBook(
        BookEntity(
            title = "测试书籍",
            uriString = "content://test/book.txt",
            bookType = BookType.NOVEL
        )
    )

    // Save reading progress
    progressDao.saveProgress(
        ReadingProgressEntity(
            bookId = bookId,
            bookType = BookType.NOVEL,
            currentPage = 5,
            totalPages = 20,
            progressPercent = 0.25f,
            characterOffset = 3250L,
            chapterTitle = "第一章 启程"
        )
    )

    val saved = progressDao.getProgressForBook(bookId)
    assertNotNull(saved)
    assertEquals(5, saved?.currentPage)
    assertEquals(20, saved?.totalPages)
    assertEquals(0.25f, saved?.progressPercent ?: 0f, 0.001f)
    assertEquals(3250L, saved?.characterOffset)
    assertEquals("第一章 启程", saved?.chapterTitle)

    db.close()
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

