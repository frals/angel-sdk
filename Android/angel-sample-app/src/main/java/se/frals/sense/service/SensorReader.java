package se.frals.sense.service;

import android.content.Context;

import com.angel.sdk.BleCharacteristic;
import com.angel.sdk.BleDevice;
import com.angel.sdk.ChBatteryLevel;
import com.angel.sdk.ChHeartRateMeasurement;
import com.angel.sdk.ChStepCount;
import com.angel.sdk.ChTemperatureMeasurement;
import com.angel.sdk.SrvActivityMonitoring;
import com.angel.sdk.SrvBattery;
import com.angel.sdk.SrvHealthThermometer;
import com.angel.sdk.SrvHeartRate;

public class SensorReader {

    private Context mContext;
    private String mBleDeviceAddress;
    private SensorReading mCallback;

    public SensorReader(Context context, String bleAddress, SensorReading callback) {
        if (bleAddress.isEmpty()) {
            throw new RuntimeException("No BT address!");
        }
        mBleDeviceAddress = bleAddress;
        mContext = context;
        mCallback = callback;
    }

    public void connect() {
        connect(mBleDeviceAddress);
    }

    public void stop() {
        mBleDevice.disconnect();
    }

    private BleDevice mBleDevice;

    private void connect(String deviceAddress) {
        if (mBleDevice != null) {
            mBleDevice.disconnect();
        }

        DeviceLifecycleCallback cb = new DeviceLifecycleCallback(mCallback);
        mBleDevice = new BleDevice(mContext, cb);

        try {
            mBleDevice.registerServiceClass(SrvHeartRate.class);
            mBleDevice.registerServiceClass(SrvHealthThermometer.class);
            mBleDevice.registerServiceClass(SrvBattery.class);
            mBleDevice.registerServiceClass(SrvActivityMonitoring.class);

        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InstantiationException e) {
            throw new AssertionError();
        }

        mBleDevice.connect(deviceAddress);
    }

    private class DeviceLifecycleCallback implements BleDevice.LifecycleCallback {

        private SensorReading mCallback;

        public DeviceLifecycleCallback(SensorReading callback) {
            mCallback = callback;
        }

        @Override
        public void onBluetoothServicesDiscovered(BleDevice device) {
            device.getService(SrvHeartRate.class).getHeartRateMeasurement().enableNotifications(new BleCharacteristic.ValueReadyCallback<ChHeartRateMeasurement.HeartRateMeasurementValue>() {
                @Override
                public void onValueReady(final ChHeartRateMeasurement.HeartRateMeasurementValue hrMeasurement) {
                    mCallback.onHeartRateReading(hrMeasurement.getHeartRateMeasurement(), hrMeasurement.getEnergyExpended(), hrMeasurement.getRRIntervals());
                }
            });
            device.getService(SrvHealthThermometer.class).getTemperatureMeasurement().enableNotifications(new BleCharacteristic.ValueReadyCallback<ChTemperatureMeasurement.TemperatureMeasurementValue>() {
                @Override
                public void onValueReady(final ChTemperatureMeasurement.TemperatureMeasurementValue temp) {
                    mCallback.onTemperatureReading(temp.getTemperatureMeasurement(), temp.getTemperatureUnits(), temp.getTimeStamp(), temp.getTemperatureType());
                }
            });
            device.getService(SrvBattery.class).getBatteryLevel().enableNotifications(new BleCharacteristic.ValueReadyCallback<ChBatteryLevel.BatteryLevelValue>() {
                @Override
                public void onValueReady(final ChBatteryLevel.BatteryLevelValue batteryLevel) {
                    mCallback.onBatteryLevelReading(batteryLevel.value);
                }
            });
            device.getService(SrvActivityMonitoring.class).getStepCount().enableNotifications(new BleCharacteristic.ValueReadyCallback<ChStepCount.StepCountValue>() {
                @Override
                public void onValueReady(final ChStepCount.StepCountValue stepCountValue) {
                    mCallback.onStepCountReading(stepCountValue.value);
                }
            });

            //mChAccelerationEnergyMagnitude = device.getService(SrvActivityMonitoring.class).getChAccelerationEnergyMagnitude();
            //ChAlarmClockControlPoint clockControlPoint = device.getService(SrvAlarmClock.class).getControlPointCharacteristic();
            // TOOD: once angel-sdk supports it, set correct device time
        }


        @Override
        public void onBluetoothDeviceDisconnected() {
            mCallback.onSensorDisconnected();
        }

        @Override
        public void onReadRemoteRssi(final int rssi) {
            mCallback.onSignalStrengthReading(rssi);
        }
    }
}
