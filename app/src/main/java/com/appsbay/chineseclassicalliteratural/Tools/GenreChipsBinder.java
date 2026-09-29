package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.Model.BookGenres;
import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public final class GenreChipsBinder {

    public interface Listener {
        void onGenreSelected(@Nullable String categoryName);
    }

    private GenreChipsBinder() {
    }

    public static void bind(ChipGroup chipGroup, @Nullable String selectedCategory, Listener listener) {
        Context context = chipGroup.getContext();
        chipGroup.removeAllViews();
        chipGroup.setSingleSelection(true);
        chipGroup.setSelectionRequired(true);

        addChip(chipGroup, BookGenres.ALL, context.getString(R.string.genre_all), selectedCategory, listener);
        for (String name : BookGenres.ALL_NAMES) {
            addChip(chipGroup, name, context.getString(BookGenres.shortLabelRes(name)), selectedCategory, listener);
        }
        applyTheme(chipGroup);
    }

    public static void applyTheme(ChipGroup chipGroup) {
        Context context = chipGroup.getContext();
        int accent = MyColor.getAccentColor(context);
        int onAccent = MyColor.getBackgroundColor(context);
        int surface = MyColor.getElevatedSurfaceColor(context);
        int text = MyColor.getTitleTextColor(context);
        int stroke = MyColor.getSeparatorColor(context);

        ColorStateList bg = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{accent, surface}
        );
        ColorStateList fg = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{onAccent, text}
        );
        ColorStateList outline = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{accent, stroke}
        );

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChipBackgroundColor(bg);
                chip.setTextColor(fg);
                chip.setChipStrokeColor(outline);
                chip.setChipStrokeWidth(context.getResources().getDimension(R.dimen.chip_stroke));
            }
        }
    }

    private static void addChip(
            ChipGroup chipGroup,
            String categoryKey,
            String label,
            @Nullable String selectedCategory,
            Listener listener
    ) {
        Chip chip = new Chip(chipGroup.getContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setChipCornerRadius(chipGroup.getResources().getDimension(R.dimen.space_16));
        chip.setTag(categoryKey);
        boolean selected = categoryKey.equals(selectedCategory == null ? BookGenres.ALL : selectedCategory);
        chip.setChecked(selected);
        chip.setOnClickListener(v -> {
            if (!chip.isChecked()) {
                chip.setChecked(true);
            }
            String key = (String) chip.getTag();
            listener.onGenreSelected(BookGenres.ALL.equals(key) ? null : key);
        });
        chipGroup.addView(chip);
    }
}
