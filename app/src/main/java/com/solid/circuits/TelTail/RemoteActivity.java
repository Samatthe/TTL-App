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
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class RemoteActivity extends AppCompatActivity
        implements View.OnTouchListener{

    private final static String TAG = RemoteActivity.class.getSimpleName();

    public static final String PREFS_NAME = "MyPrefsFile";
    boolean AuxEnable = false;
    boolean AUX_PRESSED = false;

    int touchX = 0;
    int touchY = 0;
    int touchStartX = 0;
    int touchStartY = 0;
    int remote_connected = 0;
    private BluetoothService mBluetoothService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                if(mBluetoothService.mConnectionState!=2) {
                    Toast.makeText(RemoteActivity.this, "Connect to a Bluetooth device before use", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        ImageButton mode_down = (ImageButton)findViewById(R.id.remote_left_button);
        ImageButton LED_toggle = (ImageButton)findViewById(R.id.remote_toggle_button);
        ImageButton AUX_toggle = (ImageButton)findViewById(R.id.remote_aux_button);
        ImageButton mode_up = (ImageButton)findViewById(R.id.remote_right_button);

        AUX_toggle.setOnTouchListener(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;

        restoresettings();

        if(AuxEnable) {
            AUX_toggle.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mode_down.getLayoutParams();
            params.height = (int) (screenHeight / 9.0);
            params.width = (int) (screenWidth * (1.0 / 4.0));
            mode_down.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) LED_toggle.getLayoutParams();
            params.height = (int) (screenHeight / 9.0);
            params.width = (int) (screenWidth * (1.0 / 4.0));
            LED_toggle.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) AUX_toggle.getLayoutParams();
            params.height = (int) (screenHeight / 9.0);
            params.width = (int) (screenWidth * (1.0 / 4.0));
            AUX_toggle.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) mode_up.getLayoutParams();
            params.height = (int) (screenHeight / 9.0);
            params.width = (int) (screenWidth * (1.0 / 4.0));
            mode_up.setLayoutParams(params);
        } else{
            AUX_toggle.setVisibility(View.GONE);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mode_down.getLayoutParams();
            params.height = (int) (screenHeight / 9.0);
            params.width = (int) (screenWidth * (1.0 / 3.0));
            mode_down.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) LED_toggle.getLayoutParams();
            params.height = (int) (screenHeight / 9.0);
            params.width = (int) (screenWidth * (1.0 / 3.0));
            LED_toggle.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) mode_up.getLayoutParams();
            params.height = (int) (screenHeight / 9.0);
            params.width = (int) (screenWidth * (1.0 / 3.0));
            mode_up.setLayoutParams(params);
        }
    }

    void restoresettings(){

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        AuxEnable = settings.getBoolean("AuxEnable", false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if(mBluetoothService != null && mBluetoothService.mConnectionState!=2) {
            Toast.makeText(RemoteActivity.this, "Connect to a Bluetooth device before use", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.remote_menu, menu);
        return true;
    }

    public void onButtonClick(View view) {
        Intent broadcastIntent;
        switch (view.getId()){
            case R.id.remote_left_button:
                //Toast.makeText(RemoteActivity.this, "left", Toast.LENGTH_SHORT).show();
                broadcastIntent = new Intent(LoggingService.ACTION_LED_MODE_DOWN);
                sendBroadcast(broadcastIntent);
                break;
            case R.id.remote_toggle_button:
                //Toast.makeText(RemoteActivity.this, "toggle", Toast.LENGTH_SHORT).show();
                broadcastIntent = new Intent(LoggingService.ACTION_LED_TOGGLE);
                sendBroadcast(broadcastIntent);
                break;
            case R.id.remote_right_button:
                //Toast.makeText(RemoteActivity.this, "right", Toast.LENGTH_SHORT).show();
                broadcastIntent = new Intent(LoggingService.ACTION_LED_MODE_UP);
                sendBroadcast(broadcastIntent);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        if (id == R.id.action_remote_help) {
            Toast.makeText(RemoteActivity.this, "Feature not yet available", Toast.LENGTH_SHORT).show();
        } else if(id == R.id.action_remote_connect){
            if(mBluetoothService.mBluetoothDeviceAddress != null) {
                if(mBluetoothService.mConnectionState == 0) {
                    mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                } else if(mBluetoothService.mConnectionState == 2){
                    mBluetoothService.disconnect();
                }
                else{
                    Toast.makeText(RemoteActivity.this, "Connecting: Please wait", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(RemoteActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        CustomDrawableView remoteView = (CustomDrawableView) findViewById(R.id.remote_custom_drawable);
        if(remote_connected <= 2)
            remoteView.handleTouch(event);
        int offBaseX = 110;
        int offBaseY = 370;
        int offJoyX = 85;
        int offJoyY = 345;
        int joyLimit = 325;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP: {
                if (mBluetoothService != null && mBluetoothService.mConnectionState == 2 && remote_connected <= 2) {
                    final byte[] txbuf = new byte[]{
                            (byte) 0x0A5,
                            (byte) 0x001,
                            (byte) 0x0BD,
                            (byte) (0x080),
                            (byte) 0x05A
                    };
                    for(int i = 0; i < 3; i++){
                        //Log.d(TAG, bytesToHex(txbuf));
                        while (!mBluetoothService.writeBytes(txbuf)) {}
                    }
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                if (remote_connected <= 2) {
                    touchStartX = ((int) event.getX());
                    touchStartY = ((int) event.getY());
                } else
                    Toast.makeText(RemoteActivity.this, "        Remote connected\nDisconnect to use this remote", Toast.LENGTH_LONG).show();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (remote_connected <= 2) {
                    //touchX = ((int) event.getX());
                    touchX = touchStartX;
                    touchY = ((int) event.getY());

/*                    if ((touchY - touchStartY) < 0 && (touchX - touchStartX) >= 0) {
                        if (touchX >= (Math.cos(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * joyLimit) + touchStartX;

                        if (touchY <= (Math.sin(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * -joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(-(touchY - touchStartY), touchX - touchStartX)) * -joyLimit) + touchStartY;
                    }
                    if ((touchY - touchStartY) >= 0 && (touchX - touchStartX) < 0) {
                        if (touchX <= (Math.cos(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * -joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * -joyLimit) + touchStartX;

                        if (touchY >= (Math.sin(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(touchY - touchStartY, -(touchX - touchStartX))) * joyLimit) + touchStartY;
                    }
                    if ((touchY - touchStartY) < 0 && (touchX - touchStartX) < 0) {
                        if (touchX <= (Math.cos(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartX;

                        if (touchY <= (Math.sin(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(-(touchY - touchStartY), -(touchX - touchStartX))) * -joyLimit) + touchStartY;
                    }
                    if ((touchY - touchStartY) >= 0 && (touchX - touchStartX) >= 0) {
                        if (touchX >= (Math.cos(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartX)
                            touchX = (int) (Math.cos(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartX;

                        if (touchY >= (Math.sin(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartY)
                            touchY = (int) (Math.sin(Math.atan2(touchY - touchStartY, touchX - touchStartX)) * joyLimit) + touchStartY;
                    }*/

                    if(touchY > joyLimit + touchStartY)
                        touchY = joyLimit + touchStartY;
                    else if(touchY < -joyLimit + touchStartY)
                        touchY = -joyLimit + touchStartY;

                    if (mBluetoothService != null && mBluetoothService.mConnectionState == 2) {
                        final byte[] txbuf = new byte[]{
                                (byte) 0x0A5,
                                (byte) 0x001,
                                (byte) 0x0BD,
                                //(byte) ((int) ((touchX - touchStartX + joyLimit) * (255.0 / (2 * joyLimit)))),
                                (byte) ((int) ((touchY - touchStartY - joyLimit) * -(255.0 / (2 * joyLimit)))),
                                (byte) 0x05A
                        };
                        //Log.d(TAG, bytesToHex(txbuf));
                        mBluetoothService.writeBytes(txbuf);
                    }
                }
                break;
            }
        }
        return true;

    }
    /////////////////////////////////////////////////////////////

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                finish();
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                if(data.length == 19) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x23:
                                if(i+1 >= data.length)
                                    break;
                                remote_connected = ((data[i + 1] & 0x6) >> 1);
                                i++;
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

    public long[] genVibratorPattern( float intensity, long duration )
    {
        float dutyCycle = Math.abs( ( intensity * 2.0f ) - 1.0f );
        long hWidth = (long) ( dutyCycle * ( duration - 1 ) ) + 1;
        long lWidth = dutyCycle == 1.0f ? 0 : 1;

        int pulseCount = (int) ( 2.0f * ( (float) duration / (float) ( hWidth + lWidth ) ) );
        long[] pattern = new long[ pulseCount ];

        for( int i = 0; i < pulseCount; i++ )
        {
            pattern[i] = intensity < 0.5f ? ( i % 2 == 0 ? hWidth : lWidth ) : ( i % 2 == 0 ? lWidth : hWidth );
        }

        return pattern;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event){
        if(event.getAction() == MotionEvent.ACTION_DOWN && AUX_PRESSED == false) {
            final byte[] txbuf = new byte[]{
                    (byte) 0x0A5,
                    (byte) 0x000,
                    (byte) 0x0AA,
                    (byte) 0x05A
            };
            //Log.d(TAG, bytesToHex(txbuf));
            if(!mBluetoothService.writeBytes(txbuf)) {
                Toast.makeText(RemoteActivity.this, "Failed to send aux press command", Toast.LENGTH_SHORT).show();
            } else {
                AUX_PRESSED = true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP && AUX_PRESSED == true) {
            final byte[] txbuf = new byte[]{
                    (byte) 0x0A5,
                    (byte) 0x000,
                    (byte) 0x0AB,
                    (byte) 0x05A
            };
            //Log.d(TAG, bytesToHex(txbuf));
            if(!mBluetoothService.writeBytes(txbuf)) {
                Toast.makeText(RemoteActivity.this, "Failed to send aux release command", Toast.LENGTH_SHORT).show();
            } else {
                AUX_PRESSED = false;
            }
        }

        return true;
    }
}
