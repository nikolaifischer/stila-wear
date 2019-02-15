package lmu.pms.stila.watchface;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.Locale;

import lmu.pms.stila.util.BodySensorPermissionActivity;

/**
 * Helper Class for both the Analog and the Digital Stila Watch Face
 */

public class StilaWatchFacesHelper {


    /**
     * Starts an Activity to ask the User for permission to use the Body Sensors of the Device
     * @param context
     */
    public static void getBodySensorPermission(Context context){
        Intent dialogIntent = new Intent(context, BodySensorPermissionActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(dialogIntent);
    }


    public static String formatTwoDigitNumber(int hour) {
        return String.format(Locale.getDefault(),"%02d", hour);
    }

    /**
     * Creates a Text Paint for the Watchface
     * @param defaultInteractiveColor The Color in which the Text should be displayed in
     * @param typeface the Typeface the Text should use
     * @return the finished Paint
     */
    public static Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
        Paint paint = new Paint();
        paint.setColor(defaultInteractiveColor);
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);
        return paint;
    }


}
