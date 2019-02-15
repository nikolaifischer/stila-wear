package lmu.pms.stila.config;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;

public class SynchronisatonIntervalActivity extends WearableActivity  {

    private TextView mSyncTextView;
    private SeekBar mSyncSeekBar;
    private TextView mMinutesTextView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synchronisaton_interval);
        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());
        mSyncTextView = (TextView) findViewById(R.id.configSyncProgressTextView);
        mSyncSeekBar = (SeekBar) findViewById(R.id.configSyncSeekBar);
        mMinutesTextView = (TextView) findViewById(R.id.configSyncMinutesTextView);

        sharedPreferences = getSharedPreferences(getString(R.string.stila_watchface_preferences), 0);
        int savedValue = sharedPreferences.getInt(getString(R.string.preference_sync_interval),0);
        setLabels(savedValue);
        mSyncSeekBar.setProgress(savedValue);
        mSyncSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int roundedProgress = progress/10;
                roundedProgress = roundedProgress *10;
                setLabels(roundedProgress);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(getString(R.string.preference_sync_interval),roundedProgress);
                editor.commit();

            }
        });

    }

    /**
     * Sets the Text View in the Activity according to the current value of the slider
     * when the slider is at 0, the textview presents a String reading "Instant"
     * @param progress the Progress of the Slider
     */
    private void setLabels(int progress){
        if(progress>0) {
            mSyncTextView.setText(String.valueOf(progress));
            mMinutesTextView.setVisibility(View.VISIBLE);
        }
        else if(progress == 0) {
            mSyncTextView.setText(getString(R.string.instant));
            mMinutesTextView.setVisibility(View.INVISIBLE);
        }


    }
}
