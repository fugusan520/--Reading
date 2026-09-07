package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import com.example.ui.reader.ReaderViewModel
import com.example.ui.reader.comic.ComicReaderScreen
import com.example.ui.reader.novel.NovelReaderScreen
import com.example.ui.shelf.ShelfScreen
import com.example.ui.shelf.ShelfViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val shelfViewModel: ShelfViewModel by viewModels()
  private val readerViewModel: ReaderViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF121212)
        ) {
          var activeBook by remember { mutableStateOf<BookEntity?>(null) }

          AnimatedContent(
            targetState = activeBook,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
          ) { currentBook ->
            if (currentBook == null) {
              ShelfScreen(
                viewModel = shelfViewModel,
                onOpenBook = { book ->
                  readerViewModel.loadBook(book.id)
                  activeBook = book
                }
              )
            } else {
              when (currentBook.bookType) {
                BookType.NOVEL -> {
                  NovelReaderScreen(
                    viewModel = readerViewModel,
                    onBackToShelf = { activeBook = null }
                  )
                }
                BookType.COMIC -> {
                  ComicReaderScreen(
                    viewModel = readerViewModel,
                    onBackToShelf = { activeBook = null }
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

