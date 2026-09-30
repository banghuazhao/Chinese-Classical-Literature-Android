package com.appsbay.chineseclassicalliteratural.Model

import android.content.Context
import com.appsbay.chineseclassicalliteratural.Tools.TinyDB

class BookLibrary {

    fun have(context: Context, book: Book): Boolean {
        return savedIds(context).contains(book.id)
    }

    fun save(context: Context, book: Book) {
        val tinydb = TinyDB(context)
        val ids = savedIds(context)
        if (!ids.contains(book.id)) {
            ids.add(book.id)
            tinydb.putListString(BOOK_LIBRARY_KEY, ids)
        }
    }

    fun remove(context: Context, book: Book) {
        val tinydb = TinyDB(context)
        val ids = savedIds(context)
        if (ids.remove(book.id)) {
            tinydb.putListString(BOOK_LIBRARY_KEY, ids)
        }
    }

    fun books(context: Context): ArrayList<Book> {
        return ArrayList(savedIds(context).mapNotNull { BookStore.shared.bookForId(context, it) })
    }

    fun saveOrder(context: Context, orderedBooks: List<Book>) {
        val tinydb = TinyDB(context)
        val ids = ArrayList<String>(orderedBooks.size)
        for (book in orderedBooks) {
            if (!ids.contains(book.id)) ids.add(book.id)
        }
        tinydb.putListString(BOOK_LIBRARY_KEY, ids)
    }

    /** Older versions stored displayed titles. Convert them once without losing order. */
    private fun savedIds(context: Context): ArrayList<String> {
        val tinydb = TinyDB(context)
        val stored = tinydb.getListString(BOOK_LIBRARY_KEY)
        val ids = ArrayList<String>()
        for (value in stored) {
            val id = BookStore.shared.idForName(value)
            if (!ids.contains(id)) ids.add(id)
        }
        if (stored != ids) tinydb.putListString(BOOK_LIBRARY_KEY, ids)
        return ids
    }

    companion object {
        const val BOOK_LIBRARY_KEY = "BookLibrary"

        @JvmField
        val shared = BookLibrary()
    }
}
