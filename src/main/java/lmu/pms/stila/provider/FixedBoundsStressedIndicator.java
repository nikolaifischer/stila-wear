package lmu.pms.stila.provider;

import android.content.Context;
import android.content.SharedPreferences;

import lmu.pms.stila.R;

/**
 * This class returns fixed bounds indicating if a user is stressed.
 * The bounds are different depending on the measuring mode the watch is in.
 * When the watch measures every second, the variance in the data is far less. Because of this the
 * stress bounds are set higher.
 */
public class FixedBoundsStressedIndicator implements StressedIndicatorAlgorithm {

    private final Context mContext;

    private int mHRMeasureInterval;


    /**
     * Describes how much higher the bounds should be when the watch measures every second.
     */
    private final float MEASURE_FACTOR = (float) 2.0;

    public FixedBoundsStressedIndicator(Context context) {
        mContext = context;
        mHRMeasureInterval = getHRMeasureInterval();
    }

    @Override
    public float getRelaxedBound() {
        if(mHRMeasureInterval>1)
            return 10;
        else
            return MEASURE_FACTOR * 10;
    }

    @Override
    public float getStressedBound() {
        if(mHRMeasureInterval > 1)
            return 30;
        else
            return  MEASURE_FACTOR * 30;
    }

    @Override
    public void updateData() {
        mHRMeasureInterval = getHRMeasureInterval();
    }

    private int getHRMeasureInterval(){
        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.stila_watchface_preferences),Context.MODE_PRIVATE);
        int interval = prefs.getInt(mContext.getString(R.string.preference_heartrate_interval),10);
        return interval;

    }
}
