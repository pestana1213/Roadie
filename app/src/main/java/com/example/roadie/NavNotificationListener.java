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

            if (smallIcon != null) {
                byte[] iconBytes = iconToByteArray(smallIcon);
                String iconBase64 = Base64.encodeToString(iconBytes, Base64.NO_WRAP);

                String enhancedData = "TEXT:" + text + "|ICON:" + iconBase64;
                BluetoothHelper.sendData(enhancedData);
                // NotificationUtils.showNavigationStep(this, text, iconBase64);
            } else {
                BluetoothHelper.sendData("TEXT:" + text + "|");
                // NotificationUtils.showNavigationStep(this, text);
            }

        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("com.google.android.apps.maps")) {
            NotificationUtils.cancelNavigationNotification(this);
        }
    }

    private byte[] iconToByteArray(Icon icon) {
        try {
            Drawable drawable = icon.loadDrawable(this);
            if (drawable == null) return new byte[0];

            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                int size = 48;
                bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, size, size);
                drawable.draw(canvas);
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
            Bitmap rgb565Bitmap = resizedBitmap.copy(Bitmap.Config.RGB_565, false);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            rgb565Bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            Log.e("NavNotificationListener", "Icon conversion error", e);
            return new byte[0];
        }
    }
}
