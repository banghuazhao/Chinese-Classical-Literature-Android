package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

import com.appsbay.chineseclassicalliteratural.R;

/**
 * Shared AppCompat SearchView styling and submit/IME helpers.
 */
public final class SearchChrome {

    public interface QueryListener {
        void onQueryChanged(@NonNull String query);

        void onQuerySubmitted(@NonNull String query);

        void onClosed();
    }

    private SearchChrome() {
    }

    public static void style(@NonNull SearchView searchView, @NonNull Context context) {
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setQueryHint(context.getString(R.string.search_books_hint));

        int iconColor = MyColor.getTitleTextColor(context);

        EditText text = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (text != null) {
            text.setTextColor(iconColor);
            text.setHintTextColor(MyColor.getDetailTextColor(context));
            text.setTextSize(16f);
        }

        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_button);
        if (searchIcon != null) {
            searchIcon.setImageDrawable(MyImage.changeDrawableColor(
                    context, R.drawable.ic_search, iconColor));
            searchIcon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeIcon != null) {
            closeIcon.setImageDrawable(MyImage.changeDrawableColor(
                    context, R.drawable.nav_close, iconColor));
            closeIcon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        ImageView magIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (magIcon != null) {
            magIcon.setImageDrawable(MyImage.changeDrawableColor(
                    context, R.drawable.ic_search, iconColor));
            magIcon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        ImageView goIcon = searchView.findViewById(androidx.appcompat.R.id.search_go_btn);
        if (goIcon != null) {
            goIcon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        searchView.setSubmitButtonEnabled(false);
    }

    public static void tintMenuIcon(@NonNull android.view.MenuItem item, @NonNull Context context) {
        item.setIcon(MyImage.changeDrawableColor(
                context, R.drawable.ic_search, MyColor.getTitleTextColor(context)));
    }

    public static void bind(
            @NonNull SearchView searchView,
            @NonNull Context context,
            @Nullable String initialQuery,
            @NonNull QueryListener listener
    ) {
        style(searchView, context);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String q = query == null ? "" : query.trim();
                if (!q.isEmpty()) {
                    SearchHistory.add(context, q);
                }
                listener.onQuerySubmitted(q);
                dismissKeyboard(searchView);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                listener.onQueryChanged(newText == null ? "" : newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            listener.onClosed();
            return false;
        });

        if (initialQuery != null && !initialQuery.isEmpty()) {
            searchView.setQuery(initialQuery, false);
        }
    }

    public static void restoreQuery(@Nullable SearchView searchView, @Nullable String query) {
        if (searchView == null) {
            return;
        }
        String q = query == null ? "" : query;
        CharSequence current = searchView.getQuery();
        if (current != null && q.contentEquals(current)) {
            return;
        }
        searchView.setQuery(q, false);
    }

    public static void dismissKeyboard(@NonNull View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
