package com.aware.plugin.notificationdiary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

//import com.neura.sdk.object.Permission;

import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;
import com.aware.plugin.notificationdiary.Providers.WordBins;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

/**
 * Created by aku on 14/11/16.
 */
public class AppManagement {
    private static final String TAG = "AppManagement";

    public static final String SHARED_PREFS = "com.aware.plugin.notificationdiary";
    public static final String TEST_COUNT = "TEST_COUNT";
    public static final String NUM_CLUSTERS = "OPTIMAL_NUM_CLUSTERS";
    public static final String PREDICTIONS_ENABLED = "PREDICTIONS_ENABLED";
    public static final String RINGER_MODE = "RINGER_MODE";
    public static final String SOUND_VOLUME = "SOUND_VOLUME";

    public static final String SOUND_CONTROL_ALLOWED = "SOUND_CONTROL_ALLOWED";
    public static final String SELF_NOTIFICATIONS_HIDDEN = "SELF_NOTIFICATIONS_HIDDEN";

    public static final int INTERACTION_CHECK_DELAY = 3000;
    public static final String INTERACTION_TYPE_SYSTEM_DISMISS = "system_dismiss";
    public static final String INTERACTION_TYPE_REPLACE = "replaced";
    public static final String INTERACTION_TYPE_DISMISS = "dismiss";
    public static final String INTERACTION_TYPE_CLICK = "click";
    public static final String INTERACTION_TYPE_PREDICTION_HIDE = "prediction_hide";

    public static final int NOTIFICATION_UNLABELED_NOTIFICATIONS = 7812378;
    public static final int NOTIFICATION_UNVERIFIED_PREDICTIONS = 98347734;

    public static long LAST_TOUCH = 0;

    //public static final ArrayList<Permission> neuraPermissions = Permission.list(new String[]{"userArrivedHome", "userArrivedToWork", "userArrivedAtCafe", "userArrivedAtHospital", "userArrivedAtAirport", "userArrivedAtSchoolCampus", "userArrivedAtGroceryStore"});

    public static ArrayList<String> BLACKLIST = new ArrayList<>();
    static SharedPreferences sp;
    static SharedPreferences.Editor spe;

    public static void init(Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        //spe = sp.edit();

        BLACKLIST.add("android");
        // messaging action button does not launch an activity so dismiss/click is uncertain
        BLACKLIST.add("com.facebook.orca");
        // notifications launch different application (appstore)
        BLACKLIST.add("com.android.providers.downloads");

        // this application :)
        //BLACKLIST.add("com.aware.plugin.notificationdiary");
    }

    public static Boolean predictionsEnabled(Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(PREDICTIONS_ENABLED, false);
    }

    public static void enablePredictions(Context c, boolean enabled) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        spe = sp.edit();
        spe.putBoolean(PREDICTIONS_ENABLED, enabled);
        spe.apply();
        setOwnNotificationsHidden(c, false);
        setSoundControlAllowed(c, true);
    }

    public static void storeNumClusters(int num_clusters, Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        spe = sp.edit();

        spe.putInt(NUM_CLUSTERS, num_clusters);
        spe.apply();
    }

    public static int getNumClusters(Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        return sp.getInt(NUM_CLUSTERS, 15);
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis()/1000;
    }

    static Calendar calendar = Calendar.getInstance();
    public static int getHour(long timestamp) {
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(timestamp);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static String getApplicationNameFromPackage(Context c, String pkg) {
        final PackageManager pm = c.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo( pkg, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    }

    public static String getDate(Context c, long time) {
        Calendar cal = Calendar.getInstance(c.getResources().getConfiguration().locale);
        cal.setTimeInMillis(time*1000);
        String date = DateFormat.format("kk:mm MMM d", cal).toString();
        return date;
    }

    // extract word bins
    static ArrayList<Cluster> clusters;
    public static ArrayList<Cluster> extractClusters(Context c) {
        WordBins helper = new WordBins(c);
        clusters = helper.extractAllClusters(c, true);
        helper.close();
        return clusters;
    }

    public static ArrayList<Cluster> getClusters(Context c) {
        if (clusters == null) extractClusters(c);
        return clusters;
    }

    public static void storeNewRingerMode(Context c, int mode, int volume) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        spe = sp.edit();

        spe.putInt(RINGER_MODE, mode);
        spe.putInt(SOUND_VOLUME, volume);
        spe.apply();
    }

    public static int getRingerMode(Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        return sp.getInt(RINGER_MODE, AudioManager.RINGER_MODE_VIBRATE);
    }

    public static int getSoundVolume(Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        return sp.getInt(SOUND_VOLUME, 0);
    }

    public static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        int random = r.nextInt((max - min) + 1) + min;
        Log.d(TAG, "new random: " + random);
        return random;
    }

    public static void setSoundControlAllowed(Context c, boolean soundControlAllowed) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        spe = sp.edit();
        spe.putBoolean(SOUND_CONTROL_ALLOWED, soundControlAllowed);
        Log.d(TAG, "setSoundControlAllowed: " + getSoundControlAllowed(c));
        spe.apply();
    }

    public static boolean getSoundControlAllowed(Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        Log.d(TAG, "getSoundControlAllowed: " + sp.getBoolean(SOUND_CONTROL_ALLOWED, true));
        return sp.getBoolean(SOUND_CONTROL_ALLOWED, true);
    }

    public static void setOwnNotificationsHidden(Context c, boolean ownNotificationsHidden) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        spe = sp.edit();
        spe.putBoolean(SELF_NOTIFICATIONS_HIDDEN, ownNotificationsHidden);
        spe.apply();
    }

    public static boolean getOwnNotificationsHidden(Context c) {
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(SELF_NOTIFICATIONS_HIDDEN, true);
    }

    // locations

}
