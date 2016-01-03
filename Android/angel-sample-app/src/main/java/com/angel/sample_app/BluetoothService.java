package com.angel.sample_app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.GregorianCalendar;

public class BluetoothService extends Service implements SensorReading {

    private static final String TAG = "ServiceReading";
    private SensorReader mSensor = null;

    public BluetoothService() {
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service starting", Toast.LENGTH_SHORT).show();

        SharedPreferences preferences = getApplicationContext().getSharedPreferences("ble", Context.MODE_PRIVATE);
        final String bleDeviceAddress = preferences.getString("ble", "");

        if (bleDeviceAddress.isEmpty()) {
            Toast.makeText(this, "No device found!", Toast.LENGTH_SHORT).show();
        } else {
            mSensor = new SensorReader(getApplicationContext(), bleDeviceAddress, this);
            mSensor.connect();
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "AngelService killed", Toast.LENGTH_LONG).show();
        mSensor.stop();
    }


    @Override
    public void onHeartRateReading(int bpm, int energyExpended, int[] RRIntervals) {
        Log.d(TAG, "HR: " + bpm + " EnergyExpended: " + energyExpended);
        Toast.makeText(this, "HR: " + bpm, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSignalStrengthReading(int db) {
        Log.d(TAG, "RSSI: " + db);
    }

    @Override
    public void onBatteryLevelReading(int percent) {
        Log.d(TAG, "Battery: " + percent + "%");
        Toast.makeText(this, "Battery: " + percent + "%", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTemperatureReading(float degreesCelsius, int temperatureUnits, GregorianCalendar timestamp, int temperatureType) {
        Log.d(TAG, "Temp: " + degreesCelsius + "c. Timestamp: " + timestamp.toString());
    }

    @Override
    public void onStepCountReading(int stepCount) {
        Log.d(TAG, "Step: " + stepCount);
    }

    @Override
    public void onSensorDisconnected() {
        Log.d(TAG, "Sensor disconnected!");
        Toast.makeText(this, "Sensor lost!", Toast.LENGTH_LONG).show();
    }
}
