package com.appsbay.chineseclassicalliteratural;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appsbay.chineseclassicalliteratural.Controller.MainActivity;
import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate;
import com.google.android.gms.ads.AdView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AgeGateTest {
    @Test
    public void firstLaunchAgeChoiceShowsBothOptions() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        SharedPreferences preferences = context.getSharedPreferences("Age Group", Context.MODE_PRIVATE);
        boolean hadAge = preferences.contains("ageGroup");
        int previousAge = preferences.getInt("ageGroup", AgeGate.UNKNOWN);
        MainActivity activity = null;
        try {
            preferences.edit().remove("ageGroup").commit();
            Intent intent = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity = (MainActivity) instrumentation.startActivitySync(intent);
            onView(withId(R.id.age_gate_under_18)).check(matches(isDisplayed()));
            onView(withId(R.id.age_gate_adult)).check(matches(isDisplayed()));
            onView(withId(R.id.age_gate_under_18)).perform(click());
            assertEquals(AgeGate.UNDER_18, AgeGate.getGroup(context));
        } finally {
            if (activity != null) {
                MainActivity launched = activity;
                instrumentation.runOnMainSync(launched::finish);
            }
            SharedPreferences.Editor restore = preferences.edit();
            if (hadAge) restore.putInt("ageGroup", previousAge);
            else restore.remove("ageGroup");
            restore.commit();
        }
    }

    @Test
    public void removedWorksAreAbsentAndMatureWorksRequireAdultChoice() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context isolated = new ContextWrapper(target) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("catalog_age_test_" + name, mode);
            }
        };
        SharedPreferences preferences = isolated.getSharedPreferences("Age Group", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        BookStore.shared.fetchFromLocal(isolated);
        try {
            assertEquals(49, BookStore.shared.getBooks(isolated).size());
            for (String id : new String[]{"水浒传", "封神演义", "隋唐演义"}) {
                assertFalse(BookStore.shared.isAvailableId(isolated, id));
            }
            assertTrue(AgeGate.saveGroup(isolated, AgeGate.ADULT));
            assertEquals(52, BookStore.shared.getBooks(isolated).size());
            Set<String> ids = new HashSet<>();
            for (Book book : BookStore.shared.getBooks(isolated)) ids.add(book.getId());
            for (String id : new String[]{"隋炀帝艳史", "醒世恒言", "二刻拍案惊奇",
                    "初刻拍案惊奇", "红楼梦", "金石缘", "喻世明言"}) {
                assertFalse(id, ids.contains(id));
                assertFalse(BookStore.shared.isAvailableId(isolated, id));
            }
        } finally {
            preferences.edit().clear().commit();
        }
    }

    @Test
    public void ageGroupIsLocalPersistentAndFailsClosed() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context isolated = new ContextWrapper(target) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("age_gate_test_" + name, mode);
            }
        };
        SharedPreferences preferences = isolated.getSharedPreferences("Age Group", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        try {
            assertEquals(AgeGate.UNKNOWN, AgeGate.getGroup(isolated));
            assertFalse(AdsHelper.shouldShowAds(isolated));

            assertTrue(AgeGate.saveGroup(isolated, AgeGate.UNDER_18));
            assertEquals(AgeGate.UNDER_18, AgeGate.getGroup(isolated));
            assertFalse(AdsHelper.shouldShowAds(isolated));

            assertTrue(AgeGate.saveGroup(isolated, AgeGate.ADULT));
            assertTrue(AgeGate.isAdult(isolated));

            preferences.edit().putInt("ageGroup", 99).commit();
            assertEquals(AgeGate.UNKNOWN, AgeGate.getGroup(isolated));
            assertFalse(AdsHelper.shouldShowAds(isolated));
            preferences.edit().putString("ageGroup", "adult").commit();
            assertEquals(AgeGate.UNKNOWN, AgeGate.getGroup(isolated));
            assertFalse(AdsHelper.shouldShowAds(isolated));
            try {
                AgeGate.saveGroup(isolated, 99);
                fail("Invalid age group must be rejected");
            } catch (IllegalArgumentException expected) {
                assertEquals(AgeGate.UNKNOWN, AgeGate.getGroup(isolated));
            }
        } finally {
            preferences.edit().clear().commit();
        }
    }

    @Test
    public void restrictedReaderRendersWithoutAnyAdView() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        SharedPreferences preferences = context.getSharedPreferences("Age Group", Context.MODE_PRIVATE);
        boolean hadAge = preferences.contains("ageGroup");
        int previousAge = preferences.getInt("ageGroup", AgeGate.UNKNOWN);
        MainActivity activity = null;
        try {
            assertTrue(AgeGate.saveGroup(context, AgeGate.UNDER_18));
            Intent intent = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity = (MainActivity) instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();

            assertNotNull(activity.findViewById(R.id.main_category_recycler_view));
            FrameLayout adContainer = activity.findViewById(R.id.ad_container);
            assertNotNull(adContainer);
            assertEquals(0, adContainer.getChildCount());
            View content = activity.findViewById(android.R.id.content);
            assertFalse(containsAdView(content));
        } finally {
            if (activity != null) {
                MainActivity launched = activity;
                instrumentation.runOnMainSync(launched::finish);
            }
            if (hadAge) {
                preferences.edit().putInt("ageGroup", previousAge).commit();
            } else {
                preferences.edit().remove("ageGroup").commit();
            }
        }
    }

    private static boolean containsAdView(View view) {
        if (view instanceof AdView) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (containsAdView(group.getChildAt(i))) return true;
        }
        return false;
    }
}
