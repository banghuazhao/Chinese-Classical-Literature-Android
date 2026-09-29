package com.appsbay.chineseclassicalliteratural.Model

import android.content.Context

class BookCategoryStore {
    var cachedCategories: ArrayList<BookCategory>? = null

    fun getCategories(context: Context): ArrayList<BookCategory> {
        val categories = ArrayList<BookCategory>()
        for (name in BookGenres.ALL_NAMES) {
            val books = ArrayList(BookStore.shared.getBooks(context).filter {
                BookGenres.categoryNameFor(it.bookType) == name
            })
            categories.add(BookCategory(name, books))
        }
        return categories
    }

    fun setCategories(categories: ArrayList<BookCategory>) {
        cachedCategories = categories
    }

    companion object {
        @JvmField val shared = BookCategoryStore()
    }
}
