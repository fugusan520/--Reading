package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import com.example.data.repository.ReaderPreferences
import com.example.ui.detail.BookDetailScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.reader.comic.ComicReaderScreen
import com.example.ui.reader.novel.NovelReaderScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.shelf.ShelfScreen
import com.example.ui.shelf.ShelfViewModel
import com.example.ui.theme.MyApplicationTheme

sealed interface AppScreen {
    data object Shelf : AppScreen
    data class BookDetails(val book: BookEntity) : AppScreen
    data class NovelReader(val book: BookEntity) : AppScreen
    data class ComicReader(val book: BookEntity) : AppScreen
    data class Settings(val returnTo: AppScreen) : AppScreen
}

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
                    val context = LocalContext.current
                    val preferences = remember { ReaderPreferences(context) }
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Shelf) }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is AppScreen.Shelf -> {
                                ShelfScreen(
                                    viewModel = shelfViewModel,
                                    onOpenBook = { book ->
                                        readerViewModel.loadBook(book.id)
                                        if (preferences.getSkipDetailsPage()) {
                                            currentScreen = if (book.bookType == BookType.NOVEL) {
                                                AppScreen.NovelReader(book)
                                            } else {
                                                AppScreen.ComicReader(book)
                                            }
                                        } else {
                                            currentScreen = AppScreen.BookDetails(book)
                                        }
                                    },
                                    onOpenSettings = {
                                        currentScreen = AppScreen.Settings(returnTo = AppScreen.Shelf)
                                    }
                                )
                            }

                            is AppScreen.BookDetails -> {
                                BackHandler {
                                    currentScreen = AppScreen.Shelf
                                }
                                BookDetailScreen(
                                    book = screen.book,
                                    viewModel = readerViewModel,
                                    onBack = { currentScreen = AppScreen.Shelf },
                                    onStartReading = {
                                        currentScreen = if (screen.book.bookType == BookType.NOVEL) {
                                            AppScreen.NovelReader(screen.book)
                                        } else {
                                            AppScreen.ComicReader(screen.book)
                                        }
                                    },
                                    onJumpToChapter = { chapter ->
                                        readerViewModel.jumpToNovelChapter(chapter)
                                        currentScreen = AppScreen.NovelReader(screen.book)
                                    },
                                    onJumpToBookmark = { bookmark ->
                                        readerViewModel.jumpToBookmark(bookmark)
                                        currentScreen = if (screen.book.bookType == BookType.NOVEL) {
                                            AppScreen.NovelReader(screen.book)
                                        } else {
                                            AppScreen.ComicReader(screen.book)
                                        }
                                    }
                                )
                            }

                            is AppScreen.NovelReader -> {
                                NovelReaderScreen(
                                    viewModel = readerViewModel,
                                    onBackToShelf = { currentScreen = AppScreen.Shelf },
                                    onOpenSettings = {
                                        currentScreen = AppScreen.Settings(returnTo = screen)
                                    }
                                )
                            }

                            is AppScreen.ComicReader -> {
                                ComicReaderScreen(
                                    viewModel = readerViewModel,
                                    onBackToShelf = { currentScreen = AppScreen.Shelf },
                                    onOpenSettings = {
                                        currentScreen = AppScreen.Settings(returnTo = screen)
                                    }
                                )
                            }

                            is AppScreen.Settings -> {
                                BackHandler {
                                    currentScreen = screen.returnTo
                                }
                                SettingsScreen(
                                    shelfViewModel = shelfViewModel,
                                    readerViewModel = readerViewModel,
                                    onBack = { currentScreen = screen.returnTo }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
