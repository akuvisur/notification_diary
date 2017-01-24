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
import com.aware.plugin.notificationdiary.R;
import com.aware.plugin.notificationdiary.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.aware.plugin.notificationdiary.ContentAnalysisService.EMPTY_VALUE;

/**
 * Created by aku on 18/11/16.
 */
public class UnsyncedData extends SQLiteOpenHelper {
    private static final String TAG = "UnsyncedData.class";

    private static final int DATABASE_VERSION = 13;
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
            Notifications_Table.synced + " integer default 0, " +

            // no = no, 1 = yes
            Notifications_Table.seen + " integer default 0," +

            // 0 = no, 1 = yes, -1 = skipped
            Notifications_Table.labeled + " integer default 0, " +
            Notifications_Table.content_importance + " REAL, " +
            Notifications_Table.timing + " REAL, " +
            Notifications_Table.title + " TEXT, " +
            Notifications_Table.message + " TEXT, " +

            // prediction results
            Notifications_Table.predicted_as_show + " integer default -1, " +
            Notifications_Table.prediction_correct + " integer default -1);";

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

    SQLiteDatabase database = null;
    public void init() {
        if( database != null ) {
            if( !database.isOpen() ) {
                database = null;
                database = this.getWritableDatabase();
            }
        }
        else {
            database = this.getWritableDatabase();
        }
    }

    public synchronized UnsyncedNotification get(long id) {
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
        return result;
    }

    public long insertRecord(Context c, ContentValues values) {
        init();
        Log.d(TAG, "inserting new with id: " + values.getAsString(Notifications_Table.notification_id));
        values.put(Notifications_Table.labeled, 0);
        values.put(Notifications_Table.TIMESTAMP, System.currentTimeMillis());
        values.put(Notifications_Table.DEVICE_ID, Aware.getSetting(c, Aware_Preferences.DEVICE_ID));
        if (values.containsKey(Notifications_Table.seen_timestamp) && values.getAsLong(Notifications_Table.seen_timestamp) != null && values.getAsLong(Notifications_Table.seen_timestamp) > 0.0) values.put(Notifications_Table.seen, 1); else values.put(Notifications_Table.seen, 0);
        long id = database.insert(DATABASE_NAME, null, values);
        return id;
    }

    public synchronized void updateEntry(final Context c, int id, ContentValues updated_values) {
        init();
        database.update(DATABASE_NAME, updated_values, Notifications_Table._ID + "=? " , new String[]{String.valueOf(id)});
        if (!syncing && ((System.currentTimeMillis() - AppManagement.getSyncTimestamp(c) > AppManagement.SYNC_DELAY))) syncAlltoProvider(c);
    }

    public synchronized ArrayList<UnsyncedNotification> getUnlabeledNotifications() {
        init();
        ArrayList<UnsyncedNotification> result = new ArrayList<>();
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                "(" + UnsyncedData.Notifications_Table.labeled + "=? AND " + UnsyncedData.Notifications_Table.interaction_type + "=?) OR (" +
                Notifications_Table.interaction_type + "=? AND " + Notifications_Table.labeled + " =?) OR (" +
                Notifications_Table.interaction_type + "=? AND " + Notifications_Table.labeled + " =?)",
                new String[]{"0", AppManagement.INTERACTION_TYPE_DISMISS, AppManagement.INTERACTION_TYPE_REPLACE, "0", AppManagement.INTERACTION_TYPE_CLICK, "0"},
                null, null,
                UnsyncedData.Notifications_Table.interaction_timestamp + " ASC");
        // dont ask twice about replaced ones that were skipped once
        ArrayList<Integer> replaced_skipped = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                UnsyncedNotification u = new UnsyncedNotification();
                u.notification_id = cursor.getInt(cursor.getColumnIndex(Notifications_Table.notification_id));
                u.message = cursor.getString(cursor.getColumnIndex(UnsyncedData.Notifications_Table.message));
                u.title = cursor.getString(cursor.getColumnIndex(UnsyncedData.Notifications_Table.title));
                u.seen_timestamp = cursor.getLong(cursor.getColumnIndex(UnsyncedData.Notifications_Table.seen_timestamp));
                u.application_package = cursor.getString(cursor.getColumnIndex(UnsyncedData.Notifications_Table.application_package));
                u.sqlite_row_id = cursor.getInt(cursor.getColumnIndex(Notifications_Table._ID));
                if (!cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type)).equals(AppManagement.INTERACTION_TYPE_REPLACE)) {
                    result.add(u);
                }
                // add 10% of replaced ones
                else if (AppManagement.getRandomNumberInRange(0,100) <= 10) {
                    result.add(u);
                }
                else {
                    replaced_skipped.add(cursor.getInt(cursor.getColumnIndex(Notifications_Table._ID)));
                }
            }
            cursor.close();
            // put the replaced ones as skipped
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(Notifications_Table.labeled, "-1");
            try {
                for (Integer id : replaced_skipped) {
                    database.update(DATABASE_NAME, values, "_id=" + id, null);
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
        return result;
    }

    public ArrayList<UnsyncedNotification> getArrivedNotifications() {
        init();
        ArrayList<UnsyncedNotification> result = new ArrayList();
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                Notifications_Table.interaction_type + " == NULL",
                null,null, null,
                UnsyncedData.Notifications_Table.interaction_timestamp + " ASC LIMIT 50");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                UnsyncedNotification u = new UnsyncedNotification();
                u.sqlite_row_id = cursor.getLong(cursor.getColumnIndex(Notifications_Table._ID));
                u.message = cursor.getString(cursor.getColumnIndex(Notifications_Table.message));
                u.title= cursor.getString(cursor.getColumnIndex(Notifications_Table.title));
                u.application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package));
                u.notification_id = cursor.getInt(cursor.getColumnIndex(Notifications_Table.notification_id));
                if (!result.contains(u)) result.add(u);
            }
            cursor.close();
        }
        return result;
    }

    public synchronized int countUnlabeledNotifications() {
        init();
        int count = 0;
        Cursor cursor = database.query(DATABASE_NAME,
                new String[]{UnsyncedData.Notifications_Table._ID},
                Notifications_Table.labeled + "=? AND " + Notifications_Table.interaction_type + "=?",
                new String[]{"0", AppManagement.INTERACTION_TYPE_DISMISS},
                null, null,
                UnsyncedData.Notifications_Table.interaction_timestamp + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                count++;
            }
            cursor.close();
        }
        return count;
    }

    public synchronized ArrayList<UnsyncedNotification> getLabeledNotifications() {
        init();
        ArrayList<UnsyncedNotification> result = new ArrayList<>();

        // getString all that are EITHER 1) labeled and dismissed or 2) clicked
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                Notifications_Table.labeled + "=? OR " + Notifications_Table.interaction_type + "=? OR "
                + Notifications_Table.prediction_correct + " > -1",
                new String[]{"1", AppManagement.INTERACTION_TYPE_CLICK},
                null, null,
                UnsyncedData.Notifications_Table.interaction_timestamp + " ASC");

        if (cursor != null) {
            if (!cursor.moveToFirst()) Log.d(TAG, "empty cursor");
            while (cursor.moveToNext()) {
                UnsyncedNotification u = new UnsyncedNotification();
                u.notification_id = cursor.getInt(cursor.getColumnIndex(Notifications_Table.notification_id));
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
                u.headphone_jack = cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack));
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
        return result;
    }


    public synchronized int getNumOfTrainingData() {
        init();
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                Notifications_Table.labeled + "=? OR " + Notifications_Table.interaction_type + "=?",
                new String[]{"1", AppManagement.INTERACTION_TYPE_CLICK},
                null, null,
                null);
        int count = 0;
        if (cursor != null) {while (cursor.moveToNext()) {count++;} cursor.close();}
        return count;
    }

    public synchronized ArrayList<NotificationText> getAllNotificationText() {
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

    public synchronized ContentImportance getContentImportance(Context c) {
        init();
        ContentImportance result = new ContentImportance();

        HashMap<String, ArrayList<Integer>> word_importances = new HashMap<>();

        Cursor cursor = database.query(DATABASE_NAME,
                new String[]{Notifications_Table.title, Notifications_Table.message, Notifications_Table.content_importance, Notifications_Table.interaction_type},
                "(" + Notifications_Table.labeled + "=? AND " + Notifications_Table.content_importance + ">?) OR " + Notifications_Table.interaction_type + "=?",
                new String[]{"1", "0", AppManagement.INTERACTION_TYPE_CLICK},
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                ArrayList<String> words = strip(c, cursor.getString(cursor.getColumnIndex(Notifications_Table.title)), cursor.getString(cursor.getColumnIndex(Notifications_Table.message)));
                for (String word : words) {
                    if (!word_importances.containsKey(word)) word_importances.put(word, new ArrayList<Integer>());
                    if (AppManagement.INTERACTION_TYPE_CLICK.equals(cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type)))) word_importances.get(word).add(5);
                    else word_importances.get(word).add(cursor.getInt(cursor.getColumnIndex(Notifications_Table.content_importance)));
                }
            }
            cursor.close();
        }
        HashMap<String, Double> word_importances_avg = new HashMap<>();
        for (String word : word_importances.keySet()) {
            word_importances_avg.put(word, AppManagement.average(word_importances.get(word)));
        }
        // these are actually flipped since keyset() returns keys in REVERSE order
        Map<String, Double> highest = AppManagement.MapUtil.sortByHighestValue(word_importances_avg);
        Map<String, Double> lowest = AppManagement.MapUtil.sortByLowestValue(word_importances_avg);
        int index = 0;
        // first result in iterator is the LAST key, so the lowest
        for (String key : highest.keySet()) {
            if (index >= AppManagement.WORD_IMPORTANCE_SIZE) break;
            result.unimportant[index] = key;
            index++;
        }
        index = 0;
        for (String key : lowest.keySet()) {
            if (index >= AppManagement.WORD_IMPORTANCE_SIZE) break;
            result.important[index] = key;
            index++;
        }
        return result;
    }

    public class ContentImportance {
        String[] important = new String[AppManagement.WORD_IMPORTANCE_SIZE];
        String[] unimportant = new String[AppManagement.WORD_IMPORTANCE_SIZE];
        public ContentImportance() {
            for (int i = 0;i < AppManagement.WORD_IMPORTANCE_SIZE;i++) {
                important[i] = "";
                unimportant[i] = "";
            }
        }
        public String importantToString() {
            String result = "";
            for (int i = 0;i < AppManagement.WORD_IMPORTANCE_SIZE;i++) {
                if (i < (AppManagement.WORD_IMPORTANCE_SIZE-1)) result += important[i] + ",";
                else result += important[i];
            }
            return result;
        }
        public String unimportantToString() {
            String result = "";
            for (int i = 0;i < AppManagement.WORD_IMPORTANCE_SIZE;i++) {
                if (i < (AppManagement.WORD_IMPORTANCE_SIZE-1)) result += unimportant[i] + ",";
                else result += unimportant[i];
            }
            return result;
        }
    }

    private ArrayList<String> stopWordsEng = null;
    private ArrayList<String> stopWordsFin = null;
    private ArrayList<String> strip(Context c, String title, String contents) {
        if (stopWordsEng == null) stopWordsEng = new ArrayList<>(Arrays.asList(Utils.readStopWords(c, R.raw.english)));
        if (stopWordsFin == null) stopWordsFin = new ArrayList<>(Arrays.asList(Utils.readStopWords(c, R.raw.finnish)));
        String a = title + " " + contents;
        a = a.toLowerCase().replaceAll("^[a-zA-Z0-9äöüÄÖÜ]", " ");
        ArrayList<String> words = new ArrayList<>(Arrays.asList(a.split(" ")));
        words.removeAll(stopWordsEng);
        words.removeAll(stopWordsFin);
        return words;
    }

    public synchronized ArrayList<Prediction> getPredictions(Context c) {
        ArrayList<String> predictedInteractions = new ArrayList<>(Arrays.asList(
                AppManagement.INTERACTION_TYPE_CLICK,
                AppManagement.INTERACTION_TYPE_DISMISS,
                AppManagement.INTERACTION_TYPE_PREDICTION_HIDE,
                ""
        ));

        init();
        ArrayList<Prediction> result = new ArrayList<>();
        Cursor cursor = database.query(DATABASE_NAME,
                new String[]{
                        Notifications_Table.title,
                        Notifications_Table.message,
                        Notifications_Table.application_package,
                        Notifications_Table._ID,
                        Notifications_Table.seen_timestamp,
                        Notifications_Table.predicted_as_show,
                        Notifications_Table.prediction_correct,
                        Notifications_Table.labeled,
                        Notifications_Table.interaction_type},
                Notifications_Table.predicted_as_show + " > -1 ",null,null, null,
                Notifications_Table.generate_timestamp + " DESC");
        int predicted;
        int verified;
        String interaction;
        ArrayList<Long> duplicate_ids = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                interaction = cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type));
                if (interaction == null) interaction = "";
                predicted = cursor.getInt(cursor.getColumnIndex(Notifications_Table.predicted_as_show));
                verified = cursor.getInt(cursor.getColumnIndex(Notifications_Table.prediction_correct));

                if (predictedInteractions.contains(interaction) && predicted > -1 && verified == -1) {
                    Prediction p = new Prediction(
                            cursor.getString(cursor.getColumnIndex(Notifications_Table.message)),
                            cursor.getString(cursor.getColumnIndex(Notifications_Table.title)),
                            cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package)),
                            cursor.getInt(cursor.getColumnIndex(Notifications_Table._ID)),
                            cursor.getLong(cursor.getColumnIndex(Notifications_Table.seen_timestamp)),
                            cursor.getInt(cursor.getColumnIndex(Notifications_Table.predicted_as_show)),
                            cursor.getInt(cursor.getColumnIndex(Notifications_Table.labeled)),
                            cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type))
                    );
                    // dont add duplicates
                    if (result.contains(p)) {
                        duplicate_ids.add(p.sqlite_id);
                    }
                    // max size 50
                    else if (result.size() <= 50) result.add(p);
                    else break;
                }
            }
            cursor.close();
        }
        else {
            Log.d(TAG, "getPredictions null cursor");
        }
        init();
        try {
            database.beginTransaction();
            ContentValues set_as_duplicate = new ContentValues();
            set_as_duplicate.put(Notifications_Table.prediction_correct, -2);
            for (Long id : duplicate_ids) {
                database.update(DATABASE_NAME, set_as_duplicate, "_id=" + id, null);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        if (cursor != null) cursor.close();

        return result;
    }

    public void batchUpdatePredictions(Context c, ArrayList<Prediction> predictions, int accepted) {
        init();
        database.beginTransaction();

        ContentValues values = new ContentValues();
        values.put(Notifications_Table.prediction_correct, accepted);
        try {
            for (Prediction p: predictions) {
                database.update(DATABASE_NAME, values, "_id=" + p.sqlite_id, null);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static boolean syncing = false;
    public void syncAlltoProvider(Context c) {
        if (syncing) return;
        syncing = true;

        init();

        AppManagement.setSyncTimestamp(c, System.currentTimeMillis());

        Log.d(TAG, "Syncing local database in order to be uploaded...");
        Cursor cursor = database.query(
            DATABASE_NAME,
            null, Notifications_Table.synced + " < 1 ", null, null,null, Notifications_Table.generate_timestamp + " ASC LIMIT 250"
        );
        String interaction;
        int predicted;
        boolean sync;
        ArrayList<Integer> ids = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (cursor.getInt(cursor.getColumnIndex(Notifications_Table.synced)) == 1) {
                    Log.d(TAG, "was already synced");
                    continue;
                }
                sync = false;
                interaction = cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type));
                predicted = cursor.getInt(cursor.getColumnIndex(Notifications_Table.predicted_as_show));
                int prediction_correct = cursor.getInt(cursor.getColumnIndex(Notifications_Table.prediction_correct));
                int labeled = cursor.getInt(cursor.getColumnIndex(Notifications_Table.labeled));
                if (interaction == null) interaction = "";
                // predictions were on
                if (predicted > -1) {
                    // if not interacted with yet
                    if (interaction == null || (interaction != null && interaction.equals(""))) {
                        sync = false;
                    }
                    // dismissed and labeled/skipped, and verified
                    else if (interaction.equals(AppManagement.INTERACTION_TYPE_DISMISS) &&
                            cursor.getInt(cursor.getColumnIndex(Notifications_Table.labeled)) != 0 &&
                            cursor.getInt(cursor.getColumnIndex(Notifications_Table.prediction_correct)) > -1) {
                        sync = true;
                    }
                    // not dismissed and prediction verified
                    else if (!interaction.equals(AppManagement.INTERACTION_TYPE_DISMISS) && cursor.getInt(cursor.getColumnIndex(Notifications_Table.prediction_correct)) > -1) {
                        sync = true;
                    }
                    else if (interaction.equals(AppManagement.INTERACTION_TYPE_REPLACE)) sync = true;
                    // duplicate
                    else if (prediction_correct == -2) sync = true;
                }
                // predictions werent on
                else {
                    // also labeled or skipped
                    if (interaction.equals(AppManagement.INTERACTION_TYPE_DISMISS) &&
                            cursor.getInt(cursor.getColumnIndex(Notifications_Table.labeled)) != 0) {
                        sync = true;
                    }
                    // if not dismissed
                    else if (!interaction.equals(AppManagement.INTERACTION_TYPE_DISMISS)) sync = true;
                }
                if (sync) {
                    ids.add(cursor.getInt(cursor.getColumnIndex(Notifications_Table._ID)));
                    UnsyncedNotification u = new UnsyncedNotification();
                    u.seen_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.seen_timestamp));
                    u.seen = cursor.getLong(cursor.getColumnIndex(Notifications_Table.seen_timestamp)) > 0;
                    u.notification_id = cursor.getInt(cursor.getColumnIndex(Notifications_Table.notification_id));
                    u.generate_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.generate_timestamp));
                    u.interaction_timestamp = cursor.getLong(cursor.getColumnIndex(Notifications_Table.interaction_timestamp));
                    u.application_package = cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.application_package));
                    u.notification_category = cursor.getString(cursor.getColumnIndex(Notifications_Table.notification_category)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.notification_category));
                    u.interaction_type = cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.interaction_type));
                    u.activity = cursor.getString(cursor.getColumnIndex(Notifications_Table.activity)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.activity));
                    u.battery_level = cursor.getString(cursor.getColumnIndex(Notifications_Table.battery_level)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.battery_level));
                    u.foreground_application_package = (cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package)) == null || cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package)) == "")
                            ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.foreground_application_package));

                    u.location = cursor.getString(cursor.getColumnIndex(Notifications_Table.location)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.location));
                    u.wifi_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.wifi_availability)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.wifi_availability));
                    u.network_availability = cursor.getString(cursor.getColumnIndex(Notifications_Table.network_availability)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.network_availability));
                    u.location = cursor.getString(cursor.getColumnIndex(Notifications_Table.location)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.location));
                    u.screen_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.screen_mode)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.screen_mode));
                    u.ringer_mode = cursor.getString(cursor.getColumnIndex(Notifications_Table.ringer_mode)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.ringer_mode));
                    u.headphone_jack = cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack)) == null ? EMPTY_VALUE : cursor.getString(cursor.getColumnIndex(Notifications_Table.headphone_jack));

                    u.content_importance_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.content_importance));
                    u.timing_value = cursor.getDouble(cursor.getColumnIndex(Notifications_Table.timing));
                    u.labeled = cursor.getInt(cursor.getColumnIndex(Notifications_Table.labeled)) > 0;
                    u.predicted_as_show = cursor.getString(cursor.getColumnIndex(Notifications_Table.predicted_as_show)) == null ? -1 : cursor.getInt(cursor.getColumnIndex(Notifications_Table.predicted_as_show));
                    u.prediction_correct = cursor.getString(cursor.getColumnIndex(Notifications_Table.prediction_correct)) == null ? -1 : cursor.getInt(cursor.getColumnIndex(Notifications_Table.prediction_correct));
                    Log.d(TAG, "synced: " + u.toString() + " " + u.interaction_type);
                    c.getContentResolver().insert(com.aware.plugin.notificationdiary.Providers.Provider.Notifications_Data.CONTENT_URI, u.toSyncableContentValues(c));
                }
            }
            cursor.close();
        }

        init();
        database.beginTransaction();

        ContentValues values = new ContentValues();
        values.put(Notifications_Table.synced, "1");
        try {
            for (Integer id : ids) {
                database.update(DATABASE_NAME, values, "_id=" + id, null);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        syncing = false;
    }

    @Override
    public void close() {
        if (database != null && database.isOpen()) database.close();
    }

    public class Prediction {
        public String text;
        public String title;
        public String package_name;
        public long sqlite_id;
        public long timestamp;
        public int predicted_as_show;
        public int labeled;
        public String interaction_type;

        Prediction(String text, String title, String package_name, long sqlite_id, long timestamp, int predicted_as_show, int labeled, String interaction_type) {
            this.sqlite_id = sqlite_id;
            this.text = text;
            this.title = title;
            this.package_name = package_name;
            this.sqlite_id = sqlite_id;
            this.timestamp = timestamp;
            this.predicted_as_show = predicted_as_show;
            this.labeled = labeled;
            this.interaction_type = interaction_type;
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
