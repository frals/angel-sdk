package se.frals.sense.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import se.frals.sense.persistence.ReadingsContract.SensorEntry;

public class SenseDatabase extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "sensorreadings.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SensorEntry.TABLE_NAME + " (" +
                    SensorEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY, " +
                    SensorEntry.COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
                    SensorEntry.COLUMN_NAME_SENSOR + TEXT_TYPE + COMMA_SEP +
                    SensorEntry.COLUMN_NAME_VALUE + TEXT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SensorEntry.TABLE_NAME;

    private final String[] DEFAULT_SELECT_COLUMNS = new String[]{SensorEntry.COLUMN_NAME_TIMESTAMP, SensorEntry.COLUMN_NAME_SENSOR, SensorEntry.COLUMN_NAME_VALUE};

    public SenseDatabase(Context context) {
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

    public Cursor getLastBatteryReading() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor =
                db.query(SensorEntry.TABLE_NAME,
                        DEFAULT_SELECT_COLUMNS,
                        SensorEntry.COLUMN_NAME_SENSOR + " = 'battery'", 
                        null, // selections args
                        null, // group by
                        null, // having
                        "TIMESTAMP DESC", // order by
                        "1"); // limit
        return cursor;
    }

    public Cursor getLastHeartrateReading() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor =
                db.query(SensorEntry.TABLE_NAME,
                        DEFAULT_SELECT_COLUMNS,
                        SensorEntry.COLUMN_NAME_SENSOR + " = 'hr'", 
                        null, // selections args
                        null, // group by
                        null, // having
                        "TIMESTAMP DESC", // order by
                        "1"); // limit
        return cursor;
    }

    public Cursor getLastTemperatureReading() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor =
                db.query(SensorEntry.TABLE_NAME,
                        DEFAULT_SELECT_COLUMNS,
                        SensorEntry.COLUMN_NAME_SENSOR + " = 'temp'", 
                        null, // selections args
                        null, // group by
                        null, // having
                        "TIMESTAMP DESC", // order by
                        "1"); // limit
        return cursor;
    }

}
