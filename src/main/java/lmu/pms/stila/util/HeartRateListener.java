package lmu.pms.stila.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import lmu.pms.stila.R;
import lmu.pms.stila.watchface.StilaWatchFace;
import lmu.pms.stila.communication.HeartrateSyncJobService;
import lmu.pms.stila.model.HeartRateContract;
import lmu.pms.stila.model.HeartRateDbHelper;
import lmu.pms.stila.watchface.StilaDigitalWatchFaceService;

/**
 * Listener for Changes in the Heart Rate
 * Used by Stila Watchfaces
 */

public class HeartRateListener implements SensorEventListener {

    private static String TAG = "HeartRateListener";
    private Context mContext;
    private StilaWatchFace mWatchFaceService;
    private SharedPreferences prefs;

    /**
     * Flags whether the HR measurements should be send to the phone instantly instead
     * of cached.
     */
    private boolean syncInstant;

    /** Counter for retrying measurements*/
    private int unSucessfullMeasuresCounter = 0;

    public HeartRateListener( StilaWatchFace watchFaceService, Context context){
        mContext = context;
        mWatchFaceService = watchFaceService;
        prefs = mContext.getSharedPreferences(mContext.getString(R.string.stila_watchface_preferences),Context.MODE_PRIVATE);
        int synchMinutes = prefs.getInt(context.getString(R.string.preference_sync_interval),0);
        if(synchMinutes < 1){
            syncInstant = true;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        mWatchFaceService.setHRListenerRegistered(false);
        int measureInterval = prefs.getInt(mContext.getString(R.string.preference_heartrate_interval),mContext.getResources().getInteger(R.integer.measure_periodically));
        // Unregister this Listener to let the sensor sleep for the measurement interval
        // Do not register if the hr measurement frequency is set to continuous.
        if( !mWatchFaceService.getHRListenerRegistered() && measureInterval != mContext.getResources().getInteger(R.integer.measure_continuous)){
            Log.d(TAG, "onSensorChanged: Unregistering Listener");
            mWatchFaceService.getSensorManager().unregisterListener(this);
        }
        long eventTime = new Date().getTime()/1000; // Converting milliseconds to seconds
        SimpleDateFormat simpleDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        Date date = new Date();
        String readableDate = simpleDate.format(date);
        
        if(sensorEvent.accuracy< SensorManager.SENSOR_STATUS_ACCURACY_LOW || sensorEvent.values.length<1) {
            // Sensor Accuracy is very low or Sensor has no Skin Contact - Discard
            Log.d(TAG, "Discarded Sensor reading - Accuracy too low!");


            // Try to measure the Heart Rate 2 times (usually once a second) - If there
            // is still not enough accuracy give up to safe battery life.

            if(unSucessfullMeasuresCounter>2){
                Log.d(TAG, "Accuracy too low - Giving up");
                if(!mWatchFaceService.getHRListenerRegistered() && measureInterval != mContext.getResources().getInteger(R.integer.measure_continuous)){
                    Log.d(TAG, "onSensorChanged: Unregistering Listener");
                    mWatchFaceService.getSensorManager().unregisterListener(this);
                }
                unSucessfullMeasuresCounter = 0;
            }
            unSucessfullMeasuresCounter ++;

        }
        else {

            Log.d(TAG, "onSensorChanged:  "+ sensorEvent.values[0] + " Date: "+ readableDate+ " Timestamp "+ eventTime);
            int rateInteger = Math.round(sensorEvent.values[0]);

           saveToDb(rateInteger, eventTime);
            //Log.d(TAG, "onSensorChanged: Writing to DB successful? "+successful);

        }


    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    /**
     * Inserts a measurement in the DB
     * @param rate The heartrate
     * @param timestamp the time the measurement was taken
     */
    @SuppressLint("StaticFieldLeak")
    public void saveToDb(final int rate, final long timestamp){

        HeartRateDbHelper dbHelper = HeartRateDbHelper.getInstance(mContext);
        dbHelper.addEntry(rate,timestamp);
        if(syncInstant){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HeartrateSyncJobService s = new HeartrateSyncJobService();
                    s.syncHeartRateToPhone(mContext);
                    Log.d(TAG, "saveToDb: Syncing Instantly");
                }
            }).start();

        }

    }


    public void setSyncInstant(boolean flag){
        Log.d(TAG, "setSyncInstant: Instant Synchronisation? "+flag);
        syncInstant = flag;
    }


}
