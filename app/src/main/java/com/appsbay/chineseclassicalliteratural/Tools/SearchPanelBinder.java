package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Binds search status, suggestions, and empty-state chrome shared across screens.
 */
public final class SearchPanelBinder {

    public interface Callbacks {
        void onSuggestionSelected(@NonNull String suggestion);

        void onClearSearch();

        void onSearchAllGenres();
    }

    private SearchPanelBinder() {
    }

    public static void applyTheme(@NonNull View root) {
        Context context = root.getContext();
        TextView count = root.findViewById(R.id.search_result_count);
        TextView genreCue = root.findViewById(R.id.search_genre_scope);
        TextView suggestionsLabel = root.findViewById(R.id.search_suggestions_label);
        TextView emptyTitle = root.findViewById(R.id.search_empty_title);
        TextView emptySubtitle = root.findViewById(R.id.search_empty_subtitle);
        TextView clearSearch = root.findViewById(R.id.search_empty_clear);
        TextView clearGenre = root.findViewById(R.id.search_empty_clear_genre);

        if (count != null) {
            count.setTextColor(MyColor.getDetailTextColor(context));
        }
        if (genreCue != null) {
            genreCue.setTextColor(MyColor.getDetailTextColor(context));
        }
        if (suggestionsLabel != null) {
            suggestionsLabel.setTextColor(MyColor.getDetailTextColor(context));
        }
        if (emptyTitle != null) {
            emptyTitle.setTextColor(MyColor.getTitleTextColor(context));
        }
        if (emptySubtitle != null) {
            emptySubtitle.setTextColor(MyColor.getDetailTextColor(context));
        }
        if (clearSearch != null) {
            clearSearch.setTextColor(MyColor.getAccentColor(context));
        }
        if (clearGenre != null) {
            clearGenre.setTextColor(MyColor.getAccentColor(context));
        }

        ChipGroup chips = root.findViewById(R.id.search_suggestion_chips);
        themeSuggestionChips(chips);
    }

    public static void update(
            @NonNull View root,
            boolean searchExpanded,
            @NonNull String query,
            int resultCount,
            @Nullable String genreLabel,
            @NonNull List<String> recent,
            @NonNull List<String> popularAuthors,
            @NonNull Callbacks callbacks
    ) {
        View statusRow = root.findViewById(R.id.search_status_row);
        View suggestions = root.findViewById(R.id.search_suggestions);
        View empty = root.findViewById(R.id.search_empty);
        if (empty == null) {
            empty = root.findViewById(R.id.search_empty_include);
        }
        TextView countView = root.findViewById(R.id.search_result_count);
        TextView genreCue = root.findViewById(R.id.search_genre_scope);
        TextView emptyTitle = root.findViewById(R.id.search_empty_title);
        TextView emptySubtitle = root.findViewById(R.id.search_empty_subtitle);
        View clearSearch = root.findViewById(R.id.search_empty_clear);
        View clearGenre = root.findViewById(R.id.search_empty_clear_genre);
        ChipGroup chipGroup = root.findViewById(R.id.search_suggestion_chips);
        Context context = root.getContext();

        boolean hasQuery = !query.trim().isEmpty();
        boolean hasGenre = genreLabel != null && !genreLabel.trim().isEmpty();

        if (!searchExpanded) {
            if (statusRow != null) {
                statusRow.setVisibility(View.GONE);
            }
            if (suggestions != null) {
                suggestions.setVisibility(View.GONE);
            }
            if (empty != null) {
                empty.setVisibility(View.GONE);
                empty.setClickable(false);
                empty.setFocusable(false);
            }
            return;
        }

        if (!hasQuery) {
            if (statusRow != null) {
                statusRow.setVisibility(View.GONE);
            }
            if (empty != null) {
                empty.setVisibility(View.GONE);
                empty.setClickable(false);
                empty.setFocusable(false);
            }
            if (suggestions != null) {
                List<String> chips = mergeSuggestions(recent, popularAuthors);
                if (chips.isEmpty()) {
                    suggestions.setVisibility(View.GONE);
                } else {
                    suggestions.setVisibility(View.VISIBLE);
                    bindSuggestionChips(chipGroup, recent, popularAuthors, callbacks);
                }
            }
            applyTheme(root);
            return;
        }

        if (suggestions != null) {
            suggestions.setVisibility(View.GONE);
        }

        if (statusRow != null) {
            statusRow.setVisibility(View.VISIBLE);
        }
        if (countView != null) {
            countView.setText(context.getResources().getQuantityString(
                    R.plurals.search_result_count, resultCount, resultCount));
        }
        if (genreCue != null) {
            genreCue.setClickable(false);
            genreCue.setFocusable(false);
            genreCue.setOnClickListener(null);
            if (hasGenre) {
                genreCue.setVisibility(View.VISIBLE);
                genreCue.setText(context.getString(R.string.search_in_genre, genreLabel));
            } else {
                genreCue.setVisibility(View.GONE);
            }
        }

        if (empty != null) {
            if (resultCount == 0) {
                empty.setVisibility(View.VISIBLE);
                empty.setClickable(true);
                empty.setFocusable(true);
                if (emptyTitle != null) {
                    emptyTitle.setText(context.getString(R.string.search_no_results_title, query.trim()));
                }
                if (emptySubtitle != null) {
                    emptySubtitle.setText(hasGenre
                            ? context.getString(R.string.search_no_results_subtitle_genre)
                            : context.getString(R.string.search_no_results_subtitle));
                }
                if (clearSearch != null) {
                    clearSearch.setOnClickListener(v -> callbacks.onClearSearch());
                }
                if (clearGenre != null) {
                    // Genre can be changed via the chips; no separate "All genres" action.
                    clearGenre.setVisibility(View.GONE);
                    clearGenre.setOnClickListener(null);
                }
            } else {
                empty.setVisibility(View.GONE);
                empty.setClickable(false);
                empty.setFocusable(false);
            }
        }

        applyTheme(root);
    }

    @NonNull
    private static List<String> mergeSuggestions(
            @NonNull List<String> recent,
            @NonNull List<String> popularAuthors
    ) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.addAll(recent);
        set.addAll(popularAuthors);
        return new ArrayList<>(set);
    }

    private static void bindSuggestionChips(
            @Nullable ChipGroup chipGroup,
            @NonNull List<String> recent,
            @NonNull List<String> popularAuthors,
            @NonNull Callbacks callbacks
    ) {
        if (chipGroup == null) {
            return;
        }
        chipGroup.removeAllViews();

        if (!recent.isEmpty()) {
            for (String item : recent) {
                addChip(chipGroup, item, callbacks);
            }
        }
        int addedPopular = 0;
        for (String author : popularAuthors) {
            boolean already = false;
            for (String r : recent) {
                if (r.equalsIgnoreCase(author)) {
                    already = true;
                    break;
                }
            }
            if (already) {
                continue;
            }
            addChip(chipGroup, author, callbacks);
            addedPopular++;
            if (addedPopular >= 6) {
                break;
            }
        }
        themeSuggestionChips(chipGroup);
    }

    private static void addChip(
            @NonNull ChipGroup chipGroup,
            @NonNull String label,
            @NonNull Callbacks callbacks
    ) {
        Chip chip = new Chip(chipGroup.getContext());
        chip.setText(label);
        chip.setCheckable(false);
        chip.setClickable(true);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setChipCornerRadius(chipGroup.getResources().getDimension(R.dimen.space_16));
        chip.setOnClickListener(v -> callbacks.onSuggestionSelected(label));
        chipGroup.addView(chip);
    }

    private static void themeSuggestionChips(@Nullable ChipGroup chips) {
        if (chips == null) {
            return;
        }
        Context context = chips.getContext();
        int accent = MyColor.getAccentColor(context);
        int surface = MyColor.getElevatedSurfaceColor(context);
        int text = MyColor.getTitleTextColor(context);
        int stroke = MyColor.getSeparatorColor(context);
        for (int i = 0; i < chips.getChildCount(); i++) {
            View child = chips.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(surface));
                chip.setTextColor(text);
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(stroke));
                chip.setChipStrokeWidth(context.getResources().getDimension(R.dimen.chip_stroke));
                chip.setRippleColor(android.content.res.ColorStateList.valueOf(accent));
            }
        }
    }
}
