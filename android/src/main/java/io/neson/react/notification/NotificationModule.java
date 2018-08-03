package io.neson.react.notification;

import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.Activity;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.LifecycleEventListener;

import io.neson.react.notification.NotificationManager;
import io.neson.react.notification.Notification;
import io.neson.react.notification.NotificationAttributes;
import io.neson.react.notification.NotificationEventReceiver;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * The main React native module.
 *
 * Provides JS accessible API, bridge Java and JavaScript.
 */
public class NotificationModule extends ReactContextBaseJavaModule {
    final static String PREFERENCES_KEY = "ReactNativeSystemNotification";
    public Context mContext = null;
    private Intent mIntent;
    public NotificationManager mNotificationManager = null;
    public WakeupManager mWakeupManager = null;

    @Override
    public String getName() {
        return "NotificationModule";
    }

    /**
     * Constructor.
     */
    public NotificationModule(ReactApplicationContext reactContext,Intent intent) {
        super(reactContext);

        this.mContext = reactContext;
        mIntent = intent;
        this.mNotificationManager = (NotificationManager) new NotificationManager(reactContext);
        this.mWakeupManager = (WakeupManager) new WakeupManager(reactContext);

        listenNotificationEvent();
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        if (mIntent != null) {
            String wakeupId = mIntent.getStringExtra("wakeupId");
            Log.d("ReactSystemNotification", wakeupId);
            WakeupManager wakeupManager = new WakeupManager(mContext);
            Wakeup wakeup = wakeupManager.find(wakeupId);
            Log.d("ReactSystemNotification", wakeup.toString());

            String wakeupAttr ="";
            if (wakeup.getAttributes() != null) {

                try {
                    wakeupAttr = ReactNativeJson.convertMapToJson(wakeup.getAttributes().asReadableMap()).toString();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            constants.put("launchWakeup", wakeupAttr);
        }
        return constants;
    }

    /**
     * React method to create a wakeup
     */
    @ReactMethod
    public void createWakeup(
        String scheduleID,
        ReadableMap wakeupAttributes,
        Callback errorCallback,
        Callback successCallback
    ){
        try {
            WakeupAttributes a = getWakeupAttributesFromReadableMap(wakeupAttributes);
            Wakeup n = mWakeupManager.createOrUpdate(scheduleID, a);

            successCallback.invoke(n.getAttributes().asReadableMap());

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: create wakeup Error: " + Log.getStackTraceString(e));
        }
    }
    
    @ReactMethod
    public void stopService() {
        if (mIntent != null) {
            new android.os.Handler().postDelayed(new Runnable() {
                public void run() {
                    getReactApplicationContext().stopService(mIntent);
                }
            }, 1000);
        }
    }

    /**
     * React method to create or update a notification.
     */
    @ReactMethod
    public void rCreate(
        String notificationID,
        ReadableMap notificationAttributes,
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            NotificationAttributes a = getNotificationAttributesFromReadableMap(notificationAttributes);
            Notification n = mNotificationManager.createOrUpdate(notificationID, a);

            successCallback.invoke(n.getAttributes().asReadableMap());

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rCreate Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to get all notification ids.
     */
    @ReactMethod
    public void rGetIDs(
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            ArrayList<String> ids = mNotificationManager.getIDs();
            WritableArray rids = new WritableNativeArray();

            for (String id: ids) {
                rids.pushString(id);
            }

            successCallback.invoke((ReadableArray) rids);

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rGetIDs Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to get data of a notification.
     */
    @ReactMethod
    public void rFind(
        String notificationID,
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            Notification n = mNotificationManager.find(notificationID);
            successCallback.invoke(n.getAttributes().asReadableMap());

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rFind Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to delete (i.e. cancel a wakeup) wakeup.
     */
    @ReactMethod
    public void deleteWakeup(
            String wakeupID,
            Callback errorCallback,
            Callback successCallback
    ) {
        try {
            Wakeup n = mWakeupManager.delete(wakeupID);

            successCallback.invoke(n.getAttributes().asReadableMap());

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: deleteWakeup Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to delete (i.e. cancel a scheduled) notification.
     */
    @ReactMethod
    public void rDelete(
        String notificationID,
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            Notification n = mNotificationManager.delete(notificationID);

            successCallback.invoke(n.getAttributes().asReadableMap());

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rDelete Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to delete (i.e. cancel a scheduled) notification.
     */
    @ReactMethod
    public void deleteAllWakeup(
            Callback errorCallback,
            Callback successCallback
    ) {
        try {
            ArrayList<String> ids = mWakeupManager.getIDs();

            for (String id: ids) {
                try {
                    mWakeupManager.delete(id);
                } catch (Exception e) {
                    Log.e("ReactSystemNotification", "NotificationModule: deleteAllWakeup Error: " + Log.getStackTraceString(e));
                }
            }

            successCallback.invoke();

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: deleteAllWakeup Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to delete (i.e. cancel a scheduled) notification.
     */
    @ReactMethod
    public void rDeleteAll(
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            ArrayList<String> ids = mNotificationManager.getIDs();

            for (String id: ids) {
                try {
                    mNotificationManager.delete(id);
                } catch (Exception e) {
                    Log.e("ReactSystemNotification", "NotificationModule: rDeleteAll Error: " + Log.getStackTraceString(e));
                }
            }

            successCallback.invoke();

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rDeleteAll Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to clear a notification.
     */
    @ReactMethod
    public void rClear(
        String notificationID,
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            Notification n = mNotificationManager.clear(notificationID);

            successCallback.invoke(n.getAttributes().asReadableMap());

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rClear Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * React method to clear all notifications of this app.
     */
    @ReactMethod
    public void rClearAll(
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            mNotificationManager.clearAll();
            successCallback.invoke();

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rClearAll Error: " + Log.getStackTraceString(e));
        }
    }

    @ReactMethod
    public void rGetApplicationName(
        Callback errorCallback,
        Callback successCallback
    ) {
        try {
            int stringId = getReactApplicationContext().getApplicationInfo().labelRes;
            successCallback.invoke(getReactApplicationContext().getString(stringId));

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
            Log.e("ReactSystemNotification", "NotificationModule: rGetApplicationName Error: " + Log.getStackTraceString(e));
        }
    }

    /**
     * Emit JavaScript events.
     */
    private void sendEvent(
        String eventName,
        Object params
    ) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);

        Log.i("ReactSystemNotification", "NotificationModule: sendEvent (to JS): " + eventName);
    }

    @ReactMethod
    public void getInitialSysNotification(Callback cb) {
        final Activity activity = getCurrentActivity();

        if (activity == null) {
          return;
        }
        
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            String initialSysNotificationId = extras.getString("initialSysNotificationId");
            if (initialSysNotificationId != null) {
                cb.invoke(initialSysNotificationId, extras.getString("initialSysNotificationAction"), extras.getString("initialSysNotificationPayload"));
                return;
            }
        }
    }
    
    @ReactMethod
    public void removeInitialSysNotification() {
        final Activity activity = getCurrentActivity();

      if (activity == null) {
        return;
      }
      
      activity.getIntent().removeExtra("initialSysNotificationId");
      activity.getIntent().removeExtra("initialSysNotificationAction");
      activity.getIntent().removeExtra("initialSysNotificationPayload");
    }

    private NotificationAttributes getNotificationAttributesFromReadableMap(
        ReadableMap readableMap
    ) {
        NotificationAttributes notificationAttributes = new NotificationAttributes();

        notificationAttributes.loadFromReadableMap(readableMap);

        return notificationAttributes;
    }
    private WakeupAttributes getWakeupAttributesFromReadableMap(
            ReadableMap readableMap
    ) {
        WakeupAttributes wakeupAttributes = new WakeupAttributes();

        wakeupAttributes.loadFromReadableMap(readableMap);

        return wakeupAttributes;
    }

    private void listenNotificationEvent() {
        IntentFilter intentFilter = new IntentFilter("NotificationEvent");

        getReactApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle extras = intent.getExtras();

                WritableMap params = Arguments.createMap();
                params.putString("notificationID", extras.getString(NotificationEventReceiver.NOTIFICATION_ID));
                params.putString("action", extras.getString(NotificationEventReceiver.ACTION));
                params.putString("payload", extras.getString(NotificationEventReceiver.PAYLOAD));

                sendEvent("sysModuleNotificationClick", params);
            }
        }, intentFilter);
    }

}
