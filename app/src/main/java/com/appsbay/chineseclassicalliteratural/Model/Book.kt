package com.appsbay.chineseclassicalliteratural.Model

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.gson.JsonObject

class Book : Parcelable {
    var id: String = ""
    var name: String = ""
    var author: String = ""
    var fileName: String = ""
    var bookCover: String = ""
    var bookType: BookType = BookType.other
    var isOnline: Boolean = false
    var isCollection: Boolean = false
    var parent: String? = null

    constructor()

    constructor(json: JsonObject) {
        name = json.get("name").asString
        id = json.get("id")?.asString ?: name
        author = json.get("author").asString
        bookType = try {
            BookType.valueOf(json.get("bookType").asString)
        } catch (_: IllegalArgumentException) {
            BookType.other
        }
        bookCover = json.get("bookCover").asString
        fileName = json.get("fileName").asString
        if (json.get("isOnline") != null) {
            isOnline = json.get("isOnline").asBoolean
            Log.d("myTag", fileName)
            Log.d("myTag", isOnline.toString())
        }
        if (json.get("isCollection") != null) {
            isCollection = json.get("isCollection").asBoolean
        }
        if (json.get("parent") != null) {
            parent = json.get("parent").asString
        }
    }

    private constructor(parcel: Parcel) {
        name = parcel.readString() ?: ""
        author = parcel.readString() ?: ""
        fileName = parcel.readString() ?: ""
        bookCover = parcel.readString() ?: ""
        bookType = parcel.readSerializable() as BookType
        isOnline = parcel.readByte().toInt() != 0
        isCollection = parcel.readByte().toInt() != 0
        parent = parcel.readString()
        id = if (parcel.dataAvail() > 0) parcel.readString() ?: name else name
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(author)
        dest.writeString(fileName)
        dest.writeString(bookCover)
        dest.writeSerializable(bookType)
        dest.writeByte(if (isOnline) 1 else 0)
        dest.writeByte(if (isCollection) 1 else 0)
        dest.writeString(parent)
        dest.writeString(id)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Book> = object : Parcelable.Creator<Book> {
            override fun createFromParcel(parcel: Parcel): Book = Book(parcel)
            override fun newArray(size: Int): Array<Book?> = arrayOfNulls(size)
        }
    }
}
