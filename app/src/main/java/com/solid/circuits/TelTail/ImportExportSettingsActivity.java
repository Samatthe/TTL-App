package com.solid.circuits.TelTail;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class ImportExportSettingsActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "MyPrefsFile";

    private static final int MY_REQUEST_IMPORT_PERMISSION = 1000;
    private static final int MY_RESULT_IMPORT_FILECHOOSER = 2000;

    long BLE_WRITE_TIMEOUT = 1000;

    FileOutputStream fos = null;
    FileInputStream fis;
    File ExportFile = null;
    String ImportPath = null;

    CheckBox write_on_import_check;
    CheckBox read_on_export_check;
    Button ImportExportLEDsButton;
    Button ImportExportRemoteButton;
    Button ImportExporControlsButton;
    Button ImportExportOrientationButton;
    Button ImportExportAppButton;
    Button ImportExportTTLButton;
    Button ImportExportAllButton;

    boolean LED_MODE_VALUES_READ = false;
    boolean LIGHTS_CONFIG_VALUES_READ = false;
    boolean REMOTE_VALUES_READ = false;
    boolean CONTROLS_VALUES_READ = false;
    boolean ORIENTATION_VALUES_READ = false;

    boolean LED_MODE_VALUES_WRITTEN = false;
    boolean LIGHTS_CONFIG_VALUES_WRITTEN = false;
    boolean REMOTE_VALUES_WRITTEN = false;
    boolean CONTROLS_VALUES_WRITTEN = false;
    boolean ORIENTATION_VALUES_WRITTEN = false;

    boolean IMPORT_FILE_CHOSEN = false;
    int FileResult = 0;
    List<String[][]> import_list = new ArrayList<String[][]>();
    List<String[][]> TTL_Read_List = new ArrayList<String[][]>();

    byte[] read_values_packet = {
            (byte) 0x0A5,
            (byte) 0x000,
            (byte) 0x000,
            (byte) 0x05A
    };
    byte read_led_vars_byte = (byte)0x0CD;
    byte read_lights_config_byte = (byte)0x0A1;
    byte read_controls_config_byte =  (byte)0x0FC;
    byte read_remote_config_byte =  (byte)0x0FB;
    byte read_orientation_config_byte = (byte)0x0FE;
    byte led_mode_down_byte = (byte)0x0E2;

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

    TTLWritePacket analog_static_values_write_packet = new TTLWritePacket((byte) 0x0ED,(byte) 0x008, new byte[8]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), LG, LB, RR, RG, RB, (SHUFFFLE[7])
    TTLWritePacket analog_cycle_values_write_packet = new TTLWritePacket((byte) 0x0EC,(byte) 0x004, new byte[4]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), rate, brightness,(SHUFFFLE[7])
    TTLWritePacket analog_compass_values_write_packet = new TTLWritePacket((byte) 0x0EB,(byte) 0x003, new byte[3]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), sensitivity, (SHUFFFLE[7])
    TTLWritePacket analog_throttle_values_write_packet = new TTLWritePacket((byte) 0x0EA,(byte) 0x004, new byte[4]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), sens, bright, (SHUFFFLE[7])
    TTLWritePacket analog_rpm_values_write_packet = new TTLWritePacket((byte) 0x0E9,(byte) 0x003, new byte[3]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), rate, (SHUFFFLE[7])
    TTLWritePacket analog_rpm_throttle_values_write_packet = new TTLWritePacket((byte) 0x0E8,(byte) 0x002, new byte[2]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), (SHUFFFLE[7])
    TTLWritePacket analog_x_accel_values_write_packet = new TTLWritePacket((byte) 0x0E7,(byte) 0x003, new byte[3]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), sens, (SHUFFFLE[7])
    TTLWritePacket analog_y_accel_values_write_packet = new TTLWritePacket((byte) 0x0E6,(byte) 0x003, new byte[3]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), sense, (SHUFFFLE[7])
    TTLWritePacket analog_custom_values_write_packet = new TTLWritePacket((byte) 0x0B1,(byte) 0x00C, new byte[12]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), color, (rate[4], bright base), LR, LG, LB, RR, RG, RB, rate, bright, (SHUFFFLE[7])
    TTLWritePacket digital_static_values_write_packet = new TTLWritePacket((byte) 0x0B9,(byte) 0x005, new byte[5]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), zoom, shift, bright, (SHUFFFLE[7])
    TTLWritePacket digital_skittles_values_write_packet = new TTLWritePacket((byte) 0x0BA,(byte) 0x003, new byte[3]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), bright, (SHUFFFLE[7])
    TTLWritePacket digital_cycle_values_write_packet = new TTLWritePacket((byte) 0x0BB,(byte) 0x005, new byte[5]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), zoom, rate, bright, (SHUFFFLE[7])
    TTLWritePacket digital_compass_values_write_packet = new TTLWritePacket((byte) 0x0BC,(byte) 0x003, new byte[3]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), bright, (SHUFFFLE[7])
    TTLWritePacket digital_throttle_values_write_packet = new TTLWritePacket((byte) 0x0C0,(byte) 0x006, new byte[6]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), zoom, shift, sens, bright, (SHUFFFLE[7])
    TTLWritePacket digital_rpm_values_write_packet = new TTLWritePacket((byte) 0x0BE,(byte) 0x005, new byte[5]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), zoom, rate, bright, (SHUFFFLE[7])
    TTLWritePacket digital_rpm_throttle_values_write_packet = new TTLWritePacket((byte) 0x0BF,(byte) 0x002, new byte[2]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), (SHUFFFLE[7])
    TTLWritePacket digital_compass_wheel_values_write_packet = new TTLWritePacket((byte) 0x0C1,(byte) 0x002, new byte[2]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE), (SHUFFFLE[7])
    TTLWritePacket digital_compass_snake_values_write_packet = new TTLWritePacket((byte) 0x0C6,(byte) 0x002, new byte[2]); // (SENSOR[7], LIGHT[6], HB[5], SIDE[4], rgb TYPE),(SHUFFFLE[7])

    TTLWritePacket lights_config_write_packet = new TTLWritePacket((byte) 0x0C5,(byte) 0x005, new byte[5]); // (RGB type[4], brake mode), deadzone, LEDnum, (sync RGB[7], brake always on[6], default state[5],highbeams[4], standby[3], shuffle[2]), low beam level
    TTLWritePacket remote_config_write_packet = new TTLWritePacket((byte) 0x0C3,(byte) 0x003, new byte[3]); // remote type, button type, deadzone
    TTLWritePacket controls_config_write_packet = new TTLWritePacket((byte) 0x0C2, (byte) 0x006, new byte[6]); // (aux check[7], turncheck[6], aux type), aux time, (aux[4], toggle all), (toggle head[4], toggle side), (mode down[4], mode up), bright
    TTLWritePacket orientation_config_write_packet = new TTLWritePacket((byte) 0x0FD, (byte) 0x002, new byte[2]); // coonector, power

    Spinner ModeSpinner;
    Switch SideSwitch;
    Switch HeadSwitch;
    Switch LightSwitch;
    Switch SensSwitch;
    SeekBar StaticLR;
    SeekBar StaticLG;
    SeekBar StaticLB;
    SeekBar StaticRR;
    SeekBar StaticRG;
    SeekBar StaticRB;
    SeekBar CycleRate;
    SeekBar CycleBright;
    SeekBar CompBright;
    SeekBar ThrottleSens;
    SeekBar ThrottleBright;
    SeekBar rpmRate;
    SeekBar XaccelRate;
    SeekBar YaccelRate;
    Spinner ColorSpin;
    Spinner RateSpin;
    Spinner BrightSpin;
    SeekBar CustomLR;
    SeekBar CustomLG;
    SeekBar CustomLB;
    SeekBar CustomRR;
    SeekBar CustomRG;
    SeekBar CustomRB;
    SeekBar CustomRate;
    SeekBar CustomBright;
    SeekBar DigitalStaticZoom;
    SeekBar DigitalStaticShift;
    SeekBar DigitalStaticBright;
    SeekBar DigitalSkittlesBright;
    SeekBar DigitalCycleZoom;
    SeekBar DigitalCycleRate;
    SeekBar DigitalCycleBright;
    SeekBar DigitalCompassBright;
    SeekBar DigitalThrottleZoom;
    SeekBar DigitalThrottleShift;
    SeekBar DigitalThrottleSens;
    SeekBar DigitalThrottleBright;
    SeekBar DigitalRPMZoom;
    SeekBar DigitalRPMRate;
    SeekBar DigitalRPMBright;

    CheckBox AnalogStaticShuffleCheck;
    CheckBox AnalogCycleShuffleCheck;
    CheckBox AnalogCompassShuffleCheck;
    CheckBox AnalogThrottleShuffleCheck;
    CheckBox AnalogRPMShuffleCheck;
    CheckBox AnalogRPMThrottleShuffleCheck;
    CheckBox AnalogXAccelShuffleCheck;
    CheckBox AnalogYaccelShuffleCheck;
    CheckBox AnalogCustomShuffleCheck;
    CheckBox DigitalStaticShuffleCheck;
    CheckBox DigitalCycleShuffleCheck;
    CheckBox DigitalCompassShuffleCheck;
    CheckBox DigitalThrottleShuffleCheck;
    CheckBox DigitalRPMShuffleCheck;
    CheckBox DigitalRPMThrottleShuffleCheck;
    CheckBox DigitalSkittlesShuffleCheck;
    CheckBox DigitalCompassSnakeShuffleCheck;
    CheckBox DigitalCompassWheelShuffleCheck;

    public enum AnalogModes{
        ANALOG_MODE_STATIC,
        ANALOG_MODE_CYCLE,
        ANALOG_MODE_COMPASS,
        ANALOG_MODE_THROTTLE,
        ANALOG_MODE_RPM,
        //ANALOG_MODE_RPM_THROTTLE, // No settings
        ANALOG_MODE_X_ACCEL,
        ANALOG_MODE_Y_ACCEL,
        ANALOG_MODE_CUSTOM
    }
    enum DigitalModes{
        DIGITAL_MODE_STATIC,
        DIGITAL_MODE_SKITTLES,
        DIGITAL_MODE_CYCLE,
        DIGITAL_MODE_COMPASS,
        DIGITAL_MODE_THROTTLE,
        DIGITAL_MODE_RPM_Cycle
        //DIGITAL_MODE_RPM_THROTTLE, // No settings
        //DIGITAL_MODE__COMPASS_WHEEL, // No settings
        //DIGITAL_MODE_COMPASS_SNAKE, // No settings
    }

    // FORMAT: Param name, preference name, data type (bool, string, float, or int)
    String[][] general_led_mode_settings_list = {
            {"RGB Type", "RGBType", "int"},
            {"LED Mode", "LEDMode", "int"},
            {"Head/Brake Enabled", "HeadSwitch", "bool"},
            {"Side Enabled", "SideSwitch", "bool"},
            {"Light Sensor Enabled", "LightSwitch", "bool"},
            {"Auto On/Off Enabled", "SensSwitch", "bool"}
    };
    String[][] led_mode_shuffle_settings_list = {
            {"Shuffle Enabled", "ShuffleEnable", "bool"},
            {"Analog Static Shuffle", "AnalogStaticShuffleCheck", "bool"},
            {"Analog Cycle Shuffle", "AnalogCycleShuffleCheck", "bool"},
            {"Analog Compass Shuffle", "AnalogCompassShuffleCheck", "bool"},
            {"Analog Throttle Shuffle", "AnalogThrottleShuffleCheck", "bool"},
            {"Analog RPM Shuffle", "AnalogRPMShuffleCheck", "bool"},
            {"Analog RPM_Throttle Shuffle", "AnalogRPMThrottleShuffleCheck", "bool"},
            {"Analog X Accel Shuffle", "AnalogXAccelShuffleCheck", "bool"},
            {"Analog Y Accel Shuffle", "AnalogYaccelShuffleCheck", "bool"},
            {"Analog Custom Shuffle", "AnalogCustomShuffleCheck", "bool"},
            {"Digital Static Shuffle", "DigitalStaticShuffleCheck", "bool"},
            {"Digital Skittles Shuffle", "DigitalSkittlesShuffleCheck", "bool"},
            {"Digital Cycle Shuffle", "DigitalCycleShuffleCheck", "bool"},
            {"Digital Compass Shuffle", "DigitalCompassShuffleCheck", "bool"},
            {"Digital Throttle Shuffle", "DigitalThrottleShuffleCheck", "bool"},
            {"Digital RPM_Cycle Shuffle", "DigitalRPMShuffleCheck", "bool"},
            {"Digital RPM_Throttle Shuffle", "DigitalRPMThrottleShuffleCheck", "bool"},
            {"Digital Compass_Wheel Shuffle", "DigitalCompassWheelShuffleCheck", "bool"},
            {"Digital Compass_Snake Shuffle", "DigitalCompassSnakeShuffleCheck", "bool"}
    };
    String[][][] analog_led_mode_settings_lists = {
            {// Static
                    {"Analog Static LR", "StaticLR", "int"},
                    {"Analog Static LG", "StaticLG", "int"},
                    {"Analog Static LB", "StaticLB", "int"},
                    {"Analog Static RR", "StaticRR", "int"},
                    {"Analog Static RG", "StaticRG", "int"},
                    {"Analog Static RB", "StaticRB", "int"}},

            {// Cycle
                    {"Analog Cycle Rate", "CycleRate", "int"},
                    {"Analog Cycle Brightness", "CycleBright", "int"}},

            {// Compass
                    {"Analog Compass Brightness", "CompBright", "int"}},

            {// Throttle
                    {"Analog Throttle Sensitivity", "ThrottleSens", "int"},
                    {"Analog Throttle Brightness", "ThrottleBright", "int"}},

            {// RPM
                    {"Analog RPM Rate", "rpmRate", "int"}},

            {// X Accel
                    {"Analog X Accel Sensitivity", "XaccelRate", "int"}},

            {// Y Accel
                    {"Analog Y Accel Sensitivity", "YaccelRate", "int"}},

            {// Custom
                    {"Analog Color Base", "ColorSpin", "int"},
                    {"Analog Rate Base", "RateSpin", "int"},
                    {"Analog Brightness Base", "BrightSpin", "int"},
                    {"Analog Custom LR", "CustomLR", "int"},
                    {"Analog Custom LG", "CustomLG", "int"},
                    {"Analog Custom LB", "CustomLB", "int"},
                    {"Analog Custom RR", "CustomRR", "int"},
                    {"Analog Custom RG", "CustomRG", "int"},
                    {"Analog Custom RB", "CustomRB", "int"},
                    {"Analog Custom Rate", "CustomRate", "int"},
                    {"Analog Custom Bright", "CustomBright", "int"}}
    };
    String[][][] digital_led_mode_settings_lists = {
            {// Static
                    {"Digital Static Zoom", "DigitalStaticZoom", "int"},
                    {"Digital Static Shift", "DigitalStaticShift", "int"},
                    {"Digital Static Brightness", "DigitalStaticBright", "int"}},

            {// Skittles
                    {"Digital Skittles Brightness", "DigitalSkittlesBright", "int"}},

            {// Cycle
                    {"Digital Cycle Zoom", "DigitalCycleZoom", "int"},
                    {"Digital Cycle Rate", "DigitalCycleRate", "int"},
                    {"Digital Cycle Brightness", "DigitalCycleBright", "int"}},

            {// Compass
                    {"Digital Compass Brightness", "DigitalCompassBright", "int"}},

            {// Throttle
                    {"Digital Throttle Zoom", "DigitalThrottleZoom", "int"},
                    {"Digital Throttle Shift", "DigitalThrottleShift", "int"},
                    {"Digital Throttle Sensitivity", "DigitalThrottleSens", "int"},
                    {"Digital Throttle Brightness", "DigitalThrottleBright", "int"}},

            {// RPM
                    {"Digital RPM Zoom", "DigitalRPMZoom", "int"},
                    {"Digital RPM Rate", "DigitalRPMRate", "int"},
                    {"Digital RPM Brightness", "DigitalRPMBright", "int"}}
    };
    String[][] app_settings_list = {
            {"Connect BLE on Startup", "bleConnect", "bool"},
            {"Enable Logging", "LogEnable", "bool"},
            {"Max Log Size", "LogSize", "int"},
            {"Read Modes on Start", "ReadCurrentLED", "bool"},
            {"Display Notification", "DispNotif", "bool"}
    };
    String[][] lights_config_settings_list = {
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
            {"Low-Beam Level", "LowbeamLevel", "int"}
    };
    String[][] remote_settings_list = {
        {"Remote Type", "RemoteType", "int"},
        {"Button Sig Type", "ButtonType", "int"},
        {"Brake Deadzone", "Deadzone", "int"}
    };
    String[][] controls_settings_list = {
            {"Horn Enabled", "AuxEnable", "bool"},
            {"Horn Output Type", "AuxType", "int"},
            {"Horn Time", "AuxTime", "int"},
            {"Activate Horn Control", "HornCtrl", "int"},
            {"Toggle Brights Control", "BrightsCtrl", "int"},
            {"Toggle All Control", "AllCtrl", "int"},
            {"Toggle Head Control", "HeadCtrl", "int"},
            {"Toggle Side Control", "SideCtrl", "int"},
            {"Mode Up Control", "ModeUpCtrl", "int"},
            {"Mode Down Control", "ModeDownCtrl", "int"}
    };
    String[][] orientation_settings_list = {
            {"Connector Orientation", "ConnectorOrient", "int"},
            {"Power Orientation", "PowerOrient", "int"}
    };

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
        setContentView(R.layout.activity_import_export_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        write_on_import_check = findViewById(R.id.write_on_import_check);
        read_on_export_check = findViewById(R.id.read_on_export_check);
        ImportExportLEDsButton = findViewById(R.id.import_export_lights_config_settings_button);
        ImportExportRemoteButton = findViewById(R.id.import_export_remote_settings_button);
        ImportExporControlsButton = findViewById(R.id.import_export_control_settings_button);
        ImportExportOrientationButton = findViewById(R.id.import_export_orientation_settings_button);
        ImportExportAppButton = findViewById(R.id.import_export_app_settings_button);
        ImportExportTTLButton = findViewById(R.id.import_export_ttl_settings_button);
        ImportExportAllButton = findViewById(R.id.import_export_all_settings_button);

        ModeSpinner = (Spinner) findViewById(R.id.modes_spinner);
        SideSwitch = (Switch) findViewById(R.id.led_side_switch);
        HeadSwitch = (Switch) findViewById(R.id.led_hb_switch);
        LightSwitch = (Switch) findViewById(R.id.led_light_switch);
        SensSwitch = (Switch) findViewById(R.id.led_sensor_switch);
        StaticLR = (SeekBar) findViewById(R.id.left_red_seeker);
        StaticLG = (SeekBar) findViewById(R.id.left_green_seeker);
        StaticLB = (SeekBar) findViewById(R.id.left_blue_seeker);
        StaticRR = (SeekBar) findViewById(R.id.right_red_seeker);
        StaticRG = (SeekBar) findViewById(R.id.right_green_seeker);
        StaticRB = (SeekBar) findViewById(R.id.right_blue_seeker);
        CycleRate = (SeekBar) findViewById(R.id.cycle_speed_seeker);
        CycleBright = (SeekBar) findViewById(R.id.cycle_bright_seeker);
        CompBright = (SeekBar) findViewById(R.id.compass_bright_seeker);
        ThrottleSens = (SeekBar) findViewById(R.id.throttle_speed_seeker);
        ThrottleBright = (SeekBar) findViewById(R.id.throttle_bright_seeker);
        rpmRate = (SeekBar) findViewById(R.id.rpm_speed_seeker);
        XaccelRate = (SeekBar) findViewById(R.id.x_accel_rate_seeker);
        YaccelRate = (SeekBar) findViewById(R.id.y_accel_speed_seeker);
        ColorSpin = (Spinner) findViewById(R.id.custom_color_select_spinner);
        RateSpin = (Spinner) findViewById(R.id.custom_rate_select_spinner);
        BrightSpin = (Spinner) findViewById(R.id.custom_brightness_select_spinner);
        CustomLR = (SeekBar) findViewById(R.id.custom_left_red_seeker);
        CustomLG = (SeekBar) findViewById(R.id.custom_left_green_seeker);
        CustomLB = (SeekBar) findViewById(R.id.custom_left_blue_seeker);
        CustomRR = (SeekBar) findViewById(R.id.custom_right_red_seeker);
        CustomRG = (SeekBar) findViewById(R.id.custom_right_green_seeker);
        CustomRB = (SeekBar) findViewById(R.id.custom_right_blue_seeker);
        CustomRate = (SeekBar) findViewById(R.id.custom_rate_seeker);
        CustomBright = (SeekBar) findViewById(R.id.custom_brightness_seeker);
        DigitalStaticZoom = (SeekBar) findViewById(R.id.digital_static_zoom_seeker);
        DigitalStaticShift = (SeekBar) findViewById(R.id.digital_static_shift_seeker);
        DigitalStaticBright = (SeekBar) findViewById(R.id.digital_static_bright_seeker);
        DigitalSkittlesBright = (SeekBar) findViewById(R.id.digital_skittles_bright_seeker);
        DigitalCycleZoom = (SeekBar) findViewById(R.id.digital_cycle_zoom_seeker);
        DigitalCycleRate = (SeekBar) findViewById(R.id.digital_cycle_rate_seeker);
        DigitalCycleBright = (SeekBar) findViewById(R.id.digital_cycle_bright_seeker);
        DigitalCompassBright = (SeekBar) findViewById(R.id.digital_compass_bright_seeker);
        DigitalThrottleZoom = (SeekBar) findViewById(R.id.digital_throttle_zoom_seeker);
        DigitalThrottleShift = (SeekBar) findViewById(R.id.digital_throttle_shift_seeker);
        DigitalThrottleSens = (SeekBar) findViewById(R.id.digital_throttle_sens_seeker);
        DigitalThrottleBright = (SeekBar) findViewById(R.id.digital_throttle_bright_seeker);
        DigitalRPMZoom = (SeekBar) findViewById(R.id.digital_rpm_zoom_seeker);
        DigitalRPMRate = (SeekBar) findViewById(R.id.digital_rpm_rate_seeker);
        DigitalRPMBright = (SeekBar) findViewById(R.id.digital_rpm_bright_seeker);

        AnalogStaticShuffleCheck = findViewById(R.id.LED_static_shuffle_check);
        AnalogCycleShuffleCheck = findViewById(R.id.LED_cycle_shuffle_check);
        AnalogCompassShuffleCheck = findViewById(R.id.LED_compass_shuffle_check);
        AnalogThrottleShuffleCheck = findViewById(R.id.LED_throttle_shuffle_check);
        AnalogRPMShuffleCheck = findViewById(R.id.LED_rpm_shuffle_check);
        AnalogRPMThrottleShuffleCheck = findViewById(R.id.LED_rpm_throttle_shuffle_check);
        AnalogXAccelShuffleCheck = findViewById(R.id.LED_x_accel_shuffle_check);
        AnalogYaccelShuffleCheck = findViewById(R.id.LED_y_accel_shuffle_check);
        AnalogCustomShuffleCheck = findViewById(R.id.LED_custom_shuffle_check);
        DigitalStaticShuffleCheck = findViewById(R.id.digital_static_shuffle_check);
        DigitalCycleShuffleCheck = findViewById(R.id.digital_cycle_shuffle_check);
        DigitalCompassShuffleCheck = findViewById(R.id.digital_compass_shuffle_check);
        DigitalThrottleShuffleCheck = findViewById(R.id.digital_throttle_shuffle_check);
        DigitalRPMShuffleCheck = findViewById(R.id.digital_rpm_shuffle_check);
        DigitalRPMThrottleShuffleCheck = findViewById(R.id.digital_rpm_throttle_shuffle_check);
        DigitalSkittlesShuffleCheck = findViewById(R.id.digital_skittles_shuffle_check);
        DigitalCompassSnakeShuffleCheck = findViewById(R.id.digital_compass_snake_shuffle_check);
        DigitalCompassWheelShuffleCheck = findViewById(R.id.digital_compass_wheel_shuffle_check);

        restoresettings();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    void savesettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("WriteOnImport", write_on_import_check.isChecked());
        editor.putBoolean("ReadOnExport", read_on_export_check.isChecked());

        // Commit the edits!
        editor.commit();
    }

    void restoresettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        write_on_import_check.setChecked(settings.getBoolean("WriteOnImport", false));
        read_on_export_check.setChecked(settings.getBoolean("ReadOnExport", false));
    }

    public void onButtonClick(final View view) {
        new AlertDialog.Builder(this)
                .setMessage("Would you like to import or export " + ((Button) view).getText() + "?")
                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (view.getId()) {
                            case R.id.import_export_led_mode_settings_button:
                                for(int i = 0; i < analog_led_mode_settings_lists.length; i++){
                                    import_list.add(analog_led_mode_settings_lists[i]);
                                }
                                for(int i = 0; i < digital_led_mode_settings_lists.length; i++){
                                    import_list.add(digital_led_mode_settings_lists[i]);
                                }
                                import_list.add(led_mode_shuffle_settings_list);
                                import_list.add(general_led_mode_settings_list);
                                break;
                            case R.id.import_export_lights_config_settings_button:
                                import_list.add(lights_config_settings_list);
                                break;
                            case R.id.import_export_remote_settings_button:
                                import_list.add(remote_settings_list);
                                break;
                            case R.id.import_export_control_settings_button:
                                import_list.add(controls_settings_list);
                                break;
                            case R.id.import_export_orientation_settings_button:
                                import_list.add(orientation_settings_list);
                                break;
                            case R.id.import_export_app_settings_button:
                                import_list.add(app_settings_list);
                                break;
                            case R.id.import_export_ttl_settings_button:
                                for(int i = 0; i < analog_led_mode_settings_lists.length; i++){
                                    import_list.add(analog_led_mode_settings_lists[i]);
                                }
                                for(int i = 0; i < digital_led_mode_settings_lists.length; i++){
                                    import_list.add(digital_led_mode_settings_lists[i]);
                                }
                                import_list.add(led_mode_shuffle_settings_list);
                                import_list.add(general_led_mode_settings_list);
                                import_list.add(lights_config_settings_list);
                                import_list.add(remote_settings_list);
                                import_list.add(controls_settings_list);
                                import_list.add(orientation_settings_list);
                                break;
                            case R.id.import_export_all_settings_button:
                                for(int i = 0; i < analog_led_mode_settings_lists.length; i++){
                                    import_list.add(analog_led_mode_settings_lists[i]);
                                }
                                for(int i = 0; i < digital_led_mode_settings_lists.length; i++){
                                    import_list.add(digital_led_mode_settings_lists[i]);
                                }
                                import_list.add(led_mode_shuffle_settings_list);
                                import_list.add(general_led_mode_settings_list);
                                import_list.add(lights_config_settings_list);
                                import_list.add(remote_settings_list);
                                import_list.add(controls_settings_list);
                                import_list.add(orientation_settings_list);
                                import_list.add(app_settings_list);
                                break;
                        }
                        import_settings();
                    }
                })
                .setNegativeButton("Export", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LED_MODE_VALUES_READ = false;
                        LIGHTS_CONFIG_VALUES_READ = false;
                        REMOTE_VALUES_READ = false;
                        CONTROLS_VALUES_READ = false;
                        ORIENTATION_VALUES_READ = false;

                        creatNewEportFile();
                        if(read_on_export_check.isChecked()){
                            switch (view.getId()) {
                                case R.id.import_export_led_mode_settings_button:
                                    export_all_led_mode_settings(true);
                                    break;
                                case R.id.import_export_lights_config_settings_button:
                                    TTL_Read_List.add(lights_config_settings_list);
                                    break;
                                case R.id.import_export_remote_settings_button:
                                    TTL_Read_List.add(remote_settings_list);
                                    break;
                                case R.id.import_export_control_settings_button:
                                    TTL_Read_List.add(controls_settings_list);
                                    break;
                                case R.id.import_export_orientation_settings_button:
                                    TTL_Read_List.add(orientation_settings_list);
                                    break;
                                case R.id.import_export_ttl_settings_button:
                                    export_all_led_mode_settings(true);
                                    TTL_Read_List.add(lights_config_settings_list);
                                    TTL_Read_List.add(remote_settings_list);
                                    TTL_Read_List.add(controls_settings_list);
                                    TTL_Read_List.add(orientation_settings_list);
                                    break;
                                case R.id.import_export_app_settings_button:
                                    export_settings(app_settings_list);
                                    break;
                                case R.id.import_export_all_settings_button:
                                    export_all_led_mode_settings(true);
                                    TTL_Read_List.add(lights_config_settings_list);
                                    TTL_Read_List.add(remote_settings_list);
                                    TTL_Read_List.add(controls_settings_list);
                                    TTL_Read_List.add(orientation_settings_list);
                                    export_settings(app_settings_list);
                                    break;
                            }
                            readNextExportSetting();
                        } else{
                            switch (view.getId()) {
                                case R.id.import_export_led_mode_settings_button:
                                    export_all_led_mode_settings(false);
                                    break;
                                case R.id.import_export_lights_config_settings_button:
                                    export_settings(lights_config_settings_list);
                                    break;
                                case R.id.import_export_remote_settings_button:
                                    export_settings(remote_settings_list);
                                    break;
                                case R.id.import_export_control_settings_button:
                                    export_settings(controls_settings_list);
                                    break;
                                case R.id.import_export_orientation_settings_button:
                                    export_settings(orientation_settings_list);
                                    break;
                                case R.id.import_export_ttl_settings_button:
                                    export_all_led_mode_settings(false);
                                    export_settings(lights_config_settings_list);
                                    export_settings(remote_settings_list);
                                    export_settings(controls_settings_list);
                                    export_settings(orientation_settings_list);
                                    break;
                                case R.id.import_export_app_settings_button:
                                    export_settings(app_settings_list);
                                    break;
                                case R.id.import_export_all_settings_button:
                                    export_all_led_mode_settings(false);
                                    export_settings(lights_config_settings_list);
                                    export_settings(remote_settings_list);
                                    export_settings(controls_settings_list);
                                    export_settings(orientation_settings_list);
                                    export_settings(app_settings_list);
                                    break;
                            }
                            closeExportFile();
                        }
                    }
                })
                .setCancelable(true)
                .show();
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                finish();
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                if(fos != null && (data.length == 3 || data.length == 6 || data.length == 7 || data.length == 10 || data.length == 15 || data.length == 17 || data.length == 20)){
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x31:
                                if (i + 2 >= data.length)
                                    break;
                                editor.putBoolean(general_led_mode_settings_list[2][1],((data[i + 1] & 0x08) >> 3) == 1);// head
                                editor.putBoolean(general_led_mode_settings_list[3][1],((data[i + 1] & 0x04) >> 2) == 1);// side
                                editor.putBoolean(general_led_mode_settings_list[4][1],((data[i + 1] & 0x02) >> 1) == 1);// light
                                editor.putBoolean(general_led_mode_settings_list[5][1],(data[i + 1] & 0x01) == 1);// sens
                                editor.putInt(general_led_mode_settings_list[0][1],(data[i + 2] & 0xFF));// led strip type
                                editor.putInt(general_led_mode_settings_list[1][1],(data[i + 1] & 0xF0) >> 4);// mode
                                i += 2;
                                break;
                            case 0x32:
                                if (i + 6 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][0][1],(data[i + 1] & 0xFF));//LR
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][1][1],(data[i + 2] & 0xFF));//LG
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][2][1],(data[i + 3] & 0xFF));//LB
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][3][1],(data[i + 4] & 0xFF));//RR
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][4][1],(data[i + 5] & 0xFF));//RG
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][5][1],(data[i + 6] & 0xFF));//RB
                                i += 6;
                                break;
                            case 0x33:
                                if (i + 2 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CYCLE.ordinal()][0][1],data[i + 1] & 0xFF);
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CYCLE.ordinal()][1][1],data[i + 2] & 0xFF);
                                i += 2;
                                break;
                            case 0x34:
                                if (i + 1 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_COMPASS.ordinal()][0][1],data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x35:
                                if (i + 2 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_THROTTLE.ordinal()][0][1],data[i + 1] & 0xFF);
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_THROTTLE.ordinal()][1][1],data[i + 2] & 0xFF);
                                i += 2;
                                break;
                            case 0x36:
                                if (i + 1 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_RPM.ordinal()][0][1],data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x37:
                                if (i + 1 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_X_ACCEL.ordinal()][0][1],data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x38:
                                if (i + 1 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_Y_ACCEL.ordinal()][0][1],data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x39:
                                if (i + 10 >= data.length)
                                    break;
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][0][1],(data[i + 1] & 0xF0) >> 4);// color base
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][2][1],data[i + 1] & 0x0F);// bright base
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][1][1],data[i + 2] & 0xFF);// rate base
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][3][1],data[i + 3] & 0xFF);// LR
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][4][1],data[i + 4] & 0xFF);// LG
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][5][1],data[i + 5] & 0xFF);// LB
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][6][1],data[i + 6] & 0xFF);// RR
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][7][1],data[i + 7] & 0xFF);// RG
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][8][1],data[i + 8] & 0xFF);// RB
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][9][1],data[i + 9] & 0xFF);// Rate
                                editor.putInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][10][1],data[i + 10] & 0xFF);// bright
                                i += 10;
                                break;
                            case 0x3A:
                                if (i + 3 >= data.length)
                                    break;
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_STATIC.ordinal()][0][1],data[i + 1] & 0xFF);// zoom
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_STATIC.ordinal()][1][1],data[i + 2] & 0xFF);// shift
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_STATIC.ordinal()][2][1],data[i + 3] & 0xFF);// bright
                                i += 3;
                                break;
                            case 0x3B:
                                if (i + 1 >= data.length)
                                    break;
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_SKITTLES.ordinal()][0][1],data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x3C:
                                if (i + 3 >= data.length)
                                    break;
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_CYCLE.ordinal()][0][1],data[i + 1] & 0xFF);//zoom
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_CYCLE.ordinal()][1][1],data[i + 2] & 0xFF);//rate
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_CYCLE.ordinal()][2][1],data[i + 3] & 0xFF);//bright
                                i += 3;
                                break;
                            case 0x3D:
                                if (i + 1 >= data.length)
                                    break;
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_COMPASS.ordinal()][0][1],data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x3E:
                                if (i + 4 >= data.length)
                                    break;
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][0][1],data[i + 1] & 0xFF);// zoom
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][1][1],data[i + 2] & 0xFF);//shift
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][2][1],data[i + 3] & 0xFF);//sens
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][3][1],data[i + 4] & 0xFF);//bright
                                i += 4;
                                break;
                            case 0x3F:
                                if (i + 3 >= data.length)
                                    break;
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()][0][1],data[i + 1] & 0xFF);// zoom
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()][0][1],data[i + 2] & 0xFF);// rate
                                editor.putInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()][0][1],data[i + 3] & 0xFF);// bright
                                i += 3;
                                break;
                            case 0x40:
                                if (i + 5 >= data.length)
                                    break;
                                editor.putBoolean(led_mode_shuffle_settings_list[0][1],(data[i + 1] & 0x01) == 1);
                                editor.putBoolean(led_mode_shuffle_settings_list[1][1],((data[i + 2] & 0x01)) == 1);//an static
                                editor.putBoolean(led_mode_shuffle_settings_list[2][1],((data[i + 2] & 0x02) >> 1) == 1);// an cycle
                                editor.putBoolean(led_mode_shuffle_settings_list[3][1],((data[i + 2] & 0x04) >> 2) == 1);// an compass
                                editor.putBoolean(led_mode_shuffle_settings_list[4][1],((data[i + 2] & 0x08) >> 3) == 1);// an throttle
                                editor.putBoolean(led_mode_shuffle_settings_list[5][1],((data[i + 2] & 0x10) >> 4) == 1);// an rpm
                                editor.putBoolean(led_mode_shuffle_settings_list[6][1],((data[i + 2] & 0x20) >> 5) == 1);// an rpm throttle
                                editor.putBoolean(led_mode_shuffle_settings_list[7][1],((data[i + 2] & 0x40) >> 6) == 1);// an x accel
                                editor.putBoolean(led_mode_shuffle_settings_list[8][1],((data[i + 2] & 0x80) >> 7) == 1);// an y accel
                                editor.putBoolean(led_mode_shuffle_settings_list[9][1],((data[i + 3] & 0x01)) == 1);// an custom
                                editor.putBoolean(led_mode_shuffle_settings_list[10][1],((data[i + 4] & 0x01)) == 1);// di static
                                editor.putBoolean(led_mode_shuffle_settings_list[11][1],((data[i + 4] & 0x02) >> 1) == 1);// di skittles
                                editor.putBoolean(led_mode_shuffle_settings_list[12][1],((data[i + 4] & 0x04) >> 2) == 1);// di cycle
                                editor.putBoolean(led_mode_shuffle_settings_list[13][1],((data[i + 4] & 0x08) >> 3) == 1);// di compass
                                editor.putBoolean(led_mode_shuffle_settings_list[14][1],((data[i + 4] & 0x10) >> 4) == 1);// di throttle
                                editor.putBoolean(led_mode_shuffle_settings_list[15][1],((data[i + 4] & 0x20) >> 5) == 1);// di rpm cycle
                                editor.putBoolean(led_mode_shuffle_settings_list[16][1],((data[i + 4] & 0x40) >> 6) == 1);// di rpm throttle
                                editor.putBoolean(led_mode_shuffle_settings_list[17][1],((data[i + 4] & 0x80) >> 7) == 1);// di compass wheel
                                editor.putBoolean(led_mode_shuffle_settings_list[18][1],((data[i + 5] & 0x01)) == 1);// di compass snake
                                editor.commit();
                                export_all_led_mode_settings(false);
                                readNextExportSetting();
                                i += 5;
                                break;//*/
                            case 0x71: //orientation
                                if(i+2 >= data.length)
                                    break;
                                editor.putInt(orientation_settings_list[0][1],(data[i + 1] & 0xFF));
                                editor.putInt(orientation_settings_list[1][1],(data[i + 2] & 0xFF));
                                editor.commit();
                                export_settings(orientation_settings_list);
                                readNextExportSetting();
                                i+=2;
                                break;
                            case 0x72: //Remote
                                if(i+2 >= data.length)
                                    break;
                                editor.putInt(remote_settings_list[0][1],(data[i + 1] & 0xF0) >> 4);
                                editor.putInt(remote_settings_list[1][1],(data[i + 1] & 0x0F));
                                editor.putInt(remote_settings_list[2][1],(data[i + 2] & 0xFF));
                                editor.commit();
                                export_settings(remote_settings_list);
                                readNextExportSetting();
                                i+=2;
                                break;
                            case 0x75:
                                if(i+5 >= data.length)
                                    break;
                                editor.putInt(lights_config_settings_list[2][1],(data[i + 1] & 0xF0) >> 4);// RGB type
                                editor.putInt(lights_config_settings_list[5][1],(data[i + 1] & 0x0F)); // brake mode
                                editor.putInt(lights_config_settings_list[7][1],(data[i + 2] & 0xFF)); // deadzone
                                editor.putString(lights_config_settings_list[4][1],""+(data[i+3] & 0xFF));// led num
                                editor.putBoolean(lights_config_settings_list[3][1],(data[i+4] & 0x80) == 0x80); // rgb sync
                                editor.putBoolean(lights_config_settings_list[6][1],(data[i+4] & 0x40) == 0x40); // always on brake
                                editor.putBoolean(lights_config_settings_list[8][1],(data[i+4] & 0x20) == 0x20); // default state
                                editor.putBoolean(lights_config_settings_list[9][1],(data[i+4] & 0x10) == 0x10); // highbeam
                                editor.putBoolean(lights_config_settings_list[0][1],(data[i+4] & 0x08) == 0x08); // standby
                                editor.putBoolean(lights_config_settings_list[1][1],(data[i+4] & 0x04) == 0x04); // shuffle
                                editor.putInt(lights_config_settings_list[10][1],(data[i + 5] & 0xFF)); // lowbeam
                                editor.commit();
                                export_settings(lights_config_settings_list);
                                readNextExportSetting();
                                i+=5;
                                break;
                            case 0x81:
                                if(i+6 >= data.length)
                                    break;
                                editor.putBoolean(controls_settings_list[0][1],(data[i + 1] & 0x80) == 0x80); //aux enable
                                editor.putInt(controls_settings_list[1][1],(data[i + 1] & 0x0F)); // aux type
                                editor.putInt(controls_settings_list[2][1],(data[i + 2] & 0xFF)); // aux time
                                editor.putInt(controls_settings_list[3][1],(data[i + 3] & 0xF0) >> 4); // aux
                                editor.putInt(controls_settings_list[5][1],(data[i + 3] & 0x0F)); // all
                                editor.putInt(controls_settings_list[6][1],(data[i + 4] & 0xF0) >> 4); // head
                                editor.putInt(controls_settings_list[7][1],(data[i + 4] & 0x0F)); // side
                                editor.putInt(controls_settings_list[9][1],(data[i + 5] & 0xF0) >> 4); // down
                                editor.putInt(controls_settings_list[8][1],(data[i + 5] & 0x0F)); // up
                                editor.putInt(controls_settings_list[4][1],(data[i + 6] & 0x0F)); // brights
                                editor.commit();
                                export_settings(controls_settings_list);
                                readNextExportSetting();
                                i+=6;
                                break;
                        }
                        editor.commit();
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

        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothService = null;
        unregisterReceiver(mGattUpdateReceiver);
    }

    void import_settings() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Check if we have Call permission
            int permisson = ActivityCompat.checkSelfPermission(ImportExportSettingsActivity.this
                    ,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permisson != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_REQUEST_IMPORT_PERMISSION
                );
                return;
            }
        }

        doBrowseFile();

        Toast.makeText(ImportExportSettingsActivity.this, "Import", Toast.LENGTH_SHORT).show();
    }

    void export_settings(String[][] settings_list) {
        //Toast.makeText(ImportExportSettingsActivity.this, "Export", Toast.LENGTH_SHORT).show();
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

    void save_ipmorted_setting(String value, String[] setting_info, SharedPreferences.Editor editor){
        if(setting_info[2].equals("int")){
            editor.putInt(setting_info[1],Integer.parseInt(value));
        } else if(setting_info[2].equals("bool")){
            editor.putBoolean(setting_info[1],Boolean.parseBoolean(value));
        } else if(setting_info[2].equals("string")){
            editor.putString(setting_info[1], value);
        } else if(setting_info[2].equals("float")){
            editor.putFloat(setting_info[1], Float.parseFloat(value));
        }
    }

    public void writeStringToLog(String text){
        try {
            fos.write(text.getBytes());
        } catch (Exception e){
            Toast.makeText(ImportExportSettingsActivity.this, "Failed write export", Toast.LENGTH_SHORT).show();
        }
    }

    public void writeBytesToLog(byte[] data){
        try {
            fos.write(data);
        } catch (Exception e){
            Toast.makeText(ImportExportSettingsActivity.this, "Failed write export", Toast.LENGTH_SHORT).show();
        }
    }

    private void doBrowseFile()  {
            Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFileIntent.setType("text/plain");
            // Only return URIs that can be opened with ContentResolver
            chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

            chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file");
            startActivityForResult(chooseFileIntent, MY_RESULT_IMPORT_FILECHOOSER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MY_RESULT_IMPORT_FILECHOOSER:
                if (resultCode == Activity.RESULT_OK ) {
                    if(data != null)  {
                        Uri fileUri = data.getData();
                        Log.i(TAG, "Uri: " + fileUri);
                        ImportPath = FileUtils.getPath(ImportExportSettingsActivity.this, fileUri);

                        InputStreamReader isr = null;
                        BufferedReader buf = null;
                        String line = null;
                        String[][] name_value_list =  new String[100][2];
                        int index = 0;

                        try {
                            //Toast.makeText(ImportExportSettingsActivity.this, ImportPath, Toast.LENGTH_LONG).show();
                            File ImportFile = new File(ImportPath);

                            FileInputStream fileInputStream = new FileInputStream(ImportFile);
                            isr = new InputStreamReader(fileInputStream);
                            buf = new BufferedReader(isr);
                            while ((line = buf.readLine()) != null) {
                                name_value_list[index] = line.split(",");
                                index++;
                            }

                            buf.close();
                            isr.close();
                        }catch (Exception e) {
                            Log.e(TAG,"Error: " + e);
                            Toast.makeText(ImportExportSettingsActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                        }


                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();

                        boolean ALL_SETTINGS_FOUND = true;
                        int imported_setting_count = 0;
                        int settings_to_import_count = 0;
                        String[][] temp_list;
                        for(int i = 0; i < import_list.size(); i++){
                            temp_list = import_list.get(i);
                            for(int j = 0; j < temp_list.length; j++) {
                                boolean SETTING_FOUND = false;
                                settings_to_import_count++;
                                for (int k = 0; k < name_value_list.length; k++) {
                                    if (temp_list[j][0].equals(name_value_list[k][0])) {
                                        SETTING_FOUND = true;
                                        save_ipmorted_setting(name_value_list[k][1], temp_list[j], editor);
                                        imported_setting_count++;
                                        break;
                                        //Toast.makeText(ImportExportSettingsActivity.this, "Match", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                if(!SETTING_FOUND){
                                    ALL_SETTINGS_FOUND = false;
                                }
                            }
                        }
                        editor.commit();

                        if(!ALL_SETTINGS_FOUND){
                            if(imported_setting_count > 0) {
                                Toast.makeText(ImportExportSettingsActivity.this, "Settings partially imported: " + imported_setting_count + "/" + settings_to_import_count, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ImportExportSettingsActivity.this, "No settings imported, check import file", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(ImportExportSettingsActivity.this, "Settings imported successfully", Toast.LENGTH_SHORT).show();
                        }
                        //Toast.makeText(ImportExportSettingsActivity.this, "Settings partially imported: " + imported_setting_count + "/" + settings_to_import_count, Toast.LENGTH_LONG).show();
                        // Write values through BLE if needed
                        if(write_on_import_check.isChecked()){
                            //Toast.makeText(ImportExportSettingsActivity.this, "Writing settings to TTL", Toast.LENGTH_SHORT).show();
                            int temp_size = import_list.size();
                            for(int i = 0; i < temp_size; i++){
                                temp_list = import_list.get(0);
                                import_list.remove(0);
                                TTLWritePacket temp_packet = null;
                                for(int j = 0; j < analog_led_mode_settings_lists.length; j++) {
                                    if(temp_list.equals(analog_led_mode_settings_lists[j])){
                                        byte switches_led_type;
                                        switches_led_type = (byte) (settings.getInt(general_led_mode_settings_list[0][1],0) & 0x0F);
                                        switches_led_type = (byte) (switches_led_type | ((byte) 0xFF & (byte) (settings.getBoolean(general_led_mode_settings_list[3][1], false) ? 1 : 0) << 4));
                                        switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[2][1], false) ? 1 : 0)) << 5);
                                        switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[4][1], false) ? 1 : 0)) << 6);
                                        switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[5][1], false) ? 1 : 0)) << 7);
                                        byte mode_bools;
                                        switch (AnalogModes.values()[j]){
                                            case ANALOG_MODE_STATIC:
                                                temp_packet = analog_static_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_STATIC.ordinal()+1][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][0][1], 0)*2.55);
                                                temp_packet.data[2] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][1][1], 0)*2.55);
                                                temp_packet.data[3] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][2][1], 0)*2.55);
                                                temp_packet.data[4] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][3][1], 0)*2.55);
                                                temp_packet.data[5] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][4][1], 0)*2.55);
                                                temp_packet.data[6] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_STATIC.ordinal()][5][1], 0)*2.55);
                                                temp_packet.data[7] = mode_bools;
                                                break;
                                            case ANALOG_MODE_CYCLE:
                                                temp_packet = analog_cycle_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_CYCLE.ordinal()+1][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CYCLE.ordinal()][0][1], 0));
                                                temp_packet.data[2] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CYCLE.ordinal()][1][1], 0));
                                                temp_packet.data[3] = mode_bools;
                                                break;
                                            case ANALOG_MODE_COMPASS:
                                                temp_packet = analog_compass_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_COMPASS.ordinal()+1][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_COMPASS.ordinal()][0][1], 0));
                                                temp_packet.data[2] = mode_bools;
                                                break;
                                            case ANALOG_MODE_THROTTLE:
                                                temp_packet = analog_throttle_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_THROTTLE.ordinal()+1][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_THROTTLE.ordinal()][0][1], 0));
                                                temp_packet.data[2] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_THROTTLE.ordinal()][1][1], 0));
                                                temp_packet.data[3] = mode_bools;
                                                break;
                                            case ANALOG_MODE_RPM:
                                                temp_packet = analog_rpm_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_RPM.ordinal()+1][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_RPM.ordinal()][0][1], 0));
                                                temp_packet.data[2] = mode_bools;
                                                break;
                                            /*case ANALOG_MODE_RPM_THROTTLE:
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_RPM.ordinal()][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_RPM.ordinal()][0][1], 0));
                                                temp_packet.data[2] = mode_bools;
                                                break;*/
                                            case ANALOG_MODE_X_ACCEL:
                                                temp_packet = analog_x_accel_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_X_ACCEL.ordinal()+2][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_X_ACCEL.ordinal()][0][1], 0));
                                                temp_packet.data[2] = mode_bools;
                                                break;
                                            case ANALOG_MODE_Y_ACCEL:
                                                temp_packet = analog_y_accel_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_Y_ACCEL.ordinal()+2][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_Y_ACCEL.ordinal()][0][1], 0));
                                                temp_packet.data[2] = mode_bools;
                                                break;
                                            case ANALOG_MODE_CUSTOM:
                                                temp_packet = analog_custom_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()+2][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][0][1], 0));
                                                temp_packet.data[2] = (byte) ((settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][1][1], 0) << 4)|
                                                                                settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][2][1], 0));//
                                                temp_packet.data[3] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][3][1], 0)*2.55);
                                                temp_packet.data[4] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][4][1], 0)*2.55);
                                                temp_packet.data[5] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][5][1], 0)*2.55);
                                                temp_packet.data[6] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][6][1], 0)*2.55);
                                                temp_packet.data[7] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][7][1], 0)*2.55);
                                                temp_packet.data[8] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][8][1], 0)*2.55);
                                                temp_packet.data[9] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][9][1], 0));
                                                temp_packet.data[10] = (byte) (settings.getInt(analog_led_mode_settings_lists[AnalogModes.ANALOG_MODE_CUSTOM.ordinal()][10][1], 0));
                                                temp_packet.data[11] = mode_bools;
                                                break;
                                        }
                                        if(temp_packet != null) {
                                            write_ttl_packet(temp_packet);
                                            temp_packet = null;
                                        }
                                    }
                                }
                                for(int j = 0; j < digital_led_mode_settings_lists.length; j++) {
                                    if(temp_list.equals(digital_led_mode_settings_lists[j])){
                                        //Toast.makeText(ImportExportSettingsActivity.this, "here", Toast.LENGTH_SHORT).show();
                                        byte switches_led_type;
                                        switches_led_type = (byte) (settings.getInt(general_led_mode_settings_list[0][1],0) & 0x0F);
                                        switches_led_type = (byte) (switches_led_type | ((byte) 0xFF & (byte) (settings.getBoolean(general_led_mode_settings_list[3][1], false) ? 1 : 0) << 4));
                                        switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[2][1], false) ? 1 : 0)) << 5);
                                        switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[4][1], false) ? 1 : 0)) << 6);
                                        switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[5][1], false) ? 1 : 0)) << 7);
                                        byte mode_bools;
                                        switch (DigitalModes.values()[j]){
                                            case DIGITAL_MODE_STATIC:
                                                temp_packet = digital_static_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_STATIC.ordinal()][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_STATIC.ordinal()][0][1], 0));
                                                temp_packet.data[2] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_STATIC.ordinal()][1][1], 0));
                                                temp_packet.data[3] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_STATIC.ordinal()][2][1], 0));
                                                temp_packet.data[4] = mode_bools;
                                                break;
                                            case DIGITAL_MODE_SKITTLES:
                                                temp_packet = digital_skittles_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_SKITTLES.ordinal()][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_SKITTLES.ordinal()][0][1], 0));
                                                temp_packet.data[2] = mode_bools;
                                                break;
                                            case DIGITAL_MODE_CYCLE:
                                                temp_packet = digital_cycle_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_CYCLE.ordinal()][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_CYCLE.ordinal()][0][1], 0));
                                                temp_packet.data[2] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_CYCLE.ordinal()][1][1], 0));
                                                temp_packet.data[3] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_CYCLE.ordinal()][2][1], 0));
                                                temp_packet.data[4] = mode_bools;
                                                break;
                                            case DIGITAL_MODE_COMPASS:
                                                temp_packet = digital_compass_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_COMPASS.ordinal()][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_COMPASS.ordinal()][0][1], 0));
                                                temp_packet.data[2] = mode_bools;
                                                break;
                                            case DIGITAL_MODE_THROTTLE:
                                                temp_packet = digital_throttle_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][0][1], 0));
                                                temp_packet.data[2] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][1][1], 0));
                                                temp_packet.data[3] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][2][1], 0));
                                                temp_packet.data[4] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_THROTTLE.ordinal()][3][1], 0));
                                                temp_packet.data[5] = mode_bools;
                                                break;
                                            case DIGITAL_MODE_RPM_Cycle:
                                                temp_packet = digital_rpm_values_write_packet;
                                                mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()][1], false) ? 1 : 0)) << 7);
                                                temp_packet.data[0] = switches_led_type;
                                                temp_packet.data[1] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()][0][1], 0));
                                                temp_packet.data[2] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()][1][1], 0));
                                                temp_packet.data[3] = (byte) (settings.getInt(digital_led_mode_settings_lists[DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()][2][1], 0));
                                                temp_packet.data[4] = mode_bools;
                                                break;
                                        }
                                        if(temp_packet != null) {
                                            write_ttl_packet(temp_packet);
                                            temp_packet = null;
                                        }
                                    }
                                }
                                if(temp_list.equals(lights_config_settings_list)){
                                    // (RGB type[4], brake mode), deadzone, LEDnum, (sync RGB[7], brake always on[6], default state[5],highbeams[4], standby[3], shuffle[2]), low beam level
                                    temp_packet = lights_config_write_packet;
                                    temp_packet.data[0] = (byte) ((settings.getInt(lights_config_settings_list[2][1], 0) << 4)|
                                            settings.getInt(lights_config_settings_list[5][1], 0));
                                    temp_packet.data[1] = (byte) (settings.getInt(lights_config_settings_list[7][1], 0));
                                    temp_packet.data[2] = (byte) (Integer.parseInt(settings.getString(lights_config_settings_list[4][1], "0")));
                                    temp_packet.data[3] = (byte) (((settings.getBoolean(lights_config_settings_list[3][1], false) ? 1 : 0) << 7) |
                                            ((settings.getBoolean(lights_config_settings_list[6][1], false) ? 1 : 0) << 6) |
                                            ((settings.getBoolean(lights_config_settings_list[8][1], false) ? 1 : 0) << 5) |
                                            ((settings.getBoolean(lights_config_settings_list[9][1], false) ? 1 : 0) << 4) |
                                            ((settings.getBoolean(lights_config_settings_list[0][1], false) ? 1 : 0) << 3) |
                                            ((settings.getBoolean(lights_config_settings_list[1][1], false) ? 1 : 0) << 2));
                                    temp_packet.data[4] = (byte) (settings.getInt(lights_config_settings_list[10][1], 0));
                                } else  if(temp_list.equals(remote_settings_list)){
                                    // remote type, button type, deadzone
                                    temp_packet = remote_config_write_packet;
                                    temp_packet.data[0] = (byte) (((settings.getInt(remote_settings_list[0][1], 0)) << 4) |
                                            (settings.getInt(remote_settings_list[1][1], 0)));
                                    temp_packet.data[1] = (byte) (settings.getInt(remote_settings_list[2][1], 0));
                                } else  if(temp_list.equals(controls_settings_list)){
                                    // (aux check[7], turncheck[6], aux type), aux time, (aux[4], toggle all), (toggle head[4], toggle side), (mode down[4], mode up), bright
                                    temp_packet = controls_config_write_packet;
                                    temp_packet.data[0] = (byte) (((settings.getBoolean(controls_settings_list[0][1], false) ? 1 : 0) << 7) |
                                            (settings.getInt(controls_settings_list[1][1], 0)));
                                    temp_packet.data[1] = (byte) (settings.getInt(controls_settings_list[2][1], 0));
                                    temp_packet.data[2] = (byte) (((settings.getInt(controls_settings_list[3][1], 0)) << 4) |
                                            (settings.getInt(controls_settings_list[5][1], 0)));
                                    temp_packet.data[3] = (byte) (((settings.getInt(controls_settings_list[6][1], 0)) << 4) |
                                            (settings.getInt(controls_settings_list[7][1], 0)));
                                    temp_packet.data[4] = (byte) (((settings.getInt(controls_settings_list[9][1], 0)) << 4) |
                                            (settings.getInt(controls_settings_list[8][1], 0)));
                                    temp_packet.data[5] = (byte) (settings.getInt(controls_settings_list[4][1], 0));
                                } else  if(temp_list.equals(orientation_settings_list)){
                                    // coonector, power
                                    temp_packet = orientation_config_write_packet;
                                    temp_packet.data[0] = (byte) (settings.getInt(orientation_settings_list[0][1], 0));
                                    temp_packet.data[1] = (byte) (settings.getInt(orientation_settings_list[1][1], 0));
                                } else  if(temp_list.equals(led_mode_shuffle_settings_list)){
                                    // do something to handle the shuffle settings not covered above
                                    byte switches_led_type;
                                    switches_led_type = (byte) (settings.getInt(general_led_mode_settings_list[0][1],0) & 0x0F);
                                    switches_led_type = (byte) (switches_led_type | ((byte) 0xFF & (byte) (settings.getBoolean(general_led_mode_settings_list[3][1], false) ? 1 : 0) << 4));
                                    switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[2][1], false) ? 1 : 0)) << 5);
                                    switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[4][1], false) ? 1 : 0)) << 6);
                                    switches_led_type = (byte) (switches_led_type | ((byte) (settings.getBoolean(general_led_mode_settings_list[5][1], false) ? 1 : 0)) << 7);
                                    byte mode_bools;
                                    // analog rpm throttle
                                    temp_packet = analog_rpm_throttle_values_write_packet;
                                    mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[AnalogModes.ANALOG_MODE_RPM.ordinal()+2][1], false) ? 1 : 0)) << 7);
                                    temp_packet.data[0] = switches_led_type;
                                    temp_packet.data[1] = mode_bools;
                                    write_ttl_packet(temp_packet);
                                    // digital rpm throttle
                                    temp_packet = digital_rpm_throttle_values_write_packet;
                                    mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()+1][1], false) ? 1 : 0)) << 7);
                                    temp_packet.data[0] = switches_led_type;
                                    temp_packet.data[1] = mode_bools;
                                    write_ttl_packet(temp_packet);
                                    // digital compass wheel
                                    temp_packet = digital_compass_wheel_values_write_packet;
                                    mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()+2][1], false) ? 1 : 0)) << 7);
                                    temp_packet.data[0] = switches_led_type;
                                    temp_packet.data[1] = mode_bools;
                                    write_ttl_packet(temp_packet);
                                    // digital compass snake
                                    temp_packet = digital_compass_snake_values_write_packet;
                                    mode_bools = (byte) (((byte) (settings.getBoolean(led_mode_shuffle_settings_list[10+DigitalModes.DIGITAL_MODE_RPM_Cycle.ordinal()+3][1], false) ? 1 : 0)) << 7);
                                    temp_packet.data[0] = switches_led_type;
                                    temp_packet.data[1] = mode_bools;
                                    write_ttl_packet(temp_packet);
                                    temp_packet = null;

                                    // sett the correct mode
                                    int led_mode = settings.getInt(general_led_mode_settings_list[1][1], 0);
                                    if(8 != led_mode){
                                     for(int j = 0; j < 8-led_mode;  j++){
                                        read_ttl_values(led_mode_down_byte);// Send the LED mode down command until the correct mode is reached;
                                     }
                                    }
                                }
                                if(temp_packet != null) {
                                    write_ttl_packet(temp_packet);
                                    temp_packet = null;
                                }
                            }
                        } else {
                            import_list.clear();
                        }
                    }
                } else if(resultCode == Activity.RESULT_CANCELED){
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

   void creatNewEportFile(){
       String filepath = Environment.getExternalStorageDirectory().getPath();
       File dir = new File(filepath+"/TelTail");
       if (!dir.exists()) {
           if(!dir.mkdirs()){
               Toast.makeText(ImportExportSettingsActivity.this, "Failed to make TTL dir", Toast.LENGTH_SHORT).show();
           }
       }
       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH-mm-ss", Locale.getDefault());
       sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
       ExportFile = new File(dir, "Settings Export " + sdf.format(new Date()) + ".txt");

       //Toast.makeText(ImportExportSettingsActivity.this, filepath, Toast.LENGTH_SHORT).show();
       try {
           fos = new FileOutputStream(ExportFile);
       } catch (Exception e){
           Toast.makeText(ImportExportSettingsActivity.this, "Failed to open export file", Toast.LENGTH_SHORT).show();
       }
   }

   void closeExportFile(){
       try {
           fos.close();
       } catch (Exception e) {
           Toast.makeText(ImportExportSettingsActivity.this, "Failed to close export", Toast.LENGTH_SHORT).show();
       }

       try {
           if (ExportFile.length() == 0){
               ExportFile.delete();
           }else{
               Toast.makeText(ImportExportSettingsActivity.this, "Exported", Toast.LENGTH_SHORT).show();
           }
       } catch (Exception e){
           Toast.makeText(ImportExportSettingsActivity.this, "Failed to delete empty export file", Toast.LENGTH_SHORT).show();
       }
       fos = null;
   }

    void readNextExportSetting(){
        if(TTL_Read_List.isEmpty()){
            closeExportFile();
            return;
        }

        if(is_contained_by(TTL_Read_List.get(0),analog_led_mode_settings_lists)){
           //Toast.makeText(ImportExportSettingsActivity.this, "Reading LED Modes", Toast.LENGTH_SHORT).show();
           if(!read_ttl_values(read_led_vars_byte)){
               Toast.makeText(ImportExportSettingsActivity.this, "Could not request LED mode values", Toast.LENGTH_SHORT).show();
           }
           for(int i = 0; i < analog_led_mode_settings_lists.length+digital_led_mode_settings_lists.length+1; i++){ // remove all analog and digital mode lists. Add 1 since general mode list and shuffle list is also included
               TTL_Read_List.remove(0);
           }
        }else if(TTL_Read_List.get(0) == lights_config_settings_list){
           //Toast.makeText(ImportExportSettingsActivity.this, "Reading Lights Config", Toast.LENGTH_SHORT).show();
           if(!read_ttl_values(read_lights_config_byte)){
                   Toast.makeText(ImportExportSettingsActivity.this, "Could not request lights config values", Toast.LENGTH_SHORT).show();
           }
       }else if(TTL_Read_List.get(0) == remote_settings_list){
           //Toast.makeText(ImportExportSettingsActivity.this, "Reading Remote Config", Toast.LENGTH_SHORT).show();
           if(!read_ttl_values(read_remote_config_byte)){
               Toast.makeText(ImportExportSettingsActivity.this, "Could not request remote config values", Toast.LENGTH_SHORT).show();
           }
       }else if(TTL_Read_List.get(0) == controls_settings_list){
           //Toast.makeText(ImportExportSettingsActivity.this, "Reading Controls Config", Toast.LENGTH_SHORT).show();
           if(!read_ttl_values(read_controls_config_byte)){
               Toast.makeText(ImportExportSettingsActivity.this, "Could not request controls config values", Toast.LENGTH_SHORT).show();
           }
       }else if(TTL_Read_List.get(0) == orientation_settings_list){
           //Toast.makeText(ImportExportSettingsActivity.this, "Reading Orientation Config", Toast.LENGTH_SHORT).show();
           if(!read_ttl_values(read_orientation_config_byte)){
               Toast.makeText(ImportExportSettingsActivity.this, "Could not request orientation config values", Toast.LENGTH_SHORT).show();
           }
       }
       TTL_Read_List.remove(0);
    }

    boolean write_ttl_values(byte ID, byte DataLength, byte[] Data){
        final int packet_length = 4+DataLength;
        byte[] txbuf = new byte[packet_length];
        txbuf[0] = (byte) 0x0A5;
        txbuf[1] = (byte) 0x002;
        txbuf[2] = ID;
        for(int i = 0; i < DataLength; i++) {
            txbuf[i+3] = Data[i];
        }
        txbuf[3] = (byte) 0x05A;
        long timeout_timer = System.currentTimeMillis();
        while(!mBluetoothService.writeBytes(txbuf)){
            if(System.currentTimeMillis() - timeout_timer > 1000) {
                return false;
            }
        }
        return true;
    }

    boolean read_ttl_values(byte ID){
        read_values_packet[2] = ID;
        long timeout_timer = System.currentTimeMillis();
        while(!mBluetoothService.writeBytes(read_values_packet)){
            if(System.currentTimeMillis() - timeout_timer > BLE_WRITE_TIMEOUT) {
                return false;
            }
        }
        return true;
    }

    boolean is_contained_by(String[][] arg, String[][][] container){
       for(int i = 0; i < container.length; i++){
           if(container[i].equals(arg)){
               return true;
           }
       }
       return false;
    }

    void export_all_led_mode_settings(boolean READ){
        if(READ){
            for(int i = 0; i < analog_led_mode_settings_lists.length; i++){
                TTL_Read_List.add(analog_led_mode_settings_lists[i]);
            }
            for(int i = 0; i < digital_led_mode_settings_lists.length; i++){
                TTL_Read_List.add(digital_led_mode_settings_lists[i]);
            }
            TTL_Read_List.add(led_mode_shuffle_settings_list);
            TTL_Read_List.add(general_led_mode_settings_list);
        } else {
            export_settings(led_mode_shuffle_settings_list);
            for (int i = 0; i < analog_led_mode_settings_lists.length; i++) {
                export_settings(analog_led_mode_settings_lists[i]);
            }
            for (int i = 0; i < digital_led_mode_settings_lists.length; i++) {
                export_settings(digital_led_mode_settings_lists[i]);
            }
            export_settings(general_led_mode_settings_list);
        }
    }

    boolean write_ttl_packet(TTLWritePacket packet){
        long send_timer = System.currentTimeMillis();
        final int packet_size = 4+packet.data.length;
        byte[] txbuf = new byte[packet_size];
        txbuf[0] = (byte) 0x0A5;
        txbuf[1] = (byte) (packet.data.length);
        txbuf[2] = packet.id;
        for(int i = 0; i < packet.data.length; i++){
            txbuf[3+i] = packet.data[i];
        }
        txbuf[3+packet.data.length] = (byte) 0x05A;
        while (!mBluetoothService.writeBytes(txbuf)){
            if(System.currentTimeMillis() - send_timer > BLE_WRITE_TIMEOUT){
                return false;
            }
        }
        return true;
    }
}
