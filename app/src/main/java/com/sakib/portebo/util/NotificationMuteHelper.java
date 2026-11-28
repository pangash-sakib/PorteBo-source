package com.sakib.portebo.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

public final class NotificationMuteHelper {

    private NotificationMuteHelper() {
    }

    /**
     * Checks if the user has granted Do Not Disturb (Notification Policy) access.
     */
    public static boolean isDndAccessGranted(Context context) {
        if (context == null) return false;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return false;
        return nm.isNotificationPolicyAccessGranted();
    }

    /**
     * Opens the system settings screen where the user can grant DND access.
     */
    public static void openDndSettings(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "সেটিংস খোলা যায়নি।", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Applies the mute state to the specific package.
     * If permission is missing, it prompts the user automatically.
     */
    public static void setMuted(Context context, String packageName, boolean muted) {
        if (context == null || packageName == null) return;

        // 1. Check Permission
        if (!isDndAccessGranted(context)) {
            Toast.makeText(context, "নোটিফিকেশন মিউট করতে DND অ্যাক্সেস দিন।", Toast.LENGTH_LONG).show();
            openDndSettings(context);
            return;
        }

        // 2. Apply Mute (Requires API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.setNotificationsBanned(packageName, muted);
                }
            } catch (Exception e) {
                // Silently fail or log
            }
        } else {
            Toast.makeText(context, "এই অ্যান্ড্রয়েড ভার্সনে মিউট সাপোর্ট করে না।", Toast.LENGTH_SHORT).show();
        }
    }
}