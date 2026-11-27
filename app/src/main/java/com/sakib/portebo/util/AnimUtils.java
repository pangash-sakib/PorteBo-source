package com.sakib.portebo.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.view.HapticFeedbackConstants;
import android.view.animation.OvershootInterpolator;

public class AnimUtils {

    public static boolean areAnimationsEnabled(Context context) {
        if (context == null) return false;
        try {
            float scale = Settings.Global.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            return scale > 0.05f;
        } catch (Exception e) {
            return true;
        }
    }

    public static void pulse(View view) {
        if (view == null) return;
        if (!areAnimationsEnabled(view.getContext())) return;
        view.animate()
            .scaleX(1.025f)
            .scaleY(1.025f)
            .setDuration(120)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(130)
                    .start();
            }).start();
    }

    public static void shake(View view) {
        if (view == null) return;
        if (!areAnimationsEnabled(view.getContext())) return;
        view.animate()
            .rotation(-14f)
            .setDuration(50)
            .withEndAction(() -> {
                view.animate().rotation(14f).setDuration(50).withEndAction(() -> {
                    view.animate().rotation(-8f).setDuration(40).withEndAction(() -> {
                        view.animate().rotation(0f).setDuration(40).start();
                    }).start();
                }).start();
            }).start();
    }

    public static void pressFeedback(View view, Runnable onEnd) {
        if (view == null) return;
        if (!areAnimationsEnabled(view.getContext())) {
            if (onEnd != null) onEnd.run();
            return;
        }
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        view.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(80)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(130)
                    .setInterpolator(new OvershootInterpolator(0.7f))
                    .withEndAction(() -> {
                        if (onEnd != null) onEnd.run();
                    }).start();
            }).start();
    }

    public static void rotate(View view, float degrees) {
        if (view == null) return;
        if (!areAnimationsEnabled(view.getContext())) {
            view.setRotation(degrees);
            return;
        }
        view.animate()
            .rotation(degrees)
            .setDuration(200)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    public static void slideIndicator(View indicator, float targetX, int targetWidth) {
        if (indicator == null) return;
        if (!areAnimationsEnabled(indicator.getContext())) {
            indicator.setTranslationX(targetX);
            if (targetWidth > 0) {
                indicator.getLayoutParams().width = targetWidth;
                indicator.requestLayout();
            }
            return;
        }
        indicator.animate()
            .translationX(targetX)
            .setDuration(180)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
        if (targetWidth > 0 && indicator.getWidth() != targetWidth) {
            ValueAnimator widthAnim = ValueAnimator.ofInt(indicator.getWidth(), targetWidth);
            widthAnim.setDuration(180);
            widthAnim.addUpdateListener(animation -> {
                indicator.getLayoutParams().width = (int) animation.getAnimatedValue();
                indicator.requestLayout();
            });
            widthAnim.start();
        }
    }

    public static void fadeInSlideUp(View view, int delayMs) {
        if (view == null) return;
        if (!areAnimationsEnabled(view.getContext())) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(200)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
}