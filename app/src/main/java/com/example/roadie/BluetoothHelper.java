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
    private static BluetoothSocket bluetoothSocket;
    private static OutputStream outputStream;
    static BluetoothGatt bluetoothGatt;

    public static BluetoothGattCharacteristic writeCharacteristic;

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

        sendOverBle(data);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private static void sendOverBle(String message) {
        UUID UART_SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
        UUID TX_CHARACTERISTIC_UUID = UUID.fromString("00000016-0000-3512-2118-0009af100700");
        if (bluetoothGatt == null) {
            Log.w(TAG, "BLE not connected");
            return;
        }

        // Get the UART service and characteristic
        BluetoothGattService uartService = bluetoothGatt.getService(UART_SERVICE_UUID);
        if (uartService == null) {
            Log.e(TAG, "UART service not found");
            return;
        }

        BluetoothGattCharacteristic txCharacteristic = uartService.getCharacteristic(TX_CHARACTERISTIC_UUID);
        if (txCharacteristic == null) {
            Log.e(TAG, "TX characteristic not found");
            return;
        }

        // Split into 20-byte chunks (BLE requirement)
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        int chunkSize = 20;

        for (int i = 0; i < bytes.length; i += chunkSize) {
            int end = Math.min(bytes.length, i + chunkSize);
            byte[] chunk = Arrays.copyOfRange(bytes, i, end);

            txCharacteristic.setValue(chunk);
            if (!bluetoothGatt.writeCharacteristic(txCharacteristic)) {
                Log.e(TAG, "Failed to write chunk " + (i/chunkSize + 1));
            }
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
