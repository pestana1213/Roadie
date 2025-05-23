package com.example.roadie;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
    ArrayList<BluetoothDevice> pairedDeviceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = findViewById(R.id.deviceListView);
        Button btnConnect = findViewById(R.id.btn_connect);
        Button btnGrantAccess = findViewById(R.id.btn_grant_access);

        btnGrantAccess.setOnClickListener(view -> openNotificationAccessSettings());

        // Request permissions
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BLUETOOTH_PERMISSION);
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "nav_channel", "Navigation", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Turn-by-turn navigation directions");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        btnConnect.setOnClickListener(view -> checkBluetoothConnection());
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void checkBluetoothConnection() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            return;
        }

        if (!adapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
            return;
        }

        listPairedDevices();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void listPairedDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = adapter.getBondedDevices();

        pairedDeviceList.clear();
        ArrayList<String> names = new ArrayList<>();

        for (BluetoothDevice device : devices) {
            names.add(device.getName() + "\n" + device.getAddress());
            pairedDeviceList.add(device);
        }

        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        deviceListView.setAdapter(listAdapter);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = pairedDeviceList.get(position);
            boolean connected = BluetoothHelper.connectToClassic(selectedDevice);

            if (connected) {
                Toast.makeText(this, "Connected to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                BluetoothHelper.sendData("Welcome to Roadie!");
                deviceListView.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openNotificationAccessSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open notification settings", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNotificationAccessEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNotificationAccessEnabled()) {
            Log.d("MainActivity", "Notification access granted!");
        } else {
            Log.d("MainActivity", "Notification access NOT granted.");
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Bluetooth permission granted");
                listPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }
}
