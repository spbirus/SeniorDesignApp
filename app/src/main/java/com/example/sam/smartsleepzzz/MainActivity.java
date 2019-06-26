package com.example.sam.smartsleepzzz;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
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


    // Data
    private BleManager mBleManager;
    private boolean mIsScanPaused = true;
    private BleDevicesScanner mScanner;

    private ArrayList<BluetoothDeviceData> mScannedDevices;
    private BluetoothDeviceData mSelectedDeviceData;
    private Class<?> mComponentToStartWhenConnected;
    private boolean mShouldEnableWifiOnQuit = false;
    private String mLatestCheckedDeviceAddress;

    private long mLastUpdateMillis;
    private final static long kMinDelayToUpdateUI = 200;    // in milliseconds

    private BluetoothDevice heartRateDevice;

    //data receiving stuff
    private volatile ArrayList<UartDataChunk> mDataBuffer;
    private volatile int mReceivedBytes;
    private boolean mShowDataInHexFormat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBleManager = BleManager.getInstance(this);

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
        boolean isScanning = mScanner != null && mScanner.isScanning();
        if (isScanning) {
            stopScanning();
        } else {
            startScan(null);
        }

    }

    private void stopScanning() {
        // Stop scanning
        if (mScanner != null) {
            mScanner.stop();
            mScanner = null;
        }

        Toast.makeText(this, "Stopped scan", Toast.LENGTH_LONG);
    }

    // region Scan
    public void startScan(final UUID[] servicesToScan) {
        Log.d(TAG, "startScan");

        // Stop current scanning (if needed)
        stopScanning();

        // Configure scanning
        BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(getApplicationContext());
        if (BleUtils.getBleStatus(this) != BleUtils.STATUS_BLE_ENABLED) {
            Log.w(TAG, "startScan: BluetoothAdapter not initialized or unspecified address.");
        } else {
            mScanner = new BleDevicesScanner(bluetoothAdapter, servicesToScan, new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    //final String deviceName = device.getName();
                    //Log.d(TAG, "Discovered device: " + (deviceName != null ? deviceName : "<unknown>"));

                    BluetoothDeviceData previouslyScannedDeviceData = null;
                    if (mScannedDevices == null) {
                        mScannedDevices = new ArrayList<>();       // Safeguard
                    }

                    // Check that the device was not previously found
                    for (BluetoothDeviceData deviceData : mScannedDevices) {
                        if (deviceData.device.getAddress().equals(device.getAddress())) {
                            previouslyScannedDeviceData = deviceData;
                            break;
                        }
                    }

                    BluetoothDeviceData deviceData;
                    if (previouslyScannedDeviceData == null) {
                        // Add it to the mScannedDevice list
                        deviceData = new BluetoothDeviceData();
                        mScannedDevices.add(deviceData);
                    } else {
                        deviceData = previouslyScannedDeviceData;
                    }

                    deviceData.device = device;
                    deviceData.rssi = rssi;
                    deviceData.scanRecord = scanRecord;
                    decodeScanRecords(deviceData);

                    // Update device data
                    long currentMillis = SystemClock.uptimeMillis();
                    if (previouslyScannedDeviceData == null || currentMillis - mLastUpdateMillis > kMinDelayToUpdateUI) {          // Avoid updating when not a new device has been found and the time from the last update is really short to avoid updating UI so fast that it will become unresponsive
                        mLastUpdateMillis = currentMillis;

                        Log.v("scanned devices",  mScannedDevices.toString());
                        for(BluetoothDeviceData d : mScannedDevices){
                            if(d != null && d.advertisedName != null && d.advertisedName.equals("Adafruit Bluefruit LE")){
                                stopScanning();
                                Log.v("Found device", "device found");
                                Toast.makeText(MainActivity.this, "found device", Toast.LENGTH_LONG);
                                heartRateDevice = d.device;
                                connect(heartRateDevice);
                            }
                        }
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                updateUI();
//                            }
//                        });
                    }

                }
            });

            // Start scanning
            mScanner.start();
        }

        // Update UI
        Toast.makeText(this, "Scanning for bluetooth", Toast.LENGTH_LONG);
    }

    private void connect(BluetoothDevice device) {
        boolean isConnecting = mBleManager.connect(this, device.getAddress());
        TextView d_name = (TextView) findViewById(R.id.deviceName);
        TextView d_connection = (TextView) findViewById(R.id.deviceConnected);

        d_name.setText(heartRateDevice.getName());
        d_connection.setText("Connected!");
        if (isConnecting) {
//            showConnectionStatus(true);
        }
    }

    private void decodeScanRecords(BluetoothDeviceData deviceData) {
        // based on http://stackoverflow.com/questions/24003777/read-advertisement-packet-in-android
        final byte[] scanRecord = deviceData.scanRecord;

        ArrayList<UUID> uuids = new ArrayList<>();
        byte[] advertisedData = Arrays.copyOf(scanRecord, scanRecord.length);
        int offset = 0;
        deviceData.type = BluetoothDeviceData.kType_Unknown;

        // Check if is an iBeacon ( 0x02, 0x0x1, a flag byte, 0x1A, 0xFF, manufacturer (2bytes), 0x02, 0x15)
        final boolean isBeacon = advertisedData[0] == 0x02 && advertisedData[1] == 0x01 && advertisedData[3] == 0x1A && advertisedData[4] == (byte) 0xFF && advertisedData[7] == 0x02 && advertisedData[8] == 0x15;

        // Check if is an URIBeacon
        final byte[] kUriBeaconPrefix = {0x03, 0x03, (byte) 0xD8, (byte) 0xFE};
        final boolean isUriBeacon = Arrays.equals(Arrays.copyOf(scanRecord, kUriBeaconPrefix.length), kUriBeaconPrefix) && advertisedData[5] == 0x16 && advertisedData[6] == kUriBeaconPrefix[2] && advertisedData[7] == kUriBeaconPrefix[3];

        if (isBeacon) {
            deviceData.type = BluetoothDeviceData.kType_Beacon;

            // Read uuid
            offset = 9;
            UUID uuid = BleUtils.getUuidFromByteArrayBigEndian(Arrays.copyOfRange(scanRecord, offset, offset + 16));
            uuids.add(uuid);
            offset += 16;

            // Skip major minor
            offset += 2 * 2;   // major, minor

            // Read txpower
            final int txPower = advertisedData[offset++];
            deviceData.txPower = txPower;
        } else if (isUriBeacon) {
            deviceData.type = BluetoothDeviceData.kType_UriBeacon;

            // Read txpower
            final int txPower = advertisedData[9];
            deviceData.txPower = txPower;
        } else {
            // Read standard advertising packet
            while (offset < advertisedData.length - 2) {
                // Length
                int len = advertisedData[offset++];
                if (len == 0) break;

                // Type
                int type = advertisedData[offset++];
                if (type == 0) break;

                // Data
//            Log.d(TAG, "record -> lenght: " + length + " type:" + type + " data" + data);

                switch (type) {
                    case 0x02:          // Partial list of 16-bit UUIDs
                    case 0x03: {        // Complete list of 16-bit UUIDs
                        while (len > 1) {
                            int uuid16 = advertisedData[offset++] & 0xFF;
                            uuid16 |= (advertisedData[offset++] << 8);
                            len -= 2;
                            uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                        }
                        break;
                    }

                    case 0x06:          // Partial list of 128-bit UUIDs
                    case 0x07: {        // Complete list of 128-bit UUIDs
                        while (len >= 16) {
                            try {
                                // Wrap the advertised bits and order them.
                                UUID uuid = BleUtils.getUuidFromByteArraLittleEndian(Arrays.copyOfRange(advertisedData, offset, offset + 16));
                                uuids.add(uuid);

                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "BlueToothDeviceFilter.parseUUID: " + e.toString());
                            } finally {
                                // Move the offset to read the next uuid.
                                offset += 16;
                                len -= 16;
                            }
                        }
                        break;
                    }

                    case 0x09: {
                        byte[] nameBytes = new byte[len - 1];
                        for (int i = 0; i < len - 1; i++) {
                            nameBytes[i] = advertisedData[offset++];
                        }

                        String name = null;
                        try {
                            name = new String(nameBytes, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        deviceData.advertisedName = name;
                        break;
                    }

                    case 0x0A: {        // TX Power
                        final int txPower = advertisedData[offset++];
                        deviceData.txPower = txPower;
                        break;
                    }

                    default: {
                        offset += (len - 1);
                        break;
                    }
                }
            }

            // Check if Uart is contained in the uuids
            boolean isUart = false;
            for (UUID uuid : uuids) {
                if (uuid.toString().equalsIgnoreCase("6e400001-b5a3-f393-e0a9-e50e24dcca9e")) {
                    isUart = true;
                    break;
                }
            }
            if (isUart) {
                deviceData.type = BluetoothDeviceData.kType_Uart;
            }
        }

        deviceData.uuids = uuids;
    }

    // region Helpers
    private class BluetoothDeviceData {
        BluetoothDevice device;
        public int rssi;
        byte[] scanRecord;
        private String advertisedName;           // Advertised name
        private String cachedNiceName;
        private String cachedName;

        // Decoded scan record (update R.array.scan_devicetypes if this list is modified)
        static final int kType_Unknown = 0;
        static final int kType_Uart = 1;
        static final int kType_Beacon = 2;
        static final int kType_UriBeacon = 3;

        public int type;
        int txPower;
        ArrayList<UUID> uuids;

        String getName() {
            if (cachedName == null) {
                cachedName = device.getName();
                if (cachedName == null) {
                    cachedName = advertisedName;      // Try to get a name (but it seems that if device.getName() is null, this is also null)
                }
            }

            return cachedName;
        }

        String getNiceName() {
            if (cachedNiceName == null) {
                cachedNiceName = getName();
                if (cachedNiceName == null) {
                    cachedNiceName = device.getAddress();
                }
            }

            return cachedNiceName;
        }
    }
    //endregion


    // receiving data stuff
    public static class UartInterfaceActivity extends AppCompatActivity implements BleManager.BleManagerListener {
        // Log
        private final String TAG = UartInterfaceActivity.class.getSimpleName();

        // Service Constants
        public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
        public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
        public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
        public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";
        public static final int kTxMaxCharacters = 20;

        // Data
        protected BleManager mBleManager;
        protected BluetoothGattService mUartService;
        private boolean isRxNotificationEnabled = false;


        // region Send Data to UART
        protected void sendData(String text) {
            final byte[] value = text.getBytes(Charset.forName("UTF-8"));
            sendData(value);
        }

        protected void sendData(byte[] data) {
            if (mUartService != null) {
                // Split the value into chunks (UART service has a maximum number of characters that can be written )
                for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                    final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                    mBleManager.writeService(mUartService, UUID_TX, chunk);
                }
            } else {
                Log.w(TAG, "Uart Service not discovered. Unable to send data");
            }
        }

        // Send data to UART and add a byte with a custom CRC
        protected void sendDataWithCRC(byte[] data) {

            // Calculate checksum
            byte checksum = 0;
            for (byte aData : data) {
                checksum += aData;
            }
            checksum = (byte) (~checksum);       // Invert

            // Add crc to data
            byte dataCrc[] = new byte[data.length + 1];
            System.arraycopy(data, 0, dataCrc, 0, data.length);
            dataCrc[data.length] = checksum;

            // Send it
            Log.d(TAG, "Send to UART: " + BleUtils.bytesToHexWithSpaces(dataCrc));
            sendData(dataCrc);
        }
        // endregion

        // region SendDataWithCompletionHandler
        protected interface SendDataCompletionHandler {
            void sendDataResponse(String data);
        }

        final private Handler sendDataTimeoutHandler = new Handler();
        private Runnable sendDataRunnable = null;
        private SendDataCompletionHandler sendDataCompletionHandler = null;

        protected void sendData(byte[] data, SendDataCompletionHandler completionHandler) {

            if (completionHandler == null) {
                sendData(data);
                return;
            }

            if (!isRxNotificationEnabled) {
                Log.w(TAG, "sendData warning: RX notification not enabled. completionHandler will not be executed");
            }

            if (sendDataRunnable != null || sendDataCompletionHandler != null) {
                Log.d(TAG, "sendData error: waiting for a previous response");
                return;
            }

            Log.d(TAG, "sendData");
            sendDataCompletionHandler = completionHandler;
            sendDataRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "sendData timeout");
                    final SendDataCompletionHandler dataCompletionHandler = sendDataCompletionHandler;

                    UartInterfaceActivity.this.sendDataRunnable = null;
                    UartInterfaceActivity.this.sendDataCompletionHandler = null;

                    dataCompletionHandler.sendDataResponse(null);
                }
            };

            sendDataTimeoutHandler.postDelayed(sendDataRunnable, 2 * 1000);
            sendData(data);

        }

        protected boolean isWaitingForSendDataResponse() {
            return sendDataRunnable != null;
        }

        // endregion

        // region BleManagerListener  (used to implement sendData with completionHandler)

        @Override
        public void onConnected() {

        }

        @Override
        public void onConnecting() {

        }

        @Override
        public void onDisconnected() {

        }

        @Override
        public void onServicesDiscovered() {
            mUartService = mBleManager.getGattService(UUID_SERVICE);
        }

        protected void enableRxNotifications() {
            isRxNotificationEnabled = true;
            mBleManager.enableNotification(mUartService, UUID_RX, true);
        }

        @Override
        public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
            // Check if there is a pending sendDataRunnable
            if (sendDataRunnable != null) {
                if (characteristic.getService().getUuid().toString().equalsIgnoreCase(UUID_SERVICE)) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(UUID_RX)) {

                        Log.d(TAG, "sendData received data");
                        sendDataTimeoutHandler.removeCallbacks(sendDataRunnable);
                        sendDataRunnable = null;

                        if (sendDataCompletionHandler != null) {
                            final byte[] bytes = characteristic.getValue();
                            final String data = new String(bytes, Charset.forName("UTF-8"));

                            final SendDataCompletionHandler dataCompletionHandler = sendDataCompletionHandler;
                            sendDataCompletionHandler = null;
                            dataCompletionHandler.sendDataResponse(data);
                        }
                    }
                }
            }
        }

        @Override
        public void onDataAvailable(BluetoothGattDescriptor descriptor) {

        }

        @Override
        public void onReadRemoteRssi(int rssi) {

        }

        // endregion
    }

    public synchronized void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        UartInterfaceActivity uart = new UartInterfaceActivity();
        uart.onDataAvailable(characteristic);
        // UART RX
        if (characteristic.getService().getUuid().toString().equalsIgnoreCase("6e400001-b5a3-f393-e0a9-e50e24dcca9e")) {
            if (characteristic.getUuid().toString().equalsIgnoreCase("6e400003-b5a3-f393-e0a9-e50e24dcca9e")) {
                final byte[] bytes = characteristic.getValue();
                mReceivedBytes += bytes.length;

                final UartDataChunk dataChunk = new UartDataChunk(System.currentTimeMillis(), UartDataChunk.TRANSFERMODE_RX, bytes);
                mDataBuffer.add(dataChunk);

                final String formattedData = mShowDataInHexFormat ? BleUtils.bytesToHex2(bytes) : BleUtils.bytesToText(bytes, true);

                Log.v("data from uart ", formattedData);
            }
        }
    }



}
