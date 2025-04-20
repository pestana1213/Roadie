package com.example.roadie;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.UUID;

public class BLEAdvertiser {

    private static final String TAG = "BLEAdvertiser";
    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    public BLEAdvertiser(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothAdapter.isMultipleAdvertisementSupported()) {
            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }

        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "BLE advertising started.");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "BLE advertising failed with code " + errorCode);
            }
        };
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void startAdvertising() {
        if (advertiser == null) {
            Log.e(TAG, "BLE advertiser is not available.");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb")))
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void stopAdvertising() {
        if (advertiser != null) {
            advertiser.stopAdvertising(advertiseCallback);
            Log.i(TAG, "BLE advertising stopped.");
        }
    }
}
