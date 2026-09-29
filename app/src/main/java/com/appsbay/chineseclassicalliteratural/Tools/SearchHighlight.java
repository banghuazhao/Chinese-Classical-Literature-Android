package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.R;

import java.util.Locale;

/**
 * Title/author highlight plus match-reason cue for search results.
 */
public final class SearchHighlight {

    private SearchHighlight() {
    }

    public static void bindListRow(
            @NonNull Context context,
            @NonNull Book book,
            @Nullable String rawQuery,
            @NonNull TextView titleView,
            @NonNull TextView authorView,
            @Nullable TextView matchReasonView
    ) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        String name = book.getName() == null ? "" : book.getName();
        String author = book.getAuthor() == null ? "" : book.getAuthor();

        titleView.setTextColor(MyColor.getTitleTextColor(context));
        authorView.setTextColor(MyColor.getDetailTextColor(context));

        if (query.isEmpty()) {
            titleView.setText(name);
            authorView.setText(author);
            if (matchReasonView != null) {
                matchReasonView.setVisibility(View.GONE);
            }
            return;
        }

        highlight(context, titleView, name, query);
        highlight(context, authorView, author, query);

        LiteraryCopy.MatchKind kind = LiteraryCopy.shared.resolveMatch(book, query);
        if (matchReasonView == null) {
            return;
        }
        if (kind == LiteraryCopy.MatchKind.INTRODUCTION
                || kind == LiteraryCopy.MatchKind.AUTHOR_BIO) {
            matchReasonView.setVisibility(View.VISIBLE);
            matchReasonView.setTextColor(MyColor.getAccentColor(context));
            matchReasonView.setText(labelFor(context, kind));
        } else {
            matchReasonView.setVisibility(View.GONE);
        }
    }

    public static void bindTitleOnly(
            @NonNull Context context,
            @NonNull Book book,
            @Nullable String rawQuery,
            @NonNull TextView titleView,
            @Nullable TextView matchReasonView
    ) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        String name = book.getName() == null ? "" : book.getName();
        titleView.setTextColor(MyColor.getTitleTextColor(context));

        if (query.isEmpty()) {
            titleView.setText(name);
            if (matchReasonView != null) {
                matchReasonView.setVisibility(View.GONE);
            }
            return;
        }

        highlight(context, titleView, name, query);
        LiteraryCopy.MatchKind kind = LiteraryCopy.shared.resolveMatch(book, query);
        if (matchReasonView == null) {
            return;
        }
        if (kind == LiteraryCopy.MatchKind.INTRODUCTION
                || kind == LiteraryCopy.MatchKind.AUTHOR_BIO
                || kind == LiteraryCopy.MatchKind.AUTHOR) {
            matchReasonView.setVisibility(View.VISIBLE);
            matchReasonView.setTextColor(MyColor.getAccentColor(context));
            if (kind == LiteraryCopy.MatchKind.AUTHOR) {
                matchReasonView.setText(book.getAuthor());
            } else {
                matchReasonView.setText(labelFor(context, kind));
            }
        } else {
            matchReasonView.setVisibility(View.GONE);
        }
    }

    @NonNull
    public static String labelFor(@NonNull Context context, @NonNull LiteraryCopy.MatchKind kind) {
        switch (kind) {
            case INTRODUCTION:
                return context.getString(R.string.search_match_introduction);
            case AUTHOR_BIO:
                return context.getString(R.string.search_match_author_bio);
            case AUTHOR:
                return context.getString(R.string.search_match_author);
            case TITLE:
            default:
                return context.getString(R.string.search_match_title);
        }
    }

    private static void highlight(
            @NonNull Context context,
            @NonNull TextView target,
            @NonNull String full,
            @NonNull String query
    ) {
        int startPos = full.toLowerCase(Locale.US).indexOf(query.toLowerCase(Locale.US));
        if (startPos < 0) {
            target.setText(full);
            return;
        }
        int endPos = startPos + query.length();
        Spannable spannable = new SpannableString(full);
        ColorStateList accent = new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{MyColor.getAccentColor(context)}
        );
        TextAppearanceSpan highlightSpan = new TextAppearanceSpan(null, Typeface.BOLD, -1, accent, null);
        spannable.setSpan(highlightSpan, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        target.setText(spannable);
    }
}
