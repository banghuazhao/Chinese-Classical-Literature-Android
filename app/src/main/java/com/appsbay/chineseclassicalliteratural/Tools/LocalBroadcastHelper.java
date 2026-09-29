package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.appsbay.chineseclassicalliteratural.data.ReadingRepository;

public final class LocalBroadcastHelper {

    public static final String ACTION_AD_FREE_CHANGED = "NotificationAdFreeChanged";
    public static final String ACTION_CONTINUE_READING_CHANGED = "NotificationContinueReadingChanged";
    public static final String ACTION_OFFLINE_DOWNLOAD_CHANGED = "NotificationOfflineDownloadChanged";

    private LocalBroadcastHelper() {
    }

    public static void sendAdFreeChanged(Context context) {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(ACTION_AD_FREE_CHANGED));
    }

    public static void sendContinueReadingChanged(Context context) {
        ReadingRepository.getInstance(context).refreshContinueReading();
    }

    public static void sendOfflineDownloadChanged(Context context) {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(ACTION_OFFLINE_DOWNLOAD_CHANGED));
    }
}
