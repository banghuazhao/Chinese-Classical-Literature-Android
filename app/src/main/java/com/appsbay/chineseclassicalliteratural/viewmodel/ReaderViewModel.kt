package com.appsbay.chineseclassicalliteratural.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Model.BookChapter
import com.appsbay.chineseclassicalliteratural.data.BookRepository
import com.appsbay.chineseclassicalliteratural.data.ReadingRepository
import com.appsbay.chineseclassicalliteratural.data.Resource
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val readingRepository: ReadingRepository,
    private val bookRepository: BookRepository,
    private val book: Book,
    initialChapter: BookChapter,
    initialChapterIndex: Int,
    initialTotalChapters: Int
) : ViewModel() {

    private var bookChapter: BookChapter = initialChapter
    private var chapterIndex: Int = initialChapterIndex
    private var totalChapters: Int = initialTotalChapters
    private var chapters: List<BookChapter> = emptyList()

    private val _textSize = MutableLiveData(readingRepository.getTextSize())
    val textSize: LiveData<Int> = _textSize

    private val _canGoPrevious = MutableLiveData(false)
    val canGoPrevious: LiveData<Boolean> = _canGoPrevious

    private val _canGoNext = MutableLiveData(false)
    val canGoNext: LiveData<Boolean> = _canGoNext

    init {
        updateNavState()
        loadChapters()
    }

    fun getChapterIndex(): Int = chapterIndex

    fun getSavedScrollFraction(): Float {
        val savedIndex = readingRepository.getChapterIndex(book)
        return if (savedIndex == chapterIndex) {
            readingRepository.getScrollFraction(book)
        } else {
            0f
        }
    }

    fun setTextSize(size: Int) {
        readingRepository.setTextSize(size)
        _textSize.value = size
    }

    fun saveReadingProgress(scrollFraction: Float) {
        val index = if (chapterIndex >= 0) {
            chapterIndex
        } else {
            readingRepository.getChapterIndex(book).coerceAtLeast(0)
        }
        val total = if (totalChapters > 0) {
            totalChapters
        } else {
            readingRepository.getTotalChapters(book)
        }
        readingRepository.saveSession(
            book = book,
            chapterIndex = index,
            chapterNumberName = bookChapter.chapterNumberName,
            chapterName = bookChapter.chapterName,
            totalChapters = total,
            scrollFraction = scrollFraction
        )
    }

    fun goToPreviousChapter(): BookChapter? = goToChapter(chapterIndex - 1)

    fun goToNextChapter(): BookChapter? = goToChapter(chapterIndex + 1)

    private fun loadChapters() {
        viewModelScope.launch {
            when (val result = bookRepository.loadChapters(book)) {
                is Resource.Success -> {
                    chapters = result.data
                    totalChapters = chapters.size
                    if (chapterIndex < 0 || chapterIndex >= chapters.size) {
                        val matched = chapters.indexOfFirst {
                            it.chapterNumberName == bookChapter.chapterNumberName &&
                                it.chapterName == bookChapter.chapterName
                        }
                        if (matched >= 0) {
                            chapterIndex = matched
                        }
                    }
                    updateNavState()
                }
                else -> updateNavState()
            }
        }
    }

    private fun goToChapter(index: Int): BookChapter? {
        if (index < 0 || index >= chapters.size) {
            return null
        }
        chapterIndex = index
        bookChapter = chapters[index]
        totalChapters = chapters.size
        readingRepository.markChapterOpened(
            book = book,
            chapterIndex = index,
            chapterNumberName = bookChapter.chapterNumberName,
            chapterName = bookChapter.chapterName,
            totalChapters = totalChapters
        )
        updateNavState()
        return bookChapter
    }

    private fun updateNavState() {
        val hasList = chapters.isNotEmpty() && chapterIndex >= 0
        _canGoPrevious.value = hasList && chapterIndex > 0
        _canGoNext.value = hasList && chapterIndex < chapters.size - 1
    }
}
