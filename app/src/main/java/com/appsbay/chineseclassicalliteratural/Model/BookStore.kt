package com.appsbay.chineseclassicalliteratural.Model

import android.content.Context
import android.os.Build
import com.google.gson.JsonParser
import java.io.IOException
import java.util.Locale

class BookStore {
    private val simplified = ArrayList<Book>()
    private val traditional = ArrayList<Book>()

    fun getBooks(context: Context): ArrayList<Book> =
        if (usesTraditional(context)) traditional else simplified

    fun getAllBooks(context: Context): ArrayList<Book> =
        ArrayList<Book>(simplified.size + traditional.size).apply {
            addAll(getBooks(context))
            addAll(if (usesTraditional(context)) simplified else traditional)
        }

    fun getBooksForCollection(book: Book): ArrayList<Book> =
        ArrayList(getAllBooksForLoadedCatalog().filter { it.parent == book.name })

    private fun getAllBooksForLoadedCatalog(): List<Book> = simplified + traditional

    fun fetchFromLocal(context: Context) {
        if (simplified.isNotEmpty() || traditional.isNotEmpty()) return
        loadCatalog(context, "bookInfo-simplified.json", simplified)
        loadCatalog(context, "bookInfo-traditional.json", traditional)
    }

    private fun loadCatalog(context: Context, fileName: String, into: ArrayList<Book>) {
        try {
            context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { reader ->
                for (element in JsonParser.parseReader(reader).asJsonArray) {
                    val book = Book(element.asJsonObject)
                    if (book.parent == null) into.add(book)
                }
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
    }

    fun usesTraditional(context: Context): Boolean {
        val prefs = context.getSharedPreferences("Language Preference", Context.MODE_PRIVATE)
        if (prefs.contains("language")) return prefs.getInt("language", 0) == 1
        val configuration = context.resources.configuration
        @Suppress("DEPRECATION")
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0] ?: Locale.getDefault()
        } else {
            configuration.locale ?: Locale.getDefault()
        }
        return locale.country.equals("TW", ignoreCase = true) ||
            locale.country.equals("HK", ignoreCase = true) ||
            locale.script.equals("Hant", ignoreCase = true)
    }

    fun setTraditional(context: Context, traditionalScript: Boolean) {
        context.getSharedPreferences("Language Preference", Context.MODE_PRIVATE)
            .edit().putInt("language", if (traditionalScript) 1 else 0).apply()
    }

    companion object {
        @JvmField val shared = BookStore()
    }
}
