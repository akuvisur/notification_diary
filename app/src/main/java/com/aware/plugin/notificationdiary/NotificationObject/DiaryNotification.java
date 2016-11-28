package com.aware.plugin.notificationdiary.NotificationObject;

import android.content.ContentValues;

/**
 * Created by aku on 17/11/16.
 */
public class DiaryNotification {
    public Integer notification_id;
    public boolean labeled = false;

    // shared data for all notifications
    public Long generate_timestamp;
    public Long interaction_timestamp;
    public String interaction_type;
    public Boolean seen = false;
    public Long seen_timestamp;
    public String application_package;
    public String notification_category;

    // context (when interacting)
    public String location;
    public String activity;
    public String headphone_jack;
    public String screen_mode;
    public String ringer_mode;
    public String battery_level;
    public String network_availability;
    public String wifi_availability;
    public String foreground_application_package;

    // match grouped notifications
    public int getHashIdentifier() {
        return (String.valueOf(notification_id) + String.valueOf(application_package)).hashCode();
    }

    public static int getHashIdentifier(int id, String pkg) {
        return (String.valueOf(id) + String.valueOf(pkg)).hashCode();
    }

}
