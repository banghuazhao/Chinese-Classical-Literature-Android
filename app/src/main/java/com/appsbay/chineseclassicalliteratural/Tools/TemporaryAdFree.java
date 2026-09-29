package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Rewarded-ad temporary ad-free window (separate from the permanent Remove Ads IAP).
 */
public final class TemporaryAdFree {

    private static final String PREFS = "Ads Preference";
    private static final String KEY_UNTIL = "tempAdFreeUntil";
    public static final long DURATION_MS = 24L * 60L * 60L * 1000L;

    private TemporaryAdFree() {
    }

    public static boolean isActive(@NonNull Context context) {
        long until = prefs(context).getLong(KEY_UNTIL, 0L);
        if (until <= 0L) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= until) {
            prefs(context).edit().remove(KEY_UNTIL).apply();
            return false;
        }
        return true;
    }

    public static long remainingMillis(@NonNull Context context) {
        long until = prefs(context).getLong(KEY_UNTIL, 0L);
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public static void grantTwentyFourHours(@NonNull Context context) {
        long until = System.currentTimeMillis() + DURATION_MS;
        prefs(context).edit().putLong(KEY_UNTIL, until).apply();
        LocalBroadcastHelper.sendAdFreeChanged(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
