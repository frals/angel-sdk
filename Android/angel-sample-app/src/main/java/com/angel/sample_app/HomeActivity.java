/*
 * Copyright (c) 2015, Seraphim Sense Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 *    and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.angel.sample_app;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.angel.sample_app.persistence.DatabaseHelper;
import com.angel.sample_app.persistence.ReadingsContract;
import com.angel.sdk.BleCharacteristic;
import com.angel.sdk.BleDevice;
import com.angel.sdk.ChAccelerationEnergyMagnitude;
import com.angel.sdk.ChAccelerationWaveform;
import com.angel.sdk.ChBatteryLevel;
import com.angel.sdk.ChHeartRateMeasurement;
import com.angel.sdk.ChOpticalWaveform;
import com.angel.sdk.ChStepCount;
import com.angel.sdk.ChTemperatureMeasurement;
import com.angel.sdk.SrvActivityMonitoring;
import com.angel.sdk.SrvBattery;
import com.angel.sdk.SrvHealthThermometer;
import com.angel.sdk.SrvHeartRate;
import com.angel.sdk.SrvWaveformSignal;

import junit.framework.Assert;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class HomeActivity extends Activity {

    private DatabaseHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);
        orientation = getResources().getConfiguration().orientation;

        SharedPreferences preferences = getApplicationContext().getSharedPreferences("ble", Context.MODE_PRIVATE);
        mBleDeviceAddress = preferences.getString("ble", "");
        if (mBleDeviceAddress.isEmpty()) {
            throw new RuntimeException("Activity started without a BT device, abort!");
        }

        mHandler = new Handler(this.getMainLooper());

        Intent intent = new Intent(getApplicationContext(), BluetoothService.class);
        startService(intent);

        mDbHelper = new DatabaseHelper(getApplicationContext());

        mPeriodicReader = new Runnable() {
            @Override
            public void run() {
                updateReadings();
                mHandler.postDelayed(mPeriodicReader, RSSI_UPDATE_INTERVAL);
            }
        };
        updateReadings();
        scheduleUpdaters();
    }

    private void updateReadings() {
        getLastTemp();
        getLastBPM();
        getLastBat();
    }

    private void getLastBat() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor =
                db.query(ReadingsContract.SensorEntry.TABLE_NAME, // a. table
                        new String[]{ReadingsContract.SensorEntry.COLUMN_NAME_TIMESTAMP, ReadingsContract.SensorEntry.COLUMN_NAME_SENSOR, ReadingsContract.SensorEntry.COLUMN_NAME_VALUE}, // b. column names
                        ReadingsContract.SensorEntry.COLUMN_NAME_SENSOR + " = 'battery'", // c. selections
                        null, // d. selections args
                        null, // e. group by
                        null, // f. having
                        "TIMESTAMP DESC", // g. order by
                        "1"); // h. limit

        if (cursor != null)
            cursor.moveToFirst();

        displayBatteryLevel(Integer.parseInt(cursor.getString(2)));
    }

    private void getLastBPM() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor =
                db.query(ReadingsContract.SensorEntry.TABLE_NAME, // a. table
                        new String[]{ReadingsContract.SensorEntry.COLUMN_NAME_TIMESTAMP, ReadingsContract.SensorEntry.COLUMN_NAME_SENSOR, ReadingsContract.SensorEntry.COLUMN_NAME_VALUE}, // b. column names
                        ReadingsContract.SensorEntry.COLUMN_NAME_SENSOR + " = 'hr'", // c. selections
                        null, // d. selections args
                        null, // e. group by
                        null, // f. having
                        "TIMESTAMP DESC", // g. order by
                        "1"); // h. limit

        if (cursor != null)
            cursor.moveToFirst();

        displayHeartRate(cursor.getString(2));
    }

    private void getLastTemp() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor =
                db.query(ReadingsContract.SensorEntry.TABLE_NAME, // a. table
                        new String[]{ReadingsContract.SensorEntry.COLUMN_NAME_TIMESTAMP, ReadingsContract.SensorEntry.COLUMN_NAME_SENSOR, ReadingsContract.SensorEntry.COLUMN_NAME_VALUE}, // b. column names
                        ReadingsContract.SensorEntry.COLUMN_NAME_SENSOR + " = 'temp'", // c. selections
                        null, // d. selections args
                        null, // e. group by
                        null, // f. having
                        "TIMESTAMP DESC", // g. order by
                        "1"); // h. limit

        if (cursor != null)
            cursor.moveToFirst();

        displayTemperature(cursor.getString(2));
    }

    protected void onStart() {
        super.onStart();

        /*if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            connectGraphs(mBleDeviceAddress);
        } else {
            connect(mBleDeviceAddress);
        }*/

    }

    @Override
    protected void onStop() {
        super.onStop();
        unscheduleUpdaters();
        /*if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            displaySignalStrength(0);
        }
        unscheduleUpdaters();
        mBleDevice.disconnect();*/
    }

    private void connectGraphs(String deviceAddress) {

        if (mBleDevice != null) {
            mBleDevice.disconnect();
        }
        mBleDevice = new BleDevice(this, mDeviceGraphLifecycleCallback, mHandler);

        try {
            mBleDevice.registerServiceClass(SrvWaveformSignal.class);

        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InstantiationException e) {
            throw new AssertionError();
        }

        mBleDevice.connect(deviceAddress);

    }


    private void connect(String deviceAddress) {
        // A device has been chosen from the list. Create an instance of BleDevice,
        // populate it with interesting services and then connect

        scheduleUpdaters();
        displayOnDisconnect();
    }

    private final BleDevice.LifecycleCallback mDeviceGraphLifecycleCallback = new BleDevice.LifecycleCallback() {
        @Override
        public void onBluetoothServicesDiscovered(BleDevice bleDevice) {
            bleDevice.getService(SrvWaveformSignal.class).getAccelerationWaveform().enableNotifications(mAccelerationWaveformListener);
            bleDevice.getService(SrvWaveformSignal.class).getOpticalWaveform().enableNotifications(mOpticalWaveformListener);
        }

        @Override
        public void onBluetoothDeviceDisconnected() {
            unscheduleUpdaters();
            connectGraphs(mBleDeviceAddress);
        }

        @Override
        public void onReadRemoteRssi(int i) {

        }
    };


    private final BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue> mAccelerationWaveformListener = new BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue>() {
        @Override
        public void onValueReady(ChAccelerationWaveform.AccelerationWaveformValue accelerationWaveformValue) {
            if (accelerationWaveformValue != null && accelerationWaveformValue.wave != null && mAccelerationWaveformView != null)
                for (Integer item : accelerationWaveformValue.wave) {
                    mAccelerationWaveformView.addValue(item);
                }

        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue> mOpticalWaveformListener = new BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue>() {
        @Override
        public void onValueReady(ChOpticalWaveform.OpticalWaveformValue opticalWaveformValue) {
            if (opticalWaveformValue != null && opticalWaveformValue.wave != null)
                for (ChOpticalWaveform.OpticalSample item : opticalWaveformValue.wave) {
                    mGreenOpticalWaveformView.addValue(item.green);
                    mBlueOpticalWaveformView.addValue(item.blue);
                }
        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue> mAccelerationEnergyMagnitudeListener =
        new BleCharacteristic.ValueReadyCallback<ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue>() {
            @Override
            public void onValueReady(final ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue accelerationEnergyMagnitudeValue) {
                displayAccelerationEnergyMagnitude(accelerationEnergyMagnitudeValue.value);
            }
        };

    private void displayHeartRate(String bpm) {
        TextView textView = (TextView)findViewById(R.id.textview_heart_rate);
        textView.setText(bpm + " bpm");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);

        View heartView = findViewById(R.id.imageview_heart);
        heartView.startAnimation(effect);
    }

    private void displaySignalStrength(int db) {
        int iconId;
        if (db > -70) {
            iconId = R.drawable.ic_signal_4;
        } else if (db > - 80) {
            iconId = R.drawable.ic_signal_3;
        } else if (db > - 85) {
            iconId = R.drawable.ic_signal_2;
        } else if (db > - 87) {
            iconId = R.drawable.ic_signal_1;
        } else {
            iconId = R.drawable.ic_signal_0;
        }
        ImageView imageView = (ImageView)findViewById(R.id.imageview_signal);
        imageView.setImageResource(iconId);
        TextView textView = (TextView)findViewById(R.id.textview_signal);
        textView.setText(db + "db");
    }

    private void displayBatteryLevel(int percents) {
        int iconId;
        if (percents < 20) {
            iconId = R.drawable.ic_battery_0;
        } else if (percents < 40) {
            iconId = R.drawable.ic_battery_1;
        } else if (percents < 60) {
            iconId = R.drawable.ic_battery_2;
        } else if (percents < 80) {
            iconId = R.drawable.ic_battery_3;
        } else {
            iconId = R.drawable.ic_battery_4;
        }

        ImageView imageView = (ImageView)findViewById(R.id.imageview_battery);
        imageView.setImageResource(iconId);
        TextView textView = (TextView)findViewById(R.id.textview_battery);
        textView.setText(percents + "%");
    }

    private void displayTemperature(String degreesCelsius) {
        TextView textView = (TextView)findViewById(R.id.textview_temperature);
        float c = Float.parseFloat(degreesCelsius);
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        textView.setText(df.format(c) + "\u00b0C");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);
        View thermometerTop = findViewById(R.id.imageview_thermometer_top);
        thermometerTop.startAnimation(effect);
    }

    private void displayStepCount(final int stepCount) {
        TextView textView = (TextView)findViewById(R.id.textview_step_count);
        Assert.assertNotNull(textView);
        textView.setText(stepCount + " steps");

        TranslateAnimation moveDown = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_PARENT, 0.25f);
        moveDown.setDuration(ANIMATION_DURATION);
        moveDown.setRepeatMode(Animation.REVERSE);
        moveDown.setRepeatCount(1);
        View stepLeft = findViewById(R.id.imageview_step_left);
        stepLeft.startAnimation(moveDown);

        TranslateAnimation moveUp = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_PARENT, -0.25f);
        moveUp.setDuration(ANIMATION_DURATION);
        moveUp.setRepeatMode(Animation.REVERSE);
        moveUp.setRepeatCount(1);
        View stepRight = findViewById(R.id.imageview_step_right);
        stepRight.startAnimation(moveUp);
    }

    private void displayAccelerationEnergyMagnitude(final int accelerationEnergyMagnitude) {
        TextView textView = (TextView) findViewById(R.id.textview_acceleration);
        Assert.assertNotNull(textView);
        textView.setText(accelerationEnergyMagnitude + "g");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);

        View imageView = findViewById(R.id.imageview_acceleration);
        imageView.startAnimation(effect);
    }

    private void displayOnDisconnect() {
        displaySignalStrength(-99);
        displayBatteryLevel(0);
    }

    private void scheduleUpdaters() {
        mHandler.post(mPeriodicReader);
    }

    private void unscheduleUpdaters() {
        mHandler.removeCallbacks(mPeriodicReader);
    }

    private static final int RSSI_UPDATE_INTERVAL = 1000; // Milliseconds
    private static final int ANIMATION_DURATION = 500; // Milliseconds

    private int orientation;

    private GraphView mAccelerationWaveformView, mBlueOpticalWaveformView, mGreenOpticalWaveformView;

    private BleDevice mBleDevice;
    private String mBleDeviceAddress;

    private Handler mHandler;
    private Runnable mPeriodicReader;
    private ChAccelerationEnergyMagnitude mChAccelerationEnergyMagnitude = null;
}
