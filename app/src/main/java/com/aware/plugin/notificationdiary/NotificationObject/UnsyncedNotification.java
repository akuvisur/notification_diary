package com.aware.plugin.notificationdiary.NotificationObject;

/**
 * Created by aku on 17/11/16.
 */
public class UnsyncedNotification extends DiaryNotification {
    // notification contents only included in non-synced notifications for privacy
    public String title;
    public String message;
    //
    public Boolean synced = false;

    public long sqlite_row_id;

    public UnsyncedNotification() {}

}
