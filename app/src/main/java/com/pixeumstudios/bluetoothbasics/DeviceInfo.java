package com.pixeumstudios.bluetoothbasics;

import android.bluetooth.BluetoothDevice;

public class DeviceInfo {
    private String name;
    private String macAddress;
    private boolean connected;
    private BluetoothDevice bluetoothDevice;

    public DeviceInfo(String name, String macAddress, boolean connected, BluetoothDevice bluetoothDevice) {
        this.name = name;
        this.macAddress = macAddress;
        this.connected = connected;
        this.bluetoothDevice = bluetoothDevice;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
