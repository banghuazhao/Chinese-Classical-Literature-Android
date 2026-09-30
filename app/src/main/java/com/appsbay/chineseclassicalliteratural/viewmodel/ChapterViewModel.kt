package com.appsbay.chineseclassicalliteratural.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Model.BookChapter
import com.appsbay.chineseclassicalliteratural.data.BookLoadError
import com.appsbay.chineseclassicalliteratural.data.BookRepository
import com.appsbay.chineseclassicalliteratural.data.ReadingRepository
import com.appsbay.chineseclassicalliteratural.data.Resource
import kotlinx.coroutines.launch

class ChapterViewModel(
    private val bookRepository: BookRepository,
    private val readingRepository: ReadingRepository,
    private val book: Book
) : ViewModel() {

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    /** True while the book's text is being fetched from the CDN for the first time. */
    private val _downloading = MutableLiveData(false)
    val downloading: LiveData<Boolean> = _downloading

    private val _chapterList = MutableLiveData<List<BookChapter>>(emptyList())
    val chapterList: LiveData<List<BookChapter>> = _chapterList

    private val _loadError = MutableLiveData<BookLoadError?>()
    val loadError: LiveData<BookLoadError?> = _loadError

    init {
        loadChapters()
    }

    fun loadChapters() {
        _loading.value = true
        _loadError.value = null
        _downloading.value = !bookRepository.isAvailableOffline(book)
        viewModelScope.launch {
            when (val result = bookRepository.loadChapters(book)) {
                is Resource.Loading -> _loading.value = true
                is Resource.Success -> {
                    _loading.value = false
                    _downloading.value = false
                    _chapterList.value = result.data
                }
                is Resource.Error -> {
                    _loading.value = false
                    _downloading.value = false
                    _loadError.value = result.reason
                }
            }
        }
    }

    fun getContinueChapterIndex(): Int = readingRepository.getContinueChapterIndex()

    fun getChapterBookmarkIndex(): Int = readingRepository.getChapterIndex(book)

    fun openChapter(chapter: BookChapter, chapterIndex: Int, totalChapters: Int) {
        readingRepository.markChapterOpened(
            book = book,
            chapterIndex = chapterIndex,
            chapterNumberName = chapter.chapterNumberName,
            chapterName = chapter.chapterName,
            totalChapters = totalChapters
        )
    }
}
