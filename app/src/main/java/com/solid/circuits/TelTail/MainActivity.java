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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import android.text.BoringLayout;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import 	androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, SeekBar.OnSeekBarChangeListener, GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, Switch.OnClickListener, ServiceCallbacks, TabLayout.OnTabSelectedListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "TTLPrefsFile";

    NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF484848)
            .setPriority(Notification.PRIORITY_MAX);
    int mNotificationId = 750;

    View vLEDS;
    View vMOTOR;
    View vSENSOR;
    View vGPS;
    View vStatic;
    View vCycle;
    View vCompass;
    View vThrottle;
    View vRPM;
    View vRPMAccel;
    View vXAccel;
    View vYAccel;
    View vCustom;
    View vDigitalStatic;
    View vDigitalFuzz; // RGB animated static
    View vDigitalCycle;
    View vDigitalCompass;
    View vDigitalThrottle;
    View vDigitalRPM;
    View vDigitalRPMThrottle;
    View vDigitalCompassWheel;
    View vDigitalCompassSnake;

    CheckBox motorCheck1;
    CheckBox motorCheck2;
    CheckBox motorCheck3;
    CheckBox motorCheck4;
    CheckBox motorCheck5;
    CheckBox motorCheck6;
    CheckBox motorCheck7;
    CheckBox motorCheck8;
    CheckBox motorCheck9;
    CheckBox motorCheck10;

    CheckBox sensorCheck1;
    CheckBox sensorCheck2;
    CheckBox sensorCheck3;
    CheckBox sensorCheck4;
    CheckBox sensorCheck5;
    CheckBox sensorCheck6;
    CheckBox sensorCheck7;

    RelativeLayout AnalogStaticShuffleLayout;
    RelativeLayout AnalogCycleShuffleLayout;
    RelativeLayout AnalogCompassShuffleLayout;
    RelativeLayout AnalogThrottleShuffleLayout;
    RelativeLayout AnalogRPMShuffleLayout;
    RelativeLayout AnalogRPMThrottleShuffleLayout;
    RelativeLayout AnalogXAccelShuffleLayout;
    RelativeLayout AnalogYaccelShuffleLayout;
    RelativeLayout AnalogCustomShuffleLayout;
    RelativeLayout DigitalStaticShuffleLayout;
    RelativeLayout DigitalCycleShuffleLayout;
    RelativeLayout DigitalCompassShuffleLayout;
    RelativeLayout DigitalThrottleShuffleLayout;
    RelativeLayout DigitalRPMShuffleLayout;
    RelativeLayout DigitalRPMThrottleShuffleLayout;
    RelativeLayout DigitalSkittlesShuffleLayout;
    RelativeLayout DigitalCompassSnakeShuffleLayout;
    RelativeLayout DigitalCompassWheelShuffleLayout;

    private Menu menu;

    final int FAULT_CODE_NONE = 0;
    final int FAULT_CODE_OVER_VOLTAGE = 1;
    final int FAULT_CODE_UNDER_VOLTAGE = 2;
    final int FAULT_CODE_DRV8302 = 3;
    final int FAULT_CODE_ABS_OVER_CURRENT = 4;
    final int FAULT_CODE_OVER_TEMP_FET = 5;
    final int FAULT_CODE_OVER_TEMP_MOTOR = 6;

    // Graph Series for VESC data
    final int MAX_DATA_POINTS = 100;
    boolean BC = false;
    boolean BV = false;
    boolean MC = false;
    boolean TMP = false;
    boolean DTY = false;
    boolean RPM = false;
    boolean MAHU = false;
    boolean MAHC = false;
    boolean WHU = false;
    boolean WHC = false;
    boolean NUNCHUCK = false;
    boolean ACCEL = false;
    boolean GYRO = false;
    boolean COMP = false;
    boolean LGHT = false;
    boolean HEAD = false;
    boolean IMU_TEMP = false;
    double graph_index = 0;
    LineGraphSeries<DataPoint> BC_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> BV_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> MC_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> TMP_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> DTY_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> RPM_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> mahU_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> mahC_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> whU_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> whC_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> NX_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> NY_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> AX_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> AY_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> AZ_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> GX_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> GY_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> GZ_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> CX_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> CY_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> CZ_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> LGHT_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> HEAD_LineSeries = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> IMU_Temp_LinSeries = new LineGraphSeries<>();
    float battery_current = 0;
    float battery_voltage = 0;
    float previous_voltage = 0;
    int battery_update = 0;
    float motor_current = 0;
    float motor_temp = 0;
    float duty_cycle = 0;
    int motor_rpm = 0;
    float mAh_used = 0;
    float previous_mAh_used = 0;
    float mAh_charged = 0;
    float previous_mAh_charged = 0;
    float wh_used = 0;
    float previous_wh_used = 0;
    float wh_charged = 0;
    float previous_wh_charged = 0;
    int error_codes = 0;
    int remote_x = 0;
    int remote_y = 0;
    int remote_button = 0;
    int remote_connected = 0;
    int accel_x = 0;
    int accel_y = 0;
    int accel_z = 0;
    int gyro_x = 0;
    int gyro_y = 0;
    int gyro_z = 0;
    int compass_x = 0;
    int compass_y = 0;
    int compass_z = 0;
    int light_sense = 0;
    float heading = 0;
    float IMU_temp = 0;

    // LED Mode ui state variables
    int led_mode = 0;
    boolean led_switch_side = false;
    boolean led_switch_hb = false;
    boolean led_switch_light = false;
    boolean led_switch_sensor = false;
    int LED_STRIP_TYPE = 0; // 0:Analog 1:Digital_APA 2:Digital_SK98

    boolean STATIC_LINK = false;

    GraphView vMotorGraph;
    GraphView vSensorGraph;

    ScrollView vSensorScroll;

    int leftColor = 0xFF7F3FCC;
    int rightColor = 0xFF00CC99;
    int customLeftColor = 0xFF7F3FCC;
    int customRightColor = 0xFF00CC99;

    boolean AUX_PRESSED = false;

    private static final int MY_LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean mLocationPermissionDenied = false;

    private GoogleMap mMap;
    private UiSettings mMapSettings;
    public Polyline path;
    private List<LatLng> polyPoints = new ArrayList<LatLng>();
    private boolean NEW_PATH = true;
    private boolean BOARD_MOVING = false;
    private double longitude = 0;
    private double latitude = 0;
    private double pLat = 0;
    private double pLong = 0;
    private float distance = 0;
    private double maxSpeed = 0;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;
    public boolean autoConnect = false;
    private boolean RECIEVE_BLE_DATA = false;
    boolean READ_CURRENT_LED_SETTINGS = false;
    boolean READ_CURRENT_FW = false;
    boolean BEEN_CONNECTED = false;
    boolean SHUFFLE_LED_MODES = false;
    boolean STORE_SHUFFLE = false;

    public LoggingService mLoggingService;
    public boolean LOG_MAP = false;
    public boolean LOG_MOTOR = false;
    public boolean LOG_SENSOR = false;
    public boolean LOG_ENABLED = false;

    int ttl_error_code = 0;

    File versionFile;
    int Latest_FW = 0;
    int TTL_FW = 0;
    boolean FW_Download_Atempted = false;

    final byte fw_read[] = new byte[]{
            (byte) 0x0A5,
            (byte) 0x000,
            (byte) 0x0F9,
            (byte) 0x05A
    };

    boolean SHOW_SETUP_WIZARD = true;

    // Code to manage Service lifecycle.
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if (componentName.getClassName().equals(BluetoothService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding BLE", Toast.LENGTH_SHORT).show();
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                // Automatically connects to the device upon successful start-up initialization
                if (autoConnect && mBluetoothService.mBluetoothDeviceAddress != null) {                // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                    // BluetoothAdapter through BluetoothManager.
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = bluetoothManager.getAdapter();

                    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                    // fire an intent to display a dialog asking the user to grant permission to enable it.
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else
                        mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                }

            } else if (componentName.getClassName().equals(LoggingService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
                mLoggingService = ((LoggingService.LocalBinder) service).getService();
                if (mMap != null)
                    mLoggingService.mMap = mMap;

                mLoggingService.setCallbacks(MainActivity.this);
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
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TabLayout.Tab firstTab = tabLayout.newTab();
        firstTab.setIcon(R.mipmap.ic_led_white); // set an icon for the first tab
        tabLayout.addTab(firstTab); // add  the tab at in the TabLayout
        TabLayout.Tab secondTab = tabLayout.newTab();
        secondTab.setIcon(R.mipmap.ic_motor_white); // set an icon for the first tab
        tabLayout.addTab(secondTab); // add  the tab at in the TabLayout
        TabLayout.Tab thirdTab = tabLayout.newTab();
        thirdTab.setIcon(R.mipmap.ic_sensor_white); // set an icon for the first tab
        tabLayout.addTab(thirdTab); // add  the tab at in the TabLayout
        TabLayout.Tab forthTab = tabLayout.newTab();
        forthTab.setIcon(R.mipmap.ic_gps_white); // set an icon for the first tab
        tabLayout.addTab(forthTab); // add  the tab at in the TabLayout
        // perform setOnTabSelectedListener event on TabLayout
        tabLayout.setOnTabSelectedListener(this);

        vLEDS = findViewById(R.id.led_layout);
        vMOTOR = findViewById(R.id.motor_layout);
        vSENSOR = findViewById(R.id.sensor_layout);
        vGPS = findViewById(R.id.gps_layout);
        vStatic = findViewById(R.id.led_static_ui);
        vCycle = findViewById(R.id.led_cycle_ui);
        vCompass = findViewById(R.id.led_compass_ui);
        vThrottle = findViewById(R.id.led_throttle_ui);
        vRPM = findViewById(R.id.led_rpm_ui);
        vRPMAccel = findViewById(R.id.led_rpm_accel_ui);
        vXAccel = findViewById(R.id.led_x_accel_ui);
        vYAccel = findViewById(R.id.led_y_accel_ui);
        vCustom = findViewById(R.id.led_custom_ui);
        vDigitalStatic = findViewById(R.id.led_digital_static_ui);
        vDigitalFuzz = findViewById(R.id.led_digital_skittles_ui);// RGB animated static
        vDigitalCycle = findViewById(R.id.led_digital_cycle_ui);
        vDigitalCompass = findViewById(R.id.led_digital_compass_ui);
        vDigitalThrottle = findViewById(R.id.led_digital_throttle_ui);
        vDigitalRPM = findViewById(R.id.led_digital_rpm_ui);
        vDigitalRPMThrottle = findViewById(R.id.led_digital_rpm_throttle_ui);
        vDigitalCompassWheel = findViewById(R.id.led_digital_compass_wheel_ui);
        vDigitalCompassSnake = findViewById(R.id.led_digital_compass_snake_ui);

        AnalogStaticShuffleLayout = findViewById(R.id.static_shuffle_layout);
        AnalogCycleShuffleLayout = findViewById(R.id.cycle_shuffle_layout);
        AnalogCompassShuffleLayout = findViewById(R.id.compass_shuffle_layout);
        AnalogThrottleShuffleLayout = findViewById(R.id.throttle_shuffle_layout);
        AnalogRPMShuffleLayout = findViewById(R.id.rpm_shuffle_layout);
        AnalogRPMThrottleShuffleLayout = findViewById(R.id.rpm_throttle_shuffle_layout);
        AnalogXAccelShuffleLayout = findViewById(R.id.x_accel_shuffle_layout);
        AnalogYaccelShuffleLayout = findViewById(R.id.y_accel_shuffle_layout);
        AnalogCustomShuffleLayout = findViewById(R.id.custom_shuffle_layout);
        DigitalStaticShuffleLayout = findViewById(R.id.digital_static_shuffle_layout);
        DigitalCycleShuffleLayout = findViewById(R.id.digital_cycle_shuffle_layout);
        DigitalCompassShuffleLayout = findViewById(R.id.digital_compass_shuffle_layout);
        DigitalThrottleShuffleLayout = findViewById(R.id.digital_throttle_shuffle_layout);
        DigitalRPMShuffleLayout = findViewById(R.id.digital_rpm_shuffle_layout);
        DigitalRPMThrottleShuffleLayout = findViewById(R.id.digital_rpm_throttle_shuffle_layout);
        DigitalSkittlesShuffleLayout = findViewById(R.id.digital_skittles_shuffle_layout);
        DigitalCompassSnakeShuffleLayout = findViewById(R.id.digital_compass_snake_shuffle_layout);
        DigitalCompassWheelShuffleLayout = findViewById(R.id.digital_compass_wheel_shuffle_layout);

        if (LED_STRIP_TYPE == 0) {
            vStatic.setVisibility(View.VISIBLE);
            vDigitalStatic.setVisibility(View.GONE);
        } else {
            vDigitalStatic.setVisibility(View.VISIBLE);
            vStatic.setVisibility(View.GONE);
        }
        vLEDS.setVisibility(View.VISIBLE);
        vMOTOR.setVisibility(View.GONE);
        vSENSOR.setVisibility(View.GONE);
        vGPS.setVisibility(View.GONE);
        vCycle.setVisibility(View.GONE);
        vCompass.setVisibility(View.GONE);
        vThrottle.setVisibility(View.GONE);
        vRPM.setVisibility(View.GONE);
        vRPMAccel.setVisibility(View.GONE);
        vXAccel.setVisibility(View.GONE);
        vYAccel.setVisibility(View.GONE);
        vCustom.setVisibility(View.GONE);
        vDigitalFuzz.setVisibility(View.GONE); // RGB animated static
        vDigitalCycle.setVisibility(View.GONE);
        vDigitalCompass.setVisibility(View.GONE);
        vDigitalThrottle.setVisibility(View.GONE);
        vDigitalRPM.setVisibility(View.GONE);
        vDigitalRPMThrottle.setVisibility(View.GONE);

        Spinner modeSpinner = (Spinner) findViewById(R.id.modes_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> modeAdapter;
        if (LED_STRIP_TYPE == 0) {
            modeAdapter = ArrayAdapter.createFromResource(this,
                    R.array.analog_modes_array, android.R.layout.simple_spinner_item);
        } else {
            modeAdapter = ArrayAdapter.createFromResource(this,
                    R.array.digital_modes_array, android.R.layout.simple_spinner_item);
        }
        // Specify the layout to use when the list of choices appears
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setOnItemSelectedListener(this);

        Spinner gpsSpinner = (Spinner) findViewById(R.id.gps_spinner);
        ArrayAdapter<CharSequence> gps_adapter = ArrayAdapter.createFromResource(this,
                R.array.gps_array, R.layout.gps_spinner_item);
        gps_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        gpsSpinner.setAdapter(gps_adapter);
        gpsSpinner.setOnItemSelectedListener(this);

        Spinner rateBaseSpinner = (Spinner) findViewById(R.id.custom_rate_select_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> rateAdapter = ArrayAdapter.createFromResource(this,
                R.array.rate_base_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        rateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        rateBaseSpinner.setAdapter(rateAdapter);
        rateBaseSpinner.setOnItemSelectedListener(this);

        Spinner brightBaseSpinner = (Spinner) findViewById(R.id.custom_brightness_select_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> brightAdapter = ArrayAdapter.createFromResource(this,
                R.array.bright_base_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        brightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        brightBaseSpinner.setAdapter(brightAdapter);
        brightBaseSpinner.setOnItemSelectedListener(this);

        Spinner colorBaseSpinner = (Spinner) findViewById(R.id.custom_color_select_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.color_base_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        colorBaseSpinner.setAdapter(colorAdapter);
        colorBaseSpinner.setOnItemSelectedListener(this);

        SeekBar custom_bright_seek = findViewById(R.id.custom_brightness_seeker);
        SeekBar custom_rate_seek =  findViewById(R.id.custom_rate_seeker);
        SeekBar custom_left_red_seek =  findViewById(R.id.custom_left_red_seeker);
        SeekBar custom_left_green_seek =  findViewById(R.id.custom_left_green_seeker);
        SeekBar custom_left_blue_seek =  findViewById(R.id.custom_left_blue_seeker);
        SeekBar custom_right_red_seek =  findViewById(R.id.custom_right_red_seeker);
        SeekBar custom_right_green_seek =  findViewById(R.id.custom_right_green_seeker);
        SeekBar custom_right_blue_seek =  findViewById(R.id.custom_right_blue_seeker);
        SeekBar left_red_seek =  findViewById(R.id.left_red_seeker);
        SeekBar left_green_seek =  findViewById(R.id.left_green_seeker);
        SeekBar left_blue_seek =  findViewById(R.id.left_blue_seeker);
        SeekBar right_red_seek =  findViewById(R.id.right_red_seeker);
        SeekBar right_green_seek =  findViewById(R.id.right_green_seeker);
        SeekBar right_blue_seek =  findViewById(R.id.right_blue_seeker);
        SeekBar cycle_speed_seek =  findViewById(R.id.cycle_speed_seeker);
        SeekBar cycle_bright_seek =  findViewById(R.id.cycle_bright_seeker);
        SeekBar comp_bright_seek =  findViewById(R.id.compass_bright_seeker);
        SeekBar x_accel_speed_seek =  findViewById(R.id.throttle_speed_seeker);
        SeekBar y_accel_speed_seek =  findViewById(R.id.y_accel_speed_seeker);
        SeekBar x_accel_bright_seek =  findViewById(R.id.throttle_bright_seeker);
        SeekBar rpm_speed_seek =  findViewById(R.id.rpm_speed_seeker);
        SeekBar x_accel_rate_seeker =  findViewById(R.id.x_accel_rate_seeker);
        SeekBar digital_static_zoom_seeker =  findViewById(R.id.digital_static_zoom_seeker);
        SeekBar digital_static_shift_seeker =  findViewById(R.id.digital_static_shift_seeker);
        SeekBar digital_static_bright_seeker =  findViewById(R.id.digital_static_bright_seeker);
        SeekBar digital_skittles_bright_seeker =  findViewById(R.id.digital_skittles_bright_seeker);
        SeekBar digital_cycle_rate_seeker =  findViewById(R.id.digital_cycle_rate_seeker);
        SeekBar digital_cycle_bright_seeker =  findViewById(R.id.digital_cycle_bright_seeker);
        SeekBar digital_compass_bright_seeker =  findViewById(R.id.digital_compass_bright_seeker);
        SeekBar digital_throttle_zoom_seeker =  findViewById(R.id.digital_throttle_zoom_seeker);
        SeekBar digital_throttle_shift_seeker =  findViewById(R.id.digital_throttle_shift_seeker);
        SeekBar digital_throttle_sens_seeker =  findViewById(R.id.digital_throttle_sens_seeker);
        SeekBar digital_throttle_bright_seeker =  findViewById(R.id.digital_throttle_bright_seeker);
        SeekBar digital_rpm_rate_seeker =  findViewById(R.id.digital_rpm_rate_seeker);
        SeekBar digital_rpm_bright_seeker =  findViewById(R.id.digital_rpm_bright_seeker);
        SeekBar digital_cycle_zoom_seeker =  findViewById(R.id.digital_cycle_zoom_seeker);
        SeekBar digital_rpm_zoom_seeker =  findViewById(R.id.digital_rpm_zoom_seeker);
        custom_bright_seek.setOnSeekBarChangeListener(this);
        custom_rate_seek.setOnSeekBarChangeListener(this);
        custom_left_red_seek.setOnSeekBarChangeListener(this);
        custom_left_green_seek.setOnSeekBarChangeListener(this);
        custom_left_blue_seek.setOnSeekBarChangeListener(this);
        custom_right_red_seek.setOnSeekBarChangeListener(this);
        custom_right_green_seek.setOnSeekBarChangeListener(this);
        custom_right_blue_seek.setOnSeekBarChangeListener(this);
        left_red_seek.setOnSeekBarChangeListener(this);
        left_green_seek.setOnSeekBarChangeListener(this);
        left_blue_seek.setOnSeekBarChangeListener(this);
        right_red_seek.setOnSeekBarChangeListener(this);
        right_green_seek.setOnSeekBarChangeListener(this);
        right_blue_seek.setOnSeekBarChangeListener(this);
        cycle_speed_seek.setOnSeekBarChangeListener(this);
        cycle_bright_seek.setOnSeekBarChangeListener(this);
        comp_bright_seek.setOnSeekBarChangeListener(this);
        x_accel_speed_seek.setOnSeekBarChangeListener(this);
        y_accel_speed_seek.setOnSeekBarChangeListener(this);
        x_accel_bright_seek.setOnSeekBarChangeListener(this);
        rpm_speed_seek.setOnSeekBarChangeListener(this);
        x_accel_rate_seeker.setOnSeekBarChangeListener(this);
        digital_static_zoom_seeker.setOnSeekBarChangeListener(this);
        digital_static_shift_seeker.setOnSeekBarChangeListener(this);
        digital_static_bright_seeker.setOnSeekBarChangeListener(this);
        digital_cycle_rate_seeker.setOnSeekBarChangeListener(this);
        digital_cycle_bright_seeker.setOnSeekBarChangeListener(this);
        digital_compass_bright_seeker.setOnSeekBarChangeListener(this);
        digital_throttle_zoom_seeker.setOnSeekBarChangeListener(this);
        digital_throttle_shift_seeker.setOnSeekBarChangeListener(this);
        digital_throttle_sens_seeker.setOnSeekBarChangeListener(this);
        digital_throttle_bright_seeker.setOnSeekBarChangeListener(this);
        digital_rpm_rate_seeker.setOnSeekBarChangeListener(this);
        digital_rpm_bright_seeker.setOnSeekBarChangeListener(this);
        digital_skittles_bright_seeker.setOnSeekBarChangeListener(this);
        digital_cycle_zoom_seeker.setOnSeekBarChangeListener(this);
        digital_rpm_zoom_seeker.setOnSeekBarChangeListener(this);
        View left_rgb = findViewById(R.id.left_rgb_display);
        View right_rgb = findViewById(R.id.right_rgb_display);
        left_rgb.setBackgroundColor(0xFF7F3FCC);
        right_rgb.setBackgroundColor(0xFF00CC99);
        View custom_left_rgb = findViewById(R.id.custom_left_rgb_display);
        View custom_right_rgb = findViewById(R.id.custom_right_rgb_display);
        custom_left_rgb.setBackgroundColor(0xFF7F3FCC);
        custom_right_rgb.setBackgroundColor(0xFF00CC99);

        vSensorGraph = (GraphView) findViewById(R.id.sensor_graph);
        vMotorGraph = (GraphView) findViewById(R.id.motor_graph);
        // activate horizontal zooming and scrolling
        vMotorGraph.getViewport().setScalable(true);
        vSensorGraph.getViewport().setScalable(true);

        // activate horizontal scrolling
        vMotorGraph.getViewport().setScrollable(true);
        vSensorGraph.getViewport().setScrollable(true);

        // set X max and min
        vMotorGraph.getViewport().setMinX(0);
        vMotorGraph.getViewport().setMaxX(100);
        vSensorGraph.getViewport().setMinX(0);
        vSensorGraph.getViewport().setMaxX(100);

        // set Y max
        //vMotorGraph.getViewport().setMaxY(10000);
        //vSensorGraph.getViewport().setMaxY(10000);

        // set Label style
        vMotorGraph.getGridLabelRenderer().setTextSize(30);
        vMotorGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        vMotorGraph.getGridLabelRenderer().setNumVerticalLabels(6);
        vMotorGraph.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.LEFT);
        vSensorGraph.getGridLabelRenderer().setTextSize(30);
        vSensorGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        vSensorGraph.getGridLabelRenderer().setNumVerticalLabels(6);
        vSensorGraph.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.LEFT);

        BC_LineSeries.setTitle("Battery Current");
        BC_LineSeries.setColor(0xFFFF0000);
        BV_LineSeries.setTitle("Battery Voltage");
        BV_LineSeries.setColor(0xFF00FF00);
        MC_LineSeries.setTitle("Motor Current");
        MC_LineSeries.setColor(0xFF0000FF);
        TMP_LineSeries.setTitle("Temp");
        TMP_LineSeries.setColor(0xFFFFFF00);
        DTY_LineSeries.setTitle("Duty Cycle");
        DTY_LineSeries.setColor(0xFFFF00FF);
        RPM_LineSeries.setTitle("RPM");
        RPM_LineSeries.setColor(0xFF00FFFF);
        mahU_LineSeries.setTitle("mAh Used");
        mahU_LineSeries.setColor(0xFF9966FF);
        mahC_LineSeries.setTitle("mAh Charged");
        mahC_LineSeries.setColor(0xFFFAE583);
        whU_LineSeries.setTitle("Wh Used");
        whU_LineSeries.setColor(0xFF913F55);
        whC_LineSeries.setTitle("Wh Charged");
        whC_LineSeries.setColor(0xFFFF9900);
        NX_LineSeries.setTitle("Nun X");
        NX_LineSeries.setColor(0xFFF9CFCF);
        NY_LineSeries.setTitle("Nun Y");
        NY_LineSeries.setColor(0xFFF26544);
        AX_LineSeries.setTitle("Accel X");
        AX_LineSeries.setColor(0xFF00F6FF);
        AY_LineSeries.setTitle("Accel Y");
        AY_LineSeries.setColor(0xFFFAE583);
        AZ_LineSeries.setTitle("Accel Z");
        AZ_LineSeries.setColor(0xFF913F55);
        GX_LineSeries.setTitle("Gyro X");
        GX_LineSeries.setColor(0xFFFF9900);
        GY_LineSeries.setTitle("Gyro Y");
        GY_LineSeries.setColor(0xFF9966FF);
        GZ_LineSeries.setTitle("Gyro Z");
        GZ_LineSeries.setColor(0xFFFFFF99);
        CX_LineSeries.setTitle("Comp X");
        CX_LineSeries.setColor(0xFF330000);
        CY_LineSeries.setTitle("Comp Y");
        CY_LineSeries.setColor(0xFF003333);
        CZ_LineSeries.setTitle("Comp Z");
        CZ_LineSeries.setColor(0xFF666600);
        LGHT_LineSeries.setTitle("Light");
        LGHT_LineSeries.setColor(0xFF00FF99);
        HEAD_LineSeries.setTitle("Heading");
        HEAD_LineSeries.setColor(0xDFA9DF59);
        IMU_Temp_LinSeries.setTitle("IMU Temp");
        IMU_Temp_LinSeries.setColor(0xFF006666);

        updateSensorGraphContent();
        updateMotorGraphContent();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        vSensorScroll = (ScrollView) findViewById(R.id.sensor_main_scroll);
        ((WorkaroundMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .setListener(new WorkaroundMapFragment.OnTouchListener() {
                    @Override
                    public void onTouch() {
                        vSensorScroll.requestDisallowInterceptTouchEvent(true);
                    }
                });

        motorCheck1 = (CheckBox) findViewById(R.id.motor_bcurrent_check);
        motorCheck2 = (CheckBox) findViewById(R.id.motor_duty_check);
        motorCheck3 = (CheckBox) findViewById(R.id.motor_mahc_check);
        motorCheck4 = (CheckBox) findViewById(R.id.motor_mahu_check);
        motorCheck5 = (CheckBox) findViewById(R.id.motor_mcurrent_check);
        motorCheck6 = (CheckBox) findViewById(R.id.motor_rpm_check);
        motorCheck7 = (CheckBox) findViewById(R.id.motor_temp_check);
        motorCheck8 = (CheckBox) findViewById(R.id.motor_volt_check);
        motorCheck9 = (CheckBox) findViewById(R.id.motor_whc_check);
        motorCheck10 = (CheckBox) findViewById(R.id.motor_whu_check);

        sensorCheck1 = (CheckBox) findViewById(R.id.sensor_accel_check);
        sensorCheck2 = (CheckBox) findViewById(R.id.sensor_gyro_check);
        sensorCheck3 = (CheckBox) findViewById(R.id.sensor_compass_check);
        sensorCheck4 = (CheckBox) findViewById(R.id.sensor_heading_check);
        sensorCheck5 = (CheckBox) findViewById(R.id.sensor_light_check);
        sensorCheck6 = (CheckBox) findViewById(R.id.sensor_nunchuck_joy_check);
        sensorCheck7 = (CheckBox) findViewById(R.id.sensor_temp_check);

        File directory = new File(getFilesDir().toString());
        versionFile = new File(directory.toString() + "/FW Version.txt");

        loadPreferences();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        Intent LogServiceIntent = new Intent(this, LoggingService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        bindService(LogServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if(SHOW_SETUP_WIZARD){
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("It appears you have not run the setup wizard yet. Would you like to open the setup wizard to easily configure your TTL system?")
                    .setTitle("Setup Wizard")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(MainActivity.this, SetupWizardActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setNeutralButton("Never", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SHOW_SETUP_WIZARD = false;
                            savePreferences();
                            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean("runWizard", SHOW_SETUP_WIZARD);
                            editor.commit();
                        }
                    })
                    .setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .setCancelable(true)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        RECIEVE_BLE_DATA = true;
        loadPreferences();

        Spinner ModeSpinner = (Spinner) findViewById(R.id.modes_spinner);
        ModeSpinner.setSelection(led_mode);

        if (menu != null) {
            MenuItem bleAction = menu.findItem(R.id.action_ble);
            MenuItem logAction = menu.findItem(R.id.action_log);

            if (mBluetoothService != null && mBluetoothService.mConnectionState == 2) {
                bleAction.setTitle("Disconnect BLE");
                bleAction.setEnabled(true);
            } else {
                bleAction.setTitle("Connect BLE");
                bleAction.setEnabled(true);
            }

            if (LOG_ENABLED) {
                logAction.setVisible(true);

                if (mLoggingService != null && mLoggingService.LOG_STARTED) {
                    logAction.setTitle("Stop Logging");
                } else {
                    logAction.setTitle("Start Logging");
                }
            } else {
                logAction.setVisible(false);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        RECIEVE_BLE_DATA = false;

        savePreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();

        RECIEVE_BLE_DATA = false;

        savePreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothService = null;
        mLoggingService = null;

        savePreferences();

        unregisterReceiver(mGattUpdateReceiver);
    }

    private void loadPreferences() {
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        autoConnect = settings.getBoolean("bleConnect", false);
        LOG_MAP = settings.getBoolean("LogMap", false);
        LOG_MOTOR = settings.getBoolean("LogMotor", false);
        LOG_SENSOR = settings.getBoolean("LogSensor", false);
        LOG_ENABLED = settings.getBoolean("LogEnable", false);
        int MotorChecks = settings.getInt("MotorChecks", 0);
        int SensorChecks = settings.getInt("SensorChecks", 0);
        previous_voltage = settings.getFloat("BattVolt", 0);
        previous_mAh_used = settings.getFloat("mAhUsed", 0);
        previous_mAh_charged = settings.getFloat("mAhUsed", 0);
        previous_wh_used = settings.getFloat("WhUsed", 0);
        previous_wh_charged = settings.getFloat("WhUsed", 0);
        STATIC_LINK = settings.getBoolean("staticLink", false);
        READ_CURRENT_LED_SETTINGS = settings.getBoolean("ReadCurrentLED",false);
        READ_CURRENT_FW = settings.getBoolean("fwAutoCheck",false);
        SHUFFLE_LED_MODES = settings.getBoolean("ShuffleEnable",false);
        SHOW_SETUP_WIZARD = settings.getBoolean("runWizard",true);
        //led_mode = settings.getInt("LEDmode", 0);
        //led_switch_hb = settings.getBoolean("",false);

        LED_STRIP_TYPE = settings.getInt("RGBType", 0);

        updateModeSpinner();
        updateLEDui();

        WHU = ((MotorChecks & 0x200) == 0x200);
        WHC = ((MotorChecks & 0x100) == 0x100);
        BV = ((MotorChecks & 0x80) == 0x80);
        TMP = ((MotorChecks & 0x40) == 0x40);
        RPM = ((MotorChecks & 0x20) == 0x20);
        MC = ((MotorChecks & 0x10) == 0x10);
        MAHU = ((MotorChecks & 0x8) == 0x8);
        MAHC = ((MotorChecks & 0x4) == 0x4);
        DTY = ((MotorChecks & 0x2) == 0x2);
        BC = ((MotorChecks & 0x1) == 0x1);

        IMU_TEMP = ((SensorChecks & 0x40) == 0x40);
        NUNCHUCK = ((SensorChecks & 0x20) == 0x20);
        LGHT = ((SensorChecks & 0x10) == 0x10);
        HEAD = ((SensorChecks & 0x8) == 0x8);
        COMP = ((SensorChecks & 0x4) == 0x4);
        GYRO = ((SensorChecks & 0x2) == 0x2);
        ACCEL = ((SensorChecks & 0x1) == 0x1);

        updateMotorGraphContent();
        updateSensorGraphContent();

        motorCheck1.setChecked(BC);
        motorCheck2.setChecked(DTY);
        motorCheck3.setChecked(MAHC);
        motorCheck4.setChecked(MAHU);
        motorCheck5.setChecked(MC);
        motorCheck6.setChecked(RPM);
        motorCheck7.setChecked(TMP);
        motorCheck8.setChecked(BV);
        motorCheck9.setChecked(WHC);
        motorCheck10.setChecked(WHU);

        sensorCheck1.setChecked(ACCEL);
        sensorCheck2.setChecked(GYRO);
        sensorCheck3.setChecked(COMP);
        sensorCheck4.setChecked(HEAD);
        sensorCheck5.setChecked(LGHT);
        sensorCheck6.setChecked(NUNCHUCK);
        sensorCheck7.setChecked(IMU_TEMP);

        ToggleButton MapLog = (ToggleButton) findViewById(R.id.gps_log_button);
        ToggleButton MotorLog = (ToggleButton) findViewById(R.id.motor_log_button);
        ToggleButton SensorLog = (ToggleButton) findViewById(R.id.sensor_log_button);

        MapLog.setChecked(LOG_MAP);
        MotorLog.setChecked(LOG_MOTOR);
        SensorLog.setChecked(LOG_SENSOR);

        CheckBox static_link = (CheckBox) findViewById(R.id.LED_static_link_check);
        static_link.setChecked(STATIC_LINK);

        TextView left_title = (TextView) findViewById(R.id.left_LED_text);
        RelativeLayout right_layout = findViewById(R.id.right_LED_layout);
        if (static_link.isChecked()) {
            left_title.setText("LED Color");
            right_layout.setVisibility(View.GONE);
        } else {
            left_title.setText("Left LED Color");
            right_layout.setVisibility(View.VISIBLE);
        }

        if (LOG_ENABLED) {
            MapLog.setVisibility(View.VISIBLE);
            MotorLog.setVisibility(View.VISIBLE);
            SensorLog.setVisibility(View.VISIBLE);
        } else {
            MapLog.setVisibility(View.GONE);
            MotorLog.setVisibility(View.GONE);
            SensorLog.setVisibility(View.GONE);
        }

        updateShuffleCheckVisibility();
    }

    private void savePreferences() {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        int MotorCheck1 = (BC ? 1 : 0);
        int MotorCheck2 = (DTY ? 1 : 0);
        int MotorCheck3 = (MAHC ? 1 : 0);
        int MotorCheck4 = (MAHU ? 1 : 0);
        int MotorCheck5 = (MC ? 1 : 0);
        int MotorCheck6 = (RPM ? 1 : 0);
        int MotorCheck7 = (TMP ? 1 : 0);
        int MotorCheck8 = (BV ? 1 : 0);
        int MotorCheck9 = (WHC ? 1 : 0);
        int MotorCheck10 = (WHU ? 1 : 0);

        int SensorCheck1 = (ACCEL ? 1 : 0);
        int SensorCheck2 = (GYRO ? 1 : 0);
        int SensorCheck3 = (COMP ? 1 : 0);
        int SensorCheck4 = (HEAD ? 1 : 0);
        int SensorCheck5 = (LGHT ? 1 : 0);
        int SensorCheck6 = (NUNCHUCK ? 1 : 0);
        int SensorCheck7 = (IMU_TEMP ? 1 : 0);

        int MotorChecks = (MotorCheck10 << 9) | (MotorCheck9 << 8) | (MotorCheck8 << 7) | (MotorCheck7 << 6) | (MotorCheck6 << 5) | (MotorCheck5 << 4) | (MotorCheck4 << 3) | (MotorCheck3 << 2) | (MotorCheck2 << 1) | (MotorCheck1);
        int SensorChecks = (SensorCheck7 << 6) | (SensorCheck6 << 5) | (SensorCheck5 << 4) | (SensorCheck4 << 3) | (SensorCheck3 << 2) | (SensorCheck2 << 1) | (SensorCheck1);
        editor.putInt("MotorChecks", MotorChecks);
        editor.putInt("SensorChecks", SensorChecks);
        editor.putBoolean("staticLink", STATIC_LINK);

        save_LED_mode_values(editor);

        if (mBluetoothService != null && mBluetoothService.mConnectionState == 2 && battery_voltage > 0) {
            editor.putFloat("BattVolt", battery_voltage);
            editor.putFloat("mAhUsed", mAh_used + previous_mAh_used);
            editor.putFloat("mAhCharged", mAh_charged + previous_mAh_charged);
            editor.putFloat("WhUsed", wh_used + previous_wh_used);
            editor.putFloat("WhCharged", wh_charged + previous_wh_charged);
        }
        if(STORE_SHUFFLE) {
            editor.putBoolean("ShuffleEnable", SHUFFLE_LED_MODES);
            STORE_SHUFFLE = false;
        }
        // Commit the edits!
        editor.commit();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mLocationPermissionDenied) {
            PermissionUtils.PermissionDeniedDialog
                    .newInstance(false).show(getSupportFragmentManager(), "dialog");
            mLocationPermissionDenied = false;
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem bleAction = menu.findItem(R.id.action_ble);
        MenuItem logAction = menu.findItem(R.id.action_log);

        if (mBluetoothService != null && mBluetoothService.mConnectionState == 2) {
            bleAction.setTitle("Disconnect BLE");
            bleAction.setEnabled(true);
        } else {
            bleAction.setTitle("Connect BLE");
            bleAction.setEnabled(true);
        }

        if (LOG_ENABLED) {
            logAction.setVisible(true);

            if (mLoggingService != null && mLoggingService.LOG_STARTED) {
                logAction.setTitle("Stop Logging");
            } else {
                logAction.setTitle("Start Logging");
            }
        } else {
            logAction.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        MenuItem bleAction = menu.findItem(R.id.action_ble);
        MenuItem logAction = menu.findItem(R.id.action_log);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        } else if (id == R.id.action_calibrate) {
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
                                Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        } else if (id == R.id.action_toggle) {
            final byte txbuf[] = new byte[]{
                    (byte) 0x0A5,
                    (byte) 0x000,
                    (byte) 0x0E3,
                    (byte) 0x05A
            };
            if (!mBluetoothService.writeBytes(txbuf))
                Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            //String txstring = bytesToHex(txbuf);
            // Toast.makeText(MainActivity.this, txstring, Toast.LENGTH_SHORT).show();

            return true;
        } else if (id == R.id.action_ble) {
            if (mBluetoothService.mBluetoothDeviceAddress != null) {
                //Log.i(TAG,"CONNECT/DISCONNECT PRESSED");
                // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                // BluetoothAdapter through BluetoothManager.
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();

                if (mBluetoothService.mConnectionState == 0) {
                    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                    // fire an intent to display a dialog asking the user to grant permission to enable it.
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        bleAction.setTitle("Connecting BLE");
                        bleAction.setEnabled(false);
                        //Log.i(TAG, "CONNECTING");
                        mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                    }
                } else if (mBluetoothService.mConnectionState == 2) {
                    bleAction.setTitle("Disconnecting BLE");
                    bleAction.setEnabled(false);
                    mBluetoothService.disconnect();
                } else {
                    Toast.makeText(MainActivity.this, "Connecting: Please wait", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
            }

            return true;
        } else if (id == R.id.action_log) {
            //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            //Menu navMenu = navigationView.getMenu();
            //MenuItem logStatus = navMenu.findItem(R.id.logging_status);

            mLoggingService.ToggleLogging();
            if (mLoggingService.LOG_STARTED) {
                logAction.setTitle("Stop Logging");
            } else {
                logAction.setTitle("Start Logging");
            }
            return true;
        } else if (id == R.id.action_remote) {
            Intent intent = new Intent(this, RemoteActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Function to show settings alert dialog
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    public void showBatteryUpdateAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Battery Update");

        // Setting Dialog Message
        if (battery_update == 1)
            alertDialog.setMessage("A change in battery voltage was detected. Has the battery been recently charged?");
        else if (battery_update == 2)
            alertDialog.setMessage("A change in battery voltage was detected. Has the board been run without being connected?");

        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);

        // On pressing yes button
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (battery_update == 1) {
                    Toast.makeText(MainActivity.this, "Stored battery stats have been deleted", Toast.LENGTH_LONG).show();
                    previous_mAh_charged = 0;
                    previous_mAh_used = 0;
                    previous_wh_used = 0;
                    previous_wh_charged = 0;
                } else if (battery_update == 2) {
                    Toast.makeText(MainActivity.this, "Battery stats kept\nThey will be slightly off now", Toast.LENGTH_LONG).show();
                }
            }
        });

        // on pressing no button
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (battery_update == 1) {
                    dialog.cancel();
                } else if (battery_update == 2) {
                    Toast.makeText(MainActivity.this, "This may be a sign of battery issues\nPlease your battery condition", Toast.LENGTH_SHORT).show();
                    dialog.cancel();
                }
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        RelativeLayout static_layout = (RelativeLayout) findViewById(R.id.custom_static_layout);
        RelativeLayout bright_layout = (RelativeLayout) findViewById(R.id.custom_bright_layout);
        RelativeLayout rate_layout = (RelativeLayout) findViewById(R.id.custom_rate_layout);
        Spinner rate_spinner = (Spinner) findViewById(R.id.custom_rate_select_spinner);
        Spinner color_spinner = (Spinner) findViewById(R.id.custom_color_select_spinner);

        long parent_id = parent.getId();
        if (parent_id == R.id.modes_spinner) {
            led_mode = pos;
            updateLEDui();
        } else if (parent_id == R.id.custom_color_select_spinner) {
            if (pos == 0) {
                static_layout.setVisibility(View.VISIBLE);
                rate_spinner.setEnabled(false);
                rate_layout.setVisibility(View.GONE);
            } else if (pos == 2) {
                static_layout.setVisibility(View.GONE);
                rate_spinner.setEnabled(false);
                rate_layout.setVisibility(View.GONE);
            } else {
                static_layout.setVisibility(View.GONE);
                rate_spinner.setEnabled(true);
                if (rate_spinner.getSelectedItemPosition() == 0)
                    rate_layout.setVisibility(View.VISIBLE);
                else
                    rate_layout.setVisibility(View.GONE);
            }
        } else if (parent_id == R.id.custom_brightness_select_spinner) {
            if (pos == 0) {
                bright_layout.setVisibility(View.VISIBLE);
            } else {
                bright_layout.setVisibility(View.GONE);
            }
        } else if (parent_id == R.id.custom_rate_select_spinner) {
            if (pos == 0 && color_spinner.getSelectedItemPosition() != 0) {
                rate_layout.setVisibility(View.VISIBLE);
            } else {
                rate_layout.setVisibility(View.GONE);
            }
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView custom_bright_number =  findViewById(R.id.custom_brightness_number);
        TextView custom_rate_number =  findViewById(R.id.custom_rate_number);
        TextView custom_left_red_number =  findViewById(R.id.custom_left_red_number);
        TextView custom_left_green_number =  findViewById(R.id.custom_left_green_number);
        TextView custom_left_blue_number =  findViewById(R.id.custom_left_blue_number);
        TextView custom_right_red_number =  findViewById(R.id.custom_right_red_number);
        TextView custom_right_green_number =  findViewById(R.id.custom_right_green_number);
        TextView custom_right_blue_number =  findViewById(R.id.custom_right_blue_number);
        TextView left_red_number =  findViewById(R.id.left_red_number);
        TextView left_green_number =  findViewById(R.id.left_green_number);
        TextView left_blue_number =  findViewById(R.id.left_blue_number);
        TextView right_red_number =  findViewById(R.id.right_red_number);
        TextView right_green_number =  findViewById(R.id.right_green_number);
        TextView right_blue_number =  findViewById(R.id.right_blue_number);
        TextView cycle_speed_number =  findViewById(R.id.cycle_speed_number);
        TextView cycle_bright_number =  findViewById(R.id.cycle_bright_number);
        TextView comp_bright_number =  findViewById(R.id.compass_bright_number);
        TextView throttle_rate_number =  findViewById(R.id.throttle_speed_number);
        TextView throttle_bright_number =  findViewById(R.id.throttle_bright_number);
        TextView rpm_sens_number =  findViewById(R.id.rpm_speed_number);
        View left_rgb = findViewById(R.id.left_rgb_display);
        View right_rgb = findViewById(R.id.right_rgb_display);
        TextView x_accel_sens_number =  findViewById(R.id.x_accel_rate_number);
        TextView y_accel_sens_number =  findViewById(R.id.y_accel_speed_number);
        View custom_left_rgb = findViewById(R.id.custom_left_rgb_display);
        View custom_right_rgb = findViewById(R.id.custom_right_rgb_display);
        TextView digital_static_zoom_number =  findViewById(R.id.digital_static_zoom_number);
        TextView digital_static_shift_number =  findViewById(R.id.digital_static_shift_number);
        TextView digital_static_bright_number =  findViewById(R.id.digital_static_bright_number);
        TextView digital_skittles_bright_number =  findViewById(R.id.digital_skittles_bright_number);
        TextView digital_cycle_rate_number =  findViewById(R.id.digital_cycle_rate_number);
        TextView digital_cycle_bright_number =  findViewById(R.id.digital_cycle_bright_number);
        TextView digital_compass_bright_number =  findViewById(R.id.digital_compass_bright_number);
        TextView digital_throttle_zoom_number =  findViewById(R.id.digital_throttle_zoom_number);
        TextView digital_throttle_shift_number =  findViewById(R.id.digital_throttle_shift_number);
        TextView digital_throttle_sens_number =  findViewById(R.id.digital_throttle_sens_number);
        TextView digital_throttle_bright_number =  findViewById(R.id.digital_throttle_bright_number);
        TextView digital_rpm_zoom_number =  findViewById(R.id.digital_rpm_zoom_number);
        TextView digital_rpm_rate_number =  findViewById(R.id.digital_rpm_rate_number);
        TextView digital_rpm_bright_number =  findViewById(R.id.digital_rpm_bright_number);
        TextView digital_cycle_zoom_number =  findViewById(R.id.digital_cycle_zoom_number);


        if (seekBar.getId() == R.id.left_red_seeker) {
            left_red_number.setText("" + progress);
            leftColor = (leftColor & 0xFF00FFFF) | ((int) (progress * 2.55) << 16);
            left_rgb.setBackgroundColor(leftColor);
        } else if (seekBar.getId() == R.id.left_green_seeker) {
            left_green_number.setText("" + progress);
            leftColor = (leftColor & 0xFFFF00FF) | ((int) (progress * 2.55) << 8);
            left_rgb.setBackgroundColor(leftColor);
        } else if (seekBar.getId() == R.id.left_blue_seeker) {
            left_blue_number.setText("" + progress);
            leftColor = (leftColor & 0xFFFFFF00) | ((int) (progress * 2.55));
            left_rgb.setBackgroundColor(leftColor);
        } else if (seekBar.getId() == R.id.right_red_seeker) {
            right_red_number.setText("" + progress);
            rightColor = (rightColor & 0xFF00FFFF) | ((int) (progress * 2.55) << 16);
            right_rgb.setBackgroundColor(rightColor);
        } else if (seekBar.getId() == R.id.right_green_seeker) {
            right_green_number.setText("" + progress);
            rightColor = (rightColor & 0xFFFF00FF) | ((int) (progress * 2.55) << 8);
            right_rgb.setBackgroundColor(rightColor);
        } else if (seekBar.getId() == R.id.right_blue_seeker) {
            right_blue_number.setText("" + progress);
            rightColor = (rightColor & 0xFFFFFF00) | ((int) (progress * 2.55));
            right_rgb.setBackgroundColor(rightColor);
        } else if (seekBar.getId() == R.id.cycle_speed_seeker) {
            cycle_speed_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.cycle_bright_seeker) {
            cycle_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.compass_bright_seeker) {
            comp_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.throttle_speed_seeker) {
            throttle_rate_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.throttle_bright_seeker) {
            throttle_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.rpm_speed_seeker) {
            rpm_sens_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.x_accel_rate_seeker) {
            x_accel_sens_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.y_accel_speed_seeker) {
            y_accel_sens_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.custom_brightness_seeker) {
            custom_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.custom_rate_seeker) {
            custom_rate_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.custom_left_red_seeker) {
            custom_left_red_number.setText("" + progress);
            customLeftColor = (customLeftColor & 0xFF00FFFF) | ((int) (progress * 2.55) << 16);
            custom_left_rgb.setBackgroundColor(customLeftColor);
        } else if (seekBar.getId() == R.id.custom_left_green_seeker) {
            custom_left_green_number.setText("" + progress);
            customLeftColor = (customLeftColor & 0xFFFF00FF) | ((int) (progress * 2.55) << 8);
            custom_left_rgb.setBackgroundColor(customLeftColor);
        } else if (seekBar.getId() == R.id.custom_left_blue_seeker) {
            custom_left_blue_number.setText("" + progress);
            customLeftColor = (customLeftColor & 0xFFFFFF00) | ((int) (progress * 2.55));
            custom_left_rgb.setBackgroundColor(customLeftColor);
        } else if (seekBar.getId() == R.id.custom_right_red_seeker) {
            custom_right_red_number.setText("" + progress);
            customRightColor = (customRightColor & 0xFF00FFFF) | ((int) (progress * 2.55) << 16);
            custom_right_rgb.setBackgroundColor(customRightColor);
        } else if (seekBar.getId() == R.id.custom_right_green_seeker) {
            custom_right_green_number.setText("" + progress);
            customRightColor = (customRightColor & 0xFFFF00FF) | ((int) (progress * 2.55) << 8);
            custom_right_rgb.setBackgroundColor(customRightColor);
        } else if (seekBar.getId() == R.id.custom_right_blue_seeker) {
            custom_right_blue_number.setText("" + progress);
            customRightColor = (customRightColor & 0xFFFFFF00) | ((int) (progress * 2.55));
            custom_right_rgb.setBackgroundColor(customRightColor);
        } else if (seekBar.getId() == R.id.digital_static_zoom_seeker) {
            digital_static_zoom_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_static_shift_seeker) {
            digital_static_shift_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_static_bright_seeker) {
            digital_static_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_skittles_bright_seeker) {
            digital_skittles_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_cycle_rate_seeker) {
            digital_cycle_rate_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_cycle_bright_seeker) {
            digital_cycle_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_compass_bright_seeker) {
            digital_compass_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_throttle_zoom_seeker) {
            digital_throttle_zoom_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_throttle_shift_seeker) {
            digital_throttle_shift_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_throttle_sens_seeker) {
            digital_throttle_sens_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_throttle_bright_seeker) {
            digital_throttle_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_rpm_zoom_seeker) {
            digital_rpm_zoom_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_rpm_rate_seeker) {
            digital_rpm_rate_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_rpm_bright_seeker) {
            digital_rpm_bright_number.setText("" + progress);
        } else if (seekBar.getId() == R.id.digital_cycle_zoom_seeker) {
            digital_cycle_zoom_number.setText("" + progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        mMapSettings = googleMap.getUiSettings();
        mMapSettings.setCompassEnabled(true);
        mMapSettings.setZoomControlsEnabled(true);
        mMapSettings.setAllGesturesEnabled(true);
        mMap.setTrafficEnabled(false);
        enableMyLocation();

        if (mLoggingService != null)
            mLoggingService.mMap = mMap;
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, MY_LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {

        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != MY_LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mLocationPermissionDenied = true;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    public void onCheckboxClicked(View view) {
        CheckBox checkbox = (CheckBox) findViewById(view.getId());
        switch (view.getId()) {
            case R.id.sensor_accel_check:
                ACCEL = checkbox.isChecked();
                updateSensorGraphContent();
                break;
            case R.id.sensor_gyro_check:
                GYRO = checkbox.isChecked();
                updateSensorGraphContent();
                break;
            case R.id.sensor_compass_check:
                COMP = checkbox.isChecked();
                updateSensorGraphContent();
                break;
            case R.id.sensor_heading_check:
                HEAD = checkbox.isChecked();
                updateSensorGraphContent();
                break;
            case R.id.sensor_temp_check:
                IMU_TEMP = checkbox.isChecked();
                updateSensorGraphContent();
                break;
            case R.id.sensor_light_check:
                LGHT = checkbox.isChecked();
                updateSensorGraphContent();
                break;
            case R.id.sensor_nunchuck_joy_check:
                NUNCHUCK = checkbox.isChecked();
                updateSensorGraphContent();
                break;
            case R.id.motor_bcurrent_check:
                BC = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_duty_check:
                DTY = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_mcurrent_check:
                MC = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_rpm_check:
                RPM = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_mahu_check:
                MAHU = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_mahc_check:
                MAHC = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_whu_check:
                WHU = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_whc_check:
                WHC = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_temp_check:
                TMP = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.motor_volt_check:
                BV = checkbox.isChecked();
                updateMotorGraphContent();
                break;
            case R.id.LED_static_link_check:
                TextView left_title = (TextView) findViewById(R.id.left_LED_text);
                RelativeLayout right_layout = (RelativeLayout) findViewById(R.id.right_LED_layout);
                if (checkbox.isChecked()) {
                    left_title.setText("LED Color");
                    right_layout.setVisibility(View.GONE);
                } else {
                    left_title.setText("Left LED Color");
                    right_layout.setVisibility(View.VISIBLE);
                }
                STATIC_LINK = checkbox.isChecked();
                break;
            case R.id.custom_static_link_check:
                TextView custom_left_title = (TextView) findViewById(R.id.custom_left_LED_text);
                RelativeLayout custom_right_layout = (RelativeLayout) findViewById(R.id.custom_right_LED_layout);
                if (checkbox.isChecked()) {
                    custom_left_title.setText("LED Color");
                    custom_right_layout.setVisibility(View.GONE);
                } else {
                    custom_left_title.setText("Left LED Color");
                    custom_right_layout.setVisibility(View.VISIBLE);
                }
                STATIC_LINK = checkbox.isChecked();
                break;
        }
    }

    @Override
    public void onClick(View view) {
        Switch vSwitch = (Switch) findViewById(view.getId());
        if (view.getId() == R.id.led_side_switch) {
            led_switch_side = vSwitch.isChecked();
        } else if (view.getId() == R.id.led_hb_switch) {
            led_switch_hb = vSwitch.isChecked();
        } else if (view.getId() == R.id.led_light_switch) {
            led_switch_light = vSwitch.isChecked();
        } else if (view.getId() == R.id.led_sensor_switch) {
            led_switch_sensor = vSwitch.isChecked();
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            //Menu navMenu = navigationView.getMenu();
            //MenuItem bleStatus = navMenu.findItem(R.id.bluetooth_status);

            MenuItem bleAction = menu.findItem(R.id.action_ble);
            MenuItem logAction = menu.findItem(R.id.action_log);
            ImageView bleTitleBar = (ImageView) findViewById(R.id.toolbar_ble);

            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                bleTitleBar.setImageResource(R.mipmap.ic_ble_white);
                bleAction.setTitle("Disconnect BLE");
                bleAction.setEnabled(true);
                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                //Log.i(TAG, "CONNECTED");
                if(!BEEN_CONNECTED) {
                    if (READ_CURRENT_LED_SETTINGS) {
                        final byte txbuf[] = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x000,
                                (byte) 0x0CD,
                                (byte) 0x05A
                        };
                        mBluetoothService.last_send = System.nanoTime() / 1000000;
                        while (!mBluetoothService.writeBytes(txbuf)) {
                        }
                        //Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    }
                    if(READ_CURRENT_FW){
                        while (!mBluetoothService.writeBytes(fw_read)) {}
                    }
                    BEEN_CONNECTED = true;
                }
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                bleTitleBar.setImageResource(R.mipmap.ic_ble_black);
                bleAction.setTitle("Connect BLE");
                bleAction.setEnabled(true);
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                BEEN_CONNECTED = false;
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                TextView nxyText = (TextView) findViewById(R.id.sensor_nunchuck_joy_text);
                TextView accelText = (TextView) findViewById(R.id.sensor_accel_text);
                TextView gyroText = (TextView) findViewById(R.id.sensor_gyro_text);
                TextView compText = (TextView) findViewById(R.id.sensor_compass_text);
                TextView lightText = (TextView) findViewById(R.id.sensor_light_text);
                TextView headingText = (TextView) findViewById(R.id.sensor_heading_text);
                TextView IMUtempText = (TextView) findViewById(R.id.sensor_temp_text);
                int temp;

                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                if ((data.length == 5 ||data.length == 4 ||data.length == 3 ||data.length == 10 || data.length == 11 || data.length == 15 || data.length == 16 || data.length == 17 || data.length == 18 || data.length == 19 || data.length == 20) && RECIEVE_BLE_DATA) {
                    for (int i = 0; i < data.length; i++) {
                        temp = 0;
                        if (i >= data.length)
                            break;
                        switch (data[i] & 0xFF) {
                            case 0x11:
                                if (i + 2 >= data.length)
                                    break;
                                TextView bcText = (TextView) findViewById(R.id.motor_bcurrent_text);
                                temp = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                battery_current = ((float) (temp)) / 100;
                                BC_LineSeries.appendData(new DataPoint(graph_index, battery_current), true, MAX_DATA_POINTS);
                                bcText.setText("Battery current: " + battery_current + " A");
                                i += 2;
                                break;
                            case 0x12:
                                if (i + 2 >= data.length)
                                    break;
                                TextView bvText = (TextView) findViewById(R.id.motor_volt_text);
                                temp = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                battery_voltage = ((float) (temp)) / 10;
                                BV_LineSeries.appendData(new DataPoint(graph_index, battery_voltage), true, MAX_DATA_POINTS);
                                bvText.setText("Battery voltage: " + battery_voltage + " V");

                                if (previous_voltage > battery_voltage + 1) {
                                    battery_update = 1;
                                    //showBatteryUpdateAlert();
                                } else if (previous_voltage < battery_voltage - 1) {
                                    battery_update = 2;
                                    //showBatteryUpdateAlert();
                                }
                                previous_voltage = battery_voltage;

                                i += 2;
                                break;
                            case 0x13:
                                if (i + 2 >= data.length)
                                    break;
                                TextView mcText = (TextView) findViewById(R.id.motor_mcurrent_text);
                                temp = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                motor_current = ((float) (temp)) / 100;
                                MC_LineSeries.appendData(new DataPoint(graph_index, motor_current), true, MAX_DATA_POINTS);
                                mcText.setText("Motor current: " + motor_current + " A");
                                i += 2;
                                break;
                            case 0x14:
                                if (i + 2 >= data.length)
                                    break;
                                TextView tempText = (TextView) findViewById(R.id.motor_temp_text);
                                temp = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                motor_temp = ((float) (temp)) / 10;
                                TMP_LineSeries.appendData(new DataPoint(graph_index, motor_temp), true, MAX_DATA_POINTS);
                                tempText.setText("MOSFET Temp: " + motor_temp + "  \u2103");
                                i += 2;
                                break;
                            case 0x15:
                                if (i + 2 >= data.length)
                                    break;
                                TextView dutyText = (TextView) findViewById(R.id.motor_duty_text);
                                temp = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                duty_cycle = ((float) (temp)) / 10;
                                DTY_LineSeries.appendData(new DataPoint(graph_index, duty_cycle), true, MAX_DATA_POINTS);
                                dutyText.setText("Duty Cycle: " + duty_cycle + "%");
                                i += 2;
                                break;
                            case 0x16:
                                if (i + 3 >= data.length)
                                    break;
                                TextView rpmText = (TextView) findViewById(R.id.motor_rpm_text);
                                motor_rpm = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                RPM_LineSeries.appendData(new DataPoint(graph_index, motor_rpm), true, MAX_DATA_POINTS);
                                rpmText.setText("Electrical speed: " + motor_rpm + " rpm");

                                if (motor_rpm < 750) {
                                    BOARD_MOVING = false;
                                    NEW_PATH = true;
                                } else
                                    BOARD_MOVING = true;

                                i += 3;
                                break;
                            case 0x17:
                                if (i + 3 >= data.length)
                                    break;
                                TextView mahuText = (TextView) findViewById(R.id.motor_mahu_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                mAh_used = ((float) temp) / 100;
                                mahU_LineSeries.appendData(new DataPoint(graph_index, mAh_used), true, MAX_DATA_POINTS);
                                mahuText.setText("Drawn Cap: " + mAh_used + " Ah");
                                i += 3;
                                break;
                            case 0x18:
                                if (i + 3 >= data.length)
                                    break;
                                TextView mahcText = (TextView) findViewById(R.id.motor_mahc_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                mAh_charged = ((float) temp) / 100;
                                mahC_LineSeries.appendData(new DataPoint(graph_index, mAh_charged), true, MAX_DATA_POINTS);
                                mahcText.setText("Charged Cap: " + mAh_charged + " Ah");
                                i += 3;
                                break;
                            case 0x19:
                                if (i + 3 >= data.length)
                                    break;
                                TextView whuText = (TextView) findViewById(R.id.motor_whu_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                wh_used = ((float) temp) / 100;
                                whU_LineSeries.appendData(new DataPoint(graph_index, wh_used), true, MAX_DATA_POINTS);
                                whuText.setText("Drawn energy: " + wh_used + " Wh");
                                i += 3;
                                break;
                            case 0x1A:
                                if (i + 3 >= data.length)
                                    break;
                                TextView whcText = (TextView) findViewById(R.id.motor_whc_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                wh_charged = ((float) temp) / 100;
                                whC_LineSeries.appendData(new DataPoint(graph_index, wh_charged), true, MAX_DATA_POINTS);
                                whcText.setText("Charged energy: " + wh_charged + " Wh");
                                i += 3;
                                break;
                            case 0x1B:
                                if (i + 1 >= data.length)
                                    break;
                                error_codes = data[i + 1];
                                TextView faultText = (TextView) findViewById(R.id.motor_error_text);
                                switch (error_codes) {
                                    case FAULT_CODE_NONE:
                                        faultText.setText("Fault code: NONE");
                                        break;
                                    case FAULT_CODE_OVER_VOLTAGE:
                                        faultText.setText("Fault code: Over Voltage");
                                        break;
                                    case FAULT_CODE_UNDER_VOLTAGE:
                                        faultText.setText("Fault code: Under Voltage");
                                        break;
                                    case FAULT_CODE_DRV8302:
                                        faultText.setText("Fault code: DRV8302");
                                        break;
                                    case FAULT_CODE_ABS_OVER_CURRENT:
                                        faultText.setText("Fault code: ABS Over Current");
                                        break;
                                    case FAULT_CODE_OVER_TEMP_FET:
                                        faultText.setText("Fault code: Over Temp FET");
                                        break;
                                    case FAULT_CODE_OVER_TEMP_MOTOR:
                                        faultText.setText("Fault code: Over Temp Motor");
                                        break;
                                }
                                i++;
                                break;
                            case 0x21:
                                if (i + 1 >= data.length)
                                    break;
                                remote_x = (data[i + 1] & 0xFF);
                                NX_LineSeries.appendData(new DataPoint(graph_index, remote_x), true, MAX_DATA_POINTS);
                                if (remote_connected == 2)
                                    nxyText.setText("Remote X: " + remote_x + "\t\tY: " + remote_y);
                                else if (remote_connected == 1)
                                    nxyText.setText("Remote X: n/a\t\tY: " + remote_y);
                                else if (remote_connected == 0)
                                    nxyText.setText("Remote X: n/a\t\tY: n/a");
                                i++;
                                break;
                            case 0x22:
                                if (i + 1 >= data.length)
                                    break;
                                remote_y = (data[i + 1] & 0xFF);
                                NY_LineSeries.appendData(new DataPoint(graph_index, remote_y), true, MAX_DATA_POINTS);
                                if (remote_connected == 2)
                                    nxyText.setText("Remote X: " + remote_x + "\t\tY: " + remote_y);
                                else if (remote_connected == 1)
                                    nxyText.setText("Remote X: n/a\t\tY: " + remote_y);
                                else if (remote_connected == 0)
                                    nxyText.setText("Remote X: n/a\t\tY: n/a");
                                i++;
                                break;
                            case 0x23:
                                if (i + 1 >= data.length)
                                    break;
                                TextView nz_text = (TextView) findViewById(R.id.sensor_NZ_text);
                                remote_button = ((data[i + 1] & 0x1));
                                remote_connected = ((data[i + 1] & 0x6) >> 1);
                                nz_text.setText("Remote Button: " + remote_button);
                                i++;
                                break;
                            case 0x24:
                                if (i + 2 >= data.length)
                                    break;
                                accel_x = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                AX_LineSeries.appendData(new DataPoint(graph_index, accel_x), true, MAX_DATA_POINTS);
                                accelText.setText("Accel X: " + accel_x + "\t\tY: " + accel_y + "\t\tZ: " + accel_z);
                                i += 2;
                                break;
                            case 0x25:
                                if (i + 2 >= data.length)
                                    break;
                                accel_y = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                AY_LineSeries.appendData(new DataPoint(graph_index, accel_y), true, MAX_DATA_POINTS);
                                accelText.setText("Accel X: " + accel_x + "\t\tY: " + accel_y + "\t\tZ: " + accel_z);
                                i += 2;
                                break;
                            case 0x26:
                                if (i + 2 >= data.length)
                                    break;
                                accel_z = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                AZ_LineSeries.appendData(new DataPoint(graph_index, accel_z), true, MAX_DATA_POINTS);
                                accelText.setText("Accel X: " + accel_x + "\t\tY: " + accel_y + "\t\tZ: " + accel_z);
                                i += 2;
                                break;
                            case 0x27:
                                if (i + 2 >= data.length)
                                    break;
                                gyro_x = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                GX_LineSeries.appendData(new DataPoint(graph_index, gyro_x), true, MAX_DATA_POINTS);
                                gyroText.setText("Gyro X: " + gyro_x + "\t\tY: " + gyro_y + "\t\tZ: " + gyro_z);
                                i += 2;
                                break;
                            case 0x28:
                                if (i + 2 >= data.length)
                                    break;
                                gyro_y = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                GY_LineSeries.appendData(new DataPoint(graph_index, gyro_y), true, MAX_DATA_POINTS);
                                gyroText.setText("Gyro X: " + gyro_x + "\t\tY: " + gyro_y + "\t\tZ: " + gyro_z);
                                i += 2;
                                break;
                            case 0x29:
                                if (i + 2 >= data.length)
                                    break;
                                gyro_z = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                GZ_LineSeries.appendData(new DataPoint(graph_index, gyro_z), true, MAX_DATA_POINTS);
                                gyroText.setText("Gyro X: " + gyro_x + "\t\tY: " + gyro_y + "\t\tZ: " + gyro_z);
                                i += 2;
                                break;
                            case 0x2A:
                                if (i + 2 >= data.length)
                                    break;
                                compass_x = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                CX_LineSeries.appendData(new DataPoint(graph_index, compass_x), true, MAX_DATA_POINTS);
                                compText.setText("Compass X: " + compass_x + "\t\tY: " + compass_y + "\t\tZ: " + compass_z);
                                i += 2;
                                break;
                            case 0x2B:
                                if (i + 2 >= data.length)
                                    break;
                                compass_y = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                CY_LineSeries.appendData(new DataPoint(graph_index, compass_y), true, MAX_DATA_POINTS);
                                compText.setText("Compass X: " + compass_x + "\t\tY: " + compass_y + "\t\tZ: " + compass_z);
                                i += 2;
                                break;
                            case 0x2C:
                                if (i + 2 >= data.length)
                                    break;
                                compass_z = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                CZ_LineSeries.appendData(new DataPoint(graph_index, compass_z), true, MAX_DATA_POINTS);
                                compText.setText("Compass X: " + compass_x + "\t\tY: " + compass_y + "\t\tZ: " + compass_z);
                                i += 2;
                                break;
                            case 0x2D:
                                if (i + 2 >= data.length)
                                    break;
                                light_sense = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                LGHT_LineSeries.appendData(new DataPoint(graph_index, light_sense), true, MAX_DATA_POINTS);
                                lightText.setText("Light Sensor: " + light_sense);
                                i += 2;
                                break;
                            case 0x2E:
                                if (i + 2 >= data.length)
                                    break;
                                heading = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                heading = heading / 10;
                                HEAD_LineSeries.appendData(new DataPoint(graph_index, heading), true, MAX_DATA_POINTS);
                                headingText.setText("Heading: " + heading + "°");
                                i += 2;
                                break;
                            case 0x2F:
                                if (i + 2 >= data.length)
                                    break;
                                IMU_temp = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                IMU_temp = IMU_temp / 256;
                                IMU_Temp_LinSeries.appendData(new DataPoint(graph_index, IMU_temp), true, MAX_DATA_POINTS);
                                IMUtempText.setText("TTL Temp: " + String.format("%.1f", IMU_temp) + " °C");
                                i += 2;
                                break;
                            case 0x31:
                                if (i + 2 >= data.length)
                                    break;
                                Spinner ModeSpinner = (Spinner) findViewById(R.id.modes_spinner);
                                Switch SideSwitch = (Switch) findViewById(R.id.led_side_switch);
                                Switch HeadSwitch = (Switch) findViewById(R.id.led_hb_switch);
                                Switch LightSwitch = (Switch) findViewById(R.id.led_light_switch);
                                Switch SensSwitch = (Switch) findViewById(R.id.led_sensor_switch);
                                led_mode = (data[i + 1] & 0xF0) >> 4;
                                HeadSwitch.setChecked(((data[i + 1] & 0x08) >> 3) == 1);
                                SideSwitch.setChecked(((data[i + 1] & 0x04) >> 2) == 1);
                                LightSwitch.setChecked(((data[i + 1] & 0x02) >> 1) == 1);
                                SensSwitch.setChecked((data[i + 1] & 0x01) == 1);
                                LED_STRIP_TYPE = (data[i + 2] & 0xFF);
                                updateModeSpinner();
                                ModeSpinner.setSelection(led_mode);
                                i += 2;
                                break;
                            case 0x32:
                                if (i + 6 >= data.length)
                                    break;
                                //CheckBox LinkCheck = (CheckBox) findViewById(R.id.LED_static_link_check);
                                SeekBar StaticLR = (SeekBar) findViewById(R.id.left_red_seeker);
                                SeekBar StaticLG = (SeekBar) findViewById(R.id.left_green_seeker);
                                SeekBar StaticLB = (SeekBar) findViewById(R.id.left_blue_seeker);
                                SeekBar StaticRR = (SeekBar) findViewById(R.id.right_red_seeker);
                                SeekBar StaticRG = (SeekBar) findViewById(R.id.right_green_seeker);
                                SeekBar StaticRB = (SeekBar) findViewById(R.id.right_blue_seeker);
                                StaticLR.setProgress(data[i + 1] & 0xFF);
                                StaticLG.setProgress(data[i + 2] & 0xFF);
                                StaticLB.setProgress(data[i + 3] & 0xFF);
                                StaticRR.setProgress(data[i + 4] & 0xFF);
                                StaticRG.setProgress(data[i + 5] & 0xFF);
                                StaticRB.setProgress(data[i + 6] & 0xFF);
                                i += 6;
                                break;
                            case 0x33:
                                if (i + 2 >= data.length)
                                    break;
                                SeekBar CycleRate = (SeekBar) findViewById(R.id.cycle_speed_seeker);
                                SeekBar CycleBright = (SeekBar) findViewById(R.id.cycle_bright_seeker);
                                CycleRate.setProgress(data[i + 1] & 0xFF);
                                CycleBright.setProgress(data[i + 2] & 0xFF);
                                i += 2;
                                break;
                            case 0x34:
                                if (i + 1 >= data.length)
                                    break;
                                SeekBar CompBright = (SeekBar) findViewById(R.id.compass_bright_seeker);
                                CompBright.setProgress(data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x35:
                                if (i + 2 >= data.length)
                                    break;
                                SeekBar ThrottleSens = (SeekBar) findViewById(R.id.throttle_speed_seeker);
                                SeekBar ThrottleBright = (SeekBar) findViewById(R.id.throttle_bright_seeker);
                                ThrottleSens.setProgress(data[i + 1] & 0xFF);
                                ThrottleBright.setProgress(data[i + 2] & 0xFF);
                                i += 2;
                                break;
                            case 0x36:
                                if (i + 1 >= data.length)
                                    break;
                                SeekBar rpmRate = (SeekBar) findViewById(R.id.rpm_speed_seeker);
                                rpmRate.setProgress(data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x37:
                                if (i + 1 >= data.length)
                                    break;
                                SeekBar XaccelRate = (SeekBar) findViewById(R.id.x_accel_rate_seeker);
                                XaccelRate.setProgress(data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x38:
                                if (i + 1 >= data.length)
                                    break;
                                SeekBar YaccelRate = (SeekBar) findViewById(R.id.y_accel_speed_seeker);
                                YaccelRate.setProgress(data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x39:
                                if (i + 10 >= data.length)
                                    break;
                                Spinner ColorSpin = (Spinner) findViewById(R.id.custom_color_select_spinner);
                                Spinner RateSpin = (Spinner) findViewById(R.id.custom_rate_select_spinner);
                                Spinner BrightSpin = (Spinner) findViewById(R.id.custom_brightness_select_spinner);
                                //CheckBox LinkCheck = (CheckBox) findViewById(R.id.custom_static_link_check);
                                SeekBar CustomLR = (SeekBar) findViewById(R.id.custom_left_red_seeker);
                                SeekBar CustomLG = (SeekBar) findViewById(R.id.custom_left_green_seeker);
                                SeekBar CustomLB = (SeekBar) findViewById(R.id.custom_left_blue_seeker);
                                SeekBar CustomRR = (SeekBar) findViewById(R.id.custom_right_red_seeker);
                                SeekBar CustomRG = (SeekBar) findViewById(R.id.custom_right_green_seeker);
                                SeekBar CustomRB = (SeekBar) findViewById(R.id.custom_right_blue_seeker);
                                SeekBar CustomRate = (SeekBar) findViewById(R.id.custom_rate_seeker);
                                SeekBar CustomBright = (SeekBar) findViewById(R.id.custom_brightness_seeker);
                                ColorSpin.setSelection((data[i + 1] & 0xF0) >> 4);
                                BrightSpin.setSelection(data[i + 1] & 0x0F);
                                RateSpin.setSelection(data[i + 2] & 0xFF);
                                CustomLR.setProgress(data[i + 3] & 0xFF);
                                CustomLG.setProgress(data[i + 4] & 0xFF);
                                CustomLB.setProgress(data[i + 5] & 0xFF);
                                CustomRR.setProgress(data[i + 6] & 0xFF);
                                CustomRG.setProgress(data[i + 7] & 0xFF);
                                CustomRB.setProgress(data[i + 8] & 0xFF);
                                CustomRate.setProgress(data[i + 9] & 0xFF);
                                CustomBright.setProgress(data[i + 10] & 0xFF);
                                i += 10;
                                break;
                            case 0x3A:
                                if (i + 3 >= data.length)
                                    break;
                                SeekBar DigitalStaticZoom = (SeekBar) findViewById(R.id.digital_static_zoom_seeker);
                                SeekBar DigitalStaticShift = (SeekBar) findViewById(R.id.digital_static_shift_seeker);
                                SeekBar DigitalStaticBright = (SeekBar) findViewById(R.id.digital_static_bright_seeker);
                                DigitalStaticZoom.setProgress(data[i + 1] & 0xFF);
                                DigitalStaticShift.setProgress(data[i + 2] & 0xFF);
                                DigitalStaticBright.setProgress(data[i + 3] & 0xFF);
                                i += 3;
                                break;
                            case 0x3B:
                                if (i + 1 >= data.length)
                                    break;
                                SeekBar DigitalSkittlesBright = (SeekBar) findViewById(R.id.digital_skittles_bright_seeker);
                                DigitalSkittlesBright.setProgress(data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x3C:
                                if (i + 3 >= data.length)
                                    break;
                                SeekBar DigitalCycleZoom = (SeekBar) findViewById(R.id.digital_cycle_zoom_seeker);
                                SeekBar DigitalCycleRate = (SeekBar) findViewById(R.id.digital_cycle_rate_seeker);
                                SeekBar DigitalCycleBright = (SeekBar) findViewById(R.id.digital_cycle_bright_seeker);
                                DigitalCycleZoom.setProgress(data[i + 1] & 0xFF);
                                DigitalCycleRate.setProgress(data[i + 2] & 0xFF);
                                DigitalCycleBright.setProgress(data[i + 3] & 0xFF);
                                i += 3;
                                break;
                            case 0x3D:
                                if (i + 1 >= data.length)
                                    break;
                                SeekBar DigitalCompassBright = (SeekBar) findViewById(R.id.digital_compass_bright_seeker);
                                DigitalCompassBright.setProgress(data[i + 1] & 0xFF);
                                i += 1;
                                break;
                            case 0x3E:
                                if (i + 4 >= data.length)
                                    break;
                                SeekBar DigitalThrottleZoom = (SeekBar) findViewById(R.id.digital_throttle_zoom_seeker);
                                SeekBar DigitalThrottleShift = (SeekBar) findViewById(R.id.digital_throttle_shift_seeker);
                                SeekBar DigitalThrottleSens = (SeekBar) findViewById(R.id.digital_throttle_sens_seeker);
                                SeekBar DigitalThrottleBright = (SeekBar) findViewById(R.id.digital_throttle_bright_seeker);
                                DigitalThrottleZoom.setProgress(data[i + 1] & 0xFF);
                                DigitalThrottleShift.setProgress(data[i + 2] & 0xFF);
                                DigitalThrottleSens.setProgress(data[i + 3] & 0xFF);
                                DigitalThrottleBright.setProgress(data[i + 4] & 0xFF);
                                i += 4;
                                break;
                            case 0x3F:
                                if (i + 3 >= data.length)
                                    break;
                                SeekBar DigitalRPMZoom = (SeekBar) findViewById(R.id.digital_rpm_zoom_seeker);
                                SeekBar DigitalRPMRate = (SeekBar) findViewById(R.id.digital_rpm_rate_seeker);
                                SeekBar DigitalRPMBright = (SeekBar) findViewById(R.id.digital_rpm_bright_seeker);
                                DigitalRPMZoom.setProgress(data[i + 1] & 0xFF);
                                DigitalRPMRate.setProgress(data[i + 2] & 0xFF);
                                DigitalRPMBright.setProgress(data[i + 3] & 0xFF);
                                i += 3;
                                break;
                            case 0x40:
                                if (i + 5 >= data.length)
                                    break;
                                CheckBox AnalogStaticShuffleCheck = findViewById(R.id.LED_static_shuffle_check);
                                CheckBox AnalogCycleShuffleCheck = findViewById(R.id.LED_cycle_shuffle_check);
                                CheckBox AnalogCompassShuffleCheck = findViewById(R.id.LED_compass_shuffle_check);
                                CheckBox AnalogThrottleShuffleCheck = findViewById(R.id.LED_throttle_shuffle_check);
                                CheckBox AnalogRPMShuffleCheck = findViewById(R.id.LED_rpm_shuffle_check);
                                CheckBox AnalogRPMThrottleShuffleCheck = findViewById(R.id.LED_rpm_throttle_shuffle_check);
                                CheckBox AnalogXAccelShuffleCheck = findViewById(R.id.LED_x_accel_shuffle_check);
                                CheckBox AnalogYaccelShuffleCheck = findViewById(R.id.LED_y_accel_shuffle_check);
                                CheckBox AnalogCustomShuffleCheck = findViewById(R.id.LED_custom_shuffle_check);
                                CheckBox DigitalStaticShuffleCheck = findViewById(R.id.digital_static_shuffle_check);
                                CheckBox DigitalCycleShuffleCheck = findViewById(R.id.digital_cycle_shuffle_check);
                                CheckBox DigitalCompassShuffleCheck = findViewById(R.id.digital_compass_shuffle_check);
                                CheckBox DigitalThrottleShuffleCheck = findViewById(R.id.digital_throttle_shuffle_check);
                                CheckBox DigitalRPMShuffleCheck = findViewById(R.id.digital_rpm_shuffle_check);
                                CheckBox DigitalRPMThrottleShuffleCheck = findViewById(R.id.digital_rpm_throttle_shuffle_check);
                                CheckBox DigitalSkittlesShuffleCheck = findViewById(R.id.digital_skittles_shuffle_check);
                                CheckBox DigitalCompassSnakeShuffleCheck = findViewById(R.id.digital_compass_snake_shuffle_check);
                                CheckBox DigitalCompassWheelShuffleCheck = findViewById(R.id.digital_compass_wheel_shuffle_check);

                                SHUFFLE_LED_MODES = (data[i + 1] & 0x01) == 1;
                                AnalogStaticShuffleCheck.setChecked(((data[i + 2] & 0x01)) == 1);
                                AnalogCycleShuffleCheck.setChecked(((data[i + 2] & 0x02) >> 1) == 1);
                                AnalogCompassShuffleCheck.setChecked(((data[i + 2] & 0x04) >> 2) == 1);
                                AnalogThrottleShuffleCheck.setChecked(((data[i + 2] & 0x08) >> 3) == 1);
                                AnalogRPMShuffleCheck.setChecked(((data[i + 2] & 0x10) >> 4) == 1);
                                AnalogRPMThrottleShuffleCheck.setChecked(((data[i + 2] & 0x20) >> 5) == 1);
                                AnalogXAccelShuffleCheck.setChecked(((data[i + 2] & 0x40) >> 6) == 1);
                                AnalogYaccelShuffleCheck.setChecked(((data[i + 2] & 0x80) >> 7) == 1);
                                AnalogCustomShuffleCheck.setChecked(((data[i + 3] & 0x01)) == 1);
                                DigitalStaticShuffleCheck.setChecked(((data[i + 4] & 0x01)) == 1);
                                DigitalSkittlesShuffleCheck.setChecked(((data[i + 4] & 0x02) >> 1) == 1);
                                DigitalCycleShuffleCheck.setChecked(((data[i + 4] & 0x04) >> 2) == 1);
                                DigitalCompassShuffleCheck.setChecked(((data[i + 4] & 0x08) >> 3) == 1);
                                DigitalThrottleShuffleCheck.setChecked(((data[i + 4] & 0x10) >> 4) == 1);
                                DigitalRPMShuffleCheck.setChecked(((data[i + 4] & 0x20) >> 5) == 1);
                                DigitalRPMThrottleShuffleCheck.setChecked(((data[i + 4] & 0x40) >> 6) == 1);
                                DigitalCompassWheelShuffleCheck.setChecked(((data[i + 4] & 0x80) >> 7) == 1);
                                DigitalCompassSnakeShuffleCheck.setChecked(((data[i + 5] & 0x01)) == 1);

                                updateShuffleCheckVisibility();
                                STORE_SHUFFLE = true;
                                i += 5;
                                break;
                            case 0x74:
                                downloadVersionFile download = new downloadVersionFile();
                                download.execute();
                                if (i + 4 == data.length-1) {
                                    TTL_FW = 0;
                                    TTL_FW += (data[i + 1] & 0x0FF);
                                    TTL_FW += (data[i + 2] & 0x0FF) * 100;
                                    i += 4;
                                } else if(i + 2 == data.length-1){
                                    TTL_FW = 0;
                                    TTL_FW += (data[i + 1] & 0x0FF);
                                    TTL_FW += (data[i + 2] & 0x0FF) * 100;
                                    i += 2;
                                } else {
                                    break;
                                }

                                if (READ_CURRENT_FW) {
                                    while(!FW_Download_Atempted){download.getStatus();}
                                    if(TTL_FW == 0){
                                        Toast.makeText(context, "TTL FW Not Read", Toast.LENGTH_SHORT).show();
                                    } else if(Latest_FW == 0){
                                        Toast.makeText(context, "Latest FW Not Read", Toast.LENGTH_SHORT).show();
                                    } else if(TTL_FW != Latest_FW && TTL_FW != 0 && Latest_FW != 0){
                                        //Toast.makeText(context, "TTL FW Out-of-Date", Toast.LENGTH_SHORT).show();
                                        new AlertDialog.Builder(MainActivity.this)
                                                .setMessage("The firmware on your TTL module is out of date. Would you like to update it to the latest version?")
                                                .setIcon(R.drawable.ic_warning)
                                                .setTitle("Firmware Out of Date")
                                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(MainActivity.this, FirmwareSettingsActivity.class);
                                                        startActivity(intent);
                                                    }
                                                })
                                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        // Do nothing
                                                    }
                                                })
                                                .setCancelable(true)
                                                .show();
                                    } else {
                                        Toast.makeText(context, "TTL FW Up-to-Date", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                break;
                            case 0x76:
                                if (i + 2 >= data.length)
                                    break;
                                ttl_error_code = ((data[i+1]&0x00FF)<<8);
                                ttl_error_code |= (data[i+2]&0x00FF);
                                //Toast.makeText(MainActivity.this, ""+ttl_error_code, Toast.LENGTH_SHORT).show();
                                break;
                            case 0x77:
                                if (i + 19 >= data.length)
                                    break;
                                String temp_string = new String(data,i+1,19, StandardCharsets.US_ASCII);
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(temp_string)
                                        .setPositiveButton("OK", null)
                                        .setTitle("TTL Info Message")
                                        .show();
                                break;
                            case 0xDE:
                                graph_index++;
                                break;
                        }
                    }
                }
            } else if (LoggingService.ACTION_LOG_TOGGLE.equals(action)) {
                mLoggingService.ToggleLogging();
                if (mLoggingService.LOG_STARTED) {
                    logAction.setTitle("Stop Logging");
                } else {
                    logAction.setTitle("Start Logging");
                }
            } else if (LoggingService.ACTION_CLOSE_APP.equals(action)) {
                Intent close_intent = new Intent(getApplicationContext(), MainActivity.class);
                close_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(close_intent);
                MainActivity.super.finish();
            } else if (LoggingService.ACTION_LED_TOGGLE.equals(action)) {
                //Toast.makeText(MainActivity.this, "toggle", Toast.LENGTH_SHORT).show();
                final byte txbuf[] = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0E3,
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            } else if (LoggingService.ACTION_AUX_TOGGLE.equals(action)) {
                //Toast.makeText(MainActivity.this, "toggle", Toast.LENGTH_SHORT).show();
                byte txbuf[];
                if (AUX_PRESSED == false) {
                    txbuf = new byte[]{
                            (byte) 0x0A5,
                            (byte) 0x000,
                            (byte) 0x0AA,
                            (byte) 0x05A
                    };
                    if (!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    else
                        AUX_PRESSED = true;
                } else {
                    txbuf = new byte[]{
                            (byte) 0x0A5,
                            (byte) 0x000,
                            (byte) 0x0AB,
                            (byte) 0x05A
                    };
                    if (!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    else
                        AUX_PRESSED = false;
                }
            } else if (LoggingService.ACTION_LED_MODE_DOWN.equals(action)) {
                final byte txbuf[] = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0E2,
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            } else if (LoggingService.ACTION_LED_MODE_UP.equals(action)) {
                final byte txbuf[] = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0E1,
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(LoggingService.ACTION_LOG_TOGGLE);
        intentFilter.addAction(LoggingService.ACTION_LED_TOGGLE);
        intentFilter.addAction(LoggingService.ACTION_AUX_TOGGLE);
        intentFilter.addAction(LoggingService.ACTION_LED_MODE_DOWN);
        intentFilter.addAction(LoggingService.ACTION_LED_MODE_UP);
        intentFilter.addAction(LoggingService.ACTION_CLOSE_APP);
        return intentFilter;
    }

    public void onButtonClick(View view) {
        final byte txbuf[];

        switch (view.getId()) {
            case R.id.led_current_button:
                txbuf = new byte[]{
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0CD,
                        (byte) 0x05A
                };
                if (!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                //String txstring = bytesToHex(txbuf);
                // Toast.makeText(MainActivity.this, txstring, Toast.LENGTH_SHORT).show();
                break;

            case R.id.led_apply_button:
                Switch vSwitch;
                SeekBar vProgress;
                byte switches_led_type;

                switches_led_type = (byte) (LED_STRIP_TYPE & 0x0F);
                vSwitch = (Switch) findViewById(R.id.led_side_switch);
                switches_led_type = (byte) (switches_led_type | ((byte) 0xFF & (byte) (vSwitch.isChecked() ? 1 : 0) << 4));
                vSwitch = (Switch) findViewById(R.id.led_hb_switch);
                switches_led_type = (byte) (switches_led_type | ((byte) (vSwitch.isChecked() ? 1 : 0)) << 5);
                vSwitch = (Switch) findViewById(R.id.led_light_switch);
                switches_led_type = (byte) (switches_led_type | ((byte) (vSwitch.isChecked() ? 1 : 0)) << 6);
                vSwitch = (Switch) findViewById(R.id.led_sensor_switch);
                switches_led_type = (byte) (switches_led_type | ((byte) (vSwitch.isChecked() ? 1 : 0)) << 7);

                if(LED_STRIP_TYPE == 0) {
                    if (led_mode == 0) { // Static
                        CheckBox linkCheck = (CheckBox) findViewById(R.id.LED_static_link_check);
                        CheckBox shuffleCheck = findViewById(R.id.LED_static_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        if (linkCheck.isChecked()) {
                            txbuf = new byte[]{
                                    (byte) 0x0A5,
                                    (byte) 0x008,
                                    (byte) 0x0ED,
                                    switches_led_type,
                                    (byte) ((leftColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((leftColor & 0x0000FF00) >> 8), //Green
                                    (byte) (leftColor & 0x000000FF), //Blue
                                    (byte) ((leftColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((leftColor & 0x0000FF00) >> 8), //Green
                                    (byte) (leftColor & 0x000000FF), //Blue
                                    mode_bools,
                                    (byte) 0x05A
                            };
                        } else {
                            txbuf = new byte[]{
                                    (byte) 0x0A5,
                                    (byte) 0x008,
                                    (byte) 0x0ED,
                                    switches_led_type,
                                    (byte) ((leftColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((leftColor & 0x0000FF00) >> 8), //Green
                                    (byte) (leftColor & 0x000000FF), //Blue
                                    (byte) ((rightColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((rightColor & 0x0000FF00) >> 8), //Green
                                    (byte) (rightColor & 0x000000FF), //Blue
                                    mode_bools,
                                    (byte) 0x05A
                            };
                        }
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 1) { // Color Cycle
                        CheckBox shuffleCheck = findViewById(R.id.LED_cycle_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.cycle_speed_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.cycle_bright_seeker);
                        byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x004,
                                (byte) 0x0EC,
                                switches_led_type,
                                progress1, //rate
                                progress2, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 2) { // Compass Cycle
                        CheckBox shuffleCheck = findViewById(R.id.LED_compass_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.compass_bright_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x003,
                                (byte) 0x0EB,
                                switches_led_type,
                                progress1, //sensitivity
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 3) { // Throttle
                        CheckBox shuffleCheck = findViewById(R.id.LED_throttle_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.throttle_speed_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.throttle_bright_seeker);
                        byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x004,
                                (byte) 0x0EA,
                                switches_led_type,
                                progress1, //sensitivity
                                progress2, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 4) { // RPM
                        CheckBox shuffleCheck = findViewById(R.id.LED_rpm_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.rpm_speed_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x003,
                                (byte) 0x0E9,
                                switches_led_type,
                                progress1, //rate
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 5) { // RPM + Throttle
                        CheckBox shuffleCheck = findViewById(R.id.LED_rpm_throttle_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x002,
                                (byte) 0x0E8,
                                switches_led_type,
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 6) { // X Accel
                        CheckBox shuffleCheck = findViewById(R.id.LED_x_accel_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.x_accel_rate_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x003,
                                (byte) 0x0E7,
                                switches_led_type,
                                progress1,
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 7) { // Y Accel
                        CheckBox shuffleCheck = findViewById(R.id.LED_y_accel_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.y_accel_speed_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x003,
                                (byte) 0x0E6,
                                switches_led_type,
                                progress1,
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 8) { // Custom
                        CheckBox shuffleCheck = findViewById(R.id.LED_custom_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.custom_rate_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.custom_brightness_seeker);
                        byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                        Spinner ColorBaseSpin = (Spinner) findViewById(R.id.custom_color_select_spinner);
                        Spinner RateBaseSpin = (Spinner) findViewById(R.id.custom_rate_select_spinner);
                        Spinner BrightBaseSpin = (Spinner) findViewById(R.id.custom_brightness_select_spinner);
                        //switches_led_type = (byte) (switches_led_type | ColorBaseSpin.getSelectedItemPosition());
                        byte RateBrightBase = (byte) ((RateBaseSpin.getSelectedItemPosition() << 4) | BrightBaseSpin.getSelectedItemPosition());
                        CheckBox linkCheck = (CheckBox) findViewById(R.id.custom_static_link_check);
                        if (linkCheck.isChecked()) {
                            txbuf = new byte[]{
                                    (byte) 0x0A5,
                                    (byte) 0x00C,
                                    (byte) 0x0B1,
                                    switches_led_type,
                                    (byte) (ColorBaseSpin.getSelectedItemPosition() & 0x0FF),
                                    RateBrightBase,
                                    (byte) ((customLeftColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((customLeftColor & 0x0000FF00) >> 8), //Green
                                    (byte) (customLeftColor & 0x000000FF), //Blue
                                    (byte) ((customLeftColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((customLeftColor & 0x0000FF00) >> 8), //Green
                                    (byte) (customLeftColor & 0x000000FF), //Blue
                                    progress1, // Rate
                                    progress2, // Brightness
                                    mode_bools,
                                    (byte) 0x05A
                            };
                        } else {
                            txbuf = new byte[]{
                                    (byte) 0x0A5,
                                    (byte) 0x00C,
                                    (byte) 0x0B1,
                                    switches_led_type,
                                    (byte) (ColorBaseSpin.getSelectedItemPosition() & 0x0FF),
                                    RateBrightBase,
                                    (byte) ((customLeftColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((customLeftColor & 0x0000FF00) >> 8), //Green
                                    (byte) (customLeftColor & 0x000000FF), //Blue
                                    (byte) ((customRightColor & 0x00FF0000) >> 16), //Red
                                    (byte) ((customRightColor & 0x0000FF00) >> 8), //Green
                                    (byte) (customRightColor & 0x000000FF), //Blue
                                    progress1, // Rate
                                    progress2, // Brightness
                                    mode_bools,
                                    (byte) 0x05A
                            };
                        }
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    if (led_mode == 0) { // Static
                        CheckBox shuffleCheck = findViewById(R.id.digital_static_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.digital_static_zoom_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_static_shift_seeker);
                        byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_static_bright_seeker);
                        byte progress3 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x005,
                                (byte) 0x0B9,
                                switches_led_type,
                                progress1, //zoom
                                progress2, //shift
                                progress3, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 1) { // Skittles
                        CheckBox shuffleCheck = findViewById(R.id.digital_skittles_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.digital_skittles_bright_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x003,
                                (byte) 0x0BA,
                                switches_led_type,
                                progress1, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 2) { // Color Cycle
                        CheckBox shuffleCheck = findViewById(R.id.digital_cycle_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.digital_cycle_zoom_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_cycle_rate_seeker);
                        byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_cycle_bright_seeker);
                        byte progress3 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x005,
                                (byte) 0x0BB,
                                switches_led_type,
                                progress1, //zoom
                                progress2, //rate
                                progress3, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 3) { // Compass Cycle
                        CheckBox shuffleCheck = findViewById(R.id.digital_compass_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.digital_compass_bright_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x003,
                                (byte) 0x0BC,
                                switches_led_type,
                                progress1, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 4) { // Throttle
                        CheckBox shuffleCheck = findViewById(R.id.digital_throttle_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.digital_throttle_zoom_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_throttle_shift_seeker);
                        byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_throttle_sens_seeker);
                        byte progress3 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_throttle_bright_seeker);
                        byte progress4 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x006,
                                (byte) 0x0C0,
                                switches_led_type,
                                progress1, //zoom
                                progress2, //shift
                                progress3, //sensitivity
                                progress4, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 5) { // RPM
                        CheckBox shuffleCheck = findViewById(R.id.digital_rpm_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        vProgress = (SeekBar) findViewById(R.id.digital_rpm_zoom_seeker);
                        byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_rpm_rate_seeker);
                        byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                        vProgress = (SeekBar) findViewById(R.id.digital_rpm_bright_seeker);
                        byte progress3 = (byte) (vProgress.getProgress() & 0xFF);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x005,
                                (byte) 0x0BE,
                                switches_led_type,
                                progress1, //zoom
                                progress2, //rate
                                progress3, //brightness
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 6) { // RPM + Throttle
                        CheckBox shuffleCheck = findViewById(R.id.digital_rpm_throttle_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x002,
                                (byte) 0x0BF,
                                switches_led_type,
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 7) { // Compass Wheel
                        CheckBox shuffleCheck = findViewById(R.id.digital_compass_wheel_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x002,
                                (byte) 0x0C1,
                                switches_led_type,
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    } else if (led_mode == 8) { // Compass Snake
                        CheckBox shuffleCheck = findViewById(R.id.digital_compass_snake_shuffle_check);
                        byte mode_bools = (byte) (((byte) (shuffleCheck.isChecked() ? 1 : 0)) << 7);
                        txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x002,
                                (byte) 0x0C6,
                                switches_led_type,
                                mode_bools,
                                (byte) 0x05A
                        };
                        if (!mBluetoothService.writeBytes(txbuf))
                            Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.motor_log_button:
                mLoggingService.LOG_MOTOR_DATA = !mLoggingService.LOG_MOTOR_DATA;
                break;
            case R.id.gps_log_button:
                mLoggingService.LOG_MAP_DATA = !mLoggingService.LOG_MAP_DATA;
                break;
            case R.id.sensor_log_button:
                mLoggingService.LOG_SENSOR_DATA = !mLoggingService.LOG_SENSOR_DATA;
                break;
        }
    }

    @Override
    public void updateLocation(double val, int field) {
        TextView longitudetext = (TextView) findViewById(R.id.gps_maplong_text);
        TextView latitudetext = (TextView) findViewById(R.id.gps_maplat_text);
        TextView altitudetext = (TextView) findViewById(R.id.gps_mapalt_text);
        TextView speedtext = (TextView) findViewById(R.id.gps_speed_text);
        TextView maxSpeedText = (TextView) findViewById(R.id.gps_max_text);
        switch (field) {
            case 0:
                longitudetext.setText("Longitude: " + val + (char) 0x00B0);
                pLong = longitude;
                longitude = val;
                break;
            case 1:
                latitudetext.setText("Latitude: " + val + (char) 0x00B0);
                pLat = latitude;
                latitude = val;
                break;
            case 2:
                altitudetext.setText("Altitude: " + (double) (Math.round(val * 3.28084 * 100)) / 100 + "ft");
                break;
            case 3:
                double temp = (double) (Math.round(val * 2.23694 * 100)) / 100;
                speedtext.setText("Speed: " + temp + "mph");
                if (temp > maxSpeed) {// && BOARD_MOVING) {
                    maxSpeed = temp;
                    maxSpeedText.setText("Max Speed: " + maxSpeed + " mph");
                }
                break;
        }
    }

    @Override
    public void updatePath() {
        TextView gpsDistText = (TextView) findViewById(R.id.gps_dist_text);
        if (NEW_PATH) {
            if (BOARD_MOVING) {
                polyPoints.clear();
                polyPoints.add(new LatLng(latitude, longitude));
                path = mMap.addPolyline(new PolylineOptions()
                        .width(15)
                        .color(Color.BLUE));
                //path.setStartCap(new RoundCap());
                path.setPoints(polyPoints);
                NEW_PATH = false;
            }
        } else {
            if (BOARD_MOVING) {
                polyPoints.add(new LatLng(latitude, longitude));
                path.setPoints(polyPoints);
                float[] temp = {0};
                Location.distanceBetween(pLat, pLong, latitude, longitude, temp);
                distance = distance + (temp[0] * (float) 0.000621371);
                String tempDist = String.format("%.2f", distance);
                gpsDistText.setText("Distance: " + tempDist + " mi");
            }
        }
    }

    public void updateSensorGraphContent() {
        vSensorGraph.removeAllSeries();

        if (NUNCHUCK) {
            vSensorGraph.addSeries(NX_LineSeries);
            vSensorGraph.addSeries(NY_LineSeries);
        } else {
            vSensorGraph.removeSeries(NX_LineSeries);
            vSensorGraph.removeSeries(NY_LineSeries);
        }
        if (ACCEL) {
            vSensorGraph.addSeries(AX_LineSeries);
            vSensorGraph.addSeries(AY_LineSeries);
            vSensorGraph.addSeries(AZ_LineSeries);
        } else {
            vSensorGraph.removeSeries(AX_LineSeries);
            vSensorGraph.removeSeries(AY_LineSeries);
            vSensorGraph.removeSeries(AZ_LineSeries);
        }
        if (GYRO) {
            vSensorGraph.addSeries(GX_LineSeries);
            vSensorGraph.addSeries(GY_LineSeries);
            vSensorGraph.addSeries(GZ_LineSeries);
        } else {
            vSensorGraph.removeSeries(GX_LineSeries);
            vSensorGraph.removeSeries(GY_LineSeries);
            vSensorGraph.removeSeries(GZ_LineSeries);
        }
        if (COMP) {
            vSensorGraph.addSeries(CX_LineSeries);
            vSensorGraph.addSeries(CY_LineSeries);
            vSensorGraph.addSeries(CZ_LineSeries);
        } else {
            vSensorGraph.removeSeries(CX_LineSeries);
            vSensorGraph.removeSeries(CY_LineSeries);
            vSensorGraph.removeSeries(CZ_LineSeries);
        }
        if (HEAD) {
            vSensorGraph.addSeries(HEAD_LineSeries);
        } else {
            vSensorGraph.removeSeries(HEAD_LineSeries);
        }
        if (IMU_TEMP) {
            vSensorGraph.addSeries(IMU_Temp_LinSeries);
        } else {
            vSensorGraph.removeSeries(IMU_Temp_LinSeries);
        }
        if (LGHT)
            vSensorGraph.addSeries(LGHT_LineSeries);
        else
            vSensorGraph.removeSeries(LGHT_LineSeries);

        // setup the X axis range
        vSensorGraph.getViewport().setMinX(0);
        vSensorGraph.getViewport().setMaxX(100);
        vSensorGraph.getViewport().setXAxisBoundsManual(true);

        // setup the legend
        vSensorGraph.getLegendRenderer().setVisible(true);
        vSensorGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        vSensorGraph.getLegendRenderer().setTextSize(20);
        vSensorGraph.getLegendRenderer().setTextColor(0xEFFFFFFF);
        vSensorGraph.getLegendRenderer().setMargin(5);
        vSensorGraph.getLegendRenderer().setSpacing(3);
    }

    public void updateMotorGraphContent() {
        vMotorGraph.removeAllSeries();

        if (BC)
            vMotorGraph.addSeries(BC_LineSeries);
        else
            vMotorGraph.removeSeries(BC_LineSeries);
        if (BV)
            vMotorGraph.addSeries(BV_LineSeries);
        else
            vMotorGraph.removeSeries(BV_LineSeries);
        if (MC)
            vMotorGraph.addSeries(MC_LineSeries);
        else
            vMotorGraph.removeSeries(MC_LineSeries);
        if (DTY)
            vMotorGraph.addSeries(DTY_LineSeries);
        else
            vMotorGraph.removeSeries(DTY_LineSeries);
        if (TMP)
            vMotorGraph.addSeries(TMP_LineSeries);
        else
            vMotorGraph.removeSeries(TMP_LineSeries);
        if (RPM)
            vMotorGraph.addSeries(RPM_LineSeries);
        else
            vMotorGraph.removeSeries(RPM_LineSeries);
        if (MAHU)
            vMotorGraph.addSeries(mahU_LineSeries);
        else
            vMotorGraph.removeSeries(mahU_LineSeries);
        if (MAHC)
            vMotorGraph.addSeries(mahC_LineSeries);
        else
            vMotorGraph.removeSeries(mahC_LineSeries);
        if (WHU)
            vMotorGraph.addSeries(whU_LineSeries);
        else
            vMotorGraph.removeSeries(whU_LineSeries);
        if (WHC)
            vMotorGraph.addSeries(whC_LineSeries);
        else
            vMotorGraph.removeSeries(whC_LineSeries);

        // set the X axis range
        vMotorGraph.getViewport().setMinX(0);
        vMotorGraph.getViewport().setMaxX(100);
        vMotorGraph.getViewport().setXAxisBoundsManual(true);

        // setup the legend
        vMotorGraph.getLegendRenderer().setVisible(true);
        vMotorGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        vMotorGraph.getLegendRenderer().setTextSize(20);
        vMotorGraph.getLegendRenderer().setTextColor(0xEFFFFFFF);
        vMotorGraph.getLegendRenderer().setMargin(5);
        vMotorGraph.getLegendRenderer().setSpacing(3);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int id = tab.getPosition();

        if (id == 0) {
            // Handle the led action
            vLEDS.setVisibility(View.VISIBLE);
            vMOTOR.setVisibility(View.GONE);
            vSENSOR.setVisibility(View.GONE);
            vGPS.setVisibility(View.GONE);
        } else if (id == 1) {
            // Handle the motor data action
            vLEDS.setVisibility(View.GONE);
            vMOTOR.setVisibility(View.VISIBLE);
            vSENSOR.setVisibility(View.GONE);
            vGPS.setVisibility(View.GONE);
        } else if (id == 2) {
            // Handle the sensor data action
            vLEDS.setVisibility(View.GONE);
            vMOTOR.setVisibility(View.GONE);
            vSENSOR.setVisibility(View.VISIBLE);
            vGPS.setVisibility(View.GONE);
        } else if (id == 3) {
            // Handle the sensor data action
            vLEDS.setVisibility(View.GONE);
            vMOTOR.setVisibility(View.GONE);
            vSENSOR.setVisibility(View.GONE);
            vGPS.setVisibility(View.VISIBLE);

            int on = 0;
            try {
                on = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Exception e) {
            }
            if (on == 0) {
                showSettingsAlert();
            }
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    void SwitchToView(int mode) {
        vStatic.setVisibility(View.GONE);
        vCycle.setVisibility(View.GONE);
        vCompass.setVisibility(View.GONE);
        vThrottle.setVisibility(View.GONE);
        vRPM.setVisibility(View.GONE);
        vRPMAccel.setVisibility(View.GONE);
        vXAccel.setVisibility(View.GONE);
        vYAccel.setVisibility(View.GONE);
        vCustom.setVisibility(View.GONE);
        vDigitalStatic.setVisibility(View.GONE);
        vDigitalFuzz.setVisibility(View.GONE); // RGB animated static
        vDigitalCycle.setVisibility(View.GONE);
        vDigitalCompass.setVisibility(View.GONE);
        vDigitalThrottle.setVisibility(View.GONE);
        vDigitalRPM.setVisibility(View.GONE);
        vDigitalRPMThrottle.setVisibility(View.GONE);
        vDigitalCompassWheel.setVisibility(View.GONE);
        vDigitalCompassSnake.setVisibility(View.GONE);

        if(LED_STRIP_TYPE == 0) {
            switch (mode) {
                case 0:
                    vStatic.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    vCycle.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    vCompass.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    vThrottle.setVisibility(View.VISIBLE);
                    break;
                case 4:
                    vRPM.setVisibility(View.VISIBLE);
                    break;
                case 5:
                    vRPMAccel.setVisibility(View.VISIBLE);
                    break;
                case 6:
                    vXAccel.setVisibility(View.VISIBLE);
                    break;
                case 7:
                    vYAccel.setVisibility(View.VISIBLE);
                    break;
                case 8:
                    vCustom.setVisibility(View.VISIBLE);
                    break;
            }
        } else {
            switch (mode) {
                case 0:
                    vDigitalStatic.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    vDigitalFuzz.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    vDigitalCycle.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    vDigitalCompass.setVisibility(View.VISIBLE);
                    break;
                case 4:
                    vDigitalThrottle.setVisibility(View.VISIBLE);
                    break;
                case 5:
                    vDigitalRPM.setVisibility(View.VISIBLE);
                    break;
                case 6:
                    vDigitalRPMThrottle.setVisibility(View.VISIBLE);
                    break;
                case 7:
                    vDigitalCompassWheel.setVisibility(View.VISIBLE);
                    break;
                case 8:
                    vDigitalCompassSnake.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void updateLEDui() {
        SwitchToView(led_mode);
    }

    public void updateModeSpinner(){
        Spinner modeSpinner = (Spinner) findViewById(R.id.modes_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> modeAdapter;
        if (LED_STRIP_TYPE == 0) {
            modeAdapter = ArrayAdapter.createFromResource(this,
                    R.array.analog_modes_array, android.R.layout.simple_spinner_item);
        } else {
            modeAdapter = ArrayAdapter.createFromResource(this,
                    R.array.digital_modes_array, android.R.layout.simple_spinner_item);
        }
        // Specify the layout to use when the list of choices appears
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        modeSpinner.setAdapter(modeAdapter);
    }

    private class downloadVersionFile extends AsyncTask<Void, Void, Boolean> {
        //boolean FW_Download_Atempted = false;
        @Override
        public Boolean doInBackground(Void... params) {
            try {
                URL u = new URL("https://github.com/Samatthe/TTL-Firmware/raw/master/Release%20FW/FW%20Version.txt");

                URLConnection ucon = u.openConnection();
                ucon.setReadTimeout(5000);
                ucon.setConnectTimeout(10000);

                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                if (versionFile.exists()) {
                    versionFile.delete();
                }
                versionFile.createNewFile();

                FileOutputStream outStream = new FileOutputStream(versionFile);
                byte[] buff = new byte[5 * 1024];

                int len;
                while ((len = inStream.read(buff)) != -1) {
                    outStream.write(buff, 0, len);
                }

                outStream.flush();
                outStream.close();
                inStream.close();

                readFirmwareVersionFromFile(versionFile);

            } catch (final Exception e) {
                FW_Download_Atempted = true;
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
                return false;
            }
            FW_Download_Atempted = true;
            return true;
        }
    }

    public void readFirmwareVersionFromFile(File inputFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(versionFile));
            String line;
            Latest_FW = 0;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\.");
                for (int i = 0; i < split.length; i++) {
                    if (i == 0) {
                        Latest_FW += Integer.valueOf(split[i]) * 100;
                    } else if (i == 1) {
                        Latest_FW += Integer.valueOf(split[i]);
                    }
                }
            }
            br.close();
        } catch (final Exception e) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void updateShuffleCheckVisibility(){
        AnalogStaticShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogCycleShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogCompassShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogThrottleShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogRPMShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogRPMThrottleShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogXAccelShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogYaccelShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        AnalogCustomShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalStaticShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalCycleShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalCompassShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalThrottleShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalRPMShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalRPMThrottleShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalSkittlesShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalCompassSnakeShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
        DigitalCompassWheelShuffleLayout.setVisibility(SHUFFLE_LED_MODES? View.VISIBLE : View.GONE);
    }

    void save_LED_mode_values(SharedPreferences.Editor editor){
        Spinner ModeSpinner = findViewById(R.id.modes_spinner);
        Switch SideSwitch = findViewById(R.id.led_side_switch);
        Switch HeadSwitch = findViewById(R.id.led_hb_switch);
        Switch LightSwitch = findViewById(R.id.led_light_switch);
        Switch SensSwitch = findViewById(R.id.led_sensor_switch);
        editor.putInt("RGBType", LED_STRIP_TYPE);
        editor.putInt("LEDMode", ModeSpinner.getSelectedItemPosition());
        editor.putBoolean("HeadSwitch", HeadSwitch.isChecked());
        editor.putBoolean("SideSwitch", SideSwitch.isChecked());
        editor.putBoolean("LightSwitch", LightSwitch.isChecked());
        editor.putBoolean("SensSwitch", SensSwitch.isChecked());

        SeekBar StaticLR = findViewById(R.id.left_red_seeker);
        SeekBar StaticLG = findViewById(R.id.left_green_seeker);
        SeekBar StaticLB = findViewById(R.id.left_blue_seeker);
        SeekBar StaticRR = findViewById(R.id.right_red_seeker);
        SeekBar StaticRG = findViewById(R.id.right_green_seeker);
        SeekBar StaticRB = findViewById(R.id.right_blue_seeker);
        editor.putInt("StaticLR", StaticLR.getProgress());
        editor.putInt("StaticLG", StaticLG.getProgress());
        editor.putInt("StaticLB", StaticLB.getProgress());
        editor.putInt("StaticRR", StaticRR.getProgress());
        editor.putInt("StaticRG", StaticRG.getProgress());
        editor.putInt("StaticRB", StaticRB.getProgress());

        SeekBar CycleRate = findViewById(R.id.cycle_speed_seeker);
        SeekBar CycleBright = findViewById(R.id.cycle_bright_seeker);
        editor.putInt("CycleRate", CycleRate.getProgress());
        editor.putInt("CycleBright", CycleBright.getProgress());

        SeekBar CompBright = findViewById(R.id.compass_bright_seeker);
        editor.putInt("CompBright", CompBright.getProgress());

        SeekBar ThrottleSens = findViewById(R.id.throttle_speed_seeker);
        SeekBar ThrottleBright = findViewById(R.id.throttle_bright_seeker);
        editor.putInt("ThrottleSens", ThrottleSens.getProgress());
        editor.putInt("ThrottleBright", ThrottleBright.getProgress());

        SeekBar rpmRate = findViewById(R.id.rpm_speed_seeker);
        editor.putInt("rpmRate", rpmRate.getProgress());

        SeekBar XaccelRate = findViewById(R.id.x_accel_rate_seeker);
        editor.putInt("XaccelRate", XaccelRate.getProgress());

        SeekBar YaccelRate = findViewById(R.id.y_accel_speed_seeker);
        editor.putInt("YaccelRate", YaccelRate.getProgress());

        Spinner ColorSpin = findViewById(R.id.custom_color_select_spinner);
        Spinner RateSpin = findViewById(R.id.custom_rate_select_spinner);
        Spinner BrightSpin = findViewById(R.id.custom_brightness_select_spinner);
        SeekBar CustomLR = findViewById(R.id.custom_left_red_seeker);
        SeekBar CustomLG = findViewById(R.id.custom_left_green_seeker);
        SeekBar CustomLB = findViewById(R.id.custom_left_blue_seeker);
        SeekBar CustomRR = findViewById(R.id.custom_right_red_seeker);
        SeekBar CustomRG = findViewById(R.id.custom_right_green_seeker);
        SeekBar CustomRB = findViewById(R.id.custom_right_blue_seeker);
        SeekBar CustomRate = findViewById(R.id.custom_rate_seeker);
        SeekBar CustomBright = findViewById(R.id.custom_brightness_seeker);
        editor.putInt("ColorSpin", ColorSpin.getSelectedItemPosition());
        editor.putInt("RateSpin", RateSpin.getSelectedItemPosition());
        editor.putInt("BrightSpin", BrightSpin.getSelectedItemPosition());
        editor.putInt("CustomLR", CustomLR.getProgress());
        editor.putInt("CustomLG", CustomLG.getProgress());
        editor.putInt("CustomLB", CustomLB.getProgress());
        editor.putInt("CustomRR", CustomRR.getProgress());
        editor.putInt("CustomRG", CustomRG.getProgress());
        editor.putInt("CustomRB", CustomRB.getProgress());
        editor.putInt("CustomRate", CustomRate.getProgress());
        editor.putInt("CustomBright", CustomBright.getProgress());

        SeekBar DigitalStaticZoom = findViewById(R.id.digital_static_zoom_seeker);
        SeekBar DigitalStaticShift = findViewById(R.id.digital_static_shift_seeker);
        SeekBar DigitalStaticBright = findViewById(R.id.digital_static_bright_seeker);
        editor.putInt("DigitalStaticZoom", DigitalStaticZoom.getProgress());
        editor.putInt("DigitalStaticShift", DigitalStaticShift.getProgress());
        editor.putInt("DigitalStaticBright", DigitalStaticBright.getProgress());

        SeekBar DigitalSkittlesBright = findViewById(R.id.digital_skittles_bright_seeker);
        editor.putInt("DigitalSkittlesBright", DigitalSkittlesBright.getProgress());

        SeekBar DigitalCycleZoom = findViewById(R.id.digital_cycle_zoom_seeker);
        SeekBar DigitalCycleRate = findViewById(R.id.digital_cycle_rate_seeker);
        SeekBar DigitalCycleBright = findViewById(R.id.digital_cycle_bright_seeker);
        editor.putInt("DigitalCycleZoom", DigitalCycleZoom.getProgress());
        editor.putInt("DigitalCycleRate", DigitalCycleRate.getProgress());
        editor.putInt("DigitalCycleBright", DigitalCycleBright.getProgress());

        SeekBar DigitalCompassBright = findViewById(R.id.digital_compass_bright_seeker);
        editor.putInt("DigitalCompassBright", DigitalCompassBright.getProgress());

        SeekBar DigitalThrottleZoom = findViewById(R.id.digital_throttle_zoom_seeker);
        SeekBar DigitalThrottleShift = findViewById(R.id.digital_throttle_shift_seeker);
        SeekBar DigitalThrottleSens = findViewById(R.id.digital_throttle_sens_seeker);
        SeekBar DigitalThrottleBright = findViewById(R.id.digital_throttle_bright_seeker);
        editor.putInt("DigitalThrottleZoom", DigitalThrottleZoom.getProgress());
        editor.putInt("DigitalThrottleShift", DigitalThrottleShift.getProgress());
        editor.putInt("DigitalThrottleSens", DigitalThrottleSens.getProgress());
        editor.putInt("DigitalThrottleBright", DigitalThrottleBright.getProgress());

        SeekBar DigitalRPMZoom = findViewById(R.id.digital_rpm_zoom_seeker);
        SeekBar DigitalRPMRate = findViewById(R.id.digital_rpm_rate_seeker);
        SeekBar DigitalRPMBright = findViewById(R.id.digital_rpm_bright_seeker);
        editor.putInt("DigitalRPMZoom", DigitalRPMZoom.getProgress());
        editor.putInt("DigitalRPMRate", DigitalRPMRate.getProgress());
        editor.putInt("DigitalRPMBright", DigitalRPMBright.getProgress());

        CheckBox AnalogStaticShuffleCheck = findViewById(R.id.LED_static_shuffle_check);
        CheckBox AnalogCycleShuffleCheck = findViewById(R.id.LED_cycle_shuffle_check);
        CheckBox AnalogCompassShuffleCheck = findViewById(R.id.LED_compass_shuffle_check);
        CheckBox AnalogThrottleShuffleCheck = findViewById(R.id.LED_throttle_shuffle_check);
        CheckBox AnalogRPMShuffleCheck = findViewById(R.id.LED_rpm_shuffle_check);
        CheckBox AnalogRPMThrottleShuffleCheck = findViewById(R.id.LED_rpm_throttle_shuffle_check);
        CheckBox AnalogXAccelShuffleCheck = findViewById(R.id.LED_x_accel_shuffle_check);
        CheckBox AnalogYaccelShuffleCheck = findViewById(R.id.LED_y_accel_shuffle_check);
        CheckBox AnalogCustomShuffleCheck = findViewById(R.id.LED_custom_shuffle_check);
        CheckBox DigitalStaticShuffleCheck = findViewById(R.id.digital_static_shuffle_check);
        CheckBox DigitalCycleShuffleCheck = findViewById(R.id.digital_cycle_shuffle_check);
        CheckBox DigitalCompassShuffleCheck = findViewById(R.id.digital_compass_shuffle_check);
        CheckBox DigitalThrottleShuffleCheck = findViewById(R.id.digital_throttle_shuffle_check);
        CheckBox DigitalRPMShuffleCheck = findViewById(R.id.digital_rpm_shuffle_check);
        CheckBox DigitalRPMThrottleShuffleCheck = findViewById(R.id.digital_rpm_throttle_shuffle_check);
        CheckBox DigitalSkittlesShuffleCheck = findViewById(R.id.digital_skittles_shuffle_check);
        CheckBox DigitalCompassSnakeShuffleCheck = findViewById(R.id.digital_compass_snake_shuffle_check);
        CheckBox DigitalCompassWheelShuffleCheck = findViewById(R.id.digital_compass_wheel_shuffle_check);
        editor.putBoolean("AnalogStaticShuffleCheck", AnalogStaticShuffleCheck.isChecked());
        editor.putBoolean("AnalogCycleShuffleCheck", AnalogCycleShuffleCheck.isChecked());
        editor.putBoolean("AnalogCompassShuffleCheck", AnalogCompassShuffleCheck.isChecked());
        editor.putBoolean("AnalogThrottleShuffleCheck", AnalogThrottleShuffleCheck.isChecked());
        editor.putBoolean("AnalogRPMShuffleCheck", AnalogRPMShuffleCheck.isChecked());
        editor.putBoolean("AnalogRPMThrottleShuffleCheck", AnalogRPMThrottleShuffleCheck.isChecked());
        editor.putBoolean("AnalogXAccelShuffleCheck", AnalogXAccelShuffleCheck.isChecked());
        editor.putBoolean("AnalogYaccelShuffleCheck", AnalogYaccelShuffleCheck.isChecked());
        editor.putBoolean("AnalogCustomShuffleCheck", AnalogCustomShuffleCheck.isChecked());
        editor.putBoolean("DigitalStaticShuffleCheck", DigitalStaticShuffleCheck.isChecked());
        editor.putBoolean("DigitalCycleShuffleCheck", DigitalCycleShuffleCheck.isChecked());
        editor.putBoolean("DigitalCompassShuffleCheck", DigitalCompassShuffleCheck.isChecked());
        editor.putBoolean("DigitalThrottleShuffleCheck", DigitalThrottleShuffleCheck.isChecked());
        editor.putBoolean("DigitalRPMShuffleCheck", DigitalRPMShuffleCheck.isChecked());
        editor.putBoolean("DigitalRPMThrottleShuffleCheck", DigitalRPMThrottleShuffleCheck.isChecked());
        editor.putBoolean("DigitalSkittlesShuffleCheck", DigitalSkittlesShuffleCheck.isChecked());
        editor.putBoolean("DigitalCompassSnakeShuffleCheck", DigitalCompassSnakeShuffleCheck.isChecked());
        editor.putBoolean("DigitalCompassWheelShuffleCheck", DigitalCompassWheelShuffleCheck.isChecked());
    }
}