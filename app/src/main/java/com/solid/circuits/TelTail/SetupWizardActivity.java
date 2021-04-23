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
import android.os.Environment;
import android.os.IBinder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

public class SetupWizardActivity extends FragmentActivity {
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 13;

    public static final String PREFS_NAME = "TTLPrefsFile";

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private CustomViewPager mPager;

    FileOutputStream fos = null;
    File ExportFile = null;

    class TTLWritePacket
    {
        public TTLWritePacket(byte id, byte dataLength, byte[] data) {
            this.id = id;
            this.dataLength = dataLength;
            this.data = data;
        }
        public byte id;
        public byte dataLength;
        public byte[] data;
    };

    TTLWritePacket analog_cycle_values_write_packet = new TTLWritePacket((byte) 0x0EC,(byte) 0x004, new byte[4]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), rate, brightness,(SHUFFFLE[7])
    TTLWritePacket digital_skittles_values_write_packet = new TTLWritePacket((byte) 0x0BA,(byte) 0x003, new byte[3]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), bright, (SHUFFFLE[7])
    TTLWritePacket lights_config_write_packet = new TTLWritePacket((byte) 0x0C5,(byte) 0x005, new byte[5]); // (RGB type[4], brake mode), deadzone, LEDnum, (sync RGB[7], brake always on[6], default state[5],highbeams[4], standby[3], shuffle[2]), low beam level
    TTLWritePacket remote_config_write_packet = new TTLWritePacket((byte) 0x0C3,(byte) 0x003, new byte[3]); // remote type, button type, deadzone
    TTLWritePacket controls_config_write_packet = new TTLWritePacket((byte) 0x0C2, (byte) 0x006, new byte[6]); // (aux check[7], turncheck[6], aux type), aux time, (aux[4], toggle all), (toggle head[4], toggle side), (mode down[4], mode up), bright
    TTLWritePacket orientation_config_write_packet = new TTLWritePacket((byte) 0x0FD, (byte) 0x002, new byte[2]); // coonector, power

    String[][] settings_export_list = {
            {"Connect BLE on Startup", "bleConnect", "bool"},
            {"Enable Logging", "LogEnable", "bool"},
            {"Max Log Size", "LogSize", "int"},
            {"Read Modes on Start", "ReadCurrentLED", "bool"},
            {"Display Notification", "DispNotif", "bool"},
            {"Standby Enabled", "StandbyEnable", "bool"},
            {"Shuffle Enabled", "ShuffleEnable", "bool"},
            {"RGB Type", "RGBType", "int"},
            {"Sync RGB Strips", "SyncSide", "bool"},
            {"Addressable LED Number", "LEDnum", "string"},
            {"Brake Mode", "BrakeMode", "int"},
            {"Brakes Always On", "BrakeAlwaysOn", "bool"},
            {"Brake Deadzone", "Deadzone", "int"},
            {"Default State", "DefaultState", "bool"},
            {"High Beams Enabled", "HighbeamEnable", "bool"},
            {"Low-Beam Level", "LowbeamLevel", "int"},
            {"Remote Type", "RemoteType", "int"},
            {"Button Sig Type", "ButtonType", "int"},
            {"Horn Enabled", "AuxEnable", "bool"},
            {"Horn Output Type", "AuxType", "int"},
            {"Horn Time", "AuxTime", "int"},
            {"Activate Horn Control", "HornCtrl", "int"},
            {"Toggle Brights Control", "BrightsCtrl", "int"},
            {"Toggle All Control", "AllCtrl", "int"},
            {"Toggle Head Control", "HeadCtrl", "int"},
            {"Toggle Side Control", "SideCtrl", "int"},
            {"Mode Up Control", "ModeUpCtrl", "int"},
            {"Mode Down Control", "ModeDownCtrl", "int"},
            {"Connector Orientation", "ConnectorOrient", "int"},
            {"Power Orientation", "PowerOrient", "int"},
            {"LED Mode", "LEDMode", "int"},
            {"Head/Brake Enabled", "HeadSwitch", "bool"},
            {"Side Enabled", "SideSwitch", "bool"},
            {"Light Sensor Enabled", "LightSwitch", "bool"},
            {"Auto On/Off Enabled", "SensSwitch", "bool"}
    };

    String ConnectorOrientationValue;
    String PowerOrientationValue;
    int RemoteSelection;
    String LEDStripValue;
    String BrakeModeValue;
    int WriteActionSelection;
    String RemoteTypeValue;
    String ButtonTypeValue;

    String ShuffleValue = "true";
    String StandbyValue = "false";
    String SyncValue = "true";
    String InitialStateValue = "false";
    String AlwaysOnBrakeValue = "false";
    String AutoConnectValue = "false";
    String AutoReadValue = "false";
    String DispNotifValue = "false";

    int toggle_all_action = 0;
    int mode_up_action = 0;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter pagerAdapter;

    boolean SEND_VALUES = false;
    long BLE_WRITE_TIMEOUT = 1000;
    private BluetoothService mBluetoothService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_wizard_main);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setPagingEnabled(false);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                if (SEND_VALUES) {
                    send_settings_to_TTL();
                    SEND_VALUES = false;
                }
            }
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        return intentFilter;
    }

    @Override
    protected  void onResume(){
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

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    return new SetupWizardIntro();
                case 1:
                    return new SetupWizardConnectorsOrientation();
                case 2:
                    return new SetupWizardPowerOrientation();
                case 3:
                    return new SetupWizardRemote();
                case 4:
                    return new SetupWizardLEDstrip();
                case 5:
                    return new SetupWizardShuffleModes();
                case 6:
                    return new SetupWizardStandbyMode();
                case 7:
                    return new SetupWizardSyncLights();
                case 8:
                    return new SetupWizardBrakeMode();
                case 9:
                    return new SetupWizardInitialState();
                case 10:
                    return new SetupWizardControls();
                case 11:
                    return new SetupWizardAppSettings();
                case 12:
                    return new SetupWizardWriteSave();
            }
            return new SetupWizardIntro();
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }


    public void onCheckboxClicked(View view) {
        final CheckBox checkbox = (CheckBox) findViewById(view.getId());

        switch (view.getId()) {
            case R.id.wizard_shuffle_check:
                ShuffleValue = ""+checkbox.isChecked();
                break;
            case R.id.wizard_standby_enable_check:
                StandbyValue = ""+checkbox.isChecked();
                break;
            case R.id.wizard_sync_check:
                SyncValue = ""+checkbox.isChecked();
                break;
            case R.id.wizard_brake_always_on_check:
                AlwaysOnBrakeValue = ""+checkbox.isChecked();
                break;
            case R.id.wizard_app_initial_check:
                InitialStateValue = ""+checkbox.isChecked();
                break;
            case R.id.wizard_app_notification_check:
                DispNotifValue = ""+checkbox.isChecked();
                break;
            case R.id.wizard_app_auto_ble_check:
                AutoConnectValue = ""+checkbox.isChecked();
                break;
            case R.id.wizard_app_read_LED_check:
                AutoReadValue = ""+checkbox.isChecked();
                break;
        }
    }

    public void onButtonClick(View view) {
        switch (view.getId()) {
            case R.id.wizard_intro_quit_button:
            case R.id.wizard_connector_orientation_back_button:
            case R.id.wizard_power_orientation_back_button:
            case R.id.wizard_brake_back_button:
            case R.id.wizard_controls_back_button:
            case R.id.wizard_app_back_button:
            case R.id.wizard_horn_back_button:
            case R.id.wizard_initial_state_back_button:
            case R.id.wizard_led_strip_back_button:
            case R.id.wizard_remote_back_button:
            case R.id.wizard_shuffle_back_button:
            case R.id.wizard_sync_back_button:
            case R.id.wizard_standby_back_button:
            case R.id.wizard_write_back_button:
                this.onBackPressed();
                break;
            case R.id.wizard_power_orientation_next_button:
                if(ConnectorOrientationValue.equals(PowerOrientationValue) || AreParallel(ConnectorOrientationValue, PowerOrientationValue)){
                    Toast.makeText(this, "Orientation of power cannot be the same as or parallel with the connectors.\n\nPlease select a different orientation", Toast.LENGTH_LONG).show();
                } else{
                    mPager.setCurrentItem(mPager.getCurrentItem()+1);
                }
                break;
            case R.id.wizard_remote_next_button:
                if(false){

                } else {
                    mPager.setCurrentItem(mPager.getCurrentItem()+1);
                }
                break;
            case R.id.wizard_connector_orientation_next_button:
            case R.id.wizard_controls_next_button:
            case R.id.wizard_intro_begin_button:
            case R.id.wizard_brake_next_button:
            case R.id.wizard_app_next_button:
            case R.id.wizard_horn_next_button:
            case R.id.wizard_initial_state_next_button:
            case R.id.wizard_led_strip_next_button:
            case R.id.wizard_shuffle_next_button:
            case R.id.wizard_sync_next_button:
            case R.id.wizard_standby_next_button:
                mPager.setCurrentItem(mPager.getCurrentItem()+1);
                break;
            case R.id.wizard_write_finish_button:
                save_all_settings();
                if(WriteActionSelection == 0){
                    write_settings();
                } else if(WriteActionSelection == 1){
                    creatNewExportFile();
                    export_settings(settings_export_list);
                    closeExportFile();
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("runWizard", false);
                editor.commit();
                    this.finish();
                } else {
                    creatNewExportFile();
                    export_settings(settings_export_list);
                    closeExportFile();
                    write_settings();
                }
                break;
        }
    }

    public void SpinnerItemSelected(int view_id){
        switch (view_id){
            case R.id.wizard_remote_spinner: {
                switch(RemoteSelection){
                    // PWM + Mom Btn
                    case 1://Enertion
                    case 2://TB Nano
                        RemoteTypeValue = "0";
                        ButtonTypeValue = "1";
                        break;
                    // PWM + PWM Latching Btn
                    case 6://GT2B
                    case 7://APS
                        RemoteTypeValue = "0";
                        ButtonTypeValue = "3";
                        break;
                    // PWM w/o BTN
                    case 0://Hoyt
                    case 3://TB Mini
                    case 4://VX1
                    case 5://VX3
                    case 8://Maytech
                    case 9://Maytech waterproof
                        RemoteTypeValue = "0";
                        ButtonTypeValue = "6";
                        break;
                    case 10://Other
                        RemoteTypeValue = "0";
                        ButtonTypeValue = "0";
                        break;
                }
                break;
            }
        }
    }

    public void UpdateControlsText(TextView textview){
        if(ButtonTypeValue == "1" || ButtonTypeValue == "2" || ButtonTypeValue == "3") {
            toggle_all_action = 1;
            mode_up_action = 2;
            textview.setText("Based on the remote you are using, the following control scheme has been configured:\n\nToggle All Lights: Tap aux button once\nChange LED Mode: Tap aux button twice\n\n\n\nIt is recommended that you test these controls out after finishing the wizard to verify everything was setup correctly.\n\nIf you would like to alter this configuration, navigate to the \"Configure Control Actions\" menu in settings after finishing the wizard.");
        } else if(ButtonTypeValue == "6") {
            toggle_all_action = 1;
            mode_up_action = 2;
            textview.setText("Based on the remote you are using, the following control scheme has been configured:\n\nToggle All Lights: Flick throttle towards brake once\nChange LED Mode: Flick throttle towards brake twice\n\n\n\nIt is recommended that you test these controls out after finishing the wizard to verify everything was setup correctly.\n\nIf you would like to alter this configuration, navigate to the \"Configure Control Actions\" menu in settings after finishing the wizard.");
        } else if(ButtonTypeValue == "7") {
            toggle_all_action = 1;
            mode_up_action = 2;
            textview.setText("Based on the remote you are using, the following control scheme has been configured:\n\nToggle All Lights: Flick throttle towards accelerate once\nChange LED Mode: Flick throttle towards accelerate twice\n\n\n\nIt is recommended that you test these controls out after finishing the wizard to verify everything was setup correctly.\n\nIf you would like to alter this configuration, navigate to the \"Configure Control Actions\" menu in settings after finishing the wizard.");
        } else {
            toggle_all_action = 0;
            mode_up_action = 0;
            textview.setText("\n\n\nDue to a lack of inputs sources, the remote you are using will not be able to control the TTL system");
        }
    }

    void export_settings(String[][] settings_list) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        for(int i = 0; i < settings_list.length; i++){
            writeStringToLog(settings_list[i][0] + ",");
            if(settings_list[i][2].equals("int")){
                writeStringToLog(""+settings.getInt(settings_list[i][1],0)+"\n");
            } else if(settings_list[i][2].equals("bool")){
                writeStringToLog(""+settings.getBoolean(settings_list[i][1],false)+"\n");
            } else if(settings_list[i][2].equals("string")){
                writeStringToLog(""+settings.getString(settings_list[i][1],"DEFAULT")+"\n");
            } else if(settings_list[i][2].equals("float")){
                writeStringToLog(""+settings.getFloat(settings_list[i][1],0)+"\n");
            }
        }
    }

    void creatNewExportFile(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File dir = new File(filepath+"/TelTail");
        if (!dir.exists()) {
            if(!dir.mkdirs()){
                Toast.makeText(SetupWizardActivity.this, "Failed to make TTL dir", Toast.LENGTH_SHORT).show();
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH-mm-ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        ExportFile = new File(dir, "Settings Export " + sdf.format(new Date()) + ".txt");

        //Toast.makeText(SetupWizardActivity.this, filepath, Toast.LENGTH_SHORT).show();
        try {
            fos = new FileOutputStream(ExportFile);
        } catch (Exception e){
            Toast.makeText(SetupWizardActivity.this, "Failed to open export file", Toast.LENGTH_SHORT).show();
        }
    }

    void closeExportFile(){
        try {
            fos.close();
        } catch (Exception e) {
            Toast.makeText(SetupWizardActivity.this, "Failed to close export", Toast.LENGTH_SHORT).show();
        }

        try {
            if (ExportFile.length() == 0){
                ExportFile.delete();
            }else{
                Toast.makeText(SetupWizardActivity.this, "Exported", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e){
            Toast.makeText(SetupWizardActivity.this, "Failed to delete empty export file", Toast.LENGTH_SHORT).show();
        }
        fos = null;
    }

    public void writeStringToLog(String text){
        try {
            fos.write(text.getBytes());
        } catch (Exception e){
            Toast.makeText(SetupWizardActivity.this, "Failed write export", Toast.LENGTH_SHORT).show();
        }
    }

        void save_all_settings(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        save_setting(AutoConnectValue, settings_export_list[0][1], settings_export_list[0][2], editor); //{"Connect BLE on Startup", "bleConnect", "bool"},
        save_setting("false", settings_export_list[1][1], settings_export_list[1][2], editor); //{"Enable Logging", "LogEnable", "bool"},
        save_setting("100", settings_export_list[2][1], settings_export_list[2][2], editor); //{"Max Log Size", "LogSize", "int"},
        save_setting(AutoReadValue, settings_export_list[3][1], settings_export_list[3][2], editor); //{"Read Modes on Start", "ReadCurrentLED", "bool"},
        save_setting(DispNotifValue, settings_export_list[4][1], settings_export_list[4][2], editor); //{"Display Notification", "DispNotif", "bool"},
        save_setting(StandbyValue, settings_export_list[5][1], settings_export_list[5][2], editor); //{"Standby Enabled", "StandbyEnable", "bool"},
        save_setting(ShuffleValue, settings_export_list[6][1], settings_export_list[6][2], editor); //{"Shuffle Enabled", "ShuffleEnable", "bool"},
        save_setting(LEDStripValue, settings_export_list[7][1], settings_export_list[7][2], editor); //{"RGB Type", "RGBType", "int"},
        save_setting(SyncValue, settings_export_list[8][1], settings_export_list[8][2], editor); //{"Sync RGB Strips", "SyncSide", "bool"},
        save_setting("30", settings_export_list[9][1], settings_export_list[9][2], editor); //{"Addressable LED Number", "LEDnum", "string"},
        save_setting(BrakeModeValue, settings_export_list[10][1], settings_export_list[10][2], editor); //{"Brake Mode", "BrakeMode", "int"},
        save_setting(AlwaysOnBrakeValue, settings_export_list[11][1], settings_export_list[11][2], editor); //{"Brakes Always On", "BrakeAlwaysOn", "bool"},
        save_setting("20", settings_export_list[12][1], settings_export_list[12][2], editor); //{"Brake Deadzone", "Deadzone", "int"},
        save_setting(InitialStateValue, settings_export_list[13][1], settings_export_list[13][2], editor); //{"Default State", "DefaultState", "bool"},
        save_setting("false", settings_export_list[14][1], settings_export_list[14][2], editor); //{"High Beams Enabled", "HighbeamEnable", "bool"},
        save_setting("50", settings_export_list[15][1], settings_export_list[15][2], editor); //{"Low-Beam Level", "LowbeamLevel", "int"},
        save_setting(RemoteTypeValue, settings_export_list[16][1], settings_export_list[16][2], editor); //{"Remote Type", "RemoteType", "int"},
        save_setting(ButtonTypeValue, settings_export_list[17][1], settings_export_list[17][2], editor); //{"Button Sig Type", "ButtonType", "int"},
        save_setting("false", settings_export_list[18][1], settings_export_list[18][2], editor); //{"Horn Enabled", "AuxEnable", "bool"},
        save_setting("1", settings_export_list[19][1], settings_export_list[19][2], editor); //{"Horn Output Type", "AuxType", "int"},
        save_setting("10", settings_export_list[20][1], settings_export_list[20][2], editor); //{"Horn Time", "AuxTime", "int"},
        save_setting("0", settings_export_list[21][1], settings_export_list[21][2], editor); //{"Activate Horn Control", "HornCtrl", "int"},
        save_setting("0", settings_export_list[22][1], settings_export_list[22][2], editor); //{"Toggle Brights Control", "BrightsCtrl", "int"},
        save_setting("1", settings_export_list[23][1], settings_export_list[23][2], editor); //{"Toggle All Control", "AllCtrl", "int"},
        save_setting("0", settings_export_list[24][1], settings_export_list[24][2], editor); //{"Toggle Head Control", "HeadCtrl", "int"},
        save_setting("0", settings_export_list[25][1], settings_export_list[25][2], editor); //{"Toggle Side Control", "SideCtrl", "int"},
        save_setting("2", settings_export_list[26][1], settings_export_list[26][2], editor); //{"Mode Up Control", "ModeUpCtrl", "int"},
        save_setting("0", settings_export_list[27][1], settings_export_list[27][2], editor); //{"Mode Down Control", "ModeDownCtrl", "int"},
        save_setting(ConnectorOrientationValue, settings_export_list[28][1], settings_export_list[28][2], editor); //{"Connector Orientation", "ConnectorOrient", "int"},
        save_setting(PowerOrientationValue, settings_export_list[29][1], settings_export_list[29][2], editor); //{"Power Orientation", "PowerOrient", "int"},
        save_setting("1", settings_export_list[30][1], settings_export_list[30][2], editor); //{"LED Mode", "LEDMode", "int"},
        save_setting("true", settings_export_list[31][1], settings_export_list[31][2], editor); //{"Head/Brake Enabled", "HeadSwitch", "bool"},
        save_setting("true", settings_export_list[32][1], settings_export_list[32][2], editor); //{"Side Enabled", "SideSwitch", "bool"},
        save_setting("false", settings_export_list[33][1], settings_export_list[33][2], editor); //{"Light Sensor Enabled", "LightSwitch", "bool"},
        save_setting("false", settings_export_list[34][1], settings_export_list[34][2], editor); //{"Auto On/Off Enabled", "SensSwitch", "bool"}
        editor.commit();
    }

    void save_setting(String value, String setting_name, String setting_type, SharedPreferences.Editor editor){
        if(setting_type.equals("int")){
            editor.putInt(setting_name,Integer.parseInt(value));
        } else if(setting_type.equals("bool")){
            editor.putBoolean(setting_name,Boolean.parseBoolean(value));
        } else if(setting_type.equals("string")){
            editor.putString(setting_name, value);
        } else if(setting_type.equals("float")){
            editor.putFloat(setting_name, Float.parseFloat(value));
        }
    }

    final int BLE_SCAN_REQUEST = 100;
    void write_settings(){
        //check if connected to TTL
        if(mBluetoothService.mConnectionState != mBluetoothService.STATE_CONNECTED) {
            //If not, check a BLE device is selected
            if(mBluetoothService.mBluetoothDeviceAddress == null) {
                //If not, have user select one
                new AlertDialog.Builder(this)
                        .setMessage("To write the settings, you will need to connect to the TTL system.\n\nMake sure the system is power before continuing.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(SetupWizardActivity.this, BluetoothScan.class);
                                startActivityForResult(intent, BLE_SCAN_REQUEST);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setIcon(R.mipmap.ic_ble_black)
                        .setTitle("Connect Bluetooth")
                        .setCancelable(false)
                        .show();
            } else {
                SEND_VALUES = true;
                mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
            }
        } else {
            send_settings_to_TTL(); //Send all settings
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case BLE_SCAN_REQUEST:
                mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                SEND_VALUES = true;
                break;
        }
    }

    void send_settings_to_TTL(){
        TTLWritePacket temp_packet = null;

        //Send the values for the analog cycle mode
        // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), rate, brightness,(SHUFFFLE[7])
        byte switches_led_type = 0;
        switches_led_type = (byte) (0 & 0x0F); //RGB LED Type = Analog
        switches_led_type = (byte) (switches_led_type | ((byte) 1 << 4)); //Side lights enable = true
        switches_led_type = (byte) (switches_led_type | ((byte) 1 << 5)); //head lights enable = true
        switches_led_type = (byte) (switches_led_type | ((byte) 0 << 6)); //light control enable = false
        switches_led_type = (byte) (switches_led_type | ((byte) 0 << 7)); //sensor control enable = false
        temp_packet = analog_cycle_values_write_packet;
        temp_packet.data[0] = switches_led_type;
        temp_packet.data[1] = (byte)50;
        temp_packet.data[2] = (byte)50;
        temp_packet.data[3] = (byte)(1 << 7);// Shuffle = true;
        write_ttl_packet(temp_packet);

        // Send the values for the digital skittles mode
        // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), bright, (SHUFFFLE[7])
        switches_led_type = 0;
        switches_led_type = (byte) (1 & 0x0F); //RGB LED Type = Digital
        switches_led_type = (byte) (switches_led_type | ((byte) 1 << 4)); //Side lights enable = true
        switches_led_type = (byte) (switches_led_type | ((byte) 1 << 5)); //head lights enable = true
        switches_led_type = (byte) (switches_led_type | ((byte) 0 << 6)); //light control enable = false
        switches_led_type = (byte) (switches_led_type | ((byte) 0 << 7)); //sensor control enable = false
        temp_packet = digital_skittles_values_write_packet;
        temp_packet.data[0] = switches_led_type;
        temp_packet.data[1] = (byte)50;
        temp_packet.data[2] = (byte)(1 << 7);// Shuffle = true;
        write_ttl_packet(temp_packet);

        // send remote config settings
        // remote type, button type, deadzone
        temp_packet = remote_config_write_packet;
        temp_packet.data[0] = (byte) ((Integer.parseInt(RemoteTypeValue) << 4) |
                (Integer.parseInt(ButtonTypeValue)));
        temp_packet.data[1] = (byte) 20;
        write_ttl_packet(temp_packet);

        // send controls config settings
        // (aux check[7], aux type), aux time, (aux[4], toggle all), (toggle head[4], toggle side), (mode down[4], mode up), bright
        temp_packet = controls_config_write_packet;
        temp_packet.data[0] = (byte) ((0 << 7) | 1);
        temp_packet.data[1] = (byte) 10;
        temp_packet.data[2] = (byte) ((0 << 4) | toggle_all_action);
        temp_packet.data[3] = (byte) 0;
        temp_packet.data[4] = (byte) mode_up_action;
        temp_packet.data[5] = (byte) 0;
        write_ttl_packet(temp_packet);

         // send orientation settings
        // coonector, power
        temp_packet = orientation_config_write_packet;
        temp_packet.data[0] = (byte) Integer.parseInt(ConnectorOrientationValue);
        temp_packet.data[1] = (byte) Integer.parseInt(PowerOrientationValue);
        write_ttl_packet(temp_packet);

        // send light config settings
        // (RGB type[4], brake mode), deadzone, LEDnum, (sync RGB[7], brake always on[6], default state[5],highbeams[4], standby[3], shuffle[2]), low beam level
        temp_packet = lights_config_write_packet;
        temp_packet.data[0] = (byte) ((Integer.parseInt(LEDStripValue) << 4)| Integer.parseInt(BrakeModeValue));
        temp_packet.data[1] = (byte) 20;
        temp_packet.data[2] = (byte) 30;
        temp_packet.data[3] = (byte) (((Boolean.parseBoolean(SyncValue) ? 1 : 0) << 7) |
                ((Boolean.parseBoolean(AlwaysOnBrakeValue) ? 1 : 0) << 6) |
                ((Boolean.parseBoolean(InitialStateValue) ? 1 : 0) << 5) |
                (0 << 4) |
                ((Boolean.parseBoolean(StandbyValue) ? 1 : 0) << 3) |
                ((Boolean.parseBoolean(ShuffleValue) ? 1 : 0) << 2));
        temp_packet.data[4] = (byte) 50;
        write_ttl_packet(temp_packet);

        Toast.makeText(SetupWizardActivity.this, "Settings Written to TTL", Toast.LENGTH_SHORT).show();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("runWizard", false);
        editor.commit();
        this.finish();
    }

    boolean write_ttl_packet(TTLWritePacket packet){
        if(packet != null) {
            long send_timer = System.currentTimeMillis();
            final int packet_size = 4 + packet.data.length;
            byte[] txbuf = new byte[packet_size];
            txbuf[0] = (byte) 0x0A5;
            txbuf[1] = (byte) (packet.data.length);
            txbuf[2] = packet.id;
            for (int i = 0; i < packet.data.length; i++) {
                txbuf[3 + i] = packet.data[i];
            }
            txbuf[3 + packet.data.length] = (byte) 0x05A;
            while (!mBluetoothService.writeBytes(txbuf)) {
                if (System.currentTimeMillis() - send_timer > BLE_WRITE_TIMEOUT) {
                    Toast.makeText(SetupWizardActivity.this, "Failed to write settings", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    Boolean AreParallel(String val1, String val2){
        if(((val1.equals("1") || val1.equals("2"))&&(val2.equals("1") || val2.equals("2"))) ||
           ((val1.equals("3") || val1.equals("4"))&&(val2.equals("3") || val2.equals("4"))) ||
           ((val1.equals("5") || val1.equals("6"))&&(val2.equals("5") || val2.equals("6")))){
            return true;
        } else {
            return false;
        }
    }
}