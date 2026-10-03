package com.appsbay.chineseclassicalliteratural;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appsbay.chineseclassicalliteratural.Controller.MainActivity;
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BackgroundChangeRegressionTest {
    @Test
    public void changingBackgroundFromMoreDoesNotCrashHiddenHomeFragment() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        SharedPreferences age = context.getSharedPreferences("Age Group", Context.MODE_PRIVATE);
        boolean hadAge = age.contains("ageGroup");
        int previousAge = age.getInt("ageGroup", AgeGate.UNKNOWN);
        MainActivity activity = null;
        try {
            assertTrue(AgeGate.saveGroup(context, AgeGate.UNDER_18));
            Intent intent = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity = (MainActivity) instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();
            MainActivity launched = activity;
            instrumentation.runOnMainSync(() -> {
                MaterialToolbar toolbar = launched.findViewById(R.id.toolbar);
                assertNotNull(toolbar.getMenu().findItem(R.id.home_menu_action_change));
                BottomNavigationView navigation = launched.findViewById(R.id.bottom_navigation_main);
                navigation.setSelectedItemId(R.id.nav_more);
            });
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                BottomNavigationView navigation = launched.findViewById(R.id.bottom_navigation_main);
                MaterialToolbar toolbar = launched.findViewById(R.id.toolbar);
                Fragment home = launched.getSupportFragmentManager().findFragmentByTag("1");
                assertEquals(R.id.nav_more, navigation.getSelectedItemId());
                assertNotNull(home);
                assertTrue(home.isHidden());
                assertTrue(toolbar.getMenu().size() < 2);
                LocalBroadcastManager.getInstance(launched).sendBroadcast(
                        new Intent("NotificationBackgroundChange"));
            });
            instrumentation.waitForIdleSync();
            assertFalse(activity.isFinishing());
        } finally {
            if (activity != null) {
                MainActivity launched = activity;
                instrumentation.runOnMainSync(launched::finish);
            }
            SharedPreferences.Editor restore = age.edit();
            if (hadAge) restore.putInt("ageGroup", previousAge);
            else restore.remove("ageGroup");
            restore.commit();
        }
    }
}
