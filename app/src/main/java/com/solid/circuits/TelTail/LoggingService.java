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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import android.widget.Toast;
import android.os.Build;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class LoggingService extends Service implements LocationListener{
    private final static String TAG = LoggingService.class.getSimpleName();

    public static final String PREFS_NAME = "TTLPrefsFile";

    // Registered callbacks
    private ServiceCallbacks serviceCallbacks;

    public GoogleMap mMap;
    private LocationManager locationManager;
    private static final long MIN_TIME = 1000;
    private static final float MIN_DISTANCE = 5;
    private boolean firstMapUpdate = true;
    private boolean gotAltitude = true;

    boolean AuxEnable = false;

    private boolean First_Init = true;

    public boolean LOG_STARTED = false;
    public boolean LOG_MAP_DATA = false;
    public boolean LOG_SENSOR_DATA = false;
    public boolean LOG_MOTOR_DATA = false;
    public boolean LOG_ENABLED = false;

    boolean DispNotifiaction = false;
    boolean NotifIsDisplayed = false;

    FileOutputStream fos;
    File logFile = null;

    NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF484848)
            .setPriority(Notification.PRIORITY_MAX);
    // Sets an ID for the notification
    int mNotificationId = 725;

    public final static String ACTION_LOG_TOGGLE =
            "com.solid.circuits.TelTail.ACTION_TOGGLE_LOG";
    public final static String ACTION_LED_TOGGLE =
            "com.solid.circuits.TelTail.ACTION_LED_TOGGLE";
    public final static String ACTION_AUX_TOGGLE =
            "com.solid.circuits.TelTail.ACTION_AUX_TOGGLE";
    public final static String ACTION_LED_MODE_UP =
            "com.solid.circuits.TelTail.ACTION_LED_MODE_UP";
    public final static String ACTION_LED_MODE_DOWN =
            "com.solid.circuits.TelTail.ACTION_LED_MODE_DOWN";
    public final static String ACTION_CLOSE_APP =
            "com.solid.circuits.TelTail.ACTION_CLOSE_APP";

    final int FAULT_CODE_NONE = 0;
    final int FAULT_CODE_OVER_VOLTAGE = 1;
    final int FAULT_CODE_UNDER_VOLTAGE = 2;
    final int FAULT_CODE_DRV8302 = 3;
    final int FAULT_CODE_ABS_OVER_CURRENT = 4;
    final int FAULT_CODE_OVER_TEMP_FET = 5;
    final int FAULT_CODE_OVER_TEMP_MOTOR = 6;
    int error_codes = 0;
    int last_error_code = 0;

    PendingIntent ContentPendingIntent;
    PendingIntent LEDTogglePendingIntent;
    PendingIntent AUXPendingIntent;
    PendingIntent LEDModeUpPendingIntent;
    PendingIntent LEDModeDownPendingIntent;
    PendingIntent LogStartPendingIntent;
    PendingIntent MainClosePendingIntent;

    public BluetoothService mBluetoothService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if(componentName.getClassName().equals(BluetoothService.class.getName())){
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(componentName.getClassName().equals(LoggingService.class.getName())) {
                mBluetoothService = null;
            }
        }
    };

    public class LocalBinder extends Binder {
        LoggingService getService() {
            return LoggingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        loadPreferences();

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        LOG_MAP_DATA = settings.getBoolean("LogMap", false);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps_enabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
        if (network_enabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);


        Intent ble_toggle_intent = new Intent(ACTION_LED_TOGGLE);
        Intent ble_aux_intent = new Intent(ACTION_AUX_TOGGLE);
        Intent ble_mode_up_intent = new Intent(ACTION_LED_MODE_UP);
        Intent ble_mode_down_intent = new Intent(ACTION_LED_MODE_DOWN);
        Intent log_start_intent = new Intent(ACTION_LOG_TOGGLE);
        Intent main_close_intent = new Intent(ACTION_CLOSE_APP);
        Intent main_activity_intent = new Intent(this, MainActivity.class);
        ContentPendingIntent = PendingIntent.getActivity(this, 0, main_activity_intent, PendingIntent.FLAG_UPDATE_CURRENT);
        LEDTogglePendingIntent = PendingIntent.getBroadcast(this, 0, ble_toggle_intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AUXPendingIntent = PendingIntent.getBroadcast(this, 0, ble_aux_intent, PendingIntent.FLAG_UPDATE_CURRENT);
        LEDModeUpPendingIntent = PendingIntent.getBroadcast(this, 0, ble_mode_up_intent, PendingIntent.FLAG_UPDATE_CURRENT);
        LEDModeDownPendingIntent = PendingIntent.getBroadcast(this, 0, ble_mode_down_intent, PendingIntent.FLAG_UPDATE_CURRENT);
        LogStartPendingIntent = PendingIntent.getBroadcast(this, 0, log_start_intent, PendingIntent.FLAG_UPDATE_CURRENT);
        MainClosePendingIntent = PendingIntent.getBroadcast(this, 0, main_close_intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent BluetoothServiceIntent = new Intent(this, BluetoothService.class);
        bindService(BluetoothServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        updateNotification();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLogging();

        savePreferences();

        if(!First_Init) {
            try {
                fos.close();
            } catch (Exception e) {
                Toast.makeText(LoggingService.this, "Failed to close log", Toast.LENGTH_SHORT).show();
            }
        }

        unregisterReceiver(mGattUpdateReceiver);

        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothService = null;

        stopForeground(true);

        //Toast.makeText(LoggingService.this, "Unbinding Logging", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    private void savePreferences(){
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("LogMap", LOG_MAP_DATA);
        editor.putBoolean("LogMotor", LOG_MOTOR_DATA);
        editor.putBoolean("LogSensor", LOG_SENSOR_DATA);
        // Commit the edits!
        editor.commit();
    }

    private void loadPreferences(){
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        LOG_MAP_DATA = settings.getBoolean("LogMap", false);
        LOG_MOTOR_DATA = settings.getBoolean("LogMotor", false);
        LOG_SENSOR_DATA = settings.getBoolean("LogSensor", false);
        LOG_ENABLED = settings.getBoolean("LogEnable", false);
        AuxEnable = settings.getBoolean("AuxEnable", false);
        DispNotifiaction = settings.getBoolean("DispNotif", true);
    }

    private final IBinder mBinder = new LocalBinder();

    public void writeStringToLog(String text){
        if(LOG_STARTED){
            try {
                fos.write(text.getBytes());
            } catch (Exception e){
                Toast.makeText(LoggingService.this, "Failed write log", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void writeBytesToLog(byte[] data){
        if(LOG_STARTED){
            try {
                fos.write(data);
            } catch (Exception e){
                Toast.makeText(LoggingService.this, "Failed write log", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if(mMap != null) {
            CameraPosition currentPosistion = mMap.getCameraPosition();
            CameraPosition newPosition;
            if (firstMapUpdate) {
                firstMapUpdate = false;
                newPosition = new CameraPosition.Builder()
                        .target(latLng)      // Sets the center of the map to Mountain View
                        .zoom(15)                   // Sets the zoom
                        .bearing(0)                // Sets the orientation of the camera to east
                        .tilt(45)                   // Sets the tilt of the camera to 30 degrees
                        .build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition);
                mMap.moveCamera(cameraUpdate);
            } else {
                newPosition = new CameraPosition.Builder()
                        .target(latLng)      // Sets the center of the map to Mountain View
                        .zoom(currentPosistion.zoom)                   // Sets the zoom
                        .bearing(currentPosistion.bearing)                // Sets the orientation of the camera to east
                        .tilt(currentPosistion.tilt)                   // Sets the tilt of the camera to 30 degrees
                        .build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition);
                mMap.animateCamera(cameraUpdate);
            }

            if (location.getAltitude() == 0) {
                gotAltitude = false;
                locationManager.removeUpdates(this);
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            } else if (!gotAltitude) {
                gotAltitude = true;
                locationManager.removeUpdates(this);
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            }

            if(LOG_MAP_DATA) {
                SimpleDateFormat sdf = new SimpleDateFormat("'T:'yyyy-MM-dd' 'HH:mm:ss:SS", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                writeStringToLog(sdf.format(new Date()) + "\n");
                writeStringToLog("M_LAT " + location.getLatitude() + "\n");
                writeStringToLog("M_LON " + location.getLongitude() + "\n");
                writeStringToLog("M_ALT " + location.getAltitude() + "\n");
                writeStringToLog("M_SPD " + location.getSpeed() + "\n");
            }

            serviceCallbacks.updateLocation(location.getLongitude(), 0);
            serviceCallbacks.updateLocation(location.getLatitude(), 1);
            serviceCallbacks.updateLocation(location.getAltitude(), 2);
            serviceCallbacks.updateLocation(location.getSpeed(), 3);

            serviceCallbacks.updatePath();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    public void Initialize(){
        if(First_Init)
        {
            //Toast.makeText(LoggingService.this, "Initializing Logging", Toast.LENGTH_SHORT).show();

            First_Init = false;

            String filepath = Environment.getExternalStorageDirectory().getPath();
            File dir = new File(filepath+"/TelTail");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            logFile = new File(dir, "logfile " + sdf.format(new Date()) + ".txt");

            try {
                fos = new FileOutputStream(logFile);
            } catch (Exception e){
                Toast.makeText(LoggingService.this, "Failed to open log file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startLogging(){
        if(!LOG_STARTED) {

            Initialize();

            if(LOG_MAP_DATA){
                int on = 0;
                try {
                    on = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
                } catch (Exception e) {}
                if(on == 0) {
                    Toast.makeText(LoggingService.this, "Enable GPS for location logging", Toast.LENGTH_LONG).show();
                }
            }

            LOG_STARTED = true;

            Toast.makeText(LoggingService.this, "Logging Started", Toast.LENGTH_SHORT).show();

            updateNotification();
       }
    }

    public void stopLogging(){
        if(LOG_STARTED) {
            LOG_STARTED = false;
            Toast.makeText(LoggingService.this, "Logging Stopped", Toast.LENGTH_SHORT).show();

            updateNotification();
        }
    }

    public void ToggleLogging(){
        if(LOG_STARTED)
            stopLogging();
        else
            startLogging();
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                SimpleDateFormat sdf = new SimpleDateFormat("'T:'HH:mm:ss:SS", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                writeStringToLog(sdf.format(new Date()) + "\n");
                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                for (int i = 0; i < data.length; i++){
                    if(i >= data.length)
                        break;
                    switch (data[i] & 0xFF) {
                        case 0x16:
                        case 0x17:
                        case 0x18:
                        case 0x19:
                        case 0x1A:
                            {
                                if (i + 3 >= data.length)
                                    break;
                                if(!LOG_MOTOR_DATA){
                                    i+=3;
                                    break;
                                }
                                final byte[] txbuff = new byte[]{
                                        data[i],
                                        data[i + 1],
                                        data[i + 2],
                                        data[i + 3]
                                };
                                writeBytesToLog(txbuff);
                                writeStringToLog("\n");
                                i += 3;
                                break;
                            }
                        case 0x15:
                        case 0x1B:
                        case 0x21:
                        case 0x22:
                        case 0x23:
                            {
                                if (i + 1 >= data.length)
                                    break;

                                if((data[i] & 0xFF) == 0x1B){
                                    error_codes = data[i + 1];
                                    if(error_codes != last_error_code) {
                                        switch (error_codes) {
                                            case FAULT_CODE_OVER_VOLTAGE:
                                                createFaultNotification("Over Voltage");
                                                break;
                                            case FAULT_CODE_UNDER_VOLTAGE:
                                                createFaultNotification("Under Voltage");
                                                break;
                                            case FAULT_CODE_DRV8302:
                                                createFaultNotification("DRV8302");
                                                break;
                                            case FAULT_CODE_ABS_OVER_CURRENT:
                                                createFaultNotification("ABS Over Current");
                                                break;
                                            case FAULT_CODE_OVER_TEMP_FET:
                                                createFaultNotification("Over Temp FET");
                                                break;
                                            case FAULT_CODE_OVER_TEMP_MOTOR:
                                                createFaultNotification("Over Temp Motor");
                                                break;
                                        }
                                    }
                                    last_error_code = error_codes;
                                }

                                if(((data[i] & 0x020) == 0x020 && !LOG_SENSOR_DATA) ||
                                        ((data[i] & 0x010) == 0x010 && !LOG_MOTOR_DATA)){
                                    i++;
                                    break;
                                }

                                final byte[] txbuff = new byte[]{
                                        data[i],
                                        data[i + 1]
                                };

                                writeBytesToLog(txbuff);
                                writeStringToLog("\n");
                                i++;
                                break;
                            }
                        case 0x11:
                        case 0x12:
                        case 0x13:
                        case 0x14:
                        case 0x24:
                        case 0x25:
                        case 0x26:
                        case 0x27:
                        case 0x28:
                        case 0x29:
                        case 0x2A:
                        case 0x2B:
                        case 0x2C:
                        case 0x2D:
                            {
                                if (i + 2 >= data.length)
                                    break;
                                if(((data[i] & 0x020) == 0x020 && !LOG_SENSOR_DATA) ||
                                        ((data[i] & 0x010) == 0x010 && !LOG_MOTOR_DATA)){
                                    i+=2;
                                    break;
                                }
                                final byte[] txbuff = new byte[]{
                                        data[i],
                                        data[i + 1],
                                        data[i + 2]
                                };
                                writeBytesToLog(txbuff);
                                writeStringToLog("\n");
                                i += 2;
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
        intentFilter.addAction(ACTION_LED_TOGGLE);
        return intentFilter;
    }

    public void updateNotification(){
        if(DispNotifiaction) {
            loadPreferences();

            mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(0xFF484848)
                    .setPriority(Notification.PRIORITY_MAX);

            //mBuilder.setContentTitle("TelTail Lights Running");
            // Because clicking the notification opens a new ("special") activity, there's
            // no need to create an artificial back stack.

            mBuilder.setContentIntent(ContentPendingIntent);
            // Apply the media style template
            if (LOG_ENABLED)
                mBuilder.addAction(R.mipmap.ic_log, "", LogStartPendingIntent);
            mBuilder.addAction(R.mipmap.ic_left, "", LEDModeDownPendingIntent);
            mBuilder.addAction(R.mipmap.ic_power, "", LEDTogglePendingIntent);
            if (AuxEnable)
                mBuilder.addAction(R.mipmap.ic_action_aux_black, "", AUXPendingIntent);
            mBuilder.addAction(R.mipmap.ic_right, "", LEDModeUpPendingIntent);
            mBuilder.addAction(R.mipmap.ic_close, "", MainClosePendingIntent);

            Drawable myDrawable = getResources().getDrawable(R.mipmap.ic_ttl_normal);
            Bitmap LargeIcon = ((BitmapDrawable) myDrawable).getBitmap();
            mBuilder.setLargeIcon(LargeIcon);
            mBuilder.setShowWhen(false);

            if (LOG_ENABLED) {
                if (LOG_STARTED)
                    mBuilder.setContentText("Logging...");
                else
                    mBuilder.setContentText("Ready to log");
                if (AuxEnable) {
                    mBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(1, 2, 4));
                } else {
                    mBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(1, 2, 3));
                }
            } else {
                //mBuilder.setContentText("");
                if (AuxEnable) {
                    mBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 3));
                } else {
                    mBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2));
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startMyOwnForeground();
            else
                startForeground(mNotificationId, mBuilder.build());
            NotifIsDisplayed = true;
        } else if(NotifIsDisplayed){
            //NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //manager.cancelAll();
            //TODO: close the notifiaction
            NotifIsDisplayed = false;
            Toast.makeText(mBluetoothService, "Restart app to hide notification", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMyOwnForeground(){
            String NOTIFICATION_CHANNEL_ID = "com.solid.circuits.TelTail";
            String channelName = "TelTail Lights";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MAX);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("App is running")
                    .setPriority(NotificationManager.IMPORTANCE_MAX)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setColor(0xFF484848)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(ContentPendingIntent);

            //mBuilder.setContentTitle("TelTail Lights Running");
            // Because clicking the notification opens a new ("special") activity, there's
            // no need to create an artificial back stack.

            // Apply the media style template
            if (LOG_ENABLED)
                notificationBuilder.addAction(R.mipmap.ic_log, "", LogStartPendingIntent);
            notificationBuilder.addAction(R.mipmap.ic_left, "", LEDModeDownPendingIntent);
            notificationBuilder.addAction(R.mipmap.ic_power, "", LEDTogglePendingIntent);
            if (AuxEnable)
                notificationBuilder.addAction(R.mipmap.ic_action_aux_black, "", AUXPendingIntent);
            notificationBuilder.addAction(R.mipmap.ic_right, "", LEDModeUpPendingIntent);
            notificationBuilder.addAction(R.mipmap.ic_close, "", MainClosePendingIntent);

            Drawable myDrawable = getResources().getDrawable(R.mipmap.ic_ttl_normal);
            Bitmap LargeIcon = ((BitmapDrawable) myDrawable).getBitmap();
            notificationBuilder.setLargeIcon(LargeIcon);
            notificationBuilder.setShowWhen(false);

            if (LOG_ENABLED) {
                if (LOG_STARTED)
                    notificationBuilder.setContentText("Logging...");
                else
                    notificationBuilder.setContentText("Ready to log");
                if (AuxEnable) {
                    notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(1, 2, 4));
                } else {
                    notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(1, 2, 3));
                }
            } else {
                //mBuilder.setContentText("");
                if (AuxEnable) {
                    notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 3));
                } else {
                    notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2));
                }
            }//*/
            Notification notification = notificationBuilder.build();
            startForeground(mNotificationId, notification);
    }

    int mFaultNotifId = 1;
    public void createFaultNotification(String fault){
        if(DispNotifiaction) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

            NotificationCompat.Builder mFaultNotif = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(0xFF484848);

            mFaultNotif.setContentTitle("Fault Detected: " + fault);

            mFaultNotif.setContentIntent(ContentPendingIntent);

            Drawable myDrawable = getResources().getDrawable(R.mipmap.ic_ttl_normal);
            Bitmap LargeIcon = ((BitmapDrawable) myDrawable).getBitmap();
            mFaultNotif.setLargeIcon(LargeIcon);
            mFaultNotif.setShowWhen(true);

            notificationManager.notify(mFaultNotifId, mFaultNotif.build());
            mFaultNotifId++;
        }
    }
}