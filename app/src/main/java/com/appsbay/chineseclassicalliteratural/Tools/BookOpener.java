package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.Intent;

import com.appsbay.chineseclassicalliteratural.Controller.BookChapterActivity;
import com.appsbay.chineseclassicalliteratural.Controller.BooksListActivity;
import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;

public final class BookOpener {
    public static final String EXTRA_BOOK = "book";
    public static final String EXTRA_BOOKS = "books";
    public static final String EXTRA_COLLECTION_BOOK = "collectionBook";
    public static final String EXTRA_CATEGORY_NAME = "categoryName";
    public static final String EXTRA_SEARCH_QUERY = "searchQuery";
    public static final String EXTRA_AUTO_OPEN_CHAPTER = "autoOpenChapter";

    private BookOpener() {
    }

    public static void open(Context context, Book book) {
        if (book == null || context == null || !BookStore.shared.isAvailable(context, book)) {
            return;
        }
        if (book.isCollection()) {
            Intent intent = new Intent(context, BooksListActivity.class);
            java.util.ArrayList<Book> available = new java.util.ArrayList<>();
            for (Book item : BookStore.shared.getBooksForCollection(book)) {
                if (BookStore.shared.isAvailable(context, item)) available.add(item);
            }
            intent.putParcelableArrayListExtra(EXTRA_BOOKS, available);
            intent.putExtra(EXTRA_COLLECTION_BOOK, book);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(context, BookChapterActivity.class);
            intent.putExtra(EXTRA_BOOK, book);
            context.startActivity(intent);
        }
    }

    public static void continueReading(Context context, Book book) {
        if (book == null || context == null || !BookStore.shared.isAvailable(context, book)) {
            return;
        }
        if (book.isCollection()) {
            open(context, book);
            return;
        }
        Intent intent = new Intent(context, BookChapterActivity.class);
        intent.putExtra(EXTRA_BOOK, book);
        intent.putExtra(EXTRA_AUTO_OPEN_CHAPTER, true);
        context.startActivity(intent);
    }
}
