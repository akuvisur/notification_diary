package com.aware.plugin.notificationdiary.NotificationContext;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.util.Log;

import com.aware.plugin.notificationdiary.NotificationListener;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;
import com.aware.providers.Applications_Provider;
import com.aware.providers.Battery_Provider;
import com.aware.providers.Screen_Provider;

import java.util.HashMap;

/**
 * Created by aku on 23/11/16.
 */
public class InteractionContext {
    private static final String TAG = "InteractionContext";

    public static final String RINGER_NORMAL = "RINGER_NORMAL";
    public static final String RINGER_SILENT = "RINGER_SILENT";
    public static final String RINGER_VIBRATE = "RINGER_VIBRATE";

    HashMap<String, String> c = new HashMap<>();

    public InteractionContext(Context context) {
        // TODO google activity api thing
        c.put(UnsyncedData.Notifications_Table.activity, NotificationListener.ACTIVITY);

        // TODO google geofences thing
        // https://dev.theneura.com/docs/guide/android/setup
        c.put(UnsyncedData.Notifications_Table.location, "");

        // WIFI AND NETWORK AVAILABILITY
        c.put(UnsyncedData.Notifications_Table.network_availability, NotificationListener.NETWORK_AVAILABLE);
        c.put(UnsyncedData.Notifications_Table.wifi_availability, NotificationListener.WIFI_AVAILABLE);

        // HEADPHONE JACK
        c.put(UnsyncedData.Notifications_Table.headphone_jack, NotificationListener.HEADSET_STATUS);

        // RINGER MODE
        switch (((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                c.put(UnsyncedData.Notifications_Table.ringer_mode, RINGER_NORMAL);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                c.put(UnsyncedData.Notifications_Table.ringer_mode, RINGER_SILENT);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                c.put(UnsyncedData.Notifications_Table.ringer_mode, RINGER_VIBRATE);
                break;
        }

        // BATTERY LEVEL
        Cursor battery_level = context.getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, null, null, "TIMESTAMP DESC LIMIT 1");
        if (battery_level != null && battery_level.moveToFirst()) {
            c.put(UnsyncedData.Notifications_Table.battery_level, battery_level.getString(battery_level.getColumnIndex(Battery_Provider.Battery_Data.LEVEL)));
            battery_level.close();
        }

        // SCREEN
        Cursor screen = context.getContentResolver().query(Screen_Provider.Screen_Data.CONTENT_URI, null, null, null, "TIMESTAMP DESC LIMIT 1");
        if (screen != null && screen.moveToFirst()) {
            c.put(UnsyncedData.Notifications_Table.screen_mode, screen.getString(screen.getColumnIndex(Screen_Provider.Screen_Data.SCREEN_STATUS)));
            screen.close();
        }


    }

    public ContentValues addToValues(ContentValues values) {
        for (String key : c.keySet()) {
            values.put(key, c.get(key));
        }
        Log.d(TAG, "context: " + values.toString());
        return values;
    }
}

