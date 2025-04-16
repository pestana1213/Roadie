package com.example.roadie;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    boolean autoConnect = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = findViewById(R.id.deviceListView);
        Button btnConnect = findViewById(R.id.btn_connect);

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
            boolean connected = BluetoothHelper.connectToBluetooth(selectedDevice, this);
            if (connected) {
                Toast.makeText(this, "Connected to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                deviceListView.setVisibility(View.GONE); // Oculta a lista
            } else {
                Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show();
            }
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
}
