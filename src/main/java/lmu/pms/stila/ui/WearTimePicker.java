package lmu.pms.stila.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import lmu.pms.stila.R;

/**
 * Activity with TimePicker. Used in SaveActivity.
 */
public class WearTimePicker extends WearableActivity {
    private TimePicker mTimePicker;
    private Intent returnIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_time_picker);

        returnIntent = new Intent();
        mTimePicker = findViewById(R.id.timePicker);
        mTimePicker.setIs24HourView(true);
        if(getIntent() !=null && getIntent().getExtras() !=null){
            int minute = getIntent().getExtras().getInt("minute",0);
            int hour = getIntent().getExtras().getInt("hour",0);
            mTimePicker.setMinute(minute);
            mTimePicker.setHour(hour);
        }

        mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                returnIntent.putExtra("hour",hourOfDay);
                returnIntent.putExtra("minute",minute);
                setResult(Activity.RESULT_OK,returnIntent);
            }
        });

        Button okButton = findViewById(R.id.timePickerOkButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
