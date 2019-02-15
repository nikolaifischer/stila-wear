package lmu.pms.stila.util;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import lmu.pms.stila.watchface.StilaWatchFace;

import static android.content.Context.POWER_SERVICE;

public class HeartRateScheduler implements Runnable {

    public static final String TAG = HeartRateScheduler.class.getSimpleName();
    private int mInterval = -2;
    private StilaWatchFace mWatchface;
    private Context mContext;

    private boolean runningFlag = true;

    public HeartRateScheduler(StilaWatchFace callingWatchface, Context context){

        mWatchface = callingWatchface;
        mContext = context;
    }


    public void setInterval(int interval){
        mInterval = interval;
    }

    public void setStartFlag(){
        runningFlag = true;
    }
    public void setStopFlag(){
        runningFlag = false;
    }

    @Override
    public void run() {
        PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "HeartRateScheduler Wake Lock");
        wakeLock.acquire();
        while(runningFlag){
            Log.d(TAG, "run: awake");
            mWatchface.registerHRReceiver();
            try {
                Log.d(TAG, "run: Sleeping now millis: "+(mInterval)*1000);
                Thread.sleep((mInterval)*1000); // 2 Seconds buffer to allow measurement.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        wakeLock.release();


    }
}
