package com.aware.plugin.notificationdiary.Providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.google.fused_location.Provider;
import com.aware.plugin.notificationdiary.AppManagement;
import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;

import java.util.ArrayList;

import static com.aware.plugin.notificationdiary.ContentAnalysisService.EMPTY_VALUE;

/**
 * Created by aku on 18/11/16.
 */
public class UnsyncedData extends SQLiteOpenHelper {
    private static final String TAG = "UnsyncedData.class";

    private static final int DATABASE_VERSION = 11;
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

        public static String predicted_as_show = "predicted_as_show";
        public static String prediction_correct = "prediction_correct";
    }

    //Define each database table fields
    private static final String NOTIFICATIONS_CREATE_TABLE =
            "CREATE TABLE " + DATABASE_NAME + "  (" +
            Notifications_Table._ID + " integer primary key autoincrement," +
            Notifications_Table.DEVICE_ID + " REAL, " +
            Notifications_Table.TIMESTAMP + " REAL, " +
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
            Notifications_Table.message + " TEXT, " +

            // prediction results
            Notifications_Table.predicted_as_show + " integer, " +
            Notifications_Table.prediction_correct + " integer);";

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

    public UnsyncedNotification get(long id, boolean closeAfter) {
        init();
        UnsyncedNotification result = new UnsyncedNotification();
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                Notifications_Table._ID + " =?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (cursor.moveToFirst()) {
            result.sqlite_row_id = id;
            result.notification_id = cursor.getInt(cursor.getColumnIndex(Notifications_Table.notification_id));
            result.generate_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.generate_timestamp));
            result.interaction_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.interaction_timestamp));
            result.interaction_type = cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type));
            result.seen_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.seen_timestamp));
            result.application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package));
            result.notification_category = cursor.getString(cursor.getColumnIndex(Notifications_Table.notification_category));
            result.location = cursor.getString(cursor.getColumnIndex(Notifications_Table.location));
            result.activity = cursor.getString(cursor.getColumnIndex(Notifications_Table.activity));
            result.headphone_jack = cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack));
            result.screen_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.screen_mode));
            result.ringer_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.ringer_mode));
            result.battery_level = cursor.getString(cursor.getColumnIndex(Notifications_Table.battery_level));
            result.network_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.network_availability));
            result.wifi_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.wifi_availability));
            result.foreground_application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package));

            result.synced = cursor.getInt(cursor.getColumnIndex(Notifications_Table.synced)) > 0;
            result.seen = cursor.getInt(cursor.getColumnIndex(Notifications_Table.seen)) > 0;

            result.labeled = cursor.getInt(cursor.getColumnIndex(Notifications_Table.location)) > 0;
            result.content_importance_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.content_importance));
            result.timing_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.timing));

            result.predicted_as_show = cursor.getInt(cursor.getColumnIndex(Notifications_Table.predicted_as_show));
            result.prediction_correct = cursor.getInt(cursor.getColumnIndex(Notifications_Table.prediction_correct));
        }
        if (closeAfter) database.close();
        return result;
    }

    public long insertRecord(Context c, ContentValues values) {
        init();
        values.put(Notifications_Table.labeled, 0);
        values.put(Notifications_Table.TIMESTAMP, System.currentTimeMillis());
        values.put(Notifications_Table.DEVICE_ID, Aware.getSetting(c, Aware_Preferences.DEVICE_ID));
        long id = database.insert(DATABASE_NAME, null, values);
        database.close();
        return id;
    }

    public void updateEntry(int id, ContentValues updated_values, boolean closeAfter) {
        init();
        Log.d(TAG, "updating..");
        database.update(DATABASE_NAME, updated_values, "_id=" + id, null);
        Log.d(TAG, "updated id " + id);
        Log.d(TAG, "values: " + updated_values.toString());
        if (closeAfter) database.close();
    }

    public ArrayList<UnsyncedNotification> getUnlabeledNotifications(boolean closeAfter) {
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
        if (closeAfter) database.close();
        return result;
    }


    public ArrayList<UnsyncedNotification> getLabeledNotifications() {
        init();
        ArrayList<UnsyncedNotification> result = new ArrayList<>();

        // getString all that are EITHER 1) labeled and dismissed or 2) clicked
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                UnsyncedData.Notifications_Table.seen + "=? AND ((" + Notifications_Table.labeled + "=? AND " +
                        Notifications_Table.interaction_type + "=?) OR " + Notifications_Table.interaction_type + "=?) OR "
                + Notifications_Table.prediction_correct + " > -1",
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
                u.message = cursor.getString(cursor.getColumnIndex(Notifications_Table.message)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.message));
                u.title = cursor.getString(cursor.getColumnIndex(Notifications_Table.title)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.title));
                u.application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package))  == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package));
                u.notification_category = cursor.getString(cursor.getColumnIndex(Notifications_Table.notification_category)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.notification_category));
                u.interaction_type = cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type));
                u.activity = cursor.getString(cursor.getColumnIndex(Notifications_Table.activity)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.activity));
                u.battery_level = cursor.getString(cursor.getColumnIndex(Notifications_Table.battery_level)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.battery_level));
                u.foreground_application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package));
                u.location = cursor.getString(cursor.getColumnIndex(Notifications_Table.location)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.location));
                u.wifi_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.wifi_availability)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.wifi_availability));
                u.network_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.network_availability)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.network_availability));
                u.location = cursor.getString(cursor.getColumnIndex(Notifications_Table.location)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.location));
                u.screen_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.screen_mode)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.screen_mode));
                u.ringer_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.ringer_mode)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.ringer_mode));
                u.headphone_jack = cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack));;
                u.content_importance_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.content_importance));
                u.timing_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.timing));
                u.predicted_as_show = cursor.getString(cursor.getColumnIndex(Notifications_Table.predicted_as_show)) == null ? -1 : cursor.getInt(cursor.getColumnIndex(Notifications_Table.predicted_as_show));
                u.prediction_correct = cursor.getString(cursor.getColumnIndex(Notifications_Table.prediction_correct)) == null ? -1 : cursor.getInt(cursor.getColumnIndex(Notifications_Table.prediction_correct));
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

    public ArrayList<Prediction> getPredictions() {
        init();
        ArrayList<Prediction> result = new ArrayList<>();
        Cursor cursor = database.query(DATABASE_NAME,
                new String[]{Notifications_Table.title, Notifications_Table.message, Notifications_Table.application_package, Notifications_Table._ID, Notifications_Table.seen_timestamp, Notifications_Table.predicted_as_show},
                Notifications_Table.predicted_as_show + " > -1 AND " + Notifications_Table.prediction_correct + " IS NULL",
                null, null, null, Notifications_Table.generate_timestamp + " DESC");

        if (cursor != null) {
            rowiteration:
            while (cursor.moveToNext()) {
                Prediction p = new Prediction(
                        cursor.getString(cursor.getColumnIndex(Notifications_Table.message)),
                        cursor.getString(cursor.getColumnIndex(Notifications_Table.title)),
                        cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package)),
                        cursor.getInt(cursor.getColumnIndex(Notifications_Table._ID)),
                        cursor.getLong(cursor.getColumnIndex(Notifications_Table.seen_timestamp)),
                        cursor.getInt(cursor.getColumnIndex(Notifications_Table.predicted_as_show))
                );
                for (Prediction inserted : result) {
                    if (p.equals(inserted)) {
                        ContentValues c = new ContentValues();
                        c.put(Notifications_Table.prediction_correct, -1);
                        updateEntry(cursor.getInt(cursor.getColumnIndex(Notifications_Table._ID)), c, false);
                        continue rowiteration;
                    }
                }
                // max size 40
                if (result.size() <= 40) result.add(p);
            }
            cursor.close();
        }

        return result;
    }

    public class Prediction {
        public String text;
        public String title;
        public String package_name;
        public int sqlite_id;
        public long timestamp;
        public int predicted_as_show;

        Prediction(String text, String title, String package_name, int sqlite_id, long timestamp, int predicted_as_show) {
            this.text = text;
            this.title = title;
            this.package_name = package_name;
            this.sqlite_id = sqlite_id;
            this.timestamp = timestamp;
            this.predicted_as_show = predicted_as_show;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Prediction)) return false;
            Prediction p = (Prediction) o;
            return (
                p.title != null && p.title.equals(this.title) &&
                p.text != null && p.text.equals(this.text) &&
                p.package_name != null && p.package_name.equals(this.package_name)
            );
        }
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
