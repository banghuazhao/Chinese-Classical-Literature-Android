package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.Model.Book;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Books flagged {@code isOnline} ship without their text; the JSON is pulled from
 * the CDN on first open and kept in app storage so later opens work offline.
 */
public final class OfflineBookManager {

    private static final String PREFS = "Offline Books";
    private static final String KEY_DOWNLOADED = "downloadedFileNames";

    private OfflineBookManager() {
    }

    public static String cdnUrl(Book book) {
        // This catalog ships every book in assets. No remote book endpoint is configured.
        return "";
    }

    public static boolean isAvailableOffline(Context context, Book book) {
        if (book == null) {
            return false;
        }
        if (!book.isOnline()) {
            return true;
        }
        File file = offlineFile(context, book);
        return file.exists() && file.length() > 0;
    }

    public static boolean isDownloaded(Context context, Book book) {
        if (book == null || !book.isOnline()) {
            return false;
        }
        return isAvailableOffline(context, book);
    }

    @Nullable
    public static String readOfflineJson(Context context, Book book) {
        if (!isDownloaded(context, book)) {
            return null;
        }
        File file = offlineFile(context, book);
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int read = in.read(buffer);
            if (read <= 0) {
                return null;
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (IOException | OutOfMemoryError e) {
            return null;
        }
    }

    public static void ensureOfflineCopy(@NonNull Context context, @NonNull Book book, @NonNull String json) {
        if (!book.isOnline() || isDownloaded(context, book)) {
            return;
        }
        try {
            writeOfflineJson(context, book, json);
            markDownloaded(context, book);
        } catch (IOException e) {
            // Best-effort: the book still opens this session, it just re-downloads next time.
            deleteDownload(context, book);
        }
    }

    public static void deleteDownload(Context context, Book book) {
        if (context == null || book == null) {
            return;
        }
        File file = offlineFile(context, book);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> downloaded = new HashSet<>(prefs.getStringSet(KEY_DOWNLOADED, new HashSet<>()));
        downloaded.remove(book.getFileName());
        prefs.edit().putStringSet(KEY_DOWNLOADED, downloaded).apply();
    }

    private static void writeOfflineJson(Context context, Book book, String json) throws IOException {
        File dir = new File(context.getFilesDir(), "offline_books");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create offline directory");
        }
        File file = offlineFile(context, book);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void markDownloaded(Context context, Book book) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> downloaded = new HashSet<>(prefs.getStringSet(KEY_DOWNLOADED, new HashSet<>()));
        downloaded.add(book.getFileName());
        prefs.edit().putStringSet(KEY_DOWNLOADED, downloaded).apply();
    }

    private static File offlineFile(Context context, Book book) {
        return new File(new File(context.getFilesDir(), "offline_books"),
                book.getFileName() + ".json");
    }
}
