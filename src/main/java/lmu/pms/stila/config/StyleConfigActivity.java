package lmu.pms.stila.config;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.jraf.android.androidwearcolorpicker.app.ColorPickActivity;

import java.io.IOException;

import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.watchface.StilaAnalogWatchFaceService;

public class StyleConfigActivity extends WearableActivity implements Switch.OnCheckedChangeListener {

    private Switch mDarkThemeSwitch;
    private SharedPreferences mPrefs;
    private Context mContext;

    private final static int REQUEST_PICK_COLOR = 140;
    private Button mColorButton;
    private Button mBrightnessButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mPrefs = getSharedPreferences(getString(R.string.stila_watchface_preferences), 0);
        setContentView(R.layout.activity_digital_style_config);
        mDarkThemeSwitch = (Switch) findViewById(R.id.darkThemeSwitch);
        mDarkThemeSwitch.setOnCheckedChangeListener(this);
        mPrefs =  getSharedPreferences(getString(R.string.stila_watchface_preferences), 0);
        boolean isDarkTheme = mPrefs.getBoolean(getString(R.string.use_dark_theme),false);
        mDarkThemeSwitch.setChecked(isDarkTheme);

        mColorButton = findViewById(R.id.settings_color_button);
        mColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startColorPickerActivity();
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_COLOR:
                if (resultCode == RESULT_CANCELED) {
                    // The user pressed 'Cancel'
                    break;
                }

                int pickedColor = ColorPickActivity.getPickedColor(data);
                mPrefs.edit().putInt(getString(R.string.preference_color_hue),pickedColor).commit();
                AnalyticsHelper.getInstance().trackChangedWatchfaceStyle();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(getString(R.string.use_dark_theme),b);
        editor.commit();
    }

    private void startColorPickerActivity(){
        int oldColor = mPrefs.getInt(getString(R.string.preference_color_hue),0);
        Intent intent = new ColorPickActivity.IntentBuilder().oldColor(oldColor).build(mContext);
        startActivityForResult(intent, REQUEST_PICK_COLOR);

    }
}
