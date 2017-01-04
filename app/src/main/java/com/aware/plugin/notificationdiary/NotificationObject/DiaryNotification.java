package com.aware.plugin.notificationdiary.NotificationObject;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.notificationdiary.Providers.Provider;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by aku on 17/11/16.
 */
public class DiaryNotification {
    private final String TAG = "DiaryNotification";

    public static final String SEEN_DELAY_SECONDS = "seen_delay_seconds";
    public static final String INTERACTION_DELAY_SECONDS = "interaction_delay_seconds";

    public String DEVICE_ID;
    public Long timestamp;

    public Integer notification_id;
    public Boolean labeled = false;

    // shared data for all notifications
    public Long generate_timestamp;
    public Long interaction_timestamp;
    public String interaction_type;
    public Boolean seen = false;
    public Long seen_timestamp;

    // context (when interacting)
    public String application_package;
    public String notification_category;
    public String location;
    public String activity;
    public String headphone_jack;
    public String screen_mode;
    public String ringer_mode;
    public String battery_level;
    public String network_availability;
    public String wifi_availability;
    public String foreground_application_package;

    // user labeling
    public Double timing_value = -1.0;
    public Double content_importance_value = -1.0;

    // predictions
    public Integer predicted_as_show = -1;
    public Integer prediction_correct = -1;

    // context variables and types
    public static final String CONTEXT_VARIABLE_TYPE_STRING = "CONTEXT_VARIABLE_TYPE_STRING";
    public static final String CONTEXT_VARIABLE_TYPE_DOUBLE = "CONTEXT_VARIABLE_TYPE_DOUBLE";

    public static final AttributeWithType attribute_application_package = new AttributeWithType(UnsyncedData.Notifications_Table.application_package, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_notification_category = new AttributeWithType(UnsyncedData.Notifications_Table.notification_category, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_location = new AttributeWithType(UnsyncedData.Notifications_Table.location, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_activity = new AttributeWithType(UnsyncedData.Notifications_Table.activity, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_headphone_jack = new AttributeWithType(UnsyncedData.Notifications_Table.headphone_jack, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_screen_mode = new AttributeWithType(UnsyncedData.Notifications_Table.screen_mode, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_ringer_mode = new AttributeWithType(UnsyncedData.Notifications_Table.ringer_mode, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_battery_level= new AttributeWithType(UnsyncedData.Notifications_Table.battery_level, CONTEXT_VARIABLE_TYPE_DOUBLE);
    public static final AttributeWithType attribute_network = new AttributeWithType(UnsyncedData.Notifications_Table.network_availability, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_wifi = new AttributeWithType(UnsyncedData.Notifications_Table.wifi_availability, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_foreground_app = new AttributeWithType(UnsyncedData.Notifications_Table.foreground_application_package, CONTEXT_VARIABLE_TYPE_STRING);
    public static final AttributeWithType attribute_seen_delay = new AttributeWithType(SEEN_DELAY_SECONDS, CONTEXT_VARIABLE_TYPE_DOUBLE);
    public static final AttributeWithType attribute_interaction_delay = new AttributeWithType(INTERACTION_DELAY_SECONDS, CONTEXT_VARIABLE_TYPE_DOUBLE);

    private static final ArrayList<AttributeWithType> CONTEXT_VARIABLES = new ArrayList<>(Arrays.asList(
        attribute_application_package,
        attribute_notification_category,
        attribute_location,
        attribute_activity,
        attribute_headphone_jack,
        attribute_screen_mode,
        attribute_ringer_mode,
        attribute_battery_level,
        attribute_network,
        attribute_wifi,
        attribute_foreground_app,
        attribute_seen_delay,
        attribute_interaction_delay
    ));
    public static final int CONTEXT_ATTRIBUTE_COUNT = CONTEXT_VARIABLES.size();

    // match grouped notifications
    public int getHashIdentifier() {
        return (String.valueOf(notification_id) + String.valueOf(application_package)).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DiaryNotification) {
            return getHashIdentifier() == ((DiaryNotification) o).getHashIdentifier();
        }
        return false;
    }

    public static int getHashIdentifier(int id, String pkg) {
        return (String.valueOf(id) + String.valueOf(pkg)).hashCode();
    }

    public static ArrayList<AttributeWithType> getContextVariables() {
        return CONTEXT_VARIABLES;
    }

    public int getSeenDelaySeconds() {
        return ((int) (seen_timestamp-generate_timestamp)/1000);
    }

    public int getInteractionDelaySeconds() {
        return ((int) (interaction_timestamp-seen_timestamp)/1000);
    }

    public String getString(int i) {
        if (i < 0 | i > CONTEXT_ATTRIBUTE_COUNT) return "NA";
        if (!(CONTEXT_VARIABLES.get(i).type.equals(CONTEXT_VARIABLE_TYPE_STRING))) return "NA";
        switch(i) {
            case 0:
                return application_package;
            case 1:
                return notification_category;
            case 2:
                return location;
            case 3:
                return activity;
            case 4:
                return headphone_jack;
            case 5:
                return screen_mode;
            case 6:
                return ringer_mode;
            case 7:
                return battery_level;
            case 8:
                return network_availability;
            case 9:
                return wifi_availability;
            case 10:
                return foreground_application_package;
            case 11:
                return String.valueOf(getSeenDelaySeconds());
            case 12:
                return String.valueOf(getInteractionDelaySeconds());
            default:
                return "NA";
        }
    }

    public String getString(String s) {
        for (int i = 0; i < CONTEXT_ATTRIBUTE_COUNT; i++) {
            if (CONTEXT_VARIABLES.get(i).name.equals(s)) return getString(i);
        }
        return "NA";
    }

    public Double getDouble(String s) {
        try {
            if (s.equals(UnsyncedData.Notifications_Table.battery_level))
                return Double.valueOf(battery_level);
            else if (s.equals(SEEN_DELAY_SECONDS)) return (double) getSeenDelaySeconds();
            else if (s.equals(INTERACTION_DELAY_SECONDS))
                return (double) getInteractionDelaySeconds();
        }
        catch (NumberFormatException e) {
            return -1.0;
        }
        return -1.0;
    }

    public ContentValues toSyncableContentValues(Context c) {
        ContentValues result = new ContentValues();

        // required for syncing
        result.put(Provider.Notifications_Data.TIMESTAMP, System.currentTimeMillis());
        result.put(Provider.Notifications_Data.DEVICE_ID, Aware.getSetting(c, Aware_Preferences.DEVICE_ID));

        if (notification_id != null) result.put(Provider.Notifications_Data.notification_id, notification_id);
        result.put(Provider.Notifications_Data.labeled, labeled);

        // shared data for all notifications
        if (generate_timestamp != null) result.put(Provider.Notifications_Data.generate_timestamp, generate_timestamp);
        if (interaction_timestamp != null) result.put(Provider.Notifications_Data.interaction_timestamp, interaction_timestamp);

        if (interaction_type != null) result.put(Provider.Notifications_Data.interaction_type, interaction_type);
        if (seen != null) result.put(Provider.Notifications_Data.seen, seen);
        if (seen_timestamp != null) result.put(Provider.Notifications_Data.seen_timestamp, seen_timestamp);

        // context (when interacting)
        if (application_package != null) result.put(Provider.Notifications_Data.application_package, application_package);
        if (notification_category != null) result.put(Provider.Notifications_Data.notification_category, notification_category);
        if (location != null) result.put(Provider.Notifications_Data.location, location);
        if (activity != null) result.put(Provider.Notifications_Data.activity, activity);
        if (headphone_jack != null) result.put(Provider.Notifications_Data.headphone_jack, headphone_jack);
        if (screen_mode != null) result.put(Provider.Notifications_Data.screen_mode, screen_mode);
        if (ringer_mode != null) result.put(Provider.Notifications_Data.ringer_mode, ringer_mode);
        if (battery_level != null) result.put(Provider.Notifications_Data.battery_level, battery_level);
        if (network_availability != null) result.put(Provider.Notifications_Data.network_availability, network_availability);
        if (wifi_availability != null) result.put(Provider.Notifications_Data.wifi_availability, wifi_availability);
        if (foreground_application_package != null) result.put(Provider.Notifications_Data.foreground_application_package, foreground_application_package);

        // user labeling
        if (labeled) result.put(Provider.Notifications_Data.labeled, 1); else result.put(Provider.Notifications_Data.labeled, 0);
        if (timing_value != null) result.put(Provider.Notifications_Data.timing, timing_value);
        if (content_importance_value != null) result.put(Provider.Notifications_Data.content_importance, content_importance_value);
        // predictions
        if (predicted_as_show != null) result.put(Provider.Notifications_Data.predicted_as_show, predicted_as_show);
        if (prediction_correct != null) result.put(Provider.Notifications_Data.prediction_correct, prediction_correct);

        return result;
    }

    @Override
    public String toString() {
        return "id: " + notification_id + " package: " + application_package + " labeled: " + labeled;
    }
}
