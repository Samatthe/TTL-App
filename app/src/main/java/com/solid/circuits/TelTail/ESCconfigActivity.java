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

public class ESCconfigActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    public static final String PREFS_NAME = "MyPrefsFile";

    //ArrayAdapter<CharSequence> single_button_config_adapter;
    List<CharSequence> esc_fw_list = new ArrayList<CharSequence>();
    List<CharSequence> esc_comms_list = new ArrayList<CharSequence>();
    List<CharSequence> uart_baud_list = new ArrayList<CharSequence>();

    ArrayAdapter<CharSequence> esc_fw_adapter;
    ArrayAdapter<CharSequence> esc_comms_adapter;
    ArrayAdapter<CharSequence> uart_baud_adapter;

    Spinner esc_fw_spinner;
    Spinner esc_comms_spinner;
    Spinner uart_baud_spinner;

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
                        (byte) 0x0FA,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(ESCconfigActivity.this, "Could not read ESC config\nPlease try again", Toast.LENGTH_SHORT).show();
                }
            } else if (componentName.getClassName().equals(LoggingService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
                //mLoggingService = ((LoggingService.LocalBinder) service).getService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            } else if (componentName.getClassName().equals(LoggingService.class.getName())) {
               // mLoggingService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esc_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeESCfwList(esc_fw_list);
        initializeESCcommsList(esc_comms_list);
        initializeUARTbaudList(uart_baud_list);

        esc_fw_spinner = (Spinner) findViewById(R.id.esc_type_spinner);
        esc_comms_spinner = (Spinner) findViewById(R.id.esc_comms_spinner);
        uart_baud_spinner = (Spinner) findViewById(R.id.uart_baud_spinner);

        esc_fw_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, esc_fw_list);
        esc_fw_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        esc_fw_adapter.notifyDataSetChanged();
        esc_fw_spinner.setAdapter(esc_fw_adapter);

        esc_comms_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, esc_comms_list);
        esc_comms_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        esc_comms_adapter.notifyDataSetChanged();
        esc_comms_spinner.setAdapter(esc_comms_adapter);

        uart_baud_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, uart_baud_list);
        uart_baud_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        uart_baud_adapter.notifyDataSetChanged();
        uart_baud_spinner.setAdapter(uart_baud_adapter);

        esc_fw_spinner.setOnItemSelectedListener(this);
        esc_comms_spinner.setOnItemSelectedListener(this);
        uart_baud_spinner.setOnItemSelectedListener(this);

        restoresettings();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        //Intent LogServiceIntent = new Intent(this, LoggingService.class);
        //bindService(LogServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    void savesettings() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("ESCused", esc_fw_spinner.getSelectedItemPosition());
        editor.putInt("ESCcomms", esc_comms_spinner.getSelectedItemPosition());
        editor.putInt("UARTbaud", uart_baud_spinner.getSelectedItemPosition());

        //mLoggingService.updateNotification();

        // Commit the edits!
        editor.commit();
    }

    void restoresettings() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        esc_fw_spinner.setSelection(settings.getInt("ESCused", 0));
        esc_comms_spinner.setSelection(settings.getInt("ESCcomms", 0));
        uart_baud_spinner.setSelection(settings.getInt("UARTbaud", 0));
    }

    public void onButtonClick(View view) {
        byte txbuf[];
        switch (view.getId()) {
            case R.id.esc_config_read_button:
                txbuf = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0FA,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(ESCconfigActivity.this, "Could not read orientation\nPlease try again", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.esc_config_apply_button:
                txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x002,
                        (byte) 0x0C4,
                        (byte) (esc_fw_spinner.getSelectedItemPosition()),
                        (byte) ((esc_comms_spinner.getSelectedItemPosition() << 4) | uart_baud_spinner.getSelectedItemPosition()),
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(ESCconfigActivity.this, "Could write orientation\nPlease try again", Toast.LENGTH_SHORT).show();
                } else {
                    txbuf = new byte[] {
                            (byte) 0x0A5,
                            (byte) 0x000,
                            (byte) 0x0FA,
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
                    Toast.makeText(ESCconfigActivity.this, "Remote config failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                    CHECK_DATA = false;
                }
                else if(data.length == 3) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x73:
                                if(i+2 >= data.length)
                                    break;
                                //Toast.makeText(OrientationActivity.this, "Con: "+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                if(CHECK_DATA) {
                                    boolean dataCorrect = true;
                                    if(esc_fw_spinner.getSelectedItemPosition() != (data[i + 1] & 0xFF)) dataCorrect = false;
                                    if(esc_comms_spinner.getSelectedItemPosition() != ((data[i + 2] & 0xF0) >> 4)) dataCorrect = false;
                                    if(uart_baud_spinner.getSelectedItemPosition() != (data[i + 2] & 0x0F)) dataCorrect = false;
                                    if(dataCorrect){
                                        Toast.makeText(ESCconfigActivity.this, "ESC config applied successfully", Toast.LENGTH_SHORT).show();
                                        Toast.makeText(ESCconfigActivity.this, "Please power cycle board for settings to take affect", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(ESCconfigActivity.this, "ESC config failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                                    }
                                    CHECK_DATA = false;
                                } else {
                                    esc_fw_spinner.setSelection(data[i + 1] & 0x0FF);
                                    esc_comms_spinner.setSelection((data[i + 2] & 0xF0) >> 4);
                                    uart_baud_spinner.setSelection(data[i + 2] & 0x0F);
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

        String selectedItem = parent.getSelectedItem().toString();

        if (parent_id == R.id.esc_comms_spinner) {
            if(pos == 0) { // Momentary
                uart_baud_spinner.setEnabled(false);
            } else {
                uart_baud_spinner.setEnabled(true);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent){
    }

    void initializeESCfwList(List<CharSequence> list){
        list.clear();
        list.add("Official <= v2.18");
        list.add("Official >= v3.0");
        list.add("FOCBOX Unity");
        list.add("AckManiac");
    }

    void initializeESCcommsList(List<CharSequence> list){
        list.clear();
        list.add("None");
        list.add("I2C");
        list.add("UART");
    }

    void initializeUARTbaudList(List<CharSequence> list){
        list.clear();
        list.add("9600");
        list.add("38400");
        list.add("57600");
        list.add("115200");
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

