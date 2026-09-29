package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.view.View;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public final class AdsHelper {

    private AdsHelper() {
    }

    public static boolean shouldShowAds(Context context) {
        if (BillingManager.get(context).isAdFree()) {
            return false;
        }
        return !TemporaryAdFree.isActive(context);
    }

    public static void bindBanner(AdView adView) {
        if (adView == null) {
            return;
        }
        View parent = (View) adView.getParent();
        View container = null;
        if (parent != null && parent.getId() == R.id.ad_container) {
            container = parent;
        }
        if (!shouldShowAds(adView.getContext())) {
            adView.setVisibility(View.GONE);
            if (container != null) {
                container.setVisibility(View.GONE);
            }
            return;
        }
        adView.setVisibility(View.VISIBLE);
        if (container != null) {
            container.setVisibility(View.VISIBLE);
        }
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static void hideAdContainer(View adContainer) {
        if (adContainer != null) {
            adContainer.setVisibility(View.GONE);
        }
    }
}
