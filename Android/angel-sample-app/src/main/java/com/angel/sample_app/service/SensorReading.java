package com.angel.sample_app.service;

import java.util.GregorianCalendar;

public interface SensorReading {

    void onHeartRateReading(int bpm, int energyExpended, int[] RRIntervals);
    void onSignalStrengthReading(int db);
    void onBatteryLevelReading(int percent);
    void onTemperatureReading(float degreesCelsius, int temperatureUnits, GregorianCalendar timestamp, int temperatureType);
    void onStepCountReading(int stepCount);

    void onSensorDisconnected();
}
