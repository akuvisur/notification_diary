package com.aware.plugin.notificationdiary;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by aku on 02/12/16.
 */

public class NotificationAlarmManager extends Service {

    BroadcastReceiver ringerReceiver;

    @Override
    public void onCreate() {

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

    @Override
    public int onStartCommand(Intent intent, int flags, int something) {

        ringerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        IntentFilter filt = new IntentFilter();
        filt.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(ringerReceiver, filt);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // notification sound level
        am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        // ringer mode
        am.getRingerMode();

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
