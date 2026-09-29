package com.appsbay.chineseclassicalliteratural.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Model.BookChapter
import com.appsbay.chineseclassicalliteratural.data.BookRepository
import com.appsbay.chineseclassicalliteratural.data.ReadingRepository

class NovelsHubViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(application) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }

    class ChapterFactory(
        private val application: Application,
        private val book: Book
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChapterViewModel::class.java)) {
                return ChapterViewModel(
                    BookRepository.getInstance(application),
                    ReadingRepository.getInstance(application),
                    book
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }

    class ReaderFactory(
        private val application: Application,
        private val book: Book,
        private val bookChapter: BookChapter,
        private val chapterIndex: Int,
        private val totalChapters: Int
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                return ReaderViewModel(
                    ReadingRepository.getInstance(application),
                    BookRepository.getInstance(application),
                    book,
                    bookChapter,
                    chapterIndex,
                    totalChapters
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
