package com.aware.plugin.notificationdiary;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;
import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;
import java.util.List;

public class MainTabs extends AppCompatActivity {

    private static final String TAG = "MainTabs";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    private static Context context;

    private static ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();

    private static String SKIP_PACKAGE = "";
    private static Integer SKIP_COUNT = 0;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private static Toolbar toolbar;

    private static boolean DEBUG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tabs);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                switch(position) {
                    case 0:
                        toolbar.setTitle("Notification Diary");
                        break;
                    case 1:
                        toolbar.setTitle("Predictions");
                        break;
                    case 2:
                        toolbar.setTitle("Help");
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}


        });

        context = this;

        AppManagement.init(context);

        Aware.startAWARE();

        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_NETWORK_STATE);

        PackageManager pm = context.getPackageManager();
        boolean allPermissionsOk = true;
        for (String perm : REQUIRED_PERMISSIONS) {
            int hasPerm = pm.checkPermission(
                    perm,
                    context.getPackageName());
            Log.d(TAG, "permission: " + perm + " " + hasPerm);
            if (hasPerm != PackageManager.PERMISSION_GRANTED) {
                allPermissionsOk = false;

                Intent permissions = new Intent(this, PermissionsHandler.class);
                permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
                permissions.putExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY, getPackageName() + "/" + getClass().getName());
                startActivity(permissions);
                Log.d(TAG, "launching permissions handler");
                break;
            }
        }

        if (allPermissionsOk) {
            Log.d(TAG, "All is good.");
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    Aware.setSetting(context, Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND, true);
                    Aware.setSetting(context, Applications.STATUS_AWARE_ACCESSIBILITY, true);
                    Aware.setSetting(context, Aware_Preferences.STATUS_APPLICATIONS, true);
                    Aware.startApplications(context);
                    Aware.startBattery(context);
                    Aware.startScreen(context);
                    Aware.startNetwork(context);
                    Aware.startLocations(context);
                }
            });

            isAccessibilityServiceActive(context);

            SharedPreferences sp = getSharedPreferences(AppManagement.SHARED_PREFS, MODE_PRIVATE);
            int test_count = sp.getInt(AppManagement.TEST_COUNT, 0);
            if (test_count <= 5) {
                Toast.makeText(this, "Please change foreground application to test application functionality..", Toast.LENGTH_LONG).show();
            }

            Intent service = new Intent(this, NotificationListener.class);
            startService(service);

            //Intent touchService = new Intent(this, TouchDetectService.class);
            //startService(touchService);
        }
        else {
            Aware.setSetting(this, Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND, false);
            Aware.setSetting(this, Applications.STATUS_AWARE_ACCESSIBILITY, false);
            Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, false);

            Aware.stopBattery(this);
            Aware.stopScreen(this);
            Aware.stopNetwork(this);
            Aware.stopLocations(this);
            Toast.makeText(this, "Please allow all permissions and restart application.", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_tabs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_diary) {
            mViewPager.setCurrentItem(0);
            return true;
        }

        else if (id == R.id.action_prediction) {
            mViewPager.setCurrentItem(1);
            return true;
        }

        else if (id == R.id.action_help) {
            mViewPager.setCurrentItem(2);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    static TextView notifications_remaining;

    static RelativeLayout notification_layout;

    static ToggleButton content_unsure_button;
    static ToggleButton timing_unsure_button;
    static RatingBar content_value;
    static RatingBar timing_value;
    static Button content_help_button;
    static Button timing_help_button;
    static Button skip_button;
    static Button next_button;
    static TextView notification_title;
    static TextView notification_app_name;
    static TextView notification_message;
    static TextView notification_timestamp;

    static Button skip_all_button;

    private static int curFragmentNumber = 1;

    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static int curSectionNumber;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            curSectionNumber = sectionNumber;
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            curFragmentNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                // Diary view
                case 1:
                    return generateDiaryView(inflater, container);
                case 2:
                    return generatePredictionView(inflater, container);
                case 3:
                    return generateHelpView(inflater, container);
                default:
                    return generateDiaryView(inflater, container);
            }

        }

    }

    private static boolean content_inputted = false;
    private static boolean timing_inputted = false;
    private static boolean skip_included = false;

    private static View emptyView;
    private static View curRootView;
    private static RelativeLayout button_container;
    private static LinearLayout skipall_layout;

    private static LayoutInflater sharedInflater;
    private static ViewGroup sharedContainer;

    public static View generateDiaryView(final LayoutInflater inflater, final ViewGroup container) {
        Log.d(TAG, "Generating new diary view");

        sharedInflater = inflater;
        sharedContainer = container;

        remainingNotifications = fetchRemainingNotifications();
        emptyView = inflater.inflate(R.layout.diary_view_empty, container, false);
        if (remainingNotifications.size() == 0) {
            return emptyView;
        }
        final View rootView = inflater.inflate(R.layout.diary_view, container, false);

        notifications_remaining = (TextView) rootView.findViewById(R.id.diary_notifications_remaining);

        notification_layout = (RelativeLayout) rootView.findViewById(R.id.notification_layout);
        notification_app_name = (TextView) rootView.findViewById(R.id.diary_appname);
        notification_message= (TextView) rootView.findViewById(R.id.diary_contents);
        notification_title = (TextView) rootView.findViewById(R.id.diary_title);
        notification_timestamp = (TextView) rootView.findViewById(R.id.diary_timestamp);

        skipall_layout = (LinearLayout) rootView.findViewById(R.id.diary_skip_all_layout);
        button_container = (RelativeLayout) rootView.findViewById(R.id.diary_button_container);

        content_help_button = (Button) rootView.findViewById(R.id.diary_content_help);
        content_help_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(rootView, R.string.diary_content_help, Snackbar.LENGTH_LONG).show();
            }
        });

        timing_help_button = (Button) rootView.findViewById(R.id.diary_timing_help);
        timing_help_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(rootView, R.string.diary_timing_help, Snackbar.LENGTH_LONG).show();
            }
        });

        content_unsure_button = (ToggleButton) rootView.findViewById(R.id.diary_content_unsure);
        content_unsure_button.setBackgroundResource(R.color.colorPrimaryLight);
        content_unsure_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d(TAG, "Content unsure clicked");
                content_value.setEnabled(!b);
                if (b) {
                    content_value.setRating(0F);
                    content_unsure_button.setBackgroundResource(R.color.colorAccent);
                    content_inputted = true;
                }
                else {
                    content_unsure_button.setBackgroundResource(R.color.colorPrimaryLight);
                    content_inputted = false;
                }
                next_button.setEnabled(content_inputted && timing_inputted);
            }
        });
        timing_unsure_button = (ToggleButton) rootView.findViewById(R.id.diary_timing_unsure);
        timing_unsure_button.setBackgroundResource(R.color.colorPrimaryLight);
        timing_unsure_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d(TAG, "Timing unsure clicked");
                timing_value.setEnabled(!b);
                if (b) {
                    timing_value.setRating(0F);
                    timing_unsure_button.setBackgroundResource(R.color.colorAccent);
                    timing_inputted = true;
                }
                else {
                    timing_unsure_button.setBackgroundResource(R.color.colorPrimaryLight);
                    timing_inputted = false;
                }
                next_button.setEnabled(content_inputted && timing_inputted);
            }
        });

        content_value = (RatingBar) rootView.findViewById(R.id.diary_content_value);
        content_value.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                if (v >= 0) content_inputted = true;
                else content_inputted = false;
                next_button.setEnabled(content_inputted && timing_inputted);
            }
        });
        timing_value = (RatingBar) rootView.findViewById(R.id.diary_timing_value);
        timing_value.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                if (v >= 0) timing_inputted = true;
                else timing_inputted = false;
                next_button.setEnabled(content_inputted && timing_inputted);
            }
        });

        skip_button = (Button) rootView.findViewById(R.id.diary_skip);
        next_button = (Button) rootView.findViewById(R.id.diary_next);

        skip_button = (Button) rootView.findViewById(R.id.diary_skip);
        skip_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues updated_values = new ContentValues();
                updated_values.put(UnsyncedData.Notifications_Table.labeled, -1);
                UnsyncedData helper = new UnsyncedData(context);
                helper.updateEntry((int) remainingNotifications.get(0).sqlite_row_id, updated_values);

                if (remainingNotifications.get(0).application_package.equals(SKIP_PACKAGE)) { SKIP_COUNT++; }
                else { SKIP_COUNT = 0; }
                SKIP_PACKAGE = remainingNotifications.get(0).application_package;

                remainingNotifications.remove(0);
                notification_layout.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_out));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshDiaryFragment();
                    }
                }, 400);
            }
        });

        next_button = (Button) rootView.findViewById(R.id.diary_next);
        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues updated_values = new ContentValues();
                updated_values.put(UnsyncedData.Notifications_Table.labeled, 1);
                updated_values.put(UnsyncedData.Notifications_Table.content_importance, content_value.getRating());
                updated_values.put(UnsyncedData.Notifications_Table.timing, timing_value.getRating());
                UnsyncedData helper = new UnsyncedData(context);
                helper.updateEntry((int) remainingNotifications.get(0).sqlite_row_id, updated_values);

                remainingNotifications.remove(0);

                SKIP_COUNT = 0;
                Log.d(TAG, "skip reset");

                content_inputted = false;
                timing_inputted = false;

                notification_layout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_out_left));
                skip_all_button.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_out_left));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshDiaryFragment();
                    }
                }, 400);
            }
        });

        skip_all_button = (Button) rootView.findViewById(R.id.diary_skip_all);
        skip_all_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skip_all_button.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_out));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        skipAll(SKIP_PACKAGE);
                    }
                },400);
            }
        });

        curRootView = rootView;

        refreshDiaryFragment();

        if (DEBUG) {
            Button b = new Button(context);
            b.setText("POST NOTIFICATION");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                    .setContentTitle("a"+System.currentTimeMillis())
                                    .setContentText("asdf!" + System.currentTimeMillis());
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            notificationManager.notify((int) (System.currentTimeMillis() % 12345), builder.build());
                        }
                    },2000);

                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(startMain);
                }
            });
            button_container.addView(b);
        }

        return rootView;
    }

    private static void refreshDiaryFragment() {
        updateRemainingNotifications();

        if (remainingNotifications.size() == 0) {
            Log.d(TAG, "no more stuff");
            ((LinearLayout) curRootView.findViewById(R.id.diary_parent_view)).removeAllViews();
            ((LinearLayout) curRootView.findViewById(R.id.diary_parent_view)).addView(emptyView);
            curRootView.findViewById(R.id.diary_parent_view).invalidate();
            return;
        }
        content_inputted = false;
        timing_inputted = false;

        notification_app_name.setText(AppManagement.getApplicationNameFromPackage(context, remainingNotifications.get(0).application_package));
        notification_message.setText(remainingNotifications.get(0).message);
        notification_title.setText(remainingNotifications.get(0).title);
        notification_timestamp.setText(AppManagement.getDate(context, remainingNotifications.get(0).seen_timestamp));

        notification_app_name.invalidate();
        notification_message.invalidate();
        notification_title.invalidate();
        notification_timestamp.invalidate();

        content_value.setRating(0F);
        timing_value.setRating(0F);

        content_unsure_button.setChecked(false);
        timing_unsure_button.setChecked(false);

        next_button.setEnabled(content_inputted & timing_inputted);
        next_button.invalidate();

        notification_layout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_in_right));

        Log.d(TAG, "skipcount: " + SKIP_COUNT);
        if (SKIP_COUNT < 2) {
            button_container.removeView(skipall_layout);
            skip_included = false;
        }
        else if (!skip_included) {
            button_container.addView(skipall_layout);
            skip_included = true;
        }
    }

    static List<UnsyncedNotification> remainingNotifications = new ArrayList<>();
    private static void updateRemainingNotifications() {
        remainingNotifications = fetchRemainingNotifications();
        notifications_remaining.setText(remainingNotifications.size() +  " Notifications remaining.");
    }

    private static List<UnsyncedNotification> fetchRemainingNotifications() {
        UnsyncedData helper = new UnsyncedData(context);
        return helper.getUnlabeledNotifications();
    }

    private static void skipAll(String package_name) {
        ArrayList<UnsyncedNotification> removed = new ArrayList<>();
        for (UnsyncedNotification n : remainingNotifications) {
            if (n.application_package.equals(package_name)) {
                removed.add(n);
                ContentValues updated_values = new ContentValues();
                updated_values.put(UnsyncedData.Notifications_Table.labeled, -1);
                UnsyncedData helper = new UnsyncedData(context);
                helper.updateEntry((int) (n.sqlite_row_id), updated_values);
            }
        }
        remainingNotifications.removeAll(removed);
        SKIP_COUNT = 0;
        refreshDiaryFragment();
    }

    public static View generatePredictionView(final LayoutInflater inflater, final ViewGroup container) {
        sharedInflater = inflater;
        sharedContainer = container;

        final View rootView = inflater.inflate(R.layout.prediction_view_disabled, container, false);

        Intent srvIntent = new Intent(context, ContentAnalysisService.class);
        context.startService(srvIntent);

        return rootView;
    }

    public static View generateHelpView(final LayoutInflater inflater, final ViewGroup container) {
        sharedInflater = inflater;
        sharedContainer = container;

        final View rootView = inflater.inflate(R.layout.help_view, container, false);

        return rootView;
    }

    private static PlaceholderFragment curFragment;
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            curFragment = PlaceholderFragment.newInstance(position + 1);
            return curFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "DIARY";
                case 1:
                    return "PREDICTIONS";
                case 2:
                    return "HELP";
            }
            return null;
        }
    }


    public static boolean isAccessibilityServiceActive(Context c) {
        if (!isAccessibilityEnabled(c)) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c);
            mBuilder.setSmallIcon(R.drawable.ic_notif_icon);
            mBuilder.setContentTitle("Please enable Notification Diary");
            mBuilder.setContentText(c.getResources().getString(R.string.aware_activate_accessibility));
            mBuilder.setAutoCancel(true);
            mBuilder.setOnlyAlertOnce(true); //notify the user only once
            mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);

            Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            accessibilitySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent clickIntent = PendingIntent.getActivity(c, 0, accessibilitySettings, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(clickIntent);
            NotificationManager notManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            notManager.notify(Applications.ACCESSIBILITY_NOTIFICATION_ID, mBuilder.build());

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent restart = new Intent(context, MainTabs.class);
                    context.startActivity(restart);
                }
            }, 30000);
            Toast.makeText(context, "Please enable accessibility services. Automatically restarting application in 30 seconds..", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private synchronized static boolean isAccessibilityEnabled(Context c) {
        boolean enabled = false;

        AccessibilityManager accessibilityManager = (AccessibilityManager) c.getSystemService(ACCESSIBILITY_SERVICE);

        //Try to fetch active accessibility services directly from Android OS database instead of broken API...
        String settingValue = android.provider.Settings.Secure.getString(c.getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            Log.d("ACCESSIBILITY", "Settings secure: " + settingValue);
            if (settingValue.contains(c.getPackageName())) {
                enabled = true;
            }
        }
        if (!enabled) {
            try {
                List<AccessibilityServiceInfo> enabledServices = AccessibilityManagerCompat.getEnabledAccessibilityServiceList(accessibilityManager, AccessibilityEventCompat.TYPES_ALL_MASK);
                if (!enabledServices.isEmpty()) {
                    for (AccessibilityServiceInfo service : enabledServices) {
                        Log.d("ACCESSIBILITY", "AccessibilityManagerCompat enabled: " + service.toString());
                        if (service.getId().contains(c.getPackageName())) {
                            enabled = true;
                            break;
                        }
                    }
                }
            } catch (NoSuchMethodError e) {
            }
        }
        if (!enabled) {
            try {
                List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
                if (!enabledServices.isEmpty()) {
                    for (AccessibilityServiceInfo service : enabledServices) {
                        Log.d("ACCESSIBILITY", "AccessibilityManager enabled: " + service.toString());
                        if (service.getId().contains(c.getPackageName())) {
                            enabled = true;
                            break;
                        }
                    }
                }
            } catch (NoSuchMethodError e) {
            }
        }

        //Keep the global setting up-to-date
        Aware.setSetting(c, Applications.STATUS_AWARE_ACCESSIBILITY, enabled, "com.aware.phone");

        return enabled;
    }

}
