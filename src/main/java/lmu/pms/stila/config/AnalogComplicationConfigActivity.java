package lmu.pms.stila.config;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.concurrent.Executors;

import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.model.WatchfaceConfigData;
import lmu.pms.stila.watchface.StilaAnalogWatchFaceService;

public class AnalogComplicationConfigActivity extends WearableActivity implements View.OnClickListener {

    private static final String TAG = "ConfigActivity";

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;
    private int mLeftComplicationId;
    private int mRightComplicationId;
    private int mBottomComplicationId;

    // Selected complication id by user.
    private int mSelectedComplicationId;

    // ComponentName used to identify a specific service that renders the icn_watch face.
    private ComponentName mWatchFaceComponentName;

    // Required to retrieve complication data from icn_watch face for preview.
    private ProviderInfoRetriever mProviderInfoRetriever;

    private ImageView mLeftComplicationBackground;
    private ImageView mRightComplicationBackground;
    private ImageView mBottomComplicationBackground;

    private ImageButton mLeftComplication;
    private ImageButton mRightComplication;
    private ImageButton mBottomComplication;

    private Drawable mDefaultAddComplicationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());
        setContentView(R.layout.activity_analog_complication_config);

        mDefaultAddComplicationDrawable = getDrawable(R.drawable.add_complication);

        mSelectedComplicationId = -1;

        mLeftComplicationId =
                StilaAnalogWatchFaceService.getComplicationId(WatchfaceConfigData.ComplicationLocation.LEFT);
        mRightComplicationId =
                StilaAnalogWatchFaceService.getComplicationId(WatchfaceConfigData.ComplicationLocation.RIGHT);
        mBottomComplicationId =
                StilaAnalogWatchFaceService.getComplicationId(WatchfaceConfigData.ComplicationLocation.BOTTOM);

        mWatchFaceComponentName =
                new ComponentName(getApplicationContext(), StilaAnalogWatchFaceService.class);


        // Sets up left complication preview.
        mLeftComplicationBackground = (ImageView) findViewById(R.id.left_complication_background);
        mLeftComplication = (ImageButton) findViewById(R.id.left_complication);
        mLeftComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mLeftComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mLeftComplicationBackground.setVisibility(View.INVISIBLE);

        // Sets up right complication preview.
        mRightComplicationBackground = (ImageView) findViewById(R.id.right_complication_background);
        mRightComplication = (ImageButton) findViewById(R.id.right_complication);
        mRightComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mRightComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mRightComplicationBackground.setVisibility(View.INVISIBLE);

        // Sets up right complication preview.
        mBottomComplicationBackground = (ImageView) findViewById(R.id.bottom_complication_background);
        mBottomComplication = (ImageButton) findViewById(R.id.bottom_complication);
        mBottomComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mBottomComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mBottomComplicationBackground.setVisibility(View.INVISIBLE);

        mProviderInfoRetriever =
                new ProviderInfoRetriever(getApplicationContext(), Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();

        retrieveInitialComplicationsData();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mProviderInfoRetriever.release();
    }

    public void retrieveInitialComplicationsData() {

        final int[] complicationIds = StilaAnalogWatchFaceService.getComplicationIds();

        mProviderInfoRetriever.retrieveProviderInfo(
                new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                    @Override
                    public void onProviderInfoReceived(
                            int watchFaceComplicationId,
                            @Nullable ComplicationProviderInfo complicationProviderInfo) {

                        Log.d(TAG, "\n\nonProviderInfoReceived: " + complicationProviderInfo);

                        updateComplicationViews(watchFaceComplicationId, complicationProviderInfo);
                    }
                },
                mWatchFaceComponentName,
                complicationIds);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mLeftComplication)) {
            Log.d(TAG, "Left Complication click()");
            launchComplicationHelperActivity(WatchfaceConfigData.ComplicationLocation.LEFT);

        } else if (view.equals(mRightComplication)) {
            Log.d(TAG, "Right Complication click()");
            launchComplicationHelperActivity(WatchfaceConfigData.ComplicationLocation.RIGHT);
        }
        else if (view.equals(mBottomComplication)) {
            Log.d(TAG, "Bottom Complication click()");
            launchComplicationHelperActivity(WatchfaceConfigData.ComplicationLocation.BOTTOM);
        }
    }

    // Verifies the icn_watch face supports the complication location, then launches the helper
    // class, so user can choose their complication data provider.

    private void launchComplicationHelperActivity(WatchfaceConfigData.ComplicationLocation complicationLocation) {

        mSelectedComplicationId =
                StilaAnalogWatchFaceService.getComplicationId(complicationLocation);

        if (mSelectedComplicationId >= 0) {

            int[] supportedTypes =
                    StilaAnalogWatchFaceService.getSupportedComplicationTypes(
                            complicationLocation);

            startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            getApplicationContext(),
                            mWatchFaceComponentName,
                            mSelectedComplicationId,
                            supportedTypes),
                    AnalogComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);

        } else {
            Log.d(TAG, "Complication not supported by icn_watch face.");
        }
    }

    public void updateComplicationViews(
            int watchFaceComplicationId, ComplicationProviderInfo complicationProviderInfo) {
        Log.d(TAG, "updateComplicationViews(): id: " + watchFaceComplicationId);
        Log.d(TAG, "\tinfo: " + complicationProviderInfo);

        if (watchFaceComplicationId == mLeftComplicationId) {
            if (complicationProviderInfo != null) {
                mLeftComplication.setImageIcon(complicationProviderInfo.providerIcon);
                mLeftComplicationBackground.setVisibility(View.VISIBLE);

            } else {
                mLeftComplication.setImageDrawable(mDefaultAddComplicationDrawable);
                mLeftComplicationBackground.setVisibility(View.INVISIBLE);
            }

        } else if (watchFaceComplicationId == mRightComplicationId) {
            if (complicationProviderInfo != null) {
                mRightComplication.setImageIcon(complicationProviderInfo.providerIcon);
                mRightComplicationBackground.setVisibility(View.VISIBLE);

            } else {
                mRightComplication.setImageDrawable(mDefaultAddComplicationDrawable);
                mRightComplicationBackground.setVisibility(View.INVISIBLE);
            }
        } else if (watchFaceComplicationId == mBottomComplicationId) {
            if (complicationProviderInfo != null) {
                mBottomComplication.setImageIcon(complicationProviderInfo.providerIcon);
                mBottomComplicationBackground.setVisibility(View.VISIBLE);

            } else {
                mBottomComplication.setImageDrawable(mDefaultAddComplicationDrawable);
                mBottomComplicationBackground.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);

            if (mSelectedComplicationId >= 0) {
                updateComplicationViews(mSelectedComplicationId, complicationProviderInfo);
            }
        }
    }
}
