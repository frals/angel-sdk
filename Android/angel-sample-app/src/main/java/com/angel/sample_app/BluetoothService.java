package com.angel.sample_app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.angel.sample_app.persistence.ReadingsContract.SensorEntry;
import com.angel.sample_app.persistence.SenseDatabase;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class BluetoothService extends Service implements SensorReading {

    private static final String TAG = "ServiceReading";
    private SensorReader mSensor = null;
    SenseDatabase mSenseDatabase = null;

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
            mSenseDatabase = new SenseDatabase(getApplicationContext());
            mSensor = new SensorReader(getApplicationContext(), bleDeviceAddress, this);
            mSensor.connect();

            Notification notification = new Notification(R.drawable.ic_top_logo, "Angel Sensor connected",
                    System.currentTimeMillis());
            Intent notificationIntent = new Intent(this, HomeActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notification.setLatestEventInfo(this, "Angel Sensor",
                    "Connected", pendingIntent);
            startForeground(123, notification);
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
        //Log.d(TAG, "HR: " + bpm + " EnergyExpended: " + energyExpended);
        //Toast.makeText(this, "HR: " + bpm, Toast.LENGTH_LONG).show();
        SQLiteDatabase db = mSenseDatabase.getWritableDatabase();

        long unixTime = System.currentTimeMillis() / 1000L;
        ContentValues values = new ContentValues();
        values.put(SensorEntry.COLUMN_NAME_SENSOR, "hr");
        values.put(SensorEntry.COLUMN_NAME_TIMESTAMP, unixTime);
        values.put(SensorEntry.COLUMN_NAME_VALUE, bpm);

        db.insert(SensorEntry.TABLE_NAME, "null", values);
    }

    @Override
    public void onSignalStrengthReading(int db) {
        Log.d(TAG, "RSSI: " + db);
    }

    @Override
    public void onBatteryLevelReading(int percent) {
        //Log.d(TAG, "Battery: " + percent + "%");
        //Toast.makeText(this, "Battery: " + percent + "%", Toast.LENGTH_LONG).show();

        long unixTime = System.currentTimeMillis() / 1000L;
        ContentValues values = new ContentValues();
        values.put(SensorEntry.COLUMN_NAME_SENSOR, "battery");
        values.put(SensorEntry.COLUMN_NAME_TIMESTAMP, unixTime);
        values.put(SensorEntry.COLUMN_NAME_VALUE, percent);

        SQLiteDatabase db = mSenseDatabase.getWritableDatabase();
        db.insert(SensorEntry.TABLE_NAME, "null", values);
    }

    @Override
    public void onTemperatureReading(float degreesCelsius, int temperatureUnits, GregorianCalendar timestamp, int temperatureType) {
        //Log.d(TAG, "Temp: " + degreesCelsius + "c. Timestamp: " + format(timestamp));

        long unixTime = System.currentTimeMillis() / 1000L;
        ContentValues values = new ContentValues();
        values.put(SensorEntry.COLUMN_NAME_SENSOR, "temp");
        values.put(SensorEntry.COLUMN_NAME_TIMESTAMP, unixTime);
        values.put(SensorEntry.COLUMN_NAME_VALUE, degreesCelsius);

        SQLiteDatabase db = mSenseDatabase.getWritableDatabase();
        db.insert(SensorEntry.TABLE_NAME, "null", values);
    }

    @Override
    public void onStepCountReading(int stepCount) {
        //Log.d(TAG, "Step: " + stepCount);
    }

    @Override
    public void onSensorDisconnected() {
        Log.d(TAG, "Sensor disconnected!");
        Toast.makeText(this, "Sensor lost!", Toast.LENGTH_LONG).show();
        stopForeground(true);
        stopSelf();
    }

    public static String format(GregorianCalendar calendar) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fmt.setCalendar(calendar);
        return fmt.format(calendar.getTime());
    }
}
