package com.aware.plugin.notificationdiary.Providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

/**
 * Created by denzil on 07/04/16.
 */
public class Provider extends ContentProvider {
    public static String TAG = "syncprovider";

    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.plugin.notificationdiary.provider.notificationdiary";

    public static final String DATABASE_NAME = "plugin_notificationdiary.db";

    public static final String DB_TBL_NOTIFICATIONS = "notificationdiary_notifications";
    public static final String DB_TBL_PREDICTIONS = "notificationdiary_predictions";

    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 5;

    public static final String[] DATABASE_TABLES = {
            DB_TBL_NOTIFICATIONS,
            DB_TBL_PREDICTIONS
    };


    public static final class Notifications_Data implements AWAREColumns {

        /**
         * Your ContentProvider table content URI.<br/>
         * The last segment needs to match your database table name
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_NOTIFICATIONS);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "vnd.android.cursor.dir/vnd.aware.plugin.notificationdiary";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +"vnd.android.cursor.item/vnd.aware.plugin.notificationdiary";

        public static final String notification_id = "notification_id";
        public static final String generate_timestamp = "generate_timestamp";
        public static final String interaction_timestamp = "interaction_timestamp";
        public static final String interaction_type = "interaction_type";
        public static final String seen_timestamp = "seen_timestamp";
        public static final String application_package = "application_package";
        public static final String notification_category = "notification_category";

        // context (when interacting)
        public static final String location = "location";
        public static final String activity = "activity";
        public static final String headphone_jack = "headphone_jack";
        public static final String screen_mode = "screen_mode";
        public static final String ringer_mode = "ringer_mode";
        public static final String battery_level = "battery_level";
        public static final String network_availability = "network_availability";
        public static final String wifi_availability = "wifi_availability";
        public static final String foreground_application_package = "foreground_application_package";

        // set to true once user skips or gives label
        public static final String synced = "synced";
        public static final String seen = "seen";

        public static final String labeled = "labeled";
        public static final String content_importance = "content_importance";
        public static final String timing = "timing";

        public static final String predicted_as_show = "predicted_as_show";
        public static final String prediction_correct = "prediction_correct";
    }

    public static final class Predictions_Data implements AWAREColumns {
        private Predictions_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_PREDICTIONS);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "vnd.android.cursor.dir/vnd.aware.plugin.notificationdiary";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "vnd.android.cursor.item/vnd.aware.plugin.notificationdiary";

        public static final String classifier_id = "classifier_id";
        public static final String generate_timestamp = "generate_timestamp";
        public static final String num_instances = "num_instances";
        public static final String accuracy = "accuracy";
        public static final String roc_area = "roc_area";
        public static final String show_false_positive = "show_false_positive";
        public static final String hide_false_positive = "hide_false_positive";
        public static final String kappa = "kappa";
        public static final String num_clusters = "num_clusters";
    }


    /**
     * Database table fields
     */
    public static final String TABLES_FIELDS_PREDICTIONS =
            Predictions_Data._ID + " integer primary key autoincrement," +
            Predictions_Data.TIMESTAMP + " real default 0," +
            Predictions_Data.DEVICE_ID + " text default ''," +
            Predictions_Data.classifier_id + " integer," +
            Predictions_Data.generate_timestamp + " real," +
            Predictions_Data.num_instances + " integer," +
            Predictions_Data.accuracy + " real," +
            Predictions_Data.roc_area + " real," +
            Predictions_Data.show_false_positive + " real," +
            Predictions_Data.hide_false_positive + " real," +
            Predictions_Data.kappa + " real," +
            Predictions_Data.num_clusters + " integer";

    public static final String TABLES_FIELDS_NOTIFICATIONS =
            Notifications_Data._ID + " integer primary key autoincrement," +
            Notifications_Data.TIMESTAMP + " real default 0," +
            Notifications_Data.DEVICE_ID + " text default ''," +
            Notifications_Data.notification_id + " TEXT," +
            Notifications_Data.generate_timestamp + " real," +
            Notifications_Data.interaction_timestamp + " real," +
            Notifications_Data.interaction_type + " TEXT," +
            Notifications_Data.seen_timestamp + " real," +
            Notifications_Data.application_package + " TEXT," +
            Notifications_Data.notification_category + " TEXT," +
            Notifications_Data.location + " TEXT," +
            Notifications_Data.activity + " TEXT," +
            Notifications_Data.headphone_jack + " TEXT," +
            Notifications_Data.screen_mode + " TEXT," +
            Notifications_Data.ringer_mode + " TEXT," +
            Notifications_Data.battery_level + " integer," +
            Notifications_Data.network_availability + " TEXT," +
            Notifications_Data.wifi_availability + " TEXT," +
            Notifications_Data.foreground_application_package + " TEXT," +
            Notifications_Data.synced + " TEXT," +
            // no = no, 1 = yes
            Notifications_Data.seen + " integer," +
            // 0 = no, 1 = yes, -1 = skipped
            Notifications_Data.labeled + " integer," +
            Notifications_Data.content_importance + " REAL," +
            Notifications_Data.timing + " REAL," +
            // prediction results
            Notifications_Data.predicted_as_show + " integer," +
            Notifications_Data.prediction_correct + " integer";

    public static final String[] TABLES_FIELDS = {
            TABLES_FIELDS_NOTIFICATIONS,
            TABLES_FIELDS_PREDICTIONS
    };

    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    private static UriMatcher sUriMatcher = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    //ContentProvider query indexes
    private static final int NOTIFS_DIR = 1;
    private static final int NOTIFS_ITEM = 2;
    private static final int PREDICTIONS_DIR = 3;
    private static final int PREDICTIONS_ITEM = 4;

    private static HashMap<String, String> notifsHash, predictionsHash;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        }
        if (databaseHelper != null && (database == null || !database.isOpen())) {
            database = databaseHelper.getWritableDatabase();
        }
        return (database != null && databaseHelper != null);
    }

    @Override
    public boolean onCreate() {

        AUTHORITY = getContext().getPackageName() + ".provider.notificationdiary";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], NOTIFS_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", NOTIFS_ITEM);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], PREDICTIONS_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", PREDICTIONS_ITEM);

        notifsHash = new HashMap<>();
        notifsHash.put(Notifications_Data._ID, Notifications_Data._ID);
        notifsHash.put(Notifications_Data.TIMESTAMP, Notifications_Data.TIMESTAMP);
        notifsHash.put(Notifications_Data.DEVICE_ID, Notifications_Data.DEVICE_ID);
        notifsHash.put(Notifications_Data.notification_id, Notifications_Data.notification_id);
        notifsHash.put(Notifications_Data.generate_timestamp, Notifications_Data.generate_timestamp);
        notifsHash.put(Notifications_Data.interaction_timestamp, Notifications_Data.interaction_timestamp);
        notifsHash.put(Notifications_Data.interaction_type, Notifications_Data.interaction_type);
        notifsHash.put(Notifications_Data.seen_timestamp, Notifications_Data.seen_timestamp);
        notifsHash.put(Notifications_Data.application_package, Notifications_Data.application_package);
        notifsHash.put(Notifications_Data.notification_category, Notifications_Data.notification_category);
        notifsHash.put(Notifications_Data.location, Notifications_Data.location);
        notifsHash.put(Notifications_Data.activity, Notifications_Data.activity);
        notifsHash.put(Notifications_Data.headphone_jack, Notifications_Data.headphone_jack);
        notifsHash.put(Notifications_Data.screen_mode, Notifications_Data.screen_mode);
        notifsHash.put(Notifications_Data.ringer_mode, Notifications_Data.ringer_mode);
        notifsHash.put(Notifications_Data.battery_level, Notifications_Data.battery_level);
        notifsHash.put(Notifications_Data.network_availability, Notifications_Data.network_availability);
        notifsHash.put(Notifications_Data.wifi_availability ,Notifications_Data.wifi_availability);
        notifsHash.put(Notifications_Data.foreground_application_package, Notifications_Data.foreground_application_package);
        notifsHash.put(Notifications_Data.synced, Notifications_Data.synced);
        notifsHash.put(Notifications_Data.seen, Notifications_Data.seen);
        notifsHash.put(Notifications_Data.labeled, Notifications_Data.labeled);
        notifsHash.put(Notifications_Data.content_importance, Notifications_Data.content_importance);
        notifsHash.put(Notifications_Data.timing, Notifications_Data.timing);
        notifsHash.put(Notifications_Data.predicted_as_show, Notifications_Data.predicted_as_show);
        notifsHash.put(Notifications_Data.prediction_correct, Notifications_Data.prediction_correct);

        predictionsHash = new HashMap<>();
        predictionsHash.put(Predictions_Data._ID, Predictions_Data._ID);
        predictionsHash.put(Predictions_Data.TIMESTAMP, Predictions_Data.TIMESTAMP);
        predictionsHash.put(Predictions_Data.DEVICE_ID, Predictions_Data.DEVICE_ID);
        predictionsHash.put(Predictions_Data.classifier_id, Predictions_Data.classifier_id);
        predictionsHash.put(Predictions_Data.generate_timestamp, Predictions_Data.generate_timestamp);
        predictionsHash.put(Predictions_Data.num_instances, Predictions_Data.num_instances);
        predictionsHash.put(Predictions_Data.accuracy, Predictions_Data.accuracy);
        predictionsHash.put(Predictions_Data.roc_area, Predictions_Data.roc_area);
        predictionsHash.put(Predictions_Data.show_false_positive, Predictions_Data.show_false_positive);
        predictionsHash.put(Predictions_Data.hide_false_positive, Predictions_Data.hide_false_positive);
        predictionsHash.put(Predictions_Data.kappa, Predictions_Data.kappa);
        predictionsHash.put(Predictions_Data.num_clusters, Predictions_Data.num_clusters);

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case NOTIFS_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(notifsHash);
                break;
            case PREDICTIONS_DIR:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(predictionsHash);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTIFS_DIR:
                return Notifications_Data.CONTENT_TYPE;
            case NOTIFS_ITEM:
                return Notifications_Data.CONTENT_ITEM_TYPE;
            case PREDICTIONS_DIR:
                return Predictions_Data.CONTENT_TYPE;
            case PREDICTIONS_ITEM:
                return Predictions_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return null;
        }
        Log.d(TAG, "Inserting new values to syncable database: " + uri.toString());
        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();
        long _id = 0;
        switch (sUriMatcher.match(uri)) {
            case NOTIFS_DIR:
                _id = database.insert(DATABASE_TABLES[0], Notifications_Data.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Notifications_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            case PREDICTIONS_DIR:
                _id = database.insert(DATABASE_TABLES[1], Predictions_Data.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Predictions_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return 0;
        }

        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTIFS_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case PREDICTIONS_DIR:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return 0;
        }

        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTIFS_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            case PREDICTIONS_DIR:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;
            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}