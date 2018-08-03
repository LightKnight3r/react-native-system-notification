package io.neson.react.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Random;

/**
 * An object for wakeup app
 *
 */
public class Wakeup {
    private Context context;
    private String id;
    private int subId;
    private WakeupAttributes attributes;

    /**
     * Constructor.
     */
    public Wakeup(Context context, String id, @Nullable WakeupAttributes attributes) {
        this.context = context;
        this.id = id;
        Random rand = new Random();
        this.subId = rand.nextInt();
        this.attributes = attributes;
    }

    /**
     * Public context getter.
     */
    public Context getContext() {
        return context;
    }

    /**
     * Public attributes getter.
     */
    public WakeupAttributes getAttributes() {
        return attributes;
    }

    /**
     * Create the wakeup, show it now or set the schedule.
     */
    public Wakeup create() {
        setAlarmAndSaveOrRun();

        Log.i("ReactSystemNotification", "Notification Created: " + id);

        return this;
    }

    /**
     * Update the wakeup, resets its schedule.
     */
    public Wakeup update(WakeupAttributes wakeupAttributes) {
        delete();
        attributes = wakeupAttributes;
        setAlarmAndSaveOrRun();

        return this;
    }


    /**
     * Cancel the wakeup.
     */
    public Wakeup delete() {
//        getSysNotificationManager().cancel(subId);

        if (attributes.delayed || attributes.scheduled) {
            cancelAlarm();
        }

        deleteFromPreferences();

        Log.i("ReactSystemNotification", "WakeupDeleted: " + id);

        return this;
    }


//    /**
//     * Show the notification now.
//     */
//    public void show() {
//        getSysNotificationManager().notify(0, build());
//
//        Log.i("ReactSystemNotification", "Notification Show: " + id);
//    }

    /**
     * Setup alarm or show the wakeup.
     */
    public void setAlarmAndSaveOrRun() {
        if (attributes.delayed) {
            setDelay();
            saveAttributesToPreferences();

        } else if (attributes.scheduled) {
            setSchedule();
            saveAttributesToPreferences();

        } else {
//            show();
        }
    }

    /**
     * Schedule the delayed wakeup.
     */
    public void setDelay() {
        PendingIntent pendingIntent = getScheduleWakeupIntent();

        long futureInMillis = SystemClock.elapsedRealtime() + attributes.delay;
        getAlarmManager().set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);

        Log.i("ReactSystemNotification", "Wakeup Delay Alarm Set: " + id + ", Repeat Type: " + attributes.repeatType + ", Current Time: " + System.currentTimeMillis() + ", Delay: " + attributes.delay);
    }

    /**
     * Schedule the wakeup.
     */
    public void setSchedule() {
        PendingIntent pendingIntent = getScheduleWakeupIntent();

        if (attributes.repeatType == null) {
            getAlarmManager().set(AlarmManager.RTC_WAKEUP, attributes.sendAt, pendingIntent);
            Log.i("ReactSystemNotification", "Set One-Time Alarm: " + id);

        } else {
            switch (attributes.repeatType) {
                case "time":
                    getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, attributes.sendAt, attributes.repeatTime, pendingIntent);
                    Log.i("ReactSystemNotification", "Set " + attributes.repeatTime + "ms Alarm: " + id);
                    break;

                case "minute":
                    getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, attributes.sendAt, 60000, pendingIntent);
                    Log.i("ReactSystemNotification", "Set Minute Alarm: " + id);
                    break;

                case "hour":
                    getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, attributes.sendAt, AlarmManager.INTERVAL_HOUR, pendingIntent);
                    Log.i("ReactSystemNotification", "Set Hour Alarm: " + id);
                    break;

                case "halfDay":
                    getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, attributes.sendAt, AlarmManager.INTERVAL_HALF_DAY, pendingIntent);
                    Log.i("ReactSystemNotification", "Set Half-Day Alarm: " + id);
                    break;

                case "day":
                case "week":
                case "month":
                case "year":
                    getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, attributes.sendAt, AlarmManager.INTERVAL_DAY, pendingIntent);
                    Log.i("ReactSystemNotification", "Set Day Alarm: " + id + ", Type: " + attributes.repeatType);
                    break;

                default:
                    getAlarmManager().set(AlarmManager.RTC_WAKEUP, attributes.sendAt, pendingIntent);
                    Log.i("ReactSystemNotification", "Set One-Time Alarm: " + id);
                    break;
            }
        }

        Log.i("ReactSystemNotification", "Notification Schedule Alarm Set: " + id + ", Repeat Type: " + attributes.repeatType + ", Current Time: " + System.currentTimeMillis() + ", First Send At: " + attributes.sendAt);
    }

    /**
     * Cancel the delayed notification.
     */
    public void cancelAlarm() {
        PendingIntent pendingIntent = getScheduleWakeupIntent();
        getAlarmManager().cancel(pendingIntent);

        Log.i("ReactSystemNotification", "Notification Alarm Canceled: " + id);
    }

    public void saveAttributesToPreferences() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();

        String attributesJSONString = new Gson().toJson(attributes);

        editor.putString(id, attributesJSONString);

        if (Build.VERSION.SDK_INT < 9) {
            editor.commit();
        } else {
            editor.apply();
        }

        Log.i("ReactSystemNotification", "Notification Saved To Pref: " + id + ": " + attributesJSONString);
    }

    public void loadAttributesFromPreferences() {
        String attributesJSONString = getSharedPreferences().getString(id, null);
        this.attributes = (WakeupAttributes) new Gson().fromJson(attributesJSONString, WakeupAttributes.class);

        Log.i("ReactSystemNotification", "Wakeup Loaded From Pref: " + id + ": " + attributesJSONString);
    }

    public void deleteFromPreferences() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();

        editor.remove(id);

        if (Build.VERSION.SDK_INT < 9) {
            editor.commit();
        } else {
            editor.apply();
        }

        Log.i("ReactSystemNotification", "Notification Deleted From Pref: " + id);
    }

//    private NotificationManager getSysNotificationManager() {
//        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private SharedPreferences getSharedPreferences () {
        return (SharedPreferences) context.getSharedPreferences(io.neson.react.notification.NotificationManager.PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

//    private PendingIntent getContentIntent() {
//        Intent intent = new Intent(context, NotificationEventReceiver.class);
//
//        intent.putExtra(NotificationEventReceiver.NOTIFICATION_ID, id);
//        intent.putExtra(NotificationEventReceiver.ACTION, attributes.action);
//        intent.putExtra(NotificationEventReceiver.PAYLOAD, attributes.payload);
//
//        return PendingIntent.getBroadcast(context, subId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//    }

    private PendingIntent getScheduleWakeupIntent() {
        Intent wakeupIntent = new Intent(context, WakeupPublisher.class);
        wakeupIntent .putExtra(WakeupPublisher.WAKEUP_ID, id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, subId, wakeupIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }
}
