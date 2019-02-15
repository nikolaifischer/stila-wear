/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lmu.pms.stila.watchface;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.common.App;
import lmu.pms.stila.communication.HeartrateJobDispatcher;
import lmu.pms.stila.model.WatchfaceConfigData;
import lmu.pms.stila.provider.ComputedStressComplicationProvider;
import lmu.pms.stila.util.HeartRateListener;
import lmu.pms.stila.util.HeartRateScheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static lmu.pms.stila.watchface.StilaWatchFacesHelper.formatTwoDigitNumber;

/**
 *
 * Stila digital watch face with inbuilt Heart Rate recording and support for multiple
 * themes and complications. Some parts are based on the Google Example Code for a digital
 * watch face.
 *
 * This class is the center piece of the watch face. It paints the watch face after reading out
 * user-specific settings.
 *
 * The Heartrate Measurement is also triggered from here by adding a SensorListener to the HR Sensor
 * of the Watch. (HeartrateListener.java)
 *
 * After the measurement the Listener unregistered it's self and has to be re-registered by this
 * class. For this an AlarmManager is used which triggers an Alarm at an interval specified by
 * the user (10 seconds, 1 minute ...)
 *
 * There is also a Job Scheduleded which synchronizes the measured data to the phone. For this see
 * the class HeartrateJobDispatcher and HeartrateSyncJobService
 */
public class StilaDigitalWatchFaceService extends CanvasWatchFaceService implements StilaWatchFace {


    private static final String TAG = "DigitalWatchFaceService";

    /** The Typeface used to render the Time**/
    private Typeface mTimeTypeface;
    /** The Typeface used to render the Stila String **/
    private Typeface mStilaTypeface;

    /** SensorManager to access the Heartrate Sensor**/
    private SensorManager mSensorManager;

    /** The Heart Rate Sensor of this Watch **/
    private Sensor mHeartrateSensor;

    /** Holds Reference to the Listener for Heart Rate Changes*/
    private HeartRateListener mHeartRateListener;

    /** Preference Object, which holds Information on User - Specific Settings **/
    private SharedPreferences mSharedPreferences;

    /** Reference to Object of Helper Class, which holds Information on Colors and Menu
     * Options
     */
    private WatchfaceConfigData mDigitalConfigData;

    /**
     * Broadcast Receiver for Battery Events
     */
    private BroadcastReceiver mPowerChangedReceiver;

    /**
     * Update rate in milliseconds for normal (not ambient and not mute) mode. We update twice
     * a second to blink the colons.
     */
    private static final long NORMAL_UPDATE_RATE_MS = 500;

    private HeartRateScheduler mHeartRateScheduler;
    private Thread mHeartRateSchedulerThread;



    /**
     * COMPLICATIONS
     */
    private static final int LEFT_COMPLICATION_ID = 0;
    private static final int RIGHT_COMPLICATION_ID = 1;

    private static final int[] COMPLICATION_IDS = {LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID};

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            }
    };

    // Uses SparseArrays to avoid boxing/unboxing of ints
    // is used to map active complication IDs to their data
    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

    // is used to map active complication IDs to their drawables
    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    /** Scheduler and Alarms for HR Measurement and Synchronisation **/

    /** Global int storing the interval in seconds between to HR Measurements **/
    private int mMeasureIntervalInSeconds;

    /** Flag to indicate whether there is a HR Listener Registered **/
    private boolean mHRListenerRegistered;

    private boolean mCharging;

    @Override
    public Engine onCreateEngine() {
        if(App.context == null){
            App.context = getApplicationContext();
        }
      mTimeTypeface = Typeface.createFromAsset(getAssets(),"fonts/roboto_mono_thin.ttf");
      mStilaTypeface = Typeface.createFromAsset(getAssets(),"fonts/poiret_one_regular.ttf");

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        mCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        // Get the necessary Permissions to use the Heart Rate Sensor
        if(checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED){
            StilaWatchFacesHelper.getBodySensorPermission(this);
        }

        mHeartRateListener = new HeartRateListener(this, getApplicationContext());
        mSharedPreferences =  getSharedPreferences(getString(R.string.stila_watchface_preferences), 0);
        mSharedPreferences.edit().putString(getString(R.string.preference_watchface_mode), getString(R.string.digital)).commit();
        mDigitalConfigData = new WatchfaceConfigData(this);




        // REGISTER HEARTRATE SENSOR:
        try {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (mSensorManager != null) {
                mHeartrateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
                Log.d(TAG, "getMinDelay in Millis: "+String.valueOf(mHeartrateSensor.getMinDelay()));
                if(mHeartrateSensor == null){
                    throw new Exception("No Heart Rate Sensor found on this device");
                }
            }
            else {

                throw new Exception("No Sensor Manager found on this device");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMeasureIntervalInSeconds = mSharedPreferences.getInt(getString(R.string.preference_heartrate_interval),getResources().getInteger(R.integer.measure_periodically));

        // SCHEDULE THE SYNC JOB:
        int syncMinutes = mSharedPreferences.getInt(getString(R.string.preference_sync_interval), 0);
        if(syncMinutes > 0){
            HeartrateJobDispatcher.scheduleHeartrateSync(this, syncMinutes);
        }

        mHeartRateScheduler = new HeartRateScheduler(this, getApplicationContext());
        // Start Periodic HR Measurement using the Scheduler
        if(mMeasureIntervalInSeconds>getResources().getInteger(R.integer.measure_continuous)) {
            mHeartRateScheduler.setInterval(mMeasureIntervalInSeconds);
            mHeartRateSchedulerThread = new Thread(mHeartRateScheduler);
            mHeartRateSchedulerThread.start();
        }
        else{
            registerHRReceiver();
        }


        // Broadcast Receiver for Battery Charging Events
        // If the Watch is being charged the HR Sensor will not measure.
        IntentFilter powerFilter = new IntentFilter();
        powerFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        powerFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");

        mPowerChangedReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(TAG, "onReceive: "+intent.getAction());

                boolean isCharging = intent.getAction() == "android.intent.action.ACTION_POWER_CONNECTED";

                if(isCharging){
                   cancelHRMonitoring();
                }
                else{
                    startHRMonitoring();
                }
            }
        };
        getApplicationContext().registerReceiver(mPowerChangedReceiver,powerFilter);

        return new Engine();

    }


    private class Engine extends CanvasWatchFaceService.Engine  {

        static final String COLON_STRING = ":";

        static final int MSG_UPDATE_TIME = 0;

        /** How often {@link #mUpdateTimeHandler} ticks in milliseconds. */
        long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

        /** Handler to update the time periodically in interactive mode.
         * HandlerLeak is suppressed because this is the way the time is updated in all of Google's
         * examples.*/

        @SuppressLint("HandlerLeak")
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };


        /**
         * Handles time zone and locale changes.
         */
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };


        /**
         * Unregistering an unregistered receiver throws an exception. Keep track of the
         * registration state to prevent that.
         */
        boolean mRegisteredReceiver = false;

        Paint mBackgroundPaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mColonPaint;
        Paint mHighlightPaint;
        float mColonWidth;

        Calendar mCalendar;
        Date mDate;

        /** Flag to show if Colons shoud be drawn - colons are only drawn periodically
         * to achieve blinking effect
         */
        boolean mShouldDrawColons;

        /** Geometric Properties **/
        float mXOffset;
        float mYOffset;
        float mLineHeight;

        //Flag to show if dark or light theme is used
        boolean mIsDarkTheme = mSharedPreferences.getBoolean(getString(R.string.use_dark_theme),false);

        /** Init Colors **/
        int mInteractiveBackgroundColor =
                mDigitalConfigData.getInteractiveBackgroundColor(mIsDarkTheme);
        int mInteractiveHighlightColor =
                mDigitalConfigData.getInteractiveHighlightColor();
        int mInteractiveHourDigitsColor =
                mDigitalConfigData.getInteractiveHoursColor(mIsDarkTheme);
        int mInteractiveMinuteDigitsColor =
                mDigitalConfigData.getInteractiveMinutesColor(mIsDarkTheme);
        int mInteractiveSecondDigitsColor =
                mDigitalConfigData.getInteractiveSecondsColor(mIsDarkTheme);
        int mAmbientBackgroundColor =
                mDigitalConfigData.getAmbientBackgroundColor();
        int mAmbientDigitsColor=
                mDigitalConfigData.getAmbientDigitsColor();

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        /**
         * Listener on the SharedPreferences. When the Theme Selection changes, all Paints have
         * to be assigned new colors and the canvas has to be redrawn.
         */
        SharedPreferences.OnSharedPreferenceChangeListener spChanged = new
                SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                          String key) {
                        if(key.equals(getString(R.string.use_dark_theme)) || key.equals(getString(R.string.preference_color_hue))){
                            mIsDarkTheme = sharedPreferences.getBoolean(getString(R.string.use_dark_theme),false);
                            mInteractiveBackgroundColor =
                                    mDigitalConfigData.getInteractiveBackgroundColor(mIsDarkTheme);
                            mInteractiveHourDigitsColor =
                                    mDigitalConfigData.getInteractiveHoursColor(mIsDarkTheme);
                            mInteractiveMinuteDigitsColor =
                                    mDigitalConfigData.getInteractiveMinutesColor(mIsDarkTheme);
                            mInteractiveSecondDigitsColor =
                                    mDigitalConfigData.getInteractiveSecondsColor(mIsDarkTheme);
                            mInteractiveHighlightColor =
                                    mDigitalConfigData.getInteractiveHighlightColor();


                            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
                            mMinutePaint.setColor(mInteractiveMinuteDigitsColor);
                            mSecondPaint.setColor(mInteractiveSecondDigitsColor);
                            mHighlightPaint.setColor(mInteractiveHighlightColor);
                            mHourPaint.setColor(mInteractiveHourDigitsColor);
                            mColonPaint.setColor(mInteractiveHourDigitsColor);
                            invalidate();
                        }
                        if(key.equals(getString(R.string.preference_heartrate_interval))){
                            mMeasureIntervalInSeconds =
                                    sharedPreferences.getInt(getString(R.string.preference_heartrate_interval),getResources().getInteger(R.integer.measure_periodically));
                            // Cancel current Scheduler:
                            if(mHeartRateScheduler!=null)
                                mHeartRateScheduler.setStopFlag();
                            // Start new Periodic HR Measurement Alarm if the setting is not continuous
                            if(mMeasureIntervalInSeconds>getResources().getInteger(R.integer.measure_continuous)) {
                                mHeartRateScheduler.setInterval(mMeasureIntervalInSeconds);
                                mHeartRateScheduler.setStartFlag();
                                mHeartRateSchedulerThread = new Thread(mHeartRateScheduler);
                                mHeartRateSchedulerThread.start();
                            }
                            else{
                                registerHRReceiver();
                            }

                            // Update the Complication to show whether HR Recording is active
                            ComponentName provider = new ComponentName(getApplicationContext(), ComputedStressComplicationProvider.class);
                            new ProviderUpdateRequester(getApplicationContext(), provider)
                                    .requestUpdateAll();

                        }

                        if(key.equals(getString(R.string.preference_sync_interval))){
                            int syncMinutes =
                                    sharedPreferences.getInt(getString(R.string.preference_sync_interval),0);

                            // Instant Synchronisation
                            if(syncMinutes <1){
                                HeartrateJobDispatcher.cancelHeartrateJobSync(getApplicationContext());
                                mHeartRateListener.setSyncInstant(true);
                            }
                            // Cached Synchronisation:
                            else{
                                mHeartRateListener.setSyncInstant(false);
                                HeartrateJobDispatcher.cancelHeartrateJobSync(getApplicationContext());
                                HeartrateJobDispatcher.scheduleHeartrateSync(getApplicationContext(), syncMinutes);

                            }

                        }

                    }
                };


        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");
            super.onCreate(holder);

            // Initialize Complications:
            initializeComplications();
            // Set Default Complications:
            setDefaultComplicationProvider(LEFT_COMPLICATION_ID,new ComponentName(getApplicationContext(),ComputedStressComplicationProvider.class),  ComplicationData.TYPE_SHORT_TEXT);
            setDefaultSystemComplicationProvider(RIGHT_COMPLICATION_ID, SystemProviders.DATE,ComplicationData.TYPE_SHORT_TEXT);

            mSharedPreferences.registerOnSharedPreferenceChangeListener(spChanged);




            setWatchFaceStyle(new WatchFaceStyle.Builder(StilaDigitalWatchFaceService.this)
                     .setAcceptsTapEvents(true)
                    .build());
            Resources resources = StilaDigitalWatchFaceService.this.getResources();

            /* Init Geometry and Paints */
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
            mHighlightPaint  = new Paint();
            mHighlightPaint.setColor(mInteractiveHighlightColor);
            mHighlightPaint.setAntiAlias(true);
            mHourPaint = createTextPaint(mInteractiveHourDigitsColor, mTimeTypeface);
            mMinutePaint = createTextPaint(mInteractiveMinuteDigitsColor);
            mSecondPaint = createTextPaint(mInteractiveSecondDigitsColor);
            mColonPaint = createTextPaint(mInteractiveHourDigitsColor);

            mCalendar = Calendar.getInstance();
            mDate = new Date();
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "onDestroy: ");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            getApplicationContext().unregisterReceiver(mPowerChangedReceiver);
            mSensorManager.unregisterListener(mHeartRateListener);
            // If the watch is charging, the HR Measurement Value has to be reset to the
            // backup Interval to restart with the correct Interval when the watchfaced is restarted.
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(spChanged);
            if(mCharging){
                int backupInterval = mSharedPreferences.getInt(getString(R.string.preference_backup_heartrate), getResources().getInteger(R.integer.measure_periodically));
                mSharedPreferences.edit().putInt(getString(R.string.preference_heartrate_interval),backupInterval).commit();
            }
            super.onDestroy();
        }


        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, mTimeTypeface);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            paint.setFakeBoldText(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone and date formats, in case they changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                //mSensorManager.unregisterListener(mHeartRateListener);
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }


        private void registerReceiver() {
            if (mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            StilaDigitalWatchFaceService.this.registerReceiver(mReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = false;
            StilaDigitalWatchFaceService.this.unregisterReceiver(mReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = StilaDigitalWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mLineHeight = resources.getDimension(R.dimen.digital_line_height);
            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mSecondPaint.setTextSize(textSize);
            mColonPaint.setTextSize(textSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            //mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
                        + ", low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            // Get the necessary Permissions to use the Heart Rate Sensor
            if(checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED){
                StilaWatchFacesHelper.getBodySensorPermission(getApplicationContext());
            }

            adjustPaintColorToCurrentMode(mBackgroundPaint, mInteractiveBackgroundColor,
                    mAmbientBackgroundColor);
            adjustPaintColorToCurrentMode(mHourPaint, mInteractiveHourDigitsColor,
                    mAmbientDigitsColor);
            adjustPaintColorToCurrentMode(mMinutePaint, mInteractiveMinuteDigitsColor,
                    mAmbientDigitsColor);
            // Actually, the seconds are not rendered in the ambient mode, so we could pass just any
            // value as ambientColor here.
            adjustPaintColorToCurrentMode(mSecondPaint, mInteractiveSecondDigitsColor,
                    mAmbientDigitsColor);
            adjustPaintColorToCurrentMode(mColonPaint,mInteractiveHourDigitsColor,mAmbientDigitsColor);

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mColonPaint.setAntiAlias(antiAlias);

            }

            ComplicationDrawable complicationDrawable;
            // Render Complications in Ambient Mode:
            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_ID);
                complicationDrawable.setInAmbientMode(inAmbientMode);
            }

            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }





        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
           // boolean is24Hour = DateFormat.is24HourFormat(StilaDigitalWatchFaceService.this);

            // Overwritten for now: Watch Face always shows Time as 24 hour String:
            boolean is24Hour = true;



            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            // Draw the highlighted area
            if(!isInAmbientMode()){
                float left = 0 - bounds.width()/2;
                float top = bounds.height()/2 + bounds.height()/8;
                float right = bounds.width()+ bounds.width()/2;
                float bottom = bounds.height()*2;
                canvas.drawArc(left,top,right,bottom,180F,180F,true, mHighlightPaint);
            }

            // Draw the hours.
            float x = mXOffset;
            if(isInAmbientMode()){
                x+=getResources().getDimension(R.dimen.interactive_additional_x_offset_ambient);
            }
            String hourString;
            if (is24Hour) {
                hourString = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                hourString = String.valueOf(hour);
            }
            canvas.drawText(hourString, x, mYOffset, mHourPaint);
            x += mHourPaint.measureText(hourString);

            // In ambient and mute modes, always draw the first colon. Otherwise, draw the
            // first colon for the first half of each second.
            if (isInAmbientMode() || mShouldDrawColons) {
                canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);
            }
            x += mColonWidth;

            // Draw the minutes.
            String minuteString = formatTwoDigitNumber(mCalendar.get(Calendar.MINUTE));
            canvas.drawText(minuteString, x, mYOffset, mMinutePaint);
            x += mMinutePaint.measureText(minuteString);


            // In interactive mode, draw a second blinking colon followed by the seconds.
            if (!isInAmbientMode()) {
                if (mShouldDrawColons) {
                    canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);
                }
                x += mColonWidth;
                canvas.drawText(formatTwoDigitNumber(
                        mCalendar.get(Calendar.SECOND)), x, mYOffset, mSecondPaint);

                // Draw Stila Text
                String stilaString = getString(R.string.stila_lower);
                float stilaY = mYOffset + getResources().getDimension(R.dimen.stila_text_offset);
                Paint stilaPaint = new Paint();
                stilaPaint.setTypeface(mStilaTypeface);
                stilaPaint.setTextSize(getResources().getDimension(R.dimen.stila_text_size));
                stilaPaint.setColor(mDigitalConfigData.getStilaStringColor(mIsDarkTheme));
                stilaPaint.setTextAlign(Paint.Align.CENTER);
                stilaPaint.setAntiAlias(true);
                float stilaX = bounds.width() / 2;
                canvas.drawText(stilaString, stilaX, stilaY, stilaPaint);

            }

            drawComplications(canvas,now);

        }


        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }


        /**
         * Listens to Taps on the Complication to trigger actions defined by the
         * complication
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            switch (tapType) {
                case TAP_TYPE_TAP:
                    int tappedComplicationId = getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        onComplicationTap(tappedComplicationId);
                    }
                    break;
            }
        }


        /**
         * Determines if tap inside a complication area or returns -1.
         * @param x X Coordinate of the Tap
         * @param y Y Coordinate of the Tap
         * @return ID of the tapped complication or -1 if nothing was tapped
         */
        private int getTappedComplicationId(int x, int y) {

            int complicationId;
            ComplicationData complicationData;
            ComplicationDrawable complicationDrawable;

            long currentTimeMillis = System.currentTimeMillis();

            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                complicationId = COMPLICATION_ID;
                complicationData = mActiveComplicationDataSparseArray.get(complicationId);

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                    Rect complicationBoundingRect = complicationDrawable.getBounds();

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return complicationId;
                        }
                    } else {
                        Log.e(TAG, "Not a recognized complication id.");
                    }
                }
            }
            return -1;
        }


        private void onComplicationTap(int complicationId) {
            Log.d(TAG, "onComplicationTap()");

            ComplicationData complicationData =
                    mActiveComplicationDataSparseArray.get(complicationId);

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "onComplicationTap() tap action error: " + e);
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    ComponentName componentName = new ComponentName(
                            getApplicationContext(),
                            StilaDigitalWatchFaceService.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }

            } else {
                Log.d(TAG, "No PendingIntent for complication " + complicationId + ".");
            }
        }


        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // Calculate Positions for Complications
            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;
            int paddingBottom = 20;
            int paddingLeft = 5;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication-paddingLeft);
            int verticalOffset = height - sizeOfComplication - paddingBottom;

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (width - horizontalOffset - sizeOfComplication),
                            verticalOffset,
                            ((width - horizontalOffset)),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);

        }


        /** Helper Method which initialises the complications with the right data providers **/
        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            if (leftComplicationDrawable != null) {
                leftComplicationDrawable.setContext(getApplicationContext());
            }

            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            if (rightComplicationDrawable != null) {
                rightComplicationDrawable.setContext(getApplicationContext());
            }

            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);

            setActiveComplications(COMPLICATION_IDS);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Helper Method to adjust the colors of elements depending on the mode
         * @param paint The Paint which color has to be changed
         * @param interactiveColor the desired color in interactive mode
         * @param ambientColor the desired color in ambient mode
         */
        private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor,
                                                   int ambientColor) {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }


    }

    public SensorManager getSensorManager(){
        return mSensorManager;
    }


    /**
     * HELPER METHODS FOR COMPLICATIONS
     */

    private void drawComplications(Canvas canvas, long currentTimeMillis) {
        int complicationId;
        ComplicationDrawable complicationDrawable;

        for (int COMPLICATION_ID : COMPLICATION_IDS) {
            complicationId = COMPLICATION_ID;
            complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

            complicationDrawable.draw(canvas, currentTimeMillis);
        }
    }

    public static int getComplicationId(
        WatchfaceConfigData.ComplicationLocation complicationLocation){
            switch (complicationLocation) {
                case LEFT:
                    return LEFT_COMPLICATION_ID;
                case RIGHT:
                    return RIGHT_COMPLICATION_ID;
                default:
                    return -1;
            }
        }



    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    public static int[] getSupportedComplicationTypes(
            WatchfaceConfigData.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            default:
                return new int[] {};
        }
    }

    /**
     * Sets a flag indicating that there is a HR Listener registered
     * @param bool true if there is a HR Listener registered
     */
    public void setHRListenerRegistered(boolean bool){
            mHRListenerRegistered = bool;
    }

    /**
     * Returns true if there is a HR Listener registered
     * @return true if there is a HR Listener registered
     */
    public boolean getHRListenerRegistered(){
        return mHRListenerRegistered;
    }

    /**
     * Public method to register this watchface's HR Listener
     */
    public void registerHRReceiver(){
        Log.d(TAG, "registerHRReceiver: registering Receiver");
        mSensorManager.registerListener(mHeartRateListener, mHeartrateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mHRListenerRegistered = true;

    }

    /**
     * Public method to unregister this watchface's HR Listener
     */
    public void unregisterHRListener(){
        mSensorManager.unregisterListener(mHeartRateListener);
    }


    public void cancelHRMonitoring(){
        if(!mCharging) {
            // Save the current Setting
            int currentMeasureInterval = mSharedPreferences.getInt(getString(R.string.preference_heartrate_interval), getResources().getInteger(R.integer.measure_periodically));
            mSharedPreferences.edit().putInt(getString(R.string.preference_backup_heartrate), currentMeasureInterval).apply();
            // Deactivate Stila HR Monitoring globally
            mSharedPreferences.edit().putInt(getString(R.string.preference_heartrate_interval), getResources().getInteger(R.integer.measure_never)).commit();
            mCharging = true;
        }

    }

    public void startHRMonitoring(){
        if(mCharging){
            // Load the saved interval setting
            int backupInterval = mSharedPreferences.getInt(getString(R.string.preference_backup_heartrate), getResources().getInteger(R.integer.measure_periodically));
            // Restart Measurment by changing the value in the settings
            mSharedPreferences.edit().putInt(getString(R.string.preference_heartrate_interval),backupInterval).commit();
            mCharging = false;

        }



    }


}
