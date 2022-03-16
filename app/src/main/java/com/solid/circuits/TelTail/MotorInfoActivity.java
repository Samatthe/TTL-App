/*
	Copyright 2019 Matthew Sauve	mattsauve@solidcircuits.net

	This file is part of the TTL android app.

	The TTL android app is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The TTL android app is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.solid.circuits.TelTail;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class MotorInfoActivity  extends AppCompatActivity {

    private final static String TAG = MotorInfoActivity.class.getSimpleName();

    public boolean autoConnect = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;

    // Code to manage Service lifecycle.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding BLE", Toast.LENGTH_SHORT).show();
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                // Automatically connects to the device upon successful start-up initialization
                if(autoConnect){
                    // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                    // BluetoothAdapter through BluetoothManager.
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = bluetoothManager.getAdapter();

                    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                    // fire an intent to display a dialog asking the user to grant permission to enable it.
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }

                    mBluetoothService.connectTTL();
                }

                final byte[] txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0DD,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MotorInfoActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MotorInfoActivity.this, "Reading motor configuration", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_info);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothService = null;

        unregisterReceiver(mGattUpdateReceiver);
    }

    public void onButtonClick(View view){
        switch(view.getId()) {
            case R.id.motor_info_read_button:
                final byte[] txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0DD,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MotorInfoActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                finish();
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                TextView vMotorMaxCurrent = (TextView)findViewById(R.id.limits_current_mmax_text);
                TextView vMotorMinCurrent = (TextView)findViewById(R.id.limits_current_mmin_text);
                TextView vBatteryMaxCurrent = (TextView)findViewById(R.id.limits_current_bmax_text);
                TextView vBatteryMinCurrent = (TextView)findViewById(R.id.limits_current_bmin_text);
                TextView vAbsMaxCurrent = (TextView)findViewById(R.id.limits_current_absmax_text);
                TextView vBatteryMaxVoltage = (TextView)findViewById(R.id.limits_voltage_inmax_text);
                TextView vBatteryMinVoltage = (TextView)findViewById(R.id.limits_voltage_inmin_text);
                TextView vBatteryCutStart = (TextView)findViewById(R.id.limits_voltage_cutstart_text);
                TextView vBatteryCutEnd = (TextView)findViewById(R.id.limits_voltage_cutend_text);
                TextView vRPMmax = (TextView)findViewById(R.id.limits_rpm_erpmmax_text);
                TextView vRPMmin = (TextView)findViewById(R.id.limits_rpm_erpmmin_text);
                TextView vRPMmaxFull = (TextView)findViewById(R.id.limits_rpm_erpmmax_fb_text);
                TextView vRPMmaxFullcc = (TextView)findViewById(R.id.limits_rpm_erpmmax_fbc_text);
                TextView vMosfetMax = (TextView)findViewById(R.id.limits_temp_fetstart_text);
                TextView vMosfetMin = (TextView)findViewById(R.id.limits_temp_fetend_text);
                TextView vMotorMax = (TextView)findViewById(R.id.limits_temp_mstart_text);
                TextView vMotorMin = (TextView)findViewById(R.id.limits_temp_mend_text);
                TextView vDutyMax = (TextView)findViewById(R.id.limits_duty_max_text);
                TextView vDutyMin = (TextView)findViewById(R.id.limits_duty_min_text);

                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                //Log.d(TAG, bytesToHex(data));
                if(data.length == 10 || data.length == 12) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x41:
                                if(i+1 >= data.length)
                                    break;
                                vMotorMaxCurrent.setText("Motor max:\t" + (data[i + 1] & 0xFF) + " A");
                                i++;
                                break;
                            case 0x42:
                                if(i+1 >= data.length)
                                    break;
                                vMotorMinCurrent.setText("Motor min:\t" + data[i + 1] + " A");
                                i++;
                                break;
                            case 0x43:
                                if(i+1 >= data.length)
                                    break;
                                vBatteryMaxCurrent.setText("Battery max:\t" + (data[i + 1] & 0xFF) + " A");
                                i++;
                                break;
                            case 0x44:
                                if(i+1 >= data.length)
                                    break;
                                vBatteryMinCurrent.setText("Battery min:\t" + data[i + 1] + " A");
                                i++;
                                break;
                            case 0x45:
                                if(i+1 >= data.length)
                                    break;
                                vAbsMaxCurrent.setText("Absolute max:\t" + (data[i + 1] & 0xFF) + " A");
                                i++;
                                break;
                            case 0x46:
                                if(i+1 >= data.length)
                                    break;
                                vBatteryMaxVoltage.setText("Maximum input voltage:\t" + (data[i + 1] & 0xFF) + " V");
                                i++;
                                break;
                            case 0x47:
                                if(i+1 >= data.length)
                                    break;
                                vBatteryMinVoltage.setText("Minimum input voltage:\t" + (data[i + 1] & 0xFF) + " V");
                                i++;
                                break;
                            case 0x48:
                                if(i+1 >= data.length)
                                    break;
                                vBatteryCutStart.setText("Battery cutoff start:\t" + (data[i + 1] & 0xFF) + " V");
                                i++;
                                break;
                            case 0x49:
                                if(i+1 >= data.length)
                                    break;
                                vBatteryCutEnd.setText("Battery cutoff end:\t" + (data[i + 1] & 0xFF) + " V");
                                i++;
                                break;
                            case 0x4A:
                                if(i+3 >= data.length)
                                    break;
                                vRPMmax.setText("Max ERPM:\t" + (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF)) + " rpm");
                                i+=3;
                                break;
                            case 0x4B:
                                if(i+3 >= data.length)
                                    break;
                                vRPMmin.setText("Min ERPM:\t" + (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF)) + " rpm");
                                i+=3;
                                break;
                            case 0x4C:
                                if(i+3 >= data.length)
                                    break;
                                vRPMmaxFull.setText("Max ERPM at full brake:\t" + (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF)) + " rpm");
                                i+=3;
                                break;
                            case 0x4D:
                                if(i+3 >= data.length)
                                    break;
                                vRPMmaxFullcc.setText("Max ERPM at full brake (cc mode):\t" + (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF)) + " rpm");
                                i+=3;
                                break;
                            case 0x4E:
                                if(i+1 >= data.length)
                                    break;
                                vMosfetMax.setText("MOSFET Start:\t" + data[i + 1] + " ℃");
                                i++;
                                break;
                            case 0x4F:
                                if(i+1 >= data.length)
                                    break;
                                vMosfetMin.setText("MOSFET End:\t" + data[i + 1] + " ℃");
                                i++;
                                break;
                            case 0x50:
                                if(i+1 >= data.length)
                                    break;
                                vMotorMax.setText("Motor Start:\t" + data[i + 1] + " ℃");
                                i++;
                                break;
                            case 0x51:
                                if(i+1 >= data.length)
                                    break;
                                vMotorMin.setText("Motor End:\t" + data[i + 1] + " ℃");
                                i++;
                                break;
                            case 0x52:
                                if(i+1 >= data.length)
                                    break;
                                vDutyMin.setText("Maximum duty cycle:\t" + data[i + 1] + "%");
                                i++;
                                break;
                            case 0x53:
                                if(i+1 >= data.length)
                                    break;
                                vDutyMax.setText("Minimum duty cycle:\t" + data[i + 1] + "%");
                                i++;
                                break;
                        }
                    }
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}

