package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.util.JsonReader;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookChapter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Streams bundled books so full-text search does not retain the library in memory. */
public final class ReaderContent {
    private ReaderContent() {}

    public static final class Match {
        public final Book book;
        public final int chapterIndex;
        public final int offset;
        public final String chapterName;
        public final String snippet;

        public Match(Book book, int chapterIndex, int offset, String chapterName, String snippet) {
            this.book = book;
            this.chapterIndex = chapterIndex;
            this.offset = offset;
            this.chapterName = chapterName;
            this.snippet = snippet;
        }
    }

    public interface Cancelled { boolean get(); }

    public static List<BookChapter> load(Context context, Book book) throws IOException {
        List<BookChapter> chapters = new ArrayList<>();
        read(context, book, (index, chapter) -> { chapters.add(chapter); return true; });
        return chapters;
    }

    public static List<Match> search(Context context, List<Book> books, String query,
                                     int limit, Cancelled cancelled) {
        List<Match> matches = new ArrayList<>();
        String needle = query.toLowerCase(Locale.ROOT);
        boolean caseInsensitive = false;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                caseInsensitive = true;
                break;
            }
        }
        for (Book book : books) {
            if (cancelled.get() || matches.size() >= limit) break;
            try {
                // A cheap string scan avoids parsing thousands of chapters in books
                // that cannot possibly match. The largest bundled file is <3 MB.
                String json = readJson(context, book);
                if (!(caseInsensitive ? json.toLowerCase(Locale.ROOT).contains(needle)
                        : json.contains(query))) continue;
                read(new JsonReader(new StringReader(json)), (index, chapter) -> {
                    if (cancelled.get() || matches.size() >= limit) return false;
                    String body = chapter.getText();
                    int offset = body.toLowerCase(Locale.ROOT).indexOf(needle);
                    if (offset < 0 && !chapter.getChapterName().toLowerCase(Locale.ROOT).contains(needle)) return true;
                    String snippet = offset < 0 ? chapter.getChapterName() : snippet(body, offset, query.length());
                    matches.add(new Match(book, index, Math.max(0, offset), chapter.getChapterName(), snippet));
                    return true;
                });
            } catch (IOException ignored) {
                // A missing bundled file must not suppress results from other books.
            }
        }
        return matches;
    }

    public static String snippet(String body, int offset, int length) {
        int start = Math.max(0, offset - 32);
        int end = Math.min(body.length(), offset + length + 52);
        return (start > 0 ? "…" : "") + body.substring(start, end).replace('\n', ' ').trim()
                + (end < body.length() ? "…" : "");
    }

    private interface Visitor { boolean visit(int index, BookChapter chapter); }

    private static void read(Context context, Book book, Visitor visitor) throws IOException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(
                context.getAssets().open("files/" + book.getFileName() + ".json"), "UTF-8"))) {
            read(reader, visitor);
        }
    }

    private static String readJson(Context context, Book book) throws IOException {
        try (InputStream input = context.getAssets().open("files/" + book.getFileName() + ".json");
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return bytes.toString("UTF-8");
        }
    }

    private static void read(JsonReader reader, Visitor visitor) throws IOException {
        try (JsonReader input = reader) {
            input.beginObject();
            if (!input.hasNext()) return;
            input.nextName();
            input.beginArray();
            int index = 0;
            boolean keepGoing = true;
            while (input.hasNext() && keepGoing) {
                String number = "", name = "", body = "";
                input.beginObject();
                while (input.hasNext()) {
                    String key = input.nextName();
                    if (key.equals("章节") || key.equals("章節")) number = input.nextString();
                    else if (key.equals("章节名称") || key.equals("章節名稱")) name = input.nextString();
                    else if (key.equals("章节内容") || key.equals("章節內容")) body = input.nextString();
                    else input.skipValue();
                }
                input.endObject();
                keepGoing = visitor.visit(index++, new BookChapter(number, name, body));
            }
        }
    }
}
