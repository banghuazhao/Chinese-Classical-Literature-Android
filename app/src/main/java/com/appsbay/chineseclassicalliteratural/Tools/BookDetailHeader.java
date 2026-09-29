package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;

public final class BookDetailHeader {
    private BookDetailHeader() {
    }

    public static void bind(View header, Book book, int contentsLabelRes) {
        bind(header, book, contentsLabelRes, null);
    }

    public static void bind(View header, Book book, int contentsLabelRes,
                            @Nullable View.OnClickListener continueClick) {
        if (header == null || book == null) {
            return;
        }
        Context context = header.getContext();

        ImageView coverView = header.findViewById(R.id.book_detail_cover);
        TextView titleView = header.findViewById(R.id.book_detail_title);
        TextView authorView = header.findViewById(R.id.book_detail_author);
        TextView eyebrowView = header.findViewById(R.id.book_detail_eyebrow);
        TextView progressText = header.findViewById(R.id.book_detail_progress_text);
        ProgressBar progressBar = header.findViewById(R.id.book_detail_progress);
        MaterialButton continueButton = header.findViewById(R.id.book_detail_continue);
        TextView tocLabel = header.findViewById(R.id.book_detail_toc_label);
        View tocRule = header.findViewById(R.id.book_detail_toc_rule);

        int titleColor = MyColor.getTitleTextColor(context);
        int mutedColor = MyColor.getDetailTextColor(context);
        int accentColor = MyColor.getAccentColor(context);

        if (eyebrowView != null) {
            eyebrowView.setTextColor(accentColor);
        }

        if (titleView != null) {
            titleView.setText(book.getName());
            titleView.setTextColor(titleColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                titleView.setAccessibilityHeading(true);
            }
        }

        bindAuthor(authorView, book, accentColor, mutedColor);
        ReadingProgressHelper.bindProgressRow(context, book, progressText, progressBar);
        bindContinueButton(continueButton, book, accentColor, continueClick);

        if (tocLabel != null) {
            if (contentsLabelRes != 0) {
                tocLabel.setVisibility(View.VISIBLE);
                tocLabel.setText(contentsLabelRes);
                tocLabel.setTextColor(accentColor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    tocLabel.setAccessibilityHeading(true);
                }
            } else {
                tocLabel.setVisibility(View.GONE);
            }
        }
        if (tocRule != null) {
            tocRule.setBackgroundColor(accentColor);
            tocRule.setVisibility(contentsLabelRes != 0 ? View.VISIBLE : View.GONE);
        }

        bindCover(coverView, book);
        bindCoverTap(header, book);
        BookMotion.revealOnce(header);
    }

    private static void bindCoverTap(View header, Book book) {
        View coverCard = header.findViewById(R.id.book_detail_cover_card);
        View target = coverCard != null ? coverCard : header.findViewById(R.id.book_detail_cover);
        if (target == null || book == null) {
            return;
        }
        Context context = header.getContext();
        target.setClickable(true);
        target.setFocusable(true);
        target.setContentDescription(context.getString(R.string.view_book_cover));
        BookMotion.attachPressEffect(target);
        target.setOnClickListener(v -> BookCoverViewer.show(context, book));
    }

    private static void bindContinueButton(MaterialButton button, Book book, int accentColor,
                                           @Nullable View.OnClickListener continueClick) {
        if (button == null) {
            return;
        }
        if (continueClick == null) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }

        Context context = button.getContext();
        boolean hasProgress = ReadingProgressHelper.getChapterIndex(context, book.getName()) >= 0;
        button.setVisibility(View.VISIBLE);
        button.setText(hasProgress ? R.string.continue_reading : R.string.Begin_Reading);
        button.setTextColor(accentColor);
        button.setStrokeColor(ColorStateList.valueOf(accentColor));
        button.setBackgroundTintList(ColorStateList.valueOf(MyColor.getElevatedSurfaceColor(context)));
        button.setRippleColor(ColorStateList.valueOf(MyColor.getSeparatorColor(context)));
        button.setOnClickListener(continueClick);
    }

    private static void bindAuthor(TextView authorView, Book book, int accentColor, int mutedColor) {
        if (authorView == null) {
            return;
        }
        Context context = authorView.getContext();
        authorView.setText(book.getAuthor());

        String bio = LiteraryCopy.shared.authorBioFor(book);
        boolean tappable = bio != null && !bio.trim().isEmpty();
        if (tappable) {
            authorView.setTextColor(accentColor);
            authorView.setClickable(true);
            authorView.setFocusable(true);
            authorView.setBackgroundResource(R.drawable.bg_row_ripple);
            authorView.setContentDescription(context.getString(R.string.about_author_named, book.getAuthor()));
            authorView.setOnClickListener(v -> AuthorBioSheet.show(context, book.getAuthor(), bio));
        } else {
            authorView.setTextColor(mutedColor);
            authorView.setClickable(false);
            authorView.setFocusable(false);
            authorView.setBackground(null);
            authorView.setContentDescription(book.getAuthor());
            authorView.setOnClickListener(null);
        }
    }

    private static void bindCover(ImageView coverView, Book book) {
        loadCover(coverView, book);
    }

    public static void loadCover(ImageView coverView, Book book) {
        if (coverView == null || book == null) {
            return;
        }
        Context context = coverView.getContext();
        String coverName = book.getBookCover();
        if (coverName == null || coverName.isEmpty()) {
            coverView.setImageResource(R.drawable.cover_placeholder);
            return;
        }
        coverView.setContentDescription(book.getName());
        try {
            InputStream ims = context.getAssets().open("covers/" + coverName + ".png");
            Drawable d = Drawable.createFromStream(ims, null);
            coverView.setImageDrawable(d);
            ims.close();
        } catch (IOException ex) {
            coverView.setImageResource(R.drawable.cover_placeholder);
        }
    }
}
