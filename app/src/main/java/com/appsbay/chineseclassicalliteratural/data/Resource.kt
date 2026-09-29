package com.appsbay.chineseclassicalliteratural.data

/**
 * Why a book's chapters could not be loaded. Drives which recovery message and
 * action the reader is offered.
 */
enum class BookLoadError {
    /** The book still lives on the CDN and the device has no connection. */
    NO_CONNECTION,

    /** There is a connection but the download failed (timeout, server error). */
    DOWNLOAD_FAILED,

    /** The book itself is missing or unreadable; retrying will not help. */
    CONTENT_UNAVAILABLE
}

sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(
        val reason: BookLoadError = BookLoadError.DOWNLOAD_FAILED,
        val message: String? = null
    ) : Resource<Nothing>()
}
