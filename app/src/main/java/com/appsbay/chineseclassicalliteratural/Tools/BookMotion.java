package com.appsbay.chineseclassicalliteratural.Tools;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import com.appsbay.chineseclassicalliteratural.R;

/**
 * Small, purposeful motion primitives shared by the catalog and reader.
 * Android's global animator setting is honored so reduced/disabled motion stays disabled.
 */
public final class BookMotion {

    private static final long ENTER_DURATION_MS = 240L;
    private static final long EXIT_DURATION_MS = 110L;

    private BookMotion() {
    }

    public static boolean animationsEnabled(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        try {
            return Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
            ) > 0f;
        } catch (Exception ignored) {
            return true;
        }
    }

    public static void revealOnce(@NonNull View view) {
        if (Boolean.TRUE.equals(view.getTag(R.id.tag_book_motion_revealed))) {
            return;
        }
        view.setTag(R.id.tag_book_motion_revealed, true);
        if (!animationsEnabled(view.getContext())) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }

        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(dp(view, 12f));
        view.post(() -> view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ENTER_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator(1.8f))
                .start());
    }

    public static void attachPressEffect(@NonNull View view) {
        if (Boolean.TRUE.equals(view.getTag(R.id.tag_book_motion_press))) {
            return;
        }
        view.setTag(R.id.tag_book_motion_press, true);
        view.setOnTouchListener((target, event) -> {
            if (!target.isEnabled() || !animationsEnabled(target.getContext())) {
                return false;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    target.animate().cancel();
                    target.animate()
                            .scaleX(0.985f)
                            .scaleY(0.985f)
                            .setDuration(80L)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    target.animate().cancel();
                    target.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150L)
                            .setInterpolator(new DecelerateInterpolator(1.8f))
                            .start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    public static void swapPage(@NonNull View page, boolean previous,
                                @NonNull Runnable swapContent) {
        if (!animationsEnabled(page.getContext())) {
            swapContent.run();
            page.setAlpha(1f);
            page.setTranslationX(0f);
            return;
        }

        float exitDistance = dp(page, previous ? 24f : -24f);
        page.animate().cancel();
        page.animate()
                .alpha(0f)
                .translationX(exitDistance)
                .setDuration(EXIT_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator(1.4f))
                .withEndAction(() -> {
                    swapContent.run();
                    page.setTranslationX(-exitDistance);
                    page.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(ENTER_DURATION_MS)
                            .setInterpolator(new DecelerateInterpolator(1.8f))
                            .start();
                })
                .start();
    }

    private static float dp(@NonNull View view, float value) {
        return value * view.getResources().getDisplayMetrics().density;
    }
}
