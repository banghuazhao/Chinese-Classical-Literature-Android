package com.appsbay.chineseclassicalliteratural.data

import android.content.Context
import com.appsbay.chineseclassicalliteratural.Model.Book
import com.appsbay.chineseclassicalliteratural.Model.BookCategory
import com.appsbay.chineseclassicalliteratural.Model.BookCategoryStore
import com.appsbay.chineseclassicalliteratural.Model.BookChapter
import com.appsbay.chineseclassicalliteratural.Model.BookStore
import com.appsbay.chineseclassicalliteratural.Tools.NetworkState
import com.appsbay.chineseclassicalliteratural.Tools.OfflineBookManager
import com.appsbay.chineseclassicalliteratural.Tools.ReadingProgressHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class BookRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    fun getTopLevelBooks(): List<Book> = BookStore.shared.getBooks(appContext)

    fun getAllBooks(): List<Book> = BookStore.shared.getAllBooks(appContext)

    fun getCategories(): List<BookCategory> = BookCategoryStore.shared.getCategories(appContext)

    fun getBooksForCollection(book: Book): List<Book> =
        BookStore.shared.getBooksForCollection(book)

    /** True when the book can be opened without touching the network. */
    fun isAvailableOffline(book: Book): Boolean =
        OfflineBookManager.isAvailableOffline(appContext, book)

    suspend fun loadChapters(book: Book): Resource<List<BookChapter>> = withContext(Dispatchers.IO) {
        val json = if (!book.isOnline) {
            loadBundledJson(book)
                ?: return@withContext Resource.Error(BookLoadError.CONTENT_UNAVAILABLE)
        } else {
            OfflineBookManager.readOfflineJson(appContext, book)
                ?: when (val downloaded = downloadJson(book)) {
                    is Resource.Success -> downloaded.data.also { remoteJson ->
                        OfflineBookManager.ensureOfflineCopy(appContext, book, remoteJson)
                    }
                    is Resource.Error -> return@withContext downloaded
                    else -> return@withContext Resource.Error(BookLoadError.DOWNLOAD_FAILED)
                }
        }

        val chapters = try {
            parseChapters(json, book.name)
        } catch (e: JSONException) {
            // A truncated or malformed copy is useless; drop it so a retry re-downloads.
            OfflineBookManager.deleteDownload(appContext, book)
            return@withContext Resource.Error(BookLoadError.CONTENT_UNAVAILABLE, e.message)
        }

        if (chapters.isEmpty()) {
            Resource.Error(BookLoadError.CONTENT_UNAVAILABLE)
        } else {
            ReadingProgressHelper.saveTotalChapters(appContext, book, chapters.size)
            Resource.Success(chapters)
        }
    }

    private fun loadBundledJson(book: Book): String? {
        return try {
            appContext.assets.open("files/${book.fileName}.json").use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun downloadJson(book: Book): Resource<String> {
        if (!NetworkState.isConnected(appContext)) {
            return Resource.Error(BookLoadError.NO_CONNECTION)
        }
        val url = OfflineBookManager.cdnUrl(book)
        if (url.isBlank()) return Resource.Error(BookLoadError.CONTENT_UNAVAILABLE)
        val request = Request.Builder().url(url).build()
        return try {
            okHttp.newCall(request).execute().use { response ->
                val body = response.body
                when {
                    response.code == 404 || response.code == 403 ->
                        Resource.Error(BookLoadError.CONTENT_UNAVAILABLE, "HTTP ${response.code}")
                    !response.isSuccessful || body == null ->
                        Resource.Error(BookLoadError.DOWNLOAD_FAILED, "HTTP ${response.code}")
                    else -> Resource.Success(body.string())
                }
            }
        } catch (e: IOException) {
            // A dropped connection mid-download looks the same as never having one.
            val reason = if (NetworkState.isConnected(appContext)) {
                BookLoadError.DOWNLOAD_FAILED
            } else {
                BookLoadError.NO_CONNECTION
            }
            Resource.Error(reason, e.message)
        }
    }

    private fun parseChapters(jsonString: String, bookName: String): List<BookChapter> {
        val obj = JSONObject(jsonString)
        val array = when {
            obj.has(bookName) -> obj.getJSONArray(bookName)
            obj.keys().hasNext() -> obj.getJSONArray(obj.keys().next())
            else -> return emptyList()
        }
        val chapters = ArrayList<BookChapter>(array.length())
        for (i in 0 until array.length()) {
            val chapter = array.getJSONObject(i)
            chapters.add(
                BookChapter(
                    chapter.optString("章节", chapter.optString("章節")),
                    chapter.optString("章节名称", chapter.optString("章節名稱")),
                    chapter.optString("章节内容", chapter.optString("章節內容"))
                )
            )
        }
        return chapters
    }

    companion object {
        @Volatile
        private var instance: BookRepository? = null

        @JvmStatic
        fun getInstance(context: Context): BookRepository {
            return instance ?: synchronized(this) {
                instance ?: BookRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
