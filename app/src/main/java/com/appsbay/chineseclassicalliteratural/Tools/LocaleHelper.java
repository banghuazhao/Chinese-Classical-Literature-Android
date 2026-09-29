package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.appsbay.chineseclassicalliteratural.R;

import java.util.Locale;

/**
 * In-app language selection for the UI strings only. Book content shipped in
 * assets stays in its original language.
 *
 * <p>Backed by the AndroidX per-app locales API, which delegates to the system
 * picker on Android 13+ and persists the choice itself on older releases (see
 * the {@code AppLocalesMetadataHolderService} entry in the manifest).</p>
 */
public final class LocaleHelper {

    /** Empty tag means "follow the system language". */
    public static final String SYSTEM_TAG = "";

    /**
     * Selectable languages, in menu order. Names are written in their own
     * script so they read correctly whatever the current locale is. Must stay
     * in sync with {@code res/xml/locales_config.xml}.
     */
    private static final String[][] LANGUAGES = {
            {"en", "English"},
            {"zh-CN", "简体中文"},
            {"zh-TW", "繁體中文（台灣）"},
            {"zh-HK", "繁體中文（香港）"},
            {"fr", "Français"},
            {"de", "Deutsch"},
            {"ja", "日本語"},
            {"es", "Español"},
            {"hi", "हिन्दी"},
    };

    private LocaleHelper() {
    }

    /** Language tags in menu order, with the system option first. */
    @NonNull
    public static String[] tags() {
        String[] tags = new String[LANGUAGES.length + 1];
        tags[0] = SYSTEM_TAG;
        for (int i = 0; i < LANGUAGES.length; i++) {
            tags[i + 1] = LANGUAGES[i][0];
        }
        return tags;
    }

    /** Display labels matching {@link #tags()}. */
    @NonNull
    public static String[] labels(@NonNull Context context) {
        String[] labels = new String[LANGUAGES.length + 1];
        labels[0] = context.getString(R.string.language_system_default);
        for (int i = 0; i < LANGUAGES.length; i++) {
            labels[i + 1] = LANGUAGES[i][1];
        }
        return labels;
    }

    /** The tag currently applied, or {@link #SYSTEM_TAG} when following the system. */
    @NonNull
    public static String getSelectedTag() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return SYSTEM_TAG;
        }
        Locale selected = locales.get(0);
        if (selected == null) {
            return SYSTEM_TAG;
        }
        String tag = selected.toLanguageTag();
        for (String[] language : LANGUAGES) {
            if (language[0].equalsIgnoreCase(tag)) {
                return language[0];
            }
        }
        // A stored region we don't list explicitly (e.g. "zh-Hant-TW") still
        // maps onto the closest entry by language + region.
        for (String[] language : LANGUAGES) {
            Locale candidate = Locale.forLanguageTag(language[0]);
            if (candidate.getLanguage().equals(selected.getLanguage())
                    && (candidate.getCountry().isEmpty()
                    || candidate.getCountry().equals(selected.getCountry()))) {
                return language[0];
            }
        }
        return SYSTEM_TAG;
    }

    /** Index of the current selection within {@link #tags()}. */
    public static int getSelectedIndex() {
        String selected = getSelectedTag();
        String[] tags = tags();
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals(selected)) {
                return i;
            }
        }
        return 0;
    }

    /** Label of the current selection, for the menu row subtitle. */
    @NonNull
    public static String getSelectedLabel(@NonNull Context context) {
        return labels(context)[getSelectedIndex()];
    }

    /**
     * Applies a language tag. Passing {@link #SYSTEM_TAG} (or {@code null})
     * clears the override and follows the system language again.
     */
    public static void apply(@Nullable String tag) {
        LocaleListCompat locales = (tag == null || tag.isEmpty())
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(tag);
        AppCompatDelegate.setApplicationLocales(locales);
    }
}
