package com.sakib.portebo.update;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.sakib.portebo.R;
import com.sakib.portebo.ui.MainActivity;

public class UpdateNotificationManager {

    private static final String CHANNEL_ID = "update_channel_id";
    private static final int NOTIFICATION_ID = 777;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "App Updates", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows update download and installation progress");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // 1. Downloading / Verifying (Ambient Color + Progress + ETA)
    public static void showProgress(Context context, int progress, int max, String stage, String eta) {
        createChannel(context);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_porte)
                .setContentTitle("Porte Bo! Update")
                .setContentText(stage)
                .setProgress(max, progress, max == 0) // max==0 makes it indeterminate (spinning)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (eta != null) {
            builder.setSubText(eta);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, builder.build());
    }

    // 2. Error (Red Bar)
    public static void showError(Context context, String error) {
        createChannel(context);
        int redColor = ContextCompat.getColor(context, R.color.tier_blacklist); // Using your existing red

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_porte)
                .setContentTitle("Update Failed")
                .setContentText(error)
                .setColor(redColor)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, builder.build());
    }

    // 3. Installing (Green Bar)
    public static void showInstalling(Context context) {
        createChannel(context);
        int greenColor = ContextCompat.getColor(context, R.color.tier_whitelist); // Using your existing green/success color

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_porte)
                .setContentTitle("Installing Update")
                .setContentText("Please wait...")
                .setColor(greenColor)
                .setProgress(100, 100, false) // Full green bar
                .setOngoing(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, builder.build());
    }

    // 4. Hide (Task complete)
    public static void hide(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }
}