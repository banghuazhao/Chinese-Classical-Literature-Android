package com.appsbay.chineseclassicalliteratural;

import android.content.Context;
import android.content.Intent;
import android.app.Instrumentation;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Controller.BookPagerActivity;
import com.appsbay.chineseclassicalliteratural.Controller.ReaderCollectionActivity;
import com.appsbay.chineseclassicalliteratural.Model.BookChapter;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderContent;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderMarks;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ReaderFeaturesTest {
    @Test public void searchResultOpensReaderAtMatchedPassage() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        BookStore.shared.fetchFromLocal(context);
        BookStore.shared.setTraditional(context, false);
        Intent intent = new Intent(context, ReaderCollectionActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ReaderCollectionActivity search = (ReaderCollectionActivity) instrumentation.startActivitySync(intent);
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                BookPagerActivity.class.getName(), null, false);
        try {
            EditText input = search.findViewById(R.id.reader_search_input);
            LinearLayout results = search.findViewById(R.id.reader_search_results);
            instrumentation.runOnMainSync(() -> input.setText("学而时习之"));
            long deadline = System.currentTimeMillis() + 30000;
            while (System.currentTimeMillis() < deadline && results.getChildCount() == 0) {
                instrumentation.waitForIdleSync();
                Thread.sleep(100);
            }
            assertTrue(results.getChildCount() > 0);
            TextView first = (TextView) results.getChildAt(0);
            assertTrue(first.getText().toString().contains("学而时习之"));
            instrumentation.runOnMainSync(first::performClick);
            BookPagerActivity reader = (BookPagerActivity)
                    instrumentation.waitForMonitorWithTimeout(monitor, 20000);
            assertTrue(reader != null);
            assertEquals(0, reader.getIntent().getIntExtra("chapterIndex", -1));
            assertTrue(reader.getIntent().getIntExtra("focusOffset", -1) >= 0);
            TextView text = reader.findViewById(R.id.book_pager_text);
            assertTrue(text.getText().toString().contains("学而时习之"));
            instrumentation.runOnMainSync(reader::finish);
        } finally {
            instrumentation.removeMonitor(monitor);
            instrumentation.runOnMainSync(search::finish);
        }
    }

    @Test public void searchFindsPassageAndMarkSurvivesDatabaseReopen() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        BookStore.shared.fetchFromLocal(context);
        Book book = null;
        for (Book variant : BookStore.shared.variantsForId("论语")) {
            if (!BookStore.shared.isTraditional(variant)) book = variant;
        }
        assertTrue(book != null);
        List<BookChapter> chapters = ReaderContent.load(context, book);
        assertEquals(20, chapters.size());
        String query = "学而时习之";
        List<ReaderContent.Match> hits = ReaderContent.search(context,
                Collections.singletonList(book), query, 10, () -> false);
        assertFalse(hits.isEmpty());
        ReaderContent.Match hit = hits.get(0);
        assertEquals(0, hit.chapterIndex);
        assertEquals(query, chapters.get(0).getText().substring(hit.offset, hit.offset + query.length()));
        assertTrue(hit.snippet.contains(query));

        long id;
        try (ReaderMarks marks = new ReaderMarks(context)) {
            id = marks.add(book, false, hit.chapterIndex, hit.offset, hit.offset + query.length(),
                    0.1f, ReaderMarks.HIGHLIGHT, query, "first note");
        }
        try (ReaderMarks marks = new ReaderMarks(context)) {
            ReaderMarks.Mark saved = null;
            for (ReaderMarks.Mark candidate : marks.forChapter(book, false, 0)) {
                if (candidate.id == id) saved = candidate;
            }
            assertTrue(saved != null);
            assertEquals(query, saved.quote);
            assertEquals("first note", saved.note);
            marks.updateNote(id, "edited");
            assertTrue(marks.forChapter(book, true, 0).isEmpty());
        }
        try (ReaderMarks marks = new ReaderMarks(context)) {
            boolean edited = false;
            for (ReaderMarks.Mark mark : marks.forChapter(book, false, 0)) {
                if (mark.id == id) edited = "edited".equals(mark.note);
            }
            assertTrue(edited);
            marks.delete(id);
            for (ReaderMarks.Mark mark : marks.forChapter(book, false, 0)) {
                assertTrue(mark.id != id);
            }
        }
    }
}
