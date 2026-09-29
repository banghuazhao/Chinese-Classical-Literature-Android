package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public final class AuthorBioSheet {
    private AuthorBioSheet() {
    }

    public static void show(Context context, String authorName, String bio) {
        if (context == null) {
            return;
        }
        show(context, context.getString(R.string.About_the_author), authorName, bio);
    }

    public static void showBook(Context context, Book book) {
        if (context == null || book == null) {
            return;
        }
        show(context, context.getString(R.string.About_this_book), book.getName(),
                LiteraryCopy.shared.introductionFor(book));
    }

    public static boolean hasBookIntro(Book book) {
        String intro = LiteraryCopy.shared.introductionFor(book);
        return intro != null && !intro.trim().isEmpty();
    }

    private static void show(Context context, String labelText, String title, String body) {
        if (context == null || body == null || body.trim().isEmpty()) {
            return;
        }

        BottomSheetDialog dialog = DialogChrome.bottomSheet(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_author_bio, null, false);

        int paper = MyColor.getBackgroundColor(context);
        int elevated = MyColor.getElevatedSurfaceColor(context);
        int ink = MyColor.getTitleTextColor(context);
        int accent = MyColor.getAccentColor(context);

        View root = view.findViewById(R.id.author_sheet_root);
        View handle = view.findViewById(R.id.author_sheet_handle);
        TextView label = view.findViewById(R.id.author_sheet_label);
        TextView name = view.findViewById(R.id.author_sheet_name);
        View rule = view.findViewById(R.id.author_sheet_rule);
        TextView bioView = view.findViewById(R.id.author_sheet_bio);

        root.setBackgroundColor(elevated);
        DialogChrome.tintSheetHandle(handle, context);
        label.setText(labelText);
        label.setTextColor(accent);
        name.setText(title);
        name.setTextColor(ink);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            name.setAccessibilityHeading(true);
            label.setAccessibilityHeading(true);
        }
        rule.setBackgroundColor(accent);
        bioView.setText(body.trim());
        bioView.setTextColor(ink);

        dialog.setContentView(view);
        dialog.setOnShowListener(d -> {
            FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) {
                return;
            }
            sheet.setBackgroundColor(elevated);
            sheet.setBackgroundTintList(null);

            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
            behavior.setSkipCollapsed(true);
            behavior.setFitToContents(true);
            behavior.setMaxHeight((int) (metrics.heightPixels * 0.85f));
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setDraggable(true);
        });
        if (dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(paper);
        }
        dialog.show();
    }
}
