package com.example.roadie;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.app.NotificationManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
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
    BLEAdvertiser bleAdvertiser;
    MyGattServer gattServer;

    boolean autoConnect = true;

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = findViewById(R.id.deviceListView);
        Button btnConnect = findViewById(R.id.btn_connect);
        Button btnGrantAccess = findViewById(R.id.btn_grant_access);
        btnGrantAccess.setOnClickListener(view -> {
            openNotificationAccessSettings();
        });

        String[] permissions = {
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            startBluetooth();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "nav_channel",
                    "Navigation",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Turn-by-turn navigation directions");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        btnConnect.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_PERMISSION
                );
            } else {
                checkBluetoothConnection();
            }
        });
    }
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT})
    private void startBluetooth() {
        MyGattServer gattServer = new MyGattServer(this);
        gattServer.startServer();
        BLEAdvertiser advertiser = new BLEAdvertiser(this);
        advertiser.startAdvertising();
    }

    private boolean hasPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private void checkBluetoothConnection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            return;
        }

        BluetoothAdapter adapterBT = BluetoothAdapter.getDefaultAdapter();
        if (adapterBT == null) {
            Log.d("MainActivity", "Cant connect device");
            Toast.makeText(this, "Bluetooth is not available on this device.", Toast.LENGTH_LONG).show();
            return;
        }

        if (adapterBT.isEnabled()) {
            BluetoothDevice connectedDevice = getConnectedBluetoothDevice();
            Log.d("MainActivity", "Connected device: " + (connectedDevice != null ? connectedDevice.getName() : "None"));
            if (connectedDevice != null) {
                Toast.makeText(this, "Already connected", Toast.LENGTH_LONG).show();
            } else {
                listPairedDevices();
            }
        } else {
            Toast.makeText(this, "Bluetooth is disabled.", Toast.LENGTH_SHORT).show();
        }
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
            boolean connected = BluetoothHelper.connectToBluetooth(selectedDevice, this, gattCallback);
            if (connected) {
                Toast.makeText(this, "Connected to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                String dataToSend = "Welcome to Rodie!";
                BluetoothHelper.sendData(dataToSend);
                deviceListView.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("MainActivity", "BLE connected to: " + gatt.getDevice().getName());
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("MainActivity", "BLE disconnected from: " + gatt.getDevice().getName());
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "BLE services discovered");

                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        int props = characteristic.getProperties();
                        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0 ||
                                (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {

                            BluetoothHelper.writeCharacteristic = characteristic;
                            BluetoothHelper.bluetoothGatt = gatt;
                            Log.i(TAG, "Writable characteristic found: " + characteristic.getUuid());

                            BluetoothHelper.sendData("TEXT:Hello from phone|");

                            return;
                        }
                    }
                }

                Log.w(TAG, "No writable characteristic found");
            } else {
                Log.e(TAG, "Service discovery failed, status: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Message chunk delivered");
            }
        }
    };

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

    private BluetoothDevice getConnectedBluetoothDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        BluetoothAdapter adapterBT = BluetoothAdapter.getDefaultAdapter();
        if (adapterBT == null) return null;

        Set<BluetoothDevice> pairedDevices = adapterBT.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            int state = adapterBT.getProfileConnectionState(BluetoothAdapter.STATE_CONNECTED);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                return device;
            }
        }
        return null;
    }

    private void openNotificationAccessSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open Notification Settings", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNotificationAccessEnabled() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNotificationAccessEnabled()) {
            // Proceed to use the notification listener
            Log.d("MainActivity", "Notification access granted!");
        } else {
            Log.d("MainActivity", "Notification access not granted.");
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleAdvertiser != null) {
            bleAdvertiser.stopAdvertising();
        }
    }
}
