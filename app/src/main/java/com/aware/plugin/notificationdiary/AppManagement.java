package com.aware.plugin.notificationdiary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

//import com.neura.sdk.object.Permission;

import com.aware.Aware;
import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;
import com.aware.plugin.notificationdiary.Providers.WordBins;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by aku on 14/11/16.
 */
public class AppManagement {
    private static final String TAG = "AppManagement";

    public static int PREF_FREQUENCY_WATCHDOG = 15 * 60;

    public static final String SHARED_PREFS = "com.aware.plugin.notificationdiary";
    public static final String TEST_COUNT = "TEST_COUNT";
    public static final String NUM_CLUSTERS = "OPTIMAL_NUM_CLUSTERS";
    public static final String PREDICTIONS_ENABLED = "PREDICTIONS_ENABLED";
    public static final String RINGER_MODE = "RINGER_MODE";
    public static final String SOUND_VOLUME = "SOUND_VOLUME";
    public static final String FIRST_LAUNCH = "FIRST_LAUNCH";
    public static final String CONDITIONS_ACCEPTED = "CONDITIONS_ACCEPTED";

    public static final String SYNC_TIME = "SYNC_TIME";
    public static final long SYNC_DELAY = 3000;

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
    public static final int NOTIFICATION_CAN_ENABLE_PREDICTIONS = 7584929;

    public static final int WORD_IMPORTANCE_SIZE = 10;

    //public static final ArrayList<Permission> neuraPermissions = Permission.list(new String[]{"userArrivedHome", "userArrivedToWork", "userArrivedAtCafe", "userArrivedAtHospital", "userArrivedAtAirport", "userArrivedAtSchoolCampus", "userArrivedAtGroceryStore"});

    public static ArrayList<String> BLACKLIST = new ArrayList<>();

    public static void init(Context c) {
        BLACKLIST.add("android");
        // messaging action button does not launch an activity so dismiss/click is uncertain
        BLACKLIST.add("com.facebook.orca");
        // notifications launch different application (appstore)
        BLACKLIST.add("com.android.providers.downloads");
        // clock notifications always use full screen activities anyway
        BLACKLIST.add("com.sec.android.app.clockpackage");
        // call
        BLACKLIST.add("com.android.incallui");
        BLACKLIST.add("android");
    }

    public static Boolean predictionsEnabled(Context c) {
        return Aware.getSetting(c, PREDICTIONS_ENABLED).equals("true");
    }

    public static void enablePredictions(Context c, boolean enabled) {
        Aware.setSetting(c, PREDICTIONS_ENABLED, enabled);
        setOwnNotificationsHidden(c, true);
        setSoundControlAllowed(c, true);
    }

    public static void storeNumClusters(int num_clusters, Context c) {
        Aware.setSetting(c, NUM_CLUSTERS, num_clusters);
    }

    public static int getNumClusters(Context c) {
        String value = Aware.getSetting(c, NUM_CLUSTERS);
        if (value.equals("")) return 10;
        else return Integer.valueOf(value);
    }

    public static void setSyncTimestamp(Context c, long timestamp) {
        Aware.setSetting(c, SYNC_TIME, timestamp);
    }

    public static long getSyncTimestamp(Context c) {
        String value = Aware.getSetting(c, SYNC_TIME);
        if (value.equals("")) return System.currentTimeMillis();
        else return Long.valueOf(value);
    }

    static Calendar calendar = null;
    public static int getHour(long timestamp) {
        if (calendar == null) calendar = Calendar.getInstance();
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
        if (calendar == null) calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(time);
        String date = DateFormat.format("kk:mm MMM d", calendar).toString();
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
        Aware.setSetting(c, RINGER_MODE, mode);
        Aware.setSetting(c, SOUND_VOLUME, volume);
    }

    public static int getRingerMode(Context c) {
        String value = Aware.getSetting(c, RINGER_MODE);
        if (value.equals("")) return AudioManager.RINGER_MODE_VIBRATE;
        else return Integer.valueOf(value);
    }

    public static int getSoundVolume(Context c) {
        String value = Aware.getSetting(c, SOUND_VOLUME);
        if (value.equals("")) return 0;
        else return Integer.valueOf(value);
    }

    public static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        int random = r.nextInt((max - min) + 1) + min;
        return random;
    }

    public static void setSoundControlAllowed(Context c, boolean soundControlAllowed) {
        Aware.setSetting(c, SOUND_CONTROL_ALLOWED, soundControlAllowed);
        Intent soundControl = new Intent(c, NotificationAlarmManager.class);
        if (soundControlAllowed) c.startService(soundControl);
        else c.stopService(soundControl);
    }

    public static boolean getSoundControlAllowed(Context c) {
        String value = Aware.getSetting(c, SOUND_CONTROL_ALLOWED);
        if (value.equals("")) return false;
        else return value.equals("true");
    }

    public static void setOwnNotificationsHidden(Context c, boolean ownNotificationsHidden) {
        Aware.setSetting(c, SELF_NOTIFICATIONS_HIDDEN, ownNotificationsHidden);
    }

    // dont allow hiding own notifications by default
    public static boolean getOwnNotificationsHidden(Context c) {
        String value = Aware.getSetting(c, SELF_NOTIFICATIONS_HIDDEN);
        if (value.equals("")) return true;
        else return value.equals("true");
    }

    public static boolean isFirstLaunch(Context c) {
        String value = Aware.getSetting(c, FIRST_LAUNCH);
        if (value.equals("")) return true;
        else return value.equals("true");
    }

    public static void setFirstLaunch(Context c) {
        Aware.setSetting(c, FIRST_LAUNCH, false);
    }

    public static boolean conditionsAccepted(Context c) {
        String value = Aware.getSetting(c, CONDITIONS_ACCEPTED);
        if (value.equals("")) return false;
        else return value.equals("true");
    }

    public static void acceptConditions(Context c) {
        Aware.setSetting(c, CONDITIONS_ACCEPTED, true);
    }

    public static void startDailyModel(Context c) {
        if (predictionsEnabled(c)) {
            Intent i = new Intent(c, ContentAnalysisService.class);
            PendingIntent repeatingIntent = PendingIntent.getService(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) c.getSystemService(ALARM_SERVICE);
            am.cancel(repeatingIntent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + AlarmManager.INTERVAL_DAY,
                        repeatingIntent);
            } else {
                am.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
                        AlarmManager.INTERVAL_DAY,
                        repeatingIntent);
            }

        }
    }

    private static final String TUTORIAL_PAGE = "TUTORIAL_PAGE";
    public static void setTutorialPage(Context c, int page) {
        Aware.setSetting(c, TUTORIAL_PAGE, page);
    }

    public static int getTutorialPage(Context c) {
        String value = Aware.getSetting(c, TUTORIAL_PAGE);
        if (value.equals("")) return 1;
        else return Integer.valueOf(value);
    }

    public static class MapUtil
    {
        public static <K, V extends Comparable<? super V>> Map<K, V>
        sortByHighestValue( Map<K, V> map )
        {
            List<Map.Entry<K, V>> list =
                    new LinkedList<Map.Entry<K, V>>( map.entrySet() );
            Collections.sort( list, new Comparator<Map.Entry<K, V>>()
            {
                public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
                {
                    return (o1.getValue()).compareTo( o2.getValue() );
                }
            } );

            Map<K, V> result = new LinkedHashMap<K, V>();
            for (Map.Entry<K, V> entry : list)
            {
                result.put( entry.getKey(), entry.getValue() );
            }
            return result;
        }

        public static <K, V extends Comparable<? super V>> Map<K, V>
        sortByLowestValue( Map<K, V> map )
        {
            List<Map.Entry<K, V>> list =
                    new LinkedList<Map.Entry<K, V>>( map.entrySet() );
            Collections.sort( list, new Comparator<Map.Entry<K, V>>()
            {
                public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
                {
                    return (o2.getValue()).compareTo( o1.getValue() );
                }
            } );

            Map<K, V> result = new LinkedHashMap<K, V>();
            for (Map.Entry<K, V> entry : list)
            {
                result.put( entry.getKey(), entry.getValue() );
            }
            return result;
        }
    }

    public static double average(ArrayList<Integer> args) {
        double sum = 0; //average will have decimal point
        for(int i=0; i < args.size(); i++){
            //parse string to double, note that this might fail if you encounter a non-numeric string
            //Note that we could also do Integer.valueOf( args[i] ) but this is more flexible
            sum += Double.valueOf(args.get(i));
        }
        double average = sum/args.size();
        return average;
    }
}
