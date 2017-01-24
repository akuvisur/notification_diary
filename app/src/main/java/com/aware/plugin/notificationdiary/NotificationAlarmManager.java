package com.aware.plugin.notificationdiary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import static com.aware.plugin.notificationdiary.NotificationListener.SEND_NOTIFICATION_CUE;

/**
 * Created by aku on 02/12/16.
 */

public class NotificationAlarmManager extends Service {
    private static final String TAG = "NotifAlarmManager";

    private static final String ACTION_KEEP_ALIVE = "ACTION_KEEP_ALIVE";

    private static AlarmManager alarmManager = null;
    private static PendingIntent repeatingIntent = null;
    private static Intent statusMonitor = null;

    public static final String ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY = "ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY";
    public static final String NOTIFICATION_SOUND_URI = "NOTIFICATION_SOUND_URI";
    public static final String NOTIFICATION_VIBRATE = "NOTIFICATION_VIBRATE";

    public static final String CHANGE_RINGER_MODE = "CHANGE_RINGER_MODE";
    public static final String RINGER_MODE = "STORED_RINGER_MODE";
    public static final String SOUND_VOLUME = "STORED_SOUND_VOLUME";

    BroadcastReceiver ringerReceiver = null;
    CallReceiver callReceiver = null;

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Log.d(TAG, "alarm manager created");
        private_ringer_change = System.currentTimeMillis();
    }

    private long lastCue = System.currentTimeMillis();

    private final int RINGER_CHANGE_DELAY = 10000;
    long private_ringer_change = 0;
    boolean ringer_mode_change_waiting = false;

    SettingsContentObserver mSettingsContentObserver;
    @Override
    public int onStartCommand(Intent intent, int flags, int something) {
        super.onStartCommand(intent, flags, something);
        Log.d(TAG, "alarm manager started");

        if (!AppManagement.getSoundControlAllowed(this)) {
            Intent stopService = new Intent(this, NotificationAlarmManager.class);
            stopService(stopService);
            return START_NOT_STICKY;
        }

        else {
            if (ringerReceiver == null) ringerReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, Intent intent) {
                    if (!AppManagement.getSoundControlAllowed(context)) return;
                    main_selector:
                    if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                        // if sound control not allowed but this service still running for reason x
                        if (!AppManagement.getSoundControlAllowed(context)) break main_selector;
                        // if at least second since the last private ringer change, store this mode as the default mode
                        if (System.currentTimeMillis() - private_ringer_change > 1000) {
                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            store(am.getRingerMode());
                            if (!ringer_mode_change_waiting) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mute();
                                    }
                                }, RINGER_CHANGE_DELAY);
                            }
                            ringer_mode_change_waiting = true;
                        }

                    }
                    // calendar reminder
                    else if (intent.getAction().equals("EVENT_REMINDER_ACTION")) {
                        private_ringer_change = System.currentTimeMillis();
                        int ringer_mode = AppManagement.getRingerMode(context);
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.setRingerMode(ringer_mode);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mute();
                            }
                        }, RINGER_CHANGE_DELAY);
                    }
                    // clock alarm
                    else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                        private_ringer_change = System.currentTimeMillis();
                        int ringer_mode = AppManagement.getRingerMode(context);
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.setRingerMode(ringer_mode);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mute();
                            }
                        }, RINGER_CHANGE_DELAY);
                    } else if (intent.getAction().equals(SEND_NOTIFICATION_CUE)) {
                        if ((System.currentTimeMillis() - lastCue) > 2500) {
                            Log.d(TAG, "no recent cues, emitting new!");
                            private_ringer_change = System.currentTimeMillis();
                            lastCue = System.currentTimeMillis();
                            Uri notification_sound = null;
                            long[] vibrate_settings = null;
                            if (intent.hasExtra(NOTIFICATION_SOUND_URI))
                                notification_sound = intent.getParcelableExtra(NOTIFICATION_SOUND_URI);
                            if (intent.hasExtra(NOTIFICATION_VIBRATE))
                                vibrate_settings = intent.getLongArrayExtra(NOTIFICATION_VIBRATE);
                            int ringer_mode = AppManagement.getRingerMode(context);
                            if (ringer_mode == AudioManager.RINGER_MODE_NORMAL) {
                                Log.d(TAG, "ringer mode was normal");
                                if (notification_sound != null) sendSound(notification_sound);
                                else if (vibrate_settings != null) {
                                    vibrate(vibrate_settings, 1);
                                } else vibrate(250);
                            } else if (ringer_mode == AudioManager.RINGER_MODE_VIBRATE) {
                                Log.d(TAG, "ringer mode was vibrate, vibrating");
                                if (notification_sound != null && vibrate_settings != null)
                                    vibrate(vibrate_settings, 1);
                                else if (vibrate_settings == null) vibrate(250);
                            } else if (ringer_mode == AudioManager.RINGER_MODE_SILENT) {
                                Log.d(TAG, "ringer mode was silent");
                                // no cue if silent;
                            }
                        }
                    }
                }
            };

            callReceiver = new CallReceiver();

            IntentFilter callFilter = new IntentFilter();
            callFilter.addAction("android.intent.action.PHONE_STATE");
            // change ringer mode on calls
            registerReceiver(callReceiver, callFilter);

            IntentFilter filt = new IntentFilter();
            filt.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            filt.addAction(SEND_NOTIFICATION_CUE);
            filt.addAction(ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY);
            // change ringer mode calendar events
            filt.addAction("EVENT_REMINDER_ACTION");
            // change ringer mode clock alarms
            filt.addAction(Intent.ACTION_TIME_TICK);

            registerReceiver(ringerReceiver, filt);

//        Aware.startScheduler(this);
//
//        try {
//            Scheduler.Schedule sch = Scheduler.getSchedule(this, "restart_alarmmanager");
//            if (sch == null) {
//                sch = new Scheduler.Schedule("restart_alarmmanager");
//                sch.setActionClass(getPackageName() + "/" + getClass().getName())
//                        .setActionType(Scheduler.ACTION_TYPE_SERVICE)
//                        .setInterval(15);
//                Scheduler.saveSchedule(this, sch);
//            } else {}
//
//        }
//        catch (JSONException e) {
//            e.printStackTrace();
//        }

            if (statusMonitor == null) { //not set yet
                statusMonitor = new Intent(this, NotificationAlarmManager.class);
                statusMonitor.setAction(ACTION_KEEP_ALIVE);
                repeatingIntent = PendingIntent.getService(getApplicationContext(), 0, statusMonitor, PendingIntent.FLAG_UPDATE_CURRENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
                            repeatingIntent);
                } else {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
                            AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
                            repeatingIntent);
                }
            } else { //already set, schedule the next one if API23+. If < API23, it's a repeating alarm, so no need to set it again.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_KEEP_ALIVE))) {
                    //set the alarm again to the future for API 23, works even if under Doze
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
                            repeatingIntent);
                }
            }

            //new ToastRunnable(this, "Notification Diary: Alarm Manager initiated", Toast.LENGTH_SHORT).run();
            mSettingsContentObserver = new SettingsContentObserver(new Handler());
            this.getApplicationContext().getContentResolver().registerContentObserver(
                    android.provider.Settings.System.CONTENT_URI, true,
                    mSettingsContentObserver);

            mute();
        }
        return START_STICKY;
    }

    private void mute() {
        if (AppManagement.getSoundControlAllowed(this) && AppManagement.predictionsEnabled(this)) {
            // store current
            private_ringer_change = System.currentTimeMillis();

            // mute
            //Handler handler = new Handler(Looper.getMainLooper());
            //handler.post(new ToastRunnable(context, "Notification Diary set ringer mode to silent until next call or notification", Toast.LENGTH_LONG));
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);

            ringer_mode_change_waiting = false;
        }
    }

    private final long DEFAULT_MUTE_DELAY = 2000;
    private void vibrate(long duration) {
        try {
            Log.d(TAG, "CUE vibrating");
            private_ringer_change = System.currentTimeMillis();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            // turn vibration back on
            am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_ON);
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(duration);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mute();
                }
            }, DEFAULT_MUTE_DELAY);
        }
        catch (Exception e) {
            // do nothing
        }
    }

    private void vibrate(long[] duration, int repeats) {
        try {
            Log.d(TAG, "CUE vibrating");

            private_ringer_change = System.currentTimeMillis();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            // turn vibration back on
            am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_ON);
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(duration, repeats);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mute();
                }
            }, DEFAULT_MUTE_DELAY);
        }
        catch (Exception e) {
            // do nothing
        }
    }

    private void sendSound(Uri sound) {
        try {
            private_ringer_change = System.currentTimeMillis();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AppManagement.getSoundVolume(context), 0);

            am.setRingerMode(AppManagement.getRingerMode(context));
            Log.d(TAG, "CUE playing sound: " + sound.toString());
            RingtoneManager.getRingtone(context, sound).play();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mute();
                }
            }, DEFAULT_MUTE_DELAY);
        }
        catch (Exception e) {
            // do nothing
        }
    }

    private void store(int mode) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG, "storing new ringer mode: " + mode);
        AppManagement.storeNewRingerMode(context, mode, am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
    }

    @Override
    public void onDestroy() {
        if (ringerReceiver != null) unregisterReceiver(ringerReceiver);
        if (callReceiver != null) unregisterReceiver(callReceiver);
        if (mSettingsContentObserver != null) this.getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setRingerMode(AppManagement.getRingerMode(this));
        // turn vibration back on
        am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_ON);
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AppManagement.getSoundVolume(context), 0);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class CallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                private_ringer_change = System.currentTimeMillis();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mute();
                    }
                }, DEFAULT_MUTE_DELAY);
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                private_ringer_change = System.currentTimeMillis();
                int ringer_mode = AppManagement.getRingerMode(context);
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.setRingerMode(ringer_mode);
            }
        }
    }

    public class SettingsContentObserver extends ContentObserver {
        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            private_ringer_change = System.currentTimeMillis();
            final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AppManagement.storeNewRingerMode(context, null, am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
        }
    }
}
