package com.pixeumstudios.bluetoothbasics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pixeumstudios.bluetoothbasics.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Set;

/*
 * REFERENCES:
 * https://code.tutsplus.com/tutorials/create-a-bluetooth-scanner-with-androids-bluetooth-api--cms-24084
 * https://developer.android.com/guide/topics/connectivity/bluetooth.html
 * https://github.com/android/connectivity-samples
 * https://stackoverflow.com/questions/34966133/android-bluetooth-discovery-doesnt-find-any-device
 * https://stackoverflow.com/questions/38809845/android-bluetooth-app-cant-discover-other-devices
 * https://stackoverflow.com/questions/37423199/bluetooth-le-gatt-not-finding-any-devices/37423244#37423244
 * https://stackoverflow.com/questions/30222409/android-broadcast-receiver-bluetooth-events-catching
 *
 * https://create.arduino.cc/projecthub/azoreanduino/simple-bluetooth-lamp-controller-using-android-and-arduino-aa2253
 *
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_BT = 3;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final String TAG = "BLUETOOTH";
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList;
    private ActivityMainBinding binding;
    private ArrayAdapter<String> listAdapter;
    private IntentFilter intentFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requiredSetup();
        setupListView();

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);

    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, intentFilter);

    }

    private void setupListView() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceList = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(device.getName());
                //String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }


        listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, deviceList);

        binding.listView.setAdapter(listAdapter);

    }

    private void requiredSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_LOCATION_BT);
            }
        }

        // checking if device supports bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i(TAG, bluetoothAdapter.isEnabled() + "");
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Device doesnt support bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // enabling bluetooth if disabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "App requires bluetooth to function", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_BT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, yay! Start the Bluetooth device scan.
                startScan();
            } else {
                // Alert the user that this application requires the location permission to perform the scan.
                Toast.makeText(this, "Requires location permission to work", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * initializes device discovery. The list of devices are sent to the broadcast receiver
     * The discovery process usually involves an inquiry scan of about 12 seconds,
     * followed by a page scan of each device found to retrieve its Bluetooth name.
     */
    private void startScan() {
        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
            Toast.makeText(this, "stopping discovery", Toast.LENGTH_SHORT).show();
            bluetoothAdapter.cancelDiscovery();
        } else {
            try {
                registerReceiver(mReceiver, intentFilter);
                // Request discover from BluetoothAdapter
                boolean as = bluetoothAdapter.startDiscovery();
                Toast.makeText(this, as + "", Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.VISIBLE);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Toast.makeText(this, "receivers not registered", Toast.LENGTH_SHORT).show();
            }

        }
    }

    /**
     * @param view the button view that is pressed and the scanning begins
     */
    public void searchDevices(View view) {
        // checking if location permissions enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_BT);
        } else {
            startScan();
        }
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Toast.makeText(getApplicationContext(), "receiver working", Toast.LENGTH_SHORT).show();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already

                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    listAdapter.add(device.getName() + "\n" + device.getAddress() + "\n" +
                            device.getAlias() + "\n" + device.getType());
                    listAdapter.notifyDataSetChanged();
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //setProgressBarIndeterminateVisibility(false);
                //setTitle(R.string.select_device);
                binding.progressBar.setVisibility(View.INVISIBLE);
                if (listAdapter.getCount() == 0) {
                    Toast.makeText(context, "no devices found", Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                Toast.makeText(context, "Bluetooth state changed", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show();

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }
}