package lmu.pms.stila.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.Calendar;
import lmu.pms.stila.Database.ActivityDatabase;
import lmu.pms.stila.Database.ActivityDatabaseHandler;
import lmu.pms.stila.Database.ActivityTypeDatabase;
import lmu.pms.stila.Database.ActivityTypeDatabaseHandler;
import lmu.pms.stila.SysConstants.ActivityEntityConstants;
import lmu.pms.stila.SysConstants.Constants;
import lmu.pms.stila.Utils.TimeConversionUtil;
import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;

/**
 * This class represents the Activity a User sees, when he wants to save a Stila-Activity after
 * completing it or if "record earlier activity" was clicked.
 * It's main purpose is the gathering of the information that is needed to save the Stila-Activity
 * to the DB.
 */
public class SaveActivity extends WearableActivity {


    private final static String TAG = SaveActivity.class.getSimpleName();
    private Context mContext;

    // Stila-Activity Info from Intent
    private String mActivityName;
    private long mActivityStartTime;
    private long mActivityEndTime;

    // UI Elements;

    private Button mTimeRangeTextViewFrom;
    private Button mTimeRangeTextViewTo;
    private TextView mActivityNameTextView;
    private SeekBar mStressSeekBar;
    private SeekBar mPerformanceSeekBar;
    private SeekBar mValenceSeekbar;
    private ImageButton mSaveActivityButton;
    private String mErrorMsg = "Start Time can't be set after End Time";

    // Info gathered in this UI
    private int mStartHour = 0;
    private int mStartMin = 0;
    private int mEndHour = 0;
    private int mEndMin = 0;
    private String mMood= Constants.MOOD_NEUTRAL; //"NEUTRAL";
    private int mStressLevel = 0;
    private int mPerformanceLevel = 0;
    private int mValenceLevel = 0;
    private String mPosture=ActivityEntityConstants.SITTING;
    private String mDrugs = ActivityEntityConstants.NONE;
    private String mExperience = ActivityEntityConstants.NEUTRAL;

    private final int REQUEST_CODE_TIME_PICKER_START = 111;
    private final int REQUEST_CODE_TIME_PICKER_END = 222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());
        mContext = this;
        setContentView(R.layout.activity_save);

        mActivityName = getIntent().getStringExtra(getString(R.string.activity_name_extra));
        mActivityNameTextView = findViewById(R.id.textview_activity_name);
        mActivityNameTextView.setText(mActivityName);

        long endTimeDefault = System.currentTimeMillis()/1000; //  now
        long startTimeDefault = System.currentTimeMillis()/1000 -3600; //  now - 1h
        mActivityStartTime = getIntent().getLongExtra(getString(R.string.activity_start_time_extra),startTimeDefault);
        mActivityEndTime = getIntent().getLongExtra(getString(R.string.activity_end_time_extra),endTimeDefault);
        mTimeRangeTextViewFrom = findViewById(R.id.text_view_time_range_from);
        mTimeRangeTextViewTo = findViewById(R.id.text_view_time_range_to);
        mTimeRangeTextViewFrom.setText(TimeConversionUtil.utcTimestampToLongDaytimeStr(mActivityStartTime));
        mTimeRangeTextViewTo.setText(TimeConversionUtil.utcTimestampToLongDaytimeStr(mActivityEndTime));

        setTimePickerDefaultState();


        /**
         * Time Range Button (From)
         */
        mTimeRangeTextViewFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent timePickerIntent = new Intent(mContext,WearTimePicker.class);
                timePickerIntent.putExtra("hour",mStartHour);
                timePickerIntent.putExtra("minute",mStartMin);
                startActivityForResult(timePickerIntent,REQUEST_CODE_TIME_PICKER_START);

            }

        });



        /**
         * Time Range Button (To)
         */
        mTimeRangeTextViewTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent timePickerIntent = new Intent(mContext,WearTimePicker.class);
                timePickerIntent.putExtra("hour",mEndHour);
                timePickerIntent.putExtra("minute",mEndMin);
                startActivityForResult(timePickerIntent,REQUEST_CODE_TIME_PICKER_END);
            }
        });

        /**
         * Stress Seekbar
         */
        mStressSeekBar = findViewById(R.id.stressLevelSeekBar);
        mStressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStressLevel = progress-2;

            }
        });

        /**
         * Performance Seekbar
         */
        mPerformanceSeekBar = findViewById(R.id.performanceSeekBar);
        mPerformanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPerformanceLevel = progress-2;
            }
        });

        /**
         * Performance Seekbar
         */
        mValenceSeekbar = findViewById(R.id.valenceSeekBar);
        mValenceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mValenceLevel = progress-2;
            }
        });


        /**
         * Save Activity Button
         */
        mSaveActivityButton = findViewById(R.id.button_save_activity);
        mSaveActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success = saveStilaActivity();
                // Show an Animation after saving:
                if(success){
                    Intent intent = new Intent(mContext, ConfirmationActivity.class);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                            ConfirmationActivity.SUCCESS_ANIMATION);
                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                            "Saved");
                    startActivity(intent);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){
                        @Override
                        public void run(){
                            Intent intent = new Intent(mContext,MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    }, 1000);

                }
                else{
                    Intent intent = new Intent(mContext, ConfirmationActivity.class);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                            ConfirmationActivity.FAILURE_ANIMATION);
                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                            "Error");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){
                        @Override
                        public void run(){
                            Toast.makeText(mContext.getApplicationContext(), mErrorMsg, Toast.LENGTH_LONG).show();
                        }
                    }, 1000);

                }

            }
        });

        loadRadioButtonState();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TIME_PICKER_START) {
            if(resultCode == Activity.RESULT_OK){
                int resultHour=data.getIntExtra("hour",mStartHour);
                mStartHour = resultHour;

                int resultMinute = data.getIntExtra("minute",mStartMin);
                mStartMin = resultMinute;
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY,mStartHour);
                c.set(Calendar.MINUTE,mStartMin);
                long timestamp = c.getTimeInMillis()/1000;
                mActivityStartTime = timestamp;
                String time = String.format("%02d:%02d", mStartHour, mStartMin);
                mTimeRangeTextViewFrom.setText(time);
            }
        }

        if (requestCode == REQUEST_CODE_TIME_PICKER_END) {
            if(resultCode == Activity.RESULT_OK){
                int resultHour=data.getIntExtra("hour",mEndHour);
                mEndHour = resultHour;

                int resultMinute = data.getIntExtra("minute",mEndMin);
                mEndMin = resultMinute;
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY,mEndHour);
                c.set(Calendar.MINUTE,mEndMin);
                long timestamp = c.getTimeInMillis()/1000;
                mActivityEndTime = timestamp;
                String time = String.format("%02d:%02d", mEndHour, mEndMin);
                mTimeRangeTextViewTo.setText(time);
            }

        }
    }


    /**
     * Listener for the Radio Buttons. Sets the Global Vars for the Radio Buttons
     * @param view The Radio Button that was clicked.
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_negative:
                if (checked)
                    mMood=Constants.MOOD_NEGATIVE;//"NEGATIVE";
                break;
            case R.id.radio_neutral:
                if (checked)
                    mMood=Constants.MOOD_NEUTRAL;//"NEUTRAL";
                break;
            case R.id.radio_positive:
                if (checked)
                    mMood=Constants.MOOD_POSITIVE;//"POSITIVE";
                break;
            case R.id.radio_lying:
                if(checked)
                    mPosture = ActivityEntityConstants.LYING;
                break;
            case R.id.radio_standing:
                if(checked)
                    mPosture = ActivityEntityConstants.STANDING;
                break;
            case R.id.radio_sitting:
                if(checked)
                    mPosture = ActivityEntityConstants.SITTING;
                break;
            case R.id.nradio_medikation:
                if(checked)
                    mDrugs = ActivityEntityConstants.MEDICATION;
                break;
            case R.id.radio_caffeine:
                if(checked)
                    mDrugs = ActivityEntityConstants.COFFEE_DRINKS;
                break;
            case R.id.radio_none:
                if(checked)
                    mDrugs = ActivityEntityConstants.NONE;
                break;
            case R.id.radio_threatening:
                if(checked)
                    mExperience = ActivityEntityConstants.THREATENING;
                break;
            case R.id.radio_neutral_experience:
                if(checked)
                    mExperience = ActivityEntityConstants.NEUTRAL;
                break;
            case R.id.radio_challenging:
                if(checked)
                    mExperience = ActivityEntityConstants.CHALLENGING;
                break;
        }
    }


    /**
     * Sets the Radio Buttons of the Radio Button Section when the activity is loaded.
     */
    private void loadRadioButtonState() {

        // Mood
        RadioButton hasToBeChecked = findViewById(R.id.radio_neutral);
        hasToBeChecked.toggle();

        // Experience
        hasToBeChecked = findViewById(R.id.radio_neutral_experience);
        hasToBeChecked.toggle();

        // Drugs
        hasToBeChecked = findViewById(R.id.radio_none);
        hasToBeChecked.toggle();

        // Posture
        hasToBeChecked=findViewById(R.id.radio_sitting);
        hasToBeChecked.toggle();


    }

    /**
     * Saves the currently edited Activity
     * @return true if the activity was saved, false if there is a problem with the input data.
     */
    private boolean saveStilaActivity(){

        if(mActivityStartTime> mActivityEndTime){
            return false;
        }
        else{
            /**
             * Saving is done in the background
             */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long timestamp = System.currentTimeMillis()/1000;
                    // claim creation by wear app
                    String createdBy = ActivityEntityConstants.ByWearApp;
                    ActivityDatabaseHandler activityDatabaseHandler = new ActivityDatabaseHandler(mContext);
                    ActivityDatabase activity = new ActivityDatabase(mActivityStartTime,mActivityEndTime, mActivityName, mMood,getActivityTypeForActivty(mActivityName),mStressLevel,mPerformanceLevel, mValenceLevel,"", mPosture, mExperience, mDrugs, createdBy);
                    activityDatabaseHandler.addIfNotExists(activity,true);
                    // Track in Analytics
                    AnalyticsHelper.getInstance().trackNewActivity(AnalyticsHelper.DeviceType.WATCH, mActivityName);
                }
            }).start();
            return true;
        }

    }

    /**
     * This returns whether the activity is "mental" or "physical"
     * @param activityName the name of the activity
     * @return the type (mental/phyisical) of the activity. If there is no entry in the
     * activityTypeDB "mental" is returned.
     */
    private int getActivityTypeForActivty(String activityName){
        ActivityTypeDatabaseHandler dbHandler = new ActivityTypeDatabaseHandler(this);
        ActivityTypeDatabase entry = dbHandler.getEntryByName(activityName);
        if(entry != null){
            if(entry.getType().equals("physical"))
                return 1;
            if(entry.getType().equals("mental"))
                return 0;
        }
        // Default is "mental"
        return 0;
    }

    /**
     * Sets the default state of the timepickers, so it corresponds to the time which is shown
     * in the UI
     */
    private void setTimePickerDefaultState(){
        //Time-Picker Default Values:
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mActivityStartTime*1000);
        mStartHour = c.get(Calendar.HOUR_OF_DAY);
        mStartMin = c.get(Calendar.MINUTE);
        c.setTimeInMillis(mActivityEndTime*1000);
        mEndHour = c.get(Calendar.HOUR_OF_DAY);
        mEndMin = c.get(Calendar.MINUTE);
    }

}
