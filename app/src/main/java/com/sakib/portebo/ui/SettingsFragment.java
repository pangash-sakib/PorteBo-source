package com.sakib.portebo.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.sakib.portebo.R;
import com.sakib.portebo.data.PrefsManager;
import com.sakib.portebo.security.ParentControlManager;

public class SettingsFragment extends Fragment {

    private AnimatedThemeToggleView themeToggle;
    private MaterialButton englishButton;
    private MaterialButton banglaButton;

    private boolean languageSwitching = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_settings,
                container,
                false
        );

        themeToggle = view.findViewById(R.id.btnSettingsThemeToggle);
        englishButton = view.findViewById(R.id.btnLanguageEnglish);
        banglaButton = view.findViewById(R.id.btnLanguageBangla);

        setupThemeToggle();
        setupLanguageButtons();
        updateLanguageButtons();

        LanguageManager.apply(view);
        return view;
    }

    private void setupThemeToggle() {
        themeToggle.setDarkMode(PrefsManager.isDarkMode());

        themeToggle.setOnClickListener(view -> {
            if (!ParentControlManager.requireEditAccess(requireContext())) {
                return;
            }

            boolean darkMode = !PrefsManager.isDarkMode();

            Runnable applyTheme = () -> {
                PrefsManager.setDarkMode(darkMode);

                AppCompatDelegate.setDefaultNightMode(
                        darkMode
                                ? AppCompatDelegate.MODE_NIGHT_YES
                                : AppCompatDelegate.MODE_NIGHT_NO
                );
            };

            if (AnimUtils.areAnimationsEnabled(requireContext())) {
                themeToggle.animateToDarkMode(darkMode, applyTheme);
            } else {
                themeToggle.setDarkMode(darkMode);
                applyTheme.run();
            }
        });
    }

    private void setupLanguageButtons() {
        englishButton.setOnClickListener(view ->
                changeLanguage(LanguageManager.ENGLISH, englishButton)
        );

        banglaButton.setOnClickListener(view ->
                changeLanguage(LanguageManager.BANGLA, banglaButton)
        );
    }

    private void changeLanguage(String language, MaterialButton selectedButton) {
        if (languageSwitching || language.equals(PrefsManager.getLanguage())) {
            return;
        }

        languageSwitching = true;
        PrefsManager.setLanguage(language);
        updateLanguageButtons();

        selectedButton.animate()
                .scaleX(0.90f)
                .scaleY(0.90f)
                .alpha(0.75f)
                .setDuration(130)
                .withEndAction(() -> selectedButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(170)
                        .withEndAction(() -> {
                            if (isAdded()) {
                                requireActivity().recreate();
                            }
                        })
                        .start())
                .start();
    }

    private void updateLanguageButtons() {
        boolean englishSelected = LanguageManager.ENGLISH.equals(
                PrefsManager.getLanguage()
        );

        updateLanguageButton(englishButton, englishSelected);
        updateLanguageButton(banglaButton, !englishSelected);
    }

    private void updateLanguageButton(
            MaterialButton button,
            boolean selected
    ) {
        int accentColor = ThemePaletteManager.resolvePrimaryColor(requireContext());
        int normalBackground = ContextCompat.getColor(
                requireContext(),
                R.color.bg_button_secondary
        );
        int normalText = ContextCompat.getColor(
                requireContext(),
                R.color.text_secondary
        );

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        selected ? accentColor : normalBackground
                )
        );

        button.setTextColor(
                ColorStateList.valueOf(
                        selected ? 0xFFFFFFFF : normalText
                )
        );

        button.setStrokeWidth(selected ? 2 : 0);

        button.setStrokeColor(
                ColorStateList.valueOf(
                        selected ? accentColor : normalBackground
                )
        );

        button.setAlpha(selected ? 1f : 0.82f);
    }
}
