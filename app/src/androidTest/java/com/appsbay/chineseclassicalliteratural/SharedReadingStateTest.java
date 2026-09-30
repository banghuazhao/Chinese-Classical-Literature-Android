package com.appsbay.chineseclassicalliteratural;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookLibrary;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Tools.ReadingProgressHelper;
import com.appsbay.chineseclassicalliteratural.Tools.TinyDB;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SharedReadingStateTest {

    @Test
    public void progressAndFavoritesFollowTheWorkAcrossScripts() {
        Context realContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Set<String> testPrefs = new HashSet<>();
        Context context = new ContextWrapper(realContext) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                String isolatedName = "shared_reading_test_" + name;
                testPrefs.add(isolatedName);
                return super.getSharedPreferences(isolatedName, mode);
            }
        };

        try {
            BookStore.shared.fetchFromLocal(context);
            BookStore.shared.setTraditional(context, false);
            Book simplified = BookStore.shared.bookForId(context, "儒林外史");
            assertNotNull(simplified);
            ReadingProgressHelper.saveSession(context, simplified, 10, "", "", 57, .25f);
            BookLibrary.shared.save(context, simplified);

            BookStore.shared.setTraditional(context, true);
            Book traditional = BookStore.shared.bookForId(context, "儒林外史");
            assertNotNull(traditional);
            assertTrue(BookStore.shared.isTraditional(traditional));
            assertEquals(7, BookStore.shared.chapterIndexFor(traditional, 8, false));
            assertEquals(9, BookStore.shared.chapterIndexFor(simplified, 8, true));
            assertEquals(traditional.getId(), ReadingProgressHelper.getContinueReadingBook(context).getId());
            assertEquals(9, ReadingProgressHelper.getContinueChapterIndex(context));
            assertEquals(56, ReadingProgressHelper.getTotalChapters(context, traditional));
            assertEquals(.25f, ReadingProgressHelper.getScrollFraction(context, traditional), .001f);
            assertTrue(BookLibrary.shared.have(context, traditional));
            assertTrue(BookStore.shared.isTraditional(BookLibrary.shared.books(context).get(0)));

            ReadingProgressHelper.saveSession(context, traditional, 11, "", "", 56, .5f);
            BookStore.shared.setTraditional(context, false);
            assertEquals(12, ReadingProgressHelper.getContinueChapterIndex(context));
            assertEquals(57, ReadingProgressHelper.getTotalChapters(context, simplified));

            // The older app saved visible titles, so check the one-time migration too.
            ArrayList<String> legacyFavorites = new ArrayList<>();
            legacyFavorites.add("論語");
            new TinyDB(context).putListString(BookLibrary.BOOK_LIBRARY_KEY, legacyFavorites);
            Book lunyu = BookStore.shared.bookForId(context, "论语");
            assertNotNull(lunyu);
            assertTrue(BookLibrary.shared.have(context, lunyu));
            assertEquals("论语", BookLibrary.shared.books(context).get(0).getName());
            BookLibrary.shared.remove(context, lunyu);
            assertFalse(BookLibrary.shared.have(context, lunyu));

            // Legacy title keys could coexist; the last-read edition wins.
            context.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE).edit()
                    .putInt("喻世明言", 10)
                    .putInt("喻世明言 (繁體)", 33)
                    .apply();
            context.getSharedPreferences("Continue Reading", Context.MODE_PRIVATE).edit()
                    .putString("lastBookName", "喻世明言 (繁體)")
                    .apply();
            context.getSharedPreferences("Reading Progress", Context.MODE_PRIVATE).edit()
                    .putInt("喻世明言 (繁體)_total", 39)
                    .putFloat("喻世明言 (繁體)_scroll", .4f)
                    .apply();
            Book legacyWork = BookStore.shared.bookForId(context, "喻世明言");
            assertNotNull(legacyWork);
            assertEquals(34, ReadingProgressHelper.getChapterIndex(context, legacyWork));
            assertEquals(40, ReadingProgressHelper.getTotalChapters(context, legacyWork));
            assertEquals(.4f, ReadingProgressHelper.getScrollFraction(context, legacyWork), .001f);
        } finally {
            for (String name : testPrefs) {
                realContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit();
            }
        }
    }
}
