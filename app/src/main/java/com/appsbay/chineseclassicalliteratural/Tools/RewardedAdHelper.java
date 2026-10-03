package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Opt-in rewarded ad that grants 24 hours of ad-free reading.
 */
public final class RewardedAdHelper {

    private static final String LOG_TAG = "RewardedAdHelper";

    private static RewardedAdHelper instance;

    private final Context appContext;
    @Nullable
    private RewardedAd rewardedAd;
    private boolean loading;
    private int loadGeneration;

    public static synchronized RewardedAdHelper get(@NonNull Context context) {
        if (instance == null) {
            instance = new RewardedAdHelper(context.getApplicationContext());
        }
        return instance;
    }

    private RewardedAdHelper(Context appContext) {
        this.appContext = appContext;
    }

    public void preload() {
        if (!AdsHelper.shouldShowAds(appContext)) {
            return;
        }
        if (loading || rewardedAd != null) {
            return;
        }
        String adUnitId = appContext.getString(R.string.adRewardedID);
        if (TextUtils.isEmpty(adUnitId)) {
            return;
        }
        loading = true;
        final int generation = ++loadGeneration;
        RewardedAd.load(appContext, adUnitId, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        if (generation != loadGeneration) return;
                        loading = false;
                        if (!AdsHelper.shouldShowAds(appContext)) return;
                        rewardedAd = ad;
                        Log.d(LOG_TAG, "Rewarded ad preloaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        if (generation != loadGeneration) return;
                        rewardedAd = null;
                        loading = false;
                        Log.i(LOG_TAG, loadAdError.getMessage());
                    }
                });
    }

    /**
     * Shows a rewarded ad if ready; otherwise loads one and toasts failure/unavailable.
     */
    public void showForTwentyFourHourAdFree(@NonNull Activity activity) {
        if (activity.isFinishing()) {
            return;
        }
        if (!AgeGate.isAdult(activity)
                || !PrivacyManager.get(activity).canRequestAds()) {
            View anchor = DialogChrome.activityAnchor(activity);
            if (anchor != null) {
                DialogChrome.snack(anchor, R.string.rewarded_ad_unavailable);
            }
            return;
        }
        if (BillingManager.get(activity).isAdFree()) {
            View anchor = DialogChrome.activityAnchor(activity);
            if (anchor != null) {
                DialogChrome.snack(anchor, R.string.ad_free_active);
            }
            return;
        }
        if (TemporaryAdFree.isActive(activity)) {
            View anchor = DialogChrome.activityAnchor(activity);
            if (anchor != null) {
                DialogChrome.snack(anchor, R.string.temp_ad_free_already_active);
            }
            return;
        }

        if (rewardedAd != null) {
            present(activity, rewardedAd);
            return;
        }
        if (loading) {
            View anchor = DialogChrome.activityAnchor(activity);
            if (anchor != null) {
                DialogChrome.snack(anchor, R.string.rewarded_ad_loading);
            }
            return;
        }

        View anchor = DialogChrome.activityAnchor(activity);
        if (anchor != null) {
            DialogChrome.snack(anchor, R.string.rewarded_ad_loading);
        }
        String adUnitId = activity.getString(R.string.adRewardedID);
        if (TextUtils.isEmpty(adUnitId)) {
            if (anchor != null) {
                DialogChrome.snack(anchor, R.string.rewarded_ad_unavailable);
            }
            return;
        }
        loading = true;
        final int generation = ++loadGeneration;
        RewardedAd.load(activity, adUnitId, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        if (generation != loadGeneration) return;
                        loading = false;
                        if (!AdsHelper.shouldShowAds(activity)) return;
                        rewardedAd = ad;
                        if (!activity.isFinishing() && AdsHelper.shouldShowAds(activity)) {
                            present(activity, ad);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        if (generation != loadGeneration) return;
                        loading = false;
                        rewardedAd = null;
                        Log.i(LOG_TAG, loadAdError.getMessage());
                        if (!activity.isFinishing()) {
                            View anchor = DialogChrome.activityAnchor(activity);
                            if (anchor != null) {
                                DialogChrome.snack(anchor, R.string.rewarded_ad_unavailable);
                            }
                        }
                    }
                });
    }

    private void present(@NonNull Activity activity, @NonNull RewardedAd ad) {
        if (!AdsHelper.shouldShowAds(activity)) {
            rewardedAd = null;
            return;
        }
        AdCoordinator coordinator = AdCoordinator.get(appContext);
        final boolean[] earned = {false};

        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                coordinator.recordFullscreenShown();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                coordinator.notifyFullscreenDismissed();
                preload();
                if (earned[0] && !activity.isFinishing()) {
                    View anchor = DialogChrome.activityAnchor(activity);
                    if (anchor != null) {
                        DialogChrome.snackLong(anchor, R.string.temp_ad_free_granted);
                    }
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                coordinator.notifyFullscreenDismissed();
                Log.i(LOG_TAG, adError.getMessage());
                if (!activity.isFinishing()) {
                    View anchor = DialogChrome.activityAnchor(activity);
                    if (anchor != null) {
                        DialogChrome.snack(anchor, R.string.rewarded_ad_unavailable);
                    }
                }
                preload();
            }
        });

        coordinator.notifyFullscreenWillShow();
        OnUserEarnedRewardListener listener = rewardItem -> {
            earned[0] = true;
            TemporaryAdFree.grantTwentyFourHours(appContext);
        };
        ad.show(activity, listener);
        rewardedAd = null;
    }

    public void clearLoadedAd() {
        ++loadGeneration;
        rewardedAd = null;
        loading = false;
    }
}
