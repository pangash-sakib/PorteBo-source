package com.sakib.portebo.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Base64;
import android.widget.Toast;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Local, offline guardian gate for app-setting edits. */
public final class ParentControlManager {
    private static final String PREFS = "guardian_control";
    private static final String PIN_SALT = "pin_salt";
    private static final String PIN_HASH = "pin_hash";
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int HASH_BITS = 256;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 30_000L;
    private static final long SESSION_MS = 15 * 60_000L;
    private static volatile long unlockedUntilElapsed;

    private ParentControlManager() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hasPin(Context context) {
        return context != null && !prefs(context).getString(PIN_HASH, "").isEmpty();
    }

    public static boolean isEnabled(Context context) {
        return context != null && hasPin(context) && prefs(context).getBoolean("enabled", false);
    }

    public static boolean setEnabled(Context context, boolean enabled) {
        if (context == null || !hasPin(context) || !isGuardianSessionUnlocked()) return false;
        prefs(context).edit().putBoolean("enabled", enabled).apply();
        if (!enabled) lock();
        return true;
    }

    public static synchronized boolean setPin(Context context, String pin) {
        if (context == null || pin == null || !pin.matches("[0-9]{6,12}")) return false;
        if (hasPin(context) && !isGuardianSessionUnlocked()) return false;
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        try {
            byte[] hash = derive(pin.toCharArray(), salt);
            prefs(context).edit()
                    .putString(PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                    .putInt("failed_attempts", 0)
                    .remove("lockout_until")
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized boolean verifyPin(Context context, String pin) {
        if (context == null || pin == null || lockoutRemainingMs(context) > 0 || !hasPin(context)) return false;
        SharedPreferences p = prefs(context);
        try {
            byte[] salt = Base64.decode(p.getString(PIN_SALT, ""), Base64.NO_WRAP);
            byte[] expected = Base64.decode(p.getString(PIN_HASH, ""), Base64.NO_WRAP);
            byte[] actual = derive(pin.toCharArray(), salt);
            if (MessageDigest.isEqual(expected, actual)) {
                p.edit().putInt("failed_attempts", 0).remove("lockout_until").apply();
                return true;
            }
        } catch (Exception ignored) { }

        int failures = p.getInt("failed_attempts", 0) + 1;
        SharedPreferences.Editor edit = p.edit();
        if (failures >= MAX_FAILED_ATTEMPTS) {
            edit.putInt("failed_attempts", 0)
                    .putLong("lockout_until", System.currentTimeMillis() + LOCKOUT_MS);
        } else {
            edit.putInt("failed_attempts", failures);
        }
        edit.apply();
        return false;
    }

    private static byte[] derive(char[] pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(pin, '\0');
        }
    }

    public static long lockoutRemainingMs(Context context) {
        if (context == null) return 0;
        long until = prefs(context).getLong("lockout_until", 0L);
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0 && until != 0L) prefs(context).edit().remove("lockout_until").apply();
        return Math.max(0L, remaining);
    }

    public static void unlock() {
        unlockedUntilElapsed = SystemClock.elapsedRealtime() + SESSION_MS;
    }

    public static void lock() {
        unlockedUntilElapsed = 0L;
    }

    public static long sessionRemainingMs() {
        return Math.max(0L, unlockedUntilElapsed - SystemClock.elapsedRealtime());
    }

    public static boolean isGuardianSessionUnlocked() {
        return sessionRemainingMs() > 0L;
    }

    public static boolean canEdit(Context context) {
        return context != null && (!isEnabled(context) || sessionRemainingMs() > 0L);
    }

    public static boolean requireEditAccess(Context context) {
        if (canEdit(context)) return true;
        if (context != null) {
            Toast.makeText(context.getApplicationContext(),
                    "শুধু অভিভাবক অ্যাপের নিয়ম বদলাতে পারবেন।", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
