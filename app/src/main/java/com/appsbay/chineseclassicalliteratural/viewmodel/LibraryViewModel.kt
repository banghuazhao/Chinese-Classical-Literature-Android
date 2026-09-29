package com.appsbay.chineseclassicalliteratural.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy
import com.appsbay.chineseclassicalliteratural.Tools.SearchHistory
import com.appsbay.chineseclassicalliteratural.data.LibraryRepository

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val libraryRepository = LibraryRepository.getInstance(application)

    private val _searchQuery = MutableLiveData("")

    private val favoritesSource: LiveData<List<Book>> = libraryRepository.observeFavorites()

    val searchQuery: LiveData<String> = _searchQuery

    val books: LiveData<List<Book>> = MediatorLiveData<List<Book>>().apply {
        addSource(favoritesSource) { updateFilteredBooks() }
        addSource(_searchQuery) { updateFilteredBooks() }
    }

    init {
        refreshLibrary()
    }

    fun refreshLibrary() {
        libraryRepository.refreshFavorites()
    }

    fun setSearchQuery(query: String) {
        val next = query.trim()
        if (_searchQuery.value == next) return
        _searchQuery.value = next
    }

    fun toggleFavorite(book: Book) {
        libraryRepository.toggleFavorite(book)
    }

    fun isFavorite(book: Book): Boolean = libraryRepository.isFavorite(book)

    fun isSearchActive(): Boolean = !_searchQuery.value?.trim().isNullOrEmpty()

    fun popularAuthors(limit: Int = 8): List<String> {
        return SearchHistory.popularAuthors(favoritesSource.value.orEmpty(), limit)
    }

    fun reorderFavorites(orderedBooks: List<Book>) {
        if (isSearchActive()) {
            return
        }
        libraryRepository.reorderFavorites(orderedBooks)
    }

    private fun MediatorLiveData<List<Book>>.updateFilteredBooks() {
        val query = _searchQuery.value?.trim().orEmpty()
        val favorites = favoritesSource.value.orEmpty()
        value = if (query.isEmpty()) {
            favorites
        } else {
            favorites.filter { book -> LiteraryCopy.shared.matchesSearch(book, query) }
        }
    }
}
