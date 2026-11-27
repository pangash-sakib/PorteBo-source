package com.sakib.portebo.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sakib.portebo.R;
import com.sakib.portebo.security.ParentControlManager;

/** Dialog flow opened only by the hidden long-press gesture on the app logo. */
public final class GuardianAccessController {
    private static final String SETUP_MESSAGE = "শিক্ষার্থীকে ফোন দেওয়ার আগে নিজে একটা PIN সেট করে নাও। PIN ভুলে গেলে অ্যাপের ডেটা ক্লিয়ার করলে সব সেটিংস মুছে যাবে।";

    private final Activity activity;
    private final Runnable onAccessChanged;

    public GuardianAccessController(Activity activity, Runnable onAccessChanged) {
        this.activity = activity;
        this.onAccessChanged = onAccessChanged;
    }

    public void openHiddenEntry() {
        if (activity.isFinishing()) return;
        if (!ParentControlManager.hasPin(activity)) showPinSetup();
        else showPinEntry();
    }

    private void showPinSetup() {
        PinDialog views = pinDialog(SETUP_MESSAGE, "৬–১২ ডিজিটের PIN", "PIN আবার লিখো", true);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Guardian PIN সেট করো")
                .setView(views.root, 0, 0, 0, 0)
                .setNegativeButton("বাতিল", null)
                .setPositiveButton("সেট করো", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String first = text(views.pin);
                    if (!first.matches("[0-9]{6,12}")) {
                        views.pinLayout.setError("৬–১২ ডিজিটের PIN দাও");
                        views.pin.requestFocus();
                        return;
                    }
                    if (!first.equals(text(views.confirm))) {
                        views.confirmLayout.setError("দুটো PIN মেলেনি");
                        views.confirm.requestFocus();
                        return;
                    }
                    if (!ParentControlManager.setPin(activity, first)) {
                        views.pinLayout.setError("PIN সেট হয়নি। আবার চেষ্টা করো।");
                        return;
                    }
                    ParentControlManager.unlock();
                    if (!ParentControlManager.setEnabled(activity, true)) {
                        views.pinLayout.setError("সেশন শেষ। আবার চেষ্টা করো।");
                        return;
                    }
                    dialog.dismiss();
                    Toast.makeText(activity, "Guardian Control চালু হয়েছে।", Toast.LENGTH_LONG).show();
                    changed();
                    showGuardianMenu();
                }));
        showGuardianDialog(dialog);
    }

    private void showPinEntry() {
        PinDialog views = pinDialog(null, "Guardian PIN", null, false);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Guardian PIN দাও")
                .setView(views.root, 0, 0, 0, 0)
                .setNegativeButton("বাতিল", null)
                .setPositiveButton("খুলো", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    long wait = ParentControlManager.lockoutRemainingMs(activity);
                    if (wait > 0L) {
                        views.pinLayout.setError("আর " + ((wait + 999L) / 1000L) + " সেকেন্ড অপেক্ষা করো।");
                        return;
                    }
                    if (!ParentControlManager.verifyPin(activity, text(views.pin))) {
                        wait = ParentControlManager.lockoutRemainingMs(activity);
                        views.pinLayout.setError(wait > 0L
                                ? "৫ বার ভুল হয়েছে। ৩০ সেকেন্ড পরে আবার চেষ্টা করো।"
                                : "PIN মেলেনি।");
                        views.pin.selectAll();
                        return;
                    }
                    ParentControlManager.unlock();
                    dialog.dismiss();
                    Toast.makeText(activity, "Guardian session ১৫ মিনিটের জন্য খোলা হয়েছে।", Toast.LENGTH_SHORT).show();
                    changed();
                    showGuardianMenu();
                }));
        showGuardianDialog(dialog);
    }

    private void showGuardianMenu() {
        boolean enabled = ParentControlManager.isEnabled(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_guardian_menu, null, false);
        TextView message = content.findViewById(R.id.tvGuardianMenuMessage);
        MaterialButton changePin = content.findViewById(R.id.btnGuardianChangePin);
        MaterialButton toggle = content.findViewById(R.id.btnGuardianToggle);
        MaterialButton lockNow = content.findViewById(R.id.btnGuardianLockNow);

        message.setText(enabled
                ? "এখন তুমি অ্যাপের নিয়ম ও রুটিন পরিবর্তন করতে পারবে।"
                : "Guardian Control বন্ধ আছে। চালু করলে শিক্ষার্থী নিয়ম বদলাতে পারবে না।");
        toggle.setText(enabled ? "Guardian Control বন্ধ করো" : "Guardian Control চালু করো");
        lockNow.setVisibility(enabled ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Guardian Control")
                .setView(content, 0, 0, 0, 0)
                .setNegativeButton("বন্ধ", null)
                .create();
        changePin.setOnClickListener(v -> {
            if (!requireSessionOrClose(dialog)) return;
            dialog.dismiss();
            showChangePin();
        });
        toggle.setOnClickListener(v -> {
            if (!requireSessionOrClose(dialog)) return;
            if (enabled) {
                dialog.dismiss();
                confirmDisable();
            } else {
                if (!ParentControlManager.setEnabled(activity, true)) {
                    requireSessionOrClose(dialog);
                    return;
                }
                ParentControlManager.unlock();
                changed();
                dialog.dismiss();
                Toast.makeText(activity, "Guardian Control চালু হয়েছে।", Toast.LENGTH_SHORT).show();
            }
        });
        lockNow.setOnClickListener(v -> {
            if (!requireSessionOrClose(dialog)) return;
            ParentControlManager.lock();
            changed();
            dialog.dismiss();
            Toast.makeText(activity, "Guardian session লক হয়েছে।", Toast.LENGTH_SHORT).show();
        });
        showGuardianDialog(dialog);
    }

    private void showGuardianDialog(AlertDialog dialog) {
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_guardian_dialog);
        }
    }

    private void confirmDisable() {
    AlertDialog guardianDialog = new MaterialAlertDialogBuilder(activity)
            .setTitle("Guardian Control বন্ধ করবে?")
            .setMessage("বন্ধ করলে PIN ছাড়াই সব নিয়ম ও রুটিন বদলানো যাবে।")
            .setNegativeButton("না", null)
            .setPositiveButton("বন্ধ করো", (dialog, which) -> {
                if (!requireSessionOrClose(dialog)) return;
                if (!ParentControlManager.setEnabled(activity, false)) {
                    requireSessionOrClose(dialog);
                    return;
                }
                changed();
                Toast.makeText(activity, "Guardian Control বন্ধ হয়েছে।", Toast.LENGTH_SHORT).show();
            })
            .create();

    showGuardianDialog(guardianDialog);
    }

    private void showChangePin() {
        PinDialog views = pinDialog("নতুন PIN দুবার লিখো।", "নতুন ৬–১২ ডিজিটের PIN", "PIN আবার লিখো", true);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("PIN বদলাও")
                .setView(views.root, 0, 0, 0, 0)
                .setNegativeButton("বাতিল", null)
                .setPositiveButton("সেভ", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String first = text(views.pin);
                    if (!first.matches("[0-9]{6,12}")) {
                        views.pinLayout.setError("৬–১২ ডিজিটের PIN দাও");
                        return;
                    }
                    if (!first.equals(text(views.confirm))) {
                        views.confirmLayout.setError("দুটো PIN মেলেনি");
                        return;
                    }
                    if (!ParentControlManager.isGuardianSessionUnlocked()) {
                        requireSessionOrClose(dialog);
                        return;
                    }
                    boolean wasEnabled = ParentControlManager.isEnabled(activity);
                    if (!ParentControlManager.setPin(activity, first)) {
                        views.pinLayout.setError("PIN সেভ হয়নি। আবার চেষ্টা করো।");
                        return;
                    }
                    ParentControlManager.setEnabled(activity, wasEnabled);
                    ParentControlManager.unlock();
                    dialog.dismiss();
                    changed();
                    Toast.makeText(activity, "Guardian PIN বদলানো হয়েছে।", Toast.LENGTH_SHORT).show();
                }));
        showGuardianDialog(dialog);
    }

    private PinDialog pinDialog(String message, String pinHint, String confirmHint, boolean showConfirm) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_guardian_pin, null, false);
        TextView messageView = root.findViewById(R.id.tvGuardianPinMessage);
        TextInputLayout pinLayout = root.findViewById(R.id.tilGuardianPin);
        TextInputLayout confirmLayout = root.findViewById(R.id.tilGuardianPinConfirm);
        TextInputEditText pin = root.findViewById(R.id.etGuardianPin);
        TextInputEditText confirm = root.findViewById(R.id.etGuardianPinConfirm);

        if (message == null || message.isEmpty()) messageView.setVisibility(View.GONE);
        else messageView.setText(message);
        pinLayout.setHint(pinHint);
        if (showConfirm) {
            confirmLayout.setHint(confirmHint);
            confirmLayout.setVisibility(View.VISIBLE);
        }
        watchForErrorClear(pin, pinLayout);
        if (showConfirm) watchForErrorClear(confirm, confirmLayout);
        return new PinDialog(root, pinLayout, pin, confirmLayout, confirm);
    }

    private void watchForErrorClear(TextInputEditText input, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private boolean requireSessionOrClose(DialogInterface dialog) {
        if (ParentControlManager.isGuardianSessionUnlocked()) return true;
        Toast.makeText(activity, "সেশন শেষ। আবার PIN দাও।", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        return false;
    }

    private void changed() {
        if (onAccessChanged != null) onAccessChanged.run();
    }

    private static final class PinDialog {
        final View root;
        final TextInputLayout pinLayout;
        final TextInputEditText pin;
        final TextInputLayout confirmLayout;
        final TextInputEditText confirm;

        PinDialog(View root, TextInputLayout pinLayout, TextInputEditText pin,
                  TextInputLayout confirmLayout, TextInputEditText confirm) {
            this.root = root;
            this.pinLayout = pinLayout;
            this.pin = pin;
            this.confirmLayout = confirmLayout;
            this.confirm = confirm;
        }
    }
}
