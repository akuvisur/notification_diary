package com.aware.plugin.notificationdiary;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Applications;
import com.aware.Network;
import com.aware.Screen;
import com.aware.plugin.notificationdiary.NotificationContext.InteractionContext;
import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;
import com.aware.providers.Applications_Provider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class NotificationListener extends NotificationListenerService {
    static final String TAG = "NotificationListener";

    public static boolean connected = false;

    private static Context context;

    BroadcastReceiver deleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received something " + intent.getAction());
            if (intent.getAction().equals("NOTIFICATION_DELETED")) {
                Log.d(TAG, "Package of deleted: " + intent.getExtras().get("package"));
            }
        }
    };

    String FOREGROUND_APP_NAME = "";
    String FOREGROUND_APP_PACKAGE = "";

    public static final String UNKNOWN = "unknown";

    public static final String HEADSET_PLUGGED = "plugged";
    public static final String HEADSET_UNPLUGGED = "unplugged";

    public static final String AVAILABLE = "yes";
    public static final String UNAVAILABLE = "no";

    public static String HEADSET_STATUS = HEADSET_UNPLUGGED;
    public static String NETWORK_AVAILABLE = UNKNOWN;
    public static String WIFI_AVAILABLE = UNKNOWN;
    public static String LOCATION = UNKNOWN;
    public static String ACTIVITY = ActivityService.UNCERTAIN;

    public static String SCREEN_STATE = "";

    SensorReceiver ar;
    private class SensorReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND)) {
                Log.d(TAG, "Foreground app changed ");
                SharedPreferences sp = getSharedPreferences(AppManagement.SHARED_PREFS, MODE_PRIVATE);

                try {

                    Cursor app_data = getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI, null, null, null, "TIMESTAMP DESC LIMIT 1");
                    if (app_data != null) {
                        if (app_data.moveToNext()) {
                            FOREGROUND_APP_NAME = app_data.getString(app_data.getColumnIndex(Applications_Provider.Applications_Foreground.APPLICATION_NAME));
                            FOREGROUND_APP_PACKAGE = app_data.getString(app_data.getColumnIndex(Applications_Provider.Applications_Foreground.PACKAGE_NAME));
                        }
                        else Log.d(TAG, "cursor is null");
                        app_data.close();
                        SharedPreferences.Editor spe = getSharedPreferences(AppManagement.SHARED_PREFS, MODE_PRIVATE).edit();
                        int test_count = sp.getInt(AppManagement.TEST_COUNT, 0);
                        spe.putInt(AppManagement.TEST_COUNT, test_count + 1);
                        spe.apply();
                    }

                }
                catch (NullPointerException e) {
                    e.printStackTrace();
                }

                if (sp.getInt(AppManagement.TEST_COUNT, 0) <= 5) {
                    Toast.makeText(context, "Testing " + sp.getInt(AppManagement.TEST_COUNT, 0) + " ... Application changed to : " + FOREGROUND_APP_NAME, Toast.LENGTH_SHORT).show();
                }
                else if (sp.getInt(AppManagement.TEST_COUNT, 0) <= 5) {
                    Toast.makeText(context, "Everything seems to be working OK", Toast.LENGTH_SHORT).show();
                }
            }

            else if (intent.getAction().equals(Screen.ACTION_AWARE_SCREEN_OFF)) {
                Log.d(TAG, "Screen: " + intent.getAction());
                SCREEN_STATE = Screen.ACTION_AWARE_SCREEN_OFF;
                ActivityApiClient.screenOff();
            }

            else if (intent.getAction().equals(Screen.ACTION_AWARE_SCREEN_ON) || intent.getAction().equals(Screen.ACTION_AWARE_SCREEN_UNLOCKED)) {
                Log.d(TAG, "Screen: " + intent.getAction());
                SCREEN_STATE = Screen.ACTION_AWARE_SCREEN_ON;
                ActivityApiClient.screenOn();
                for (UnsyncedNotification n : arrivedNotifications) {
                    if (!n.seen) {
                        n.seen = true;
                        n.seen_timestamp = AppManagement.getCurrentTime();
                        UnsyncedData helper = new UnsyncedData(context);
                        ContentValues updated_values = new ContentValues();
                        updated_values.put(UnsyncedData.Notifications_Table.seen_timestamp, n.seen_timestamp);
                        updated_values.put(UnsyncedData.Notifications_Table.seen, n.seen);
                        helper.updateEntry((int) n.sqlite_row_id, updated_values);
                        Log.d(TAG, "seen: "  + n.title + " / " + n.message);
                    }
                }
            }

            // audio jack states
            else if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset is unplugged");
                        HEADSET_STATUS = HEADSET_UNPLUGGED;
                        break;
                    case 1:
                        Log.d(TAG, "Headset is plugged");
                        HEADSET_STATUS = HEADSET_PLUGGED;
                        break;
                    default:
                        Log.d(TAG, "I have no idea what the headset state is");
                        HEADSET_STATUS = UNKNOWN;
                }
            }

            // network states
            else if (intent.getAction().equals(Network.ACTION_AWARE_INTERNET_AVAILABLE)) {
                if ((int) (intent.getExtras().get(Network.EXTRA_ACCESS)) == 1) WIFI_AVAILABLE = AVAILABLE;
                else WIFI_AVAILABLE = UNAVAILABLE;
                NETWORK_AVAILABLE = AVAILABLE;
            }

            else if (intent.getAction().equals(Network.ACTION_AWARE_INTERNET_UNAVAILABLE)) {
                NETWORK_AVAILABLE = UNAVAILABLE;
                WIFI_AVAILABLE = UNAVAILABLE;
            }
        }
    }

    public NotificationListener() {
        Log.d(TAG, "service started");
        context = this;
    }

    @Override
    public void onListenerHintsChanged(int hints) {
        Log.d(TAG, "hints changed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int something) {
        Log.d(TAG, "onStartCommand");
        String notificationListenerString = Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners");
        //Check notifications access permission
        if (notificationListenerString == null || !notificationListenerString.contains(getPackageName()))
        {
            //The notification access has not acquired yet!
            Log.d(TAG, "no access");
            requestPermission();
        }else{
            //Your application has access to the notifications
            Log.d(TAG, "has access");
        }

        Log.d(TAG, "finished onStart");

        return START_STICKY;
    }

    @Override
    public void onCreate() {

        ar = new SensorReceiver();
        IntentFilter sensorFilter = new IntentFilter();
        sensorFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
        sensorFilter.addAction(Screen.ACTION_AWARE_SCREEN_ON);
        sensorFilter.addAction(Screen.ACTION_AWARE_SCREEN_OFF);
        sensorFilter.addAction(Screen.ACTION_AWARE_SCREEN_UNLOCKED);
        sensorFilter.addAction(Intent.ACTION_HEADSET_PLUG);

        // network states
        sensorFilter.addAction(Network.ACTION_AWARE_INTERNET_AVAILABLE);
        sensorFilter.addAction(Network.ACTION_AWARE_INTERNET_UNAVAILABLE);

        registerReceiver(ar, sensorFilter);
        //IntentFilter ifilt = new IntentFilter();
        //ifilt.addAction("NOTIFICATION_DELETED");
        //registerReceiver(deleteReceiver, ifilt);

        Intent activityService = new Intent(this, ActivityService.class);
        startService(activityService);

        // initialize google activity recognition
        new ActivityApiClient(this);

        // initialize neura location sensing
        //new NeuraApiConnector(this);
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(deleteReceiver);
        unregisterReceiver(ar);
    }

    public static void requestPermission() {
        Log.d(TAG, "requesting notif permission");
        Intent requestIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (context != null) {
            context.startActivity(requestIntent);
            Toast t = Toast.makeText(context, "Please allow access to notifications for full functionality", Toast.LENGTH_LONG);
            t.show();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "seppo connected");
        connected = true;
    }

    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "seppo disconnected");
        connected = false;
        requestPermission();
    }

    //private static HashMap<MapKey, ArrayList<String>> arrivedNotifications = new HashMap<>();
    private static ArrayList<UnsyncedNotification> arrivedNotifications = new ArrayList<>();

    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        // dont bother storing since even if user swipes the foreground activity might remain as the same
        // thus logging it as a click
        if (FOREGROUND_APP_PACKAGE.equals(sbn.getPackageName())) {return;}
        // if app in blacklist
        else if (AppManagement.BLACKLIST.contains(sbn.getPackageName())) {return;}

        // replace existing notifications from same apps to prevent "spamming" the user
        // also, these multiple notifications are not interacted with as they share the same
        // notification in the tray
        UnsyncedNotification replacedNotification = null;
        for (UnsyncedNotification n : arrivedNotifications) {
            if (n.application_package.equals(sbn.getPackageName())) {
                ContentValues replace = new ContentValues();
                replace.put(UnsyncedData.Notifications_Table.interaction_type, AppManagement.INTERACTION_TYPE_REPLACE);
                UnsyncedData helper = new UnsyncedData(context);
                helper.updateEntry((int) n.sqlite_row_id, replace);
                replacedNotification = n;
                Log.d(TAG, "replacement");
            }
        }
        if (replacedNotification != null) arrivedNotifications.remove(replacedNotification);

        Notification n = sbn.getNotification();

        getExtras(n);
        loadTexts(this, n);

        UnsyncedNotification unsynced = new UnsyncedNotification();
        unsynced.generate_timestamp = AppManagement.getCurrentTime();
        unsynced.message = String.valueOf(messageText);
        unsynced.title = String.valueOf(titleText);
        unsynced.application_package = sbn.getPackageName();
        unsynced.notification_id = sbn.getId();

        if (Build.VERSION.SDK_INT >= 21) unsynced.notification_category = n.category;
        else unsynced.notification_category = UNKNOWN;

        if (!SCREEN_STATE.equals(Screen.ACTION_AWARE_SCREEN_OFF)) {unsynced.seen = true; unsynced.seen_timestamp = AppManagement.getCurrentTime();}

        ContentValues c = new ContentValues();
        c.put(UnsyncedData.Notifications_Table.application_package, unsynced.application_package);
        c.put(UnsyncedData.Notifications_Table.notification_id, unsynced.notification_id);
        c.put(UnsyncedData.Notifications_Table.title, unsynced.title);
        c.put(UnsyncedData.Notifications_Table.message, unsynced.message);
        c.put(UnsyncedData.Notifications_Table.generate_timestamp, unsynced.generate_timestamp);
        c.put(UnsyncedData.Notifications_Table.seen_timestamp, unsynced.seen_timestamp);
        c.put(UnsyncedData.Notifications_Table.seen, unsynced.seen);
        Log.d(TAG, "inserting" + c.toString());
        UnsyncedData helper = new UnsyncedData(context);
        unsynced.sqlite_row_id = helper.insertRecord(c);

        arrivedNotifications.add(unsynced);
        Log.d(TAG, "posted: "  + unsynced.title + " / " + unsynced.message);
    }

    HashMap<StatusBarNotification, String> interactionForegroundApplications = new HashMap<>();
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "*** NOTIFICATION REMOVED");
        Log.d(TAG, "id : " + sbn.getId());
        String packageName = sbn.getPackageName();

        if (!AppManagement.BLACKLIST.contains(packageName)) new Handler(Looper.getMainLooper()).postDelayed(new StatusBarNotificationCheckedRunnable(sbn, System.currentTimeMillis()), AppManagement.INTERACTION_CHECK_DELAY);
        else Log.d(TAG, "Blacklisted app");

        interactionForegroundApplications.put(sbn, FOREGROUND_APP_PACKAGE);
    }

    private void checkNotificationInteraction(StatusBarNotification sbn, ArrayList<String> foreApps) {
        int identifier = UnsyncedNotification.getHashIdentifier(sbn.getId(), sbn.getPackageName());

        getExtras(sbn.getNotification());
        loadTexts(this, sbn.getNotification());

        UnsyncedNotification matchingNotification = null;
        for (UnsyncedNotification n : arrivedNotifications) {
            if ((identifier == n.getHashIdentifier()) &&
                    (n.title != null) &&
                    (n.title.equals(titleText))) {
                matchingNotification = n;
                break;
            }
        }
        // Did not find a matching notification
        if (matchingNotification == null) {Log.d(TAG, "No match found"); return;}
        else Log.d(TAG, "Match found.");

        ContentValues updated_values = new ContentValues();
        matchingNotification.interaction_timestamp = AppManagement.getCurrentTime();

        updated_values = new InteractionContext(context).addToValues(updated_values);
        updated_values.put(UnsyncedData.Notifications_Table.foreground_application_package, interactionForegroundApplications.get(sbn));

        if (SCREEN_STATE.equals(Screen.ACTION_AWARE_SCREEN_OFF)) {
            // system auto removed
            matchingNotification.interaction_type = AppManagement.INTERACTION_TYPE_SYSTEM_DISMISS;
            updated_values.put(UnsyncedData.Notifications_Table.interaction_type, AppManagement.INTERACTION_TYPE_SYSTEM_DISMISS);
        }

        else if (foreApps.contains(matchingNotification.application_package) || matchingNotification.application_package.equals(FOREGROUND_APP_PACKAGE)) {
            // click
            matchingNotification.interaction_type = AppManagement.INTERACTION_TYPE_CLICK;
            updated_values.put(UnsyncedData.Notifications_Table.interaction_type, AppManagement.INTERACTION_TYPE_CLICK);
        }
        else {
            // dismiss
            matchingNotification.interaction_type = AppManagement.INTERACTION_TYPE_DISMISS;
            updated_values.put(UnsyncedData.Notifications_Table.interaction_type, AppManagement.INTERACTION_TYPE_DISMISS);
        }

        UnsyncedData helper = new UnsyncedData(context);
        helper.updateEntry((int) matchingNotification.sqlite_row_id, updated_values);

        interactionForegroundApplications.remove(sbn);
        // remove this from list
        arrivedNotifications.remove(matchingNotification);
        Log.d(TAG, "removed: "  + matchingNotification.title + " / " + matchingNotification.message);
    }

    private class StatusBarNotificationCheckedRunnable implements Runnable {

        private StatusBarNotification notif;
        private long timestamp;

        public StatusBarNotificationCheckedRunnable(StatusBarNotification s, Long timestamp) {
            this.notif = s;
            this.timestamp = timestamp;
        }

        @Override
        public void run() {
            Cursor apps_run = getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI, null, "TIMESTAMP > " + timestamp, null, null);
            ArrayList<String> runApps = new ArrayList<>();
            if (apps_run != null ) {
                while (apps_run.moveToNext()) {
                    runApps.add(apps_run.getString(apps_run.getColumnIndex(Applications_Provider.Applications_Foreground.PACKAGE_NAME)));
                    Log.d(TAG, "runapps: " + apps_run.getString(apps_run.getColumnIndex(Applications_Provider.Applications_Foreground.PACKAGE_NAME)));
                }
                apps_run.close();
            }
            checkNotificationInteraction(notif, runApps);

        }
    }

    /*
    Following code originally by author of AcDisplay

 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
    CharSequence titleText;
    CharSequence titleBigText;
    CharSequence messageText;
    CharSequence messageBigText;
    CharSequence infoText;
    CharSequence subText;
    CharSequence summaryText;
    CharSequence[] messageTextLines;

    public void loadTexts(@NonNull Context context, @NonNull Notification notification) {
        final Bundle extras = getExtras(notification);
        if (extras != null) loadFromExtras(notification, extras);
        if (TextUtils.isEmpty(titleText)
                && TextUtils.isEmpty(titleBigText)
                && TextUtils.isEmpty(messageText)
                && messageTextLines == null) {
            loadFromView(context, notification);
        }
    }

    private Bundle getExtras(@NonNull Notification notification) {
        // Access extras using reflections.
        try {
            Field field = notification.getClass().getDeclaredField("extras");
            field.setAccessible(true);
            return (Bundle) field.get(notification);
        } catch (Exception e) {
            Log.w(TAG, "Failed to access extras on Jelly Bean.");
            return null;
        }
    }

    private void loadFromExtras(@NonNull Notification n, @NonNull Bundle extras) {
        titleBigText = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        titleText = extras.getCharSequence(Notification.EXTRA_TITLE);
        infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
        subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        messageBigText = removeColorSpans(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        messageText = removeColorSpans(extras.getCharSequence(Notification.EXTRA_TEXT));

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        messageTextLines = doIt(lines);
    }

    private void loadFromView(@NonNull Context context, @NonNull final Notification notification) {
        ViewGroup view;
        {

            final RemoteViews rvs = notification.bigContentView == null
                    ? notification.contentView
                    : notification.bigContentView;

            // Try to load the view from remote views.
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            try {
                view = (ViewGroup) inflater.inflate(rvs.getLayoutId(), null);
                rvs.reapply(this, view);
            } catch (Exception e) {
                return;
            }
        }

        ArrayList<TextView> textViews = new RecursiveFinder<TextView>(TextView.class).expand(view);
        removeClickableViews(textViews);
        removeSubtextViews(context, textViews);

        removeActionViews(notification.actions, textViews);

        // No views
        if (textViews.size() == 0)
            return;

        TextView title = findTitleTextView(textViews);
        textViews.remove(title); // no need of title view anymore
        titleText = title.getText();

        // No views
        if (textViews.size() == 0)
            return;

        // Pull all other texts and merge them.
        int length = textViews.size();
        CharSequence[] messages = new CharSequence[length];
        for (int i = 0; i < length; i++) messages[i] = textViews.get(i).getText();
        messageTextLines = doIt(messages);
    }

    @Nullable
    private static CharSequence removeColorSpans(@Nullable CharSequence cs) {
        if (cs == null) return null;
        if (cs instanceof Spanned) {
            cs = new SpannableStringBuilder(cs);
        }
        if (cs instanceof Spannable) {
            CharacterStyle[] styles;
            Spannable spanned = (Spannable) cs;
            styles = spanned.getSpans(0, spanned.length(), TextAppearanceSpan.class);
            for (CharacterStyle style : styles) spanned.removeSpan(style);
            styles = spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class);
            for (CharacterStyle style : styles) spanned.removeSpan(style);
            styles = spanned.getSpans(0, spanned.length(), BackgroundColorSpan.class);
            for (CharacterStyle style : styles) spanned.removeSpan(style);
        }
        return cs;
    }

    @Nullable
    private CharSequence[] doIt(@Nullable CharSequence[] lines) {
        if (lines != null) {
            // Filter empty lines.
            ArrayList<CharSequence> list = new ArrayList<CharSequence>();
            for (CharSequence msg : lines) {
                msg = removeSpaces(msg);
                if (!TextUtils.isEmpty(msg)) {
                    list.add(removeColorSpans(msg));
                }
            }

            // Create new array.
            if (list.size() > 0) {
                return list.toArray(new CharSequence[list.size()]);
            }
        }
        return null;
    }

    @Nullable
    private static String removeSpaces(@Nullable CharSequence cs) {
        if (cs == null) return null;
        String string = cs instanceof String
                ? (String) cs : cs.toString();
        return string
                .replaceAll("(\\s+$|^\\s+)", "")
                .replaceAll("\n+", "\n");
    }

    private void removeActionViews(@Nullable Notification.Action[] actions,
                                   @NonNull ArrayList<TextView> textViews) {
        if (actions == null) {
            return;
        }

        for (Notification.Action action : actions) {
            for (int i = textViews.size() - 1; i >= 0; i--) {
                CharSequence text = textViews.get(i).getText();
                if (text != null && text.equals(action.title)) {
                    textViews.remove(i);
                    break;
                }
            }
        }
    }

    private void removeClickableViews(@NonNull ArrayList<TextView> textViews) {
        for (int i = textViews.size() - 1; i >= 0; i--) {
            TextView child = textViews.get(i);
            if (child.isClickable() || child.getVisibility() != View.VISIBLE) {
                textViews.remove(i);
                break;
            }
        }
    }

    private void removeSubtextViews(@NonNull Context context,
                                    @NonNull ArrayList<TextView> textViews) {
        float subtextSize = context.getResources().getDimension(R.dimen.notification_subtext_size);
        for (int i = textViews.size() - 1; i >= 0; i--) {
            final TextView child = textViews.get(i);
            final String text = child.getText().toString();
            if (child.getTextSize() == subtextSize
                    // empty textviews
                    || text.matches("^(\\s*|)$")
                    // clock textviews
                    || text.matches("^\\d{1,2}:\\d{1,2}(\\s?\\w{2}|)$")) {
                textViews.remove(i);
            }
        }
    }

    @NonNull
    private TextView findTitleTextView(@NonNull ArrayList<TextView> textViews) {
        // The idea is that title text is the
        // largest one.
        TextView largest = null;
        for (TextView textView : textViews) {
            if (largest == null || textView.getTextSize() > largest.getTextSize()) {
                largest = textView;
            }
        }
        assert largest != null; // cause the count of views is always >= 1
        return largest;
    }

    private static class RecursiveFinder<T extends View> {

        private final ArrayList<T> list;
        private final Class<T> clazz;

        public RecursiveFinder(@NonNull Class<T> clazz) {
            this.list = new ArrayList<T>();
            this.clazz = clazz;
        }

        public ArrayList<T> expand(@NonNull ViewGroup viewGroup) {
            int offset = 0;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i + offset);

                if (child == null) {
                    continue;
                }

                if (clazz.isAssignableFrom(child.getClass())) {
                    //noinspection unchecked
                    list.add((T) child);
                } else if (child instanceof ViewGroup) {
                    expand((ViewGroup) child);
                }
            }
            return list;
        }
    }
}
