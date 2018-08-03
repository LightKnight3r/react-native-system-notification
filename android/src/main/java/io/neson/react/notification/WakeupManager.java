package io.neson.react.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

/**
 * A high level wakeup manager
 *
 * Warps the system wakeup API to make managing direct and scheduled
 * wakeup easy.
 */
public class WakeupManager {
    final static String PREFERENCES_KEY = "ReactNativeSystemWakeup";
    public Context context = null;
    public SharedPreferences sharedPreferences = null;

    /**
     * Constructor.
     */
    public WakeupManager(Context context) {
        this.context = context;
        this.sharedPreferences = (SharedPreferences) context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Create a wakeup.
     */
    public Wakeup create(
            String wakeupID,
            WakeupAttributes wakeupAttributes
    ) {
        Wakeup wakeup = new Wakeup(context, wakeupID, wakeupAttributes);

        wakeup.create();

        return wakeup;
    }

    /**
     * Create or update (if exists) a wakeup.
     */
    public Wakeup createOrUpdate(
            String wakeupID,
            WakeupAttributes wakeupAttributes
    ) {
        if (getIDs().contains(wakeupID)) {
            Wakeup wakeup = find(wakeupID);

            wakeup.update(wakeupAttributes);
            return wakeup;

        } else {
            return create(wakeupID, wakeupAttributes);
        }
    }

    /**
     * Get all wakeup ids.
     */
    public ArrayList<String> getIDs() {
        Set<String> keys = sharedPreferences.getAll().keySet();
        ArrayList<String> ids = new ArrayList<String>();

        for (String key : keys) {
            try {
                ids.add(key);
                // TODO: Delete out-dated wakeups BTW
            } catch (Exception e) {
                Log.e("ReactSystemWakeup", "WakeupManager: getIDs Error: " + Log.getStackTraceString(e));
            }
        }

        return ids;
    }

    /**
     * Get a wakeup by its id.
     */
    public Wakeup find(String wakeupID) {
        Wakeup wakeup = new Wakeup(context, wakeupID, null);

        if (wakeup.getAttributes() == null) wakeup.loadAttributesFromPreferences();

        return wakeup;
    }

    /**
     * Delete a wakeup by its id.
     */
    public Wakeup delete(String wakeupID) {
        return find(wakeupID).delete();
    }

    /**
     * Clear a wakeup by its id.
     */
//    public Wakeup clear(String wakeupID) {
//        return find(wakeupID).clear();
//    }

    /**
     * Clear all wakeups.
     */
    public void clearAll() {
//        android.app.WakeupManager systemWakeupManager = (android.app.WakeupManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        systemWakeupManager.cancelAll();
    }
}
