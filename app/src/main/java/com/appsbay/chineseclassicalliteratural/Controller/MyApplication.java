package com.appsbay.chineseclassicalliteratural.Controller;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.Tools.AdCoordinator;
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate;
import com.appsbay.chineseclassicalliteratural.Tools.AppOpenManager;
import com.appsbay.chineseclassicalliteratural.Tools.PrivacyManager;
import com.appsbay.chineseclassicalliteratural.Tools.RewardedAdHelper;

import com.google.android.gms.ads.MobileAds;


public class MyApplication extends Application {

    private static AppOpenManager appOpenManager;
    private boolean adsInitializationStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        BookStore.shared.fetchFromLocal(this);
        LiteraryCopy.shared.fetchFromLocal(this);
        appOpenManager = new AppOpenManager(this);
    }

    /** Called by the consent resolver, never during Application startup. */
    public synchronized void startAdsAfterConsent() {
        if (!AgeGate.isAdult(this)) {
            return;
        }
        if (adsInitializationStarted) {
            if (PrivacyManager.get(this).canRequestAds()) {
                preloadAds();
            }
            return;
        }
        adsInitializationStarted = true;
        new Thread(() -> MobileAds.initialize(this, status ->
                new Handler(Looper.getMainLooper()).post(() -> {
                    PrivacyManager.get(this).onAdsInitialized();
                    preloadAds();
                }))).start();
    }

    private void preloadAds() {
        AdCoordinator.get(this).preloadInterstitial();
        RewardedAdHelper.get(this).preload();
        if (appOpenManager != null) {
            appOpenManager.fetchAd();
        }
    }

    public void clearCachedAds() {
        AdCoordinator.get(this).clearLoadedAd();
        RewardedAdHelper.get(this).clearLoadedAd();
        if (appOpenManager != null) {
            appOpenManager.clearLoadedAd();
        }
    }
}
