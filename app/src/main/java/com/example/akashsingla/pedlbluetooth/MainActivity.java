package com.example.akashsingla.pedlbluetooth;

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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

//    UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D);
//    UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37);
//    UUID HEART_RATE_CONTROL_POINT_CHAR_UUID = convertFromInteger(0x2A39);
//    UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902);
//    UUID chtt = convertFromInteger(0x2A05);


    private static byte[] key1 = {32, 87, 47, 82, 54, 75, 63, 71, 48, 80, 65, 88, 17, 99, 45, 43};
    private static byte[] msg1 = {0x06, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private Button butConnect;
    private Button butUnlock;
    private Button butLockStatus;
    private Button butInfo;
    private Button butDissconnect;
    private Button butNext;
    private TextView tvScreen;
    private EditText etMacAddress;

    private BluetoothLeScanner scanner;

    private BluetoothGatt gatt;
    private byte[] accessToken;
//    private String ACTIONABLE_MAC_ADDRESS = "C4  A8  28  07  7F  7A";
    private String ACTIONABLE_MAC_ADDRESS = "C4  A8  28  05  B0  67";
    private static final int KEY_MAC_ADDRESS = 513;
    private static final UUID writableUUID = UUID.fromString("000036f5-0000-1000-8000-00805f9b34fb");
    private static final UUID readableUUID = UUID.fromString("000036f6-0000-1000-8000-00805f9b34fb");
    private static final UUID serviceUUID = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");

    private static final String RESPONSE_PREFIX_ACCESS_TOKEN = "06  02  07";
    private static final String RESPONSE_PREFIX_UNLOCK = "05  02  01";
    private static final String RESPONSE_PREFIX_LOCK_STATUS = "05  0F  01";
    private static final String RESPONSE_PREFIX_DEVICE_INFO = "05  11  0C";

    private static final int REQUEST_CODE_LOCATION_COURSE = 1001;
    private static final int REQUEST_CODE_BLUETOOTH = 1000;
    private static final int REQUEST_CODE_LOCATION_FINE = 1002;
    private BluetoothManager bluetoothManager = null;
    private boolean isScanning = false;
    private HandleScanCallback handleScanCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handleScanCallback = new HandleScanCallback();

        bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);


        BluetoothManager m1 = bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        System.out.println("m1 = " + m1);
        System.out.println("manager = " + bluetoothManager);

        mBluetoothAdapter = bluetoothManager.getAdapter();
        scanner = mBluetoothAdapter.getBluetoothLeScanner();
        init();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH);
        } else {
            validateLocation();
//            scanLeDevice(true);
        }
    }

    private void validateLocation() {
        if (checkLocationPermission()) {
            scanLeDevice(true);
            tvScreen.setText("Connecting");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_COURSE);
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return false;
        }

        return true;
    }

    private void init() {
        mHandler = new Handler();
        butConnect = findViewById(R.id.butConnect);
        butUnlock = findViewById(R.id.butUnlock);

        butLockStatus = findViewById(R.id.butLockStatus);
        butInfo = findViewById(R.id.butInfo);
        butDissconnect = findViewById(R.id.butDissconnect);
        tvScreen = findViewById(R.id.tvScreen);
        etMacAddress = findViewById(R.id.etMacAddress);
        butNext = findViewById(R.id.butNext);

        butConnect.setOnClickListener(this);
        butConnect.callOnClick();
        butUnlock.setOnClickListener(this);
        butLockStatus.setOnClickListener(this);
        butInfo.setOnClickListener(this);
        butDissconnect.setOnClickListener(this);
        butNext.setOnClickListener(this);

    }

    private void scanLeDevice(final boolean enable) {

        if (!isScanning) {
            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            ScanFilter.Builder builder = new ScanFilter.Builder();
            String str = etMacAddress.getText().toString();
            String macy = "C4A828077F7A";

            if (str != null) {
                String mac[] = str.split(":");
                macy = "";
                for (String m : mac) {
                    macy = macy + m;
                }
            }

//            byte str1[] = "C4".getBytes();
//            byte str2[] = "A8".getBytes();
//            byte str3[] = "28".getBytes();
//            byte str4[] = "07".getBytes();
//            byte str5[] = "7F".getBytes();
//            byte str6[] = "7A".getBytes();

//            builder.setManufacturerData(0x004c, new byte[] {});
            byte macByte[] = hexStringToByteArray(macy);
            System.out.println("data = " + getHex(macByte));
            builder.setManufacturerData(KEY_MAC_ADDRESS, macByte);

            builder.setDeviceName("KKSLOCK");

            ScanFilter filter = builder.build();

            System.out.println("id = " + filter.getManufacturerId());
            System.out.println("data = " + getHex(filter.getManufacturerData()));
            List<ScanFilter> filters = new ArrayList<>();
            filters.add(filter);
            scanner.startScan(filters, scanSettings, handleScanCallback);
            System.out.println("Scan Started");
            isScanning = true;

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isScanning) {
                        System.out.println("Scan finished");
                        scanner.stopScan(handleScanCallback);
                        setScreenText("Scan Completed.");
                        disconnectGatt();
                        isScanning = false;
                    }
                }
            }, SCAN_PERIOD);
        }
    }

    private class HandleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);


            System.out.println("callbackType = " + callbackType);

            BluetoothDevice bde = null;
            if ((bde = result.getDevice()) != null) {
                System.out.println(bde.getAddress() + "      " + bde.getName());
            }

            SparseArray<byte[]> sparseArrays = result.getScanRecord().getManufacturerSpecificData();

            if (sparseArrays != null && sparseArrays.size() > 0) {
                byte macByte[] = sparseArrays.get(KEY_MAC_ADDRESS);     // KEY_MAC_ADDRESS = 513

                if (macByte != null && macByte.length > 0) {
                    String macStr = getHex(macByte);
                    System.out.println("MacStr = " + macStr + "    name = " + result.getDevice().getName());
                    macStr = macStr.trim();
                    if (ACTIONABLE_MAC_ADDRESS.equalsIgnoreCase(macStr)) {
                        BluetoothGatt gatt = result.getDevice().connectGatt(MainActivity.this, false, gattCallback);
                        boolean isGatt = gatt.discoverServices();
                        System.out.println("isGatt = " + isGatt);
                        System.out.println("MAC Address = " + result.getDevice().getAddress());
                        isScanning = false;
                        scanner.stopScan(handleScanCallback);
                        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

                        for (BluetoothDevice d : devices) {
                            System.out.println("hello  = " + d.getAddress());
                        }

                    } else {
                        System.out.println("not match");
                    }
                } else {
                    System.out.println("mac byte null");
                }
            } else {
                System.out.println("sparse array null");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            setScreenText("Scan failed.");
            isScanning = false;
            System.out.println("onScanFailed  " + errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            isScanning = false;
            System.out.println("onBatchScanResults");
            setScreenText("Scan failed.");
        }
    }

    private ScanCallback stopCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            System.out.println("stopCallback = " + callbackType);
        }


        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            System.out.println("stopCallback onBatchScanResults= ");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            System.out.println("stopCallback onScanFailed= " + errorCode);
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            System.out.println("onConnectionStateChange   " + status + "       " + newState);


            if (newState == BluetoothProfile.STATE_CONNECTED) {
                System.out.println("STATE_CONNECTED");

                List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

                for (BluetoothDevice d : devices) {
                    System.out.println("hello  = " + d.getAddress());
                }

                MainActivity.this.gatt = gatt;

                boolean isService = gatt.discoverServices();
                setScreenText("discoverServices");

                List<BluetoothDevice> devicesk = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

                for (BluetoothDevice d : devicesk) {
                    System.out.println("hello  = " + d.getAddress());
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                System.out.println("STATE_DISCONNECTED");

                List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

                for (BluetoothDevice d : devices) {
                    System.out.println("hello  = " + d.getAddress());
                }

                if (MainActivity.this.gatt != null) {
                    MainActivity.this.gatt.disconnect();
                    MainActivity.this.gatt.close();
                    MainActivity.this.gatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            System.out.println("onServicesDiscovered    ");
            getAccessToken(gatt);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            System.out.println("onCharacteristicRead " + characteristic.getUuid());

            readResponseFromDevice(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            System.out.println("onCharacteristicWrite ");

            if (characteristic.getUuid().toString().equals(writableUUID.toString())) {
                BluetoothGattService service = gatt.getService(serviceUUID);
                BluetoothGattCharacteristic ch = service.getCharacteristic(readableUUID);
                boolean isChRead = gatt.readCharacteristic(ch);
                gatt.readCharacteristic(characteristic);
                System.out.println("isChRead = " + isChRead);
            }

        }

        public void printService(BluetoothGatt gatt) {
            if (gatt != null) {
                List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService sr : services) {
                    System.out.println("service uuid " + sr.getUuid());
                    List<BluetoothGattCharacteristic> ch = sr.getCharacteristics();

                    for (BluetoothGattCharacteristic c : ch) {
                        System.out.println("ch uuid  " + c.getUuid());

                        List<BluetoothGattDescriptor> lds = c.getDescriptors();

                        for (BluetoothGattDescriptor d : lds) {
                            System.out.println("d  uuid = " + d.getUuid());

                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            System.out.println("onCharacteristiconCharacteristicChangedChanged");

            readResponseFromDevice(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            System.out.println("onDescriptorRead  " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            System.out.println("onDescriptorWrite  " + status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    private void readResponseFromDevice(BluetoothGattCharacteristic characteristic) {
        byte b[] = characteristic.getValue();
        if (b != null && b.length > 0) {
            String str = getHex(decrypt(b, key1));
            System.out.println("read = " + str);
            if (str.startsWith(RESPONSE_PREFIX_ACCESS_TOKEN)) {
                String frame[] = str.split("  ");
                byte b1 = hexStringToByteArray(frame[3])[0];
                byte b2 = hexStringToByteArray(frame[4])[0];
                byte b3 = hexStringToByteArray(frame[5])[0];
                byte b4 = hexStringToByteArray(frame[6])[0];

                if (accessToken == null) {
                    accessToken = new byte[4];
                }

                accessToken[0] = b1;
                accessToken[1] = b2;
                accessToken[2] = b3;
                accessToken[3] = b4;

                setScreenText("Connected Successfully.");
                unlockDevice(gatt);

            } else if (str.startsWith(RESPONSE_PREFIX_UNLOCK)) {
                String frame[] = str.split("  ");

                if (frame != null && frame.length > 4) {
                    if ("00".equalsIgnoreCase(frame[3])) {
                        setScreenText("Unlocked");
                    } else {
                        setScreenText("Response Unlock Failure");
                    }
                } else {

                    setScreenText("Unlock Failure");
                }
            } else if (str.startsWith(RESPONSE_PREFIX_LOCK_STATUS)) {
                String frame[] = str.split("  ");

                if (frame != null && frame.length > 4) {
                    if ("00".equalsIgnoreCase(frame[3])) {
                        setScreenText("Unlocked");
                    } else {
                        setScreenText("Locked");
                    }
                } else {
                    setScreenText("Lock Status Failure");
                }
            } else if (str.startsWith(RESPONSE_PREFIX_DEVICE_INFO)) {

                System.out.println("Response device info = " + str);
                setScreenText(str);

            }
        } else {
            setScreenText("Not Connected.");
            System.out.println("onCharacteristicRead byte array ia null");
            disconnectGatt();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE_BLUETOOTH == requestCode) {
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
//            scanLeDevice(true);
            validateLocation();
        } else if (REQUEST_CODE_LOCATION_COURSE == requestCode) {

            scanLeDevice(true);
        }
    }


    @Override
    public void onClick(View v) {
        System.out.println("onClick");
        switch (v.getId()) {
            case R.id.butUnlock:
                if (gatt != null && accessToken != null) {
                    unlockDevice(gatt);
                } else {
                    tvScreen.setText("First Connect");
                }
                break;

            case R.id.butConnect:
                if (!isScanning) {
                    String str = etMacAddress.getText().toString();

                    tvScreen.setText("Connecting...");
                    if (str != null && str.length() > 0) {
                        str = str.replaceAll(":", "  ");
                        ACTIONABLE_MAC_ADDRESS = str;

                        System.out.println("ACTIONABLE_MAC_ADDRESS = " + ACTIONABLE_MAC_ADDRESS);

                    }
                    scanLeDevice(true);
                } else {
                    Toast.makeText(MainActivity.this, "Already Scanning", Toast.LENGTH_LONG);
                }
                break;

            case R.id.butDissconnect:

                disconnectGatt();

                break;

            case R.id.butInfo:
                deviceInfo();
                break;

            case R.id.butLockStatus:
                lockStatus();
                break;

            case R.id.butNext:
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                break;


            default:
        }

    }

    private void deviceInfo() {
        Toast.makeText(this, "Hale support ni krda. firmware update krna pena.", Toast.LENGTH_LONG);
        tvScreen.setText("Kake Arram kr.");

        String lockStatusStr[] = {"05", "10", "01", "01"};

        byte b1 = hexStringToByteArray(lockStatusStr[0])[0];
        byte b2 = hexStringToByteArray(lockStatusStr[1])[0];
        byte b3 = hexStringToByteArray(lockStatusStr[2])[0];
        byte b4 = hexStringToByteArray(lockStatusStr[3])[0];

        byte lockStatus[] = {b1, b2, b3, b4, accessToken[0], accessToken[1], accessToken[2], accessToken[3], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        System.out.println("device info hex " + getHex(lockStatus));
        BluetoothGattCharacteristic ch = gatt.getService(serviceUUID).getCharacteristic(writableUUID);
        ch.setValue(encrypt(lockStatus, key1));
        gatt.writeCharacteristic(ch);
        System.out.println("Device Info");
    }

    private void lockStatus() {
        if (gatt != null && accessToken != null) {
            deviceStatus();
        } else {
            tvScreen.setText("First connect.");
        }
    }

    private void disconnectGatt() {
        if (gatt != null) {

            System.out.println("disconnectGatt before");
            List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

            for (BluetoothDevice d : devices) {
                System.out.println("hello  = " + d.getAddress());
            }

            gatt.disconnect();
            ;
            gatt.close();
//            gatt.disconnect();

            tvScreen.setText("Disconnected.");
            gatt = null;

            System.out.println("disconnectGatt After");
            devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

            for (BluetoothDevice d : devices) {
                System.out.println("hello  = " + d.getAddress());
            }
        } else {
            tvScreen.setText("Not Connected.");
        }
    }

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    public byte[] decrypt(byte[] src, byte[] key) {
        try {
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypt = cipher.doFinal(src);
            return decrypt;
        } catch (Exception ex) {
            System.out.println("Dec exception " + ex.getCause());
            ex.printStackTrace();
            return null;
        }
    }

    public byte[] encrypt(byte[] src, byte[] key) {
        try {
            SecretKey secretKey = new SecretKeySpec(key, "AES");
//            Provider[] providers = Security.getProviders();
//            for (Provider p : providers) {
//                System.out.println("Provider: " + p);
//            }
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//("AES/ECS/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] decrypt = cipher.doFinal(src);
            return decrypt;
        } catch (Exception ex) {
            System.out.println("enc   exception " + ex.getCause());
            ex.printStackTrace();
            return null;
        }
    }


    public String convertByteToString(byte[] by) {
        try {
            if (by != null && by.length > 0) {
                String roundTrip = new String(by, "UTF8");
                return roundTrip;
            } else {
                System.out.println("byte array is null");
            }
        } catch (Exception ex) {
            System.out.println("exception aya = " + ex.getCause());
        }

        return "";
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public String getHex(byte b[]) {
        StringBuffer result = new StringBuffer();
        for (byte bk : b) {
            result.append(String.format("%02X ", bk));
            result.append(" "); // delimiter
        }
        System.out.println("result = " + result.toString());

        return result.toString();
    }

    public String getHex(int b[]) {
        StringBuffer result = new StringBuffer();
        for (int bk : b) {
            result.append(String.format("%02X ", bk));
            result.append(" "); // delimiter
        }
        System.out.println("result = " + result.toString());

        return result.toString();
    }

    private void getAccessToken(BluetoothGatt gatt) {
        byte get[] = {06, 01, 01, 01, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00};

        BluetoothGattCharacteristic ch = gatt.getService(serviceUUID).getCharacteristic(writableUUID);
        BluetoothGattCharacteristic readableCharacterstic = gatt.getService(serviceUUID).getCharacteristic(readableUUID);
        gatt.setCharacteristicNotification(readableCharacterstic, true);
        ch.setValue(encrypt(msg1, key1));
        boolean isWrite = gatt.writeCharacteristic(ch);
        System.out.println("isWrite For AccessToken = " + isWrite);
    }

    private void unlockDevice(BluetoothGatt gatt) {
        byte unlock[] = {0x05, 0x01, 0x06, 0x30, 0x30, 0x30, 0X30, 0x30, 0x30, accessToken[0], accessToken[1], accessToken[2], accessToken[3], 0x00, 0x00, 0x00};
        System.out.println("unlock hex " + getHex(unlock));
        BluetoothGattCharacteristic ch = gatt.getService(serviceUUID).getCharacteristic(writableUUID);
        ch.setValue(encrypt(unlock, key1));
        gatt.writeCharacteristic(ch);
        System.out.println("write unlock");
    }

    private void deviceStatus() {
        String lockStatusStr[] = {"05", "0E", "01", "01"};

        byte b1 = hexStringToByteArray(lockStatusStr[0])[0];
        byte b2 = hexStringToByteArray(lockStatusStr[1])[0];
        byte b3 = hexStringToByteArray(lockStatusStr[2])[0];
        byte b4 = hexStringToByteArray(lockStatusStr[3])[0];

        byte lockStatus[] = {b1, b2, b3, b4, accessToken[0], accessToken[1], accessToken[2], accessToken[3], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        System.out.println("lock Status hex " + getHex(lockStatus));
        BluetoothGattCharacteristic ch = gatt.getService(serviceUUID).getCharacteristic(writableUUID);
        ch.setValue(encrypt(lockStatus, key1));
        gatt.writeCharacteristic(ch);
        System.out.println("write lockStatus");
    }


//    private void getyyy(BluetoothGatt gatt) {
//
//        for (BluetoothGattService service : gatt.getServices()) {
//            for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
//                byte bt[] = ch.getValue();
//
//                if (bt != null && bt.length > 0) {
////                        String str = getHex(decrypt(bt, key.getBytes()));
//                    String str = getHex(decrypt(bt, key1));
//
////                        String str1 = convertByteToString(decrypt(bt, hexStringToByteArray(key)));
////                        String str2 = convertByteToString(decrypt(bt, key.getBytes()));
////                        String str3 = "str3 = " + convertByteToString(bt);
//
//
////                        System.out.println("onCharacteristicRead   " + ch.getUuid() + "      " + str + "       " + str1 + "          " + str2);
//                    System.out.println("onCharacteristicRead = " + ch.getUuid() + "          " + str + "      len = " + bt.length + "     " + convertByteToString(bt));
//
//
//                    if (str.startsWith("06  02  07") && k == 0) {
//
//                        k++;
//                        //if (ch.getUuid().equals("000036f6-0000-1000-8000-00805f9b34fb"))
//                        {
//                            System.out.println("char = " + str.charAt(9) + "   " + str.charAt(12) + "    " + str.charAt(15) + "      " + str.charAt(18));
//
//                            String jk[] = str.split("  ");
//                            byte b1 = hexStringToByteArray(jk[3])[0];
//                            byte b2 = hexStringToByteArray(jk[4])[0];
//                            byte b3 = hexStringToByteArray(jk[5])[0];
//                            byte b4 = hexStringToByteArray(jk[6])[0];
//                            System.out.println("byte = " + b1 + "   " + b2 + "    " + b3 + "      " + b4);
//
//                            byte unlock[] = {0x05, 0x01, 0x06, 0x30, 0x30, 0x30, 0X30, 0x30, 0x30, b1, b2, b3, b4, 0x00, 0x00, 0x00};
//
////                                byte unlock[] = {0x05, 0x01, 0x06, 0x30, 0x30, 0x30, 0X30, 0x30, 0x30, b1, b2,b3, b4,0x00, 0x00, 0x00};
//                            String lockStatusStr[] = {"05", "0E", "01", "01"};
////
//                            byte bb1 = hexStringToByteArray(lockStatusStr[0])[0];
//                            byte bb2 = hexStringToByteArray(lockStatusStr[1])[0];
//                            byte bb3 = hexStringToByteArray(lockStatusStr[2])[0];
//                            byte bb4 = hexStringToByteArray(lockStatusStr[3])[0];
//                            System.out.println("byte bb = " + bb1 + "   " + bb2 + "    " + bb3 + "      " + bb4);
//
//
//                            String statusStr[] = {"05", "10", "01", "01"};
//
//
//                            byte bbb1 = hexStringToByteArray(statusStr[0])[0];
//                            byte bbb2 = hexStringToByteArray(statusStr[1])[0];
//                            byte bbb3 = hexStringToByteArray(statusStr[2])[0];
//                            byte bbb4 = hexStringToByteArray(statusStr[3])[0];
//
//                            byte lockStatus[] = {bbb1, bbb2, bbb3, bbb4, b1, b2, b3, b4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
//
//
//                            System.out.println("unlock hex " + getHex(unlock));
//                            BluetoothGattCharacteristic chWrite = service.getCharacteristic(UUID.fromString("000036f5-0000-1000-8000-00805f9b34fb"));
//                            chWrite.setValue(encrypt(unlock, key1));
//                            gatt.writeCharacteristic(chWrite);
//                            System.out.println("write unlock");
//                        }
////                            else
////                            {
////                                System.out.println("not uuid");
////                            }
//                    } else {
//                        System.out.println("not clear");
//                    }
//
//                } else {
//                    System.out.println("onCharacteristicRead   " + ch.getUuid() + "      ch is null");
//                }
//            }
//        }
//    }

    private void setScreenText(String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvScreen.setText(msg);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        disconnectGatt();
    }
}
