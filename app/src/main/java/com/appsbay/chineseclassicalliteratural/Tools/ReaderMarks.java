package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.appsbay.chineseclassicalliteratural.Model.Book;

import java.util.ArrayList;
import java.util.List;

/** Local passage marks, keyed by the stable work id and source script. */
public final class ReaderMarks extends SQLiteOpenHelper {
    public static final String HIGHLIGHT = "highlight";
    public static final String BOOKMARK = "bookmark";

    public static final class Mark {
        public long id;
        public String bookId, bookName, type, quote, note;
        public boolean traditional;
        public int chapterIndex, start, end;
        public float scrollFraction;
    }

    public ReaderMarks(Context context) { super(context, "reader_marks.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE marks (_id INTEGER PRIMARY KEY AUTOINCREMENT, book_id TEXT NOT NULL, " +
                "book_name TEXT NOT NULL, traditional INTEGER NOT NULL, chapter_index INTEGER NOT NULL, " +
                "start_offset INTEGER NOT NULL, end_offset INTEGER NOT NULL, scroll_fraction REAL NOT NULL, " +
                "mark_type TEXT NOT NULL, quote TEXT NOT NULL, note TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX marks_chapter ON marks(book_id,traditional,chapter_index)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public long add(Book book, boolean traditional, int chapterIndex, int start, int end,
                    float fraction, String type, String quote, String note) {
        ContentValues values = new ContentValues();
        values.put("book_id", book.getId());
        values.put("book_name", book.getName());
        values.put("traditional", traditional ? 1 : 0);
        values.put("chapter_index", chapterIndex);
        values.put("start_offset", start);
        values.put("end_offset", end);
        values.put("scroll_fraction", fraction);
        values.put("mark_type", type);
        values.put("quote", quote);
        values.put("note", note);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("marks", null, values);
    }

    public List<Mark> forChapter(Book book, boolean traditional, int chapterIndex) {
        return query("book_id=? AND traditional=? AND chapter_index=?",
                new String[]{book.getId(), traditional ? "1" : "0", String.valueOf(chapterIndex)});
    }

    public List<Mark> all() { return query(null, null); }

    public void updateNote(long id, String note) {
        ContentValues values = new ContentValues();
        values.put("note", note);
        getWritableDatabase().update("marks", values, "_id=?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("marks", "_id=?", new String[]{String.valueOf(id)});
    }

    private List<Mark> query(String where, String[] args) {
        List<Mark> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("marks", null, where, args, null, null,
                "created_at DESC, _id DESC")) {
            while (cursor.moveToNext()) {
                Mark mark = new Mark();
                mark.id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                mark.bookId = cursor.getString(cursor.getColumnIndexOrThrow("book_id"));
                mark.bookName = cursor.getString(cursor.getColumnIndexOrThrow("book_name"));
                mark.traditional = cursor.getInt(cursor.getColumnIndexOrThrow("traditional")) != 0;
                mark.chapterIndex = cursor.getInt(cursor.getColumnIndexOrThrow("chapter_index"));
                mark.start = cursor.getInt(cursor.getColumnIndexOrThrow("start_offset"));
                mark.end = cursor.getInt(cursor.getColumnIndexOrThrow("end_offset"));
                mark.scrollFraction = cursor.getFloat(cursor.getColumnIndexOrThrow("scroll_fraction"));
                mark.type = cursor.getString(cursor.getColumnIndexOrThrow("mark_type"));
                mark.quote = cursor.getString(cursor.getColumnIndexOrThrow("quote"));
                mark.note = cursor.getString(cursor.getColumnIndexOrThrow("note"));
                result.add(mark);
            }
        }
        return result;
    }
}
