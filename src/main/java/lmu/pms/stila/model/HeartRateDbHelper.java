package lmu.pms.stila.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lmu.pms.stila.communication.HeartrateSyncJobService;

/**
 * Created by Niki on 01.02.2018.
 */

public class HeartRateDbHelper extends SQLiteOpenHelper {

    public final static String TAG = HeartRateDbHelper.class.getSimpleName();
    // Name of the DB File on disc
    public final static String DATABASE_NAME = "heartrate.db";

    // This has to be incremented with every change in the structure of the DB
    public final static int DATABASE_VERSION = 1;

    private static HeartRateDbHelper instance = null;

    private HeartRateDbHelper(Context context){
       super(context, DATABASE_NAME,null,DATABASE_VERSION);
    }

    public static HeartRateDbHelper getInstance(Context context){
        if(instance == null){
            instance = new HeartRateDbHelper(context);
        }
        return instance;

    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // This uses Integer as Datatype for the Timestamp because the mobile App uses
        // Integer, too.
        String createStatement =  "CREATE TABLE " + HeartRateContract.HeartRateEntry.TABLE_NAME + "("
                + HeartRateContract.HeartRateEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HeartRateContract.HeartRateEntry.COLUMN_TIMESTAMP + " INTEGER,"
                + HeartRateContract.HeartRateEntry.COLUMN_HEARTRATE + " INTEGER, " +
                "CONSTRAINT no_double_times UNIQUE ("+ HeartRateContract.HeartRateEntry.COLUMN_TIMESTAMP +")" +
                ");";

        sqLiteDatabase.execSQL(createStatement);

    }

    /**
     * Drops and recreates the Database when the database is upgraded.
     * The DB is only used for short term caching, so can be dropped without migrating the
     * data.
     * @param sqLiteDatabase reference to the sqliteDatabasew
     * @param i
     * @param i1
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String dropStatement = "DROP TABLE "+ HeartRateContract.HeartRateEntry.TABLE_NAME;
        sqLiteDatabase.execSQL(dropStatement);
        onCreate(sqLiteDatabase);
    }

    public void addEntry(final int rate, final long timestamp){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_TIMESTAMP,timestamp);
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_HEARTRATE,rate);
        long id = db.insert(HeartRateContract.HeartRateEntry.TABLE_NAME,null,cv);
    }

    public void deleteEntriesInTimeFrame(int firstTimestamp, int lastTimestamp){
       SQLiteDatabase hrDB = getWritableDatabase();
        String timestampToString = String.valueOf(lastTimestamp);
        String timestampFromString = String.valueOf(firstTimestamp);
        String [] deleteArgs = {timestampFromString, timestampToString};
        int deleteCount = hrDB.delete(HeartRateContract.HeartRateEntry.TABLE_NAME,"timestamp >= ? AND timestamp <= ?", deleteArgs);
        Log.d(TAG, "onSuccess: Deleted items from DB: "+String.valueOf(deleteCount));
    }

    public HashMap<String, ArrayList<Integer>> getHRData(){
        SQLiteDatabase hrDB = getReadableDatabase();
        Cursor cursor = hrDB.query(HeartRateContract.HeartRateEntry.TABLE_NAME,null,null,null,null,null,HeartRateContract.HeartRateEntry.COLUMN_TIMESTAMP + " asc");


        ArrayList<Integer> timestamps = new ArrayList<Integer>();
        ArrayList<Integer> heartrates = new ArrayList<Integer>();
        String [] ids = new String[cursor.getCount()];
        Log.d(TAG, "syncHeartRateToPhone: Elements in DB to be synced:_"+cursor.getCount());

        cursor.moveToFirst();
        for(int i = 0; i<cursor.getCount(); i++){
            try {
                int currentTimestamp = cursor.getInt(cursor.getColumnIndex(HeartRateContract.HeartRateEntry.COLUMN_TIMESTAMP));
                int currentHeartrate = cursor.getInt(cursor.getColumnIndex(HeartRateContract.HeartRateEntry.COLUMN_HEARTRATE));
                long id = cursor.getLong(cursor.getColumnIndex("_id"));
                timestamps.add(currentTimestamp);
                heartrates.add(currentHeartrate);
                ids[i]=String.valueOf(id);
            }
            catch(Exception e){
                e.printStackTrace();
                // Something went wrong while getting the values from the DB
                // The exceptions thrown are implementation specific, so everything is caught here
                break;
            }
            cursor.moveToNext();
        }

        cursor.close();

        HashMap<String,ArrayList<Integer>> returnMap = new HashMap<>();
        returnMap.put("timestamps",timestamps);
        returnMap.put("heartrates",heartrates);
        return returnMap;

    }


}
