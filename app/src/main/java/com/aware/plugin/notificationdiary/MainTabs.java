package com.aware.plugin.notificationdiary;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
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
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Locations;
import com.aware.plugin.notificationdiary.ContentAnalysis.EvaluationResult;
import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;
import com.aware.plugin.notificationdiary.Providers.J48Classifiers;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;
import com.aware.ui.PermissionsHandler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class MainTabs extends AppCompatActivity {

    private static final String TAG = "MainTabs";

    public static final String START_WITH_TAB = "START_WITH_TAB";
    public static final String DIARY_TAB = "DIARY_TAB";
    public static final String PREDICTION_TAB = "PREDICTION_TAB";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    private Context context;

    private static ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private Toolbar toolbar;

    private static boolean DEBUG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tabs);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this, this);

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
                    case 3:
                        toolbar.setTitle("Settings");
                        break;
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        context = this;

        AppManagement.init(this);

        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_NETWORK_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.VIBRATE);

        // change page to prediction tab if needed
        if (getIntent() != null & getIntent().hasExtra(START_WITH_TAB)) {
            if (getIntent().getStringExtra(START_WITH_TAB).equals(PREDICTION_TAB))  mViewPager.setCurrentItem(1);
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        PackageManager pm = getPackageManager();
        boolean allPermissionsOk = true;
        for (String perm : REQUIRED_PERMISSIONS) {
            int hasPerm = pm.checkPermission(
                    perm,
                    getPackageName());
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
            Intent startAware = new Intent(this, Aware.class);
            startService(startAware);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!Aware.isStudy(context)) Aware.joinStudy(context, "https://api.awareframework.com/index.php/webservice/index/980/5jg027moJgWg");
                    Aware.startAWARE();

                    Aware.setSetting(context, Applications.STATUS_AWARE_ACCESSIBILITY, true);
                    Aware.setSetting(context, Aware_Preferences.STATUS_APPLICATIONS, true);
                    Aware.setSetting(context, Aware_Preferences.STATUS_LOCATION_GPS, true);
                    Aware.setSetting(context, Aware_Preferences.STATUS_LOCATION_NETWORK, true);

                    Aware.startApplications(context);
                    Aware.startBattery(context);
                    Aware.startScreen(context);
                    Aware.startNetwork(context);
                    Aware.startLocations(context);

                    Aware.startPlugin(context, "com.aware.plugin.google.fused_location");
                    // "start" this "plugin" so AWARE understands its running and syncs the data
                    Aware.startPlugin(context, "com.aware.plugin.notificationdiary");

                    isAccessibilityServiceActive(context);

                    SharedPreferences sp = getSharedPreferences(AppManagement.SHARED_PREFS, MODE_PRIVATE);
                    int test_count = sp.getInt(AppManagement.TEST_COUNT, 0);
                    if (test_count <= 5) {
                        AppManagement.enablePredictions(context, false);
                        AppManagement.setOwnNotificationsHidden(context, false);
                        AppManagement.setSoundControlAllowed(context, true);
                        Toast.makeText(context, "Please change foreground application to test application functionality..", Toast.LENGTH_LONG).show();
                    }

                    Intent service = new Intent(context, NotificationListener.class);
                    startService(service);
                }
            }, 500);

        }
        else {
            Aware.setSetting(this, Applications.STATUS_AWARE_ACCESSIBILITY, false);
            Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, false);
            Aware.setSetting(this, Aware_Preferences.STATUS_LOCATION_GPS, false);
            Aware.setSetting(this, Aware_Preferences.STATUS_LOCATION_NETWORK, false);

            Aware.stopBattery(this);
            Aware.stopScreen(this);
            Aware.stopNetwork(this);
            Aware.stopLocations(this);
            Aware.stopPlugin(this, "com.aware.plugin.google.fused_location");
            Toast.makeText(this, "Please allow all permissions and restart application.", Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "plugin set to on " + Aware.getSetting(this, Settings.STATUS_PLUGIN_NOTIFICATIONDIARY));
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (progressReceiver != null) unregisterReceiver(progressReceiver);
        }
        catch (Exception e) {e.printStackTrace();}
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

        else if (id == R.id.action_settings) {
            mViewPager.setCurrentItem(3);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    TextView notifications_remaining;

    RelativeLayout notification_layout;

    ToggleButton content_unsure_button;
    ToggleButton timing_unsure_button;
    RatingBar content_value;
    RatingBar timing_value;
    Button content_help_button;
    Button timing_help_button;
    Button skip_button;
    Button next_button;
    TextView notification_title;
    TextView notification_app_name;
    TextView notification_message;
    TextView notification_timestamp;

    Button skip_all_button;

    private static int curFragmentNumber = 1;

    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static int curSectionNumber;

        private MainTabs activity;
        private Context context;

        public PlaceholderFragment() {
        }

        public void setContext(Context c) {this.context = c;}

        public void setActivity(MainTabs a) {this.activity = a;}

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, Context c, MainTabs a) {
            curSectionNumber = sectionNumber;
            PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.setContext(c);
            fragment.setActivity(a);

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
                    return activity.generateDiaryView(context, inflater, container);
                case 2:
                    return activity.generatePredictionView(context,inflater, container);
                case 3:
                    return activity.generateHelpView(context,inflater, container);
                case 4:
                    return activity.generateSettingsView(context, inflater, container);
                default:
                    return activity.generateDiaryView(context, inflater, container);
            }
        }
    }

    private static boolean content_inputted = false;
    private static boolean timing_inputted = false;

    private View emptyView;
    private View curRootView;
    private RelativeLayout button_container;
    private LinearLayout skipall_layout;

    public View generateDiaryView(Context c, final LayoutInflater inflater, final ViewGroup container) {
        Log.d(TAG, "Generating new diary view");

        remainingNotifications = fetchRemainingNotifications(c);
        emptyView = inflater.inflate(R.layout.diary_view_empty, container, false);
        if (remainingNotifications.size() == 0) {
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
                                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
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
                ((LinearLayout) emptyView.findViewById(R.id.header)).addView(b);
            }
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
        skip_button.setOnClickListener(new ContextButtonListener(c) {
            @Override
            public void onClick(View view) {
                ContentValues updated_values = new ContentValues();
                updated_values.put(UnsyncedData.Notifications_Table.labeled, -1);
                UnsyncedData helper = new UnsyncedData(context);
                helper.updateEntry((int) remainingNotifications.get(0).sqlite_row_id, updated_values, false);

                if (!AppManagement.predictionsEnabled(context)) {
                    UnsyncedNotification n = helper.get(remainingNotifications.get(0).sqlite_row_id, true);
                    getContentResolver().insert(com.aware.plugin.notificationdiary.Providers.Provider.Notifications_Data.CONTENT_URI, n.toSyncableContentValues());
                }

                remainingNotifications.remove(0);
                notification_layout.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_out));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshDiaryFragment(context);
                    }
                }, 400);
            }
        });

        next_button = (Button) rootView.findViewById(R.id.diary_next);
        next_button.setOnClickListener(new ContextButtonListener(c) {
            @Override
            public void onClick(View view) {
                ContentValues updated_values = new ContentValues();
                updated_values.put(UnsyncedData.Notifications_Table.labeled, 1);
                updated_values.put(UnsyncedData.Notifications_Table.content_importance, content_value.getRating());
                updated_values.put(UnsyncedData.Notifications_Table.timing, timing_value.getRating());
                UnsyncedData helper = new UnsyncedData(context);
                helper.updateEntry((int) remainingNotifications.get(0).sqlite_row_id, updated_values, false);

                if (!AppManagement.predictionsEnabled(context)) {
                    UnsyncedNotification n = helper.get(remainingNotifications.get(0).sqlite_row_id, true);
                    getContentResolver().insert(com.aware.plugin.notificationdiary.Providers.Provider.Notifications_Data.CONTENT_URI, n.toSyncableContentValues());
                }

                remainingNotifications.remove(0);

                content_inputted = false;
                timing_inputted = false;

                notification_layout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_out_left));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshDiaryFragment(context);
                    }
                }, 400);
            }
        });

        skip_all_button = (Button) rootView.findViewById(R.id.diary_skip_all);
        skip_all_button.setOnClickListener(new ContextButtonListener(context) {
            @Override
            public void onClick(View view) {
                notification_layout.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_out));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        skipAll(remainingNotifications.get(0).application_package, context);
                    }
                },400);
            }
        });

        curRootView = rootView;

        refreshDiaryFragment(c);

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
                                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
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

    private void refreshDiaryFragment(Context c) {
        updateRemainingNotifications(c);

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
    }

    static List<UnsyncedNotification> remainingNotifications = new ArrayList<>();
    private void updateRemainingNotifications(Context c) {
        remainingNotifications = fetchRemainingNotifications(c);
        notifications_remaining.setText(remainingNotifications.size() +  " Notifications remaining.");
    }

    private List<UnsyncedNotification> fetchRemainingNotifications(Context c) {
        UnsyncedData helper = new UnsyncedData(c);
        return helper.getUnlabeledNotifications(true);
    }

    private void skipAll(String package_name, Context c) {
        ArrayList<UnsyncedNotification> removed = new ArrayList<>();
        for (UnsyncedNotification n : remainingNotifications) {
            if (n.application_package.equals(package_name)) {
                removed.add(n);
                ContentValues updated_values = new ContentValues();
                updated_values.put(UnsyncedData.Notifications_Table.labeled, -1);
                UnsyncedData helper = new UnsyncedData(c);
                helper.updateEntry((int) (n.sqlite_row_id), updated_values, false);
            }
        }
        remainingNotifications.removeAll(removed);
        refreshDiaryFragment(c);
    }

    // disabled view
    Button enable_predictions;
    RoundCornerProgressBar classifier_progress;
    TextView classifier_progress_text;

    // enabled view
    Button refresh_model;
    Button launch_pred_act;
    Button disable_pred;
    TextView model_accuracy;
    TextView accuracy_description;
    LinearLayout accuracy_visualisation;

    ClassifierProgressReceiver progressReceiver;

    public View generatePredictionView(final Context c, final LayoutInflater inflater, final ViewGroup container) {
        if (AppManagement.predictionsEnabled(c)) {
            final View rootView = inflater.inflate(R.layout.prediction_view_enabled, container, false);

            refresh_model = (Button) rootView.findViewById(R.id.refresh_model);
            launch_pred_act = (Button) rootView.findViewById(R.id.launch_pred_act);

            disable_pred = (Button) rootView.findViewById(R.id.disable_predictions);
            model_accuracy = (TextView) rootView.findViewById(R.id.model_accuracy);
            accuracy_description = (TextView) rootView.findViewById(R.id.accuracy_description);
            accuracy_visualisation = (LinearLayout) rootView.findViewById(R.id.accuracy_visualisation);

            classifier_progress = (RoundCornerProgressBar) rootView.findViewById(R.id.classifier_progress);
            classifier_progress.setVisibility(View.INVISIBLE);

            classifier_progress_text = (TextView) rootView.findViewById(R.id.classifier_progress_text);
            classifier_progress.setVisibility(View.INVISIBLE);

            refresh_model.setOnClickListener(new ContextButtonListener(c) {
                @Override
                public void onClick(View v) {
                    new Handler().postDelayed(new ContextRunnable(c) {
                        @Override
                        public void run() {
                            refresh_model.setEnabled(false);
                            progressReceiver = new ClassifierProgressReceiver(classifier_progress, classifier_progress_text, c, refresh_model);
                            IntentFilter filt = new IntentFilter();
                            filt.addAction(ClassifierProgressReceiver.ACTION);
                            c.registerReceiver(progressReceiver, filt);

                            classifier_progress.setVisibility(View.VISIBLE);
                            classifier_progress_text.setVisibility(View.VISIBLE);

                            Intent srvIntent = new Intent(c, ContentAnalysisService.class);
                            c.startService(srvIntent);
                        }
                    }, 500);
                }
            });

            launch_pred_act.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent predAct = new Intent(c, PredictionActivity.class);
                    startActivity(predAct);
                }
            });

            disable_pred.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppManagement.enablePredictions(c, false);
                    Intent restartMain = new Intent(context, MainTabs.class);
                    restartMain.putExtra(MainTabs.START_WITH_TAB, MainTabs.PREDICTION_TAB);
                    startActivity(restartMain);
                }
            });

            J48Classifiers modelInfo = new J48Classifiers(c);
            EvaluationResult curResult = modelInfo.getCurrentClassifier();

            model_accuracy.setText(new DecimalFormat("#.0").format(curResult.accuracy*100) + "%");

            ViewGroup.LayoutParams params = accuracy_visualisation.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) (curResult.accuracy*160), getResources().getDisplayMetrics());
            accuracy_visualisation.setLayoutParams(params);
            if (curResult.accuracy < .8) accuracy_visualisation.setBackgroundColor(ContextCompat.getColor(c, R.color.accent_yellow));
            if (curResult.accuracy < .6) accuracy_visualisation.setBackgroundColor(ContextCompat.getColor(c, R.color.accent_red));

            DecimalFormat df = new DecimalFormat("#.0");

            accuracy_description.setText(
                    Html.fromHtml("The overall accuracy of the model is <b>" + df.format(curResult.accuracy*100) + "%</b>. " +
                    "It has approximately <b>" + df.format(curResult.hide_false_positive*100) + "%</b> probability of mistakenly hiding arriving notification and <b>" +
                    df.format(curResult.show_false_positive*100) + "%</b> probability of showing an unwanted notification. " +
                    "Finally, using the Kappa statistic the model is approximately <b>" + df.format(curResult.kappa*100) + "%</b> better at prediction than a random guess.")
            );

            modelInfo.close();

            return rootView;
        }
        else {
            final View rootView = inflater.inflate(R.layout.prediction_view_disabled, container, false);

            enable_predictions = (Button) rootView.findViewById(R.id.predictions_enable);
            enable_predictions.setOnClickListener(new ContextButtonListener(c) {
                @Override
                public void onClick(View v) {
                    new Handler().postDelayed(new ContextRunnable(c) {
                        @Override
                        public void run() {
                            progressReceiver = new ClassifierProgressReceiver(classifier_progress, classifier_progress_text, c, refresh_model);
                            IntentFilter filt = new IntentFilter();
                            filt.addAction(ClassifierProgressReceiver.ACTION);
                            c.registerReceiver(progressReceiver, filt);

                            classifier_progress.setVisibility(View.VISIBLE);
                            classifier_progress_text.setVisibility(View.VISIBLE);

                            Intent srvIntent = new Intent(c, ContentAnalysisService.class);
                            c.startService(srvIntent);
                        }
                    }, 500);
                }
            });
            UnsyncedData helper = new UnsyncedData(c);
            int training_data_amount = helper.getNumOfTrainingData();
            if (training_data_amount >= 100) enable_predictions.setEnabled(true);
            if (DEBUG) enable_predictions.setEnabled(true);

            classifier_progress = (RoundCornerProgressBar) rootView.findViewById(R.id.classifier_progress);
            classifier_progress_text = (TextView) rootView.findViewById(R.id.classifier_progress_text);

            if (progressReceiver != null && progressReceiver.progress == 0) {
                classifier_progress.setVisibility(View.INVISIBLE);
                classifier_progress_text.setVisibility(View.INVISIBLE);
            }
            else if (progressReceiver != null) {
                classifier_progress.setProgress((float) progressReceiver.progress);
                classifier_progress.setSecondaryProgress((float) progressReceiver.progress + 10);

                classifier_progress_text.setText(progressReceiver.progress + "% complete");
            }

            classifier_progress.setProgress((float) training_data_amount);
            classifier_progress_text.setText(training_data_amount + " / 100 labeled notifications");

            return rootView;
        }

    }

    public View generateHelpView(Context context, final LayoutInflater inflater, final ViewGroup container) {

        final View rootView = inflater.inflate(R.layout.help_view, container, false);

        return rootView;
    }

    CheckBox notifications_hidden;
    CheckBox sound_control_allowed;
    public View generateSettingsView(final Context context, final LayoutInflater inflater, final ViewGroup container) {
        final View rootView = inflater.inflate(R.layout.settings, container, false);
        notifications_hidden = (CheckBox) rootView.findViewById(R.id.settings_selfnotification);
        notifications_hidden.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AppManagement.setOwnNotificationsHidden(context, b);
            }
        });
        sound_control_allowed = (CheckBox) rootView.findViewById(R.id.settings_volume);
        sound_control_allowed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AppManagement.setSoundControlAllowed(context, b);
            }
        });

        notifications_hidden.setChecked(AppManagement.getOwnNotificationsHidden(context));
        sound_control_allowed.setChecked(AppManagement.getSoundControlAllowed(context));

        return rootView;
    }

    private PlaceholderFragment curFragment;
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private Context context;
        private MainTabs activity;

        public SectionsPagerAdapter(FragmentManager fm, Context c, MainTabs a) {
            super(fm);
            context = c;
            activity = a;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            curFragment = PlaceholderFragment.newInstance(position + 1, context, activity);
            return curFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
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
                case 3:
                    return "SETTINGS";
            }
            return null;
        }
    }


    public static boolean isAccessibilityServiceActive(final Context c) {
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

            Toast.makeText(c, "Please enable accessibility services and restart application..", Toast.LENGTH_LONG).show();
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
