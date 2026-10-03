package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.appsbay.chineseclassicalliteratural.BuildConfig;
import com.appsbay.chineseclassicalliteratural.Controller.MyApplication;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.util.ArrayList;

/** The single gate for starting the ads SDK and requesting ads. */
public final class PrivacyManager {
    private static PrivacyManager instance;

    private final Context appContext;
    private final ConsentInformation consentInformation;
    private boolean updateStarted;
    private boolean consentResolved;
    private boolean consentAllowsAds;
    private boolean adsInitialized;
    private boolean updateFinished;
    private boolean privacyOptionsShowing;
    private int ageEpoch;
    private final ArrayList<Runnable> onSettled = new ArrayList<>();

    private PrivacyManager(Context context) {
        appContext = context.getApplicationContext();
        consentInformation = UserMessagingPlatform.getConsentInformation(appContext);
    }

    public static synchronized PrivacyManager get(@NonNull Context context) {
        if (instance == null) {
            instance = new PrivacyManager(context);
        }
        return instance;
    }

    /** Refresh UMP on each process launch. A failed update leaves ad requests disabled. */
    public void start(@NonNull Activity activity) {
        start(activity, null);
    }

    public void start(@NonNull Activity activity, Runnable completion) {
        if (!AgeGate.isAdult(activity)) {
            if (completion != null) completion.run();
            return;
        }
        if (completion != null) {
            if (updateFinished) {
                completion.run();
            } else {
                onSettled.add(completion);
            }
        }
        if (updateStarted) {
            return;
        }
        updateStarted = true;
        final int epoch = ageEpoch;

        ConsentRequestParameters.Builder parameters = new ConsentRequestParameters.Builder();
        if (BuildConfig.DEBUG && BuildConfig.CONSENT_DEBUG_GEOGRAPHY != 0) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(BuildConfig.CONSENT_DEBUG_GEOGRAPHY)
                    .build();
            parameters.setConsentDebugSettings(debugSettings);
        }

        consentInformation.requestConsentInfoUpdate(
                activity,
                parameters.build(),
                () -> {
                    if (epoch == ageEpoch && AgeGate.isAdult(activity)) {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                activity, error -> onConsentResult(epoch, error));
                    }
                },
                error -> onConsentResult(epoch, error));
    }

    /** The UMP form is the same entry point for first consent and later withdrawal. */
    public void showPrivacyOptions(@NonNull Activity activity) {
        if (!AgeGate.isAdult(activity) || !isPrivacyOptionsRequired()
                || privacyOptionsShowing) {
            return;
        }
        final int epoch = ageEpoch;
        privacyOptionsShowing = true;
        // Stop future loads while the user can change the choice.
        consentResolved = false;
        consentAllowsAds = false;
        ((MyApplication) appContext).clearCachedAds();
        AdsHelper.refreshBanners();
        UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> {
            if (epoch != ageEpoch || !AgeGate.isAdult(activity)) return;
            privacyOptionsShowing = false;
            onConsentResult(epoch, error);
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.recreate();
            }
        });
    }

    public boolean isPrivacyOptionsRequired() {
        return updateFinished && consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public boolean canRequestAds() {
        return AgeGate.isAdult(appContext)
                && consentResolved && consentAllowsAds && adsInitialized;
    }

    /** Forget the in-process adult decision when an age group is corrected. */
    public static void stopForRestrictedAge() {
        if (instance == null) return;
        instance.ageEpoch++;
        instance.updateStarted = false;
        instance.updateFinished = false;
        instance.consentResolved = false;
        instance.consentAllowsAds = false;
        instance.privacyOptionsShowing = false;
        instance.onSettled.clear();
    }

    /** Called only after MobileAds.initialize() completes. */
    public void onAdsInitialized() {
        adsInitialized = true;
        AdsHelper.refreshBanners();
    }

    private void onConsentResult(int epoch, FormError error) {
        if (epoch != ageEpoch || !AgeGate.isAdult(appContext)) return;
        consentResolved = error == null;
        consentAllowsAds = consentResolved
                && consentInformation.canRequestAds()
                && !hasRestrictiveChoice();
        if (!consentAllowsAds) {
            ((MyApplication) appContext).clearCachedAds();
        }
        AdsHelper.refreshBanners();
        if (consentAllowsAds) {
            ((MyApplication) appContext).startAdsAfterConsent();
        }
        updateFinished = true;
        for (Runnable completion : new ArrayList<>(onSettled)) {
            completion.run();
        }
        onSettled.clear();
    }

    /**
     * UMP decides whether an ad request is permitted. When a TCF purpose needed
     * for personalized ads is refused, this app conservatively serves no ads;
     * even non-personalized ads may use a mobile identifier for frequency caps.
     * The GMA SDK reads UMP's TCF and US-state GPP signals itself.
     */
    private boolean hasRestrictiveChoice() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        String purposes = preferences.getString("IABTCF_PurposeConsents", "");
        if (purposes != null && !purposes.isEmpty()) {
            if (purposes.length() < 4 || purposes.charAt(0) != '1'
                    || purposes.charAt(2) != '1' || purposes.charAt(3) != '1') {
                return true;
            }
        }
        // UMP writes GPP for regulated US states. Until partner-specific opt-out
        // propagation is verified, avoid starting any ad network in those states.
        String gpp = preferences.getString("IABGPP_HDR_GppString", "");
        return gpp != null && !gpp.isEmpty();
    }
}
