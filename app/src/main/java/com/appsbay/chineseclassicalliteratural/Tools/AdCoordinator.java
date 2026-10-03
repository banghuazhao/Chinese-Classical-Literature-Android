package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.Controller.BookPagerActivity;
import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Central ad timing so fullscreen ads land at natural breaks instead of
 * interrupting reading, theme changes, or cold start.
 */
public final class AdCoordinator {

    private static final String LOG_TAG = "AdCoordinator";
    private static final String PREFS = "Ads Preference";
    private static final String KEY_CHAPTERS = "chaptersSinceInterstitial";
    private static final String KEY_LAST_FULLSCREEN = "lastFullscreenAdAt";
    private static final String KEY_HAS_READ = "hasFinishedReadingOnce";

    private static final long MIN_FULLSCREEN_INTERVAL_MS = 4 * 60 * 1000L;
    private static final int CHAPTERS_BETWEEN_INTERSTITIALS = 3;
    private static final long MIN_READING_MS = 12_000L;
    private static final long MIN_BACKGROUND_MS = 5_000L;

    private static AdCoordinator instance;

    private final Context appContext;
    private final SharedPreferences prefs;

    private InterstitialAd interstitialAd;
    private boolean loadingInterstitial;
    private int loadGeneration;
    private boolean showingFullscreen;
    private boolean hasEnteredForeground;
    private long appWentBackgroundAt;

    public static synchronized AdCoordinator get(@NonNull Context context) {
        if (instance == null) {
            instance = new AdCoordinator(context.getApplicationContext());
        }
        return instance;
    }

    private AdCoordinator(Context appContext) {
        this.appContext = appContext;
        this.prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void onAppForegrounded() {
        hasEnteredForeground = true;
    }

    public void onAppBackgrounded() {
        appWentBackgroundAt = System.currentTimeMillis();
    }

    public boolean isColdStart() {
        return !hasEnteredForeground;
    }

    public boolean canShowAppOpenAd(@Nullable Activity activity) {
        if (!AdsHelper.shouldShowAds(appContext)) {
            return false;
        }
        if (activity == null || showingFullscreen) {
            return false;
        }
        if (activity instanceof BookPagerActivity) {
            return false;
        }
        if (!prefs.getBoolean(KEY_HAS_READ, false)) {
            return false;
        }
        if (!wasBackgroundedLongEnough()) {
            return false;
        }
        return cooldownElapsed();
    }

    public void notifyFullscreenWillShow() {
        showingFullscreen = true;
    }

    public void notifyFullscreenDismissed() {
        showingFullscreen = false;
    }

    public void recordFullscreenShown() {
        showingFullscreen = true;
        prefs.edit()
                .putLong(KEY_LAST_FULLSCREEN, System.currentTimeMillis())
                .putInt(KEY_CHAPTERS, 0)
                .apply();
    }

    public void preloadInterstitial() {
        if (!AdsHelper.shouldShowAds(appContext)) {
            return;
        }
        if (loadingInterstitial || interstitialAd != null) {
            return;
        }
        String adUnitId = appContext.getString(R.string.adInterstitialID);
        if (TextUtils.isEmpty(adUnitId)) {
            return;
        }
        loadingInterstitial = true;
        final int generation = ++loadGeneration;
        InterstitialAd.load(appContext, adUnitId, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        if (generation != loadGeneration) return;
                        loadingInterstitial = false;
                        if (!AdsHelper.shouldShowAds(appContext)) return;
                        interstitialAd = ad;
                        Log.d(LOG_TAG, "Interstitial preloaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        if (generation != loadGeneration) return;
                        interstitialAd = null;
                        loadingInterstitial = false;
                        Log.i(LOG_TAG, loadAdError.getMessage());
                    }
                });
    }

    public void clearLoadedAd() {
        ++loadGeneration;
        interstitialAd = null;
        loadingInterstitial = false;
    }

    /**
     * Shows a preloaded interstitial after an engaged chapter.
     *
     * @return true if a fullscreen ad is now showing; {@code onContinue} runs after it closes.
     */
    public boolean maybeShowInterstitialAfterReading(@Nullable Activity activity,
                                                     long readingStartedAt,
                                                     boolean didScroll,
                                                     @NonNull Runnable onContinue) {
        if (!AdsHelper.shouldShowAds(appContext)) {
            return false;
        }
        if (activity == null || activity.isFinishing()) {
            return false;
        }

        boolean engaged = didScroll
                || System.currentTimeMillis() - readingStartedAt >= MIN_READING_MS;
        if (!engaged) {
            return false;
        }

        prefs.edit()
                .putBoolean(KEY_HAS_READ, true)
                .putInt(KEY_CHAPTERS, prefs.getInt(KEY_CHAPTERS, 0) + 1)
                .apply();

        if (prefs.getInt(KEY_CHAPTERS, 0) < CHAPTERS_BETWEEN_INTERSTITIALS
                || showingFullscreen
                || !cooldownElapsed()
                || interstitialAd == null) {
            preloadInterstitial();
            return false;
        }

        return showInterstitial(activity, onContinue);
    }

    private boolean showInterstitial(@NonNull Activity activity, @NonNull Runnable onContinue) {
        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        if (ad == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }

        showingFullscreen = true;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                showingFullscreen = false;
                preloadInterstitial();
                onContinue.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                showingFullscreen = false;
                preloadInterstitial();
                onContinue.run();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                recordFullscreenShown();
            }
        });
        try {
            ad.show(activity);
            return true;
        } catch (RuntimeException e) {
            showingFullscreen = false;
            preloadInterstitial();
            return false;
        }
    }

    private boolean wasBackgroundedLongEnough() {
        if (appWentBackgroundAt <= 0) {
            return false;
        }
        return System.currentTimeMillis() - appWentBackgroundAt >= MIN_BACKGROUND_MS;
    }

    private boolean cooldownElapsed() {
        long lastShown = prefs.getLong(KEY_LAST_FULLSCREEN, 0L);
        if (lastShown <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - lastShown >= MIN_FULLSCREEN_INTERVAL_MS;
    }
}
