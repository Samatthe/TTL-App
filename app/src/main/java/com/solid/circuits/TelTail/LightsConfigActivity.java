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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class LightsConfigActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

    public static final String PREFS_NAME = "MyPrefsFile";
    float Deadzone = 0;

    ArrayAdapter<CharSequence> single_button_config_adapter;
    List<CharSequence> RGB_type_list = new ArrayList<CharSequence>();
    List<CharSequence> brake_mode_list = new ArrayList<CharSequence>();

    ArrayAdapter<CharSequence> RGB_type_adapter;
    ArrayAdapter<CharSequence> brake_mode_adapter;

    Spinner RGB_type_spinner;
    Spinner brake_mode_spinner;
    CheckBox RGB_sync_checkbox;
    CheckBox brake_always_on_checkbox;
    SeekBar deadzone_seeker;
    EditText LED_num_edittext;
    CheckBox default_state_checkbox;

    boolean CHECK_DATA = false;
    long applytimer = 0;
    long applytime = 500;
    boolean IGNORE_SPINNER_CHANGE = false;

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
                        (byte) 0x0A1,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(LightsConfigActivity.this, "Could not read remote config\nPlease try again", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_lights_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeRGBTypeList(RGB_type_list);
        initializeBrakeModeAdapter(brake_mode_list);

        RGB_type_spinner = (Spinner) findViewById(R.id.side_LED_type_spinner);
        brake_mode_spinner = (Spinner) findViewById(R.id.brake_light_mode_spinner);
        RGB_sync_checkbox = findViewById(R.id.side_sync_checkbox);
        brake_always_on_checkbox = findViewById(R.id.brake_always_on_checkbox);
        LED_num_edittext = findViewById(R.id.LED_num_edittext);
        default_state_checkbox = findViewById(R.id.default_state_checkbox);

        // Aux control assignment spinner
        RGB_type_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, RGB_type_list);
        RGB_type_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        RGB_type_adapter.notifyDataSetChanged();
        RGB_type_spinner.setAdapter(RGB_type_adapter);

        brake_mode_adapter = new ArrayAdapter<>(this,
                R.layout.gps_spinner_item, brake_mode_list);
        brake_mode_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        brake_mode_adapter.notifyDataSetChanged();
        brake_mode_spinner.setAdapter(brake_mode_adapter);

        RGB_type_spinner.setOnItemSelectedListener(this);
        brake_mode_spinner.setOnItemSelectedListener(this);

        deadzone_seeker = findViewById(R.id.brake_deadzone_seeker);
        deadzone_seeker.setOnSeekBarChangeListener(this);

        restoresettings();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    void savesettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("RGBType", RGB_type_spinner.getSelectedItemPosition());
        editor.putBoolean("SyncSide", RGB_sync_checkbox.isChecked());
        editor.putString("LEDnum", LED_num_edittext.getText().toString());
        editor.putInt("Deadzone", deadzone_seeker.getProgress());
        editor.putInt("BrakeMode", brake_mode_spinner.getSelectedItemPosition());
        editor.putBoolean("BrakeAlwaysOn", brake_always_on_checkbox.isChecked());
        editor.putBoolean("BrakeAlwaysOn", default_state_checkbox.isChecked());

        // Commit the edits!
        editor.commit();
    }

    void restoresettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings.getInt("RGBType", 0) != RGB_type_spinner.getSelectedItemPosition()) {
            IGNORE_SPINNER_CHANGE = true;
            RGB_type_spinner.setSelection(settings.getInt("RGBType", 0));
        }
        RGB_sync_checkbox.setChecked(settings.getBoolean("SyncSide", true));
        LED_num_edittext.setText(settings.getString("LEDnum", "0"));
        deadzone_seeker.setProgress(settings.getInt("Deadzone", 50));
        brake_mode_spinner.setSelection(settings.getInt("BrakeMode", 0));
        brake_always_on_checkbox.setChecked(settings.getBoolean("BrakeAlwaysOn", false));
        default_state_checkbox.setChecked(settings.getBoolean("BrakeAlwaysOn", false));
    }

    public void onButtonClick(View view) {
        byte txbuf[];
        switch (view.getId()) {
            case R.id.lights_config_read_button:
                txbuf = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0A1,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(LightsConfigActivity.this, "Couldnt write Light Config\nPlease try again", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.lights_config_apply_button:
                int LEDnum = 0;
                try {
                    LEDnum = Integer.parseInt(LED_num_edittext.getText().toString());
                } catch(NumberFormatException nfe) {
                    Toast.makeText(mBluetoothService, "LED number invalid", Toast.LENGTH_SHORT).show();
                    //System.out.println("Could not parse " + nfe);
                }

                byte checks = (byte)((byte)0xFF & (byte)(RGB_sync_checkbox.isChecked() ? 1 : 0) << 7);
                checks = (byte)(checks | ((byte)((byte)0xFF & (byte)(brake_always_on_checkbox.isChecked() ? 1 : 0) << 6)));
                checks = (byte)(checks | ((byte)((byte)0xFF & (byte)(default_state_checkbox.isChecked() ? 1 : 0) << 5)));

                txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x004,
                        (byte) 0x0C5,
                        (byte) ((RGB_type_spinner.getSelectedItemPosition() << 4) | brake_mode_spinner.getSelectedItemPosition()),
                        (byte) (deadzone_seeker.getProgress()),
                        (byte) (LEDnum),
                        (byte) checks,
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(LightsConfigActivity.this, "Couldnt write Light Config\nPlease try again", Toast.LENGTH_SHORT).show();
                } else {
                    txbuf = new byte[] {
                            (byte) 0x0A5,
                            (byte) 0x000,
                            (byte) 0x0A1,
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
                    Toast.makeText(LightsConfigActivity.this, "Response Timed Out\nPlease try again", Toast.LENGTH_SHORT).show();
                    CHECK_DATA = false;
                }
                else if(data.length == 5) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x75:
                                if(i+4 >= data.length)
                                    break;
                                //Toast.makeText(OrientationActivity.this, "Con: "+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                if(CHECK_DATA) {

                                    int LEDnum = 0;
                                    try {
                                        LEDnum = Integer.parseInt(LED_num_edittext.getText().toString());
                                    } catch(NumberFormatException nfe) {
                                        Toast.makeText(mBluetoothService, "LED number invalid", Toast.LENGTH_SHORT).show();
                                        //System.out.println("Could not parse " + nfe);
                                    }

                                    byte checks = (byte)((byte)0xFF & (byte)(RGB_sync_checkbox.isChecked() ? 1 : 0) << 7);
                                    checks = (byte)(checks | ((byte)((byte)0xFF & (byte)(RGB_sync_checkbox.isChecked() ? 1 : 0) << 6)));

                                    boolean dataCorrect = true;
                                    if(RGB_type_spinner.getSelectedItemPosition() != ((data[i + 1] & 0xF0) >> 4)) dataCorrect = false;
                                    if(brake_mode_spinner.getSelectedItemPosition() != (data[i + 1] & 0x0F)) dataCorrect = false;
                                    if(deadzone_seeker.getProgress() != (data[i + 2] & 0xFF)) dataCorrect = false;
                                    if(LEDnum != (data[i + 3] & 0xFF)) dataCorrect = false;
                                    if(RGB_sync_checkbox.isChecked() && (data[i + 4] & 0x80) != 0x80) dataCorrect = false;
                                    if(brake_always_on_checkbox.isChecked() && (data[i + 4] & 0x40) != 0x40) dataCorrect = false;
                                    if(default_state_checkbox.isChecked() && (data[i + 4] & 0x20) != 0x20) dataCorrect = false;
                                    if(dataCorrect){
                                        Toast.makeText(LightsConfigActivity.this, "Lights config applied successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(LightsConfigActivity.this, "Lights config failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                                    }
                                    CHECK_DATA = false;
                                } else {
                                    if((data[i + 1] & 0xF0) >> 4 != RGB_type_spinner.getSelectedItemPosition())
                                        IGNORE_SPINNER_CHANGE = true;
                                    RGB_type_spinner.setSelection((data[i + 1] & 0xF0) >> 4);
                                    brake_mode_spinner.setSelection(data[i + 1] & 0x0F);
                                    deadzone_seeker.setProgress(data[i + 2] & 0xFF);
                                    LED_num_edittext.setText(Integer.toString((data[i+3] & 0xFF)));
                                    RGB_sync_checkbox.setChecked((data[i+4] & 0x80) == 0x80);
                                    brake_always_on_checkbox.setChecked((data[i+4] & 0x40) == 0x40);
                                    default_state_checkbox.setChecked((data[i+4] & 0x20) == 0x20);
                                }
                                i+=4;
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
        if(parent_id == R.id.side_LED_type_spinner){
            if(IGNORE_SPINNER_CHANGE)
                IGNORE_SPINNER_CHANGE = false;
            else
                Toast.makeText(mBluetoothService, "The board needs to be restarted if a different side LED type is configured", Toast.LENGTH_LONG).show();

            TextView LEDcountText = findViewById(R.id.LED_num_text);
            EditText LEDcountEditText = findViewById(R.id.LED_num_edittext);
            if(parent.getSelectedItemPosition() == 0){
                LEDcountText.setEnabled(false);
                LEDcountEditText.setEnabled(false);
                LEDcountText.setTextColor(Color.GRAY);
                LEDcountEditText.setTextColor(Color.GRAY);
            } else{
                LEDcountText.setEnabled(true);
                LEDcountEditText.setEnabled(true);
                LEDcountText.setTextColor(Color.WHITE);
                LEDcountEditText.setTextColor(Color.WHITE);
            }
        }
        //String selectedItem = parent.getSelectedItem().toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent){
    }

    void initializeRGBTypeList(List<CharSequence> list){
        list.clear();
        list.add("Analog");
        list.add("Digital (APA102)");
        list.add("Digital (SK9822)");
        list.add("None");
    }

    void initializeBrakeModeAdapter(List<CharSequence> list){
        list.clear();
        list.add("Fade");
        list.add("Blink");
        list.add("Fade-Blink");
        list.add("Blink-Fade");
        list.add("Fading Blink");
        list.add("Paced Blink");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView deadzone_number = (TextView) findViewById(R.id.brake_deadzone_number);

        if (seekBar.getId() == R.id.brake_deadzone_seeker) {
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
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        savesettings();
        unregisterReceiver(mGattUpdateReceiver);
    }
}

