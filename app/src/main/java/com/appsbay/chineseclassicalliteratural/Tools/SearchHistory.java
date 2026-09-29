package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Recent search queries persisted with TinyDB.
 */
public final class SearchHistory {

    private static final String KEY = "search_recent_queries";
    private static final int MAX = 8;

    private SearchHistory() {
    }

    @NonNull
    public static ArrayList<String> get(@NonNull Context context) {
        TinyDB db = new TinyDB(context.getApplicationContext());
        ArrayList<String> stored = db.getListString(KEY);
        ArrayList<String> cleaned = new ArrayList<>();
        for (String item : stored) {
            if (item != null) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    cleaned.add(trimmed);
                }
            }
        }
        return cleaned;
    }

    public static void add(@NonNull Context context, String rawQuery) {
        if (rawQuery == null) {
            return;
        }
        String query = rawQuery.trim();
        if (query.isEmpty()) {
            return;
        }
        ArrayList<String> current = get(context);
        String lower = query.toLowerCase(Locale.US);
        Iterator<String> it = current.iterator();
        while (it.hasNext()) {
            if (it.next().toLowerCase(Locale.US).equals(lower)) {
                it.remove();
            }
        }
        current.add(0, query);
        while (current.size() > MAX) {
            current.remove(current.size() - 1);
        }
        new TinyDB(context.getApplicationContext()).putListString(KEY, current);
    }

    public static void clear(@NonNull Context context) {
        new TinyDB(context.getApplicationContext()).putListString(KEY, new ArrayList<>());
    }

    @NonNull
    public static List<String> popularAuthors(@NonNull List<?> books, int limit) {
        java.util.LinkedHashMap<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (Object item : books) {
            if (!(item instanceof com.appsbay.chineseclassicalliteratural.Model.Book)) {
                continue;
            }
            String author = ((com.appsbay.chineseclassicalliteratural.Model.Book) item).getAuthor();
            if (author == null) {
                continue;
            }
            String name = author.trim();
            if (name.isEmpty()) {
                continue;
            }
            Integer prev = counts.get(name);
            counts.put(name, prev == null ? 1 : prev + 1);
        }
        ArrayList<java.util.Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        // ArrayList#sort needs API 24; minSdk is 23.
        java.util.Collections.sort(entries, (a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            if (cmp != 0) {
                return cmp;
            }
            return a.getKey().compareToIgnoreCase(b.getKey());
        });
        ArrayList<String> result = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : entries) {
            result.add(entry.getKey());
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }
}
