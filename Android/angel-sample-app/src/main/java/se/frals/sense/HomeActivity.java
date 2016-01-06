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

package se.frals.sense;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import se.frals.sense.persistence.SenseDatabase;
import se.frals.sense.service.BluetoothService;
import se.frals.sense.util.ShareHelper;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends Activity {

    private SenseDatabase mSenseDatabase;

    private static final int RSSI_UPDATE_INTERVAL = 1000; // Milliseconds
    private static final int ANIMATION_DURATION = 500; // Milliseconds

    private String mBleDeviceAddress;

    private Handler mHandler;
    private Runnable mPeriodicReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);

        SharedPreferences preferences = getApplicationContext().getSharedPreferences("ble", Context.MODE_PRIVATE);
        mBleDeviceAddress = preferences.getString("ble", "");
        if (mBleDeviceAddress.isEmpty()) {
            //throw new RuntimeException("Activity started without a BT device, abort!");
        }

        mHandler = new Handler(this.getMainLooper());

        Intent intent = new Intent(getApplicationContext(), BluetoothService.class);
        startService(intent);

        mSenseDatabase = new SenseDatabase(getApplicationContext());

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

    public void OnShareClicked(View view) {
        try {
            ShareHelper.shareDatabase(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateReadings() {
        getLastTemp();
        getLastBPM();
        getLastBat();
    }

    private void getLastBat() {
        Cursor cursor = mSenseDatabase.getLastBatteryReading();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            displayBatteryLevel(Integer.parseInt(cursor.getString(2)));
        }
    }

    private void getLastBPM() {
        Cursor cursor = mSenseDatabase.getLastHeartrateReading();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = fmt.format(new Date(Long.parseLong(cursor.getString(0)) * 1000));
            displayHeartRate(cursor.getString(2), timestamp);
        }
    }

    private void getLastTemp() {
        Cursor cursor = mSenseDatabase.getLastTemperatureReading();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            displayTemperature(cursor.getString(2));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unscheduleUpdaters();
    }

    private void displayHeartRate(String bpm, String timestamp) {
        TextView textView = (TextView) findViewById(R.id.textview_heart_rate);
        textView.setText(bpm + " bpm");

        TextView timestampView = (TextView) findViewById(R.id.textview_heart_rate_timestamp);
        timestampView.setText(timestamp);

        ScaleAnimation effect = new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
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
        } else if (db > -80) {
            iconId = R.drawable.ic_signal_3;
        } else if (db > -85) {
            iconId = R.drawable.ic_signal_2;
        } else if (db > -87) {
            iconId = R.drawable.ic_signal_1;
        } else {
            iconId = R.drawable.ic_signal_0;
        }
        ImageView imageView = (ImageView) findViewById(R.id.imageview_signal);
        imageView.setImageResource(iconId);
        TextView textView = (TextView) findViewById(R.id.textview_signal);
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

        ImageView imageView = (ImageView) findViewById(R.id.imageview_battery);
        imageView.setImageResource(iconId);
        TextView textView = (TextView) findViewById(R.id.textview_battery);
        textView.setText(percents + "%");
    }

    private void displayTemperature(String degreesCelsius) {
        TextView textView = (TextView) findViewById(R.id.textview_temperature);
        float c = Float.parseFloat(degreesCelsius);
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        textView.setText(df.format(c) + "\u00b0C");

        ScaleAnimation effect = new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);
        View thermometerTop = findViewById(R.id.imageview_thermometer_top);
        thermometerTop.startAnimation(effect);
    }

    private void displayStepCount(final int stepCount) {
        TextView textView = (TextView) findViewById(R.id.textview_step_count);
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
        textView.setText(accelerationEnergyMagnitude + "g");

        ScaleAnimation effect = new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);

        View imageView = findViewById(R.id.imageview_acceleration);
        imageView.startAnimation(effect);
    }

    private void scheduleUpdaters() {
        mHandler.post(mPeriodicReader);
    }

    private void unscheduleUpdaters() {
        mHandler.removeCallbacks(mPeriodicReader);
    }
}
