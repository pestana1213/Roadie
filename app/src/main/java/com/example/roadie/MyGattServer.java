package com.example.roadie;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.UUID;

public class MyGattServer {

    private static final String TAG = "MyGattServer";

    private BluetoothGattServer gattServer;
    private final Context context;

    // Custom UUIDs (replace with your own if needed)
    public static final UUID SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("00000016-0000-3512-2118-0009af100700");

    public MyGattServer(Context context) {
        this.context = context;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void startServer() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        gattServer = bluetoothManager.openGattServer(context, new BluetoothGattServerCallback() {

            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                Log.i(TAG, "Connection state changed: " + device.getAddress() + " → " +
                        (newState == BluetoothProfile.STATE_CONNECTED ? "Connected" : "Disconnected"));
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                    int offset, BluetoothGattCharacteristic characteristic) {
                Log.i(TAG, "Read request received");
                if (CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                    byte[] response = "Hello Zepp!".getBytes();
                    gattServer.sendResponse(device, requestId, 1, offset, response);
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                     BluetoothGattCharacteristic characteristic,
                                                     boolean preparedWrite, boolean responseNeeded,
                                                     int offset, byte[] value) {
                Log.i(TAG, "Write request: " + new String(value));
                if (CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                    // Optionally store or process the value here
                    if (responseNeeded) {
                        gattServer.sendResponse(device, requestId, 1, offset, null);
                    }
                }
            }

        });

        // Create the service
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Create the characteristic
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        // 0x2901 - User Description
        BluetoothGattDescriptor descriptorUserDescription = new BluetoothGattDescriptor(
                UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_READ
        );
        descriptorUserDescription.setValue("TX Data".getBytes()); // Example description
        characteristic.addDescriptor(descriptorUserDescription);

// 0x2902 - Client Characteristic Configuration (for notifications)
        BluetoothGattDescriptor descriptorNotification = new BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
        );
        descriptorNotification.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        characteristic.addDescriptor(descriptorNotification);

// 0x2904 - Characteristic Presentation Format (optional, example only)
        BluetoothGattDescriptor descriptorPresentation = new BluetoothGattDescriptor(
                UUID.fromString("00002904-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_READ
        );
        descriptorPresentation.setValue(new byte[] {
                0x04,       // format (unsigned 8-bit integer)
                0x00, 0x00, // exponent (0)
                0x27, // unit (e.g., bpm = org.bluetooth.unit.heart_rate)
                0x01,       // namespace
                0x00        // description
        });
        service.addCharacteristic(characteristic);
        gattServer.addService(service);

        Log.i(TAG, "GATT Server started with custom service and characteristic.");
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void stopServer() {
        if (gattServer != null) {
            gattServer.close();
            Log.i(TAG, "GATT Server stopped.");
        }
    }
}
