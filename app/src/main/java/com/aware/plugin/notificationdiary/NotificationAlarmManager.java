package com.aware.plugin.notificationdiary;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import static com.aware.plugin.notificationdiary.NotificationListener.SEND_NOTIFICATION_CUE;

/**
 * Created by aku on 02/12/16.
 */

public class NotificationAlarmManager extends Service {
    private static final String TAG = "NotifAlarmManager";

    public static final String ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY = "ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY";
    public static final String NOTIFICATION_SOUND_URI = "NOTIFICATION_SOUND_URI";

    BroadcastReceiver ringerReceiver = null;

    @Override
    public void onCreate() {
        Log.d(TAG, "alarm manager created");
    }

    // TODO
    /*
        when service starts, store "default" ringer mode and notification stream volume
        when ringer_mode_changed_action occurs, and there is no additional extra
        telling that the broadcast was from this application, change the default values

        set up broadcast receiver to send a notificationCue with default values if needed, include
        extra telling that the change that notifies the change came from this application to not
        trigger anything multiple times, or set the default modes because of this app sending the
        cues

        set up another broadcast receiver to listen for incoming calls and notify the user normally
        with default values
     */
    private long lastCue = System.currentTimeMillis();

    boolean private_ringer_change = false;
    boolean ringer_mode_change_waiting = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int something) {
        Log.d(TAG, "alarm manager started");

        if (!AppManagement.getSoundControlAllowed(this)) {
            Intent stopService = new Intent(this, NotificationAlarmManager.class);
            stopService(stopService);
        }

        if (ringerReceiver == null) ringerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                    if (!intent.hasExtra(ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY)) {
                        // if sound control not allowed but this service still running for reason x
                        if (!AppManagement.getSoundControlAllowed(context)) return;
                        // if ringer mode change was due to this application, dont override default mode
                        if (private_ringer_change) return;
                        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        AppManagement.storeNewRingerMode(context, am.getRingerMode(), am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));

                        if (!ringer_mode_change_waiting) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    private_ringer_change = true;
                                    am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                                    am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.post(new ToastRunnable(context, "Notification Diary set ringer mode to silent", Toast.LENGTH_LONG));
                                    private_ringer_change = false;
                                    ringer_mode_change_waiting = false;
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

                        Uri notification_sound = (Uri) intent.getParcelableExtra(NOTIFICATION_SOUND_URI);
                        int ringer_mode = AppManagement.getRingerMode(context);
                        if (ringer_mode == AudioManager.RINGER_MODE_NORMAL) {

                            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AppManagement.getSoundVolume(context), 0);
                            am.setRingerMode(ringer_mode);
                            Log.d(TAG, "notification_sound: " + notification_sound);
                            if (notification_sound != null) RingtoneManager.getRingtone(context, notification_sound).play();

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
                                    am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                                }
                            }, 500);
                        } else if (ringer_mode == AudioManager.RINGER_MODE_VIBRATE) {
                            am.setRingerMode(ringer_mode);
                            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(250);
                        }
                        else if (ringer_mode == AudioManager.RINGER_MODE_SILENT) {
                            // no cue if silent;
                        }
                    }
                    private_ringer_change = false;
                }
            }
        };

        IntentFilter filt = new IntentFilter();
        filt.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filt.addAction(SEND_NOTIFICATION_CUE);
        filt.addAction(ACTION_MODE_CHANGED_FROM_NOTIFICATION_DIARY);
        registerReceiver(ringerReceiver, filt);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AppManagement.storeNewRingerMode(this, am.getRingerMode(), am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));

        am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
        Log.d(TAG, "vibrate mode: " + am.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION));

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        if (ringerReceiver != null) unregisterReceiver(ringerReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
