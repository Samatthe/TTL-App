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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class SensorSettingsActivity extends AppCompatActivity
        implements  SeekBar.OnSeekBarChangeListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    public boolean autoConnect = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;

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
                        (byte) 0x0AC,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(SensorSettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(SensorSettingsActivity.this, "Reading sensor configuration", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_sensor_settins);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SeekBar light_sensitivity_seeker = (SeekBar) findViewById(R.id.light_sensitivity_seeker);
        SeekBar accel_x_sensitivity_seeker = (SeekBar) findViewById(R.id.accel_x_sensitivity_seeker);
        SeekBar accel_y_sensitivity_seeker = (SeekBar) findViewById(R.id.accel_y_sensitivity_seeker);
        SeekBar accel_z_sensitivity_seeker = (SeekBar) findViewById(R.id.accel_z_sensitivity_seeker);
        SeekBar gyro_x_sensitivity_seeker = (SeekBar) findViewById(R.id.gyro_x_sensitivity_seeker);
        SeekBar gyro_y_sensitivity_seeker = (SeekBar) findViewById(R.id.gyro_y_sensitivity_seeker);
        SeekBar gyro_z_sensitivity_seeker = (SeekBar) findViewById(R.id.gyro_z_sensitivity_seeker);

        light_sensitivity_seeker.setOnSeekBarChangeListener(this);
        accel_x_sensitivity_seeker.setOnSeekBarChangeListener(this);
        accel_y_sensitivity_seeker.setOnSeekBarChangeListener(this);
        accel_z_sensitivity_seeker.setOnSeekBarChangeListener(this);
        gyro_x_sensitivity_seeker.setOnSeekBarChangeListener(this);
        gyro_y_sensitivity_seeker.setOnSeekBarChangeListener(this);
        gyro_z_sensitivity_seeker.setOnSeekBarChangeListener(this);

        light_sensitivity_seeker.setEnabled(false);
        accel_x_sensitivity_seeker.setEnabled(false);
        accel_y_sensitivity_seeker.setEnabled(false);
        accel_z_sensitivity_seeker.setEnabled(false);
        gyro_x_sensitivity_seeker.setEnabled(false);
        gyro_y_sensitivity_seeker.setEnabled(false);
        gyro_z_sensitivity_seeker.setEnabled(false);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    public void onButtonClick(View view){
        switch(view.getId()) {
            case R.id.sensor_settings_read_button:
                final byte[] txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0AC,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(SensorSettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
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
                EditText vLightError = (EditText)findViewById(R.id.light_kalman_error_edit);
                SeekBar vLightSeek = (SeekBar)findViewById(R.id.light_sensitivity_seeker);
                EditText vAxError = (EditText)findViewById(R.id.accel_x_kalman_error_edit);
                SeekBar vAxSeek = (SeekBar)findViewById(R.id.accel_x_sensitivity_seeker);
                EditText vAyError = (EditText)findViewById(R.id.accel_y_kalman_error_edit);
                SeekBar vAySeek = (SeekBar)findViewById(R.id.accel_y_sensitivity_seeker);
                EditText vAzError = (EditText)findViewById(R.id.accel_z_kalman_error_edit);
                SeekBar vAzSeek = (SeekBar)findViewById(R.id.accel_z_sensitivity_seeker);
                EditText vGxError = (EditText)findViewById(R.id.gyro_x_kalman_error_edit);
                SeekBar vGxSeek = (SeekBar)findViewById(R.id.gyro_x_sensitivity_seeker);
                EditText vGyError = (EditText)findViewById(R.id.gyro_y_kalman_error_edit);
                SeekBar vGySeek = (SeekBar)findViewById(R.id.gyro_y_sensitivity_seeker);
                EditText vGzError = (EditText)findViewById(R.id.gyro_z_kalman_error_edit);
                SeekBar vGzSeek = (SeekBar)findViewById(R.id.gyro_z_sensitivity_seeker);

                int temp;
                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                //Log.d(TAG, bytesToHex(data));
                if(data.length == 14) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x61:
                                if(i+1 >= data.length)
                                    break;
                                Toast.makeText(SensorSettingsActivity.this, "AX"+String.valueOf(data[i + 1]), Toast.LENGTH_SHORT).show();
                                vAxError.setText(""+String.valueOf(data[i + 1] & 0xFF));
                                i++;
                                break;
                            case 0x62:
                                if(i+1 >= data.length)
                                    break;
                                Toast.makeText(SensorSettingsActivity.this, "AY"+String.valueOf(data[i + 1]), Toast.LENGTH_SHORT).show();
                                vAyError.setText(""+String.valueOf(data[i + 1] & 0xFF));
                                i++;
                                break;
                            case 0x63:
                                if(i+1 >= data.length)
                                    break;
                                Toast.makeText(SensorSettingsActivity.this, "AZ"+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                vAzError.setText(""+String.valueOf(data[i + 1] & 0xFF));
                                i++;
                                break;
                            case 0x64:
                                if(i+1 >= data.length)
                                    break;
                                Toast.makeText(SensorSettingsActivity.this, "GX"+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                vGxError.setText(""+String.valueOf(data[i + 1] & 0xFF));
                                i++;
                                break;
                            case 0x65:
                                if(i+1 >= data.length)
                                    break;
                                Toast.makeText(SensorSettingsActivity.this, "GY"+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                vGyError.setText(""+String.valueOf(data[i + 1] & 0xFF));
                                i++;
                                break;
                            case 0x66:
                                if(i+1 >= data.length)
                                    break;
                                Toast.makeText(SensorSettingsActivity.this, "GZ"+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                vGzError.setText(""+String.valueOf(data[i + 1] & 0xFF));
                                i++;
                                break;
                            case 0x67:
                                if(i+1 >= data.length)
                                    break;
                                Toast.makeText(SensorSettingsActivity.this, "Light"+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                vLightError.setText(""+String.valueOf(data[i + 1] & 0xFF));
                                i++;
                                break;
                            case 0x68:
                                if(i+1 >= data.length)
                                    break;
                                temp = (data[i + 1] & 0xFF);
                                vAxSeek.setProgress(temp);
                                i++;
                                break;
                            case 0x69:
                                if(i+1 >= data.length)
                                    break;
                                temp = (data[i + 1] & 0xFF);
                                vAySeek.setProgress(temp);
                                i++;
                                break;
                            case 0x6A:
                                if(i+1 >= data.length)
                                    break;
                                temp = (data[i + 1] & 0xFF);
                                vAzSeek.setProgress(temp);
                                i++;
                                break;
                            case 0x6B:
                                if(i+1 >= data.length)
                                    break;
                                temp = (data[i + 1] & 0xFF);
                                vGxSeek.setProgress(temp);
                                i++;
                                break;
                            case 0x6C:
                                if(i+1 >= data.length)
                                    break;
                                temp = (data[i + 1] & 0xFF);
                                vGySeek.setProgress(temp);
                                i++;
                                break;
                            case 0x6D:
                                if(i+1 >= data.length)
                                    break;
                                temp = (data[i + 1] & 0xFF);
                                vGzSeek.setProgress(temp);
                                i++;
                                break;
                            case 0x6E:
                                if(i+1 >= data.length)
                                    break;
                                temp = (data[i + 1] & 0xFF);
                                vLightSeek.setProgress(temp);
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView light_sensitivity_number = (TextView) findViewById(R.id.light_sensitivity_number);
        TextView accel_x_sensitivity_number = (TextView) findViewById(R.id.accel_x_sensitivity_number);
        TextView accel_y_sensitivity_number = (TextView) findViewById(R.id.accel_y_sensitivity_number);
        TextView accel_z_sensitivity_number = (TextView) findViewById(R.id.accel_z_sensitivity_number);
        TextView gyro_x_sensitivity_number = (TextView) findViewById(R.id.gyro_x_sensitivity_number);
        TextView gyro_y_sensitivity_number = (TextView) findViewById(R.id.gyro_y_sensitivity_number);
        TextView gyro_z_sensitivity_number = (TextView) findViewById(R.id.gyro_z_sensitivity_number);

        if(progress <= 0) {
            seekBar.setProgress(1);
            progress = 1;
        }

        if(seekBar.getId() == R.id.light_sensitivity_seeker){
            light_sensitivity_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.accel_x_sensitivity_seeker){
            accel_x_sensitivity_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.accel_y_sensitivity_seeker){
            accel_y_sensitivity_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.accel_z_sensitivity_seeker){
            accel_z_sensitivity_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.gyro_x_sensitivity_seeker){
            gyro_x_sensitivity_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.gyro_y_sensitivity_seeker){
            gyro_y_sensitivity_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.gyro_z_sensitivity_seeker){
            gyro_z_sensitivity_number.setText("" + progress);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sensor_config_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        SeekBar vLightSeek = (SeekBar)findViewById(R.id.light_sensitivity_seeker);
        SeekBar vAxSeek = (SeekBar)findViewById(R.id.accel_x_sensitivity_seeker);
        SeekBar vAySeek = (SeekBar)findViewById(R.id.accel_y_sensitivity_seeker);
        SeekBar vAzSeek = (SeekBar)findViewById(R.id.accel_z_sensitivity_seeker);
        SeekBar vGxSeek = (SeekBar)findViewById(R.id.gyro_x_sensitivity_seeker);
        SeekBar vGySeek = (SeekBar)findViewById(R.id.gyro_y_sensitivity_seeker);
        SeekBar vGzSeek = (SeekBar)findViewById(R.id.gyro_z_sensitivity_seeker);

        int id = item.getItemId();
        if (id == R.id.action_sensors_connect) {
            mBluetoothService.connectTTL();
            return true;
        } else if (id == R.id.action_sensor_editable) {
            if(item.isChecked()) {
                item.setChecked(false);
                vLightSeek.setEnabled(false);
                vAxSeek.setEnabled(false);
                vAySeek.setEnabled(false);
                vAzSeek.setEnabled(false);
                vGxSeek.setEnabled(false);
                vGySeek.setEnabled(false);
                vGzSeek.setEnabled(false);
            } else {
                item.setChecked(true);
                vLightSeek.setEnabled(true);
                vAxSeek.setEnabled(true);
                vAySeek.setEnabled(true);
                vAzSeek.setEnabled(true);
                vGxSeek.setEnabled(true);
                vGySeek.setEnabled(true);
                vGzSeek.setEnabled(true);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
