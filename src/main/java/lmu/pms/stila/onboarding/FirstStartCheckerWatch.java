package lmu.pms.stila.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

import lmu.pms.stila.R;

/**
 * Utility class, which can be used to check and set the First Start flag on the watch for tutorial and onboarding
 * purposes.
 */
public class FirstStartCheckerWatch {

    static boolean DEBUG_MODE = false;
    /**
     * Checks if this app is started for the first time.
     * @param context a valid context
     * @return true if the app is started for the first time, false else.
     */
    public static boolean isFirstStart(Context context){
        if(DEBUG_MODE)
            return true;
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.stila_watchface_preferences),Context.MODE_PRIVATE);
        return prefs.getBoolean(context.getString(R.string.prefs_first_start),true);
    }

    /**
     * Sets the first start preference
     * @param context a valid context
     * @param bool the new value of the preference
     */
    public static void setFirstStart(Context context, Boolean bool){
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.stila_watchface_preferences),Context.MODE_PRIVATE);
        prefs.edit().putBoolean(context.getString(R.string.prefs_first_start),bool).apply();
    }

}
