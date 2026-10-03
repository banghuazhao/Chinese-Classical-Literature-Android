package com.appsbay.chineseclassicalliteratural.Model

import android.content.Context
import android.os.Build
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import java.io.IOException
import java.util.Locale

class BookStore {
    private val adultOnlyIds = setOf("水浒传", "封神演义", "隋唐演义")
    private val simplified = ArrayList<Book>()
    private val traditional = ArrayList<Book>()
    private val missingTraditionalChapters = HashMap<String, List<Int>>()

    fun getBooks(context: Context): ArrayList<Book> =
        ArrayList((if (usesTraditional(context)) traditional else simplified).filter {
            isAvailable(context, it)
        })

    fun getAllBooks(context: Context): ArrayList<Book> =
        ArrayList<Book>(simplified.size + traditional.size).apply {
            addAll(getBooks(context))
            addAll((if (usesTraditional(context)) simplified else traditional).filter {
                isAvailable(context, it)
            })
        }

    fun isAvailable(context: Context, book: Book): Boolean = isAvailableId(context, book.id)

    fun isAvailableId(context: Context, id: String): Boolean =
        (simplified.any { it.id == id } || traditional.any { it.id == id }) &&
            (AgeGate.isAdult(context) || id !in adultOnlyIds)

    fun getBooksForCollection(book: Book): ArrayList<Book> =
        ArrayList(getAllBooksForLoadedCatalog().filter { it.parent == book.name })

    private fun getAllBooksForLoadedCatalog(): List<Book> = simplified + traditional

    fun fetchFromLocal(context: Context) {
        if (simplified.isNotEmpty() || traditional.isNotEmpty()) return
        loadCatalog(context, "bookInfo-simplified.json", simplified)
        loadCatalog(context, "bookInfo-traditional.json", traditional)
        loadChapterIndexMap(context)
    }

    fun idForName(name: String): String =
        (simplified + traditional).firstOrNull { it.name == name || it.id == name }?.id ?: name

    fun namesForId(id: String): List<String> =
        (simplified + traditional).filter { it.id == id }.map { it.name }

    fun variantsForId(id: String): List<Book> =
        (simplified + traditional).filter { it.id == id }

    fun bookForId(context: Context, id: String): Book? =
        getBooks(context).firstOrNull { it.id == id }

    fun bookForName(context: Context, name: String): Book? =
        getAllBooks(context).firstOrNull { it.name == name }

    fun isTraditional(book: Book): Boolean = book.bookType.name.endsWith("_Fan")

    /** Map a saved chapter to the same work in the other script. */
    fun chapterIndexFor(book: Book, savedIndex: Int, savedTraditional: Boolean): Int {
        if (savedIndex < 0 || savedTraditional == isTraditional(book)) return savedIndex
        val omitted = missingTraditionalChapters[book.id] ?: return savedIndex
        if (isTraditional(book)) {
            // If this exact chapter is absent, resume at its preceding chapter.
            return savedIndex - omitted.count { it <= savedIndex }
        }
        var result = savedIndex
        for (missing in omitted.sorted()) {
            if (result >= missing) result++
        }
        return result
    }

    fun chapterCountFor(book: Book, savedTotal: Int, savedTraditional: Boolean): Int {
        if (savedTraditional == isTraditional(book)) return savedTotal
        val omitted = missingTraditionalChapters[book.id]?.size ?: 0
        return savedTotal + if (isTraditional(book)) -omitted else omitted
    }

    private fun loadChapterIndexMap(context: Context) {
        try {
            context.assets.open("chapter-index-map.json").bufferedReader(Charsets.UTF_8).use { reader ->
                val map: JsonObject = JsonParser.parseReader(reader).asJsonObject
                for ((id, value) in map.entrySet()) {
                    missingTraditionalChapters[id] = value.asJsonArray.map { it.asInt }
                }
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
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
