package com.sakib.portebo.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sakib.portebo.R;
import com.sakib.portebo.data.PrefsManager;
import com.sakib.portebo.message.MessageManager;

public class DevModeActivity extends PaletteActivity {
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private ProgressBar progress;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PrefsManager.init(this);
        setContentView(R.layout.activity_dev_mode);
        emailLayout = findViewById(R.id.tilDevEmail);
        passwordLayout = findViewById(R.id.tilDevPassword);
        emailInput = findViewById(R.id.etDevEmail);
        passwordInput = findViewById(R.id.etDevPassword);
        loginButton = findViewById(R.id.btnDevLogin);
        progress = findViewById(R.id.progressDevLogin);
        errorText = findViewById(R.id.tvDevLoginError);
        loginButton.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.btnDevCancel).setOnClickListener(v -> finish());
        emailInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { emailLayout.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        passwordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { passwordLayout.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
        emailLayout.setError(null);
        passwordLayout.setError(null);
        errorText.setVisibility(View.GONE);
        if (email.isEmpty()) {
            emailLayout.setError("ইমেইল লিখো।");
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError("পাসওয়ার্ড লিখো।");
            passwordInput.requestFocus();
            return;
        }
        loginButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        MessageManager.signInWithPassword(this, email, password, (ok, message) -> {
            if (isFinishing() || isDestroyed()) return;
            progress.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            if (ok) {
                getSharedPreferences("dev_auth", MODE_PRIVATE).edit().putString("id_token", message).apply();
                PrefsManager.setDevModeEnabled(true);
                setResult(RESULT_OK);
                finish();
            } else {
                errorText.setText(message);
                errorText.setVisibility(View.VISIBLE);
            }
        });
    }
}
