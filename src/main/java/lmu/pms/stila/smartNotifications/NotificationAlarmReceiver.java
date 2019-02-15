package lmu.pms.stila.smartNotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lmu.pms.stila.Database.ActivityDatabase;
import lmu.pms.stila.Database.ActivityDatabaseHandler;
import lmu.pms.stila.Database.ActivityTypeDatabaseHandler;
import lmu.pms.stila.Database.HRVDatabase;
import lmu.pms.stila.Database.HRVDatabaseHandler;
import lmu.pms.stila.R;
import lmu.pms.stila.Utils.TimeConversionUtil;
import lmu.pms.stila.model.RunningActivityDbHelper;
import lmu.pms.stila.provider.FixedBoundsStressedIndicator;
import lmu.pms.stila.provider.StressedIndicatorAlgorithm;

/**
 * This class is a Alarm Receiver which gets called when the NotificationScheduler triggers.
 * It checks if a new notification should be sent and sends it to the watch if all conditions return true.
 *
 * Bugfix: This Alarm Receiver also gets called manually from the DataLayerListener. Alarms sometimes
 * do not get fired on Android and are thus not trusted.
 */
public class NotificationAlarmReceiver extends BroadcastReceiver {

    public static final String TAG = NotificationAlarmReceiver.class.getSimpleName();
    long TWENTYFIVE_MINUTES =1500;
    long FIFTY_MINUTES = TWENTYFIVE_MINUTES *2;
    long TEN_MINUTES =600;
    long TWENTY_MINUTES = 2*TEN_MINUTES;
    long FORTY_MINUTES = 4*TEN_MINUTES;

    private NotificationHelperWatch mNotificationHelper;
    private Context mContext;
    public NotificationAlarmReceiver(Context context){
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationScheduler.SCHEDULED_NOTIFICATION_BROADCAST);
        context.registerReceiver(this,filter);
        mNotificationHelper = new NotificationHelperWatch(context);
    }
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive: Notification Alarm Broadcast");

        boolean sent = checkNotificationSend();
        if(!sent){
            checkBackupNotification();
        }

    }

    /**
     * Checks if the necessary situation to send a notification is given. If yes a Notification
     * is send to the user, if no none is send.
     * @return True if a Notification was sent, else false.
     */
    public boolean checkNotificationSend(){

        StressedIndicatorAlgorithm stressedIndicator = new FixedBoundsStressedIndicator(mContext);
        stressedIndicator.updateData();
        long now = System.currentTimeMillis()/1000;

        long lastNotificationTimestamp = getLastNotificationTimestamp();
        // CONDITION 0: Last Notification is older than 1 Hour:
        if(now-lastNotificationTimestamp<3600){
            Log.d(TAG, "checkNotificationSend: Failed Condition 0");
            return false; // Condition fails => early return.
        }
        // CONDITION 1: Only send Notifications between 9:00 and 21:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now*1000);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if(hour < 9 || hour>=21){
            Log.d(TAG, "checkNotificationSend: Failed Condition 1");
            return false;
        }

        // CONDITION 2: No Activities were tracked in the last 2 hours by the user:
        ActivityDatabaseHandler activityDatabaseHandler = new ActivityDatabaseHandler(mContext);
        ArrayList<ActivityDatabase> activities = activityDatabaseHandler.getAllEntriesInTimeframe(now-7200,now);
        if(activities.size()>0){
            Log.d(TAG, "checkNotificationSend: Failed Condition 2");
            return false;
        }

        // CONDITION 2.5: No Activities are running now
        RunningActivityDbHelper runningActivityDbHelper = new RunningActivityDbHelper(mContext);
        if(runningActivityDbHelper.getRunningActivity()!=null){
            Log.d(TAG, "checkNotificationSend: Failed Condition 2.5");
            return false;
        }

        // CONDITION 3: Last 10 Minutes were neutral or relaxed

        HRVDatabaseHandler hrvDatabaseHandler = new HRVDatabaseHandler(mContext);
        HRVDatabase lastEntry = hrvDatabaseHandler.getLastEntry();
        if(lastEntry == null){
            return false;
        }
        if(lastEntry.getComputedStress() > stressedIndicator.getStressedBound()){
            Log.d(TAG, "checkNotificationSend: Failed Condition 3");
            return false; // Condition failed => Early return
        }

        // CONDITION 4: At least 20 minutes before relax phase were stressing:
        // Leaves 5 Minutes room
        long endofStressedPhase = now-TWENTY_MINUTES; // or start of relaxed phase
        ArrayList<HRVDatabase>earlierHRVs =  hrvDatabaseHandler.getEntriesInTimeRange(now-FORTY_MINUTES,endofStressedPhase, null);
        if(earlierHRVs.size() < 1){
            return false;
        }
        for(HRVDatabase entry: earlierHRVs){
            if(entry.getComputedStress() < stressedIndicator.getStressedBound()){
                Log.d(TAG, "checkNotificationSend: Failed Condition 4");
                return false; // Condition failed => Early return
            }
            endofStressedPhase = entry.getTimestamp()+600; // 10 minutes after the last timestamp
        }

        long startOfStressedPhase= identifyStartOfStressedPhase(endofStressedPhase);
        mNotificationHelper.sendTrackingNotification(startOfStressedPhase,endofStressedPhase);
        setLastNotificationTimestamp(now);
        return true;
    }

    /**
     * This method checks if there was a long period of time where the user was not triggered at all.
     * Under these conditions, a simple notification is sent, reminding the user to track her
     * activities.
     */
    public void checkBackupNotification(){
        long now = System.currentTimeMillis()/1000;
        long lastNotification = getLastNotificationTimestamp();

        // Between 9:00 and 21:00?
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now*1000);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if(hour < 9 || hour>=21){
            Log.d(TAG, "checkBackupNotificattion: Failed Condition 1");
            return;
        }

        // Last notification older than 3 hours?
        if(now - lastNotification < 10800) { // 3 hours in seconds
            Log.d(TAG, "checkBackupNotificattion: Failed Condition 2");
            return;
        }

        HRVDatabaseHandler hrvDatabaseHandler = new HRVDatabaseHandler(mContext);
        HRVDatabase lastHRVMeasurement = hrvDatabaseHandler.getLastEntry();
        if(lastHRVMeasurement == null){
            return;
        }
        // HRV Measurements are active?
        if(now - lastHRVMeasurement.getTimestamp()> 3600) {
            Log.d(TAG, "checkBackupNotificattion: Failed Condition 3");
            return;
        }
        // Has HRV Measurements been running for some time?
        List list = hrvDatabaseHandler.getEntriesUpFromTimestamp(TimeConversionUtil.getDayBeginTimestampInSecsOf(now)).get(1);
        if(list == null || list.size()<10) {
            Log.d(TAG, "checkBackupNotificattion: Failed Condition 4");
            return;
        }

        mNotificationHelper.sendSimpleNotification();
        setLastNotificationTimestamp(now);

    }



    /**
     * Gets the timestamp of the last notification
     * @return the timestamp of the last notification or -1 if the app never sent a notification
     */
    public long getLastNotificationTimestamp(){
        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.stila_watchface_preferences),Context.MODE_PRIVATE);
        long last = prefs.getLong(mContext.getString(R.string.preference_last_notification),-1);
        return last;
    }

    /**
     * Saves the timestamp of the last notification in the preference file
     * @param now the timestamp of the last notification in seconds.
     */
    public void setLastNotificationTimestamp(long now){
        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.stila_watchface_preferences),Context.MODE_PRIVATE);
        prefs.edit().putLong(mContext.getString(R.string.preference_last_notification),now).apply();
    }

    /**
     * Helper Method to find the start timestamp of a stressed time window.
     * Checks the last 100 Minutes for the start of the stressed phase.
     * @param endTimestamp The endtimestamp of the stressed phase
     * @return the timestamp for the start of the stressedphase or a timestamp 100 minutes before
     * the endtimestamp if the stressed phase is longer than 100 minutes.
     */
    private long identifyStartOfStressedPhase(long endTimestamp){
        StressedIndicatorAlgorithm stressedIndicator = new FixedBoundsStressedIndicator(mContext);
        stressedIndicator.updateData();
        HRVDatabaseHandler hrvDatabaseHandler = new HRVDatabaseHandler(mContext);
        // check the last 100 Minutes (arbitrary) for the start of the stressed phase
        ArrayList<HRVDatabase> entries = hrvDatabaseHandler.getEntriesInTimeRange(endTimestamp-FIFTY_MINUTES * 2,endTimestamp, "desc");

        for(int i=0; i<entries.size();i++){
            HRVDatabase entry = entries.get(i);
            if(entry.getComputedStress()<stressedIndicator.getStressedBound() && i>0){
                return  entries.get(i-1).getTimestamp();
            }
        }
        return entries.get(entries.size()-1).getTimestamp();
    }
}
