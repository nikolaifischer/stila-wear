package lmu.pms.stila.smartNotifications;



import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.TimeZone;

import lmu.pms.stila.R;
import lmu.pms.stila.Utils.NotificationUtil;
import lmu.pms.stila.Utils.TimeConversionUtil;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.ui.SelectActivity;

/**
 * This class creates notifications to be shown on the watch.
 * The notifications are especially targeted at small displays and should not be used on
 * phones.
 */
public class NotificationHelperWatch implements NotificationHelper {

    private static final String TAG = NotificationHelperWatch.class.getSimpleName();
    private Context mContext;

    public static final int NOTIFICATION_ID = 888;

    private NotificationManagerCompat mNotificationManagerCompat;

    private final String notificationTitle = "Stila";

    public NotificationHelperWatch(Context context){
        mContext = context;
        mNotificationManagerCompat = NotificationManagerCompat.from(context);
    }


    /**
     * Builds and sends a notification urging the user to track her activities in a
     * time window because she was stressed.
     * @param from start timestamp of stressed time window
     * @param to end timestamp of  stressed time windows
     */
    @Override
    public void sendTrackingNotification(long from, long to) {

        Log.d(TAG, "generateBigTextStyleNotification()");

        String fromStr = formatDate(from);
        String toStr = formatDate(to);

        String content = "You were stressed from "+fromStr+" to "+toStr+". Do you want to track " +
                "your activity?";

        // Main steps for building a BIG_TEXT_STYLE notification:
        //                .setBigContentTitle(notificationTitle);
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up main Intent for notification
        //      4. Create additional Actions for the Notification
        //      5. Build and issue the notification

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(mContext);

        // 2. Build the BIG_TEXT_STYLE
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                // Overrides ContentText in the big form of the template.
                .bigText(content);
                // Overrides ContentTitle in the big form of the template.

        // 3. Set up main Intent for notification.
        Intent mainIntent = new Intent(mContext, SelectActivity.class);
        mainIntent.putExtra("notification",true);
        mainIntent.putExtra("startTime",from);
        mainIntent.putExtra("endTime",to);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 4. Create additional Actions (Intents) for the Notification.

        // In our case, we create two additional actions: a Snooze action and a Dismiss action.

        // Snooze Action.
        Intent snoozeIntent = new Intent(mContext, BigTextIntentService.class);
        snoozeIntent.setAction(BigTextIntentService.ACTION_SNOOZE);

        PendingIntent snoozePendingIntent = PendingIntent.getService(mContext, 0, snoozeIntent, 0);
        NotificationCompat.Action snoozeAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_alarm_white_48dp,
                        "Snooze 30 minutes",
                        snoozePendingIntent)
                        .build();

        // Dismiss Action.
        Intent dismissIntent = new Intent(mContext, BigTextIntentService.class);
        dismissIntent.setAction(BigTextIntentService.ACTION_DISMISS);

        PendingIntent dismissPendingIntent = PendingIntent.getService(mContext, 0, dismissIntent, 0);
        NotificationCompat.Action dismissAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_cancel_white_48dp,
                        "Dismiss",
                        dismissPendingIntent)
                        .build();


        // Enables launching app in Wear 2.0 while keeping the old Notification Style behavior.
        NotificationCompat.Action mainAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_add_white,
                "Record Activity",
                mainPendingIntent)
                .build();

        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        // Notification Channel Id is ignored for Android pre O (26).
        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(
                        mContext, notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content.
                .setStyle(bigTextStyle)
                .setContentTitle(notificationTitle)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_add_white)
                .setLargeIcon(BitmapFactory.decodeResource(
                        mContext.getResources(),
                        R.drawable.stila_ic_launcher_round))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(mContext, R.color.primaryColor)) //getApplicationContext
                .setColorized(true)

                .setCategory(Notification.CATEGORY_REMINDER)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(NotificationCompat.PRIORITY_HIGH)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility( NotificationCompat.VISIBILITY_PUBLIC)

                .addAction(mainAction)
                // Adds additional actions specified above.
                .addAction(snoozeAction)
                .addAction(dismissAction);

        // This line can also be used to allow single-click notifications with out actions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            notificationCompatBuilder.setContentIntent(mainPendingIntent);
        }

        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
        AnalyticsHelper.getInstance().trackNotificationSent(AnalyticsHelper.DeviceType.WATCH);

    }


    public void sendSimpleNotification() {


        Log.d(TAG, "sendSimpleNotification()");


        String content = "Remember to regularly record your activities. It helps you to become more" +
                " aware of your stress.";

        // Main steps for building a BIG_TEXT_STYLE notification:
        //                .setBigContentTitle(notificationTitle);
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up main Intent for notification
        //      4. Create additional Actions for the Notification
        //      5. Build and issue the notification

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(mContext);

        // 2. Build the BIG_TEXT_STYLE
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                // Overrides ContentText in the big form of the template.
                .bigText(content);
        // Overrides ContentTitle in the big form of the template.

        // 3. Set up main Intent for notification.
        long now = System.currentTimeMillis()/1000;
        long hourBefore = now - 3600;
        Intent mainIntent = new Intent(mContext, SelectActivity.class);
        mainIntent.putExtra("notification",true);
        mainIntent.putExtra("startTime",hourBefore);
        mainIntent.putExtra("endTime",now);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 4. Create additional Actions (Intents) for the Notification.

        // In our case, we create two additional actions: a Snooze action and a Dismiss action.

        // Snooze Action.
        Intent snoozeIntent = new Intent(mContext, BigTextIntentService.class);
        snoozeIntent.setAction(BigTextIntentService.ACTION_SNOOZE);

        PendingIntent snoozePendingIntent = PendingIntent.getService(mContext, 0, snoozeIntent, 0);
        NotificationCompat.Action snoozeAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_alarm_white_48dp,
                        "Snooze 30 minutes",
                        snoozePendingIntent)
                        .build();

        // Dismiss Action.
        Intent dismissIntent = new Intent(mContext, BigTextIntentService.class);
        dismissIntent.setAction(BigTextIntentService.ACTION_DISMISS);

        PendingIntent dismissPendingIntent = PendingIntent.getService(mContext, 0, dismissIntent, 0);
        NotificationCompat.Action dismissAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_cancel_white_48dp,
                        "Dismiss",
                        dismissPendingIntent)
                        .build();


        // Enables launching app in Wear 2.0 while keeping the old Notification Style behavior.
        NotificationCompat.Action mainAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_add_white,
                "Record Activity",
                mainPendingIntent)
                .build();

        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        // Notification Channel Id is ignored for Android pre O (26).
        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(
                        mContext, notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content.
                .setStyle(bigTextStyle)
                .setContentTitle(notificationTitle)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_add_white)
                .setLargeIcon(BitmapFactory.decodeResource(
                        mContext.getResources(),
                        R.drawable.stila_ic_launcher_round))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(mContext, R.color.primaryColor)) //getApplicationContext
                .setColorized(true)

                .setCategory(Notification.CATEGORY_REMINDER)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(NotificationCompat.PRIORITY_HIGH)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility( NotificationCompat.VISIBILITY_PUBLIC)

                .addAction(mainAction)
                // Adds additional actions specified above.
                .addAction(snoozeAction)
                .addAction(dismissAction);

        // This line can also be used to allow single-click notifications with out actions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            notificationCompatBuilder.setContentIntent(mainPendingIntent);
        }

        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
        AnalyticsHelper.getInstance().trackNotificationSent(AnalyticsHelper.DeviceType.WATCH);

    }

    private String formatDate(long timestampInSecs) {
        long milliseconds = timestampInSecs * 1000;
        DateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        TimeZone tz = TimeZone.getDefault();
        sdf.setTimeZone(tz);
        return sdf.format(calendar.getTime());
    }
}
