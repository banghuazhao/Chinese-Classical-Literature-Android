package com.appsbay.chineseclassicalliteratural.Model

import android.content.Context
import com.appsbay.chineseclassicalliteratural.Tools.TinyDB

class BookLibrary {

    fun have(context: Context, book: Book): Boolean {
        val tinydb = TinyDB(context)
        val books = tinydb.getListString(BOOK_LIBRARY_KEY)
        return books.contains(book.name)
    }

    fun save(context: Context, book: Book) {
        val tinydb = TinyDB(context)
        val books = tinydb.getListString(BOOK_LIBRARY_KEY)
        books.add(book.name)
        tinydb.putListString(BOOK_LIBRARY_KEY, books)
    }

    fun remove(context: Context, book: Book) {
        val tinydb = TinyDB(context)
        val books = tinydb.getListString(BOOK_LIBRARY_KEY)
        if (have(context, book)) {
            books.remove(book.name)
            tinydb.putListString(BOOK_LIBRARY_KEY, books)
        }
    }

    fun books(context: Context): ArrayList<Book> {
        val result = ArrayList<Book>()
        val tinydb = TinyDB(context)
        val bookStrings = tinydb.getListString(BOOK_LIBRARY_KEY)

        for (bookString in bookStrings) {
            for (book in BookStore.shared.getAllBooks(context)) {
                if (book.name == bookString) {
                    result.add(book)
                    break
                }
            }
        }

        return result
    }

    fun saveOrder(context: Context, orderedBooks: List<Book>) {
        val tinydb = TinyDB(context)
        val names = ArrayList<String>(orderedBooks.size)
        for (book in orderedBooks) {
            names.add(book.name)
        }
        tinydb.putListString(BOOK_LIBRARY_KEY, names)
    }

    companion object {
        const val BOOK_LIBRARY_KEY = "BookLibrary"

        @JvmField
        val shared = BookLibrary()
    }
}
