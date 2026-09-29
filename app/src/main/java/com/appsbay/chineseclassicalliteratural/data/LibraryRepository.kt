package com.appsbay.chineseclassicalliteratural.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Model.BookLibrary

class LibraryRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val favoritesLiveData = MutableLiveData<List<Book>>()

    fun observeFavorites(): LiveData<List<Book>> = favoritesLiveData

    fun refreshFavorites() {
        favoritesLiveData.value = loadFavorites()
    }

    fun getFavorites(): List<Book> = loadFavorites()

    fun isFavorite(book: Book): Boolean = BookLibrary.shared.have(appContext, book)

    fun addFavorite(book: Book) {
        if (!isFavorite(book)) {
            BookLibrary.shared.save(appContext, book)
            refreshFavorites()
        }
    }

    fun removeFavorite(book: Book) {
        if (isFavorite(book)) {
            BookLibrary.shared.remove(appContext, book)
            refreshFavorites()
        }
    }

    fun toggleFavorite(book: Book) {
        if (isFavorite(book)) {
            removeFavorite(book)
        } else {
            addFavorite(book)
        }
    }

    fun reorderFavorites(orderedBooks: List<Book>) {
        BookLibrary.shared.saveOrder(appContext, orderedBooks)
        refreshFavorites()
    }

    private fun loadFavorites(): List<Book> = BookLibrary.shared.books(appContext)

    companion object {
        @Volatile
        private var instance: LibraryRepository? = null

        @JvmStatic
        fun getInstance(context: Context): LibraryRepository {
            return instance ?: synchronized(this) {
                instance ?: LibraryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
