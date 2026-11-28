package com.sakib.portebo.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.sakib.portebo.data.PrefsManager;

/**
 * Applies the saved accent palette and selected language
 * to all regular app activities.
 */
public abstract class PaletteActivity extends AppCompatActivity {

    protected int getPaletteThemeStyle() {
        return ThemePaletteManager.getSelectedThemeStyle(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle state) {
        setTheme(getPaletteThemeStyle());
        super.onCreate(state);
        PrefsManager.init(this);

        /*
         * Apply runtime translations after every layout pass. This catches
         * RecyclerView items, fragments and dynamically created controls.
         */
        getWindow().getDecorView()
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(() -> {
                    View content = findViewById(android.R.id.content);
                    LanguageManager.apply(content);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LanguageManager.apply(findViewById(android.R.id.content));
    }
}
