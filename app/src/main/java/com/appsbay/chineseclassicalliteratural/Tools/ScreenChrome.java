package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * In-layout AppBar + edge-to-edge insets. Every screen should call {@link #setup}
 * or {@link #setupHome} after {@code setContentView}.
 */
public final class ScreenChrome {

    private ScreenChrome() {
    }

    public static void setup(@NonNull AppCompatActivity activity,
                             @NonNull View root,
                             @Nullable View bottomInsetTarget) {
        attach(activity, root, bottomInsetTarget, true);
    }

    public static void setupHome(@NonNull AppCompatActivity activity,
                                 @NonNull MaterialToolbar toolbar,
                                 @NonNull View root,
                                 @NonNull View bottomInsetTarget,
                                 @Nullable View hairline) {
        attach(activity, root, bottomInsetTarget, false);
    }

    public static void tint(@NonNull AppCompatActivity activity) {
        View appBar = activity.findViewById(R.id.app_bar);
        if (appBar != null) {
            appBar.setBackgroundColor(MyColor.getActionBarColor(activity));
        }
        View toolbarView = activity.findViewById(R.id.toolbar);
        if (toolbarView instanceof MaterialToolbar) {
            MaterialToolbar toolbar = (MaterialToolbar) toolbarView;
            toolbar.setBackgroundColor(MyColor.getActionBarColor(activity));
            toolbar.setTitleTextColor(MyColor.getTitleTextColor(activity));
            Drawable nav = toolbar.getNavigationIcon();
            if (nav != null) {
                nav.mutate().setColorFilter(MyColor.getButtonTintColor(activity), PorterDuff.Mode.SRC_IN);
            }
        }
        View hairline = activity.findViewById(R.id.toolbar_hairline);
        if (hairline != null) {
            hairline.setBackgroundColor(MyColor.getSeparatorColor(activity));
        }
        View bottomNav = activity.findViewById(R.id.bottom_navigation_main);
        if (bottomNav != null) {
            bottomNav.setBackgroundColor(MyColor.getBottomBarColor(activity));
        }
        applyBarAppearance(activity);
    }

    public static void tintHomeChrome(@NonNull AppCompatActivity activity) {
        tint(activity);
    }

    public static void tintActionBarContainer(@NonNull AppCompatActivity activity) {
        tint(activity);
    }

    public static void applyToolbarColors(@NonNull AppCompatActivity activity,
                                          @NonNull MaterialToolbar toolbar,
                                          @Nullable View hairline) {
        applyToolbarColors(activity, toolbar, hairline, true);
    }

    private static void attach(@NonNull AppCompatActivity activity,
                               @NonNull View root,
                               @Nullable View bottomInsetTarget,
                               boolean showUp) {
        prepareEdgeToEdge(activity);
        MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
        View hairline = activity.findViewById(R.id.toolbar_hairline);
        View appBar = activity.findViewById(R.id.app_bar);
        if (toolbar != null) {
            activity.setSupportActionBar(toolbar);
            applyToolbarColors(activity, toolbar, hairline, showUp);
            if (showUp) {
                toolbar.setNavigationOnClickListener(v ->
                        activity.getOnBackPressedDispatcher().onBackPressed());
            }
        }
        View top = appBar != null ? appBar : toolbar;
        View bottom = bottomInsetTarget != null ? bottomInsetTarget : root;
        if (top != null) {
            attachInsets(root, top, bottom);
        }
        applyBarAppearance(activity);
    }

    private static void attachInsets(@NonNull View root,
                                     @NonNull View topInsetTarget,
                                     @NonNull View bottomInsetTarget) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int top = Math.max(bars.top, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top);
            if (top == 0) {
                top = statusBarHeight(v.getContext());
            }
            v.setPadding(bars.left, 0, bars.right, 0);
            applyExtraPadding(topInsetTarget, top, 0);
            applyExtraPadding(bottomInsetTarget, 0, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
        root.post(() -> ViewCompat.requestApplyInsets(root));
    }

    private static void prepareEdgeToEdge(@NonNull AppCompatActivity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    private static void applyBarAppearance(@NonNull AppCompatActivity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        boolean lightIcons = !MyColor.isDark(activity);
        controller.setAppearanceLightStatusBars(lightIcons);
        controller.setAppearanceLightNavigationBars(lightIcons);
    }

    private static void applyToolbarColors(@NonNull AppCompatActivity activity,
                                           @NonNull MaterialToolbar toolbar,
                                           @Nullable View hairline,
                                           boolean applyBackIcon) {
        toolbar.setBackgroundColor(MyColor.getActionBarColor(activity));
        toolbar.setTitleTextColor(MyColor.getTitleTextColor(activity));
        if (applyBackIcon) {
            toolbar.setNavigationIcon(MyImage.changeDrawableColor(
                    activity,
                    androidx.appcompat.R.drawable.abc_ic_ab_back_material,
                    MyColor.getButtonTintColor(activity)
            ));
        }
        if (hairline != null) {
            hairline.setBackgroundColor(MyColor.getSeparatorColor(activity));
        }
    }

    private static void applyExtraPadding(@NonNull View view, int extraTop, int extraBottom) {
        Integer baseTop = (Integer) view.getTag(R.id.tag_inset_padding_top);
        Integer baseBottom = (Integer) view.getTag(R.id.tag_inset_padding_bottom);
        if (baseTop == null) {
            baseTop = view.getPaddingTop();
            view.setTag(R.id.tag_inset_padding_top, baseTop);
        }
        if (baseBottom == null) {
            baseBottom = view.getPaddingBottom();
            view.setTag(R.id.tag_inset_padding_bottom, baseBottom);
        }
        view.setPadding(
                view.getPaddingLeft(),
                baseTop + extraTop,
                view.getPaddingRight(),
                baseBottom + extraBottom
        );
    }

    private static int statusBarHeight(@NonNull Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return Math.round(24f * context.getResources().getDisplayMetrics().density);
    }
}
