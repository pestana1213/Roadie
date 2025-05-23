package com.example.roadie;

import android.Manifest;
import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class NavNotificationListener extends NotificationListenerService {
    private static final String TAG = "NavNotification";
    private static final String MAPS_PACKAGE = "com.google.android.apps.maps";
    private static final int ICON_SIZE = 32; // Reduced size for ESP32 displays

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!MAPS_PACKAGE.equals(sbn.getPackageName())) {
            return;
        }

        try {
            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;

            // Extract title and text
            CharSequence titleSeq = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textSeq = extras.getCharSequence(Notification.EXTRA_TEXT);

            String notificationText = formatNotificationText(titleSeq, textSeq);

            // Initialize iconData
            String iconData = "";

            // Try largeIcon first if available
            Icon largeIcon = extras.getParcelable(Notification.EXTRA_LARGE_ICON);
            if (largeIcon != null) {
                // iconData = convertIconToBinaryString(largeIcon);
                byte[] iconBytes = convertIconToByteArray(largeIcon);
                iconData = Base64.encodeToString(iconBytes, Base64.NO_WRAP);
            }

            String payload = "TEXT:" + normalizeText(notificationText) + "|ICON:" + iconData;
            BluetoothHelper.sendData(payload);
            Log.d(TAG, "Payload sent (length): " + payload.length());

        } catch (Exception e) {
            Log.e(TAG, "Error processing notification", e);
        }
    }

    private String convertIconToBinaryString(Icon icon) {
        try {
            Bitmap bitmap = convertIconToBitmap(icon);
            if (bitmap == null) return "";

            // Resize and convert to 1-bit monochrome
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, ICON_SIZE, ICON_SIZE, true);
            StringBuilder binaryString = new StringBuilder();

            for (int y = 0; y < ICON_SIZE; y++) {
                for (int x = 0; x < ICON_SIZE; x++) {
                    int pixel = resized.getPixel(x, y);
                    // Simple brightness threshold to convert to 1-bit
                    binaryString.append((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel) > 384 ? "1" : "0"));
                }
            }

            return binaryString.toString();

        } catch (Exception e) {
            Log.e(TAG, "Icon conversion failed", e);
            return "";
        }
    }

    private Bitmap convertIconToBitmap(Icon icon) {
        Drawable drawable = icon.loadDrawable(this);
        if (drawable == null) return null;

        int size = ICON_SIZE;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Scale and center the drawable within the canvas
        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();

        if (dWidth > 0 && dHeight > 0) {
            float scale = Math.min((float) size / dWidth, (float) size / dHeight);
            int width = Math.round(dWidth * scale);
            int height = Math.round(dHeight * scale);
            int dx = (size - width) / 2;
            int dy = (size - height) / 2;

            drawable.setBounds(dx, dy, dx + width, dy + height);
        } else {
            // Fallback if drawable has no intrinsic size
            drawable.setBounds(0, 0, size, size);
        }

        drawable.draw(canvas);
        return bitmap;
    }

    private String formatNotificationText(CharSequence... textComponents) {
        StringBuilder builder = new StringBuilder();
        for (CharSequence component : textComponents) {
            if (component != null && component.length() > 0) {
                if (builder.length() > 0) builder.append(" ");
                builder.append(component);
            }
        }
        return builder.toString();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (MAPS_PACKAGE.equals(sbn.getPackageName())) {
            NotificationUtils.cancelNavigationNotification(this);
        }
    }

    public String normalizeText(String input) {
        // Normalize to decomposed form (NFD)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Remove diacritics (accents)
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }

    private byte[] convertIconToByteArray(Icon icon) {
        try {
            Bitmap bitmap = convertIconToBitmap(icon);
            if (bitmap == null) return new byte[0];

            Bitmap resized = Bitmap.createScaledBitmap(bitmap, ICON_SIZE, ICON_SIZE, true);
            byte[] byteArray = new byte[(ICON_SIZE * ICON_SIZE) / 8];
            int bitIndex = 0;

            for (int y = 0; y < ICON_SIZE; y++) {
                for (int x = 0; x < ICON_SIZE; x++) {
                    int pixel = resized.getPixel(x, y);
                    int brightness = (int)(0.3 * Color.red(pixel) + 0.59 * Color.green(pixel) + 0.11 * Color.blue(pixel));
                    boolean bit = brightness > 96; // Lower threshold to better show midtones

                    if (bit) {
                        byteArray[bitIndex / 8] |= (1 << (7 - (bitIndex % 8)));
                    }
                    bitIndex++;
                }
            }

            return byteArray;

        } catch (Exception e) {
            Log.e(TAG, "Icon conversion failed", e);
            return new byte[0];
        }
    }
}
