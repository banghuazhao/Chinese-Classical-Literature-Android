package com.appsbay.chineseclassicalliteratural.Controller;

import android.app.Application;
import android.util.Log;

import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.Tools.AdCoordinator;
import com.appsbay.chineseclassicalliteratural.Tools.AppOpenManager;
import com.appsbay.chineseclassicalliteratural.Tools.BillingManager;
import com.appsbay.chineseclassicalliteratural.Tools.RewardedAdHelper;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;

import java.util.Map;


public class MyApplication extends Application {

    private static AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        BookStore.shared.fetchFromLocal(this);
        LiteraryCopy.shared.fetchFromLocal(this);
        BillingManager.get(this);

        new Thread(
                () -> MobileAds.initialize(this, this::onMobileAdsInitialized)
        ).start();

        appOpenManager = new AppOpenManager(this);
    }

    private void onMobileAdsInitialized(InitializationStatus initializationStatus) {
        Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
        for (String adapterClass : statusMap.keySet()) {
            AdapterStatus status = statusMap.get(adapterClass);
            if (status == null) {
                continue;
            }
            Log.d("MyApp", String.format(
                    "Adapter name: %s, Description: %s, Latency: %d",
                    adapterClass, status.getDescription(), status.getLatency()));
        }
        AdCoordinator.get(MyApplication.this).preloadInterstitial();
        RewardedAdHelper.get(MyApplication.this).preload();
    }
}
