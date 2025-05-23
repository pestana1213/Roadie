package com.example.roadie;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothHelper {
    private static final String TAG = "BluetoothHelper";
    public static BluetoothGattCharacteristic writeCharacteristic;
    public static BluetoothSocket bluetoothSocket;
    public static OutputStream outputStream;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static boolean connectToClassic(BluetoothDevice device) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Log.d("BluetoothHelper", "Connected via Classic Bluetooth");
            return true;
        } catch (IOException e) {
            Log.e("BluetoothHelper", "Classic Bluetooth connection failed", e);
            return false;
        }
    }
    public static boolean connectToBluetooth(BluetoothDevice device, Activity activity, BluetoothGattCallback gattCallback) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return false;
        }

        try {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC || device.getUuids() != null) {
                UUID uuid = device.getUuids()[0].getUuid();  // SPP UUID or other service
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                Log.i(TAG, "Connected via Classic Bluetooth to: " + device.getName());
                return true;
            }
        } catch (IOException | NullPointerException e) {
            Log.w(TAG, "Classic Bluetooth connection failed or not supported: " + e.getMessage());
        }
        Log.e(TAG, "Failed to connect to Bluetooth device: " + device.getName());
        return false;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static void sendData(String data) {
        if (bluetoothSocket != null && outputStream != null) {
            try {
                outputStream.write((data + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                Log.d(TAG, "Sent over classic BT: " + data);
            } catch (IOException e) {
                Log.e(TAG, "Classic BT send error", e);
            }
        } else {
            Log.w(TAG, "Classic Bluetooth not connected");
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
