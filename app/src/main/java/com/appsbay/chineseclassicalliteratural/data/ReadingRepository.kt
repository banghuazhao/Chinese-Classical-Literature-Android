package com.appsbay.chineseclassicalliteratural.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Tools.ReadingProgressHelper

class ReadingRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val continueReadingLiveData = MutableLiveData<Book?>()

    fun observeContinueReading(): LiveData<Book?> = continueReadingLiveData

    fun refreshContinueReading() {
        continueReadingLiveData.value = ReadingProgressHelper.getContinueReadingBook(appContext)
    }

    fun getContinueReadingBook(): Book? =
        ReadingProgressHelper.getContinueReadingBook(appContext)

    fun getContinueChapterIndex(): Int =
        ReadingProgressHelper.getContinueChapterIndex(appContext)

    fun getContinueChapterLabel(): String? =
        ReadingProgressHelper.getContinueChapterLabel(appContext)

    fun getProgress(book: Book): ReadingProgress? {
        val chapterIndex = ReadingProgressHelper.getChapterIndex(appContext, book.name)
        if (chapterIndex < 0) {
            return null
        }
        val total = ReadingProgressHelper.getTotalChapters(appContext, book.name)
        val percent = ReadingProgressHelper.getProgressPercent(appContext, book)
        val label = ReadingProgressHelper.getProgressLabel(appContext, book)
        return ReadingProgress(
            percent = maxOf(percent, 0),
            chapterIndex = chapterIndex,
            totalChapters = total,
            chapterLabel = label
        )
    }

    fun getChapterIndex(bookName: String): Int =
        ReadingProgressHelper.getChapterIndex(appContext, bookName)

    fun getTotalChapters(bookName: String): Int =
        ReadingProgressHelper.getTotalChapters(appContext, bookName)

    fun saveSession(
        book: Book,
        chapterIndex: Int,
        chapterNumberName: String,
        chapterName: String,
        totalChapters: Int,
        scrollFraction: Float
    ) {
        ReadingProgressHelper.saveSession(
            appContext,
            book,
            chapterIndex,
            chapterNumberName,
            chapterName,
            totalChapters,
            scrollFraction
        )
        refreshContinueReading()
    }

    fun markChapterOpened(
        book: Book,
        chapterIndex: Int,
        chapterNumberName: String,
        chapterName: String,
        totalChapters: Int
    ) {
        ReadingProgressHelper.markChapterOpened(
            appContext,
            book,
            chapterIndex,
            chapterNumberName,
            chapterName,
            totalChapters
        )
        refreshContinueReading()
    }

    fun getScrollFraction(bookName: String): Float =
        ReadingProgressHelper.getScrollFraction(appContext, bookName)

    fun getTextSize(): Int {
        return appContext.getSharedPreferences("Font Preference", Context.MODE_PRIVATE)
            .getInt("textSize", 18)
    }

    fun setTextSize(size: Int) {
        appContext.getSharedPreferences("Font Preference", Context.MODE_PRIVATE)
            .edit()
            .putInt("textSize", size)
            .apply()
    }

    companion object {
        @Volatile
        private var instance: ReadingRepository? = null

        @JvmStatic
        fun getInstance(context: Context): ReadingRepository {
            return instance ?: synchronized(this) {
                instance ?: ReadingRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
