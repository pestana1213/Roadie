package com.example.roadie;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothHelper {

    private static final String TAG = "BluetoothHelper";
    private static BluetoothSocket bluetoothSocket;
    private static OutputStream outputStream;

    public static boolean connectToBluetooth(BluetoothDevice device, Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return false;
        }

        try {
            UUID uuid = device.getUuids()[0].getUuid();
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Log.i(TAG, "Connected to Bluetooth device: " + device.getName());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to connect to Bluetooth device", e);
            return false;
        }
    }

    public static void sendData(String data) {
        if (bluetoothSocket != null && outputStream != null) {
            try {
                outputStream.write((data + "\n").getBytes());
                outputStream.flush();
                Log.d(TAG, "Sent data: " + data);
            } catch (IOException e) {
                Log.e(TAG, "Error sending data", e);
            }
        } else {
            Log.w(TAG, "Bluetooth not connected, data not sent");
        }
    }

    public static void disconnect() {
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
            Log.i(TAG, "Bluetooth connection closed");
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth connection", e);
        }
    }
}
