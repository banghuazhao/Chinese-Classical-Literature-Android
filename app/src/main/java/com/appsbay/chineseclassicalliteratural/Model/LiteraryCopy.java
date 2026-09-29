package com.appsbay.chineseclassicalliteratural.Model;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class LiteraryCopy {
    public static final LiteraryCopy shared = new LiteraryCopy();

    private final Map<String, String> books = new HashMap<>();
    private final Map<String, String> authors = new HashMap<>();

    public void fetchFromLocal(Context context) {
        if (!books.isEmpty() || !authors.isEmpty()) {
            return;
        }
        loadMap(context, "literary_copy/books.json", books);
        loadMap(context, "literary_copy/authors.json", authors);
    }

    public String introductionFor(Book book) {
        if (book == null) {
            return null;
        }
        String intro = books.get(normalize(book.getName()));
        if (notBlank(intro)) {
            return intro;
        }
        intro = books.get(normalize(book.getParent()));
        if (notBlank(intro)) {
            return intro;
        }
        return null;
    }

    public String authorBioFor(Book book) {
        if (book == null) {
            return null;
        }
        String bio = authors.get(normalize(book.getAuthor()));
        return notBlank(bio) ? bio : null;
    }

    public enum MatchKind {
        NONE,
        TITLE,
        AUTHOR,
        INTRODUCTION,
        AUTHOR_BIO
    }

    /**
     * True when query matches title, author, book introduction, or author bio.
     */
    public boolean matchesSearch(Book book, String rawQuery) {
        return resolveMatch(book, rawQuery) != MatchKind.NONE;
    }

    /**
     * First matching field for the query, or {@link MatchKind#NONE}.
     * Empty query matches everything as {@link MatchKind#TITLE} for display convenience.
     */
    public MatchKind resolveMatch(Book book, String rawQuery) {
        if (book == null) {
            return MatchKind.NONE;
        }
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase();
        if (query.isEmpty()) {
            return MatchKind.TITLE;
        }
        if (book.getName() != null && book.getName().toLowerCase().contains(query)) {
            return MatchKind.TITLE;
        }
        if (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(query)) {
            return MatchKind.AUTHOR;
        }
        String intro = introductionFor(book);
        if (intro != null && intro.toLowerCase().contains(query)) {
            return MatchKind.INTRODUCTION;
        }
        String bio = authorBioFor(book);
        if (bio != null && bio.toLowerCase().contains(query)) {
            return MatchKind.AUTHOR_BIO;
        }
        return MatchKind.NONE;
    }

    private void loadMap(Context context, String assetPath, Map<String, String> target) {
        String json = readAsset(context, assetPath);
        if (json == null || json.trim().isEmpty()) {
            return;
        }
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isJsonNull()) {
                    continue;
                }
                String value = entry.getValue().getAsString();
                if (notBlank(value)) {
                    target.put(normalize(entry.getKey()), value.trim());
                }
            }
        } catch (Exception e) {
            Log.e("LiteraryCopy", "Failed to load " + assetPath, e);
        }
    }

    private String readAsset(Context context, String assetPath) {
        try {
            InputStream is = context.getAssets().open(assetPath);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e("LiteraryCopy", "Missing asset " + assetPath, e);
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
