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
import android.graphics.Color;
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
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.common.App;
import lmu.pms.stila.communication.HeartrateJobDispatcher;
import lmu.pms.stila.model.WatchfaceConfigData;
import lmu.pms.stila.provider.ComputedStressComplicationProvider;
import lmu.pms.stila.smartNotifications.NotificationAlarmReceiver;
import lmu.pms.stila.smartNotifications.NotificationScheduler;
import lmu.pms.stila.util.HeartRateListener;
import lmu.pms.stila.R;
import lmu.pms.stila.util.HeartRateScheduler;

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
public class StilaAnalogWatchFaceService extends CanvasWatchFaceService implements StilaWatchFace {


    private static final String TAG = "AnalogWatchFaceService";

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
    private static final long NORMAL_UPDATE_RATE_MS = 1000;

    private SurfaceHolder mSurfaceHolder;

    /**
     * COMPLICATIONS
     */
    private static final int LEFT_COMPLICATION_ID = 0;
    private static final int RIGHT_COMPLICATION_ID = 1;
    private static final int BOTTOM_COMPLICATION_ID = 2;

    private static final int[] COMPLICATION_IDS = {LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID, BOTTOM_COMPLICATION_ID};

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

    /** True, if the watch is charging**/
    private boolean mCharging;


    private HeartRateScheduler mHeartRateScheduler;
    private Thread mHeartRateSchedulerThread;


    private float mCenterX;
    private float mCenterY;
    private float mSecondHandLength;
    private float mMinuteHandLength;
    private float mHourHandLength;


    private static final float HOUR_STROKE_WIDTH = 5f;
    private static final float MINUTE_STROKE_WIDTH = 3f;
    private static final float SECOND_TICK_STROKE_WIDTH = 2f;

    private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;
    private boolean mBurnInProtection;
    private boolean mAmbient;
    private NotificationScheduler mNotificationScheduler;

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
        mSharedPreferences.edit().putString(getString(R.string.preference_watchface_mode), getString(R.string.analog)).commit();
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

        mHeartRateScheduler = new HeartRateScheduler(this,getApplicationContext());
        // Start Periodic HR Measurement using the Scheduler
        if(mMeasureIntervalInSeconds>getResources().getInteger(R.integer.measure_continuous)) {
            mHeartRateScheduler.setInterval(mMeasureIntervalInSeconds);
            mHeartRateScheduler.setStartFlag();
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
                        mDigitalConfigData = new WatchfaceConfigData(getApplicationContext());
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
                            updateComplicationDrawables();
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
                        invalidate();

                    }
                };


        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");
            super.onCreate(holder);

            mSurfaceHolder = holder;
            // Initialize Complications:
            initializeComplications();
            // Set Default Complications:
            setDefaultComplicationProvider(BOTTOM_COMPLICATION_ID,new ComponentName(getApplicationContext(),ComputedStressComplicationProvider.class),  ComplicationData.TYPE_SHORT_TEXT);

            mSharedPreferences.registerOnSharedPreferenceChangeListener(spChanged);


            setWatchFaceStyle(new WatchFaceStyle.Builder(StilaAnalogWatchFaceService.this)
                     .setAcceptsTapEvents(true)
                    .build());
            Resources resources = StilaAnalogWatchFaceService.this.getResources();

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

            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);

            mCalendar = Calendar.getInstance();
            mDate = new Date();

            /** Schedule periodically checking if Triggers (Notifications) should be send to the user **/
            NotificationAlarmReceiver receiver = new NotificationAlarmReceiver(getApplicationContext());
            //DEBUG;
            mNotificationScheduler = new NotificationScheduler(getApplicationContext());
            mNotificationScheduler.scheduleRecurringNotifications();
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
            mNotificationScheduler.disableRecurringNotifications();
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
            StilaAnalogWatchFaceService.this.registerReceiver(mReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = false;
            StilaAnalogWatchFaceService.this.unregisterReceiver(mReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = StilaAnalogWatchFaceService.this.getResources();
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

            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            //mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
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
            mAmbient = inAmbientMode;
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
            drawBackground(canvas);
            drawComplications(canvas,now);
            drawWatchFace(canvas);
        }







        private static final long LOGO_OFFSET_Y = 50;
        private static final long LOGO_OFFSET_X = 20;


        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);

            } else {
                Paint textPaint = new Paint();
                textPaint.setTypeface(mStilaTypeface);
                textPaint.setColor(mInteractiveHourDigitsColor);
                textPaint.setStrokeWidth(25f);
                textPaint.setTextSize(25f);
                textPaint.setAntiAlias(true);
                canvas.drawColor(new WatchfaceConfigData(getApplicationContext()).getInteractiveBackgroundColor(true));
                long y = canvas.getHeight()/2-LOGO_OFFSET_Y;
                long x = canvas.getWidth()/2-LOGO_OFFSET_X;
                canvas.drawText(getString(R.string.stila_lower),x,y,textPaint);
            }
        }


        private void drawWatchFace(Canvas canvas) {

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(
                        mCenterX + innerX,
                        mCenterY + innerY,
                        mCenterX + outerX,
                        mCenterY + outerY,
                        mHourPaint);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the icn_watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mMinutePaint);
            }
            canvas.drawCircle(
                    mCenterX, mCenterY, CENTER_GAP_AND_CIRCLE_RADIUS, mColonPaint);

            /* Restore the canvas' original orientation. */
            canvas.restore();
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
                            StilaAnalogWatchFaceService.class);

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


            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the icn_watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on icn_watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mHourHandLength = (float) (mCenterX * 0.5);
            // ANALOG STOP
            // Calculate Positions for Complications

            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;
            int paddingBottom = 0;
            int paddingLeft = 50;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication-paddingLeft);
            int verticalOffset = height/2 - sizeOfComplication/2 - paddingBottom;
            int bottomComplicationVerticalOffset = height-130;

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


            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen - sizeOfComplication/2),
                            bottomComplicationVerticalOffset,
                            (midpointOfScreen+sizeOfComplication/2),
                            (bottomComplicationVerticalOffset + sizeOfComplication));

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);



        }


        /** Helper Method which initialises the complications with the right data providers **/
        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);



            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.analog_complication_styles);
            if (leftComplicationDrawable != null) {

                leftComplicationDrawable.setBackgroundColorActive(mInteractiveBackgroundColor);
                leftComplicationDrawable.setContext(getApplicationContext());
                leftComplicationDrawable.setBorderColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setIconColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setTextColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setRangedValuePrimaryColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setTitleColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setBackgroundColorAmbient(mAmbientBackgroundColor);
                leftComplicationDrawable.setBorderColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setIconColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setTextColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setRangedValuePrimaryColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setTitleColorAmbient(mAmbientDigitsColor);

                leftComplicationDrawable.setContext(getApplicationContext());
            }

            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.analog_complication_styles);
            if (rightComplicationDrawable != null) {

                rightComplicationDrawable.setBackgroundColorActive(mInteractiveBackgroundColor);
                rightComplicationDrawable.setContext(getApplicationContext());
                rightComplicationDrawable.setBorderColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setIconColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setTextColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setRangedValuePrimaryColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setTitleColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setBackgroundColorAmbient(mAmbientBackgroundColor);
                rightComplicationDrawable.setBorderColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setIconColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setTextColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setRangedValuePrimaryColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setTitleColorAmbient(mAmbientDigitsColor);

                rightComplicationDrawable.setContext(getApplicationContext());
            }
            ComplicationDrawable bottomComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.analog_complication_styles);
            if (bottomComplicationDrawable != null) {
                bottomComplicationDrawable.setBackgroundColorActive(mInteractiveBackgroundColor);
                bottomComplicationDrawable.setContext(getApplicationContext());
                bottomComplicationDrawable.setBorderColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setIconColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setTextColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setRangedValuePrimaryColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setTitleColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setBackgroundColorAmbient(mAmbientBackgroundColor);
                bottomComplicationDrawable.setBorderColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setIconColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setTextColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setRangedValuePrimaryColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setTitleColorAmbient(mAmbientDigitsColor);

            }

            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);



            setActiveComplications(COMPLICATION_IDS);
        }

        /**
         * This method Updates the Colors of the Complications after a change of the Background
         * color of the watchface
         */
        private void updateComplicationDrawables(){
            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            if (leftComplicationDrawable != null) {

                leftComplicationDrawable.setBackgroundColorActive(mInteractiveBackgroundColor);
                leftComplicationDrawable.setContext(getApplicationContext());
                leftComplicationDrawable.setBorderColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setIconColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setTextColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setRangedValuePrimaryColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setTitleColorActive(mInteractiveHourDigitsColor);
                leftComplicationDrawable.setBackgroundColorAmbient(mAmbientBackgroundColor);
                leftComplicationDrawable.setBorderColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setIconColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setTextColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setRangedValuePrimaryColorAmbient(mAmbientDigitsColor);
                leftComplicationDrawable.setTitleColorAmbient(mAmbientDigitsColor);

                leftComplicationDrawable.setContext(getApplicationContext());
            }

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            if (rightComplicationDrawable != null) {

                rightComplicationDrawable.setBackgroundColorActive(mInteractiveBackgroundColor);
                rightComplicationDrawable.setContext(getApplicationContext());
                rightComplicationDrawable.setBorderColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setIconColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setTextColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setRangedValuePrimaryColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setTitleColorActive(mInteractiveHourDigitsColor);
                rightComplicationDrawable.setBackgroundColorAmbient(mAmbientBackgroundColor);
                rightComplicationDrawable.setBorderColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setIconColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setTextColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setRangedValuePrimaryColorAmbient(mAmbientDigitsColor);
                rightComplicationDrawable.setTitleColorAmbient(mAmbientDigitsColor);

                rightComplicationDrawable.setContext(getApplicationContext());
            }
            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            if (bottomComplicationDrawable != null) {
                bottomComplicationDrawable.setBackgroundColorActive(mInteractiveBackgroundColor);
                bottomComplicationDrawable.setContext(getApplicationContext());
                bottomComplicationDrawable.setBorderColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setIconColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setTextColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setRangedValuePrimaryColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setTitleColorActive(mInteractiveHourDigitsColor);
                bottomComplicationDrawable.setBackgroundColorAmbient(mAmbientBackgroundColor);
                bottomComplicationDrawable.setBorderColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setIconColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setTextColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setRangedValuePrimaryColorAmbient(mAmbientDigitsColor);
                bottomComplicationDrawable.setTitleColorAmbient(mAmbientDigitsColor);

            }

            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);


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
                case BOTTOM:
                    return BOTTOM_COMPLICATION_ID;
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
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[2];
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

    public void registerHRReceiver(){
        Log.d(TAG, "registerHRReceiver: registering Receiver");
        mSensorManager.registerListener(mHeartRateListener, mHeartrateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mHRListenerRegistered = true;

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

    public void startHRMonitoring() {
        if (mCharging) {
            // Load the saved interval setting
            int backupInterval = mSharedPreferences.getInt(getString(R.string.preference_backup_heartrate), getResources().getInteger(R.integer.measure_periodically));
            // Restart Measurment by changing the value in the settings
            mSharedPreferences.edit().putInt(getString(R.string.preference_heartrate_interval), backupInterval).commit();
            mCharging = false;

        }
    }


    }
