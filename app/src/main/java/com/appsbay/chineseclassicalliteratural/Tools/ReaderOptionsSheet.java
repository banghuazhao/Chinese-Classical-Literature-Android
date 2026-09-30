package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

/**
 * Material bottom sheets for reader overflow actions and font size.
 */
public final class ReaderOptionsSheet {

    public interface MenuCallbacks {
        void onFontSizeSelected();

        void onBackground();

        void onShare();

        void onSpeechControls();

        void onSearch();
        void onBookmark();
        void onSavedPassages();
        void onSwitchScript();

    }

    public interface FontSizeCallbacks {
        int currentFontSize();

        void onFontSizeChanged(int size);
    }

    private ReaderOptionsSheet() {
    }

    public static void showMenu(@NonNull Activity activity, @NonNull MenuCallbacks callbacks) {
        BottomSheetDialog dialog = DialogChrome.bottomSheet(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.sheet_reader_options, null, false);

        View handle = content.findViewById(R.id.sheet_handle);
        TextView title = content.findViewById(R.id.sheet_title);
        MaterialButton font = content.findViewById(R.id.sheet_action_font);
        MaterialButton background = content.findViewById(R.id.sheet_action_background);
        MaterialButton share = content.findViewById(R.id.sheet_action_share);
        MaterialButton tts = content.findViewById(R.id.sheet_action_tts);
        MaterialButton search = content.findViewById(R.id.sheet_action_search);
        MaterialButton bookmark = content.findViewById(R.id.sheet_action_bookmark);
        MaterialButton marks = content.findViewById(R.id.sheet_action_marks);
        MaterialButton script = content.findViewById(R.id.sheet_action_script);

        DialogChrome.tintSheetHandle(handle, activity);
        title.setTextColor(MyColor.getAccentColor(activity));
        DialogChrome.tintSheetAction(font, activity);
        DialogChrome.tintSheetAction(background, activity);
        DialogChrome.tintSheetAction(share, activity);
        DialogChrome.tintSheetAction(tts, activity);
        DialogChrome.tintSheetAction(search, activity);
        DialogChrome.tintSheetAction(bookmark, activity);
        DialogChrome.tintSheetAction(marks, activity);
        DialogChrome.tintSheetAction(script, activity);

        tts.setText(R.string.speech_controls);

        font.setOnClickListener(v -> {
            dialog.dismiss();
            callbacks.onFontSizeSelected();
        });
        background.setOnClickListener(v -> {
            dialog.dismiss();
            callbacks.onBackground();
        });
        share.setOnClickListener(v -> {
            dialog.dismiss();
            callbacks.onShare();
        });
        tts.setOnClickListener(v -> {
            dialog.dismiss();
            callbacks.onSpeechControls();
        });
        search.setOnClickListener(v -> { dialog.dismiss(); callbacks.onSearch(); });
        bookmark.setOnClickListener(v -> { dialog.dismiss(); callbacks.onBookmark(); });
        marks.setOnClickListener(v -> { dialog.dismiss(); callbacks.onSavedPassages(); });
        script.setOnClickListener(v -> { dialog.dismiss(); callbacks.onSwitchScript(); });

        DialogChrome.prepareSheet(dialog, content, activity);
        dialog.show();
    }

    public static void showFontSize(@NonNull Activity activity, @NonNull FontSizeCallbacks callbacks) {
        BottomSheetDialog dialog = DialogChrome.bottomSheet(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.sheet_font_size, null, false);

        View handle = content.findViewById(R.id.sheet_handle);
        TextView value = content.findViewById(R.id.sheet_font_size_value);
        Slider slider = content.findViewById(R.id.sheet_font_size_slider);

        DialogChrome.tintSheetHandle(handle, activity);
        int size = Math.max(12, Math.min(40, callbacks.currentFontSize()));
        value.setText(String.valueOf(size));
        value.setTextColor(MyColor.getTitleTextColor(activity));
        slider.setValue(size);
        slider.setHaloTintList(android.content.res.ColorStateList.valueOf(MyColor.getSeparatorColor(activity)));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(MyColor.getAccentColor(activity)));
        slider.setTrackActiveTintList(android.content.res.ColorStateList.valueOf(MyColor.getAccentColor(activity)));
        slider.setTrackInactiveTintList(android.content.res.ColorStateList.valueOf(MyColor.getSeparatorColor(activity)));

        slider.addOnChangeListener((s, newValue, fromUser) -> {
            int next = Math.round(newValue);
            value.setText(String.valueOf(next));
            if (fromUser) {
                callbacks.onFontSizeChanged(next);
            }
        });

        DialogChrome.prepareSheet(dialog, content, activity);
        dialog.show();
    }
}
