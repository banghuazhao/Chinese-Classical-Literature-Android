package com.appsbay.chineseclassicalliteratural.Model

import android.os.Parcel
import android.os.Parcelable

class BookChapter : Parcelable {
    var chapterNumber: Int = 0
    var chapterNumberName: String = ""
    var chapterName: String = ""
    var text: String = ""

    constructor()

    constructor(chapterNumberName: String, chapterName: String, chapterText: String) {
        this.chapterNumberName = chapterNumberName
        this.chapterName = chapterName
        this.text = chapterText
    }

    private constructor(parcel: Parcel) {
        chapterNumber = parcel.readInt()
        chapterNumberName = parcel.readString() ?: ""
        chapterName = parcel.readString() ?: ""
        text = parcel.readString() ?: ""
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(chapterNumber)
        dest.writeString(chapterNumberName)
        dest.writeString(chapterName)
        dest.writeString(text)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BookChapter> = object : Parcelable.Creator<BookChapter> {
            override fun createFromParcel(parcel: Parcel): BookChapter = BookChapter(parcel)
            override fun newArray(size: Int): Array<BookChapter?> = arrayOfNulls(size)
        }
    }
}
