package io.neson.react.notification;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Publisher for scheduled wakeup.
 */
public class WakeupPublisher extends BroadcastReceiver {
    final static String WAKEUP_ID = "wakeupId";
    final static String WAKEUP = "wakeup";

    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra(WAKEUP_ID);
        long currentTime = System.currentTimeMillis();
        Log.i("ReactSystemNotification", "WakeupPublisher: Prepare To Publish: " + id + ", Now Time: " + currentTime);

        if (!applicationIsRunning(context)) {
            Intent newIntent = new Intent(context, BackgroundService.class);
            newIntent.putExtra("wakeupId", id);
            context.startService(newIntent);
            abortBroadcast();
        }

    }

    private boolean applicationIsRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
                if (processInfo.processName.equals(context.getApplicationContext().getPackageName())) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
//                            || processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
//                             || processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                        for (String d: processInfo.pkgList) {
                            Log.v("ReactSystemNotification", "NotificationEventReceiver: ok: " + d);
                            return true;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }

        return false;
    }
}
