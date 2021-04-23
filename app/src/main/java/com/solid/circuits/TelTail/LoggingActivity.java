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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;


public class LoggingActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "TTLPrefsFile";

    CheckBox enableCheck;
    TextView logSizeText1;
    EditText logSizeText2;
    TextView logSizeText3;

    public int logSize = 0;
    public boolean logEnable = false;
    public boolean logStart = false;

    public LoggingService mLoggingService;

    // Code to manage Service lifecycle.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if(componentName.getClassName().equals(LoggingService.class.getName())){
                //Toast.makeText(MainActivity.this, "Binding Log", Toast.LENGTH_SHORT).show();
                mLoggingService = ((LoggingService.LocalBinder) service).getService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(componentName.getClassName().equals(LoggingService.class.getName())) {
                mLoggingService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        enableCheck = (CheckBox) findViewById(R.id.checkbox_log_enable);
        logSizeText1 = (TextView) findViewById(R.id.log_size_text);
        logSizeText2 = (EditText) findViewById(R.id.log_size_text1);
        logSizeText3 = (TextView) findViewById(R.id.log_size_text2);

        logSizeText2.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                try {
                    logSize = Integer.parseInt(logSizeText2.getText().toString());
                } catch (NumberFormatException nfe) {
                    //Log.e(TAG, "Could not parse " + nfe);
                }
            }
        });

        Intent LogServiceIntent = new Intent(this, LoggingService.class);
        bindService(LogServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        restoresettings();
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
    }

    public void onCheckboxClicked(View view) {
        CheckBox checkbox = (CheckBox) findViewById(view.getId());

        switch (view.getId()) {
            case R.id.checkbox_log_enable:
                //Toast.makeText(LoggingActivity.this, "Enable is" + checkbox.isChecked(), Toast.LENGTH_SHORT).show();
                logSizeText1.setEnabled(checkbox.isChecked());
                logSizeText2.setEnabled(checkbox.isChecked());
                logSizeText3.setEnabled(checkbox.isChecked());
                break;
        }
    }

    void savesettings(){

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("LogSize", logSize);
        editor.putBoolean("LogEnable", enableCheck.isChecked());

        mLoggingService.updateNotification();

        // Commit the edits!
        editor.commit();
    }

    void restoresettings(){

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        logSize = settings.getInt("LogSize", 100);
        logEnable = settings.getBoolean("LogEnable", false);
        logStart = settings.getBoolean("LogStart", true);
        enableCheck.setChecked(logEnable);
        logSizeText1.setEnabled(logEnable);
        logSizeText2.setEnabled(logEnable);
        logSizeText3.setEnabled(logEnable);

        EditText LogSize = findViewById(R.id.log_size_text1);
        LogSize.setText(""+LogSize);
    }
}
