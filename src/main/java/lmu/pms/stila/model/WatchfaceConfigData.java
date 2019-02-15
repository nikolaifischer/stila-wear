package lmu.pms.stila.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import lmu.pms.stila.R;
import lmu.pms.stila.config.AnalogComplicationConfigActivity;
import lmu.pms.stila.config.DigitalComplicationConfigActivity;
import lmu.pms.stila.config.StyleConfigActivity;
import lmu.pms.stila.config.MeasureIntervalActivity;
import lmu.pms.stila.config.SynchronisatonIntervalActivity;
import lmu.pms.stila.onboarding.OnboardingActivity;
import lmu.pms.stila.ui.MainActivity;

/**
 * This Class represents represents data for construction the configuration activity of the
 * digital watchface as well as data for construction the colors and complications of the watchface
 * corresponding to the current Theme and Mode
 */

public class WatchfaceConfigData {


    private Context mContext;

    // Constants to identify menu options on configuration screen
    private final String MEASURE_INTERVAL_OPTION ="MEASURE_INTERVAL_OPTION";
    private final String SYNC_INTERVAL_OPTION = "SYNC_INTERVAL_OPTION" ;
    private final String WATCHFACE_STYLE_OPTION = "WATCHFACE_STYLE_OPTION";
    private final String COMPLICATIONS_OPTION = "COMPLICATIONS_OPTION";
    private final String TUTORIAL_OPTION = "TUTORIAL_OPTION";


    // Lists to hold elements for every menu option
    private List<String> mMenuOptions;
    private List<Integer> mIcons;
    private List<Class> mActivities;
    private List<String> mMenuStrings;


    // Constant to identify the complications
    public enum ComplicationLocation {
        LEFT,
        RIGHT,
        BOTTOM
    }


    public WatchfaceConfigData(Context context){
        mMenuStrings = new ArrayList<>();
        mMenuOptions = new ArrayList<>();


        // This controls the order of the menu items on the config screen
        mMenuOptions.add(MEASURE_INTERVAL_OPTION);
        mMenuOptions.add(SYNC_INTERVAL_OPTION);
        mMenuOptions.add(WATCHFACE_STYLE_OPTION);
        mMenuOptions.add(COMPLICATIONS_OPTION);
        mMenuOptions.add(TUTORIAL_OPTION);

        mContext = context;
        buildIconAndActivitiesList(mMenuOptions);

    }

    /**
     *
     * @param index The index of the Menu Element
     * @return The readable String of the Menu Element
     */
    public String getMenuOptionString(int index){
        return mMenuStrings.get(index);
    }

    /**
     *
     * @return The number of configuration Options
     */
    public int getMenuOptionsSize(){
        return mMenuOptions.size();
    }

    /**
     * @param index The index of the Menu Element
     * @return The Resource ID of a Icon
     */
    public int getIcon(int index){
        return mIcons.get(index);
    }

    /**
     * @param index The index of the Menu Element
     * @return The .class for the Activity controlling the submenu
     */

    public Class getConfigActivity(int index){
        return mActivities.get(index);
    }

    /**
     * @param isDarkTheme Signals if the watchface uses the Dark Theme or the Light Theme
     * @return The Color of the Background in interactive mode
     */

    public int getInteractiveBackgroundColor(boolean isDarkTheme){

        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.stila_watchface_preferences), 0);
        int color = prefs.getInt(mContext.getString(R.string.preference_color_hue),0);
        if(color == 0){
            return Color.WHITE;
        }
        else {
            return color;
        }
    }

    /**
     * @return The Color of the Background in ambient mode
     */
    public int getAmbientBackgroundColor(){
        return Color.BLACK;
    }

    /**
     * @return The Color of the Highlighted Area where the complications are positioned
     */
    // Colors the Arc
    public int getInteractiveHighlightColor(){
        return Color.BLACK;
    }

    /**
     * @param isDarkTheme Signals if the watchface uses the Dark Theme or the Light Theme
     * @return The Color of the Hour digits
     */

    public int getInteractiveHoursColor(boolean isDarkTheme){
        if(isDarkTheme){
            return Color.WHITE;
        }
        else
            return Color.BLACK;
    }
    /**
     * @param isDarkTheme Signals if the watchface uses the Dark Theme or the Light Theme
     * @return The Color of the Minute digits
     */

    public int getInteractiveMinutesColor(boolean isDarkTheme){
        if(isDarkTheme){
            return Color.WHITE;
        }
        else
            return Color.BLACK;
    }

    /**
     * @param isDarkTheme Signals if the watchface uses the Dark Theme or the Light Theme
     * @return The Color of the Seconds Digits
     */

    public int getInteractiveSecondsColor(boolean isDarkTheme){
        if(isDarkTheme){
            return Color.WHITE;
        }
        else
            return Color.GRAY;
    }

    /**
     * @return The Color of the digits in Ambient Mode
     */

    public int getAmbientDigitsColor(){
        return Color.WHITE;
    }

    /**
     * @param isDarkTheme Signals if the watchface uses the Dark Theme or the Light Theme
     * @return The Color of "Stila" String in Interactive Mode
     */

    public int getStilaStringColor(boolean isDarkTheme){
        if(isDarkTheme){
            return Color.WHITE;
        }
        else{
            return Color.BLACK;
        }
    }


    /**
     * Helper Method which builds the Icon, Activity and String Lists
     * @param optionStrings The List containing the menu constants
     */
    private void  buildIconAndActivitiesList(List <String> optionStrings){

        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.stila_watchface_preferences), 0);
        String mode = prefs.getString(mContext.getString(R.string.preference_watchface_mode),"digital");
        mIcons = new ArrayList<>();
        mActivities = new ArrayList<>();
        for(int i = 0; i<optionStrings.size();i++){
            switch (optionStrings.get(i)){
                case MEASURE_INTERVAL_OPTION:
                    mIcons.add(R.drawable.icn_heart_pulse);
                    mActivities.add(MeasureIntervalActivity.class);
                    mMenuStrings.add(mContext.getString(R.string.config_menu_measure_interval));
                    break;
                case SYNC_INTERVAL_OPTION:
                    mIcons.add(R.drawable.icn_sync);
                    mActivities.add(SynchronisatonIntervalActivity.class);
                    mMenuStrings.add(mContext.getString(R.string.config_menu_sync_interval));
                    break;
                case WATCHFACE_STYLE_OPTION:
                    mIcons.add(R.drawable.icn_styles);
                    mActivities.add(StyleConfigActivity.class);
                    mMenuStrings.add(mContext.getString(R.string.config_menu_watchface_style));
                    break;
                case COMPLICATIONS_OPTION:
                    mIcons.add(R.drawable.icn_watch);
                    if(mode.equals(mContext.getString(R.string.analog)))
                        mActivities.add(AnalogComplicationConfigActivity.class);
                    else
                        mActivities.add(DigitalComplicationConfigActivity.class);
                    mMenuStrings.add(mContext.getString(R.string.config_menu_complications));
                    break;
                case TUTORIAL_OPTION:
                    mIcons.add(R.drawable.icn_help);
                    mActivities.add(OnboardingActivity.class);
                    mMenuStrings.add(mContext.getString(R.string.config_menu_tutorial));
                default:
                    // No Icon available - add placeholder to avoid exception
                    mIcons.add(R.drawable.icn_watch);
                    mActivities.add(MainActivity.class);
                    mMenuStrings.add("NOT IMPLEMENTED");
            }
        }

    }


}
