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
    private static final String SCRIPT_SUFFIX = "_savedTraditional";

    private ReadingProgressHelper() {
    }

    public static void saveSession(Context context, Book book, int chapterIndex,
                                   String chapterNumberName, String chapterName,
                                   int totalChapters, float scrollFraction) {
        if (context == null || book == null) {
            return;
        }
        context.getSharedPreferences(CONTINUE_PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_BOOK, book.getId())
                .putInt(KEY_CHAPTER_INDEX, chapterIndex)
                .putString(KEY_CHAPTER_NUMBER, chapterNumberName)
                .putString(KEY_CHAPTER_NAME, chapterName)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();

        context.getSharedPreferences(BOOKMARKS_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(book.getId(), chapterIndex)
                .putBoolean(scriptKey(book.getId()), BookStore.shared.isTraditional(book))
                .apply();
        context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(totalKey(book), totalChapters)
                .putFloat(scrollKey(book.getId()), scrollFraction)
                .apply();
    }

    public static void markChapterOpened(Context context, Book book, int chapterIndex,
                                         String chapterNumberName, String chapterName,
                                         int totalChapters) {
        if (context == null || book == null) {
            return;
        }
        int previousIndex = getChapterIndex(context, book);
        float scroll = previousIndex == chapterIndex
                ? getScrollFraction(context, book)
                : 0f;
        // The previous app saved a per-chapter position in its own preference file.
        // Carry it forward the first time this reader opens that chapter.
        SharedPreferences progressPrefs = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE);
        if (previousIndex == chapterIndex && !progressPrefs.contains(scrollKey(book.getId()))) {
            String legacyKey = "bookName: " + book.getName() + ", chapterNumber: "
                    + chapterNumberName + ", chapterName: " + chapterName;
            scroll = context.getSharedPreferences(legacyKey, Context.MODE_PRIVATE)
                    .getFloat(legacyKey, scroll);
        }
        saveSession(context, book, chapterIndex, chapterNumberName, chapterName, totalChapters, scroll);
    }

    public static float getScrollFraction(Context context, String bookName) {
        if (context == null || bookName == null) {
            return 0f;
        }
        Book book = BookStore.shared.bookForName(context, bookName);
        return book == null ? 0f : getScrollFraction(context, book);
    }

    public static float getScrollFraction(Context context, Book book) {
        if (context == null || book == null) return 0f;
        SharedPreferences prefs = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE);
        String key = scrollKey(book.getId());
        if (prefs.contains(key)) return prefs.getFloat(key, 0f);
        for (String name : BookStore.shared.namesForId(book.getId())) {
            String legacyKey = scrollKey(name);
            if (prefs.contains(legacyKey)) {
                float fraction = prefs.getFloat(legacyKey, 0f);
                prefs.edit().putFloat(key, fraction).apply();
                return fraction;
            }
        }
        return 0f;
    }

    public static void saveTotalChapters(Context context, Book book, int totalChapters) {
        if (context == null || book == null || totalChapters <= 0) {
            return;
        }
        context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(totalKey(book), totalChapters)
                .apply();
    }

    @Nullable
    public static Book getContinueReadingBook(Context context) {
        if (context == null) {
            return null;
        }
        String bookNameOrId = context.getSharedPreferences(CONTINUE_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_BOOK, null);
        if (bookNameOrId == null || bookNameOrId.isEmpty()) {
            bookNameOrId = findMostRecentBookmarkedBook(context);
        }
        if (bookNameOrId == null || bookNameOrId.isEmpty()) {
            return null;
        }
        return BookStore.shared.bookForId(context, BookStore.shared.idForName(bookNameOrId));
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
        Book book = getContinueReadingBook(context);
        if (book != null) {
            int index = getChapterIndex(context, book);
            if (index >= 0) return index;
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
        Book book = getContinueReadingBook(context);
        if (book != null) {
            SharedPreferences bookmarks = context.getSharedPreferences(BOOKMARKS_PREFS, Context.MODE_PRIVATE);
            if (bookmarks.contains(book.getId()) &&
                    bookmarks.getBoolean(scriptKey(book.getId()), false) != BookStore.shared.isTraditional(book)) {
                int index = getChapterIndex(context, book);
                return context.getString(com.appsbay.chineseclassicalliteratural.R.string.reading_chapter_only,
                        index + 1);
            }
        }
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
        Book book = BookStore.shared.bookForName(context, bookName);
        return book == null ? -1 : getChapterIndex(context, book);
    }

    public static int getChapterIndex(Context context, Book book) {
        if (context == null || book == null) return -1;
        SharedPreferences prefs = context.getSharedPreferences(BOOKMARKS_PREFS, Context.MODE_PRIVATE);
        String id = book.getId();
        if (!prefs.contains(scriptKey(id))) {
            // Before work IDs, both script titles could have separate bookmarks.
            // Prefer the title from the last reading session, then this edition.
            String lastTitle = context.getSharedPreferences(CONTINUE_PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_BOOK, null);
            Book source = null;
            for (Book variant : BookStore.shared.variantsForId(id)) {
                if (variant.getName().equals(lastTitle) && prefs.contains(variant.getName())) {
                    source = variant;
                    break;
                }
            }
            if (source == null && prefs.contains(book.getName())) {
                source = book;
            }
            if (source == null) {
                for (Book variant : BookStore.shared.variantsForId(id)) {
                    if (prefs.contains(variant.getName())) {
                        source = variant;
                        break;
                    }
                }
            }
            if (source != null) {
                prefs.edit().putInt(id, prefs.getInt(source.getName(), -1))
                        .putBoolean(scriptKey(id), BookStore.shared.isTraditional(source))
                        .apply();
                SharedPreferences progress = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE);
                String oldScroll = scrollKey(source.getName());
                if (progress.contains(oldScroll)) {
                    progress.edit().putFloat(scrollKey(id), progress.getFloat(oldScroll, 0f)).apply();
                }
            }
        }
        if (!prefs.contains(id)) return -1;
        // An id can equal the old simplified title. Its legacy bookmark has no script flag.
        boolean sourceTraditional = prefs.getBoolean(scriptKey(id), false);
        return BookStore.shared.chapterIndexFor(book, prefs.getInt(id, -1), sourceTraditional);
    }

    public static int getTotalChapters(Context context, String bookName) {
        Book book = BookStore.shared.bookForName(context, bookName);
        return book == null ? 0 : getTotalChapters(context, book);
    }

    public static int getTotalChapters(Context context, Book book) {
        if (context == null || book == null) return 0;
        SharedPreferences prefs = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE);
        String currentKey = totalKey(book);
        if (prefs.contains(currentKey)) return prefs.getInt(currentKey, 0);
        for (Book source : BookStore.shared.variantsForId(book.getId())) {
            if (BookStore.shared.isTraditional(source) == BookStore.shared.isTraditional(book)) continue;
            String sourceKey = totalKey(source);
            int total = prefs.getInt(sourceKey, 0);
            if (total > 0) {
                return BookStore.shared.chapterCountFor(book, total, BookStore.shared.isTraditional(source));
            }
        }
        for (Book source : BookStore.shared.variantsForId(book.getId())) {
            if (BookStore.shared.isTraditional(source) != BookStore.shared.isTraditional(book)) continue;
            String legacyKey = totalKey(source.getName());
            if (prefs.contains(legacyKey)) {
                int total = prefs.getInt(legacyKey, 0);
                prefs.edit().putInt(currentKey, total).apply();
                return total;
            }
        }
        for (Book source : BookStore.shared.variantsForId(book.getId())) {
            if (BookStore.shared.isTraditional(source) == BookStore.shared.isTraditional(book)) continue;
            String legacyKey = totalKey(source.getName());
            if (prefs.contains(legacyKey)) {
                int total = BookStore.shared.chapterCountFor(book,
                        prefs.getInt(legacyKey, 0), BookStore.shared.isTraditional(source));
                prefs.edit().putInt(currentKey, total).apply();
                return total;
            }
        }
        return 0;
    }

    public static int getProgressPercent(Context context, Book book) {
        if (context == null || book == null) {
            return -1;
        }
        int chapterIndex = getChapterIndex(context, book);
        if (chapterIndex < 0) {
            return -1;
        }
        int total = getTotalChapters(context, book);
        if (total <= 0) {
            return -1;
        }
        float scroll = getScrollFraction(context, book);
        float progress = ((chapterIndex + scroll) / total) * 100f;
        return Math.min(100, Math.max(0, Math.round(progress)));
    }

    @Nullable
    public static String getProgressLabel(Context context, Book book) {
        int percent = getProgressPercent(context, book);
        int chapterIndex = getChapterIndex(context, book);
        if (chapterIndex < 0) {
            return null;
        }
        int total = getTotalChapters(context, book);
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

    private static String totalKey(Book book) {
        return book.getId() + (BookStore.shared.isTraditional(book) ? "_traditional_total" : "_simplified_total");
    }

    private static String scriptKey(String id) {
        return id + SCRIPT_SUFFIX;
    }

    private static String scrollKey(String bookName) {
        return bookName + "_scroll";
    }
}
