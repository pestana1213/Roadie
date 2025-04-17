package com.example.roadie;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NavNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if (packageName.equals("com.google.android.apps.maps")) {
            Bundle extras = sbn.getNotification().extras;

            CharSequence textCS = extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence titleCS = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence subTextCS = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            Icon smallIcon = sbn.getNotification().getSmallIcon();

            // Log small icon info for debugging
            if (smallIcon != null) {
                Log.d("NavNotificationListener", "Small Icon: " + smallIcon.toString());
            }

            StringBuilder textBuilder = new StringBuilder();

            if (titleCS != null) {
                textBuilder.append(titleCS).append(" ");
            }
            if (textCS != null) {
                textBuilder.append(textCS).append(" ");
            }
            if (subTextCS != null) {
                textBuilder.append(subTextCS);
            }

            String text = textBuilder.toString().trim();

            if (text.isEmpty()) {
                text = "[No text in notification]";
            }

            Log.d("NavNotificationListener", "Received notification: " + text);

            // Handle icon sending
            if (smallIcon != null) {
                // Convert Icon to byte array (for sending over Bluetooth)
                byte[] iconBytes = iconToByteArray(smallIcon);
                String iconBase64 = Base64.encodeToString(iconBytes, Base64.DEFAULT);

                // Send the icon and text over Bluetooth
                String enhancedData = "Text: " + text + "\nIcon: " + iconBase64;
                BluetoothHelper.sendData(enhancedData);
            } else {
                // Send only the text if there's no icon
                BluetoothHelper.sendData(text);
            }

            // Display the navigation step with the text
            NotificationUtils.showNavigationStep(this, text);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("com.google.android.apps.maps")) {
            NotificationUtils.cancelNavigationNotification(this);
        }
    }

    /**
     * Convert an Icon to a byte array (for sending over Bluetooth)
     */
    private byte[] iconToByteArray(Icon icon) {
        try {
            Drawable drawable = icon.loadDrawable(this);

            if (drawable == null) return new byte[0];

            Bitmap bitmap;

            // Convert Drawable to Bitmap
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                // Create a Bitmap with the intrinsic size of the Drawable
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            Log.e("NavNotificationListener", "Error converting icon to byte array", e);
            return new byte[0];
        }
    }
}
