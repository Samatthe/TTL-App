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

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class RawDataActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothService mBluetoothService;
    public boolean autoConnect = false;
    private boolean NewLine = false;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                if(mBluetoothService.mConnectionState!=2) {
                    Toast.makeText(RawDataActivity.this, "Connect to a Bluetooth device first", Toast.LENGTH_SHORT).show();
                    finish();
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
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        setContentView(R.layout.activity_bluetooth_raw);
        /*TextView rawDataText = (TextView) findViewById(R.id.raw_text_view);

        rawDataText = (TextView) findViewById(R.id.raw_text_view);
        int maxLines = rawDataText.getHeight() / rawDataText.getLineHeight();
        rawDataText.setMaxLines(maxLines);*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if(autoConnect) {
            if(mBluetoothService != null) {
                if (mBluetoothService.mBluetoothDeviceAddress != null) {
                    final boolean result = mBluetoothService.connect(mBluetoothService.mBluetoothDeviceAddress);
                    //Log.d(TAG, "Connect request result=" + result);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.raw_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        TextView rawDataText = (TextView) findViewById(R.id.raw_text_view);

        int id = item.getItemId();
        if (id == R.id.action_send_hex) {
            if(item.isChecked()) {
                item.setChecked(false);
            } else {
                item.setChecked(true);
            }
            return true;
        } else if (id == R.id.action_disp_hex) {
            if(item.isChecked()) {
                item.setChecked(false);
            } else {
                item.setChecked(true);
            }
            return true;
        } else if (id == R.id.action_new_line) {
            if(item.isChecked()) {
                item.setChecked(false);
                NewLine = false;
            } else {
                item.setChecked(true);
                NewLine = true;
            }
            return true;
        }  else if (id == R.id.action_auto_scroll) {
            if(item.isChecked()) {
                item.setChecked(false);
                rawDataText.setGravity(Gravity.LEFT);
            } else {
                item.setChecked(true);
                rawDataText.setGravity(Gravity.BOTTOM);
            }
            return true;
        } else if (id == R.id.action_clear) {
            rawDataText.setText("");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onButtonClick(View view){
        EditText mSendText = (EditText) findViewById(R.id.raw_edit_text);
        switch (view.getId()) {
            case R.id.raw_send_button:
                mBluetoothService.WriteValue(mSendText.getText().toString());
                mSendText.setText("");
                break;
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            TextView rawDataText = (TextView) findViewById(R.id.raw_text_view);

            if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                //Log.e(TAG, intent.getStringExtra(BluetoothService.EXTRA_DATA));
                if(NewLine) {
                    if (rawDataText.getLineCount() == 27)
                        rawDataText.setText("");
                    rawDataText.append(bytesToHex(intent.getByteArrayExtra(BluetoothService.EXTRA_DATA)) + "\n");
                }
                else {
                    if (rawDataText.getLineCount() == 27)
                        rawDataText.setText("");
                    rawDataText.append(bytesToHex(intent.getByteArrayExtra(BluetoothService.EXTRA_DATA)));
                }
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                finish();
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
}
