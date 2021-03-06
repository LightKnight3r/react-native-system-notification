package io.neson.react.notification;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.SystemClock;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.net.Uri;
import android.app.NotificationChannel;
import android.content.ContentResolver;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import java.lang.System;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import io.neson.react.notification.NotificationAttributes;
import io.neson.react.notification.NotificationEventReceiver;
import io.neson.react.notification.NotificationPublisher;

import java.util.Random;
import android.util.Base64;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.graphics.Color;
import android.media.AudioAttributes;

/**
 * An object-oriented Wrapper class around the system notification class.
 *
 * Each instance is an representation of a single, or a set of scheduled
 * notifications. It handles operations like showing, canceling and clearing.
 */
public class Notification {
    private Context context;
    private String id;
    private int subId;
    private NotificationAttributes attributes;
    private static String CHANNEL_ID;

    /**
     * Constructor.
     */
    public Notification(Context context, String id, @Nullable NotificationAttributes attributes) {
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
    public NotificationAttributes getAttributes() {
        return attributes;
    }

    /**
     * Create the notification, show it now or set the schedule.
     */
    public Notification create() {
        setAlarmAndSaveOrShow();

        Log.i("ReactSystemNotification", "Notification Created: " + id);

        return this;
    }

    /**
     * Update the notification, resets its schedule.
     */
    public Notification update(NotificationAttributes notificationAttributes) {
        delete();
        attributes = notificationAttributes;
        setAlarmAndSaveOrShow();

        return this;
    }

    /**
     * Clear the notification from the status bar.
     */
    public Notification clear() {
        getSysNotificationManager().cancel(subId);

        Log.i("ReactSystemNotification", "Notification Cleared: " + id);

        return this;
    }

    /**
     * Cancel the notification.
     */
    public Notification delete() {
        getSysNotificationManager().cancel(subId);

        if (attributes.delayed || attributes.scheduled) {
            cancelAlarm();
        }

        deleteFromPreferences();

        Log.i("ReactSystemNotification", "Notification Deleted: " + id);

        return this;
    }

    /**
     * Build the notification.
     */
     public void createChannel() {
         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
           return;
         NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
         // The id of the channel.
         CHANNEL_ID = "channel_heyu_sysnotify_channel";
         String id = CHANNEL_ID + attributes.sound;
         CHANNEL_ID = CHANNEL_ID + attributes.sound;
         // The user-visible name of the channel.
         CharSequence name = "Thông báo SS-HeyU";
         int importance = NotificationManager.IMPORTANCE_HIGH;
         NotificationChannel mChannel = new NotificationChannel(id, name, importance);
         AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
         Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

         if("default_rington".equals(attributes.sound)){
             soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
         } else if(!attributes.sound.equals("default")) {
             int resId = context.getResources().getIdentifier(attributes.sound, "raw", context.getPackageName());
             soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + resId);
         }

         // Configure the notification channel.
         //mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
         mChannel.enableLights(true);
         mChannel.enableVibration(true);
         mChannel.setLightColor(Color.BLUE);
         mChannel.setSound(soundUri, audioAttributes);
         mNotificationManager.createNotificationChannel(mChannel);
     }

    public android.app.Notification build() {
        CHANNEL_ID = "channel_heyu_sysnotify_channel";
        createChannel();
        android.support.v4.app.NotificationCompat.Builder notificationBuilder = new android.support.v4.app.NotificationCompat.Builder(context,CHANNEL_ID);

        notificationBuilder
            .setContentTitle(attributes.subject)
            .setContentText(attributes.message)
            .setSmallIcon(context.getResources().getIdentifier(attributes.smallIcon, "mipmap", context.getPackageName()))
            .setAutoCancel(attributes.autoClear)
            .setContentIntent(getContentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setChannelId(CHANNEL_ID);

        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        if (attributes.largeIcon != null) {
            int largeIconId = context.getResources().getIdentifier(attributes.largeIcon, "drawable", context.getPackageName());
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), largeIconId);
            notificationBuilder.setLargeIcon(largeIcon);
        }

        if (attributes.group != null) {
          notificationBuilder.setGroup(attributes.group);
          notificationBuilder.setGroupSummary(true);
        }

        if(attributes.inboxStyle){

            android.support.v4.app.NotificationCompat.InboxStyle inboxStyle = new android.support.v4.app.NotificationCompat.InboxStyle();

            if(attributes.inboxStyleBigContentTitle != null){
                inboxStyle.setBigContentTitle(attributes.inboxStyleBigContentTitle);
            }
            if(attributes.inboxStyleSummaryText != null){
                inboxStyle.setSummaryText(attributes.inboxStyleSummaryText);
            }
            if(attributes.inboxStyleLines != null){
                for(int i=0; i< attributes.inboxStyleLines.size(); i++){
                    inboxStyle.addLine(Html.fromHtml(attributes.inboxStyleLines.get(i)));
                }
            }
            notificationBuilder.setStyle(inboxStyle);


            Log.i("ReactSystemNotification", "set inbox style!!");

        }
        // else{
        //
        //     int defaults = 0;
        //     if ("default".equals(attributes.sound)) {
        //         defaults = defaults | android.app.Notification.DEFAULT_SOUND;
        //     }else if("default_rington".equals(attributes.sound)){
        //         attributes.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString();
        //     }
        //     if ("default".equals(attributes.vibrate)) {
        //         defaults = defaults | android.app.Notification.DEFAULT_VIBRATE;
        //     }
        //     if ("default".equals(attributes.lights)) {
        //         defaults = defaults | android.app.Notification.DEFAULT_LIGHTS;
        //     }
        //     notificationBuilder.setDefaults(defaults);
        //
        // }

        if (attributes.onlyAlertOnce != null) {
            notificationBuilder.setOnlyAlertOnce(attributes.onlyAlertOnce);
        }

        if (attributes.tickerText != null) {
            notificationBuilder.setTicker(attributes.tickerText);
        }

        if (attributes.when != null) {
            notificationBuilder.setWhen(attributes.when);
            notificationBuilder.setShowWhen(true);
        }

        // if bigText is not null, it have priority over bigStyleImageBase64
        if (attributes.bigText != null) {
            notificationBuilder
              .setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
              .bigText(attributes.bigText));
        }
        else if (attributes.bigStyleUrlImgage != null && attributes.bigStyleUrlImgage != "") {

            Bitmap bigPicture = null;

            try {

                Log.i("ReactSystemNotification", "start to get image from URL : " + attributes.bigStyleUrlImgage);
                URL url = new URL(attributes.bigStyleUrlImgage);
                bigPicture = BitmapFactory.decodeStream(url.openStream());
                Log.i("ReactSystemNotification", "finishing to get image from URL");

            } catch (Exception e) {
                Log.e("ReactSystemNotification", "Error when getting image from URL" + e.getStackTrace());
            }

            if (bigPicture != null) {
                notificationBuilder
                        .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bigPicture));
            }
        }
        else if (attributes.bigStyleImageBase64 != null) {

            Bitmap bigPicture = null;

            try {

                Log.i("ReactSystemNotification", "start to convert bigStyleImageBase64 to bitmap");
                // Convert base64 image to Bitmap
                byte[] bitmapAsBytes = Base64.decode(attributes.bigStyleImageBase64.getBytes(), Base64.DEFAULT);
                bigPicture = BitmapFactory.decodeByteArray(bitmapAsBytes, 0, bitmapAsBytes.length);
                Log.i("ReactSystemNotification", "finished to convert bigStyleImageBase64 to bitmap");

            } catch (Exception e) {
                Log.e("ReactSystemNotification", "Error when converting base 64 to Bitmap" + e.getStackTrace());
            }

            if (bigPicture != null) {
                notificationBuilder
                        .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bigPicture));
            }
        }

        if (attributes.color != null) {
          notificationBuilder.setColor(Color.parseColor(attributes.color));
        }

        if (attributes.subText != null) {
            notificationBuilder.setSubText(attributes.subText);
        }

        if (attributes.progress != null) {
            if (attributes.progress < 0 || attributes.progress > 1000) {
                notificationBuilder.setProgress(1000, 100, true);
            } else {
                notificationBuilder.setProgress(1000, attributes.progress, false);
            }
        }

        if (attributes.number != null) {
            notificationBuilder.setNumber(attributes.number);
        }

        if (attributes.localOnly != null) {
            notificationBuilder.setLocalOnly(attributes.localOnly);
        }
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if("default_rington".equals(attributes.sound)){
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        } else if(!attributes.sound.equals("default")) {
            int resId = context.getResources().getIdentifier(attributes.sound, "raw", context.getPackageName());
            soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + resId);
        }
        if (attributes.sound != null) {
            notificationBuilder.setSound(soundUri);
        }

        wakeScreen();

        return notificationBuilder.build();
    }

    public void wakeScreen() {
      PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
      boolean isScreenOn = pm.isScreenOn();
      Log.e("screen on.................................", ""+isScreenOn);
      if(isScreenOn==false)
      {
          WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"MyLock");
          wl.acquire(10000);
          WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyCpuLock");

          wl_cpu.acquire(10000);
      }
    }

    /**
     * Show the notification now.
     */
    public void show() {
        getSysNotificationManager().notify(subId, build());

        Log.i("ReactSystemNotification", "Notification Show: " + id);
    }

    /**
     * Setup alarm or show the notification.
     */
    public void setAlarmAndSaveOrShow() {
        if (attributes.delayed) {
            setDelay();
            saveAttributesToPreferences();

        } else if (attributes.scheduled) {
            setSchedule();
            saveAttributesToPreferences();

        } else {
            show();
        }
    }

    /**
     * Schedule the delayed notification.
     */
    public void setDelay() {
        PendingIntent pendingIntent = getScheduleNotificationIntent();

        long futureInMillis = SystemClock.elapsedRealtime() + attributes.delay;
        getAlarmManager().set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);

        Log.i("ReactSystemNotification", "Notification Delay Alarm Set: " + id + ", Repeat Type: " + attributes.repeatType + ", Current Time: " + System.currentTimeMillis() + ", Delay: " + attributes.delay);
    }

    /**
     * Schedule the notification.
     */
    public void setSchedule() {
        PendingIntent pendingIntent = getScheduleNotificationIntent();

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
        PendingIntent pendingIntent = getScheduleNotificationIntent();
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
        this.attributes = (NotificationAttributes) new Gson().fromJson(attributesJSONString, NotificationAttributes.class);

        Log.i("ReactSystemNotification", "Notification Loaded From Pref: " + id + ": " + attributesJSONString);
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

    private NotificationManager getSysNotificationManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private SharedPreferences getSharedPreferences () {
        return (SharedPreferences) context.getSharedPreferences(io.neson.react.notification.NotificationManager.PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    private PendingIntent getContentIntent() {
        Intent intent = new Intent(context, NotificationEventReceiver.class);

        intent.putExtra(NotificationEventReceiver.NOTIFICATION_ID, id);
        intent.putExtra(NotificationEventReceiver.ACTION, attributes.action);
        intent.putExtra(NotificationEventReceiver.PAYLOAD, attributes.payload);

        return PendingIntent.getBroadcast(context, subId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getScheduleNotificationIntent() {
        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, subId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }
}
