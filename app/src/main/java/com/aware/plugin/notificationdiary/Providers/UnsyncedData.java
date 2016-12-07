package com.aware.plugin.notificationdiary.Providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.aware.plugin.notificationdiary.AppManagement;
import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;
import com.aware.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by aku on 18/11/16.
 */
public class UnsyncedData extends SQLiteOpenHelper {
    private static final String TAG = "UnsyncedData.class";

    private static final int DATABASE_VERSION = 8;
    private static final String DATABASE_NAME = "unsynced_notifications";

    public static final class Notifications_Table implements Provider.AWAREColumns {

        //Note: integers and strings don't need a type prefix_
        public static String notification_id = "notification_id";
        public static String generate_timestamp = "generate_timestamp";
        public static String interaction_timestamp = "interaction_timestamp";
        public static String interaction_type = "interaction_type";
        public static String seen_timestamp = "seen_timestamp";
        public static String application_package = "application_package";
        public static String notification_category = "notification_category";

        // context (when interacting)
        public static String location = "location";
        public static String activity = "activity";
        public static String headphone_jack = "headphone_jack";
        public static String screen_mode = "screen_mode";
        public static String ringer_mode = "ringer_mode";
        public static String battery_level = "battery_level";
        public static String network_availability = "network_availability";
        public static String wifi_availability = "wifi_availability";
        public static String foreground_application_package = "foreground_application_package";

        // set to true once user skips or gives label
        public static String synced = "synced";
        public static String seen = "seen";

        public static String labeled = "labeled";
        public static String content_importance = "content_importance";
        public static String timing = "timing";

        public static String title = "title";
        public static String message = "message";
    }

    //Define each database table fields
    private static final String NOTIFICATIONS_CREATE_TABLE =
            "CREATE TABLE " + DATABASE_NAME + "  (" +
            Notifications_Table._ID + " integer primary key autoincrement," +
            Notifications_Table.notification_id + " TEXT, " +
            Notifications_Table.generate_timestamp + " real, " +
            Notifications_Table.interaction_timestamp + " real, " +
            Notifications_Table.interaction_type + " TEXT, " +
            Notifications_Table.seen_timestamp + " real, " +
            Notifications_Table.application_package + " TEXT, " +
            Notifications_Table.notification_category + " TEXT, " +
            Notifications_Table.location + " TEXT, " +
            Notifications_Table.activity + " TEXT, " +
            Notifications_Table.headphone_jack + " TEXT, " +
            Notifications_Table.screen_mode + " TEXT, " +
            Notifications_Table.ringer_mode + " TEXT, " +
            Notifications_Table.battery_level + " integer, " +
            Notifications_Table.network_availability + " TEXT, " +
            Notifications_Table.wifi_availability + " TEXT, " +
            Notifications_Table.foreground_application_package + " TEXT, " +
            Notifications_Table.synced + " TEXT, " +

            // no = no, 1 = yes
            Notifications_Table.seen + " integer," +

            // 0 = no, 1 = yes, -1 = skipped
            Notifications_Table.labeled + " integer, " +
            Notifications_Table.content_importance + " REAL, " +
            Notifications_Table.timing + " REAL, " +
            Notifications_Table.title + " TEXT, " +
            Notifications_Table.message + " TEXT);";

    public UnsyncedData(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "Creating");
        sqLiteDatabase.execSQL(NOTIFICATIONS_CREATE_TABLE);
        Log.d(TAG, "Created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.d(TAG, "updating");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        onCreate(sqLiteDatabase);
    }

    SQLiteDatabase database;
    public void init() {
        if (database == null) {database = this.getWritableDatabase();}
    }

    public long insertRecord(ContentValues values) {
        init();
        values.put(Notifications_Table.labeled, 0);
        long id = database.insert(DATABASE_NAME, null, values);
        database.close();
        return id;
    }

    public void updateEntry(int id, ContentValues updated_values) {
        init();
        Log.d(TAG, "updating..");
        database.update(DATABASE_NAME, updated_values, "_id=" + id, null);
        Log.d(TAG, "updated id " + id);
        Log.d(TAG, "values: " + updated_values.toString());
        database.close();
    }

    public ArrayList<UnsyncedNotification> getUnlabeledNotifications() {
        init();
        ArrayList<UnsyncedNotification> result = new ArrayList<>();

        Cursor cursor = database.query(DATABASE_NAME,
                null,
                UnsyncedData.Notifications_Table.seen + "=? AND " + Notifications_Table.labeled + "=? AND " + Notifications_Table.interaction_type + "=?",
                new String[]{"1", "0", AppManagement.INTERACTION_TYPE_DISMISS},
                null, null,
                UnsyncedData.Notifications_Table.interaction_timestamp + " ASC");

        if (cursor != null) {
            if (!cursor.moveToFirst()) Log.d(TAG, "empty cursor");
            while (cursor.moveToNext()) {
                UnsyncedNotification u = new UnsyncedNotification();
                u.message = cursor.getString(cursor.getColumnIndex(UnsyncedData.Notifications_Table.message));
                u.title = cursor.getString(cursor.getColumnIndex(UnsyncedData.Notifications_Table.title));
                u.seen_timestamp = cursor.getLong(cursor.getColumnIndex(UnsyncedData.Notifications_Table.seen_timestamp));
                u.application_package = cursor.getString(cursor.getColumnIndex(UnsyncedData.Notifications_Table.application_package));
                u.sqlite_row_id = cursor.getInt(cursor.getColumnIndex(Notifications_Table._ID));
                result.add(u);
            }
            cursor.close();
        }
        else {
            Log.d(TAG, "cursor was null");
        }
        Log.d(TAG, "done: " + result.size());
        database.close();
        return result;
    }


    public ArrayList<UnsyncedNotification> getLabeledNotifications() {
        init();
        ArrayList<UnsyncedNotification> result = new ArrayList<>();

        // getString all that are EITHER 1) labeled and dismissed or 2) clicked
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                UnsyncedData.Notifications_Table.seen + "=? AND ((" + Notifications_Table.labeled + "=? AND " +
                        Notifications_Table.interaction_type + "=?) OR " + Notifications_Table.interaction_type + "=?)",
                new String[]{"1", "1", AppManagement.INTERACTION_TYPE_DISMISS, AppManagement.INTERACTION_TYPE_CLICK},
                null, null,
                UnsyncedData.Notifications_Table.interaction_timestamp + " ASC");

        if (cursor != null) {
            if (!cursor.moveToFirst()) Log.d(TAG, "empty cursor");
            while (cursor.moveToNext()) {
                UnsyncedNotification u = new UnsyncedNotification();
                u.seen_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.seen_timestamp));
                u.generate_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.generate_timestamp));
                u.interaction_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.interaction_timestamp));
                u.message = cursor.getString(cursor.getColumnIndex(Notifications_Table.message)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.message));
                u.title = cursor.getString(cursor.getColumnIndex(Notifications_Table.title)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.title));
                u.application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package))  == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package));
                u.notification_category = cursor.getString(cursor.getColumnIndex(Notifications_Table.notification_category)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.notification_category));
                u.interaction_type = cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type));
                u.activity = cursor.getString(cursor.getColumnIndex(Notifications_Table.activity)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.activity));
                u.battery_level = cursor.getString(cursor.getColumnIndex(Notifications_Table.battery_level)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.battery_level));
                u.foreground_application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package));
                u.location = cursor.getString(cursor.getColumnIndex(Notifications_Table.location)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.location));
                u.wifi_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.wifi_availability)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.wifi_availability));
                u.network_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.network_availability)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.network_availability));
                u.location = cursor.getString(cursor.getColumnIndex(Notifications_Table.location)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.location));
                u.screen_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.screen_mode)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.screen_mode));
                u.ringer_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.ringer_mode)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.ringer_mode));
                u.headphone_jack = cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack)) == null ? "null" : cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack));;
                u.content_importance_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.content_importance));
                u.timing_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.timing));
                result.add(u);
            }
            cursor.close();
        }
        else {
            Log.d(TAG, "cursor was null");
        }
        Log.d(TAG, "done: " + result.size());
        database.close();
        return result;
    }


    public int getNumOfTrainingData() {
        init();
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                Notifications_Table.labeled + "=?",
                new String[]{"1"},
                null, null,
                null);
        int count = 0;
        if (cursor != null) {while (cursor.moveToNext()) {count++;} cursor.close();}
        return count;
    }

    public ArrayList<NotificationText> getAllNotificationText() {
        init();
        ArrayList<NotificationText> result = new ArrayList<>();
        Cursor cursor = database.query(DATABASE_NAME,
                new String[]{Notifications_Table.title, Notifications_Table.message},
                Notifications_Table.interaction_type + "=? OR " + Notifications_Table.interaction_type + "=?",
                new String[]{AppManagement.INTERACTION_TYPE_CLICK, AppManagement.INTERACTION_TYPE_DISMISS},
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.add(new NotificationText(
                    cursor.getString(cursor.getColumnIndex(Notifications_Table.title)).toLowerCase().replaceAll("^[a-zA-Z0-9äöüÄÖÜ]", " "),
                    cursor.getString(cursor.getColumnIndex(Notifications_Table.message)).toLowerCase().replaceAll("^[a-zA-Z0-9äöüÄÖÜ]", " ")
                ));
            }
            cursor.close();
        }

        return result;
    }

    public class NotificationText {
        public String title;
        public String contents;
        public NotificationText(String title, String contents) {

            this.title = title;
            this.contents = contents;
        }

    }

}
