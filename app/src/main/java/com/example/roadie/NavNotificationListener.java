package com.example.roadie;

import android.app.Notification;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

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
                Log.d("NavNotificationListener", "Tem smallIcon: " + smallIcon.toString());
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
                text = "[Sem texto na notificação]";
            }

            Log.d("NavNotificationListener", "Received notification: " + text);
            BluetoothHelper.sendData(text);

        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle the removal of a notification if necessary
    }
}
