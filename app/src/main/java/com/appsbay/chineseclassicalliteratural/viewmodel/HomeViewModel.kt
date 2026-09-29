package com.appsbay.chineseclassicalliteratural.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Model.BookCategory
import com.appsbay.chineseclassicalliteratural.Model.BookGenres
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy
import com.appsbay.chineseclassicalliteratural.data.BookRepository
import com.appsbay.chineseclassicalliteratural.data.LibraryRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository.getInstance(application)
    private val libraryRepository = LibraryRepository.getInstance(application)

    private val _searchQuery = MutableLiveData("")
    private val selectedCategory = MutableLiveData<String?>(null)

    private val categoriesLiveData = MutableLiveData<List<BookCategory>>()
    private val allBooksLiveData = MutableLiveData<List<Book>>()

    val searchQuery: LiveData<String> = _searchQuery
    val selectedCategoryName: LiveData<String?> = selectedCategory

    val categories: LiveData<List<BookCategory>> = MediatorLiveData<List<BookCategory>>().apply {
        addSource(categoriesLiveData) { updateFilteredCategories() }
        addSource(selectedCategory) { updateFilteredCategories() }
        addSource(_searchQuery) { updateFilteredCategories() }
    }

    val allBooks: LiveData<List<Book>> = MediatorLiveData<List<Book>>().apply {
        addSource(allBooksLiveData) { updateFilteredBooks() }
        addSource(_searchQuery) { updateFilteredBooks() }
        addSource(selectedCategory) { updateFilteredBooks() }
    }

    init {
        loadHomeData()
        libraryRepository.refreshFavorites()
    }

    fun loadHomeData() {
        categoriesLiveData.value = bookRepository.getCategories()
        allBooksLiveData.value = bookRepository.getTopLevelBooks()
    }

    fun setSearchQuery(query: String) {
        val next = query.trim()
        if (_searchQuery.value == next) return
        _searchQuery.value = next
    }

    fun setSelectedCategory(categoryName: String?) {
        val next = categoryName?.takeIf { it.isNotBlank() }
        if (selectedCategory.value == next) return
        selectedCategory.value = next
    }

    fun isSearchActive(): Boolean = !_searchQuery.value?.trim().isNullOrEmpty()

    fun isFavorite(book: Book): Boolean = libraryRepository.isFavorite(book)

    fun popularAuthors(limit: Int = 8): List<String> {
        return com.appsbay.chineseclassicalliteratural.Tools.SearchHistory.popularAuthors(
            allBooksLiveData.value.orEmpty(),
            limit
        )
    }

    private fun MediatorLiveData<List<BookCategory>>.updateFilteredCategories() {
        val all = categoriesLiveData.value.orEmpty()
        val selected = selectedCategory.value
        val query = _searchQuery.value?.trim().orEmpty()
        val scoped = if (selected.isNullOrEmpty()) {
            all
        } else {
            all.filter { it.categoryName == selected }
        }
        value = if (query.isEmpty()) {
            scoped
        } else {
            scoped.mapNotNull { category ->
                val matched = ArrayList(category.books.filter { book ->
                    LiteraryCopy.shared.matchesSearch(book, query)
                })
                if (matched.isEmpty()) {
                    null
                } else {
                    BookCategory(category.categoryName, matched)
                }
            }
        }
    }

    private fun MediatorLiveData<List<Book>>.updateFilteredBooks() {
        val query = _searchQuery.value?.trim().orEmpty()
        val selected = selectedCategory.value
        val books = allBooksLiveData.value.orEmpty()
            .filter { BookGenres.matches(it, selected) }
        value = if (query.isEmpty()) {
            books
        } else {
            books.filter { book -> LiteraryCopy.shared.matchesSearch(book, query) }
        }
    }
}
