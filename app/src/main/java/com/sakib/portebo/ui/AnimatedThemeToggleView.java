package com.sakib.portebo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import com.sakib.portebo.R;
import java.util.ArrayList;
import java.util.List;

/** Spring-animated sun/moon toggle adapted from the linked React-Spring SVG concept. */
public class AnimatedThemeToggleView extends AppCompatImageButton {
    private float radius = 9f;
    private float maskX = 12f;
    private float maskY = 4f;
    private float rayAlpha = 0f;
    private float iconRotation = 40f;
    private boolean darkMode;
    private int animationsRemaining;
    private boolean transitionFinished = true;
    private Runnable animationEnd;
    private Runnable timeoutRunnable;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<SpringAnimation> runningAnimations = new ArrayList<>();

    // Faster and more tightly damped than the web demo so Android theme changes stay responsive.
    private static final float SPRING_STIFFNESS = 900f;
    private static final float SPRING_DAMPING_RATIO = 0.78f;
    private static final long MAX_THEME_APPLY_DELAY_MS = 400L;

    private static final FloatPropertyCompat<AnimatedThemeToggleView> RADIUS =
            new FloatPropertyCompat<AnimatedThemeToggleView>("radius") {
                @Override public float getValue(AnimatedThemeToggleView view) { return view.radius; }
                @Override public void setValue(AnimatedThemeToggleView view, float value) { view.radius = value; view.invalidate(); }
            };
    private static final FloatPropertyCompat<AnimatedThemeToggleView> MASK_X =
            new FloatPropertyCompat<AnimatedThemeToggleView>("maskX") {
                @Override public float getValue(AnimatedThemeToggleView view) { return view.maskX; }
                @Override public void setValue(AnimatedThemeToggleView view, float value) { view.maskX = value; view.invalidate(); }
            };
    private static final FloatPropertyCompat<AnimatedThemeToggleView> MASK_Y =
            new FloatPropertyCompat<AnimatedThemeToggleView>("maskY") {
                @Override public float getValue(AnimatedThemeToggleView view) { return view.maskY; }
                @Override public void setValue(AnimatedThemeToggleView view, float value) { view.maskY = value; view.invalidate(); }
            };
    private static final FloatPropertyCompat<AnimatedThemeToggleView> RAY_ALPHA =
            new FloatPropertyCompat<AnimatedThemeToggleView>("rayAlpha") {
                @Override public float getValue(AnimatedThemeToggleView view) { return view.rayAlpha; }
                @Override public void setValue(AnimatedThemeToggleView view, float value) { view.rayAlpha = clamp(value, 0f, 1f); view.invalidate(); }
            };
    private static final FloatPropertyCompat<AnimatedThemeToggleView> ICON_ROTATION =
            new FloatPropertyCompat<AnimatedThemeToggleView>("iconRotation") {
                @Override public float getValue(AnimatedThemeToggleView view) { return view.iconRotation; }
                @Override public void setValue(AnimatedThemeToggleView view, float value) { view.iconRotation = value; view.invalidate(); }
            };

    public AnimatedThemeToggleView(Context context) { super(context); init(); }
    public AnimatedThemeToggleView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public AnimatedThemeToggleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setScaleType(ScaleType.CENTER);
        setContentDescription("Switch to dark theme");
    }

    /** Sets the icon to the action offered for the currently active theme. */
    public void setDarkMode(boolean dark) {
        darkMode = dark;
        radius = dark ? 5f : 9f;
        maskX = dark ? 30f : 12f;
        maskY = dark ? 0f : 4f;
        rayAlpha = dark ? 1f : 0f;
        iconRotation = dark ? 90f : 40f;
        setContentDescription(dark ? "Switch to light theme" : "Switch to dark theme");
        invalidate();
    }

    /** Animates the icon, but never holds the actual theme change longer than 400 ms. */
    public void animateToDarkMode(boolean dark, @Nullable Runnable onFinished) {
        if (animationsRemaining > 0) return;
        darkMode = dark;
        animationEnd = onFinished;
        transitionFinished = false;
        animationsRemaining = 5;
        runningAnimations.clear();
        setClickable(false);
        setContentDescription(dark ? "Switch to light theme" : "Switch to dark theme");
        timeoutRunnable = this::finishTransition;
        mainHandler.postDelayed(timeoutRunnable, MAX_THEME_APPLY_DELAY_MS);

        animateProperty(RADIUS, dark ? 5f : 9f, 4f, 10f);
        animateProperty(MASK_X, dark ? 30f : 12f, 0f, 32f);
        animateProperty(MASK_Y, dark ? 0f : 4f, 0f, 24f);
        animateProperty(RAY_ALPHA, dark ? 1f : 0f, 0f, 1f);
        animateProperty(ICON_ROTATION, dark ? 90f : 40f, 0f, 180f);
    }

    private void animateProperty(FloatPropertyCompat<AnimatedThemeToggleView> property,
                                 float target, float min, float max) {
        SpringForce force = new SpringForce(target)
                .setStiffness(SPRING_STIFFNESS)
                .setDampingRatio(SPRING_DAMPING_RATIO);
        SpringAnimation animation = new SpringAnimation(this, property);
        animation.setSpring(force);
        animation.setMinValue(min);
        animation.setMaxValue(max);
        animation.addEndListener((a, canceled, value, velocity) -> {
            runningAnimations.remove(a);
            if (transitionFinished) return;
            if (--animationsRemaining <= 0) finishTransition();
        });
        runningAnimations.add(animation);
        animation.animateToFinalPosition(target);
    }

    private void finishTransition() {
        if (transitionFinished) return;
        transitionFinished = true;
        if (timeoutRunnable != null) mainHandler.removeCallbacks(timeoutRunnable);
        timeoutRunnable = null;
        for (SpringAnimation animation : new ArrayList<>(runningAnimations)) {
            if (animation.isRunning()) animation.cancel();
        }
        runningAnimations.clear();
        animationsRemaining = 0;
        radius = darkMode ? 5f : 9f;
        maskX = darkMode ? 30f : 12f;
        maskY = darkMode ? 0f : 4f;
        rayAlpha = darkMode ? 1f : 0f;
        iconRotation = darkMode ? 90f : 40f;
        setClickable(true);
        invalidate();
        Runnable callback = animationEnd;
        animationEnd = null;
        if (callback != null) callback.run();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float size = 24f * density;
        int outer = canvas.save();
        canvas.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        canvas.scale(density, density);
        canvas.rotate(iconRotation, 12f, 12f);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int iconColor = ContextCompat.getColor(getContext(), R.color.text_primary);
        paint.setColor(iconColor);
        paint.setStyle(Paint.Style.FILL);

        int iconLayer = canvas.saveLayer(0f, 0f, 24f, 24f, null);
        canvas.drawCircle(12f, 12f, radius, paint);
        if (!darkMode) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawCircle(maskX, maskY, 9f, paint);
            paint.setXfermode(null);
        }
        canvas.restoreToCount(iconLayer);

        if (rayAlpha > 0.001f) {
            paint.setXfermode(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setAlpha((int) (255f * rayAlpha));
            canvas.drawLine(12f, 1f, 12f, 3f, paint);
            canvas.drawLine(12f, 21f, 12f, 23f, paint);
            canvas.drawLine(4.22f, 4.22f, 5.64f, 5.64f, paint);
            canvas.drawLine(18.36f, 18.36f, 19.78f, 19.78f, paint);
            canvas.drawLine(1f, 12f, 3f, 12f, paint);
            canvas.drawLine(21f, 12f, 23f, 12f, paint);
            canvas.drawLine(4.22f, 19.78f, 5.64f, 18.36f, paint);
            canvas.drawLine(18.36f, 5.64f, 19.78f, 4.22f, paint);
        }
        canvas.restoreToCount(outer);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
