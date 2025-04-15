package com.example.roadie;

import android.app.Notification;
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
            String text = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString();
            Log.d("NavNotificationListener", "Received notification: " + text);
            BluetoothHelper.sendData(text);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle the removal of a notification if necessary
    }
}
