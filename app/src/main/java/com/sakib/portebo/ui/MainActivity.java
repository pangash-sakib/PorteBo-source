package com.sakib.portebo.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.sakib.portebo.R;
import com.sakib.portebo.data.PrefsManager;
import com.sakib.portebo.message.MessageManager;
import com.sakib.portebo.security.ParentControlManager;
import com.sakib.portebo.util.AnimUtils;

public class MainActivity extends PaletteActivity {
    private View indicator, devInbox;
    private TextView[] navLabels = new TextView[4];
    private FrameLayout bell;
    private TextView badge;
    private ImageView bellIcon;
    private AnimatedThemeToggleView themeButton;
    private GuardianAccessController guardianAccess;
    private int currentTab = 0;
    private boolean switching = false;
    private final Handler badgeHandler = new Handler(Looper.getMainLooper());
    private final Handler guardianHandler = new Handler(Looper.getMainLooper());
    private final Runnable badgeRefresh = new Runnable() {
        @Override public void run() { updateBell(); badgeHandler.postDelayed(this, 5000); }
    };
    private final Runnable guardianExpiry = this::refreshGuardianGates;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        PrefsManager.init(this);
        AppCompatDelegate.setDefaultNightMode(PrefsManager.isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);
        bind();
        guardianAccess = new GuardianAccessController(this, this::refreshGuardianGates);
        View logo = findViewById(R.id.ivAppLogo);
        logo.setOnLongClickListener(v -> { guardianAccess.openHiddenEntry(); return true; });
        if (state != null) currentTab = state.getInt("current_tab", 0);
        setupTabs();
        ensureFragments();
        showTab(currentTab, false);
        updateThemeIcon();
        MessageManager.refreshReleases(this);
        updateBell();
        badgeHandler.postDelayed(badgeRefresh, 5000);
        MessageManager.pollLikeResponses(this);
    }

    private void bind() {
        indicator = findViewById(R.id.navPillIndicator);
        navLabels[0] = findViewById(R.id.tvNavHome);
        navLabels[1] = findViewById(R.id.tvNavTiers);
        navLabels[2] = findViewById(R.id.tvNavRoutine);
        navLabels[3] = findViewById(R.id.tvNavAbout);
        bell = findViewById(R.id.btnNotificationBell);
        devInbox = findViewById(R.id.btnDeveloperInbox);
        badge = findViewById(R.id.tvBellBadge);
        bellIcon = findViewById(R.id.ivBellIcon);
        themeButton = findViewById(R.id.btnThemeToggle);
    }

    private void setupTabs() {
        int[] ids = {R.id.navTabHome, R.id.navTabTiers, R.id.navTabRoutine, R.id.navTabAbout};
        for (int i = 0; i < ids.length; i++) {
            final int index = i;
            findViewById(ids[i]).setOnClickListener(v ->
                    AnimUtils.pressFeedback(v, () -> switchFragment(index)));
        }
        themeButton.setOnClickListener(v -> {
            if (!ParentControlManager.requireEditAccess(this)) return;
            boolean dark = !PrefsManager.isDarkMode();
            Runnable apply = () -> {
                PrefsManager.setDarkMode(dark);
                AppCompatDelegate.setDefaultNightMode(dark
                        ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            };
            if (AnimUtils.areAnimationsEnabled(this)) themeButton.animateToDarkMode(dark, apply);
            else { themeButton.setDarkMode(dark); apply.run(); }
        });
        bell.setOnClickListener(v -> AnimUtils.pressFeedback(v,
                () -> startActivity(new android.content.Intent(this, NotificationsActivity.class))));
        devInbox.setOnClickListener(v -> AnimUtils.pressFeedback(v,
                () -> startActivity(new android.content.Intent(this, DevInboxActivity.class))));
    }

    private Fragment create(int index) {
        switch (index) {
            case 0: return new HomeFragment();
            case 1: return new AppTiersFragment();
            case 2: return new RoutineFragment();
            default: return new AboutFragment();
        }
    }

    private String tag(int index) { return "TAB_" + index; }

    private void ensureFragments() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction().setReorderingAllowed(true);
        for (int i = 0; i < 4; i++) {
            Fragment fragment = manager.findFragmentByTag(tag(i));
            if (fragment == null) {
                fragment = create(i);
                transaction.add(R.id.fragmentContainer, fragment, tag(i));
            }
            transaction.hide(fragment);
        }
        transaction.commitNow();
    }

    public void switchFragment(int index) {
        if (index < 0 || index > 3 || switching || index == currentTab) return;
        showTab(index, true);
    }

    private void showTab(int index, boolean animate) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction().setReorderingAllowed(true);
        if (animate && AnimUtils.areAnimationsEnabled(this)) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        for (int i = 0; i < 4; i++) {
            Fragment fragment = manager.findFragmentByTag(tag(i));
            if (fragment != null) {
                if (i == index) transaction.show(fragment); else transaction.hide(fragment);
            }
        }
        switching = true;
        transaction.runOnCommit(() -> switching = false);
        transaction.commit();
        currentTab = index;
        updateNav(index);
    }

    private void updateNav(int index) {
        int accentColor = ThemePaletteManager.resolvePrimaryColor(this);
        for (int i = 0; i < 4; i++) {
            navLabels[i].setTextColor(i == index ? accentColor : getColorCompat(R.color.text_secondary));
        }
        indicator.post(() -> {
            View items = findViewById(R.id.layoutNavItems);
            if (items == null || items.getWidth() <= 0) return;
            int width = items.getWidth() / 4;
            float x = width * index + width / 2f - indicator.getWidth() / 2f;
            if (AnimUtils.areAnimationsEnabled(this)) {
                indicator.animate().translationX(x).setDuration(240)
                        .setInterpolator(new android.view.animation.PathInterpolator(.25f, .1f, .25f, 1f)).start();
            } else indicator.setTranslationX(x);
        });
    }

    private int getColorCompat(int id) { return androidx.core.content.ContextCompat.getColor(this, id); }
    private void updateThemeIcon() { themeButton.setDarkMode(PrefsManager.isDarkMode()); }

    private void refreshGuardianGates() {
        if (isFinishing()) return;
        guardianHandler.removeCallbacks(guardianExpiry);
        FragmentManager manager = getSupportFragmentManager();
        Fragment tiers = manager.findFragmentByTag(tag(1));
        Fragment routine = manager.findFragmentByTag(tag(2));
        Fragment home = manager.findFragmentByTag(tag(0));
        if (tiers instanceof AppTiersFragment) ((AppTiersFragment) tiers).refreshAccess();
        if (routine instanceof RoutineFragment) ((RoutineFragment) routine).refreshAccess();
        if (home instanceof HomeFragment) ((HomeFragment) home).refreshAccess();
        if (ParentControlManager.isEnabled(this)) {
            long remaining = ParentControlManager.sessionRemainingMs();
            if (remaining > 0L) guardianHandler.postDelayed(guardianExpiry, remaining + 100L);
        }
    }

    private void updateBell() {
        if (badge == null) return;
        MessageManager.pollLikeResponses(this);
        int count = MessageManager.getUnreadCount(this);
        badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) {
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
            AnimUtils.shake(bellIcon);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (badge != null) updateBell();
        if (devInbox != null) devInbox.setVisibility(PrefsManager.isDevModeEnabled() ? View.VISIBLE : View.GONE);
        refreshGuardianGates();
    }

    @Override protected void onStop() {
        super.onStop();
        if (ParentControlManager.isEnabled(this)) {
            ParentControlManager.lock();
            guardianHandler.removeCallbacks(guardianExpiry);
        }
    }

    @Override protected void onDestroy() {
        badgeHandler.removeCallbacks(badgeRefresh);
        guardianHandler.removeCallbacks(guardianExpiry);
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("current_tab", currentTab);
        super.onSaveInstanceState(state);
    }
}
