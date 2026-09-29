package com.appsbay.chineseclassicalliteratural.data

data class ReadingProgress(
    val percent: Int,
    val chapterIndex: Int,
    val totalChapters: Int,
    val chapterLabel: String?
)
