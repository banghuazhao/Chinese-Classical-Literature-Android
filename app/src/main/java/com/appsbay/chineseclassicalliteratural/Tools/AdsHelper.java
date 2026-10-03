package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.WeakHashMap;

public final class AdsHelper {

    // Plain containers exist before consent. Construct AdViews only after an
    // adult's UMP choice permits the ads SDK to run.
    private static final WeakHashMap<FrameLayout, Boolean> banners = new WeakHashMap<>();

    private AdsHelper() {
    }

    public static boolean shouldShowAds(Context context) {
        if (!AgeGate.isAdult(context)) {
            return false;
        }
        if (!PrivacyManager.get(context).canRequestAds()) {
            return false;
        }
        if (BillingManager.get(context).isAdFree()) {
            return false;
        }
        return !TemporaryAdFree.isActive(context);
    }

    public static void bindBanner(FrameLayout container) {
        if (container == null) {
            return;
        }
        if (!banners.containsKey(container)) {
            banners.put(container, false);
        }
        if (!shouldShowAds(container.getContext())) {
            removeAdView(container);
            // Keep inset padding on activity containers even when no ad is shown.
            container.setVisibility(View.VISIBLE);
            return;
        }
        String unitId = container.getContext().getString(R.string.adBannerID);
        if (unitId.isEmpty()) {
            removeAdView(container);
            container.setVisibility(View.VISIBLE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        if (!container.isShown()) {
            // Hidden tabs must not request an ad. An activity may call this
            // before its window attaches, so retry once after attachment.
            removeAdView(container);
            if (!container.isAttachedToWindow()) {
                container.post(() -> {
                    if (banners.containsKey(container)) bindBanner(container);
                });
            }
            return;
        }
        if (Boolean.TRUE.equals(banners.get(container))) {
            return;
        }
        AdView adView = new AdView(container.getContext());
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(unitId);
        container.addView(adView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        banners.put(container, true);
        adView.loadAd(new AdRequest.Builder().build());
    }

    /** Refresh containers created before consent or after the user's choice changes. */
    public static void refreshBanners() {
        for (FrameLayout container : new ArrayList<>(banners.keySet())) {
            if (container != null) {
                bindBanner(container);
            }
        }
    }

    public static void releaseBanner(FrameLayout container) {
        if (container == null) return;
        removeAdView(container);
        banners.remove(container);
    }

    private static void removeAdView(FrameLayout container) {
        for (int i = container.getChildCount() - 1; i >= 0; --i) {
            View child = container.getChildAt(i);
            if (child instanceof AdView) {
                container.removeViewAt(i);
                ((AdView) child).destroy();
            }
        }
        banners.put(container, false);
    }

    public static void hideAdContainer(View adContainer) {
        if (adContainer != null) {
            adContainer.setVisibility(View.GONE);
        }
    }
}
