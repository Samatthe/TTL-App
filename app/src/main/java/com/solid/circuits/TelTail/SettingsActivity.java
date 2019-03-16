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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;


public class SettingsActivity extends AppCompatActivity{

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "MyPrefsFile";

    boolean READ_CURRENT = false;

    private BluetoothService mBluetoothService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        setContentView(R.layout.activity_settings);

        restoresettings();
    }

    @Override
    protected void onResume(){
        super.onResume();
        restoresettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savesettings();
    }

    @Override
    protected void onStop(){
        super.onStop();
        savesettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        savesettings();
    }

    void savesettings(){

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        CheckBox read_current = (CheckBox) findViewById(R.id.settings_read_current_check);
        editor.putBoolean("ReadCurrent", read_current.isChecked());

        // Commit the edits!
        editor.commit();
    }

    void restoresettings(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        READ_CURRENT = settings.getBoolean("ReadCurrent", false);
        CheckBox read_current = (CheckBox) findViewById(R.id.settings_read_current_check);
        read_current.setChecked(READ_CURRENT);
    }

    public void onButtonClick(View view){
        Intent intent;
        switch(view.getId()) {
            case R.id.conf_bluetooth_button:
                intent = new Intent(this, BluetoothActivity.class);
                startActivity(intent);
                break;
            case R.id.conf_logging_button:
                intent = new Intent(this, LoggingActivity.class);
                startActivity(intent);
                break;
            case R.id.motor_info_button:
                intent = new Intent(this, MotorInfoActivity.class);
                startActivity(intent);
                break;
            case R.id.sensor_settins_button:
                intent = new Intent(this, SensorSettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.calibrate_button:
                //Toast.makeText(mBluetoothService, "hello", Toast.LENGTH_SHORT).show();

                new AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to calibrate the IMU?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final byte txbuf[] = new byte[] {
                                        (byte) 0x0AD,
                                        (byte) 0x0AE
                                };
                                if(!mBluetoothService.writeBytes(txbuf))
                                    Toast.makeText(SettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                //String txstring = bytesToHex(txbuf);
                // Toast.makeText(MainActivity.this, txstring, Toast.LENGTH_SHORT).show();
                break;
            case R.id.orientation_button:
                intent = new Intent(this, OrientationActivity.class);
                startActivity(intent);
                break;
            case R.id.controls_button:
                intent = new Intent(this, ControlsConfigActivity.class);
                startActivity(intent);
                break;
        }
    }

    public void onCheckboxClicked(View view) {
        CheckBox checkbox = (CheckBox) findViewById(view.getId());

        switch (view.getId()) {
            case R.id.settings_read_current_check:
                READ_CURRENT = checkbox.isChecked();
                break;
        }
    }
}
