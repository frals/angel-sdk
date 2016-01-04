package com.angel.sample_app.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by frals on 2016-01-03.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "sensorreadings.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ReadingsContract.SensorEntry.TABLE_NAME + " (" +
                    ReadingsContract.SensorEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY, " +
                    ReadingsContract.SensorEntry.COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
                    ReadingsContract.SensorEntry.COLUMN_NAME_SENSOR + TEXT_TYPE + COMMA_SEP +
                    ReadingsContract.SensorEntry.COLUMN_NAME_VALUE + TEXT_TYPE +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ReadingsContract.SensorEntry.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        //db.execSQL(SQL_DELETE_ENTRIES);
        //onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
