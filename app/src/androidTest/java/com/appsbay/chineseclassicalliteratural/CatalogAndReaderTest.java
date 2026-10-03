package com.appsbay.chineseclassicalliteratural;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appsbay.chineseclassicalliteratural.Controller.BookChapterActivity;
import com.appsbay.chineseclassicalliteratural.Controller.BookPagerActivity;
import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CatalogAndReaderTest {
    @Test
    public void bothCatalogsHaveReadableBooksAndIntroductions() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.appsbay.chineseclassicalliteratural", context.getPackageName());
        BookStore.shared.fetchFromLocal(context);
        LiteraryCopy.shared.fetchFromLocal(context);
        List<Book> books = BookStore.shared.getAllBooks(context);
        // Restricted readers see 49 works per script; adults see all 52.
        assertTrue(books.size() >= 98);
        assertEquals(BookStore.shared.getBooks(context).size() * 2, books.size());
        for (Book book : books) {
            assertFalse(book.getName(), book.getName().trim().isEmpty());
            assertFalse(book.getName(), book.getAuthor().trim().isEmpty());
            assertNotNull(book.getName(), LiteraryCopy.shared.introductionFor(book));
            assertNotNull(book.getName(), LiteraryCopy.shared.authorBioFor(book));
            try (InputStream content = context.getAssets().open("files/" + book.getFileName() + ".json");
                 InputStream cover = context.getAssets().open("covers/" + book.getBookCover() + ".png")) {
                assertTrue(book.getName(), content.read() != -1);
                assertTrue(book.getName(), cover.read() != -1);
            }
        }
    }

    @Test
    public void opensSimplifiedAndTraditionalBookInReader() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        BookStore.shared.fetchFromLocal(context);
        List<Book> books = BookStore.shared.getAllBooks(context);
        Book simplified = null;
        Book traditional = null;
        for (Book book : books) {
            if (book.getName().equals("论语")) simplified = book;
            if (book.getName().equals("論語")) traditional = book;
        }
        assertNotNull(simplified);
        assertNotNull(traditional);
        SharedPreferences language = context.getSharedPreferences("Language Preference", Context.MODE_PRIVATE);
        boolean hadLanguage = language.contains("language");
        int previousLanguage = language.getInt("language", 0);
        try {
            BookStore.shared.setTraditional(context, false);
            openFirstChapter(instrumentation, context, simplified);
            BookStore.shared.setTraditional(context, true);
            openFirstChapter(instrumentation, context, traditional);
        } finally {
            SharedPreferences.Editor editor = language.edit();
            if (hadLanguage) {
                editor.putInt("language", previousLanguage);
            } else {
                editor.remove("language");
            }
            editor.apply();
        }
    }

    private void openFirstChapter(Instrumentation instrumentation, Context context, Book book) {
        Intent intent = new Intent(context, BookChapterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(BookOpener.EXTRA_BOOK, book);
        BookChapterActivity chapters = (BookChapterActivity) instrumentation.startActivitySync(intent);
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                BookPagerActivity.class.getName(), null, false);
        try {
            RecyclerView list = chapters.findViewById(R.id.book_chapter_recycler_view);
            long deadline = System.currentTimeMillis() + 15000;
            while (System.currentTimeMillis() < deadline && list.getAdapter().getItemCount() < 2) {
                instrumentation.waitForIdleSync();
                Thread.sleep(100);
            }
            assertTrue(book.getName(), list.getAdapter().getItemCount() > 1);
            instrumentation.runOnMainSync(() -> list.scrollToPosition(1));
            RecyclerView.ViewHolder[] row = new RecyclerView.ViewHolder[1];
            long rowDeadline = SystemClock.uptimeMillis() + 5000;
            while (SystemClock.uptimeMillis() < rowDeadline && row[0] == null) {
                instrumentation.waitForIdleSync();
                instrumentation.runOnMainSync(() -> row[0] = list.findViewHolderForAdapterPosition(1));
                if (row[0] == null) Thread.sleep(50);
            }
            assertNotNull(book.getName() + " chapter row was not attached", row[0]);
            instrumentation.runOnMainSync(() -> row[0].itemView.performClick());
            Activity reader = instrumentation.waitForMonitorWithTimeout(monitor, 10000);
            assertNotNull("Reader did not open " + book.getName(), reader);
            instrumentation.waitForIdleSync();
            TextView text = reader.findViewById(R.id.book_pager_text);
            assertTrue(book.getName(), text.getText().length() > 0);
            instrumentation.runOnMainSync(reader::finish);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        } finally {
            instrumentation.removeMonitor(monitor);
            instrumentation.runOnMainSync(chapters::finish);
            instrumentation.waitForIdleSync();
        }
    }
}
