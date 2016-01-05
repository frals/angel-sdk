package com.angel.sample_app.persistence;

import android.provider.BaseColumns;

public class ReadingsContract {
    public ReadingsContract() {
    }

    /* Inner class that defines the table contents */
    public static abstract class SensorEntry implements BaseColumns {
        public static final String TABLE_NAME = "readings";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SENSOR = "sensor";
        public static final String COLUMN_NAME_VALUE = "value";
    }
}
