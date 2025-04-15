package com.example.roadie;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NavNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if (packageName.equals("com.google.android.apps.maps")) {
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString("android.title");
            String text = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString();

        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle the removal of a notification if necessary
    }
}
