package com.aware.plugin.notificationdiary;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.aware.plugin.notificationdiary.ContentAnalysis.ClusterGenerator;
import com.aware.plugin.notificationdiary.ContentAnalysis.Graph;

public class ContentAnalysisService extends Service {
    private final String TAG = "AnalysisService";

    private final int NUM_CLUSTERS = 10;

    private Context context;

    public ContentAnalysisService() {
        context = this;
    }

    @Override
    public int onStartCommand(Intent i, int i1, int it2) {
        Log.d(TAG, "started");
        new Handler().post(new CoreRunnable(context));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class CoreRunnable implements Runnable {
        Context context;

        public CoreRunnable(Context c1) {
            this.context = c1;
        }

        @Override
        public void run() {
            Log.d(TAG, "running core runnable");
            new ClusterGenerator(new Graph(context).getNodes(), NUM_CLUSTERS);
        }
    }

}
