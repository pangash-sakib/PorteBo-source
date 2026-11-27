package com.sakib.portebo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import com.sakib.portebo.R;

public class FakeBatteryOverlayActivity extends PaletteActivity {
    @Override
    protected int getPaletteThemeStyle() {
        return ThemePaletteManager.getSelectedFullscreenThemeStyle(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        setContentView(R.layout.overlay_fake_battery);

        findViewById(R.id.btnCloseBlockedApp).setOnClickListener(v -> closeToHome());
    }

    private void closeToHome() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } catch (Exception ignored) { }
        finish();
    }
}