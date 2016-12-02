package com.aware.plugin.notificationdiary;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.aware.plugin.notificationdiary.ContentAnalysis.ClusterGenerator;
import com.aware.plugin.notificationdiary.ContentAnalysis.Graph;
import com.aware.plugin.notificationdiary.Providers.WordBins;

public class ContentAnalysisService extends Service {
    private final String TAG = "AnalysisService";

    final int NUM_CLUSTERS = 20;

    private Context context;

    public ContentAnalysisService() {
        context = this;
    }

    @Override
    public int onStartCommand(Intent i, int i1, int it2) {
        Log.d(TAG, "started");
        new CoreRunnable(context).execute(context);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class CoreRunnable extends AsyncTask<Context, Integer, Long> {
        Context context;

        public CoreRunnable(Context c1) {
            this.context = c1;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            Intent step = new Intent();
            step.setAction(ClassifierProgressReceiver.ACTION);
            step.putExtra(ClassifierProgressReceiver.PROGRESS, progress[0]);
            step.putExtra(ClassifierProgressReceiver.PROGRESS_LABEL, progress[0] + "% complete");
            sendBroadcast(step);
        }

        @Override
        protected Long doInBackground(Context... params) {
            context = params[0];
            Log.d(TAG, "running core runnable");
            publishProgress(10);

            ClusterGenerator gen = new ClusterGenerator(new Graph(context).getNodes(), NUM_CLUSTERS);
            publishProgress(30);

            WordBins helper = new WordBins(context);

            publishProgress(60);

            Log.d(TAG, "storing clusters");
            helper.storeClusters(gen.getClusters());

            publishProgress(100);

            Intent stopIntent = new Intent(context, ContentAnalysisService.class);
            stopService(stopIntent);

            long value = 1;
            return value;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ContentAnalysisService finished and destroyed");
    }

}
