package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * Shared Material dialog / sheet / snackbar chrome aligned with the paper theme
 * and the user's selected light/dark preference ({@link MyColor}).
 */
public final class DialogChrome {

    private DialogChrome() {
    }

    @NonNull
    public static MaterialAlertDialogBuilder alert(@NonNull Context context) {
        return new MaterialAlertDialogBuilder(context, R.style.Theme_NovelsHub_AlertDialog);
    }

    @NonNull
    public static BottomSheetDialog bottomSheet(@NonNull Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Theme_NovelsHub_BottomSheet);
        dialog.setDismissWithAnimation(true);
        return dialog;
    }

    public static void styleAlertDialog(@NonNull android.app.Dialog dialog, @NonNull Context context) {
        if (!(dialog instanceof androidx.appcompat.app.AlertDialog)) {
            return;
        }
        androidx.appcompat.app.AlertDialog alert = (androidx.appcompat.app.AlertDialog) dialog;
        int surface = MyColor.getElevatedSurfaceColor(context);
        int ink = MyColor.getTitleTextColor(context);
        int muted = MyColor.getDetailTextColor(context);
        int accent = MyColor.getAccentColor(context);

        Window window = alert.getWindow();
        if (window != null && window.getDecorView() != null) {
            View root = window.getDecorView();
            root.setBackgroundColor(surface);
        }

        int titleId = com.google.android.material.R.id.alertTitle;
        TextView title = alert.findViewById(titleId);
        if (title == null) {
            title = alert.findViewById(androidx.appcompat.R.id.alertTitle);
        }
        if (title != null) {
            title.setTextColor(ink);
        }
        TextView message = alert.findViewById(android.R.id.message);
        if (message != null) {
            message.setTextColor(muted);
        }

        if (alert.getButton(android.content.DialogInterface.BUTTON_POSITIVE) != null) {
            alert.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(accent);
        }
        if (alert.getButton(android.content.DialogInterface.BUTTON_NEGATIVE) != null) {
            alert.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(accent);
        }
        if (alert.getButton(android.content.DialogInterface.BUTTON_NEUTRAL) != null) {
            alert.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).setTextColor(accent);
        }
    }

    public static void prepareSheet(@NonNull BottomSheetDialog dialog,
                                    @NonNull View content,
                                    @NonNull Context context) {
        int paper = MyColor.getBackgroundColor(context);
        int elevated = MyColor.getElevatedSurfaceColor(context);
        content.setBackgroundColor(elevated);
        dialog.setContentView(content);
        dialog.setOnShowListener(d -> {
            FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) {
                return;
            }
            GradientDrawable background = new GradientDrawable();
            background.setColor(elevated);
            float radius = content.getResources().getDimension(R.dimen.space_20);
            background.setCornerRadii(new float[]{
                    radius, radius, radius, radius, 0, 0, 0, 0
            });
            sheet.setBackground(background);
            sheet.setBackgroundTintList(null);

            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setDraggable(true);
        });
        Window window = dialog.getWindow();
        if (window != null) {
            window.setNavigationBarColor(paper);
        }
    }

    public static void tintSheetHandle(@NonNull View handle, @NonNull Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(MyColor.getDetailTextColor(context));
        shape.setCornerRadius(handle.getResources().getDimension(R.dimen.space_4));
        shape.setAlpha(140);
        handle.setBackground(shape);
    }

    public static void tintSheetAction(@NonNull MaterialButton button, @NonNull Context context) {
        int ink = MyColor.getTitleTextColor(context);
        int ripple = MyColor.getSeparatorColor(context);
        button.setTextColor(ink);
        button.setIconTint(ColorStateList.valueOf(MyColor.getAccentColor(context)));
        button.setRippleColor(ColorStateList.valueOf(ripple));
        button.setBackgroundTintList(ColorStateList.valueOf(MyColor.getElevatedSurfaceColor(context)));
    }

    public static void snack(@NonNull View anchor, @StringRes int messageRes) {
        snack(anchor, anchor.getContext().getString(messageRes));
    }

    public static void snack(@NonNull View anchor, @NonNull CharSequence message) {
        snack(anchor, message, Snackbar.LENGTH_SHORT);
    }

    public static void snack(@NonNull View anchor,
                             @NonNull CharSequence message,
                             int duration) {
        if (anchor.getWindowToken() == null) {
            return;
        }
        Context context = anchor.getContext();
        Snackbar snackbar = Snackbar.make(anchor, message, duration);
        snackbar.setBackgroundTint(MyColor.getElevatedSurfaceColor(context));
        snackbar.setTextColor(MyColor.getTitleTextColor(context));
        snackbar.setActionTextColor(MyColor.getAccentColor(context));
        View snackView = snackbar.getView();
        snackView.setElevation(snackView.getResources().getDimension(R.dimen.space_8));
        snackbar.show();
    }

    public static void snackLong(@NonNull View anchor, @StringRes int messageRes) {
        snack(anchor, anchor.getContext().getString(messageRes), Snackbar.LENGTH_LONG);
    }

    @Nullable
    public static View activityAnchor(@Nullable Context context) {
        if (!(context instanceof android.app.Activity)) {
            return null;
        }
        android.app.Activity activity = (android.app.Activity) context;
        View content = activity.findViewById(android.R.id.content);
        return content != null ? content : activity.getWindow().getDecorView();
    }
}
