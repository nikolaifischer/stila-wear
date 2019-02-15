package lmu.pms.stila.provider;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import lmu.pms.stila.Database.HRVDatabase;
import lmu.pms.stila.Database.HRVDatabaseHandler;
import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.common.App;
import lmu.pms.stila.ui.MainActivity;

/**
 * Complication Provider for Stila Computed Stress Levels.
 * Updates every 10 minutes (see Manifest.xml for configuration)
 * Only supported type is Short Text.
 */
public class ComputedStressComplicationProvider extends ComplicationProviderService {

    private static final String TAG = ComputedStressComplicationProvider.class.getSimpleName();
    private StressedIndicatorAlgorithm stressedIndicator;
    /*
     * default sampling rate = 1 / default_hr_sampling_interval = 0.1 Hz
     */
    private static final int DEFAULT_HR_SAMPLING_INTERVAL = 10;

    @Override
    public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {

        // Check if HR Measurement is activated. If not show on Complication
        SharedPreferences prefs = getSharedPreferences(getString(R.string.stila_watchface_preferences), MODE_PRIVATE);
        // if there is not choosen sampling interval in preferences use the default from code
        int hrInterval = prefs.getInt(getString(R.string.preference_heartrate_interval), DEFAULT_HR_SAMPLING_INTERVAL);

        // A StressedIndicatorAlgorithm returns computed stress bounds for this provider to decided
        // if a user is stressed, neutral or relaxed.
        StressedIndicatorAlgorithm algorithm = new FixedBoundsStressedIndicator(this);
        algorithm.updateData();

        // Used to create a unique key to use with SharedPreferences for this complication.
        ComponentName thisProvider = new ComponentName(this, getClass());

        String feeling = "";

        String relaxed = "relaxed";
        String stressed = "stressed";
        String neutral = "neutral";

        double computedStress = getComputedStressValue();

        if(computedStress > algorithm.getStressedBound()){
            feeling = stressed;
        }
        else if(computedStress <= algorithm.getStressedBound() && computedStress>algorithm.getRelaxedBound()){

            feeling = neutral;
        }
        else if(computedStress <= algorithm.getRelaxedBound()){
            feeling = relaxed;
        }

        ComplicationData complicationData = null;

        if(computedStress==-1){
            feeling = neutral;
        }

        if(hrInterval == 0){
            feeling = "No HR";
        }


        // Pending Intent for Tap on Complication: Open the Stress Graph
        Intent intent = new Intent(this,MainActivity.class);
        intent.putExtra(getString(R.string.flag_from_complication),true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        switch (dataType) {
            case ComplicationData.TYPE_SHORT_TEXT:
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                .setShortText(ComplicationText.plainText(feeling))
                                //.setShortTitle(ComplicationText.plainText("Stila"))
                                .setTapAction(pendingIntent)
                                .build();
                break;
            default:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type " + dataType);
                }
        }

        if (complicationData != null) {
            complicationManager.updateComplicationData(complicationId, complicationData);

        } else {
            // If no data is sent, we still need to inform the ComplicationManager, so
            // the update job can finish and the wake lock isn't held any longer.
            complicationManager.noUpdateRequired(complicationId);
        }

        // Track Usage of Complication:
        if(App.context != null)
            AnalyticsHelper.getInstance().trackComplicationUsage();
    }

    public double getComputedStressValue(){
        HRVDatabaseHandler hrvDatabaseHandler = new HRVDatabaseHandler(this);
        HRVDatabase lastEntry = hrvDatabaseHandler.getLastEntry();
        if(lastEntry == null){
            return -1;
        }
        else {
            long timeNow = System.currentTimeMillis()/1000;
            long age = timeNow - lastEntry.getTimestamp();
            if(age>1800)  // If the last HRV calculation is older than 30 minutes its not valid.
                return -1;
            return lastEntry.getComputedStress();
        }
    }
}
