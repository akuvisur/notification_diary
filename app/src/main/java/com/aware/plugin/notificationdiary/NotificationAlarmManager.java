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
import android.widget.Toast;

import com.aware.Aware;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.Date;

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
    BroadcastReceiver modeChangeReceiver = null;
    CallReceiver callReceiver = null;

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Log.d(TAG, "alarm manager created");
    }

    private long lastCue = System.currentTimeMillis();

    boolean private_ringer_change = false;
    boolean ringer_mode_change_waiting = false;

    SettingsContentObserver mSettingsContentObserver;
    @Override
    public int onStartCommand(Intent intent, int flags, int something) {
        super.onStartCommand(intent, flags, something);
        Log.d(TAG, "alarm manager started");

        if (!AppManagement.getSoundControlAllowed(this)) {
            Intent stopService = new Intent(this, NotificationAlarmManager.class);
            stopService(stopService);
        }

        if (ringerReceiver == null) ringerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                main_selector:
                if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {

                    // if sound control not allowed but this service still running for reason x
                    if (!AppManagement.getSoundControlAllowed(context)) break main_selector;
                    // if ringer mode change was due to this application, dont override default mode
                    if (private_ringer_change) break main_selector;
                    else {
                        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        Log.d(TAG, "storing new ringer mode: " + am.getRingerMode());
                        AppManagement.storeNewRingerMode(context, am.getRingerMode(), am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));

                        if (!ringer_mode_change_waiting) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!AppManagement.getSoundControlAllowed(context)) return;
                                    mute();
                                }
                            }, 10000);
                        }
                        ringer_mode_change_waiting = true;

                        Log.d(TAG, "vibrate mode: " + am.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION));
                    }
                }
                else if (intent.getAction().equals(SEND_NOTIFICATION_CUE)) {

                    Log.d(TAG, "RECEIVED CUE EMITTER THING");
                    private_ringer_change = true;
                    if ((System.currentTimeMillis() - lastCue) > 2500) {
                        Log.d(TAG, "no recent cues, emitting new!");
                        lastCue = System.currentTimeMillis();

                        final AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
                        Uri notification_sound = null;
                        long[] vibrate_settings = null;
                        if (intent.hasExtra(NOTIFICATION_SOUND_URI)) notification_sound = (Uri) intent.getParcelableExtra(NOTIFICATION_SOUND_URI);
                        if (intent.hasExtra(NOTIFICATION_VIBRATE)) vibrate_settings = intent.getLongArrayExtra(NOTIFICATION_VIBRATE);
                        int ringer_mode = AppManagement.getRingerMode(context);
                        if (ringer_mode == AudioManager.RINGER_MODE_NORMAL) {
                            Log.d(TAG, "ringer mode was normal");
                            private_ringer_change = true;
                            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AppManagement.getSoundVolume(context), 0);
                            am.setRingerMode(ringer_mode);
                            Log.d(TAG, "notification_sound: " + notification_sound);
                            if (notification_sound != null) sendSound(notification_sound);
                            else if (vibrate_settings != null) {
                                vibrate(vibrate_settings, 1);
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mute();
                                }
                            }, 500);
                        } else if (ringer_mode == AudioManager.RINGER_MODE_VIBRATE) {
                            Log.d(TAG, "ringer mode was vibrate, vibrating");
                            private_ringer_change = true;
                            am.setRingerMode(ringer_mode);
                            if (notification_sound != null && vibrate_settings != null) vibrate(vibrate_settings, 1);
                            else if (vibrate_settings == null) vibrate(new long[]{250}, 1);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mute();
                                }
                            }, 500);
                        }
                        else if (ringer_mode == AudioManager.RINGER_MODE_SILENT) {
                            Log.d(TAG, "ringer mode was silent");
                            // no cue if silent;
                        }
                    }
                    private_ringer_change = false;
                }
            }
        };

        modeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.hasExtra(ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY)) {
                    AppManagement.storeNewRingerMode(context,
                            intent.getIntExtra(RINGER_MODE, AudioManager.RINGER_MODE_VIBRATE),
                            intent.getIntExtra(SOUND_VOLUME, 50));
                }
            }
        };

        callReceiver = new CallReceiver();

        IntentFilter callFilter = new IntentFilter();
        callFilter.addAction("android.intent.action.PHONE_STATE");

        registerReceiver(callReceiver, callFilter);

        IntentFilter filt = new IntentFilter();
        filt.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filt.addAction(SEND_NOTIFICATION_CUE);
        filt.addAction(ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY);

        registerReceiver(ringerReceiver, filt);

        mute();

        Aware.startScheduler(this);

        try {
            Scheduler.Schedule sch = Scheduler.getSchedule(this, "restart_alarmmanager");
            if (sch == null) {
                sch = new Scheduler.Schedule("restart_alarmmanager");
                sch.setActionClass(getPackageName() + "/" + getClass().getName())
                        .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                        .setInterval(15);
                Scheduler.saveSchedule(this, sch);
            } else {}

        }
        catch (JSONException e) {
            e.printStackTrace();
        }

//
//        if (statusMonitor == null) { //not set yet
//            statusMonitor = new Intent(this, NotificationAlarmManager.class);
//            statusMonitor.setAction(ACTION_KEEP_ALIVE);
//            repeatingIntent = PendingIntent.getService(getApplicationContext(), 0, statusMonitor, PendingIntent.FLAG_UPDATE_CURRENT);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
//                        System.currentTimeMillis() + AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
//                        repeatingIntent);
//            } else {
//                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
//                        System.currentTimeMillis() + AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
//                        AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
//                        repeatingIntent);
//            }
//        } else { //already set, schedule the next one if API23+. If < API23, it's a repeating alarm, so no need to set it again.
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_KEEP_ALIVE))) {
//                //set the alarm again to the future for API 23, works even if under Doze
//                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
//                        System.currentTimeMillis() + AppManagement.PREF_FREQUENCY_WATCHDOG * 1000,
//                        repeatingIntent);
//            }
//        }
//
//
//        //new ToastRunnable(this, "Notification Diary: Alarm Manager initiated", Toast.LENGTH_SHORT).run();
//
//
//
        mSettingsContentObserver = new SettingsContentObserver( new Handler() );
        this.getApplicationContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                mSettingsContentObserver );

        return START_STICKY;
    }

    private void mute() {
        private_ringer_change = true;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new ToastRunnable(context, "Notification Diary set ringer mode to silent until next call or notification", Toast.LENGTH_LONG));
        if (AppManagement.getSoundControlAllowed(this)) {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            private_ringer_change = false;
        }
    }

    private void vibrate(long[] duration, int repeats) {
        private_ringer_change = true;
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(duration, repeats);
    }

    private void sendSound(Uri sound) {
        private_ringer_change = true;
        RingtoneManager.getRingtone(context, sound).play();
    }

    @Override
    public void onDestroy() {
        if (ringerReceiver != null) unregisterReceiver(ringerReceiver);
        if (callReceiver != null) unregisterReceiver(callReceiver);
        if (mSettingsContentObserver != null) this.getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
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
            private_ringer_change = true;
            if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        AppManagement.storeNewRingerMode(context, am.getRingerMode(), am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
                        mute();
                    }
                },2000);
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                int ringer_mode = AppManagement.getRingerMode(context);
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.setRingerMode(ringer_mode);
            }
            private_ringer_change = false;
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
            final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AppManagement.storeNewRingerMode(context, AppManagement.getRingerMode(context), am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
        }
    }
}
