package com.appsbay.chineseclassicalliteratural.Model;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class LiteraryCopy {
    public static final LiteraryCopy shared = new LiteraryCopy();

    private final Map<String, String> books = new HashMap<>();
    private final Map<String, String> authors = new HashMap<>();
    private final Map<String, String> traditionalBooks = new HashMap<>();
    private final Map<String, String> traditionalAuthors = new HashMap<>();

    public void fetchFromLocal(Context context) {
        if (!books.isEmpty() && !authors.isEmpty()
                && !traditionalBooks.isEmpty() && !traditionalAuthors.isEmpty()) {
            return;
        }
        loadMap(context, "literary_copy/books.json", books);
        loadMap(context, "literary_copy/authors.json", authors);
        loadMap(context, "literary_copy/books-traditional.json", traditionalBooks);
        loadMap(context, "literary_copy/authors-traditional.json", traditionalAuthors);
    }

    public String introductionFor(Book book) {
        if (book == null) {
            return null;
        }
        Map<String, String> copy = isTraditional(book) ? traditionalBooks : books;
        String intro = copy.get(normalize(book.getName()));
        if (notBlank(intro)) {
            return intro;
        }
        intro = copy.get(normalize(book.getParent()));
        if (notBlank(intro)) {
            return intro;
        }
        return null;
    }

    public String authorBioFor(Book book) {
        if (book == null) {
            return null;
        }
        Map<String, String> copy = isTraditional(book) ? traditionalAuthors : authors;
        String bio = copy.get(normalize(book.getAuthor()));
        return notBlank(bio) ? bio : null;
    }

    private static boolean isTraditional(Book book) {
        return book.getBookType() != null && book.getBookType().name().endsWith("_Fan");
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
            try (InputStream input = context.getAssets().open(assetPath);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
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
