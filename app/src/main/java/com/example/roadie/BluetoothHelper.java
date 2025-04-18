package com.example.roadie;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothHelper {

    private static final String TAG = "BluetoothHelper";
    private static BluetoothSocket bluetoothSocket;
    private static OutputStream outputStream;
    static BluetoothGatt bluetoothGatt;

    public static BluetoothGattCharacteristic writeCharacteristic;

    public static boolean connectToBluetooth(BluetoothDevice device, Activity activity, BluetoothGattCallback gattCallback) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return false;
        }

        // Try Classic Bluetooth first
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

        // Fall back to BLE
        try {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                bluetoothGatt = device.connectGatt(activity, false, gattCallback);
                if (bluetoothGatt != null) {
                    Log.i(TAG, "Attempting BLE connection to: " + device.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "BLE connection failed: ", e);
        }

        Log.e(TAG, "Failed to connect to Bluetooth device: " + device.getName());
        return false;
    }

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

        if (bluetoothGatt != null && writeCharacteristic != null) {
            writeCharacteristic.setValue(data.getBytes(StandardCharsets.UTF_8));
            boolean success = bluetoothGatt.writeCharacteristic(writeCharacteristic);
            Log.d(TAG, "Sent over BLE: " + success + " | Data: " + data);
        } else {
            Log.w(TAG, "BLE not connected or writeCharacteristic missing");
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
