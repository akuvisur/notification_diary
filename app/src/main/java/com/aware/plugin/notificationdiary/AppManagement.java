package com.aware.plugin.notificationdiary;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.format.DateFormat;

//import com.neura.sdk.object.Permission;

import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;
import com.aware.plugin.notificationdiary.Providers.WordBins;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by aku on 14/11/16.
 */
public class AppManagement {
    public static final String SHARED_PREFS = "com.aware.plugin.notificationdiary";
    public static final String TEST_COUNT = "TEST_COUNT";
    public static final String NUM_CLUSTERS = "OPTIMAL_NUM_CLUSTERS";

    public static final int INTERACTION_CHECK_DELAY = 3000;
    public static final String INTERACTION_TYPE_SYSTEM_DISMISS = "system_dismiss";
    public static final String INTERACTION_TYPE_REPLACE = "replaced";
    public static final String INTERACTION_TYPE_DISMISS = "dismiss";
    public static final String INTERACTION_TYPE_CLICK = "click";

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
        clusters = helper.extractClusters(c);
        helper.close();
        return clusters;
    }

    public static ArrayList<Cluster> getClusters(Context c) {
        if (clusters == null) extractClusters(c);
        return clusters;
    }

    // locations

    /**
     * Haversine formula for geographic distances.  Returns distance in meters.
     */
    public static Double wgs84_dist(Double lat1, Double lon1, Double lat2, Double lon2) {
        Double EARTH_RADIUS = 6378137.;
        Double dLat = Math.toRadians(lat2 - lat1);
        Double dLon = Math.toRadians(lon2 - lon1);
        Double a = (Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2));
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double d = EARTH_RADIUS * c;
        return d;
    }
}
