package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.R;

/**
 * Full-screen book cover preview with drag-to-dismiss, close, and about-book info.
 */
public final class BookCoverViewer {

    private static final float DISMISS_DISTANCE_DP = 120f;
    private static final float DISMISS_VELOCITY_DP = 900f;

    private BookCoverViewer() {
    }

    public static void show(@NonNull Context context, @NonNull Book book) {
        Dialog dialog = new Dialog(context, R.style.Theme_NovelsHub_CoverViewer);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_book_cover_viewer, null, false);
        dialog.setContentView(content);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.setAttributes(params);
            }
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }

        View root = content.findViewById(R.id.cover_viewer_root);
        ImageView image = content.findViewById(R.id.cover_viewer_image);
        View toolbar = content.findViewById(R.id.cover_viewer_toolbar);
        ImageButton close = content.findViewById(R.id.cover_viewer_close);
        ImageButton info = content.findViewById(R.id.cover_viewer_info);

        BookDetailHeader.loadCover(image, book);
        image.setContentDescription(book.getName());

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    bars.top + Math.round(dp(toolbar, 8f)),
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom());
            image.setPadding(
                    image.getPaddingLeft(),
                    bars.top + Math.round(dp(image, 56f)),
                    image.getPaddingRight(),
                    bars.bottom + Math.round(dp(image, 24f)));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        close.setOnClickListener(v -> dialog.dismiss());

        boolean showInfo = AuthorBioSheet.hasBookIntro(book);
        info.setVisibility(showInfo ? View.VISIBLE : View.GONE);
        if (showInfo) {
            info.setOnClickListener(v -> AuthorBioSheet.showBook(context, book));
        }

        // Drag on the image; toolbar buttons keep their own click handling.
        attachDragToDismiss(root, image, toolbar, dialog);

        dialog.setCancelable(true);
        dialog.show();
    }

    private static void attachDragToDismiss(@NonNull View root,
                                            @NonNull View image,
                                            @NonNull View toolbar,
                                            @NonNull Dialog dialog) {
        final float touchSlop = ViewConfiguration.get(root.getContext()).getScaledTouchSlop();
        final float dismissDistance = dp(root, DISMISS_DISTANCE_DP);
        final float dismissVelocity = dp(root, DISMISS_VELOCITY_DP);
        final float density = root.getResources().getDisplayMetrics().density;

        View.OnTouchListener dragListener = new View.OnTouchListener() {
            float downY;
            float lastY;
            long lastTime;
            float velocityY;
            boolean dragging;
            boolean settled;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = event.getRawY();
                        lastY = downY;
                        lastTime = event.getEventTime();
                        velocityY = 0f;
                        dragging = false;
                        settled = false;
                        image.animate().cancel();
                        toolbar.animate().cancel();
                        root.animate().cancel();
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dy = event.getRawY() - downY;
                        long now = event.getEventTime();
                        long dt = Math.max(1L, now - lastTime);
                        velocityY = (event.getRawY() - lastY) / dt * 1000f;
                        lastY = event.getRawY();
                        lastTime = now;

                        if (!dragging && Math.abs(dy) > touchSlop) {
                            dragging = true;
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        if (!dragging) {
                            return true;
                        }

                        float offset = Math.max(0f, dy);
                        float progress = Math.min(1f, offset / (dismissDistance * 1.6f));
                        image.setTranslationY(offset);
                        toolbar.setAlpha(1f - progress);
                        root.setAlpha(1f - progress * 0.55f);
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        if (settled) {
                            return true;
                        }
                        settled = true;
                        float offset = Math.max(0f, image.getTranslationY());
                        boolean shouldDismiss = offset > dismissDistance
                                || (offset > touchSlop * 2f && velocityY > dismissVelocity);
                        if (shouldDismiss) {
                            dismissWithMotion(root, image, toolbar, dialog, velocityY, density);
                        } else {
                            settleBack(root, image, toolbar);
                        }
                        return true;
                    }
                    default:
                        return false;
                }
            }
        };
        image.setOnTouchListener(dragListener);
    }

    private static void dismissWithMotion(@NonNull View root,
                                          @NonNull View image,
                                          @NonNull View toolbar,
                                          @NonNull Dialog dialog,
                                          float velocityY,
                                          float density) {
        if (!BookMotion.animationsEnabled(root.getContext())) {
            dialog.dismiss();
            return;
        }
        float extra = Math.max(dp(root, 160f), Math.abs(velocityY) / density * 0.08f);
        long duration = Math.max(120L, Math.min(220L, (long) (180f - Math.abs(velocityY) / 40f)));
        image.animate()
                .translationY(image.getTranslationY() + extra)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        toolbar.animate()
                .alpha(0f)
                .setDuration(duration)
                .start();
        root.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction(dialog::dismiss)
                .start();
    }

    private static void settleBack(@NonNull View root, @NonNull View image, @NonNull View toolbar) {
        if (!BookMotion.animationsEnabled(root.getContext())) {
            image.setTranslationY(0f);
            toolbar.setAlpha(1f);
            root.setAlpha(1f);
            return;
        }
        image.animate()
                .translationY(0f)
                .setDuration(200L)
                .setInterpolator(new DecelerateInterpolator(1.8f))
                .start();
        toolbar.animate()
                .alpha(1f)
                .setDuration(200L)
                .setInterpolator(new DecelerateInterpolator(1.8f))
                .start();
        root.animate()
                .alpha(1f)
                .setDuration(200L)
                .setInterpolator(new DecelerateInterpolator(1.8f))
                .start();
    }

    private static float dp(@NonNull View view, float value) {
        return value * view.getResources().getDisplayMetrics().density;
    }
}
