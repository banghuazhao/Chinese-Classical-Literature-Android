package com.appsbay.chineseclassicalliteratural.Model

import android.content.Context
import com.appsbay.chineseclassicalliteratural.R

object BookGenres {
    const val ALL = ""
    const val CLASSICS = "classics"
    const val PHILOSOPHY = "philosophy"
    const val FICTION = "fiction"
    const val OTHER = "other"

    @JvmField val ALL_NAMES: List<String> = listOf(CLASSICS, PHILOSOPHY, FICTION, OTHER)

    @JvmStatic fun shortLabelRes(categoryName: String): Int = when (categoryName) {
        CLASSICS -> R.string.genre_classics
        PHILOSOPHY -> R.string.genre_philosophy
        FICTION -> R.string.genre_fiction
        OTHER -> R.string.genre_other
        else -> R.string.genre_all
    }

    @JvmStatic fun displayName(context: Context, categoryName: String?): String =
        context.getString(shortLabelRes(categoryName ?: ALL))

    @JvmStatic fun matches(book: Book, categoryName: String?): Boolean =
        categoryName.isNullOrEmpty() || categoryNameFor(book.bookType) == categoryName

    @JvmStatic fun categoryNameFor(type: BookType): String = when (type) {
        BookType.siShuWuJing, BookType.siShuWuJing_Fan -> CLASSICS
        BookType.zhuZiBaiJia, BookType.zhuZiBaiJia_Fan -> PHILOSOPHY
        BookType.novel, BookType.novel_Fan -> FICTION
        BookType.other, BookType.other_Fan -> OTHER
    }
}
