package com.sakib.portebo.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import com.sakib.portebo.R;

/** Provides the app's supported accent palettes and selected Material theme overlay. */
public final class ThemePaletteManager {
    public static final String DEFAULT_PALETTE_ID = "red";

    public static final class Palette {
        public final String id;
        public final String label;
        public final int colorResource;

        private Palette(String id, String label, int colorResource) {
            this.id = id;
            this.label = label;
            this.colorResource = colorResource;
        }
    }

    private static final Palette[] PALETTES = {
            new Palette("red", "লাল", R.color.palette_red),
            new Palette("blue", "নীল", R.color.palette_blue),
            new Palette("purple", "বেগুনি", R.color.palette_purple),
            new Palette("green", "সবুজ", R.color.palette_green),
            new Palette("orange", "কমলা", R.color.palette_orange),
            new Palette("pink", "গোলাপি", R.color.palette_pink),
            new Palette("teal", "টিল", R.color.palette_teal),
            new Palette("slate", "স্লেট", R.color.palette_slate)
    };

    private ThemePaletteManager() { }

    public static Palette[] getPalettes() {
        return PALETTES.clone();
    }

    public static boolean isValidPaletteId(String id) {
        if (id == null) return false;
        for (Palette palette : PALETTES) {
            if (palette.id.equals(id)) return true;
        }
        return false;
    }

    /** Reads preferences directly so the theme can be selected before Activity inflation. */
    private static String getSelectedPaletteId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("portebo_prefs", Context.MODE_PRIVATE);
        String id = preferences.getString("theme_palette_id", DEFAULT_PALETTE_ID);
        return isValidPaletteId(id) ? id : DEFAULT_PALETTE_ID;
    }

    public static int getSelectedThemeStyle(Context context) {
        return getThemeStyle(getSelectedPaletteId(context), false);
    }

    public static int getSelectedFullscreenThemeStyle(Context context) {
        return getThemeStyle(getSelectedPaletteId(context), true);
    }

    private static int getThemeStyle(String id, boolean fullscreen) {
        if (fullscreen) {
            switch (id) {
                case "blue": return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Blue;
                case "purple": return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Purple;
                case "green": return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Green;
                case "orange": return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Orange;
                case "pink": return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Pink;
                case "teal": return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Teal;
                case "slate": return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Slate;
                case "red":
                default: return R.style.Theme_PorteBo_FullscreenAMOLED_Palette_Red;
            }
        }
        switch (id) {
            case "blue": return R.style.Theme_PorteBo_Palette_Blue;
            case "purple": return R.style.Theme_PorteBo_Palette_Purple;
            case "green": return R.style.Theme_PorteBo_Palette_Green;
            case "orange": return R.style.Theme_PorteBo_Palette_Orange;
            case "pink": return R.style.Theme_PorteBo_Palette_Pink;
            case "teal": return R.style.Theme_PorteBo_Palette_Teal;
            case "slate": return R.style.Theme_PorteBo_Palette_Slate;
            case "red":
            default: return R.style.Theme_PorteBo_Palette_Red;
        }
    }

    public static int resolvePrimaryColor(Context context) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, value, true)) {
            if (value.resourceId != 0) return ContextCompat.getColor(context, value.resourceId);
            return value.data;
        }
        return ContextCompat.getColor(context, R.color.primary_red);
    }
}
