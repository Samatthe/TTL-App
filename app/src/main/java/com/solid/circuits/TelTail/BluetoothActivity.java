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
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;


public class BluetoothActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "TTLPrefsFile";

    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;
    public boolean autoConnect = false;
    public boolean onStartup = false;

    CheckBox bleAuto;
    CheckBox bleConnect;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding BLE", Toast.LENGTH_SHORT).show();
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
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
        setContentView(R.layout.activity_bluetooth_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        bleAuto = (CheckBox) findViewById(R.id.checkbox_auto);
        bleConnect = (CheckBox) findViewById(R.id.checkbox_on_start);

        loadPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();

        savePreferences();
    }

    @Override
    protected void onStop(){
        super.onStop();

        savePreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        savePreferences();

        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothService = null;
    }

    private void savePreferences(){
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("bleAuto", bleAuto.isChecked());
        editor.putBoolean("bleConnect", bleConnect.isChecked());
        // Commit the edits!
        editor.commit();
    }

    private void loadPreferences(){
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        autoConnect = settings.getBoolean("bleAuto", false);
        onStartup = settings.getBoolean("bleConnect", false);
        bleAuto.setChecked(autoConnect);
        bleConnect.setChecked(onStartup);
    }

    public void onButtonClick(View view){
        Intent intent;
        switch(view.getId()){
            case R.id.device_select_button:
                intent = new Intent(BluetoothActivity.this, BluetoothScan.class);
                startActivity(intent);
                break;

            case R.id.connect_button:
                if(mBluetoothService.mBluetoothDeviceAddress != null) {
                    if(mBluetoothService.mConnectionState == 0) {
                        mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                    } else {
                        mBluetoothService.disconnect();
                    }
                }
                else {
                    Toast.makeText(BluetoothActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.raw_button:
                intent = new Intent(BluetoothActivity.this, RawDataActivity.class);
                startActivity(intent);
                break;
        }
    }
}
