/*
Copyright 2016 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package lmu.pms.stila.smartNotifications;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import lmu.pms.stila.R;
import lmu.pms.stila.Utils.NotificationUtil;
import lmu.pms.stila.ui.MainActivity;

import static lmu.pms.stila.SysConstants.Constants.SNOOZE_TIME_IN_SECS;

/**
 * Asynchronously handles snooze and dismiss actions for reminder app (and active Notification).
 * Notification for for reminder app uses BigTextStyle.
 */
public class BigTextIntentService extends IntentService {

    private static final String TAG = "BigTextService";

    public static final String ACTION_DISMISS =
            "lmu.pms.stila.wearnotifications.handlers.action.DISMISS";
    public static final String ACTION_SNOOZE =
            "lmu.pms.stila.wearnotifications.handlers.action.SNOOZE";

    private static final long SNOOZE_TIME = TimeUnit.SECONDS.toMillis(SNOOZE_TIME_IN_SECS); // 30 minutes

    public BigTextIntentService() {
        super("BigTextIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent(): " + intent);

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DISMISS.equals(action)) {
                handleActionDismiss();
            } else if (ACTION_SNOOZE.equals(action)) {
                handleActionSnooze();
            }
        }
    }

    /**
     * Handles action Dismiss in the provided background thread.
     */
    private void handleActionDismiss() {
        Log.d(TAG, "handleActionDismiss()");

        NotificationManagerCompat notificationManagerCompat =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.cancel(NotificationHelperWatch.NOTIFICATION_ID);
    }

    /**
     * Handles action Snooze in the provided background thread.
     */
    private void handleActionSnooze() {
        Log.d(TAG, "handleActionSnooze()");

        // You could use NotificationManager.getActiveNotifications() if you are targeting SDK 23
        // and above, but we are targeting devices with lower SDK API numbers, so we saved the
        // builder globally and get the notification back to recreate it later.

        NotificationCompat.Builder notificationCompatBuilder =
                GlobalNotificationBuilder.getNotificationCompatBuilderInstance();

        // Recreate builder from persistent state if app process is killed
        if (notificationCompatBuilder == null) {
            // Process was killed while snoozing.
            return;
        }

        Notification notification;
        notification = notificationCompatBuilder.build();


        if (notification != null) {
            NotificationManagerCompat notificationManagerCompat =
                    NotificationManagerCompat.from(getApplicationContext());

            notificationManagerCompat.cancel(NotificationHelperWatch.NOTIFICATION_ID);

            try {
                Thread.sleep(SNOOZE_TIME);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            notificationManagerCompat.notify(NotificationHelperWatch.NOTIFICATION_ID, notification);
        }

    }

}