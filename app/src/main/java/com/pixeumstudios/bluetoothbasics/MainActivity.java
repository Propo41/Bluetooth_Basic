package com.pixeumstudios.bluetoothbasics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.pixeumstudios.bluetoothbasics.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

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
    private static final String TAG = "MAIN_ACTIVITY";
    private static final int DISCOVERABLE_OPEN_FOR = 2 * 60;
    private static final int REQUEST_ENABLE_DISCOVERY = 4;
    ;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<DeviceInfo> deviceList;
    private ActivityMainBinding binding;
    private IntentFilter intentFilter;
    private DeviceListRecyclerAdapter recyclerAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private BluetoothConnectionService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private BluetoothDevice mBTDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requiredSetup();
        setupUiElements();

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

    private void setupUiElements() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceList = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(new DeviceInfo(device.getName(), device.getAddress(), false, device));
                //String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }

        binding.listView.setHasFixedSize(true); // this will lock the scrolling. We cant scroll
        layoutManager = new LinearLayoutManager(this);
        recyclerAdapter = new DeviceListRecyclerAdapter(deviceList);

        binding.listView.setLayoutManager(layoutManager);
        binding.listView.setAdapter(recyclerAdapter);

        recyclerAdapter.setOnItemClickListener(position -> {
            String macAddress = deviceList.get(position).getMacAddress();
            Toast.makeText(MainActivity.this, macAddress + " selected", Toast.LENGTH_SHORT).show();

            bluetoothAdapter.cancelDiscovery();
            binding.progressBar.setVisibility(View.INVISIBLE);
            startBTConnection( deviceList.get(position).getBluetoothDevice(), MY_UUID_INSECURE, position);
            /*//create the pairing bond.
            Log.d(TAG, "Trying to pair with " + deviceList.get(position).getName());
            deviceList.get(position).getBluetoothDevice().createBond();
            mBTDevice = deviceList.get(position).getBluetoothDevice();
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this, bluetoothAdapter);*/

        });

        binding.makeDiscoverableBtn.setOnClickListener(view -> {
            // make the device discoverable for 2 mins.
            // Your activity then receives a call to the onActivityResult() callback
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_OPEN_FOR);
            startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERY);
        });

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
            Toast.makeText(this, "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // enabling bluetooth if disabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //Broadcasts when bond state changes (ie:pairing)
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(mReceiver2, filter);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "App requires bluetooth to function", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_ENABLE_DISCOVERY && resultCode == DISCOVERABLE_OPEN_FOR) {
            Toast.makeText(this, "Device is now discoverable", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
     * starting the connection. First checks if device is paired. If paired, then start client thread
     * if not, then start the pairing process
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid, int position){
        mBluetoothConnection = new BluetoothConnectionService(MainActivity.this, bluetoothAdapter);
        //create the pairing bond if device not paired
        if(device.getBondState() == BluetoothDevice.BOND_NONE){
            Log.d(TAG, "Trying to pair with " + deviceList.get(position).getName());
            device.createBond();
            mBTDevice = device;
        }else{
            // if device is already paired, then start the chat service
            Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
            mBluetoothConnection.startClient(device, uuid);
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
            int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

            switch (mode) {
                //Device is in Discoverable Mode
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                    break;
                //Device not in discoverable mode
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                    break;
                case BluetoothAdapter.SCAN_MODE_NONE:
                    Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                    break;
                case BluetoothAdapter.STATE_CONNECTING:
                    Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                    break;
                case BluetoothAdapter.STATE_CONNECTED:
                    Log.d(TAG, "mBroadcastReceiver2: Connected.");
                    break;
            }

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed alread
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    deviceList.add(new DeviceInfo(device.getName(), device.getAddress(), false, device));
                    recyclerAdapter.notifyItemInserted(deviceList.size());
                }
                // When discovery is finished,
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                binding.progressBar.setVisibility(View.INVISIBLE);
                if (recyclerAdapter.getItemCount() == 0) {
                    Toast.makeText(context, "no devices found", Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                Toast.makeText(context, "Bluetooth state changed", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show();
            }
        }
    };


    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
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
        this.unregisterReceiver(mReceiver2);

    }
}