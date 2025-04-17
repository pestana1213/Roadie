package com.example.roadie;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationUtils {

    private static final String CHANNEL_ID = "nav_channel";
    private static final int NOTIF_ID = 1001;

    public static void showNavigationStep(Context context, String directionText) {
        Log.d("Notification step", "posting not");

        // Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Handle the case where permission is not granted
                // You can either notify the user or skip showing the notification
                return;
            }
        }

        // Create notification channel if needed (only for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Navigation",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Navigation directions to be shown on watch");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Build and show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map) // You can replace with your own icon
                .setContentTitle("🧭 Navigation")
                .setContentText(directionText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(directionText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(NOTIF_ID, builder.build());
    }

    public static void showNavigationStep(Context context, String directionText, String base64Icon) {
        Log.d("Notification step", "posting not");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Navigation",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Navigation directions to be shown on watch");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("🧭 Navigation")
                .setContentText(directionText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(directionText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .extend(new NotificationCompat.WearableExtender());

        if (base64Icon != null && !base64Icon.isEmpty()) {
            try {
                byte[] iconBytes = Base64.decode(base64Icon, Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);

                if (bitmap != null) {
                    Bitmap watchBitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true);
                    builder.setLargeIcon(watchBitmap);
                }
            } catch (Exception e) {
                Log.e("NotificationUtils", "Icon decoding failed", e);
            }
        }

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(NOTIF_ID, builder.build());
    }

    public static void cancelNavigationNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID);
    }
}
