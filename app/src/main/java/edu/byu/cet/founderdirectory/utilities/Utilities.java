package edu.byu.cet.founderdirectory.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

/**
 * Created by Liddle on 3/29/16.
 */
public class Utilities {
    /**
     * Key for device ID shared preference.
     */
    private static final String DEVICE_ID_KEY = "deviceId";

    public static String getDeviceId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.contains(DEVICE_ID_KEY)) {
            return prefs.getString(DEVICE_ID_KEY, "");
        }

        // Jordan shared this approach, which has the benefit of simplicity.  KISS
        String id = UUID.randomUUID().toString();
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(DEVICE_ID_KEY, id);
        editor.commit();

        return id;
    }


}
