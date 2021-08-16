/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solid.circuits.TelTail;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothService extends Service {
    private final static String TAG = BluetoothService.class.getSimpleName();

    public static final String PREFS_NAME = "TTLPrefsFile";

    static boolean SEND_OK = true;
    static long last_send = 0;
    static long send_time = 0;
    static boolean FirstSend = true;
    byte[] last_sent_data;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    public String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;
    public int mConnectionState = STATE_DISCONNECTED;

    private boolean First_Init = true;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.solid.circuits.TelTail.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.solid.circuits.TelTail.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.solid.circuits.TelTail.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.solid.circuits.TelTail.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.solid.circuits.TelTail.EXTRA_DATA";

    public final static UUID UUID_NOTIFY =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_SERVICE =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    public BluetoothGattCharacteristic mNotifyCharacteristic;

    PendingIntent ContentPendingIntent;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            //Toast.makeText(getA.this, gatt.toString()+" "+Integer.toString(status), Toast.LENGTH_SHORT).show();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;

                broadcastUpdate(intentAction);
                //Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                //Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                //Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
                close();
                bluetoothGattCharacteristicHM_10 = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.w(TAG, "onServicesDiscovered received: " + status);
                findService(gatt.getServices());
            } else if(mBluetoothGatt.getDevice().getUuids() == null) {
                //Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                //Log.i(TAG, "Char Read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //Log.i(TAG, "OnCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                          int status)
        {
            //Log.i(TAG, "OnCharacteristicWrite");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor bd,
                                     int status) {
            //Log.e(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor bd,
                                      int status) {
            //Log.e(TAG, "onDescriptorWrite");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int a, int b)
        {
            //Log.e(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int a)
        {
            //Log.e(TAG, "onReliableWriteCompleted");
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        //Log.w(TAG, "broadcastUpdate()");

        final byte[] data = characteristic.getValue();

        //Log.v(TAG, "data.length: " + data.length);

        if (data != null && data.length > 0) {
            if((data[0]&0xFF)==0x91 && (data[1]&0xFF)==0x91){
                writeBytesFast(last_sent_data);
            }
                intent.putExtra(EXTRA_DATA, data);
        }

        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        if (!initialize()) {
            Toast.makeText(BluetoothService.this, "Unable to initialize Bluetooth", Toast.LENGTH_LONG).show();
            //Log.e(TAG, "Unable to initialize Bluetooth");
        }

        Intent main_activity_intent = new Intent(this, MainActivity.class);
        ContentPendingIntent = PendingIntent.getActivity(this, 0, main_activity_intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        savePreferences();

        //Toast.makeText(BluetoothService.this, "Unbinding BLE", Toast.LENGTH_SHORT).show();

        return super.onUnbind(intent);
    }

    private void savePreferences(){
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("DeviceAddress", mBluetoothDeviceAddress);
        // Commit the edits!
        editor.commit();
        //Toast.makeText(BluetoothService.this, "Saving BLE settings", Toast.LENGTH_SHORT).show();
    }

    private void loadPreferences(){
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mBluetoothDeviceAddress = settings.getString("DeviceAddress", null);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if(First_Init) {
            loadPreferences();

            First_Init = false;
            // For API level 18 and above, get a reference to BluetoothAdapter through
            // BluetoothManager.
            if (mBluetoothManager == null) {
                mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (mBluetoothManager == null) {
                    //Log.e(TAG, "Unable to initialize BluetoothManager.");
                    return false;
                }
            }

            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                //Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }

            //Toast.makeText(BluetoothService.this, "Initializing BLE", Toast.LENGTH_SHORT).show();
        }//else{
         //   return false;
        //}

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        Toast.makeText(BluetoothService.this, "Connecting", Toast.LENGTH_SHORT).show();

        if (mBluetoothAdapter == null || address == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            //Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        //Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        Toast.makeText(BluetoothService.this, "Disconnecting", Toast.LENGTH_SHORT).show();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            //Log.i(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        //Log.i(TAG, characteristic.toString());
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void WriteValue(String strValue)
    {
        mNotifyCharacteristic.setValue(strValue.getBytes());
        mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
    }

    public void findService(List<BluetoothGattService> gattServices)
    {
        //Log.i(TAG, "Count is:" + gattServices.size());
        for (BluetoothGattService gattService : gattServices)
        {
            //Log.i(TAG, gattService.getUuid().toString());
            //Log.i(TAG, UUID_SERVICE.toString());
            if(gattService.getUuid().toString().equalsIgnoreCase(UUID_SERVICE.toString()))
            {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                //Log.i(TAG, "Count is:" + gattCharacteristics.size());
                for (BluetoothGattCharacteristic gattCharacteristic :
                        gattCharacteristics)
                {
                    if(gattCharacteristic.getUuid().toString().equalsIgnoreCase(UUID_NOTIFY.toString()))
                    {
                        //Toast.makeText(BluetoothService.this, "services", Toast.LENGTH_SHORT).show();
                        //Log.i(TAG, gattCharacteristic.getUuid().toString());
                        //Log.i(TAG, UUID_NOTIFY.toString());
                        mNotifyCharacteristic = gattCharacteristic;
                        setCharacteristicNotification(gattCharacteristic, true);
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                        setupHM10Characteristic(getSupportedGattServices());  ////////
                        return;
                    }
                }
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void setupHM10Characteristic(List<BluetoothGattService> gattServices) {

        UUID UUID_HM_10 = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

        if (gattServices == null) return;

        String uuid;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();

                //Check if it is "HM_10"
                if (uuid.equals("0000ffe1-0000-1000-8000-00805f9b34fb")) {
                    //Log.i(TAG, "HM-11 ----------");
                    //Toast.makeText(BluetoothService.this, "HM-11", Toast.LENGTH_SHORT).show();
                    bluetoothGattCharacteristicHM_10 = gattService.getCharacteristic(UUID_HM_10);
                }
            }
        }
    }

    //byte[] temp = new byte[1];
    public boolean writeBytes(byte[] data){
        send_time = System.nanoTime()/1000000;
        if(last_send > send_time)
            last_send = 0;
        if(bluetoothGattCharacteristicHM_10 != null && ((send_time - last_send > 50) || FirstSend/*|| SEND_OK*/)) {
            writeBytes_helper(data);
            last_send = send_time;
            FirstSend = false;
            return true;
        }
        return false;
    }

    public boolean writeBytesFast(byte[] data){
        send_time = System.nanoTime()/1000000;
        if(last_send > send_time)
            last_send = 0;
        if(bluetoothGattCharacteristicHM_10 != null && (send_time - last_send > 15)) {
            writeBytes_helper(data);
            last_send = send_time;
            return true;
        }
        return false;
    }

    private void writeBytes_helper(byte[] data){
            //Log.i(TAG, "SENT STUFF");
            //Toast.makeText(BluetoothService.this, (mBluetoothManager==null)+""+(bluetoothGattCharacteristicHM_10==null)+" "+(mBluetoothGatt==null)+" "+(mBluetoothAdapter==null), Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, data.toString(), Toast.LENGTH_SHORT).show();
            last_sent_data = data;
            bluetoothGattCharacteristicHM_10.setValue(data);
            writeCharacteristic(bluetoothGattCharacteristicHM_10);
            setCharacteristicNotification(bluetoothGattCharacteristicHM_10, true);
    }

    int mFaultNotifId = 1;
    public void createFaultNotification(String fault){
        NotificationCompat.Builder mFaultNotif = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFF484848)
                .setPriority(Notification.PRIORITY_MAX);

        mFaultNotif.setContentTitle("TelTail Fault Detected");
        mFaultNotif.setContentText(fault);

        mFaultNotif.setContentIntent(ContentPendingIntent);

        Drawable myDrawable = getResources().getDrawable(R.mipmap.ic_ttl_normal);
        Bitmap LargeIcon = ((BitmapDrawable) myDrawable).getBitmap();
        mFaultNotif.setLargeIcon(LargeIcon);
        mFaultNotif.setShowWhen(false);

        startForeground(mFaultNotifId, mFaultNotif.build());
        mFaultNotifId++;
    }
}