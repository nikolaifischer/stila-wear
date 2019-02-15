package lmu.pms.stila.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.model.RunningActivityDbHelper;
import lmu.pms.stila.R;

/**
 * This class represents the Screen the user sees, when he wants to track a new Stila-Activity.
 */
public class StartNewActivity extends WearableActivity implements MenuItem.OnMenuItemClickListener, View.OnClickListener {

    private static final String TAG = StartNewActivity.class.getSimpleName();
    WearableActionDrawerView mWearableActionDrawer;
    FloatingActionButton mStartFab;
    // Indicates whether there is a Stila Activity running
    private boolean mActivityRunningFlag = false;
    private TextView mStartedAtTextView;
    private String mActivityName = "Activity"; //Default Value in Error Cases
    private long mActivityStartTime;
    private TextView mActivityNameTextView;
    RunningActivityDbHelper.RunningActivityEntry mRunningActivityEntry;
    private Timer mStopwatchTimer;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());

        mContext = this;

        mActivityNameTextView = findViewById(R.id.textview_activity_name);

        mStartFab = findViewById(R.id.fab_start_activity);
        mStartFab.setOnClickListener(this);

        mStartedAtTextView = findViewById(R.id.textview_started_at);

        // Check if there is an activity running
        RunningActivityDbHelper db = new RunningActivityDbHelper(this);
        RunningActivityDbHelper.RunningActivityEntry runningActivityEntry = db.getRunningActivity();
        if(runningActivityEntry != null){
            mActivityRunningFlag = true;
            resumeStilaActivity(runningActivityEntry);
            mRunningActivityEntry = runningActivityEntry;
        }
        else{
            mActivityName = getIntent().getStringExtra(getString(R.string.activity_name_extra));
        }
        mActivityNameTextView.setText(mActivityName);


        mWearableActionDrawer = (WearableActionDrawerView) findViewById(R.id.start_activity_bottom_action_drawer);
        // Peeks action drawer on the bottom.
        mWearableActionDrawer.getController().peekDrawer();
        Menu menu = mWearableActionDrawer.getMenu();
        MenuItem recordEarlierItem = menu.findItem(R.id.start_activity_earlier_menu_option);
        recordEarlierItem.setTitle(getString(R.string.record_earlier)+" "+mActivityName);
        mWearableActionDrawer.setOnMenuItemClickListener(this);

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        //Clean the RunningActivity DB:
        RunningActivityDbHelper db = new RunningActivityDbHelper(this);
        db.deleteRunningActivity();
        // Free Resources
        if(mStopwatchTimer!=null){
            mStopwatchTimer.cancel();
        }

        if(item.getItemId() == R.id.start_activity_cancel_menu_option){
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else{
            Intent intent = new Intent(this, SaveActivity.class);
            intent.putExtra(getString(R.string.activity_name_extra),mActivityName);
            startActivity(intent);
        }

        return true;
    }
    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.fab_start_activity){


            // Change Icon in FAB
            if(!mActivityRunningFlag){
               startStilaActivity();
            }
            else {
                stopStilaActivity();
            }
            mActivityRunningFlag = !mActivityRunningFlag;
        }
    }

    private void startStilaActivity(){
        mStartFab.setImageResource(R.drawable.ic_stop_white);
        mActivityStartTime = System.currentTimeMillis();
        mRunningActivityEntry = new RunningActivityDbHelper.RunningActivityEntry(mActivityStartTime/1000,mActivityName);
        startTimer();
        RunningActivityDbHelper db = new RunningActivityDbHelper(this);
        db.changeRunningActivity(mRunningActivityEntry);

    }

    private void resumeStilaActivity(RunningActivityDbHelper.RunningActivityEntry runningActivity){
        mStartFab.setImageResource(R.drawable.ic_stop_white);
        mActivityName = runningActivity.getActivityName();
        startTimer();

    }

    private void stopStilaActivity(){
        long endTime = System.currentTimeMillis()/1000;
        mStartFab.setImageResource(R.drawable.ic_play_arrow_white_24dp);

        //Clean the RunningActivity DB:
        RunningActivityDbHelper db = new RunningActivityDbHelper(this);
        db.deleteRunningActivity();
        // Free Resources
        if(mStopwatchTimer!=null){
            mStopwatchTimer.cancel();
        }




        // Save the Stila-Activity and start Details Activity
        Intent intent = new Intent(this, SaveActivity.class);
        intent.putExtra(getString(R.string.activity_name_extra),mRunningActivityEntry.getActivityName());
        intent.putExtra(getString(R.string.activity_start_time_extra), mRunningActivityEntry.getStartTime());
        intent.putExtra(getString(R.string.activity_end_time_extra),endTime);
        startActivity(intent);

    }


    /**
     *  This Method manages the Timer Textview to present to the user how long he is already performing an
     *  activity.
     */
    private void startTimer() {
        mStopwatchTimer = new Timer();
        mStopwatchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView timerTextView = (TextView) findViewById(R.id.textview_started_at);
                        timerTextView.setText(stopwatch());
                    }
                });

            }
        }, 0, 1000); // Measures Activity length in Seconds
    }

    /**
     * Returns the combined string for the stopwatch, counting in seconds
     * @return The String representation of the stopwatch
     */
    private String stopwatch() {
        long nowTime = System.currentTimeMillis();
        long cast = nowTime - mRunningActivityEntry.getStartTime()*1000;
        Date date = new Date(cast);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        // Timezone has to be set to UTC, because the time is calculated from Epoch Timestamps
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

}
