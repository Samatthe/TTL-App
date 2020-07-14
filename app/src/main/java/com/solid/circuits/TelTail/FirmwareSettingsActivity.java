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

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import androidx.appcompat.app.AppCompatActivity;


public class FirmwareSettingsActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String PREFS_NAME = "MyPrefsFile";

    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;
    public boolean autoCheckFW = false;
    //public boolean onStartup = false;

    CheckBox fwAutoCheck;

    int TTL_FW = 0;
    int Latest_FW = 0;
    File versionFile;
    File firmwareFile;
    int APP_MEM_SIZE = 826; // Number of rows used for application flash memory. 1 row = 4 pages = 64*4 byes
    boolean RESPONSE_EXPECTED = false;
    boolean RESPONSE_RECIEVED = false;
    int bootloader_response = -1;
    boolean RESEND_REQUESTED = false;
    boolean UPLOAD_COMPLETE = false;
    boolean UPLOAD_FAILED = false;
    boolean FIRST_TTL_FW_READ = true;
    boolean FIRST_FILE_FW_READ = true;

    final byte fw_read[] = new byte[]{
            (byte) 0x0A5,
            (byte) 0x000,
            (byte) 0x0F9,
            (byte) 0x05A
    };
    final byte go_to_app[] = new byte[]{
            (byte) 0x47,// 'G'
            (byte) 0x32,// '2'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x23 // '#'
    };
    final byte restart_cmd[] = new byte[]{
            (byte) 0x0A5,
            (byte) 0x000,
            (byte) 0x0F8,
            (byte) 0x05A
    };
    final byte erase_mem_cmd[] = new byte[]{
            (byte) 0x58,// 'X'
            (byte) 0x23 // '#'
    };
    final byte enter_boot[] = new byte[]{
            (byte) 0x023
    };
    final byte clear_cmd_buf[] = new byte[]{
            (byte) 0x23 // '#'
    };
    final byte read_CTRLB_Buf[] = new byte[]{
            (byte) 0x77,// 'w'
            (byte) 0x34,// '4'
            (byte) 0x31,// '1'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x34,// '4'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x34,// '4'
            (byte) 0x2C,// ','
            (byte) 0x23// '#'
    };
    final byte read_ready_Buf[] = new byte[]{
            (byte) 0x6F,// 'o'
            (byte) 0x34,// '4'
            (byte) 0x31,// '1'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x34,// '4'
            (byte) 0x30,// '0'
            (byte) 0x31,// '1'
            (byte) 0x34,// '4'
            (byte) 0x2C,// ','
            (byte) 0x23// '#'
    };
    final byte erase_row_cmd[] = new byte[]{
            (byte) 0x57,// 'W'
            (byte) 0x34,// '4'
            (byte) 0x31,// '1'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x34,// '4'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x2C,// ','
            (byte) 0x41,// 'A'
            (byte) 0x35,// '5'
            (byte) 0x30,// '0'
            (byte) 0x32,// '2'
            (byte) 0x23 // '#'
    };
    final byte write_page_cmd[] = new byte[]{
            (byte) 0x57,// 'W'
            (byte) 0x34,// '4'
            (byte) 0x31,// '1'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x34,// '4'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x30,// '0'
            (byte) 0x2C,// ','
            (byte) 0x41,// 'A'
            (byte) 0x35,// '5'
            (byte) 0x30,// '0'
            (byte) 0x34,// '4'
            (byte) 0x23 // '#'
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Log.e(TAG, componentName.getClassName());
            if(componentName.getClassName().equals(BluetoothService.class.getName())) {
                mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
                final byte txbuf[] = new byte[] {
                        (byte) 0x0A5,
                        (byte) 0x000,
                        (byte) 0x0F9,
                        (byte) 0x05A
                };
                mBluetoothService.writeBytes(txbuf);
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
        setContentView(R.layout.activity_firmware_settings);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        fwAutoCheck = (CheckBox) findViewById(R.id.checkbox_auto_check_fw);

        loadPreferences();

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        File directory = new File(getFilesDir().toString());
        versionFile = new File( directory.toString()+"/FW Version.txt");
        firmwareFile = new File( directory.toString()+"/TTL FW.bin");

        downloadBinFile();
        downloadVersionFile();
    }

    @Override
    protected void onResume(){
        super.onResume();
        loadPreferences();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();

        savePreferences();
    }

    @Override
    protected void onStop(){
        super.onStop();

        savePreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        savePreferences();

        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothService = null;
    }

    private void savePreferences(){
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("fwAutoCheck", fwAutoCheck.isChecked());
        editor.commit();
    }

    private void loadPreferences(){
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        autoCheckFW = settings.getBoolean("fwAutoCheck", false);
        fwAutoCheck.setChecked(autoCheckFW);
    }


    public void onButtonClick(View view){
        switch(view.getId()){
            case R.id.check_current_fw_button: {
                Toast.makeText(FirmwareSettingsActivity.this, "Reading Current TTL FW", Toast.LENGTH_SHORT).show();
                if (!mBluetoothService.writeBytes(fw_read))
                    Toast.makeText(FirmwareSettingsActivity.this, "Connect to board and try again", Toast.LENGTH_LONG).show();
                break;
            }
            case R.id.check_latest_fw_button:
                Toast.makeText(FirmwareSettingsActivity.this, "Downloading the latest FW", Toast.LENGTH_SHORT).show();
                downloadBinFile();
                downloadVersionFile();
                break;
            case R.id.update_fw_button:{
                if(Latest_FW == 0){
                    Toast.makeText(FirmwareSettingsActivity.this, "Latest FW needs to be downloaded first", Toast.LENGTH_LONG).show();
                }else {
                    final LoadingDialog fwUpdateDialog = new LoadingDialog(FirmwareSettingsActivity.this);
                    fwUpdateDialog.startLoadingDialog();
                    fwUpdateDialog.fwProgressBar.setMax(APP_MEM_SIZE);
                    fwUpdateDialog.fwProgressBar.setProgress(0);
                    fwUpdateDialog.fwProgressText.setText("Erasing Existing FW");
                    //update_fw(fwUpdateDialog);
                    final Thread thrd = new Thread(new Runnable() {
                        public void run() {
                            update_fw(fwUpdateDialog);
                        }
                    });

                    new AlertDialog.Builder(FirmwareSettingsActivity.this)
                            .setMessage("Are you sure you want to update to the latest FW?")
                            .setIcon(R.drawable.ic_warning)
                            .setTitle("Confirm Update")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!mBluetoothService.writeBytes(restart_cmd)) {
                                        Toast.makeText(FirmwareSettingsActivity.this, "Connect to board and try again", Toast.LENGTH_SHORT).show();
                                        return;
                                    } else {
                                        Handler boot_handler = new Handler();
                                        Runnable r = new Runnable() {
                                            public void run() {
                                                thrd.start();
                                            }
                                        };
                                        boot_handler.postDelayed(r, 100);
                                    }
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    fwUpdateDialog.dismissDialog();
                                }
                            })
                            .setCancelable(false)
                            .show();//*/
                }
                break;
            }
        }
    }

    public void update_fw(LoadingDialog fwUpdateDialog){// Eenter Bootloader Mode
        if (!mBluetoothService.writeBytes(enter_boot)) {
            //Toast.makeText(FirmwareSettingsActivity.this, "Failed to send bootloader command", Toast.LENGTH_SHORT).show();

            while(!fwUpdateDialog.DialogIsDismissed()) {
                fwUpdateDialog.dismissDialog();
            }
            return;
        } else {
            // Open the FW file and read it into a byte array
            int fw_size = (int) firmwareFile.length();
            byte[] FW_Bytes = new byte[fw_size];
            int[] FW_Words = new int[fw_size/4];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(firmwareFile));
                buf.read(FW_Bytes, 0, FW_Bytes.length);
                buf.close();
            } catch (FileNotFoundException e) {
                //Toast.makeText(FirmwareSettingsActivity.this, "File not found", Toast.LENGTH_SHORT).show();
                SendGotoApp(); // Jump to application starting address

                while(!fwUpdateDialog.DialogIsDismissed()) {
                    fwUpdateDialog.dismissDialog();
                }
                return;
            } catch (IOException e) {
                //Toast.makeText(FirmwareSettingsActivity.this, "IO Exception", Toast.LENGTH_SHORT).show();
                SendGotoApp(); // Jump to application starting address

                while(!fwUpdateDialog.DialogIsDismissed()) {
                    fwUpdateDialog.dismissDialog();
                }
                return;
            }

            // Erase all of the application memory space, protecting for 0x2000 of bootloader and 0x4000 of EEPROM
            // 0x2000 - 0x3A7FF (231423 bytes)
            while (!mBluetoothService.writeBytesFast(erase_mem_cmd)){}
            RESPONSE_EXPECTED = true;
            boolean ERASE_STARTED = false;
            long timer = System.currentTimeMillis();
            int erase_fail_count = 0;
            while(bootloader_response < 0x338){//RESPONSE_RECIEVED = false){
                if(RESPONSE_RECIEVED) {
                    fwUpdateDialog.fwProgressBar.setProgress(bootloader_response + 1);
                    RESPONSE_RECIEVED = false;
                    ERASE_STARTED = true;
                }else if(!ERASE_STARTED){
                    if((System.currentTimeMillis()-timer) > 500){
                        while (!mBluetoothService.writeBytesFast(erase_mem_cmd)){}
                        timer = System.currentTimeMillis();
                        erase_fail_count++;
                    }
                    if(erase_fail_count == 10){
                        publishTitle(fwUpdateDialog,"New FW Failed to Upload. Please try again or use USB");

                        timer = System.currentTimeMillis();
                        while ((System.currentTimeMillis() - timer) < 2000) {
                        }

                        while(!fwUpdateDialog.DialogIsDismissed()) {
                            fwUpdateDialog.dismissDialog();
                        }
                        return;
                    }
                }
            }
            RESPONSE_EXPECTED = false;
            RESPONSE_RECIEVED = false;

            timer = System.currentTimeMillis();
            while((System.currentTimeMillis()-timer) < 500){}
//*/
            // Change the progress bar info for writin FW bytes
            fwUpdateDialog.fwProgressBar.setMax(fw_size);
            fwUpdateDialog.fwProgressBar.setProgress(0);
            publishTitle(fwUpdateDialog,"Writing New FW (" + 1 + "/" + fw_size + ")");


            // Write the FW to memory word by word
            // Also store the FW in a long (word) array for easy comparison during verification
            byte[] tmp_bytes = new byte[19];
            FWuploadStart(fw_size);
            RESPONSE_EXPECTED = true;
            boolean CATCH_UP = false;
            int drop_number = 0;
            int i = fw_size-1;
            while((i+1)%64 != 0){
                i++;
            }
            while(!UPLOAD_COMPLETE && !UPLOAD_FAILED){
                if(RESPONSE_RECIEVED = true && RESEND_REQUESTED && !CATCH_UP){
                    drop_number = i;
                    i = bootloader_response + 16;
                    RESEND_REQUESTED = false;
                    RESPONSE_RECIEVED = false;
                    CATCH_UP = true;
                }
                if(i<=fw_size-1) {
                    tmp_bytes[15-(i % 16)] = FW_Bytes[i];
                } else{
                    tmp_bytes[15-(i % 16)] = 0;
                }
                if((i)%16 == 0){
                    tmp_bytes[16] = (byte) (((i) & 0x0FF0000) >> 16);
                    tmp_bytes[17] = (byte) (((i) & 0x0FF00) >> 8);
                    tmp_bytes[18] = (byte) ((i) & 0x0FF);
                    while (!mBluetoothService.writeBytesFast(tmp_bytes)){}
                }
                if(!CATCH_UP) {
                    publishTitle(fwUpdateDialog,"Writing New FW (" + (fw_size-i) + "/" + fw_size + ")");
                    fwUpdateDialog.fwProgressBar.setProgress(fw_size-i);
                } else if(i == drop_number){
                    CATCH_UP = false;
                }

                if(i != 0) {
                    i--;
                    timer = System.currentTimeMillis();
                }
                else if(i == 0 && (System.currentTimeMillis()-timer) > 2000) {
                    break;
                }
            }
            RESPONSE_EXPECTED = false;
            RESPONSE_RECIEVED = false;

            if(UPLOAD_COMPLETE) {
                publishTitle(fwUpdateDialog,"New FW Uploaded Successfully!");

                ClearCommandBuff();

                timer = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timer) < 1000) {
                }

                SendGotoApp(); // Jump to application starting address
            } else{
                publishTitle(fwUpdateDialog,"New FW Failed to Uploaded");

                timer = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timer) < 2000) {
                }
            }
            UPLOAD_COMPLETE = false;
            UPLOAD_FAILED = false;
//*/
            timer = System.currentTimeMillis();
            while((System.currentTimeMillis()-timer) < 100){}

            while(!fwUpdateDialog.DialogIsDismissed()) {
                fwUpdateDialog.dismissDialog();
            }
        }
    }

    private void publishTitle(final LoadingDialog fwUpdateDialog, final String text) {
        Log.v(TAG, "reporting back from the Random Number Thread");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fwUpdateDialog.fwProgressText.setText(text);
            }
        });
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                finish();
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                if(data.length >= 1 && data.length <=4) {
                    if(RESPONSE_EXPECTED){
                        if(data.length == 1){
                            if((data[0] & 0x0FF) == 0x55){
                                UPLOAD_COMPLETE = true;
                            } else if((data[0] & 0x0FF) == 0x5A){
                                UPLOAD_FAILED = true;
                            }
                            bootloader_response = data[0] & 0x0FF;
                        }else if(data.length == 2){
                            bootloader_response = (((data[1]&0x0FF)<<8)|(data[0]&0x0FF)) & 0x0FFFF;
                        }else if(data.length == 3){
                            bootloader_response = (((data[0]&0x0FF)<<16)|((data[1]&0x0FF)<<8)|(data[2]&0x0FF)) & 0x0FFFFFF;
                        }else if(data.length == 4){
                            if(data[3] == 'R'){
                                //Toast.makeText(FirmwareSettingsActivity.this, "Resend Requested", Toast.LENGTH_SHORT).show();
                                RESEND_REQUESTED = true;
                                bootloader_response = (((data[2]&0x0FF)<<16)|((data[1]&0x0FF)<<8)|(data[0]&0x0FF)) & 0x0FFFFFF;
                            }else {
                                bootloader_response = (((data[0] & 0x0FF) << 24) | ((data[1] & 0x0FF) << 16) | ((data[2] & 0x0FF) << 8) | (data[3] & 0x0FF));
                            }
                        }
                        //RESPONSE_EXPECTED = false;
                        RESPONSE_RECIEVED = true;
                    }else {
                        for (int i = 0; i < data.length; i++) {
                            switch (data[i] & 0xFF) {
                                case 0x74:
                                    if (i + 2 >= data.length)
                                        break;
                                    TTL_FW = 0;
                                    TTL_FW += (data[i + 1] & 0x0FF);
                                    TTL_FW += (data[i + 2] & 0x0FF) * 100;
                                    TextView TTL_FW_VIEW = findViewById(R.id.current_fw_text);
                                    String TTL_FW_TEXT = "Current Firmware: v" + (TTL_FW / 100) + "." + (TTL_FW % 100);
                                    TTL_FW_VIEW.setText(TTL_FW_TEXT);
                                    i += 2;

                                    if(FIRST_TTL_FW_READ) {
                                        setWarningIcon(false);
                                        FIRST_TTL_FW_READ = false;
                                    } else
                                        setWarningIcon(true);
                                    break;
                            }
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

    private void downloadBinFile() {
        try {
            URL u = new URL("https://github.com/Samatthe/TTL-Firmware/raw/master/Release%20FW/Teltail%20HW%204v1.bin");
            URLConnection conn = u.openConnection();
            int contentLength = conn.getContentLength();

            DataInputStream stream = new DataInputStream(u.openStream());

            byte[] buffer = new byte[contentLength];
            stream.readFully(buffer);
            stream.close();

            if (firmwareFile.exists())
            {
                //Toast.makeText(FirmwareSettingsActivity.this, "Firmware: Deleted", Toast.LENGTH_SHORT).show();
                firmwareFile.delete();
            }
            firmwareFile.createNewFile();

            DataOutputStream fos = new DataOutputStream(new FileOutputStream(firmwareFile));
            //Toast.makeText(FirmwareSettingsActivity.this, firmwareFile.toString(), Toast.LENGTH_SHORT).show();
            fos.write(buffer);
            fos.flush();
            fos.close();
        } catch(FileNotFoundException e) {
            Toast.makeText(FirmwareSettingsActivity.this, "Firmware: File not found", Toast.LENGTH_SHORT).show();
            return; // swallow a 404
        } catch (IOException e) {
            Toast.makeText(FirmwareSettingsActivity.this, "Firmware: IO exception", Toast.LENGTH_SHORT).show();
            return; // swallow a 404
        }
    }

    public boolean downloadVersionFile()
    {
        try
        {
            URL u = new URL("https://github.com/Samatthe/TTL-Firmware/raw/master/Release%20FW/FW%20Version.txt");

            URLConnection ucon = u.openConnection();
            ucon.setReadTimeout(5000);
            ucon.setConnectTimeout(10000);

            InputStream is = ucon.getInputStream();
            BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

            if (versionFile.exists())
            {
                //Toast.makeText(FirmwareSettingsActivity.this, "Version: Deleted", Toast.LENGTH_SHORT).show();
                versionFile.delete();
            }
            versionFile.createNewFile();

            FileOutputStream outStream = new FileOutputStream(versionFile);
            byte[] buff = new byte[5 * 1024];

            //Toast.makeText(FirmwareSettingsActivity.this, versionFile.toString(), Toast.LENGTH_SHORT).show();

            int len;
            while ((len = inStream.read(buff)) != -1)
            {
                outStream.write(buff, 0, len);
            }

            outStream.flush();
            outStream.close();
            inStream.close();

            readFirmwareVersionFromFile(versionFile);

        }
        catch (Exception e)
        {
            Toast.makeText(FirmwareSettingsActivity.this, "Version: Exception", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void readFirmwareVersionFromFile(File inputFile){
        try {
            BufferedReader br = new BufferedReader(new FileReader(versionFile));
            String line;
            Latest_FW = 0;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\.");
                for (int i = 0; i < split.length; i++) {
                    if(i == 0) {
                        Latest_FW += Integer.valueOf(split[i])*100;
                    }else if(i == 1){
                        Latest_FW += Integer.valueOf(split[i]);
                    }
                    //Toast.makeText(FirmwareSettingsActivity.this, " "+Latest_FW, Toast.LENGTH_SHORT).show();
                }
            }
            br.close();

            TextView LATEST_FW_VIEW = findViewById(R.id.latest_fw_text);
            String LATEST_FW_TEXT = "Latest Firmware: v"+(Latest_FW/100)+"."+(Latest_FW%100);
            LATEST_FW_VIEW.setText(LATEST_FW_TEXT);

            if(FIRST_FILE_FW_READ) {
                setWarningIcon(false);
                FIRST_FILE_FW_READ = false;
            } else
                setWarningIcon(true);
        }
        catch (Exception e) {
            Toast.makeText(FirmwareSettingsActivity.this, "Read: Exception", Toast.LENGTH_SHORT).show();
        }
    }

    private void setWarningIcon(boolean ShowToasts){
        ImageView FW_WARNING_ICON = findViewById(R.id.ttl_fw_warning_icon);
        if(TTL_FW != 0 && Latest_FW != 0 && TTL_FW != Latest_FW) {
            FW_WARNING_ICON.setVisibility(View.VISIBLE);
            if(ShowToasts) {
                Toast.makeText(FirmwareSettingsActivity.this, "The TTL FW is out of date. Please update the TTL FW now.", Toast.LENGTH_LONG).show();
            }
        }else{
            if(ShowToasts) {
                if (TTL_FW == 0) {
                    Toast.makeText(FirmwareSettingsActivity.this, "The TTL FW was not read yet", Toast.LENGTH_LONG).show();
                }
                if (Latest_FW == 0) {
                    Toast.makeText(FirmwareSettingsActivity.this, "The Latest FW was not downloaded yet", Toast.LENGTH_LONG).show();
                }
                if (TTL_FW == Latest_FW && TTL_FW != 0) {
                    Toast.makeText(FirmwareSettingsActivity.this, "The TTL FW is up to date", Toast.LENGTH_LONG).show();
                }
            }
            FW_WARNING_ICON.setVisibility(View.INVISIBLE);
        }
    }

    void SendGotoApp(){
        // Jump to application starting address
        while(!mBluetoothService.writeBytes(go_to_app)) {}
    }

    void SendEraseRow(int addr){
        // Get the size of the FW array as a hex string
        String hex_addr = int2hex(addr/2, 5);

        byte write_ADDR_reg[] = new byte[]{
                (byte) 0x57,// 'W'
                (byte) 0x34,// '4'
                (byte) 0x31,// '1'
                (byte) 0x30,// '0'
                (byte) 0x30,// '0'
                (byte) 0x34,// '4'
                (byte) 0x30,// '0'
                (byte) 0x31,// '1'
                (byte) 0x43,// 'C'
                (byte) 0x2C,// ','
                (byte) hex_addr.charAt(0),// ADDR
                (byte) hex_addr.charAt(1),// ADDR
                (byte) hex_addr.charAt(2),// ADDR
                (byte) hex_addr.charAt(3),// ADDR
                (byte) hex_addr.charAt(4),// ADDR
                (byte) 0x23// '#'
        };
        while(!WaitNVMReady()){}
        while (!mBluetoothService.writeBytesFast(write_ADDR_reg)){}
        while(!WaitNVMReady()){}
        while (!mBluetoothService.writeBytesFast(erase_row_cmd)){}
    }

    void SendFWword(int addr, int data){
        String hex_addr = int2hex(addr, 8);
        String hex_data = int2hex(data, 8);

        byte write_fw_word[] = new byte[]{
                (byte) 0x57,// 'W'
                (byte) hex_addr.charAt(0),// ADDR
                (byte) hex_addr.charAt(1),// ADDR
                (byte) hex_addr.charAt(2),// ADDR
                (byte) hex_addr.charAt(3),// ADDR
                (byte) hex_addr.charAt(4),// ADDR
                (byte) hex_addr.charAt(5),// ADDR
                (byte) hex_addr.charAt(6),// ADDR
                (byte) hex_addr.charAt(7),// ADDR
                (byte) 0x2C,// ','
                (byte) hex_data.charAt(0),// DATA
                (byte) hex_data.charAt(1),// DATA
                (byte) hex_data.charAt(2),// DATA
                (byte) hex_data.charAt(3),// DATA
                (byte) hex_data.charAt(4),// DATA
                (byte) hex_data.charAt(5),// DATA
                (byte) hex_data.charAt(6),// DATA
                (byte) hex_data.charAt(7),// DATA
                (byte) 0x23// '#'
        };
        while(!mBluetoothService.writeBytesFast(write_fw_word)){}
    }

    void SendPageWriteCMD(){
        while (!mBluetoothService.writeBytesFast(write_page_cmd)){}
    }

    int ReadFWword(int addr){
        // Get the size of the FW array as a hex string
        String hex_addr = int2hex(addr, 5);

        byte read_fw_word[] = new byte[]{
                (byte) 0x77,// 'w'
                (byte) hex_addr.charAt(0),// ADDR
                (byte) hex_addr.charAt(1),// ADDR
                (byte) hex_addr.charAt(2),// ADDR
                (byte) hex_addr.charAt(3),// ADDR
                (byte) hex_addr.charAt(4),// ADDR
                (byte) 0x2C,// ','
                (byte) 0x23// '#'
        };
        RESPONSE_EXPECTED = true;
        while (!mBluetoothService.writeBytesFast(read_fw_word)){}
        long timer = System.currentTimeMillis();
        while(RESPONSE_RECIEVED == false){
            if(System.currentTimeMillis()-timer > 500){
                return -1;
            }
        }
        RESPONSE_RECIEVED = false;
        return bootloader_response;
    }

    boolean WaitNVMReady(){
        RESPONSE_EXPECTED = true;
        while (!mBluetoothService.writeBytesFast(read_ready_Buf)) {}
        long timer = System.currentTimeMillis();
        while (RESPONSE_RECIEVED == false){
            if(System.currentTimeMillis()-timer>=1250) {
                return false;
            }
        }
        if(bootloader_response == 0x01){
            RESPONSE_RECIEVED = false;
            return true;
        }else {
            RESPONSE_RECIEVED = false;
            return false;
        }
    }

    void SetManPageWrite(){
        RESPONSE_EXPECTED = true;
        while (!mBluetoothService.writeBytesFast(read_CTRLB_Buf)) {}
        while (RESPONSE_RECIEVED == false){}
        RESPONSE_EXPECTED = false;

        bootloader_response = bootloader_response | 0x080;

        SendFWword(0x41004004, bootloader_response);
    }

    void FWuploadStart(int size){
        String hex_size = int2hex(size, 8);

        byte fw_upload_start[] = new byte[]{
                (byte) 0x55,// 'U'
                (byte) hex_size.charAt(0),// ADDR
                (byte) hex_size.charAt(1),// ADDR
                (byte) hex_size.charAt(2),// ADDR
                (byte) hex_size.charAt(3),// ADDR
                (byte) hex_size.charAt(4),// ADDR
                (byte) hex_size.charAt(5),// ADDR
                (byte) hex_size.charAt(6),// ADDR
                (byte) hex_size.charAt(7),// ADDR
                (byte) 0x23 // '#'
        };
        while(!mBluetoothService.writeBytesFast(fw_upload_start)){}
    }

    String int2hex(int num, int len) {
        String tmp = Long.toHexString(num);
        for (int i = tmp.length(); i < len*2; i++) {
            tmp = '0' + tmp;
            //Toast.makeText(FirmwareSettingsActivity.this, hex_addr, Toast.LENGTH_SHORT).show();
        }

        return tmp.substring(tmp.length()-len);
        //return tmp;
    }

    void ClearCommandBuff(){
        while(!mBluetoothService.writeBytesFast(clear_cmd_buf)){}
    }
}
