package lmu.pms.stila.config;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;

public class MeasureIntervalActivity extends WearableActivity {

    private static final String TAG = "MeasureIntervalActivity";
    private SharedPreferences sharedPreferences;
    private boolean isCharging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());
        setContentView(R.layout.activity_measure_interval);
        sharedPreferences = getSharedPreferences(getString(R.string.stila_watchface_preferences), 0);
        loadRadioButtonState();
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        if(isCharging){
            Toast toast = Toast.makeText(this,R.string.not_while_charging, Toast.LENGTH_LONG);
            toast.show();
            this.finish();
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        int newVal = getResources().getInteger(R.integer.measure_never);

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_continuous:
                if (checked)
                    newVal=getResources().getInteger(R.integer.measure_continuous);
                    break;
            case R.id.radio_often:
                if (checked)
                    newVal=getResources().getInteger(R.integer.measure_often);
                    break;
            case R.id.radio_periodically:
                if (checked)
                    newVal=getResources().getInteger(R.integer.measure_periodically);
                    break;
            case R.id.radio_never:
                if (checked)
                    newVal=getResources().getInteger(R.integer.measure_never);
                    break;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.preference_heartrate_interval),newVal);
        editor.commit();

        Log.d(TAG, "HR Interval is now "+ String.valueOf(newVal));

        AnalyticsHelper.getInstance().trackHRMeasurementInterval(newVal);

    }



    public void loadRadioButtonState() {

        int value = sharedPreferences.getInt(getString(R.string.preference_heartrate_interval),getResources().getInteger(R.integer.measure_periodically));

        RadioButton hasToBeChecked = findViewById(R.id.radio_never);


        if(value == getResources().getInteger(R.integer.measure_never)){
            hasToBeChecked = findViewById(R.id.radio_never);
        }
        else if(value == getResources().getInteger(R.integer.measure_continuous)){
            hasToBeChecked = findViewById(R.id.radio_continuous);
        }
        else if(value == getResources().getInteger(R.integer.measure_often)){
            hasToBeChecked = findViewById(R.id.radio_often);
        }
        else if(value == getResources().getInteger(R.integer.measure_periodically)){
            hasToBeChecked = findViewById(R.id.radio_periodically);
        }
        else if(value == getResources().getInteger(R.integer.measure_never)){
            hasToBeChecked = findViewById(R.id.radio_never);
        }

        hasToBeChecked.toggle();


    }



}
