package com.aware.plugin.notificationdiary.Predictions;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.aware.plugin.notificationdiary.AppManagement;
import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;

import java.util.ArrayList;

public class PredictionService extends Service {
    public static final String ACTION = "notification_received";

    private ArrayList<Cluster> clusters;

    private PredictionReceiver rec;
    public PredictionService() {
    }

    @Override
    public int onStartCommand(Intent i1, int i2, int i3) {
        clusters = AppManagement.getClusters(this);

        rec = new PredictionReceiver(this);
        IntentFilter filt = new IntentFilter();
        filt.addAction(ACTION);
        registerReceiver(rec, filt);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(rec);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class PredictionReceiver extends BroadcastReceiver {

        private Context context;
        public PredictionReceiver (Context c) {
            context = c;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
}
