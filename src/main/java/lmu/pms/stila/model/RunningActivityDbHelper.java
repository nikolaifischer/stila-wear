package lmu.pms.stila.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RunningActivityDbHelper extends SQLiteOpenHelper {

    private final static String TAG = RunningActivityEntry.class.getSimpleName();
    // Name of the DB File on disc
    public final static String DATABASE_NAME = "running_activity.db";

    // This has to be incremented with every change in the structure of the DB
    public final static int DATABASE_VERSION = 1;
    public final static String TABLE_NAME= "runningActivitiesTable";
    public final static String COLUMN_START_TIME= "startTime";
    public final static String COLUMN_ACTIVITY_NAME= "name";
    private Context mContext;

    public RunningActivityDbHelper(Context context){
        super(context, DATABASE_NAME,null,DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createStatement =  "CREATE TABLE " +TABLE_NAME+ "("
                + "_id" + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ACTIVITY_NAME + " TEXT,"
                + COLUMN_START_TIME+ " INTEGER " +
                ");";

        db.execSQL(createStatement);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Persists the activty the user is currently running to the DB.
     * @param runningActivityEntry - The activity to be persisted (containing startTime and
     *                             name)
     */
    public void changeRunningActivity(RunningActivityEntry runningActivityEntry){

        // Delete all records => There can only be one active Activity for a user
        getWritableDatabase().delete(TABLE_NAME, null,null);
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ACTIVITY_NAME,runningActivityEntry.activityName);
        cv.put(COLUMN_START_TIME,runningActivityEntry.startTime);
        this.getWritableDatabase().insert(TABLE_NAME,null,cv);

    }

    /**
     * Deletes the currently tracked Activity
     */
    public void deleteRunningActivity(){
        getWritableDatabase().delete(TABLE_NAME, null,null);
    }


    /**
     * Gets the currently running Stila Activity from the DB
     * @return the running Activity or null if there is none.
     */
    public RunningActivityEntry getRunningActivity(){

        Cursor cursor;
        cursor = getReadableDatabase().query(TABLE_NAME,null,null,null,null,null,"startTime DESC");

        if(cursor.getCount()==1){
            cursor.moveToFirst();
           RunningActivityEntry entry = new RunningActivityEntry(cursor.getLong(cursor.getColumnIndex(COLUMN_START_TIME)), cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_NAME)));
           cursor.close();
           return entry;
        }
        else if(cursor.getCount() == 0){
            cursor.close();
            return null;
        }
        else{
            Log.e(TAG, "getRunningActivity: INCONSISTENT DB STATE: DB CONTAINS MORE THAN ONE ACTIVE ACTIVITY");
            cursor.close();
            return null;
        }

    }

    /**
     * This class represents an entry in the running Activity Database.
     */
    public static class RunningActivityEntry{
        private long startTime;
        private String activityName;

        public RunningActivityEntry(long startTime, String activityName) {
            this.startTime = startTime;
            this.activityName = activityName;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }


        public String getActivityName() {
            return activityName;
        }

        public void setActivityName(String activityName) {
            this.activityName = activityName;
        }


    }
}
