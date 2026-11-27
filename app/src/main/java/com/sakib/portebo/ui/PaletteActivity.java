package com.sakib.portebo.ui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.sakib.portebo.data.PrefsManager;

/** Applies the saved accent palette consistently to regular app activities. */
public abstract class PaletteActivity extends AppCompatActivity {
    protected int getPaletteThemeStyle() {
        return ThemePaletteManager.getSelectedThemeStyle(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle state) {
        setTheme(getPaletteThemeStyle());
        super.onCreate(state);
        PrefsManager.init(this);
    }
}
