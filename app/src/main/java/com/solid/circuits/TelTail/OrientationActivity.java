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
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;


public class OrientationActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    int connector_orientation = 0;
    int power_orientation = 0;

    boolean CHECK_DATA = false;

    private BluetoothService mBluetoothService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();

                final byte txbuf[] = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0FE,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(OrientationActivity.this, "Could not read orientation\nPlease try again", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_orientation);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Spinner con_orient_spinner = (Spinner) findViewById(R.id.connect_orientation_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> orient_adapter = ArrayAdapter.createFromResource(this,
                R.array.orientation_array, R.layout.gps_spinner_item);
        // Specify the layout to use when the list of choices appears
        orient_adapter.setDropDownViewResource(R.layout.gps_spinner_item);
        // Apply the adapter to the spinner
        con_orient_spinner.setAdapter(orient_adapter);
        con_orient_spinner.setOnItemSelectedListener(this);

        Spinner pow_orient_spinner = (Spinner) findViewById(R.id.power_orientation_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        //ArrayAdapter<CharSequence> pow_orient_adapter = ArrayAdapter.createFromResource(this,
        //        R.array.orientation_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        //pow_orient_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        pow_orient_spinner.setAdapter(orient_adapter);
        pow_orient_spinner.setOnItemSelectedListener(this);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        con_orient_spinner.setSelection(0);
        pow_orient_spinner.setSelection(0);
    }

    @Override
    protected  void onResume(){
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        long parent_id = parent.getId();
        if(parent_id == R.id.connect_orientation_spinner) {
            connector_orientation = pos;
        } else if(parent_id == R.id.power_orientation_spinner) {
            power_orientation = pos;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onButtonClick(View view) {
        byte txbuf[];
        switch (view.getId()) {
            case R.id.orientation_read_button:
                txbuf = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0FE,
                        (byte) 0x05A
                };
                if(!mBluetoothService.writeBytes(txbuf)) {
                    Toast.makeText(OrientationActivity.this, "Could not read orientation\nPlease try again", Toast.LENGTH_SHORT).show();

                    Spinner spin1 = (Spinner) findViewById(R.id.connect_orientation_spinner);
                    Spinner spin2 = (Spinner) findViewById(R.id.power_orientation_spinner);
                    spin1.setSelection(0);
                    spin2.setSelection(0);
                }
                break;
            case R.id.orientation_apply_button:
                if(connector_orientation > 0 && power_orientation > 0 &&
                        connector_orientation != power_orientation &&
                        (connector_orientation-1)/2 != (power_orientation-1)/2) {
                    txbuf = new byte[]{
                            (byte) 0x0A5,
                            (byte) 0x002,
                            (byte) 0x0FD,
                            (byte) connector_orientation,
                            (byte) power_orientation,
                            (byte) 0x05A
                    };
                    if (!mBluetoothService.writeBytes(txbuf)) {
                        Toast.makeText(OrientationActivity.this, "Could write orientation\nPlease try again", Toast.LENGTH_SHORT).show();
                    } else {
                        txbuf = new byte[] {
                                (byte) 0x0A5,
                                (byte) 0x000,
                                (byte) 0x0FE,
                                (byte) 0x05A
                        };
                        while(!mBluetoothService.writeBytes(txbuf)) {}
                        CHECK_DATA = true;
                    }
                } else {
                    Toast.makeText(OrientationActivity.this, "Orientation Not Valid", Toast.LENGTH_SHORT).show();
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
                if(data.length == 3) {
                    for (int i = 0; i < data.length; i++) {
                        switch (data[i] & 0xFF) {
                            case 0x71:
                                if(i+2 >= data.length)
                                    break;
                                //Toast.makeText(OrientationActivity.this, "Con: "+String.valueOf(data[i + 1] & 0xFF), Toast.LENGTH_SHORT).show();
                                if(CHECK_DATA) {
                                    boolean dataCorrect = true;
                                    if(connectorOrientaion.getSelectedItemPosition() != (data[i + 1] & 0xFF)) dataCorrect = false;
                                    if(powerOrientaion.getSelectedItemPosition() != (data[i + 2] & 0xFF)) dataCorrect = false;
                                    if(dataCorrect){
                                        Toast.makeText(OrientationActivity.this, "Orientation applied successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(OrientationActivity.this, "Orientation failed to apply\nPlease try again", Toast.LENGTH_SHORT).show();
                                    }
                                    CHECK_DATA = false;
                                } else {
                                    connectorOrientaion.setSelection(data[i + 1] & 0xFF);
                                    powerOrientaion.setSelection(data[i + 2] & 0xFF);
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
}
