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
import android.app.AlertDialog;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, SeekBar.OnSeekBarChangeListener, GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, Switch.OnClickListener, ServiceCallbacks, TabLayout.OnTabSelectedListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "MyPrefsFile";

    NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification)
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

    // LED Mode ui state variables
    int led_mode = 0;
    boolean led_switch_side = false;
    boolean led_switch_hb = false;
    boolean led_switch_light = false;
    boolean led_switch_sensor = false;

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
    boolean READ_CURRENT = false;

    public LoggingService mLoggingService;
    public boolean LOG_MAP = false;
    public boolean LOG_MOTOR = false;
    public boolean LOG_SENSOR = false;
    public boolean LOG_ENABLED = false;

    // Code to manage Service lifecycle.
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                //Toast.makeText(MainActivity.this, "Binding BLE", Toast.LENGTH_SHORT).show();
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                // Automatically connects to the device upon successful start-up initialization
                if(autoConnect){                // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                    // BluetoothAdapter through BluetoothManager.
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = bluetoothManager.getAdapter();

                    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                    // fire an intent to display a dialog asking the user to grant permission to enable it.
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                    else
                        mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                }

            } else if(componentName.getClassName().equals(LoggingService.class.getName())){
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
                mLoggingService = ((LoggingService.LocalBinder) service).getService();
                if(mMap != null)
                    mLoggingService.mMap = mMap;

                mLoggingService.setCallbacks(MainActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            } else if(componentName.getClassName().equals(LoggingService.class.getName())) {
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

        vLEDS.setVisibility(View.VISIBLE);
        vMOTOR.setVisibility(View.GONE);
        vSENSOR.setVisibility(View.GONE);
        vGPS.setVisibility(View.GONE);
        vStatic.setVisibility(View.VISIBLE);
        vCycle.setVisibility(View.GONE);
        vCompass.setVisibility(View.GONE);
        vThrottle.setVisibility(View.GONE);
        vRPM.setVisibility(View.GONE);
        vRPMAccel.setVisibility(View.GONE);
        vXAccel.setVisibility(View.GONE);
        vYAccel.setVisibility(View.GONE);
        vCustom.setVisibility(View.GONE);

        Spinner modeSpinner = (Spinner) findViewById(R.id.modes_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> modeAdapter = ArrayAdapter.createFromResource(this,
                R.array.modes_array, android.R.layout.simple_spinner_item);
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

        SeekBar custom_bright_seek = (SeekBar) findViewById(R.id.custom_brightness_seeker);
        SeekBar custom_rate_seek = (SeekBar) findViewById(R.id.custom_rate_seeker);
        SeekBar custom_left_red_seek = (SeekBar) findViewById(R.id.custom_left_red_seeker);
        SeekBar custom_left_green_seek = (SeekBar) findViewById(R.id.custom_left_green_seeker);
        SeekBar custom_left_blue_seek = (SeekBar) findViewById(R.id.custom_left_blue_seeker);
        SeekBar custom_right_red_seek = (SeekBar) findViewById(R.id.custom_right_red_seeker);
        SeekBar custom_right_green_seek = (SeekBar) findViewById(R.id.custom_right_green_seeker);
        SeekBar custom_right_blue_seek = (SeekBar) findViewById(R.id.custom_right_blue_seeker);
        SeekBar left_red_seek = (SeekBar) findViewById(R.id.left_red_seeker);
        SeekBar left_green_seek = (SeekBar) findViewById(R.id.left_green_seeker);
        SeekBar left_blue_seek = (SeekBar) findViewById(R.id.left_blue_seeker);
        SeekBar right_red_seek = (SeekBar) findViewById(R.id.right_red_seeker);
        SeekBar right_green_seek = (SeekBar) findViewById(R.id.right_green_seeker);
        SeekBar right_blue_seek = (SeekBar) findViewById(R.id.right_blue_seeker);
        SeekBar cycle_speed_seek = (SeekBar) findViewById(R.id.cycle_speed_seeker);
        SeekBar cycle_bright_seek = (SeekBar) findViewById(R.id.cycle_bright_seeker);
        SeekBar comp_bright_seek = (SeekBar) findViewById(R.id.compass_bright_seeker);
        SeekBar x_accel_speed_seek = (SeekBar) findViewById(R.id.throttle_speed_seeker);
        SeekBar y_accel_speed_seek = (SeekBar) findViewById(R.id.y_accel_speed_seeker);
        SeekBar x_accel_bright_seek = (SeekBar) findViewById(R.id.throttle_bright_seeker);
        SeekBar rpm_speed_seek = (SeekBar) findViewById(R.id.rpm_speed_seeker);
        SeekBar x_accel_rate_seeker = (SeekBar) findViewById(R.id.x_accel_rate_seeker);
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

        loadPreferences();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        Intent LogServiceIntent = new Intent(this, LoggingService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        bindService(LogServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        RECIEVE_BLE_DATA = true;
        loadPreferences();

        if(menu != null) {
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
    protected void onStop(){
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

    private void loadPreferences(){
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        autoConnect = settings.getBoolean("bleConnect", false);
        LOG_MAP = settings.getBoolean("LogMap",false);
        LOG_MOTOR = settings.getBoolean("LogMotor",false);
        LOG_SENSOR = settings.getBoolean("LogSensor",false);
        LOG_ENABLED = settings.getBoolean("LogEnable", false);
        int MotorChecks = settings.getInt("MotorChecks", 0);
        int SensorChecks = settings.getInt("SensorChecks", 0);
        previous_voltage = settings.getFloat("BattVolt", 0);
        previous_mAh_used = settings.getFloat("mAhUsed",0);
        previous_mAh_charged = settings.getFloat("mAhUsed",0);
        previous_wh_used = settings.getFloat("WhUsed",0);
        previous_wh_charged = settings.getFloat("WhUsed",0);
        STATIC_LINK = settings.getBoolean("staticLink", false);
        READ_CURRENT = settings.getBoolean("ReadCurrent", false);

        WHU = ((MotorChecks & 0x200) == 0x200);
        WHC = ((MotorChecks & 0x100) == 0x100);
        BV = ((MotorChecks & 0x80) == 0x80);
        TMP = ((MotorChecks & 0x40) == 0x40);
        RPM  = ((MotorChecks & 0x20) == 0x20);
        MC = ((MotorChecks & 0x10) == 0x10);
        MAHU = ((MotorChecks & 0x8) == 0x8);
        MAHC = ((MotorChecks & 0x4) == 0x4);
        DTY = ((MotorChecks & 0x2) == 0x2);
        BC = ((MotorChecks & 0x1) == 0x1);

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

        ToggleButton MapLog = (ToggleButton) findViewById(R.id.gps_log_button);
        ToggleButton MotorLog = (ToggleButton) findViewById(R.id.motor_log_button);
        ToggleButton SensorLog = (ToggleButton) findViewById(R.id.sensor_log_button);

        MapLog.setChecked(LOG_MAP);
        MotorLog.setChecked(LOG_MOTOR);
        SensorLog.setChecked(LOG_SENSOR);

        CheckBox static_link = (CheckBox) findViewById(R.id.LED_static_link_check);
        static_link.setChecked(STATIC_LINK);

        TextView left_title = (TextView) findViewById(R.id.left_LED_text);
        RelativeLayout right_layout = (RelativeLayout) findViewById(R.id.right_LED_layout);
        if (static_link.isChecked()){
            left_title.setText("LED Color");
            right_layout.setVisibility(View.GONE);
        } else{
            left_title.setText("Left LED Color");
            right_layout.setVisibility(View.VISIBLE);
        }

        if(LOG_ENABLED){
            MapLog.setVisibility(View.VISIBLE);
            MotorLog.setVisibility(View.VISIBLE);
            SensorLog.setVisibility(View.VISIBLE);
        } else {
            MapLog.setVisibility(View.GONE);
            MotorLog.setVisibility(View.GONE);
            SensorLog.setVisibility(View.GONE);
        }
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

        int MotorChecks = (MotorCheck10 << 9)|(MotorCheck9 << 8)|(MotorCheck8<<7)|(MotorCheck7<<6)|(MotorCheck6<<5)|(MotorCheck5<<4)|(MotorCheck4<<3)|(MotorCheck3<<2)|(MotorCheck2<<1)|(MotorCheck1);
        int SensorChecks =(SensorCheck6 << 5)|(SensorCheck5 << 4)|(SensorCheck4 << 3)|(SensorCheck3 << 2)|(SensorCheck2 << 1)|(SensorCheck1);
        editor.putInt("MotorChecks", MotorChecks);
        editor.putInt("SensorChecks", SensorChecks);
        editor.putBoolean("staticLink", STATIC_LINK);
        if(mBluetoothService != null && mBluetoothService.mConnectionState == 2 && battery_voltage > 0) {
            editor.putFloat("BattVolt",battery_voltage);
            editor.putFloat("mAhUsed",mAh_used + previous_mAh_used);
            editor.putFloat("mAhCharged",mAh_charged + previous_mAh_charged);
            editor.putFloat("WhUsed",wh_used + previous_wh_used);
            editor.putFloat("WhCharged",wh_charged + previous_wh_charged);
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
            final byte txbuf[] = new byte[] {
                    (byte) 0x0AD,
                    (byte) 0x0AE
            };
            if(!mBluetoothService.writeBytes(txbuf))
                Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            //String txstring = bytesToHex(txbuf);
            // Toast.makeText(MainActivity.this, txstring, Toast.LENGTH_SHORT).show();

            return true;
        } else if (id == R.id.action_toggle) {
            final byte txbuf[] = new byte[] {
                    (byte) 0x0E3,
                    (byte) 0x0AE
            };
            if(!mBluetoothService.writeBytes(txbuf))
                Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            //String txstring = bytesToHex(txbuf);
            // Toast.makeText(MainActivity.this, txstring, Toast.LENGTH_SHORT).show();

            return true;
        } else if (id == R.id.action_ble) {
            if(mBluetoothService.mBluetoothDeviceAddress != null) {
                // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                // BluetoothAdapter through BluetoothManager.
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();

                if(mBluetoothService.mConnectionState == 0) {
                    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                    // fire an intent to display a dialog asking the user to grant permission to enable it.
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                    else {
                        bleAction.setTitle("Connecting BLE");
                        bleAction.setEnabled(false);
                        mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                    }
                } else if(mBluetoothService.mConnectionState == 2){
                    bleAction.setTitle("Disconnecting BLE");
                    bleAction.setEnabled(false);
                    mBluetoothService.disconnect();
                }
                else{
                    Toast.makeText(MainActivity.this, "Connecting: Please wait", Toast.LENGTH_SHORT).show();
                }
            }
            else {
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
        } else if(id == R.id.action_remote) {
            Intent intent = new Intent(this, RemoteActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Function to show settings alert dialog
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
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
        if(parent_id == R.id.modes_spinner){
            led_mode = pos;
            updateLEDui();
        } else if(parent_id == R.id.custom_color_select_spinner) {
            if(pos == 0) {
                static_layout.setVisibility(View.VISIBLE);
                rate_spinner.setEnabled(false);
                rate_layout.setVisibility(View.GONE);
            } else if(pos  == 2) {
                static_layout.setVisibility(View.GONE);
                rate_spinner.setEnabled(false);
                rate_layout.setVisibility(View.GONE);
            } else{
                static_layout.setVisibility(View.GONE);
                rate_spinner.setEnabled(true);
                if(rate_spinner.getSelectedItemPosition() == 0)
                    rate_layout.setVisibility(View.VISIBLE);
                else
                    rate_layout.setVisibility(View.GONE);
            }
        } else if(parent_id == R.id.custom_brightness_select_spinner) {
            if(pos == 0) {
                bright_layout.setVisibility(View.VISIBLE);
            } else{
                bright_layout.setVisibility(View.GONE);
            }
        } else if(parent_id == R.id.custom_rate_select_spinner) {
            if(pos == 0 && color_spinner.getSelectedItemPosition() != 0) {
                rate_layout.setVisibility(View.VISIBLE);
            } else{
                rate_layout.setVisibility(View.GONE);
            }
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView custom_bright_number = (TextView) findViewById(R.id.custom_brightness_number);
        TextView custom_rate_number = (TextView) findViewById(R.id.custom_rate_number);
        TextView custom_left_red_number = (TextView) findViewById(R.id.custom_left_red_number);
        TextView custom_left_green_number = (TextView) findViewById(R.id.custom_left_green_number);
        TextView custom_left_blue_number = (TextView) findViewById(R.id.custom_left_blue_number);
        TextView custom_right_red_number = (TextView) findViewById(R.id.custom_right_red_number);
        TextView custom_right_green_number = (TextView) findViewById(R.id.custom_right_green_number);
        TextView custom_right_blue_number = (TextView) findViewById(R.id.custom_right_blue_number);
        TextView left_red_number = (TextView) findViewById(R.id.left_red_number);
        TextView left_green_number = (TextView) findViewById(R.id.left_green_number);
        TextView left_blue_number = (TextView) findViewById(R.id.left_blue_number);
        TextView right_red_number = (TextView) findViewById(R.id.right_red_number);
        TextView right_green_number = (TextView) findViewById(R.id.right_green_number);
        TextView right_blue_number = (TextView) findViewById(R.id.right_blue_number);
        TextView cycle_speed_number = (TextView) findViewById(R.id.cycle_speed_number);
        TextView cycle_bright_number = (TextView) findViewById(R.id.cycle_bright_number);
        TextView comp_bright_number = (TextView) findViewById(R.id.compass_bright_number);
        TextView throttle_rate_number = (TextView) findViewById(R.id.throttle_speed_number);
        TextView throttle_bright_number = (TextView) findViewById(R.id.throttle_bright_number);
        TextView rpm_sens_number = (TextView) findViewById(R.id.rpm_speed_number);
        View left_rgb = findViewById(R.id.left_rgb_display);
        View right_rgb = findViewById(R.id.right_rgb_display);
        TextView x_accel_sens_number = (TextView) findViewById(R.id.x_accel_rate_number);
        TextView y_accel_sens_number = (TextView) findViewById(R.id.y_accel_speed_number);
        View custom_left_rgb = findViewById(R.id.custom_left_rgb_display);
        View custom_right_rgb = findViewById(R.id.custom_right_rgb_display);


        if(seekBar.getId() == R.id.left_red_seeker){
            left_red_number.setText("" + progress);
            leftColor = (leftColor & 0xFF00FFFF) | ((int)(progress * 2.55) << 16);
            left_rgb.setBackgroundColor(leftColor);
        } else if(seekBar.getId() == R.id.left_green_seeker){
            left_green_number.setText("" + progress);
            leftColor = (leftColor & 0xFFFF00FF) | ((int)(progress * 2.55) << 8);
            left_rgb.setBackgroundColor(leftColor);
        } else if(seekBar.getId() == R.id.left_blue_seeker){
            left_blue_number.setText("" + progress);
            leftColor = (leftColor & 0xFFFFFF00) | ((int)(progress * 2.55));
            left_rgb.setBackgroundColor(leftColor);
        } else if(seekBar.getId() == R.id.right_red_seeker){
            right_red_number.setText("" + progress);
            rightColor = (rightColor & 0xFF00FFFF) | ((int)(progress * 2.55) << 16);
            right_rgb.setBackgroundColor(rightColor);
        } else if(seekBar.getId() == R.id.right_green_seeker){
            right_green_number.setText("" + progress);
            rightColor = (rightColor & 0xFFFF00FF) | ((int)(progress * 2.55) << 8);
            right_rgb.setBackgroundColor(rightColor);
        } else if(seekBar.getId() == R.id.right_blue_seeker){
            right_blue_number.setText("" + progress);
            rightColor = (rightColor & 0xFFFFFF00) | ((int)(progress * 2.55));
            right_rgb.setBackgroundColor(rightColor);
        } else if(seekBar.getId() == R.id.cycle_speed_seeker){
            //cycle_rate = progress;
            cycle_speed_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.cycle_bright_seeker) {
            //cycle_brightness = progress;
            cycle_bright_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.compass_bright_seeker) {
            //comp_brightness = progress;
            comp_bright_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.throttle_speed_seeker){
            //accel_sensitivity = progress;
            throttle_rate_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.throttle_bright_seeker) {
            //accel_brightness = progress;
            throttle_bright_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.rpm_speed_seeker){
            //rpm_sensitivity = progress;
            rpm_sens_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.x_accel_rate_seeker){
            //accel_sensitivity = progress;
            x_accel_sens_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.y_accel_speed_seeker){
            //accel_sensitivity = progress;
            y_accel_sens_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.custom_brightness_seeker){
            custom_bright_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.custom_rate_seeker){
            custom_rate_number.setText("" + progress);
        } else if(seekBar.getId() == R.id.custom_left_red_seeker){
            custom_left_red_number.setText("" + progress);
            customLeftColor = (customLeftColor & 0xFF00FFFF) | ((int)(progress * 2.55) << 16);
            custom_left_rgb.setBackgroundColor(customLeftColor);
        } else if(seekBar.getId() == R.id.custom_left_green_seeker){
            custom_left_green_number.setText("" + progress);
            customLeftColor = (customLeftColor & 0xFFFF00FF) | ((int)(progress * 2.55) << 8);
            custom_left_rgb.setBackgroundColor(customLeftColor);
        } else if(seekBar.getId() == R.id.custom_left_blue_seeker){
            custom_left_blue_number.setText("" + progress);
            customLeftColor = (customLeftColor & 0xFFFFFF00) | ((int)(progress * 2.55));
            custom_left_rgb.setBackgroundColor(customLeftColor);
        } else if(seekBar.getId() == R.id.custom_right_red_seeker){
            custom_right_red_number.setText("" + progress);
            customRightColor = (customRightColor & 0xFF00FFFF) | ((int)(progress * 2.55) << 16);
            custom_right_rgb.setBackgroundColor(customRightColor);
        } else if(seekBar.getId() == R.id.custom_right_green_seeker){
            custom_right_green_number.setText("" + progress);
            customRightColor = (customRightColor & 0xFFFF00FF) | ((int)(progress * 2.55) << 8);
            custom_right_rgb.setBackgroundColor(customRightColor);
        } else if(seekBar.getId() == R.id.custom_right_blue_seeker){
            custom_right_blue_number.setText("" + progress);
            customRightColor = (customRightColor & 0xFFFFFF00) | ((int)(progress * 2.55));
            custom_right_rgb.setBackgroundColor(customRightColor);
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

        if(mLoggingService != null)
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

    public void onCheckboxClicked(View view){
        CheckBox checkbox = (CheckBox) findViewById(view.getId());
        switch(view.getId()){
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
                if (checkbox.isChecked()){
                    left_title.setText("LED Color");
                    right_layout.setVisibility(View.GONE);
                } else{
                    left_title.setText("Left LED Color");
                    right_layout.setVisibility(View.VISIBLE);
                }
                STATIC_LINK = checkbox.isChecked();
                break;
            case R.id.custom_static_link_check:
                TextView custom_left_title = (TextView) findViewById(R.id.custom_left_LED_text);
                RelativeLayout custom_right_layout = (RelativeLayout) findViewById(R.id.custom_right_LED_layout);
                if (checkbox.isChecked()){
                    custom_left_title.setText("LED Color");
                    custom_right_layout.setVisibility(View.GONE);
                } else{
                    custom_left_title.setText("Left LED Color");
                    custom_right_layout.setVisibility(View.VISIBLE);
                }
                STATIC_LINK = checkbox.isChecked();
                break;
        }
    }

    @Override
    public void onClick(View view){
        Switch vSwitch = (Switch)findViewById(view.getId());
        if(view.getId() == R.id.led_side_switch){
            led_switch_side = vSwitch.isChecked();
        } else if(view.getId() == R.id.led_hb_switch){
            led_switch_hb = vSwitch.isChecked();
        } else if(view.getId() == R.id.led_light_switch) {
            led_switch_light = vSwitch.isChecked();
        } else if(view.getId() == R.id.led_sensor_switch) {
            led_switch_sensor = vSwitch.isChecked();
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
                if(READ_CURRENT){
                    final byte txbuf[] = new byte[] {
                            (byte) 0xCD,
                            (byte) 0x0AE
                    };
                    while(!mBluetoothService.writeBytes(txbuf)){}
                        //Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                bleTitleBar.setImageResource(R.mipmap.ic_ble_black);
                bleAction.setTitle("Connect BLE");
                bleAction.setEnabled(true);
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                TextView nxyText = (TextView) findViewById(R.id.sensor_nunchuck_joy_text);
                TextView accelText = (TextView) findViewById(R.id.sensor_accel_text);
                TextView gyroText = (TextView) findViewById(R.id.sensor_gyro_text);
                TextView compText = (TextView) findViewById(R.id.sensor_compass_text);
                TextView lightText = (TextView) findViewById(R.id.sensor_light_text);
                TextView headingText = (TextView) findViewById(R.id.sensor_heading_text);
                int temp;

                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                if((data.length == 15 || data.length == 16 || data.length == 18 || data.length == 19 || data.length == 20) && RECIEVE_BLE_DATA) {
                    for (int i = 0; i < data.length; i++) {
                        temp = 0;
                        if(i >= data.length)
                            break;
                        switch (data[i] & 0xFF) {
                            case 0x11:
                                if(i+2 >= data.length)
                                    break;
                                TextView bcText = (TextView) findViewById(R.id.motor_bcurrent_text);
                                temp = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                battery_current = ((float) (temp)) / 100;
                                 BC_LineSeries.appendData(new DataPoint(graph_index, battery_current), true, MAX_DATA_POINTS);
                                bcText.setText("Battery current: " + battery_current + " A");
                                i += 2;
                                break;
                            case 0x12:
                                if(i+2 >= data.length)
                                    break;
                                TextView bvText = (TextView) findViewById(R.id.motor_volt_text);
                                temp = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                battery_voltage = ((float) (temp)) / 10;
                                BV_LineSeries.appendData(new DataPoint(graph_index, battery_voltage), true, MAX_DATA_POINTS);
                                bvText.setText("Battery voltage: " + battery_voltage + " V");

                                if(previous_voltage > battery_voltage + 1) {
                                    battery_update = 1;
                                    //showBatteryUpdateAlert();
                                } else if(previous_voltage < battery_voltage - 1) {
                                    battery_update = 2;
                                    //showBatteryUpdateAlert();
                                }
                                previous_voltage = battery_voltage;

                                i += 2;
                                break;
                            case 0x13:
                                if(i+2 >= data.length)
                                    break;
                                TextView mcText = (TextView) findViewById(R.id.motor_mcurrent_text);
                                temp = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                motor_current = ((float) (temp)) / 100;
                                MC_LineSeries.appendData(new DataPoint(graph_index, motor_current), true, MAX_DATA_POINTS);
                                mcText.setText("Motor current: " + motor_current + " A");
                                i += 2;
                                break;
                            case 0x14:
                                if(i+2 >= data.length)
                                    break;
                                TextView tempText = (TextView) findViewById(R.id.motor_temp_text);
                                temp = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                motor_temp = ((float) (temp)) / 10;
                                TMP_LineSeries.appendData(new DataPoint(graph_index, motor_temp), true, MAX_DATA_POINTS);
                                tempText.setText("MOSFET Temp: " + motor_temp + "  \u2103");
                                i += 2;
                                break;
                            case 0x15:
                                if(i+2 >= data.length)
                                    break;
                                TextView dutyText = (TextView) findViewById(R.id.motor_duty_text);
                                temp = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                duty_cycle = ((float) (temp))/10;
                                DTY_LineSeries.appendData(new DataPoint(graph_index, duty_cycle), true, MAX_DATA_POINTS);
                                dutyText.setText("Duty Cycle: " + duty_cycle + "%");
                                i+=2;
                                break;
                            case 0x16:
                                if(i+3 >= data.length)
                                    break;
                                TextView rpmText = (TextView) findViewById(R.id.motor_rpm_text);
                                motor_rpm = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                RPM_LineSeries.appendData(new DataPoint(graph_index, motor_rpm), true, MAX_DATA_POINTS);
                                rpmText.setText("Electrical speed: " + motor_rpm + " rpm");

                                if(motor_rpm < 750){
                                    BOARD_MOVING = false;
                                    NEW_PATH = true;
                                } else
                                    BOARD_MOVING = true;

                                i += 3;
                                break;
                            case 0x17:
                                if(i+3 >= data.length)
                                    break;
                                TextView mahuText = (TextView) findViewById(R.id.motor_mahu_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                mAh_used = ((float) temp) / 100;
                                mahU_LineSeries.appendData(new DataPoint(graph_index, mAh_used), true, MAX_DATA_POINTS);
                                mahuText.setText("Drawn Cap: " + mAh_used + " Ah");
                                i += 3;
                                break;
                            case 0x18:
                                if(i+3 >= data.length)
                                    break;
                                TextView mahcText = (TextView) findViewById(R.id.motor_mahc_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                mAh_charged = ((float) temp) / 100;
                                mahC_LineSeries.appendData(new DataPoint(graph_index, mAh_charged), true, MAX_DATA_POINTS);
                                mahcText.setText("Charged Cap: " + mAh_charged + " Ah");
                                i += 3;
                                break;
                            case 0x19:
                                if(i+3 >= data.length)
                                    break;
                                TextView whuText = (TextView) findViewById(R.id.motor_whu_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                wh_used = ((float) temp) / 100;
                                whU_LineSeries.appendData(new DataPoint(graph_index, wh_used), true, MAX_DATA_POINTS);
                                whuText.setText("Drawn energy: " + wh_used + " Wh");
                                i += 3;
                                break;
                            case 0x1A:
                                if(i+3 >= data.length)
                                    break;
                                TextView whcText = (TextView) findViewById(R.id.motor_whc_text);
                                temp = (((data[i + 3]) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                wh_charged = ((float) temp) / 100;
                                whC_LineSeries.appendData(new DataPoint(graph_index, wh_charged), true, MAX_DATA_POINTS);
                                whcText.setText("Charged energy: " + wh_charged + " Wh");
                                i += 3;
                                break;
                            case 0x1B:
                                if(i+1 >= data.length)
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
                                if(i+1 >= data.length)
                                    break;
                                remote_x = (data[i + 1] & 0xFF);
                                NX_LineSeries.appendData(new DataPoint(graph_index, remote_x), true, MAX_DATA_POINTS);
                                if(remote_connected == 2)
                                    nxyText.setText("Remote X: " + remote_x + "\t\tY: " + remote_y);
                                else if(remote_connected == 1)
                                    nxyText.setText("Remote X: n/a\t\tY: " + remote_y);
                                else if(remote_connected == 0)
                                    nxyText.setText("Remote X: n/a\t\tY: n/a");
                                i++;
                                break;
                            case 0x22:
                                if(i+1 >= data.length)
                                    break;
                                remote_y = (data[i + 1] & 0xFF);
                                NY_LineSeries.appendData(new DataPoint(graph_index, remote_y), true, MAX_DATA_POINTS);
                                if(remote_connected == 2)
                                    nxyText.setText("Remote X: " + remote_x + "\t\tY: " + remote_y);
                                else if(remote_connected == 1)
                                    nxyText.setText("Remote X: n/a\t\tY: " + remote_y);
                                else if(remote_connected == 0)
                                    nxyText.setText("Remote X: n/a\t\tY: n/a");
                                i++;
                                break;
                            case 0x23:
                                if(i+1 >= data.length)
                                    break;
                                TextView nz_text = (TextView) findViewById(R.id.sensor_NZ_text);
                                remote_button = ((data[i + 1] & 0x1));
                                remote_connected = ((data[i + 1] & 0x6) >> 1);
                                nz_text.setText("Remote Button: " + remote_button);
                                i++;
                                break;
                            case 0x24:
                                if(i+2 >= data.length)
                                    break;
                                accel_x = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                AX_LineSeries.appendData(new DataPoint(graph_index, accel_x), true, MAX_DATA_POINTS);
                                accelText.setText("Accel X: " + accel_x + "\t\tY: " + accel_y + "\t\tZ: " + accel_z);
                                i += 2;
                                break;
                            case 0x25:
                                if(i+2 >= data.length)
                                    break;
                                accel_y = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                AY_LineSeries.appendData(new DataPoint(graph_index, accel_y), true, MAX_DATA_POINTS);
                                accelText.setText("Accel X: " + accel_x + "\t\tY: " + accel_y + "\t\tZ: " + accel_z);
                                i += 2;
                                break;
                            case 0x26:
                                if(i+2 >= data.length)
                                    break;
                                accel_z = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                AZ_LineSeries.appendData(new DataPoint(graph_index, accel_z), true, MAX_DATA_POINTS);
                                accelText.setText("Accel X: " + accel_x + "\t\tY: " + accel_y + "\t\tZ: " + accel_z);
                                i += 2;
                                break;
                            case 0x27:
                                if(i+2 >= data.length)
                                    break;
                                gyro_x = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                GX_LineSeries.appendData(new DataPoint(graph_index, gyro_x), true, MAX_DATA_POINTS);
                                gyroText.setText("Gyro X: " + gyro_x + "\t\tY: " + gyro_y + "\t\tZ: " + gyro_z);
                                i += 2;
                                break;
                            case 0x28:
                                if(i+2 >= data.length)
                                    break;
                                gyro_y = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                GY_LineSeries.appendData(new DataPoint(graph_index, gyro_y), true, MAX_DATA_POINTS);
                                gyroText.setText("Gyro X: " + gyro_x + "\t\tY: " + gyro_y + "\t\tZ: " + gyro_z);
                                i += 2;
                                break;
                            case 0x29:
                                if(i+2 >= data.length)
                                    break;
                                gyro_z = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                GZ_LineSeries.appendData(new DataPoint(graph_index, gyro_z), true, MAX_DATA_POINTS);
                                gyroText.setText("Gyro X: " + gyro_x + "\t\tY: " + gyro_y + "\t\tZ: " + gyro_z);
                                i += 2;
                                break;
                            case 0x2A:
                                if(i+2 >= data.length)
                                    break;
                                compass_x = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                CX_LineSeries.appendData(new DataPoint(graph_index, compass_x), true, MAX_DATA_POINTS);
                                compText.setText("Compass X: " + compass_x + "\t\tY: " + compass_y + "\t\tZ: " + compass_z);
                                i += 2;
                                break;
                            case 0x2B:
                                if(i+2 >= data.length)
                                    break;
                                compass_y = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                CY_LineSeries.appendData(new DataPoint(graph_index, compass_y), true, MAX_DATA_POINTS);
                                compText.setText("Compass X: " + compass_x + "\t\tY: " + compass_y + "\t\tZ: " + compass_z);
                                i += 2;
                                break;
                            case 0x2C:
                                if(i+2 >= data.length)
                                    break;
                                compass_z = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                CZ_LineSeries.appendData(new DataPoint(graph_index, compass_z), true, MAX_DATA_POINTS);
                                compText.setText("Compass X: " + compass_x + "\t\tY: " + compass_y + "\t\tZ: " + compass_z);
                                i += 2;
                                break;
                            case 0x2D:
                                if(i+2 >= data.length)
                                    break;
                                light_sense = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                                LGHT_LineSeries.appendData(new DataPoint(graph_index, light_sense), true, MAX_DATA_POINTS);
                                lightText.setText("Light Sensor: " + light_sense);
                                i += 2;
                                break;
                            case 0x2E:
                                if(i+2 >= data.length)
                                    break;
                                heading = (((data[i + 2]) << 8) | (data[i + 1] & 0xFF));
                                heading = heading / 10;
                                HEAD_LineSeries.appendData(new DataPoint(graph_index, heading), true, MAX_DATA_POINTS);
                                headingText.setText("Heading: " + heading + "°");
                                i += 2;
                                break;
                            case 0xDE:
                                graph_index++;
                                break;
                            case 0x31:
                                if(i+1 >= data.length)
                                    break;
                                Spinner ModeSpinner = (Spinner) findViewById(R.id.modes_spinner);
                                Switch SideSwitch = (Switch) findViewById(R.id.led_side_switch);
                                Switch HeadSwitch = (Switch) findViewById(R.id.led_hb_switch);
                                Switch LightSwitch = (Switch) findViewById(R.id.led_light_switch);
                                Switch SensSwitch = (Switch) findViewById(R.id.led_sensor_switch);
                                led_mode = (data[i+1] & 0xF0) >> 4;
                                updateLEDui();
                                ModeSpinner.setSelection(led_mode);
                                HeadSwitch.setChecked(((data[i+1] & 0x08) >> 3)==1);
                                SideSwitch.setChecked(((data[i+1] & 0x04) >> 2)==1);
                                LightSwitch.setChecked(((data[i+1] & 0x02) >> 1)==1);
                                SensSwitch.setChecked((data[i+1] & 0x01)==1);
                                i += 1;
                                break;
                            case 0x32:
                                if(i+6 >= data.length)
                                    break;
                                //CheckBox LinkCheck = (CheckBox) findViewById(R.id.LED_static_link_check);
                                SeekBar StaticLR = (SeekBar) findViewById(R.id.left_red_seeker);
                                SeekBar StaticLG = (SeekBar) findViewById(R.id.left_green_seeker);
                                SeekBar StaticLB = (SeekBar) findViewById(R.id.left_blue_seeker);
                                SeekBar StaticRR = (SeekBar) findViewById(R.id.right_red_seeker);
                                SeekBar StaticRG = (SeekBar) findViewById(R.id.right_green_seeker);
                                SeekBar StaticRB = (SeekBar) findViewById(R.id.right_blue_seeker);
                                StaticLR.setProgress(data[i+1] & 0xFF);
                                StaticLG.setProgress(data[i+2] & 0xFF);
                                StaticLB.setProgress(data[i+3] & 0xFF);
                                StaticRR.setProgress(data[i+4] & 0xFF);
                                StaticRG.setProgress(data[i+5] & 0xFF);
                                StaticRB.setProgress(data[i+6] & 0xFF);
                                i += 6;
                                break;
                            case 0x33:
                                if(i+2 >= data.length)
                                    break;
                                SeekBar CycleRate = (SeekBar) findViewById(R.id.cycle_speed_seeker);
                                SeekBar CycleBright = (SeekBar) findViewById(R.id.cycle_bright_seeker);
                                CycleRate.setProgress(data[i+1] & 0xFF);
                                CycleBright.setProgress(data[i+2] & 0xFF);
                                i += 2;
                                break;
                            case 0x34:
                                if(i+1 >= data.length)
                                    break;
                                SeekBar CompBright = (SeekBar) findViewById(R.id.compass_bright_seeker);
                                CompBright.setProgress(data[i+1] & 0xFF);
                                i += 1;
                                break;
                            case 0x35:
                                if(i+2 >= data.length)
                                    break;
                                SeekBar ThrottleSens = (SeekBar) findViewById(R.id.throttle_speed_seeker);
                                SeekBar ThrottleBright = (SeekBar) findViewById(R.id.throttle_bright_seeker);
                                ThrottleSens.setProgress(data[i+1] & 0xFF);
                                ThrottleBright.setProgress(data[i+2] & 0xFF);
                                i += 2;
                                break;
                            case 0x36:
                                if(i+1 >= data.length)
                                    break;
                                SeekBar rpmRate = (SeekBar) findViewById(R.id.rpm_speed_seeker);
                                rpmRate.setProgress(data[i+1] & 0xFF);
                                i += 1;
                                break;
                            case 0x37:
                                if(i+1 >= data.length)
                                    break;
                                SeekBar XaccelRate = (SeekBar) findViewById(R.id.x_accel_rate_seeker);
                                XaccelRate.setProgress(data[i+1] & 0xFF);
                                i += 1;
                                break;
                            case 0x38:
                                if(i+1 >= data.length)
                                    break;
                                SeekBar YaccelRate = (SeekBar) findViewById(R.id.y_accel_speed_seeker);
                                YaccelRate.setProgress(data[i+1] & 0xFF);
                                i += 1;
                                break;
                            case 0x39:
                                if(i+10 >= data.length)
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
                                ColorSpin.setSelection((data[i+1] & 0xF0) >> 4);
                                BrightSpin.setSelection(data[i+1] & 0x0F);
                                RateSpin.setSelection(data[i+2] & 0xFF);
                                CustomLR.setProgress(data[i+3] & 0xFF);
                                CustomLG.setProgress(data[i+4] & 0xFF);
                                CustomLB.setProgress(data[i+5] & 0xFF);
                                CustomRR.setProgress(data[i+6] & 0xFF);
                                CustomRG.setProgress(data[i+7] & 0xFF);
                                CustomRB.setProgress(data[i+8] & 0xFF);
                                CustomRate.setProgress(data[i+9] & 0xFF);
                                CustomBright.setProgress(data[i+10] & 0xFF);
                                i += 10;
                                break;
                        }
                    }
                }
            } else if(LoggingService.ACTION_LOG_TOGGLE.equals(action)) {
                mLoggingService.ToggleLogging();
                if (mLoggingService.LOG_STARTED) {
                    logAction.setTitle("Stop Logging");
                } else {
                    logAction.setTitle("Start Logging");
                }
            } else if(LoggingService.ACTION_MAIN_CLOSE.equals(action)) {
                MainActivity.super.finish();
            } else if(LoggingService.ACTION_LED_TOGGLE.equals(action)) {
                //Toast.makeText(MainActivity.this, "toggle", Toast.LENGTH_SHORT).show();
                final byte txbuf[] = new byte[] {
                        (byte)0x0E3,
                        (byte) 0x0AE
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            } else if(LoggingService.ACTION_AUX_TOGGLE.equals(action)) {
                //Toast.makeText(MainActivity.this, "toggle", Toast.LENGTH_SHORT).show();
                byte txbuf[];
                if(AUX_PRESSED == false) {
                    txbuf = new byte[]{
                            (byte) 0x0AA,
                            (byte) 0x0AE
                    };
                    if (!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    else
                        AUX_PRESSED = true;
                } else {
                    txbuf = new byte[]{
                            (byte) 0x0AB,
                            (byte) 0x0AE
                    };
                    if (!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                    else
                        AUX_PRESSED = false;
                }
            } else if(LoggingService.ACTION_LED_MODE_DOWN.equals(action)) {
                final byte txbuf[] = new byte[] {
                        (byte)0x0E2,
                        (byte) 0x0AE
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
            } else if(LoggingService.ACTION_LED_MODE_UP.equals(action)) {
                final byte txbuf[] = new byte[] {
                        (byte) 0x0E1,
                        (byte) 0x0AE
                };
                if(!mBluetoothService.writeBytes(txbuf))
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
        intentFilter.addAction(LoggingService.ACTION_MAIN_CLOSE);
        return intentFilter;
    }

    public void onButtonClick(View view){
        final byte txbuf[];

        switch(view.getId()){
            case R.id.led_current_button:
                txbuf = new byte[] {
                        (byte) 0x0CD,
                        (byte) 0x0AE
                };
                if(!mBluetoothService.writeBytes(txbuf))
                    Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                //String txstring = bytesToHex(txbuf);
                // Toast.makeText(MainActivity.this, txstring, Toast.LENGTH_SHORT).show();
                break;

            case R.id.led_apply_button:
                Switch vSwitch;
                SeekBar vProgress;
                byte switches;

                vSwitch = (Switch)findViewById(R.id.led_side_switch);
                switches = (byte)((byte)0xFF & (byte)(vSwitch.isChecked() ? 1 : 0) << 4);
                vSwitch = (Switch)findViewById(R.id.led_hb_switch);
                switches = (byte)(switches | ((byte)(vSwitch.isChecked() ? 1 : 0)) << 5);
                vSwitch = (Switch)findViewById(R.id.led_light_switch);
                switches = (byte)(switches | ((byte)(vSwitch.isChecked() ? 1 : 0)) << 6);
                vSwitch = (Switch)findViewById(R.id.led_sensor_switch);
                switches = (byte)(switches | ((byte)(vSwitch.isChecked() ? 1 : 0)) << 7);

                if(led_mode == 0){ // Static
                    CheckBox linkCheck = (CheckBox) findViewById(R.id.LED_static_link_check);
                    if(linkCheck.isChecked()) {
                        txbuf = new byte[]{
                                (byte) 0x0ED,
                                switches,
                                (byte) ((leftColor & 0x00FF0000) >> 16), //Red
                                (byte) ((leftColor & 0x0000FF00) >> 8), //Green
                                (byte) (leftColor & 0x000000FF), //Blue
                                (byte) ((leftColor & 0x00FF0000) >> 16), //Red
                                (byte) ((leftColor & 0x0000FF00) >> 8), //Green
                                (byte) (leftColor & 0x000000FF), //Blue
                                (byte) 0x0AE
                        };
                    } else {
                        txbuf = new byte[]{
                                (byte) 0x0ED,
                                switches,
                                (byte) ((leftColor & 0x00FF0000) >> 16), //Red
                                (byte) ((leftColor & 0x0000FF00) >> 8), //Green
                                (byte) (leftColor & 0x000000FF), //Blue
                                (byte) ((rightColor & 0x00FF0000) >> 16), //Red
                                (byte) ((rightColor & 0x0000FF00) >> 8), //Green
                                (byte) (rightColor & 0x000000FF), //Blue
                                (byte) 0x0AE
                        };
                    }
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                } else if(led_mode == 1){ // Color Cycle
                    vProgress = (SeekBar)findViewById(R.id.cycle_speed_seeker);
                    byte progress1 = (byte)(vProgress.getProgress() & 0xFF);
                    vProgress = (SeekBar)findViewById(R.id.cycle_bright_seeker);
                    byte progress2 = (byte)(vProgress.getProgress() & 0xFF);
                    txbuf = new byte[] {
                            (byte)0x0EC,
                            switches,
                            progress1, //rate
                            progress2, //brightness
                            (byte) 0x0AE
                    };
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                } else if(led_mode == 2){ // Compass Cycle
                    vProgress = (SeekBar)findViewById(R.id.compass_bright_seeker);
                    byte progress1 = (byte)(vProgress.getProgress() & 0xFF);
                    txbuf = new byte[] {
                            (byte)0x0EB,
                            switches,
                            progress1, //sensitivity
                            (byte) 0x0AE
                    };
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                }else if(led_mode == 3){ // Throttle
                    vProgress = (SeekBar)findViewById(R.id.throttle_speed_seeker);
                    byte progress1 = (byte)(vProgress.getProgress() & 0xFF);
                    vProgress = (SeekBar)findViewById(R.id.throttle_bright_seeker);
                    byte progress2 = (byte)(vProgress.getProgress() & 0xFF);
                    txbuf = new byte[] {
                            (byte)0x0EA,
                            switches,
                            progress1, //sensitivity
                            progress2, //brightness
                            (byte) 0x0AE
                    };
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                } else if(led_mode == 4){ // RPM
                    vProgress = (SeekBar)findViewById(R.id.rpm_speed_seeker);
                    byte progress1 = (byte)(vProgress.getProgress() & 0xFF);
                    txbuf = new byte[] {
                            (byte)0x0E9,
                            switches,
                            progress1, //rate
                            (byte) 0x0AE
                    };
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                } else if(led_mode == 5){ // RPM + Throttle
                    txbuf = new byte[] {
                            (byte)0x0E8,
                            switches,
                            (byte) 0x0AE
                    };
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                } else if(led_mode == 6){ // X Accel
                    vProgress = (SeekBar)findViewById(R.id.x_accel_rate_seeker);
                    byte progress1 = (byte)(vProgress.getProgress() & 0xFF);
                    txbuf = new byte[] {
                            (byte)0x0E7,
                            switches,
                            progress1,
                            (byte) 0x0AE
                    };
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                } else if(led_mode == 7){ // Y Accel
                    vProgress = (SeekBar)findViewById(R.id.y_accel_speed_seeker);
                    byte progress1 = (byte)(vProgress.getProgress() & 0xFF);
                    txbuf = new byte[] {
                            (byte)0x0E6,
                            switches,
                            progress1,
                            (byte) 0x0AE
                    };
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                } else if(led_mode == 8) { // Custom
                    vProgress = (SeekBar) findViewById(R.id.custom_rate_seeker);
                    byte progress1 = (byte) (vProgress.getProgress() & 0xFF);
                    vProgress = (SeekBar) findViewById(R.id.custom_brightness_seeker);
                    byte progress2 = (byte) (vProgress.getProgress() & 0xFF);
                    Spinner ColorBaseSpin = (Spinner) findViewById(R.id.custom_color_select_spinner);
                    Spinner RateBaseSpin = (Spinner) findViewById(R.id.custom_rate_select_spinner);
                    Spinner BrightBaseSpin = (Spinner) findViewById(R.id.custom_brightness_select_spinner);
                    switches = (byte) (switches | ColorBaseSpin.getSelectedItemPosition());
                    byte RateBrightBase = (byte) ((RateBaseSpin.getSelectedItemPosition() << 4) | BrightBaseSpin.getSelectedItemPosition());
                    CheckBox linkCheck = (CheckBox) findViewById(R.id.custom_static_link_check);
                    if (linkCheck.isChecked()) {
                        txbuf = new byte[]{
                                (byte) 0x0B1,
                                switches,
                                RateBrightBase,
                                (byte) ((customLeftColor & 0x00FF0000) >> 16), //Red
                                (byte) ((customLeftColor & 0x0000FF00) >> 8), //Green
                                (byte) (customLeftColor & 0x000000FF), //Blue
                                (byte) ((customLeftColor & 0x00FF0000) >> 16), //Red
                                (byte) ((customLeftColor & 0x0000FF00) >> 8), //Green
                                (byte) (customLeftColor & 0x000000FF), //Blue
                                progress1, // Rate
                                progress2, // Brightness
                                (byte) 0x0AE
                        };
                    } else {
                        txbuf = new byte[]{
                                (byte) 0x0B1,
                                switches,
                                RateBrightBase,
                                (byte) ((customLeftColor & 0x00FF0000) >> 16), //Red
                                (byte) ((customLeftColor & 0x0000FF00) >> 8), //Green
                                (byte) (customLeftColor & 0x000000FF), //Blue
                                (byte) ((customRightColor & 0x00FF0000) >> 16), //Red
                                (byte) ((customRightColor & 0x0000FF00) >> 8), //Green
                                (byte) (customRightColor & 0x000000FF), //Blue
                                progress1, // Rate
                                progress2, // Brightness
                                (byte) 0x0AE
                        };
                    }
                    if(!mBluetoothService.writeBytes(txbuf))
                        Toast.makeText(MainActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
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
    public void updateLocation(double val, int field){
        TextView longitudetext = (TextView) findViewById(R.id.gps_maplong_text);
        TextView latitudetext = (TextView) findViewById(R.id.gps_maplat_text);
        TextView altitudetext = (TextView) findViewById(R.id.gps_mapalt_text);
        TextView speedtext = (TextView) findViewById(R.id.gps_speed_text);
        TextView maxSpeedText = (TextView) findViewById(R.id.gps_max_text);
        switch(field){
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
                altitudetext.setText("Altitude: " + (double)(Math.round(val*3.28084*100))/100 + "ft");
                break;
            case 3:
                double temp = (double)(Math.round(val*2.23694*100))/100;
                speedtext.setText("Speed: " + temp + "mph");
                if(temp > maxSpeed){// && BOARD_MOVING) {
                    maxSpeed = temp;
                    maxSpeedText.setText("Max Speed: " + maxSpeed + " mph");
                }
                break;
        }
    }

    @Override
    public void updatePath(){
        TextView gpsDistText = (TextView) findViewById(R.id.gps_dist_text);
        if(NEW_PATH){
            if(BOARD_MOVING) {
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
            if(BOARD_MOVING) {
                polyPoints.add(new LatLng(latitude, longitude));
                path.setPoints(polyPoints);
                float[] temp = {0};
                Location.distanceBetween(pLat, pLong, latitude, longitude, temp);
                distance = distance + (temp[0] * (float)0.000621371);
                String tempDist = String.format("%.2f",distance);
                gpsDistText.setText("Distance: " + tempDist + " mi");
            }
        }
    }

    public void updateSensorGraphContent(){
        vSensorGraph.removeAllSeries();

        if(NUNCHUCK){
            vSensorGraph.addSeries(NX_LineSeries);
            vSensorGraph.addSeries(NY_LineSeries);
        } else{
            vSensorGraph.removeSeries(NX_LineSeries);
            vSensorGraph.removeSeries(NY_LineSeries);
        }
        if(ACCEL) {
            vSensorGraph.addSeries(AX_LineSeries);
            vSensorGraph.addSeries(AY_LineSeries);
            vSensorGraph.addSeries(AZ_LineSeries);
        } else{
            vSensorGraph.removeSeries(AX_LineSeries);
            vSensorGraph.removeSeries(AY_LineSeries);
            vSensorGraph.removeSeries(AZ_LineSeries);
        }
        if(GYRO) {
            vSensorGraph.addSeries(GX_LineSeries);
            vSensorGraph.addSeries(GY_LineSeries);
            vSensorGraph.addSeries(GZ_LineSeries);
        } else{
            vSensorGraph.removeSeries(GX_LineSeries);
            vSensorGraph.removeSeries(GY_LineSeries);
            vSensorGraph.removeSeries(GZ_LineSeries);
        }
        if(COMP) {
            vSensorGraph.addSeries(CX_LineSeries);
            vSensorGraph.addSeries(CY_LineSeries);
            vSensorGraph.addSeries(CZ_LineSeries);
        } else{
            vSensorGraph.removeSeries(CX_LineSeries);
            vSensorGraph.removeSeries(CY_LineSeries);
            vSensorGraph.removeSeries(CZ_LineSeries);
        }
        if(HEAD) {
            vSensorGraph.addSeries(HEAD_LineSeries);
        } else{
            vSensorGraph.removeSeries(HEAD_LineSeries);
        }
        if(LGHT)
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

    public void updateMotorGraphContent(){
        vMotorGraph.removeAllSeries();

        if(BC)
            vMotorGraph.addSeries(BC_LineSeries);
        else
            vMotorGraph.removeSeries(BC_LineSeries);
        if(BV)
            vMotorGraph.addSeries(BV_LineSeries);
        else
            vMotorGraph.removeSeries(BV_LineSeries);
        if(MC)
            vMotorGraph.addSeries(MC_LineSeries);
        else
            vMotorGraph.removeSeries(MC_LineSeries);
        if(DTY)
            vMotorGraph.addSeries(DTY_LineSeries);
        else
            vMotorGraph.removeSeries(DTY_LineSeries);
        if(TMP)
            vMotorGraph.addSeries(TMP_LineSeries);
        else
            vMotorGraph.removeSeries(TMP_LineSeries);
        if(RPM)
            vMotorGraph.addSeries(RPM_LineSeries);
        else
            vMotorGraph.removeSeries(RPM_LineSeries);
        if(MAHU)
            vMotorGraph.addSeries(mahU_LineSeries);
        else
            vMotorGraph.removeSeries(mahU_LineSeries);
        if(MAHC)
            vMotorGraph.addSeries(mahC_LineSeries);
        else
            vMotorGraph.removeSeries(mahC_LineSeries);
        if(WHU)
            vMotorGraph.addSeries(whU_LineSeries);
        else
            vMotorGraph.removeSeries(whU_LineSeries);
        if(WHC)
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
            } catch (Exception e) {}
            if(on == 0) {
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
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void updateLEDui(){
        if(led_mode == 0){
            vStatic.setVisibility(View.VISIBLE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 1){
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.VISIBLE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 2){
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.VISIBLE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 3){
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.VISIBLE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 4){
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.VISIBLE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 5){
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.VISIBLE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 6){
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.VISIBLE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 7){
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.VISIBLE);
            vCustom.setVisibility(View.GONE);
        } else if(led_mode == 8) {
            vStatic.setVisibility(View.GONE);
            vCycle.setVisibility(View.GONE);
            vCompass.setVisibility(View.GONE);
            vThrottle.setVisibility(View.GONE);
            vRPM.setVisibility(View.GONE);
            vRPMAccel.setVisibility(View.GONE);
            vXAccel.setVisibility(View.GONE);
            vYAccel.setVisibility(View.GONE);
            vCustom.setVisibility(View.VISIBLE);
        }
    }
}