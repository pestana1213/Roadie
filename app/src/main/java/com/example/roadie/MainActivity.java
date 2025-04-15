package com.example.roadie;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    ListView deviceListView;
    ArrayAdapter<String> adapter;
    ArrayList<BluetoothDevice> pairDevicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deviceListView = findViewById(R.id.deviceListView);

        // 🔐 Check Bluetooth permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_PERMISSION
            );
            return; // Wait for user to grant permission
        }

        listPairedDevices();
    }

    private void listPairedDevices() {
        BluetoothAdapter adapterBT = BluetoothAdapter.getDefaultAdapter();
        if (adapterBT == null) {
            Toast.makeText(this, "Bluetooth is not available on this device.", Toast.LENGTH_LONG).show();
            return;
        }

        Set<BluetoothDevice> devices;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        devices = adapterBT.getBondedDevices();

        ArrayList<String> deviceNames = new ArrayList<>();
        pairDevicesList.clear();

        for (BluetoothDevice device : devices) {
            deviceNames.add(device.getName() + "\n" + device.getAddress());
            pairDevicesList.add(device);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        deviceListView.setAdapter(adapter);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = pairDevicesList.get(position);
            boolean connected = BluetoothHelper.connectToBluetooth(selectedDevice, this);
            Toast.makeText(this,
                    connected ? "Connected to " + selectedDevice.getName() : "Failed to connect",
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Bluetooth permission granted!");
                listPairedDevices(); // Retry now that permission is granted
            } else {
                Toast.makeText(this, "Bluetooth permission is required for this feature.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isNotificationListenerEnabled() {
        // Check if your notification listener service is enabled
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(NotificationListenerService.class.getName());
    }
}
