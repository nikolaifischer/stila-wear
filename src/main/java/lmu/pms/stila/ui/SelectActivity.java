package lmu.pms.stila.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.view.View;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

import lmu.pms.stila.Database.ActivityTypeDatabase;
import lmu.pms.stila.Database.ActivityTypeDatabaseHandler;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.config.RecyclerItemClickListener;
import lmu.pms.stila.R;
import lmu.pms.stila.SysConstants.Constants;
import lmu.pms.stila.smartNotifications.NotificationHelperWatch;

public class SelectActivity extends WearableActivity {

    private WearableRecyclerView mRecyclerView;
    /**
     * Flag to show if this activity was started from the smart notification
     */
    private boolean mNotificationFlag=false;
    private long notificationStartTime;
    private long notificationEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras!=null){
            boolean b = extras.getBoolean("notification");
            if(b){
                mNotificationFlag = true;
                notificationStartTime = extras.getLong("startTime");
                notificationEndTime = extras.getLong ("endTime");
                // User came here from a Notification => Successful Trigger
                AnalyticsHelper.getInstance().trackSuccessfulTrigger(AnalyticsHelper.DeviceType.WATCH);
                // Dismiss Notification
                NotificationManagerCompat notificationManagerCompat =
                        NotificationManagerCompat.from(getApplicationContext());
                notificationManagerCompat.cancel(NotificationHelperWatch.NOTIFICATION_ID);
            }
        }
        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());
        setContentView(R.layout.activity_select);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.select_recycler_view);
        mRecyclerView.setEdgeItemsCenteringEnabled(false);

        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(this));
        ActivityTypeDatabaseHandler activityTypeDatabaseHandler = new ActivityTypeDatabaseHandler(this);
        ArrayList<ActivityTypeDatabase> activityTypes = activityTypeDatabaseHandler.getAll();
        final SelectAdapter adapter = new SelectAdapter(this, activityTypes);

        mRecyclerView.setAdapter(adapter);

        /**
         * On Click Listener for the List
         */
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, mRecyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {

                        String activityName = adapter.getActivityName(position-1);
                        startStilaActivity(activityName);

                    }

                    @Override public void onLongItemClick(View view, int position) {
                    }
                })
        );

    }

    /**
     * Starts a Stila-Activity with the current Time as startTimestamp. The running Activity
     * is then persisted in the DB in order to persist the activity between reboots etc.
     * @param activityName
     */
    public void startStilaActivity(String activityName){

        // If called from a notification, the user gets redirected right to the save
        // screen.
        if(mNotificationFlag){
            Intent intent = new Intent(this,SaveActivity.class);
            intent.putExtra(getString(R.string.activity_start_time_extra),notificationStartTime);
            intent.putExtra(getString(R.string.activity_end_time_extra),notificationEndTime);
            intent.putExtra(getString(R.string.activity_name_extra), activityName);
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(this, StartNewActivity.class);
        intent.putExtra(getString(R.string.activity_name_extra), activityName);
        startActivity(intent);

    }
}
