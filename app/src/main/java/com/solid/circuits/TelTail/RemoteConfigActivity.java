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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RemoteConfigActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

    public static final String PREFS_NAME = "MyPrefsFile";
    float Deadzone = 0;

    ArrayAdapter<CharSequence> single_button_config_adapter;
    List<CharSequence> remote_type_list = new ArrayList<CharSequence>();
    List<CharSequence> button_type_list = new ArrayList<CharSequence>();

    ArrayAdapter<CharSequence> remote_type_adapter;
    ArrayAdapter<CharSequence> button_type_adapter;

    Spinner remote_type_spinner;
    Spinner button_type_spinner;
    SeekBar deadzone_seeker;

    boolean CHECK_DATA = false;
    long applytimer = 0;
    long applytime = 500;

    private final static String TAG = ControlsConfigActivity.class.getSimpleName();

    private BluetoothService mBluetoothService;
    //public LoggingService mLoggingService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                final byte txbuf[] = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0FB,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(RemoteConfigActivity.this, "Could not read remote config\nPlease try again", Toast.LENGTH_SHORT).show();
                }
            }// else if (componentName.getClassName().equals(LoggingService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
            //    mLoggingService = ((LoggingService.LocalBinder) service).getService();
            //}
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            }// else if (componentName.getClassName().equals(LoggingService.class.getName())) {
             //   mLoggingService = null;
            //}
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeRemoteTypeList(remote_type_list);
        initializeButtonTypeList(button_type_list);

        remote_type_spinner = (Spinner) findViewById(R.id.remote_type_spinner);
        button_type_spinner = (Spinner) findViewById(R.id.remote_button_spinner);

        // Aux control assignment spinner
        remote_type_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, remote_type_list);
        remote_type_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        remote_type_adapter.notifyDataSetChanged();
        remote_type_spinner.setAdapter(remote_type_adapter);

        button_type_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, button_type_list);
        button_type_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        button_type_adapter.notifyDataSetChanged();
        button_type_spinner.setAdapter(button_type_adapter);

        remote_type_spinner.setOnItemSelectedListener(this);
        button_type_spinner.setOnItemSelectedListener(this);

        deadzone_seeker = (SeekBar) findViewById(R.id.remote_deadzone_seeker);
        deadzone_seeker.setOnSeekBarChangeListener(this);

        restoresettings();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    void savesettings() {
        SeekBar deadzone_seeker = (SeekBar) findViewById(R.id.remote_deadzone_seeker);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("RemoteType", remote_type_spinner.getSelectedItemPosition());
        editor.putInt("Deadzone", deadzone_seeker.getProgress());
        editor.putInt("ButtonType", button_type_spinner.getSelectedItemPosition());

        // Commit the edits!
        editor.commit();
    }

    void restoresettings() {
        SeekBar deadzone_seeker = (SeekBar) findViewById(R.id.remote_deadzone_seeker);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        remote_type_spinner.setSelection(settings.getInt("RemoteType", 0));
        deadzone_seeker.setProgress(settings.getInt("Deadzone", 50));
        button_type_spinner.setSelection(settings.getInt("ButtonType", 0));
    }

    public void onButtonClick(View view) {
        byte txbuf[];
        switch (view.getId()) {
            case R.id.remote_config_read_button:
                txbuf = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0FB,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(RemoteConfigActivity.this, "Could not read orientation\nPlease try again", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.remote_config_apply_button:
                txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x002,
                        (byte) 0x0C3,
                        (byte) ((remote_type_spinner.getSelectedItemPosition() << 4) | button_type_spinner.getSelectedItemPosition()),
                        (byte) deadzone_seeker.getProgress(),
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(RemoteConfigActivity.this, "Could write orientation\nPlease try again", Toast.LENGTH_SHORT).show();
                } else {
                    txbuf = new byte[] {
                            (byte) 0x0A5,
                            (byte) 0x000,
                            (byte) 0x0FB,
                            (byte) 0x05A
                    };
                    while(!mBluetoothService.writeBytes(txbuf)) {}
                    CHECK_DATA = true;
                    applytimer = System.currentTimeMillis();
                }
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
                if(CHECK_DATA && (System.currentTimeMillis() - applytimer) > applytime){
                    Toast.makeText(RemoteConfigActivity.this, "Remote config failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                    CHECK_DATA = false;
                }
                else if(data.length == 3) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x72:
                                if(i+2 >= data.length)
                                    break;
                                //Toast.makeText(OrientationActivity.this, "Con: "+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                if(CHECK_DATA) {
                                    boolean dataCorrect = true;
                                    if(remote_type_spinner.getSelectedItemPosition() != ((data[i + 1] & 0xF0) >> 4)) dataCorrect = false;
                                    if(button_type_spinner.getSelectedItemPosition() != (data[i + 1] & 0x0F)) dataCorrect = false;
                                    if(deadzone_seeker.getProgress() != (data[i + 2] & 0xFF)) dataCorrect = false;
                                    //if(ppm_button_seeker.getProgress() != (data[i + 2] & 0xFF)) dataCorrect = false;
                                    if(dataCorrect){
                                        Toast.makeText(RemoteConfigActivity.this, "Remote config applied successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(RemoteConfigActivity.this, "Remote config failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                                    }
                                    CHECK_DATA = false;
                                } else {
                                    remote_type_spinner.setSelection((data[i + 1] & 0xF0) >> 4);
                                    button_type_spinner.setSelection(data[i + 1] & 0x0F);
                                    deadzone_seeker.setProgress(data[i + 2] & 0xFF);
                                    //ppm_button_seeker.setProgress(data[i + 2] & 0xFF);
                                }
                                i+=2;
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        long parent_id = parent.getId();
        if(parent_id == R.id.remote_button_spinner){
            switch(pos){

            }
        }
        String selectedItem = parent.getSelectedItem().toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent){
    }

    void initializeRemoteTypeList(List<CharSequence> list){
        list.clear();
        list.add("PPM");
        list.add("UART + PPM");
        list.add("UART Only");//(Chuck Struct) Single Axis");
        //list.add("UART (Chuck Struct) Dual Axis");
    }

    void initializeButtonTypeList(List<CharSequence> list){
        list.clear();
        list.add("None");
        list.add("Momentary");
        list.add("Latching");
        list.add("Latching PPM");
        list.add("UART (Chuck Struct) C");
        list.add("UART (Chuck Struct) Z");
        list.add("Throttle Down");
        list.add("Throttle Up");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView deadzone_number = (TextView) findViewById(R.id.remote_deadzone_number);

        if (seekBar.getId() == R.id.remote_deadzone_seeker) {
            Deadzone = (((float)progress));
            deadzone_number.setText("" + Deadzone);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected  void onResume(){
        super.onResume();

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
        savesettings();
    }
}

