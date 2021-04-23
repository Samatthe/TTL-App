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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends AppCompatActivity{

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "TTLPrefsFile";

    boolean READ_CURRENT = false;

    boolean DispNotifaction = true;

    int TTL_FW = 0;

    public LoggingService mLoggingService;
    private BluetoothService mBluetoothService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
                byte txbuf[] = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0F9,
                        (byte) 0x05A
                };
                mBluetoothService.writeBytes(txbuf);
                txbuf[2] = (byte) 0x0A3;
                long send_timer = System.currentTimeMillis();
                while(!mBluetoothService.writeBytes(txbuf)){if(System.currentTimeMillis()-send_timer > 250) break;}
            } else if (componentName.getClassName().equals(LoggingService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
                mLoggingService = ((LoggingService.LocalBinder) service).getService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            } else if (componentName.getClassName().equals(LoggingService.class.getName())) {
                mLoggingService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Intent LogServiceIntent = new Intent(this, LoggingService.class);
        bindService(LogServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        setContentView(R.layout.activity_settings);

        restoresettings();
    }

    @Override
    protected void onResume(){
        super.onResume();
        restoresettings();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
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
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        savesettings();
        unregisterReceiver(mGattUpdateReceiver);
    }

    void savesettings(){

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        CheckBox read_current = findViewById(R.id.settings_read_current_check);
        CheckBox disp_notif = findViewById(R.id.settings_disp_notif_check);
        editor.putBoolean("ReadCurrentLED", read_current.isChecked());
        editor.putBoolean("DispNotif", disp_notif.isChecked());

        // Commit the edits!
        editor.commit();
    }

    void restoresettings(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        READ_CURRENT = settings.getBoolean("ReadCurrentLED", false);
        DispNotifaction = settings.getBoolean("DispNotif", true);
        CheckBox read_current = findViewById(R.id.settings_read_current_check);
        CheckBox disp_notif = findViewById(R.id.settings_disp_notif_check);
        read_current.setChecked(READ_CURRENT);
        disp_notif.setChecked(DispNotifaction);
    }

    public void onButtonClick(View view){
        Intent intent;
        switch(view.getId()) {
            case R.id.run_wizard_button:
                intent = new Intent(this, SetupWizardActivity.class);
                startActivity(intent);
                break;
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
            /*case R.id.sensor_settins_button:
                intent = new Intent(this, SensorSettingsActivity.class);
                startActivity(intent);
                break;*/
            case R.id.calibrate_button:
                new AlertDialog.Builder(this)
                        .setMessage("To Calibrate the IMU:\n1. Place board on level ground\n2. Continue with Calibration\n3. Let board sit for > 5 seconds\n\nAre you sure you want to calibrate the IMU?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final byte txbuf[] = new byte[] {
                                        (byte) 0x0A5,
                                        (byte) 0x000,
                                        (byte) 0x0AD,
                                        (byte) 0x05A
                                };
                                if(!mBluetoothService.writeBytes(txbuf))
                                    Toast.makeText(SettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                break;
            case R.id.LED_test_button:
                new AlertDialog.Builder(this)
                        .setMessage("This test will turn on all LED and horn outputs. If a horn is connected, it will activate.")
                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final byte txbuf[] = new byte[] {
                                        (byte) 0x0A5,
                                        (byte) 0x000,
                                        (byte) 0x0FF,
                                        (byte) 0x05A
                                };
                                if(!mBluetoothService.writeBytes(txbuf))
                                    Toast.makeText(SettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setIcon(R.drawable.ic_warning)
                        .setTitle("Output Test Warning")
                        .show();
                break;
            case R.id.orientation_button:
                intent = new Intent(this, OrientationActivity.class);
                startActivity(intent);
                break;
            case R.id.controls_button:
                intent = new Intent(this, ControlsConfigActivity.class);
                startActivity(intent);
                break;
            case R.id.settings_remote_button:
                intent = new Intent(this, RemoteConfigActivity.class);
                startActivity(intent);
                break;
            //case R.id.settings_esc_button:
            //    intent = new Intent(this, ESCconfigActivity.class);
            //    startActivity(intent);
            //    break;
            case R.id.led_settins_button:
                intent = new Intent(this, LightsConfigActivity.class);
                startActivity(intent);
                break;
            case R.id.ttl_fw_button:
                final byte txbuf[] = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0F9,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(SettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                break;
            case R.id.firmware_settins_button:
                intent = new Intent(this, FirmwareSettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.settings_import_export_menu_button:
                intent = new Intent(this, ImportExportSettingsActivity.class);
                startActivity(intent);
                break;
        }
    }

    public void onCheckboxClicked(View view) {
        final CheckBox checkbox = (CheckBox) findViewById(view.getId());

        switch (view.getId()) {
            case R.id.settings_read_current_check:
                READ_CURRENT = checkbox.isChecked();
                break;
            case R.id.settings_disp_notif_check:
                DispNotifaction = checkbox.isChecked();
                mLoggingService.DispNotifiaction = DispNotifaction;
                mLoggingService.updateNotification();
                break;
            case R.id.settings_detect_esc_check:
                if(checkbox.isChecked()){
                    new AlertDialog.Builder(this)
                            .setMessage("This will configure the TTL to not begin UART comms until an ESC is detected\n\nThis may cause UART to not work on certain ESCs")
                            .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    byte txbuf[] = new byte[]{
                                            (byte) 0x0A5,
                                            (byte) 0x001,
                                            (byte) 0x0A2,
                                            (byte) 0x001,
                                            (byte) 0x05A
                                    };
                                    if (!mBluetoothService.writeBytes(txbuf)) {
                                        Toast.makeText(SettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                                        checkbox.setChecked(false);
                                    } else {
                                        txbuf = new byte[]{
                                                (byte) 0x0A5,
                                                (byte) 0x000,
                                                (byte) 0x0A3,
                                                (byte) 0x05A
                                        };
                                        while (!mBluetoothService.writeBytes(txbuf)) {}
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkbox.setChecked(false);
                                }
                            })
                            .setCancelable(false)
                            .setIcon(R.drawable.ic_warning)
                            //.setTitle("")
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage("This will configure the TTL to begin communicating over UART immediately at startup\n\nThis can burn out some ESCs if the TTL is powered while the ESC is not")
                            .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    byte txbuf[] = new byte[]{
                                            (byte) 0x0A5,
                                            (byte) 0x001,
                                            (byte) 0x0A2,
                                            (byte) 0x000,
                                            (byte) 0x05A
                                    };
                                    if (!mBluetoothService.writeBytes(txbuf)) {
                                        Toast.makeText(SettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                                        checkbox.setChecked(true);
                                    } else {
                                        txbuf = new byte[]{
                                                (byte) 0x0A5,
                                                (byte) 0x000,
                                                (byte) 0x0A3,
                                                (byte) 0x05A
                                        };
                                        while (!mBluetoothService.writeBytes(txbuf)) {}
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkbox.setChecked(true);
                                }
                            })
                            .setCancelable(false)
                            .setIcon(R.drawable.ic_warning)
                            //.setTitle("Output Test Warning")
                            .show();
                }
                READ_CURRENT = checkbox.isChecked();
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
                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                if(data.length == 5 || data.length == 3 || data.length == 2) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x74:
                                if(i+4 == data.length-1) {
                                    TTL_FW = 0;
                                    TTL_FW += (data[i + 1] & 0x0FF);
                                    TTL_FW += (data[i + 2] & 0x0FF) * 100;
                                    TextView TTL_FW_VIEW = findViewById(R.id.ttl_fw_text);
                                    String TTL_FW_TEXT = "v" + (TTL_FW / 100) + "." + (TTL_FW % 100);
                                    TTL_FW_VIEW.setText(TTL_FW_TEXT);
                                    i += 4;
                                } else if(i+2 == data.length-1) {
                                    TTL_FW = 0;
                                    TTL_FW += (data[i + 1] & 0x0FF);
                                    TTL_FW += (data[i + 2] & 0x0FF) * 100;
                                    TextView TTL_FW_VIEW = findViewById(R.id.ttl_fw_text);
                                    String TTL_FW_TEXT = "v" + (TTL_FW / 100) + "." + (TTL_FW % 100);
                                    TTL_FW_VIEW.setText(TTL_FW_TEXT);
                                    i += 2;
                                }
                                break;
                            case 0x78:
                                if(i+1 != data.length-1)
                                    break;
                                CheckBox check = (findViewById(R.id.settings_detect_esc_check));
                                check.setChecked(data[i+1] == 0x01);
                                i+=1;
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
}
