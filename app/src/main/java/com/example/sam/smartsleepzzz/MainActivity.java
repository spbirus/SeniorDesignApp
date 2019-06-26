package com.example.sam.smartsleepzzz;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "tag";
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    private String name;
    private String email;
    private Uri photoUrl;


//    // Data
//    private BleManager mBleManager;
//    private boolean mIsScanPaused = true;
//    private BleDevicesScanner mScanner;
//
//    private ArrayList<BluetoothDeviceData> mScannedDevices;
//    private BluetoothDeviceData mSelectedDeviceData;
//    private Class<?> mComponentToStartWhenConnected;
//    private boolean mShouldEnableWifiOnQuit = false;
//    private String mLatestCheckedDeviceAddress;
//
//    private long mLastUpdateMillis;
//    private final static long kMinDelayToUpdateUI = 200;    // in milliseconds
//
//    private BluetoothDevice heartRateDevice;
//
//    //data receiving stuff
//    private volatile ArrayList<UartDataChunk> mDataBuffer;
//    private volatile int mReceivedBytes;
//    private boolean mShowDataInHexFormat;

    // Bluetooth's variables
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothManager bluetoothManager;
    BluetoothScanCallback bluetoothScanCallback;
    BluetoothGatt gattClient;

    BluetoothGattCharacteristic characteristicID; // To get Value

    // UUID's (set yours)
    final UUID SERVICE_UUID = UUID.fromString("ab0828b1-198e-4351-b779-901fa0e0371e");
    final UUID CHARACTERISTIC_UUID_ID = UUID.fromString("1a220d0a-6b06-4767-8692-243153d94d85");
    final UUID DESCRIPTOR_UUID_ID = UUID.fromString("ec6e1003-884b-4a1c-850f-1cfce9cf6567");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mBleManager = BleManager.getInstance(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            name = user.getDisplayName();
            email = user.getEmail();
            photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            String uid = user.getUid();

            // Bluetooth
            bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    public void onProfileClick(View view){
        Intent myIntent = new Intent(this, ProfileActivity.class);
        myIntent.putExtra("name", name); //Optional parameters
        myIntent.putExtra("email", email);
        startActivity(myIntent);
    }

    public void onConnectClick(View view){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
//        boolean isScanning = mScanner != null && mScanner.isScanning();
//        if (isScanning) {
//            stopScanning();
//        } else {
            startScan();
//        }

    }

    // BLUETOOTH SCAN

    private void startScan(){
        Log.i(TAG,"startScan()");
        bluetoothScanCallback = new BluetoothScanCallback();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(bluetoothScanCallback);
    }

    // BLUETOOTH CONNECTION
    private void connectDevice(BluetoothDevice device) {
        if (device == null) Log.i(TAG,"Device is null");
        GattClientCallback gattClientCallback = new GattClientCallback();
        gattClient = device.connectGatt(this,false,gattClientCallback);
    }

    // BLE Scan Callbacks
    private class BluetoothScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG, "onScanResult");
            if (result.getDevice().getName() != null){
                if (result.getDevice().getName().equals("Adafruit Bluefruit LE")) {
                    // When find your device, connect.
                    connectDevice(result.getDevice());
                    TextView d_name = (TextView) findViewById(R.id.deviceName);
                    TextView d_connection = (TextView) findViewById(R.id.deviceConnected);

                    d_name.setText(result.getDevice().getName());
                    d_connection.setText("Connected!");
                    bluetoothLeScanner.stopScan(bluetoothScanCallback); // stop scan
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(TAG, "onBathScanResults");
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "ErrorCode: " + errorCode);
        }
    }

    // Bluetooth GATT Client Callback
    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG,"onConnectionStateChange");

            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i(TAG, "onConnectionStateChange GATT FAILURE");
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onConnectionStateChange != GATT_SUCCESS");
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange CONNECTED");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange DISCONNECTED");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG,"onServicesDiscovered");
            if (status != BluetoothGatt.GATT_SUCCESS) return;

            // Reference your UUIDs
            characteristicID = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID_ID);
            gatt.setCharacteristicNotification(characteristicID,true);

            BluetoothGattDescriptor descriptor = characteristicID.getDescriptor(DESCRIPTOR_UUID_ID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG,"onCharacteristicRead");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG,"onCharacteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG,"onCharacteristicChanged");
            Log.v("Here", new String(characteristic.getValue()));
            // Here you can read the characteristc's value
            // new String(characteristic.getValue();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.i(TAG,"onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG,"onDescriptorWrite");
        }
    }


}
