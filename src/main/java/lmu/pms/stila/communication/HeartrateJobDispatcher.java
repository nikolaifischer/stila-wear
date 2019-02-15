package lmu.pms.stila.communication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;

/**
 * Created by Niki on 03.03.2018.
 */

public class HeartrateJobDispatcher {

    private final static String TAG ="HeartrateJobDispatcher";
    private static boolean isInitialized = false;
    private static final String HR_SYNC_TAG = "HR_SYNC";


    synchronized public static void scheduleHeartrateSync(@NonNull final Context context, int minutes){

        int scheduleWindowInSeconds = minutes *  60;
        Log.d(TAG, "scheduleHeartrateSync: "+String.valueOf(scheduleWindowInSeconds));
        if(isInitialized){
            Log.d(TAG, "scheduleHeartrateSync: Job already running!");
            return;
        }
        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);
        Job heartrateSyncJob = dispatcher.newJobBuilder()
                .setService(HeartrateSyncJobService.class)
                .setTag(HR_SYNC_TAG)
                .setTrigger(Trigger.executionWindow(scheduleWindowInSeconds, scheduleWindowInSeconds+10))
                .setReplaceCurrent(false)
                .setRecurring(true)
                .build();
        dispatcher.mustSchedule(heartrateSyncJob);
        isInitialized = true;
        Log.d(TAG, "scheduleHeartrateSync: Initialized the Job");

    }

    public static void cancelHeartrateJobSync(Context context){
        isInitialized = false;
        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);
        dispatcher.cancel(HR_SYNC_TAG);
        Log.d(TAG, "scheduleHeartrateSync: Canceled the Job");
    }
}
