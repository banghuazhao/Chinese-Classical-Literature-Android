package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;

public final class ReadingProgressHelper {

    private static final String CONTINUE_PREFS = "Continue Reading";
    private static final String PROGRESS_PREFS = "Reading Progress";
    private static final String BOOKMARKS_PREFS = "Bookmarks";

    private static final String KEY_BOOK = "lastBookName";
    private static final String KEY_CHAPTER_INDEX = "lastChapterIndex";
    private static final String KEY_CHAPTER_NUMBER = "lastChapterNumberName";
    private static final String KEY_CHAPTER_NAME = "lastChapterName";
    private static final String KEY_UPDATED_AT = "lastUpdatedAt";

    private ReadingProgressHelper() {
    }

    public static void saveSession(Context context, Book book, int chapterIndex,
                                   String chapterNumberName, String chapterName,
                                   int totalChapters, float scrollFraction) {
        if (context == null || book == null) {
            return;
        }
        context.getSharedPreferences(CONTINUE_PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_BOOK, book.getName())
                .putInt(KEY_CHAPTER_INDEX, chapterIndex)
                .putString(KEY_CHAPTER_NUMBER, chapterNumberName)
                .putString(KEY_CHAPTER_NAME, chapterName)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();

        SharedPreferences.Editor editor = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(totalKey(book.getName()), totalChapters);
        editor.putFloat(scrollKey(book.getName()), scrollFraction);
        editor.apply();
    }

    public static void markChapterOpened(Context context, Book book, int chapterIndex,
                                         String chapterNumberName, String chapterName,
                                         int totalChapters) {
        if (context == null || book == null) {
            return;
        }
        int previousIndex = getChapterIndex(context, book.getName());
        float scroll = previousIndex == chapterIndex
                ? getScrollFraction(context, book.getName())
                : 0f;
        // The previous app saved a per-chapter position in its own preference file.
        // Carry it forward the first time this reader opens that chapter.
        SharedPreferences progressPrefs = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE);
        if (previousIndex == chapterIndex && !progressPrefs.contains(scrollKey(book.getName()))) {
            String legacyKey = "bookName: " + book.getName() + ", chapterNumber: "
                    + chapterNumberName + ", chapterName: " + chapterName;
            scroll = context.getSharedPreferences(legacyKey, Context.MODE_PRIVATE)
                    .getFloat(legacyKey, scroll);
        }
        context.getSharedPreferences(BOOKMARKS_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(book.getName(), chapterIndex)
                .apply();
        saveSession(context, book, chapterIndex, chapterNumberName, chapterName, totalChapters, scroll);
    }

    public static float getScrollFraction(Context context, String bookName) {
        if (context == null || bookName == null) {
            return 0f;
        }
        return context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE)
                .getFloat(scrollKey(bookName), 0f);
    }

    public static void saveTotalChapters(Context context, Book book, int totalChapters) {
        if (context == null || book == null || totalChapters <= 0) {
            return;
        }
        context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(totalKey(book.getName()), totalChapters)
                .apply();
    }

    @Nullable
    public static Book getContinueReadingBook(Context context) {
        if (context == null) {
            return null;
        }
        String bookName = context.getSharedPreferences(CONTINUE_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_BOOK, null);
        if (bookName == null || bookName.isEmpty()) {
            bookName = findMostRecentBookmarkedBook(context);
        }
        if (bookName == null || bookName.isEmpty()) {
            return null;
        }
        for (Book book : BookStore.shared.getBooks(context)) {
            if (bookName.equals(book.getName())) {
                return book;
            }
        }
        return null;
    }

    @Nullable
    private static String findMostRecentBookmarkedBook(Context context) {
        java.util.Map<String, ?> bookmarks = context.getSharedPreferences(BOOKMARKS_PREFS, Context.MODE_PRIVATE).getAll();
        String fallback = null;
        int highestIndex = -1;
        for (java.util.Map.Entry<String, ?> entry : bookmarks.entrySet()) {
            if (entry.getValue() instanceof Integer) {
                int index = (Integer) entry.getValue();
                if (index > highestIndex) {
                    highestIndex = index;
                    fallback = entry.getKey();
                }
            }
        }
        return fallback;
    }

    public static int getContinueChapterIndex(Context context) {
        if (context == null) {
            return 0;
        }
        return context.getSharedPreferences(CONTINUE_PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_CHAPTER_INDEX, 0);
    }

    @Nullable
    public static String getContinueChapterLabel(Context context) {
        if (context == null) {
            return null;
        }
        SharedPreferences prefs = context.getSharedPreferences(CONTINUE_PREFS, Context.MODE_PRIVATE);
        String chapterName = prefs.getString(KEY_CHAPTER_NAME, null);
        if (chapterName != null && !chapterName.isEmpty()) {
            return chapterName;
        }
        int index = prefs.getInt(KEY_CHAPTER_INDEX, -1);
        if (index >= 0) {
            return "Chapter " + (index + 1);
        }
        return null;
    }

    public static int getChapterIndex(Context context, String bookName) {
        return context.getSharedPreferences(BOOKMARKS_PREFS, Context.MODE_PRIVATE)
                .getInt(bookName, -1);
    }

    public static int getTotalChapters(Context context, String bookName) {
        return context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE)
                .getInt(totalKey(bookName), 0);
    }

    public static int getProgressPercent(Context context, Book book) {
        if (context == null || book == null) {
            return -1;
        }
        int chapterIndex = getChapterIndex(context, book.getName());
        if (chapterIndex < 0) {
            return -1;
        }
        int total = getTotalChapters(context, book.getName());
        if (total <= 0) {
            return -1;
        }
        float scroll = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE)
                .getFloat(scrollKey(book.getName()), 0f);
        float progress = ((chapterIndex + scroll) / total) * 100f;
        return Math.min(100, Math.max(0, Math.round(progress)));
    }

    @Nullable
    public static String getProgressLabel(Context context, Book book) {
        int percent = getProgressPercent(context, book);
        int chapterIndex = getChapterIndex(context, book.getName());
        if (chapterIndex < 0) {
            return null;
        }
        int total = getTotalChapters(context, book.getName());
        if (percent >= 0 && total > 0) {
            return context.getString(com.appsbay.chineseclassicalliteratural.R.string.reading_progress_label,
                    percent, chapterIndex + 1, total);
        }
        return context.getString(com.appsbay.chineseclassicalliteratural.R.string.reading_chapter_only, chapterIndex + 1);
    }

    public static void bindProgressRow(Context context, Book book,
                                       TextView progressText, ProgressBar progressBar) {
        if (progressText == null || progressBar == null) {
            return;
        }
        String label = getProgressLabel(context, book);
        int percent = getProgressPercent(context, book);
        if (label == null) {
            progressText.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            return;
        }
        progressText.setVisibility(View.VISIBLE);
        progressText.setText(label);
        progressText.setTextColor(MyColor.getDetailTextColor(context));
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(100);
        progressBar.setProgress(Math.max(percent, 0));
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(MyColor.getAccentColor(context)));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(MyColor.getSeparatorColor(context)));
    }

    public static void bindOfflineBadge(Context context, Book book, TextView badge) {
        if (badge != null) {
            badge.setVisibility(View.GONE);
        }
    }

    private static String totalKey(String bookName) {
        return bookName + "_total";
    }

    private static String scrollKey(String bookName) {
        return bookName + "_scroll";
    }
}
