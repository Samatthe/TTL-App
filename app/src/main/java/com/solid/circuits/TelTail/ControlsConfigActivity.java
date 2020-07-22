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
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
//import android.util.Log;
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


class ControlSort implements Comparator<CharSequence>
{
    // Used for sorting in ascending order of
    // roll number
    List<CharSequence> items = new ArrayList<CharSequence>();
    public int compare(CharSequence a, CharSequence b)
    {
        items.add("None");
        items.add("Single Tap");
        items.add("Double Tap");
        items.add("Triple Tap");
        items.add("Left + Single Tap");
        items.add("Right + Single Tap");
        items.add("Medium Press (0.5><1s)");
        items.add("Long Press (> 1s)");
        return items.indexOf(a) - items.indexOf(b);
    }
}

public class ControlsConfigActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

    public static final String PREFS_NAME = "MyPrefsFile";
    float auxTime = 0;

    ArrayAdapter<CharSequence> single_button_config_adapter;
    List<CharSequence> single_aux_list = new ArrayList<CharSequence>();
    List<CharSequence> single_toggle_brights_list = new ArrayList<CharSequence>();
    List<CharSequence> single_toggle_all_list = new ArrayList<CharSequence>();
    List<CharSequence> single_toggle_head_list = new ArrayList<CharSequence>();
    List<CharSequence> single_toggle_side_list = new ArrayList<CharSequence>();
    List<CharSequence> single_mode_up_list = new ArrayList<CharSequence>();
    List<CharSequence> single_mode_down_list = new ArrayList<CharSequence>();
    /*List<CharSequence> dual_aux_list = new ArrayList<CharSequence>();
    List<CharSequence> dual_toggle_all_list = new ArrayList<CharSequence>();
    List<CharSequence> dual_toggle_head_list = new ArrayList<CharSequence>();
    List<CharSequence> dual_toggle_side_list = new ArrayList<CharSequence>();
    List<CharSequence> dual_mode_up_list = new ArrayList<CharSequence>();
    List<CharSequence> dual_mode_down_list = new ArrayList<CharSequence>();*/

    ArrayAdapter<CharSequence> single_aux_adapter;
    ArrayAdapter<CharSequence> single_toggle_brights_adapter;
    ArrayAdapter<CharSequence> single_mode_down_adapter;
    ArrayAdapter<CharSequence> single_mode_up_adapter;
    ArrayAdapter<CharSequence> single_toggle_all_adapter;
    ArrayAdapter<CharSequence> single_toggle_head_adapter;
    ArrayAdapter<CharSequence> single_toggle_side_adapter;
    /*ArrayAdapter<CharSequence> dual_aux_adapter;
    ArrayAdapter<CharSequence> dual_mode_down_adapter;
    ArrayAdapter<CharSequence> dual_mode_up_adapter;
    ArrayAdapter<CharSequence> dual_toggle_all_adapter;
    ArrayAdapter<CharSequence> dual_toggle_head_adapter;
    ArrayAdapter<CharSequence> dual_toggle_side_adapter;*/

    CheckBox aux_enable_check;
    Spinner aux_type_spinner;
    SeekBar aux_time_seeker;
    Spinner single_aux_control_spinner;
    Spinner single_brights_toggle_spinner;
    Spinner single_mode_down_spinner;
    Spinner single_mode_up_spinner;
    Spinner single_toggle_all_spinner;
    Spinner single_toggle_head_spinner;
    Spinner single_toggle_side_spinner;
    /*Spinner dual_aux_control_spinner;
    Spinner dual_mode_down_spinner;
    Spinner dual_mode_up_spinner;
    Spinner dual_toggle_all_spinner;
    Spinner dual_toggle_head_spinner;
    Spinner dual_toggle_side_spinner;*/
    CheckBox turn_enable_check;

    String single_aux_control_last = "None";
    String single_brights_toggle_last = "None";
    String single_all_toggle_last = "None";
    String single_head_toggle_last = "None";
    String single_side_toggle_last = "None";
    String single_mode_down_last = "None";
    String single_mode_up_last = "None";
    /*String dual_aux_control_last = "None";
    String dual_all_toggle_last = "None";
    String dual_head_toggle_last = "None";
    String dual_side_toggle_last = "None";
    String dual_mode_down_last = "None";
    String dual_mode_up_last = "None";*/

    boolean CHECK_DATA = false;
    long applytimer = 0;
    long applytime = 500;

    boolean HIGHBEAMS_ENABLED = false;

    private final static String TAG = ControlsConfigActivity.class.getSimpleName();

    private BluetoothService mBluetoothService;
    public LoggingService mLoggingService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                final byte txbuf[] = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0FC,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(ControlsConfigActivity.this, "Could not read controls\nPlease try again", Toast.LENGTH_SHORT).show();
                }
            } else if (componentName.getClassName().equals(LoggingService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
                mLoggingService = ((LoggingService.LocalBinder) service).getService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            } else if (componentName.getClassName().equals(LoggingService.class.getName())) {
                mLoggingService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls_config);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeSingleList(single_aux_list);
        initializeSingleList(single_toggle_brights_list);
        initializeSingleList(single_toggle_all_list);
        initializeSingleList(single_toggle_head_list);
        initializeSingleList(single_toggle_side_list);
        initializeSingleList(single_mode_down_list);
        initializeSingleList(single_mode_up_list);
        /*initializeDualList(dual_aux_list);
        initializeDualList(dual_toggle_all_list);
        initializeDualList(dual_toggle_head_list);
        initializeDualList(dual_toggle_side_list);
        initializeDualList(dual_mode_down_list);
        initializeDualList(dual_mode_up_list);*/

        aux_type_spinner = (Spinner) findViewById(R.id.aux_type_spinner);
        single_aux_control_spinner = (Spinner) findViewById(R.id.single_aux_control_spinner);
        single_brights_toggle_spinner = (Spinner) findViewById(R.id.single_toggle_brights_spinner);
        single_mode_down_spinner = (Spinner) findViewById(R.id.single_mode_down_spinner);
        single_mode_up_spinner = (Spinner) findViewById(R.id.single_mode_up_spinner);
        single_toggle_all_spinner = (Spinner) findViewById(R.id.single_toggle_all_spinner);
        single_toggle_head_spinner = (Spinner) findViewById(R.id.single_toggle_head_spinner);
        single_toggle_side_spinner = (Spinner) findViewById(R.id.single_toggle_side_spinner);
        /*dual_aux_control_spinner = (Spinner) findViewById(R.id.dual_aux_control_spinner);
        dual_mode_down_spinner = (Spinner) findViewById(R.id.dual_mode_down_spinner);
        dual_mode_up_spinner = (Spinner) findViewById(R.id.dual_mode_up_spinner);
        dual_toggle_all_spinner = (Spinner) findViewById(R.id.dual_toggle_all_spinner);
        dual_toggle_head_spinner = (Spinner) findViewById(R.id.dual_toggle_head_spinner);
        dual_toggle_side_spinner = (Spinner) findViewById(R.id.dual_toggle_side_spinner);*/

        aux_enable_check = (CheckBox) findViewById(R.id.controls_aux_enable_check);
        turn_enable_check = (CheckBox) findViewById(R.id.controls_turn_enable_check);

        aux_time_seeker = (SeekBar) findViewById(R.id.aux_time_seeker);
        aux_time_seeker.setOnSeekBarChangeListener(this);

        // Aux control type spinner
        aux_type_spinner = (Spinner) findViewById(R.id.aux_type_spinner);
        ArrayAdapter<CharSequence> aux_type_adapter = ArrayAdapter.createFromResource(this,
                R.array.aux_type_array, R.layout.gps_spinner_item);
        aux_type_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        aux_type_spinner.setAdapter(aux_type_adapter);

        // Aux control assignment spinner
        single_aux_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, single_aux_list);
        single_aux_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        single_aux_adapter.notifyDataSetChanged();
        single_aux_control_spinner.setAdapter(single_aux_adapter);

        // Brights toggle spinner
        single_toggle_brights_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, single_toggle_brights_list);
        single_toggle_brights_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        single_toggle_brights_adapter.notifyDataSetChanged();
        single_brights_toggle_spinner.setAdapter(single_toggle_brights_adapter);

        // Single mode down spinner
        single_mode_down_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, single_mode_down_list);
        single_mode_down_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        single_mode_down_adapter.notifyDataSetChanged();
        single_mode_down_spinner.setAdapter(single_mode_down_adapter);

        // Single mode up spinner
        single_mode_up_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, single_mode_up_list);
        single_mode_up_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        single_mode_up_adapter.notifyDataSetChanged();
        single_mode_up_spinner.setAdapter(single_mode_up_adapter);

        // Single toggle all spinner
        single_toggle_all_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, single_toggle_all_list);
        single_toggle_all_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        single_toggle_all_adapter.notifyDataSetChanged();
        single_toggle_all_spinner.setAdapter(single_toggle_all_adapter);

        // Single toggle head spinner
        single_toggle_head_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, single_toggle_head_list);
        single_toggle_head_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        single_toggle_head_adapter.notifyDataSetChanged();
        single_toggle_head_spinner.setAdapter(single_toggle_head_adapter);

        // Single toggle side spinner
        single_toggle_side_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, single_toggle_side_list);
        single_toggle_side_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        single_toggle_side_adapter.notifyDataSetChanged();
        single_toggle_side_spinner.setAdapter(single_toggle_side_adapter);

        // Aux control assignment spinner
        /*dual_aux_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, dual_aux_list);
        dual_aux_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        dual_aux_adapter.notifyDataSetChanged();
        dual_aux_control_spinner.setAdapter(dual_aux_adapter);

        // Dual mode down spinner
        dual_mode_down_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, dual_mode_down_list);
        dual_mode_down_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        dual_mode_down_adapter.notifyDataSetChanged();
        dual_mode_down_spinner.setAdapter(dual_mode_down_adapter);

        // Single mode up spinner
        dual_mode_up_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, dual_mode_up_list);
        dual_mode_up_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        dual_mode_up_adapter.notifyDataSetChanged();
        dual_mode_up_spinner.setAdapter(dual_mode_up_adapter);

        // Single toggle all spinner
        dual_toggle_all_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, dual_toggle_all_list);
        dual_toggle_all_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        dual_toggle_all_adapter.notifyDataSetChanged();
        dual_toggle_all_spinner.setAdapter(dual_toggle_all_adapter);

        // Single toggle head spinner
        dual_toggle_head_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, dual_toggle_head_list);
        dual_toggle_head_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        dual_toggle_head_adapter.notifyDataSetChanged();
        dual_toggle_head_spinner.setAdapter(dual_toggle_head_adapter);

        // Single toggle side spinner
        dual_toggle_side_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, dual_toggle_side_list);
        dual_toggle_side_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        dual_toggle_side_adapter.notifyDataSetChanged();
        dual_toggle_side_spinner.setAdapter(dual_toggle_side_adapter);*/

        aux_type_spinner.setOnItemSelectedListener(this);
        single_aux_control_spinner.setOnItemSelectedListener(this);
        single_brights_toggle_spinner.setOnItemSelectedListener(this);
        single_mode_down_spinner.setOnItemSelectedListener(this);
        single_mode_up_spinner.setOnItemSelectedListener(this);
        single_toggle_all_spinner.setOnItemSelectedListener(this);
        single_toggle_head_spinner.setOnItemSelectedListener(this);
        single_toggle_side_spinner.setOnItemSelectedListener(this);
        /*dual_aux_control_spinner.setOnItemSelectedListener(this);
        dual_mode_down_spinner.setOnItemSelectedListener(this);
        dual_mode_up_spinner.setOnItemSelectedListener(this);
        dual_toggle_all_spinner.setOnItemSelectedListener(this);
        dual_toggle_head_spinner.setOnItemSelectedListener(this);
        dual_toggle_side_spinner.setOnItemSelectedListener(this);*/

        restoresettings();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Intent LogServiceIntent = new Intent(this, LoggingService.class);
        bindService(LogServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    void savesettings() {
        SeekBar aux_time_seeker = (SeekBar) findViewById(R.id.aux_time_seeker);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("AuxEnable", aux_enable_check.isChecked());
        editor.putInt("AuxType", aux_type_spinner.getSelectedItemPosition());
        editor.putInt("AuxTime", aux_time_seeker.getProgress());

        mLoggingService.updateNotification();

        // Commit the edits!
        editor.commit();
    }

    void restoresettings() {
        SeekBar aux_time_seeker = (SeekBar) findViewById(R.id.aux_time_seeker);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        aux_enable_check.setChecked(settings.getBoolean("AuxEnable", false));
        aux_type_spinner.setSelection(settings.getInt("AuxType", 0));
        aux_time_seeker.setProgress(settings.getInt("AuxTime", 50));

        TextView brights_toggle_text = findViewById(R.id.single_toggle_brights_text);
        HIGHBEAMS_ENABLED = settings.getBoolean("HighbeamEnable", false);
        if(!HIGHBEAMS_ENABLED) {
            brights_toggle_text.setTextColor(Color.GRAY);
            single_brights_toggle_spinner.setEnabled(false);
        }else{
            brights_toggle_text.setTextColor(Color.WHITE);
            single_brights_toggle_spinner.setEnabled(true);
        }
    }

    public void onButtonClick(View view) {
        byte txbuf[];
        switch (view.getId()) {
            case R.id.controls_read_button:
                txbuf = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0FC,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(ControlsConfigActivity.this, "Could not read orientation\nPlease try again", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.controls_apply_button:
                int checkState1 = aux_enable_check.isChecked() ? 1 : 0;
                int checkState2 = turn_enable_check.isChecked() ? 1 : 0;
                txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x006,
                        (byte) 0x0C2,
                        (byte) ((checkState1 << 7) | (checkState2 << 6) | aux_type_spinner.getSelectedItemPosition()),
                        (byte) aux_time_seeker.getProgress(),
                        (byte) ((getControlID(single_aux_control_spinner.getSelectedItem().toString()) << 4) | getControlID(single_toggle_all_spinner.getSelectedItem().toString())),
                        (byte) ((getControlID(single_toggle_head_spinner.getSelectedItem().toString()) << 4) | getControlID(single_toggle_side_spinner.getSelectedItem().toString())),
                        (byte) ((getControlID(single_mode_down_spinner.getSelectedItem().toString()) << 4) | getControlID(single_mode_up_spinner.getSelectedItem().toString())),
                        (byte) getControlID(single_brights_toggle_spinner.getSelectedItem().toString()),
                        //(byte) ((getControlID(dual_aux_control_spinner.getSelectedItem().toString()) << 4) | getControlID(dual_toggle_all_spinner.getSelectedItem().toString())),
                        //(byte) ((getControlID(dual_toggle_head_spinner.getSelectedItem().toString()) << 4) | getControlID(dual_toggle_side_spinner.getSelectedItem().toString())),
                        //(byte) ((getControlID(dual_mode_down_spinner.getSelectedItem().toString()) << 4) | getControlID(dual_mode_up_spinner.getSelectedItem().toString())),
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(ControlsConfigActivity.this, "Could write orientation\nPlease try again", Toast.LENGTH_SHORT).show();
                } else {
                    txbuf = new byte[] {
                            (byte) 0x0A5,
                            (byte) 0x000,
                            (byte) 0x0FC,
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
                Spinner connectorOrientaion = (Spinner)findViewById(R.id.connect_orientation_spinner);
                Spinner powerOrientaion = (Spinner)findViewById(R.id.power_orientation_spinner);
                int temp;
                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                if(CHECK_DATA && (System.currentTimeMillis() - applytimer) > applytime){
                    Toast.makeText(ControlsConfigActivity.this, "Remote config failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                    CHECK_DATA = false;
                }
                else if(data.length == 7) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x81:
                                if(i+6 >= data.length)
                                    break;
                                //Toast.makeText(OrientationActivity.this, "Con: "+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                if(CHECK_DATA) {
                                    boolean dataCorrect = true;
                                    if(aux_enable_check.isChecked() != ((data[i + 1] & 0x80) == 0x80)) dataCorrect = false;
                                    if(turn_enable_check.isChecked() != ((data[i + 1] & 0x40) == 0x40)) dataCorrect = false;
                                    if(aux_type_spinner.getSelectedItemPosition() != (data[i + 1] & 0x0F)) dataCorrect = false;
                                    if(aux_time_seeker.getProgress() != (data[i + 2] & 0xFF)) dataCorrect = false;
                                    if(getControlID(single_aux_control_spinner.getSelectedItem().toString()) != ((data[i + 3] & 0xF0) >> 4)) dataCorrect = false;
                                    if(getControlID(single_toggle_all_spinner.getSelectedItem().toString()) != (data[i + 3] & 0x0F)) dataCorrect = false;
                                    if(getControlID(single_toggle_head_spinner.getSelectedItem().toString()) != ((data[i + 4] & 0xF0) >> 4)) dataCorrect = false;
                                    if(getControlID(single_toggle_side_spinner.getSelectedItem().toString()) != (data[i + 4] & 0x0F)) dataCorrect = false;
                                    if(getControlID(single_mode_down_spinner.getSelectedItem().toString()) != ((data[i + 5] & 0xF0) >> 4)) dataCorrect = false;
                                    if(getControlID(single_mode_up_spinner.getSelectedItem().toString()) != (data[i + 5] & 0x0F)) dataCorrect = false;
                                    //if(getControlID(dual_aux_control_spinner.getSelectedItem().toString()) != ((data[i + 6] & 0xF0) >> 4)) dataCorrect = false;
                                    //if(getControlID(dual_toggle_all_spinner.getSelectedItem().toString()) != (data[i + 6] & 0x0F)) dataCorrect = false;
                                    //if(getControlID(dual_toggle_head_spinner.getSelectedItem().toString()) != ((data[i + 7] & 0xF0) >> 4)) dataCorrect = false;
                                    //if(getControlID(dual_toggle_side_spinner.getSelectedItem().toString()) != (data[i + 7] & 0x0F)) dataCorrect = false;
                                    //if(getControlID(dual_mode_down_spinner.getSelectedItem().toString()) != ((data[i + 8] & 0xF0) >> 4)) dataCorrect = false;
                                    //if(getControlID(dual_mode_up_spinner.getSelectedItem().toString()) != (data[i + 8] & 0x0F)) dataCorrect = false;
                                    if(getControlID(single_brights_toggle_spinner.getSelectedItem().toString()) != (data[i + 6] & 0x0F)) dataCorrect = false;
                                    if(dataCorrect){
                                        Toast.makeText(ControlsConfigActivity.this, "Controls applied successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ControlsConfigActivity.this, "Controls failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                                    }
                                    CHECK_DATA = false;
                                } else {
                                    resetAdapters();

                                    try{
                                        aux_enable_check.setChecked((data[i + 1] & 0x80) == 0x80);
                                        aux_enable_check.callOnClick();
                                        turn_enable_check.setChecked((data[i + 1] & 0x40) == 0x40);
                                        aux_type_spinner.setSelection(data[i + 1] & 0x0F);
                                        aux_time_seeker.setProgress(data[i + 2] & 0xFF);

                                        single_aux_control_last = getControlFromID((data[i + 3] & 0xF0) >> 4).toString();
                                        single_all_toggle_last = getControlFromID((data[i + 3] & 0x0F)).toString();
                                        single_head_toggle_last = getControlFromID(((data[i + 4] & 0xF0) >> 4)).toString();
                                        single_side_toggle_last = getControlFromID((data[i + 4] & 0x0F)).toString();
                                        single_mode_down_last = getControlFromID(((data[i + 5] & 0xF0) >> 4)).toString();
                                        single_mode_up_last = getControlFromID((data[i + 5] & 0x0F)).toString();
                                        single_brights_toggle_last = getControlFromID((data[i + 6] & 0x0F)).toString();
                                        //dual_aux_control_last = getControlFromID(((data[i + 6] & 0xF0) >> 4)).toString();
                                        //dual_all_toggle_last = getControlFromID((data[i + 6] & 0x0F)).toString();
                                        //dual_head_toggle_last = getControlFromID(((data[i + 7] & 0xF0) >> 4)).toString();
                                        //dual_side_toggle_last = getControlFromID((data[i + 7] & 0x0F)).toString();
                                        //dual_mode_down_last = getControlFromID(((data[i + 8] & 0xF0) >> 4)).toString();
                                        //dual_mode_up_last = getControlFromID((data[i + 8] & 0x0F)).toString();
                                        updateAllAdapters();
                                        single_aux_control_spinner.setSelection(single_aux_adapter.getPosition(single_aux_control_last));
                                        single_brights_toggle_spinner.setSelection(single_toggle_brights_adapter.getPosition(single_brights_toggle_last));
                                        single_toggle_all_spinner.setSelection(single_toggle_all_adapter.getPosition(single_all_toggle_last));
                                        single_toggle_head_spinner.setSelection(single_toggle_head_adapter.getPosition(single_head_toggle_last));
                                        single_toggle_side_spinner.setSelection(single_toggle_side_adapter.getPosition(single_side_toggle_last));
                                        single_mode_down_spinner.setSelection(single_mode_down_adapter.getPosition(single_mode_down_last));
                                        single_mode_up_spinner.setSelection(single_mode_up_adapter.getPosition(single_mode_up_last));
                                        //dual_aux_control_spinner.setSelection(dual_aux_adapter.getPosition(dual_aux_control_last));
                                        //dual_toggle_all_spinner.setSelection(dual_toggle_all_adapter.getPosition(dual_all_toggle_last));
                                        //dual_toggle_head_spinner.setSelection(dual_toggle_head_adapter.getPosition(dual_head_toggle_last));
                                        //dual_toggle_side_spinner.setSelection(dual_toggle_side_adapter.getPosition(dual_side_toggle_last));
                                        //dual_mode_down_spinner.setSelection(dual_mode_down_adapter.getPosition(dual_mode_down_last));
                                        //dual_mode_up_spinner.setSelection(dual_mode_up_adapter.getPosition(dual_mode_up_last));
                                    }
                                    catch(Exception e){
                                        //Log.e("BUG:", e.toString());
                                        Toast.makeText(ControlsConfigActivity.this, "Could not read settings correctly", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                i+=6;
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

        if (parent_id == R.id.aux_type_spinner) {
            if(pos == 0) { // Momentary
                //resetAdapters();

                single_aux_adapter.add("Hold > 0.5s");
                //dual_aux_adapter.add("Hold > 0.5s");
                single_aux_control_spinner.setSelection(single_aux_adapter.getCount()-1);
                //dual_aux_control_spinner.setSelection(dual_aux_adapter.getCount()-1);

                resetAdapters();
                updateAdapter(single_toggle_brights_adapter, "None", "Medium Press (0.5><1s)");
                updateAdapter(single_toggle_all_adapter, "None", "Medium Press (0.5><1s)");
                updateAdapter(single_toggle_head_adapter, "None", "Medium Press (0.5><1s)");
                updateAdapter(single_toggle_side_adapter, "None", "Medium Press (0.5><1s)");
                updateAdapter(single_mode_up_adapter, "None", "Medium Press (0.5><1s)");
                updateAdapter(single_mode_down_adapter, "None", "Medium Press (0.5><1s)");

                updateAdapter(single_toggle_brights_adapter, "None","Long Press (> 1s)");
                updateAdapter(single_toggle_all_adapter, "None","Long Press (> 1s)");
                updateAdapter(single_toggle_head_adapter, "None","Long Press (> 1s)");
                updateAdapter(single_toggle_side_adapter, "None","Long Press (> 1s)");
                updateAdapter(single_mode_up_adapter, "None","Long Press (> 1s)");
                updateAdapter(single_mode_down_adapter, "None","Long Press (> 1s)");

                //updateAdapter(dual_toggle_all_adapter, "None", "Medium Press (0.5><1s)");
                //updateAdapter(dual_toggle_head_adapter, "None", "Medium Press (0.5><1s)");
                //updateAdapter(dual_toggle_side_adapter, "None", "Medium Press (0.5><1s)");
                //updateAdapter(dual_mode_up_adapter, "None", "Medium Press (0.5><1s)");
                //updateAdapter(dual_mode_down_adapter, "None", "Medium Press (0.5><1s)");

                //updateAdapter(dual_toggle_all_adapter, "None","Long Press (> 1s)");
                //updateAdapter(dual_toggle_head_adapter, "None","Long Press (> 1s)");
                //updateAdapter(dual_toggle_side_adapter, "None","Long Press (> 1s)");
                //updateAdapter(dual_mode_up_adapter, "None","Long Press (> 1s)");
                //updateAdapter(dual_mode_down_adapter, "None","Long Press (> 1s)");

                single_aux_control_spinner.setEnabled(false);
                single_brights_toggle_spinner.setEnabled(HIGHBEAMS_ENABLED);
                single_mode_down_spinner.setEnabled(true);
                single_mode_up_spinner.setEnabled(true);
                single_toggle_all_spinner.setEnabled(true);
                single_toggle_head_spinner.setEnabled(true);
                single_toggle_side_spinner.setEnabled(true);

                //dual_aux_control_spinner.setEnabled(false);
                //dual_mode_down_spinner.setEnabled(true);
                //dual_mode_up_spinner.setEnabled(true);
                //dual_toggle_all_spinner.setEnabled(true);
                //dual_toggle_head_spinner.setEnabled(true);
                //dual_toggle_side_spinner.setEnabled(true);

                turn_enable_check.setEnabled(false);
            } else {
                if(single_aux_adapter.getPosition("Hold > 0.5s") != -1){
                    single_aux_control_last = "None";
                    updateAdapter(single_aux_adapter, "None", "Hold > 0.5s");

                    updateAdapter(single_toggle_brights_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(single_toggle_all_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(single_toggle_head_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(single_toggle_side_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(single_mode_up_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(single_mode_down_adapter, "Medium Press (0.5><1s)", "None");

                    updateAdapter(single_toggle_brights_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(single_toggle_all_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(single_toggle_head_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(single_toggle_side_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(single_mode_up_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(single_mode_down_adapter, "Long Press (> 1s)", "None");
                }
                /*if(dual_aux_adapter.getPosition("Hold > 0.5s") != -1){
                    dual_aux_control_last = "None";
                    updateAdapter(dual_aux_adapter, "None", "Hold > 0.5s");

                    updateAdapter(dual_toggle_all_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(dual_toggle_head_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(dual_toggle_side_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(dual_mode_up_adapter, "Medium Press (0.5><1s)", "None");
                    updateAdapter(dual_mode_down_adapter, "Medium Press (0.5><1s)", "None");

                    updateAdapter(dual_toggle_all_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(dual_toggle_head_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(dual_toggle_side_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(dual_mode_up_adapter, "Long Press (> 1s)", "None");
                    updateAdapter(dual_mode_down_adapter, "Long Press (> 1s)", "None");
                }*/

                single_aux_control_spinner.setEnabled(true);
                single_brights_toggle_spinner.setEnabled(HIGHBEAMS_ENABLED);
                single_mode_down_spinner.setEnabled(true);
                single_mode_up_spinner.setEnabled(true);
                single_toggle_all_spinner.setEnabled(true);
                single_toggle_head_spinner.setEnabled(true);
                single_toggle_side_spinner.setEnabled(true);

                /*dual_aux_control_spinner.setEnabled(true);
                dual_mode_down_spinner.setEnabled(true);
                dual_mode_up_spinner.setEnabled(true);
                dual_toggle_all_spinner.setEnabled(true);
                dual_toggle_head_spinner.setEnabled(true);
                dual_toggle_side_spinner.setEnabled(true);*/

                turn_enable_check.setEnabled(true);
            }
            aux_time_seeker.setEnabled(pos == 2);
        } else if (parent_id == R.id.single_aux_control_spinner) {
            if(selectedItem != single_aux_control_last) {
                //Toast.makeText(mBluetoothService, single_aux_control_last, Toast.LENGTH_SHORT).show();
                updateAdapter(single_toggle_brights_adapter, single_aux_control_last, selectedItem);
                updateAdapter(single_toggle_all_adapter, single_aux_control_last, selectedItem);
                updateAdapter(single_toggle_head_adapter, single_aux_control_last, selectedItem);
                updateAdapter(single_toggle_side_adapter, single_aux_control_last, selectedItem);
                updateAdapter(single_mode_down_adapter, single_aux_control_last, selectedItem);
                updateAdapter(single_mode_up_adapter, single_aux_control_last, selectedItem);
                single_aux_control_last = selectedItem;
            }
        } else if (parent_id == R.id.single_toggle_brights_spinner) {
            if(selectedItem != single_brights_toggle_last) {
                //Toast.makeText(mBluetoothService, single_mode_down_last, Toast.LENGTH_SHORT).show();
                updateAdapter(single_aux_adapter, single_brights_toggle_last, selectedItem);
                updateAdapter(single_toggle_all_adapter, single_brights_toggle_last, selectedItem);
                updateAdapter(single_toggle_head_adapter, single_brights_toggle_last, selectedItem);
                updateAdapter(single_toggle_side_adapter, single_brights_toggle_last, selectedItem);
                updateAdapter(single_mode_down_adapter, single_brights_toggle_last, selectedItem);
                updateAdapter(single_mode_up_adapter, single_brights_toggle_last, selectedItem);
                single_brights_toggle_last = selectedItem;
            }
        } else if (parent_id == R.id.single_mode_down_spinner) {
            if(selectedItem != single_mode_down_last) {
                //Toast.makeText(mBluetoothService, single_mode_down_last, Toast.LENGTH_SHORT).show();
                updateAdapter(single_aux_adapter, single_mode_down_last, selectedItem);
                updateAdapter(single_toggle_brights_adapter, single_mode_down_last, selectedItem);
                updateAdapter(single_toggle_all_adapter, single_mode_down_last, selectedItem);
                updateAdapter(single_toggle_head_adapter, single_mode_down_last, selectedItem);
                updateAdapter(single_toggle_side_adapter, single_mode_down_last, selectedItem);
                updateAdapter(single_mode_up_adapter, single_mode_down_last, selectedItem);
                single_mode_down_last = selectedItem;
            }
        } else if (parent_id == R.id.single_mode_up_spinner) {
                if(selectedItem != single_mode_up_last) {
                //Toast.makeText(mBluetoothService, single_mode_up_last, Toast.LENGTH_SHORT).show();
                updateAdapter(single_aux_adapter, single_mode_up_last, selectedItem);
                updateAdapter(single_toggle_brights_adapter, single_mode_up_last, selectedItem);
                updateAdapter(single_toggle_all_adapter, single_mode_up_last, selectedItem);
                updateAdapter(single_toggle_head_adapter, single_mode_up_last, selectedItem);
                updateAdapter(single_toggle_side_adapter, single_mode_up_last, selectedItem);
                updateAdapter(single_mode_down_adapter, single_mode_up_last, selectedItem);
                single_mode_up_last = selectedItem;
           }
        } else if (parent_id == R.id.single_toggle_all_spinner) {
            if(selectedItem != single_all_toggle_last) {
                //Toast.makeText(mBluetoothService, single_head_toggle_last, Toast.LENGTH_SHORT).show();
                updateAdapter(single_aux_adapter, single_all_toggle_last, selectedItem);
                updateAdapter(single_toggle_brights_adapter, single_all_toggle_last, selectedItem);
                updateAdapter(single_toggle_head_adapter, single_all_toggle_last, selectedItem);
                updateAdapter(single_toggle_side_adapter, single_all_toggle_last, selectedItem);
                updateAdapter(single_mode_down_adapter, single_all_toggle_last, selectedItem);
                updateAdapter(single_mode_up_adapter, single_all_toggle_last, selectedItem);
                single_all_toggle_last = selectedItem;
            }
        } else if (parent_id == R.id.single_toggle_head_spinner) {
            if(selectedItem != single_head_toggle_last) {
                //Toast.makeText(mBluetoothService, single_head_toggle_last, Toast.LENGTH_SHORT).show();
                updateAdapter(single_aux_adapter, single_head_toggle_last, selectedItem);
                updateAdapter(single_toggle_brights_adapter, single_head_toggle_last, selectedItem);
                updateAdapter(single_toggle_all_adapter, single_head_toggle_last, selectedItem);
                updateAdapter(single_toggle_side_adapter, single_head_toggle_last, selectedItem);
                updateAdapter(single_mode_down_adapter, single_head_toggle_last, selectedItem);
                updateAdapter(single_mode_up_adapter, single_head_toggle_last, selectedItem);
                single_head_toggle_last = selectedItem;
            }
        } else if (parent_id == R.id.single_toggle_side_spinner) {
            if(selectedItem != single_side_toggle_last) {
                //Toast.makeText(mBluetoothService, single_side_toggle_last, Toast.LENGTH_SHORT).show();
                updateAdapter(single_aux_adapter, single_side_toggle_last, selectedItem);
                updateAdapter(single_toggle_brights_adapter, single_side_toggle_last, selectedItem);
                updateAdapter(single_toggle_all_adapter, single_side_toggle_last, selectedItem);
                updateAdapter(single_toggle_head_adapter, single_side_toggle_last, selectedItem);
                updateAdapter(single_mode_down_adapter, single_side_toggle_last, selectedItem);
                updateAdapter(single_mode_up_adapter, single_side_toggle_last, selectedItem);
                single_side_toggle_last = selectedItem;
            }
        } /*else if (parent_id == R.id.dual_aux_control_spinner) {
            if(selectedItem != dual_aux_control_last) {
                //Toast.makeText(mBluetoothService, dual_aux_control_last, Toast.LENGTH_SHORT).show();
                updateAdapter(dual_toggle_all_adapter, dual_aux_control_last, selectedItem);
                updateAdapter(dual_toggle_head_adapter, dual_aux_control_last, selectedItem);
                updateAdapter(dual_toggle_side_adapter, dual_aux_control_last, selectedItem);
                updateAdapter(dual_mode_down_adapter, dual_aux_control_last, selectedItem);
                updateAdapter(dual_mode_up_adapter, dual_aux_control_last, selectedItem);
                dual_aux_control_last = selectedItem;
            }
        } else if (parent_id == R.id.dual_mode_down_spinner) {
            if(selectedItem != dual_mode_down_last) {
                //Toast.makeText(mBluetoothService, dual_mode_down_last, Toast.LENGTH_SHORT).show();
                updateAdapter(dual_aux_adapter, dual_mode_down_last, selectedItem);
                updateAdapter(dual_toggle_all_adapter, dual_mode_down_last, selectedItem);
                updateAdapter(dual_toggle_head_adapter, dual_mode_down_last, selectedItem);
                updateAdapter(dual_toggle_side_adapter, dual_mode_down_last, selectedItem);
                updateAdapter(dual_mode_up_adapter, dual_mode_down_last, selectedItem);
                dual_mode_down_last = selectedItem;
            }
        } else if (parent_id == R.id.dual_mode_up_spinner) {
            if(selectedItem != dual_mode_up_last) {
                //Toast.makeText(mBluetoothService, dual_mode_up_last, Toast.LENGTH_SHORT).show();
                updateAdapter(dual_aux_adapter, dual_mode_up_last, selectedItem);
                updateAdapter(dual_toggle_all_adapter, dual_mode_up_last, selectedItem);
                updateAdapter(dual_toggle_head_adapter, dual_mode_up_last, selectedItem);
                updateAdapter(dual_toggle_side_adapter, dual_mode_up_last, selectedItem);
                updateAdapter(dual_mode_down_adapter, dual_mode_up_last, selectedItem);
                dual_mode_up_last = selectedItem;
            }
        } else if (parent_id == R.id.dual_toggle_all_spinner) {
            if(selectedItem != dual_all_toggle_last) {
                //Toast.makeText(mBluetoothService, dual_head_toggle_last, Toast.LENGTH_SHORT).show();
                updateAdapter(dual_aux_adapter, dual_all_toggle_last, selectedItem);
                updateAdapter(dual_toggle_head_adapter, dual_all_toggle_last, selectedItem);
                updateAdapter(dual_toggle_side_adapter, dual_all_toggle_last, selectedItem);
                updateAdapter(dual_mode_down_adapter, dual_all_toggle_last, selectedItem);
                updateAdapter(dual_mode_up_adapter, dual_all_toggle_last, selectedItem);
                dual_all_toggle_last = selectedItem;
            }
        } else if (parent_id == R.id.dual_toggle_head_spinner) {
            if(selectedItem != dual_head_toggle_last) {
                //Toast.makeText(mBluetoothService, dual_head_toggle_last, Toast.LENGTH_SHORT).show();
                updateAdapter(dual_aux_adapter, dual_head_toggle_last, selectedItem);
                updateAdapter(dual_toggle_all_adapter, dual_head_toggle_last, selectedItem);
                updateAdapter(dual_toggle_side_adapter, dual_head_toggle_last, selectedItem);
                updateAdapter(dual_mode_down_adapter, dual_head_toggle_last, selectedItem);
                updateAdapter(dual_mode_up_adapter, dual_head_toggle_last, selectedItem);
                dual_head_toggle_last = selectedItem;
            }
        } else if (parent_id == R.id.dual_toggle_side_spinner) {
            if(selectedItem != dual_side_toggle_last) {
                //Toast.makeText(mBluetoothService, dual_side_toggle_last, Toast.LENGTH_SHORT).show();
                updateAdapter(dual_aux_adapter, dual_side_toggle_last, selectedItem);
                updateAdapter(dual_toggle_all_adapter, dual_side_toggle_last, selectedItem);
                updateAdapter(dual_toggle_head_adapter, dual_side_toggle_last, selectedItem);
                updateAdapter(dual_mode_down_adapter, dual_side_toggle_last, selectedItem);
                updateAdapter(dual_mode_up_adapter, dual_side_toggle_last, selectedItem);
                dual_side_toggle_last = selectedItem;
            }
        }*/
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent){
    }

    public void onCheckboxClicked(View view) {
        CheckBox checkbox = (CheckBox) findViewById(view.getId());
        Spinner aux_type_spinner = (Spinner) findViewById(R.id.aux_type_spinner);
        SeekBar aux_time_seeker = (SeekBar) findViewById(R.id.aux_time_seeker);
        Spinner single_aux_control_spinner = (Spinner) findViewById(R.id.single_aux_control_spinner);
        //Spinner dual_aux_control_spinner = (Spinner) findViewById(R.id.dual_aux_control_spinner);
        switch (view.getId()) {
            case R.id.controls_aux_enable_check:
                aux_type_spinner.setEnabled(checkbox.isChecked());
                aux_time_seeker.setEnabled(checkbox.isChecked() && aux_type_spinner.getSelectedItemPosition()==2);
                single_aux_control_spinner.setEnabled(checkbox.isChecked() && aux_type_spinner.getSelectedItemPosition()!=0);
                //dual_aux_control_spinner.setEnabled(checkbox.isChecked() && aux_type_spinner.getSelectedItemPosition()!=0);
                if(aux_type_spinner.getSelectedItemPosition()==0 && checkbox.isChecked()) {
                    resetAdapters();

                    single_aux_adapter.add("Hold > 0.5s");
                    //dual_aux_adapter.add("Hold > 0.5s");
                    single_aux_control_last = "Hold > 0.5s";
                    //dual_aux_control_last = "Hold > 0.5s";
                    single_aux_control_spinner.setSelection(single_aux_adapter.getCount() - 1);
                    //dual_aux_control_spinner.setSelection(dual_aux_adapter.getCount() - 1);

                    //resetAdapters();
                    updateAdapter(single_toggle_brights_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(single_toggle_all_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(single_toggle_head_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(single_toggle_side_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(single_mode_up_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(single_mode_down_adapter, "None", "Medium Press (0.5><1s)");

                    updateAdapter(single_toggle_brights_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(single_toggle_all_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(single_toggle_head_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(single_toggle_side_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(single_mode_up_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(single_mode_down_adapter, "None", "Long Press (> 1s)");

                    /*updateAdapter(dual_toggle_all_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(dual_toggle_head_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(dual_toggle_side_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(dual_mode_up_adapter, "None", "Medium Press (0.5><1s)");
                    updateAdapter(dual_mode_down_adapter, "None", "Medium Press (0.5><1s)");

                    updateAdapter(dual_toggle_all_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(dual_toggle_head_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(dual_toggle_side_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(dual_mode_up_adapter, "None", "Long Press (> 1s)");
                    updateAdapter(dual_mode_down_adapter, "None", "Long Press (> 1s)");*/

                    single_aux_control_spinner.setEnabled(false);
                    single_brights_toggle_spinner.setEnabled(HIGHBEAMS_ENABLED);
                    single_mode_down_spinner.setEnabled(true);
                    single_mode_up_spinner.setEnabled(true);
                    single_toggle_all_spinner.setEnabled(true);
                    single_toggle_head_spinner.setEnabled(true);
                    single_toggle_side_spinner.setEnabled(true);

                    /*dual_aux_control_spinner.setEnabled(false);
                    dual_mode_down_spinner.setEnabled(true);
                    dual_mode_up_spinner.setEnabled(true);
                    dual_toggle_all_spinner.setEnabled(true);
                    dual_toggle_head_spinner.setEnabled(true);
                    dual_toggle_side_spinner.setEnabled(true);*/

                    turn_enable_check.setEnabled(false);
                } else if(!checkbox.isChecked()) {
                    if(single_aux_adapter.getPosition("Hold > 0.5s") != -1){
                        single_aux_control_last = "None";
                        updateAdapter(single_aux_adapter, "None", "Hold > 0.5s");

                        updateAdapter(single_toggle_brights_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(single_toggle_all_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(single_toggle_head_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(single_toggle_side_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(single_mode_up_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(single_mode_down_adapter, "Medium Press (0.5><1s)", "None");

                        updateAdapter(single_toggle_brights_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(single_toggle_all_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(single_toggle_head_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(single_toggle_side_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(single_mode_up_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(single_mode_down_adapter, "Long Press (> 1s)", "None");
                    }
                    /*if(dual_aux_adapter.getPosition("Hold > 0.5s") != -1){
                        dual_aux_control_last = "None";
                        updateAdapter(dual_aux_adapter, "None", "Hold > 0.5s");

                        updateAdapter(dual_toggle_all_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(dual_toggle_head_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(dual_toggle_side_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(dual_mode_up_adapter, "Medium Press (0.5><1s)", "None");
                        updateAdapter(dual_mode_down_adapter, "Medium Press (0.5><1s)", "None");

                        updateAdapter(dual_toggle_all_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(dual_toggle_head_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(dual_toggle_side_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(dual_mode_up_adapter, "Long Press (> 1s)", "None");
                        updateAdapter(dual_mode_down_adapter, "Long Press (> 1s)", "None");
                    }*/

                    turn_enable_check.setEnabled(true);
                }
                break;
            case R.id.controls_turn_enable_check:

                break;
        }
    }

    void updateAdapter(ArrayAdapter<CharSequence> adapter, String add, String remove){
        if(remove!="None")
            adapter.remove(remove);
        if(add!="None" && add!=remove)
            adapter.add(add);
        adapter.sort(new ControlSort());
        if(adapter == single_aux_adapter)
            single_aux_control_spinner.setSelection(adapter.getPosition(single_aux_control_last));
        else if(adapter == single_toggle_brights_adapter)
            single_brights_toggle_spinner.setSelection(adapter.getPosition(single_brights_toggle_last));
        else if(adapter == single_mode_down_adapter)
            single_mode_down_spinner.setSelection(adapter.getPosition(single_mode_down_last));
        else if(adapter == single_mode_up_adapter)
            single_mode_up_spinner.setSelection(adapter.getPosition(single_mode_up_last));
        else if(adapter == single_toggle_all_adapter)
            single_toggle_all_spinner.setSelection(adapter.getPosition(single_all_toggle_last));
        else if(adapter == single_toggle_head_adapter)
            single_toggle_head_spinner.setSelection(adapter.getPosition(single_head_toggle_last));
        else if(adapter == single_toggle_side_adapter)
            single_toggle_side_spinner.setSelection(adapter.getPosition(single_side_toggle_last));

        /*if(adapter == dual_aux_adapter)
            dual_aux_control_spinner.setSelection(adapter.getPosition(dual_aux_control_last));
        else if(adapter == dual_mode_down_adapter)
            dual_mode_down_spinner.setSelection(adapter.getPosition(dual_mode_down_last));
        else if(adapter == dual_mode_up_adapter)
            dual_mode_up_spinner.setSelection(adapter.getPosition(dual_mode_up_last));
        else if(adapter == dual_toggle_all_adapter)
            dual_toggle_all_spinner.setSelection(adapter.getPosition(dual_all_toggle_last));
        else if(adapter == dual_toggle_head_adapter)
            dual_toggle_head_spinner.setSelection(adapter.getPosition(dual_head_toggle_last));
        else if(adapter == dual_toggle_side_adapter)
            dual_toggle_side_spinner.setSelection(adapter.getPosition(dual_side_toggle_last));*/
    }

    void initializeSingleList(List<CharSequence> list){
        list.clear();
        list.add("None");
        list.add("Single Tap");
        list.add("Double Tap");
        list.add("Triple Tap");
        list.add("Medium Press (0.5><1s)");
        list.add("Long Press (> 1s)");
    }

    /*void initializeDualList(List<CharSequence> list){
        list.clear();
        list.add("None");
        list.add("Single Tap");
        list.add("Double Tap");
        list.add("Triple Tap");
        list.add("Left + Single Tap");
        list.add("Right + Single Tap");
        list.add("Medium Press (0.5><1s)");
        list.add("Long Press (> 1s)");
    }*/

    int getControlID(String controlText) {
        List<CharSequence> controls = new ArrayList<CharSequence>();
        controls.add("None");
        controls.add("Single Tap");
        controls.add("Double Tap");
        controls.add("Triple Tap");
        controls.add("Left + Single Tap");
        controls.add("Right + Single Tap");
        controls.add("Medium Press (0.5><1s)");
        controls.add("Long Press (> 1s)");
        controls.add("Hold > 0.5s");
        return controls.indexOf(controlText);
    }

    CharSequence getControlFromID(int controlPos) {
        List<CharSequence> controls = new ArrayList<CharSequence>();
        controls.add("None");
        controls.add("Single Tap");
        controls.add("Double Tap");
        controls.add("Triple Tap");
        controls.add("Left + Single Tap");
        controls.add("Right + Single Tap");
        controls.add("Medium Press (0.5><1s)");
        controls.add("Long Press (> 1s)");
        controls.add("Hold > 0.5s");
        return controls.get(controlPos);
    }

    void updateAllAdapters(){
        if(single_brights_toggle_last != "None") single_aux_adapter.remove(single_brights_toggle_last);
        if(single_all_toggle_last != "None") single_aux_adapter.remove(single_all_toggle_last);
        if(single_head_toggle_last != "None") single_aux_adapter.remove(single_head_toggle_last);
        if(single_side_toggle_last != "None") single_aux_adapter.remove(single_side_toggle_last);
        if(single_mode_down_last != "None") single_aux_adapter.remove(single_mode_down_last);
        if(single_mode_up_last != "None") single_aux_adapter.remove(single_mode_up_last);

        if(single_aux_control_last != "None") single_toggle_brights_adapter.remove(single_aux_control_last);
        if(single_all_toggle_last != "None") single_toggle_brights_adapter.remove(single_all_toggle_last);
        if(single_head_toggle_last != "None") single_toggle_brights_adapter.remove(single_head_toggle_last);
        if(single_side_toggle_last != "None") single_toggle_brights_adapter.remove(single_side_toggle_last);
        if(single_mode_down_last != "None") single_toggle_brights_adapter.remove(single_mode_down_last);
        if(single_mode_up_last != "None") single_toggle_brights_adapter.remove(single_mode_up_last);

        if(single_aux_control_last != "None") single_toggle_all_adapter.remove(single_aux_control_last);
        if(single_brights_toggle_last != "None") single_toggle_all_adapter.remove(single_brights_toggle_last);
        if(single_head_toggle_last != "None") single_toggle_all_adapter.remove(single_head_toggle_last);
        if(single_side_toggle_last != "None") single_toggle_all_adapter.remove(single_side_toggle_last);
        if(single_mode_down_last != "None") single_toggle_all_adapter.remove(single_mode_down_last);
        if(single_mode_up_last != "None") single_toggle_all_adapter.remove(single_mode_up_last);

        if(single_aux_control_last != "None") single_toggle_head_adapter.remove(single_aux_control_last);
        if(single_brights_toggle_last != "None") single_toggle_head_adapter.remove(single_brights_toggle_last);
        if(single_all_toggle_last != "None") single_toggle_head_adapter.remove(single_all_toggle_last);
        if(single_side_toggle_last != "None") single_toggle_head_adapter.remove(single_side_toggle_last);
        if(single_mode_down_last != "None") single_toggle_head_adapter.remove(single_mode_down_last);
        if(single_mode_up_last != "None") single_toggle_head_adapter.remove(single_mode_up_last);

        if(single_aux_control_last != "None") single_toggle_side_adapter.remove(single_aux_control_last);
        if(single_brights_toggle_last != "None") single_toggle_side_adapter.remove(single_brights_toggle_last);
        if(single_all_toggle_last != "None") single_toggle_side_adapter.remove(single_all_toggle_last);
        if(single_head_toggle_last != "None") single_toggle_side_adapter.remove(single_head_toggle_last);
        if(single_mode_down_last != "None") single_toggle_side_adapter.remove(single_mode_down_last);
        if(single_mode_up_last != "None") single_toggle_side_adapter.remove(single_mode_up_last);

        if(single_aux_control_last != "None") single_mode_down_adapter.remove(single_aux_control_last);
        if(single_brights_toggle_last != "None") single_mode_down_adapter.remove(single_brights_toggle_last);
        if(single_all_toggle_last != "None") single_mode_down_adapter.remove(single_all_toggle_last);
        if(single_head_toggle_last != "None") single_mode_down_adapter.remove(single_head_toggle_last);
        if(single_side_toggle_last != "None") single_mode_down_adapter.remove(single_side_toggle_last);
        if(single_mode_up_last != "None") single_mode_down_adapter.remove(single_mode_up_last);

        if(single_aux_control_last != "None") single_mode_up_adapter.remove(single_aux_control_last);
        if(single_brights_toggle_last != "None") single_mode_up_adapter.remove(single_brights_toggle_last);
        if(single_all_toggle_last != "None") single_mode_up_adapter.remove(single_all_toggle_last);
        if(single_head_toggle_last != "None") single_mode_up_adapter.remove(single_head_toggle_last);
        if(single_side_toggle_last != "None") single_mode_up_adapter.remove(single_side_toggle_last);
        if(single_mode_down_last != "None") single_mode_up_adapter.remove(single_mode_down_last);

        /*if(dual_all_toggle_last != "None") dual_aux_adapter.remove(dual_all_toggle_last);
        if(dual_head_toggle_last != "None") dual_aux_adapter.remove(dual_head_toggle_last);
        if(dual_side_toggle_last != "None") dual_aux_adapter.remove(dual_side_toggle_last);
        if(dual_mode_down_last != "None") dual_aux_adapter.remove(dual_mode_down_last);
        if(dual_mode_up_last != "None") dual_aux_adapter.remove(dual_mode_up_last);

        if(dual_aux_control_last != "None") dual_toggle_all_adapter.remove(dual_aux_control_last);
        if(dual_head_toggle_last != "None") dual_toggle_all_adapter.remove(dual_head_toggle_last);
        if(dual_side_toggle_last != "None") dual_toggle_all_adapter.remove(dual_side_toggle_last);
        if(dual_mode_down_last != "None") dual_toggle_all_adapter.remove(dual_mode_down_last);
        if(dual_mode_up_last != "None") dual_toggle_all_adapter.remove(dual_mode_up_last);

        if(dual_aux_control_last != "None") dual_toggle_head_adapter.remove(dual_aux_control_last);
        if(dual_all_toggle_last != "None") dual_toggle_head_adapter.remove(dual_all_toggle_last);
        if(dual_side_toggle_last != "None") dual_toggle_head_adapter.remove(dual_side_toggle_last);
        if(dual_mode_down_last != "None") dual_toggle_head_adapter.remove(dual_mode_down_last);
        if(dual_mode_up_last != "None") dual_toggle_head_adapter.remove(dual_mode_up_last);

        if(dual_aux_control_last != "None") dual_toggle_side_adapter.remove(dual_aux_control_last);
        if(dual_all_toggle_last != "None") dual_toggle_side_adapter.remove(dual_all_toggle_last);
        if(dual_head_toggle_last != "None") dual_toggle_side_adapter.remove(dual_head_toggle_last);
        if(dual_mode_down_last != "None") dual_toggle_side_adapter.remove(dual_mode_down_last);
        if(dual_mode_up_last != "None") dual_toggle_side_adapter.remove(dual_mode_up_last);

        if(dual_aux_control_last != "None") dual_mode_down_adapter.remove(dual_aux_control_last);
        if(dual_all_toggle_last != "None") dual_mode_down_adapter.remove(dual_all_toggle_last);
        if(dual_head_toggle_last != "None") dual_mode_down_adapter.remove(dual_head_toggle_last);
        if(dual_side_toggle_last != "None") dual_mode_down_adapter.remove(dual_side_toggle_last);
        if(dual_mode_up_last != "None") dual_mode_down_adapter.remove(dual_mode_up_last);

        if(dual_aux_control_last != "None") dual_mode_up_adapter.remove(dual_aux_control_last);
        if(dual_all_toggle_last != "None") dual_mode_up_adapter.remove(dual_all_toggle_last);
        if(dual_head_toggle_last != "None") dual_mode_up_adapter.remove(dual_head_toggle_last);
        if(dual_side_toggle_last != "None") dual_mode_up_adapter.remove(dual_side_toggle_last);
        if(dual_mode_down_last != "None") dual_mode_up_adapter.remove(dual_mode_down_last);*/
    }

    void resetAdapters(){
        single_aux_control_spinner.setSelection(0);
        single_brights_toggle_spinner.setSelection(0);
        single_toggle_all_spinner.setSelection(0);
        single_toggle_head_spinner.setSelection(0);
        single_toggle_side_spinner.setSelection(0);
        single_mode_up_spinner.setSelection(0);
        single_mode_down_spinner.setSelection(0);
        /*dual_aux_control_spinner.setSelection(0);
        dual_toggle_all_spinner.setSelection(0);
        dual_toggle_head_spinner.setSelection(0);
        dual_toggle_side_spinner.setSelection(0);
        dual_mode_up_spinner.setSelection(0);
        dual_mode_down_spinner.setSelection(0);*/

        single_aux_control_last = "None";
        single_brights_toggle_last = "None";
        single_all_toggle_last = "None";
        single_head_toggle_last = "None";
        single_side_toggle_last = "None";
        single_mode_up_last = "None";
        single_mode_down_last = "None";
        /*dual_aux_control_last = "None";
        dual_all_toggle_last = "None";
        dual_head_toggle_last = "None";
        dual_side_toggle_last = "None";
        dual_mode_up_last = "None";
        dual_mode_down_last = "None";*/

        initializeSingleList(single_aux_list);
        initializeSingleList(single_toggle_brights_list);
        initializeSingleList(single_toggle_all_list);
        initializeSingleList(single_toggle_head_list);
        initializeSingleList(single_toggle_side_list);
        initializeSingleList(single_mode_down_list);
        initializeSingleList(single_mode_up_list);
        /*initializeDualList(dual_aux_list);
        initializeDualList(dual_toggle_all_list);
        initializeDualList(dual_toggle_head_list);
        initializeDualList(dual_toggle_side_list);
        initializeDualList(dual_mode_down_list);
        initializeDualList(dual_mode_up_list);*/
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView aux_time_number = (TextView) findViewById(R.id.aux_time_number);

        if (seekBar.getId() == R.id.aux_time_seeker) {
            auxTime = (((float)progress)/10);
            aux_time_number.setText("" + auxTime);
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
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        savesettings();
        unregisterReceiver(mGattUpdateReceiver);
    }
}

